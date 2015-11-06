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

import org.apache.log4j.Logger;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceKernelLoadExecVSHParam;
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
    public int sceKernelExitVSHKernel(@CanBeNull TPointer param) {
		//  Test in real PSP in  "Hatsune Miku Project Diva Extend" chinese patched version,same effect that sceKernelExitGame
		if (param.isNotNull()) {
			log.info(String.format("sceKernelExitVSHKernel param=%s", Utilities.getMemoryDump(param.getAddress(), 36)));
		}
		Emulator.PauseEmu();
		RuntimeContext.reset();
		Modules.ThreadManForUserModule.stop();
		return 0;
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

    	return Modules.LoadExecForUserModule.hleKernelLoadExec(filename, loadExecVSHParam.args, loadExecVSHParam.argp);
    }
}
