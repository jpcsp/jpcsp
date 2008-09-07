/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.Allegrex;

/**
 *
 * @author hli
 */
public class CpuState {

    public int[] gpr;
    public long hilo;

    public void set(int reg, int value) {
        gpr[reg] = value;
    }

    public int get(int reg) {
        return gpr[reg];
    }

    public void setHi(int value) {
        hilo = (hilo & 0xffffffffL) | ((long) value << 32);
    }

    public int getHi() {
        return (int) (hilo >>> 32);
    }

    public void setLo(int value) {
        hilo = (hilo & ~0xffffffffL) | (((long) value) & 0xffffffffL);
    }

    public int getLo() {
        return (int) (hilo & 0xffffffffL);
    }

    public void reset() {
        gpr = new int[32];
        hilo = 0;
    }

    public CpuState() {
        reset();
    }

    public CpuState(CpuState that) {
        gpr = new int[32];
        for (int reg = 0; reg < 32; ++reg) {
            gpr[reg] = that.gpr[reg];
        }
        hilo = that.hilo;
    }
    
    public static final long signedDivMod(int x, int y) {
        return ((long) (x % y)) << 32 | (((long) (x / y)) & 0xffffffff);
    }

    public static final long unsignedDivMod(long x, long y) {
        return ((x % y)) << 32 | ((x / y) & 0xffffffff);
    }

    public static final int max(int x, int y) {
        return (x > y) ? x : y;
    }

    public static final int min(int x, int y) {
        return (x < y) ? x : y;
    }

    public static final int extractBits(int x, int pos, int len) {
        return (x >>> pos) & ~(~0 << len);
    }

    public static final int insertBits(int x, int y, int lsb, int msb) {
        int mask = ~(~0 << (msb - lsb + 1)) << lsb;
        return (x & ~mask) | ((y << lsb) & mask);
    }

    public static final int signExtend(int value) {
        return (value << 16) >> 16;
    }

    public static final int signExtend8(int value) {
        return (value << 24) >> 24;
    }

    public static final int zeroExtend(int value) {
        return (value & 0xffff);
    }

    public static final int zeroExtend8(int value) {
        return (value & 0xff);
    }

    public static final int signedCompare(int i, int j) {
        return (i - j) >>> 31;
    }

    public static final int unsignedCompare(int i, int j) {
        return ((i - j) ^ i ^ j) >>> 31;
    }

    public static final int branchTarget(int npc, int simm16) {
        return npc + (simm16 << 2);
    }

    public static final int jumpTarget(int npc, int uimm26) {
        return (npc & 0xf0000000) | (uimm26 << 2);
    }

}
