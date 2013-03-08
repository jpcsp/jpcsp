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
import jpcsp.HLE.kernel.managers.MbxManager;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.managers.ThreadWaitingList;

public class SceKernelMbxInfo extends pspAbstractMemoryMappedStructureVariableLength {
    //Mbx info
    public final String name;
    public final int attr;
    public final ThreadWaitingList threadWaitingList;
    private int numMessages;
    private int firstMessageAddr;

    // Internal info
    public final int uid;
    public int lastMessageAddr;

    public SceKernelMbxInfo(String name, int attr) {
        this.name = name;
        this.attr = attr;

        numMessages = 0;
        firstMessageAddr = 0;
        lastMessageAddr = 0;

        uid = SceUidManager.getNewUid("ThreadMan-Mbx");
        threadWaitingList = ThreadWaitingList.createThreadWaitingList(SceKernelThreadInfo.PSP_WAIT_MBX, uid, attr, MbxManager.PSP_MBX_ATTR_PRIORITY);
    }

	@Override
	protected void write() {
		super.write();
		writeStringNZ(32, name);
		write32(attr);
		write32(getNumWaitThreads());
		write32(numMessages);
		write32(firstMessageAddr);
	}

    public int removeMsg(Memory mem) {
    	int msgAddr = firstMessageAddr;
    	if (msgAddr != 0) {
    		SceKernelMsgPacket packet = new SceKernelMsgPacket();
    		packet.read(mem, msgAddr);
    		firstMessageAddr = packet.nextMsgPacketAddr;
    		if (firstMessageAddr == 0) {
    			lastMessageAddr = 0;
    		}
    		packet.nextMsgPacketAddr = 0;
    		packet.write(mem);
    		numMessages--;
    	}
    	return msgAddr;
    }

    private void insertMsgAfter(Memory mem, int msgAddr, int refMsgAddr) {
		SceKernelMsgPacket msgPacket = new SceKernelMsgPacket();
		msgPacket.read(mem, msgAddr);

		if (lastMessageAddr == 0) {
			// Insert into an empty queue
			msgPacket.nextMsgPacketAddr = 0;

			firstMessageAddr = msgAddr;
    		lastMessageAddr = msgAddr;
		} else if (refMsgAddr == 0) {
			// Insert in front of the queue
			msgPacket.nextMsgPacketAddr = firstMessageAddr;

			firstMessageAddr = msgAddr;
    	} else {
    		// Insert in the middle of the queue
			SceKernelMsgPacket refMsgPacket = new SceKernelMsgPacket();
			refMsgPacket.read(mem, refMsgAddr);

			msgPacket.nextMsgPacketAddr = refMsgPacket.nextMsgPacketAddr;

			refMsgPacket.nextMsgPacketAddr = msgAddr;
			refMsgPacket.write(mem);

			if (lastMessageAddr == refMsgAddr) {
				// Inset at the end of the queue
				lastMessageAddr = msgAddr;
			}
    	}

		msgPacket.write(mem);

		numMessages++;
    }

    public void addMsg(Memory mem, int msgAddr) {
    	if (msgAddr != 0) {
    		insertMsgAfter(mem, msgAddr, lastMessageAddr);
    	}
    }

    public void addMsgByPriority(Memory mem, int msgAddr) {
    	if (msgAddr != 0) {
    		SceKernelMsgPacket msgPacket = new SceKernelMsgPacket();
    		msgPacket.read(mem, msgAddr);
    		SceKernelMsgPacket currentMsgPacket = new SceKernelMsgPacket();
    		int currentMsgAddr = firstMessageAddr;
    		int previousMsgAddr = 0;
    		for (int i = 0; i < numMessages; i++) {
    			currentMsgPacket.read(mem, currentMsgAddr);
    			if (msgPacket.compare(msgPacket, currentMsgPacket) < 0) {
    				break;
    			}
    			previousMsgAddr = currentMsgAddr;
    		}
			insertMsgAfter(mem, msgAddr, previousMsgAddr);
    	}
    }

    public boolean hasMessage() {
    	return firstMessageAddr != 0;
    }

	public int getNumWaitThreads() {
		return threadWaitingList.getNumWaitingThreads();
	}

	@Override
	public String toString() {
		return String.format("SceKernelMbxInfo[uid=0x%X, name='%s', attr=0x%X, numWaitingThreads=%d]", uid, name, attr, getNumWaitThreads());
	}
}