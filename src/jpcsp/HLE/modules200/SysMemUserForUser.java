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
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class SysMemUserForUser extends jpcsp.HLE.modules150.SysMemUserForUser {
    protected int compiledSdkVersion;
    protected int compilerVersion;

	@Override
	public void installModule(HLEModuleManager mm, int version) {
		super.installModule(mm, version);

		if (version >= 200) {

			mm.addFunction(0xFC114573, sceKernelGetCompiledSdkVersionFunction);
			mm.addFunction(0x7591C7DB, sceKernelSetCompiledSdkVersionFunction);
			mm.addFunction(0xF77D77CB, sceKernelSetCompilerVersionFunction);
			mm.addFunction(0xA6848DF8, SysMemUserForUser_A6848DF8Function);

		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		super.uninstallModule(mm, version);

		if (version >= 200) {

			mm.removeFunction(sceKernelGetCompiledSdkVersionFunction);
			mm.removeFunction(sceKernelSetCompiledSdkVersionFunction);
			mm.removeFunction(sceKernelSetCompilerVersionFunction);
			mm.removeFunction(SysMemUserForUser_A6848DF8Function);

		}
	}

	@Override
	public void start() {
		compiledSdkVersion = 0;
		super.start();
	}

	public int hleGetCompiledSdkVersion() {
		return compiledSdkVersion;
	}

	public void sceKernelGetCompiledSdkVersion(Processor processor) {
		CpuState cpu = processor.cpu;

		if(log.isDebugEnabled()) {
            log.debug("sceKernelGetCompiledSdkVersion");
        }

		cpu.gpr[2] = compiledSdkVersion;
	}

	public void sceKernelSetCompiledSdkVersion(Processor processor) {
		CpuState cpu = processor.cpu;

        int sdkVersion = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceKernelSetCompiledSdkVersion: sdkVersion=" + Integer.toHexString(sdkVersion));
        }

        compiledSdkVersion = sdkVersion;
		cpu.gpr[2] = 0;
	}

	public void sceKernelSetCompilerVersion(Processor processor) {
		CpuState cpu = processor.cpu;

		int compVersion = cpu.gpr[4];

        if(log.isDebugEnabled()) {
            log.debug("sceKernelSetCompilerVersion: compVersion=" + Integer.toHexString(compVersion));
        }

        compilerVersion = compVersion;
		cpu.gpr[2] = 0;
	}

	public void SysMemUserForUser_A6848DF8(Processor processor) {
		CpuState cpu = processor.cpu;

		log.debug("Unimplemented NID function SysMemUserForUser_A6848DF8 [0xA6848DF8]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public final HLEModuleFunction sceKernelGetCompiledSdkVersionFunction = new HLEModuleFunction("SysMemUserForUser", "sceKernelGetCompiledSdkVersion") {
		@Override
		public final void execute(Processor processor) {
			sceKernelGetCompiledSdkVersion(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.SysMemUserForUserModule.sceKernelGetCompiledSdkVersion(processor);";
		}
	};

	public final HLEModuleFunction sceKernelSetCompiledSdkVersionFunction = new HLEModuleFunction("SysMemUserForUser", "sceKernelSetCompiledSdkVersion") {
		@Override
		public final void execute(Processor processor) {
			sceKernelSetCompiledSdkVersion(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.SysMemUserForUserModule.sceKernelSetCompiledSdkVersion(processor);";
		}
	};

	public final HLEModuleFunction sceKernelSetCompilerVersionFunction = new HLEModuleFunction("SysMemUserForUser", "sceKernelSetCompilerVersion") {
		@Override
		public final void execute(Processor processor) {
			sceKernelSetCompilerVersion(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.SysMemUserForUserModule.sceKernelSetCompilerVersion(processor);";
		}
	};

	public final HLEModuleFunction SysMemUserForUser_A6848DF8Function = new HLEModuleFunction("SysMemUserForUser", "SysMemUserForUser_A6848DF8") {
		@Override
		public final void execute(Processor processor) {
			SysMemUserForUser_A6848DF8(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.SysMemUserForUserModule.SysMemUserForUser_A6848DF8(processor);";
		}
	};
}