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
package jpcsp.state;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;

public class StateOutputStream extends ObjectOutputStream {
	public static final int NULL_ARRAY_LENGTH = -1;

	public StateOutputStream(OutputStream out) throws IOException {
		super(out);
	}

	public void writeVersion(int version) throws IOException {
		writeInt(version);
	}

	public void writeInts(int[] a) throws IOException {
		writeInts(a, 0, a.length);
	}

	public void writeIntsWithLength(int[] a) throws IOException {
		if (a == null) {
			writeInt(NULL_ARRAY_LENGTH);
		} else {
			writeInt(a.length);
			writeInts(a, 0, a.length);
		}
	}

	public void writeInts(int[] a, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			writeInt(a[i + offset]);
		}
	}

	public void writeFloats(float[] a) throws IOException {
		for (int i = 0; i < a.length; i++) {
			writeFloat(a[i]);
		}
	}

	public void writeBooleans(boolean[] a) throws IOException {
		for (int i = 0; i < a.length; i++) {
			writeBoolean(a[i]);
		}
	}

	public void writeBytes(byte[] a) throws IOException {
		writeBytes(a, 0, a.length);
	}

	public void writeBytesWithLength(byte[] a) throws IOException {
		if (a == null) {
			writeInt(NULL_ARRAY_LENGTH);
		} else {
			writeInt(a.length);
			writeBytes(a);
		}
	}

	public void writeBytes(byte[] a, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			writeByte(a[i + offset]);
		}
	}

	public void writeString(String s) throws IOException {
		writeObject(s);
	}

	public void write(InetAddress inetAddress) throws IOException {
		if (inetAddress == null) {
			writeInt(-1);
		} else {
			byte[] inetAddressBytes = inetAddress.getAddress();
			writeInt(inetAddressBytes.length);
			write(inetAddressBytes);
		}
	}
}
