/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp;

import jpcsp.Allegrex.*;
import java.nio.ByteBuffer;
import java.util.*;
   
/**
 *
 * @author hli
 */
public class Recompiler extends Processor {

    public BasicBlock currentBasicBlock = null;
    
    protected Map<Integer, BasicBlock> basicBlockMap = new HashMap<Integer, BasicBlock>();
    
    @Override
    public void load(ByteBuffer buffer) {
    }

    @Override
    public void save(ByteBuffer buffer) {
    }

    public void run() {

        // check whether a basic block is in progress
        currentBasicBlock = basicBlockMap.get(cpu.pc);
        if (currentBasicBlock != null) {
            currentBasicBlock.execute(this);
            currentBasicBlock = null;
            return;
        }
        
        currentBasicBlock = new BasicBlock(cpu, cpu.pc);
        basicBlockMap.put(cpu.pc, currentBasicBlock);
        currentBasicBlock.compile(this);
    }
}
