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
package jpcsp.HLE.kernel.types;

import static jpcsp.HLE.modules.sceGe_user.PSP_GE_LIST_CANCEL_DONE;
import static jpcsp.HLE.modules.sceGe_user.PSP_GE_LIST_DONE;
import static jpcsp.HLE.modules.sceGe_user.PSP_GE_LIST_DRAWING;
import static jpcsp.HLE.modules.sceGe_user.PSP_GE_LIST_QUEUED;
import static jpcsp.HLE.modules.sceGe_user.PSP_GE_LIST_STALL_REACHED;
import static jpcsp.HLE.modules.sceGe_user.PSP_GE_LIST_STRINGS;
import static jpcsp.HLE.modules.sceGe_user.log;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.scheduler.Scheduler.getNow;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jpcsp.HLE.Modules;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.externalge.ExternalGE;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.scheduler.Scheduler;
import jpcsp.Memory;
import jpcsp.MemoryMap;

public class PspGeList {
	private VideoEngine videoEngine;
	private static final int pcAddressMask = 0xFFFFFFFC & Memory.addressMask;
    public int list_addr;
    private int stall_addr;
    public int cbid;
    public pspGeListOptParam optParams;
    private int stackAddr;

    private int pc;

    // a stack entry contains the PC and the baseOffset
    private int[] stack = new int[32*2];
    private int stackIndex;

    public int status;
    public int id;

    public List<Integer> blockedThreadIds; // the threads we are blocking
    private boolean finished;
    private boolean paused;
    private boolean ended;
    private boolean reset;
    private boolean restarted;
    private Semaphore sync; // Used for async display
    private IMemoryReader memoryReader;
    private int saveContextAddr;
    private IMemoryReader baseMemoryReader;
    private int baseMemoryReaderStartAddress;
    private int baseMemoryReaderEndAddress;
    private long startTimestamp;
    private long minimumDuration;
    private long pauseTimestamp;
    private long pauseDuration;

    private static class OnGeListSyncDone implements IAction {
    	private PspGeList list;

		public OnGeListSyncDone(PspGeList list) {
			this.list = list;
		}

		@Override
		public void execute() {
			Modules.sceGe_userModule.hleGeListSyncDone(list);
		}
    }

    public PspGeList(int id) {
    	videoEngine = VideoEngine.getInstance();
    	this.id = id;
    	blockedThreadIds = new LinkedList<Integer>();
    	reset();
    }

    private void init() {
    	stackIndex = 0;
    	blockedThreadIds.clear();
    	finished = true;
    	paused = false;
    	reset = true;
        ended = true;
        restarted = false;
        memoryReader = null;
        baseMemoryReader = null;
        baseMemoryReaderStartAddress = 0;
        baseMemoryReaderEndAddress = 0;
        pc = 0;
        saveContextAddr = 0;
    }

    public void init(int list_addr, int stall_addr, int cbid, pspGeListOptParam optParams) {
        init();

        list_addr &= pcAddressMask;
        stall_addr &= pcAddressMask;

        this.list_addr = list_addr;
        this.stall_addr = stall_addr;
        this.cbid = cbid;
        this.optParams = optParams;

        if (optParams != null) {
        	stackAddr = optParams.stackAddr;
        } else {
        	stackAddr = 0;
        }
        setPc(list_addr);
        status = (pc == stall_addr) ? PSP_GE_LIST_STALL_REACHED : PSP_GE_LIST_QUEUED;
    	finished = false;
    	reset = false;
        ended = false;
        pauseDuration = 0L;
        minimumDuration = 0L;

    	sync = new Semaphore(0);
    }

    public void reset() {
    	status = PSP_GE_LIST_DONE;
    	init();
    }

    public void pushSignalCallback(int behavior, int signal) {
    	int listPc = getPc();
    	if (!ExternalGE.isActive()) {
    		// PC address after the END command
    		listPc += 4;
    	}
        Modules.sceGe_userModule.triggerSignalCallback(this, listPc, behavior, signal);
    }

    public void pushFinishCallback(int arg) {
    	int listPc = getPc();
    	if (!ExternalGE.isActive()) {
    		// PC address after the END command
    		listPc += 4;
    	}
    	Modules.sceGe_userModule.triggerFinishCallback(this, listPc, arg);
    }

    private void pushStack(int value) {
    	stack[stackIndex++] = value;
    }

    private int popStack() {
    	return stack[--stackIndex];
    }

    public int getAddressRel(int argument) {
    	return Memory.normalizeAddress((videoEngine.getBase() | argument));
    }

    public int getAddressRelOffset(int argument) {
    	return Memory.normalizeAddress((videoEngine.getBase() | argument) + videoEngine.getBaseOffset());
    }

    public boolean isStackEmpty() {
    	return stackIndex <= 0;
    }

    public void setPc(int pc) {
    	pc &= pcAddressMask;
    	if (this.pc != pc) {
    		int oldPc = this.pc;
    		this.pc = pc;
			resetMemoryReader(oldPc);
    	}
    }

