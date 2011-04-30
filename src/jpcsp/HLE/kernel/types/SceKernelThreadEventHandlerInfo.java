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

public class SceKernelThreadEventHandlerInfo extends pspAbstractMemoryMappedStructure {
	public int size;
	public String name;
	public int thid;
	public int mask;
    public int handler;
    public int common;

    // Internal info.
	public final int uid;
    public int result;  // Return value from the handler's callback.

	private static final int DEFAULT_SIZE = 52;

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
		size = DEFAULT_SIZE;
		this.name = name;
		this.thid = thid;
		this.mask = mask;
        this.handler = handler;
        this.common = common;

		uid = SceUidManager.getNewUid("ThreadMan-ThreadEventHandler");
	}

    public boolean checkCreateMask() {
        return ((mask & THREAD_EVENT_CREATE) == THREAD_EVENT_CREATE);
    }

    public boolean checkStartMask() {
        return ((mask & THREAD_EVENT_START) == THREAD_EVENT_START);
    }

    public boolean checkExitMask() {
        return ((mask & THREAD_EVENT_EXIT) == THREAD_EVENT_EXIT);
    }

    public boolean checkDeleteMask() {
        return ((mask & THREAD_EVENT_DELETE) == THREAD_EVENT_DELETE);
    }

    public void triggerThreadEventHandler(int evt) {
        // Uses the current thread's (caller) context and uid.
        SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();

        if (thread != null) {
            Modules.ThreadManForUserModule.executeCallback(thread, handler, new AfterEventHandler(), true, evt, thread.uid, common);
        }
    }

    private class AfterEventHandler implements IAction {
			@Override
			public void execute() {
				result = Emulator.getProcessor().cpu.gpr[2];

				Modules.log.info("Thread Event Handler exit detected (thid="
                        + thid + ", result="
                        + Integer.toHexString(result) + ")");
			}
        }

	@Override
	protected void read() {
		size = read32();
		setMaxSize(size);
		name = readStringNZ(32);
		thid = read32();
		mask = read32();
        handler = read32();
        common = read32();
	}

	@Override
	protected void write() {
		setMaxSize(size);
		write32(size);
        writeStringNZ(32, name);
		write32(thid);
		write32(mask);
		write32(handler);
        write32(common);
	}

	@Override
	public int sizeof() {
		return size;
	}
}