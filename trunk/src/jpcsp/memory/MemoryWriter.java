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
public class MemoryWriter {
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

	private static IMemoryWriter getFastMemoryWriter(FastMemory mem, int address, int step) {
		int[] memoryInt = mem.getAll();

		switch (step) {
		case 1: return new MemoryWriterIntArray8(memoryInt, address);
		case 2: return new MemoryWriterIntArray16(memoryInt, address);
		case 4: return new MemoryWriterIntArray32(memoryInt, address);
		}

		// Default (generic) MemoryWriter
		return new MemoryWriterGeneric(address, getMaxLength(address), step);
	}

	/**
	 * Creates a MemoryWriter to write values from memory.
	 *
	 * @param address the address where to start writing.
	 *                When step == 2, the address has to be 16-bit aligned ((address & 1) == 0).
	 *                When step == 4, the address has to be 32-bit aligned ((address & 3) == 0).
	 * @param length  the maximum number of bytes that can be written.
	 * @param step    when step == 1, write 8-bit values
	 *                when step == 2, write 16-bit values
	 *                when step == 4, write 32-bit values
	 *                other value for step are not allowed.
	 * @return        the MemoryWriter
	 */
	public static IMemoryWriter getMemoryWriter(int address, int length, int step) {
		Memory mem = Memory.getInstance();

		address &= Memory.addressMask;
		if (mem instanceof FastMemory) {
			return getFastMemoryWriter((FastMemory) mem, address, step);
		}
		Buffer buffer = Memory.getInstance().getBuffer(address, length);

		if (buffer instanceof IntBuffer) {
			IntBuffer intBuffer = (IntBuffer) buffer;
			switch (step) {
			case 1: return new MemoryWriterInt8(intBuffer, address & 0x03);
			case 2: return new MemoryWriterInt16(intBuffer, (address & 0x02) >> 1);
			case 4: return new MemoryWriterInt32(intBuffer);
			}
		} else if (buffer instanceof ByteBuffer) {
			ByteBuffer byteBuffer = (ByteBuffer) buffer;
			switch (step) {
			case 1: return new MemoryWriterByte8(byteBuffer);
			case 2: return new MemoryWriterByte16(byteBuffer);
			case 4: return new MemoryWriterByte32(byteBuffer);
			}
		}

		// Default (generic) MemoryWriter
		return new MemoryWriterGeneric(address, length, step);
	}

	/**
	 * Creates a MemoryWriter to write values from memory.
	 *
	 * @param address the address where to start writing.
	 *                When step == 2, the address has to be 16-bit aligned ((address & 1) == 0).
	 *                When step == 4, the address has to be 32-bit aligned ((address & 3) == 0).
	 * @param step    when step == 1, write 8-bit values
	 *                when step == 2, write 16-bit values
	 *                when step == 4, write 32-bit values
	 *                other value for step are not allowed.
	 * @return        the MemoryWriter
	 */
	public static IMemoryWriter getMemoryWriter(int address, int step) {
		Memory mem = Memory.getInstance();

		address &= Memory.addressMask;
		if (mem instanceof FastMemory) {
			return getFastMemoryWriter((FastMemory) mem, address, step);
		}
		return getMemoryWriter(address, getMaxLength(address), step);
	}

	private static class MemoryWriterGeneric implements IMemoryWriter {
		private Memory mem;
		private int address;
		private int length;
		private int step;

		public MemoryWriterGeneric(int address, int length, int step) {
			this.address = address;
			this.length = length;
			this.step = step;
			mem = Memory.getInstance();
		}

		@Override
		public void writeNext(int value) {
			if (length <= 0) {
				return;
			}

			switch (step) {
			case 1: mem.write8(address, (byte) value);  break;
			case 2: mem.write16(address, (short) value); break;
			case 4: mem.write32(address, value); break;
			}

			address += step;
			length -= step;
		}

		@Override
		public void flush() {
		}
	}

	private static class MemoryWriterIntArray8 implements IMemoryWriter {
		private int index;
		private int offset;
		private int value;
		private int[] buffer;
		private static final int mask[] = { 0, 0x000000FF, 0x0000FFFF, 0x00FFFFFF, 0xFFFFFFFF };

