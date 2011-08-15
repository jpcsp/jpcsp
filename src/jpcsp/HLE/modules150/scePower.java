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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
import jpcsp.HLE.HLEFunction;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.hardware.Battery;

import org.apache.log4j.Logger;

public class scePower extends HLEModule {

    protected static Logger log = Modules.getLogger("scePower");

    @Override
    public String getName() {
        return "scePower";
    }
    /**
     * Power callback flags
     */
    // indicates the power switch it pushed, putting the unit into suspend mode
    public static final int PSP_POWER_CB_POWER_SWITCH = 0x80000000;
    // indicates the hold switch is on
    public static final int PSP_POWER_CB_HOLD_SWITCH = 0x40000000;
    // what is standby mode?
    public static final int PSP_POWER_CB_STANDBY = 0x00080000;
    // indicates the resume process has been completed (only seems to be triggered when another event happens)
    public static final int PSP_POWER_CB_RESUME_COMPLETE = 0x00040000;
    // indicates the unit is resuming from suspend mode
    public static final int PSP_POWER_CB_RESUMING = 0x00020000;
    // indicates the unit is suspending, seems to occur due to inactivity
    public static final int PSP_POWER_CB_SUSPENDING = 0x00010000;
    // indicates the unit is plugged into an AC outlet
    public static final int PSP_POWER_CB_AC_POWER = 0x00001000;
    // indicates the battery charge level is low
    public static final int PSP_POWER_CB_BATTERY_LOW = 0x00000100;
    // indicates there is a battery present in the unit
    public static final int PSP_POWER_CB_BATTERY_EXIST = 0x00000080;
    // unknown
    public static final int PSP_POWER_CB_BATTPOWER = 0x0000007F;

    /**
     * Power callback slots
     */
    public static final int PSP_POWER_CB_SLOT_AUTO = -1;
    protected int[] powerCBSlots = new int[16];

    // PLL clock:
    // Operates at fixed rates of 148MHz, 190MHz, 222MHz, 266MHz, 333MHz.
    // Starts at 222MHz.
    protected int pllClock = 222;
    // CPU clock:
    // Operates at variable rates from 1MHz to 333MHz.
    // Starts at 222MHz.
    // Note: Cannot have a higher frequency than the PLL clock's frequency.
    protected int cpuClock = 222;
    // BUS clock:
    // Operates at variable rates from 37MHz to 166MHz.
    // Starts at 111MHz.
    // Note: Cannot have a higher frequency than 1/2 of the PLL clock's frequency
    // or lower than 1/4 of the PLL clock's frequency.
    protected int busClock = 111;
    protected static final int backlightMaximum = 4;

    @HLEFunction(nid = 0x2B51FE2F, version = 150)
    public void scePower_2B51FE2F(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePower_2B51FE2F [0x2B51FE2F]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x442BFBAC, version = 150)
    public void scePowerGetBacklightMaximum(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetBacklightMaximum backlightMaxium=" + backlightMaximum);

        cpu.gpr[2] = backlightMaximum;
    }

