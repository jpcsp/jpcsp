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
package jpcsp.HLE.modules500;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelErrors;

@HLELogging
public class LoadExecForUser extends jpcsp.HLE.modules150.LoadExecForUser {
	@HLELogging(level="info")
    @HLEFunction(nid = 0x362A956B, version = 500)
    public int LoadExecForUser_362A956B() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("LoadExecForUser_362A956B registeredExitCallbackUid=0x%X", registeredExitCallbackUid));
    	}

    	SceKernelCallbackInfo callbackInfo = Modules.ThreadManForUserModule.hleKernelReferCallbackStatus(registeredExitCallbackUid);
    	if (callbackInfo == null) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("LoadExecForUser_362A956B registeredExitCallbackUid=0x%x callback not found", registeredExitCallbackUid));
        	}
    		return SceKernelErrors.ERROR_KERNEL_NOT_FOUND_CALLBACK;
    	}
    	int callbackArgument = callbackInfo.callback_arg_addr;
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
