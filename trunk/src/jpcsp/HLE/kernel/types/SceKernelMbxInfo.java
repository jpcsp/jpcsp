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
import jpcsp.Memory;

public class SceKernelMbxInfo extends pspAbstractMemoryMappedStructure {
    //Mbx info
    public int size;
    public String name;
    public int attr;
    public int numWaitThreads;
    private int numMessages;
    private int firstMessage_addr;

    // Internal info
    public final int uid;
    public int lastMessage_addr;

    public SceKernelMbxInfo(String name, int attr) {
        this.name = name;
        this.attr = attr;

        this.numWaitThreads = 0;
        this.numMessages = 0;
        this.firstMessage_addr = 0;
        this.lastMessage_addr = 0;

        this.uid = SceUidManager.getNewUid("ThreadMan-Mbx");
    }

	@Override
	protected void read() {
		size = read32();
		setMaxSize(size);
		name = readStringNZ(32);
		attr = read32();
		numWaitThreads = read32();
		numMessages = read32();
		firstMessage_addr = read32();
	}

	@Override
	protected void write() {
		setMaxSize(size);
		write32(size);
		writeStringNZ(32, name);
		write32(attr);
		write32(numWaitThreads);
		write32(numMessages);
		write32(firstMessage_addr);
	}

	@Override
	public int sizeof() {
		return size;
	}

    public int removeMsg(Memory mem) {
    	int msgAddr = firstMessage_addr;

    	if (msgAddr != 0) {
    		SceKernelMsgPacket packet = new SceKernelMsgPacket();
    		packet.read(mem, msgAddr);
    		firstMessage_addr = packet.nextMsgPacketAddr;
    		if (firstMessage_addr == 0) {
    			lastMessage_addr = 0;
    		}
    		packet.nextMsgPacketAddr = 0;
    		packet.write(mem);

    		numMessages--;
    	}

    	return msgAddr;
    }

    public void addMsg(Memory mem, int msgAddr) {
    	if (msgAddr != 0) {
    		SceKernelMsgPacket packet = new SceKernelMsgPacket();
    		packet.read(mem, msgAddr);
    		packet.nextMsgPacketAddr = 0;
    		packet.write(mem);

    		if (lastMessage_addr == 0) {
    			firstMessage_addr = msgAddr;
    			lastMessage_addr = msgAddr;
    		} else {
    			SceKernelMsgPacket lastPacket = new SceKernelMsgPacket();
    			lastPacket.read(mem, lastMessage_addr);
    			lastPacket.nextMsgPacketAddr = msgAddr;
    			lastPacket.write(mem);
    			lastMessage_addr = msgAddr;
    		}

    		numMessages++;
    	}
    }

    public boolean hasMessage() {
    	return numMessages > 0;
    }
}