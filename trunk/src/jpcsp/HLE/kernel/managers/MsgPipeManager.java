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

import java.util.HashMap;
import java.util.Iterator;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.pspSysMem;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.kernel.types.SceKernelMppInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;
import jpcsp.util.Utilities;

public class MsgPipeManager {

    private HashMap<Integer, SceKernelMppInfo> msgMap;

    public void reset() {
        msgMap = new HashMap<Integer, SceKernelMppInfo>();
    }


    /** @return true if the thread was waiting on a valid XXX */
    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        if (thread.wait.waitingOnMsgPipeSend) {
            // Untrack
            thread.wait.waitingOnMsgPipeSend = false;

            // Update numSendWaitThreads
            SceKernelMppInfo info = msgMap.get(thread.wait.MsgPipe_id);
            if (info != null) {
                info.numSendWaitThreads--;

                if (info.numSendWaitThreads < 0) {
                    Modules.log.warn("removing waiting thread " + Integer.toHexString(thread.uid)
                        + ", MsgPipe " + Integer.toHexString(info.uid) + " numSendWaitThreads underflowed");
                    info.numSendWaitThreads = 0;
                }

                return true;
            }
        } else if (thread.wait.waitingOnMsgPipeSend) {
            // Untrack
            thread.wait.waitingOnMsgPipeReceive = false;

            // Update numReceiveWaitThreads
            SceKernelMppInfo info = msgMap.get(thread.wait.MsgPipe_id);
            if (info != null) {
                info.numReceiveWaitThreads--;

                if (info.numReceiveWaitThreads < 0) {
                    Modules.log.warn("removing waiting thread " + Integer.toHexString(thread.uid)
                        + ", MsgPipe " + Integer.toHexString(info.uid) + " numReceiveWaitThreads underflowed");
                    info.numReceiveWaitThreads = 0;
                }

                return true;
            }
        }

