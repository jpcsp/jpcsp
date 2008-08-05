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
public class DisSpecial3 {

    public String special3(int value) {
        
        int rs = (value >> 21) & 0x1f;
        int rt = (value >> 16) & 0x1f;
        int special3 = (value & 0x3f);
        
        switch (special3) {
            case EXT: {
                int lsb = (value >> 6) & 0x1f;
                int dist = (value >> 11) & 0x1f;
                return "ext " + gprNames[rt] + ", " + gprNames[rs] + ", " + lsb + ", " + (dist + 1);
            }

            case INS: {
                int lsb = (value >> 6) & 0x1f;
                int msb = (value >> 11) & 0x1f;
                return "ins " + gprNames[rt] + ", " + gprNames[rs] + ", " + lsb + ", " + (msb - lsb + 1);
            }

            case BSHFL: {
                int bshfl = (value >> 6) & 0x1f;
        
                switch (bshfl) {
                    case WSBH:
                        return Dis_RDRT("wsbh", value);

                    case WSBW:
                        return Dis_RDRT("wsbw", value);

                    case BITREV:
                        return Dis_RDRT("bitrev", value);

                    case SEB:
                        return Dis_RDRT("seb", value);

                    case SEH:
                        return Dis_RDRT("seh", value);

                    default:
                        return "Unknown bshfl table instruction " + Integer.toHexString(bshfl);
                }
            }
            default:
                return "Unknown Special 3 instruction " + Integer.toHexString(special3);
        }
    }
}
