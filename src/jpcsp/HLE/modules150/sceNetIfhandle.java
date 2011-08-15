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
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;

import org.apache.log4j.Logger;

public class sceNetIfhandle extends HLEModule {

    protected static Logger log = Modules.getLogger("sceNetIfhandle");

    @Override
    public String getName() {
        return "sceNetIfhandle";
    }

    private int netDropRate;
    private int netDropDuration;

    @HLEFunction(nid = 0xC80181A2, version = 150)
    public void sceNetGetDropRate(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int dropRateAddr = cpu.gpr[4];
        int dropDurationAddr = cpu.gpr[5];

        log.warn("PARTIAL: sceNetGetDropRate (dropRateAddr=0x" + Integer.toHexString(dropRateAddr)
                + ", dropDurationAddr=0x" + Integer.toHexString(dropDurationAddr) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if(Memory.isAddressGood(dropRateAddr) && Memory.isAddressGood(dropDurationAddr)) {
            mem.write32(dropRateAddr, netDropRate);
            mem.write32(dropDurationAddr, netDropDuration);
        }
        cpu.gpr[2] = 0;
    }
    
    @HLEFunction(nid = 0xFD8585E1, version = 150)
    public void sceNetSetDropRate(Processor processor) {
        CpuState cpu = processor.cpu;
        
        int dropRate = cpu.gpr[4];
        int dropDuration = cpu.gpr[5];

        log.warn("PARTIAL: sceNetSetDropRate (dropRate=" + dropRate
                + "%, dropDuration=" + dropDuration + "s)");
        
        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        netDropRate = dropRate;
        netDropDuration = dropDuration;
        cpu.gpr[2] = 0;
    }

}