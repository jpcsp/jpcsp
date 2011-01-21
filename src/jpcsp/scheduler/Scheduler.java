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
import jpcsp.Allegrex.compiler.RuntimeContext;
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

	public synchronized void reset() {
		actions = new LinkedList<SchedulerAction>();
		nextAction = null;
	}

	public synchronized void step() {
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

	public synchronized long getNextActionDelay(long noActionDelay) {
		if (nextAction == null) {
			return noActionDelay;
		}

		long now = getNow();
		return nextAction.getSchedule() - now;
	}

	private void addSchedulerAction(SchedulerAction schedulerAction) {
		actions.add(schedulerAction);
		if (updateNextAction(schedulerAction)) {
			RuntimeContext.onNextScheduleModified();
		}
	}

	/**
	 * Add a new action to be executed as soon as possible to the Scheduler.
	 * This method has to be thread-safe.
	 *
	 * @param action	action to be executed on the defined schedule.
	 */
	public synchronized void addAction(IAction action) {
		SchedulerAction schedulerAction = new SchedulerAction(0, action);
		addSchedulerAction(schedulerAction);
	}

	/**
	 * Add a new action to the Scheduler.
	 * This method has to be thread-safe.
	 *
	 * @param schedule	microTime when the action has to be executed. 0 for now.
	 * @param action	action to be executed on the defined schedule.
	 */
	public synchronized void addAction(long schedule, IAction action) {
		SchedulerAction schedulerAction = new SchedulerAction(schedule, action);
		addSchedulerAction(schedulerAction);
	}

	public synchronized void removeAction(long schedule, IAction action) {
		for (ListIterator<SchedulerAction> lit = actions.listIterator(); lit.hasNext(); ) {
			SchedulerAction schedulerAction = lit.next();
			if (schedulerAction.getSchedule() == schedule && schedulerAction.getAction() == action) {
				lit.remove();
				updateNextAction();
				break;
			}
		}
	}

	private boolean updateNextAction(SchedulerAction schedulerAction) {
		if (nextAction == null || schedulerAction.getSchedule() < nextAction.getSchedule()) {
			nextAction = schedulerAction;
			return true;
		}

		return false;
	}

	private void updateNextAction() {
		nextAction = null;

		for (Iterator<SchedulerAction> it = actions.iterator(); it.hasNext(); ) {
			SchedulerAction schedulerAction = it.next();
			updateNextAction(schedulerAction);
		}

		RuntimeContext.onNextScheduleModified();
	}

	public synchronized IAction getAction(long now) {
		if (nextAction == null || now < nextAction.getSchedule()) {
			return null;
		}

		IAction action = nextAction.getAction();

		actions.remove(nextAction);
		updateNextAction();

		return action;
	}

	public static long getNow() {
		return Emulator.getClock().microTime();
	}
}
