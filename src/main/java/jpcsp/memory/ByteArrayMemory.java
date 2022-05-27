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

import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.writeUnaligned16;
import static jpcsp.util.Utilities.writeUnaligned32;

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
import jpcsp.util.Utilities;

public class ByteArrayMemory extends Memory {
	private final byte[] memory;
	private final int offset;
	private final int baseAddress;

	private static class ByteArrayTPointer32 extends TPointer32 {
		public ByteArrayTPointer32(Memory memory, int address) {
			super(memory, address);
		}

		@Override
		public Memory getNewPointerMemory() {
			return Memory.getInstance();
		}
	}

	private static class ByteArrayTPointer extends TPointer {
		public ByteArrayTPointer(Memory memory, int address) {
			super(memory, address);
		}

		@Override
		public Memory getNewPointerMemory() {
			return Memory.getInstance();
		}
	}

	public ByteArrayMemory(byte[] memory) {
		this.memory = memory;
		offset = 0;
		baseAddress = 0;
	}

	public ByteArrayMemory(byte[] memory, int offset) {
		this.memory = memory;
		this.offset = offset;
		baseAddress = 0;
	}

	public ByteArrayMemory(byte[] memory, int offset, int baseAddress) {
		this.memory = memory;
		this.offset = offset;
		this.baseAddress = baseAddress & addressMask;
	}

	public TPointer getPointer(int address) {
		return new ByteArrayTPointer(this, address).forceNonNull();
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
		return new ByteArrayTPointer32(this, address).forceNonNull();
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
		return (address & addressMask) - baseAddress + offset;
	}

	@Override
	public int read32(int address) {
		return readUnaligned32(memory, getOffset(address));
	}

	@Override
	public int read16(int address) {
		return readUnaligned16(memory, getOffset(address));
	}

	@Override
	public int read8(int address) {
		return Utilities.read8(memory, getOffset(address));
	}

	@Override
	public void write32(int address, int value) {
		writeUnaligned32(memory, getOffset(address), value);
	}

	@Override
	public void write16(int address, short value) {
		writeUnaligned16(memory, getOffset(address), value & 0xFFFF);
	}

	@Override
	public void write8(int address, byte value) {
		Utilities.write8(memory, getOffset(address), value & 0xFF);
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
		if (checkOverlap) {
			log.error(String.format("Unimplemented memcpy destination=0x%08X, source=0x%08X, length=0x%X, checkOverlap=%b", destination, source, length, checkOverlap));
		} else {
			for (int i = 0; i < length; i++) {
				write8(destination + i, (byte) read8(source + i));
			}
		}
	}

	public int getSize() {
		return memory.length - offset;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		int size = stream.readInt();
		int length = Math.min(size, getSize());
		stream.readBytes(memory, offset, length);
		if (length < size) {
			stream.skipBytes(size - length);
		}
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		int size = getSize();
		stream.writeInt(size);
		stream.writeBytes(memory, offset, size);
	}
}
