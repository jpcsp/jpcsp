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
import java.util.concurrent.ConcurrentLinkedQueue;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspGeCallbackData;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.HLE.kernel.types.pspGeContext;
import jpcsp.HLE.kernel.types.interrupts.GeCallbackInterruptHandler;
import jpcsp.HLE.kernel.types.interrupts.GeInterruptHandler;

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;

public class pspge {

    private static pspge instance;

    public volatile boolean waitingForSync;
    public volatile boolean syncDone;
    private HashMap<Integer, SceKernelCallbackInfo> signalCallbacks;
    private HashMap<Integer, SceKernelCallbackInfo> finishCallbacks;

    // PSP has an array of 64 GE lists
    private static final int NUMBER_GE_LISTS = 64;
    private PspGeList[] allGeLists;
    private ConcurrentLinkedQueue<PspGeList> listFreeQueue;

    private ConcurrentLinkedQueue<Integer> deferredThreadWakeupQueue;

    public final static int PSP_GE_LIST_DONE = 0;
    public final static int PSP_GE_LIST_QUEUED = 1;
    public final static int PSP_GE_LIST_DRAWING = 2;
    public final static int PSP_GE_LIST_STALL_REACHED = 3;
    public final static int PSP_GE_LIST_END_REACHED = 4;
    public final static int PSP_GE_LIST_CANCEL_DONE = 5;
    public final static String[] PSP_GE_LIST_STRINGS = {
    	"PSP_GE_LIST_DONE",
    	"PSP_GE_LIST_QUEUED",
    	"PSP_GE_LIST_DRAWING",
    	"PSP_GE_LIST_STALL_REACHED",
    	"PSP_GE_LIST_END_REACHED",
    	"PSP_GE_LIST_CANCEL_DONE"
    };

    public final static int PSP_GE_BEHAVIOR_SUSPEND  = 1;
    public final static int PSP_GE_BEHAVIOR_CONTINUE = 2;
    public final static int PSP_GE_BEHAVIOR_BREAK    = 3;

    public static pspge getInstance() {
        if (instance == null) {
            instance = new pspge();
        }
        return instance;
    }

    private pspge() {
    }

    public void Initialise() {
        waitingForSync = false;
        syncDone = false;

        signalCallbacks = new HashMap<Integer, SceKernelCallbackInfo>();
        finishCallbacks = new HashMap<Integer, SceKernelCallbackInfo>();

        listFreeQueue = new ConcurrentLinkedQueue<PspGeList>();
    	allGeLists = new PspGeList[NUMBER_GE_LISTS];
    	for (int i = 0; i < NUMBER_GE_LISTS; i++) {
    		allGeLists[i] = new PspGeList(i);
    		listFreeQueue.add(allGeLists[i]);
    	}

        deferredThreadWakeupQueue = new ConcurrentLinkedQueue<Integer>();
    }

    /** call from main emulation thread */
    public void step() {
        ThreadMan threadMan = ThreadMan.getInstance();

        for (Integer thid = deferredThreadWakeupQueue.poll(); thid != null; thid = deferredThreadWakeupQueue.poll()) {
        	if (VideoEngine.log.isDebugEnabled()) {
        		VideoEngine.log.debug("really waking thread " + Integer.toHexString(thid) + "(" + ThreadMan.getInstance().getThreadName(thid) + ")");
        	}
            threadMan.unblockThread(thid);
        }
    }

    public void sceGeEdramGetSize() {
        Emulator.getProcessor().cpu.gpr[2] = MemoryMap.SIZE_VRAM;
    }

    public void sceGeEdramGetAddr() {
        Emulator.getProcessor().cpu.gpr[2] = MemoryMap.START_VRAM;
    }

