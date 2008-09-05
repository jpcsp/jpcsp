/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.Allegrex;

/**
 *
 * @author hli
 */
public class FpuRegisters {

    public class Fcr31 {
        public int rm;
        public boolean c;
        public boolean fs;
        
        public Fcr31() {
            rm = 0;
            c = false;
            fs = false;
        }
    }
    
    public float[] fpr;

    public Fcr31 fcr31;
    
    public void set(int reg, float value) {
        fpr[reg] = value;
    }

    public float get(int reg) {
        return fpr[reg];
    }
    
    public FpuRegisters() {
        fpr = new float[32];
        fcr31 = new Fcr31();
    }
}
