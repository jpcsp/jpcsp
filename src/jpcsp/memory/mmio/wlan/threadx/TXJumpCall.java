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
package jpcsp.memory.mmio.wlan.threadx;

import static jpcsp.arm.ARMInstructions.getRegisterName;
import static jpcsp.arm.ARMProcessor.REG_R0;
import static jpcsp.memory.mmio.wlan.threadx.hle.TXManager.disassembleFunctions;

import jpcsp.arm.ARMProcessor;

/**
 * @author gid15
 *
 */
public class TXJumpCall extends TXBaseCall {
	private int register;

	public TXJumpCall(int register) {
		this.register = register;
	}

	@Override
	public void call(ARMProcessor processor, int imm) {
		int addr = processor.getRegister(register);

		if (disassembleFunctions) {
			getTxManager().disassemble(processor, String.format("Disassembling %s", this), addr);
		}

		if (log.isDebugEnabled()) {
			StringBuilder args = new StringBuilder();
			for (int i = REG_R0; i < register; i++) {
				args.append(String.format(", %s=0x%08X", getRegisterName(i), processor.getRegister(i)));
			}
			log.debug(String.format("HLEJumpCall %s=0x%08X%s", getRegisterName(register), addr, args));
		}

		jump(processor, addr);
	}

	@Override
	public String toString() {
		return String.format("TXJumpCall %s", getRegisterName(register));
	}
}
