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
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import jpcsp.HLE.modules150.sceNetAdhoc;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public abstract class AdhocSocket {
	protected static Logger log = sceNetAdhoc.log;
	private int receivedPort;
	private InetAddress receivedAddress;

	public abstract int bind(int port) throws IOException;
	public abstract void connect(SocketAddress socketAddress, int port) throws IOException;
	public abstract void close() throws IOException;
	public abstract void setTimeout(int millis) throws SocketException;
	public abstract void send(SocketAddress socketAddress, AdhocMessage adhocMessage) throws IOException;
	public abstract int receive(byte[] buffer, int size) throws IOException;

	public int getReceivedPort() {
		return receivedPort;
	}

	protected void setReceivedPort(int receivedPort) {
		this.receivedPort = receivedPort;
	}

	public InetAddress getReceivedAddress() {
		return receivedAddress;
	}

	public void setReceivedAddress(InetAddress receivedAddress) {
		this.receivedAddress = receivedAddress;
	}
}
