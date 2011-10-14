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
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_QUEUED;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_STALL_REACHED;
import static jpcsp.HLE.modules150.sceGe_user.PSP_GE_LIST_STRINGS;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import jpcsp.HLE.Modules;
import jpcsp.graphics.VideoEngine;
import jpcsp.Memory;

public class PspGeList
{
	private VideoEngine videoEngine;
	private static final int pcAddressMask = 0xFFFFFFFC;
	private Memory mem;
    public int list_addr;
    private int stall_addr;
    public int cbid;
    public int arg_addr;
    public pspGeListOptParam optParams;

    public int pc;

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
    }

    public void init(int list_addr, int stall_addr, int cbid, int arg_addr) {
        Memory mem = Memory.getInstance();
        init();

        this.list_addr = list_addr;
        this.stall_addr = stall_addr;
        this.cbid = cbid;
        this.arg_addr = arg_addr;

        if(Memory.isAddressGood(arg_addr)) {
            optParams = new pspGeListOptParam();
            optParams.read(mem, arg_addr);
        }
        pc = list_addr;
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
        Modules.sceGe_userModule.triggerSignalCallback(cbid, listId, behavior, signal);
    }

    public void pushFinishCallback(int listId, int arg) {
    	Modules.sceGe_userModule.triggerFinishCallback(cbid, listId, arg);
    }

    private void pushStack(int value) {
    	stack[stackIndex++] = value;
    }

    private int popStack() {
    	return stack[--stackIndex];
    }

    public int getAddressRel(int argument) {
    	return mem.normalizeAddress((videoEngine.getBase() | argument));
    }

    public int getAddressRelOffset(int argument) {
    	return mem.normalizeAddress((videoEngine.getBase() | argument) + videoEngine.getBaseOffset());
    }

    public boolean isStackEmpty() {
    	return stackIndex <= 0;
    }

    public void jumpAbsolute(int argument) {
    	pc = mem.normalizeAddress(argument) & pcAddressMask;
    }

    public void jumpRelative(int argument) {
    	pc = getAddressRel(argument) & pcAddressMask;
    }

    public void jumpRelativeOffset(int argument) {
    	pc = getAddressRelOffset(argument) & pcAddressMask;
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
    		pc = popStack();
    	}
    }

    private void sync() {
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
			}
    	}

    	return true;
    }

    public void setStallAddr(int stall_addr) {
    	if (this.stall_addr != stall_addr) {
    		this.stall_addr = stall_addr;
    		sync();
    	}
    }

    public int getStallAddr() {
    	return stall_addr;
    }

    public boolean isStallReached() {
    	return pc == stall_addr;
    }

    public void startList() {
    	paused = false;
        videoEngine.pushDrawList(this);
    }

    public void startListHead() {
        paused = false;
        videoEngine.pushDrawListHead(this);
    }

    public void pauseList() {
    	paused = true;
    }

    public void restartList() {
    	paused = false;
    	restarted = true;
    	sync();
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
    }

    public void endList() {
        if(isFinished()) {
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

	@Override
	public String toString() {
		return String.format("PspGeList[id=0x%x, status=%s, pc=0x%08X, stall=0x%08X, cbid=0x%X, ended=%b, finished=%b, paused=%b, restarted=%b, reset=%b]", id, PSP_GE_LIST_STRINGS[status], pc, stall_addr, cbid, ended, finished, paused, restarted, reset);
	}
}