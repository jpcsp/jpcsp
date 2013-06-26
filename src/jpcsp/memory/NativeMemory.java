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

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;

/**
 * A Memory implementation using a memory area allocated natively.
 * Using NativeMemoryUtils to perform the native operations.
 * 
 * @author gid15
 *
 */
public class NativeMemory extends Memory {
	private long memory;
	private int memorySize;

	@Override
	public boolean allocate() {
		NativeMemoryUtils.init();

		memorySize = MemoryMap.END_RAM + 1;
		memory = NativeMemoryUtils.alloc(memorySize);

		if (memory == 0) {
			// Not enough native memory available
			return false;
		}

		log.info(String.format("Using NativeMemory(littleEndian=%b)", NativeMemoryUtils.isLittleEndian()));

		return super.allocate();
	}

	@Override
	public void Initialise() {
		NativeMemoryUtils.memset(memory, 0, 0, memorySize);
	}

	@Override
	public int read8(int address) {
		address &= addressMask;
		return NativeMemoryUtils.read8(memory, address);
	}

	@Override
	public int read16(int address) {
		address &= addressMask;
		return NativeMemoryUtils.read16(memory, address);
	}

	@Override
	public int read32(int address) {
		address &= addressMask;
		return NativeMemoryUtils.read32(memory, address);
	}

	@Override
	public void write8(int address, byte data) {
		address &= addressMask;
		NativeMemoryUtils.write8(memory, address, data);
        Modules.sceDisplayModule.write8(address);
	}

	@Override
	public void write16(int address, short data) {
		address &= addressMask;
		NativeMemoryUtils.write16(memory, address, data);
        Modules.sceDisplayModule.write16(address);
	}

	@Override
	public void write32(int address, int data) {
		address &= addressMask;
		NativeMemoryUtils.write32(memory, address, data);
        Modules.sceDisplayModule.write32(address);
	}

	@Override
	public void memset(int address, byte data, int length) {
		address &= addressMask;
		NativeMemoryUtils.memset(memory, address, data, length);
	}

	@Override
	public Buffer getMainMemoryByteBuffer() {
		return null;
	}

	@Override
	public Buffer getBuffer(int address, int length) {
		address &= addressMask;
		ByteBuffer buffer = NativeMemoryUtils.getBuffer(memory, address, length);

		// Set the correct byte order
		if (NativeMemoryUtils.isLittleEndian()) {
			buffer.order(ByteOrder.LITTLE_ENDIAN);
		} else {
			buffer.order(ByteOrder.BIG_ENDIAN);
		}

		return buffer;
	}

	@Override
	public void copyToMemory(int address, ByteBuffer source, int length) {
		address &= addressMask;
		length = Math.min(length, source.capacity());
		if (source.isDirect()) {
			NativeMemoryUtils.copyBufferToMemory(memory, address, source, source.position(), length);
		} else {
			for (; length > 0; address++, length--) {
				NativeMemoryUtils.write8(memory, address, source.get());
			}
		}
	}

	@Override
	protected void memcpy(int destination, int source, int length, boolean checkOverlap) {
		if (length <= 0) {
			return;
		}

		destination &= addressMask;
		source &= addressMask;
        Modules.sceDisplayModule.write8(destination);

        if (!checkOverlap || source >= destination || !areOverlapping(destination, source, length)) {
        	NativeMemoryUtils.memcpy(memory, destination, memory, source, length);
        } else {
        	// Source and destination are overlapping and source < destination,
        	// copy from the tail.
			for (int i = length - 1; i >= 0; i--) {
				int b = NativeMemoryUtils.read8(memory, source + i);
				NativeMemoryUtils.write8(memory, destination + i, b);
			}
        }
	}
}
