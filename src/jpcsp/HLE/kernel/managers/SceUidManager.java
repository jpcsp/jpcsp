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
package jpcsp.HLE.kernel.managers;

import jpcsp.Emulator;
import java.util.HashMap;

import jpcsp.HLE.kernel.types.SceUid;

/**
 *
 * @author hli
 */
public class SceUidManager {

    private static HashMap<Integer, SceUid> uidMap;
    private static int uidNext;

    static {
        uidMap = new HashMap<Integer, SceUid>();
        uidNext = 0x1000;
    }

    /** classes should call getUid to get a new unique SceUID */
    static public int getNewUid(Object purpose) {
        SceUid uid = new SceUid(purpose, uidNext++);
        uidMap.put(uid.getUid(), uid);
        return uid.getUid();
    }

    /** classes should call checkUidPurpose before using a SceUID
     * @return true is the uid is ok. */
    static public boolean checkUidPurpose(int uid, Object purpose, boolean allowUnknown) {
        SceUid found = uidMap.get(uid);

        if (found == null) {
            if (!allowUnknown) {
                Emulator.log.warn("Attempt to use unknown SceUID (purpose='" + purpose.toString() + "')");
                return false;
            }
        } else if (!purpose.equals(found.getPurpose())) {
            Emulator.log.error("Attempt to use SceUID for different purpose (purpose='" + purpose.toString() + "',original='" + found.getPurpose().toString() + "')");
            return false;
        }

        return true;
    }

    /** classes should call releaseUid when they are finished with a SceUID
     * @return true on success. */
    static public boolean releaseUid(int uid, Object purpose) {
        SceUid found = uidMap.get(uid);

        if (found == null) {
            Emulator.log.warn("Attempt to release unknown SceUID (purpose='" + purpose.toString() + "')");
            return false;
        }

        if (purpose.equals(found.getPurpose())) {
            uidMap.remove(found);
        } else {
            Emulator.log.error("Attempt to release SceUID for different purpose (purpose='" + purpose.toString() + "',original='" + found.getPurpose().toString() + "')");
            return false;
        }

        return true;
    }
}
