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

import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_High;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_HighAligned;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules150.SysMemUserForUser.PSP_SMEM_LowAligned;
import static jpcsp.HLE.modules620.ThreadManForUser.PSP_ATTR_ADDR_HIGH;
import static jpcsp.util.Utilities.alignUp;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;

/**
 * Thread-local storage introduced in PSP 6.20.
 *
 */
public class SceKernelTls {
	public String name;
	public int attr;
	public int blockSize;
	public int alignedBlockSize;
	public int numberBlocks;
	public int uid;
	private static final String uidPurpose = "SceKernelTls";
	private SysMemInfo sysMemInfo;
	private int[] threadIds;

	public SceKernelTls(String name, int partitionId, int attr, int blockSize, int alignedBlockSize, int numberBlocks, int alignment) {
		blockSize = alignUp(blockSize, 3);

		this.name = name;
		this.attr = attr;
		this.blockSize = blockSize;
		this.alignedBlockSize = alignedBlockSize;
		this.numberBlocks = numberBlocks;

		int type = alignment == 0 ? PSP_SMEM_Low : PSP_SMEM_LowAligned;
		if ((attr & PSP_ATTR_ADDR_HIGH) != 0) {
			type = alignment == 0 ? PSP_SMEM_High : PSP_SMEM_HighAligned;
		}

		int size = alignedBlockSize * numberBlocks;

		sysMemInfo = Modules.SysMemUserForUserModule.malloc(partitionId, name, type, size, alignment);
		uid = SceUidManager.getNewUid(uidPurpose);

		threadIds = new int[numberBlocks];
	}

	public void free() {
		Memory.getInstance().memset(sysMemInfo.addr, (byte) 0, sysMemInfo.allocatedSize); 
		Modules.SysMemUserForUserModule.free(sysMemInfo);
		sysMemInfo = null;
		SceUidManager.releaseUid(uid, uidPurpose);
		uid = -1;
	}

	public int getBaseAddress() {
		if (sysMemInfo == null) {
			return 0;
		}

		return sysMemInfo.addr;
	}

	public void freeTlsAddress() {
		if (sysMemInfo == null) {
			return;
		}

		int currentThreadId = Modules.ThreadManForUserModule.getCurrentThreadID();
		for (int i = 0; i < threadIds.length; i++) {
			if (threadIds[i] == currentThreadId) {
				threadIds[i] = 0;
				break;
			}
		}
	}

	public int getTlsAddress() {
		if (sysMemInfo == null) {
			return 0;
		}

		int currentThreadId = Modules.ThreadManForUserModule.getCurrentThreadID();
		int block = -1;
		// If a block has already been allocated for this thread, use it
		for (int i = 0; i < threadIds.length; i++) {
			if (threadIds[i] == currentThreadId) {
				block = i;
				break;
			}
		}

		if (block < 0) {
			// Return the first free block
			for (int i = 0; i < threadIds.length; i++) {
				if (threadIds[i] == 0) {
					block = i;
					threadIds[block] = currentThreadId;
					break;
				}
			}

			if (block < 0) {
				return 0;
			}
		}

		return sysMemInfo.addr + block * alignedBlockSize;
	}
}
