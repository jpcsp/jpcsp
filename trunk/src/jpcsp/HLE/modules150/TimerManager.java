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
package jpcsp.HLE.modules150;

import java.util.HashMap;
import java.util.Map;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.managers.SystemTimeManager;
import jpcsp.HLE.kernel.types.SceKernelAlarmInfo;
import jpcsp.HLE.kernel.types.SceKernelVTimerInfo;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NOT_FOUND_ALARM;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NOT_FOUND_VTIMER;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ILLEGAL_ADDR;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.scheduler.Scheduler;
import jpcsp.util.Utilities;

public class TimerManager implements HLEModule {

	@Override
	public String getName() {
		return "TimerManager";
	}

	@Override
	public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {
            mm.addFunction(sceKernelSetAlarmFunction, 0x6652B8CA);
            mm.addFunction(sceKernelSetSysClockAlarmFunction, 0xB2C25152);
            mm.addFunction(sceKernelCancelAlarmFunction, 0x7E65B999);
            mm.addFunction(sceKernelReferAlarmStatusFunction, 0xDAA3F564);
            mm.addFunction(sceKernelCreateVTimerFunction, 0x20FFF560);
            mm.addFunction(sceKernelDeleteVTimerFunction, 0x328F9E52);
            mm.addFunction(sceKernelGetVTimerBaseFunction, 0xB3A59970);
            mm.addFunction(sceKernelGetVTimerBaseWideFunction, 0xB7C18B77);
            mm.addFunction(sceKernelGetVTimerTimeFunction, 0x034A921F);
            mm.addFunction(sceKernelGetVTimerTimeWideFunction, 0xC0B3FFD2);
            mm.addFunction(sceKernelSetVTimerTimeFunction, 0x542AD630);
            mm.addFunction(sceKernelSetVTimerTimeWideFunction, 0xFB6425C3);
            mm.addFunction(sceKernelStartVTimerFunction, 0xC68D9437);
            mm.addFunction(sceKernelStopVTimerFunction, 0xD0AEEE87);
            mm.addFunction(sceKernelSetVTimerHandlerFunction, 0xD8B299AE);
            mm.addFunction(sceKernelSetVTimerHandlerWideFunction, 0x53B00E9A);
            mm.addFunction(sceKernelCancelVTimerHandlerFunction, 0xD2D615EF);
            mm.addFunction(sceKernelReferVTimerStatusFunction, 0x5F32BEAA);
        }

        alarms = new HashMap<Integer, SceKernelAlarmInfo>();
        vtimers = new HashMap<Integer, SceKernelVTimerInfo>();
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {
            mm.removeFunction(sceKernelSetAlarmFunction);
            mm.removeFunction(sceKernelSetSysClockAlarmFunction);
            mm.removeFunction(sceKernelCancelAlarmFunction);
            mm.removeFunction(sceKernelReferAlarmStatusFunction);
            mm.removeFunction(sceKernelCreateVTimerFunction);
            mm.removeFunction(sceKernelDeleteVTimerFunction);
            mm.removeFunction(sceKernelGetVTimerBaseFunction);
            mm.removeFunction(sceKernelGetVTimerBaseWideFunction);
            mm.removeFunction(sceKernelGetVTimerTimeFunction);
            mm.removeFunction(sceKernelGetVTimerTimeWideFunction);
            mm.removeFunction(sceKernelSetVTimerTimeFunction);
            mm.removeFunction(sceKernelSetVTimerTimeWideFunction);
            mm.removeFunction(sceKernelStartVTimerFunction);
            mm.removeFunction(sceKernelStopVTimerFunction);
            mm.removeFunction(sceKernelSetVTimerHandlerFunction);
            mm.removeFunction(sceKernelSetVTimerHandlerWideFunction);
            mm.removeFunction(sceKernelCancelVTimerHandlerFunction);
            mm.removeFunction(sceKernelReferVTimerStatusFunction);
        }

