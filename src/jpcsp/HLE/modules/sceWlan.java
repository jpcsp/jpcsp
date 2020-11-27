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

import static jpcsp.HLE.HLEModuleManager.HLESyscallNid;
import static jpcsp.HLE.Modules.sceNetIfhandleModule;
import static jpcsp.HLE.Modules.sceWlanModule;
import static jpcsp.HLE.kernel.managers.SceUidManager.INVALID_ID;
import static jpcsp.HLE.kernel.types.SceNetIfMessage.TYPE_MULTICAST_ANY;
import static jpcsp.HLE.kernel.types.SceNetIfMessage.TYPE_MULTICAST_GROUP;
import static jpcsp.HLE.kernel.types.SceNetIfMessage.TYPE_SHORT_MESSAGE;
import static jpcsp.HLE.modules.SysMemUserForUser.KERNEL_PARTITION_ID;
import static jpcsp.scheduler.Scheduler.getNow;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.NIDMapper;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.managers.SystemTimeManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceNetIfHandle;
import jpcsp.HLE.kernel.types.SceNetIfMessage;
import jpcsp.HLE.kernel.types.SceNetWlanMessage;
import jpcsp.HLE.kernel.types.SceNetWlanScanInfo;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.Modules;
import jpcsp.hardware.Wlan;
import jpcsp.network.IWlanAdapter;
import jpcsp.network.WlanAdapterFactory;
import jpcsp.network.accesspoint.AccessPoint;
import jpcsp.network.accesspoint.IAccessPointCallback;
import jpcsp.network.protocols.EtherFrame;
import jpcsp.util.HLEUtilities;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceWlan extends HLEModule implements IAccessPointCallback {
    public static Logger log = Modules.getLogger("sceWlan");
    public static final int IOCTL_CMD_UNKNOWN_0x2 = 0x2;
    public static final int IOCTL_CMD_START_SCANNING = 0x34;
    public static final int IOCTL_CMD_CREATE = 0x35;
    public static final int IOCTL_CMD_CONNECT = 0x36;
    public static final int IOCTL_CMD_GET_INFO = 0x37;
    public static final int IOCTL_CMD_DISCONNECT = 0x38;
    public static final int IOCTL_CMD_UNKNOWN_0x42 = 0x42;
    public static final int IOCTL_CMD_ENTER_GAME_MODE = 0x44;
    public static final int IOCTL_CMD_SET_WEP_KEY = 0x47;
    public static final int WLAN_MODE_INFRASTRUCTURE = 1;
    public static final int WLAN_MODE_ADHOC = 2;
    private static final int wlanThreadPollingDelayUs = 12000; // 12ms
    private static final int wlanScanActionDelayUs = 50000; // 50ms
    private static final int wlanConnectActionDelayUs = 50000; // 50ms
    private static final int wlanCreateActionDelayUs = 50000; // 50ms
    private static final int wlanDisconnectActionDelayUs = 50000; // 50ms
    public static final int[] channels = new int[] { 1, 6, 11 };
    private int joinedChannel;
    private TPointer wlanHandleAddr;
    private int wlanThreadUid;
    private int unknownValue1;
    private int unknownValue2;
    private int unknownValue3;
    private boolean isGameMode;
    private List<pspNetMacAddress> activeMacAddresses;
    private List<GameModeState> gameModeStates;
    private int gameModeDataLength;
    private String[] channelSSIDs;
    private int[] channelModes;
    private int wlanDropRate;
    private int wlanDropDuration;
    private AccessPoint accessPoint;
	private boolean scanInProgress;
	private int scanCallCount;
	private long scanCallTimestamp;
	private TPointer scanHandleAddr;
	private TPointer scanInputAddr;
	private TPointer scanOutputAddr;
	private int WLAN_LOOP_ADDRESS;
	private int WLAN_UP_CALLBACK_ADDRESS;
	private int WLAN_DOWN_CALLBACK_ADDRESS;
	private int WLAN_SEND_CALLBACK_ADDRESS;
	private int WLAN_IOCTL_CALLBACK_ADDRESS;
	private IWlanAdapter wlanAdapter;

    private static class GameModeState {
    	public long timeStamp;
    	public boolean updated;
    	public pspNetMacAddress macAddress;
    	public byte[] data;
    	public int dataLength;

    	public GameModeState(pspNetMacAddress macAddress) {
    		this.macAddress = macAddress;
    		dataLength = Modules.sceWlanModule.gameModeDataLength;
    		data = new byte[dataLength];
    	}

    	public void doUpdate() {
    		updated = true;
    		timeStamp = SystemTimeManager.getSystemTime();
    	}

		@Override
		public String toString() {
			return String.format("macAddress=%s, updated=%b, timeStamp=%d, dataLength=0x%X, data=%s", macAddress, updated, timeStamp, dataLength, Utilities.getMemoryDump(data, 0, dataLength));
		}
    }

    private class WlanConnectAction implements IAction {
    	private TPointer handleAddr;

		public WlanConnectAction(TPointer handleAddr) {
			this.handleAddr = handleAddr;
		}

		@Override
		public void execute() {
			hleWlanConnectAction(handleAddr);
		}
    }

    private class WlanCreateAction implements IAction {
    	private TPointer handleAddr;

		public WlanCreateAction(TPointer handleAddr) {
			this.handleAddr = handleAddr;
		}

		@Override
		public void execute() {
			hleWlanCreateAction(handleAddr);
		}
    }

    private class WlanDisconnectAction implements IAction {
    	private TPointer handleAddr;

		public WlanDisconnectAction(TPointer handleAddr) {
			this.handleAddr = handleAddr;
		}

		@Override
		public void execute() {
			hleWlanDisconnectAction(handleAddr);
		}
    }

    private class AfterNetIfDequeueAction implements IAction {
    	private TPointer handleAddr;

		public AfterNetIfDequeueAction(TPointer handleAddr) {
			this.handleAddr = handleAddr;
		}

		@Override
		public void execute() {
			afterNetIfDequeue(handleAddr, Emulator.getProcessor().cpu._v0);
		}
    }

    @Override
	public void start() {
		wlanThreadUid = INVALID_ID;
		activeMacAddresses = new LinkedList<pspNetMacAddress>();
		gameModeStates = new LinkedList<GameModeState>();
		gameModeDataLength = 256;
		int maxChannel = -1;
		for (int i = 0; i < channels.length; i++) {
			maxChannel = Math.max(maxChannel, channels[i]);
		}
		channelSSIDs = new String[maxChannel + 1];
		channelModes = new int[maxChannel + 1];
		joinedChannel = -1;

		WLAN_LOOP_ADDRESS = HLEUtilities.getInstance().installLoopHandler(this, "hleWlanThread");
		WLAN_UP_CALLBACK_ADDRESS = HLEUtilities.getInstance().installHLESyscall(this, "hleWlanUpCallback");
		WLAN_DOWN_CALLBACK_ADDRESS = HLEUtilities.getInstance().installHLESyscall(this, "hleWlanDownCallback");
		WLAN_SEND_CALLBACK_ADDRESS = HLEUtilities.getInstance().installHLESyscall(this, "hleWlanSendCallback");
		WLAN_IOCTL_CALLBACK_ADDRESS = HLEUtilities.getInstance().installHLESyscall(this, "hleWlanIoctlCallback");

		super.start();
	}

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public void hleWlanThread() {
    	if (log.isTraceEnabled()) {
    		log.trace(String.format("hleWlanThread isGameMode=%b", isGameMode));
    	}

    	if (wlanThreadMustExit()) {
    		// Only exit the hleWlanThread thread at this point,
    		// i.e. before we perform any action that could change the current thread
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Exiting hleWlanThread %s", Modules.ThreadManForUserModule.getCurrentThread()));
    		}
    		Modules.ThreadManForUserModule.hleKernelExitDeleteThread(0);
    	}

    	if (isGameMode && !wlanThreadMustExit()) {
    		hleWlanSendGameMode();
    	}

    	while (!wlanThreadMustExit() && hleWlanReceive()) {
			// Receive all available messages
		}

    	if (scanInProgress && !wlanThreadMustExit()) {
    		long now = getNow();
    		if (now - scanCallTimestamp >= wlanScanActionDelayUs) {
    			hleWlanScanAction();
    		}
    	}

		Modules.ThreadManForUserModule.hleKernelDelayThread(wlanThreadPollingDelayUs, true);
    }

    private boolean wlanThreadMustExit() {
    	return wlanThreadUid != Modules.ThreadManForUserModule.getCurrentThreadID();
    }

    private void hleWlanScanAction() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanScanAction scanCallCount=%d", scanCallCount));
    	}

    	try {
			wlanAdapter.wlanScan();
		} catch (IOException e) {
			log.error("hleWlanScanAction", e);
		}

		if (scanCallCount < 20) {
			// Schedule this action for 20 times (1 second)
			// before terminating the scan action
			scanCallTimestamp = getNow();
			scanCallCount++;
		} else {
			if (log.isDebugEnabled()) {
				log.debug(String.format("End of scan action:"));
				for (int ch : channels) {
					log.debug(String.format("Scan result channel#%d, ssid='%s', mode=%d", ch, channelSSIDs[ch], channelModes[ch]));
				}
			}

			TPointer addr = new TPointer(scanOutputAddr);
			for (int i = 0; i < 14; i++) {
				int channel = scanInputAddr.getValue8(10 + i);
				if (channel == 0) {
					break;
				}
				if (log.isDebugEnabled()) {
					log.debug(String.format("Scan on channel %d", channel));
				}

				if (isValidChannel(channel)) {
					String ssid = channelSSIDs[channel];
					if (ssid != null && ssid.length() > 0) {
						SceNetWlanScanInfo scanInfo = new SceNetWlanScanInfo();
						scanInfo.bssid = "Jpcsp";
						scanInfo.channel = channel;
						scanInfo.ssid = ssid;
						scanInfo.mode = channelModes[channel];
						scanInfo.beaconInterval = 1000; // Need to be != 0
						scanInfo.write(addr.getMemory(), addr.getAddress() + 4);

	    				addr.setValue32(0, addr.getAddress() + 4 + scanInfo.sizeof()); // Link to next SSID
		    			addr.add(4 + scanInfo.sizeof());
					}
				}
			}

			if (addr.getAddress() > scanOutputAddr.getAddress()) {
				addr.setValue32(-96, 0); // Last SSID, no next one
			}

	    	SceNetIfHandle handle = new SceNetIfHandle();
	    	handle.read(scanHandleAddr);

	    	scanHandleAddr = null;
	    	scanInputAddr = null;
	    	scanOutputAddr = null;
	    	scanInProgress = false;

	    	// Signal the sema when the scan has completed
    		Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);
		}
    }

    private void hleWlanConnectAction(TPointer handleAddr) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanConnectAction handleAddr=%s", handleAddr));
    	}

    	// Signal the sema that the connect/join has completed
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);
		Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);
    }

    private void hleWlanCreateAction(TPointer handleAddr) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanCreateAction handleAddr=%s", handleAddr));
    	}

		// Signal the sema that the create has completed
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);
		Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);
    }

    private void hleWlanDisconnectAction(TPointer handleAddr) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanDisconnectAction handleAddr=%s", handleAddr));
    	}

		// Signal the sema that the disconnect has completed
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);
		Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);
    }

    private boolean hleWlanReceiveMessage() {
    	boolean packetReceived = false;

		try {
	    	byte[] bytes = new byte[10000];
	    	int dataLength = wlanAdapter.receiveWlanPacket(bytes, 0, bytes.length);
	    	if (dataLength < 0) {
	    		return packetReceived;
	    	}

	    	if (log.isDebugEnabled()) {
				log.debug(String.format("hleWlanReceiveMessage message: %s", Utilities.getMemoryDump(bytes, 0, dataLength)));
			}

			packetReceived = true;

	    	SceNetIfMessage message = new SceNetIfMessage();
	    	final int size = message.sizeof() + dataLength;
	    	int allocatedAddr = Modules.sceNetIfhandleModule.hleNetMallocInternal(size);
	    	if (allocatedAddr > 0) {
	    		Memory mem = Memory.getInstance();
		    	mem.memset(allocatedAddr, (byte) 0, size);
		    	RuntimeContext.debugMemory(allocatedAddr, size);

		    	TPointer messageAddr = new TPointer(mem, allocatedAddr);
		    	TPointer data = new TPointer(messageAddr, message.sizeof());

		    	// Write the received bytes to memory
		    	data.setArray(bytes, dataLength);

		    	SceNetWlanMessage wlanMessage = new SceNetWlanMessage();
		    	wlanMessage.read(data);
		    	sceWlanModule.addActiveMacAddress(wlanMessage.srcMacAddress);
		    	sceWlanModule.addActiveMacAddress(wlanMessage.dstMacAddress);

		    	// Write the message header
		    	message.dataAddr = data.getAddress();
				message.dataLength = dataLength;
				message.unknown16 = 1;
				message.type = TYPE_SHORT_MESSAGE;
				if (wlanMessage.dstMacAddress.isMulticast()) {
					if (wlanMessage.dstMacAddress.isAnyMacAddress()) {
						message.type |= TYPE_MULTICAST_ANY;
					} else {
						message.type |= TYPE_MULTICAST_GROUP;
					}
				}
				message.totalDataLength = dataLength;
				message.write(messageAddr);

		    	if (dataLength > 0) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Notifying received message: %s", message));
						log.debug(String.format("Message WLAN: %s", wlanMessage));
						log.debug(String.format("Message data: %s", Utilities.getMemoryDump(data, dataLength)));
					}

					int sceNetIfEnqueue = NIDMapper.getInstance().getAddressByName("sceNetIfEnqueue");
					if (sceNetIfEnqueue != 0) {
						SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
						Modules.ThreadManForUserModule.executeCallback(thread, sceNetIfEnqueue, null, true, sceWlanModule.getHandleAddr().getAddress(), messageAddr.getAddress());
					}
				}
	    	}
		} catch (IOException e) {
			log.error("hleWlanReceiveMessage", e);
		}

		return packetReceived;
    }

    private boolean hleWlanReceive() {
    	if (isGameMode) {
    		return hleWlanReceiveGameMode();
    	}

    	return hleWlanReceiveMessage();
    }

    public boolean hasJoinedChannel() {
    	return joinedChannel >= 0;
    }

    public int getJoinedChannel() {
    	return joinedChannel;
    }

    public int getChannelMode(int channel) {
    	return channelModes[channel];
    }

    public String getChannelSSID(int channel) {
    	return channelSSIDs[channel];
    }

    public String getJoinedChannelSSID() {
    	if (!hasJoinedChannel()) {
    		return null;
    	}
    	return getChannelSSID(getJoinedChannel());
    }

    public AccessPoint getAccessPoint() {
    	return accessPoint;
    }

    public TPointer getHandleAddr() {
    	return wlanHandleAddr;
    }

    @Override
	public void sendPacketFromAccessPoint(byte[] buffer, int bufferLength, EtherFrame etherFrame) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sendAccessPointPacket %s", Utilities.getMemoryDump(buffer, 0, bufferLength)));
    	}

    	try {
			wlanAdapter.sendAccessPointPacket(buffer, 0, bufferLength, etherFrame);
		} catch (IOException e) {
			log.error("sendPacketFromAccessPoint", e);
		}
	}

    private GameModeState getGameModeStat(byte[] macAddress) {
    	GameModeState myGameModeState = null;
    	for (GameModeState gameModeState : gameModeStates) {
    		if (gameModeState.macAddress.equals(macAddress)) {
    			myGameModeState = gameModeState;
    			break;
    		}
    	}

    	return myGameModeState;
    }

    private GameModeState getMyGameModeState() {
    	return getGameModeStat(Wlan.getMacAddress());
    }

    public void addActiveMacAddress(pspNetMacAddress macAddress) {
    	if (!sceNetAdhoc.isAnyMacAddress(macAddress.macAddress)) {
    		if (!activeMacAddresses.contains(macAddress)) {
    			activeMacAddresses.add(macAddress);
    			gameModeStates.add(new GameModeState(macAddress));
    		}
    	}
    }

    private boolean isValidChannel(int channel) {
    	for (int i = 0; i < channels.length; i++) {
    		if (channels[i] == channel) {
    			return true;
    		}
    	}

    	return false;
    }

    public void setChannelSSID(int channel, String ssid, int mode) {
    	if (ssid != null && ssid.length() > 0 && isValidChannel(channel)) {
    		channelSSIDs[channel] = ssid;
    		channelModes[channel] = mode;
    	}
    }

    private void joinChannelSSID(int channel, String ssid, int mode) {
    	setChannelSSID(channel, ssid, mode);
    	joinedChannel = channel;
    }

    private void hleWlanSendGameMode() {
    	GameModeState myGameModeState = getMyGameModeState();
    	if (myGameModeState == null) {
    		return;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanSendGameMode sending packet: %s", Utilities.getMemoryDump(myGameModeState.data, 0, myGameModeState.dataLength)));
    	}

    	try {
			wlanAdapter.sendGameModePacket(myGameModeState.macAddress, myGameModeState.data, 0, myGameModeState.dataLength);
		} catch (IOException e) {
			log.error("hleWlanSendGameMode", e);
		}

    	myGameModeState.updated = false;
    }

    private boolean hleWlanReceiveGameMode() {
    	boolean packetReceived = false;

    	pspNetMacAddress macAddress = new pspNetMacAddress();

		try {
	    	byte[] bytes = new byte[gameModeDataLength];
	    	int dataLength = wlanAdapter.receiveGameModePacket(macAddress, bytes, 0, bytes.length);
	    	if (dataLength < 0) {
	    		return packetReceived;
	    	}

	    	if (log.isDebugEnabled()) {
				log.debug(String.format("hleWlanReceiveGameMode message: %s", Utilities.getMemoryDump(bytes, 0, dataLength)));
			}

			packetReceived = true;

			GameModeState gameModeState = getGameModeStat(macAddress.macAddress);
			if (gameModeState != null) {
				int length = Math.min(dataLength, gameModeState.dataLength);
				System.arraycopy(bytes, 0, gameModeState.data, 0, length);

				gameModeState.doUpdate();
				if (log.isDebugEnabled()) {
					log.debug(String.format("hleWlanReceiveGameMode updated GameModeState %s", gameModeState));
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("hleWlanReceiveGameMode could not find GameModeState for MAC address %s", macAddress));
				}
			}
		} catch (SocketTimeoutException e) {
			// Timeout can be ignored as we are polling
		} catch (IOException e) {
			log.error("hleWlanReceiveMessage", e);
		}

		return packetReceived;
    }

    protected void hleWlanSendMessage(TPointer handleAddr, SceNetIfMessage message) {
    	Memory mem = handleAddr.getMemory();
    	SceNetWlanMessage wlanMessage = new SceNetWlanMessage();
    	wlanMessage.read(mem, message.dataAddr);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanSendMessage message: %s: %s", message, Utilities.getMemoryDump(message.getBaseAddress(), message.sizeof())));
    		log.debug(String.format("hleWlanSendMessage WLAN message : %s", wlanMessage));
    		log.debug(String.format("hleWlanSendMessage message data: %s", Utilities.getMemoryDump(message.dataAddr + wlanMessage.sizeof(), message.dataLength - wlanMessage.sizeof())));
    	}

    	byte[] messageBytes = null;
    	while (true) {
    		if (message.dataLength > 0) {
        		int messageBytesOffset = messageBytes == null ? 0 : messageBytes.length;
    			messageBytes = Utilities.extendArray(messageBytes, message.dataLength);
    			Utilities.readBytes(message.dataAddr, message.dataLength, messageBytes, messageBytesOffset);
    		}

        	if (message.nextDataAddr == 0) {
        		break;
        	}
        	message.read(mem, message.nextDataAddr);
    	}

    	if (messageBytes != null) {
        	try {
				wlanAdapter.sendWlanPacket(messageBytes, 0, messageBytes.length);
			} catch (IOException e) {
				log.error("hleWlanSendMessage", e);
			}
    	}
    }

    private void afterNetIfDequeue(TPointer handleAddr, int returnValue) {
    	if (returnValue <= 0) {
    		log.error(String.format("sceNetIfDequeue returned error 0x%08X", returnValue));
    		return;
    	}

    	Memory mem = handleAddr.getMemory();
    	TPointer firstMessageAddr = new TPointer(mem, returnValue);
		if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanSendCallback handleAddr=%s, firstMessageAddr=%s", handleAddr, firstMessageAddr));
		}

    	SceNetIfMessage message = new SceNetIfMessage();
    	message.read(firstMessageAddr);
    	RuntimeContext.debugMemory(firstMessageAddr.getAddress(), message.sizeof());

    	hleWlanSendMessage(handleAddr, message);

    	// Call sceNetMFreem to free the received message
    	int sceNetMFreem = NIDMapper.getInstance().getAddressByName("sceNetMFreem");
    	if (sceNetMFreem != 0) {
    		Modules.ThreadManForUserModule.executeCallback(null, sceNetMFreem, null, true, firstMessageAddr.getAddress());
    	} else {
    		Modules.sceNetIfhandleModule.sceNetMFreem(firstMessageAddr);
    	}
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleWlanSendCallback(TPointer handleAddr) {
    	int sceNetIfDequeue = NIDMapper.getInstance().getAddressByName("sceNetIfDequeue");
    	if (sceNetIfDequeue != 0) {
    		Modules.ThreadManForUserModule.executeCallback(null, sceNetIfDequeue, new AfterNetIfDequeueAction(handleAddr), true, handleAddr.getAddress());
    	} else {
    		afterNetIfDequeue(handleAddr, Modules.sceNetIfhandleModule.sceNetIfDequeue(handleAddr));
    	}

    	return 0;
    }

    // Called by sceNetIfhandleIfUp
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleWlanUpCallback(TPointer handleAddr) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanUpCallback handleAddr: %s", Utilities.getMemoryDump(handleAddr.getAddress(), 44)));
    		int handleInternalAddr = handleAddr.getValue32();
    		if (handleInternalAddr != 0) {
        		log.debug(String.format("hleWlanUpCallback handleInternalAddr: %s", Utilities.getMemoryDump(handleInternalAddr, 320)));
    		}
    	}

    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanUpCallback handleAddr=%s: %s", handleAddr, handle));
    	}

    	wlanHandleAddr = handleAddr;

		wlanAdapter = WlanAdapterFactory.createWlanAdapter();
		try {
			wlanAdapter.start();
		} catch (IOException e) {
			log.error("hleWlanUpCallback", e);
		}

    	// Add my own MAC address to the active list
    	addActiveMacAddress(new pspNetMacAddress(Wlan.getMacAddress()));

    	// This thread will call hleWlanThread() in a loop
    	SceKernelThreadInfo thread = Modules.ThreadManForUserModule.hleKernelCreateThread("SceWlanHal", WLAN_LOOP_ADDRESS, 39, 2048, 0, 0, KERNEL_PARTITION_ID);
    	if (thread != null) {
    		wlanThreadUid = thread.uid;
    		Modules.ThreadManForUserModule.hleKernelStartThread(thread, 0, TPointer.NULL, 0);
    	}

    	Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);

    	return 0;
    }

    // Called by sceNetIfhandleIfDown
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleWlanDownCallback(TPointer handleAddr) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanDownCallback handleAddr: %s", Utilities.getMemoryDump(handleAddr.getAddress(), 44)));
    		int handleInternalAddr = handleAddr.getValue32();
    		if (handleInternalAddr != 0) {
        		log.debug(String.format("hleWlanDownCallback handleInternalAddr: %s", Utilities.getMemoryDump(handleInternalAddr, 320)));
    		}
    	}

    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanDownCallback handleAddr=%s: %s", handleAddr, handle));
    	}

    	// This will force the current wlan thread to exit
    	wlanThreadUid = INVALID_ID;

		try {
			wlanAdapter.stop();
		} catch (IOException e) {
			log.error("hleWlanDownCallback", e);
		}

		Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);

    	return 0;
    }

    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleWlanIoctlCallback(TPointer handleAddr, int cmd, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.in) TPointer unknown, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.in) TPointer32 buffersAddr) {
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	Memory mem = Memory.getInstance();
		int inputAddr = buffersAddr.getValue(0);
		int outputAddr = buffersAddr.getValue(4);

		if (log.isDebugEnabled()) {
    		int inputLength = 0x80;
    		int outputLength = 0x80;
    		switch (cmd) {
    			case IOCTL_CMD_START_SCANNING:
    				inputLength = 0x4C;
    				outputLength = 0x600;
    				break;
    			case IOCTL_CMD_CREATE:
    				inputLength = 0x70;
    				break;
    			case IOCTL_CMD_CONNECT:
    				inputLength = 0x70;
    				break;
    			case IOCTL_CMD_GET_INFO:
    				inputLength = 0x60;
    				break;
    			case IOCTL_CMD_ENTER_GAME_MODE:
    				inputLength = 0x50;
    				outputLength = 0x6;
    				break;
    			case IOCTL_CMD_SET_WEP_KEY:
    				inputLength = 0xA0;
    				break;
    			case IOCTL_CMD_UNKNOWN_0x42:
    				// Has no input and no output parameters
    				break;
    			case IOCTL_CMD_UNKNOWN_0x2:
    				// Has no input and no output parameters
    				break;
    		}
    		log.debug(String.format("hleWlanIoctlCallback cmd=0x%X, handleAddr=%s: %s", cmd, handleAddr, handle));
    		if (inputAddr != 0 && Memory.isAddressGood(inputAddr) && inputLength > 0) {
    			log.debug(String.format("hleWlanIoctlCallback inputAddr: %s", Utilities.getMemoryDump(inputAddr, inputLength)));
    			RuntimeContext.debugMemory(inputAddr, inputLength);
    		}
    		if (outputAddr != 0 && Memory.isAddressGood(outputAddr) && outputLength > 0) {
    			log.debug(String.format("hleWlanIoctlCallback outputAddr: %s", Utilities.getMemoryDump(outputAddr, outputLength)));
    			RuntimeContext.debugMemory(outputAddr, outputLength);
    		}
    		RuntimeContext.debugMemory(unknown.getAddress(), 32);
    	}

		boolean signalSema = true;
		int errorCode = 0;
		String ssid;
		String bssid;
		int ssidLength;
		int mode;
		int channel;
    	switch (cmd) {
    		case IOCTL_CMD_START_SCANNING: // Start scanning
    			mode = mem.read32(inputAddr + 0);
    			channel = mem.read8(inputAddr + 10);
    			// If scanning only the joined channel, it seems no
    			// scan is really started as all information are available
    			if (channel == joinedChannel && mem.read8(inputAddr + 11) == 0) {
					SceNetWlanScanInfo scanInfo = new SceNetWlanScanInfo();
					scanInfo.bssid = "Jpcsp";
					scanInfo.channel = channel;
					scanInfo.ssid = channelSSIDs[channel];
					scanInfo.mode = channelModes[channel];
					scanInfo.beaconInterval = 1000; // Need to be != 0
					scanInfo.write(handleAddr.getMemory(), outputAddr + 4);

	    			mem.write32(outputAddr, 0); // Link to next SSID
    			} else {
    				if (channel != joinedChannel) {
    	    			// When called by sceNetAdhocctlCreate() or sceNetAdhocctlConnect(),
    	    			// the SSID is available in the inputAddr structure.
    	    			// When called by sceNetAdhocctlScan(), no SSID is available
    	    			// in the inputAddr structure.
    	    			ssidLength = mem.read8(inputAddr + 24);
    	    			ssid = Utilities.readStringNZ(mem, inputAddr + 28, ssidLength);
    					setChannelSSID(channel, ssid, mode);
    				}

    				signalSema = false;
    				scanHandleAddr = handleAddr;
    				scanInputAddr = new TPointer(handleAddr.getMemory(), inputAddr);
    				scanOutputAddr = new TPointer(handleAddr.getMemory(), outputAddr);
    				scanCallCount = 0;
    				scanCallTimestamp = 0L;
    				scanInProgress = true;
    			}
    			break;
    		case IOCTL_CMD_CREATE: // Called by sceNetAdhocctlCreate()
    			channel = mem.read8(inputAddr + 6);
    			ssidLength = mem.read8(inputAddr + 7);
    			ssid = Utilities.readStringNZ(mem, inputAddr + 8, ssidLength);
    			mode = mem.read32(inputAddr + 40);
    			int unknown44 = mem.read32(inputAddr + 44); // 0x64
    			int unknown62 = mem.read16(inputAddr + 62); // 0x22
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("hleWlanIoctlCallback cmd=0x%X, channel=%d, ssid='%s', mode=0x%X, unknown44=0x%X, unknown62=0x%X", cmd, channel, ssid, mode, unknown44, unknown62));
    			}
    			joinChannelSSID(channel, ssid, mode);

    			signalSema = false;
    			Emulator.getScheduler().addAction(getNow() + wlanCreateActionDelayUs, new WlanCreateAction(handleAddr));
    			break;
    		case IOCTL_CMD_CONNECT: // Called by sceNetAdhocctlConnect() and sceNetAdhocctlJoin()
    			// Receiving as input the SSID structure returned by cmd=IOCTL_CMD_START_SCANNING
    			SceNetWlanScanInfo scanInfo = new SceNetWlanScanInfo();
    			scanInfo.read(mem, inputAddr);
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("hleWlanIoctlCallback cmd=0x%X, channel=%d, ssid='%s', mode=0x%X", cmd, scanInfo.channel, scanInfo.ssid, scanInfo.mode));
    			}
    			joinChannelSSID(scanInfo.channel, scanInfo.ssid, scanInfo.mode);

    			signalSema = false;
    			Emulator.getScheduler().addAction(getNow() + wlanConnectActionDelayUs, new WlanConnectAction(handleAddr));
    			break;
    		case IOCTL_CMD_GET_INFO: // Get joined SSID
    			// Remark: returning the joined SSID in the inputAddr!
    			mem.memset(inputAddr, (byte) 0, 40);
    			if (joinedChannel >= 0) {
        			bssid = "Jpcsp";
        			Utilities.writeStringNZ(mem, inputAddr + 0, 6, bssid);
        			mem.write8(inputAddr + 6, (byte) joinedChannel);
        			mem.write8(inputAddr + 7, (byte) channelSSIDs[joinedChannel].length());
        			Utilities.writeStringNZ(mem, inputAddr + 8, 32, channelSSIDs[joinedChannel]);
    			}
    			break;
    		case IOCTL_CMD_DISCONNECT: // Disconnect
    			isGameMode = false;
    			joinedChannel = -1;

    			signalSema = false;
    			Emulator.getScheduler().addAction(getNow() + wlanDisconnectActionDelayUs, new WlanDisconnectAction(handleAddr));
    			break;
    		case IOCTL_CMD_ENTER_GAME_MODE: // Enter Game Mode
    			pspNetMacAddress multicastMacAddress = new pspNetMacAddress();
    			multicastMacAddress.read(mem, inputAddr + 6);

    			ssidLength = mem.read8(inputAddr + 12);
    			ssid = Utilities.readStringNZ(mem,  inputAddr + 14, ssidLength);

    			pspNetMacAddress masterMacAddress = new pspNetMacAddress();
    			masterMacAddress.read(mem, outputAddr + 0);

    			if (log.isDebugEnabled()) {
    				log.debug(String.format("hleWlanIoctlCallback cmd=0x%X, ssid='%s', multicastMacAddress=%s, masterMacAddress=%s", cmd, ssid, multicastMacAddress, masterMacAddress));
    			}
    			isGameMode = true;
    			break;
    		case IOCTL_CMD_SET_WEP_KEY:
    			int unknown1 = mem.read32(inputAddr + 0); // Always 0
    			int unknown2 = mem.read32(inputAddr + 4); // Always 1
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("hleWlanIoctlCallback unknown1=0x%X, unknown2=0x%X", unknown1, unknown2));
    			}

    			int wepKeyAddr = inputAddr + 12;
    			// 4 times the same data...
    			for (int i = 0; i < 4; i++) {
    				mode = mem.read32(wepKeyAddr + 0);
    				String wepKey = Utilities.readStringNZ(wepKeyAddr + 4, 13);
    				if (log.isDebugEnabled()) {
    					log.debug(String.format("hleWlanIoctlCallback cmd=0x%X, wekKey#%d: mode=0x%X, wepKey='%s'", cmd, i, mode, wepKey));
    				}
    				wepKeyAddr += 20;
    			}
    			break;
			default:
				log.warn(String.format("hleWlanIoctlCallback unknown cmd=0x%X", cmd));
				break;
    	}
    	handle.handleInternal.errorCode = errorCode;
    	handle.write(handleAddr);
    	if (signalSema) {
    		Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);
    	}

    	return 0;
    }

    private class AfterNetCreateIfhandleEtherAction implements IAction {
    	private SceKernelThreadInfo thread;
    	private TPointer handleAddr;

		public AfterNetCreateIfhandleEtherAction(SceKernelThreadInfo thread, TPointer handleAddr) {
			this.thread = thread;
			this.handleAddr = handleAddr;
		}

		@Override
		public void execute() {
			afterNetCreateIfhandleEtherAction(thread, handleAddr);
		}
    }

    private void afterNetCreateIfhandleEtherAction(SceKernelThreadInfo thread, TPointer handleAddr) {
    	int tempMem = Modules.sceNetIfhandleModule.hleNetMallocInternal(32);
    	if (tempMem <= 0) {
    		return;
    	}

    	int macAddressAddr = tempMem;
    	int interfaceNameAddr = tempMem + 8;

    	pspNetMacAddress macAddress = new pspNetMacAddress(Wlan.getMacAddress());
    	macAddress.write(handleAddr.getMemory(), macAddressAddr);

    	Utilities.writeStringZ(handleAddr.getMemory(), interfaceNameAddr, "wlan");

    	int sceNetAttachIfhandleEther = NIDMapper.getInstance().getAddressByName("sceNetAttachIfhandleEther");
    	if (sceNetAttachIfhandleEther == 0) {
    		return;
    	}

    	Modules.ThreadManForUserModule.executeCallback(thread, sceNetAttachIfhandleEther, null, true, handleAddr.getAddress(), macAddressAddr, interfaceNameAddr);
    }

    private int createWlanInterface() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Using MAC address %s", sceNet.convertMacAddressToString(Wlan.getMacAddress())));
    	}

    	SceNetIfHandle handle = new SceNetIfHandle();
		handle.callbackArg4 = 0x11040404; // dummy callback value
		handle.upCallbackAddr = WLAN_UP_CALLBACK_ADDRESS;
		handle.downCallbackAddr = WLAN_DOWN_CALLBACK_ADDRESS;
		handle.sendCallbackAddr = WLAN_SEND_CALLBACK_ADDRESS;
		handle.ioctlCallbackAddr = WLAN_IOCTL_CALLBACK_ADDRESS;
		int handleMem = Modules.sceNetIfhandleModule.hleNetMallocInternal(handle.sizeof());
		TPointer handleAddr = new TPointer(Memory.getInstance(), handleMem);
		handle.write(handleAddr);
		RuntimeContext.debugMemory(handleAddr.getAddress(), handle.sizeof());

		int sceNetCreateIfhandleEther = NIDMapper.getInstance().getAddressByName("sceNetCreateIfhandleEther");
		if (sceNetCreateIfhandleEther == 0) {
			int result = sceNetIfhandleModule.hleNetCreateIfhandleEther(handleAddr);
			if (result < 0) {
				return result;
			}

			result = sceNetIfhandleModule.hleNetAttachIfhandleEther(handleAddr, new pspNetMacAddress(Wlan.getMacAddress()), "wlan");
			if (result < 0) {
				return result;
			}
		} else {
			SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
			Modules.ThreadManForUserModule.executeCallback(thread, sceNetCreateIfhandleEther, new AfterNetCreateIfhandleEtherAction(thread, handleAddr), false, handleAddr.getAddress());
		}

		accessPoint = new AccessPoint(this);

		return 0;
	}

	/**
     * Get the Ethernet Address of the wlan controller
     *
     * @param etherAddr - pointer to a buffer of u8 (NOTE: it only writes to 6 bytes, but
     * requests 8 so pass it 8 bytes just in case)
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x0C622081, version = 150, checkInsideInterrupt = true)
    public int sceWlanGetEtherAddr(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=6, usage=Usage.out) TPointer etherAddr) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.setMacAddress(Wlan.getMacAddress());
    	macAddress.write(etherAddr);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceWlanGetEtherAddr returning %s", macAddress));
    	}

    	return 0;
    }

    /**
     * Determine the state of the Wlan power switch
     *
     * @return 0 if off, 1 if on
     */
    @HLEFunction(nid = 0xD7763699, version = 150)
    public int sceWlanGetSwitchState() {
        return Wlan.getSwitchState();
    }

    /**
     * Determine if the wlan device is currently powered on
     *
     * @return 0 if off, 1 if on
     */
    @HLEFunction(nid = 0x93440B11, version = 150)
    public int sceWlanDevIsPowerOn() {
        return Wlan.getSwitchState();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x482CAE9A, version = 150)
    @HLEFunction(nid = 0x1C18F5FE, version = 660)
    public int sceWlanDevAttach() {
    	// Has no parameters
    	int result = createWlanInterface();
    	if (result < 0) {
			log.error(String.format("Cannot create the WLAN Interface: 0x%08X", result));
			return result;
		}

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC9A8CAB7, version = 150)
    @HLEFunction(nid = 0x8DF43D4C, version = 660)
    public int sceWlanDevDetach() {
    	if (accessPoint != null) {
    		accessPoint.exit();
    		accessPoint = null;
    	}

    	// Has no parameters
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8D5F551B, version = 150)
    @HLEFunction(nid = 0x561FD176, version = 660)
    public int sceWlanDrv_lib_8D5F551B(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x749B813A, version = 150)
    @HLEFunction(nid = 0x58EDD140, version = 660)
    public int sceWlanSetHostDiscover(int unknown1, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFE8A0B46, version = 150)
    @HLEFunction(nid = 0x0CAB500F, version = 660)
    public int sceWlanSetWakeUp(int unknown1, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer unknown2) {
        return 0;
    }

    @HLEFunction(nid = 0x5E7C8D94, version = 150)
    public boolean sceWlanDevIsGameMode() {
        return isGameMode;
    }

    @HLEFunction(nid = 0x5ED4049A, version = 150)
    @HLEFunction(nid = 0xED79DCD4, version = 660)
    public int sceWlanGPPrevEstablishActive(pspNetMacAddress macAddress) {
    	int index = 0;
    	for (pspNetMacAddress activeMacAddress : activeMacAddresses) {
    		if (activeMacAddress.equals(macAddress.macAddress)) {
    			return index;
    		}
    		index++;
    	}

    	return -1;
    }

    /*
     * Called by sceNetAdhocGameModeUpdateReplica()
     */
    @HLEFunction(nid = 0xA447103A, version = 150)
    @HLEFunction(nid = 0x04DBA413, version = 660)
    public int sceWlanGPRecv(int id, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer buffer, int bufferLength, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.variableLength, usage=Usage.out) TPointer updateInfoAddr) {
    	if (!isGameMode) {
    		return SceKernelErrors.ERROR_WLAN_NOT_IN_GAMEMODE;
    	}
    	if (id < 0 || id >= gameModeStates.size()) {
    		return SceKernelErrors.ERROR_WLAN_BAD_PARAMS;
    	}
    	if (bufferLength < 0 || bufferLength > gameModeDataLength) {
    		return SceKernelErrors.ERROR_WLAN_BAD_PARAMS;
    	}

    	GameModeState gameModeState = gameModeStates.get(id);
    	int size = Math.min(gameModeState.dataLength, bufferLength);
    	Utilities.writeBytes(buffer.getAddress(), size, gameModeState.data, 0);

    	if (updateInfoAddr.isNotNull()) {
    		sceNetAdhoc.GameModeUpdateInfo updateInfo = new sceNetAdhoc.GameModeUpdateInfo();
    		updateInfo.read(updateInfoAddr);
    		updateInfo.updated = gameModeState.updated ? 1 : 0;
    		updateInfo.timeStamp = gameModeState.timeStamp;
    		updateInfo.write(updateInfoAddr);
    	}

    	return 0;
    }

    /*
     * Called by sceNetAdhocGameModeUpdateMaster()
     */
    @HLEFunction(nid = 0xB4D7CB74, version = 150)
    @HLEFunction(nid = 0xEEC2F8AB, version = 660)
    public int sceWlanGPSend(int unknown, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer buffer, int bufferLength) {
    	if (!isGameMode) {
    		return SceKernelErrors.ERROR_WLAN_NOT_IN_GAMEMODE;
    	}
    	if (bufferLength < 0 || bufferLength > gameModeDataLength) {
    		return SceKernelErrors.ERROR_WLAN_BAD_PARAMS;
    	}

    	GameModeState myGameModeState = getMyGameModeState();
    	if (myGameModeState == null) {
    		log.error(String.format("sceWlanGPSend not found my GameModeState!"));
    		return -1;
    	}

    	Utilities.readBytes(buffer.getAddress(), bufferLength, myGameModeState.data, 0);
    	myGameModeState.doUpdate();

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2D0FAE4E, version = 150)
    public int sceWlanDrv_lib_2D0FAE4E(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=6, usage=Usage.in) TPointer16 unknown) {
    	unknownValue1 = unknown.getValue(0);
    	unknownValue2 = unknown.getValue(2);
    	unknownValue3 = unknown.getValue(4);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x56F467CA, version = 150)
    public int sceWlanDrv_lib_56F467CA(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=6, usage=Usage.out) TPointer16 unknown) {
    	unknown.setValue(0, unknownValue1);
    	unknown.setValue(2, unknownValue2);
    	unknown.setValue(4, unknownValue3);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5BAA1FE5, version = 150)
    public int sceWlanDrv_lib_5BAA1FE5(int unknown1, int unknown2) {
        return 0;
    }

    /**
     * Checks if a packet has to be dropped according
     * to the parameters defined by sceNetSetDropRate.
     * 
     * @return true if the packet should be dropped
     *         false if the packet should be processed
     */
    @HLEFunction(nid = 0x2519EAA7, version = 150)
    public boolean sceWlanIsPacketToBeDropped() {
    	// Has no parameters
        return false;
    }

    @HLEFunction(nid = 0x325F7172, version = 150)
    public int sceWlanSetDropRate(int dropRate, int dropDuration) {
    	wlanDropRate = dropRate;
    	wlanDropDuration = dropDuration;

    	return 0;
    }

    @HLEFunction(nid = 0xB6A9700D, version = 150)
    public int sceWlanGetDropRate(@CanBeNull TPointer32 dropRateAddr, @CanBeNull TPointer32 dropDurationAddr) {
    	dropRateAddr.setValue(wlanDropRate);
    	dropDurationAddr.setValue(wlanDropDuration);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x90E5530F, version = 150)
    public void sceWlanDrv_driver_90E5530F(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer wlanDriver1, int wlanDriver1Size, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer wlanDriver2, int wlanDriver2Size) {
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9658C9F7, version = 150)
    @HLEFunction(nid = 0x02E66C38, version = 660)
    public int sceWlanGPRegisterCallback(int callbackUid) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4C7F62E0, version = 150)
    @HLEFunction(nid = 0x54764A18, version = 660)
    public int sceWlanGPUnRegisterCallback(int callbackUid) {
    	return 0;
    }
}