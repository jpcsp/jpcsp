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

package jpcsp.HLE.modules271;

import jpcsp.Processor;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class ThreadManForUser extends jpcsp.HLE.modules150.ThreadManForUser {
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		super.installModule(mm, version);
		
		if (version >= 271) {
		
			mm.addFunction(sceKernelTryLockMutexFunction, 0x0DDCD2C9);
			mm.addFunction(sceKernelLockMutexCBFunction, 0x5BF4DD27);
			mm.addFunction(sceKernelUnlockMutexFunction, 0x6B30100F);
			mm.addFunction(sceKernelCancelMutexFunction, 0x87D9223C);
			mm.addFunction(sceKernelReferMutexStatusFunction, 0xA9C2CB9A);
			mm.addFunction(sceKernelLockMutexFunction, 0xB011B11F);
			mm.addFunction(sceKernelCreateMutexFunction, 0xB7D098C6);
			mm.addFunction(sceKernelDeleteMutexFunction, 0xF8170FBE);
			
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		super.uninstallModule(mm, version);
		
		if (version >= 271) {
		
			mm.removeFunction(sceKernelTryLockMutexFunction);
			mm.removeFunction(sceKernelLockMutexCBFunction);
			mm.removeFunction(sceKernelUnlockMutexFunction);
			mm.removeFunction(sceKernelCancelMutexFunction);
			mm.removeFunction(sceKernelReferMutexStatusFunction);
			mm.removeFunction(sceKernelLockMutexFunction);
			mm.removeFunction(sceKernelCreateMutexFunction);
			mm.removeFunction(sceKernelDeleteMutexFunction);
			
		}
	}
	
	public void sceKernelTryLockMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelTryLockMutex(gpr[4], gpr[5]);
	}
        
	public void sceKernelLockMutexCB(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelLockMutexCB(gpr[4], gpr[5], gpr[6]);
	}
    	    
	public void sceKernelUnlockMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelUnlockMutex(gpr[4], gpr[5]);
	}
        
	public void sceKernelCancelMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelCancelMutex(gpr[4]);
	}
    
	public void sceKernelReferMutexStatus(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelReferMutexStatus(gpr[4], gpr[5]);
	}
    
	public void sceKernelLockMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelLockMutex(gpr[4], gpr[5], gpr[6]);
	}
    
	public void sceKernelCreateMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelCreateMutex(gpr[4], gpr[5], gpr[6], gpr[7]);
	}
    
	public void sceKernelDeleteMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelDeleteMutex(gpr[4]);
	}
    
	public final HLEModuleFunction sceKernelTryLockMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelTryLockMutex") {
		@Override
		public final void execute(Processor processor) {
			sceKernelTryLockMutex(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelTryLockMutex(processor);";
		}
	};
        
	public final HLEModuleFunction sceKernelLockMutexCBFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelLockMutexCB") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLockMutexCB(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelLockMutexCB(processor);";
		}
	};
        
	public final HLEModuleFunction sceKernelUnlockMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelUnlockMutex") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUnlockMutex(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelUnlockMutex(processor);";
		}
	};
        
	public final HLEModuleFunction sceKernelCancelMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelMutex") {
		@Override
		public final void execute(Processor processor) {
			sceKernelCancelMutex(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCancelMutex(processor);";
		}
	};
    
	public final HLEModuleFunction sceKernelReferMutexStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferMutexStatus") {
		@Override
		public final void execute(Processor processor) {
			sceKernelReferMutexStatus(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelReferMutexStatus(processor);";
		}
	};
        
	public final HLEModuleFunction sceKernelLockMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelLockMutex") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLockMutex(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelLockMutex(processor);";
		}
	};
        
	public final HLEModuleFunction sceKernelCreateMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateMutex") {
		@Override
		public final void execute(Processor processor) {
			sceKernelCreateMutex(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelCreateMutex(processor);";
		}
	};
        
	public final HLEModuleFunction sceKernelDeleteMutexFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteMutex") {
		@Override
		public final void execute(Processor processor) {
			sceKernelDeleteMutex(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelDeleteMutex(processor);";
		}
	};
};