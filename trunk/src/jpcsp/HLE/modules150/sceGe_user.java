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
package jpcsp.HLE.modules150;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.kernel.types.pspGeCallbackData;
import jpcsp.HLE.kernel.types.interrupts.GeCallbackInterruptHandler;
import jpcsp.HLE.kernel.types.interrupts.GeInterruptHandler;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;

import org.apache.log4j.Logger;

@HLELogging
public class sceGe_user extends HLEModule {
    public static Logger log = Modules.getLogger("sceGe_user");

    public volatile boolean waitingForSync;
    public volatile boolean syncDone;
    private HashMap<Integer, SceKernelCallbackInfo> signalCallbacks;
    private HashMap<Integer, SceKernelCallbackInfo> finishCallbacks;
    private static final String geCallbackPurpose = "sceGeCallback";

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

    public final static int PSP_GE_SIGNAL_HANDLER_SUSPEND  = 0x01;
    public final static int PSP_GE_SIGNAL_HANDLER_CONTINUE = 0x02;
    public final static int PSP_GE_SIGNAL_HANDLER_PAUSE    = 0x03;
    public final static int PSP_GE_SIGNAL_SYNC             = 0x08;
    public final static int PSP_GE_SIGNAL_JUMP             = 0x10;
    public final static int PSP_GE_SIGNAL_CALL             = 0x11;
    public final static int PSP_GE_SIGNAL_RETURN           = 0x12;
    public final static int PSP_GE_SIGNAL_TBP0_REL         = 0x20;
    public final static int PSP_GE_SIGNAL_TBP1_REL         = 0x21;
    public final static int PSP_GE_SIGNAL_TBP2_REL         = 0x22;
    public final static int PSP_GE_SIGNAL_TBP3_REL         = 0x23;
    public final static int PSP_GE_SIGNAL_TBP4_REL         = 0x24;
    public final static int PSP_GE_SIGNAL_TBP5_REL         = 0x25;
    public final static int PSP_GE_SIGNAL_TBP6_REL         = 0x26;
    public final static int PSP_GE_SIGNAL_TBP7_REL         = 0x27;
    public final static int PSP_GE_SIGNAL_TBP0_REL_OFFSET  = 0x28;
    public final static int PSP_GE_SIGNAL_TBP1_REL_OFFSET  = 0x29;
    public final static int PSP_GE_SIGNAL_TBP2_REL_OFFSET  = 0x2A;
    public final static int PSP_GE_SIGNAL_TBP3_REL_OFFSET  = 0x2B;
    public final static int PSP_GE_SIGNAL_TBP4_REL_OFFSET  = 0x2C;
    public final static int PSP_GE_SIGNAL_TBP5_REL_OFFSET  = 0x2D;
    public final static int PSP_GE_SIGNAL_TBP6_REL_OFFSET  = 0x2E;
    public final static int PSP_GE_SIGNAL_TBP7_REL_OFFSET  = 0x2F;
    public final static int PSP_GE_SIGNAL_BREAK            = 0xFF;

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

    public int eDRAMMemoryWidth;

    @Override
    public String getName() {
        return "sceGe_user";
    }

    @Override
    public void start() {
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

        eDRAMMemoryWidth = 1024;

        super.start();
    }

    public void step() {
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;

        for (Integer thid = deferredThreadWakeupQueue.poll(); thid != null; thid = deferredThreadWakeupQueue.poll()) {
        	if (log.isDebugEnabled()) {
        		log.debug("really waking thread " + Integer.toHexString(thid) + "(" + threadMan.getThreadName(thid) + ")");
        	}
            threadMan.hleUnblockThread(thid);
        }
    }

