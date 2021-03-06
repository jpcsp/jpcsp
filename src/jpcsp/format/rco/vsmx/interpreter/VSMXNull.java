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

public class VSMXNull extends VSMXBaseObject {
	public static final VSMXNull singleton = new VSMXNull();

	private VSMXNull() {
		super(null);
	}

	public static void init(VSMXInterpreter interpreter) {
		singleton.setInterpreter(interpreter);
	}

	@Override
	public boolean equals(VSMXBaseObject value) {
		// null == undefined
		return this == value || value == VSMXUndefined.singleton;
	}

	@Override
	public String typeOf() {
		return "undefined";
	}

	@Override
	public String getClassName() {
		return "Null";
	}

	@Override
	public String toString() {
		return "null";
	}
}
