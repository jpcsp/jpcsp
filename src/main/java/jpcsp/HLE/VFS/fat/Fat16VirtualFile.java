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

import static jpcsp.HLE.VFS.fat.FatUtils.readSectorInt16;
import static jpcsp.HLE.VFS.fat.FatUtils.storeSectorInt16;

import jpcsp.HLE.VFS.IVirtualFileSystem;

public class Fat16VirtualFile extends Fat12VirtualFile {
	public Fat16VirtualFile(String deviceName, IVirtualFileSystem vfs, int totalSectors) {
		super(deviceName, vfs, totalSectors);
	}

	@Override
	protected int getClusterMask() {
		return 0x0000FFFF;
	}

	@Override
	protected int getFatEOC() {
		return 0xFFF8; // Last cluster in file (EOC)
	}

	@Override
	protected void readFatSector(int fatIndex) {
		readEmptySector();

		if (log.isDebugEnabled()) {
			log.debug(String.format("Fat16VirtualFile.readFatSector fatIndex=0x%X", fatIndex));
		}

		int offset = (fatIndex * sectorSize) >> 1;
		int maxSize = Math.min(sectorSize, (fatClusterMap.length - offset) << 1);
		for (int i = 0, j = 0; i < maxSize; i += 2, j++) {
			storeSectorInt16(currentSector, i, fatClusterMap[offset + j]);
		}
	}

	@Override
	protected void writeFatSector(int fatIndex) {
		int offset = (fatIndex * sectorSize) >> 1;

		if (log.isDebugEnabled()) {
			log.debug(String.format("Fat16VirtualFile.writeFatSector fatIndex=0x%X, offset=0x%X", fatIndex, offset));
		}

		for (int i = 0, j = 0; i < sectorSize; i += 2, j++) {
			int fatEntry = readSectorInt16(currentSector, i);
			if (fatEntry != fatClusterMap[offset + j]) {
				writeFatSectorEntry(offset + j, fatEntry);
			}
		}
	}
}
