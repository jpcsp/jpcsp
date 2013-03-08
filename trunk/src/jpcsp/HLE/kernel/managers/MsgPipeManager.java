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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_SIZE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_MESSAGE_PIPE_EMPTY;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_MESSAGE_PIPE_FULL;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_MESSAGE_PIPE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_STATUS_RELEASED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_MSGPIPE;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_High;

import java.util.HashMap;
import java.util.Iterator;

import jpcsp.HLE.Modules;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelMppInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;

import org.apache.log4j.Logger;

public class MsgPipeManager {
    protected static Logger log = Modules.getLogger("ThreadManForUser");

    private HashMap<Integer, SceKernelMppInfo> msgMap;
    private MsgPipeSendWaitStateChecker msgPipeSendWaitStateChecker;
    private MsgPipeReceiveWaitStateChecker msgPipeReceiveWaitStateChecker;

    public static final int PSP_MPP_ATTR_SEND_FIFO = 0;
    public static final int PSP_MPP_ATTR_SEND_PRIORITY = 0x100;
    public static final int PSP_MPP_ATTR_RECEIVE_FIFO = 0;
    public static final int PSP_MPP_ATTR_RECEIVE_PRIORITY = 0x1000;
    private final static int PSP_MPP_ATTR_ADDR_HIGH = 0x4000;

    protected static final int PSP_MPP_WAIT_MODE_COMPLETE = 0; // receive always a complete buffer
    protected static final int PSP_MPP_WAIT_MODE_PARTIAL = 1;  // can receive a partial buffer

    public void reset() {
        msgMap = new HashMap<Integer, SceKernelMppInfo>();
        msgPipeSendWaitStateChecker = new MsgPipeSendWaitStateChecker();
        msgPipeReceiveWaitStateChecker = new MsgPipeReceiveWaitStateChecker();
    }

    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        SceKernelMppInfo info = msgMap.get(thread.wait.MsgPipe_id);
        if (info == null) {
        	return false;
        }

        if (thread.wait.MsgPipe_isSend) {
        	info.sendThreadWaitingList.removeWaitingThread(thread);
    	} else {
    		info.receiveThreadWaitingList.removeWaitingThread(thread);
    	}

