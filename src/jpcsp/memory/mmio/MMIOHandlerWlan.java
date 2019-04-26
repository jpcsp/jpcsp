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
package jpcsp.memory.mmio;

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_WLAN_INTR;
import static jpcsp.HLE.modules.sceNet.convertMacAddressToString;
import static jpcsp.HLE.modules.sceWlan.WLAN_CMD_DATA;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.hardware.Wlan.getMacAddress;
import static jpcsp.memory.mmio.MMIOHandlerDdr.DDR_FLUSH_DMAC;
import static jpcsp.util.Utilities.alignUp;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.sceNetAdhocctl;
import jpcsp.HLE.modules.sceNetInet;
import jpcsp.HLE.modules.sceWlan;
import jpcsp.memory.IntArrayMemory;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.NetworkAdapterFactory;
import jpcsp.network.accesspoint.AccessPoint;
import jpcsp.network.accesspoint.IAccessPointCallback;
import jpcsp.network.protocols.EtherFrame;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

/**
 * MMIO for Wlan.
 * 
 * The Wlan interface is very similar to the MemoryStick Pro interface.
 * 
 * @author gid15
 *
 */
public class MMIOHandlerWlan extends MMIOHandlerBaseMemoryStick implements IAccessPointCallback {
	private static final int STATE_VERSION = 0;
	// Based on https://github.com/torvalds/linux/blob/master/drivers/net/wireless/marvell/libertas/host.h
	public static final int CMD_GET_HW_SPEC                   = 0x0003;
	public static final int CMD_EEPROM_UPDATE                 = 0x0004;
	public static final int CMD_802_11_RESET                  = 0x0005;
	public static final int CMD_802_11_SCAN                   = 0x0006;
	public static final int CMD_802_11_GET_LOG                = 0x000B;
	public static final int CMD_MAC_MULTICAST_ADR             = 0x0010;
	public static final int CMD_802_11_AUTHENTICATE           = 0x0011;
	public static final int CMD_UNKNOWN_0012                  = 0x0012;
	public static final int CMD_802_11_SET_WEP                = 0x0013;
	public static final int CMD_802_11_GET_STAT               = 0x0014;
	public static final int CMD_802_3_GET_STAT                = 0x0015;
	public static final int CMD_802_11_SNMP_MIB               = 0x0016;
	public static final int CMD_MAC_REG_MAP                   = 0x0017;
	public static final int CMD_BBP_REG_MAP                   = 0x0018;
	public static final int CMD_MAC_REG_ACCESS                = 0x0019;
	public static final int CMD_BBP_REG_ACCESS                = 0x001A;
	public static final int CMD_RF_REG_ACCESS                 = 0x001B;
	public static final int CMD_802_11_RADIO_CONTROL          = 0x001C;
	public static final int CMD_802_11_RF_CHANNEL             = 0x001D;
	public static final int CMD_802_11_RF_TX_POWER            = 0x001E;
	public static final int CMD_802_11_RSSI                   = 0x001F;
	public static final int CMD_802_11_RF_ANTENNA             = 0x0020;
	public static final int CMD_802_11_PS_MODE                = 0x0021;
	public static final int CMD_802_11_DATA_RATE              = 0x0022;
	public static final int CMD_RF_REG_MAP                    = 0x0023;
	public static final int CMD_802_11_DEAUTHENTICATE         = 0x0024;
	public static final int CMD_802_11_REASSOCIATE            = 0x0025;
	public static final int CMD_UNKNOWN_0026                  = 0x0026;
	public static final int CMD_MAC_CONTROL                   = 0x0028;
	public static final int CMD_UNKNOWN_002A                  = 0x002A;
	public static final int CMD_802_11_AD_HOC_START           = 0x002B;
	public static final int CMD_802_11_AD_HOC_JOIN            = 0x002C;
	public static final int CMD_802_11_QUERY_TKIP_REPLY_CNTRS = 0x002E;
	public static final int CMD_802_11_ENABLE_RSN             = 0x002F;
	public static final int CMD_UNKNOWN_0030                  = 0x0030;
	public static final int CMD_UNKNOWN_0032                  = 0x0032;
	public static final int CMD_UNKNOWN_0034                  = 0x0034;
	public static final int CMD_UNKNOWN_0035                  = 0x0035;
	public static final int CMD_802_11_SET_AFC                = 0x003C;
	public static final int CMD_802_11_GET_AFC                = 0x003D;
	public static final int CMD_802_11_DEEP_SLEEP             = 0x003E;
	public static final int CMD_802_11_AD_HOC_STOP            = 0x0040;
	public static final int CMD_UNKNOWN_0041                  = 0x0041;
	public static final int CMD_UNKNOWN_0042                  = 0x0042;
	public static final int CMD_802_11_HOST_SLEEP_CFG         = 0x0043;
	public static final int CMD_802_11_WAKEUP_CONFIRM         = 0x0044;
	public static final int CMD_802_11_HOST_SLEEP_ACTIVATE    = 0x0045;
	public static final int CMD_UNKNOWN_0047                  = 0x0047;
	public static final int CMD_802_11_BEACON_STOP            = 0x0049;
	public static final int CMD_802_11_MAC_ADDRESS            = 0x004D;
	public static final int CMD_802_11_LED_GPIO_CTRL          = 0x004E;
	public static final int CMD_802_11_ASSOCIATE              = 0x0050;
	public static final int CMD_802_11_BAND_CONFIG            = 0x0058;
	public static final int CMD_802_11_EEPROM_ACCESS          = 0x0059;
	public static final int CMD_GSPI_BUS_CONFIG               = 0x005A;
	public static final int CMD_802_11D_DOMAIN_INFO           = 0x005B;
	public static final int CMD_802_11_KEY_MATERIAL           = 0x005E;
	public static final int CMD_802_11_SLEEP_PARAMS           = 0x0066;
	public static final int CMD_802_11_INACTIVITY_TIMEOUT     = 0x0067;
	public static final int CMD_802_11_SLEEP_PERIOD           = 0x0068;
	public static final int CMD_802_11_TPC_CFG                = 0x0072;
	public static final int CMD_802_11_PA_CFG                 = 0x0073;
	public static final int CMD_802_11_FW_WAKE_METHOD         = 0x0074;
	public static final int CMD_802_11_SUBSCRIBE_EVENT        = 0x0075;
	public static final int CMD_802_11_RATE_ADAPT_RATESET     = 0x0076;
	public static final int CMD_UNKNOWN_0079                  = 0x0079;
	public static final int CMD_UNKNOWN_007A                  = 0x007A;
	public static final int CMD_UNKNOWN_007B                  = 0x007B;
	public static final int CMD_UNKNOWN_007C                  = 0x007C;
	public static final int CMD_UNKNOWN_007D                  = 0x007D;
	public static final int CMD_802_11_TX_RATE_QUERY          = 0x007F;
	public static final int CMD_GET_TSF                       = 0x0080;
	public static final int CMD_BT_ACCESS                     = 0x0087;
	public static final int CMD_FWT_ACCESS                    = 0x0095;
	public static final int CMD_802_11_MONITOR_MODE           = 0x0098;
	public static final int CMD_MESH_ACCESS                   = 0x009B;
	public static final int CMD_MESH_CONFIG_OLD               = 0x00A3;
	public static final int CMD_MESH_CONFIG                   = 0x00AC;
	public static final int CMD_SET_BOOT2_VER                 = 0x00A5;
	public static final int CMD_FUNC_INIT                     = 0x00A9;
	public static final int CMD_FUNC_SHUTDOWN                 = 0x00AA;
	public static final int CMD_802_11_BEACON_CTRL            = 0x00B0;
	//
	public static final int BSS_TYPE_INFRASTRUCTURE = 1;
	public static final int BSS_TYPE_ADHOC = 2;
	//
	public static final int DATA_ADDRESS_CMD = 0x100;
	public static final int DATA_ADDRESS_PACKET = 0x102;
	//
	private final IntArrayMemory attributesMemory = new IntArrayMemory(new int[0x40 / 4]);
	private final IntArrayMemory commandPacket = new IntArrayMemory(new int[0xC00 / 4]);
	private final TPointer commandPacketPtr = commandPacket.getPointer();
	private final IntArrayMemory sendDataPacket = new IntArrayMemory(new int[0xC00 / 4]);
	private final TPointer sendDataPacketPtr = sendDataPacket.getPointer();
	private final IntArrayMemory receiveDataPacket = new IntArrayMemory(new int[0xC00 / 4]);
	private final TPointer receiveDataPacketPtr = receiveDataPacket.getPointer();
	private static final int DUMMY_ATTRIBUTE_ENTRY = 0x1234;
	private static final int WLAN_REG_RECEIVED_PACKET_LENGTH = 0x40;
	private static final int WLAN_REG_OUTPUT_PACKET_SIZE = 0x44;
	private static final int WLAN_REG_CMD_RESPONSE_PACKET_LENGTH = 0x4A;
	private static final int WLAN_REG_INPUT_PACKET_SIZE = 0x4C;
	private static final int WLAN_REG_RESULT = 0x54;
	//
	private static final int WLAN_RESULT_READY_TO_SEND = 0x01;
	private static final int WLAN_RESULT_DATA_PACKET_RECEIVED = 0x02;
	private static final int WLAN_RESULT_COMMAND_RESPONSE_AVAILABLE = 0x10;
	//
	private final byte[] chipCode = new byte[0x173FC];
	private int chipCodeIndex;
	private boolean booting;
	private boolean adhocStarted;
	private boolean adhocJoined;
	private String adhocSsid;
	private static final int dataSocketPortBase = 31000;
    private static final int maxDataSocketPorts = 16;
	private DatagramSocket dataSocket;
    private int dataSocketPort;
    private AccessPoint accessPoint;
    private INetworkAdapter networkAdapter;

