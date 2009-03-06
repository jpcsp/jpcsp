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

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.Allegrex.CpuState;
import jpcsp.Allegrex.compiler.Compiler;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Debugger.InstructionCounter;
import jpcsp.Debugger.StepLogger;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.pspdisplay;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.textures.TextureCache;

import org.apache.log4j.Logger;

public class Emulator implements Runnable {
    private static Emulator instance;
    private static Processor processor;
    private static Clock clock;
    private boolean moduleLoaded;
    private Thread mainThread;
    public static boolean run = false;
    public static boolean pause = false;
    private static MainGUI gui;
    private InstructionCounter instructionCounter;
    public static Logger log = Logger.getLogger("misc");
    private SceModule module;
    private int firmwareVersion = 150;

    public Emulator(MainGUI gui) {
        Emulator.gui = gui;
        processor = new Processor();
        clock = new Clock();

        moduleLoaded = false;
        mainThread = new Thread(this, "Emu");

        instance = this;
    }

    public static void exit() {
        log.info(TextureCache.getInstance().statistics.toString());
        Compiler.exit();
        RuntimeContext.exit();
        if (ThreadMan.getInstance().statistics != null && pspdisplay.getInstance().statistics != null) {
            long totalMillis = getClock().milliTime();
            long displayMillis = pspdisplay.getInstance().statistics.cumulatedTimeMillis;
            long cpuMillis = totalMillis - displayMillis;
            long cpuCycles = ThreadMan.getInstance().statistics.allCycles;
            double totalSecs = totalMillis / 1000.0;
            double displaySecs = displayMillis / 1000.0;
            double cpuSecs = cpuMillis / 1000.0;
            if (totalSecs != 0) {
                log.info("Total execution time: " + String.format("%.3f", totalSecs) + "s");
                log.info("     PSP CPU time: " + String.format("%.3f", cpuSecs) + "s (" + String.format("%.1f", cpuSecs / totalSecs * 100) + "%)");
                log.info("     Display time: " + String.format("%.3f", displaySecs) + "s (" + String.format("%.1f", displaySecs / totalSecs * 100) + "%)");
            }
            if (VideoEngine.getStatistics() != null) {
                long videoCalls = VideoEngine.getStatistics().numberCalls;
                if (videoCalls != 0) {
                	log.info("Elapsed time per frame: " + String.format("%.3f", totalSecs / videoCalls) + "s:");
                	log.info("    Display time: " + String.format("%.3f", displaySecs / videoCalls));
                	log.info("    PSP CPU time: " + String.format("%.3f", cpuSecs / videoCalls) + " (" + (cpuCycles / videoCalls) + " instr)");
                }
                if (totalSecs  != 0) {
                	log.info("Display Speed: " + String.format("%.2f", videoCalls / totalSecs) + " FPS");
                }
            }
            if (cpuSecs != 0) {
                log.info("PSP CPU Speed: " + String.format("%.3f", cpuCycles / cpuSecs / (1024 * 1024)) + "Mhz (" + (long) (cpuCycles / cpuSecs) + " instructions per second)");
            }
        }
    }

    public SceModule load(String pspfilename, ByteBuffer f) throws IOException, GeneralJpcspException {

        initNewPsp();

        module = jpcsp.Loader.getInstance().LoadModule(pspfilename, f, 0x08800000);

        //if (module.fileFormat == Loader.FORMAT_UNKNOWN ||
        //    (module.fileFormat & Loader.FORMAT_PSP) == Loader.FORMAT_PSP) {
        if ((module.fileFormat & Loader.FORMAT_ELF) != Loader.FORMAT_ELF) {
            throw new GeneralJpcspException("File format not supported!");
        }

        moduleLoaded = true;
        initCpu();

        // Delete breakpoints and reset to PC
        if (State.debugger != null) {
            State.debugger.resetDebugger();
        }

        // Update instruction counter dialog with the new app
        if (instructionCounter != null) {
            instructionCounter.setModule(module);
        }

        return module;
    }

    private void initCpu() {
        RuntimeContext.update();
        //set the default values for registers not sure if they are correct and UNTESTED!!
        //some settings from soywiz/pspemulator
        CpuState cpu = processor.cpu;

        cpu.pc = module.entry_addr; //set the pc register.
        cpu.npc = cpu.pc + 4;
        // Gets set in ThreadMan cpu.gpr[4] = 0; //a0
        // Gets set in ThreadMan cpu.gpr[5] = (int) romManager.getBaseoffset() + (int) elf.getHeader().getE_entry(); // argumentsPointer a1 reg
        //cpu.gpr[6] = 0; //a2
        // Gets set in ThreadMan cpu.gpr[26] = 0x09F00000; //k0
        cpu.gpr[27] = 0; //k1 should probably be 0
        cpu.gpr[28] = module.gp_value; //gp reg    gp register should get the GlobalPointer!!!
        // Gets set in ThreadMan cpu.gpr[29] = 0x09F00000; //sp
        // Gets set in ThreadMan cpu.gpr[31] = 0x08000004; //ra, should this be 0?
        // All other registers are uninitialised/random values

        jpcsp.HLE.ThreadMan.getInstance().Initialise(cpu.pc, module.attribute, module.pspfilename, module.modid);
        jpcsp.HLE.psputils.getInstance().Initialise();
        jpcsp.HLE.pspge.getInstance().Initialise();
        jpcsp.HLE.pspdisplay.getInstance().Initialise();
        jpcsp.HLE.pspiofilemgr.getInstance().Initialise();

        if (State.memoryViewer != null)
            State.memoryViewer.RefreshMemory();
    }

