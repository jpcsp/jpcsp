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

public class ScePspDateTime extends pspAbstractMemoryMappedStructure {
	public static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	public static final int SIZEOF = 16;
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

    /** @param time MSDOS time, seconds since the epoch/1980. */
    public static ScePspDateTime fromMSDOSTime(long time) {
        // Calculate each time parameter.
        long milliseconds = time / 10000;
        long days = milliseconds / (24 * 60 * 60 * 1000);
        milliseconds -= days * (24 * 60 * 60 * 1000);
        long hours = milliseconds / (60 * 60 * 1000);
        milliseconds -= hours * (60 * 60 * 1000);
        long minutes = milliseconds / (60 * 1000);
        milliseconds -= minutes * (60 * 1000);
        long seconds = milliseconds / 1000;
        milliseconds -= seconds * 1000;
        // Initialize a new calendar and set it for the rigth epoch.
        Calendar cal = Calendar.getInstance();
        cal.set(1980, Calendar.JANUARY, 1, 0, 0, 0);
        cal.add(Calendar.DATE, (int)days);
        cal.add(Calendar.HOUR_OF_DAY, (int)hours);
        cal.add(Calendar.MINUTE, (int)minutes);
        cal.add(Calendar.SECOND, (int)seconds);
        cal.add(Calendar.MILLISECOND, (int)milliseconds);

        int year = cal.get(Calendar.YEAR);
        int month = 1 + cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        int microsecond = cal.get(Calendar.MILLISECOND) * 1000;

        return new ScePspDateTime(year, month, day, hour, minute, second, microsecond);
    }

    /** @param time FILETIME time, 100 nanoseconds since the epoch/1601. */
    public static ScePspDateTime fromFILETIMETime(long time) {
        // Calculate each time parameter.
        long milliseconds = time / 10000;
        long days = milliseconds / (24 * 60 * 60 * 1000);
        milliseconds -= days * (24 * 60 * 60 * 1000);
        long hours = milliseconds / (60 * 60 * 1000);
        milliseconds -= hours * (60 * 60 * 1000);
        long minutes = milliseconds / (60 * 1000);
        milliseconds -= minutes * (60 * 1000);
        long seconds = milliseconds / 1000;
        milliseconds -= seconds * 1000;
        // Initialize a new calendar and set it for the rigth epoch.
        Calendar cal = Calendar.getInstance();
        cal.set(1601, Calendar.JANUARY, 1, 0, 0, 0);
        cal.add(Calendar.DATE, (int)days);
        cal.add(Calendar.HOUR_OF_DAY, (int)hours);
        cal.add(Calendar.MINUTE, (int)minutes);
        cal.add(Calendar.SECOND, (int)seconds);
        cal.add(Calendar.MILLISECOND, (int)milliseconds);

        int year = cal.get(Calendar.YEAR);
        int month = 1 + cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);
        int microsecond = cal.get(Calendar.MILLISECOND) * 1000;

        return new ScePspDateTime(year, month, day, hour, minute, second, microsecond);
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
        Calendar cal = Calendar.getInstance(GMT);
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


	@Override
	protected void read() {
		year = read16();
		month = read16();
		day = read16();
		hour = read16();
		minute = read16();
		second = read16();
		microsecond = read32();
	}

	@Override
	protected void write() {
		write16((short) year);
		write16((short) month);
		write16((short) day);
		write16((short) hour);
		write16((short) minute);
		write16((short) second);
		write32(microsecond);
	}

	@Override
	public int sizeof() {
		return SIZEOF;
	}

    @Override
    public String toString() {
        return String.format("%04d-%02d-%02d %02d:%02d:%02d micros=%d", year, month, day, hour, minute, second, microsecond);
    }
}