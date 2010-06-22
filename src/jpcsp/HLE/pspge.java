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
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.kernel.types.pspGeCallbackData;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.HLE.kernel.types.pspGeContext;
import jpcsp.HLE.kernel.types.interrupts.GeCallbackInterruptHandler;
import jpcsp.HLE.kernel.types.interrupts.GeInterruptHandler;
import jpcsp.HLE.modules.ThreadManForUser;

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;

/*
 * TODO list:
 * 1. Write a sample to test sceGeGetMtx().
 */

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

    public final static int PSP_GE_MATRIX_BONE0  = 0;
    public final static int PSP_GE_MATRIX_BONE1  = 1;
    public final static int PSP_GE_MATRIX_BONE2  = 2;
    public final static int PSP_GE_MATRIX_BONE3  = 3;
    public final static int PSP_GE_MATRIX_BONE4  = 4;
    public final static int PSP_GE_MATRIX_BONE5  = 5;
    public final static int PSP_GE_MATRIX_BONE6  = 6;
    public final static int PSP_GE_MATRIX_BONE7  = 7;
    public final static int PSP_GE_MATRIX_WORLD  = 8;
    public final static int PSP_GE_MATRIX_VIEW   = 9;
    public final static int PSP_GE_MATRIX_PROJECTION = 10;
    public final static int PSP_GE_MATRIX_TEXGEN = 11;

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
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        for (Integer thid = deferredThreadWakeupQueue.poll(); thid != null; thid = deferredThreadWakeupQueue.poll()) {
        	if (VideoEngine.log.isDebugEnabled()) {
        		VideoEngine.log.debug("really waking thread " + Integer.toHexString(thid) + "(" + threadMan.getThreadName(thid) + ")");
        	}
            threadMan.hleUnblockThread(thid);
        }
    }

    public void sceGeEdramGetSize() {
        Emulator.getProcessor().cpu.gpr[2] = MemoryMap.SIZE_VRAM;
    }

    public void sceGeEdramGetAddr() {
        Emulator.getProcessor().cpu.gpr[2] = MemoryMap.START_VRAM;
    }

    public void sceGeEdramSetAddrTranslation(int size) {
        VideoEngine.log.warn("UNIMPLEMENTED: sceGeEdramSetAddrTranslation size=" + size);
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceGeListEnQueue(int list_addr, int stall_addr, int cbid, int arg_addr) {
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
    		cpu.gpr[2] = SceKernelErrors.ERROR_LIST_BUSY;
    		VideoEngine.log.warn("sceGeListEnQueue can't enqueue duplicate list address");
    	} else {
    		synchronized (this) {
    	    	PspGeList list = listFreeQueue.poll();
    	    	if (list == null) {
    	    		cpu.gpr[2] = SceKernelErrors.ERROR_LIST_OUT_OF_MEMORY;
    	    		VideoEngine.log.warn("sceGeListEnQueue no more free list available!");
    	    	} else {
    	    		list.init(list_addr, stall_addr, cbid, arg_addr);
    	    		startGeList(list);
    	            cpu.gpr[2] = list.id;
    	    	}
			}
    	}

		if (VideoEngine.log.isDebugEnabled()) {
			VideoEngine.log.debug(String.format("sceGeListEnQueue returning 0x%x", cpu.gpr[2]));
		}
    }

    public void sceGeListEnQueueHead(int list_addr, int stall_addr, int cbid, int arg_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        // Identical to sceGeListEnQueue, but places the list at the
        // head of the drawing queue.

        if (VideoEngine.log.isDebugEnabled()) {
	        VideoEngine.log.debug("sceGeListEnQueueHead(list=0x" + Integer.toHexString(list_addr)
	            + ",stall=0x" + Integer.toHexString(stall_addr)
	            + ",cbid=0x" + Integer.toHexString(cbid)
	            + ",arg=0x" + Integer.toHexString(arg_addr) + ")");
    	}

    	list_addr &= Memory.addressMask;
    	stall_addr &= Memory.addressMask;

    	if (VideoEngine.getInstance().hasDrawList(list_addr)) {
    		cpu.gpr[2] = SceKernelErrors.ERROR_LIST_BUSY;
    		VideoEngine.log.warn("sceGeListEnQueueHead can't enqueue duplicate list address");
    	} else {
    		synchronized (this) {
    	    	PspGeList list = listFreeQueue.poll();
    	    	if (list == null) {
    	    		cpu.gpr[2] = SceKernelErrors.ERROR_LIST_OUT_OF_MEMORY;
    	    		VideoEngine.log.warn("sceGeListEnQueueHead no more free list available!");
    	    	} else {
    	    		list.init(list_addr, stall_addr, cbid, arg_addr);
    	    		startGeListHead(list);
    	            cpu.gpr[2] = list.id;
    	    	}
			}
    	}

		if (VideoEngine.log.isDebugEnabled()) {
			VideoEngine.log.debug(String.format("sceGeListEnQueueHead returning 0x%x", cpu.gpr[2]));
		}
    }

    public void sceGeListDeQueue(int id) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (VideoEngine.log.isDebugEnabled()) {
        	VideoEngine.log.debug("sceGeListDeQueue(id=" + id + ")");
        }

        if (id < 0 || id >= NUMBER_GE_LISTS) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_LIST_ID;
        } else {
        	synchronized (this) {
            	PspGeList list = allGeLists[id];
            	list.reset();
            	if (!listFreeQueue.contains(list)) {
            		listFreeQueue.add(list);
            	}
			}
            cpu.gpr[2] = 0;
        }
    }

    public void sceGeListUpdateStallAddr(int id, int stall_addr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (VideoEngine.log.isDebugEnabled()) {
        	VideoEngine.log.debug(String.format("sceGeListUpdateStallAddr(id=0x%x, stall=0x%08X)", id, stall_addr));
        }

        stall_addr &= Memory.addressMask;

        if (id < 0 || id >= NUMBER_GE_LISTS) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_LIST_ID;
        } else {
        	synchronized (this) {
            	PspGeList list = allGeLists[id];
            	if (list.getStallAddr() != stall_addr) {
            		list.setStallAddr(stall_addr);
                    pspdisplay.getInstance().setGeDirty(true);
            	}
			}
            cpu.gpr[2] = 0;
        }
    }

    /** Called from VideoEngine */
    public void hleGeListSyncDone(PspGeList list) {
        if (VideoEngine.log.isDebugEnabled()) {
            String msg = "hleGeListSyncDone list " + list;

            if (list.isDone()) {
                msg += ", done";
            } else {
                msg += ", NOT done";
            }

            if (list.blockedThreadIds.size() > 0 && list.status != PSP_GE_LIST_END_REACHED) {
                msg += ", waking thread";
                for (int threadId : list.blockedThreadIds) {
                	msg += " " + Integer.toHexString(threadId);
                }
            }

            VideoEngine.log.debug(msg);
        }

        synchronized (this) {
            if (list.blockedThreadIds.size() > 0 && list.status != PSP_GE_LIST_END_REACHED) {
                // things might go wrong if the thread already exists in the queue
                deferredThreadWakeupQueue.addAll(list.blockedThreadIds);
            }

            if (list.isDone()) {
            	listFreeQueue.add(list);
            }
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
    	// Send the list to the VideoEngine before triggering the display (setting GE dirty)
    	list.startList();
    	pspdisplay.getInstance().setGeDirty(true);
    }

    private void startGeListHead(PspGeList list) {
    	// Send the list to the VideoEngine at the head of the queue.
    	list.startListHead();
    	pspdisplay.getInstance().setGeDirty(true);
    }

    public void sceGeListSync(int id, int mode) {
        CpuState cpu = Emulator.getProcessor().cpu;

        if (VideoEngine.log.isDebugEnabled()) {
        	VideoEngine.log.debug(String.format("sceGeListSync(id=0x%x,mode=%d)", id, mode));
        }

        if (mode != 0 && mode != 1) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_ARGUMENT;
        } else if (id < 0 || id >= NUMBER_GE_LISTS) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_LIST_ID;
        } else if (mode == 0 && IntrManager.getInstance().isInsideInterrupt()) {
    		VideoEngine.log.warn("sceGeListSync mode=0 called inside an Interrupt!");
    		cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
        	PspGeList list = null;
        	boolean blockCurrentThread = false;

        	synchronized (this) {
            	list = allGeLists[id];
            	if (VideoEngine.log.isDebugEnabled()) {
                	VideoEngine.log.debug("sceGeListSync on list: " + list);
            	}

            	if (list.isReset()) {
            		cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_LIST_ID;
            	} else if (mode == 0 && !list.isDone()) {
            		cpu.gpr[2] = 0;
            		blockCurrentThread = true;
            	} else {
            		cpu.gpr[2] = list.status;
            	}
			}

        	// Block the current thread outside of the synchronized block
        	if (blockCurrentThread) {
        		blockCurrentThreadOnList(list, null);
        	}
        }
    }

    private void blockCurrentThreadOnList(PspGeList list, IAction action) {
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;

    	boolean blockCurrentThread = false;
    	boolean executeAction = false;

    	synchronized (this) {
    		int currentThreadId = threadMan.getCurrentThreadID();
            if (list.isDone()) {
        		// There has been some race condition: the list has just completed
            	// do not block the thread
    	    	if (VideoEngine.log.isDebugEnabled()) {
    	    		VideoEngine.log.debug("blockCurrentThreadOnList not blocking thread " + Integer.toHexString(currentThreadId) + ", list completed " + list);
    	    	}
        		executeAction = true;
        	} else {
    	    	if (VideoEngine.log.isDebugEnabled()) {
    	    		VideoEngine.log.debug("blockCurrentThreadOnList blocking thread " + Integer.toHexString(currentThreadId) + " on list " + list);
    	    	}
        		list.blockedThreadIds.add(currentThreadId);
    	    	blockCurrentThread = true;
        	}
		}

    	// Execute the action outside of the synchronized block
    	if (executeAction && action != null) {
    		action.execute();
    	}

    	// Block the thread outside of the synchronized block
    	if (blockCurrentThread) {
    		threadMan.hleBlockCurrentThreadCB(action, new ListSyncWaitStateChecker(list));
    	}
    }

    // sceGeDrawSync is resetting all the lists having status PSP_GE_LIST_DONE
    private void hleGeAfterDrawSyncAction() {
    	synchronized (this) {
        	for (int i = 0; i < NUMBER_GE_LISTS; i++) {
        		if (allGeLists[i].status == PSP_GE_LIST_DONE) {
        			allGeLists[i].reset();
        		}
        	}
		}
    }

    public void sceGeDrawSync(int mode) {
        CpuState cpu = Emulator.getProcessor().cpu;

    	if (VideoEngine.log.isDebugEnabled()) {
    		VideoEngine.log.debug("sceGeDrawSync mode=" + mode);
    	}

        // no synchronization on "this" required because we are not accessing
    	// local data, only list information from the VideoEngine.
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
	    			Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
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

        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        SceKernelCallbackInfo callbackSignal = threadMan.hleKernelCreateCallback("GeCallbackSignal", cbdata.signalFunction, cbdata.signalArgument);
        SceKernelCallbackInfo callbackFinish = threadMan.hleKernelCreateCallback("GeCallbackFinish", cbdata.finishFunction, cbdata.finishArgument);
        signalCallbacks.put(cbid, callbackSignal);
        finishCallbacks.put(cbid, callbackFinish);

        Emulator.getProcessor().cpu.gpr[2] = cbid;
    }

    public void sceGeUnsetCallback(int cbid) {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceGeUnsetCallback cbid=" + Integer.toHexString(cbid));
    	}

    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        SceKernelCallbackInfo callbackSignal = signalCallbacks.remove(cbid);
        SceKernelCallbackInfo callbackFinish = finishCallbacks.remove(cbid);
        if (callbackSignal != null) {
            threadMan.hleKernelDeleteCallback(callbackSignal.uid);
        }
        if (callbackFinish != null) {
            threadMan.hleKernelDeleteCallback(callbackFinish.uid);
        }
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceGeContinue() {
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceGeContinue()");
    	}

    	PspGeList list = VideoEngine.getInstance().getCurrentList();
    	if (list != null) {
    		synchronized (this) {
        		if (list.status == PSP_GE_LIST_END_REACHED) {
                	Memory mem = Memory.getInstance();
                	if (mem.read32(list.pc) == (GeCommands.FINISH << 24) &&
                		mem.read32(list.pc + 4) == (GeCommands.END << 24)) {
                		list.pc += 8;
                	}
                	list.restartList();
        		}
			}
    	}

        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceGeBreak() {
    	Modules.log.warn("Unsupported sceGeBreak");
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceGeGetCmd(int cmd) {
        VideoEngine ve = VideoEngine.getInstance();
        int arg = ve.getCommandValue(cmd);
        String cmdString = ve.commandToString(cmd);


        Modules.log.info("sceGeGetCmd " + cmdString.toUpperCase() + ":" + " cmd=0x" + Integer.toHexString(cmd) + " value=0x" + Integer.toHexString(arg));
        Emulator.getProcessor().cpu.gpr[2] = arg;
    }

    public void sceGeGetMtx(int mtxtype, int mtx_addr) {
        VideoEngine ve = VideoEngine.getInstance();
        float[] mtx = ve.getMatrix(mtxtype);

        Modules.log.info("UNIMPLEMENTED: sceGeGetMtx mtxtype=" + mtxtype + " mtx_addr=0x" + Integer.toHexString(mtx_addr));
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

    private class ListSyncWaitStateChecker implements IWaitStateChecker {
    	private PspGeList list;

    	public ListSyncWaitStateChecker(PspGeList list) {
    		this.list = list;
    	}

		@Override
		public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
    		// Continue the wait state until the list is done
    		return !list.isDone();
		}
    }
}