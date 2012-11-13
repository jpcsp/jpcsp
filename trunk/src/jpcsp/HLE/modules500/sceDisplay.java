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
package jpcsp.HLE.modules500;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;

import org.lwjgl.LWJGLException;

@HLELogging
public class sceDisplay extends jpcsp.HLE.modules200.sceDisplay {
	public sceDisplay() throws LWJGLException {
		super();
	}

	/**
	 * Wait for Vblank start after multiple VSYNCs.
	 *
	 * @param cycleNum  Number of VSYNCs to wait before blocking the thread on VBLANK.
	 * @return 0
	 */
	@HLEFunction(nid = 0x40F1469C, version = 500, checkInsideInterrupt = true)
	public int sceDisplayWaitVblankStartMulti(int cycleNum) {
        blockCurrentThreadOnVblank(cycleNum, false);

        return 0;
	}

	/**
	 * Wait for Vblank start after multiple VSYNCs, with Callback execution.
	 *
	 * @param cycleNum  Number of VSYNCs to wait before blocking the thread on VBLANK.
	 * @return 0
	 */
	@HLEFunction(nid = 0x77ED8B3A, version = 500, checkInsideInterrupt = true)
	public int sceDisplayWaitVblankStartMultiCB(int cycleNum) {
        blockCurrentThreadOnVblank(cycleNum, true);

        return 0;
	}
}