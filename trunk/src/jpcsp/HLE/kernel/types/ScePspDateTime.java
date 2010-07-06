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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import jpcsp.Memory;

public class ScePspDateTime {
    public int year;
    public int month;
    public int day;
    public int hour;
    public int minute;
    public int second;
    public int microsecond;

    /** All fields will be initialised to the time the object was created. */
    public ScePspDateTime() {
        Calendar cal = Calendar.getInstance();

        year = cal.get(Calendar.YEAR);
        month = 1 + cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        minute = cal.get(Calendar.MINUTE);
        second = cal.get(Calendar.SECOND);
        microsecond = cal.get(Calendar.MILLISECOND) * 1000;
    }

    public ScePspDateTime(int timezone) {
        Calendar cal = Calendar.getInstance();
        int minutes = timezone;
        int hours = 0;
        while(minutes > 59) {
            hours++;
            minutes -= 60;
        }

        String timeString = String.format("UTC+%02d%02d", hours, minutes);
        TimeZone tz = TimeZone.getTimeZone(timeString);
        cal.setTimeZone(tz);

        year = cal.get(Calendar.YEAR);
        month = 1 + cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        minute = cal.get(Calendar.MINUTE);
        second = cal.get(Calendar.SECOND);
        microsecond = cal.get(Calendar.MILLISECOND) * 1000;
    }

    public ScePspDateTime(int year, int month, int day,
            int hour, int minute, int second, int microsecond) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.microsecond = microsecond;
    }

    /** @param time Unix time, seconds since the epoch/1970. */
    public static ScePspDateTime fromUnixTime(long time) {
        Calendar cal = Calendar.getInstance();
        Date date = new Date(time);
        cal.setTime(date);

        int year = cal.get(Calendar.YEAR);
        int month = 1 + cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        int microsecond = cal.get(Calendar.MILLISECOND) * 1000;

        return new ScePspDateTime(year, month, day, hour, minute, second, microsecond);
    }

    /** @param microseconds */
    public static ScePspDateTime fromMicros(long micros) {
        Calendar cal = Calendar.getInstance();
        Date date = new Date(micros / 1000L);
        cal.setTime(date);

        int year = cal.get(Calendar.YEAR);
        int month = 1 + cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        int microsecond = (int)(micros % 1000000L);

        return new ScePspDateTime(year, month, day, hour, minute, second, microsecond);
    }

    public void read(Memory mem, int address) {
        year = mem.read16(address);
        month = mem.read16(address + 2);
        day = mem.read16(address + 4);
        hour = mem.read16(address + 6);
        minute = mem.read16(address + 8);
        second = mem.read16(address + 10);
        microsecond = mem.read32(address + 12);
    }

    public void write(Memory mem, int address) {
        mem.write16(address, (short)(year & 0xffff));
        mem.write16(address + 2, (short)(month & 0xffff));
        mem.write16(address + 4, (short)(day & 0xffff));
        mem.write16(address + 6, (short)(hour & 0xffff));
        mem.write16(address + 8, (short)(minute & 0xffff));
        mem.write16(address + 10, (short)(second & 0xffff));
        mem.write32(address + 12, microsecond);
    }

    public static int sizeof() {
        return 16;
    }

    @Override
    public String toString() {
        return String.format("%04d-%02d-%02d %02d:%02d:%02d", year, month, day, hour, minute, second);
    }
}