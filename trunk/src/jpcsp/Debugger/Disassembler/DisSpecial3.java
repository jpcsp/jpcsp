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

package jpcsp.Debugger.Disassembler;
import static jpcsp.R4000OpCodes.*;
import static jpcsp.Debugger.Disassembler.DisHelper.*;

/**
 *
 * @author shadow
 */
public class DisSpecial3 {
      public String special3(int value)
      {
          int rs = (value >> 21) & 0x1f;
          int rt = (value >> 16) & 0x1f;
          int special3 = (value & 0x3f);
          switch(special3)
          {
              case EXT:
                  int ext_size = (value >>11) & 0x1f;
                  int ext_pos = (value >>6) & 0x1f;
                  return "ext " + cpuregs[rt] + ", " + cpuregs[rs] + ", " + ext_pos + ", " + ext_size+1 ;
              case BSHFL:
                  int bshfl = (value >> 6) & 0x1f;
                  switch(bshfl)
                  {
                     case SEB:
                        return Dis_RDRT("seb",value);
                     case SEH:
                        return Dis_RDRT("seh",value);
                     default:
                         return "Unknown bshfl table instruction " + Integer.toHexString(bshfl);
                  }
              default:
                    return "Unknown Special 3 instruction " + Integer.toHexString(special3);
                    
             }
          
          
      }
}
