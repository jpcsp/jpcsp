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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.pspSysEventHandler;

public class sceSysEventForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("sceSysEventForKernel");

    @HLEUnimplemented
    @HLEFunction(nid = 0xAEB300AE, version = 150)
    public int sceKernelIsRegisterSysEventHandler() {
    	return 0;
    }

    /**
     * Register a SysEvent handler.
     *
     * @param handler			the handler to register
     * @return					0 on success, < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xCD9E4BB5, version = 150)
    public int sceKernelRegisterSysEventHandler(TPointer handler) {
    	pspSysEventHandler sysEventHandler = new pspSysEventHandler();
    	sysEventHandler.read(handler);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceKernelRegisterSysEventHandler handler: %s", sysEventHandler));
    	}

    	if ("SceFatfsSysEvent".equals(sysEventHandler.name)) {
    		Modules.sceMSstorModule.installDrivers();
    	}

    	return 0;
    }

    /**
     * Unregister a SysEvent handler.
     *
     * @param handler			the handler to unregister
     * @return					0 on success, < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xD7D3FDCD, version = 150)
    public int sceKernelUnregisterSysEventHandler(TPointer handler) {
    	return 0;
    }

    /**
     * Dispatch a SysEvent event.
     *
     * @param eventTypeMask		the event type mask
     * @param eventId			the event id
     * @param eventName			the event name
     * @param param				the pointer to the custom parameters
     * @param resultAddr		the pointer to the result
     * @param breakNonzero		set to 1 to interrupt the calling chain after the first non-zero return
     * @param breakHandler		the pointer to the event handler having interrupted
     * @return					0 on success, < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x36331294, version = 150)
    public int sceKernelSysEventDispatch(int eventTypeMask, int eventId, String eventName, int param, TPointer32 resultAddr, int breakNonzero, int breakHandler) {
    	return 0;
    }

    /**
     * Get the first SysEvent handler (the rest can be found with the linked list).
     *
     * @return					0 on error, handler on success
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x68D55505, version = 150)
    public int sceKernelReferSysEventHandler() {
    	return 0;
    }
}
