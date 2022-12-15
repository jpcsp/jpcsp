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
package jpcsp.HLE.modules;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.io.IOException;
import java.nio.ByteBuffer;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE;
import static jpcsp.HLE.Modules.scePspNpDrm_userModule;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_LOADEXEC_FILENAME;
import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;
import jpcsp.Loader;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.Modules;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.xmb.XmbIsoVirtualFile;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class LoadExecForUser extends HLEModule {
    public static Logger log = Modules.getLogger("LoadExecForUser");
    protected int registeredExitCallbackUid;
    protected static final String encryptedBootPath = "disc0:/PSP_GAME/SYSDIR/EBOOT.BIN";
    protected static final String unencryptedBootPath = "disc0:/PSP_GAME/SYSDIR/BOOT.BIN";

    public void triggerExitCallback() {
        Modules.ThreadManForUserModule.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_EXIT, 0);
    }

    public int hleKernelLoadExec(ByteBuffer moduleBuffer, int argSize, int argAddr, String moduleFileName, UmdIsoReader iso) {
        byte[] arguments = null;
        if (argSize > 0) {
            // Save the memory content for the arguments because
            // the memory would be overwritten by the loading of the new module.
            arguments = new byte[argSize];
            IMemoryReader memoryReader = MemoryReader.getMemoryReader(argAddr, argSize, 1);
            for (int i = 0; i < argSize; i++) {
            	arguments[i] = (byte) memoryReader.readNext();
            }
        }

        // Flush system memory to mimic a real PSP reset.
        Modules.SysMemUserForUserModule.reset();

        try {
            if (moduleBuffer != null) {
                SceModule module = Emulator.getInstance().load(moduleFileName, moduleBuffer, true, Modules.ModuleMgrForUserModule.isSignChecked(moduleFileName), null);
                Emulator.getClock().resume();

                // After a sceKernelLoadExec, host0: is relative to the directory where
                // the loaded file (prx) was located.
                // E.g.:
                //  after
                //    sceKernelLoadExec("disc0:/PSP_GAME/USRDIR/A.PRX")
                //  the following file access
                //    sceIoOpen("host0:B")
                //  is actually referencing the file
                //    disc0:/PSP_GAME/USRDIR/B
                if (moduleFileName != null) {
	                int pathIndex = moduleFileName.lastIndexOf("/");
	                if (pathIndex >= 0) {
	                	Modules.IoFileMgrForUserModule.setHost0Path(moduleFileName.substring(0, pathIndex + 1));
	                }
                }

                if ((module.fileFormat & Loader.FORMAT_ELF) != Loader.FORMAT_ELF) {
                    log.warn("sceKernelLoadExec - failed, target is not an ELF");
                    throw new SceKernelErrorException(ERROR_KERNEL_ILLEGAL_LOADEXEC_FILENAME);
                }

            	// Set the given arguments to the root thread.
            	// Do not pass the file name as first parameter (tested on PSP).
            	SceKernelThreadInfo rootThread = Modules.ThreadManForUserModule.getRootThread(module);
            	Modules.ThreadManForUserModule.hleKernelSetThreadArguments(rootThread, arguments, argSize);

            	// The memory model (32MB / 64MB) could have been changed, update the RuntimeContext
            	RuntimeContext.updateMemory();

            	if (iso != null) {
            		Modules.IoFileMgrForUserModule.setIsoReader(iso);
            		Modules.sceUmdUserModule.setIsoReader(iso);
            	}
            }
        } catch (GeneralJpcspException e) {
            log.error("General Error", e);
            Emulator.PauseEmu();
        } catch (IOException e) {
            log.error(String.format("sceKernelLoadExec - Error while loading module '%s'", moduleFileName), e);
            return ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE;
        }

        return 0;
    }

    public int hleKernelLoadExec(PspString filename, int argSize, int argAddr) {
        String name = filename.getString();

        // The PSP is replacing a loadexec of disc0:/PSP_GAME/SYSDIR/BOOT.BIN with EBOOT.BIN
        if (name.equals(unencryptedBootPath)) {
    		log.info(String.format("sceKernelLoadExec '%s' replaced by '%s'", name, encryptedBootPath));
    		name = encryptedBootPath;
        }

        ByteBuffer moduleBuffer = null;

        IVirtualFile vFile = Modules.IoFileMgrForUserModule.getVirtualFile(name, IoFileMgrForUser.PSP_O_RDONLY, 0);
        UmdIsoReader iso = null;
    	if (vFile instanceof XmbIsoVirtualFile) {
    		try {
	    		IVirtualFile vFileLoadExec = ((XmbIsoVirtualFile) vFile).ioReadForLoadExec();
	    		if (vFileLoadExec != null) {
	    			iso = ((XmbIsoVirtualFile) vFile).getIsoReader();
	
	        		vFile.ioClose();
	    			vFile = vFileLoadExec;
	    		}
    		} catch (IOException e) {
    			log.debug("hleKernelLoadExec", e);
    		}
    	}

    	if (vFile != null) {
        	byte[] moduleBytes = Utilities.readCompleteFile(vFile);
        	vFile.ioClose();
        	if (moduleBytes != null) {
        		moduleBuffer = ByteBuffer.wrap(moduleBytes);
        	}
        } else {
	        SeekableDataInput moduleInput = Modules.IoFileMgrForUserModule.getFile(name, IoFileMgrForUser.PSP_O_RDONLY);
	        if (moduleInput != null) {
				try {
					byte[] moduleBytes = new byte[(int) moduleInput.length()];
		            moduleInput.readFully(moduleBytes);
		            moduleInput.close();
		            moduleBuffer = ByteBuffer.wrap(moduleBytes);
				} catch (IOException e) {
		            log.error(String.format("sceKernelLoadExec - Error while loading module '%s'", name), e);
		            return ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE;
				}
	        }
        }

    	return hleKernelLoadExec(moduleBuffer, argSize, argAddr, name, iso);
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0xBD2F1094, version = 150, checkInsideInterrupt = true)
    public int sceKernelLoadExec(PspString filename, @CanBeNull TPointer32 optionAddr) {
        int argSize = 0;
        int argAddr = 0;
        if (optionAddr.isNotNull()) {
            int optSize = optionAddr.getValue(0);      // Size of the option struct.
            if (optSize >= 16) {
	            argSize = optionAddr.getValue(4);      // Size of memory required for arguments.
	            argAddr = optionAddr.getValue(8);      // Arguments (memory area of size argSize).
	            int keyAddr = optionAddr.getValue(12); // Pointer to an encryption key (may not be used).

	            if (log.isDebugEnabled()) {
	            	log.debug(String.format("sceKernelLoadExec params: optSize=%d, argSize=%d, argAddr=0x%08X, keyAddr=0x%08X: %s", optSize, argSize, argAddr, keyAddr, Utilities.getMemoryDump(argAddr, argSize)));
	            }

            }
        }

        return hleKernelLoadExec(filename, argSize, argAddr);
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0x2AC9954B, version = 150, checkInsideInterrupt = true)
    public int sceKernelExitGameWithStatus(int status) {
        Emulator.PauseEmuWithStatus(status);
        RuntimeContext.reset();
        Modules.ThreadManForUserModule.stop();

        return 0;
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0x05572A5F, version = 150, checkInsideInterrupt = true)
    public int sceKernelExitGame() {
    	// When running a PSP official updater, perform a PSP reboot
    	if (Emulator.getInstance().isPspOfficialUpdater()) {
    		return Modules.scePowerModule.scePowerRequestColdReset(0);
    	}

        Emulator.PauseEmu();
        RuntimeContext.reset();
        Modules.ThreadManForUserModule.stop();

        return 0;
    }

    @HLEFunction(nid = 0x4AC57943, version = 150, checkInsideInterrupt = true)
    public int sceKernelRegisterExitCallback(int uid) {
        if (Modules.ThreadManForUserModule.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_EXIT, uid)) {
            registeredExitCallbackUid = uid;
        }

        return 0;
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0x362A956B, version = 500)
    public int LoadExecForUser_362A956B() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("LoadExecForUser_362A956B registeredExitCallbackUid=0x%X", registeredExitCallbackUid));
    	}

    	SceKernelCallbackInfo callbackInfo = Modules.ThreadManForUserModule.getCallbackInfo(registeredExitCallbackUid);
    	if (callbackInfo == null) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("LoadExecForUser_362A956B registeredExitCallbackUid=0x%x callback not found", registeredExitCallbackUid));
        	}
    		return SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK;
    	}
    	int callbackArgument = callbackInfo.getCallbackArgument();
    	if (!Memory.isAddressGood(callbackArgument)) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("LoadExecForUser_362A956B invalid address for callbackArgument=0x%08X", callbackArgument));
        	}
    		return SceKernelErrors.ERROR_KERNEL_ILLEGAL_ADDR;
    	}

    	Memory mem = Processor.memory;

    	int unknown1 = mem.read32(callbackArgument - 8);
    	if (unknown1 < 0 || unknown1 >= 4) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("LoadExecForUser_362A956B invalid value unknown1=0x%08X", unknown1));
        	}
    		return SceKernelErrors.ERROR_KERNEL_ILLEGAL_ARGUMENT;
    	}

    	int parameterArea = mem.read32(callbackArgument - 4);
    	if (!Memory.isAddressGood(parameterArea)) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("LoadExecForUser_362A956B invalid address for parameterArea=0x%08X", parameterArea));
        	}
    		return SceKernelErrors.ERROR_KERNEL_ILLEGAL_ADDR;
    	}

    	int size = mem.read32(parameterArea);
    	if (size < 12) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("LoadExecForUser_362A956B invalid parameter area size %d", size));
        	}
    		return SceKernelErrors.ERROR_KERNEL_ILLEGAL_SIZE;
    	}

    	mem.write32(parameterArea + 4, 0);
    	mem.write32(parameterArea + 8, -1);

    	return 0;
    }

    @HLEFunction(nid = 0xAA5FC85B, version = 150, checkInsideInterrupt = true)
    @HLEFunction(nid = 0x8ADA38D3, version = 150, checkInsideInterrupt = true)
    public int sceKernelLoadExecNpDrm(PspString fileName, @CanBeNull TPointer optionAddr) {
        // Flush system memory to mimic a real PSP reset.
        Modules.SysMemUserForUserModule.reset();

        byte[] key = null;
        if (optionAddr.isNotNull()) {
            int optSize = optionAddr.getValue32(0);  // Size of the option struct.
            int argSize = optionAddr.getValue32(4);  // Number of args (strings).
            int argAddr = optionAddr.getValue32(8);  // Pointer to a list of strings.
            TPointer keyAddr = optionAddr.getPointer(12); // Pointer to an encryption key.

            if (keyAddr.isNotNull()) {
            	key = keyAddr.getArray8(16);
            }

            if (log.isDebugEnabled()) {
                log.debug(String.format("sceKernelLoadExecNpDrm (params: optSize=%d, argSize=%d, argAddr=0x%08X, keyAddr=%s)", optSize, argSize, argAddr, keyAddr));
            }
        }

        // SPRX modules can't be decrypted yet.
        if (scePspNpDrm_userModule.isDLCDecryptionEnabled()) {
            log.warn(String.format("sceKernelLoadExecNpDrm detected encrypted DLC module: %s", fileName.getString()));
            return SceKernelErrors.ERROR_NPDRM_INVALID_PERM;
        }

        int result;
        try {
            SeekableDataInput moduleInput = Modules.IoFileMgrForUserModule.getFile(fileName.getString(), IoFileMgrForUser.PSP_O_RDONLY);
            if (moduleInput != null) {
                byte[] moduleBytes = new byte[(int) moduleInput.length()];
                moduleInput.readFully(moduleBytes);
                moduleInput.close();
                ByteBuffer moduleBuffer = ByteBuffer.wrap(moduleBytes);

                SceModule module = Emulator.getInstance().load(fileName.getString(), moduleBuffer, true, Modules.ModuleMgrForUserModule.isSignChecked(fileName.getString()), key);
                Emulator.getClock().resume();

                if ((module.fileFormat & Loader.FORMAT_ELF) == Loader.FORMAT_ELF) {
                    result = 0;
                } else {
                    log.warn("sceKernelLoadExecNpDrm - failed, target is not an ELF");
                    result = SceKernelErrors.ERROR_KERNEL_ILLEGAL_LOADEXEC_FILENAME;
                }
            } else {
                result = SceKernelErrors.ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE;
            }
        } catch (GeneralJpcspException e) {
            log.error("sceKernelLoadExecNpDrm", e);
            result = SceKernelErrors.ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE;
        } catch (IOException e) {
            log.error(String.format("sceKernelLoadExecNpDrm - Error while loading module '%s'", fileName), e);
            result = SceKernelErrors.ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE;
        }

        return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD1FB50DC, version = 500)
    public int LoadExecForUser_D1FB50DC() {
    	return 0;
    }
}