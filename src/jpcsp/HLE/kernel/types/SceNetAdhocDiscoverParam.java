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

import static jpcsp.HLE.modules150.sceNetAdhocctl.GROUP_NAME_LENGTH;

public class SceNetAdhocDiscoverParam extends pspAbstractMemoryMappedStructure {
	public int unknown1;
	public String groupName;
	public int unknown2;
	public int result;

	public static final int NET_ADHOC_DISCOVER_RESULT_NO_PEER_FOUND = 0;
	public static final int NET_ADHOC_DISCOVER_RESULT_PEER_FOUND = 2;
	public static final int NET_ADHOC_DISCOVER_RESULT_ABORTED = 3;

	@Override
	protected void read() {
		unknown1 = read32();
		groupName = readStringNZ(GROUP_NAME_LENGTH);
		unknown2 = read32();
		result = read32();
	}

	@Override
	protected void write() {
		write32(unknown1);
		writeStringNZ(GROUP_NAME_LENGTH, groupName);
		write32(unknown2);
		write32(result);
	}

	@Override
	public int sizeof() {
		return 20;
	}

	@Override
	public String toString() {
		return String.format("SceNetAdhocDiscoverParam unknown1=0x%08X, groupName='%s', unknown2=0x%08X, result=0x%08X", unknown1, groupName, unknown2, result);
	}
}
