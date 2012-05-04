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
package jpcsp.HLE.modules150;

import static jpcsp.HLE.modules150.sceNet.convertMacAddressToString;
import static jpcsp.HLE.modules150.sceNetAdhoc.AdhocPtpMessage.PTP_MESSAGE_TYPE_CONNECT_CONFIRM;
import static jpcsp.HLE.modules150.sceNetAdhoc.AdhocPtpMessage.PTP_MESSAGE_TYPE_DATA;
import static jpcsp.HLE.modules150.sceNetAdhoc.AdhocPtpMessage.PTP_MESSAGE_TYPE_CONNECT;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;
import jpcsp.HLE.TPointer32;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;
import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructureVariableLength;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.hardware.Wlan;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.scheduler.Scheduler;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceNetAdhoc extends HLEModule {
    protected static Logger log = Modules.getLogger("sceNetAdhoc");

    // For test purpose when running 2 different Jpcsp instances on the same computer:
    // one computer has to have netClientPortShift=0 and netServerPortShift=100,
    // the other computer, netClientPortShift=100 and netServerPortShift=0.
    private int netClientPortShift = 0;
    private int netServerPortShift = 0;

    // Polling period (micro seconds) for blocking operations
    protected static final int BLOCKED_OPERATION_POLLING_MICROS = 10000;
    // Period to update the Game Mode
    protected static final int GAME_MODE_UPDATE_MICROS = 12000;

    protected static final int PSP_ADHOC_POLL_READY_TO_SEND = 1;
    protected static final int PSP_ADHOC_POLL_DATA_AVAILABLE = 2;
    protected static final int PSP_ADHOC_POLL_CAN_CONNECT = 4;
    protected static final int PSP_ADHOC_POLL_CAN_ACCEPT = 8;

    protected HashMap<Integer, PdpObject> pdpObjects;
    protected HashMap<Integer, PtpObject> ptpObjects;
    private int currentFreePort;
	public static final byte[] ANY_MAC_ADDRESS = new byte[] {
		(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
	};
	private static final String uidPurpose = "sceNetAdhoc";
	private GameModeScheduledAction gameModeScheduledAction;
	protected GameModeArea masterGameModeArea;
	protected LinkedList<GameModeArea> replicaGameModeAreas;
	private static final String replicaIdPurpose = "sceNetAdhoc-Replica";
    private static final int adhocGameModePort = 31000;
    private DatagramSocket gameModeSocket;

    /**
     * An AdhocMessage is consisting of:
     * - 6 bytes for the MAC address of the message sender
     * - 6 bytes for the MAC address of the message recipient
     * - n bytes for the message data
     */
    public static abstract class AdhocMessage {
    	protected byte[] fromMacAddress = new byte[Wlan.MAC_ADDRESS_LENGTH];
    	protected byte[] toMacAddress = new byte[Wlan.MAC_ADDRESS_LENGTH];
    	protected byte[] data = new byte[0];
    	protected byte[] additionalHeaderData;
    	protected static final int HEADER_SIZE = Wlan.MAC_ADDRESS_LENGTH + Wlan.MAC_ADDRESS_LENGTH;
    	protected int additionalHeaderLength;

    	public AdhocMessage() {
    		init(0, 0, ANY_MAC_ADDRESS);
    	}

    	public AdhocMessage(byte[] fromMacAddress, byte[] toMacAddress) {
    		init(0, 0, toMacAddress);
    	}

    	public AdhocMessage(byte[] message, int length) {
    		additionalHeaderLength = getAdditionalHeaderLength();
    		if (length >= HEADER_SIZE + additionalHeaderLength) {
    			System.arraycopy(message, 0, fromMacAddress, 0, fromMacAddress.length);
    			System.arraycopy(message, fromMacAddress.length, toMacAddress, 0, toMacAddress.length);
    			additionalHeaderData = new byte[additionalHeaderLength];
    			System.arraycopy(message, HEADER_SIZE, additionalHeaderData, 0, additionalHeaderData.length);
    			data = new byte[length - getHeaderSize()];
    			System.arraycopy(message, getHeaderSize(), data, 0, data.length);
    		}
    	}

    	public AdhocMessage(int address, int length) {
    		init(address, length, ANY_MAC_ADDRESS);
    	}

    	public AdhocMessage(int address, int length, byte[] toMacAddress) {
    		init(address, length, toMacAddress);
    	}

    	protected int getHeaderSize() {
    		return HEADER_SIZE + additionalHeaderLength;
    	}

    	protected void setAdditionalHeaderDataByte(int b) {
    		if (additionalHeaderLength >= 1) {
    			additionalHeaderData[0] = (byte) b;
    		}
    	}

    	protected int getAdditionalHeaderDataByte() {
    		if (additionalHeaderLength < 1) {
    			return 0;
    		}

    		return additionalHeaderData[0] & 0xFF;
    	}

    	private void init(int address, int length, byte[] toMacAddress) {
    		init(address, length, Wlan.getMacAddress(), toMacAddress);
    	}

    	private void init(int address, int length, byte[] fromMacAddress, byte[] toMacAddress) {
    		additionalHeaderLength = getAdditionalHeaderLength();
    		setFromMacAddress(fromMacAddress);
    		setToMacAddress(toMacAddress);
    		additionalHeaderData = new byte[additionalHeaderLength];
    		data = new byte[length];
    		if (length > 0 && address != 0) {
	    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
	    		for (int i = 0; i < length; i++) {
	    			data[i] = (byte) memoryReader.readNext();
	    		}
    		}
    	}

    	public void setData(byte[] data) {
    		this.data = new byte[data.length];
    		System.arraycopy(data, 0, this.data, 0, data.length);
    	}

    	public void setDataInt32(int value) {
    		data = new byte[4];
    		for (int i = 0; i < 4; i++) {
    			data[i] = (byte) (value >> (i * 8));
    		}
    	}

    	public int getDataInt32() {
    		int value = 0;

    		for (int i = 0; i < 4 && i < data.length; i++) {
    			value |= (data[i] & 0xFF) << (i * 8);
    		}

    		return value;
    	}

    	public byte[] getMessage() {
    		byte[] message = new byte[getMessageLength()];
    		System.arraycopy(fromMacAddress, 0, message, 0, fromMacAddress.length);
    		System.arraycopy(toMacAddress, 0, message, fromMacAddress.length, toMacAddress.length);
    		System.arraycopy(additionalHeaderData, 0, message, HEADER_SIZE, additionalHeaderData.length);
    		System.arraycopy(data, 0, message, getHeaderSize(), data.length);

    		return message;
    	}

    	public int getMessageLength() {
    		return getMessageLength(data.length + additionalHeaderLength);
    	}

    	public static int getMessageLength(int dataLength) {
    		return HEADER_SIZE + dataLength;
    	}

    	public void writeDataToMemory(int address) {
    		writeBytes(address, getDataLength(), data, 0);
    	}

    	public void writeDataToMemory(int address, int maxLength) {
    		writeBytes(address, Math.min(getDataLength(), maxLength), data, 0);
    	}

    	public byte[] getData() {
    		return data;
    	}

    	public int getDataLength() {
    		return data.length;
    	}

    	public byte[] getFromMacAddress() {
    		return fromMacAddress;
    	}

    	public byte[] getToMacAddress() {
    		return toMacAddress;
    	}

    	public void setFromMacAddress(byte[] fromMacAddress) {
    		System.arraycopy(fromMacAddress, 0, this.fromMacAddress, 0, this.fromMacAddress.length);
    	}

    	public void setToMacAddress(byte[] toMacAddress) {
    		System.arraycopy(toMacAddress, 0, this.toMacAddress, 0, this.toMacAddress.length);
    	}

    	private static boolean isAnyMacAddress(byte[] macAddress) {
    		return isSameMacAddress(macAddress, ANY_MAC_ADDRESS);
    	}

    	public byte[] getAdditionalHeaderData() {
    		return additionalHeaderData;
    	}

    	protected abstract int getAdditionalHeaderLength();

    	public boolean isForMe() {
    		return isAnyMacAddress(toMacAddress) || isSameMacAddress(toMacAddress, Wlan.getMacAddress());
    	}

		@Override
		public String toString() {
			return String.format("AdhocMessage[fromMacAddress=%s, toMacAddress=%s, dataLength=%d]", convertMacAddressToString(fromMacAddress), convertMacAddressToString(toMacAddress), getDataLength());
		}
    }

    protected static class AdhocPdpMessage extends AdhocMessage {
    	private static final int ADDITIONAL_HEADER_LENGTH = 0;

		public AdhocPdpMessage(byte[] message, int length) {
			super(message, length);
		}

		public AdhocPdpMessage(int address, int length, byte[] toMacAddress) {
			super(address, length, toMacAddress);
		}

		@Override
		protected int getAdditionalHeaderLength() {
			return ADDITIONAL_HEADER_LENGTH;
		}

		@Override
		public String toString() {
			return String.format("AdhocPdpMessage[fromMacAddress=%s, toMacAddress=%s, dataLength=%d]", convertMacAddressToString(fromMacAddress), convertMacAddressToString(toMacAddress), getDataLength());
		}
    }

    protected static class AdhocPtpMessage extends AdhocMessage {
    	private static final int ADDITIONAL_HEADER_LENGTH = 1;
    	protected static final int PTP_MESSAGE_TYPE_CONNECT = 1;
    	protected static final int PTP_MESSAGE_TYPE_CONNECT_CONFIRM = 2;
    	protected static final int PTP_MESSAGE_TYPE_DATA = 3;

		public AdhocPtpMessage(byte[] message, int length) {
			super(message, length);
		}

    	public AdhocPtpMessage(int type) {
    		super();
    		setAdditionalHeaderDataByte(type);
    	}

    	public AdhocPtpMessage(int address, int length, int type) {
    		super(address, length);
    		setAdditionalHeaderDataByte(type);
    	}

    	public int getType() {
			return getAdditionalHeaderDataByte();
		}

		@Override
		protected int getAdditionalHeaderLength() {
			return ADDITIONAL_HEADER_LENGTH;
		}

		@Override
		public String toString() {
			return String.format("AdhocPtpMessage[fromMacAddress=%s, toMacAddress=%s, dataLength=%d, type=%d]", convertMacAddressToString(fromMacAddress), convertMacAddressToString(toMacAddress), getDataLength(), getType());
		}
    }

    protected static abstract class AdhocObject {
    	/** uid */
    	private final int id;
    	private int port;
    	protected DatagramSocket socket;
    	/** Buffer size */
    	private int bufSize;
    	protected SysMemInfo buffer;

    	public AdhocObject() {
    		id = SceUidManager.getNewUid(uidPurpose);
    	}

    	public AdhocObject(AdhocObject adhocObject) {
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

    	protected void openSocket() throws SocketException {
			if (socket == null) {
				if (port == 0) {
					socket = new DatagramSocket();
					if (log.isDebugEnabled()) {
						log.debug(String.format("Opening socket on free local port %d", socket.getLocalPort()));
					}
					setPort(socket.getLocalPort());
				} else {
					int realPort = Modules.sceNetAdhocModule.getRealPortFromServerPort(port);
					if (log.isDebugEnabled()) {
						log.debug(String.format("Opening socket on port %d(%d)", port, realPort));
					}
					socket = new DatagramSocket(realPort);
				}
				socket.setBroadcast(true);
				socket.setSoTimeout(1);
			}
		}

		protected void closeSocket() {
			if (socket != null) {
				socket.close();
				socket = null;
			}
		}

		protected void setTimeout(int timeout, int nonblock) throws SocketException {
			if (nonblock != 0) {
				socket.setSoTimeout(1);
			} else {
				// SoTimeout accepts milliseconds, PSP timeout is given in microseconds
				socket.setSoTimeout(Math.max(timeout / 1000, 1));
			}
		}

		protected void send(AdhocMessage adhocMessage) throws IOException {
			send(adhocMessage, getPort());
		}

		protected void send(AdhocMessage adhocMessage, int destPort) throws IOException {
			openSocket();

			int realPort = Modules.sceNetAdhocModule.getRealPortFromClientPort(adhocMessage.getToMacAddress(), destPort);
			SocketAddress socketAddress = Modules.sceNetAdhocModule.getSocketAddress(adhocMessage.getToMacAddress(), realPort);
			DatagramPacket packet = new DatagramPacket(adhocMessage.getMessage(), adhocMessage.getMessageLength(), socketAddress);
			socket.send(packet);

			if (log.isDebugEnabled()) {
				log.debug(String.format("Successfully sent %d bytes to port %d(%d): %s", adhocMessage.getDataLength(), destPort, realPort, adhocMessage));
			}
		}
    }

    protected static class AdhocBufferMessage {
    	public int length;
    	public int offset;
    	public pspNetMacAddress macAddress = new pspNetMacAddress();
    	public int port;
    }

    protected class PdpObject extends AdhocObject {
    	/** MAC address */
    	private pspNetMacAddress macAddress;
    	/** Bytes received */
    	protected int rcvdData;
    	private LinkedList<AdhocBufferMessage> rcvdMessages = new LinkedList<AdhocBufferMessage>();

    	public PdpObject() {
    		super();
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
			}

			return result;
		}

		public int send(pspNetMacAddress destMacAddress, int destPort, TPointer data, int length, int timeout, int nonblock) {
			int result = 0;

			try {
				openSocket();
				setTimeout(timeout, nonblock);
				AdhocMessage adhocMessage = new AdhocPdpMessage(data.getAddress(), length, destMacAddress.macAddress);
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
			bufferMessage.port = getClientPortFromRealPort(adhocMessage.getFromMacAddress(), port);
			bufferMessage.offset = rcvdData;
			adhocMessage.writeDataToMemory(buffer.addr + bufferMessage.offset);

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

		// For Pdp sockets, data in read one packets at a time.
		// The called has to provide enough space to fully read the next packet.
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
					socket.setSoTimeout(1);
					byte[] bytes = new byte[AdhocMessage.getMessageLength(getBufSize() - rcvdData)];
					DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
					socket.receive(packet);
					AdhocMessage adhocMessage = createMessage(packet);
					if (isForMe(adhocMessage, packet.getPort())) {
						if (getRcvdData() + adhocMessage.getDataLength() <= getBufSize()) {
							addReceivedMessage(adhocMessage, packet.getPort());
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

		protected AdhocMessage createMessage(DatagramPacket packet) {
			return new AdhocPdpMessage(packet.getData(), packet.getLength());
		}

		protected boolean isForMe(AdhocMessage adhocMessage, int port) {
			return adhocMessage.isForMe();
		}

		@Override
		public String toString() {
			return String.format("PdpObject[id=%d, macAddress=%s, port=%d, bufSize=%d, rcvdData=%d]", getId(), macAddress, getPort(), getBufSize(), rcvdData);
		}
    }

    protected class PtpObject extends PdpObject {
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
    	private AdhocPtpMessage connectRequest;
    	private int connectRequestPort;
    	private AdhocPtpMessage connectConfirm;
    	private boolean connected;

    	public PtpObject() {
    		super();
    	}

    	public PtpObject(PtpObject ptpObject) {
    		super(ptpObject);
    		destMacAddress = ptpObject.destMacAddress;
    		destPort = ptpObject.destPort;
    		retryDelay = ptpObject.retryDelay;
    		retryCount = ptpObject.retryCount;
    		queue = ptpObject.queue;
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

		public boolean isConnectRequestReceived() {
			return connectRequest != null;
		}

		public boolean isConnectConfirmReceived() {
			return connectConfirm != null;
		}

		public int open() {
			int result = 0;

			try {
				openSocket();
			} catch (SocketException e) {
				log.error("open", e);
			}

			return result;
		}

		public int connect(int timeout, int nonblock) {
			int result = 0;

			try {
				AdhocPtpMessage adhocPtpMessage = new AdhocPtpMessage(PTP_MESSAGE_TYPE_CONNECT);
				send(adhocPtpMessage);

				if (!pollConnect(Modules.ThreadManForUserModule.getCurrentThread())) {
					if (nonblock != 0) {
						result = SceKernelErrors.ERROR_NET_ADHOC_NO_DATA_AVAILABLE;
					} else {
						BlockedPtpAction blockedPtpAction = new BlockedPtpConnect(this, timeout);
						blockedPtpAction.blockCurrentThread();
					}
				}
			} catch (SocketException e) {
				log.error("connect", e);
			} catch (IOException e) {
				log.error("connect", e);
			}

			return result;
		}

		public int listen() {
			int result = 0;

			try {
				openSocket();
			} catch (SocketException e) {
				log.error("listen", e);
			}

			return result;
		}

		public int accept(int peerMacAddr, int peerPortAddr, int timeout, int nonblock) {
			int result = 0;

			SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
			if (pollAccept(peerMacAddr, peerPortAddr, thread)) {
				// Accept completed immediately
				result = thread.cpuContext.gpr[Common._v0];
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

		public boolean pollAccept(int peerMacAddr, int peerPortAddr, SceKernelThreadInfo thread) {
			boolean acceptCompleted = false;
			Memory mem = Memory.getInstance();

			try {
				// Process a previously received connect message, if available
				AdhocPtpMessage adhocPtpMessage = connectRequest;
				int adhocPtpMessagePort = connectRequestPort;
				if (adhocPtpMessage == null) {
					byte[] bytes = new byte[getBufSize()];
					DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
					socket.receive(packet);
					adhocPtpMessage = new AdhocPtpMessage(packet.getData(), packet.getLength());
					adhocPtpMessagePort = packet.getPort();

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
						case AdhocPtpMessage.PTP_MESSAGE_TYPE_CONNECT:
							pspNetMacAddress peerMacAddress = new pspNetMacAddress();
							peerMacAddress.setMacAddress(adhocPtpMessage.getFromMacAddress());
							int peerPort = getClientPortFromRealPort(adhocPtpMessage.getFromMacAddress(), adhocPtpMessagePort);

							if (peerMacAddr != 0) {
								peerMacAddress.write(mem, peerMacAddr);
							}
							if (peerPortAddr != 0) {
								mem.write16(peerPortAddr, (short) peerPort);
							}

							// As a result of the "accept" call, create a new PTP Object
							PtpObject ptpObject = new PtpObject(this);
							ptpObject.setDestMacAddress(peerMacAddress);
							ptpObject.setDestPort(peerPort);
							ptpObject.setPort(0);
							ptpObjects.put(ptpObject.getId(), ptpObject);

							// Return the ID of the new PTP Object
							setReturnValue(thread, ptpObject.getId());

							// Get a new free port
							ptpObject.setPort(0);
							ptpObject.openSocket();

							// Send a connect confirmation message including the new port
							AdhocPtpMessage confirmMessage = new AdhocPtpMessage(PTP_MESSAGE_TYPE_CONNECT_CONFIRM);
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

		public boolean pollConnect(SceKernelThreadInfo thread) {
			boolean connectCompleted = false;

			try {
				// Process a previously received confirm message, if available
				AdhocPtpMessage adhocPtpMessage = connectConfirm;
				if (adhocPtpMessage == null) {
					byte[] bytes = new byte[getBufSize()];
					DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
					socket.receive(packet);
					adhocPtpMessage = new AdhocPtpMessage(packet.getData(), packet.getLength());
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
							int port = getClientPortFromRealPort(adhocPtpMessage.getFromMacAddress(), adhocPtpMessage.getDataInt32());
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
				AdhocPtpMessage adhocPtpMessage = new AdhocPtpMessage(PTP_MESSAGE_TYPE_CONNECT);
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

		protected void send(AdhocPtpMessage adhocPtpMessage) throws IOException {
			adhocPtpMessage.setFromMacAddress(getMacAddress().macAddress);
			adhocPtpMessage.setToMacAddress(getDestMacAddress().macAddress);
			send(adhocPtpMessage, getDestPort());
		}

		public int send(int data, TPointer32 dataSizeAddr, int timeout, int nonblock) {
			int result = 0;

			try {
				AdhocPtpMessage adhocPtpMessage = new AdhocPtpMessage(data, dataSizeAddr.getValue(), PTP_MESSAGE_TYPE_DATA);
				send(adhocPtpMessage);
			} catch (IOException e) {
				log.error("send", e);
			}

			return result;
		}

		// For Ptp sockets, data in read as a byte stream. Data is not organized in packets.
		// Read as much data as the provided buffer can contain.
		public int recv(TPointer data, TPointer32 dataLengthAddr, int timeout, int nonblock) {
			int result = SceKernelErrors.ERROR_NET_ADHOC_NO_DATA_AVAILABLE;
			int length = dataLengthAddr.getValue();

			if (length > 0) {
				if (getRcvdData() <= 0) {
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
						log.debug(String.format("Returned received data: %d bytes: %s", length, Utilities.getMemoryDump(data.getAddress(), length, 4, 16)));
					}
					result = 0;
				}
			}

			return result;
		}

		@Override
		protected AdhocMessage createMessage(DatagramPacket packet) {
			return new AdhocPtpMessage(packet.getData(), packet.getLength());
		}

		@Override
		protected boolean isForMe(AdhocMessage adhocMessage, int port) {
			if (adhocMessage instanceof AdhocPtpMessage) {
				AdhocPtpMessage adhocPtpMessage = (AdhocPtpMessage) adhocMessage;
				int type = adhocPtpMessage.getType();
				if (type == AdhocPtpMessage.PTP_MESSAGE_TYPE_CONNECT_CONFIRM) {
					if (connected) {
						if (log.isDebugEnabled()) {
							log.debug(String.format("Received connect confirmation but already connected, discarding"));
						}
					} else {
						if (log.isDebugEnabled()) {
							log.debug(String.format("Received connect confirmation, processing later"));
						}
						connectConfirm = (AdhocPtpMessage) adhocMessage;
					}
					return false;
				} else if (type == AdhocPtpMessage.PTP_MESSAGE_TYPE_CONNECT) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Received connect request, processing later"));
					}
					connectRequest = (AdhocPtpMessage) adhocMessage;
					connectRequestPort = port;
					return false;
				} else if (type != AdhocPtpMessage.PTP_MESSAGE_TYPE_DATA) {
					return false;
				}
			}

			return super.isForMe(adhocMessage, port);
		}

		@Override
		public String toString() {
			return String.format("PtpObject[id=%d, srcMacAddress=%s, srcPort=%d, destMacAddress=%s, destPort=%d, bufSize=%d, retryDelay=%d, retryCount=%d, queue=%d, rcvdData=%d]", getId(), getMacAddress(), getPort(), getDestMacAddress(), getDestPort(), getBufSize(), getRetryDelay(), getRetryCount(), getQueue(), getRcvdData());
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
				log.debug(String.format("Successfully received message %s: %s", adhocMessage, Utilities.getMemoryDump(addr, length, 1, 16)));
			}
		}

		@Override
		protected void closeSocket() {
			super.closeSocket();
			connected = false;
		}
    }

    protected static abstract class BlockedPtpAction implements IAction {
    	protected final PtpObject ptpObject;
    	protected final long timeoutMicros;
    	protected final int threadUid;
    	protected final SceKernelThreadInfo thread;

    	protected BlockedPtpAction(PtpObject ptpObject, int timeout) {
    		this.ptpObject = ptpObject;
    		timeoutMicros = Emulator.getClock().microTime() + timeout;
    		threadUid = Modules.ThreadManForUserModule.getCurrentThreadID();
			thread = Modules.ThreadManForUserModule.getThreadById(threadUid);

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("BlockedPtdAccept for thread %s", thread));
    		}
    	}

    	public void blockCurrentThread() {
			long schedule = Emulator.getClock().microTime() + BLOCKED_OPERATION_POLLING_MICROS;
			Emulator.getScheduler().addAction(schedule, this);
			Modules.ThreadManForUserModule.hleBlockCurrentThread();
    	}

    	@Override
		public void execute() {
			if (log.isDebugEnabled()) {
				log.debug(String.format("BlockedPtpAction: poll on %s, thread %s", ptpObject, thread));
			}

			if (poll()) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("BlockedPtpAction: unblocking thread %s", thread));
				}
				Modules.ThreadManForUserModule.hleUnblockThread(threadUid);
			} else {
				long now = Emulator.getClock().microTime();
				if (now >= timeoutMicros) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("BlockedPtpAction: timeout for thread %s", thread));
					}
					// Unblock thread and return timeout error
					setReturnValue(thread, SceKernelErrors.ERROR_NET_ADHOC_TIMEOUT);
					Modules.ThreadManForUserModule.hleUnblockThread(threadUid);
				} else {
					if (log.isDebugEnabled()) {
						log.debug(String.format("BlockedPtpAction: continue polling"));
					}
					long schedule = now + BLOCKED_OPERATION_POLLING_MICROS;
					Emulator.getScheduler().addAction(schedule, this);
				}
			}
		}

		protected abstract boolean poll();
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
		protected boolean poll() {
			return ptpObject.pollAccept(peerMacAddr, peerPortAddr, thread);
		}
    }

    protected static class BlockedPtpConnect extends BlockedPtpAction {
    	public BlockedPtpConnect(PtpObject ptpObject, int timeout) {
    		super(ptpObject, timeout);
    	}

		@Override
		protected boolean poll() {
			return ptpObject.pollConnect(thread);
		}
    }

    protected static class GameModeScheduledAction implements IAction {
    	private final int scheduleRepeatMicros;
    	private long nextSchedule;

    	public GameModeScheduledAction(int scheduleRepeatMicros) {
    		this.scheduleRepeatMicros = scheduleRepeatMicros;
    	}

    	public void stop() {
    		Scheduler.getInstance().removeAction(nextSchedule, this);
    	}

    	public void start() {
    		Scheduler.getInstance().addAction(this);
    	}

    	@Override
		public void execute() {
    		Modules.sceNetAdhocModule.hleGameModeUpdate();

    		nextSchedule = Scheduler.getNow() + scheduleRepeatMicros;
    		Scheduler.getInstance().addAction(nextSchedule, this);
		}
    }

    protected static class GameModeArea {
    	public pspNetMacAddress macAddress;
    	public int addr;
    	public int size;
    	public int id;
    	private byte[] newData;
    	private long updateTimestamp;

    	public GameModeArea(int addr, int size) {
    		this.addr = addr;
    		this.size = size;
    		id = -1;
    	}

    	public GameModeArea(pspNetMacAddress macAddress, int addr, int size) {
    		this.macAddress = macAddress;
    		this.addr = addr;
    		this.size = size;
    		id = SceUidManager.getNewUid(replicaIdPurpose);
    	}

    	public void delete() {
    		if (id >= 0) {
    			SceUidManager.releaseUid(id, replicaIdPurpose);
    			id = -1;
    		}
    	}

    	public void setNewData(byte[] newData) {
    		updateTimestamp = Emulator.getClock().microTime();
    		this.newData = newData;
    	}

    	public void setNewData() {
    		byte[] data = new byte[size];
    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, size, 1);
    		for (int i = 0; i < data.length; i++) {
    			data[i] = (byte) memoryReader.readNext();
    		}

    		setNewData(data);
    	}

    	public void resetNewData() {
    		newData = null;
    	}

    	public byte[] getNewData() {
    		return newData;
    	}

    	public boolean hasNewData() {
    		return newData != null;
    	}

    	public void writeNewData() {
    		if (newData != null) {
    			writeBytes(addr, Math.min(size, newData.length), newData, 0);
    		}
    	}

    	public long getUpdateTimestamp() {
    		return updateTimestamp;
    	}

    	@Override
		public String toString() {
			if (macAddress == null) {
				return String.format("Master GameModeArea addr=0x%08X, size=%d", addr, size);
			}
			return String.format("Replica GameModeArea id=%d, macAddress=%s, addr=0x%08X, size=%d", id, macAddress, addr, size);
		}
    }

    protected static class AdhocGameModeMessage extends AdhocMessage {
    	private static final int ADDITIONAL_HEADER_LENGTH = 0;

    	public AdhocGameModeMessage(GameModeArea gameModeArea) {
    		super();
    		setData(gameModeArea.getNewData());
    		gameModeArea.resetNewData();
    		if (gameModeArea.macAddress != null) {
    			setToMacAddress(gameModeArea.macAddress.macAddress);
    		}
    	}

    	public AdhocGameModeMessage(byte[] message, int length) {
    		super(message, length);
    	}

    	@Override
		protected int getAdditionalHeaderLength() {
			return ADDITIONAL_HEADER_LENGTH;
		}
    }

	protected static String getPollEventName(int event) {
		return String.format("Unknown 0x%X", event);
	}

	protected static class pspAdhocPollId extends pspAbstractMemoryMappedStructure {
		public int id;
		public int events;
		public int revents;

		@Override
		protected void read() {
			id = read32();
			events = read32();
			revents = read32();
		}

		@Override
		protected void write() {
			write32(id);
			write32(events);
			write32(revents);
		}

		@Override
		public int sizeof() {
			return 12;
		}

		@Override
		public String toString() {
			return String.format("PollId[id=%d, events=0x%X(%s), revents=0x%X(%s)]", id, events, getPollEventName(events), revents, getPollEventName(revents));
		}
	}

	protected static class GameModeUpdateInfo extends pspAbstractMemoryMappedStructureVariableLength {
		public int updated;
		public long timeStamp;

		@Override
		protected void read() {
			super.read();
			updated = read32();
			timeStamp = read64();
		}

		@Override
		protected void write() {
			super.write();
			write32(updated);
			write64(timeStamp);
		}
	}

	@Override
    public String getName() {
        return "sceNetAdhoc";
    }

    @Override
	public void start() {
	    pdpObjects = new HashMap<Integer, sceNetAdhoc.PdpObject>();
	    ptpObjects = new HashMap<Integer, sceNetAdhoc.PtpObject>();
	    currentFreePort = 0x4000;
	    replicaGameModeAreas = new LinkedList<sceNetAdhoc.GameModeArea>();

	    super.start();
	}

    public void setNetClientPortShift(int netClientPortShift) {
    	this.netClientPortShift = netClientPortShift;
    	log.info(String.format("Using netClientPortShift=%d", netClientPortShift));
    }

    public void setNetServerPortShift(int netServerPortShift) {
    	this.netServerPortShift = netServerPortShift;
    	log.info(String.format("Using netServerPortShift=%d", netServerPortShift));
    }

    public int getClientPortFromRealPort(byte[] clientMacAddress, int realPort) {
    	if (isMyMacAddress(clientMacAddress)) {
    		// if the client is my-self, then this is actually a server port...
    		return getServerPortFromRealPort(realPort);
    	}

    	return realPort - netClientPortShift;
    }

    public int getRealPortFromClientPort(byte[] clientMacAddress, int clientPort) {
    	if (isMyMacAddress(clientMacAddress)) {
    		// if the client is my-self, then this is actually a server port...
    		return getRealPortFromServerPort(clientPort);
    	}

    	return clientPort + netClientPortShift;
    }

    public int getServerPortFromRealPort(int realPort) {
    	return realPort - netServerPortShift;
    }

    public int getRealPortFromServerPort(int serverPort) {
    	return serverPort + netServerPortShift;
    }

    public void hleExitGameMode() {
    	masterGameModeArea = null;
    	replicaGameModeAreas.clear();
    	stopGameMode();
    }

    public void hleGameModeUpdate() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleGameModeUpdate"));
    	}

		try {
			if (gameModeSocket == null) {
				gameModeSocket = new DatagramSocket(Modules.sceNetAdhocModule.getRealPortFromServerPort(adhocGameModePort));
	    		// For broadcast
				gameModeSocket.setBroadcast(true);
	    		// Non-blocking (timeout = 0 would mean blocking)
				gameModeSocket.setSoTimeout(1);
			}

			// Send master area
			if (masterGameModeArea != null && masterGameModeArea.hasNewData()) {
				try {
					AdhocGameModeMessage adhocGameModeMessage = new AdhocGameModeMessage(masterGameModeArea);
			    	SocketAddress socketAddress = Modules.sceNetAdhocModule.getSocketAddress(sceNetAdhoc.ANY_MAC_ADDRESS, Modules.sceNetAdhocModule.getRealPortFromClientPort(sceNetAdhoc.ANY_MAC_ADDRESS, adhocGameModePort));
			    	DatagramPacket packet = new DatagramPacket(adhocGameModeMessage.getMessage(), adhocGameModeMessage.getMessageLength(), socketAddress);
			    	gameModeSocket.send(packet);

			    	if (log.isDebugEnabled()) {
			    		log.debug(String.format("GameMode message sent to all: %s", adhocGameModeMessage));
			    	}
				} catch (SocketTimeoutException e) {
					// Ignore exception
				}
			}

			// Receive all waiting messages
			do {
				try {
					byte[] bytes = new byte[10000];
			    	DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
			    	gameModeSocket.receive(packet);
			    	AdhocGameModeMessage adhocGameModeMessage = new AdhocGameModeMessage(packet.getData(), packet.getLength());

			    	if (log.isDebugEnabled()) {
			    		log.debug(String.format("GameMode received: %s", adhocGameModeMessage));
			    	}

			    	for (GameModeArea gameModeArea : replicaGameModeAreas) {
			    		if (isSameMacAddress(gameModeArea.macAddress.macAddress, adhocGameModeMessage.fromMacAddress)) {
			    			if (log.isDebugEnabled()) {
			    				log.debug(String.format("Received new Data for GameMode Area %s", gameModeArea));
			    			}
			    			gameModeArea.setNewData(adhocGameModeMessage.getData());
			    			break;
			    		}
			    	}
				} catch (SocketTimeoutException e) {
					// No more messages available
					break;
				}
			} while (true);
		} catch (IOException e) {
			log.error("hleGameModeUpdate", e);
		}
    }

    protected void startGameMode() {
    	if (gameModeScheduledAction == null) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Starting GameMode"));
    		}
    		gameModeScheduledAction = new GameModeScheduledAction(GAME_MODE_UPDATE_MICROS);
    		gameModeScheduledAction.start();
    	}
    }

    protected void stopGameMode() {
    	if (gameModeScheduledAction != null) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Stopping GameMode"));
    		}
    		gameModeScheduledAction.stop();
    		gameModeScheduledAction = null;
    	}

    	if (gameModeSocket != null) {
    		gameModeSocket.close();
    		gameModeSocket = null;
    	}
    }

    private static void setReturnValue(SceKernelThreadInfo thread, int value) {
    	thread.cpuContext.gpr[Common._v0] = value;
    }

    public SocketAddress getSocketAddress(byte[] macAddress, int macPort) throws UnknownHostException {
		if (netClientPortShift > 0 || netServerPortShift > 0) {
			return new InetSocketAddress(InetAddress.getLocalHost(), macPort);
		}
		return sceNetInet.getBroadcastInetSocketAddress(macPort);
	}

	public static boolean isSameMacAddress(byte[] macAddress1, byte[] macAddress2) {
		if (macAddress1.length != macAddress2.length) {
			return false;
		}

		for (int i = 0; i < macAddress1.length; i++) {
			if (macAddress1[i] != macAddress2[i]) {
				return false;
			}
		}

		return true;
	}

	public static boolean isMyMacAddress(byte[] macAddress) {
		return isSameMacAddress(Wlan.getMacAddress(), macAddress);
	}

	private int getFreePort() {
    	int freePort = currentFreePort;
    	if (netClientPortShift > 0 || netServerPortShift > 0) {
    		currentFreePort += 2;
    	} else {
    		currentFreePort++;
    	}

		if (currentFreePort > 0x7FFF) {
			currentFreePort -= 0x4000;
		}

		return freePort;
    }

	public static void writeBytes(int address, int length, byte[] bytes, int offset) {
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, length, 1);
		for (int i = 0; i < length; i++) {
			memoryWriter.writeNext(bytes[i + offset] & 0xFF);
		}
		memoryWriter.flush();
	}

	public int checkPdpId(int pdpId) {
		if (!pdpObjects.containsKey(pdpId)) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Invalid Pdp Id=%d", pdpId));
			}
			throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOC_INVALID_SOCKET_ID);
		}

		return pdpId;
	}

	public int checkPtpId(int ptpId) {
		if (!ptpObjects.containsKey(ptpId)) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Invalid Ptp Id=%d", ptpId));
			}
			throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOC_INVALID_SOCKET_ID);
		}

		return ptpId;
	}

	/**
     * Initialise the adhoc library.
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xE1D621D7, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocInit() {
        log.warn("IGNORING: sceNetAdhocInit");

        log.info(String.format("Using MAC address=%s, nick name='%s'", sceNet.convertMacAddressToString(Wlan.getMacAddress()), sceUtility.getSystemParamNickname()));

        return 0;
    }

    /**
     * Terminate the adhoc library
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xA62C6F57, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocTerm() {
        log.warn("IGNORING: sceNetAdhocTerm");

        return 0;
    }

    @HLEFunction(nid = 0x7A662D6B, version = 150)
    public int sceNetAdhocPollSocket(TPointer socketsAddr, int count, int timeout, int nonblock) {
    	Memory mem = Memory.getInstance();
        log.warn(String.format("PARTIAL: sceNetAdhocPollSocket socketsAddr=%s, count=%d, timeout=%d, nonblock=%d", socketsAddr, count, timeout, nonblock));

        int countEvents = 0;
        for (int i = 0; i < count; i++) {
        	pspAdhocPollId pollId = new pspAdhocPollId();
        	pollId.read(mem, socketsAddr.getAddress() + i * pollId.sizeof());

        	PdpObject pdpObject = pdpObjects.get(pollId.id);
        	PtpObject ptpObject = null;
        	if (pdpObject == null) {
        		ptpObject = ptpObjects.get(pollId.id);
        		pdpObject = ptpObject;
        	}
        	if (pdpObject != null) {
        		pdpObject.update();
        	}

        	pollId.revents = 0;
        	if ((pollId.events & PSP_ADHOC_POLL_DATA_AVAILABLE) != 0 && pdpObject.getRcvdData() > 0) {
        		pollId.revents |= PSP_ADHOC_POLL_DATA_AVAILABLE;
        	}
        	if ((pollId.events & PSP_ADHOC_POLL_READY_TO_SEND) != 0) {
        		// Data can always be sent
        		pollId.revents |= PSP_ADHOC_POLL_READY_TO_SEND;
        	}
        	if ((pollId.events & PSP_ADHOC_POLL_CAN_CONNECT) != 0) {
    			if (ptpObject != null && ptpObject.isConnectConfirmReceived()) {
    				pollId.revents |= PSP_ADHOC_POLL_CAN_CONNECT;
        		}
        	}
        	if ((pollId.events & PSP_ADHOC_POLL_CAN_ACCEPT) != 0) {
    			if (ptpObject != null && ptpObject.isConnectRequestReceived()) {
    				pollId.revents |= PSP_ADHOC_POLL_CAN_ACCEPT;
        		}
        	}

        	if (pollId.revents != 0) {
        		countEvents++;
        	}

        	pollId.write(mem);

        	log.info(String.format("sceNetAdhocPollSocket pollId[%d]=%s", i, pollId));
        }

        return countEvents;
    }

    @HLEFunction(nid = 0x73BFD52D, version = 150)
    public int sceNetAdhocSetSocketAlert(int id, int unknown) {
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocSetSocketAlert id=%d, unknown=0x%X", id, unknown));

        return 0;
    }

    @HLEFunction(nid = 0x4D2CE199, version = 150)
    public void sceNetAdhocGetSocketAlert(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGetSocketAlert");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Create a PDP object.
     *
     * @param mac - Your MAC address (from sceWlanGetEtherAddr)
     * @param port - Port to use, lumines uses 0x309
     * @param bufsize - Socket buffer size, lumines sets to 0x400
     * @param unk1 - Unknown, lumines sets to 0
     *
     * @return The ID of the PDP object (< 0 on error)
     */
    @HLEFunction(nid = 0x6F92741B, version = 150)
    public int sceNetAdhocPdpCreate(int macAddr, int port, int bufSize, int unk1) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr);

    	log.warn(String.format("PARTIAL: sceNetAdhocPdpCreate macAddr=0x%08X(%s), port=%d, bufsize=%d, unk1=%d", macAddr, macAddress.toString(), port, bufSize, unk1));

		if (port == 0) {
			// Allocate a free port
			port = getFreePort();
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceNetAdhocPdpCreate: using free port %d", port));
			}
		}

		PdpObject pdpObject = new PdpObject();
    	int result = pdpObject.create(macAddress, port, bufSize);
    	if (result == pdpObject.getId()) {
    		pdpObjects.put(pdpObject.getId(), pdpObject);

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceNetAdhocPdpCreate: returning id=%d", result));
    		}
    	} else {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("sceNetAdhocPdpCreate: returning error=0x%08X", result));
    		}
    	}

		return result;
    }

    /**
     * Send a PDP packet to a destination
     *
     * @param id - The ID as returned by ::sceNetAdhocPdpCreate
     * @param destMacAddr - The destination MAC address, can be set to all 0xFF for broadcast
     * @param port - The port to send to
     * @param data - The data to send
     * @param len - The length of the data.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return Bytes sent, < 0 on error
     */
    @HLEFunction(nid = 0xABED3790, version = 150)
    public int sceNetAdhocPdpSend(@CheckArgument("checkPdpId") int id, TPointer destMacAddr, int port, TPointer data, int len, int timeout, int nonblock) {
    	pspNetMacAddress destMacAddress = new pspNetMacAddress();
    	destMacAddress.read(Memory.getInstance(), destMacAddr.getAddress());

    	log.warn(String.format("PARTIAL: sceNetAdhocPdpSend id=%d, destMacAddr=%s(%s), port=%d, data=%s, len=%d, timeout=%d, nonblock=%d, data: %s", id, destMacAddr, destMacAddress, port, data, len, timeout, nonblock, Utilities.getMemoryDump(data.getAddress(), len, 4, 16)));

    	return pdpObjects.get(id).send(destMacAddress, port, data, len, timeout, nonblock);
    }

    /**
     * Receive a PDP packet
     *
     * @param id - The ID of the PDP object, as returned by ::sceNetAdhocPdpCreate
     * @param srcMacAddr - Buffer to hold the source mac address of the sender
     * @param port - Buffer to hold the port number of the received data
     * @param data - Data buffer
     * @param dataLength - The length of the data buffer
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return Number of bytes received, < 0 on error.
     */
    @HLEFunction(nid = 0xDFE53E03, version = 150)
    public int sceNetAdhocPdpRecv(@CheckArgument("checkPdpId") int id, TPointer srcMacAddr, TPointer16 portAddr, TPointer data, TPointer32 dataLengthAddr, int timeout, int nonblock) {

    	log.warn(String.format("PARTIAL: sceNetAdhocPdpRecv id=%d, srcMacAddr=%s, portAddr=%s, data=%s, dataLengthAddr=%s(%d), timeout=%d, nonblock=%d", id, srcMacAddr, portAddr, data, dataLengthAddr, dataLengthAddr.getValue(), timeout, nonblock));

    	pspNetMacAddress srcMacAddress = new pspNetMacAddress();
        int result = pdpObjects.get(id).recv(srcMacAddress, portAddr, data, dataLengthAddr, timeout, nonblock);
        srcMacAddress.write(Memory.getInstance(), srcMacAddr.getAddress());

        return result;
    }

    /**
     * Delete a PDP object.
     *
     * @param id - The ID returned from ::sceNetAdhocPdpCreate
     * @param unk1 - Unknown, set to 0
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x7F27BB5E, version = 150)
    public int sceNetAdhocPdpDelete(@CheckArgument("checkPdpId") int id, int unk1) {
        log.warn(String.format("PARTIAL: sceNetAdhocPdpDelete id=%d, unk=%d", id, unk1));

        pdpObjects.remove(id).delete();

        return 0;
    }

    /**
     * Get the status of all PDP objects
     *
     * @param size - Pointer to the size of the stat array (e.g 20 for one structure)
     * @param stat - Pointer to a list of ::pspStatStruct structures.
     *
     * typedef struct pdpStatStruct
     * {
     *    struct pdpStatStruct *next; // Pointer to next PDP structure in list
     *    int pdpId;                  // pdp ID
     *    unsigned char mac[6];       // MAC address
     *    unsigned short port;        // Port
     *    unsigned int rcvdData;      // Bytes received
     * } pdpStatStruct
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xC7C1FC57, version = 150)
    public int sceNetAdhocGetPdpStat(TPointer32 sizeAddr, @CanBeNull TPointer buf) {
    	final int objectInfoSize = 20;
        log.warn(String.format("PARTIAL: sceNetAdhocGetPdpStat sizeAddr=%s(%d), buf=%s", sizeAddr.toString(), sizeAddr.getValue(), buf.toString()));

        if (buf.getAddress() == 0) {
        	// Return size required
        	sizeAddr.setValue(objectInfoSize * pdpObjects.size());
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceNetAdhocGetPdpStat returning size=%d", sizeAddr.getValue()));
        	}
        } else {
        	Memory mem = Memory.getInstance();
        	int addr = buf.getAddress();
        	int endAddr = addr + sizeAddr.getValue();
        	sizeAddr.setValue(objectInfoSize * pdpObjects.size());
        	for (int pdpId : pdpObjects.keySet()) {
        		PdpObject pdpObject = pdpObjects.get(pdpId);

        		// Check if enough space available to write the next structure
        		if (addr + objectInfoSize > endAddr || pdpObject == null) {
        			break;
        		}

        		pdpObject.update();

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetAdhocGetPdpStat returning %s at 0x%08X", pdpObject, addr));
        		}

        		/** Pointer to next PDP structure in list: will be written later */
        		addr += 4;

        		/** pdp ID */
        		mem.write32(addr, pdpObject.getId());
        		addr += 4;

        		/** MAC address */
        		pdpObject.getMacAddress().write(mem, addr);
        		addr += pdpObject.getMacAddress().sizeof();

        		/** Port */
        		mem.write16(addr, (short) pdpObject.getPort());
        		addr += 2;

        		/** Bytes received */
        		mem.write32(addr, pdpObject.getRcvdData());
        		addr += 4;
        	}

        	for (int nextAddr = buf.getAddress(); nextAddr < addr; nextAddr += objectInfoSize) {
        		if (nextAddr + objectInfoSize >= addr) {
        			// Last one
        			mem.write32(nextAddr, 0);
        		} else {
        			// Pointer to next one
        			mem.write32(nextAddr, nextAddr + objectInfoSize);
        		}
        	}
        }

        return 0;
    }

    /**
     * Open a PTP connection
     *
     * @param srcmac - Local mac address.
     * @param srcport - Local port.
     * @param destmac - Destination mac.
     * @param destport - Destination port
     * @param bufsize - Socket buffer size
     * @param delay - Interval between retrying (microseconds).
     * @param count - Number of retries.
     * @param unk1 - Pass 0.
     *
     * @return A socket ID on success, < 0 on error.
     */
    @HLEFunction(nid = 0x877F6D66, version = 150)
    public int sceNetAdhocPtpOpen(TPointer srcMacAddr, int srcPort, TPointer destMacAddr, int destPort, int bufSize, int retryDelay, int retryCount, int unk1) {
    	Memory mem = Memory.getInstance();
    	pspNetMacAddress srcMacAddress = new pspNetMacAddress();
    	srcMacAddress.read(mem, srcMacAddr.getAddress());
    	pspNetMacAddress destMacAddress = new pspNetMacAddress();
    	destMacAddress.read(mem, destMacAddr.getAddress());

    	log.warn(String.format("PARTIAL: sceNetAdhocPtpOpen scrMacAddr=%s(%s), srcPort=%d, destMacAddr=%s(%s), destPort=%d, bufSize=0x%X, retryDelay=%d, retryCount=%d, unk1=%d", srcMacAddr, srcMacAddress, srcPort, destMacAddr, destMacAddress, destPort, bufSize, retryDelay, retryCount, unk1));

    	PtpObject ptpObject = new PtpObject();
    	ptpObject.setMacAddress(srcMacAddress);
    	ptpObject.setPort(srcPort);
    	ptpObject.setDestMacAddress(destMacAddress);
    	ptpObject.setDestPort(destPort);
    	ptpObject.setBufSize(bufSize);
    	ptpObject.setRetryDelay(retryDelay);
    	ptpObject.setRetryCount(retryCount);
    	int result = ptpObject.open();
    	if (result != 0) {
    		// Open failed...
    		ptpObject.delete();
    		return result;
    	}

    	ptpObjects.put(ptpObject.getId(), ptpObject);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetAdhocPtpOpen: returning id=%d", ptpObject.getId()));
		}

		return ptpObject.getId();
    }

    /**
     * Wait for connection created by sceNetAdhocPtpOpen()
     *
     * @param id - A socket ID.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xFC6FC07B, version = 150)
    public int sceNetAdhocPtpConnect(@CheckArgument("checkPtpId") int id, int timeout, int nonblock) {
        log.warn(String.format("PARTIAL: sceNetAdhocPtpConnect id=%d, timeout=%d, nonblock=%d", id, timeout, nonblock));

        return ptpObjects.get(id).connect(timeout, nonblock);
    }

    /**
     * Wait for an incoming PTP connection
     *
     * @param srcmac - Local mac address.
     * @param srcport - Local port.
     * @param bufsize - Socket buffer size
     * @param delay - Interval between retrying (microseconds).
     * @param count - Number of retries.
     * @param queue - Connection queue length.
     * @param unk1 - Pass 0.
     *
     * @return A socket ID on success, < 0 on error.
     */
    @HLEFunction(nid = 0xE08BDAC1, version = 150)
    public int sceNetAdhocPtpListen(TPointer srcMacAddr, int srcPort, int bufSize, int retryDelay, int retryCount, int queue, int unk1) {
    	Memory mem = Memory.getInstance();
    	pspNetMacAddress srcMacAddress = new pspNetMacAddress();
    	srcMacAddress.read(mem, srcMacAddr.getAddress());

    	log.warn(String.format("PARTIAL: sceNetAdhocPtpListen scrMacAddr=%s(%s), srcPort=%d, bufSize=0x%X, retryDelay=%d, retryCount=%d, queue=%d, unk1=%d", srcMacAddr, srcMacAddress, srcPort, bufSize, retryDelay, retryCount, queue, unk1));

    	PtpObject ptpObject = new PtpObject();
    	ptpObject.setMacAddress(srcMacAddress);
    	ptpObject.setPort(srcPort);
    	ptpObject.setBufSize(bufSize);
    	ptpObject.setRetryDelay(retryDelay);
    	ptpObject.setRetryCount(retryCount);
    	ptpObject.setQueue(queue);
    	int result = ptpObject.listen();
    	if (result != 0) {
    		// Listen failed...
    		ptpObject.delete();
    		return result;
    	}

    	ptpObjects.put(ptpObject.getId(), ptpObject);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetAdhocPtpListen: returning id=%d", ptpObject.getId()));
		}

    	return ptpObject.getId();
    }

    /**
     * Accept an incoming PTP connection
     *
     * @param id - A socket ID.
     * @param mac - Connecting peers mac.
     * @param port - Connecting peers port.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x9DF81198, version = 150)
    public int sceNetAdhocPtpAccept(@CheckArgument("checkPtpId") int id, @CanBeNull TPointer peerMacAddr, @CanBeNull TPointer16 peerPortAddr, int timeout, int nonblock) {
        log.warn(String.format("PARTIAL: sceNetAdhocPtpAccept id=%d, peerMacAddr=%s, peerPort=%s, timeout=%d, nonblock=%d", id, peerMacAddr, peerPortAddr, timeout, nonblock));

        return ptpObjects.get(id).accept(peerMacAddr.getAddress(), peerPortAddr.getAddress(), timeout, nonblock);
    }

    /**
     * Send data
     *
     * @param id - A socket ID.
     * @param data - Data to send.
     * @param datasize - Size of the data.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return 0 success, < 0 on error.
     */
    @HLEFunction(nid = 0x4DA4C788, version = 150)
    public int sceNetAdhocPtpSend(@CheckArgument("checkPtpId") int id, TPointer data, TPointer32 dataSizeAddr, int timeout, int nonblock) {
        log.warn(String.format("PARTIAL: sceNetAdhocPtpSend id=%d, data=%s, dataSizeAddr=%s(%d), timeout=%d, nonblock=%d: %s", id, data, dataSizeAddr, dataSizeAddr.getValue(), timeout, nonblock, Utilities.getMemoryDump(data.getAddress(), dataSizeAddr.getValue(), 4, 16)));

        return ptpObjects.get(id).send(data.getAddress(), dataSizeAddr, timeout, nonblock);
    }

    /**
     * Receive data
     *
     * @param id - A socket ID.
     * @param data - Buffer for the received data.
     * @param datasize - Size of the data received.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x8BEA2B3E, version = 150)
    public int sceNetAdhocPtpRecv(@CheckArgument("checkPtpId") int id, TPointer data, TPointer32 dataSizeAddr, int timeout, int nonblock) {
        log.warn(String.format("PARTIAL: sceNetAdhocPtpRecv id=%d, data=%s, dataSizeAddr=%s(%d), timeout=%d, nonblock=%d", id, data, dataSizeAddr, dataSizeAddr.getValue(), timeout, nonblock));

        return ptpObjects.get(id).recv(data, dataSizeAddr, timeout, nonblock);
    }

    /**
     * Wait for data in the buffer to be sent
     *
     * @param id - A socket ID.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return A socket ID on success, < 0 on error.
     */
    @HLEFunction(nid = 0x9AC2EEAC, version = 150)
    public int sceNetAdhocPtpFlush(@CheckArgument("checkPtpId") int id, int timeout, int nonblock) {
        log.warn(String.format("PARTIAL: sceNetAdhocPtpFlush id=%d, timeout=%d, nonblock=%d", id, timeout, nonblock));

        // Faked: return successful completion
        return 0;
    }

    /**
     * Close a socket
     *
     * @param id - A socket ID.
     * @param unk1 - Pass 0.
     *
     * @return A socket ID on success, < 0 on error.
     */
    @HLEFunction(nid = 0x157E6225, version = 150)
    public int sceNetAdhocPtpClose(@CheckArgument("checkPtpId") int id, int unknown) {
        log.warn(String.format("PARTIAL: sceNetAdhocPtpClose id=%d, unknown=%d", id, unknown));

        ptpObjects.remove(id).delete();

        return 0;
    }

    /**
     * Get the status of all PTP objects
     *
     * @param size - Pointer to the size of the stat array (e.g 20 for one structure)
     * @param stat - Pointer to a list of ::ptpStatStruct structures.
     *
     * typedef struct ptpStatStruct
     * {
     *    struct ptpStatStruct *next; // Pointer to next PTP structure in list
     *    int ptpId;                  // ptp ID
     *    unsigned char mac[6];       // MAC address
     *    unsigned char peermac[6];   // Peer MAC address
     *    unsigned short port;        // Port
     *    unsigned short peerport;    // Peer Port
     *    unsigned int sentData;      // Bytes sent
     *    unsigned int rcvdData;      // Bytes received
     *    int unk1;                   // Unknown
     * } ptpStatStruct;
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xB9685118, version = 150)
    public int sceNetAdhocGetPtpStat(TPointer32 sizeAddr, @CanBeNull TPointer buf) {
    	final int objectInfoSize = 36;
        log.warn(String.format("PARTIAL: sceNetAdhocGetPtpStat sizeAddr=%s(%d), buf=%s", sizeAddr.toString(), sizeAddr.getValue(), buf.toString()));

        if (buf.getAddress() == 0) {
        	// Return size required
        	sizeAddr.setValue(objectInfoSize * ptpObjects.size());
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceNetAdhocGetPtpStat returning size=%d", sizeAddr.getValue()));
        	}
        } else {
        	Memory mem = Memory.getInstance();
        	int addr = buf.getAddress();
        	int endAddr = addr + sizeAddr.getValue();
        	sizeAddr.setValue(objectInfoSize * ptpObjects.size());
        	pspNetMacAddress nonExistingDestMacAddress = new pspNetMacAddress();
        	for (int pdpId : ptpObjects.keySet()) {
        		PtpObject ptpObject = ptpObjects.get(pdpId);

        		// Check if enough space available to write the next structure
        		if (addr + objectInfoSize > endAddr || ptpObject == null) {
        			break;
        		}

        		ptpObject.update();

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetAdhocGetPtpStat returning %s at 0x%08X", ptpObject, addr));
        		}

        		/** Pointer to next PDP structure in list: will be written later */
        		addr += 4;

        		/** ptp ID */
        		mem.write32(addr, ptpObject.getId());
        		addr += 4;

        		/** MAC address */
        		ptpObject.getMacAddress().write(mem, addr);
        		addr += ptpObject.getMacAddress().sizeof();

        		/** Dest MAC address */
        		if (ptpObject.getDestMacAddress() != null) {
        			ptpObject.getDestMacAddress().write(mem, addr);
        			addr += ptpObject.getDestMacAddress().sizeof();
        		} else {
        			nonExistingDestMacAddress.write(mem, addr);
        			addr += nonExistingDestMacAddress.sizeof();
        		}

        		/** Port */
        		mem.write16(addr, (short) ptpObject.getPort());
        		addr += 2;

        		/** Dest Port */
        		mem.write16(addr, (short) ptpObject.getDestPort());
        		addr += 2;

        		/** Bytes sent */
        		mem.write32(addr, ptpObject.getSentData());
        		addr += 4;

        		/** Bytes received */
        		mem.write32(addr, ptpObject.getRcvdData());
        		addr += 4;

        		/** Unknown */
        		mem.write32(addr, 4); // PSP seems to return value 4 here
        		addr += 4;
        	}

        	for (int nextAddr = buf.getAddress(); nextAddr < addr; nextAddr += objectInfoSize) {
        		if (nextAddr + objectInfoSize >= addr) {
        			// Last one
        			mem.write32(nextAddr, 0);
        		} else {
        			// Pointer to next one
        			mem.write32(nextAddr, nextAddr + objectInfoSize);
        		}
        	}
        }

        return 0;
    }

    /**
     * Create own game object type data.
     *
     * @param data - A pointer to the game object data.
     * @param size - Size of the game data.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x7F75C338, version = 150)
    public int sceNetAdhocGameModeCreateMaster(TPointer data, int size) {
        log.warn(String.format("PARTIAL: sceNetAdhocGameModeCreateMaster data=%s, size=%d", data, size));

        masterGameModeArea = new GameModeArea(data.getAddress(), size);
        startGameMode();

        return 0;
    }

    /**
     * Create peer game object type data.
     *
     * @param mac - The mac address of the peer.
     * @param data - A pointer to the game object data.
     * @param size - Size of the game data.
     *
     * @return The id of the replica on success, < 0 on error.
     */
    @HLEFunction(nid = 0x3278AB0C, version = 150)
    public int sceNetAdhocGameModeCreateReplica(TPointer macAddr, TPointer data, int size) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr.getAddress());
        log.warn(String.format("PARTIAL: sceNetAdhocGameModeCreateReplica macAddr=%s(%s), data=%s, size=%d", macAddr, macAddress, data, size));

        boolean found = false;
        int result = 0;
        for (GameModeArea gameModeArea : replicaGameModeAreas) {
        	if (isSameMacAddress(gameModeArea.macAddress.macAddress, macAddress.macAddress)) {
        		// Updating the exiting replica
        		gameModeArea.addr = data.getAddress();
        		gameModeArea.size = size;
        		result = gameModeArea.id;
        		found = true;
        		break;
        	}
        }

        if (!found) {
        	GameModeArea gameModeArea = new GameModeArea(macAddress, data.getAddress(), size);
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("Adding GameMode Replica %s", gameModeArea));
        	}
        	result = gameModeArea.id;
        	replicaGameModeAreas.add(gameModeArea);
        }

        startGameMode();

        return result;
    }

    /**
     * Update own game object type data.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x98C204C8, version = 150)
    public int sceNetAdhocGameModeUpdateMaster() {
        log.warn("PARTIAL: sceNetAdhocGameModeUpdateMaster");

        if (masterGameModeArea != null) {
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("Master Game Mode Area: %s", Utilities.getMemoryDump(masterGameModeArea.addr, masterGameModeArea.size, 4, 16)));
        	}
        	masterGameModeArea.setNewData();
        }

        return 0;
    }

    /**
     * Update peer game object type data.
     *
     * @param id - The id of the replica returned by sceNetAdhocGameModeCreateReplica.
     * @param info - address of GameModeUpdateInfo structure.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xFA324B4E, version = 150)
    public int sceNetAdhocGameModeUpdateReplica(int id, @CanBeNull TPointer infoAddr) {
    	Memory mem = Memory.getInstance();
        log.warn(String.format("PARTIAL: sceNetAdhocGameModeUpdateReplica id=%d, infoAddr=%s", id, infoAddr));

        for (GameModeArea gameModeArea : replicaGameModeAreas) {
        	if (gameModeArea.id == id) {
        		GameModeUpdateInfo gameModeUpdateInfo = new GameModeUpdateInfo();
        		if (infoAddr.getAddress() != 0) {
        			gameModeUpdateInfo.read(mem, infoAddr.getAddress());
        		}

        		if (gameModeArea.hasNewData()) {
        			if (log.isDebugEnabled()) {
        				log.debug(String.format("Updating GameMode Area with new data: %s", gameModeArea));
        			}
        			gameModeArea.writeNewData();
        			gameModeArea.resetNewData();
                	if (log.isTraceEnabled()) {
                		log.trace(String.format("Replica GameMode Area updated: %s", Utilities.getMemoryDump(gameModeArea.addr, gameModeArea.size, 4, 16)));
                	}
        			gameModeUpdateInfo.updated = 1;
        		} else {
        			gameModeUpdateInfo.updated = 0;
        		}

        		if (infoAddr.getAddress() != 0) {
        			gameModeUpdateInfo.timeStamp = gameModeArea.getUpdateTimestamp();
        			gameModeUpdateInfo.write(mem);
        		}
        		break;
        	}
        }

        return 0;
    }

    /**
     * Delete own game object type data.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xA0229362, version = 150)
    public int sceNetAdhocGameModeDeleteMaster() {
        log.warn("PARTIAL: sceNetAdhocGameModeDeleteMaster");

        masterGameModeArea = null;
        if (replicaGameModeAreas.size() <= 0) {
        	stopGameMode();
        }

        return 0;
    }

    /**
     * Delete peer game object type data.
     *
     * @param id - The id of the replica.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x0B2228E9, version = 150)
    public int sceNetAdhocGameModeDeleteReplica(int id) {
        log.warn(String.format("PARTIAL: sceNetAdhocGameModeDeleteReplica id=%d", id));

        for (GameModeArea gameModeArea : replicaGameModeAreas) {
        	if (gameModeArea.id == id) {
        		replicaGameModeAreas.remove(gameModeArea);
        		break;
        	}
        }

        if (replicaGameModeAreas.size() <= 0 && masterGameModeArea == null) {
        	stopGameMode();
        }

        return 0;
    }
}