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

import static jpcsp.Allegrex.Common._v0;
import static jpcsp.Allegrex.Common._zr;
import static jpcsp.Memory.addressMask;
import static jpcsp.format.Elf32ProgramHeader.PF_X;
import static jpcsp.format.Elf32SectionHeader.SHF_ALLOCATE;
import static jpcsp.format.Elf32SectionHeader.SHF_EXECUTE;
import static jpcsp.format.Elf32SectionHeader.SHF_NONE;
import static jpcsp.format.Elf32SectionHeader.SHF_WRITE;
import static jpcsp.util.HLEUtilities.JR;
import static jpcsp.util.HLEUtilities.MOVE;
import static jpcsp.util.HLEUtilities.NOP;
import static jpcsp.util.HLEUtilities.SYSCALL;
import static jpcsp.util.Utilities.patch;
import static jpcsp.util.Utilities.patchRemoveStringChar;
import static jpcsp.util.Utilities.readUByte;
import static jpcsp.util.Utilities.readUHalf;
import static jpcsp.util.Utilities.readUWord;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.writeInt32;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.Opcodes;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.Debugger.ElfHeaderInfo;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules.scePopsMan;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.format.DeferredStub;
import jpcsp.format.DeferredVStub32;
import jpcsp.format.DeferredVStubHI16;
import jpcsp.format.DeferredVstubLO16;
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
import jpcsp.memory.MemorySection;
import jpcsp.memory.MemorySections;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

public class Loader {
    private static Loader instance;
    private static Logger log = Logger.getLogger("loader");

    public final static int SCE_MAGIC = 0x4543537E;
    public final static int PSP_MAGIC = 0x50535000;
    public final static int EDAT_MAGIC = 0x54414445;
    public final static int FIRMWAREVERSION_HOMEBREW = 999; // Simulate version 9.99 instead of 1.50

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

    /**
     * @param pspfilename   Example:
     *                      ms0:/PSP/GAME/xxx/EBOOT.PBP
     *                      disc0:/PSP_GAME/SYSDIR/BOOT.BIN
     *                      disc0:/PSP_GAME/SYSDIR/EBOOT.BIN
     *                      xxx:/yyy/zzz.prx
     * @param f             the module file contents
     * @param baseAddress   should be at least 64-byte aligned,
     *                      or how ever much is the default alignment in pspsysmem.
     * @param analyzeOnly   true, if the module is not really loaded, but only
     *                            the SceModule object is returned;
     *                      false, if the module is really loaded in memory.
     * @return              Always a SceModule object, you should check the
     *                      fileFormat member against the FORMAT_* bits.
     *                      Example: (fileFormat & FORMAT_ELF) == FORMAT_ELF
     **/
    public SceModule LoadModule(String pspfilename, ByteBuffer f, TPointer baseAddress, int mpidText, int mpidData, boolean analyzeOnly, boolean allocMem, boolean fromSyscall, boolean isSignChecked, byte[] key) throws IOException {
        SceModule module = new SceModule(false);

        int currentOffset = f.position();
        module.fileFormat = FORMAT_UNKNOWN;
        module.pspfilename = pspfilename;
        module.mpidtext = mpidText;
        module.mpiddata = mpidData;

        // The PSP startup code requires a ":" into the argument passed to the root thread.
        // On Linux, there is no ":" in the file name when loading a .pbp file;
        // on Windows, there is luckily one ":" in "C:/...".
        // Simulate a ":" by prefixing by "ms0:", as this is not really used by an application.
        if (module.pspfilename != null && !module.pspfilename.contains(":")) {
        	module.pspfilename = "ms0:" + module.pspfilename;
        }

        if (f.capacity() - f.position() == 0) {
            log.error("LoadModule: no data.");
            return module;
        }

        // chain loaders
        do {
            f.position(currentOffset);
            if (LoadPBP(f, module, baseAddress, analyzeOnly, allocMem, fromSyscall)) {
                currentOffset = f.position();

                // probably kxploit stub
                if (currentOffset == f.limit()) {
                    break;
                }
            } else if (!fromSyscall) {
                loadPSF(module, analyzeOnly, allocMem, fromSyscall);
            }

            if (module.psf != null) {
                log.info(String.format("PBP meta data:%s%s", System.lineSeparator(), module.psf));

                if (!fromSyscall) {
                    // Set firmware version from PSF embedded in PBP
                	if (module.psf.isLikelyHomebrew()) {
                		Emulator.getInstance().setFirmwareVersion(FIRMWAREVERSION_HOMEBREW);
                	} else {
                		Emulator.getInstance().setFirmwareVersion(module.psf.getString("PSP_SYSTEM_VER"));
                	}
                    Modules.SysMemUserForUserModule.setMemory64MB(module.psf.getNumeric("MEMSIZE") == 1);
                }
            }
            
            f.position(currentOffset);
            if (LoadSPRX(f, module, baseAddress, analyzeOnly, allocMem, fromSyscall, isSignChecked, key)) {
                break;
            }

            f.position(currentOffset);
            if (LoadSCE(f, module, baseAddress, analyzeOnly, allocMem, fromSyscall, isSignChecked, key)) {
                break;
            }

            f.position(currentOffset);
            if (LoadPSP(f, module, baseAddress, analyzeOnly, allocMem, fromSyscall, isSignChecked, key)) {
                break;
            }

            f.position(currentOffset);
            if (LoadELF(f, module, baseAddress, analyzeOnly, allocMem, fromSyscall)) {
            	if (!fromSyscall) {
            		Emulator.getInstance().setFirmwareVersion(FIRMWAREVERSION_HOMEBREW);
            	}
                break;
            }

            f.position(currentOffset);
            LoadUNK(f, module, baseAddress, analyzeOnly, allocMem, fromSyscall);
        } while (false);

        if (!analyzeOnly) {
        	patchModule(module);
        }

        if (analyzeOnly) {
        	module.free();
        }

        return module;
    }

