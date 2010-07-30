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
package jpcsp.HLE.kernel.managers;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ILLEGAL_ATTR;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ILLEGAL_MEMSIZE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ILLEGAL_MEMBLOCK;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NOT_FOUND_FPOOL;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NO_MEMORY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_CAN_NOT_WAIT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_FPL;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_High;

import java.util.HashMap;
import java.util.Iterator;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelFplInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.util.Utilities;

public class FplManager {

    private HashMap<Integer, SceKernelFplInfo> fplMap;
    private FplWaitStateChecker fplWaitStateChecker;

    private final static int PSP_FPL_ATTR_FIFO = 0;
    private final static int PSP_FPL_ATTR_PRIORITY = 0x100;
    private final static int PSP_FPL_ATTR_MASK = 0x41FF;            // Anything outside this mask is an illegal attr.
    private final static int PSP_FPL_ATTR_ADDR_HIGH = 0x4000;       // Create the fpl in high memory.

    public void reset() {
        fplMap = new HashMap<Integer, SceKernelFplInfo>();
        fplWaitStateChecker = new FplWaitStateChecker();
    }

    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        // Untrack
        thread.wait.waitingOnEventFlag = false;
        // Update numWaitThreads
        SceKernelFplInfo fpl = fplMap.get(thread.wait.Fpl_id);
        if (fpl != null) {
            fpl.numWaitThreads--;
            if (fpl.numWaitThreads < 0) {
                Modules.log.warn("removing waiting thread " + Integer.toHexString(thread.uid)
                    + ", fpl " + Integer.toHexString(fpl.uid) + " numWaitThreads underflowed");
                fpl.numWaitThreads = 0;
            }
            return true;
        }
        return false;
    }

    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return WAIT_TIMEOUT
            thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
        } else {
            Modules.log.warn("FPL deleted while we were waiting for it! (timeout expired)");
            // Return WAIT_DELETE
            thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
        }
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        if (thread.wait.waitingOnFpl) {
            removeWaitingThread(thread);
        }
    }

    private void onFplDeletedCancelled(int fid, int result) {
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();
            if (thread.waitType == PSP_WAIT_FPL &&
                thread.wait.waitingOnFpl &&
                thread.wait.Fpl_id == fid) {
                // FPL was deleted
                Modules.log.warn("FPL deleted while we were waiting for it! thread:" + Integer.toHexString(thread.uid) + "/'" + thread.name + "'");
                // Untrack
                thread.wait.waitingOnFpl = false;
                // Return ERROR_WAIT_DELETE / ERROR_WAIT_CANCELLED
                thread.cpuContext.gpr[2] = result;
                // Wakeup
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
            }
        }
        threadMan.hleRescheduleCurrentThread();
    }

    private void onFplDeleted(int fid) {
        onFplDeletedCancelled(fid, ERROR_WAIT_DELETE);
    }

    private void onFplCancelled(int fid) {
        onFplDeletedCancelled(fid, ERROR_WAIT_CANCELLED);
    }

    private void onFplFree(SceKernelFplInfo info) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        if ((info.attr & PSP_FPL_ATTR_FIFO) == PSP_FPL_ATTR_FIFO) {
            for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_FPL &&
                        thread.wait.waitingOnFpl &&
                        thread.wait.Fpl_id == info.uid) {
                    if (Modules.log.isDebugEnabled()) {
                        Modules.log.debug(String.format("onFplFree waking thread %s", thread.toString()));
                    }
                    info.numWaitThreads--;
                    thread.wait.waitingOnFpl = false;
                    thread.cpuContext.gpr[2] = 0;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
        } else if ((info.attr & PSP_FPL_ATTR_PRIORITY) == PSP_FPL_ATTR_PRIORITY) {
            for (Iterator<SceKernelThreadInfo> it = threadMan.iteratorByPriority(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_FPL &&
                        thread.wait.waitingOnFpl &&
                        thread.wait.Fpl_id == info.uid) {
                    if (Modules.log.isDebugEnabled()) {
                        Modules.log.debug(String.format("onFplFree waking thread %s", thread.toString()));
                    }
                    info.numWaitThreads--;
                    thread.wait.waitingOnFpl = false;
                    thread.cpuContext.gpr[2] = 0;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
        }
    }

    /** @return the address of the allocated block or 0 if failed. */
    private int tryAllocateFpl(SceKernelFplInfo info) {
        int block;
        int addr = 0;

        if (info.freeBlocks == 0 || (block = info.findFreeBlock()) == -1) {
            Modules.log.warn("tryAllocateFpl no free blocks (numBlocks=" + info.numBlocks + ")");
            return 0;
        }
		addr = info.allocateBlock(block);

        return addr;
    }

    public void sceKernelCreateFpl(int name_addr, int partitionid, int attr, int blocksize, int blocks, int opt_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        String name = Utilities.readStringZ(name_addr);
        Modules.log.debug("sceKernelCreateFpl(name='" + name
            + "',partition=" + partitionid
            + ",attr=0x" + Integer.toHexString(attr)
            + ",blocksize=0x" + Integer.toHexString(blocksize)
            + ",blocks=" + blocks
            + ",opt=0x" + Integer.toHexString(opt_addr) + ")");

        if (mem.isAddressGood(opt_addr)) {
            int optsize = mem.read32(opt_addr);
            Modules.log.warn("UNIMPLEMENTED:sceKernelCreateFpl option at 0x" + Integer.toHexString(opt_addr)
                + " (size=" + optsize + ")");
            // [1] = 0x40 phantasy star
            for (int i = 4; i < optsize && i < 200; i += 4) {
                Modules.log.debug(String.format("opt[%d] = %08X", (i / 4), mem.read32(opt_addr + i)));
            }
        }

        int memType = PSP_SMEM_Low;
        if((attr & PSP_FPL_ATTR_ADDR_HIGH) == PSP_FPL_ATTR_ADDR_HIGH) {
            memType = PSP_SMEM_High;
        }

        if ((attr & ~PSP_FPL_ATTR_MASK) != 0) {
            Modules.log.warn("sceKernelCreateFpl bad attr value 0x" + Integer.toHexString(attr));
            cpu.gpr[2] = ERROR_ILLEGAL_ATTR;
        } else if (blocksize == 0) {
            Modules.log.warn("sceKernelCreateFpl bad blocksize, cannot be 0");
            cpu.gpr[2] = ERROR_ILLEGAL_MEMSIZE;
        } else {
            SceKernelFplInfo info = SceKernelFplInfo.tryCreateFpl(name, partitionid, attr, blocksize, blocks, memType);
            if (info != null) {
                Modules.log.debug("sceKernelCreateFpl '" + name + "' assigned uid " + Integer.toHexString(info.uid));
                fplMap.put(info.uid, info);
                cpu.gpr[2] = info.uid;
            } else {
                cpu.gpr[2] = ERROR_NO_MEMORY;
            }
        }
    }

    public void sceKernelDeleteFpl(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;
        String msg = "sceKernelDeleteFpl(uid=0x" + Integer.toHexString(uid) + ")";

        SceKernelFplInfo info = fplMap.remove(uid);
        if (info == null) {
            Modules.log.warn(msg + " unknown uid");
            cpu.gpr[2] = ERROR_NOT_FOUND_FPOOL;
        } else {
            msg += " '" + info.name + "'";
            Modules.log.debug(msg);

            if (info.freeBlocks < info.numBlocks) {
                Modules.log.warn(msg + " " + (info.numBlocks - info.freeBlocks) + " unfreed blocks, continuing");
            }
            info.deleteSysMemInfo();

            cpu.gpr[2] = 0;
            onFplDeleted(uid);
        }
    }

    private void hleKernelAllocateFpl(int uid, int data_addr, int timeout_addr, boolean wait, boolean doCallbacks) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        Modules.log.debug("hleKernelAllocateFpl uid=0x" + Integer.toHexString(uid)
                + " data_addr=0x" + Integer.toHexString(data_addr)
                + " timeout_addr=0x" + Integer.toHexString(timeout_addr)
                + " callbacks=" + doCallbacks);

        SceUidManager.checkUidPurpose(uid, "ThreadMan-Fpl", true);
        SceKernelFplInfo fpl = fplMap.get(uid);
        if (fpl == null) {
            Modules.log.warn("hleKernelAllocateFpl unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_FPOOL;
        } else {
            int addr = tryAllocateFpl(fpl);
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            int micros = 0;
            if (mem.isAddressGood(timeout_addr)) {
                micros = mem.read32(timeout_addr);
            }
            if (addr == 0) {
                Modules.log.debug("hleKernelAllocateFpl fast check failed");
                if (wait) {
                    fpl.numWaitThreads++;
                    // Go to wait state
                    SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
                    currentThread.waitType = PSP_WAIT_FPL;
                    currentThread.waitId = uid;
                    // Wait on a specific fpl
                    threadMan.hleKernelThreadWait(currentThread, micros, (timeout_addr == 0));

                    currentThread.wait.waitingOnFpl = true;
                    currentThread.wait.Fpl_id = uid;
                    currentThread.wait.Fpl_dataAddr = data_addr;
                    currentThread.wait.waitStateChecker = fplWaitStateChecker;

                    threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
                    cpu.gpr[2] = ERROR_WAIT_CAN_NOT_WAIT;
                } else {
                    cpu.gpr[2] = 0;
                }
            } else {
                Modules.log.debug("hleKernelAllocateFpl fast check succeeded");
                mem.write32(data_addr, addr);
                cpu.gpr[2] = 0;
            }
            threadMan.hleRescheduleCurrentThread(doCallbacks);
        }
    }

    public void sceKernelAllocateFpl(int uid, int data_addr, int timeout_addr) {
        Modules.log.debug("sceKernelAllocateFpl redirecting to hleKernelAllocateFpl(callbacks=false)");
        hleKernelAllocateFpl(uid, data_addr, timeout_addr, true, false);
    }

    public void sceKernelAllocateFplCB(int uid, int data_addr, int timeout_addr) {
        Modules.log.debug("sceKernelAllocateFplCB redirecting to hleKernelAllocateFpl(callbacks=true)");
        hleKernelAllocateFpl(uid, data_addr, timeout_addr, true, true);
    }

    public void sceKernelTryAllocateFpl(int uid, int data_addr) {
        Modules.log.debug("sceKernelTryAllocateFpl redirecting to hleKernelAllocateFpl");
        hleKernelAllocateFpl(uid, data_addr, 0, false, false);
    }

    public void sceKernelFreeFpl(int uid, int data_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.debug("sceKernelFreeFpl(uid=0x" + Integer.toHexString(uid)
            + ",data=0x" + Integer.toHexString(data_addr) + ")");

        SceKernelFplInfo info = fplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelFreeFpl unknown uid");
            cpu.gpr[2] = ERROR_NOT_FOUND_FPOOL;
        } else {
            int block = info.findBlockByAddress(data_addr);
            if (block == -1) {
                Modules.log.warn("sceKernelFreeFpl unknown block address=0x" + Integer.toHexString(data_addr));
                cpu.gpr[2] = ERROR_ILLEGAL_MEMBLOCK;
            } else {
                info.freeBlock(block);
                cpu.gpr[2] = 0;
                onFplFree(info);
            }
        }
    }

    public void sceKernelCancelFpl(int uid, int numWaitThreadAddr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.warn("sceKernelCancelFpl(uid=0x" + Integer.toHexString(uid)
            + ",numWaitThreadAddr=0x" + Integer.toHexString(numWaitThreadAddr) + ")");

        SceKernelFplInfo info = fplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelCancelFpl unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_FPOOL;
        } else {
            Memory mem = Memory.getInstance();
            if (mem.isAddressGood(numWaitThreadAddr)) {
                mem.write32(numWaitThreadAddr, info.numWaitThreads);
            }
            cpu.gpr[2] = 0;
            onFplCancelled(uid);
        }
    }

    public void sceKernelReferFplStatus(int uid, int info_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        Modules.log.debug("sceKernelReferFplStatus(uid=0x" + Integer.toHexString(uid)
            + ",info_addr=0x" + Integer.toHexString(info_addr) + ")");

        SceKernelFplInfo info = fplMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferFplStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_FPOOL;
        } else {
            info.write(mem, info_addr);
            cpu.gpr[2] = 0;
        }
    }

    private class FplWaitStateChecker implements IWaitStateChecker {
		@Override
		public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
			// Check if the thread has to continue its wait state or if the fpl
			// has been allocated during the callback execution.
			SceKernelFplInfo fpl = fplMap.get(wait.Fpl_id);
			if (fpl == null) {
	            thread.cpuContext.gpr[2] = ERROR_NOT_FOUND_FPOOL;
				return false;
			}

			// Check fpl
			if (tryAllocateFpl(fpl) != 0) {
				fpl.numWaitThreads--;
	            thread.cpuContext.gpr[2] = 0;
				return false;
			}

			return true;
		}
    }

    public static final FplManager singleton;

    private FplManager() {
    }

    static {
        singleton = new FplManager();
    }
}