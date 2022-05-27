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

import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.network.accesspoint.AccessPoint.IP_ADDRESS_LENGTH;

import java.io.EOFException;

import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.util.BytesPacket;

public class NetPacket extends BytesPacket {
	public NetPacket(int length) {
		super(length);
		setBigEndian();
	}

	public NetPacket(byte[] buffer) {
		super(buffer);
		setBigEndian();
	}

	public NetPacket(byte[] buffer, int length) {
		super(buffer, length);
		setBigEndian();
	}

	public NetPacket(byte[] buffer, int offset, int length) {
		super(buffer, offset, length);
		setBigEndian();
	}

	public pspNetMacAddress readMacAddress() throws EOFException {
		return readMacAddress(MAC_ADDRESS_LENGTH);
	}

	public pspNetMacAddress readMacAddress(int length) throws EOFException {
		pspNetMacAddress macAddress = new pspNetMacAddress();
		readBytes(macAddress.macAddress, 0, Math.min(length, MAC_ADDRESS_LENGTH));
		skip8(length - MAC_ADDRESS_LENGTH);
		return macAddress;
	}

	public byte[] readIpAddress() throws EOFException {
		return readIpAddress(IP_ADDRESS_LENGTH);
	}

	public byte[] readIpAddress(int length) throws EOFException {
		return readBytes(new byte[length]);
	}

	public String readDnsNameNotation() throws EOFException {
		StringBuilder name = new StringBuilder();

		while (true) {
			int numberBytes = read8();
			if (numberBytes == 0) {
				break;
			}

			if (name.length() > 0) {
				name.append('.');
			}

			for (int i = 0; i < numberBytes; i++) {
				name.append(readAsciiChar());
			}
		}

		return name.toString();
	}

	public String readLine() throws EOFException {
		StringBuilder line = new StringBuilder();

		while (true) {
			char c = readAsciiChar();
			if (c == '\r') {
				char c2 = readAsciiChar();
				if (c2 == '\n') {
					break;
				}
				line.append(c);
				line.append(c2);
			} else {
				line.append(c);
			}
		}

		return line.toString();
	}

	public void writeDnsNameNotation(String name) throws EOFException {
		if (name != null && name.length() > 0) {
			String[] parts = name.split("\\.");
			if (parts != null && parts.length > 0) {
				for (String part : parts) {
					int length = part.length();
					if (length > 0) {
						write8(length);
						for (int i = 0; i < length; i++) {
							writeAsciiChar(part.charAt(i));
						}
					}
				}
			}
		}

		write8(0);
	}

	public void writeMacAddress(pspNetMacAddress macAddress) throws EOFException {
		writeMacAddress(macAddress, MAC_ADDRESS_LENGTH);
	}

	public void writeMacAddress(pspNetMacAddress macAddress, int length) throws EOFException {
		writeBytes(macAddress.macAddress, 0, Math.min(length, MAC_ADDRESS_LENGTH));
		skip8(length - MAC_ADDRESS_LENGTH);
	}

	public void writeIpAddress(byte[] ip) throws EOFException {
		writeIpAddress(ip, IP_ADDRESS_LENGTH);
	}

	public void writeIpAddress(byte[] ip, int length) throws EOFException {
		writeBytes(ip, 0, length);
	}

	public static String getIpAddressString(byte[] ip) {
		return String.format("%d.%d.%d.%d", ip[0] & 0xFF, ip[1] & 0xFF, ip[2] & 0xFF, ip[3] & 0xFF);
	}
}
