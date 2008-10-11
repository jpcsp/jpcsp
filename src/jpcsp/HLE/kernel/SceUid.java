/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.HLE.kernel;

/**
 *
 * @author hli
 */
public class SceUid {

    private int uid;
    private static int currentUid = 100;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }
    
    public int nextUid() {
        return currentUid++;
    }    
}
