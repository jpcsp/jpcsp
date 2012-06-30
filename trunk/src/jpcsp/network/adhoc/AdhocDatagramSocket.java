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
package jpcsp.network.adhoc;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * @author gid15
 *
 */
public class AdhocDatagramSocket extends AdhocSocket {
	private DatagramSocket socket;

	@Override
	public int bind(int port) throws SocketException {
		if (port == 0) {
			socket = new DatagramSocket();
			if (log.isDebugEnabled()) {
				log.debug(String.format("Opening socket on free local port %d", socket.getLocalPort()));
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Opening socket on real port %d", port));
			}
			socket = new DatagramSocket(port);
		}
		socket.setBroadcast(true);
		socket.setSoTimeout(1);

		return socket.getLocalPort();
	}

	@Override
	public void close() {
		socket.close();
		socket = null;
	}

	@Override
	public void setTimeout(int millis) throws SocketException {
		socket.setSoTimeout(millis);
	}

	@Override
	public void send(SocketAddress socketAddress, AdhocMessage adhocMessage) throws IOException {
		DatagramPacket packet = new DatagramPacket(adhocMessage.getMessage(), adhocMessage.getMessageLength(), socketAddress);
		socket.send(packet);
	}

	@Override
	public int receive(byte[] buffer, int size) throws IOException {
		DatagramPacket packet = new DatagramPacket(buffer, size);
		socket.receive(packet);
		setReceivedPort(packet.getPort());
		setReceivedAddress(packet.getAddress());

		return packet.getLength();
	}

	@Override
	public void connect(SocketAddress socketAddress, int port) throws IOException {
		// Nothing to do for Datagrams
	}
}
