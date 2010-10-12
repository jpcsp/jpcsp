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
package jpcsp.Allegrex.compiler;

import java.util.Stack;
import java.util.concurrent.Semaphore;

import jpcsp.HLE.kernel.types.SceKernelThreadInfo;

/**
 * @author gid15
 *
 */
public class RuntimeThread extends Thread {
	private Semaphore semaphore = new Semaphore(1);
	private SceKernelThreadInfo threadInfo;
	private boolean isInSyscall;
	public Stack<JumpState> stack = new Stack<JumpState>();

	public RuntimeThread(SceKernelThreadInfo threadInfo) {
		this.threadInfo = threadInfo;
		isInSyscall = false;
		if (RuntimeContext.log.isDebugEnabled()) {
			setName(threadInfo.name + "_" + Integer.toHexString(threadInfo.uid));
		} else {
			setName(threadInfo.name);
		}
		suspendRuntimeExecution();
	}

	@Override
	public void run() {
		RuntimeContext.runThread(this);
	}

	public void suspendRuntimeExecution() {
		boolean acquired = false;

		while (!acquired) {
			try {
				semaphore.acquire();
				acquired = true;
			} catch (InterruptedException e) {
			}
		}
	}

	public void continueRuntimeExecution() {
		semaphore.release();
	}

	public SceKernelThreadInfo getThreadInfo() {
		return threadInfo;
	}

	public boolean isInSyscall() {
		return isInSyscall;
	}

	public void setInSyscall(boolean isInSyscall) {
		this.isInSyscall = isInSyscall;
	}

	public void pushStackState(int ra, int sp) {
		stack.add(new JumpState(ra, sp));
	}

	public JumpState popStackState() {
		return stack.pop();
	}

	public boolean hasStackState(int ra, int sp) {
		for (JumpState state : stack) {
			if (state.ra == ra) {
				return true;
			}
		}

		return false;
	}

	public Stack<JumpState> getStack() {
		return stack;
	}

	public static class JumpState {
		public int ra;
		public int sp;

		public JumpState(int ra, int sp) {
			this.ra = ra;
			this.sp = sp;
		}

		@Override
		public String toString() {
			return String.format("(ra=0x%08X, sp=0x%08X)", ra, sp);
		}
	}
}
