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
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
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
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Utilities;

/*
 * TODO list:
 * 1. Implement relocation type 0x700000A1 in relocateFromHeaders:
 *      -> Info: http://forums.ps2dev.org/viewtopic.php?p=80416#80416
 *
 * 2. Save debugger info for all loaded modules in LoadELFDebuggerInfo.
 */

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

        int currentOffset = f.position();
        module.fileFormat = FORMAT_UNKNOWN;
        module.pspfilename = pspfilename;

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
            } else if (!loadedFirstModule) {
                loadPSF(module);
            }

            if (module.psf != null) {
                Emulator.log.info("PBP meta data :\n" + module.psf);

                if (!loadedFirstModule) {
                    // Set firmware version from PSF embedded in PBP
                    Emulator.getInstance().setFirmwareVersion(module.psf.getString("PSP_SYSTEM_VER"));
                    Modules.SysMemUserForUserModule.setMemory64MB(module.psf.getNumeric("MEMSIZE") == 1);
                }
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
            // Load unpacked PSF in the same directory
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
                    module.psf = new PSF();
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
            return true;
        }
		// Not a valid PBP
		return false;
    }

    /** @return true on success */
    private boolean LoadSCE(ByteBuffer f, SceModule module, int baseAddress) throws IOException {
        long magic = Utilities.readUWord(f);
        if (magic == 0x4543537EL) {
            module.fileFormat |= FORMAT_SCE;
            Emulator.log.warn("Encrypted file not supported! (~SCE)");
            return true;
        }
		// Not a valid PSP
		return false;
    }

    /** @return true on success */
    private boolean LoadPSP(ByteBuffer f, SceModule module, int baseAddress) throws IOException {
        PSP psp = new PSP(f);
        if (psp.isValid()) {
            LoadELF(psp.decrypt(f), module, baseAddress);
            module.fileFormat |= FORMAT_PSP;
            Emulator.log.warn("Encrypted file detected! (~PSP)");
            if(Emulator.getInstance().getFirmwareVersion() >= 280) {
                Emulator.log.info("Calling crypto engine for PRX version 2.");
            }
            return true;
        }
		// Not a valid PSP
		return false;
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
                Emulator.log.debug("Loader: Relocation required (PRX)");
                module.fileFormat |= FORMAT_PRX;
            } else if (elf.getHeader().requiresRelocation()) {
                // Seen in .elf's generated by pspsdk with BUILD_PRX=1 before conversion to .prx
                Emulator.log.info("Loader: Relocation required (ELF)");
            } else {
                // After the user chooses a game to run and we load it, then
                // we can't load another PBP at the same time. We can only load
                // relocatable modules (PRX's) after the user loaded app.
                if (baseAddress > 0x08900000)
                    Emulator.log.warn("Loader: Probably trying to load PBP ELF while another PBP ELF is already loaded");

                baseAddress = 0;
            }

            module.baseAddress = baseAddress;
            if (elf.getHeader().getE_entry() == 0xFFFFFFFFL) {
                module.entry_addr = -1;
            } else {
                module.entry_addr = baseAddress + (int)elf.getHeader().getE_entry();
            }

            // Note: baseAddress is 0 unless we are loading a PRX
            module.loadAddressLow = (baseAddress != 0) ? (int)baseAddress : 0x08900000;
            module.loadAddressHigh = baseAddress;

            // Load into mem
            LoadELFProgram(f, module, baseAddress, elf, elfOffset);
            LoadELFSections(f, module, baseAddress, elf, elfOffset);

            // Relocate PRX
            if (elf.getHeader().requiresRelocation()) {
                relocateFromHeaders(f, module, baseAddress, elf, elfOffset);
            }

            // The following can only be done after relocation
            // Load .rodata.sceModuleInfo
            LoadELFModuleInfo(f, module, baseAddress, elf, elfOffset);
            // After LoadELFModuleInfo so the we can name the memory allocation after the module name
            LoadELFReserveMemory(module);
            // Save imports
            LoadELFImports(module);
            // Save exports
            LoadELFExports(module);
            // Try to fixup imports for ALL modules
            Managers.modules.addModule(module);
            ProcessUnresolvedImports();

            // Debug
            LoadELFDebuggerInfo(f, module, baseAddress, elf, elfOffset);

            // Flush module struct to psp mem
            module.write(Memory.getInstance(), module.address);

            loadedFirstModule = true;
            return true;
        }
		// Not a valid ELF
		Emulator.log.debug("Loader: Not a ELF");
		return false;
    }

    /** Dummy loader for unrecognized file formats, put at the end of a loader chain.
     * @return true on success */
    private boolean LoadUNK(ByteBuffer f, SceModule module, int baseAddress) throws IOException {

        byte m0 = f.get();
        byte m1 = f.get();
        byte m2 = f.get();
        byte m3 = f.get();

        // Catch common user errors
        if (m0 == 0x43 && m1 == 0x49 && m2 == 0x53 && m3 == 0x4F) { // CSO
            Emulator.log.info("This is not an executable file!");
            Emulator.log.info("Try using the Load UMD menu item");
        } else if ((m0 == 0 && m1 == 0x50 && m2 == 0x53 && m3 == 0x46)) { // PSF
            Emulator.log.info("This is not an executable file!");
        } else {
            boolean handled = false;

            // check for ISO
            if (f.limit() >= 16 * 2048 + 6) {
                f.position(16 * 2048);
                byte[] id = new byte[6];
                f.get(id);
                if((((char)id[1])=='C')&&
                   (((char)id[2])=='D')&&
                   (((char)id[3])=='0')&&
                   (((char)id[4])=='0')&&
                   (((char)id[5])=='1'))
                {
                    Emulator.log.info("This is not an executable file!");
                    Emulator.log.info("Try using the Load UMD menu item");
                    handled = true;
                }
            }

            if (!handled) {
                Emulator.log.info("Unrecognized file format");
                Emulator.log.info(String.format("File magic %02X %02X %02X %02X", m0, m1, m2, m3));
            }
        }

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
                    Memory.log.trace(String.format("PH#%d: new loadAddressHigh %08X", i, module.loadAddressHigh));
                }

                Memory.log.trace(String.format("PH#%d: contributes %08X to bss size", i, (int)(phdr.getP_memsz() - phdr.getP_filesz())));
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

        for (Elf32SectionHeader shdr : sectionHeaderList) {
            if ((shdr.getSh_flags() & Elf32SectionHeader.SHF_ALLOCATE) == Elf32SectionHeader.SHF_ALLOCATE) {
                switch (shdr.getSh_type()) {
                    case Elf32SectionHeader.SHT_PROGBITS: // 1
                    {
                        // Load this section into memory
                        // now loaded using program header type 1
                        int memOffset = baseAddress + (int)shdr.getSh_addr();
                        int len = (int)shdr.getSh_size();

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
                            Memory.log.debug(String.format("%s: clearing section %08X - %08X (len %08X)", shdr.getSh_namez(), memOffset, (memOffset + len), len));

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
                        } else {
                            Memory.log.warn(String.format("Type 8 section outside valid range %08X - %08X", memOffset, (memOffset + len)));
                        }
                        break;
                    }
                }
            }
        }

        // Save the address/size of some sections for SceModule
        Elf32SectionHeader shdr = elf.getSectionHeader(".text");
        if (shdr != null) {
            Emulator.log.trace(String.format("SH: Storing text size %08X %d", shdr.getSh_size(), shdr.getSh_size()));
            module.text_addr = (int)(baseAddress + shdr.getSh_addr());
            module.text_size = (int)shdr.getSh_size();
        }

        shdr = elf.getSectionHeader(".data");
        if (shdr != null) {
            Emulator.log.trace(String.format("SH: Storing data size %08X %d", shdr.getSh_size(), shdr.getSh_size()));
            module.data_size = (int)shdr.getSh_size();
        }

        shdr = elf.getSectionHeader(".bss");
        if (shdr != null && shdr.getSh_size() != 0) {
            Emulator.log.trace(String.format("SH: Storing bss size %08X %d", shdr.getSh_size(), shdr.getSh_size()));

            if (module.bss_size == (int)shdr.getSh_size()) {
                Emulator.log.trace("SH: Same bss size already set");
            } else if (module.bss_size > (int)shdr.getSh_size()) {
                Emulator.log.trace(String.format("SH: Larger bss size already set (%08X > %08X)", module.bss_size, shdr.getSh_size()));
            } else if (module.bss_size != 0) {
                Emulator.log.warn(String.format("SH: Overwriting bss size %08X with %08X", module.bss_size, shdr.getSh_size()));
                module.bss_size = (int)shdr.getSh_size();
            } else {
                Emulator.log.info("SH: bss size not already set");
                module.bss_size = (int)shdr.getSh_size();
            }
        }

        module.nsegment += 1;
        module.segmentaddr[0] = module.loadAddressLow;
        module.segmentsize[0] = module.loadAddressHigh - module.loadAddressLow;
    }

    private void LoadELFReserveMemory(SceModule module) {
        // Mark the area of memory the module loaded into as used
    	if (Modules.log.isDebugEnabled()) {
    		Memory.log.debug(String.format("Reserving 0x%X bytes at 0x%08X for module '%s'", module.loadAddressHigh - module.loadAddressLow, module.loadAddressLow, module.pspfilename));
    	}

        int address = module.loadAddressLow & ~(SysMemUserForUser.defaultSizeAlignment - 1); // Round down to match sysmem allocations
        int size = module.loadAddressHigh - address;

        SysMemInfo info = Modules.SysMemUserForUserModule.malloc(2, module.modname, SysMemUserForUser.PSP_SMEM_Addr, size, address);
        if (info == null || info.addr != address) {
            Memory.log.warn(String.format("Failed to properly reserve memory consumed by module %s at address 0x%08X, size 0x%X: allocated %s", module.modname, address, size, info));
        }
        module.addAllocatedMemory(info);
    }

    /** Loads from memory */
    private void LoadELFModuleInfo(ByteBuffer f, SceModule module, int baseAddress,
        Elf32 elf, int elfOffset) throws IOException {

        Elf32ProgramHeader phdr = elf.getProgramHeader(0);
        Elf32SectionHeader shdr = elf.getSectionHeader(".rodata.sceModuleInfo");

        if (!elf.getHeader().isPRXDetected() && shdr == null) {
            Emulator.log.warn("ELF is not PRX, but has no section headers!");
            int memOffset = (int)(phdr.getP_vaddr() + (phdr.getP_paddr() & 0x7FFFFFFFL) - phdr.getP_offset());
            Emulator.log.warn("Manually locating ModuleInfo at address: 0x" + Integer.toHexString(memOffset));

            PSPModuleInfo moduleInfo = new PSPModuleInfo();
            moduleInfo.read(Memory.getInstance(), memOffset);
            module.copy(moduleInfo);
        } else if (elf.getHeader().isPRXDetected()) {
            int memOffset = (int)(baseAddress + (phdr.getP_paddr() & 0x7FFFFFFFL) - phdr.getP_offset());

            PSPModuleInfo moduleInfo = new PSPModuleInfo();
            moduleInfo.read(Memory.getInstance(), memOffset);
            module.copy(moduleInfo);
        } else if (shdr != null) {
            int memOffset = (int)(baseAddress + shdr.getSh_addr());

            PSPModuleInfo moduleInfo = new PSPModuleInfo();
            moduleInfo.read(Memory.getInstance(), memOffset);
            module.copy(moduleInfo);
        } else {
            Emulator.log.error("ModuleInfo not found!");
            return;
        }

        Emulator.log.info("Found ModuleInfo name:'" + module.modname
            + "' version:" + String.format("%02x%02x", module.version[1], module.version[0])
            + " attr:" + String.format("%08x", module.attribute)
            + " gp:" + String.format("%08x", module.gp_value));

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

    	Elf32Relocate rel = new Elf32Relocate();
        int AHL = 0; // (AHI << 16) | (ALO & 0xFFFF)
        List<Integer> deferredHi16 = new LinkedList<Integer>(); // We'll use this to relocate R_MIPS_HI16 when we get a R_MIPS_LO16

        Memory mem = Memory.getInstance();
        for (int i = 0; i < RelCount; i++) {
            rel.read(f);

            int R_TYPE    = (int)( rel.getR_info()        & 0xFF);
            int OFS_BASE  = (int)((rel.getR_info() >>  8) & 0xFF);
            int ADDR_BASE = (int)((rel.getR_info() >> 16) & 0xFF);
            if (Memory.log.isTraceEnabled()) {
            	Memory.log.trace(String.format("Relocation #%d type=%d,base=%08X,addr=%08X", i, R_TYPE, OFS_BASE, ADDR_BASE));
            }

            int phOffset     = (int)elf.getProgramHeader(OFS_BASE).getP_vaddr();
            int phBaseOffset = (int)elf.getProgramHeader(ADDR_BASE).getP_vaddr();

            // Address of data to relocate
            int data_addr = (int)(baseAddress + rel.getR_offset() + phOffset);
            // Value of data to relocate
            int data = mem.read32(data_addr);
            long result = 0; // Used to hold the result of relocation, OR this back into data

            int word32 = data & 0xFFFFFFFF; // <=> data;
            int targ26 = data & 0x03FFFFFF;
            int hi16 = data & 0x0000FFFF;
            int lo16 = data & 0x0000FFFF;
            int rel16 = data & 0x0000FFFF;

            int A = 0; // addend
            int S = (int) baseAddress + phBaseOffset;
            int GP_ADDR = (int) baseAddress + (int) rel.getR_offset();
            int GP_OFFSET = GP_ADDR - ((int) baseAddress & 0xFFFF0000);

            switch (R_TYPE) {
                case 0: //R_MIPS_NONE
                    // Tested on PSP:
                    // R_MIPS_NONE just returns 0.
                    if (Memory.log.isTraceEnabled()) {
                		Memory.log.trace(String.format("R_MIPS_NONE addr=%08X", data_addr));
                	}
                    break;

                case 1: // R_MIPS_16
                	data = (data & 0xFFFF0000) | ((data + S) & 0x0000FFFF);
                	if (Memory.log.isTraceEnabled()) {
                		Memory.log.trace(String.format("R_MIPS_16 addr=%08X before=%08X after=%08X", data_addr, word32, data));
                    }
                	break;

                case 2: //R_MIPS_32
                    data += S;
                	if (Memory.log.isTraceEnabled()) {
                		Memory.log.trace(String.format("R_MIPS_32 addr=%08X before=%08X after=%08X", data_addr, word32, data));
                    }
                    break;

                case 4: //R_MIPS_26
                    A = targ26;
                    result = ((A << 2) + S) >> 2;
                    data &= ~0x03FFFFFF;
                    data |= (int) (result & 0x03FFFFFF); // truncate
                	if (Memory.log.isTraceEnabled()) {
                		Memory.log.trace(String.format("R_MIPS_26 addr=%08X before=%08X after=%08X", data_addr, word32, data));
                    }
                    break;

                case 5: //R_MIPS_HI16
                    A = hi16;
                    AHL = A << 16;
                    deferredHi16.add(data_addr);
                	if (Memory.log.isTraceEnabled()) {
                		Memory.log.trace(String.format("R_MIPS_HI16 addr=%08X", data_addr));
                	}
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
                        int data2 = mem.read32(data_addr2);
                        result = ((data2 & 0x0000FFFF) << 16) + A + S;
                        // The low order 16 bits are always treated as a signed
                        // value. Therefore, a negative value in the low order bits
                        // requires an adjustment in the high order bits. We need
                        // to make this adjustment in two ways: once for the bits we
                        // took from the data, and once for the bits we are putting
                        // back in to the data.
                        if ((A & 0x8000) != 0) {
                            result -= 0x10000;
                        }
                        if ((result & 0x8000) != 0) {
                             result += 0x10000;
                        }
                        data2 &= ~0x0000FFFF;
                        data2 |= (result >> 16) & 0x0000FFFF; // truncate
                    	if (Memory.log.isTraceEnabled()) {
                    		Memory.log.trace(String.format("R_MIPS_HILO16 addr=%08X before=%08X after=%08X", data_addr2, mem.read32(data_addr2), data2));
                        }
                        mem.write32(data_addr2, data2);
                        it.remove();
                    }
                	if (Memory.log.isTraceEnabled()) {
                		Memory.log.trace(String.format("R_MIPS_LO16 addr=%08X before=%08X after=%08X", data_addr, word32, data));
                	}
                    break;

                case 7: // R_MIPS_GPREL16
                    A = rel16;
                    if (A == 0) {
                        result = S - GP_ADDR;
                    } else {
                        result = S + GP_OFFSET + (((A & 0x00008000) != 0) ? A | 0xFFFF0000 : A) - GP_ADDR;
                    }
                    if ((result > 32768) || (result < -32768)) {
                        Memory.log.warn("Relocation overflow (R_MIPS_GPREL16)");
                    }
                    data &= ~0x0000FFFF;
                    data |= (int) (result & 0x0000FFFF);
                    if (Memory.log.isTraceEnabled()) {
                		Memory.log.trace(String.format("R_MIPS_GPREL16 addr=%08X before=%08X after=%08X", data_addr, word32, data));
                    }
                    break;

                default:
                	Memory.log.warn(String.format("Unhandled relocation type %d at %08X", R_TYPE, data_addr));
                    break;
            }

            mem.write32(data_addr, data);
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
                return;
            } else if (phdr.getP_type() == 0x700000A1L) {
                Memory.log.warn("Unimplemented:PH#" + i + ": relocate type 0x700000A1");
            }
            i++;
        }

        // Relocate from section headers
        for (Elf32SectionHeader shdr : elf.getSectionHeaderList()) {
            if (shdr.getSh_type() == Elf32SectionHeader.SHT_REL) {
                Memory.log.warn(shdr.getSh_namez() + ": not relocating section");
            }

            if (shdr.getSh_type() == Elf32SectionHeader.SHT_PRXREL) {
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
                else if (nid == 0) {
                    Emulator.log.warn(String.format("Ignoring import at 0x%08X [0x%08X] (attempt %d)",
                        importAddress, nid, module.importFixupAttempts));

                    it.remove();
                    mem.write32(importAddress + 4, 0); // write a nop over our "unmapped import detection special syscall"
                } else {
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

                        if (loadedFirstModule) {
                            Emulator.log.debug(String.format("Mapped import at 0x%08X to syscall 0x%05X [0x%08X] (attempt %d)",
                                importAddress, code, nid, module.importFixupAttempts));
                        }
                    } else {
                        Emulator.log.warn(String.format("Failed to map import at 0x%08X [0x%08X] Module '%s'(attempt %d)",
                            importAddress, nid, moduleName, module.importFixupAttempts));
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
    private void LoadELFImports(SceModule module) throws IOException {
        Memory mem = Memory.getInstance();
        int stubHeadersAddress = module.stub_top;
        int stubHeadersCount = module.stub_size / Elf32StubHeader.sizeof();

        // n modules to import, 1 stub header per module to import.
        String moduleName;
        for (int i = 0; i < stubHeadersCount; i++) {
            Elf32StubHeader stubHeader = new Elf32StubHeader(mem, stubHeadersAddress);

            // Skip 0 sized entries.
            if (stubHeader.getSize() <= 0) {
                Emulator.log.warn("Skipping dummy entry with size " + stubHeader.getSize());
                stubHeadersAddress += Elf32StubHeader.sizeof() / 2;
            } else {
                if (Memory.isAddressGood((int)stubHeader.getOffsetModuleName())) {
                    moduleName = Utilities.readStringNZ((int) stubHeader.getOffsetModuleName(), 64);
                } else {
                    // Generate a module name.
                    moduleName = module.modname;
                }
                stubHeader.setModuleNamez(moduleName);

                if (stubHeader.getSize() > 5) {
                    stubHeadersAddress += stubHeader.getSize() * 4;
                    Emulator.log.warn("'" + stubHeader.getModuleNamez() + "' has size " + stubHeader.getSize());
                } else {
                    stubHeadersAddress += Elf32StubHeader.sizeof();
                }

                // n stubs per module to import
                for (int j = 0; j < stubHeader.getImports(); j++) {
                    int nid = mem.read32((int) (stubHeader.getOffsetNid() + j * 4));
                    int importAddress = (int) (stubHeader.getOffsetText() + j * 8);
                    DeferredStub deferredStub = new DeferredStub(stubHeader.getModuleNamez(), importAddress, nid);
                    module.unresolvedImports.add(deferredStub);

                    // Add a 0xfffff syscall so we can detect if an unresolved import is called
                    int instruction = // syscall <code>
                            ((jpcsp.AllegrexOpcodes.SPECIAL & 0x3f) << 26) | (jpcsp.AllegrexOpcodes.SYSCALL & 0x3f) | ((0xfffff & 0x000fffff) << 6);

                    mem.write32(importAddress + 4, instruction);
                }
            }
        }

        Emulator.log.info("Found " + module.unresolvedImports.size() + " imports from " + stubHeadersCount + " modules");
    }

    /* Loads from memory */
    private void LoadELFExports(SceModule module) throws IOException {
        NIDMapper nidMapper = NIDMapper.getInstance();
        Memory mem = Memory.getInstance();
        int entHeadersAddress = module.ent_top;
        int entHeadersCount = module.ent_size / Elf32EntHeader.sizeof();
        int entCount = 0;

        // n modules to export, 1 ent header per module to export.
        String moduleName;
        for (int i = 0; i < entHeadersCount; i++) {
            Elf32EntHeader entHeader = new Elf32EntHeader(mem, entHeadersAddress);

            // Skip 0 sized entries.
            if ((entHeader.getSize() <= 0)) {
                Emulator.log.warn("Skipping dummy entry with size " + entHeader.getSize());
                entHeadersAddress += Elf32EntHeader.sizeof() / 2;
            } else {
                if (Memory.isAddressGood((int)entHeader.getOffsetModuleName())) {
                    moduleName = Utilities.readStringNZ((int) entHeader.getOffsetModuleName(), 64);
                } else {
                    // Generate a module name.
                    moduleName = module.modname;
                }
                entHeader.setModuleNamez(moduleName);

                if (entHeader.getSize() > 4) {
                    entHeadersAddress += entHeader.getSize() * 4;
                    Emulator.log.warn("'" + entHeader.getModuleNamez() + "' has size " + entHeader.getSize());
                } else {
                    entHeadersAddress += Elf32EntHeader.sizeof();
                }

                // The export section is organized as as sequence of:
                // - 32-bit NID * functionCount
                // - 32-bit NID * variableCount
                // - 32-bit export address * functionCount
                // - 32-bit variable address * variableCount
                //   (each variable address references another structure, depending on its NID)
                //
                int functionCount = entHeader.getFunctionCount();
                int variableCount = entHeader.getVariableCount();
                int nidAddr = (int) entHeader.getOffsetResident();
                IMemoryReader nidReader = MemoryReader.getMemoryReader(nidAddr, 4);
                int exportAddr = nidAddr + (functionCount + variableCount) * 4;
                IMemoryReader exportReader = MemoryReader.getMemoryReader(exportAddr, 4);
                for (int j = 0; j < functionCount; j++) {
                    int nid = nidReader.readNext();
                    int exportAddress = exportReader.readNext();

                    switch (nid) {
                        case 0xD632ACDB: // module_start
                            module.module_start_func = exportAddress;
                            if (Emulator.log.isDebugEnabled()) {
                                Emulator.log.debug(String.format("module_start found: nid=0x%08X, function=0x%08X", nid, exportAddress));
                            }
                            break;
                        case 0xCEE8593C: // module_stop
                            module.module_stop_func = exportAddress;
                            if (Emulator.log.isDebugEnabled()) {
                                Emulator.log.debug(String.format("module_stop found: nid=0x%08X, function=0x%08X", nid, exportAddress));
                            }
                            break;
                        case 0x2F064FA6: // module_reboot_before
                            module.module_reboot_before_func = exportAddress;
                            if (Emulator.log.isDebugEnabled()) {
                                Emulator.log.debug(String.format("module_reboot_before found: nid=0x%08X, function=0x%08X", nid, exportAddress));
                            }
                            break;
                        case 0xADF12745: // module_reboot_phase
                            module.module_reboot_phase_func = exportAddress;
                            if (Emulator.log.isDebugEnabled()) {
                                Emulator.log.debug(String.format("module_reboot_phase found: nid=0x%08X, function=0x%08X", nid, exportAddress));
                            }
                            break;
                        case 0xD3744BE0: // module_bootstart
                            module.module_bootstart_func = exportAddress;
                            if (Emulator.log.isDebugEnabled()) {
                                Emulator.log.debug(String.format("module_bootstart found: nid=0x%08X, function=0x%08X", nid, exportAddress));
                            }
                            break;
                        default:
                            // Only accept exports from custom modules (attr != 0x4000) and with valid export addresses.
                            if (Memory.isAddressGood(exportAddress) && ((entHeader.getAttr() & 0x4000) != 0x4000)) {
                                nidMapper.addModuleNid(moduleName, nid, exportAddress);
                                entCount++;
                                if (Emulator.log.isDebugEnabled()) {
                                    Emulator.log.debug(String.format("Export found at 0x%08X [0x%08X]", exportAddress, nid));
                                }
                            }
                            break;
                    }
                }

                int variableTableAddr = exportAddr + functionCount * 4;
                IMemoryReader variableReader = MemoryReader.getMemoryReader(variableTableAddr, 4);
                for (int j = 0; j < variableCount; j++) {
                    int nid = nidReader.readNext();
                    int variableAddr = variableReader.readNext();

                    switch (nid) {
                        case 0xF01D73A7: // module_info
                            // Seems to be ignored by the PSP
                            if (Emulator.log.isDebugEnabled()) {
                                Emulator.log.debug(String.format("module_info found: nid=0x%08X, addr=0x%08X", nid, variableAddr));
                            }
                            break;
                        case 0x0F7C276C: // module_start_thread_parameter
                            module.module_start_thread_priority = mem.read32(variableAddr + 4);
                            module.module_start_thread_stacksize = mem.read32(variableAddr + 8);
                            module.module_start_thread_attr = mem.read32(variableAddr + 12);
                            if (Emulator.log.isDebugEnabled()) {
                                Emulator.log.debug(String.format("module_start_thread_parameter found: nid=0x%08X, priority=%d, stacksize=%d, attr=0x%08X", nid, module.module_start_thread_priority, module.module_start_thread_stacksize, module.module_start_thread_attr));
                            }
                            break;
                        case 0xCF0CC697: // module_stop_thread_parameter
                            module.module_stop_thread_priority = mem.read32(variableAddr + 4);
                            module.module_stop_thread_stacksize = mem.read32(variableAddr + 8);
                            module.module_stop_thread_attr = mem.read32(variableAddr + 12);
                            if (Emulator.log.isDebugEnabled()) {
                                Emulator.log.debug(String.format("module_stop_thread_parameter found: nid=0x%08X, priority=%d, stacksize=%d, attr=0x%08X", nid, module.module_stop_thread_priority, module.module_stop_thread_stacksize, module.module_stop_thread_attr));
                            }
                            break;
                        case 0xF4F4299D: // module_reboot_before_thread_parameter
                            module.module_reboot_before_thread_priority = mem.read32(variableAddr + 4);
                            module.module_reboot_before_thread_stacksize = mem.read32(variableAddr + 8);
                            module.module_reboot_before_thread_attr = mem.read32(variableAddr + 12);
                            if (Emulator.log.isDebugEnabled()) {
                                Emulator.log.debug(String.format("module_reboot_before_thread_parameter found: nid=0x%08X, priority=%d, stacksize=%d, attr=0x%08X", nid, module.module_reboot_before_thread_priority, module.module_reboot_before_thread_stacksize, module.module_reboot_before_thread_attr));
                            }
                            break;
                        case 0x11B97506: // module_sdk_version
                            // Currently ignored
                            int sdk_version = mem.read32(variableAddr);
                            if (Emulator.log.isDebugEnabled()) {
                                Emulator.log.warn(String.format("module_sdk_version found: nid=0x%08X, sdk_version=0x%08X", nid, sdk_version));
                            }
                            break;
                        default:
                            Emulator.log.warn(String.format("Unknown variable entry found: nid=0x%08X, addr=0x%08X", nid, variableAddr));
                            break;
                    }
                }
            }
        }

        if (entCount > 0) {
            Emulator.log.info("Found " + entCount + " exports");
        }
    }

    private void LoadELFDebuggerInfo(ByteBuffer f, SceModule module, int baseAddress,
        Elf32 elf, int elfOffset) throws IOException {
        // Save executable section address/size for the debugger/instruction counter
        Elf32SectionHeader shdr;

        shdr = elf.getSectionHeader(".init");
        if (shdr != null) {
            module.initsection[0] = (int)(baseAddress + shdr.getSh_addr());
            module.initsection[1] = (int)shdr.getSh_size();
        }

        shdr = elf.getSectionHeader(".fini");
        if (shdr != null) {
            module.finisection[0] = (int)(baseAddress + shdr.getSh_addr());
            module.finisection[1] = (int)shdr.getSh_size();
        }

        shdr = elf.getSectionHeader(".sceStub.text");
        if (shdr != null)
        {
            module.stubtextsection[0] = (int)(baseAddress + shdr.getSh_addr());
            module.stubtextsection[1] = (int)shdr.getSh_size();
        }

        if (!loadedFirstModule) {
            ElfHeaderInfo.ElfInfo = elf.getElfInfo();
            ElfHeaderInfo.ProgInfo = elf.getProgInfo();
            ElfHeaderInfo.SectInfo = elf.getSectInfo();
        }
    }
}