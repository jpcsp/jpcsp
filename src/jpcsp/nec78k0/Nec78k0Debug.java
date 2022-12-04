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
package jpcsp.nec78k0;

import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.getInterruptName;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class Nec78k0Debug {
	private final Logger log;
	private final StackCallInfo stackCallInfos[] = new StackCallInfo[128];
	private int stackCallInfoIndex;

	private static class StackCallInfo {
		public int pc;
		public int callAddress;
		public int sp;
		public int vectorTableAddress;
		public boolean isInterrupt;
	}

	public Nec78k0Debug(Logger log) {
		this.log = log;

		for (int i = 0; i < stackCallInfos.length; i++) {
			stackCallInfos[i] = new StackCallInfo();
		}
		stackCallInfoIndex = 0;
	}

	public void call(Nec78k0Processor processor, int addr) {
		if (stackCallInfoIndex < stackCallInfos.length) {
			stackCallInfos[stackCallInfoIndex].pc = processor.getCurrentInstructionPc();
			stackCallInfos[stackCallInfoIndex].callAddress = addr;
			stackCallInfos[stackCallInfoIndex].sp = processor.getSp();
			stackCallInfos[stackCallInfoIndex].isInterrupt = false;
			stackCallInfoIndex++;
		} else {
			log.error(String.format("Stack overflow"));
		}
	}

	public void ret() {
		if (stackCallInfoIndex == 0) {
			log.error(String.format("Stack underflow"));
		} else {
			stackCallInfoIndex--;
		}
	}

	public void callInterrupt(Nec78k0Processor processor, int addr, int vectorTableAddr) {
		if (stackCallInfoIndex < stackCallInfos.length) {
			stackCallInfos[stackCallInfoIndex].pc = processor.getCurrentInstructionPc();
			stackCallInfos[stackCallInfoIndex].callAddress = addr;
			stackCallInfos[stackCallInfoIndex].sp = processor.getSp();
			stackCallInfos[stackCallInfoIndex].vectorTableAddress = vectorTableAddr;
			stackCallInfos[stackCallInfoIndex].isInterrupt = true;
			stackCallInfoIndex++;
		} else {
			log.error(String.format("Stack overflow"));
		}
	}

	public void dump() {
		for (int i = 0; i < stackCallInfoIndex; i++) {
			String interruptName = stackCallInfos[i].isInterrupt ? String.format("(%s)", getInterruptName(stackCallInfos[i].vectorTableAddress)) : "";
			log.debug(String.format("Stack#0x%02X: 0x%04X - %s !0x%04X%s; sp=0x%04X", i, stackCallInfos[i].pc, stackCallInfos[i].isInterrupt ? "interrupt" : "call", stackCallInfos[i].callAddress, interruptName, stackCallInfos[i].sp));
		}
	}
}
