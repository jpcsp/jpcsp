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
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.modules.sceNetAdhoc;
import jpcsp.HLE.modules150.SysMemUserForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.network.INetworkAdapter;

/**
 * @author gid15
 *
 */
public abstract class AdhocObject {
	protected static Logger log = sceNetAdhoc.log;
	private static final String uidPurpose = "sceNetAdhoc";

	/** uid */
	private final int id;
	private int port;
	protected AdhocSocket socket;
	/** Buffer size */
	private int bufSize;
	protected SysMemInfo buffer;
	/** Network Adapter */
	protected final INetworkAdapter networkAdapter;

	public AdhocObject(INetworkAdapter networkAdapter) {
		this.networkAdapter = networkAdapter;
		id = SceUidManager.getNewUid(uidPurpose);
	}

	public AdhocObject(AdhocObject adhocObject) {
		networkAdapter = adhocObject.networkAdapter;
		id = SceUidManager.getNewUid(uidPurpose);
		port = adhocObject.port;
		setBufSize(adhocObject.bufSize);
	}

	public int getId() {
		return id;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getBufSize() {
		return bufSize;
	}

	public void setBufSize(int bufSize) {
		this.bufSize = bufSize;
		if (buffer != null) {
			Modules.SysMemUserForUserModule.free(buffer);
			buffer = null;
		}
		buffer = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, Modules.sceNetAdhocModule.getName(), SysMemUserForUser.PSP_SMEM_Low, bufSize, 0);
	}

	public void delete() {
		closeSocket();
		if (buffer != null) {
			Modules.SysMemUserForUserModule.free(buffer);
			buffer = null;
		}
		SceUidManager.releaseUid(id, uidPurpose);
	}

	public void openSocket() throws UnknownHostException, IOException {
		if (socket == null) {
			socket = createSocket();
			if (getPort() == 0) {
				int localPort = socket.bind(getPort());
				setPort(localPort);
			} else {
				int realPort = Modules.sceNetAdhocModule.getRealPortFromServerPort(getPort());
				socket.bind(realPort);
			}
		}
	}

	protected abstract AdhocSocket createSocket() throws UnknownHostException, IOException;

	protected void closeSocket() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				log.error("Error while closing Adhoc socket", e);
			}
			socket = null;
		}
	}

	protected void setTimeout(int timeout, int nonblock) throws SocketException {
		if (nonblock != 0) {
			socket.setTimeout(1);
		} else {
			// SoTimeout accepts milliseconds, PSP timeout is given in microseconds
			socket.setTimeout(Math.max(timeout / 1000, 1));
		}
	}

	protected void send(AdhocMessage adhocMessage) throws IOException {
		send(adhocMessage, getPort());
	}

	protected void send(AdhocMessage adhocMessage, int destPort) throws IOException {
		if (adhocMessage == null) {
			// Nothing to send
			return;
		}

		openSocket();

		int realPort = Modules.sceNetAdhocModule.getRealPortFromClientPort(adhocMessage.getToMacAddress(), destPort);
		SocketAddress socketAddress = Modules.sceNetAdhocModule.getSocketAddress(adhocMessage.getToMacAddress(), realPort);
		socket.send(socketAddress, adhocMessage);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Successfully sent %d bytes to port %d(%d): %s", adhocMessage.getDataLength(), destPort, realPort, adhocMessage));
		}
	}

	public void setSocket(AdhocSocket socket) {
		this.socket = socket;
	}
}