    public synchronized void sceGeListEnQueue(int list_addr, int stall_addr, int cbid, int arg_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (VideoEngine.log.isDebugEnabled()) {
	        VideoEngine.log.debug("sceGeListEnQueue(list=0x" + Integer.toHexString(list_addr)
	            + ",stall=0x" + Integer.toHexString(stall_addr)
	            + ",cbid=0x" + Integer.toHexString(cbid)
	            + ",arg=0x" + Integer.toHexString(arg_addr) + ")");
    	}

    	list_addr &= Memory.addressMask;
    	stall_addr &= Memory.addressMask;

    	if (VideoEngine.getInstance().hasDrawList(list_addr)) {
    		cpu.gpr[2] = 0x80000021;
    		VideoEngine.log.warn("sceGeListEnQueue can't enqueue duplicate list address");
    	} else {
	    	PspGeList list = listFreeQueue.poll();
	    	if (list == null) {
	    		cpu.gpr[2] = 0x80000022;
	    		VideoEngine.log.warn("sceGeListEnQueue no more free list available!");
	    	} else {
	    		list.init(list_addr, stall_addr, cbid, arg_addr);
	    		startGeList(list);
	            cpu.gpr[2] = list.id;
	    	}
    	}

		if (VideoEngine.log.isDebugEnabled()) {
			VideoEngine.log.debug(String.format("sceGeListEnQueue returning 0x%x", cpu.gpr[2]));
		}
    }

    public synchronized void sceGeListDeQueue(int id) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (VideoEngine.log.isDebugEnabled()) {
        	VideoEngine.log.debug("sceGeListDeQueue(id=" + id + ")");
        }

