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
package jpcsp.HLE.kernel.types;

import jpcsp.HLE.TPointer;
import jpcsp.util.Utilities;

public class SceResidentLibraryEntryTable extends pspAbstractMemoryMappedStructure {
	public TPointer libNameAddr;
	public String libName;
	public final byte[] version = new byte[2];
	public int attribute;
	public int len;
	public int vStubCount;
	public int stubCount;
	public TPointer entryTable;
	public int vStubCountNew;
	public int unknown18;
	public int unknown19;

	@Override
	protected void read() {
		libNameAddr = readPointer();
		if (libNameAddr != null && libNameAddr.isNotNull()) {
			libName = Utilities.readStringZ(libNameAddr.getAddress());
		}
		read8Array(version);
		attribute = read16();
		len = read8();
		vStubCount = read8();
		stubCount = read16();
		entryTable = readPointer();
		if (len > 4) {
			vStubCountNew = read16();
			unknown18 = read8();
			unknown19 = read8();
		}
	}

	@Override
	protected void write() {
		writePointer(libNameAddr);
		write8Array(version);
		write16((short) attribute);
		write8((byte) len);
		write8((byte) vStubCount);
		write16((short) stubCount);
		writePointer(entryTable);
		if (len > 4) {
			write16((short) vStubCountNew);
			write8((byte) unknown18);
			write8((byte) unknown19);
		}
	}

	@Override
	public int sizeof() {
		return len << 2;
	}
}
