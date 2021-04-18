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
package jpcsp.network.xlinkkai;

import static java.util.ResourceBundle.getBundle;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.showConfirmDialog;
import static jpcsp.HLE.kernel.types.pspNetMacAddress.isAnyMacAddress;
import static jpcsp.HLE.kernel.types.pspNetMacAddress.isEmptyMacAddress;
import static jpcsp.HLE.kernel.types.pspNetMacAddress.isMulticastMacAddress;
import static jpcsp.HLE.modules.sceNet.convertMacAddressToString;
import static jpcsp.HLE.modules.sceNetAdhoc.ANY_MAC_ADDRESS;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.hardware.Wlan.getMacAddress;
import static jpcsp.scheduler.Scheduler.getNow;
import static jpcsp.util.Utilities.read8;
import static jpcsp.util.Utilities.readStringNZ;
import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.write8;
import static jpcsp.util.Utilities.writeUnaligned16;
import static jpcsp.util.Utilities.writeUnaligned32;
import static jpcsp.util.Utilities.writeUnaligned64;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.GUI.ChatGUI;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.sceNetAdhoc;
import jpcsp.hardware.Wlan;
import jpcsp.memory.mmio.wlan.MMIOHandlerWlan;
import jpcsp.network.BaseWlanAdapter;
import jpcsp.network.protocols.EtherFrame;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.AbstractIntSettingsListener;
import jpcsp.settings.AbstractStringSettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

public class XLinkKaiWlanAdapter extends BaseWlanAdapter {
	public static Logger log = Logger.getLogger("XLinkKai");
	private static final boolean filterDuplicateMessages = true;
	private static boolean enabled = false;
	private DatagramSocket socket;
	private String uniqueIdentifier;
	private InetAddress destAddr;
	private String destServer = "localhost";
	private int destPort = 34523;
	final static private String applicationName = "Jpcsp";
	final static long timeout = 2000000; // 2 seconds
	private volatile boolean connected;
	private volatile boolean disconnected;
	private List<byte[]> receivedData = new LinkedList<byte[]>();
	private byte[] lastDataReceived = null;
	private int gameModeCounter;
	private static final boolean workaroundBugMulticast = false;
	private static final byte[] rawIdentifier = "802.11.Raw".getBytes();
	private ChatGUI chatGUI;
	private String username;
	private String arena = "";
	private String gameName = "";
	private String gameConsole = "";

