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

import java.util.concurrent.Semaphore;

import jpcsp.HLE.kernel.types.SceKernelThreadInfo;

/**
 * @author gid15
 *
 */
public class RuntimeThread extends Thread {
	private Semaphore semaphore = new Semaphore(1);
	private SceKernelThreadInfo threadInfo;

	public RuntimeThread(SceKernelThreadInfo threadInfo) {
		this.threadInfo = threadInfo;
		setName(threadInfo.name);
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
}
