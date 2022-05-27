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
package jpcsp.network.protocols;

import java.io.EOFException;

import jpcsp.HLE.kernel.types.pspNetMacAddress;

public class EtherFrame {
    public static final int ETHER_TYPE_IPv4 = 0x0800;
    public static final int ETHER_TYPE_ARP = 0x0806;
	// Frame specification as defined by IEEE Std 802.3
	// Frame:
	//     destination address: 6 octets
	//     source address: 6 octets
	//     length/type: 2 octets
	//     client data: 46 to 1500 octets
	public pspNetMacAddress dstMac;
	public pspNetMacAddress srcMac;
	public int type;

	public EtherFrame() {
	}

	public EtherFrame(EtherFrame frame) {
		dstMac = frame.dstMac;
		srcMac = frame.srcMac;
		type = frame.type;
	}

	public void swapSourceAndDestination() {
		pspNetMacAddress mac = srcMac;
		srcMac = dstMac;
		dstMac = mac;
	}

	public void read(NetPacket packet) throws EOFException {
		dstMac = packet.readMacAddress();
		srcMac = packet.readMacAddress();
		type = packet.read16();
	}

	public NetPacket write(NetPacket packet) throws EOFException {
		packet.writeMacAddress(dstMac);
		packet.writeMacAddress(srcMac);
		packet.write16(type);

		return packet;
	}

	public NetPacket write() throws EOFException {
		return write(new NetPacket(sizeOf()));
	}

	public static int sizeOf() {
		return 14;
	}

	@Override
	public String toString() {
		return String.format("dstMac=%s, srcMac=%s, type/length=0x%04X", dstMac, srcMac, type);
	}
}
