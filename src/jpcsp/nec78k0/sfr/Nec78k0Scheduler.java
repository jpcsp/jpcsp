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
package jpcsp.nec78k0.sfr;

import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.now;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.scheduler.SchedulerAction;

/**
 * @author gid15
 *
 */
public class Nec78k0Scheduler extends Thread {
	private Logger log;
	private final Object update = new Object();
	private final List<SchedulerAction> actions;
	private SchedulerAction nextAction;

	public Nec78k0Scheduler() {
		actions = new LinkedList<SchedulerAction>();
	}

	public void setLogger(Logger log) {
		this.log = log;
	}

	@Override
	public void run() {
		RuntimeContext.setLog4jMDC();

		while (true) {
			waitForNextAction();

			long now = now();
			while (true) {
				IAction action = getAction(now);
				if (action == null) {
					break;
				}
				action.execute();
			}
		}
	}

	private void waitForNextAction() {
		long delayMicros = getNextActionDelay(10000L);
		if (delayMicros > 0) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("Scheduler.waitForNextAction delay=0x%X, now=0x%X", delayMicros, now()));
			}

			try {
				synchronized (update) {
					update.wait(delayMicros / 1000, ((int) (delayMicros % 1000)) * 1000);
				}
			} catch (InterruptedException e) {
				// Ignore exception
			}
		}
	}

	private synchronized long getNextActionDelay(long noActionDelay) {
		if (nextAction == null) {
			return noActionDelay;
		}

		long now = now();
		return nextAction.getSchedule() - now;
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
	}

	private synchronized IAction getAction(long now) {
		if (nextAction == null || now < nextAction.getSchedule()) {
			return null;
		}

		IAction action = nextAction.getAction();

		actions.remove(nextAction);
		updateNextAction();

		if (log.isTraceEnabled()) {
			log.trace(String.format("Scheduler.getAction %s, now=0x%X", action, now));
		}

		return action;
	}

	private void addSchedulerAction(SchedulerAction schedulerAction) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("Scheduler.addSchedulerAction %s", schedulerAction));
		}

		actions.add(schedulerAction);
		if (updateNextAction(schedulerAction)) {
			synchronized (update) {
				update.notifyAll();
			}
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

	public synchronized void removeAction(IAction action) {
		for (ListIterator<SchedulerAction> lit = actions.listIterator(); lit.hasNext(); ) {
			SchedulerAction schedulerAction = lit.next();
			if (schedulerAction.getAction() == action) {
				if (log.isTraceEnabled()) {
					log.trace(String.format("Scheduler.removeAction %s", schedulerAction));
				}

				lit.remove();
				updateNextAction();
				break;
			}
		}
	}
}