	private static class EnabledSettingsListener extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setEnabled(value);
		}
	}

	private class ServerSettingsListener extends AbstractStringSettingsListener {
		@Override
		protected void settingsValueChanged(String value) {
			setServer(value);
		}
	}

	private class PortSettingsListener extends AbstractIntSettingsListener {
		@Override
		protected void settingsValueChanged(int value) {
			setPort(value);
		}
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean enabled) {
		XLinkKaiWlanAdapter.enabled = enabled;
		if (enabled) {
			log.info("Enabling XLink Kai network");
		}
	}

	public static void init() {
		Settings.getInstance().registerSettingsListener("XLinkKai", "emu.enableXLinkKai", new EnabledSettingsListener());
	}

	public static void exit() {
		Settings.getInstance().removeSettingsListener("XLinkKai");

		if (!isEnabled()) {
			return;
		}
	}

	public XLinkKaiWlanAdapter() {
		Settings.getInstance().registerSettingsListener("XLinkKai", "network.XLinkKai.server", new ServerSettingsListener());
		Settings.getInstance().registerSettingsListener("XLinkKai", "network.XLinkKai.port", new PortSettingsListener());
	}

	public void setServer(String server) {
		destServer = server;

		// If we are already started, we also need to change the destAddr
		if (destAddr != null) {
			try {
				destAddr = InetAddress.getByName(destServer);
			} catch (UnknownHostException e) {
				log.error("Cannot change XLink Kai server", e);
			}
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("setServer '%s'", server));
		}
	}

	public void setPort(int port) {
		destPort = port;

		if (log.isDebugEnabled()) {
			log.debug(String.format("setPort %d", port));
		}
	}

	private void send(byte[] buffer, int offset, int length) throws IOException {
		if (socket == null) {
			return;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("Sending bytes %s", Utilities.getMemoryDump(buffer, offset, length)));
		}

		DatagramPacket packet = new DatagramPacket(buffer, offset, length, destAddr, destPort);
		socket.send(packet);
	}

	private void send(String s) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Sending '%s'", s));
		}

		byte[] bytes = s.getBytes();
		send(bytes, 0, bytes.length);
	}

	private void sendDataPacket(byte[] buffer, int offset, int length) throws IOException {
		final int headerSize = 4;
		byte[] bytes = new byte[length + headerSize];
		bytes[0] = 'e';
		bytes[1] = ';';
		bytes[2] = 'e';
		bytes[3] = ';';
		System.arraycopy(buffer, offset, bytes, headerSize, length);

		if (workaroundBugMulticast) {
			if (!isAnyMacAddress(bytes, headerSize) && isMulticastMacAddress(bytes, headerSize)) {
				// Replace a multicast address with FF:FF:FF:FF:FF:FF as XLink Kai is not properly
				// forwarding such packets to the DDS interface.
				System.arraycopy(sceNetAdhoc.ANY_MAC_ADDRESS, 0, bytes, headerSize, Wlan.MAC_ADDRESS_LENGTH);
			}
		}

		send(bytes, 0, bytes.length);
	}

	private byte[] receiveBytes() throws IOException {
		if (socket == null) {
			return null;
		}

		byte[] buffer = new byte[10000];
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		try {
			socket.receive(packet);
		} catch (SocketTimeoutException e) {
			return null;
		}

		byte[] data = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

		if (log.isTraceEnabled()) {
			log.trace(String.format("Received bytes from %s: %s", packet.getAddress(), Utilities.getMemoryDump(data)));
		}

		return data;
	}

	private String extractParameters(String s) {
		if (s != null) {
			int start = s.indexOf(';');
			if (start >= 0) {
				s = s.substring(start + 1);
				if (s.endsWith(";")) {
					s = s.substring(0, s.length() - 1);
				}
			}
		}

		return s;
	}

	private void process(byte[] data) throws IOException {
		if (data.length < 4) {
			log.warn(String.format("Received too short packet %s", Utilities.getMemoryDump(data)));
			return;
		}

		if (data[0] == 'e' && data[1] == ';' && data[2] == 'e' && data[3] == ';') {
			// A data message
			processData(data, 4, data.length - 4);
		} else {
			// A control message
			String controlMessage = new String(data);

			if (log.isDebugEnabled()) {
				log.debug(String.format("Processing control message '%s'", controlMessage));
			}

			if (controlMessage.startsWith("connected;")) {
				connected = true;
			} else if (controlMessage.startsWith("disconnected;")) {
				disconnected = true;
			} else if (controlMessage.startsWith("keepalive;")) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Received keepalive"));
				}

				// Send a keepalive response
				send("keepalive;");
			} else if (controlMessage.startsWith("message;")) {
				String message = extractParameters(controlMessage);
				if (log.isDebugEnabled()) {
					log.debug(String.format("Received message '%s'", message));
				}
				if (chatGUI != null) {
					chatGUI.addChatMessage("System message", message);
				}
			} else if (controlMessage.startsWith("chat;")) {
				String message = extractParameters(controlMessage);
				if (log.isDebugEnabled()) {
					log.debug(String.format("Received chat '%s'", message));
				}

				String playerName = "chat";
				int endOfPlayerName = message.indexOf(": ");
				if (endOfPlayerName >= 0) {
					playerName = message.substring(0, endOfPlayerName);
					message = message.substring(endOfPlayerName + 2);
				}
				if (chatGUI != null) {
					chatGUI.addChatMessage(playerName, message);
				}
			} else if (controlMessage.startsWith("directmessage;")) {
				String message = extractParameters(controlMessage);
				if (log.isDebugEnabled()) {
					log.debug(String.format("Received direct message '%s'", message));
				}

				String playerName = "Direct message";
				int endOfPlayerName = message.indexOf(": ");
				if (endOfPlayerName >= 0) {
					playerName = message.substring(0, endOfPlayerName);
					message = message.substring(endOfPlayerName + 2);
				}
				if (chatGUI != null) {
					chatGUI.addChatMessage(playerName, message);
				}
			} else if (controlMessage.startsWith("player_names;")) {
				String names = extractParameters(controlMessage);
				if (log.isDebugEnabled()) {
					log.debug(String.format("Received player names '%s'", names));
				}

				String[] players = names.split("/");
				List<String> chatMembers = new LinkedList<String>();
				if (players != null) {
					for (int i = 0; i < players.length; i++) {
						String player = players[i];
						if (player.length() > 0 && !player.equals(username)) {
							chatMembers.add(player);
							if (log.isDebugEnabled()) {
								log.debug(String.format("Player#%d: '%s'", i, players[i]));
							}
						}
					}
				}

				// Open the chat window only if we have some players to show
				if (chatGUI == null && chatMembers.size() > 0) {
					openChat();
				}

				if (chatGUI != null) {
					chatGUI.updateMembers(chatMembers);
				}
			} else if (controlMessage.startsWith("player_join;")) {
				String name = extractParameters(controlMessage);
				if (log.isDebugEnabled()) {
					log.debug(String.format("Player '%s' joined", name));
				}

				if (!name.equals(username)) {
					openChat();
					if (chatGUI != null) {
						chatGUI.addMember(name);
					}
				}
			} else if (controlMessage.startsWith("player_leave;")) {
				String name = extractParameters(controlMessage);
				if (log.isDebugEnabled()) {
					log.debug(String.format("Player '%s' leaved", name));
				}

				if (chatGUI != null && !name.equals(username)) {
					chatGUI.removeMember(name);
				}
			} else if (controlMessage.startsWith("arena;")) {
				arena = extractParameters(controlMessage);
				if (log.isDebugEnabled()) {
					log.debug(String.format("Arena '%s'", arena));
				}

				// The game info is reset when changing the arena
				gameConsole = "";
				gameName = "";
				updateChat();

				// Refresh the list of players when changing the arena
				send("getplayernames;");
			} else if (controlMessage.startsWith("gameinfo;")) {
				String gameInfo = extractParameters(controlMessage);
				String[] gameInfos = gameInfo.split(";");
				if (gameInfos != null && gameInfos.length >= 2) {
					gameConsole = gameInfos[0];
					gameName = gameInfos[1];
				}
				if (log.isDebugEnabled()) {
					log.debug(String.format("Game info Console '%s', Game name '%s'", gameConsole, gameName));
				}

				updateChat();
			} else if (controlMessage.startsWith("username;")) {
				username = extractParameters(controlMessage);
				if (log.isDebugEnabled()) {
					log.debug(String.format("Username '%s'", username));
				}

				updateChat();
			} else {
				log.warn(String.format("Received unknown control message '%s'", controlMessage));
			}
		}
	}

	private void receive() throws IOException {
		byte[] data = receiveBytes();
		if (data != null) {
			process(data);
		}
	}

	private void processData(byte[] data, int offset, int length) throws IOException {
		byte[] buffer = new byte[length];
		System.arraycopy(data, offset, buffer, 0, length);

		// I do receive all messages 2 times, not sure why.
		// Filter out any received message which is identical to the previously received message.
		if (filterDuplicateMessages) {
			if (lastDataReceived != null && buffer.length == lastDataReceived.length && Utilities.equals(buffer, 0, lastDataReceived, 0, length)) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Dropping duplicate data message"));
				}
				lastDataReceived = null;
				return;
			}
			lastDataReceived = buffer.clone();
		}

		if (length >= 12 + rawIdentifier.length && Utilities.memcmp(data, offset + 12, rawIdentifier, 0, rawIdentifier.length) == 0) {
			processRawMessage(data, offset + 12 + rawIdentifier.length, length - 12 - rawIdentifier.length);
			return;
		}

		if (workaroundBugMulticast) {
			if (isAnyMacAddress(buffer)) {
				byte[] gameModeGroupAddress = MMIOHandlerWlan.getInstance().getGameModeGroupAddress();
				if (!isEmptyMacAddress(gameModeGroupAddress)) {
					System.arraycopy(gameModeGroupAddress, 0, buffer, 0, MAC_ADDRESS_LENGTH);
				}
			}
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Processing data message %s", Utilities.getMemoryDump(buffer)));
		}

		receivedData.add(buffer);
	}

	private void connect() throws IOException {
		connected = false;
		String connectString = String.format("connect;%s;%s;", uniqueIdentifier, applicationName);

		send(connectString);

		// Wait for confirmation of connection
		long start = getNow();
		while (!connected) {
			receive();

			long now = getNow();
			if (now - start > timeout) {
				int response = showConfirmDialog(null, getBundle("jpcsp/languages/jpcsp").getString("XLinkKai.cannotConnect"), "XLink Kai", OK_CANCEL_OPTION, WARNING_MESSAGE);
				if (response == OK_OPTION) {
					// Retry to connect
					send(connectString);
					start = getNow();
				} else {
					log.error(String.format("Could not connect to XLink Kai, please make sure it is started"));
					throw new IOException("Could not connect to XLink Kai");
				}
			}
		}
	}

	private void disconnect() throws IOException {
		disconnected = false;
		send(String.format("disconnect;%s;%s;", uniqueIdentifier, applicationName));

		// Wait for confirmation of disconnection
		long start = getNow();
		while (!disconnected) {
			receive();

			long now = getNow();
			if (now - start > timeout) {
				log.error(String.format("Cannot disconnect from XLink Kai, please make sure it is started"));
				throw new IOException("Cannot disconnect from XLink Kai");
			}
		}
	}

	private void enableChat() throws IOException {
		send("setting;chat;true;");
		send("getplayernames;");
	}

	@Override
	public void start() throws IOException {
		uniqueIdentifier = String.format("%s_%s", applicationName, convertMacAddressToString(Wlan.getMacAddress()));

		while (true) {
			try {
				destAddr = InetAddress.getByName(destServer);
				break;
			} catch (UnknownHostException e) {
				int response = showConfirmDialog(null, getBundle("jpcsp/languages/jpcsp").getString("XLinkKai.cannotConnect"), "XLink Kai", OK_CANCEL_OPTION, WARNING_MESSAGE);
				if (response == OK_OPTION) {
					// Retry
				} else {
					throw e;
				}
			}
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("XLink Kai server %s(%s), port %d", destServer, destAddr, destPort));
		}

		// Create socket for DDS communication
		socket = new DatagramSocket();
		// Non-blocking (timeout = 0 would mean blocking)
		socket.setSoTimeout(1);

		// Init state variables
		connected = false;
		disconnected = false;
		receivedData.clear();
		gameModeCounter = 0;

		connect();
		enableChat();
		send("getusername;");
	}

	@Override
	public void stop() throws IOException {
		closeChat();
		disconnect();
	}

	@Override
	public void sendWlanPacket(byte[] buffer, int offset, int length) throws IOException {
		// In GameMode, the master is sending a beacon frame before sending its GameMode data packet 
		byte[] gameModeGroupAddress = MMIOHandlerWlan.getInstance().getGameModeGroupAddress();
		if (pspNetMacAddress.equals(gameModeGroupAddress, 0, buffer, offset) && MMIOHandlerWlan.getInstance().isGameModeMaster()) {
			// If sending in GameMode and I am the Master, send a Beacon frame, including the Sony Specific GameMode counter
			gameModeCounter = (gameModeCounter + 1) & 0xF;
			sendBeacon(MMIOHandlerWlan.getInstance().getSsid(), 1, gameModeCounter);
		}

		sendDataPacket(buffer, offset, length);
	}

	@Override
	public void sendAccessPointPacket(byte[] buffer, int offset, int length, EtherFrame etherFrame) throws IOException {
		log.error("Unimplemented sendAccessPointPacket");
	}

	@Override
	public void sendGameModePacket(pspNetMacAddress macAddress, byte[] buffer, int offset, int length) throws IOException {
		log.error("Unimplemented sendGameModePacket");
	}

	@Override
	public int receiveWlanPacket(byte[] buffer, int offset, int length) throws IOException {
		receive();

		if (receivedData.size() <= 0) {
			return -1;
		}

		byte[] bytes = receivedData.remove(0);
		int receivedLength = Math.min(bytes.length, length);
		System.arraycopy(bytes, 0, buffer, offset, receivedLength);

		return receivedLength;
	}

	@Override
	public int receiveGameModePacket(pspNetMacAddress macAddress, byte[] buffer, int offset, int length) throws IOException {
		// TODO receiveGameModePacket not implemented
		return -1;
	}

	@Override
	public void wlanScan(String ssid, int[] channels) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("wlanScan ssid=%s, channels=%s", ssid, channels));
		}

		// Send a Probe Request
		sendProbeRequest(ssid);
	}

	private void writeMacAddress(byte[] buffer, int offset, byte[] macAddress, int macAddressOffset) {
		System.arraycopy(macAddress, macAddressOffset, buffer, offset, MAC_ADDRESS_LENGTH);
	}

	private void writeMacAddress(byte[] buffer, int offset, byte[] macAddress) {
		writeMacAddress(buffer, offset, macAddress, 0);
	}

	private void sendRawMessage(byte[] buffer, int offset, int length) throws IOException {
		if (log.isTraceEnabled()) {
			log.trace(String.format("sendRawMessage length=0x%X: %s", length, Utilities.getMemoryDump(buffer, offset, length)));
		}

		byte[] dataPacket = new byte[length + 12 + rawIdentifier.length];
		writeMacAddress(dataPacket, 0, ANY_MAC_ADDRESS); // Destination MAC Address
		writeMacAddress(dataPacket, 6, getMacAddress()); // Source MAC Address
		System.arraycopy(rawIdentifier, 0, dataPacket, 12, rawIdentifier.length);
		System.arraycopy(buffer, offset, dataPacket, 12 + rawIdentifier.length, length);

		sendDataPacket(dataPacket, 0, dataPacket.length);
	}

	private int addHeader(byte[] buffer, int offset, int type, byte[] destMacAddress, int destMacAddressOffset, byte[] bssId) {
		write8(buffer, offset, 0); // Header revision
		offset++;
		write8(buffer, offset, 0); // Header pad
		offset++;
		int headerLengthOffset = offset;
		writeUnaligned16(buffer, offset, 0); // Header length (will be updated below)
		offset += 2;
		writeUnaligned32(buffer, offset, 0x0000482E); // Present flags
		offset += 4;
		write8(buffer, offset, 0); // Flags
		offset++;
		write8(buffer, offset, 0x04); // Data Rate
		offset++;
		writeUnaligned16(buffer, offset, 2412); // Channel frequency
		offset += 2;
		writeUnaligned16(buffer, offset, 0x00A0); // Channel flags
		offset += 2;
		write8(buffer, offset, -45); // Antenna signal: -45 dBm
		offset++;
		write8(buffer, offset, 1); // Antenna
		offset++;
		writeUnaligned16(buffer, offset, 0x0000); // RX flags
		offset += 2;
		writeUnaligned16(buffer, headerLengthOffset, offset);

		write8(buffer, offset, type); // Frame Control Version/Type/Subtype for Probe Response
		offset++;
		write8(buffer, offset, 0x00); // Frame Control Flags
		offset++;
		writeUnaligned16(buffer, offset, 0xFFFF); // Duration
		offset += 2;

		writeMacAddress(buffer, offset, destMacAddress, destMacAddressOffset); // Destination address
		offset += MAC_ADDRESS_LENGTH;
		writeMacAddress(buffer, offset, getMacAddress()); // Source address
		offset += MAC_ADDRESS_LENGTH;
		writeMacAddress(buffer, offset, bssId); // BSS Id
		offset += MAC_ADDRESS_LENGTH;
		writeUnaligned16(buffer, offset, 0x0000); // Fragment number/Sequence number
		offset += 2;

		return offset;
	}

	private int addProbeHeader(byte[] buffer, int offset) {
		writeUnaligned64(buffer, offset, 0L); // Timestamp
		offset += 8;
		writeUnaligned16(buffer, offset, 100); // Beacon interval
		offset += 2;
		writeUnaligned16(buffer, offset, 0x0022); // Capabilities (WLAN_CAPABILITY_IBSS | WLAN_CAPABILITY_SHORT_PREAMBLE)
		offset += 2;

		return offset;
	}

	private int addTagSSID(byte[] buffer, int offset, String ssid) {
		write8(buffer, offset, 0x00); // Tag Number: SSID parameter set
		offset++;
		byte[] ssidBytes = ssid == null ? new byte[0] : ssid.getBytes();
		write8(buffer, offset, ssidBytes.length); // Tag length
		offset++;
		if (ssidBytes.length > 0) {
			System.arraycopy(ssidBytes, 0, buffer, offset, ssidBytes.length);
			offset += ssidBytes.length;
		}

		return offset;
	}

	private int addTagCurrentChannel(byte[] buffer, int offset, int channel) {
		write8(buffer, offset, 0x03); // Tag Number: Current Channel
		offset++;
		write8(buffer, offset, 1); // Tag length
		offset++;
		write8(buffer, offset, channel); // Channel number
		offset++;

		return offset;
	}

	private int addTagSupportedRates(byte[] buffer, int offset) {
		write8(buffer, offset, 0x01); // Tag Number: Supported Rates
		offset++;
		write8(buffer, offset, 4); // Tag length
		offset++;
		write8(buffer, offset, 0x82); // Supported Rates: 1(B)
		offset++;
		write8(buffer, offset, 0x84); // Supported Rates: 2(B)
		offset++;
		write8(buffer, offset, 0x0B); // Supported Rates: 5.5
		offset++;
		write8(buffer, offset, 0x16); // Supported Rates: 11
		offset++;

		return offset;
	}

	private int addTagATIMWindow(byte[] buffer, int offset) {
		write8(buffer, offset, 0x06); // Tag Number: ATIM window
		offset++;
		write8(buffer, offset, 2); // Tag length
		offset++;
		writeUnaligned16(buffer, offset, 0); // ATIM window 0
		offset += 2;

		return offset;
	}

	private void sendProbeRequest(String ssid) throws IOException {
		byte[] buffer = new byte[100];
		int offset = 0;

		offset = addHeader(buffer, offset, 0x40, ANY_MAC_ADDRESS, 0, ANY_MAC_ADDRESS);
		offset = addTagSSID(buffer, offset, ssid);
		offset = addTagSupportedRates(buffer, offset);

		sendRawMessage(buffer, 0, offset);
	}

	private void sendProbeResponse(byte[] destMacAddress, int destMacAddresssOffset, String ssid, int channel) throws IOException {
		byte[] buffer = new byte[100];
		int offset = 0;

		offset = addHeader(buffer, offset, 0x50, destMacAddress, destMacAddresssOffset, getMacAddress());
		offset = addProbeHeader(buffer, offset);
		offset = addTagSSID(buffer, offset, ssid);
		offset = addTagSupportedRates(buffer, offset);
		offset = addTagCurrentChannel(buffer, offset, channel);
		offset = addTagATIMWindow(buffer, offset);

		sendRawMessage(buffer, 0, offset);
	}

	private void sendBeacon(String ssid, int channel, int gameModeCounter) throws IOException {
		byte[] buffer = new byte[200];
		int offset = 0;

		offset = addHeader(buffer, offset, 0x80, ANY_MAC_ADDRESS, 0, getMacAddress());
		offset = addProbeHeader(buffer, offset);
		offset = addTagSSID(buffer, offset, ssid);
		offset = addTagCurrentChannel(buffer, offset, channel);
		offset = addTagSupportedRates(buffer, offset);
		offset = addTagATIMWindow(buffer, offset);

		if (gameModeCounter >= 0) {
			write8(buffer, offset, 0xDD); // Tag Number: Vendor Specific
			offset++;
			write8(buffer, offset, 10); // Tag length
			offset++;
			write8(buffer, offset + 0, 0x00); // OUI: Sony
			write8(buffer, offset + 1, 0x04); // OUI: Sony
			write8(buffer, offset + 2, 0x1F); // OUI: Sony
			offset += 3;
			// Vendor Specific Data
			write8(buffer, offset + 0, 0x00); 
			write8(buffer, offset + 1, 0x00); 
			write8(buffer, offset + 2, 0x02); 
			write8(buffer, offset + 3, gameModeCounter);
			write8(buffer, offset + 4, 0x02);
			write8(buffer, offset + 5, 0x0F);
			write8(buffer, offset + 6, 0x08);
			offset += 7;
		}

		sendRawMessage(buffer, 0, offset);
	}

	private boolean isSSIDMatching(String ssid, String matchSsid) {
		// Not connected to any SSID, never matching
		if (ssid == null) {
			return false;
		}

		// Always matching the wildcard SSID
		if (matchSsid == null || matchSsid.length() == 0) {
			return true;
		}

		return matchSsid.equals(ssid);
	}

	private void processRawMessage(byte[] buffer, int offset, int length) throws IOException {
		if (log.isTraceEnabled()) {
			log.trace(String.format("processRawMessage length=0x%X: %s", length, Utilities.getMemoryDump(buffer, offset, length)));
		}

		int headerRevision = read8(buffer, offset + 0);
		if (headerRevision == 0x00) {
			int headerLength = readUnaligned16(buffer, offset + 2);
			final int headerLength802_11 = 24;
			if (headerLength + headerLength802_11 > length) {
				if (log.isTraceEnabled()) {
					log.trace(String.format("Received raw message with an incorrect header length: packet length=0x%X, header length=0x%X", length, headerLength));
				}
				return;
			}

			final int sourceMacAddressOffset = offset + headerLength + 10;
			if (pspNetMacAddress.isMyMacAddress(buffer, sourceMacAddressOffset)) {
				log.trace(String.format("processRawMessage ignoring raw message coming from myself"));
				return;
			}

			int frameControlField = readUnaligned16(buffer, offset + headerLength);
			if (frameControlField == 0x0080) {
				// Beacon frame
				byte[] gameModeGroupAddress = MMIOHandlerWlan.getInstance().getGameModeGroupAddress();
				if (isEmptyMacAddress(gameModeGroupAddress)) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("processRawMessage Beacon frame, sending own Beacon"));
					}
					sendBeacon(MMIOHandlerWlan.getInstance().getSsid(), 1, -1);
				} else {
					if (log.isDebugEnabled()) {
						log.debug(String.format("processRawMessage Beacon frame in GameMode, ignoring"));
					}
				}
			} else if (frameControlField == 0x0040) {
				// Probe Request
				int channel = 1;
				String matchSsid = null;
				for (int i = offset + headerLength + headerLength802_11; i < length; i += 2) {
					int tagNumber = read8(buffer, i);
					int tagLength = read8(buffer, i + 1);
					if (tagNumber == 0) {
						matchSsid = readStringNZ(buffer, i + 2, tagLength);
					} else if (tagNumber == 3 && tagLength >= 1) {
						channel = read8(buffer, i + 2);
					}
					i += tagLength;
				}

				String currentSsid = MMIOHandlerWlan.getInstance().getSsid();
				if (isSSIDMatching(currentSsid, matchSsid)) {
					// Send the Probe Response back to the sender of the Probe Request
					if (log.isDebugEnabled()) {
						log.debug(String.format("processRawMessage Probe Request, simulating Probe Response to %s with SSID=%s, channel=%d", pspNetMacAddress.toString(buffer, sourceMacAddressOffset), matchSsid, channel));
					}

					sendProbeResponse(buffer, sourceMacAddressOffset, currentSsid, channel);
				} else {
					if (log.isDebugEnabled()) {
						log.debug(String.format("processRawMessage Probe Request, non-matching SSID from %s with SSID=%s, channel=%d", pspNetMacAddress.toString(buffer, sourceMacAddressOffset), matchSsid, channel));
					}
				}
			} else if (frameControlField == 0x0050) {
				// Probe Response
				int channel = 1;
				String ssid = null;
				pspNetMacAddress sourceMacAddress = new pspNetMacAddress(buffer, sourceMacAddressOffset);
				byte[] ibss = sourceMacAddress.macAddress;
				for (int i = offset + headerLength + headerLength802_11; i < length; i += 2) {
					int tagNumber = read8(buffer, i);
					int tagLength = read8(buffer, i + 1);
					if (tagNumber == 0) {
						ssid = readStringNZ(buffer, i + 2, tagLength);
					} else if (tagNumber == 3 && tagLength >= 1) {
						channel = read8(buffer, i + 2);
					}
					i += tagLength;
				}
				if (log.isDebugEnabled()) {
					log.debug(String.format("processRawMessage Probe Response sourceMacAddress=%s, ssid=%s, channel=%d", sourceMacAddress, ssid, channel));
				}
				Modules.sceNetAdhocctlModule.hleNetAdhocctlAddNetwork(sourceMacAddress, ssid, ibss, channel);
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("processRawMessage unknown frameControl=0x%04X", frameControlField));
				}
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug(String.format("processRawMessage unknown headerRevision=0x%02X", headerRevision));
			}
		}
	}

	private void openChat() {
		if (!hasChatEnabled()) {
			return;
		}

		if (chatGUI == null || !chatGUI.isVisible()) {
			chatGUI = new ChatGUI();
			updateChat();
			Emulator.getMainGUI().startBackgroundWindowDialog(chatGUI);
		}
	}

	private void closeChat() {
		if (chatGUI != null) {
			chatGUI.dispose();
			chatGUI = null;
		}
	}

	private void updateChat() {
		if (chatGUI != null) {
			chatGUI.setMyNickName(username);
			chatGUI.setGroupName(gameName);
			if (arena != null && arena.length() > 0) {
				chatGUI.setTitle(String.format("Chat in %s", arena));
			} else {
				chatGUI.setTitle("Chat");
			}
		}
	}

	@Override
	public void sendChatMessage(String message) {
		try {
			send("chat;" + message);
		} catch (IOException e) {
			if (log.isDebugEnabled()) {
				log.debug("sendChatMessage", e);
			}
		}
	}
}
