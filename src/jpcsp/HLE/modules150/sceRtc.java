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

import java.util.Calendar;
import java.util.GregorianCalendar;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer64;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

public class sceRtc extends HLEModule {
    protected static Logger log = Modules.getLogger("sceRtc");

    @Override
    public String getName() { return "sceRtc"; }

    final static int PSP_TIME_INVALID_YEAR = -1;
    final static int PSP_TIME_INVALID_MONTH = -2;
    final static int PSP_TIME_INVALID_DAY = -3;
    final static int PSP_TIME_INVALID_HOUR = -4;
    final static int PSP_TIME_INVALID_MINUTES = -5;
    final static int PSP_TIME_INVALID_SECONDS = -6;
    final static int PSP_TIME_INVALID_MICROSECONDS = -7;

    // Statics verified on PSP.
    final static int PSP_TIME_SECONDS_IN_MINUTE = 60;
    final static int PSP_TIME_SECONDS_IN_HOUR = 3600;
    final static int PSP_TIME_SECONDS_IN_DAY = 86400;
    final static int PSP_TIME_SECONDS_IN_WEEK = 604800;
    final static int PSP_TIME_SECONDS_IN_MONTH = 2629743;
    final static int PSP_TIME_SECONDS_IN_YEAR = 31556926;

    private long rtcMagicOffset = 62135596800000000L;

    protected long hleGetCurrentTick() {
        return Emulator.getClock().microTime();
    }

    /** 64 bit addend */
    protected int hleRtcTickAdd64(TPointer64 dstPtr, TPointer64 srcPtr, long value, long multiplier) {
        log.debug("hleRtcTickAdd64 " + multiplier + " * " + value);
        
        long src = srcPtr.getValue();
        dstPtr.setValue(src + multiplier * value);

        return 0;
    }

    /** 32 bit addend */
    protected int hleRtcTickAdd32(TPointer64 dstPtr, TPointer64 srcPtr, int value, long multiplier) {
        log.debug("hleRtcTickAdd32 " + multiplier + " * " + value);

        long src = srcPtr.getValue();
        dstPtr.setValue(src + multiplier * value);
        
        return 0;
    }

    /**
     * Obtains the Tick Resolution.
     * 
     * @param processor
     * 
     * @return The Tick Resolution in microseconds.
     */
    @HLEFunction(nid = 0xC41C2853, version = 150)
    public int sceRtcGetTickResolution() {
        return 1000000;
    }

    @HLEFunction(nid = 0x3F7AD767, version = 150)
    public int sceRtcGetCurrentTick(TPointer64 currentTick) {
        currentTick.setValue(hleGetCurrentTick());

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceRtcGetCurrentTick 0x%08X, returning %d", currentTick.getAddress(), currentTick.getValue()));
        }

