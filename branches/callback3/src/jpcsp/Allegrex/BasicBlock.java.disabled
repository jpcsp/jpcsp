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
package jpcsp.Allegrex;

import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;

import jpcsp.Processor;

/**
 *
 * @author hli
 */
public class BasicBlock {

    protected int entry;
    protected int size;
    protected StringBuffer buffer = new StringBuffer();
    protected boolean freezed = false;
    protected static ClassPool pool = ClassPool.getDefault();
    protected Method method = null;
    protected long creationTimestamp = 0;
    protected CpuTracked cpu = null;
    protected int executionCount = 1;
    protected BasicBlock branchTrue = null;
    protected BasicBlock branchFalse = null;
    protected boolean stop = false;
    
    public final int getEntry() {
        return entry;
    }

    public final int getSize() {
        return entry;
    }

    public BasicBlock(CpuState cpu, int entry) {
        this.cpu = new CpuTracked(this, cpu);
        this.entry = entry;

        creationTimestamp = System.currentTimeMillis();
    }

    public void emit(String javaString) {
        if (!freezed) {
            if (javaString.endsWith(";")) {
                buffer.append(javaString + "\n");
            } else {
                buffer.append(javaString);
            }
        }
    }

    public void compile(Processor processor) {       
        while (!stop) {
            int opcode = cpu.fetchOpcode();
            Common.Instruction insn = Decoder.instruction(opcode);
            // insn.compile(processor, opcode);
        }
    }

