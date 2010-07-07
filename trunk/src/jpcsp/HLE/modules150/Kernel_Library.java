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

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.hardware.Interrupts;

public class Kernel_Library implements HLEModule {

    @Override
    public String getName() {
        return "Kernel_Library";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x092968F4, sceKernelCpuSuspendIntrFunction);
            mm.addFunction(0x5F10D406, sceKernelCpuResumeIntrFunction);
            mm.addFunction(0x3B84732D, sceKernelCpuResumeIntrWithSyncFunction);
            mm.addFunction(0x47A0B729, sceKernelIsCpuIntrSuspendedFunction);
            mm.addFunction(0xB55249D2, sceKernelIsCpuIntrEnableFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceKernelCpuSuspendIntrFunction);
            mm.removeFunction(sceKernelCpuResumeIntrFunction);
            mm.removeFunction(sceKernelCpuResumeIntrWithSyncFunction);
            mm.removeFunction(sceKernelIsCpuIntrSuspendedFunction);
            mm.removeFunction(sceKernelIsCpuIntrEnableFunction);

        }
    }

    private final int flagInterruptsEnabled = 1;
    private final int flagInterruptsDisabled = 0;

    /**
     * Suspend all interrupts.
     *
     * @returns The current state of the interrupt controller, to be used with ::sceKernelCpuResumeIntr().
     */
    public void sceKernelCpuSuspendIntr(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceKernelCpuSuspendIntr interruptsEnabled=" + Interrupts.isInterruptsEnabled());
        }

        if (Interrupts.isInterruptsEnabled()) {
        	cpu.gpr[2] = flagInterruptsEnabled;
            Interrupts.disableInterrupts();
        } else {
        	cpu.gpr[2] = flagInterruptsDisabled;
        }
    }

    /**
     * Resume all interrupts.
     *
     * @param flags - The value returned from ::sceKernelCpuSuspendIntr().
     */
    public void sceKernelCpuResumeIntr(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor

        int flagInterrupts = cpu.gpr[4];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceKernelCpuResumeIntr flag=" + flagInterrupts);
        }

        if (flagInterrupts == flagInterruptsEnabled) {
        	Interrupts.enableInterrupts();
        } else if (flagInterrupts == flagInterruptsDisabled) {
        	Interrupts.disableInterrupts();
        } else {
        	Modules.log.warn("sceKernelCpuResumeIntr unknown flag value " + flagInterrupts);
        }
    }

    /**
     * Resume all interrupts (using sync instructions).
     *
     * @param flags - The value returned from ::sceKernelCpuSuspendIntr()
     */
    public void sceKernelCpuResumeIntrWithSync(Processor processor) {
    	if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceKernelCpuResumeIntrWithSync redirecting to sceKernelCpuResumeIntr");
    	}
    	sceKernelCpuResumeIntr(processor);
    }

    /**
     * Determine if interrupts are suspended or active, based on the given flags.
     *
     * @param flags - The value returned from ::sceKernelCpuSuspendIntr().
     *
     * @returns 1 if flags indicate that interrupts were not suspended, 0 otherwise.
     */
    public void sceKernelIsCpuIntrSuspended(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor

        int flagInterrupts = cpu.gpr[4];
    	Modules.log.warn("sceKernelIsCpuIntrSuspended flag=" + flagInterrupts);

    	if (flagInterrupts == flagInterruptsDisabled) {
    		cpu.gpr[2] = 1;
    	} else {
    		cpu.gpr[2] = 0;
    	}
    }

    /**
     * Determine if interrupts are enabled or disabled.
     *
     * @returns 1 if interrupts are currently enabled.
     */
    public void sceKernelIsCpuIntrEnable(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceKernelIsCpuIntrEnable interruptsEnabled=" + Interrupts.isInterruptsEnabled());
        }

        if (Interrupts.isInterruptsEnabled()) {
        	cpu.gpr[2] = 1;
        } else {
        	cpu.gpr[2] = 0;
        }
    }

    public final HLEModuleFunction sceKernelCpuSuspendIntrFunction = new HLEModuleFunction("Kernel_Library", "sceKernelCpuSuspendIntr") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCpuSuspendIntr(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.Kernel_LibraryModule.sceKernelCpuSuspendIntr(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCpuResumeIntrFunction = new HLEModuleFunction("Kernel_Library", "sceKernelCpuResumeIntr") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCpuResumeIntr(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.Kernel_LibraryModule.sceKernelCpuResumeIntr(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCpuResumeIntrWithSyncFunction = new HLEModuleFunction("Kernel_Library", "sceKernelCpuResumeIntrWithSync") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCpuResumeIntrWithSync(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.Kernel_LibraryModule.sceKernelCpuResumeIntrWithSync(processor);";
        }
    };
    public final HLEModuleFunction sceKernelIsCpuIntrSuspendedFunction = new HLEModuleFunction("Kernel_Library", "sceKernelIsCpuIntrSuspended") {

        @Override
        public final void execute(Processor processor) {
            sceKernelIsCpuIntrSuspended(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.Kernel_LibraryModule.sceKernelIsCpuIntrSuspended(processor);";
        }
    };
    public final HLEModuleFunction sceKernelIsCpuIntrEnableFunction = new HLEModuleFunction("Kernel_Library", "sceKernelIsCpuIntrEnable") {

        @Override
        public final void execute(Processor processor) {
            sceKernelIsCpuIntrEnable(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.Kernel_LibraryModule.sceKernelIsCpuIntrEnable(processor);";
        }
    };
};