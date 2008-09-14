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
import jpcsp.AllegrexDecoder;
import jpcsp.AllegrexInstructions;
import jpcsp.Memory;

/**
 * @author guillaume.serre@gmail.com
 */
public class JavassistRecompiler implements AllegrexInstructions {

	private DynaCode dynaCode = null;
	private AllegrexDecoder decoder = new AllegrexDecoder();

	public void setDynaCode(DynaCode dynaCode) {
		this.dynaCode = dynaCode;
	}

	public JavassistRecompiler() {
	}

	@Override
	public void doABSS(int fd, int fs) {
		System.err.println("ABSS Instruction is not supported by recompiler");
	}

	@Override
	public void doADD(int rd, int rs, int rt) {
		dynaCode.addLocal("long result");
        dynaCode.addJavaInstruction("result = (long) " + dynaCode.getGPRCodeRepr(rs) + " + (long) " + dynaCode.getGPRCodeRepr(rt) + ";");
        dynaCode.gprAssignExpression(rd, "(int) result");
	}

	@Override
	public void doADDI(int rt, int rs, int simm16) {
		System.err.println("ADDI Instruction is not supported by recompiler");
	}

	@Override
	public void doADDIU(int rt, int rs, int simm16) {
		Integer currentRSValue = dynaCode.getFixedValue(rs);
		if (currentRSValue != null) {
			dynaCode.gprAssignValue(rt, (currentRSValue + simm16));
		} else {
			dynaCode.gprAssignExpression(rt, dynaCode.getGPRCodeRepr(rs) + " + " + simm16);
		}
	}

	@Override
	public void doADDS(int fd, int fs, int ft) {
		dynaCode.addJavaInstruction("fpr[" + fd + "] = fpr[" + fs + "] + fpr[" + ft + "];");
	}

	@Override
	public void doADDU(int rd, int rs, int rt) {
		if (dynaCode.getFixedValue(rs) != null && dynaCode.getFixedValue(rt) != null) {			
			dynaCode.gprAssignValue(rd, dynaCode.getFixedValue(rs) + dynaCode.getFixedValue(rt) );
		} else {
			dynaCode.gprAssignExpression(rd, dynaCode.getGPRCodeRepr(rs) + " + " + dynaCode.getGPRCodeRepr(rt));
		}
	}

	@Override
	public void doAND(int rd, int rs, int rt) {
    	if (dynaCode.getFixedValue(rs) != null && dynaCode.getFixedValue(rt) != null) {
    		dynaCode.gprAssignValue(rd, dynaCode.getFixedValue(rs) & dynaCode.getFixedValue(rt));        		
    	} else {
    		dynaCode.gprAssignExpression(rd, dynaCode.getGPRCodeRepr( rs ) + " & " + dynaCode.getGPRCodeRepr( rt ));
    	}
	}

	@Override
	public void doANDI(int rt, int rs, int uimm16) {
    	if (dynaCode.getFixedValue(rs) != null) {
    		dynaCode.gprAssignValue(rt, dynaCode.getFixedValue(rs) & uimm16);        		
    	} else {
    		dynaCode.gprAssignExpression(rt, dynaCode.getGPRCodeRepr( rs ) + " & " + uimm16);
    	}
	}

	@Override
	public void doBC1F(int simm16) {
		System.err.println("BC1F Instruction is not supported by recompiler");
	}

	@Override
	public void doBC1FL(int simm16) {
		System.err.println("BC1FL Instruction is not supported by recompiler");
	}

	@Override
	public void doBC1T(int simm16) {
		System.err.println("BC1T Instruction is not supported by recompiler");
	}

	@Override
	public void doBC1TL(int simm16) {
		System.err.println("BC1TL Instruction is not supported by recompiler");
	}

	@Override
	public void doBEQ(int rs, int rt, int simm16) {
		System.err.println("BEQ Instruction is not supported by recompiler");
	}

	@Override
	public void doBEQL(int rs, int rt, int simm16) {
		System.err.println("BEQL Instruction is not supported by recompiler");
	}

	@Override
	public void doBGEZ(int rs, int simm16) {
		System.err.println("BGEZ Instruction is not supported by recompiler");
	}

	@Override
	public void doBGEZAL(int rs, int simm16) {
		System.err.println("BGEZAL Instruction is not supported by recompiler");
	}

	@Override
	public void doBGEZALL(int rs, int simm16) {
		System.err.println("BGEZALL Instruction is not supported by recompiler");
	}

	@Override
	public void doBGEZL(int rs, int simm16) {
		System.err.println("BGEZL Instruction is not supported by recompiler");
	}

	@Override
	public void doBGTZ(int rs, int simm16) {
		System.err.println("BGTZ Instruction is not supported by recompiler");
	}

	@Override
	public void doBGTZL(int rs, int simm16) {
		System.err.println("BGTZL Instruction is not supported by recompiler");

	}

	@Override
	public void doBITREV(int rd, int rt) {
		System.err.println("BITREV Instruction is not supported by recompiler");

	}

