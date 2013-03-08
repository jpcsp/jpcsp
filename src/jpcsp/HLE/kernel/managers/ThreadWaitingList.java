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
package jpcsp.HLE.kernel.managers;

import java.util.LinkedList;
import java.util.List;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;

/**
 * Base implementation of a list of waiting threads.
 * Two implementations are provided to implement a FIFO list
 * and a list ordered by the thread priority.
 * 
 * @author gid15
 *
 */
public abstract class ThreadWaitingList {
	protected List<Integer> waitingThreads = new LinkedList<Integer>();
	protected int waitType;
	protected int waitId;

	public static ThreadWaitingList createThreadWaitingList(int waitType, int waitId, int attr, int attrPrioriyFlag) {
		if ((attr & attrPrioriyFlag) == attrPrioriyFlag) {
			return new ThreadWaitingListPriority(waitType, waitId);
		}
		return new ThreadWaitingListFIFO(waitType, waitId);
	}

	protected ThreadWaitingList(int waitType, int waitId) {
		this.waitType = waitType;
		this.waitId = waitId;
	}

	public int getNumWaitingThreads() {
		return waitingThreads.size();
	}

	public abstract void addWaitingThread(SceKernelThreadInfo thread);

	public void removeWaitingThread(SceKernelThreadInfo thread) {
		waitingThreads.remove(new Integer(thread.uid));
	}

	public SceKernelThreadInfo getNextWaitingThread(SceKernelThreadInfo baseThread) {
		if (baseThread == null) {
			return getFirstWaitingThread();
		}

		int index = waitingThreads.indexOf(baseThread.uid);
		if (index < 0 || (index + 1) >= getNumWaitingThreads()) {
			return null;
		}

		int uid = waitingThreads.get(index + 1);
		SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getThreadById(uid);

		// Is the thread still existing
		if (thread == null) {
			// Thread is no longer existing, delete it from the waiting list and retry
			waitingThreads.remove(index + 1);
			return getNextWaitingThread(baseThread);
		}

		// Is the thread still waiting on this ID?
		if (!thread.isWaitingForType(waitType) || thread.waitId != waitId) {
			// The thread is no longer waiting on this object, remove it from the waiting list and retry
			waitingThreads.remove(index + 1);
			return getNextWaitingThread(baseThread);
		}

		return thread;
	}

	public SceKernelThreadInfo getFirstWaitingThread() {
		// Is the waiting list empty?
		if (getNumWaitingThreads() <= 0) {
			return null;
		}

		int uid = waitingThreads.get(0);
		SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getThreadById(uid);

		// Is the thread still existing
		if (thread == null) {
			// Thread is no longer existing, delete it from the waiting list and retry
			waitingThreads.remove(0);
			return getFirstWaitingThread();
		}

		// Is the thread still waiting on this ID?
		if (!thread.isWaitingForType(waitType) || thread.waitId != waitId) {
			// The thread is no longer waiting on this object, remove it from the waiting list and retry
			waitingThreads.remove(0);
			return getFirstWaitingThread();
		}

		return thread;
	}

	public void removeAllWaitingThreads() {
		waitingThreads.clear();
	}
}
