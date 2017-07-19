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
package jpcsp.HLE.VFS.fat32;

import static jpcsp.HLE.VFS.AbstractVirtualFileSystem.IO_ERROR;
import static jpcsp.HLE.VFS.fat32.Fat32Builder.bootSectorNumber;
import static jpcsp.HLE.VFS.fat32.Fat32Builder.directoryTableEntrySize;
import static jpcsp.HLE.VFS.fat32.Fat32Builder.fatSectorNumber;
import static jpcsp.HLE.VFS.fat32.Fat32Builder.firstClusterNumber;
import static jpcsp.HLE.VFS.fat32.Fat32Builder.fsInfoSectorNumber;
import static jpcsp.HLE.VFS.fat32.Fat32Builder.numberOfFats;
import static jpcsp.HLE.VFS.fat32.Fat32Builder.reservedSectors;
import static jpcsp.HLE.VFS.fat32.Fat32Builder.sectorsPerCluster;
import static jpcsp.HLE.VFS.fat32.Fat32Utils.getSectorNumber;
import static jpcsp.HLE.VFS.fat32.Fat32Utils.getSectorOffset;
import static jpcsp.HLE.VFS.fat32.Fat32Utils.readSectorInt16;
import static jpcsp.HLE.VFS.fat32.Fat32Utils.readSectorInt32;
import static jpcsp.HLE.VFS.fat32.Fat32Utils.readSectorString;
import static jpcsp.HLE.VFS.fat32.Fat32Utils.storeSectorInt16;
import static jpcsp.HLE.VFS.fat32.Fat32Utils.storeSectorInt32;
import static jpcsp.HLE.VFS.fat32.Fat32Utils.storeSectorInt8;
import static jpcsp.HLE.VFS.fat32.Fat32Utils.storeSectorString;
import static jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure.charset16;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.hardware.MemoryStick;
import jpcsp.util.Utilities;

// See format description: https://en.wikipedia.org/wiki/Design_of_the_FAT_file_system
public class Fat32VirtualFile implements IVirtualFile {
	public static Logger log = Logger.getLogger("fat32");
    public final static int sectorSize = 512;
    public final static int CLUSTER_MASK = 0x0FFFFFFF;
    private final byte[] currentSector = new byte[sectorSize];
    private static final byte[] emptySector = new byte[sectorSize];
	private IVirtualFileSystem vfs;
	private long position;
    private int totalSectors;
    private int fatSectors;
    private int[] fatClusterMap;
    private Fat32FileInfo[] fatFileInfoMap;
    private byte[] pendingCreateDirectoryEntryLFN;
    private Map<Integer, byte[]> pendingWriteSectors = new HashMap<Integer, byte[]>();
    private Map<Integer, Fat32FileInfo> pendingDeleteFiles = new HashMap<Integer, Fat32FileInfo>();
    private Fat32Builder builder;

	public Fat32VirtualFile(IVirtualFileSystem vfs) {
		this.vfs = vfs;

		totalSectors = (int) (MemoryStick.getTotalSize() / sectorSize);
		fatSectors = getFatSectors(totalSectors, sectorsPerCluster);

		if (log.isDebugEnabled()) {
			log.debug(String.format("totalSectors=0x%X, fatSectors=0x%X", totalSectors, fatSectors));
		}

		// Allocate the FAT cluster map
		fatClusterMap = new int[fatSectors * (sectorSize / 4)];
		// First 2 special entries in the cluster map
		fatClusterMap[0] = 0x0FFFFFF8; // 0xF8 is matching the boot sector Media type field
		fatClusterMap[1] = 0x0FFFFFFF;

		// Allocate the FAT file info map
		fatFileInfoMap = new Fat32FileInfo[fatClusterMap.length];

		builder = new Fat32Builder(this, vfs);
	}

	public void scan() {
		builder.scan();
	}

