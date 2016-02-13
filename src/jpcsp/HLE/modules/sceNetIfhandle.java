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
package jpcsp.HLE.modules;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.Modules;

import org.apache.log4j.Logger;

public class sceNetIfhandle extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetIfhandle");
    private int netDropRate;
    private int netDropDuration;

    @HLEFunction(nid = 0xC80181A2, version = 150, checkInsideInterrupt = true)
    public int sceNetGetDropRate(@CanBeNull TPointer32 dropRateAddr, @CanBeNull TPointer32 dropDurationAddr) {
        dropRateAddr.setValue(netDropRate);
        dropDurationAddr.setValue(netDropDuration);

        return 0;
    }

    @HLEFunction(nid = 0xFD8585E1, version = 150, checkInsideInterrupt = true)
    public int sceNetSetDropRate(int dropRate, int dropDuration) {
        netDropRate = dropRate;
        netDropDuration = dropDuration;

        return 0;
    }
}