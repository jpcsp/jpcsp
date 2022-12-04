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

import java.io.IOException;

import jpcsp.nec78k0.sfr.Nec78k0SerialInterface;
import jpcsp.nec78k0.sfr.Nec78k0SerialInterfaceUART6;
import jpcsp.nec78k0.sfr.Nec78k0Sfr;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * Implementation for the serial interface connected to a Battery.
 *
 * @author gid15
 *
 */
public class BatterySerialInterface extends Nec78k0SerialInterface {
	private static final int STATE_VERSION = 0;

	public BatterySerialInterface(Nec78k0Sfr sfr, Nec78k0SerialInterfaceUART6 serialInterface) {
		super(sfr, serialInterface);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
	}

	public void sendRequest(int sysconCommand, int[] data) {
		if (data == null) {
			data = new int[0];
		}

		addReceptionBufferData8(0x5A);
		addReceptionBufferData8(data.length + 2); // Length
		addReceptionBufferData8(sysconCommand - 0x60); // Command
		for (int data8 : data) {
			addReceptionBufferData8(data8);
		}
		endReceptionBuffer();
	}
}
