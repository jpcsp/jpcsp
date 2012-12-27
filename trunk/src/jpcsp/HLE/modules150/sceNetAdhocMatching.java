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
package jpcsp.HLE.modules150;

import static jpcsp.Allegrex.Common._s0;
import static jpcsp.HLE.modules150.sceNetAdhocctl.fillNextPointersInLinkedList;
import static jpcsp.util.Utilities.writeBytes;

import java.util.HashMap;
import java.util.List;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.Processor;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.adhoc.MatchingObject;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

@HLELogging
public class sceNetAdhocMatching extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetAdhocMatching");
    protected HashMap<Integer, MatchingObject> matchingObjects;
    public static final int loopThreadRegisterArgument = _s0; // $s0 is preserved across calls
    private boolean isInitialized;

    /**
     * Matching events used in pspAdhocMatchingCallback
     */
    /** Hello event. optdata contains data if optlen > 0. */
    public static final int PSP_ADHOC_MATCHING_EVENT_HELLO = 1;
    /** Join request. optdata contains data if optlen > 0. */
    public static final int PSP_ADHOC_MATCHING_EVENT_JOIN = 2;
    /** Target left matching. */
    public static final int PSP_ADHOC_MATCHING_EVENT_LEFT = 3;
    /** Join request rejected. */
    public static final int PSP_ADHOC_MATCHING_EVENT_REJECT = 4;
    /** Join request cancelled. */
    public static final int PSP_ADHOC_MATCHING_EVENT_CANCEL = 5;
    /** Join request accepted. optdata contains data if optlen > 0. */
    public static final int PSP_ADHOC_MATCHING_EVENT_ACCEPT = 6;
    /** Matching is complete. */
    public static final int PSP_ADHOC_MATCHING_EVENT_COMPLETE = 7;
    /** Ping timeout event. */
    public static final int PSP_ADHOC_MATCHING_EVENT_TIMEOUT = 8;
    /** Error event. */
    public static final int PSP_ADHOC_MATCHING_EVENT_ERROR = 9;
    /** Peer disconnect event. */
    public static final int PSP_ADHOC_MATCHING_EVENT_DISCONNECT = 10;
    /** Data received event. optdata contains data if optlen > 0. */
    public static final int PSP_ADHOC_MATCHING_EVENT_DATA = 11;
    /** Data acknowledged event. */
    public static final int PSP_ADHOC_MATCHING_EVENT_DATA_CONFIRM = 12;
    /** Data timeout event. */
    public static final int PSP_ADHOC_MATCHING_EVENT_DATA_TIMEOUT = 13;

    /** Internal ping message. */
    public static final int PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING = 100;

    /**
     * Matching modes used in sceNetAdhocMatchingCreate
     */
    /** Host */
    public static final int PSP_ADHOC_MATCHING_MODE_HOST = 1;
    /** Client */
    public static final int PSP_ADHOC_MATCHING_MODE_CLIENT = 2;
    /** Peer to peer */
    public static final int PSP_ADHOC_MATCHING_MODE_PTP = 3;

    @Override
    public String getName() {
        return "sceNetAdhocMatching";
    }

	@Override
	public void start() {
		matchingObjects = new HashMap<Integer, MatchingObject>();
		isInitialized = false;

		super.start();
	}

	protected INetworkAdapter getNetworkAdapter() {
		return Modules.sceNetModule.getNetworkAdapter();
	}

	public int checkMatchingId(int matchingId) {
    	checkInitialized();

		if (!matchingObjects.containsKey(matchingId)) {
			throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOC_INVALID_MATCHING_ID);
		}

		return matchingId;
	}

	public void hleNetAdhocMatchingEventThread(Processor processor) {
		int matchingId = processor.cpu.getRegister(loopThreadRegisterArgument);
		if (log.isTraceEnabled()) {
			log.trace(String.format("hleNetAdhocMatchingEventThread matchingId=%d", matchingId));
		}

		MatchingObject matchingObject = matchingObjects.get(matchingId);
		if (matchingObject != null && matchingObject.eventLoop()) {
			Modules.ThreadManForUserModule.hleKernelDelayThread(10000, false);
		} else {
			// Exit thread with status 0
			processor.cpu._v0 = 0;
			Modules.ThreadManForUserModule.hleKernelExitDeleteThread();
		}
	}

	public void hleNetAdhocMatchingInputThread(Processor processor) {
		int matchingId = processor.cpu.getRegister(loopThreadRegisterArgument);
		if (log.isTraceEnabled()) {
			log.trace(String.format("hleNetAdhocMatchingInputThread matchingId=%d", matchingId));
		}

		MatchingObject matchingObject = matchingObjects.get(matchingId);
		if (matchingObject != null && matchingObject.inputLoop()) {
			Modules.ThreadManForUserModule.hleKernelDelayThread(10000, false);
		} else {
			// Exit thread with status 0
			processor.cpu._v0 = 0;
			Modules.ThreadManForUserModule.hleKernelExitDeleteThread();
		}
	}

	private static String getModeName(int mode) {
		switch (mode) {
			case PSP_ADHOC_MATCHING_MODE_HOST: return "HOST";
			case PSP_ADHOC_MATCHING_MODE_CLIENT: return "CLIENT";
			case PSP_ADHOC_MATCHING_MODE_PTP: return "PTP";
		}

		return String.format("Unknown mode %d", mode);
	}

	protected void checkInitialized() {
		if (!isInitialized) {
			throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOC_MATCHING_NOT_INITIALIZED);
		}
	}

	/**
     * Initialize the Adhoc matching library
     *
     * @param memsize - Internal memory pool size. Lumines uses 0x20000
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x2A2A1E07, version = 150)
    public int sceNetAdhocMatchingInit(int memsize) {
    	if (isInitialized) {
    		return SceKernelErrors.ERROR_NET_ADHOC_MATCHING_ALREADY_INITIALIZED;
    	}

    	isInitialized = true;

    	return 0;
    }

    /**
     * Terminate the Adhoc matching library
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x7945ECDA, version = 150)
    public int sceNetAdhocMatchingTerm() {
    	isInitialized = false;

    	return 0;
    }

    /**
     * Create an Adhoc matching object
     *
     * @param mode - One of ::pspAdhocMatchingModes
     * @param maxpeers - Maximum number of peers to match (only used when mode is PSP_ADHOC_MATCHING_MODE_HOST)
     * @param port - Port. Lumines uses 0x22B
     * @param bufsize - Receiving buffer size
     * @param hellodelay - Hello message send delay in microseconds (only used when mode is PSP_ADHOC_MATCHING_MODE_HOST or PSP_ADHOC_MATCHING_MODE_PTP)
     * @param pingdelay - Ping send delay in microseconds. Lumines uses 0x5B8D80 (only used when mode is PSP_ADHOC_MATCHING_MODE_HOST or PSP_ADHOC_MATCHING_MODE_PTP)
     * @param initcount - Initial count of the of the resend counter. Lumines uses 3
     * @param msgdelay - Message send delay in microseconds
     * @param callback - Callback to be called for matching
     *
     * @return ID of object on success, < 0 on error.
     */
    @HLEFunction(nid = 0xCA5EDA6F, version = 150)
    public int sceNetAdhocMatchingCreate(int mode, int maxPeers, int port, int bufSize, int helloDelay, int pingDelay, int initCount, int msgDelay, @CanBeNull TPointer callback) {
    	checkInitialized();

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetAdhocMatchingCreate mode=%s", getModeName(mode)));
    	}

        MatchingObject matchingObject = getNetworkAdapter().createMatchingObject();
        matchingObject.setMode(mode);
        matchingObject.setMaxPeers(maxPeers);
        matchingObject.setPort(port);
        matchingObject.setBufSize(bufSize);
        matchingObject.setHelloDelay(helloDelay);
        matchingObject.setPingDelay(pingDelay);
        matchingObject.setInitCount(initCount);
        matchingObject.setMsgDelay(msgDelay);
        matchingObject.setCallback(callback.getAddress());
        matchingObject.create();
        matchingObjects.put(matchingObject.getId(), matchingObject);

        return matchingObject.getId();
    }

    /**
     * Start a matching object
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param evthpri - Priority of the event handler thread. Lumines uses 0x10
     * @param evthstack - Stack size of the event handler thread. Lumines uses 0x2000
     * @param inthpri - Priority of the input handler thread. Lumines uses 0x10
     * @param inthstack - Stack size of the input handler thread. Lumines uses 0x2000
     * @param optlen - Size of hellodata
     * @param optdata - Pointer to block of data passed to callback
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x93EF3843, version = 150)
    public int sceNetAdhocMatchingStart(@CheckArgument("checkMatchingId") int matchingId, int evthPri, int evthStack, int inthPri, int inthStack, int optLen, @CanBeNull TPointer optData) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("Matching opt data: %s", Utilities.getMemoryDump(optData.getAddress(), optLen)));
		}

        return matchingObjects.get(matchingId).start(evthPri, evthStack, inthPri, inthStack, optLen, optData.getAddress());
    }

    /**
     * Stop a matching object
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x32B156B3, version = 150)
    public int sceNetAdhocMatchingStop(@CheckArgument("checkMatchingId") int matchingId) {
        return matchingObjects.get(matchingId).stop();
    }

    /**
     * Delete an Adhoc matching object
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xF16EAF4F, version = 150)
    public int sceNetAdhocMatchingDelete(@CheckArgument("checkMatchingId") int matchingId) {
        matchingObjects.remove(matchingId).delete();

        return 0;
    }

    /**
     * Send data to a matching target
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - The MAC address to send the data to
     * @param datalen - Length of the data
     * @param data - Pointer to the data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xF79472D7, version = 150)
    public int sceNetAdhocMatchingSendData(@CheckArgument("checkMatchingId") int matchingId, pspNetMacAddress macAddress, int dataLen, TPointer data) {
        if (log.isTraceEnabled()) {
        	log.trace(String.format("Send data: %s", Utilities.getMemoryDump(data.getAddress(), dataLen)));
        }

        return matchingObjects.get(matchingId).send(macAddress, dataLen, data.getAddress());
    }

    /**
     * Abort a data send to a matching target
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - The MAC address to send the data to
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xEC19337D, version = 150)
    public int sceNetAdhocMatchingAbortSendData(@CheckArgument("checkMatchingId") int matchingId, pspNetMacAddress macAddress) {
    	return 0;
    }

    /**
     * Select a matching target
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - MAC address to select
     * @param optlen - Optional data length
     * @param optdata - Pointer to the optional data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x5E3D4B79, version = 150)
    public int sceNetAdhocMatchingSelectTarget(@CheckArgument("checkMatchingId") int matchingId, pspNetMacAddress macAddress, int optLen, @CanBeNull TPointer optData) {
        return matchingObjects.get(matchingId).selectTarget(macAddress, optLen, optData.getAddress());
    }

    /**
     * Cancel a matching target
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - The MAC address to cancel
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xEA3C6108, version = 150)
    public int sceNetAdhocMatchingCancelTarget(@CheckArgument("checkMatchingId") int matchingId, pspNetMacAddress macAddress) {
        return matchingObjects.get(matchingId).cancelTarget(macAddress);
    }

    /**
     * Cancel a matching target (with optional data)
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - The MAC address to cancel
     * @param optlen - Optional data length
     * @param optdata - Pointer to the optional data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x8F58BEDF, version = 150)
    public int sceNetAdhocMatchingCancelTargetWithOpt(@CheckArgument("checkMatchingId") int matchingId, pspNetMacAddress macAddress, int optLen, @CanBeNull TPointer optData) {
        if (log.isTraceEnabled()) {
        	log.trace(String.format("Opt data: %s", Utilities.getMemoryDump(optData.getAddress(), optLen)));
        }

        return matchingObjects.get(matchingId).cancelTarget(macAddress, optLen, optData.getAddress());
    }

    /**
     * Get the optional hello message
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param optlenAddr - Length of the hello data (input/output)
     * @param optdata - Pointer to the hello data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xB5D96C2A, version = 150)
    public int sceNetAdhocMatchingGetHelloOpt(@CheckArgument("checkMatchingId") int matchingId, TPointer32 optLenAddr, @CanBeNull TPointer optData) {
        MatchingObject matchingObject = matchingObjects.get(matchingId);
        int helloOptLen = matchingObject.getHelloOptLen();

        int bufSize = optLenAddr.getValue();
        optLenAddr.setValue(helloOptLen);

        if (helloOptLen > 0 && optData.getAddress() != 0 && bufSize > 0) {
        	int length = Math.min(bufSize, helloOptLen);
        	writeBytes(optData.getAddress(), length, matchingObject.getHelloOptData(), 0);
        }

        return 0;
    }

    /**
     * Set the optional hello message
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param optlen - Length of the hello data
     * @param optdata - Pointer to the hello data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xB58E61B7, version = 150)
    public int sceNetAdhocMatchingSetHelloOpt(@CheckArgument("checkMatchingId") int matchingId, int optLen, @CanBeNull TPointer optData) {
        if (log.isTraceEnabled()) {
        	log.trace(String.format("Hello opt data: %s", Utilities.getMemoryDump(optData.getAddress(), optLen)));
        }

        matchingObjects.get(matchingId).setHelloOpt(optLen, optData.getAddress());

        return 0;
    }

    /**
     * Get a list of matching members
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param length - The length of the list.
     * @param buf - An allocated area of size length.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xC58BCD9E, version = 150)
    public int sceNetAdhocMatchingGetMembers(@CheckArgument("checkMatchingId") int matchingId, TPointer32 sizeAddr, @CanBeNull TPointer buf) {
    	final int matchingMemberSize = 12;

        MatchingObject matchingObject = matchingObjects.get(matchingId);
        List<pspNetMacAddress> members = matchingObject.getMembers();

        int size = sizeAddr.getValue();
    	sizeAddr.setValue(matchingMemberSize * members.size());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetAdhocMatchingGetMembers returning size=%d", sizeAddr.getValue()));
    	}

    	if (buf.isNotNull()) {
        	int offset = 0;
        	for (pspNetMacAddress member : members) {
        		// Check if enough space available to write the next structure
        		if (offset + matchingMemberSize > size || member == null) {
        			break;
        		}

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetAdhocMatchingGetMembers returning %s at 0x%08X", member, buf.getAddress() + offset));
        		}

        		/** Pointer to next Member structure in list: will be written later */
        		offset += 4;

        		/** MAC address */
        		member.write(buf, offset);
        		offset += member.sizeof();

        		/** Padding */
        		buf.setValue16(offset, (short) 0);
        		offset += 2;
        	}

        	fillNextPointersInLinkedList(buf, offset, matchingMemberSize);
        }

        return 0;
    }

    /**
     * Get the status of the memory pool used by the matching library
     *
     * @param poolstat - A ::pspAdhocPoolStat.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x9C5CFB7D, version = 150)
    public int sceNetAdhocMatchingGetPoolStat() {
    	checkInitialized();

    	return 0;
    }

    /**
     * Get the maximum memory usage by the matching library
     *
     * @return The memory usage on success, < 0 on error.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x40F8F435, version = 150)
    public int sceNetAdhocMatchingGetPoolMaxAlloc() {
    	checkInitialized();

    	return 0;
    }
}