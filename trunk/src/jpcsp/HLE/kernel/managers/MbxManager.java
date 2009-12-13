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
package jpcsp.HLE.kernel.managers;

import jpcsp.Emulator;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import static jpcsp.HLE.kernel.types.SceKernelErrors.*;

public class MbxManager {
	public void reset() {
	}

	/**
	 * Check if a message has arrived in a messagebox
	 *
	 * @par Example:
	 * @code
	 * void *msg;
	 * sceKernelPollMbx(mbxid, &msg);
	 * @endcode
	 *
	 * @param mbxid - The mbx id returned from sceKernelCreateMbx
	 * @param messageAddr - A pointer to where a pointer to the
	 *                   received message should be stored
	 *
	 * @return < 0 on error (ERROR_MESSAGEBOX_NO_MESSAGE if the mbx is empty).
	 */
	public void sceKernelPollMbx(int mbxid, int messageAddr) {
        CpuState cpu = Emulator.getProcessor().cpu;

        Modules.log.warn("Unimplemented sceKernelPollMbx(" + mbxid + ", 0x" + Integer.toHexString(messageAddr) + ")");
        // TODO Fake no message available
		cpu.gpr[2] = ERROR_MESSAGEBOX_NO_MESSAGE;
	}

	public static final MbxManager singleton;

    private MbxManager() {
    }

    static {
        singleton = new MbxManager();
    }
}
