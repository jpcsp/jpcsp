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
package jpcsp.memory.mmio.wlan;

import jpcsp.arm.ARMProcessor;

/**
 * @author gid15
 *
 */
public class HLEStrncpy extends BaseHLECall {
	@Override
	public void call(ARMProcessor processor, int imm) {
		int dest = getParameterValue(processor, 0);
		int src = getParameterValue(processor, 1);
		int size = getParameterValue(processor, 2);

		if (log.isTraceEnabled()) {
			log.trace(String.format("strncpy 0x%08X, 0x%08X(\"%s\"), 0x%X", dest, src, readStringNZ(processor, src, size), size));
		}

		// Simply continue with the normal execution of the strncpy code
	}
}
