/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp;

import java.nio.ByteBuffer;
import jpcsp.Debugger.DisassemblerModule.DisassemblerFrame;
import jpcsp.Debugger.FileLogger.FileLoggerFrame;
import jpcsp.Debugger.MemoryViewer;

/**
 *
 * @author hli
 */
public class State extends jpcsp.HLE.Modules {

    // re-enable this when we remove getInstance from all code
    // also Emulator calls new Processor() too!
    //public static final Processor processor = new Processor();
    public static final Memory memory;
    public static final Controller controller;

    public static DisassemblerFrame debugger; // can be null
    public static MemoryViewer memoryViewer; // can be null
    public static final FileLoggerFrame fileLogger;
    
    static {
        //processor = new Processor();
        memory = Memory.getInstance();
        controller = new Controller();
        
        //debugger = new DisassemblerFrame();
        //memoryViewer = new MemoryViewer();
        fileLogger = new FileLoggerFrame();
    }
    
    @Override
    public void step() {
        //processor.step();

        super.step();
    }

    @Override
    public void load(ByteBuffer buffer) {
        //processor.load(buffer);
        memory.load(buffer);

        super.load(buffer);
    }

    @Override
    public void save(ByteBuffer buffer) {
        //processor.save(buffer);
        memory.save(buffer);

        super.save(buffer);
    }
}
