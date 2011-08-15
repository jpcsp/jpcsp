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

import jpcsp.HLE.HLEFunction;
import org.lwjgl.LWJGLException;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceDisplay extends jpcsp.HLE.modules150.sceDisplay {
	private static final long serialVersionUID = 7951510954219695582L;

	public sceDisplay() throws LWJGLException {
		super();
	}

	@Override
	public String getName() { return "sceDisplay"; }
	
	@HLEFunction(nid = 0xBF79F646, version = 200)
	public void sceDisplayGetResumeMode(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceDisplayGetResumeMode [0xBF79F646]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x69B53541, version = 200)
	public void sceDisplayGetVblankRest(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceDisplayGetVblankRest [0x69B53541]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x21038913, version = 200)
	public void sceDisplayIsVsync(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceDisplayIsVsync [0x21038913]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

}