	@Override
	public void doBLEZ(int rs, int simm16) {
		System.err.println("BLEZ Instruction is not supported by recompiler");

	}

	@Override
	public void doBLEZL(int rs, int simm16) {
		System.err.println("BLEZL Instruction is not supported by recompiler");

	}

	@Override
	public void doBLTZ(int rs, int simm16) {
		System.err.println("BLTZ Instruction is not supported by recompiler");

	}

	@Override
	public void doBLTZAL(int rs, int simm16) {
		System.err.println("BLTZAL Instruction is not supported by recompiler");

	}

	@Override
	public void doBLTZALL(int rs, int simm16) {
		System.err.println("BLTZALL Instruction is not supported by recompiler");

	}

	@Override
	public void doBLTZL(int rs, int simm16) {
		System.err.println("BLTZL Instruction is not supported by recompiler");

	}

	@Override
	public void doBNE(int rs, int rt, int simm16) {
		System.err.println("BNE Instruction is not supported by recompiler");

	}

	@Override
	public void doBNEL(int rs, int rt, int simm16) {
		System.err.println("BNEL Instruction is not supported by recompiler");

	}

	@Override
	public void doBREAK(int code) {
		System.err.println("BREAK Instruction is not supported by recompiler");
	}

	@Override
	public void doCACHE(int rt, int rs, int simm16) {
		System.err.println("CACHE Instruction is not supported by recompiler");

	}

	@Override
	public void doCCONDS(int fs, int ft, int cond) {
		dynaCode.addLocal("float x");
		dynaCode.addLocal("float y");
		dynaCode.addLocal("boolean unordered");
		dynaCode.addJavaInstruction("x = " + dynaCode.getFPRCodeRepr(fs) + ";"); 
		dynaCode.addJavaInstruction("y = " + dynaCode.getFPRCodeRepr(ft) + ";"); 
		dynaCode.addJavaInstruction("unordered = ((" + cond + " & 1) != 0) && (Float.isNaN(x) || Float.isNaN(y));");

		dynaCode.addJavaInstruction("if (unordered) {");
		dynaCode.addJavaInstruction("if ((" + cond + " & 8) != 0) {");
		dynaCode.addJavaInstruction("processor.doUNK(\"C.cond.S instruction raises an Unordered exception\");");
		dynaCode.addJavaInstruction("}");
		dynaCode.addJavaInstruction("processor.fcr31_c = true;");
		dynaCode.addJavaInstruction("} else {");
		dynaCode.addJavaInstruction("boolean equal = ((" + cond + " & 2) != 0) && (x == y);");
		dynaCode.addJavaInstruction("boolean less = ((" + cond + " & 4) != 0) && (x < y);");
		dynaCode.addJavaInstruction("processor.fcr31_c = less || equal;");
		dynaCode.addJavaInstruction("}");
	}

	@Override
	public void doCEILWS(int fd, int fs) {
		System.err.println("CEILWS Instruction is not supported by recompiler");

	}

	@Override
	public void doCFC0(int rt, int c0cr) {
		System.err.println("CFC0 Instruction is not supported by recompiler");

	}

	@Override
	public void doCFC1(int rt, int c1cr) {
		System.err.println("CFC1 Instruction is not supported by recompiler");

	}

	@Override
	public void doCLO(int rd, int rs) {
		System.err.println("CLO Instruction is not supported by recompiler");

	}

	@Override
	public void doCLZ(int rd, int rs) {
		System.err.println("CLZ Instruction is not supported by recompiler");

	}

	@Override
	public void doCTC0(int rt, int c0cr) {
		System.err.println("CTC0 Instruction is not supported by recompiler");

	}

	@Override
	public void doCTC1(int rt, int c1cr) {
		System.err.println("CTC1 Instruction is not supported by recompiler");

	}

	@Override
	public void doCVTSW(int fd, int fs) {
        dynaCode.fprAssignExpression(fd, "(float) Float.floatToRawIntBits(" + dynaCode.getFPRCodeRepr(fs) + ")");
	}

	@Override
	public void doCVTWS(int fd, int fs) {
		System.err.println("CVTWS Instruction is not supported by recompiler");

	}

	@Override
	public void doDIV(int rs, int rt) {
        dynaCode.addJavaInstruction("lo = " + dynaCode.getGPRCodeRepr(rs) + " / " + dynaCode.getGPRCodeRepr(rt) + ";");
        dynaCode.addJavaInstruction("hi = " + dynaCode.getGPRCodeRepr(rs) + " % " + dynaCode.getGPRCodeRepr(rt) + ";");
        dynaCode.addJavaInstruction("processor.hilo = ((long) hi) << 32 | (((long) lo) & 0xffffffff);");

	}

	@Override
	public void doDIVS(int fd, int fs, int ft) {
		dynaCode.fprAssignExpression(fd, dynaCode.getFPRCodeRepr(fs) + "/" + dynaCode.getFPRCodeRepr(ft));
	}

