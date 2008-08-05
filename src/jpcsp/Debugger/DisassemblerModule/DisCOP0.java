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
 * @author shadow, hlide
 */
public class DisCOP0 {

    // (hlide) CACHE is not a COP0 instruction
    public String DisCache(int value) {
        int rs = (value >> 21) & 0x1f;
        int rd = (value >> 11) & 0x1f;
        int imm = ((value & 0xffff) << 16) >> 16;
        int cacheOp = (value >> 16) & 0x1f;
        
        return "cache " + Integer.toHexString(cacheOp) + ", " + imm + "(" + gprNames[rs] + ")";
    }

    public String DisCop0(int value) {
        int rt = (value >> 16) & 0x1f;
        int rd = (value >> 11) & 0x1f;
        int cop0 = ((value >> 21) & 0x1f); //bits 21-25

        switch (cop0) {
            case MFC0:
                return "mfc0 " + gprNames[rt] + ", " + cop0Names[rd];
            
            case MTC0:
                return "mtc0 " + gprNames[rt] + ", " + cop0Names[rd];
        
            default:
                return "unknown cop0 opcode " + Integer.toHexString(cop0);
        }
    }
}
