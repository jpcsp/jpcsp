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

/*
 * Computes the "Internet Checksum" as defined by RFC 1071.
 * See https://tools.ietf.org/html/rfc1071
 */
public class InternetChecksum {
	public static int computeInternetChecksum(byte[] buffer, int offset, int length) {
		NetPacket packet = new NetPacket(buffer, offset, length);
		int sum = 0;
		try {
			while (length > 1) {
				sum += packet.read16();
				length -= 2;
			}

			// Add left-over byte, if any
			if (length > 0) {
				sum += packet.read8() << 8;
			}
		} catch (EOFException e) {
			// Ignore exception
		}

		// Add the carry
		while ((sum >>> 16) != 0) {
			sum = (sum & 0xFFFF) + (sum >>> 16);
		}

		// Flip all the bits to obtain the checksum
		int checksum = sum ^ 0xFFFF;

		return checksum;
	}
}
