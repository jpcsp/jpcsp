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

import static jpcsp.Emulator.exitCalled;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.HLE.kernel.types.SceUid;

/**
 *
 * @author hli, gid15
 */
public class SceUidManager {
	public static Logger log = Emulator.log;
	// UID is a unique identifier across all purposes
    private static HashMap<Integer, SceUid> uidMap = new HashMap<>();
    private static int uidNext = 0x1; // LocoRoco expects UID to be 8bit
    public static final int INVALID_ID = Integer.MIN_VALUE;

    // ID is an identifier only unique for the same purpose.
    // Different purposes can share the save ID values.
    // An ID has always a range of valid values, e.g. [0..255]
    private static HashMap<String, LinkedList<Integer>> freeIdsMap = new HashMap<>();

    static public void reset() {
    	uidMap.clear();
    	freeIdsMap.clear();
    	uidNext = 1;
    }

    /** classes should call getUid to get a new unique SceUID */
    static public int getNewUid(String purpose) {
        SceUid uid = new SceUid(purpose, uidNext++);
        uidMap.put(uid.getUid(), uid);
        return uid.getUid();
    }

    static public boolean hasUidPurpose(int uid, String purpose) {
        SceUid found = uidMap.get(uid);
        if (found == null) {
        	return false;
        }

        return purpose.equals(found.getPurpose());
    }

    /** classes should call checkUidPurpose before using a SceUID
     * @return true is the uid is ok. */
    static public boolean checkUidPurpose(int uid, String purpose, boolean allowUnknown) {
        SceUid found = uidMap.get(uid);

        if (found == null) {
            if (!allowUnknown) {
            	if (!exitCalled()) {
            		log.warn(String.format("Attempt to use unknown SceUID=0x%X (purpose='%s')", uid, purpose));
            	}
                return false;
            }
        } else if (!purpose.equals(found.getPurpose())) {
        	if (!exitCalled()) {
        		log.error(String.format("Attempt to use SceUID=0x%X for different purpose (purpose='%s', original='%s')", uid, purpose, found.getPurpose()));
        	}
            return false;
        }

        return true;
    }

    /** classes should call releaseUid when they are finished with a SceUID
     * @return true on success. */
    static public boolean releaseUid(int uid, String purpose) {
        SceUid found = uidMap.get(uid);

        if (found == null) {
        	if (!exitCalled()) {
        		log.warn("Attempt to release unknown SceUID (purpose='" + purpose.toString() + "')");
        	}
            return false;
        }

        if (purpose.equals(found.getPurpose())) {
            uidMap.remove(uid);
        } else {
        	if (!exitCalled()) {
        		log.error("Attempt to release SceUID for different purpose (purpose='" + purpose.toString() + "',original='" + found.getPurpose().toString() + "')");
        	}
            return false;
        }

        return true;
    }

    static public boolean isValidUid(int uid) {
    	return uidMap.containsKey(uid);
    }

    /**
     * Return a new ID for the given purpose.
     * The ID will be unique for the given purpose but will not be unique
     * across different purposes.
     * The ID will be higher of equal to minimumId, and lower or equal to
     * maximumId, i.e. in the range [minimumId..maximumId].
     * The ID will be lowest possible free ID.
     * 
     * @param purpose    The ID will be unique for this purpose
     * @param minimumId  The lowest possible value for the ID
     * @param maximumId  The highest possible value for the ID
     * @return           The lowest possible free ID for the given purpose
     */
    static public int getNewId(String purpose, int minimumId, int maximumId) {
    	LinkedList<Integer> freeIds = freeIdsMap.get(purpose);
    	if (freeIds == null) {
    		freeIds = new LinkedList<Integer>();
    		for (int id = minimumId; id <= maximumId; id++) {
    			freeIds.add(id);
    		}
    		freeIdsMap.put(purpose, freeIds);
    	}

    	// No more free IDs?
    	if (freeIds.size() <= 0) {
    		// Return an invalid ID
    		return INVALID_ID;
    	}

    	// Return the lowest free ID
    	return freeIds.remove();
    }

    static public void resetIds(String purpose) {
    	freeIdsMap.remove(purpose);
    }

    /**
     * Release an ID for a given purpose. The ID had to be created first
     * by getNewId().
     * After release, the ID is marked as being free and can be returned
     * again by getNewId().
     *
     * @param id       The ID to be released
     * @param purpose  The ID will be releases for this purpose.
     * @return         true if the ID was successfully released
     *                 false if the ID could not be released
     *                       (because the purpose was not exiting or
     *                        the ID was already released)
     */
    static public boolean releaseId(int id, String purpose) {
    	LinkedList<Integer> freeIds = freeIdsMap.get(purpose);

    	if (freeIds == null) {
        	if (!exitCalled()) {
        		log.warn(String.format("Attempt to release ID=%d with unknown purpose='%s'", id, purpose));
        	}
    		return false;
    	}

    	// Add the id back to the freeIds list,
    	// and keep the id's ordered (lowest first).
    	for (ListIterator<Integer> lit = freeIds.listIterator(); lit.hasNext(); ) {
    		int currentId = lit.next();
    		if (currentId == id) {
            	if (!exitCalled()) {
            		log.warn(String.format("Attempt to release free ID=%d with purpose='%s'", id, purpose));
            	}
        		return false;
    		}
    		if (currentId > id) {
    			// Insert the id before the currentId
    			lit.set(id);
    			lit.add(currentId);
    			return true;
    		}
    	}

    	freeIds.add(id);

    	return true;
    }
}
