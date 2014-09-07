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
package jpcsp.media.codec.util;

public class BitBuffer implements IBitReader {
	// Store bits as ints for faster reading
	private final int[] bits;
	private int readIndex;
	private int writeIndex;
	private int readCount;
	private int writeCount;

	public BitBuffer(int length) {
		bits = new int[length];
	}

	@Override
	public int read1() {
		readCount++;
		int bit = bits[readIndex];
		readIndex++;
		if (readIndex >= bits.length) {
			readIndex = 0;
		}

		return bit;
	}

	@Override
	public int read(int n) {
		int value = 0;
		for (; n > 0; n--) {
			value = (value << 1) + read1();
		}

		return value;
	}

	public int getBitsRead() {
		return readCount;
	}

	public int getBytesRead() {
		return getBitsRead() >>> 3;
	}

	public int getBitsWritten() {
		return writeCount;
	}

	public int getBytesWritten() {
		return getBitsWritten() >>> 3;
	}

	@Override
	public void skip(int n) {
		readCount += n;
		readIndex += n;
		while (readIndex < 0) {
			readIndex += bits.length;
		}
		while (readIndex >= bits.length) {
			readIndex -= bits.length;
		}
	}

	private void writeBit(int n) {
		bits[writeIndex] = n;
		writeIndex++;
		writeCount++;
		if (writeIndex >= bits.length) {
			writeIndex = 0;
		}
	}

	public void writeByte(int n) {
		for (int bit = 7; bit >= 0; bit--) {
			writeBit((n >> bit) & 0x1);
		}
	}

	@Override
	public boolean readBool() {
		return read1() != 0;
	}

	@Override
	public int peek(int n) {
		int read = read(n);
		skip(-n);
		return read;
	}

	@Override
	public String toString() {
		return String.format("BitBuffer readIndex=%d, writeIndex=%d, readCount=%d", readIndex, writeIndex, readCount);
	}
}
