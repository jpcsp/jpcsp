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
package libkirk;

import static java.lang.System.arraycopy;

import java.util.Arrays;

import org.apache.log4j.Logger;

public class Utilities {
	public static Logger log = Logger.getLogger("libkirk");

	public static int u8(int n) {
		return n & 0xFF;
	}

	public static int u8(byte b) {
		return b & 0xFF;
	}

	public static int u8(byte[] a, int offset) {
		return u8(a[offset]);
	}

	public static int read(byte[] buffer, int offset, byte[] result) {
		arraycopy(buffer, offset, result, 0, result.length);
		return offset + result.length;
	}

	public static int write(byte[] buffer, int offset, byte[] result) {
		arraycopy(result, 0, buffer, offset, result.length);
		return offset + result.length;
	}

	public static int read8(byte[] buffer, int offset) {
		return u8(buffer, offset);
	}

	public static int write8(byte[] buffer, int offset, int value) {
		buffer[offset] = (byte) value;
		return offset + 1;
	}

	public static int read16(byte[] buffer, int offset) {
		return u8(buffer, offset) | (u8(buffer, offset + 1) << 8);
	}

	public static int write16(byte[] buffer, int offset, int value) {
		offset = write8(buffer, offset, value);
		offset = write8(buffer, offset, value >> 8);
		return offset;
	}

	public static int read32(byte[] buffer, int offset) {
		return u8(buffer, offset) | (u8(buffer, offset + 1) << 8) | (u8(buffer, offset + 2) << 16) | (u8(buffer, offset + 3) << 24);
	}

	public static int write32(byte[] buffer, int offset, int value) {
		offset = write8(buffer, offset, value);
		offset = write8(buffer, offset, value >> 8);
		offset = write8(buffer, offset, value >> 16);
		offset = write8(buffer, offset, value >> 24);
		return offset;
	}

	public static long read64(byte[] buffer, int offset) {
		return read32(buffer, offset) & 0xFFFFFFFFL | (((long) read32(buffer, offset + 4)) << 32);
	}

	public static int write64(byte[] buffer, int offset, long value) {
		offset = write32(buffer, offset, (int) value);
		offset = write32(buffer, offset, (int) (value >> 32));
		return offset;
	}

	public static void memcpy(byte[] dst, byte[] src, int length) {
		arraycopy(src, 0, dst, 0, length);
	}

	public static void memcpy(byte[] dst, int dstOffset, byte[] src, int length) {
		arraycopy(src, 0, dst, dstOffset, length);
	}

	public static void memcpy(byte[] dst, byte[] src, int srcOffset, int length) {
		arraycopy(src, srcOffset, dst, 0, length);
	}

	public static void memcpy(byte[] dst, int dstOffset, byte[] src, int srcOffset, int length) {
		arraycopy(src, srcOffset, dst, dstOffset, length);
	}

	public static int memcmp(byte[] a, byte[] b, int length) {
		return memcmp(a, 0, b, 0, length);
	}

	public static int memcmp(byte[] a, int offset1, byte[] b, int offset2, int length) {
		for (int i = 0; i < length; i++) {
			byte aa = a[i + offset1];
			byte bb = b[i + offset2];
			if (aa != bb) {
				return aa - bb;
			}
		}

		return 0;
	}

	public static void memset(byte[] a, int value, int length) {
		memset(a, 0, value, length);
	}

	public static void memset(byte[] a, int offset, int value, int length) {
		Arrays.fill(a, offset, offset + length, (byte) value);
	}

	public static int curtime() {
		return (int) System.currentTimeMillis();
	}

    public static int alignUp(int value, int alignment) {
        return alignDown(value + alignment, alignment);
    }

    public static int alignDown(int value, int alignment) {
        return value & ~alignment;
    }

    public static byte[] intArrayToByteArray(int[] a) {
    	if (a == null) {
    		return null;
    	}

    	byte[] bytes = new byte[a.length];
    	for (int i = 0; i < bytes.length; i++) {
    		bytes[i] = (byte) a[i];
    	}

    	return bytes;
    }

    public static boolean isZero(byte[] a, int offset, int length) {
    	for (int i = 0; i < length; i++) {
    		if (a[offset + i] != (byte) 0) {
    			return false;
    		}
    	}

    	return true;
    }

    public static String toString(byte[] a) {
    	int length = a == null ? 0 : a.length;

    	return toString(a, 0, length);
    }

    public static String toString(byte[] a, int offset, int length) {
    	if (a == null) {
    		return null;
    	}

    	StringBuilder s = new StringBuilder();
    	s.append('{');
    	if (length > 0 && isZero(a, offset, length)) {
    		// If the array is containing only 0's, return a more compact display: "N x 0x00"
    		s.append(String.format("%d x 0x%02X", length, u8(a, offset)));
    	} else {
			for (int i = 0; i < length; i++) {
				if (i > 0) {
					s.append(", ");
				}
				s.append(String.format("0x%02X", u8(a, offset + i)));
			}
    	}
		s.append('}');

    	return s.toString();
    }
}
