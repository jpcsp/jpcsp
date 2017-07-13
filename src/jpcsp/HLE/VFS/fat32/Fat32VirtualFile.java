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
import static jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure.charset16;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.hardware.MemoryStick;
import jpcsp.util.Utilities;

// See format description: https://en.wikipedia.org/wiki/Design_of_the_FAT_file_system
public class Fat32VirtualFile implements IVirtualFile {
	public static Logger log = Logger.getLogger("fat32");
    public final static int sectorSize = 512;
    private final static int bootSectorNumber = 0;
    private final static int fsInfoSectorNumber = bootSectorNumber + 1;
    private final static int numberOfFats = 2;
    private final static int reservedSectors = 32;
	private final static int sectorsPerCluster = 64;
    private final static int fatSectorNumber = bootSectorNumber + reservedSectors;
    private final static int directoryTableEntrySize = 32;
    private final static int firstClusterNumber = 2;
    private final static int clusterSize = sectorSize * sectorsPerCluster;
    private final byte[] currentSector = new byte[sectorSize];
    private static final byte[] emptySector = new byte[sectorSize];
	private IVirtualFileSystem vfs;
	private long position;
    private int totalSectors;
    private int fatSectors;
    private int firstFreeCluster;
    private Fat32FileInfo rootDirectory;
    private int[] fatClusterMap;
    private Fat32FileInfo[] fatFileInfoMap;
    private byte[] pendingCreateDirectoryEntryLFN;
    private Map<Integer, byte[]> pendingWriteSectors = new HashMap<Integer, byte[]>();

	public Fat32VirtualFile(IVirtualFileSystem vfs) {
		this.vfs = vfs;

		totalSectors = (int) (MemoryStick.getTotalSize() / sectorSize);
		fatSectors = getFatSectors(totalSectors, sectorsPerCluster);
		firstFreeCluster = firstClusterNumber;

		if (log.isDebugEnabled()) {
			log.debug(String.format("totalSectors=0x%X, fatSectors=0x%X", totalSectors, fatSectors));
		}

		// Allocate the FAT cluster map
		fatClusterMap = new int[fatSectors * (sectorSize / 4)];
		// First 2 special entries in the cluster map
		fatClusterMap[0] = 0x0FFFFFF8; // 0xF8 is matching the boot sector Media type field
		fatClusterMap[1] = 0x0FFFFFFF;

		fatFileInfoMap = new Fat32FileInfo[fatClusterMap.length];
	}

	private static int getSectorNumber(long position) {
		return (int) (position / sectorSize);
	}

