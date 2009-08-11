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

import java.util.HashMap;

import jpcsp.HLE.kernel.types.SceKernelUid;

/**
 *
 * @author hli
 */
public class UidManager {

    private static HashMap<Integer, SceKernelUid> uidMap;
    private static int uidNext;

    public int addObject(SceKernelUid object) {
        while (uidMap.containsKey(uidNext)) {
            uidNext = (uidNext + 1) & 0x7FFFFFFF;
        }
        uidMap.put(uidNext, object);
        return uidNext++;
    }

    public boolean removeObject(SceKernelUid object) {
        int uid = object.getUid();
        if (uidMap.get(uid) == null) {
            return false;
        }
        uidMap.remove(uid);
        return true;
    }

    public void reset() {
        uidMap = new HashMap<Integer, SceKernelUid>();
        uidNext = 0x1000;
    }

    public static final UidManager singleton;

    private UidManager() {
    }

    static {
        singleton = new UidManager();
        singleton.reset();
    }

}
