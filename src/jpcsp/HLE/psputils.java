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


public class psputils {
    private static psputils instance;
    private int initialclocks;
    private HashMap<Integer, SceKernelUtilsMt19937Context> Mt19937List;

    public static psputils get_instance() {
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
        long millis = Calendar.getInstance().getTimeInMillis();
        int seconds = (int)(millis / 1000);
        if (time_t_addr != 0)
            Memory.getInstance().write32(time_t_addr, seconds);
        Emulator.getProcessor().gpr[2] = seconds;
    }

    /** returns the number of clocks since the "process" started.
     * Current implemention uses clocks since Initialise was last called, and
     * we are using clocks = seconds * CLOCKS_PER_SEC, where CLOCKS_PER_SEC
     * is 1 million (1000000). */
    protected int currentClocks = 0;
    public void sceKernelLibcClock() {
        //int clocks = (int)System.nanoTime() - initialclocks; // seconds * 1 million
    	currentClocks += 100;	// FIXME: quick hack to fix NesterJ
        Emulator.getProcessor().gpr[2] = currentClocks;
    }

    public void sceKernelDcacheWritebackAll() {
        System.out.println("(Unimplement):sceKernelDcacheWritebackAll");
    }

    public void sceKernelDcacheWritebackInvalidateAll() {
        System.out.println("(Unimplement):sceKernelDcacheWritebackInvalidateAll");
    }

    public void sceKernelDcacheWritebackRange(int p_addr, int size) {
        System.out.println("(Unimplement):sceKernelDcacheWritebackRange");
    }

    public void sceKernelDcacheWritebackInvalidateRange(int p_addr, int size) {
       System.out.println("(Unimplement):sceKernelDcacheWritebackInvalidateRange");
    }

    public void sceKernelDcacheInvalidateRange(int p_addr, int size) {
        System.out.println("(Unimplement):sceKernelDcacheInvalidateRange");
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

        Emulator.getProcessor().gpr[2] = 0;
    }

    public void sceKernelUtilsMt19937UInt(int ctx_addr) {
        SceKernelUtilsMt19937Context ctx = Mt19937List.get(ctx_addr);
        if (ctx != null) {
            Emulator.getProcessor().gpr[2] = ctx.r.nextInt();
        } else {
            // TODO what happens if the ctx is bad?
            System.out.println("sceKernelUtilsMt19937UInt uninitialised context " + Integer.toHexString(ctx_addr));
            Emulator.getProcessor().gpr[2] = 0;
        }
    }

    private class SceKernelUtilsMt19937Context {
        private Random r;

        public SceKernelUtilsMt19937Context(int seed) {
            r = new Random(seed);
        }
    }
}
