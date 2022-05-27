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

/*
 * Parameter Structure for sceUsbMicInputInitEx().
 */
public class pspUsbMicInputInitExParam extends pspAbstractMemoryMappedStructure {
	public int unknown1;
	public int unknown2;
	public int unknown3;
	public int unknown4;
	public int unknown5;
	public int unknown6;

	@Override
	protected void read() {
		unknown1 = read16();
		readUnknown(2);
		unknown2 = read16();
		readUnknown(2);
		unknown3 = read16();
		readUnknown(2);
		unknown4 = read16();
		readUnknown(2);
		unknown5 = read16();
		readUnknown(2);
		unknown6 = read16();
		readUnknown(2);
	}

	@Override
	protected void write() {
		write16((short) unknown1);
		write16((short) 0);
		write16((short) unknown2);
		write16((short) 0);
		write16((short) unknown3);
		write16((short) 0);
		write16((short) unknown4);
		write16((short) 0);
		write16((short) unknown5);
		write16((short) 0);
		write16((short) unknown6);
		write16((short) 0);
	}

	@Override
	public int sizeof() {
		return 24;
	}

	@Override
	public String toString() {
		return String.format("unknown1=0x%X, unknown2=0x%X, unknown3=0x%X, unknown4=0x%X, unknown5=0x%X, unknown6=0x%X", unknown1, unknown2, unknown3, unknown4, unknown5, unknown6);
	}
}
