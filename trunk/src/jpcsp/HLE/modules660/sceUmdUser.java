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
package jpcsp.HLE.modules660;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;

@HLELogging
public class sceUmdUser extends jpcsp.HLE.modules200.sceUmdUser {
    @HLEUnimplemented
    @HLEFunction(nid = 0x14C6C45C, version = 660)
    public int sceUmdUnuseUMDInMsUsbWlan() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB103FA38, version = 660)
    public int sceUmdUseUMDInMsUsbWlan() {
        return 0;
    }
}