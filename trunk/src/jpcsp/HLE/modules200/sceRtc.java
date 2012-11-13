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
package jpcsp.HLE.modules200;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer64;

import java.util.Calendar;

import jpcsp.HLE.kernel.types.ScePspDateTime;

@HLELogging
public class sceRtc extends jpcsp.HLE.modules150.sceRtc {
	@HLEFunction(nid = 0x203CEB0D, version = 200)
	public int sceRtcGetLastReincarnatedTime(TPointer64 tickAddr) {
        // Returns the last tick that was saved upon a battery shutdown.
        // Just return our current tick, since there's no need to mimick such behavior.
        tickAddr.setValue(hleGetCurrentTick());

        return 0;
	}

	@HLEFunction(nid = 0x62685E98, version = 200)
	public int sceRtcGetLastAdjustedTime(TPointer64 tickAddr) {
        // Returns the last time that was manually set by the user.
        // Just return our current tick, since there's no need to mimick such behavior.
        tickAddr.setValue(hleGetCurrentTick());

        return 0;
	}

	@HLEFunction(nid = 0x1909C99B, version = 200)
	public int sceRtcSetTime64_t(TPointer dateAddr, long time) {
        ScePspDateTime dateTime = ScePspDateTime.fromUnixTime(time);
        dateTime.write(dateAddr);

        return 0;
	}

	@HLEFunction(nid = 0xE1C93E47, version = 200)
	public int sceRtcGetTime64_t(ScePspDateTime dateTime, TPointer64 timeAddr) {
        Calendar cal = Calendar.getInstance();
        cal.set(dateTime.year, dateTime.month - 1, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second);
        long unixtime = cal.getTime().getTime() / 1000L;

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceRtcGetTime64_t psptime=%s returning unixtime=%d", dateTime, unixtime));
        }

        timeAddr.setValue(unixtime);

        return 0;
	}
}