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

import java.util.ArrayList;
import java.util.Collections;

import jpcsp.Memory;
import jpcsp.HLE.kernel.managers.SceUidManager;

public class SceKernelMbxInfo extends pspAbstractMemoryMappedStructure {
    //Mbx info
    public int size;
    public String name;
    public int attr;
    public int numWaitThreads;
    private int numMessages;
    private int firstMessageAddr;

    // Internal info
    public final int uid;
    public int lastMessageAddr;
    public ArrayList<SceKernelMsgPacket> msgQueue;

    public SceKernelMbxInfo(String name, int attr) {
        this.name = name;
        this.attr = attr;

        numWaitThreads = 0;
        numMessages = 0;
        firstMessageAddr = 0;
        lastMessageAddr = 0;
        msgQueue = new ArrayList<SceKernelMsgPacket>();

        uid = SceUidManager.getNewUid("ThreadMan-Mbx");
    }

	@Override
	protected void read() {
		size = read32();
		setMaxSize(size);
		name = readStringNZ(32);
		attr = read32();
		numWaitThreads = read32();
		numMessages = read32();
		firstMessageAddr = read32();
	}

	@Override
	protected void write() {
		setMaxSize(size);
		write32(size);
		writeStringNZ(32, name);
		write32(attr);
		write32(numWaitThreads);
		write32(numMessages);
		write32(firstMessageAddr);
	}

	@Override
	public int sizeof() {
		return size;
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
            msgQueue.remove(packet);
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
            msgQueue.add(packet);
    		if (lastMessageAddr == 0) {
    			firstMessageAddr = msgAddr;
    			lastMessageAddr = msgAddr;
    		} else {
    			SceKernelMsgPacket lastPacket = new SceKernelMsgPacket();
    			lastPacket.read(mem, lastMessageAddr);
    			lastPacket.nextMsgPacketAddr = msgAddr;
    			lastPacket.write(mem);
    			lastMessageAddr = msgAddr;
    		}
    		numMessages++;
    	}
    }

    public void addMsgByPriority(Memory mem, int msgAddr) {
    	if (msgAddr != 0) {
    		SceKernelMsgPacket packet = new SceKernelMsgPacket();
    		packet.read(mem, msgAddr);
    		packet.nextMsgPacketAddr = 0;
    		packet.write(mem);
            msgQueue.add(packet);
            Collections.sort(msgQueue, packet);
            if(msgQueue.get(0) != null) {
                msgAddr = msgQueue.get(0).nextMsgPacketAddr;
            }
    		if (lastMessageAddr == 0) {
    			firstMessageAddr = msgAddr;
    			lastMessageAddr = msgAddr;
    		} else {
    			SceKernelMsgPacket lastPacket = new SceKernelMsgPacket();
    			lastPacket.read(mem, lastMessageAddr);
    			lastPacket.nextMsgPacketAddr = msgAddr;
    			lastPacket.write(mem);
    			lastMessageAddr = msgAddr;
    		}
    		numMessages++;
    	}
    }

    public boolean hasMessage() {
    	return numMessages > 0;
    }
}