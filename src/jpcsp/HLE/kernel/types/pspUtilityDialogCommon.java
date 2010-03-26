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

public class pspUtilityDialogCommon extends pspAbstractMemoryMappedStructure {
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

	protected void read() {
		size           = read32();
		setMaxSize(size);
		language       = read32();
		buttonSwap     = read32();
		graphicsThread = read32();
		accessThread   = read32();
		fontThread     = read32();
		soundThread    = read32();
		result         = read32();
		readUnknown(16);
	}

	protected void write() {
	    setMaxSize(size);
		write32(size);
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
        
        int newResultAddr = ((address + size) - 4);

        // While most games fill the base address + size - 4 (end of dialog data)
        // with zeros (and occasionally with addresses), others fill this area
        // with 1 and then try to read them as an address, if it's not 0.

        // Final Fantasy expects to find a pointer to "0xBABEFACE" (????), when
        // saving, if it doesn't find 0 first.
        // Is this some kind of fake saving detection mechanism?

        if(mem.read32(newResultAddr) != 0) {
            mem.write32(newResultAddr, result);
            mem.write32(newResultAddr - 4, result);
        }
	}

	public int sizeof() {
		return 12 * 4;
	}
}
