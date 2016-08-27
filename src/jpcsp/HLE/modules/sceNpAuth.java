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

import java.nio.charset.Charset;

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
import jpcsp.HLE.kernel.types.SceNpTicket;
import jpcsp.HLE.kernel.types.SceNpTicket.TicketParam;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceNpAuth extends HLEModule {
    public static Logger log = Modules.getLogger("sceNpAuth");
    public static boolean useDummyTicket = true;

    public final static int STATUS_ACCOUNT_SUSPENDED = 0x80;
    public final static int STATUS_ACCOUNT_CHAT_RESTRICTED = 0x100;
    public final static int STATUS_ACCOUNT_PARENTAL_CONTROL_ENABLED = 0x200;

    private boolean initialized;
    private int npMemSize;     // Memory allocated by the NP utility.
    private int npMaxMemSize;  // Maximum memory used by the NP utility.
    private int npFreeMemSize; // Free memory available to use by the NP utility.
    private SceKernelCallbackInfo npAuthCreateTicketCallback;
    private String serviceId;

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

    private void addTicketParam(SceNpTicket ticket, int type, String value, int length) {
    	byte[] stringBytes = value.getBytes(Charset.forName("ASCII"));
    	byte[] bytes = new byte[length];
    	System.arraycopy(stringBytes, 0, bytes, 0, Math.min(length, stringBytes.length));
    	ticket.parameters.add(new TicketParam(type, bytes));
    }

    private void addTicketParam(SceNpTicket ticket, String value, int length) {
    	addTicketParam(ticket, TicketParam.PARAM_TYPE_STRING_ASCII, value, length);
    }

    private void addTicketParam(SceNpTicket ticket, int value) {
    	byte bytes[] = new byte[4];
    	Utilities.writeUnaligned32(bytes, 0, Utilities.endianSwap32(value));
    	ticket.parameters.add(new TicketParam(TicketParam.PARAM_TYPE_INT, bytes));
    }

    private void addTicketParam(SceNpTicket ticket, long time) {
    	byte bytes[] = new byte[8];
    	Utilities.writeUnaligned32(bytes, 0, Utilities.endianSwap32((int) (time >> 32)));
    	Utilities.writeUnaligned32(bytes, 4, Utilities.endianSwap32((int) time));
    	ticket.parameters.add(new TicketParam(TicketParam.PARAM_TYPE_DATE, bytes));
    }

    private void addTicketParam(SceNpTicket ticket, byte[] value) {
    	ticket.parameters.add(new TicketParam(TicketParam.PARAM_TYPE_UNKNOWN, value));
    }

    private void addTicketParam(SceNpTicket ticket) {
    	ticket.parameters.add(new TicketParam(TicketParam.PARAM_TYPE_NULL, new byte[0]));
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

    	serviceId = param.serviceId;

    	if (param.ticketCallback != 0) {
    		int ticketLength = 248;
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
    	if (useDummyTicket) {
    		SceNpTicket ticket = new SceNpTicket();
    		ticket.version = 0x00000121;
    		ticket.size = 0xF0;
    		addTicketParam(ticket, "XXXXXXXXXXXXXXXXXXXX", 20);
    		addTicketParam(ticket, 0);
    		long now = System.currentTimeMillis();
    		addTicketParam(ticket, now);
    		addTicketParam(ticket, now + 10 * 60 * 1000); // now + 10 minutes
    		addTicketParam(ticket, new byte[8]);
    		addTicketParam(ticket, TicketParam.PARAM_TYPE_STRING, "DummyOnlineID", 32);
    		addTicketParam(ticket, "gb", 4);
    		addTicketParam(ticket, TicketParam.PARAM_TYPE_STRING, "XX", 4);
    		addTicketParam(ticket, serviceId, 24);
			int status = 0;
			if (Modules.sceNpModule.parentalControl == sceNp.PARENTAL_CONTROL_ENABLED) {
				status |= STATUS_ACCOUNT_PARENTAL_CONTROL_ENABLED;
			}
			status |= (Modules.sceNpModule.userAge & 0x7F) << 24;
    		addTicketParam(ticket, status);
    		addTicketParam(ticket);
    		addTicketParam(ticket);
    		ticket.unknownBytes = new byte[72];
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceNpAuthGetTicket returning dummy ticket: %s", ticket));
    		}
    		ticket.write(buffer);
    	} else {
        	buffer.clear(length);
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6900F084, version = 150)
    public int sceNpAuthGetEntitlementById(TPointer ticketBuffer, int ticketLength, int unknown1, int unknown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x72BB0467, version = 150)
    public int sceNpAuthDestroyRequest(int id) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD99455DD, version = 150)
    public int sceNpAuthAbortRequest(int id) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5A3CB57A, version = 150)
    public int sceNpAuthGetTicketParam(TPointer ticketBuffer, int ticketLength, int paramNumber, TPointer buffer) {
    	// This clear is always done, even when an error is returned
    	buffer.clear(256);

    	if (paramNumber < 0 || paramNumber >= SceNpTicket.NUMBER_PARAMETERS) {
    		return SceKernelErrors.ERROR_NP_MANAGER_INVALID_ARGUMENT;
    	}

    	SceNpTicket ticket = new SceNpTicket();
    	ticket.read(ticketBuffer);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNpAuthGetTicketParam ticket: %s", ticket));
    	}

    	TicketParam ticketParam = ticket.parameters.get(paramNumber);
    	ticketParam.writeForPSP(buffer);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x75FB0AE3, version = 150)
    public int sceNpAuthGetEntitlementIdList() {
    	return 0;
    }
}
