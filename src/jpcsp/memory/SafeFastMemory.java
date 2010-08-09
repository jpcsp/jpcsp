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

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import jpcsp.Emulator;
import jpcsp.MemoryMap;

public class SafeFastMemory extends FastMemory {
	//
	// This class is using the FastMemory implementation but
	// additionally checks the validity of the address for each access.
	//
	private boolean isAddressGood(int address, int length) {
		return isAddressGood(address) && isAddressGood(address + length - 1);
	}

	@Override
	public int read8(int address) {
		if (!isAddressGood(address)) {
            int normalizedAddress = normalizeAddress(address);
            if (isRawAddressGood(normalizedAddress)) {
                address = normalizedAddress;
            } else {
				invalidMemoryAddress(address, "read8", Emulator.EMU_STATUS_MEM_READ);
				return 0;
            }
		}

		return super.read8(address);
	}

	@Override
	public int read16(int address) {
		if (!isAddressGood(address)) {
            int normalizedAddress = normalizeAddress(address);
            if (isRawAddressGood(normalizedAddress)) {
                address = normalizedAddress;
            } else {
            	invalidMemoryAddress(address, "read16", Emulator.EMU_STATUS_MEM_READ);
    			return 0;
            }
		}

		return super.read16(address);
	}

	@Override
	public int read32(int address) {
		if (!isAddressGood(address)) {
            if (read32AllowedInvalidAddress(address)) {
            	return 0;
            }

            int normalizedAddress = normalizeAddress(address);
            if (isRawAddressGood(normalizedAddress)) {
                address = normalizedAddress;
            } else {
                invalidMemoryAddress(address, "read32", Emulator.EMU_STATUS_MEM_READ);
                return 0;
            }
		}

		return super.read32(address);
	}

	@Override
	public long read64(int address) {
		if (!isAddressGood(address)) {
            int normalizedAddress = normalizeAddress(address);
            if (isRawAddressGood(normalizedAddress)) {
                address = normalizedAddress;
            } else {
				invalidMemoryAddress(address, "read64", Emulator.EMU_STATUS_MEM_READ);
				return 0;
            }
		}

		return super.read64(address);
	}

	@Override
	public void write8(int address, byte data) {
		if (!isAddressGood(address)) {
            int normalizedAddress = normalizeAddress(address);
            if (isRawAddressGood(normalizedAddress)) {
                address = normalizedAddress;
            } else {
				invalidMemoryAddress(address, "write8", Emulator.EMU_STATUS_MEM_WRITE);
				return;
            }
		}

		super.write8(address, data);
	}

	@Override
	public void write16(int address, short data) {
		if (!isAddressGood(address)) {
            int normalizedAddress = normalizeAddress(address);
            if (isRawAddressGood(normalizedAddress)) {
                address = normalizedAddress;
            } else {
				invalidMemoryAddress(address, "write16", Emulator.EMU_STATUS_MEM_WRITE);
				return;
            }
		}

		super.write16(address, data);
	}

	@Override
	public void write32(int address, int data) {
		if (!isAddressGood(address)) {
            int normalizedAddress = normalizeAddress(address);
            if (isRawAddressGood(normalizedAddress)) {
                address = normalizedAddress;
            } else {
				invalidMemoryAddress(address, "write32", Emulator.EMU_STATUS_MEM_WRITE);
				return;
            }
		}

		super.write32(address, data);
	}

	@Override
	public void write64(int address, long data) {
		if (!isAddressGood(address)) {
            int normalizedAddress = normalizeAddress(address);
            if (isRawAddressGood(normalizedAddress)) {
                address = normalizedAddress;
            } else {
				invalidMemoryAddress(address, "write64", Emulator.EMU_STATUS_MEM_WRITE);
				return;
            }
		}

		super.write64(address, data);
	}

	@Override
	public void memset(int address, byte data, int length) {
		if (length <= 0) {
			return;
		}

		if (!isAddressGood(address, length)) {
			invalidMemoryAddress(address, "memset", Emulator.EMU_STATUS_MEM_WRITE);
			return;
		}

		super.memset(address, data, length);
	}

	@Override
	public void copyToMemory(int address, ByteBuffer source, int length) {
		if (!isAddressGood(address, length)) {
			invalidMemoryAddress(address, "copyToMemory", Emulator.EMU_STATUS_MEM_WRITE);
			return;
		}

		super.copyToMemory(address, source, length);
	}

	@Override
	public IntBuffer getBuffer(int address, int length) {
		if (!isAddressGood(address, length)) {
		    if (isAddressGood(address) && address >= MemoryMap.START_VRAM && address <= MemoryMap.END_VRAM) {
		        // Accept loading a texture e.g. at address 0x4154000 with length 0x100000
		        // The address 0x42xxxxx should map to 0x40xxxxx but we ignore this here
		        // because we cannot build a buffer starting at 0x4154000 and ending
		        // at 0x4054000.
		    } else {
		        invalidMemoryAddress(address, "getBuffer", Emulator.EMU_STATUS_MEM_READ);
		        return null;
		    }
		}

		return super.getBuffer(address, length);
	}

	@Override
	public void memcpy(int destination, int source, int length, boolean checkOverlap) {
		if (length <= 0) {
			return;
		}

		if (!isAddressGood(destination, length)) {
			invalidMemoryAddress(destination, "memcpy", Emulator.EMU_STATUS_MEM_WRITE);
			return;
		}
		if (!isAddressGood(source, length)) {
			invalidMemoryAddress(source, "memcpy", Emulator.EMU_STATUS_MEM_READ);
			return;
		}

		super.memcpy(destination, source, length, checkOverlap);
	}
}
