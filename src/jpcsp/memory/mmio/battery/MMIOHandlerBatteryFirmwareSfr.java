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
package jpcsp.memory.mmio.battery;

import static jpcsp.memory.mmio.battery.BatteryEmulator.readEeprom16;
import static jpcsp.memory.mmio.battery.BatteryEmulator.writeEeprom16;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.isRaisingBit;

import java.io.IOException;

import jpcsp.nec78k0.sfr.Nec78k0Sfr;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * Special Function Registers (SFR) for a NEC 78k0 running in a PSP Battery.
 * 
 * @author gid15
 *
 */
public class MMIOHandlerBatteryFirmwareSfr extends Nec78k0Sfr {
	private static final int STATE_VERSION = 0;
	private int eepromValue;
	private int eepromBitCount;
	private int eepromBitExpected;
	private boolean eepromEraseWriteEnabled;

	public MMIOHandlerBatteryFirmwareSfr(int baseAddress) {
		super(baseAddress);

		adConverter = new BatteryAdConverter(this, scheduler);
		if (BatteryEmulator.isEnabled()) {
			serialInterfaceUART6.setSerialInterface(null); // Will be set later
		} else {
			serialInterfaceUART6.setSerialInterface(new BatterySerialInterface(this, serialInterfaceUART6));
		}

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		eepromValue = 0;
		eepromBitCount = 0;
		eepromBitExpected = 11;
		eepromEraseWriteEnabled = false;

		// Input P12.0 = 1
		setPortInputBit(12, 0);
	}

	private void processEeprom(int port) {
		final int dataPortBit = 3;

		// EEPROM interface, based on chip 93LC66
		if (isInputPort(port, dataPortBit)) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("EEPROM read value=0x%X, count=%d", eepromValue, eepromBitCount));
			}

			if (hasBit(eepromValue, eepromBitExpected - 1)) {
				setPortInputBit(port, dataPortBit);
			} else {
				clearPortInputBit(port, dataPortBit);
			}
			eepromValue = (eepromValue << 1) & 0xFFFF;
			eepromBitCount++;

			if (eepromBitCount == eepromBitExpected) {
				eepromBitCount = 0;
				eepromValue = 0;
				eepromBitExpected = 11;
			}
		} else {
			eepromValue <<= 1;
			if (getPortOutputBit(port, dataPortBit)) {
				eepromValue |= 1;
			}
			eepromBitCount++;

			if (log.isDebugEnabled()) {
				log.debug(String.format("EEPROM write value=0x%X, count=%d", eepromValue, eepromBitCount));
			}

			if (eepromBitCount == eepromBitExpected) {
				int instruction = (eepromValue >> (eepromBitExpected - 3)) & 0x7;
				int address = (eepromValue >> (eepromBitExpected - 11)) & 0xFF;
				if (log.isDebugEnabled()) {
					log.debug(String.format("EEPROM instruction=0x%X, address=0x%02X", instruction, address));
				}

				switch (instruction) {
					case 0x6: // Read instruction
						eepromValue = readEeprom16(address);
						eepromBitCount = 0;
						eepromBitExpected = 16;
						break;
					case 0x4:
						int subInstruction = (address >> 6) & 0x3;
						switch (subInstruction) {
							case 0x0: // Erase Write Disable (EWDS) instruction
								if (log.isDebugEnabled()) {
									log.debug(String.format("EEPROM Erase Write Disable"));
								}
								eepromEraseWriteEnabled = false;
								break;
							case 0x1: // Write All (WRAL) instruction
								log.error(String.format("EEPROM unimplemented Write All instruction"));
								break;
							case 0x2: // Erase All (ERAL) instruction
								log.error(String.format("EEPROM unimplemented Erase All instruction"));
								break;
							case 0x3: // Erase Write Enable (EWEN) instruction
								if (log.isDebugEnabled()) {
									log.debug(String.format("EEPROM Erase Write Enable"));
								}
								eepromEraseWriteEnabled = true;
								break;
						}
						eepromValue = 0;
						eepromBitCount = 0;
						eepromBitExpected = 11;
						break;
					case 0x5: // Write instruction
						if (eepromBitExpected == 27) {
							if (!eepromEraseWriteEnabled) {
								log.debug(String.format("EEPROM Write not enabled for address=0x%02X: 0x%04X", address, eepromValue & 0xFFFF));
							} else {
								writeEeprom16(address, eepromValue);
							}
							eepromValue = 0;
							eepromBitCount = 0;
							eepromBitExpected = 11;
						} else {
							if (log.isDebugEnabled()) {
								log.debug(String.format("EEPROM Write address=0x%02X: collecting 16-bit data", address));
							}
							eepromBitExpected += 16;
						}
						break;
					default:
						eepromValue = 0;
						eepromBitCount = 0;
						eepromBitExpected = 11;
						log.error(String.format("EEPROM unimplemented instruction=0x%X, address=0x%02X", instruction, address));
						break;
				}
			}
		}
	}

	@Override
	protected void setPortOutput(int port, int value) {
		if (port == 3) {
			int oldValue = getPortOutput(port);
			if (isRaisingBit(oldValue, value, 0)) {
				processEeprom(port);
			}
		}

		super.setPortOutput(port, value);
	}

	@Override
	public String toString() {
		return String.format("Battery SFR %s", debugInterruptRequests());
	}
}
