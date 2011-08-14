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
import java.util.Calendar;
import java.util.GregorianCalendar;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceRtc implements HLEModule {
    protected static Logger log = Modules.getLogger("sceRtc");

    @Override
    public String getName() { return "sceRtc"; }

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

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
    protected void hleRtcTickAdd64(Processor processor, long multiplier) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int dest_addr = cpu.gpr[4];
        int src_addr = cpu.gpr[5];
        long value = ((((long)cpu.gpr[6]) & 0xFFFFFFFFL) | (((long)cpu.gpr[7])<<32));

        log.debug("hleRtcTickAdd64 " + multiplier + " * " + value);

        if (Memory.isAddressGood(src_addr) && Memory.isAddressGood(dest_addr)) {
            long src = mem.read64(src_addr);
            mem.write64(dest_addr, src + multiplier * value);
            cpu.gpr[2] = 0;
        } else {
            log.warn("hleRtcTickAdd64 bad address "
                + String.format("0x%08X 0x%08X", src_addr, dest_addr));
            cpu.gpr[2] = -1;
        }
    }

    /** 32 bit addend */
    protected void hleRtcTickAdd32(Processor processor, long multiplier) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int dest_addr = cpu.gpr[4];
        int src_addr = cpu.gpr[5];
        int value = cpu.gpr[6];

        log.debug("hleRtcTickAdd32 " + multiplier + " * " + value);

        if (Memory.isAddressGood(src_addr) && Memory.isAddressGood(dest_addr)) {
            long src = mem.read64(src_addr);
            mem.write64(dest_addr, src + multiplier * value);
            cpu.gpr[2] = 0;
        } else {
            log.warn("hleRtcTickAdd32 bad address "
                + String.format("0x%08X 0x%08X", src_addr, dest_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceRtcGetTickResolution(Processor processor) {
        CpuState cpu = processor.cpu;

        // resolution = micro seconds
        cpu.gpr[2] = 1000000;
    }

    public void sceRtcGetCurrentTick(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int addr = cpu.gpr[4];
        mem.write64(addr, hleGetCurrentTick());

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("sceRtcGetCurrentTick 0x%08X, returning %d", addr, mem.read64(addr)));
        }

        cpu.gpr[2] = 0;
    }

    public void sceRtcGetAccumulativeTime(Processor processor) {
        CpuState cpu = processor.cpu;

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceRtcGetAccumulativeTime");
        }
        // Returns the difference between the last reincarnated time and the current tick.
        // Just return our current tick, since there's no need to mimick such behaviour.
        long accumTick = hleGetCurrentTick();

        cpu.gpr[2] = (int)(accumTick & 0xffffffffL);
        cpu.gpr[3] = (int)((accumTick >> 32) & 0xffffffffL);
    }

    public void sceRtcGetAccumlativeTime(Processor processor) {
        CpuState cpu = processor.cpu;

        // Typo. Refers to the same function.
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceRtcGetAccumlativeTime");
        }
        long accumTick = hleGetCurrentTick();

        cpu.gpr[2] = (int)(accumTick & 0xffffffffL);
        cpu.gpr[3] = (int)((accumTick >> 32) & 0xffffffffL);
    }

    public void sceRtcGetCurrentClock(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int addr = cpu.gpr[4];
        int tz = cpu.gpr[5]; //Time Zone (minutes from UTC)
        ScePspDateTime pspTime = new ScePspDateTime(tz);
        pspTime.write(mem, addr);

        log.debug("sceRtcGetCurrentClock addr=" + Integer.toHexString(addr) + " time zone=" + tz);

        cpu.gpr[2] = 0;
    }

    public void sceRtcGetCurrentClockLocalTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int addr = cpu.gpr[4];
        ScePspDateTime pspTime = new ScePspDateTime();
        pspTime.write(mem, addr);

        cpu.gpr[2] = 0;
    }

    public void sceRtcConvertUtcToLocalTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int utc_addr = cpu.gpr[4];
        int local_addr = cpu.gpr[5];

        log.debug("PARTIAL: sceRtcConvertUtcToLocalTime");

        long utc = mem.read64(utc_addr);
        long local = utc; // TODO
        mem.write64(local_addr, local);

        cpu.gpr[2] = 0;
    }

    public void sceRtcConvertLocalTimeToUTC(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int local_addr = cpu.gpr[4];
        int utc_addr = cpu.gpr[5];

        log.debug("PARTIAL: sceRtcConvertLocalTimeToUTC");

        long local = mem.read64(local_addr);
        long utc = local; // TODO
        mem.write64(utc_addr, utc);

        cpu.gpr[2] = 0;
    }

    public void sceRtcIsLeapYear(Processor processor) {
        CpuState cpu = processor.cpu;

        log.debug("sceRtcIsLeapYear");

        int year = cpu.gpr[4];

        if((year % 4 == 0) && (year % 100 != 0) || (year % 400 == 0))
            cpu.gpr[2] = 1;
        else
            cpu.gpr[2] = 0;
    }

    public void sceRtcGetDaysInMonth(Processor processor) {
        CpuState cpu = processor.cpu;

        int year = cpu.gpr[4];
        int month = cpu.gpr[5];

        Calendar cal = new GregorianCalendar(year, month - 1, 1);

        int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        log.debug(String.format("sceRtcGetDaysInMonth %04d-%02d ret:%d", year, month, days));
        cpu.gpr[2] = days;
    }

    // pspsdk says 0=monday but I tested and 0=sunday... (fiveofhearts)
    public void sceRtcGetDayOfWeek(Processor processor) {
        CpuState cpu = processor.cpu;
        int year = cpu.gpr[4];
        int month = cpu.gpr[5];
        int day = cpu.gpr[6];

        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day);

        int number = cal.get(Calendar.DAY_OF_WEEK);
        number = (number - 1 + 7) % 7;

        log.debug(String.format("sceRtcGetDayOfWeek %04d-%02d-%02d ret:%d", year, month, day, number));
        cpu.gpr[2] = number;
    }

    /**
     * Validate pspDate component ranges
     *
     * @param date - pointer to pspDate struct to be checked
     * @return 0 on success, one of PSP_TIME_INVALID_* on error
     */
    public void sceRtcCheckValid(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int time_addr = cpu.gpr[4];

        if (Memory.isAddressGood(time_addr)) {
            ScePspDateTime time = new ScePspDateTime();
            time.read(mem, time_addr);
            Calendar cal = new GregorianCalendar(time.year, time.month - 1, time.day,
                time.hour, time.minute, time.second);
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

            cpu.gpr[2] = result;
        } else {
            log.warn("sceRtcGetTick bad address " + String.format("0x%08X", time_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceRtcSetTime_t(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int date_addr = cpu.gpr[4];
        int time = cpu.gpr[5];

        if (Memory.isAddressGood(date_addr)) {
            ScePspDateTime dateTime = ScePspDateTime.fromUnixTime(time);
            dateTime.write(mem, date_addr);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceRtcSetTime_t bad address " + String.format("0x%08X", date_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceRtcGetTime_t(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int date_addr = cpu.gpr[4];
        int time_addr = cpu.gpr[5];

        if (Memory.isAddressGood(date_addr) && Memory.isAddressGood(time_addr)) {
            ScePspDateTime dateTime = new ScePspDateTime();
            dateTime.read(mem, date_addr);
            Calendar cal = Calendar.getInstance();
            cal.set(dateTime.year, dateTime.month - 1, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second);
            int unixtime = (int)(cal.getTime().getTime() / 1000L);
            log.debug("sceRtcGetTime_t psptime:" + dateTime + " unixtime:" + unixtime);
            mem.write32(time_addr, unixtime);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceRtcGetTime_t bad address " + String.format("0x%08X 0x%08X", date_addr, time_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceRtcSetDosTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int date_addr = cpu.gpr[4];
        int time = cpu.gpr[5];

        if (Memory.isAddressGood(date_addr)) {
            ScePspDateTime dateTime = ScePspDateTime.fromMSDOSTime(time);
            dateTime.write(mem, date_addr);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceRtcSetDosTime bad address " + String.format("0x%08X", date_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceRtcGetDosTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int date_addr = cpu.gpr[4];
        int time_addr = cpu.gpr[5];

        if (Memory.isAddressGood(date_addr) && Memory.isAddressGood(time_addr)) {
            ScePspDateTime dateTime = new ScePspDateTime();
            dateTime.read(mem, date_addr);
            Calendar cal = Calendar.getInstance();
            cal.set(dateTime.year, dateTime.month - 1, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second);
            int dostime = (int)(cal.getTime().getTime() / 1000L);
            log.debug("sceRtcGetDosTime psptime:" + dateTime + " dostime:" + dostime);
            mem.write32(time_addr, dostime);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceRtcGetDosTime bad address " + String.format("0x%08X 0x%08X", date_addr, time_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceRtcSetWin32FileTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int date_addr = cpu.gpr[4];
        long time = Utilities.getRegister64(cpu, cpu.gpr[5]);

        if (Memory.isAddressGood(date_addr)) {
            ScePspDateTime dateTime = ScePspDateTime.fromFILETIMETime(time);
            dateTime.write(mem, date_addr);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceRtcSetWin32FileTime bad address " + String.format("0x%08X", date_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceRtcGetWin32FileTime(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int date_addr = cpu.gpr[4];
        int time_addr = cpu.gpr[5];

        if (Memory.isAddressGood(date_addr) && Memory.isAddressGood(time_addr)) {
            ScePspDateTime dateTime = new ScePspDateTime();
            dateTime.read(mem, date_addr);
            Calendar cal = Calendar.getInstance();
            cal.set(dateTime.year, dateTime.month - 1, dateTime.day, dateTime.hour, dateTime.minute, dateTime.second);
            int filetimetime = (int)(cal.getTime().getTime() / 1000L);
            log.debug("sceRtcGetWin32FileTime psptime:" + dateTime + " filetimetime:" + filetimetime);
            mem.write64(time_addr, filetimetime);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceRtcGetWin32FileTime bad address " + String.format("0x%08X 0x%08X", date_addr, time_addr));
            cpu.gpr[2] = -1;
        }
    }

    /** Set a pspTime struct based on ticks. */
    public void sceRtcSetTick(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int time_addr = cpu.gpr[4];
        int ticks_addr = cpu.gpr[5];

        log.debug("sceRtcSetTick");

        if (Memory.isAddressGood(time_addr) && Memory.isAddressGood(ticks_addr)) {
            long ticks = mem.read64(ticks_addr) - rtcMagicOffset;
            ScePspDateTime time = ScePspDateTime.fromMicros(ticks);
            time.write(mem, time_addr);
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceRtcSetTick bad address "
                + String.format("0x%08X 0x%08X", time_addr, ticks_addr));
            cpu.gpr[2] = -1;
        }
    }

    /** Set ticks based on a pspTime struct. */
    public void sceRtcGetTick(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int time_addr = cpu.gpr[4];
        int ticks_addr = cpu.gpr[5];

        if (Memory.isAddressGood(time_addr) && Memory.isAddressGood(ticks_addr)) {
            // use java library to convert a date to seconds, then multiply it by the tick resolution
            ScePspDateTime time = new ScePspDateTime();
            time.read(mem, time_addr);
            Calendar cal = new GregorianCalendar(time.year, time.month - 1, time.day,
                time.hour, time.minute, time.second);
            long ticks = rtcMagicOffset + (cal.getTimeInMillis() * 1000) + (time.microsecond % 1000);
            mem.write64(ticks_addr, ticks);

            log.debug("sceRtcGetTick " + time.toString() + " -> tick:" + ticks + " saved to 0x" + Integer.toHexString(ticks_addr));
            cpu.gpr[2] = 0;
        } else {
            log.warn("sceRtcGetTick bad address "
                + String.format("0x%08X 0x%08X", time_addr, ticks_addr));
            cpu.gpr[2] = -1;
        }
    }

    public void sceRtcCompareTick(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int first = cpu.gpr[4];
        int second = cpu.gpr[5];

        log.debug("sceRtcCompareTick");

        if (Memory.isAddressGood(first) && Memory.isAddressGood(second)) {
            long tick1 = mem.read64(first);
            long tick2 = mem.read64(second);

            if (tick1 == tick2)
                cpu.gpr[2] = 0;
            else if (tick1 < tick2)
                cpu.gpr[2] = -1;
            else if (tick1 > tick2)
                cpu.gpr[2] = 1;
        } else {
            log.warn("sceRtcCompareTick bad address "
                + String.format("0x%08X 0x%08X", first, second));
            cpu.gpr[2] = -1;
        }
    }

    public void sceRtcTickAddTicks(Processor processor) {
        log.debug("sceRtcTickAddTicks redirecting to hleRtcTickAdd64(1)");
        hleRtcTickAdd64(processor, 1);
    }

    public void sceRtcTickAddMicroseconds(Processor processor) {
        log.debug("sceRtcTickAddMicroseconds redirecting to hleRtcTickAdd64(1)");
        hleRtcTickAdd64(processor, 1);
    }

    public void sceRtcTickAddSeconds(Processor processor) {
        log.debug("sceRtcTickAddSeconds redirecting to hleRtcTickAdd64(1000000)");
        hleRtcTickAdd64(processor, 1000000L);
    }

    public void sceRtcTickAddMinutes(Processor processor) {
        log.debug("sceRtcTickAddMinutes redirecting to hleRtcTickAdd64(60*1000000)");
        hleRtcTickAdd64(processor, PSP_TIME_SECONDS_IN_MINUTE*1000000L);
    }

    public void sceRtcTickAddHours(Processor processor) {
        log.debug("sceRtcTickAddHours redirecting to hleRtcTickAdd32(60*60*1000000)");
        hleRtcTickAdd32(processor, PSP_TIME_SECONDS_IN_HOUR*1000000L);
    }

    public void sceRtcTickAddDays(Processor processor) {
        log.debug("sceRtcTickAddDays redirecting to hleRtcTickAdd32(24*60*60*1000000)");
        hleRtcTickAdd32(processor, PSP_TIME_SECONDS_IN_DAY*1000000L);
    }

    public void sceRtcTickAddWeeks(Processor processor) {
        log.debug("sceRtcTickAddWeeks redirecting to hleRtcTickAdd32(7*24*60*60*1000000)");
        hleRtcTickAdd32(processor, PSP_TIME_SECONDS_IN_WEEK*1000000L);
    }

    public void sceRtcTickAddMonths(Processor processor) {
        log.debug("sceRtcTickAddMonths redirecting to hleRtcTickAdd32(30*24*60*60*1000000)");
        hleRtcTickAdd32(processor, PSP_TIME_SECONDS_IN_MONTH*1000000L);
    }

    public void sceRtcTickAddYears(Processor processor) {
        log.debug("sceRtcTickAddYears redirecting to hleRtcTickAdd32(365*24*60*60*1000000)");
        hleRtcTickAdd32(processor, PSP_TIME_SECONDS_IN_YEAR*1000000L);
    }

    public void sceRtcFormatRFC2822(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceRtcFormatRFC2822 [0xC663B3B9]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceRtcFormatRFC2822LocalTime(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceRtcFormatRFC2822LocalTime [0x7DE6711B]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceRtcFormatRFC3339(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceRtcFormatRFC3339 [0x0498FB3C]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceRtcFormatRFC3339LocalTime(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceRtcFormatRFC3339LocalTime [0x27F98543]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceRtcParseDateTime(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceRtcParseDateTime [0xDFBC5F16]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceRtcParseRFC3339(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceRtcParseRFC3339 [0x28E1E988]");

        cpu.gpr[2] = 0xDEADC0DE;
    }
    @HLEFunction(nid = 0xC41C2853, version = 150) public HLEModuleFunction sceRtcGetTickResolutionFunction;

    @HLEFunction(nid = 0x3F7AD767, version = 150) public HLEModuleFunction sceRtcGetCurrentTickFunction;

    @HLEFunction(nid = 0x011F03C1, version = 150) public HLEModuleFunction sceRtcGetAccumulativeTimeFunction;

    @HLEFunction(nid = 0x029CA3B3, version = 150) public HLEModuleFunction sceRtcGetAccumlativeTimeFunction;

    @HLEFunction(nid = 0x4CFA57B0, version = 150) public HLEModuleFunction sceRtcGetCurrentClockFunction;

    @HLEFunction(nid = 0xE7C27D1B, version = 150) public HLEModuleFunction sceRtcGetCurrentClockLocalTimeFunction;

    @HLEFunction(nid = 0x34885E0D, version = 150) public HLEModuleFunction sceRtcConvertUtcToLocalTimeFunction;

    @HLEFunction(nid = 0x779242A2, version = 150) public HLEModuleFunction sceRtcConvertLocalTimeToUTCFunction;

    @HLEFunction(nid = 0x42307A17, version = 150) public HLEModuleFunction sceRtcIsLeapYearFunction;

    @HLEFunction(nid = 0x05EF322C, version = 150) public HLEModuleFunction sceRtcGetDaysInMonthFunction;

    @HLEFunction(nid = 0x57726BC1, version = 150) public HLEModuleFunction sceRtcGetDayOfWeekFunction;

    @HLEFunction(nid = 0x4B1B5E82, version = 150) public HLEModuleFunction sceRtcCheckValidFunction;

    @HLEFunction(nid = 0x3A807CC8, version = 150) public HLEModuleFunction sceRtcSetTime_tFunction;

    @HLEFunction(nid = 0x27C4594C, version = 150) public HLEModuleFunction sceRtcGetTime_tFunction;

    @HLEFunction(nid = 0xF006F264, version = 150) public HLEModuleFunction sceRtcSetDosTimeFunction;

    @HLEFunction(nid = 0x36075567, version = 150) public HLEModuleFunction sceRtcGetDosTimeFunction;

    @HLEFunction(nid = 0x7ACE4C04, version = 150) public HLEModuleFunction sceRtcSetWin32FileTimeFunction;

    @HLEFunction(nid = 0xCF561893, version = 150) public HLEModuleFunction sceRtcGetWin32FileTimeFunction;

    @HLEFunction(nid = 0x7ED29E40, version = 150) public HLEModuleFunction sceRtcSetTickFunction;

    @HLEFunction(nid = 0x6FF40ACC, version = 150) public HLEModuleFunction sceRtcGetTickFunction;

    @HLEFunction(nid = 0x9ED0AE87, version = 150) public HLEModuleFunction sceRtcCompareTickFunction;

    @HLEFunction(nid = 0x44F45E05, version = 150) public HLEModuleFunction sceRtcTickAddTicksFunction;

    @HLEFunction(nid = 0x26D25A5D, version = 150) public HLEModuleFunction sceRtcTickAddMicrosecondsFunction;

    @HLEFunction(nid = 0xF2A4AFE5, version = 150) public HLEModuleFunction sceRtcTickAddSecondsFunction;

    @HLEFunction(nid = 0xE6605BCA, version = 150) public HLEModuleFunction sceRtcTickAddMinutesFunction;

    @HLEFunction(nid = 0x26D7A24A, version = 150) public HLEModuleFunction sceRtcTickAddHoursFunction;

    @HLEFunction(nid = 0xE51B4B7A, version = 150) public HLEModuleFunction sceRtcTickAddDaysFunction;

    @HLEFunction(nid = 0xCF3A2CA8, version = 150) public HLEModuleFunction sceRtcTickAddWeeksFunction;

    @HLEFunction(nid = 0xDBF74F1B, version = 150) public HLEModuleFunction sceRtcTickAddMonthsFunction;

    @HLEFunction(nid = 0x42842C77, version = 150) public HLEModuleFunction sceRtcTickAddYearsFunction;

    @HLEFunction(nid = 0xC663B3B9, version = 150) public HLEModuleFunction sceRtcFormatRFC2822Function;

    @HLEFunction(nid = 0x7DE6711B, version = 150) public HLEModuleFunction sceRtcFormatRFC2822LocalTimeFunction;

    @HLEFunction(nid = 0x0498FB3C, version = 150) public HLEModuleFunction sceRtcFormatRFC3339Function;

    @HLEFunction(nid = 0x27F98543, version = 150) public HLEModuleFunction sceRtcFormatRFC3339LocalTimeFunction;

    @HLEFunction(nid = 0xDFBC5F16, version = 150) public HLEModuleFunction sceRtcParseDateTimeFunction;

    @HLEFunction(nid = 0x28E1E988, version = 150) public HLEModuleFunction sceRtcParseRFC3339Function;

}