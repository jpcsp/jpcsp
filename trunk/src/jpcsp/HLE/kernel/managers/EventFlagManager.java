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
import jpcsp.Allegrex.CpuState;
import static jpcsp.util.Utilities.*;

import jpcsp.HLE.Modules;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.kernel.types.*;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;

public class EventFlagManager {

    private static HashMap<Integer, SceKernelEventFlagInfo> eventMap;

    private final static int PSP_EVENT_WAITMULTIPLE = 0x200;

    private final static int PSP_EVENT_WAITAND = 0x00;
    private final static int PSP_EVENT_WAITOR = 0x01;
    private final static int PSP_EVENT_WAITCLEARALL = 0x10;
    private final static int PSP_EVENT_WAITCLEAR = 0x20;

    public void reset() {
        eventMap = new HashMap<Integer, SceKernelEventFlagInfo>();
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
                Modules.log.warn("removing waiting thread " + Integer.toHexString(thread.uid)
                    + ", event " + Integer.toHexString(event.uid) + " numWaitThreads underflowed");
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
            Modules.log.warn("EventFlag deleted while we were waiting for it! (timeout expired)");

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


    /** May yield, so call last/after setting gpr[2] */
    private void onEventFlagDeletedCancelled(int evid, int result) {
        ThreadMan threadMan = ThreadMan.getInstance();
        int currentPriority = threadMan.getCurrentThread().currentPriority;
        boolean yield = false;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();

            // We're assuming if waitingOnEventFlag is set then thread.status = waiting
            if (thread.wait.waitingOnEventFlag &&
                thread.wait.EventFlag_id == evid) {
                // EventFlag was deleted
                Modules.log.warn("EventFlag deleted while we were waiting for it! thread:" + Integer.toHexString(thread.uid) + "/'" + thread.name + "'");

                // Untrack
                thread.wait.waitingOnEventFlag = false;

                // Return ERROR_WAIT_DELETE / ERROR_WAIT_CANCELLED
                thread.cpuContext.gpr[2] = result;

                // Wakeup
                threadMan.changeThreadState(thread, PSP_THREAD_READY);

                // switch in the target thread if it's now higher priority (check)
                if (thread.currentPriority < currentPriority) {
                    yield = true;
                }
            }
        }

        if (yield) {
            Modules.log.debug("onEventFlagDeletedCancelled yielding to thread with higher priority");
            threadMan.yieldCurrentThread();
        }
    }

    /** May yield, so call last/after setting gpr[2] */
    private void onEventFlagDeleted(int evid) {
        onEventFlagDeletedCancelled(evid, ERROR_WAIT_DELETE);
    }

    /** May yield, so call last/after setting gpr[2] */
    private void onEventFlagCancelled(int evid) {
        onEventFlagDeletedCancelled(evid, ERROR_WAIT_CANCELLED);
    }

    /** May yield, so call last/after setting gpr[2] */
    private void onEventFlagModified(SceKernelEventFlagInfo event) {
        ThreadMan threadMan = ThreadMan.getInstance();
        int currentPriority = threadMan.getCurrentThread().currentPriority;
        boolean yield = false;

        for (Iterator<SceKernelThreadInfo> it = threadMan.iterator(); it.hasNext(); ) {
            SceKernelThreadInfo thread = it.next();

            // We're assuming if waitingOnEventFlag is set then thread.status = waiting
            if (thread.wait.waitingOnEventFlag &&
                thread.wait.EventFlag_id == event.uid) {

                int bits = thread.wait.EventFlag_bits;
                int wait = thread.wait.EventFlag_wait;
                int outBits_addr = thread.wait.EventFlag_outBits_addr;

                // Check EventFlag
                if (checkEventFlag(event, bits, wait, outBits_addr)) {
                    // Success
                    Modules.log.debug("onEventFlagModified waking thread 0x" + Integer.toHexString(thread.uid)
                        + " name:'" + thread.name + "'");

                    // Update numWaitThreads
                    event.numWaitThreads--;

                    // Untrack
                    thread.wait.waitingOnEventFlag = false;

                    // Return success
                    thread.cpuContext.gpr[2] = 0;

                    // Wakeup
                    threadMan.changeThreadState(thread, PSP_THREAD_READY);

                    // switch in the target thread if it's now higher priority (check)
                    if (thread.currentPriority < currentPriority) {
                        yield = true;
                    }
                }
            }
        }

        if (yield) {
            Modules.log.debug("onEventFlagModified yielding to thread with higher priority");
            threadMan.yieldCurrentThread();
        }
    }


