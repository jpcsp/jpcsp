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
package jpcsp.HLE.kernel.types.interrupts;

import java.util.Vector;

import jpcsp.HLE.kernel.managers.IntrManager;

public class IntrHandler extends AbstractInterruptHandler {
	private Vector<SubIntrHandler> subInterrupts = new Vector<SubIntrHandler>();
	private int minIndex = Integer.MAX_VALUE;
	private int maxIndex = Integer.MIN_VALUE;
	private boolean enabled;

	public IntrHandler() {
		enabled = true;
	}

	public IntrHandler(boolean enabled) {
		this.enabled = enabled;
	}

	public void addSubIntrHandler(int id, SubIntrHandler subIntrHandler) {
		if (id >= subInterrupts.size()) {
			subInterrupts.setSize(id + 1);
		}

		if (id < minIndex) {
			minIndex = id;
		}
		if (id > maxIndex) {
			maxIndex = id;
		}

		subInterrupts.set(id, subIntrHandler);
	}

	public boolean removeSubIntrHandler(int id) {
		if (id < 0 || id >= subInterrupts.size()) {
			return false;
		}

		boolean removed = (subInterrupts.get(id) != null);
		subInterrupts.set(id, null);

		// Find the first non-null sub-interrupt
		minIndex = Integer.MAX_VALUE;
		for (int i = 0; i < subInterrupts.size(); i++) {
			if (subInterrupts.get(i) != null) {
				minIndex = i;
				break;
			}
		}

		// Find the last non-null sub-interrupt
		maxIndex = Integer.MIN_VALUE;
		for (int i = subInterrupts.size() - 1; i >= minIndex; i--) {
			if (subInterrupts.get(i) != null) {
				maxIndex = i;
				break;
			}
		}

		return removed;
	}

	public SubIntrHandler getSubIntrHandler(int id) {
		if (id < 0 || id >= subInterrupts.size()) {
			return null;
		}

		return subInterrupts.get(id);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public void execute() {
		if (isEnabled()) {
			super.execute();
		}
	}

	@Override
	protected void executeInterrupt() {
		if (isEnabled()) {
			for (int id = minIndex; id <= maxIndex; id++) {
				SubIntrHandler subIntrHandler = getSubIntrHandler(id);
				if (subIntrHandler != null && subIntrHandler.isEnabled()) {
					IntrManager.getInstance().pushAllegrexInterruptHandler(subIntrHandler);
				}
			}
		}
	}
}
