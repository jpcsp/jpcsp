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

import jpcsp.Emulator;

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
			invalidMemoryAddress(address, "read8", Emulator.EMU_STATUS_MEM_READ);
			return 0;
		}

		return super.read8(address);
	}

	@Override
	public int read16(int address) {
		if (!isAddressGood(address)) {
			invalidMemoryAddress(address, "read16", Emulator.EMU_STATUS_MEM_READ);
			return 0;
		}

		return super.read16(address);
	}

	@Override
	public int read32(int address) {
		if (!isAddressGood(address)) {
            if (read32AllowedInvalidAddress(address)) {
            	return 0;
            }

            invalidMemoryAddress(address, "read32", Emulator.EMU_STATUS_MEM_READ);
			return 0;
		}

		return super.read32(address);
	}

	@Override
	public long read64(int address) {
		if (!isAddressGood(address)) {
			invalidMemoryAddress(address, "read64", Emulator.EMU_STATUS_MEM_READ);
			return 0;
		}

		return super.read64(address);
	}

	@Override
	public void write8(int address, byte data) {
		if (!isAddressGood(address)) {
			invalidMemoryAddress(address, "write8", Emulator.EMU_STATUS_MEM_WRITE);
			return;
		}

		super.write8(address, data);
	}

	@Override
	public void write16(int address, short data) {
		if (!isAddressGood(address)) {
			invalidMemoryAddress(address, "write16", Emulator.EMU_STATUS_MEM_WRITE);
			return;
		}

		super.write16(address, data);
	}

	@Override
	public void write32(int address, int data) {
		if (!isAddressGood(address)) {
			invalidMemoryAddress(address, "write32", Emulator.EMU_STATUS_MEM_WRITE);
			return;
		}

		super.write32(address, data);
	}

	@Override
	public void write64(int address, long data) {
		if (!isAddressGood(address)) {
			invalidMemoryAddress(address, "write64", Emulator.EMU_STATUS_MEM_WRITE);
			return;
		}

		super.write64(address, data);
	}

	@Override
	public void memset(int address, byte data, int length) {
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
    public void copyToMemoryFromOffset(int address, ByteBuffer source,int offset, int length)
    {
        if (!isAddressGood(address, length)) {
			invalidMemoryAddress(address, "copyToMemoryFromOffset", Emulator.EMU_STATUS_MEM_WRITE);
			return;
		}
        super.copyToMemoryFromOffset(address, source, offset, length);
    }

	@Override
	public Buffer getBuffer(int address, int length) {
		if (!isAddressGood(address, length)) {
			invalidMemoryAddress(address, "getBuffer", Emulator.EMU_STATUS_MEM_READ);
			return null;
		}

		return super.getBuffer(address, length);
	}
}
