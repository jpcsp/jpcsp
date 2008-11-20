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

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Debugger.ElfHeaderInfo;
import jpcsp.HLE.pspSysMem;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.format.DeferredStub;
import jpcsp.format.Elf32;
import jpcsp.format.Elf32EntHeader;
import jpcsp.format.Elf32ProgramHeader;
import jpcsp.format.Elf32Relocate;
import jpcsp.format.Elf32SectionHeader;
import jpcsp.format.Elf32StubHeader;
import jpcsp.format.PBP;
import jpcsp.format.PSF;
import jpcsp.format.PSP;
import jpcsp.format.PSPModuleInfo;
import jpcsp.util.Utilities;

public class Loader {

    private static Loader instance;

    private boolean loadedFirstModule;

    // Format bits
    public final static int FORMAT_UNKNOWN  = 0x00;
    public final static int FORMAT_ELF      = 0x01;
    public final static int FORMAT_PRX      = 0x02;
    public final static int FORMAT_PBP      = 0x04;
    public final static int FORMAT_SCE      = 0x08;
    public final static int FORMAT_PSP      = 0x10;


    public static Loader getInstance() {
        if (instance == null)
            instance = new Loader();
        return instance;
    }

    private Loader() {
    }


    public void reset() {
        loadedFirstModule = false;
    }

    /**
     * @param pspfilename   Example:
     *                      ms0:/PSP/GAME/xxx/EBOOT.PBP
     *                      disc0:/PSP_GAME/SYSDIR/BOOT.BIN
     *                      disc0:/PSP_GAME/SYSDIR/EBOOT.BIN
     *                      xxx:/yyy/zzz.prx
     * @param baseAddress   should be at least 64-byte aligned,
     *                      or how ever much is the default alignment in pspsysmem.
     * @return              Always a SceModule object, you should check the
     *                      fileFormat member against the FORMAT_* bits.
     *                      Example: (fileFormat & FORMAT_ELF) == FORMAT_ELF */
    public SceModule LoadModule(String pspfilename, ByteBuffer f, int baseAddress) throws IOException {
        SceModule module = new SceModule(false);

        // init context
        int currentOffset = f.position();
        module.fileFormat = FORMAT_UNKNOWN;
        module.pspfilename = pspfilename;

        // safety check
        if (f.capacity() - f.position() == 0) {
            Emulator.log.error("LoadModule: no data.");
            return module;
        }

        // chain loaders
        do {
            f.position(currentOffset);
            if (LoadPBP(f, module, baseAddress)) {
                currentOffset = f.position();

                // probably kxploit stub
                if (currentOffset == f.limit())
                    break;
            }

            f.position(currentOffset);
            if (LoadSCE(f, module, baseAddress))
                break;

            f.position(currentOffset);
            if (LoadPSP(f, module, baseAddress))
                break;

            f.position(currentOffset);
            if (LoadELF(f, module, baseAddress))
                break;

            f.position(currentOffset);
            LoadUNK(f, module, baseAddress);
        } while(false);

        loadPSF(module);
        if(module.psf != null)
            Emulator.log.info("PBP meta data :\n" + module.psf);

        return module;
    }