    	return true;
    }

    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return WAIT_TIMEOUT
            thread.cpuContext._v0 = ERROR_KERNEL_WAIT_TIMEOUT;
        } else {
            log.warn("MsgPipe deleted while we were waiting for it! (timeout expired)");
            // Return WAIT_DELETE
            thread.cpuContext._v0 = ERROR_KERNEL_WAIT_DELETE;
        }
    }

    public void onThreadWaitReleased(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return ERROR_WAIT_STATUS_RELEASED
            thread.cpuContext._v0 = ERROR_KERNEL_WAIT_STATUS_RELEASED;
        } else {
            log.warn("EventFlag deleted while we were waiting for it!");
            // Return WAIT_DELETE
            thread.cpuContext._v0 = ERROR_KERNEL_WAIT_DELETE;
        }
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        removeWaitingThread(thread);
    }

    private void onMsgPipeDeletedCancelled(int msgpid, int result) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
            SceKernelThreadInfo thread = it.next();
            if (thread.isWaitingFor(PSP_WAIT_MSGPIPE, msgpid)) {
                thread.cpuContext._v0 = result;
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                reschedule = true;
            }
        }
        // Reschedule only if threads waked up.
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    private void onMsgPipeDeleted(int msgpid) {
        onMsgPipeDeletedCancelled(msgpid, ERROR_KERNEL_WAIT_DELETE);
    }

    private void onMsgPipeCancelled(int msgpid) {
        onMsgPipeDeletedCancelled(msgpid, ERROR_KERNEL_WAIT_CANCELLED);
    }

    private void onMsgPipeSendModified(SceKernelMppInfo info) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        SceKernelThreadInfo checkedThread = null;
        while (true) {
            SceKernelThreadInfo thread = info.sendThreadWaitingList.getNextWaitingThread(checkedThread);
            if (thread == null) {
            	break;
            }
            if (thread.wait.MsgPipe_isSend && trySendMsgPipe(info, thread.wait.MsgPipe_address, thread.wait.MsgPipe_size, thread.wait.MsgPipe_waitMode, thread.wait.MsgPipe_resultSize_addr)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("onMsgPipeSendModified waking thread %s", thread));
                }
                info.sendThreadWaitingList.removeWaitingThread(thread);
                thread.cpuContext._v0 = 0;
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                reschedule = true;
            } else {
            	checkedThread = thread;
            }
        }

        // Reschedule only if threads waked up.
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    private void onMsgPipeReceiveModified(SceKernelMppInfo info) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        SceKernelThreadInfo checkedThread = null;
        while (true) {
            SceKernelThreadInfo thread = info.receiveThreadWaitingList.getNextWaitingThread(checkedThread);
            if (thread == null) {
            	break;
            }
            if (!thread.wait.MsgPipe_isSend && tryReceiveMsgPipe(info, thread.wait.MsgPipe_address, thread.wait.MsgPipe_size, thread.wait.MsgPipe_waitMode, thread.wait.MsgPipe_resultSize_addr)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("onMsgPipeReceiveModified waking thread %s", thread));
                }
                info.receiveThreadWaitingList.removeWaitingThread(thread);
                thread.cpuContext._v0 = 0;
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                reschedule = true;
            } else {
            	checkedThread = thread;
            }
        }

        // Reschedule only if threads waked up.
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    private boolean trySendMsgPipe(SceKernelMppInfo info, TPointer addr, int size, int waitMode, TPointer32 resultSizeAddr) {
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
        info.append(addr.getMemory(), addr.getAddress(), size);
        resultSizeAddr.setValue(size);

        return true;
    }

    private boolean tryReceiveMsgPipe(SceKernelMppInfo info, TPointer addr, int size, int waitMode, TPointer32 resultSizeAddr) {
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
        resultSizeAddr.setValue(size);
        info.consume(addr.getMemory(), addr.getAddress(), size);

        return true;
    }

    public int checkMsgPipeID(int uid) {
        if (!msgMap.containsKey(uid)) {
            log.warn(String.format("checkMsgPipeID unknown uid=0x%X", uid));
            throw new SceKernelErrorException(ERROR_KERNEL_NOT_FOUND_MESSAGE_PIPE);
        }

        return uid;
    }

    public int sceKernelCreateMsgPipe(String name, int partitionid, int attr, int size, TPointer option) {
        if (option.isNotNull()) {
            int optionSize = option.getValue32();
            log.warn(String.format("sceKernelCreateMsgPipe option at %s, size=%d", option, optionSize));
        }

        int memType = PSP_SMEM_Low;
        if ((attr & PSP_MPP_ATTR_ADDR_HIGH) == PSP_MPP_ATTR_ADDR_HIGH) {
            memType = PSP_SMEM_High;
        }

        SceKernelMppInfo info = SceKernelMppInfo.tryCreateMpp(name, partitionid, attr, size, memType);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelCreateMsgPipe returning %s", info));
        }
        msgMap.put(info.uid, info);

        return info.uid;
    }

    public int sceKernelDeleteMsgPipe(int uid) {
        SceKernelMppInfo info = msgMap.remove(uid);
        info.deleteSysMemInfo();
        onMsgPipeDeleted(uid);

        return 0;
    }

    private int hleKernelSendMsgPipe(int uid, TPointer msgAddr, int size, int waitMode, TPointer32 resultSizeAddr, TPointer32 timeoutAddr, boolean doCallbacks, boolean poll) {
        SceKernelMppInfo info = msgMap.get(uid);
        if (size > info.bufSize) {
            log.warn(String.format("hleKernelSendMsgPipe illegal size 0x%X max 0x%X", size, info.bufSize));
            return ERROR_KERNEL_ILLEGAL_SIZE;
        }
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        if (!trySendMsgPipe(info, msgAddr, size, waitMode, resultSizeAddr)) {
            if (!poll) {
                // Failed, but it's ok, just wait a little
                if (log.isDebugEnabled()) {
                    log.debug(String.format("hleKernelSendMsgPipe %s waiting for 0x%X bytes to become available", info, size));
                }
                SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
                info.sendThreadWaitingList.addWaitingThread(currentThread);
                // Wait on a specific MsgPipe.
                currentThread.wait.MsgPipe_isSend = true;
                currentThread.wait.MsgPipe_id = uid;
                currentThread.wait.MsgPipe_address = msgAddr;
                currentThread.wait.MsgPipe_size = size;
                threadMan.hleKernelThreadEnterWaitState(PSP_WAIT_MSGPIPE, uid, msgPipeSendWaitStateChecker, timeoutAddr.getAddress(), doCallbacks);
            } else {
                log.warn(String.format("hleKernelSendMsgPipe illegal size 0x%X, max 0x%X (pipe needs consuming)", size, info.freeSize));
                return ERROR_KERNEL_MESSAGE_PIPE_FULL;
            }
        } else {
            // Success, do not reschedule the current thread.
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleKernelSendMsgPipe %s fast check succeeded", info));
            }
            onMsgPipeReceiveModified(info);
        }

        return 0;
    }

    public int sceKernelSendMsgPipe(int uid, TPointer msgAddr, int size, int waitMode, TPointer32 resultSizeAddr, TPointer32 timeoutAddr) {
        return hleKernelSendMsgPipe(uid, msgAddr, size, waitMode, resultSizeAddr, timeoutAddr, false, false);
    }

    public int sceKernelSendMsgPipeCB(int uid, TPointer msgAddr, int size, int waitMode, TPointer32 resultSizeAddr, TPointer32 timeoutAddr) {
        return hleKernelSendMsgPipe(uid, msgAddr, size, waitMode, resultSizeAddr, timeoutAddr, true, false);
    }

    public int sceKernelTrySendMsgPipe(int uid, TPointer msgAddr, int size, int waitMode, TPointer32 resultSizeAddr) {
        return hleKernelSendMsgPipe(uid, msgAddr, size, waitMode, resultSizeAddr, TPointer32.NULL, false, true);
    }

    private int hleKernelReceiveMsgPipe(int uid, TPointer msgAddr, int size, int waitMode, TPointer32 resultSizeAddr, TPointer32 timeoutAddr, boolean doCallbacks, boolean poll) {
        SceKernelMppInfo info = msgMap.get(uid);
        if (size > info.bufSize) {
            log.warn(String.format("hleKernelReceiveMsgPipe illegal size 0x%X, max 0x%X", size, info.bufSize));
            return ERROR_KERNEL_ILLEGAL_SIZE;
        }

        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        if (!tryReceiveMsgPipe(info, msgAddr, size, waitMode, resultSizeAddr)) {
            if (!poll) {
                // Failed, but it's ok, just wait a little
                if (log.isDebugEnabled()) {
                    log.debug(String.format("hleKernelReceiveMsgPipe %s waiting for 0x%X bytes to become available", info, size));
                }
                SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
                info.receiveThreadWaitingList.addWaitingThread(currentThread);
                // Wait on a specific MsgPipe.
                currentThread.wait.MsgPipe_isSend = false;
                currentThread.wait.MsgPipe_id = uid;
                currentThread.wait.MsgPipe_address = msgAddr;
                currentThread.wait.MsgPipe_size = size;
                currentThread.wait.MsgPipe_resultSize_addr = resultSizeAddr;
                threadMan.hleKernelThreadEnterWaitState(PSP_WAIT_MSGPIPE, uid, msgPipeReceiveWaitStateChecker, timeoutAddr.getAddress(), doCallbacks);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("hleKernelReceiveMsgPipe trying to read more than is available size 0x%X, available 0x%X", size, info.bufSize - info.freeSize));
                }
                return ERROR_KERNEL_MESSAGE_PIPE_EMPTY;
            }
        } else {
            // Success, do not reschedule the current thread.
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleKernelReceiveMsgPipe %s fast check succeeded", info));
            }
            onMsgPipeSendModified(info);
        }

        return 0;
    }

    public int sceKernelReceiveMsgPipe(int uid, TPointer msgAddr, int size, int waitMode, TPointer32 resultSizeAddr, TPointer32 timeoutAddr) {
        return hleKernelReceiveMsgPipe(uid, msgAddr, size, waitMode, resultSizeAddr, timeoutAddr, false, false);
    }

    public int sceKernelReceiveMsgPipeCB(int uid, TPointer msgAddr, int size, int waitMode, TPointer32 resultSizeAddr, TPointer32 timeoutAddr) {
        return hleKernelReceiveMsgPipe(uid, msgAddr, size, waitMode, resultSizeAddr, timeoutAddr, true, false);
    }

    public int sceKernelTryReceiveMsgPipe(int uid, TPointer msgAddr, int size, int waitMode, TPointer32 resultSizeAddr) {
        return hleKernelReceiveMsgPipe(uid, msgAddr, size, waitMode, resultSizeAddr, TPointer32.NULL, false, true);
    }

    public int sceKernelCancelMsgPipe(int uid, TPointer32 sendAddr, TPointer32 recvAddr) {
        SceKernelMppInfo info = msgMap.get(uid);

        sendAddr.setValue(info.getNumSendWaitThreads());
        recvAddr.setValue(info.getNumReceiveWaitThreads());
        info.sendThreadWaitingList.removeAllWaitingThreads();
        info.receiveThreadWaitingList.removeAllWaitingThreads();
        onMsgPipeCancelled(uid);

        return 0;
    }

    public int sceKernelReferMsgPipeStatus(int uid, TPointer infoAddr) {
        SceKernelMppInfo info = msgMap.get(uid);
        info.write(infoAddr);

        return 0;
    }

    private class MsgPipeSendWaitStateChecker implements IWaitStateChecker {
        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the msgpipe
            // has received a new message during the callback execution.
            SceKernelMppInfo info = msgMap.get(wait.MsgPipe_id);
            if (info == null) {
                thread.cpuContext._v0 = ERROR_KERNEL_NOT_FOUND_MESSAGE_PIPE;
                return false;
            }

            if (trySendMsgPipe(info, thread.wait.MsgPipe_address, thread.wait.MsgPipe_size, thread.wait.MsgPipe_waitMode, thread.wait.MsgPipe_resultSize_addr)) {
                info.sendThreadWaitingList.removeWaitingThread(thread);
                thread.cpuContext._v0 = 0;
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
                thread.cpuContext._v0 = ERROR_KERNEL_NOT_FOUND_MESSAGE_PIPE;
                return false;
            }

            if (tryReceiveMsgPipe(info, wait.MsgPipe_address, wait.MsgPipe_size, wait.MsgPipe_waitMode, wait.MsgPipe_resultSize_addr)) {
            	info.receiveThreadWaitingList.removeWaitingThread(thread);
                thread.cpuContext._v0 = 0;
                return false;
            }

            return true;
        }
    }
    public static final MsgPipeManager singleton = new MsgPipeManager();

    private MsgPipeManager() {
    }
}