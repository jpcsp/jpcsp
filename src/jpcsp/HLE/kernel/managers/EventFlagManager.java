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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_EVENT_FLAG_ILLEGAL_WAIT_PATTERN;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_EVENT_FLAG_NO_MULTI_PERM;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_EVENT_FLAG_POLL_FAILED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NOT_FOUND_EVENT_FLAG;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_STATUS_RELEASED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_WAITING;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_EVENTFLAG;
import static jpcsp.util.Utilities.readStringZ;

import java.util.HashMap;
import java.util.Iterator;

import jpcsp.Allegrex.CpuState;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelEventFlagInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;

import org.apache.log4j.Logger;

public class EventFlagManager {

    protected static Logger log = Modules.getLogger("ThreadManForUser");

    private static HashMap<Integer, SceKernelEventFlagInfo> eventMap;
    private EventFlagWaitStateChecker eventFlagWaitStateChecker;

    protected final static int PSP_EVENT_WAITSINGLE = 0;
    protected final static int PSP_EVENT_WAITMULTIPLE = 0x200;
    protected final static int PSP_EVENT_WAITANDOR_MASK = 0x01;
    protected final static int PSP_EVENT_WAITAND = 0x00;
    protected final static int PSP_EVENT_WAITOR = 0x01;
    protected final static int PSP_EVENT_WAITCLEARALL = 0x10;
    protected final static int PSP_EVENT_WAITCLEAR = 0x20;

    public void reset() {
        eventMap = new HashMap<Integer, SceKernelEventFlagInfo>();
        eventFlagWaitStateChecker = new EventFlagWaitStateChecker();
    }

