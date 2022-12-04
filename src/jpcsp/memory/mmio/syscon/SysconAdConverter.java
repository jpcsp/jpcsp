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

import static jpcsp.util.Utilities.u8;

import jpcsp.State;
import jpcsp.nec78k0.sfr.Nec78k0AdConverter;
import jpcsp.nec78k0.sfr.Nec78k0Scheduler;

/**
 * @author gid15
 *
 */
public class SysconAdConverter extends Nec78k0AdConverter {
	public SysconAdConverter(MMIOHandlerSysconFirmwareSfr sfr, Nec78k0Scheduler scheduler) {
		super(sfr, scheduler);
	}

	@Override
	protected int updateResult(int inputChannel) {
		int result;

		switch (inputChannel) {
			case 5: // Video detect
				// The result will impact the value returned by PSP_SYSCON_CMD_GET_VIDEO_CABLE:
				//      if (result <= 0x1AFF) videoCable = 0x06
				// else if (result <= 0x41FF) videoCable = 0x05
				// else if (result <= 0x69FF) videoCable = 0x01
				// else if (result <= 0x90FF) videoCable = 0x04
				// else if (result <= 0xB7FF) videoCable = 0x03
				// else if (result <= 0xDEFF) videoCable = 0x02
				// else                       videoCable = 0x00 (probably meaning that no video cable is connected)
				result = 0xFF00; // No video cable connected
				break;
			case 6: // Analog X
				State.controller.hleControllerPoll();
				result = u8(State.controller.getLx()) << 8; // 0x8000 means center
				break;
			case 7: // Analog Y
				State.controller.hleControllerPoll();
				result = u8(State.controller.getLy()) << 8; // 0x8000 means center
				break;
			default:
				log.error(String.format("SysconAdConverter unknown inputChannel=%d", inputChannel));
				result = 0x0000;
				break;
		}

		return result;
	}
}
