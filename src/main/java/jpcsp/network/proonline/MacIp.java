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
package jpcsp.network.proonline;

import static jpcsp.network.proonline.ProOnlineNetworkAdapter.convertIpToString;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jpcsp.HLE.kernel.types.pspNetMacAddress;

/**
 * @author gid15
 *
 */
public class MacIp {
	public byte[] mac;
	public pspNetMacAddress macAddress;
	public int ip;
	public InetAddress inetAddress;

	public MacIp(byte[] mac, int ip) {
		setMac(mac);
		setIp(ip);
	}

	public void setMac(byte[] mac) {
		this.mac = mac.clone();
		macAddress = new pspNetMacAddress(this.mac);
	}

	public void setIp(int ip) {
		this.ip = ip;
		try {
			inetAddress = InetAddress.getByAddress(getRawIp(ip));
		} catch (UnknownHostException e) {
			ProOnlineNetworkAdapter.log.error("Incorrect IP", e);
		}
	}

	public static byte[] getRawIp(int ip) {
		byte[] rawIp = new byte[4];
		rawIp[0] = (byte) (ip);
		rawIp[1] = (byte) (ip >> 8);
		rawIp[2] = (byte) (ip >> 16);
		rawIp[3] = (byte) (ip >> 24);

		return rawIp;
	}

	@Override
	public String toString() {
		return String.format("MAC=%s, ip=%s", macAddress, convertIpToString(ip));
	}
}
