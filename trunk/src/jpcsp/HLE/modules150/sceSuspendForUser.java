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
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_POWER_VMEM_IN_USE;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.hardware.Screen;

import org.apache.log4j.Logger;

public class sceSuspendForUser extends HLEModule {
    private static Logger log = Modules.getLogger("sceSuspendForUser");
    public static final int KERNEL_POWER_TICK_SUSPEND_AND_DISPLAY = 0;
    public static final int KERNEL_POWER_TICK_SUSPEND = 1;
    public static final int KERNEL_POWER_TICK_DISPLAY = 6;
    private boolean volatileMemLocked;

    @Override
    public String getName() {
        return "sceSuspendForUser";
    }

    @Override
    public void start() {
        volatileMemLocked = false;

        super.start();
    }

    @HLEFunction(nid = 0xEADB1BD7, version = 150, checkInsideInterrupt = true)
    public int sceKernelPowerLock(int type) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("IGNORING:sceKernelPowerLock type=%d", type));
        }

        return 0;
    }

    @HLEFunction(nid = 0x3AEE7261, version = 150, checkInsideInterrupt = true)
    public int sceKernelPowerUnlock(int type) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("IGNORING:sceKernelPowerUnlock type=%d", type));
        }

        return 0;
    }

    @HLEFunction(nid = 0x090CCB3F, version = 150, checkInsideInterrupt = true)
    public int sceKernelPowerTick(int flag) {
    	// The PSP is checking each of the lower 8 bits of the flag value to tick different
    	// components.
    	// Here we check only a few known bits...
        if ((flag & KERNEL_POWER_TICK_SUSPEND) == KERNEL_POWER_TICK_SUSPEND) {
            if (log.isTraceEnabled()) {
                log.trace("IGNORING:sceKernelPowerTick(KERNEL_POWER_TICK_SUSPEND)");
            }
        }

        if ((flag & KERNEL_POWER_TICK_DISPLAY) == KERNEL_POWER_TICK_DISPLAY) {
        	Screen.hleKernelPowerTick();
            if (log.isTraceEnabled()) {
                log.trace("IGNORING:sceKernelPowerTick(KERNEL_POWER_TICK_DISPLAY)");
            }
        }

        if (flag == KERNEL_POWER_TICK_SUSPEND_AND_DISPLAY) {
        	Screen.hleKernelPowerTick();
            if (log.isTraceEnabled()) {
                log.trace("IGNORING:sceKernelPowerTick(KERNEL_POWER_TICK_SUSPEND_AND_DISPLAY)");
            }
        }

        return 0;
    }

    protected int hleKernelVolatileMemLock(int type, TPointer32 paddr, TPointer32 psize, boolean trylock) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("hleKernelVolatileMemLock type=%d, paddr=%s, psize=%s, trylock=%b", type, paddr, psize, trylock));
        }

        if (type != 0) {
            log.warn("hleKernelVolatileMemLock bad param: type != 0");
            return ERROR_INVALID_ARGUMENT;
        }

        if (volatileMemLocked) {
            log.warn("hleKernelVolatileMemLock already locked");
            return trylock ? ERROR_POWER_VMEM_IN_USE : -1;
        }

        volatileMemLocked = true;
        if (!paddr.isNull()) {
            paddr.setValue(0x08400000); // Volatile mem is always at 0x08400000
        }
        if (!psize.isNull()) {
            psize.setValue(0x400000);   // Volatile mem size is 4Megs
        }

        return 0;
    }

    @HLEFunction(nid = 0x3E0271D3, version = 150, checkInsideInterrupt = true)
    public int sceKernelVolatileMemLock(int type, @CanBeNull TPointer32 paddr, @CanBeNull TPointer32 psize) {
        return hleKernelVolatileMemLock(type, paddr, psize, false);
    }

    @HLEFunction(nid = 0xA14F40B2, version = 150)
    public int sceKernelVolatileMemTryLock(int type, @CanBeNull TPointer32 paddr, @CanBeNull TPointer32 psize) {
        return hleKernelVolatileMemLock(type, paddr, psize, true);
    }

    @HLEFunction(nid = 0xA569E425, version = 150)
    public int sceKernelVolatileMemUnlock(int type) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceKernelVolatileMemUnlock type=%d", type));
    	}

        if (type != 0) {
            log.warn("sceKernelVolatileMemUnlock bad param: type != 0");
            return ERROR_INVALID_ARGUMENT;
        }
        if (!volatileMemLocked) {
            log.warn("sceKernelVolatileMemUnlock - Volatile Memory was not locked!");
            return -1;
        }

        volatileMemLocked = false;

        return 0;
    }
}