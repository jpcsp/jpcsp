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
import jpcsp.Debugger.DisassemblerModule.DisassemblerFrame;
import jpcsp.Debugger.FileLogger.FileLoggerFrame;
import jpcsp.Debugger.MemoryViewer;

/**
 *
 * @author hli
 */
public class State extends jpcsp.HLE.Modules {

    // re-enable this when we remove getInstance from all code
    // also Emulator calls "new Processor()" too!
    //public static final Processor processor = new Processor();
    public static final Memory memory;
    public static final Controller controller;

    public static DisassemblerFrame debugger; // can be null
    public static MemoryViewer memoryViewer; // can be null
    public static final FileLoggerFrame fileLogger;

    public static String discId;
    public static String title;
    // make sure these are valid filenames because it gets used by the screenshot system
    public static final String DISCID_UNKNOWN_NOTHING_LOADED = "[unknown, nothing loaded]";
    public static final String DISCID_UNKNOWN_FILE = "[unknown, file]";
    public static final String DISCID_UNKNOWN_UMD = "[unknown, umd]";

    public static boolean captureGeNextFrame;
    public static boolean replayGeNextFrame;

    static {
        //processor = new Processor();
        memory = Memory.getInstance();
        controller = new Controller();

        //debugger = new DisassemblerFrame();
        //memoryViewer = new MemoryViewer();
        fileLogger = new FileLoggerFrame();

        discId = DISCID_UNKNOWN_NOTHING_LOADED;

        captureGeNextFrame = false;
        replayGeNextFrame = false;
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
