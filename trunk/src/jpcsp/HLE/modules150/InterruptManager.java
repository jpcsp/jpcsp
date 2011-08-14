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
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import org.apache.log4j.Logger;

public class InterruptManager implements HLEModule {
    private static Logger log = Modules.getLogger("InterruptManager");

	@Override
	public String getName() { return "InterruptManager"; }
	
	@Override
	public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }
	
	@HLEFunction(nid = 0xCA04A2B9, version = 150)
	public void sceKernelRegisterSubIntrHandler(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		gpr[2] = Managers.intr.sceKernelRegisterSubIntrHandler(gpr[4], gpr[5], gpr[6], gpr[7]);
	}
    
	@HLEFunction(nid = 0xD61E6961, version = 150)
	public void sceKernelReleaseSubIntrHandler(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		gpr[2] = Managers.intr.sceKernelReleaseSubIntrHandler(gpr[4], gpr[5]);
	}
    
	@HLEFunction(nid = 0xFB8E22EC, version = 150)
	public void sceKernelEnableSubIntr(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		gpr[2] = Managers.intr.sceKernelEnableSubIntr(gpr[4], gpr[5]);
	}
    
	@HLEFunction(nid = 0x8A389411, version = 150)
	public void sceKernelDisableSubIntr(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		gpr[2] = Managers.intr.sceKernelDisableSubIntr(gpr[4], gpr[5]);
	}
    
	@HLEFunction(nid = 0x5CB5A78B, version = 150)
	public void sceKernelSuspendSubIntr(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceKernelSuspendSubIntr [0x5CB5A78B]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0x7860E0DC, version = 150)
	public void sceKernelResumeSubIntr(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceKernelResumeSubIntr [0x7860E0DC]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0xFC4374B8, version = 150)
	public void sceKernelIsSubInterruptOccurred(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceKernelIsSubInterruptOccurred [0xFC4374B8]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0xD2E8363F, version = 150)
	public void QueryIntrHandlerInfo(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function QueryIntrHandlerInfo [0xD2E8363F]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	@HLEFunction(nid = 0xEEE43F47, version = 150)
	public void sceKernelRegisterUserSpaceIntrStack(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function sceKernelRegisterUserSpaceIntrStack [0xEEE43F47]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

}