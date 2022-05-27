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

import static jpcsp.state.StateOutputStream.NULL_ARRAY_LENGTH;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.InetAddress;

import jpcsp.Emulator;

public class StateInputStream extends ObjectInputStream {
	public StateInputStream(InputStream in) throws IOException {
		super(in);
	}

	public int readVersion(int maxVersion) throws IOException {
		int version = readInt();
		if (version > maxVersion) {
			throw new InvalidStateException(String.format("Unsupported State version %d(maxVersion=%d)", version, maxVersion));
		}

		return version;
	}

	public void readInts(int[] a) throws IOException {
		readInts(a, 0, a.length);
	}

	public int[] readIntsWithLength() throws IOException {
		int length = readInt();
		if (length == NULL_ARRAY_LENGTH) {
			return null;
		}
		int a[] = new int[length];
		readInts(a);

		return a;
	}

	public void readInts(int[] a, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			a[i + offset] = readInt();
		}
	}

	public void readFloats(float[] a) throws IOException {
		for (int i = 0; i < a.length; i++) {
			a[i] = readFloat();
		}
	}

	public void readBooleans(boolean[] a) throws IOException {
		for (int i = 0; i < a.length; i++) {
			a[i] = readBoolean();
		}
	}

	public void readBytes(byte[] a) throws IOException {
		readBytes(a, 0, a.length);
	}

	public void readBytes(byte[] a, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			a[i + offset] = readByte();
		}
	}

	public byte[] readBytesWithLength() throws IOException {
		int length = readInt();
		if (length == NULL_ARRAY_LENGTH) {
			return null;
		}
		byte a[] = new byte[length];
		readBytes(a);

		return a;
	}

	public String readString() throws IOException {
		try {
			Object a = readObject();
			if (a == null) {
				return null;
			}
			return a.toString();
		} catch (ClassNotFoundException e) {
			Emulator.log.error("readString", e);
			return null;
		}
	}

	public InetAddress readInetAddress() throws IOException {
		int inetAddressLength = readInt();
		if (inetAddressLength < 0) {
			return null;
		}
		byte[] inetAddressBytes = new byte[inetAddressLength];
		read(inetAddressBytes);
		return InetAddress.getByAddress(inetAddressBytes);
	}
}
