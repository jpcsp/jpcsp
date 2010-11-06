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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ILLEGAL_SIZE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MESSAGE_PIPE_EMPTY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MESSAGE_PIPE_FULL;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NOT_FOUND_MESSAGE_PIPE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NO_MEMORY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_MSGPIPE;
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
import jpcsp.HLE.kernel.types.SceKernelMppInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class MsgPipeManager {

    protected static Logger log = Modules.getLogger("ThreadManForUser");

    private HashMap<Integer, SceKernelMppInfo> msgMap;
    private MsgPipeSendWaitStateChecker msgPipeSendWaitStateChecker;
    private MsgPipeReceiveWaitStateChecker msgPipeReceiveWaitStateChecker;

    private static final int PSP_MPP_ATTR_SEND_FIFO = 0;
    private static final int PSP_MPP_ATTR_SEND_PRIORITY = 0x100;
    private static final int PSP_MPP_ATTR_RECEIVE_FIFO = 0;
    private static final int PSP_MPP_ATTR_RECEIVE_PRIORITY = 0x1000;
    private static final int PSP_MPP_ATTR_SEND = PSP_MPP_ATTR_SEND_FIFO | PSP_MPP_ATTR_SEND_PRIORITY;
    private static final int PSP_MPP_ATTR_RECEIVE = PSP_MPP_ATTR_RECEIVE_FIFO | PSP_MPP_ATTR_RECEIVE_PRIORITY;
    private final static int PSP_MPP_ATTR_ADDR_HIGH = 0x4000;

    protected static final int PSP_MPP_WAIT_MODE_COMPLETE = 0; // receive always a complete buffer
    protected static final int PSP_MPP_WAIT_MODE_PARTIAL = 1;  // can receive a partial buffer

    public void reset() {
        msgMap = new HashMap<Integer, SceKernelMppInfo>();
        msgPipeSendWaitStateChecker = new MsgPipeSendWaitStateChecker();
        msgPipeReceiveWaitStateChecker = new MsgPipeReceiveWaitStateChecker();
    }

    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        if (thread.wait.waitingOnMsgPipeSend) {
            // Untrack
            thread.wait.waitingOnMsgPipeSend = false;
            // Update numSendWaitThreads.
            SceKernelMppInfo info = msgMap.get(thread.wait.MsgPipe_id);
            if (info != null) {
                info.numSendWaitThreads--;
                if (info.numSendWaitThreads < 0) {
                    log.warn("removing waiting thread " + Integer.toHexString(thread.uid) + ", MsgPipe " + Integer.toHexString(info.uid) + " numSendWaitThreads underflowed");
                    info.numSendWaitThreads = 0;
                }
                return true;
            }
        } else if (thread.wait.waitingOnMsgPipeSend) {
            // Untrack
            thread.wait.waitingOnMsgPipeReceive = false;
            // Update numReceiveWaitThreads.
            SceKernelMppInfo info = msgMap.get(thread.wait.MsgPipe_id);
            if (info != null) {
                info.numReceiveWaitThreads--;
                if (info.numReceiveWaitThreads < 0) {
                    log.warn("removing waiting thread " + Integer.toHexString(thread.uid) + ", MsgPipe " + Integer.toHexString(info.uid) + " numReceiveWaitThreads underflowed");
                    info.numReceiveWaitThreads = 0;
                }
                return true;
            }
        }
        return false;
    }

    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return WAIT_TIMEOUT
            thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
        } else {
            log.warn("MsgPipe deleted while we were waiting for it! (timeout expired)");
            // Return WAIT_DELETE
            thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
        }
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        removeWaitingThread(thread);
    }

    private void updateWaitingMsgPipeSend(SceKernelMppInfo info) {
        Memory mem = Memory.getInstance();
        // Find threads waiting on this XXX and wake them up.
        if ((info.attr & PSP_MPP_ATTR_SEND) == PSP_MPP_ATTR_SEND_FIFO) {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_MSGPIPE &&
                        thread.wait.waitingOnMsgPipeSend &&
                        thread.wait.MsgPipe_id == info.uid &&
                        trySendMsgPipe(mem, info, thread.wait.MsgPipe_address, thread.wait.MsgPipe_size, thread.wait.MsgPipe_waitMode, thread.wait.MsgPipe_resultSize_addr)) {
                    // Untrack.
                    thread.wait.waitingOnMsgPipeSend = false;
                    // Adjust waiting threads.
                    info.numSendWaitThreads--;
                    // Return success.
                    thread.cpuContext.gpr[2] = 0;
                    // Wakeup.
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
        } else if ((info.attr & PSP_MPP_ATTR_SEND) == PSP_MPP_ATTR_SEND_PRIORITY) {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            for (Iterator<SceKernelThreadInfo> it = threadMan.iteratorByPriority(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_MSGPIPE &&
                        thread.wait.waitingOnMsgPipeSend &&
                        thread.wait.MsgPipe_id == info.uid &&
                        trySendMsgPipe(mem, info, thread.wait.MsgPipe_address, thread.wait.MsgPipe_size, thread.wait.MsgPipe_waitMode, thread.wait.MsgPipe_resultSize_addr)) {
                    // Untrack.
                    thread.wait.waitingOnMsgPipeSend = false;
                    // Adjust waiting threads.
                    info.numSendWaitThreads--;
                    // Return success.
                    thread.cpuContext.gpr[2] = 0;
                    // Wakeup.
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
        }
    }

    private void updateWaitingMsgPipeReceive(SceKernelMppInfo info) {
        Memory mem = Memory.getInstance();
        // Find threads waiting on this XXX and wake them up.
        if ((info.attr & PSP_MPP_ATTR_RECEIVE) == PSP_MPP_ATTR_RECEIVE_FIFO) {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_MSGPIPE &&
                        thread.wait.waitingOnMsgPipeReceive &&
                        thread.wait.MsgPipe_id == info.uid &&
                        tryReceiveMsgPipe(mem, info, thread.wait.MsgPipe_address, thread.wait.MsgPipe_size, thread.wait.MsgPipe_waitMode, thread.wait.MsgPipe_resultSize_addr)) {
                    // Untrack.
                    thread.wait.waitingOnMsgPipeReceive = false;
                    // Adjust waiting threads.
                    info.numReceiveWaitThreads--;
                    // Return success.
                    thread.cpuContext.gpr[2] = 0;
                    // Wakeup.
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
        } else if ((info.attr & PSP_MPP_ATTR_RECEIVE) == PSP_MPP_ATTR_RECEIVE_PRIORITY) {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            for (Iterator<SceKernelThreadInfo> it = threadMan.iteratorByPriority(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if (thread.waitType == PSP_WAIT_MSGPIPE &&
                        thread.wait.waitingOnMsgPipeReceive &&
                        thread.wait.MsgPipe_id == info.uid &&
                        tryReceiveMsgPipe(mem, info, thread.wait.MsgPipe_address, thread.wait.MsgPipe_size, thread.wait.MsgPipe_waitMode, thread.wait.MsgPipe_resultSize_addr)) {
                    // Untrack.
                    thread.wait.waitingOnMsgPipeReceive = false;
                    // Adjust waiting threads.
                    info.numReceiveWaitThreads--;
                    // Return success.
                    thread.cpuContext.gpr[2] = 0;
                    // Wakeup.
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
        }
    }

    private boolean trySendMsgPipe(Memory mem, SceKernelMppInfo info, int addr, int size, int waitMode, int resultSize_addr) {
        if (size > 0) {
            int availableSize = info.availableWriteSize();
            if (availableSize == 0) {
                return false;
            }
            // Trying to send more than available?
            if (size > availableSize) {
                // Do we need to send the complete size?
                if (waitMode == PSP_MPP_WAIT_MODE_COMPLETE) {
                    return false;
                }
                // We can just send the available size.
                size = availableSize;
            }
        }
        info.append(mem, addr, size);
        if (Memory.isAddressGood(resultSize_addr)) {
            mem.write32(resultSize_addr, size);
        }
        return true;
    }

    private boolean tryReceiveMsgPipe(Memory mem, SceKernelMppInfo info, int addr, int size, int waitMode, int resultSize_addr) {
        if (size > 0) {
            int availableSize = info.availableReadSize();
            if (availableSize == 0) {
                return false;
            }
            // Trying to receive more than available?
            if (size > availableSize) {
                // Do we need to receive the complete size?
                if (waitMode == PSP_MPP_WAIT_MODE_COMPLETE) {
                    return false;
                }
                // We can just receive the available size.
                size = availableSize;
            }
        }
        if (Memory.isAddressGood(resultSize_addr)) {
            mem.write32(resultSize_addr, size);
        }
        info.consume(mem, addr, size);
        return true;
    }

    public void sceKernelCreateMsgPipe(int name_addr, int partitionid, int attr, int size, int opt_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        String name = Utilities.readStringZ(name_addr);
        if (log.isDebugEnabled()) {
            log.debug("sceKernelCreateMsgPipe(name=" + name + ", partition=" + partitionid + ", attr=0x" + Integer.toHexString(attr) + ", size=0x" + Integer.toHexString(size) + ", opt=0x" + Integer.toHexString(opt_addr) + ")");
        }

        if (Memory.isAddressGood(opt_addr)) {
            int optsize = mem.read32(opt_addr);
            log.warn("sceKernelCreateMsgPipe option at 0x" + Integer.toHexString(opt_addr) + " (size=" + optsize + ")");
        }

        int memType = PSP_SMEM_Low;
        if ((attr & PSP_MPP_ATTR_ADDR_HIGH) == PSP_MPP_ATTR_ADDR_HIGH) {
            memType = PSP_SMEM_High;
        }

        SceKernelMppInfo info = SceKernelMppInfo.tryCreateMpp(name, partitionid, attr, size, memType);
        if (info != null) {
            if (log.isDebugEnabled()) {
                log.debug("sceKernelCreateMsgPipe '" + name + "' assigned uid " + Integer.toHexString(info.uid));
            }
            msgMap.put(info.uid, info);
            cpu.gpr[2] = info.uid;
        } else {
            cpu.gpr[2] = ERROR_NO_MEMORY;
        }
    }

    public void sceKernelDeleteMsgPipe(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelDeleteMsgPipe(uid=0x" + Integer.toHexString(uid) + ")");
        }

        SceKernelMppInfo info = msgMap.remove(uid);
        if (info == null) {
            log.warn("sceKernelDeleteMsgPipe unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_PIPE;
        } else {
            info.deleteSysMemInfo();
            // Find threads waiting on this XXX and wake them up.
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if ((thread.wait.waitingOnMsgPipeSend || thread.wait.waitingOnMsgPipeReceive) &&
                        thread.wait.MsgPipe_id == uid) {
                    // Untrack.
                    thread.wait.waitingOnMsgPipeSend = false;
                    thread.wait.waitingOnMsgPipeReceive = false;
                    // Return WAIT_DELETE.
                    thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
                    // Wakeup.
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
            cpu.gpr[2] = 0;
        }
    }

    private void hleKernelSendMsgPipe(int uid, int msg_addr, int size, int waitMode, int resultSize_addr, int timeout_addr,
            boolean doCallbacks, boolean poll) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int micros = 0;
        if (!poll && Memory.isAddressGood(timeout_addr)) {
            micros = mem.read32(timeout_addr);
        }

        if (log.isDebugEnabled()) {
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
            log.debug("hleKernelSendMsgPipe(uid=0x" + Integer.toHexString(uid) + ", msg=0x" + Integer.toHexString(msg_addr) + " ,size=0x" + Integer.toHexString(size) + " ,waitMode=0x" + Integer.toHexString(waitMode) + " ,resultSize_addr=0x" + Integer.toHexString(resultSize_addr) + " ,timeout=0x" + Integer.toHexString(timeout_addr) + ")" + " " + waitType);
        }

        SceKernelMppInfo info = msgMap.get(uid);
        if (info == null) {
            log.warn("hleKernelSendMsgPipe unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_PIPE;
        } else if (size > info.bufSize) {
            log.warn("hleKernelSendMsgPipe illegal size 0x" + Integer.toHexString(size) + " max 0x" + Integer.toHexString(info.bufSize));
            cpu.gpr[2] = ERROR_ILLEGAL_SIZE;
        } else {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            if (!trySendMsgPipe(mem, info, msg_addr, size, waitMode, resultSize_addr)) {
                if (!poll) {
                    // Failed, but it's ok, just wait a little
                    if (log.isDebugEnabled()) {
                        log.debug("hleKernelSendMsgPipe - '" + info.name + "' waiting for " + size + " bytes to become available");
                    }
                    info.numSendWaitThreads++;
                    SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
                    // Wait type.
                    currentThread.waitType = PSP_WAIT_MSGPIPE;
                    currentThread.waitId = uid;
                    // Go to wait state.
                    threadMan.hleKernelThreadWait(currentThread, micros, (timeout_addr == 0));
                    // Wait on a specific XXX.
                    currentThread.wait.waitingOnMsgPipeSend = true;
                    currentThread.wait.MsgPipe_id = uid;
                    currentThread.wait.MsgPipe_address = msg_addr;
                    currentThread.wait.MsgPipe_size = size;
                    currentThread.wait.waitStateChecker = msgPipeSendWaitStateChecker;
                    threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
                } else {
                    log.warn("hleKernelSendMsgPipe illegal size 0x" + Integer.toHexString(size) + " max 0x" + Integer.toHexString(info.freeSize) + " (pipe needs consuming)");
                    cpu.gpr[2] = ERROR_MESSAGE_PIPE_FULL;
                }
            } else {
                cpu.gpr[2] = 0;
                updateWaitingMsgPipeReceive(info);
            }
            threadMan.hleRescheduleCurrentThread(doCallbacks);
        }
    }

    public void sceKernelSendMsgPipe(int uid, int msg_addr, int size, int waitMode, int resultSize_addr, int timeout_addr) {
        hleKernelSendMsgPipe(uid, msg_addr, size, waitMode, resultSize_addr, timeout_addr, false, false);
    }

    public void sceKernelSendMsgPipeCB(int uid, int msg_addr, int size, int waitMode, int resultSize_addr, int timeout_addr) {
        hleKernelSendMsgPipe(uid, msg_addr, size, waitMode, resultSize_addr, timeout_addr, true, false);
    }

    public void sceKernelTrySendMsgPipe(int uid, int msg_addr, int size, int waitMode, int resultSize_addr) {
        hleKernelSendMsgPipe(uid, msg_addr, size, waitMode, resultSize_addr, 0, false, true);
    }

    private void hleKernelReceiveMsgPipe(int uid, int msg_addr, int size, int waitMode, int resultSize_addr, int timeout_addr,
            boolean doCallbacks, boolean poll) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int micros = 0;
        if (!poll && Memory.isAddressGood(timeout_addr)) {
            micros = mem.read32(timeout_addr);
        }

        if (log.isDebugEnabled()) {
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
            log.debug("hleKernelReceiveMsgPipe(uid=0x" + Integer.toHexString(uid) + ", msg=0x" + Integer.toHexString(msg_addr) + ", size=0x" + Integer.toHexString(size) + " ,waitMode=0x" + Integer.toHexString(waitMode) + ", resultSize_addr=0x" + Integer.toHexString(resultSize_addr) + ", timeout=0x" + Integer.toHexString(timeout_addr) + ")" + " " + waitType);
        }

        SceKernelMppInfo info = msgMap.get(uid);
        if (info == null) {
            log.warn("hleKernelReceiveMsgPipe unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_PIPE;
        } else if (size > info.bufSize) {
            log.warn("hleKernelReceiveMsgPipe illegal size 0x" + Integer.toHexString(size) + " max 0x" + Integer.toHexString(info.bufSize));
            cpu.gpr[2] = ERROR_ILLEGAL_SIZE;
        } else {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            if (!tryReceiveMsgPipe(mem, info, msg_addr, size, waitMode, resultSize_addr)) {
                if (!poll) {
                    // Failed, but it's ok, just wait a little
                    if (log.isDebugEnabled()) {
                        log.debug("hleKernelReceiveMsgPipe - '" + info.name + "' waiting for " + size + " bytes to become available");
                    }
                    info.numReceiveWaitThreads++;
                    SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
                    // Wait type.
                    currentThread.waitType = PSP_WAIT_MSGPIPE;
                    currentThread.waitId = uid;
                    // Go to wait state.
                    threadMan.hleKernelThreadWait(currentThread, micros, (timeout_addr == 0));
                    // Wait on a specific XXX.
                    currentThread.wait.waitingOnMsgPipeReceive = true;
                    currentThread.wait.MsgPipe_id = uid;
                    currentThread.wait.MsgPipe_address = msg_addr;
                    currentThread.wait.MsgPipe_size = size;
                    currentThread.wait.MsgPipe_resultSize_addr = resultSize_addr;
                    currentThread.wait.waitStateChecker = msgPipeReceiveWaitStateChecker;
                    threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("hleKernelReceiveMsgPipe trying to read more than is available size 0x" + Integer.toHexString(size) + " available 0x" + Integer.toHexString(info.bufSize - info.freeSize));
                    }
                    cpu.gpr[2] = ERROR_MESSAGE_PIPE_EMPTY;
                }
            } else {
                cpu.gpr[2] = 0;
                updateWaitingMsgPipeSend(info);
            }
            threadMan.hleRescheduleCurrentThread(doCallbacks);
        }
    }

    public void sceKernelReceiveMsgPipe(int uid, int msg_addr, int size, int waitMode, int resultSize_addr, int timeout_addr) {
        hleKernelReceiveMsgPipe(uid, msg_addr, size, waitMode, resultSize_addr, timeout_addr, false, false);
    }

    public void sceKernelReceiveMsgPipeCB(int uid, int msg_addr, int size, int waitMode, int resultSize_addr, int timeout_addr) {
        hleKernelReceiveMsgPipe(uid, msg_addr, size, waitMode, resultSize_addr, timeout_addr, true, false);
    }

    public void sceKernelTryReceiveMsgPipe(int uid, int msg_addr, int size, int waitMode, int resultSize_addr) {
        hleKernelReceiveMsgPipe(uid, msg_addr, size, waitMode, resultSize_addr, 0, false, true);
    }

    public void sceKernelCancelMsgPipe(int uid, int send_addr, int recv_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Emulator.getMemory();

        if (log.isDebugEnabled()) {
            log.debug("sceKernelCancelMsgPipe(uid=0x" + Integer.toHexString(uid) + ", send=0x" + Integer.toHexString(send_addr) + ", recv=0x" + Integer.toHexString(recv_addr) + ")");
        }

        SceKernelMppInfo info = msgMap.get(uid);
        if (info == null) {
            log.warn("sceKernelCancelMsgPipe unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_PIPE;
        } else {
            if (Memory.isAddressGood(send_addr)) {
                mem.write32(send_addr, info.numSendWaitThreads);
            }
            if (Memory.isAddressGood(recv_addr)) {
                mem.write32(recv_addr, info.numReceiveWaitThreads);
            }
            info.numSendWaitThreads = 0;
            info.numReceiveWaitThreads = 0;
            // Find threads waiting on this XXX and wake them up.
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
                SceKernelThreadInfo thread = it.next();
                if ((thread.wait.waitingOnMsgPipeSend || thread.wait.waitingOnMsgPipeReceive) &&
                        thread.wait.MsgPipe_id == uid) {
                    // Untrack.
                    thread.wait.waitingOnMsgPipeSend = false;
                    thread.wait.waitingOnMsgPipeReceive = false;
                    // Return WAIT_CANCELLED.
                    thread.cpuContext.gpr[2] = ERROR_WAIT_CANCELLED;
                    // Wakeup.
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                }
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelReferMsgPipeStatus(int uid, int info_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferMsgPipeStatus(uid=0x" + Integer.toHexString(uid) + ",info=0x" + Integer.toHexString(info_addr) + ")");
        }

        SceKernelMppInfo info = msgMap.get(uid);
        if (info == null) {
            log.warn("sceKernelReferMsgPipeStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_PIPE;
        } else {
            info.write(mem, info_addr);
            cpu.gpr[2] = 0;
        }
    }

    private class MsgPipeSendWaitStateChecker implements IWaitStateChecker {

        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the msgpipe
            // has received a new message during the callback execution.
            SceKernelMppInfo info = msgMap.get(wait.MsgPipe_id);
            if (info == null) {
                thread.cpuContext.gpr[2] = ERROR_NOT_FOUND_MESSAGE_PIPE;
                return false;
            }

            Memory mem = Memory.getInstance();
            if (trySendMsgPipe(mem, info, thread.wait.MsgPipe_address, thread.wait.MsgPipe_size, thread.wait.MsgPipe_waitMode, thread.wait.MsgPipe_resultSize_addr)) {
                info.numSendWaitThreads--;
                thread.cpuContext.gpr[2] = 0;
                return false;
            }

            return true;
        }
    }

    private class MsgPipeReceiveWaitStateChecker implements IWaitStateChecker {

        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the msgpipe
            // has been sent a new message during the callback execution.
            SceKernelMppInfo info = msgMap.get(wait.MsgPipe_id);
            if (info == null) {
                thread.cpuContext.gpr[2] = ERROR_NOT_FOUND_MESSAGE_PIPE;
                return false;
            }

            Memory mem = Memory.getInstance();
            if (tryReceiveMsgPipe(mem, info, wait.MsgPipe_address, wait.MsgPipe_size, wait.MsgPipe_waitMode, wait.MsgPipe_resultSize_addr)) {
                info.numReceiveWaitThreads--;
                thread.cpuContext.gpr[2] = 0;
                return false;
            }

            return true;
        }
    }
    public static final MsgPipeManager singleton;

    private MsgPipeManager() {
    }


    static {
        singleton = new MsgPipeManager();
    }
}