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
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointer64;

import org.apache.log4j.Logger;

public class SystemTimeManager {

    protected static Logger log = Modules.getLogger("ThreadManForUser");

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

    public int sceKernelUSec2SysClock(int usec, TPointer64 sysClockAddr) {
    	sysClockAddr.setValue(usec & 0xFFFFFFFFL);
        return 0;
    }

    public long sceKernelUSec2SysClockWide(int usec) {
        return usec & 0xFFFFFFFFL;
    }

    public int sceKernelSysClock2USec(TPointer64 sysClockAddr, TPointer32 secAddr, TPointer32 microSecAddr) {
        long sysClock = sysClockAddr.getValue();
        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceKernelSysClock2USec sysClockAddr=%s(%d), secAddr=%s, microSecAddr=%s", sysClockAddr, sysClock, secAddr, microSecAddr));
        }

        if (secAddr.isNull()) {
        	// PSP is copying sysclock value directly to microSecAddr when secAddr is NULL
        	microSecAddr.setValue((int) sysClock);
        } else {
	    	secAddr.setValue((int) (sysClock / 1000000));
	    	microSecAddr.setValue((int) (sysClock % 1000000));
        }

        return 0;
    }

    public int sceKernelSysClock2USecWide(long sysClock, TPointer32 secAddr, TPointer32 microSecAddr) {
        if (secAddr.isNull()) {
        	// PSP is copying sysclock value directly to microSecAddr when secAddr is NULL
        	microSecAddr.setValue((int) sysClock);
        } else {
	    	secAddr.setValue((int) (sysClock / 1000000));
	    	microSecAddr.setValue((int) (sysClock % 1000000));
        }

        return 0;
    }

    public static long getSystemTime() {
        // System time is number of microseconds since program start
        return Emulator.getClock().microTime();
    }

    public int sceKernelGetSystemTime(TPointer64 time_addr) {
        long systemTime = getSystemTime();
        time_addr.setValue(systemTime);

        return 0;
    }

    public long sceKernelGetSystemTimeWide() {
        long systemTime = getSystemTime();
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetSystemTimeWide returning %d", systemTime));
        }
        return systemTime;
    }

    public int sceKernelGetSystemTimeLow() {
        int systemTimeLow = (int) (getSystemTime() & 0xFFFFFFFFL);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetSystemTimeLow returning %d", systemTimeLow));
        }
        return systemTimeLow;
    }

    public static final SystemTimeManager singleton = new SystemTimeManager();

    private SystemTimeManager() {
    }
}