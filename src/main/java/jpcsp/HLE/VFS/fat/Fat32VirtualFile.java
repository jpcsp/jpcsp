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

import static jpcsp.HLE.VFS.fat.FatBuilder.numberOfFats;
import static jpcsp.HLE.VFS.fat.FatBuilder.reservedSectors;
import static jpcsp.HLE.VFS.fat.FatUtils.readSectorInt32;
import static jpcsp.HLE.VFS.fat.FatUtils.storeSectorInt16;
import static jpcsp.HLE.VFS.fat.FatUtils.storeSectorInt32;
import static jpcsp.HLE.VFS.fat.FatUtils.storeSectorInt8;
import static jpcsp.HLE.VFS.fat.FatUtils.storeSectorString;

import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.hardware.MemoryStick;

public class Fat32VirtualFile extends FatVirtualFile {
	private final int[] rootDirectoryClusters = new int[1];

	public Fat32VirtualFile(String deviceName, IVirtualFileSystem vfs) {
		super(deviceName, vfs, (int) (MemoryStick.getTotalSize() / sectorSize));
	}

	@Override
	protected int getClusterMask() {
		return 0x0FFFFFFF;
	}

	@Override
	protected int getFatEOC() {
		return 0x0FFFFFFF; // Last cluster in file (EOC)
	}

	@Override
	protected int getSectorsPerCluster() {
		return 64;
	}

	@Override
	protected int getFatSectors(int totalSectors, int sectorsPerCluster) {
    	int totalClusters = (totalSectors / sectorsPerCluster) + 1;
    	int fatSectors = (totalClusters / (sectorSize / 4)) + 1;

    	return fatSectors;
    }

	@Override
	protected void readBIOSParameterBlock() {
    	// Bytes per sector
    	storeSectorInt16(currentSector, 11, sectorSize);

    	// Sectors per cluster
    	storeSectorInt8(currentSector, 13, getSectorsPerCluster());

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
    	final int fatSectors = getFatSectors(totalSectors, getSectorsPerCluster());
    	storeSectorInt32(currentSector, 36, fatSectors);

    	// Drive description / mirroring flags
    	storeSectorInt16(currentSector, 40, 0);

    	// Version
    	storeSectorInt16(currentSector, 42, 0);

    	// Cluster number of root directory start
    	final int rootDirFirstCluster = 2;
    	storeSectorInt32(currentSector, 44, rootDirFirstCluster);

    	// Sector number of FS Information Sector
    	storeSectorInt16(currentSector, 48, getFsInfoSectorNumber());

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
	}

	@Override
	protected void readFatSector(int fatIndex) {
		readEmptySector();

		int offset = (fatIndex * sectorSize) >> 2;
		int maxSize = Math.min(sectorSize, (fatClusterMap.length - offset) << 2);
		for (int i = 0, j = 0; i < maxSize; i += 4, j++) {
			storeSectorInt32(currentSector, i, fatClusterMap[offset + j]);
		}
	}

	@Override
	protected void writeFatSector(int fatIndex) {
		int offset = (fatIndex * sectorSize) >> 2;

		if (log.isDebugEnabled()) {
			log.debug(String.format("Fat32VirtualFile.writeFatSector fatIndex=0x%X, offset=0x%X", fatIndex, offset));
		}

		for (int i = 0, j = 0; i < sectorSize; i += 4, j++) {
			int fatEntry = readSectorInt32(currentSector, i);
			if (fatEntry != fatClusterMap[offset + j]) {
				writeFatSectorEntry(offset + j, fatEntry);
			}
		}
	}

	@Override
	protected int getFirstDataClusterOffset() {
		// The first data cluster is starting at the root directory
		return 0;
	}

	@Override
	protected int getFirstFreeCluster() {
		// Allocate the first cluster(s) for the root directory
		int clusterNumber = super.getFirstFreeCluster();
		for (int i = 0; i < rootDirectoryClusters.length; i++) {
			rootDirectoryClusters[i] = clusterNumber++;
		}

		return clusterNumber;
	}

	@Override
	protected void setRootDirectory(FatFileInfo rootDirectory) {
		for (int i = 0; i < rootDirectoryClusters.length; i++) {
			setFatFileInfoMap(rootDirectoryClusters[i], rootDirectory);
			int nextCluster = i < rootDirectoryClusters.length - 1 ? rootDirectoryClusters[i + 1] : getFatEOC();
			setFatClusterMap(rootDirectoryClusters[i], nextCluster);
		}
		rootDirectory.setClusters(rootDirectoryClusters);
	}
}
