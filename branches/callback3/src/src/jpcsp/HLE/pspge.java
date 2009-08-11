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
//import java.util.concurrent.Semaphore;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspGeCallbackData;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.HLE.kernel.types.pspGeContext;

import jpcsp.graphics.VideoEngine;

public class pspge {

    private static pspge instance;
    private static final boolean DEFER_CALLBACKS = true;

    private int syncThreadId;
    public volatile boolean waitingForSync;
    public volatile boolean syncDone;
    private HashMap<Integer, SceKernelCallbackInfo> signalCallbacks;
    private HashMap<Integer, SceKernelCallbackInfo> finishCallbacks;

    private ConcurrentLinkedQueue<PspGeList> listQueue;
    private int listIdAllocator;

    private ConcurrentLinkedQueue<DeferredCallbackInfo> deferredSignalCallbackQueue;
    private ConcurrentLinkedQueue<DeferredCallbackInfo> deferredFinishCallbackQueue;
    private ConcurrentLinkedQueue<Integer> deferredThreadWakeupQueue;


    public final static int PSP_GE_LIST_DONE = 0;
    public final static int PSP_GE_LIST_QUEUED = 1;
    public final static int PSP_GE_LIST_DRAWING_DONE = 2;
    public final static int PSP_GE_LIST_STALL_REACHED = 3;
    public final static int PSP_GE_LIST_CANCEL_DONE = 4;

    public static pspge getInstance() {
        if (instance == null) {
            instance = new pspge();
        }
        return instance;
    }

    private pspge() {
    }

    public void Initialise() {

        syncThreadId = -1;
        waitingForSync = false;
        syncDone = false;

        signalCallbacks = new HashMap<Integer, SceKernelCallbackInfo>();
        finishCallbacks = new HashMap<Integer, SceKernelCallbackInfo>();

        listQueue = new ConcurrentLinkedQueue<PspGeList>();
        listIdAllocator = 0;

        deferredSignalCallbackQueue = new ConcurrentLinkedQueue<DeferredCallbackInfo>();
        deferredFinishCallbackQueue = new ConcurrentLinkedQueue<DeferredCallbackInfo>();
        deferredThreadWakeupQueue = new ConcurrentLinkedQueue<Integer>();
    }

    /** call from main emulation thread */
    public void step() {
        ThreadMan threadMan = ThreadMan.getInstance();

        // Hopefully we won't need this, setDirty is called from draw/list sync and when the user clicks Run (unpause)
        //if (VideoEngine.getInstance().hasDrawLists())
        //    pspdisplay.getInstance().setGeDirty(true);

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

        for (Integer thid = deferredThreadWakeupQueue.poll(); thid != null; thid = deferredThreadWakeupQueue.poll()) {
            VideoEngine.log.debug("really waking thread " + Integer.toHexString(thid));
            ThreadMan.getInstance().unblockThread(thid);
        }
    }

    public void sceGeEdramGetSize() {
        Emulator.getProcessor().cpu.gpr[2] = MemoryMap.SIZE_VRAM;
    }

    public void sceGeEdramGetAddr() {
        Emulator.getProcessor().cpu.gpr[2] = MemoryMap.START_VRAM;
    }

    public void sceGeListEnQueue(int list_addr, int stall_addr, int cbid, int arg_addr) {
        VideoEngine.log.debug("sceGeListEnQueue(list=0x" + Integer.toHexString(list_addr)
            + ",stall=0x" + Integer.toHexString(stall_addr)
            + ",cbid=0x" + Integer.toHexString(cbid)
            + ",arg=0x" + Integer.toHexString(arg_addr) + ") result id " + listIdAllocator);

        PspGeList list = new PspGeList(list_addr, stall_addr, cbid, arg_addr);
        list.id = listIdAllocator++;
        listQueue.add(list);

        Emulator.getProcessor().cpu.gpr[2] = list.id;
    }

