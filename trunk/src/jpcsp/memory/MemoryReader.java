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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jpcsp.Memory;
import jpcsp.MemoryMap;

/**
 * @author gid15
 *
 */
public class MemoryReader {
	private static int getMaxLength(int address) {
		int length;
		
		if (address >= MemoryMap.START_RAM && address <= MemoryMap.END_RAM) {
			length = MemoryMap.END_RAM - address + 1;
		} else if (address >= MemoryMap.START_VRAM && address <= MemoryMap.END_VRAM) {
			length = MemoryMap.END_VRAM - address + 1;
		} else if (address >= MemoryMap.START_SCRATCHPAD && address <= MemoryMap.END_SCRATCHPAD) {
			length = MemoryMap.END_SCRATCHPAD - address + 1;
		} else {
			length = 0;
		}

		return length;
	}

	private static IMemoryReader getFastMemoryReader(FastMemory mem, int address, int step) {
		int[] memoryInt = mem.getAll();

		switch (step) {
		case 1: return new MemoryReaderIntArray8(memoryInt, address);
		case 2: return new MemoryReaderIntArray16(memoryInt, address);
		case 4: return new MemoryReaderIntArray32(memoryInt, address);
		}

		// Default (generic) MemoryReader
		return new MemoryReaderGeneric(address, getMaxLength(address), step);
	}

	/**
	 * Creates a MemoryReader to read values from memory.
	 *
	 * @param address the address where to start reading.
	 *                When step == 2, the address has to be 16-bit aligned ((address & 1) == 0).
	 *                When step == 4, the address has to be 32-bit aligned ((address & 3) == 0).
	 * @param length  the maximum number of bytes that can be read.
	 * @param step    when step == 1, read 8-bit values
	 *                when step == 2, read 16-bit values
	 *                when step == 4, read 32-bit values
	 *                other value for step are not allowed.
	 * @return        the MemoryReader
	 */
	public static IMemoryReader getMemoryReader(int address, int length, int step) {
		Memory mem = Memory.getInstance();

		address &= Memory.addressMask;
		if (mem instanceof FastMemory) {
			return getFastMemoryReader((FastMemory) mem, address, step);
		}
		Buffer buffer = Memory.getInstance().getBuffer(address, length);

		if (buffer instanceof IntBuffer) {
			IntBuffer intBuffer = (IntBuffer) buffer;
			switch (step) {
			case 1: return new MemoryReaderInt8(intBuffer, address & 0x03);
			case 2: return new MemoryReaderInt16(intBuffer, (address & 0x02) >> 1);
			case 4: return new MemoryReaderInt32(intBuffer);
			}
		} else if (buffer instanceof ByteBuffer) {
			ByteBuffer byteBuffer = (ByteBuffer) buffer;
			switch (step) {
			case 1: return new MemoryReaderByte8(byteBuffer);
			case 2: return new MemoryReaderByte16(byteBuffer);
			case 4: return new MemoryReaderByte32(byteBuffer);
			}
		}

		// Default (generic) MemoryReader
		return new MemoryReaderGeneric(address, length, step);
	}

	/**
	 * Creates a MemoryReader to read values from memory.
	 *
	 * @param address the address where to start reading.
	 *                When step == 2, the address has to be 16-bit aligned ((address & 1) == 0).
	 *                When step == 4, the address has to be 32-bit aligned ((address & 3) == 0).
	 * @param step    when step == 1, read 8-bit values
	 *                when step == 2, read 16-bit values
	 *                when step == 4, read 32-bit values
	 *                other value for step are not allowed.
	 * @return        the MemoryReader
	 */
	public static IMemoryReader getMemoryReader(int address, int step) {
		Memory mem = Memory.getInstance();

		address &= Memory.addressMask;
		if (mem instanceof FastMemory) {
			return getFastMemoryReader((FastMemory) mem, address, step);
		}
		return getMemoryReader(address, getMaxLength(address), step);
	}

	// The Java JIT compiler is producing slightly faster code for "final" methods.
	// Added "final" here only for performance reasons. Can be removed if inheritance
	// of these classes is required.
	private final static class MemoryReaderGeneric implements IMemoryReader {
		private Memory mem;
		private int address;
		private int length;
		private int step;

		public MemoryReaderGeneric(int address, int length, int step) {
			this.address = address;
			this.length = length;
			this.step = step;
			mem = Memory.getInstance();
		}

		@Override
		public final int readNext() {
			int n;

			if (length <= 0) {
				return 0;
			}

			switch (step) {
			case 1: n = mem.read8(address);  break;
			case 2: n = mem.read16(address); break;
			case 4: n = mem.read32(address); break;
			default: n = 0; break;
			}

			address += step;
			length -= step;

			return n;
		}

		@Override
		public final void skip(int n) {
			address += n * step;
			length -= n * step;
		}
	}

