/* SceUID Manager
 * Function:
 * Allocates SceUIDs to other modules.
 *
 * Why:
 * So we can avoid duplicate SceUID and detect when SceUID for one
 * purpose is used for another, example: thread UID used in semaphore.
 */
package jpcsp.HLE;

import java.util.HashMap;
import jpcsp.GeneralJpcspException;

public class SceUIDMan {
    private static SceUIDMan instance;
    private static HashMap<Integer, SceUID> uids;
    private static int uidnext;

    public static SceUIDMan get_instance() {
        if (instance == null) {
            instance = new SceUIDMan();
        }
        return instance;
    }

    private SceUIDMan() {
        uids = new HashMap<Integer, SceUID>();
        uidnext = 1000;
    }

    /** classes should call getUid to get a new unique SceUID */
    public int getNewUid(Object purpose) {
        SceUID uid = new SceUID(purpose);
        uids.put(uid.getUid(), uid);
        return uid.getUid();
    }

    /** classes should call checkUidPurpose before using a SceUID */
    public void checkUidPurpose(int uid, Object purpose) throws GeneralJpcspException {
        SceUID found = uids.get(uid);

        if (found == null) {
            throw new GeneralJpcspException("Attempt to use unknown SceUID (purpose='" + purpose.toString() + "')");
        }

        if (!purpose.equals(found.getPurpose())) {
            throw new GeneralJpcspException("Attempt to use SceUID for different purpose (purpose='" + purpose.toString() + "',original='" + found.getPurpose().toString() + "')");
        }
    }

    /** classes should call releaseUid when they are finished with a SceUID */
    public void releaseUid(int uid, Object purpose) throws GeneralJpcspException {
        SceUID found = uids.get(uid);

        if (found == null) {
            throw new GeneralJpcspException("Attempt to release unknown SceUID (purpose='" + purpose.toString() + "')");
        }

        if (purpose.equals(found.getPurpose())) {
            uids.remove(found);
        } else {
            throw new GeneralJpcspException("Attempt to release SceUID for different purpose (purpose='" + purpose.toString() + "',original='" + found.getPurpose().toString() + "')");
        }
    }

    private class SceUID {
        private Object purpose;
        private int uid;

        public SceUID(Object purpose) {
            this.purpose = purpose;
            uid = uidnext++;
        }

        public Object getPurpose() {
            return purpose;
        }

        public int getUid() {
            return uid;
        }
    }
}
