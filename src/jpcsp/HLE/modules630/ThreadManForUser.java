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

package jpcsp.HLE.modules630;

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class ThreadManForUser extends jpcsp.HLE.modules380.ThreadManForUser {
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		super.installModule(mm, version);

		if (version >= 630) {

			mm.addFunction(0xBC80EC7C, sceKernelExtendThreadStackFunction);

		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		super.uninstallModule(mm, version);

		if (version >= 630) {

			mm.removeFunction(sceKernelExtendThreadStackFunction);

		}
	}

	public void sceKernelExtendThreadStack(Processor processor) {
		CpuState cpu = processor.cpu;

        int size = cpu.gpr[4];

        log.warn(String.format("PARTIAL: sceKernelExtendThreadStack size=0x%X", size));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = setThreadCurrentStackSize(size);
	}

	public final HLEModuleFunction sceKernelExtendThreadStackFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelExtendThreadStack") {
		@Override
		public final void execute(Processor processor) {
			sceKernelExtendThreadStack(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.ThreadManForUserModule.sceKernelExtendThreadStack(processor);";
		}
	};
}