    private void loadPSF(SceModule module, boolean analyzeOnly, boolean allocMem, boolean fromSyscall) {
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
            	RandomAccessFile raf = new RandomAccessFile(metapbp, "r");
                roChannel = raf.getChannel();
                ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());
                PBP meta = new PBP(readbuffer);
                module.psf = meta.readPSF(readbuffer);
                raf.close();
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
                	RandomAccessFile raf = new RandomAccessFile(psffile[0], "r");
                    FileChannel roChannel = raf.getChannel();
                    ByteBuffer readbuffer = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());
                    module.psf = new PSF();
                    module.psf.read(readbuffer);
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** @return true on success */
    private boolean LoadPBP(ByteBuffer f, SceModule module, TPointer baseAddress, boolean analyzeOnly, boolean allocMem, boolean fromSyscall) throws IOException {
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
            f.position(pbp.getOffsetPspData());
            return true;
        }
        // Not a valid PBP
        return false;
    }
    
    /** @return true on success */
    private boolean LoadSPRX(ByteBuffer f, SceModule module, TPointer baseAddress, boolean analyzeOnly, boolean allocMem, boolean fromSyscall, boolean isSignChecked, byte[] key) throws IOException {
        int magicPSP = Utilities.readWord(f);
        int magicEDAT = Utilities.readWord(f);
        if ((magicPSP == PSP_MAGIC) && (magicEDAT == EDAT_MAGIC)) {
            log.warn("Encrypted file detected! (.PSPEDAT)");
            // Skip the EDAT header and load as a regular ~PSP prx.
            f.position(0x90);
            LoadPSP(f.slice(), module, baseAddress, analyzeOnly, allocMem, fromSyscall, isSignChecked, key);
            return true;
        }
        // Not a valid SPRX
        return false;
    }

    /** @return true on success */
    private boolean LoadSCE(ByteBuffer f, SceModule module, TPointer baseAddress, boolean analyzeOnly, boolean allocMem, boolean fromSyscall, boolean isSignChecked, byte[] key) throws IOException {
        int magic = Utilities.readWord(f);
        if (magic == SCE_MAGIC) {
        	int size = Utilities.readWord(f);
        	f.position(f.position() + size - 8);
        	return LoadPSP(f, module, baseAddress, analyzeOnly, allocMem, fromSyscall, isSignChecked, key);
        }
        // Not a valid PSP
        return false;
    }

    /** @return true on success */
    private boolean LoadPSP(ByteBuffer f, SceModule module, TPointer baseAddress, boolean analyzeOnly, boolean allocMem, boolean fromSyscall, boolean isSignChecked, byte[] key) throws IOException {
    	int position = f.position();
        PSP psp = new PSP(f);
        // Reset the position after reading the header
        f.position(position);

        if (!psp.isValid()) {
            // Not a valid PSP
        	return false;
        }
        module.fileFormat |= FORMAT_PSP;

        if (key == null) {
        	key = scePopsMan.readEbootKeys(module.pspfilename);
        }

        if (module.psf != null) {
        	String updaterVer = module.psf.getString("UPDATER_VER");
        	if (updaterVer != null) {
        		Emulator.getInstance().setFirmwareVersion(updaterVer);
        	}
        }

        long start = System.currentTimeMillis();
    	ByteBuffer decryptedPrx = psp.decrypt(f, isSignChecked, key);
    	long end = System.currentTimeMillis();

    	if (decryptedPrx == null) {
    		return false;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Called crypto engine for PRX (duration=%d ms)", end - start));
    	}

    	return LoadELF(decryptedPrx, module, baseAddress, analyzeOnly, allocMem, fromSyscall);
    }

    /** @return true on success */
    private boolean LoadELF(ByteBuffer f, SceModule module, TPointer baseAddress, boolean analyzeOnly, boolean allocMem, boolean fromSyscall) throws IOException {
        int elfOffset = f.position();
        Elf32 elf = new Elf32(f);
        if (elf.getHeader().isValid()) {
            module.fileFormat |= FORMAT_ELF;

            if (!elf.getHeader().isMIPSExecutable()) {
                log.error("Loader NOT a MIPS executable");
                return false;
            }

            if (elf.isKernelMode()) {
                module.mpidtext = SysMemUserForUser.KERNEL_PARTITION_ID;
                module.mpiddata = SysMemUserForUser.KERNEL_PARTITION_ID;
                if (!analyzeOnly && baseAddress.getAddress() == MemoryMap.START_USERSPACE + 0x4000) {
                	baseAddress.setAddress(MemoryMap.START_RAM + Utilities.alignUp(ThreadManForUser.INTERNAL_THREAD_ADDRESS_SIZE, SysMemUserForUser.defaultSizeAlignment - 1));
                }
            }

            if (elf.getHeader().isPRXDetected()) {
                log.debug("Loader: Relocation required (PRX)");
                module.fileFormat |= FORMAT_PRX;
            } else if (elf.getHeader().requiresRelocation()) {
                // Seen in .elf's generated by pspsdk with BUILD_PRX=1 before conversion to .prx
                log.info("Loader: Relocation required (ELF)");
            } else {
                // After the user chooses a game to run and we load it, then
                // we can't load another PBP at the same time. We can only load
                // relocatable modules (PRX's) after the user loaded app.
                if (baseAddress.getAddress() > 0x08900000) {
                    log.warn("Loader: Probably trying to load PBP ELF while another PBP ELF is already loaded");
                }

                baseAddress.setAddress(0);
            }

            module.baseAddress = baseAddress.getAddress();
            if (elf.getHeader().getE_entry() == -1) {
                module.entry_addr = -1;
            } else {
                module.entry_addr = baseAddress.getAddress() + elf.getHeader().getE_entry();
            }

            // Note: baseAddress is 0 unless we are loading a PRX
            // Set loadAddressLow to the highest possible address, it will be updated
            // by LoadELFProgram().
            module.loadAddressLow = baseAddress.isNotNull() ? baseAddress.getAddress() : MemoryMap.END_USERSPACE;
            module.loadAddressHigh = baseAddress.getAddress();

            // Load into mem
            LoadELFProgram(f, module, baseAddress, elf, elfOffset, analyzeOnly);
            LoadELFSections(f, module, baseAddress, elf, elfOffset, analyzeOnly);

            if (module.loadAddressLow > module.loadAddressHigh) {
            	log.error(String.format("Incorrect ELF module address: loadAddressLow=0x%08X, loadAddressHigh=0x%08X", module.loadAddressLow, module.loadAddressHigh));
            	module.loadAddressHigh = module.loadAddressLow;
            }

            if (!analyzeOnly) {
	            // Relocate PRX
	            if (elf.getHeader().requiresRelocation()) {
	                relocateFromHeaders(f, module, baseAddress, elf, elfOffset);
	            }

	            // The following can only be done after relocation
	            // Load .rodata.sceModuleInfo
	            LoadELFModuleInfo(f, module, baseAddress, elf, elfOffset, analyzeOnly);
	            if (allocMem) {
		            // After LoadELFModuleInfo so the we can name the memory allocation after the module name
		            LoadELFReserveMemory(module);
	            }
	            // Save imports
	            LoadELFImports(module, baseAddress);
	            // Save exports
	            LoadELFExports(module, baseAddress);
	            // Try to fixup imports for ALL modules
	            Managers.modules.addModule(module);
	            ProcessUnresolvedImports(module, baseAddress, fromSyscall);

	            // Debug
	            LoadELFDebuggerInfo(f, module, baseAddress, elf, elfOffset, fromSyscall);

	            // If no text_addr is available up to now, use the lowest program header address
	            if (module.text_addr == 0) {
	                for (Elf32ProgramHeader phdr : elf.getProgramHeaderList()) {
	                	if (module.text_addr == 0 || phdr.getP_vaddr() < module.text_addr) {
	                		module.text_addr = phdr.getP_vaddr();
	                		// Align the text_addr if an alignment has been specified
	                		if (phdr.getP_align() > 0) {
	                			module.text_addr = Utilities.alignDown(module.text_addr, phdr.getP_align() - 1);
	                		}
	                	}
	                }
	            }

	            // Flush module struct to psp mem
	            if (baseAddress.getMemory() == Emulator.getMemory()) {
	            	module.write(baseAddress.getMemory(), module.address);
	            }
            } else {
	            LoadELFModuleInfo(f, module, baseAddress, elf, elfOffset, analyzeOnly);
	            if (elf.getHeader().requiresRelocation()) {
	            	LoadSDKVersion(f, module, elf, elfOffset);
	            }
            }
            return true;
        }
		// Not a valid ELF
		log.debug("Loader: Not a ELF");
		return false;
    }

    /** Dummy loader for unrecognized file formats, put at the end of a loader chain.
     * @return true on success */
    private boolean LoadUNK(ByteBuffer f, SceModule module, TPointer baseAddress, boolean analyzeOnly, boolean allocMem, boolean fromSyscall) throws IOException {

        byte m0 = f.get();
        byte m1 = f.get();
        byte m2 = f.get();
        byte m3 = f.get();

        // Catch common user errors
        if (m0 == 0x43 && m1 == 0x49 && m2 == 0x53 && m3 == 0x4F) { // CSO
            log.info("This is not an executable file!");
            log.info("Try using the Load UMD menu item");
        } else if ((m0 == 0 && m1 == 0x50 && m2 == 0x53 && m3 == 0x46)) { // PSF
            log.info("This is not an executable file!");
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
                    log.info("This is not an executable file!");
                    log.info("Try using the Load UMD menu item");
                    handled = true;
                }
            }

            if (!handled) {
                log.info("Unrecognized file format");
                log.info(String.format("File magic %02X %02X %02X %02X", m0, m1, m2, m3));
                if (log.isDebugEnabled()) {
                    byte[] buffer = new byte[0x150];
                    buffer[0] = m0;
                    buffer[1] = m1;
                    buffer[2] = m2;
                    buffer[3] = m3;
                    f.get(buffer, 4, buffer.length - 4);
                    log.debug(String.format("File header: %s", Utilities.getMemoryDump(buffer, 0, buffer.length)));
                }
            }
        }

        return false;
    }

    // ELF Loader

    /** Load some programs into memory */
    private void LoadELFProgram(ByteBuffer f, SceModule module, TPointer baseAddress, Elf32 elf, int elfOffset, boolean analyzeOnly) throws IOException {

        List<Elf32ProgramHeader> programHeaderList = elf.getProgramHeaderList();
        Memory mem = baseAddress.getMemory();

        module.text_size = 0;
        module.data_size = 0;
        module.bss_size = 0;

        int i = 0;
        for (Elf32ProgramHeader phdr : programHeaderList) {
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("ELF Program Header: %s", phdr.toString()));
        	}
            if (phdr.getP_type() == 0x00000001L) {
                int fileOffset = (int)phdr.getP_offset();
                int memOffset = baseAddress.getAddress() + (int)phdr.getP_vaddr();
                if (!Memory.isAddressGood(memOffset)) {
                    memOffset = (int)phdr.getP_vaddr();
                    if (!Memory.isAddressGood(memOffset)) {
                    	log.warn(String.format("Program header has invalid memory offset 0x%08X!", memOffset));
                    }
                }
                int fileLen = (int)phdr.getP_filesz();
                int memLen = (int)phdr.getP_memsz();

                if (log.isDebugEnabled()) {
                	log.debug(String.format("PH#%d: loading program 0x%08X-0x%08X", i, memOffset, memOffset + memLen));
                	log.debug(String.format("PH#%d:\n%s", i, phdr));
                }

                f.position(elfOffset + fileOffset);
                if (f.position() + fileLen > f.limit()) {
                    int newLen = f.limit() - f.position();
                    log.warn(String.format("PH#%d: program overflow clamping len %08X to %08X", i, fileLen, newLen));
                    fileLen = newLen;
                }
                if (!analyzeOnly) {
                	if (memLen > fileLen) {
                		// Clear the memory part not loaded from the file
                		mem.memset(memOffset + fileLen, (byte) 0, memLen - fileLen);
                	}

                	if (((memOffset | fileLen | f.position()) & 3) == 0) {
                		ByteOrder order = f.order();
                		f.order(ByteOrder.LITTLE_ENDIAN);
                		IntBuffer intBuffer = f.asIntBuffer();
                		TPointer destAddr = new TPointer(baseAddress.getMemory(), memOffset);
                		// Optimize the most common case
                		if (RuntimeContext.hasMemoryInt(destAddr)) {
                			intBuffer.get(RuntimeContext.getMemoryInt(), (memOffset & addressMask) >> 2, fileLen >> 2);
                		} else {
                			int[] buffer = new int[fileLen >> 2];
                			intBuffer.get(buffer);
                			writeInt32(destAddr, fileLen, buffer, 0);
                		}
                		f.order(order);
                		f.position(f.position() + fileLen);
                	} else {
                		mem.copyToMemory(memOffset, f, fileLen);
                	}
                }

                // Update memory area consumed by the module
                if (memOffset < module.loadAddressLow) {
                    module.loadAddressLow = memOffset;
                    if (log.isDebugEnabled()) {
                    	log.debug(String.format("PH#%d: new loadAddressLow %08X", i, module.loadAddressLow));
                    }
                }
                if (memOffset + memLen > module.loadAddressHigh) {
                    module.loadAddressHigh = memOffset + memLen;
                    if (log.isTraceEnabled()) {
                    	log.trace(String.format("PH#%d: new loadAddressHigh %08X", i, module.loadAddressHigh));
                    }
                }

                module.segmentaddr[module.nsegment] = memOffset;
                module.segmentsize[module.nsegment] = memLen;
                module.nsegment++;

                /*
                 * If the segment is executable, it contains the .text section.
                 * Otherwise, it contains the .data section.
                 */
                if ((phdr.getP_flags() & PF_X) != 0) {
                	module.text_size += fileLen;
                } else {
                	module.data_size += fileLen;
                }

                /* Add the "extra" segment bytes to the .bss section. */
                if (fileLen < memLen) {
                	module.bss_size += memLen - fileLen;
                }
            }
            i++;
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("PH alloc consumption %08X (mem %08X)", (module.loadAddressHigh - module.loadAddressLow), module.bss_size));
        }
    }

    /** Load some sections into memory */
    private void LoadELFSections(ByteBuffer f, SceModule module, TPointer baseAddress, Elf32 elf, int elfOffset, boolean analyzeOnly) throws IOException {
        List<Elf32SectionHeader> sectionHeaderList = elf.getSectionHeaderList();
        Memory mem = baseAddress.getMemory();

        module.text_addr = baseAddress.getAddress();

        for (Elf32SectionHeader shdr : sectionHeaderList) {
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("ELF Section Header: %s", shdr.toString()));
        	}

            int memOffset = shdr.getSh_addr(baseAddress);
            int len = shdr.getSh_size();
            int flags = shdr.getSh_flags();

            if (flags != SHF_NONE && Memory.isAddressGood(memOffset)) {
        		boolean read = (flags & SHF_ALLOCATE) != 0;
        		boolean write = (flags & SHF_WRITE) != 0;
        		boolean execute = (flags & SHF_EXECUTE) != 0;
        		MemorySection memorySection = new MemorySection(memOffset, len, read, write, execute);
        		MemorySections.getInstance().addMemorySection(memorySection);
        	}

        	if ((flags & SHF_ALLOCATE) != 0) {
                switch (shdr.getSh_type()) {
                    case Elf32SectionHeader.SHT_PROGBITS: { // 1
                        // Load this section into memory
                        // now loaded using program header type 1
                        if (len == 0) {
                        	if (log.isDebugEnabled()) {
                        		log.debug(String.format("%s: ignoring zero-length type 1 section %08X", shdr.getSh_namez(), memOffset));
                        	}
                        } else if (!Memory.isAddressGood(memOffset)) {
                            log.error(String.format("Section header (type 1) has invalid memory offset 0x%08X!", memOffset));
                        } else {
	                        // Update memory area consumed by the module
	                        if (memOffset < module.loadAddressLow) {
	                            log.warn(String.format("%s: section allocates more than program %08X - %08X", shdr.getSh_namez(), memOffset, (memOffset + len)));
	                            module.loadAddressLow = memOffset;
	                        }
	                        if (memOffset + len > module.loadAddressHigh) {
	                            log.warn(String.format("%s: section allocates more than program %08X - %08X", shdr.getSh_namez(), memOffset, (memOffset + len)));
	                            module.loadAddressHigh = memOffset + len;
	                        }

	                        if ((flags & SHF_WRITE) != 0) {
	                        	if (log.isTraceEnabled()) {
	                        		log.trace(String.format("Section Header as data, len=0x%08X, data_size=0x%08X", len, module.data_size));
	                        	}
	                        } else {
	                        	if (log.isTraceEnabled()) {
	                        		log.trace(String.format("Section Header as text, len=0x%08X, text_size=0x%08X", len, module.text_size));
	                        	}
	                        }
                        }
                        break;
                    }

                    case Elf32SectionHeader.SHT_NOBITS: { // 8
                        // Zero out this portion of memory
                        if (len == 0) {
                        	if (log.isDebugEnabled()) {
                        		log.debug(String.format("%s: ignoring zero-length type 8 section %08X", shdr.getSh_namez(), memOffset));
                        	}
                        } else if (!Memory.isAddressGood(memOffset)) {
                            log.error(String.format("Section header (type 8) has invalid memory offset 0x%08X!", memOffset));
                        } else {
                        	if (log.isDebugEnabled()) {
                        		log.debug(String.format("%s: clearing section %08X - %08X (len %08X)", shdr.getSh_namez(), memOffset, (memOffset + len), len));
                        	}

                        	if (!analyzeOnly) {
                        		mem.memset(memOffset, (byte) 0x0, len);
                        	}

                            // Update memory area consumed by the module
                            if (memOffset < module.loadAddressLow) {
                                module.loadAddressLow = memOffset;
                                if (log.isDebugEnabled()) {
                                	log.debug(String.format("%s: new loadAddressLow %08X (+%08X)", shdr.getSh_namez(), module.loadAddressLow, len));
                                }
                            }
                            if (memOffset + len > module.loadAddressHigh) {
                                module.loadAddressHigh = memOffset + len;
                                if (log.isDebugEnabled()) {
                                	log.debug(String.format("%s: new loadAddressHigh %08X (+%08X)", shdr.getSh_namez(), module.loadAddressHigh, len));
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        if (log.isTraceEnabled()) {
    		log.trace(String.format("Storing module info: text addr 0x%08X, text_size 0x%08X, data_size 0x%08X, bss_size 0x%08X", module.text_addr, module.text_size, module.data_size, module.bss_size));
    	}
    }

    private void LoadELFReserveMemory(SceModule module) {
        // Mark the area of memory the module loaded into as used
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Reserving 0x%X bytes at 0x%08X for module '%s'", module.loadAddressHigh - module.loadAddressLow, module.loadAddressLow, module.pspfilename));
    	}

        int address = module.loadAddressLow & ~(SysMemUserForUser.defaultSizeAlignment - 1); // Round down to match sysmem allocations
        int size = module.loadAddressHigh - address;

        int partition = module.mpidtext > 0 ? module.mpidtext : SysMemUserForUser.USER_PARTITION_ID;
        SysMemInfo info = Modules.SysMemUserForUserModule.malloc(partition, module.modname, SysMemUserForUser.PSP_SMEM_Addr, size, address);
        if (info == null || info.addr != (address & Memory.addressMask)) {
            log.warn(String.format("Failed to properly reserve memory consumed by module %s at address 0x%08X, size 0x%X: allocated %s", module.modname, address, size, info));
        }
        module.addAllocatedMemory(info);
    }

    /** Loads from memory */
    private void LoadELFModuleInfo(ByteBuffer f, SceModule module, TPointer baseAddress, Elf32 elf, int elfOffset, boolean analyzeOnly) throws IOException {

        Elf32ProgramHeader phdr = elf.getProgramHeader(0);
        Elf32SectionHeader shdr = elf.getSectionHeader(".rodata.sceModuleInfo");

        int moduleInfoAddr = 0;
        int moduleInfoFileOffset = -1;
        if (!elf.getHeader().isPRXDetected() && shdr == null) {
        	if (analyzeOnly) {
        		moduleInfoFileOffset = phdr.getP_paddr() & Memory.addressMask;
        	} else {
	            log.warn("ELF is not PRX, but has no section headers!");
	            moduleInfoAddr = phdr.getP_vaddr() + (phdr.getP_paddr() & Memory.addressMask) - phdr.getP_offset();
	            log.warn("Manually locating ModuleInfo at address: 0x" + Integer.toHexString(moduleInfoAddr));
        	}
        } else if (elf.getHeader().isPRXDetected()) {
        	if (analyzeOnly) {
        		moduleInfoFileOffset = phdr.getP_paddr() & Memory.addressMask;
        	} else {
        		moduleInfoAddr = baseAddress.getAddress() + (phdr.getP_paddr() & Memory.addressMask) - phdr.getP_offset();
        	}
        } else if (shdr != null) {
        	moduleInfoAddr = shdr.getSh_addr(baseAddress);
        }

        if (moduleInfoAddr != 0) {
            PSPModuleInfo moduleInfo = new PSPModuleInfo();
            moduleInfo.read(baseAddress.getMemory(), moduleInfoAddr);
            module.copy(moduleInfo);
        } else if (moduleInfoFileOffset >= 0) {
            PSPModuleInfo moduleInfo = new PSPModuleInfo();
            f.position(moduleInfoFileOffset);
            moduleInfo.read(f);
            module.copy(moduleInfo);
        } else {
            log.error("ModuleInfo not found!");
            return;
        }

        if (!analyzeOnly) {
	        if (log.isInfoEnabled()) {
	        	log.info(String.format("Found ModuleInfo at 0x%08X, name:'%s', version: %04X, attr: 0x%08X, gp: 0x%08X", moduleInfoAddr, module.modname, module.version, module.attribute, module.gp_value));
	        }

	        if ((module.attribute & SceModule.PSP_MODULE_KERNEL) != 0) {
	            log.debug("Kernel mode module detected");
	        }
	        if ((module.attribute & SceModule.PSP_MODULE_VSH) != 0) {
	            log.debug("VSH mode module detected");
	        }
        }
    }

    /**
     * @param f        The position of this buffer must be at the start of a
     *                 list of Elf32Rel structs.
     * @param RelCount The number of Elf32Rel structs to read and process.
     * @param pspRelocationFormat true if the relocation are in the PSP specific format,
     *                            false if the relocation is in standard ELF format.
     */
    private void relocateFromBuffer(ByteBuffer f, SceModule module, TPointer baseAddress, Elf32 elf, int RelCount, boolean pspRelocationFormat) throws IOException {

    	Elf32Relocate rel = new Elf32Relocate();
        int AHL = 0; // (AHI << 16) | (ALO & 0xFFFF)
        List<Integer> deferredHi16 = new LinkedList<Integer>(); // We'll use this to relocate R_MIPS_HI16 when we get a R_MIPS_LO16

        Memory mem = baseAddress.getMemory();
        for (int i = 0; i < RelCount; i++) {
            rel.read(f);

            int phOffset;
            int phBaseOffset;

            int R_OFFSET = rel.getR_offset();
            int R_TYPE = rel.getR_info() & 0xFF;
            if (pspRelocationFormat) {
	            int OFS_BASE  = (rel.getR_info() >>  8) & 0xFF;
	            int ADDR_BASE = (rel.getR_info() >> 16) & 0xFF;
	            if (log.isTraceEnabled()) {
	            	log.trace(String.format("Relocation #%d type=%d, Offset PH#%d, Base Offset PH#%d, Offset 0x%08X", i, R_TYPE, OFS_BASE, ADDR_BASE, R_OFFSET));
	            }

	            phOffset = elf.getProgramHeader(OFS_BASE).getP_vaddr();
	            phBaseOffset = elf.getProgramHeader(ADDR_BASE).getP_vaddr();
            } else {
            	phOffset = 0;
            	phBaseOffset = 0;
	            if (log.isTraceEnabled()) {
	            	log.trace(String.format("Relocation #%d type=%d, Symbol 0x%06X, Offset 0x%08X", i, R_TYPE, rel.getR_info() >> 8, R_OFFSET));
	            }
            }

            // Address of data to relocate
            int data_addr = baseAddress.getAddress() + R_OFFSET + phOffset;
            // Value of data to relocate
            int data = readUnaligned32(mem, data_addr);
            long result = 0; // Used to hold the result of relocation, OR this back into data

            int word32 = data & 0xFFFFFFFF; // <=> data;
            int targ26 = data & 0x03FFFFFF;
            int hi16 = data & 0x0000FFFF;
            int lo16 = data & 0x0000FFFF;

            int A = 0; // addend
            int S = baseAddress.getAddress() + phBaseOffset;

            switch (R_TYPE) {
                case 0: //R_MIPS_NONE
                    // Tested on PSP: R_MIPS_NONE is ignored
                    if (log.isTraceEnabled()) {
                		log.trace(String.format("R_MIPS_NONE addr=0x%08X", data_addr));
                	}
                    break;

                case 1: // R_MIPS_16
                	data = (data & 0xFFFF0000) | ((data + S) & 0x0000FFFF);
                	if (log.isTraceEnabled()) {
                		log.trace(String.format("R_MIPS_16 addr=0x%08X before=0x%08X after=0x%08X", data_addr, word32, data));
                    }
                	break;

                case 2: //R_MIPS_32
                    data += S;
                	if (log.isTraceEnabled()) {
                		log.trace(String.format("R_MIPS_32 addr=0x%08X before=0x%08X after=0x%08X", data_addr, word32, data));
                    }
                    break;

                case 4: //R_MIPS_26
                    A = targ26;
                    result = ((A << 2) + S) >> 2;
                    data &= ~0x03FFFFFF;
                    data |= (int) (result & 0x03FFFFFF); // truncate
                	if (log.isTraceEnabled()) {
                		log.trace(String.format("R_MIPS_26 addr=0x%08X before=0x%08X after=0x%08X", data_addr, word32, data));
                    }
                    break;

                case 5: //R_MIPS_HI16
                    A = hi16;
                    AHL = A << 16;
                    deferredHi16.add(data_addr);
                	if (log.isTraceEnabled()) {
                		log.trace(String.format("R_MIPS_HI16 addr=0x%08X", data_addr));
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
                        int data2 = readUnaligned32(mem, data_addr2);
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
                        data2 |= (int) ((result >> 16) & 0x0000FFFF);
                    	if (log.isTraceEnabled()) {
                    		log.trace(String.format("R_MIPS_HILO16 addr=0x%08X before=0x%08X after=0x%08X", data_addr2, readUnaligned32(mem, data_addr2), data2));
                        }
                    	writeUnaligned32(mem, data_addr2, data2);
                        it.remove();
                    }
                	if (log.isTraceEnabled()) {
                		log.trace(String.format("R_MIPS_LO16 addr=0x%08X before=0x%08X after=0x%08X", data_addr, word32, data));
                	}
                    break;

                case 7: // R_MIPS_GPREL16
                	// This relocation type is ignored by the PSP
                    if (log.isTraceEnabled()) {
                		log.trace(String.format("R_MIPS_GPREL16 addr=0x%08X before=0x%08X after=0x%08X", data_addr, word32, data));
                    }
                    break;

                default:
                	log.warn(String.format("Unhandled relocation type %d at 0x%08X", R_TYPE, data_addr));
                    break;
            }

            writeUnaligned32(mem, data_addr, data);
        }
    }

    private static String getRTypeName(int R_TYPE) {
    	String[] names = {
    			"R_MIPS_NONE",
    			"R_MIPS_16",
    			"R_MIPS_32",
    			"R_MIPS_26",
    			"R_MIPS_HI16",
    			"R_MIPS_LO16",
    			"R_MIPS_J26",
    			"R_MIPS_JAL26"
    	};
    	if (R_TYPE < 0 || R_TYPE >= names.length) {
    		return String.format("%d", R_TYPE);
    	}
    	return names[R_TYPE];
    }

    private void relocateFromBufferA1(ByteBuffer f, SceModule module, Elf32 elf, TPointer baseAddress, int programHeaderNumber, int size) throws IOException {
        Memory mem = baseAddress.getMemory();

        // Relocation variables.
        int R_OFFSET = 0;
        int R_BASE = 0;
        int OFS_BASE = 0;

        // Data variables.
        int data_addr;
        int data;
        int lo16 = 0;
        int hi16;
        int phBaseOffset;
        int r = 0;

        int end = f.position() + size;

        // Skip 2 unused bytes
        readUByte(f);
        readUByte(f);

        // Locate and read the flag, type and segment bits.
        int fbits = readUByte(f);
        int flagShift = 0;
        int flagMask = (1 << fbits) - 1;

        int sbits = programHeaderNumber < 3 ? 1 : 2;
        int segmentShift = fbits;
        int segmentMask = (1 << sbits) - 1;

        int tbits = readUByte(f);
        int typeShift = fbits + sbits;
        int typeMask = (1 << tbits) - 1;

        // Locate the flag table.
        int flags[] = new int[readUByte(f)];
        flags[0] = flags.length;
        for (int j = 1; j < flags.length; j++) {
        	flags[j] = readUByte(f);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("R_FLAG(%d bits) 0x%X -> 0x%X", fbits, j, flags[j]));
        	}
        }

        // Locate the type table.
        int types[] = new int[readUByte(f)];
        types[0] = types.length;
        for (int j = 1; j < types.length; j++) {
        	types[j] = readUByte(f);
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("R_TYPE(%d bits) 0x%X -> 0x%X", tbits, j, types[j]));
        	}
        }

        // loadcore.prx and sysmem.prx are being loaded and relocated by
        // the PSP reboot code. It is using a different type mapping.
        // See https://github.com/uofw/uofw/blob/master/src/reboot/elf.c#L327
    	if ("flash0:/kd/loadcore.prx".equals(module.pspfilename) || "flash0:/kd/sysmem.prx".equals(module.pspfilename)) {
    		final int[] rebootTypeRemapping = new int[] { 0, 3, 6, 7, 1, 2, 4, 5 };
    		for (int i = 1; i < types.length; i++) {
    			types[i] = rebootTypeRemapping[types[i]];
    		}
    	}

        int pos = f.position();
        int R_TYPE_OLD = types.length;
        while (pos < end) {
            // Read the CMD byte.
            int R_CMD = readUHalf(f);
            pos += 2;

            // Process the relocation flag.
            int flagIndex = (R_CMD >> flagShift) & flagMask;
            int R_FLAG = flags[flagIndex];

            // Set the segment offset.
            int S = (R_CMD >> segmentShift) & segmentMask;

            // Process the relocation type.
            int typeIndex = (R_CMD >> typeShift) & typeMask;
            int R_TYPE = types[typeIndex];

            // Operate on segment offset based on the relocation flag.
            if ((R_FLAG & 0x01) == 0) {
                if (log.isTraceEnabled()) {
                	log.trace(String.format("Relocation 0x%04X, R_FLAG=0x%02X(%d), S=%d, rest=0x%X", R_CMD, R_FLAG, flagIndex, S, R_CMD >> (fbits + sbits)));
                }

                OFS_BASE = S;
                if ((R_FLAG & 0x06) == 0) {
                    R_BASE = (R_CMD >> (fbits + sbits));
                } else if ((R_FLAG & 0x06) == 4) {
                    R_BASE = readUWord(f);
                    pos += 4;
                } else {
                    log.warn("PH Relocation type 0x700000A1: Invalid size flag!");
                    R_BASE = 0;
                }
            } else { // Operate on segment address based on the relocation flag.
                if (log.isTraceEnabled()) {
                	log.trace(String.format("Relocation 0x%04X, R_FLAG=0x%02X(%d), S=%d, %s(%d), rest=0x%X", R_CMD, R_FLAG, flagIndex, S, getRTypeName(R_TYPE), typeIndex, R_CMD >> (fbits + tbits + sbits)));
                }

                int ADDR_BASE = S;
                phBaseOffset = baseAddress.getAddress() + elf.getProgramHeader(ADDR_BASE).getP_vaddr();

                if ((R_FLAG & 0x06) == 0x00) {
                    R_OFFSET = (int) (short) R_CMD; // sign-extend 16 to 32 bits
                    R_OFFSET >>= (fbits + tbits + sbits);
                    R_BASE += R_OFFSET;
                } else if ((R_FLAG & 0x06) == 0x02) {
                    R_OFFSET = (R_CMD << 16) >> (fbits + tbits + sbits);
                    R_OFFSET &= 0xFFFF0000;
                    R_OFFSET |= readUHalf(f);
                    pos += 2;
                    R_BASE += R_OFFSET;
                } else if ((R_FLAG & 0x06) == 0x04) {
                    R_BASE = readUWord(f);
                    pos += 4;
                } else {
                    log.warn("PH Relocation type 0x700000A1: Invalid relocation size flag!");
                }

                // Process lo16.
                if ((R_FLAG & 0x38) == 0x00) {
                    lo16 = 0;
                } else if ((R_FLAG & 0x38) == 0x08) {
                    if ((R_TYPE_OLD ^ 0x04) != 0x00) {
                        lo16 = 0;
                    }
                } else if ((R_FLAG & 0x38) == 0x10) {
                    lo16 = readUHalf(f);
                    lo16 = (int) (short) lo16; // sign-extend 16 to 32 bits
                    pos += 2;
                } else if ((R_FLAG & 0x38) == 0x18) {
                    log.warn("PH Relocation type 0x700000A1: Invalid lo16 setup!");
                } else {
                    log.warn("PH Relocation type 0x700000A1: Invalid lo16 setup!");
                }

                // Read the data.
                data_addr = R_BASE + baseAddress.getAddress() + elf.getProgramHeader(OFS_BASE).getP_vaddr();
                data = readUnaligned32(mem, data_addr);

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Relocation #%d type=%d, Offset PH#%d, Base Offset PH#%d, Offset 0x%08X",
                            r, R_TYPE, OFS_BASE, ADDR_BASE, R_OFFSET));
                }

                int previousData = data;
                // Apply the changes as requested by the relocation type.
                switch (R_TYPE) {
                    case 0: // R_MIPS_NONE
                    	break;
                    case 2: // R_MIPS_32
                        data += phBaseOffset;
                        break;
                    case 3: // R_MIPS_26
                        data = (data & 0xFC000000) | (((data & 0x03FFFFFF) + (phBaseOffset >>> 2)) & 0x03FFFFFF);
                        break;
                    case 6: // R_MIPS_J26
                        data = (Opcodes.J << 26) | (((data & 0x03FFFFFF) + (phBaseOffset >>> 2)) & 0x03FFFFFF);
                        break;
                    case 7: // R_MIPS_JAL26
                        data = (Opcodes.JAL << 26) | (((data & 0x03FFFFFF) + (phBaseOffset >>> 2)) & 0x03FFFFFF);
                        break;
                    case 4: // R_MIPS_HI16
                        hi16 = ((data << 16) + lo16) + phBaseOffset;
                        if ((hi16 & 0x8000) == 0x8000) {
                            hi16 += 0x00010000;
                        }
                        data = (data & 0xffff0000) | (hi16 >>> 16);
                        break;
                    case 1: // R_MIPS_16
                    case 5: // R_MIPS_LO16
                        data = (data & 0xffff0000) | ((((int) (short) data) + phBaseOffset) & 0xffff);
                        break;
                    default:
                        break;
                }

                if (previousData != data) {
	                // Write the data.
	                writeUnaligned32(mem, data_addr, data);
	                if (log.isTraceEnabled()) {
	                	log.trace(String.format("Relocation at 0x%08X: 0x%08X -> 0x%08X", data_addr, previousData, data));
	                }
                }
                r++;

                R_TYPE_OLD = R_TYPE;
            }
        }
    }

    private boolean mustRelocate(Elf32 elf, Elf32SectionHeader shdr) {
    	if (shdr.getSh_type() == Elf32SectionHeader.SHT_PRXREL) {
    		// PSP PRX relocation section
    		return true;
    	}

    	if (shdr.getSh_type() == Elf32SectionHeader.SHT_REL) {
    		// Standard ELF relocation section
    		Elf32SectionHeader relatedShdr = elf.getSectionHeader(shdr.getSh_info());
    		// No relocation required for a debug section (sh_flags==SHF_NONE)
    		if (relatedShdr != null && relatedShdr.getSh_flags() != Elf32SectionHeader.SHF_NONE) {
    			return true;
    		}
    	}

    	return false;
    }

    /** Uses info from the elf program headers and elf section headers to
     * relocate a PRX. */
    private void relocateFromHeaders(ByteBuffer f, SceModule module, TPointer baseAddress, Elf32 elf, int elfOffset) throws IOException {

        // Relocate from program headers
        int i = 0;
        for (Elf32ProgramHeader phdr : elf.getProgramHeaderList()) {
            if (phdr.getP_type() == 0x700000A0) {
                int RelCount = phdr.getP_filesz() / Elf32Relocate.sizeof();
                if (log.isDebugEnabled()) {
                	log.debug(String.format("PH#%d: relocating %d entries", i, RelCount));
                }

                f.position(elfOffset + phdr.getP_offset());
                relocateFromBuffer(f, module, baseAddress, elf, RelCount, true);
                return;
            } else if (phdr.getP_type() == 0x700000A1) {
                if (log.isDebugEnabled()) {
                	log.debug(String.format("Type 0x700000A1 PH#%d: relocating A1 entries, size=0x%X", i, phdr.getP_filesz()));
                }
                f.position(elfOffset + phdr.getP_offset());
                relocateFromBufferA1(f, module, elf, baseAddress, i, phdr.getP_filesz());
                return;
            }
            i++;
        }

        // Relocate from section headers
        for (Elf32SectionHeader shdr : elf.getSectionHeaderList()) {
            if (mustRelocate(elf, shdr)) {
                int RelCount = shdr.getSh_size() / Elf32Relocate.sizeof();
                if (log.isDebugEnabled()) {
                	log.debug(shdr.getSh_namez() + ": relocating " + RelCount + " entries");
                }

                f.position(elfOffset + shdr.getSh_offset());
                relocateFromBuffer(f, module, baseAddress, elf, RelCount, shdr.getSh_type() != Elf32SectionHeader.SHT_REL);
            }
        }
    }

    private boolean isPopsLoader(SceModule module) throws IOException {
        // Pops loader from EBOOT.PBP
    	if (module.pspfilename.endsWith(scePopsMan.EBOOT_PBP)) {
	        if ("complex".equals(module.modname) || "simple".equals(module.modname)) {
	        	for (DeferredStub deferredStub : module.unresolvedImports) {
	        		if (deferredStub.getNid() == 0x29B3FB24 && "scePopsMan".equals(deferredStub.getModuleName())) {
	        			return true;
	        		}
	        	}
	        }
    	}

        return false;
    }

    private void ProcessUnresolvedImports(SceModule sourceModule, TPointer baseAddress, boolean fromSyscall) throws IOException {
        Memory mem = baseAddress.getMemory();
        NIDMapper nidMapper = NIDMapper.getInstance();
        int numberoffailedNIDS = 0;
        int numberofmappedNIDS = 0;

        if (isPopsLoader(sourceModule)) {
			Modules.scePopsManModule.loadOnDemand(sourceModule);
        }

        for (SceModule module : Managers.modules.values()) {
            module.importFixupAttempts++;
            for (Iterator<DeferredStub> it = module.unresolvedImports.iterator(); it.hasNext(); ) {
                DeferredStub deferredStub = it.next();
                String moduleName = deferredStub.getModuleName();
                int nid           = deferredStub.getNid();
                int importAddress = deferredStub.getImportAddress();

                // Attempt to fixup stub to point to an already loaded module export
                int exportAddress = nidMapper.getAddressByNid(nid, moduleName);
                if (exportAddress != 0) {
                	deferredStub.resolve(mem, exportAddress);
                    it.remove();
                    sourceModule.resolvedImports.add(deferredStub);
                    numberofmappedNIDS++;

                    if (log.isDebugEnabled()) {
                    	log.debug(String.format("Mapped import at 0x%08X to export at 0x%08X [0x%08X] (attempt %d)",
                    			importAddress, exportAddress, nid, module.importFixupAttempts));
                    }
                } else if (nid == 0) {
                	// Ignore patched nids
                    log.warn(String.format("Ignoring import at 0x%08X [0x%08X] (attempt %d)",
                        importAddress, nid, module.importFixupAttempts));

                    it.remove();
                    // This is an import to be ignored, implement it with the following
                    // code sequence:
                    //    jr $ra          (already written to memory)
                    //    li $v0, 0
                    // Rem.: "BUST A MOVE GHOST" is testing the return value $v0,
                    //       so it has to be set explicitly to 0.
                    mem.write32(importAddress + 4, MOVE(_v0, _zr));
                } else {
                    // Attempt to fixup stub to known syscalls
                    int code = nidMapper.getSyscallByNid(nid, moduleName);
                    if (code >= 0) {
                        // Some homebrews do not have a "jr $ra" set before the syscall
                        if (mem.read32(importAddress) == 0) {
                        	mem.write32(importAddress, JR());
                        }
                        // Fixup stub, replacing nop with syscall
                        mem.write32(importAddress + 4, SYSCALL(code));
                        it.remove();
                        numberofmappedNIDS++;

                        if ((fromSyscall && log.isDebugEnabled()) || log.isTraceEnabled()) {
                            log.debug(String.format("Mapped import at 0x%08X to syscall 0x%05X [0x%08X] (attempt %d)",
                                importAddress, code, nid, module.importFixupAttempts));
                        }
                    } else if (!nidMapper.isHideAllSyscalls()) {
                        log.warn(String.format("Failed to map import at 0x%08X [0x%08X] Module '%s'(attempt %d)",
                            importAddress, nid, moduleName, module.importFixupAttempts));
                        numberoffailedNIDS++;
                    }
                }
            }
        }

        log.info(numberofmappedNIDS + " NIDS mapped");
        if (numberoffailedNIDS > 0) {
            log.info(numberoffailedNIDS + " remaining unmapped NIDS");
        }
    }

    /* Loads from memory */
    private void LoadELFImports(SceModule module, TPointer baseAddress) throws IOException {
        Memory mem = baseAddress.getMemory();
        int stubHeadersAddress = module.stub_top;
        int stubHeadersEndAddress = module.stub_top + module.stub_size;

        // n modules to import, 1 stub header per module to import.
        String moduleName;
        for (int i = 0; stubHeadersAddress < stubHeadersEndAddress; i++) {
            Elf32StubHeader stubHeader = new Elf32StubHeader(mem, stubHeadersAddress);

            // Skip 0 sized entries.
            if (stubHeader.getSize() <= 0) {
                log.warn("Skipping dummy entry with size " + stubHeader.getSize());
                stubHeadersAddress += Elf32StubHeader.sizeof() / 2;
            } else {
                if (Memory.isAddressGood((int)stubHeader.getOffsetModuleName())) {
                    moduleName = Utilities.readStringNZ((int) stubHeader.getOffsetModuleName(), 64);
                } else {
                    // Generate a module name.
                    moduleName = module.modname;
                }
                stubHeader.setModuleNamez(moduleName);

                if (log.isDebugEnabled()) {
                	log.debug(String.format("Processing Import #%d: %s", i, stubHeader.toString()));
                }

                if (stubHeader.hasVStub()) {
                    if (log.isDebugEnabled()) {
                    	log.debug(String.format("'%s': Header with VStub has size %d: %s", stubHeader.getModuleNamez(), stubHeader.getSize(), Utilities.getMemoryDump(stubHeadersAddress, stubHeader.getSize() * 4, 4, 16)));
                    }
                	int vStub = (int) stubHeader.getVStub();
                	if (vStub != 0) {
                    	int vStubSize = stubHeader.getVStubSize();
                		if (log.isDebugEnabled()) {
                			log.debug(String.format("VStub has size %d: %s", vStubSize, Utilities.getMemoryDump(vStub, vStubSize * 8, 4, 16)));
                		}
                		IMemoryReader vstubReader = MemoryReader.getMemoryReader(mem, vStub, vStubSize * 8, 4);
                    	for (int j = 0; j < vStubSize; j++) {
                    		int relocAddr = vstubReader.readNext();
	                    	int nid = vstubReader.readNext();
	                    	// relocAddr points to a list of relocation terminated by a 0
	                    	IMemoryReader relocReader = MemoryReader.getMemoryReader(mem, relocAddr, 4);
	                    	while (true) {
	                    		int reloc = relocReader.readNext();
	                    		if (reloc == 0) {
	                    			// End of relocation list
	                    			break;
	                    		}
	                    		int opcode = reloc >>> 26;
            					int address = (reloc & 0x03FFFFFF) << 2;
            					DeferredStub deferredStub = null;
                    			switch (opcode) {
                    				case AllegrexOpcodes.BNE:
                    					deferredStub = new DeferredVStubHI16(module, stubHeader.getModuleNamez(), address, nid);
                    					break;
                    				case AllegrexOpcodes.BLEZ:
                    					deferredStub = new DeferredVstubLO16(module, stubHeader.getModuleNamez(), address, nid);
                    					break;
                    				case AllegrexOpcodes.J:
                    					deferredStub = new DeferredVStub32(module, stubHeader.getModuleNamez(), address, nid);
                    					break;
                    				default:
        	                    		log.warn(String.format("Unknown Vstub relocation nid 0x%08X, reloc=0x%08X", nid, reloc));
        	                    		break;
                    			}

                    			if (deferredStub != null) {
                    				if (log.isDebugEnabled()) {
                    					log.debug(String.format("Vstub reloc %s", deferredStub));
                    				}
                    				module.unresolvedImports.add(deferredStub);
                    			}
	                    	}
                    	}
                    }
                }
                stubHeadersAddress += stubHeader.getSize() * 4;

                if (!Memory.isAddressGood((int) stubHeader.getOffsetNid()) || !Memory.isAddressGood((int) stubHeader.getOffsetText())) {
                	log.warn(String.format("Incorrect s_nid or s_text address in StubHeader #%d: %s", i, stubHeader.toString()));
                } else {
	                // n stubs per module to import
	                IMemoryReader nidReader = MemoryReader.getMemoryReader(mem, (int) stubHeader.getOffsetNid(), stubHeader.getImports() * 4, 4);
	                for (int j = 0; j < stubHeader.getImports(); j++) {
	                	int nid = nidReader.readNext();
	                	int importAddress = (int) (stubHeader.getOffsetText() + j * 8);
	                    DeferredStub deferredStub = new DeferredStub(module, stubHeader.getModuleNamez(), importAddress, nid);

	                    // Add a 0xfffff syscall so we can detect if an unresolved import is called
	                    deferredStub.unresolve(mem);
	                }
                }
            }
        }

        if (module.unresolvedImports.size() > 0) {
        	if (log.isInfoEnabled()) {
        		log.info(String.format("Found %d unresolved imports", module.unresolvedImports.size()));
        	}
        }
    }

    private int getOffsetFromRelocation(ByteBuffer f, Elf32 elf, int elfOffset, int relocationOffset, int relocationSize, int position) throws IOException {
    	int relocationCount = relocationSize / Elf32Relocate.sizeof();
    	f.position(elfOffset + relocationOffset);
    	Elf32Relocate rel = new Elf32Relocate();
        for (int i = 0; i < relocationCount; i++) {
        	rel.read(f);
            int R_TYPE = rel.getR_info() & 0xFF;
            if (R_TYPE == 2) { //R_MIPS_32
	            int OFS_BASE  = (rel.getR_info() >>  8) & 0xFF;
	            int ADDR_BASE = (rel.getR_info() >> 16) & 0xFF;
	        	int relocationPosition = rel.getR_offset() + elf.getProgramHeader(OFS_BASE).getP_vaddr();
	        	if (relocationPosition == position) {
	        		return elf.getProgramHeader(ADDR_BASE).getP_offset();
	        	}
            }
        }

        return -1;
    }

    private int getOffsetFromRelocationA1(ByteBuffer f, SceModule module, Elf32 elf, int elfOffset, int programHeaderNumber, int relocationOffset, int relocationSize, int position) throws IOException {
        // Relocation variables.
        int R_OFFSET = 0;
        int R_BASE = 0;

        // Buffer position variable.
        int pos = elfOffset + relocationOffset;
        int end = pos + relocationSize;

        // Locate and read the flag, type and segment bits.
        f.position(pos + 2);
        int fbits = readUByte(f);
        int flagShift = 0;
        int flagMask = (1 << fbits) - 1;

        int sbits = programHeaderNumber < 3 ? 1 : 2;
        int segmentShift = fbits;
        int segmentMask = (1 << sbits) - 1;

        int tbits = readUByte(f);
        int typeShift = fbits + sbits;
        int typeMask = (1 << tbits) - 1;

        // Locate the flag table.
        int flags[] = new int[readUByte(f)];
        flags[0] = flags.length;
        for (int j = 1; j < flags.length; j++) {
        	flags[j] = readUByte(f);
        }

        // Locate the type table.
        int types[] = new int[readUByte(f)];
        types[0] = types.length;
        for (int j = 1; j < types.length; j++) {
        	types[j] = readUByte(f);
        }

        // loadcore.prx and sysmem.prx are being loaded and relocated by
        // the PSP reboot code. It is using a different type mapping.
        // See https://github.com/uofw/uofw/blob/master/src/reboot/elf.c#L327
    	if ("flash0:/kd/loadcore.prx".equals(module.pspfilename) || "flash0:/kd/sysmem.prx".equals(module.pspfilename)) {
    		final int[] rebootTypeRemapping = new int[] { 0, 3, 6, 7, 1, 2, 4, 5 };
    		for (int i = 1; i < types.length; i++) {
    			types[i] = rebootTypeRemapping[types[i]];
    		}
    	}

        // Save the current position.
        pos = f.position();

        while (pos < end) {
            // Read the CMD byte.
            int R_CMD = readUHalf(f);
            pos += 2;

            // Process the relocation flag.
            int flagIndex = (R_CMD >> flagShift) & flagMask;
            int R_FLAG = flags[flagIndex];

            // Set the segment offset.
            int S = (R_CMD >> segmentShift) & segmentMask;

            // Process the relocation type.
            int typeIndex = (R_CMD >> typeShift) & typeMask;
            int R_TYPE = types[typeIndex];

            if ((R_FLAG & 0x01) == 0) {
                if ((R_FLAG & 0x06) == 0) {
                    R_BASE = (R_CMD >> (fbits + sbits));
                } else if ((R_FLAG & 0x06) == 4) {
                    R_BASE = readUWord(f);
                    pos += 4;
                } else {
                    R_BASE = 0;
                }
            } else {
                int ADDR_BASE = S;

                if ((R_FLAG & 0x06) == 0x00) {
                    R_OFFSET = (int) (short) R_CMD; // sign-extend 16 to 32 bits
                    R_OFFSET >>= (fbits + tbits + sbits);
                    R_BASE += R_OFFSET;
                } else if ((R_FLAG & 0x06) == 0x02) {
                    R_OFFSET = (R_CMD << 16) >> (fbits + tbits + sbits);
                    R_OFFSET &= 0xFFFF0000;
                    R_OFFSET |= readUHalf(f);
                    pos += 2;
                    R_BASE += R_OFFSET;
                } else if ((R_FLAG & 0x06) == 0x04) {
                    R_BASE = readUWord(f);
                    pos += 4;
                }

                if ((R_FLAG & 0x38) == 0x10) {
                	readUHalf(f);
                    pos += 2;
                }

                if (R_TYPE == 2) { // R_MIPS_32
                	if (R_BASE == position) {
                		return elf.getProgramHeader(ADDR_BASE).getP_offset();
                	}
                }
            }
        }

        return -1;
    }

    private int getOffsetFromRelocation(ByteBuffer f, SceModule module, Elf32 elf, int elfOffset, int position) throws IOException {
    	int i = 0;
        for (Elf32ProgramHeader phdr : elf.getProgramHeaderList()) {
            if (phdr.getP_type() == 0x700000A0) {
                int offset = getOffsetFromRelocation(f, elf, elfOffset, phdr.getP_offset(), phdr.getP_filesz(), position);
                if (offset >= 0) {
                	return offset;
                }
            } else if (phdr.getP_type() == 0x700000A1) {
                int offset = getOffsetFromRelocationA1(f, module, elf, elfOffset, i, phdr.getP_offset(), phdr.getP_filesz(), position);
                if (offset >= 0) {
                	return offset;
                }
            }
            i++;
        }

        for (Elf32SectionHeader shdr : elf.getSectionHeaderList()) {
        	if (shdr.getSh_type() == Elf32SectionHeader.SHT_PRXREL) {
                int offset = getOffsetFromRelocation(f, elf, elfOffset, shdr.getSh_offset(), shdr.getSh_size(), position);
                if (offset >= 0) {
                	return offset;
                }
        	}
        }

        return 0;
    }

    private void LoadSDKVersion(ByteBuffer f, SceModule module, Elf32 elf, int elfOffset) throws IOException {
        int entHeadersOffset = elfOffset + elf.getProgramHeader(0).getP_offset();
        int entHeadersAddress = module.ent_top;
        int entHeadersEndAddress = module.ent_top + module.ent_size;
        while (entHeadersAddress < entHeadersEndAddress) {
            f.position(entHeadersOffset + entHeadersAddress);
        	Elf32EntHeader entHeader = new Elf32EntHeader(f);

        	if (entHeader.getSize() <= 0) {
                entHeadersAddress += Elf32EntHeader.sizeof() / 2;
        	} else if (entHeader.getSize() > 4) {
                entHeadersAddress += entHeader.getSize() * 4;
            } else {
                entHeadersAddress += Elf32EntHeader.sizeof();
            }

            int functionCount = entHeader.getFunctionCount();
            int variableCount = entHeader.getVariableCount();
            int nidAddr = (int) entHeader.getOffsetResident();
            int exportAddr = nidAddr + (functionCount + variableCount) * 4;
            int variableTableAddr = exportAddr + functionCount * 4;

            int[] variableNids = new int[variableCount];
            f.position(entHeadersOffset + nidAddr + functionCount * 4);
            for (int j = 0; j < variableCount; j++) {
            	variableNids[j] = readUWord(f);
            }

            int[] variableTable = new int[variableCount];
            for (int j = 0; j < variableCount; j++) {
            	int pos = variableTableAddr + j * 4;
            	int offset = getOffsetFromRelocation(f, module, elf, elfOffset, pos);
            	f.position(entHeadersOffset + pos);
            	variableTable[j] = readUWord(f) + offset;
            }

            for (int j = 0; j < variableCount; j++) {
            	switch (variableNids[j]) {
            		case 0x11B97506: // module_sdk_version
            			f.position(elfOffset + variableTable[j]);
            			module.moduleVersion = readUWord(f);
            			if (log.isDebugEnabled()) {
            				log.debug(String.format("Found sdkVersion=0x%08X", module.moduleVersion));
            			}
            			break;
            	}
            }
        }
    }

    /* Loads from memory */
    private void LoadELFExports(SceModule module, TPointer baseAddress) throws IOException {
        NIDMapper nidMapper = NIDMapper.getInstance();
        Memory mem = baseAddress.getMemory();
        int entHeadersAddress = module.ent_top;
        int entHeadersEndAddress = module.ent_top + module.ent_size;
        int entCount = 0;

        // n modules to export, 1 ent header per module to export.
        String moduleName;
        for (int i = 0; entHeadersAddress < entHeadersEndAddress; i++) {
            Elf32EntHeader entHeader = new Elf32EntHeader(mem, entHeadersAddress);

            if ((entHeader.getSize() <= 0)) {
                // Skip 0 sized entries.
                log.warn("Skipping dummy entry with size " + entHeader.getSize());
                entHeadersAddress += Elf32EntHeader.sizeof() / 2;
            } else {
                if (Memory.isAddressGood((int)entHeader.getOffsetModuleName())) {
                    moduleName = Utilities.readStringNZ((int) entHeader.getOffsetModuleName(), 64);
                } else {
                    // Generate a module name.
                    moduleName = module.modname;
                }
                entHeader.setModuleNamez(moduleName);

                if (log.isDebugEnabled()) {
                	log.debug(String.format("Processing header #%d at 0x%08X: %s", i, entHeadersAddress, entHeader.toString()));
                }

                if (entHeader.getSize() > 4) {
                    if (log.isDebugEnabled()) {
                    	log.debug(String.format("'%s': Header has size %d: %s", entHeader.getModuleNamez(), entHeader.getSize(), Utilities.getMemoryDump(entHeadersAddress, entHeader.getSize() * 4, 4, 16)));
                    }
                    entHeadersAddress += entHeader.getSize() * 4;
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
                IMemoryReader nidReader = MemoryReader.getMemoryReader(mem, nidAddr, (functionCount + variableCount) * 4, 4);
                int exportAddr = nidAddr + (functionCount + variableCount) * 4;
                IMemoryReader exportReader = MemoryReader.getMemoryReader(mem, exportAddr, functionCount * 4, 4);
                if ((entHeader.getAttr() & 0x8000) == 0) {
	                for (int j = 0; j < functionCount; j++) {
	                    int nid = nidReader.readNext();
	                    int exportAddress = exportReader.readNext();
                        // Only accept exports with valid export addresses and
	                    // from custom modules (attr != 0x4000) unless
	                    // the module is a homebrew (loaded from MemoryStick) or
	                    // this is the EBOOT module.
                        if (Memory.isAddressGood(exportAddress) && ((entHeader.getAttr() & 0x4000) != 0x4000) || module.pspfilename.startsWith("ms0:") || module.pspfilename.startsWith("disc0:/PSP_GAME/SYSDIR/EBOOT.") || module.pspfilename.startsWith("flash0:")) {
                            nidMapper.addModuleNid(module, moduleName, nid, exportAddress, false, entHeader.requiresSyscall());
                            entCount++;
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("Export found at 0x%08X [0x%08X]", exportAddress, nid));
                            }
                        }
	                }
                } else {
	                for (int j = 0; j < functionCount; j++) {
	                    int nid = nidReader.readNext();
	                    int exportAddress = exportReader.readNext();

	                    switch (nid) {
	                        case 0xD632ACDB: // module_start
	                            module.module_start_func = exportAddress;
	                            if (log.isDebugEnabled()) {
	                                log.debug(String.format("module_start found: nid=0x%08X, function=0x%08X", nid, exportAddress));
	                            }
	                            break;
	                        case 0xCEE8593C: // module_stop
	                            module.module_stop_func = exportAddress;
	                            if (log.isDebugEnabled()) {
	                                log.debug(String.format("module_stop found: nid=0x%08X, function=0x%08X", nid, exportAddress));
	                            }
	                            break;
	                        case 0x2F064FA6: // module_reboot_before
	                            module.module_reboot_before_func = exportAddress;
	                            if (log.isDebugEnabled()) {
	                                log.debug(String.format("module_reboot_before found: nid=0x%08X, function=0x%08X", nid, exportAddress));
	                            }
	                            break;
	                        case 0xADF12745: // module_reboot_phase
	                            module.module_reboot_phase_func = exportAddress;
	                            if (log.isDebugEnabled()) {
	                                log.debug(String.format("module_reboot_phase found: nid=0x%08X, function=0x%08X", nid, exportAddress));
	                            }
	                            break;
	                        case 0xD3744BE0: // module_bootstart
	                            module.module_bootstart_func = exportAddress;
	                            if (log.isDebugEnabled()) {
	                                log.debug(String.format("module_bootstart found: nid=0x%08X, function=0x%08X", nid, exportAddress));
	                            }
	                            break;
	                        default:
	                            // Only accept exports from custom modules (attr != 0x4000) and with valid export addresses.
	                            if (Memory.isAddressGood(exportAddress) && ((entHeader.getAttr() & 0x4000) != 0x4000)) {
	                                nidMapper.addModuleNid(module, moduleName, nid, exportAddress, false, entHeader.requiresSyscall());
	                                entCount++;
	                                if (log.isDebugEnabled()) {
	                                    log.debug(String.format("Export found at 0x%08X [0x%08X]", exportAddress, nid));
	                                }
	                            }
	                            break;
	                    }
	                }
                }

                int variableTableAddr = exportAddr + functionCount * 4;
                IMemoryReader variableReader = MemoryReader.getMemoryReader(mem, variableTableAddr, variableCount * 4, 4);
                for (int j = 0; j < variableCount; j++) {
                    int nid = nidReader.readNext();
                    int variableAddr = variableReader.readNext();

                    switch (nid) {
                        case 0xF01D73A7: // module_info
                            // Seems to be ignored by the PSP
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("module_info found: nid=0x%08X, addr=0x%08X", nid, variableAddr));
                            }
                            break;
                        case 0x0F7C276C: // module_start_thread_parameter
                            module.module_start_thread_priority = mem.read32(variableAddr + 4);
                            module.module_start_thread_stacksize = mem.read32(variableAddr + 8);
                            module.module_start_thread_attr = mem.read32(variableAddr + 12);
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("module_start_thread_parameter found: nid=0x%08X, priority=%d, stacksize=%d, attr=0x%08X", nid, module.module_start_thread_priority, module.module_start_thread_stacksize, module.module_start_thread_attr));
                            }
                            break;
                        case 0xCF0CC697: // module_stop_thread_parameter
                            module.module_stop_thread_priority = mem.read32(variableAddr + 4);
                            module.module_stop_thread_stacksize = mem.read32(variableAddr + 8);
                            module.module_stop_thread_attr = mem.read32(variableAddr + 12);
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("module_stop_thread_parameter found: nid=0x%08X, priority=%d, stacksize=%d, attr=0x%08X", nid, module.module_stop_thread_priority, module.module_stop_thread_stacksize, module.module_stop_thread_attr));
                            }
                            break;
                        case 0xF4F4299D: // module_reboot_before_thread_parameter
                            module.module_reboot_before_thread_priority = mem.read32(variableAddr + 4);
                            module.module_reboot_before_thread_stacksize = mem.read32(variableAddr + 8);
                            module.module_reboot_before_thread_attr = mem.read32(variableAddr + 12);
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("module_reboot_before_thread_parameter found: nid=0x%08X, priority=%d, stacksize=%d, attr=0x%08X", nid, module.module_reboot_before_thread_priority, module.module_reboot_before_thread_stacksize, module.module_reboot_before_thread_attr));
                            }
                            break;
                        case 0x11B97506: // module_sdk_version
                            module.moduleVersion = mem.read32(variableAddr);
                            if (log.isDebugEnabled()) {
                                log.debug(String.format("module_sdk_version found: nid=0x%08X, sdkVersion=0x%08X", nid, module.moduleVersion));
                            }
                            break;
                        default:
                            // Only accept exports from custom modules (attr != 0x4000) and with valid export addresses.
                            if (Memory.isAddressGood(variableAddr) && ((entHeader.getAttr() & 0x4000) != 0x4000)) {
                                nidMapper.addModuleNid(module, moduleName, nid, variableAddr, true, entHeader.requiresSyscall());
                                entCount++;
                                if (log.isDebugEnabled()) {
                                    log.debug(String.format("Export found at 0x%08X [0x%08X]", variableAddr, nid));
                                }
                            } else {
                            	log.warn(String.format("Unknown variable entry found: nid=0x%08X, addr=0x%08X", nid, variableAddr));
                            }
                            break;
                    }
                }
            }
        }

        if (entCount > 0) {
        	if (log.isInfoEnabled()) {
        		log.info(String.format("Found %d exports", entCount));
        	}
        }
    }

    private void LoadELFDebuggerInfo(ByteBuffer f, SceModule module, TPointer baseAddress, Elf32 elf, int elfOffset, boolean fromSyscall) throws IOException {
        // Save executable section address/size for the debugger/instruction counter
        Elf32SectionHeader shdr;

        shdr = elf.getSectionHeader(".init");
        if (shdr != null) {
            module.initsection[0] = shdr.getSh_addr(baseAddress);
            module.initsection[1] = shdr.getSh_size();
        }

        shdr = elf.getSectionHeader(".fini");
        if (shdr != null) {
            module.finisection[0] = shdr.getSh_addr(baseAddress);
            module.finisection[1] = shdr.getSh_size();
        }

        shdr = elf.getSectionHeader(".sceStub.text");
        if (shdr != null) {
            module.stubtextsection[0] = shdr.getSh_addr(baseAddress);
            module.stubtextsection[1] = shdr.getSh_size();
        }

        if (!fromSyscall) {
            ElfHeaderInfo.ElfInfo = elf.getElfInfo();
            ElfHeaderInfo.ProgInfo = elf.getProgInfo();
            ElfHeaderInfo.SectInfo = elf.getSectInfo();
        }
    }

    /**
     * Apply patches to some VSH and Kernel modules
     * 
     * @param module
     */
    private void patchModule(SceModule module) {
    	Memory mem = Emulator.getMemory();

    	// Same patches as ProCFW
    	if ("vsh_module".equals(module.modname)) {
    		patch(mem, module, 0x000122B0, 0x506000E0, NOP());
    		patch(mem, module, 0x00012058, 0x1440003B, NOP());
    		patch(mem, module, 0x00012060, 0x14400039, NOP());
    	}

    	// Patches to replace "https" with "http" so that the URL calls
    	// can be proxied through the internal HTTP server.
    	if ("sceNpCommerce2".equals(module.modname)) {
    		patch(mem, module, 0x0000A598, 0x00000073, 0x00000000); // replace "https" with "http"
    		patch(mem, module, 0x00003A60, 0x240701BB, 0x24070050); // replace port 443 with 80
    	}
    	if ("sceNpCore".equals(module.modname)) {
    		patchRemoveStringChar(mem, module, 0x00000D50, 's'); // replace "https" with "http" in "https://auth.%s.ac.playstation.net/nav/auth"
    	}
    	if ("sceNpService".equals(module.modname)) {
    		patch(mem, module, 0x0001075C, 0x00000073, 0x00000000); // replace "https" with "http" for "https://getprof.%s.np.community.playstation.net/basic_view/sec/get_self_profile"
    	}
    	if ("sceVshNpInstaller_Module".equals(module.modname)) {
    		patchRemoveStringChar(mem, module, 0x00016F90, 's'); // replace "https" with "http" in "https://commerce.%s.ac.playstation.net/cap.m"
    		patchRemoveStringChar(mem, module, 0x00016FC0, 's'); // replace "https" with "http" in "https://commerce.%s.ac.playstation.net/cdp.m"
    		patchRemoveStringChar(mem, module, 0x00016FF0, 's'); // replace "https" with "http" in "https://commerce.%s.ac.playstation.net/kdp.m"
    		patchRemoveStringChar(mem, module, 0x00017020, 's'); // replace "https" with "http" in "https://account.%s.ac.playstation.net/ecomm/ingame/startDownloadDRM"
    		patchRemoveStringChar(mem, module, 0x00017064, 's'); // replace "https" with "http" in "https://account.%s.ac.playstation.net/ecomm/ingame/finishDownloadDRM"
    	}
    	if ("marlindownloader".equals(module.modname)) {
    		patchRemoveStringChar(mem, module, 0x000046C8, 's'); // replace "https" with "http" in "https://mds.%s.ac.playstation.net/"
    	}
    	if ("sceVshStoreBrowser_Module".equals(module.modname)) {
    		patchRemoveStringChar(mem, module, 0x0005A244, 's'); // replace "https" with "http" in "https://nsx-e.sec.%s.dl.playstation.net/nsx/sec/..."
    		patchRemoveStringChar(mem, module, 0x0005A2D8, 's'); // replace "https" with "http" in "https://nsx.sec.%s.dl.playstation.net/nsx/sec/..."
    	}
    	if ("sceGameUpdate_Library".equals(module.modname)) {
    		patchRemoveStringChar(mem, module, 0x000030C4, 's'); // replace "https" with "http" in "https://a0.ww.%s.dl.playstation.net/tpl/..."
    	}

    	if ("sceModuleManager".equals(module.modname)) {
    		patch(mem, module, 0x000030CC, 0x24030020, 0x24030010); // replace "li $v1, 32" with "li $v1, 16" (this will be stored at SceLoadCoreExecFileInfo.apiType)
    	}

    	if ("sceLoaderCore".equals(module.modname)) {
    		patch(mem, module, 0x0000469C, 0x15C0FFA0, NOP()); // Allow loading of privileged modules being not encrypted (https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L339)
    		patch(mem, module, 0x00004548, 0x7C0F6244, NOP()); // Allow loading of privileged modules being not encrypted (take SceLoadCoreExecFileInfo.modInfoAttribute from the ELF module info, https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L351)
    		patch(mem, module, 0x00004550, 0x14E0002C, 0x1000002C); // Allow loading of privileged modules being not encrypted (https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L352)
    		patch(mem, module, 0x00003D58, 0x10C0FFBE, NOP()); // Allow linking user stub to kernel lib
    	}

    	Modules.scePopsManModule.patchModule(mem, module);
    }
}