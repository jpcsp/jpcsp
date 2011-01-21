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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
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
	// Implement stack as an array for more efficiency (time critical)
	private JumpState[] stack = new JumpState[0];
	private int stackIndex = -1;

	public RuntimeThread(SceKernelThreadInfo threadInfo) {
		this.threadInfo = threadInfo;
		threadInfo.javaThreadId = getId();
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
		setInSyscall(true);

		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		if (threadMXBean.isThreadCpuTimeEnabled()) {
			threadInfo.javaThreadCpuTimeNanos = threadMXBean.getCurrentThreadCpuTime();
		}
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

	public int pushStackState(int ra, int sp) {
		for (int i = stackIndex; i >= 0; i--) {
			JumpState state = stack[i];
			if (state.ra == ra) {
				int previousSp = state.sp;
				state.sp = sp;
				return previousSp;
			}
		}

		stackIndex++;
		if (stackIndex == stack.length) {
			// Extend the stack array
			JumpState[] newStack = new JumpState[stack.length + 1];
			System.arraycopy(stack, 0, newStack, 0, stack.length);
			newStack[stack.length] = new JumpState();
			stack = newStack;
		}

		stack[stackIndex].setState(ra, sp);

		return 0;
	}

	public void popStackState(int ra, int previousSp) {
		if (previousSp != 0) {
			for (int i = stackIndex; i >= 0; i--) {
				JumpState state = stack[i];
				if (state.ra == ra) {
					state.sp = previousSp;
					return;
				}
			}
		}
		stackIndex--;
	}

	public boolean hasStackState(int ra, int sp) {
		for (JumpState state : stack) {
			if (state.ra == ra) {
				return true;
			}
		}

		return false;
	}

	public String getStackString() {
		StringBuilder result = new StringBuilder();

		for (int i = stackIndex; i >= 0; i--) {
			if (result.length() > 0) {
				result.append(",");
			}
			result.append(stack[i].toString());
		}

		return result.toString();
	}

	public static class JumpState {
		public int ra;
		public int sp;

		public void setState(int ra, int sp) {
			this.ra = ra;
			this.sp = sp;
		}

		@Override
		public String toString() {
			return String.format("(ra=0x%08X, sp=0x%08X)", ra, sp);
		}
	}
}
