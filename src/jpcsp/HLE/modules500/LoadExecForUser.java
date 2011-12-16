package jpcsp.HLE.modules500;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelErrors;

public class LoadExecForUser extends jpcsp.HLE.modules150.LoadExecForUser {
    @HLEFunction(nid = 0x362A956B, version = 500)
    public int LoadExecForUser_362A956B() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("LoadExecForUser_362A956B registeredExitCallbackUid=0x%x", registeredExitCallbackUid));
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
