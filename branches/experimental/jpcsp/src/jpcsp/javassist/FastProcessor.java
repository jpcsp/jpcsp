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
package jpcsp.javassist;

import java.util.HashMap;
import java.util.Map;

import jpcsp.Memory;
import jpcsp.Processor;

/**
 * @author guillaume.serre@gmail.com
 *
 */
public class FastProcessor extends Processor {
	
    protected static Map<Integer, DynaCode> globalDynaCodes = new HashMap<Integer, DynaCode>();

    protected DynaCode currentDynaCode = null;
	protected JavassistRecompiler recompiler = new JavassistRecompiler();
	protected int interruptState = 0;
	protected boolean inSlot = false;
		
	public void recompile(int insn) {
		recompiler.recompile(insn);
	}
	
	@Override
	public void reset() {
		super.reset();
		currentDynaCode = null;
	}

    public FastProcessor() {
    	super();
    }

    @Override
    public void stepDelayslot() {
    	inSlot = true;
    	super.stepDelayslot();
    }

    public static int zeroExtend8(int value) {
        return (value & 0xff);
    }

    public static int signedCompare(int i, int j) {
        return (i - j) >>> 31;
    }

    public static int unsignedCompare(int i, int j) {
        return ((i - j) ^ i ^ j) >>> 31;
    }

    public static int branchTarget(int npc, int simm16) {
        return npc + (simm16 << 2);
    }

    public static int jumpTarget(int npc, int uimm26) {
        return (npc & 0xf0000000) | (uimm26 << 2);
    }

    public static boolean addSubOverflow(long value) {
        long tmp = value << (62 - 31);
        return ((tmp >>> 1) == (tmp & 1));
    }

    public void step() {
        npc = pc + 4;
        
        inSlot = false;
                
        if (currentDynaCode == null) {
	        currentDynaCode = globalDynaCodes.get(pc);
	        if (currentDynaCode != null) {
	        	if (!currentDynaCode.freezed) {
	        		currentDynaCode.freeze();
	        	}
	        	npc = currentDynaCode.entry;
        		currentDynaCode.execute();
        		currentDynaCode = null;
        		return;
	        } else {
	        	// no DynaCode for current PC
	        	currentDynaCode = new DynaCode(pc);
	        	globalDynaCodes.put( pc, currentDynaCode );
	        }
	        recompiler.setDynaCode( currentDynaCode );
        }

        currentDynaCode.setPc ( pc );
        currentDynaCode.setNpc ( npc );        	

        int insn = Memory.read32(pc);

        // by default, any Allegrex instruction takes 1 cycle at least
        cycles += 1;

        // by default, the next instruction to emulate is at the next address
        pc = npc;

        // process the current instruction
        decoder.process(this, insn);
    }

    @Override
    public void doSLL(int rd, int rt, int sa) {
    	super.doSLL(rd, rt, sa);
    	recompiler.doSLL(rd, rt, sa);
    }

    @Override
    public void doSRL(int rd, int rt, int sa) {
    	super.doSRL(rd, rt, sa);
    	recompiler.doSRL(rd, rt, sa);
    }

    @Override
    public void doSRA(int rd, int rt, int sa) {
    	super.doSRA(rd, rt, sa);
    	recompiler.doSRA(rd, rt, sa);
    }

    @Override
    public void doSLLV(int rd, int rt, int rs) {
    	super.doSLLV(rd, rt, rs);
    	recompiler.doSLLV(rd, rt, rs);
    }

    @Override
    public void doSRLV(int rd, int rt, int rs) {
    	super.doSRLV(rd, rt, rs);
    	recompiler.doSRLV(rd, rt, rs);
    }

    @Override
    public void doSRAV(int rd, int rt, int rs) {
    	super.doSRAV(rd, rt, rs);
    	recompiler.doSRAV(rd, rt, rs);
    }

    @Override
    public void doJR(int rs) {
        long target_cycles = cycles + 2;
        npc = gpr[rs];
        currentDynaCode.addJavaInstruction("processor.npc = " + currentDynaCode.getGPRCodeRepr( rs ) + ";");
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
        currentDynaCode = null;
    }

    @Override
    public void doJALR(int rd, int rs) {
   		currentDynaCode.gprAssignValue(rd, pc + 4);
        currentDynaCode.addJavaInstruction("processor.npc = " + currentDynaCode.getGPRCodeRepr( rs ) + ";");
        super.doJALR(rd, rs);
        currentDynaCode = null;
    }

    @Override
    public void doMFHI(int rd) {
    	super.doMFHI(rd);
    	recompiler.doMFHI(rd);
    }

