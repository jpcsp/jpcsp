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

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.TPointer;

/**
 * @author gid15
 *
 */
public class MemoryReader {
	private static int getMaxLength(int rawAddress) {
		int length;

		int address = rawAddress & Memory.addressMask;

		if (address >= MemoryMap.START_RAM && address <= MemoryMap.END_RAM) {
			length = MemoryMap.END_RAM - address + 1;
		} else if (address >= MemoryMap.START_VRAM && address <= MemoryMap.END_VRAM) {
			length = MemoryMap.END_VRAM - address + 1;
		} else if (address >= MemoryMap.START_SCRATCHPAD && address <= MemoryMap.END_SCRATCHPAD) {
			length = MemoryMap.END_SCRATCHPAD - address + 1;
		} else if (rawAddress >= MemoryMap.START_IO_0 && rawAddress <= MemoryMap.END_IO_1) {
			length = MemoryMap.END_IO_1 - rawAddress + 1;
		} else {
			length = 0;
		}

		return length;
	}

	private static IMemoryReader getFastMemoryReader(int address, int step) {
		int[] memoryInt = RuntimeContext.getMemoryInt();

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
		if (RuntimeContext.hasMemoryInt(address)) {
			return getFastMemoryReader(address, step);
		}

		if (!DebuggerMemory.isInstalled()) {
			Buffer buffer = Emulator.getMemory(address).getBuffer(address, length);

			if (buffer instanceof IntBuffer) {
				IntBuffer intBuffer = (IntBuffer) buffer;
				switch (step) {
				case 1: return new MemoryReaderInt8(intBuffer, address);
				case 2: return new MemoryReaderInt16(intBuffer, address);
				case 4: return new MemoryReaderInt32(intBuffer, address);
				}
			} else if (buffer instanceof ByteBuffer) {
				ByteBuffer byteBuffer = (ByteBuffer) buffer;
				switch (step) {
				case 1: return new MemoryReaderByte8(byteBuffer, address);
				case 2: return new MemoryReaderByte16(byteBuffer, address);
				case 4: return new MemoryReaderByte32(byteBuffer, address);
				}
			}
		}

		// Default (generic) MemoryReader
		return new MemoryReaderGeneric(address, length, step);
	}

	/**
	 * Creates a MemoryReader to read values from memory.
	 *
	 * @param mem     the memory to be used.
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
	public static IMemoryReader getMemoryReader(Memory mem, int address, int length, int step) {
		// Use the optimized version if we are just using the standard memory
		if (mem == RuntimeContext.memory) {
			return getMemoryReader(address, length, step);
		}

		// Default (generic) MemoryReader
		return new MemoryReaderGeneric(mem, address, length, step);
	}

	/**
	 * Creates a MemoryReader to read values from memory.
	 *
	 * @param address the address and memory where to start reading.
	 * @param length  the maximum number of bytes that can be read.
	 * @param step    when step == 1, read 8-bit values
	 *                when step == 2, read 16-bit values
	 *                when step == 4, read 32-bit values
	 *                other value for step are not allowed.
	 * @return        the MemoryReader
	 */
	public static IMemoryReader getMemoryReader(TPointer address, int length, int step) {
		return getMemoryReader(address.getMemory(), address.getAddress(), length, step);
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
		if (RuntimeContext.hasMemoryInt(address)) {
			return getFastMemoryReader(address, step);
		}
		return getMemoryReader(address, getMaxLength(address), step);
	}

	/**
	 * Creates a MemoryReader to read values from memory.
	 *
	 * @param mem     the memory to be used.
	 * @param address the address where to start reading.
	 *                When step == 2, the address has to be 16-bit aligned ((address & 1) == 0).
	 *                When step == 4, the address has to be 32-bit aligned ((address & 3) == 0).
	 * @param step    when step == 1, read 8-bit values
	 *                when step == 2, read 16-bit values
	 *                when step == 4, read 32-bit values
	 *                other value for step are not allowed.
	 * @return        the MemoryReader
	 */
	public static IMemoryReader getMemoryReader(Memory mem, int address, int step) {
		return getMemoryReader(mem, address, getMaxLength(address), step);
	}

	public static IMemoryReader getMemoryReader(int address, byte[] bytes, int offset, int length, int step) {
		switch (step) {
		case 1: return new MemoryReaderBytes8(address, bytes, offset, length);
		case 2: return new MemoryReaderBytes16(address, bytes, offset, length);
		case 4: return new MemoryReaderBytes32(address, bytes, offset, length);
		}
		return null;
	}

	public static IMemoryReader getMemoryReader(int address, int[] ints, int offset, int length) {
		return new MemoryReaderInts32(address, ints, offset, length);
		
	}

