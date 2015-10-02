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

public class Timer extends BaseNativeObject {
	private static final Logger log = VSMX.log;
	private int currentTimerId = 0;
	private VSMXInterpreter interpreter;

	public static VSMXNativeObject create(VSMXInterpreter interpreter) {
		Timer timer = new Timer(interpreter);
		VSMXNativeObject object = new VSMXNativeObject(interpreter, timer);
		timer.setObject(object);

		return object;
	}

	private Timer(VSMXInterpreter interpreter) {
		this.interpreter = interpreter;
	}

	private VSMXBaseObject setInterval(VSMXBaseObject object, VSMXBaseObject function, VSMXBaseObject interval, VSMXBaseObject... parameters) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Timer.setInterval function=%s, interval=%d, numberOfParameters=%d", function, interval.getIntValue(), parameters.length));
			for (int i = 0; i < parameters.length; i++) {
				log.debug(String.format("Timer.setInterval param%d=%s", i, parameters[i]));
			}
		}

		int id = currentTimerId++;

		return new VSMXNumber(interpreter, id);
	}

	public VSMXBaseObject setInterval(VSMXBaseObject object, VSMXBaseObject function, VSMXBaseObject interval) {
		return setInterval(object, function, interval, new VSMXBaseObject[0]);
	}

	public VSMXBaseObject setInterval(VSMXBaseObject object, VSMXBaseObject function, VSMXBaseObject interval, VSMXBaseObject param1) {
		return setInterval(object, function, interval, new VSMXBaseObject[] { param1 });
	}

	public VSMXBaseObject setInterval(VSMXBaseObject object, VSMXBaseObject function, VSMXBaseObject interval, VSMXBaseObject param1, VSMXBaseObject param2) {
		return setInterval(object, function, interval, new VSMXBaseObject[] { param1, param2 });
	}

	public VSMXBaseObject setInterval(VSMXBaseObject object, VSMXBaseObject function, VSMXBaseObject interval, VSMXBaseObject param1, VSMXBaseObject param2, VSMXBaseObject param3) {
		return setInterval(object, function, interval, new VSMXBaseObject[] { param1, param2, param3 });
	}

	public VSMXBaseObject setInterval(VSMXBaseObject object, VSMXBaseObject function, VSMXBaseObject interval, VSMXBaseObject param1, VSMXBaseObject param2, VSMXBaseObject param3, VSMXBaseObject param4) {
		return setInterval(object, function, interval, new VSMXBaseObject[] { param1, param2, param3, param4 });
	}

	public VSMXBaseObject setInterval(VSMXBaseObject object, VSMXBaseObject function, VSMXBaseObject interval, VSMXBaseObject param1, VSMXBaseObject param2, VSMXBaseObject param3, VSMXBaseObject param4, VSMXBaseObject param5) {
		return setInterval(object, function, interval, new VSMXBaseObject[] { param1, param2, param3, param4, param5 });
	}

	public VSMXBaseObject setInterval(VSMXBaseObject object, VSMXBaseObject function, VSMXBaseObject interval, VSMXBaseObject param1, VSMXBaseObject param2, VSMXBaseObject param3, VSMXBaseObject param4, VSMXBaseObject param5, VSMXBaseObject param6) {
		return setInterval(object, function, interval, new VSMXBaseObject[] { param1, param2, param3, param4, param5, param6 });
	}

	public void clearInterval(VSMXBaseObject id) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Timer.clearInterval %d", id.getIntValue()));
		}
	}
}
