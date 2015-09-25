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

public abstract class VSMXBaseObject {
	protected static final String lengthName = "length";

	public VSMXBaseObject getValue() {
		return this;
	}

	public float getFloatValue() {
		return 0f;
	}

	public int getIntValue() {
		return (int) getFloatValue();
	}

	public boolean getBooleanValue() {
		return getFloatValue() != 0f;
	}

	public String getStringValue() {
		return toString();
	}

	public boolean equals(VSMXBaseObject value) {
		return getFloatValue() == value.getFloatValue();
	}

	public boolean identity(VSMXBaseObject value) {
		return this == value;
	}

	public VSMXBaseObject getPropertyValue(String name) {
		return VSMXUndefined.singleton;
	}

	public VSMXBaseObject getPropertyValue(int index) {
		return getPropertyValue(Integer.toString(index));
	}

	public void setPropertyValue(String name, VSMXBaseObject value) {
	}

	public void setPropertyValue(int index, VSMXBaseObject value) {
		setPropertyValue(Integer.toString(index), value);
	}

	public void deletePropertyValue(String name) {
		setPropertyValue(name, VSMXUndefined.singleton);
	}

	public void deletePropertyValue(int index) {
		deletePropertyValue(Integer.toString(index));
	}

	public void setFloatValue(float value) {
	}

	public abstract String typeOf();

	@Override
	public String toString() {
		return Float.toString(getFloatValue());
	}
}
