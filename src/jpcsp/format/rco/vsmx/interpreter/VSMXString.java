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

public class VSMXString extends VSMXBaseObject {
	private String value;

	public VSMXString(VSMXInterpreter interpreter, String value) {
		super(interpreter);
		this.value = value;
	}

	@Override
	public float getFloatValue() {
		return Float.valueOf(value).floatValue();
	}

	@Override
	public void setFloatValue(float value) {
		this.value = Float.toString(value);
	}

	@Override
	public String getStringValue() {
		return value;
	}

	@Override
	public VSMXBaseObject getPropertyValue(String name) {
		if (lengthName.equals(name)) {
			return new VSMXNumber(interpreter, value.length());
		}

		return super.getPropertyValue(name);
	}

	@Override
	public boolean equals(VSMXBaseObject value) {
		return getStringValue().equals(value.getStringValue());
	}

	@Override
	public boolean identity(VSMXBaseObject value) {
		if (value instanceof VSMXString) {
			return getStringValue().equals(value.getStringValue());
		}
		return super.identity(value);
	}

	@Override
	public String typeOf() {
		return "string";
	}

	@Override
	public String getClassName() {
		return "String";
	}

	@Override
	public String toString() {
		return String.format("\"%s\"", value);
	}
}
