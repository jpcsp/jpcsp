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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_ARGUMENT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_POWER_VMEM_IN_USE;
import jpcsp.HLE.HLEFunction;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.hardware.Screen;

import org.apache.log4j.Logger;

public class sceSuspendForUser implements HLEModule, HLEStartModule {

    private static Logger log = Modules.getLogger("sceSuspendForUser");

    @Override
    public String getName() {
        return "sceSuspendForUser";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    @Override
    public void start() {
        volatileMemLocked = false;
    }

    @Override
    public void stop() {
    }
    public static final int KERNEL_POWER_TICK_SUSPEND_AND_DISPLAY = 0;
    public static final int KERNEL_POWER_TICK_SUSPEND = 1;
    public static final int KERNEL_POWER_TICK_DISPLAY = 6;
    private boolean volatileMemLocked;

    @HLEFunction(nid = 0xEADB1BD7, version = 150)
    public void sceKernelPowerLock(Processor processor) {
        CpuState cpu = processor.cpu;

        int type = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("IGNORING:sceKernelPowerLock type=" + type);
        }
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x3AEE7261, version = 150)
    public void sceKernelPowerUnlock(Processor processor) {
        CpuState cpu = processor.cpu;

        int type = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("IGNORING:sceKernelPowerUnlock type=" + type);
        }
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x090CCB3F, version = 150)
    public void sceKernelPowerTick(Processor processor) {
        CpuState cpu = processor.cpu;

        int flag = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        switch (flag) {
            case KERNEL_POWER_TICK_SUSPEND_AND_DISPLAY:
            	Screen.hleKernelPowerTick();
                if (log.isTraceEnabled()) {
                    log.trace("IGNORING:sceKernelPowerTick(KERNEL_POWER_TICK_SUSPEND_AND_DISPLAY)");
                }
                break;
            case KERNEL_POWER_TICK_SUSPEND:
                if (log.isTraceEnabled()) {
                    log.trace("IGNORING:sceKernelPowerTick(KERNEL_POWER_TICK_SUSPEND)");
                }
                break;
            case KERNEL_POWER_TICK_DISPLAY:
            	Screen.hleKernelPowerTick();
                if (log.isTraceEnabled()) {
                    log.trace("IGNORING:sceKernelPowerTick(KERNEL_POWER_TICK_DISPLAY)");
                }
                break;
            default:
                log.warn("IGNORING:sceKernelPowerTick(" + flag + ")");
                break;
        }
        cpu.gpr[2] = 0;
    }

    protected void hleKernelVolatileMemLock(Processor processor, boolean trylock) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int type = cpu.gpr[4];
        int paddr = cpu.gpr[5];
        int psize = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("hleKernelVolatileMemLock type=%d, paddr=0x%08X, psize=0x%08X, trylock=%b", type, paddr, psize, trylock));
        }

        if (type != 0) {
            log.warn("hleKernelVolatileMemLock bad param: type != 0");
            cpu.gpr[2] = ERROR_INVALID_ARGUMENT;
        } else {
            if (!volatileMemLocked) {
                volatileMemLocked = true;
                if (Memory.isAddressGood(paddr)) {
                    mem.write32(paddr, 0x08400000); // Volatile mem is always at 0x08400000
                }
                if (Memory.isAddressGood(psize)) {
                    mem.write32(psize, 0x400000);   // Volatile mem size is 4Megs
                }
                cpu.gpr[2] = 0;
            } else {
                log.warn("hleKernelVolatileMemLock already locked");
                if (trylock) {
                    cpu.gpr[2] = ERROR_POWER_VMEM_IN_USE;
                } else {
                    cpu.gpr[2] = -1;
                }
            }
        }
    }

    @HLEFunction(nid = 0x3E0271D3, version = 150)
    public void sceKernelVolatileMemLock(Processor processor) {
        CpuState cpu = processor.cpu;

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        hleKernelVolatileMemLock(processor, false);
    }

    @HLEFunction(nid = 0xA14F40B2, version = 150)
    public void sceKernelVolatileMemTryLock(Processor processor) {
        hleKernelVolatileMemLock(processor, true);
    }

    @HLEFunction(nid = 0xA569E425, version = 150)
    public void sceKernelVolatileMemUnlock(Processor processor) {
        CpuState cpu = processor.cpu;

        int type = cpu.gpr[4];

        log.debug("sceKernelVolatileMemUnlock(type=" + type + ")");

        if (type != 0) {
            log.warn("sceKernelVolatileMemUnlock bad param: type != 0");
            cpu.gpr[2] = ERROR_INVALID_ARGUMENT;
        } else if (!volatileMemLocked) {
            log.warn("sceKernelVolatileMemUnlock - Volatile Memory was not locked!");
            cpu.gpr[2] = -1;
        } else {
            volatileMemLocked = false;
            cpu.gpr[2] = 0;
        }
    }

}