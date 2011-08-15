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

package jpcsp.HLE.modules380;

import jpcsp.HLE.HLEFunction;
import jpcsp.Allegrex.CpuState;
import jpcsp.Processor;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class Kernel_Library extends jpcsp.HLE.modules150.Kernel_Library {

	@HLEFunction(nid = 0x15B6446B, version = 380)
	public void sceKernelUnlockLwMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
		Managers.lwmutex.sceKernelUnlockLwMutex(gpr[4], gpr[5]);
	}

	@HLEFunction(nid = 0x1FC64E09, version = 380)
	public void sceKernelLockLwMutexCB(Processor processor) {
		int[] gpr = processor.cpu.gpr;
        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
		Managers.lwmutex.sceKernelLockLwMutexCB(gpr[4], gpr[5], gpr[6]);
	}

	@HLEFunction(nid = 0xBEA46419, version = 380)
	public void sceKernelLockLwMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
		Managers.lwmutex.sceKernelLockLwMutex(gpr[4], gpr[5], gpr[6]);
	}

	@HLEFunction(nid = 0xC1734599, version = 380)
	public void sceKernelReferLwMutexStatus(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.lwmutex.sceKernelReferLwMutexStatus(gpr[4], gpr[5]);
	}

	@HLEFunction(nid = 0xDC692EE3, version = 380)
	public void sceKernelTryLockLwMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
		Managers.lwmutex.sceKernelTryLockLwMutex(gpr[4], gpr[5]);
	}

    @HLEFunction(nid = 0x37431849, version = 380)
    public void sceKernelTryLockLwMutex_600(Processor processor) {
        int[] gpr = processor.cpu.gpr;

        if(log.isDebugEnabled()) {
            log.debug("sceKernelTryLockLwMutex_600 redirecting to sceKernelTryLockLwMutex");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
		Managers.lwmutex.sceKernelTryLockLwMutex(gpr[4], gpr[5]);
	}

    @HLEFunction(nid = 0x1839852A, version = 380)
    public void sceKernelMemcpy(Processor processor) {
		CpuState cpu = processor.cpu;

		int dst = cpu.gpr[4];
		int src = cpu.gpr[5];
		int length = cpu.gpr[6];

		Processor.memory.memcpy(dst, src, length);

		cpu.gpr[2] = dst;
	}

}