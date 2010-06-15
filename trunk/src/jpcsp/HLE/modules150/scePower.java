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

import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.Modules;
import jpcsp.hardware.Battery;

import jpcsp.Processor;

import jpcsp.Allegrex.CpuState;

public class scePower implements HLEModule {
	@Override
	public String getName() { return "scePower"; }

	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.addFunction(scePower_2B51FE2FFunction, 0x2B51FE2F);
			mm.addFunction(scePowerGetBacklightMaximumFunction, 0x442BFBAC);
			mm.addFunction(scePowerTickFunction, 0xEFD3C963);
			mm.addFunction(scePowerGetIdleTimerFunction, 0xEDC13FE5);
			mm.addFunction(scePowerIdleTimerEnableFunction, 0x7F30B3B1);
			mm.addFunction(scePowerIdleTimerDisableFunction, 0x972CE941);
			mm.addFunction(scePowerBatteryUpdateInfoFunction, 0x27F3292C);
			mm.addFunction(scePowerGetForceSuspendCapacityFunction, 0xE8E4E204);
			mm.addFunction(scePowerGetLowBatteryCapacityFunction, 0xB999184C);
			mm.addFunction(scePowerIsPowerOnlineFunction, 0x87440F5E);
			mm.addFunction(scePowerIsBatteryExistFunction, 0x0AFD0D8B);
			mm.addFunction(scePowerIsBatteryChargingFunction, 0x1E490401);
			mm.addFunction(scePowerGetBatteryChargingStatusFunction, 0xB4432BC8);
			mm.addFunction(scePowerIsLowBatteryFunction, 0xD3075926);
			mm.addFunction(scePowerIsSuspendRequiredFunction, 0x78A1A796);
			mm.addFunction(scePowerGetBatteryRemainCapacityFunction, 0x94F5A53F);
			mm.addFunction(scePowerGetBatteryFullCapacityFunction, 0xFD18A0FF);
			mm.addFunction(scePowerGetBatteryLifePercentFunction, 0x2085D15D);
			mm.addFunction(scePowerGetBatteryLifeTimeFunction, 0x8EFB3FA2);
			mm.addFunction(scePowerGetBatteryTempFunction, 0x28E12023);
			mm.addFunction(scePowerGetBatteryElecFunction, 0x862AE1A6);
			mm.addFunction(scePowerGetBatteryVoltFunction, 0x483CE86B);
			mm.addFunction(scePower_23436A4AFunction, 0x23436A4A);
			mm.addFunction(scePowerSetPowerSwModeFunction, 0x0CD21B1F);
			mm.addFunction(scePowerGetPowerSwModeFunction, 0x165CE085);
			mm.addFunction(scePowerVolatileMemLockFunction, 0x23C31FFE);
			mm.addFunction(scePowerVolatileMemTryLockFunction, 0xFA97A599);
			mm.addFunction(scePowerVolatileMemUnlockFunction, 0xB3EDD801);
			mm.addFunction(scePowerLockFunction, 0xD6D016EF);
			mm.addFunction(scePowerUnlockFunction, 0xCA3D34C1);
			mm.addFunction(scePowerCancelRequestFunction, 0xDB62C9CF);
			mm.addFunction(scePowerIsRequestFunction, 0x7FA406DD);
			mm.addFunction(scePowerRequestStandbyFunction, 0x2B7C7CF4);
			mm.addFunction(scePowerRequestSuspendFunction, 0xAC32C9CC);
			mm.addFunction(scePower_2875994BFunction, 0x2875994B);
			mm.addFunction(scePowerWaitRequestCompletionFunction, 0x3951AF53);
			mm.addFunction(scePowerGetResumeCountFunction, 0x0074EF9B);
			mm.addFunction(scePowerRegisterCallbackFunction, 0x04B7766E);
			mm.addFunction(scePowerUnregisterCallbackFunction, 0xDFA8BAF8);
			mm.addFunction(scePowerUnregitserCallbackFunction, 0xDB9D28DD);
			mm.addFunction(scePowerSetCpuClockFrequencyFunction, 0x843FBF43);
			mm.addFunction(scePowerSetBusClockFrequencyFunction, 0xB8D7B3FB);
			mm.addFunction(scePowerGetCpuClockFrequencyFunction, 0xFEE03A2F);
			mm.addFunction(scePowerGetBusClockFrequencyFunction, 0x478FE6F5);
			mm.addFunction(scePowerGetCpuClockFrequencyIntFunction, 0xFDB5BFE9);
			mm.addFunction(scePowerGetBusClockFrequencyIntFunction, 0xBD681969);
			mm.addFunction(scePowerGetPllClockFrequencyIntFunction, 0x34F9C463);
			mm.addFunction(scePowerGetCpuClockFrequencyFloatFunction, 0xB1A52C83);
			mm.addFunction(scePowerGetBusClockFrequencyFloatFunction, 0x9BADB3EB);
			mm.addFunction(scePowerGetPllClockFrequencyFloatFunction, 0xEA382A27);
			mm.addFunction(scePowerSetClockFrequencyFunction, 0x737486F2);
            mm.addFunction(scePower_EBD177D6Function, 0xEBD177D6);

		}
	}

	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {

			mm.removeFunction(scePower_2B51FE2FFunction);
			mm.removeFunction(scePowerGetBacklightMaximumFunction);
			mm.removeFunction(scePowerTickFunction);
			mm.removeFunction(scePowerGetIdleTimerFunction);
			mm.removeFunction(scePowerIdleTimerEnableFunction);
			mm.removeFunction(scePowerIdleTimerDisableFunction);
			mm.removeFunction(scePowerBatteryUpdateInfoFunction);
			mm.removeFunction(scePowerGetForceSuspendCapacityFunction);
			mm.removeFunction(scePowerGetLowBatteryCapacityFunction);
			mm.removeFunction(scePowerIsPowerOnlineFunction);
			mm.removeFunction(scePowerIsBatteryExistFunction);
			mm.removeFunction(scePowerIsBatteryChargingFunction);
			mm.removeFunction(scePowerGetBatteryChargingStatusFunction);
			mm.removeFunction(scePowerIsLowBatteryFunction);
			mm.removeFunction(scePowerIsSuspendRequiredFunction);
			mm.removeFunction(scePowerGetBatteryRemainCapacityFunction);
			mm.removeFunction(scePowerGetBatteryFullCapacityFunction);
			mm.removeFunction(scePowerGetBatteryLifePercentFunction);
			mm.removeFunction(scePowerGetBatteryLifeTimeFunction);
			mm.removeFunction(scePowerGetBatteryTempFunction);
			mm.removeFunction(scePowerGetBatteryElecFunction);
			mm.removeFunction(scePowerGetBatteryVoltFunction);
			mm.removeFunction(scePower_23436A4AFunction);
			mm.removeFunction(scePowerSetPowerSwModeFunction);
			mm.removeFunction(scePowerGetPowerSwModeFunction);
			mm.removeFunction(scePowerVolatileMemLockFunction);
			mm.removeFunction(scePowerVolatileMemTryLockFunction);
			mm.removeFunction(scePowerVolatileMemUnlockFunction);
			mm.removeFunction(scePowerLockFunction);
			mm.removeFunction(scePowerUnlockFunction);
			mm.removeFunction(scePowerCancelRequestFunction);
			mm.removeFunction(scePowerIsRequestFunction);
			mm.removeFunction(scePowerRequestStandbyFunction);
			mm.removeFunction(scePowerRequestSuspendFunction);
			mm.removeFunction(scePower_2875994BFunction);
			mm.removeFunction(scePowerWaitRequestCompletionFunction);
			mm.removeFunction(scePowerGetResumeCountFunction);
			mm.removeFunction(scePowerRegisterCallbackFunction);
			mm.removeFunction(scePowerUnregisterCallbackFunction);
			mm.removeFunction(scePowerUnregitserCallbackFunction);
			mm.removeFunction(scePowerSetCpuClockFrequencyFunction);
			mm.removeFunction(scePowerSetBusClockFrequencyFunction);
			mm.removeFunction(scePowerGetCpuClockFrequencyFunction);
			mm.removeFunction(scePowerGetBusClockFrequencyFunction);
			mm.removeFunction(scePowerGetCpuClockFrequencyIntFunction);
			mm.removeFunction(scePowerGetBusClockFrequencyIntFunction);
			mm.removeFunction(scePowerGetPllClockFrequencyIntFunction);
			mm.removeFunction(scePowerGetCpuClockFrequencyFloatFunction);
			mm.removeFunction(scePowerGetBusClockFrequencyFloatFunction);
			mm.removeFunction(scePowerGetPllClockFrequencyFloatFunction);
			mm.removeFunction(scePowerSetClockFrequencyFunction);
            mm.removeFunction(scePower_EBD177D6Function);

		}
	}

	/**
	 * Power callback flags
	 */
	// indicates the power switch it pushed, putting the unit into suspend mode
	public static final int PSP_POWER_CB_POWER_SWITCH    = 0x80000000;
	// indicates the hold switch is on
	public static final int PSP_POWER_CB_HOLD_SWITCH     = 0x40000000;
	// what is standby mode?
	public static final int PSP_POWER_CB_STANDBY         = 0x00080000;
	// indicates the resume process has been completed (only seems to be triggered when another event happens)
	public static final int PSP_POWER_CB_RESUME_COMPLETE = 0x00040000;
	// indicates the unit is resuming from suspend mode
	public static final int PSP_POWER_CB_RESUMING        = 0x00020000;
	// indicates the unit is suspending, seems to occur due to inactivity
	public static final int PSP_POWER_CB_SUSPENDING      = 0x00010000;
	// indicates the unit is plugged into an AC outlet
	public static final int PSP_POWER_CB_AC_POWER        = 0x00001000;
	// indicates the battery charge level is low
	public static final int PSP_POWER_CB_BATTERY_LOW     = 0x00000100;
	// indicates there is a battery present in the unit
	public static final int PSP_POWER_CB_BATTERY_EXIST   = 0x00000080;
	// unknown
	public static final int PSP_POWER_CB_BATTPOWER       = 0x0000007F;

	private int cpuClock = 222;
    private int busClock = 111;

    private static final int backlightMaximum = 4;

	public void scePower_2B51FE2F(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePower_2B51FE2F [0x2B51FE2F]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerGetBacklightMaximum(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerGetBacklightMaximum backlightMaxium=" + backlightMaximum);

		cpu.gpr[2] = backlightMaximum;
	}

	public void scePowerTick(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("Unimplemented NID function scePowerTick [0xEFD3C963]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerGetIdleTimer(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerGetIdleTimer [0xEDC13FE5]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerIdleTimerEnable(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerIdleTimerEnable [0x7F30B3B1]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerIdleTimerDisable(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerIdleTimerDisable [0x972CE941]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerBatteryUpdateInfo(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerBatteryUpdateInfo [0x27F3292C]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerGetForceSuspendCapacity(Processor processor) {
		CpuState cpu = processor.cpu;

		int forceSuspendCapacity = (Battery.getForceSuspendPercent() * Battery.getFullCapacity()) / 100;
		Modules.log.debug("scePowerGetForceSuspendCapacity " + forceSuspendCapacity + "mAh");

		cpu.gpr[2] = forceSuspendCapacity;
	}

	public void scePowerGetLowBatteryCapacity(Processor processor) {
		CpuState cpu = processor.cpu;

		int lowBatteryCapacity = (Battery.getLowPercent() * Battery.getFullCapacity()) / 100;
		Modules.log.debug("scePowerGetLowBatteryCapacity " + lowBatteryCapacity + "mAh");

		cpu.gpr[2] = lowBatteryCapacity;
	}

	public void scePowerIsPowerOnline(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerIsPowerOnline pluggedIn=" + Battery.isPluggedIn());

		cpu.gpr[2] = Battery.isPluggedIn() ? 1 : 0;
	}

	public void scePowerIsBatteryExist(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerIsBatteryExist batteryPresent=" + Battery.isPresent());

		cpu.gpr[2] = Battery.isPresent() ? 1 : 0;
	}

	public void scePowerIsBatteryCharging(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerIsBatteryCharging batteryCharging=" + Battery.isCharging());

		cpu.gpr[2] = Battery.isCharging() ? 1 : 0;
	}

	public void scePowerGetBatteryChargingStatus(Processor processor) {
		CpuState cpu = processor.cpu;

		int status = 0;
		if (Battery.isPresent()) {
			status |= PSP_POWER_CB_BATTERY_EXIST;
		}
		if (Battery.isPluggedIn()) {
			status |= PSP_POWER_CB_AC_POWER;
		}
		if (Battery.isCharging()) {
			// I don't know exactly what to return under PSP_POWER_CB_BATTPOWER
			status |= PSP_POWER_CB_BATTPOWER;
		}

		Modules.log.debug("scePowerGetBatteryChargingStatus status=0x" + Integer.toHexString(status));

		cpu.gpr[2] = status;
	}

	public void scePowerIsLowBattery(Processor processor) {
		CpuState cpu = processor.cpu;

		int isLow = (Battery.getCurrentPowerPercent() <= Battery.getLowPercent()) ? 1 : 0;
		Modules.log.debug("scePowerIsLowBattery " + isLow);

		cpu.gpr[2] = isLow;
	}

	/**
	 * Check if suspend is requided
	 *
	 * @note: This function return 1 only when
	 * the battery charge is low and
	 * go in suspend mode!
	 *
	 * @return 1 if suspend is requided, otherwise 0
	 */
	public void scePowerIsSuspendRequired(Processor processor) {
		CpuState cpu = processor.cpu;

		int isSuspendRequired = (Battery.getCurrentPowerPercent() <= Battery.getForceSuspendPercent() ? 1 : 0);
		Modules.log.debug("scePowerIsSuspendRequired isSuspendRequired=" + isSuspendRequired);

		cpu.gpr[2] = isSuspendRequired;
	}

	public void scePowerGetBatteryRemainCapacity(Processor processor) {
		CpuState cpu = processor.cpu;

		int batteryRemainCapacity = (Battery.getCurrentPowerPercent() * Battery.getFullCapacity()) / 100;
		Modules.log.debug("scePowerGetBatteryRemainCapacity " + batteryRemainCapacity + "mAh");

		cpu.gpr[2] = batteryRemainCapacity;
	}

	public void scePowerGetBatteryFullCapacity(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerGetBatteryFullCapacity " + Battery.getFullCapacity() + "mAh");

		cpu.gpr[2] = Battery.getFullCapacity();
	}

	public void scePowerGetBatteryLifePercent(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerGetBatteryLifePercent percent=" + Battery.getCurrentPowerPercent());

		cpu.gpr[2] = Battery.getCurrentPowerPercent();
	}

	public void scePowerGetBatteryLifeTime(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerGetBatteryLifeTime batteryLifeTime=" + Battery.getLifeTime());

		cpu.gpr[2] = Battery.getLifeTime();
	}

	public void scePowerGetBatteryTemp(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerGetBatteryTemp batteryTemp=" + Battery.getTemperature());

		cpu.gpr[2] = Battery.getTemperature();
	}

	public void scePowerGetBatteryElec(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerGetBatteryElec [0x862AE1A6]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerGetBatteryVolt(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerGetBatteryVolt batteryVoltage=" + Battery.getVoltage());

		cpu.gpr[2] = Battery.getVoltage();
	}

	public void scePower_23436A4A(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePower_23436A4A [0x23436A4A]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerSetPowerSwMode(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerSetPowerSwMode [0x0CD21B1F]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerGetPowerSwMode(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerGetPowerSwMode [0x165CE085]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerVolatileMemLock(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerVolatileMemLock [0x23C31FFE]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerVolatileMemTryLock(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerVolatileMemTryLock [0xFA97A599]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerVolatileMemUnlock(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerVolatileMemUnlock [0xB3EDD801]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerLock(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerLock [0xD6D016EF]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerUnlock(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerUnlock [0xCA3D34C1]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerCancelRequest(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerCancelRequest [0xDB62C9CF]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerIsRequest(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerIsRequest [0x7FA406DD]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerRequestStandby(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerRequestStandby [0x2B7C7CF4]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerRequestSuspend(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerRequestSuspend [0xAC32C9CC]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePower_2875994B(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePower_2875994B [0x2875994B]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerWaitRequestCompletion(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerWaitRequestCompletion [0x3951AF53]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerGetResumeCount(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerGetResumeCount [0x0074EF9B]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerRegisterCallback(Processor processor) {
		CpuState cpu = processor.cpu;

        int slot = cpu.gpr[4];
        int uid = cpu.gpr[5];

        Modules.log.warn("UNIMPLEMENTED:scePowerRegisterCallback slot=" + slot + " SceUID=" + Integer.toHexString(uid));

		cpu.gpr[2] = 0; // Return success for now
	}

	public void scePowerUnregisterCallback(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerUnregisterCallback [0xDFA8BAF8]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerUnregitserCallback(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerUnregitserCallback [0xDB9D28DD]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerSetCpuClockFrequency(Processor processor) {
		CpuState cpu = processor.cpu;

		cpuClock = cpu.gpr[4];

		Modules.log.debug("scePowerSetCpuClockFrequency : " + cpuClock);

		cpu.gpr[2] = 0;
	}

	public void scePowerSetBusClockFrequency(Processor processor) {
		CpuState cpu = processor.cpu;

		busClock = cpu.gpr[4];

		Modules.log.debug("scePowerSetBusClockFrequency : " + busClock);

		cpu.gpr[2] = 0;
	}

	public void scePowerGetCpuClockFrequency(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerGetCpuClockFrequency ret:" + cpuClock);

		cpu.gpr[2] = cpuClock;
	}

	public void scePowerGetBusClockFrequency(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerGetBusClockFrequency ret:" + busClock);

		cpu.gpr[2] = busClock;
	}

	public void scePowerGetCpuClockFrequencyInt(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerGetCpuClockFrequencyInt ret:" + cpuClock);

		cpu.gpr[2] = cpuClock;
	}

	public void scePowerGetBusClockFrequencyInt(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.debug("scePowerGetBusClockFrequencyInt ret:" + busClock);

		cpu.gpr[2] = busClock;
	}

	public void scePowerGetPllClockFrequencyInt(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerGetPllClockFrequencyInt [0x34F9C463]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerGetCpuClockFrequencyFloat(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerGetCpuClockFrequencyFloat [0xB1A52C83]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerGetBusClockFrequencyFloat(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerGetBusClockFrequencyFloat [0x9BADB3EB]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerGetPllClockFrequencyFloat(Processor processor) {
		CpuState cpu = processor.cpu;

		System.out.println("Unimplemented NID function scePowerGetPllClockFrequencyFloat [0xEA382A27]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public void scePowerSetClockFrequency(Processor processor) {
		CpuState cpu = processor.cpu;

		//valid from 19 - 333
		int pplClock = cpu.gpr[4];
		//valid from 1-333
		cpuClock = cpu.gpr[5];
		//valid from 1-167
		busClock = cpu.gpr[6];

		Modules.log.debug("scePowerSetClockFrequency ppl:" + pplClock + " cpu:" + cpuClock + " bus:" + busClock);

		cpu.gpr[2] = 0;
	}

    public void scePower_EBD177D6(Processor processor) {
		CpuState cpu = processor.cpu;

        // Identical to scePowerSetClockFrequency.

		int pplClock = cpu.gpr[4];
		cpuClock = cpu.gpr[5];
		busClock = cpu.gpr[6];

		Modules.log.debug("scePower_EBD177D6 ppl:" + pplClock + " cpu:" + cpuClock + " bus:" + busClock);

		cpu.gpr[2] = 0;
	}

	public final HLEModuleFunction scePower_2B51FE2FFunction = new HLEModuleFunction("scePower", "scePower_2B51FE2F") {
		@Override
		public final void execute(Processor processor) {
			scePower_2B51FE2F(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePower_2B51FE2F(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetBacklightMaximumFunction = new HLEModuleFunction("scePower", "scePowerGetBacklightMaximum") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetBacklightMaximum(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetBacklightMaximum(processor);";
		}
	};

	public final HLEModuleFunction scePowerTickFunction = new HLEModuleFunction("scePower", "scePowerTick") {
		@Override
		public final void execute(Processor processor) {
			scePowerTick(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerTick(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetIdleTimerFunction = new HLEModuleFunction("scePower", "scePowerGetIdleTimer") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetIdleTimer(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetIdleTimer(processor);";
		}
	};

	public final HLEModuleFunction scePowerIdleTimerEnableFunction = new HLEModuleFunction("scePower", "scePowerIdleTimerEnable") {
		@Override
		public final void execute(Processor processor) {
			scePowerIdleTimerEnable(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerIdleTimerEnable(processor);";
		}
	};

	public final HLEModuleFunction scePowerIdleTimerDisableFunction = new HLEModuleFunction("scePower", "scePowerIdleTimerDisable") {
		@Override
		public final void execute(Processor processor) {
			scePowerIdleTimerDisable(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerIdleTimerDisable(processor);";
		}
	};

	public final HLEModuleFunction scePowerBatteryUpdateInfoFunction = new HLEModuleFunction("scePower", "scePowerBatteryUpdateInfo") {
		@Override
		public final void execute(Processor processor) {
			scePowerBatteryUpdateInfo(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerBatteryUpdateInfo(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetForceSuspendCapacityFunction = new HLEModuleFunction("scePower", "scePowerGetForceSuspendCapacity") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetForceSuspendCapacity(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetForceSuspendCapacity(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetLowBatteryCapacityFunction = new HLEModuleFunction("scePower", "scePowerGetLowBatteryCapacity") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetLowBatteryCapacity(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetLowBatteryCapacity(processor);";
		}
	};

	public final HLEModuleFunction scePowerIsPowerOnlineFunction = new HLEModuleFunction("scePower", "scePowerIsPowerOnline") {
		@Override
		public final void execute(Processor processor) {
			scePowerIsPowerOnline(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerIsPowerOnline(processor);";
		}
	};

	public final HLEModuleFunction scePowerIsBatteryExistFunction = new HLEModuleFunction("scePower", "scePowerIsBatteryExist") {
		@Override
		public final void execute(Processor processor) {
			scePowerIsBatteryExist(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerIsBatteryExist(processor);";
		}
	};

	public final HLEModuleFunction scePowerIsBatteryChargingFunction = new HLEModuleFunction("scePower", "scePowerIsBatteryCharging") {
		@Override
		public final void execute(Processor processor) {
			scePowerIsBatteryCharging(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerIsBatteryCharging(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetBatteryChargingStatusFunction = new HLEModuleFunction("scePower", "scePowerGetBatteryChargingStatus") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetBatteryChargingStatus(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetBatteryChargingStatus(processor);";
		}
	};

	public final HLEModuleFunction scePowerIsLowBatteryFunction = new HLEModuleFunction("scePower", "scePowerIsLowBattery") {
		@Override
		public final void execute(Processor processor) {
			scePowerIsLowBattery(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerIsLowBattery(processor);";
		}
	};

	public final HLEModuleFunction scePowerIsSuspendRequiredFunction = new HLEModuleFunction("scePower", "scePowerIsSuspendRequired") {
		@Override
		public final void execute(Processor processor) {
			scePowerIsSuspendRequired(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerIsSuspendRequired(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetBatteryRemainCapacityFunction = new HLEModuleFunction("scePower", "scePowerGetBatteryRemainCapacity") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetBatteryRemainCapacity(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetBatteryRemainCapacity(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetBatteryFullCapacityFunction = new HLEModuleFunction("scePower", "scePowerGetBatteryFullCapacity") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetBatteryFullCapacity(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetBatteryFullCapacity(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetBatteryLifePercentFunction = new HLEModuleFunction("scePower", "scePowerGetBatteryLifePercent") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetBatteryLifePercent(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetBatteryLifePercent(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetBatteryLifeTimeFunction = new HLEModuleFunction("scePower", "scePowerGetBatteryLifeTime") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetBatteryLifeTime(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetBatteryLifeTime(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetBatteryTempFunction = new HLEModuleFunction("scePower", "scePowerGetBatteryTemp") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetBatteryTemp(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetBatteryTemp(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetBatteryElecFunction = new HLEModuleFunction("scePower", "scePowerGetBatteryElec") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetBatteryElec(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetBatteryElec(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetBatteryVoltFunction = new HLEModuleFunction("scePower", "scePowerGetBatteryVolt") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetBatteryVolt(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetBatteryVolt(processor);";
		}
	};

	public final HLEModuleFunction scePower_23436A4AFunction = new HLEModuleFunction("scePower", "scePower_23436A4A") {
		@Override
		public final void execute(Processor processor) {
			scePower_23436A4A(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePower_23436A4A(processor);";
		}
	};

	public final HLEModuleFunction scePowerSetPowerSwModeFunction = new HLEModuleFunction("scePower", "scePowerSetPowerSwMode") {
		@Override
		public final void execute(Processor processor) {
			scePowerSetPowerSwMode(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerSetPowerSwMode(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetPowerSwModeFunction = new HLEModuleFunction("scePower", "scePowerGetPowerSwMode") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetPowerSwMode(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetPowerSwMode(processor);";
		}
	};

	public final HLEModuleFunction scePowerVolatileMemLockFunction = new HLEModuleFunction("scePower", "scePowerVolatileMemLock") {
		@Override
		public final void execute(Processor processor) {
			scePowerVolatileMemLock(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerVolatileMemLock(processor);";
		}
	};

	public final HLEModuleFunction scePowerVolatileMemTryLockFunction = new HLEModuleFunction("scePower", "scePowerVolatileMemTryLock") {
		@Override
		public final void execute(Processor processor) {
			scePowerVolatileMemTryLock(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerVolatileMemTryLock(processor);";
		}
	};

	public final HLEModuleFunction scePowerVolatileMemUnlockFunction = new HLEModuleFunction("scePower", "scePowerVolatileMemUnlock") {
		@Override
		public final void execute(Processor processor) {
			scePowerVolatileMemUnlock(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerVolatileMemUnlock(processor);";
		}
	};

	public final HLEModuleFunction scePowerLockFunction = new HLEModuleFunction("scePower", "scePowerLock") {
		@Override
		public final void execute(Processor processor) {
			scePowerLock(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerLock(processor);";
		}
	};

	public final HLEModuleFunction scePowerUnlockFunction = new HLEModuleFunction("scePower", "scePowerUnlock") {
		@Override
		public final void execute(Processor processor) {
			scePowerUnlock(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerUnlock(processor);";
		}
	};

	public final HLEModuleFunction scePowerCancelRequestFunction = new HLEModuleFunction("scePower", "scePowerCancelRequest") {
		@Override
		public final void execute(Processor processor) {
			scePowerCancelRequest(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerCancelRequest(processor);";
		}
	};

	public final HLEModuleFunction scePowerIsRequestFunction = new HLEModuleFunction("scePower", "scePowerIsRequest") {
		@Override
		public final void execute(Processor processor) {
			scePowerIsRequest(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerIsRequest(processor);";
		}
	};

	public final HLEModuleFunction scePowerRequestStandbyFunction = new HLEModuleFunction("scePower", "scePowerRequestStandby") {
		@Override
		public final void execute(Processor processor) {
			scePowerRequestStandby(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerRequestStandby(processor);";
		}
	};

	public final HLEModuleFunction scePowerRequestSuspendFunction = new HLEModuleFunction("scePower", "scePowerRequestSuspend") {
		@Override
		public final void execute(Processor processor) {
			scePowerRequestSuspend(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerRequestSuspend(processor);";
		}
	};

	public final HLEModuleFunction scePower_2875994BFunction = new HLEModuleFunction("scePower", "scePower_2875994B") {
		@Override
		public final void execute(Processor processor) {
			scePower_2875994B(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePower_2875994B(processor);";
		}
	};

	public final HLEModuleFunction scePowerWaitRequestCompletionFunction = new HLEModuleFunction("scePower", "scePowerWaitRequestCompletion") {
		@Override
		public final void execute(Processor processor) {
			scePowerWaitRequestCompletion(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerWaitRequestCompletion(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetResumeCountFunction = new HLEModuleFunction("scePower", "scePowerGetResumeCount") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetResumeCount(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetResumeCount(processor);";
		}
	};

	public final HLEModuleFunction scePowerRegisterCallbackFunction = new HLEModuleFunction("scePower", "scePowerRegisterCallback") {
		@Override
		public final void execute(Processor processor) {
			scePowerRegisterCallback(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerRegisterCallback(processor);";
		}
	};

	public final HLEModuleFunction scePowerUnregisterCallbackFunction = new HLEModuleFunction("scePower", "scePowerUnregisterCallback") {
		@Override
		public final void execute(Processor processor) {
			scePowerUnregisterCallback(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerUnregisterCallback(processor);";
		}
	};

	public final HLEModuleFunction scePowerUnregitserCallbackFunction = new HLEModuleFunction("scePower", "scePowerUnregitserCallback") {
		@Override
		public final void execute(Processor processor) {
			scePowerUnregitserCallback(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerUnregitserCallback(processor);";
		}
	};

	public final HLEModuleFunction scePowerSetCpuClockFrequencyFunction = new HLEModuleFunction("scePower", "scePowerSetCpuClockFrequency") {
		@Override
		public final void execute(Processor processor) {
			scePowerSetCpuClockFrequency(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerSetCpuClockFrequency(processor);";
		}
	};

	public final HLEModuleFunction scePowerSetBusClockFrequencyFunction = new HLEModuleFunction("scePower", "scePowerSetBusClockFrequency") {
		@Override
		public final void execute(Processor processor) {
			scePowerSetBusClockFrequency(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerSetBusClockFrequency(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetCpuClockFrequencyFunction = new HLEModuleFunction("scePower", "scePowerGetCpuClockFrequency") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetCpuClockFrequency(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetCpuClockFrequency(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetBusClockFrequencyFunction = new HLEModuleFunction("scePower", "scePowerGetBusClockFrequency") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetBusClockFrequency(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetBusClockFrequency(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetCpuClockFrequencyIntFunction = new HLEModuleFunction("scePower", "scePowerGetCpuClockFrequencyInt") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetCpuClockFrequencyInt(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetCpuClockFrequencyInt(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetBusClockFrequencyIntFunction = new HLEModuleFunction("scePower", "scePowerGetBusClockFrequencyInt") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetBusClockFrequencyInt(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetBusClockFrequencyInt(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetPllClockFrequencyIntFunction = new HLEModuleFunction("scePower", "scePowerGetPllClockFrequencyInt") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetPllClockFrequencyInt(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetPllClockFrequencyInt(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetCpuClockFrequencyFloatFunction = new HLEModuleFunction("scePower", "scePowerGetCpuClockFrequencyFloat") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetCpuClockFrequencyFloat(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetCpuClockFrequencyFloat(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetBusClockFrequencyFloatFunction = new HLEModuleFunction("scePower", "scePowerGetBusClockFrequencyFloat") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetBusClockFrequencyFloat(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetBusClockFrequencyFloat(processor);";
		}
	};

	public final HLEModuleFunction scePowerGetPllClockFrequencyFloatFunction = new HLEModuleFunction("scePower", "scePowerGetPllClockFrequencyFloat") {
		@Override
		public final void execute(Processor processor) {
			scePowerGetPllClockFrequencyFloat(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerGetPllClockFrequencyFloat(processor);";
		}
	};

	public final HLEModuleFunction scePowerSetClockFrequencyFunction = new HLEModuleFunction("scePower", "scePowerSetClockFrequency") {
		@Override
		public final void execute(Processor processor) {
			scePowerSetClockFrequency(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePowerSetClockFrequency(processor);";
		}
	};

    public final HLEModuleFunction scePower_EBD177D6Function = new HLEModuleFunction("scePower", "scePower_EBD177D6") {
		@Override
		public final void execute(Processor processor) {
			scePower_EBD177D6(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.scePowerModule.scePower_EBD177D6(processor);";
		}
	};
};