        if (id < 0 || id >= NUMBER_GE_LISTS) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_LIST_ID;
        } else {
        	PspGeList list = allGeLists[id];
        	list.reset();
        	if (!listFreeQueue.contains(list)) {
        		listFreeQueue.add(list);
        	}
            cpu.gpr[2] = 0;
        }
    }

    public synchronized void sceGeListUpdateStallAddr(int id, int stall_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (VideoEngine.log.isDebugEnabled()) {
        	VideoEngine.log.debug(String.format("sceGeListUpdateStallAddr(id=%d, stall=0x%08X)", id, stall_addr));
        }

        stall_addr &= Memory.addressMask;

        if (id < 0 || id >= NUMBER_GE_LISTS) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_LIST_ID;
        } else {
        	PspGeList list = allGeLists[id];
        	if (list.getStallAddr() != stall_addr) {
        		list.setStallAddr(stall_addr);
                pspdisplay.getInstance().setGeDirty(true);
        	}
            cpu.gpr[2] = 0;
        }
    }

    /** Called from VideoEngine */
    public synchronized void hleGeListSyncDone(PspGeList list) {
        if (VideoEngine.log.isDebugEnabled()) {
            String msg = "hleGeListSyncDone list " + list;

            if (list.isDone()) {
                msg += ", done";
            } else {
                msg += ", NOT done";
            }

            if (list.thid > 0 && list.status != PSP_GE_LIST_END_REACHED) {
                msg += ", waking thread " + Integer.toHexString(list.thid);
            }

            VideoEngine.log.debug(msg);
        }

        if (list.thid > 0 && list.status != PSP_GE_LIST_END_REACHED) {
            // things might go wrong if the thread already exists in the queue
            deferredThreadWakeupQueue.add(list.thid);
        }

        if (list.isDone()) {
        	listFreeQueue.add(list);
        }
    }

    public void hleGeOnAfterCallback(int listId, int behavior) {
		// (gid15) I could not make any difference between
		//    PSP_GE_BEHAVIOR_CONTINUE and PSP_GE_BEHAVIOR_SUSPEND
		// Both wait for the completion of the callback before continuing
		// the list processing...
		if (behavior == PSP_GE_BEHAVIOR_CONTINUE || behavior == PSP_GE_BEHAVIOR_SUSPEND) {
			if (listId >= 0 && listId < NUMBER_GE_LISTS) {
				PspGeList list = allGeLists[listId];
				if (VideoEngine.log.isDebugEnabled()) {
					VideoEngine.log.debug("hleGeOnAfterCallback restarting list " + list);
				}
				list.restartList();
			}
		}
    }

    private void startGeList(PspGeList list) {
    	pspdisplay.getInstance().setGeDirty(true);
    	list.startList();
    }

    public synchronized void sceGeListSync(int id, int mode) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (VideoEngine.log.isDebugEnabled()) {
        	VideoEngine.log.debug("sceGeListSync(id=" + id + ",mode=" + mode + ")");
        }

        if (mode != 0 && mode != 1) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_ARGUMENT;
        } else if (id < 0 || id >= NUMBER_GE_LISTS) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_LIST_ID;
        } else if (mode == 0 && IntrManager.getInstance().isInsideInterrupt()) {
    		VideoEngine.log.warn("sceGeListSync mode=0 called inside an Interrupt!");
    		cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
        	PspGeList list = allGeLists[id];
        	if (VideoEngine.log.isDebugEnabled()) {
            	VideoEngine.log.debug("sceGeListSync on list: " + list);
        	}

        	if (list.isReset()) {
        		cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_LIST_ID;
        	} else if (mode == 0 && !list.isDone()) {
        		cpu.gpr[2] = 0;
        		blockCurrentThreadOnList(list, null);
        	} else {
        		cpu.gpr[2] = list.status;
        	}
        }
    }

    private void blockCurrentThreadOnList(PspGeList list, IAction action) {
        ThreadMan threadMan = ThreadMan.getInstance();
        list.thid = threadMan.getCurrentThreadID();
    	if (VideoEngine.log.isDebugEnabled()) {
    		VideoEngine.log.debug("blockCurrentThreadOnList blocking thread " + Integer.toHexString(list.thid) + " on list " + list);
    	}
        threadMan.blockCurrentThreadCB(action);
        //threadMan.checkCallbacks();
    }

    // sceGeDrawSync is resetting all the lists having status PSP_GE_LIST_DONE
    private void hleGeAfterDrawSyncAction() {
    	for (int i = 0; i < NUMBER_GE_LISTS; i++) {
    		if (allGeLists[i].status == PSP_GE_LIST_DONE) {
    			allGeLists[i].reset();
    		}
    	}
    }

    public synchronized void sceGeDrawSync(int mode) {
        CpuState cpu = Emulator.getProcessor().cpu;

    	if (VideoEngine.log.isDebugEnabled()) {
    		VideoEngine.log.debug("sceGeDrawSync mode=" + mode);
    	}

        if (mode == 0) {
            if (IntrManager.getInstance().isInsideInterrupt()) {
        		VideoEngine.log.warn("sceGeDrawSync mode=0 called inside an Interrupt!");
        		cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        	} else {
	    		cpu.gpr[2] = 0;
	
	    		PspGeList lastList = VideoEngine.getInstance().getLastDrawList();
	    		if (lastList != null) {
	    			blockCurrentThreadOnList(lastList, new HLEAfterDrawSyncAction());
	    		} else {
	    			if (VideoEngine.log.isDebugEnabled()) {
	    				VideoEngine.log.debug("sceGeDrawSync all lists completed, not waiting");
	    			}
	    			hleGeAfterDrawSyncAction();
	    			ThreadMan.getInstance().rescheduleCurrentThread();
	    		}
        	}
    	} else if (mode == 1) {
    		PspGeList currentList = VideoEngine.getInstance().getCurrentList();
    		if (currentList == null) {
    			cpu.gpr[2] = 0;
    		} else {
    			cpu.gpr[2] = currentList.status;
    		}
    	} else {
    		VideoEngine.log.warn("sceGeDrawSync invalid mode=" + mode);
        	cpu.gpr[2] = SceKernelErrors.ERROR_ARGUMENT;
    	}
    }

    public void sceGeSaveContext(int contextAddr) {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceGeSaveContext contextAddr=" + Integer.toHexString(contextAddr));
    	}

    	pspGeContext context = new pspGeContext();
    	VideoEngine.getInstance().hleSaveContext(context);
    	context.write(Memory.getInstance(), contextAddr);

    	Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceGeRestoreContext(int contextAddr) {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceGeRestoreContext contextAddr=" + Integer.toHexString(contextAddr));
    	}

    	pspGeContext context = new pspGeContext();
    	context.read(Memory.getInstance(), contextAddr);
    	VideoEngine.getInstance().hleRestoreContext(context);

    	Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceGeSetCallback(int cbdata_addr) {
        pspGeCallbackData cbdata = new pspGeCallbackData();
        cbdata.read(Emulator.getMemory(), cbdata_addr);
        int cbid = SceUidManager.getNewUid("pspge-callback");
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceGeSetCallback signalFunc=0x" + Integer.toHexString(cbdata.signalFunction)
                              + ", signalArg=0x" + Integer.toHexString(cbdata.signalArgument)
                              + ", finishFunc=0x" + Integer.toHexString(cbdata.finishFunction)
                              + ", finishArg=0x" + Integer.toHexString(cbdata.finishArgument)
                              + ", result cbid=" + Integer.toHexString(cbid));
        }

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
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceGeUnsetCallback cbid=" + Integer.toHexString(cbid));
    	}
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

    public synchronized void sceGeContinue() {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceGeContinue()");
    	}

    	PspGeList list = VideoEngine.getInstance().getCurrentList();
    	if (list != null) {
    		if (list.status == PSP_GE_LIST_END_REACHED) {
            	Memory mem = Memory.getInstance();
            	if (mem.read32(list.pc) == (GeCommands.FINISH << 24) &&
            		mem.read32(list.pc + 4) == (GeCommands.END << 24)) {
            		list.pc += 8;
            	}
            	list.restartList();
    		}
    	}

        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public synchronized void sceGeBreak() {
    	Modules.log.warn("Unsupported sceGeBreak");
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    private void triggerAsyncCallback(int cbid, int listId, int behavior, int signalId, HashMap<Integer, SceKernelCallbackInfo> callbacks) {
    	SceKernelCallbackInfo callback = callbacks.get(cbid);
    	if (callback != null && callback.callback_addr != 0) {
    		GeCallbackInterruptHandler geCallbackInterruptHandler = new GeCallbackInterruptHandler(callback.callback_addr, callback.callback_arg_addr);
    		GeInterruptHandler geInterruptHandler = new GeInterruptHandler(geCallbackInterruptHandler, listId, behavior, signalId);
    		Emulator.getScheduler().addAction(0, geInterruptHandler);
    	} else {
    		hleGeOnAfterCallback(listId, behavior);
    	}
    }

    /** safe to call from the Async display thread */
    public void triggerFinishCallback(int cbid, int callbackNotifyArg1) {
		triggerAsyncCallback(cbid, -1, PSP_GE_BEHAVIOR_SUSPEND, callbackNotifyArg1, finishCallbacks);
    }

    /** safe to call from the Async display thread */
    public void triggerSignalCallback(int cbid, int listId, int behavior, int callbackNotifyArg1) {
		triggerAsyncCallback(cbid, listId, behavior, callbackNotifyArg1, signalCallbacks);
    }

    static class DeferredCallbackInfo {
        public final int cbid;
        public final int callbackIndex;
        public final int listId;
        public final int behavior;
        public final int callbackNotifyArg1;

        public DeferredCallbackInfo(int cbid, int callbackIndex, int callbackNotifyArg1) {
            this.cbid = cbid;
            this.callbackIndex = callbackIndex;
            this.listId = -1;
            this.behavior = PSP_GE_BEHAVIOR_SUSPEND;
            this.callbackNotifyArg1 = callbackNotifyArg1;
        }

        public DeferredCallbackInfo(int cbid, int callbackIndex, int listId, int behavior, int callbackNotifyArg1) {
            this.cbid = cbid;
            this.callbackIndex = callbackIndex;
            this.listId = listId;
            this.behavior = behavior;
            this.callbackNotifyArg1 = callbackNotifyArg1;
        }
    }

    private class HLEAfterDrawSyncAction implements IAction {
		@Override
		public void execute() {
			hleGeAfterDrawSyncAction();
		}
    }
}
