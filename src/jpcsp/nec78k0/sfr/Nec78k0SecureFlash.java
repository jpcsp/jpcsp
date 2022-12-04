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
package jpcsp.nec78k0.sfr;

import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.getByte0;
import static jpcsp.util.Utilities.getByte1;
import static jpcsp.util.Utilities.getByte2;
import static jpcsp.util.Utilities.getByte3;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.isRaisingBit;
import static jpcsp.util.Utilities.setBit;
import static jpcsp.util.Utilities.setByte0;
import static jpcsp.util.Utilities.setByte1;
import static jpcsp.util.Utilities.setByte2;
import static jpcsp.util.Utilities.setByte3;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * @author gid15
 *
 */
public class Nec78k0SecureFlash implements IState {
	private static final int STATE_VERSION = 0;
	private static final int READ_DATA_ADDRESS = 0xF8F2;
	// Dummy key values
	private static final int[] secureFlashKey790 = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18 };
	private static final int[] secureFlashKey7C0 = { 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38 };
	private static final int SECURE_FLASH_SIZE = 0x4000;
	private final int[] secureFlashMemory = new int[SECURE_FLASH_SIZE >> 2];
	private final Nec78k0Sfr sfr;
	private Logger log;
	private int programmingModeControl;
	private int address;
	private int unknown1;
	private int unknown4;
	private int unknown5;
	private int unknown6;
	private int unknown7;
	private int unknownB;
	private int writeData32;

	public Nec78k0SecureFlash(Nec78k0Sfr sfr) {
		this.sfr = sfr;
		log = sfr.log;

		write8(0x0009, 0x02 | 0x04 | 0x08 | 0x10); // Security flags
		write8(0x0081, 0x01 | 0x02 | 0x04 | 0x08); // Unknown flags
		write8(0x0481, 0x01 | 0x02 | 0x04 | 0x08); // Unknown flags (copy of 0x0081?)
		write16(0x0204, 0x0000); // Unknown 16-bit value written to 0xF8F6
		write16(0x0206, 0x0000); // Unknown 16-bit value written to 0xF8F6

		for (int i = 0; i < secureFlashKey790.length; i++) {
			write8(0x790 + i, secureFlashKey790[i]);
		}
		for (int i = 0; i < secureFlashKey7C0.length; i++) {
			write8(0x7C0 + i, secureFlashKey7C0[i]);
		}
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		programmingModeControl = stream.readInt();
		address = stream.readInt();
		unknown1 = stream.readInt();
		unknown4 = stream.readInt();
		unknown5 = stream.readInt();
		unknown6 = stream.readInt();
		unknown7 = stream.readInt();
		unknownB = stream.readInt();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(programmingModeControl);
		stream.writeInt(address);
		stream.writeInt(unknown1);
		stream.writeInt(unknown4);
		stream.writeInt(unknown5);
		stream.writeInt(unknown6);
		stream.writeInt(unknown7);
		stream.writeInt(unknownB);
	}

	public void setLogger(Logger log) {
		this.log = log;
	}

	public void reset() {
		programmingModeControl = 0x00;
		address = 0x0000;
		unknown1 = 0x00;
		unknown4 = 0x00;
		unknown5 = 0x00;
		unknown6 = 0x00;
		unknown7 = 0x00;
		unknownB = 0x00;
	}

	public void setAddress(int address) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("SecureFlash.setAddress 0x%04X", address));
		}
		this.address = address;
	}

	public int getAddress() {
		return address;
	}

	public void setFlashProtectCommandRegister(int value) {
		if (value != 0xA5) {
			log.error(String.format("SecureFlash.setFlashProtectCommandRegister unknown value 0x%02X", value));
		}
	}

	public int getUnknown1() {
		return unknown1;
	}

	public void setUnknown1(int value) {
		unknown1 = value;
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
		if (value != 0x00 && value != 0x02 && value != 0x08 && value != 0x09 && value != 0x0A && value != 0x0C) {
			log.error(String.format("SecureFlash.setUnknown5 unknown value 0x%02X", value));
		}

		int oldValue = unknown5;
		unknown5 = value;

		if (isRaisingBit(oldValue, unknown5, 1) && hasBit(unknown1, 7)) {
			boolean success = true;
			int operation = unknown1 & 0x0F;
			if (operation == 0x0) {
				// Read operation
				int value32 = read32(address);
				writeUnaligned32(sfr.getMemory(), READ_DATA_ADDRESS, value32);

				// Read and compare?
				if (hasBit(unknown7, 3)) {
					if (value32 != writeData32) {
						success = false;
					}
				}
			} else if (operation == 0x2) {
				// Extended read operation
				writeUnaligned32(sfr.getMemory(), READ_DATA_ADDRESS, read32(address));
				sfr.getMemory().write8(READ_DATA_ADDRESS - 1, (byte) 0x3F); // Unknown value, will be written to setUnknownB
			} else if (operation == 0x1) {
				// Verify operation
				int verifyData32 = read32(address);
				if (writeData32 != verifyData32) {
					log.error(String.format("SecureFlash verify operation failed at address 0x%04X: 0x%08X != 0x%08X", address, writeData32, verifyData32));
					success = false;
				}
			} else if (operation == 0x4) {
				// Erase operation
				write32(address, writeData32);
			} else if (operation == 0x5) {
				// Extended verify operation
				int verifyData32 = read32(address);
				if (writeData32 != verifyData32) {
					log.error(String.format("SecureFlash verify extended operation failed at address 0x%04X: 0x%08X != 0x%08X", address, writeData32, verifyData32));
					success = false;
				} else if (unknownB != 0x3F) {
					log.error(String.format("SecureFlash verify extended operation failed at address 0x%04X: 0x%02X", address, unknownB));
					success = false;
				}
			} else if (operation == 0x8) {
				// Write operation, ignored here
			} else {
				log.error(String.format("SecureFlash.setUnknown5 unknown operation 0x%X for address 0x%04X", operation, address));
			}

			if (success) {
				unknown7 = setBit(unknown7, 7);
			} else {
				unknown7 = clearBit(unknown7, 7);
			}
		}
	}

	public int getUnknown6() {
		return unknown6;
	}

	public void setUnknown6(int value) {
		if (value != 0x00 && value != 0x02) {
			log.error(String.format("SecureFlash.setUnknown6 unknown value 0x%02X", value));
		}

		int oldValue = unknown6;
		unknown6 = value;

		if (isRaisingBit(oldValue, unknown6, 1) && hasBit(unknown1, 7)) {
			int operation = unknown1 & 0x0F;
			if (operation == 0x8) {
				// Write operation
				write32(address, writeData32);
			} else {
				log.error(String.format("SecureFlash.setUnknown6 unknown operation 0x%X for address 0x%04X", operation, address));
			}
		}
	}

	public int getUnknown7() {
		return unknown7;
	}

	public void setUnknown7(int value) {
		if ((value & 0x72) != 0x00) {
			log.error(String.format("SecureFlash.setUnknown7 unknown value 0x%02X", value));
		}

		unknown7 = value;
	}

	public int getUnknown8() {
		return 0x00;
	}

	public void setFlashProgrammingModeControlRegister(int value) {
		if (value != 0x00 && value != 0x01 && value != 0x80 && value != 0x81 && value != 0x7E && value != 0x7F && value != 0xFE && value != 0xFF) {
			log.error(String.format("SecureFlash.setFlashProgrammingModeControlRegister unknown value 0x%02X", value));
		}
		programmingModeControl = value;
	}

	public int getFlashProgrammingModeControl() {
		return programmingModeControl;
	}

	public void setUnknownB(int value) {
		if (value != 0x3F) {
			log.error(String.format("SecureFlash.setUnknownB unknown value 0x%02X", value));
		}

		unknownB = value;
	}

	public void setWriteData0(int value) {
		writeData32 = setByte0(writeData32, value);
	}

	public void setWriteData1(int value) {
		writeData32 = setByte1(writeData32, value);
	}

	public void setWriteData2(int value) {
		writeData32 = setByte2(writeData32, value);
	}

	public void setWriteData3(int value) {
		writeData32 = setByte3(writeData32, value);

		if (log.isDebugEnabled()) {
			log.debug(String.format("SecureFlash.setWriteData 0x%08X", writeData32));
		}
	}

	private int internalRead32(int addr) {
		return secureFlashMemory[addr >> 2];
	}

	private void internalWrite32(int addr, int value) {
		secureFlashMemory[addr >> 2] = value;
	}

	private boolean isAddressValid(int addr, int alignment) {
		return addr >= 0 && addr < SECURE_FLASH_SIZE && (addr & alignment) == 0;
	}

	public int read32(int addr) {
		int value;

		if (isAddressValid(addr, 3)) {
			value = internalRead32(addr);
			if (log.isDebugEnabled()) {
				log.debug(String.format("SecureFlash.read32(0x%04X) returning 0x%08X", addr, value));
			}
		} else {
			log.error(String.format("SecureFlash.read32 unknown address 0x%04X", addr));
			value = 0;
		}

		return value;
	}

	public int read8(int addr) {
		int value8 = 0x00;
		if (isAddressValid(addr, 0)) {
			int value32 = internalRead32(addr);
			switch (addr & 0x3) {
				case 0: value8 = getByte2(value32); break;
				case 1: value8 = getByte3(value32); break;
				case 2: value8 = getByte0(value32); break;
				case 3: value8 = getByte1(value32); break;
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("SecureFlash.read8(0x%04X) returning 0x%02X", addr, value8));
			}
		} else {
			log.error(String.format("SecureFlash.read8 unknown address 0x%04X", addr));
		}

		return value8;
	}

	public void write32(int addr, int value) {
		if (isAddressValid(addr, 3)) {
			internalWrite32(addr, value);
			if (log.isDebugEnabled()) {
				log.debug(String.format("SecureFlash.write32(0x%04X, 0x%08X)", addr, value));
			}
		} else {
			log.error(String.format("SecureFlash.write32 unknown address 0x%04X", addr));
		}
	}

	public void write8(int addr, int value8) {
		if (isAddressValid(addr, 0)) {
			int value32 = internalRead32(addr);
			switch (addr & 0x3) {
				case 0: value32 = setByte2(value32, value8); break;
				case 1: value32 = setByte3(value32, value8); break;
				case 2: value32 = setByte0(value32, value8); break;
				case 3: value32 = setByte1(value32, value8); break;
			}
			internalWrite32(addr, value32);

			if (log.isDebugEnabled()) {
				log.debug(String.format("SecureFlash.write8(0x%04X, 0x%02X)", addr, value8));
			}
		} else {
			log.error(String.format("SecureFlash.write8 unknown address 0x%04X", addr));
		}
	}

	public void write16(int addr, int value16) {
		if (isAddressValid(addr, 1)) {
			int value32 = internalRead32(addr);
			switch (addr & 0x2) {
				case 0: value32 = setByte2(value32, getByte0(value16)); value32 = setByte3(value32, getByte1(value16)); break;
				case 2: value32 = setByte0(value32, getByte0(value16)); value32 = setByte1(value32, getByte1(value16)); break;
			}
			internalWrite32(addr, value32);

			if (log.isDebugEnabled()) {
				log.debug(String.format("SecureFlash.write16(0x%04X, 0x%04X)", addr, value16));
			}
		} else {
			log.error(String.format("SecureFlash.write16 unknown address 0x%04X", addr));
		}
	}
}
