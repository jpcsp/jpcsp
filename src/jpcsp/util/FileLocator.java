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
package jpcsp.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.VFS.ByteArrayVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.PartialVirtualFile;
import jpcsp.HLE.VFS.iso.UmdIsoVirtualFile;
import jpcsp.HLE.modules.sceAtrac3plus;
import jpcsp.HLE.modules.sceMpeg;
import jpcsp.HLE.modules150.IoFileMgrForUser.IIoListener;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class FileLocator {
	private static Logger log = Logger.getLogger("fileLocator");
	private static FileLocator instance;
    private static boolean scanAllFileMagicOffsets = true;
    private IoListener ioListener;

	public static FileLocator getInstance() {
		if (instance == null) {
			instance = new FileLocator();
		}
		return instance;
	}

	private FileLocator() {
		ioListener = new IoListener();
		Modules.IoFileMgrForUserModule.registerIoListener(ioListener);
	}

    public void setFileData(SeekableDataInput dataInput, IVirtualFile vFile, int address, long startPosition, int length) {
    	ioListener.setFileData(dataInput, vFile, address, startPosition, length);
    }

    public IVirtualFile getVirtualFile(int address, int length, int fileSize, byte[] checkData) {
    	return ioListener.getVirtualFile(address, length, fileSize, checkData);
    }

    private static class IoListener implements IIoListener {
    	private static class ReadInfo {
    		public int address;
    		public int size;
    		public SeekableDataInput dataInput;
    		public IVirtualFile vFile;
    		public long position;

    		public ReadInfo(int address, int size, SeekableDataInput dataInput, IVirtualFile vFile, long position) {
    			this.address = address;
    			this.size = size;
    			this.dataInput = dataInput;
    			this.vFile = vFile;
    			this.position = position;
    		}

			@Override
			public String toString() {
				return String.format("ReadInfo(0x%08X-0x%08X(size=0x%X), position=%d, %s)", address, address + size, size, position, dataInput.toString());
			}
    	}

    	private HashMap<Integer, ReadInfo> readInfos;
    	private HashMap<Integer, ReadInfo> readMagics;
    	private static final int MAGIC_HASH_LENGTH = sceAtrac3plus.ATRAC_HEADER_HASH_LENGTH;
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

    	private byte[] readData(ReadInfo readInfo, int positionOffset, int length) {
    		byte[] fileData;
    		try {
        		fileData = new byte[length];
        		if (readInfo.vFile != null) {
					long currentPosition = readInfo.vFile.getPosition();
					if (currentPosition < 0) {
						// File is already closed
						return null;
					}
					readInfo.vFile.ioLseek(readInfo.position + positionOffset);
		    		readInfo.vFile.ioRead(fileData, 0, fileData.length);
					readInfo.vFile.ioLseek(currentPosition);
        		} else if (readInfo.dataInput != null) {
					long currentPosition = readInfo.dataInput.getFilePointer();
					if (currentPosition < 0) {
						// File is already closed
						return null;
					}
					readInfo.dataInput.seek(readInfo.position + positionOffset);
		    		readInfo.dataInput.readFully(fileData);
					readInfo.dataInput.seek(currentPosition);
        		} else {
        			return null;
        		}
			} catch (IOException e) {
				return null;
			}

    		return fileData;
    	}

    	public IVirtualFile getVirtualFile(int address, int length, int fileSize, byte[] checkData) {
    		int positionOffset = 0;
    		ReadInfo readInfo = readInfos.get(address);
    		if (readInfo == null) {
    			// The file data has not been read at this address.
    			// Search for files having the same size and content
    			for (ReadInfo ri : readInfos.values()) {
    				try {
    					if (ri.vFile != null && ri.vFile.length() == fileSize) {
							// Both file have the same length, check the content
							byte[] fileData = new byte[length];
							long currentPosition = ri.vFile.getPosition();
							ri.vFile.ioLseek(ri.position);
							ri.vFile.ioRead(fileData, 0, length);
							ri.vFile.ioLseek(currentPosition);
							if (memcmp(fileData, address, length)) {
								// Both files have the same content, we have found it!
								readInfo = ri;
								break;
							}
    					} else if (ri.dataInput != null && ri.dataInput.length() == fileSize) {
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
    						if (ri.vFile != null && ri.vFile.length() >= fileSize) {
    							// Check if the file contents are matching our buffer
    							int checkLength = Math.min(length, fileSize);
    							byte[] fileData = new byte[checkLength];
    							long currentPosition = ri.vFile.getPosition();
    							ri.vFile.ioLseek(ri.position);
    							ri.vFile.ioRead(fileData, 0, checkLength);
    							ri.vFile.ioLseek(currentPosition);

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
    						} else if (ri.dataInput != null && ri.dataInput.length() >= fileSize) {
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

			int checkLength = Math.min(length, MAGIC_HASH_LENGTH);
    		byte[] fileData = readData(readInfo, positionOffset, checkLength);
    		if (fileData == null) {
    			// Could not read the file data...
    			return null;
    		}

			// Check if the file data is really matching the data in memory
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

			if (readInfo.vFile != null) {
				IVirtualFile vFile = readInfo.vFile.duplicate();
				if (vFile == null) {
					vFile = readInfo.vFile;
				} else {
					vFile.ioLseek(readInfo.position);
				}

				if (fileSize > vFile.length()) {
					if (vFile instanceof UmdIsoVirtualFile) {
						// Extend the UMD file to at least the requested file size
						UmdIsoVirtualFile umdIsoVirtualFile = (UmdIsoVirtualFile) vFile;
						umdIsoVirtualFile.setLength(fileSize);
					}
				}

				if (readInfo.position + positionOffset != 0 || vFile.length() != fileSize) {
					vFile = new PartialVirtualFile(vFile, readInfo.position + positionOffset, fileSize);
				}
				return vFile;
			}
			if (readInfo.dataInput != null) {
				if (readInfo.dataInput instanceof UmdIsoFile) {
					UmdIsoFile umdIsoFile = (UmdIsoFile) readInfo.dataInput;
					try {
						UmdIsoFile duplicate = umdIsoFile.duplicate();
						if (duplicate != null) {
							duplicate.seek(readInfo.position);
							umdIsoFile = duplicate;
						}
					} catch (IOException e) {
						log.warn("Cannot duplicate UmdIsoFile", e);
					}

					if (fileSize > umdIsoFile.length()) {
						// Extend the UMD file to at least the requested file size
						umdIsoFile.setLength(fileSize);
					}

					IVirtualFile vFile = new UmdIsoVirtualFile(umdIsoFile);
					if (readInfo.position + positionOffset != 0 || vFile.length() != fileSize) {
						vFile = new PartialVirtualFile(vFile, readInfo.position + positionOffset, fileSize);
					}
					return vFile;
				}
			}

			try {
				fileData = readData(readInfo, positionOffset, fileSize);
			} catch (OutOfMemoryError e) {
				log.error(String.format("Error '%s' while decoding external audio file (fileSize=%d, position=%d, dataInput=%s)", e.toString(), fileSize, readInfo.position + positionOffset, readInfo.dataInput.toString()));
				return null;
			}

			if (fileData == null) {
				return null;
			}

			return new ByteArrayVirtualFile(fileData, 0, fileData.length);
    	}

    	public void setFileData(SeekableDataInput dataInput, IVirtualFile vFile, int address, long startPosition, int length) {
        	ReadInfo ri = new ReadInfo(address, length, dataInput, vFile, startPosition);
        	readInfos.put(address, ri);
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
		public void sceIoRead(int result, int uid, int data_addr, int size,	int bytesRead, long position, SeekableDataInput dataInput, IVirtualFile vFile) {
			if (result >= 0 && (dataInput != null || vFile != null)) {
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
							readInfo = new ReadInfo(magicAddress, nextMagicOffset - magicOffset, dataInput, vFile, position + magicOffset);
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
						readInfo = new ReadInfo(magicAddress, bytesRead - magicOffset, dataInput, vFile, position + magicOffset);
						readInfos.put(magicAddress, readInfo);
						readMagics.put(getMagicHash(magicAddress), readInfo);
						processed = true;
					}
				}

				if (!processed) {
					if (readInfo == null) {
						readInfo = new ReadInfo(data_addr, bytesRead, dataInput, vFile, position);
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
