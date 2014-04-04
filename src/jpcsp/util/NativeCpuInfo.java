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

import org.apache.log4j.Logger;

/**
 *
 * @author shadow
 */
public class NativeCpuInfo {
	private static Logger log = Logger.getLogger("cpuinfo");
	private static boolean isAvailable = false;

    static {
        try {
            System.loadLibrary("cpuinfo");
            isAvailable = true;
        } catch (UnsatisfiedLinkError ule) {
            log.error("Loading cpuinfo native library", ule);
        }
    }

    public static boolean isAvailable() {
    	return isAvailable;
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

    public static void printInfo() {
        log.info("Supports SSE    "+ hasSSE());
        log.info("Supports SSE2   "+ hasSSE2());
        log.info("Supports SSE3   "+ hasSSE3());
        log.info("Supports SSSE3  "+ hasSSSE3());
        log.info("Supports SSE4.1 "+ hasSSE41());
        log.info("Supports SSE4.2 "+ hasSSE42());
        log.info("Supports AVX    "+ hasAVX());
        log.info("Supports AVX2   "+ hasAVX2());
    }
}