	// The Java JIT compiler is producing slightly faster code for "final" methods.
	// Added "final" here only for performance reasons. Can be removed if inheritance
	// of these classes is required.
	private final static class MemoryReaderGeneric implements IMemoryReader {
		private final Memory mem;
		private int address;
		private int length;
		private final int step;

		public MemoryReaderGeneric(Memory mem, int address, int length, int step) {
			this.mem = mem;
			this.address = address;
			this.length = length;
			this.step = step;
		}

		public MemoryReaderGeneric(int address, int length, int step) {
			this.address = address;
			this.length = length;
			this.step = step;
			mem = Emulator.getMemory(address);
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

		@Override
		public int getCurrentAddress() {
			return address;
		}
	}

	private final static class MemoryReaderIntArray8 implements IMemoryReader {
		private int index;
		private int offset;
		private int value;
		private int[] buffer;

		public MemoryReaderIntArray8(int[] buffer, int addr) {
			this.buffer = buffer;
			offset = (addr & Memory.addressMask) >> 2;
			index = addr & 3;
			value = buffer[offset] >> (index << 3);
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
			if (n != 0) {
				index += n;
				offset += index >> 2;
				index &= 3;
				value = buffer[offset] >> (index << 3);
			}
		}

		@Override
		public int getCurrentAddress() {
			return (offset << 2) + index;
		}
	}

	private final static class MemoryReaderIntArray16 implements IMemoryReader {
		private int index;
		private int offset;
		private int value;
		private int[] buffer;

		public MemoryReaderIntArray16(int[] buffer, int addr) {
			this.buffer = buffer;
			offset = (addr & Memory.addressMask) >> 2;
			index = (addr >> 1) & 1;
			if (index != 0) {
				value = buffer[offset];
			}
		}

		@Override
		public final int readNext() {
			int n;

			if (index == 0) {
				value = buffer[offset];
				n = value & 0xFFFF;
				index = 1;
			} else {
				index = 0;
				offset++;
				n = value >>> 16;
			}

			return n;
		}

		@Override
		public final void skip(int n) {
			if (n != 0) {
				index += n;
				offset += index >> 1;
				index &= 1;
				if (index != 0) {
					value = buffer[offset];
				}
			}
		}

		@Override
		public int getCurrentAddress() {
			return (offset << 2) + (index << 1);
		}
	}

	private final static class MemoryReaderIntArray32 implements IMemoryReader {
		private int offset;
		private int[] buffer;

		public MemoryReaderIntArray32(int[] buffer, int addr) {
			offset = (addr & Memory.addressMask) >> 2;
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

		@Override
		public int getCurrentAddress() {
			return offset << 2;
		}
	}

	private final static class MemoryReaderInt8 implements IMemoryReader {
		private int index;
		private int value;
		private IntBuffer buffer;
		private int address;

		public MemoryReaderInt8(IntBuffer buffer, int index) {
			this.buffer = buffer;
			this.address = address & ~3;
			index = address & 0x03;
			if (buffer.capacity() > 0) {
				value = buffer.get() >> (index << 3);
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
			if (n != 0) {
				index += n;
				buffer.position(buffer.position() + (index >> 2));
				index &= 3;
				value = buffer.get() >> (8 * index);
			}
		}

		@Override
		public int getCurrentAddress() {
			return address + (buffer.position() << 2) + index;
		}
	}

	private final static class MemoryReaderInt16 implements IMemoryReader {
		private int index;
		private int value;
		private IntBuffer buffer;
		private int address;

		public MemoryReaderInt16(IntBuffer buffer, int index) {
			this.buffer = buffer;
			this.address = address & ~3;
			this.index = (address & 0x02) >> 1;
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
			if (n != 0) {
				index += n;
				buffer.position(buffer.position() + (index >> 1));
				index &= 1;
				if (index != 0) {
					value = buffer.get();
				}
			}
		}

		@Override
		public int getCurrentAddress() {
			return address + (buffer.position() << 2) + index;
		}
	}

	private final static class MemoryReaderInt32 implements IMemoryReader {
		private IntBuffer buffer;
		private int address;

		public MemoryReaderInt32(IntBuffer buffer, int address) {
			this.buffer = buffer;
			this.address = address;
		}

		@Override
		public final int readNext() {
			return buffer.get();
		}

		@Override
		public final void skip(int n) {
			if (n != 0) {
				buffer.position(buffer.position() + n);
			}
		}

		@Override
		public int getCurrentAddress() {
			return address + (buffer.position() << 2);
		}
	}

