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

/*
 * GPS Data Structure for sceUsbGpsGetData().
 * Based on MapThis! homebrew v5.2
 */
public class pspUsbGpsData extends pspAbstractMemoryMappedStructure {
	public short year;
	public short month;
	public short date;
	public short hour;
	public short minute;
	public short second;
	public float garbage1;
	public float hdop; // Horizontal Dilution of Precision: <1 ideal, 1-2 excellent, 2-5 good, 5-10 moderate, 10-20 fair, >20 poor
	public float garbage2;
	public float latitude;
	public float longitude;
	public float altitude; // in meters
	public float garbage3;
	public float speed; // in meters/second ??
	public float bearing; // 0..360

	@Override
	protected void read() {
		year = (short) read16();
		month = (short) read16();
		date = (short) read16();
		hour = (short) read16();
		minute = (short) read16();
		second = (short) read16();
		garbage1 = readFloat();
		hdop = readFloat();
		garbage2 = readFloat();
		latitude = readFloat();
		longitude = readFloat();
		altitude = readFloat();
		garbage3 = readFloat();
		speed = readFloat();
		bearing = readFloat();
	}

	@Override
	protected void write() {
		write16(year);
		write16(month);
		write16(date);
		write16(hour);
		write16(minute);
		write16(second);
		writeFloat(garbage1);
		writeFloat(hdop);
		writeFloat(garbage2);
		writeFloat(latitude);
		writeFloat(longitude);
		writeFloat(altitude);
		writeFloat(garbage3);
		writeFloat(speed);
		writeFloat(bearing);
	}

	public void setCalendar(Calendar cal) {
		year = (short) cal.get(Calendar.YEAR);
		month = (short) cal.get(Calendar.MONTH);
		date = (short) cal.get(Calendar.DATE);
		hour = (short) cal.get(Calendar.HOUR_OF_DAY);
		minute = (short) cal.get(Calendar.MINUTE);
		second = (short) cal.get(Calendar.SECOND);
	}

	@Override
	public int sizeof() {
		return 48;
	}
}
