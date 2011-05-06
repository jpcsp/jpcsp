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

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.hardware.Wlan;

import org.apache.log4j.Logger;

public class sceWlan implements HLEModule {

    protected static Logger log = Modules.getLogger("sceWlan");

    @Override
    public String getName() {
        return "sceWlan";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {
            mm.addFunction(0x0C622081, sceWlanGetEtherAddrFunction);
            mm.addFunction(0xD7763699, sceWlanGetSwitchStateFunction);
            mm.addFunction(0x93440B11, sceWlanDevIsPowerOnFunction);
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {
            mm.removeFunction(sceWlanGetEtherAddrFunction);
            mm.removeFunction(sceWlanGetSwitchStateFunction);
            mm.removeFunction(sceWlanDevIsPowerOnFunction);
        }
    }

    /**
     * Get the Ethernet Address of the wlan controller
     *
     * @param etherAddr - pointer to a buffer of u8 (NOTE: it only writes to 6 bytes, but
     * requests 8 so pass it 8 bytes just in case)
     * @return 0 on success, < 0 on error
     */
    public void sceWlanGetEtherAddr(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int ether_addr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceWlanGetEtherAddr ether_addr=0x" + Integer.toHexString(ether_addr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(ether_addr)) {
        	byte[] wlanAddr = Wlan.getMacAddress();
            for (int i = 0; i < wlanAddr.length; i++) {
                mem.write8(ether_addr + i, wlanAddr[i]);
            }
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_WLAN_BAD_PARAMS;
        }
    }

    /**
     * Determine the state of the Wlan power switch
     *
     * @return 0 if off, 1 if on
     */
    public void sceWlanGetSwitchState(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceWlanGetSwitchState");
        }

        cpu.gpr[2] = Wlan.getSwitchState();
    }

    /**
     * Determine if the wlan device is currently powered on
     *
     * @return 0 if off, 1 if on
     */
    public void sceWlanDevIsPowerOn(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceWlanDevIsPowerOn");
        }

        cpu.gpr[2] = Wlan.getSwitchState();
    }

    public final HLEModuleFunction sceWlanGetEtherAddrFunction = new HLEModuleFunction("sceWlan", "sceWlanGetEtherAddr") {

        @Override
        public final void execute(Processor processor) {
            sceWlanGetEtherAddr(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceWlanModule.sceWlanGetEtherAddr(processor);";
        }
    };

    public final HLEModuleFunction sceWlanGetSwitchStateFunction = new HLEModuleFunction("sceWlan", "sceWlanGetSwitchState") {

        @Override
        public final void execute(Processor processor) {
            sceWlanGetSwitchState(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceWlanModule.sceWlanGetSwitchState(processor);";
        }
    };

    public final HLEModuleFunction sceWlanDevIsPowerOnFunction = new HLEModuleFunction("sceWlan", "sceWlanDevIsPowerOn") {

        @Override
        public final void execute(Processor processor) {
        	sceWlanDevIsPowerOn(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceWlanModule.sceWlanDevIsPowerOn(processor);";
        }
    };
}