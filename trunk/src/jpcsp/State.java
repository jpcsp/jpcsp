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
import jpcsp.Debugger.ElfHeaderInfo;
import jpcsp.Debugger.FileLogger.FileLoggerFrame;
import jpcsp.Debugger.ImageViewer;
import jpcsp.Debugger.InstructionCounter;
import jpcsp.GUI.CheatsGUI;
import jpcsp.GUI.ControlsGUI;
import jpcsp.GUI.LogGUI;
import jpcsp.GUI.MemStickBrowser;
import jpcsp.GUI.SettingsGUI;
import jpcsp.log.LogWindow;

/**
 *
 * @author hli
 */
public class State extends jpcsp.HLE.Modules {

    public static final Memory memory;
    public static final Controller controller;
    // additional frames
    public static DisassemblerFrame debugger;
    public static MemoryViewer memoryViewer;
    public static ImageViewer imageViewer;
    public static FileLoggerFrame fileLogger;
    public static CheatsGUI cheatsGUI;
    public static ControlsGUI controlsGUI;
    public static LogGUI logGUI;
    public static ElfHeaderInfo elfHeader;
    public static SettingsGUI settingsGUI;
    public static MemStickBrowser memStickBrowser;
    public static LogWindow logWindow;
    public static InstructionCounter instructionCounter;
    // disc related
    public static String discId;
    public static String title;
    // The UMD ID extracted from the UMD_DATA.BIN file
    public static String umdId;
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
        discId = DISCID_UNKNOWN_NOTHING_LOADED;
        captureGeNextFrame = false;
        replayGeNextFrame = false;
        exportGeNextFrame = false;
    }
}
