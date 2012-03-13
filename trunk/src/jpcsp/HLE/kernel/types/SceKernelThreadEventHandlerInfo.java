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

import jpcsp.Emulator;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;

public class SceKernelThreadEventHandlerInfo extends pspAbstractMemoryMappedStructureVariableLength {
	public final String name;
	public final int thid;
	public int mask;
    public int handler;
    public final int common;

    // Internal info.
	public final int uid;
    public int result;  // Return value from the handler's callback.

	private static final String uidPurpose = "ThreadMan-ThreadEventHandler";

    // Thread Event IDs.
    public final static int THREAD_EVENT_ID_ALL = 0xFFFFFFFF;
    public final static int THREAD_EVENT_ID_KERN = 0xFFFFFFF8;
    public final static int THREAD_EVENT_ID_USER = 0xFFFFFFF0;
    public final static int THREAD_EVENT_ID_CURRENT = 0x0;
    // Thread Events.
    public final static int THREAD_EVENT_CREATE = 0x1;
    public final static int THREAD_EVENT_START = 0x2;
    public final static int THREAD_EVENT_EXIT = 0x4;
    public final static int THREAD_EVENT_DELETE = 0x8;
    public final static int THREAD_EVENT_ALL = 0xF;

	public SceKernelThreadEventHandlerInfo(String name, int thid, int mask, int handler, int common) {
		this.name = name;
		this.thid = thid;
		this.mask = mask;
        this.handler = handler;
        this.common = common;

		uid = SceUidManager.getNewUid(uidPurpose);
	}

	public void release() {
		SceUidManager.releaseUid(uid, uidPurpose);
		mask = 0;
		handler = 0;
	}

	public boolean hasEventMask(int event) {
		return (mask & event) == event;
	}

	public void triggerThreadEventHandler(SceKernelThreadInfo contextThread, int event) {
        Modules.ThreadManForUserModule.executeCallback(contextThread, handler, new AfterEventHandler(), false, event, thid, common);
    }

    private class AfterEventHandler implements IAction {
		@Override
		public void execute() {
			result = Emulator.getProcessor().cpu.gpr[2];

			if (Modules.log.isInfoEnabled()) {
				Modules.log.info(String.format("Thread Event Handler exit detected (thid=%X, result=0x%08X)", thid, result));
			}
		}
    }

	@Override
	protected void write() {
		super.write();
        writeStringNZ(32, name);
		write32(thid);
		write32(mask);
		write32(handler);
        write32(common);
	}
}