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

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;

import org.apache.log4j.Logger;

public class sceMp4 implements HLEModule, HLEStartModule {

    protected static Logger log = Modules.getLogger("sceMp4");

    @Override
    public String getName() {
        return "sceMp4";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x68651CBC, sceMp4InitFunction);
            mm.addFunction(0x9042B257, sceMp4FinishFunction);
            mm.addFunction(0xB1221EE7, sceMp4CreateFunction);
            mm.addFunction(0x538C2057, sceMp4DeleteFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceMp4InitFunction);
            mm.removeFunction(sceMp4FinishFunction);
            mm.removeFunction(sceMp4CreateFunction);
            mm.removeFunction(sceMp4DeleteFunction);

        }
    }

    @Override
	public void start() {
	}

	@Override
	public void stop() {
	}

    public void sceMp4Init(Processor processor) {
        CpuState cpu = processor.cpu;

        int unk1 = cpu.gpr[4]; // Values: 0 or 1
        int unk2 = cpu.gpr[5]; // Values: 0 or 1

        log.warn("PARTIAL: sceMp4Init (unk1=0x" + Integer.toHexString(unk1)
                + ", unk2=0x" + Integer.toHexString(unk2) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceMp4Finish(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("PARTIAL: sceMp4Finish");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceMp4Create(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceMp4Create");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceMp4Delete(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceMp4Delete");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public final HLEModuleFunction sceMp4InitFunction = new HLEModuleFunction("sceMp4", "sceMp4Init") {

        @Override
        public final void execute(Processor processor) {
            sceMp4Init(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp4Module.sceMp4Init(processor);";
        }
    };

    public final HLEModuleFunction sceMp4FinishFunction = new HLEModuleFunction("sceMp4", "sceMp4Finish") {

        @Override
        public final void execute(Processor processor) {
            sceMp4Finish(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp4Module.sceMp4Finish(processor);";
        }
    };

    public final HLEModuleFunction sceMp4CreateFunction = new HLEModuleFunction("sceMp4", "sceMp4Create") {

        @Override
        public final void execute(Processor processor) {
            sceMp4Create(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp4Module.sceMp4Create(processor);";
        }
    };

    public final HLEModuleFunction sceMp4DeleteFunction = new HLEModuleFunction("sceMp4", "sceMp4Delete") {

        @Override
        public final void execute(Processor processor) {
            sceMp4Delete(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceMp4Module.sceMp4Delete(processor);";
        }
    };
}