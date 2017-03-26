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

public class SceNetWlanMessage extends pspAbstractMemoryMappedStructure {
	public pspNetMacAddress dstMacAddress;
	public pspNetMacAddress srcMacAddress;
	public int unknown12;
	public int unknown14;
	public int unknown16;
	public int unknown17;
	public int unknown18;

	@Override
	protected void read() {
		dstMacAddress = new pspNetMacAddress();
		read(dstMacAddress);
		srcMacAddress = new pspNetMacAddress();
		read(srcMacAddress);
		unknown12 = read16();
		unknown14 = read16();
		unknown16 = read8();
		unknown17 = read8();
		unknown18 = endianSwap16((short) read16());
	}

	@Override
	protected void write() {
		write(dstMacAddress);
		write(srcMacAddress);
		write16((short) unknown12);
		write16((short) unknown14);
		write8((byte) unknown16);
		write8((byte) unknown17);
		write16((short) endianSwap16((short) unknown18));
	}

	@Override
	public int sizeof() {
		return 20;
	}

	@Override
	public String toString() {
		return String.format("dstMac=%s, srcMac=%s, unknown12=0x%X, unknown14=0x%X, unknown16=0x%X, unknown17=0x%X, unknown18=0x%X", dstMacAddress, srcMacAddress, unknown12, unknown14, unknown16, unknown17, unknown18);
	}
}