    private void triggerAsyncCallback(int cbid, int listId, int listAddr, int behavior, int signalId, HashMap<Integer, SceKernelCallbackInfo> callbacks) {
    	SceKernelCallbackInfo callback = callbacks.get(cbid);
    	if (callback != null && callback.callback_addr != 0) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Scheduling Async Callback %s, listId=0x%X, behavior=%d, signalId=0x%X", callback.toString(), listId, behavior, signalId));
    		}
    		GeCallbackInterruptHandler geCallbackInterruptHandler = new GeCallbackInterruptHandler(callback.callback_addr, callback.callback_arg_addr, listAddr);
    		GeInterruptHandler geInterruptHandler = new GeInterruptHandler(geCallbackInterruptHandler, listId, behavior, signalId);
    		Emulator.getScheduler().addAction(geInterruptHandler);
    	} else {
    		hleGeOnAfterCallback(listId, behavior, false);
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
    	    	if (log.isDebugEnabled()) {
    	    		log.debug("blockCurrentThreadOnList not blocking thread " + Integer.toHexString(currentThreadId) + ", list completed " + list);
    	    	}
        		executeAction = true;
        	} else {
    	    	if (log.isDebugEnabled()) {
    	    		log.debug("blockCurrentThreadOnList blocking thread " + Integer.toHexString(currentThreadId) + " on list " + list);
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
    		// Block the thread, but do not execute callbacks.
    		threadMan.hleBlockCurrentThread(action, new ListSyncWaitStateChecker(list));
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

    /** Called from VideoEngine */
    public void hleGeListSyncDone(PspGeList list) {
        if (log.isDebugEnabled()) {
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

            log.debug(msg);
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

    public void hleGeOnAfterCallback(int listId, int behavior, boolean hasCallback) {
		// (gid15) I could not make any difference between
		//    PSP_GE_BEHAVIOR_CONTINUE and PSP_GE_BEHAVIOR_SUSPEND
		// Both wait for the completion of the callback before continuing
		// the list processing...
		if (behavior == PSP_GE_SIGNAL_HANDLER_CONTINUE
                || behavior == PSP_GE_SIGNAL_HANDLER_SUSPEND
                || !hasCallback) {
			if (listId >= 0 && listId < NUMBER_GE_LISTS) {
				PspGeList list = allGeLists[listId];
				if (log.isDebugEnabled()) {
					log.debug("hleGeOnAfterCallback restarting list " + list);
				}

				list.restartList();
			}
		}
    }

    private void startGeList(PspGeList list) {
    	// Send the list to the VideoEngine before triggering the display (setting GE dirty)
    	list.startList();
    	Modules.sceDisplayModule.setGeDirty(true);
    }

    private void startGeListHead(PspGeList list) {
    	// Send the list to the VideoEngine at the head of the queue.
    	list.startListHead();
    	Modules.sceDisplayModule.setGeDirty(true);
    }

    /** safe to call from the Async display thread */
    public void triggerFinishCallback(int cbid, int listId, int listAddr, int callbackNotifyArg1) {
		triggerAsyncCallback(cbid, listId, listAddr, PSP_GE_SIGNAL_HANDLER_SUSPEND, callbackNotifyArg1, finishCallbacks);
    }

    /** safe to call from the Async display thread */
    public void triggerSignalCallback(int cbid, int listId, int listAddr, int behavior, int callbackNotifyArg1) {
		triggerAsyncCallback(cbid, listId, listAddr, behavior, callbackNotifyArg1, signalCallbacks);
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
            this.behavior = PSP_GE_SIGNAL_HANDLER_SUSPEND;
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

    private static class ListSyncWaitStateChecker implements IWaitStateChecker {
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

    public int checkListId(int id) {
    	if (id < 0 || id >= NUMBER_GE_LISTS) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_INVALID_ID);
    	}

    	return id;
    }

    public int checkMode(int mode) {
    	if (mode < 0 || mode > 1) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_INVALID_MODE);
    	}

    	return mode;
    }

    public int hleGeListEnQueue(TPointer listAddr, @CanBeNull TPointer stallAddr, int cbid, @CanBeNull TPointer argAddr, int saveContextAddr) {
        int result;
    	synchronized (this) {
	    	PspGeList list = listFreeQueue.poll();
	    	if (list == null) {
	    		log.warn("sceGeListEnQueue no more free list available!");
	    		throw new SceKernelErrorException(SceKernelErrors.ERROR_OUT_OF_MEMORY);
	    	}

	    	list.init(listAddr.getAddress(), stallAddr.getAddress(), cbid, argAddr.getAddress());
	    	list.setSaveContextAddr(saveContextAddr);
    		startGeList(list);
            result = list.id;
		}

    	if (log.isDebugEnabled()) {
			log.debug(String.format("sceGeListEnQueue returning 0x%X", result));
		}

		return result;
    }

    public int hleGeListSync(int id) {
    	if (id < 0 || id >= NUMBER_GE_LISTS) {
    		return -1;
    	}

    	PspGeList list = null;
    	int result;
    	synchronized (this) {
        	list = allGeLists[id];
    		result = list.status;
		}

    	return result;
    }

    @HLEFunction(nid = 0x1F6752AD, version = 150)
    public int sceGeEdramGetSize() {
        return MemoryMap.SIZE_VRAM;
    }

    @HLEFunction(nid = 0xE47E40E4, version = 150)
    public int sceGeEdramGetAddr() {
        return MemoryMap.START_VRAM;
    }

    @HLEFunction(nid = 0xB77905EA, version = 150)
    public int sceGeEdramSetAddrTranslation(int size) {
        // Faking. There's no need for real memory width conversion.
        int previousWidth = eDRAMMemoryWidth;
        eDRAMMemoryWidth = size;

        return previousWidth;
    }

    @HLEFunction(nid = 0xDC93CFEF, version = 150)
    public int sceGeGetCmd(int cmd) {
        VideoEngine ve = VideoEngine.getInstance();
        int value = ve.getCommandValue(cmd);

        if (log.isInfoEnabled()) {
        	log.info(String.format("sceGeGetCmd %s: cmd=0x%X, value=0x%06X", ve.commandToString(cmd).toUpperCase(), cmd, value));
        }

        return value;
    }

    @HLEFunction(nid = 0x57C8945B, version = 150)
    public int sceGeGetMtx(int mtxType, TPointer mtxAddr) {
        VideoEngine ve = VideoEngine.getInstance();
        float[] mtx = ve.getMatrix(mtxType);

        if (mtx == null) {
        	log.warn(String.format("sceGeGetMtx invalid type mtxType=%d", mtxType));
        	return -1;
        }

        for (int i = 0; i < mtx.length; i++) {
        	mtxAddr.setFloat(i << 2, mtx[i]);
        }

        if (log.isInfoEnabled()) {
        	log.info(String.format("sceGeGetMtx mtxType=%d, mtxAddr=%s, mtx=%s", mtxType, mtxAddr, mtx));
        }

        return 0;
    }

    @HLEFunction(nid = 0x438A385A, version = 150)
    public int sceGeSaveContext(TPointer contextAddr) {
    	VideoEngine.getInstance().hleSaveContext(contextAddr.getAddress());

    	return 0;
    }

    @HLEFunction(nid = 0x0BF608FB, version = 150)
    public int sceGeRestoreContext(TPointer contextAddr) {
    	VideoEngine.getInstance().hleRestoreContext(contextAddr.getAddress());

    	return 0;
    }

    @HLEFunction(nid = 0xAB49E76A, version = 150)
    public int sceGeListEnQueue(TPointer listAddr, @CanBeNull TPointer stallAddr, int cbid, @CanBeNull TPointer argAddr) {
    	if (VideoEngine.getInstance().hasDrawList(listAddr.getAddress())) {
    		log.warn("sceGeListEnQueue can't enqueue duplicate list address");
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_BUSY);
    	}

    	return hleGeListEnQueue(listAddr, stallAddr, cbid, argAddr, 0);
    }

    @HLEFunction(nid = 0x1C0D95A6, version = 150)
    public int sceGeListEnQueueHead(TPointer listAddr, @CanBeNull TPointer stallAddr, int cbid, @CanBeNull TPointer argAddr) {
    	if (VideoEngine.getInstance().hasDrawList(listAddr.getAddress())) {
    		log.warn("sceGeListEnQueueHead can't enqueue duplicate list address");
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_BUSY);
    	}

    	int result;
    	synchronized (this) {
	    	PspGeList list = listFreeQueue.poll();
	    	if (list == null) {
	    		log.warn("sceGeListEnQueueHead no more free list available!");
	    		throw new SceKernelErrorException(SceKernelErrors.ERROR_OUT_OF_MEMORY);
	    	}

	    	list.init(listAddr.getAddress(), stallAddr.getAddress(), cbid, argAddr.getAddress());
    		startGeListHead(list);
            result = list.id;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceGeListEnQueueHead returning 0x%X", result));
		}

		return result;
    }

    @HLEFunction(nid = 0x5FB86AB0, version = 150)
    public int sceGeListDeQueue(@CheckArgument("checkListId") int id) {
    	synchronized (this) {
        	PspGeList list = allGeLists[id];
        	list.reset();
        	if (!listFreeQueue.contains(list)) {
        		listFreeQueue.add(list);
        	}
		}

    	return 0;
    }

    @HLEFunction(nid = 0xE0D68148, version = 150)
    public int sceGeListUpdateStallAddr(@CheckArgument("checkListId") int id, @CanBeNull TPointer stallAddr) {
    	synchronized (this) {
        	PspGeList list = allGeLists[id];
        	if (list.getStallAddr() != stallAddr.getAddress()) {
        		list.setStallAddr(stallAddr.getAddress());
                Modules.sceDisplayModule.setGeDirty(true);
        	}
		}

    	return 0;
    }

    @HLEFunction(nid = 0x03444EB4, version = 150)
    public int sceGeListSync(@CheckArgument("checkListId") int id, @CheckArgument("checkMode") int mode) {
        if (mode == 0 && IntrManager.getInstance().isInsideInterrupt()) {
    		log.debug("sceGeListSync (mode==0) cannot be called inside an interrupt handler!");
    		return SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
        }

        PspGeList list = null;
    	boolean blockCurrentThread = false;
    	int result;
    	synchronized (this) {
        	list = allGeLists[id];
        	if (log.isDebugEnabled()) {
            	log.debug(String.format("sceGeListSync on list: %s", list));
        	}

        	if (list.isReset()) {
        		throw new SceKernelErrorException(SceKernelErrors.ERROR_INVALID_ID);
        	}

        	if (mode == 0 && !list.isDone()) {
        		result = 0;
        		blockCurrentThread = true;
        	} else {
        		result = list.status;
        	}
		}

    	// Block the current thread outside of the synchronized block
    	if (blockCurrentThread) {
    		blockCurrentThreadOnList(list, null);
    	}

    	return result;
    }

    @HLEFunction(nid = 0xB287BD61, version = 150)
    public int sceGeDrawSync(@CheckArgument("checkMode") int mode) {
        if (mode == 0 && IntrManager.getInstance().isInsideInterrupt()) {
            log.debug("sceGeDrawSync (mode==0) cannot be called inside an interrupt handler!");
            return SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
        }

        // no synchronization on "this" required because we are not accessing
    	// local data, only list information from the VideoEngine.
        int result = 0;
        if (mode == 0) {
            PspGeList lastList = VideoEngine.getInstance().getLastDrawList();
            if (lastList != null) {
                blockCurrentThreadOnList(lastList, new HLEAfterDrawSyncAction());
            } else {
    			if (log.isDebugEnabled()) {
                    log.debug("sceGeDrawSync all lists completed, not waiting");
                }
                hleGeAfterDrawSyncAction();
                Modules.ThreadManForUserModule.hleRescheduleCurrentThread();
    		}
        } else if (mode == 1) {
            PspGeList currentList = VideoEngine.getInstance().getFirstDrawList();
            if (currentList != null) {
                result = currentList.status;
            }
            if (log.isDebugEnabled()) {
            	log.debug(String.format("sceGeDrawSync mode=%d, returning %d", mode, result));
            }
        }

        return result;
    }

    @HLEFunction(nid = 0xB448EC0D, version = 150)
    public int sceGeBreak(@CheckArgument("checkMode") int mode, TPointer brk_addr) {
        int result = 0;
        PspGeList list = VideoEngine.getInstance().getCurrentList();
        if (mode == 0) {  // Pause the current list only.
            if (list != null) {
                list.pauseList();
                result = list.id;
            }
        } else if (mode == 1) {  // Pause the current list and cancel the rest of the queue.
            if (list != null) {
                list.pauseList();
                for (int i = 0; i < NUMBER_GE_LISTS; i++) {
                    allGeLists[i].status = PSP_GE_LIST_CANCEL_DONE;
                }
                result = list.id;
            }
        }

        return result;
    }

    @HLEFunction(nid = 0x4C06E472, version = 150)
    public int sceGeContinue() {
    	PspGeList list = VideoEngine.getInstance().getCurrentList();
    	if (list != null) {
    		synchronized (this) {
        		if (list.status == PSP_GE_LIST_END_REACHED) {
        	    	Memory mem = Memory.getInstance();
                	if (mem.read32(list.getPc()) == (GeCommands.FINISH << 24) &&
                		mem.read32(list.getPc() + 4) == (GeCommands.END << 24)) {
                		list.readNextInstruction();
                		list.readNextInstruction();
                	}
        		}
            	list.restartList();
			}
    	}

        return 0;
    }

    @HLEFunction(nid = 0xA4FC06A4, version = 150, checkInsideInterrupt = true)
    public int sceGeSetCallback(TPointer cbdata_addr) {
        pspGeCallbackData cbdata = new pspGeCallbackData();
        cbdata.read(Emulator.getMemory(), cbdata_addr.getAddress());

        // The cbid returned has a value in the range [0..15].
        int cbid = SceUidManager.getNewId(geCallbackPurpose, 0, 15);
        if (cbid == SceUidManager.INVALID_ID) {
        	log.warn(String.format("sceGeSetCallback no more callback ID available"));
        	return SceKernelErrors.ERROR_OUT_OF_MEMORY;
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceGeSetCallback signalFunc=0x%08X, signalArg=0x%08X, finishFunc=0x%08X, finishArg=0x%08X, result cbid=0x%X", cbdata.signalFunction, cbdata.signalArgument, cbdata.finishFunction, cbdata.finishArgument, cbid));
        }

        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        SceKernelCallbackInfo callbackSignal = threadMan.hleKernelCreateCallback("GeCallbackSignal", cbdata.signalFunction, cbdata.signalArgument);
        SceKernelCallbackInfo callbackFinish = threadMan.hleKernelCreateCallback("GeCallbackFinish", cbdata.finishFunction, cbdata.finishArgument);
        signalCallbacks.put(cbid, callbackSignal);
        finishCallbacks.put(cbid, callbackFinish);

        return cbid;
    }

    @HLEFunction(nid = 0x05DB22CE, version = 150, checkInsideInterrupt = true)
    public int sceGeUnsetCallback(int cbid) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        SceKernelCallbackInfo callbackSignal = signalCallbacks.remove(cbid);
        SceKernelCallbackInfo callbackFinish = finishCallbacks.remove(cbid);
        if (callbackSignal != null) {
            threadMan.hleKernelDeleteCallback(callbackSignal.uid);
        }
        if (callbackFinish != null) {
            threadMan.hleKernelDeleteCallback(callbackFinish.uid);
        }
        SceUidManager.releaseId(cbid, geCallbackPurpose);

        return 0;
    }
}