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

import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_BASE;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_CHALLENGE1;
import static jpcsp.HLE.modules.sceSyscon.getSysconCmdName;

import jpcsp.nec78k0.sfr.Nec78k0SerialInterface;
import jpcsp.nec78k0.sfr.Nec78k0SerialInterfaceUART6;
import jpcsp.nec78k0.sfr.Nec78k0Sfr;

/**
 * Connect the Syscon Serial Interface to the Battery Serial Interface.
 *
 * @author gid15
 *
 */
public class SysconToBatterySerialInterface extends Nec78k0SerialInterface {
	private final Nec78k0Sfr batterySfr;

	public SysconToBatterySerialInterface(Nec78k0Sfr sfr, Nec78k0SerialInterfaceUART6 serialInterface, Nec78k0Sfr batterySfr) {
		super(sfr, serialInterface);

		this.batterySfr = batterySfr;
	}

	private Nec78k0SerialInterface getBatterySerialInterface() {
		return batterySfr.getSerialInterfaceUART6().getConnectedSerialInterface();
	}

	private void avoidSysconTimeout() {
		if (sfr.getTimer50().getClockSelection() == 0x07) {
			sfr.getTimer50().setClockStep(100000000); // Increase clock step to 100ms (instead of 1.639ms)
		}
	}

	@Override
	public synchronized void startTransmission() {
		getBatterySerialInterface().startReception();

		avoidSysconTimeout();

		super.startTransmission();
	}

	@Override
	public synchronized void transmit(int value) {
		getBatterySerialInterface().received(value);

		super.transmit(value);
	}

	private void debugSysconCmdBattery() {
		int length = transmissionBuffer.peek(1);
		int sysconCmdBattery = PSP_SYSCON_CMD_BATTERY_BASE + transmissionBuffer.peek(2);
		switch (sysconCmdBattery) {
			case PSP_SYSCON_CMD_BATTERY_CHALLENGE1:
				if (length == 11) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("UART6 Battery command %s, keyId=0x%02X, challenge=%s", getSysconCmdName(sysconCmdBattery), transmissionBuffer.peek(3), transmissionBuffer.toString(8, 4, 8)));
					}
				} else {
					if (log.isDebugEnabled()) {
						log.debug(String.format("UART6 Battery command %s: %s", getSysconCmdName(sysconCmdBattery), transmissionBuffer.toString(8)));
					}
				}
				break;
			default:
				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 Battery command %s: %s", getSysconCmdName(sysconCmdBattery), transmissionBuffer.toString(8)));
				}
				break;
		}
	}

	@Override
	public synchronized void endTransmission() {
		super.endTransmission();

		if (transmissionBuffer.peek(0) == 0x5A) {
			debugSysconCmdBattery();
		}

		transmissionBuffer.clear();
	}
}