	private final static class MemoryReaderIntArray8 implements IMemoryReader {
		private int index;
		private int offset;
		private int value;
		private int[] buffer;

		public MemoryReaderIntArray8(int[] buffer, int addr) {
			this.buffer = buffer;
			offset = addr / 4;
			index = addr & 3;
			value = buffer[offset] >> (8 * index);
		}

		@Override
		public final int readNext() {
			int n;

			if (index == 4) {
				index = 0;
				offset++;
				value = buffer[offset];
			}
			n = value & 0xFF;
			value >>= 8;
			index++;

			return n;
		}

		@Override
		public final void skip(int n) {
			index += n;
			offset += index / 4;
			index &= 3;
			value = buffer[offset] >> (8 * index);
		}
	}

	private final static class MemoryReaderIntArray16 implements IMemoryReader {
		private int index;
		private int offset;
		private int value;
		private int[] buffer;

		public MemoryReaderIntArray16(int[] buffer, int addr) {
			this.buffer = buffer;
			offset = addr / 4;
			index = (addr / 2) & 1;
			if (index != 0) {
				value = buffer[offset++];
			}
		}

		@Override
		public final int readNext() {
			int n;

			if (index == 0) {
				value = buffer[offset++];
				n = value & 0xFFFF;
				index = 1;
			} else {
				index = 0;
				n = value >>> 16;
			}

			return n;
		}

		@Override
		public final void skip(int n) {
			index += n;
			offset += index / 2;
			index &= 1;
			if (index != 0) {
				value = buffer[offset++];
			}
		}
	}

	private final static class MemoryReaderIntArray32 implements IMemoryReader {
		private int offset;
		private int[] buffer;

		public MemoryReaderIntArray32(int[] buffer, int addr) {
			offset = addr / 4;
			this.buffer = buffer;
		}

		@Override
		public final int readNext() {
			return buffer[offset++];
		}

		@Override
		public final void skip(int n) {
			offset += n;
		}
	}

	private final static class MemoryReaderInt8 implements IMemoryReader {
		private int index;
		private int value;
		private IntBuffer buffer;

		public MemoryReaderInt8(IntBuffer buffer, int index) {
			this.buffer = buffer;
			this.index = index;
			if (buffer.capacity() > 0) {
				value = buffer.get() >> (8 * index);
			}
		}

		@Override
		public final int readNext() {
			int n;

			if (index == 4) {
				index = 0;
				value = buffer.get();
			}
			n = value & 0xFF;
			value >>= 8;
			index++;

			return n;
		}

		@Override
		public final void skip(int n) {
			index += n;
			buffer.position(buffer.position() + (index / 4));
			index &= 3;
			value = buffer.get() >> (8 * index);
		}
	}

	private final static class MemoryReaderInt16 implements IMemoryReader {
		private int index;
		private int value;
		private IntBuffer buffer;

		public MemoryReaderInt16(IntBuffer buffer, int index) {
			this.buffer = buffer;
			this.index = index;
			if (index != 0 && buffer.capacity() > 0) {
				value = buffer.get();
			}
		}

		@Override
		public final int readNext() {
			int n;

			if (index == 0) {
				value = buffer.get();
				n = value & 0xFFFF;
				index = 1;
			} else {
				index = 0;
				n = value >>> 16;
			}

			return n;
		}

		@Override
		public final void skip(int n) {
			index += n;
			buffer.position(buffer.position() + (index / 2));
			index &= 1;
			if (index != 0) {
				value = buffer.get();
			}
		}
	}

	private final static class MemoryReaderInt32 implements IMemoryReader {
		private IntBuffer buffer;

		public MemoryReaderInt32(IntBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public final int readNext() {
			return buffer.get();
		}

		@Override
		public final void skip(int n) {
			buffer.position(buffer.position() + n);
		}
	}

	private final static class MemoryReaderByte8 implements IMemoryReader {
		private ByteBuffer buffer;

		public MemoryReaderByte8(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public final int readNext() {
			return (buffer.get()) & 0xFF;
		}

		@Override
		public final void skip(int n) {
			buffer.position(buffer.position() + n);
		}
	}

	private final static class MemoryReaderByte16 implements IMemoryReader {
		private ByteBuffer buffer;

		public MemoryReaderByte16(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public final int readNext() {
			return (buffer.getShort()) & 0xFFFF;
		}

		@Override
		public final void skip(int n) {
			buffer.position(buffer.position() + n * 2);
		}
	}

	private final static class MemoryReaderByte32 implements IMemoryReader {
		private ByteBuffer buffer;

		public MemoryReaderByte32(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public final int readNext() {
			return buffer.getInt();
		}

		@Override
		public final void skip(int n) {
			buffer.position(buffer.position() + n * 4);
		}
	}
}
