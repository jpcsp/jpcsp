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

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class SysMemUserForUser extends jpcsp.HLE.modules200.SysMemUserForUser {
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		super.installModule(mm, version);
		
		if (version >= 280) {
		
			mm.addFunction(0x2A3E5280, sceKernelQueryMemoryInfoFunction);
			mm.addFunction(0x39F49610, sceKernelGetPTRIGFunction);
			mm.addFunction(0x6231A71D, sceKernelSetPTRIGFunction);
			
			// Kernel export
			mm.addFunction(0x6373995D, sceKernelGetModelFunction);
			
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		super.uninstallModule(mm, version);
		
		if (version >= 280) {
		
			mm.removeFunction(sceKernelQueryMemoryInfoFunction);
			mm.removeFunction(sceKernelGetPTRIGFunction);
			mm.removeFunction(sceKernelSetPTRIGFunction);
			
			// Kernel export
			mm.removeFunction(sceKernelGetModelFunction);
		}
	}
	
	public void sceKernelQueryMemoryInfo(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceKernelQueryMemoryInfo [0x2A3E5280]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelGetPTRIG(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceKernelGetPTRIG [0x39F49610]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelSetPTRIG(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceKernelSetPTRIG [0x6231A71D]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
	
	public void sceKernelGetModel(Processor processor) {
		CpuState cpu = processor.cpu;

		int result = 0; // <= 0 original, 1 slim

        log.debug("sceKernelGetModel ret:" + result);

        cpu.gpr[2] = result;
	}
        
	public final HLEModuleFunction sceKernelQueryMemoryInfoFunction = new HLEModuleFunction("SysMemUserForUser", "sceKernelQueryMemoryInfo") {
		@Override
		public final void execute(Processor processor) {
			sceKernelQueryMemoryInfo(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.SysMemUserForUserModule.sceKernelQueryMemoryInfo(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelGetPTRIGFunction = new HLEModuleFunction("SysMemUserForUser", "sceKernelGetPTRIG") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGetPTRIG(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.SysMemUserForUserModule.sceKernelGetPTRIG(processor);";
		}
	};
        
	public final HLEModuleFunction sceKernelSetPTRIGFunction = new HLEModuleFunction("SysMemUserForUser", "sceKernelSetPTRIG") {
		@Override
		public final void execute(Processor processor) {
			sceKernelSetPTRIG(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.SysMemUserForUserModule.sceKernelSetPTRIG(processor);";
		}
	};
	
	public final HLEModuleFunction sceKernelGetModelFunction = new HLEModuleFunction("SysMemForKernel", "sceKernelGetModel") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGetModel(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.SysMemUserForUserModule.sceKernelSetPTRIG(processor);";
		}
	};
}