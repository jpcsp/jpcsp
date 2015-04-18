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

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.Loader;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelModuleInfo;
import jpcsp.HLE.kernel.types.SceKernelLMOption;
import jpcsp.HLE.kernel.types.SceKernelSMOption;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.IoFileMgrForUser;//Todo:Need change to IoFileMgrForKernel ?
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.umdiso.UmdIsoFile;
import jpcsp.format.PSP;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

@HLELogging
public class ModuleMgrForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("ModuleMgrForKernel");

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
        pspnet_adhoc_auth,
        pspnet_ap_dialog_dummy,
        sceNetApDialogDummy_Library,
        libparse_uri,
        libparse_http,
        memab
    }

    public static final int loadHLEModuleDelay = 50000; // 50 ms delay

    @Override
    public String getName() {
        return "ModuleMgrForKernel";
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
            int indexSce = libName.indexOf("sce");
            int indexSpace = libName.indexOf(" ");
            if (indexSce >= 0 && indexSpace > indexSce) {
                String module = libName.substring(indexSce, indexSpace);
                // Compare with known names and assign the real name for this module.
                if (module.startsWith("sceFont")) {
                    result = "libfont";
                } else if (module.startsWith("sceMpeg")) {
                    result = "mpeg";
                } else if (module.startsWith("sceSAScore")) {
                    result = "sc_sascore";
                } else if (module.startsWith("sceATRAC3plus")) {
                    result = "libatrac3plus";
                }
            }
        } catch (IOException ioe) {
            // Sector doesn't exist...
        }
        return result;
    }

    private int loadHLEModule(String name, String prxname) {
        HLEModuleManager moduleManager = HLEModuleManager.getInstance();

        // Check if this an HLE module
        if (moduleManager.hasFlash0Module(prxname)) {
        	if (log.isInfoEnabled()) {
        		log.info(String.format("hleKernelLoadModule(path='%s') HLE module %s loaded", name, prxname));
        	}
            return moduleManager.LoadFlash0Module(prxname);
        }

        // Ban some modules
        for (bannedModulesList bannedModuleName : bannedModulesList.values()) {
            if (bannedModuleName.name().equalsIgnoreCase(prxname)) {
                log.warn(String.format("IGNORED:hleKernelLoadModule(path='%s'): module %s from banlist not loaded", name , prxname));
                return moduleManager.LoadFlash0Module(prxname);
            }
        }

        return -1;
    }

    private int hleKernelLoadHLEModule(String name, StringBuilder prxname) {
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
            return moduleManager.LoadFlash0Module(prxname.toString());
        }

        // Check if the PRX name matches an HLE module
        int result = loadHLEModule(name, prxname.toString());
        if (result >= 0) {
        	return result;
        }

        // Extract the library name from the file itself
        // for files in "~SCE"/"~PSP" format.
        SeekableDataInput moduleInput = Modules.IoFileMgrForUserModule.getFile(name, IoFileMgrForUser.PSP_O_RDONLY);
    	result = detectHleModule(name, prxname, moduleInput);
    	if (moduleInput != null) {
    		try {
				moduleInput.close();
			} catch (IOException e) {
			}
    	}
    	if (result >= 0) {
    		return result;
    	}

        return -1;
    }

    private int detectHleModule(String name, StringBuilder prxname, SeekableDataInput file) {
    	int result = -1;

    	if (file == null) {
    		return result;
    	}

    	final int sceHeaderLength = 0x40;
    	byte[] header = new byte[sceHeaderLength + 100];
    	try {
        	long position = file.getFilePointer();
    		file.readFully(header);
    		file.seek(position);

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
    					result = loadHLEModule(name, libName);
    					if (result >= 0) {
    						if (prxname != null) {
    							prxname.setLength(0);
    							prxname.append(libName);
    						}
    						return result;
    					}
    				}
    			}
    		}
		} catch (IOException e) {
			// Ignore exception
		}

    	return result;
    }

    public int hleKernelLoadModule(String name, int flags, int uid, SceKernelLMOption lmOption, boolean byUid) {
        StringBuilder prxname = new StringBuilder();
        int result = hleKernelLoadHLEModule(name, prxname);
        if (result >= 0) {
        	Modules.ThreadManForUserModule.hleKernelDelayThread(loadHLEModuleDelay, false);
            return result;
        }

        // Load module as ELF
        try {
            SeekableDataInput moduleInput = Modules.IoFileMgrForUserModule.getFile(name, flags);
            if (moduleInput != null) {
                if (moduleInput instanceof UmdIsoFile) {
                    UmdIsoFile umdIsoFile = (UmdIsoFile) moduleInput;
                    String realFileName = umdIsoFile.getName();
                    if (realFileName != null && !name.endsWith(realFileName)) {
                        result = hleKernelLoadHLEModule(realFileName, null);
                        if (result >= 0) {
                            moduleInput.close();
                            return result;
                        }
                    }
                }

                byte[] moduleBytes = new byte[(int) moduleInput.length()];
                moduleInput.readFully(moduleBytes);
                moduleInput.close();
                ByteBuffer moduleBuffer = ByteBuffer.wrap(moduleBytes);

                // TODO
                // We need to get a load address so that we can allocate
                // the memory required by the module.
                // As we don't know yet how much space the module will require,
                // estimate the requirement to the size of the file itself plus
                // some space for the module header itself.
                // We allocate the estimated size and free it immediately so that
                // we know the load address.
                final int partitionId = lmOption != null && lmOption.mpidText != 0 ? lmOption.mpidText : SysMemUserForUser.USER_PARTITION_ID;
                final int allocType = lmOption != null ? lmOption.position : SysMemUserForUser.PSP_SMEM_Low;
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
                    return -1;
                }
                int testBase = testInfo.addr;
                Modules.SysMemUserForUserModule.free(testInfo);

                // Allocate the memory for the memory header itself,
                // the space required by the module will be allocated by the Loader.
                SysMemInfo moduleInfo = Modules.SysMemUserForUserModule.malloc(partitionId, "ModuleMgr", SysMemUserForUser.PSP_SMEM_Addr, moduleHeaderSize, testBase);
                if (moduleInfo == null) {
                    log.error(String.format("Failed module allocation 0x%08X != null for '%s'", testBase, name));
                    return -1;
                }
                if (moduleInfo.addr != testBase) {
                    log.error(String.format("Failed module allocation 0x%08X != 0x%08X for '%s'", testBase, moduleInfo.addr, name));
                    return -1;
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
                    fakeModule.write(Memory.getInstance(), moduleInfo.addr);
                    Managers.modules.addModule(fakeModule);
                    result = fakeModule.modid;
                } else if ((module.fileFormat & Loader.FORMAT_ELF) == Loader.FORMAT_ELF) {
                    module.addAllocatedMemory(moduleInfo);
                    result = module.modid;
                	if (log.isDebugEnabled()) {
                		log.debug(String.format("hleKernelLoadModule returning uid=0x%X", result));
                	}
                } else {
                    // The Loader class now manages the module's memory footprint, it won't allocate if it failed to load
                    return -1;
                }
            } else {
                log.warn("hleKernelLoadModule(path='" + name + "') can't find file");
                return ERROR_ERRNO_FILE_NOT_FOUND;
            }
        } catch (IOException e) {
            log.error("hleKernelLoadModule - Error while loading module " + name + ": " + e.getMessage());
            return -1;
        }

    	return result;
	}

    protected int getSelfModuleId() {
        return Modules.ThreadManForUserModule.getCurrentThread().moduleid;
    }

    @HLEFunction(nid = 0xB7F46618, version = 150, checkInsideInterrupt = true)
    public int sceKernelLoadModuleByID(int uid, @CanBeNull TPointer optionAddr) {
        String name = Modules.IoFileMgrForUserModule.getFileFilename(uid);
        SeekableDataInput file = Modules.IoFileMgrForUserModule.getFile(uid);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelLoadModuleByID name='%s'", name));
        }

        SceKernelLMOption lmOption = null;
        if (optionAddr.isNotNull()) {
            lmOption = new SceKernelLMOption();
            lmOption.read(optionAddr);
            if (log.isInfoEnabled()) {
            	log.info(String.format("sceKernelLoadModuleByID options: %s", lmOption));
            }
        }

        int result = detectHleModule(name, null, file);
        if (result >= 0) {
        	return result;
        }

        return hleKernelLoadModule(name, 0, uid, lmOption, true);
    }

    @HLEFunction(nid = 0x977DE386, version = 150, checkInsideInterrupt = true)
    public int sceKernelLoadModule(PspString path, int flags, @CanBeNull TPointer optionAddr) {
        SceKernelLMOption lmOption = null;
        if (optionAddr.isNotNull()) {
            lmOption = new SceKernelLMOption();
            lmOption.read(optionAddr);
            if (log.isInfoEnabled()) {
            	log.info(String.format("sceKernelLoadModule options: %s", lmOption));
            }
        }

        return hleKernelLoadModule(path.getString(), flags, 0, lmOption, false);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x710F61B5, version = 150, checkInsideInterrupt = true)
    public int sceKernelLoadModuleMs() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF9275D98, version = 150, checkInsideInterrupt = true)
    public int sceKernelLoadModuleBufferUsbWlan() {
        return 0;
    }

    @HLEFunction(nid = 0x50F0C1EC, version = 150, checkInsideInterrupt = true)
    public int sceKernelStartModule(int uid, int argSize, @CanBeNull TPointer argp, @CanBeNull TPointer32 statusAddr, @CanBeNull TPointer optionAddr) {
        SceModule sceModule = Managers.modules.getModuleByUID(uid);
        SceKernelSMOption smOption = null;
        if (optionAddr.isNotNull()) {
            smOption = new SceKernelSMOption();
            smOption.read(optionAddr);
        }

        if (sceModule == null) {
            log.warn(String.format("sceKernelStartModule - unknown module UID 0x%X", uid));
            return ERROR_KERNEL_UNKNOWN_MODULE;
        }

        statusAddr.setValue(0);

        if (sceModule.isFlashModule) {
            // Trying to start a module loaded from flash0:
            // Do nothing...
            if (HLEModuleManager.getInstance().hasFlash0Module(sceModule.modname)) {
                log.info(String.format("IGNORING:sceKernelStartModule HLE module '%s'", sceModule.modname));
            } else {
                log.warn(String.format("IGNORING:sceKernelStartModule flash module '%s'", sceModule.modname));
            }
            sceModule.start();
            return sceModule.modid; // return the module id
        }
        if (HLEModuleManager.getInstance().hasFlash0Module(sceModule.modname)) {
        	if (log.isInfoEnabled()) {
        		log.info(String.format("sceKernelStartModule - loading missing HLE module '%s' (was loaded as ELF)", sceModule.modname));
        	}
            HLEModuleManager.getInstance().LoadFlash0Module(sceModule.modname);
            sceModule.start();
            return sceModule.modid; // return the module id
        }

        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        int attribute = sceModule.attribute;
        int entryAddr = sceModule.entry_addr;
        if (Memory.isAddressGood(sceModule.module_start_func)) {
        	// Always take the module start function if one is defined.
            entryAddr = sceModule.module_start_func;
            attribute = sceModule.module_start_thread_attr;
        }

        if (Memory.isAddressGood(entryAddr)) {
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

            // Remember the current thread as it can be changed by hleKernelStartThread.
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

            SceKernelThreadInfo thread = threadMan.hleKernelCreateThread("SceModmgrStart", entryAddr, priority, stackSize, attribute, 0);
            // override inherited module id with the new module we are starting
            thread.moduleid = sceModule.modid;
            // Store the thread exit status into statusAddr when the thread terminates
            thread.exitStatusAddr = statusAddr;
            sceModule.start();

            // Start the module start thread
            threadMan.hleKernelStartThread(thread, argSize, argp.getAddress(), sceModule.gp_value);

            // Wait for the end of the module start thread.
            // Do no return the thread exit status as the result of this call,
            // return the module ID.
            threadMan.hleKernelWaitThreadEnd(currentThread, thread.uid, TPointer32.NULL, false, false);
        } else if (entryAddr == 0 || entryAddr == -1) {
            Modules.log.info("sceKernelStartModule - no entry address");
            sceModule.start();
        } else {
            log.warn(String.format("sceKernelStartModule - invalid entry address 0x%08X", entryAddr));
            return -1;
        }

        return sceModule.modid;
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0xD1FF982A, version = 150, checkInsideInterrupt = true)
    public int sceKernelStopModule(int uid, int argSize, @CanBeNull TPointer argp, @CanBeNull TPointer32 statusAddr, @CanBeNull TPointer optionAddr) {
        SceModule sceModule = Managers.modules.getModuleByUID(uid);
        SceKernelSMOption smOption = null;
        if (optionAddr.isNotNull()) {
            smOption = new SceKernelSMOption();
            smOption.read(optionAddr);
        }

        if (sceModule == null) {
            log.warn("sceKernelStopModule - unknown module UID 0x" + Integer.toHexString(uid));
            return ERROR_KERNEL_UNKNOWN_MODULE;
        }

        statusAddr.setValue(0);

        if (sceModule.isFlashModule) {
            // Trying to stop a module loaded from flash0:
            // Shouldn't get here...
            if (HLEModuleManager.getInstance().hasFlash0Module(sceModule.modname)) {
                log.info("IGNORING:sceKernelStopModule HLE module '" + sceModule.modname + "'");
            } else {
                log.warn("IGNORING:sceKernelStopModule flash module '" + sceModule.modname + "'");
            }
            sceModule.stop();
            return 0; // Fake success.
        }

        if (Memory.isAddressGood(sceModule.module_stop_func)) {
            int priority = 0x20;
            if (smOption != null && smOption.priority > 0) {
                priority = smOption.priority;
            } else if (sceModule.module_stop_thread_priority > 0) {
                priority = sceModule.module_stop_thread_priority;
            }

            int stackSize = 0x40000;
            if (smOption != null && smOption.stackSize > 0) {
                stackSize = smOption.stackSize;
            } else if (sceModule.module_stop_thread_stacksize > 0) {
                stackSize = sceModule.module_stop_thread_stacksize;
            }

            int attribute = sceModule.module_stop_thread_attr;
            if (smOption != null) {
                attribute = smOption.attribute;
            }

            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
            SceKernelThreadInfo currentThread = threadMan.getCurrentThread();

            SceKernelThreadInfo thread = threadMan.hleKernelCreateThread("SceModmgrStop",
                    sceModule.module_stop_func, priority,
                    stackSize, attribute, 0);

            thread.moduleid = sceModule.modid;
            // Store the thread exit status into statusAddr when the thread terminates
            thread.exitStatusAddr = statusAddr;
            sceModule.stop();

            // Start the "stop" thread...
            threadMan.hleKernelStartThread(thread, argSize, argp.getAddress(), sceModule.gp_value);

            // ...and wait for its end.
            threadMan.hleKernelWaitThreadEnd(currentThread, thread.uid, TPointer32.NULL, false, false);
        } else if (sceModule.module_stop_func == 0) {
            log.info("sceKernelStopModule - module has no stop function");
            sceModule.stop();
        } else if (sceModule.isModuleStopped()) {
            log.warn("sceKernelStopModule - module already stopped");
            return SceKernelErrors.ERROR_KERNEL_MODULE_ALREADY_STOPPED;
        } else {
            log.warn(String.format("sceKernelStopModule - invalid stop function 0x%08X", sceModule.module_stop_func));
            return -1;
        }

        return 0;
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0x2E0911AA, version = 150, checkInsideInterrupt = true)
    public int sceKernelUnloadModule(int uid) {
        SceModule sceModule = Managers.modules.getModuleByUID(uid);
        if (sceModule == null) {
            log.warn(String.format("sceKernelUnloadModule unknown module UID 0x%X", uid));
            return -1;
        }
        if (sceModule.isModuleStarted() && !sceModule.isModuleStopped()) {
            log.warn(String.format("sceKernelUnloadModule module 0x%X is still running!", uid));
            return SceKernelErrors.ERROR_KERNEL_MODULE_CANNOT_REMOVE;
        }

        sceModule.unload();
        HLEModuleManager.getInstance().UnloadFlash0Module(sceModule);

        return sceModule.modid;
    }

    @HLEFunction(nid = 0xD675EBB8, version = 150, checkInsideInterrupt = true)
    public int sceKernelSelfStopUnloadModule(int exitCode, int argSize, @CanBeNull TPointer argp) {
        SceModule sceModule = Managers.modules.getModuleByUID(getSelfModuleId());
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        SceKernelThreadInfo thread = null;
        sceModule.stop();
        if (Memory.isAddressGood(sceModule.module_stop_func)) {
            // Start the module stop thread function.
            thread = threadMan.hleKernelCreateThread("SceModmgrStop",
                    sceModule.module_stop_func, sceModule.module_stop_thread_priority,
                    sceModule.module_stop_thread_stacksize, sceModule.module_stop_thread_attr, 0);
            thread.moduleid = sceModule.modid;
            // Unload the module when the stop thread will be deleted
            thread.unloadModuleAtDeletion = true;
        } else {
        	// Unload the module immediately
        	sceModule.unload();
        }

        threadMan.hleKernelExitDeleteThread();  // Delete the current thread.
        if (thread != null) {
        	threadMan.hleKernelStartThread(thread, argSize, argp.getAddress(), sceModule.gp_value);
        }

        return 0;
    }

    @HLEFunction(nid = 0x8F2DF740, version = 150, checkInsideInterrupt = true)
    public int sceKernelStopUnloadSelfModuleWithStatus(int exitCode, int argSize, @CanBeNull TPointer argp, @CanBeNull TPointer32 statusAddr, @CanBeNull TPointer optionAddr) {
        SceModule sceModule = Managers.modules.getModuleByUID(getSelfModuleId());

        if (log.isInfoEnabled()) {
        	log.info(String.format("sceKernelStopUnloadSelfModuleWithStatus %s, exitCode=0x%X", sceModule, exitCode));
        }

        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        SceKernelThreadInfo thread = null;
        statusAddr.setValue(0);
        sceModule.stop();
        if (Memory.isAddressGood(sceModule.module_stop_func)) {
            // Start the module stop thread function.
            statusAddr.setValue(0); // TODO set to return value of the thread (when it exits, of course)

            thread = threadMan.hleKernelCreateThread("SceModmgrStop",
                    sceModule.module_stop_func, sceModule.module_stop_thread_priority,
                    sceModule.module_stop_thread_stacksize, sceModule.module_stop_thread_attr, optionAddr.getAddress());
            thread.moduleid = sceModule.modid;
            // Store the thread exit status into statusAddr when the thread terminates
            thread.exitStatusAddr = statusAddr;
            threadMan.getCurrentThread().exitStatus = exitCode; // Set the current thread's exit status.
            // Unload the module when the stop thread will be deleted
            thread.unloadModuleAtDeletion = true;
        } else {
        	// Unload the module immediately
        	sceModule.unload();
        }

        threadMan.hleKernelExitDeleteThread();  // Delete the current thread.
        if (thread != null) {
        	threadMan.hleKernelStartThread(thread, argSize, argp.getAddress(), sceModule.gp_value);
        }

        return 0;
    }

    @HLEFunction(nid = 0xCC1D3699, version = 150, checkInsideInterrupt = true)
    public int sceKernelStopUnloadSelfModule(int argSize, @CanBeNull TPointer argp, @CanBeNull TPointer32 statusAddr, @CanBeNull TPointer optionAddr) {
        SceModule sceModule = Managers.modules.getModuleByUID(getSelfModuleId());

        if (log.isInfoEnabled()) {
        	log.info(String.format("sceKernelStopUnloadSelfModule %s", sceModule));
        }

        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        SceKernelThreadInfo thread = null;
        statusAddr.setValue(0);
    	sceModule.stop();
        if (Memory.isAddressGood(sceModule.module_stop_func)) {
            // Start the module stop thread function.
            thread = threadMan.hleKernelCreateThread("SceModmgrStop",
                    sceModule.module_stop_func, sceModule.module_stop_thread_priority,
                    sceModule.module_stop_thread_stacksize, sceModule.module_stop_thread_attr, optionAddr.getAddress());
            thread.moduleid = sceModule.modid;
            // Store the thread exit status into statusAddr when the thread terminates
            thread.exitStatusAddr = statusAddr;
            // Unload the module when the stop thread will be deleted
            thread.unloadModuleAtDeletion = true;
        } else {
        	// Unload the module immediately
        	sceModule.unload();
        }

        threadMan.hleKernelExitDeleteThread();  // Delete the current thread.
        if (thread != null) {
        	threadMan.hleKernelStartThread(thread, argSize, argp.getAddress(), sceModule.gp_value);
        }

        return 0;
    }

    /**
     * Get a list of module IDs.
     * @param resultBuffer      Buffer to store the module list
     * @param resultBufferSize  Number of bytes in the resultBuffer
     * @param idCountAddr       Returns the number of module ids
     * @return >= 0 on success 
     */
    @HLEFunction(nid = 0x644395E2, version = 150, checkInsideInterrupt = true)
    public int sceKernelGetModuleIdList(TPointer32 resultBuffer, int resultBufferSize, TPointer32 idCountAddr) {
    	int idCount = 0;
    	int resultBufferOffset = 0;
    	for (SceModule module : Managers.modules.values()) {
    		if (!module.isFlashModule && module.isLoaded) {
    			if (resultBufferOffset < resultBufferSize) {
    				resultBuffer.setValue(resultBufferOffset, module.modid);
    				resultBufferOffset += 4;
    			}
    			idCount++;
    		}
    	}
    	idCountAddr.setValue(idCount);

    	return 0;
    }

    @HLEFunction(nid = 0x748CBED9, version = 150, checkInsideInterrupt = true)
    public int sceKernelQueryModuleInfo(int uid, TPointer infoAddr) {
        SceModule sceModule = Managers.modules.getModuleByUID(uid);
        if (sceModule == null) {
            log.warn("sceKernelQueryModuleInfo unknown module UID 0x" + Integer.toHexString(uid));
            return -1;
        }

        SceKernelModuleInfo moduleInfo = new SceKernelModuleInfo();
        moduleInfo.copy(sceModule);
        moduleInfo.write(infoAddr);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceKernelQueryModuleInfo returning %s", Utilities.getMemoryDump(infoAddr.getAddress(), infoAddr.getValue32())));
        }

        return 0;
    }

    @HLEFunction(nid = 0xF0A26395, version = 150, checkInsideInterrupt = true)
    public int sceKernelGetModuleId() {
        int moduleId = getSelfModuleId();

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelGetModuleId returning 0x%X", moduleId));
        }

        return moduleId;
    }

    @HLEFunction(nid = 0xD8B73127, version = 150, checkInsideInterrupt = true)
    public int sceKernelGetModuleIdByAddress(TPointer addr) {
        SceModule module = Managers.modules.getModuleByAddress(addr.getAddress());
        if (module == null) {
            log.warn(String.format("sceKernelGetModuleIdByAddress addr=%s module not found", addr));
            return -1;
        }

        return module.modid;
    }

    @HLEFunction(nid = 0XA1A78C58, version = 150, checkInsideInterrupt = true)
    public int ModuleMgrForKernel_a1a78c58(PspString path, int flags, @CanBeNull TPointer optionAddr) {
		// same as sceKernelLoadModule
        SceKernelLMOption lmOption = null;
        if (optionAddr.isNotNull()) {
            lmOption = new SceKernelLMOption();
            lmOption.read(optionAddr);
            if (log.isInfoEnabled()) {
            	log.info(String.format("ModuleMgrForKernel_a1a78c58 options: %s", lmOption));
            }
        }

        return hleKernelLoadModule(path.getString(), flags, 0, lmOption, false);
	}
}
