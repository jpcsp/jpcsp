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
package jpcsp.HLE.VFS.iso;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_FILE_READ_ERROR;
import static jpcsp.filesystems.umdiso.UmdIsoFile.sectorLength;

import java.io.IOException;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFile;
import jpcsp.filesystems.umdiso.UmdIsoReader;

public class UmdIsoReaderVirtualFile extends AbstractVirtualFile {
	private UmdIsoReader iso;
	private long position;
	private final byte[] buffer = new byte[sectorLength];

	public UmdIsoReaderVirtualFile(String fileName) throws IOException {
		super(null);
		iso = new UmdIsoReader(fileName);
	}

	@Override
	public long getPosition() {
		return position;
	}

	private int getSectorNumber() {
		return (int) (position / sectorLength);
	}

	private int getSectorOffset() {
		return (int) (position % sectorLength);
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		int readLength = 0;
		int outputOffset = 0;
		while (outputLength > 0) {
			try {
				iso.readSector(getSectorNumber(), buffer);
			} catch (IOException e) {
				log.error("ioRead", e);
				return ERROR_KERNEL_FILE_READ_ERROR;
			}

			int sectorOffset = getSectorOffset();
			int length = Math.min(sectorLength - sectorOffset, outputLength);
			outputPointer.setArray(outputOffset, buffer, sectorOffset, length);

			readLength += length;
			outputOffset += length;
			position += length;
			outputLength -= length;
		}

		return readLength;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		int readLength = 0;
		while (outputLength > 0) {
			int sectorOffset = getSectorOffset();
			int length;

			// Can we read one or multiple sectors directly into the outputBuffer?
			if (sectorOffset == 0 && outputLength >= sectorLength) {
				try {
					int numberSectors = outputLength / sectorLength;
					iso.readSectors(getSectorNumber(), numberSectors, outputBuffer, outputOffset);

					length = numberSectors * sectorLength;
				} catch (IOException e) {
					log.error("ioRead", e);
					return ERROR_KERNEL_FILE_READ_ERROR;
				}
			} else {
				try {
					iso.readSector(getSectorNumber(), buffer);

					length = Math.min(sectorLength - sectorOffset, outputLength);
					System.arraycopy(buffer, sectorOffset, outputBuffer, outputOffset, length);
				} catch (IOException e) {
					log.error("ioRead", e);
					return ERROR_KERNEL_FILE_READ_ERROR;
				}
			}

			readLength += length;
			outputOffset += length;
			position += length;
			outputLength -= length;
		}

		return readLength;
	}

	@Override
	public long ioLseek(long offset) {
		position = offset;

		return offset;
	}

	@Override
	public int ioClose() {
		try {
			iso.close();
		} catch (IOException e) {
			log.error("ioClose", e);
			return IO_ERROR;
		}

		return 0;
	}

	@Override
	public long length() {
		return iso.getNumSectors() * (long) sectorLength;
	}

	public boolean hasFile(String fileName) {
		return iso.hasFile(fileName);
	}

	@Override
	public String toString() {
		return String.format("%s, position=0x%X", iso, position);
	}
}
