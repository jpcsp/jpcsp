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

import static jpcsp.HLE.Modules.ModuleMgrForUserModule;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.NIDMapper;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceKernelLoadExecVSHParam;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

public class SystemCtrlForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("SystemCtrlForKernel");

    /**
     * Sets a function to be called just before module_start of a module is gonna be called (useful for patching purposes)
     *
     * @param startModuleHandler - The function, that will receive the module structure before the module is started.
     *
     * @returns - The previous set function (NULL if none);
     * @Note: because only one handler function is handled by HEN, you should
     *        call the previous function in your code.
     *
     * @Example: 
     *
     * STMOD_HANDLER previous = NULL;
     *
     * int OnModuleStart(SceModule2 *mod);
     *
     * void somepointofmycode()
     * {
     *     previous = sctrlHENSetStartModuleHandler(OnModuleStart);
     * }
     *
     * int OnModuleStart(SceModule2 *mod)
     * {
     *     if (strcmp(mod->modname, "vsh_module") == 0)
     *     {
     *         // Do something with vsh module here
     *     }
     *
     *     if (!previous)
     *         return 0;
     *
     *     // Call previous handler
     *
     *     return previous(mod);
     * }
     *
     * @Note2: The above example should be compiled with the flag -fno-pic
     *         in order to avoid problems with gp register that may lead to a crash.
     *
     */
    @HLEFunction(nid = 0x1C90BECB, version = 150)
    public int sctrlHENSetStartModuleHandler(TPointer startModuleHandler) {
    	int previousStartModuleHandler = ModuleMgrForUserModule.getStartModuleHandler();

    	ModuleMgrForUserModule.setStartModuleHandler(startModuleHandler.getAddress());

    	return previousStartModuleHandler;
    }

    /** 
     * Finds a driver 
     * 
     * @param drvname - The name of the driver (without ":" or numbers) 
     * 
     * @returns the driver if found, NULL otherwise 
     * 
     */ 
    @HLEUnimplemented
    @HLEFunction(nid = 0x78E46415, version = 150)
    public int sctrlHENFindDriver(String drvname) {
    	return 0;
    }

    /** 
     * Sets the boot config file for next reboot
     *
     * @param index - The index identifying the file
     *                (0 -> normal bootconf,
     *                 1 -> march33 driver bootconf,
     *                 2 -> np9660 bootcnf,
     *                 3 -> inferno bootconf,
     *                 4 -> inferno vsh mount)
     */
    @HLEFunction(nid = 0xBC939DC1, version = 150)
    public void sctrlSESetBootConfFileIndex(int index) {
    	// This information can simply be ignored here
    }

    /**
     * Sets the current umd file (kernel only)
     *
     * @param file - The umd file
     */
    @HLEFunction(nid = 0x5A35C948, version = 150)
    public void sctrlSESetUmdFile(PspString file) {
    	String fileName = file.getString();

    	// Map e.g. "ms0:/ISO/cube.iso" to "ms0/ISO/cube.iso"
    	int deviceNameIndex = fileName.indexOf(':');
    	if (deviceNameIndex > 0) {
    		String deviceName = fileName.substring(0, deviceNameIndex);
    		String directoryMapping = Settings.getInstance().getDirectoryMapping(deviceName);
    		if (directoryMapping != null) {
    			int fileNameIndex = deviceNameIndex + 1;
    			if (fileName.charAt(fileNameIndex) == '/') {
    				fileNameIndex++;
    			}
    			fileName = directoryMapping + fileName.substring(fileNameIndex);
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sctrlSESetUmdFile local fileName='%s'", fileName));
    			}
    		}
    	}

    	try {
			UmdIsoReader iso = new UmdIsoReader(fileName);

			Modules.IoFileMgrForUserModule.setfilepath("disc0/");
            Modules.IoFileMgrForUserModule.setIsoReader(iso);
            Modules.sceUmdUserModule.setIsoReader(iso);
		} catch (FileNotFoundException e) {
			log.error("sctrlSESetUmdFile", e);
		} catch (IOException e) {
			log.error("sctrlSESetUmdFile", e);
		}
    }

    /**
     * Executes a new executable with the specified apitype
     *
     * @param apitype - The apitype
     * @param filename - The file to execute.
     * @param param - Pointer to a ::SceKernelLoadExecVSHParam structure, or NULL.
     *
     * @returns < 0 on some errors. 
     */
    @HLEFunction(nid = 0x2D10FB28, version = 150)
    public int sctrlKernelLoadExecVSHWithApitype(int apiType, PspString filename, TPointer param) {
    	SceKernelLoadExecVSHParam loadExecVSHParam = new SceKernelLoadExecVSHParam();
    	loadExecVSHParam.read(param);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sctrlKernelLoadExecVSHWithApitype param: %s", loadExecVSHParam));
    		if (loadExecVSHParam.args > 0) {
    			log.debug(String.format("sctrlKernelLoadExecVSHWithApitype argp: %s", Utilities.getMemoryDump(loadExecVSHParam.argp, loadExecVSHParam.args)));
    		}
    		if (loadExecVSHParam.vshmainArgsSize > 0) {
    			log.debug(String.format("sctrlKernelLoadExecVSHWithApitype vshmainArgs: %s", Utilities.getMemoryDump(loadExecVSHParam.vshmainArgs, loadExecVSHParam.vshmainArgsSize)));
    		}
    	}

    	String realFileName = filename.getString();
    	if (loadExecVSHParam.args > 0 && loadExecVSHParam.argp != 0) {
    		String arg = Utilities.readStringNZ(loadExecVSHParam.argp, loadExecVSHParam.args);
    		if (arg.startsWith("disc0:")) {
    			Modules.IoFileMgrForUserModule.setfilepath("disc0/");

    			// When filename == "ms0:/ISO/cube.iso" and arg == "disc0:/PSP_GAME/SYSDIR/EBOOT.BIN",
    			// then use "disc0:/PSP_GAME/SYSDIR/EBOOT.BIN" as the real file name
    			if (realFileName.startsWith("ms0:")) {
    				// Ignore any arguments passed after the real file name
    				int argIndex = arg.indexOf(' ');
    				if (argIndex < 0) {
    					realFileName = arg;
    				} else {
    					realFileName = arg.substring(0, argIndex);
    				}
    			}
    		} else if (arg.startsWith("ms0:")) {
    	    	int dirIndex = arg.lastIndexOf('/');
    	    	if (dirIndex >= 0) {
    	    		Modules.IoFileMgrForUserModule.setfilepath(Settings.getInstance().getDirectoryMapping("ms0") + arg.substring(4, dirIndex));
    	    	}
    		}
    	}

    	return Modules.LoadExecForUserModule.hleKernelLoadExec(new PspString(realFileName), loadExecVSHParam.args, loadExecVSHParam.argp);
    }

    /**
     * Sets the SE configuration
     *
     * @param config - pointer to a SEConfig structure that has the SE configuration to set
     * @returns 0 on success
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x1DDDAD0C, version = 150)
    public int sctrlSESetConfig(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=72, usage=Usage.in) TPointer configAddr) {
    	return 0;
    }

    /**
     * Gets the SE configuration
     *
     * @param config - pointer to a SEConfig structure that receives the SE configuration
     * @returns 0 on success
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x16C3B7EE, version = 150)
    public int sctrlSEGetConfig(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=72, usage=Usage.out) TPointer configAddr) {
    	return 0;
    }

    @HLEFunction(nid = 0x159AF5CC, version = 150)
    public int sctrlHENFindFunction(@CanBeNull PspString szMod, @CanBeNull PspString szLib, int nid) {
    	return NIDMapper.getInstance().getAddressByNid(nid, szMod.getString());
    }
}
