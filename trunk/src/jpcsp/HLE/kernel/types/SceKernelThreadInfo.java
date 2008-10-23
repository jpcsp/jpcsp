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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.kernel.Managers;

import jpcsp.HLE.pspSysMem;

/**
 *
 * @author hli
 */
public class SceKernelThreadInfo extends SceKernelUid implements Comparator<SceKernelThreadInfo> {
    // SceKernelThreadInfo <http://psp.jim.sh/pspsdk-doc/structSceKernelThreadInfo.html>
    public ThreadStatus status;
    public int entry_addr;
    public int stack_addr;
    public int stackSize;
    public int gpReg_addr;
    public int initPriority;
    public int currentPriority;
    public int waitType;
    public int waitId;
    public int wakeupCount;
    public int exitStatus;
    public long runClocks;
    public int intrPreemptCount;
    public int threadPreemptCount;
    public int releaseCount;    // internal variables
    public CpuState cpuContext;
    public long delaysteps;
    public boolean do_delete;
    public boolean do_callbacks; // in this implementation, only valid for PSP_THREAD_WAITING and PSP_THREAD_SUSPEND
    public boolean do_waitThreadEnd;
    public int waitThreadEndUid;

    public enum ThreadStatus {

        THREAD_RUNNING(1),
        THREAD_READY(2),
        THREAD_WAITING(4),
        THREAD_SUSPEND(8),
        THREAD_STOPPED(16),
        THREAD_KILLED(32);
        private int value;

        private ThreadStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static final int THREAD_ATTR_USER = 0x80000000;
    public static final int THREAD_ATTR_USBWLAN = 0xa0000000;
    public static final int THREAD_ATTR_VSH = 0xc0000000;
    public static final int THREAD_ATTR_KERNEL = 0x00001000; // TODO are module/thread attr interchangeable?
    public static final int THREAD_ATTR_VFPU = 0x00004000;
    public static final int THREAD_ATTR_SCRATCH_SRAM = 0x00008000;
    public static final int THREAD_ATTR_NO_FILLSTACK = 0x00100000; // Disables filling the stack with 0xFF on creation.
    public static final int THREAD_ATTR_CLEAR_STACK = 0x00200000; // Clear the stack when the thread is deleted.

    private int mallocStack(int size) {
        if (size > 0) {
            int p = pspSysMem.getInstance().malloc(2, pspSysMem.PSP_SMEM_High, size, 0);
            pspSysMem.getInstance().addSysMemInfo(2, "ThreadMan-Stack", pspSysMem.PSP_SMEM_High, size, 0);
            p += size;

            return p;
        } else {
            return 0;
        }
    }

    public SceKernelThreadInfo(String name, int entry_addr, int initPriority, int stackSize, int attr) {
        super(name, attr);

        if (-1 < this.getUid()) {

            // Ignore 0 size from the idle threads (don't want them stealing space)
            if (stackSize != 0) {
                if (stackSize < 512) {
                    // 512 byte min
                    stackSize = 512;
                } else {
                    // 256 byte size alignment
                    stackSize = (stackSize + 0xFF) & ~0xFF;
                }
            }

            this.name = name;
            this.entry_addr = entry_addr;
            this.initPriority = initPriority;
            this.stackSize = stackSize;
            this.attr = attr;

            status = ThreadStatus.THREAD_SUSPEND;
            stack_addr = mallocStack(stackSize);
            if (stackSize > 0 && (attr & THREAD_ATTR_NO_FILLSTACK) != THREAD_ATTR_NO_FILLSTACK) {
                memset(stack_addr - stackSize, (byte) 0xFF, stackSize);
            }
            gpReg_addr = Emulator.getProcessor().cpu.gpr[28]; // inherit gpReg
            currentPriority = initPriority;
            waitType = 0; // ?
            waitId = 0; // ?
            wakeupCount = 0;
            exitStatus = 0x800201a4; // thread is not DORMANT
            runClocks = 0;
            intrPreemptCount = 0;
            threadPreemptCount = 0;
            releaseCount = 0;

            // Inherit context
            cpuContext = new CpuState(Emulator.getProcessor().cpu);

            // Thread specific registers
            cpuContext.pc = entry_addr;
            cpuContext.npc = entry_addr; // + 4;
            cpuContext.gpr[29] = stack_addr; //sp
            cpuContext.gpr[26] = cpuContext.gpr[29]; // k0 mirrors sp?

            // We'll hook "jr ra" where ra = 0 as the thread exiting
            cpuContext.gpr[31] = 0; // ra

            delaysteps = 0;
            do_delete = false;
            do_callbacks = false;
            do_waitThreadEnd = false;
        }
    }

    public void saveContext(Processor processor) {
        cpuContext = processor.cpu;
    }

    public void restoreContext(Processor processor) {
        cpuContext.pc = cpuContext.npc;

        processor.cpu = cpuContext;
    }

    public void release() {
        Managers.threads.releaseObject(this);
    }

    /** For use in the scheduler */
    @Override
    public int compare(SceKernelThreadInfo o1, SceKernelThreadInfo o2) {
        return o1.currentPriority - o2.currentPriority;
    }
}
