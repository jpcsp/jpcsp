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
package jpcsp.network.jpcsp;

import static jpcsp.HLE.Modules.sceWlanModule;
import static jpcsp.HLE.modules.sceWlan.WLAN_MODE_INFRASTRUCTURE;
import static jpcsp.HLE.modules.sceWlan.channels;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.hardware.Wlan.getLocalInetAddress;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.sceNetAdhoc;
import jpcsp.HLE.modules.sceNetInet;
import jpcsp.hardware.Wlan;
import jpcsp.network.BaseWlanAdapter;
import jpcsp.network.protocols.EtherFrame;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class JpcspWlanAdapter extends BaseWlanAdapter {
    public static final byte WLAN_CMD_DATA          = (byte) 0;
    public static final byte WLAN_CMD_SCAN_REQUEST  = (byte) 1;
    public static final byte WLAN_CMD_SCAN_RESPONSE = (byte) 2;
    private static int wlanSocketPort = 30010;
    private DatagramSocket wlanSocket;

    private boolean createWlanSocket() {
    	if (wlanSocket == null) {
			boolean retry;
			do {
				retry = false;
	    		try {
	    			InetAddress localInetAddress = getLocalInetAddress();
					wlanSocket = new DatagramSocket(wlanSocketPort, getLocalInetAddress());
		    		// For broadcast
					wlanSocket.setBroadcast(true);
		    		// Non-blocking (timeout = 0 would mean blocking)
					wlanSocket.setSoTimeout(1);
					if (log.isDebugEnabled()) {
						log.debug(String.format("createWlanSocket successful on port %d, localInetAddress %s", wlanSocketPort, localInetAddress));
					}
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

	@Override
	public void start() throws IOException {
		if (!createWlanSocket()) {
			throw new IOException("Cannot create JpcspWlanAdapter socket");
		}
	}

    public static int getSocketPort() {
    	return wlanSocketPort;
    }

	private int getBroadcastPort(int channel) {
    	if (channel >= 0 && sceWlanModule.getChannelMode(channel) == WLAN_MODE_INFRASTRUCTURE) {
    		return sceWlanModule.getAccessPoint().getPort();
    	}

    	if (Wlan.hasLocalInetAddress()) {
    		return wlanSocketPort;
    	}

    	// If no specific local IP address has been assigned, use port shifting
    	return wlanSocketPort ^ 1;
    }

    private void sendPacket(byte[] buffer, int offset, int length) throws IOException {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sendPacket %s", Utilities.getMemoryDump(buffer, offset, length)));
    	}

		InetSocketAddress broadcastAddress[] = sceNetInet.getBroadcastInetSocketAddress(getBroadcastPort(sceWlanModule.getJoinedChannel()));
		if (broadcastAddress != null) {
			for (int i = 0; i < broadcastAddress.length; i++) {
				DatagramPacket packet = new DatagramPacket(buffer, offset, length, broadcastAddress[i]);
				try {
					wlanSocket.send(packet);
					if (log.isTraceEnabled()) {
						log.trace(String.format("sendPacket successful on %s", broadcastAddress[i]));
					}
				} catch (SocketException e) {
					// Ignore "Network is unreachable"
					if (log.isDebugEnabled()) {
						log.debug(String.format("sendPacket on %s", broadcastAddress[i]), e);
					}
				}
			}
		}
    }

	@Override
	public void sendWlanPacket(byte[] buffer, int offset, int length) throws IOException {
    	if (!createWlanSocket()) {
    		return;
    	}

    	byte[] packetBuffer = new byte[length + 1 + 32];
    	int packetOffset = 0;
    	// Add the cmd in front of the data
    	packetBuffer[packetOffset] = WLAN_CMD_DATA;
    	packetOffset++;
    	// Add the joined SSID in front of the data
		Utilities.writeStringNZ(packetBuffer, packetOffset, 32, Modules.sceWlanModule.getJoinedChannelSSID());
    	packetOffset += 32;
    	// Add the data
    	System.arraycopy(buffer, offset, packetBuffer, packetOffset, length);
    	packetOffset += length;

    	sendPacket(packetBuffer, 0, packetOffset);
	}

    private void processCmd(byte cmd, byte[] buffer, int offset, int length) throws IOException {
    	byte[] packetMacAddress = new byte[MAC_ADDRESS_LENGTH];
    	System.arraycopy(buffer, offset, packetMacAddress, 0, MAC_ADDRESS_LENGTH);
    	offset += MAC_ADDRESS_LENGTH;
    	length -= MAC_ADDRESS_LENGTH;

    	if (sceNetAdhoc.isMyMacAddress(packetMacAddress)) {
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

    			scanResponse[responseOffset] = (byte) sceWlanModule.getChannelMode(channel);
    			responseOffset++;

    			Utilities.writeStringNZ(scanResponse, responseOffset, 32, sceWlanModule.getChannelSSID(channel));
        		responseOffset += 32;
    		}
    		sendPacket(scanResponse, 0, responseOffset);
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
    				if (channel != sceWlanModule.getJoinedChannel()) {
    					sceWlanModule.setChannelSSID(channel, ssid, mode);
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

    @Override
	public int receiveWlanPacket(byte[] buffer, int offset, int length) throws IOException {
		int dataLength = -1;

		if (!createWlanSocket()) {
    		return dataLength;
    	}

    	byte[] bytes = new byte[length + 1 + 32];
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
		try {
			wlanSocket.receive(packet);
			if (log.isDebugEnabled()) {
				log.debug(String.format("receiveWlanPacket message: %s", Utilities.getMemoryDump(packet.getData(), packet.getOffset(), packet.getLength())));
			}

			byte[] dataBytes = packet.getData();
			int dataOffset = packet.getOffset();
			dataLength = packet.getLength();

			if (dataLength < 33) {
				return -1;
			}

			byte cmd = dataBytes[dataOffset];
			dataOffset++;
			dataLength--;
			if (cmd != WLAN_CMD_DATA) {
				processCmd(cmd, dataBytes, dataOffset, dataLength);
				return -1;
			}

			String ssid = Utilities.readStringNZ(dataBytes, dataOffset, 32);
			dataOffset += 32;
			dataLength -= 32;

			if (sceWlanModule.hasJoinedChannel() && !ssid.equals(sceWlanModule.getJoinedChannelSSID())) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("receiveWlanPacket message SSID('%s') not matching the joined SSID('%s')", ssid, sceWlanModule.getJoinedChannelSSID()));
				}
				return -1;
			}

			System.arraycopy(dataBytes, dataOffset, buffer, offset, dataLength);
		} catch (SocketTimeoutException e) {
			// Timeout can be ignored as we are polling
		}

		return dataLength;
	}

	@Override
	public void sendGameModePacket(pspNetMacAddress macAddress, byte[] buffer, int offset, int length) throws IOException {
		byte[] bytes = new byte[macAddress.sizeof() + length];
		int bytesOffset = 0;

		System.arraycopy(macAddress.macAddress, 0, bytes, 0, macAddress.sizeof());
		bytesOffset += macAddress.sizeof();

		System.arraycopy(buffer, offset, bytes, bytesOffset, length);
		bytesOffset += length;

		sendWlanPacket(bytes, 0, bytesOffset);
	}

	@Override
	public int receiveGameModePacket(pspNetMacAddress macAddress, byte[] buffer, int offset, int length) throws IOException {
		int dataLength = -1;

    	if (!createWlanSocket()) {
    		return dataLength;
    	}

    	byte[] bytes = new byte[length + 1 + 32 + macAddress.sizeof()];
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
		try {
			wlanSocket.receive(packet);
			if (log.isDebugEnabled()) {
				log.debug(String.format("receiveGameModePacket message: %s", Utilities.getMemoryDump(packet.getData(), packet.getOffset(), packet.getLength())));
			}

			byte[] dataBytes = packet.getData();
			int dataOffset = packet.getOffset();
			dataLength = packet.getLength();

			byte cmd = dataBytes[dataOffset];
			dataOffset++;
			dataLength--;
			if (cmd != WLAN_CMD_DATA) {
				processCmd(cmd, dataBytes, dataOffset, dataLength);
				return -1;
			}

			String ssid = Utilities.readStringNZ(dataBytes, dataOffset, 32);
			dataOffset += 32;
			dataLength -= 32;

			if (sceWlanModule.hasJoinedChannel() && !ssid.equals(sceWlanModule.getJoinedChannelSSID())) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("receiveGameModePacket message SSID('%s') not matching the joined SSID('%s')", ssid, sceWlanModule.getJoinedChannelSSID()));
				}
				return -1;
			}

			macAddress.setMacAddress(dataBytes, dataOffset);
			dataOffset += macAddress.sizeof();
			dataLength -= macAddress.sizeof();

			System.arraycopy(dataBytes, dataOffset, buffer, offset, dataLength);
		} catch (SocketTimeoutException e) {
			// Timeout can be ignored as we are polling
		}

		return dataLength;
	}

	@Override
	public void wlanScan(String ssid, int[] channels) throws IOException {
    	// Send a scan request packet
    	byte[] scanRequestPacket = new byte[1 + MAC_ADDRESS_LENGTH];
    	scanRequestPacket[0] = WLAN_CMD_SCAN_REQUEST;
    	System.arraycopy(Wlan.getMacAddress(), 0, scanRequestPacket, 1, MAC_ADDRESS_LENGTH);
		sendPacket(scanRequestPacket, 0, scanRequestPacket.length);
	}

	@Override
	public void sendAccessPointPacket(byte[] buffer, int offset, int length, EtherFrame etherFrame) throws IOException {
		InetSocketAddress broadcastAddress[] = sceNetInet.getBroadcastInetSocketAddress(getSocketPort());
		if (broadcastAddress != null) {
			for (int i = 0; i < broadcastAddress.length; i++) {
				DatagramPacket packet = new DatagramPacket(buffer, offset, length, broadcastAddress[i]);
				try {
					wlanSocket.send(packet);
				} catch (SocketException e) {
					// Ignore "Network is unreachable"
					if (log.isDebugEnabled()) {
						log.debug("sendPacketFromAccessPoint", e);
					}
				}
			}
		}
	}

	@Override
	public void sendChatMessage(String message) {
		log.info(String.format("Chat: %s", message));
	}
}
