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

import jpcsp.HLE.Modules;
import jpcsp.util.Utilities;

public class SceSysmemUidCB extends pspAbstractMemoryMappedStructure {
	public int parent0;
	public int nextChild;
	public int meta;
	public int uid;
	public int nameAddr;
	public String name;
	public int childSize;
	public int size;
	public int attr;
	public int next;
	public int parent1;
	public int funcTable;

	@Override
	protected void read() {
		parent0 = read32(); // Offset 0
		nextChild = read32(); // Offset 4
		meta = read32(); // Offset 8
		uid = read32(); // Offset 12
		nameAddr = read32(); // Offset 16
		if (nameAddr == 0) {
			name = null;
		} else {
			name = readStringZ(nameAddr);
		}
		childSize = read8(); // Offset 20
		size = read8(); // Offset 21
		attr = read16(); // Offset 22
		next = read32(); // Offset 24
		parent1 = read32(); // Offset 28
		funcTable = read32(); // Offset 32
	}

	@Override
	protected void write() {
		write32(parent0);
		write32(nextChild);
		write32(meta);
		write32(uid);
		write32(nameAddr);
		write8((byte) childSize);
		write8((byte) size);
		write16((short) attr);
		write32(next);
		write32(parent1);
		write32(funcTable);
	}

	public void allocAndSetName(int uidHeap, String name) {
		this.name = name;
		if (name == null) {
			nameAddr = 0;
		} else {
			nameAddr = Modules.SysMemForKernelModule.sceKernelAllocHeapMemory(uidHeap, name.length() + 1);
			if (nameAddr < 0) {
				nameAddr = 0;
			} else if (nameAddr != 0) {
				Utilities.writeStringZ(mem, nameAddr, name);
			}
		}
	}

	@Override
	public int sizeof() {
		return 36;
	}
}
