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
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNumber;

public class GlobalVariables extends BaseNativeObject {
	private static final Logger log = VSMX.log;
	private static final Logger logWriteln = Logger.getLogger("writeln");

	public static VSMXNativeObject create(VSMXInterpreter interpreter) {
		GlobalVariables globalVariables = new GlobalVariables(interpreter);
		VSMXNativeObject object = new VSMXNativeObject(interpreter, globalVariables);
		globalVariables.setObject(object);

		object.setPropertyValue("timer", Timer.create(interpreter));

		return object;
	}

	private GlobalVariables(VSMXInterpreter interpreter) {
	}

	public void writeln(VSMXBaseObject object, VSMXBaseObject s) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("writeln: '%s'", s.getStringValue()));
		}
		logWriteln.debug(s.getStringValue());
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
}
