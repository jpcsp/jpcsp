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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.kernel.types.IAction;
import jpcsp.format.rco.vsmx.VSMX;
import jpcsp.format.rco.vsmx.interpreter.VSMXArray;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXFunction;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNumber;
import jpcsp.scheduler.Scheduler;

public class Timer extends BaseNativeObject {
	private static final Logger log = VSMX.log;
	private int currentTimerId = 0;
	private VSMXInterpreter interpreter;
	private Map<Integer, TimerAction> timers;

	private class TimerAction implements IAction {
		private int id;
		private VSMXBaseObject object;
		private VSMXBaseObject function;
		private VSMXBaseObject[] parameters;

		public TimerAction(int id, VSMXBaseObject object, VSMXBaseObject function, VSMXBaseObject[] parameters) {
			this.id = id;
			this.object = object;
			this.function = function;
			this.parameters = parameters;
		}

		@Override
		public void execute() {
			onTimer(id, object, function, parameters);
		}
	}

	public static VSMXNativeObject create(VSMXInterpreter interpreter) {
		Timer timer = new Timer(interpreter);
		VSMXNativeObject object = new VSMXNativeObject(interpreter, timer);
		timer.setObject(object);

		return object;
	}

	private Timer(VSMXInterpreter interpreter) {
		this.interpreter = interpreter;
		timers = new HashMap<Integer, Timer.TimerAction>();
	}

	private void onTimer(int id, VSMXBaseObject object, VSMXBaseObject function, VSMXBaseObject[] parameters) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Timer.onTimer id=%d, object=%s, function=%s, parameters=%s", id, object, function, parameters));
			for (int i = 0; i < parameters.length; i++) {
				log.debug(String.format("Timer.onTimer param%d=%s", i, parameters[i]));
			}
		}

		if (function instanceof VSMXFunction) {
			interpreter.interpretFunction((VSMXFunction) function, object, parameters);
		}
	}

	private VSMXBaseObject setInterval(VSMXBaseObject object, VSMXBaseObject function, VSMXBaseObject interval, VSMXBaseObject... parameters) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Timer.setInterval function=%s, interval=%d, numberOfParameters=%d", function, interval.getIntValue(), parameters.length));
			for (int i = 0; i < parameters.length; i++) {
				log.debug(String.format("Timer.setInterval param%d=%s", i, parameters[i]));
			}
		}

		int id = currentTimerId++;
		long schedule = Scheduler.getNow() + interval.getIntValue() * 1000;

		TimerAction timerAction = new TimerAction(id, object, function, parameters);
		timers.put(id, timerAction);
		Scheduler.getInstance().addAction(schedule, timerAction);

		// setInterval seems to return an array object. Not sure how to fill it.
		VSMXArray result = new VSMXArray(interpreter, 1);
		result.setPropertyValue(0, new VSMXNumber(interpreter, id));

		return result;
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

	public void clearInterval(VSMXBaseObject object, VSMXBaseObject id) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Timer.clearInterval %d", id.getPropertyValue(0).getIntValue()));
		}
	}
}
