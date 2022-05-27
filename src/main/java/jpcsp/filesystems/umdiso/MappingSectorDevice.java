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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import jpcsp.Emulator;

import org.apache.log4j.Logger;

/**
 * Implements a SectorDevice where the sectors are stored in random
 * order in another SectorDevice. Not all the sectors need to be available.
 * Writing a new sectors is also supported by adding the new sectors at the
 * end of the mapped SectorDevice.
 * 
 * @author gid15
 *
 */
public class MappingSectorDevice implements ISectorDevice {
	protected static Logger log = Emulator.log;
	protected static final int freeSectorNumber = -1;
	protected ISectorDevice sectorDevice;
	protected int[] sectorMapping;
	protected File mappingFile;
	protected boolean sectorMappingDirty;

	public MappingSectorDevice(ISectorDevice sectorDevice, File mappingFile) {
		this.sectorDevice = sectorDevice;
		this.mappingFile = mappingFile;

		// Create a default empty mapping in case the mapping file cannot be read
		sectorMapping = new int[0];
		sectorMappingDirty = true;

		try {
			readMappingFile();
		} catch (FileNotFoundException e) {
			log.debug("Mapping file not found, creating it", e);
		} catch (IOException e) {
			log.warn("Error reading mapping file", e);
		}
	}

	public void setNumSectors(int numSectors) throws IOException {
		int previousNumSectors = getNumSectors();
		if (numSectors != previousNumSectors) {
			// Shrink or extend the sectorMapping array
			sectorMapping = Arrays.copyOf(sectorMapping, numSectors);
			if (numSectors > previousNumSectors) {
				// Extending the sector mapping with -1 values
				Arrays.fill(sectorMapping, previousNumSectors, sectorMapping.length, freeSectorNumber);
			}
			sectorMappingDirty = true;
		}
	}

	protected void readMappingFile() throws IOException {
		InputStream mappingFileReader = new FileInputStream(mappingFile);
		int mappingSize = (int) (mappingFile.length() / 4);
		sectorMapping = new int[mappingSize];
		byte[] buffer = new byte[4];
		IntBuffer intBuffer = ByteBuffer.wrap(buffer).asIntBuffer();
		for (int i = 0; i < mappingSize; i++) {
			mappingFileReader.read(buffer);
			sectorMapping[i] = intBuffer.get(0);
		}
		mappingFileReader.close();

		sectorMappingDirty = false;
	}

	protected void writeMappingFile() throws IOException {
		OutputStream mappingFileWriter = new FileOutputStream(mappingFile);
		byte[] buffer = new byte[4];
		IntBuffer intBuffer = ByteBuffer.wrap(buffer).asIntBuffer();
		int numSectors = getNumSectors();
		for (int i = 0; i < numSectors; i++) {
			intBuffer.put(0, sectorMapping[i]);
			mappingFileWriter.write(buffer);
		}
		mappingFileWriter.close();

		sectorMappingDirty = false;
	}

	protected int mapSector(int sectorNumber) throws IOException {
		if (sectorNumber >= 0 && sectorNumber < getNumSectors()) {
			return sectorMapping[sectorNumber];
		}

		return freeSectorNumber;
	}

	protected int getFreeSectorNumber() throws IOException {
		int numSectors = getNumSectors();
		for (int i = 0; i < numSectors; i++) {
			if (sectorMapping[i] == freeSectorNumber) {
				return i;
			}
		}

		return freeSectorNumber;
	}

	protected void setSectorMapping(int sectorNumber, int mappedSectorNumber) {
		sectorMapping[sectorNumber] = mappedSectorNumber;
		sectorMappingDirty = true;
	}

	@Override
	public void readSector(int sectorNumber, byte[] buffer, int offset) throws IOException {
		int mappedSectorNumber = mapSector(sectorNumber);
		if (mappedSectorNumber >= 0) {
			sectorDevice.readSector(mappedSectorNumber, buffer, offset);
		} else {
			Arrays.fill(buffer, offset, offset + sectorLength, (byte) 0);
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
	public int getNumSectors() throws IOException {
		return sectorMapping.length;
	}

	@Override
	public void close() throws IOException {
		// Write back the mapping file if it has been changed
		if (sectorMappingDirty) {
			writeMappingFile();
		}

		sectorDevice.close();
	}

	@Override
	public void writeSector(int sectorNumber, byte[] buffer, int offset) throws IOException {
		int freeSectorNumber = getFreeSectorNumber();
		if (freeSectorNumber < 0) {
			throw new IOException(String.format("Sector Device '%s' is full", mappingFile));
		}

		sectorDevice.writeSector(freeSectorNumber, buffer, offset);
		setSectorMapping(freeSectorNumber, sectorNumber);
	}

	@Override
	public void writeSectors(int sectorNumber, int numberSectors, byte[] buffer, int offset) throws IOException {
		for (int i = 0; i < numberSectors; i++) {
			writeSector(sectorNumber + i, buffer, offset + i * sectorLength);
		}
	}
}
