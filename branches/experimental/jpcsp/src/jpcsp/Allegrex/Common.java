/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.Allegrex;

import jpcsp.Processor;

/**
 *
 * @author hli
 */
public class Common {

    public static abstract class Instruction {

        private int count = 0;
        
        public abstract void interpret(Processor processor, int insn);

        public abstract void compile(Processor processor, int insn);

        public abstract String disasm(int address, int insn);

        public void resetCount() {
            count = 0;
        }
        
        public void increaseCount() {
            count++;
        }
        
        public int getCount() {
            return count;
        }
        
        public Instruction instance(int insn) {
            return this;
        }
        
        public Instruction(int index) {
            jpcsp.Allegrex.Common.m_instances[index] = this;
        }

        public Instruction() {
        }
    }

    public static abstract class STUB extends Instruction {

        @Override
        public void interpret(Processor processor, int insn) {
            instance(insn).interpret(processor, insn);
        }

        @Override
        public void compile(Processor processor, int insn) {
            instance(insn).compile(processor, insn);
        }

        @Override
        public String disasm(int address, int insn) {
            return instance(insn).disasm(address, insn);
        }

        @Override
        public abstract Instruction instance(int insn);        
    }
    public static final Instruction UNK = new Instruction() {

        @Override
        public void interpret(Processor processor, int insn) {
        }

        @Override
        public void compile(Processor processor, int insn) {
        }

        @Override
        public String disasm(int address, int insn) {
            return "Unknown instruction (" + Integer.toHexString(insn) + " " + Integer.toString(insn, 2) + ") at 0x" + Integer.toHexString(address);
        }
    };
    
    protected static Instruction[] m_instances = new Instruction[234];
    
    public static final Instruction[] instructions() {
        return m_instances;
    }
}