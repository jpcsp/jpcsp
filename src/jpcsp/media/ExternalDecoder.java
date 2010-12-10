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
import java.util.HashMap;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceAtrac3plus;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.HLE.modules150.IoFileMgrForUser.IIoListener;
import jpcsp.connector.AtracCodec;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

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

    public ExternalDecoder() {
    	init();
    }

    public static void setEnabled(boolean flag) {
    	enabled = flag;
    }

    private static void init() {
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

    private boolean executeExternalDecoder(String inputFile, String outputFile) {
		String[] cmd;
		if (extAudioDecoder.toString().endsWith(".bat")) {
			cmd = new String[] {
					"cmd",
					"/C",
					extAudioDecoder.toString(),
					inputFile,
					outputFile };
		} else {
			cmd = new String[] {
					extAudioDecoder.toString(),
					inputFile,
					outputFile };
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

    public void decodeExtAudio(byte[] mpegData, int mpegFileSize, int mpegOffset) {
    	if (dumpPmfFile) {
			try {
				new File(MediaEngine.getExtAudioBasePath(mpegFileSize)).mkdirs();
				FileOutputStream pmfOut = new FileOutputStream(MediaEngine.getExtAudioPath(mpegFileSize, "pmf"));
				pmfOut.write(mpegData);
				pmfOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}

    	MpegDemux mpegDemux = new MpegDemux(mpegData, mpegOffset);
		mpegDemux.demux(false, true);

		ByteBuffer audioStream = mpegDemux.getAudioStream();
		if (audioStream != null) {
			if (dumpAudioStreamFile) {
				try {
					new File(MediaEngine.getExtAudioBasePath(mpegFileSize)).mkdirs();
					FileOutputStream pmfOut = new FileOutputStream(MediaEngine.getExtAudioPath(mpegFileSize, "audio"));
					pmfOut.getChannel().write(audioStream);
					audioStream.rewind();
					pmfOut.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			ByteBuffer omaBuffer = OMAFormat.convertStreamToOMA(audioStream); 
			if (omaBuffer != null) {
				try {
					new File(MediaEngine.getExtAudioBasePath(mpegFileSize)).mkdirs();
					String encodedFile = MediaEngine.getExtAudioPath(mpegFileSize, "oma");
					FileOutputStream os = new FileOutputStream(encodedFile);
					os.getChannel().write(omaBuffer);
					os.close();
	
					String decodedFile = MediaEngine.getExtAudioPath(mpegFileSize, "wav");
					if (!executeExternalDecoder(encodedFile, decodedFile)) {
						new File(encodedFile).delete();
					}
				} catch (IOException e) {
					// Ignore Exception
					log.error(e);
				}
			}
		}
    }

    public void decodeExtAudio(int address, int mpegFileSize, int mpegOffset) {
    	if (!isEnabled()) {
    		return;
    	}

    	// At least 2048 bytes of MPEG data is provided
		byte[] mpegData = ioListener.readFileData(address, 2048, mpegFileSize);
    	if (mpegData == null) {
    		// MPEG data cannot be retrieved...
    		return;
    	}

    	decodeExtAudio(mpegData, mpegFileSize, mpegOffset);
    }

    private static String getAtracAudioPath(int address, int atracFileSize, String suffix) {
    	return String.format("%sAtrac-%08X-%08X.%s", AtracCodec.getBaseDirectory(), atracFileSize, address, suffix);
    }

    public String decodeAtrac(int address, int length, int atracFileSize) {
    	if (!isEnabled()) {
    		return null;
    	}

		String decodedFileName = getAtracAudioPath(address, atracFileSize, "wav");
		File decodedFile = new File(decodedFileName);
		if (decodedFile.canRead() && decodedFile.length() > 0) {
			// Already decoded
			return decodedFileName;
		}

		byte[] atracData = ioListener.readFileData(address, length, atracFileSize);
    	if (atracData == null) {
    		// Atrac data cannot be retrieved...
    		return null;
    	}

    	try {
	    	ByteBuffer riffBuffer = ByteBuffer.wrap(atracData);
	    	if (dumpEncodedFile) {
	    		// For debugging purpose, optionally dump the original atrac file in RIFF format
	    		FileOutputStream encodedOut = new FileOutputStream(getAtracAudioPath(address, atracFileSize, "encoded"));
	    		encodedOut.getChannel().write(riffBuffer);
	    		encodedOut.close();
	    		riffBuffer.rewind();
	    	}
	    	ByteBuffer omaBuffer = OMAFormat.convertRIFFtoOMA(riffBuffer);
	    	if (omaBuffer == null) {
	    		return null;
	    	}

			new File(AtracCodec.getBaseDirectory()).mkdirs();
			String encodedFileName = getAtracAudioPath(address, atracFileSize, "oma");
			File encodedFile = new File(encodedFileName);
			FileOutputStream os = new FileOutputStream(encodedFileName);
			os.getChannel().write(omaBuffer);
			os.close();

			if (!executeExternalDecoder(encodedFileName, decodedFileName)) {
				encodedFile.delete();
				return null;
			}

			if (decodedFile.canRead() && decodedFile.length() == 0) {
				// Only an empty file has been generated, the file could not be converted
				decodedFile.delete();
				encodedFile.delete();
				return null;
			}
    	} catch (IOException e) {
			// Ignore Exception
			log.error(e);
		}

		return decodedFileName;
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
    	private static final int[] fileMagics = {
    		sceAtrac3plus.RIFF_MAGIC,
    		sceMpeg.PSMF_MAGIC
    	};

    	public IoListener() {
    		readInfos = new HashMap<Integer, ReadInfo>();
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

    	public byte[] readFileData(int address, int length, int fileSize) {
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
						}
					} catch (IOException e) {
						// Ignore the exception
					}
    			}

    			if (readInfo == null) {
    				return null;
    			}
    		}

    		byte[] fileData = new byte[fileSize];
    		try {
				long currentPosition = readInfo.dataInput.getFilePointer();
				readInfo.dataInput.seek(readInfo.position);
	    		readInfo.dataInput.readFully(fileData);
				readInfo.dataInput.seek(currentPosition);
			} catch (IOException e) {
				return null;
			}

			return fileData;
    	}

    	private boolean isFileMagic(int address) {
    		if (!Memory.isAddressGood(address)) {
    			return false;
    		}

    		int magicValue = Memory.getInstance().read32(address);
    		for (int i = 0; i < fileMagics.length; i++) {
    			if (magicValue == fileMagics[i]) {
    				return true;
    			}
    		}

    		return false;
    	}

    	@Override
		public void sceIoRead(int result, int uid, int data_addr, int size,	int bytesRead, long position, SeekableDataInput dataInput) {
			if (result >= 0 && dataInput != null) {
				ReadInfo readInfo = readInfos.get(data_addr);
				if (readInfo == null || isFileMagic(data_addr)) {
					readInfo = new ReadInfo(data_addr, bytesRead, dataInput, position);
					readInfos.put(data_addr, readInfo);
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
