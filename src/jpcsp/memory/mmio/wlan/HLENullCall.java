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

import static jpcsp.arm.ARMProcessor.REG_R0;

import jpcsp.arm.ARMInstructions;
import jpcsp.arm.ARMProcessor;

/**
 * @author gid15
 *
 */
public class HLENullCall extends BaseHLECall {
	private int numberArguments;

	public HLENullCall(int numberArguments) {
		this.numberArguments = numberArguments;
	}

	@Override
	public void call(ARMProcessor processor, int imm) {
		if (log.isDebugEnabled()) {
			StringBuilder args = new StringBuilder();
			for (int i = REG_R0; i < numberArguments; i++) {
				args.append(String.format(", %s=0x%X", ARMInstructions.getRegisterName(i), processor.getRegister(i)));
			}
			log.debug(String.format("HLENullCall imm=0x%X%s", imm, args));
		}

		returnToLr(processor);
	}
}
