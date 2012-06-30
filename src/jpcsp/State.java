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

import jpcsp.Debugger.MemoryViewer;
import jpcsp.Debugger.DisassemblerModule.DisassemblerFrame;
import jpcsp.Debugger.FileLogger.FileLoggerFrame;
import jpcsp.Debugger.ImageViewer;

/**
 *
 * @author hli
 */
public class State extends jpcsp.HLE.Modules {
    public static final Memory memory;
    public static final Controller controller;
    public static DisassemblerFrame debugger;
    public static MemoryViewer memoryViewer;
    public static ImageViewer imageViewer;
    public static final FileLoggerFrame fileLogger;
    public static String discId;
    public static String title;

    // make sure these are valid filenames because it gets used by the screenshot system
    public static final String DISCID_UNKNOWN_NOTHING_LOADED = "[unknown, nothing loaded]";
    public static final String DISCID_UNKNOWN_FILE = "[unknown, file]";
    public static final String DISCID_UNKNOWN_UMD = "[unknown, umd]";

    public static boolean captureGeNextFrame;
    public static boolean replayGeNextFrame;
    public static boolean exportGeNextFrame;
    public static boolean exportGeOnlyVisibleElements;

    static {
        memory = Memory.getInstance();
        controller = Controller.getInstance();
        fileLogger = new FileLoggerFrame();
        discId = DISCID_UNKNOWN_NOTHING_LOADED;
        captureGeNextFrame = false;
        replayGeNextFrame = false;
        exportGeNextFrame = false;
    }
}