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
package jpcsp.HLE.modules;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

public class sceSystimer extends HLEModule {
    public static Logger log = Modules.getLogger("sceSystimer");

    /**
     * Obtain a hardware timer. Cannot be called from an interrupt handler. 
     * 
     * @return The timer id (greater than or equal to 0) on success.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xC99073E3, version = 150, checkInsideInterrupt = true)
    @HLEFunction(nid = 0x71059CBF, version = 635, checkInsideInterrupt = true)
    @HLEFunction(nid = 0x893DEC1A, version = 660, checkInsideInterrupt = true)
    public int sceSTimerAlloc() {
    	// Has no parameters
    	return 0;
    }

    /**
     * Return the hardware timer that was obtained  by ::sceSTimerAlloc().
     * Cannot be called from an interrupt handler.
     * 
     * @param timerId The ID of the timer to return.
     * 
     * @return SCE_ERROR_OK on success.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xC105CF38, version = 150, checkInsideInterrupt = true)
    @HLEFunction(nid = 0xF03AE143, version = 635, checkInsideInterrupt = true)
    @HLEFunction(nid = 0x93B952D1, version = 660, checkInsideInterrupt = true)
    public int sceSTimerFree(int timerId) {
    	return 0;
    }

    /**
     * Start hardware timer counting. The timer is set to "in use" state.
     * 
     * @param timerId The ID of the timer to start counting.
     * 
     * @return SCE_ERROR_OK on success.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xA95143E2, version = 150)
    @HLEFunction(nid = 0x8FB264FB, version = 635)
    @HLEFunction(nid = 0x4BC8C61C, version = 660)
    public int sceSTimerStartCount(int timerId) {
    	return 0;
    }

    /**
     * Read the current value of hardware timer's counter register.
     * 
     * @param timerId The ID of the timer to obtain the timer counter value from.
     * @param count A pointer where the obtained counter value will be stored into.
     * 
     * @return SCE_ERROR_OK on success.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x228EDAE4, version = 150)
    @HLEFunction(nid = 0x94BA6594, version = 635)
    @HLEFunction(nid = 0xDDA5D3EA, version = 660)
    public int sceSTimerGetCount(int timerId, @BufferInfo(usage=Usage.out) TPointer32 countAddr) {
    	return 0;
    }

    /**
     * Reset the hardware timer's counter register to 0.
     * 
     * @param timerId The ID of the timer to reset its counter.
     * 
     * @return SCE_ERROR_OK on success.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x54BB5DB4, version = 150)
    @HLEFunction(nid = 0x964F73FD, version = 635)
    @HLEFunction(nid = 0x6AC97B04, version = 660)
    public int sceSTimerResetCount(int timerId) {
    	return 0;
    }

    /**
     * Stop the hardware timer counting and set the timer's state to "not in use".
     * 
     * @param timerId The ID of the timer to stop its counting.
     * 
     * @return SCE_ERROR_OK on success.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x4A01F9D3, version = 150)
    @HLEFunction(nid = 0xD01C6E08, version = 635)
    @HLEFunction(nid = 0x973104D0, version = 660)
    public int sceSTimerStopCount(int timerId) {
    	return 0;
    }

    /**
     * Set the prescale of a hardware timer. It can be only set on timers which are in the "not in use" state.
     * The input signal is divided into the resulting ratio. The ratio has to be less than 1/11.
     * 
     * @param timerId The ID of the timer to set the prescale.
     * @param numerator The numerator of the prescale. Must not be 0.
     * @param denominator The denominator of the prescale. Must not be 0.
     * 
     * @return SCE_ERROR_OK on success.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xB53534B4, version = 150)
    @HLEFunction(nid = 0x4467BD60, version = 635)
    @HLEFunction(nid = 0xCAC43FEB, version = 660)
    public int sceSTimerSetPrscl(int timerId, int numerator, int denominator) {
    	return 0;
    }

    /**
     * Set the comparison value and the time-up handler of the hardware timer counter register.
     * The hardware timer counter register begins counting up from zero. If the counter register matches
     * the comparison value, an interrupt occurs, the counter register is returned to zero, and counting
     * continues. The time-up handler is called via this interrupt.
     * 
     * @param timerId The ID of the timer to set the compare value and time-up handler.
     * @param compareValue The count comparison value. Should not be greater 4194303 (~1/10 seconds) and less than 0.
     * @param timeUpHandler Specify the time-up handler that is called when count matches the comparison value.
     *                      Pass null to delete a registered time-up handler. This will also stop the hardware timer
     *                      counting. 
     * @param common Pointer to memory common between time-up handler and general routines.
     * 
     * @return SCE_ERROR_OK on success. 
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x975D8E84, version = 150)
    @HLEFunction(nid = 0x847D785B, version = 635)
    @HLEFunction(nid = 0xEEE11CA5, version = 660)
    public int sceSTimerSetHandler(int timerId, int compareValue, @BufferInfo(usage=Usage.in) @CanBeNull TPointer timeUpHandler, int common) {
    	return 0;
    }

    /**
     * Unknown purpose.
     * 
     * @param timerId The ID of the timer.
     * @param unknown Unknown. Should not be less than 0 and greater 4194304.
     * 
     * @return SCE_ERROR_OK on success.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x53231A15, version = 150)
    @HLEFunction(nid = 0xCE5A60D8, version = 635)
    @HLEFunction(nid = 0xE265669A, version = 660)
    public int sceSTimerSetTMCY(int timerId, int unknown) {
    	return 0;
    }
}