	private static int getSectorOffset(long position) {
		return (int) (position % sectorSize);
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

	public void scan() {
		// Allocate a whole cluster for the root directory
		int[] rootDirectoryClusters = allocateClusters(clusterSize);
		rootDirectory = new Fat32FileInfo(null, null, true, false, null, 0);
		rootDirectory.setParentDirectory(rootDirectory);

		scan(null, rootDirectory);

		setClusters(rootDirectory, rootDirectoryClusters);

		if (log.isDebugEnabled()) {
			debugScan(rootDirectory);
		}
	}

	private void debugScan(Fat32FileInfo fileInfo) {
		log.debug(String.format("scan %s", fileInfo));
		List<Fat32FileInfo> children = fileInfo.getChildren();
		if (children != null) {
			for (Fat32FileInfo child: children) {
				debugScan(child);
			}
		}
	}

	private void setClusters(Fat32FileInfo fileInfo, int[] clusters) {
		if (clusters != null) {
			for (int i = 0; i < clusters.length; i++) {
				fatFileInfoMap[clusters[i]] = fileInfo;
			}
		}
		fileInfo.setClusters(clusters);
	}

	private int allocateCluster() {
		return firstFreeCluster++;
	}

	private int[] allocateClusters(long size) {
		int clusterSize = sectorSize * sectorsPerCluster;
		int numberClusters = (int) ((size + clusterSize - 1) / clusterSize);
		if (numberClusters <= 0) {
			return null;
		}

		int[] clusters = new int[numberClusters];
		for (int i = 0; i < numberClusters; i++) {
			clusters[i] = allocateCluster();
		}

		// Fill the cluster chain in the cluster map
		for (int i = 0; i < numberClusters - 1; i++) {
			// Pointing to the next cluster
			fatClusterMap[clusters[i]] = clusters[i + 1];
		}
		if (numberClusters > 0) {
			// Last cluster in file (EOC)
			fatClusterMap[clusters[numberClusters - 1]] = 0x0FFFFFFF;
		}

		return clusters;
	}

	private void allocateClusters(Fat32FileInfo fileInfo) {
		long dataSize = fileInfo.getFileSize();
		if (fileInfo.isDirectory()) {
			// Two child entries for "." and ".."
			int directoryTableEntries = 2;

			List<Fat32FileInfo> children = fileInfo.getChildren();
			if (children != null) {
				// TODO: take fake entries into account to support "long filename"
				directoryTableEntries += children.size();
			} 

			dataSize = directoryTableEntrySize * directoryTableEntries;
		}

		int[] clusters = allocateClusters(dataSize);
		setClusters(fileInfo, clusters);
	}

	private void scan(String dirName, Fat32FileInfo parent) {
		String[] names = vfs.ioDopen(dirName);
		if (names == null || names.length == 0) {
			return;
		}

		SceIoStat stat = new SceIoStat();
		SceIoDirent dir = new SceIoDirent(stat, null);
		for (int i = 0; i < names.length; i++) {
			dir.filename = names[i];
			if (vfs.ioDread(dirName, dir) >= 0) {
				boolean directory = (dir.stat.attr & 0x10) != 0;
				boolean readOnly = (dir.stat.mode & 0x2) == 0;
				Fat32FileInfo fileInfo = new Fat32FileInfo(dirName, dir.filename, directory, readOnly, dir.stat.mtime, dir.stat.size);

				parent.addChild(fileInfo);

				if (directory) {
					if (dirName == null) {
						scan(dir.filename, fileInfo);
					} else {
						scan(dirName + "/" + dir.filename, fileInfo);
					}
				}

				// Allocate the clusters after having scanned the children
				allocateClusters(fileInfo);
			}
		}

		List<Fat32FileInfo> children = parent.getChildren();
		if (children != null) {
			for (Fat32FileInfo child: children) {
				computeFileName83(child, children);
			}
		}
	}

	private int getFatSectors(int totalSectors, int sectorsPerCluster) {
    	int totalClusters = (totalSectors / sectorsPerCluster) + 1;
    	int fatSectors = (totalClusters / (sectorSize / 4)) + 1;

    	return fatSectors;
    }

	private static void storeSectorInt32(byte[] sector, int offset, int value) {
		Utilities.writeUnaligned32(sector, offset, value);
	}

	private static void storeSectorInt8(byte[] sector, int offset, int value) {
		sector[offset] = (byte) value;
	}

	private static void storeSectorInt16(byte[] sector, int offset, int value) {
		Utilities.writeUnaligned16(sector, offset, value);
	}

	private static int readSectorInt32(byte[] sector, int offset) {
		return Utilities.readUnaligned32(sector, offset);
	}

	private static int readSectorInt16(byte[] sector, int offset) {
		return Utilities.readUnaligned16(sector, offset);
	}

	private static void storeSectorString(byte[] sector, int offset, String value, int length) {
		int stringLength = Math.min(value.length(), length);
		Utilities.writeStringNZ(sector, offset, stringLength, value);

		// Fill rest with spaces
		for (int i = stringLength; i < length; i++) {
			sector[offset + i] = (byte) ' ';
		}
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

	private static String convertFileName8_3To83(String fileName8_3) {
		String name = fileName8_3;
		String extension = "";
		int dotIndex = name.indexOf('.');
		if (dotIndex >= 0) {
			extension = name.substring(dotIndex + 1);
			name = name.substring(0, dotIndex);
		}

		name = (name + "        ").substring(0, 8);
		extension = (extension + "   ").substring(0, 3);

		return name + extension;
	}

	private boolean isLongFileName(String fileName8_3, String fileName) {
		return !fileName8_3.equalsIgnoreCase(fileName);
	}

	// Convert a "long" file name into a 8.3 file name.
	public static String convertFileNameTo8_3(String fileName, int collisionIndex) {
		if (fileName == null) {
			return null;
		}

		// Special character '+' is turned into '_'
		fileName = fileName.replace("+", "_");
		// File name is upper-cased
		fileName = fileName.toUpperCase();

		// Split into the name and extension parts
		int lastDot = fileName.lastIndexOf(".");
		String name;
		String ext;
		if (lastDot < 0) {
			name = fileName;
			ext = "";
		} else {
			name = fileName.substring(0, lastDot);
			ext = fileName.substring(lastDot + 1);
		}

		// All dots in name part are dropped
		name = name.replace(".", "");

		// The file extension is truncated to 3 characters
		if (ext.length() > 3) {
			ext = ext.substring(0, 3);
		}

		if (collisionIndex >= 1) {
			if (collisionIndex <= 4) {
				// The name is truncated to 6 characters, followed by "~N"
				if (name.length() > 6) {
					name = name.substring(0, 6);
				}
				name += "~" + collisionIndex;
			} else {
				// The name is truncated to 2 characters,
				// followed by 4 hexadecimal digits derived
				// from an undocumented hash of the filename,
				// followed by a tilde, followed by a single digit.
				if (name.length() > 2) {
					name = name.substring(0, 2);
				}
				name += String.format("%04X", collisionIndex) + "~1";
			}
		} else if (name.length() > 8) {
			// The name is truncated to 6 characters (if longer than 8 characters)
			// followed by "~1"
			name = name.substring(0, 6) + "~1";
		}

		if (ext.length() == 0) {
			return name;
		}
		return name + "." + ext;
	}

	public static String convertFileNameTo8_3(String fileName) {
		return convertFileNameTo8_3(fileName, 0);
	}

	public static String convertFileNameTo83(String fileName, int collisionIndex) {
		String fileName8_3 = convertFileNameTo8_3(fileName, collisionIndex);
		return convertFileName8_3To83(fileName8_3);
	}

	private void computeFileName83(Fat32FileInfo fileInfo, List<Fat32FileInfo> siblings) {
		int collisionIndex = 0;
		String fileName = fileInfo.getFileName();
		String fileName83 = convertFileNameTo83(fileName, collisionIndex);

		// Check if the 8.3 file name is not colliding
		// with other 8.3 file names in the same directory.
		boolean hasCollision;
		do {
			hasCollision = false;
			for (Fat32FileInfo sibling: siblings) {
				String siblingFileName83 = sibling.getFileName83();
				if (siblingFileName83 != null) {
					if (fileName83.equals(siblingFileName83)) {
						// 8.3 file name collision
						collisionIndex++;
						hasCollision = true;
						fileName83 = convertFileNameTo83(fileName, collisionIndex);
						break;
					}
				}
			}
		} while (hasCollision);

		fileInfo.setFileName83(fileName83);
	}

	private byte[] addLongFileNameDirectoryEntries(byte[] directoryData, String fileName, int fileNameChecksum) {
		byte[] fileNameBytes = fileName.getBytes(charset16);
		int numberEntries = (fileNameBytes.length + 2 + 25) / 26;

		byte[] extend = new byte[numberEntries * 26 - fileNameBytes.length];
		extend[0] = (byte) 0;
		extend[1] = (byte) 0;
		for (int i = 2; i < extend.length; i++) {
			extend[i] = (byte) 0xFF;
		}
		fileNameBytes = Utilities.extendArray(fileNameBytes, extend);

		int offset = directoryData.length;
		directoryData = Utilities.extendArray(directoryData, directoryTableEntrySize * numberEntries);

		for (int i = numberEntries; i > 0; i--) {
			int sequenceNumber = i;
			if (i == numberEntries) {
				sequenceNumber |= 0x40; // Last LFN entry
			}
			storeSectorInt8(directoryData, offset + 0, sequenceNumber);

			// Name characters (five UCS-2 characters)
			int fileNameBytesOffset = (i - 1) * 26;
			System.arraycopy(fileNameBytes, fileNameBytesOffset, directoryData, offset + 1, 10);
			fileNameBytesOffset += 10;

			// Attributes (always 0x0F)
			storeSectorInt8(directoryData, offset + 11, 0x0F);

			// Type (always 0x00 for VFAT LFN)
			storeSectorInt8(directoryData, offset + 12, 0x00);

			// Checksum of DOS file name
			storeSectorInt8(directoryData, offset + 13, fileNameChecksum);

			// Name characters (six UCS-2 characters)
			System.arraycopy(fileNameBytes, fileNameBytesOffset, directoryData, offset + 14, 12);
			fileNameBytesOffset += 12;

			// First cluster (always 0)
			storeSectorInt16(directoryData, offset + 26, 0);

			// Name characters (two UCS-2 characters)
			System.arraycopy(fileNameBytes, fileNameBytesOffset, directoryData, offset + 28, 4);

			offset += directoryTableEntrySize;
		}

		return directoryData;
	}

	private int getFileNameChecksum(String fileName) {
		int checksum = 0;

		for (int i = 0; i < fileName.length(); i++) {
			int c = fileName.charAt(i) & 0xFF;
			checksum = ((checksum & 1) << 7) + (checksum >> 1) + c;
		}

		return checksum;
	}

	private byte[] addDirectoryEntry(byte[] directoryData, Fat32FileInfo fileInfo) {
		String fileName = fileInfo.getFileName();
		String fileName8_3 = convertFileNameTo8_3(fileName);
		String fileName83 = convertFileName8_3To83(fileName8_3);
		if (isLongFileName(fileName8_3, fileName)) {
			int checksum = getFileNameChecksum(fileName83);
			directoryData = addLongFileNameDirectoryEntries(directoryData, fileName, checksum);
		}

		int offset = directoryData.length;
		directoryData = Utilities.extendArray(directoryData, directoryTableEntrySize);

		storeSectorString(directoryData, offset + 0, fileName83, 8 + 3);

		int fileAttributes = 0x20; // Archive attribute
		if (fileInfo.isReadOnly()) {
			fileAttributes |= 0x01; // Read Only attribute
		}
		if (fileInfo.isDirectory()) {
			fileAttributes |= 0x10; // Sub-directory attribute
		}
		storeSectorInt8(directoryData, offset + 11, fileAttributes);

		// Has extended attributes?
		storeSectorInt8(directoryData, offset + 12, 0);

		ScePspDateTime lastModified = fileInfo.getLastModified();
		storeSectorInt8(directoryData, offset + 13, 0); // Milliseconds always set to 0 by the PSP

		int createTime = lastModified.hour << 11;
		createTime |= lastModified.minute << 5;
		createTime |= lastModified.second >> 1;
		storeSectorInt16(directoryData, offset + 14, createTime);

		int createDate = (lastModified.year - 1980) << 9;
		createDate |= lastModified.month << 5;
		createDate |= lastModified.day;
		storeSectorInt16(directoryData, offset + 16, createDate);

		storeSectorInt16(directoryData, offset + 18, createDate);

		int[] clusters = fileInfo.getClusters();
		if (clusters != null) {
			storeSectorInt16(directoryData, offset + 20, clusters[0] >>> 16);
		} else {
			storeSectorInt16(directoryData, offset + 20, 0); // Empty file
		}

		storeSectorInt16(directoryData, offset + 22, createTime);
		storeSectorInt16(directoryData, offset + 24, createDate);

		if (clusters != null) {
			storeSectorInt16(directoryData, offset + 26, clusters[0] & 0xFFFF);
		} else {
			storeSectorInt16(directoryData, offset + 26, 0); // Empty file
		}

		storeSectorInt32(directoryData, offset + 28, (int) fileInfo.getFileSize());

		return directoryData;
	}

	private void buildDotDirectoryEntry(byte[] directoryData, int offset, Fat32FileInfo fileInfo, String dotName, ScePspDateTime alternateLastModified) {
		storeSectorString(directoryData, offset + 0, dotName, 8 + 3);

		// File attributes: directory
		storeSectorInt8(directoryData, offset + 11, 0x10);

		// Has extended attributes?
		storeSectorInt8(directoryData, offset + 12, 0);

		ScePspDateTime lastModified = fileInfo.getLastModified();
		if (lastModified == null) {
			// The root directory has no lastModified date/time,
			// rather use the date/time of the sub-directory.
			lastModified = alternateLastModified;
		}
		storeSectorInt8(directoryData, offset + 13, 0); // Milliseconds, always set to 0 by the PSP

		int createTime = lastModified.hour << 11;
		createTime |= lastModified.minute << 5;
		createTime |= lastModified.second >> 1;
		storeSectorInt16(directoryData, offset + 14, createTime);

		int createDate = (lastModified.year - 1980) << 9;
		createDate |= lastModified.month << 5;
		createDate |= lastModified.day;
		storeSectorInt16(directoryData, offset + 16, createDate);

		storeSectorInt16(directoryData, offset + 18, createDate);

		int[] clusters = fileInfo.getClusters();
		if (clusters != null) {
			storeSectorInt16(directoryData, offset + 20, clusters[0] >>> 16);
		} else {
			storeSectorInt16(directoryData, offset + 20, 0); // Empty file
		}

		storeSectorInt16(directoryData, offset + 22, createTime);
		storeSectorInt16(directoryData, offset + 24, createDate);

		if (clusters != null) {
			storeSectorInt16(directoryData, offset + 26, clusters[0] & 0xFFFF);
		} else {
			storeSectorInt16(directoryData, offset + 26, 0); // Empty file
		}

		// File size
		storeSectorInt32(directoryData, offset + 28, 0);
	}

	private byte[] buildDirectoryData(Fat32FileInfo fileInfo) {
		byte[] directoryData;

		// Is this the root directory?
		if (fileInfo.isRootDirectory()) {
			// The root directory has no "." nor ".." directory entries
			directoryData = new byte[0];
		} else {
			// Non-root directories have "." and ".." directory entries
			directoryData = new byte[directoryTableEntrySize * 2];

			buildDotDirectoryEntry(directoryData, 0, fileInfo, ".", fileInfo.getLastModified());
			buildDotDirectoryEntry(directoryData, directoryTableEntrySize, fileInfo.getParentDirectory(), "..", fileInfo.getLastModified());
		}

		List<Fat32FileInfo> children = fileInfo.getChildren();
		if (children != null) {
			for (Fat32FileInfo child : children) {
				directoryData = addDirectoryEntry(directoryData, child);
			}
		}

		return directoryData;
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
				directoryData = buildDirectoryData(fileInfo);
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
			int newClusterNumber = value & 0x0FFFFFFF;
			// Setting a new data cluster number?
			if (newClusterNumber >= 2 && newClusterNumber <= 0x0FFFFFEF) {
				if (!fileInfo.hasCluster(newClusterNumber)) {
					fileInfo.addCluster(newClusterNumber);
					fatFileInfoMap[newClusterNumber] = fileInfo;
				}
			}
			checkPendingWriteSectors(fileInfo);
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
		// TODO
	}

	private void deleteDirectoryEntries(Fat32FileInfo fileInfo, byte[] directoryData, int offset, int length) {
		if (directoryData == null || length <= 0 || offset >= directoryData.length) {
			return;
		}

		for (int i = 0; i < length; i += directoryTableEntrySize) {
			deleteDirectoryEntry(fileInfo, directoryData, offset + i);
		}
	}

	private String extractString(byte[] sector, int offset, int length) {
		String s = "";
		// Skip any trailing spaces
		for (int i = length - 1; i >= 0; i--) {
			if (sector[offset + i] != (byte) ' ') {
				s = new String(sector, offset, i + 1);
				break;
			}
		}

		return s;
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
			String name = extractString(sector, offset + 0, 8);
			String ext = extractString(sector, offset + 8, 3);
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

			Fat32FileInfo newFileInfo = new Fat32FileInfo(fileInfo.getFullFileName(), fileName, directory, readOnly, null, fileSize);
			newFileInfo.setFileName83(Utilities.readStringNZ(sector, offset + 0, 8 + 3));
			if (clusterNumber != 0) {
				int[] clusters = new int[1];
				clusters[0] = clusterNumber;
				newFileInfo.setClusters(clusters);
				fatFileInfoMap[clusterNumber] = newFileInfo;
			}

			fileInfo.addChild(newFileInfo);
			// TODO

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
				int[] clusters = new int[1];
				clusters[0] = newClusterNumber;
				childFileInfo.setClusters(clusters);
				fatFileInfoMap[newClusterNumber] = childFileInfo;

				// The clusters have been updated for this file,
				// check if there were pending sector writes in the new
				// clusters
				checkPendingWriteSectors(childFileInfo);
			}
		}
	}

	private boolean isLongFileNameDirectoryEntry(byte[] sector, int offset) {
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
			log.warn(String.format("writeDataSector unknown sectorNumber=0x%X, clusterNumber=0x%X", sectorNumber, clusterNumber));
			return;
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("writeDataSector clusterNumber=0x%X(sector=0x%X), fileInfo=%s", clusterNumber, sectorOffsetInCluster, fileInfo));
		}

		if (fileInfo.isDirectory()) {
			byte[] directoryData = fileInfo.getFileData();
			if (directoryData == null) {
				directoryData = buildDirectoryData(fileInfo);
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

				if (byteOffset + i >= directoryData.length) {
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
    	return vfs.toString();
	}
}
