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
		boolean removed = (subInterrupts.remove(id) != null);

		if (maxIndex >= subInterrupts.size()) {
			maxIndex = subInterrupts.size() - 1;
		}

		return removed;
	}

	public SubIntrHandler getSubIntrHandler(int id) {
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
		for (int id = minIndex; id <= maxIndex; id++) {
			SubIntrHandler subIntrHandler = getSubIntrHandler(id);
			if (subIntrHandler != null) {
				IntrManager.getInstance().pushAllegrexInterruptHandler(subIntrHandler);
			}
		}
	}
}
