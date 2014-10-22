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

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IAction;

public class DelayThreadAction implements IAction {
	private int threadId;
	private int micros;
	private boolean doCallbacks;
	private boolean unblockThread;

	public DelayThreadAction(int threadId, int micros, boolean doCallbacks, boolean unblockThread) {
		this.threadId = threadId;
		this.micros = micros;
		this.doCallbacks = doCallbacks;
		this.unblockThread = unblockThread;
	}

	@Override
	public void execute() {
		if (unblockThread) {
			Modules.ThreadManForUserModule.hleUnblockThread(threadId);
		}
		Modules.ThreadManForUserModule.hleKernelDelayThread(threadId, micros, doCallbacks);
	}
}
