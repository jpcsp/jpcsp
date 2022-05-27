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
package jpcsp.HLE.VFS.fat;

import static jpcsp.HLE.VFS.fat.FatBuilder.bootSectorNumber;
import static jpcsp.HLE.VFS.fat.FatBuilder.directoryTableEntrySize;
import static jpcsp.HLE.VFS.fat.FatUtils.readSectorInt16;
import static jpcsp.HLE.VFS.fat.FatUtils.readSectorInt32;
import static jpcsp.HLE.VFS.fat.FatUtils.readSectorInt8;
import static jpcsp.HLE.VFS.fat.FatVirtualFile.firstClusterNumber;
import static jpcsp.HLE.VFS.fat.FatVirtualFile.isLongFileNameDirectoryEntry;
import static jpcsp.HLE.VFS.fat.FatVirtualFile.sectorSize;
import static jpcsp.hardware.Nand.pageSize;

import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFileSystem;
import jpcsp.HLE.VFS.IVirtualCache;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.util.Utilities;

// See format description: https://en.wikipedia.org/wiki/Design_of_the_FAT_file_system
public class FatVirtualFileSystem extends AbstractVirtualFileSystem implements IVirtualCache {
	private static Logger log = FatVirtualFile.log;
	private final String deviceName;
	private final IVirtualFile vFile;
	private final byte[] currentSector = new byte[sectorSize];
	private int[] fatClusterMap;
	private int sectorsPerCluster;
	private int rootDirectoryClusterNumber;
	private int firstDataClusterSectorNumber;
	private int fatEOC; // Last cluster in file (EOC)
	private int clusterMask;

	private class FatVirtualFileInstance implements IVirtualFile {
		private final long length;
		private final int[] clusters;
		private long position;

		public FatVirtualFileInstance(FatFileInfo fatFileInfo) {
			length = fatFileInfo.getFileSize();
			clusters = fatFileInfo.getClusters();
		}

		@Override
		public int ioClose() {
			return 0;
		}

		private int getSectorNumberFromPosition() {
			int clusterIndex = (int) (position / (sectorSize * sectorsPerCluster));
			int clusterNumber = clusters[clusterIndex];
			int sectorNumber = getSectorNumber(clusterNumber);
			sectorNumber += (position / sectorSize) % sectorsPerCluster;

			return sectorNumber;
		}

		private int getSectorOffset() {
			return (int) (position % sectorSize);
		}

		@Override
		public int ioRead(TPointer outputPointer, int outputLength) {
			int readLength = 0;
			int outputOffset = 0;
			while (outputLength > 0) {
				int sectorNumber = getSectorNumberFromPosition();
				readSector(sectorNumber);
				int sectorOffset = getSectorOffset();
				int sectorLength = pageSize - sectorOffset;
				int length = Math.min(sectorLength, outputLength);

				outputPointer.setArray(outputOffset, currentSector, sectorOffset, length);

				outputLength -= length;
				outputOffset += length;
				position += length;
				readLength += length;
			}

			return readLength;
		}

		@Override
		public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
			int readLength = 0;
			while (outputLength > 0) {
				int sectorNumber = getSectorNumberFromPosition();
				readSector(sectorNumber);
				int sectorOffset = getSectorOffset();
				int sectorLength = pageSize - sectorOffset;
				int length = Math.min(sectorLength, outputLength);

				System.arraycopy(currentSector, sectorOffset, outputBuffer, outputOffset, length);

				outputLength -= length;
				outputOffset += length;
				position += length;
				readLength += length;
			}

			return readLength;
		}

		@Override
		public int ioWrite(TPointer inputPointer, int inputLength) {
			return IO_ERROR;
		}

