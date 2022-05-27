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

public class VSMXArray extends VSMXObject {
	private static final String className = "Array";
	private int length;

	public VSMXArray(VSMXInterpreter interpreter) {
		super(interpreter, className);
	}

	public VSMXArray(VSMXInterpreter interpreter, int size) {
		super(interpreter, className);

		if (size > 0) {
			length = size;
			for (int i = 0; i < size; i++) {
				create(i);
			}
		}
	}

	public int getLength() {
		return length;
	}

	private void create(int index) {
		super.setPropertyValue(Integer.toString(index), VSMXUndefined.singleton);
	}

	private void delete(int index) {
		super.deletePropertyValue(Integer.toString(index));
	}

	private void updateLength(int index) {
		if (index >= length) {
			for (int i = length; i <= index; i++) {
				create(i);
			}
			length = index + 1;
		}
	}

	@Override
	public VSMXBaseObject getPropertyValue(String name) {
		if (lengthName.equals(name)) {
			return new VSMXNumber(interpreter, length);
		}

		int index = getIndex(name);
		if (index >= 0) {
			return getPropertyValue(index);
		}

		return super.getPropertyValue(name);
	}

	@Override
	public void setPropertyValue(String name, VSMXBaseObject value) {
		if (lengthName.equals(name)) {
			int newLength = value.getIntValue();
			if (newLength > length) {
				for (int i = length; i < newLength; i++) {
					create(i);
				}
			} else if (newLength < length) {
				for (int i = newLength; i < length; i++) {
					delete(i);
				}
			}
			return;
		}

		int index = getIndex(name);
		if (index >= 0) {
			setPropertyValue(index, value);
		} else {
			super.setPropertyValue(name, value);
		}
	}

	@Override
	public void deletePropertyValue(String name) {
		if (lengthName.equals(name)) {
			// Cannot delete "length" property
			return;
		}

		int index = getIndex(name);
		if (index >= 0) {
			deletePropertyValue(index);
		} else {
			super.deletePropertyValue(name);
		}
	}

	@Override
	public VSMXBaseObject getPropertyValue(int index) {
		if (index < 0) {
			return VSMXUndefined.singleton;
		}

		updateLength(index);

		return super.getPropertyValue(Integer.toString(index));
	}

	@Override
	public void setPropertyValue(int index, VSMXBaseObject value) {
		if (index >= 0) {
			updateLength(index);
			super.setPropertyValue(Integer.toString(index), value);
		}
	}

	@Override
	public void deletePropertyValue(int index) {
		if (index >= 0) {
			if (index == length - 1) { // Deleting the last element of the array?
				delete(index);
				length = index;
			} else if (index < length) { // Deleting in the middle of the array?
				create(index);
			}
		}
	}

	@Override
	public boolean hasPropertyValue(String name) {
		if (lengthName.equals(name)) {
			return true;
		}

		return super.hasPropertyValue(name);
	}

	@Override
	public boolean getBooleanValue() {
		// "if" on an empty array seems to return false. E.g.
		//     x = {};
		//     if (x) { notexecuted; }
		return length > 0;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(String.format("[length=%d", length));
		toString(s);
		s.append("]");

		return s.toString();
	}

	@Override
	public boolean equals(VSMXBaseObject value) {
		if (value instanceof VSMXArray) {
			// Empty arrays are always equal
			if (getLength() == 0 && ((VSMXArray) value).getLength() == 0) {
				return true;
			}
		}

		return super.equals(value);
	}
}
