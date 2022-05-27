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

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class ScePspDateTime extends pspAbstractMemoryMappedStructure implements IState {
	private static final int STATE_VERSION = 0;
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

    public ScePspDateTime(int year, int month, int day, int hour, int minute, int second, int microsecond) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.microsecond = microsecond;
    }

    /** @param time MSDOS time, as coded in FAT directory entries. */
    public static ScePspDateTime fromMSDOSTime(int time) {
        int year = 1980 + ((time >> 25) & 0x7F);
        int month = (time >> 21) & 0xF;
        int day = (time >> 16) & 0x1F;
        int hour = (time >> 11) & 0x1F;
        int minute = (time >> 5) & 0x3F;
        int second = ((time >> 0) & 0x1F) << 1; // 2 seconds resolution
        int microsecond = 0;

        return new ScePspDateTime(year, month, day, hour, minute, second, microsecond);
    }

    public int toMSDOSTime() {
    	int time = 0;
    	time |= ((year - 1980) & 0x7F) << 25;
    	time |= (month & 0xF) << 21;
    	time |= (day & 0x1F) << 16;
    	time |= (hour & 0x1F) << 11;
    	time |= (minute & 0x3F) << 5;
    	time |= ((second >> 1) & 0x1F) << 0; // 2 seconds resolution

    	return time;
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
        // Initialize a new calendar and set it for the right epoch.
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

    private static ScePspDateTime fromMicros(long micros, Calendar cal) {
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
    /** @param microseconds */
    public static ScePspDateTime fromMicros(long micros) {
        return fromMicros(micros, Calendar.getInstance(GMT));
    }

    /** @param microseconds */
    public static ScePspDateTime fromMicrosLocal(long micros) {
        return fromMicros(micros, Calendar.getInstance());
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
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
		year = stream.readUnsignedShort();
		month = stream.readUnsignedShort();
		day = stream.readUnsignedShort();
		hour = stream.readUnsignedShort();
		minute = stream.readUnsignedShort();
		second = stream.readUnsignedShort();
		microsecond = stream.readInt();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
		stream.writeShort(year);
		stream.writeShort(month);
		stream.writeShort(day);
		stream.writeShort(hour);
		stream.writeShort(minute);
		stream.writeShort(second);
		stream.writeInt(microsecond);
	}

	public boolean after(ScePspDateTime that) {
		if (year > that.year) {
			return true;
		}
		if (year < that.year) {
			return false;
		}

		if (month > that.month) {
			return true;
		}
		if (month < that.month) {
			return false;
		}

		if (day > that.day) {
			return true;
		}
		if (day < that.day) {
			return false;
		}

		if (hour > that.hour) {
			return true;
		}
		if (hour < that.hour) {
			return false;
		}

		if (minute > that.minute) {
			return true;
		}
		if (minute < that.minute) {
			return false;
		}

		if (second > that.second) {
			return true;
		}
		if (second < that.second) {
			return false;
		}

		if (microsecond > that.microsecond) {
			return true;
		}

		return false;
	}

	public boolean before(ScePspDateTime that) {
		if (year < that.year) {
			return true;
		}
		if (year > that.year) {
			return false;
		}

		if (month < that.month) {
			return true;
		}
		if (month > that.month) {
			return false;
		}

		if (day < that.day) {
			return true;
		}
		if (day > that.day) {
			return false;
		}

		if (hour < that.hour) {
			return true;
		}
		if (hour > that.hour) {
			return false;
		}

		if (minute < that.minute) {
			return true;
		}
		if (minute > that.minute) {
			return false;
		}

		if (second < that.second) {
			return true;
		}
		if (second > that.second) {
			return false;
		}

		if (microsecond < that.microsecond) {
			return true;
		}

		return false;
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