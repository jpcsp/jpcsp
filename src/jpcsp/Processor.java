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

public class Processor {

    public int pc;
    public int hi,  lo;
    public int cpuregisters[] = new int[32];//32 base registers

    Processor() {
        Memory.get_instance(); //intialaze memory
        reset();

    }

    private void reset() {
        //intialaze psp
        pc = 0x00000000;
        hi = lo = 0;
        for (int i = 0; i < 32; i++) {
            cpuregisters[i] = 0;//reset registers
        }

    }
    public void stepcpu()
    {
      int value = Memory.get_instance().read32(pc);
         
      //System.out.println(pc);
      int rs = (value >> 21) & 0x1f;
      int rt = (value >> 16) & 0x1f;
      int rd = (value >> 11) & 0x1f;
      int imm = value & 0xffff;
      if ((imm & 0x8000) == 0x8000) {
            imm |= 0xffff0000;
        }
        int opcode = (value >> 26) & 0x3f;
        switch(opcode)
        {
            case 9: //addiu
                cpuregisters[rt] = cpuregisters[rs] + imm;
                break;
            case 15: //LUI
                cpuregisters[rt] = imm << 16 ; 
                break;
            default:
                System.out.println("Unsupported instruction " + Integer.toHexString(opcode));
                break;
        }
      pc += 4;  
    }
    
}