	public MMIOHandlerWlan(int baseAddress) {
		super(baseAddress);

		log = sceWlan.log;

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		stream.readBytes(chipCode);
		chipCodeIndex = stream.readInt();
		booting = stream.readBoolean();
		adhocStarted = stream.readBoolean();
		adhocJoined = stream.readBoolean();
		adhocSsid = stream.readString();
		stream.readIntArrayMemory(attributesMemory);
		stream.readIntArrayMemory(commandPacket);
		stream.readIntArrayMemory(sendDataPacket);
		stream.readIntArrayMemory(receiveDataPacket);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeBytes(chipCode);
		stream.writeInt(chipCodeIndex);
		stream.writeBoolean(booting);
		stream.writeBoolean(adhocStarted);
		stream.writeBoolean(adhocJoined);
		stream.writeString(adhocSsid);
		stream.writeIntArrayMemory(attributesMemory);
		stream.writeIntArrayMemory(commandPacket);
		stream.writeIntArrayMemory(sendDataPacket);
		stream.writeIntArrayMemory(receiveDataPacket);
		super.write(stream);
	}

	@Override
	protected void reset() {
		super.reset();

		// Possible values:
		// 0x0011 0x0001 0x0001
		// 0x0011 0x0001 0x1B18
		// 0x0011 0x0002 0x1B11
		// 0x0011 0x0002 0x0B11
		attributesMemory.writeUnsigned16(0, 0x0011);
		attributesMemory.writeUnsigned16(2, 0x0001);
		attributesMemory.writeUnsigned16(4, 0x1B18);

		for (int i = 1; i < 8; i++) {
			int offset = i * 8;
			attributesMemory.writeUnsigned16(offset + 0, i); // Has to be a value in range [1..8]
			attributesMemory.writeUnsigned16(offset + 2, DUMMY_ATTRIBUTE_ENTRY); // Unknown address used for MSPRO_CMD_READ_IO_ATRB
			attributesMemory.writeUnsigned16(offset + 4, 0x0040); // Unknown size
		}
		swapData32(attributesMemory, 0, 0x40);

		// Behaves like a Memory Stick Pro.
		registers[MS_TYPE_ADDRESS] = MS_TYPE_MEMORY_STICK_PRO;
		// For now in serial mode, it will be set later by the PSP to parallel mode.
		registers[MS_SYSTEM_ADDRESS] = MS_SYSTEM_SERIAL_MODE;

		// Unknown register value
		setRegisterValue(0x22, 0x24);

		// Clear packet buffers
		commandPacketPtr.clear(commandPacket.getSize());
		sendDataPacketPtr.clear(sendDataPacket.getSize());
		receiveDataPacketPtr.clear(receiveDataPacket.getSize());

		chipCodeIndex = -1;
		booting = true;
	}

	private void createAccessPoint() {
		if (accessPoint == null) {
			accessPoint = new AccessPoint(this);
		}
	}

	private void createNetworkAdapter() {
		if (networkAdapter == null) {
			networkAdapter = NetworkAdapterFactory.createNetworkAdapter();
			networkAdapter.start();
		}
	}

	private void setSsid(String ssid) {
		this.adhocSsid = ssid;

		Pattern p = Pattern.compile("PSP_S(.........)_L_(.*)");
		Matcher m = p.matcher(ssid);
		if (m.matches()) {
			String productId = m.group(1);
			String groupName = m.group(2);
			Modules.sceNetAdhocctlModule.hleNetAdhocctlInit(2, productId);
			networkAdapter.sceNetAdhocctlInit();
			Modules.sceNetAdhocctlModule.setGroupName(groupName, sceNetAdhocctl.PSP_ADHOCCTL_MODE_NORMAL);
		} else {
			Modules.sceNetAdhocctlModule.hleNetAdhocctlInit(2, "000000001");
			networkAdapter.sceNetAdhocctlInit();
			Modules.sceNetAdhocctlModule.setGroupName(ssid, sceNetAdhocctl.PSP_ADHOCCTL_MODE_NORMAL);
		}
	}

	private boolean createDataSocket() {
		if (dataSocket == null) {
			for (int i = 0; i < maxDataSocketPorts; i++) {
	    		try {
	    			int port = dataSocketPortBase + i;
	    			dataSocket = new DatagramSocket(port);
		    		// For broadcast
	    			dataSocket.setBroadcast(true);
		    		// Non-blocking (timeout = 0 would mean blocking)
	    			dataSocket.setSoTimeout(1);
	    			dataSocketPort = port;
	    			break;
	    		} catch (BindException e) {
	    			// The port is already busy, retrying with another port
	    			if (log.isDebugEnabled()) {
	    				log.debug(String.format("createDataSocket port %d already in use (%s) - retrying with port %d", dataSocketPort, e, dataSocketPort + 1));
	    			}
				} catch (SocketException e) {
					log.error("createDataSocket", e);
					break;
				}
			}

			if (dataSocket == null) {
				log.error(String.format("createDataSocket could not create broadcast socket in port range [%d..%d]", dataSocketPortBase, dataSocketPortBase + maxDataSocketPorts - 1));
			}
		}

		return dataSocket != null;
	}