    @Override
    public void doMTHI(int rs) {
    	super.doMTHI(rs);
    	recompiler.doMTHI(rs);
    }

    @Override
    public void doMFLO(int rd) {
    	super.doMFLO(rd);
    	recompiler.doMFLO(rd);
    }

    @Override
    public void doMTLO(int rs) {
    	super.doMTLO(rs);
    	recompiler.doMTLO(rs);
    }

    @Override
    public void doMULT(int rs, int rt) {
    	super.doMULT(rs, rt);
        recompiler.doMULT(rs, rt);
    }

    @Override
    public void doMULTU(int rs, int rt) {
        super.doMULTU(rs, rt);
        recompiler.doMULTU(rs, rt);
    }

    @Override
    public void doDIV(int rs, int rt) {
    	super.doDIV(rs, rt);
    	recompiler.doDIV(rs, rt);
    }

    @Override
    public void doDIVU(int rs, int rt) {
    	super.doDIVU(rs, rt);
    	recompiler.doDIVU(rs, rt);    	
    }

    @Override
    public void doADD(int rd, int rs, int rt) {
    	super.doADD(rd, rs, rt);
    	recompiler.doADD(rd, rs, rt);
    }

    @Override
    public void doADDU(int rd, int rs, int rt) {
    	super.doADDU(rd, rs, rt);
    	recompiler.doADDU(rd, rs, rt);
    }

    @Override
    public void doSUB(int rd, int rs, int rt) {
    	super.doSUB(rd, rs, rt);
    	recompiler.doSUB(rd, rs, rt);
    }

    @Override
    public void doSUBU(int rd, int rs, int rt) {
    	super.doSUBU(rd, rs, rt);
    	recompiler.doSUBU(rd, rs, rt);
    }

    @Override
    public void doAND(int rd, int rs, int rt) {
    	super.doAND(rd, rs, rt);
    	recompiler.doAND(rd, rs, rt);
    }

    @Override
    public void doOR(int rd, int rs, int rt) {
    	super.doOR(rd, rs, rt);
    	recompiler.doOR(rd, rs, rt);
    }

    @Override
    public void doXOR(int rd, int rs, int rt) {
    	super.doXOR(rd, rs, rt);
    	recompiler.doXOR(rd, rs, rt);
    }

    @Override
    public void doNOR(int rd, int rs, int rt) {
    	super.doNOR(rd, rs, rt);
    	recompiler.doNOR(rd, rs, rt);
    }

    @Override
    public void doSLT(int rd, int rs, int rt) {
    	super.doSLT(rd, rs, rt);
    	recompiler.doSLT(rd, rs, rt);
    }

    @Override
    public void doSLTU(int rd, int rs, int rt) {
    	super.doSLTU(rd, rs, rt);
    	recompiler.doSLTU(rd, rs, rt);
    }
    
    @Override
    public void doBREAK(int code) {
    	super.doBREAK(code);
    	recompiler.doBREAK(code);
    }

