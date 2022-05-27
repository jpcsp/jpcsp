/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.format;

import static jpcsp.util.Utilities.endianSwap32;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import jpcsp.GUI.UmdVideoPlayer;
import jpcsp.format.rco.AnimFactory;
import jpcsp.format.rco.LZR;
import jpcsp.format.rco.ObjectFactory;
import jpcsp.format.rco.RCOContext;
import jpcsp.format.rco.RCOState;
import jpcsp.format.rco.SoundFactory;
import jpcsp.format.rco.object.BaseObject;
import jpcsp.format.rco.object.ImageObject;
import jpcsp.format.rco.vsmx.VSMX;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.objects.Controller;
import jpcsp.format.rco.vsmx.objects.GlobalVariables;
import jpcsp.format.rco.vsmx.objects.MoviePlayer;
import jpcsp.format.rco.vsmx.objects.Resource;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class RCO {
	public static final Logger log = Logger.getLogger("rco");
	private static final boolean dumpImages = false;
	private static final int RCO_HEADER_SIZE = 164;
	private static final int RCO_MAGIC = 0x00505246;
	private static final int RCO_NULL_PTR = 0xFFFFFFFF;
	public static final int RCO_TABLE_MAIN = 1;
	public static final int RCO_TABLE_VSMX = 2;
	public static final int RCO_TABLE_TEXT = 3;
	public static final int RCO_TABLE_IMG = 4;
	public static final int RCO_TABLE_MODEL = 5;
	public static final int RCO_TABLE_SOUND = 6;
	public static final int RCO_TABLE_FONT = 7;
	public static final int RCO_TABLE_OBJ = 8;
	public static final int RCO_TABLE_ANIM = 9;
	public static final int RCO_DATA_COMPRESSION_NONE = 0;
	public static final int RCO_DATA_COMPRESSION_ZLIB = 1;
	public static final int RCO_DATA_COMPRESSION_RLZ = 2;
	private static final Charset textDataCharset = Charset.forName("UTF-16LE");
	private byte[] buffer;
	private int offset;
	private boolean valid;
	private int pVSMXTable;
	private int pTextData;
	private int lTextData;
	private int pLabelData;
	private int lLabelData;
	private int pImgData;
	private int lImgData;
	private RCOEntry mainTable;
	private int[] compressedTextDataOffset;
	private Map<Integer, RCOEntry> entries;
	private Map<Integer, String> events;
	private Map<Integer, BufferedImage> images;
	private Map<Integer, BaseObject> objects;

	public class RCOEntry {
		private static final int RCO_ENTRY_SIZE = 40;
		public int type; // main table uses 0x01; may be used as a current entry depth value
		public int id;
		public int labelOffset;
		public String label;
		public int eHeadSize;
		public int entrySize;
		public int numSubEntries;
		public int nextEntryOffset;
		public int prevEntryOffset;
		public int parentTblOffset;
		public RCOEntry subEntries[];
		public RCOEntry parent;
		public byte data[];
		public BaseObject obj;
		public String[] texts;
		public VSMXBaseObject vsmxBaseObject;

		public void read() {
			int entryOffset = tell();
			type = read8();
			id = read8();
			skip16();
			labelOffset = read32();
			eHeadSize = read32();
			entrySize = read32();
			numSubEntries = read32();
			nextEntryOffset = read32();
			prevEntryOffset = read32();
			parentTblOffset = read32();
			skip32();
			skip32();

			entries.put(entryOffset, this);

			if (parentTblOffset != 0) {
				parent = entries.get(entryOffset - parentTblOffset);
			}

			if (labelOffset != RCO_NULL_PTR) {
				label = readLabel(labelOffset);
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("RCO entry at offset 0x%X: %s", entryOffset, toString()));
			}

			switch (id) {
				case RCO_TABLE_MAIN:
					if (type != 1) {
						log.warn(String.format("Unknown RCO entry type 0x%X at offset 0x%X", type, entryOffset));
					}
					break;
				case RCO_TABLE_VSMX:
					if (type == 1) {
						int offsetVSMX = read32();
						int lengthVSMX = read32();
						skip(offsetVSMX);
						data = readBytes(lengthVSMX);
						// 4-bytes alignment
						skip(Utilities.alignUp(lengthVSMX, 3) - lengthVSMX);
					} else {
						log.warn(String.format("Unknown RCO entry type 0x%X at offset 0x%X", type, entryOffset));
					}
					break;
				case RCO_TABLE_IMG:
				case RCO_TABLE_MODEL:
					if (type == 1) {
						int format = read16();
						int compression = read16();
						int sizePacked = read32();
						int offset = read32();
						int sizeUnpacked; // this value doesn't exist if entry isn't compressed
						if (compression != RCO_DATA_COMPRESSION_NONE) {
							sizeUnpacked = read32();
						} else {
							sizeUnpacked = sizePacked;
						}

						if (id == RCO_TABLE_IMG) {
							BufferedImage image = readImage(offset, sizePacked);
							if (image != null) {
								obj = new ImageObject(image);
								images.put(entryOffset, image);
							}
						}

						if (log.isDebugEnabled()) {
							log.debug(String.format("RCO entry %s: format=%d, compression=%d, sizePacked=0x%X, offset=0x%X, sizeUnpacked=0x%X", id == RCO_TABLE_IMG ? "IMG" : "MODEL", format, compression, sizePacked, offset, sizeUnpacked));
						}
					} else if (type != 0) {
						log.warn(String.format("Unknown RCO entry type 0x%X at offset 0x%X", type, entryOffset));
					}
					break;
				case RCO_TABLE_SOUND:
					if (type == 1) {
						int format = read16(); // 0x01 = VAG
						int channels = read16(); // 1 or 2 channels
						int sizeTotal = read32();
						int offset = read32();
						int[] channelSize = new int[channels];
						int[] channelOffset = new int[channels];
						// now pairs of size/offset for each channel
						if (log.isDebugEnabled()) {
							log.debug(String.format("RCO entry SOUND: format=%d, channels=%d, sizeTotal=0x%X, offset=0x%X", format, channels, sizeTotal, offset));
						}
						for (int channel = 0; channel < channels; channel++) {
							channelSize[channel] = read32();
							channelOffset[channel] = read32();
							if (log.isDebugEnabled()) {
								log.debug(String.format("Channel %d: size=0x%X, offset=0x%X", channel, channelSize[channel], channelOffset[channel]));
							}
						}

						obj = SoundFactory.newSound(format, channels, channelSize, channelOffset);

						// there _must_ be two channels defined (no clear indication of size otherwise)
						if (channels < 2) {
							for (int i = channels; i < 2; i++) {
								int dummyChannelSize = read32();
								int dummyChannelOffset = read32();
								if (log.isTraceEnabled()) {
									log.trace(String.format("Dummy channel %d: size=0x%X, offset=0x%X", i, dummyChannelSize, dummyChannelOffset));
								}
							}
						}
					} else if (type != 0) {
						log.warn(String.format("Unknown RCO entry type 0x%X at offset 0x%X", type, entryOffset));
					}
					break;
				case RCO_TABLE_OBJ:
					if (type > 0) {
						obj = ObjectFactory.newObject(type);

						if (obj != null && entrySize == 0) {
							entrySize = obj.size() + RCO_ENTRY_SIZE;
						}

						if (entrySize > RCO_ENTRY_SIZE) {
							int dataLength = entrySize - RCO_ENTRY_SIZE;
							data = readBytes(dataLength);
							if (log.isTraceEnabled()) {
								log.trace(String.format("OBJ data at 0x%X: %s", entryOffset + RCO_ENTRY_SIZE, Utilities.getMemoryDump(data, 0, dataLength)));
							}

							if (obj != null) {
								RCOContext context = new RCOContext(data, 0, events, images, objects);
								obj.read(context);
								if (context.offset != dataLength) {
									log.warn(String.format("Incorrect length data for ANIM"));
								}

								objects.put(entryOffset, obj);

								if (log.isDebugEnabled()) {
									log.debug(String.format("OBJ: %s", obj));
								}
							}
						}
					}
					break;
				case RCO_TABLE_ANIM:
					if (type > 0) {
						obj = AnimFactory.newAnim(type);

						if (obj != null && entrySize == 0) {
							entrySize = obj.size() + RCO_ENTRY_SIZE;
						}

						if (entrySize > RCO_ENTRY_SIZE) {
							int dataLength = entrySize - RCO_ENTRY_SIZE;
							data = readBytes(dataLength);
							if (log.isTraceEnabled()) {
								log.trace(String.format("ANIM data at 0x%X: %s", entryOffset + RCO_ENTRY_SIZE, Utilities.getMemoryDump(data, 0, dataLength)));
							}

							if (obj != null) {
								RCOContext context = new RCOContext(data, 0, events, images, objects);
								obj.read(context);
								if (context.offset != dataLength) {
									log.warn(String.format("Incorrect length data for ANIM"));
								}

								objects.put(entryOffset, obj);

								if (log.isDebugEnabled()) {
									log.debug(String.format("ANIM: %s", obj));
								}
							}
						}
					}
					break;
				case RCO_TABLE_FONT:
					if (type == 1) {
						int format = read16();
						int compression = read16();
						int unknown1 = read32();
						int unknown2 = read32();
						if (log.isDebugEnabled()) {
							log.debug(String.format("RCO entry FONT: format=%d, compression=%d, unknown1=0x%X, unknown2=0x%X", format, compression, unknown1, unknown2));
						}
					} else if (type != 0) {
						log.warn(String.format("Unknown RCO FONT entry type 0x%X at offset 0x%X", type, entryOffset));
					}
					break;
				case RCO_TABLE_TEXT:
					if (type == 1) {
						int lang = read16();
						int format = read16();
						int numIndexes = read32();
						if (log.isDebugEnabled()) {
							log.debug(String.format("RCO entry TEXT: lang=%d, format=%d, numIndexes=0x%X", lang, format, numIndexes));
						}
						texts = new String[numIndexes];
						for (int i = 0; i < numIndexes; i++) {
							int labelOffset = read32();
							int length = read32();
							int offset = read32();
							texts[i] = readText(lang, offset, length);
							if (log.isDebugEnabled()) {
								log.debug(String.format("RCO entry TEXT Index#%d: labelOffset=%d, length=%d, offset=0x%X; '%s'", i, labelOffset, length, offset, texts[i]));
							}
						}
					} else if (type != 0) {
						log.warn(String.format("Unknown RCO TEXT entry type 0x%X at offset 0x%X", type, entryOffset));
					}
					break;
				default:
					log.warn(String.format("Unknown RCO entry id 0x%X at offset 0x%X", id, entryOffset));
					break;
			}

			if (numSubEntries > 0) {
				subEntries = new RCOEntry[numSubEntries];
				for (int i = 0; i < numSubEntries; i++) {
					subEntries[i] = new RCOEntry();
					subEntries[i].read();
				}
			}

		}

		private String getIdName(int id) {
			String idNames[] = new String[] {
					null,
					"MAIN",
					"VSMX",
					"TEXT",
					"IMG",
					"MODEL",
					"SOUND",
					"FONT",
					"OBJ",
					"ANIM"
			};
			if (id < 0 || id >= idNames.length || idNames[id] == null) {
				return String.format("0x%X", id);
			}

			return idNames[id];
		}

		@Override
		public String toString() {
			return String.format("RCOEntry[type=0x%X, id=%s, labelOffset=0x%X('%s'), eHeadSize=0x%X, entrySize=0x%X, numSubEntries=%d, nextEntryOffset=0x%X, prevEntryOffset=0x%X, parentTblOffset=0x%X", type, getIdName(id), labelOffset, label != null ? label : "", eHeadSize, entrySize, numSubEntries, nextEntryOffset, prevEntryOffset, parentTblOffset);
		}
	}

	private int read8() {
		return buffer[offset++] & 0xFF;
	}

	private int read16() {
		return read8() | (read8() << 8);
	}

	private int read32() {
		return read16() | (read16() << 16);
	}

	private void skip(int n) {
		offset += n;
	}

	private void skip32() {
		skip(4);
	}

	private void skip16() {
		skip(2);
	}

	private void seek(int offset) {
		this.offset = offset;
	}

	private int tell() {
		return offset;
	}

	public RCO(byte[] buffer) {
		this.buffer = buffer;

		valid = read();
	}

	public boolean isValid() {
		return valid;
	}

	private RCOEntry readRCOEntry() {
		RCOEntry entry = new RCOEntry();
		entry.read();
		return entry;
	}

	private RCOEntry readRCOEntry(int offset) {
		seek(offset);
		return readRCOEntry();
	}

	private boolean isNull(int ptr) {
		return ptr == RCO_NULL_PTR;
	}

	private byte[] readBytes(int length) {
		if (length < 0) {
			return null;
		}

		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			bytes[i] = (byte) read8();
		}

		return bytes;
	}

	private byte[] readVSMX(int offset, StringBuilder name) {
		if (isNull(offset)) {
			return null;
		}

		RCOEntry entry = readRCOEntry(offset);

		name.append(entry.label);

		return entry.data;
	}

	private String readLabel(int labelOffset) {
		StringBuilder s = new StringBuilder();

		int currentPosition = tell();
		seek(pLabelData + labelOffset);
		for (int maxLength = lLabelData - labelOffset; maxLength > 0; maxLength--) {
			int b = read8();
			if (b == 0) {
				break;
			}
			s.append((char) b);
		}
		seek(currentPosition);

		return s.toString();
	}

	private String readText(int lang, int offset, int length) {
		if (offset == RCO_NULL_PTR) {
			return null;
		}

		int currentPosition = tell();
		if (compressedTextDataOffset != null) {
			seek(compressedTextDataOffset[lang] + offset);
		} else {
			seek(pTextData + offset);
		}
		byte[] buffer = readBytes(length);
		seek(currentPosition);

		// Trailing null bytes?
		if (length >= 2 && buffer[length - 1] == (byte) 0 && buffer[length - 2] == (byte) 0) {
			// Remove trailing null bytes
			length -= 2;
		}

		return new String(buffer, 0, length, textDataCharset);
	}

	private BufferedImage readImage(int offset, int length) {
		int currentPosition = tell();
		seek(pImgData + offset);
		byte[] buffer = readBytes(length);
		seek(currentPosition);

		InputStream imageInputStream = new ByteArrayInputStream(buffer);
		BufferedImage bufferedImage = null;
		try {
			bufferedImage = ImageIO.read(imageInputStream);
			imageInputStream.close();

			// Add an alpha color channel if not available
			if (!bufferedImage.getColorModel().hasAlpha()) {
				BufferedImage bufferedImageWithAlpha = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = bufferedImageWithAlpha.createGraphics();
				g.drawImage(bufferedImage, 0, 0, null);
				g.dispose();
				bufferedImage = bufferedImageWithAlpha;
			}

			if (dumpImages) {
				ImageIO.write(bufferedImage, "png", new File(String.format("tmp/Image0x%X.png", offset)));
			}
		} catch (IOException e) {
			log.error(String.format("Error reading image from RCO at 0x%X, length=0x%X", offset, length), e);
		}

		return bufferedImage;
	}

	private String readString() {
		StringBuilder s = new StringBuilder();
		while (true) {
			int b = read8();
			if (b == 0) {
				break;
			}
			s.append((char) b);
		}

		return s.toString();
	}

	private static byte[] append(byte[] a, byte[] b) {
		if (a == null || a.length == 0) {
			return b;
		}
		if (b == null || b.length == 0) {
			return a;
		}

		byte[] ab = new byte[a.length + b.length];
		System.arraycopy(a, 0, ab, 0, a.length);
		System.arraycopy(b, 0, ab, a.length, b.length);

		return ab;
	}

	private static byte[] append(byte[] a, int length, byte[] b) {
		if (a == null || a.length == 0 || length <= 0) {
			return b;
		}
		if (b == null || b.length == 0) {
			return a;
		}
		length = Math.min(a.length, length);

		byte[] ab = new byte[length + b.length];
		System.arraycopy(a, 0, ab, 0, length);
		System.arraycopy(b, 0, ab, length, b.length);

		return ab;
	}

	private static int[] extend(int[] a, int length) {
		if (a == null) {
			return new int[length];
		}
		if (a.length >= length) {
			return a;
		}
		int[] b = new int[length];
		System.arraycopy(a, 0, b, 0, a.length);

		return b;
	}

	/**
	 * Read a RCO file.
	 * See description of an RCO file structure in
	 * https://github.com/kakaroto/RCOMage/blob/master/src/rcofile.h
	 * 
	 * @return true  RCO file is valid
	 *         false RCO file is invalid
	 */
	private boolean read() {
		int magic = endianSwap32(read32());
		if (magic != RCO_MAGIC) {
			log.warn(String.format("Invalid RCO magic 0x%08X", magic));
			return false;
		}
		int version = read32();
		if (log.isDebugEnabled()) {
			log.debug(String.format("RCO version 0x%X", version));
		}

		skip32(); // null
		int compression = read32();
		int umdFlag = compression & 0x0F;
		int headerCompression = (compression & 0xF0) >> 4;
		if (log.isDebugEnabled()) {
			log.debug(String.format("umdFlag=0x%X, headerCompression=0x%X", umdFlag, headerCompression));
		}

		int pMainTable = read32();
		pVSMXTable = read32();
		int pTextTable = read32();
		int pSoundTable = read32();
		int pModelTable = read32();
		int pImgTable = read32();
		skip32(); // pUnknown
		int pFontTable = read32();
		int pObjTable = read32();
		int pAnimTable = read32();
		pTextData = read32();
		lTextData = read32();
		pLabelData = read32();
		lLabelData = read32();
		int pEventData = read32();
		int lEventData = read32();
		int pTextPtrs = read32();
		int lTextPtrs = read32();
		int pImgPtrs = read32();
		int lImgPtrs = read32();
		int pModelPtrs = read32();
		int lModelPtrs = read32();
		int pSoundPtrs = read32();
		int lSoundPtrs = read32();
		int pObjPtrs = read32();
		int lObjPtrs = read32();
		int pAnimPtrs = read32();
		int lAnimPtrs = read32();
		pImgData = read32();
		lImgData = read32();
		int pSoundData = read32();
		int lSoundData = read32();
		int pModelData = read32();
		int lModelData = read32();

		skip32(); // Always 0xFFFFFFFF
		skip32(); // Always 0xFFFFFFFF
		skip32(); // Always 0xFFFFFFFF

		if (log.isDebugEnabled()) {
			log.debug(String.format("pMainTable=0x%X, pVSMXTable=0x%X, pTextTable=0x%X, pSoundTable=0x%X, pModelTable=0x%X, pImgTable=0x%X, pFontTable=0x%X, pObjTable=0x%X, pAnimTable=0x%X", pMainTable, pVSMXTable, pTextTable, pSoundTable, pModelTable, pImgTable, pFontTable, pObjTable, pAnimTable));
			log.debug(String.format("TextData=0x%X[0x%X], LabelData=0x%X[0x%X], EventData=0x%X[0x%X]", pTextData, lTextData, pLabelData, lLabelData, pEventData, lEventData));
			log.debug(String.format("TextPtrs=0x%X[0x%X], ImgPtrs=0x%X[0x%X], ModelPtrs=0x%X[0x%X], SoundPtrs=0x%X[0x%X], ObjPtrs=0x%X[0x%X], AnimPtrs=0x%X[0x%X]", pTextPtrs, lTextPtrs, pImgPtrs, lImgPtrs, pModelPtrs, lModelPtrs, pSoundPtrs, lSoundPtrs, pObjPtrs, lObjPtrs, pAnimPtrs, lAnimPtrs));
			log.debug(String.format("ImgData=0x%X[0x%X], SoundData=0x%X[0x%X], ModelData=0x%X[0x%X]", pImgData, lImgData, pSoundData, lSoundData, pModelData, lModelData));
		}

		if (headerCompression != 0) {
			int lenPacked = read32();
			int lenUnpacked = read32();
			int lenLongestText = read32();
			byte[] packedBuffer = readBytes(lenPacked);
			byte[] unpackedBuffer = new byte[lenUnpacked];
			int result;

			if (headerCompression == RCO_DATA_COMPRESSION_RLZ) {
				result = LZR.decompress(unpackedBuffer, lenUnpacked, packedBuffer);
			} else {
				log.warn(String.format("Unimplemented compression %d", headerCompression));
				result = -1;
			}

			if (log.isTraceEnabled()) {
				log.trace(String.format("Unpack header longestText=0x%X, result=0x%X: %s", lenLongestText, result, Utilities.getMemoryDump(unpackedBuffer, 0, lenUnpacked)));
			}

			if (pTextData != RCO_NULL_PTR && lTextData > 0) {
				seek(pTextData);
				int nextOffset;
				do {
					int textLang = read16();
					skip16();
					nextOffset = read32();
					int textLenPacked = read32();
					int textLenUnpacked = read32();

					byte[] textPackedBuffer = readBytes(textLenPacked);
					byte[] textUnpackedBuffer = new byte[textLenUnpacked];

					if (headerCompression == RCO_DATA_COMPRESSION_RLZ) {
						result = LZR.decompress(textUnpackedBuffer, textLenUnpacked, textPackedBuffer);
					} else {
						log.warn(String.format("Unimplemented compression %d", headerCompression));
						result = -1;
					}

					if (log.isTraceEnabled()) {
						log.trace(String.format("Unpack text lang=%d, result=0x%X: %s", textLang, result, Utilities.getMemoryDump(textUnpackedBuffer, 0, textLenUnpacked)));
					}

					if (result >= 0) {
						compressedTextDataOffset = extend(compressedTextDataOffset, textLang + 1);
						compressedTextDataOffset[textLang] = unpackedBuffer.length + RCO_HEADER_SIZE;
						unpackedBuffer = append(unpackedBuffer, textUnpackedBuffer);
					}

					if (nextOffset == 0) {
						break;
					}
					skip(nextOffset - 16 - textLenPacked);
				} while (nextOffset != 0);
			}

			if (result >= 0) {
				buffer = append(buffer, RCO_HEADER_SIZE, unpackedBuffer);
			}
		}

		events = new HashMap<Integer, String>();
		if (pEventData != RCO_NULL_PTR && lEventData > 0) {
			seek(pEventData);
			while (tell() < pEventData + lEventData) {
				int index = tell() - pEventData;
				String s = readString();
				if (s != null && s.length() > 0) {
					events.put(index, s);
				}
			}
		}

		entries = new HashMap<Integer, RCO.RCOEntry>();
		images = new HashMap<Integer, BufferedImage>();
		objects = new HashMap<Integer, BaseObject>();

		mainTable = readRCOEntry(pMainTable);
		if (log.isDebugEnabled()) {
			log.debug(String.format("mainTable: %s", mainTable));
		}

		if (pObjPtrs != RCO_NULL_PTR) {
			seek(pObjPtrs);
			for (int i = 0; i < lObjPtrs; i += 4) {
				int objPtr = read32();
				if (objPtr != 0 && !objects.containsKey(objPtr)) {
					log.warn(String.format("Object 0x%X not read", objPtr));
				}
			}
		}

		if (pImgPtrs != RCO_NULL_PTR) {
			seek(pImgPtrs);
			for (int i = 0; i < lImgPtrs; i += 4) {
				int imgPtr = read32();
				if (imgPtr != 0 && !images.containsKey(imgPtr)) {
					log.warn(String.format("Image 0x%X not read", imgPtr));
				}
			}
		}

		RCOContext context = new RCOContext(null, 0, events, images, objects);
		for (BaseObject object : objects.values()) {
			object.init(context);
		}
		return true;
	}

	public RCOState execute(UmdVideoPlayer umdVideoPlayer, String resourceName) {
		RCOState state = null;
		if (pVSMXTable != RCO_NULL_PTR) {
			state = new RCOState();
			state.interpreter = new VSMXInterpreter();
			state.controller = Controller.create(state.interpreter, umdVideoPlayer, resourceName);
			state = execute(state, umdVideoPlayer, resourceName);

			state.controller.getObject().callCallback(state.interpreter, "onAutoPlay", null);
		}

		return state;
	}

	public RCOState execute(RCOState state, UmdVideoPlayer umdVideoPlayer, String resourceName) {
		if (pVSMXTable != RCO_NULL_PTR) {
			StringBuilder vsmxName = new StringBuilder();
			VSMX vsmx = new VSMX(readVSMX(pVSMXTable, vsmxName), vsmxName.toString());
			state.interpreter.setVSMX(vsmx);
			state.globalVariables = GlobalVariables.create(state.interpreter);
			state.globalVariables.setPropertyValue(Controller.objectName, state.controller);
			state.globalVariables.setPropertyValue(MoviePlayer.objectName, MoviePlayer.create(state.interpreter, umdVideoPlayer, state.controller));
			state.globalVariables.setPropertyValue(Resource.objectName, Resource.create(state.interpreter, umdVideoPlayer.getRCODisplay(), state.controller, mainTable));
			state.globalVariables.setPropertyValue(jpcsp.format.rco.vsmx.objects.Math.objectName, jpcsp.format.rco.vsmx.objects.Math.create(state.interpreter));
			state.interpreter.run(state.globalVariables);
		}

		return state;
	}

	@Override
	public String toString() {
		return String.format("RCO valid=%b", valid);
	}
}
