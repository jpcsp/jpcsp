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
package jpcsp;

public class MemoryMap {
    public static final int START_SCRATCHPAD = 0x00010000;
    public static final int END_SCRATCHPAD = 0x00013fff;
    public static final int START_VRAM = 0x04000000;
    public static final int END_VRAM = 0x041fffff;
    public static final int START_RAM = 0x08000000;
    public static final int END_RAM = 0x09ffffff;
    public static final int START_IO_0 = 0x1c000000;
    public static final int END_IO_0 = 0x1fbfffff;
    public static final int START_IO_1 = 0x1fd00000;
    public static final int END_IO_1 = 0x1fffffff;
    public static final int START_EXCEPTIO_VEC = 0x1fc00000;
    public static final int END_EXCEPTIO_VEC = 0x1fcfffff;
}