    @Override
    public void doBLTZ(int rs, int simm16) {
        long target_cycles = cycles + 3;
        
        int branch = branchTarget(pc, simm16);
        npc = (gpr[rs] < 0) ? branch : (pc + 4);
        
        if ( branch == currentDynaCode.entry) {
            currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr( rs ) + " >= 0) { processor.npc = 0x" + Integer.toHexString(pc + 4) + "; }");        	
        } else {
            currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr( rs ) + " < 0) { processor.npc = 0x" + Integer.toHexString(branch) + "; } else { processor.npc = 0x" + Integer.toHexString(pc + 4) + "; }");        	
        }

        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
        
        currentDynaCode = null;
    }

    @Override
    public void doBGEZ(int rs, int simm16) {
        long target_cycles = cycles + 3;
        
        int branch = branchTarget(pc, simm16);
        npc = (gpr[rs] >= 0) ? branch : (pc + 4);
        
        currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr( rs ) + " >= 0) { processor.npc = 0x" + Integer.toHexString(branch) + "; } else { processor.npc = 0x" + Integer.toHexString(pc + 4) + "; }");

        stepDelayslot();

        currentDynaCode = null;
        
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
    }

    @Override
    public void doBLTZL(int rs, int simm16) {
        long target_cycles = cycles + 3;        
        int branch = branchTarget(pc, simm16);
        int currentPC = pc;
        
        currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr(rs) + " < 0)");
        currentDynaCode.addJavaInstruction("{ processor.npc = 0x" + Integer.toHexString(branch) + ";");
        
        if (gpr[rs] < 0) {
            npc = branch;
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
        	recompile( Memory.read32(pc) );
            pc += 4;
            cycles += 3;
        }
        
        currentDynaCode.addJavaInstruction("} else { processor.npc = 0x" + Integer.toHexString(currentPC + 4) + "; }");
        
        currentDynaCode = null; 
    }

    @Override
    public void doBGEZL(int rs, int simm16) {
        long target_cycles = cycles + 3;        
        int branch = branchTarget(pc, simm16);
        int currentPC = pc;
        
        currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr(rs) + " >= 0)");
        currentDynaCode.addJavaInstruction("{ processor.npc = 0x" + Integer.toHexString(branch) + ";");
        
        if (gpr[rs] >= 0) {
            npc = branch;
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
        	// make sure delay slot is recompiled 
        	recompile( Memory.read32(pc) );
            pc += 4;
            cycles += 3;
        }
        
        currentDynaCode.addJavaInstruction("} else { processor.npc = 0x" + Integer.toHexString(currentPC + 4) + "; }");
        
        currentDynaCode = null; 
    }

    @Override
    public void doBLTZAL(int rs, int simm16) {
    	super.doBLTZAL(rs, simm16);
    	recompiler.doBLTZAL(rs, simm16);
    }

    @Override
    public void doBGEZAL(int rs, int simm16) {
    	super.doBGEZAL(rs, simm16);
    	recompiler.doBGEZAL(rs, simm16);
    }

    @Override
    public void doBLTZALL(int rs, int simm16) {
    	super.doBLTZALL(rs, simm16);
    	recompiler.doBLTZALL(rs, simm16);
    }

    @Override
    public void doBGEZALL(int rs, int simm16) {
    	super.doBGEZALL(rs, simm16);
    	recompiler.doBGEZALL(rs, simm16);
    }

    @Override
    public void doJ(int uimm26) {
        long target_cycles = cycles + 2;
        
        int jump = jumpTarget(npc, uimm26);
        currentDynaCode.addJavaInstruction("processor.npc = 0x" + Integer.toHexString(jump) + ";");
        
        npc = jump;
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }

        currentDynaCode = null;
    }

    @Override
    public void doJAL(int uimm26) {
        currentDynaCode.gprAssignValue(31, pc+4);
        currentDynaCode.addJavaInstruction("processor.npc = 0x" + Integer.toHexString(jumpTarget(pc, uimm26)) + ";");
        super.doJAL(uimm26);
        currentDynaCode = null;
    }

    @Override
    public void doBEQ(int rs, int rt, int simm16) {
        long target_cycles = cycles + 3;
        
        int branch = branchTarget(pc, simm16);
        
        npc = (gpr[rs] == gpr[rt]) ? branch : (pc + 4);

        currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr( rs ) + " == " + currentDynaCode.getGPRCodeRepr(rt));
        currentDynaCode.addJavaInstruction(") { processor.npc = 0x" + Integer.toHexString(branch) + "; } else { processor.npc = 0x" + Integer.toHexString(pc + 4) + "; }");

        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
        
        currentDynaCode = null;
    }

    @Override
    public void doBNE(int rs, int rt, int simm16) {
        long target_cycles = cycles + 3;

        int branch = branchTarget(pc, simm16);
        
        npc = (gpr[rs] != gpr[rt]) ? branch : (pc + 4);
        
        if (branch == currentDynaCode.entry) {
	        currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr(rs) + " == " + currentDynaCode.getGPRCodeRepr(rt) + ")");        	
	        currentDynaCode.addJavaInstruction(" { processor.npc = 0x" + Integer.toHexString(pc + 4) + "; }");
        } else {
	        currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr(rs) + " != " + currentDynaCode.getGPRCodeRepr(rt));
	        currentDynaCode.addJavaInstruction(") { processor.npc = 0x" + Integer.toHexString(branch) + "; } else { processor.npc = 0x" + Integer.toHexString(pc + 4) + "; }");
        }
        
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }

        currentDynaCode = null;
    }

    @Override
    public void doBLEZ(int rs, int simm16) {
        long target_cycles = cycles + 3;
        int branch = branchTarget(pc, simm16);
        npc = (gpr[rs] <= 0) ? branch : (pc + 4);

        currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr(rs) + " <= 0) { processor.npc = 0x" + Integer.toHexString(branch) + "; } else { processor.npc = 0x" + Integer.toHexString(pc + 4) + "; }");        
        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
        currentDynaCode = null;
    }

    @Override
    public void doBGTZ(int rs, int simm16) {
        long target_cycles = cycles + 3;
        
        int branch = branchTarget(pc, simm16);
        
        npc = (gpr[rs] > 0) ? branch : (pc + 4);     

        currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr(rs) + " > 0) { processor.npc = 0x" + Integer.toHexString(branch) + "; } else { processor.npc = 0x" + Integer.toHexString(pc + 4) + "; }");        

        stepDelayslot();
        if (cycles < target_cycles) {
            cycles = target_cycles;
        }
        
        currentDynaCode = null;        
    }

    @Override
    public void doBEQL(int rs, int rt, int simm16) {
        long target_cycles = cycles + 3;
        
        int branch = branchTarget(pc, simm16);
        
        int currentPC = pc;
        
        currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr(rs) + " == " + currentDynaCode.getGPRCodeRepr(rt) + ")");
        currentDynaCode.addJavaInstruction("{ processor.npc = 0x" + Integer.toHexString(branch) + ";");
        
        if (gpr[rs] == gpr[rt]) {
            npc = branchTarget(pc, simm16);
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
        	recompile( Memory.get_instance().read32(pc) );
            pc += 4;
            cycles += 3;
        }
        
        currentDynaCode.addJavaInstruction("} else { processor.npc = 0x" + Integer.toHexString(currentPC + 4) + "; }");
        
        currentDynaCode = null;        
    }

    @Override
    public void doBNEL(int rs, int rt, int simm16) {
        long target_cycles = cycles + 3;
        
        int branch = branchTarget(pc, simm16);
        
        int currentPC = pc;
                
        currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr( rs ) + " != " + currentDynaCode.getGPRCodeRepr( rt ) + ")");
        currentDynaCode.addJavaInstruction("{ processor.npc = 0x" + Integer.toHexString(branch) + ";");

        if (gpr[rs] != gpr[rt]) {
            npc = branch;
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
        	recompile( Memory.get_instance().read32(pc) );
            pc += 4;
            cycles += 3;
        }
        
        currentDynaCode.addJavaInstruction("} else { processor.npc = 0x" + Integer.toHexString(currentPC + 4) + "; }");
        
        currentDynaCode = null;
    }

    @Override
    public void doBLEZL(int rs, int simm16) {
        long target_cycles = cycles + 3;
        int branch = branchTarget(pc, simm16);
        int currentPC = pc;
        currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr( rs ) + " <= 0) { processor.npc = 0x" + Integer.toHexString(branch) + ";");
        if (gpr[rs] <= 0) {
            npc = branch;
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
        	// recompile delay slot
        	recompile( Memory.get_instance().read32( currentPC ) );
            pc += 4;
            cycles += 3;
        }
        currentDynaCode.addJavaInstruction("} else { processor.npc = 0x" + Integer.toHexString(currentPC + 4) + "; }");
        
        currentDynaCode = null;
    }

    @Override
    public void doBGTZL(int rs, int simm16) {
        long target_cycles = cycles + 3;
        
        int branch = branchTarget(pc, simm16);
        
        npc = (gpr[rs] > 0) ? branch : (pc + 4);
        
        int currentPC = pc;

        currentDynaCode.addJavaInstruction("if (" + currentDynaCode.getGPRCodeRepr( rs ) + " > 0) { processor.npc = 0x" + Integer.toHexString(branch) + ";");
          
        if (gpr[rs] > 0) {
            stepDelayslot();
            if (cycles < target_cycles) {
                cycles = target_cycles;
            }
        } else {
        	// recompile delay slot
        	recompile( Memory.get_instance().read32( currentPC ) );
            pc += 4;
            cycles += 3;
        }
        
        currentDynaCode.addJavaInstruction("} else { processor.npc = 0x" + Integer.toHexString(currentPC + 4) + "; }");
        currentDynaCode = null;  
    }

    @Override
    public void doADDI(int rt, int rs, int simm16) {
    	super.doADDI(rt, rs, simm16);
    	recompiler.doADDI(rt, rs, simm16);
    }

    @Override
    public void doADDIU(int rt, int rs, int simm16) {
    	super.doADDIU(rt, rs, simm16);
    	recompiler.doADDIU(rt, rs, simm16);
    }

    @Override
    public void doSLTI(int rt, int rs, int simm16) {
    	super.doSLTI(rt, rs, simm16);
    	recompiler.doSLTI(rt, rs, simm16);
    }

    @Override
    public void doSLTIU(int rt, int rs, int simm16) {
    	super.doSLTIU(rt, rs, simm16);
    	recompiler.doSLTIU(rt, rs, simm16);
    }

    @Override
    public void doANDI(int rt, int rs, int uimm16) {
    	super.doANDI(rt, rs, uimm16);
    	recompiler.doANDI(rt, rs, uimm16);
    }

    @Override
    public void doORI(int rt, int rs, int uimm16) {
    	super.doORI(rt, rs, uimm16);
    	recompiler.doORI(rt, rs, uimm16);
    }

    @Override
    public void doXORI(int rt, int rs, int uimm16) {
    	super.doXORI(rt, rs, uimm16);
    	recompiler.doXORI(rt, rs, uimm16);
    }

    @Override
    public void doLUI(int rt, int uimm16) {
    	super.doLUI(rt, uimm16);
    	recompiler.doLUI(rt, uimm16);
    }

    @Override
    public void doHALT() {
    	super.doHALT();
    	recompiler.doHALT();
    }

    @Override
    public void doMFIC(int rt) {
    	super.doMFIC(rt);
    	recompiler.doMFIC(rt);
    }

    @Override
    public void doMTIC(int rt) {
    	super.doMTIC(rt);
    	recompiler.doMTIC(rt);
    }

    @Override
    public void doMFC0(int rt, int c0dr) {
    	super.doMFC0(rt, c0dr);
    	recompiler.doMFC0(rt, c0dr);
    }

    @Override
    public void doCFC0(int rt, int c0cr) {
    	super.doCFC0(rt, c0cr);
    	recompiler.doCFC0(rt, c0cr);
    }

    @Override
    public void doMTC0(int rt, int c0dr) {
    	super.doMTC0(rt, c0dr);
    	recompiler.doMTC0(rt, c0dr);
    }

    @Override
    public void doCTC0(int rt, int c0cr) {
    	super.doCTC0(rt, c0cr);
    	recompiler.doCTC0(rt, c0cr);
    }

    @Override
    public void doERET() {
    	super.doERET();
    	recompiler.doERET();
    }

    @Override
    public void doLB(int rt, int rs, int simm16) {
    	super.doLB(rt, rs, simm16);
    	recompiler.doLB(rt, rs, simm16);
    }

    @Override
    public void doLBU(int rt, int rs, int simm16) {
    	super.doLBU(rt, rs, simm16);
    	recompiler.doLBU(rt, rs, simm16);
    }

    @Override
    public void doLH(int rt, int rs, int simm16) {
    	super.doLH(rt, rs, simm16);
    	recompiler.doLH(rt, rs, simm16);
    }

    @Override
    public void doLHU(int rt, int rs, int simm16) {
    	super.doLHU(rt, rs, simm16);
    	recompiler.doLHU(rt, rs, simm16);
    }

    @Override
    public void doLWL(int rt, int rs, int simm16) {
    	super.doLWL(rt, rs, simm16);
    	recompiler.doLWL(rt, rs, simm16);
    }

    @Override
    public void doLW(int rt, int rs, int simm16) {
    	super.doLW(rt, rs, simm16);
    	recompiler.doLW(rt, rs, simm16);
    }

    @Override
    public void doLWR(int rt, int rs, int simm16) {
    	super.doLWR(rt, rs, simm16);
    	recompiler.doLWR(rt, rs, simm16);
    }

    @Override
    public void doSB(int rt, int rs, int simm16) {
    	super.doSB(rt, rs, simm16);
    	recompiler.doSB(rt, rs, simm16);
    }

    @Override
    public void doSH(int rt, int rs, int simm16) {
    	super.doSH(rt, rs, simm16);
    	recompiler.doSH(rt, rs, simm16);
    }

    @Override
    public void doSWL(int rt, int rs, int simm16) {
    	super.doSWL(rt, rs, simm16);
    	recompiler.doSWL(rt, rs, simm16);
    }

    @Override
    public void doSW(int rt, int rs, int simm16) {
    	super.doSW(rt, rs, simm16);
    	recompiler.doSW(rt, rs, simm16);
    }

    @Override
    public void doSWR(int rt, int rs, int simm16) {
    	super.doSWR(rt, rs, simm16);
    	recompiler.doSWR(rt, rs, simm16);
    }

    @Override
    public void doCACHE(int code, int rs, int simm16) {
    	super.doCACHE(code, rs, simm16);
    	recompiler.doCACHE(code, rs, simm16);
    }

    @Override
    public void doLL(int rt, int rs, int simm16) {
    	super.doLL(rt, rs, simm16);
    	recompiler.doLL(rt, rs, simm16);
    }

    @Override
    public void doLWC1(int ft, int rs, int simm16) {
    	super.doLWC1(ft, rs, simm16);
    	recompiler.doLWC1(ft, rs, simm16);
    }

    @Override
    public void doLVS(int vt, int rs, int simm14) {
    	super.doLVS(vt, rs, simm14);
    	recompiler.doLVS(vt, rs, simm14);
    }

    @Override
    public void doSC(int rt, int rs, int simm16) {
    	super.doSC(rt, rs, simm16);
    	recompiler.doSC(rt, rs, simm16);
    }

    @Override
    public void doSWC1(int ft, int rs, int simm16) {
    	super.doSWC1(ft, rs, simm16);
    	recompiler.doSWC1(ft, rs, simm16);
    }

    @Override
    public void doSVS(int vt, int rs, int simm14) {
    	super.doSVS(vt, rs, simm14);
    	recompiler.doSVS(vt, rs, simm14);
    }

    @Override
    public void doROTR(int rd, int rt, int sa) {
    	super.doROTR(rd, rt, sa);
    	recompiler.doROTR(rd, rt, sa);
    }

    @Override
    public void doROTRV(int rd, int rt, int rs) {
    	super.doROTRV(rd, rt, rs);
    	recompiler.doROTRV(rd, rt, rs);
    }

    @Override
    public void doMOVZ(int rd, int rs, int rt) {
    	super.doMOVZ(rd, rs, rt);
    	recompiler.doMOVZ(rd, rs, rt);
    }

    @Override
    public void doMOVN(int rd, int rs, int rt) {
    	super.doMOVN(rd, rs, rt);
    	recompiler.doMOVN(rd, rs, rt);
    }

    @Override
    public void doSYSCALL(int code) {
        currentDynaCode.addJavaInstruction("processor.pc = 0x" + Integer.toHexString(pc) + ";");	// make sure pc is up to date
        if (!inSlot) {
        	currentDynaCode.addJavaInstruction("processor.npc = 0x" + Integer.toHexString(npc) + ";");	// make sure npc is up to date
        }
        super.doSYSCALL(code);
        recompiler.doSYSCALL(code);
    }

    @Override
    public void doSYNC() {
    	super.doSYNC();
    	recompiler.doSYNC();
    }

    @Override
    public void doCLZ(int rd, int rs) {
    	super.doCLZ(rd, rs);
    	recompiler.doCLZ(rd, rs);
    }

    @Override
    public void doCLO(int rd, int rs) {
    	super.doCLO(rd, rs);
    	recompiler.doCLO(rd, rs);
    }

    @Override
    public void doMADD(int rs, int rt) {
    	super.doMADD(rs, rt);
    	recompiler.doMADD(rs, rt);
    }

    @Override
    public void doMADDU(int rs, int rt) {
    	super.doMADD(rs, rt);
    	recompiler.doMADD(rs, rt);
    }

    @Override
    public void doMAX(int rd, int rs, int rt) {
    	super.doMAX(rd, rs, rt);
    	recompiler.doMAX(rd, rs, rt);
    }

    @Override
    public void doMIN(int rd, int rs, int rt) {
    	super.doMIN(rd, rs, rt);
        recompiler.doMIN(rd, rs, rt);
    }

    @Override
    public void doMSUB(int rs, int rt) {
    	super.doMSUB(rs, rt);
    	recompiler.doMSUB(rs, rt);
    }

    @Override
    public void doMSUBU(int rs, int rt) {
    	super.doMSUBU(rs, rt);
    	recompiler.doMSUBU(rs, rt);
    }

    @Override
    public void doEXT(int rt, int rs, int rd, int sa) {
    	super.doEXT(rt, rs, rd, sa);
    	recompiler.doEXT(rt, rs, rd, sa);
    }

    @Override
    public void doINS(int rt, int rs, int rd, int sa) {
    	super.doINS(rt, rs, rd, sa);
    	recompiler.doINS(rt, rs, rd, sa);
    }

    @Override
    public void doWSBH(int rd, int rt) {
    	super.doWSBH(rd, rt);
    	recompiler.doWSBH(rd, rt);
    }

    @Override
    public void doWSBW(int rd, int rt) {
    	super.doWSBW(rd, rt);
    	recompiler.doWSBW(rd, rt);
    }

    @Override
    public void doSEB(int rd, int rt) {
    	super.doSEB(rd, rt);
    	recompiler.doSEB(rd, rt);
    }

    @Override
    public void doBITREV(int rd, int rt) {
    	super.doBITREV(rd, rt);
    	recompiler.doBITREV(rd, rt);
    }

    @Override
    public void doSEH(int rd, int rt) {
    	super.doSEH(rd, rt);
    	recompiler.doSEH(rd, rt);
    }

    @Override
    public void doMFC1(int rt, int c1dr) {
    	super.doMFC1(rt, c1dr);
    	recompiler.doMFC1(rt, c1dr);
    }

    @Override
    public void doCFC1(int rt, int c1cr) {
    	super.doCFC1(rt, c1cr);
    	recompiler.doCFC1(rt, c1cr);
    }

    @Override
    public void doMTC1(int rt, int c1dr) {
    	super.doMTC1(rt, c1dr);
    	recompiler.doMTC1(rt, c1dr);
    }

    @Override
    public void doCTC1(int rt, int c1cr) {
    	super.doCTC1(rt, c1cr);
    	recompiler.doCTC1(rt, c1cr);
    }

    @Override
    public void doBC1F(int simm16) {
    	int branch = branchTarget(pc, simm16);
        npc = !fcr31_c ? branch : (pc + 4);
        currentDynaCode.addJavaInstruction("if (!processor.fcr31_c) { processor.npc = 0x" + Integer.toHexString(branch)+"; } else { processor.npc = 0x" + Integer.toHexString(pc+4) +";}");
        stepDelayslot();        
        currentDynaCode = null;
    }

    @Override
    public void doBC1T(int simm16) {
    	int branch = branchTarget(pc, simm16);
        npc = fcr31_c ? branch : (pc + 4);
        currentDynaCode.addJavaInstruction("if (processor.fcr31_c) { processor.npc = 0x" + Integer.toHexString(branch)+"; } else { processor.npc = 0x" + Integer.toHexString(pc+4) +";}");
        stepDelayslot();        
        currentDynaCode = null;
    }

    @Override
    public void doBC1FL(int simm16) {
    	super.doBC1FL(simm16);
    	recompiler.doBC1FL(simm16);
    }

    @Override
    public void doBC1TL(int simm16) {
    	super.doBC1TL(simm16);
    	recompiler.doBC1TL(simm16);
    }

    @Override
    public void doADDS(int fd, int fs, int ft) {
    	super.doADDS(fd, fs, ft);
    	recompiler.doADDS(fd, fs, ft);
    }

    @Override
    public void doSUBS(int fd, int fs, int ft) {
    	super.doSUBS(fd, fs, ft);
    	recompiler.doSUBS(fd, fs, ft);
    }

    @Override
    public void doMULS(int fd, int fs, int ft) {
    	super.doMULS(fd, fs, ft);
    	recompiler.doMULS(fd, fs, ft);
    }

    @Override
    public void doDIVS(int fd, int fs, int ft) {
    	super.doDIVS(fd, fs, ft);
    	recompiler.doDIVS(fd, fs, ft);
    }

    @Override
    public void doSQRTS(int fd, int fs) {
    	super.doSQRTS(fd, fs);
    	recompiler.doSQRTS(fd, fs);
    }

    @Override
    public void doABSS(int fd, int fs) {
    	super.doABSS(fd, fs);
    	recompiler.doABSS(fd, fs);
    }

    @Override
    public void doMOVS(int fd, int fs) {
    	super.doMOVS(fd, fs);
    	recompiler.doMOVS(fd, fs);
    }

    @Override
    public void doNEGS(int fd, int fs) {
    	super.doNEGS(fd, fs);
    	recompiler.doNEGS(fd, fs);
    }

    @Override
    public void doROUNDWS(int fd, int fs) {
    	super.doROUNDWS(fd, fs);
    	recompiler.doROUNDWS(fd, fs);
    }

    @Override
    public void doTRUNCWS(int fd, int fs) {
    	super.doTRUNCWS(fd, fs);
    	recompiler.doTRUNCWS(fd, fs);
    }

    @Override
    public void doCEILWS(int fd, int fs) {
    	super.doCEILWS(fd, fs);
    	recompiler.doCEILWS(fd, fs);
    }

    @Override
    public void doFLOORWS(int fd, int fs) {
    	super.doFLOORWS(fd, fs);
    	recompiler.doFLOORWS(fd, fs);
    }

    @Override
    public void doCVTSW(int fd, int fs) {
    	super.doCVTSW(fd, fs);
    	recompiler.doCVTSW(fd, fs);
    }

    @Override
    public void doCVTWS(int fd, int fs) {
    	super.doCVTWS(fd, fs);
    	recompiler.doCVTWS(fd, fs);
    }

    @Override
    public void doCCONDS(int fs, int ft, int cond) {
    	super.doCCONDS(fs, ft, cond);
    	recompiler.doCCONDS(fs, ft, cond);
    }

