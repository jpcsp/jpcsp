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
package jpcsp.scheduler;

import jpcsp.HLE.kernel.types.IAction;

public class SchedulerAction {
	private long schedule;
	private IAction action;

	public SchedulerAction(long schedule, IAction action) {
		this.schedule = schedule;
		this.action = action;
	}

	public long getSchedule() {
		return schedule;
	}

	public void setSchedule(long schedule) {
		this.schedule = schedule;
	}

	public IAction getAction() {
		return action;
	}

	public void setAction(IAction action) {
		this.action = action;
	}
}
