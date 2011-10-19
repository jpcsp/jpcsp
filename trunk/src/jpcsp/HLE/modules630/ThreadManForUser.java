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
package jpcsp.HLE.modules630;

import static jpcsp.Allegrex.Common._a0;
import static jpcsp.Allegrex.Common._ra;
import static jpcsp.Allegrex.Common._sp;

import jpcsp.Emulator;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;

public class ThreadManForUser extends jpcsp.HLE.modules380.ThreadManForUser {
	private static class AfterSceKernelExtendThreadStackAction implements IAction {
		private SceKernelThreadInfo thread;
		private int savedPc;
		private int savedSp;
		private int savedRa;

		public AfterSceKernelExtendThreadStackAction(SceKernelThreadInfo thread, int savedPc, int savedSp, int savedRa) {
			this.thread = thread;
			this.savedPc = savedPc;
			this.savedSp = savedSp;
			this.savedRa = savedRa;
		}

		@Override
		public void execute() {
			CpuState cpu = Emulator.getProcessor().cpu;

			if (log.isDebugEnabled()) {
				log.debug(String.format("AfterSceKernelExtendThreadStackAction savedSp=0x%08X, savedRa=0x%08X", savedSp, savedRa));
			}

			cpu.pc = savedPc;
			cpu.gpr[_sp] = savedSp;
			cpu.gpr[_ra] = savedRa;

			// The return value in $v0 of the entryAdd is passed back as return value
			// of sceKernelExtendThreadStack.

			thread.freeExtendedStack();
		}
	}

	public int checkStackSize(int size) {
		if (size < 0x200) {
        	throw new SceKernelErrorException(SceKernelErrors.ERROR_KERNEL_ILLEGAL_STACK_SIZE);
		}

		// Size is rounded up to a multiple of 256
		return (size + 0xFF) & ~0xFF;
	}

	@HLEFunction(nid = 0xBC80EC7C, version = 630, checkInsideInterrupt = true)
    public int sceKernelExtendThreadStack(Processor processor, @CheckArgument("checkStackSize") int size, TPointer entryAddr, int entryParameter) {
		CpuState cpu = processor.cpu;

		if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelExtendThreadStack size=0x%X, entryAddr=%s, entryParameter=0x%08X", size, entryAddr, entryParameter));
        }

        // sceKernelExtendThreadStack executes the code at entryAddr using a larger
        // stack. The entryParameter is  passed as the only parameter ($a0) to
        // the code at entryAddr.
        // When the code at entryAddr returns, sceKernelExtendThreadStack also returns
		// with the return value of entryAddr.
        SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
        int extendedStackAddr = thread.extendStack(size);
        IAction afterAction = new AfterSceKernelExtendThreadStackAction(thread, cpu.pc, cpu.gpr[_sp], cpu.gpr[_ra]);
        cpu.gpr[_a0] = entryParameter;
        cpu.gpr[_sp] = extendedStackAddr + size;
        Modules.ThreadManForUserModule.callAddress(entryAddr.getAddress(), afterAction, false);

        return 0;
    }
}