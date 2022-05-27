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

public class FloatType extends BaseType {
	@Override
	public float getFloatValue() {
		return Float.intBitsToFloat(super.getIntValue());
	}

	@Override
	public void setFloatValue(float value) {
		super.setIntValue(Float.floatToRawIntBits(value));
	}

	@Override
	public int getIntValue() {
		return (int) getFloatValue();
	}

	@Override
	public void setIntValue(int value) {
		setFloatValue((float) value);
	}

	@Override
	public String toString() {
		return String.format("value=%f", getFloatValue());
	}
}
