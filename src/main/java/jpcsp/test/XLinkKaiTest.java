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
package jpcsp.test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.modules.sceNet;
import jpcsp.hardware.Wlan;
import jpcsp.util.LWJGLFixer;
import jpcsp.util.Utilities;

public class XLinkKaiTest {
	public static Logger log;
	private DatagramSocket socket;
	private String uniqueIdentifier;
	private InetAddress destAddr;
	final static private int destPort = 34523;
	final static private String applicationName = "Jpcsp";
	private ReadThread readThread;
	private volatile boolean connected;
	private volatile boolean disconnected;

	private class ReadThread extends Thread {
		private volatile boolean exit;

		@Override
		public void run() {
			RuntimeContext.setLog4jMDC();

			while (!exit) {
				try {
					byte[] data = receiveBytes();
					if (data != null) {
						process(data);
					}
				} catch (IOException e) {
					if (!exit) {
						log.error(e);
					}
				}
			}

			// Forget myself when exiting the thread
			readThread = null;
		}

		public void exit() {
			exit = true;
		}
	}

	public static void main(String[] args) {
        LWJGLFixer.fixOnce();
        DOMConfigurator.configure("LogSettings.xml");
        log = Logger.getLogger("XLinkKai");
		RuntimeContext.setLog4jMDC();
        Wlan.initialize();

        XLinkKaiTest test = new XLinkKaiTest();
        try {
//			test.testConnectDisconnect();
			test.testListen();
		} catch (IOException e) {
			log.error(e);
		}
	}

	private void send(byte[] buffer, int offset, int length) throws IOException {
		log.debug(String.format("Sending bytes %s", Utilities.getMemoryDump(buffer, offset, length)));

		DatagramPacket packet = new DatagramPacket(buffer, offset, length, destAddr, destPort);
		socket.send(packet);
	}

	private void send(String s) throws IOException {
		log.debug(String.format("Sending '%s'", s));

		byte[] bytes = s.getBytes();
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

		log.debug(String.format("Received bytes from %s: %s", packet.getAddress(), Utilities.getMemoryDump(data)));

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

			log.debug(String.format("Processing control message '%s'", controlMessage));

			if (controlMessage.startsWith("connected;")) {
				connected = true;
			} else if (controlMessage.startsWith("disconnected;")) {
				disconnected = true;
			} else if (controlMessage.startsWith("keepalive;")) {
				log.debug(String.format("Received keepalive"));
				// Send a keepalive response
				send("keepalive;");
			} else if (controlMessage.startsWith("message;")) {
				String message = controlMessage.substring(8);
				log.info(String.format("Received message '%s'", message));
			} else if (controlMessage.startsWith("chat;")) {
				String message = controlMessage.substring(5);
				log.info(String.format("Received chat '%s'", message));
			} else if (controlMessage.startsWith("directmessage;")) {
				String message = controlMessage.substring(14);
				log.info(String.format("Received direct message '%s'", message));
			} else {
				log.warn(String.format("Received unknown control message '%s'", controlMessage));
			}
		}
	}

	private void processData(byte[] data, int offset, int length) throws IOException {
		log.debug(String.format("Processing data message %s", Utilities.getMemoryDump(data, offset, length)));
	}

	private void connect() throws IOException {
		connected = false;
		send(String.format("connect;%s;%s;", uniqueIdentifier, applicationName));

		// Wait for confirmation of connection
		while (!connected) {
			// Wait 1ms
			Utilities.sleep(1, 0);
		}
	}

	private void disconnect() throws IOException {
		disconnected = false;
		send(String.format("disconnect;%s;%s;", uniqueIdentifier, applicationName));

		// Wait for confirmation of disconnection
		while (!disconnected) {
			// Wait 1ms
			Utilities.sleep(1, 0);
		}
	}

	private void enableChat() throws IOException {
		send("setting;chat;true;");
	}

	private void init() throws IOException {
		uniqueIdentifier = String.format("%s_%s", applicationName, sceNet.convertMacAddressToString(Wlan.getMacAddress()));

		destAddr = InetAddress.getByName("localhost");

		// Create socket for DDS communication
		socket = new DatagramSocket();
		// Non-blocking (timeout = 0 would mean blocking)
		socket.setSoTimeout(1);

		// Init state variables
		connected = false;
		disconnected = false;

		// Start read thread
		readThread = new ReadThread();
		readThread.setName("XLink Kai Read Thread");
		readThread.setDaemon(true);
		readThread.start();
	}

	private void exit() throws IOException {
		readThread.exit();
		socket.close();
	}

	public void testConnectDisconnect() throws IOException {
		init();

		// Try a simple connect/disconnect
		connect();
		disconnect();

		exit();
	}

	public void testListen() throws IOException {
		init();

		// Connect and listen indefinitely for any data received
		connect();
		enableChat();
		while (!disconnected) {
			// Wait 10ms
			Utilities.sleep(10, 0);
		}

		exit();
	}
}
