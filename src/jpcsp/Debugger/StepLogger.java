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
package jpcsp.Debugger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import jpcsp.Emulator;
import jpcsp.Allegrex.CpuState;

public class StepLogger {
    private static int size = 0;
    private static int position = 0;
    private static final int capacity = 64;
    private static StepFrame[] frames;
    private static String name;
    private static int status = Emulator.EMU_STATUS_UNKNOWN;

    static {
        frames = new StepFrame[capacity];
        for(int i = 0; i < capacity; i++) {
            frames[i] = new StepFrame();
        }
    }

    public static void append(CpuState cpu) {
        frames[position].make(cpu);

        if (size < capacity)
            size++;

        position = (position + 1) % capacity;
    }

    public static void clear() {
        size = 0;
        position = 0;
    }

    private static String statusToString(int status) {
        switch(status) {
        case Emulator.EMU_STATUS_OK: return "OK";
        case Emulator.EMU_STATUS_UNKNOWN: return "Unknown";
        case Emulator.EMU_STATUS_WDT_IDLE: return "WDT (idle)";
        case Emulator.EMU_STATUS_WDT_HOG: return "WDT (hog)";
        case Emulator.EMU_STATUS_MEM_READ: return "Memory (read)";
        case Emulator.EMU_STATUS_MEM_WRITE: return "Memory (write)";
        case Emulator.EMU_STATUS_BREAKPOINT: return "Breakpoint";
        case Emulator.EMU_STATUS_UNIMPLEMENTED: return "Unimplemented";
        case Emulator.EMU_STATUS_PAUSE: return "Pause";
        case Emulator.EMU_STATUS_JUMPSELF: return "Jump to self (death loop)";
        default: return "Unknown 0x" + Integer.toHexString(status);
        }
    }

    public static void flush() {
        if (status == Emulator.EMU_STATUS_OK) {
            clear();
            status = Emulator.EMU_STATUS_UNKNOWN;
            return;
        }

        try {
            FileWriter fw = new FileWriter("step-trace.txt");
            PrintWriter out = new PrintWriter(fw);
            int i;
            int flushPosition = (position - size + capacity) % capacity;

            out.println(name);
            out.println("Instruction Trace Dump - " + statusToString(status));
            out.println();

            // Don't bother printing on wdt hog, the log gets thrashed
            if (status != Emulator.EMU_STATUS_WDT_HOG) {
                int localDepth = 5;

                out.println("Local depth: " + localDepth);
                out.println();

                for (i = 0; i < size; i++) {
                    out.println(frames[flushPosition].getMessage());
                    out.println();


                    if (frames[flushPosition].isJAL()) {
                        localDepth++;
                        out.println("Local depth: " + localDepth);
                        out.println();
                    } else if (frames[flushPosition].isJRRA()) {
                        localDepth--;
                        out.println("Local depth: " + localDepth);
                        out.println();
                    }

                    flushPosition = (flushPosition + 1) % capacity;
                }
            } else {
                out.println("Detailed log unavailable.");
            }

            out.close();
            fw.close();
        } catch(IOException e) {
            e.printStackTrace();
        }

        clear();
        status = Emulator.EMU_STATUS_UNKNOWN;
    }

    public static void setName(String name) {
        StepLogger.name = name;
    }

    public static void setStatus(int status) {
        StepLogger.status = status;
    }
}
