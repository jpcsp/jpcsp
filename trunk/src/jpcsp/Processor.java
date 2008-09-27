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

//import static jpcsp.AllegrexInstructions;
import jpcsp.Allegrex.*;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

public class Processor {

// "New-Style" Processor
    public CpuState cpu = new CpuState();

    public static final jpcsp.Memory memory = jpcsp.Memory.getInstance();
    public static Logger log = Logger.getLogger("cpu");
    
    Processor() {
        reset();
    }

    public void reset() {
        cpu.reset();
    }

    public void load(ByteBuffer buffer) {
        
    }

    public void save(ByteBuffer buffer) {
        
    }
    
    public void interpret() {

        int opcode = cpu.fetchOpcode();
        
        Common.Instruction insn = Decoder.instruction(opcode);
        
        insn.interpret(this, opcode);
    }

    public void interpretDelayslot() {

        int opcode = cpu.nextOpcode();
        
        Common.Instruction insn = Decoder.instruction(opcode);
        
        insn.interpret(this, opcode);
        
        cpu.nextPc();
    }

    public void step() {
        interpret();
    }    
}
