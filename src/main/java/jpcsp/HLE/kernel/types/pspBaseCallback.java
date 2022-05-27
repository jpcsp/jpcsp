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
package jpcsp.HLE.kernel.types;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;

public class pspBaseCallback {
	public static final String callbackUidPurpose = "ThreadMan-callback";
	private final int arguments[];
	private final int callbackFunction;
	private final int uid;

	public pspBaseCallback(int callbackFunction, int numberArguments) {
		this.callbackFunction = callbackFunction;
		arguments = new int[numberArguments];
        uid = SceUidManager.getNewUid(callbackUidPurpose);
	}

	public int getArgument(int n) {
		return arguments[n];
	}

	public void setArgument(int n, int value) {
		arguments[n] = value;
	}

	public int getCallbackFunction() {
		return callbackFunction;
	}

	public boolean hasCallbackFunction() {
		return callbackFunction != 0;
	}

    public int getUid() {
    	return uid;
    }

	public void call(SceKernelThreadInfo thread, IAction afterAction) {
		Modules.ThreadManForUserModule.executeCallback(thread, callbackFunction, afterAction, true, arguments);
	}
}
