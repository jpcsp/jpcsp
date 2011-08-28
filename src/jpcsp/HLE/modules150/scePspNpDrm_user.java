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
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

public class scePspNpDrm_user extends HLEModule {

    protected static Logger log = Modules.getLogger("scePspNpDrm_user");

    @Override
    public String getName() {
        return "scePspNpDrm_user";
    }

    public static final int PSP_NPDRM_KEY_LENGHT = 16;
    private int npDrmKey[] = new int[PSP_NPDRM_KEY_LENGHT];

    @HLEFunction(nid = 0xA1336091, version = 150)
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

    @HLEFunction(nid = 0x9B745542, version = 150)
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

    @HLEFunction(nid = 0x275987D1, version = 150)
    public void sceNpDrmRenameCheck(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpDrmRenameCheck");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x08D98894, version = 150)
    public void sceNpDrmEdataSetupKey(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpDrmEdataSetupKey");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x219EF5CC, version = 150)
    public void sceNpDrmEdataGetDataSize(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpDrmEdataGetDataSize");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x2BAA4294, version = 150)
    public void sceNpDrmOpen(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNpDrmOpen");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xC618D0B1, version = 150)
    public void sceKernelLoadModuleNpDrm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceKernelLoadModuleNpDrm");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xAA5FC85B, version = 150)
    public void sceKernelLoadExecNpDrm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceKernelLoadExecNpDrm");

        cpu.gpr[2] = 0xDEADC0DE;
    }

}