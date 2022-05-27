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

/**
 * Interface defining a sector-oriented device.
 * Only sector-based operations can be performed on this device.
 * A sector is 2048 bytes long.
 *
 * @author gid15
 *
 */
public interface ISectorDevice {
	/**
	 * The size in bytes of a sector.
	 */
	public static final int sectorLength = 2048;

	/**
	 * @return the total number of sectors for this device
	 * @throws IOException
	 */
	public int getNumSectors() throws IOException;

	/**
	 * Read one sector of the device.
	 * 2048 bytes will be set in the buffer: buffer[offset..offset+2048-1].
	 * 
	 * @param sectorNumber  the sector number to be read. Must be in range 0 to getNumSectors() - 1.
	 * @param buffer        the buffer where to store the read bytes.
	 * @param offset        the offset inside the buffer where to start storing the read bytes.
	 * @throws IOException
	 */
	public void readSector(int sectorNumber, byte[] buffer, int offset) throws IOException;

	/**
	 * Read multiple sectors of the device.
	 * 2048 bytes will be set in the buffer for each sector: buffer[offset..offset+numberSectors*2048-1].
	 * 
	 * @param sectorNumber  the sector number of the first sector to be read. Must be in range 0 to getNumSectors() - 1.
	 * @param numberSectors the number of sectors to be read. Must be in range 0 to getNumSectors() - sectorNumber.
	 * @param buffer        the buffer where to store the read bytes.
	 * @param offset        the offset inside the buffer where to start storing the read bytes.
	 * @return
	 * @throws IOException
	 */
	public int readSectors(int sectorNumber, int numberSectors, byte[] buffer, int offset) throws IOException;

	/**
	 * Write one sector of the device.
	 * 2048 bytes of the buffer will be written: buffer[offset..offset+2048-1].
	 * Not all the devices support this operation. If the operation is not supported by the device,
	 * an IOException will be raised.
	 *
	 * @param sectorNumber  the sector number to be written. Must be in range 0 to getNumSectors() - 1.
	 * @param buffer        the buffer storing the bytes to be written.
	 * @param offset        the offset inside the buffer where the bytes are stored.
	 * @throws IOException
	 */
	public void writeSector(int sectorNumber, byte[] buffer, int offset) throws IOException;

	/**
	 * Write multiple sectors of the device.
	 * 2048 bytes of the buffer will be written for each sector: buffer[offset..offset+numberSectors*2048-1].
	 * Not all the devices support this operation. If the operation is not supported by the device,
	 * an IOException will be raised.
	 *
	 * @param sectorNumber  the sector number of the first sector to be written. Must be in range 0 to getNumSectors() - 1.
	 * @param numberSectors the number of sectors to be written. Must be in range 0 to getNumSectors() - sectorNumber.
	 * @param buffer        the buffer storing the bytes to be written.
	 * @param offset        the offset inside the buffer where the bytes are stored.
	 * @throws IOException
	 */
	public void writeSectors(int sectorNumber, int numberSectors, byte[] buffer, int offset) throws IOException;

	/**
	 * Close any associated resource.
	 * After a close, the device cannot be used any longer.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException;
}
