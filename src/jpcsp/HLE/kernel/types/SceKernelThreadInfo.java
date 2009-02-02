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
package jpcsp.HLE.kernel.types;

import java.util.Comparator;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.kernel.managers.SceUidManager;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;

/** Don't forget to call ThreadMan.threadMap.put(thread.uid, thread) after instantiating one of these. */
public class SceKernelThreadInfo implements Comparator<SceKernelThreadInfo> {

    /* Posted at http://forums.ps2dev.org/viewtopic.php?p=75691#75691 by Insert_witty_name
     * http://forums.ps2dev.org/viewtopic.php?p=77135#77135 by phobox
     * http://forums.ps2dev.org/viewtopic.php?t=8917 by SilverSpring
    public static final int PSP_MODULE_USER                 = 0;

    public static final int PSP_MODULE_NO_STOP              = 0x00000001;
    public static final int PSP_MODULE_SINGLE_LOAD          = 0x00000002;
    public static final int PSP_MODULE_SINGLE_START         = 0x00000004;

    public static final int PSP_MODULE_POPS                 = 0x00000200;
    public static final int PSP_MODULE_DEMO                 = 0x00000200; // same as PSP_MODULE_POPS
    public static final int PSP_MODULE_GAMESHARING          = 0x00000400;
    public static final int PSP_MODULE_VSH                  = 0x00000800;

    public static final int PSP_MODULE_KERNEL               = 0x00001000;
    public static final int PSP_MODULE_USE_MEMLMD_LIB       = 0x00002000;
    public static final int PSP_MODULE_USE_SEMAPHORE_LIB    = 0x00004000; // not kernel semaphores, but a fake name (actually security stuff)
    */

    // TODO are module/thread attr interchangeable? (probably yes)
    public static final int PSP_THREAD_ATTR_USER = 0x80000000;
    public static final int PSP_THREAD_ATTR_USBWLAN = 0xa0000000;
    public static final int PSP_THREAD_ATTR_VSH = 0xc0000000;
    public static final int PSP_THREAD_ATTR_KERNEL = 0x00001000;
    public static final int PSP_THREAD_ATTR_VFPU = 0x00004000;
    public static final int PSP_THREAD_ATTR_SCRATCH_SRAM = 0x00008000;
    public static final int PSP_THREAD_ATTR_NO_FILLSTACK = 0x00100000; // Disables filling the stack with 0xFF on creation.
    public static final int PSP_THREAD_ATTR_CLEAR_STACK = 0x00200000; // Clear the stack when the thread is deleted.

    // PspThreadStatus
    public static final int PSP_THREAD_RUNNING  = 0x00000001;
    public static final int PSP_THREAD_READY    = 0x00000002;
    public static final int PSP_THREAD_WAITING  = 0x00000004;
    public static final int PSP_THREAD_SUSPEND  = 0x00000008;
    public static final int PSP_THREAD_STOPPED  = 0x00000010;
    public static final int PSP_THREAD_KILLED   = 0x00000020;

    // SceKernelThreadInfo <http://psp.jim.sh/pspsdk-doc/structSceKernelThreadInfo.html>
    public final String name;
    public int attr;
    public int status;
    public final int entry_addr;
    public final int stack_addr; // currently using bottom
    public final int stackSize;
    public int gpReg_addr;
    public final int initPriority;
    public int currentPriority;
    public int waitType; // 3 = sema
    public int waitId;
    public int wakeupCount;
    public int exitStatus;
    public long runClocks;
    public int intrPreemptCount;
    public int threadPreemptCount;
    public int releaseCount;

    // internal variables
    public final int uid;
    public CpuState cpuContext;
    public boolean do_delete;
    public boolean do_callbacks;

    public final ThreadWaitInfo wait;

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

