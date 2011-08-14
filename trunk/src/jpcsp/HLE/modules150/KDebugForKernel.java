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
import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import jpcsp.Processor;

import jpcsp.Allegrex.CpuState;

public class KDebugForKernel implements HLEModule {
    protected static Logger kprintf = Logger.getLogger("kprintf");

    @Override
	public String getName() { return "KDebugForKernel"; }
	
	@Override
	public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }
	
	
	@HLEFunction(nid = 0xE7A3874D, version = 150)
	public void sceKernelRegisterAssertHandler(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelRegisterAssertHandler [0xE7A3874D]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0x2FF4E9F9, version = 150)
	public void sceKernelAssert(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelAssert [0x2FF4E9F9]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0x9B868276, version = 150)
	public void sceKernelGetDebugPutchar(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelGetDebugPutchar [0x9B868276]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0xE146606D, version = 150)
	public void sceKernelRegisterDebugPutchar(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelRegisterDebugPutchar [0xE146606D]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0x7CEB2C09, version = 150)
	public void sceKernelRegisterKprintfHandler(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelRegisterKprintfHandler [0x7CEB2C09]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0x84F370BC, version = 150)
	public void Kprintf(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.SysMemUserForUserModule.hleKernelPrintf(cpu, kprintf, "Kprintf");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0x5CE9838B, version = 150)
	public void sceKernelDebugWrite(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelDebugWrite [0x5CE9838B]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0x66253C4E, version = 150)
	public void sceKernelRegisterDebugWrite(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelRegisterDebugWrite [0x66253C4E]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0xDBB5597F, version = 150)
	public void sceKernelDebugRead(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelDebugRead [0xDBB5597F]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0xE6554FDA, version = 150)
	public void sceKernelRegisterDebugRead(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelRegisterDebugRead [0xE6554FDA]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0xB9C643C9, version = 150)
	public void sceKernelDebugEcho(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelDebugEcho [0xB9C643C9]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0x7D1C74F0, version = 150)
	public void sceKernelDebugEchoSet(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelDebugEchoSet [0x7D1C74F0]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0x24C32559, version = 150)
	public void sceKernelDipsw(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelDipsw [0x24C32559]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0xD636B827, version = 150)
	public void sceKernelDipswAll(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelDipswAll [0xD636B827]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0x5282DD5E, version = 150)
	public void sceKernelDipswSet(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelDipswSet [0x5282DD5E]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0xEE75658D, version = 150)
	public void sceKernelDipswClear(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function sceKernelDipswClear [0xEE75658D]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0x9F8703E4, version = 150)
	public void KDebugForKernel_9F8703E4(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function KDebugForKernel_9F8703E4 [0x9F8703E4]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0x333DCEC7, version = 150)
	public void KDebugForKernel_333DCEC7(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function KDebugForKernel_333DCEC7 [0x333DCEC7]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0xE892D9A1, version = 150)
	public void KDebugForKernel_E892D9A1(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function KDebugForKernel_E892D9A1 [0xE892D9A1]");

		cpu.gpr[2] = 0;
	}
    
	@HLEFunction(nid = 0xA126F497, version = 150)
	public void KDebugForKernel_A126F497(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function KDebugForKernel_A126F497 [0xA126F497]");

		cpu.gpr[2] = 0;
	}

	@HLEFunction(nid = 0xB7251823, version = 150)
	public void KDebugForKernel_B7251823(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn("Unimplemented NID function KDebugForKernel_B7251823 [0xB7251823]");

		cpu.gpr[2] = 0;
	}

};
