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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;
import jpcsp.HLE.TPointer32;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructureVariableLength;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.hardware.Wlan;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.adhoc.AdhocMessage;
import jpcsp.network.adhoc.PdpObject;
import jpcsp.network.adhoc.PtpObject;
import jpcsp.scheduler.Scheduler;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

@HLELogging
public class sceNetAdhoc extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetAdhoc");

    // For test purpose when running 2 different Jpcsp instances on the same computer:
    // one computer has to have netClientPortShift=0 and netServerPortShift=100,
    // the other computer, netClientPortShift=100 and netServerPortShift=0.
    private int netClientPortShift = 0;
    private int netServerPortShift = 0;

    // Period to update the Game Mode
    protected static final int GAME_MODE_UPDATE_MICROS = 12000;

    protected static final int PSP_ADHOC_POLL_READY_TO_SEND = 1;
    protected static final int PSP_ADHOC_POLL_DATA_AVAILABLE = 2;
    protected static final int PSP_ADHOC_POLL_CAN_CONNECT = 4;
    protected static final int PSP_ADHOC_POLL_CAN_ACCEPT = 8;

    protected HashMap<Integer, PdpObject> pdpObjects;
    protected HashMap<Integer, PtpObject> ptpObjects;
    private int currentFreePort;
	public static final byte[] ANY_MAC_ADDRESS = new byte[] {
		(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
	};
	private GameModeScheduledAction gameModeScheduledAction;
	protected GameModeArea masterGameModeArea;
	protected LinkedList<GameModeArea> replicaGameModeAreas;
	private static final String replicaIdPurpose = "sceNetAdhoc-Replica";
    private static final int adhocGameModePort = 31000;
    private DatagramSocket gameModeSocket;
    private boolean isInitialized;

    protected static class GameModeScheduledAction implements IAction {
    	private final int scheduleRepeatMicros;
    	private long nextSchedule;

    	public GameModeScheduledAction(int scheduleRepeatMicros) {
    		this.scheduleRepeatMicros = scheduleRepeatMicros;
    	}

    	public void stop() {
    		Scheduler.getInstance().removeAction(nextSchedule, this);
    	}

    	public void start() {
    		Scheduler.getInstance().addAction(this);
    	}

    	@Override
		public void execute() {
    		Modules.sceNetAdhocModule.hleGameModeUpdate();

    		nextSchedule = Scheduler.getNow() + scheduleRepeatMicros;
    		Scheduler.getInstance().addAction(nextSchedule, this);
		}
    }

    public static class GameModeArea {
    	public pspNetMacAddress macAddress;
    	public int addr;
    	public int size;
    	public int id;
    	private byte[] newData;
    	private long updateTimestamp;

    	public GameModeArea(int addr, int size) {
    		this.addr = addr;
    		this.size = size;
    		id = -1;
    	}

    	public GameModeArea(pspNetMacAddress macAddress, int addr, int size) {
    		this.macAddress = macAddress;
    		this.addr = addr;
    		this.size = size;
    		id = SceUidManager.getNewUid(replicaIdPurpose);
    	}

    	public void delete() {
    		if (id >= 0) {
    			SceUidManager.releaseUid(id, replicaIdPurpose);
    			id = -1;
    		}
    	}

    	public void setNewData(byte[] newData) {
    		updateTimestamp = Emulator.getClock().microTime();
    		this.newData = newData;
    	}

    	public void setNewData() {
    		byte[] data = new byte[size];
    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, size, 1);
    		for (int i = 0; i < data.length; i++) {
    			data[i] = (byte) memoryReader.readNext();
    		}

    		setNewData(data);
    	}

    	public void resetNewData() {
    		newData = null;
    	}

    	public byte[] getNewData() {
    		return newData;
    	}

    	public boolean hasNewData() {
    		return newData != null;
    	}

    	public void writeNewData() {
    		if (newData != null) {
    			writeBytes(addr, Math.min(size, newData.length), newData, 0);
    		}
    	}

    	public long getUpdateTimestamp() {
    		return updateTimestamp;
    	}

    	@Override
		public String toString() {
			if (macAddress == null) {
				return String.format("Master GameModeArea addr=0x%08X, size=%d", addr, size);
			}
			return String.format("Replica GameModeArea id=%d, macAddress=%s, addr=0x%08X, size=%d", id, macAddress, addr, size);
		}
    }

	protected static String getPollEventName(int event) {
		return String.format("Unknown 0x%X", event);
	}

	protected static class pspAdhocPollId extends pspAbstractMemoryMappedStructure {
		public int id;
		public int events;
		public int revents;

		@Override
		protected void read() {
			id = read32();
			events = read32();
			revents = read32();
		}

		@Override
		protected void write() {
			write32(id);
			write32(events);
			write32(revents);
		}

		@Override
		public int sizeof() {
			return 12;
		}

		@Override
		public String toString() {
			return String.format("PollId[id=%d, events=0x%X(%s), revents=0x%X(%s)]", id, events, getPollEventName(events), revents, getPollEventName(revents));
		}
	}

	protected static class GameModeUpdateInfo extends pspAbstractMemoryMappedStructureVariableLength {
		public int updated;
		public long timeStamp;

		@Override
		protected void read() {
			super.read();
			updated = read32();
			timeStamp = read64();
		}

		@Override
		protected void write() {
			super.write();
			write32(updated);
			write64(timeStamp);
		}
	}

	@Override
    public String getName() {
        return "sceNetAdhoc";
    }

    @Override
	public void start() {
	    pdpObjects = new HashMap<Integer, PdpObject>();
	    ptpObjects = new HashMap<Integer, PtpObject>();
	    currentFreePort = 0x4000;
	    replicaGameModeAreas = new LinkedList<sceNetAdhoc.GameModeArea>();
	    isInitialized = false;

	    super.start();
	}

    public void setNetClientPortShift(int netClientPortShift) {
    	this.netClientPortShift = netClientPortShift;
    	log.info(String.format("Using netClientPortShift=%d", netClientPortShift));
    }

    public void setNetServerPortShift(int netServerPortShift) {
    	this.netServerPortShift = netServerPortShift;
    	log.info(String.format("Using netServerPortShift=%d", netServerPortShift));
    }

    public int getClientPortFromRealPort(byte[] clientMacAddress, int realPort) {
    	if (isMyMacAddress(clientMacAddress)) {
    		// if the client is my-self, then this is actually a server port...
    		return getServerPortFromRealPort(realPort);
    	}

    	return realPort - netClientPortShift;
    }

    public int getRealPortFromClientPort(byte[] clientMacAddress, int clientPort) {
    	if (isMyMacAddress(clientMacAddress)) {
    		// if the client is my-self, then this is actually a server port...
    		return getRealPortFromServerPort(clientPort);
    	}

    	return clientPort + netClientPortShift;
    }

    public int getServerPortFromRealPort(int realPort) {
    	return realPort - netServerPortShift;
    }

    public int getRealPortFromServerPort(int serverPort) {
    	return serverPort + netServerPortShift;
    }

    public boolean hasNetPortShiftActive() {
    	return netServerPortShift > 0 || netClientPortShift > 0;
    }

    protected void checkInitialized() {
    	if (!isInitialized) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOC_NOT_INITIALIZED);
    	}
    }

    public void hleExitGameMode() {
    	masterGameModeArea = null;
    	replicaGameModeAreas.clear();
    	stopGameMode();
    }

    public void hleGameModeUpdate() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleGameModeUpdate"));
    	}

		try {
			if (gameModeSocket == null) {
				gameModeSocket = new DatagramSocket(Modules.sceNetAdhocModule.getRealPortFromServerPort(adhocGameModePort));
	    		// For broadcast
				gameModeSocket.setBroadcast(true);
	    		// Non-blocking (timeout = 0 would mean blocking)
				gameModeSocket.setSoTimeout(1);
			}

			// Send master area
			if (masterGameModeArea != null && masterGameModeArea.hasNewData()) {
				try {
					AdhocMessage adhocGameModeMessage = getNetworkAdapter().createAdhocGameModeMessage(masterGameModeArea);
			    	SocketAddress socketAddress = Modules.sceNetAdhocModule.getSocketAddress(sceNetAdhoc.ANY_MAC_ADDRESS, Modules.sceNetAdhocModule.getRealPortFromClientPort(sceNetAdhoc.ANY_MAC_ADDRESS, adhocGameModePort));
			    	DatagramPacket packet = new DatagramPacket(adhocGameModeMessage.getMessage(), adhocGameModeMessage.getMessageLength(), socketAddress);
			    	gameModeSocket.send(packet);

			    	if (log.isDebugEnabled()) {
			    		log.debug(String.format("GameMode message sent to all: %s", adhocGameModeMessage));
			    	}
				} catch (SocketTimeoutException e) {
					// Ignore exception
				}
			}

			// Receive all waiting messages
			do {
				try {
					byte[] bytes = new byte[10000];
			    	DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
			    	gameModeSocket.receive(packet);
			    	AdhocMessage adhocGameModeMessage = getNetworkAdapter().createAdhocGameModeMessage(packet.getData(), packet.getLength());

			    	if (log.isDebugEnabled()) {
			    		log.debug(String.format("GameMode received: %s", adhocGameModeMessage));
			    	}

			    	for (GameModeArea gameModeArea : replicaGameModeAreas) {
			    		if (isSameMacAddress(gameModeArea.macAddress.macAddress, adhocGameModeMessage.getFromMacAddress())) {
			    			if (log.isDebugEnabled()) {
			    				log.debug(String.format("Received new Data for GameMode Area %s", gameModeArea));
			    			}
			    			gameModeArea.setNewData(adhocGameModeMessage.getData());
			    			break;
			    		}
			    	}
				} catch (SocketTimeoutException e) {
					// No more messages available
					break;
				}
			} while (true);
		} catch (IOException e) {
			log.error("hleGameModeUpdate", e);
		}
    }

    protected void startGameMode() {
    	if (gameModeScheduledAction == null) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Starting GameMode"));
    		}
    		gameModeScheduledAction = new GameModeScheduledAction(GAME_MODE_UPDATE_MICROS);
    		gameModeScheduledAction.start();
    	}
    }

    protected void stopGameMode() {
    	if (gameModeScheduledAction != null) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Stopping GameMode"));
    		}
    		gameModeScheduledAction.stop();
    		gameModeScheduledAction = null;
    	}

    	if (gameModeSocket != null) {
    		gameModeSocket.close();
    		gameModeSocket = null;
    	}
    }

    public SocketAddress getSocketAddress(byte[] macAddress, int realPort) throws UnknownHostException {
    	return getNetworkAdapter().getSocketAddress(macAddress, realPort);
	}

	public static boolean isSameMacAddress(byte[] macAddress1, byte[] macAddress2) {
		if (macAddress1.length != macAddress2.length) {
			return false;
		}

		for (int i = 0; i < macAddress1.length; i++) {
			if (macAddress1[i] != macAddress2[i]) {
				return false;
			}
		}

		return true;
	}

	public static boolean isAnyMacAddress(byte[] macAddress) {
		return isSameMacAddress(macAddress, ANY_MAC_ADDRESS);
	}

	public static boolean isMyMacAddress(byte[] macAddress) {
		return isSameMacAddress(Wlan.getMacAddress(), macAddress);
	}

	private int getFreePort() {
    	int freePort = currentFreePort;
    	if (netClientPortShift > 0 || netServerPortShift > 0) {
    		currentFreePort += 2;
    	} else {
    		currentFreePort++;
    	}

		if (currentFreePort > 0x7FFF) {
			currentFreePort -= 0x4000;
		}

		return freePort;
    }

	public int checkPdpId(int pdpId) {
		checkInitialized();

		if (!pdpObjects.containsKey(pdpId)) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Invalid Pdp Id=%d", pdpId));
			}
			throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOC_INVALID_SOCKET_ID);
		}

		return pdpId;
	}

	public int checkPtpId(int ptpId) {
		checkInitialized();

		if (!ptpObjects.containsKey(ptpId)) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Invalid Ptp Id=%d", ptpId));
			}
			throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOC_INVALID_SOCKET_ID);
		}

		return ptpId;
	}

	public void hleAddPtpObject(PtpObject ptpObject) {
		ptpObjects.put(ptpObject.getId(), ptpObject);
	}

	protected INetworkAdapter getNetworkAdapter() {
		return Modules.sceNetModule.getNetworkAdapter();
	}

	/**
     * Initialize the adhoc library.
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xE1D621D7, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocInit() {
        log.info(String.format("sceNetAdhocInit: using MAC address=%s, nick name='%s'", sceNet.convertMacAddressToString(Wlan.getMacAddress()), sceUtility.getSystemParamNickname()));

        if (isInitialized) {
        	return SceKernelErrors.ERROR_NET_ADHOC_ALREADY_INITIALIZED;
        }

        isInitialized = true;

        return 0;
    }

    /**
     * Terminate the adhoc library
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xA62C6F57, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocTerm() {
    	isInitialized = false;

    	return 0;
    }

    @HLEFunction(nid = 0x7A662D6B, version = 150)
    public int sceNetAdhocPollSocket(TPointer socketsAddr, int count, int timeout, int nonblock) {
		checkInitialized();

    	Memory mem = Memory.getInstance();

        int countEvents = 0;
        for (int i = 0; i < count; i++) {
        	pspAdhocPollId pollId = new pspAdhocPollId();
        	pollId.read(mem, socketsAddr.getAddress() + i * pollId.sizeof());

        	PdpObject pdpObject = pdpObjects.get(pollId.id);
        	PtpObject ptpObject = null;
        	if (pdpObject == null) {
        		ptpObject = ptpObjects.get(pollId.id);
        		pdpObject = ptpObject;
        	}
        	if (pdpObject != null) {
        		try {
        			pdpObject.update();
        		} catch (IOException e) {
        			// Ignore exception
        		}
        	}

        	pollId.revents = 0;
        	if ((pollId.events & PSP_ADHOC_POLL_DATA_AVAILABLE) != 0 && pdpObject.getRcvdData() > 0) {
        		pollId.revents |= PSP_ADHOC_POLL_DATA_AVAILABLE;
        	}
        	if ((pollId.events & PSP_ADHOC_POLL_READY_TO_SEND) != 0) {
        		// Data can always be sent
        		pollId.revents |= PSP_ADHOC_POLL_READY_TO_SEND;
        	}
        	if ((pollId.events & PSP_ADHOC_POLL_CAN_CONNECT) != 0) {
    			if (ptpObject != null && ptpObject.canConnect()) {
    				pollId.revents |= PSP_ADHOC_POLL_CAN_CONNECT;
        		}
        	}
        	if ((pollId.events & PSP_ADHOC_POLL_CAN_ACCEPT) != 0) {
    			if (ptpObject != null && ptpObject.canAccept()) {
    				pollId.revents |= PSP_ADHOC_POLL_CAN_ACCEPT;
        		}
        	}

        	if (pollId.revents != 0) {
        		countEvents++;
        	}

        	pollId.write(mem);

        	log.info(String.format("sceNetAdhocPollSocket pollId[%d]=%s", i, pollId));
        }

        return countEvents;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x73BFD52D, version = 150)
    public int sceNetAdhocSetSocketAlert(int id, int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4D2CE199, version = 150)
    public int sceNetAdhocGetSocketAlert() {
    	return 0;
    }

    /**
     * Create a PDP object.
     *
     * @param mac - Your MAC address (from sceWlanGetEtherAddr)
     * @param port - Port to use, lumines uses 0x309
     * @param bufsize - Socket buffer size, lumines sets to 0x400
     * @param unk1 - Unknown, lumines sets to 0
     *
     * @return The ID of the PDP object (< 0 on error)
     */
    @HLEFunction(nid = 0x6F92741B, version = 150)
    public int sceNetAdhocPdpCreate(pspNetMacAddress macAddress, int port, int bufSize, int unk1) {
    	checkInitialized();

    	if (port == 0) {
			// Allocate a free port
			port = getFreePort();
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceNetAdhocPdpCreate: using free port %d", port));
			}
		}

		PdpObject pdpObject = getNetworkAdapter().createPdpObject();
    	int result = pdpObject.create(macAddress, port, bufSize);
    	if (result == pdpObject.getId()) {
    		pdpObjects.put(pdpObject.getId(), pdpObject);

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceNetAdhocPdpCreate: returning id=0x%X", result));
    		}
    	} else {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceNetAdhocPdpCreate: returning error=0x%08X", result));
    		}
    	}

		return result;
    }

    /**
     * Send a PDP packet to a destination
     *
     * @param id - The ID as returned by ::sceNetAdhocPdpCreate
     * @param destMacAddr - The destination MAC address, can be set to all 0xFF for broadcast
     * @param port - The port to send to
     * @param data - The data to send
     * @param len - The length of the data.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return Bytes sent, < 0 on error
     */
    @HLEFunction(nid = 0xABED3790, version = 150)
    public int sceNetAdhocPdpSend(@CheckArgument("checkPdpId") int id, pspNetMacAddress destMacAddress, int port, TPointer data, int len, int timeout, int nonblock) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("Send data: %s", Utilities.getMemoryDump(data.getAddress(), len)));
		}

    	return pdpObjects.get(id).send(destMacAddress, port, data, len, timeout, nonblock);
    }

    /**
     * Receive a PDP packet
     *
     * @param id - The ID of the PDP object, as returned by ::sceNetAdhocPdpCreate
     * @param srcMacAddr - Buffer to hold the source mac address of the sender
     * @param port - Buffer to hold the port number of the received data
     * @param data - Data buffer
     * @param dataLength - The length of the data buffer
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return Number of bytes received, < 0 on error.
     */
    @HLEFunction(nid = 0xDFE53E03, version = 150)
    public int sceNetAdhocPdpRecv(@CheckArgument("checkPdpId") int id, TPointer srcMacAddr, TPointer16 portAddr, TPointer data, TPointer32 dataLengthAddr, int timeout, int nonblock) {
        int result = pdpObjects.get(id).recv(srcMacAddr, portAddr, data, dataLengthAddr, timeout, nonblock);

        return result;
    }

    /**
     * Delete a PDP object.
     *
     * @param id - The ID returned from ::sceNetAdhocPdpCreate
     * @param unk1 - Unknown, set to 0
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x7F27BB5E, version = 150)
    public int sceNetAdhocPdpDelete(@CheckArgument("checkPdpId") int id, int unk1) {
        pdpObjects.remove(id).delete();

        return 0;
    }

    /**
     * Get the status of all PDP objects
     *
     * @param size - Pointer to the size of the stat array (e.g 20 for one structure)
     * @param stat - Pointer to a list of ::pspStatStruct structures.
     *
     * typedef struct pdpStatStruct
     * {
     *    struct pdpStatStruct *next; // Pointer to next PDP structure in list
     *    int pdpId;                  // pdp ID
     *    unsigned char mac[6];       // MAC address
     *    unsigned short port;        // Port
     *    unsigned int rcvdData;      // Bytes received
     * } pdpStatStruct
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xC7C1FC57, version = 150)
    public int sceNetAdhocGetPdpStat(TPointer32 sizeAddr, @CanBeNull TPointer buf) {
		checkInitialized();

    	final int objectInfoSize = 20;

    	sizeAddr.setValue(objectInfoSize * pdpObjects.size());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetAdhocGetPdpStat returning size=%d", sizeAddr.getValue()));
    	}

    	if (buf.isNotNull()) {
        	Memory mem = Memory.getInstance();
        	int addr = buf.getAddress();
        	int endAddr = addr + sizeAddr.getValue();
        	for (int pdpId : pdpObjects.keySet()) {
        		PdpObject pdpObject = pdpObjects.get(pdpId);

        		// Check if enough space available to write the next structure
        		if (addr + objectInfoSize > endAddr || pdpObject == null) {
        			break;
        		}

        		try {
					pdpObject.update();
				} catch (IOException e) {
					// Ignore error
				}

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetAdhocGetPdpStat returning %s at 0x%08X", pdpObject, addr));
        		}

        		/** Pointer to next PDP structure in list: will be written later */
        		addr += 4;

        		/** pdp ID */
        		mem.write32(addr, pdpObject.getId());
        		addr += 4;

        		/** MAC address */
        		pdpObject.getMacAddress().write(mem, addr);
        		addr += pdpObject.getMacAddress().sizeof();

        		/** Port */
        		mem.write16(addr, (short) pdpObject.getPort());
        		addr += 2;

        		/** Bytes received */
        		mem.write32(addr, pdpObject.getRcvdData());
        		addr += 4;
        	}

        	for (int nextAddr = buf.getAddress(); nextAddr < addr; nextAddr += objectInfoSize) {
        		if (nextAddr + objectInfoSize >= addr) {
        			// Last one
        			mem.write32(nextAddr, 0);
        		} else {
        			// Pointer to next one
        			mem.write32(nextAddr, nextAddr + objectInfoSize);
        		}
        	}
        }

        return 0;
    }

    /**
     * Open a PTP connection
     *
     * @param srcmac - Local mac address.
     * @param srcport - Local port.
     * @param destmac - Destination mac.
     * @param destport - Destination port
     * @param bufsize - Socket buffer size
     * @param delay - Interval between retrying (microseconds).
     * @param count - Number of retries.
     * @param unk1 - Pass 0.
     *
     * @return A socket ID on success, < 0 on error.
     */
    @HLEFunction(nid = 0x877F6D66, version = 150)
    public int sceNetAdhocPtpOpen(pspNetMacAddress srcMacAddress, int srcPort, pspNetMacAddress destMacAddress, int destPort, int bufSize, int retryDelay, int retryCount, int unk1) {
		checkInitialized();

    	PtpObject ptpObject = getNetworkAdapter().createPtpObject();
    	ptpObject.setMacAddress(srcMacAddress);
    	ptpObject.setPort(srcPort);
    	ptpObject.setDestMacAddress(destMacAddress);
    	ptpObject.setDestPort(destPort);
    	ptpObject.setBufSize(bufSize);
    	ptpObject.setRetryDelay(retryDelay);
    	ptpObject.setRetryCount(retryCount);
    	int result = ptpObject.open();
    	if (result != 0) {
    		// Open failed...
    		ptpObject.delete();
    		return result;
    	}

    	ptpObjects.put(ptpObject.getId(), ptpObject);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetAdhocPtpOpen: returning id=%d", ptpObject.getId()));
		}

		return ptpObject.getId();
    }

    /**
     * Wait for connection created by sceNetAdhocPtpOpen()
     *
     * @param id - A socket ID.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xFC6FC07B, version = 150)
    public int sceNetAdhocPtpConnect(@CheckArgument("checkPtpId") int id, int timeout, int nonblock) {
        return ptpObjects.get(id).connect(timeout, nonblock);
    }

    /**
     * Wait for an incoming PTP connection
     *
     * @param srcmac - Local mac address.
     * @param srcport - Local port.
     * @param bufsize - Socket buffer size
     * @param delay - Interval between retrying (microseconds).
     * @param count - Number of retries.
     * @param queue - Connection queue length.
     * @param unk1 - Pass 0.
     *
     * @return A socket ID on success, < 0 on error.
     */
    @HLEFunction(nid = 0xE08BDAC1, version = 150)
    public int sceNetAdhocPtpListen(pspNetMacAddress srcMacAddress, int srcPort, int bufSize, int retryDelay, int retryCount, int queue, int unk1) {
		checkInitialized();

    	PtpObject ptpObject = getNetworkAdapter().createPtpObject();
    	ptpObject.setMacAddress(srcMacAddress);
    	ptpObject.setPort(srcPort);
    	ptpObject.setBufSize(bufSize);
    	ptpObject.setRetryDelay(retryDelay);
    	ptpObject.setRetryCount(retryCount);
    	ptpObject.setQueue(queue);
    	int result = ptpObject.listen();
    	if (result != 0) {
    		// Listen failed...
    		ptpObject.delete();
    		return result;
    	}

    	ptpObjects.put(ptpObject.getId(), ptpObject);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetAdhocPtpListen: returning id=%d", ptpObject.getId()));
		}

    	return ptpObject.getId();
    }

    /**
     * Accept an incoming PTP connection
     *
     * @param id - A socket ID.
     * @param mac - Connecting peers mac.
     * @param port - Connecting peers port.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x9DF81198, version = 150)
    public int sceNetAdhocPtpAccept(@CheckArgument("checkPtpId") int id, @CanBeNull TPointer peerMacAddr, @CanBeNull TPointer16 peerPortAddr, int timeout, int nonblock) {
        return ptpObjects.get(id).accept(peerMacAddr.getAddress(), peerPortAddr.getAddress(), timeout, nonblock);
    }

    /**
     * Send data
     *
     * @param id - A socket ID.
     * @param data - Data to send.
     * @param datasize - Size of the data.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return 0 success, < 0 on error.
     */
    @HLEFunction(nid = 0x4DA4C788, version = 150)
    public int sceNetAdhocPtpSend(@CheckArgument("checkPtpId") int id, TPointer data, TPointer32 dataSizeAddr, int timeout, int nonblock) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("Send data: %s", Utilities.getMemoryDump(data.getAddress(), dataSizeAddr.getValue())));
		}

        return ptpObjects.get(id).send(data.getAddress(), dataSizeAddr, timeout, nonblock);
    }

    /**
     * Receive data
     *
     * @param id - A socket ID.
     * @param data - Buffer for the received data.
     * @param datasize - Size of the data received.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x8BEA2B3E, version = 150)
    public int sceNetAdhocPtpRecv(@CheckArgument("checkPtpId") int id, TPointer data, TPointer32 dataSizeAddr, int timeout, int nonblock) {
        return ptpObjects.get(id).recv(data, dataSizeAddr, timeout, nonblock);
    }

    /**
     * Wait for data in the buffer to be sent
     *
     * @param id - A socket ID.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return A socket ID on success, < 0 on error.
     */
    @HLEFunction(nid = 0x9AC2EEAC, version = 150)
    public int sceNetAdhocPtpFlush(@CheckArgument("checkPtpId") int id, int timeout, int nonblock) {
        // Faked: return successful completion
        return 0;
    }

    /**
     * Close a socket
     *
     * @param id - A socket ID.
     * @param unk1 - Pass 0.
     *
     * @return A socket ID on success, < 0 on error.
     */
    @HLEFunction(nid = 0x157E6225, version = 150)
    public int sceNetAdhocPtpClose(@CheckArgument("checkPtpId") int id, int unknown) {
        ptpObjects.remove(id).delete();

        return 0;
    }

    /**
     * Get the status of all PTP objects
     *
     * @param size - Pointer to the size of the stat array (e.g 20 for one structure)
     * @param stat - Pointer to a list of ::ptpStatStruct structures.
     *
     * typedef struct ptpStatStruct
     * {
     *    struct ptpStatStruct *next; // Pointer to next PTP structure in list
     *    int ptpId;                  // ptp ID
     *    unsigned char mac[6];       // MAC address
     *    unsigned char peermac[6];   // Peer MAC address
     *    unsigned short port;        // Port
     *    unsigned short peerport;    // Peer Port
     *    unsigned int sentData;      // Bytes sent
     *    unsigned int rcvdData;      // Bytes received
     *    int unk1;                   // Unknown
     * } ptpStatStruct;
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xB9685118, version = 150)
    public int sceNetAdhocGetPtpStat(TPointer32 sizeAddr, @CanBeNull TPointer buf) {
		checkInitialized();

    	final int objectInfoSize = 36;

    	// Return size required
    	sizeAddr.setValue(objectInfoSize * ptpObjects.size());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetAdhocGetPtpStat returning size=%d", sizeAddr.getValue()));
    	}

    	if (buf.isNotNull()) {
        	Memory mem = Memory.getInstance();
        	int addr = buf.getAddress();
        	int endAddr = addr + sizeAddr.getValue();
        	pspNetMacAddress nonExistingDestMacAddress = new pspNetMacAddress();
        	for (int pdpId : ptpObjects.keySet()) {
        		PtpObject ptpObject = ptpObjects.get(pdpId);

        		// Check if enough space available to write the next structure
        		if (addr + objectInfoSize > endAddr || ptpObject == null) {
        			break;
        		}

        		try {
					ptpObject.update();
				} catch (IOException e) {
					// Ignore error
				}

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetAdhocGetPtpStat returning %s at 0x%08X", ptpObject, addr));
        		}

        		/** Pointer to next PDP structure in list: will be written later */
        		addr += 4;

        		/** ptp ID */
        		mem.write32(addr, ptpObject.getId());
        		addr += 4;

        		/** MAC address */
        		ptpObject.getMacAddress().write(mem, addr);
        		addr += ptpObject.getMacAddress().sizeof();

        		/** Dest MAC address */
        		if (ptpObject.getDestMacAddress() != null) {
        			ptpObject.getDestMacAddress().write(mem, addr);
        			addr += ptpObject.getDestMacAddress().sizeof();
        		} else {
        			nonExistingDestMacAddress.write(mem, addr);
        			addr += nonExistingDestMacAddress.sizeof();
        		}

        		/** Port */
        		mem.write16(addr, (short) ptpObject.getPort());
        		addr += 2;

        		/** Dest Port */
        		mem.write16(addr, (short) ptpObject.getDestPort());
        		addr += 2;

        		/** Bytes sent */
        		mem.write32(addr, ptpObject.getSentData());
        		addr += 4;

        		/** Bytes received */
        		mem.write32(addr, ptpObject.getRcvdData());
        		addr += 4;

        		/** Unknown */
        		mem.write32(addr, 4); // PSP seems to return value 4 here
        		addr += 4;
        	}

        	for (int nextAddr = buf.getAddress(); nextAddr < addr; nextAddr += objectInfoSize) {
        		if (nextAddr + objectInfoSize >= addr) {
        			// Last one
        			mem.write32(nextAddr, 0);
        		} else {
        			// Pointer to next one
        			mem.write32(nextAddr, nextAddr + objectInfoSize);
        		}
        	}
        }

        return 0;
    }

    /**
     * Create own game object type data.
     *
     * @param data - A pointer to the game object data.
     * @param size - Size of the game data.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x7F75C338, version = 150)
    public int sceNetAdhocGameModeCreateMaster(TPointer data, int size) {
		checkInitialized();

        masterGameModeArea = new GameModeArea(data.getAddress(), size);
        startGameMode();

        return 0;
    }

    /**
     * Create peer game object type data.
     *
     * @param mac - The mac address of the peer.
     * @param data - A pointer to the game object data.
     * @param size - Size of the game data.
     *
     * @return The id of the replica on success, < 0 on error.
     */
    @HLEFunction(nid = 0x3278AB0C, version = 150)
    public int sceNetAdhocGameModeCreateReplica(pspNetMacAddress macAddress, TPointer data, int size) {
		checkInitialized();

        boolean found = false;
        int result = 0;
        for (GameModeArea gameModeArea : replicaGameModeAreas) {
        	if (isSameMacAddress(gameModeArea.macAddress.macAddress, macAddress.macAddress)) {
        		// Updating the exiting replica
        		gameModeArea.addr = data.getAddress();
        		gameModeArea.size = size;
        		result = gameModeArea.id;
        		found = true;
        		break;
        	}
        }

        if (!found) {
        	GameModeArea gameModeArea = new GameModeArea(macAddress, data.getAddress(), size);
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("Adding GameMode Replica %s", gameModeArea));
        	}
        	result = gameModeArea.id;
        	replicaGameModeAreas.add(gameModeArea);
        }

        startGameMode();

        return result;
    }

    /**
     * Update own game object type data.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x98C204C8, version = 150)
    public int sceNetAdhocGameModeUpdateMaster() {
		checkInitialized();

        if (masterGameModeArea != null) {
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("Master Game Mode Area: %s", Utilities.getMemoryDump(masterGameModeArea.addr, masterGameModeArea.size)));
        	}
        	masterGameModeArea.setNewData();
        }

        return 0;
    }

    /**
     * Update peer game object type data.
     *
     * @param id - The id of the replica returned by sceNetAdhocGameModeCreateReplica.
     * @param info - address of GameModeUpdateInfo structure.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xFA324B4E, version = 150)
    public int sceNetAdhocGameModeUpdateReplica(int id, @CanBeNull TPointer infoAddr) {
		checkInitialized();

        for (GameModeArea gameModeArea : replicaGameModeAreas) {
        	if (gameModeArea.id == id) {
        		GameModeUpdateInfo gameModeUpdateInfo = new GameModeUpdateInfo();
        		if (infoAddr.isNotNull()) {
        			gameModeUpdateInfo.read(infoAddr);
        		}

        		if (gameModeArea.hasNewData()) {
        			if (log.isDebugEnabled()) {
        				log.debug(String.format("Updating GameMode Area with new data: %s", gameModeArea));
        			}
        			gameModeArea.writeNewData();
        			gameModeArea.resetNewData();
                	if (log.isTraceEnabled()) {
                		log.trace(String.format("Replica GameMode Area updated: %s", Utilities.getMemoryDump(gameModeArea.addr, gameModeArea.size)));
                	}
        			gameModeUpdateInfo.updated = 1;
        		} else {
        			gameModeUpdateInfo.updated = 0;
        		}

        		if (infoAddr.getAddress() != 0) {
        			gameModeUpdateInfo.timeStamp = gameModeArea.getUpdateTimestamp();
        			gameModeUpdateInfo.write(Memory.getInstance());
        		}
        		break;
        	}
        }

        return 0;
    }

    /**
     * Delete own game object type data.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xA0229362, version = 150)
    public int sceNetAdhocGameModeDeleteMaster() {
		checkInitialized();

        masterGameModeArea = null;
        if (replicaGameModeAreas.size() <= 0) {
        	stopGameMode();
        }

        return 0;
    }

    /**
     * Delete peer game object type data.
     *
     * @param id - The id of the replica.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x0B2228E9, version = 150)
    public int sceNetAdhocGameModeDeleteReplica(int id) {
		checkInitialized();

        for (GameModeArea gameModeArea : replicaGameModeAreas) {
        	if (gameModeArea.id == id) {
        		replicaGameModeAreas.remove(gameModeArea);
        		break;
        	}
        }

        if (replicaGameModeAreas.size() <= 0 && masterGameModeArea == null) {
        	stopGameMode();
        }

        return 0;
    }
}