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

import java.util.ListIterator;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;

/**
 * ThreadWaitingList where the list is ordered by the thread priority.
 * 
 * @author gid15
 *
 */
public class ThreadWaitingListPriority extends ThreadWaitingList {
	public ThreadWaitingListPriority(int waitType, int waitId) {
		super(waitType, waitId);
	}

	@Override
	public void addWaitingThread(SceKernelThreadInfo thread) {
		boolean added = false;

		if (!waitingThreads.isEmpty()) {
			for (ListIterator<Integer> lit = waitingThreads.listIterator(); lit.hasNext(); ) {
				int uid = lit.next().intValue();
				SceKernelThreadInfo waitingThread = Modules.ThreadManForUserModule.getThreadById(uid);
				if (waitingThread != null) {
					if (thread.currentPriority < waitingThread.currentPriority) {
						lit.previous();
						lit.add(thread.uid);
						added = true;
						break;
					}
				}
			}
		}

		if (!added) {
			waitingThreads.add(thread.uid);
		}
	}
}