    public void sceKernelCreateEventFlag(int name_addr, int attr, int initPattern, int option)
    {
        String name = readStringZ(name_addr);

        Modules.log.debug("sceKernelCreateEventFlag(name='" + name
            + "',attr=0x" + Integer.toHexString(attr)
            + ",initPattern=0x" + Integer.toHexString(initPattern)
            + ",option=0x" + Integer.toHexString(option) + ")");

        if (option !=0) Modules.log.warn("sceKernelCreateEventFlag: UNSUPPORTED Option Value");
        SceKernelEventFlagInfo event = new SceKernelEventFlagInfo(name, attr, initPattern, initPattern); //initPattern and currentPattern should be the same at init
        eventMap.put(event.uid, event);

        Modules.log.debug("sceKernelCreateEventFlag assigned uid=0x" + Integer.toHexString(event.uid));
        Emulator.getProcessor().cpu.gpr[2] = event.uid;
    }

    public void sceKernelDeleteEventFlag(int uid)
    {
        String msg = "sceKernelDeleteEventFlag uid=0x" + Integer.toHexString(uid);
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.remove(uid);
        if (event == null) {
            Modules.log.warn(msg + " unknown uid");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            Modules.log.debug(msg + " name:'" + event.name + "'");
            if (event.numWaitThreads > 0) {
                Modules.log.warn("sceKernelDeleteEventFlag numWaitThreads " + event.numWaitThreads);
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
            onEventFlagDeleted(uid);
        }
    }

    public void sceKernelSetEventFlag(int uid, int bitsToSet)
    {
        Modules.log.debug("sceKernelSetEventFlag uid=0x" + Integer.toHexString(uid) + " bitsToSet=0x" + Integer.toHexString(bitsToSet));
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            Modules.log.warn("sceKernelSetEventFlag unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            event.currentPattern |= bitsToSet;
            Emulator.getProcessor().cpu.gpr[2] = 0;
            onEventFlagModified(event);
        }
    }

    public void sceKernelClearEventFlag(int uid, int bitsToKeep)
    {
        Modules.log.debug("sceKernelClearEventFlag uid=0x" + Integer.toHexString(uid) + " bitsToKeep=0x" + Integer.toHexString(bitsToKeep));
        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            Modules.log.warn("sceKernelClearEventFlag unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            event.currentPattern &= bitsToKeep;
            Emulator.getProcessor().cpu.gpr[2] = 0;
            onEventFlagModified(event);
        }
    }

    /** If there was a match we attempt to write the current pattern to outBits_addr.
     * @return true if there was a match. */
    private boolean checkEventFlag(SceKernelEventFlagInfo event, int bits, int wait, int outBits_addr) {
        boolean matched = false;

        if ((wait & PSP_EVENT_WAITOR) == PSP_EVENT_WAITOR &&
            (event.currentPattern & bits) != 0) {
            //Modules.log.debug("checkEventFlag matched PSP_EVENT_WAITOR");
            matched = true;
        }

        // PSP_EVENT_WAITAND is 0x00, check last
        else if ((event.currentPattern & bits) == bits) {
            //Modules.log.debug("checkEventFlag matched PSP_EVENT_WAITAND");
            matched = true;
        }

        if (matched) {
            // Write current pattern
            Memory mem = Memory.getInstance();
            if (mem.isAddressGood(outBits_addr)) {
                mem.write32(outBits_addr, event.currentPattern);
            }

            // PSP_EVENT_WAITCLEARALL from noxa/pspplayer
            if ((wait & PSP_EVENT_WAITCLEARALL) == PSP_EVENT_WAITCLEARALL) {
                Modules.log.debug("checkEventFlag matched PSP_EVENT_WAITCLEARALL");
                event.currentPattern = 0;
            }

            if ((wait & PSP_EVENT_WAITCLEAR) == PSP_EVENT_WAITCLEAR) {
                //Modules.log.debug("checkEventFlag matched PSP_EVENT_WAITCLEAR");
                event.currentPattern &= ~bits;
            }
        }

        return matched;
    }

    public void hleKernelWaitEventFlag(int uid, int bits, int wait, int outBits_addr, int timeout_addr, boolean do_callbacks)
    {
        Modules.log.debug("hleKernelWaitEventFlag uid=0x" + Integer.toHexString(uid)
            + " bits=0x" + Integer.toHexString(bits)
            + " wait=0x" + Integer.toHexString(wait)
            + " outBits=0x" + Integer.toHexString(outBits_addr)
            + " timeout=0x" + Integer.toHexString(timeout_addr)
            + " callbacks=" + do_callbacks);

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            Modules.log.warn("hleKernelWaitEventFlag unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else if (event.numWaitThreads >= 1 &&
            (event.attr & PSP_EVENT_WAITMULTIPLE) != PSP_EVENT_WAITMULTIPLE) {
            Modules.log.warn("hleKernelWaitEventFlag already another thread waiting on it");
            Emulator.getProcessor().cpu.gpr[2] = ERROR_EVENT_FLAG_NO_MULTI_PERM;
        } else {
            ThreadMan threadMan = ThreadMan.getInstance();
            Memory mem = Memory.getInstance();
            int micros = 0;
            if (mem.isAddressGood(timeout_addr)) {
                micros = mem.read32(timeout_addr);
                //Modules.log.debug("sceKernelWaitEventFlag found timeout micros = " + micros);
            }

            if (!checkEventFlag(event, bits, wait, outBits_addr)) {
                // Failed, but it's ok, just wait a little
                Modules.log.debug("hleKernelWaitEventFlag fast check failed");
                event.numWaitThreads++;

                // Go to wait state
                SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
                currentThread.do_callbacks = do_callbacks;
                currentThread.waitType = PSP_WAIT_MISC;
                currentThread.waitId = uid;

                // Wait on a specific event flag
                threadMan.hleKernelThreadWait(currentThread.wait, micros, (timeout_addr == 0));

                currentThread.wait.waitingOnEventFlag = true;
                currentThread.wait.EventFlag_id = uid;
                currentThread.wait.EventFlag_bits = bits;
                currentThread.wait.EventFlag_wait = wait;
                currentThread.wait.EventFlag_outBits_addr = outBits_addr;

                threadMan.changeThreadState(currentThread, PSP_THREAD_WAITING);
                threadMan.contextSwitch(ThreadMan.getInstance().nextThread());
            } else {
                // Success
                Modules.log.debug("hleKernelWaitEventFlag fast check succeeded");
                Emulator.getProcessor().cpu.gpr[2] = 0;

                if (!threadMan.isInsideCallback()) {
	                // TODO yield anyway? probably yes, at least when do_callbacks is true
	                if (do_callbacks) {
	                	threadMan.yieldCurrentThreadCB();
	                } else {
	                	//threadMan.yieldCurrentThread();
	                }
                } else {
                    Modules.log.warn("hleKernelWaitEventFlag called from inside callback!");
                }
            }
        }
    }

    public void sceKernelWaitEventFlag(int uid, int bits, int wait, int outBits_addr, int timeout_addr)
    {
        Modules.log.debug("sceKernelWaitEventFlag redirecting to hleKernelWaitEventFlag(callbacks=false)");
        hleKernelWaitEventFlag(uid, bits, wait, outBits_addr, timeout_addr, false);
    }

    public void sceKernelWaitEventFlagCB(int uid, int bits, int wait, int outBits_addr, int timeout_addr)
    {
        Modules.log.debug("sceKernelWaitEventFlagCB redirecting to hleKernelWaitEventFlag(callbacks=true)");
        hleKernelWaitEventFlag(uid, bits, wait, outBits_addr, timeout_addr, true);
    }

    public void sceKernelPollEventFlag(int uid, int bits, int wait, int outBits_addr)
    {
        Modules.log.debug("sceKernelPollEventFlag uid=0x" + Integer.toHexString(uid)
            + " bits=0x" + Integer.toHexString(bits)
            + " wait=0x" + Integer.toHexString(wait)
            + " outBits=0x" + Integer.toHexString(outBits_addr));

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            Modules.log.warn("sceKernelPollEventFlag unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            if (!checkEventFlag(event, bits, wait, outBits_addr)) {
                Emulator.getProcessor().cpu.gpr[2] = ERROR_EVENT_FLAG_POLL_FAILED;
            } else {
                Emulator.getProcessor().cpu.gpr[2] = 0;
            }
        }
    }

