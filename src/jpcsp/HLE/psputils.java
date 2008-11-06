/*
TODO
- HLE everything in http://psp.jim.sh/pspsdk-doc/psputils_8h.html


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
package jpcsp.HLE;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.State;

public class psputils {
    private static psputils instance;
    private int initialclocks;
    private HashMap<Integer, SceKernelUtilsMt19937Context> Mt19937List;

    public static psputils getInstance() {
        if (instance == null) {
            instance = new psputils();
        }
        return instance;
    }

    private psputils() {
    }

    /** call this when resetting the emulator */
    public void Initialise() {
        //System.out.println("Utils: Initialise");
        initialclocks = (int)System.nanoTime(); // seconds * 1 million
        Mt19937List = new HashMap<Integer, SceKernelUtilsMt19937Context>();
    }

    /** Get the time in seconds since the epoc (1st Jan 1970).  */
    public void sceKernelLibcTime(int time_t_addr) {
        Memory mem = Memory.getInstance();
        int seconds = (int)(Calendar.getInstance().getTimeInMillis() / 1000);
        if (mem.isAddressGood(time_t_addr)) {
            mem.write32(time_t_addr, seconds);
        }
        Emulator.getProcessor().cpu.gpr[2] = seconds;
    }

    /** returns the number of clocks since the "process" started.
     * Current implemention uses clocks since Initialise was last called, and
     * we are using clocks = seconds * CLOCKS_PER_SEC, where CLOCKS_PER_SEC
     * is 1 million (1000000). */
    protected int currentClocks = 0;
    public void sceKernelLibcClock() {
        //int clocks = (int)System.nanoTime() - initialclocks; // seconds * 1 million
        currentClocks += 100; // FIXME: quick hack to fix NesterJ
        Emulator.getProcessor().cpu.gpr[2] = currentClocks;
    }

    /* from man pages:
    struct timeval {
        time_t tv_sec; // seconds since Jan. 1, 1970
        suseconds_t tv_usec; // and microseconds
    };

    struct timezone {
        int tz_minuteswest; // of Greenwich
        int tz_dsttime; // type of dst correction to apply
    };
    */
    public void sceKernelLibcGettimeofday(int tp, int tzp) {
        Memory mem = Memory.getInstance();

        if (mem.isAddressGood(tp)) {
            int tv_sec = (int)(Calendar.getInstance().getTimeInMillis() / 1000);
            int tv_usec = (int)((System.nanoTime() / 1000) % 1000000);
            mem.write32(tp, tv_sec);
            mem.write32(tp + 4, tv_usec);
        }

        if (mem.isAddressGood(tzp)) {
            int tz_minuteswest = 0; // TODO
            int tz_dsttime = 0; // TODO
            mem.write32(tzp, tz_minuteswest);
            mem.write32(tzp + 4, tz_dsttime);
        }

        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceKernelDcacheWritebackAll() {
        Modules.log.debug("(Unimplement):sceKernelDcacheWritebackAll");
    }

    public void sceKernelDcacheWritebackInvalidateAll() {
        Modules.log.debug("(Unimplement):sceKernelDcacheWritebackInvalidateAll");
    }

    public void sceKernelDcacheWritebackRange(int p_addr, int size) {
        Modules.log.debug("(Unimplement):sceKernelDcacheWritebackRange");
    }

    public void sceKernelDcacheWritebackInvalidateRange(int p_addr, int size) {
        Modules.log.debug("(Unimplement):sceKernelDcacheWritebackInvalidateRange");
    }

    public void sceKernelDcacheInvalidateRange(int p_addr, int size) {
        Modules.log.debug("(Unimplement):sceKernelDcacheInvalidateRange");
    }

    public void sceKernelUtilsMt19937Init(int ctx_addr, int seed) {
        // We'll use the address of the ctx as a key
        Mt19937List.remove(ctx_addr); // Remove records of any already existing context at a0
        Mt19937List.put(ctx_addr, new SceKernelUtilsMt19937Context(seed));

        // We'll overwrite all the context memory, 628 bytes
        Memory mem = Memory.getInstance();
        for (int i = 0; i < 628; i += 4) {
            mem.write32(ctx_addr + i, 0xcdcdcdcd);
        }

        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceKernelUtilsMt19937UInt(int ctx_addr) {
        SceKernelUtilsMt19937Context ctx = Mt19937List.get(ctx_addr);
        if (ctx != null) {
            Emulator.getProcessor().cpu.gpr[2] = ctx.r.nextInt();
        } else {
            // TODO what happens if the ctx is bad?
            System.out.println("sceKernelUtilsMt19937UInt uninitialised context " + Integer.toHexString(ctx_addr));
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    private class SceKernelUtilsMt19937Context {
        private Random r;

        public SceKernelUtilsMt19937Context(int seed) {
            r = new Random(seed);
        }
    }

    /** a0 = address to write result to? */
    public void sceKernelGetGPI() {
        if (State.debugger != null) {
            int gpi = State.debugger.GetGPI();
            Modules.log.info("sceKernelGetGPI 0x" + String.format("%02X", gpi));
            Emulator.getProcessor().cpu.gpr[2] = gpi;
        } else {
            Modules.log.info("sceKernelGetGPI debugger not enabled");
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceKernelSetGPO(int value) {
        Modules.log.info("sceKernelSetGPO 0x" + String.format("%02X", value));
        if (State.debugger != null) {
            State.debugger.SetGPO(value);
        }
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }
}
