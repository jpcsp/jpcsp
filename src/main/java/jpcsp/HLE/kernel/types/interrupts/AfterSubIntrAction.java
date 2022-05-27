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

import java.util.Iterator;

import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.IAction;

public class AfterSubIntrAction implements IAction {
	private IntrManager intrManager;
	private Iterator<AbstractAllegrexInterruptHandler> allegrexInterruptHandlersIterator;
	private InterruptState interruptState;

	public AfterSubIntrAction(IntrManager intrManager, InterruptState interruptState, Iterator<AbstractAllegrexInterruptHandler> allegrexInterruptHandlersIterator) {
		this.intrManager = intrManager;
		this.interruptState = interruptState;
		this.allegrexInterruptHandlersIterator = allegrexInterruptHandlersIterator;
	}

	@Override
	public void execute() {
		IAction afterHandlerAction = interruptState.getAfterHandlerAction();
		if (afterHandlerAction != null) {
			afterHandlerAction.execute();
		}

		intrManager.continueCallAllegrexInterruptHandler(interruptState, allegrexInterruptHandlersIterator, this);
	}
}
