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
public class DisCOP0 {
    public String DisCop0(int value)
    {
       int rt = (value >> 16) & 0x1f;
       int rd = (value >> 11) & 0x1f;
       int cop0 = ((value >> 21) & 0x1f); //bits 21-25
       switch(cop0)
       { 
          case MFC0:
          return "mfc0 " + cpuregs[rt] + ", " + cop0regs[rd];
          case MTC0:
          return "mtc0 " + cpuregs[rt] + ", " + cop0regs[rd];
          default:
          return "unknown cop0 opcode " + Integer.toHexString(cop0);
          
         }
                
    }
}
