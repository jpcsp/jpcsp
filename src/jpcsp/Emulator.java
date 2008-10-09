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
import jpcsp.Debugger.MemoryViewer;
import jpcsp.Debugger.DisassemblerModule.DisassemblerFrame;
import jpcsp.HLE.Modules;
import jpcsp.format.DeferredStub;
import jpcsp.format.Elf32;
import jpcsp.format.Elf32Relocate;
import jpcsp.format.Elf32SectionHeader;
import jpcsp.format.Elf32SectionHeader.ShType;
import jpcsp.format.Elf32StubHeader;
import jpcsp.format.PSF;
import static jpcsp.util.Utilities.*;

public class Emulator implements Runnable {
public static String ElfInfo, ProgInfo, PbpInfo, SectInfo;
    private static Processor processor;
    private static Recompiler recompiler;
    private static Controller controller;
    private FileManager romManager;
    private boolean mediaImplemented = false;
    private Thread mainThread;
    public static boolean run = false;
    public static boolean pause = false;
    private static MainGUI gui;
    private static DisassemblerFrame debugger;
    private static MemoryViewer memview;
    private String pspfilename;
    public static Logger log = Logger.getLogger("misc");

    // For instruction counter
    public static int[] textsection = new int[2];
    public static int[] initsection = new int[2];
    public static int[] finisection = new int[2];
    public static int[] stubtextsection= new int[2];

    public Emulator(MainGUI gui) {
        Emulator.gui = gui;
        processor = new Processor();

        if (Settings.getInstance().readBool("emu.recompiler"))
            recompiler = new Recompiler();
        else
            recompiler = null;

        controller = new Controller();
        mainThread = new Thread(this, "Emu");
    }

    /* unused
    public void load(SeekableDataInput f) throws IOException
    {
        this.pspfilename = "";

        initNewPsp();
        romManager = new FileManager(f);
        initElf32();

        // Delete breakpoints and reset to PC
        if (debugger != null) {
            debugger.resetDebugger();
        }
    }
    */

    public void load(String pspfilename, ByteBuffer f) throws IOException {
        this.pspfilename = pspfilename;

        //  here load fileName, iso or etc...
        processLoading(f);
        if (!mediaImplemented) {
            throw new IOException("This kind of file format still not supported.");
        }

        // Delete breakpoints and reset to PC
        if (debugger != null) {
            debugger.resetDebugger();
        }
    }

    private void processLoading(ByteBuffer f) throws IOException {
        initNewPsp();
        romManager = new FileManager(f);

        switch (romManager.getType()) {
            case FileManager.FORMAT_ELF:
                initElf32();//RAM, CPU, GPU...
                break;
            case FileManager.FORMAT_ISO:
                break;
            case FileManager.FORMAT_PBP:
                initPbp();//RAM, CPU, GPU...
                break;
            case FileManager.FORMAT_UMD:
                break;
            case FileManager.FORMAT_PSP:
                break;
            default:
                throw new IOException("Is not an acceptable format, please choose the rigth file.");
        }
    }

    private void initElf32() throws IOException {
        mediaImplemented = true;
        initRamBy(romManager.getElf32());
        initCpuBy(romManager.getElf32());
    }

    private void initPbp() throws IOException {
        mediaImplemented = true;
        initRamBy(romManager.getPBP().getElf32());
        initCpuBy(romManager.getPBP().getElf32());

        // Set gui title from param.sfo
        PSF psf = romManager.getPBP().getPSF();
        if (psf != null) {
            String title = psf.getString("TITLE");
            gui.setTitle(jpcsp.util.MetaInformation.FULL_NAME + " - " + title);
        }
    }

