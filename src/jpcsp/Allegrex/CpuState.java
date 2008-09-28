/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.Allegrex;

/**
 * Central Point Unit, handles all operations
 *
 * @author hli
 */

public class CpuState extends VfpuState {

    @Override
    public void reset() {
        resetAll();
    }

    public CpuState() {
    }

    public void copy(CpuState that) {
        super.copy(that);
    }
    
    public CpuState(CpuState that) {
        copy(that);
    }
}
