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
package jpcsp.memory;

/**
 * @author gid15
 *
 */
public class BufferedMemoryReader {
	private IMemoryReader memoryReader;
	private int readValue;
	private int index;

	public BufferedMemoryReader(int address) {
		memoryReader = MemoryReader.getMemoryReader(address, 4);
		init(address);
	}

	public BufferedMemoryReader(int address, int length) {
		memoryReader = MemoryReader.getMemoryReader(address, length, 4);
		init(address);
	}

	private void init(int address) {
		index = address & 0x03;
		if (index > 0) {
			readValue = memoryReader.readNext();
		}
	}

	public int readNext32() {
		index = 0;
		return memoryReader.readNext();
	}

	public void skipNext32(int count) {
		index = 0;
		memoryReader.skip(count);
	}

	public void skipNext32() {
		skipNext32(1);
	}

	public int readNext16() {
		if (index == 0 || index == 3) {
			index = 2;
			readValue = memoryReader.readNext();
			return readValue & 0xFFFF;
		}
		index = 0;
		return readValue >>> 16;
	}

	public void skipNext16() {
		if (index == 0 || index == 3) {
			index = 2;
			readValue = memoryReader.readNext();
		} else {
			index = 0;
		}
	}

	public int readNext8() {
		switch (index) {
		case 0:
			readValue = memoryReader.readNext();
			index = 1;
			return (readValue & 0xFF);
		case 1:
			index = 2;
			return (readValue >> 8) & 0xFF;
		case 2:
			index = 3;
			return (readValue >> 16) & 0xFF;
		default: // index == 3
			index = 0;
			return (readValue >>> 24);
		}
	}

	public void skipNext8() {
		if (index == 0) {
			readValue = memoryReader.readNext();
			index = 1;
		} else if (index == 3) {
			index = 0;
		} else {
			index++;
		}
	}

	public float readNextFloat() {
		index = 0;
		return Float.intBitsToFloat(memoryReader.readNext());
	}

	public void align16() {
		index = (index + 1) & ~1;
	}

	public void align32() {
		index = 0;
	}
}
