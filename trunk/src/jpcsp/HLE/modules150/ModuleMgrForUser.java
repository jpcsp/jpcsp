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
package jpcsp.HLE.modules150;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_ERRNO_FILE_NOT_FOUND;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_UNKNOWN_MODULE;

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.Loader;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelModuleInfo;
import jpcsp.HLE.kernel.types.SceKernelLMOption;
import jpcsp.HLE.kernel.types.SceKernelSMOption;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.format.PSP;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class ModuleMgrForUser implements HLEModule {
    protected static Logger log = Modules.getLogger("ModuleMgrForUser");

    // Modules that should never be loaded
    // (include here only modules not described in HLEModuleManager)
    enum bannedModulesList {
        audiocodec,
        sceAudiocodec_Driver,
        videocodec,
        sceVideocodec_Driver,
        mpegbase,
        sceMpegbase_Driver,
        pspnet_adhoc_download,
        pspnet_ap_dialog_dummy,
        sceNetApDialogDummy_Library,
        libparse_uri,
        libparse_http
    }

    public static final int loadHLEModuleDelay = 1000; // 1 ms delay

    @Override
    public String getName() {
        return "ModuleMgrForUser";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0xB7F46618, sceKernelLoadModuleByIDFunction);
            mm.addFunction(0x977DE386, sceKernelLoadModuleFunction);
            mm.addFunction(0x710F61B5, sceKernelLoadModuleMsFunction);
            mm.addFunction(0xF9275D98, sceKernelLoadModuleBufferUsbWlanFunction);
            mm.addFunction(0x50F0C1EC, sceKernelStartModuleFunction);
            mm.addFunction(0xD1FF982A, sceKernelStopModuleFunction);
            mm.addFunction(0x2E0911AA, sceKernelUnloadModuleFunction);
            mm.addFunction(0xD675EBB8, sceKernelSelfStopUnloadModuleFunction);
            mm.addFunction(0xCC1D3699, sceKernelStopUnloadSelfModuleFunction);
            mm.addFunction(0x644395E2, sceKernelGetModuleIdListFunction);
            mm.addFunction(0x748CBED9, sceKernelQueryModuleInfoFunction);
            mm.addFunction(0xF0A26395, sceKernelGetModuleIdFunction);
            mm.addFunction(0xD8B73127, sceKernelGetModuleIdByAddressFunction);
            mm.addFunction(0x8f2df740, sceKernelStopUnloadSelfModuleWithStatusFunction);
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceKernelLoadModuleByIDFunction);
            mm.removeFunction(sceKernelLoadModuleFunction);
            mm.removeFunction(sceKernelLoadModuleMsFunction);
            mm.removeFunction(sceKernelLoadModuleBufferUsbWlanFunction);
            mm.removeFunction(sceKernelStartModuleFunction);
            mm.removeFunction(sceKernelStopModuleFunction);
            mm.removeFunction(sceKernelUnloadModuleFunction);
            mm.removeFunction(sceKernelSelfStopUnloadModuleFunction);
            mm.removeFunction(sceKernelStopUnloadSelfModuleFunction);
            mm.removeFunction(sceKernelGetModuleIdListFunction);
            mm.removeFunction(sceKernelQueryModuleInfoFunction);
            mm.removeFunction(sceKernelGetModuleIdFunction);
            mm.removeFunction(sceKernelGetModuleIdByAddressFunction);
            mm.removeFunction(sceKernelStopUnloadSelfModuleWithStatusFunction);
        }
    }

    //
    // When an HLE module is loaded using sector syntax, with no file corresponding to the
    // referenced sector, try searching for the real module's name inside the file itself.
    // For encrypted modules, the real name can be found in the first sector of the file.
    // This name is not encrypted.
    //
    // For example:
    //   MONSTER HUNTER FREEDOM UNITE ULES01213
    //     hleKernelLoadModule(path='disc0:/sce_lbn0x11981_size0x59c0')
    //   and the sector 0x11981 is found inside a huge "DATA.BIN" file (a CD image):
    //     PSP_GAME/USRDIR/DATA.BIN: Starting at sector 0xD960, with size 737 MB
    //
    private String extractHLEModuleName(String path) {
        String result = "UNKNOWN";
        String sectorString = path.substring(path.indexOf("sce_lbn") + 7, path.indexOf("_size"));
        int PRXStartSector = (int) Utilities.parseHexLong(sectorString);

        try {
            byte[] buffer = Modules.IoFileMgrForUserModule.getIsoReader().readSector(PRXStartSector);
            String libName = new String(buffer);
            if (libName.contains("sce") && (libName.indexOf("sce") >= 0) && (libName.indexOf(" ") >= 0)) {
                String module = libName.substring(libName.indexOf("sce"), libName.indexOf(" "));
                // Compare with known names and assign the real name for this module.
                if (module.contains("sceFont")) {
                    result = "libfont";
                } else if (module.contains("sceMpeg")) {
                    result = "mpeg";
                } else if (module.contains("sceSAScore")) {
                    result = "sc_sascore";
                } else if (module.contains("sceATRAC3plus")) {
                    result = "libatrac3plus";
                }
            }
        } catch (IOException ioe) {
            // Sector doesn't exist...
        }
        return result;
    }

    private boolean loadHLEModule(Processor processor, String name, String prxname) {
        CpuState cpu = processor.cpu;
        HLEModuleManager moduleManager = HLEModuleManager.getInstance();

        // Check if this an HLE module
        if (moduleManager.hasFlash0Module(prxname)) {
            log.info("hleKernelLoadModule(path='" + name + "') HLE module loaded");
            cpu.gpr[2] = moduleManager.LoadFlash0Module(prxname);
            return true;
        }

        // Ban some modules
        for (bannedModulesList bannedModuleName : bannedModulesList.values()) {
            if (bannedModuleName.name().equalsIgnoreCase(prxname.toString())) {
                log.warn("IGNORED:hleKernelLoadModule(path='" + name + "'): module from banlist not loaded");
                cpu.gpr[2] = moduleManager.LoadFlash0Module(prxname.toString());
                return true;
            }
        }

        return false;
    }

    private boolean hleKernelLoadHLEModule(Processor processor, String name, StringBuilder prxname) {
        CpuState cpu = processor.cpu;
        HLEModuleManager moduleManager = HLEModuleManager.getInstance();

        if (prxname == null) {
            prxname = new StringBuilder();
        }

        // Extract the PRX name from the file name
        int findprx = name.lastIndexOf("/");
        int endprx = name.toLowerCase().indexOf(".prx");
        if (endprx >= 0) {
            prxname.append(name.substring(findprx + 1, endprx));
        } else if (name.contains("sce_lbn")) {
            prxname.append(extractHLEModuleName(name));
        } else {
            prxname.append("UNKNOWN");
        }

        // Ban flash0 modules
        if (name.startsWith("flash0:")) {
            log.warn("IGNORED:hleKernelLoadModule(path='" + name + "'): module from flash0 not loaded");
            cpu.gpr[2] = moduleManager.LoadFlash0Module(prxname.toString());
            return true;
        }

        // Check if the PRX name matches an HLE module
        if (loadHLEModule(processor, name, prxname.toString())) {
        	return true;
        }

        // Extract the library name from the file itself
        // for files in "~SCE"/"~PSP" format.
        SeekableDataInput moduleInput = Modules.IoFileMgrForUserModule.getFile(name, IoFileMgrForUser.PSP_O_RDONLY);
        if (moduleInput != null) {
        	final int sceHeaderLength = 0x40;
        	byte[] header = new byte[sceHeaderLength + 100];
        	try {
				moduleInput.readFully(header);
        		ByteBuffer f = ByteBuffer.wrap(header);
        		int sceMagic = Utilities.readWord(f);
        		if (sceMagic == Loader.SCE_MAGIC) {
        			f.position(sceHeaderLength);
        			int pspMagic = Utilities.readWord(f);
        			if (pspMagic == PSP.PSP_MAGIC) {
        				f.position(f.position() + 6);
        				String libName = Utilities.readStringZ(f);
        				if (libName != null && libName.length() > 0) {
        					// We could extract the library name from the file,
        					// check if it matches an HLE module
        					if (loadHLEModule(processor, name, libName)) {
            					prxname.setLength(0);
            					prxname.append(libName);
        						return true;
        					}
        				}
        			}
        		}
			} catch (IOException e) {
				// Ignore exception
			} finally {
				try {
					moduleInput.close();
				} catch (IOException e) {
					// Ignore exception
				}
			}
        }

        return false;
    }

    private void hleKernelLoadModule(Processor processor, String name, int flags, int uid, boolean byUid) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        StringBuilder prxname = new StringBuilder();
        if (hleKernelLoadHLEModule(processor, name, prxname)) {
        	Modules.ThreadManForUserModule.hleKernelDelayThread(loadHLEModuleDelay, false);
            return;
        }

        // Load module as ELF
        try {
            SeekableDataInput moduleInput = Modules.IoFileMgrForUserModule.getFile(name, flags);
            if (moduleInput != null) {
                if (moduleInput instanceof UmdIsoFile) {
                    UmdIsoFile umdIsoFile = (UmdIsoFile) moduleInput;
                    String realFileName = umdIsoFile.getName();
                    if (realFileName != null && !name.endsWith(realFileName)) {
                        if (hleKernelLoadHLEModule(processor, realFileName, null)) {
                            moduleInput.close();
                            return;
                        }
                    }
                }

                byte[] moduleBytes = new byte[(int) moduleInput.length()];
                moduleInput.readFully(moduleBytes);
                ByteBuffer moduleBuffer = ByteBuffer.wrap(moduleBytes);

                // TODO
                // We need to get a load address so that we can allocate
                // the memory required by the module.
                // As we don't know yet how much space the module will require,
                // estimate the requirement to the size of the file itself plus
                // some space for the module header itself.
                // We allocate the estimated size and free it immediately so that
                // we know the load address.
                final int partitionId = 2;
                final int allocType = SysMemUserForUser.PSP_SMEM_Low;
                final int moduleHeaderSize = 256;

                // Load the module in analyze mode to find out its required memory size
                SceModule testModule = Loader.getInstance().LoadModule(name, moduleBuffer, MemoryMap.START_USERSPACE, true);
                moduleBuffer.rewind();
                int totalAllocSize = moduleHeaderSize + testModule.loadAddressHigh - testModule.loadAddressLow;
                if (log.isDebugEnabled()) {
                	log.debug(String.format("Module '%s' requires %d bytes memory", name, totalAllocSize));
                }

                SysMemInfo testInfo = Modules.SysMemUserForUserModule.malloc(partitionId, "ModuleMgr-TestInfo", allocType, totalAllocSize, 0);
                if (testInfo == null) {
                    log.error(String.format("Failed module allocation of size 0x%08X for '%s' (maxFreeMemSize=0x%08X)", totalAllocSize, name, Modules.SysMemUserForUserModule.maxFreeMemSize()));
                    cpu.gpr[2] = -1;
                    return;
                }
                int testBase = testInfo.addr;
                Modules.SysMemUserForUserModule.free(testInfo);

                // Allocate the memory for the memory header itself,
                // the space required by the module will be allocated by the Loader.
                SysMemInfo moduleInfo = Modules.SysMemUserForUserModule.malloc(partitionId, "ModuleMgr", SysMemUserForUser.PSP_SMEM_Addr, moduleHeaderSize, testBase);
                if (moduleInfo == null) {
                    log.error(String.format("Failed module allocation 0x%08X != null for '%s'", testBase, name));
                    cpu.gpr[2] = -1;
                    return;
                }
                if (moduleInfo.addr != testBase) {
                    log.error(String.format("Failed module allocation 0x%08X != 0x%08X for '%s'", testBase, moduleInfo.addr, name));
                    cpu.gpr[2] = -1;
                    return;
                }
                int moduleBase = moduleInfo.addr;

                // Load the module
                SceModule module = Loader.getInstance().LoadModule(name, moduleBuffer, moduleBase + moduleHeaderSize, false);
                module.load();

                if ((module.fileFormat & Loader.FORMAT_SCE) == Loader.FORMAT_SCE ||
                        (module.fileFormat & Loader.FORMAT_PSP) == Loader.FORMAT_PSP) {
                    // Simulate a successful loading
                    log.info("hleKernelLoadModule(path='" + name + "') encrypted module not loaded");
                    SceModule fakeModule = new SceModule(true);
                    fakeModule.addAllocatedMemory(moduleInfo);
                    fakeModule.modname = prxname.toString();
                    fakeModule.write(mem, moduleInfo.addr);
                    Managers.modules.addModule(fakeModule);
                    cpu.gpr[2] = fakeModule.modid;
                } else if ((module.fileFormat & Loader.FORMAT_ELF) == Loader.FORMAT_ELF) {
                    module.addAllocatedMemory(moduleInfo);
                    cpu.gpr[2] = module.modid;
                } else {
                    // The Loader class now manages the module's memory footprint, it won't allocate if it failed to load
                    cpu.gpr[2] = -1;
                }

                moduleInput.close();
            } else {
                log.warn("hleKernelLoadModule(path='" + name + "') can't find file");
                cpu.gpr[2] = ERROR_ERRNO_FILE_NOT_FOUND;
            }
        } catch (IOException e) {
            log.error("hleKernelLoadModule - Error while loading module " + name + ": " + e.getMessage());
            cpu.gpr[2] = -1;
        }
    }

    protected int getSelfModuleId() {
        return Modules.ThreadManForUserModule.getCurrentThread().moduleid;
    }

    public void sceKernelLoadModuleByID(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int uid = cpu.gpr[4];
        int option_addr = cpu.gpr[5];
        String name = Modules.IoFileMgrForUserModule.getFileFilename(uid);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelLoadModuleByID(uid=0x" + Integer.toHexString(uid) + "('" + name + "')" + ",option=0x" + Integer.toHexString(option_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceKernelLMOption lmOption = null;
        if (option_addr != 0) {
            lmOption = new SceKernelLMOption();
            lmOption.read(mem, option_addr);
            log.info("sceKernelLoadModule: partition=" + lmOption.mpidText + ", position=" + lmOption.position);
        }

        hleKernelLoadModule(processor, name, 0, uid, true);
    }

    public void sceKernelLoadModule(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int path_addr = cpu.gpr[4];
        int flags = cpu.gpr[5];
        int option_addr = cpu.gpr[6];
        String name = Utilities.readStringZ(path_addr);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelLoadModule(path='" + name + "',flags=0x" + Integer.toHexString(flags) + ",option=0x" + Integer.toHexString(option_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceKernelLMOption lmOption = null;
        if (option_addr != 0) {
            lmOption = new SceKernelLMOption();
            lmOption.read(mem, option_addr);
            log.info("sceKernelLoadModule: partition=" + lmOption.mpidText + ", position=" + lmOption.position);
        }

        hleKernelLoadModule(processor, name, flags, 0, false);
    }

    public void sceKernelLoadModuleMs(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceKernelLoadModuleMs [0x710F61B5]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceKernelLoadModuleBufferUsbWlan(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceKernelLoadModuleBufferUsbWlan [0xF9275D98]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceKernelStartModule(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int uid = cpu.gpr[4];
        int argsize = cpu.gpr[5];
        int argp_addr = cpu.gpr[6];
        int status_addr = cpu.gpr[7]; // TODO
        int option_addr = cpu.gpr[8];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelStartModule(uid=0x" + Integer.toHexString(uid) + ", argsize=" + argsize + ", argp=0x" + Integer.toHexString(argp_addr) + ", status=0x" + Integer.toHexString(status_addr) + ", option=0x" + Integer.toHexString(option_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceModule sceModule = Managers.modules.getModuleByUID(uid);
        SceKernelSMOption smOption = null;
        if (option_addr != 0) {
            smOption = new SceKernelSMOption();
            smOption.read(mem, option_addr);
        }

        if (sceModule == null) {
            log.warn("sceKernelStartModule - unknown module UID 0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_KERNEL_UNKNOWN_MODULE;
        } else if (sceModule.isFlashModule) {
            // Trying to start a module loaded from flash0:
            // Do nothing...
            if (HLEModuleManager.getInstance().hasFlash0Module(sceModule.modname)) {
                log.info("IGNORING:sceKernelStartModule HLE module '" + sceModule.modname + "'");
            } else {
                log.warn("IGNORING:sceKernelStartModule flash module '" + sceModule.modname + "'");
            }
            sceModule.start();
            cpu.gpr[2] = sceModule.modid; // return the module id
        } else {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            int attribute = sceModule.attribute;
            int entryAddr = sceModule.entry_addr;
            if (entryAddr == -1) {
                log.info("sceKernelStartModule - module has no entry point, trying to use module_start_func");
                entryAddr = sceModule.module_start_func;
                attribute = sceModule.module_start_thread_attr;
            }

            if (Memory.isAddressGood(entryAddr)) {
                if (Memory.isAddressGood(status_addr)) {
                    mem.write32(status_addr, 0); // TODO set to return value of the thread (when it exits, of course)
                }

                int priority = 0x20;
                if (smOption != null && smOption.priority > 0) {
                    priority = smOption.priority;
                } else if (sceModule.module_start_thread_priority > 0) {
                    priority = sceModule.module_start_thread_priority;
                }

                int stackSize = 0x40000;
                if (smOption != null && smOption.stackSize > 0) {
                    stackSize = smOption.stackSize;
                } else if (sceModule.module_start_thread_stacksize > 0) {
                    stackSize = sceModule.module_start_thread_stacksize;
                }

                if (smOption != null) {
                    attribute = smOption.attribute;
                }

                SceKernelThreadInfo thread = threadMan.hleKernelCreateThread("SceModmgrStart", entryAddr, priority, stackSize, attribute, 0);
                // override inherited module id with the new module we are starting
                thread.moduleid = sceModule.modid;
                cpu.gpr[2] = sceModule.modid; // return the module id
                sceModule.start();
                threadMan.hleKernelStartThread(thread, argsize, argp_addr, sceModule.gp_value);
            } else if (entryAddr == 0) {
                Modules.log.info("sceKernelStartModule - no entry address");
                sceModule.start();
                cpu.gpr[2] = sceModule.modid; // return the module id
            } else {
                Modules.log.warn("sceKernelStartModule - invalid entry address 0x" + Integer.toHexString(entryAddr));
                cpu.gpr[2] = -1;
            }
        }
    }

    public void sceKernelStopModule(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int uid = cpu.gpr[4];
        int argsize = cpu.gpr[5];
        int argp_addr = cpu.gpr[6];
        int status_addr = cpu.gpr[7]; // TODO
        int option_addr = cpu.gpr[8];

        log.info("sceKernelStopModule(uid=0x" + Integer.toHexString(uid) + ", argsize=" + argsize + ", argp=0x" + Integer.toHexString(argp_addr) + ", status=0x" + Integer.toHexString(status_addr) + ", option=0x" + Integer.toHexString(option_addr) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceModule sceModule = Managers.modules.getModuleByUID(uid);
        if (sceModule == null) {
            log.warn("sceKernelStopModule - unknown module UID 0x" + Integer.toHexString(uid));
            cpu.gpr[2] = ERROR_KERNEL_UNKNOWN_MODULE;
        } else if (sceModule.isFlashModule) {
            // Trying to stop a module loaded from flash0:
            // Shouldn't get here...
            if (HLEModuleManager.getInstance().hasFlash0Module(sceModule.modname)) {
                log.info("IGNORING:sceKernelStopModule HLE module '" + sceModule.modname + "'");
            } else {
                log.warn("IGNORING:sceKernelStopModule flash module '" + sceModule.modname + "'");
            }
            sceModule.stop();
            cpu.gpr[2] = 0; // Fake success.
        } else {
            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            if (Memory.isAddressGood(sceModule.module_stop_func)) {
                if (Memory.isAddressGood(status_addr)) {
                    mem.write32(status_addr, 0); // TODO set to return value of the thread (when it exits, of course)
                }
                SceKernelThreadInfo thread = threadMan.hleKernelCreateThread("SceModmgrStop",
                        sceModule.module_stop_func, sceModule.module_stop_thread_priority,
                        sceModule.module_stop_thread_stacksize, sceModule.module_stop_thread_attr, option_addr);
                thread.moduleid = sceModule.modid;
                cpu.gpr[2] = 0;
                sceModule.stop();
                threadMan.hleKernelStartThread(thread, argsize, argp_addr, sceModule.gp_value);
            } else if (sceModule.module_stop_func == 0) {
                log.info("sceKernelStopModule - module has no stop function");
                sceModule.stop();
                cpu.gpr[2] = sceModule.modid;
            } else if (sceModule.isModuleStopped()) {
                log.warn("sceKernelStopModule - module already stopped");
                cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_MODULE_ALREADY_STOPPED;
            } else {
                log.warn(String.format("sceKernelStopModule - invalid stop function 0x%08X", sceModule.module_stop_func));
                cpu.gpr[2] = -1;
            }
        }
    }

    public void sceKernelUnloadModule(Processor processor) {
        CpuState cpu = processor.cpu;

        int uid = cpu.gpr[4];

        log.info("sceKernelUnloadModule(uid=" + Integer.toHexString(uid) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceModule sceModule = Managers.modules.getModuleByUID(uid);
        if (sceModule == null) {
            log.warn("sceKernelUnloadModule unknown module UID 0x" + Integer.toHexString(uid));
            cpu.gpr[2] = -1;
        } else if (sceModule.isModuleStarted() && !sceModule.isModuleStopped()) {
            log.warn("sceKernelUnloadModule module 0x" + Integer.toHexString(uid) + " is still running!");
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_MODULE_CANNOT_REMOVE;
        } else {
            HLEModuleManager.getInstance().UnloadFlash0Module(sceModule);
            cpu.gpr[2] = sceModule.modid; // Returns the module ID.
        }
    }

    public void sceKernelSelfStopUnloadModule(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int argsize = cpu.gpr[4];
        int argp_addr = cpu.gpr[5];
        int status_addr = cpu.gpr[6];
        int options_addr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelSelfStopUnloadModule(argsize=" + argsize
                    + ",argp_addr=0x" + Integer.toHexString(argp_addr)
                    + ",status_addr=0x" + Integer.toHexString(status_addr)
                    + ",options_addr=0x" + Integer.toHexString(options_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceModule sceModule = Managers.modules.getModuleByUID(getSelfModuleId());
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        SceKernelThreadInfo thread = null;
        if (Memory.isAddressGood(sceModule.module_stop_func)) {
            // Start the module stop thread function.
            if (Memory.isAddressGood(status_addr)) {
                mem.write32(status_addr, 0); // TODO set to return value of the thread (when it exits, of course)
            }
            thread = threadMan.hleKernelCreateThread("SceModmgrStop",
                    sceModule.module_stop_func, sceModule.module_stop_thread_priority,
                    sceModule.module_stop_thread_stacksize, sceModule.module_stop_thread_attr, options_addr);
            thread.moduleid = sceModule.modid;
            cpu.gpr[2] = 0;
            sceModule.stop();
            sceModule.unload();
        } else {
            cpu.gpr[2] = 0;
        }

        threadMan.hleKernelExitDeleteThread();  // Delete the current thread.
        if (thread != null) {
        	threadMan.hleKernelStartThread(thread, argsize, argp_addr, sceModule.gp_value);
        }
    }

    public void sceKernelStopUnloadSelfModuleWithStatus(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int exitcode = cpu.gpr[4];
        int argsize = cpu.gpr[5];
        int argp_addr = cpu.gpr[6];
        int status_addr = cpu.gpr[7];
        int options_addr = cpu.gpr[8];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelSelfStopUnloadModule(exitcode=" + exitcode
                    + ",argsize=" + argsize
                    + ",argp_addr=0x" + Integer.toHexString(argp_addr)
                    + ",status_addr=0x" + Integer.toHexString(status_addr)
                    + ",options_addr=0x" + Integer.toHexString(options_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceModule sceModule = Managers.modules.getModuleByUID(getSelfModuleId());
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        SceKernelThreadInfo thread = null;
        if (Memory.isAddressGood(sceModule.module_stop_func)) {
            // Start the module stop thread function.
            if (Memory.isAddressGood(status_addr)) {
                mem.write32(status_addr, 0); // TODO set to return value of the thread (when it exits, of course)
            }
            thread = threadMan.hleKernelCreateThread("SceModmgrStop",
                    sceModule.module_stop_func, sceModule.module_stop_thread_priority,
                    sceModule.module_stop_thread_stacksize, sceModule.module_stop_thread_attr, options_addr);
            thread.moduleid = sceModule.modid;
            cpu.gpr[2] = 0;
            threadMan.getCurrentThread().exitStatus = exitcode; // Set the current thread's exit status.
            sceModule.stop();
            sceModule.unload();
        } else {
            cpu.gpr[2] = 0;
        }

        threadMan.hleKernelExitDeleteThread();  // Delete the current thread.
        if (thread != null) {
        	threadMan.hleKernelStartThread(thread, argsize, argp_addr, sceModule.gp_value);
        }
    }

    public void sceKernelStopUnloadSelfModule(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int argsize = cpu.gpr[4];
        int argp_addr = cpu.gpr[5];
        int status_addr = cpu.gpr[6];
        int options_addr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelStopUnloadSelfModule(argsize=" + argsize
                    + ",argp_addr=0x" + Integer.toHexString(argp_addr)
                    + ",status_addr=0x" + Integer.toHexString(status_addr)
                    + ",option_addr=0x" + Integer.toHexString(options_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceModule sceModule = Managers.modules.getModuleByUID(getSelfModuleId());
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        SceKernelThreadInfo thread = null;
        if (Memory.isAddressGood(sceModule.module_stop_func)) {
            // Start the module stop thread function.
            if (Memory.isAddressGood(status_addr)) {
                mem.write32(status_addr, 0); // TODO set to return value of the thread (when it exits, of course)
            }
            thread = threadMan.hleKernelCreateThread("SceModmgrStop",
                    sceModule.module_stop_func, sceModule.module_stop_thread_priority,
                    sceModule.module_stop_thread_stacksize, sceModule.module_stop_thread_attr, options_addr);
            thread.moduleid = sceModule.modid;
            cpu.gpr[2] = 0;
            sceModule.stop();
            sceModule.unload();
        } else {
            cpu.gpr[2] = 0;
        }

        threadMan.hleKernelExitDeleteThread();  // Delete the current thread.
        if (thread != null) {
        	threadMan.hleKernelStartThread(thread, argsize, argp_addr, sceModule.gp_value);
        }
    }

    public void sceKernelGetModuleIdList(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceKernelGetModuleIdList [0x644395E2]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceKernelQueryModuleInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int uid = cpu.gpr[4];
        int info_addr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelQueryModuleInfo UID 0x" + Integer.toHexString(uid) + " info " + String.format("0x%08X", info_addr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceModule sceModule = Managers.modules.getModuleByUID(uid);
        if (sceModule == null) {
            log.warn("sceKernelQueryModuleInfo unknown module UID 0x" + Integer.toHexString(uid));
            cpu.gpr[2] = -1;
        } else if (!Memory.isAddressGood(info_addr)) {
            log.warn("sceKernelQueryModuleInfo bad info pointer " + String.format("0x%08X", info_addr));
            cpu.gpr[2] = -1;
        } else {
            SceKernelModuleInfo moduleInfo = new SceKernelModuleInfo();
            moduleInfo.copy(sceModule);
            moduleInfo.write(mem, info_addr);
            cpu.gpr[2] = 0;
        }
    }

    public void sceKernelGetModuleId(Processor processor) {
        CpuState cpu = processor.cpu;

        int moduleid = getSelfModuleId();

        if (log.isDebugEnabled()) {
            log.debug("sceKernelGetModuleId returning 0x" + Integer.toHexString(moduleid));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = moduleid;
    }

    public void sceKernelGetModuleIdByAddress(Processor processor) {
        CpuState cpu = processor.cpu;

        int addr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug("sceKernelGetModuleIdByAddress(addr=0x" + Integer.toHexString(addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceModule module = Managers.modules.getModuleByAddress(addr);
        if (module != null) {
            cpu.gpr[2] = module.modid;
        } else {
            log.warn("sceKernelGetModuleIdByAddress(addr=0x" + Integer.toHexString(addr) + ") module not found");
            cpu.gpr[2] = -1;
        }
    }
    public final HLEModuleFunction sceKernelLoadModuleByIDFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelLoadModuleByID") {

        @Override
        public final void execute(Processor processor) {
            sceKernelLoadModuleByID(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelLoadModuleByID(processor);";
        }
    };
    public final HLEModuleFunction sceKernelLoadModuleFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelLoadModule") {

        @Override
        public final void execute(Processor processor) {
            sceKernelLoadModule(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelLoadModule(processor);";
        }
    };
    public final HLEModuleFunction sceKernelLoadModuleMsFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelLoadModuleMs") {

        @Override
        public final void execute(Processor processor) {
            sceKernelLoadModuleMs(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelLoadModuleMs(processor);";
        }
    };
    public final HLEModuleFunction sceKernelLoadModuleBufferUsbWlanFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelLoadModuleBufferUsbWlan") {

        @Override
        public final void execute(Processor processor) {
            sceKernelLoadModuleBufferUsbWlan(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelLoadModuleBufferUsbWlan(processor);";
        }
    };
    public final HLEModuleFunction sceKernelStartModuleFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelStartModule") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStartModule(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelStartModule(processor);";
        }
    };
    public final HLEModuleFunction sceKernelStopModuleFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelStopModule") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStopModule(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelStopModule(processor);";
        }
    };
    public final HLEModuleFunction sceKernelUnloadModuleFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelUnloadModule") {

        @Override
        public final void execute(Processor processor) {
            sceKernelUnloadModule(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelUnloadModule(processor);";
        }
    };
    public final HLEModuleFunction sceKernelSelfStopUnloadModuleFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelSelfStopUnloadModule") {

        @Override
        public final void execute(Processor processor) {
            sceKernelSelfStopUnloadModule(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelSelfStopUnloadModule(processor);";
        }
    };
    public final HLEModuleFunction sceKernelStopUnloadSelfModuleFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelStopUnloadSelfModule") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStopUnloadSelfModule(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelStopUnloadSelfModule(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetModuleIdListFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelGetModuleIdList") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetModuleIdList(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelGetModuleIdList(processor);";
        }
    };
    public final HLEModuleFunction sceKernelQueryModuleInfoFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelQueryModuleInfo") {

        @Override
        public final void execute(Processor processor) {
            sceKernelQueryModuleInfo(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelQueryModuleInfo(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetModuleIdFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelGetModuleId") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetModuleId(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelGetModuleId(processor);";
        }
    };
    public final HLEModuleFunction sceKernelGetModuleIdByAddressFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelGetModuleIdByAddress") {

        @Override
        public final void execute(Processor processor) {
            sceKernelGetModuleIdByAddress(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelGetModuleIdByAddress(processor);";
        }
    };
    public final HLEModuleFunction sceKernelStopUnloadSelfModuleWithStatusFunction = new HLEModuleFunction("ModuleMgrForUser", "sceKernelStopUnloadSelfModuleWithStatus") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStopUnloadSelfModuleWithStatus(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.ModuleMgrForUserModule.sceKernelStopUnloadSelfModuleWithStatus(processor);";
        }
    };
}