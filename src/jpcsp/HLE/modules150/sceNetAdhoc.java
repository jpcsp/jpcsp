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
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
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

    protected HashMap<Integer, PdpObject> pdpObjects;
    private int currentPdpId;
    private int currentFreePort;

    /**
     * An AdhocMessage is consisting of:
     * - 6 bytes for the MAC address of the message sender
     * - 6 bytes for the MAC address of the message recipient
     * - n bytes for the message data
     */
    public static class AdhocMessage {
    	protected byte[] fromMacAddress = new byte[Wlan.MAC_ADDRESS_LENGTH];
    	protected byte[] toMacAddress = new byte[Wlan.MAC_ADDRESS_LENGTH];
    	protected byte[] data = new byte[0];
    	protected static final int HEADER_SIZE = Wlan.MAC_ADDRESS_LENGTH + Wlan.MAC_ADDRESS_LENGTH;
    	protected static final byte[] ANY_MAC_ADDRESS = new byte[] {
    			(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
    	};

    	protected AdhocMessage() {
    	}

    	public AdhocMessage(byte[] message, int length) {
    		if (length >= HEADER_SIZE) {
    			System.arraycopy(message, 0, fromMacAddress, 0, fromMacAddress.length);
    			System.arraycopy(message, fromMacAddress.length, toMacAddress, 0, toMacAddress.length);
    			data = new byte[length - HEADER_SIZE];
    			System.arraycopy(message, HEADER_SIZE, data, 0, data.length);
    		}
    	}

    	public AdhocMessage(int address, int length) {
    		init(address, length, ANY_MAC_ADDRESS);
    	}

    	public AdhocMessage(int address, int length, byte[] toMacAddress) {
    		init(address, length, toMacAddress);
    	}

    	private void init(int address, int length, byte[] toMacAddress) {
    		System.arraycopy(Wlan.getMacAddress(), 0, fromMacAddress, 0, fromMacAddress.length);
    		System.arraycopy(toMacAddress, 0, this.toMacAddress, 0, this.toMacAddress.length);
    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
    		data = new byte[length];
    		for (int i = 0; i < length; i++) {
    			data[i] = (byte) memoryReader.readNext();
    		}
    	}

    	public byte[] getMessage() {
    		byte[] message = new byte[getMessageLength()];
    		System.arraycopy(fromMacAddress, 0, message, 0, fromMacAddress.length);
    		System.arraycopy(toMacAddress, 0, message, fromMacAddress.length, toMacAddress.length);
    		System.arraycopy(data, 0, message, HEADER_SIZE, data.length);

    		return message;
    	}

    	public int getMessageLength() {
    		return getMessageLength(data.length);
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

    	public boolean isForMe() {
    		return isAnyMacAddress(toMacAddress) || isSameMacAddress(toMacAddress, Wlan.getMacAddress());
    	}

		@Override
		public String toString() {
			return String.format("AdhocMessage[fromMacAddress=%s, toMacAddress=%d, dataLength=%d]", convertMacAddressToString(fromMacAddress), convertMacAddressToString(toMacAddress), getDataLength());
		}
    }

    private class PdpObject {
    	/** pdp ID */
    	private final int pdpId;
    	/** MAC address */
    	private pspNetMacAddress macAddress;
    	/** Port */
    	private int port;
    	/** Bytes received */
    	private int rcvdData;
    	/** Buffer size */
    	private int bufSize;
    	private DatagramSocket socket;
    	private SysMemInfo buffer;
    	private pspNetMacAddress rcvdMacAddress = new pspNetMacAddress();
    	private int rcvdPort;

    	public PdpObject() {
    		pdpId = currentPdpId++;
    	}

		public pspNetMacAddress getMacAddress() {
			return macAddress;
		}

		public void setMacAddress(pspNetMacAddress macAddress) {
			this.macAddress = macAddress;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public int getRcvdData() {
			return rcvdData;
		}

		public int getPdpId() {
			return pdpId;
		}

		public void setBufSize(int bufSize) {
			this.bufSize = bufSize;
			if (buffer != null) {
				Modules.SysMemUserForUserModule.free(buffer);
				buffer = null;
			}
			buffer = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, getName(), SysMemUserForUser.PSP_SMEM_Low, bufSize, 0);
		}

		public void delete() {
			closeSocket();
		}

		private SocketAddress getSocketAddress(pspNetMacAddress macAddress, int macPort) throws UnknownHostException {
			if (netClientPortShift > 0 || netServerPortShift > 0) {
				return new InetSocketAddress(InetAddress.getLocalHost(), macPort);
			}
			return sceNetInet.getBroadcastInetSocketAddress(macPort);
		}

		private void setTimeout(int timeout, int nonblock) throws SocketException {
			if (nonblock != 0) {
				socket.setSoTimeout(1);
			} else {
				socket.setSoTimeout(timeout);
			}
		}

		private void setBroadcast(pspNetMacAddress macAddress) throws SocketException {
			socket.setBroadcast(macAddress.isAnyMacAddress());
		}

		public int send(pspNetMacAddress destMacAddress, int destPort, TPointer data, int length, int timeout, int nonblock) {
			int result = length;

			try {
				openSocket();
				setTimeout(timeout, nonblock);
				setBroadcast(destMacAddress);
				int realPort = destPort + netClientPortShift;
				SocketAddress socketAddress = getSocketAddress(destMacAddress, realPort);
				AdhocMessage adhocMessage = new AdhocMessage(data.getAddress(), length, destMacAddress.macAddress);
				DatagramPacket packet = new DatagramPacket(adhocMessage.getMessage(), adhocMessage.getMessageLength(), socketAddress);
				socket.send(packet);
				if (log.isDebugEnabled()) {
					log.debug(String.format("Successfully sent %d bytes to port %d(%d)", length, destPort, realPort));
				}
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
					AdhocMessage adhocMessage = new AdhocMessage(packet.getData(), packet.getLength());
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
			if (rcvdData < bufSize) {
				try {
					openSocket();
					socket.setSoTimeout(1);
					byte[] bytes = new byte[AdhocMessage.getMessageLength(bufSize - rcvdData)];
					DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
					socket.receive(packet);
					AdhocMessage adhocMessage = new AdhocMessage(packet.getData(), packet.getLength());
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

		private void openSocket() throws SocketException {
			if (socket == null) {
				int realPort = port + netServerPortShift;
				if (log.isDebugEnabled()) {
					log.debug(String.format("Opening socket on port %d(%d)", port, realPort));
				}
				socket = new DatagramSocket(realPort);
			}
		}

		private void closeSocket() {
			if (socket != null) {
				socket.close();
				socket = null;
			}
		}

		@Override
		public String toString() {
			return String.format("PdpObject[id=%d, macAddress=%s, port=%d, bufSize=%d, rcvdData=%d]", pdpId, macAddress, port, bufSize, rcvdData);
		}
    }

    @Override
    public String getName() {
        return "sceNetAdhoc";
    }

    @Override
	public void start() {
	    pdpObjects = new HashMap<Integer, sceNetAdhoc.PdpObject>();
	    currentPdpId = 1;
	    currentFreePort = 0x4000;

	    super.start();
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
			throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOC_INVALID_PDP_ID);
		}

		return pdpId;
	}

	/**
     * Initialise the adhoc library.
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xE1D621D7, version = 150)
    public void sceNetAdhocInit(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("IGNORING: sceNetAdhocInit");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    /**
     * Terminate the adhoc library
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xA62C6F57, version = 150)
    public void sceNetAdhocTerm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("IGNORING: sceNetAdhocTerm");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
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
    	pdpObjects.put(pdpObject.getPdpId(), pdpObject);

    	return pdpObject.getPdpId();
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
        		mem.write32(addr, pdpObject.getPdpId());
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
    public void sceNetAdhocPtpOpen(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpOpen");

        cpu.gpr[2] = 0xDEADC0DE;
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
    public void sceNetAdhocPtpConnect(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpConnect");

        cpu.gpr[2] = 0xDEADC0DE;
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
    public void sceNetAdhocPtpListen(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpListen");

        cpu.gpr[2] = 0xDEADC0DE;
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
    public void sceNetAdhocPtpAccept(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpAccept");

        cpu.gpr[2] = 0xDEADC0DE;
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
    public void sceNetAdhocPtpSend(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpSend");

        cpu.gpr[2] = 0xDEADC0DE;
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
    public void sceNetAdhocPtpRecv(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpRecv");

        cpu.gpr[2] = 0xDEADC0DE;
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
    public void sceNetAdhocPtpFlush(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpFlush");

        cpu.gpr[2] = 0xDEADC0DE;
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