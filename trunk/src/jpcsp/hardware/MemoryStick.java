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
    public final static int PSP_MEMORYSTICK_STATE_INSERTED  = 1;
    public final static int PSP_MEMORYSTICK_STATE_EJECTED   = 2;
    public final static int PSP_MEMORYSTICK_STATE_INSERTING = 4; // mscmhc0 0x02015804 only
    private static int state = PSP_MEMORYSTICK_STATE_INSERTED;

    // available size on memory stick, in bytes.
    private static long freeSize = 1 * 1024 * 1024 * 1024;	// 1GB
    private static int sectorSize = 32 * 1024; // 32KB

	public static int getState() {
		return state;
	}

	public static void setState(int state) {
		MemoryStick.state = state;
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
		if (sizeKb < 1024) {
			return String.format("%d KB", sizeKb);
		}
		sizeKb /= 1024;
		if (sizeKb < 1024) {
			return String.format("%d MB", sizeKb);
		}
		sizeKb /= 1024;
		return String.format("%d GB", sizeKb);
	}
}