		@Override
		public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
			return IO_ERROR;
		}

		@Override
		public long ioLseek(long offset) {
			position = offset;
			return position;
		}

		@Override
		public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
			return IO_ERROR;
		}

		@Override
		public long length() {
			return length;
		}

		@Override
		public boolean isSectorBlockMode() {
			return false;
		}

		@Override
		public long getPosition() {
			return position;
		}

		@Override
		public IVirtualFile duplicate() {
			return null;
		}

		@Override
		public Map<IoOperation, IoOperationTiming> getTimings() {
			return null;
		}
	}

	public FatVirtualFileSystem(String deviceName, IVirtualFile vFile) {
		this.deviceName = deviceName;
		this.vFile = vFile;

		init();
	}

	private void init() {
		boolean isFat32 = false;
		boolean isFat16 = false;
		boolean isFat12 = false;

		readSector(bootSectorNumber);

    	// Sectors per cluster
		sectorsPerCluster = readSectorInt8(currentSector, 13);

    	// Reserved sectors
		int reservedSectors = readSectorInt16(currentSector, 14);

    	// Number of File Allocation Tables (FATs)
		int numberOfFats = readSectorInt8(currentSector, 16);

    	// Max entries in root dir
		int numberOfRootDirectoryEntries = readSectorInt16(currentSector, 17);

		// Total sectors
		int totalSectors = readSectorInt16(currentSector, 19);
		if (totalSectors == 0) {
    		// If 0, use value at offset 32
			totalSectors = readSectorInt32(currentSector, 32);
		}

    	// Count of sectors used by the FAT table
		int fatSectors = readSectorInt16(currentSector, 22);

		if (fatSectors == 0) {
			isFat32 = true;
			fatSectors = readSectorInt32(currentSector, 36);
		}

		// The first data cluster is starting after the root directory
		int firstDataClusterOffset = (numberOfRootDirectoryEntries << 5) / sectorSize;
		int usedSectors = reservedSectors + fatSectors * numberOfFats + firstDataClusterOffset;
		int maxNumberClusters = (totalSectors - usedSectors) / sectorsPerCluster;

		if (!isFat32) {
			isFat16 = maxNumberClusters > 0xFF4;
			isFat12 = !isFat16;
		}

		// Read the whole FAT table
		int fatSectorNumber = bootSectorNumber + reservedSectors;
		byte[] fatTable = new byte[fatSectors * sectorSize];
		readSectors(fatSectorNumber, fatTable, 0, fatTable.length);

		// Decode the FAT table
		fatClusterMap = new int[maxNumberClusters];
		if (isFat32) {
			fatEOC = 0x0FFFFFFF;
			clusterMask = 0x0FFFFFFF;
			for (int i = 0, n = 0; n < fatClusterMap.length; i += 4) {
				int fatEntry = readSectorInt32(fatTable, i);
				fatClusterMap[n++] = fatEntry;
			}
		} else if (isFat16) {
			fatEOC = 0xFFF8;
			clusterMask = 0x0000FFFF;
			for (int i = 0, n = 0; n < fatClusterMap.length; i += 2) {
				int fatEntry = readSectorInt16(fatTable, i);
				fatClusterMap[n++] = fatEntry;
			}
		} else if (isFat12) {
			fatEOC = 0xFF8;
			clusterMask = 0x00000FFF;
			for (int i = 0, n = 0; n < fatClusterMap.length; i += 3) {
				int fatEntry0 = readSectorInt8(fatTable, i) | ((readSectorInt8(fatTable, i + 1) & 0x0F) << 8);
				fatClusterMap[n++] = fatEntry0;

				int fatEntry1 = (readSectorInt8(fatTable, i + 1) >> 4) | (readSectorInt8(fatTable, i + 2) << 4);
				if (n < fatClusterMap.length) {
					fatClusterMap[n++] = fatEntry1;
				}
			}
		} else {
			log.error(String.format("Unknown FAT type"));
		}

		int rootDirectorySectorNumber = fatSectorNumber + numberOfFats * fatSectors;
		firstDataClusterSectorNumber = rootDirectorySectorNumber + firstDataClusterOffset;
		if (isFat32) {
	    	// Cluster number of root directory start
			rootDirectoryClusterNumber = readSectorInt32(currentSector, 44);
		} else {
			rootDirectoryClusterNumber = 1;
		}
	}

	private boolean isEndOfClusterChain(int clusterNumber) {
		return (clusterNumber & fatEOC) == fatEOC;
	}

	private void readSectors(int sectorNumber, byte[] buffer, int offset, int length) {
		int readLength;
		// synchronize the vFile to make sure that the ioLseek/ioRead combination is atomic
		synchronized (vFile) {
			vFile.ioLseek(sectorNumber * (long) sectorSize);
			readLength = vFile.ioRead(buffer, offset, length);
		}
		if (readLength != length) {
			log.error(String.format("FatVirtualFileSystem.readSectors cannot read sectors sectorNumber=0x%X, length=0x%X, readLength=0x%X", sectorNumber, length, readLength));

			if (readLength < 0) {
				readLength = 0;
			}
			Arrays.fill(buffer, offset + readLength, offset + length - readLength, (byte) 0);
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("readSectors sectorNumber=0x%X: %s", sectorNumber, Utilities.getMemoryDump(buffer, offset, readLength)));
		}
	}

	private void readSector(int sectorNumber) {
		readSectors(sectorNumber, currentSector, 0, sectorSize);
	}

	private int getSectorNumber(int clusterNumber) {
		return (clusterNumber - firstClusterNumber) * sectorsPerCluster + firstDataClusterSectorNumber;
	}

	private boolean isDataClusterNumber(int clusterNumber) {
		clusterNumber &= clusterMask;
		return clusterNumber >= 2 && clusterNumber <= (0x0FFFFFEF & clusterMask);
	}

	private int[] getClusters(int clusterNumber) {
		int[] clusters = new int[] { clusterNumber };

		while (clusterNumber >= 0 && clusterNumber < fatClusterMap.length) {
			int nextCluster = fatClusterMap[clusterNumber];
			if (!isDataClusterNumber(nextCluster)) {
				break;
			}

			// Add the nextCluster to the clusters array
			clusters = Utilities.extendArray(clusters, 1);
			clusterNumber = nextCluster & clusterMask;
			clusters[clusters.length - 1] = clusterNumber;
		}

		return clusters;
	}

	public FatFileInfo[] getDirectoryEntries(int clusterNumber) {
		FatFileInfo[] entries = null;
		byte[] lfn = null;
		boolean end = false;

		if (log.isDebugEnabled()) {
			log.debug(String.format("getDirectoryEntries clusterNumber=0x%X", clusterNumber));
		}

		while (!end && !isEndOfClusterChain(clusterNumber)) {
			int sectorNumber = getSectorNumber(clusterNumber);
			for (int i = 0; !end && i < sectorsPerCluster; i++) {
				readSector(sectorNumber + i);

				for (int offset = 0; offset < sectorSize; offset += directoryTableEntrySize) {
					if (log.isTraceEnabled()) {
						log.trace(String.format("getDirectoryEntries sectorNumber=0x%X, offset=0x%X, %s", sectorNumber, offset, Utilities.getMemoryDump(currentSector, offset, directoryTableEntrySize)));
					}

					int firstByte = readSectorInt8(currentSector, offset + 0);
					if (firstByte == 0x00) {
						// End marker
						end = true;
						break;
					}
					if (firstByte == 0xE5) {
						// Deleted entry
						continue;
					}

					if (isLongFileNameDirectoryEntry(currentSector, offset)) {
						lfn = Utilities.extendArray(lfn, currentSector, offset, directoryTableEntrySize);
					} else {
						String entryName = FatVirtualFile.getFileName(currentSector, offset, lfn);
						lfn = null;

						// Ignore "." and ".." entries
						if (!".".equals(entryName) && !"..".equals(entryName)) {
							String fileName83 = new String(currentSector, offset + 0, 8 + 3);
							int fileAttributes = readSectorInt8(currentSector, offset + 11);
							int entryClusterNumber = readSectorInt16(currentSector, offset + 20) << 16;
							entryClusterNumber |= readSectorInt16(currentSector, offset + 26);
							long fileSize = readSectorInt32(currentSector, offset + 28) & 0xFFFFFFFFL;
							int time = readSectorInt16(currentSector, offset + 22);
							time |= readSectorInt16(currentSector, offset + 24) << 16;
							ScePspDateTime lastModified = ScePspDateTime.fromMSDOSTime(time);

							FatFileInfo fatFileInfo = new FatFileInfo();
							fatFileInfo.setFileName(entryName);
							fatFileInfo.setReadOnly((fileAttributes & 0x01) != 0);
							fatFileInfo.setDirectory((fileAttributes & 0x10) != 0);
							fatFileInfo.setFileSize(fileSize);
							fatFileInfo.setLastModified(lastModified);
							fatFileInfo.setFileName83(fileName83);
							if (entryClusterNumber != 0) {
								fatFileInfo.setClusters(getClusters(entryClusterNumber));
							}
							if (log.isTraceEnabled()) {
								log.trace(String.format("Found directoryEntry %s", fatFileInfo));
							}

							entries = Utilities.add(entries, fatFileInfo);
						}
					}
				}
			}

			clusterNumber = fatClusterMap[clusterNumber];

			if (log.isDebugEnabled()) {
				log.debug(String.format("getDirectoryEntries next clusterNumber=0x%X", clusterNumber));
			}
		}

		return entries;
	}

	private FatFileInfo getFatFileInfo(int clusterNumber, String fileName) {
		FatFileInfo entries[] = getDirectoryEntries(clusterNumber);
		if (entries != null) {
			for (FatFileInfo entry : entries) {
				if (fileName.equals(entry.getFileName())) {
					return entry;
				}
			}
		}

		// Not found
		return null;
	}

	private FatFileInfo getFatFileInfo(String fileName) {
		if (fileName == null || fileName.length() == 0) {
			return null;
		}

		int clusterNumber = rootDirectoryClusterNumber;
		while (true) {
			int directorySeparator = fileName.indexOf('/');
			if (directorySeparator < 0) {
				FatFileInfo fatFileInfo = getFatFileInfo(clusterNumber, fileName);
				if (fatFileInfo == null) {
					break;
				}
				return fatFileInfo;
			}

			String directoryName = fileName.substring(0, directorySeparator);
			FatFileInfo fatFileInfo = getFatFileInfo(clusterNumber, directoryName);
			if (fatFileInfo == null) {
				break;
			}

			clusterNumber = fatFileInfo.getFirstCluster();
			fileName = fileName.substring(directorySeparator + 1);
		}

		// Not found
		return null;
	}

	private int getFirstClusterNumber(String fileName) {
		if (fileName == null || fileName.length() == 0) {
			return rootDirectoryClusterNumber;
		}

		FatFileInfo fatFileInfo = getFatFileInfo(fileName);
		if (fatFileInfo == null) {
			// Not found
			return -1;
		}

		return fatFileInfo.getFirstCluster();
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		if (flags != IoFileMgrForUser.PSP_O_RDONLY) {
			log.error(String.format("FatVirtualFileSystem.ioOpen unimplemented fileName='%s', flags=0x%X", fileName, flags));
			return null;
		}

		FatFileInfo fatFileInfo = getFatFileInfo(fileName);
		if (fatFileInfo == null) {
			return null;
		}

		IVirtualFile vFile = new FatVirtualFileInstance(fatFileInfo);

		return vFile;
	}

	@Override
	public String[] ioDopen(String dirName) {
		int clusterNumber = getFirstClusterNumber(dirName);
		if (clusterNumber < 0) {
			return null;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("ioDopen dirName='%s', clusterNumber=0x%X", dirName, clusterNumber));
		}

		FatFileInfo entries[] = getDirectoryEntries(clusterNumber);
		if (entries == null) {
			return null;
		}

		String[] fileNames = new String[entries.length];
		for (int i = 0; i < entries.length; i++) {
			fileNames[i] = entries[i].getFileName();
		}

		return fileNames;
	}

	@Override
	public int ioDclose(String dirName) {
		return 0;
	}

	@Override
	public int ioDread(String dirName, SceIoDirent dir) {
		int clusterNumber = getFirstClusterNumber(dirName);
		if (clusterNumber < 0) {
			return IO_ERROR;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("ioDread dirName='%s', fileName='%s', clusterNumber=0x%X", dirName, dir.filename, clusterNumber));
		}

		FatFileInfo fatFileInfo = getFatFileInfo(clusterNumber, dir.filename);
		if (fatFileInfo == null) {
			return IO_ERROR;
		}

		int attr = fatFileInfo.isDirectory() ? 0x10 : 0x20;
		int mode = fatFileInfo.isReadOnly() ? 0555 : 0777;
        mode |= attr << 8;

        dir.stat.mode = mode;
		dir.stat.attr = attr;
		dir.stat.size = fatFileInfo.getFileSize();
		dir.stat.ctime = fatFileInfo.getLastModified();
		dir.stat.atime = ScePspDateTime.fromUnixTime(0L);
		dir.stat.mtime = fatFileInfo.getLastModified();

		// Success is 1 for sceIoDread
		return 1;
	}

	@Override
	public int ioGetstat(String fileName, SceIoStat stat) {
		log.error(String.format("FatVirtualFileSystem.ioGetstat unimplemented fileName='%s'", fileName));
		return IO_ERROR;
	}

	@Override
	public void invalidateCachedData() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("invalidateCachedData for %s", this));
		}

		init();

		if (vFile instanceof IVirtualCache) {
			((IVirtualCache) vFile).invalidateCachedData();
		}
	}

    @Override
	public void flushCachedData() {
		if (vFile instanceof IVirtualCache) {
			((IVirtualCache) vFile).flushCachedData();
		}
	}

    @Override
	public void closeCachedFiles() {
		if (vFile instanceof IVirtualCache) {
			((IVirtualCache) vFile).closeCachedFiles();
		}
	}

    public int[] getFatClusterMap() {
    	return fatClusterMap;
    }

    public int getRootDirectoryClusterNumber() {
    	return rootDirectoryClusterNumber;
    }

    @Override
	public String toString() {
    	return String.format("%s%s", deviceName == null ? "" : deviceName, vFile);
	}
}
