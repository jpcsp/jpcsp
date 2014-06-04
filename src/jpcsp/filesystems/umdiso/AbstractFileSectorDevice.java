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

import jpcsp.Emulator;

import org.apache.log4j.Logger;

public abstract class AbstractFileSectorDevice implements ISectorDevice {
	protected static Logger log = Emulator.log;
	protected RandomAccessFile fileAccess;

	public AbstractFileSectorDevice(RandomAccessFile fileAccess) {
		this.fileAccess = fileAccess;
	}

	@Override
	public int getNumSectors() throws IOException {
		return (int) (fileAccess.length() / sectorLength);
	}

	@Override
	public void close() throws IOException {
		fileAccess.close();
		fileAccess = null;
	}

	@Override
	public int readSectors(int sectorNumber, int numberSectors, byte[] buffer, int offset) throws IOException {
        for (int i = 0; i < numberSectors; i++) {
        	readSector(sectorNumber + i, buffer, offset + i * sectorLength);
        }

        return numberSectors;
	}

	@Override
	public void writeSector(int sectorNumber, byte[] buffer, int offset) throws IOException {
		throw new IOException("Device is read-only");
	}

	@Override
	public void writeSectors(int sectorNumber, int numberSectors, byte[] buffer, int offset) throws IOException {
		throw new IOException("Device is read-only");
	}
}
