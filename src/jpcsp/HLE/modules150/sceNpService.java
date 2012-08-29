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
package jpcsp.HLE.modules150;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

@HLELogging
public class sceNpService extends HLEModule {
    public static Logger log = Modules.getLogger("sceNpService");

    @Override
    public String getName() {
        return "sceNpService";
    }

    private int npManagerMemSize;     // Memory allocated by the NP Manager utility.
    private int npManagerMaxMemSize;  // Maximum memory used by the NP Manager utility.
    private int npManagerFreeMemSize; // Free memory available to use by the NP Manager utility.

    @HLEFunction(nid = 0x0F8F5821, version = 150, checkInsideInterrupt = true)
    public int sceNpService_0F8F5821(int poolSize, int stackSize, int threadPriority) {
        npManagerMemSize = poolSize;
        npManagerMaxMemSize = poolSize / 2;    // Dummy
        npManagerFreeMemSize = poolSize - 16;  // Dummy.

        return 0;
    }

    @HLEFunction(nid = 0x00ACFAC3, version = 150, checkInsideInterrupt = true)
    public int sceNpService_00ACFAC3(TPointer32 memStatAddr) {
        memStatAddr.setValue(0, npManagerMemSize);
        memStatAddr.setValue(4, npManagerMaxMemSize);
        memStatAddr.setValue(8, npManagerFreeMemSize);

        return 0;
    }
}