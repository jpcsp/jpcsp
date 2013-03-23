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

import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.managers.LwMutexManager;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.managers.ThreadWaitingList;

public class SceKernelLwMutexInfo extends pspAbstractMemoryMappedStructureVariableLength {
    public final String name;
    public final int attr;
    public final int lwMutexUid;
    public final TPointer lwMutexOpaqueWorkAreaAddr;
    public final int initCount;
    public int lockedCount;
    public final ThreadWaitingList threadWaitingList;

    public final int uid;
    public int threadid;

    public SceKernelLwMutexInfo(TPointer workArea, String name, int count, int attr) {
        this.lwMutexUid = 0;
        this.lwMutexOpaqueWorkAreaAddr = workArea;
        this.name = name;
        this.attr = attr;

        initCount = count;
        lockedCount = count;

        // If the initial count is 0, the lwmutex is not acquired.
        if (count > 0) {
        	threadid = Modules.ThreadManForUserModule.getCurrentThreadID();
        } else {
        	threadid = -1;
        }

        uid = SceUidManager.getNewUid("ThreadMan-LwMutex");
        threadWaitingList = ThreadWaitingList.createThreadWaitingList(SceKernelThreadInfo.PSP_WAIT_LWMUTEX, uid, attr, LwMutexManager.PSP_LWMUTEX_ATTR_PRIORITY);

        lwMutexOpaqueWorkAreaAddr.setValue32(uid);
    }

	@Override
	protected void write() {
		super.write();
		writeStringNZ(32, name);
		write32(attr);
		write32(lwMutexUid);
		write32(lwMutexOpaqueWorkAreaAddr.getAddress());
		write32(initCount);
		write32(lockedCount);
		write32(threadid);
		write32(getNumWaitThreads());
	}

	public int getNumWaitThreads() {
		return threadWaitingList.getNumWaitingThreads();
	}

    @Override
    public String toString() {
        return String.format("SceKernelLwMutexInfo(uid=0x%X, name=%s, mutexUid=0x%X, lwMutexOpaqueWorkAreaAddr=%s, initCount=%d, lockedCount=%d, numWaitThreads=%d, attr=0x%X, threadid=0x%X)", uid, name, lwMutexUid, lwMutexOpaqueWorkAreaAddr, initCount, lockedCount, getNumWaitThreads(), attr, threadid);
    }
}