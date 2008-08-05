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
package jpcsp.Debugger.DisassemblerModule;

import static jpcsp.AllegrexOpcodes.*;
import static jpcsp.Debugger.DisassemblerModule.DisHelper.*;

/**
 *
 * @author shadow
 */
public class DisSpecial2 {

    public String Special2(int value) {
        int rt = (value >> 16) & 0x1f;
        int rd = (value >> 11) & 0x1f;
        int allegrexop = (value & 0x3f);
        
        switch (allegrexop) {
            case HALT:
                return "halt";
            
            case MFIC:
                return "mfic" + " " + gprNames[rt] + ", " + gprNames[rd];
            
            case MTIC:
                return "mtic" + " " + gprNames[rt] + ", " + gprNames[rd];

            default:
                return "Unknown special2 instruction " + Integer.toHexString(allegrexop);
        }
    }
}
