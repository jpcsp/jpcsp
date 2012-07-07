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
package jpcsp.network.jpcsp;

import static jpcsp.network.adhoc.AdhocMessage.MAX_HEADER_SIZE;
import static jpcsp.network.jpcsp.JpcspAdhocPtpMessage.PTP_MESSAGE_TYPE_CONNECT;
import static jpcsp.network.jpcsp.JpcspAdhocPtpMessage.PTP_MESSAGE_TYPE_CONNECT_CONFIRM;
import static jpcsp.network.jpcsp.JpcspAdhocPtpMessage.PTP_MESSAGE_TYPE_DATA;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.adhoc.AdhocDatagramSocket;
import jpcsp.network.adhoc.AdhocMessage;
import jpcsp.network.adhoc.AdhocSocket;
import jpcsp.network.adhoc.PtpObject;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class JpcspPtpObject extends PtpObject {
	private JpcspAdhocPtpMessage connectRequest;
	private int connectRequestPort;
	private JpcspAdhocPtpMessage connectConfirm;
	private boolean connected;

	public JpcspPtpObject(PtpObject ptpObject) {
		super(ptpObject);
	}

	public JpcspPtpObject(INetworkAdapter networkAdapter) {
		super(networkAdapter);
	}

	@Override
	public boolean canAccept() {
		return connectRequest != null;
	}

	@Override
	public boolean canConnect() {
		return connectConfirm != null;
	}

	@Override
	public int connect(int timeout, int nonblock) {
		int result = 0;

		try {
			JpcspAdhocPtpMessage adhocPtpMessage = new JpcspAdhocPtpMessage(PTP_MESSAGE_TYPE_CONNECT);
			send(adhocPtpMessage);

			result = super.connect(timeout, nonblock);
		} catch (SocketException e) {
			log.error("connect", e);
		} catch (IOException e) {
			log.error("connect", e);
		}

		return result;
	}

	@Override
	protected boolean pollAccept(int peerMacAddr, int peerPortAddr, SceKernelThreadInfo thread) {
		boolean acceptCompleted = false;
		Memory mem = Memory.getInstance();

		try {
			// Process a previously received connect message, if available
			JpcspAdhocPtpMessage adhocPtpMessage = connectRequest;
			int adhocPtpMessagePort = connectRequestPort;
			if (adhocPtpMessage == null) {
				byte[] bytes = new byte[getBufSize() + MAX_HEADER_SIZE];
				int length = socket.receive(bytes, bytes.length);
				adhocPtpMessage = new JpcspAdhocPtpMessage(bytes, length);
				adhocPtpMessagePort = socket.getReceivedPort();

				if (log.isDebugEnabled()) {
					log.debug(String.format("pollAccept: received message %s", adhocPtpMessage));
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("pollAccept: processing pending message %s", adhocPtpMessage));
				}
			}

			if (adhocPtpMessage.isForMe()) {
				switch (adhocPtpMessage.getType()) {
					case PTP_MESSAGE_TYPE_CONNECT:
						pspNetMacAddress peerMacAddress = new pspNetMacAddress();
						peerMacAddress.setMacAddress(adhocPtpMessage.getFromMacAddress());
						int peerPort = Modules.sceNetAdhocModule.getClientPortFromRealPort(adhocPtpMessage.getFromMacAddress(), adhocPtpMessagePort);

						if (peerMacAddr != 0) {
							peerMacAddress.write(mem, peerMacAddr);
						}
						if (peerPortAddr != 0) {
							mem.write16(peerPortAddr, (short) peerPort);
						}

						// As a result of the "accept" call, create a new PTP Object
						PtpObject ptpObject = new JpcspPtpObject(this);
						ptpObject.setDestMacAddress(peerMacAddress);
						ptpObject.setDestPort(peerPort);
						ptpObject.setPort(0);
						Modules.sceNetAdhocModule.hleAddPtpObject(ptpObject);

						// Return the ID of the new PTP Object
						setReturnValue(thread, ptpObject.getId());

						// Get a new free port
						ptpObject.setPort(0);
						ptpObject.openSocket();

						// Send a connect confirmation message including the new port
						JpcspAdhocPtpMessage confirmMessage = new JpcspAdhocPtpMessage(PTP_MESSAGE_TYPE_CONNECT_CONFIRM);
						confirmMessage.setDataInt32(ptpObject.getPort());
						ptpObject.send(confirmMessage);

						if (log.isDebugEnabled()) {
							log.debug(String.format("accept completed, creating new Ptp object %s", ptpObject));
						}

						acceptCompleted = true;
						connectRequest = null;
						break;
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("pollAccept: received a message not for me: %s", adhocPtpMessage));
				}
			}
		} catch (SocketException e) {
			log.error("pollAccept", e);
		} catch (SocketTimeoutException e) {
			// Ignore exception
		} catch (IOException e) {
			log.error("pollAccept", e);
		}

		return acceptCompleted;
	}

	@Override
	protected boolean pollConnect(SceKernelThreadInfo thread) {
		boolean connectCompleted = false;

		try {
			// Process a previously received confirm message, if available
			JpcspAdhocPtpMessage adhocPtpMessage = connectConfirm;
			if (adhocPtpMessage == null) {
				byte[] bytes = new byte[getBufSize() + MAX_HEADER_SIZE];
				int length = socket.receive(bytes, bytes.length);
				adhocPtpMessage = new JpcspAdhocPtpMessage(bytes, length);
				if (log.isDebugEnabled()) {
					log.debug(String.format("pollConnect: received message %s", adhocPtpMessage));
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("pollConnect: processing pending message %s", adhocPtpMessage));
				}
			}

			if (adhocPtpMessage.isForMe()) {
				switch (adhocPtpMessage.getType()) {
					case PTP_MESSAGE_TYPE_CONNECT_CONFIRM:
						// Connect successfully completed, retrieve the new destination port
						int port = Modules.sceNetAdhocModule.getClientPortFromRealPort(adhocPtpMessage.getFromMacAddress(), adhocPtpMessage.getDataInt32());
						if (log.isDebugEnabled()) {
							log.debug(String.format("Received connect confirmation, changing destination port from %d to %d", getDestPort(), port));
						}
						setDestPort(port);
						setReturnValue(thread, 0);
						connectConfirm = null;
						connectCompleted = true;
						break;
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("pollConnect: received a message not for me: %s", adhocPtpMessage));
				}
			}
		} catch (SocketException e) {
			log.error("pollConnect", e);
		} catch (SocketTimeoutException e) {
			// Ignore exception
			JpcspAdhocPtpMessage adhocPtpMessage = new JpcspAdhocPtpMessage(PTP_MESSAGE_TYPE_CONNECT);
			try {
				send(adhocPtpMessage);
			} catch (IOException e1) {
			}
		} catch (IOException e) {
			log.error("pollConnect", e);
		}

		if (connectCompleted) {
			connected = true;
		}

		return connectCompleted;
	}

	@Override
	protected boolean isForMe(AdhocMessage adhocMessage, int port, InetAddress address) {
		if (adhocMessage instanceof JpcspAdhocPtpMessage) {
			JpcspAdhocPtpMessage adhocPtpMessage = (JpcspAdhocPtpMessage) adhocMessage;
			int type = adhocPtpMessage.getType();
			if (type == PTP_MESSAGE_TYPE_CONNECT_CONFIRM) {
				if (connected) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Received connect confirmation but already connected, discarding"));
					}
				} else {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Received connect confirmation, processing later"));
					}
					connectConfirm = adhocPtpMessage;
				}
				return false;
			} else if (type == PTP_MESSAGE_TYPE_CONNECT) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Received connect request, processing later"));
				}
				connectRequest = adhocPtpMessage;
				connectRequestPort = port;
				return false;
			} else if (type != PTP_MESSAGE_TYPE_DATA) {
				return false;
			}
		}

		return super.isForMe(adhocMessage, port, address);
	}

	// For Ptp sockets, data is stored in the internal buffer as a continuous byte stream.
	// The organization in packets doesn't matter.
	@Override
	protected void addReceivedMessage(AdhocMessage adhocMessage, int port) {
		int length = Math.min(adhocMessage.getDataLength(), getBufSize() - getRcvdData());
		int addr = buffer.addr + getRcvdData();
		adhocMessage.writeDataToMemory(addr, length);
		rcvdData += length;
		if (log.isDebugEnabled()) {
			log.debug(String.format("Successfully received message %s", adhocMessage));
			if (log.isTraceEnabled()) {
				log.trace(String.format("Message data: %s", Utilities.getMemoryDump(addr, length)));
			}
		}
	}

	@Override
	protected void closeSocket() {
		super.closeSocket();
		connected = false;
	}

	@Override
	protected AdhocSocket createSocket() {
		return new AdhocDatagramSocket();
	}
}
