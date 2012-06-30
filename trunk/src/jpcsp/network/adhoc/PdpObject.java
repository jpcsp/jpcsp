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
import java.net.BindException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.INetworkAdapter;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public abstract class PdpObject extends AdhocObject {
	/** MAC address */
	private pspNetMacAddress macAddress;
	/** Bytes received */
	protected int rcvdData;
	private LinkedList<AdhocBufferMessage> rcvdMessages = new LinkedList<AdhocBufferMessage>();

	public PdpObject(INetworkAdapter networkAdapter) {
		super(networkAdapter);
	}

	public PdpObject(PdpObject pdpObject) {
		super(pdpObject);
		macAddress = pdpObject.macAddress;
	}

	public pspNetMacAddress getMacAddress() {
		return macAddress;
	}

	public void setMacAddress(pspNetMacAddress macAddress) {
		this.macAddress = macAddress;
	}

	public int getRcvdData() {
		return rcvdData;
	}

	public int create(pspNetMacAddress macAddress, int port, int bufSize) {
		int result = getId();

		setMacAddress(macAddress);
		setPort(port);
		setBufSize(bufSize);
		try {
			openSocket();
		} catch (BindException e) {
			if (log.isDebugEnabled()) {
				log.debug("create", e);
			}
			result = SceKernelErrors.ERROR_NET_ADHOC_PORT_IN_USE;
		} catch (SocketException e) {
			log.error("create", e);
		} catch (UnknownHostException e) {
			log.error("create", e);
		} catch (IOException e) {
			log.error("create", e);
		}

		return result;
	}

	public int send(pspNetMacAddress destMacAddress, int destPort, TPointer data, int length, int timeout, int nonblock) {
		int result = 0;

		try {
			openSocket();
			setTimeout(timeout, nonblock);
			AdhocMessage adhocMessage = networkAdapter.createAdhocPdpMessage(data.getAddress(), length, destMacAddress.macAddress);
			send(adhocMessage, destPort);
		} catch (SocketException e) {
			log.error("send", e);
		} catch (UnknownHostException e) {
			log.error("send", e);
		} catch (SocketTimeoutException e) {
			log.error("send", e);
		} catch (IOException e) {
			log.error("send", e);
		}

		return result;
	}

	// For Pdp sockets, data is stored in the internal buffer as a sequence of packets.
	// The organization in packets must be kept for reading.
	protected void addReceivedMessage(AdhocMessage adhocMessage, int port) {
		AdhocBufferMessage bufferMessage = new AdhocBufferMessage();
		bufferMessage.length = adhocMessage.getDataLength();
		bufferMessage.macAddress.setMacAddress(adhocMessage.getFromMacAddress());
		bufferMessage.port = Modules.sceNetAdhocModule.getClientPortFromRealPort(adhocMessage.getFromMacAddress(), port);
		bufferMessage.offset = rcvdData;
		adhocMessage.writeDataToMemory(buffer.addr + bufferMessage.offset);

		// Update the timestamp of the peer
		Modules.sceNetAdhocctlModule.hleNetAdhocctlPeerUpdateTimestamp(adhocMessage.getFromMacAddress());

		if (log.isDebugEnabled()) {
			log.debug(String.format("Successfully received %d bytes from %s on port %d(%d): %s", bufferMessage.length, bufferMessage.macAddress, bufferMessage.port, port, Utilities.getMemoryDump(buffer.addr + bufferMessage.offset, bufferMessage.length, 1, 16)));
		}

		rcvdData += bufferMessage.length;
		rcvdMessages.add(bufferMessage);
	}

	private void removeFirstReceivedMessage() {
		AdhocBufferMessage bufferMessage = rcvdMessages.removeFirst();
		if (bufferMessage == null) {
			return;
		}

		if (rcvdData > bufferMessage.length) {
			// Move the remaining buffer data to the beginning of the buffer
			Memory.getInstance().memcpy(buffer.addr, buffer.addr + bufferMessage.length, rcvdData - bufferMessage.length);
			for (AdhocBufferMessage rcvdMessage : rcvdMessages) {
				rcvdMessage.offset -= bufferMessage.length;
			}
		}
		rcvdData -= bufferMessage.length;
	}

	// For Pdp sockets, data is read one packet at a time.
	// The caller has to provide enough space to fully read the available packet.
	public int recv(pspNetMacAddress srcMacAddress, TPointer16 portAddr, TPointer data, TPointer32 dataLengthAddr, int timeout, int nonblock) {
		int result = nonblock != 0 ? SceKernelErrors.ERROR_NET_ADHOC_NO_DATA_AVAILABLE : SceKernelErrors.ERROR_NET_ADHOC_TIMEOUT;
		int length = dataLengthAddr.getValue();

		if (rcvdMessages.isEmpty()) {
			update();
		}

		if (!rcvdMessages.isEmpty()) {
			AdhocBufferMessage bufferMessage = rcvdMessages.getFirst();
			if (length < bufferMessage.length) {
				// Buffer is too small to contain all the available data.
				// Return the buffer size that would be required.
				dataLengthAddr.setValue(bufferMessage.length);
				result = SceKernelErrors.ERROR_NET_BUFFER_TOO_SMALL;
			} else {
				// Copy the data already received
				dataLengthAddr.setValue(bufferMessage.length);
				Memory.getInstance().memcpy(data.getAddress(), buffer.addr + bufferMessage.offset, bufferMessage.length);
				if (srcMacAddress != null) {
					srcMacAddress.setMacAddress(bufferMessage.macAddress.macAddress);
				}
				if (portAddr != null) {
					portAddr.setValue(bufferMessage.port);
				}

				removeFirstReceivedMessage();

				if (log.isDebugEnabled()) {
					log.debug(String.format("Returned received data: %d bytes from %s on port %d: %s", dataLengthAddr.getValue(), bufferMessage.macAddress, portAddr.getValue(), Utilities.getMemoryDump(data.getAddress(), dataLengthAddr.getValue(), 4, 16)));
				}
				result = 0;
			}
		}

		return result;
	}

	public void update() {
		// Receive all messages available
		while (rcvdData < getBufSize()) {
			try {
				openSocket();
				socket.setTimeout(1);
				byte[] bytes = new byte[getBufSize() - rcvdData + AdhocMessage.MAX_HEADER_SIZE];
				int length = socket.receive(bytes, bytes.length);
				int receivedPort = socket.getReceivedPort();
				InetAddress receivedAddress = socket.getReceivedAddress();
				AdhocMessage adhocMessage = createAdhocMessage(bytes, length);
				if (isForMe(adhocMessage, receivedPort, receivedAddress)) {
					if (getRcvdData() + adhocMessage.getDataLength() <= getBufSize()) {
						addReceivedMessage(adhocMessage, receivedPort);
					} else {
						if (log.isDebugEnabled()) {
							log.debug(String.format("Discarded message, receive buffer full (%d of %d): %s", getRcvdData(), getBufSize(), adhocMessage));
						}
					}
				} else {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Received message not for me: %s", adhocMessage));
					}
				}
			} catch (SocketException e) {
				log.error("update", e);
				break;
			} catch (SocketTimeoutException e) {
				// Timeout
				break;
			} catch (IOException e) {
				log.error("update", e);
				break;
			}
		}
	}

	protected AdhocMessage createAdhocMessage(byte[] message, int length) {
		return networkAdapter.createAdhocPdpMessage(message, length);
	}

	protected boolean isForMe(AdhocMessage adhocMessage, int port, InetAddress address) {
		return adhocMessage.isForMe();
	}

	@Override
	public String toString() {
		return String.format("PdpObject[id=%d, macAddress=%s, port=%d, bufSize=%d, rcvdData=%d]", getId(), macAddress, getPort(), getBufSize(), rcvdData);
	}
}
