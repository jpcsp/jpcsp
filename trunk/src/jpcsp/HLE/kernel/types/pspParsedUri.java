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

public class pspParsedUri extends pspAbstractMemoryMappedStructure {
	public int unknown;
	public int schemeAddr;
	public int userInfoUserNameAddr;
	public int userInfoPasswordAddr;
	public int hostAddr;
	public int pathAddr;
	public int queryAddr;
	public int fragmentAddr;
	public int port;

	@Override
	protected void read() {
		unknown = read32();
		schemeAddr = read32();
		userInfoUserNameAddr = read32();
		userInfoPasswordAddr = read32();
		hostAddr = read32();
		pathAddr = read32();
		queryAddr = read32();
		fragmentAddr = read32();
		port = read16();
		readUnknown(10);
	}

	@Override
	protected void write() {
		write32(unknown);
		write32(schemeAddr);
		write32(userInfoUserNameAddr);
		write32(userInfoPasswordAddr);
		write32(hostAddr);
		write32(pathAddr);
		write32(queryAddr);
		write32(fragmentAddr);
		write16((short) port);
		writeUnknown(10);
	}

	@Override
	public int sizeof() {
		return 44;
	}
}
