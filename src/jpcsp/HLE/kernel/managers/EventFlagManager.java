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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_EVENT_FLAG_ILLEGAL_WAIT_PATTERN;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_EVENT_FLAG_NO_MULTI_PERM;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_EVENT_FLAG_POLL_FAILED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_MODE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_NOT_FOUND_EVENT_FLAG;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_CANCELLED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_DELETE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_STATUS_RELEASED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_WAIT_TIMEOUT;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_THREAD_READY;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.PSP_WAIT_EVENTFLAG;

import java.util.HashMap;
import java.util.Iterator;

import jpcsp.HLE.Modules;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
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

    /** Don't call this unless thread.waitType == PSP_WAIT_EVENTFLAG
     * @return true if the thread was waiting on a valid event flag */
    private boolean removeWaitingThread(SceKernelThreadInfo thread) {
        SceKernelEventFlagInfo event = eventMap.get(thread.wait.EventFlag_id);
        if (event == null) {
        	return false;
        }

        event.threadWaitingList.removeWaitingThread(thread);

        return true;
    }

    /** Don't call this unless thread.wait.waitingOnEventFlag == true */
    public void onThreadWaitTimeout(SceKernelThreadInfo thread) {
        // Untrack
        if (removeWaitingThread(thread)) {
            // Return WAIT_TIMEOUT
            thread.cpuContext._v0 = ERROR_KERNEL_WAIT_TIMEOUT;
        } else {
            log.warn("EventFlag deleted while we were waiting for it! (timeout expired)");
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
        if (thread.isWaitingForType(PSP_WAIT_EVENTFLAG)) {
            // decrement numWaitThreads
            removeWaitingThread(thread);
        }
    }

    private void onEventFlagDeletedCancelled(int evid, int result) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext();) {
            SceKernelThreadInfo thread = it.next();
            if (thread.isWaitingFor(PSP_WAIT_EVENTFLAG, evid)) {
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

    private void onEventFlagDeleted(int evid) {
        onEventFlagDeletedCancelled(evid, ERROR_KERNEL_WAIT_DELETE);
    }

    private void onEventFlagCancelled(int evid) {
        onEventFlagDeletedCancelled(evid, ERROR_KERNEL_WAIT_CANCELLED);
    }

    private void onEventFlagModified(SceKernelEventFlagInfo event) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        boolean reschedule = false;

        SceKernelThreadInfo checkedThread = null;
        while (event.currentPattern != 0) {
            SceKernelThreadInfo thread = event.threadWaitingList.getNextWaitingThread(checkedThread);
            if (thread == null) {
            	break;
            }
            if (checkEventFlag(event, thread.wait.EventFlag_bits, thread.wait.EventFlag_wait, thread.wait.EventFlag_outBits_addr)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("onEventFlagModified waking thread %s", thread));
                }
                event.threadWaitingList.removeWaitingThread(thread);
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

    private boolean checkEventFlag(SceKernelEventFlagInfo event, int bits, int wait, TPointer32 outBitsAddr) {
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
            outBitsAddr.setValue(event.currentPattern);

            if ((wait & PSP_EVENT_WAITCLEARALL) == PSP_EVENT_WAITCLEARALL) {
                event.currentPattern = 0;
            }
            if ((wait & PSP_EVENT_WAITCLEAR) == PSP_EVENT_WAITCLEAR) {
                event.currentPattern &= ~bits;
            }
        }
        return matched;
    }

    public int checkEventFlagID(int uid) {
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        if (!eventMap.containsKey(uid)) {
            log.warn(String.format("checkEventFlagID unknown uid=0x%X", uid));
            throw new SceKernelErrorException(ERROR_KERNEL_NOT_FOUND_EVENT_FLAG);
        }

        return uid;
    }

    public int sceKernelCreateEventFlag(String name, int attr, int initPattern, TPointer option) {
        SceKernelEventFlagInfo event = new SceKernelEventFlagInfo(name, attr, initPattern, initPattern);
        eventMap.put(event.uid, event);

        return event.uid;
    }

    public int sceKernelDeleteEventFlag(int uid) {
        SceKernelEventFlagInfo event = eventMap.remove(uid);

        if (event.getNumWaitThreads() > 0) {
            log.warn(String.format("sceKernelDeleteEventFlag numWaitThreads %d", event.getNumWaitThreads()));
        }
        onEventFlagDeleted(uid);

        return 0;
    }

    public int sceKernelSetEventFlag(int uid, int bitsToSet) {
        SceKernelEventFlagInfo event = eventMap.get(uid);

        event.currentPattern |= bitsToSet;
        onEventFlagModified(event);

        return 0;
    }

    public int sceKernelClearEventFlag(int uid, int bitsToKeep) {
        SceKernelEventFlagInfo event = eventMap.get(uid);

        event.currentPattern &= bitsToKeep;

        return 0;
    }

    public int hleKernelWaitEventFlag(int uid, int bits, int wait, TPointer32 outBitsAddr, TPointer32 timeoutAddr, boolean doCallbacks) {
        if ((wait & ~(PSP_EVENT_WAITOR | PSP_EVENT_WAITCLEAR | PSP_EVENT_WAITCLEARALL)) != 0 ||
            (wait & (PSP_EVENT_WAITCLEAR | PSP_EVENT_WAITCLEARALL)) == (PSP_EVENT_WAITCLEAR | PSP_EVENT_WAITCLEARALL)) {
        	return ERROR_KERNEL_ILLEGAL_MODE;
        }
        if (bits == 0) {
        	return ERROR_KERNEL_EVENT_FLAG_ILLEGAL_WAIT_PATTERN;
        }
        if (!Modules.ThreadManForUserModule.isDispatchThreadEnabled()) {
        	return ERROR_KERNEL_WAIT_CAN_NOT_WAIT;
        }

        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event.getNumWaitThreads() >= 1 && (event.attr & PSP_EVENT_WAITMULTIPLE) != PSP_EVENT_WAITMULTIPLE) {
            log.warn("hleKernelWaitEventFlag already another thread waiting on it");
            return ERROR_KERNEL_EVENT_FLAG_NO_MULTI_PERM;
        }

        if (!checkEventFlag(event, bits, wait, outBitsAddr)) {
            // Failed, but it's ok, just wait a little
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleKernelWaitEventFlag - %s fast check failed", event));
            }
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
            event.threadWaitingList.addWaitingThread(currentThread);
            // Wait on a specific event flag
            currentThread.wait.EventFlag_id = uid;
            currentThread.wait.EventFlag_bits = bits;
            currentThread.wait.EventFlag_wait = wait;
            currentThread.wait.EventFlag_outBits_addr = outBitsAddr;

            threadMan.hleKernelThreadEnterWaitState(PSP_WAIT_EVENTFLAG, uid, eventFlagWaitStateChecker, timeoutAddr.getAddress(), doCallbacks);
        } else {
            // Success, do not reschedule the current thread.
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleKernelWaitEventFlag - %s fast check succeeded", event));
            }
        }

        return 0;
    }

    public int sceKernelWaitEventFlag(int uid, int bits, int wait, TPointer32 outBitsAddr, TPointer32 timeoutAddr) {
        return hleKernelWaitEventFlag(uid, bits, wait, outBitsAddr, timeoutAddr, false);
    }

    public int sceKernelWaitEventFlagCB(int uid, int bits, int wait, TPointer32 outBitsAddr, TPointer32 timeoutAddr) {
        return hleKernelWaitEventFlag(uid, bits, wait, outBitsAddr, timeoutAddr, true);
    }

    public int sceKernelPollEventFlag(int uid, int bits, int wait, TPointer32 outBitsAddr) {
        if (bits == 0) {
        	return ERROR_KERNEL_EVENT_FLAG_ILLEGAL_WAIT_PATTERN;
        }

        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (!checkEventFlag(event, bits, wait, outBitsAddr)) {
        	// Write the outBits, even if the poll failed
            outBitsAddr.setValue(event.currentPattern);
            return ERROR_KERNEL_EVENT_FLAG_POLL_FAILED;
        }

        return 0;
    }

    public int sceKernelCancelEventFlag(int uid, int newPattern, TPointer32 numWaitThreadAddr) {
        SceKernelEventFlagInfo event = eventMap.get(uid);

        numWaitThreadAddr.setValue(event.getNumWaitThreads());
        event.threadWaitingList.removeAllWaitingThreads();
        event.currentPattern = newPattern;
        onEventFlagCancelled(uid);

        return 0;
    }

    public int sceKernelReferEventFlagStatus(int uid, TPointer addr) {
        SceKernelEventFlagInfo event = eventMap.get(uid);
        event.write(addr);

        return 0;
    }

    private class EventFlagWaitStateChecker implements IWaitStateChecker {
        @Override
        public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
            // Check if the thread has to continue its wait state or if the event flag
            // has been set during the callback execution.
            SceKernelEventFlagInfo event = eventMap.get(wait.EventFlag_id);
            if (event == null) {
                thread.cpuContext._v0 = ERROR_KERNEL_NOT_FOUND_EVENT_FLAG;
                return false;
            }

            // Check EventFlag.
            if (checkEventFlag(event, wait.EventFlag_bits, wait.EventFlag_wait, wait.EventFlag_outBits_addr)) {
                event.threadWaitingList.removeWaitingThread(thread);
                thread.cpuContext._v0 = 0;
                return false;
            }

            return true;
        }
    }
    public static final EventFlagManager singleton = new EventFlagManager();

    private EventFlagManager() {
    }
}