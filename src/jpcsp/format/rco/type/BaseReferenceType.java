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
package jpcsp.format.rco.type;

public class BaseReferenceType extends BaseType {
	public int unknownShort0;
	public int unknownShort1;

	@Override
	public int size() {
		return 8;
	}

	@Override
	public int read(byte[] buffer, int offset) {
		unknownShort0 = read16(buffer, offset);
		offset += 2;
		unknownShort1 = read16(buffer, offset);
		offset += 2;

		return super.read(buffer, offset);
	}

	@Override
	public String toString() {
		return String.format("short0=0x%X, short1=0x%X, value=0x%X", unknownShort0, unknownShort1, value);
	}
}
