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
package jpcsp.HLE.modules600;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;

@HLELogging
public class sceAtrac3plus extends jpcsp.HLE.modules250.sceAtrac3plus {
	public AtracID getAtracIdFromContext(int atrac3Context) {
		for (AtracID id : atracIDs.values()) {
			SysMemInfo context = id.getContext();
			if (context != null && context.addr == atrac3Context) {
				return id;
			}
		}

		return null;
	}

	@HLELogging(level="info")
	@HLEFunction(nid = 0x231FC6B7, version = 600, checkInsideInterrupt = true)
    public int _sceAtracGetContextAddress(int at3IDNum) {
        AtracID id = atracIDs.get(at3IDNum);
        if (id == null) {
        	return 0;
        }

        id.createContext();
        SysMemInfo atracContext = id.getContext();
        if (atracContext == null) {
        	return 0;
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("_sceAtracGetContextAddress returning 0x%08X", atracContext.addr));
        }

        return atracContext.addr;
    }
}