        alarms = null;
        vtimers = null;
	}

	protected static final int INTR_NUMBER = IntrManager.PSP_SYSTIMER0_INTR;
	protected Map<Integer, SceKernelAlarmInfo> alarms;
	protected Map<Integer, SceKernelVTimerInfo> vtimers;

	public void cancelAlarm(SceKernelAlarmInfo sceKernelAlarmInfo) {
		Scheduler.getInstance().removeAction(sceKernelAlarmInfo.schedule, sceKernelAlarmInfo.alarmInterruptAction);
		sceKernelAlarmInfo.schedule = 0;
	}

	public void rescheduleAlarm(SceKernelAlarmInfo sceKernelAlarmInfo, int delay) {
		if (delay < 0) {
			delay = 100;
		}

		sceKernelAlarmInfo.schedule += delay;
		scheduleAlarm(sceKernelAlarmInfo);

		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("New Schedule for Alarm uid=%x: %d", sceKernelAlarmInfo.uid, sceKernelAlarmInfo.schedule));
		}
	}

	private void scheduleAlarm(SceKernelAlarmInfo sceKernelAlarmInfo) {
		Scheduler.getInstance().addAction(sceKernelAlarmInfo.schedule, sceKernelAlarmInfo.alarmInterruptAction);
	}

	protected void hleKernelSetAlarm(Processor processor, long delayUsec, int handlerAddress, int handlerArgument) {
        CpuState cpu = processor.cpu;

        Scheduler scheduler = Scheduler.getInstance();
		long now = scheduler.getNow();
		long schedule = now + delayUsec;
        SceKernelAlarmInfo sceKernelAlarmInfo = new SceKernelAlarmInfo(schedule, handlerAddress, handlerArgument);
        alarms.put(sceKernelAlarmInfo.uid, sceKernelAlarmInfo);

        scheduleAlarm(sceKernelAlarmInfo);

        cpu.gpr[2] = sceKernelAlarmInfo.uid;
	}

	protected long getSystemTime() {
		return SystemTimeManager.getSystemTime();
	}

	public long getVTimerTime(SceKernelVTimerInfo sceKernelVTimerInfo) {
		long time = sceKernelVTimerInfo.current;

		if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_RUNNING) {
			time += getSystemTime() - sceKernelVTimerInfo.base;
		}

		return time;
	}

	protected long getVTimerScheduleForScheduler(SceKernelVTimerInfo sceKernelVTimerInfo) {
		return sceKernelVTimerInfo.base + sceKernelVTimerInfo.schedule;
	}

	protected void setVTimer(SceKernelVTimerInfo sceKernelVTimerInfo, long time) {
		sceKernelVTimerInfo.current = time;
	}

	protected void startVTimer(SceKernelVTimerInfo sceKernelVTimerInfo) {
		sceKernelVTimerInfo.active = SceKernelVTimerInfo.ACTIVE_RUNNING;
		sceKernelVTimerInfo.base = getSystemTime();

		if (sceKernelVTimerInfo.schedule != 0 && sceKernelVTimerInfo.handlerAddress != 0) {
			scheduleVTimer(sceKernelVTimerInfo, sceKernelVTimerInfo.schedule);
		}
	}

	protected void stopVTimer(SceKernelVTimerInfo sceKernelVTimerInfo) {
		sceKernelVTimerInfo.active = SceKernelVTimerInfo.ACTIVE_STOPPED;
		sceKernelVTimerInfo.current = getSystemTime() - sceKernelVTimerInfo.base;
	}

	protected void scheduleVTimer(SceKernelVTimerInfo sceKernelVTimerInfo, long schedule) {
		if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_RUNNING) {
			sceKernelVTimerInfo.schedule = schedule;

			Scheduler scheduler = Scheduler.getInstance();
			scheduler.addAction(getVTimerScheduleForScheduler(sceKernelVTimerInfo), sceKernelVTimerInfo.vtimerInterruptAction);
		}
	}

	public void cancelVTimer(SceKernelVTimerInfo sceKernelVTimerInfo) {
		Scheduler.getInstance().removeAction(getVTimerScheduleForScheduler(sceKernelVTimerInfo), sceKernelVTimerInfo.vtimerInterruptAction);
		sceKernelVTimerInfo.schedule = 0;
		sceKernelVTimerInfo.handlerAddress = 0;
		sceKernelVTimerInfo.handlerArgument = 0;
	}

	public void rescheduleVTimer(SceKernelVTimerInfo sceKernelVTimerInfo, int delay) {
		if (delay < 0) {
			delay = 100;
		}

		sceKernelVTimerInfo.schedule += delay;

		scheduleVTimer(sceKernelVTimerInfo, delay);

		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug(String.format("New Schedule for VTimer uid=%x: %d", sceKernelVTimerInfo.uid, sceKernelVTimerInfo.schedule));
		}
	}

	/**
	 * Set an alarm.
	 * @param delayUsec - The number of micro seconds till the alarm occurs.
	 * @param handlerAddress - Pointer to a ::SceKernelAlarmHandler
	 * @param handlerArgument - Common pointer for the alarm handler
	 *
	 * @return A UID representing the created alarm, < 0 on error.
	 */
	public void sceKernelSetAlarm(Processor processor) {
        CpuState cpu = processor.cpu;

        int delayUsec = cpu.gpr[4];
        int handlerAddress = cpu.gpr[5];
        int handlerArgument = cpu.gpr[6];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelSetAlarm(%d,0x%08X,0x%08X)", delayUsec, handlerAddress, handlerArgument));
        }

        hleKernelSetAlarm(processor, delayUsec, handlerAddress, handlerArgument);
    }

	/**
	 * Set an alarm using a ::SceKernelSysClock structure for the time
	 *
	 * @param delaySysclockAddr - Pointer to a ::SceKernelSysClock structure
	 * @param handlerAddress - Pointer to a ::SceKernelAlarmHandler
	 * @param handlerArgument - Common pointer for the alarm handler.
	 *
	 * @return A UID representing the created alarm, < 0 on error.
	 */
	public void sceKernelSetSysClockAlarm(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int delaySysclockAddr = cpu.gpr[4];
        int handlerAddress = cpu.gpr[5];
        int handlerArgument = cpu.gpr[6];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelSetSysClockAlarm(0x%08X,0x%08X,0x%08X)", delaySysclockAddr, handlerAddress, handlerArgument));
        }

        if (mem.isAddressGood(delaySysclockAddr)) {
        	long delaySysclock = mem.read64(delaySysclockAddr);
        	long delayUsec = SystemTimeManager.hleSysClock2USec(delaySysclock);

        	hleKernelSetAlarm(processor, delayUsec, handlerAddress, handlerArgument);
        } else {
        	cpu.gpr[2] = ERROR_ILLEGAL_ADDR;
        }
	}

	/**
	 * Cancel a pending alarm.
	 *
	 * @param alarmUid - UID of the alarm to cancel.
	 *
	 * @return 0 on success, < 0 on error.
	 */
    public void sceKernelCancelAlarm(Processor processor) {
        CpuState cpu = processor.cpu;

        int alarmUid = cpu.gpr[4];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelCancelAlarm(uid=0x%x)", alarmUid));
        }

    	SceKernelAlarmInfo sceKernelAlarmInfo = alarms.get(alarmUid);
        if (sceKernelAlarmInfo == null) {
        	Modules.log.warn(String.format("sceKernelCancelAlarm unknown uid=0x%x", alarmUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_ALARM;
        } else {
        	cancelAlarm(sceKernelAlarmInfo);
        	cpu.gpr[2] = 0;
        }
    }

    /**
     * Refer the status of a created alarm.
     *
     * @param alarmUid - UID of the alarm to get the info of
     * @param infoAddr - Pointer to a ::SceKernelAlarmInfo structure
     *
     * @return 0 on success, < 0 on error.
     */
    public void sceKernelReferAlarmStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int alarmUid = cpu.gpr[4];
        int infoAddr = cpu.gpr[5];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelReferAlarmStatus(uid=0x%x, infoAddr=0x%08X)", alarmUid, infoAddr));
        }

    	SceKernelAlarmInfo sceKernelAlarmInfo = alarms.get(alarmUid);
        if (sceKernelAlarmInfo == null) {
        	Modules.log.warn(String.format("sceKernelReferAlarmStatus unknown uid=0x%x", alarmUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_ALARM;
        } else if (!mem.isAddressGood(infoAddr)) {
        	cpu.gpr[2] = ERROR_ILLEGAL_ADDR;
        } else {
        	int size = mem.read32(infoAddr);
        	sceKernelAlarmInfo.size = size;
        	sceKernelAlarmInfo.write(mem, infoAddr);
        	cpu.gpr[2] = 0;
        }
    }

    /**
     * Create a virtual timer
     *
     * @param nameAddr - Name for the timer.
     * @param optAddr  - Pointer to an ::SceKernelVTimerOptParam (pass NULL)
     *
     * @return The VTimer's UID or < 0 on error.
     */
    public void sceKernelCreateVTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int nameAddr = cpu.gpr[4];
        int optAddr = cpu.gpr[5];
        String name = Utilities.readStringZ(nameAddr);
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelCreateVTimer(name=%s(0x%08X), optAddr=0x%08X)", name, nameAddr, optAddr));
        }

        SceKernelVTimerInfo sceKernelVTimerInfo = new SceKernelVTimerInfo(name);
        vtimers.put(sceKernelVTimerInfo.uid, sceKernelVTimerInfo);

        cpu.gpr[2] = sceKernelVTimerInfo.uid;
    }

    /**
     * Delete a virtual timer
     *
     * @param vtimerUid - The UID of the timer
     *
     * @return < 0 on error.
     */
    public void sceKernelDeleteVTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelDeleteVTimer(uid=0x%x)", vtimerUid));
        }

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.remove(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelDeleteVTimer unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else {
        	sceKernelVTimerInfo.delete();
        	cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the timer base
     *
     * @param vtimerUid - UID of the vtimer
     * @param baseAddr - Pointer to a ::SceKernelSysClock structure
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelGetVTimerBase(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int vtimerUid = cpu.gpr[4];
        int baseAddr = cpu.gpr[5];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelGetVTimerBase(uid=0x%x,baseAddr=0x%08X)", vtimerUid, baseAddr));
        }

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelGetVTimerBase unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else if (!mem.isAddressGood(baseAddr)) {
        	cpu.gpr[2] = ERROR_ILLEGAL_ADDR;
        } else {
        	mem.write64(baseAddr, sceKernelVTimerInfo.base);
        	cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the timer base (wide format)
     *
     * @param vtimerUid - UID of the vtimer
     *
     * @return The 64bit timer base
     */
    public void sceKernelGetVTimerBaseWide(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelGetVTimerBaseWide(uid=0x%x)", vtimerUid));
        }

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelGetVTimerBaseWide unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else {
        	Utilities.returnRegister64(cpu, sceKernelVTimerInfo.base);
        }
    }

    /**
     * Get the timer time
     *
     * @param vtimerUid - UID of the vtimer
     * @param timeAddr - Pointer to a ::SceKernelSysClock structure
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelGetVTimerTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int vtimerUid = cpu.gpr[4];
        int timeAddr = cpu.gpr[5];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelGetVTimerTime(uid=0x%x,timeAddr=0x%08X)", vtimerUid, timeAddr));
        }

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelGetVTimerTime unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else if (!mem.isAddressGood(timeAddr)) {
        	cpu.gpr[2] = ERROR_ILLEGAL_ADDR;
        } else {
        	long time = getVTimerTime(sceKernelVTimerInfo);
            if (Modules.log.isDebugEnabled()) {
            	Modules.log.debug(String.format("sceKernelGetVTimerTime returning %d", time));
            }
        	mem.write64(timeAddr, time);
        	cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the timer time (wide format)
     *
     * @param vtimerUid - UID of the vtimer
     *
     * @return The 64bit timer time
     */
    public void sceKernelGetVTimerTimeWide(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelGetVTimerTimeWide(uid=0x%x)", vtimerUid));
        }

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelGetVTimerTimeWide unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else {
        	long time = getVTimerTime(sceKernelVTimerInfo);
            if (Modules.log.isDebugEnabled()) {
            	Modules.log.debug(String.format("sceKernelGetVTimerTimeWide returning %d", time));
            }
        	Utilities.returnRegister64(cpu, time);
        }
    }

    /**
     * Set the timer time
     *
     * @param vtimerUid - UID of the vtimer
     * @param timeAddr - Pointer to a ::SceKernelSysClock structure
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelSetVTimerTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int vtimerUid = cpu.gpr[4];
        int timeAddr = cpu.gpr[5];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelSetVTimerTime(uid=0x%x,timeAddr=0x%08X)", vtimerUid, timeAddr));
        }

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelSetVTimerTime unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else if (!mem.isAddressGood(timeAddr)) {
        	cpu.gpr[2] = ERROR_ILLEGAL_ADDR;
        } else {
        	long time = mem.read64(timeAddr);
        	setVTimer(sceKernelVTimerInfo, time);
        	cpu.gpr[2] = 0;
        }
    }

    /**
     * Set the timer time (wide format)
     *
     * @param vtimerUid - UID of the vtimer
     * @param time - a ::SceKernelSysClock structure
     *
     * @return Possibly the last time
     */
    public void sceKernelSetVTimerTimeWide(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        // cpu.gpr[5] not used!
        long time = Utilities.getRegister64(cpu, 6);
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelSetVTimerTime(uid=0x%x,time=0x%016X)", vtimerUid, time));
        }

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelSetVTimerTime unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else {
        	setVTimer(sceKernelVTimerInfo, time);
        	cpu.gpr[2] = 0;
        }
    }

    /**
     * Start a virtual timer
     *
     * @param vtimerUid - The UID of the timer
     *
     * @return < 0 on error
     */
    public void sceKernelStartVTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelStartVTimer(uid=0x%x)", vtimerUid));
        }

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelStartVTimer unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else {
        	if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_RUNNING) {
        		cpu.gpr[2] = 1; // already started
        	} else {
        		startVTimer(sceKernelVTimerInfo);
        		cpu.gpr[2] = 0;
        	}
        }
    }

    /**
     * Stop a virtual timer
     *
     * @param vtimerUid - The UID of the timer
     *
     * @return < 0 on error
     */
    public void sceKernelStopVTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelStopVTimer(uid=0x%x)", vtimerUid));
        }

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelStopVTimer unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else {
        	if (sceKernelVTimerInfo.active == SceKernelVTimerInfo.ACTIVE_STOPPED) {
        		cpu.gpr[2] = 0; // already stopped
        	} else {
        		stopVTimer(sceKernelVTimerInfo);
        		cpu.gpr[2] = 1;
        	}
        }
    }

    /**
     * Set the timer handler
     *
     * @param vtimerUid - UID of the vtimer
     * @param scheduleAddr - Time to call the handler
     * @param handlerAddress - The timer handler
     * @param handlerArgument  - Common pointer
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelSetVTimerHandler(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int vtimerUid = cpu.gpr[4];
        int scheduleAddr = cpu.gpr[5];
        int handlerAddress = cpu.gpr[6];
        int handlerArgument = cpu.gpr[7];
    	Modules.log.warn(String.format("NOT TESTED: sceKernelSetVTimerHandler(uid=0x%x,scheduleAddr=0x%08X,handlerAddress=0x%08X,handlerArgument=0x%08X)", vtimerUid, scheduleAddr, handlerAddress, handlerArgument));

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelSetVTimerHandler unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else if (!mem.isAddressGood(scheduleAddr)) {
        	cpu.gpr[2] = ERROR_ILLEGAL_ADDR;
        } else {
        	long schedule = mem.read64(scheduleAddr);
        	sceKernelVTimerInfo.handlerAddress = handlerAddress;
        	sceKernelVTimerInfo.handlerArgument = handlerArgument;
        	if (handlerAddress != 0) {
        		scheduleVTimer(sceKernelVTimerInfo, schedule);
        	}
        	cpu.gpr[2] = 0;
        }
    }

    /**
     * Set the timer handler (wide mode)
     *
     * @param vtimerUid - UID of the vtimer
     * @param schedule - Time to call the handler
     * @param handlerAddress - The timer handler
     * @param handlerArgument  - Common pointer
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelSetVTimerHandlerWide(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        // cpu.gpr[5] not used!
        long schedule = Utilities.getRegister64(cpu, 6);
        int handlerAddress = cpu.gpr[8];
        int handlerArgument = cpu.gpr[9];
    	Modules.log.debug(String.format("NOT TESTED: sceKernelSetVTimerHandlerWide(uid=0x%x,schedule=0x%016X,handlerAddress=0x%08X,handlerArgument=0x%08X)", vtimerUid, schedule, handlerAddress, handlerArgument));

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelSetVTimerHandler unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else {
        	sceKernelVTimerInfo.handlerAddress = handlerAddress;
        	sceKernelVTimerInfo.handlerArgument = handlerArgument;
        	if (handlerAddress != 0) {
        		scheduleVTimer(sceKernelVTimerInfo, schedule);
        	}
        	cpu.gpr[2] = 0;
        }
    }

    /**
     * Cancel the timer handler
     *
     * @param vtimerUid - The UID of the vtimer
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelCancelVTimerHandler(Processor processor) {
        CpuState cpu = processor.cpu;

        int vtimerUid = cpu.gpr[4];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelCancelVTimerHandler(uid=0x%x)", vtimerUid));
        }

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelCancelVTimerHandler unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else {
        	cancelVTimer(sceKernelVTimerInfo);
        	cpu.gpr[2] = 0;
        }
    }

    /**
     * Get the status of a VTimer
     *
     * @param vtimerUid - The uid of the VTimer
     * @param infoAddr - Pointer to a ::SceKernelVTimerInfo structure
     *
     * @return 0 on success, < 0 on error
     */
    public void sceKernelReferVTimerStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int vtimerUid = cpu.gpr[4];
        int infoAddr = cpu.gpr[5];
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceKernelReferVTimerStatus(uid=0x%x,infoAddr=0x%08X)", vtimerUid, infoAddr));
        }

    	SceKernelVTimerInfo sceKernelVTimerInfo = vtimers.get(vtimerUid);
        if (sceKernelVTimerInfo == null) {
        	Modules.log.warn(String.format("sceKernelReferVTimerStatus unknown uid=0x%x", vtimerUid));
        	cpu.gpr[2] = ERROR_NOT_FOUND_VTIMER;
        } else if (!mem.isAddressGood(infoAddr)) {
        	cpu.gpr[2] = ERROR_ILLEGAL_ADDR;
        } else {
        	int size = mem.read32(infoAddr);
        	sceKernelVTimerInfo.size = size;
        	sceKernelVTimerInfo.write(mem, infoAddr);
        	cpu.gpr[2] = 0;
        }
    }

    public final HLEModuleFunction sceKernelSetAlarmFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetAlarm") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetAlarm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelSetAlarm(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSetSysClockAlarmFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetSysClockAlarm") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetSysClockAlarm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelSetSysClockAlarm(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCancelAlarmFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelAlarm") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelAlarm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelCancelAlarm(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferAlarmStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferAlarmStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferAlarmStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelReferAlarmStatus(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCreateVTimerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCreateVTimer") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCreateVTimer(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelCreateVTimer(processor);";
        }
    };
    public final HLEModuleFunction sceKernelDeleteVTimerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelDeleteVTimer") {

        @Override
        public final void execute(Processor processor) {
            sceKernelDeleteVTimer(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelDeleteVTimer(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetVTimerBaseFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetVTimerBase") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetVTimerBase(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelGetVTimerBase(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetVTimerBaseWideFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetVTimerBaseWide") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetVTimerBaseWide(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelGetVTimerBaseWide(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetVTimerTimeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetVTimerTime") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetVTimerTime(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelGetVTimerTime(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetVTimerTimeWideFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelGetVTimerTimeWide") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetVTimerTimeWide(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelGetVTimerTimeWide(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSetVTimerTimeFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetVTimerTime") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetVTimerTime(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelSetVTimerTime(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSetVTimerTimeWideFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetVTimerTimeWide") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetVTimerTimeWide(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelSetVTimerTimeWide(processor);";
        }
    };
    public final HLEModuleFunction sceKernelStartVTimerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelStartVTimer") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStartVTimer(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelStartVTimer(processor);";
        }
    };
    public final HLEModuleFunction sceKernelStopVTimerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelStopVTimer") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStopVTimer(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelStopVTimer(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSetVTimerHandlerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetVTimerHandler") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetVTimerHandler(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelSetVTimerHandler(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSetVTimerHandlerWideFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelSetVTimerHandlerWide") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSetVTimerHandlerWide(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelSetVTimerHandlerWide(processor);";
        }
    };
    public final HLEModuleFunction sceKernelCancelVTimerHandlerFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelCancelVTimerHandler") {

        @Override
        public final void execute(Processor processor) {
            sceKernelCancelVTimerHandler(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelCancelVTimerHandler(processor);";
        }
    };
    public final HLEModuleFunction sceKernelReferVTimerStatusFunction = new HLEModuleFunction("ThreadManForUser", "sceKernelReferVTimerStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelReferVTimerStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.TimerManager.sceKernelReferVTimerStatus(processor);";
        }
    };
}
