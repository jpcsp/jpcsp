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

import static jpcsp.HLE.Modules.ThreadManForUserModule;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_SUBINTR_ALREADY_REGISTERED;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_KERNEL_SUBINTR_NOT_REGISTERED;

import org.apache.log4j.Logger;

import jpcsp.HLE.AfterCallbackAction;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.pspSysEventHandler;

public class sceSysEventForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("sceSysEventForKernel");
    private TPointer sysEventHandlers;

	@Override
	public void start() {
		sysEventHandlers = TPointer.NULL;

		super.start();
	}

    @HLEFunction(nid = 0xAEB300AE, version = 150)
    public boolean sceKernelIsRegisterSysEventHandler(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = pspSysEventHandler.SIZEOF, usage = Usage.in) TPointer handler) {
    	TPointer current = sysEventHandlers;
    	while (current.isNotNull()) {
    		if (current.equals(handler)) {
    				break;
    		}
    		current = pspSysEventHandler.getNext(current);
    	}

    	return current.isNotNull();
    }

    /**
     * Register a SysEvent handler.
     *
     * @param handler			the handler to register
     * @return					0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xCD9E4BB5, version = 150)
    public int sceKernelRegisterSysEventHandler(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = pspSysEventHandler.SIZEOF, usage = Usage.in) TPointer handler) {
    	pspSysEventHandler sysEventHandler = new pspSysEventHandler();
    	sysEventHandler.read(handler);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceKernelRegisterSysEventHandler handler: %s", sysEventHandler));
    	}

    	if (sceKernelIsRegisterSysEventHandler(handler)) {
    		return ERROR_KERNEL_SUBINTR_ALREADY_REGISTERED;
    	}

    	sysEventHandler.busy = false;
    	sysEventHandler.gp = getProcessor().cpu._gp;
    	sysEventHandler.next = sysEventHandlers;
    	sysEventHandler.write(handler);

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
    @HLEFunction(nid = 0xD7D3FDCD, version = 150)
    public int sceKernelUnregisterSysEventHandler(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = pspSysEventHandler.SIZEOF, usage = Usage.in) TPointer handler) {
    	if (pspSysEventHandler.isBusy(handler)) {
    		return -1;
    	}

    	TPointer current = sysEventHandlers;
    	TPointer previous = TPointer.NULL;

    	while (current.isNotNull()) {
			TPointer next = pspSysEventHandler.getNext(current);
    		if (current.equals(handler)) {
    			if (previous.isNull()) {
    				sysEventHandlers = next;
    			} else {
    				pspSysEventHandler.setNext(previous, next);
    			}
    			break;
    		}

    		previous = current;
    		current = next;
    	}

    	if (current.isNull()) {
    		return ERROR_KERNEL_SUBINTR_NOT_REGISTERED;
    	}

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
    @HLEFunction(nid = 0x36331294, version = 150)
    public int sceKernelSysEventDispatch(int eventTypeMask, int eventId, PspString eventName, int param, TPointer32 resultAddr, int breakNonzero, @CanBeNull @BufferInfo(usage = Usage.out) TPointer32 breakHandler) {
		pspSysEventHandler sysEventHandler = new pspSysEventHandler();
    	TPointer current = sysEventHandlers;

    	int result = 0;
    	while (current.isNotNull()) {
    		if (pspSysEventHandler.isMatchingTypeMask(current, eventTypeMask)) {
    			pspSysEventHandler.setBusy(current, true);
    			sysEventHandler.read(current);
    			AfterCallbackAction afterCallbackAction = new AfterCallbackAction(sysEventHandler.handler);
        		ThreadManForUserModule.executeCallback(sysEventHandler.handler.getAddress(), sysEventHandler.gp, afterCallbackAction, eventId, eventName.getAddress(), param, resultAddr.getAddress());
    			pspSysEventHandler.setBusy(current, false);

    			result = afterCallbackAction.getReturnValue();
    			if (result < 0 && breakNonzero != 0) {
    				breakHandler.setPointer(current);
    				break;
    			}
    			result = 0;
    		}

    		current = pspSysEventHandler.getNext(current);
    	}

    	return result;
    }

    /**
     * Get the first SysEvent handler (the rest can be found with the linked list).
     *
     * @return					0 on error, handler on success
     */
    @HLEFunction(nid = 0x68D55505, version = 150)
    public int sceKernelReferSysEventHandler() {
    	return sysEventHandlers.getAddress();
    }
}
