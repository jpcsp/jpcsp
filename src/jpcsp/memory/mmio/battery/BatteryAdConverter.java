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
package jpcsp.memory.mmio.battery;

import jpcsp.nec78k0.sfr.Nec78k0AdConverter;
import jpcsp.nec78k0.sfr.Nec78k0Scheduler;

/**
 * @author gid15
 *
 */
public class BatteryAdConverter extends Nec78k0AdConverter {
	public BatteryAdConverter(MMIOHandlerBatteryFirmwareSfr sfr, Nec78k0Scheduler scheduler) {
		super(sfr, scheduler);
	}

	@Override
	protected int updateResult(int inputChannel) {
		int result;

		switch (inputChannel) {
			case 0: // Volt
				if (log.isDebugEnabled()) {
					log.debug(String.format("BatteryAdConverter volt inputChannel=%d", inputChannel));
				}
				result = 0xD000;
				break;
			case 2:
				if (log.isDebugEnabled()) {
					log.debug(String.format("BatteryAdConverter unknown inputChannel=%d", inputChannel));
				}
				result = 0x8F40;
				break;
			case 3:
				if (log.isDebugEnabled()) {
					log.debug(String.format("BatteryAdConverter unknown inputChannel=%d", inputChannel));
				}
				result = 0x1000;
				break;
			default:
				log.error(String.format("BatteryAdConverter unknown inputChannel=%d", inputChannel));
				result = 0x0000;
				break;
		}

		return result;
	}
}
