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

import static jpcsp.nec78k0.Nec78k0Processor.REGISTER_ADDRESS_BANK3;
import static jpcsp.nec78k0.Nec78k0Processor.SFR_ADDRESS;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.setBit;

import org.apache.log4j.Logger;

import jpcsp.memory.mmio.IMMIOHandler;
import jpcsp.memory.mmio.MMIO;
import jpcsp.nec78k0.sfr.Nec78k0Sfr;

/**
 * Generic NEC 78k0 Memory map:
 *   - [0x0000..0xFEDF]: RAM
 *   - [0xFEE0..0xFEE7]: Register address banks 3
 *   - [0xFEE8..0xFEEF]: Register address banks 2
 *   - [0xFEF0..0xFEF7]: Register address banks 1
 *   - [0xFEF8..0xFEFF]: Register address banks 0
 *   - [0xFF00..0xFFFF]: Special Function Registers (SFR)
 *   - [0xFF1C..0xFF1D]: Register SP
 *   - [0xFF1E..0xFF1E]: Register PSW
 *
 * @author gid15
 *
 */
public class Nec78k0Memory extends MMIO {
	public static final int SIZE_RAM0 = REGISTER_ADDRESS_BANK3;
	public static final int BASE_RAM0 = 0x0000;
	public static final int END_RAM0 = BASE_RAM0 + SIZE_RAM0 - 1;
	private final Nec78k0BackendMemory backendMemory;
	private final int[] ram0;
	private final Nec78k0MMIOHandlerReadWrite ram0Handler;
	private final Nec78k0MMIORegisterBanks registerBanksHandler;
	private final Nec78k0Sfr sfr;

	public Nec78k0Memory(Logger log, Nec78k0Sfr sfr, int baseRam0, int sizeRam0) {
		super(new Nec78k0BackendMemory());

		backendMemory = (Nec78k0BackendMemory) getBackendMemory();

		ram0 = new int[sizeRam0 >> 2];
		ram0Handler = new Nec78k0MMIOHandlerReadWrite(baseRam0, sizeRam0, ram0);
		ram0Handler.setLogger(log);

		registerBanksHandler = new Nec78k0MMIORegisterBanks();
		registerBanksHandler.setLogger(log);

		this.sfr = sfr;
		sfr.setLogger(log);
	}

	public void setProcessor(Nec78k0Processor processor) {
		backendMemory.setProcessor(processor);
		ram0Handler.setProcessor(processor);
		registerBanksHandler.setProcessor(processor);
		sfr.setProcessor(processor);
	}

	public Nec78k0Sfr getSfr() {
		return sfr;
	}

	@Override
	protected IMMIOHandler getHandler(int address) {
		if (address >= BASE_RAM0 && address <= END_RAM0) {
			return ram0Handler;
		}
		if (address >= REGISTER_ADDRESS_BANK3 && address < SFR_ADDRESS) {
			return registerBanksHandler;
		}
		if (isSfrHandler(address)) {
			return sfr;
		}

		return super.getHandler(address);
	}

	private boolean isSfrHandler(int address) {
		return address >= SFR_ADDRESS && address <= 0xFFFF;
	}

	public static boolean isAddressGood(int address) {
		if (address >= BASE_RAM0 && address <= END_RAM0) {
			return true;
		}
		if (address >= REGISTER_ADDRESS_BANK3 && address < SFR_ADDRESS) {
			return true;
		}
		if (address >= SFR_ADDRESS && address <= 0xFFFF) {
			return true;
		}

		return false;
    }

	public boolean read1(int address, int bit) {
		boolean value;
		if (isSfrHandler(address)) {
			value = sfr.read1(address, bit);
		} else {
			value = hasBit(read8(address), bit);
		}

		return value;
	}

	public void write1(int address, int bit, boolean value) {
		if (isSfrHandler(address)) {
			sfr.write1(address, bit, value);
		} else {
			int value8 = read8(address);
			if (value) {
				value8 = setBit(value8, bit);
			} else {
				value8 = clearBit(value8, bit);
			}
			write8(address, (byte) value8);
		}
	}

	public void set1(int address, int bit) {
		if (isSfrHandler(address)) {
			sfr.set1(address, bit);
		} else {
			write1(address, bit, true);
		}
	}

	public void clear1(int address, int bit) {
		if (isSfrHandler(address)) {
			sfr.clear1(address, bit);
		} else {
			write1(address, bit, false);
		}
	}

	@Override
	public int normalize(int address) {
		// There is no need to normalize a 78k0 address
		return address;
	}
}
