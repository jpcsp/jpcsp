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
package jpcsp.Debugger;

import java.util.Arrays;
import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.Decoder;
import jpcsp.Memory;
import jpcsp.HLE.ThreadMan;

public class StepFrame {

    // Optimize for speed and memory, just store the raw details and calculate
    // the formatted message the first time getMessage it called.
    private int pc;
    private int[] gpr = new int[32];

    private int opcode;
    private String asm;

    private int threadID;
    private String threadName;

    private boolean dirty;
    private String message;

    public StepFrame() {
        dirty = false;
        message = "";
    }

    public void make(CpuState cpu) {
        pc = cpu.pc;
        //gpr = Arrays.copyOf(cpu.gpr, 32); // this will allocate
        for (int i = 0; i < 32; i++) gpr[i] = cpu.gpr[i]; // this will copy
        threadID = ThreadMan.getInstance().getCurrentThreadID();
        threadName = ThreadMan.getInstance().getThreadName(threadID);

        if (cpu.memory.isAddressGood(cpu.pc)) {
            opcode = cpu.memory.read32(cpu.pc);
            Common.Instruction insn = Decoder.instruction(opcode);
            asm = insn.disasm(cpu.pc, opcode);
        } else {
            opcode = 0;
            asm = "?";
        }

        dirty = true;
    }

    private String getThreadInfo() {
        // Thread ID - 0x04600843
        // Th Name   - user_main
        return String.format("Thread ID - 0x%08X\n", threadID)
            + "Th Name   - " + threadName + "\n";
    }

    private String getRegistersInfo() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 32; i += 4) {
            sb.append(String.format("%s:0x%08X %s:0x%08X %s:0x%08X %s:0x%08X\n",
                Common.gprNames[i + 0].substring(1), gpr[i + 0],
                Common.gprNames[i + 1].substring(1), gpr[i + 1],
                Common.gprNames[i + 2].substring(1), gpr[i + 2],
                Common.gprNames[i + 3].substring(1), gpr[i + 3]));
        }

        return sb.toString();
    }

    private void makeMessage() {
        String address = String.format("0x%08X", pc);
        String rawdata = String.format("0x%08X", opcode);

        // 0x0895BD1C: 0x8CE60000 '....' - lw         $a2, 0($a3)
        message = getThreadInfo()
            //+ getModuleInfo()
            + getRegistersInfo()
            + address
            + ": " + rawdata
            //+ "'" + printabledata + "'"
            + " - " + asm;
    }

    public String getMessage() {
        if (dirty) {
            dirty = false;
            makeMessage();
        }
        return message;
    }
}
