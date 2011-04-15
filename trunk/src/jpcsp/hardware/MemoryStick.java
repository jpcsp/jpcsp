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
    // MS and FatMS states.
    private static int msState = PSP_MEMORYSTICK_STATE_DRIVER_READY;
    private static int fatMsState = PSP_FAT_MEMORYSTICK_STATE_ASSIGNED;

    // available size on memory stick, in bytes.
    private static long freeSize = 1L * 1024 * 1024 * 1024;	// 1GB
    private static int sectorSize = 32 * 1024; // 32KB

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

	public static long getFreeSize() {
		return freeSize;
	}

	public static int getFreeSizeKb() {
		return Utilities.getSizeKb(getFreeSize());
	}

	public static void setFreeSize(long freeSize) {
		MemoryStick.freeSize = freeSize;
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

	public static String getSizeKbString(int sizeKb) {
		if (sizeKb < 3 * 1024) {
			return String.format("%d KB", sizeKb);
		}
		sizeKb /= 1024;
		if (sizeKb < 3 * 1024) {
			return String.format("%d MB", sizeKb);
		}
		sizeKb /= 1024;
		return String.format("%d GB", sizeKb);
	}
}