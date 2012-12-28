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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.hardware.Battery;

import org.apache.log4j.Logger;

@HLELogging
public class scePower extends HLEModule {
    public static Logger log = Modules.getLogger("scePower");

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

    @HLEUnimplemented
    @HLEFunction(nid = 0x2B51FE2F, version = 150)
    public int scePower_2B51FE2F() {
    	return 0;
    }

    @HLEFunction(nid = 0x442BFBAC, version = 150)
    public int scePowerGetBacklightMaximum() {
        return backlightMaximum;
    }

    @HLELogging(level="trace")
    @HLEFunction(nid = 0xEFD3C963, version = 150)
    public int scePowerTick(int flag) {
    	return Modules.sceSuspendForUserModule.hleKernelPowerTick(flag);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEDC13FE5, version = 150)
    public int scePowerGetIdleTimer() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7F30B3B1, version = 150)
    public int scePowerIdleTimerEnable() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x972CE941, version = 150)
    public int scePowerIdleTimerDisable() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x27F3292C, version = 150)
    public int scePowerBatteryUpdateInfo() {
    	return 0;
    }

    @HLEFunction(nid = 0xE8E4E204, version = 150)
    public int scePowerGetForceSuspendCapacity() {
        int forceSuspendCapacity = (Battery.getForceSuspendPercent() * Battery.getFullCapacity()) / 100;
        if (log.isDebugEnabled()) {
        	log.debug(String.format("scePowerGetForceSuspendCapacity returning %d mAh", forceSuspendCapacity));
        }

        return forceSuspendCapacity;
    }

    @HLEFunction(nid = 0xB999184C, version = 150)
    public int scePowerGetLowBatteryCapacity() {
        int lowBatteryCapacity = (Battery.getLowPercent() * Battery.getFullCapacity()) / 100;
        if (log.isDebugEnabled()) {
        	log.debug(String.format("scePowerGetLowBatteryCapacity returning %d mAh", lowBatteryCapacity));
        }

        return lowBatteryCapacity;
    }

    @HLEFunction(nid = 0x87440F5E, version = 150)
    public boolean scePowerIsPowerOnline() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerIsPowerOnline returning %b", Battery.isPluggedIn()));
    	}

        return Battery.isPluggedIn();
    }

    @HLEFunction(nid = 0x0AFD0D8B, version = 150)
    public boolean scePowerIsBatteryExist() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerIsBatteryExist returning %b", Battery.isPresent()));
    	}

        return Battery.isPresent();
    }

    @HLEFunction(nid = 0x1E490401, version = 150)
    public boolean scePowerIsBatteryCharging() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerIsBatteryCharging returning %b", Battery.isCharging()));
    	}

        return Battery.isCharging();
    }

    @HLEFunction(nid = 0xB4432BC8, version = 150)
    public int scePowerGetBatteryChargingStatus() {
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

        if (log.isDebugEnabled()) {
        	log.debug(String.format("scePowerGetBatteryChargingStatus returning 0x%X", status));
        }

        return status;
    }

    @HLEFunction(nid = 0xD3075926, version = 150)
    public boolean scePowerIsLowBattery() {
        boolean isLow = Battery.getCurrentPowerPercent() <= Battery.getLowPercent();
        if (log.isDebugEnabled()) {
        	log.debug(String.format("scePowerIsLowBattery returning %b", isLow));
        }

        return isLow;
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
    public boolean scePowerIsSuspendRequired() {
        boolean isSuspendRequired = Battery.getCurrentPowerPercent() <= Battery.getForceSuspendPercent();
        if (log.isDebugEnabled()) {
        	log.debug(String.format("scePowerIsSuspendRequired returning %b", isSuspendRequired));
        }

        return isSuspendRequired;
    }

    @HLEFunction(nid = 0x94F5A53F, version = 150)
    public int scePowerGetBatteryRemainCapacity() {
        int batteryRemainCapacity = (Battery.getCurrentPowerPercent() * Battery.getFullCapacity()) / 100;
        if (log.isDebugEnabled()) {
        	log.debug(String.format("scePowerGetBatteryRemainCapacity returning %d mAh", batteryRemainCapacity));
        }

        return batteryRemainCapacity;
    }

    @HLEFunction(nid = 0xFD18A0FF, version = 150)
    public int scePowerGetBatteryFullCapacity() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetBatteryFullCapacity returning %d mAh", Battery.getFullCapacity()));
    	}

        return Battery.getFullCapacity();
    }

    @HLEFunction(nid = 0x2085D15D, version = 150)
    public int scePowerGetBatteryLifePercent() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetBatteryLifePercent returning %d %%", Battery.getCurrentPowerPercent()));
    	}

        return Battery.getCurrentPowerPercent();
    }

    @HLEFunction(nid = 0x8EFB3FA2, version = 150)
    public int scePowerGetBatteryLifeTime() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetBatteryLifeTime returning %d", Battery.getLifeTime()));
    	}

        return Battery.getLifeTime();
    }

    @HLEFunction(nid = 0x28E12023, version = 150)
    public int scePowerGetBatteryTemp() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetBatteryTemp returning %d C", Battery.getTemperature()));
    	}

        return Battery.getTemperature();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x862AE1A6, version = 150)
    public int scePowerGetBatteryElec() {
    	return 0;
    }

    @HLEFunction(nid = 0x483CE86B, version = 150)
    public int scePowerGetBatteryVolt() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetBatteryVolt %d", Battery.getVoltage()));
    	}

        return Battery.getVoltage();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x23436A4A, version = 150)
    public int scePower_23436A4A() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0CD21B1F, version = 150)
    public int scePowerSetPowerSwMode() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x165CE085, version = 150)
    public int scePowerGetPowerSwMode() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x23C31FFE, version = 150)
    public int scePowerVolatileMemLock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFA97A599, version = 150)
    public int scePowerVolatileMemTryLock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB3EDD801, version = 150)
    public int scePowerVolatileMemUnlock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD6D016EF, version = 150)
    public int scePowerLock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCA3D34C1, version = 150)
    public int scePowerUnlock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDB62C9CF, version = 150)
    public int scePowerCancelRequest() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7FA406DD, version = 150)
    public int scePowerIsRequest() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2B7C7CF4, version = 150)
    public int scePowerRequestStandby() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAC32C9CC, version = 150)
    public int scePowerRequestSuspend() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2875994B, version = 150)
    public int scePower_2875994B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3951AF53, version = 150)
    public int scePowerWaitRequestCompletion() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0074EF9B, version = 150)
    public int scePowerGetResumeCount() {
    	return 0;
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0x04B7766E, version = 150)
    public int scePowerRegisterCallback(int slot, int uid) {
        boolean notifyCallback = false;
        int result;

        // Multiple power callbacks (up to 16) can be assigned for multiple threads.
        if (slot == PSP_POWER_CB_SLOT_AUTO) {
        	// Return ERROR_OUT_OF_MEMORY when no free slot found
        	result = SceKernelErrors.ERROR_OUT_OF_MEMORY;

        	for (int i = 0; i < powerCBSlots.length; i++) {
                if (powerCBSlots[i] == 0) {
                    powerCBSlots[i] = uid;
                    result = i;
                    notifyCallback = true;
                    break;
                }
            }
        } else if (slot >= 0 && slot < powerCBSlots.length) {
        	if (powerCBSlots[slot] == 0) {
        		powerCBSlots[slot] = uid;
        		result = 0;
        		notifyCallback = true;
        	} else {
        		result = SceKernelErrors.ERROR_ALREADY;
        	}
        } else {
            result = -1;
        }

        if (notifyCallback) {
	        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
	        if (threadMan.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_POWER, uid)) {
	            // Start by notifying the POWER callback that we're using AC power.
	            threadMan.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_POWER, uid, PSP_POWER_CB_AC_POWER);
	        }
        }

        return result;
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0xDFA8BAF8, version = 150)
    public int scePowerUnregisterCallback(int slot) {
    	if (slot < 0 || slot >= powerCBSlots.length) {
    		return -1;
    	}

    	if (powerCBSlots[slot] != 0) {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            threadMan.hleKernelUnRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_POWER, powerCBSlots[slot]);
            powerCBSlots[slot] = 0;
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDB9D28DD, version = 150)
    public int scePowerUnregisterCallback() {
    	return 0;
    }

    @HLEFunction(nid = 0x843FBF43, version = 150, checkInsideInterrupt = true)
    public int scePowerSetCpuClockFrequency(int freq) {
        cpuClock = freq;

        return 0;
    }

    @HLEFunction(nid = 0xB8D7B3FB, version = 150, checkInsideInterrupt = true)
    public int scePowerSetBusClockFrequency(int freq) {
        busClock = freq;

        return 0;
    }

    @HLEFunction(nid = 0xFEE03A2F, version = 150)
    public int scePowerGetCpuClockFrequency() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetCpuClockFrequency returning 0x%X", cpuClock));
    	}

        return cpuClock;
    }

    @HLEFunction(nid = 0x478FE6F5, version = 150)
    public int scePowerGetBusClockFrequency() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetBusClockFrequency returning 0x%X", busClock));
    	}

        return busClock;
    }

    @HLEFunction(nid = 0xFDB5BFE9, version = 150)
    public int scePowerGetCpuClockFrequencyInt() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetCpuClockFrequencyInt returning 0x%X", cpuClock));
    	}

        return cpuClock;
    }

    @HLEFunction(nid = 0xBD681969, version = 150)
    public int scePowerGetBusClockFrequencyInt() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetBusClockFrequencyInt returning 0x%X", busClock));
    	}

        return busClock;
    }

    @HLEFunction(nid = 0x34F9C463, version = 150)
    public int scePowerGetPllClockFrequencyInt() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetPllClockFrequencyInt returning 0x%X", pllClock));
    	}

        return pllClock;
    }

    @HLEFunction(nid = 0xB1A52C83, version = 150)
    public float scePowerGetCpuClockFrequencyFloat() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetCpuClockFrequencyFloat returning %f", (float) cpuClock));
    	}

        return (float) cpuClock;
    }

    @HLEFunction(nid = 0x9BADB3EB, version = 150)
    public float scePowerGetBusClockFrequencyFloat() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetBusClockFrequencyFloat returning %f", (float) busClock));
    	}

        return (float) busClock;
    }

    @HLEFunction(nid = 0xEA382A27, version = 150)
    public float scePowerGetPllClockFrequencyFloat() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("scePowerGetPllClockFrequencyFloat returning %f", (float) pllClock));
    	}

        return (float) pllClock;
    }

    @HLEFunction(nid = 0x737486F2, version = 150)
    public int scePowerSetClockFrequency(int pllClock, int cpuClock, int busClock) {
        this.pllClock = pllClock;
        this.cpuClock = cpuClock;
        this.busClock = busClock;

        return 0;
    }

    @HLEFunction(nid = 0xEBD177D6, version = 150)
    public int scePower_EBD177D6(int pllClock, int cpuClock, int busClock) {
        // Identical to scePowerSetClockFrequency.
        this.pllClock = pllClock;
        this.cpuClock = cpuClock;
        this.busClock = busClock;

        return 0;
    }
}