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

public class pspIoDrv extends pspAbstractMemoryMappedStructure {
	int nameAddr;
	String name;
	int devType;
	int unknown;
	int name2Addr;
	String name2;
	int funcsAddr;

	@Override
	protected void read() {
		nameAddr = read32();
		devType = read32();
		unknown = read32();
		name2Addr = read32();
		funcsAddr = read32();

		name = readStringZ(nameAddr);
		name2 = readStringZ(name2Addr);
	}

	@Override
	protected void write() {
		write32(nameAddr);
		write32(devType);
		write32(unknown);
		write32(name2Addr);
		write32(funcsAddr);
	}

	@Override
	public int sizeof() {
		return 20;
	}

	@Override
	public String toString() {
		return String.format("name=0x%08X('%s'), devType=0x%X, unknown=0x%X, name2=0x%08X('%s'), funcsAddr=0x%08X", nameAddr, name, devType, unknown, name2Addr, name2, funcsAddr);
	}
}
