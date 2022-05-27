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
package jpcsp.format.rco.anim;

import static jpcsp.scheduler.Scheduler.getNow;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.format.rco.vsmx.VSMX;

public abstract class AbstractAnimAction implements IAction {
	protected static final Logger log = VSMX.log;
	private int duration;
	private long start;

	protected AbstractAnimAction(int duration) {
		this.duration = duration * 1000;

		start = getNow();
	}

	private long getNextSchedule(long now) {
		return now + Math.max(duration / 10000, 1000);
	}

	@Override
	public void execute() {
		long now = getNow();
		int currentDuration = (int) (now - start);
		if (log.isDebugEnabled()) {
			log.debug(String.format("BaseAnimAction duration=%d/%d", currentDuration, duration));
		}
		currentDuration = Math.min(currentDuration, duration);
		float step = currentDuration == duration ? 1f : currentDuration / (float) duration;

		anim(step);

		if (currentDuration < duration) {
			Emulator.getScheduler().addAction(getNextSchedule(now), this);
		}
	}

	static protected float interpolate(float start, float end, float step) {
		return start + (end - start) * step;
	}

	protected abstract void anim(float step);
}
