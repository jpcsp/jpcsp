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

import jpcsp.*;
import jpcsp.HLE.*;
import jpcsp.HLE.kernel.types.*;
import jpcsp.HLE.modules.ThreadManForUser;

import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;

import java.util.Iterator;

public class DumpDebugState {
    public static void dumpDebugState() {
        log("------------------------------------------------------------");
        if (isGameLoaded()) {
            dumpCurrentFrame();
            dumpThreads();
            pspSysMem.getInstance().dumpSysMemInfo();
        } else {
            log("No game loaded");
        }
        log("------------------------------------------------------------");
    }

    private static String getThreadStatusName(int status) {
        String name = "";

        // A thread status is a bitfield so it could be in multiple states, but I don't think we use this "feature" in our HLE, handle it anyway
        if ((status & PSP_THREAD_RUNNING) == PSP_THREAD_RUNNING)
            name += " | PSP_THREAD_RUNNING";
        if ((status & PSP_THREAD_READY) == PSP_THREAD_READY)
            name += " | PSP_THREAD_READY";
        if ((status & PSP_THREAD_WAITING) == PSP_THREAD_WAITING)
            name += " | PSP_THREAD_WAITING";
        if ((status & PSP_THREAD_SUSPEND) == PSP_THREAD_SUSPEND)
            name += " | PSP_THREAD_SUSPEND";
        if ((status & PSP_THREAD_STOPPED) == PSP_THREAD_STOPPED)
            name += " | PSP_THREAD_STOPPED";
        if ((status & PSP_THREAD_KILLED) == PSP_THREAD_KILLED)
            name += " | PSP_THREAD_KILLED";

        // Strip off leading " | "
        if (name.length() > 0)
            name = name.substring(3);
        else
            name = "UNKNOWN";

        return name;
    }

    private static String getThreadWaitName(SceKernelThreadInfo thread) {
        ThreadWaitInfo wait = thread.wait;
        String name = "";

        // A thread should only be waiting on at most 1 thing, handle it anyway
        if (wait.waitingOnThreadEnd)
            name += String.format(" | ThreadEnd (0x%04X)", wait.ThreadEnd_id);

        if (wait.waitingOnEventFlag)
            name += String.format(" | EventFlag (0x%04X)", wait.EventFlag_id);

        if (wait.waitingOnSemaphore)
            name += String.format(" | Semaphore (0x%04X)", wait.Semaphore_id);

        if (wait.waitingOnMutex)
            name += String.format(" | Mutex (0x%04X)", wait.Mutex_id);

        if (wait.waitingOnIo)
            name += String.format(" | Io (0x%04X)", wait.Io_id);

        if (wait.waitingOnUmd)
            name += String.format(" | Umd (0x%02X)", wait.wantedUmdStat);

        // Strip off leading " | "
        if (name.length() > 0)
        {
            name = name.substring(3);
        }
        else if ((thread.status & PSP_THREAD_WAITING) == PSP_THREAD_WAITING)
        {
            if (wait.forever)
                name = "None (sleeping)";
            else
                name = "None (delay)";
        }
        else
        {
            name = "None";
        }

        return name;
    }

    public static void dumpThreads() {
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();)
        {
            SceKernelThreadInfo thread = it.next();

            log("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            log(String.format("Thread Name: '%s' ID: 0x%04X Module ID: 0x%04X", thread.name, thread.uid, thread.moduleid));
            log(String.format("Thread Status: 0x%08X %s", thread.status, getThreadStatusName(thread.status)));
            log(String.format("Thread Attr: 0x%08X Current Priority: 0x%02X Initial Priority: 0x%02X", thread.attr, thread.currentPriority, thread.initPriority));
            log(String.format("Thread Entry: 0x%08X Stack: 0x%08X - 0x%08X Stack Size: 0x%08X", thread.entry_addr, thread.stack_addr, thread.stack_addr + thread.stackSize, thread.stackSize));
            log(String.format("Thread Run Clocks: %d Exit Code: 0x%08X", thread.runClocks, thread.exitStatus));
            log(String.format("Thread Wait Type: %s Us: %d Forever: %s", getThreadWaitName(thread), thread.wait.micros, thread.wait.forever));
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

    private static void log(String msg) {
        System.err.println(msg);
        Modules.log.error(msg);
    }
}