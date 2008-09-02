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
package jpcsp.HLE;

import jpcsp.Memory;

/** http://psp.jim.sh/pspsdk-doc/structScePspDateTime.html */
public class ScePspDateTime {
    public int year;
    public int month;
    public int day;
    public int hour;
    public int minute;
    public int second;
    public int microsecond;

    public ScePspDateTime() {
        this.year = 0;
        this.month = 0;
        this.day = 0;
        this.hour = 0;
        this.minute = 0;
        this.second = 0;
        this.microsecond = 0;
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
}
