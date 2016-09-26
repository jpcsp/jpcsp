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
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer32;

import java.io.IOException;
import java.nio.ByteBuffer;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_ILLEGAL_LOADEXEC_FILENAME;
import jpcsp.Emulator;
import jpcsp.GeneralJpcspException;
import jpcsp.Loader;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.filesystems.SeekableDataInput;
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

    public int hleKernelLoadExec(PspString filename, int argSize, int argAddr) {
        String name = filename.getString();

        // The PSP is replacing a loadexec of disc0:/PSP_GAME/SYSDIR/BOOT.BIN with EBOOT.BIN
        if (name.equals(unencryptedBootPath)) {
    		log.info(String.format("sceKernelLoadExec '%s' replaced by '%s'", name, encryptedBootPath));
    		name = encryptedBootPath;
        }

        // Flush system memory to mimic a real PSP reset.
        Modules.SysMemUserForUserModule.reset();

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

        try {
            SeekableDataInput moduleInput = Modules.IoFileMgrForUserModule.getFile(name, IoFileMgrForUser.PSP_O_RDONLY);
            if (moduleInput != null) {
                byte[] moduleBytes = new byte[(int) moduleInput.length()];
                moduleInput.readFully(moduleBytes);
                moduleInput.close();
                ByteBuffer moduleBuffer = ByteBuffer.wrap(moduleBytes);

                SceModule module = Emulator.getInstance().load(name, moduleBuffer, true);
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
                int pathIndex = name.lastIndexOf("/");
                if (pathIndex >= 0) {
                	Modules.IoFileMgrForUserModule.setHost0Path(name.substring(0, pathIndex + 1));
                }

                if ((module.fileFormat & Loader.FORMAT_ELF) != Loader.FORMAT_ELF) {
                    log.warn("sceKernelLoadExec - failed, target is not an ELF");
                    throw new SceKernelErrorException(ERROR_KERNEL_ILLEGAL_LOADEXEC_FILENAME);
                }

            	// Set the given arguments to the root thread.
            	// Do not pass the file name as first parameter (tested on PSP).
            	SceKernelThreadInfo rootThread = Modules.ThreadManForUserModule.getCurrentThread();
            	Modules.ThreadManForUserModule.hleKernelSetThreadArguments(rootThread, arguments, argSize);

            	// The memory model (32MB / 64MB) could have been changed, update the RuntimeContext
            	RuntimeContext.updateMemory();
            }
        } catch (GeneralJpcspException e) {
            log.error("General Error", e);
            Emulator.PauseEmu();
        } catch (IOException e) {
            log.error(String.format("sceKernelLoadExec - Error while loading module '%s'", name), e);
            return ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE;
        }

        return 0;
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
}