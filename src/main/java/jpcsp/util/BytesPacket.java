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

import java.io.EOFException;

public class BytesPacket {
	private byte[] buffer;
	private int offset;
	private int length;
	private boolean littleEndian;
	private int bufferBits;
	private int bit;

	public BytesPacket(int length) {
		buffer = new byte[length];
		this.length = length;
	}

	public BytesPacket(byte[] buffer) {
		this.buffer = buffer;
		length = buffer.length;
	}

	public BytesPacket(byte[] buffer, int length) {
		this.buffer = buffer;
		this.length = length;
	}

	public BytesPacket(byte[] buffer, int offset, int length) {
		this.buffer = buffer;
		this.offset = offset;
		this.length = length;
	}

	public void setLittleEndian() {
		littleEndian = true;
	}

	public void setBigEndian() {
		littleEndian = false;
	}

	public byte[] getBuffer() {
		return buffer;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

	public boolean isEmpty() {
		return length <= 0 && bit <= 0;
	}

	public byte readByte() throws EOFException {
		if (length <= 0) {
			throw new EOFException();
		}

		length--;
		return buffer[offset++];
	}

	public int read8() throws EOFException {
		return readByte() & 0xFF;
	}

	public int read16() throws EOFException {
		if (littleEndian) {
			return read8() | (read8() << 8);
		}

		return (read8() << 8) | read8();
	}

	public int read32() throws EOFException {
		if (littleEndian) {
			return read8() | (read8() << 8) | (read8() << 16) | (read8() << 24);
		}

		return (read8() << 24) | (read8() << 16) | (read8() << 8) | read8();
	}

	public int read1() throws EOFException {
		if (bit <= 0) {
			bufferBits = read8();
			bit = 8;
		}

		bit--;
		return (bufferBits >> bit) & 1;
	}

	public int readBits(int n) throws EOFException {
		if (n <= bit) {
			bit -= n;
			return (bufferBits >> bit) & ((1 << n) - 1);
		}

		int value = 0;
		for (int i = 0; i < n; i++) {
			value = (value << 1) | read1();
		}

		return value;
	}

	public boolean readBoolean() throws EOFException {
		return read1() != 0;
	}

	public byte[] readBytes(byte[] dataBuffer) throws EOFException {
		return readBytes(dataBuffer, 0, dataBuffer.length);
	}

	public byte[] readBytes(int dataLength) throws EOFException {
		return readBytes(new byte[dataLength], 0, dataLength);
	}

	public byte[] readBytes(byte[] dataBuffer, int dataOffset, int dataLength) throws EOFException {
		for (int i = 0; i < dataLength; i++) {
			dataBuffer[dataOffset + i] = readByte();
		}

		return dataBuffer;
	}

	public char readAsciiChar() throws EOFException {
		return (char) read8();
	}

	public void skip8() throws EOFException {
		skip8(1);
	}

	public void skip8(int n) throws EOFException {
		if (n > 0) {
			if (length < n) {
				offset += length;
				length = 0;
				throw new EOFException();
			}

			offset += n;
			length -= n;
		}
	}

	public String readStringNZ(int n) throws EOFException {
		StringBuilder s = new StringBuilder();

		while (n > 0) {
			n--;
			int c = read8();
			if (c == 0) {
				break;
			}
			s.append((char) c);
		}
		skip8(n);

		return s.toString();
	}

	public void writeByte(byte b) throws EOFException {
		if (length <= 0) {
			throw new EOFException();
		}

		length--;
		buffer[offset++] = b;
	}

	public void writeBytesZero(int n) throws EOFException {
		for (; n > 0; n--) {
			writeByte((byte) 0);
		}
	}

	public void write8(int n) throws EOFException {
		writeByte((byte) (n & 0xFF));
	}

	public void write16(int n) throws EOFException {
		if (littleEndian) {
			write8(n);
			write8(n >> 8);
		} else {
			write8(n >> 8);
			write8(n);
		}
	}

	public void write32(int n) throws EOFException {
		if (littleEndian) {
			write8(n);
			write8(n >> 8);
			write8(n >> 16);
			write8(n >> 24);
		} else {
			write8(n >> 24);
			write8(n >> 16);
			write8(n >> 8);
			write8(n);
		}
	}

	public void writeBytes(byte[] dataBuffer) throws EOFException {
		if (dataBuffer != null) {
			writeBytes(dataBuffer, 0, dataBuffer.length);
		}
	}

	public void writeBytes(byte[] dataBuffer, int dataOffset, int dataLength) throws EOFException {
		if (dataBuffer != null) {
			int copyLength = Math.min(dataLength, dataBuffer.length - dataOffset);
			for (int i = 0; i < copyLength; i++) {
				writeByte(dataBuffer[dataOffset + i]);
			}
			dataLength -= copyLength;
		}

		writeBytesZero(dataLength);
	}

	public void write1(int n) throws EOFException {
		if (bit == 0) {
			bufferBits = 0;
		}
		bit++;
		bufferBits |= (n & 1) << (8 - bit);

		if (bit == 8) {
			write8(bufferBits);
			bit = 0;
			bufferBits = 0;
		}
	}

	public void writeBoolean(boolean b) throws EOFException {
		write1(b ? 1 : 0);
	}

	public void writeBits(int b, int n) throws EOFException {
		for (int i = n - 1; i >= 0; i--) {
			write1(b >> i);
		}
	}

	public void writeAsciiChar(char c) throws EOFException {
		writeByte((byte) c);
	}

	public void rewind(int newOffset) {
		if (newOffset < offset) {
			length += offset - newOffset;
			offset = newOffset;
		}
	}

	public void writeString(String s) throws EOFException {
		if (s != null) {
			int length = s.length();
			for (int i = 0; i < length; i++) {
				writeAsciiChar(s.charAt(i));
			}
		}
	}

	public void writeStringNZ(String s, int n) throws EOFException {
		if (s != null) {
			int copyLength = Math.min(s.length(), n);
			for (int i = 0; i < copyLength; i++) {
				writeAsciiChar(s.charAt(i));
			}
			n -= copyLength;
		}

		writeBytesZero(n);
	}
}
