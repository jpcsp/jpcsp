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
package jpcsp.hardware;

import static jpcsp.util.Utilities.GB;
import static jpcsp.util.Utilities.KB;
import static jpcsp.util.Utilities.MB;

import jpcsp.util.Utilities;

public class MemoryStick {
	// States for mscmhc0 (used in callbacks).
    public final static int PSP_MEMORYSTICK_STATE_DRIVER_READY     = 1;
    public final static int PSP_MEMORYSTICK_STATE_DRIVER_BUSY      = 2;
    public final static int PSP_MEMORYSTICK_STATE_DEVICE_INSERTED  = 4;
    public final static int PSP_MEMORYSTICK_STATE_DEVICE_REMOVED   = 8;
    // States for fatms0 (used in callbacks).
    public final static int PSP_FAT_MEMORYSTICK_STATE_UNASSIGNED   = 0;
    public final static int PSP_FAT_MEMORYSTICK_STATE_ASSIGNED     = 1;
    public final static int PSP_FAT_MEMORYSTICK_STATE_REMOVED      = 2;
    // MS and FatMS states.
    private static int msState = PSP_MEMORYSTICK_STATE_DRIVER_READY;
    private static int fatMsState = PSP_FAT_MEMORYSTICK_STATE_ASSIGNED;

    // Memory Stick power
    private static boolean msPower = true;

    private final static int sectorSize = 32 * KB;
    // Total size of the memory stick, in bytes
    private static long totalSize = 16L * GB;
    // Free size on memory stick, in bytes
    private static long freeSize = 1L * GB;

    private static boolean locked = false;

	public static int getStateMs() {
		return msState;
	}

	public static void setStateMs(int state) {
		MemoryStick.msState = state;
	}

    public static int getStateFatMs() {
		return fatMsState;
	}

	public static void setStateFatMs(int state) {
		MemoryStick.fatMsState = state;
	}

	public static boolean isInserted() {
		return fatMsState != PSP_FAT_MEMORYSTICK_STATE_REMOVED;
	}

	public static long getFreeSize() {
		return freeSize;
	}

	public static int getFreeSizeKb() {
		return Utilities.getSizeKb(getFreeSize());
	}

	public static int getSectorSize() {
		return sectorSize;
	}

	public static int getSectorSizeKb() {
		return Utilities.getSizeKb(getSectorSize());
	}

	public static int getSize32Kb(int sizeKb) {
		return (sizeKb + 31) & ~31;
	}

	private static long divideWithRounding(long value, long divide) {
		return (value + divide / 2) / divide;
	}

	public static String getSizeString(long size) {
		if (size < 3L * MB) {
			return String.format("%d KB", divideWithRounding(size, KB));
		}
		if (size < 3L * GB) {
			return String.format("%d MB", divideWithRounding(size, MB));
		}
		return String.format("%d GB", divideWithRounding(size, GB));
	}

	public static String getSizeKbString(int sizeKb) {
		return getSizeString(((long) sizeKb) * KB);
	}

	public static boolean isLocked() {
		return locked;
	}

	public static void setLocked(boolean locked) {
		MemoryStick.locked = locked;
	}

	public static boolean hasMsPower() {
		return msPower;
	}

	public static void setMsPower(boolean msPower) {
		MemoryStick.msPower = msPower;
	}

	public static long getTotalSize() {
		return totalSize;
	}

	public static void setTotalSize(long totalSize) {
		MemoryStick.totalSize = totalSize;
		if (freeSize > totalSize) {
			freeSize = totalSize;
		}
	}
}