    public SceKernelThreadInfo(String name, int entry_addr, int initPriority, int stackSize, int attr) {
        // Ignore 0 size from the idle threads (don't want them stealing space)
        if (stackSize != 0) {
            if (stackSize < 512) {
                // 512 byte min
                stackSize = 512;
            } else {
                // 256 byte size alignment (should be 16?)
                stackSize = (stackSize + 0xFF) & ~0xFF;
            }
        }

        this.name = name;
        this.entry_addr = entry_addr;
        this.initPriority = initPriority;
        this.stackSize = stackSize;
        this.attr = attr;

        status = PSP_THREAD_SUSPEND;
        stack_addr = ThreadMan.getInstance().mallocStack(stackSize);
        if (stack_addr != 0 &&
            stackSize > 0 &&
            (attr & PSP_THREAD_ATTR_NO_FILLSTACK) != PSP_THREAD_ATTR_NO_FILLSTACK) {
            Memory.getInstance().memset(stack_addr, (byte)0xFF, stackSize);
        }
        gpReg_addr = Emulator.getProcessor().cpu.gpr[28]; // inherit gpReg // TODO addr into ModuleInfo struct?
        currentPriority = initPriority;
        waitType = 0; // not yet used by us
        waitId = 0; // ?
        wakeupCount = 0;
        exitStatus = ERROR_THREAD_IS_NOT_DORMANT;
        runClocks = 0;
        intrPreemptCount = 0;
        threadPreemptCount = 0;
        releaseCount = 0;

        // internal state
        uid = SceUidManager.getNewUid("ThreadMan-thread");

        // Inherit context
        //cpuContext = new CpuState();
        //saveContext();
        cpuContext = new CpuState(Emulator.getProcessor().cpu);

        // Thread specific registers
        cpuContext.pc = entry_addr;
        cpuContext.npc = entry_addr; // + 4;

        // sp, 512 byte padding at the top for user data, this will get re-jigged when we call start thread
        cpuContext.gpr[29] = stack_addr + stackSize - 512;
        cpuContext.gpr[26] = cpuContext.gpr[29]; // k0 mirrors initial sp (almost)

        // We'll hook "jr ra" where ra = 0 as the thread exiting
        cpuContext.gpr[31] = 0; // ra

        do_delete = false;
        do_callbacks = false;

        wait = new ThreadWaitInfo();

        callbackRegistered = new boolean[THREAD_CALLBACK_SIZE];
        callbackReady = new boolean[THREAD_CALLBACK_SIZE];
        callbackInfo = new SceKernelCallbackInfo[THREAD_CALLBACK_SIZE];
        for (int i = 0; i < THREAD_CALLBACK_SIZE; i++) {
            callbackRegistered[i] = false;
            callbackReady[i] = false;
            callbackInfo[i] = null;
        }
    }

    public void saveContext() {
        cpuContext = Emulator.getProcessor().cpu;
        //cpuContext.copy(Emulator.getProcessor().cpu);

        // ignore PSP_THREAD_ATTR_VFPU flag
    }

    public void restoreContext() {
        // Assuming context switching only happens on syscall,
        // we always execute npc after a syscall,
        // so we can set pc = npc regardless of cop0.status.bd.
        //if (!cpu.cop0_status_bd)
            cpuContext.pc = cpuContext.npc;

        Emulator.getProcessor().cpu = cpuContext;
        //Emulator.getProcessor().cpu.copy(cpuContext);

        // ignore PSP_THREAD_ATTR_VFPU flag

        /*
        if (this != idle0 && this != idle1) {
            Modules.log.debug("restoreContext SceUID=" + Integer.toHexString(uid)
                + " name:" + name
                + " PC:" + Integer.toHexString(cpuContext.pc)
                + " NPC:" + Integer.toHexString(cpuContext.npc));
        }
        */
        RuntimeContext.update();
    }

    /** For use in the scheduler */
    @Override
    public int compare(SceKernelThreadInfo o1, SceKernelThreadInfo o2) {
        return o1.currentPriority - o2.currentPriority;
    }

    public void write(Memory mem, int address) {
        mem.write32(address, 104); // size

        int i, len = name.length();
        for (i = 0; i < 32 && i < len; i++)
            mem.write8(address + 4 + i, (byte)name.charAt(i));
        for (; i < 32; i++)
            mem.write8(address + 4 + i, (byte)0);

        mem.write32(address + 36, attr);
        mem.write32(address + 40, status);
        mem.write32(address + 44, entry_addr);
        mem.write32(address + 48, stack_addr);
        mem.write32(address + 52, stackSize);
        mem.write32(address + 56, gpReg_addr);
        mem.write32(address + 60, initPriority);
        mem.write32(address + 64, currentPriority);
        mem.write32(address + 68, waitType);
        mem.write32(address + 72, waitId);
        mem.write32(address + 76, wakeupCount);
        mem.write32(address + 80, exitStatus);
        mem.write64(address + 84, runClocks);
        mem.write32(address + 92, intrPreemptCount);
        mem.write32(address + 96, threadPreemptCount);
        mem.write32(address + 100, releaseCount);
    }
}