    private void initNewPsp() {
        moduleLoaded = false;

        RuntimeContext.reset();
        getClock().reset();
        getProcessor().reset();
        Memory.getInstance().Initialise();

        NIDMapper.getInstance().Initialise();
        Loader.getInstance().reset();
        State.fileLogger.resetLogging();

        // Firmware version can be changed by calling setFirmwareVersion, but
        // you have to do it before the loader reaches the "process imports" stage.

        // TODO cleanup initialisation
        // - load UMD calls setFirmwareVersion before initNewPsp., because PSF is read separate from BIN
        // - load PBP calls initNewPsp before setFirmwareVersion, because PSF is embedded in PBP
        // - load ELF/PRX only calls initNewPsp, because it doesn't have a PSF
        // this means some stuff is initialised twice (but with different parameters)

        jpcsp.HLE.modules.HLEModuleManager.getInstance().Initialise(firmwareVersion);
        jpcsp.HLE.kernel.Managers.reset();
        jpcsp.HLE.pspSysMem.getInstance().Initialise();
    }

    @Override
    public void run()
    {
        RuntimeContext.isActive = Settings.getInstance().readBool("emu.compiler");

        clock.resume();

        while (true) {
    		if (pause) {
    			clock.pause();
	            try {
	            	synchronized(this) {
            			while (pause) {
            				wait();
            			}
	                }
	            } catch (InterruptedException e) {
	            	// Ignore exception
        		}
    			clock.resume();
            }

            if (RuntimeContext.isActive) {
            	RuntimeContext.run();
            } else {
                processor.step();
                jpcsp.HLE.pspge.getInstance().step();
                jpcsp.HLE.ThreadMan.getInstance().step();
                jpcsp.HLE.pspdisplay.getInstance().step();
                jpcsp.HLE.modules.HLEModuleManager.getInstance().step();
                State.controller.checkControllerState();

                if (State.debugger != null)
                    State.debugger.step();
                //delay(cpu.numberCyclesDelay());
            }
        }

    }

    public synchronized void RunEmu()
    {
        if (!moduleLoaded) {
            Emulator.log.debug("Nothing loaded, can't run...");
            return;
        }

        if (pause)
        {
            pause = false;
            notifyAll();
        }
        else if (!run)
        {
            run = true;
            mainThread.start();
        }

        jpcsp.HLE.pspdisplay.getInstance().setDirty(true);
        jpcsp.HLE.ThreadMan.getInstance().clearSyscallFreeCycles();

        gui.RefreshButtons();
        if (State.debugger != null)
            State.debugger.RefreshButtons();
    }

    // static so Memory can pause emu on invalid read/write
    public static synchronized void PauseEmu()
    {
        if (run && !pause)
        {
            pause = true;

            // TODO execute this stuff in the gui thread
            {
                gui.RefreshButtons();

                if (State.debugger != null) {
                    State.debugger.RefreshButtons();
                    State.debugger.SafeRefreshDebugger(true);
                }

                if (State.memoryViewer != null)
                    State.memoryViewer.RefreshMemory();
            }

            StepLogger.flush();
        }
    }

    // static so Memory can pause emu on invalid read/write
    public static final int EMU_STATUS_OK = 0x00;
    public static final int EMU_STATUS_UNKNOWN = 0xFFFFFFFF;
    public static final int EMU_STATUS_WDT_IDLE = 0x01;
    public static final int EMU_STATUS_WDT_HOG = 0x02;
    public static final int EMU_STATUS_WDT_ANY = EMU_STATUS_WDT_IDLE | EMU_STATUS_WDT_HOG;
    public static final int EMU_STATUS_MEM_READ = 0x04;
    public static final int EMU_STATUS_MEM_WRITE = 0x08;
    public static final int EMU_STATUS_MEM_ANY = EMU_STATUS_MEM_READ | EMU_STATUS_MEM_WRITE;
    public static final int EMU_STATUS_BREAKPOINT = 0x10;
    public static final int EMU_STATUS_UNIMPLEMENTED = 0x20;
    public static final int EMU_STATUS_PAUSE = 0x40;
    public static final int EMU_STATUS_JUMPSELF = 0x80;
    public static synchronized void PauseEmuWithStatus(int status)
    {
        if (run && !pause)
        {
            pause = true;
            // TODO execute this stuff in the gui thread
            {
                gui.RefreshButtons();

                if (State.debugger != null) {
                    State.debugger.RefreshButtons();
                    State.debugger.SafeRefreshDebugger(true);
                }

                if (State.memoryViewer != null)
                    State.memoryViewer.RefreshMemory();
            }

            StepLogger.setStatus(status);
            StepLogger.flush();
        }
    }

    public static void setFpsTitle(String fps)
    {
         gui.setMainTitle(fps);
    }

    public static Processor getProcessor() {
        return processor;
    }

    public static Memory getMemory() {
        return Memory.getInstance();
    }

    public static Clock getClock() {
    	return clock;
    }

    public static Emulator getInstance() {
        return instance;
    }

    public void setInstructionCounter(InstructionCounter instructionCounter) {
        this.instructionCounter = instructionCounter;
        instructionCounter.setModule(module);
    }

    /** version in this format: ABB, where A = major and B = minor, for example 271 */
    public void setFirmwareVersion(int firmwareVersion) {
        this.firmwareVersion = firmwareVersion;

        NIDMapper.getInstance().Initialise();
        jpcsp.HLE.modules.HLEModuleManager.getInstance().Initialise(this.firmwareVersion);
    }

    /** version in this format: "A.BB", where A = major and B = minor, for example "2.71" */
    public void setFirmwareVersion(String firmwareVersion) {
        setFirmwareVersion(jpcsp.HLE.modules.HLEModuleManager.psfFirmwareVersionToInt(firmwareVersion));
    }
}
