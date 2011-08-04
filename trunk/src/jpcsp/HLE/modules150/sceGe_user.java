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

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
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
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;

import org.apache.log4j.Logger;

public class sceGe_user implements HLEModule, HLEStartModule {
    private static Logger log = Modules.getLogger("sceGe_user");

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
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x1F6752AD, sceGeEdramGetSizeFunction);
            mm.addFunction(0xE47E40E4, sceGeEdramGetAddrFunction);
            mm.addFunction(0xB77905EA, sceGeEdramSetAddrTranslationFunction);
            mm.addFunction(0xDC93CFEF, sceGeGetCmdFunction);
            mm.addFunction(0x57C8945B, sceGeGetMtxFunction);
            mm.addFunction(0x438A385A, sceGeSaveContextFunction);
            mm.addFunction(0x0BF608FB, sceGeRestoreContextFunction);
            mm.addFunction(0xAB49E76A, sceGeListEnQueueFunction);
            mm.addFunction(0x1C0D95A6, sceGeListEnQueueHeadFunction);
            mm.addFunction(0x5FB86AB0, sceGeListDeQueueFunction);
            mm.addFunction(0xE0D68148, sceGeListUpdateStallAddrFunction);
            mm.addFunction(0x03444EB4, sceGeListSyncFunction);
            mm.addFunction(0xB287BD61, sceGeDrawSyncFunction);
            mm.addFunction(0xB448EC0D, sceGeBreakFunction);
            mm.addFunction(0x4C06E472, sceGeContinueFunction);
            mm.addFunction(0xA4FC06A4, sceGeSetCallbackFunction);
            mm.addFunction(0x05DB22CE, sceGeUnsetCallbackFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceGeEdramGetSizeFunction);
            mm.removeFunction(sceGeEdramGetAddrFunction);
            mm.removeFunction(sceGeEdramSetAddrTranslationFunction);
            mm.removeFunction(sceGeGetCmdFunction);
            mm.removeFunction(sceGeGetMtxFunction);
            mm.removeFunction(sceGeSaveContextFunction);
            mm.removeFunction(sceGeRestoreContextFunction);
            mm.removeFunction(sceGeListEnQueueFunction);
            mm.removeFunction(sceGeListEnQueueHeadFunction);
            mm.removeFunction(sceGeListDeQueueFunction);
            mm.removeFunction(sceGeListUpdateStallAddrFunction);
            mm.removeFunction(sceGeListSyncFunction);
            mm.removeFunction(sceGeDrawSyncFunction);
            mm.removeFunction(sceGeBreakFunction);
            mm.removeFunction(sceGeContinueFunction);
            mm.removeFunction(sceGeSetCallbackFunction);
            mm.removeFunction(sceGeUnsetCallbackFunction);

        }
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
    }

    @Override
    public void stop() {
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

    private void triggerAsyncCallback(int cbid, int listId, int behavior, int signalId, HashMap<Integer, SceKernelCallbackInfo> callbacks) {
    	SceKernelCallbackInfo callback = callbacks.get(cbid);
    	if (callback != null && callback.callback_addr != 0) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Scheduling Async Callback %s, listId=0x%X, behavior=%d, signalId=0x%X", callback.toString(), listId, behavior, signalId));
    		}
    		GeCallbackInterruptHandler geCallbackInterruptHandler = new GeCallbackInterruptHandler(callback.callback_addr, callback.callback_arg_addr);
    		GeInterruptHandler geInterruptHandler = new GeInterruptHandler(geCallbackInterruptHandler, listId, behavior, signalId);
    		Emulator.getScheduler().addAction(geInterruptHandler);
    	} else {
    		hleGeOnAfterCallback(listId, behavior);
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

    public void hleGeOnAfterCallback(int listId, int behavior) {
		// (gid15) I could not make any difference between
		//    PSP_GE_BEHAVIOR_CONTINUE and PSP_GE_BEHAVIOR_SUSPEND
		// Both wait for the completion of the callback before continuing
		// the list processing...
		if (behavior == PSP_GE_SIGNAL_HANDLER_CONTINUE
                || behavior == PSP_GE_SIGNAL_HANDLER_SUSPEND) {
			if (listId >= 0 && listId < NUMBER_GE_LISTS) {
				PspGeList list = allGeLists[listId];
				if (log.isDebugEnabled()) {
					log.debug("hleGeOnAfterCallback restarting list " + list);
				}

				if (!list.isFinished()) {
					// If the list is still on the END command, skip it.
					Memory mem = Memory.getInstance();
					if (Memory.isAddressGood(list.pc)) {
						if (VideoEngine.command(mem.read32(list.pc)) == GeCommands.END) {
							list.pc += 4;
						}
					}
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
    public void triggerFinishCallback(int cbid, int listId, int callbackNotifyArg1) {
		triggerAsyncCallback(cbid, listId, PSP_GE_SIGNAL_HANDLER_SUSPEND, callbackNotifyArg1, finishCallbacks);
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

    public void sceGeEdramGetSize(Processor processor) {
        CpuState cpu = processor.cpu;
        cpu.gpr[2] = MemoryMap.SIZE_VRAM;
    }

    public void sceGeEdramGetAddr(Processor processor) {
    	CpuState cpu = processor.cpu;
        cpu.gpr[2] = MemoryMap.START_VRAM;
    }

    public void sceGeEdramSetAddrTranslation(Processor processor) {
        CpuState cpu = processor.cpu;

        int size = cpu.gpr[4];

        // Faking. There's no need for real memory width conversion.
        int previousWidth = eDRAMMemoryWidth;
        eDRAMMemoryWidth = size;

        cpu.gpr[2] = previousWidth;
    }

    public void sceGeGetCmd(Processor processor) {
        CpuState cpu = processor.cpu;

        int cmd = cpu.gpr[4];

        VideoEngine ve = VideoEngine.getInstance();
        int arg = ve.getCommandValue(cmd);

        log.info("sceGeGetCmd " + ve.commandToString(cmd).toUpperCase() + ":" + " cmd=0x" + Integer.toHexString(cmd) + " value=0x" + Integer.toHexString(arg));

        cpu.gpr[2] = arg;
    }

    public void sceGeGetMtx(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int mtxtype = cpu.gpr[4];
        int mtx_addr = cpu.gpr[5];

        VideoEngine ve = VideoEngine.getInstance();
        float[] mtx = ve.getMatrix(mtxtype);

        if(Memory.isAddressGood(mtx_addr)) {
            for(int i = 0; i < mtx.length; i++) {
                mem.write32(mtx_addr, (int)mtx[i]);
            }
        }

        log.info("sceGeGetMtx mtxtype=" + mtxtype + " mtx_addr=0x" + Integer.toHexString(mtx_addr) + ", mtx=" + mtx);

        cpu.gpr[2] = 0;
    }

    public void sceGeSaveContext(Processor processor) {
        CpuState cpu = processor.cpu;

        int contextAddr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
    		log.debug("sceGeSaveContext contextAddr=" + Integer.toHexString(contextAddr));
    	}

    	VideoEngine.getInstance().hleSaveContext(contextAddr);

    	cpu.gpr[2] = 0;
    }

    public void sceGeRestoreContext(Processor processor) {
        CpuState cpu = processor.cpu;

        int contextAddr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
    		log.debug("sceGeRestoreContext contextAddr=" + Integer.toHexString(contextAddr));
    	}

    	VideoEngine.getInstance().hleRestoreContext(contextAddr);

    	cpu.gpr[2] = 0;
    }

    public void sceGeListEnQueue(Processor processor) {
        CpuState cpu = processor.cpu;

        int list_addr = cpu.gpr[4];
        int stall_addr = cpu.gpr[5];
        int cbid = cpu.gpr[6];
        int arg_addr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
	        log.debug("sceGeListEnQueue(list=0x" + Integer.toHexString(list_addr)
	            + ",stall=0x" + Integer.toHexString(stall_addr)
	            + ",cbid=0x" + Integer.toHexString(cbid)
	            + ",arg=0x" + Integer.toHexString(arg_addr) + ")");
    	}

    	list_addr &= Memory.addressMask;
    	stall_addr &= Memory.addressMask;

    	if (VideoEngine.getInstance().hasDrawList(list_addr)) {
    		cpu.gpr[2] = SceKernelErrors.ERROR_BUSY;
    		log.warn("sceGeListEnQueue can't enqueue duplicate list address");
    	} else {
    		synchronized (this) {
    	    	PspGeList list = listFreeQueue.poll();
    	    	if (list == null) {
    	    		cpu.gpr[2] = SceKernelErrors.ERROR_OUT_OF_MEMORY;
    	    		log.warn("sceGeListEnQueue no more free list available!");
    	    	} else {
    	    		list.init(list_addr, stall_addr, cbid, arg_addr);
    	    		startGeList(list);
    	            cpu.gpr[2] = list.id;
    	    	}
			}
    	}

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceGeListEnQueue returning 0x%x", cpu.gpr[2]));
		}
    }

    public void sceGeListEnQueueHead(Processor processor) {
    	CpuState cpu = processor.cpu;

        int list_addr = cpu.gpr[4];
        int stall_addr = cpu.gpr[5];
        int cbid = cpu.gpr[6];
        int arg_addr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
	        log.debug("sceGeListEnQueueHead(list=0x" + Integer.toHexString(list_addr)
	            + ",stall=0x" + Integer.toHexString(stall_addr)
	            + ",cbid=0x" + Integer.toHexString(cbid)
	            + ",arg=0x" + Integer.toHexString(arg_addr) + ")");
    	}

    	list_addr &= Memory.addressMask;
    	stall_addr &= Memory.addressMask;

    	if (VideoEngine.getInstance().hasDrawList(list_addr)) {
    		cpu.gpr[2] = SceKernelErrors.ERROR_BUSY;
    		log.warn("sceGeListEnQueueHead can't enqueue duplicate list address");
    	} else {
    		synchronized (this) {
    	    	PspGeList list = listFreeQueue.poll();
    	    	if (list == null) {
    	    		cpu.gpr[2] = SceKernelErrors.ERROR_OUT_OF_MEMORY;
    	    		log.warn("sceGeListEnQueueHead no more free list available!");
    	    	} else {
    	    		list.init(list_addr, stall_addr, cbid, arg_addr);
    	    		startGeListHead(list);
    	            cpu.gpr[2] = list.id;
    	    	}
			}
    	}

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceGeListEnQueueHead returning 0x%x", cpu.gpr[2]));
		}
    }

    public void sceGeListDeQueue(Processor processor) {
        CpuState cpu = processor.cpu;

        int id = cpu.gpr[4];

        if (log.isDebugEnabled()) {
        	log.debug("sceGeListDeQueue(id=" + id + ")");
        }

        if (id < 0 || id >= NUMBER_GE_LISTS) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_ID;
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

    public void sceGeListUpdateStallAddr(Processor processor) {
        CpuState cpu = processor.cpu;

        int id = cpu.gpr[4];
        int stall_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceGeListUpdateStallAddr(id=0x%x, stall=0x%08X)", id, stall_addr));
        }

        stall_addr &= Memory.addressMask;

        if (id < 0 || id >= NUMBER_GE_LISTS) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_ID;
        } else {
        	synchronized (this) {
            	PspGeList list = allGeLists[id];
            	if (list.getStallAddr() != stall_addr) {
            		list.setStallAddr(stall_addr);
                    Modules.sceDisplayModule.setGeDirty(true);
            	}
			}
            cpu.gpr[2] = 0;
        }
    }

    public void sceGeListSync(Processor processor) {
        CpuState cpu = processor.cpu;

        int id = cpu.gpr[4];
        int mode = cpu.gpr[5];

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceGeListSync(id=0x%x,mode=%d)", id, mode));
        }

        if (mode != 0 && mode != 1) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_MODE;
        } else if (id < 0 || id >= NUMBER_GE_LISTS) {
        	cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_ID;
        } else if (IntrManager.getInstance().isInsideInterrupt() && mode == 0) {
    		log.debug("sceGeListSync (mode==0) cannot be called inside an interrupt handler!");
    		cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
        	PspGeList list = null;
        	boolean blockCurrentThread = false;

        	synchronized (this) {
            	list = allGeLists[id];
            	if (log.isDebugEnabled()) {
                	log.debug("sceGeListSync on list: " + list);
            	}

            	if (list.isReset()) {
            		cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_ID;
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

    public void sceGeDrawSync(Processor processor) {
        CpuState cpu = processor.cpu;

        int mode = cpu.gpr[4];

        if (log.isDebugEnabled()) {
    		log.debug("sceGeDrawSync mode=" + mode);
    	}

        // no synchronization on "this" required because we are not accessing
    	// local data, only list information from the VideoEngine.
        if (IntrManager.getInstance().isInsideInterrupt() && mode == 0) {
            log.debug("sceGeListSync (mode==0) cannot be called inside an interrupt handler!");
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            if (mode == 0) {
                cpu.gpr[2] = 0;
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
                if (currentList == null) {
                    cpu.gpr[2] = 0;
                } else {
                    cpu.gpr[2] = currentList.status;
                }
                if (log.isDebugEnabled()) {
                	log.debug("sceGeDrawSync mode=" + mode + ", returning " + cpu.gpr[2]);
                }
            } else {
                log.warn("sceGeDrawSync invalid mode=" + mode);
                cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_MODE;
            }
        }
    }

    public void sceGeBreak(Processor processor) {
        CpuState cpu = processor.cpu;

        int mode = cpu.gpr[4];
        int brk_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
    		log.debug("sceGeBreak mode=" + mode + ", brk_addr=0x" + Integer.toHexString(brk_addr));
    	}

        PspGeList list = VideoEngine.getInstance().getCurrentList();
        if(mode == 0) {  // Pause the current list only.
            if(list != null) {
                list.pauseList();
                cpu.gpr[2] = list.id;
            } else {
                cpu.gpr[2] = 0;
            }
        } else if (mode == 1) {  // Pause the current list and cancel the rest of the queue.
            if(list != null) {
                list.pauseList();
                for (int i = 0; i < NUMBER_GE_LISTS; i++) {
                    allGeLists[i].status = PSP_GE_LIST_CANCEL_DONE;
                }
                cpu.gpr[2] = list.id;
            } else {
                cpu.gpr[2] = 0;
            }
        } else {
            log.warn("sceGeBreak invalid mode=" + mode);
            cpu.gpr[2] = SceKernelErrors.ERROR_INVALID_MODE;
        }
    }

    public void sceGeContinue(Processor processor) {
        CpuState cpu = processor.cpu;
    	Memory mem = Processor.memory;

        if (log.isDebugEnabled()) {
    		log.debug("sceGeContinue()");
    	}

    	PspGeList list = VideoEngine.getInstance().getCurrentList();
    	if (list != null) {
    		synchronized (this) {
        		if (list.status == PSP_GE_LIST_END_REACHED) {
                	if (mem.read32(list.pc) == (GeCommands.FINISH << 24) &&
                		mem.read32(list.pc + 4) == (GeCommands.END << 24)) {
                		list.pc += 8;
                	}
        		}
            	list.restartList();
			}
    	}

        cpu.gpr[2] = 0;
    }

    public void sceGeSetCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int cbdata_addr = cpu.gpr[4];

        pspGeCallbackData cbdata = new pspGeCallbackData();
        cbdata.read(Emulator.getMemory(), cbdata_addr);

        int cbid = SceUidManager.getNewUid("pspge-callback");

        if (log.isDebugEnabled()) {
        	log.debug("sceGeSetCallback signalFunc=0x" + Integer.toHexString(cbdata.signalFunction)
                              + ", signalArg=0x" + Integer.toHexString(cbdata.signalArgument)
                              + ", finishFunc=0x" + Integer.toHexString(cbdata.finishFunction)
                              + ", finishArg=0x" + Integer.toHexString(cbdata.finishArgument)
                              + ", result cbid=" + Integer.toHexString(cbid));
        }

         if (IntrManager.getInstance().isInsideInterrupt()) {
             log.warn("sceGeSetCallback called inside an Interrupt!");
             cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
         } else {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelCallbackInfo callbackSignal = threadMan.hleKernelCreateCallback("GeCallbackSignal", cbdata.signalFunction, cbdata.signalArgument);
            SceKernelCallbackInfo callbackFinish = threadMan.hleKernelCreateCallback("GeCallbackFinish", cbdata.finishFunction, cbdata.finishArgument);
            signalCallbacks.put(cbid, callbackSignal);
            finishCallbacks.put(cbid, callbackFinish);

            cpu.gpr[2] = cbid;
        }
    }

    public void sceGeUnsetCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int cbid = cpu.gpr[4];

        if (log.isDebugEnabled()) {
    		log.debug("sceGeUnsetCallback cbid=" + Integer.toHexString(cbid));
    	}

        if (IntrManager.getInstance().isInsideInterrupt()) {
             log.warn("sceGeUnsetCallback called inside an Interrupt!");
             cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
        } else {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelCallbackInfo callbackSignal = signalCallbacks.remove(cbid);
            SceKernelCallbackInfo callbackFinish = finishCallbacks.remove(cbid);
            if (callbackSignal != null) {
                threadMan.hleKernelDeleteCallback(callbackSignal.uid);
            }
            if (callbackFinish != null) {
                threadMan.hleKernelDeleteCallback(callbackFinish.uid);
            }
            cpu.gpr[2] = 0;
        }
    }
    public final HLEModuleFunction sceGeEdramGetSizeFunction = new HLEModuleFunction("sceGe_user", "sceGeEdramGetSize") {

        @Override
        public final void execute(Processor processor) {
            sceGeEdramGetSize(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeEdramGetSize(processor);";
        }
    };
    public final HLEModuleFunction sceGeEdramGetAddrFunction = new HLEModuleFunction("sceGe_user", "sceGeEdramGetAddr") {

        @Override
        public final void execute(Processor processor) {
            sceGeEdramGetAddr(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeEdramGetAddr(processor);";
        }
    };
    public final HLEModuleFunction sceGeEdramSetAddrTranslationFunction = new HLEModuleFunction("sceGe_user", "sceGeEdramSetAddrTranslation") {

        @Override
        public final void execute(Processor processor) {
            sceGeEdramSetAddrTranslation(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeEdramSetAddrTranslation(processor);";
        }
    };
    public final HLEModuleFunction sceGeGetCmdFunction = new HLEModuleFunction("sceGe_user", "sceGeGetCmd") {

        @Override
        public final void execute(Processor processor) {
            sceGeGetCmd(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeGetCmd(processor);";
        }
    };
    public final HLEModuleFunction sceGeGetMtxFunction = new HLEModuleFunction("sceGe_user", "sceGeGetMtx") {

        @Override
        public final void execute(Processor processor) {
            sceGeGetMtx(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeGetMtx(processor);";
        }
    };
    public final HLEModuleFunction sceGeSaveContextFunction = new HLEModuleFunction("sceGe_user", "sceGeSaveContext") {

        @Override
        public final void execute(Processor processor) {
            sceGeSaveContext(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeSaveContext(processor);";
        }
    };
    public final HLEModuleFunction sceGeRestoreContextFunction = new HLEModuleFunction("sceGe_user", "sceGeRestoreContext") {

        @Override
        public final void execute(Processor processor) {
            sceGeRestoreContext(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeRestoreContext(processor);";
        }
    };
    public final HLEModuleFunction sceGeListEnQueueFunction = new HLEModuleFunction("sceGe_user", "sceGeListEnQueue") {

        @Override
        public final void execute(Processor processor) {
            sceGeListEnQueue(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeListEnQueue(processor);";
        }
    };
    public final HLEModuleFunction sceGeListEnQueueHeadFunction = new HLEModuleFunction("sceGe_user", "sceGeListEnQueueHead") {

        @Override
        public final void execute(Processor processor) {
            sceGeListEnQueueHead(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeListEnQueueHead(processor);";
        }
    };
    public final HLEModuleFunction sceGeListDeQueueFunction = new HLEModuleFunction("sceGe_user", "sceGeListDeQueue") {

        @Override
        public final void execute(Processor processor) {
            sceGeListDeQueue(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeListDeQueue(processor);";
        }
    };
    public final HLEModuleFunction sceGeListUpdateStallAddrFunction = new HLEModuleFunction("sceGe_user", "sceGeListUpdateStallAddr") {

        @Override
        public final void execute(Processor processor) {
            sceGeListUpdateStallAddr(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeListUpdateStallAddr(processor);";
        }
    };
    public final HLEModuleFunction sceGeListSyncFunction = new HLEModuleFunction("sceGe_user", "sceGeListSync") {

        @Override
        public final void execute(Processor processor) {
            sceGeListSync(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeListSync(processor);";
        }
    };
    public final HLEModuleFunction sceGeDrawSyncFunction = new HLEModuleFunction("sceGe_user", "sceGeDrawSync") {

        @Override
        public final void execute(Processor processor) {
            sceGeDrawSync(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeDrawSync(processor);";
        }
    };
    public final HLEModuleFunction sceGeBreakFunction = new HLEModuleFunction("sceGe_user", "sceGeBreak") {

        @Override
        public final void execute(Processor processor) {
            sceGeBreak(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeBreak(processor);";
        }
    };
    public final HLEModuleFunction sceGeContinueFunction = new HLEModuleFunction("sceGe_user", "sceGeContinue") {

        @Override
        public final void execute(Processor processor) {
            sceGeContinue(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeContinue(processor);";
        }
    };
    public final HLEModuleFunction sceGeSetCallbackFunction = new HLEModuleFunction("sceGe_user", "sceGeSetCallback") {

        @Override
        public final void execute(Processor processor) {
            sceGeSetCallback(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeSetCallback(processor);";
        }
    };
    public final HLEModuleFunction sceGeUnsetCallbackFunction = new HLEModuleFunction("sceGe_user", "sceGeUnsetCallback") {

        @Override
        public final void execute(Processor processor) {
            sceGeUnsetCallback(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGe_userModule.sceGeUnsetCallback(processor);";
        }
    };
}