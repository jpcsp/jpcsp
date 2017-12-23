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

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceKernelLoadExecVSHParam;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;
import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContext;

public class LoadExecForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("LoadExecForKernel");

    @HLEFunction(nid = 0xA3D5E142, version = 150)
    public int sceKernelExitVSHVSH(@CanBeNull TPointer param) {
		// when called in game mode it will have the same effect that sceKernelExitGame 
		if (param.isNotNull()) {
			log.info(String.format("sceKernelExitVSHVSH param=%s", Utilities.getMemoryDump(param.getAddress(), 36)));
		}
		Emulator.PauseEmu();
		RuntimeContext.reset();
		Modules.ThreadManForUserModule.stop();
		return 0;
	}

    @HLEFunction(nid = 0x6D302D3D, version = 150)
    public int sceKernelExitVSHKernel(@BufferInfo(lengthInfo=LengthInfo.variableLength, usage=Usage.in) @CanBeNull TPointer param) {
    	SceKernelLoadExecVSHParam loadExecVSHParam = new SceKernelLoadExecVSHParam();
    	loadExecVSHParam.read(param);

    	//  Test in real PSP in  "Hatsune Miku Project Diva Extend" chinese patched version, same effect as sceKernelExitGame
		if (param.isNotNull()) {
			log.info(String.format("sceKernelExitVSHKernel param=%s", loadExecVSHParam));
		}
		Emulator.PauseEmu();
		RuntimeContext.reset();
		Modules.ThreadManForUserModule.stop();
		return 0;
	}

    @HLEFunction(nid = 0xC3474C2A, version = 660)
    public int sceKernelExitVSHKernel_660(@BufferInfo(lengthInfo=LengthInfo.variableLength, usage=Usage.in) @CanBeNull TPointer param) {
    	return sceKernelExitVSHKernel(param);
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0x28D0D249, version = 150)
    public int sceKernelLoadExecVSHMs2(PspString filename, TPointer param) {
    	SceKernelLoadExecVSHParam loadExecVSHParam = new SceKernelLoadExecVSHParam();
    	loadExecVSHParam.read(param);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceKernelLoadExecVSHMs2 param: %s", loadExecVSHParam));
    		if (loadExecVSHParam.args > 0) {
    			log.debug(String.format("sceKernelLoadExecVSHMs2 argp: %s", Utilities.getMemoryDump(loadExecVSHParam.argp, loadExecVSHParam.args)));
    		}
    		if (loadExecVSHParam.vshmainArgsSize > 0) {
    			log.debug(String.format("sceKernelLoadExecVSHMs2 vshmainArgs: %s", Utilities.getMemoryDump(loadExecVSHParam.vshmainArgs, loadExecVSHParam.vshmainArgsSize)));
    		}
    	}

    	if (loadExecVSHParam.args > 0 && loadExecVSHParam.argp != 0) {
    		String arg = Utilities.readStringNZ(loadExecVSHParam.argp, loadExecVSHParam.args);
    		if (arg.startsWith("disc0:")) {
    			Modules.IoFileMgrForUserModule.setfilepath("disc0/");
    		} else if (arg.startsWith("ms0:")) {
    	    	int dirIndex = arg.lastIndexOf('/');
    	    	if (dirIndex >= 0) {
    	    		Modules.IoFileMgrForUserModule.setfilepath(Settings.getInstance().getDirectoryMapping("ms0") + arg.substring(4, dirIndex));
    	    	}
    		}
    	}

    	return Modules.LoadExecForUserModule.hleKernelLoadExec(filename, loadExecVSHParam.args, loadExecVSHParam.argp);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x08F7166C, version = 660, checkInsideInterrupt = true)
    public int sceKernelExitVSHVSH_660(TPointer param) {
    	SceKernelLoadExecVSHParam loadExecVSHParam = new SceKernelLoadExecVSHParam();
    	loadExecVSHParam.read(param);

    	if (param.isNotNull()) {
			log.info(String.format("sceKernelExitVSHVSH_660 param=%s", loadExecVSHParam));
		}

    	return 0;
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0xD940C83C, version = 660)
    public int sceKernelLoadExecVSHMs2_660(PspString filename, TPointer param) {
    	return sceKernelLoadExecVSHMs2(filename, param);
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0xF9CFCF2F, version = 660)
    public int sceKernelLoadExec_F9CFCF2F(PspString filename, TPointer param) {
    	return sceKernelLoadExecVSHMs2(filename, param);
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0xD8320A28, version = 660)
    public int sceKernelLoadExecVSHDisc_660(PspString filename, TPointer param) {
    	return sceKernelLoadExecVSHMs2(filename, param);
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0xBEF585EC, version = 150)
    public int sceKernelLoadExecBufferVSHUsbWlan(int bufferSize, TPointer bufferAddr, TPointer param) {
    	SceKernelLoadExecVSHParam loadExecParam = new SceKernelLoadExecVSHParam();
    	loadExecParam.read(param);

    	int argSize = 0;
    	int argAddr = 0;
    	if (param.isNotNull()) {
    		argSize = loadExecParam.args;
    		argAddr = loadExecParam.argp;
			log.info(String.format("sceKernelLoadExecBufferVSHUsbWlan param=%s", loadExecParam));
		}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceKernelLoadExecBufferVSHUsbWlan buffAddr: %s", Utilities.getMemoryDump(bufferAddr.getAddress(), Math.min(bufferSize, 1024))));
    	}

    	byte[] moduleBytes = bufferAddr.getArray8(bufferSize);
    	ByteBuffer moduleBuffer = ByteBuffer.wrap(moduleBytes);

    	return Modules.LoadExecForUserModule.hleKernelLoadExec(moduleBuffer, argSize, argAddr, null, null);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x11412288, version = 150)
    public int sceKernelLoadExec_11412288(TPointer callback) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA5ECA6E3, version = 660)
    public int sceKernelLoadExec_11412288_660(TPointer callback) {
    	return sceKernelLoadExec_11412288(callback);
    }
}
