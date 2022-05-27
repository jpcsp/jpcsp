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

import static jpcsp.memory.FastMemory.memory16Mask;
import static jpcsp.memory.FastMemory.memory16Shift;
import static jpcsp.memory.FastMemory.memory8Mask;
import static jpcsp.memory.FastMemory.memory8Shift;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

import jpcsp.Memory;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointer64;
import jpcsp.HLE.TPointer8;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class IntArrayMemory extends Memory {
	private final int[] memory;
	private final int offset;
	private final int baseAddress;

	private static class IntArrayTPointer32 extends TPointer32 {
		public IntArrayTPointer32(Memory memory, int address) {
			super(memory, address);
		}

		@Override
		public Memory getNewPointerMemory() {
			return Memory.getInstance();
		}
	}

	private static class IntArrayTPointer extends TPointer {
		public IntArrayTPointer(Memory memory, int address) {
			super(memory, address);
		}

		@Override
		public Memory getNewPointerMemory() {
			return Memory.getInstance();
		}
	}

	public IntArrayMemory(int[] memory) {
		this.memory = memory;
		offset = 0;
		baseAddress = 0;
	}

	public IntArrayMemory(int[] memory, int offset) {
		this.memory = memory;
		this.offset = offset;
		baseAddress = 0;
	}

	public IntArrayMemory(int[] memory, int offset, int baseAddress) {
		this.memory = memory;
		this.offset = offset;
		this.baseAddress = baseAddress & addressMask;
	}

	public TPointer getPointer(int address) {
		return new IntArrayTPointer(this, address).forceNonNull();
	}

	public TPointer getPointer() {
		return getPointer(baseAddress);
	}

	public TPointer8 getPointer8(int address) {
		return new TPointer8(this, address).forceNonNull();
	}

	public TPointer8 getPointer8() {
		return getPointer8(baseAddress);
	}

	public TPointer16 getPointer16(int address) {
		return new TPointer16(this, address).forceNonNull();
	}

	public TPointer16 getPointer16() {
		return getPointer16(baseAddress);
	}

	public TPointer32 getPointer32(int address) {
		return new IntArrayTPointer32(this, address).forceNonNull();
	}

	public TPointer32 getPointer32() {
		return getPointer32(baseAddress);
	}

	public TPointer64 getPointer64(int address) {
		return new TPointer64(this, address).forceNonNull();
	}

	public TPointer64 getPointer64() {
		return getPointer64(baseAddress);
	}

	@Override
	public void Initialise() {
	}

	private int getOffset(int address) {
		return (((address & addressMask) - baseAddress) >> 2) + offset;
	}

	@Override
	public int read32(int address) {
		return memory[getOffset(address)];
	}

	@Override
	public int read16(int address) {
		int data = (memory[getOffset(address)] >> memory16Shift[address & 0x02]) & 0xFFFF;
		return data;
	}

	@Override
	public int read8(int address) {
		int data = (memory[getOffset(address)] >> memory8Shift[address & 0x03]) & 0xFF;
		return data;
	}

	@Override
	public void write32(int address, int value) {
		memory[getOffset(address)] = value;
	}

	@Override
	public void write16(int address, short value) {
		int index = address & 0x02;
		int memData = (memory[getOffset(address)] & memory16Mask[index]) | ((value & 0xFFFF) << memory16Shift[index]);
		memory[getOffset(address)] = memData;
	}

	@Override
	public void write8(int address, byte value) {
		int index = address & 0x03;
		int memData = (memory[getOffset(address)] & memory8Mask[index]) | ((value & 0xFF) << memory8Shift[index]);
		memory[getOffset(address)] = memData;
	}

	@Override
	public void memset(int address, byte data, int length) {
		for (int i = 0; i < length; i++) {
			write8(address + i, data);
		}
	}

	@Override
	public Buffer getMainMemoryByteBuffer() {
		return null;
	}

	@Override
	public Buffer getBuffer(int address, int length) {
		return null;
	}

	@Override
	public void copyToMemory(int address, ByteBuffer source, int length) {
		for (int i = 0; i < length; i++) {
			write8(address + i, source.get(i));
		}
	}

	@Override
	protected void memcpy(int destination, int source, int length, boolean checkOverlap) {
		log.error(String.format("Unimplemented memcpy destination=0x%08X, source=0x%08X, length=0x%X, checkOverlap=%b", destination, source, length, checkOverlap));
	}

	public int getSize() {
		return (memory.length - offset) << 2;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		int size4 = stream.readInt();
		int length4 = Math.min(size4, memory.length - offset);
		stream.readInts(memory, offset, length4);
		if (length4 < size4) {
			stream.skipBytes((size4 - length4) << 2);
		}
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		int size4 = memory.length - offset;
		stream.writeInt(size4);
		stream.writeInts(memory, offset, size4);
	}
}
