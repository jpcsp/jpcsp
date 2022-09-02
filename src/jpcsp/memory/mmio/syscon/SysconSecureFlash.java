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
package jpcsp.memory.mmio.syscon;

import static jpcsp.util.Utilities.isRaisingBit;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.HLE.TPointer;
import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * @author gid15
 *
 */
public class SysconSecureFlash implements IState {
	protected Logger log = Nec78k0Processor.log;
	private static final int STATE_VERSION = 0;
	private static final int READ_DATA_ADDRESS = 0xF8F2;
	// Unknown values
	private static final int[] secureFlashKey790 = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18 };
	private static final int[] secureFlashKey7C0 = { 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38 };
	private final MMIOHandlerSysconFirmwareSfr sfr;
	private int address;
	private int unknown4;
	private int unknown5;

	public SysconSecureFlash(MMIOHandlerSysconFirmwareSfr sfr) {
		this.sfr = sfr;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		address = stream.readInt();
		unknown4 = stream.readInt();
		unknown5 = stream.readInt();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(address);
		stream.writeInt(unknown4);
		stream.writeInt(unknown5);
	}

	public void reset() {
		address = 0x0000;
		unknown4 = 0x00;
		unknown5 = 0x00;
	}

	public void setAddressLow(int low) {
		address = (address & 0xFF00) | (low & 0xFF);
	}

	public void setAddressHigh(int high) {
		address = (address & 0x00FF) | ((high << 8) & 0xFF00);
	}

	public void setFlashProtectCommandRegister(int value) {
		if (value != 0xA5) {
			log.error(String.format("SysconSecureFlash.setFlashProtectCommandRegister unknown value 0x%02X", value));
		}
	}

	public int getUnknown1() {
		return 0x00;
	}

	public void setUnknown1(int value) {
		if (value != 0x00 && value != 0x80) {
			log.error(String.format("SysconSecureFlash.setUnknown1 unknown value 0x%02X", value));
		}
	}

	public int getUnknown4() {
		// This flag seems to be read-only and requires to be set
		unknown4 |= 0x04;

		return unknown4;
	}

	public void setUnknown4(int value) {
		unknown4 = value;
	}

	public int getUnknown5() {
		return unknown5;
	}

	public void setUnknown5(int value) {
		if (value != 0x00 && value != 0x02 && value != 0x08 && value != 0x0A) {
			log.error(String.format("SysconSecureFlash.setUnknown5 unknown value 0x%02X", value));
		}

		int oldValue = unknown5;
		unknown5 = value;

		if (isRaisingBit(oldValue, unknown5, 1)) {
			TPointer resultPtr = new TPointer(sfr.getMemory(), READ_DATA_ADDRESS);
			for (int i = 0; i < 4; i++) {
				resultPtr.setUnsignedValue8(i, read8(address + i));
			}
		}
	}

	public void setUnknown6(int value) {
		if (value != 0x00) {
			log.error(String.format("SysconSecureFlash.setUnknown6 unknown value 0x%02X", value));
		}
	}

	public int getUnknown7() {
		return 0x00;
	}

	public void setUnknown7(int value) {
		if (value != 0x00) {
			log.error(String.format("SysconSecureFlash.setUnknown7 unknown value 0x%02X", value));
		}
	}

	public void setFlashProgrammingModeControlRegister(int value) {
		if (value != 0x00 && value != 0x01 && value != 0xFE && value != 0xFF) {
			log.error(String.format("SysconSecureFlash.setFlashProgrammingModeControlRegister unknown value 0x%02X", value));
		}
	}

	public int read8(int addr) {
		if (addr >= 0x0790 && addr <= 0x079F) {
			return secureFlashKey790[addr - 0x0790];
		}
		if (addr >= 0x07C0 && addr <= 0x07CF) {
			return secureFlashKey7C0[addr - 0x07C0];
		}

		log.error(String.format("SysconSecureFlash.read8 unknown address 0x%04X", addr));

		return 0x00;
	}
}
