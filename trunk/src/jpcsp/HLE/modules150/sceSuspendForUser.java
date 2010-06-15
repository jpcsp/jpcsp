/* This autogenerated file is part of jpcsp. */
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

package jpcsp.HLE.modules150;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;

import jpcsp.Memory;
import jpcsp.Processor;

import jpcsp.Allegrex.CpuState; // New-Style Processor

public class sceSuspendForUser implements HLEModule {
    @Override
    public String getName() { return "sceSuspendForUser"; }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(sceKernelPowerLockFunction, 0xEADB1BD7);
            mm.addFunction(sceKernelPowerUnlockFunction, 0x3AEE7261);
            mm.addFunction(sceKernelPowerTickFunction, 0x090CCB3F);
            mm.addFunction(sceKernelVolatileMemLockFunction, 0x3E0271D3);
            mm.addFunction(sceKernelVolatileMemTryLockFunction, 0xA14F40B2);
            mm.addFunction(sceKernelVolatileMemUnlockFunction, 0xA569E425);

        }

		volatileMemLocked = false;
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceKernelPowerLockFunction);
            mm.removeFunction(sceKernelPowerUnlockFunction);
            mm.removeFunction(sceKernelPowerTickFunction);
            mm.removeFunction(sceKernelVolatileMemLockFunction);
            mm.removeFunction(sceKernelVolatileMemTryLockFunction);
            mm.removeFunction(sceKernelVolatileMemUnlockFunction);

        }
    }

    public static final int KERNEL_POWER_TICK_SUSPEND_AND_DISPLAY = 0;
    public static final int KERNEL_POWER_TICK_SUSPEND = 1;
    public static final int KERNEL_POWER_TICK_DISPLAY = 6;

    private boolean volatileMemLocked;

    public void sceKernelPowerLock(Processor processor) {
        CpuState cpu = processor.cpu;

        if (Modules.log.isTraceEnabled()) {
        	Modules.log.trace("IGNORING:sceKernelPowerLock");
        }

        cpu.gpr[2] = 0;
    }

    public void sceKernelPowerUnlock(Processor processor) {
        CpuState cpu = processor.cpu;

        if (Modules.log.isTraceEnabled()) {
        	Modules.log.trace("IGNORING:sceKernelPowerUnlock");
        }

        cpu.gpr[2] = 0;
    }

    public void sceKernelPowerTick(Processor processor) {
        CpuState cpu = processor.cpu;

        int flag = cpu.gpr[4];
        switch (flag) {
	        case KERNEL_POWER_TICK_SUSPEND_AND_DISPLAY:
	        	if (Modules.log.isTraceEnabled()) {
	        		Modules.log.trace("IGNORING:sceKernelPowerTick(KERNEL_POWER_TICK_SUSPEND_AND_DISPLAY)");
	        	}
	        	break;
	        case KERNEL_POWER_TICK_SUSPEND:
	        	if (Modules.log.isTraceEnabled()) {
	        		Modules.log.trace("IGNORING:sceKernelPowerTick(KERNEL_POWER_TICK_SUSPEND)");
	        	}
	        	break;
	        case KERNEL_POWER_TICK_DISPLAY:
	        	if (Modules.log.isTraceEnabled()) {
	        		Modules.log.trace("IGNORING:sceKernelPowerTick(KERNEL_POWER_TICK_DISPLAY)");
	        	}
	        	break;
        	default:
                Modules.log.warn("IGNORING:sceKernelPowerTick(" + flag + ")");
        		break;
        }

        cpu.gpr[2] = 0;
    }

    protected void hleKernelVolatileMemLock(Processor processor, boolean trylock) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int unk1 = cpu.gpr[4];
        int paddr = cpu.gpr[5];
        int psize = cpu.gpr[6];

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("PARTIAL:hleKernelVolatileMemLock unk1=%d, paddr=0x%08X, psize=0x%08X, trylock=%b", unk1, paddr, psize, trylock));
        }

        if (unk1 != 0) {
            Modules.log.warn("hleKernelVolatileMemLock bad param: unk1 != 0");
            cpu.gpr[2] = ERROR_ARGUMENT;
        } else {
            if (!volatileMemLocked) {
                volatileMemLocked = true;

                if (mem.isAddressGood(paddr)) {
                    mem.write32(paddr, 0x08400000); // Volatile mem is always at 0x08400000
                }

                if (mem.isAddressGood(psize)) {
                    mem.write32(psize,   0x400000); // Volatile mem size is 4Megs
                }

                cpu.gpr[2] = 0;
            } else {
                Modules.log.warn("hleKernelVolatileMemLock already locked");

                if (trylock) {
                    cpu.gpr[2] = 0x802b0200; // unknown meaning
                } else {
                    // TODO implement mem locking using psp semaphores, block here until unlock
                    Modules.log.warn("UNIMPLEMENTED:hleKernelVolatileMemLock blocking current thread");
                    Modules.ThreadManForUserModule.hleBlockCurrentThread();
                    cpu.gpr[2] = -1; // check, probably 0 if/when the psp wakes from the semaphore... when we implement it :)
                }
            }
        }
    }

    public void sceKernelVolatileMemLock(Processor processor) {
        hleKernelVolatileMemLock(processor, false);
    }

    public void sceKernelVolatileMemTryLock(Processor processor) {
        hleKernelVolatileMemLock(processor, true);
    }

    public void sceKernelVolatileMemUnlock(Processor processor) {
        CpuState cpu = processor.cpu;

        int unk1 = cpu.gpr[4];

        Modules.log.debug("sceKernelVolatileMemUnlock(unk1=" + unk1 + ")");

        if (unk1 != 0) {
            Modules.log.warn("sceKernelVolatileMemUnlock bad param: unk1 != 0");
            cpu.gpr[2] = ERROR_ARGUMENT;
        } else if (!volatileMemLocked) {
            Modules.log.warn("sceKernelVolatileMemUnlock - Volatile Memory was not locked!");
            cpu.gpr[2] = ERROR_SEMA_OVERFLOW;
        } else {
            volatileMemLocked = false;
            cpu.gpr[2] = 0;
        }
    }

    public final HLEModuleFunction sceKernelPowerLockFunction = new HLEModuleFunction("sceSuspendForUser", "sceKernelPowerLock") {
        @Override
        public final void execute(Processor processor) {
            sceKernelPowerLock(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSuspendForUserModule.sceKernelPowerLock(processor);";
        }
    };

    public final HLEModuleFunction sceKernelPowerUnlockFunction = new HLEModuleFunction("sceSuspendForUser", "sceKernelPowerUnlock") {
        @Override
        public final void execute(Processor processor) {
            sceKernelPowerUnlock(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSuspendForUserModule.sceKernelPowerUnlock(processor);";
        }
    };

    public final HLEModuleFunction sceKernelPowerTickFunction = new HLEModuleFunction("sceSuspendForUser", "sceKernelPowerTick") {
        @Override
        public final void execute(Processor processor) {
            sceKernelPowerTick(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSuspendForUserModule.sceKernelPowerTick(processor);";
        }
    };

    public final HLEModuleFunction sceKernelVolatileMemLockFunction = new HLEModuleFunction("sceSuspendForUser", "sceKernelVolatileMemLock") {
        @Override
        public final void execute(Processor processor) {
            sceKernelVolatileMemLock(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSuspendForUserModule.sceKernelVolatileMemLock(processor);";
        }
    };

    public final HLEModuleFunction sceKernelVolatileMemTryLockFunction = new HLEModuleFunction("sceSuspendForUser", "sceKernelVolatileMemTryLock") {
        @Override
        public final void execute(Processor processor) {
            sceKernelVolatileMemTryLock(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSuspendForUserModule.sceKernelVolatileMemTryLock(processor);";
        }
    };

    public final HLEModuleFunction sceKernelVolatileMemUnlockFunction = new HLEModuleFunction("sceSuspendForUser", "sceKernelVolatileMemUnlock") {
        @Override
        public final void execute(Processor processor) {
            sceKernelVolatileMemUnlock(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSuspendForUserModule.sceKernelVolatileMemUnlock(processor);";
        }
    };

};
