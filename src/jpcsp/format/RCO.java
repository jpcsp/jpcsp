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
import jpcsp.format.rco.AnimFactory;
import jpcsp.format.rco.ObjectFactory;
import jpcsp.format.rco.object.BaseObject;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class RCO {
	public static final Logger log = Logger.getLogger("rco");
	private static final int RCO_MAGIC = 0x00505246;
	private static final int RCO_NULL_PTR = 0xFFFFFFFF;
	private static final int RCO_TABLE_MAIN = 1;
	private static final int RCO_TABLE_VSMX = 2;
	private static final int RCO_TABLE_TEXT = 3;
	private static final int RCO_TABLE_IMG = 4;
	private static final int RCO_TABLE_MODEL = 5;
	private static final int RCO_TABLE_SOUND = 6;
	private static final int RCO_TABLE_FONT = 7;
	private static final int RCO_TABLE_OBJ = 8;
	private static final int RCO_TABLE_ANIM = 9;
	private static final int RCO_DATA_COMPRESSION_NONE = 0;
	private static final int RCO_DATA_COMPRESSION_ZLIB = 1;
	private static final int RCO_DATA_COMPRESSION_RLZ = 2;
	private byte[] buffer;
	private int offset;
	private boolean valid;
	public byte[] VSMX;
	private int pLabelData;
	private int lLabelData;

	private class RCOEntry {
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
		public byte data[];
		public BaseObject obj;

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
						// now pairs of size/offset for each channel
						if (log.isDebugEnabled()) {
							log.debug(String.format("RCO entry SOUND: format=%d, channels=%d, sizeTotal=0x%X, offset=0x%X", format, channels, sizeTotal, offset));
						}
						for (int channel = 0; channel < channels; channel++) {
							int channelSize = read32();
							int channelOffset = read32();
							if (log.isDebugEnabled()) {
								log.debug(String.format("Channel %d: size=0x%X, offset=0x%X", channel, channelSize, channelOffset));
							}
						}
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
								int readLength = obj.read(data, 0);
								if (readLength != dataLength) {
									log.warn(String.format("Incorrect length data for ANIM"));
								}
								if (log.isDebugEnabled()) {
									log.debug(String.format("OBJ: %s", obj));
								}
							}
						}
					}
					break;
				case RCO_TABLE_ANIM:
					if (type > 1) {
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
								int readLength = obj.read(data, 0);
								if (readLength != dataLength) {
									log.warn(String.format("Incorrect length data for ANIM"));
								}
								if (log.isDebugEnabled()) {
									log.debug(String.format("ANIM: %s", obj));
								}
							}
						}
					}
					break;
				default:
					log.warn(String.format("Unknown RCO entry %s at offset 0x%X", getIdName(id), entryOffset));
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

	private byte[] readVSMX(int offset) {
		if (isNull(offset)) {
			return null;
		}

		RCOEntry entry = readRCOEntry(offset);
		int offsetVSMX = read32();
		int lengthVSMX = read32();
		skip(offsetVSMX);

		if (log.isDebugEnabled()) {
			log.debug(String.format("VSMX at 0x%X: entry=%s, offset=0x%X, length=0x%X", offset, entry, offsetVSMX, lengthVSMX));
		}

		return readBytes(lengthVSMX);
	}

	private String readLabel(int labelOffset) {
		StringBuilder s = new StringBuilder();

		int currentPosition = tell();
		seek(pLabelData + labelOffset);
		while (true) {
			int b = read8();
			if (b == 0) {
				break;
			}
			s.append((char) b);
		}
		seek(currentPosition);

		return s.toString();
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
		if (headerCompression != 0) {
			log.warn(String.format("Unimplemented RCO compression 0x%02X", compression));
			return false;
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("umdFlag=0x%X, headerCompression=0x%X", umdFlag, headerCompression));
		}

		int pMainTable = read32();
		int pVSMXTable = read32();
		int pTextTable = read32();
		int pSoundTable = read32();
		int pModelTable = read32();
		int pImgTable = read32();
		skip32(); // pUnknown
		int pFontTable = read32();
		int pObjTable = read32();
		int pAnimTable = read32();
		int pTextData = read32();
		int lTextData = read32();
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
		int pImgData = read32();
		int lImgData = read32();
		int pSoundData = read32();
		int lSoundData = read32();
		int pModelData = read32();
		int lModelData = read32();

		skip32(); // Always 0xFFFFFFFF
		skip32(); // Always 0xFFFFFFFF
		skip32(); // Always 0xFFFFFFFF

		RCOEntry mainTable = readRCOEntry(pMainTable);
		if (log.isDebugEnabled()) {
			log.debug(String.format("mainTable: %s", mainTable));
		}

		VSMX = readVSMX(pVSMXTable);

		return true;
	}

	@Override
	public String toString() {
		return String.format("RCO valid=%b", valid);
	}
}