	private final static class MemoryReaderByte8 implements IMemoryReader {
		private ByteBuffer buffer;
		private int address;

		public MemoryReaderByte8(ByteBuffer buffer, int address) {
			this.buffer = buffer;
			this.address = address;
		}

		@Override
		public final int readNext() {
			return (buffer.get()) & 0xFF;
		}

		@Override
		public final void skip(int n) {
			if (n != 0) {
				buffer.position(buffer.position() + n);
			}
		}

		@Override
		public int getCurrentAddress() {
			return address + buffer.position();
		}
	}

	private final static class MemoryReaderByte16 implements IMemoryReader {
		private ByteBuffer buffer;
		private int address;

		public MemoryReaderByte16(ByteBuffer buffer, int address) {
			this.buffer = buffer;
			this.address = address;
		}

		@Override
		public final int readNext() {
			return (buffer.getShort()) & 0xFFFF;
		}

		@Override
		public final void skip(int n) {
			if (n != 0) {
				buffer.position(buffer.position() + (n << 1));
			}
		}

		@Override
		public int getCurrentAddress() {
			return address + buffer.position();
		}
	}

	private final static class MemoryReaderByte32 implements IMemoryReader {
		private ByteBuffer buffer;
		private int address;

		public MemoryReaderByte32(ByteBuffer buffer, int address) {
			this.buffer = buffer;
			this.address = address;
		}

		@Override
		public final int readNext() {
			return buffer.getInt();
		}

		@Override
		public final void skip(int n) {
			if (n != 0) {
				buffer.position(buffer.position() + (n << 2));
			}
		}

		@Override
		public int getCurrentAddress() {
			return address + buffer.position();
		}
	}

	private final static class MemoryReaderBytes8 implements IMemoryReader {
		private int address;
		private final byte[] bytes;
		private int offset;
		private int maxOffset;

		public MemoryReaderBytes8(int address, byte[] bytes, int offset, int length) {
			this.address = address;
			this.bytes = bytes;
			this.offset = offset;
			maxOffset = offset + length;
		}

		@Override
		public int readNext() {
			if (offset >= maxOffset) {
				return 0;
			}
			address++;
			return bytes[offset++] & 0xFF;
		}

		@Override
		public void skip(int n) {
			offset += n;
			address += n;
		}

		@Override
		public int getCurrentAddress() {
			return address;
		}
	}

	private final static class MemoryReaderBytes16 implements IMemoryReader {
		private int address;
		private final byte[] bytes;
		private int offset;
		private int maxOffset;

		public MemoryReaderBytes16(int address, byte[] bytes, int offset, int length) {
			this.address = address;
			this.bytes = bytes;
			this.offset = offset;
			maxOffset = offset + length;
		}

		@Override
		public int readNext() {
			if (offset >= maxOffset) {
				return 0;
			}
			address += 2;
			return (bytes[offset++] & 0xFF) | ((bytes[offset++] & 0xFF) << 8);
		}

		@Override
		public void skip(int n) {
			offset += n * 2;
			address += n * 2;
		}

		@Override
		public int getCurrentAddress() {
			return address;
		}
	}

	private final static class MemoryReaderBytes32 implements IMemoryReader {
		private int address;
		private final byte[] bytes;
		private int offset;
		private int maxOffset;

		public MemoryReaderBytes32(int address, byte[] bytes, int offset, int length) {
			this.address = address;
			this.bytes = bytes;
			this.offset = offset;
			maxOffset = offset + length;
		}

		@Override
		public int readNext() {
			if (offset >= maxOffset) {
				return 0;
			}
			address += 4;
			return (bytes[offset++] & 0xFF) |
			       ((bytes[offset++] & 0xFF) << 8) |
			       ((bytes[offset++] & 0xFF) << 16) |
			       ((bytes[offset++] & 0xFF) << 24);
		}

		@Override
		public void skip(int n) {
			offset += n * 4;
			address += n * 4;
		}

		@Override
		public int getCurrentAddress() {
			return address;
		}
	}

	private final static class MemoryReaderInts32 implements IMemoryReader {
		private int address;
		private final int[] ints;
		private int offset;
		private int maxOffset;

		public MemoryReaderInts32(int address, int[] ints, int offset, int length) {
			this.address = address;
			this.ints = ints;
			this.offset = offset;
			maxOffset = offset + (length >> 2);
		}

		@Override
		public int readNext() {
			if (offset >= maxOffset) {
				return 0;
			}
			address += 4;
			return ints[offset++];
		}

		@Override
		public void skip(int n) {
			offset += n;
			address += n * 4;
		}

		@Override
		public int getCurrentAddress() {
			return address;
		}
	}
}
