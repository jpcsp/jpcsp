/*
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/psprtc_8h.html


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

import java.util.Calendar;
import jpcsp.Emulator;
import jpcsp.Memory;

public class psprtc {
    
    private static psprtc instance;
    
    private psprtc() {
    }
    
    public static psprtc get_instance() {
        if (instance == null) {
            instance = new psprtc();
        }
        return instance;
    }
    
    public void sceRtcGetTickResolution() {
        /* 1000 ticks a second */
        Emulator.getProcessor().gpr[2] = 1000;
    }
    
    public void sceRtcGetCurrentTick(int a0) {
        Memory.get_instance().write64(a0, System.currentTimeMillis());
        Emulator.getProcessor().gpr[2] = 0;
    }
    
    public void sceRtcGetCurrentClockLocalTime(int a0) {
        Calendar rightNow = Calendar.getInstance();
        
        Memory mem = Memory.get_instance();
        mem.write16(a0, (short)rightNow.get(Calendar.YEAR));
        mem.write16(a0 +2, (short)rightNow.get(Calendar.MONTH));
        mem.write16(a0 +4, (short)rightNow.get(Calendar.DAY_OF_MONTH));
        mem.write16(a0 +6, (short)rightNow.get(Calendar.HOUR));
        mem.write16(a0 +8, (short)rightNow.get(Calendar.MINUTE));
        mem.write16(a0 +10, (short)rightNow.get(Calendar.SECOND));
        mem.write32(a0 +12, rightNow.get(Calendar.MILLISECOND));
                
        Emulator.getProcessor().gpr[2] = 0;
    }
    
}
