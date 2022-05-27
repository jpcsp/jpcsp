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

import jpcsp.HLE.modules.sceNetAdhocctl;

/** Peer info structure */
public class SceNetAdhocctlPeerInfo extends pspAbstractMemoryMappedStructure {
	public int nextAddr;
	/** Nickname */
	public String nickName;
	/** Mac address */
	public pspNetMacAddress macAddress;
	/** Time stamp */
	public long timestamp;

	@Override
	protected void read() {
		nextAddr = read32();
		nickName = readStringNZ(sceNetAdhocctl.NICK_NAME_LENGTH);
		macAddress = new pspNetMacAddress();
		read(macAddress);
		readUnknown(6);
		timestamp = read64();
	}

	@Override
	protected void write() {
		write32(nextAddr);
		writeStringNZ(sceNetAdhocctl.NICK_NAME_LENGTH, nickName);
		write(macAddress);
		writeUnknown(6);
		write64(timestamp);
	}

	@Override
	public int sizeof() {
		return 152;
	}

	@Override
	public String toString() {
		return String.format("nickName='%s', macAddress=%s, timestamp=%d", nickName, macAddress, timestamp);
	}
}
