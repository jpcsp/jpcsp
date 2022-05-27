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
package jpcsp.HLE.modules;

import jpcsp.Processor;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceKernelTls;

import org.apache.log4j.Logger;

public class Kernel_Library extends HLEModule {
    public static Logger log = Modules.getLogger("Kernel_Library");

    private static final int flagInterruptsEnabled = 1;
    private static final int flagInterruptsDisabled = 0;

    /**
     * Suspend all interrupts.
     *
     * @returns The current state of the interrupt controller, to be used with ::sceKernelCpuResumeIntr().
     */
    @HLEFunction(nid = 0x092968F4, version = 150)
    public boolean sceKernelCpuSuspendIntr(Processor processor) {
        boolean returnValue = processor.isInterruptsEnabled();
    	processor.disableInterrupts();

        return returnValue;
    }

    protected void hleKernelCpuResumeIntr(Processor processor, int flagInterrupts) {
        if (flagInterrupts == flagInterruptsEnabled) {
        	processor.enableInterrupts();
        } else if (flagInterrupts == flagInterruptsDisabled) {
        	processor.disableInterrupts();
        } else {
        	log.warn(String.format("hleKernelCpuResumeIntr unknown flag value 0x%X", flagInterrupts));
        }
    }

    /**
     * Resume all interrupts.
     *
     * @param flags - The value returned from ::sceKernelCpuSuspendIntr().
     */
    @HLEFunction(nid = 0x5F10D406, version = 150)
    public void sceKernelCpuResumeIntr(Processor processor, int flagInterrupts) {
    	hleKernelCpuResumeIntr(processor, flagInterrupts);
    }

    /**
     * Resume all interrupts (using sync instructions).
     *
     * @param flags - The value returned from ::sceKernelCpuSuspendIntr()
     */
    @HLEFunction(nid = 0x3B84732D, version = 150)
    public void sceKernelCpuResumeIntrWithSync(Processor processor, int flagInterrupts) {
    	hleKernelCpuResumeIntr(processor, flagInterrupts);
    }

    /**
     * Determine if interrupts are suspended or active, based on the given flags.
     *
     * @param flags - The value returned from ::sceKernelCpuSuspendIntr().
     *
     * @returns 1 if flags indicate that interrupts were not suspended, 0 otherwise.
     */
    @HLEFunction(nid = 0x47A0B729, version = 150)
    public boolean sceKernelIsCpuIntrSuspended(int flagInterrupts) {
		return flagInterrupts == flagInterruptsDisabled;
    }

    /**
     * Determine if interrupts are enabled or disabled.
     *
     * @returns 1 if interrupts are currently enabled.
     */
    @HLEFunction(nid = 0xB55249D2, version = 150)
    public boolean sceKernelIsCpuIntrEnable(Processor processor) {
        return processor.isInterruptsEnabled();
    }

	@HLEFunction(nid = 0x15B6446B, version = 150, checkInsideInterrupt = true)
	public int sceKernelUnlockLwMutex(TPointer workAreaAddr, int count) {
		return Managers.lwmutex.sceKernelUnlockLwMutex(workAreaAddr, count);
	}

	@HLEFunction(nid = 0x1FC64E09, version = 380, checkInsideInterrupt = true)
	public int sceKernelLockLwMutexCB(TPointer workAreaAddr, int count, @CanBeNull TPointer32 timeoutAddr) {
		return Managers.lwmutex.sceKernelLockLwMutexCB(workAreaAddr, count, timeoutAddr);
	}

	@HLEFunction(nid = 0xBEA46419, version = 150, checkInsideInterrupt = true)
	public int sceKernelLockLwMutex(TPointer workAreaAddr, int count, @CanBeNull TPointer32 timeoutAddr) {
		return Managers.lwmutex.sceKernelLockLwMutex(workAreaAddr, count, timeoutAddr);
	}

	@HLEFunction(nid = 0xC1734599, version = 380)
	public int sceKernelReferLwMutexStatus(TPointer workAreaAddr, TPointer addr) {
		return Managers.lwmutex.sceKernelReferLwMutexStatus(workAreaAddr, addr);
	}

	@HLEFunction(nid = 0xDC692EE3, version = 380, checkInsideInterrupt = true)
    @HLEFunction(nid = 0x37431849, version = 600, checkInsideInterrupt = true)
	public int sceKernelTryLockLwMutex(TPointer workAreaAddr, int count) {
		return Managers.lwmutex.sceKernelTryLockLwMutex(workAreaAddr, count);
	}

    @HLELogging(level="trace")
    @HLEFunction(nid = 0x1839852A, version = 150)
    public int sceKernelMemcpy(@BufferInfo(lengthInfo=LengthInfo.nextNextParameter, usage=Usage.out) TPointer dst, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer src, int length) {
    	if (dst.getAddress() != src.getAddress()) {
    		dst.getMemory().memcpyWithVideoCheck(dst.getAddress(), src.getAddress(), length);
    	}

		return dst.getAddress();
	}

    @HLEFunction(nid = 0xA089ECA4, version = 150)
	@HLEFunction(nid = 0x8AE776AF, version = 660)
    public int sceKernelMemset(TPointer destAddr, int data, int size) {
        destAddr.memset((byte) data, size);

        return 0;
    }

    @HLEFunction(nid = 0xFA835CDE, version = 620)
	public int sceKernel_FA835CDE(int uid) {
		SceKernelTls tls = Modules.ThreadManForUserModule.getKernelTls(uid);
		if (tls == null) {
			return 0;
		}

		int addr = tls.getTlsAddress();
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceKernel_FA835CDE returning 0x%08X", addr));
		}

		return addr;
	}
}