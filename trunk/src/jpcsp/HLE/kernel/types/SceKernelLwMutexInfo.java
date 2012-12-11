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

import jpcsp.Memory;
import jpcsp.HLE.kernel.managers.SceUidManager;

public class SceKernelLwMutexInfo extends pspAbstractMemoryMappedStructureVariableLength {
    public final String name;
    public final int attr;
    public final int lwMutexUid;
    public final int lwMutexOpaqueWorkAreaAddr;
    public final int initCount;
    public int lockedCount;
    public int numWaitThreads;

    public final int uid;
    public int threadid;

    public SceKernelLwMutexInfo(int workArea, String name, int count, int attr) {
        Memory mem = Memory.getInstance();
        this.lwMutexUid = 0;
        this.lwMutexOpaqueWorkAreaAddr = workArea;
        this.name = name;
        this.attr = attr;

        initCount = count;
        lockedCount = count;
        numWaitThreads = 0;

        uid = SceUidManager.getNewUid("ThreadMan-LwMutex");
        mem.write32(lwMutexOpaqueWorkAreaAddr, uid);
    }

	@Override
	protected void write() {
		super.write();
		writeStringNZ(32, name);
		write32(attr);
		write32(lwMutexUid);
		write32(lwMutexOpaqueWorkAreaAddr);
		write32(initCount);
		write32(lockedCount);
		write32(numWaitThreads);
	}

    @Override
    public String toString() {
        return String.format("SceKernelLwMutexInfo(uid=0x%X, name=%s, mutexUid=0x%X, lwMutexOpaqueWorkAreaAddr=0x%X, initCount=%d, lockedCount=%d, numWaitThreads=%d, attr=0x%X, threadid=0x%X)", uid, name, lwMutexUid, lwMutexOpaqueWorkAreaAddr, initCount, lockedCount, numWaitThreads, attr, threadid);
    }
}