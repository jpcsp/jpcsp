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
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.CpuState;
import jpcsp.Debugger.DisassemblerModule.DisassemblerFrame;
import jpcsp.Debugger.InstructionCounter;
import jpcsp.Debugger.MemoryViewer;
import jpcsp.HLE.Modules;

public class Emulator implements Runnable {
    private static Processor processor;
    private static Recompiler recompiler;
    private static Controller controller;
    private boolean moduleLoaded;
    private Thread mainThread;
    public static boolean run = false;
    public static boolean pause = false;
    private static MainGUI gui;
    private static DisassemblerFrame debugger;
    private InstructionCounter instructionCounter;
    private static MemoryViewer memview;
    private String pspfilename;
    public static Logger log = Logger.getLogger("misc");

    public Emulator(MainGUI gui) {
        Emulator.gui = gui;
        processor = new Processor();

        if (Settings.getInstance().readBool("emu.recompiler"))
            recompiler = new Recompiler();
        else
            recompiler = null;

        controller = new Controller();
        moduleLoaded = false;
        mainThread = new Thread(this, "Emu");
    }

    private ModuleContext module;

    public void load(String pspfilename, ByteBuffer f) throws IOException, GeneralJpcspException {
        this.pspfilename = pspfilename;

        initNewPsp();

        module = jpcsp.Loader.getInstance().LoadModule(pspfilename, f, 0x08800000);

        if (module.fileFormat == Loader.FORMAT_UNKNOWN ||
            (module.fileFormat & Loader.FORMAT_PSP) == Loader.FORMAT_PSP) {
            throw new GeneralJpcspException("File format not supported!");
        }

        moduleLoaded = true;
        initCpu();

        // Delete breakpoints and reset to PC
        if (debugger != null) {
            debugger.resetDebugger();
        }

        // Update instruction counter dialog with the new app
        if (instructionCounter != null) {
            instructionCounter.setModule(module);
        }
    }

    private void initCpu() {
        //set the default values for registers not sure if they are correct and UNTESTED!!
        //some settings from soywiz/pspemulator
        CpuState cpu = processor.cpu;

        cpu.pc = module.entryAddress; //set the pc register.
        cpu.npc = cpu.pc + 4;
        // Gets set in ThreadMan cpu.gpr[4] = 0; //a0
        // Gets set in ThreadMan cpu.gpr[5] = (int) romManager.getBaseoffset() + (int) elf.getHeader().getE_entry(); // argumentsPointer a1 reg
        //cpu.gpr[6] = 0; //a2
        // Gets set in ThreadMan cpu.gpr[26] = 0x09F00000; //k0
        cpu.gpr[27] = 0; //k1 should probably be 0
        cpu.gpr[28] = (int)module.moduleInfo.getM_gp(); //gp reg    gp register should get the GlobalPointer!!!
        // Gets set in ThreadMan cpu.gpr[29] = 0x09F00000; //sp
        // Gets set in ThreadMan cpu.gpr[31] = 0x08000004; //ra, should this be 0?
        // All other registers are uninitialised/random values

        jpcsp.HLE.ThreadMan.getInstance().Initialise(cpu.pc, module.moduleInfo.getM_attr(), pspfilename);
        jpcsp.HLE.kernel.Managers.fpl.initialize();
        jpcsp.HLE.psputils.getInstance().Initialise();
        jpcsp.HLE.pspge.getInstance().Initialise();
        jpcsp.HLE.pspdisplay.getInstance().Initialise();
        jpcsp.HLE.pspiofilemgr.getInstance().Initialise();

        if (memview != null)
            memview.RefreshMemory();
    }

    private void initNewPsp() {
        moduleLoaded = false;

        getProcessor().reset();
        Memory.getInstance().Initialise();

        NIDMapper.getInstance().Initialise();
        Loader.getInstance().Initialise();

        jpcsp.HLE.modules.HLEModuleManager.getInstance().Initialise();
        jpcsp.HLE.pspSysMem.getInstance().Initialise();
    }

    @Override
    public void run()
    {
        while (true) {
            try {
             synchronized(this) {
                    while (pause)
                        wait();
                }
            } catch (InterruptedException e){
            }

            if (recompiler != null) {
                recompiler.run();
            } else {
                processor.step();
                jpcsp.HLE.pspge.getInstance().step();
                jpcsp.HLE.ThreadMan.getInstance().step();
                jpcsp.HLE.pspdisplay.getInstance().step();
                jpcsp.HLE.modules.HLEModuleManager.getInstance().step();
                controller.checkControllerState();

                if (debugger != null)
                    debugger.step();
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
            notify();
        }
        else if (!run)
        {
            run = true;
            mainThread.start();
        }

        jpcsp.HLE.ThreadMan.getInstance().clearSyscallFreeCycles();

        gui.RefreshButtons();
        if (debugger != null)
            debugger.RefreshButtons();
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

                if (debugger != null) {
                    debugger.RefreshButtons();
                    debugger.RefreshDebugger(true);
                }

                if (memview != null)
                    memview.RefreshMemory();
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

                if (debugger != null) {
                    debugger.RefreshButtons();
                    debugger.RefreshDebugger(true);
                }

                if (memview != null)
                    memview.RefreshMemory();
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

    public static Controller getController() {
        return controller;
    }

    public void setDebugger(DisassemblerFrame debugger) {
        Emulator.debugger = debugger;
    }

    // This is so bad... for GPIO
    public static DisassemblerFrame getDebugger() {
        return debugger;
    }

    public void setInstructionCounter(InstructionCounter instructionCounter) {
        this.instructionCounter = instructionCounter;
        instructionCounter.setModule(module);
    }

    public void setMemoryViewer(MemoryViewer memview) {
        this.memview = memview;
    }
}
