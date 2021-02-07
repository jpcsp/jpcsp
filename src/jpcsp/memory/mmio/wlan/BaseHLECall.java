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

import static jpcsp.arm.ARMInterpreter.PC_END_RUN;
import static jpcsp.arm.ARMProcessor.REG_R0;
import static jpcsp.arm.ARMProcessor.REG_R3;
import static jpcsp.arm.ARMProcessor.REG_R4;
import static jpcsp.util.Utilities.clearBit;

import org.apache.log4j.Logger;

import jpcsp.arm.ARMProcessor;
import jpcsp.arm.IARMHLECall;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public abstract class BaseHLECall implements IARMHLECall {
	public static Logger log = WlanEmulator.log;

	protected void jump(ARMProcessor processor, int addr) {
		processor.jumpWithMode(addr);
	}

	protected void returnToLr(ARMProcessor processor) {
		jump(processor, processor.getLr());
	}

	protected void returnToLr(ARMProcessor processor, int returnValue) {
		processor.setRegister(REG_R0, returnValue);
		returnToLr(processor);
	}

	protected int getParameterValue(ARMProcessor processor, int n) {
		int value;
		if (n <= REG_R3) {
			value = processor.getRegister(n);
		} else {
			int sp = processor.getSp();
			int offset = (n - REG_R4) << 2;
			value = processor.mem.read32(sp + offset);
		}

		return value;
	}

	protected String readStringZ(ARMProcessor processor, int address) {
		if (address == 0) {
			return null;
		}

		return Utilities.readStringZ(processor.mem, address);
	}

	protected String readStringNZ(ARMProcessor processor, int address, int length) {
		if (address == 0) {
			return null;
		}

		return Utilities.readStringNZ(processor.mem, address, length);
	}

	protected void execute(ARMProcessor processor, int addr, String comment) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Starting CodeBlock 0x%08X for %s", clearBit(addr, 0), comment));
		}

		processor.jumpWithMode(addr);
		processor.setLr(PC_END_RUN);
		processor.interpreter.run();

		if (log.isDebugEnabled()) {
			log.debug(String.format("Returning from CodeBlock 0x%08X for %s", clearBit(addr, 0), comment));
		}
	}

	protected String getMemoryDump(ARMProcessor processor, int addr, int length) {
		return Utilities.getMemoryDump(processor.mem, addr, length);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