		public MemoryWriterIntArray8(int[] buffer, int addr) {
			this.buffer = buffer;
			offset = addr / 4;
			index = addr & 3;
			if (index > 0) {
				value = buffer[offset] & mask[index];
			}
		}

		@Override
		public void writeNext(int n) {
			n &= 0xFF;
			if (index == 4) {
				buffer[offset++] = value;
				value = n;
				index = 1;
			} else {
				value |= (n << (index << 3));
				index++;
			}
		}

		@Override
		public void flush() {
			if (index > 0) {
				buffer[offset] = (buffer[offset] & ~mask[index]) | value;
			}
		}
	}

	private static class MemoryWriterIntArray16 implements IMemoryWriter {
		private int index;
		private int offset;
		private int value;
		private int[] buffer;

		public MemoryWriterIntArray16(int[] buffer, int addr) {
			this.buffer = buffer;
			offset = addr / 4;
			index = (addr / 2) & 1;
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
		public void flush() {
			if (index != 0) {
				buffer[offset] = (buffer[offset] & 0xFFFF0000) | value;
			}
		}
	}

	private static class MemoryWriterIntArray32 implements IMemoryWriter {
		private int offset;
		private int[] buffer;

		public MemoryWriterIntArray32(int[] buffer, int addr) {
			offset = addr / 4;
			this.buffer = buffer;
		}

		@Override
		public void writeNext(int value) {
			buffer[offset++] = value;
		}

		@Override
		public void flush() {
		}
	}

	private static class MemoryWriterInt8 implements IMemoryWriter {
		private int index;
		private int value;
		private IntBuffer buffer;
		private static final int mask[] = { 0, 0x000000FF, 0x0000FFFF, 0x00FFFFFF, 0xFFFFFFFF };

		public MemoryWriterInt8(IntBuffer buffer, int index) {
			this.buffer = buffer;
			this.index = index;
			if (index > 0 && buffer.capacity() > 0) {
				value = buffer.get(buffer.position()) & mask[index];
			}
		}

		@Override
		public void writeNext(int n) {
			n &= 0xFF;
			if (index == 4) {
				buffer.put(value);
				value = n;
				index = 1;
			} else {
				value |= (n << (index << 3));
				index++;
			}
		}

		@Override
		public void flush() {
			if (index > 0) {
				buffer.put((buffer.get(buffer.position()) & ~mask[index]) | value);
			}
		}
	}

	private static class MemoryWriterInt16 implements IMemoryWriter {
		private int index;
		private int value;
		private IntBuffer buffer;

		public MemoryWriterInt16(IntBuffer buffer, int index) {
			this.buffer = buffer;
			this.index = index;
			if (index != 0 && buffer.capacity() > 0) {
				value = buffer.get(buffer.position()) & 0x0000FFFF;
			}
		}

		@Override
		public void writeNext(int n) {
			if (index == 0) {
				value = n & 0xFFFF;
				index = 1;
			} else {
				buffer.put((n << 16) | value);
				index = 0;
			}
		}

		@Override
		public void flush() {
			if (index != 0) {
				buffer.put((buffer.get(buffer.position()) & 0xFFFF0000) | value);
			}
		}
	}

	private static class MemoryWriterInt32 implements IMemoryWriter {
		private IntBuffer buffer;

		public MemoryWriterInt32(IntBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public void writeNext(int value) {
			buffer.put(value);
		}

		@Override
		public void flush() {
		}
	}

	private static class MemoryWriterByte8 implements IMemoryWriter {
		private ByteBuffer buffer;

		public MemoryWriterByte8(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public void writeNext(int value) {
			buffer.put((byte) value);
		}

		@Override
		public void flush() {
		}
	}

	private static class MemoryWriterByte16 implements IMemoryWriter {
		private ByteBuffer buffer;

		public MemoryWriterByte16(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public void writeNext(int value) {
			buffer.putShort((short) value);
		}

		@Override
		public void flush() {
		}
	}

	private static class MemoryWriterByte32 implements IMemoryWriter {
		private ByteBuffer buffer;

		public MemoryWriterByte32(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public void writeNext(int value) {
			buffer.putInt(value);
		}

		@Override
		public void flush() {
		}
	}
}
