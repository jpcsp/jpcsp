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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import jpcsp.Clock.TimeNanos;
import jpcsp.Emulator;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointer64;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

@HLELogging
public class sceRtc extends HLEModule {
    public static Logger log = Modules.getLogger("sceRtc");

    @Override
    public String getName() {
    	return "sceRtc";
	}

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
    protected static SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    protected long hleGetCurrentTick() {
    	TimeNanos timeNanos = Emulator.getClock().currentTimeNanos();
    	return (timeNanos.micros + timeNanos.millis * 1000) + timeNanos.seconds * 1000000L + rtcMagicOffset;
    }

    /** 64 bit addend */
    protected int hleRtcTickAdd64(TPointer64 dstPtr, TPointer64 srcPtr, long value, long multiplier) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleRtcTickAdd64 dstPtr=%s, srcPtr=%s(%d), %d * %d", dstPtr, srcPtr, srcPtr.getValue(), value, multiplier));
    	}

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

    protected Date getDateFromTick(long tick) {
    	return new Date((tick - rtcMagicOffset) / 1000L);
    }

    protected String formatRFC3339(Date date) {
    	String result = rfc3339.format(date);
    	// SimpleDateFormat outputs the timezone offset in the format "hhmm"
    	// instead of "hh:mm" as required by RFC3339.
    	result = result.replaceFirst("(\\d\\d)(\\d\\d)$", "$1:$2");

    	return result;
    }

    protected TimeZone getLocalTimeZone() {
    	return TimeZone.getDefault();
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

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceRtcGetCurrentTick returning %d", currentTick.getValue()));
        }

        return 0;
    }

    @HLEFunction(nid = 0x011F03C1, version = 150)
    public long sceRtcGetAccumulativeTime() {
        // Returns the difference between the last reincarnated time and the current tick.
        // Just return our current tick, since there's no need to mimick such behaviour.
        return hleGetCurrentTick();
    }

    @HLEFunction(nid = 0x029CA3B3, version = 150)
    public long sceRtcGetAccumlativeTime() {
        // Typo. Same as sceRtcGetAccumulativeTime.
        return hleGetCurrentTick();
    }

    @HLEFunction(nid = 0x4CFA57B0, version = 150)
    public int sceRtcGetCurrentClock(TPointer addr, int tz) {
        ScePspDateTime pspTime = new ScePspDateTime(tz);
        pspTime.write(addr);

        return 0;
    }

    @HLEFunction(nid = 0xE7C27D1B, version = 150)
    public int sceRtcGetCurrentClockLocalTime(TPointer addr) {
        ScePspDateTime pspTime = new ScePspDateTime();
        pspTime.write(addr);

        return 0;
    }

    @HLEFunction(nid = 0x34885E0D, version = 150)
    public int sceRtcConvertUtcToLocalTime(TPointer64 utcPtr, TPointer64 localPtr) {
    	// Add the offset of the local time zone to UTC
        TimeZone localTimeZone = getLocalTimeZone();
        hleRtcTickAdd64(localPtr, utcPtr, localTimeZone.getRawOffset(), 1000L);

        return 0;
    }

    @HLEFunction(nid = 0x779242A2, version = 150)
    public int sceRtcConvertLocalTimeToUTC(TPointer64 localPtr, TPointer64 utcPtr) {
    	// Subtract the offset of the local time zone to UTC
        TimeZone localTimeZone = getLocalTimeZone();
        hleRtcTickAdd64(utcPtr, localPtr, -localTimeZone.getRawOffset(), 1000L);

        return 0;
    }

    @HLEFunction(nid = 0x42307A17, version = 150)
    public boolean sceRtcIsLeapYear(int year) {
        return (year % 4 == 0) && (year % 100 != 0) || (year % 400 == 0);
    }

    @HLEFunction(nid = 0x05EF322C, version = 150)
    public int sceRtcGetDaysInMonth(int year, int month) {
        Calendar cal = new GregorianCalendar(year, month - 1, 1);
        int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceRtcGetDaysInMonth returning %d", days));
        }

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

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceRtcGetDayOfWeek returning %d", dayOfWeekNumber));
        }

        return dayOfWeekNumber;
    }

    /**
     * Validate pspDate component ranges
     *
     * @param date - pointer to pspDate struct to be checked
     * @return 0 on success, one of PSP_TIME_INVALID_* on error
     */
    @HLEFunction(nid = 0x4B1B5E82, version = 150)
    public int sceRtcCheckValid(ScePspDateTime time) {
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
        	log.debug(String.format("sceRtcCheckValid time=%s, cal=%s, returning 0x%08X", time, cal, result));
        }

        return result;
    }

    @HLEFunction(nid = 0x3A807CC8, version = 150)
    public int sceRtcSetTime_t(TPointer dateAddr, int time) {
        ScePspDateTime dateTime = ScePspDateTime.fromUnixTime(time);
        dateTime.write(dateAddr);

        return 0;
    }

    @HLEFunction(nid = 0x27C4594C, version = 150)
    public int sceRtcGetTime_t(ScePspDateTime dateTime, TPointer32 timeAddr) {
        Calendar cal = Calendar.getInstance();
        cal.set(dateTime.year, dateTime.month - 1, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second);
        int unixtime = (int)(cal.getTime().getTime() / 1000L);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceRtcGetTime_t returning %d", unixtime));
        }

        timeAddr.setValue(unixtime);

        return 0;
    }

    @HLEFunction(nid = 0xF006F264, version = 150)
    public int sceRtcSetDosTime(TPointer dateAddr, int time) {
        ScePspDateTime dateTime = ScePspDateTime.fromMSDOSTime(time);
        dateTime.write(dateAddr);

        return 0;
    }

    @HLEFunction(nid = 0x36075567, version = 150)
    public int sceRtcGetDosTime(ScePspDateTime dateTime, TPointer32 timeAddr) {
        Calendar cal = Calendar.getInstance();
        cal.set(dateTime.year, dateTime.month - 1, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second);
        int dostime = (int)(cal.getTime().getTime() / 1000L);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceRtcGetDosTime returning %d", dostime));
        }

        timeAddr.setValue(dostime);
        
        return 0;
    }

    @HLEFunction(nid = 0x7ACE4C04, version = 150)
    public int sceRtcSetWin32FileTime(TPointer dateAddr, long time) {
        ScePspDateTime dateTime = ScePspDateTime.fromFILETIMETime(time);
        dateTime.write(dateAddr);
        
        return 0;
    }

    @HLEFunction(nid = 0xCF561893, version = 150)
    public int sceRtcGetWin32FileTime(ScePspDateTime dateTime, TPointer64 timeAddr) {
        Calendar cal = Calendar.getInstance();
        cal.set(dateTime.year, dateTime.month - 1, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second);
        int filetimetime = (int)(cal.getTime().getTime() / 1000L);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceRtcGetWin32FileTime returning %d", filetimetime));
        }

        timeAddr.setValue(filetimetime);

        return 0;
    }

    /** Set a pspTime struct based on ticks. */
    @HLEFunction(nid = 0x7ED29E40, version = 150)
    public int sceRtcSetTick(TPointer timeAddr, TPointer64 ticksAddr) {
        long ticks = ticksAddr.getValue() - rtcMagicOffset;
        ScePspDateTime time = ScePspDateTime.fromMicros(ticks);
        time.write(timeAddr);

        return 0;
    }

    /** Set ticks based on a pspTime struct. */
    @HLEFunction(nid = 0x6FF40ACC, version = 150)
    public int sceRtcGetTick(ScePspDateTime time, TPointer64 ticksAddr) {
        // use java library to convert a date to seconds, then multiply it by the tick resolution
        Calendar cal = new GregorianCalendar(time.year, time.month - 1, time.day, time.hour, time.minute, time.second);
        cal.set(Calendar.MILLISECOND, time.microsecond / 1000);
        cal.setTimeZone(ScePspDateTime.GMT);
        long ticks = rtcMagicOffset + (cal.getTimeInMillis() * 1000L) + (time.microsecond % 1000);
        ticksAddr.setValue(ticks);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceRtcGetTick returning %d", ticks));
        }

        return 0;
    }

    @HLEFunction(nid = 0x9ED0AE87, version = 150)
    public int sceRtcCompareTick(TPointer64 firstPtr, TPointer64 secondPtr) {
        long tick1 = firstPtr.getValue();
        long tick2 = secondPtr.getValue();

        if (tick1 < tick2) {
        	return -1;
        }
        if (tick1 > tick2) {
        	return 1;
        }
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
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7DE6711B, version = 150)
    public int sceRtcFormatRFC2822LocalTime() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0498FB3C, version = 150)
    public int sceRtcFormatRFC3339() {
    	return 0;
    }

    @HLEFunction(nid = 0x27F98543, version = 150)
    public int sceRtcFormatRFC3339LocalTime(TPointer resultString, TPointer64 srcPtr) {
    	Date date = getDateFromTick(srcPtr.getValue());
    	String result = formatRFC3339(date);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceRtcFormatRFC3339LocalTime src=%d, returning '%s'", srcPtr.getValue(), result));
    	}

    	resultString.setStringZ(result);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDFBC5F16, version = 150)
    public int sceRtcParseDateTime() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x28E1E988, version = 150)
    public int sceRtcParseRFC3339() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7D1FBED3, version = 150)
    public int sceRtcSetAlarmTick(TPointer64 srcPtr) {
    	return 0;
    }
}