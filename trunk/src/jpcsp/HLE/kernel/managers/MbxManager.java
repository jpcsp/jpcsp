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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MESSAGEBOX_NO_MESSAGE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NOT_FOUND_MESSAGE_BOX;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NO_MEMORY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_MBX;

import java.util.HashMap;
import java.util.Iterator;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelMbxInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.util.Utilities;

public class MbxManager {

    private HashMap<Integer, SceKernelMbxInfo> mbxMap;
    private MbxWaitStateChecker mbxWaitStateChecker;
    private final static int PSP_MBX_ATTR_FIFO = 0;
    private final static int PSP_MBX_ATTR_PRIORITY = 0x100;
    private final static int PSP_MBX_ATTR_MSG_FIFO = 0;           // Add new messages by FIFO.
    private final static int PSP_MBX_ATTR_MSG_PRIORITY = 0x400;   // Add new messages by MsgPacket priority.

    public void reset() {
        mbxMap = new HashMap<Integer, SceKernelMbxInfo>();
        mbxWaitStateChecker = new MbxWaitStateChecker();
    }

    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        thread.wait.waitingOnMbxReceive = false;
        SceKernelMbxInfo info = mbxMap.get(thread.wait.Mbx_id);
        if (info != null) {
            info.numWaitThreads--;
            if (info.numWaitThreads < 0) {
                Modules.log.warn("Removing waiting thread " + Integer.toHexString(thread.uid)
                        + ", Mbx " + Integer.toHexString(info.uid) + " numWaitThreads underflowed");
                info.numWaitThreads = 0;
            }
            return true;
        }
        return false;
    }

    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        if (removeWaitingThread(thread)) {
            thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
        } else {
            Modules.log.warn("Mbx deleted while we were waiting for it! (timeout expired)");
            thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
        }
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        removeWaitingThread(thread);
    }

    private void updateWaitingMbxReceive(SceKernelMbxInfo info) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        if ((info.attr & PSP_MBX_ATTR_FIFO) == PSP_MBX_ATTR_FIFO) {
            for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_MBX &&
                        thread.wait.waitingOnMbxReceive &&
                        thread.wait.Mbx_id == info.uid &&
                        info.hasMessage()) {
                    if (Modules.log.isDebugEnabled()) {
                        Modules.log.debug(String.format("updateWaitingMbxReceive waking thread %s", thread.toString()));
                    }
                    Memory mem = Memory.getInstance();
                    int msgAddr = info.removeMsg(mem);
                    mem.write32(thread.wait.Mbx_resultAddr, msgAddr);
                    info.numWaitThreads--;
                    thread.wait.waitingOnMbxReceive = false;
                    thread.cpuContext.gpr[2] = 0;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
        } else if ((info.attr & PSP_MBX_ATTR_PRIORITY) == PSP_MBX_ATTR_PRIORITY) {
            for (Iterator<SceKernelThreadInfo> it = threadMan.iteratorByPriority(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_MBX &&
                        thread.wait.waitingOnMbxReceive &&
                        thread.wait.Mbx_id == info.uid &&
                        info.hasMessage()) {
                    if (Modules.log.isDebugEnabled()) {
                        Modules.log.debug(String.format("updateWaitingMbxReceive waking thread %s", thread.toString()));
                    }
                    Memory mem = Memory.getInstance();
                    int msgAddr = info.removeMsg(mem);
                    mem.write32(thread.wait.Mbx_resultAddr, msgAddr);
                    info.numWaitThreads--;
                    thread.wait.waitingOnMbxReceive = false;
                    thread.cpuContext.gpr[2] = 0;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
        }
    }

    private void cancelWaitingMbxReceive(SceKernelMbxInfo info) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
            SceKernelThreadInfo thread = it.next();
            if ((thread.waitType == PSP_WAIT_MBX &&
                    thread.wait.waitingOnMbxReceive &&
                    thread.wait.Mbx_id == info.uid)) {
                thread.wait.waitingOnMbxReceive = false;
                thread.cpuContext.gpr[2] = ERROR_WAIT_CANCELLED;
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
            }
        }
        info.numWaitThreads = 0;
    }

    private void deleteWaitingMbxReceive(SceKernelMbxInfo info) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
            SceKernelThreadInfo thread = it.next();
            if ((thread.waitType == PSP_WAIT_MBX &&
                    thread.wait.waitingOnMbxReceive &&
                    thread.wait.Mbx_id == info.uid)) {
                thread.wait.waitingOnMbxReceive = false;
                thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
            }
        }
        info.numWaitThreads = 0;
    }

    public void sceKernelCreateMbx(int name_addr, int attr, int opt_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        String name = Utilities.readStringZ(name_addr);
        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug("sceKernelCreateMbx(name=" + name + ",attr=0x" + Integer.toHexString(attr) + ",opt=0x" + Integer.toHexString(opt_addr) + ")");
        }

        if (mem.isAddressGood(opt_addr)) {
            int optsize = mem.read32(opt_addr);
            Modules.log.warn("UNIMPLEMENTED:sceKernelCreateMbx option at 0x" + Integer.toHexString(opt_addr) + " (size=" + optsize + ")");
        }

        SceKernelMbxInfo info = new SceKernelMbxInfo(name, attr);
        if (info != null) {
            if (Modules.log.isDebugEnabled()) {
                Modules.log.debug("sceKernelCreateMbx '" + name + "' assigned uid " + Integer.toHexString(info.uid));
            }
            mbxMap.put(info.uid, info);
            cpu.gpr[2] = info.uid;
        } else {
            cpu.gpr[2] = ERROR_NO_MEMORY;
        }
    }

    public void sceKernelDeleteMbx(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug("sceKernelDeleteMbx(uid=0x" + Integer.toHexString(uid) + ")");
        }

        SceKernelMbxInfo info = mbxMap.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelDeleteMbx unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_BOX;
        } else {
            deleteWaitingMbxReceive(info);
            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelSendMbx(int uid, int msg_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug("sceKernelSendMbx(uid=0x" + Integer.toHexString(uid) + ",msg=0x" + Integer.toHexString(msg_addr) + ")");
        }

        SceKernelMbxInfo info = mbxMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelSendMbx unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_BOX;
        } else {
            if((info.attr & PSP_MBX_ATTR_MSG_FIFO) == PSP_MBX_ATTR_MSG_FIFO) {
                info.addMsg(mem, msg_addr);
            } else if((info.attr & PSP_MBX_ATTR_MSG_PRIORITY) == PSP_MBX_ATTR_MSG_PRIORITY) {
                info.addMsgByPriority(mem, msg_addr);
            }
            updateWaitingMbxReceive(info);
            cpu.gpr[2] = 0;
        }
    }

    private void hleKernelReceiveMbx(int uid, int addr_msg_addr, int timeout_addr,
            boolean doCallbacks, boolean poll) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int micros = 0;
        if (!poll && mem.isAddressGood(timeout_addr)) {
            micros = mem.read32(timeout_addr);
        }

        if (Modules.log.isDebugEnabled()) {
            String waitType = "";
            if (poll) {
                waitType = "poll";
            } else if (timeout_addr == 0) {
                waitType = "forever";
            } else {
                waitType = micros + " ms";
            }
            if (doCallbacks) {
                waitType += " + CB";
            }
            Modules.log.debug("hleKernelReceiveMbx(uid=0x" + Integer.toHexString(uid)
                    + ", msg_pointer=0x" + Integer.toHexString(addr_msg_addr)
                    + ", timeout=0x" + Integer.toHexString(timeout_addr) + ")" + " " + waitType);
        }

        SceKernelMbxInfo info = mbxMap.get(uid);
        if (info == null) {
            Modules.log.warn("hleKernelReceiveMbx unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_BOX;
        } else {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            if (!info.hasMessage()) {
                if (!poll) {
                    if (Modules.log.isDebugEnabled()) {
                        Modules.log.debug("hleKernelReceiveMbx - '" + info.name + "' (waiting)");
                    }
                    info.numWaitThreads++;
                    SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
                    currentThread.waitType = PSP_WAIT_MBX;
                    currentThread.waitId = uid;
                    threadMan.hleKernelThreadWait(currentThread, micros, (timeout_addr == 0));
                    currentThread.wait.waitingOnMbxReceive = true;
                    currentThread.wait.Mbx_id = uid;
                    currentThread.wait.Mbx_resultAddr = addr_msg_addr;
                    currentThread.wait.waitStateChecker = mbxWaitStateChecker;
                    threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
                } else {
                    if (Modules.log.isDebugEnabled()) {
                        Modules.log.debug("hleKernelReceiveMbx has no messages.");
                    }
                    cpu.gpr[2] = ERROR_MESSAGEBOX_NO_MESSAGE;
                }
            } else {
                int msgAddr = info.removeMsg(mem);
                mem.write32(addr_msg_addr, msgAddr);
                cpu.gpr[2] = 0;
            }
            threadMan.hleRescheduleCurrentThread(doCallbacks);
        }
    }

    public void sceKernelReceiveMbx(int uid, int addr_msg_addr, int timeout_addr) {
        hleKernelReceiveMbx(uid, addr_msg_addr, timeout_addr, false, false);
    }

    public void sceKernelReceiveMbxCB(int uid, int addr_msg_addr, int timeout_addr) {
        hleKernelReceiveMbx(uid, addr_msg_addr, timeout_addr, true, false);
    }

    public void sceKernelPollMbx(int uid, int addr_msg_addr) {
        hleKernelReceiveMbx(uid, addr_msg_addr, 0, false, true);
    }

    public void sceKernelCancelReceiveMbx(int uid, int pnum_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug("sceKernelCancelReceiveMbx(uid=0x" + Integer.toHexString(uid) + ")");
        }

        SceKernelMbxInfo info = mbxMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelCancelReceiveMbx unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_BOX;
        } else {
            if (mem.isAddressGood(pnum_addr)) {
                mem.write32(pnum_addr, info.numWaitThreads);
            }
            cancelWaitingMbxReceive(info);
            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelReferMbxStatus(int uid, int info_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        if (Modules.log.isDebugEnabled()) {
            Modules.log.debug("sceKernelReferMbxStatus(uid=0x" + Integer.toHexString(uid) + ",info=0x" + Integer.toHexString(info_addr) + ")");
        }

        SceKernelMbxInfo info = mbxMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferMbxStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_BOX;
        } else {
            int maxSize = mem.read32(info_addr);
            info.size = maxSize;
            info.write(mem, info_addr);
            cpu.gpr[2] = 0;
        }
    }

    private class MbxWaitStateChecker implements IWaitStateChecker {

        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the mbx
            // has received a new message during the callback execution.
            SceKernelMbxInfo info = mbxMap.get(wait.Mbx_id);
            if (info == null) {
                thread.cpuContext.gpr[2] = ERROR_NOT_FOUND_MESSAGE_BOX;
                return false;
            }

            // Check the mbx for a new message
            if (info.hasMessage()) {
                Memory mem = Memory.getInstance();
                int msgAddr = info.removeMsg(mem);
                mem.write32(wait.Mbx_resultAddr, msgAddr);
                info.numWaitThreads--;
                thread.cpuContext.gpr[2] = 0;
                return false;
            }

            return true;
        }
    }
    public static final MbxManager singleton;

    private MbxManager() {
    }

    static {
        singleton = new MbxManager();
    }
}