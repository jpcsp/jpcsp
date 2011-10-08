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
package jpcsp.HLE.modules630;

import jpcsp.HLE.HLEFunction;

public class scePower extends jpcsp.HLE.modules150.scePower {

	@HLEFunction(nid = 0x469989AD, version = 630)
    public int scePower_469989AD(int pllClock, int cpuClock, int busClock) {
        // Identical to scePowerSetClockFrequency.
        this.pllClock = pllClock;
        this.cpuClock = cpuClock;
        this.busClock = busClock;

        if (log.isDebugEnabled()) {
        	log.debug(String.format("scePower_469989AD pll: %d, cpu: %d, bus: %d", pllClock, cpuClock, busClock));
        }

        return 0;
    }
}