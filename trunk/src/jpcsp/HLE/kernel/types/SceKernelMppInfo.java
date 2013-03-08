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
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.MsgPipeManager;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.managers.ThreadWaitingList;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;

public class SceKernelMppInfo extends pspAbstractMemoryMappedStructureVariableLength {

    // PSP info
    public final String name;
    public final int attr;
    public final int bufSize;
    public int freeSize;
    public final ThreadWaitingList sendThreadWaitingList;
    public final ThreadWaitingList receiveThreadWaitingList;

    private final SysMemInfo sysMemInfo;
    // Internal info
    public final int uid;
    public final int partitionid;
    public final int address;
    private int head; // relative to address
    private int tail; // relative to address

    private SceKernelMppInfo(String name, int partitionid, int attr, int size, int memType) {
        this.name = name;
        this.attr = attr;

        bufSize = size;
        freeSize = size;

        sysMemInfo = Modules.SysMemUserForUserModule.malloc(partitionid, "ThreadMan-MsgPipe", memType, size, 0);
        if (sysMemInfo == null) {
            throw new RuntimeException("SceKernelFplInfo: not enough free mem");
        }
        address = sysMemInfo.addr;

        uid = SceUidManager.getNewUid("ThreadMan-MsgPipe");
        sendThreadWaitingList = ThreadWaitingList.createThreadWaitingList(SceKernelThreadInfo.PSP_WAIT_MSGPIPE, uid, attr, MsgPipeManager.PSP_MPP_ATTR_SEND_PRIORITY);
        receiveThreadWaitingList = ThreadWaitingList.createThreadWaitingList(SceKernelThreadInfo.PSP_WAIT_MSGPIPE, uid, attr, MsgPipeManager.PSP_MPP_ATTR_RECEIVE_PRIORITY);
        this.partitionid = partitionid;
        head = 0;
        tail = 0;
    }

    public static SceKernelMppInfo tryCreateMpp(String name, int partitionid, int attr, int size, int memType) {
        SceKernelMppInfo info = null;
        int alignedSize = (size + 0xFF) & ~0xFF; // 256 byte align (or is this stage done by pspsysmem? aren't we using 64-bytes in pspsysmem?)
        int maxFreeSize = Modules.SysMemUserForUserModule.maxFreeMemSize();

        if (size <= 0) {
            Modules.log.warn("tryCreateMpp invalid size " + size);
        } else if (alignedSize > maxFreeSize) {
            Modules.log.warn("tryCreateMpp not enough free mem (want=" + alignedSize + ",free=" + maxFreeSize + ",diff=" + (alignedSize - maxFreeSize) + ")");
        } else {
            info = new SceKernelMppInfo(name, partitionid, attr, size, memType);
        }

        return info;
    }

	@Override
	protected void write() {
		super.write();
		writeStringNZ(32, name);
		write32(attr);
		write32(bufSize);
		write32(freeSize);
		write32(getNumSendWaitThreads());
		write32(getNumReceiveWaitThreads());
	}

    public int availableReadSize() {
        return bufSize - freeSize;
    }

    public int availableWriteSize() {
        return freeSize;
    }

    public void deleteSysMemInfo() {
        Modules.SysMemUserForUserModule.free(sysMemInfo);
    }

    // this will clobber itself if used carelessly but won't overflow outside of its allocated memory
    public void append(Memory mem, int src, int size) {
        int copySize;

        freeSize -= size;

        while (size > 0) {
            copySize = Math.min(bufSize - tail, size);
            mem.memcpy(address + tail, src, copySize);
            src += copySize;
            size -= copySize;
            tail = (tail + copySize) % bufSize;
        }
    }

    public void consume(Memory mem, int dst, int size) {
        int copySize;

        freeSize += size;

        while (size > 0) {
            copySize = Math.min(bufSize - head, size);
            mem.memcpy(dst, address + head, copySize);
            dst += copySize;
            size -= copySize;
            head = (head + copySize) % bufSize;
        }
    }

    public int getNumSendWaitThreads() {
		return sendThreadWaitingList.getNumWaitingThreads();
	}

    public int getNumReceiveWaitThreads() {
		return receiveThreadWaitingList.getNumWaitingThreads();
	}

	@Override
	public String toString() {
		return String.format("SceKernelMppInfo(uid=0x%X, name='%s', attr=0x%X, bufSize=0x%X, freeSize=0x%X, numSendWaitThreads=%d, numReceiveWaitThreads=%d)", uid, name, attr, bufSize, freeSize, getNumSendWaitThreads(), getNumReceiveWaitThreads());
	}
}