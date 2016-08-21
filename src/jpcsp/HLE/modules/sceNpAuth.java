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

import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.THREAD_CALLBACK_USER_DEFINED;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceNpAuthRequestParameter;

import org.apache.log4j.Logger;

public class sceNpAuth extends HLEModule {
    public static Logger log = Modules.getLogger("sceNpAuth");

    private boolean initialized;
    private int npMemSize;     // Memory allocated by the NP utility.
    private int npMaxMemSize;  // Maximum memory used by the NP utility.
    private int npFreeMemSize; // Free memory available to use by the NP utility.
    private SceKernelCallbackInfo npAuthCreateTicketCallback;

	@Override
	public void start() {
		initialized = false;
		super.start();
	}

    protected void checkInitialized() {
    	if (!initialized) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_NPAUTH_NOT_INIT);
    	}
    }

    /**
     * Initialization.
     * 
     * @param poolSize
     * @param stackSize
     * @param threadPriority
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xA1DE86F8, version = 150, checkInsideInterrupt = true)
    public int sceNpAuthInit(int poolSize, int stackSize, int threadPriority) {
        npMemSize = poolSize;
        npMaxMemSize = poolSize / 2;    // Dummy
        npFreeMemSize = poolSize - 16;  // Dummy.

        initialized = true;

        return 0;
    }

    /**
     * Termination.
     * 
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x4EC1F667, version = 150, checkInsideInterrupt = true)
    public int sceNpAuthTerm() {
    	initialized = false;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF4531ADC, version = 150, checkInsideInterrupt = true)
    public int sceNpAuthGetMemoryStat(TPointer32 memStatAddr) {
    	checkInitialized();

    	memStatAddr.setValue(0, npMemSize);
        memStatAddr.setValue(4, npMaxMemSize);
        memStatAddr.setValue(8, npFreeMemSize);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCD86A656, version = 150)
    public int sceNpAuthCreateStartRequest(TPointer paramAddr) {
    	SceNpAuthRequestParameter param = new SceNpAuthRequestParameter();
    	param.read(paramAddr);
    	if (log.isInfoEnabled()) {
    		log.info(String.format("sceNpAuthCreateStartRequest param: %s", param));
    	}

    	if (param.ticketCallback != 0) {
    		int ticketLength = 100;
    		npAuthCreateTicketCallback = Modules.ThreadManForUserModule.hleKernelCreateCallback("sceNpAuthCreateStartRequest", param.ticketCallback, param.callbackArgument);
    		if (Modules.ThreadManForUserModule.hleKernelRegisterCallback(THREAD_CALLBACK_USER_DEFINED, npAuthCreateTicketCallback.uid)) {
    			Modules.ThreadManForUserModule.hleKernelNotifyCallback(THREAD_CALLBACK_USER_DEFINED, npAuthCreateTicketCallback.uid, ticketLength);
    		}
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3F1C1F70, version = 150)
    public int sceNpAuthGetTicket(int id, TPointer buffer, int length) {
    	buffer.clear(length);

    	return 0;
    }
}
