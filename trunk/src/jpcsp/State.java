/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpcsp;

import java.nio.ByteBuffer;

/**
 *
 * @author hli
 */
public class State extends jpcsp.HLE.Modules {

    // re-enable this when we remove getInstance from all code
    // also Emulator calls new Processor() too!
    //public static final Processor processor = new Processor();
    public static final Memory memory = Memory.getInstance();

    @Override
    public void step() {
        processor.step();

        super.step();
    }

    @Override
    public void load(ByteBuffer buffer) {
        processor.load(buffer);
        memory.load(buffer);

        super.load(buffer);
    }

    @Override
    public void save(ByteBuffer buffer) {
        processor.save(buffer);
        memory.save(buffer);

        super.save(buffer);
    }
}