    private void initRamBy(Elf32 elf) throws IOException {
        // Relocation
        final boolean logRelocations = false;
        //boolean logRelocations = true;

        if (elf.getHeader().requiresRelocation()) {
            for (Elf32SectionHeader shdr : elf.getListSectionHeader()) {
                if (shdr.getSh_type() == ShType.REL.getValue()) {
                    Memory.log.warn(shdr.getSh_namez() + ": not relocating section");
                }

                if (shdr.getSh_type() == ShType.PRXREL.getValue() /*|| // 0x700000A0
                        shdr.getSh_type() == ShType.REL.getValue()*/) // 0x00000009
                {
                    Elf32Relocate rel = new Elf32Relocate();
                    romManager.getActualFile().position((int)(romManager.getElfoffset() + shdr.getSh_offset()));

                    int RelCount = (int) shdr.getSh_size() / Elf32Relocate.sizeof();
                    Memory.log.debug(shdr.getSh_namez() + ": relocating " + RelCount + " entries");

                    int AHL = 0; // (AHI << 16) | (ALO & 0xFFFF)

                    //int HI_addr = 0; // We'll use this to relocate R_MIPS_HI16 when we get a R_MIPS_LO16
                    List<Integer> deferredHi16 = new LinkedList<Integer>();

                    for (int i = 0; i < RelCount; i++) {
                        rel.read(romManager.getActualFile());

                        int R_TYPE = (int) (rel.getR_info() & 0xFF);
                        int OFS_BASE = (int) ((rel.getR_info() >> 8) & 0xFF);
                        int ADDR_BASE = (int) ((rel.getR_info() >> 16) & 0xFF);
                        //System.out.println("type=" + R_TYPE + ",base=" + OFS_BASE + ",addr=" + ADDR_BASE + "");

                        int phOffset = (int)elf.getProgramHeader(OFS_BASE).getP_vaddr();
                        int phBaseOffset = (int)elf.getProgramHeader(ADDR_BASE).getP_vaddr();

                        // Address of data to relocate
                        int data_addr = (int)(romManager.getBaseoffset() + rel.getR_offset() + phOffset);
                        // Value of data to relocate
                        int data = Memory.getInstance().read32(data_addr);
                        long result = 0; // Used to hold the result of relocation, OR this back into data

                        // these are the addends?
                        // SysV ABI MIPS quote: "Because MIPS uses only Elf32_Rel re-location entries, the relocated field holds the addend."
                        int half16 = data & 0x0000FFFF; // 31/07/08 unused (fiveofhearts)

                        int word32 = data & 0xFFFFFFFF; // <=> data;
                        int targ26 = data & 0x03FFFFFF;
                        int hi16 = data & 0x0000FFFF;
                        int lo16 = data & 0x0000FFFF;
                        int rel16 = data & 0x0000FFFF;

                        int A = 0; // addend
                        // moved outside the loop so context is saved
                        //int AHL = 0; // (AHI << 16) | (ALO & 0xFFFF)

                        int S = (int) romManager.getBaseoffset() + phBaseOffset;
                        int GP = (int) romManager.getBaseoffset() + (int) romManager.getPSPModuleInfo().getM_gp(); // final gp value, computed correctly? 31/07/08 only used in R_MIPS_GPREL16 which is untested (fiveofhearts)

                        switch (R_TYPE) {
                            case 0: //R_MIPS_NONE
                                // Don't do anything
                                if (logRelocations)
                                    Memory.log.warn("R_MIPS_NONE addr=" + String.format("%08x", data_addr));
                                break;

                            case 5: //R_MIPS_HI16
                                A = hi16;
                                AHL = A << 16;
                                //HI_addr = data_addr;
                                deferredHi16.add(data_addr);
                                if (logRelocations) Memory.log.debug("R_MIPS_HI16 addr=" + String.format("%08x", data_addr));
                                break;

                            case 6: //R_MIPS_LO16
                                A = lo16;
                                AHL &= ~0x0000FFFF; // delete lower bits, since many R_MIPS_LO16 can follow one R_MIPS_HI16

                                AHL |= A & 0x0000FFFF;

                                result = AHL + S;
                                data &= ~0x0000FFFF;
                                data |= result & 0x0000FFFF; // truncate

                                // Process deferred R_MIPS_HI16
                                for (Iterator<Integer> it = deferredHi16.iterator(); it.hasNext();) {
                                    int data_addr2 = it.next();
                                    int data2 = Memory.getInstance().read32(data_addr2);

                                    result = ((data2 & 0x0000FFFF) << 16) + A + S;
                                    // The low order 16 bits are always treated as a signed
                                    // value. Therefore, a negative value in the low order bits
                                    // requires an adjustment in the high order bits. We need
                                     // to make this adjustment in two ways: once for the bits we
                                    // took from the data, and once for the bits we are putting
                                     // back in to the data.
                                    if ((A & 0x8000) != 0)
                                    {
                                         result -= 0x10000;
                                    }
                                    if ((result & 0x8000) != 0)
                                    {
                                         result += 0x10000;
                                    }
                                    data2 &= ~0x0000FFFF;
                                    data2 |= (result >> 16) & 0x0000FFFF; // truncate


                                    if (logRelocations)  {
                                        Memory.log.debug("R_MIPS_HILO16 addr=" + String.format("%08x", data_addr2)
                                            + " data2 before=" + Integer.toHexString(Memory.getInstance().read32(data_addr2))
                                            + " after=" + Integer.toHexString(data2));
                                    }
                                    Memory.getInstance().write32(data_addr2, data2);
                                    it.remove();
                                }

                                if (logRelocations)  {
                                    Memory.log.debug("R_MIPS_LO16 addr=" + String.format("%08x", data_addr) + " data before=" + Integer.toHexString(word32)
                                        + " after=" + Integer.toHexString(data));
                                }
                                break;

                            case 4: //R_MIPS_26
                                A = targ26;

                                // docs say "sign-extend(A < 2)", but is it meant to be A << 2? if so then there's no point sign extending
                                //result = (sign-extend(A < 2) + S) >> 2;
                                //result = (((A < 2) ? 0xFFFFFFFF : 0x00000000) + S) >> 2;
                                result = ((A << 2) + S) >> 2; // copied from soywiz/pspemulator

                                data &= ~0x03FFFFFF;
                                data |= (int) (result & 0x03FFFFFF); // truncate

                                if (logRelocations) {
                                    Memory.log.debug("R_MIPS_26 addr=" + String.format("%08x", data_addr) + " before=" + Integer.toHexString(word32)
                                        + " after=" + Integer.toHexString(data));
                                }
                                break;

                            case 2: //R_MIPS_32
                                data += S;

                                if (logRelocations) {
                                    Memory.log.debug("R_MIPS_32 addr=" + String.format("%08x", data_addr) + " before=" + Integer.toHexString(word32)
                                        + " after=" + Integer.toHexString(data));
                                }
                                break;

                            /* sample before relocation: 0x00015020: 0x8F828008 '....' - lw         $v0, -32760($gp)
                            case 7: //R_MIPS_GPREL16
                                // 31/07/08 untested (fiveofhearts)
                                Memory.log.warn("Untested relocation type " + R_TYPE + " at " + String.format("%08x", data_addr));

                                A = rel16;

                                //result = sign-extend(A) + S + GP;
                                result = (((A & 0x00008000) != 0) ? A | 0xFFFF0000 : A) + S + GP;

                                // verify
                                if ((result & ~0x0000FFFF) != 0) {
                                    //throw new IOException("Relocation overflow (R_MIPS_GPREL16)");
                                    Memory.log.warn("Relocation overflow (R_MIPS_GPREL16)");
                                }

                                data &= ~0x0000FFFF;
                                data |= (int)(result & 0x0000FFFF);

                                break;
                            /* */

                            default:
                            	Memory.log.warn("Unhandled relocation type " + R_TYPE + " at " + String.format("%08x", data_addr));
                                break;
                        }

                        //System.out.println("Relocation type " + R_TYPE + " at " + String.format("%08x", (int)baseoffset + (int)rel.r_offset));
                        Memory.getInstance().write32(data_addr, data);
                    }
                }
            }
        }

        // Imports
        // ... and code section finder, for instruction counter (should not be here! should go in FileManager.secondStep())
        int numberoffailedNIDS=0;
        int numberofmappedNIDS=0;
        boolean foundStubSection = false;

        textsection[0] = textsection[1] = 0;
        initsection[0] = initsection[1] = 0;
        finisection[0] = finisection[1] = 0;
        stubtextsection[0] = stubtextsection[1] = 0;

        for (Elf32SectionHeader shdr : elf.getListSectionHeader()) {
            if (shdr.getSh_namez().equals(".lib.stub")) {
                Memory mem = Memory.getInstance();
                int stubHeadersAddress = (int)(romManager.getBaseoffset() + shdr.getSh_addr());
                int stubHeadersCount = (int)(shdr.getSh_size() / Elf32StubHeader.sizeof());
                foundStubSection = true;

                Elf32StubHeader stubHeader;
                List<DeferredStub> deferred = new LinkedList<DeferredStub>();
                NIDMapper nidMapper = NIDMapper.get_instance();

                //System.out.println(shdr.getSh_namez() + ":" + stubsCount + " module entries");

                for (int i = 0; i < stubHeadersCount; i++)
                {
                    stubHeader = new Elf32StubHeader(mem, stubHeadersAddress);
                    stubHeader.setModuleNamez(readStringNZ(mem.mainmemory, (int)(stubHeader.getOffsetModuleName() - MemoryMap.START_RAM), 64));
                    stubHeadersAddress += Elf32StubHeader.sizeof(); //stubHeader.s_size * 4;
                    //System.out.println(stubHeader.toString());

                    for (int j = 0; j < stubHeader.getImports(); j++)
                    {
                        int nid = mem.read32((int)(stubHeader.getOffsetNid() + j * 4));
                        int importAddress = (int)(stubHeader.getOffsetText() + j * 8);
                        int exportAddress;
                        int code;

                        // Attempt to fixup stub to point to an already loaded module export
                        exportAddress = nidMapper.moduleNidToAddress(stubHeader.getModuleNamez(), nid);
                        if (exportAddress != -1)
                        {
                            int instruction = // j <jumpAddress>
                                ((jpcsp.AllegrexOpcodes.J & 0x3f) << 26)
                                | ((exportAddress >>> 2) & 0x03ffffff);

                            mem.write32(importAddress, instruction);

                            Modules.log.debug("Mapped NID " + Integer.toHexString(nid) + " to export");
                        }

                        // Attempt to fixup stub to known syscalls
                        else
                        {
                            code = nidMapper.nidToSyscall(nid);
                            if (code != -1)
                            {
                                // Fixup stub, replacing nop with syscall
                                int instruction = // syscall <code>
                                    ((jpcsp.AllegrexOpcodes.SPECIAL & 0x3f) << 26)
                                    | (jpcsp.AllegrexOpcodes.SYSCALL & 0x3f)
                                    | ((code & 0x000fffff) << 6);

                                mem.write32(importAddress + 4, instruction);
                                numberofmappedNIDS++;
                                //System.out.println("Mapped NID " + Integer.toHexString(nid) + " to syscall " + Integer.toHexString(code));
                            }
                            else
                            {
                                // Save nid for deferred fixup
                                deferred.add(new DeferredStub(stubHeader.getModuleNamez(), importAddress, nid));
                                Modules.log.warn("Failed to map NID " + Integer.toHexString(nid) + " (load time)");
                                numberoffailedNIDS++;

                                // Add a 0xfffff syscall so we can detect if an unresolved import is called
                                int instruction = // syscall <code>
                                    ((jpcsp.AllegrexOpcodes.SPECIAL & 0x3f) << 26)
                                    | (jpcsp.AllegrexOpcodes.SYSCALL & 0x3f)
                                    | ((0xfffff & 0x000fffff) << 6);

                                mem.write32(importAddress + 4, instruction);
                            }
                        }
                    }
                }

                romManager.addDeferredImports(deferred);
            }

            //the following are used for the instruction counter panel
            if (shdr.getSh_namez().equals(".text"))
            {
                textsection[0] = (int)(romManager.getBaseoffset() + shdr.getSh_addr());
                textsection[1] = (int)shdr.getSh_size();
            }
            if (shdr.getSh_namez().equals(".init"))
            {
                initsection[0] = (int)(romManager.getBaseoffset() + shdr.getSh_addr());
                initsection[1] = (int)shdr.getSh_size();
            }
            if (shdr.getSh_namez().equals(".fini"))
            {
                finisection[0] = (int)(romManager.getBaseoffset() + shdr.getSh_addr());
                finisection[1] = (int)shdr.getSh_size();
            }
            if (shdr.getSh_namez().equals(".sceStub.text"))
            {
                stubtextsection[0] = (int)(romManager.getBaseoffset() + shdr.getSh_addr());
                stubtextsection[1] = (int)shdr.getSh_size();
            }

            //test the instruction counter
            //if (/*shdr.getSh_namez().equals(".text") || */shdr.getSh_namez().equals(".init") /*|| shdr.getSh_namez().equals(".fini")*/) {
            /*
               int sectionAddress = (int)(romManager.getBaseoffset() + shdr.getSh_addr());
               System.out.println(Integer.toHexString(sectionAddress) + " size = " + shdr.getSh_size());
               for(int i =0; i< shdr.getSh_size(); i+=4)
               {
                 int memread32 = Memory.get_instance().read32(sectionAddress+i);
                 //System.out.println(memread32);
                 jpcsp.Allegrex.Decoder.instruction(memread32).increaseCount();
               }


            }
            System.out.println(jpcsp.Allegrex.Instructions.ADDIU.getCount());*/
        }

        if (!foundStubSection) Modules.log.warn("Failed to find .lib.stub section");
        Modules.log.info(numberofmappedNIDS + " NIDS mapped");
        if (numberoffailedNIDS > 0) Modules.log.warn("Total Failed to map NIDS = " + numberoffailedNIDS);
    }

