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

public class VSMXBaseObject {
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

	public boolean equals(VSMXBaseObject value) {
		return getFloatValue() == value.getFloatValue();
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

	public int getLength() {
		return 0;
	}

	public void setFloatValue(float value) {
	}

	@Override
	public String toString() {
		return String.format("%f", getFloatValue());
	}
}
