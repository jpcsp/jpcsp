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

import static jpcsp.network.adhoc.AdhocMessage.MAX_HEADER_SIZE;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
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
    // Polling period (micro seconds) for blocking operations
    protected static final int BLOCKED_OPERATION_POLLING_MICROS = 10000;

	protected static abstract class BlockedPdpAction implements IAction {
		protected final PdpObject pdpObject;
    	protected final long timeoutMicros;
    	protected final int threadUid;
    	protected final SceKernelThreadInfo thread;

    	public BlockedPdpAction(PdpObject pdpObject, long timeout) {
    		this.pdpObject = pdpObject;
    		timeoutMicros = Emulator.getClock().microTime() + timeout;
    		threadUid = Modules.ThreadManForUserModule.getCurrentThreadID();
			thread = Modules.ThreadManForUserModule.getThreadById(threadUid);

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("BlockedPdpAction for thread %s", thread));
    		}
    	}

    	public void blockCurrentThread() {
			long schedule = Emulator.getClock().microTime() + BLOCKED_OPERATION_POLLING_MICROS;
			Emulator.getScheduler().addAction(schedule, this);
			Modules.ThreadManForUserModule.hleBlockCurrentThread(SceKernelThreadInfo.JPCSP_WAIT_NET);
    	}

    	@Override
		public void execute() {
			if (log.isDebugEnabled()) {
				log.debug(String.format("BlockedPdpAction: poll on %s, thread %s", pdpObject, thread));
			}

			try {
				if (poll()) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("BlockedPdpAction: unblocking thread %s", thread));
					}
					Modules.ThreadManForUserModule.hleUnblockThread(threadUid);
				} else {
					long now = Emulator.getClock().microTime();
					if (now >= timeoutMicros) {
						if (log.isDebugEnabled()) {
							log.debug(String.format("BlockedPdpAction: timeout for thread %s", thread));
						}
						// Unblock thread and return timeout error
						setReturnValue(thread, SceKernelErrors.ERROR_NET_ADHOC_TIMEOUT);
						Modules.ThreadManForUserModule.hleUnblockThread(threadUid);
					} else {
						if (log.isDebugEnabled()) {
							log.debug(String.format("BlockedPdpAction: continue polling"));
						}
						long schedule = now + BLOCKED_OPERATION_POLLING_MICROS;
						Emulator.getScheduler().addAction(schedule, this);
					}
				}
			} catch (IOException e) {
				setReturnValue(thread, getExceptionResult(e));
				log.error(getClass().getSimpleName(), e);
			}
		}

    	protected abstract boolean poll() throws IOException;
    	protected abstract int getExceptionResult(IOException e);
	}

	protected static class BlockedPdpRecv extends BlockedPdpAction {
		final protected TPointer srcMacAddr;
		final protected TPointer16 portAddr;
		final protected TPointer data;
		final protected TPointer32 dataLengthAddr;

		public BlockedPdpRecv(PdpObject pdpObject, TPointer srcMacAddr, TPointer16 portAddr, TPointer data, TPointer32 dataLengthAddr, long timeout) {
			super(pdpObject, timeout);
			this.srcMacAddr = srcMacAddr;
			this.portAddr = portAddr;
			this.data = data;
			this.dataLengthAddr = dataLengthAddr;
		}

		@Override
		protected boolean poll() throws IOException {
			return pdpObject.pollRecv(srcMacAddr, portAddr, data, dataLengthAddr, thread);
		}

		@Override
		protected int getExceptionResult(IOException e) {
			return SceKernelErrors.ERROR_NET_ADHOC_TIMEOUT;
		}
	}

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
			result = SceKernelErrors.ERROR_NET_ADHOC_INVALID_ADDR;
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
	private void addReceivedMessage(AdhocMessage adhocMessage, int port) {
		AdhocBufferMessage bufferMessage = new AdhocBufferMessage();
		bufferMessage.length = adhocMessage.getDataLength();
		bufferMessage.macAddress.setMacAddress(adhocMessage.getFromMacAddress());
		bufferMessage.port = Modules.sceNetAdhocModule.getClientPortFromRealPort(adhocMessage.getFromMacAddress(), port);
		bufferMessage.offset = rcvdData;
		adhocMessage.writeDataToMemory(buffer.addr + bufferMessage.offset);

		// Update the timestamp of the peer
		Modules.sceNetAdhocctlModule.hleNetAdhocctlPeerUpdateTimestamp(adhocMessage.getFromMacAddress());

		if (log.isDebugEnabled()) {
			log.debug(String.format("Successfully received %d bytes from %s on port %d(%d)", bufferMessage.length, bufferMessage.macAddress, bufferMessage.port, port));
			if (log.isTraceEnabled()) {
				log.trace(String.format("Message data: %s", Utilities.getMemoryDump(buffer.addr + bufferMessage.offset, bufferMessage.length)));
			}
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
	public int recv(TPointer srcMacAddr, TPointer16 portAddr, TPointer data, TPointer32 dataLengthAddr, int timeout, int nonblock) {
		int result = 0;

		try {
			SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
			if (pollRecv(srcMacAddr, portAddr, data, dataLengthAddr, thread)) {
				// Recv completed immediately
				result = thread.cpuContext._v0;
			} else if (nonblock != 0) {
				// Recv cannot be completed in non-blocking mode
				result = SceKernelErrors.ERROR_NET_ADHOC_NO_DATA_AVAILABLE;
			} else {
				// Block current thread
				BlockedPdpAction blockedPdpAction = new BlockedPdpRecv(this, srcMacAddr, portAddr, data, dataLengthAddr, timeout);
				blockedPdpAction.blockCurrentThread();
			}
		} catch (IOException e) {
			result = SceKernelErrors.ERROR_NET_ADHOC_DISCONNECTED;
			log.error("recv", e);
		}

		return result;
	}

	public boolean pollRecv(TPointer srcMacAddr, TPointer16 portAddr, TPointer data, TPointer32 dataLengthAddr, SceKernelThreadInfo thread) throws IOException {
		int length = dataLengthAddr.getValue();
		boolean completed = false;

		if (rcvdMessages.isEmpty()) {
			update();
		}

		if (!rcvdMessages.isEmpty()) {
			AdhocBufferMessage bufferMessage = rcvdMessages.getFirst();
			if (length < bufferMessage.length) {
				// Buffer is too small to contain all the available data.
				// Return the buffer size that would be required.
				dataLengthAddr.setValue(bufferMessage.length);
				setReturnValue(thread, SceKernelErrors.ERROR_NET_BUFFER_TOO_SMALL);
			} else {
				// Copy the data already received
				dataLengthAddr.setValue(bufferMessage.length);
				Memory.getInstance().memcpy(data.getAddress(), buffer.addr + bufferMessage.offset, bufferMessage.length);
				if (srcMacAddr != null && !srcMacAddr.isNull()) {
					bufferMessage.macAddress.write(Memory.getInstance(), srcMacAddr.getAddress());
				}
				if (portAddr != null && portAddr.isNotNull()) {
					portAddr.setValue(bufferMessage.port);
				}

				removeFirstReceivedMessage();

				if (log.isDebugEnabled()) {
					log.debug(String.format("Returned received data: %d bytes from %s on port %d", dataLengthAddr.getValue(), bufferMessage.macAddress, portAddr.getValue()));
					if (log.isTraceEnabled()) {
						log.trace(String.format("Returned data: %s", Utilities.getMemoryDump(data.getAddress(), dataLengthAddr.getValue())));
					}
				}
				setReturnValue(thread, 0);
			}
			completed = true;
		}

		return completed;
	}

	public void update() throws IOException {
		// Receive all messages available
		while (rcvdData < getBufSize()) {
			try {
				openSocket();
				socket.setTimeout(1);
				byte[] bytes = new byte[getBufSize() - rcvdData + MAX_HEADER_SIZE];
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
			}
		}
	}

	protected AdhocMessage createAdhocMessage(byte[] message, int length) {
		return networkAdapter.createAdhocPdpMessage(message, length);
	}

	protected boolean isForMe(AdhocMessage adhocMessage, int port, InetAddress address) {
		return adhocMessage.isForMe();
	}

	protected static void setReturnValue(SceKernelThreadInfo thread, int value) {
    	thread.cpuContext._v0 = value;
    }

	@Override
	public String toString() {
		return String.format("PdpObject[id=%d, macAddress=%s, port=%d, bufSize=%d, rcvdData=%d]", getId(), macAddress, getPort(), getBufSize(), rcvdData);
	}
}
