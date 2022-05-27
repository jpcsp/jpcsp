package jpcsp.util;

public class MathUtil {
    static public int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
