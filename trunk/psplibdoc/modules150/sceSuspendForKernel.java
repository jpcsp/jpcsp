/* This autogenerated file is part of jpcsp. */
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

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import jpcsp.Memory;
import jpcsp.Processor;

import jpcsp.Allegrex.CpuState; // New-Style Processor

public class sceSuspendForKernel implements HLEModule {
	@Override
	public String getName() { return "sceSuspendForKernel"; }
	
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {
		
			mm.addFunction(sceKernelRegisterPowerHandlersFunction, 0xBDE686CD);
			mm.addFunction(sceKernelPowerLockFunction, 0xEADB1BD7);
			mm.addFunction(sceKernelPowerLockForUserFunction, 0xB53B2147);
			mm.addFunction(sceKernelPowerUnlockFunction, 0x3AEE7261);
			mm.addFunction(sceKernelPowerUnlockForUserFunction, 0xC7C928C7);
			mm.addFunction(sceKernelPowerTickFunction, 0x090CCB3F);
			mm.addFunction(sceKernelVolatileMemLockFunction, 0x3E0271D3);
			mm.addFunction(sceKernelVolatileMemTryLockFunction, 0xA14F40B2);
			mm.addFunction(sceKernelVolatileMemUnlockFunction, 0xA569E425);
			mm.addFunction(sceKernelPowerRebootStartFunction, 0x98A1D061);
			mm.addFunction(sceKernelRegisterSuspendHandlerFunction, 0x91A77137);
			mm.addFunction(sceKernelRegisterResumeHandlerFunction, 0xB43D1A8C);
			mm.addFunction(sceSuspendForKernel_67B59042Function, 0x67B59042);
			mm.addFunction(sceSuspendForKernel_B2C9640BFunction, 0xB2C9640B);
			mm.addFunction(sceKernelDispatchSuspendHandlersFunction, 0x8F58B1EC);
			mm.addFunction(sceKernelDispatchResumeHandlersFunction, 0x0AB0C6F3);
			
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {
		
			mm.removeFunction(sceKernelRegisterPowerHandlersFunction);
			mm.removeFunction(sceKernelPowerLockFunction);
			mm.removeFunction(sceKernelPowerLockForUserFunction);
			mm.removeFunction(sceKernelPowerUnlockFunction);
			mm.removeFunction(sceKernelPowerUnlockForUserFunction);
			mm.removeFunction(sceKernelPowerTickFunction);
			mm.removeFunction(sceKernelVolatileMemLockFunction);
			mm.removeFunction(sceKernelVolatileMemTryLockFunction);
			mm.removeFunction(sceKernelVolatileMemUnlockFunction);
			mm.removeFunction(sceKernelPowerRebootStartFunction);
			mm.removeFunction(sceKernelRegisterSuspendHandlerFunction);
			mm.removeFunction(sceKernelRegisterResumeHandlerFunction);
			mm.removeFunction(sceSuspendForKernel_67B59042Function);
			mm.removeFunction(sceSuspendForKernel_B2C9640BFunction);
			mm.removeFunction(sceKernelDispatchSuspendHandlersFunction);
			mm.removeFunction(sceKernelDispatchResumeHandlersFunction);
			
		}
	}
	
	
	public void sceKernelRegisterPowerHandlers(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelRegisterPowerHandlers [0xBDE686CD]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelPowerLock(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelPowerLock [0xEADB1BD7]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelPowerLockForUser(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelPowerLockForUser [0xB53B2147]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelPowerUnlock(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelPowerUnlock [0x3AEE7261]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelPowerUnlockForUser(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelPowerUnlockForUser [0xC7C928C7]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelPowerTick(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelPowerTick [0x090CCB3F]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelVolatileMemLock(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelVolatileMemLock [0x3E0271D3]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelVolatileMemTryLock(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelVolatileMemTryLock [0xA14F40B2]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelVolatileMemUnlock(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelVolatileMemUnlock [0xA569E425]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelPowerRebootStart(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelPowerRebootStart [0x98A1D061]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelRegisterSuspendHandler(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelRegisterSuspendHandler [0x91A77137]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelRegisterResumeHandler(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelRegisterResumeHandler [0xB43D1A8C]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceSuspendForKernel_67B59042(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceSuspendForKernel_67B59042 [0x67B59042]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceSuspendForKernel_B2C9640B(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceSuspendForKernel_B2C9640B [0xB2C9640B]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelDispatchSuspendHandlers(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelDispatchSuspendHandlers [0x8F58B1EC]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void sceKernelDispatchResumeHandlers(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelDispatchResumeHandlers [0x0AB0C6F3]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public final HLEModuleFunction sceKernelRegisterPowerHandlersFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelRegisterPowerHandlers") {
		@Override
		public final void execute(Processor processor) {
			sceKernelRegisterPowerHandlers(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelRegisterPowerHandlers(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelPowerLockFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelPowerLock") {
		@Override
		public final void execute(Processor processor) {
			sceKernelPowerLock(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelPowerLock(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelPowerLockForUserFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelPowerLockForUser") {
		@Override
		public final void execute(Processor processor) {
			sceKernelPowerLockForUser(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelPowerLockForUser(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelPowerUnlockFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelPowerUnlock") {
		@Override
		public final void execute(Processor processor) {
			sceKernelPowerUnlock(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelPowerUnlock(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelPowerUnlockForUserFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelPowerUnlockForUser") {
		@Override
		public final void execute(Processor processor) {
			sceKernelPowerUnlockForUser(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelPowerUnlockForUser(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelPowerTickFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelPowerTick") {
		@Override
		public final void execute(Processor processor) {
			sceKernelPowerTick(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelPowerTick(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelVolatileMemLockFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelVolatileMemLock") {
		@Override
		public final void execute(Processor processor) {
			sceKernelVolatileMemLock(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelVolatileMemLock(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelVolatileMemTryLockFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelVolatileMemTryLock") {
		@Override
		public final void execute(Processor processor) {
			sceKernelVolatileMemTryLock(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelVolatileMemTryLock(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelVolatileMemUnlockFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelVolatileMemUnlock") {
		@Override
		public final void execute(Processor processor) {
			sceKernelVolatileMemUnlock(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelVolatileMemUnlock(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelPowerRebootStartFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelPowerRebootStart") {
		@Override
		public final void execute(Processor processor) {
			sceKernelPowerRebootStart(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelPowerRebootStart(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelRegisterSuspendHandlerFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelRegisterSuspendHandler") {
		@Override
		public final void execute(Processor processor) {
			sceKernelRegisterSuspendHandler(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelRegisterSuspendHandler(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelRegisterResumeHandlerFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelRegisterResumeHandler") {
		@Override
		public final void execute(Processor processor) {
			sceKernelRegisterResumeHandler(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelRegisterResumeHandler(processor);";
		}
	};
    
	public final HLEModuleFunction sceSuspendForKernel_67B59042Function = new HLEModuleFunction("sceSuspendForKernel", "sceSuspendForKernel_67B59042") {
		@Override
		public final void execute(Processor processor) {
			sceSuspendForKernel_67B59042(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceSuspendForKernel_67B59042(processor);";
		}
	};
    
	public final HLEModuleFunction sceSuspendForKernel_B2C9640BFunction = new HLEModuleFunction("sceSuspendForKernel", "sceSuspendForKernel_B2C9640B") {
		@Override
		public final void execute(Processor processor) {
			sceSuspendForKernel_B2C9640B(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceSuspendForKernel_B2C9640B(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelDispatchSuspendHandlersFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelDispatchSuspendHandlers") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDispatchSuspendHandlers(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelDispatchSuspendHandlers(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelDispatchResumeHandlersFunction = new HLEModuleFunction("sceSuspendForKernel", "sceKernelDispatchResumeHandlers") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDispatchResumeHandlers(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceSuspendForKernelModule.sceKernelDispatchResumeHandlers(processor);";
		}
	};
    
};