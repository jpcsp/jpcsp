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

	public VSMXBaseObject setInterval(VSMXBaseObject object, VSMXBaseObject function, VSMXBaseObject interval) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Timer.setInterval function=%s, interval=%d", function, interval.getIntValue()));
		}

		int id = currentTimerId++;

		return new VSMXNumber(interpreter, id);
	}

	public void clearInterval(VSMXBaseObject id) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Timer.clearInterval %d", id.getIntValue()));
		}
	}
}
