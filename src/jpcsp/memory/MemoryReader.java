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
	public static IMemoryReader getMemoryReader(int address, int length, int step) {
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

	public static IMemoryReader getMemoryReader(int address, int step) {
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

		return getMemoryReader(address, length, step);
	}

	private static class MemoryReaderGeneric implements IMemoryReader {
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
		public int readNext() {
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
	}

	private static class MemoryReaderInt8 implements IMemoryReader {
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
		public int readNext() {
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
	}

	private static class MemoryReaderInt16 implements IMemoryReader {
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
		public int readNext() {
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
	}

	private static class MemoryReaderInt32 implements IMemoryReader {
		private IntBuffer buffer;

		public MemoryReaderInt32(IntBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public int readNext() {
			return buffer.get();
		}
	}

	private static class MemoryReaderByte8 implements IMemoryReader {
		private ByteBuffer buffer;

		public MemoryReaderByte8(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public int readNext() {
			return ((int) buffer.get()) & 0xFF;
		}
	}

	private static class MemoryReaderByte16 implements IMemoryReader {
		private ByteBuffer buffer;

		public MemoryReaderByte16(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public int readNext() {
			return ((int) buffer.getShort()) & 0xFFFF;
		}
	}

	private static class MemoryReaderByte32 implements IMemoryReader {
		private ByteBuffer buffer;

		public MemoryReaderByte32(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public int readNext() {
			return buffer.getInt();
		}
	}
}