	@Override
	public void doDIVU(int rs, int rt) {
    	dynaCode.addLocal("long x");
    	dynaCode.addLocal("long y");
    	dynaCode.addLocal("int lo");
    	dynaCode.addLocal("int hi");
    	dynaCode.addJavaInstruction("x = ((long) " + dynaCode.getGPRCodeRepr(rs) + ") & 0xffffffff;");
    	dynaCode.addJavaInstruction("y = ((long) " + dynaCode.getGPRCodeRepr(rt) + ") & 0xffffffff;");
    	dynaCode.addJavaInstruction("lo = (int) (x / y);");
    	dynaCode.addJavaInstruction("hi = (int) (x % y);");
    	dynaCode.addJavaInstruction("processor.hilo = ((long) hi) << 32 | (((long) lo) & 0xffffffff);");
	}

	@Override
	public void doERET() {
		System.err.println("ERET Instruction is not supported by recompiler");

	}

	@Override
	public void doEXT(int rt, int rs, int rd, int sa) {
        if (rt != 0) {
        	dynaCode.addLocal("int mask");
            dynaCode.addJavaInstruction("mask = ~(~0 << (" + rd+1 + "));");
        	dynaCode.gprAssignExpression(rt, "(" + dynaCode.getGPRCodeRepr( rs ) + " >>> + " + sa + ") & mask");
        }
	}

	@Override
	public void doFLOORWS(int fd, int fs) {
		System.err.println("FLOORWS Instruction is not supported by recompiler");

	}

	@Override
	public void doHALT() {
		System.err.println("HALT Instruction is not supported by recompiler");

	}

	@Override
	public void doINS(int rt, int rs, int rd, int sa) {
        if (rt != 0) {
        	dynaCode.addLocal("int mask");
        	dynaCode.addJavaInstruction("mask = ~(~0 << (" + (rd-sa+1) + ")) << " + sa + ";");
        	dynaCode.gprAssignExpression(rt, "(" + dynaCode.getGPRCodeRepr(rt) + " & ~mask) | ((" + dynaCode.getGPRCodeRepr(rs) + " << " + sa + ") & mask)");
        }
	}

	@Override
	public void doJ(int uimm26) {
		System.err.println("J Instruction is not supported by recompiler");

	}

	@Override
	public void doJAL(int uimm26) {
		System.err.println("JAL Instruction is not supported by recompiler");
	}

	@Override
	public void doJALR(int rd, int rs) {
		System.err.println("JALR Instruction is not supported by recompiler");

	}

	@Override
	public void doJR(int rs) {
		System.err.println("JR Instruction is not supported by recompiler");
	}

	@Override
	public void doLB(int rt, int rs, int simm16) {
		dynaCode.addJavaInstruction("word = (memory.read8("
				+ dynaCode.getGPRCodeRepr(rs) + " + " + simm16
				+ ") << 24) >> 24;");
		if (rt != 0) {
			dynaCode.addJavaInstruction("gpr[" + rt + "] = word;");
		}
		dynaCode.fixGPRVar(rt, "word");

	}

	@Override
	public void doLBU(int rt, int rs, int simm16) {
		dynaCode.addJavaInstruction("word = memory.read8("
				+ dynaCode.getGPRCodeRepr(rs) + "  + " + simm16
				+ ") & 0xff;");
		if (rt != 0) {
			dynaCode.addJavaInstruction("gpr[" + rt + "] = word;");
			dynaCode.fixGPRVar(rt, "word");
		}
	}

	@Override
	public void doLH(int rt, int rs, int simm16) {
        dynaCode.addJavaInstruction("word = (memory.read16("+ dynaCode.getGPRCodeRepr(rs) + " + " + simm16 + ") << 16) >> 16;");
        dynaCode.gprAssignLocal(rt, "word");
	}

	@Override
	public void doLHU(int rt, int rs, int simm16) {
		dynaCode.addJavaInstruction("word = memory.read16("
				+ dynaCode.getGPRCodeRepr(rs) + "  + " + simm16
				+ ") & 0xffff;");
		if (rt != 0) {
			dynaCode.addJavaInstruction("gpr[" + rt + "] = word;");
			dynaCode.fixGPRVar(rt, "word");
		}
	}

	@Override
	public void doLL(int rt, int rs, int simm16) {
		System.err.println("LL Instruction is not supported by recompiler");

	}

	@Override
	public void doLUI(int rt, int uimm16) {
		dynaCode.gprAssignValue(rt, (uimm16 << 16));
	}

	@Override
	public void doLVS(int vt, int rs, int simm14) {
		System.err.println("LVS Instruction is not supported by recompiler");

	}

	@Override
	public void doLW(int rt, int rs, int simm16) {
		
		if (dynaCode.getFixedValue(rs) != null) {
			int page = 0;
			int address = dynaCode.getFixedValue(rs) + simm16;
			try {
				page = Memory.get_instance().indexFromAddr(address);
			} catch (Exception e) {
				e.printStackTrace();
			}
			dynaCode.addJavaInstruction("word = memory.read32(0x" + Integer.toHexString(page) + ", 0x"
					+ Integer.toHexString(address) + ");");
		} else {
			dynaCode.addJavaInstruction("word = memory.read32("
					+ dynaCode.getGPRCodeRepr(rs) + " + " + simm16 + ");");
		}
		
		if (rt != 0) {
			dynaCode.addJavaInstruction("gpr[" + rt + "] = word;");
		}
		dynaCode.fixGPRVar(rt, "word");
	}

