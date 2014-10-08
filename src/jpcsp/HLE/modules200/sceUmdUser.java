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
package jpcsp.HLE.modules200;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_UMD_NOT_READY;
import jpcsp.Emulator;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;

@HLELogging
public class sceUmdUser extends jpcsp.HLE.modules150.sceUmdUser {
    private boolean umdAllowReplace;

    private static class DelayedUmdSwitch implements IAction {
		@Override
		public void execute() {
			Modules.sceUmdUserModule.hleDelayedUmdSwitch();
		}
    }

    public boolean isUmdAllowReplace() {
		return umdAllowReplace;
	}

	private void setUmdAllowReplace(boolean umdAllowReplace) {
		this.umdAllowReplace = umdAllowReplace;

		// Update the visibility of the "Switch UMD" menu item
		Emulator.getMainGUI().onUmdChange();
	}

	@Override
	public void start() {
		setUmdAllowReplace(false);

		super.start();
	}

	public void hleUmdSwitch() {
		// First notify that the UMD has been removed
		int notifyArg = getNotificationArg(false);
    	Modules.ThreadManForUserModule.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, notifyArg);

    	// After 100ms delay, notify that a new UMD has been inserted
    	long schedule = Emulator.getClock().microTime() + 100 * 1000;
    	Emulator.getScheduler().addAction(schedule, new DelayedUmdSwitch());
	}

	protected void hleDelayedUmdSwitch() {
		int notifyArg = getNotificationArg() | PSP_UMD_CHANGED;
    	Modules.ThreadManForUserModule.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_UMD, notifyArg);
	}

	@HLEFunction(nid = 0x87533940, version = 200)
    public int sceUmdReplaceProhibit() {
        if ((getUmdStat() & PSP_UMD_READY) != PSP_UMD_READY || (getUmdStat() & PSP_UMD_READABLE) != PSP_UMD_READABLE) {
            return ERROR_UMD_NOT_READY;
        }

        setUmdAllowReplace(false);

        return 0;
    }

    @HLEFunction(nid = 0xCBE9F02A, version = 200)
    public int sceUmdReplacePermit() {
    	setUmdAllowReplace(true);

        return 0;
    }
}