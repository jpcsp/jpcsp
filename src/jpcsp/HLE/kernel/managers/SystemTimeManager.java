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
package jpcsp.HLE.kernel.managers;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.Modules;

public class SystemTimeManager {

    public void reset() {
    }

    /**
     * Convert a number of sysclocks into microseconds.
     * 
     * @param sysclocks	- number of sysclocks
     * @return microseconds
     */
    public static long hleSysClock2USec(long sysclocks) {
    	// 1 sysclock == 1 microsecond
    	return sysclocks;
    }

    /**
     * Convert a number of sysclocks into microseconds,
     * truncating to 32 bits.
     * 
     * @param sysclocks	- number of sysclocks
     * @return microseconds (truncated to 32 bits)
     *         Integer.MAX_VALUE or MIN_VALUE in case of truncation overflow.
     */
    public static int hleSysClock2USec32(long sysclocks) {
    	long micros64 = hleSysClock2USec(sysclocks);

    	int micros32 = (int) micros64;
    	if (micros64 > Integer.MAX_VALUE) {
    		micros32 = Integer.MAX_VALUE;
    	} else if (micros64 < Integer.MIN_VALUE) {
    		micros32 = Integer.MIN_VALUE;
    	}

    	return micros32;
    }

    public void sceKernelUSec2SysClock(int usec, int clock_addr) {
        Memory mem = Memory.getInstance();
        if (!mem.isAddressGood(clock_addr)) {
            Modules.log.error("sceKernelUSec2SysClock bad clock pointer 0x" + Integer.toHexString(clock_addr));
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
        } else {
            mem.write64(clock_addr, usec);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceKernelUSec2SysClockWide(long usec) {
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceKernelUSec2SysClockWide usec:" + usec);
        }
        Emulator.getProcessor().cpu.gpr[2] = (int)(usec & 0xffffffffL);
        Emulator.getProcessor().cpu.gpr[3] = (int)((usec >> 32) & 0xffffffffL);
    }

    public void sceKernelSysClock2USec(int clock_addr, int low_addr, int high_addr) {
        Memory mem = Memory.getInstance();
        if (!mem.isAddressGood(clock_addr)) {
            Modules.log.error("sceKernelSysClock2USec bad clock pointer 0x" + Integer.toHexString(clock_addr));
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
        } else {
            boolean ok = false;
            long clocks = mem.read64(clock_addr);

            if (mem.isAddressGood(low_addr)) {
                mem.write32(low_addr, (int)(clocks / 1000000));
                ok = true;
            }

            if (mem.isAddressGood(high_addr)) {
                mem.write32(high_addr, (int)(clocks % 1000000));
                ok = true;
            }

            if (!ok) {
                Modules.log.error("sceKernelSysClock2USec bad output pointers "
                    + " 0x" + Integer.toHexString(low_addr)
                    + " 0x" + Integer.toHexString(high_addr));
                Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_WRITE);
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceKernelSysClock2USecWide(int sysclockLow, int sysclockHigh, int low_addr, int high_addr) {
        // sysclockLow and sysclockHigh are for example
        // the result from sceKernelGetSystemTimeWide()
        long clocks = (sysclockLow) & 0xFFFFFFFFL | (((long) sysclockHigh) << 32);

        Memory mem = Memory.getInstance();
        boolean ok = false;
        if (mem.isAddressGood(low_addr)) {
            mem.write32(low_addr, (int)(clocks / 1000000));
            ok = true;
        }

        if (mem.isAddressGood(high_addr)) {
            mem.write32(high_addr, (int)(clocks % 1000000));
            ok = true;
        }

        if (!ok) {
            Modules.log.error("sceKernelSysClock2USecWide bad output pointers "
                + " 0x" + Integer.toHexString(low_addr)
                + " 0x" + Integer.toHexString(high_addr));
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_WRITE);
        }

        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public static long getSystemTime() {
    	// System time is number of microseconds since program start
    	return Emulator.getClock().microTime();
    }

    /** SceKernelSysClock time_addr http://psp.jim.sh/pspsdk-doc/structSceKernelSysClock.html
     * +1mil every second
     * high 32-bits never set on real psp? */
    public void sceKernelGetSystemTime(int time_addr) {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceKernelGetSystemTime pointer=0x" + Integer.toHexString(time_addr));
    	}

    	Memory mem = Memory.getInstance();
        if (mem.isAddressGood(time_addr)) {
            long systemTime = getSystemTime();
            mem.write64(time_addr, systemTime);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
    }

    public void sceKernelGetSystemTimeWide() {
        long systemTime = getSystemTime();
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceKernelGetSystemTimeWide ret:" + systemTime);
        }
        Emulator.getProcessor().cpu.gpr[2] = (int)(systemTime & 0xffffffffL);
        Emulator.getProcessor().cpu.gpr[3] = (int)((systemTime >> 32) & 0xffffffffL);
    }

    public void sceKernelGetSystemTimeLow() {
        long systemTime = getSystemTime();
        int low = (int)(systemTime & 0x7fffffffL); // check, don't use msb?
        //Modules.log.debug("sceKernelGetSystemTimeLow return:" + low);
        Emulator.getProcessor().cpu.gpr[2] = low;
    }

    // -------------------------- singleton --------------------------

    public static final SystemTimeManager singleton;

    private SystemTimeManager() {
    }

    static {
        singleton = new SystemTimeManager();
    }
}
