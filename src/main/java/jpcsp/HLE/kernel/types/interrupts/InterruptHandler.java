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

import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.IAction;

public class InterruptHandler extends AbstractInterruptHandler {
	private TPointer func;
	private int gp;
	private int interruptNumber;
	private int funcArg;
	private int cb;

	private static class AfterInterruptHandler implements IAction {
		private boolean insideInterrupt;

		public AfterInterruptHandler(boolean insideInterrupt) {
			this.insideInterrupt = insideInterrupt;
		}

		@Override
		public void execute() {
			Managers.intr.setInsideInterrupt(insideInterrupt);
		}
	}

	public InterruptHandler(TPointer func, int gp, int interruptNumber, int funcArg, int cb) {
		this.func = func;
		this.gp = gp;
		this.interruptNumber = interruptNumber;
		this.funcArg = funcArg;
		this.cb = cb;
	}

	@Override
	protected void executeInterrupt() {
		IntrManager intrManager = Managers.intr;
		boolean insideInterrupt = intrManager.isInsideInterrupt();
		intrManager.setInsideInterrupt(true);
		Modules.ThreadManForUserModule.executeCallback(func.getAddress(), gp, new AfterInterruptHandler(insideInterrupt), interruptNumber, funcArg, cb);
	}
}
