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

import static jpcsp.HLE.modules.sceNet.convertMacAddressToString;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.hardware.Wlan;
import jpcsp.network.BaseWlanAdapter;
import jpcsp.network.protocols.EtherFrame;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

public class XLinkKaiWlanAdapter extends BaseWlanAdapter {
	public static Logger log = Logger.getLogger("XLinkKai");
	private static boolean enabled = false;
	private DatagramSocket socket;
	private String uniqueIdentifier;
	private InetAddress destAddr;
	final static private int destPort = 34523;
	final static private String applicationName = "Jpcsp";
	private volatile boolean connected;
	private volatile boolean disconnected;
	private List<byte[]> receivedData = new LinkedList<byte[]>();

	private static class EnabledSettingsListener extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setEnabled(value);
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

	private void send(byte[] buffer, int offset, int length) throws IOException {
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
		byte[] bytes = new byte[length + 4];
		bytes[0] = 'e';
		bytes[1] = ';';
		bytes[2] = 'e';
		bytes[3] = ';';
		System.arraycopy(buffer, offset, bytes, 4, length);

		send(bytes, 0, bytes.length);
	}

	private byte[] receiveBytes() throws IOException {
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
				String message = controlMessage.substring(8);
				if (log.isInfoEnabled()) {
					log.info(String.format("Received message '%s'", message));
				}
			} else if (controlMessage.startsWith("chat;")) {
				String message = controlMessage.substring(5);
				if (log.isInfoEnabled()) {
					log.info(String.format("Received chat '%s'", message));
				}
			} else if (controlMessage.startsWith("directmessage;")) {
				String message = controlMessage.substring(14);
				if (log.isInfoEnabled()) {
					log.info(String.format("Received direct message '%s'", message));
				}
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
		if (log.isDebugEnabled()) {
			log.debug(String.format("Processing data message %s", Utilities.getMemoryDump(data, offset, length)));
		}

		byte[] buffer = new byte[length];
		System.arraycopy(data, offset, buffer, 0, length);

		receivedData.add(buffer);
	}

	private void connect() throws IOException {
		connected = false;
		send(String.format("connect;%s;%s;", uniqueIdentifier, applicationName));

		// Wait for confirmation of connection
		while (!connected) {
			receive();
		}
	}

	private void disconnect() throws IOException {
		disconnected = false;
		send(String.format("disconnect;%s;%s;", uniqueIdentifier, applicationName));

		// Wait for confirmation of disconnection
		while (!disconnected) {
			receive();
		}
	}

	private void enableChat() throws IOException {
		send("setting;chat;true;");
	}

	@Override
	public void start() throws IOException {
		uniqueIdentifier = String.format("%s_%s", applicationName, convertMacAddressToString(Wlan.getMacAddress()));

		destAddr = InetAddress.getByName("localhost");

		// Create socket for DDS communication
		socket = new DatagramSocket();
		// Non-blocking (timeout = 0 would mean blocking)
		socket.setSoTimeout(1);

		// Init state variables
		connected = false;
		disconnected = false;
		receivedData.clear();

		connect();
		enableChat();
	}

	@Override
	public void stop() throws IOException {
		disconnect();
	}

	@Override
	public void sendWlanPacket(byte[] buffer, int offset, int length) throws IOException {
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
		log.error("Unimplemented receiveGameModePacket");
		return -1;
	}

	@Override
	public void wlanScan() throws IOException {
		log.error("Unimplemented wlanScan");
	}
}
