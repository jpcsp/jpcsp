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
package jpcsp.HLE.VFS.fat;

import static jpcsp.HLE.VFS.fat.Fat32VirtualFile.sectorSize;

import jpcsp.util.Utilities;

public class FatUtils {
	public static int getSectorNumber(long position) {
		return (int) (position / sectorSize);
	}

	public static int getSectorOffset(long position) {
		return (int) (position % sectorSize);
	}

	public static void storeSectorInt32(byte[] sector, int offset, int value) {
		Utilities.writeUnaligned32(sector, offset, value);
	}

	public static void storeSectorInt8(byte[] sector, int offset, int value) {
		sector[offset] = (byte) value;
	}

	public static void storeSectorInt16(byte[] sector, int offset, int value) {
		Utilities.writeUnaligned16(sector, offset, value);
	}

	public static int readSectorInt32(byte[] sector, int offset) {
		return Utilities.readUnaligned32(sector, offset);
	}

	public static int readSectorInt16(byte[] sector, int offset) {
		return Utilities.readUnaligned16(sector, offset);
	}

	public static int readSectorInt8(byte[] sector, int offset) {
		return Utilities.read8(sector, offset);
	}

	public static String readSectorString(byte[] sector, int offset, int length) {
		String s = "";
		// Skip any trailing spaces
		for (int i = length - 1; i >= 0; i--) {
			if (sector[offset + i] != (byte) ' ') {
				s = new String(sector, offset, i + 1);
				break;
			}
		}

		return s;
	}

	public static void storeSectorString(byte[] sector, int offset, String value, int length) {
		int stringLength = Math.min(value.length(), length);
		Utilities.writeStringNZ(sector, offset, stringLength, value);

		// Fill rest with spaces
		for (int i = stringLength; i < length; i++) {
			sector[offset + i] = (byte) ' ';
		}
	}

    public static FatFileInfo[] extendArray(FatFileInfo[] array, int extend) {
        if (array == null) {
            return new FatFileInfo[extend];
        }

        FatFileInfo[] newArray = new FatFileInfo[array.length + extend];
        System.arraycopy(array, 0, newArray, 0, array.length);

        return newArray;
    }
}
