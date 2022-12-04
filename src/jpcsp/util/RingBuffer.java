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

import java.io.IOException;

import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * FIFO Ring Buffer implementation
 *
 * @author gid15
 *
 */
public class RingBuffer implements IState {
	private static final int STATE_VERSION = 0;
	private int[] buffer;
	private int count;
	private int readIndex;
	private int writeIndex;

	public RingBuffer(int size) {
		buffer = new int[size];
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		buffer = stream.readIntsWithLength();
		count = stream.readInt();
		readIndex = stream.readInt();
		writeIndex = stream.readInt();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeIntsWithLength(buffer);
		stream.writeInt(count);
		stream.writeInt(readIndex);
		stream.writeInt(writeIndex);
	}

	private int increment(int index, int value) {
		index += value;
		while (index >= buffer.length) {
			index -= buffer.length;
		}
		while (index < 0) {
			index += buffer.length;
		}

		return index;
	}

	private int increment(int index) {
		index++;
		if (index >= buffer.length) {
			return 0;
		}

		return index;
	}

	public void write(int value) {
		if (isFull()) {
			return;
		}

		buffer[writeIndex] = value;
		writeIndex = increment(writeIndex);
		count++;
	}

	public int read() {
		if (isEmpty()) {
			return 0;
		}

		int value = buffer[readIndex];
		readIndex = increment(readIndex);
		count--;

		return value;
	}

	public int peek() {
		if (isEmpty()) {
			return 0;
		}

		return buffer[readIndex];
	}

	public int peek(int index) {
		if (index < 0 || index >= count) {
			return 0;
		}

		int value = buffer[increment(readIndex, index)];

		return value;
	}

	public int peek(int index, int[] data, int offset, int length) {
		if (index + length > count) {
			length = count - index;
		}
		if (length <= 0) {
			return 0;
		}

		if (readIndex + index + length <= buffer.length) {
			System.arraycopy(buffer, readIndex + index, data, offset, length);
		} else {
			int size1 = buffer.length - (readIndex + index);
			int size2 = length - size1;
			System.arraycopy(buffer, readIndex + index, data, offset, size1);
			System.arraycopy(buffer, 0, data, offset + size1, size2);
		}

		return length;
	}

	public int readAll(int[] data, int offset) {
		int readSize = peek(0, data, offset, count);
		clear();

		return readSize;
	}

	public int[] readAll() {
		int[] data = new int[size()];
		readAll(data, 0);

		return data;
	}

	public int size() {
		return count;
	}

	public boolean isEmpty() {
		return count <= 0;
	}

	public boolean isFull() {
		return count >= buffer.length;
	}

	public void clear() {
		count = 0;
		readIndex = 0;
		writeIndex = 0;
	}

	public String toString(int bits, int offset, int length) {
		String format;
		switch (bits) {
			case  8: format = "0x%02X"; break;
			case 16: format = "0x%04X"; break;
			case 32: format = "0x%08X"; break;
			default: format = "0x%X";   break;
		}

		StringBuilder s = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(String.format(format, peek(offset + i)));
		}

		return s.toString();
	}

	public String toString(int bits) {
		return toString(bits, 0, count);
	}

	@Override
	public String toString() {
		return String.format("count=%d", count);
	}
}
