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

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import org.apache.log4j.Logger;

public class scePspNpDrm_user implements HLEModule {

    protected static Logger log = Modules.getLogger("scePspNpDrm_user");

    @Override
    public String getName() {
        return "scePspNpDrm_user";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0xA1336091, sceNpDrmSetLicenseeKeyFunction);
            mm.addFunction(0x9B745542, sceNpDrmClearLicenseeKeyFunction);
            mm.addFunction(0x275987D1, sceNpDrmRenameCheckFunction);
            mm.addFunction(0x08D98894, sceNpDrmEdataSetupKeyFunction);
            mm.addFunction(0x219EF5CC, sceNpDrmEdataGetDataSizeFunction);
            mm.addFunction(0x2BAA4294, sceNpDrmOpenFunction);
            mm.addFunction(0xC618D0B1, sceKernelLoadModuleNpDrmFunction);
            mm.addFunction(0xAA5FC85B, sceKernelLoadExecNpDrmFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceNpDrmSetLicenseeKeyFunction);
            mm.removeFunction(sceNpDrmClearLicenseeKeyFunction);
            mm.removeFunction(sceNpDrmRenameCheckFunction);
            mm.removeFunction(sceNpDrmEdataSetupKeyFunction);
            mm.removeFunction(sceNpDrmEdataGetDataSizeFunction);
            mm.removeFunction(sceNpDrmOpenFunction);
            mm.removeFunction(sceKernelLoadModuleNpDrmFunction);
            mm.removeFunction(sceKernelLoadExecNpDrmFunction);

        }
    }

    public static final int PSP_NPDRM_KEY_LENGHT = 16;
    private int npDrmKey[] = new int[PSP_NPDRM_KEY_LENGHT];

    public void sceNpDrmSetLicenseeKey(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int npDrmKeyAddr = cpu.gpr[4];

        log.warn("PARTIAL: sceNpDrmSetLicenseeKey (npDrmKeyAddr=0x" + Integer.toHexString(npDrmKeyAddr) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if(Memory.isAddressGood(npDrmKeyAddr)) {
            String key = "";
            for(int i = 0; i < PSP_NPDRM_KEY_LENGHT; i++) {
                npDrmKey[i] = mem.read8(npDrmKeyAddr + i);
                key += Integer.toHexString(npDrmKey[i]);
            }
            log.info("NPDRM Encryption key detected: 0x" + key);
        }
        cpu.gpr[2] = 0;
    }

    public void sceNpDrmClearLicenseeKey(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("PARTIAL: sceNpDrmClearLicenseeKey");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        for(int i = 0; i < PSP_NPDRM_KEY_LENGHT; i++) {
            npDrmKey[i] = 0;
        }
        cpu.gpr[2] = 0;
    }

    public void sceNpDrmRenameCheck(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpDrmRenameCheck");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpDrmEdataSetupKey(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpDrmEdataSetupKey");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpDrmEdataGetDataSize(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpDrmEdataGetDataSize");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNpDrmOpen(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpDrmOpen");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceKernelLoadModuleNpDrm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceKernelLoadModuleNpDrm");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceKernelLoadExecNpDrm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceKernelLoadExecNpDrm");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public final HLEModuleFunction sceNpDrmSetLicenseeKeyFunction = new HLEModuleFunction("scePspNpDrm_user", "sceNpDrmSetLicenseeKey") {

        @Override
        public final void execute(Processor processor) {
            sceNpDrmSetLicenseeKey(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePspNpDrm_userModule.sceNpDrmSetLicenseeKey(processor);";
        }
    };

    public final HLEModuleFunction sceNpDrmClearLicenseeKeyFunction = new HLEModuleFunction("scePspNpDrm_user", "sceNpDrmClearLicenseeKey") {

        @Override
        public final void execute(Processor processor) {
            sceNpDrmClearLicenseeKey(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePspNpDrm_userModule.sceNpDrmClearLicenseeKey(processor);";
        }
    };

    public final HLEModuleFunction sceNpDrmRenameCheckFunction = new HLEModuleFunction("scePspNpDrm_user", "sceNpDrmRenameCheck") {

        @Override
        public final void execute(Processor processor) {
            sceNpDrmRenameCheck(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePspNpDrm_userModule.sceNpDrmRenameCheck(processor);";
        }
    };

    public final HLEModuleFunction sceNpDrmEdataSetupKeyFunction = new HLEModuleFunction("scePspNpDrm_user", "sceNpDrmEdataSetupKey") {

        @Override
        public final void execute(Processor processor) {
            sceNpDrmEdataSetupKey(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePspNpDrm_userModule.sceNpDrmEdataSetupKey(processor);";
        }
    };

    public final HLEModuleFunction sceNpDrmEdataGetDataSizeFunction = new HLEModuleFunction("scePspNpDrm_user", "sceNpDrmEdataGetDataSize") {

        @Override
        public final void execute(Processor processor) {
            sceNpDrmEdataGetDataSize(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePspNpDrm_userModule.sceNpDrmEdataGetDataSize(processor);";
        }
    };

    public final HLEModuleFunction sceNpDrmOpenFunction = new HLEModuleFunction("scePspNpDrm_user", "sceNpDrmOpen") {

        @Override
        public final void execute(Processor processor) {
            sceNpDrmOpen(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePspNpDrm_userModule.sceNpDrmOpen(processor);";
        }
    };

    public final HLEModuleFunction sceKernelLoadModuleNpDrmFunction = new HLEModuleFunction("scePspNpDrm_user", "sceKernelLoadModuleNpDrm") {

        @Override
        public final void execute(Processor processor) {
            sceKernelLoadModuleNpDrm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePspNpDrm_userModule.sceKernelLoadModuleNpDrm(processor);";
        }
    };

    public final HLEModuleFunction sceKernelLoadExecNpDrmFunction = new HLEModuleFunction("scePspNpDrm_user", "sceKernelLoadExecNpDrm") {

        @Override
        public final void execute(Processor processor) {
            sceKernelLoadExecNpDrm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.scePspNpDrm_userModule.sceKernelLoadExecNpDrm(processor);";
        }
    };
}