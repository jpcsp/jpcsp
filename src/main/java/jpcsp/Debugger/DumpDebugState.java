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

import java.util.Iterator;

import jpcsp.Emulator;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.ThreadManForUser;

public class DumpDebugState {
    public static void dumpDebugState() {
        log("------------------------------------------------------------");
        if (isGameLoaded()) {
            dumpCurrentFrame();
            dumpThreads();
            Modules.SysMemUserForUserModule.dumpSysMemInfo();
        } else {
            log("No game loaded");
        }
        log("------------------------------------------------------------");
    }

    public static void dumpThreads() {
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();)
        {
            SceKernelThreadInfo thread = it.next();

            log("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            log(String.format("Thread Name: '%s' ID: 0x%04X Module ID: 0x%04X", thread.name, thread.uid, thread.moduleid));
            log(String.format("Thread Status: 0x%08X %s", thread.status, thread.getStatusName()));
            log(String.format("Thread Attr: 0x%08X Current Priority: 0x%02X Initial Priority: 0x%02X", thread.attr, thread.currentPriority, thread.initPriority));
            log(String.format("Thread Entry: 0x%08X Stack: 0x%08X - 0x%08X Stack Size: 0x%08X", thread.entry_addr, thread.getStackAddr(), thread.getStackAddr() + thread.stackSize, thread.stackSize));
            log(String.format("Thread Run Clocks: %d Exit Code: 0x%08X", thread.runClocks, thread.exitStatus));
            log(String.format("Thread Wait Type: %s Us: %d Forever: %s", thread.getWaitName(), thread.wait.micros, thread.wait.forever));
        }
    }

    public static void dumpCurrentFrame() {
        StepFrame frame = new StepFrame();
        frame.make(Emulator.getProcessor().cpu);
        log(frame.getMessage());
    }

    private static boolean isGameLoaded() {
        return Modules.ThreadManForUserModule.getCurrentThreadID() != -1;
    }

    public static void log(String msg) {
        System.err.println(msg);
        Modules.log.error(msg);
    }
}