    private void initCpuBy(Elf32 elf) {
        //set the default values for registers not sure if they are correct and UNTESTED!!
        //some settings from soywiz/pspemulator
        CpuState cpu = processor.cpu;

        cpu.pc = (int)(romManager.getBaseoffset() + elf.getHeader().getE_entry()); //set the pc register.
        cpu.npc = cpu.pc + 4;
        // Gets set in ThreadMan cpu.gpr[4] = 0; //a0
        // Gets set in ThreadMan cpu.gpr[5] = (int) romManager.getBaseoffset() + (int) elf.getHeader().getE_entry(); // argumentsPointer a1 reg
        //cpu.gpr[6] = 0; //a2
        // Gets set in ThreadMan cpu.gpr[26] = 0x09F00000; //k0
        cpu.gpr[27] = 0; //k1 should probably be 0
        cpu.gpr[28] = (int)(romManager.getBaseoffset() + romManager.getPSPModuleInfo().getM_gp()); //gp reg    gp register should get the GlobalPointer!!!
        // Gets set in ThreadMan cpu.gpr[29] = 0x09F00000; //sp
        // Gets set in ThreadMan cpu.gpr[31] = 0x08000004; //ra, should this be 0?
        // All other registers are uninitialised/random values

        jpcsp.HLE.modules.HLEModuleManager.getInstance().Initialise();
        jpcsp.HLE.pspSysMem.get_instance().Initialise(romManager.getLoadAddressLow(), romManager.getLoadAddressHigh() - romManager.getLoadAddressLow());
        jpcsp.HLE.ThreadMan.get_instance().Initialise(cpu.pc, romManager.getPSPModuleInfo().getM_attr(), pspfilename);
        jpcsp.HLE.psputils.get_instance().Initialise();
        jpcsp.HLE.pspge.get_instance().Initialise();
        jpcsp.HLE.pspdisplay.get_instance().Initialise();
        jpcsp.HLE.pspiofilemgr.get_instance().Initialise();

        if (memview != null)
            memview.RefreshMemory();
    }

    private void initNewPsp() {
        getProcessor().reset();
        Memory.getInstance().Initialise();
        NIDMapper.get_instance().Initialise();
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
                jpcsp.HLE.pspge.get_instance().step();
                jpcsp.HLE.ThreadMan.get_instance().step();
                jpcsp.HLE.pspdisplay.get_instance().step();
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
        if (!mediaImplemented)
            return;

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

        jpcsp.HLE.ThreadMan.get_instance().clearSyscallFreeCycles();

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

            gui.RefreshButtons();

            if (debugger != null) {
                debugger.RefreshButtons();
                debugger.RefreshDebugger();
            }

            if (memview != null)
                memview.RefreshMemory();

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
    public static synchronized void PauseEmuWithStatus(int status)
    {
        if (run && !pause)
        {
            pause = true;

            gui.RefreshButtons();

            if (debugger != null) {
                debugger.RefreshButtons();
                debugger.RefreshDebugger();
            }

            if (memview != null)
                memview.RefreshMemory();

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
        this.debugger = debugger;
    }

    public void setMemoryViewer(MemoryViewer memview) {
        this.memview = memview;
    }
}
