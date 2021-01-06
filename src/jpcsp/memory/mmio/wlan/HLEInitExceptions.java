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
package jpcsp.memory.mmio.wlan;

import jpcsp.arm.ARMProcessor;

/**
 * @author gid15
 *
 */
public class HLEInitExceptions extends BaseHLECall {
	@Override
	public void call(ARMProcessor processor, int imm) {
		log.error(String.format("Unimplemented HLEInitExceptions imm=0x%X", imm));

		if (imm == 1) {
//			jump(processor, 0x00000F59);
			processor.setRegister(0, 0x12345678);
			jump(processor, 0x0000EFD9);
		} else {
			returnToLr(processor);
		}
	}
}