	static private int swap32(int value) {
		return (value >>> 16) | (value << 16);
	}

	static private void swapData32(Memory mem, int address, int length) {
		for (int i = 0; i < length; i += 4) {
			mem.write32(address + i, swap32(mem.read32(address + i)));
		}
	}

	@Override
	protected void initMsproAttributeMemory() {
		log.error(String.format("MMIOHandlerWlan.initMsproAttributeMemory not supported"));
	}

	@Override
	protected int getInterruptNumber() {
		return PSP_WLAN_INTR;
	}

	@Override
	protected int getInterruptBit() {
		return 0x0000;
	}

	private void addResultFlag(int flag) {
		registers[WLAN_REG_RESULT] |= flag;
	}

	private void clearResultFlag(int flag) {
		registers[WLAN_REG_RESULT] &= ~flag;
	}

	private int getWlanOutputPacketSize() {
		return getRegisterValue(WLAN_REG_OUTPUT_PACKET_SIZE, 2);
	}

	private void setWlanInputPacketSize(int inputPacketSize) {
		setRegisterValue(WLAN_REG_INPUT_PACKET_SIZE, 2, inputPacketSize);
	}

	@Override
	protected int getRegisterValue(int register) {
		switch (register) {
			case WLAN_REG_CMD_RESPONSE_PACKET_LENGTH:
				if (booting) {
					// Finished writing the chip code, return a fixed magic value
					setRegisterValue(WLAN_REG_CMD_RESPONSE_PACKET_LENGTH, 4, endianSwap32(0x46554755));
					chipCodeIndex = -1;
					booting = false;
				}
				break;
			case WLAN_REG_RESULT:
				receiveBroadcastMessages();
				break;
		}

		return super.getRegisterValue(register);
	}

	@Override
	protected void setRegisterValue(int register, int value) {
		switch (register) {
			case WLAN_REG_RESULT:
				// Writing to this register seems to only have the effect to clear bits of its value
				value = registers[register] & value;
				break;
		}

		super.setRegisterValue(register, value);

		switch (register) {
			case 0x56:
				// Writing to this register seems to also have the effect of updating the result register
				addResultFlag(0x04);
				break;
			case 0x5E:
				// Writing to this register seems to also have the effect of updating the result register
				addResultFlag(0x80);
				break;
		}
	}

	@Override
	protected int readData16(int dataAddress, int dataIndex, boolean endOfCommand) {
		int value = 0;

		switch (cmd) {
			case MSPRO_CMD_READ_IO_ATRB:
				if (dataAddress == 0 && dataIndex < attributesMemory.getSize()) {
					value = attributesMemory.read16(dataIndex);
				} else if (dataAddress == DUMMY_ATTRIBUTE_ENTRY) {
					// Dummy entry
					value = 0;
				} else {
					log.error(String.format("MMIOHandlerWlan.readData16 unimplemented cmd=0x%X(%s), dataAddress=0x%X, dataIndex=0x%X", cmd, getCommandName(cmd), dataAddress, dataIndex));
				}
				break;
			case MSPRO_CMD_IN_IO_FIFO:
				value = getReceivePacket(dataAddress).read16(dataIndex);
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerWlan.readData16 MSPRO_CMD_IN_IO_FIFO, dataAddress=0x%X, dataIndex=0x%X, endOfCommand=%b, value=0x%04X", dataAddress, dataIndex, endOfCommand, value));
				}

				if (endOfCommand) {
					switch (dataAddress) {
						case DATA_ADDRESS_CMD:
							if (log.isTraceEnabled()) {
								int bufferLength = dataIndex + 2;
								byte[] buffer = commandPacketPtr.getArray8(bufferLength);
								endianSwap32(buffer, 0, bufferLength);
								log.trace(String.format("MMIOHandlerWlan.readData16 finished reading command packet: %s", Utilities.getMemoryDump(buffer, 0, bufferLength)));
							}
							clearResultFlag(WLAN_RESULT_COMMAND_RESPONSE_AVAILABLE);
							break;
						case DATA_ADDRESS_PACKET:
							if (log.isTraceEnabled()) {
								int bufferLength = dataIndex + 2;
								byte[] buffer = receiveDataPacketPtr.getArray8(bufferLength);
								endianSwap32(buffer, 0, bufferLength);
								log.trace(String.format("MMIOHandlerWlan.readData16 finished reading data packet: %s", Utilities.getMemoryDump(buffer, 0, bufferLength)));
							}
							clearResultFlag(WLAN_RESULT_DATA_PACKET_RECEIVED);
							break;
						default:
							log.error(String.format("MMIOHandlerWlan.readData16 unimplemented dataAddress=0x%X", dataAddress));
							break;
					}
				}
				break;
			default:
				log.error(String.format("MMIOHandlerWlan.readData16 unimplemented cmd=0x%X(%s), dataAddress=0x%X, dataIndex=0x%X", cmd, getCommandName(cmd), dataAddress, dataIndex));
				break;
		}

