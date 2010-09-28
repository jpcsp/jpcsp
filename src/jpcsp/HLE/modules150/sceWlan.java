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

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceWlanGetEtherAddrFunction);
            mm.removeFunction(sceWlanGetSwitchStateFunction);

        }
    }
    
    public static int PSP_WLAN_SWITCH_OFF = 0;
    public static int PSP_WLAN_SWITCH_ON = 1;
    private byte[] fakeWlanAddr = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x00, 0x00};

    public void sceWlanGetEtherAddr(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int ether_addr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceWlanGetEtherAddr ether_addr=0x" + Integer.toHexString(ether_addr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mem.isAddressGood(ether_addr)) {
            for (int i = 0; i < fakeWlanAddr.length; i++) {
                mem.write8(ether_addr + i, fakeWlanAddr[i]);
            }
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_WLAN_BAD_PARAMS;
        }
    }

    public void sceWlanGetSwitchState(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceWlanGetSwitchState");
        }

        cpu.gpr[2] = PSP_WLAN_SWITCH_ON;
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
}