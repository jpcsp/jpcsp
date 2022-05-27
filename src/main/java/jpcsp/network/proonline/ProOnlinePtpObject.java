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
package jpcsp.network.proonline;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.adhoc.AdhocMessage;
import jpcsp.network.adhoc.AdhocServerStreamSocket;
import jpcsp.network.adhoc.AdhocSocket;
import jpcsp.network.adhoc.AdhocStreamSocket;
import jpcsp.network.adhoc.PtpObject;

/**
 * @author gid15
 *
 */
public class ProOnlinePtpObject extends PtpObject {
	final protected ProOnlineNetworkAdapter proOnline;
	final private static String socketProtocol = "TCP";
	protected boolean isServerSocket;

	public ProOnlinePtpObject(ProOnlinePtpObject ptpObject) {
		super(ptpObject);
		proOnline = ptpObject.proOnline;
	}

	public ProOnlinePtpObject(INetworkAdapter networkAdapter) {
		super(networkAdapter);
		proOnline = (ProOnlineNetworkAdapter) networkAdapter;
	}

	@Override
	protected boolean pollAccept(int peerMacAddr, int peerPortAddr, SceKernelThreadInfo thread) {
		boolean acceptCompleted = false;

		try {
			AdhocSocket acceptedSocket = socket.accept();
			if (acceptedSocket != null) {
				byte[] destMacAddress = proOnline.getMacAddress(acceptedSocket.getReceivedAddress());
				if (destMacAddress != null) {
					// Return the accepted peer address and port
					pspNetMacAddress peerMacAddress = new pspNetMacAddress(destMacAddress);
					int peerPort = acceptedSocket.getReceivedPort();
					Memory mem = Memory.getInstance();
					if (peerMacAddr != 0) {
						peerMacAddress.write(mem, peerMacAddr);
					}
					if (peerPortAddr != 0) {
						mem.write16(peerPortAddr, (short) peerPort);
					}

					// As a result of the "accept" call, create a new PTP Object
					PtpObject ptpObject = new ProOnlinePtpObject(this);
					// Add information about the accepted peer address and port
					ptpObject.setDestMacAddress(peerMacAddress);
					ptpObject.setDestPort(peerPort);
					ptpObject.setSocket(acceptedSocket);

					// Add the received socket as a new Ptp Object
					Modules.sceNetAdhocModule.hleAddPtpObject(ptpObject);

					// Return the ID of the new PTP Object
					setReturnValue(thread, ptpObject.getId());

					if (log.isDebugEnabled()) {
						log.debug(String.format("accept completed, creating new Ptp object %s", ptpObject));
					}

					acceptCompleted = true;
				}
			}
		} catch (SocketTimeoutException e) {
			// Ignore exception
		} catch (IOException e) {
			log.error("pollAccept", e);
		}

		return acceptCompleted;
	}

	@Override
	protected boolean pollConnect(SceKernelThreadInfo thread) {
		// A StreamSocket is always connected
		return true;
	}

	@Override
	public boolean canAccept() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canConnect() {
		// A StreamSocket is always connected
		return true;
	}

	@Override
	protected AdhocSocket createSocket() throws UnknownHostException, IOException {
		return isServerSocket ? new AdhocServerStreamSocket() : new AdhocStreamSocket();
	}

	@Override
	public int open() {
		// Open the TCP port in the router
		proOnline.sceNetPortOpen(socketProtocol, getPort());

		// This is a normal socket, no server socket
		isServerSocket = false;

		return super.open();
	}

	@Override
	public int listen() {
		// Open the TCP port in the router
		proOnline.sceNetPortOpen(socketProtocol, getPort());

		// This is a server socket
		isServerSocket = true;

		return super.listen();
	}

	@Override
	public void delete() {
		// Close the TCP port in the router
		proOnline.sceNetPortClose(socketProtocol, getPort());

		super.delete();
	}

	@Override
	protected boolean isForMe(AdhocMessage adhocMessage, int port, InetAddress address) {
		// Always for me on stream sockets
		return true;
	}
}
