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
package jpcsp.format.rco.vsmx;

public class VSMXGroup {
	public static final int SIZE_OF = 8;
	public int id;
	public int value;

	public float getFloatValue() {
		return Float.intBitsToFloat(value);
	}

	public int getOpcode() {
		return id & 0xFF;
	}

	public boolean isOpcode(int opcode) {
		return getOpcode() == opcode;
	}

	@Override
	public String toString() {
		return String.format("%s(value=0x%X, id=0x%X)", VSMXCode.VsmxDecOps[getOpcode()], value, id);
	}
}
