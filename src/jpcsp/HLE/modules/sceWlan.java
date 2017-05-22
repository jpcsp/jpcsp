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

import static jpcsp.HLE.Modules.sceNetIfhandleModule;
import static jpcsp.HLE.kernel.managers.SceUidManager.INVALID_ID;
import static jpcsp.HLE.kernel.types.SceNetWlanMessage.WLAN_PROTOCOL_SUBTYPE_CONTROL;
import static jpcsp.HLE.kernel.types.SceNetWlanMessage.WLAN_PROTOCOL_SUBTYPE_DATA;
import static jpcsp.HLE.kernel.types.SceNetWlanMessage.WLAN_PROTOCOL_TYPE_SONY;
import static jpcsp.HLE.modules.SysMemUserForUser.KERNEL_PARTITION_ID;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
import jpcsp.network.accesspoint.AccessPoint;
import jpcsp.scheduler.Scheduler;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceWlan extends HLEModule {
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
    private static int wlanSocketPort = 30010;
    private static final int wlanThreadPollingDelayUs = 12000; // 12ms
    private static final int wlanScanActionDelayUs = 50000; // 50ms
    private static final int wlanConnectActionDelayUs = 50000; // 50ms
    private static final int wlanCreateActionDelayUs = 50000; // 50ms
    private static final int wlanDisconnectActionDelayUs = 50000; // 50ms
    public static final byte WLAN_CMD_DATA          = (byte) 0;
    public static final byte WLAN_CMD_SCAN_REQUEST  = (byte) 1;
    public static final byte WLAN_CMD_SCAN_RESPONSE = (byte) 2;
    static private final byte[] dummyOtherMacAddress = new byte[] { 0x10,  0x22, 0x33, 0x44, 0x55, 0x66 };
    private static final int[] channels = new int[] { 1, 6, 11 };
    private int joinedChannel;
    private int dummyMessageStep;
    private TPointer dummyMessageHandleAddr;
    private DatagramSocket wlanSocket;
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

    private class WlanScanAction implements IAction {
    	private TPointer handleAddr;
    	private TPointer inputAddr;
    	private TPointer outputAddr;
    	private int callCount;

		public WlanScanAction(TPointer handleAddr, int inputAddr, int outputAddr) {
			this.handleAddr = handleAddr;
			this.inputAddr = new TPointer(handleAddr.getMemory(), inputAddr);
			this.outputAddr = new TPointer(handleAddr.getMemory(), outputAddr);
		}

		@Override
		public void execute() {
			hleWlanScanAction(handleAddr, inputAddr, outputAddr, this, callCount);
			callCount++;
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

    @Override
	public void start() {
		wlanThreadUid = INVALID_ID;
		dummyMessageStep = -1;
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

		super.start();
	}

    public void hleWlanThread() {
    	if (log.isTraceEnabled()) {
    		log.trace(String.format("hleWlanThread isGameMode=%b", isGameMode));
    	}

    	if (wlanThreadMustExit()) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Exiting hleWlanThread %s", Modules.ThreadManForUserModule.getCurrentThread()));
    		}
    		Modules.ThreadManForUserModule.hleKernelExitDeleteThread(0);
    		return;
    	}

    	if (isGameMode) {
    		hleWlanSendGameMode();
    	}

    	while (!wlanThreadMustExit() && hleWlanReceive()) {
			// Receive all available messages
		}

    	if (dummyMessageStep > 0) {
    		sendDummyMessage(dummyMessageStep, dummyMessageHandleAddr);
    		dummyMessageStep = 0;
    	}

    	Modules.ThreadManForUserModule.hleKernelDelayThread(wlanThreadPollingDelayUs, true);
    }

    private boolean wlanThreadMustExit() {
    	return wlanThreadUid != Modules.ThreadManForUserModule.getCurrentThreadID();
    }

    private void hleWlanScanAction(TPointer handleAddr, TPointer inputAddr, TPointer outputAddr, WlanScanAction action, int callCount) {
    	// Send a scan request packet
    	byte[] scanRequestPacket = new byte[1 + MAC_ADDRESS_LENGTH];
    	scanRequestPacket[0] = WLAN_CMD_SCAN_REQUEST;
    	System.arraycopy(Wlan.getMacAddress(), 0, scanRequestPacket, 1, MAC_ADDRESS_LENGTH);
		sendPacket(scanRequestPacket, scanRequestPacket.length);

		while (hleWlanReceive()) {
			// Process all pending messages
		}

		if (callCount < 20) {
			// Schedule this action for 20 times (1 second)
			// before terminating the scan action
			Emulator.getScheduler().addAction(Scheduler.getNow() + wlanScanActionDelayUs, action);
		} else {
			if (log.isDebugEnabled()) {
				log.debug(String.format("End of scan action:"));
				for (int ch : channels) {
					log.debug(String.format("Scan result channel#%d, ssid='%s', mode=%d", ch, channelSSIDs[ch], channelModes[ch]));
				}
			}

			TPointer addr = new TPointer(outputAddr);
			for (int i = 0; i < 14; i++) {
				int channel = inputAddr.getValue8(10 + i);
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
						scanInfo.unknown44 = 1000; // Unknown value, need to be != 0
						scanInfo.write(addr.getMemory(), addr.getAddress() + 4);

	    				addr.setValue32(0, addr.getAddress() + 4 + scanInfo.sizeof()); // Link to next SSID
		    			addr.add(4 + scanInfo.sizeof());
					}
				}
			}

			if (addr.getAddress() > outputAddr.getAddress()) {
				addr.setValue32(-96, 0); // Last SSID, no next one
			}

			// Signal the sema when the scan has completed
	    	SceNetIfHandle handle = new SceNetIfHandle();
	    	handle.read(handleAddr);
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

    private boolean hleWlanReceive() {
    	if (isGameMode) {
    		return hleWlanReceiveGameMode();
    	}

    	return hleWlanReceiveMessage();
    }

    private boolean createWlanSocket() {
    	if (wlanSocket == null) {
			boolean retry;
			do {
				retry = false;
	    		try {
					wlanSocket = new DatagramSocket(wlanSocketPort);
		    		// For broadcast
					wlanSocket.setBroadcast(true);
		    		// Non-blocking (timeout = 0 would mean blocking)
					wlanSocket.setSoTimeout(1);
	    		} catch (BindException e) {
	    			if (log.isDebugEnabled()) {
	    				log.debug(String.format("createWlanSocket port %d already in use (%s) - retrying with port %d", wlanSocketPort, e, wlanSocketPort + 1));
	    			}
	    			// The port is already busy, retrying with another port
	    			wlanSocketPort++;
	    			retry = true;
				} catch (SocketException e) {
					log.error("createWlanSocket", e);
				}
			} while (retry);
    	}

    	return wlanSocket != null;
    }

    public static int getSocketPort() {
    	return wlanSocketPort;
    }

    private int getBroadcastPort(int channel) {
    	if (channel >= 0 && channelModes[channel] == WLAN_MODE_INFRASTRUCTURE) {
    		return AccessPoint.getInstance().getPort();
    	}

    	return wlanSocketPort ^ 1;
    }

    protected void sendPacket(byte[] buffer, int bufferLength) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sendPacket %s", Utilities.getMemoryDump(buffer, 0, bufferLength)));
    	}

    	try {
			InetSocketAddress broadcastAddress[] = sceNetInet.getBroadcastInetSocketAddress(getBroadcastPort(joinedChannel));
			if (broadcastAddress != null) {
				for (int i = 0; i < broadcastAddress.length; i++) {
					DatagramPacket packet = new DatagramPacket(buffer, bufferLength, broadcastAddress[i]);
					wlanSocket.send(packet);
				}
			}
		} catch (UnknownHostException e) {
			log.error("sendPacket", e);
		} catch (IOException e) {
			log.error("sendPacket", e);
		}
    }

    protected void sendDataPacket(byte[] buffer, int bufferLength) {
    	byte[] packetBuffer = new byte[bufferLength + 1 + 32];
    	int offset = 0;
    	// Add the cmd in front of the data
    	packetBuffer[offset] = WLAN_CMD_DATA;
    	offset++;
    	// Add the joined SSID in front of the data
    	if (joinedChannel >= 0) {
    		Utilities.writeStringNZ(packetBuffer, offset, 32, channelSSIDs[joinedChannel]);
    	}
    	offset += 32;
    	// Add the data
    	System.arraycopy(buffer, 0, packetBuffer, offset, bufferLength);
    	offset += bufferLength;

    	sendPacket(packetBuffer, offset);
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

    private void addActiveMacAddress(pspNetMacAddress macAddress) {
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

    private void setChannelSSID(int channel, String ssid, int mode) {
    	if (ssid != null && ssid.length() > 0 && isValidChannel(channel)) {
    		channelSSIDs[channel] = ssid;
    		channelModes[channel] = mode;
    	}
    }

    private void joinChannelSSID(int channel, String ssid, int mode) {
    	setChannelSSID(channel, ssid, mode);
    	joinedChannel = channel;
    }

    private void processCmd(byte cmd, byte[] buffer, int offset, int length) {
    	byte[] packetMacAddress = new byte[MAC_ADDRESS_LENGTH];
    	System.arraycopy(buffer, offset, packetMacAddress, 0, MAC_ADDRESS_LENGTH);
    	offset += MAC_ADDRESS_LENGTH;
    	length -= MAC_ADDRESS_LENGTH;
    	byte[] myMacAddress = Wlan.getMacAddress();
    	boolean macAddressEqual = true;
    	for (int i = 0; i < MAC_ADDRESS_LENGTH; i++) {
    		if (packetMacAddress[i] != myMacAddress[i]) {
    			macAddressEqual = false;
    			break;
    		}
    	}
    	if (macAddressEqual) {
    		// This packet is coming from myself, ignore it
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Ignoring packet coming from myself"));
    		}
    		return;
    	}

    	if (cmd == WLAN_CMD_SCAN_REQUEST) {
    		byte[] scanResponse = new byte[1 + MAC_ADDRESS_LENGTH + (32 + 2) * channels.length];
    		int responseOffset = 0;

    		scanResponse[responseOffset] = WLAN_CMD_SCAN_RESPONSE;
    		responseOffset++;

    		System.arraycopy(Wlan.getMacAddress(), 0, scanResponse, responseOffset, MAC_ADDRESS_LENGTH);
    		responseOffset += MAC_ADDRESS_LENGTH;

    		for (int channel : channels) {
    			scanResponse[responseOffset] = (byte) channel;
    			responseOffset++;

    			scanResponse[responseOffset] = (byte) channelModes[channel];
    			responseOffset++;

    			Utilities.writeStringNZ(scanResponse, responseOffset, 32, channelSSIDs[channel]);
        		responseOffset += 32;
    		}
    		sendPacket(scanResponse, responseOffset);
    	} else if (cmd == WLAN_CMD_SCAN_RESPONSE) {
    		while (length >= 34) {
    			int channel = buffer[offset];
    			offset++;
    			length--;

    			int mode = buffer[offset];
    			offset++;
    			length--;

    			String ssid = Utilities.readStringNZ(buffer, offset, 32);
    			if (ssid != null && ssid.length() > 0) {
    				// Do not overwrite the information for our joined channel
    				if (channel != joinedChannel) {
    					setChannelSSID(channel, ssid, mode);
    				}
    			}
    			offset += 32;
    			length -= 32;
    		}
    	} else {
    		if (log.isInfoEnabled()) {
    			log.info(String.format("processCmd unknown cmd=0x%X, buffer=%s", cmd, Utilities.getMemoryDump(buffer, offset, length)));
    		}
    	}
    }

    private boolean hleWlanReceiveMessage() {
    	boolean packetReceived = false;

    	if (!createWlanSocket()) {
    		return packetReceived;
    	}

    	byte[] bytes = new byte[10000];
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
		try {
			wlanSocket.receive(packet);
			if (log.isDebugEnabled()) {
				log.debug(String.format("hleWlanReceiveMessage message: %s", Utilities.getMemoryDump(packet.getData(), packet.getOffset(), packet.getLength())));
			}

			packetReceived = true;

			byte[] dataBytes = packet.getData();
			int dataOffset = packet.getOffset();
			int dataLength = packet.getLength();

			byte cmd = dataBytes[dataOffset];
			dataOffset++;
			dataLength--;
			if (cmd != WLAN_CMD_DATA) {
				processCmd(cmd, dataBytes, dataOffset, dataLength);
				return packetReceived;
			}

			String ssid = Utilities.readStringNZ(dataBytes, dataOffset, 32);
			dataOffset += 32;
			dataLength -= 32;

			if (joinedChannel >= 0 && !ssid.equals(channelSSIDs[joinedChannel])) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("hleWlanReceiveMessage message SSID('%s') not matching the joined SSID('%s')", ssid, channelSSIDs[joinedChannel]));
				}
				return packetReceived;
			}

	    	SceNetIfMessage message = new SceNetIfMessage();
	    	final int size = message.sizeof() + dataLength;
	    	int allocatedAddr = Modules.sceNetIfhandleModule.hleNetMallocInternal(size);
	    	if (allocatedAddr > 0) {
	    		Memory mem = Memory.getInstance();
		    	mem.memset(allocatedAddr, (byte) 0, size);
		    	RuntimeContext.debugMemory(allocatedAddr, size);

		    	TPointer messageAddr = new TPointer(mem, allocatedAddr);
		    	TPointer data = new TPointer(mem, messageAddr.getAddress() + message.sizeof());

		    	// Write the received bytes to memory
		    	Utilities.writeBytes(data.getAddress(), dataLength, dataBytes, dataOffset);

		    	// Write the message header
		    	message.dataAddr = data.getAddress();
				message.dataLength = dataLength;
				message.unknown16 = 1;
				message.unknown18 = 2;
				message.unknown24 = dataLength;
				message.write(messageAddr);

		    	SceNetWlanMessage wlanMessage = new SceNetWlanMessage();
		    	wlanMessage.read(data);
		    	addActiveMacAddress(wlanMessage.srcMacAddress);
		    	addActiveMacAddress(wlanMessage.dstMacAddress);

		    	if (dataLength > 0) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Notifying received message: %s", message));
						log.debug(String.format("Message WLAN: %s", wlanMessage));
						log.debug(String.format("Message data: %s", Utilities.getMemoryDump(data.getAddress(), dataLength)));
					}

					int sceNetIfEnqueue = NIDMapper.getInstance().getAddressByName("sceNetIfEnqueue");
					if (sceNetIfEnqueue != 0) {
						SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
						Modules.ThreadManForUserModule.executeCallback(thread, sceNetIfEnqueue, null, true, wlanHandleAddr.getAddress(), messageAddr.getAddress());
					}
				}
	    	}
		} catch (SocketTimeoutException e) {
			// Timeout can be ignored as we are polling
		} catch (IOException e) {
			log.error("hleWlanReceiveMessage", e);
		}

		return packetReceived;
    }

    private void hleWlanSendGameMode() {
    	GameModeState myGameModeState = getMyGameModeState();
    	if (myGameModeState == null) {
    		return;
    	}

    	byte[] buffer = new byte[myGameModeState.dataLength + myGameModeState.macAddress.sizeof()];
    	int offset = 0;

    	System.arraycopy(myGameModeState.macAddress.macAddress, 0, buffer, offset, myGameModeState.macAddress.sizeof());
    	offset += myGameModeState.macAddress.sizeof();

    	System.arraycopy(myGameModeState.data, 0, buffer, offset, myGameModeState.dataLength);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanSendGameMode sending packet: %s", Utilities.getMemoryDump(buffer, 0, buffer.length)));
    	}

    	sendDataPacket(buffer, buffer.length);

    	myGameModeState.updated = false;
    }

    private boolean hleWlanReceiveGameMode() {
    	boolean packetReceived = false;

    	if (!createWlanSocket()) {
    		return packetReceived;
    	}

    	pspNetMacAddress macAddress = new pspNetMacAddress();

    	byte[] bytes = new byte[gameModeDataLength + macAddress.sizeof() + 1 + 8];
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
		try {
			wlanSocket.receive(packet);
			if (log.isDebugEnabled()) {
				log.debug(String.format("hleWlanReceiveGameMode message: %s", Utilities.getMemoryDump(packet.getData(), packet.getOffset(), packet.getLength())));
			}

			packetReceived = true;

			byte[] dataBytes = packet.getData();
			int dataOffset = packet.getOffset();
			int dataLength = packet.getLength();

			byte cmd = dataBytes[dataOffset];
			dataOffset++;
			dataLength--;
			if (cmd != WLAN_CMD_DATA) {
				processCmd(cmd, dataBytes, dataOffset, dataLength);
				return packetReceived;
			}

			String ssid = Utilities.readStringNZ(dataBytes, dataOffset, 32);
			dataOffset += 32;
			dataLength -= 32;

			if (joinedChannel >= 0 && !ssid.equals(channelSSIDs[joinedChannel])) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("hleWlanReceiveGameMode message SSID('%s') not matching the joined SSID('%s')", ssid, channelSSIDs[joinedChannel]));
				}
				return packetReceived;
			}

			macAddress.setMacAddress(dataBytes, dataOffset);
			dataOffset += macAddress.sizeof();
			dataLength -= macAddress.sizeof();

			GameModeState gameModeState = getGameModeStat(macAddress.macAddress);
			if (gameModeState != null) {
				int length = Math.min(dataLength, gameModeState.dataLength);
				System.arraycopy(dataBytes, dataOffset, gameModeState.data, 0, length);

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

    	if (!createWlanSocket()) {
    		return;
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
    		sendDataPacket(messageBytes, messageBytes.length);
    	}

    	if (false) {
    		sendDummyMessage(handleAddr, message, wlanMessage);
    	}
    }

    public int hleWlanSendCallback(TPointer handleAddr) {
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	Memory mem = handleAddr.getMemory();
    	TPointer firstMessageAddr = new TPointer(mem, handle.addrFirstMessageToBeSent);
    	SceNetIfMessage message = new SceNetIfMessage();
    	message.read(firstMessageAddr);
    	RuntimeContext.debugMemory(firstMessageAddr.getAddress(), message.sizeof());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanSendCallback handleAddr=%s: %s", handleAddr, handle));
    	}

    	hleWlanSendMessage(handleAddr, message);

    	// Unlink the message from the handle
    	handle.addrFirstMessageToBeSent = message.nextMessageAddr;
    	handle.numberOfMessagesToBeSent--;
    	if (handle.addrFirstMessageToBeSent == 0) {
    		handle.addrLastMessageToBeSent = 0;
    	}
    	handle.write(handleAddr);

    	// Call sceNetMFreem to free the received message
    	int sceNetMFreem = NIDMapper.getInstance().getAddressByName("sceNetMFreem");
    	if (sceNetMFreem != 0) {
    		Modules.ThreadManForUserModule.executeCallback(null, sceNetMFreem, null, true, firstMessageAddr.getAddress());
    	} else {
    		Modules.sceNetIfhandleModule.sceNetMFreem(firstMessageAddr);
    	}

    	return 0;
    }

    // Called by sceNetIfhandleIfUp
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

    	// Add my own MAC address to the active list
    	addActiveMacAddress(new pspNetMacAddress(Wlan.getMacAddress()));

    	// This thread will call hleWlanThread() in a loop
    	SceKernelThreadInfo thread = Modules.ThreadManForUserModule.hleKernelCreateThread("SceWlanHal", ThreadManForUser.WLAN_LOOP_ADDRESS, 39, 2048, 0, 0, KERNEL_PARTITION_ID);
    	if (thread != null) {
    		wlanThreadUid = thread.uid;
    		Modules.ThreadManForUserModule.hleKernelStartThread(thread, 0, 0, 0);
    	}

    	Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);

    	return 0;
    }

    // Called by sceNetIfhandleIfDown
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

    	Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);

    	return 0;
    }

    public int hleWlanIoctlCallback(TPointer handleAddr, int cmd, TPointer unknown, TPointer32 buffersAddr) {
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
					scanInfo.unknown44 = 1000; // Unknown value, need to be != 0
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

	    			if (createWlanSocket()) {
	    				signalSema = false;
	    				Emulator.getScheduler().addAction(Scheduler.getNow(), new WlanScanAction(handleAddr, inputAddr, outputAddr));
	    			}
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
    			Emulator.getScheduler().addAction(Scheduler.getNow() + wlanCreateActionDelayUs, new WlanCreateAction(handleAddr));
    			break;
    		case IOCTL_CMD_CONNECT: // Called by sceNetAdhocctlConnect() and sceNetAdhocctlJoin()
    			// Receiving as input the SSID structure returned by cmd=0x34
    			SceNetWlanScanInfo scanInfo = new SceNetWlanScanInfo();
    			scanInfo.read(mem, inputAddr);
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("hleWlanIoctlCallback cmd=0x%X, channel=%d, ssid='%s', mode=0x%X", cmd, scanInfo.channel, scanInfo.ssid, scanInfo.mode));
    			}
    			joinChannelSSID(scanInfo.channel, scanInfo.ssid, scanInfo.mode);

    			signalSema = false;
    			Emulator.getScheduler().addAction(Scheduler.getNow() + wlanConnectActionDelayUs, new WlanConnectAction(handleAddr));
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
    			Emulator.getScheduler().addAction(Scheduler.getNow() + wlanDisconnectActionDelayUs, new WlanDisconnectAction(handleAddr));
    			break;
    		case IOCTL_CMD_ENTER_GAME_MODE: // Enter Game Mode
    			pspNetMacAddress multicastMacAddress = new pspNetMacAddress();
    			multicastMacAddress.read(mem, inputAddr + 6);

    			ssidLength = mem.read8(inputAddr + 12);
    			ssid = Utilities.readStringNZ(mem,  inputAddr + 14, ssidLength);

    			pspNetMacAddress macAddress = new pspNetMacAddress();
    			macAddress.read(mem, outputAddr + 0);

    			if (log.isDebugEnabled()) {
    				log.debug(String.format("hleWlanIoctlCallback cmd=0x%X, ssid='%s', multicastMacAddress=%s, macAddress=%s", cmd, ssid, multicastMacAddress, macAddress));
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

    static private void sendDummyMessage(int step, TPointer handleAddr) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sendDummyMessage step=%d", step));
    	}
    	Memory mem = Memory.getInstance();
    	SceNetIfMessage message = new SceNetIfMessage();
    	SceNetWlanMessage wlanMessage = new SceNetWlanMessage();

    	final int size = message.sizeof() + wlanMessage.sizeof() + SceNetWlanMessage.maxContentLength + 0x12;
    	int allocatedAddr = Modules.sceNetIfhandleModule.hleNetMallocInternal(size);
    	if (allocatedAddr <= 0) {
    		return;
    	}
    	RuntimeContext.debugMemory(allocatedAddr, size);
    	mem.memset(allocatedAddr, (byte) 0, size);

    	TPointer messageAddr = new TPointer(mem, allocatedAddr);
    	TPointer data = new TPointer(mem, messageAddr.getAddress() + message.sizeof());
    	TPointer header = new TPointer(mem, data.getAddress());
    	TPointer content = new TPointer(mem, header.getAddress() + wlanMessage.sizeof());

    	int dataLength;
    	int controlType;
    	int contentLength;
    	switch (step) {
    		case 1:
	        	controlType = 2; // possible values: [1..8]
		    	contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
		    	dataLength = wlanMessage.sizeof() + contentLength;
	
		    	wlanMessage.dstMacAddress = new pspNetMacAddress(Wlan.getMacAddress());
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
	
		    	content.clear(contentLength);
		    	break;
    		case 2:
	        	controlType = 0;
		    	contentLength = 0x4C;
		    	dataLength = wlanMessage.sizeof() + contentLength;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(new byte[] { -1, -1, -1, -1, -1, -1}); // Broadcast MAC address
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_DATA; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 0;
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = contentLength;

		    	content.clear(contentLength);
		    	content.setStringNZ(0x34, 5, "Jpcsp");
		    	break;
    		case 3:
    			controlType = 2; // possible values: [1..8]
    			contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
    			dataLength = wlanMessage.sizeof() + contentLength + 0x12;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(new byte[] { -1, -1, -1, -1, -1, -1});
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];

		    	content.setStringNZ(0, 0x80, "JpcspOther");
		    	content.setValue8(0x80, (byte) 1);
		    	content.setValue8(0x81, (byte) 4);
		    	content.setUnalignedValue32(0x82, Modules.SysMemUserForUserModule.sceKernelDevkitVersion());
		    	content.setValue8(0x86, (byte) 2);
		    	content.setValue8(0x87, (byte) 4);
		    	content.setUnalignedValue32(0x88, Modules.SysMemUserForUserModule.sceKernelGetCompiledSdkVersion());
		    	content.setValue8(0x8C, (byte) 3);
		    	content.setValue8(0x8D, (byte) 4);
		    	content.setUnalignedValue32(0x8E, Modules.SysMemForKernelModule.sceKernelGetModel());
		    	break;
    		case 4:
    			controlType = 3; // possible values: [1..8]
    			contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
    			dataLength = wlanMessage.sizeof() + contentLength + 0x12;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(Wlan.getMacAddress());
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];

		    	content.clear(contentLength);
		    	content.setStringNZ(0xA0, 0x80, "JpcspOther");
		    	content.setValue8(0x120, (byte) 1);
		    	content.setValue8(0x121, (byte) 4);
		    	content.setUnalignedValue32(0x82, Modules.SysMemUserForUserModule.sceKernelDevkitVersion());
		    	content.setValue8(0x126, (byte) 2);
		    	content.setValue8(0x127, (byte) 4);
		    	content.setUnalignedValue32(0x88, Modules.SysMemUserForUserModule.sceKernelGetCompiledSdkVersion());
		    	content.setValue8(0x12C, (byte) 3);
		    	content.setValue8(0x12D, (byte) 4);
		    	content.setUnalignedValue32(0x12E, Modules.SysMemForKernelModule.sceKernelGetModel());
		    	break;
    		case 5:
    			controlType = 4; // possible values: [1..8]
    			contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
    			dataLength = wlanMessage.sizeof() + contentLength;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(Wlan.getMacAddress());
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];

		    	content.clear(contentLength);
		    	break;
    		case 6:
    			controlType = 5; // possible values: [1..8]
    			contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
    			dataLength = wlanMessage.sizeof() + contentLength;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(Wlan.getMacAddress());
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];

		    	content.clear(contentLength);
		    	break;
    		case 7:
    			controlType = 6; // possible values: [1..8]
    			contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
    			dataLength = wlanMessage.sizeof() + contentLength;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(Wlan.getMacAddress());
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];

		    	content.clear(contentLength);
		    	break;
    		case 8:
    			controlType = 8; // possible values: [1..8]
    			contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
    			dataLength = wlanMessage.sizeof() + contentLength;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(Wlan.getMacAddress());
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];

		    	content.clear(contentLength);
		    	break;
    		default:
    			dataLength = 0;
    			break;
		}

    	wlanMessage.write(header);

    	message.dataAddr = data.getAddress();
		message.dataLength = dataLength;
		message.unknown18 = 0;
		message.unknown24 = dataLength;
		message.write(messageAddr);

		if (dataLength > 0) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Sending dummy message: %s", message));
				log.debug(String.format("Dummy message data: %s", Utilities.getMemoryDump(data.getAddress(), dataLength)));
			}

			int sceNetIfEnqueue = NIDMapper.getInstance().getAddressByName("sceNetIfEnqueue");
			if (sceNetIfEnqueue != 0) {
				SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
				Modules.ThreadManForUserModule.executeCallback(thread, sceNetIfEnqueue, null, true, handleAddr.getAddress(), messageAddr.getAddress());
			}
		}
    }

    private void sendDummyMessage(TPointer handleAddr, SceNetIfMessage sentMessage, SceNetWlanMessage sentWlanMessage) {
    	int step = 0;
    	if (false) {
    		step = 1;
    	} else if (false) {
    		step = 2;
    	} else if (dummyMessageStep < 0 && !sentWlanMessage.dstMacAddress.equals(dummyOtherMacAddress)) {
    		step = 3;
		} else if (sentWlanMessage.controlType == 3) {
			step = 5;
		} else if (sentWlanMessage.controlType == 4) {
			step = 5;
		} else if (sentWlanMessage.controlType == 5) {
			step = 7;
		} else if (sentWlanMessage.controlType == 7) {
			step = 8;
		} else {
			step = 0;
		}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Adding action step=%d for sending dummy message", step));
    	}
    	dummyMessageStep = step;
    	dummyMessageHandleAddr = handleAddr;
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
		SceNetIfHandle handle = new SceNetIfHandle();
		handle.callbackArg4 = 0x11040404; // dummy callback value
		handle.upCallbackAddr = ThreadManForUser.WLAN_UP_CALLBACK_ADDRESS;
		handle.downCallbackAddr = ThreadManForUser.WLAN_DOWN_CALLBACK_ADDRESS;
		handle.sendCallbackAddr = ThreadManForUser.WLAN_SEND_CALLBACK_ADDRESS;
		handle.ioctlCallbackAddr = ThreadManForUser.WLAN_IOCTL_CALLBACK_ADDRESS;
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
    public int sceWlanDevDetach() {
    	// Has no parameters
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8D5F551B, version = 150)
    public int sceWlanDrv_lib_8D5F551B(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x749B813A, version = 150)
    public int sceWlanSetHostDiscover(int unknown1, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFE8A0B46, version = 150)
    public int sceWlanSetWakeUp(int unknown1, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer unknown2) {
        return 0;
    }

    @HLEFunction(nid = 0x5E7C8D94, version = 150)
    public boolean sceWlanDevIsGameMode() {
        return isGameMode;
    }

    @HLEFunction(nid = 0x5ED4049A, version = 150)
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
}