package jpcsp.Debugger;

import jpcsp.*;
//import jpcsp.Allegrex.*;
import jpcsp.HLE.*;
import jpcsp.HLE.kernel.types.*;

import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;

import java.util.Iterator;

public class DumpDebugState
{
    public static void dumpDebugState()
    {
        log("------------------------------------------------------------");

        if (isGameLoaded())
        {
            // print registers, current instruction and current thread
            dumpCurrentFrame();

            // print all threads
            dumpThreads();

            // print memory
            pspSysMem.getInstance().dumpSysMemInfo();
        }
        else
        {
            log("No game loaded");
        }

        log("------------------------------------------------------------");
    }

    private static String getThreadStatusName(int status)
    {
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

    private static String getThreadWaitName(SceKernelThreadInfo thread)
    {
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

    public static void dumpThreads()
    {
        ThreadMan threadMan = ThreadMan.getInstance();

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

/*
    // callbacks, only 1 of each type can be registered per thread
    public final static int THREAD_CALLBACK_UMD         = 0;
    public final static int THREAD_CALLBACK_IO          = 1;
    public final static int THREAD_CALLBACK_GE_SIGNAL   = 2;
    public final static int THREAD_CALLBACK_GE_FINISH   = 3;
    public final static int THREAD_CALLBACK_MEMORYSTICK = 4;
    public final static int THREAD_CALLBACK_SIZE        = 5;
    public boolean[] callbackRegistered;
    public boolean[] callbackReady;
    public SceKernelCallbackInfo[] callbackInfo;
*/
        }
    }

    public static void dumpCurrentFrame()
    {
        StepFrame frame = new StepFrame();
        frame.make(Emulator.getProcessor().cpu);
        log(frame.getMessage());
    }

    private static boolean isGameLoaded()
    {
        // HACK
        return ThreadMan.getInstance().getCurrentThreadID() != -1;
    }

    private static void log(long x)
    {
        System.err.println("" + x);
    }

    private static void log(int x)
    {
        System.err.println("" + x);
    }

    private static void log(String msg)
    {
        System.err.println(msg);
    }
}
