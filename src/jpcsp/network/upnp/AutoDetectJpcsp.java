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
package jpcsp.network.upnp;

import static jpcsp.HLE.modules150.sceNetApctl.getLocalHostIP;
import static jpcsp.network.upnp.UPnP.discoveryPort;
import static jpcsp.network.upnp.UPnP.discoveryTimeoutMillis;
import static jpcsp.network.upnp.UPnP.multicastIp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpcsp.HLE.Modules;

import org.apache.log4j.Logger;

public class AutoDetectJpcsp {
	protected static Logger log = Logger.getLogger("network");
	private static AutoDetectJpcsp instance = null;
	private ListenerThread listenerThread;
	private static final String deviceName = "Jpcsp";

	public static AutoDetectJpcsp getInstance() {
		if (instance == null) {
			instance = new AutoDetectJpcsp();
		}
		return instance;
	}

	private AutoDetectJpcsp() {
	}

	public boolean isOtherJpcspAvailable() {
		boolean found = false;
		try {
			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(discoveryTimeoutMillis);
			socket.setReuseAddress(true);
			String discoveryRequest = String.format("M-SEARCH * HTTP/1.1\r\nHOST: %s:%d\r\nST: %s\r\n\r\n", multicastIp, discoveryPort, deviceName);
			DatagramPacket packet = new DatagramPacket(discoveryRequest.getBytes(), discoveryRequest.length(), new InetSocketAddress(multicastIp, discoveryPort));
			socket.send(packet);
			byte[] response = new byte[1536];
			DatagramPacket responsePacket = new DatagramPacket(response, response.length);
			socket.receive(responsePacket);
			if (responsePacket.getLength() > 0) {
				String reply = new String(responsePacket.getData(), responsePacket.getOffset(), responsePacket.getLength());
				log.debug(String.format("Discovery %s: %s", deviceName, reply));
				Pattern p = Pattern.compile("^location: *(\\S+):(\\d+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
				Matcher m = p.matcher(reply);
				if (m.find()) {
					String address = m.group(1);
					int port = Integer.parseInt(m.group(2));
					log.info(String.format("Found %s at location: address='%s', port=%d", deviceName, address, port));
					if (address.equals(getLocalHostIP())) {
						Modules.sceNetAdhocModule.setNetClientPortShift(port);
						Modules.sceNetAdhocModule.setNetServerPortShift(0);
						found = true;
					}
				} else {
					log.error(String.format("Could not parse discovery response for %s: %s", deviceName, reply));
				}
			}
			socket.close();
		} catch (SocketTimeoutException e) {
			log.debug(String.format("Timeout while discovering Jpcsp: %s", e.getMessage()));
		} catch (IOException e) {
			log.error("Discover Jpcsp", e);
		}

		return found;
	}

	public void startDaemon() {
		listenerThread = new ListenerThread();
		listenerThread.setName("AutoDetectJpcsp - ListenerThread");
		listenerThread.setDaemon(true);
		listenerThread.start();
	}

	public static void exit() {
		if (instance != null) {
			if (instance.listenerThread != null) {
				instance.listenerThread.exit();
				instance.listenerThread = null;
			}
			instance = null;
		}
	}

	private class ListenerThread extends Thread {
		private boolean exit = false;

		@Override
		public void run() {
			log.debug(String.format("Starting AutoDetectJpcsp ListenerThread"));
			byte[] response = new byte[256];

			while (!exit) {
				try {
					InetAddress listenAddress = InetAddress.getByName(multicastIp);
					MulticastSocket socket = new MulticastSocket(discoveryPort);
					socket.joinGroup(listenAddress);
					while (!exit) {
						DatagramPacket packet = new DatagramPacket(response, response.length);
						socket.receive(packet);
						processRequest(socket, new String(packet.getData(), packet.getOffset(), packet.getLength()), packet.getAddress(), packet.getPort());
					}
					socket.close();
				} catch (IOException e) {
					log.error("ListenerThread", e);
					exit();
				}
			}
		}

		private void processRequest(MulticastSocket socket, String request, InetAddress address, int port) throws IOException {
			log.debug(String.format("Received '%s' from %s:%d", request, address, port));

			Pattern p = Pattern.compile("SEARCH +\\* +.*^ST: *" + deviceName + "$.*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
			Matcher m = p.matcher(request);
			if (m.find()) {
				StringBuilder response = new StringBuilder();
				int netServerPortShift = Modules.sceNetAdhocModule.getRealPortFromServerPort(0);
				if (netServerPortShift == 0) {
					// Set a default server port shift if none has been set.
					netServerPortShift = 100;
					Modules.sceNetAdhocModule.setNetServerPortShift(netServerPortShift);
					Modules.sceNetAdhocModule.setNetClientPortShift(0);
				}
				response.append(String.format("Location: %s:%d", getLocalHostIP(), netServerPortShift));

				log.debug(String.format("Sending response '%s' to %s:%d", response, address, port));
				DatagramPacket packet = new DatagramPacket(response.toString().getBytes(), response.length(), address, port);
				socket.send(packet);
			}
		}

		public void exit() {
			exit = true;
		}
	}
}
