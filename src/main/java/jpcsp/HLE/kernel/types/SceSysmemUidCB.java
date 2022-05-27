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

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
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
	}

	@Override
	protected void write() {
		write32(parent0); // Offset 0
		write32(nextChild); // Offset 4
		write32(meta); // Offset 8
		write32(uid); // Offset 12
		write32(nameAddr); // Offset 16
		write8((byte) childSize); // Offset 20
		write8((byte) size); // Offset 21
		write16((short) attr); // Offset 22
		write32(next); // Offset 24
	}

	public void allocAndSetName(Memory mem, int uidHeap, String name) {
		if (name == null) {
			nameAddr = 0;
		} else {
			nameAddr = Modules.SysMemForKernelModule.sceKernelAllocHeapMemory(uidHeap, name.length() + 1);
			if (nameAddr < 0) {
				nameAddr = 0;
				name = null;
			} else if (nameAddr != 0) {
				Utilities.writeStringZ(mem, nameAddr, name);
			}
		}
		this.name = name;
	}

	public void freeName(int uidHeap) {
		if (nameAddr != 0) {
			Modules.SysMemForKernelModule.sceKernelFreeHeapMemory(uidHeap, new TPointer(mem, nameAddr));
			nameAddr = 0;
			name = null;
		}
	}

	@Override
	public int sizeof() {
		return 28;
	}
}
