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
package jpcsp.HLE;

import jpcsp.Emulator;
import jpcsp.HLE.kernel.types.IAction;

/**
 * Action which can be used to store the return value of a callback.
 * See ThreadManForUser.executeCallback().
 * 
 * @author gid15
 *
 */
public class AfterCallbackAction implements IAction {
	private final TPointerFunction callback;
	private boolean called;
	private int returnValue;

	public AfterCallbackAction() {
		callback = null;
	}

	public AfterCallbackAction(TPointerFunction callback) {
		this.callback = callback;
	}

	@Override
	public void execute() {
		called = true;
		returnValue = Emulator.getProcessor().cpu._v0;
	}

	public boolean isCalled() {
		return called;
	}

	public int getReturnValue() {
		return returnValue;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("Callback");

		if (callback != null && callback.isNotNull()) {
			s.append(String.format(" %s", callback));
		}

		if (isCalled()) {
			s.append(String.format(" returned 0x%08X", getReturnValue()));
		} else {
			s.append(" not called");
		}

		return s.toString();
	}
}
