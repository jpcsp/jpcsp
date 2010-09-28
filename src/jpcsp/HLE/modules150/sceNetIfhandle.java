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

public class sceNetIfhandle implements HLEModule {

    protected static Logger log = Modules.getLogger("sceNetIfhandle");

    @Override
    public String getName() {
        return "sceNetIfhandle";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0xC80181A2, sceNetGetDropRateFunction);
            mm.addFunction(0xFD8585E1, sceNetSetDropRateFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceNetGetDropRateFunction);
            mm.removeFunction(sceNetSetDropRateFunction);

        }
    }

    private int netDropRate;
    private int netDropDuration;

    public void sceNetGetDropRate(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int dropRateAddr = cpu.gpr[4];
        int dropDurationAddr = cpu.gpr[5];

        log.warn("PARTIAL: sceNetGetDropRate (dropRateAddr=0x" + Integer.toHexString(dropRateAddr)
                + ", dropDurationAddr=0x" + Integer.toHexString(dropDurationAddr) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if(mem.isAddressGood(dropRateAddr) && mem.isAddressGood(dropDurationAddr)) {
            mem.write32(dropRateAddr, netDropRate);
            mem.write32(dropDurationAddr, netDropDuration);
        }
        cpu.gpr[2] = 0;
    }
    
    public void sceNetSetDropRate(Processor processor) {
        CpuState cpu = processor.cpu;
        
        int dropRate = cpu.gpr[4];
        int dropDuration = cpu.gpr[5];

        log.warn("PARTIAL: sceNetSetDropRate (dropRate=" + dropRate
                + "%, dropDuration=" + dropDuration + "s)");
        
        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        netDropRate = dropRate;
        netDropDuration = dropDuration;
        cpu.gpr[2] = 0;
    }

    public final HLEModuleFunction sceNetGetDropRateFunction = new HLEModuleFunction("sceNetIfhandle", "sceNetGetDropRate") {

        @Override
        public final void execute(Processor processor) {
            sceNetGetDropRate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetIfhandleModule.sceNetGetDropRate(processor);";
        }
    };

    public final HLEModuleFunction sceNetSetDropRateFunction = new HLEModuleFunction("sceNetIfhandle", "sceNetSetDropRate") {

        @Override
        public final void execute(Processor processor) {
            sceNetSetDropRate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetIfhandleModule.sceNetSetDropRate(processor);";
        }
    };
}