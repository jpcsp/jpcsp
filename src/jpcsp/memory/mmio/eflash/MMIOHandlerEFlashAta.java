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
package jpcsp.memory.mmio.eflash;

import java.io.IOException;

import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.modules.sceEFlash;
import jpcsp.memory.mmio.MMIOHandlerBaseAta;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerEFlashAta extends MMIOHandlerBaseAta {
	private static final int STATE_VERSION = 0;

	public MMIOHandlerEFlashAta(int baseAddress) {
		super(baseAddress);

		log = sceEFlash.log;
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
	protected int getInterruptNumber() {
		return IntrManager.PSP_EFLASH1_INTR;
	}

	@Override
	protected void executePacketCommand(int[] data) {
		int operationCode = data[0];

		switch (operationCode) {
			default:
				log.error(String.format("MMIOHandlerEFlashAta.executePacketCommand unknown operation code 0x%02X(%s)", operationCode, getOperationCodeName(operationCode)));
				break;
		}
	}
}
