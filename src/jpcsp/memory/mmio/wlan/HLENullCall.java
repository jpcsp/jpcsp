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

import static jpcsp.arm.ARMInstructions.getRegisterName;
import static jpcsp.arm.ARMProcessor.REG_R0;
import static jpcsp.arm.ARMProcessor.REG_R3;
import static jpcsp.arm.ARMProcessor.REG_R4;
import static jpcsp.arm.ARMProcessor.REG_SP;

import jpcsp.arm.ARMMemory;
import jpcsp.arm.ARMProcessor;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class HLENullCall extends BaseHLECall {
	private int numberArguments;
	private boolean doReturnValue;
	private int returnValue;

	public HLENullCall(int numberArguments) {
		this.numberArguments = numberArguments;
	}

	public HLENullCall(int numberArguments, int returnValue) {
		this.numberArguments = numberArguments;
		doReturnValue = true;
		this.returnValue = returnValue;
	}

	@Override
	public void call(ARMProcessor processor, int imm) {
		if (log.isDebugEnabled()) {
			StringBuilder args = new StringBuilder();
			int[] memoryDumps = null;
			String[] memoryDumpsPrefix = null;
			for (int i = REG_R0; i < numberArguments; i++) {
				int value;
				String name;
				if (i <= REG_R3) {
					name = getRegisterName(i);
					value = processor.getRegister(i);
				} else {
					int sp = processor.getSp();
					int offset = (i - REG_R4) << 2;
					name = String.format("[%s, #0x%X]", getRegisterName(REG_SP), offset);
					value = processor.mem.read32(sp + offset);
				}
				args.append(String.format(", %s=0x%08X", name, value));

				if (log.isTraceEnabled() && ARMMemory.isAddressGood(value) && (value < 0 || value >= 0x200)) {
					memoryDumpsPrefix = Utilities.add(memoryDumpsPrefix, name);
					memoryDumps = Utilities.add(memoryDumps, value);
				}
			}
			log.debug(String.format("HLENullCall imm=0x%X%s", imm, args));

			if (memoryDumps != null) {
				for (int i = 0; i < memoryDumps.length; i++) {
					int addr = memoryDumps[i];
					log.trace(String.format("%s: %s", memoryDumpsPrefix[i], Utilities.getMemoryDump(processor.mem, addr, 0x10)));
				}
			}
		}

		if (doReturnValue) {
			processor.setRegister(REG_R0, returnValue);
		}

		returnToLr(processor);
	}
}
