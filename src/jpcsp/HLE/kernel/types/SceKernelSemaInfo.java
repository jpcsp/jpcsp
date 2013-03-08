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

import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.managers.SemaManager;
import jpcsp.HLE.kernel.managers.ThreadWaitingList;

public class SceKernelSemaInfo extends pspAbstractMemoryMappedStructureVariableLength {
    public final String name;
    public final int attr;
    public final int initCount;
    public int currentCount;
    public final int maxCount;
    public final ThreadWaitingList threadWaitingList;

    public final int uid;

    public SceKernelSemaInfo(String name, int attr, int initCount, int maxCount) {
        this.name = name;
        this.attr = attr;
        this.initCount = initCount;
        this.currentCount = initCount;
        this.maxCount = maxCount;

        uid = SceUidManager.getNewUid("ThreadMan-sema");
        threadWaitingList = ThreadWaitingList.createThreadWaitingList(SceKernelThreadInfo.PSP_WAIT_SEMA, uid, attr, SemaManager.PSP_SEMA_ATTR_PRIORITY);
    }

	@Override
	protected void write() {
		super.write();
		writeStringNZ(32, name);
		write32(attr);
		write32(initCount);
		write32(currentCount);
		write32(maxCount);
		write32(getNumWaitThreads());
	}

	public int getNumWaitThreads() {
		return threadWaitingList.getNumWaitingThreads();
	}

	@Override
	public String toString() {
		return String.format("SceKernelSemaInfo(uid=0x%X, name='%s', attr=0x%X, currentCount=%d, maxCount=%d, numWaitThreads=%d)", uid, name, attr, currentCount, maxCount, getNumWaitThreads());
	}
}
