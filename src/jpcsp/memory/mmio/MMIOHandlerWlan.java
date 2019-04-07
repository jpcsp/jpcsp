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
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.hardware.Wlan.getMacAddress;
import static jpcsp.util.Utilities.alignUp;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.io.IOException;

import jpcsp.Memory;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.sceWlan;
import jpcsp.memory.IntArrayMemory;
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
public class MMIOHandlerWlan extends MMIOHandlerBaseMemoryStick {
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
	private final IntArrayMemory attributesMemory = new IntArrayMemory(new int[0x40 / 4]);
	private final IntArrayMemory packet = new IntArrayMemory(new int[0xC00 / 4]);
	private final TPointer packetPtr = packet.getPointer();
	private static final int DUMMY_ATTRIBUTE_ENTRY = 0x1234;
	private static final int WLAN_REG_RESULT = 0x54;
	private static final int WLAN_REG_OUTPUT_PACKET_SIZE = 0x44;
	private static final int WLAN_REG_RECEIVE_PACKET_SIZE = 0x4A;
	private static final int WLAN_REG_INPUT_PACKET_SIZE = 0x4C;
	private final byte[] chipCode = new byte[0x173FC];
	private int chipCodeIndex;
	private boolean booting;

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
		stream.readIntArrayMemory(attributesMemory);
		stream.readIntArrayMemory(packet);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeBytes(chipCode);
		stream.writeInt(chipCodeIndex);
		stream.writeBoolean(booting);
		stream.writeIntArrayMemory(attributesMemory);
		stream.writeIntArrayMemory(packet);
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

		chipCodeIndex = -1;
		booting = true;
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
			case WLAN_REG_RECEIVE_PACKET_SIZE:
				if (booting) {
					// Finished writing the chip code, return a fixed magic value
					setRegisterValue(WLAN_REG_RECEIVE_PACKET_SIZE, 4, endianSwap32(0x46554755));
					chipCodeIndex = -1;
					booting = false;
				}
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
				value = packet.read16(dataIndex);
				if (endOfCommand) {
					clearResultFlag(0x10);
				}

				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerWlan.readData16 MSPRO_CMD_IN_IO_FIFO, dataAddress=0x%X, dataIndex=0x%X, endOfCommand=%b, value=0x%04X", dataAddress, dataIndex, endOfCommand, value));
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

	private void sendResponse(int size) {
		int cmd = packet.read16(0);
		packet.writeUnsigned16(0, cmd | 0x8000);

		setRegisterValue(WLAN_REG_RECEIVE_PACKET_SIZE, 2, alignUp(size, 7));

		endianSwap32(packet, 0, size);

		addResultFlag(0x10);
	}