		return value;
	}

	@Override
	protected void readPageBuffer() {
		log.error(String.format("MMIOHandlerWlan.readPageBuffer unimplemented"));
	}

	@Override
	protected void writePageBuffer() {
		log.error(String.format("MMIOHandlerWlan.writePageBuffer unimplemented"));
	}

	private void sendCommandResponse(int size) {
		int cmd = commandPacket.read16(0);
		commandPacket.writeUnsigned16(0, cmd | 0x8000);

		setRegisterValue(WLAN_REG_CMD_RESPONSE_PACKET_LENGTH, 2, alignUp(size, 7));

		endianSwap32(commandPacket, 0, size);

		addResultFlag(WLAN_RESULT_COMMAND_RESPONSE_AVAILABLE);
	}

	private void receiveBroadcastMessages() {
		if (!createDataSocket()) {
			return;
		}

    	byte[] bytes = new byte[10000];
		DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
		try {
			dataSocket.receive(datagramPacket);
			byte[] buffer = datagramPacket.getData();
			int offset = datagramPacket.getOffset();
			int length = datagramPacket.getLength();

			if (log.isTraceEnabled()) {
				log.trace(String.format("receiveBroadcastMessages received message: %s", Utilities.getMemoryDump(buffer, offset, length)));
			}

			final int rxPacketLocation = 24; // Needs to be 24 and not 20 (which would be sufficient to be past the header)
			receiveDataPacket.writeUnsigned16(0, 0); // rxStatus
			receiveDataPacket.write8(2, (byte) 0); // SNR
			receiveDataPacket.write8(3, (byte) 0); // rxControl
			receiveDataPacket.writeUnsigned16(4, length); // rxPacketLength
			receiveDataPacket.write8(6, (byte) 0); // NF
			receiveDataPacket.write8(7, (byte) 0); // rxRate
			receiveDataPacket.write32(8, rxPacketLocation); // rxPacketLocation
			receiveDataPacket.write32(12, 0); // reserved
			receiveDataPacket.write8(16, (byte) 0); // priority
			receiveDataPacket.write8(17, (byte) 0); // reserved
			receiveDataPacket.writeUnsigned16(18, 0); // reserved
			receiveDataPacketPtr.clear(20, rxPacketLocation - 20);
			receiveDataPacketPtr.setArray(rxPacketLocation, buffer, offset, length);

			// The PSP is moving 12 bytes from the offset 16 to the offset 24:
			//     memmove(addr + 24, addr + 16, 12)
			// This doesn't really make sense as it overwrites the source and destination
			// MAC addresses of the EtherFrame. Maybe due to a bug in the Wlan card firmware?
			// Anyway, we have to mimic this behavior.
			receiveDataPacketPtr.setArray(16, buffer, offset, 12);

			processReceiveDataPacket(rxPacketLocation + length);
		} catch (SocketTimeoutException e) {
			// Timeout can be ignored as we are polling
		} catch (IOException e) {
			log.error("hleWlanReceiveMessage", e);
		}
	}

	private void broadcastAdhocDataPacket(byte[] buffer, int bufferLength) {
		if (!createDataSocket()) {
			return;
		}

		if (log.isTraceEnabled()) {
    		log.trace(String.format("broadcastAdhocDataPacket %s", Utilities.getMemoryDump(buffer, 0, bufferLength)));
		}

		// Broadcast on all data socket ports, excepted on my own port
		for (int i = 0; i < maxDataSocketPorts; i++) {
			int broadcastDataPort = dataSocketPortBase + i;
			if (broadcastDataPort != dataSocketPort) {
		    	try {
					InetSocketAddress broadcastAddress[] = sceNetInet.getBroadcastInetSocketAddress(broadcastDataPort);
					if (broadcastAddress != null) {
						for (int j = 0; j < broadcastAddress.length; j++) {
							if (log.isTraceEnabled()) {
								log.trace(String.format("broadcastAdhocDataPacket to %s", broadcastAddress[j]));
							}
							DatagramPacket datagramPacket = new DatagramPacket(buffer, bufferLength, broadcastAddress[j]);
							dataSocket.send(datagramPacket);
						}
					}
				} catch (UnknownHostException e) {
					log.error("broadcastAdhocDataPacket", e);
				} catch (IOException e) {
					log.error("broadcastAdhocDataPacket", e);
				}
			}
		}
	}

	private void sendDataPacketToAccessPoint(byte[] txPacket, int txPacketLength) {
		if (!createDataSocket()) {
			return;
		}

		createAccessPoint();

    	byte[] buffer = new byte[txPacketLength + 1 + 32];
    	int offset = 0;
    	// Add the cmd in front of the data
    	buffer[offset] = WLAN_CMD_DATA;
    	offset++;
    	// Add the joined SSID in front of the data - no SSID available in infrastructure mode
    	offset += 32;
    	// Add the data
    	System.arraycopy(txPacket, 0, buffer, offset, txPacketLength);
    	offset += txPacketLength;

    	int bufferLength = offset;
    	try {
			InetSocketAddress broadcastAddress[] = sceNetInet.getBroadcastInetSocketAddress(accessPoint.getPort());
			if (broadcastAddress != null) {
				for (int i = 0; i < broadcastAddress.length; i++) {
					DatagramPacket packet = new DatagramPacket(buffer, bufferLength, broadcastAddress[i]);
					dataSocket.send(packet);
				}
			}
		} catch (UnknownHostException e) {
			log.error("sendAccessPointDataPacket", e);
		} catch (IOException e) {
			log.error("sendAccessPointDataPacket", e);
		}
	}

	private void processTransmitDataPacket(int size) {
		int txStatus = sendDataPacket.read32(0);
		int txControl = sendDataPacket.read32(4);
		int txPacketLocation = sendDataPacket.read32(8);
		int txPacketLength = sendDataPacket.read16(12);
		pspNetMacAddress txDestAddr = new pspNetMacAddress();
		txDestAddr.read(sendDataPacket, 14);
		int priority = sendDataPacket.read8(20);
		int flags = sendDataPacket.read8(21);
		int reserved = sendDataPacket.read16(22);
		if (log.isTraceEnabled()) {
			log.trace(String.format("processTransmitDataPacket size=0x%X, txStatus=0x%X, txControl=0x%X, txPacketLocation=0x%X, txPacketLength=0x%X, txDestAddr=%s, priority=0x%X, flags=0x%X, reserved=0x%X, data:%s", size, txStatus, txControl, txPacketLocation, txPacketLength, txDestAddr, priority, flags, reserved, Utilities.getMemoryDump(sendDataPacket, txPacketLocation, txPacketLength)));
		}

		byte[] txPacket = sendDataPacketPtr.getArray8(txPacketLocation, txPacketLength);
		if (adhocJoined) {
			broadcastAdhocDataPacket(txPacket, txPacketLength);
		} else {
			sendDataPacketToAccessPoint(txPacket, txPacketLength);
		}
	}

	private void processReceiveDataPacket(int size) {
		int rxStatus = receiveDataPacket.read16(0);
		int signalToNoiseRatio = receiveDataPacket.read8(2); // SNR
		int rxControl = receiveDataPacket.read8(3);
		int rxPacketLength = receiveDataPacket.read16(4);
		int noiseFloor = receiveDataPacket.read8(6); // NF
		int rxRate = receiveDataPacket.read8(7);
		int rxPacketLocation = receiveDataPacket.read32(8);
		int reserved1 = receiveDataPacket.read32(12);
		int priority = receiveDataPacket.read8(16);
		int reserved2 = receiveDataPacket.read8(17);
		int reserved3 = receiveDataPacket.read16(18);
		if (log.isTraceEnabled()) {
			log.trace(String.format("processReceiveDataPacket size=0x%X, rxStatus=0x%X, SNR=0x%X, rxControl=0x%X, rxPacketLength=0x%X, NF=0x%X, rxRate=0x%X, rxPacketLocation=0x%X, reserved1=0x%X, priority=0x%X, reserved2=0x%X, reserved3=0x%X, data:%s", size, rxStatus, signalToNoiseRatio, rxControl, rxPacketLength, noiseFloor, rxRate, rxPacketLocation, reserved1, priority, reserved2, reserved3, Utilities.getMemoryDump(receiveDataPacket, rxPacketLocation, rxPacketLength)));
		}

		setRegisterValue(WLAN_REG_RECEIVED_PACKET_LENGTH, 2, alignUp(size, 7));

		endianSwap32(receiveDataPacket, 0, size);

		addResultFlag(WLAN_RESULT_DATA_PACKET_RECEIVED);
	}

	private void processCommandPacket(int size) {
		int cmd = commandPacket.read16(0);
		int bodySize = commandPacket.read16(2);
		int sequenceNumber = commandPacket.read16(4);
		if (log.isTraceEnabled()) {
			log.trace(String.format("processCommandPacket cmd=0x%X, bodySize=0x%X, sequenceNumber=0x%X, %s", cmd, bodySize, sequenceNumber, Utilities.getMemoryDump(commandPacket, 0, size)));
		}

		int resultCode = 0;
		int responseSize = Math.min(size, bodySize);
		int action;
		String ssid;
		int bssType;
		byte[] bssid;
		pspNetMacAddress peerMacAddress;
		switch (cmd) {
			case CMD_GET_HW_SPEC:
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_GET_HW_SPEC bodySize=0x%X", bodySize));
				}
				commandPacket.writeUnsigned16(8, 0); // Hardware interface version number
				commandPacket.writeUnsigned16(10, 0); // Hardware version number
				commandPacket.writeUnsigned16(12, 0); // Number of WCB
				commandPacket.writeUnsigned16(14, 1); // Maximum number of multicast addresses
				commandPacketPtr.setArray(16, getMacAddress());
				commandPacket.writeUnsigned16(22, 0); // Region code
				commandPacket.writeUnsigned16(24, 0); // Number of antenna used
				writeUnaligned32(commandPacket, 26, 0x01000000); // Firmware release number
				writeUnaligned32(commandPacket, 42, 0x000); // Firmware capability information
				responseSize = Math.min(size, bodySize + 2);
				break;
			case CMD_802_11_RF_ANTENNA:
				action = commandPacket.read16(8);
				int antennaNumber = commandPacket.read16(10);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_RF_ANTENNA bodySize=0x%X, action=0x%X, antennaNumber=0x%X", bodySize, action, antennaNumber));
				}
				if (action == 1 && antennaNumber == 1) {
					// Ignored
				} else if (action == 2 && antennaNumber == 1) {
					// Ignored
				} else {
					log.error(String.format("processCommandPacket CMD_802_11_RF_ANTENNA unimplemented action=0x%X, antennaNumber=0x%X", action, antennaNumber));
					resultCode = -1;
				}
				break;
			case CMD_802_11_RADIO_CONTROL:
				action = commandPacket.read16(8);
				int control = commandPacket.read16(10);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_RADIO_CONTROL bodySize=0x%X, action=0x%X, control=0x%X", bodySize, action, control));
				}
				if (action == 1 && (control == 1 || control == 3)) {
					// Ignored
				} else {
					log.error(String.format("processCommandPacket CMD_802_11_RADIO_CONTROL unimplemented action=0x%X, control=0x%X", action, control));
					resultCode = -1;
				}
				break;
			case CMD_802_11_RF_TX_POWER:
				action = commandPacket.read16(8);
				int currentPowerLevel = commandPacket.read16(10);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_RF_TX_POWER bodySize=0x%X, action=0x%X, currentPowerLevel=0x%X", bodySize, action, currentPowerLevel));
				}
				if (action == 1 && currentPowerLevel == 10) {
					commandPacket.write8(12, (byte) 20); // Maximum valid power level
					commandPacket.write8(13, (byte)  0); // Minimum valid power level
				} else {
					log.error(String.format("processCommandPacket CMD_802_11_RF_TX_POWER unimplemented action=0x%X, currentPowerLevel=0x%X", action, currentPowerLevel));
					resultCode = -1;
				}
				break;
			case CMD_802_11_DATA_RATE:
				action = commandPacket.read16(8);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_DATA_RATE bodySize=0x%X, action=0x%X", bodySize, action));
				}
				if (action == 0) { // CMD_ACT_SET_TX_AUTO
					commandPacket.write8(12, (byte) 1); // Data rate [1..2]
					commandPacket.write8(12, (byte) 0); // End
				} else {
					log.error(String.format("processCommandPacket CMD_802_11_DATA_RATE unimplemented action=0x%X", action));
					resultCode = -1;
				}
				break;
			case CMD_802_11_SNMP_MIB:
				action = commandPacket.read16(8);
				int oid = commandPacket.read16(10);
				int oidValueSize = commandPacket.read16(12);
				if (action == 1 && oid == 5 && oidValueSize == 2) { // set RTS Threshold
					if (log.isDebugEnabled()) {
						log.debug(String.format("processCommandPacket CMD_802_11_SNMP_MIB set RTS Threshold=%d", commandPacket.read16(14)));
					}
				} else if (action == 1 && oid == 6 && oidValueSize == 2) { // set Short Retry Limit
					if (log.isDebugEnabled()) {
						log.debug(String.format("processCommandPacket CMD_802_11_SNMP_MIB set Short Retry Limit=%d", commandPacket.read16(14)));
					}
				} else if (action == 1 && oid == 8 && oidValueSize == 2) { // set Fragment Threshold
					if (log.isDebugEnabled()) {
						log.debug(String.format("processCommandPacket CMD_802_11_SNMP_MIB set Fragment Threshold=%d", commandPacket.read16(14)));
					}
				} else if (action == 1 && oid == 0 && oidValueSize == 1) { // set Desired BSS Type
					if (log.isDebugEnabled()) {
						log.debug(String.format("processCommandPacket CMD_802_11_SNMP_MIB set Desired BSS Type=0x%X", commandPacket.read8(14)));
					}
				} else {
					log.error(String.format("processCommandPacket CMD_802_11_SNMP_MIB unimplemented action=0x%X, oid=0x%X, oidValueSize=0x%X, %s", action, Utilities.getMemoryDump(commandPacket, 0, size)));
					resultCode = -1;
				}
				break;
			case CMD_UNKNOWN_007A:
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_UNKNOWN_007A bodySize=0x%X, %s", bodySize, Utilities.getMemoryDump(commandPacket, 0, size)));
				}
				break;
			case CMD_MAC_CONTROL:
				action = commandPacket.read16(8);
				boolean rxOn = (action & 0x0001) != 0;
				boolean txOn = (action & 0x0002) != 0;
				boolean wepOn = (action & 0x0008) != 0;
				boolean unknown20 = (action & 0x0020) != 0;
				boolean unknown40 = (action & 0x0040) != 0;
				boolean promiscousOn = (action & 0x0080) != 0;
				boolean multicastOn = (action & 0x0100) != 0;
				boolean enableProtection = (action & 0x0400) != 0;
				boolean enableWMM = (action & 0x0800) != 0;
				boolean wepType104 = (action & 0x8000) != 0;
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_MAC_CONTROL bodySize=0x%X, action=0x%X, rxOn=%b, txOn=%b, wepOn=%b, unknown20=%b, unknown40=%b, promiscousOn=%b, multicastOn=%b, enableProtection=%b, enableWMM=%b, wepType104=%b", bodySize, action, rxOn, txOn, wepOn, unknown20, unknown40, promiscousOn, multicastOn, enableProtection, enableWMM, wepType104));
				}
				break;
			case CMD_802_11_SCAN:
				bssType = commandPacket.read8(10);
				pspNetMacAddress macAddressFilter = new pspNetMacAddress();
				macAddressFilter.read(commandPacket, 11);
				ssid = null;
				if (bssType == BSS_TYPE_ADHOC) {
					ssid = commandPacketPtr.getStringNZ(17, 32);
				}
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_SCAN bodySize=0x%X, bssType=0x%X, macAddressFilter=%s, unknown=%s", bodySize, bssType, macAddressFilter, Utilities.getMemoryDump(commandPacket, 17, size - 17)));
				}

				commandPacketPtr.clear(11, 21); // Clear the request MAC address and SSID

				int count = 0;
				List<sceNetAdhocctl.AdhocctlNetwork> networks = null;
				switch (bssType) {
					case BSS_TYPE_INFRASTRUCTURE:
						count = 2;
						break;
					case BSS_TYPE_ADHOC:
						createNetworkAdapter();
						networkAdapter.sceNetAdhocctlScan();

						if (!adhocStarted && !adhocJoined) {
							count = 0;
						} else {
							networks = Modules.sceNetAdhocctlModule.getNetworks();
							count = networks.size();
						}
						break;
				}

				commandPacketPtr.setUnsignedValue8(32, count); // Number of APs in the buffer
				int offset = 33;

				final int rssi = 40; // RSSI - Received Signal Strength Indication, value range [0..40] for 1% to 100%
				bssid = null;
				for (int n = 0; n < count; n++) {
					commandPacketPtr.setValue8(14 + n, (byte) rssi);

					int capabilities = 0x0000; // Flags
					switch (bssType) {
						case BSS_TYPE_INFRASTRUCTURE:
							capabilities |= 0x0001; // WLAN_CAPABILITY_BSS
							bssid = new byte[] { 'J', 'p', 'c', 's', 'p', (byte) ('0' + n) };
							ssid = String.format("Jpcsp SSID %d", n + 1);
							break;
						case BSS_TYPE_ADHOC:
							capabilities |= 0x0002; // WLAN_CAPABILITY_IBSS
							bssid = networks.get(n).bssid.getBytes();
							break;
					}

					commandPacketPtr.setArray(offset + 2, bssid);
					long packetTimestamp = 0x0L;
					commandPacketPtr.setUnalignedValue64(offset + 8, packetTimestamp);
					int beaconInterval = 1000; // Need to be != 0
					commandPacketPtr.setUnalignedValue16(offset + 16, beaconInterval);
					commandPacketPtr.setUnalignedValue16(offset + 18, capabilities);
					int params = offset + 20;

					commandPacketPtr.setUnsignedValue8(params + 0, 2); // WLAN_EID_FH_PARAMS
					commandPacketPtr.setUnsignedValue8(params + 1, 5); // length
					commandPacketPtr.setUnalignedValue16(params + 2, 0); // dwell time
					commandPacketPtr.setUnsignedValue8(params + 4, 0); // hop set
					commandPacketPtr.setUnsignedValue8(params + 5, 0); // hop pattern
					commandPacketPtr.setUnsignedValue8(params + 6, 0); // hop index
					params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);

					commandPacketPtr.setUnsignedValue8(params + 0, 3); // WLAN_EID_DS_PARAMS
					commandPacketPtr.setUnsignedValue8(params + 1, 1); // length
					commandPacketPtr.setUnsignedValue8(params + 2, 1); // Channel number
					params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);

					int ssidLength = ssid.length();
					commandPacketPtr.setUnsignedValue8(params + 0, 0); // WLAN_EID_SSID
					commandPacketPtr.setUnsignedValue8(params + 1, ssidLength); // length
					commandPacketPtr.setStringNZ(params + 2, ssidLength, ssid);
					params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);

					int[] rates = new int[] { 0x82, 0x84, 0x8B, 0x96 }; // Supported rates: 1MB, 2MB, 5MB, 11MB
					commandPacketPtr.setUnsignedValue8(params + 0, 1); // WLAN_EID_SUPP_RATES
					commandPacketPtr.setUnsignedValue8(params + 1, rates.length); // length
					for (int i = 0; i < rates.length; i++) {
						commandPacketPtr.setUnsignedValue8(params + 2 + i, rates[i]);
					}
					params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);

					int[] extendedRates = new int[] { 0x0C, 0x12 }; // Supported rates: 6MB, 9MB
					commandPacketPtr.setUnsignedValue8(params + 0, 50); // WLAN_EID_EXT_SUPP_RATES
					commandPacketPtr.setUnsignedValue8(params + 1, extendedRates.length); // length
					for (int i = 0; i < extendedRates.length; i++) {
						commandPacketPtr.setUnsignedValue8(params + 2 + i, extendedRates[i]);
					}
					params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);

					commandPacketPtr.setUnsignedValue8(params + 0, 6); // WLAN_EID_IBSS_PARAMS
					commandPacketPtr.setUnsignedValue8(params + 1, 2); // length
					commandPacketPtr.setUnalignedValue16(params + 2, 0); // ATIM window
					params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);

					commandPacketPtr.setUnsignedValue8(params + 0, 48); // WLAN_EID_RSN
					commandPacketPtr.setUnsignedValue8(params + 1, 18); // length
					commandPacketPtr.setUnalignedValue16(params + 2, 1); // Version
					// Group Cipher Suite
					commandPacketPtr.setUnsignedValue8(params + 4, 0x00);
					commandPacketPtr.setUnsignedValue8(params + 5, 0x0F);
					commandPacketPtr.setUnsignedValue8(params + 6, 0xAC);
					commandPacketPtr.setUnsignedValue8(params + 7, 0x05); // WEP-104
					// Pairwise Cipher Suite
					commandPacketPtr.setUnalignedValue16(params + 8, 1); // Pairwise Cipher Suite Count
					commandPacketPtr.setUnsignedValue8(params + 10, 0x00);
					commandPacketPtr.setUnsignedValue8(params + 11, 0x0F);
					commandPacketPtr.setUnsignedValue8(params + 12, 0xAC);
					commandPacketPtr.setUnsignedValue8(params + 13, 0x05); // WEP-104
					// Authentication Suite
					commandPacketPtr.setUnalignedValue16(params + 14, 1); // Authentication Suite Count
					commandPacketPtr.setUnsignedValue8(params + 16, 0x00);
					commandPacketPtr.setUnsignedValue8(params + 17, 0x0F);
					commandPacketPtr.setUnsignedValue8(params + 18, 0xAC);
					commandPacketPtr.setUnsignedValue8(params + 19, 0x05); // WEP-104
					params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);

					commandPacketPtr.setUnsignedValue8(params + 0, 221); // WLAN_EID_VENDOR_SPECIFIC
					commandPacketPtr.setUnsignedValue8(params + 1, 22); // length
					commandPacketPtr.setArray(params + 2, new byte[] { (byte) 0x00,  (byte) 0x50, (byte) 0xF2, (byte) 0x01 });
					commandPacketPtr.setUnalignedValue16(params + 6, 0x0000);
					commandPacketPtr.setArray(params + 8, new byte[] { (byte) 0x00,  (byte) 0x50, (byte) 0xF2, (byte) 0x01 });
					commandPacketPtr.setUnalignedValue16(params + 12, 0x0001); // Count for following entries
					commandPacketPtr.setArray(params + 14, new byte[] { (byte) 0x00,  (byte) 0x50, (byte) 0xF2, (byte) 0x02 });
					commandPacketPtr.setUnalignedValue16(params + 18, 0x0001); // Count for following entries
					commandPacketPtr.setArray(params + 20, new byte[] { (byte) 0x00,  (byte) 0x50, (byte) 0xF2, (byte) 0x01 });
					params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);

					commandPacketPtr.setUnalignedValue16(offset + 0, params - offset - 2); // Total IE length

					offset = params;
				}

				for (int n = 0; n < count; n++) {
					long tsfTimestamp = 0L; // TSF - Timing Synchronization Function
					commandPacketPtr.setUnalignedValue64(offset, tsfTimestamp);
					offset += 8;
				}

				commandPacketPtr.setUnalignedValue16(30, offset - 33); // Scan response buffer size
				responseSize = offset;

				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_SCAN response: %s", Utilities.getMemoryDump(commandPacket, 0, responseSize)));
				}
				break;
			case CMD_802_11_RF_CHANNEL:
				action = commandPacket.read16(8);
				int channelNumber = commandPacket.read16(10);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_RF_CHANNEL bodySize=0x%X, action=0x%X, channelNumber=0x%X", bodySize, action, channelNumber));
				}
				if (action == 1 && (channelNumber == 1 || channelNumber == 11)) {
					// Ignored
				} else {
					log.error(String.format("processCommandPacket CMD_802_11_RF_CHANNEL unimplemented action=0x%X, channelNumber=0x%X", action, channelNumber));
					resultCode = -1;
				}
				break;
			case CMD_802_11_SET_WEP:
				action = commandPacket.read16(8);
				int transmitKeyIndex = commandPacket.read16(10);
				int[] keyWepTypes = new int[4];
				byte[][] keyWep = new byte[4][16];
				for (int i = 0; i < 4; i++) {
					keyWepTypes[i] = commandPacketPtr.getUnsignedValue8(12 + i);
					keyWep[i] = commandPacketPtr.getArray8(16 + i * 16, 16);
				}
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_SET_WEP bodySize=0x%X, action=0x%X, transmitKeyIndex=0x%X", bodySize, action, transmitKeyIndex));
				}
				if (action == 4 && transmitKeyIndex == 0) { // ACT_REMOVE
					// Ignored
				} else {
					log.error(String.format("processCommandPacket CMD_802_11_SET_WEP unimplemented action=0x%X, transmitKeyIndex=0x%X", action, transmitKeyIndex));
					resultCode = -1;
				}
				break;
			case CMD_UNKNOWN_007C:
				int unknown8 = commandPacket.read16(8);
				int unknown10 = commandPacket.read16(10);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_UNKNOWN_007C bodySize=0x%X, unknown8=0x%X, unknown10=0x%X", bodySize, unknown8, unknown10));
				}
				if (bodySize == 0xC && unknown8 == 1 && unknown10 == 1) {
					// Executed just before a CMD_802_11_AD_HOC_START command
				} else if (bodySize == 0xC && unknown8 == 1 && unknown10 == 0) {
					// Executed just before a CMD_802_11_AD_HOC_STOP command
				} else {
					log.error(String.format("processCommandPacket CMD_UNKNOWN_007C unimplemented %s", Utilities.getMemoryDump(commandPacket, 0, size)));
					resultCode = -1;
				}
				break;
			case CMD_802_11_AD_HOC_START:
				ssid = commandPacketPtr.getStringNZ(8, 32);
				bssType = commandPacketPtr.getUnsignedValue8(40);
				int beaconPeriod = commandPacketPtr.getUnalignedValue16(41);
				int atimWindow = commandPacketPtr.getUnsignedValue8(43);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_AD_HOC_START bodySize=0x%X, ssid='%s', bssType=0x%X, beaconPeriod=0x%X, ATIM Window=0x%X", bodySize, ssid, bssType, beaconPeriod, atimWindow));
				}

				setSsid(ssid);
				adhocStarted = true;
				break;
			case CMD_802_11_AD_HOC_STOP:
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_AD_HOC_STOP bodySize=0x%X", bodySize));
				}
				adhocSsid = null;
				adhocStarted = false;
				adhocJoined = false;
				break;
			case CMD_802_11_AD_HOC_JOIN:
				bssid = commandPacketPtr.getArray8(8, 6);
				ssid = commandPacketPtr.getStringNZ(14, 32);
				bssType = commandPacketPtr.getUnsignedValue8(46);
				beaconPeriod = commandPacketPtr.getUnalignedValue16(47);
				int dtimPeriod = commandPacketPtr.getUnsignedValue8(49);
				long timestamp = commandPacketPtr.getUnalignedValue64(50);
				long startTimestamp = commandPacketPtr.getUnalignedValue64(58);
				int capabilities = commandPacketPtr.getUnalignedValue16(81);
				byte[] dataRates = commandPacketPtr.getArray8(83, 8);
				int failTimeout = commandPacketPtr.getUnalignedValue16(91);
				int probeDelay = commandPacketPtr.getUnalignedValue16(93);
				if (log.isDebugEnabled()) {
					StringBuilder dataRatesString = new StringBuilder();
					for (int i = 0; i < dataRates.length; i++) {
						if (dataRates[i] != (byte) 0) {
							if (dataRatesString.length() > 0) {
								dataRatesString.append(", ");
							}
							dataRatesString.append(String.format("0x%02X", dataRates[i] & 0xFF));
						}
					}
					log.debug(String.format("processCommandPacket CMD_802_11_AD_HOC_JOIN bodySize=0x%X, bssid=%s, ssid='%s', bssType=0x%X, beaconPeriod=0x%X, dtimPeriod=0x%X, timestamp=0x%X, startTimestamp=0x%X, capabilities=0x%X, failTimeout=0x%X, probeDelay=0x%X, dataRates=%s", bodySize, convertMacAddressToString(bssid), ssid, bssType, beaconPeriod, dtimPeriod, timestamp, startTimestamp, capabilities, failTimeout, probeDelay, dataRatesString));
				}

				setSsid(ssid);
				networkAdapter.sceNetAdhocctlConnect();
				adhocJoined = true;
