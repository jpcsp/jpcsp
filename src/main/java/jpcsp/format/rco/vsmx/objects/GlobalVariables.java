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
package jpcsp.format.rco.vsmx.objects;

import org.apache.log4j.Logger;

import jpcsp.format.rco.vsmx.VSMX;
import jpcsp.format.rco.vsmx.interpreter.VSMXArray;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXBoolean;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNumber;
import jpcsp.format.rco.vsmx.interpreter.VSMXObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXUndefined;
import jpcsp.hardware.Screen;

public class GlobalVariables extends BaseNativeObject {
	private static final Logger log = VSMX.log;
	private static final Logger logWriteln = Logger.getLogger("writeln");
	private StringBuilder writeBuffer = new StringBuilder();

	public static VSMXNativeObject create(VSMXInterpreter interpreter) {
		GlobalVariables globalVariables = new GlobalVariables(interpreter);
		VSMXNativeObject object = new VSMXNativeObject(interpreter, globalVariables);
		globalVariables.setObject(object);

		object.setPropertyValue("undefined", VSMXUndefined.singleton);
		object.setPropertyValue("Array", new VSMXArray(interpreter));
		object.setPropertyValue("Object", new VSMXObject(interpreter, null));

		object.setPropertyValue("timer", Timer.create(interpreter));

		object.setPropertyValue("x", new VSMXNumber(interpreter, 0));
		object.setPropertyValue("y", new VSMXNumber(interpreter, 0));
		object.setPropertyValue("width", new VSMXNumber(interpreter, Screen.width));
		object.setPropertyValue("height", new VSMXNumber(interpreter, Screen.height));

		return object;
	}

	private GlobalVariables(VSMXInterpreter interpreter) {
	}

	private void writeln() {
		logWriteln.debug(writeBuffer.toString());
		writeBuffer.setLength(0);
	}

	private void writeln(VSMXBaseObject object, VSMXBaseObject... strings) {
		write(object, strings);
		writeln();
	}

	private void write(VSMXBaseObject object, VSMXBaseObject... strings) {
		for (int i = 0; i < strings.length; i++) {
			writeBuffer.append(strings[i].getStringValue());
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("write: '%s'", writeBuffer.toString()));
		}
	}

	public void write(VSMXBaseObject object, VSMXBaseObject s1) {
		write(object, new VSMXBaseObject[] { s1 });
	}

	public void writeln(VSMXBaseObject object) {
		writeln();
	}

	public void writeln(VSMXBaseObject object, VSMXBaseObject s1) {
		writeln(object, new VSMXBaseObject[] { s1 });
	}

	public void writeln(VSMXBaseObject object, VSMXBaseObject s1, VSMXBaseObject s2) {
		writeln(object, new VSMXBaseObject[] { s1, s2 });
	}

	public void writeln(VSMXBaseObject object, VSMXBaseObject s1, VSMXBaseObject s2, VSMXBaseObject s3) {
		writeln(object, new VSMXBaseObject[] { s1, s3 });
	}

	public void writeln(VSMXBaseObject object, VSMXBaseObject s1, VSMXBaseObject s2, VSMXBaseObject s3, VSMXBaseObject s4) {
		writeln(object, new VSMXBaseObject[] { s1, s3, s4 });
	}

	public void writeln(VSMXBaseObject object, VSMXBaseObject s1, VSMXBaseObject s2, VSMXBaseObject s3, VSMXBaseObject s4, VSMXBaseObject s5) {
		writeln(object, new VSMXBaseObject[] { s1, s3, s4, s5 });
	}

	public void writeln(VSMXBaseObject object, VSMXBaseObject s1, VSMXBaseObject s2, VSMXBaseObject s3, VSMXBaseObject s4, VSMXBaseObject s5, VSMXBaseObject s6) {
		writeln(object, new VSMXBaseObject[] { s1, s3, s4, s5, s6 });
	}

	public VSMXBaseObject parseFloat(VSMXBaseObject object, VSMXBaseObject value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("parseFloat: %s", value));
		}

		return new VSMXNumber(object.getInterpreter(), value.getFloatValue());
	}

	public VSMXBaseObject parseInt(VSMXBaseObject object, VSMXBaseObject value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("parseInt: %s", value));
		}

		return new VSMXNumber(object.getInterpreter(), value.getIntValue());
	}

	public VSMXBaseObject isNaN(VSMXBaseObject object, VSMXBaseObject value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("isNaN: %s", value));
		}

		boolean isNaN = Float.isNaN(value.getFloatValue());

		return VSMXBoolean.getValue(isNaN);
	}

	public VSMXBaseObject Float(VSMXBaseObject object, VSMXBaseObject value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Float: %s", value));
		}

		return new VSMXNumber(object.getInterpreter(), value.getFloatValue());
	}

	public VSMXBaseObject Int(VSMXBaseObject object, VSMXBaseObject value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Int: %s", value));
		}

		return new VSMXNumber(object.getInterpreter(), value.getIntValue());
	}
}