    private void loadPSF(SceModule module) {
        if (module.psf != null)
            return;

        String filetoload = module.pspfilename;
        if (filetoload.startsWith("ms0:"))
            filetoload = filetoload.replace("ms0:", "ms0");

        // PBP doesn't have a PSF included. Check for one in kxploit directories
        File metapbp = null;
        File pbpfile = new File(filetoload);
        if (pbpfile.getParentFile() == null ||
            pbpfile.getParentFile().getParentFile() == null) {
            // probably dynamically loading a prx
            return;
        }

        // %__SCE__kxploit
        File metadir = new File(pbpfile.getParentFile().getParentFile().getPath()
                + File.separatorChar + "%" + pbpfile.getParentFile().getName());
        if (metadir.exists()) {
            File[] eboot = metadir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File arg0) {
                    return arg0.getName().equalsIgnoreCase("eboot.pbp");
                }
            });
            if (eboot.length > 0)
                metapbp = eboot[0];
        }

        // kxploit%
        metadir = new File(pbpfile.getParentFile().getParentFile().getPath()
                + File.separatorChar + pbpfile.getParentFile().getName() + "%");
        if (metadir.exists()) {
            File[] eboot = metadir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File arg0) {
                    return arg0.getName().equalsIgnoreCase("eboot.pbp");
                }
            });
            if (eboot.length > 0)
                metapbp = eboot[0];
        }

        if (metapbp != null) {
            // Load PSF embedded in PBP
            FileChannel roChannel;
            try {
                roChannel = new RandomAccessFile(metapbp, "r").getChannel();
                ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());
                PBP meta = new PBP(readbuffer);
                module.psf = meta.readPSF(readbuffer);
                roChannel.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Load unpacked PSF
            File[] psffile = pbpfile.getParentFile().listFiles(new FileFilter() {
                @Override
                public boolean accept(File arg0) {
                    return arg0.getName().equalsIgnoreCase("param.sfo");
                }
            });
            if (psffile != null && psffile.length > 0) {
                try {
                    FileChannel roChannel = new RandomAccessFile(psffile[0], "r").getChannel();
                    ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());
                    module.psf = new PSF(0);
                    module.psf.read(readbuffer);
                    roChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** @return true on success */
    private boolean LoadPBP(ByteBuffer f, SceModule module, int baseAddress) throws IOException {
        PBP pbp = new PBP(f);
        if (pbp.isValid()) {
            module.fileFormat |= FORMAT_PBP;

            // Dump PSF info
            if (pbp.getOffsetParam() > 0) {
                module.psf = pbp.readPSF(f);
            }

            // Dump unpacked PBP
            if (Settings.getInstance().readBool("emu.pbpunpack")) {
                PBP.unpackPBP(f);
            }

            // Save PBP info for debugger
            ElfHeaderInfo.PbpInfo = pbp.toString();

            // Setup position for chaining loaders
            f.position((int)pbp.getOffsetPspData());
            //Emulator.log.debug("Loader: PBP loaded");
            return true;
        } else {
            // Not a valid PBP
            //Emulator.log.debug("Loader: Not a PBP");
            return false;
        }
    }

    /** @return true on success */
    private boolean LoadSCE(ByteBuffer f, SceModule module, int baseAddress) throws IOException {
        long magic = Utilities.readUWord(f);
        if (magic == 0x4543537EL) {
            module.fileFormat |= FORMAT_SCE;
            Emulator.log.warn("Encrypted file not supported! (~SCE)");
            return true;
        } else {
            // Not a valid PSP
            return false;
        }
    }

    /** @return true on success */
    private boolean LoadPSP(ByteBuffer f, SceModule module, int baseAddress) throws IOException {
        PSP psp = new PSP(f);
        if (psp.isValid()) {
            module.fileFormat |= FORMAT_PSP;
            Emulator.log.warn("Encrypted file not supported! (~PSP)");
            return true;
        } else {
            // Not a valid PSP
            return false;
        }
    }

    /** @return true on success */
    private boolean LoadELF(ByteBuffer f, SceModule module, int baseAddress) throws IOException {
        int elfOffset = f.position();
        Elf32 elf = new Elf32(f);
        if (elf.getHeader().isValid()) {
            module.fileFormat |= FORMAT_ELF;

            if (!elf.getHeader().isMIPSExecutable()) {
                Emulator.log.error("Loader NOT a MIPS executable");
                return false;
            }

            if (elf.getHeader().isPRXDetected()) {
                Emulator.log.info("Loader: Relocation required (PRX)");
                module.fileFormat |= FORMAT_PRX;
            } else if (elf.getHeader().requiresRelocation()) {
                // seen in .elf's generated by pspsdk with BUILD_PRX=1 before conversion to .prx
                Emulator.log.info("Loader: Relocation required (ELF)");
            } else {
                //Emulator.log.debug("Relocation NOT required");

                // After the user chooses a game to run and we load it, then
                // we can't load another PBP at the same time. We can only load
                // relocatable modules (PRX's) after the user loaded app.
                if (baseAddress > 0x08900000)
                    Emulator.log.warn("Loader: Probably trying to load PBP ELF while another PBP ELF is already loaded");

                baseAddress = 0;
            }

            module.baseAddress = baseAddress;
            module.entry_addr = baseAddress + (int)elf.getHeader().getE_entry();

            // Note: baseAddress is 0 unless we are loading a PRX
            module.loadAddressLow = (baseAddress != 0) ? (int)baseAddress : 0x08900000;
            module.loadAddressHigh = baseAddress;

            // Load into mem
            LoadELFProgram(f, module, baseAddress, elf, elfOffset);
            LoadELFSections(f, module, baseAddress, elf, elfOffset);
            LoadELFReserveMemory(module);

            // Relocate PRX
            if (elf.getHeader().requiresRelocation()) {
                relocateFromHeaders(f, module, baseAddress, elf, elfOffset);
            }

            // The following can only be done after relocation
            // Load .rodata.sceModuleInfo
            LoadELFModuleInfo(f, module, baseAddress, elf, elfOffset);

            // Save imports
            LoadELFImports(module, baseAddress, elf);
            // Save exports
            LoadELFExports(module, baseAddress, elf);

            // Try to fixup imports for ALL modules
            Managers.modules.addModule(module);
            ProcessUnresolvedImports();


            // Save some debugger stuff
            LoadELFDebuggerInfo(f, module, baseAddress, elf, elfOffset);

            // Flush module struct to psp mem
            module.write(Memory.getInstance(), module.address);

            loadedFirstModule = true;
            //Emulator.log.debug("Loader: ELF loaded");
            return true;
        } else {
            // Not a valid ELF
            Emulator.log.debug("Loader: Not a ELF");
            return false;
        }
    }

    /** Dummy loader for unrecognized file formats, put at the end of a loader chain.
     * @return true on success */
    private boolean LoadUNK(ByteBuffer f, SceModule module, int baseAddress) throws IOException {
        Emulator.log.info("Unrecognized file format");

        // print some debug info
        byte m0 = f.get();
        byte m1 = f.get();
        byte m2 = f.get();
        byte m3 = f.get();
        Emulator.log.info(String.format("File magic %02X %02X %02X %02X", m0, m1, m2, m3));

        return false;
    }

    // ELF Loader

    /** Load some programs into memory */
    private void LoadELFProgram(ByteBuffer f, SceModule module, int baseAddress,
        Elf32 elf, int elfOffset) throws IOException {

        List<Elf32ProgramHeader> programHeaderList = elf.getProgramHeaderList();
        Memory mem = Memory.getInstance();

        int i = 0;
        module.bss_size = 0;
        for (Elf32ProgramHeader phdr : programHeaderList) {
            if (phdr.getP_type() == 0x00000001L) {
                int fileOffset = (int)phdr.getP_offset();
                int memOffset = baseAddress + (int)phdr.getP_vaddr();
                int fileLen = (int)phdr.getP_filesz();
                int memLen = (int)phdr.getP_memsz();

                Memory.log.debug(String.format("PH#%d: loading program %08X - %08X - %08X", i, memOffset, memOffset + fileLen, memOffset + memLen));

                f.position(elfOffset + fileOffset);
                if (f.position() + fileLen > f.limit()) {
                    int newLen = f.limit() - f.position();
                    Memory.log.warn(String.format("PH#%d: program overflow clamping len %08X to %08X", i, fileLen, newLen));
                    fileLen = newLen;
                }
                mem.copyToMemory(memOffset, f, fileLen);

                // Update memory area consumed by the module
                if (memOffset < module.loadAddressLow) {
                    module.loadAddressLow = memOffset;
                    Memory.log.debug(String.format("PH#%d: new loadAddressLow %08X", i, module.loadAddressLow));
                }
                if (memOffset + memLen > module.loadAddressHigh) {
                    module.loadAddressHigh = memOffset + memLen;
                    Memory.log.debug(String.format("PH#%d: new loadAddressHigh %08X", i, module.loadAddressHigh));
                }

                module.bss_size += (int)(phdr.getP_memsz() - phdr.getP_filesz());
            }
            i++;
        }

        Memory.log.debug(String.format("PH alloc consumption %08X (mem %08X)", (module.loadAddressHigh - module.loadAddressLow), module.bss_size));
    }

    /** Load some sections into memory */
    private void LoadELFSections(ByteBuffer f, SceModule module, int baseAddress,
        Elf32 elf, int elfOffset) throws IOException {

        List<Elf32SectionHeader> sectionHeaderList = elf.getSectionHeaderList();
        Memory mem = Memory.getInstance();

        //boolean noBssInSh = true;
        for (Elf32SectionHeader shdr : sectionHeaderList) {
            if ((shdr.getSh_flags() & Elf32SectionHeader.SHF_ALLOCATE) == Elf32SectionHeader.SHF_ALLOCATE) {
                switch (shdr.getSh_type()) {
                    case Elf32SectionHeader.SHT_PROGBITS: // 1
                    {
                        // Load this section into memory
                        // now loaded using program header type 1
                        //int fileOffset = elfOffset + (int)shdr.getSh_offset();
                        int memOffset = baseAddress + (int)shdr.getSh_addr();
                        int len = (int)shdr.getSh_size();
                        /*
                        Memory.log.debug(String.format("%s: loading section %08X - %08X", shdr.getSh_namez(), memOffset, (memOffset + len)));

                        f.position(fileOffset);
                        Utilities.copyByteBuffertoByteBuffer(f, mainmemory, memOffset - MemoryMap.START_RAM, len);
                        */

                        // Update memory area consumed by the module
                        if ((int)(baseAddress + shdr.getSh_addr()) < module.loadAddressLow) {
                            Memory.log.warn(String.format("%s: section allocates more than program %08X - %08X", shdr.getSh_namez(), memOffset, (memOffset + len)));
                            module.loadAddressLow = (int)(baseAddress + shdr.getSh_addr());
                        }
                        if ((int)(baseAddress + shdr.getSh_addr() + shdr.getSh_size()) > module.loadAddressHigh) {
                            Memory.log.warn(String.format("%s: section allocates more than program %08X - %08X", shdr.getSh_namez(), memOffset, (memOffset + len)));
                            module.loadAddressHigh = (int)(baseAddress + shdr.getSh_addr() + shdr.getSh_size());
                        }
                        break;
                    }

                    case Elf32SectionHeader.SHT_NOBITS: // 8
                    {
                        // Zero out this portion of memory
                        int memOffset = baseAddress + (int)shdr.getSh_addr();
                        int len = (int)shdr.getSh_size();

                        if (len == 0) {
                            Memory.log.debug(String.format("%s: ignoring zero-length type 8 section %08X", shdr.getSh_namez(), memOffset));
                        } else if (memOffset >= MemoryMap.START_RAM && memOffset + len <= MemoryMap.END_RAM) {
                            Memory.log.debug(String.format("%s: clearing section %08X - %08X", shdr.getSh_namez(), memOffset, (memOffset + len)));

                        	mem.memset(memOffset, (byte) 0x0, len);

                            // Update memory area consumed by the module
                            if (memOffset < module.loadAddressLow) {
                                module.loadAddressLow = memOffset;
                                Memory.log.debug(String.format("%s: new loadAddressLow %08X (+%08X)", shdr.getSh_namez(), module.loadAddressLow, len));
                            }
                            if (memOffset + len > module.loadAddressHigh) {
                                module.loadAddressHigh = memOffset + len;
                                Memory.log.debug(String.format("%s: new loadAddressHigh %08X (+%08X)", shdr.getSh_namez(), module.loadAddressHigh, len));
                            }

                            /*
                            if (shdr.getSh_namez().equals(".bss")) {
                                noBssInSh = false;
                            }
                            */
                        } else {
                            Memory.log.warn(String.format("Type 8 section outside valid range %08X - %08X", memOffset, (memOffset + len)));
                        }
                        break;
                    }
                }
            }
        }

        // TODO completely ignore sh .bss and use only ph .bss?
        // If so then maybe we should just be using memsz instead of filesz in LoadELFProgram
        /*
        if (noBssInSh) {
            module.loadAddressHigh += module.bss_size;
            Memory.log.debug(String.format(".bss: new loadAddressHigh %08X (+%08X PH extra)", module.loadAddressHigh, module.bss_size));
        }
        */

        // Save the address/size of some sections for SceModule
        Elf32SectionHeader shdr = elf.getSectionHeader(".text");
        if (shdr != null) {
            module.text_addr = (int)(baseAddress + shdr.getSh_addr());
            module.text_size = (int)shdr.getSh_size();
        }

        shdr = elf.getSectionHeader(".data");
        if (shdr != null)
            module.data_size = (int)shdr.getSh_size();

        shdr = elf.getSectionHeader(".bss");
        if (shdr != null && shdr.getSh_size() != 0)
            module.bss_size = (int)shdr.getSh_size();
    }

    private void LoadELFReserveMemory(SceModule module) {
        // Mark the area of memory the module loaded into as used
        Memory.log.debug("Reserving " + (module.loadAddressHigh - module.loadAddressLow) + " bytes at "
            + String.format("%08x", module.loadAddressLow)
            + " for module '" + module.pspfilename + "'");

        pspSysMem SysMemUserForUserModule = pspSysMem.getInstance();
        int addr = SysMemUserForUserModule.malloc(2, pspSysMem.PSP_SMEM_Addr, module.loadAddressHigh - module.loadAddressLow, module.loadAddressLow);
        if (addr != module.loadAddressLow) {
            Memory.log.warn("Failed to properly reserve memory consumed by module " + module.modname
                + " at address 0x" + Integer.toHexString(module.loadAddressLow)
                + " size " + (module.loadAddressHigh - module.loadAddressLow)
                + " new address 0x" + Integer.toHexString(addr));
        }
        SysMemUserForUserModule.addSysMemInfo(2, module.modname, pspSysMem.PSP_SMEM_Low, module.loadAddressHigh - module.loadAddressLow, module.loadAddressLow);
    }

    /** Loads from memory */
    private void LoadELFModuleInfo(ByteBuffer f, SceModule module, int baseAddress,
        Elf32 elf, int elfOffset) throws IOException {

        Elf32ProgramHeader phdr = elf.getProgramHeader(0);
        Elf32SectionHeader shdr = elf.getSectionHeader(".rodata.sceModuleInfo");

        if (elf.getHeader().isPRXDetected()) {
            //int fileOffset = (int)(elfOffset + (phdr.getP_paddr() & 0x7FFFFFFFL));
            int memOffset = (int)(baseAddress + (phdr.getP_paddr() & 0x7FFFFFFFL) - phdr.getP_offset());
            //Emulator.log.debug(String.format("ModuleInfo file=%08X mem=%08X (PRX)", fileOffset, memOffset));

            PSPModuleInfo moduleInfo = new PSPModuleInfo();
            //f.position(fileOffset);
            //moduleInfo.read(f);
            moduleInfo.read(Memory.getInstance(), memOffset);
            module.copy(moduleInfo);
        } else if (shdr != null) {
            //int fileOffset = (int)(elfOffset + shdr.getSh_offset());
            int memOffset = (int)(baseAddress + shdr.getSh_addr());
            //Emulator.log.debug(String.format("ModuleInfo file=%08X mem=%08X", fileOffset, memOffset));

            PSPModuleInfo moduleInfo = new PSPModuleInfo();
            //f.position(fileOffset);
            //moduleInfo.read(f);
            moduleInfo.read(Memory.getInstance(), memOffset);
            module.copy(moduleInfo);
        } else {
            Emulator.log.error("ModuleInfo not found!");
            return;
        }

        Emulator.log.info("Found ModuleInfo name:'" + module.modname
            + "' version:" + String.format("%02x%02x", module.version[1], module.version[0])
            + " attr:" + String.format("%08x", module.attribute));

        if ((module.attribute & 0x1000) != 0) {
            Emulator.log.warn("Kernel mode module detected");
        }
        if ((module.attribute & 0x0800) != 0) {
            Emulator.log.warn("VSH mode module detected");
        }
    }

    /**
     * @param f        The position of this buffer must be at the start of a
     *                 list of Elf32Rel structs.
     * @param RelCount The number of Elf32Rel structs to read and process.
     */
    private void relocateFromBuffer(ByteBuffer f, SceModule module, int baseAddress,
        Elf32 elf, int RelCount) throws IOException {

        final boolean logRelocations = false;
        //boolean logRelocations = true;

        Elf32Relocate rel = new Elf32Relocate();
        int AHL = 0; // (AHI << 16) | (ALO & 0xFFFF)
        List<Integer> deferredHi16 = new LinkedList<Integer>(); // We'll use this to relocate R_MIPS_HI16 when we get a R_MIPS_LO16

        for (int i = 0; i < RelCount; i++) {
            rel.read(f);

            int R_TYPE    = (int)( rel.getR_info()        & 0xFF);
            int OFS_BASE  = (int)((rel.getR_info() >>  8) & 0xFF);
            int ADDR_BASE = (int)((rel.getR_info() >> 16) & 0xFF);
            //System.out.println("type=" + R_TYPE + ",base=" + OFS_BASE + ",addr=" + ADDR_BASE + "");

            int phOffset     = (int)elf.getProgramHeader(OFS_BASE).getP_vaddr();
            int phBaseOffset = (int)elf.getProgramHeader(ADDR_BASE).getP_vaddr();

            // Address of data to relocate
            int data_addr = (int)(baseAddress + rel.getR_offset() + phOffset);
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

            int S = (int) baseAddress + phBaseOffset;
            int GP = (int) baseAddress + (int) module.gp_value; // final gp value, computed correctly? 31/07/08 only used in R_MIPS_GPREL16 which is untested (fiveofhearts)

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

            //System.out.println("Relocation type " + R_TYPE + " at " + String.format("%08x", (int)baseAddress + (int)rel.r_offset));
            Memory.getInstance().write32(data_addr, data);
        }
    }

    /** Uses info from the elf program headers and elf section headers to
     * relocate a PRX. */
    private void relocateFromHeaders(ByteBuffer f, SceModule module, int baseAddress,
        Elf32 elf, int elfOffset) throws IOException {

        // Relocate from program headers
        int i = 0;
        for (Elf32ProgramHeader phdr : elf.getProgramHeaderList()) {
            if (phdr.getP_type() == 0x700000A0L) {
                int RelCount = (int)phdr.getP_filesz() / Elf32Relocate.sizeof();
                Memory.log.debug("PH#" + i + ": relocating " + RelCount + " entries");

                f.position((int)(elfOffset + phdr.getP_offset()));
                relocateFromBuffer(f, module, baseAddress, elf, RelCount);
                // TODO now skip relocate from section headers?
                return;
            }
            i++;
        }

        // Relocate from section headers
        for (Elf32SectionHeader shdr : elf.getSectionHeaderList()) {
            if (shdr.getSh_type() == Elf32SectionHeader.SHT_REL) {
                Memory.log.warn(shdr.getSh_namez() + ": not relocating section");
            }

            if (shdr.getSh_type() == Elf32SectionHeader.SHT_PRXREL /*|| // 0x700000A0
                shdr.getSh_type() == Elf32SectionHeader.SHT_REL*/) // 0x00000009
            {
                int RelCount = (int)shdr.getSh_size() / Elf32Relocate.sizeof();
                Memory.log.debug(shdr.getSh_namez() + ": relocating " + RelCount + " entries");

                f.position((int)(elfOffset + shdr.getSh_offset()));
                relocateFromBuffer(f, module, baseAddress, elf, RelCount);
            }
        }
    }

    private void ProcessUnresolvedImports() {
        Memory mem = Memory.getInstance();
        NIDMapper nidMapper = NIDMapper.getInstance();
        int numberoffailedNIDS = 0;
        int numberofmappedNIDS = 0;

        for (SceModule module : Managers.modules.values()) {
            module.importFixupAttempts++;
            for (Iterator<DeferredStub> it = module.unresolvedImports.iterator(); it.hasNext(); ) {
                DeferredStub deferredStub = it.next();
                String moduleName = deferredStub.getModuleName();
                int nid           = deferredStub.getNid();
                int importAddress = deferredStub.getImportAddress();
                int exportAddress;

                // Attempt to fixup stub to point to an already loaded module export
                exportAddress = nidMapper.moduleNidToAddress(moduleName, nid);
                if (exportAddress != -1)
                {
                    int instruction = // j <jumpAddress>
                        ((jpcsp.AllegrexOpcodes.J & 0x3f) << 26)
                        | ((exportAddress >>> 2) & 0x03ffffff);

                    mem.write32(importAddress, instruction);
                    mem.write32(importAddress + 4, 0); // write a nop over our "unmapped import detection special syscall"
                    it.remove();
                    numberofmappedNIDS++;

                    Emulator.log.debug(String.format("Mapped import at 0x%08X to export at 0x%08X [0x%08X] (attempt %d)",
                        importAddress, exportAddress, nid, module.importFixupAttempts));
                }

                // Ignore patched nids
                else if (nid == 0)
                {
                    Emulator.log.warn(String.format("Ignoring import at 0x%08X [0x%08X] (attempt %d)",
                        importAddress, nid, module.importFixupAttempts));

                    mem.write32(importAddress + 4, 0); // write a nop over our "unmapped import detection special syscall"
                }

                else
                {
                    // Attempt to fixup stub to known syscalls
                    int code = nidMapper.nidToSyscall(nid);
                    if (code != -1)
                    {
                        // Fixup stub, replacing nop with syscall
                        int instruction = // syscall <code>
                            ((jpcsp.AllegrexOpcodes.SPECIAL & 0x3f) << 26)
                            | (jpcsp.AllegrexOpcodes.SYSCALL & 0x3f)
                            | ((code & 0x000fffff) << 6);

                        mem.write32(importAddress + 4, instruction);
                        it.remove();
                        numberofmappedNIDS++;

                        // Don't spam mappings on the first module (the one the user loads)
                        if (loadedFirstModule) {
                            Emulator.log.debug(String.format("Mapped import at 0x%08X to syscall 0x%05X [0x%08X] (attempt %d)",
                                importAddress, code, nid, module.importFixupAttempts));
                        }
                    }

                    // Save for later
                    else
                    {
                        Emulator.log.warn(String.format("Failed to map import at 0x%08X [0x%08X] (attempt %d)",
                            importAddress, nid, module.importFixupAttempts));
                        numberoffailedNIDS++;
                    }
                }
            }
        }

        Emulator.log.info(numberofmappedNIDS + " NIDS mapped");
        if (numberoffailedNIDS > 0)
            Emulator.log.info(numberoffailedNIDS + " remaining unmapped NIDS");
    }

    /* Loads from memory */
    private void LoadELFImports(SceModule module, int baseAddress, Elf32 elf) throws IOException {

        Memory mem = Memory.getInstance();
        int stubHeadersAddress;
        int stubHeadersCount;

        if (false) {
            // Old: from file, from sections
            Elf32SectionHeader shdr = elf.getSectionHeader(".lib.stub");
            if (shdr == null) {
                Emulator.log.warn("Failed to find .lib.stub section");
                return;
            }
            stubHeadersAddress = (int)(baseAddress + shdr.getSh_addr());
            stubHeadersCount = (int)(shdr.getSh_size() / Elf32StubHeader.sizeof());
            //System.out.println(shdr.getSh_namez() + ":" + stubsCount + " module entries");
        } else {
            // New: from memory, from module info
            stubHeadersAddress = module.stub_top;
            stubHeadersCount = module.stub_size / Elf32StubHeader.sizeof();
        }

        // n modules to import, 1 stub header per module to import
        for (int i = 0; i < stubHeadersCount; i++)
        {
            Elf32StubHeader stubHeader = new Elf32StubHeader(mem, stubHeadersAddress);
            stubHeader.setModuleNamez(Utilities.readStringNZ((int)stubHeader.getOffsetModuleName(), 64));
            stubHeadersAddress += Elf32StubHeader.sizeof(); //stubHeader.s_size * 4;
            //System.out.println(stubHeader.toString());

            // n stubs per module to import
            for (int j = 0; j < stubHeader.getImports(); j++)
            {
                int nid = mem.read32((int)(stubHeader.getOffsetNid() + j * 4));
                int importAddress = (int)(stubHeader.getOffsetText() + j * 8);
                DeferredStub deferredStub = new DeferredStub(stubHeader.getModuleNamez(), importAddress, nid);
                module.unresolvedImports.add(deferredStub);

                // Add a 0xfffff syscall so we can detect if an unresolved import is called
                int instruction = // syscall <code>
                    ((jpcsp.AllegrexOpcodes.SPECIAL & 0x3f) << 26)
                    | (jpcsp.AllegrexOpcodes.SYSCALL & 0x3f)
                    | ((0xfffff & 0x000fffff) << 6);

                mem.write32(importAddress + 4, instruction);
            }
        }

        Emulator.log.info("Found " + module.unresolvedImports.size() + " imports from " + stubHeadersCount + " modules");
    }

    /* Loads from memory */
    private void LoadELFExports(SceModule module, int baseAddress, Elf32 elf) throws IOException {

        NIDMapper nidMapper = NIDMapper.getInstance();
        Memory mem = Memory.getInstance();
        int entHeadersAddress;
        int entHeadersCount;
        int entCount = 0;

        if (false) {
            // Old: from file, from sections
            Elf32SectionHeader shdr = elf.getSectionHeader(".lib.ent");
            if (shdr == null) {
                Emulator.log.warn("Failed to find .lib.ent section");
                return;
            }
            entHeadersAddress = (int)(baseAddress + shdr.getSh_addr());
            entHeadersCount = (int)(shdr.getSh_size() / Elf32EntHeader.sizeof());
            //System.out.println(shdr.getSh_namez() + ":" + stubsCount + " module entries");
        } else {
            // New: from memory, from module info
            entHeadersAddress = module.ent_top;
            entHeadersCount = module.ent_size / Elf32EntHeader.sizeof();
        }

        // n modules to export, 1 ent header per module to import
        String moduleName;
        for (int i = 0; i < entHeadersCount; i++)
        {
            Elf32EntHeader entHeader = new Elf32EntHeader(mem, entHeadersAddress);
            if (entHeader.getOffsetModuleName() != 0) {
                moduleName = Utilities.readStringNZ((int) entHeader.getOffsetModuleName(), 64);
            } else {
                // Generate a module name
                moduleName = module.modname;
            }
            entHeader.setModuleNamez(moduleName);
            entHeadersAddress += Elf32EntHeader.sizeof(); //entHeader.size * 4;
            //System.out.println(entHeader.toString());

            // n ents per module to export
            int functionCount = entHeader.getFunctionCount();
            for (int j = 0; j < functionCount; j++)
            {
                int nid           = mem.read32((int)(entHeader.getOffsetResident() + j * 4));
                int exportAddress = mem.read32((int)(entHeader.getOffsetResident() + (j + functionCount) * 4));

                switch(nid) {
                // magic export nids from yapspd
                case 0xd3744be0: // module_bootstart
                case 0x2f064fa6: // module_reboot_before
                case 0xadf12745: // module_reboot_phase
                case 0xd632acdb: // module_start
                case 0xcee8593c: // module_stop
                case 0xf01d73a7: // module_stop
                case 0x0f7c276c: // ?
                    // Ignore magic exports
                    break;
                default:
                    // Save export
                    nidMapper.addModuleNid(moduleName, nid, exportAddress);
                    break;
                }

                entCount++;
                //Emulator.log.debug(String.format("Export found at 0x%08X [0x%08X]", exportAddress, nid));
            }
        }

        if (entCount > 0)
            Emulator.log.info("Found " + entCount + " exports");
    }

    private void LoadELFDebuggerInfo(ByteBuffer f, SceModule module, int baseAddress,
        Elf32 elf, int elfOffset) throws IOException {

        // Save executable section address/size for the debugger/instruction counter
        Elf32SectionHeader shdr;

        // .text moved to module.text_addr/module.text_size, assigned in LoadELFSections

        shdr = elf.getSectionHeader(".init");
        if (shdr != null)
        {
            module.initsection[0] = (int)(baseAddress + shdr.getSh_addr());
            module.initsection[1] = (int)shdr.getSh_size();
        }

        shdr = elf.getSectionHeader(".fini");
        if (shdr != null)
        {
            module.finisection[0] = (int)(baseAddress + shdr.getSh_addr());
            module.finisection[1] = (int)shdr.getSh_size();
        }

        shdr = elf.getSectionHeader(".sceStub.text");
        if (shdr != null)
        {
            module.stubtextsection[0] = (int)(baseAddress + shdr.getSh_addr());
            module.stubtextsection[1] = (int)shdr.getSh_size();
        }

        // test the instruction counter
        //if (/*shdr.getSh_namez().equals(".text") || */shdr.getSh_namez().equals(".init") /*|| shdr.getSh_namez().equals(".fini")*/) {
        /*
           int sectionAddress = (int)(baseAddress + shdr.getSh_addr());
           System.out.println(Integer.toHexString(sectionAddress) + " size = " + shdr.getSh_size());
           for(int i =0; i< shdr.getSh_size(); i+=4)
           {
             int memread32 = Memory.getInstance().read32(sectionAddress+i);
             //System.out.println(memread32);
             jpcsp.Allegrex.Decoder.instruction(memread32).increaseCount();
           }


        }
        System.out.println(jpcsp.Allegrex.Instructions.ADDIU.getCount());
        */

        // Only do this for the app the user loads and not any prx's loaded
        // later, otherwise the last loaded module overwrites previous saved info.
        // TODO save debugger info for all loaded modules
        if (!loadedFirstModule) {
            // Set ELF info in the debugger
            ElfHeaderInfo.ElfInfo = elf.getElfInfo();
            ElfHeaderInfo.ProgInfo = elf.getProgInfo();
            ElfHeaderInfo.SectInfo = elf.getSectInfo();
        }
    }
}