    /** Don't call this unless thread.wait.waitingOnEventFlag == true
     * @return true if the thread was waiting on a valid event flag */
    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        // Untrack
        thread.wait.waitingOnEventFlag = false;
        // Update numWaitThreads
        SceKernelEventFlagInfo event = eventMap.get(thread.wait.EventFlag_id);
        if (event != null) {
            event.numWaitThreads--;
            if (event.numWaitThreads < 0) {
                log.warn("removing waiting thread " + Integer.toHexString(thread.uid) + ", event " + Integer.toHexString(event.uid) + " numWaitThreads underflowed");
                event.numWaitThreads = 0;
            }
            return true;
        }
        return false;
    }

    /** Don't call this unless thread.wait.waitingOnEventFlag == true */
    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return WAIT_TIMEOUT
            thread.cpuContext.gpr[2] = ERROR_WAIT_TIMEOUT;
        } else {
            log.warn("EventFlag deleted while we were waiting for it! (timeout expired)");
            // Return WAIT_DELETE
            thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
        }
    }

    public void onThreadWaitReleased(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return ERROR_WAIT_STATUS_RELEASED
            thread.cpuContext.gpr[2] = ERROR_WAIT_STATUS_RELEASED;
        } else {
            log.warn("EventFlag deleted while we were waiting for it!");
            // Return WAIT_DELETE
            thread.cpuContext.gpr[2] = ERROR_WAIT_DELETE;
        }
    }

    public void onThreadDeleted(SceKernelThreadInfo thread) {
        if (thread.wait.waitingOnEventFlag) {
            // decrement numWaitThreads
            removeWaitingThread(thread);
        }
    }

    private void onEventFlagDeletedCancelled(int evid, int result) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
            SceKernelThreadInfo thread = it.next();
            if (thread.waitType == PSP_WAIT_EVENTFLAG &&
                    thread.wait.waitingOnEventFlag &&
                    thread.wait.EventFlag_id == evid) {
                thread.wait.waitingOnEventFlag = false;
                thread.cpuContext.gpr[2] = result;
                threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                reschedule = true;
            }
        }
        // Reschedule only if threads waked up.
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    private void onEventFlagDeleted(int evid) {
        onEventFlagDeletedCancelled(evid, ERROR_WAIT_DELETE);
    }

    private void onEventFlagCancelled(int evid) {
        onEventFlagDeletedCancelled(evid, ERROR_WAIT_CANCELLED);
    }

    private void onEventFlagModified(SceKernelEventFlagInfo event) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
            SceKernelThreadInfo thread = it.next();
            if (thread.waitType == PSP_WAIT_EVENTFLAG &&
                    thread.wait.waitingOnEventFlag &&
                    thread.wait.EventFlag_id == event.uid) {
                if (checkEventFlag(event, thread.wait.EventFlag_bits, thread.wait.EventFlag_wait, thread.wait.EventFlag_outBits_addr)) {
                    if (log.isDebugEnabled()) {
                        log.debug("onEventFlagModified waking thread 0x" + Integer.toHexString(thread.uid) + " name:'" + thread.name + "'");
                    }
                    event.numWaitThreads--;
                    thread.wait.waitingOnEventFlag = false;
                    thread.cpuContext.gpr[2] = 0;
                    threadMan.hleChangeThreadState(thread, PSP_THREAD_READY);
                    reschedule = true;

                    if (event.currentPattern == 0) {
                        break;
                    }
                }
            }
        }
        // Reschedule only if threads waked up.
        if (reschedule) {
            threadMan.hleRescheduleCurrentThread();
        }
    }

    private boolean checkEventFlag(SceKernelEventFlagInfo event, int bits, int wait, int outBits_addr) {
        boolean matched = false;

        if (((wait & PSP_EVENT_WAITANDOR_MASK) == PSP_EVENT_WAITAND) &&
                ((event.currentPattern & bits) == bits)) {
            matched = true;
        } else if (((wait & PSP_EVENT_WAITANDOR_MASK) == PSP_EVENT_WAITOR) &&
                ((event.currentPattern & bits) != 0)) {
            matched = true;
        }

        if (matched) {
            // Write current pattern.
            Memory mem = Memory.getInstance();
            if (Memory.isAddressGood(outBits_addr)) {
                mem.write32(outBits_addr, event.currentPattern);
            }
            if ((wait & PSP_EVENT_WAITCLEARALL) == PSP_EVENT_WAITCLEARALL) {
                event.currentPattern = 0;
            }
            if ((wait & PSP_EVENT_WAITCLEAR) == PSP_EVENT_WAITCLEAR) {
                event.currentPattern &= ~bits;
            }
        }
        return matched;
    }

    public void sceKernelCreateEventFlag(int name_addr, int attr, int initPattern, int option) {
        CpuState cpu = Emulator.getProcessor().cpu;

        String name = readStringZ(name_addr);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelCreateEventFlag(name='" + name + "', attr=0x" + Integer.toHexString(attr) + ", initPattern=0x" + Integer.toHexString(initPattern) + ", option=0x" + Integer.toHexString(option) + ")");
        }

        SceKernelEventFlagInfo event = new SceKernelEventFlagInfo(name, attr, initPattern, initPattern);
        eventMap.put(event.uid, event);

        cpu.gpr[2] = event.uid;
    }

    public void sceKernelDeleteEventFlag(int uid) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelDeleteEventFlag uid=0x" + Integer.toHexString(uid) + ")");
        }

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.remove(uid);
        if (event == null) {
            log.warn("sceKernelDeleteEventFlag unknown uid");
            cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            if (event.numWaitThreads > 0) {
                log.warn("sceKernelDeleteEventFlag numWaitThreads " + event.numWaitThreads);
            }
            cpu.gpr[2] = 0;
            onEventFlagDeleted(uid);
        }
    }

    public void sceKernelSetEventFlag(int uid, int bitsToSet) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelSetEventFlag uid=0x" + Integer.toHexString(uid) + " bitsToSet=0x" + Integer.toHexString(bitsToSet));
        }

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            log.warn("sceKernelSetEventFlag unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            event.currentPattern |= bitsToSet;
            cpu.gpr[2] = 0;
            onEventFlagModified(event);
        }
    }

    public void sceKernelClearEventFlag(int uid, int bitsToKeep) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelClearEventFlag uid=0x" + Integer.toHexString(uid) + " bitsToKeep=0x" + Integer.toHexString(bitsToKeep));
        }

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            log.warn("sceKernelClearEventFlag unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            event.currentPattern &= bitsToKeep;
            cpu.gpr[2] = 0;
        }
    }

    public void hleKernelWaitEventFlag(int uid, int bits, int wait, int outBits_addr, int timeout_addr, boolean doCallbacks) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (log.isDebugEnabled()) {
            log.debug("hleKernelWaitEventFlag uid=0x" + Integer.toHexString(uid) + " bits=0x" + Integer.toHexString(bits) + " wait=0x" + Integer.toHexString(wait) + " outBits=0x" + Integer.toHexString(outBits_addr) + " timeout=0x" + Integer.toHexString(timeout_addr) + " callbacks=" + doCallbacks);
        }

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            log.warn("hleKernelWaitEventFlag unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else if (bits == 0) {
        	cpu.gpr[2] = ERROR_EVENT_FLAG_ILLEGAL_WAIT_PATTERN;
        } else if (event.numWaitThreads >= 1 &&
                (event.attr & PSP_EVENT_WAITMULTIPLE) != PSP_EVENT_WAITMULTIPLE) {
            log.warn("hleKernelWaitEventFlag already another thread waiting on it");
            cpu.gpr[2] = ERROR_EVENT_FLAG_NO_MULTI_PERM;
        } else {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            Memory mem = Memory.getInstance();
            int micros = 0;
            if (Memory.isAddressGood(timeout_addr)) {
                micros = mem.read32(timeout_addr);
            }
            if (!checkEventFlag(event, bits, wait, outBits_addr)) {
                // Failed, but it's ok, just wait a little
                if (log.isDebugEnabled()) {
                    log.debug("hleKernelWaitEventFlag - '" + event.name + "' fast check failed");
                }
                event.numWaitThreads++;
                // Go to wait state
                SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
                currentThread.waitType = PSP_WAIT_EVENTFLAG;
                currentThread.waitId = uid;
                // Wait on a specific event flag
                threadMan.hleKernelThreadWait(currentThread, micros, (timeout_addr == 0));
                currentThread.wait.waitingOnEventFlag = true;
                currentThread.wait.EventFlag_id = uid;
                currentThread.wait.EventFlag_bits = bits;
                currentThread.wait.EventFlag_wait = wait;
                currentThread.wait.EventFlag_outBits_addr = outBits_addr;
                currentThread.wait.waitStateChecker = eventFlagWaitStateChecker;

                threadMan.hleChangeThreadState(currentThread, PSP_THREAD_WAITING);
                threadMan.hleRescheduleCurrentThread(doCallbacks);
            } else {
                // Success, do not reschedule the current thread.
                if (log.isDebugEnabled()) {
                    log.debug("hleKernelWaitEventFlag - '" + event.name + "' fast check succeeded");
                }
                cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelWaitEventFlag(int uid, int bits, int wait, int outBits_addr, int timeout_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelWaitEventFlag redirecting to hleKernelWaitEventFlag(callbacks=false)");
        }
        hleKernelWaitEventFlag(uid, bits, wait, outBits_addr, timeout_addr, false);
    }

    public void sceKernelWaitEventFlagCB(int uid, int bits, int wait, int outBits_addr, int timeout_addr) {
        if (log.isDebugEnabled()) {
            log.debug("sceKernelWaitEventFlagCB redirecting to hleKernelWaitEventFlag(callbacks=true)");
        }
        hleKernelWaitEventFlag(uid, bits, wait, outBits_addr, timeout_addr, true);
    }

    public void sceKernelPollEventFlag(int uid, int bits, int wait, int outBits_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelPollEventFlag uid=0x" + Integer.toHexString(uid) + " bits=0x" + Integer.toHexString(bits) + " wait=0x" + Integer.toHexString(wait) + " outBits=0x" + Integer.toHexString(outBits_addr));
        }

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            log.warn("sceKernelPollEventFlag unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else if (bits == 0) {
        	cpu.gpr[2] = ERROR_EVENT_FLAG_ILLEGAL_WAIT_PATTERN;
        } else {
            if (!checkEventFlag(event, bits, wait, outBits_addr)) {
            	// Write the outBits, even if the poll failed
            	if (Memory.isAddressGood(outBits_addr)) {
                    Memory.getInstance().write32(outBits_addr, event.currentPattern);
            	}
                cpu.gpr[2] = ERROR_EVENT_FLAG_POLL_FAILED;
            } else {
                cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelCancelEventFlag(int uid, int newPattern, int numWaitThreadAddr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelCancelEventFlag uid=0x" + Integer.toHexString(uid) + " newPattern=0x" + Integer.toHexString(newPattern) + " numWaitThreadAddr=0x" + Integer.toHexString(numWaitThreadAddr));
        }

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            log.warn("sceKernelCancelEventFlag unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            Memory mem = Memory.getInstance();
            if (Memory.isAddressGood(numWaitThreadAddr)) {
                mem.write32(numWaitThreadAddr, event.numWaitThreads);
            }
            event.currentPattern = newPattern;
            event.numWaitThreads = 0;
            cpu.gpr[2] = 0;
            onEventFlagCancelled(uid);
        }
    }

    public void sceKernelReferEventFlagStatus(int uid, int addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (log.isDebugEnabled()) {
            log.debug("sceKernelReferEventFlagStatus uid=0x" + Integer.toHexString(uid) + " addr=0x" + Integer.toHexString(addr));
        }

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            log.warn("sceKernelReferEventFlagStatus unknown uid=0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            event.write(Memory.getInstance(), addr);
            cpu.gpr[2] = 0;
        }
    }

    private class EventFlagWaitStateChecker implements IWaitStateChecker {

        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the event flag
            // has been set during the callback execution.
            SceKernelEventFlagInfo event = eventMap.get(wait.EventFlag_id);
            if (event == null) {
                thread.cpuContext.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
                return false;
            }

            // Check EventFlag.
            if (checkEventFlag(event, wait.EventFlag_bits, wait.EventFlag_wait, wait.EventFlag_outBits_addr)) {
                event.numWaitThreads--;
                thread.cpuContext.gpr[2] = 0;
                return false;
            }

            return true;
        }
    }
    public static final EventFlagManager singleton = new EventFlagManager();

    private EventFlagManager() {
    }

}