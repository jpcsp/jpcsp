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

package jpcsp.HLE.modules200;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceDisplay extends jpcsp.HLE.modules150.sceDisplay {
	private static final long serialVersionUID = 7951510954219695582L;

	@Override
	public String getName() { return "sceDisplay"; }
	
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		super.installModule(mm, version);
		
		if (version >= 200) {
		
			mm.addFunction(0xBF79F646, sceDisplayGetResumeModeFunction);
			mm.addFunction(0x69B53541, sceDisplayGetVblankRestFunction);
			mm.addFunction(0x21038913, sceDisplayIsVsyncFunction);
			
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		super.uninstallModule(mm, version);
		
		if (version >= 200) {
		
			mm.removeFunction(sceDisplayGetResumeModeFunction);
			mm.removeFunction(sceDisplayGetVblankRestFunction);
			mm.removeFunction(sceDisplayIsVsyncFunction);
			
		}
	}
	
	public void sceDisplayGetResumeMode(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceDisplayGetResumeMode [0xBF79F646]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceDisplayGetVblankRest(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceDisplayGetVblankRest [0x69B53541]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceDisplayIsVsync(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceDisplayIsVsync [0x21038913]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public final HLEModuleFunction sceDisplayGetResumeModeFunction = new HLEModuleFunction("sceDisplay", "sceDisplayGetResumeMode") {
		@Override
		public final void execute(Processor processor) {
			sceDisplayGetResumeMode(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayGetResumeMode(processor);";
		}
	};
    
	public final HLEModuleFunction sceDisplayGetVblankRestFunction = new HLEModuleFunction("sceDisplay", "sceDisplayGetVblankRest") {
		@Override
		public final void execute(Processor processor) {
			sceDisplayGetVblankRest(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayGetVblankRest(processor);";
		}
	};
    
	public final HLEModuleFunction sceDisplayIsVsyncFunction = new HLEModuleFunction("sceDisplay", "sceDisplayIsVsync") {
		@Override
		public final void execute(Processor processor) {
			sceDisplayIsVsync(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayIsVsync(processor);";
		}
	};
}