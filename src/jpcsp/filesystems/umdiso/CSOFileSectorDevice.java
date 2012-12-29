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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.bolet.jgz.Inflater;

public class CSOFileSectorDevice extends ISOFileSectorDevice {
	protected int offsetShift;
	protected int numSectors;
	protected long[] sectorOffsets;
	private static final long sectorOffsetMask = 0x7FFFFFFFL;

	public CSOFileSectorDevice(RandomAccessFile fileAccess, byte[] header) throws IOException {
		super(fileAccess);
		ByteBuffer byteBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);

        /*
	        u32 'CISO'
	        u32 0?
	        u32 image size in bytes (why? it could have been sectors and make things simpler!)
	        u32 sector size? (00000800 = 2048 = sector size)
	        u32 ? (1)
	        u32[] sector offsets (as many as image size / sector size, I guess)
         */

		int lengthInBytes = byteBuffer.getInt(8);
		int sectorSize = byteBuffer.getInt(16);
		offsetShift = byteBuffer.get(21) & 0xFF;
		numSectors = lengthInBytes / sectorSize;
		sectorOffsets = new long[numSectors + 1];

		byte[] offsetData = new byte[(numSectors + 1) * 4];
		fileAccess.readFully(offsetData);
		ByteBuffer offsetBuffer = ByteBuffer.wrap(offsetData).order(ByteOrder.LITTLE_ENDIAN);

		for (int i = 0; i <= numSectors; i++) {
			sectorOffsets[i] = offsetBuffer.getInt(i * 4) & 0xFFFFFFFFL;
			if (i > 0) {
				if ((sectorOffsets[i] & sectorOffsetMask) < (sectorOffsets[i - 1] & sectorOffsetMask)) {
                    throw new IOException(String.format("Invalid offset [%d]: 0x%08X < 0x%08X", i, sectorOffsets[i], sectorOffsets[i - 1]));
				}
			}
		}
	}

	@Override
	public int getNumSectors() {
		return numSectors;
	}

	@Override
	public void readSector(int sectorNumber, byte[] buffer, int offset) throws IOException {
		long sectorOffset = sectorOffsets[sectorNumber];
        long sectorEnd = sectorOffsets[sectorNumber + 1];

        if ((sectorOffset & 0x80000000) != 0) {
            long realOffset = (sectorOffset & sectorOffsetMask) << offsetShift;
            fileAccess.seek(realOffset);
            fileAccess.read(buffer, offset, sectorLength);
        } else {
	        sectorEnd = (sectorEnd & sectorOffsetMask) << offsetShift;
	        sectorOffset = (sectorOffset & sectorOffsetMask) << offsetShift;

	        int compressedLength = (int) (sectorEnd - sectorOffset);
	        if (compressedLength < 0) {
	        	Arrays.fill(buffer, offset, offset + sectorLength, (byte) 0);
	        } else {
		        byte[] compressedData = new byte[compressedLength];
		        fileAccess.seek(sectorOffset);
		        fileAccess.read(compressedData);

		        try {
		            Inflater inf = new Inflater();
		            ByteArrayInputStream b = new ByteArrayInputStream(compressedData);
		            inf.reset(b);
		            inf.readAll(buffer, offset, sectorLength);
		        } catch (IOException e) {
		            throw new IOException(String.format("Exception while uncompressing sector %d", sectorNumber));
		        }
	        }
        }
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
		throw new IOException("CSO is read-only");
	}

	@Override
	public void writeSectors(int sectorNumber, int numberSectors, byte[] buffer, int offset) throws IOException {
		throw new IOException("CSO is read-only");
	}
}
