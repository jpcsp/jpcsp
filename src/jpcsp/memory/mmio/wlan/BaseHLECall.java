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

import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.hasBit;

import org.apache.log4j.Logger;

import jpcsp.arm.ARMProcessor;
import jpcsp.arm.IARMHLECall;

/**
 * @author gid15
 *
 */
public abstract class BaseHLECall implements IARMHLECall {
	public static Logger log = WlanEmulator.log;

	protected void jump(ARMProcessor processor, int addr) {
		if (hasBit(addr, 0)) {
			addr = clearBit(addr, 0);
			processor.setThumbMode();
		} else {
			processor.setARMMode();
		}
		processor.jump(addr);
	}

	protected void returnToLr(ARMProcessor processor) {
		jump(processor, processor.getLr());
	}
}
