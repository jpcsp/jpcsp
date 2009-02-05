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

import jpcsp.HLE.Modules;
import jpcsp.Allegrex.CpuState;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;

public class SystemTimeManager {

    public void reset() {
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
                Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    /** SceKernelSysClock time_addr http://psp.jim.sh/pspsdk-doc/structSceKernelSysClock.html
     * +1mil every second
     * high 32-bits never set on real psp? */
    public void sceKernelGetSystemTime(int time_addr) {
        Modules.log.debug("sceKernelGetSystemTime pointer=0x" + Integer.toHexString(time_addr));
        Memory mem = Memory.getInstance();
        if (mem.isAddressGood(time_addr)) {
            long systemTime = System.nanoTime() / 1000L;
            int low = (int)(systemTime & 0xffffffffL);
            int hi = (int)((systemTime >> 32) & 0xffffffffL);

            mem.write32(time_addr, low);
            mem.write32(time_addr + 4, hi);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
    }

    public void sceKernelGetSystemTimeWide() {
        long systemTime = System.nanoTime() / 1000L;
        Modules.log.debug("sceKernelGetSystemTimeWide ret:" + systemTime);
        Emulator.getProcessor().cpu.gpr[2] = (int)(systemTime & 0xffffffffL);
        Emulator.getProcessor().cpu.gpr[3] = (int)((systemTime >> 32) & 0xffffffffL);
    }

    //private int timeLow = 0;
    // microseconds
    public void sceKernelGetSystemTimeLow() {
        long systemTime = System.nanoTime() / 1000L;
        int low = (int)(systemTime & 0x7fffffffL); // check, don't use msb?
        //int low = timeLow; timeLow += 10;
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
