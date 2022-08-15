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
package jpcsp.Allegrex.compiler.nativeCode;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_INDEX;
import static jpcsp.memory.mmio.MMIOHandlerGpio.GPIO_PORT_SYSCON_END_CMD;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.sleep;

import jpcsp.memory.mmio.MMIOHandlerGpio;
import jpcsp.memory.mmio.syscon.SysconEmulator;

/**
 * @author gid15
 *
 */
public class TestGpio extends AbstractNativeCodeSequence {
	static public void call() {
		int bit = getGprA0();
		int result;
		if (bit >= 0 && bit < 32) {
			result = hasBit(MMIOHandlerGpio.getInstance().getInterruptTriggered(), bit) ? 1 : 0;

			if (result == 0 && bit == GPIO_PORT_SYSCON_END_CMD && SysconEmulator.isEnabled()) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Introducing small delay while testing end of Syscon Command in Syscon Emulator"));
				}
				sleep(1, 0);
			}
		} else {
			result = ERROR_INVALID_INDEX;
		}
		setGprV0(result);
	}
}
