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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;
import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.*;
import jpcsp.HLE.Modules;
import jpcsp.HLE.pspSysMem;
import jpcsp.HLE.ThreadMan;
import jpcsp.Allegrex.CpuState;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.util.Utilities;

public class CallbackManager {

    private HashMap<Integer, SceKernelCallbackInfo> callbackMap;
    private List<SceKernelCallbackInfo> readyCallbacks;
    private boolean dirty;

    public void reset() {
        callbackMap = new HashMap<Integer, SceKernelCallbackInfo>();
        readyCallbacks = new ArrayList<SceKernelCallbackInfo>();
        dirty = false;
    }

    public void step() {
        if (dirty) {
            //Modules.log.info("hleKernelCheckCallback (from step)");
            hleKernelCheckCallback(false);
            //Modules.log.info("hleKernelCheckCallback (from step) DONE");
        }
    }

    // Helpers

    public boolean isCallbackUid(int uid) {
        return callbackMap.containsKey(uid);
    }

    /** @return the initialised callback info. */
    public SceKernelCallbackInfo hleKernelCreateCallback(String name, int func_addr, int user_arg_addr) {
        ThreadMan threadMan = ThreadMan.getInstance();
        SceKernelThreadInfo thread = threadMan.getCurrentThread();


        SceKernelCallbackInfo callback = new SceKernelCallbackInfo(name, thread.uid, func_addr, user_arg_addr);
        callbackMap.put(callback.uid, callback);

        Modules.log.debug("hleKernelCreateCallback SceUID=" + Integer.toHexString(callback.uid)
            + " name:'" + name
            + "' PC=" + Integer.toHexString(func_addr)
            + " arg=" + Integer.toHexString(user_arg_addr)
            + " thread:'" + thread.name + "'");

        return callback;
    }

    /** @return true if successful. */
    public boolean hleKernelDeleteCallback(int uid) {
        String msg = "hleKernelDeleteCallback(uid=" + Integer.toHexString(uid) + ")";
        boolean success;

        SceKernelCallbackInfo info = callbackMap.remove(uid);
        if (info == null) {
            Modules.log.warn(msg + " unknown uid");
            success = false;
        } else {
            Modules.log.debug(msg);
            hleKernelCancelCallback(info);
            success = true;
        }

        return success;
    }

    private void hleKernelCancelCallback(SceKernelCallbackInfo info) {
        if (readyCallbacks.contains(info)) {
            info.notifyCount = 0;

            // unmark thread
            ThreadMan threadMan = ThreadMan.getInstance();
            SceKernelThreadInfo thread = threadMan.getThreadById(info.threadId);
            thread.numWaitCallbacks--;

            // remove from ready list
            readyCallbacks.remove(info);
        }
    }

    /**
     * @param  forceNotify : Allow the callback to trigger even if no threads are in waitCB. For use by GE (it's probably interrupt driven).
     * @return true        : on success. */
    public boolean hleKernelNotifyCallbackCommon(int uid, int arg1, int arg2, boolean forceNotify) {
        String msg = "hleKernelNotifyCallbackCommon(uid=" + Integer.toHexString(uid)
            + ",arg1=0x" + Integer.toHexString(arg1)
            + ",arg2=0x" + Integer.toHexString(arg2) + ")"
            + " forceNotify=" + forceNotify;
        boolean success;

        SceKernelCallbackInfo info = callbackMap.get(uid);
        if (info == null) {
            Modules.log.warn(msg + " unknown uid");
            success = false;
        } else {
            // spams on GE finish callback
            if (Modules.log.isDebugEnabled()) {
                Modules.log.debug(msg);
            }

            // update callback
            info.notifyCount = arg1;
            info.notifyArg = arg2;

            // Ignore "forceNotify" for now because previous implementation didn't pre-empt it worked ok
            // If we do want faster response we could "pre-empt" in any syscall, not just waitCB syscalls
            //info.forceNotify = forceNotify;
            info.forceNotify = false;

            // add to ready list
            if (!readyCallbacks.contains(info)) {
                //Modules.log.info("hleKernelNotifyCallbackCommon adding to ready list");

                // mark thread
                ThreadMan threadMan = ThreadMan.getInstance();
                SceKernelThreadInfo thread = threadMan.getThreadById(info.threadId);
                thread.numWaitCallbacks++;

                // add to ready list
                readyCallbacks.add(info);
            }

            // check callbacks on next step
            dirty = true;

            success = true;
        }

        return success;
    }