	public void setFatFileInfoMap(int clusterNumber, Fat32FileInfo fileInfo) {
		fatFileInfoMap[clusterNumber] = fileInfo;
	}

	public void setFatClusterMap(int clusterNumber, int value) {
		fatClusterMap[clusterNumber] = value;
	}

	private int getClusterNumber(int sectorNumber) {
		sectorNumber -= fatSectorNumber;
		sectorNumber -= numberOfFats * fatSectors;
		return firstClusterNumber + (sectorNumber / sectorsPerCluster);
	}

	private int getSectorNumberFromCluster(int clusterNumber) {
		int sectorNumber = (clusterNumber - firstClusterNumber) * sectorsPerCluster;
		sectorNumber += fatSectorNumber;
		sectorNumber += numberOfFats * fatSectors;
		return sectorNumber;
	}

	private int getSectorOffsetInCluster(int sectorNumber) {
		sectorNumber -= fatSectorNumber;
		sectorNumber -= numberOfFats * fatSectors;
		return sectorNumber % sectorsPerCluster;
	}

	private static boolean isFreeClusterNumber(int clusterNumber) {
		clusterNumber &= CLUSTER_MASK;
		return clusterNumber == 0;
	}

	private static boolean isDataClusterNumber(int clusterNumber) {
		clusterNumber &= CLUSTER_MASK;
		return clusterNumber >= 2 && clusterNumber <= 0x0FFFFFEF;
	}

	private static boolean isLastClusterNumber(int clusterNumber) {
		clusterNumber &= CLUSTER_MASK;
		return clusterNumber == 0x0FFFFFFF;
	}

	private int getFatSectors(int totalSectors, int sectorsPerCluster) {
    	int totalClusters = (totalSectors / sectorsPerCluster) + 1;
    	int fatSectors = (totalClusters / (sectorSize / 4)) + 1;

    	return fatSectors;
    }

	private void readBootSector() {
		readEmptySector();

		// Jump Code
    	storeSectorInt8(currentSector, 0, 0xEB);
    	storeSectorInt8(currentSector, 1, 0x58);
    	storeSectorInt8(currentSector, 2, 0x90);

    	// OEM Name
    	storeSectorString(currentSector, 3, "", 8);

    	// Bytes per sector
    	storeSectorInt16(currentSector, 11, sectorSize);

    	// Sectors per cluster
    	storeSectorInt8(currentSector, 13, sectorsPerCluster);

    	// Reserved sectors
    	storeSectorInt16(currentSector, 14, reservedSectors);

    	// Number of File Allocation Tables (FATs)
    	storeSectorInt8(currentSector, 16, numberOfFats);

    	// Max entries in root dir (0 for FAT32)
    	storeSectorInt16(currentSector, 17, 0);

    	// Total sectors (use FAT32 count instead)
    	storeSectorInt16(currentSector, 19, 0);

    	// Media type
    	storeSectorInt8(currentSector, 21, 0xF8); // Fixed disk

    	// Count of sectors used by the FAT table (0 for FAT32)
    	storeSectorInt16(currentSector, 22, 0);

    	// Sectors per track (default)
    	storeSectorInt16(currentSector, 24, 0x3F);

    	// Number of heads (default)
    	storeSectorInt16(currentSector, 26, 0xFF);

    	// Count of hidden sectors
    	storeSectorInt32(currentSector, 28, 0);

    	// Total sectors
    	storeSectorInt32(currentSector, 32, totalSectors);

    	// Sectors per FAT
    	final int fatSectors = getFatSectors(totalSectors, sectorsPerCluster);
    	storeSectorInt32(currentSector, 36, fatSectors);

    	// Drive description / mirroring flags
    	storeSectorInt16(currentSector, 40, 0);

    	// Version
    	storeSectorInt16(currentSector, 42, 0);

    	// Cluster number of root directory start
    	final int rootDirFirstCluster = 2;
    	storeSectorInt32(currentSector, 44, rootDirFirstCluster);

    	// Sector number of FS Information Sector
    	storeSectorInt16(currentSector, 48, fsInfoSectorNumber);

    	// First sector number of a copy of the three FAT32 boot sectors, typically 6.
    	storeSectorInt16(currentSector, 50, 6);

    	// Drive number
    	storeSectorInt8(currentSector, 64, 0);

    	// Extended boot signature
    	storeSectorInt8(currentSector, 66, 0x29);

    	// Volume ID
    	storeSectorInt32(currentSector, 67, 0x00000000);

    	// Volume label
    	storeSectorString(currentSector, 71, "", 11);

    	// File system type
    	storeSectorString(currentSector, 82, "FAT32", 8);

    	// Signature
    	storeSectorInt8(currentSector, 510, 0x55);
    	storeSectorInt8(currentSector, 511, 0xAA);
	}

