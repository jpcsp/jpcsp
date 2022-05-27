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
package jpcsp.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

public class AutoDetectLocalIPAddress {
	public static Logger log = Logger.getLogger("network");
	private static AutoDetectLocalIPAddress instance = null;
    // Try to use a port unused by other applications...
    private static final int serverSocketPort = 30005;
	private InetAddress localIPAddress;
	private ServerSocket localIPSocket;
	private AutoDetectAcceptOnServerSocket autoDetectAcceptOnServerSocket;

	private class AutoDetectAcceptOnServerSocket extends Thread {
		@Override
		public void run() {
			while (localIPSocket != null) {
				acceptOnServerSocket();
			}
		}
	}

	public static AutoDetectLocalIPAddress getInstance() {
		if (instance == null) {
			instance = new AutoDetectLocalIPAddress();
		}

		return instance;
	}

	private AutoDetectLocalIPAddress() {
	}

	public InetAddress getLocalIPAddress() {
		if (localIPAddress == null) {
			detectLocalIPAddress();
		}

		return localIPAddress;
	}

	private void detectLocalIPAddress() {
		for (int i = 1; i < 128; i++) {
			try {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Trying to Auto Detect Local IP Address 127.0.0.%d", i));
				}
				localIPAddress = InetAddress.getByAddress(new byte[] { 127, 0, 0, (byte) i });
				localIPSocket = new ServerSocket(serverSocketPort, 10, localIPAddress);

				autoDetectAcceptOnServerSocket = new AutoDetectAcceptOnServerSocket();
				autoDetectAcceptOnServerSocket.setName("Auto Detect Accept On Server Socket");
				autoDetectAcceptOnServerSocket.setDaemon(true);
				autoDetectAcceptOnServerSocket.start();

				if (log.isDebugEnabled()) {
					log.debug(String.format("Successfully Auto Detected Local IP Address %s", localIPAddress));
				}
				// Local IP Address found
				break;
			} catch (UnknownHostException e) {
				// Ignore exception
			} catch (IOException e) {
				// Ignore exception
			}

			localIPAddress = null;
			if (localIPSocket != null) {
				try {
					localIPSocket.close();
				} catch (IOException e) {
					// Ignore exception
				}
			}
			localIPSocket = null;
		}
	}

	private void acceptOnServerSocket() {
		try {
			Socket socket = localIPSocket.accept();
			if (log.isDebugEnabled()) {
				log.debug(String.format("acceptOnServerSocket socket=%s", socket));
			}
			socket.close();
		} catch (IOException e) {
			// Ignore exception
		}
	}
}
