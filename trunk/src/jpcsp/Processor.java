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

import static jpcsp.Allegrex.GprState.NUMBER_REGISTERS;

import java.nio.ByteBuffer;

import jpcsp.Allegrex.Common.Instruction;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;

import org.apache.log4j.Logger;

public class Processor {
    public CpuState cpu = new CpuState();
    public static final Memory memory = Memory.getInstance();
    public static Logger log = Logger.getLogger("cpu");

    public Processor() {
        reset();
    }

    public void setCpu(CpuState cpu) {
    	this.cpu = cpu;
    }

    public void reset() {
        cpu.reset();
    }

    public void load(ByteBuffer buffer) {
        cpu.pc = buffer.getInt();
        cpu.npc = buffer.getInt();

        for (int i = 0; i < NUMBER_REGISTERS; i++) {
            cpu.setRegister(i, buffer.getInt());
        }
    }

    public void save(ByteBuffer buffer) {
        buffer.putInt(cpu.pc);
        buffer.putInt(cpu.npc);

        for (int i = 0; i < NUMBER_REGISTERS; i++) {
            buffer.putInt(cpu.getRegister(i));
        }
    }

    public void interpret() {
        int opcode = cpu.fetchOpcode();
        Instruction insn = Decoder.instruction(opcode);
        insn.interpret(this, opcode);
    }

    public void interpretDelayslot() {
        int opcode = cpu.nextOpcode();
        Instruction insn = Decoder.instruction(opcode);
        insn.interpret(this, opcode);
        cpu.nextPc();
    }

    public void step() {
        interpret();
    }
}