	@Override
	public void doLWC1(int ft, int rs, int simm16) {
        dynaCode.fprAssignExpression(ft, "Float.intBitsToFloat(memory.read32(" + dynaCode.getGPRCodeRepr(rs) + " + " + simm16 + "))");
	}

	@Override
	public void doLWL(int rt, int rs, int simm16) {
		dynaCode.addLocal("int address");
		dynaCode.addLocal("int offset");
		dynaCode.addLocal("int reg");
        dynaCode.addJavaInstruction("address = " + dynaCode.getGPRCodeRepr(rs) + " + " + simm16 + ";");
        dynaCode.addJavaInstruction("offset = address & 0x3;");
        dynaCode.addJavaInstruction("reg = " + dynaCode.getGPRCodeRepr(rt) + ";");

        dynaCode.addJavaInstruction("word = memory.get_instance().read32(address & 0xfffffffc);");

        dynaCode.addJavaInstruction(
        "switch (offset) {" + 
            "case 0:" + 
                "word = ((word & 0xff) << 24) | (reg & 0xffffff);" + 
                "break;" + 

            "case 1:" + 
                "word = ((word & 0xffff) << 16) | (reg & 0xffff);" + 
                "break;" + 

            "case 2:" + 
                "word = ((word & 0xffffff) << 8) | (reg & 0xff);" + 
                "break;" + 

            "case 3:" + 
                "break;" + 
        "}");        
        dynaCode.gprAssignLocal(rt, "word");
	}

	@Override
	public void doLWR(int rt, int rs, int simm16) {
		dynaCode.addLocal("int address");
		dynaCode.addLocal("int offset");
		dynaCode.addLocal("int reg");
        dynaCode.addJavaInstruction("address = " + dynaCode.getGPRCodeRepr(rs) + " + " + simm16 + ";");
        dynaCode.addJavaInstruction("offset = address & 0x3;");
        dynaCode.addJavaInstruction("reg = " + dynaCode.getGPRCodeRepr(rt) + ";");

        dynaCode.addJavaInstruction("word = memory.get_instance().read32(address & 0xfffffffc);");

        dynaCode.addJavaInstruction(
        "switch (offset) {" + 
            "case 0:" + 
                "break;" + 

            "case 1:" + 
                "word = (reg & 0xff000000) | ((word & 0xffffff00) >> 8);" + 
                "break;" + 

            "case 2:" + 
                "word = (reg & 0xffff0000) | ((word & 0xffff0000) >> 16);" + 
                "break;" + 

            "case 3:" + 
            	"word = (reg & 0xffffff00) | ((word & 0xff000000) >> 24);" +
                "break;" + 
        "}");        
        dynaCode.gprAssignLocal(rt, "word");
	}

	@Override
	public void doMADD(int rs, int rt) {
       dynaCode.addJavaInstruction("processor.hilo += ((long) " + dynaCode.getGPRCodeRepr(rs) + ") * ((long) " + dynaCode.getGPRCodeRepr(rt) + ");");
	}

	@Override
	public void doMADDU(int rs, int rt) {
		System.err.println("MADDU Instruction is not supported by recompiler");

	}

	@Override
	public void doMAX(int rd, int rs, int rt) {
        if (rd != 0) {
            dynaCode.addJavaInstruction("x = " + dynaCode.getGPRCodeRepr(rs) + ";");
            dynaCode.addJavaInstruction("y = " + dynaCode.getGPRCodeRepr(rt) + ";");
            dynaCode.gprAssignExpression(rd, "(x > y) ? x : y");
        }
	}

	@Override
	public void doMFC0(int rt, int c0dr) {
		System.err.println("MFC0 Instruction is not supported by recompiler");

	}

	@Override
	public void doMFC1(int rt, int c1dr) {
		dynaCode.gprAssignExpression(rt, "Float.floatToRawIntBits(fpr[" + c1dr + "])");
	}

	@Override
	public void doMFHI(int rd) {
		dynaCode.gprAssignExpression(rd, "(int) (processor.hilo >>> 32)");
	}

	@Override
	public void doMFIC(int rt) {
		System.err.println("MFIC Instruction is not supported by recompiler");

	}

	@Override
	public void doMFLO(int rd) {
		dynaCode.gprAssignExpression(rd, "(int) (processor.hilo & 0xffffffff)");
	}

	@Override
	public void doMIN(int rd, int rs, int rt) {
		if (rd != 0) {
            dynaCode.addJavaInstruction("x = " + dynaCode.getGPRCodeRepr(rs) + ";");
            dynaCode.addJavaInstruction("y = " + dynaCode.getGPRCodeRepr(rt) + ";");
            dynaCode.gprAssignExpression(rd, "(x < y) ? x : y");
        }
	}