    public void sceKernelCancelEventFlag(int uid, int newPattern, int result_addr)
    {
        Modules.log.debug("sceKernelCancelEventFlag uid=0x" + Integer.toHexString(uid)
            + " newPattern=0x" + Integer.toHexString(newPattern)
            + " result=0x" + Integer.toHexString(result_addr));

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            Modules.log.warn("sceKernelCancelEventFlag unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            event.currentPattern = newPattern;
            event.numWaitThreads = 0;

            // TODO CANNOT_CANCEL 0x80020261
            Memory mem = Memory.getInstance();
            if (mem.isAddressGood(result_addr))
            	mem.write32(result_addr, 0);

            Emulator.getProcessor().cpu.gpr[2] = 0;
            onEventFlagCancelled(uid);
        }
    }

    public void sceKernelReferEventFlagStatus(int uid, int addr)
    {
        Modules.log.debug("sceKernelReferEventFlagStatus uid=0x" + Integer.toHexString(uid)
            + " addr=0x" + Integer.toHexString(addr));

        SceUidManager.checkUidPurpose(uid, "ThreadMan-eventflag", true);
        SceKernelEventFlagInfo event = eventMap.get(uid);
        if (event == null) {
            Modules.log.warn("sceKernelReferEventFlagStatus unknown uid=0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = ERROR_NOT_FOUND_EVENT_FLAG;
        } else {
            event.write(Memory.getInstance(), addr);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public static final EventFlagManager singleton;

    private EventFlagManager() {
    }

    static {
        singleton = new EventFlagManager();
        singleton.reset();
    }
}
