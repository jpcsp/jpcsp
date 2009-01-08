/*
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/pspge_8h.html


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
package jpcsp.HLE;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspGeCallbackData;
import jpcsp.graphics.DisplayList;
import jpcsp.graphics.VideoEngine;

public class pspge {

    private static pspge instance;
    private static final boolean DEFER_CALLBACKS = true;

    private int syncThreadId;
    public volatile boolean waitingForSync;
    public volatile boolean syncDone;
    private HashMap<Integer, SceKernelCallbackInfo> signalCallbacks;
    private HashMap<Integer, SceKernelCallbackInfo> finishCallbacks;
    private volatile ConcurrentLinkedQueue<DeferredCallbackInfo> deferredSignalCallbackQueue;
    private volatile ConcurrentLinkedQueue<DeferredCallbackInfo> deferredFinishCallbackQueue;

    public static pspge getInstance() {
        if (instance == null) {
            instance = new pspge();
        }
        return instance;
    }

    private pspge() {
    }

    public void Initialise() {
        DisplayList.Lock();
        DisplayList.Initialise();
        DisplayList.Unlock();

        syncThreadId = -1;
        waitingForSync = false;
        syncDone = false;

        signalCallbacks = new HashMap<Integer, SceKernelCallbackInfo>();
        finishCallbacks = new HashMap<Integer, SceKernelCallbackInfo>();

        deferredSignalCallbackQueue = new ConcurrentLinkedQueue<DeferredCallbackInfo>();
        deferredFinishCallbackQueue = new ConcurrentLinkedQueue<DeferredCallbackInfo>();
    }

    /** call from main emulation thread */
    public void step() {
        ThreadMan threadMan = ThreadMan.getInstance();

        if (waitingForSync) {
            if (syncDone) {
                VideoEngine.log.debug("syncDone");
                threadMan.unblockThread(syncThreadId);
                waitingForSync = false;
                syncDone = false;
            } else {
                // I don't like this...
                pspdisplay.getInstance().setDirty(true);
            }
        }

        // Process deferred callbacks
        // TODO if these callbacks block GE we have more work to do...
        if (DEFER_CALLBACKS && !threadMan.isInsideCallback()) {
            DeferredCallbackInfo info = deferredFinishCallbackQueue.poll();
            if (info != null) {
                triggerCallback(info.cbid, info.callbackNotifyArg1, SceKernelThreadInfo.THREAD_CALLBACK_GE_FINISH, finishCallbacks);
            }

            info = deferredSignalCallbackQueue.poll();
            if (info != null) {
                triggerCallback(info.cbid, info.callbackNotifyArg1, SceKernelThreadInfo.THREAD_CALLBACK_GE_SIGNAL, signalCallbacks);
            }
        }
    }

    public void sceGeEdramGetSize() {
        Emulator.getProcessor().cpu.gpr[2] = MemoryMap.SIZE_VRAM;
    }

    public void sceGeEdramGetAddr() {
        Emulator.getProcessor().cpu.gpr[2] = MemoryMap.START_VRAM;
    }

    public void sceGeListEnQueue(int list, int stall, int callbackId, int argument) {
        DisplayList.Lock();

        /*
        list 	- The head of the list to queue.
        stall 	- The stall address. If NULL then no stall address set and the list is transferred immediately.
        cbid 	- ID of the callback set by calling sceGeSetCallback
        arg 	- Probably a parameter to the callbacks (to be confirmed)
        */

        // remove uncache bit
        list &= 0x3fffffff;
        stall &= 0x3fffffff;

        if (Memory.getInstance().isAddressGood(list)) {
            DisplayList displayList = new DisplayList(list, stall, callbackId, argument);
            DisplayList.addDisplayList(displayList);
            VideoEngine.log.debug("New list " + displayList.toString());

            if (displayList.status == DisplayList.QUEUED)
                pspdisplay.getInstance().setDirty(true);

            Emulator.getProcessor().cpu.gpr[2] = displayList.id;
        } else {
            VideoEngine.log.error("sceGeListEnQueue bad address 0x" + Integer.toHexString(stall));
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
        DisplayList.Unlock();
    }

    public void sceGeListDeQueue(int qid) {
        DisplayList.Lock();
        // TODO if we render asynchronously, using another thread then we need to interupt it first
        if (DisplayList.removeDisplayList(qid)) {
            VideoEngine.log.debug("sceGeListDeQueue qid=" + qid);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            VideoEngine.log.error("sceGeListDeQueue failed qid=" + qid);
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
        DisplayList.Unlock();
    }

    public void sceGeListUpdateStallAddr(int qid, int stallAddress) {
        DisplayList.Lock();
        DisplayList displayList = DisplayList.getDisplayList(qid);
        if (displayList != null) {
            // remove uncache bit
            stallAddress &= 0x3fffffff;

            VideoEngine.log.trace("sceGeListUpdateStallAddr qid=" + qid
                + " addr:" + String.format("%08x", stallAddress)
                + " approx " + ((stallAddress - displayList.stallAddress) / 4) + " new commands");

            displayList.stallAddress = stallAddress;
            if (displayList.pc != displayList.stallAddress) {
                displayList.status = DisplayList.QUEUED;
                pspdisplay.getInstance().setDirty(true);
            }

            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else {
            VideoEngine.log.error("sceGeListUpdateStallAddr qid="+ qid +" failed, no longer exists");
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
        DisplayList.Unlock();
    }

    // TODO handle sync type
    public void sceGeDrawSync(int syncType) {
        if (syncType == 0 || syncType == 1) {
            VideoEngine.log.debug("sceGeDrawSync syncType=" + syncType);
        } else {
            VideoEngine.log.warn("sceGeDrawSync syncType=" + syncType); // unhandled
        }

        Emulator.getProcessor().cpu.gpr[2] = 0;

        if (!pspdisplay.getInstance().disableGE) {
            if (syncType == DisplayList.QUEUED) {
                // We always queue straight away so I guess we can return straight away? (fiveofhearts)
                ThreadMan.getInstance().yieldCurrentThread();
            } else {
                int count = 0;
                DisplayList.Lock();
                for (Iterator<DisplayList> it = DisplayList.iterator(); it.hasNext();) {
                    DisplayList list = it.next();
                    if (list.status == DisplayList.QUEUED)
                        count++;
                }
                DisplayList.Unlock();

                // Don't block if there's nothing to block on
                if (count > 0) {
                    if (waitingForSync)
                        VideoEngine.log.warn("sceGeDrawSync called again before previous call finished syncing");

                    waitingForSync = true;
                    syncThreadId = ThreadMan.getInstance().getCurrentThreadID();
                    ThreadMan.getInstance().blockCurrentThread();
                } else {
                    VideoEngine.log.debug("sceGeDrawSync no queued lists, ignoring");
                    ThreadMan.getInstance().yieldCurrentThread();
                }
            }
        } else {
            ThreadMan.getInstance().yieldCurrentThread();
        }
    }

    public void sceGeSetCallback(int cbdata_addr) {
        pspGeCallbackData cbdata = new pspGeCallbackData();
        cbdata.read(Emulator.getMemory(), cbdata_addr);
        int cbid = SceUidManager.getNewUid("pspge-callback");
        Modules.log.debug("sceGeSetCallback signalFunc=0x" + Integer.toHexString(cbdata.signalFunction)
                              + ", signalArg=0x" + Integer.toHexString(cbdata.signalArgument)
                              + ", finishFunc=0x" + Integer.toHexString(cbdata.finishFunction)
                              + ", finishArg=0x" + Integer.toHexString(cbdata.finishArgument)
                              + ", result cbid=" + Integer.toHexString(cbid));

        // HACK using kernel callback mechanism to trigger the ge callback
        // could be a bad idea since $a2 will get clobbered. ge callback has 2
        // args, kernel callback has 3. setting the 3rd arg to 0x12121212 so we
        // can detect errors caused by this more easily.

        // update: restored use of 3rd, needs fixing properly, or
        // checking on real psp (since it's possible pspsdk is wrong).

        ThreadMan threadMan = ThreadMan.getInstance();
        SceKernelCallbackInfo callbackSignal = threadMan.hleKernelCreateCallback("GeCallbackSignal", cbdata.signalFunction, cbdata.signalArgument);
        SceKernelCallbackInfo callbackFinish = threadMan.hleKernelCreateCallback("GeCallbackFinish", cbdata.finishFunction, cbdata.finishArgument);
        signalCallbacks.put(cbid, callbackSignal);
        finishCallbacks.put(cbid, callbackFinish);
        threadMan.setCallback(SceKernelThreadInfo.THREAD_CALLBACK_GE_SIGNAL, callbackSignal.uid);
        threadMan.setCallback(SceKernelThreadInfo.THREAD_CALLBACK_GE_FINISH, callbackFinish.uid);

        Emulator.getProcessor().cpu.gpr[2] = cbid;
    }

    public void sceGeUnsetCallback(int cbid) {
        Modules.log.debug("sceGeUnsetCallback cbid=" + cbid);
        ThreadMan threadMan = ThreadMan.getInstance();
        SceKernelCallbackInfo callbackSignal = signalCallbacks.remove(cbid);
        SceKernelCallbackInfo callbackFinish = finishCallbacks.remove(cbid);
        if (callbackSignal != null) {
            threadMan.clearCallback(SceKernelThreadInfo.THREAD_CALLBACK_GE_SIGNAL, callbackSignal.uid);
            threadMan.hleKernelDeleteCallback(callbackSignal.uid);
        }
        if (callbackFinish != null) {
            threadMan.clearCallback(SceKernelThreadInfo.THREAD_CALLBACK_GE_FINISH, callbackFinish.uid);
            threadMan.hleKernelDeleteCallback(callbackFinish.uid);
        }
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    private void triggerCallback(int cbid, int callbackNotifyArg1, int callbackIndex, HashMap<Integer, SceKernelCallbackInfo> callbacks) {
        SceKernelCallbackInfo callback = callbacks.get(cbid);
        if (callback != null) {
            if (VideoEngine.log.isDebugEnabled()) {
                VideoEngine.log.debug("Triggering callback " + callbackIndex + ", addr=0x" + Integer.toHexString(callback.callback_addr) + ", cbid=" + Integer.toHexString(cbid) + ", callback notify arg=0x" + Integer.toHexString(callbackNotifyArg1));
            }
            ThreadMan threadMan = ThreadMan.getInstance();

            // HACK push GE callback using Kernel callback code
            threadMan.pushCallback(callbackIndex, callback.uid, callbackNotifyArg1, callback.callback_arg_addr);
        }
    }

    /** safe to call from outside the main emulation thread */
    public void triggerFinishCallback(int cbid, int callbackNotifyArg1) {
        if (DEFER_CALLBACKS)
            deferredFinishCallbackQueue.offer(new DeferredCallbackInfo(cbid, callbackNotifyArg1));
        else
            triggerCallback(cbid, callbackNotifyArg1, SceKernelThreadInfo.THREAD_CALLBACK_GE_FINISH, finishCallbacks);

    }

    /** safe to call from outside the main emulation thread */
    public void triggerSignalCallback(int cbid, int callbackNotifyArg1) {
        if (DEFER_CALLBACKS)
            deferredSignalCallbackQueue.offer(new DeferredCallbackInfo(cbid, callbackNotifyArg1));
        else
            triggerCallback(cbid, callbackNotifyArg1, SceKernelThreadInfo.THREAD_CALLBACK_GE_SIGNAL, signalCallbacks);
    }

    class DeferredCallbackInfo {
        public final int cbid;
        public final int callbackNotifyArg1;

        public DeferredCallbackInfo(int cbid, int callbackNotifyArg1) {
            this.cbid = cbid;
            this.callbackNotifyArg1 = callbackNotifyArg1;
        }
    }
}
