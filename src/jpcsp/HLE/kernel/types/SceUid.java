/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.HLE.kernel.types;

/**
 *
 * @author hli
 */
@Deprecated
public class SceUid {

    private Object purpose;
    private int uid;

    public SceUid(Object purpose, int uid) {
        this.purpose = purpose;
        this.uid = uid;
    }

    public Object getPurpose() {
        return purpose;
    }

    public int getUid() {
        return uid;
    }
}
