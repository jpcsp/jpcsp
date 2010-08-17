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

import jpcsp.Allegrex.CpuState;
import jpcsp.Processor;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class Kernel_Library extends jpcsp.HLE.modules150.Kernel_Library {
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		super.installModule(mm, version);

		if (version >= 380) {

			mm.addFunction(0x15B6446B, sceKernelUnlockLwMutexFunction);
			mm.addFunction(0x1FC64E09, sceKernelLockLwMutexCBFunction);
			mm.addFunction(0xBEA46419, sceKernelLockLwMutexFunction);
			mm.addFunction(0xC1734599, sceKernelReferLwMutexStatusFunction);
			mm.addFunction(0xDC692EE3, sceKernelTryLockLwMutexFunction);
            mm.addFunction(0x37431849, Kernel_Library_37431849Function);
            mm.addFunction(0x1839852A, sceKernelMemcpyFunction);

		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		super.uninstallModule(mm, version);

		if (version >= 380) {

			mm.removeFunction(sceKernelUnlockLwMutexFunction);
			mm.removeFunction(sceKernelLockLwMutexCBFunction);
			mm.removeFunction(sceKernelLockLwMutexFunction);
			mm.removeFunction(sceKernelReferLwMutexStatusFunction);
			mm.removeFunction(sceKernelTryLockLwMutexFunction);
            mm.removeFunction(Kernel_Library_37431849Function);
            mm.removeFunction(sceKernelMemcpyFunction);

		}
	}

	public void sceKernelUnlockLwMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelUnlockLwMutex(gpr[4], gpr[5]);
	}

	public void sceKernelLockLwMutexCB(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelLockLwMutexCB(gpr[4], gpr[5], gpr[6]);
	}

	public void sceKernelLockLwMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelLockLwMutex(gpr[4], gpr[5], gpr[6]);
	}

	public void sceKernelReferLwMutexStatus(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelReferLwMutexStatus(gpr[4], gpr[5]);
	}

	public void sceKernelTryLockLwMutex(Processor processor) {
		int[] gpr = processor.cpu.gpr;
		Managers.mutex.sceKernelTryLockLwMutex(gpr[4], gpr[5]);
	}

    public void Kernel_Library_37431849(Processor processor) {
		CpuState cpu = processor.cpu;

        int unk1 = cpu.gpr[4];  // Address to a mutex lock count.
        int unk2 = cpu.gpr[5];  // Integer value 1.

        log.warn("IGNORING: Kernel_Library_37431849 unk1=0x" + Integer.toHexString(unk1) +
                ", unk2=" + unk2);

        cpu.gpr[2] = 0x800201cb;  // Unknown meaning.
	}

    public void sceKernelMemcpy(Processor processor) {
		CpuState cpu = processor.cpu;

		int dst = cpu.gpr[4];
		int src = cpu.gpr[5];
		int length = cpu.gpr[6];

		Processor.memory.memcpy(dst, src, length);

		cpu.gpr[2] = dst;
	}

	public final HLEModuleFunction sceKernelUnlockLwMutexFunction = new HLEModuleFunction("Kernel_Library", "sceKernelUnlockLwMutex") {
		@Override
		public final void execute(Processor processor) {
			sceKernelUnlockLwMutex(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.Kernel_LibraryModule.sceKernelUnlockLwMutex(processor);";
		}
	};

	public final HLEModuleFunction sceKernelLockLwMutexCBFunction = new HLEModuleFunction("Kernel_Library", "sceKernelLockLwMutexCB") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLockLwMutexCB(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.Kernel_LibraryModule.sceKernelLockLwMutexCB(processor);";
		}
	};

	public final HLEModuleFunction sceKernelLockLwMutexFunction = new HLEModuleFunction("Kernel_Library", "sceKernelLockLwMutex") {
		@Override
		public final void execute(Processor processor) {
			sceKernelLockLwMutex(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.Kernel_LibraryModule.sceKernelLockLwMutex(processor);";
		}
	};

	public final HLEModuleFunction sceKernelReferLwMutexStatusFunction = new HLEModuleFunction("Kernel_Library", "sceKernelReferLwMutexStatus") {
		@Override
		public final void execute(Processor processor) {
			sceKernelReferLwMutexStatus(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.Kernel_LibraryModule.sceKernelReferLwMutexStatus(processor);";
		}
	};

	public final HLEModuleFunction sceKernelTryLockLwMutexFunction = new HLEModuleFunction("Kernel_Library", "sceKernelTryLockLwMutex") {
		@Override
		public final void execute(Processor processor) {
			sceKernelTryLockLwMutex(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.Kernel_LibraryModule.sceKernelTryLockLwMutex(processor);";
		}
	};

    public final HLEModuleFunction Kernel_Library_37431849Function = new HLEModuleFunction("Kernel_Library", "Kernel_Library_37431849") {
		@Override
		public final void execute(Processor processor) {
			Kernel_Library_37431849(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.Kernel_LibraryModule.Kernel_Library_37431849(processor);";
		}
	};

    public final HLEModuleFunction sceKernelMemcpyFunction = new HLEModuleFunction("Kernel_Library", "sceKernelMemcpy") {
		@Override
		public final void execute(Processor processor) {
			sceKernelMemcpy(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.Kernel_LibraryModule.sceKernelMemcpy(processor);";
		}
	};
}