/*

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

import jpcsp.Memory;

/**
 * @author gid15
 *
 */
public class MemoryReaderWriter {
	private static IMemoryReaderWriter getFastMemoryReaderWriter(FastMemory mem, int address, int step) {
		int[] memoryInt = mem.getAll();

		// Implement the most common cases with dedicated classes.
		switch (step) {
		case 2: return new MemoryReaderWriterIntArray16(memoryInt, address);
		case 4: return new MemoryReaderWriterIntArray32(memoryInt, address);
		}

		// No dedicated class available, use the generic one.
		return new MemoryReaderWriterGeneric(address, step);
	}

	public static IMemoryReaderWriter getMemoryReaderWriter(int address, int length, int step) {
		Memory mem = Memory.getInstance();

		address &= Memory.addressMask;
		if (mem instanceof FastMemory) {
			return getFastMemoryReaderWriter((FastMemory) mem, address, step);
		}

		// No dedicated class available, use the generic one.
		return new MemoryReaderWriterGeneric(address, length, step);
	}

	public static IMemoryReaderWriter getMemoryReaderWriter(int address, int step) {
		Memory mem = Memory.getInstance();

		address &= Memory.addressMask;
		if (mem instanceof FastMemory) {
			return getFastMemoryReaderWriter((FastMemory) mem, address, step);
		}

		// No dedicated class available, use the generic one.
		return new MemoryReaderWriterGeneric(address, step);
	}

	private static final class MemoryReaderWriterGeneric implements IMemoryReaderWriter {
		private IMemoryReader memoryReader;
		private IMemoryWriter memoryWriter;
		private int currentValue;

		public MemoryReaderWriterGeneric(int address, int length, int step) {
			memoryReader = MemoryReader.getMemoryReader(address, length, step);
			memoryWriter = MemoryWriter.getMemoryWriter(address, length, step);
			currentValue = memoryReader.readNext();
		}

		public MemoryReaderWriterGeneric(int address, int step) {
			memoryReader = MemoryReader.getMemoryReader(address, step);
			memoryWriter = MemoryWriter.getMemoryWriter(address, step);
			currentValue = memoryReader.readNext();
		}

		@Override
		public void writeNext(int value) {
			memoryWriter.writeNext(value);
			currentValue = memoryReader.readNext();
		}

		@Override
		public void skip(int n) {
			if (n > 0) {
				memoryWriter.skip(n);
				memoryReader.skip(n - 1);
				currentValue = memoryReader.readNext();
			}
		}

		@Override
		public void flush() {
			memoryWriter.flush();
		}

		@Override
		public int getCurrentAddress() {
			return memoryWriter.getCurrentAddress();
		}

		@Override
		public int readCurrent() {
			return currentValue;
		}
	}

	private static final class MemoryReaderWriterIntArray32 implements IMemoryReaderWriter {
		private int offset;
		private final int[] buffer;

		public MemoryReaderWriterIntArray32(int[] buffer, int addr) {
			offset = addr >> 2;
			this.buffer = buffer;
		}

		@Override
		public void writeNext(int value) {
			buffer[offset++] = value;
		}

		@Override
		public void skip(int n) {
			offset += n;
		}

		@Override
		public void flush() {
		}

		@Override
		public int getCurrentAddress() {
			return offset << 2;
		}

		@Override
		public int readCurrent() {
			return buffer[offset];
		}
	}

	private static final class MemoryReaderWriterIntArray16 implements IMemoryReaderWriter {
		private int index;
		private int offset;
		private int value;
		private final int[] buffer;

		public MemoryReaderWriterIntArray16(int[] buffer, int addr) {
			this.buffer = buffer;
			offset = addr >> 2;
			index = (addr >> 1) & 1;
			if (index != 0) {
				value = buffer[offset] & 0x0000FFFF;
			}
		}

		@Override
		public void writeNext(int n) {
			if (index == 0) {
				value = n & 0xFFFF;
				index = 1;
			} else {
				buffer[offset++] = (n << 16) | value;
				index = 0;
			}
		}

		@Override
		public void skip(int n) {
			if (n > 0) {
				flush();
				index += n;
				offset += index >> 1;
				index &= 1;
				if (index != 0) {
					value = buffer[offset] & 0x0000FFFF;
				}
			}
		}

		@Override
		public void flush() {
			if (index != 0) {
				buffer[offset] = (buffer[offset] & 0xFFFF0000) | value;
			}
		}

		@Override
		public int getCurrentAddress() {
			return (offset << 2) + (index << 1);
		}

		@Override
		public int readCurrent() {
			if (index == 0) {
				return buffer[offset] & 0xFFFF;
			}
			return buffer[offset] >>> 16;
		}
	}
}