    @HLEFunction(nid = 0xEFD3C963, version = 150)
    public void scePowerTick(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("Unimplemented NID function scePowerTick [0xEFD3C963]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xEDC13FE5, version = 150)
    public void scePowerGetIdleTimer(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerGetIdleTimer [0xEDC13FE5]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x7F30B3B1, version = 150)
    public void scePowerIdleTimerEnable(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerIdleTimerEnable [0x7F30B3B1]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x972CE941, version = 150)
    public void scePowerIdleTimerDisable(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerIdleTimerDisable [0x972CE941]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x27F3292C, version = 150)
    public void scePowerBatteryUpdateInfo(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerBatteryUpdateInfo [0x27F3292C]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xE8E4E204, version = 150)
    public void scePowerGetForceSuspendCapacity(Processor processor) {
        CpuState cpu = processor.cpu;

        int forceSuspendCapacity = (Battery.getForceSuspendPercent() * Battery.getFullCapacity()) / 100;
        log.debug("scePowerGetForceSuspendCapacity " + forceSuspendCapacity + "mAh");

        cpu.gpr[2] = forceSuspendCapacity;
    }

    @HLEFunction(nid = 0xB999184C, version = 150)
    public void scePowerGetLowBatteryCapacity(Processor processor) {
        CpuState cpu = processor.cpu;

        int lowBatteryCapacity = (Battery.getLowPercent() * Battery.getFullCapacity()) / 100;
        log.debug("scePowerGetLowBatteryCapacity " + lowBatteryCapacity + "mAh");

        cpu.gpr[2] = lowBatteryCapacity;
    }

    @HLEFunction(nid = 0x87440F5E, version = 150)
    public void scePowerIsPowerOnline(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerIsPowerOnline pluggedIn=" + Battery.isPluggedIn());

        cpu.gpr[2] = Battery.isPluggedIn() ? 1 : 0;
    }

    @HLEFunction(nid = 0x0AFD0D8B, version = 150)
    public void scePowerIsBatteryExist(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerIsBatteryExist batteryPresent=" + Battery.isPresent());

        cpu.gpr[2] = Battery.isPresent() ? 1 : 0;
    }

    @HLEFunction(nid = 0x1E490401, version = 150)
    public void scePowerIsBatteryCharging(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerIsBatteryCharging batteryCharging=" + Battery.isCharging());

        cpu.gpr[2] = Battery.isCharging() ? 1 : 0;
    }

    @HLEFunction(nid = 0xB4432BC8, version = 150)
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

        log.debug("scePowerGetBatteryChargingStatus status=0x" + Integer.toHexString(status));

        cpu.gpr[2] = status;
    }

    @HLEFunction(nid = 0xD3075926, version = 150)
    public void scePowerIsLowBattery(Processor processor) {
        CpuState cpu = processor.cpu;

        int isLow = (Battery.getCurrentPowerPercent() <= Battery.getLowPercent()) ? 1 : 0;
        log.debug("scePowerIsLowBattery " + isLow);

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
    @HLEFunction(nid = 0x78A1A796, version = 150)
    public void scePowerIsSuspendRequired(Processor processor) {
        CpuState cpu = processor.cpu;

        int isSuspendRequired = (Battery.getCurrentPowerPercent() <= Battery.getForceSuspendPercent() ? 1 : 0);
        log.debug("scePowerIsSuspendRequired isSuspendRequired=" + isSuspendRequired);

        cpu.gpr[2] = isSuspendRequired;
    }

    @HLEFunction(nid = 0x94F5A53F, version = 150)
    public void scePowerGetBatteryRemainCapacity(Processor processor) {
        CpuState cpu = processor.cpu;

        int batteryRemainCapacity = (Battery.getCurrentPowerPercent() * Battery.getFullCapacity()) / 100;
        log.debug("scePowerGetBatteryRemainCapacity " + batteryRemainCapacity + "mAh");

        cpu.gpr[2] = batteryRemainCapacity;
    }

    @HLEFunction(nid = 0xFD18A0FF, version = 150)
    public void scePowerGetBatteryFullCapacity(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetBatteryFullCapacity " + Battery.getFullCapacity() + "mAh");

        cpu.gpr[2] = Battery.getFullCapacity();
    }

    @HLEFunction(nid = 0x2085D15D, version = 150)
    public void scePowerGetBatteryLifePercent(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetBatteryLifePercent percent=" + Battery.getCurrentPowerPercent());

        cpu.gpr[2] = Battery.getCurrentPowerPercent();
    }

    @HLEFunction(nid = 0x8EFB3FA2, version = 150)
    public void scePowerGetBatteryLifeTime(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetBatteryLifeTime batteryLifeTime=" + Battery.getLifeTime());

        cpu.gpr[2] = Battery.getLifeTime();
    }

    @HLEFunction(nid = 0x28E12023, version = 150)
    public void scePowerGetBatteryTemp(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetBatteryTemp batteryTemp=" + Battery.getTemperature());

        cpu.gpr[2] = Battery.getTemperature();
    }

    @HLEFunction(nid = 0x862AE1A6, version = 150)
    public void scePowerGetBatteryElec(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerGetBatteryElec [0x862AE1A6]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x483CE86B, version = 150)
    public void scePowerGetBatteryVolt(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetBatteryVolt batteryVoltage=" + Battery.getVoltage());

        cpu.gpr[2] = Battery.getVoltage();
    }

    @HLEFunction(nid = 0x23436A4A, version = 150)
    public void scePower_23436A4A(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePower_23436A4A [0x23436A4A]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x0CD21B1F, version = 150)
    public void scePowerSetPowerSwMode(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerSetPowerSwMode [0x0CD21B1F]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x165CE085, version = 150)
    public void scePowerGetPowerSwMode(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerGetPowerSwMode [0x165CE085]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x23C31FFE, version = 150)
    public void scePowerVolatileMemLock(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerVolatileMemLock [0x23C31FFE]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xFA97A599, version = 150)
    public void scePowerVolatileMemTryLock(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerVolatileMemTryLock [0xFA97A599]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xB3EDD801, version = 150)
    public void scePowerVolatileMemUnlock(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerVolatileMemUnlock [0xB3EDD801]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xD6D016EF, version = 150)
    public void scePowerLock(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerLock [0xD6D016EF]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xCA3D34C1, version = 150)
    public void scePowerUnlock(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerUnlock [0xCA3D34C1]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xDB62C9CF, version = 150)
    public void scePowerCancelRequest(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerCancelRequest [0xDB62C9CF]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x7FA406DD, version = 150)
    public void scePowerIsRequest(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerIsRequest [0x7FA406DD]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x2B7C7CF4, version = 150)
    public void scePowerRequestStandby(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerRequestStandby [0x2B7C7CF4]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xAC32C9CC, version = 150)
    public void scePowerRequestSuspend(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerRequestSuspend [0xAC32C9CC]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x2875994B, version = 150)
    public void scePower_2875994B(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePower_2875994B [0x2875994B]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x3951AF53, version = 150)
    public void scePowerWaitRequestCompletion(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerWaitRequestCompletion [0x3951AF53]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x0074EF9B, version = 150)
    public void scePowerGetResumeCount(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerGetResumeCount [0x0074EF9B]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x04B7766E, version = 150)
    public void scePowerRegisterCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int slot = cpu.gpr[4];
        int uid = cpu.gpr[5];

        log.info(String.format("scePowerRegisterCallback slot=%d, SceUID=0x%X", slot, uid));

        boolean notifyCallback = false;

        // Multiple power callbacks (up to 16) can be assigned for multiple threads.
        if (slot == PSP_POWER_CB_SLOT_AUTO) {
        	// Return ERROR_OUT_OF_MEMORY when no free slot found
        	cpu.gpr[2] = SceKernelErrors.ERROR_OUT_OF_MEMORY;

        	for (int i = 0; i < powerCBSlots.length; i++) {
                if (powerCBSlots[i] == 0) {
                    powerCBSlots[i] = uid;
                    cpu.gpr[2] = i;
                    notifyCallback = true;
                    break;
                }
            }
        } else if (slot >= 0 && slot < powerCBSlots.length) {
        	if (powerCBSlots[slot] == 0) {
        		powerCBSlots[slot] = uid;
        		cpu.gpr[2] = 0;
        		notifyCallback = true;
        	} else {
        		cpu.gpr[2] = SceKernelErrors.ERROR_ALREADY;
        	}
        } else {
            cpu.gpr[2] = -1;
        }

        if (notifyCallback) {
	        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
	        if (threadMan.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_POWER, uid)) {
	            // Start by notifying the POWER callback that we're using AC power.
	            threadMan.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_POWER, uid, PSP_POWER_CB_AC_POWER);
	        }
        }
    }

    @HLEFunction(nid = 0xDFA8BAF8, version = 150)
    public void scePowerUnregisterCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int slot = cpu.gpr[4];

        log.info(String.format("scePowerUnregisterCallback slot=%d", slot));

        if (slot >= 0 && slot < powerCBSlots.length) {
        	if (powerCBSlots[slot] != 0) {
	            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
	            threadMan.hleKernelUnRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_POWER, powerCBSlots[slot]);
	            powerCBSlots[slot] = 0;
        	}
            cpu.gpr[2] = 0;
        } else {
        	cpu.gpr[2] = -1;
        }
    }

    @HLEFunction(nid = 0xDB9D28DD, version = 150)
    public void scePowerUnregitserCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function scePowerUnregitserCallback [0xDB9D28DD]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x843FBF43, version = 150)
    public void scePowerSetCpuClockFrequency(Processor processor) {
        CpuState cpu = processor.cpu;

        int freq = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        log.debug("scePowerSetCpuClockFrequency : " + freq);
        cpuClock = freq;
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xB8D7B3FB, version = 150)
    public void scePowerSetBusClockFrequency(Processor processor) {
        CpuState cpu = processor.cpu;

        int freq = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        log.debug("scePowerSetBusClockFrequency : " + freq);
        busClock = freq;
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xFEE03A2F, version = 150)
    public void scePowerGetCpuClockFrequency(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetCpuClockFrequency ret:" + cpuClock);

        cpu.gpr[2] = cpuClock;
    }

    @HLEFunction(nid = 0x478FE6F5, version = 150)
    public void scePowerGetBusClockFrequency(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetBusClockFrequency ret:" + busClock);

        cpu.gpr[2] = busClock;
    }

    @HLEFunction(nid = 0xFDB5BFE9, version = 150)
    public void scePowerGetCpuClockFrequencyInt(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetCpuClockFrequencyInt ret:" + cpuClock);

        cpu.gpr[2] = cpuClock;
    }

    @HLEFunction(nid = 0xBD681969, version = 150)
    public void scePowerGetBusClockFrequencyInt(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetBusClockFrequencyInt ret:" + busClock);

        cpu.gpr[2] = busClock;
    }

    @HLEFunction(nid = 0x34F9C463, version = 150)
    public void scePowerGetPllClockFrequencyInt(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetPllClockFrequencyInt ret:" + pllClock);

        cpu.gpr[2] = pllClock;
    }

    @HLEFunction(nid = 0xB1A52C83, version = 150)
    public void scePowerGetCpuClockFrequencyFloat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetCpuClockFrequencyFloat ret:" + Float.intBitsToFloat(cpuClock));

        // Return float value in $f0
        cpu.fpr[0] = cpuClock;
    }

    @HLEFunction(nid = 0x9BADB3EB, version = 150)
    public void scePowerGetBusClockFrequencyFloat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetBusClockFrequencyInt ret:" + Float.intBitsToFloat(busClock));

        // Return float value in $f0
        cpu.fpr[0] = busClock;
    }

    @HLEFunction(nid = 0xEA382A27, version = 150)
    public void scePowerGetPllClockFrequencyFloat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("scePowerGetPllClockFrequencyInt ret:" + Float.intBitsToFloat(pllClock));

        // Return float value in $f0
        cpu.fpr[0] = pllClock;
    }

    @HLEFunction(nid = 0x737486F2, version = 150)
    public void scePowerSetClockFrequency(Processor processor) {
        CpuState cpu = processor.cpu;

        pllClock = cpu.gpr[4];
        cpuClock = cpu.gpr[5];
        busClock = cpu.gpr[6];

        log.debug("scePowerSetClockFrequency pll:" + pllClock + " cpu:" + cpuClock + " bus:" + busClock);

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xEBD177D6, version = 150)
    public void scePower_EBD177D6(Processor processor) {
        CpuState cpu = processor.cpu;

        // Identical to scePowerSetClockFrequency.
        pllClock = cpu.gpr[4];
        cpuClock = cpu.gpr[5];
        busClock = cpu.gpr[6];

        log.debug("scePower_EBD177D6 pll:" + pllClock + " cpu:" + cpuClock + " bus:" + busClock);

        cpu.gpr[2] = 0;
    }

}