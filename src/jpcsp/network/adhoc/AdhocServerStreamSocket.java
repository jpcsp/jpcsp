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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * @author gid15
 *
 */
public class AdhocServerStreamSocket extends AdhocSocket {
	private ServerSocket serverSocket;

	@Override
	public int bind(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		serverSocket.setSoTimeout(1);

		return serverSocket.getLocalPort();
	}

	@Override
	public void connect(SocketAddress socketAddress, int port) throws IOException {
		log.error(String.format("Connect not supported on ServerSocket: address=%s, port=%d", socketAddress, port));
	}

	@Override
	public void close() throws IOException {
		if (serverSocket != null) {
			serverSocket.close();
			serverSocket = null;
		}
	}

	@Override
	public void setTimeout(int millis) throws SocketException {
		serverSocket.setSoTimeout(millis);
	}

	@Override
	public void send(SocketAddress socketAddress, AdhocMessage adhocMessage) throws IOException {
		log.error(String.format("Send not supported on ServerSocket: address=%s, message=%s", socketAddress, adhocMessage));
	}

	@Override
	public int receive(byte[] buffer, int size) throws IOException {
		log.error(String.format("Receive not supported on ServerSocket"));
		return -1;
	}

	@Override
	public AdhocSocket accept() throws IOException {
		Socket socket = serverSocket.accept();

		if (socket == null) {
			return null;
		}

		AdhocSocket adhocSocket = new AdhocStreamSocket(socket);
		// Provide information about the accepted socket
		adhocSocket.setReceivedAddress(socket.getInetAddress());
		adhocSocket.setReceivedPort(socket.getPort());

		return adhocSocket;
	}
}
