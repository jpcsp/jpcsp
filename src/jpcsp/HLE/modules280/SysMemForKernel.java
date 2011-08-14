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

package jpcsp.HLE.modules280;

import jpcsp.HLE.HLEFunction;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;

public class SysMemForKernel extends jpcsp.HLE.modules150.SysMemForKernel implements HLEModule, HLEStartModule {

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

    @HLEFunction(nid = 0x6373995D, version = 280)
    public void sceKernelGetModel(Processor processor) {
		CpuState cpu = processor.cpu;

		int result = 0; // <= 0 original, 1 slim

        log.debug("sceKernelGetModel ret:" + result);

        cpu.gpr[2] = result;
	}

}