        return false;
    }

    /** Don't call this unless thread.wait.waitingOnMsgPipeSend or waitingOnMsgPipeReceive is true */
    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return WAIT_TIMEOUT
            thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
        } else {
            Modules.log.warn("MsgPipe deleted while we were waiting for it! (timeout expired)");

            // Return WAIT_DELETE
            thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
        }
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        removeWaitingThread(thread);
    }


    // TODO check if we should yield when waking higher priority threads
    private void updateWaitingMsgPipeSend(SceKernelMppInfo info) {
        Memory mem = Memory.getInstance();

        // Find threads waiting on this XXX and wake them up
        ThreadMan threadMan = ThreadMan.getInstance();
        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();

            if (thread.wait.waitingOnMsgPipeSend &&
                thread.wait.MsgPipe_id == info.uid &&
                trySendMsgPipe(mem, info, thread.wait.MsgPipe_address, thread.wait.MsgPipe_size)) {
                // Untrack
                thread.wait.waitingOnMsgPipeSend = false;

                // Return success
                thread.cpuContext.gpr[2] = 0;

                // Wakeup
                threadMan.changeThreadState(thread, PSP_THREAD_READY);
            }
        }
    }

    // TODO check if we should yield when waking higher priority threads
    private void updateWaitingMsgPipeReceive(SceKernelMppInfo info) {
        Memory mem = Memory.getInstance();

        // Find threads waiting on this XXX and wake them up
        ThreadMan threadMan = ThreadMan.getInstance();
        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();

            if (thread.wait.waitingOnMsgPipeReceive &&
                thread.wait.MsgPipe_id == info.uid &&
                tryReceiveMsgPipe(mem, info, thread.wait.MsgPipe_address, thread.wait.MsgPipe_size)) {
                // Untrack
                thread.wait.waitingOnMsgPipeReceive = false;

                // Return success
                thread.cpuContext.gpr[2] = 0;

                // Wakeup
                threadMan.changeThreadState(thread, PSP_THREAD_READY);
            }
        }
    }


    /** @return true on success */
    private boolean trySendMsgPipe(Memory mem, SceKernelMppInfo info, int addr, int size) {
        if (size > info.availableWriteSize())
            return false;

        info.append(mem, addr, size);

        return true;
    }

    /** @return true on success */
    private boolean tryReceiveMsgPipe(Memory mem, SceKernelMppInfo info, int addr, int size) {
        if (size > info.availableReadSize())
            return false;

        info.consume(mem, addr, size);

        return true;
    }


    public void sceKernelCreateMsgPipe(int name_addr, int partitionid, int attr, int size, int opt_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        String name = Utilities.readStringZ(name_addr);
        Modules.log.warn("PARTIAL:sceKernelCreateMsgPipe(name=" + name
            + ",partition=" + partitionid
            + ",attr=0x" + Integer.toHexString(attr)
            + ",size=0x" + Integer.toHexString(size)
            + ",opt=0x" + Integer.toHexString(opt_addr) + ")");

        // 0x1100 Star Ocean: First Departure
        if (attr != 0) Modules.log.warn("UNIMPLEMENTED:sceKernelCreateMsgPipe attr value 0x" + Integer.toHexString(attr));

        if (mem.isAddressGood(opt_addr)) {
            int optsize = mem.read32(opt_addr);
            Modules.log.warn("UNIMPLEMENTED:sceKernelCreateMsgPipe option at 0x" + Integer.toHexString(opt_addr)
                + " (size=" + optsize + ")");
        }

        SceKernelMppInfo info = SceKernelMppInfo.tryCreateMpp(name, partitionid, attr, size);
        if (info != null) {
            Modules.log.info("sceKernelCreateMsgPipe '" + name + "' assigned uid " + Integer.toHexString(info.uid));
            msgMap.put(info.uid, info);
            cpu.gpr[2] = info.uid;
        } else {
            cpu.gpr[2] = ERROR_NO_MEMORY;
        }
    }

    public void sceKernelDeleteMsgPipe(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.info("sceKernelDeleteMsgPipe(uid=0x" + Integer.toHexString(uid) + ")");

        SceKernelMppInfo info = msgMap.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelDeleteMsgPipe unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_PIPE;
        } else {
            // Free memory
            info.free();

            cpu.gpr[2] = 0;
        }
    }

    private void hleKernelSendMsgPipe(int uid, int msg_addr, int size, int unk1, int unk2, int timeout_addr,
        boolean do_callbacks, boolean poll) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int micros = 0;
        if (!poll && mem.isAddressGood(timeout_addr)) {
            micros = mem.read32(timeout_addr);
        }

        String waitType = "";
        if (poll) waitType = "poll";
        else if (timeout_addr == 0) waitType = "forever";
        else waitType = micros + " ms";
        if (do_callbacks) waitType += " + CB";

        Modules.log.info("hleKernelSendMsgPipe(uid=0x" + Integer.toHexString(uid)
            + ",msg=0x" + Integer.toHexString(msg_addr)
            + ",size=0x" + Integer.toHexString(size)
            + ",unk1=0x" + Integer.toHexString(unk1)
            + ",unk2=0x" + Integer.toHexString(unk2)
            + ",timeout=0x" + Integer.toHexString(timeout_addr) + ")"
            + " " + waitType);

        SceKernelMppInfo info = msgMap.get(uid);
        if (info == null) {
            Modules.log.warn("hleKernelSendMsgPipe unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_PIPE;
        } else if (size > info.bufSize) {
            Modules.log.warn("hleKernelSendMsgPipe illegal size 0x" + Integer.toHexString(size)
                + " max 0x" + Integer.toHexString(info.bufSize));
            cpu.gpr[2] = ERROR_ILLEGAL_SIZE;
        } else {
            if (!trySendMsgPipe(mem, info, msg_addr, size)) {
                if (!poll) {
                    // Failed, but it's ok, just wait a little
                    Modules.log.info("hleKernelSendMsgPipe - '" + info.name + "' waiting for " + size + " bytes to become available");
                    info.numSendWaitThreads++;

                    ThreadMan threadMan = ThreadMan.getInstance();
                    SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

                    // Do callbacks?
                    currentThread.do_callbacks = do_callbacks;

                    // wait type
                    currentThread.waitType = PSP_WAIT_MSGPIPE;
                    currentThread.waitId = uid;

                    // Go to wait state
                    threadMan.hleKernelThreadWait(currentThread.wait, micros, (timeout_addr == 0));

                    // Wait on a specific XXX
                    currentThread.wait.waitingOnMsgPipeSend = true;
                    currentThread.wait.MsgPipe_id = uid;
                    currentThread.wait.MsgPipe_address = msg_addr;
                    currentThread.wait.MsgPipe_size = size;

                    threadMan.changeThreadState(currentThread, PSP_THREAD_WAITING);

                    threadMan.contextSwitch(threadMan.nextThread());
                } else {
                    Modules.log.warn("hleKernelSendMsgPipe illegal size 0x" + Integer.toHexString(size)
                        + " max 0x" + Integer.toHexString(info.freeSize) + " (pipe needs consuming)");
                    cpu.gpr[2] = ERROR_ILLEGAL_SIZE;

                    // not sure about this
                    if (do_callbacks) {
                        ThreadMan.getInstance().yieldCurrentThreadCB();
                    }
                }
            } else {
                // success
                cpu.gpr[2] = 0;

                // not sure about this
                if (do_callbacks) {
                    ThreadMan.getInstance().yieldCurrentThreadCB();
                }

                updateWaitingMsgPipeReceive(info);
            }
        }
    }

    public void sceKernelSendMsgPipe(int uid, int msg_addr, int size, int unk1, int unk2, int timeout_addr) {
        hleKernelSendMsgPipe(uid, msg_addr, size, unk1, unk2, timeout_addr, false, false);
    }

    public void sceKernelSendMsgPipeCB(int uid, int msg_addr, int size, int unk1, int unk2, int timeout_addr) {
        hleKernelSendMsgPipe(uid, msg_addr, size, unk1, unk2, timeout_addr, true, false);
    }

    public void sceKernelTrySendMsgPipe(int uid, int msg_addr, int size, int unk1, int unk2) {
        hleKernelSendMsgPipe(uid, msg_addr, size, unk1, unk2, 0, false, true);
    }

    private void hleKernelReceiveMsgPipe(int uid, int msg_addr, int size, int unk1, int unk2, int timeout_addr,
        boolean do_callbacks, boolean poll) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        int micros = 0;
        if (!poll && mem.isAddressGood(timeout_addr)) {
            micros = mem.read32(timeout_addr);
        }

        String waitType = "";
        if (poll) waitType = "poll";
        else if (timeout_addr == 0) waitType = "forever";
        else waitType = micros + " ms";
        if (do_callbacks) waitType += " + CB";

        Modules.log.info("hleKernelReceiveMsgPipe(uid=0x" + Integer.toHexString(uid)
            + ",msg=0x" + Integer.toHexString(msg_addr)
            + ",size=0x" + Integer.toHexString(size)
            + ",unk1=0x" + Integer.toHexString(unk1)
            + ",unk2=0x" + Integer.toHexString(unk2)
            + ",timeout=0x" + Integer.toHexString(timeout_addr) + ")"
            + " " + waitType);

        SceKernelMppInfo info = msgMap.get(uid);
        if (info == null) {
            Modules.log.warn("hleKernelReceiveMsgPipe unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_PIPE;
        } else if (size > info.bufSize) {
            Modules.log.warn("hleKernelReceiveMsgPipe illegal size 0x" + Integer.toHexString(size)
                + " max 0x" + Integer.toHexString(info.bufSize));
            cpu.gpr[2] = ERROR_ILLEGAL_SIZE;
        } else {
            if (!tryReceiveMsgPipe(mem, info, msg_addr, size)) {
                if (!poll) {
                    // Failed, but it's ok, just wait a little
                    Modules.log.info("hleKernelReceiveMsgPipe - '" + info.name + "' waiting for " + size + " bytes to become available");
                    info.numSendWaitThreads++;

                    ThreadMan threadMan = ThreadMan.getInstance();
                    SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

                    // Do callbacks?
                    currentThread.do_callbacks = do_callbacks;

                    // wait type
                    currentThread.waitType = PSP_WAIT_MSGPIPE;
                    currentThread.waitId = uid;

                    // Go to wait state
                    threadMan.hleKernelThreadWait(currentThread.wait, micros, (timeout_addr == 0));

                    // Wait on a specific XXX
                    currentThread.wait.waitingOnMsgPipeReceive = true;
                    currentThread.wait.MsgPipe_id = uid;
                    currentThread.wait.MsgPipe_address = msg_addr;
                    currentThread.wait.MsgPipe_size = size;

                    threadMan.changeThreadState(currentThread, PSP_THREAD_WAITING);

                    threadMan.contextSwitch(threadMan.nextThread());
                } else {
                    Modules.log.warn("hleKernelReceiveMsgPipe trying to read more than is available size 0x" + Integer.toHexString(size)
                        + " available 0x" + Integer.toHexString(info.bufSize - info.freeSize));
                    cpu.gpr[2] = ERROR_MESSAGE_PIPE_EMPTY;

                    // not sure about this
                    if (do_callbacks) {
                        ThreadMan.getInstance().yieldCurrentThreadCB();
                    }
                }
            } else {
                // success
                cpu.gpr[2] = 0;

                // not sure about this
                if (do_callbacks) {
                    ThreadMan.getInstance().yieldCurrentThreadCB();
                }

                updateWaitingMsgPipeSend(info);
            }
        }
    }

    public void sceKernelReceiveMsgPipe(int uid, int msg_addr, int size, int unk1, int unk2, int timeout_addr) {
        hleKernelReceiveMsgPipe(uid, msg_addr, size, unk1, unk2, timeout_addr, false, false);
    }

    public void sceKernelReceiveMsgPipeCB(int uid, int msg_addr, int size, int unk1, int unk2, int timeout_addr) {
        hleKernelReceiveMsgPipe(uid, msg_addr, size, unk1, unk2, timeout_addr, true, false);
    }

    public void sceKernelTryReceiveMsgPipe(int uid, int msg_addr, int size, int unk1, int unk2) {
        hleKernelReceiveMsgPipe(uid, msg_addr, size, unk1, unk2, 0, false, true);
    }

    public void sceKernelCancelMsgPipe(int uid, int send_addr, int recv_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = cpu.memory;

        Modules.log.debug("sceKernelCancelMsgPipe(uid=0x" + Integer.toHexString(uid)
            + ",send=0x" + Integer.toHexString(send_addr)
            + ",recv=0x" + Integer.toHexString(recv_addr) + ")");

        SceKernelMppInfo info = msgMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelCancelMsgPipe unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_PIPE;
        } else {
            if (mem.isAddressGood(send_addr)) {
                mem.write32(send_addr, info.numSendWaitThreads);
            }
            if (mem.isAddressGood(recv_addr)) {
                mem.write32(recv_addr, info.numReceiveWaitThreads);
            }

            info.numSendWaitThreads = 0;
            info.numReceiveWaitThreads = 0;

            // Find threads waiting on this XXX and wake them up
            ThreadMan threadMan = ThreadMan.getInstance();
            for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
                SceKernelThreadInfo thread = it.next();

                if ((thread.wait.waitingOnMsgPipeSend || thread.wait.waitingOnMsgPipeReceive) &&
                    thread.wait.MsgPipe_id == uid) {
                    // Untrack
                    thread.wait.waitingOnMsgPipeSend = false;
                    thread.wait.waitingOnMsgPipeReceive = false;

                    // Return WAIT_CANCELLED
                    thread.cpuContext.gpr[2] = ERROR_WAIT_CANCELLED;

                    // Wakeup
                    threadMan.changeThreadState(thread, PSP_THREAD_READY);
                }
            }

            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelReferMsgPipeStatus(int uid, int info_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Processor.memory;

        Modules.log.debug("sceKernelReferMsgPipeStatus(uid=0x" + Integer.toHexString(uid)
            + ",info=0x" + Integer.toHexString(info_addr) + ")");

        SceKernelMppInfo info = msgMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferMsgPipeStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_PIPE;
        } else {
            info.write(mem, info_addr);
            cpu.gpr[2] = 0;
        }
    }

    public static final MsgPipeManager singleton;

    private MsgPipeManager() {
    }

    static {
        singleton = new MsgPipeManager();
    }
}
