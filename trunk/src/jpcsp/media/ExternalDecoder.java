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
package jpcsp.media;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Vector;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceAtrac3plus;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.HLE.modules150.IoFileMgrForUser.IIoListener;
import jpcsp.connector.AtracCodec;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.Hash;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class ExternalDecoder {
	private static Logger log = Modules.log;
    private static File extAudioDecoder;
    private static IoListener ioListener;
    private static boolean enabled = true;
    private static boolean dumpEncodedFile = false;
    private static boolean dumpPmfFile = false;
    private static boolean dumpAudioStreamFile = false;
    private static boolean keepOmaFile = true;
    private static boolean scanAllFileMagicOffsets = true;
    private static EnableExternalDecoderSettingsListerner enableExternalDecoderSettingsListerner;

	private static class EnableExternalDecoderSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setEnabled(value);
		}
	}

    public ExternalDecoder() {
    	init();
    }

    private static void setEnabled(boolean flag) {
    	enabled = flag;
    }

    private static void init() {
    	if (enableExternalDecoderSettingsListerner == null) {
    		enableExternalDecoderSettingsListerner = new EnableExternalDecoderSettingsListerner();
    		Settings.getInstance().registerSettingsListener("ExternalDecoder", "emu.useExternalDecoder", enableExternalDecoderSettingsListerner);
    	}

    	if (enabled && extAudioDecoder == null) {
    		String extAudioDecoderPath = System.getProperty("java.library.path");
    		if (extAudioDecoderPath == null) {
    			extAudioDecoderPath = "";
    		} else if (!extAudioDecoderPath.endsWith("/")) {
    			extAudioDecoderPath += "/";
    		}

    		String extAudioDecoders[] = { "DecodeAudio.bat", "DecodeAudio.exe", "DecodeAudio.sh" };
            for (int i = 0; i < extAudioDecoders.length; i++) {
                File f = new File(String.format("%s%s", extAudioDecoderPath, extAudioDecoders[i]));
                if (f.exists()) {
                    extAudioDecoder = f;
                    break;
                }
            }

            if (extAudioDecoder == null) {
            	enabled = false;
            } else {
            	log.info("Using the external audio decoder (SonicStage)");
            }
    	}

    	if (enabled && ioListener == null) {
    		ioListener = new IoListener();
    		Modules.IoFileMgrForUserModule.registerIoListener(ioListener);
    	}
    }

    public static boolean isEnabled() {
    	init();

    	return enabled;
    }

    private boolean executeExternalDecoder(String inputFileName, String outputFileName, boolean keepInputFile) {
    	boolean decoded = executeExternalDecoder(inputFileName, outputFileName);

    	if (!keepInputFile) {
    		File inputFile = new File(inputFileName);
    		inputFile.delete();
    	}

    	File outputFile = new File(outputFileName);
    	if (outputFile.canRead() && outputFile.length() == 0) {
			// Only an empty file has been generated, the file could not be converted
    		outputFile.delete();
    		decoded = false;
    	}

    	return decoded;
    }

    private boolean executeExternalDecoder(String inputFileName, String outputFileName) {
		String[] cmd;
		if (extAudioDecoder.toString().endsWith(".bat")) {
			cmd = new String[] {
					"cmd",
					"/C",
					extAudioDecoder.toString(),
					inputFileName,
					outputFileName };
		} else {
			cmd = new String[] {
					extAudioDecoder.toString(),
					inputFileName,
					outputFileName };
		}
		try {
			Process extAudioDecodeProcess = Runtime.getRuntime().exec(cmd);
			StreamReader stdoutReader = new StreamReader(extAudioDecodeProcess.getInputStream());
			StreamReader stderrReader = new StreamReader(extAudioDecodeProcess.getErrorStream());
			stdoutReader.start();
			stderrReader.start();
			int exitValue = extAudioDecodeProcess.waitFor();
			if (log.isDebugEnabled()) {
				log.debug(String.format("External AudioDecode Process '%s' returned %d", extAudioDecoder.toString(), exitValue));
				log.debug("stdout: " + stdoutReader.getInput());
				log.debug("stderr: " + stderrReader.getInput());
			}
		} catch (InterruptedException e) {
			log.error(e);
			return false;
		} catch (IOException e) {
			log.error(e);
			return false;
		}

		return true;
    }

    public boolean decodeExtAudio(byte[] mpegData, int mpegFileSize, int mpegOffset) {
    	if (dumpPmfFile) {
			try {
				new File(MediaEngine.getExtAudioBasePath(mpegFileSize)).mkdirs();
				FileOutputStream pmfOut = new FileOutputStream(MediaEngine.getExtAudioPath(mpegFileSize, "pmf"));
				pmfOut.write(mpegData);
				pmfOut.close();
			} catch (IOException e) {
				log.error(e);
			}
    	}

    	MpegDemux mpegDemux = new MpegDemux(mpegData, mpegOffset);
    	try {
    		mpegDemux.demux(false, true);
		} catch (OutOfMemoryError e) {
			log.error(String.format("Error '%s' while decoding external audio file (mpegFileSize=%d)", e.toString(), mpegFileSize));
			return false;
		}

		ByteBuffer audioStream = mpegDemux.getAudioStream();
		if (audioStream == null) {
			return false;
		}

		if (dumpAudioStreamFile) {
			try {
				new File(MediaEngine.getExtAudioBasePath(mpegFileSize)).mkdirs();
				FileOutputStream pmfOut = new FileOutputStream(MediaEngine.getExtAudioPath(mpegFileSize, "audio"));
				pmfOut.getChannel().write(audioStream);
				audioStream.rewind();
				pmfOut.close();
			} catch (IOException e) {
				log.error(e);
			}
		}

		ByteBuffer omaBuffer = OMAFormat.convertStreamToOMA(audioStream); 
		if (omaBuffer == null) {
			return false;
		}

		try {
			new File(MediaEngine.getExtAudioBasePath(mpegFileSize)).mkdirs();
			String encodedFileName = MediaEngine.getExtAudioPath(mpegFileSize, "oma");
			FileOutputStream os = new FileOutputStream(encodedFileName);
			os.getChannel().write(omaBuffer);
			os.close();

			String decodedFileName = MediaEngine.getExtAudioPath(mpegFileSize, "wav");

			if (!executeExternalDecoder(encodedFileName, decodedFileName, keepOmaFile)) {
				return false;
			}
		} catch (IOException e) {
			log.error(e);
			return false;
		}

		return true;
    }

    public void decodeExtAudio(int address, int mpegFileSize, int mpegOffset, byte[] bufferHeaderData) {
    	if (!isEnabled()) {
    		return;
    	}

		byte[] mpegData = ioListener.readFileData(address, sceMpeg.MPEG_HEADER_BUFFER_MINIMUM_SIZE, mpegFileSize, bufferHeaderData);
    	if (mpegData == null) {
    		// MPEG data cannot be retrieved...
    		return;
    	}

    	decodeExtAudio(mpegData, mpegFileSize, mpegOffset);
    }

    private static String getAtracAudioPath(int address, int atracFileSize, String suffix) {
    	return String.format("%sAtrac-%08X-%08X.%s", AtracCodec.getBaseDirectory(), atracFileSize, address, suffix);
    }

    private String decodeAtrac(byte[] atracData, int address, int atracFileSize, String decodedFileName) {
    	try {
	    	ByteBuffer riffBuffer = ByteBuffer.wrap(atracData);
	    	if (dumpEncodedFile) {
	    		// For debugging purpose, optionally dump the original atrac file in RIFF format
				new File(AtracCodec.getBaseDirectory()).mkdirs();
	    		FileOutputStream encodedOut = new FileOutputStream(getAtracAudioPath(address, atracFileSize, "encoded"));
	    		encodedOut.getChannel().write(riffBuffer);
	    		encodedOut.close();
	    		riffBuffer.rewind();
	    	}
	    	ByteBuffer omaBuffer = OMAFormat.convertRIFFtoOMA(riffBuffer);
	    	if (omaBuffer == null) {
				Modules.log.info("AT3+ data could not be decoded by the external decoder (error while converting to OMA)");
	    		return null;
	    	}

			new File(AtracCodec.getBaseDirectory()).mkdirs();
			String encodedFileName = getAtracAudioPath(address, atracFileSize, "oma");
			FileOutputStream os = new FileOutputStream(encodedFileName);
			os.getChannel().write(omaBuffer);
			os.close();

			if (!executeExternalDecoder(encodedFileName, decodedFileName, keepOmaFile)) {
				int channels = OMAFormat.getOMANumberAudioChannels(omaBuffer);
				if (channels == 1) {
					// It seems that SonicStage has problems decoding mono AT3+ data
					// or we might generate an incorrect OMA file for monaural audio.
					Modules.log.info("Mono AT3+ data could not be decoded by the external decoder");
				} else if (channels == 2) {
					Modules.log.info("Stereo AT3+ data could not be decoded by the external decoder");
				} else {
					Modules.log.info("AT3+ data could not be decoded by the external decoder (channels=" + channels + ")");
				}
				return null;
			}
    	} catch (IOException e) {
			// Ignore Exception
			log.error(e);
		}

		return decodedFileName;
    }

    public String decodeAtrac(PacketChannel packetChannel, int address, int atracFileSize) {
    	if (!isEnabled()) {
    		return null;
    	}

    	byte[] atracData = new byte[atracFileSize];
    	int readLength = packetChannel.read(atracData, atracData.length);
    	if (readLength != atracData.length) {
    		return null;
    	}

		String decodedFileName = getAtracAudioPath(address, atracFileSize, "wav");
    	return decodeAtrac(atracData, address, atracFileSize, decodedFileName);
    }

    public String decodeAtrac(int address, int length, int atracFileSize, AtracCodec atracCodec) {
    	if (!isEnabled()) {
    		return null;
    	}

		String decodedFileName = getAtracAudioPath(address, atracFileSize, "wav");
		File decodedFile = new File(decodedFileName);
		if (decodedFile.canRead() && decodedFile.length() > 0) {
			// Already decoded
			return decodedFileName;
		}

		byte[] atracData;
		if (length >= atracFileSize) {
			// We have the complete atrac data available, no need to use the ioListener
			atracData = new byte[atracFileSize];
			// Copy the memory to the atracData
			Utilities.putBuffer(ByteBuffer.wrap(atracData), Memory.getInstance().getBuffer(address, length), ByteOrder.LITTLE_ENDIAN, atracData.length);
		} else {
			// We do not have the complete atrac data in memory, try to read
			// the complete data from the UMD.
			atracData = ioListener.readFileData(address, length, atracFileSize, null);
		}
    	if (atracData == null) {
    		// Atrac data cannot be retrieved...
			Modules.log.debug("AT3+ data could not be decoded by the external decoder (complete atrac data need to be retrieved)");
			atracCodec.setRequireAllAtracData();
    		return null;
    	}

    	return decodeAtrac(atracData, address, atracFileSize, decodedFileName);
    }

    private static class IoListener implements IIoListener {
    	private static class ReadInfo {
    		public int address;
    		public int size;
    		public SeekableDataInput dataInput;
    		public long position;

    		public ReadInfo(int address, int size, SeekableDataInput dataInput, long position) {
    			this.address = address;
    			this.size = size;
    			this.dataInput = dataInput;
    			this.position = position;
    		}

			@Override
			public String toString() {
				return String.format("ReadInfo(0x%08X-0x%08X(size=0x%X), position=%d, %s)", address, address + size, size, position, dataInput.toString());
			}
    	}

    	private HashMap<Integer, ReadInfo> readInfos;
    	private HashMap<Integer, ReadInfo> readMagics;
    	private static final int MAGIC_HASH_LENGTH = 16;
    	private static final int[] fileMagics = {
    		sceAtrac3plus.RIFF_MAGIC,
    		sceMpeg.PSMF_MAGIC
    	};

    	public IoListener() {
    		readInfos = new HashMap<Integer, ReadInfo>();
    		readMagics = new HashMap<Integer, ReadInfo>();
    	}

    	private static boolean memcmp(byte[] data, int address, int length) {
    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
    		for (int i = 0; i < length; i++) {
    			if (memoryReader.readNext() != (data[i] & 0xFF)) {
    				return false;
    			}
    		}

    		return true;
    	}

    	private static boolean cmp(byte[] data, byte[] checkData, int length) {
    		length = Math.min(length, checkData.length);
    		for (int i = 0; i < length; i++) {
    			if (data[i] != checkData[i]) {
    				return false;
    			}
    		}

    		return true;
    	}

    	private static int getMagicHash(int address) {
    		return Hash.getHashCodeFloatingMemory(0, address, MAGIC_HASH_LENGTH);
    	}

    	private static int getMagicHash(byte[] data) {
    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(data, 0, MAGIC_HASH_LENGTH, 4);
    		return Hash.getHashCodeFloatingMemory(0, memoryReader, MAGIC_HASH_LENGTH);
    	}

    	public byte[] readFileData(int address, int length, int fileSize, byte[] checkData) {
    		int positionOffset = 0;
    		ReadInfo readInfo = readInfos.get(address);
    		if (readInfo == null) {
    			// The file data has not been read at this address.
    			// Search for files having the same size and content
    			for (ReadInfo ri : readInfos.values()) {
    				try {
						if (ri.dataInput.length() == fileSize) {
							// Both file have the same length, check the content
							byte[] fileData = new byte[length];
							long currentPosition = ri.dataInput.getFilePointer();
							ri.dataInput.seek(ri.position);
							ri.dataInput.readFully(fileData);
							ri.dataInput.seek(currentPosition);
							if (memcmp(fileData, address, length)) {
								// Both files have the same content, we have found it!
								readInfo = ri;
								break;
							}
						} else if (ri.address < address && ri.address + ri.size >= address + length) {
							positionOffset = address - ri.address;
							readInfo = ri;
							break;
						}
					} catch (IOException e) {
						// Ignore the exception
					}
    			}

    			if (readInfo == null) {
    				// Search for a file having the same magic hash value
    				ReadInfo ri = readMagics.get(getMagicHash(address));
    				// If not found at the given address
    				// (e.g. the memory has already been overwritten),
    				// try with the checkData.
    				if (ri == null && checkData != null && checkData.length >= MAGIC_HASH_LENGTH) {
    					ri = readMagics.get(getMagicHash(checkData));
    				}

    				if (ri != null) {
    					try {
    						// Check if the file length is large enough
    						if (ri.dataInput.length() >= fileSize) {
    							// Check if the file contents are matching our buffer
    							int checkLength = Math.min(length, fileSize);
    							byte[] fileData = new byte[checkLength];
    							long currentPosition = ri.dataInput.getFilePointer();
    							ri.dataInput.seek(ri.position);
    							ri.dataInput.readFully(fileData);
    							ri.dataInput.seek(currentPosition);

    							boolean match;
    							if (checkData != null) {
    								// Check against checkData
    								match = cmp(fileData, checkData, checkLength);
    							} else {
    								// Check against memory data located at "address"
    								match = memcmp(fileData, address, checkLength);
    							}

    							if (match) {
									// Both files have the same content, we have found it!
									readInfo = ri;
    							}
    						}
    					} catch (IOException e) {
    						// Ignore exception
    					}
    				}

    				if (readInfo == null) {
    					return null;
    				}
    			}
    		}

    		byte[] fileData;
    		try {
        		fileData = new byte[fileSize];
				long currentPosition = readInfo.dataInput.getFilePointer();
				readInfo.dataInput.seek(readInfo.position + positionOffset);
	    		readInfo.dataInput.readFully(fileData);
				readInfo.dataInput.seek(currentPosition);
			} catch (IOException e) {
				return null;
			} catch (OutOfMemoryError e) {
				log.error(String.format("Error '%s' while decoding external audio file (fileSize=%d, position=%d, dataInput=%s)", e.toString(), fileSize, readInfo.position + positionOffset, readInfo.dataInput.toString()));
				return null;
			}

			// Check if the file data is really matching the data in memory
			int checkLength = Math.min(length, MAGIC_HASH_LENGTH);
			boolean match;
			if (checkData != null) {
				// Check against checkData
				match = cmp(fileData, checkData, checkLength);
			} else {
				// Check against memory data located at "address"
				match = memcmp(fileData, address, checkLength);
			}
			if (!match) {
				// This is the wrong file...
				return null;
			}

			return fileData;
    	}

    	private static boolean isFileMagicValue(int magicValue) {
    		for (int i = 0; i < fileMagics.length; i++) {
    			if (magicValue == fileMagics[i]) {
    				return true;
    			}
    		}

    		return false;
    	}

    	/**
    	 * Search for the first File Magic into a specified memory buffer.
    	 * For performance reason, file magic are checked only at the beginning
    	 * of UMD sectors (i.e. every 2048 bytes).
    	 * 
    	 * @param address the base address where to start searching
    	 * @param size    the length of the memory buffer where to search
    	 * @return        the offset of the first file magic value, relative to
    	 *                the start address, or -1 if no file magic was found.
    	 */
    	private static int getFirstFileMagicOffset(int address, int size) {
    		if (Memory.isAddressGood(address)) {
	    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, size, 4);
	    		final int stepSize = UmdIsoFile.sectorLength;
	    		final int skip = (stepSize / 4) - 1;
	    		for (int i = 0; i < size; i += stepSize) {
	    			int magicValue = memoryReader.readNext();
	    			if (isFileMagicValue(magicValue)) {
	    				return i;
	    			}
	    			memoryReader.skip(skip);
	    		}
    		}

    		return -1;
    	}

    	/**
    	 * Search for all the File Magic into a specified memory buffer.
    	 * For performance reason, file magics are checked only every 16 bytes.
    	 * 
    	 * @param address the base address where to start searching
    	 * @param size    the length of the memory buffer where to search
    	 * @return        the list of offsets of the file magic values found,
    	 *                relative to the start address.
    	 *                Returns null if no file magic was found.
    	 */
    	private static int[] getAllFileMagicOffsets(int address, int size) {
    		if (!Memory.isAddressGood(address)) {
    			return null;
    		}

    		Vector<Integer> magicOffsets = new Vector<Integer>();

    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, size, 4);
    		final int stepSize = 16;
    		final int skip = (stepSize / 4) - 1;
    		final int endSize = (size / stepSize) * stepSize;
    		for (int i = 0; i < endSize; i += stepSize) {
    			int magicValue = memoryReader.readNext();
    			if (isFileMagicValue(magicValue)) {
    				magicOffsets.add(i);
    			}
    			memoryReader.skip(skip);
    		}

    		if (magicOffsets.size() <= 0) {
    			return null;
    		}

    		int[] fileMagicOffsets = new int[magicOffsets.size()];
    		for (int i = 0; i < fileMagicOffsets.length; i++) {
    			fileMagicOffsets[i] = magicOffsets.get(i);
    		}

    		return fileMagicOffsets;
    	}

    	@Override
		public void sceIoRead(int result, int uid, int data_addr, int size,	int bytesRead, long position, SeekableDataInput dataInput) {
			if (result >= 0 && dataInput != null) {
				ReadInfo readInfo = readInfos.get(data_addr);
				boolean processed = false;

				if (scanAllFileMagicOffsets) {
					// Accurate but also time intensive search method
					int[] magicOffsets = getAllFileMagicOffsets(data_addr, bytesRead);
					if (magicOffsets != null && magicOffsets.length > 0) {
						for (int i = 0; i < magicOffsets.length; i++) {
							int magicOffset = magicOffsets[i];
							int nextMagicOffset = i + 1 < magicOffsets.length ? magicOffsets[i + 1] : bytesRead;
							int magicAddress = data_addr + magicOffset;
							readInfo = new ReadInfo(magicAddress, nextMagicOffset - magicOffset, dataInput, position + magicOffset);
							readInfos.put(magicAddress, readInfo);
							readMagics.put(getMagicHash(magicAddress), readInfo);
						}
						processed = true;
					}
				} else {
					// Simple but fast search method
					int magicOffset = getFirstFileMagicOffset(data_addr, bytesRead);
					if (magicOffset >= 0) {
						int magicAddress = data_addr + magicOffset;
						readInfo = new ReadInfo(magicAddress, bytesRead - magicOffset, dataInput, position + magicOffset);
						readInfos.put(magicAddress, readInfo);
						readMagics.put(getMagicHash(magicAddress), readInfo);
						processed = true;
					}
				}

				if (!processed) {
					if (readInfo == null) {
						readInfo = new ReadInfo(data_addr, bytesRead, dataInput, position);
						readInfos.put(data_addr, readInfo);
					}
				}
			}
		}

		@Override
		public void sceIoAssign(int result, int dev1_addr, String dev1, int dev2_addr, String dev2, int dev3_addr, String dev3, int mode, int unk1, int unk2) {
		}

		@Override
		public void sceIoCancel(int result, int uid) {
		}

		@Override
		public void sceIoChdir(int result, int path_addr, String path) {
		}

		@Override
		public void sceIoChstat(int result, int path_addr, String path,	int stat_addr, int bits) {
		}

		@Override
		public void sceIoClose(int result, int uid) {
		}

		@Override
		public void sceIoDclose(int result, int uid) {
		}

		@Override
		public void sceIoDevctl(int result, int device_addr, String device, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen) {
		}

		@Override
		public void sceIoDopen(int result, int path_addr, String path) {
		}

		@Override
		public void sceIoDread(int result, int uid, int dirent_addr) {
		}

		@Override
		public void sceIoGetStat(int result, int path_addr, String path, int stat_addr) {
		}

		@Override
		public void sceIoIoctl(int result, int uid, int cmd, int indata_addr, int inlen, int outdata_addr, int outlen) {
		}

		@Override
		public void sceIoMkdir(int result, int path_addr, String path, int permissions) {
		}

		@Override
		public void sceIoOpen(int result, int filename_addr, String filename, int flags, int permissions, String mode) {
		}

		@Override
		public void sceIoPollAsync(int result, int uid, int res_addr) {
		}

		@Override
		public void sceIoRemove(int result, int path_addr, String path) {
		}

		@Override
		public void sceIoRename(int result, int path_addr, String path, int new_path_addr, String newpath) {
		}

		@Override
		public void sceIoRmdir(int result, int path_addr, String path) {
		}

		@Override
		public void sceIoSeek32(int result, int uid, int offset, int whence) {
		}

		@Override
		public void sceIoSeek64(long result, int uid, long offset, int whence) {
		}

		@Override
		public void sceIoSync(int result, int device_addr, String device, int unknown) {
		}

		@Override
		public void sceIoWaitAsync(int result, int uid, int res_addr) {
		}

		@Override
		public void sceIoWrite(int result, int uid, int data_addr, int size, int bytesWritten) {
		}
    }
}
