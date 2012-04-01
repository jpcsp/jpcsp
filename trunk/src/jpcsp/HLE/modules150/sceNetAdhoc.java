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
import static jpcsp.HLE.modules150.sceNetAdhoc.AdhocPtpMessage.PTP_MESSAGE_TYPE_OPEN;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;

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
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.hardware.Wlan;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceNetAdhoc extends HLEModule {
    protected static Logger log = Modules.getLogger("sceNetAdhoc");

    // For test purpose when running 2 different Jpcsp instances on the same computer:
    // one computer has to have netClientPortShift=0 and netServerPortShift=1,
    // the other computer, netClientPortShift=1 and netServerPortShift=0.
    public int netClientPortShift = 0;
    public int netServerPortShift = 0;

    // Polling period (micro seconds) for blocking operations
    protected static final int BLOCKED_OPERATION_POLLING_MICROS = 10000;

    protected HashMap<Integer, PdpObject> pdpObjects;
    protected HashMap<Integer, PtpObject> ptpObjects;
    private int currentFreePort;
	public static final byte[] ANY_MAC_ADDRESS = new byte[] {
		(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
	};
	private static final String uidPurpose = "sceNetAdhoc";

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
    		System.arraycopy(fromMacAddress, 0, this.fromMacAddress, 0, this.fromMacAddress.length);
    		System.arraycopy(toMacAddress, 0, this.toMacAddress, 0, this.toMacAddress.length);
    		additionalHeaderData = new byte[additionalHeaderLength];
    		data = new byte[length];
    		if (length > 0 && address != 0) {
	    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
	    		for (int i = 0; i < length; i++) {
	    			data[i] = (byte) memoryReader.readNext();
	    		}
    		}
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

    	public int getDataLength() {
    		return data.length;
    	}

    	public byte[] getFromMacAddress() {
    		return fromMacAddress;
    	}

    	public byte[] getToMacAddress() {
    		return toMacAddress;
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
    	protected static final int PTP_MESSAGE_TYPE_OPEN = 1;

		public AdhocPtpMessage(byte[] message, int length) {
			super(message, length);
		}

    	public AdhocPtpMessage(byte[] fromMacAddress, byte[] toMacAddress, int type) {
    		super(fromMacAddress, toMacAddress);
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
				int realPort = port + Modules.sceNetAdhocModule.netServerPortShift;
				if (log.isDebugEnabled()) {
					log.debug(String.format("Opening socket on port %d(%d)", port, realPort));
				}
				socket = new DatagramSocket(realPort);
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
				socket.setSoTimeout(timeout);
			}
		}

		protected void setBroadcast(pspNetMacAddress macAddress) throws SocketException {
			socket.setBroadcast(macAddress.isAnyMacAddress());
		}

		protected void send(AdhocMessage adhocMessage) throws IOException {
			send(adhocMessage, getPort());
		}

		protected void send(AdhocMessage adhocMessage, int destPort) throws IOException {
			openSocket();

			int realPort = destPort + Modules.sceNetAdhocModule.netClientPortShift;
			SocketAddress socketAddress = Modules.sceNetAdhocModule.getSocketAddress(adhocMessage.getToMacAddress(), realPort);
			DatagramPacket packet = new DatagramPacket(adhocMessage.getMessage(), adhocMessage.getMessageLength(), socketAddress);
			socket.send(packet);

			if (log.isDebugEnabled()) {
				log.debug(String.format("Successfully sent %d bytes to port %d(%d)", adhocMessage.getDataLength(), destPort, realPort));
			}
		}
    }

    protected class PdpObject extends AdhocObject {
    	/** MAC address */
    	private pspNetMacAddress macAddress;
    	/** Bytes received */
    	private int rcvdData;
    	private pspNetMacAddress rcvdMacAddress = new pspNetMacAddress();
    	private int rcvdPort;

		public pspNetMacAddress getMacAddress() {
			return macAddress;
		}

		public void setMacAddress(pspNetMacAddress macAddress) {
			this.macAddress = macAddress;
		}

		public int getRcvdData() {
			return rcvdData;
		}

		public int send(pspNetMacAddress destMacAddress, int destPort, TPointer data, int length, int timeout, int nonblock) {
			int result = length;

			try {
				openSocket();
				setTimeout(timeout, nonblock);
				setBroadcast(destMacAddress);
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

			// Faked: sending all data
			return result;
		}

		public int recv(pspNetMacAddress srcMacAddress, TPointer16 portAddr, TPointer data, TPointer32 dataLengthAddr, int timeout, int nonblock) {
			int result = SceKernelErrors.ERROR_NET_ADHOC_NO_DATA_AVAILABLE;
			int length = dataLengthAddr.getValue();

			if (rcvdData > 0) {
				// Copy the data already received
				if (rcvdData < length) {
					length = rcvdData;
				}
				Memory.getInstance().memcpy(data.getAddress(), buffer.addr, length);
				dataLengthAddr.setValue(length);
				srcMacAddress.setMacAddress(rcvdMacAddress.macAddress);
				portAddr.setValue(rcvdPort);

				// Update the buffer
				if (length < rcvdData) {
					// Move the remaining buffer data to the beginning of the buffer
					Memory.getInstance().memmove(buffer.addr, buffer.addr + length, rcvdData - length);
				}
				rcvdData -= length;

				if (log.isDebugEnabled()) {
					log.debug(String.format("Returned received data: %d bytes from %s on port %d: %s", length, srcMacAddress, portAddr.getValue(), Utilities.getMemoryDump(data.getAddress(), dataLengthAddr.getValue(), 4, 16)));
				}
				result = 0;
			} else {
				try {
					openSocket();
					setTimeout(timeout, nonblock);
					byte[] bytes = new byte[AdhocMessage.getMessageLength(length)];
					DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
					socket.receive(packet);
					AdhocMessage adhocMessage = new AdhocPdpMessage(packet.getData(), packet.getLength());
					if (adhocMessage.isForMe()) {
						adhocMessage.writeDataToMemory(data.getAddress());
						dataLengthAddr.setValue(adhocMessage.getDataLength());
						srcMacAddress.setMacAddress(adhocMessage.getFromMacAddress());
						int clientPort = packet.getPort() - netClientPortShift;
						portAddr.setValue(clientPort);
						if (log.isDebugEnabled()) {
							log.debug(String.format("Successfully received %d bytes from %s on port %d(%d): %s", adhocMessage.getDataLength(), srcMacAddress, clientPort, packet.getPort(), Utilities.getMemoryDump(data.getAddress(), dataLengthAddr.getValue(), 4, 16)));
						}
						result = 0;
					} else {
						if (log.isDebugEnabled()) {
							log.debug(String.format("Received message not for me: %s", adhocMessage));
						}
					}
				} catch (SocketException e) {
					log.error("recv", e);
				} catch (SocketTimeoutException e) {
					// Timeout
				} catch (IOException e) {
					log.error("recv", e);
				}
			}

			return result;
		}

		public void update() {
			if (rcvdData < getBufSize()) {
				try {
					openSocket();
					socket.setSoTimeout(1);
					byte[] bytes = new byte[AdhocMessage.getMessageLength(getBufSize() - rcvdData)];
					DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
					socket.receive(packet);
					AdhocMessage adhocMessage = new AdhocPdpMessage(packet.getData(), packet.getLength());
					if (adhocMessage.isForMe()) {
						int bufferAddr = buffer.addr + rcvdData;
						adhocMessage.writeDataToMemory(bufferAddr);
						rcvdData += adhocMessage.getDataLength();
						rcvdMacAddress.setMacAddress(adhocMessage.getFromMacAddress());
						rcvdPort = packet.getPort() - netClientPortShift;
						if (log.isDebugEnabled()) {
							log.debug(String.format("Successfully received %d bytes from %s on port %d(%d): %s", adhocMessage.getDataLength(), rcvdMacAddress, rcvdPort, packet.getPort(), Utilities.getMemoryDump(bufferAddr, adhocMessage.getDataLength(), 4, 16)));
						}
					} else {
						if (log.isDebugEnabled()) {
							log.debug(String.format("Received message not for me: %s", adhocMessage));
						}
					}
				} catch (SocketException e) {
					log.error("update", e);
				} catch (SocketTimeoutException e) {
					// Timeout
				} catch (IOException e) {
					log.error("update", e);
				}
			}
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

		public int open() {
			int result = 0;

			try {
				AdhocPtpMessage adhocPtpMessage = new AdhocPtpMessage(getMacAddress().macAddress, getDestMacAddress().macAddress, PTP_MESSAGE_TYPE_OPEN);
				send(adhocPtpMessage, destPort);
			} catch (SocketException e) {
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
			} catch (SocketException e) {
				log.error("listen", e);
			}

			return result;
		}

		public int accept(int peerMacAddr, int peerPortAddr, int timeout, int nonblock) {
			int result = 0;

			if (nonblock == 0) {
				long schedule = Emulator.getClock().microTime() + BLOCKED_OPERATION_POLLING_MICROS;
				Emulator.getScheduler().addAction(schedule, new BlockedPtpAccept(this, peerMacAddr, peerPortAddr, timeout));
				Modules.ThreadManForUserModule.hleBlockCurrentThread();
			}

			return result;
		}

		public boolean pollAccept(int peerMacAddr, int peerPortAddr) {
			boolean acceptCompleted = false;
			Memory mem = Memory.getInstance();

			try {
				byte[] bytes = new byte[getBufSize()];
				DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
				socket.receive(packet);
				AdhocPtpMessage adhocPtpMessage = new AdhocPtpMessage(packet.getData(), packet.getLength());
				if (log.isDebugEnabled()) {
					log.debug(String.format("pollAccept: received message %s", adhocPtpMessage));
				}
				if (adhocPtpMessage.isForMe()) {
					switch (adhocPtpMessage.getType()) {
						case AdhocPtpMessage.PTP_MESSAGE_TYPE_OPEN:
							if (peerMacAddr != 0) {
								pspNetMacAddress peerMacAddress = new pspNetMacAddress();
								peerMacAddress.setMacAddress(adhocPtpMessage.getFromMacAddress());
								peerMacAddress.write(mem, peerMacAddr);
							}
							if (peerPortAddr != 0) {
								int peerPort = packet.getPort() - netClientPortShift;
								mem.write16(peerPortAddr, (short) peerPort);
							}
							acceptCompleted = true;
							break;
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
    }

    protected static class BlockedPtpAccept implements IAction {
    	private final PtpObject ptpObject;
    	private final int peerMacAddr;
    	private final int peerPortAddr;
    	private final long timeoutMicros;
    	private final int threadUid;

    	public BlockedPtpAccept(PtpObject ptpObject, int peerMacAddr, int peerPortAddr, int timeout) {
    		this.ptpObject = ptpObject;
    		this.peerMacAddr = peerMacAddr;
    		this.peerPortAddr = peerPortAddr;
    		timeoutMicros = Emulator.getClock().microTime() + timeout;
    		threadUid = Modules.ThreadManForUserModule.getCurrentThreadID();
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("BlockedPtdAccept for thread %s", Modules.ThreadManForUserModule.getThreadById(threadUid)));
    		}
    	}

		@Override
		public void execute() {
			SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getThreadById(threadUid);

			if (log.isDebugEnabled()) {
				log.debug(String.format("BlockedPtpObject: poll on %s, thread %s", ptpObject, thread));
			}

			if (ptpObject.pollAccept(peerMacAddr, peerPortAddr)) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("BlockedPtpObject: unblocking thread %s", thread));
				}
				thread.cpuContext.gpr[Common._v0] = ptpObject.getId();
				Modules.ThreadManForUserModule.hleUnblockThread(threadUid);
			} else {
				long now = Emulator.getClock().microTime();
				if (now >= timeoutMicros) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("BlockedPtpObject: timeout for thread %s", thread));
					}
					// Unblock thread and return timeout error
					thread.cpuContext.gpr[Common._v0] = SceKernelErrors.ERROR_NET_ADHOC_TIMEOUT;
					Modules.ThreadManForUserModule.hleUnblockThread(threadUid);
				} else {
					if (log.isDebugEnabled()) {
						log.debug(String.format("BlockedPtpObject: continue polling"));
					}
					long schedule = now + BLOCKED_OPERATION_POLLING_MICROS;
					Emulator.getScheduler().addAction(schedule, this);
				}
			}
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

	    super.start();
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
			throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOC_INVALID_SOCKET_ID);
		}

		return pdpId;
	}

	public int checkPtpId(int ptpId) {
		if (!ptpObjects.containsKey(ptpId)) {
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
    public void sceNetAdhocPollSocket(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPollSocket");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x73BFD52D, version = 150)
    public void sceNetAdhocSetSocketAlert(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocSetSocketAlert");

        cpu.gpr[2] = 0xDEADC0DE;
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
    	pdpObject.setMacAddress(macAddress);
    	pdpObject.setPort(port);
    	pdpObject.setBufSize(bufSize);
    	pdpObjects.put(pdpObject.getId(), pdpObject);

    	return pdpObject.getId();
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

    	log.warn(String.format("UNIMPLEMENTED: sceNetAdhocPdpSend id=%d, destMacAddr=%s(%s), port=%d, data=%s, len=%d, timeout=%d, nonblock=%d, data: %s", id, destMacAddr, destMacAddress, port, data, len, timeout, nonblock, Utilities.getMemoryDump(data.getAddress(), len, 4, 16)));

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

    	log.warn(String.format("UNIMPLEMENTED: sceNetAdhocPdpRecv id=%d, srcMacAddr=%s, portAddr=%s, data=%s, dataLengthAddr=%s(%d), timeout=%d, nonblock=%d", id, srcMacAddr, portAddr, data, dataLengthAddr, dataLengthAddr.getValue(), timeout, nonblock));

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
        log.warn(String.format("PARTIAL: sceNetAdhocGetPdpStat sizeAddr=%s(%d), buf=%s", sizeAddr.toString(), sizeAddr.getValue(), buf.toString()));

        if (buf.getAddress() == 0) {
        	// Return size required
        	sizeAddr.setValue(20 * pdpObjects.size());
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceNetAdhocGetPdpStat returning size=%d", sizeAddr.getValue()));
        	}
        } else {
        	Memory mem = Memory.getInstance();
        	int addr = buf.getAddress();
        	int endAddr = addr + sizeAddr.getValue();
        	for (int pdpId : pdpObjects.keySet()) {
        		PdpObject pdpObject = pdpObjects.get(pdpId);

        		// Check if enough space available to write the next structure
        		if (addr + 20 > endAddr || pdpObject == null) {
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

        	for (int nextAddr = buf.getAddress(); nextAddr < addr; nextAddr += 20) {
        		if (nextAddr + 20 >= addr) {
        			// Last one
        			mem.write32(nextAddr, 0);
        		} else {
        			// Pointer to next one
        			mem.write32(nextAddr, nextAddr + 20);
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

    	log.warn(String.format("UNIMPLEMENTED: sceNetAdhocPtpOpen scrMacAddr=%s(%s), srcPort=%d, destMacAddr=%s(%s), destPort=%d, bufSize=0x%X, retryDelay=%d, retryCount=%d, unk1=%d", srcMacAddr, srcMacAddress, srcPort, destMacAddr, destMacAddress, destPort, bufSize, retryDelay, retryCount, unk1));

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
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocPtpConnect id=%d, timeout=%d, nonblock=%d", id, timeout, nonblock));

        return 0;
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

    	log.warn(String.format("UNIMPLEMENTED: sceNetAdhocPtpListen scrMacAddr=%s(%s), srcPort=%d, bufSize=0x%X, retryDelay=%d, retryCount=%d, queue=%d, unk1=%d", srcMacAddr, srcMacAddress, srcPort, bufSize, retryDelay, retryCount, queue, unk1));

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
    public int sceNetAdhocPtpAccept(@CheckArgument("checkPtpId") int id, TPointer peerMacAddr, TPointer16 peerPortAddr, int timeout, int nonblock) {
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocPtpAccept id=%d, peerMacAddr=%s, peerPort=%s, timeout=%d, nonblock=%d", id, peerMacAddr, peerPortAddr, timeout, nonblock));

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
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocPtpSend id=%d, data=%s, dataSizeAddr=%s(%d), timeout=%d, nonblock=%d: %s", id, data, dataSizeAddr, dataSizeAddr.getValue(), timeout, nonblock, Utilities.getMemoryDump(data.getAddress(), dataSizeAddr.getValue(), 4, 16)));

        // Faked: returning all data sent
        dataSizeAddr.setValue(dataSizeAddr.getValue());

        return 0;
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
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocPtpRecv id=%d, data=%s, dataSizeAddr=%s(%d), timeout=%d, nonblock=%d", id, data, dataSizeAddr, dataSizeAddr.getValue(), timeout, nonblock));

        // Faked: returning 0 bytes read
        dataSizeAddr.setValue(0);

        return 0;
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
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocPtpFlush id=%d, timeout=%d, nonblock=%d", id, timeout, nonblock));

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
    public void sceNetAdhocPtpClose(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpClose");

        cpu.gpr[2] = 0xDEADC0DE;
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
    public void sceNetAdhocGetPtpStat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGetPtpStat");

        cpu.gpr[2] = 0xDEADC0DE;
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
    public void sceNetAdhocGameModeCreateMaster(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeCreateMaster");

        cpu.gpr[2] = 0xDEADC0DE;
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
    public void sceNetAdhocGameModeCreateReplica(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeCreateReplica");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Update own game object type data.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x98C204C8, version = 150)
    public void sceNetAdhocGameModeUpdateMaster(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeUpdateMaster");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Update peer game object type data.
     *
     * @param id - The id of the replica returned by sceNetAdhocGameModeCreateReplica.
     * @param unk1 - Pass 0.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xFA324B4E, version = 150)
    public void sceNetAdhocGameModeUpdateReplica(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeUpdateReplica");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Delete own game object type data.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xA0229362, version = 150)
    public void sceNetAdhocGameModeDeleteMaster(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeDeleteMaster");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Delete peer game object type data.
     *
     * @param id - The id of the replica.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x0B2228E9, version = 150)
    public void sceNetAdhocGameModeDeleteReplica(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeDeleteReplica");

        cpu.gpr[2] = 0xDEADC0DE;
    }
}