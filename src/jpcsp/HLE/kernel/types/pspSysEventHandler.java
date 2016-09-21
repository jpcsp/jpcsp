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

public class pspSysEventHandler extends pspAbstractMemoryMappedStructure {
	public int size;
	public int nameAddr;
	public String name;
	public int typeMask;
	public int handlerAddr;
	public int gp;
	public boolean busy;
	public int next;
	public int reserved[] = new int[9];

	@Override
	public int sizeof() {
		return 64;
	}

	@Override
	protected void read() {
		size = read32();
		nameAddr = read32();
		typeMask = read32();
		handlerAddr = read32();
		gp = read32();
		busy = readBoolean();
		next = read32();
		for (int i = 0; i < reserved.length; i++) {
			reserved[i] = read32();
		}

		if (nameAddr != 0) {
			name = readStringZ(nameAddr);
		}
	}

	@Override
	protected void write() {
		write32(size);
		write32(nameAddr);
		write32(typeMask);
		write32(handlerAddr);
		write32(gp);
		writeBoolean(busy);
		write32(next);
		for (int i = 0; i < reserved.length; i++) {
			write32(reserved[i]);
		}
	}

	@Override
	public String toString() {
		return String.format("size=0x%X, name=0x%08X('%s'), typeMask=0x%X, handler=0x%08X, gp=0x%08X, busy=%b, next=0x%08X", size, nameAddr, name, typeMask, handlerAddr, gp, busy, next);
	}
}
