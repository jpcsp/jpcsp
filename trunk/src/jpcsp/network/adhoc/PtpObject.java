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
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.INetworkAdapter;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public abstract class PtpObject extends PdpObject {
	/** Destination MAC address */
	private pspNetMacAddress destMacAddress;
	/** Destination port */
	private int destPort;
	/** Retry delay */
	private int retryDelay;
	/** Retry count */
	private int retryCount;
	/** Queue size */
	private int queue;
	/** Bytes sent */
	private int sentData;
    // Polling period (micro seconds) for blocking operations
    protected static final int BLOCKED_OPERATION_POLLING_MICROS = 10000;
    private AdhocMessage receivedMessage;
    private int receivedMessageOffset;

    protected static abstract class BlockedPtpAction extends BlockedPdpAction {
    	protected final PtpObject ptpObject;

    	protected BlockedPtpAction(PtpObject ptpObject, int timeout) {
    		super(ptpObject, timeout);
    		this.ptpObject = ptpObject;
    	}
    }

    protected static class BlockedPtpAccept extends BlockedPtpAction {
    	private final int peerMacAddr;
    	private final int peerPortAddr;

    	public BlockedPtpAccept(PtpObject ptpObject, int peerMacAddr, int peerPortAddr, int timeout) {
    		super(ptpObject, timeout);
    		this.peerMacAddr = peerMacAddr;
    		this.peerPortAddr = peerPortAddr;
    	}

		@Override
		protected boolean poll() throws IOException {
			return ptpObject.pollAccept(peerMacAddr, peerPortAddr, thread);
		}

		@Override
		protected int getExceptionResult(IOException e) {
			return SceKernelErrors.ERROR_NET_ADHOC_TIMEOUT;
		}
    }

    protected static class BlockedPtpConnect extends BlockedPtpAction {
    	public BlockedPtpConnect(PtpObject ptpObject, int timeout) {
    		super(ptpObject, timeout);
    	}

		@Override
		protected boolean poll() throws IOException {
			return ptpObject.pollConnect(thread);
		}

		@Override
		protected int getExceptionResult(IOException e) {
			return SceKernelErrors.ERROR_NET_ADHOC_CONNECTION_REFUSED;
		}
    }

    protected static class BlockedPtpRecv extends BlockedPtpAction {
    	final protected TPointer data;
    	final protected TPointer32 dataLengthAddr;

		protected BlockedPtpRecv(PtpObject ptpObject, TPointer data, TPointer32 dataLengthAddr, int timeout) {
			super(ptpObject, timeout);
			this.data = data;
			this.dataLengthAddr = dataLengthAddr;
		}

		@Override
		protected boolean poll() throws IOException {
			return ptpObject.pollRecv(data, dataLengthAddr, thread);
		}

		@Override
		protected int getExceptionResult(IOException e) {
			return SceKernelErrors.ERROR_NET_ADHOC_TIMEOUT;
		}
    	
    }

    public PtpObject(PtpObject ptpObject) {
		super(ptpObject);
		destMacAddress = ptpObject.destMacAddress;
		destPort = ptpObject.destPort;
		retryDelay = ptpObject.retryDelay;
		retryCount = ptpObject.retryCount;
		queue = ptpObject.queue;
	}

	public PtpObject(INetworkAdapter networkAdapter) {
		super(networkAdapter);
	}

	public pspNetMacAddress getDestMacAddress() {
		return destMacAddress;
	}

	public void setDestMacAddress(pspNetMacAddress destMacAddress) {
		this.destMacAddress = destMacAddress;
	}

	public int getDestPort() {
		return destPort;
	}

	public void setDestPort(int destPort) {
		this.destPort = destPort;
	}

	public int getRetryDelay() {
		return retryDelay;
	}

	public void setRetryDelay(int retryDelay) {
		this.retryDelay = retryDelay;
	}

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public int getQueue() {
		return queue;
	}

	public void setQueue(int queue) {
		this.queue = queue;
	}

	public int getSentData() {
		return sentData;
	}

	@Override
	public void openSocket() throws UnknownHostException, IOException {
		if (socket == null) {
			super.openSocket();
			if (getDestMacAddress() != null) {
				int realDestPort = Modules.sceNetAdhocModule.getRealPortFromClientPort(getDestMacAddress().macAddress, getDestPort());
				SocketAddress socketAddress = Modules.sceNetAdhocModule.getSocketAddress(getDestMacAddress().macAddress, realDestPort);
				socket.connect(socketAddress, realDestPort);
			}
		}
	}

	public int open() {
		int result = 0;

		try {
			openSocket();
		} catch (BindException e) {
			if (log.isDebugEnabled()) {
				log.debug("open", e);
			}
			result = SceKernelErrors.ERROR_NET_ADHOC_PORT_IN_USE;
		} catch (SocketException e) {
			log.error("open", e);
		} catch (UnknownHostException e) {
			log.error("open", e);
		} catch (IOException e) {
			log.error("open", e);
		}

		return result;
	}

	public int listen() {
		int result = 0;

		try {
			openSocket();
		} catch (BindException e) {
			if (log.isDebugEnabled()) {
				log.debug("listen", e);
			}
			result = SceKernelErrors.ERROR_NET_ADHOC_PORT_IN_USE;
		} catch (SocketException e) {
			log.error("listen", e);
		} catch (UnknownHostException e) {
			log.error("listen", e);
		} catch (IOException e) {
			log.error("listen", e);
		}

		return result;
	}

	public int accept(int peerMacAddr, int peerPortAddr, int timeout, int nonblock) {
		int result = 0;

		SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
		if (pollAccept(peerMacAddr, peerPortAddr, thread)) {
			// Accept completed immediately
			result = thread.cpuContext._v0;
		} else if (nonblock != 0) {
			// Accept cannot be completed in non-blocking mode
			result = SceKernelErrors.ERROR_NET_ADHOC_NO_DATA_AVAILABLE;
		} else {
			// Block current thread
			BlockedPtpAction blockedPtpAction = new BlockedPtpAccept(this, peerMacAddr, peerPortAddr, timeout);
			blockedPtpAction.blockCurrentThread();
		}

		return result;
	}

	public int connect(int timeout, int nonblock) {
		int result = 0;

		if (!pollConnect(Modules.ThreadManForUserModule.getCurrentThread())) {
			if (nonblock != 0) {
				result = SceKernelErrors.ERROR_NET_ADHOC_NO_DATA_AVAILABLE;
			} else {
				BlockedPtpAction blockedPtpAction = new BlockedPtpConnect(this, timeout);
				blockedPtpAction.blockCurrentThread();
			}
		}

		return result;
	}

	@Override
	public void send(AdhocMessage adhocMessage) throws IOException {
		adhocMessage.setFromMacAddress(getMacAddress().macAddress);
		adhocMessage.setToMacAddress(getDestMacAddress().macAddress);
		send(adhocMessage, getDestPort());
	}

	public int send(int data, TPointer32 dataSizeAddr, int timeout, int nonblock) {
		int result = 0;

		try {
			AdhocMessage adhocMessage = networkAdapter.createAdhocPtpMessage(data, dataSizeAddr.getValue());
			send(adhocMessage);
		} catch (IOException e) {
			result = SceKernelErrors.ERROR_NET_ADHOC_DISCONNECTED;
			log.error("send returning ERROR_NET_ADHOC_DISCONNECTED", e);
		}

		return result;
	}

	// For Ptp sockets, data in read as a byte stream. Data is not organized in packets.
	// Read as much data as the provided buffer can contain.
	public int recv(TPointer data, TPointer32 dataLengthAddr, int timeout, int nonblock) {
		int result = 0;

		try {
			SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
			if (pollRecv(data, dataLengthAddr, thread)) {
				// Recv completed immediately
				result = thread.cpuContext._v0;
			} else if (nonblock != 0) {
				// Recv cannot be completed in non-blocking mode
				result = SceKernelErrors.ERROR_NET_ADHOC_NO_DATA_AVAILABLE;
			} else {
				// Block current thread
				BlockedPdpAction blockedPdpAction = new BlockedPtpRecv(this, data, dataLengthAddr, timeout);
				blockedPdpAction.blockCurrentThread();
			}
		} catch (IOException e) {
			result = SceKernelErrors.ERROR_NET_ADHOC_DISCONNECTED;
			log.error("recv", e);
		}

		return result;
	}

	protected boolean pollRecv(TPointer data, TPointer32 dataLengthAddr, SceKernelThreadInfo thread) throws IOException {
		int length = dataLengthAddr.getValue();
		boolean completed = false;

		if (length > 0) {
			if (getRcvdData() <= 0 || receivedMessage != null) {
				update();
			}

			if (getRcvdData() > 0) {
				if (length > getRcvdData()) {
					length = getRcvdData();
				}
				// Copy the data already received
				dataLengthAddr.setValue(length);
				Memory mem = Memory.getInstance();
				mem.memcpy(data.getAddress(), buffer.addr, length);
				if (getRcvdData() > length) {
					// Shift the remaining buffer data to the beginning of the buffer
					mem.memmove(buffer.addr, buffer.addr + length, getRcvdData() - length);
				}
				rcvdData -= length;

				if (log.isDebugEnabled()) {
					log.debug(String.format("Returned received data: %d bytes", length));
					if (log.isTraceEnabled()) {
						log.trace(String.format("Returned data: %s", Utilities.getMemoryDump(data.getAddress(), length)));
					}
				}
				setReturnValue(thread, 0);
				completed = true;
			}
		}

		return completed;
	}

	// For Ptp sockets, data is stored in the internal buffer as a continuous byte stream.
	// The organization in packets doesn't matter.
	private int addReceivedMessage(AdhocMessage adhocMessage, int offset) {
		int length = Math.min(adhocMessage.getDataLength() - offset, getBufSize() - getRcvdData());
		int addr = buffer.addr + getRcvdData();
		adhocMessage.writeDataToMemory(addr, offset, length);
		rcvdData += length;
		if (log.isDebugEnabled()) {
			if (offset == 0) {
				log.debug(String.format("Successfully received message (length=%d, rcvdData=%d) %s", length, getRcvdData(), adhocMessage));
			} else {
				log.debug(String.format("Appending received message (offset=%d, length=%d, rcvdData=%d) %s", offset, length, getRcvdData(), adhocMessage));
			}
			if (log.isTraceEnabled()) {
				log.trace(String.format("Message data: %s", Utilities.getMemoryDump(addr, length)));
			}
		}

		return length;
	}

	@Override
	public void update() throws IOException {
		// Receive all messages available
		while (getRcvdData() < getBufSize()) {
			if (receivedMessage != null) {
				receivedMessageOffset += addReceivedMessage(receivedMessage, receivedMessageOffset);
				if (receivedMessageOffset >= receivedMessage.getDataLength()) {
					receivedMessage = null;
					receivedMessageOffset = 0;
				}
			} else {
				try {
					openSocket();
					socket.setTimeout(1);
					byte[] bytes = new byte[0x10000]; // 64K buffer
					int length = socket.receive(bytes, bytes.length);
					int receivedPort = socket.getReceivedPort();
					InetAddress receivedAddress = socket.getReceivedAddress();
					AdhocMessage adhocMessage = createAdhocMessage(bytes, length);
					if (isForMe(adhocMessage, receivedPort, receivedAddress)) {
						receivedMessage = adhocMessage;
						receivedMessageOffset = 0;
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
				}
			}
		}
	}

	@Override
	protected AdhocMessage createAdhocMessage(byte[] message, int length) {
		return networkAdapter.createAdhocPtpMessage(message, length);
	}

	protected abstract boolean pollAccept(int peerMacAddr, int peerPortAddr, SceKernelThreadInfo thread);
	protected abstract boolean pollConnect(SceKernelThreadInfo thread);
	public abstract boolean canAccept();
	public abstract boolean canConnect();

	@Override
	public String toString() {
		return String.format("PtpObject[id=%d, srcMacAddress=%s, srcPort=%d, destMacAddress=%s, destPort=%d, bufSize=%d, retryDelay=%d, retryCount=%d, queue=%d, rcvdData=%d]", getId(), getMacAddress(), getPort(), getDestMacAddress(), getDestPort(), getBufSize(), getRetryDelay(), getRetryCount(), getQueue(), getRcvdData());
	}
}
