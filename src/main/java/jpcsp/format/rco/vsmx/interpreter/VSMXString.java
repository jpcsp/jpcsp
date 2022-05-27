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

	public VSMXBaseObject toUpperCase(VSMXBaseObject object) {
		return new VSMXString(getInterpreter(), getStringValue().toUpperCase());
	}

	public VSMXBaseObject toLowerCase(VSMXBaseObject object) {
		return new VSMXString(getInterpreter(), getStringValue().toLowerCase());
	}

	public VSMXBaseObject substring(VSMXBaseObject object, VSMXBaseObject start) {
		String s = getStringValue();

		int beginIndex = start.getIntValue();
		beginIndex = Math.max(beginIndex, 0);
		beginIndex = Math.min(beginIndex, s.length());

		return new VSMXString(getInterpreter(), s.substring(beginIndex));
	}

	public VSMXBaseObject substring(VSMXBaseObject object, VSMXBaseObject start, VSMXBaseObject end) {
		String s = getStringValue();

		int beginIndex = start.getIntValue();
		beginIndex = Math.max(beginIndex, 0);
		beginIndex = Math.min(beginIndex, s.length());

		int endIndex = end.getIntValue();
		endIndex = Math.max(endIndex, 0);
		endIndex = Math.min(endIndex, s.length());

		// The substring method uses the lower value of start and end as the beginning point of the substring.
		if (beginIndex > endIndex) {
			int tmp = beginIndex;
			beginIndex = endIndex;
			endIndex = tmp;
		}

		return new VSMXString(getInterpreter(), s.substring(beginIndex, endIndex));
	}

	public VSMXBaseObject lastIndexOf(VSMXBaseObject object, VSMXBaseObject substring) {
		return lastIndexOf(object, substring, new VSMXNumber(interpreter, object.getStringValue().length()));
	}

	public VSMXBaseObject lastIndexOf(VSMXBaseObject object, VSMXBaseObject substring, VSMXBaseObject startIndex) {
		int startIndexInt = startIndex.getIntValue();
		String substringString = substring.getStringValue();
		String s = getStringValue();

		if (startIndexInt < 0) {
			startIndexInt = 0;
		} else if (startIndexInt > s.length()) {
			startIndexInt = s.length();
		}

		int lastIndexOfInt = s.lastIndexOf(substringString, startIndexInt);

		return new VSMXNumber(interpreter, lastIndexOfInt);
	}

	public VSMXBaseObject charAt(VSMXBaseObject object, VSMXBaseObject index) {
		String s = getStringValue();
		int i = index.getIntValue();
		if (i < 0 || i >= s.length()) {
			return new VSMXString(interpreter, "");
		}

		char c = s.charAt(i);
		return new VSMXString(interpreter, "" + c);
	}

	@Override
	public String toString() {
		return String.format("\"%s\"", value);
	}
}
