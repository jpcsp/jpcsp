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

public class SceSysmemMemoryBlockInfo extends pspAbstractMemoryMappedStructureVariableLength {
	public String name;
	public int attr;
	public int addr;
	public int memSize;
	public int sizeLocked;
	public int unused;

	@Override
	protected void read() {
		super.read();
		name = readStringNZ(32);
		attr = read32();
		addr = read32();
		memSize = read32();
		sizeLocked = read32();
		unused = read32();
	}

	@Override
	protected void write() {
		super.write();
		writeStringN(32, name);
		write32(attr);
		write32(addr);
		write32(memSize);
		write32(sizeLocked);
		write32(unused);
	}
}
