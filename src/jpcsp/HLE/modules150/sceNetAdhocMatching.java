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

import static jpcsp.util.Utilities.writeBytes;

import java.util.HashMap;
import java.util.List;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.adhoc.MatchingObject;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceNetAdhocMatching extends HLEModule {
    protected static Logger log = Modules.getLogger("sceNetAdhocMatching");
    protected HashMap<Integer, MatchingObject> matchingObjects;
    public static final int loopThreadRegisterArgument = Common._s0; // $s0 is presserved across calls

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

		super.start();
	}

	protected INetworkAdapter getNetworkAdapter() {
		return Modules.sceNetModule.getNetworkAdapter();
	}

	public int checkMatchingId(int matchingId) {
		if (!matchingObjects.containsKey(matchingId)) {
			throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOC_INVALID_MATCHING_ID);
		}

		return matchingId;
	}

	public void hleNetAdhocMatchingEventThread(Processor processor) {
		int matchingId = processor.cpu.gpr[loopThreadRegisterArgument];
		if (log.isTraceEnabled()) {
			log.trace(String.format("hleNetAdhocMatchingEventThread matchingId=%d", matchingId));
		}

		MatchingObject matchingObject = matchingObjects.get(matchingId);
		if (matchingObject != null && matchingObject.eventLoop()) {
			Modules.ThreadManForUserModule.hleKernelDelayThread(10000, false);
		} else {
			// Exit thread with status 0
			processor.cpu.gpr[Common._v0] = 0;
			Modules.ThreadManForUserModule.hleKernelExitDeleteThread();
		}
	}

	public void hleNetAdhocMatchingInputThread(Processor processor) {
		int matchingId = processor.cpu.gpr[loopThreadRegisterArgument];
		if (log.isTraceEnabled()) {
			log.trace(String.format("hleNetAdhocMatchingInputThread matchingId=%d", matchingId));
		}

		MatchingObject matchingObject = matchingObjects.get(matchingId);
		if (matchingObject != null && matchingObject.inputLoop()) {
			Modules.ThreadManForUserModule.hleKernelDelayThread(10000, false);
		} else {
			// Exit thread with status 0
			processor.cpu.gpr[Common._v0] = 0;
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

	/**
     * Initialise the Adhoc matching library
     *
     * @param memsize - Internal memory pool size. Lumines uses 0x20000
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x2A2A1E07, version = 150)
    public int sceNetAdhocMatchingInit(int memsize) {
        log.warn(String.format("IGNORING: sceNetAdhocMatchingInit: memsize=0x%X", memsize));

		return 0;
    }

    /**
     * Terminate the Adhoc matching library
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x7945ECDA, version = 150)
    public int sceNetAdhocMatchingTerm() {
        log.warn("IGNORING: sceNetAdhocMatchingTerm");

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
        log.warn(String.format("PARTIAL: sceNetAdhocMatchingCreate mode=%d(%s), maxPeers=%d, port=%d, bufSize=%d, helloDelay=%d, pingDelay=%d, initCount=%d, msgDelay=%d, callback=%s", mode, getModeName(mode), maxPeers, port, bufSize, helloDelay, pingDelay, initCount, msgDelay, callback));

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
    	log.warn(String.format("PARTIAL: sceNetAdhocMatchingStart matchingId=%d, evthPri=%d, evthStack=0x%X, inthPri=%d, inthStack=0x%X, optLen=%d, optData=%s", matchingId, evthPri, evthStack, inthPri, inthStack, optLen, optData));
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
        log.warn(String.format("PARTIAL: sceNetAdhocMatchingStop matchingId=%d", matchingId));

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
        log.warn(String.format("PARTIAL: sceNetAdhocMatchingDelete matchingId=%d", matchingId));

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
    public int sceNetAdhocMatchingSendData(@CheckArgument("checkMatchingId") int matchingId, TPointer macAddr, int dataLen, TPointer data) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr.getAddress());
        log.warn(String.format("PARTIAL: sceNetAdhocMatchingSendData matchingId=%d, macAddr=%s(%s), dataLen=%d, data=%s", matchingId, macAddr, macAddress, dataLen, data));
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
    @HLEFunction(nid = 0xEC19337D, version = 150)
    public int sceNetAdhocMatchingAbortSendData(@CheckArgument("checkMatchingId") int matchingId, TPointer macAddr) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr.getAddress());
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingAbortSendData matchingId=%d, macAddr=%s(%s)", matchingId, macAddr, macAddress));

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
    public int sceNetAdhocMatchingSelectTarget(@CheckArgument("checkMatchingId") int matchingId, TPointer macAddr, int optLen, @CanBeNull TPointer optData) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr.getAddress());
        log.warn(String.format("PARTIAL: sceNetAdhocMatchingSelectTarget matchingId=%d, macAddr=%s(%s), optLen=%d, optData=%s", matchingId, macAddr, macAddress, optLen, optData));

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
    public int sceNetAdhocMatchingCancelTarget(@CheckArgument("checkMatchingId") int matchingId, TPointer macAddr) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr.getAddress());
        log.warn(String.format("PARTIAL: sceNetAdhocMatchingCancelTarget matchingId=%d, macAddr=%s(%s)", matchingId, macAddr, macAddress));

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
    public int sceNetAdhocMatchingCancelTargetWithOpt(@CheckArgument("checkMatchingId") int matchingId, TPointer macAddr, int optLen, @CanBeNull TPointer optData) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr.getAddress());
        log.warn(String.format("PARTIAL: sceNetAdhocMatchingCancelTargetWithOpt matchingId=%d, macAddr=%s(%s), optLen=%d, optData=%s", matchingId, macAddr, macAddress, optLen, optData));
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
        log.warn(String.format("PARTIAL: sceNetAdhocMatchingGetHelloOpt matchingId=%d, optlenAddr=%s, optData=%s", matchingId, optLenAddr, optData));

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
        log.warn(String.format("PARTIAL: sceNetAdhocMatchingSetHelloOpt matchingId=%d, optLen=%d, optData=%s", matchingId, optLen, optData));
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
        log.warn(String.format("PARTIAL: sceNetAdhocMatchingGetMembers matchingId=%d, sizeAddr=%s(%d), buf=%s", matchingId, sizeAddr.toString(), sizeAddr.getValue(), buf));

        MatchingObject matchingObject = matchingObjects.get(matchingId);
        List<pspNetMacAddress> members = matchingObject.getMembers();

        if (buf.getAddress() == 0) {
        	// Return size required
        	sizeAddr.setValue(matchingMemberSize * members.size());
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceNetAdhocMatchingGetMembers returning size=%d", sizeAddr.getValue()));
        	}
        } else {
        	Memory mem = Memory.getInstance();
        	int addr = buf.getAddress();
        	int endAddr = addr + sizeAddr.getValue();
        	sizeAddr.setValue(matchingMemberSize * members.size());
        	for (pspNetMacAddress member : members) {
        		// Check if enough space available to write the next structure
        		if (addr + matchingMemberSize > endAddr || member == null) {
        			break;
        		}

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetAdhocMatchingGetMembers returning %s at 0x%08X", member, addr));
        		}

        		/** Pointer to next Member structure in list: will be written later */
        		addr += 4;

        		/** MAC address */
        		member.write(mem, addr);
        		addr += member.sizeof();

        		/** Padding */
        		mem.memset(addr, (byte) 0, 2);
        		addr += 2;
        	}

        	for (int nextAddr = buf.getAddress(); nextAddr < addr; nextAddr += matchingMemberSize) {
        		if (nextAddr + matchingMemberSize >= addr) {
        			// Last one
        			mem.write32(nextAddr, 0);
        		} else {
        			// Pointer to next one
        			mem.write32(nextAddr, nextAddr + matchingMemberSize);
        		}
        	}
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
    @HLEFunction(nid = 0x9C5CFB7D, version = 150)
    public void sceNetAdhocMatchingGetPoolStat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingGetPoolStat");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get the maximum memory usage by the matching library
     *
     * @return The memory usage on success, < 0 on error.
     */
    @HLEFunction(nid = 0x40F8F435, version = 150)
    public void sceNetAdhocMatchingGetPoolMaxAlloc(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingGetPoolMaxAlloc");

        cpu.gpr[2] = 0xDEADC0DE;
    }
}