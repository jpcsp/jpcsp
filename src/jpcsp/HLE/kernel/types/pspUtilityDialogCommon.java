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

public class pspUtilityDialogCommon extends pspAbstractMemoryMappedStructureVariableLength {
	public int language;
	public int buttonSwap;
		public final static int BUTTON_ACCEPT_CIRCLE = 0;
		public final static int BUTTON_ACCEPT_CROSS  = 1;
	public int graphicsThread;	// 0x11
	public int accessThread;	// 0x13
	public int fontThread;		// 0x12
	public int soundThread;		// 0x10
	public int result;

	@Override
	protected void read() {
		super.read();
		language       = read32();
		buttonSwap     = read32();
		graphicsThread = read32();
		accessThread   = read32();
		fontThread     = read32();
		soundThread    = read32();
		result         = read32();
		readUnknown(16);
	}

	@Override
	protected void write() {
		super.write();
		write32(language);
		write32(buttonSwap);
		write32(graphicsThread);
		write32(accessThread);
		write32(fontThread);
		write32(soundThread);
		write32(result);
		writeUnknown(16);
	}

	public void writeResult(Memory mem) {
		writeResult(mem, getBaseAddress());
	}

	public void writeResult(Memory mem, int address) {
		mem.write32(address + 28, result);
	}

	@Override
	public int sizeof() {
		return Math.min(12 * 4, super.sizeof());
	}

	public int totalSizeof() {
		return super.sizeof();
	}
}
