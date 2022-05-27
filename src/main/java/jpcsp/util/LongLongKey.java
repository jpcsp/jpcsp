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
 * @author gid15
 *
 * A 128-bit key value that can be used as a hash key.
 */
public class LongLongKey {
	private long key1;
	private long key2;
	private int shift;

	public LongLongKey() {
	}

	public LongLongKey(LongLongKey key) {
		key1 = key.key1;
		key2 = key.key2;
		shift = key.shift;
	}

	public void reset() {
		key1 = 0;
		key2 = 0;
		shift = 0;
	}

	/**
	 * Add an integer value to the current key.
	 * 
	 * @param value  the integer value
	 * @param bits   the number of bits of the integer value to be considered.
	 */
	public void addKeyComponent(int value, int bits) {
		if (shift < Long.SIZE) {
			if (shift + bits > Long.SIZE) {
				shift = Long.SIZE;
				key2 = value;
			} else {
				key1 += ((long) value) << shift;
			}
		} else {
			key2 += ((long) value) << (shift - Long.SIZE);
		}
		shift += bits;
	}

	/**
	 * Add a boolean value to the current key.
	 * 
	 * @param value  the boolean value
	 */
	public void addKeyComponent(boolean value) {
		if (shift < Long.SIZE) {
			key1 += (value ? 1L : 0L) << shift;
		} else {
			key2 += (value ? 1L : 0L) << (shift - Long.SIZE);
		}
		shift++;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 * 
	 * Required by the Hashtable implementation.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LongLongKey) {
			LongLongKey longLongKey = (LongLongKey) obj;
			return key1 == longLongKey.key1 && key2 == longLongKey.key2;
		}
		return super.equals(obj);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 *
	 * Required by the Hashtable implementation.
	 */
	@Override
	public int hashCode() {
		// Mix both key1 and key2
		return Long.valueOf(key1).hashCode() ^ Long.valueOf(key2).hashCode();
	}

	@Override
	public String toString() {
		return String.format("LongLongKey(key1=0x%X, key2=0x%X)", key1, key2);
	}
}