	private void readFsInfoSector() {
		readEmptySector();

    	// FS Information sector signature
		storeSectorInt8(currentSector, 0, 0x52);
		storeSectorInt8(currentSector, 1, 0x52);
		storeSectorInt8(currentSector, 2, 0x61);
		storeSectorInt8(currentSector, 3, 0x41);

    	// FS Information sector signature
		storeSectorInt8(currentSector, 484, 0x72);
		storeSectorInt8(currentSector, 485, 0x72);
		storeSectorInt8(currentSector, 486, 0x41);
		storeSectorInt8(currentSector, 487, 0x61);

    	// Last known number of free data clusters, or 0xFFFFFFFF if unknown
    	storeSectorInt32(currentSector, 488, 0xFFFFFFFF);

    	// Number of the most recently known to be allocated data cluster.
    	// Should be set to 0xFFFFFFFF during format.
    	storeSectorInt32(currentSector, 492, 0xFFFFFFFF);

    	// FS Information sector signature
    	storeSectorInt8(currentSector, 510, 0x55);
    	storeSectorInt8(currentSector, 511, 0xAA);
	}

	private void readFatSector(int fatIndex) {
		readEmptySector();

		int offset = (fatIndex * sectorSize) >> 2;
		for (int i = 0, j = 0; i < sectorSize; i += 4, j++) {
			storeSectorInt32(currentSector, i, fatClusterMap[offset + j]);
		}
	}

	private void readDataSector(int sectorNumber) {
		readEmptySector();

		int clusterNumber = getClusterNumber(sectorNumber);
		int sectorOffsetInCluster = getSectorOffsetInCluster(sectorNumber);
		Fat32FileInfo fileInfo = fatFileInfoMap[clusterNumber];
		if (fileInfo == null) {
			log.warn(String.format("readDataSector unknown sectorNumber=0x%X, clusterNumber=0x%X", sectorNumber, clusterNumber));
			return;
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("readDataSector clusterNumber=0x%X(sector=0x%X), fileInfo=%s", clusterNumber, sectorOffsetInCluster, fileInfo));
		}