    /**
     * You may want to call hleKernelCheckCallback after calling this.
     * Redirects to hleKernelNotifyCallbackCommon.
     * @return true on success. */
    public boolean hleKernelNotifyCallback(int uid, int notifyArg) {
        String msg = "hleKernelNotifyCallback(uid=" + Integer.toHexString(uid)
            + ",notifyArg=0x" + Integer.toHexString(notifyArg) + ")";
        boolean success;

        SceKernelCallbackInfo info = callbackMap.get(uid);
        if (info == null) {
            Modules.log.warn(msg + " unknown uid");
            success = false;
        } else {
            Modules.log.debug(msg);
            success = hleKernelNotifyCallbackCommon(uid, info.notifyCount + 1, notifyArg, false);
        }

        return success;
    }

    public void hleKernelExitCallback() {
        ThreadMan threadMan = ThreadMan.getInstance();
        SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

        Modules.log.info("Callback V3 exit detected");

        // Restore thread context
        currentThread.restoreContext(); // also sets pc = npc

        // housekeeping
        currentThread.insideCallbackV3 = false;

        // Go back to a ready thread
        threadMan.contextSwitch(threadMan.nextThread());

        // Try another callback
        currentThread = threadMan.getCurrentThread();
        hleKernelCheckCallback(currentThread.forceAllowCallbacks);
    }

    /**
     * If a callback is successfully entered, then this function will get
     * called again automatically when the callback exits, forming a loop.
     *
     * @param  allowCurrentThread : true  - sceKernelCheckCallback or waitCB,
     *                              false - hleKernelNotifyCallback.
     * @return true  - entered a callback,
     *         false - no callbacks to enter, nothing happened.
     */
    public boolean hleKernelCheckCallback(boolean allowCurrentThread) {
        ThreadMan threadMan = ThreadMan.getInstance();
        SceKernelThreadInfo currentThread = threadMan.getCurrentThread();
        boolean handled = false;

        if (false) {
            Modules.log.info("hleKernelCheckCallback thread:'" + currentThread.name
                + "' allowCurrentThread=" + allowCurrentThread
                + " readyCallbacks=" + readyCallbacks.size());
        }

        dirty = false;
        currentThread.forceAllowCallbacks = allowCurrentThread;

        for (int i = 0; i < readyCallbacks.size(); i++) {
            SceKernelCallbackInfo info = readyCallbacks.get(i);
            SceKernelThreadInfo callbackThread = threadMan.getThreadById(info.threadId);
            if (!callbackThread.insideCallbackV3 &&
                (callbackThread.allowCallbacks || callbackThread.forceAllowCallbacks || info.forceNotify)) {

                // consistency check
                // when sleepCB and delayCB are called on a thread it will have allowCallbacks set to true and perform an initial checkCB
                // either if-statement should work
                if (callbackThread.allowCallbacks &&
                    callbackThread == currentThread &&
                    callbackThread.waitType != SceKernelThreadInfo.PSP_WAIT_SLEEP &&
                    callbackThread.waitType != SceKernelThreadInfo.PSP_WAIT_DELAY) {
                //if (callbackThread.allowCallbacks && info.threadId == currentThread.uid) {
                    Modules.log.error("current thread '" + currentThread.name + "' should not have allowCallbacks set!");
                }

                // Switch out current thread
                // Switch in callback thread
                // either if-statement should work
                if (callbackThread != currentThread) {
                //if (info.threadId != currentThread.uid) {

                    if (Modules.log.isDebugEnabled()) {
                        // spams on GE finish callback
                        Modules.log.debug("hleKernelCheckCallback switching in CB thread '" + currentThread.name + "' -> '" + callbackThread.name + "'");
                    }

                    threadMan.contextSwitch(callbackThread);
                } else {
                    if (Modules.log.isDebugEnabled()) {
                        // spams on GE finish callback
                        Modules.log.debug("hleKernelCheckCallback CB thread '" + callbackThread.name + "' already current thread");
                    }
                }

                // Save thread context
                callbackThread.saveContext();

                // Set callback context
                // we run in the thread the callback was created in
                // even if it's the same as the current thread we still need to setup the function parameters
                info.startContext(callbackThread);

                // housekeeping
                callbackThread.numWaitCallbacks--;
                callbackThread.insideCallbackV3 = true;
                // info.notifyCount = 0; // already done in info.startContext()
                readyCallbacks.remove(i); // this operation is safe because we're going to break out of the loop

                handled = true;
                break;
            }
        }

        if (!handled) {
            // No more ready callbacks, reset this flag
            currentThread.forceAllowCallbacks = false;
        }

        return handled;
    }