addResultFlag(WLAN_RESULT_READY_TO_SEND);
				break;
			case CMD_802_11_AUTHENTICATE:
				peerMacAddress = new pspNetMacAddress();
				peerMacAddress.read(commandPacket, 8);
				int authType = commandPacket.read8(14);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_AUTHENTICATE bodySize=0x%X, peerMacAddress=%s, authType=0x%X", bodySize, peerMacAddress, authType));
				}
addResultFlag(WLAN_RESULT_READY_TO_SEND);
				break;
			case CMD_UNKNOWN_0012:
				peerMacAddress = new pspNetMacAddress();
				peerMacAddress.read(commandPacket, 8);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_UNKNOWN_0012 bodySize=0x%X, peerMacAddress=%s, unknown %s", bodySize, peerMacAddress, Utilities.getMemoryDump(commandPacket, 14, bodySize - 14)));
				}
				break;
			case CMD_UNKNOWN_007D:
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_UNKNOWN_007D bodySize=0x%X, unknown %s", bodySize, Utilities.getMemoryDump(commandPacket, 8, bodySize - 8)));
				}
				break;
			case CMD_802_11_RSSI:
				int N = commandPacket.read16(8);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_RSSI bodySize=0x%X, N=0x%X", bodySize, N));
				}
				if (N == 0) {
					commandPacket.writeUnsigned16(8, 40); // SNR (a value of 40 means 100% Signal Strength)
					commandPacket.writeUnsigned16(10, 0); // Noise Floor
					commandPacket.writeUnsigned16(12, 0); // Average SNR
					commandPacket.writeUnsigned16(14, 0); // Average Noise Floor
				} else {
					log.error(String.format("processCommandPacket CMD_802_11_RSSI unimplemented N=0x%X", N));
				}
				break;
			case CMD_MAC_MULTICAST_ADR:
				action = commandPacket.read16(8);
				if (action == 1) { // ACT_SET
					int numberOfMulticastAddresses = commandPacket.read16(10);
					pspNetMacAddress[] multicastAddresses = new pspNetMacAddress[numberOfMulticastAddresses];
					for (int i = 0; i < numberOfMulticastAddresses; i++) {
						multicastAddresses[i] = new pspNetMacAddress();
						multicastAddresses[i].read(commandPacket, 12 + MAC_ADDRESS_LENGTH * i);
					}

					if (log.isDebugEnabled()) {
						log.debug(String.format("processCommandPacket CMD_MAC_MULTICAST_ADR bodySize=0x%X, action=0x%X, numberOfMulticastAddresses=0x%X", bodySize, action, numberOfMulticastAddresses));
						for (int i = 0; i < numberOfMulticastAddresses; i++) {
							log.debug(String.format("processCommandPacket CMD_MAC_MULTICAST_ADR multicastAddress#%d=%s", i, multicastAddresses[i]));
						}
					}
				} else {
					log.error(String.format("processCommandPacket CMD_MAC_MULTICAST_ADR unimplemented action=0x%X", action));
				}
				break;
			case CMD_UNKNOWN_0026:
				peerMacAddress = new pspNetMacAddress();
				peerMacAddress.read(commandPacket, 8);
				int unknown = commandPacket.read16(14);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_UNKNOWN_0026 bodySize=0x%X, peerMacAddress=%s, unknown=0x%X", bodySize, peerMacAddress, unknown));
				}
				break;
			case CMD_802_11_DEAUTHENTICATE:
				peerMacAddress = new pspNetMacAddress();
				peerMacAddress.read(commandPacket, 8);
				int reasonCode = commandPacket.read16(14);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_DEAUTHENTICATE bodySize=0x%X, peerMacAddress=%s, reasonCode=0x%X", bodySize, peerMacAddress, reasonCode));
				}
				break;
			default:
				log.error(String.format("processCommandPacket unimplemented cmd=0x%X, size=0x%X, %s", cmd, size, Utilities.getMemoryDump(commandPacket, 0, size)));
				resultCode = -1;
				break;
		}

		commandPacket.writeUnsigned16(6, resultCode); // Result code
		if (responseSize >= 0) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("processCommandPacket resultCode=0x%X, responseSize=0x%X, %s", resultCode, responseSize, Utilities.getMemoryDump(commandPacket, 0, responseSize)));
			}
			sendCommandResponse(responseSize);
		}
	}

	private IntArrayMemory getSendPacket(int dataAddress) {
		switch (dataAddress) {
			case DATA_ADDRESS_CMD:
				return commandPacket;
			case DATA_ADDRESS_PACKET:
				return sendDataPacket;
			default:
				log.error(String.format("getSendPacket unimplemented dataAddress=0x%X", dataAddress));
		}

		return commandPacket;
	}

	private IntArrayMemory getReceivePacket(int dataAddress) {
		switch (dataAddress) {
			case DATA_ADDRESS_CMD:
				return commandPacket;
			case DATA_ADDRESS_PACKET:
				return receiveDataPacket;
			default:
				log.error(String.format("getReceivePacket unimplemented dataAddress=0x%X", dataAddress));
		}

		return commandPacket;
	}

	private void writeDataEndOfCommand(int dataAddress, int size) {
		endianSwap32(getSendPacket(dataAddress), 0, size);
		switch (dataAddress) {
			case DATA_ADDRESS_CMD:
				processCommandPacket(size);
				break;
			case DATA_ADDRESS_PACKET:
				processTransmitDataPacket(size);
				break;
			default:
				log.error(String.format("MMIOHandlerWlan.writeDataEndOfCommand unimplemented dataAddress=0x%X, size=0x%X", dataAddress, size));
				break;
		}
	}

	@Override
	protected void writeData16(int dataAddress, int dataIndex, int value, boolean endOfCommand) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerWlan.writeData16 dataAddress=0x%X, dataIndex=0x%X, outputPacketSize=0x%X, chipCodeIndex=0x%X, value=0x%04X, endOfCommand=%b", dataAddress, dataIndex, getWlanOutputPacketSize(), chipCodeIndex, value, endOfCommand));
		}

		if (booting) {
			log.error(String.format("MMIOHandlerWlan.writeData16 unimplemented while booting"));
		} else {
			getSendPacket(dataAddress).write16(dataIndex, (short) value);
			if (endOfCommand) {
				writeDataEndOfCommand(dataAddress, dataIndex + 2);
			}
		}
	}

	@Override
	protected void writeData32(int dataAddress, int dataIndex, int value, boolean endOfCommand) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerWlan.writeData32 dataAddress=0x%X, dataIndex=0x%X, outputPacketSize=0x%X, chipCodeIndex=0x%X, value=0x%08X, endOfCommand=%b", dataAddress, dataIndex, getWlanOutputPacketSize(), chipCodeIndex, value, endOfCommand));
		}

		if (booting) {
			if (getWlanOutputPacketSize() == 0 && dataIndex == 0xC) {
				chipCodeIndex = 0;
				setWlanInputPacketSize(0x600);
			} else if (chipCodeIndex >= 0) {
				writeUnaligned32(chipCode, chipCodeIndex, value);
				chipCodeIndex += 4;
			}
		} else {
			getSendPacket(dataAddress).write32(dataIndex, value);
			if (endOfCommand) {
				writeDataEndOfCommand(dataAddress, dataIndex + 4);
			}
		}
	}

	@Override
	protected void startTPC(int tpc) {
		super.startTPC(tpc);

		switch (getTPCCode()) {
			case MS_TPC_WRITE_IO_DATA:
				// Dmac is waiting for a call to sceDdrFlush(DDR_FLUSH_DMAC)
				MMIOHandlerDdr.getInstance().doFlush(DDR_FLUSH_DMAC);
				break;
			case MS_TPC_SET_CMD:
			case MS_TPC_EX_SET_CMD:
				// Clear any previous call to sceDdrFlush(DDR_FLUSH_DMAC) when starting a new command
				MMIOHandlerDdr.getInstance().clearFlushDone(DDR_FLUSH_DMAC);
				break;
		}
	}

	@Override
	public void sendPacketFromAccessPoint(byte[] buffer, int bufferLength, EtherFrame etherFrame) {
		final int offset = 33;
		bufferLength -= offset;

		if (log.isTraceEnabled()) {
			log.trace(String.format("sendPacketFromAccessPoint bufferLength=0x%X, etherFrame=%s, buffer: %s", bufferLength, etherFrame, Utilities.getMemoryDump(buffer, offset, bufferLength)));
		}

		// Broadcast on all data socket ports
		for (int i = 0; i < maxDataSocketPorts; i++) {
			int broadcastDataPort = dataSocketPortBase + i;
	    	try {
				InetSocketAddress broadcastAddress[] = sceNetInet.getBroadcastInetSocketAddress(broadcastDataPort);
				if (broadcastAddress != null) {
					for (int j = 0; j < broadcastAddress.length; j++) {
						if (log.isTraceEnabled()) {
							log.trace(String.format("sendPacketFromAccessPoint to %s", broadcastAddress[j]));
						}
						DatagramPacket datagramPacket = new DatagramPacket(buffer, offset, bufferLength, broadcastAddress[j]);
						dataSocket.send(datagramPacket);
					}
				}
			} catch (UnknownHostException e) {
				log.error("sendPacketFromAccessPoint", e);
			} catch (IOException e) {
				log.error("sendPacketFromAccessPoint", e);
			}
		}
	}
}