    public void sceGeListDeQueue(int id) {
        boolean found = false;

        for (Iterator<PspGeList> it = listQueue.iterator(); it.hasNext(); ) {
            PspGeList list = it.next();
            if (list.id == id) {
                it.remove();
                VideoEngine.log.info("sceGeListDeQueue(id=" + id + ") ok");
                Emulator.getProcessor().cpu.gpr[2] = 0;
                found = true;
                break;
            }
        }

        if (!found) {
            VideoEngine.log.warn("sceGeListDeQueue(id=" + id + ") failed");
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
    }

    public void sceGeListUpdateStallAddr(int id, int stall_addr) {
        boolean found = false;

        for (Iterator<PspGeList> it = listQueue.iterator(); it.hasNext(); ) {
            PspGeList list = it.next();
            if (list.id == id) {
                list.stall_addr = stall_addr;
                if (list.currentStatus == PSP_GE_LIST_STALL_REACHED)
                    list.currentStatus = PSP_GE_LIST_QUEUED;
                VideoEngine.log.trace("sceGeListUpdateStallAddr(id=" + id + ",stall=0x" + Integer.toHexString(stall_addr) + ") ok");
                Emulator.getProcessor().cpu.gpr[2] = 0;
                found = true;
                break;
            }
        }

        if (!found) {
            VideoEngine.log.warn("sceGeListUpdateStallAddr(id=" + id + ",stall=0x" + Integer.toHexString(stall_addr) + ") failed");
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
    }

    /** Called from VideoEngine */
    public void hleGeListSyncDone(PspGeList list) {
        if (VideoEngine.log.isDebugEnabled()) {
            String msg = "hleGeListSyncDone list id=" + list.id
                + " current status " + list.currentStatus
                + ", syncType " + list.syncStatus;

            if (list.currentStatus != PSP_GE_LIST_DONE &&
                list.currentStatus != PSP_GE_LIST_CANCEL_DONE) {
                msg += ", NOT discarding list";
            } else {
                msg += ", discarding list";
            }

            if (list.thid > 0) {
                msg += ", waking thread " + Integer.toHexString(list.thid);
            }

            VideoEngine.log.debug(msg);
        }

        if (list.thid > 0) {
            // things might go wrong if the thread already exists in the queue
            deferredThreadWakeupQueue.add(list.thid);
        }

        if (list.currentStatus != PSP_GE_LIST_DONE &&
            list.currentStatus != PSP_GE_LIST_CANCEL_DONE) {
            // list probably stalled, add it back to our queue for reprocessing
            listQueue.add(list);
        }
    }

    /** @return true if the syncType is valid */
    private boolean hleGeListSync(PspGeList list, int syncType) {
        list.syncStatus = syncType;
        pspdisplay.getInstance().setGeDirty(true);

        // try syncing to done -> ok, draw
        // allow queued to behave the same as done, may need changing later
        if (syncType == PSP_GE_LIST_DONE ||
            syncType == PSP_GE_LIST_QUEUED) {
            // current status queued -> ok, draw
            if (list.currentStatus == PSP_GE_LIST_QUEUED) {
                list.currentStatus = PSP_GE_LIST_DRAWING_DONE;
                VideoEngine.getInstance().pushDrawList(list);
                VideoEngine.log.debug("hleGeListSync(id=" + list.id + ",syncType=" + syncType + ") ok");
                return true;
            } else {
                // list probably ended, can't draw again
                VideoEngine.log.info("hleGeListSync(id=" + list.id + ",syncType=" + syncType + ") failed currentStatus=" + list.currentStatus);
                return false;
            }
        //} else if (syncType == PSP_GE_LIST_QUEUED) {
        //    VideoEngine.log.info("hleGeListSync(id=" + list.id + ",syncType=" + syncType + ") ignoring syncType=1");
        //    return false;
        } else {
            // TODO allow PSP_GE_LIST_DRAWING_DONE(2) as a non-blocking sync ?
            VideoEngine.log.warn("hleGeListSync(id=" + list.id + ",syncType=" + syncType + ") unhandled syncType");
            return false;
        }
    }

    public void sceGeListSync(int id, int syncType) {
        boolean found = false;

        for (Iterator<PspGeList> it = listQueue.iterator(); it.hasNext(); ) {
            PspGeList list = it.next();
            if (list.id == id) {
                ThreadMan threadMan = ThreadMan.getInstance();
                Emulator.getProcessor().cpu.gpr[2] = 0;

                if (hleGeListSync(list, syncType)) {
                    list.thid = threadMan.getCurrentThreadID();
                    VideoEngine.log.debug("sceGeListSync(id=" + id + ",syncType=" + syncType + ") blocking thread " + Integer.toHexString(list.thid));
                    threadMan.blockCurrentThread();
                    it.remove();
                } else {
                    threadMan.yieldCurrentThread();
                    // delete on failed syncs it.remove();
                }

                found = true;
                break;
            }
        }

        if (!found) {
            VideoEngine.log.warn("sceGeListSync(id=" + id + ",syncType=" + syncType + ") failed (list not found, last allocated id=" + listIdAllocator + ")");
            Emulator.getProcessor().cpu.gpr[2] = -1;
        }
    }

    public void sceGeDrawSync(int syncType) {
        /* old
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
        */

        boolean wait = false;

        PspGeList lastList = null;
        for (Iterator<PspGeList> it = listQueue.iterator(); it.hasNext(); ) {
            PspGeList list = it.next();
            if (hleGeListSync(list, syncType)) {
                lastList = list;
                wait = true;
                it.remove();
            }
            // delete on failed syncs it.remove();
        }

        ThreadMan threadMan = ThreadMan.getInstance();
        Emulator.getProcessor().cpu.gpr[2] = 0;

        if (wait && lastList != null) {
            lastList.thid = threadMan.getCurrentThreadID();
            VideoEngine.log.debug("sceGeDrawSync(syncType=" + syncType + ") blocking thread " + Integer.toHexString(lastList.thid) + " on list id=" + lastList.id);
            threadMan.blockCurrentThread();
        } else {
            threadMan.yieldCurrentThread();
        }
    }

    public void sceGeSaveContext(int contextAddr) {
    	pspGeContext context = new pspGeContext();
    	VideoEngine.getInstance().saveContext(context);
    	context.write(Memory.getInstance(), contextAddr);

    	Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceGeRestoreContext(int contextAddr) {
    	pspGeContext context = new pspGeContext();
    	context.read(Memory.getInstance(), contextAddr);
    	VideoEngine.getInstance().restoreContext(context);

    	Emulator.getProcessor().cpu.gpr[2] = 0;
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

        // update: restored use of 3rd arg, needs fixing properly, or
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
        Modules.log.debug("sceGeUnsetCallback cbid=" + Integer.toHexString(cbid));
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
            threadMan.pushGeCallback(callbackIndex, callback.uid, callbackNotifyArg1, callback.callback_arg_addr);
        }
    }

    /** safe to call from outside the main emulation thread if DEFER_CALLBACKS is true */
    public void triggerFinishCallback(int cbid, int callbackNotifyArg1) {
        if (DEFER_CALLBACKS)
            deferredFinishCallbackQueue.offer(new DeferredCallbackInfo(cbid, callbackNotifyArg1));
        else
            triggerCallback(cbid, callbackNotifyArg1, SceKernelThreadInfo.THREAD_CALLBACK_GE_FINISH, finishCallbacks);

    }

    /** safe to call from outside the main emulation thread if DEFER_CALLBACKS is true */
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
