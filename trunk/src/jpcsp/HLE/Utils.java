/*
TODO
- HLE everything in http://psp.jim.sh/pspsdk-doc/group__Utils.html


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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;
import jpcsp.Processor;


public class Utils {
    private static Utils instance;
    private static HashMap<Integer, SceKernelUtilsMt19937Context> Mt19937List;

    public static Utils get_instance() {
        if (instance == null) {
            instance = new Utils();
        }
        return instance;
    }

    private Utils() {
    }

    /** call this when resetting the emulator */
    public void Initialise() {
        //System.out.println("Utils: Initialise");

        Mt19937List = new HashMap<Integer, SceKernelUtilsMt19937Context>();
    }

    public void Utils_sceKernelUtilsMt19937Init(int a0, int a1) {
        // We'll use the address of the ctx as a key
        Mt19937List.remove(a0); // Remove records of any already existing context at a0
        Mt19937List.put(a0, new SceKernelUtilsMt19937Context(a1));
        Emulator.getProcessor().gpr[2] = 0;
    }

    public void Utils_sceKernelUtilsMt19937UInt(int a0) {
        SceKernelUtilsMt19937Context ctx = Mt19937List.get(a0);
        if (ctx != null) {
            Emulator.getProcessor().gpr[2] = ctx.r.nextInt();
        } else {
            // TODO what happens if the ctx is bad?
            System.out.println("sceKernelUtilsMt19937UInt uninitialised context " + Integer.toHexString(a0));
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
