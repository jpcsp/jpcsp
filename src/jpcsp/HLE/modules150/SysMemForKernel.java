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
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;

import org.apache.log4j.Logger;

public class SysMemForKernel implements HLEModule, HLEStartModule {
    protected static Logger log = Modules.getLogger("SysMemForKernel");

    @Override
    public String getName() {
        return "SysMemForKernel";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    public void sceKernelMemset(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int dest_addr = cpu.gpr[4];
        int data = cpu.gpr[5];
        int size = cpu.gpr[6];

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceKernelMemset addr=0x%08X, data=0x%02X, size=%d", dest_addr, data, size));
        }

        mem.memset(dest_addr, (byte) data, size);

        cpu.gpr[2] = 0;
    }
    @HLEFunction(nid = 0xA089ECA4, version = 150)
    public final HLEModuleFunction sceKernelMemsetFunction = new HLEModuleFunction("SysMemForKernel", "sceKernelMemset") {

        @Override
        public final void execute(Processor processor) {
            sceKernelMemset(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.SysMemUserForUserModule.sceKernelMemset(processor);";
        }
    };
}