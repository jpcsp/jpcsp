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
package jpcsp.media;

public class MpegStream {
	private byte[] buffer;
	private int index;
	private int length;

	public MpegStream(byte[] buffer, int offset, int length) {
		this.buffer = buffer;
		this.index = offset;
		this.length = offset + length;
	}

	public int read8() {
		int b = buffer[index] & 0xFF;

		// Check for escape sequence: 00 00 03
		if (b == 0) {
			if (index >= 1 && buffer[index - 1] == 0) {
				if (index + 1 < length && buffer[index + 1] == 3) {
					// Skip escape byte
					index++;
				}
			}
		}
		index++;

		return b;
	}

	public int read16() {
		return (read8() << 8) | read8();
	}

	public int read32() {
		return (read8() << 24) | (read8() << 16) | (read8() << 8) | read8();
	}

	public int read(int n) {
		int value = 0;
		for (int i = 0; i < n; i++) {
			value = (value << 8) | read8();
		}

		return value;
	}

	public void skip(int n) {
		for (; n > 0; n--) {
			read8();
		}
	}

	public boolean isEmpty() {
		return index >= length;
	}
}
