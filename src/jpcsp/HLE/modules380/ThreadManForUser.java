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

package jpcsp.HLE.modules380;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class ThreadManForUser extends jpcsp.HLE.modules271.ThreadManForUser {
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		super.installModule(mm, version);
		
		if (version >= 380) {
		
			mm.addFunction(sceKernelCreateLwMutexFunction, 0x19CFF145);
			mm.addFunction(sceKernelDonateWakeupThreadFunction, 0x1AF94D03);
			mm.addFunction(ThreadManForUser_31327F19Function, 0x31327F19);
			mm.addFunction(sceKernelReferLwMutexStatusByIDFunction, 0x4C145944);
			mm.addFunction(sceKernelDeleteLwMutexFunction, 0x60107536);
			mm.addFunction(ThreadManForUser_71040D5CFunction, 0x71040D5C);
			mm.addFunction(ThreadManForUser_7CFF8CF3Function, 0x7CFF8CF3);
			mm.addFunction(ThreadManForUser_BEED3A47Function, 0xBEED3A47);
			
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		super.uninstallModule(mm, version);
		
		if (version >= 380) {
		
			mm.removeFunction(sceKernelCreateLwMutexFunction);
			mm.removeFunction(sceKernelDonateWakeupThreadFunction);
			mm.removeFunction(ThreadManForUser_31327F19Function);
			mm.removeFunction(sceKernelReferLwMutexStatusByIDFunction);
			mm.removeFunction(sceKernelDeleteLwMutexFunction);
			mm.removeFunction(ThreadManForUser_71040D5CFunction);
			mm.removeFunction(ThreadManForUser_7CFF8CF3Function);
			mm.removeFunction(ThreadManForUser_BEED3A47Function);
			
		}
	}
	
	public void sceKernelCreateLwMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelCreateLwMutex(gpr[4], gpr[5], gpr[6], gpr[7], gpr[8]);
	}
    
	public void sceKernelDonateWakeupThread(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function sceKernelDonateWakeupThread [0x1AF94D03]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
        
	public void ThreadManForUser_31327F19(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function ThreadManForUser_31327F19 [0x31327F19]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
        
	public void sceKernelReferLwMutexStatusByID(Processor processor) {
		Managers.mutex.sceKernelReferLwMutexStatusByID();
	}
        
	public void sceKernelDeleteLwMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelDeleteLwMutex(gpr[4]);
	}
        
	public void ThreadManForUser_71040D5C(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function ThreadManForUser_71040D5C [0x71040D5C]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public void ThreadManForUser_7CFF8CF3(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function ThreadManForUser_7CFF8CF3 [0x7CFF8CF3]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
        
	public void ThreadManForUser_BEED3A47(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function ThreadManForUser_BEED3A47 [0xBEED3A47]");

		cpu.gpr[2] = 0xDEADC0DE;
	}
    
	public final HLEModuleFunction sceKernelCreateLwMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateLwMutex") {
		@Override
		public final void execute(Processor processor) {
			sceKernelCreateLwMutex(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCreateLwMutex(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelDonateWakeupThreadFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDonateWakeupThread") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDonateWakeupThread(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDonateWakeupThread(processor);";
		}
	};
        
	public final HLEModuleFunction ThreadManForUser_31327F19Function = new HLEModuleFunction("ThreadManForUser", "ThreadManForUser_31327F19") {
		@Override
		public final void execute(Processor processor) {
			ThreadManForUser_31327F19(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.ThreadManForUser_31327F19(processor);";
		}
	};
        
	public final HLEModuleFunction sceKernelReferLwMutexStatusByIDFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferLwMutexStatusByID") {
		@Override
		public final void execute(Processor processor) {
			sceKernelReferLwMutexStatusByID(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferLwMutexStatusByID(processor);";
		}
	};
        
	public final HLEModuleFunction sceKernelDeleteLwMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteLwMutex") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDeleteLwMutex(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDeleteLwMutex(processor);";
		}
	};
        
	public final HLEModuleFunction ThreadManForUser_71040D5CFunction = new HLEModuleFunction("ThreadManForUser", "ThreadManForUser_71040D5C") {
		@Override
		public final void execute(Processor processor) {
			ThreadManForUser_71040D5C(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.ThreadManForUser_71040D5C(processor);";
		}
	};
        
	public final HLEModuleFunction ThreadManForUser_7CFF8CF3Function = new HLEModuleFunction("ThreadManForUser", "ThreadManForUser_7CFF8CF3") {
		@Override
		public final void execute(Processor processor) {
			ThreadManForUser_7CFF8CF3(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.ThreadManForUser_7CFF8CF3(processor);";
		}
	};
        
	public final HLEModuleFunction ThreadManForUser_BEED3A47Function = new HLEModuleFunction("ThreadManForUser", "ThreadManForUser_BEED3A47") {
		@Override
		public final void execute(Processor processor) {
			ThreadManForUser_BEED3A47(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.ThreadManForUser_BEED3A47(processor);";
		}
	};
};