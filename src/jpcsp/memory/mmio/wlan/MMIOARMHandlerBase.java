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
package jpcsp.memory.mmio.wlan;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.arm.ARMProcessor;
import jpcsp.memory.mmio.MMIOHandlerBase;
import jpcsp.settings.Settings;

/**
 * @author gid15
 *
 */
public class MMIOARMHandlerBase extends MMIOHandlerBase {
	private ARMProcessor processor;

	protected MMIOARMHandlerBase(int baseAddress) {
		super(baseAddress);
	}

	public void setProcessor(ARMProcessor processor) {
		this.processor = processor;
	}

	@Override
	public Memory getMemory() {
		return processor.mem;
	}

	@Override
    public int getPc() {
    	return processor.getCurrentInstructionPc();
    }

    private void invalidMemoryAddress(int address, String value, String prefix, int status) {
        log.error(String.format("0x%08X - %s - Invalid memory address: 0x%08X%s", getPc(), prefix, address, value));
        if (!Settings.getInstance().readBool("emu.ignoreInvalidMemoryAccess")) {
        	Emulator.PauseEmuWithStatus(status);
        }
    }

    @Override
	public int read8(int address) {
		invalidMemoryAddress(address, "", "read8", Emulator.EMU_STATUS_MEM_READ);
		return 0;
	}

    @Override
	public int read16(int address) {
		invalidMemoryAddress(address, "", "read16", Emulator.EMU_STATUS_MEM_READ);
		return 0;
	}

    @Override
	public int read32(int address) {
		invalidMemoryAddress(address, "", "read32", Emulator.EMU_STATUS_MEM_READ);
		return 0;
	}

	@Override
	public void write8(int address, byte value) {
		invalidMemoryAddress(address, String.format(" (0x%02X)", value & 0xFF), "write8", Emulator.EMU_STATUS_MEM_WRITE);
	}

	@Override
	public void write16(int address, short value) {
		invalidMemoryAddress(address, String.format(" (0x%04X)", value & 0xFFFF), "write16", Emulator.EMU_STATUS_MEM_WRITE);
	}

	@Override
	public void write32(int address, int value) {
		invalidMemoryAddress(address, String.format(" (0x%08X)", value), "write32", Emulator.EMU_STATUS_MEM_WRITE);
	}
}
