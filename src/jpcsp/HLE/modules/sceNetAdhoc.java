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

import static jpcsp.HLE.modules.sceNetAdhocctl.fillNextPointersInLinkedList;
import static jpcsp.hardware.Wlan.hasLocalInetAddress;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;
import jpcsp.HLE.TPointer32;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructureVariableLength;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.Modules;
import jpcsp.hardware.Wlan;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.adhoc.AdhocMessage;
import jpcsp.network.adhoc.AdhocSocket;
import jpcsp.network.adhoc.PdpObject;
import jpcsp.network.adhoc.PtpObject;
import jpcsp.network.upnp.AutoDetectJpcsp;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceNetAdhoc extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetAdhoc");

    // For test purpose when running 2 different Jpcsp instances on the same computer:
    // one computer has to have netClientPortShift=0 and netServerPortShift=100,
    // the other computer, netClientPortShift=100 and netServerPortShift=0.
    private int netClientPortShift = 0;
    private int netServerPortShift = 0;

    // Period to update the Game Mode
    protected static final int GAME_MODE_UPDATE_MICROS = 12000;
    protected static final int GAME_MODE_TIMEOUT_MICROS = GAME_MODE_UPDATE_MICROS / 2;

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
	protected GameModeArea masterGameModeArea;
	protected LinkedList<GameModeArea> replicaGameModeAreas;
	private static final String replicaIdPurpose = "sceNetAdhoc-Replica";
    private static final int adhocGameModePort = 31000;
    private AdhocSocket gameModeSocket;
    private boolean isInitialized;
    private GameModeUpdateThread gameModeUpdateThread;

    // The same message can be received over multiple broadcasting interfaces.
	// Remember which messages have already been received to ensure that each
    // message is only being processed once, even if received multiple times
    private Map<byte[], Set<Integer>> alreadyReceivedAdhocMessageIds = new HashMap<byte[], Set<Integer>>();
    private Set<byte[]> uniqueMacAdresses = new HashSet<byte[]>();
    private int currentAdhocMessageId;

    private class ClientPortShiftSettingsListener extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			if (value) {
				setNetClientPortShift(100);
			} else {
				setNetClientPortShift(0);
			}
		}
    }

    private class ServerPortShiftSettingsListener extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			if (value) {
				setNetServerPortShift(100);
			} else {
				setNetServerPortShift(0);
			}
		}
    }

    public static class GameModeArea {
    	public pspNetMacAddress macAddress;
    	public TPointer addr;
    	public int size;
    	public int id;
    	private byte[] data;
    	private byte[] newData;
    	private long updateTimestamp;

    	public GameModeArea(TPointer addr, int size) {
    		this.addr = addr;
    		this.size = size;
    		id = -1;

    		readData();
    	}

    	public GameModeArea(pspNetMacAddress macAddress, TPointer addr, int size) {
    		this.macAddress = macAddress;
    		this.addr = addr;
    		this.size = size;
    		id = SceUidManager.getNewUid(replicaIdPurpose);

    		readData();
    	}

    	public void delete() {
    		if (id >= 0) {
    			SceUidManager.releaseUid(id, replicaIdPurpose);
    			id = -1;
    		}
    	}

    	public synchronized void setNewData(byte[] newData) {
    		updateTimestamp = getNow();
    		this.newData = newData;
    	}

    	private synchronized void readData() {
    		data = addr.getArray8(size);
    	}

    	public void setNewData() {
    		setNewData(addr.getArray8(size));
    	}

    	public synchronized void resetNewData() {
    		if (newData != null) {
    			data = newData;
        		newData = null;
    		}
    	}

    	public synchronized byte[] getNewData() {
    		return newData;
    	}

    	public synchronized byte[] getData() {
    		return data;
    	}

    	public synchronized boolean hasNewData() {
    		return newData != null;
    	}

    	public synchronized void writeNewData() {
    		if (newData != null) {
    			addr.setArray(newData, size);
    		}
    	}

    	public long getUpdateTimestamp() {
    		return updateTimestamp;
    	}

    	@Override
		public String toString() {
			if (macAddress == null) {
				return String.format("Master GameModeArea addr=%s, size=0x%X", addr, size);
			}
			return String.format("Replica GameModeArea id=0x%X, macAddress=%s, addr=%s, size=0x%X", id, macAddress, addr, size);
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
			return String.format("PollId[id=0x%X, events=0x%X(%s), revents=0x%X(%s)]", id, events, getPollEventName(events), revents, getPollEventName(revents));
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

	private class GameModeUpdateThread extends Thread {
		private boolean exit;

		@Override
		public void run() {
			RuntimeContext.setLog4jMDC();

			long nextStart = getNow();

			while (!exit) {
				long start = nextStart;
				if (getNow() >= nextStart) {
					start = getNow();
				} else {
					while (!exit) {
						long now = getNow();
						if (now >= nextStart) {
							break;
						}
						Utilities.sleep((int) (nextStart - now));
					}
				}

				if (exit) {
					break;
				}

				nextStart = hleGameModeUpdate(start);
			}
		}

		public void exit() {
			exit = true;
		}
	}

	@Override
	public void start() {
    	setSettingsListener("emu.netClientPortShift", new ClientPortShiftSettingsListener());
    	setSettingsListener("emu.netServerPortShift", new ServerPortShiftSettingsListener());

		AutoDetectJpcsp autoDetectJpcsp = AutoDetectJpcsp.getInstance();
		if (autoDetectJpcsp != null) {
			autoDetectJpcsp.discoverOtherJpcspInBackground();
		}

    	pdpObjects = new HashMap<Integer, PdpObject>();
	    ptpObjects = new HashMap<Integer, PtpObject>();
	    currentFreePort = 0x4000;
	    replicaGameModeAreas = new LinkedList<sceNetAdhoc.GameModeArea>();
	    isInitialized = false;
	    alreadyReceivedAdhocMessageIds.clear();
	    currentAdhocMessageId = 1;

	    super.start();
	}

    public static long getNow() {
    	return Emulator.getClock().microTime();
    }

    public int getNewAdhocMessageId() {
    	return currentAdhocMessageId++;
    }

    private byte[] getUniqueFromMacAddress(AdhocMessage adhocMessage) {
    	byte[] fromMacAddress = adhocMessage.getFromMacAddress();
    	for (byte[] uniqueMacAddress : uniqueMacAdresses) {
    		if (Arrays.equals(uniqueMacAddress, fromMacAddress)) {
    			return uniqueMacAddress;
    		}
    	}

    	uniqueMacAdresses.add(fromMacAddress);
    	return fromMacAddress;
    }

    public boolean isAlreadyReceived(AdhocMessage adhocMessage) {
    	byte[] uniqueFromMacAddress = getUniqueFromMacAddress(adhocMessage);
    	Set<Integer> alreadyReceivedIds = alreadyReceivedAdhocMessageIds.get(uniqueFromMacAddress);
    	if (alreadyReceivedIds == null) {
    		return false;
    	}
    	return alreadyReceivedIds.contains(adhocMessage.getId());
    }

    public void setAlreadyReceived(AdhocMessage adhocMessage) {
    	byte[] uniqueFromMacAddress = getUniqueFromMacAddress(adhocMessage);
    	Set<Integer> alreadyReceivedIds = alreadyReceivedAdhocMessageIds.get(uniqueFromMacAddress);
    	if (alreadyReceivedIds == null) {
    		alreadyReceivedIds = new HashSet<Integer>();
    		alreadyReceivedAdhocMessageIds.put(uniqueFromMacAddress, alreadyReceivedIds);
    	}
    	alreadyReceivedIds.add(adhocMessage.getId());
    }

    public void setNetClientPortShift(int netClientPortShift) {
    	if (!hasLocalInetAddress()) {
	    	this.netClientPortShift = netClientPortShift;
	    	log.info(String.format("Using netClientPortShift=%d", netClientPortShift));
    	}
    }

    public void setNetServerPortShift(int netServerPortShift) {
    	if (!hasLocalInetAddress()) {
	    	this.netServerPortShift = netServerPortShift;
	    	log.info(String.format("Using netServerPortShift=%d", netServerPortShift));
    	}
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

    private boolean sendGameModeBuffer(byte[] toMacAddress, byte[] buffer, int length) throws IOException {
    	boolean success = false;

    	AdhocMessage adhocGameModeMessage = getNetworkAdapter().createAdhocGameModeMessage(buffer, length);
    	adhocGameModeMessage.setFromMacAddress(Wlan.getMacAddress());
    	adhocGameModeMessage.setToMacAddress(toMacAddress);
    	SocketAddress socketAddress[] = Modules.sceNetAdhocModule.getMultiSocketAddress(toMacAddress, Modules.sceNetAdhocModule.getRealPortFromClientPort(toMacAddress, adhocGameModePort));
    	for (int i = 0; i < socketAddress.length; i++) {
			try {
				gameModeSocket.send(socketAddress[i], adhocGameModeMessage);
				success = true;

				if (log.isDebugEnabled()) {
		    		log.debug(String.format("GameMode message sent to %s: %s", socketAddress[i], adhocGameModeMessage));
		    	}
			} catch (SocketException e) {
				// Ignore "Network is unreachable"
				if (log.isDebugEnabled()) {
					log.debug(String.format("hleGameModeUpdate failed for %s", socketAddress[i]));
					log.debug("hleGameModeUpdate", e);
				}
			}
    	}

    	return success;
    }

    private void sendMasterGameModeArea() throws IOException {
    	byte[] data;
    	if (masterGameModeArea == null) {
    		// Send dummy data
    		data = new byte[1];
    	} else if (masterGameModeArea.hasNewData()) {
    		data = masterGameModeArea.getNewData();
        	masterGameModeArea.resetNewData();
    	} else {
    		data = masterGameModeArea.getData();
    	}
    	sendGameModeBuffer(ANY_MAC_ADDRESS, data, data.length);
    }

    private void receiveReplicaGameModeArea(long start) throws IOException {
    	while (true) {
			try {
				byte[] bytes = new byte[10000];
		    	int length = gameModeSocket.receive(bytes, bytes.length);
		    	AdhocMessage adhocGameModeMessage = getNetworkAdapter().createAdhocGameModeMessage(bytes, length);
	
		    	if (adhocGameModeMessage.isForMe(gameModeSocket.getReceivedPort(), gameModeSocket.getReceivedAddress())) {
			    	if (log.isDebugEnabled()) {
			    		log.debug(String.format("GameMode received: %s", adhocGameModeMessage));
			    	}
		
		    		if (length == 0) {
		    			if (log.isDebugEnabled()) {
		    				log.debug(String.format("Received request to send Master GameMode Area %s", masterGameModeArea));
		    			}
		    			sendMasterGameModeArea();
//		    			break;
		    		} else if (length == 1) {
		    			// Dummy data received, Master Game Mode Area of the sender was not yet ready
		    		} else {
				    	for (GameModeArea gameModeArea : replicaGameModeAreas) {
				    		if (isSameMacAddress(gameModeArea.macAddress.macAddress, adhocGameModeMessage.getFromMacAddress())) {
				    			if (log.isDebugEnabled()) {
				    				log.debug(String.format("Received new Data for GameMode Area %s", gameModeArea));
				    			}
				    			gameModeArea.setNewData(adhocGameModeMessage.getData());
//				    			break;
				    		}
				    	}
		    		}
		    	}
			} catch (SocketTimeoutException e) {
				// Timeout exceeded, stop waiting
				if (getNow() - start > GAME_MODE_TIMEOUT_MICROS) {
					break;
				}

				// No message available yet, try again
				if (log.isDebugEnabled()) {
					log.debug(String.format("receiveReplicaGameModeArea waiting for message..."));
				}
			}
    	}
    }

    public long hleGameModeUpdate(long start) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleGameModeUpdate starting at start=%d", start));
    	}

    	long nextStart = start + GAME_MODE_UPDATE_MICROS;

    	try {
			if (gameModeSocket == null) {
				gameModeSocket = getNetworkAdapter().createAdhocGameModeSocket();
				gameModeSocket.bind(Modules.sceNetAdhocModule.getRealPortFromServerPort(adhocGameModePort));
			}

			List<pspNetMacAddress> gameModeMacs = Modules.sceNetAdhocctlModule.requiredGameModeMacs;
			if (gameModeMacs.size() > 0) {
				// Sending first my own master game mode area
				sendMasterGameModeArea();

				// Then ask in turn, each Game Mode MAC to send its master game mode area
				for (pspNetMacAddress macAddress : gameModeMacs) {
					if (!isMyMacAddress(macAddress.macAddress)) {
						if (log.isDebugEnabled()) {
							log.debug(String.format("hleGameModeUpdate sending request to %s to broadcast its Master GameMode Area", macAddress));
						}
						byte[] command = new byte[0];
						if (sendGameModeBuffer(macAddress.macAddress, command, command.length)) {
							receiveReplicaGameModeArea(start);
						}
					}
				}
			} else {
				int numberGameModeMacs = Modules.sceNetAdhocctlModule.hleNetAdhocctlGetGameModeMacs().size();
				for (int i = 0; i < numberGameModeMacs; i++) {
					receiveReplicaGameModeArea(start);
				}
			}
		} catch (IOException e) {
			log.error("hleGameModeUpdate", e);
		}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleGameModeUpdate ending with nextStart=%d", nextStart));
    	}

    	return nextStart;
    }

    protected void startGameMode() {
    	if (gameModeUpdateThread == null) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Starting GameMode"));
    		}
    		gameModeUpdateThread = new GameModeUpdateThread();
    		gameModeUpdateThread.setName("sceNetAdhoc GameMode Updade Thread");
    		gameModeUpdateThread.setDaemon(true);
    		gameModeUpdateThread.start();
    	}
    }

    protected void stopGameMode() {
    	if (gameModeUpdateThread != null) {
    		gameModeUpdateThread.exit();
    		gameModeUpdateThread = null;
    	}

    	if (gameModeSocket != null) {
    		try {
				gameModeSocket.close();
			} catch (IOException e) {
				log.error("stopGameMode", e);
			}
    		gameModeSocket = null;
    	}
    }

    public SocketAddress getSocketAddress(byte[] macAddress, int realPort) throws UnknownHostException {
    	return getNetworkAdapter().getSocketAddress(macAddress, realPort);
	}

    public SocketAddress[] getMultiSocketAddress(byte[] macAddress, int realPort) throws UnknownHostException {
    	return getNetworkAdapter().getMultiSocketAddress(macAddress, realPort);
	}

	public static boolean isSameMacAddress(byte[] macAddress1, byte[] macAddress2) {
		return pspNetMacAddress.equals(macAddress1, macAddress2);
	}

	public static boolean isAnyMacAddress(byte[] macAddress) {
		return pspNetMacAddress.isAnyMacAddress(macAddress);
	}

	public static boolean isMyMacAddress(byte[] macAddress) {
		return pspNetMacAddress.isMyMacAddress(macAddress);
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
				log.debug(String.format("Invalid Pdp Id=0x%X", pdpId));
			}
			throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOC_INVALID_SOCKET_ID);
		}

		return pdpId;
	}

	public int checkPtpId(int ptpId) {
		checkInitialized();

		if (!ptpObjects.containsKey(ptpId)) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Invalid Ptp Id=0x%X", ptpId));
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

        	log.info(String.format("sceNetAdhocPollSocket pollId[0x%X]=%s", i, pollId));
        }

        return countEvents;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x73BFD52D, version = 150)
    public int sceNetAdhocSetSocketAlert(int id, int flags) {
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
				log.debug(String.format("sceNetAdhocPdpCreate: using free port 0x%X", port));
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
    public int sceNetAdhocPdpSend(@CheckArgument("checkPdpId") int id, pspNetMacAddress destMacAddress, int port, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer data, int len, int timeout, int nonblock) {
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
    public int sceNetAdhocPdpRecv(@CheckArgument("checkPdpId") int id, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=6, usage=Usage.out) TPointer srcMacAddr, @BufferInfo(usage=Usage.out) TPointer16 portAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=128, usage=Usage.out) TPointer data, @BufferInfo(usage=Usage.inout) TPointer32 dataLengthAddr, int timeout, int nonblock) {
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

    	int size = sizeAddr.getValue();
    	sizeAddr.setValue(objectInfoSize * pdpObjects.size());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetAdhocGetPdpStat returning size=0x%X", sizeAddr.getValue()));
    	}

    	if (buf.isNotNull()) {
    		int offset = 0;
        	for (int pdpId : pdpObjects.keySet()) {
        		PdpObject pdpObject = pdpObjects.get(pdpId);

        		// Check if enough space available to write the next structure
        		if (offset + objectInfoSize > size || pdpObject == null) {
        			break;
        		}

        		try {
					pdpObject.update();
				} catch (IOException e) {
					// Ignore error
				}

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetAdhocGetPdpStat returning %s at 0x%08X", pdpObject, buf.getAddress() + offset));
        		}

        		/** Pointer to next PDP structure in list: will be written later */
        		offset += 4;

        		/** pdp ID */
        		buf.setValue32(offset, pdpObject.getId());
        		offset += 4;

        		/** MAC address */
        		pdpObject.getMacAddress().write(buf.getMemory(), buf.getAddress() + offset);
        		offset += pdpObject.getMacAddress().sizeof();

        		/** Port */
        		buf.setValue16(offset, (short) pdpObject.getPort());
        		offset += 2;

        		/** Bytes received */
        		buf.setValue32(offset, pdpObject.getRcvdData());
        		offset += 4;
        	}

        	fillNextPointersInLinkedList(buf, offset, objectInfoSize);
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
			log.debug(String.format("sceNetAdhocPtpOpen: returning id=0x%X", ptpObject.getId()));
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
			log.debug(String.format("sceNetAdhocPtpListen: returning id=0x%X", ptpObject.getId()));
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
    public int sceNetAdhocPtpSend(@CheckArgument("checkPtpId") int id, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 32, usage = Usage.in) TPointer data, @BufferInfo(usage = Usage.in) TPointer32 dataSizeAddr, int timeout, int nonblock) {
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
    public int sceNetAdhocPtpRecv(@CheckArgument("checkPtpId") int id, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 32, usage = Usage.out) TPointer data, @BufferInfo(usage = Usage.out) TPointer32 dataSizeAddr, int timeout, int nonblock) {
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
    public int sceNetAdhocGetPtpStat(@BufferInfo(usage = Usage.inout) TPointer32 sizeAddr, @CanBeNull @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 2 * 36, usage = Usage.out) TPointer buf) {
		checkInitialized();

    	final int objectInfoSize = 36;

    	int size = sizeAddr.getValue();
    	// Return size required
    	sizeAddr.setValue(objectInfoSize * ptpObjects.size());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetAdhocGetPtpStat returning size=0x%X", sizeAddr.getValue()));
    	}

    	if (buf.isNotNull()) {
    		int offset = 0;
        	pspNetMacAddress nonExistingDestMacAddress = new pspNetMacAddress();
        	for (int pdpId : ptpObjects.keySet()) {
        		PtpObject ptpObject = ptpObjects.get(pdpId);

        		// Check if enough space available to write the next structure
        		if (offset + objectInfoSize > size || ptpObject == null) {
        			break;
        		}

        		try {
					ptpObject.update();
				} catch (IOException e) {
					// Ignore error
				}

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetAdhocGetPtpStat returning %s at 0x%08X", ptpObject, buf.getAddress() + offset));
        		}

        		/** Offset 0: Pointer to next PDP structure in list: will be written later */
        		offset += 4;

        		/** Offset 4: ptp ID */
        		buf.setValue32(offset, ptpObject.getId());
        		offset += 4;

        		/** Offset 8: MAC address */
        		ptpObject.getMacAddress().write(buf.getMemory(), buf.getAddress() + offset);
        		offset += ptpObject.getMacAddress().sizeof();

        		/** Offset 14: Dest MAC address */
        		if (ptpObject.getDestMacAddress() != null) {
        			ptpObject.getDestMacAddress().write(buf.getMemory(), buf.getAddress() + offset);
        			offset += ptpObject.getDestMacAddress().sizeof();
        		} else {
        			nonExistingDestMacAddress.write(buf.getMemory(), buf.getAddress() + offset);
        			offset += nonExistingDestMacAddress.sizeof();
        		}

        		/** Offset 20: Port */
        		buf.setValue16(offset, (short) ptpObject.getPort());
        		offset += 2;

        		/** Offset 22: Dest Port */
        		buf.setValue16(offset, (short) ptpObject.getDestPort());
        		offset += 2;

        		/** Offset 24: Bytes sent */
        		buf.setValue32(offset, ptpObject.getSentData());
        		offset += 4;

        		/** Offset 28: Bytes received */
        		buf.setValue32(offset, ptpObject.getRcvdData());
        		offset += 4;

        		/** Offset 32: Status of connection, possible values 0..4, unknown meaning */
        		buf.setValue32(offset, 4);
        		offset += 4;
        	}

        	fillNextPointersInLinkedList(buf, offset, objectInfoSize);
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

        masterGameModeArea = new GameModeArea(data, size);
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
        		// Updating the existing replica
        		gameModeArea.addr = data;
        		gameModeArea.size = size;
        		result = gameModeArea.id;
        		found = true;
        		break;
        	}
        }

        if (!found) {
        	GameModeArea gameModeArea = new GameModeArea(macAddress, data, size);
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
    public int sceNetAdhocGameModeUpdateReplica(int id, @BufferInfo(lengthInfo = LengthInfo.variableLength, usage = Usage.out) @CanBeNull TPointer infoAddr) {
		checkInitialized();

        for (GameModeArea gameModeArea : replicaGameModeAreas) {
        	if (gameModeArea.id == id) {
        		GameModeUpdateInfo gameModeUpdateInfo = new GameModeUpdateInfo();
    			gameModeUpdateInfo.read(infoAddr);

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

    			gameModeUpdateInfo.timeStamp = gameModeArea.getUpdateTimestamp();
    			gameModeUpdateInfo.write(infoAddr);
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