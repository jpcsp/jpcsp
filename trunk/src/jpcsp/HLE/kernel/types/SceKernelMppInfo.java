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
    private static final String uidPurpose = "ThreadMan-MsgPipe";
    public int userAddress;
    public int userSize;

    public SceKernelMppInfo(String name, int partitionid, int attr, int size, int memType) {
        this.name = name;
        this.attr = attr;

        bufSize = size;
        freeSize = size;

        if (size != 0) {
            sysMemInfo = Modules.SysMemUserForUserModule.malloc(partitionid, "ThreadMan-MsgPipe", memType, size, 0);
            address = sysMemInfo.addr;
        } else {
        	sysMemInfo = null;
        	address = 0;
        }

        uid = SceUidManager.getNewUid(uidPurpose);
        sendThreadWaitingList = ThreadWaitingList.createThreadWaitingList(SceKernelThreadInfo.PSP_WAIT_MSGPIPE, uid, attr, MsgPipeManager.PSP_MPP_ATTR_SEND_PRIORITY);
        receiveThreadWaitingList = ThreadWaitingList.createThreadWaitingList(SceKernelThreadInfo.PSP_WAIT_MSGPIPE, uid, attr, MsgPipeManager.PSP_MPP_ATTR_RECEIVE_PRIORITY);
        this.partitionid = partitionid;
        head = 0;
        tail = 0;
    }

    public boolean isMemoryAllocated() {
    	return bufSize == 0 || sysMemInfo != null;
    }

    public void delete() {
    	if (sysMemInfo != null) {
    		Modules.SysMemUserForUserModule.free(sysMemInfo);
    	}
    	SceUidManager.releaseUid(uid, uidPurpose);
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
    	if (bufSize == 0) {
    		return getUserSize();
    	}
        return bufSize - freeSize;
    }

    public int availableWriteSize() {
        return freeSize;
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
        if (bufSize == 0) {
        	mem.memcpy(dst, userAddress, size);
        	userAddress += size;
        	userSize -= size;
        } else {
        	freeSize += size;

	        while (size > 0) {
	            int copySize = Math.min(bufSize - head, size);
	            mem.memcpy(dst, address + head, copySize);
	            dst += copySize;
	            size -= copySize;
	            head = (head + copySize) % bufSize;
	        }
        }
    }

    public int getNumSendWaitThreads() {
		return sendThreadWaitingList.getNumWaitingThreads();
	}

    public int getNumReceiveWaitThreads() {
		return receiveThreadWaitingList.getNumWaitingThreads();
	}

    public void setUserData(int address, int size) {
    	userAddress = address;
    	userSize = size;
    }

    public int getUserSize() {
    	return userSize;
    }

    @Override
	public String toString() {
		return String.format("SceKernelMppInfo(uid=0x%X, name='%s', attr=0x%X, bufSize=0x%X, freeSize=0x%X, numSendWaitThreads=%d, numReceiveWaitThreads=%d)", uid, name, attr, bufSize, freeSize, getNumSendWaitThreads(), getNumReceiveWaitThreads());
	}
}