	public int getPc() {
		return pc;
	}

	public void setListAddr(int addr) {
		list_addr = addr & pcAddressMask;
		setPc(list_addr);
	}

	public void jumpAbsolute(int argument) {
    	setPc(Memory.normalizeAddress(argument));
    }

    public void jumpRelativeOffset(int argument) {
    	setPc(getAddressRelOffset(argument));
    }

    public void callAbsolute(int argument) {
    	pushStack(pc);
    	pushStack(videoEngine.getBaseOffset());
    	jumpAbsolute(argument);
    }

    public void callRelativeOffset(int argument) {
    	pushStack(pc);
    	pushStack(videoEngine.getBaseOffset());
    	jumpRelativeOffset(argument);
    }

    public void ret() {
    	if (!isStackEmpty()) {
    		videoEngine.setBaseOffset(popStack());
    		setPc(popStack());
    	}
    }

    public void sync() {
		if (sync != null) {
			sync.release();
		}
    }

    public boolean waitForSync(int millis) {
    	while (true) {
	    	try {
	    		int availablePermits = sync.drainPermits();
	    		if (availablePermits > 0) {
	    			break;
	    		}

    			if (sync.tryAcquire(millis, TimeUnit.MILLISECONDS)) {
    				break;
    			}
				return false;
			} catch (InterruptedException e) {
				// Ignore exception and retry again
				log.debug(String.format("PspGeList waitForSync %s", e));
			}
    	}

    	return true;
    }

    public void setStallAddr(int stall_addr) {
    	stall_addr &= pcAddressMask;
    	if (this.stall_addr != stall_addr) {
    		int oldStallAddr = this.stall_addr;
    		this.stall_addr = stall_addr;
			ExternalGE.onStallAddrUpdated(this);
			VideoEngine.getInstance().onStallAddrUpdated(this, oldStallAddr);
    		sync();
    	}
    }

    public synchronized void setStallAddr(int stall_addr, IMemoryReader baseMemoryReader, int startAddress, int endAddress) {
    	// Both the stall address and the base memory reader need to be set at the same
    	// time in a synchronized call in order to avoid any race condition
    	// with the GUI thread (VideoEngine).
    	setStallAddr(stall_addr);

    	this.baseMemoryReader = baseMemoryReader;
		this.baseMemoryReaderStartAddress = startAddress;
		this.baseMemoryReaderEndAddress = endAddress;
		resetMemoryReader(pc);
    }

    public int getStallAddr() {
    	return stall_addr;
    }

    public boolean isStallReached() {
    	return pc == stall_addr && stall_addr != 0;
    }

    public boolean hasStallAddr() {
    	return stall_addr != 0;
    }

    public boolean isStalledAtStart() {
    	return isStallReached() && pc == list_addr;
    }

    public void startList() {
    	startTimestamp = getNow();
    	finished = false;
        ended = false;
    	paused = false;

    	sync = new Semaphore(0);
    	ExternalGE.onGeStartList(this);
    	if (ExternalGE.isActive()) {
    		ExternalGE.startList(this);
    	} else {
    		videoEngine.pushDrawList(this);
    	}
    	sync();
    }

    public void startListHead() {
    	startTimestamp = getNow();
        paused = false;
        ExternalGE.onGeStartList(this);
        if (ExternalGE.isActive()) {
        	ExternalGE.startListHead(this);
        } else {
        	videoEngine.pushDrawListHead(this);
        }
    }

    public void pauseList() {
    	paused = true;
    	pauseTimestamp = getNow();
    }

    private void updatePauseDuration() {
    	if (pauseTimestamp != 0L) {
    		pauseDuration += getNow() - pauseTimestamp;
    		pauseTimestamp = 0L;
    	}
    }

    public void restartList() {
    	updatePauseDuration();
    	paused = false;
    	restarted = true;
    	sync();
		ExternalGE.onRestartList(this);
    }

    public void clearRestart() {
    	restarted = false;
    }

    public void clearPaused() {
    	updatePauseDuration();
    	paused = false;
    }

    public boolean isRestarted() {
    	return restarted;
    }

    public boolean isPaused() {
    	return paused;
    }

    public boolean isFinished() {
    	return finished;
    }

    public boolean isEnded() {
        return ended;
    }

    public void finishList() {
    	finished = true;
    	ExternalGE.onGeFinishList(this);
    }

    public void endList() {
        if (isFinished()) {
            ended = true;
        } else {
            ended = false;
        }
    }

    public boolean isDone() {
    	return status == PSP_GE_LIST_DONE || status == PSP_GE_LIST_CANCEL_DONE;
    }

	public boolean isReset() {
		return reset;
	}

	public boolean isDrawing() {
		return status == PSP_GE_LIST_DRAWING;
	}

