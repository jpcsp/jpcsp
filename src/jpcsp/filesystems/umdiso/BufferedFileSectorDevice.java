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
package jpcsp.filesystems.umdiso;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;

public class BufferedFileSectorDevice extends AbstractFileSectorDevice {
	protected RandomAccessFile tocFile;
	protected ISectorDevice sectorDevice;
	protected HashMap<Integer, Integer> toc;
	protected boolean tocDirty;
	protected int nextFreeBufferedSectorNumber;
	protected int numSectors;

	public BufferedFileSectorDevice(RandomAccessFile tocFile, RandomAccessFile fileAccess, ISectorDevice sectorDevice) {
		super(fileAccess);
		this.tocFile = tocFile;
		this.sectorDevice = sectorDevice;
		readToc();
	}

	protected void readToc() {
		toc = new HashMap<Integer, Integer>();
		nextFreeBufferedSectorNumber = 0;
		tocDirty = false;
		try {
			tocFile.seek(0);
			long length = tocFile.length();
			if (length >= 4) {
				numSectors = tocFile.readInt();
				for (long i = 4; i < length; i += 8) {
					int sectorNumber = tocFile.readInt();
					int bufferedSectorNumber = tocFile.readInt();
					toc.put(sectorNumber, bufferedSectorNumber);
					nextFreeBufferedSectorNumber = Math.max(nextFreeBufferedSectorNumber, bufferedSectorNumber + 1);
				}
			} else if (sectorDevice != null) {
				numSectors = sectorDevice.getNumSectors();
			}
		} catch (IOException e) {
			log.error("readToc", e);
		}
	}

	protected void writeToc() {
		if (tocDirty) {
			try {
				tocFile.seek(0);
				tocFile.writeInt(getNumSectors());
				for (Integer sectorNumber : toc.keySet()) {
					Integer bufferedSectorNumber = toc.get(sectorNumber);
					tocFile.writeInt(sectorNumber.intValue());
					tocFile.writeInt(bufferedSectorNumber.intValue());
				}
				tocDirty = false;
			} catch (IOException e) {
				log.error("writeToc", e);
			}
		}
	}

	@Override
	public void readSector(int sectorNumber, byte[] buffer, int offset) throws IOException {
		Integer bufferedSectorNumber = toc.get(sectorNumber);
		if (bufferedSectorNumber != null) {
			fileAccess.seek(((long) sectorLength) * bufferedSectorNumber.intValue());
			fileAccess.read(buffer, offset, sectorLength);
			return;
		}

		if (sectorDevice == null) {
			log.warn(String.format("Reading outside the UMD buffer file (sector=0x%X)", sectorNumber));
			Arrays.fill(buffer, offset, offset + sectorLength, (byte) 0);
		} else {
			sectorDevice.readSector(sectorNumber, buffer, offset);
			fileAccess.seek(((long) sectorLength) * nextFreeBufferedSectorNumber);
			fileAccess.write(buffer, offset, sectorLength);
			toc.put(sectorNumber, nextFreeBufferedSectorNumber);

			nextFreeBufferedSectorNumber++;
			tocDirty = true;
		}
	}

	@Override
	public int getNumSectors() throws IOException {
		return numSectors;
	}

	@Override
	public void close() throws IOException {
		super.close();

		if (sectorDevice != null) {
			sectorDevice.close();
			sectorDevice = null;
		}

		writeToc();

		tocFile.close();
		tocFile = null;

		toc = null;
	}
}