	@Override
	public void doMOVN(int rd, int rs, int rt) {
		dynaCode.addJavaInstruction("if (" + dynaCode.getGPRCodeRepr(rt) + " != 0) {");
		dynaCode.gprAssignExpression(rd, dynaCode.getGPRCodeRepr(rs));
		dynaCode.addJavaInstruction("}");
	}

	@Override
	public void doMOVS(int fd, int fs) {
		dynaCode.fprAssignExpression(fd, dynaCode.getFPRCodeRepr(fs));
	}

	@Override
	public void doMOVZ(int rd, int rs, int rt) {
        if (rd != 0) {
        	dynaCode.addJavaInstruction("if (" + dynaCode.getGPRCodeRepr(rt) + " == 0) gpr[" + rd + "] = "+ dynaCode.getGPRCodeRepr(rs) + ";");
        	dynaCode.unfixGPR( rd );
        }
	}

	@Override
	public void doMSUB(int rs, int rt) {
		System.err.println("MSUB Instruction is not supported by recompiler");

	}

	@Override
	public void doMSUBU(int rs, int rt) {
		System.err.println("MSUBU Instruction is not supported by recompiler");

	}

	@Override
	public void doMTC0(int rt, int c0dr) {
		System.err.println("MTC0 Instruction is not supported by recompiler");

	}

	@Override
	public void doMTC1(int rt, int c1dr) {
        dynaCode.addJavaInstruction("fpr[" + c1dr + "] = Float.intBitsToFloat(" + dynaCode.getGPRCodeRepr(rt) + ");");
	}

	@Override
	public void doMTHI(int rs) {
		System.err.println("MTHI Instruction is not supported by recompiler");

	}

	@Override
	public void doMTIC(int rt) {
		System.err.println("MTIC Instruction is not supported by recompiler");

	}

	@Override
	public void doMTLO(int rs) {
		dynaCode.addLocal("int lo");
		dynaCode.addJavaInstruction("lo = " + dynaCode.getGPRCodeRepr(rs) + ";");
		dynaCode.addJavaInstruction("processor.hilo = ((processor.hilo >>> 32) << 32) | (((long) lo) & 0xffffffff);");
	}

	@Override
	public void doMULS(int fd, int fs, int ft) {
		dynaCode.fprAssignExpression(fd, dynaCode.getFPRCodeRepr(fs) + "*" + dynaCode.getFPRCodeRepr(ft));
	}

	@Override
	public void doMULT(int rs, int rt) {
		dynaCode.addJavaInstruction("processor.hilo = ((long) "
				+ dynaCode.getGPRCodeRepr(rs) + ") * ((long) "
				+ dynaCode.getGPRCodeRepr(rt) + ");");
	}

	@Override
	public void doMULTU(int rs, int rt) {
    	dynaCode.addJavaInstruction("processor.hilo = (((long) " + dynaCode.getGPRCodeRepr(rs) + ") & 0xffffffff) * (((long)" + dynaCode.getGPRCodeRepr(rt) + ") & 0xffffffff);");
	}

	@Override
	public void doNEGS(int fd, int fs) {
		// TODO: optimize if value are constants
        dynaCode.fprAssignExpression(fd, "0.0f - " + dynaCode.getFPRCodeRepr(fs) );
	}

	@Override
	public void doNOP() {
		System.err.println("NOP Instruction is not supported by recompiler");

	}

	@Override
	public void doNOR(int rd, int rs, int rt) {
		// TODO: optimize if value are constants
		dynaCode.gprAssignExpression(rd, "~("
				+ dynaCode.getGPRCodeRepr(rs) + " | "
				+ dynaCode.getGPRCodeRepr(rt) + ")");
	}

	@Override
	public void doOR(int rd, int rs, int rt) {
    	if (dynaCode.getFixedValue(rs) != null && dynaCode.getFixedValue(rt) != null) {
    		dynaCode.gprAssignValue(rd, dynaCode.getFixedValue(rs) | dynaCode.getFixedValue(rt));
    	} else {
    		dynaCode.gprAssignExpression(rd, dynaCode.getGPRCodeRepr( rs ) + " | " + dynaCode.getGPRCodeRepr( rt ));
    	}
	}

	@Override
	public void doORI(int rt, int rs, int uimm16) {
    	if (dynaCode.getFixedValue(rs) != null) {
    		dynaCode.gprAssignValue(rt, dynaCode.getFixedValue(rs) | uimm16);
    	} else {
    		dynaCode.gprAssignExpression(rt, dynaCode.getGPRCodeRepr( rs ) + " | 0x" + Integer.toHexString(uimm16) );
    	}
	}

	@Override
	public void doROTR(int rd, int rt, int sa) {
		System.err.println("ROTR Instruction is not supported by recompiler");

	}

	@Override
	public void doROTRV(int rd, int rt, int rs) {
		System.err.println("ROTRV Instruction is not supported by recompiler");

	}