// VFPU0
    @Override
    public void doVADD(int vsize, int vd, int vs, int vt) {
    	super.doVADD(vsize, vd, vs, vt);
    	recompiler.doVADD(vsize, vd, vs, vt);
    }

    @Override
    public void doVSUB(int vsize, int vd, int vs, int vt) {
    	super.doVSUB(vsize, vd, vs, vt);
    	recompiler.doVSUB(vsize, vd, vs, vt);
    }

    @Override
    public void doVSBN(int vsize, int vd, int vs, int vt) {
    	super.doVSBN(vsize, vd, vs, vt);
    	recompiler.doVSBN(vsize, vd, vs, vt);
    }

    @Override
    public void doVDIV(int vsize, int vd, int vs, int vt) {
    	super.doVDIV(vsize, vd, vs, vt);
    	recompiler.doVDIV(vsize, vd, vs, vt);
    }
// VFPU1
    @Override
    public void doVMUL(int vsize, int vd, int vs, int vt) {
    	super.doVMUL(vsize, vd, vs, vt);
    	recompiler.doVMUL(vsize, vd, vs, vt);
    }

    @Override
    public void doVDOT(int vsize, int vd, int vs, int vt) {
    	super.doVDOT(vsize, vd, vs, vt);
    	recompiler.doVDOT(vsize, vd, vs, vt);
    }

    @Override
    public void doVSCL(int vsize, int vd, int vs, int vt) {
    	super.doVSCL(vsize, vd, vs, vt);
    	recompiler.doVSCL(vsize, vd, vs, vt);
    }

    @Override
    public void doVHDP(int vsize, int vd, int vs, int vt) {
    	super.doVHDP(vsize, vd, vs, vt);
    	recompiler.doVHDP(vsize, vd, vs, vt);
    }

    @Override
    public void doVCRS(int vsize, int vd, int vs, int vt) {
    	super.doVCRS(vsize, vd, vs, vt);
    	recompiler.doVCRS(vsize, vd, vs, vt);
    }

    @Override
    public void doVDET(int vsize, int vd, int vs, int vt) {
    	super.doVDET(vsize, vd, vs, vt);
    	recompiler.doVDET(vsize, vd, vs, vt);
    }

