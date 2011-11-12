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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.hardware.Interrupts;

import org.apache.log4j.Logger;

public class Kernel_Library extends HLEModule {
    protected static Logger log = Modules.getLogger("Kernel_Library");

    @Override
    public String getName() {
        return "Kernel_Library";
    }

    private final int flagInterruptsEnabled = 1;
    private final int flagInterruptsDisabled = 0;

    /**
     * Suspend all interrupts.
     *
     * @returns The current state of the interrupt controller, to be used with ::sceKernelCpuResumeIntr().
     */
    @HLEFunction(nid = 0x092968F4, version = 150)
    public int sceKernelCpuSuspendIntr() {
        if (log.isDebugEnabled()) {
        	log.debug("sceKernelCpuSuspendIntr interruptsEnabled=" + Interrupts.isInterruptsEnabled());
        }

        int returnValue;
        if (Interrupts.isInterruptsEnabled()) {
        	returnValue = flagInterruptsEnabled;
            Interrupts.disableInterrupts();
        } else {
        	returnValue = flagInterruptsDisabled;
        }

        return returnValue;
    }

    /**
     * Resume all interrupts.
     *
     * @param flags - The value returned from ::sceKernelCpuSuspendIntr().
     */
    @HLEFunction(nid = 0x5F10D406, version = 150)
    public void sceKernelCpuResumeIntr(int flagInterrupts) {
        if (log.isDebugEnabled()) {
        	log.debug("sceKernelCpuResumeIntr flag=" + flagInterrupts);
        }

        if (flagInterrupts == flagInterruptsEnabled) {
        	Interrupts.enableInterrupts();
        } else if (flagInterrupts == flagInterruptsDisabled) {
        	Interrupts.disableInterrupts();
        } else {
        	log.warn("sceKernelCpuResumeIntr unknown flag value " + flagInterrupts);
        }
    }

    /**
     * Resume all interrupts (using sync instructions).
     *
     * @param flags - The value returned from ::sceKernelCpuSuspendIntr()
     */
    @HLEFunction(nid = 0x3B84732D, version = 150)
    public void sceKernelCpuResumeIntrWithSync(int flagInterrupts) {
    	if (log.isDebugEnabled()) {
        	log.debug("sceKernelCpuResumeIntrWithSync redirecting to sceKernelCpuResumeIntr");
    	}
    	sceKernelCpuResumeIntr(flagInterrupts);
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
    	log.warn("sceKernelIsCpuIntrSuspended flag=" + flagInterrupts);

		return flagInterrupts == flagInterruptsDisabled;
    }

    /**
     * Determine if interrupts are enabled or disabled.
     *
     * @returns 1 if interrupts are currently enabled.
     */
    @HLEFunction(nid = 0xB55249D2, version = 150)
    public boolean sceKernelIsCpuIntrEnable() {
        if (log.isDebugEnabled()) {
        	log.debug("sceKernelIsCpuIntrEnable interruptsEnabled=" + Interrupts.isInterruptsEnabled());
        }

        return Interrupts.isInterruptsEnabled();
    }
}