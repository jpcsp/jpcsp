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

import jpcsp.HLE.modules.sceNetInet;

public class pspNetSockAddrInternet extends pspAbstractMemoryMappedStructure {
	public int sin_len;
	public int sin_family;
	public int sin_port;
	public int sin_addr;
	public int sin_zero1;
	public int sin_zero2;

	@Override
	protected void read() {
		// start address is not 32-bit aligned
		sin_len = read8();
		sin_family = read8();
		sin_port = endianSwap16((short) readUnaligned16());
		sin_addr = readUnaligned32();
		sin_zero1 = readUnaligned32();
		sin_zero2 = readUnaligned32();
	}

	@Override
	protected void write() {
		// start address is not 32-bit aligned
		write8((byte) sin_len);
		write8((byte) sin_family);
		writeUnaligned16((short) endianSwap16((short) sin_port));
		writeUnaligned32(sin_addr);
		writeUnaligned32(sin_zero1);
		writeUnaligned32(sin_zero2);
	}

	@Override
	public int sizeof() {
		return 16;
	}

	@Override
	public String toString() {
		return String.format("pspNetSockAddrInternet[family=%d, port=%d, addr=0x%08X(%s)]", sin_family, sin_port, sin_addr, sceNetInet.internetAddressToString(sin_addr));
	}
}