	private void resetMemoryReader(int oldPc) {
		if (pc == 0) {
			memoryReader = null;
		} else if (pc >= baseMemoryReaderStartAddress && pc < baseMemoryReaderEndAddress && baseMemoryReader != null) {
			memoryReader = baseMemoryReader;
			memoryReader.skip((pc - baseMemoryReader.getCurrentAddress()) >> 2);
		} else if (memoryReader == null || memoryReader == baseMemoryReader || pc < oldPc) {
			memoryReader = MemoryReader.getMemoryReader(pc, 4);
		} else if (oldPc < MemoryMap.START_RAM && pc >= MemoryMap.START_RAM) {
			// Jumping from VRAM to RAM
			memoryReader = MemoryReader.getMemoryReader(pc, 4);
		} else {
			memoryReader.skip((pc - oldPc) >> 2);
		}
	}

	public synchronized void setMemoryReader(IMemoryReader memoryReader) {
		this.memoryReader = memoryReader;
	}

	public boolean hasBaseMemoryReader() {
		return baseMemoryReader != null;
	}

	public synchronized int readNextInstruction() {
		if (memoryReader == baseMemoryReader && (pc < baseMemoryReaderStartAddress || pc >= baseMemoryReaderEndAddress)) {
			resetMemoryReader(pc - 4);
		}
		pc += 4;
		return memoryReader.readNext();
	}

	public synchronized int readPreviousInstruction() {
		memoryReader.skip(-2);
		int previousInstruction = memoryReader.readNext();
		memoryReader.skip(1);

		return previousInstruction;
	}

	public synchronized void undoRead() {
		undoRead(1);
	}

	public synchronized void undoRead(int n) {
		memoryReader.skip(-n);
	}

	public int getSaveContextAddr() {
		return saveContextAddr;
	}

	public void setSaveContextAddr(int saveContextAddr) {
		this.saveContextAddr = saveContextAddr;
	}

	public boolean hasSaveContextAddr() {
		return saveContextAddr != 0;
	}

	public boolean isInUse(int listAddr, int stackAddr) {
		if (list_addr == listAddr) {
			return true;
		}
		if (stackAddr != 0 && this.stackAddr == stackAddr) {
			return true;
		}

		return false;
	}

	public int getSyncStatus() {
		// Return the status PSP_GE_LIST_STALL_REACHED only when the stall address is reached.
		// I.e. return PSP_GE_LIST_DRAWING when the stall address has been recently updated
		// but the list processing has not yet been resumed and the status is still left
		// at the value PSP_GE_LIST_STALL_REACHED.
		if (status == PSP_GE_LIST_STALL_REACHED) {
			if (!isStallReached()) {
				return PSP_GE_LIST_DRAWING;
			}
		}

		return status;
	}

	public void onGeListSyncDone() {
		if (minimumDuration != 0L && getNow() < startTimestamp + pauseDuration + minimumDuration) {
			long schedule = startTimestamp + pauseDuration + minimumDuration;
			minimumDuration = 0L;
			pauseDuration = 0L;
			Scheduler.getInstance().addAction(schedule, new OnGeListSyncDone(this));
		} else {
			Modules.sceGe_userModule.hleGeListSyncDone(this);
		}
	}

	private void addMinimumDuration(int duration) {
		minimumDuration += duration;
	}

	public void onRenderSprite(int textureAddress, int renderedTextureWidth, int renderedTextureHeight, int textureFormat) {
		// The following textures are rendered at full speed:
		// - stored in VRAM
		// - small size
		// - indexed or compressed format
		if (Memory.isVRAM(textureAddress) || renderedTextureWidth < 128 || renderedTextureHeight <= 64 || textureFormat > TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
			return;
		}

		int duration = 0;
		if (renderedTextureWidth == 128 && renderedTextureHeight == 272) {
			// Probably a video texture being rendered in 4 vertical stripes (each 128 pixels wide)
			duration = 16666 / 4; // 60 FPS
		} else if (renderedTextureWidth >= 512 && renderedTextureHeight >= 256) {
			duration = 66666; // 15 FPS
		} else if (renderedTextureWidth >= 256 && renderedTextureHeight >= 256) {
			duration = 33333; // 30 FPS
		} else if (renderedTextureWidth >= 512 && renderedTextureHeight >= 128) {
			duration = 33333; // 30 FPS
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("onRenderSprite textureAddress=0x%08X, renderedTextureWidth=%d, renderedTextureHeight=%d, textureFormat=%d, duration=%dus", textureAddress, renderedTextureWidth, renderedTextureHeight, textureFormat, duration));
		}

		addMinimumDuration(duration);
	}

	@Override
	public String toString() {
		return String.format("PspGeList[id=0x%X, status=%s, list=0x%08X, pc=0x%08X, stall=0x%08X, cbid=0x%X, ended=%b, finished=%b, paused=%b, restarted=%b, reset=%b]", id, PSP_GE_LIST_STRINGS[status], list_addr, pc, stall_addr, cbid, ended, finished, paused, restarted, reset);
	}
}