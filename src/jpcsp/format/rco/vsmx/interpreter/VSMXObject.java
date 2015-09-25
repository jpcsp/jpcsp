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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VSMXObject extends VSMXBaseObject {
	protected Map<String, VSMXBaseObject> properties = new HashMap<String, VSMXBaseObject>();

	@Override
	public VSMXBaseObject getPropertyValue(String name) {
		VSMXBaseObject value = properties.get(name);
		if (value == null) {
			value = VSMXUndefined.singleton;
			properties.put(name, value);
		}
		return value;
	}

	public Set<String> getPropertyNames() {
		return properties.keySet();
	}

	@Override
	public void setPropertyValue(String name, VSMXBaseObject value) {
		properties.put(name, value);
	}

	@Override
	public void deletePropertyValue(String name) {
		properties.remove(name);
	}

	@Override
	public boolean equals(VSMXBaseObject value) {
		if (value instanceof VSMXObject) {
			// Return true if both values refer to the same object
			return this == value;
		}
		return super.equals(value);
	}

	@Override
	public String typeOf() {
		return "object";
	}

	protected void toString(StringBuilder s) {
		String[] keys = properties.keySet().toArray(new String[0]);
		Arrays.sort(keys);
		for (String key : keys) {
			VSMXBaseObject value = properties.get(key);
			if (s.length() > 1) {
				s.append(",\n");
			}
			s.append(String.format("%s=%s", key, value));
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("[");
		toString(s);
		s.append("]");

		return s.toString();
	}
}