	@Override
	public void doROUNDWS(int fd, int fs) {
		System.err.println("ROUNDWS Instruction is not supported by recompiler");

	}

	@Override
	public void doSB(int rt, int rs, int simm16) {
		if (dynaCode.getFixedValue(rs) != null) {
			// address is constant
			int page = 0;
			try {
				page = Memory.get_instance().indexFromAddr(
						dynaCode.getFixedValue(rs) + simm16);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			dynaCode.addJavaInstruction("memory.write8(" + page + ", "
					+ dynaCode.getGPRCodeRepr(rs) + " + " + simm16
					+ ", (byte) (" + dynaCode.getGPRCodeRepr(rt)
					+ " & 0xFF));");
		} else {
			dynaCode.addJavaInstruction("memory.write8("
					+ dynaCode.getGPRCodeRepr(rs) + " + " + simm16
					+ ", (byte) (" + dynaCode.getGPRCodeRepr(rt)
					+ " & 0xFF));");
		}
	}

	@Override
	public void doSC(int rt, int rs, int simm16) {
		System.err.println("SC Instruction is not supported by recompiler");

	}

	@Override
	public void doSEB(int rd, int rt) {
		if (dynaCode.getFixedValue(rt) != null) {			
			dynaCode.gprAssignValue(rd, (dynaCode.getFixedValue(rt) << 24) >> 24);
		} else {
			dynaCode.gprAssignExpression(rd, "(gpr[" + rt + "] << 24) >> 24");
		}
	}

	@Override
	public void doSEH(int rd, int rt) {
		if (dynaCode.getFixedValue(rt) != null) {
			dynaCode.gprAssignValue(rd, (dynaCode.getFixedValue(rt) << 16) >> 16);
		} else {
			dynaCode.gprAssignExpression(rd, "(" + dynaCode.getGPRCodeRepr(rt) + " << 16) >> 16");
		}
	}

	@Override
	public void doSH(int rt, int rs, int simm16) {
		if (dynaCode.getFixedValue(rs) != null) {
			int page = 0;
			try {
				page = Memory.get_instance().indexFromAddr(
						dynaCode.getFixedValue(rs) + simm16);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			dynaCode.addJavaInstruction("memory.write16(" + page + ", "
					+ dynaCode.getGPRCodeRepr(rs) + " + " + simm16
					+ ", (short) (" + dynaCode.getGPRCodeRepr(rt)
					+ " & 0xFFFF));");
		} else {
			dynaCode.addJavaInstruction("memory.write16("
					+ dynaCode.getGPRCodeRepr(rs) + " + " + simm16
					+ ", (short) (" + dynaCode.getGPRCodeRepr(rt)
					+ " & 0xFFFF));");
		}
	}

	@Override
	public void doSLL(int rd, int rt, int sa) {
		if (rd != 0) {
			dynaCode.addJavaInstruction("gpr[" + rd + "] = ("
					+ dynaCode.getGPRCodeRepr(rt) + " << " + sa + ");");
			dynaCode.unfixGPR(rd);
		}
	}

	@Override
	public void doSLLV(int rd, int rt, int rs) {
		if (rd != 0) {
			dynaCode.addJavaInstruction("gpr[" + rd + "] = ("
					+ dynaCode.getGPRCodeRepr(rt) + " << ("
					+ dynaCode.getGPRCodeRepr(rs) + " & 31));");
			dynaCode.unfixGPR(rd);
		}
	}

	@Override
	public void doSLT(int rd, int rs, int rt) {
		dynaCode.gprAssignExpression(rd, "processor.signedCompare("
				+ dynaCode.getGPRCodeRepr(rs) + ", " + dynaCode.getGPRCodeRepr(rt) + ")");
	}

	@Override
	public void doSLTI(int rt, int rs, int simm16) {
		dynaCode.gprAssignExpression(rt, "processor.signedCompare("
				+ dynaCode.getGPRCodeRepr(rs) + ", " + simm16 + ")");
	}

	@Override
	public void doSLTIU(int rt, int rs, int simm16) {
		dynaCode.gprAssignExpression(rt, "processor.unsignedCompare("
				+ dynaCode.getGPRCodeRepr(rs) + ", " + simm16 + ")");
	}

	@Override
	public void doSLTU(int rd, int rs, int rt) {
		dynaCode.gprAssignExpression(rd, "processor.unsignedCompare("
					+ dynaCode.getGPRCodeRepr(rs) + ", "
					+ dynaCode.getGPRCodeRepr(rt) + ")");
	}

	@Override
	public void doSQRTS(int fd, int fs) {
		System.err.println("SQRTS Instruction is not supported by recompiler");

	}

	@Override
	public void doSRA(int rd, int rt, int sa) {
		if (rd != 0) {
			dynaCode.addJavaInstruction("gpr[" + rd + "] = ("
					+ dynaCode.getGPRCodeRepr(rt) + " >> " + sa + ");");
			dynaCode.unfixGPR(rd);
		}
	}

	@Override
	public void doSRAV(int rd, int rt, int rs) {
		dynaCode.gprAssignExpression(rd, dynaCode.getGPRCodeRepr( rt ) + " >> (" + dynaCode.getGPRCodeRepr( rs ) + " & 31)");
	}

	@Override
	public void doSRL(int rd, int rt, int sa) {
		dynaCode.gprAssignExpression(rd, dynaCode.getGPRCodeRepr( rt ) + " >>> " + sa);
	}

	@Override
	public void doSRLV(int rd, int rt, int rs) {
        if (rd != 0) {
        	dynaCode.addJavaInstruction("gpr[" + rd + "] = (" + dynaCode.getGPRCodeRepr( rt ) + " >>> (" + dynaCode.getGPRCodeRepr( rs ) + " & 31));");
        	dynaCode.unfixGPR(rd);
        }
	}

	@Override
	public void doSUB(int rd, int rs, int rt) {
		System.err.println("SUB Instruction is not supported by recompiler");

	}

	@Override
	public void doSUBS(int fd, int fs, int ft) {
        dynaCode.addJavaInstruction("fpr[" + fd + "] = fpr[" + fs + "] - fpr[" + ft + "];");
	}

	@Override
	public void doSUBU(int rd, int rs, int rt) {
    	dynaCode.gprAssignExpression(rd, dynaCode.getGPRCodeRepr( rs ) + " - " + dynaCode.getGPRCodeRepr( rt ));
	}

	@Override
	public void doSVS(int vt, int rs, int simm14) {
		System.err.println("SVS Instruction is not supported by recompiler");

	}

	@Override
	public void doSW(int rt, int rs, int simm16) {
		if (dynaCode.getFixedValue(rs) != null) {
			int page = 0;
			int address = dynaCode.getFixedValue(rs) + simm16;
			try {
				page = Memory.get_instance().indexFromAddr(address);
			} catch (Exception e) {
				e.printStackTrace();
			}
			dynaCode.addJavaInstruction("memory.write32(0x" + Integer.toHexString(page) + ", 0x"
					+ Integer.toHexString(address) + ", "
					+ dynaCode.getGPRCodeRepr(rt) + ");");
		} else {
			dynaCode.addJavaInstruction("memory.write32("
					+ dynaCode.getGPRCodeRepr(rs) + " + " + simm16 + ", "
					+ dynaCode.getGPRCodeRepr(rt) + ");");
		}
	}

	@Override
	public void doSWC1(int ft, int rs, int simm16) {
    	dynaCode.addJavaInstruction("memory.write32(" + dynaCode.getGPRCodeRepr(rs) + " + " + simm16 + ", Float.floatToRawIntBits(fpr[" + ft + "]));");
	}

	@Override
	public void doSWL(int rt, int rs, int simm16) {
		
		dynaCode.addLocal("int address");
		dynaCode.addLocal("int offset");
		dynaCode.addLocal("int reg");
		dynaCode.addLocal("int data");

        dynaCode.addJavaInstruction("address = " + dynaCode.getGPRCodeRepr(rs) + " + " + simm16 + ";");
        dynaCode.addJavaInstruction("offset = address & 0x3;");
        dynaCode.addJavaInstruction("reg = " + dynaCode.getGPRCodeRepr(rt) + ";");
        dynaCode.addJavaInstruction("data = memory.read32(address & 0xfffffffc);");

        dynaCode.addJavaInstruction(
        "switch (offset) {" + 
            "case 0:" + 
                "data = (data & 0xffffff00) | (reg >> 24 & 0xff);" + 
                "break;" + 

            "case 1:" + 
                "data = (data & 0xffff0000) | (reg >> 16 & 0xffff);" + 
                "break;" + 

            "case 2:" + 
                "data = (data & 0xff000000) | (reg >> 8 & 0xffffff);" + 
                "break;" + 

            "case 3:" + 
                "data = reg;" + 
                "break;" + 
        	"}");

        dynaCode.addJavaInstruction("memory.write32(address & 0xfffffffc, data);");
	}

	@Override
	public void doSWR(int rt, int rs, int simm16) {        
		dynaCode.addLocal("int address");
		dynaCode.addLocal("int offset");
		dynaCode.addLocal("int reg");
		dynaCode.addLocal("int data");

        dynaCode.addJavaInstruction("address = " + dynaCode.getGPRCodeRepr(rs) + " + " + simm16 + ";");
        dynaCode.addJavaInstruction("offset = address & 0x3;");
        dynaCode.addJavaInstruction("reg = " + dynaCode.getGPRCodeRepr(rt) + ";");
        dynaCode.addJavaInstruction("data = memory.read32(address & 0xfffffffc);");

        dynaCode.addJavaInstruction(
        "switch (offset) {" + 
            "case 0:" + 
                "data = reg;" + 
                "break;" + 

            "case 1:" + 
                "data = ((reg << 8) & 0xffffff00) | (data & 0xff);" + 
                "break;" + 

            "case 2:" + 
                "data = ((reg << 16) & 0xffff0000) | (data & 0xffff);" + 
                "break;" + 

            "case 3:" + 
                "data = ((reg << 24) & 0xff000000) | (data & 0xffffff);" + 
                "break;" + 
        	"}");

        dynaCode.addJavaInstruction("memory.write32(address & 0xfffffffc, data);");
	}

	@Override
	public void doSYNC() {
		System.err.println("SYNC Instruction is not supported by recompiler");

	}

	@Override
	public void doSYSCALL(int code) {
		dynaCode.addJavaInstruction("jpcsp.HLE.SyscallHandler.syscall( 0x" + Integer.toHexString(code) + " );");
		dynaCode.unfixAll();
	}

	@Override
	public void doTRUNCWS(int fd, int fs) {
		dynaCode.fprAssignExpression(fd, "Float.intBitsToFloat((int) (" + dynaCode.getFPRCodeRepr(fs) + "))");
	}

	@Override
	public void doUNK(String reason) {
		System.err.println("UNK Instruction is not supported by recompiler");

	}

	@Override
	public void doVADD(int vsize, int vd, int vs, int vt) {
		System.err.println("VADD Instruction is not supported by recompiler");

	}

	@Override
	public void doVCMP(int vsize, int vs, int vt, int cond) {
		System.err.println("VCMP Instruction is not supported by recompiler");

	}

	@Override
	public void doVCRS(int vsize, int vd, int vs, int vt) {
		System.err.println("VCRS Instruction is not supported by recompiler");

	}

	@Override
	public void doVDET(int vsize, int vd, int vs, int vt) {
		System.err.println("VDET Instruction is not supported by recompiler");

	}

	@Override
	public void doVDIV(int vsize, int vd, int vs, int vt) {
		System.err.println("VDIV Instruction is not supported by recompiler");

	}

	@Override
	public void doVDOT(int vsize, int vd, int vs, int vt) {
		System.err.println("VDOT Instruction is not supported by recompiler");

	}

	@Override
	public void doVFIM(int vs, int imm16) {
		System.err.println("VFIM Instruction is not supported by recompiler");

	}

	@Override
	public void doVHDP(int vsize, int vd, int vs, int vt) {
		System.err.println("VHDP Instruction is not supported by recompiler");

	}

	@Override
	public void doVIIM(int vs, int imm16) {
		System.err.println("VIIM Instruction is not supported by recompiler");

	}

	@Override
	public void doVMAX(int vsize, int vd, int vs, int vt) {
		System.err.println("VMAX Instruction is not supported by recompiler");

	}

	@Override
	public void doVMIN(int vsize, int vd, int vs, int vt) {
		System.err.println("VMIN Instruction is not supported by recompiler");

	}

	@Override
	public void doVMUL(int vsize, int vd, int vs, int vt) {
		System.err.println("VMUL Instruction is not supported by recompiler");

	}

	@Override
	public void doVPFXD(int imm24) {
		System.err.println("VPFXD Instruction is not supported by recompiler");

	}

	@Override
	public void doVPFXS(int imm24) {
		System.err.println("VPFXS Instruction is not supported by recompiler");

	}

	@Override
	public void doVPFXT(int imm24) {
		System.err.println("VPFXT Instruction is not supported by recompiler");

	}

	@Override
	public void doVSBN(int vsize, int vd, int vs, int vt) {
		System.err.println("VSBN Instruction is not supported by recompiler");
	}

	@Override
	public void doVSCL(int vsize, int vd, int vs, int vt) {
		System.err.println("VSCL Instruction is not supported by recompiler");

	}

	@Override
	public void doVSCMP(int vsize, int vd, int vs, int vt) {
		System.err.println("VSCMP Instruction is not supported by recompiler");

	}

	@Override
	public void doVSGE(int vsize, int vd, int vs, int vt) {
		System.err.println("VSGE Instruction is not supported by recompiler");

	}

	@Override
	public void doVSLT(int vsize, int vd, int vs, int vt) {
		System.err.println("VSLT Instruction is not supported by recompiler");
	}

	@Override
	public void doVSUB(int vsize, int vd, int vs, int vt) {
		System.err.println("VSUB Instruction is not supported by recompiler");

	}

	@Override
	public void doWSBH(int rd, int rt) {
		System.err.println("WSBH Instruction is not supported by recompiler");

	}

	@Override
	public void doWSBW(int rd, int rt) {
		System.err.println("WSBW Instruction is not supported by recompiler");

	}

	@Override
	public void doXOR(int rd, int rs, int rt) {
       	dynaCode.gprAssignExpression(rd, dynaCode.getGPRCodeRepr( rs ) + " ^ " + dynaCode.getGPRCodeRepr( rt ));
	}

	@Override
	public void doXORI(int rt, int rs, int uimm16) {
       	dynaCode.gprAssignExpression(rt, dynaCode.getGPRCodeRepr( rs ) + " ^ " + uimm16 );
	}

	public void recompile(int insn) {
		decoder.process(this, insn);
	}

}
