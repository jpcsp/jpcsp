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

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.StringInfo;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.Processor;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceNetAdhocctlPeerInfo;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.hardware.Wlan;
import jpcsp.network.INetworkAdapter;

import org.apache.log4j.Logger;

@HLELogging
public class sceNetAdhocctl extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetAdhocctl");

    @Override
    public String getName() {
        return "sceNetAdhocctl";
    }

    public static final int PSP_ADHOCCTL_EVENT_ERROR = 0;
    public static final int PSP_ADHOCCTL_EVENT_CONNECTED = 1;
    public static final int PSP_ADHOCCTL_EVENT_DISCONNECTED = 2;
    public static final int PSP_ADHOCCTL_EVENT_SCAN = 3;
    public static final int PSP_ADHOCCTL_EVENT_GAME = 4;
    public static final int PSP_ADHOCCTL_EVENT_DISCOVER = 5;
    public static final int PSP_ADHOCCTL_EVENT_WOL = 6;
    public static final int PSP_ADHOCCTL_EVENT_WOL_INTERRUPTED = 7;

    public static final int PSP_ADHOCCTL_STATE_DISCONNECTED = 0;
    public static final int PSP_ADHOCCTL_STATE_CONNECTED = 1;
    public static final int PSP_ADHOCCTL_STATE_SCAN = 2;
    public static final int PSP_ADHOCCTL_STATE_GAME = 3;
    public static final int PSP_ADHOCCTL_STATE_DISCOVER = 4;
    public static final int PSP_ADHOCCTL_STATE_WOL = 5;

    public static final int PSP_ADHOCCTL_MODE_NORMAL = 0;
    public static final int PSP_ADHOCCTL_MODE_GAMEMODE = 1;
    public static final int PSP_ADHOCCTL_MODE_NONE = -1;

    public static final int NICK_NAME_LENGTH = 128;
    public static final int GROUP_NAME_LENGTH = 8;
    public static final int IBSS_NAME_LENGTH = 6;
    public static final int ADHOC_ID_LENGTH = 9;
    public static final int MAX_GAME_MODE_MACS = 16;

    private boolean isInitialized;
	protected int adhocctlCurrentState;
    protected String adhocctlCurrentGroup;
    protected String adhocctlCurrentIBSS;
    protected int adhocctlCurrentMode;
    protected int adhocctlCurrentChannel;
    protected int adhocctlCurrentType;
    protected String adhocctlCurrentAdhocID;
    protected boolean doTerminate;
    protected SceKernelThreadInfo adhocctlThread;
    private boolean doScan;
    private volatile long scanStartMillis;
    private static final int SCAN_DURATION_MILLIS = 700;
    private boolean doDisconnect;
    private boolean doJoin;
    private boolean gameModeJoinComplete;
    protected LinkedList<AdhocctlPeer> peers;
    protected LinkedList<AdhocctlNetwork> networks;
    protected LinkedList<pspNetMacAddress> gameModeMacs;
    protected LinkedList<pspNetMacAddress> requiredGameModeMacs;
    protected INetworkAdapter networkAdapter;
	private long connectCompleteTimestamp;
	// Some games have problems when the PSP_ADHOCCTL_EVENT_CONNECTED
	// is sent too quickly after connecting to a network.
	// The connection will be set CONNECTED with a delay of 200ms.
	private static final int CONNECT_COMPLETE_DELAY_MILLIS = 200;

    private HashMap<Integer, AdhocctlHandler> adhocctlIdMap = new HashMap<Integer, AdhocctlHandler>();
    private static final String adhocctlHandlerIdPurpose = "sceNetAdhocctl-Handler";

    protected class AdhocctlHandler {
        private int entryAddr;
        private int currentEvent;
        private int currentError;
        private int currentArg;
        private final int id;

        private AdhocctlHandler(int addr, int arg) {
            entryAddr = addr;
            currentArg = arg;
            // PSP returns a handler ID between 0 and 3
            id = SceUidManager.getNewId(adhocctlHandlerIdPurpose, 0, 3);
        }

        protected void triggerAdhocctlHandler() {
            SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
            if (thread != null) {
                Modules.ThreadManForUserModule.executeCallback(thread, entryAddr, null, true, currentEvent, currentError, currentArg);
            }
        }

        protected int getId() {
            return id;
        }

        protected void setEvent(int event) {
            currentEvent = event;
        }

        protected void setError(int error) {
            currentError = error;
        }

        protected void delete() {
        	SceUidManager.releaseId(id, adhocctlHandlerIdPurpose);
        }

		@Override
		public String toString() {
			return String.format("AdhocctlHandler[id=%d, entry=0x%08X, arg=0x%08X]", getId(), entryAddr, currentArg);
		}
    }

    protected static class AdhocctlPeer {
    	public String nickName;
    	public byte[] macAddress;
    	public long timestamp;

    	public AdhocctlPeer(String nickName, byte[] macAddress) {
    		this.nickName = nickName;
    		this.macAddress = macAddress.clone();
    		updateTimestamp();
    	}

    	public void updateTimestamp() {
    		timestamp = getCurrentTimestamp();
    	}

    	public boolean equals(String nickName, byte[] macAddress) {
    		return nickName.equals(this.nickName) && sceNetAdhoc.isSameMacAddress(macAddress, this.macAddress);
    	}

    	public boolean equals(byte[] macAddress) {
    		return sceNetAdhoc.isSameMacAddress(macAddress, this.macAddress);
    	}

    	@Override
		public String toString() {
			return String.format("nickName='%s', macAddress=%s, timestamp=%d", nickName, sceNet.convertMacAddressToString(macAddress), timestamp);
		}
    }

    protected static class AdhocctlNetwork {
    	/** Channel number */
    	public int channel;
    	/** Name of the connection (alphanumeric characters only) */
    	public String name;
    	/** The BSSID */
    	public String bssid;
    	/** mode */
    	public int mode;

    	public boolean equals(int channel, String name, String bssid, int mode) {
    		return channel == this.channel && name.equals(this.name) && bssid.equals(this.bssid) && mode == this.mode;
    	}

		@Override
		public String toString() {
			return String.format("AdhocctlNetwork[channel=%d, name='%s', bssid='%s', mode=%d]", channel, name, bssid, mode);
		}
    }

	@Override
	public void start() {
		peers = new LinkedList<sceNetAdhocctl.AdhocctlPeer>();
		networks = new LinkedList<sceNetAdhocctl.AdhocctlNetwork>();
		gameModeMacs = new LinkedList<pspNetMacAddress>();
		requiredGameModeMacs = new LinkedList<pspNetMacAddress>();
		adhocctlCurrentIBSS = "Jpcsp";
		adhocctlCurrentMode = PSP_ADHOCCTL_MODE_NONE;
		adhocctlCurrentChannel = Wlan.getAdhocChannel();
		isInitialized = false;
		networkAdapter = Modules.sceNetModule.getNetworkAdapter();

		super.start();
	}

	protected static long getCurrentTimestamp() {
		return Emulator.getClock().microTime();
	}

	protected void checkInitialized() {
		if (!isInitialized) {
			throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOCCTL_NOT_INITIALIZED);
		}
	}

	public void hleNetAdhocctlAddGameModeMac(byte[] macAddr) {
		for (pspNetMacAddress macAddress : gameModeMacs) {
			if (sceNetAdhoc.isSameMacAddress(macAddress.macAddress, macAddr)) {
				// Already in the list
				return;
			}
		}

		pspNetMacAddress macAddress = new pspNetMacAddress();
		macAddress.setMacAddress(macAddr);
		gameModeMacs.add(macAddress);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Adding new Game Mode MAC: %s", macAddress));
		}
	}

	private void doConnect() {
		if (adhocctlCurrentGroup != null && networkAdapter.isConnectComplete()) {
			long now = Emulator.getClock().currentTimeMillis();
			if (now >= connectCompleteTimestamp) {
				if (adhocctlCurrentMode == PSP_ADHOCCTL_MODE_GAMEMODE) {
	    			setState(PSP_ADHOCCTL_STATE_GAME);
	    			notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_GAME, 0);
				} else {
					setState(PSP_ADHOCCTL_STATE_CONNECTED);
					notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_CONNECTED, 0);
				}
				doJoin = false;
			}
		}
	}

	private int getAdhocctlThreadPollDelay() {
		// Poll every 100ms
		final int quickPollDelay = 100000;

		if (adhocctlCurrentState == PSP_ADHOCCTL_STATE_SCAN) {
			// Scanning...
			return quickPollDelay;
		}
		if (doJoin) {
			// Joining...
			return quickPollDelay;
		}
		if (adhocctlCurrentState == PSP_ADHOCCTL_STATE_DISCONNECTED && adhocctlCurrentGroup != null) {
			// Connecting or Creating...
			return quickPollDelay;
		}

		// Poll every 500ms
		return 500000;
	}

	public void hleNetAdhocctlThread(Processor processor) {
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;

    	if (log.isDebugEnabled()) {
    		log.debug("hleNetAdhocctlThread");
    	}

    	if (doTerminate) {
    		setState(PSP_ADHOCCTL_STATE_DISCONNECTED);
    		notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_DISCONNECTED, 0);
    		setGroupName(null, PSP_ADHOCCTL_MODE_NONE);
    	} else if (doDisconnect) {
    		setState(PSP_ADHOCCTL_STATE_DISCONNECTED);
    		notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_DISCONNECTED, 0);
    		setGroupName(null, PSP_ADHOCCTL_MODE_NONE);
    		doDisconnect = false;
    	} else if (doScan) {
    		setState(PSP_ADHOCCTL_STATE_SCAN);
    		scanStartMillis = Emulator.getClock().milliTime();
            doScan = false;
    	} else if (doJoin) {
    		if (adhocctlCurrentMode == PSP_ADHOCCTL_MODE_GAMEMODE) {
    			// Join complete when all the required MACs have joined
    			if (requiredGameModeMacs.size() > 0 && gameModeMacs.size() >= requiredGameModeMacs.size()) {
    				if (log.isDebugEnabled()) {
    					log.debug(String.format("All GameMode MACs have joined, GameMode Join is now complete"));
    				}
    				hleNetAdhocctlSetGameModeJoinComplete(true);

    				// Make sure the list of game mode MACs is in the same order as the one
    				// given at sceNetAdhocctlCreateEnterGameMode
    				gameModeMacs.clear();
    				gameModeMacs.addAll(requiredGameModeMacs);
    			}

    			if (gameModeJoinComplete) {
    				doConnect();
    			} else {
    				// Add own MAC to list of game mode MACs
        			hleNetAdhocctlAddGameModeMac(Wlan.getMacAddress());
    			}
    		} else {
    			doConnect();
    		}
    	} else if (adhocctlCurrentState == PSP_ADHOCCTL_STATE_DISCONNECTED) {
			doConnect();
    	}

    	if (adhocctlCurrentState == PSP_ADHOCCTL_STATE_CONNECTED || adhocctlCurrentState == PSP_ADHOCCTL_STATE_GAME || doJoin) {
    		networkAdapter.updatePeers();
    	} else if (adhocctlCurrentState == PSP_ADHOCCTL_STATE_SCAN) {
    		networkAdapter.updatePeers();

    		// End of SCAN?
    		long now = Emulator.getClock().milliTime();
    		if (now - scanStartMillis > SCAN_DURATION_MILLIS) {
    			// Return to DISCONNECTED state and trigger SCAN event
    			setState(PSP_ADHOCCTL_STATE_DISCONNECTED);
    	        notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_SCAN, 0);
    		}
    	}

    	if (doTerminate) {
			// Exit thread with status 0
			processor.cpu._v0 = 0;
			threadMan.hleKernelExitDeleteThread();
			adhocctlThread = null;
			doTerminate = false;
    	} else {
			threadMan.hleKernelDelayThread(getAdhocctlThreadPollDelay(), false);
    	}
    }

    protected void setState(int state) {
    	adhocctlCurrentState = state;
    }

    protected void setGroupName(String groupName, int mode) {
    	adhocctlCurrentGroup = groupName;
    	adhocctlCurrentMode = mode;
    	gameModeJoinComplete = false;
		gameModeMacs.clear();

		if (groupName != null) {
			// Some games have problems when the PSP_ADHOCCTL_EVENT_CONNECTED
			// is sent too quickly after connecting to a network.
			// The connection will be set CONNECTED with a small delay.
			connectCompleteTimestamp = Emulator.getClock().currentTimeMillis() + CONNECT_COMPLETE_DELAY_MILLIS;
		}
    }

    public void hleNetAdhocctlConnect(String groupName) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleNetAdhocctlConnect groupName='%s'", groupName));
    	}

    	if (hleNetAdhocctlGetGroupName() == null || !hleNetAdhocctlGetGroupName().equals(groupName)) {
    		setGroupName(groupName, PSP_ADHOCCTL_MODE_NORMAL);

    		networkAdapter.sceNetAdhocctlConnect();
    	}
    }

    public void hleNetAdhocctlConnectGame(String groupName) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleNetAdhocctlConnectGame groupName='%s'", groupName));
    	}
    	setGroupName(groupName, PSP_ADHOCCTL_MODE_GAMEMODE);
    }

    public int hleNetAdhocctlGetState() {
    	return adhocctlCurrentState;
    }

    protected void notifyAdhocctlHandler(int event, int error) {
        for (AdhocctlHandler handler : adhocctlIdMap.values()) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("Notifying handler %s with event=%d, error=%d", handler, event, error));
        	}
            handler.setEvent(event);
            handler.setError(error);
            handler.triggerAdhocctlHandler();
        }
    }

    public String hleNetAdhocctlGetAdhocID() {
    	return adhocctlCurrentAdhocID;
    }

    public String hleNetAdhocctlGetGroupName() {
    	return adhocctlCurrentGroup;
    }

    public String hleNetAdhocctlGetIBSS() {
    	return adhocctlCurrentIBSS;
    }

    public int hleNetAdhocctlGetMode() {
    	return adhocctlCurrentMode;
    }

    public int hleNetAdhocctlGetChannel() {
    	return adhocctlCurrentChannel;
    }

    public void hleNetAdhocctlAddNetwork(String groupName, pspNetMacAddress mac, int mode) {
    	hleNetAdhocctlAddNetwork(groupName, mac, adhocctlCurrentChannel, adhocctlCurrentIBSS, mode);
    }

    public void hleNetAdhocctlAddNetwork(String groupName, pspNetMacAddress mac, int channel, String ibss, int mode) {
		boolean found = false;
		for (AdhocctlNetwork network : networks) {
			if (network.equals(channel, groupName, ibss, mode)) {
				found = true;
				break;
			}
		}

		if (!found) {
			AdhocctlNetwork network = new AdhocctlNetwork();
			network.channel = channel;
			network.name = groupName;
			network.bssid = ibss;
			network.mode = mode;
			networks.add(network);

			if (log.isDebugEnabled()) {
    			log.debug(String.format("New network discovered %s", network));
    		}
		}
    }

    public void hleNetAdhocctlScanComplete() {
    	// Force a completion of the scan at the next run of hleNetAdhocctlThread.
    	// Note: the scan completion has to be executed from a PSP thread because it
    	// is triggering a callback.
    	scanStartMillis = 0;
    }

    public void hleNetAdhocctlAddPeer(String nickName, pspNetMacAddress mac) {
       	boolean peerFound = false;
       	for (AdhocctlPeer peer : peers) {
       		if (peer.equals(nickName, mac.macAddress)) {
       			// Update the timestamp
       			peer.updateTimestamp();
       			peerFound = true;
       			break;
       		}
       	}

       	if (!peerFound) {
    		AdhocctlPeer peer = new AdhocctlPeer(nickName, mac.macAddress);
    		peers.add(peer);

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("New peer discovered %s", peer));
    		}
    	}
	}

    public List<String> getPeersNickName() {
    	List<String> nickNames = new LinkedList<String>();
    	for (AdhocctlPeer peer : peers) {
    		nickNames.add(peer.nickName);
    	}

    	return nickNames;
    }

    public int getNumberPeers() {
    	return peers.size();
    }

    public String getPeerNickName(byte[] macAddress) {
    	for (AdhocctlPeer peer : peers) {
    		if (peer.equals(macAddress)) {
    			return peer.nickName;
    		}
    	}

    	return null;
    }

    public void hleNetAdhocctlDeletePeer(byte[] macAddress) {
    	for (AdhocctlPeer peer : peers) {
    		if (peer.equals(macAddress)) {
    			peers.remove(peer);

    			if (log.isDebugEnabled()) {
        			log.debug(String.format("Peer deleted %s", peer));
        		}
    			break;
    		}
    	}
    }

    public void hleNetAdhocctlPeerUpdateTimestamp(byte[] macAddress) {
    	for (AdhocctlPeer peer : peers) {
    		if (peer.equals(macAddress)) {
    			peer.updateTimestamp();
    			break;
    		}
    	}
    }

    public boolean isGameModeComplete() {
		// The Join for GameMode is complete when all the required MACs have joined
    	return gameModeMacs.size() >= requiredGameModeMacs.size();
    }

    public List<pspNetMacAddress> hleNetAdhocctlGetRequiredGameModeMacs() {
    	return requiredGameModeMacs;
    }

    public void hleNetAdhocctlSetGameModeJoinComplete(boolean gameModeJoinComplete) {
    	this.gameModeJoinComplete = gameModeJoinComplete;
    }

    public void hleNetAdhocctlSetGameModeMacs(byte[][] gameModeMacs) {
		// Make sure the list of game mode MACs is in the same order as the one
		// given at sceNetAdhocctlCreateEnterGameMode
		this.gameModeMacs.clear();
		for (int i = 0; i < gameModeMacs.length; i++) {
			hleNetAdhocctlAddGameModeMac(gameModeMacs[i]);
		}
    }

    public static void fillNextPointersInLinkedList(TPointer buffer, int size, int elementSize) {
    	for (int offset = 0; offset < size; offset += elementSize) {
    		if (offset + elementSize >= size) {
    			// Last one
    			buffer.setValue32(offset, 0);
    		} else {
    			// Pointer to next one
    			buffer.setValue32(offset, buffer.getAddress() + offset + elementSize);
    		}
    	}
    }

    /**
     * Initialise the Adhoc control library
     *
     * @param stacksize - Stack size of the adhocctl thread. Set to 0x2000
     * @param priority - Priority of the adhocctl thread. Set to 0x30
     * @param product - Pass a filled in ::productStruct
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xE26F226E, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlInit(int stackSize, int priority, @CanBeNull TPointer product) {
    	if (isInitialized) {
    		return SceKernelErrors.ERROR_NET_ADHOCCTL_ALREADY_INITIALIZED;
    	}

    	if (product.isNotNull()) {
            adhocctlCurrentType = product.getValue32(0); // 0 - Commercial type / 1 - Debug type.
            adhocctlCurrentAdhocID = product.getStringNZ(4, ADHOC_ID_LENGTH);
            if (log.isDebugEnabled()) {
            	log.debug(String.format("Found product data: type=%d, AdhocID='%s'", adhocctlCurrentType, adhocctlCurrentAdhocID));
            }
        }

        setState(PSP_ADHOCCTL_STATE_DISCONNECTED);
        doTerminate = false;
        doScan = false;
        doDisconnect = false;
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        adhocctlThread = threadMan.hleKernelCreateThread("SceNetAdhocctl", ThreadManForUser.NET_ADHOC_CTL_LOOP_ADDRESS, priority, stackSize, 0, 0);
        threadMan.hleKernelStartThread(adhocctlThread, 0, 0, adhocctlThread.gpReg_addr);

        networkAdapter.sceNetAdhocctlInit();

        isInitialized = true;

        return 0;
    }

    /**
     * Terminate the Adhoc control library
     *
     * @return 0 on success, < on error.
     */
    @HLEFunction(nid = 0x9D689E13, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlTerm() {
        doTerminate = true;
        isInitialized = false;

        networkAdapter.sceNetAdhocctlTerm();

        return 0;
    }

    /**
     * Connect to the Adhoc control
     *
     * @param name - The name of the connection (maximum 8 alphanumeric characters).
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x0AD043ED, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlConnect(@CanBeNull @StringInfo(maxLength=GROUP_NAME_LENGTH) PspString groupName) {
    	checkInitialized();

        hleNetAdhocctlConnect(groupName.getString());

        return 0;
    }

    /**
     * Connect to the Adhoc control (as a host)
     *
     * @param name - The name of the connection (maximum 8 alphanumeric characters).
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xEC0635C1, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlCreate(@CanBeNull @StringInfo(maxLength=GROUP_NAME_LENGTH) PspString groupName) {
    	checkInitialized();

        setGroupName(groupName.getString(), PSP_ADHOCCTL_MODE_NORMAL);

        networkAdapter.sceNetAdhocctlCreate();

        return 0;
    }

    /**
     * Connect to the Adhoc control (as a client)
     *
     * @param scaninfo - A valid ::SceNetAdhocctlScanInfo struct that has been filled by sceNetAchocctlGetScanInfo
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x5E7F79C9, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlJoin(TPointer scanInfoAddr) {
    	checkInitialized();

        if (scanInfoAddr.isAddressGood()) {
            // IBSS Data field.
            int nextAddr = scanInfoAddr.getValue32(0);  // Next group data.
            int ch = scanInfoAddr.getValue32(4);
            String groupName = scanInfoAddr.getStringNZ(8, GROUP_NAME_LENGTH);
            String bssID = scanInfoAddr.getStringNZ(16, IBSS_NAME_LENGTH);
            int mode = scanInfoAddr.getValue32(24);

            if (log.isDebugEnabled()) {
            	log.debug(String.format("sceNetAdhocctlJoin nextAddr 0x%08X, ch %d, groupName '%s', bssID '%s', mode %d", nextAddr, ch, groupName, bssID, mode));
            }
            doJoin = true;
            setGroupName(groupName, PSP_ADHOCCTL_MODE_NORMAL);

            networkAdapter.sceNetAdhocctlJoin();
        }

        return 0;
    }

    /**
     * Scan the adhoc channels
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x08FFF7A0, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlScan() {
    	checkInitialized();

        doScan = true;

        networkAdapter.sceNetAdhocctlScan();

        return 0;
    }

    /**
     * Disconnect from the Adhoc control
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x34401D65, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlDisconnect() {
    	checkInitialized();

        doDisconnect = true;

        networkAdapter.sceNetAdhocctlDisconnect();

        // Delete all the peers
        while (!peers.isEmpty()) {
        	AdhocctlPeer peer = peers.get(0);
        	hleNetAdhocctlDeletePeer(peer.macAddress);
        }

        return 0;
    }

    /**
     * Register an adhoc event handler
     *
     * @param handler - The event handler.
     * @param unknown - Pass NULL.
     *
     * @return Handler id on success, < 0 on error.
     */
    @HLEFunction(nid = 0x20B317A0, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlAddHandler(TPointer adhocctlHandlerAddr, int adhocctlHandlerArg) {
    	checkInitialized();

    	AdhocctlHandler adhocctlHandler = new AdhocctlHandler(adhocctlHandlerAddr.getAddress(), adhocctlHandlerArg);
        int id = adhocctlHandler.getId();
        if (id == SceUidManager.INVALID_ID) {
        	return SceKernelErrors.ERROR_NET_ADHOCCTL_TOO_MANY_HANDLERS;
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceNetAdhocctlAddHandler returning id=0x%X", id));
        }
        adhocctlIdMap.put(id, adhocctlHandler);

        return id;
    }

    /**
     * Delete an adhoc event handler
     *
     * @param id - The handler id as returned by sceNetAdhocctlAddHandler.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x6402490B, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlDelHandler(int id) {
    	checkInitialized();

        AdhocctlHandler handler = adhocctlIdMap.remove(id);
        if (handler != null) {
        	handler.delete();
        }

        return 0;
    }

    /**
     * Get the state of the Adhoc control
     *
     * @param event - Pointer to an integer to receive the status. Can continue when it becomes 1.
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x75ECD386, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlGetState(TPointer32 stateAddr) {
    	checkInitialized();

        stateAddr.setValue(adhocctlCurrentState);

        return 0;
    }

    /**
     * Get the adhoc ID
     *
     * @param product - A pointer to a  ::productStruct
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x362CBE8F, version = 150)
    public int sceNetAdhocctlGetAdhocId(TPointer addr) {
    	checkInitialized();

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetAdhocctlGetAdhocId returning type=%d, adhocID='%s'", adhocctlCurrentType, adhocctlCurrentAdhocID));
    	}
    	addr.setValue32(0, adhocctlCurrentType);
    	addr.setStringNZ(4, ADHOC_ID_LENGTH, adhocctlCurrentAdhocID);

        return 0;
    }

    /**
     * Get a list of peers
     *
     * @param length - The length of the list.
     * @param buf - An allocated area of size length.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xE162CB14, version = 150)
    public int sceNetAdhocctlGetPeerList(TPointer32 sizeAddr, @CanBeNull TPointer buf) {
    	checkInitialized();

        int size = sizeAddr.getValue();
		SceNetAdhocctlPeerInfo peerInfo = new SceNetAdhocctlPeerInfo();
    	sizeAddr.setValue(peerInfo.sizeof() * peers.size());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetAdhocctlGetPeerList returning size=%d", sizeAddr.getValue()));
    	}

    	if (buf.isNotNull()) {
        	int offset = 0;
        	for (AdhocctlPeer peer : peers) {
        		// Check if enough space available to write the next structure
        		if (offset + peerInfo.sizeof() > size || peer == null) {
        			break;
        		}

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetAdhocctlGetPeerList returning %s at 0x%08X", peer, buf.getAddress() + offset));
        		}

        		peerInfo.nickName = peer.nickName;
        		peerInfo.macAddress = new pspNetMacAddress();
        		peerInfo.macAddress.setMacAddress(peer.macAddress);
        		peerInfo.timestamp = peer.timestamp;
        		peerInfo.write(buf, offset);

        		offset += peerInfo.sizeof();
        	}

        	fillNextPointersInLinkedList(buf, offset, peerInfo.sizeof());
        }

        return 0;
    }

    /**
     * Get peer information
     *
     * @param mac - The mac address of the peer.
     * @param size - Size of peerinfo.
     * @param peerinfo - Pointer to store the information.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x8DB83FDC, version = 150)
    public int sceNetAdhocctlGetPeerInfo(pspNetMacAddress macAddress, int size, TPointer peerInfoAddr) {
    	checkInitialized();

    	int result = SceKernelErrors.ERROR_NET_ADHOC_NO_ENTRY;
    	if (sceNetAdhoc.isMyMacAddress(macAddress.macAddress)) {
    		SceNetAdhocctlPeerInfo peerInfo = new SceNetAdhocctlPeerInfo();
    		peerInfo.nickName = sceUtility.getSystemParamNickname();
    		peerInfo.macAddress = new pspNetMacAddress(Wlan.getMacAddress());
    		peerInfo.timestamp = getCurrentTimestamp();
    		peerInfo.write(peerInfoAddr);
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceNetAdhocctlGetPeerInfo for own MAC address, returning %s", peerInfo));
    		}
    		result = 0;
    	} else {
	        for (AdhocctlPeer peer : peers) {
	        	if (macAddress.equals(peer.macAddress)) {
	        		SceNetAdhocctlPeerInfo peerInfo = new SceNetAdhocctlPeerInfo();
	        		peerInfo.nickName = peer.nickName;
	        		peerInfo.macAddress = new pspNetMacAddress(peer.macAddress);
	        		peerInfo.timestamp = peer.timestamp;
	        		peerInfo.write(peerInfoAddr);
	        		if (log.isDebugEnabled()) {
	        			log.debug(String.format("sceNetAdhocctlGetPeerInfo returning %s", peerInfo));
	        		}
	        		result = 0;
	        		break;
	        	}
	        }
    	}
    	if (result != 0) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceNetAdhocctlGetPeerInfo returning 0x%08X", result));
    		}
    	}

        return result;
    }

    /**
     * Get mac address from nickname
     *
     * @param nickname - The nickname.
     * @param length - The length of the list.
     * @param buf - An allocated area of size length.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x99560ABE, version = 150)
    public int sceNetAdhocctlGetAddrByName(@StringInfo(maxLength=NICK_NAME_LENGTH) PspString nickName, TPointer32 sizeAddr, @CanBeNull TPointer buf) {
    	checkInitialized();

        // Search for peers matching the given nick name
        LinkedList<AdhocctlPeer> matchingPeers = new LinkedList<sceNetAdhocctl.AdhocctlPeer>();
        for (AdhocctlPeer peer : peers) {
        	if (nickName.equals(peer.nickName)) {
        		matchingPeers.add(peer);
        	}
        }

        int size = sizeAddr.getValue();
        SceNetAdhocctlPeerInfo peerInfo = new SceNetAdhocctlPeerInfo();
    	sizeAddr.setValue(peerInfo.sizeof() * matchingPeers.size());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetAdhocctlGetAddrByName returning size=%d", sizeAddr.getValue()));
    	}

    	if (buf.isNotNull()) {
        	int offset = 0;
        	for (AdhocctlPeer peer : matchingPeers) {
        		// Check if enough space available to write the next structure
        		if (offset + peerInfo.sizeof() > size) {
        			break;
        		}

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetAdhocctlGetAddrByName returning %s at 0x%08X", peer, buf.getAddress() + offset));
        		}

        		peerInfo.nickName = peer.nickName;
        		peerInfo.macAddress = new pspNetMacAddress();
        		peerInfo.macAddress.setMacAddress(peer.macAddress);
        		peerInfo.timestamp = peer.timestamp;
        		peerInfo.write(buf, offset);

        		offset += peerInfo.sizeof();
        	}

        	fillNextPointersInLinkedList(buf, offset, peerInfo.sizeof());
        }

        return 0;
    }

    /**
     * Get nickname from a mac address
     *
     * @param mac - The mac address.
     * @param nickname - Pointer to a char buffer where the nickname will be stored.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x8916C003, version = 150)
    public int sceNetAdhocctlGetNameByAddr(pspNetMacAddress macAddress, TPointer nickNameAddr) {
    	checkInitialized();

        String nickName = "";
        for (AdhocctlPeer peer : peers) {
        	if (sceNetAdhoc.isSameMacAddress(macAddress.macAddress, peer.macAddress)) {
        		nickName = peer.nickName;
        	}
        }

        nickNameAddr.setStringNZ(NICK_NAME_LENGTH, nickName);

        return 0;
    }

    /**
     * Get Adhocctl parameter
     *
     * @param params - Pointer to a ::SceNetAdhocctlParams
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xDED9D28E, version = 150)
    public int sceNetAdhocctlGetParameter(TPointer paramsAddr) {
    	checkInitialized();

    	if (log.isDebugEnabled()) {
        	log.debug(String.format("sceNetAdhocctlGetParameter returning channel=%d, group='%s', IBSS='%s', nickName='%s'", adhocctlCurrentChannel, adhocctlCurrentGroup, adhocctlCurrentIBSS, sceUtility.getSystemParamNickname()));
        }
        paramsAddr.setValue32(0, adhocctlCurrentChannel);
        paramsAddr.setStringNZ(4, GROUP_NAME_LENGTH, adhocctlCurrentGroup);
        paramsAddr.setStringNZ(12, IBSS_NAME_LENGTH, adhocctlCurrentIBSS);
        paramsAddr.setStringNZ(18, NICK_NAME_LENGTH, sceUtility.getSystemParamNickname());

        return 0;
    }

    /**
     * Get the results of a scan
     *
     * @param length - The length of the list.
     * @param buf - An allocated area of size length.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x81AEE1BE, version = 150)
    public int sceNetAdhocctlGetScanInfo(TPointer32 sizeAddr, @CanBeNull TPointer buf) {
    	checkInitialized();

    	final int scanInfoSize = 28;

    	int size = sizeAddr.getValue();
    	sizeAddr.setValue(scanInfoSize * networks.size());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNetAdhocctlGetScanInfo returning size=%d", sizeAddr.getValue()));
    	}

    	if (buf.isNotNull()) {
        	int offset = 0;
        	for (AdhocctlNetwork network : networks) {
        		// Check if enough space available to write the next structure
        		if (offset + scanInfoSize > size || network == null) {
        			break;
        		}

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetAdhocctlGetScanInfo returning %s at 0x%08X", network, buf.getAddress() + offset));
        		}

        		/** Pointer to next Network structure in list: will be written later */
        		offset += 4;

        		/** Channel number */
        		buf.setValue32(offset, network.channel);
        		offset += 4;

        		/** Name of the connection (alphanumeric characters only) */
        		buf.setStringNZ(offset, GROUP_NAME_LENGTH, network.name);
        		offset += GROUP_NAME_LENGTH;

        		/** The BSSID */
        		buf.setStringNZ(offset, IBSS_NAME_LENGTH, network.bssid);
        		offset += IBSS_NAME_LENGTH;

        		/** Padding */
        		buf.setValue16(offset, (short) 0);
        		offset += 2;

        		/** Mode */
        		buf.setValue32(offset, network.mode);
        		offset += 4;
        	}

        	fillNextPointersInLinkedList(buf, offset, scanInfoSize);
        }

        return 0;
    }

    /**
     * Connect to the Adhoc control game mode (as a host)
     *
     * @param name - The name of the connection (maximum 8 alphanumeric characters).
     * @param unknown - Pass 1.
     * @param num - The total number of players (including the host).
     * @param macs - A pointer to a list of the participating mac addresses, host first, then clients.
     * @param timeout - Timeout in microseconds.
     * @param unknown2 - pass 0.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xA5C055CE, version = 150)
    public int sceNetAdhocctlCreateEnterGameMode(@CanBeNull @StringInfo(maxLength=GROUP_NAME_LENGTH) PspString groupName, int unknown, int num, TPointer macsAddr, int timeout, int unknown2) {
    	checkInitialized();

        gameModeMacs.clear();
        requiredGameModeMacs.clear();
        for (int i = 0; i < num; i++) {
            pspNetMacAddress macAddress = new pspNetMacAddress();
        	macAddress.read(macsAddr, i * macAddress.sizeof());
        	requiredGameModeMacs.add(macAddress);
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceNetAdhocctlCreateEnterGameMode macAddress#%d=%s", i, macAddress));
        	}
        }

        // We have to wait for all the MACs to have joined to go into CONNECTED state
        doJoin = true;
        setGroupName(groupName.getString(), PSP_ADHOCCTL_MODE_GAMEMODE);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB0B80E80, version = 150)
    public int sceNetAdhocctlCreateEnterGameModeMin() {
    	checkInitialized();

    	return 0;
    }

    /**
     * Connect to the Adhoc control game mode (as a client)
     *
     * @param name - The name of the connection (maximum 8 alphanumeric characters).
     * @param hostmac - The mac address of the host.
     * @param timeout - Timeout in microseconds.
     * @param unknown - pass 0.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x1FF89745, version = 150)
    public int sceNetAdhocctlJoinEnterGameMode(@StringInfo(maxLength=GROUP_NAME_LENGTH) PspString groupName, pspNetMacAddress macAddress, int timeout, int unknown) {
    	checkInitialized();

        doJoin = true;
        setGroupName(groupName.getString(), PSP_ADHOCCTL_MODE_GAMEMODE);

        return 0;
    }

    /**
     * Exit game mode.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xCF8E084D, version = 150)
    public int sceNetAdhocctlExitGameMode() {
    	checkInitialized();

        doDisconnect = true;
        Modules.sceNetAdhocModule.hleExitGameMode();

        return 0;
    }

    /**
     * Get game mode information
     *
     * @param gamemodeinfo - Pointer to store the info.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x5A014CE0, version = 150)
    public int sceNetAdhocctlGetGameModeInfo(TPointer gameModeInfoAddr) {
    	checkInitialized();

        int offset = 0;
        gameModeInfoAddr.setValue32(offset, gameModeMacs.size());
        offset += 4;
        for (pspNetMacAddress macAddress : gameModeMacs) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceNetAdhocctlGetGameModeInfo returning %s", macAddress));
        	}
        	macAddress.write(gameModeInfoAddr, offset);
        	offset += macAddress.sizeof();
        }

        return 0;
    }
}