	private void processRequest(int size) {
		int cmd = packet.read16(0);
		int bodySize = packet.read16(2);
		int action;
		switch (cmd) {
			case CMD_GET_HW_SPEC:
				if (log.isDebugEnabled()) {
					log.debug(String.format("processRequest CMD_GET_HW_SPEC bodySize=0x%X", bodySize));
				}
				packet.writeUnsigned16(6, 0); // Result code
				packet.writeUnsigned16(8, 0); // Hardware interface version number
				packet.writeUnsigned16(10, 0); // Hardware version number
				packet.writeUnsigned16(12, 0); // Number of WCB
				packet.writeUnsigned16(14, 1); // Maximum number of multicast addresses
				packetPtr.setArray(16, getMacAddress(), MAC_ADDRESS_LENGTH);
				packet.writeUnsigned16(22, 0); // Region code
				packet.writeUnsigned16(24, 0); // Number of antenna used
				writeUnaligned32(packet, 26, 0x01000000); // Firmware release number
				writeUnaligned32(packet, 42, 0x000); // Firmware capability information
				sendResponse(Math.min(size, bodySize + 2));
				break;
			case CMD_802_11_RF_ANTENNA:
				action = packet.read16(8);
				int antennaNumber = packet.read16(10);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processRequest CMD_802_11_RF_ANTENNA bodySize=0x%X, action=0x%X, antennaNumber=0x%X", bodySize, action, antennaNumber));
				}
				if (action == 1 && antennaNumber == 1) {
					packet.writeUnsigned16(6, 0); // Result code
					sendResponse(Math.min(size, bodySize));
				} else if (action == 2 && antennaNumber == 1) {
					packet.writeUnsigned16(6, 0); // Result code
					sendResponse(Math.min(size, bodySize));
				} else {
					log.error(String.format("processRequest CMD_802_11_RF_ANTENNA unimplemented action=0x%X, antennaNumber=0x%X", action, antennaNumber));
				}
				break;
			case CMD_802_11_RADIO_CONTROL:
				action = packet.read16(8);
				int control = packet.read16(10);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processRequest CMD_802_11_RADIO_CONTROL bodySize=0x%X, action=0x%X, control=0x%X", bodySize, action, control));
				}
				if (action == 1 && control == 1) {
					packet.writeUnsigned16(6, 0); // Result code
					sendResponse(Math.min(size, bodySize));
				} else {
					log.error(String.format("processRequest CMD_802_11_RADIO_CONTROL unimplemented action=0x%X, control=0x%X", action, control));
				}
				break;
			case CMD_802_11_RF_TX_POWER:
				action = packet.read16(8);
				int currentPowerLevel = packet.read16(10);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processRequest CMD_802_11_RF_TX_POWER bodySize=0x%X, action=0x%X, currentPowerLevel=0x%X", bodySize, action, currentPowerLevel));
				}
				if (action == 1 && currentPowerLevel == 10) {
					packet.writeUnsigned16(6, 0); // Result code
					packet.write8(12, (byte) 20); // Maximum valid power level
					packet.write8(13, (byte)  0); // Minimum valid power level
					sendResponse(Math.min(size, bodySize));
				} else {
					log.error(String.format("processRequest CMD_802_11_RF_TX_POWER unimplemented action=0x%X, currentPowerLevel=0x%X", action, currentPowerLevel));
				}
				break;
			case CMD_802_11_DATA_RATE:
				action = packet.read16(8);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processRequest CMD_802_11_DATA_RATE bodySize=0x%X, action=0x%X", bodySize, action));
				}
				if (action == 0) { // CMD_ACT_SET_TX_AUTO
					packet.writeUnsigned16(6, 0); // Result code
					packet.write8(12, (byte) 1); // Data rate [1..2]
					packet.write8(12, (byte) 0); // End
					sendResponse(Math.min(size, bodySize));
				} else {
					log.error(String.format("processRequest CMD_802_11_DATA_RATE unimplemented action=0x%X", action));
				}
				break;
			case CMD_802_11_SNMP_MIB:
				action = packet.read16(8);
				int oid = packet.read16(10);
				int oidValueSize = packet.read16(12);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processRequest CMD_802_11_SNMP_MIB bodySize=0x%X, action=0x%X, oid=0x%X, oidValueSize=0x%X, iodValue: %s", bodySize, action, oid, oidValueSize, Utilities.getMemoryDump(packet, 14, size - 14)));
				}
				if (action == 1 && oid == 5 && oidValueSize == 2) { // set RTS Threshold
					if (log.isDebugEnabled()) {
						log.debug(String.format("processRequest CMD_802_11_SNMP_MIB set RTS Threshold=%d", packet.read16(14)));
					}
				} else if (action == 1 && oid == 6 && oidValueSize == 2) { // set Short Retry Limit
					if (log.isDebugEnabled()) {
						log.debug(String.format("processRequest CMD_802_11_SNMP_MIB set Short Retry Limit=%d", packet.read16(14)));
					}
				} else if (action == 1 && oid == 8 && oidValueSize == 2) { // set Fragment Threshold
					if (log.isDebugEnabled()) {
						log.debug(String.format("processRequest CMD_802_11_SNMP_MIB set Fragment Threshold=%d", packet.read16(14)));
					}
				} else {
					log.error(String.format("processRequest CMD_802_11_SNMP_MIB unimplemented action=0x%X, %s", action, Utilities.getMemoryDump(packet, 0, size)));
				}
				packet.writeUnsigned16(6, 0); // Result code
				sendResponse(Math.min(size, bodySize));
				break;
			case CMD_UNKNOWN_007A:
				if (log.isDebugEnabled()) {
					log.debug(String.format("processRequest CMD_UNKNOWN_007A bodySize=0x%X, %s", bodySize, Utilities.getMemoryDump(packet, 0, size)));
				}
				packet.writeUnsigned16(6, 0); // Result code
				sendResponse(Math.min(size, bodySize));
				break;
			case CMD_MAC_CONTROL:
				action = packet.read16(8);
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
					log.debug(String.format("processRequest CMD_MAC_CONTROL bodySize=0x%X, action=0x%X, rxOn=%b, txOn=%b, wepOn=%b, unknown20=%b, unknown40=%b, promiscousOn=%b, multicastOn=%b, enableProtection=%b, enableWMM=%b, wepType104=%b", bodySize, action, rxOn, txOn, wepOn, unknown20, unknown40, promiscousOn, multicastOn, enableProtection, enableWMM, wepType104));
				}
				packet.writeUnsigned16(6, 0); // Result code
				sendResponse(Math.min(size, bodySize));
				break;
			case CMD_802_11_SCAN:
				int bssType = packet.read8(10);
				pspNetMacAddress macAddressFilter = new pspNetMacAddress();
				macAddressFilter.read(packet, 11);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processRequest CMD_802_11_SCAN bodySize=0x%X, bssType=%d, macAddressFilter=%s, unknown=%s", bodySize, bssType, macAddressFilter, Utilities.getMemoryDump(packet, 17, size - 17)));
				}

				packet.writeUnsigned16(6, 0); // Result code
				packet.writeUnsigned16(30, 40); // Scan response buffer size
				packet.write8(32, (byte) 1); // Number of APs in the buffer
				int offset = 33;
				packetPtr.setUnalignedValue16(offset + 0, 40); // Total IE length
				byte[] bssId = new byte[] { 'J', 'p', 'c', 's', 'p', ' ' };
				packetPtr.setArray(offset + 2, bssId, 6);
				packetPtr.setUnalignedValue32(offset + 8, 0x778899AA);
				packetPtr.setUnalignedValue32(offset + 12, 0xBBCCDDEE);
				packetPtr.setUnalignedValue16(offset + 16, 0xFF11);
				packetPtr.setUnalignedValue16(offset + 18, 0x2CFF); // Flags
				int params = offset + 20;
				// element IDs: 0, 1, 2, 3, 5, 6, 48, 50, 221
				packetPtr.setUnsignedValue8(params + 0, 3); // WLAN_EID_DS_PARAMS
				packetPtr.setUnsignedValue8(params + 1, 1); // length
				packetPtr.setUnsignedValue8(params + 2, 1); // Channel number
				params += 3;

				sendResponse(params);
				break;
			default:
				log.error(String.format("processRequest unimplemented cmd=0x%X, size=0x%X, %s", cmd, size, Utilities.getMemoryDump(packet, 0, size)));
				break;
		}
	}

	@Override
	protected void writeData32(int dataAddress, int dataIndex, int value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerWlan.writeData32 dataAddress=0x%X, dataIndex=0x%X, outputPacketSize=0x%X, chipCodeIndex=0x%X, value=0x%08X", dataAddress, dataIndex, getWlanOutputPacketSize(), chipCodeIndex, value));
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
			packet.write32(dataIndex, value);
			int size = dataIndex + 4;
			if (size >= getWlanOutputPacketSize()) {
				endianSwap32(packet, 0, size);
				processRequest(size);
			}
		}
	}
}
