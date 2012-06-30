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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * @author gid15
 *
 */
public class AdhocStreamSocket extends AdhocSocket {
	private Socket socket;

	@Override
	public int bind(int port) throws IOException {
		socket = new Socket();
		socket.bind(new InetSocketAddress(port));
		socket.setSoTimeout(1);

		return socket.getLocalPort();
	}

	@Override
	public void close() throws IOException {
		socket.close();
		socket = null;
	}

	@Override
	public void setTimeout(int millis) throws SocketException {
		socket.setSoTimeout(millis);
	}

	@Override
	public void send(SocketAddress socketAddress, AdhocMessage adhocMessage) throws IOException {
		socket.getOutputStream().write(adhocMessage.getMessage());
	}

	@Override
	public int receive(byte[] buffer, int size) throws IOException {
		return socket.getInputStream().read(buffer, 0, size);
	}

	@Override
	public void connect(SocketAddress socketAddress, int port) throws IOException {
		socket.connect(socketAddress);
	}
}
