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

import java.nio.ByteBuffer;

import jpcsp.Allegrex.*;
import jpcsp.Debugger.StepLogger;

import org.apache.log4j.Logger;

public class Processor {
    public static boolean ENABLE_STEP_TRACE = false;
    public static boolean ENABLE_INSN_EXECUTE_COUNT = false;
    private final static boolean ENABLE_INSN_CACHE = false;
    public CpuState cpu = new CpuState();
    public static final jpcsp.Memory memory = jpcsp.Memory.getInstance();
    public static Logger log = Logger.getLogger("cpu");

    public Processor() {
        insnCache = new CacheLine[INSN_CACHE_SIZE];
        for (int i = 0; i < INSN_CACHE_SIZE; i++)
            insnCache[i] = new CacheLine();
        reset();
    }

    public void reset() {
        invalidateICache();
        cpu.reset();
    }

    public void load(ByteBuffer buffer) {
        cpu.pc = buffer.getInt();
        cpu.npc = buffer.getInt();

        for(int i = 0; i < 32; i++)
            cpu.gpr[i] = buffer.getInt();
    }

    public void save(ByteBuffer buffer) {
        buffer.putInt(cpu.pc);
        buffer.putInt(cpu.npc);

        for(int i = 0; i < 32; i++)
            buffer.putInt(cpu.gpr[i]);
    }

    static class CacheLine {
        boolean valid;
        int address;
        int opcode;
        Common.Instruction insn;
    }

    private final int INSN_CACHE_SIZE = 0x1000;
    private final int INSN_CACHE_MASK = INSN_CACHE_SIZE - 1;
    private CacheLine[] insnCache;
    private long insnCacheHits, insnCacheMisses, insnCount;

    public void invalidateICache() {
        if (insnCount != 0) {
            Processor.log.info("icache hits:" + insnCacheHits + " (" + (insnCacheHits * 100 / insnCount) + "%)"
                + " misses:"  + insnCacheMisses + " (" + (insnCacheMisses * 100 / insnCount) + "%)");
        }
        for (CacheLine line : insnCache) line.valid = false;
        insnCacheHits = insnCacheMisses = insnCount = 0;
    }

    private CacheLine fetchDecodedInstruction() {
        CacheLine line = insnCache[cpu.pc & INSN_CACHE_MASK];
        if (!line.valid || line.address != cpu.pc) {
            line.valid = true;
            line.address = cpu.pc;
            line.opcode = memory.read32(cpu.pc);
            line.insn = Decoder.instruction(line.opcode);
            insnCacheMisses++;
        }
        else insnCacheHits++;

        insnCount++;
        cpu.pc = cpu.npc = cpu.pc + 4;
        return line;
    }

    private CacheLine nextDecodedInstruction() {
        CacheLine line = insnCache[cpu.pc & INSN_CACHE_MASK];
        if (!line.valid || line.address != cpu.pc) {
            line.valid = true;
            line.address = cpu.pc;
            line.opcode = memory.read32(cpu.pc);
            line.insn = Decoder.instruction(line.opcode);
            insnCacheMisses++;
        } else {
            insnCacheHits++;
        }
        insnCount++;
        cpu.pc += 4;
        return line;
    }

    public void interpret() {

        if (ENABLE_STEP_TRACE)
            StepLogger.append(cpu);
        if (ENABLE_INSN_CACHE) {
            CacheLine line = fetchDecodedInstruction();
            line.insn.interpret(this, line.opcode);

            if (ENABLE_INSN_EXECUTE_COUNT)
                line.insn.increaseCount();
        } else {
            int opcode = cpu.fetchOpcode();
            Common.Instruction insn = Decoder.instruction(opcode);
            insn.interpret(this, opcode);

            if (ENABLE_INSN_EXECUTE_COUNT)
                insn.increaseCount();
        }
    }

    public void interpretDelayslot() {

        if (ENABLE_STEP_TRACE)
            StepLogger.append(cpu);

        if (ENABLE_INSN_CACHE) {
            CacheLine line = nextDecodedInstruction();
            line.insn.interpret(this, line.opcode);

            if (ENABLE_INSN_EXECUTE_COUNT)
                line.insn.increaseCount();
        } else {
            int opcode = cpu.nextOpcode();
            Common.Instruction insn = Decoder.instruction(opcode);
            insn.interpret(this, opcode);

            if (ENABLE_INSN_EXECUTE_COUNT)
                insn.increaseCount();
        }
        cpu.nextPc();
    }

    public void step() {
        interpret();
    }
}