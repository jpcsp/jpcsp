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

import java.util.Random;

import jpcsp.format.rco.vsmx.VSMX;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNumber;

import org.apache.log4j.Logger;

public class Math extends BaseNativeObject {
	private static final Logger log = VSMX.log;
	public static final String objectName = "Math";
	private VSMXInterpreter interpreter;
	private Random random;

	public static VSMXNativeObject create(VSMXInterpreter interpreter) {
		Math math = new Math(interpreter);
		VSMXNativeObject object = new VSMXNativeObject(interpreter, math);
		math.setObject(object);

		object.setPropertyValue("PI", new VSMXNumber(interpreter, (float) java.lang.Math.PI));

		return object;
	}

	private Math(VSMXInterpreter interpreter) {
		this.interpreter = interpreter;
		random = new Random();
	}

	public VSMXBaseObject random(VSMXBaseObject object) {
		float value = random.nextFloat();

		if (log.isDebugEnabled()) {
			log.debug(String.format("Math.random() returns %f", value));
		}

		return new VSMXNumber(interpreter, value);
	}

	public VSMXBaseObject floor(VSMXBaseObject object, VSMXBaseObject value) {
		return new VSMXNumber(interpreter, (float) java.lang.Math.floor(value.getFloatValue()));
	}

	public VSMXBaseObject abs(VSMXBaseObject object, VSMXBaseObject value) {
		return new VSMXNumber(interpreter, (float) java.lang.Math.abs(value.getFloatValue()));
	}

	public VSMXBaseObject sin(VSMXBaseObject object, VSMXBaseObject value) {
		return new VSMXNumber(interpreter, (float) java.lang.Math.sin(value.getFloatValue()));
	}

	public VSMXBaseObject cos(VSMXBaseObject object, VSMXBaseObject value) {
		return new VSMXNumber(interpreter, (float) java.lang.Math.cos(value.getFloatValue()));
	}
}
