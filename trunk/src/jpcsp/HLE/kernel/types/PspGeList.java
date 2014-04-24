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

import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_CANCEL_DONE;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_DONE;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_DRAWING;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_QUEUED;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_STALL_REACHED;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_STRINGS;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceGe_user;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.externalge.ExternalGE;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.Memory;
import jpcsp.MemoryMap;

public class PspGeList {
	private VideoEngine videoEngine;
	private static final int pcAddressMask = 0xFFFFFFFC & Memory.addressMask;
	private Memory mem;
    public int list_addr;
    private int stall_addr;
    public int cbid;
    public int arg_addr;
    public pspGeListOptParam optParams;

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

    public PspGeList(int id) {
    	videoEngine = VideoEngine.getInstance();
    	mem = Memory.getInstance();
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
        pc = 0;
        saveContextAddr = 0;
    }

    public void init(int list_addr, int stall_addr, int cbid, int arg_addr) {
        init();

        list_addr &= pcAddressMask;
        stall_addr &= pcAddressMask;

        this.list_addr = list_addr;
        this.stall_addr = stall_addr;
        this.cbid = cbid;
        this.arg_addr = arg_addr;

        if (Memory.isAddressGood(arg_addr)) {
            optParams = new pspGeListOptParam();
            optParams.read(mem, arg_addr);
        }
        setPc(list_addr);
        status = (pc == stall_addr) ? PSP_GE_LIST_STALL_REACHED : PSP_GE_LIST_QUEUED;
    	finished = false;
    	reset = false;
        ended = false;

    	sync = new Semaphore(0);
    }

    public void reset() {
    	status = PSP_GE_LIST_DONE;
    	init();
    }

    public void pushSignalCallback(int listId, int behavior, int signal) {
    	int listPc = getPc();
    	if (!ExternalGE.isActive()) {
    		// PC address after the END command
    		listPc += 4;
    	}
        Modules.sceGe_userModule.triggerSignalCallback(cbid, listId, listPc, behavior, signal);
    }

    public void pushFinishCallback(int listId, int arg) {
    	int listPc = getPc();
    	if (!ExternalGE.isActive()) {
    		// PC address after the END command
    		listPc += 4;
    	}
    	Modules.sceGe_userModule.triggerFinishCallback(cbid, listId, listPc, arg);
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

    public void jumpAbsolute(int argument) {
    	setPc(Memory.normalizeAddress(argument));
    }

    public void jumpRelative(int argument) {
    	setPc(getAddressRel(argument));
    }

    public void jumpRelativeOffset(int argument) {
    	setPc(getAddressRelOffset(argument));
    }

    public void callAbsolute(int argument) {
    	pushStack(pc);
    	pushStack(videoEngine.getBaseOffset());
    	jumpAbsolute(argument);
    }

    public void callRelative(int argument) {
    	pushStack(pc);
    	pushStack(videoEngine.getBaseOffset());
    	jumpRelative(argument);
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
				sceGe_user.log.debug(String.format("PspGeList waitForSync %s", e));
			}
    	}

    	return true;
    }

    public void setStallAddr(int stall_addr) {
    	stall_addr &= pcAddressMask;
    	if (this.stall_addr != stall_addr) {
    		this.stall_addr = stall_addr;
			ExternalGE.onStallAddrUpdated(this);
    		sync();
    	}
    }

    public int getStallAddr() {
    	return stall_addr;
    }

    public boolean isStallReached() {
    	return pc == stall_addr && stall_addr != 0;
    }

    public void startList() {
    	paused = false;
    	ExternalGE.onGeStartList(this);
    	if (ExternalGE.isActive()) {
    		ExternalGE.startList(this);
    	} else {
    		videoEngine.pushDrawList(this);
    	}
    	sync();
    }

    public void startListHead() {
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
    }

    public void restartList() {
    	paused = false;
    	restarted = true;
    	sync();
		ExternalGE.onRestartList(this);
    }

    public void clearRestart() {
    	restarted = false;
    }

    public void clearPaused() {
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

	private void resetMemoryReader() {
		memoryReader = MemoryReader.getMemoryReader(pc, 4);
	}

	private void resetMemoryReader(int oldPc) {
		if (memoryReader == null || pc < oldPc) {
			resetMemoryReader();
		} else if (oldPc < MemoryMap.START_RAM && pc >= MemoryMap.START_RAM) {
			// Jumping from VRAM to RAM
			resetMemoryReader();
		} else {
			memoryReader.skip((pc - oldPc) >> 2);
		}
	}

	public void setMemoryReader(IMemoryReader memoryReader) {
		this.memoryReader = memoryReader;
	}

	public int readNextInstruction() {
		pc += 4;
		return memoryReader.readNext();
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

	@Override
	public String toString() {
		return String.format("PspGeList[id=0x%X, status=%s, list=0x%08X, pc=0x%08X, stall=0x%08X, cbid=0x%X, ended=%b, finished=%b, paused=%b, restarted=%b, reset=%b]", id, PSP_GE_LIST_STRINGS[status], list_addr, pc, stall_addr, cbid, ended, finished, paused, restarted, reset);
	}
}