    public void freeze() {
        cpu = null;

        Processor.log.debug("Freezing basic block : " + Integer.toHexString(entry));

        StringBuffer javaMethod = new StringBuffer();
        javaMethod.append("public static void execute(jpcsp.Processor processor, Integer entry) {\n");
        javaMethod.append("jpcsp.Memory memory = processor.memory; jpcsp.Allegrex.CpuState cpu = processor.cpu;\n");
        javaMethod.append("while(cpu.pc == entry.intValue()) {\n");
        javaMethod.append(buffer).append("}; };");

        Processor.log.debug(javaMethod.toString());

        CtClass dynaCtClass = pool.makeClass("BB" + Integer.toHexString(entry));
        try {
            dynaCtClass.addMethod(
                    CtNewMethod.make(javaMethod.toString(),
                    dynaCtClass));
            Class dynaClass = dynaCtClass.toClass();
            method = dynaClass.getDeclaredMethod("execute", Processor.class, Integer.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        freezed = true;
    }

    public void execute(Processor processor) {
        try {
            method.invoke(null, processor, entry);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class CpuTracked extends CpuState {

        public BasicBlock bb;

        public class RegisterTracked {

            public boolean loaded = false; // load the register content if not already done
            public boolean dirty = false; // save the register content back at basic block termination
            public boolean fixed = false; // the register helds a constant value
            public boolean labeled = false; // an associated local variable is created
        }
        public RegisterTracked gprTracked[];
        public RegisterTracked fprTracked[];
        public RegisterTracked mduTracked[];

        CpuTracked(BasicBlock bb, CpuState cpu) {
            this.bb = bb;
            super.copy(cpu);
        }

        public String ln(String javaString) {
            if (javaString.endsWith(";")) {
                return javaString + "\n";
            }
            return javaString;
        }
        
        public void resetTracker() {
            gprTracked = new RegisterTracked[32];
            fprTracked = new RegisterTracked[32];
            mduTracked = new RegisterTracked[2];
        }

        public void fixGpr(int register) {
            RegisterTracked tracked = gprTracked[register];
            tracked.loaded = false;
            tracked.dirty = true;
            tracked.fixed = true;
        }

        public void fixFpr(int register) {
            RegisterTracked tracked = fprTracked[register];
            tracked.loaded = false;
            tracked.dirty = true;
            tracked.fixed = true;
        }

        public String loadGpr(int register, boolean force) {
            RegisterTracked tracked = gprTracked[register];
            String line = "";
            if (!force && tracked.fixed) {
                return line;
            }
            if (!tracked.labeled) {
                tracked.labeled = true;
                line = ln("int gpr_" + register + " = cpu.gpr[" + register + "];");
            }
            tracked.loaded = true;
            tracked.dirty = false;
            tracked.fixed = false;
            return line;
        }

        public String loadFpr(int register, boolean force) {
            RegisterTracked tracked = fprTracked[register];
            String line = "";
            if (!force && tracked.fixed) {
                return line;
            }
            if (!tracked.labeled) {
                line = ln("float fpr_" + register + " = cpu.fpr[" + register + "];");
                tracked.labeled = true;
            }
            tracked.loaded = true;
            tracked.dirty = false;
            tracked.fixed = false;
            return line;
        }

        public String getGpr(int register) {
            if (gprTracked[register].fixed) {
                return Integer.toString(gpr[register]);
            }
            if (!gprTracked[register].labeled) {
                return "cpu.gpr[" + register + "]";
            }
            return "gpr_" + register;
        }

        public String getFpr(int register) {
            if (fprTracked[register].fixed) {
                return Float.toString(fpr[register]);
            }
            if (!fprTracked[register].labeled) {
                return "cpu.fpr[" + register + "]";
            }
            return "fpr_" + register;
        }

        public String alterGpr(int register) {
            RegisterTracked tracked = gprTracked[register];
            String line = "";
            if (!tracked.labeled) {
                line = ln("int gpr_" + register + ";");
                tracked.labeled = true;
            }
            tracked.loaded = false;
            tracked.dirty = true;
            tracked.fixed = false;
            return line;
        }

        public String alterFpr(int register) {
            RegisterTracked tracked = fprTracked[register];
            String line = "";
            if (!tracked.labeled) {
                line = ln("float fpr_" + register + ";");
                tracked.labeled = true;
            }
            tracked.loaded = false;
            tracked.dirty = true;
            tracked.fixed = false;
            return line;
        }

        public final String recSLL(int rd, int rt, int sa) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed) {
                    doSLL(rd, rt, sa);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = ((%2$s) << %3$d);", getGpr(rd), getGpr(rt), sa);
                }
            }
            return line;
        }

        public final String recSRL(int rd, int rt, int sa) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed) {
                    doSRL(rd, rt, sa);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = ((%2$s) >>> %3$d);", getGpr(rd), getGpr(rt), sa);
                }
            }
            return line;
        }

        public final String recSRA(int rd, int rt, int sa) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed) {
                    doSRA(rd, rt, sa);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = ((%2$s) >> %3$d);", getGpr(rd), getGpr(rt), sa);
                }
            }
            return line;
        }

        public final String recSLLV(int rd, int rt, int rs) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed && gprTracked[rs].fixed) {
                    doSLLV(rd, rt, rs);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = ((%2$s) << ((%3$s) & 31));", getGpr(rd), getGpr(rt), getGpr(rs));
                }
            }
            return line;
        }

        public final String recSRLV(int rd, int rt, int rs) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed && gprTracked[rs].fixed) {
                    doSRLV(rd, rt, rs);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = ((%2$s) >>> ((%3$s) & 31));", getGpr(rd), getGpr(rt), getGpr(rs));
                }
            }
            return line;
        }

        public final String recSRAV(int rd, int rt, int rs) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed && gprTracked[rs].fixed) {
                    doSRAV(rd, rt, rs);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = ((%2$s) >> ((%3$s) & 31));", getGpr(rd), getGpr(rt), getGpr(rs));
                }
            }
            return line;
        }

        public final String recADDU(int rd, int rs, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rs].fixed && gprTracked[rt].fixed) {
                    doADDU(rd, rs, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = (%2$s) + (%3$s);", getGpr(rd), getGpr(rs), getGpr(rt));
                }
            }
            return line;
        }

        public final String recSUBU(int rd, int rs, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rs].fixed && gprTracked[rt].fixed) {
                    doSUBU(rd, rs, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = (%2$s) - (%3$s);", getGpr(rd), getGpr(rs), getGpr(rt));
                }
            }
            return line;
        }

        public final String recAND(int rd, int rs, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rs].fixed && gprTracked[rt].fixed) {
                    doAND(rd, rs, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = (%2$s) & (%3$s);", getGpr(rd), getGpr(rs), getGpr(rt));
                }
            }
            return line;
        }

        public final String recOR(int rd, int rs, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rs].fixed && gprTracked[rt].fixed) {
                    doOR(rd, rs, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = (%2$s) | (%3$s);", getGpr(rd), getGpr(rs), getGpr(rt));
                }
            }
            return line;
        }

        public final String recXOR(int rd, int rs, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rs].fixed && gprTracked[rt].fixed) {
                    doXOR(rd, rs, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = (%2$s) ^ (%3$s);", getGpr(rd), getGpr(rs), getGpr(rt));
                }
            }
            return line;
        }

        public final String recNOR(int rd, int rs, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rs].fixed && gprTracked[rt].fixed) {
                    doNOR(rd, rs, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = ~((%2$s) | (%3$s));", getGpr(rd), getGpr(rs), getGpr(rt));
                }
            }
            return line;
        }

        public final String recSLT(int rd, int rs, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rs].fixed && gprTracked[rt].fixed) {
                    doSLT(rd, rs, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = ((%2$s) - (%3$s)) >>> 31;", getGpr(rd), getGpr(rs), getGpr(rt));
                }
            }
            return line;
        }

        public final String recSLTU(int rd, int rs, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rs].fixed && gprTracked[rt].fixed) {
                    doSLT(rd, rs, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = (((%2$s) - (%3$s)) ^ (%2$s) ^ (%3$s)) >>> 31;", getGpr(rd), getGpr(rs), getGpr(rt));
                }
                gpr[rd] = unsignedCompare(gpr[rs], gpr[rt]);
            }
            return line;
        }

        public final String recADDIU(int rt, int rs, int simm16) {
            String line = "";
            if (rt != 0) {
                if (gprTracked[rs].fixed) {
                    doADDIU(rt, rs, simm16);
                    fixGpr(rt);
                } else {
                    line += loadGpr(rs, false);
                    line += alterGpr(rt);
                    line += String.format("%1$s = (%2$s) + (%3$d);", getGpr(rt), getGpr(rs), simm16);
                }
            }
            return line;
        }

        public final String recSLTI(int rt, int rs, int simm16) {
            String line = "";
            if (rt != 0) {
                if (gprTracked[rs].fixed) {
                    doSLTI(rt, rs, simm16);
                    fixGpr(rt);
                } else {
                    line += loadGpr(rs, false);
                    line += alterGpr(rt);
                    line += String.format("%1$s = ((%2$s) - (%3$d)) >>> 31;", getGpr(rt), getGpr(rs), simm16);
                }
            }
            return line;
        }

        public final String recSLTIU(int rt, int rs, int simm16) {
            String line = "";
            if (rt != 0) {
                if (gprTracked[rs].fixed) {
                    doSLTIU(rt, rs, simm16);
                    fixGpr(rt);
                } else {
                    line += loadGpr(rs, false);
                    line += alterGpr(rt);
                    line += String.format("%1$s = (((%2$s) - (%3$d)) ^ (%2$s) ^ (%3$d)) >>> 31;", getGpr(rt), getGpr(rs), simm16);
                }
            }
            return line;
        }

        public final String recANDI(int rt, int rs, int uimm16) {
            String line = "";
            if (rt != 0) {
                if (gprTracked[rs].fixed) {
                    doANDI(rt, rs, uimm16);
                    fixGpr(rt);
                } else {
                    line += loadGpr(rs, false);
                    line += alterGpr(rt);
                    line += String.format("%1$s = (%2$s) & (%3$d);", getGpr(rt), getGpr(rs), uimm16);
                }
            }
            return line;
        }

        public final String recORI(int rt, int rs, int uimm16) {
            String line = "";
            if (rt != 0) {
                if (gprTracked[rs].fixed) {
                    doORI(rt, rs, uimm16);
                    fixGpr(rt);
                } else {
                    line += loadGpr(rs, false);
                    line += alterGpr(rt);
                    line += String.format("%1$s = (%2$s) | (%3$d);", getGpr(rt), getGpr(rs), uimm16);
                }
            }
            return line;
        }

        public final String recXORI(int rt, int rs, int uimm16) {
            String line = "";
            if (rt != 0) {
                if (gprTracked[rs].fixed) {
                    doXORI(rt, rs, uimm16);
                    fixGpr(rt);
                } else {
                    line += loadGpr(rs, false);
                    line += alterGpr(rt);
                    line += String.format("%1$s = (%2$s) ^ (%3$d);", getGpr(rt), getGpr(rs), uimm16);
                }
            }
            return line;
        }

        public final String recLUI(int rt, int uimm16) {
            String line = "";
            if (rt != 0) {
                doLUI(rt, uimm16);
                fixGpr(rt);
            }
            return line;
        }

        public final String recROTR(int rd, int rt, int sa) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed) {
                    doROTR(rd, rt, sa);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = Integer.rotateRight(%2$s, %3$d);", getGpr(rd), getGpr(rt), sa);
                }
            }
            return line;
        }

        public final String recROTRV(int rd, int rt, int rs) {
            String line = "";
            // no need of "gpr[rs] & 31", rotateRight does it for us
            if (rd != 0) {
                if (gprTracked[rt].fixed && gprTracked[rs].fixed) {
                    doROTRV(rd, rt, rs);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = Integer.rotateRight(%2$s, %3$s);", getGpr(rd), getGpr(rt), getGpr(rs));
                }
            }
            return line;
        }

        public final String recMOVZ(int rd, int rs, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed && gprTracked[rs].fixed) {
                    doMOVZ(rd, rs, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("if (%3$s == 0) %1$s = %2$s;", getGpr(rd), getGpr(rt), getGpr(rs));
                }
            }
            return line;
        }

        public final String recMOVN(int rd, int rs, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed && gprTracked[rs].fixed) {
                    doMOVN(rd, rs, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("if (%3$s != 0) %1$s = %2$s;", getGpr(rd), getGpr(rt), getGpr(rs));
                }
            }
            return line;
        }

        public final String recCLZ(int rd, int rs) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rs].fixed) {
                    doCLZ(rd, rs);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = Integer.numberOfLeadingZeros(%2$s);", getGpr(rd), getGpr(rs));
                }
            }
            return line;
        }

        public final String recCLO(int rd, int rs) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rs].fixed) {
                    doCLO(rd, rs);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = Integer.numberOfLeadingZeros(~(%2$s));", getGpr(rd), getGpr(rs));
                }
            }
            return line;
        }

        public final String recMAX(int rd, int rs, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rs].fixed && gprTracked[rt].fixed) {
                    doMAX(rd, rs, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = Math.max((%2$s), (%3$s));", getGpr(rd), getGpr(rt), getGpr(rs));
                }
                gpr[rd] = Math.max(gpr[rs], gpr[rt]);
            }
            return line;
        }

        public final String recMIN(int rd, int rs, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rs].fixed && gprTracked[rt].fixed) {
                    doMAX(rd, rs, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = Math.min((%2$s), (%3$s));", getGpr(rd), getGpr(rt), getGpr(rs));
                }
            }
            return line;
        }

        public final String recEXT(int rt, int rs, int rd, int sa) {
            String line = "";
            if (rt != 0) {
                if (gprTracked[rs].fixed) {
                    doEXT(rt, rs, rd, sa);
                    fixGpr(rt);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format(
                            "%1$s = ((%2$s) >>> (%4$d)) & (%3$d);",
                            getGpr(rt), getGpr(rs), ~(~0 << (rd + 1)), sa);
                }
            }
            return line;
        }

        public final String recINS(int rt, int rs, int rd, int sa) {
            String line = "";
            if (rt != 0) {
                if (gprTracked[rs].fixed) {
                    doINS(rt, rs, rd, sa);
                    fixGpr(rt);
                } else {
                    line += loadGpr(rs, false);
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format(
                            "%1$s = ((%1$s) & ~(%3$d)) | (((%2$s) << (%4$d)) & (%3$d));",
                            getGpr(rt), getGpr(rs), (~(~0 << (rd - sa + 1)) << sa), sa);
                }
            }
            return line;
        }

        public final String recWSBH(int rd, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed) {
                    doWSBH(rd, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = Integer.rotateRight(Integer.reverseBytes(%2$s), 16);", getGpr(rd), getGpr(rt));
                }
            }
            return line;
        }

        public final String recWSBW(int rd, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed) {
                    doWSBW(rd, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = Integer.reverseBytes(%2$s);", getGpr(rd), getGpr(rt));
                }
            }
            return line;
        }

        public final String recSEB(int rd, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed) {
                    doSEB(rd, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = (int) (byte) (%2$s);", getGpr(rd), getGpr(rt));
                }
            }
            return line;
        }

        public final String recBITREV(int rd, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed) {
                    doBITREV(rd, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = Integer.reverse(%2$s);", getGpr(rd), getGpr(rt));
                }
            }
            return line;
        }

        public final String recSEH(int rd, int rt) {
            String line = "";
            if (rd != 0) {
                if (gprTracked[rt].fixed) {
                    doSEH(rd, rt);
                    fixGpr(rd);
                } else {
                    line += loadGpr(rt, false);
                    line += alterGpr(rd);
                    line += String.format("%1$s = (int) (short) (%2$s);", getGpr(rd), getGpr(rt));
                }
            }
            return line;
        }
    }
}
