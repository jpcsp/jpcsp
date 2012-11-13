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
package jpcsp.HLE.modules620;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules150.SysMemUserForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.util.Utilities;

@HLELogging
public class ThreadManForUser extends jpcsp.HLE.modules380.ThreadManForUser {
    protected final static int PSP_ATTR_ADDR_HIGH = 0x4000;

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
			cpu._sp = savedSp;
			cpu._ra = savedRa;

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

	@HLEFunction(nid = 0xBC80EC7C, version = 620, checkInsideInterrupt = true)
    public int sceKernelExtendThreadStack(CpuState cpu, @CheckArgument("checkStackSize") int size, TPointer entryAddr, int entryParameter) {
        // sceKernelExtendThreadStack executes the code at entryAddr using a larger
        // stack. The entryParameter is  passed as the only parameter ($a0) to
        // the code at entryAddr.
        // When the code at entryAddr returns, sceKernelExtendThreadStack also returns
		// with the return value of entryAddr.
        SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
        int extendedStackAddr = thread.extendStack(size);
        IAction afterAction = new AfterSceKernelExtendThreadStackAction(thread, cpu.pc, cpu._sp, cpu._ra);
        cpu._a0 = entryParameter;
        cpu._sp = extendedStackAddr + size;
        Modules.ThreadManForUserModule.callAddress(entryAddr.getAddress(), afterAction, false);

        return 0;
    }

	@HLEFunction(nid = 0x8DAFF657, version = 620)
	public int ThreadManForUser_8DAFF657(PspString name, int partitionid, int attr, int blockSize, int numberBlocks, @CanBeNull TPointer optionsAddr) {
		// Similar to sceKernelAllocPartitionMemory?
		int type = SysMemUserForUser.PSP_SMEM_LowAligned;
		if ((attr & PSP_ATTR_ADDR_HIGH) != 0) {
			type = SysMemUserForUser.PSP_SMEM_HighAligned;
		}

		int alignment = 4;
		if (optionsAddr.isNotNull()) {
			int length = optionsAddr.getValue32(0);
			if (length >= 8) {
				alignment = optionsAddr.getValue32(4);
			}
		}

		blockSize = Utilities.alignUp(blockSize, 3);
		int size = blockSize * numberBlocks;
		SysMemInfo info = Modules.SysMemUserForUserModule.malloc(partitionid, name.getString(), type, size, alignment);
		if (info == null) {
			return -1;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("ThreadManForUser_8DAFF657 allocated addr 0x%08X, returning 0x%X", info.addr, info.uid));
		}

		return info.uid;
	}

	@HLEFunction(nid = 0x32BF938E, version = 620)
	public int ThreadManForUser_32BF938E(int uid) {
		// Similar to sceKernelFreePartitionMemory?
		SysMemInfo info = Modules.SysMemUserForUserModule.getSysMemInfo(uid);
		if (info == null) {
			return -1;
		}

		// Clear the memory and free it
		Memory.getInstance().memset(info.addr, (byte) 0, info.size);
		Modules.SysMemUserForUserModule.free(info);

		return 0;
	}
}