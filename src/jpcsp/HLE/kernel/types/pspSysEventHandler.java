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

import static jpcsp.util.Utilities.hasFlag;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointerFunction;

public class pspSysEventHandler extends pspAbstractMemoryMappedStructure {
	public static final int SIZEOF = 64;
	public int size;
	public int nameAddr;
	public String name;
	public int typeMask;
	public TPointerFunction handler;
	public int gp;
	public boolean busy;
	public TPointer next;
	public int reserved[] = new int[9];

	@Override
	public int sizeof() {
		return SIZEOF;
	}

	@Override
	protected void read() {
		size = read32();
		nameAddr = read32();
		typeMask = read32();
		handler = readPointerFunction();
		gp = read32();
		busy = readBoolean();
		next = readPointer();
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
		writePointerFunction(handler);
		write32(gp);
		writeBoolean(busy);
		writePointer(next);
		for (int i = 0; i < reserved.length; i++) {
			write32(reserved[i]);
		}
	}

	public static TPointer getNext(TPointer sysEventHandler) {
		return sysEventHandler.getPointer(24);
	}

	public static void setNext(TPointer sysEventHandler, TPointer next) {
		sysEventHandler.setPointer(24, next);
	}

	public static boolean isMatchingTypeMask(TPointer sysEventHandler, int typeMask) {
		return hasFlag(typeMask, sysEventHandler.getValue32(8));
	}

	public static boolean isBusy(TPointer sysEventHandler) {
		return sysEventHandler.getUnsignedValue8(20) != 0;
	}

	public static void setBusy(TPointer sysEventHandler, boolean busy) {
		sysEventHandler.setUnsignedValue8(20, busy ? 1 : 0);
	}

	@Override
	public String toString() {
		return String.format("size=0x%X, name=0x%08X('%s'), typeMask=0x%X, handler=%s, gp=0x%08X, busy=%b, next=%s", size, nameAddr, name, typeMask, handler, gp, busy, next);
	}
}
