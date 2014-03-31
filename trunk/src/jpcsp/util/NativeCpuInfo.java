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
package jpcsp.util;

/**
 *
 * @author shadow
 */
public class NativeCpuInfo {

    static {
        try {
            System.loadLibrary("cpuinfo");
        } catch (UnsatisfiedLinkError ule) {
            System.out.println(ule);
        }
    }
    public static native void init();
    
    public static native boolean hasSSE();

    public static native boolean hasSSE2();

    public static native boolean hasSSE3();
    
    public static native boolean hasSSSE3();
    
    public static native boolean hasSSE41();
    
    public static native boolean hasSSE42();
    
    public static native boolean hasAVX();
    
    public static native boolean hasAVX2();
    
    public static void printInfo()
    {
        System.out.println("Supports SSE    "+ hasSSE());
        System.out.println("Supports SSE2   "+ hasSSE2());
        System.out.println("Supports SSE3   "+ hasSSE3());
        System.out.println("Supports SSSE3  "+ hasSSSE3());
        System.out.println("Supports SSE4.1 "+ hasSSE41());
        System.out.println("Supports SSE4.2 "+ hasSSE42());
        System.out.println("Supports AVX    "+ hasAVX());
        System.out.println("Supports AVX2   "+ hasAVX2());
    }
    
}
