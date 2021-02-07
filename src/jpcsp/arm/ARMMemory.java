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
package jpcsp.arm;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.memory.mmio.IMMIOHandler;
import jpcsp.memory.mmio.MMIO;
import jpcsp.memory.mmio.MMIOHandlerReadWrite;
import jpcsp.memory.mmio.wlan.MMIOHandlerWlanFirmware;
import jpcsp.memory.mmio.wlan.MMIOHandlerWlanFirmware2;
import jpcsp.settings.Settings;

/**
 * The WLAN ARM memory:
 * - 0x00000000 - 0x00017FFF: ARM internal RAM
 * - 0x04000000 - 0x04001FFF: ARM internal RAM
 * - 0x80000000 - 0x80002FFF: ARM memory mapped I/O
 * - 0xC0000000 - 0xC0017FFF: ARM internal RAM
 * 
 * Basic information available from http://wiki.laptop.org/go/88W8388
 *
 * @author gid15
 *
 */
public class ARMMemory extends MMIO {
	public static Logger log = ARMProcessor.log;
	public static final int SIZE_RAM0 = 0x18000;
	public static final int BASE_RAM0 = 0x00000000;
	public static final int END_RAM0 = BASE_RAM0 + SIZE_RAM0 - 1;
	public static final int SIZE_RAM4 = 0x2000;
	public static final int BASE_RAM4 = 0x04000000;
	public static final int END_RAM4 = BASE_RAM4 + SIZE_RAM4 - 1;
	public static final int SIZE_HANDLER8 = 0x10000;
	public static final int BASE_HANDLER8 = 0x80000000;
	public static final int END_HANDLER8 = BASE_HANDLER8 + SIZE_HANDLER8 - 1;
	public static final int SIZE_HANDLER9 = 0x10000;
	public static final int BASE_HANDLER9 = 0x90000000;
	public static final int END_HANDLER9 = BASE_HANDLER9 + SIZE_HANDLER9 - 1;
	public static final int SIZE_RAMC = 0x18000;
	public static final int BASE_RAMC = 0xC0000000;
	public static final int END_RAMC = BASE_RAMC + SIZE_RAMC - 1;
	public static final int SIZE_ROMF = 0x10000;
	public static final int BASE_ROMF = 0xFFFF0000;
	public static final int END_ROMF = BASE_ROMF + SIZE_ROMF - 1;
	private final ARMBackendMemory backendMemory;
	private final int[] ram0;
	private ARMMMIOHandlerReadWrite ram0Handler;
	private final int[] ram4;
	private ARMMMIOHandlerReadWrite ram4Handler;
	private MMIOHandlerWlanFirmware handlerWlanFirmware;
	private MMIOHandlerWlanFirmware2 handlerWlanFirmware2;
	private final int[] ramC;
	private ARMMMIOHandlerReadWrite ramCHandler;
	private final int[] romF;
	private ARMMMIOHandlerReadWrite romFHandler;

	private static class ARMMMIOHandlerReadWrite extends MMIOHandlerReadWrite {
		private ARMProcessor processor;

		public ARMMMIOHandlerReadWrite(int baseAddress, int length, int[] memory) {
			super(baseAddress, length, memory);
		}

		public void setProcessor(ARMProcessor processor) {
			this.processor = processor;
		}

		@Override
		protected int getPc() {
			return processor.getCurrentInstructionPc();
		}
	}

	private static class ARMBackendMemory extends Memory {
		private ARMProcessor processor;

		public ARMBackendMemory() {
		}

		public void setProcessor(ARMProcessor processor) {
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

	public ARMMemory(Logger log) {
		super(new ARMBackendMemory());

		backendMemory = (ARMBackendMemory) getBackendMemory();

		ram0 = new int[SIZE_RAM0 >> 2];
		ram0Handler = new ARMMMIOHandlerReadWrite(BASE_RAM0, SIZE_RAM0, ram0);
		ram0Handler.setLogger(log);

		ram4 = new int[SIZE_RAM4 >> 2];
		ram4Handler = new ARMMMIOHandlerReadWrite(BASE_RAM4, SIZE_RAM4, ram4);
		ram4Handler.setLogger(log);

		handlerWlanFirmware = new MMIOHandlerWlanFirmware(BASE_HANDLER8);

		handlerWlanFirmware2 = new MMIOHandlerWlanFirmware2(BASE_HANDLER9, handlerWlanFirmware);

		ramC = new int[SIZE_RAMC >> 2];
		ramCHandler = new ARMMMIOHandlerReadWrite(BASE_RAMC, SIZE_RAMC, ramC);
		ramCHandler.setLogger(log);

		romF = new int[SIZE_ROMF >> 2];
		romFHandler = new ARMMMIOHandlerReadWrite(BASE_ROMF, SIZE_ROMF, romF);
		romFHandler.setLogger(log);
	}

	public void setProcessor(ARMProcessor processor) {
		backendMemory.setProcessor(processor);
		ram0Handler.setProcessor(processor);
		ram4Handler.setProcessor(processor);
		handlerWlanFirmware.setProcessor(processor);
		handlerWlanFirmware2.setProcessor(processor);
		ramCHandler.setProcessor(processor);
		romFHandler.setProcessor(processor);
	}

	public MMIOHandlerWlanFirmware getHandlerWlanFirmware() {
		return handlerWlanFirmware;
	}

	@Override
	protected IMMIOHandler getHandler(int address) {
		if (address >= BASE_RAM0 && address <= END_RAM0) {
			return ram0Handler;
		}
		if (address >= BASE_RAM4 && address <= END_RAM4) {
			return ram4Handler;
		}
		if (address >= BASE_HANDLER8 && address <= END_HANDLER8) {
			return handlerWlanFirmware;
		}
		if (address >= BASE_HANDLER9 && address <= END_HANDLER9) {
			return handlerWlanFirmware2;
		}
		if (address >= BASE_RAMC && address <= END_RAMC) {
			return ramCHandler;
		}
		if (address >= BASE_ROMF && address <= END_ROMF) {
			return romFHandler;
		}

		return super.getHandler(address);
	}

	public static boolean isAddressInRAM(int address) {
		if (address >= BASE_RAM0 && address <= END_RAM0) {
			return true;
		}
		if (address >= BASE_RAM4 && address <= END_RAM4) {
			return true;
		}
		if (address >= BASE_RAMC && address <= END_RAMC) {
			return true;
		}
		return false;
	}

	public static boolean isAddressGood(int address) {
		if (isAddressInRAM(address)) {
			return true;
		}
		if (address >= BASE_ROMF && address <= END_ROMF) {
			return true;
		}
		if (address >= BASE_HANDLER8 && address <= END_HANDLER8) {
			return true;
		}
		if (address >= BASE_HANDLER9 && address <= END_HANDLER9) {
			return true;
		}

		return false;
    }

	@Override
	public int normalize(int address) {
		// There is no need to normalize an ARM address
		return address;
	}
}
