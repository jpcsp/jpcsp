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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import jpcsp.Emulator;
import jpcsp.HLE.kernel.types.IAction;

public class Scheduler {
	private static Scheduler instance = null;
	private List<SchedulerAction> actions;
	private SchedulerAction nextAction;

	public static Scheduler getInstance() {
		if (instance == null) {
			instance = new Scheduler();
		}

		return instance;
	}

	public void reset() {
		actions = new LinkedList<SchedulerAction>();
		nextAction = null;
	}

	public void step() {
		if (nextAction == null) {
			return;
		}

		long now = getNow();
		while (true) {
			IAction action = getAction(now);
			if (action == null) {
				break;
			}
			action.execute();
		}
	}

	private void addSchedulerAction(SchedulerAction schedulerAction) {
		actions.add(schedulerAction);
		updateNextAction(schedulerAction);
	}

	public void addAction(long schedule, IAction action) {
		SchedulerAction schedulerAction = new SchedulerAction(schedule, action);
		addSchedulerAction(schedulerAction);
	}

	public void removeAction(long schedule, IAction action) {
		for (ListIterator<SchedulerAction> lit = actions.listIterator(); lit.hasNext(); ) {
			SchedulerAction schedulerAction = lit.next();
			if (schedulerAction.getSchedule() == schedule && schedulerAction.getAction() == action) {
				lit.remove();
				updateNextAction();
				break;
			}
		}
	}

	private void updateNextAction(SchedulerAction schedulerAction) {
		if (nextAction == null || schedulerAction.getSchedule() < nextAction.getSchedule()) {
			nextAction = schedulerAction;
		}
	}

	private void updateNextAction() {
		nextAction = null;

		for (Iterator<SchedulerAction> it = actions.iterator(); it.hasNext(); ) {
			SchedulerAction schedulerAction = it.next();
			updateNextAction(schedulerAction);
		}
	}

	public IAction getAction(long now) {
		if (nextAction == null || now < nextAction.getSchedule()) {
			return null;
		}

		IAction action = nextAction.getAction();

		actions.remove(nextAction);
		updateNextAction();

		return action;
	}

	public long getNow() {
		return Emulator.getClock().microTime();
	}
}
