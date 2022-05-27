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
package jpcsp.nec78k0;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.settings.Settings;

/**
 * @author gid15
 *
 */
public class Nec78k0BackendMemory extends Memory {
	private Nec78k0Processor processor;

	public Nec78k0BackendMemory() {
	}

	public void setProcessor(Nec78k0Processor processor) {
		this.processor = processor;
	}

    private void invalidMemoryAddress(int address, String value, String prefix, int status) {
        log.error(String.format("0x%08X - %s - Invalid memory address: 0x%08X%s", processor.getCurrentInstructionPc(), prefix, address, value));
        if (!Settings.getInstance().readBool("emu.ignoreInvalidMemoryAccess")) {
        	Emulator.PauseEmuWithStatus(status);
        }
    }

    @Override
    public void invalidMemoryAddress(int address, String prefix, int status) {
    	invalidMemoryAddress(address, "", prefix, status);
    }

	@Override
    public void invalidMemoryAddress(int address, int length, String prefix, int status) {
        log.error(String.format("0x%08X - %s - Invalid memory address: 0x%08X-0x%08X(length=0x%X)", processor.getCurrentInstructionPc(), prefix, address, address + length, length));
        if (!Settings.getInstance().readBool("emu.ignoreInvalidMemoryAccess")) {
        	Emulator.PauseEmuWithStatus(status);
        }
    }

    @Override
	public void Initialise() {
	}

	@Override
	public int read8(int address) {
		invalidMemoryAddress(address, "read8", Emulator.EMU_STATUS_MEM_READ);
		return 0;
	}

	@Override
	public int read16(int address) {
		invalidMemoryAddress(address, "read16", Emulator.EMU_STATUS_MEM_READ);
		return 0;
	}

	@Override
	public int read32(int address) {
		invalidMemoryAddress(address, "read32", Emulator.EMU_STATUS_MEM_READ);
		return 0;
	}

	@Override
	public void write8(int address, byte data) {
		invalidMemoryAddress(address, String.format(" (0x%02X)", data & 0xFF), "write8", Emulator.EMU_STATUS_MEM_WRITE);
	}

	@Override
	public void write16(int address, short data) {
		invalidMemoryAddress(address, String.format(" (0x%04X)", data & 0xFFFF), "write16", Emulator.EMU_STATUS_MEM_WRITE);
	}

	@Override
	public void write32(int address, int data) {
		invalidMemoryAddress(address, String.format(" (0x%08X)", data), "write32", Emulator.EMU_STATUS_MEM_WRITE);
	}

	@Override
	public void memset(int address, byte data, int length) {
		invalidMemoryAddress(address, length, "memset", Emulator.EMU_STATUS_MEM_WRITE);
	}

	@Override
	public Buffer getMainMemoryByteBuffer() {
		return null;
	}

	@Override
	public Buffer getBuffer(int address, int length) {
		invalidMemoryAddress(address, length, "getBuffer", Emulator.EMU_STATUS_MEM_READ);
		return null;
	}

	@Override
	public void copyToMemory(int address, ByteBuffer source, int length) {
		invalidMemoryAddress(address, length, "copyToMemory", Emulator.EMU_STATUS_MEM_WRITE);
	}

	@Override
	protected void memcpy(int destination, int source, int length, boolean checkOverlap) {
		invalidMemoryAddress(destination, length, "memcpy", Emulator.EMU_STATUS_MEM_WRITE);
	}
}
