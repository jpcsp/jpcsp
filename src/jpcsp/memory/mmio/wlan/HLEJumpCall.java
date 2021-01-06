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
public class HLEJumpCall extends BaseHLECall {
	private int register;

	public HLEJumpCall(int register) {
		this.register = register;
	}

	@Override
	public void call(ARMProcessor processor, int imm) {
		int addr = processor.getRegister(register);
		if (log.isDebugEnabled()) {
			log.debug(String.format("HLEJumpR0Call imm=0x%X, r0=0x%08X", imm, addr));
		}

		jump(processor, addr);
	}
}