        return 0;
    }

    @HLEFunction(nid = 0x011F03C1, version = 150)
    public long sceRtcGetAccumulativeTime() {
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceRtcGetAccumulativeTime");
        }
        // Returns the difference between the last reincarnated time and the current tick.
        // Just return our current tick, since there's no need to mimick such behaviour.

        return hleGetCurrentTick();
    }

    @HLEFunction(nid = 0x029CA3B3, version = 150)
    public long sceRtcGetAccumlativeTime() {
        // Typo. Refers to the same function.
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceRtcGetAccumlativeTime");
        }
        
        return hleGetCurrentTick();
    }

    @HLEFunction(nid = 0x4CFA57B0, version = 150)
    public int sceRtcGetCurrentClock(int addr, int tz) {
        Memory mem = Processor.memory;

        ScePspDateTime pspTime = new ScePspDateTime(tz);
        pspTime.write(mem, addr);

        log.debug("sceRtcGetCurrentClock addr=" + Integer.toHexString(addr) + " time zone=" + tz);
        
        return 0;
    }

    @HLEFunction(nid = 0xE7C27D1B, version = 150)
    public int sceRtcGetCurrentClockLocalTime(int addr) {
        Memory mem = Processor.memory;

        ScePspDateTime pspTime = new ScePspDateTime();
        pspTime.write(mem, addr);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x34885E0D, version = 150)
    public int sceRtcConvertUtcToLocalTime(TPointer64 utcPtr, TPointer64 localPtr) {
        log.debug("PARTIAL: sceRtcConvertUtcToLocalTime");

        
        long utc = utcPtr.getValue();
        long local = utc;
        // TODO
        localPtr.setValue(local);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x779242A2, version = 150)
    public int sceRtcConvertLocalTimeToUTC(TPointer64 localPtr, TPointer64 utcPtr) {
        log.debug("PARTIAL: sceRtcConvertLocalTimeToUTC");

        long local = localPtr.getValue();
        long utc = local; // TODO
        utcPtr.setValue(utc);

        return 0;
    }

    @HLEFunction(nid = 0x42307A17, version = 150)
    public int sceRtcIsLeapYear(int year) {
        log.debug("sceRtcIsLeapYear");

        return ((year % 4 == 0) && (year % 100 != 0) || (year % 400 == 0)) ? 1 : 0;
    }

    @HLEFunction(nid = 0x05EF322C, version = 150)
    public int sceRtcGetDaysInMonth(int year, int month) {
        Calendar cal = new GregorianCalendar(year, month - 1, 1);

        int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        log.debug(String.format("sceRtcGetDaysInMonth %04d-%02d ret:%d", year, month, days));
        return days;
    }

    /**
     * Returns the day of the week.
     * 0 = sunday, 1 = monday, 2 = tuesday, 3 = wednesday, 4 = thursday, 5 = friday, 6 = saturnday
     *
     * @param  year
     * @param  month
     * @param  day
     * 
     * @return The day of the week.
     */
    @HLEFunction(nid = 0x57726BC1, version = 150)
    public int sceRtcGetDayOfWeek(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day);

        int dayOfWeekNumber = cal.get(Calendar.DAY_OF_WEEK);
        dayOfWeekNumber = (dayOfWeekNumber - 1 + 7) % 7;

        log.debug(String.format("sceRtcGetDayOfWeek %04d-%02d-%02d ret:%d", year, month, day, dayOfWeekNumber));
        return dayOfWeekNumber;
    }

    /**
     * Validate pspDate component ranges
     *
     * @param date - pointer to pspDate struct to be checked
     * @return 0 on success, one of PSP_TIME_INVALID_* on error
     */
    @HLEFunction(nid = 0x4B1B5E82, version = 150)
    public int sceRtcCheckValid(int time_addr) {
        Memory mem = Processor.memory;

        if (!Memory.isAddressGood(time_addr)) {
            log.warn("sceRtcGetTick bad address " + String.format("0x%08X", time_addr));
            return -1;
        }

        ScePspDateTime time = new ScePspDateTime();
        time.read(mem, time_addr);
        Calendar cal = new GregorianCalendar(
        	time.year, time.month - 1, time.day, time.hour, time.minute, time.second
        );

        int result = 0;

        if (time.year < 1582 || time.year > 3000) {	// What are valid years?
        	result = PSP_TIME_INVALID_YEAR;
        } else if (time.month < 1 || time.month > 12) {
        	result = PSP_TIME_INVALID_MONTH;
        } else if (time.day < 1 || time.day > 31) {
        	result = PSP_TIME_INVALID_DAY;
        } else if (time.hour < 0 || time.hour > 23) {
        	result = PSP_TIME_INVALID_HOUR;
        } else if (time.minute < 0 || time.minute > 59) {
        	result = PSP_TIME_INVALID_MINUTES;
        } else if (time.second < 0 || time.second > 59) {
        	result = PSP_TIME_INVALID_SECONDS;
        } else if (time.microsecond < 0 || time.microsecond >= 1000000) {
        	result = PSP_TIME_INVALID_MICROSECONDS;
        } else if (cal.get(Calendar.DAY_OF_MONTH) != time.day) { // Check if this is a valid day of the month
        	result = PSP_TIME_INVALID_DAY;
        }

        if (log.isDebugEnabled()) {
        	log.debug("sceRtcCheckValid " + time.toString() + ", cal: " + cal + ", result: " + result);
        }

        return result;
    }

    @HLEFunction(nid = 0x3A807CC8, version = 150)
    public int sceRtcSetTime_t(int date_addr, int time) {
        Memory mem = Processor.memory;

        if (!Memory.isAddressGood(date_addr)) {
            log.warn("sceRtcSetTime_t bad address " + String.format("0x%08X", date_addr));
            return -1;
        }

        ScePspDateTime dateTime = ScePspDateTime.fromUnixTime(time);
        dateTime.write(mem, date_addr);
        return 0;
    }

    @HLEFunction(nid = 0x27C4594C, version = 150)
    public int sceRtcGetTime_t(int date_addr, int time_addr) {
        Memory mem = Processor.memory;

        if (!Memory.isAddressGood(date_addr) || !Memory.isAddressGood(time_addr)) {
            log.warn("sceRtcGetTime_t bad address " + String.format("0x%08X 0x%08X", date_addr, time_addr));
            return -1;
        }
        
        ScePspDateTime dateTime = new ScePspDateTime();
        dateTime.read(mem, date_addr);
        Calendar cal = Calendar.getInstance();
        cal.set(dateTime.year, dateTime.month - 1, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second);
        int unixtime = (int)(cal.getTime().getTime() / 1000L);
        log.debug("sceRtcGetTime_t psptime:" + dateTime + " unixtime:" + unixtime);
        mem.write32(time_addr, unixtime);

        return 0;
    }

    @HLEFunction(nid = 0xF006F264, version = 150)
    public int sceRtcSetDosTime(int date_addr, int time) {
        Memory mem = Processor.memory;

        if (!Memory.isAddressGood(date_addr)) {
            log.warn("sceRtcSetDosTime bad address " + String.format("0x%08X", date_addr));
        	return -1;
        }
        ScePspDateTime dateTime = ScePspDateTime.fromMSDOSTime(time);
        dateTime.write(mem, date_addr);

        return 0;
    }

    @HLEFunction(nid = 0x36075567, version = 150)
    public int sceRtcGetDosTime(int date_addr, int time_addr) {
        Memory mem = Processor.memory;

        if (!Memory.isAddressGood(date_addr) || !Memory.isAddressGood(time_addr)) {
            log.warn("sceRtcGetDosTime bad address " + String.format("0x%08X 0x%08X", date_addr, time_addr));
            return -1;
        }

        ScePspDateTime dateTime = new ScePspDateTime();
        dateTime.read(mem, date_addr);
        Calendar cal = Calendar.getInstance();
        cal.set(dateTime.year, dateTime.month - 1, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second);
        int dostime = (int)(cal.getTime().getTime() / 1000L);
        log.debug("sceRtcGetDosTime psptime:" + dateTime + " dostime:" + dostime);
        mem.write32(time_addr, dostime);
        
        return 0;
    }

    @HLEFunction(nid = 0x7ACE4C04, version = 150)
    public int sceRtcSetWin32FileTime(int date_addr, long time) {
    	Memory mem = Processor.memory;
    	
        if (!Memory.isAddressGood(date_addr)) {
            log.warn("sceRtcSetWin32FileTime bad address " + String.format("0x%08X", date_addr));
            return -1;
        }
        	
        ScePspDateTime dateTime = ScePspDateTime.fromFILETIMETime(time);
        dateTime.write(mem, date_addr);
        
        return 0;
    }

    @HLEFunction(nid = 0xCF561893, version = 150)
    public int sceRtcGetWin32FileTime(int date_addr, int time_addr) {
        Memory mem = Processor.memory;

        if (!Memory.isAddressGood(date_addr) || !Memory.isAddressGood(time_addr)) {
            log.warn("sceRtcGetWin32FileTime bad address " + String.format("0x%08X 0x%08X", date_addr, time_addr));
            return -1;
        }

        ScePspDateTime dateTime = new ScePspDateTime();
        dateTime.read(mem, date_addr);
        Calendar cal = Calendar.getInstance();
        cal.set(dateTime.year, dateTime.month - 1, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second);
        int filetimetime = (int)(cal.getTime().getTime() / 1000L);
        log.debug("sceRtcGetWin32FileTime psptime:" + dateTime + " filetimetime:" + filetimetime);
        mem.write64(time_addr, filetimetime);
        return 0;
    }

    /** Set a pspTime struct based on ticks. */
    @HLEFunction(nid = 0x7ED29E40, version = 150)
    public int sceRtcSetTick(Processor processor, int time_addr, int ticks_addr) {
        Memory mem = Processor.memory;

        log.debug("sceRtcSetTick");

        if (!Memory.isAddressGood(time_addr) || !Memory.isAddressGood(ticks_addr)) {
            log.warn("sceRtcSetTick bad address "
                    + String.format("0x%08X 0x%08X", time_addr, ticks_addr));
            return -1;
        }

        long ticks = mem.read64(ticks_addr) - rtcMagicOffset;
        ScePspDateTime time = ScePspDateTime.fromMicros(ticks);
        time.write(mem, time_addr);

        return 0;
    }

    /** Set ticks based on a pspTime struct. */
    @HLEFunction(nid = 0x6FF40ACC, version = 150)
    public int sceRtcGetTick(int time_addr, int ticks_addr) {
        Memory mem = Processor.memory;

        if (!Memory.isAddressGood(time_addr) || !Memory.isAddressGood(ticks_addr)) {
            log.warn("sceRtcGetTick bad address "
                    + String.format("0x%08X 0x%08X", time_addr, ticks_addr));
            return -1;
        }

        // use java library to convert a date to seconds, then multiply it by the tick resolution
        ScePspDateTime time = new ScePspDateTime();
        time.read(mem, time_addr);
        Calendar cal = new GregorianCalendar(time.year, time.month - 1, time.day,
            time.hour, time.minute, time.second);
        long ticks = rtcMagicOffset + (cal.getTimeInMillis() * 1000) + (time.microsecond % 1000);
        mem.write64(ticks_addr, ticks);

        log.debug("sceRtcGetTick " + time.toString() + " -> tick:" + ticks + " saved to 0x" + Integer.toHexString(ticks_addr));
        return 0;
    }

    @HLEFunction(nid = 0x9ED0AE87, version = 150)
    public int sceRtcCompareTick(TPointer64 firstPtr, TPointer64 secondPtr) {
        long tick1 = firstPtr.getValue();
        long tick2 = secondPtr.getValue();

        if (tick1 < tick2) return -1;
        if (tick1 > tick2) return 1;
        return 0;
    }
    
    

    @HLEFunction(nid = 0x44F45E05, version = 150)
    public int sceRtcTickAddTicks(TPointer64 dstPtr, TPointer64 srcPtr, long value) {
        log.debug("sceRtcTickAddTicks redirecting to hleRtcTickAdd64(1)");
        return hleRtcTickAdd64(dstPtr, srcPtr, value, 1);
    }

    @HLEFunction(nid = 0x26D25A5D, version = 150)
    public int sceRtcTickAddMicroseconds(TPointer64 dstPtr, TPointer64 srcPtr, long value) {
        log.debug("sceRtcTickAddMicroseconds redirecting to hleRtcTickAdd64(1)");
        return hleRtcTickAdd64(dstPtr, srcPtr, value, 1);
    }

    @HLEFunction(nid = 0xF2A4AFE5, version = 150)
    public int sceRtcTickAddSeconds(TPointer64 dstPtr, TPointer64 srcPtr, long value) {
        log.debug("sceRtcTickAddSeconds redirecting to hleRtcTickAdd64(1000000)");
        return hleRtcTickAdd64(dstPtr, srcPtr, value, 1000000L);
    }

    @HLEFunction(nid = 0xE6605BCA, version = 150)
    public int sceRtcTickAddMinutes(TPointer64 dstPtr, TPointer64 srcPtr, long value) {
        log.debug("sceRtcTickAddMinutes redirecting to hleRtcTickAdd64(60*1000000)");
        return hleRtcTickAdd64(dstPtr, srcPtr, value, PSP_TIME_SECONDS_IN_MINUTE * 1000000L);
    }

    
    
    @HLEFunction(nid = 0x26D7A24A, version = 150)
    public int sceRtcTickAddHours(TPointer64 dstPtr, TPointer64 srcPtr, int value) {
        log.debug("sceRtcTickAddHours redirecting to hleRtcTickAdd32(60*60*1000000)");
        return hleRtcTickAdd32(dstPtr, srcPtr, value, PSP_TIME_SECONDS_IN_HOUR * 1000000L);
    }

    @HLEFunction(nid = 0xE51B4B7A, version = 150)
    public int sceRtcTickAddDays(TPointer64 dstPtr, TPointer64 srcPtr, int value) {
        log.debug("sceRtcTickAddDays redirecting to hleRtcTickAdd32(24*60*60*1000000)");
        return hleRtcTickAdd32(dstPtr, srcPtr, value, PSP_TIME_SECONDS_IN_DAY * 1000000L);
    }

    @HLEFunction(nid = 0xCF3A2CA8, version = 150)
    public int sceRtcTickAddWeeks(TPointer64 dstPtr, TPointer64 srcPtr, int value) {
        log.debug("sceRtcTickAddWeeks redirecting to hleRtcTickAdd32(7*24*60*60*1000000)");
        return hleRtcTickAdd32(dstPtr, srcPtr, value, PSP_TIME_SECONDS_IN_WEEK * 1000000L);
    }

    @HLEFunction(nid = 0xDBF74F1B, version = 150)
    public int sceRtcTickAddMonths(TPointer64 dstPtr, TPointer64 srcPtr, int value) {
        log.debug("sceRtcTickAddMonths redirecting to hleRtcTickAdd32(30*24*60*60*1000000)");
        return hleRtcTickAdd32(dstPtr, srcPtr, value, PSP_TIME_SECONDS_IN_MONTH * 1000000L);
    }

    @HLEFunction(nid = 0x42842C77, version = 150)
    public int sceRtcTickAddYears(TPointer64 dstPtr, TPointer64 srcPtr, int value) {
        log.debug("sceRtcTickAddYears redirecting to hleRtcTickAdd32(365*24*60*60*1000000)");
        return hleRtcTickAdd32(dstPtr, srcPtr, value, PSP_TIME_SECONDS_IN_YEAR * 1000000L);
    }
    
    

    @HLEUnimplemented
    @HLEFunction(nid = 0xC663B3B9, version = 150)
    public int sceRtcFormatRFC2822() {
        log.warn("Unimplemented NID function sceRtcFormatRFC2822 [0xC663B3B9]");

        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7DE6711B, version = 150)
    public int sceRtcFormatRFC2822LocalTime() {
        log.warn("Unimplemented NID function sceRtcFormatRFC2822LocalTime [0x7DE6711B]");

        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0498FB3C, version = 150)
    public int sceRtcFormatRFC3339() {
        log.warn("Unimplemented NID function sceRtcFormatRFC3339 [0x0498FB3C]");

        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x27F98543, version = 150)
    public int sceRtcFormatRFC3339LocalTime() {
        log.warn("Unimplemented NID function sceRtcFormatRFC3339LocalTime [0x27F98543]");

        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDFBC5F16, version = 150)
    public int sceRtcParseDateTime() {
        log.warn("Unimplemented NID function sceRtcParseDateTime [0xDFBC5F16]");

        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x28E1E988, version = 150)
    public int sceRtcParseRFC3339() {
        log.warn("Unimplemented NID function sceRtcParseRFC3339 [0x28E1E988]");

        return 0xDEADC0DE;
    }

}