    // HLE/FW

    public void sceKernelCreateCallback(int name_addr, int func_addr, int user_arg_addr) {
        String name = Utilities.readStringNZ(name_addr, 32);
        SceKernelCallbackInfo callback = hleKernelCreateCallback(name, func_addr, user_arg_addr);

        Emulator.getProcessor().cpu.gpr[2] = callback.uid;
    }

    public void sceKernelDeleteCallback(int uid) {
        if (hleKernelDeleteCallback(uid)) {
            // TODO automatically unregister the callback if it was registered with another system?
            // example: sceKernelDeleteCallback called before sceUmdUnRegisterUMDCallBack
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
    }

    /** Check callbacks, including those on the current thread */
    public void sceKernelCheckCallback() {
        Modules.log.debug("sceKernelCheckCallback");
        Emulator.getProcessor().cpu.gpr[2] = 0;
        hleKernelCheckCallback(true);
    }

    public void sceKernelNotifyCallback(int uid, int notifyArg) {
        if (hleKernelNotifyCallback(uid, notifyArg)) {
            // success
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            // failed
            Emulator.getProcessor().cpu.gpr[2] = -1; // TODO
        }
    }

    public void sceKernelCancelCallback(int uid) {
        String msg = "sceKernelCancelCallback(uid=" + Integer.toHexString(uid) + ")";

        SceKernelCallbackInfo info = callbackMap.get(uid);
        if (info == null) {
            Modules.log.warn(msg + " unknown uid");
            Emulator.getProcessor().cpu.gpr[2] = -1; // TODO
        } else {
            Modules.log.debug(msg);
            hleKernelCancelCallback(info);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceKernelGetCallbackCount(int uid) {
        String msg = "sceKernelGetCallbackCount(uid=" + Integer.toHexString(uid) + ")";

        SceKernelCallbackInfo info = callbackMap.get(uid);
        if (info == null) {
            Modules.log.warn(msg + " unknown uid");
            Emulator.getProcessor().cpu.gpr[2] = -1; // TODO
        } else {
            Modules.log.debug(msg + " ret:" + info.notifyCount);
            Emulator.getProcessor().cpu.gpr[2] = info.notifyCount; // check
        }
    }

    public void sceKernelReferCallbackStatus(int uid, int info_addr) {
        Modules.log.debug("sceKernelReferCallbackStatus SceUID=" + Integer.toHexString(uid)
            + " info=" + Integer.toHexString(info_addr));

        Memory mem = Memory.getInstance();
        SceKernelCallbackInfo info = callbackMap.get(uid);
        if (info == null) {
            Modules.log.warn("sceKernelReferCallbackStatus unknown uid 0x" + Integer.toHexString(uid));
            Emulator.getProcessor().cpu.gpr[2] = -1;
        } else if (!mem.isAddressGood(info_addr)) {
            Modules.log.warn("sceKernelReferCallbackStatus bad info address 0x" + Integer.toHexString(info_addr));
            Emulator.getProcessor().cpu.gpr[2] = -1;
        } else {
            int size = mem.read32(info_addr);
            if (size != SceKernelCallbackInfo.size) {
                Modules.log.warn("sceKernelReferCallbackStatus bad info size got " + size + " want " + SceKernelCallbackInfo.size);
                Emulator.getProcessor().cpu.gpr[2] = -1;
            } else {
                info.write(mem, info_addr);
                Emulator.getProcessor().cpu.gpr[2] = 0;
            }
        }
    }

    // Singleton

    public static final CallbackManager singleton;

    private CallbackManager() {
    }

    static {
        singleton = new CallbackManager();
    }
}