// VFPU3
    @Override
    public void doVCMP(int vsize, int vs, int vt, int cond) {
    	super.doVCMP(vsize, vs, vt, cond);
    	recompiler.doVCMP(vsize, vs, vt, cond);
    }

    @Override
    public void doVMIN(int vsize, int vd, int vs, int vt) {
    	super.doVMIN(vsize, vd, vs, vt);
    	recompiler.doVMIN(vsize, vd, vs, vt);
    }

    @Override
    public void doVMAX(int vsize, int vd, int vs, int vt) {
    	super.doVMAX(vsize, vd, vs, vt);
    	recompiler.doVMAX(vsize, vd, vs, vt);
    }

    @Override
    public void doVSCMP(int vsize, int vd, int vs, int vt) {
    	super.doVSCMP(vsize, vd, vs, vt);
    	recompiler.doVSCMP(vsize, vd, vs, vt);
    }

    @Override
    public void doVSGE(int vsize, int vd, int vs, int vt) {
    	super.doVSGE(vsize, vd, vs, vt);
    	recompiler.doVSGE(vsize, vd, vs, vt);
    }

    @Override
    public void doVSLT(int vsize, int vd, int vs, int vt) {
    	super.doVSLT(vsize, vd, vs, vt);
    	recompiler.doVSLT(vsize, vd, vs, vt);
    }

    @Override
    public void doVPFXS(int imm24) {
    	super.doVPFXS(imm24);
    	recompiler.doVPFXS(imm24);
    }

    @Override
    public void doVPFXT(int imm24) {
    	super.doVPFXT(imm24);
    	recompiler.doVPFXT(imm24);
    }

    @Override
    public void doVPFXD(int imm24) {
    	super.doVPFXD(imm24);
    	recompiler.doVPFXD(imm24);
    }

    @Override
    public void doVIIM(int vs, int imm16) {
    	super.doVIIM(vs, imm16);
    	recompiler.doVIIM(vs, imm16);
    }

    @Override
    public void doVFIM(int vs, int imm16) {
    	super.doVFIM(vs, imm16);
    	recompiler.doVFIM(vs, imm16);
    }
   
}
