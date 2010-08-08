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

package jpcsp.HLE.modules500;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceDisplay extends jpcsp.HLE.modules200.sceDisplay {
	private static final long serialVersionUID = 5006833555228054367L;

	@Override
	public String getName() { return "sceDisplay"; }
	
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		super.installModule(mm, version);
		
		if (version >= 500) {
		
			mm.addFunction(0x40F1469C, sceDisplayWaitVblankStartMultiFunction);
			mm.addFunction(0x77ED8B3A, sceDisplay_77ED8B3AFunction);
			
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		super.uninstallModule(mm, version);
		
		if (version >= 500) {
		
			mm.removeFunction(sceDisplayWaitVblankStartMultiFunction);
			mm.removeFunction(sceDisplay_77ED8B3AFunction);
			
		}
	}
	
	
	public void sceDisplayWaitVblankStartMulti(Processor processor) {
		CpuState cpu = processor.cpu;

		// Same as sceDisplayWaitVblankStart().
        // May be different on the PSP, because it suggests multiple graphics'
        // chips interaction (GE and ME, probably).
        if (log.isDebugEnabled()) {
        	log.debug("sceDisplayWaitVblankStartMulti");
        }

        cpu.gpr[2] = 0;

        blockCurrentThreadOnVblank(false);
	}
    
	public void sceDisplay_77ED8B3A(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceDisplay_77ED8B3A [0x77ED8B3A]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public final HLEModuleFunction sceDisplayWaitVblankStartMultiFunction = new HLEModuleFunction("sceDisplay", "sceDisplayWaitVblankStartMulti") {
		@Override
		public final void execute(Processor processor) {
			sceDisplayWaitVblankStartMulti(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayWaitVblankStartMulti(processor);";
		}
	};
    
	public final HLEModuleFunction sceDisplay_77ED8B3AFunction = new HLEModuleFunction("sceDisplay", "sceDisplay_77ED8B3A") {
		@Override
		public final void execute(Processor processor) {
			sceDisplay_77ED8B3A(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplay_77ED8B3A(processor);";
		}
	};
}