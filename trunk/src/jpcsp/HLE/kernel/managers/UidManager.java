/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.HLE.kernel.managers;

import jpcsp.Emulator;
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
    
    /** classes should call releaseUid when they are finished with a SceUID
     * @return true on success. */
    public boolean releaseUid(int uid, Object purpose) {
        SceKernelUid found = uidMap.get(uid);

        if (found == null) {
            Emulator.log.warn("Attempt to release unknown SceUID (purpose='" + purpose.toString() + "')");
            return false;
        }

        uidMap.remove(found);

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
