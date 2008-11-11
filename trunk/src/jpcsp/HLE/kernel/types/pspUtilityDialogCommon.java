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

public class pspUtilityDialogCommon {
	public final static int unknown = 0x11111111;

	public int size;
	public int language;
	public int buttonSwap;
		public final static int BUTTON_ACCEPT_CIRCLE = 0;
		public final static int BUTTON_ACCEPT_CROSS  = 1;
	public int graphicsThread;	// 0x11
	public int accessThread;	// 0x13
	public int fontThread;		// 0x12
	public int soundThread;		// 0x10
	public int result;

	public void read(Memory mem, int address) {
		size           = mem.read32(address +  0);
		language       = mem.read32(address +  4);
		buttonSwap     = mem.read32(address +  8);
		graphicsThread = mem.read32(address + 12);
		accessThread   = mem.read32(address + 16);
		fontThread     = mem.read32(address + 20);
		soundThread    = mem.read32(address + 24);
		result         = mem.read32(address + 28);
	}

	public void write(Memory mem, int address) {
		mem.write32(address +   0, size);
		mem.write32(address +   4, language);
		mem.write32(address +   8, buttonSwap);
		mem.write32(address +  12, graphicsThread);
		mem.write32(address +  16, accessThread);
		mem.write32(address +  20, fontThread);
		mem.write32(address +  24, soundThread);
		mem.write32(address +  28, result);
		mem.write32(address +  32, unknown);
		mem.write32(address +  36, unknown);
		mem.write32(address +  40, unknown);
		mem.write32(address +  44, unknown);
	}

	public void writeResult(Memory mem, int address) {
		mem.write32(address + 28, result);
	}

	public int sizeof() {
		return 12 * 4;
	}
}
