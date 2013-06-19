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

import java.util.Comparator;

import jpcsp.Memory;

public class SceKernelMsgPacket extends pspAbstractMemoryMappedStructure implements Comparator<SceKernelMsgPacket> {
	private static final int OFFSET_NEXT_MSG_PACKET_ADDR = 0;
	private static final int OFFSET_MSG_PRIORITY = 4;
	public int nextMsgPacketAddr;
	int msgPriority; // SceUChar
	int unknow0; // SceUChar
	int unknow1; // SceUChar
	int unknow2; // SceUChar

	@Override
	protected void read() {
		nextMsgPacketAddr = read32();
		msgPriority = read8();
		unknow0 = read8();
		unknow1 = read8();
		unknow2 = read8();
	}

	@Override
	protected void write() {
		write32(nextMsgPacketAddr);
		write8((byte) msgPriority);
		write8((byte) unknow0);
		write8((byte) unknow1);
		write8((byte) unknow2);
	}

	public static void writeNext(Memory mem, int msgAddr, int nextMsgAddr) {
		mem.write32(msgAddr + OFFSET_NEXT_MSG_PACKET_ADDR, nextMsgAddr);
	}

	public static int readNext(Memory mem, int msgAddr) {
		return mem.read32(msgAddr + OFFSET_NEXT_MSG_PACKET_ADDR);
	}

    @Override
	public int sizeof() {
		return 8;
	}

    public static int compare(Memory mem, int msgAddr1, int msgAddr2) {
    	return mem.read8(msgAddr1 + OFFSET_MSG_PRIORITY) - mem.read8(msgAddr2 + OFFSET_MSG_PRIORITY);
    }

    @Override
    public int compare(SceKernelMsgPacket m1, SceKernelMsgPacket m2) {
        return m1.msgPriority - m2.msgPriority;
    }
}
