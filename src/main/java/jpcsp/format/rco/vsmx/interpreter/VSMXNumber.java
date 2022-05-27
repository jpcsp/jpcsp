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
package jpcsp.format.rco.vsmx.interpreter;

public class VSMXNumber extends VSMXBaseObject {
	private float value;

	public VSMXNumber(VSMXInterpreter interpreter, float value) {
		super(interpreter);
		this.value = value;
	}

	public VSMXNumber(VSMXInterpreter interpreter, int value) {
		super(interpreter);
		this.value = (float) value;
	}

	@Override
	public float getFloatValue() {
		return value;
	}

	@Override
	public String typeOf() {
		return "number";
	}

	@Override
	public String getClassName() {
		return "Number";
	}

	@Override
	public void setFloatValue(float value) {
		this.value = value;
	}

	@Override
	public boolean identity(VSMXBaseObject value) {
		if (value instanceof VSMXNumber) {
			return getFloatValue() == value.getFloatValue();
		}

		return super.identity(value);
	}
}