		if (fileInfo.isDirectory()) {
			byte[] directoryData = fileInfo.getFileData();
			if (directoryData == null) {
				directoryData = builder.buildDirectoryData(fileInfo);
				fileInfo.setFileData(directoryData);
			}

			int byteOffset = sectorOffsetInCluster * sectorSize;
			if (byteOffset < directoryData.length) {
				int length = Math.min(directoryData.length - byteOffset, sectorSize);
				System.arraycopy(directoryData, byteOffset, currentSector, 0, length);
			}
		} else {
			IVirtualFile vFile = fileInfo.getVirtualFile(vfs);
			if (vFile == null) {
				log.warn(String.format("readDataSector cannot read file '%s'", fileInfo));
				return;
			}

			long byteOffset = sectorOffsetInCluster * (long) sectorSize;
			int[] clusters = fileInfo.getClusters();
			if (clusters != null) {
				for (int i = 0; i < clusters.length; i++) {
					if (clusters[i] == clusterNumber) {
						break;
					}
					byteOffset += sectorsPerCluster * sectorSize;
				}
			}

			if (byteOffset < fileInfo.getFileSize()) {
				if (vFile.ioLseek(byteOffset) != byteOffset) {
					log.warn(String.format("readDataSector cannot seek file '%s' to 0x%X", fileInfo, byteOffset));
					return;
				}

				int length = (int) Math.min(fileInfo.getFileSize() - byteOffset, (long) sectorSize);
				int readLength = vFile.ioRead(currentSector, 0, length);
				if (log.isDebugEnabled()) {
					log.debug(String.format("readDataSector readLength=0x%X", readLength));
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("readDataSector trying to read at offset 0x%X past end of file", byteOffset, fileInfo));
				}
			}
		}
	}

	private void readEmptySector() {
		System.arraycopy(emptySector, 0, currentSector, 0, sectorSize);
	}

	private void readSector(int sectorNumber) {
		byte[] pendingWriteSector = pendingWriteSectors.get(sectorNumber);
		if (pendingWriteSector != null) {
			System.arraycopy(pendingWriteSector, 0, currentSector, 0, sectorSize);
			return;
		}

		if (sectorNumber == bootSectorNumber) {
			readBootSector();
		} else if (sectorNumber == fsInfoSectorNumber) {
			readFsInfoSector();
		} else if (sectorNumber < fatSectorNumber) {
			readEmptySector();
		} else if (sectorNumber >= fatSectorNumber && sectorNumber < fatSectorNumber + fatSectors) {
			readFatSector(sectorNumber - fatSectorNumber);
		} else {
			readDataSector(sectorNumber);
		}
	}

	private void writeBootSector() {
		log.warn(String.format("Writing to the MemoryStick boot sector!"));
		writeEmptySector();
	}

	private void writeFsInfoSector() {
		log.warn(String.format("Writing to the MemoryStick FsInfo sector!"));
		writeEmptySector();
	}

	private void writeFatSectorEntry(int clusterNumber, int value) {
		// One entry of the FAT has been updated
		if (log.isDebugEnabled()) {
			log.debug(String.format("writeFatSectorEntry[0x%X]=0x%08X", clusterNumber, value));
		}

		fatClusterMap[clusterNumber] = value;

		Fat32FileInfo fileInfo = fatFileInfoMap[clusterNumber];
		if (fileInfo != null) {
			// Freeing the cluster?
			if (isFreeClusterNumber(value)) {
				if (pendingDeleteFiles.containsValue(fileInfo)) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Deleting the file '%s'", fileInfo.getFullFileName()));
					}
					int result = vfs.ioRemove(fileInfo.getFullFileName());
					if (result < 0) {
						log.warn(String.format("Cannot delete the file '%s'", fileInfo.getFullFileName()));
					}
				}
			} else {
				// Setting a new data cluster number?
				if (isDataClusterNumber(value)) {
					int newClusterNumber = value & CLUSTER_MASK;
					if (!fileInfo.hasCluster(newClusterNumber)) {
						fileInfo.addCluster(newClusterNumber);
						fatFileInfoMap[newClusterNumber] = fileInfo;
					}
				}
				checkPendingWriteSectors(fileInfo);
			}
		}
	}

	private void writeFatSector(int fatIndex) {
		int offset = (fatIndex * sectorSize) >> 2;
		for (int i = 0, j = 0; i < sectorSize; i += 4, j++) {
			int fatEntry = readSectorInt32(currentSector, i);
			if (fatEntry != fatClusterMap[offset + j]) {
				writeFatSectorEntry(offset + j, fatEntry);
			}
		}
	}

	private void deleteDirectoryEntry(Fat32FileInfo fileInfo, byte[] directoryData, int offset) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("deleteDirectoryEntry on %s: %s", fileInfo, Utilities.getMemoryDump(directoryData, offset, directoryTableEntrySize)));
		}

		String fileName83 = Utilities.readStringNZ(directoryData, offset + 0, 8 + 3);

		Fat32FileInfo childFileInfo = fileInfo.getChildByFileName83(fileName83);
		if (childFileInfo == null) {
			log.warn(String.format("deleteDirectoryEntry cannot find child entry '%s' in %s", fileName83, fileInfo));
		} else {
			pendingDeleteFiles.put(childFileInfo.getFirstCluster(), childFileInfo);
		}
	}

	private void deleteDirectoryEntries(Fat32FileInfo fileInfo, byte[] directoryData, int offset, int length) {
		if (directoryData == null || length <= 0 || offset >= directoryData.length) {
			return;
		}

		for (int i = 0; i < length; i += directoryTableEntrySize) {
			deleteDirectoryEntry(fileInfo, directoryData, offset + i);
		}
	}

	private String getFileNameLFN(byte[] lfn) {
		boolean last = false;
		byte[] fileNameBytes = null;
		for (int sequenceNumber = 1; !last; sequenceNumber++) {
			for (int i = 0; i < lfn.length; i += directoryTableEntrySize) {
				if ((lfn[i + 0] & 0x1F) == sequenceNumber) {
					if ((lfn[i + 0] & 0x40) != 0) {
						last = true;
					}
					fileNameBytes = Utilities.extendArray(fileNameBytes, lfn, i + 1, 10);
					fileNameBytes = Utilities.extendArray(fileNameBytes, lfn, i + 14, 12);
					fileNameBytes = Utilities.extendArray(fileNameBytes, lfn, i + 28, 4);
					break;
				}
			}
		}

		if (fileNameBytes == null) {
			return "";
		}

		for (int i = 0; i < fileNameBytes.length; i += 2) {
			if (fileNameBytes[i] == ((byte) 0) && fileNameBytes[i + 1] == (byte) 0) {
				return new String(fileNameBytes, 0, i, charset16);
			}
		}

		return new String(fileNameBytes, charset16);
	}

	private String getFileName(byte[] sector, int offset, byte[] lfn) {
		String fileName;

		if (lfn != null) {
			fileName = getFileNameLFN(lfn);
		} else {
			String name = readSectorString(sector, offset + 0, 8);
			String ext = readSectorString(sector, offset + 8, 3);
			if (ext.length() == 0) {
				fileName = name;
			} else {
				fileName = name + '.' + ext;
			}
		}

		return fileName;
	}

	private void createDirectoryEntry(Fat32FileInfo fileInfo, byte[] sector, int offset) {
		if (offset >= sector.length) {
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("createDirectoryEntry on %s: %s", fileInfo, Utilities.getMemoryDump(sector, offset, directoryTableEntrySize)));
		}

		if (isLongFileNameDirectoryEntry(sector, offset)) {
			pendingCreateDirectoryEntryLFN = Utilities.extendArray(pendingCreateDirectoryEntryLFN, sector, offset, directoryTableEntrySize);
		} else {
			String fileName = getFileName(sector, offset, pendingCreateDirectoryEntryLFN);
			boolean readOnly = (sector[offset + 11] & 0x01) != 0;
			boolean directory = (sector[offset + 11] & 0x10) != 0;
			int clusterNumber = readSectorInt16(sector, offset + 20) << 16;
			clusterNumber |= readSectorInt16(sector, offset + 26);
			long fileSize = readSectorInt32(sector, offset + 28) & 0xFFFFFFFFL;
			if (log.isDebugEnabled()) {
				log.debug(String.format("createDirectoryEntry fileName='%s', readOnly=%b, directory=%b, clusterNumber=0x%X, fileSize=0x%X", fileName, readOnly, directory, clusterNumber, fileSize));
			}

			Fat32FileInfo pendingDeleteFile = pendingDeleteFiles.remove(clusterNumber);
			if (pendingDeleteFile != null) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Renaming directory entry %s into '%s'", pendingDeleteFile, fileName));
				}
				if (readOnly != pendingDeleteFile.isReadOnly()) {
					log.warn(String.format("Cannot change read-only attribute of %s", pendingDeleteFile));
				}
				if (directory != pendingDeleteFile.isDirectory()) {
					log.warn(String.format("Cannot change directory attribute of %s", pendingDeleteFile));
				}
				if (fileSize != pendingDeleteFile.getFileSize()) {
					log.warn(String.format("Cannot change file size of %s", pendingDeleteFile));
				}
				String oldFullFileName = pendingDeleteFile.getFullFileName();
				pendingDeleteFile.setFileName(fileName);
				String newFullFileName = pendingDeleteFile.getFullFileName();
				int result = vfs.ioRename(oldFullFileName, newFullFileName);
				if (result < 0) {
					log.warn(String.format("Cannot rename file '%s' into '%s'", oldFullFileName, newFullFileName));
				}
				pendingDeleteFile.setDirectory(directory);
				pendingDeleteFile.setReadOnly(readOnly);
				pendingDeleteFile.setFileSize(fileSize);
			} else {
				Fat32FileInfo newFileInfo = new Fat32FileInfo(fileInfo.getFullFileName(), fileName, directory, readOnly, null, fileSize);
				newFileInfo.setFileName83(Utilities.readStringNZ(sector, offset + 0, 8 + 3));
				if (clusterNumber != 0) {
					int[] clusters = new int[1];
					clusters[0] = clusterNumber;
					newFileInfo.setClusters(clusters);
					fatFileInfoMap[clusterNumber] = newFileInfo;
				}

				fileInfo.addChild(newFileInfo);
			}

			pendingCreateDirectoryEntryLFN = null;
		}
	}

	private void checkPendingWriteSectors(Fat32FileInfo fileInfo) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("checkPendingWriteSectors for %s", fileInfo));
		}

		if (pendingWriteSectors.isEmpty()) {
			return;
		}

		int[] clusters = fileInfo.getClusters();
		if (clusters == null) {
			return;
		}

		for (int i = 0; i < clusters.length; i++) {
			int clusterNumber = clusters[i];
			int sectorNumber = getSectorNumberFromCluster(clusterNumber);
			for (int j = 0; j < sectorsPerCluster; j++, sectorNumber++) {
				byte[] pendingWriteSector = pendingWriteSectors.remove(sectorNumber);
				if (pendingWriteSector != null) {
					writeFileSector(fileInfo, sectorNumber, pendingWriteSector);
				}
			}
		}
	}

	private int[] getClusters(int clusterNumber) {
		int[] clusters = new int[] { clusterNumber };

		while (true) {
			int nextCluster = fatClusterMap[clusterNumber];
			if (isLastClusterNumber(nextCluster)) {
				break;
			}

			// Add the nextCluster to the clusters array
			clusters = Utilities.extendArray(clusters, 1);
			clusterNumber = nextCluster & CLUSTER_MASK;
			clusters[clusters.length - 1] = clusterNumber;
		}

		return clusters;
	}

	private void updateDirectoryEntry(Fat32FileInfo fileInfo, byte[] directoryData, int directoryDataOffset, byte[] sector, int sectorOffset) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("updateDirectoryEntry on %s: from %s, to %s", fileInfo, Utilities.getMemoryDump(directoryData, directoryDataOffset, directoryTableEntrySize), Utilities.getMemoryDump(sector, sectorOffset, directoryTableEntrySize)));
		}

		int oldClusterNumber = readSectorInt16(directoryData, directoryDataOffset + 20) << 16;
		oldClusterNumber |= readSectorInt16(directoryData, directoryDataOffset + 26);

		int newClusterNumber = readSectorInt16(sector, sectorOffset + 20) << 16;
		newClusterNumber |= readSectorInt16(sector, sectorOffset + 26);
		long newFileSize = readSectorInt32(sector, sectorOffset + 28) & 0xFFFFFFFFL;
		if (log.isDebugEnabled()) {
			log.debug(String.format("updateDirectoryEntry oldClusterNumber=0x%X, newClusterNumber=0x%X, newFileSize=0x%X", oldClusterNumber, newClusterNumber, newFileSize));
		}

		String oldFileName83 = Utilities.readStringNZ(directoryData, directoryDataOffset + 0, 8 + 3);
		String newFileName83 = Utilities.readStringNZ(sector, sectorOffset + 0, 8 + 3);

		if (!oldFileName83.equals(newFileName83)) {
			// TODO
			log.warn(String.format("updateDirectoryEntry unimplemented change of 8.3. file name: from '%s' to '%s'", oldFileName83, newFileName83));
		}

		Fat32FileInfo childFileInfo = fileInfo.getChildByFileName83(oldFileName83);
		if (childFileInfo == null) {
			log.warn(String.format("updateDirectoryEntry child '%s' not found", oldFileName83));
		} else {
			// Update the file size.
			// Rem.: this must be done before calling checkPendingWriteSectors
			childFileInfo.setFileSize(newFileSize);

			// Update the clusterNumber
			if (oldClusterNumber != newClusterNumber) {
				int[] clusters = getClusters(newClusterNumber);
				builder.setClusters(childFileInfo, clusters);

				// The clusters have been updated for this file,
				// check if there were pending sector writes in the new
				// clusters
				checkPendingWriteSectors(childFileInfo);
			}
		}
	}

	private static boolean isLongFileNameDirectoryEntry(byte[] sector, int offset) {
		// Attributes (always 0x0F for LFN)
		return sector[offset + 11] == 0x0F;
	}

	private void writeFileSector(Fat32FileInfo fileInfo, int sectorNumber, byte[] sector) {
		int clusterNumber = getClusterNumber(sectorNumber);
		int sectorOffsetInCluster = getSectorOffsetInCluster(sectorNumber);

		IVirtualFile vFile = fileInfo.getVirtualFile(vfs);
		if (vFile == null) {
			log.warn(String.format("writeFileSector cannot write file '%s'", fileInfo));
			return;
		}

		long byteOffset = sectorOffsetInCluster * (long) sectorSize;
		int[] clusters = fileInfo.getClusters();
		if (clusters != null) {
			for (int i = 0; i < clusters.length; i++) {
				if (clusters[i] == clusterNumber) {
					break;
				}
				byteOffset += sectorsPerCluster * sectorSize;
			}
		}

		if (byteOffset < fileInfo.getFileSize()) {
			if (vFile.ioLseek(byteOffset) != byteOffset) {
				log.warn(String.format("writeFileSector cannot seek file '%s' to 0x%X", fileInfo, byteOffset));
				return;
			}

			int length = (int) Math.min(fileInfo.getFileSize() - byteOffset, (long) sectorSize);
			int writeLength = vFile.ioWrite(sector, 0, length);
			if (log.isDebugEnabled()) {
				log.debug(String.format("writeFileSector writeLength=0x%X", writeLength));
			}
		}
	}

	private void writeDataSector(int sectorNumber) {
		int clusterNumber = getClusterNumber(sectorNumber);
		int sectorOffsetInCluster = getSectorOffsetInCluster(sectorNumber);
		Fat32FileInfo fileInfo = fatFileInfoMap[clusterNumber];
		if (fileInfo == null) {
			pendingWriteSectors.put(sectorNumber, currentSector.clone());
			if (log.isDebugEnabled()) {
				log.debug(String.format("writeDataSector pending sectorNumber=0x%X, clusterNumber=0x%X", sectorNumber, clusterNumber));
			}
			return;
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("writeDataSector clusterNumber=0x%X(sector=0x%X), fileInfo=%s", clusterNumber, sectorOffsetInCluster, fileInfo));
		}

		if (fileInfo.isDirectory()) {
			byte[] directoryData = fileInfo.getFileData();
			if (directoryData == null) {
				directoryData = builder.buildDirectoryData(fileInfo);
				fileInfo.setFileData(directoryData);
			}

			int byteOffset = sectorOffsetInCluster * sectorSize;
			int sectorLength = sectorSize;
			for (int i = 0; i < sectorSize; i += directoryTableEntrySize) {
				// End of directory table?
				if (currentSector[i + 0] == (byte) 0) {
					// Delete the remaining directory entries of the current directory
					deleteDirectoryEntries(fileInfo, directoryData, byteOffset + i, directoryData.length - (byteOffset + i));
					sectorLength = i;
					break;
				}

				if (currentSector[i + 0] == (byte) 0xE5) {
					// Deleted file
					if (byteOffset + i < directoryData.length && directoryData[byteOffset + i + 0] != (byte) 0xE5) {
						deleteDirectoryEntry(fileInfo, directoryData, byteOffset + i);
					}
				} else if (byteOffset + i >= directoryData.length) {
					createDirectoryEntry(fileInfo, currentSector, i);
				} else if (!Utilities.equals(directoryData, byteOffset + i, currentSector, i, directoryTableEntrySize)) {
					updateDirectoryEntry(fileInfo, directoryData, byteOffset + i, currentSector, i);
				}
			}

			directoryData = Utilities.copyToArrayAndExtend(directoryData, byteOffset, currentSector, 0, sectorLength);
			fileInfo.setFileData(directoryData);
		} else {
			writeFileSector(fileInfo, sectorNumber, currentSector);
		}
	}

	private void writeEmptySector() {
	}

	private void writeSector(int sectorNumber) {
		if (sectorNumber == bootSectorNumber) {
			writeBootSector();
		} else if (sectorNumber == fsInfoSectorNumber) {
			writeFsInfoSector();
		} else if (sectorNumber < fatSectorNumber) {
			writeEmptySector();
		} else if (sectorNumber >= fatSectorNumber && sectorNumber < fatSectorNumber + fatSectors) {
			writeFatSector(sectorNumber - fatSectorNumber);
		} else if (sectorNumber >= fatSectorNumber + fatSectors && sectorNumber < fatSectorNumber + numberOfFats * fatSectors) {
			// Writing to the second FAT table, ignore it
			writeEmptySector();
		} else {
			writeDataSector(sectorNumber);
		} 
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		int readLength = 0;
		while (outputLength > 0) {
			readSector(getSectorNumber(position));
			int sectorOffset = getSectorOffset(position);
			int sectorLength = sectorSize - sectorOffset;
			int length = Math.min(sectorLength, outputLength);

			outputPointer.setArray(0, currentSector, sectorOffset, length);

			outputLength -= length;
			outputPointer.add(length);
			position += length;
			readLength += length;
		}

		return readLength;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		int readLength = 0;
		while (outputLength > 0) {
			readSector(getSectorNumber(position));
			int sectorOffset = getSectorOffset(position);
			int sectorLength = sectorSize - sectorOffset;
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
		int writeLength = 0;
		while (inputLength > 0) {
			int sectorOffset = getSectorOffset(position);
			int sectorLength = sectorSize - sectorOffset;
			int length = Math.min(sectorLength, inputLength);

			if (length != sectorSize) {
				// Not writing a complete sector, read the current sector
				readSector(getSectorNumber(position));
			}

			System.arraycopy(inputPointer.getArray8(length), 0, currentSector, sectorOffset, length);

			writeSector(getSectorNumber(position));

			inputLength -= length;
			inputPointer.add(length);
			position += length;
			writeLength += length;
		}

		return writeLength;
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
		return IO_ERROR;
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
		return IoFileMgrForUser.noDelayTimings;
	}

	@Override
	public int ioClose() {
		vfs.ioExit();
		vfs = null;

		return 0;
	}

    @Override
	public String toString() {
    	return String.format("%s", vfs);
	}
}
