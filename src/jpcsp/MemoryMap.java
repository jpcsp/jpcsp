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
    public static final int START_SCRATCHPAD         = 0x00010000;
    public static final int END_SCRATCHPAD           = 0x00013FFF;
    public static final int SIZE_SCRATCHPAD          = END_SCRATCHPAD -
                                                       START_SCRATCHPAD + 1;
    
    public static final int START_VRAM               = 0x04000000; // KU0 
    public static final int END_VRAM                 = 0x041FFFFF; // KU0
    public static final int SIZE_VRAM                = END_VRAM -
                                                       START_VRAM + 1;
    
    public static final int START_RAM                = 0x08000000;
    public static final int END_RAM                  = 0x09FFFFFF;
    public static final int SIZE_RAM                 = END_RAM - START_RAM + 1;
    
    public static final int START_IO_0               = 0x1C000000;
    public static final int END_IO_0                 = 0x1FBFFFFF;
    
    public static final int START_IO_1               = 0x1FD00000;
    public static final int END_IO_1                 = 0x1FFFFFFF;
    
    public static final int START_EXCEPTIO_VEC       = 0x1FC00000;
    public static final int END_EXCEPTIO_VEC         = 0x1FCFFFFF;
    
    public static final int START_KERNEL             = 0x88000000; // K0
    public static final int END_KERNEL               = 0x887FFFFF; // K0
    
    public static final int START_USERSPACE          = 0x08800000; // KU0
    public static final int END_USERSPACE            = 0x09FFFFFF; // KU0
    
    public static final int START_UNCACHED_RAM_VIDEO = 0x44000000;
    public static final int END_UNCACHED_RAM_VIDEO   = 0x441FFFFF;
}
