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
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.pspSysMem;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;
import jpcsp.util.Utilities;

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
    public static final int PSP_MODULE_VSH                  = 0x00000800; // can only be loaded from kernel mode?

    public static final int PSP_MODULE_KERNEL               = 0x00001000;
    public static final int PSP_MODULE_USE_MEMLMD_LIB       = 0x00002000;
    public static final int PSP_MODULE_USE_SEMAPHORE_LIB    = 0x00004000; // not kernel semaphores, but a fake name (actually security stuff)
    */

    // TODO are module/thread attr interchangeable? (probably yes)
    public static final int PSP_THREAD_ATTR_USER = 0x80000000; // module attr 0, thread attr: 0x800000FF?
    public static final int PSP_THREAD_ATTR_USBWLAN = 0xa0000000;
    public static final int PSP_THREAD_ATTR_VSH = 0xc0000000;
    public static final int PSP_THREAD_ATTR_KERNEL = 0x00001000; // module attr 0x1000, thread attr: 0?
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

    public static final int PSP_WAIT_NONE       = 0x00;
    public static final int PSP_WAIT_SLEEP      = 0x01; // sleep thread
    public static final int PSP_WAIT_DELAY      = 0x02; // delay thread
    public static final int PSP_WAIT_SEMA       = 0x03; // wait sema
    public static final int PSP_WAIT_MISC       = 0x04; // wait event flag, io, umd, vblank(?)
    public static final int PSP_WAIT_MBX        = 0x05; // wait mbx
    public static final int PSP_WAIT_VPL        = 0x06; // wait vpl
    public static final int PSP_WAIT_FPL        = 0x07; // wait fpl
    public static final int PSP_WAIT_MSGPIPE    = 0x08; // wait msg pipe (send and receive)
    public static final int PSP_WAIT_THREAD_END = 0x09; // wait thread end
    public static final int PSP_WAIT_a          = 0x0a; // ?
    public static final int PSP_WAIT_b          = 0x0b; // ?
    public static final int PSP_WAIT_MUTEX      = 0x0c; // wait mutex

    // SceKernelThreadInfo <http://psp.jim.sh/pspsdk-doc/structSceKernelThreadInfo.html>
    public final String name;
    public int attr;
    public int status; // it's a bitfield but I don't think we ever use more than 1 bit at once
    public final int entry_addr;
    public int stack_addr; // using low address, no need to add stackSize to the pointer returned by malloc
    public int stackSize;
    public int gpReg_addr;
    public final int initPriority; // lower numbers mean higher priority
    public int currentPriority;
    public int waitType;
    public int waitId;  // the uid of the wait object
    public int wakeupCount; // number of sceKernelWakeupThread() calls pending
    public int exitStatus;
    public long runClocks;
    public int intrPreemptCount;
    public int threadPreemptCount;
    public int releaseCount;

    private int sysMemUID = -1;
    // internal variables
    public final int uid;
    public int moduleid;
    public CpuState cpuContext;
    public boolean doDelete;
    public IAction doDeleteAction;
    public boolean doCallbacks;

    public final ThreadWaitInfo wait;
    public IAction onUnblockAction;

    // callbacks, only 1 of each type can be registered per thread
    public final static int THREAD_CALLBACK_UMD         = 0;
    public final static int THREAD_CALLBACK_IO          = 1;
    public final static int THREAD_CALLBACK_MEMORYSTICK = 2;
    public final static int THREAD_CALLBACK_SIZE        = 3;
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

        uid = SceUidManager.getNewUid("ThreadMan-thread");

        // setup the stack
        if (stackSize > 0) {
            stack_addr = pspSysMem.getInstance().malloc(2, pspSysMem.PSP_SMEM_High, stackSize, 0);
            if (stack_addr != 0) {
                sysMemUID = pspSysMem.getInstance().addSysMemInfo(2, "ThreadMan-Stack", pspSysMem.PSP_SMEM_High, stackSize, stack_addr);
            }
        } else {
            stack_addr = 0;
        }


        gpReg_addr = Emulator.getProcessor().cpu.gpr[28]; // inherit gpReg // TODO addr into ModuleInfo struct?
        // internal state

        // Inherit context
        cpuContext = new CpuState(Emulator.getProcessor().cpu);

        wait = new ThreadWaitInfo();

        reset();
    }

    public void reset() {
        status = PSP_THREAD_STOPPED;

        int k0 = stack_addr + stackSize - 0x100; // setup k0
        Memory mem = Memory.getInstance();
        if (stack_addr != 0 && stackSize > 0) {
            // set stack to 0xFF
            if ((attr & PSP_THREAD_ATTR_NO_FILLSTACK) != PSP_THREAD_ATTR_NO_FILLSTACK) {
                mem.memset(stack_addr, (byte)0xFF, stackSize);
            }

            // setup k0
            mem.memset(k0, (byte)0x0, 0x100);
            mem.write32(k0 + 0xc0, stack_addr);
            mem.write32(k0 + 0xca, uid);
            mem.write32(k0 + 0xf8, 0xffffffff);
            mem.write32(k0 + 0xfc, 0xffffffff);

            mem.write32(stack_addr, uid);
        }

        currentPriority = initPriority;
        waitType = PSP_WAIT_NONE;
        waitId = 0;
        wakeupCount = 0;
        exitStatus = ERROR_THREAD_IS_NOT_DORMANT;
        runClocks = 0;
        intrPreemptCount = 0;
        threadPreemptCount = 0;
        releaseCount = 0;

        // Thread specific registers
        cpuContext.pc = entry_addr;
        cpuContext.npc = entry_addr; // + 4;

        // sp, 512 byte padding at the top for user data, this will get re-jigged when we call start thread
        cpuContext.gpr[29] = stack_addr + stackSize - 512;
        cpuContext.gpr[26] = k0;

        // We'll hook "jr $ra" where $ra == address of HLE syscall hleKernelExitThread
        // when the thread is exiting
        cpuContext.gpr[31] = ThreadMan.THREAD_EXIT_HANDLER_ADDRESS; // $ra

        doDelete = false;
        doCallbacks = false;

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
        Utilities.writeStringNZ(mem, address + 4, 32, name);
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

	@Override
	public String toString() {
		return name;
	}

    public void deleteSysMemInfo() {
        pspSysMem.getInstance().free(sysMemUID, stack_addr);
    }
}
