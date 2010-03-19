/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp.Allegrex;

import jpcsp.Emulator;
import jpcsp.Processor;

/**
 * Branch Control Unit, handles branching and jumping operations
 *
 * @author hli
 *
 */
public class BcuState extends LsuState {
    public int pc;
    public int npc;

    @Override
    public void reset() {
        pc = 0;
        npc = 0;
    }

    @Override
    public void resetAll() {
        super.resetAll();
        pc = 0;
        npc = 0;
    }

    public BcuState() {
        pc = 0;
        npc = 0;
    }

    public void copy(BcuState that) {
        super.copy(that);
        pc = that.pc;
        npc = that.npc;
    }

    public BcuState(BcuState that) {
        super(that);
        pc = that.pc;
        npc = that.npc;
    }

    public static int branchTarget(int npc, int simm16) {
        return npc + (simm16 << 2);
    }

    public static int jumpTarget(int npc, int uimm26) {
        return (npc & 0xf0000000) | (uimm26 << 2);
    }

    public int fetchOpcode() {
        npc = pc + 4;

        int opcode = memory.read32(pc);

        // by default, the next instruction to emulate is at the next address
        pc = npc;

        return opcode;
    }

    public int nextOpcode() {
        int opcode = memory.read32(pc);

        // by default, the next instruction to emulate is at the next address
        pc += 4;

        return opcode;
    }

    public void nextPc() {
        pc = npc;
        npc = pc + 4;
    }

    public boolean doJR(int rs) {
        npc = gpr[rs];
        return true;
    }

    public boolean doJALR(int rd, int rs) {
        if (rd != 0) {
            gpr[rd] = pc + 4;
        }
        npc = gpr[rs];
        return true;
    }

    public boolean doBLTZ(int rs, int simm16) {
        npc = (gpr[rs] < 0) ? branchTarget(pc, simm16) : (pc + 4);
        return true;
    }

    public boolean doBGEZ(int rs, int simm16) {
        npc = (gpr[rs] >= 0) ? branchTarget(pc, simm16) : (pc + 4);
        return true;
    }

    public boolean doBLTZL(int rs, int simm16) {
        if (gpr[rs] < 0) {
            npc = branchTarget(pc, simm16);
            return true;
        } else {
            pc += 4;
        }
        return false;
    }

    public boolean doBGEZL(int rs, int simm16) {
        if (gpr[rs] >= 0) {
            npc = branchTarget(pc, simm16);
            return true;
        } else {
            pc += 4;
        }
        return false;
    }

    public boolean doBLTZAL(int rs, int simm16) {
        int target = pc + 4;
        boolean t = (gpr[rs] < 0);
        gpr[31] = target;
        npc = t ? branchTarget(pc, simm16) : target;
        return true;
    }

    public boolean doBGEZAL(int rs, int simm16) {
        int target = pc + 4;
        boolean t = (gpr[rs] >= 0);
        gpr[31] = target;
        npc = t ? branchTarget(pc, simm16) : target;
        return true;
    }

    public boolean doBLTZALL(int rs, int simm16) {
        boolean t = (gpr[rs] < 0);
        gpr[31] = pc + 4;
        if (t) {
            npc = branchTarget(pc, simm16);
        } else {
            pc += 4;
        }
        return t;
    }

    public boolean doBGEZALL(int rs, int simm16) {
        boolean t = (gpr[rs] >= 0);
        gpr[31] = pc + 4;
        if (t) {
            npc = branchTarget(pc, simm16);
        } else {
            pc += 4;
        }
        return t;
    }

    public boolean doJ(int uimm26) {
        npc = jumpTarget(pc, uimm26);
        if (npc == pc - 4) {
            Processor.log.info("Pausing emulator - jump to self (death loop)");
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_JUMPSELF);
        }
        return true;
    }

    public boolean doJAL(int uimm26) {
        gpr[31] = pc + 4;
        npc = jumpTarget(pc, uimm26);
        return true;
    }

    public boolean doBEQ(int rs, int rt, int simm16) {
        npc = (gpr[rs] == gpr[rt]) ? branchTarget(pc, simm16) : (pc + 4);
        if (npc == pc - 4 && rs == rt) {
            Processor.log.info("Pausing emulator - branch to self (death loop)");
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_JUMPSELF);
        }
        return true;
    }

    public boolean doBNE(int rs, int rt, int simm16) {
        npc = (gpr[rs] != gpr[rt]) ? branchTarget(pc, simm16) : (pc + 4);
        return true;
    }

    public boolean doBLEZ(int rs, int simm16) {
        npc = (gpr[rs] <= 0) ? branchTarget(pc, simm16) : (pc + 4);
        return true;
    }

    public boolean doBGTZ(int rs, int simm16) {
        npc = (gpr[rs] > 0) ? branchTarget(pc, simm16) : (pc + 4);
        return true;
    }

    public boolean doBEQL(int rs, int rt, int simm16) {
        if (gpr[rs] == gpr[rt]) {
            npc = branchTarget(pc, simm16);
            return true;
        } else {
            pc += 4;
        }
        return false;
    }

    public boolean doBNEL(int rs, int rt, int simm16) {
        if (gpr[rs] != gpr[rt]) {
            npc = branchTarget(pc, simm16);
            return true;
        } else {
            pc += 4;
        }
        return false;
    }

    public boolean doBLEZL(int rs, int simm16) {
        if (gpr[rs] <= 0) {
            npc = branchTarget(pc, simm16);
            return true;
        } else {
            pc += 4;
        }
        return false;
    }

    public boolean doBGTZL(int rs, int simm16) {
        if (gpr[rs] > 0) {
            npc = branchTarget(pc, simm16);
            return true;
        } else {
            pc += 4;
        }
        return false;
    }



}
