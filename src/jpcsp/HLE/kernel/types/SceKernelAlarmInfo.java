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

import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.interrupts.AlarmInterruptHandler;
import jpcsp.scheduler.AlarmInterruptAction;
import jpcsp.scheduler.AlarmInterruptResultAction;

public class SceKernelAlarmInfo extends pspAbstractMemoryMappedStructureVariableLength {
	private static final String uidPurpose = "ThreadMan-Alarm";
	public long schedule;
	public int handlerAddress;
	public int handlerArgument;

	public final int uid;
	public final AlarmInterruptHandler alarmInterruptHandler;
	public final AlarmInterruptAction alarmInterruptAction;
	public final AlarmInterruptResultAction alarmInterruptResultAction;

	public SceKernelAlarmInfo(long schedule, int handlerAddress, int handlerArgument) {
		this.schedule = schedule;
		this.handlerAddress = handlerAddress;
		this.handlerArgument = handlerArgument;

		uid = SceUidManager.getNewUid(uidPurpose);
		alarmInterruptHandler = new AlarmInterruptHandler(handlerAddress, handlerArgument);
		alarmInterruptAction = new AlarmInterruptAction(this);
		alarmInterruptResultAction = new AlarmInterruptResultAction(this);
	}

	@Override
	protected void read() {
		super.read();
		schedule = read64();
		handlerAddress = read32();
		handlerArgument = read32();
	}

	@Override
	protected void write() {
		super.write();
		write64(schedule);
		write32(handlerAddress);
		write32(handlerArgument);
	}

	public void delete() {
		SceUidManager.releaseUid(uid, uidPurpose);
	}
}
