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
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class SysMemUserForUser extends jpcsp.HLE.modules200.SysMemUserForUser {
	@Override
	public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }
	
	@HLEFunction(nid = 0x2A3E5280, version = 280)
	public void sceKernelQueryMemoryInfo(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceKernelQueryMemoryInfo [0x2A3E5280]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x39F49610, version = 280)
	public void sceKernelGetPTRIG(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceKernelGetPTRIG [0x39F49610]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x6231A71D, version = 280)
	public void sceKernelSetPTRIG(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceKernelSetPTRIG [0x6231A71D]");

		cpu.gpr[2] = 0xDEADC0DE;
	}


}