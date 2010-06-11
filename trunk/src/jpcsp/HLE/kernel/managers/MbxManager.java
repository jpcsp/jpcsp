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
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.kernel.types.SceKernelMbxInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;
import jpcsp.util.Utilities;

public class MbxManager {

    private HashMap<Integer, SceKernelMbxInfo> mbxMap;

	public void reset() {
        mbxMap = new HashMap<Integer, SceKernelMbxInfo>();
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
        ThreadMan threadMan = ThreadMan.getInstance();
        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();

            if (thread.wait.waitingOnMbxReceive &&
                thread.wait.Mbx_id == info.uid &&
                checkMbx(info)) {
                thread.wait.waitingOnMbxReceive = false;
                thread.cpuContext.gpr[2] = 0;
                threadMan.changeThreadState(thread, PSP_THREAD_READY);
            }
        }
    }

    private void cancelWaitingMbxReceive(SceKernelMbxInfo info) {
        ThreadMan threadMan = ThreadMan.getInstance();
        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();

            if ((thread.wait.waitingOnMbxReceive &&
                    thread.wait.Mbx_id == info.uid)) {
                    thread.wait.waitingOnMbxReceive = false;
                    thread.cpuContext.gpr[2] = ERROR_WAIT_CANCELLED;
                    threadMan.changeThreadState(thread, PSP_THREAD_READY);
            }
        }
        info.numWaitThreads = 0;
    }

    private boolean trySendMbx(Memory mem, SceKernelMbxInfo info, int addr) {
        if (mem.isAddressGood(addr)) {
            info.firstMessage_addr = addr;
            info.storeMsg(mem, addr);
            info.numMessages++;
            return true;
        }
        return false;
    }

    private boolean checkMbx(SceKernelMbxInfo info) {
        return (info.checkMbxNewMessage());
    }

    public void sceKernelCreateMbx(int name_addr, int attr, int opt_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        String name = Utilities.readStringZ(name_addr);
        Modules.log.warn("PARTIAL:sceKernelCreateMbx(name=" + name
            + ",attr=0x" + Integer.toHexString(attr)
            + ",opt=0x" + Integer.toHexString(opt_addr) + ")");

        if (attr != 0) Modules.log.warn("UNIMPLEMENTED:sceKernelCreateMbx attr value 0x" + Integer.toHexString(attr));

        if (mem.isAddressGood(opt_addr)) {
            int optsize = mem.read32(opt_addr);
            Modules.log.warn("UNIMPLEMENTED:sceKernelCreateMbx option at 0x" + Integer.toHexString(opt_addr)
                + " (size=" + optsize + ")");
        }

        SceKernelMbxInfo info = new SceKernelMbxInfo(name, attr);
        if (info != null) {
            Modules.log.info("sceKernelCreateMbx '" + name + "' assigned uid " + Integer.toHexString(info.uid));
            mbxMap.put(info.uid, info);
            cpu.gpr[2] = info.uid;
        } else {
            cpu.gpr[2] = ERROR_NO_MEMORY;
        }
    }

    public void sceKernelDeleteMbx(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.info("sceKernelDeleteMbx(uid=0x" + Integer.toHexString(uid) + ")");

        SceKernelMbxInfo info = mbxMap.remove(uid);
        if (info == null) {
            Modules.log.warn("sceKernelDeleteMbx unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_BOX;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelSendMbx(int uid, int msg_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        Modules.log.info("sceKernelSendMbx(uid=0x" + Integer.toHexString(uid)
            + ",msg=0x" + Integer.toHexString(msg_addr) + ")");

        SceKernelMbxInfo info = mbxMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelSendMbx unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_BOX;
        } else {
            trySendMbx(mem, info, msg_addr);
            Modules.log.info("sceKernelSendMbx sending (message= '" + info.msgText + "')");
            cpu.gpr[2] = 0;
        }
    }

    private void hleKernelReceiveMbx(int uid, int addr_msg_addr, int timeout_addr,
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

        Modules.log.info("hleKernelReceiveMbx(uid=0x" + Integer.toHexString(uid)
            + ",msg_pointer=0x" + Integer.toHexString(addr_msg_addr)
            + ",timeout=0x" + Integer.toHexString(timeout_addr) + ")"
            + " " + waitType);

        SceKernelMbxInfo info = mbxMap.get(uid);
        if (info == null) {
            Modules.log.warn("hleKernelReceiveMbx unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_BOX;
        } else {
            if (!checkMbx(info)) {
                if (!poll) {
                    Modules.log.info("hleKernelReceiveMbx - '" + info.name + "' (waiting)");
                    info.numWaitThreads++;

                    ThreadMan threadMan = ThreadMan.getInstance();
                    SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

                    currentThread.do_callbacks = do_callbacks;
                    currentThread.waitType = PSP_WAIT_MBX;
                    currentThread.waitId = uid;

                    threadMan.hleKernelThreadWait(currentThread.wait, micros, (timeout_addr == 0));

                    currentThread.wait.waitingOnMbxReceive = true;
                    currentThread.wait.Mbx_id = uid;

                    threadMan.changeThreadState(currentThread, PSP_THREAD_WAITING);
                    threadMan.contextSwitch(threadMan.nextThread());
                } else {
                    Modules.log.warn("hleKernelReceiveMbx has no messages.");
                    cpu.gpr[2] = ERROR_MESSAGEBOX_NO_MESSAGE;

                    if (do_callbacks) {
                        ThreadMan.getInstance().yieldCurrentThreadCB();
                    }
                }
            } else {
                mem.write32(addr_msg_addr, info.firstMessage_addr);
                cpu.gpr[2] = 0;
                if (do_callbacks) {
                    ThreadMan.getInstance().yieldCurrentThreadCB();
                }
                updateWaitingMbxReceive(info);
            }
        }
    }

    public void sceKernelReceiveMbx(int uid, int addr_msg_addr , int timeout_addr) {
        hleKernelReceiveMbx(uid, addr_msg_addr, timeout_addr, false, false);
    }

    public void sceKernelReceiveMbxCB(int uid, int addr_msg_addr , int timeout_addr) {
        hleKernelReceiveMbx(uid, addr_msg_addr, timeout_addr, true, false);
    }

    public void sceKernelCancelReceiveMbx(int uid, int pnum_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        Modules.log.debug("sceKernelCancelReceiveMbx(uid=0x" + Integer.toHexString(uid) + ")");

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

        Modules.log.debug("sceKernelReferMbxStatus(uid=0x" + Integer.toHexString(uid)
            + ",info=0x" + Integer.toHexString(info_addr) + ")");

        SceKernelMbxInfo info = mbxMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferMbxStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_BOX;
        } else {
            info.write(mem, info_addr);
            cpu.gpr[2] = 0;
        }
    }

	public void sceKernelPollMbx(int uid, int addr_msg_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;
        Memory mem = Memory.getInstance();

        Modules.log.info("sceKernelPollMbx(uid=0x" + Integer.toHexString(uid)
                + ", addr_msg_addr=0x" + Integer.toHexString(addr_msg_addr) + ")");

        SceKernelMbxInfo info = mbxMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelPollMbx unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_MESSAGE_BOX;
        } else {
            if (!checkMbx(info)) {
                cpu.gpr[2] = ERROR_MESSAGEBOX_NO_MESSAGE;
            } else {
                mem.write32(addr_msg_addr, info.firstMessage_addr);
                cpu.gpr[2] = 0;
            }
        }
	}

	public static final MbxManager singleton;

    private MbxManager() {
    }

    static {
        singleton = new MbxManager();
    }
}