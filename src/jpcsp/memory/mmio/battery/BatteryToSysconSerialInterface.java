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

import static jpcsp.memory.mmio.syscon.SysconBatteryEmulator.intsToString;

import jpcsp.nec78k0.sfr.Nec78k0SerialInterface;
import jpcsp.nec78k0.sfr.Nec78k0SerialInterfaceUART6;
import jpcsp.nec78k0.sfr.Nec78k0Sfr;

/**
 * Connect the Battery Serial Interface to the Syscon Serial Interface.
 *
 * @author gid15
 *
 */
public class BatteryToSysconSerialInterface extends Nec78k0SerialInterface {
	private final Nec78k0Sfr sysconSfr;
	private final int[] buffer = new int[21];
	private int index;

	public BatteryToSysconSerialInterface(Nec78k0Sfr sfr, Nec78k0SerialInterfaceUART6 serialInterface, Nec78k0Sfr sysconSfr) {
		super(sfr, serialInterface);

		this.sysconSfr = sysconSfr;
	}

	private Nec78k0SerialInterface getSysconSerialInterface() {
		return sysconSfr.getSerialInterfaceUART6().getConnectedSerialInterface();
	}

	@Override
	public synchronized void startTransmission() {
		getSysconSerialInterface().startReception();

		super.startTransmission();
	}

	private void debugTransmit(int value) {
		if (value == 0xA5) {
			index = 0;
		}
		buffer[index++] = value;
		if (index >= 2 && index == buffer[1] + 2) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("UART6 Battery response %s", intsToString(buffer, 0, index)));
			}
		}
	}

	@Override
	public synchronized void transmit(int value) {
		debugTransmit(value);

		getSysconSerialInterface().received(value);

		super.transmit(value);
	}
}
