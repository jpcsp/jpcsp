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
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import jpcsp.Memory;
import jpcsp.MemoryMap;

public class DirectBufferMemory extends Memory {
	private ByteBuffer byteBuffer;
	private ShortBuffer shortBuffer;
	private IntBuffer intBuffer;
	private static final int clearBufferSize = 10 * 1024;
	private ByteBuffer clearBuffer;

	@Override
	public boolean allocate() {
		try {
			byteBuffer = ByteBuffer.allocateDirect(MemoryMap.END_RAM + 1).order(ByteOrder.LITTLE_ENDIAN);
			intBuffer = byteBuffer.asIntBuffer();
			shortBuffer = byteBuffer.asShortBuffer();
			clearBuffer = ByteBuffer.allocateDirect(clearBufferSize).order(byteBuffer.order());
		} catch (OutOfMemoryError e) {
			// Not enough memory provided for this VM, cannot use FastMemory model
			Memory.log.warn("Cannot allocate DirectBufferMemory: add the option '-Xmx256m' to the Java Virtual Machine startup command to improve Performance");
			Memory.log.info("The current Java Virtual Machine has been started using '-Xmx" + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "m'");
			return false;
		}

		return super.allocate();
	}

	@Override
	public void Initialise() {
		byteBuffer.clear();
		intBuffer.clear();
		shortBuffer.clear();
		clearBuffer.clear();

		for (int i = 0; i < clearBufferSize; i++) {
			clearBuffer.put((byte) 0);
		}
		clearBuffer.clear();

		for (int i = 0; i < MemoryMap.END_RAM; i += clearBufferSize) {
			byteBuffer.put(clearBuffer);
		}
		clearBuffer.clear();
		byteBuffer.clear();
	}

	// Slice the buffer and keep the byteorder
	private static ByteBuffer slice(ByteBuffer buffer) {
		return buffer.slice().order(buffer.order());
	}

	@Override
	public void copyToMemory(int address, ByteBuffer source, int length) {
		address &= addressMask;
		source = slice(source);
		source.limit(length);
		ByteBuffer mem = slice(byteBuffer);
		mem.position(address);
		mem.put(source);
	}

	protected ByteBuffer getByteBuffer(int address, int length) {
		address &= addressMask;
		ByteBuffer mem = slice(byteBuffer);
		mem.position(address);
		mem.limit(address + length);
		return slice(mem);
	}

	@Override
	public Buffer getBuffer(int address, int length) {
		return getByteBuffer(address, length);
	}

	@Override
	public Buffer getMainMemoryByteBuffer() {
		return slice(byteBuffer);
	}

	@Override
	protected void memcpy(int destination, int source, int length, boolean checkOverlap) {
		destination = normalizeAddress(destination);
		source = normalizeAddress(source);

		if (checkOverlap || !areOverlapping(destination, source, length)) {
			// Direct copy if buffers do not overlap.
			// ByteBuffer operations are handling correctly overlapping buffers.
			ByteBuffer destinationBuffer = getByteBuffer(destination, length);
			ByteBuffer sourceBuffer = getByteBuffer(source, length);
			destinationBuffer.put(sourceBuffer);
		} else {
			// Buffers are overlapping and we have to copy them as they would not overlap.
			IMemoryReader sourceReader = MemoryReader.getMemoryReader(source, length, 1);
			for (int i = 0; i < length; i++) {
				write8(destination + i, (byte) sourceReader.readNext());
			}
		}
	}

	@Override
	public void memset(int address, byte data, int length) {
		ByteBuffer destination = getByteBuffer(address, length);
		ByteBuffer source;
		if (data == 0) {
			source = slice(clearBuffer);
		} else {
			source = ByteBuffer.allocateDirect(clearBufferSize);
			for (int i = 0; i < clearBufferSize; i++) {
				source.put(data);
			}
			source.clear();
		}

		while (length >= clearBufferSize) {
			destination.put(source);
			source.clear();
			length -= clearBufferSize;
		}

		if (length > 0) {
			source.limit(length);
			destination.put(source);
		}
	}

	@Override
	public int read16(int address) {
		address &= addressMask;
		return ((int) shortBuffer.get(address >> 1)) & 0xFFFF;
	}

	@Override
	public int read32(int address) {
		address &= addressMask;
		return intBuffer.get(address >> 2);
	}

	@Override
	public int read8(int address) {
		address &= addressMask;
		return ((int) byteBuffer.get(address)) & 0xFF;
	}

	@Override
	public void write16(int address, short data) {
		address &= addressMask;
		shortBuffer.put(address >> 1, data);
	}

	@Override
	public void write32(int address, int data) {
		address &= addressMask;
		intBuffer.put(address >> 2, data);
	}

	@Override
	public void write8(int address, byte data) {
		address &= addressMask;
		byteBuffer.put(address, data);
	}
}
