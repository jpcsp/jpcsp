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
import jpcsp.Allegrex.compiler.Profiler;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Debugger.InstructionCounter;
import jpcsp.Debugger.StepLogger;
import jpcsp.HLE.Modules;
import jpcsp.HLE.SyscallHandler;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.graphics.VertexCache;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.textures.TextureCache;
import jpcsp.hardware.Battery;
import jpcsp.hardware.Interrupts;
import jpcsp.scheduler.Scheduler;

import org.apache.log4j.Logger;

/*
 * TODO list:
 * 1. Cleanup initialisation in initNewPsp():
 *  - UMD: calls setFirmwareVersion before initNewPsp (PSF is read separate from BOOT.BIN).
 *  - PBP: calls initNewPsp before setFirmwareVersion (PSF is embedded in PBP).
 *  - ELF/PRX: only calls initNewPsp (doesn't have a PSF).
 */

public class Emulator implements Runnable {
    private static Emulator instance;
    private static Processor processor;
    private static Clock clock;
    private static Scheduler scheduler;
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
        scheduler = Scheduler.getInstance();

        moduleLoaded = false;
        mainThread = new Thread(this, "Emu");

        instance = this;
    }

    public static void exit() {
        log.info(TextureCache.getInstance().statistics.toString());
        log.info(VertexCache.getInstance().statistics.toString());
        Compiler.exit();
        RuntimeContext.exit();
        Profiler.exit();
        if (Modules.ThreadManForUserModule.statistics != null && Modules.sceDisplayModule.statistics != null) {
            long totalMillis = getClock().milliTime();
            long displayMillis = Modules.sceDisplayModule.statistics.cumulatedTimeMillis;
            long syscallMillis = SyscallHandler.durationStatistics.cumulatedTimeMillis;
            long idleMillis = RuntimeContext.idleDuration.cumulatedTimeMillis;
            long cpuMillis = totalMillis - displayMillis - syscallMillis - idleMillis;
            long cpuCycles = Modules.ThreadManForUserModule.statistics.allCycles;
            double totalSecs = totalMillis / 1000.0;
            double displaySecs = displayMillis / 1000.0;
            double syscallSecs = syscallMillis / 1000.0;
            double cpuSecs = cpuMillis / 1000.0;
            if (totalSecs != 0) {
                log.info("Total execution time: " + String.format("%.3f", totalSecs) + "s");
                log.info("     PSP CPU time: " + String.format("%.3f", cpuSecs) + "s (" + String.format("%.1f", cpuSecs / totalSecs * 100) + "%)");
                log.info("     Display time: " + String.format("%.3f", displaySecs) + "s (" + String.format("%.1f", displaySecs / totalSecs * 100) + "%)");
                log.info("     Syscall time: " + String.format("%.3f", syscallSecs) + "s (" + String.format("%.1f", syscallSecs / totalSecs * 100) + "%)");
            }
            log.info(SyscallHandler.durationStatistics.toString());
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
                log.info("PSP CPU Speed: " + String.format("%.2f", cpuCycles / cpuSecs / 1000000.0) + "MHz (" + (long) (cpuCycles / cpuSecs) + " instructions per second)");
            }
        }
    }

    public SceModule load(String pspfilename, ByteBuffer f) throws IOException, GeneralJpcspException {
    	return load(pspfilename, f, false);
    }

    public SceModule load(String pspfilename, ByteBuffer f, boolean fromSyscall) throws IOException, GeneralJpcspException {

        initNewPsp();

        module = jpcsp.Loader.getInstance().LoadModule(pspfilename, f, MemoryMap.START_USERSPACE + 0x4000);

        if ((module.fileFormat & Loader.FORMAT_ELF) != Loader.FORMAT_ELF) {
            throw new GeneralJpcspException("File format not supported!");
        }

        moduleLoaded = true;
        initCpu(fromSyscall);

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

    private void initCpu(boolean fromSyscall) {
        RuntimeContext.update();

        CpuState cpu = processor.cpu;

        cpu.pc = module.entry_addr; //PC.
        cpu.npc = cpu.pc + 4;
        cpu.gpr[27] = 0; //k1.
        cpu.gpr[28] = module.gp_value; //gp_reg.

        Modules.ThreadManForUserModule.Initialise(module, cpu.pc, module.attribute, module.pspfilename, module.modid, fromSyscall);
        Modules.UtilsForUserModule.Initialise();
        Modules.sceGe_userModule.Initialise();
        Modules.sceDisplayModule.Initialise();
        Modules.IoFileMgrForUserModule.Initialise();

        if (State.memoryViewer != null)
            State.memoryViewer.RefreshMemory();
    }

    private void initNewPsp() {
        moduleLoaded = false;

        RuntimeContext.reset();
        Profiler.reset();
        getClock().reset();
        getProcessor().reset();
        getScheduler().reset();
        SyscallHandler.reset();
        Memory.getInstance().Initialise();
        Battery.initialize();
        Interrupts.initialize();
        jpcsp.HLE.kernel.types.SceModule.ResetAllocator();

        NIDMapper.getInstance().Initialise();
        Loader.getInstance().reset();
        State.fileLogger.resetLogging();

        HLEModuleManager.getInstance().Initialise(firmwareVersion);
        Managers.reset();
        Modules.SysMemUserForUserModule.Initialise(firmwareVersion);
    }

    @Override
    public void run()
    {
        RuntimeContext.isActive = Settings.getInstance().readBool("emu.compiler");
        Profiler.enableProfiler = Settings.getInstance().readBool("emu.profiler");

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
                Modules.sceGe_userModule.step();
                Modules.ThreadManForUserModule.step();
                scheduler.step();
                Modules.sceDisplayModule.step();
                HLEModuleManager.getInstance().step();
                State.controller.checkControllerState();

                if (State.debugger != null)
                    State.debugger.step();
            }
        }

    }

    public synchronized void RunEmu()
    {
        if (!moduleLoaded) {
            Emulator.log.debug("Nothing loaded, can't run...");
            gui.RefreshButtons();
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

        Modules.sceDisplayModule.setGeDirty(true);
        Modules.ThreadManForUserModule.clearSyscallFreeCycles();

        gui.RefreshButtons();
        if (State.debugger != null)
            State.debugger.RefreshButtons();
    }

    public static synchronized void PauseEmu()
    {
        if (run && !pause)
        {
            pause = true;

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

    public static Scheduler getScheduler() {
    	return scheduler;
    }

    public static Emulator getInstance() {
        return instance;
    }

    public void setInstructionCounter(InstructionCounter instructionCounter) {
        this.instructionCounter = instructionCounter;
        instructionCounter.setModule(module);
    }

    /** @param firmwareVersion : in this format: ABB, where A = major and B = minor, for example 271 */
    public void setFirmwareVersion(int firmwareVersion) {
        this.firmwareVersion = firmwareVersion;

        NIDMapper.getInstance().Initialise();
        HLEModuleManager.getInstance().Initialise(this.firmwareVersion);
        Modules.SysMemUserForUserModule.setFirmwareVersion(this.firmwareVersion);
    }

    /** @param firmwareVersion : in this format: "A.BB", where A = major and B = minor, for example "2.71" */
    public void setFirmwareVersion(String firmwareVersion) {
        setFirmwareVersion(HLEModuleManager.psfFirmwareVersionToInt(firmwareVersion));
    }
}