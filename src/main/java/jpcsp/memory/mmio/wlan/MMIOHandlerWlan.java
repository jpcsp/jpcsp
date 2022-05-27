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
package jpcsp.memory.mmio.wlan;

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_WLAN_INTR;
import static jpcsp.HLE.kernel.types.SceNetWlanMessage.WLAN_PROTOCOL_SUBTYPE_CONTROL;
import static jpcsp.HLE.kernel.types.SceNetWlanMessage.WLAN_PROTOCOL_SUBTYPE_DATA;
import static jpcsp.HLE.kernel.types.SceNetWlanMessage.WLAN_PROTOCOL_TYPE_SONY;
import static jpcsp.HLE.kernel.types.pspNetMacAddress.isAnyMacAddress;
import static jpcsp.HLE.kernel.types.pspNetMacAddress.isMulticastMacAddress;
import static jpcsp.HLE.kernel.types.pspNetMacAddress.isMyMacAddress;
import static jpcsp.HLE.modules.sceNet.convertMacAddressToString;
import static jpcsp.HLE.modules.sceNetAdhocctl.PSP_ADHOCCTL_MODE_GAMEMODE;
import static jpcsp.HLE.modules.sceNetAdhocctl.PSP_ADHOCCTL_MODE_NONE;
import static jpcsp.HLE.modules.sceNetAdhocctl.PSP_ADHOCCTL_MODE_NORMAL;
import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.hardware.Wlan.getMacAddress;
import static jpcsp.network.jpcsp.JpcspWlanAdapter.WLAN_CMD_DATA;
import static jpcsp.scheduler.Scheduler.getNow;
import static jpcsp.util.Utilities.alignUp;
import static jpcsp.util.Utilities.endianSwap16;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.setFlag;
import static jpcsp.util.Utilities.writeUnaligned16;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.sceNet;
import jpcsp.HLE.modules.sceNetAdhocctl;
import jpcsp.HLE.modules.sceWlan;
import jpcsp.hardware.Wlan;
import jpcsp.memory.ByteArrayMemory;
import jpcsp.memory.IntArrayMemory;
import jpcsp.memory.mmio.MMIOHandlerBaseMemoryStick;
import jpcsp.memory.mmio.MMIOHandlerDdr;
import jpcsp.network.IWlanAdapter;
import jpcsp.network.WlanAdapterFactory;
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
	public static final int RX_RATE_11_MBPS                   = 0x3;
	//
	public static final int BSS_TYPE_INFRASTRUCTURE = 1;
	public static final int BSS_TYPE_ADHOC = 2;
	//
	public static final int DATA_ADDRESS_CMD = 0x100;
	public static final int DATA_ADDRESS_PACKET = 0x102;
	//
	public static final int WLAN_DDR_FLUSH = 0;
	//
	private static MMIOHandlerWlan instance;
	public static final int BASE_ADDRESS = 0xBD300000;
	public static final boolean useWlanFirmwareEmulation = false;
	private final IntArrayMemory attributesMemory = new IntArrayMemory(new int[0x40 / 4]);
	private final IntArrayMemory commandPacket = new IntArrayMemory(new int[0xC00 / 4]);
	private final TPointer commandPacketPtr = commandPacket.getPointer();
	private final IntArrayMemory sendDataPacket = new IntArrayMemory(new int[0xC00 / 4]);
	private final TPointer sendDataPacketPtr = sendDataPacket.getPointer();
	private final IntArrayMemory receiveDataPacket = new IntArrayMemory(new int[0xC00 / 4]);
	private final TPointer receiveDataPacketPtr = receiveDataPacket.getPointer();
	private static final int DUMMY_ATTRIBUTE_ENTRY = 0x1234;
	public static final int WLAN_REG_RECEIVED_PACKET_LENGTH = 0x40; // 16 bits
	public static final int WLAN_REG_OUTPUT_PACKET_SIZE = 0x44; // 16 bits
	public static final int WLAN_REG_CMD_RESPONSE_PACKET_LENGTH = 0x4A;
	public static final int WLAN_REG_INPUT_PACKET_SIZE = 0x4C;
	public static final int WLAN_REG_EVENT_INFORMATION = 0x4E; // 32 bits
	public static final int WLAN_REG_RESULT = 0x54;
	//
	public static final int WLAN_RESULT_READY_TO_SEND_DATA = 0x01;
	public static final int WLAN_RESULT_DATA_PACKET_RECEIVED = 0x02;
	public static final int WLAN_RESULT_READY_TO_SEND_COMMAND = 0x04;
	public static final int WLAN_RESULT_EVENT_RECEIVED = 0x08;
	public static final int WLAN_RESULT_COMMAND_RESPONSE_AVAILABLE = 0x10;
	public static final int WLAN_RESULT_UNKNOWN_80 = 0x80;
	//
	public static final int WLAN_EVENT_UNKNOWN_40 = 0x40;
	public static final int WLAN_EVENT_GAMEMODE_SEND_MASTER = 0x80;
	//
	private final byte[] firmwareBootCode = new byte[0x85C + 0xC];
	private final byte[] firmwareData = new byte[0x173FC];
	private int firmwareBootCodeIndex;
	private int firmwareDataIndex;
	private boolean booting;
	private boolean adhocStarted;
	private boolean adhocJoined;
	private boolean gameMode;
	private long gameModeEventSendMaster;
	private long gameModeEvent_40;
	private String adhocSsid;
    private AccessPoint accessPoint;
    private IWlanAdapter wlanAdapter;
    private final byte[] otherMacAddress = new byte[MAC_ADDRESS_LENGTH];
    private final byte[] gameModeGroupAddress = new byte[MAC_ADDRESS_LENGTH];
    private final byte[] gameModeMasterAddress = new byte[MAC_ADDRESS_LENGTH];
	private MMIOHandlerWlanFirmware handlerWlanFirmware;

    public static MMIOHandlerWlan getInstance() {
    	if (instance == null) {
    		instance = new MMIOHandlerWlan(BASE_ADDRESS);
    	}

    	return instance;
    }

    private MMIOHandlerWlan(int baseAddress) {
		super(baseAddress);

		log = sceWlan.log;

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		stream.readBytes(firmwareBootCode);
		stream.readBytes(firmwareData);
		firmwareDataIndex = stream.readInt();
		firmwareBootCodeIndex = stream.readInt();
		booting = stream.readBoolean();
		adhocStarted = stream.readBoolean();
		adhocJoined = stream.readBoolean();
		adhocSsid = stream.readString();
		attributesMemory.read(stream);
		commandPacket.read(stream);
		sendDataPacket.read(stream);
		receiveDataPacket.read(stream);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeBytes(firmwareBootCode);
		stream.writeBytes(firmwareData);
		stream.writeInt(firmwareDataIndex);
		stream.writeInt(firmwareBootCodeIndex);
		stream.writeBoolean(booting);
		stream.writeBoolean(adhocStarted);
		stream.writeBoolean(adhocJoined);
		stream.writeString(adhocSsid);
		attributesMemory.write(stream);
		commandPacket.write(stream);
		sendDataPacket.write(stream);
		receiveDataPacket.write(stream);
		super.write(stream);
	}

	private static String getWlanCommandName(int cmd) {
		switch (cmd) {
			case CMD_GET_HW_SPEC                   : return "CMD_GET_HW_SPEC";
			case CMD_EEPROM_UPDATE                 : return "CMD_EEPROM_UPDATE";
			case CMD_802_11_RESET                  : return "CMD_802_11_RESET";
			case CMD_802_11_SCAN                   : return "CMD_802_11_SCAN";
			case CMD_802_11_GET_LOG                : return "CMD_802_11_GET_LOG";
			case CMD_MAC_MULTICAST_ADR             : return "CMD_MAC_MULTICAST_ADR";
			case CMD_802_11_AUTHENTICATE           : return "CMD_802_11_AUTHENTICATE";
			case CMD_802_11_SET_WEP                : return "CMD_802_11_SET_WEP";
			case CMD_802_11_GET_STAT               : return "CMD_802_11_GET_STAT";
			case CMD_802_3_GET_STAT                : return "CMD_802_3_GET_STAT";
			case CMD_802_11_SNMP_MIB               : return "CMD_802_11_SNMP_MIB";
			case CMD_MAC_REG_MAP                   : return "CMD_MAC_REG_MAP";
			case CMD_BBP_REG_MAP                   : return "CMD_BBP_REG_MAP";
			case CMD_MAC_REG_ACCESS                : return "CMD_MAC_REG_ACCESS";
			case CMD_BBP_REG_ACCESS                : return "CMD_BBP_REG_ACCESS";
			case CMD_RF_REG_ACCESS                 : return "CMD_RF_REG_ACCESS";
			case CMD_802_11_RADIO_CONTROL          : return "CMD_802_11_RADIO_CONTROL";
			case CMD_802_11_RF_CHANNEL             : return "CMD_802_11_RF_CHANNEL";
			case CMD_802_11_RF_TX_POWER            : return "CMD_802_11_RF_TX_POWER";
			case CMD_802_11_RSSI                   : return "CMD_802_11_RSSI";
			case CMD_802_11_RF_ANTENNA             : return "CMD_802_11_RF_ANTENNA";
			case CMD_802_11_PS_MODE                : return "CMD_802_11_PS_MODE";
			case CMD_802_11_DATA_RATE              : return "CMD_802_11_DATA_RATE";
			case CMD_RF_REG_MAP                    : return "CMD_RF_REG_MAP";
			case CMD_802_11_DEAUTHENTICATE         : return "CMD_802_11_DEAUTHENTICATE";
			case CMD_802_11_REASSOCIATE            : return "CMD_802_11_REASSOCIATE";
			case CMD_MAC_CONTROL                   : return "CMD_MAC_CONTROL";
			case CMD_802_11_AD_HOC_START           : return "CMD_802_11_AD_HOC_START";
			case CMD_802_11_AD_HOC_JOIN            : return "CMD_802_11_AD_HOC_JOIN";
			case CMD_802_11_QUERY_TKIP_REPLY_CNTRS : return "CMD_802_11_QUERY_TKIP_REPLY_CNTRS";
			case CMD_802_11_ENABLE_RSN             : return "CMD_802_11_ENABLE_RSN";
			case CMD_802_11_SET_AFC                : return "CMD_802_11_SET_AFC";
			case CMD_802_11_GET_AFC                : return "CMD_802_11_GET_AFC";
			case CMD_802_11_DEEP_SLEEP             : return "CMD_802_11_DEEP_SLEEP";
			case CMD_802_11_AD_HOC_STOP            : return "CMD_802_11_AD_HOC_STOP";
			case CMD_802_11_HOST_SLEEP_CFG         : return "CMD_802_11_HOST_SLEEP_CFG";
			case CMD_802_11_WAKEUP_CONFIRM         : return "CMD_802_11_WAKEUP_CONFIRM";
			case CMD_802_11_HOST_SLEEP_ACTIVATE    : return "CMD_802_11_HOST_SLEEP_ACTIVATE";
			case CMD_802_11_BEACON_STOP            : return "CMD_802_11_BEACON_STOP";
			case CMD_802_11_MAC_ADDRESS            : return "CMD_802_11_MAC_ADDRESS";
			case CMD_802_11_LED_GPIO_CTRL          : return "CMD_802_11_LED_GPIO_CTRL";
			case CMD_802_11_ASSOCIATE              : return "CMD_802_11_ASSOCIATE";
			case CMD_802_11_BAND_CONFIG            : return "CMD_802_11_BAND_CONFIG";
			case CMD_802_11_EEPROM_ACCESS          : return "CMD_802_11_EEPROM_ACCESS";
			case CMD_GSPI_BUS_CONFIG               : return "CMD_GSPI_BUS_CONFIG";
			case CMD_802_11D_DOMAIN_INFO           : return "CMD_802_11D_DOMAIN_INFO";
			case CMD_802_11_KEY_MATERIAL           : return "CMD_802_11_KEY_MATERIAL";
			case CMD_802_11_SLEEP_PARAMS           : return "CMD_802_11_SLEEP_PARAMS";
			case CMD_802_11_INACTIVITY_TIMEOUT     : return "CMD_802_11_INACTIVITY_TIMEOUT";
			case CMD_802_11_SLEEP_PERIOD           : return "CMD_802_11_SLEEP_PERIOD";
			case CMD_802_11_TPC_CFG                : return "CMD_802_11_TPC_CFG";
			case CMD_802_11_PA_CFG                 : return "CMD_802_11_PA_CFG";
			case CMD_802_11_FW_WAKE_METHOD         : return "CMD_802_11_FW_WAKE_METHOD";
			case CMD_802_11_SUBSCRIBE_EVENT        : return "CMD_802_11_SUBSCRIBE_EVENT";
			case CMD_802_11_RATE_ADAPT_RATESET     : return "CMD_802_11_RATE_ADAPT_RATESET";
			case CMD_802_11_TX_RATE_QUERY          : return "CMD_802_11_TX_RATE_QUERY";
			case CMD_GET_TSF                       : return "CMD_GET_TSF";
			case CMD_BT_ACCESS                     : return "CMD_BT_ACCESS";
			case CMD_FWT_ACCESS                    : return "CMD_FWT_ACCESS";
			case CMD_802_11_MONITOR_MODE           : return "CMD_802_11_MONITOR_MODE";
			case CMD_MESH_ACCESS                   : return "CMD_MESH_ACCESS";
			case CMD_MESH_CONFIG_OLD               : return "CMD_MESH_CONFIG_OLD";
			case CMD_MESH_CONFIG                   : return "CMD_MESH_CONFIG";
			case CMD_SET_BOOT2_VER                 : return "CMD_SET_BOOT2_VER";
			case CMD_FUNC_INIT                     : return "CMD_FUNC_INIT";
			case CMD_FUNC_SHUTDOWN                 : return "CMD_FUNC_SHUTDOWN";
			case CMD_802_11_BEACON_CTRL            : return "CMD_802_11_BEACON_CTRL";
		}
		return String.format("CMD_UNKNOWN_%04X", cmd);
	}

	@Override
	public void reset() {
		super.reset();

		// Possible values:
		// 0x0011 0x0001 0x0001
		// 0x0011 0x0001 0x1B18
		// 0x0011 0x0002 0x1B11
		// 0x0011 0x0002 0x0B11
		attributesMemory.writeUnsigned16(0, 0x0011);
		attributesMemory.writeUnsigned16(2, 0x0001);
		attributesMemory.writeUnsigned16(4, 0x0001);

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

		firmwareDataIndex = -1;
		firmwareBootCodeIndex = 0;
		booting = true;
		if (useWlanFirmwareEmulation) {
			handlerWlanFirmware = WlanEmulator.getInstance().getMemory().getHandlerWlanFirmware();
		}
	}

	private void createAccessPoint() {
		if (accessPoint == null) {
			accessPoint = new AccessPoint(this);
		}
	}

	public IWlanAdapter getWlanAdapter() {
		return wlanAdapter;
	}

	private void createNetworkAdapter() {
		if (wlanAdapter == null) {
			wlanAdapter = WlanAdapterFactory.createWlanAdapter();
			try {
				wlanAdapter.start();
			} catch (IOException e) {
				log.error("createNetworkAdapter", e);
			}
		}
	}

	public String getSsid() {
		return adhocSsid;
	}

	private void setSsid(String ssid) {
		this.adhocSsid = ssid;

		if (ssid != null) {
			String productId;
			int productType;
			String groupName;

			Pattern p = Pattern.compile("PSP_([AXS])(.........)_([LG])_(.*)");
			Matcher m = p.matcher(ssid);
			if (m.matches()) {
				switch (m.group(1)) {
					case "A":
						productType = sceNetAdhocctl.PSP_ADHOCCTL_TYPE_COMMERCIAL;
						break;
					case "X":
						productType = sceNetAdhocctl.PSP_ADHOCCTL_TYPE_DEBUG;
						break;
					case "S":
						productType = sceNetAdhocctl.PSP_ADHOCCTL_TYPE_SYSTEM;
						break;
					default:
						log.error(String.format("Unknown product type '%s' in SSID='%s'", m.group(1), ssid));
						return;
				}
				productId = m.group(2);
				gameMode = "G".equals(m.group(3));
				groupName = m.group(4);
			} else {
				productType = sceNetAdhocctl.PSP_ADHOCCTL_TYPE_SYSTEM;
				productId = "000000001";
				gameMode = false;
				groupName = ssid;
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("productId=%s, productType=%d, gameMode=%b, groupName=%s", productId, productType, gameMode, groupName));
			}
			Modules.sceNetAdhocctlModule.setGroupName(groupName, gameMode ? PSP_ADHOCCTL_MODE_GAMEMODE : PSP_ADHOCCTL_MODE_NORMAL);
		} else {
			Modules.sceNetAdhocctlModule.setGroupName(null, PSP_ADHOCCTL_MODE_NONE);
		}
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
		return 0x0070; // Value found by tracing on a real PSP
	}

	private boolean hasPendingResult() {
		return hasResultFlag(WLAN_RESULT_READY_TO_SEND_DATA | WLAN_RESULT_DATA_PACKET_RECEIVED | WLAN_RESULT_READY_TO_SEND_COMMAND | WLAN_RESULT_EVENT_RECEIVED | WLAN_RESULT_COMMAND_RESPONSE_AVAILABLE);
	}

	private void checkResultInterrupt() {
		if (hasPendingResult()) {
			setInterrupt();
		}
	}

	@Override
	protected int getStatus() {
		int status = super.getStatus();

		// The WLAN interrupt handler is expecting to have both
		//     MS_INT_REG_CED and MS_INT_REG_BREQ
		// flags set in the status (i.e. in the lower 4 bits of the status).
		if (hasInterrupt() && (status & 0x000F) == (MS_INT_REG_CED >> 4) && hasPendingResult()) {
			status = setFlag(status, MS_INT_REG_BREQ >> 4);
		}

		return status;
	}

	private void addResultFlag(int flag) {
		if (!hasResultFlag(flag)) {
			registers[WLAN_REG_RESULT] |= flag;
			checkResultInterrupt();
		}
	}

	private void clearResultFlag(int flag) {
		if (hasResultFlag(flag)) {
			registers[WLAN_REG_RESULT] &= ~flag;
			checkResultInterrupt();
		}
	}

	private boolean hasResultFlag(int flag) {
		return hasFlag(registers[WLAN_REG_RESULT], flag);
	}

	private void setCardEvent(int cardEvent) {
		cardEvent &= 0xFF;
		if (hasResultFlag(WLAN_RESULT_EVENT_RECEIVED)) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("setCardEvent ignoring 0x%02X", cardEvent));
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug(String.format("setCardEvent 0x%02X", cardEvent));
			}

			setRegisterValue(WLAN_REG_EVENT_INFORMATION, 4, 0x00000010 | (cardEvent << 8));
			addResultFlag(WLAN_RESULT_EVENT_RECEIVED);
			setInterrupt();
		}
	}

	private int getWlanOutputPacketSize() {
		return getRegisterValue(WLAN_REG_OUTPUT_PACKET_SIZE, 2);
	}

	private void setWlanInputPacketSize(int inputPacketSize) {
		setRegisterValue(WLAN_REG_INPUT_PACKET_SIZE, 2, inputPacketSize);
	}

	public byte[] getGameModeGroupAddress() {
		return gameModeGroupAddress;
	}

	public byte[] getGameModeMasterAddress() {
		return gameModeMasterAddress;
	}

	public boolean isGameModeMaster() {
		// Am I the GameMode master?
		return isMyMacAddress(gameModeMasterAddress);
	}

	private void memcpyToFirmware(int dest, TPointer src, int length) {
		Memory firmwareMemory = handlerWlanFirmware.getMemory();
		if (((src.getAddress() | dest | length) & 0x3) == 0) {
			for (int i = 0; i < length; i += 4) {
				firmwareMemory.write32(dest + i, src.getValue32(i));
			}
		} else {
			for (int i = 0; i < length; i++) {
				firmwareMemory.write8(dest + i, src.getValue8(i));
			}
		}
	}

	private void memcpyToFirmware(int dest, byte[] buffer, int length) {
		memcpyToFirmware(dest, new ByteArrayMemory(buffer).getPointer(), length);
	}

	private void memcpyFromFirmware(TPointer dest, int src, int length) {
		Memory firmwareMemory = handlerWlanFirmware.getMemory();
		if (((dest.getAddress() | src | length) & 0x3) == 0) {
			for (int i = 0; i < length; i += 4) {
				dest.setValue32(i, firmwareMemory.read32(src + i));
			}
		} else {
			for (int i = 0; i < length; i++) {
				dest.setUnsignedValue8(i, firmwareMemory.read8(src + i));
			}
		}
	}

	private void sendGameModeEvents() {
		if (!gameMode) {
			return;
		}

		long now = getNow();
		if (gameModeEvent_40 != 0L) {
			if (now >= gameModeEvent_40) {
				setCardEvent(WLAN_EVENT_UNKNOWN_40);
				gameModeEventSendMaster = now + 1000;
				gameModeEvent_40 = 0L;
			}
		} else if (gameModeEventSendMaster != 0L) {
			if (now >= gameModeEventSendMaster) {
				setCardEvent(WLAN_EVENT_GAMEMODE_SEND_MASTER);
				if (isGameModeMaster() || true) {
					gameModeEvent_40 = now + 26000;
				} else {
					gameModeEventSendMaster = now + 15000;
				}
			}
		}
	}

	private void checkWlanFirmwareResult() {
		if (!hasResultFlag(WLAN_RESULT_COMMAND_RESPONSE_AVAILABLE)) {
			int commandResponseLength = handlerWlanFirmware.getCommandResponseLength();
			if (commandResponseLength > 0) {
				int commandResponseAddr = handlerWlanFirmware.getCommandResponseAddr();

				memcpyFromFirmware(commandPacketPtr, commandResponseAddr, commandResponseLength);
				handlerWlanFirmware.clearCommandResponseLength();

				if (log.isTraceEnabled()) {
					log.trace(String.format("checkWlanFirmwareResult response cmd=0x%X, length=0x%X, resultCode=0x%X: %s", commandPacket.read16(0) & 0x7FFF, commandResponseLength, commandPacket.read16(6), Utilities.getMemoryDump(commandPacket, 0, commandResponseLength)));
				}

				setRegisterValue(WLAN_REG_CMD_RESPONSE_PACKET_LENGTH, 2, alignUp(commandResponseLength, 7));

				endianSwap32(commandPacket, 0, commandResponseLength);

				addResultFlag(WLAN_RESULT_COMMAND_RESPONSE_AVAILABLE);
			}
		}
	}

	@Override
	protected int getRegisterValue(int register) {
		switch (register) {
			case WLAN_REG_CMD_RESPONSE_PACKET_LENGTH:
				if (booting) {
					if (useWlanFirmwareEmulation) {
						if (firmwareDataIndex > 0) {
							handlerWlanFirmware.setData(firmwareData, firmwareDataIndex);
						}
						if (handlerWlanFirmware.getLength() == endianSwap32(0x46554755)) {
							firmwareDataIndex = -1;
							booting = false;
						} else {
							firmwareDataIndex = 0;
						}
					} else {
						// Finished writing the chip code, return a fixed magic value
						setRegisterValue(WLAN_REG_CMD_RESPONSE_PACKET_LENGTH, 4, endianSwap32(0x46554755));
						firmwareDataIndex = -1;
						booting = false;
					}
				}
				if (useWlanFirmwareEmulation && !hasResultFlag(WLAN_RESULT_COMMAND_RESPONSE_AVAILABLE)) {
					setRegisterValue(WLAN_REG_CMD_RESPONSE_PACKET_LENGTH, 4, handlerWlanFirmware.getLength());
				}
				break;
			case WLAN_REG_RESULT:
				receiveMessage();
				sendGameModeEvents();
				if (useWlanFirmwareEmulation) {
					checkWlanFirmwareResult();
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
				clearResultFlag(~value);
				break;
			default:
				super.setRegisterValue(register, value);
				break;
		}

		switch (register) {
			case 0x56:
				// Writing to this register seems to also have the effect of updating the result register
				addResultFlag(WLAN_RESULT_READY_TO_SEND_COMMAND);
				break;
			case 0x5E:
				// Writing to this register seems to also have the effect of updating the result register
				if (hasFlag(value, 0x80)) {
					addResultFlag(WLAN_RESULT_UNKNOWN_80);
				} else {
					clearResultFlag(WLAN_RESULT_UNKNOWN_80);
				}
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
							addResultFlag(WLAN_RESULT_READY_TO_SEND_COMMAND);
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

	private boolean isForMe(byte[] destMacAddress, int destMacAddressOffset, byte[] fromMacAddress, int fromMacAddressOffset) {
		if (isMyMacAddress(fromMacAddress, fromMacAddressOffset)) {
    		// This packet is coming from myself, ignore it
			return false;
		}

		if (isMulticastMacAddress(destMacAddress, destMacAddressOffset)) {
			// If sent to FF:FF:FF:FF:FF:FF, accept it
			if (isAnyMacAddress(destMacAddress, destMacAddressOffset)) {
				return true;
			}

			// If sent to the GameMode group address, accept it
			if (pspNetMacAddress.equals(gameModeGroupAddress, 0, destMacAddress, destMacAddressOffset)) {
				return true;
			}

			// Sent to an unknown multicast address, ignore it
			return false;
		}

		if (isMyMacAddress(destMacAddress, destMacAddressOffset)) {
			// Sent directly to me, accept it
			return true;
		}

		// Sent to an unknown address, ignore it
		return false;
	}

	private void receiveMessage() {
		if (wlanAdapter == null) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("receiveMessage no wlanAdapter"));
			}
			return;
		}

		if (hasResultFlag(WLAN_RESULT_DATA_PACKET_RECEIVED)) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("receiveMessage data packet already received"));
			}
			return;
		}

		byte[] receivedMessage = new byte[10000];
		int receivedMessageLength;
		try {
			receivedMessageLength = wlanAdapter.receiveWlanPacket(receivedMessage, 0, receivedMessage.length);
		} catch (IOException e) {
			log.error("receiveMessage", e);
			return;
		}

		if (receivedMessageLength < 0) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("receiveMessage no message available"));
			}
			return;
		}

		if (receivedMessageLength < 12 || !isForMe(receivedMessage, 0, receivedMessage, MAC_ADDRESS_LENGTH)) {
			if (log.isDebugEnabled()) {
				if (log.isTraceEnabled()) {
					log.trace(String.format("Received message not for me: %s", Utilities.getMemoryDump(receivedMessage, 0, receivedMessageLength)));
				} else {
					log.debug(String.format("Received message not for me: destMacAddress=%s, sourceMacAddress=%s", pspNetMacAddress.toString(receivedMessage, 0), pspNetMacAddress.toString(receivedMessage, MAC_ADDRESS_LENGTH)));
				}
			}
			return;
		}

		if (log.isDebugEnabled()) {
			if (receivedMessageLength >= 18 && endianSwap16(readUnaligned16(receivedMessage, 12)) == WLAN_PROTOCOL_TYPE_SONY) {
				int subtype = endianSwap16(readUnaligned16(receivedMessage, 14));
				if (subtype == WLAN_PROTOCOL_SUBTYPE_CONTROL) {
					int controlType = Utilities.read8(receivedMessage, 17);
					log.debug(String.format("Receiving Control packet controlType=%d", controlType));
				} else if (subtype == WLAN_PROTOCOL_SUBTYPE_DATA) {
					log.debug(String.format("Receiving Data packet"));
				}
			}
		}
		if (Utilities.memcmp(receivedMessage, 6, Wlan.getMacAddress(), 0, MAC_ADDRESS_LENGTH) != 0) {
			System.arraycopy(receivedMessage, 6, otherMacAddress, 0, MAC_ADDRESS_LENGTH);
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("receiveMessage received message: %s", Utilities.getMemoryDump(receivedMessage, 0, receivedMessageLength)));
		}

		int totalDataPacketLength;
		if (pspNetMacAddress.equals(receivedMessage, gameModeGroupAddress)) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("Identified packet sent to GameMode Group Address %s", convertMacAddressToString(gameModeGroupAddress)));
			}

			totalDataPacketLength = receivedMessageLength + 28;
			receiveDataPacket.writeUnsigned16(0, 0x0503); // rxStatus
			receiveDataPacket.write8(2, (byte) 0); // SNR
			receiveDataPacket.write8(3, (byte) 0); // rxControl
			receiveDataPacket.writeUnsigned16(4, receivedMessageLength + 12); // rxPacketLength
			receiveDataPacketPtr.clear(6, 10);
			receiveDataPacket.writeUnsigned16(16, receivedMessageLength + 8); // Must match totalDataPacketLength - 20
			receiveDataPacket.writeUnsigned16(18, 1); // Must be 1
			receiveDataPacketPtr.setArray(20, receivedMessage, 0, MAC_ADDRESS_LENGTH * 2); // destMacAddress & srcMacAddress
			receiveDataPacket.writeUnsigned16(32, 0x0A01);
			receiveDataPacket.writeUnsigned16(34, 0xAAAA);
			receiveDataPacket.write32(36, 3);
			receiveDataPacketPtr.setArray(40, receivedMessage, 12, receivedMessageLength - 12);

			if (log.isTraceEnabled()) {
				log.trace(String.format("processReceiveDataPacket GameMode totalDataPacketLength=0x%X, data:%s", totalDataPacketLength, Utilities.getMemoryDump(receiveDataPacket, 0, totalDataPacketLength)));
			}
		} else {
			final int rxPacketLocation = 24; // Needs to be 24 and not 20 (which would be sufficient to be past the header)
			totalDataPacketLength = rxPacketLocation + receivedMessageLength;
			int rxStatus = 0;
			int signalToNoiseRatio = 0;
			int rxControl = 0;
			int rxPacketLength = receivedMessageLength;
			int noiseFloor = 0;
			int rxRate = RX_RATE_11_MBPS;
			int reserved1 = 0;
			int priority = 0;
			int reserved2 = 0;
			int reserved3 = 0;
			receiveDataPacket.writeUnsigned16(0, rxStatus); // rxStatus
			receiveDataPacket.write8(2, (byte) signalToNoiseRatio); // SNR
			receiveDataPacket.write8(3, (byte) rxControl); // rxControl
			receiveDataPacket.writeUnsigned16(4, rxPacketLength); // rxPacketLength
			receiveDataPacket.write8(6, (byte) noiseFloor); // NF
			receiveDataPacket.write8(7, (byte) rxRate); // rxRate
			receiveDataPacket.write32(8, rxPacketLocation); // rxPacketLocation
			receiveDataPacket.write32(12, reserved1); // reserved
			receiveDataPacket.write8(16, (byte) priority); // priority
			receiveDataPacket.write8(17, (byte) reserved2); // reserved
			receiveDataPacket.writeUnsigned16(18, reserved3); // reserved
			receiveDataPacketPtr.clear(20, rxPacketLocation - 20);
			receiveDataPacketPtr.setArray(rxPacketLocation, receivedMessage, 0, receivedMessageLength);

			// The PSP is moving 12 bytes from the offset 16 to the offset 24:
			//     memmove(addr + 24, addr + 16, 12)
			// This doesn't really make sense as it overwrites the source and destination
			// MAC addresses of the EtherFrame. Maybe due to a bug in the Wlan card firmware?
			// Anyway, we have to mimic this behavior.
			receiveDataPacketPtr.setArray(16, receivedMessage, 0, 12);

			if (log.isTraceEnabled()) {
				log.trace(String.format("processReceiveDataPacket totalDataPacketLength=0x%X, rxStatus=0x%X, SNR=0x%X, rxControl=0x%X, rxPacketLength=0x%X, NF=0x%X, rxRate=0x%X, rxPacketLocation=0x%X, reserved1=0x%X, priority=0x%X, reserved2=0x%X, reserved3=0x%X, data:%s", totalDataPacketLength, rxStatus, signalToNoiseRatio, rxControl, rxPacketLength, noiseFloor, rxRate, rxPacketLocation, reserved1, priority, reserved2, reserved3, Utilities.getMemoryDump(receiveDataPacket, rxPacketLocation, rxPacketLength)));
			}
		}

		processReceiveDataPacket(totalDataPacketLength);
	}

	private void sendAdhocDataPacket(byte[] buffer, int bufferLength) {
		if (log.isDebugEnabled()) {
			if (bufferLength >= 18 && endianSwap16(readUnaligned16(buffer, 12)) == WLAN_PROTOCOL_TYPE_SONY) {
				int subtype = endianSwap16(readUnaligned16(buffer, 14));
				if (subtype == WLAN_PROTOCOL_SUBTYPE_CONTROL) {
					int controlType = Utilities.read8(buffer, 17);
					log.debug(String.format("Sending Control packet controlType=%d", controlType));
				} else if (subtype == WLAN_PROTOCOL_SUBTYPE_DATA) {
					log.debug(String.format("Sending Data packet"));
				}
			}
		}

		if (log.isTraceEnabled()) {
    		log.trace(String.format("sendAdhocDataPacket %s", Utilities.getMemoryDump(buffer, 0, bufferLength)));
		}

		try {
			wlanAdapter.sendWlanPacket(buffer, 0, bufferLength);
		} catch (IOException e) {
			log.error("sendAdhocDataPacket", e);
		}
	}

	private void sendDataPacketToAccessPoint(byte[] txPacket, int txPacketLength) {
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
    	if (wlanAdapter != null) {
	    	try {
	        	wlanAdapter.sendAccessPointPacket(buffer, 0, bufferLength, null);
			} catch (IOException e) {
				log.error("sendDataPacketToAccessPoint", e);
			}
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
		if (adhocStarted || adhocJoined) {
			sendAdhocDataPacket(txPacket, txPacketLength);
		} else {
			sendDataPacketToAccessPoint(txPacket, txPacketLength);
		}

		addResultFlag(WLAN_RESULT_READY_TO_SEND_DATA);
	}

	private void processReceiveDataPacket(int size) {
		setRegisterValue(WLAN_REG_RECEIVED_PACKET_LENGTH, 2, size);

		endianSwap32(receiveDataPacket, 0, size);

		addResultFlag(WLAN_RESULT_DATA_PACKET_RECEIVED);
	}

	private void processCommandPacket(int size) {
		int cmd = commandPacket.read16(0);
		int bodySize = commandPacket.read16(2);
		int sequenceNumber = commandPacket.read16(4);
		if (log.isTraceEnabled()) {
			log.trace(String.format("processCommandPacket cmd=0x%X(%s), bodySize=0x%X, sequenceNumber=0x%X, %s", cmd, getWlanCommandName(cmd), bodySize, sequenceNumber, Utilities.getMemoryDump(commandPacket, 0, size)));
		}

		if (useWlanFirmwareEmulation) {
if (cmd == CMD_GET_HW_SPEC) {
	handlerWlanFirmware.setInterrupt(MMIOHandlerWlanFirmware.INTERRUPT_UNKNOWN_10);
	handlerWlanFirmware.setUnknownA510Bit(21);
}
if (cmd == CMD_802_11_SCAN) {
	String ssid = commandPacketPtr.getStringNZ(17, 32);
	handlerWlanFirmware.scanMemory(ssid);
	int bit = 15;
//	handlerWlanFirmware.setUnknownA510(0x6F22C8BF);
	// Possible values unknownA510: 0x6F22C8BF 
	// Bits: untested: 0, 1, 2, 3, 4, 5, 7, 11, 14, 17, 21, 24, 25, 27, 29, 30
	//       tested: 15, 26
	if (bit == 26) {
		final int frameOffset = 0x10;
		final int frameMaxSize = 0x1C0;
		final int length = frameOffset + frameMaxSize;
		final byte[] buffer = new byte[length];
		writeUnaligned16(buffer, frameOffset, 0x124);
		int subtype = 12; // [0..12] 1, 3, 5, 8, 9, 11: no effect, 10, 12: some effect
		writeUnaligned16(buffer, frameOffset + 0x2, subtype << 4);
		if (subtype == 10 || subtype == 12) {
			System.arraycopy(handlerWlanFirmware.getBSSID(), 0, buffer, frameOffset + 0xC, MAC_ADDRESS_LENGTH);
			int unknown = 0x1234;
			writeUnaligned16(buffer, frameOffset + 0x20, unknown);
		}
		memcpyToFirmware(handlerWlanFirmware.getBuffer5().addr, buffer, length);
	}
	if (bit == 15) {
		WlanBufferInfo bufferA000 = handlerWlanFirmware.getBufferA000();
		int addr = bufferA000.addr;
		int addr2 = addr + 0x100;
		int addr3 = addr + 0x200;
		handlerWlanFirmware.getMemory().write32(bufferA000.addr | (bufferA000.readIndex & 0xFF) | 0xD00, addr);
		handlerWlanFirmware.getMemory().write32(addr + 0x24, addr2);
		handlerWlanFirmware.getMemory().write8(addr + 0xA, (byte) 0x12);
		handlerWlanFirmware.getMemory().write16(addr + 0x2, (short) 0x3456);
		handlerWlanFirmware.getMemory().write16(addr + 0x1C, (short) 0x789A);
		handlerWlanFirmware.getMemory().write32(addr2 + 0x38, addr3);
		int frameType = 0; // 0-management, 1-control, 2-data
		int frameSubtype = 4; // 4-Probe request
		int frameControlField = ((frameType & 0x3) << 2) | ((frameSubtype & 0xF) << 4);
		handlerWlanFirmware.getMemory().write16(addr3 + 0x2, (short) frameControlField);
		bufferA000.incrementWriteIndex();
	}
	handlerWlanFirmware.setUnknownA510Bit(bit);
	handlerWlanFirmware.setInterrupt(MMIOHandlerWlanFirmware.INTERRUPT_UNKNOWN_10);
}
			memcpyToFirmware(handlerWlanFirmware.getAddr(), commandPacketPtr, bodySize);
			handlerWlanFirmware.setInterrupt(MMIOHandlerWlanFirmware.INTERRUPT_UNKNOWN_8);
			return;
		}

		int resultCode = 0;
		int responseSize = Math.min(size, bodySize);
		int action;
		String ssid;
		int bssType;
		byte[] ibss;
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
			case CMD_MAC_CONTROL: {
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
			}
			case CMD_802_11_SCAN:
				bssType = commandPacket.read8(10);
				pspNetMacAddress macAddressFilter = new pspNetMacAddress();
				macAddressFilter.read(commandPacket, 11);
				ssid = null;
				int[] channels = null;
				if (bssType == BSS_TYPE_ADHOC) {
					ssid = commandPacketPtr.getStringNZ(17, 32);
					for (int i = 0; i < 14; i++) {
						int channel = commandPacketPtr.getUnsignedValue8(52 + i);
						if (channel == 0) {
							break;
						}
						channels = Utilities.add(channels, channel);
					}
				}
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_SCAN bodySize=0x%X, bssType=0x%X, macAddressFilter=%s, channels=%s, unknown=%s", bodySize, bssType, macAddressFilter, channels, Utilities.getMemoryDump(commandPacket, 17, size - 17)));
				}

				commandPacketPtr.clear(11, 21); // Clear the request MAC address and SSID

				int count = 0;
				List<sceNetAdhocctl.AdhocctlNetwork> networks = null;
				List<sceNetAdhocctl.AdhocctlPeer> peers = null;
				switch (bssType) {
					case BSS_TYPE_INFRASTRUCTURE:
						count = 2;
						break;
					case BSS_TYPE_ADHOC:
						createNetworkAdapter();

						if (!adhocStarted && !adhocJoined) {
							try {
								wlanAdapter.wlanScan(ssid, channels);
							} catch (IOException e) {
								log.error("CMD_802_11_SCAN", e);
							}
							networks = Modules.sceNetAdhocctlModule.getNetworks();
							count = networks.size();
							if (!pspNetMacAddress.isEmptyMacAddress(otherMacAddress)) {
								count++; // Add 1 to include other PSP MAC address
							}
						} else {
							peers = Modules.sceNetAdhocctlModule.getPeers();
							count = peers.size();
//							count++; // Add 1 to include myself
						}
						break;
				}

				commandPacketPtr.setUnsignedValue8(32, count); // Number of APs in the buffer
				int offset = 33;

				final int rssi = 40; // RSSI - Received Signal Strength Indication, value range [0..40] for 1% to 100%
				final int channel = 1;
				ibss = null;
				for (int n = 0; n < count; n++) {
					commandPacketPtr.setValue8(14 + n, (byte) rssi);

					int capabilities = 0x0020; // Flags (WLAN_CAPABILITY_SHORT_PREAMBLE)
					switch (bssType) {
						case BSS_TYPE_INFRASTRUCTURE:
							capabilities |= 0x0001; // WLAN_CAPABILITY_BSS
							ibss = String.format("Jpcsp%d", n).getBytes();
							ssid = String.format("Jpcsp SSID %d", n + 1);
							break;
						case BSS_TYPE_ADHOC:
							capabilities |= 0x0002; // WLAN_CAPABILITY_IBSS
							if (networks != null) {
								if (n >= networks.size()) {
									// Return other PSP MAC address
									ibss = otherMacAddress;
									if (log.isDebugEnabled()) {
										log.debug(String.format("processCommandPacket CMD_802_11_SCAN returning other PSP MAC address: %s", sceNet.convertMacAddressToString(ibss)));
									}
								} else {
									if (log.isDebugEnabled()) {
										log.debug(String.format("processCommandPacket CMD_802_11_SCAN returning network#%d: %s", n, networks.get(n)));
									}
									ibss = networks.get(n).ibss;
									ssid = networks.get(n).ssid;
								}
							} else {
								if (n >= peers.size()) {
									// Return myself
									ibss = getMacAddress();
									if (log.isDebugEnabled()) {
										log.debug(String.format("processCommandPacket CMD_802_11_SCAN returning myself: %s", sceNet.convertMacAddressToString(ibss)));
									}
								} else {
									if (log.isDebugEnabled()) {
										log.debug(String.format("processCommandPacket CMD_802_11_SCAN returning peer#%d: %s", n, peers.get(n)));
									}
									ibss = peers.get(n).macAddress;
								}
							}
							break;
					}

					commandPacketPtr.setArray(offset + 2, ibss, 6);
					long packetTimestamp = 0x0L;
					commandPacketPtr.setUnalignedValue64(offset + 8, packetTimestamp);
					int beaconInterval = 100; // Need to be != 0
					commandPacketPtr.setUnalignedValue16(offset + 16, beaconInterval);
					commandPacketPtr.setUnalignedValue16(offset + 18, capabilities);
					int params = offset + 20;

					if (bssType == BSS_TYPE_INFRASTRUCTURE) {
						commandPacketPtr.setUnsignedValue8(params + 0, 2); // WLAN_EID_FH_PARAMS
						commandPacketPtr.setUnsignedValue8(params + 1, 5); // length
						commandPacketPtr.setUnalignedValue16(params + 2, 0); // dwell time
						commandPacketPtr.setUnsignedValue8(params + 4, 0); // hop set
						commandPacketPtr.setUnsignedValue8(params + 5, 0); // hop pattern
						commandPacketPtr.setUnsignedValue8(params + 6, 0); // hop index
						params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);
					}

					commandPacketPtr.setUnsignedValue8(params + 0, 3); // WLAN_EID_DS_PARAMS
					commandPacketPtr.setUnsignedValue8(params + 1, 1); // length
					commandPacketPtr.setUnsignedValue8(params + 2, channel); // Channel number
					params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);

					int ssidLength = ssid == null ? 0 : ssid.length();
					commandPacketPtr.setUnsignedValue8(params + 0, 0); // WLAN_EID_SSID
					commandPacketPtr.setUnsignedValue8(params + 1, ssidLength); // length
					commandPacketPtr.setStringNZ(params + 2, ssidLength, ssid);
					params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);

					int[] rates = new int[] { 0x82, 0x84, 0x0B, 0x16 }; // Supported rates: 1MB, 2MB, 5MB, 11MB
					commandPacketPtr.setUnsignedValue8(params + 0, 1); // WLAN_EID_SUPP_RATES
					commandPacketPtr.setUnsignedValue8(params + 1, rates.length); // length
					for (int i = 0; i < rates.length; i++) {
						commandPacketPtr.setUnsignedValue8(params + 2 + i, rates[i]);
					}
					params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);

					if (bssType == BSS_TYPE_INFRASTRUCTURE) {
						int[] extendedRates = new int[] { 0x0C, 0x12 }; // Supported rates: 6MB, 9MB
						commandPacketPtr.setUnsignedValue8(params + 0, 50); // WLAN_EID_EXT_SUPP_RATES
						commandPacketPtr.setUnsignedValue8(params + 1, extendedRates.length); // length
						for (int i = 0; i < extendedRates.length; i++) {
							commandPacketPtr.setUnsignedValue8(params + 2 + i, extendedRates[i]);
						}
						params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);
					}

					commandPacketPtr.setUnsignedValue8(params + 0, 6); // WLAN_EID_IBSS_PARAMS
					commandPacketPtr.setUnsignedValue8(params + 1, 2); // length
					commandPacketPtr.setUnalignedValue16(params + 2, 0); // ATIM window
					params += 2 + commandPacketPtr.getUnsignedValue8(params + 1);

					if (bssType == BSS_TYPE_INFRASTRUCTURE) {
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
					}

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
			case CMD_UNKNOWN_007C: {
				int unknown8 = commandPacket.read16(8);
				int unknown10 = commandPacket.read16(10);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_UNKNOWN_007C bodySize=0x%X, unknown8=0x%X, unknown10=0x%X", bodySize, unknown8, unknown10));
				}

				int wlanCardVersion = attributesMemory.read16(4);
				if (wlanCardVersion != 0x1B11 && wlanCardVersion != 0x1B18) {
					// This command is only sent for these two versions
					log.error(String.format("processCommandPacket CMD_UNKNOWN_007C unimplemented for wlanCardVersion 0x%04X: %s", wlanCardVersion, Utilities.getMemoryDump(commandPacket, 0, size)));
					resultCode = -1;
				} else if (bodySize == 0xC && unknown8 == 1 && unknown10 == 1) {
					// Executed just before a CMD_802_11_AD_HOC_START command
				} else if (bodySize == 0xC && unknown8 == 1 && unknown10 == 0) {
					// Executed just before a CMD_802_11_AD_HOC_STOP command
				} else {
					log.error(String.format("processCommandPacket CMD_UNKNOWN_007C unimplemented %s", Utilities.getMemoryDump(commandPacket, 0, size)));
					resultCode = -1;
				}
				break;
			}
			case CMD_802_11_AD_HOC_START:
				ssid = commandPacketPtr.getStringNZ(8, 32);
				bssType = commandPacketPtr.getUnsignedValue8(40);
				int beaconPeriod = commandPacketPtr.getUnalignedValue16(41);
				int atimWindow = commandPacketPtr.getUnsignedValue8(43);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_AD_HOC_START bodySize=0x%X, ssid='%s', bssType=0x%X, beaconPeriod=0x%X, ATIM Window=0x%X", bodySize, ssid, bssType, beaconPeriod, atimWindow));
				}

				commandPacketPtr.setArray(8, getMacAddress());

				setSsid(ssid);
				//wlanAdapter.sceNetAdhocctlConnect();
				adhocStarted = true;
				break;
			case CMD_802_11_AD_HOC_STOP:
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_AD_HOC_STOP bodySize=0x%X", bodySize));
				}
				adhocStarted = false;
				adhocJoined = false;
				setSsid(null);
				if (wlanAdapter != null) {
					try {
						wlanAdapter.stop();
					} catch (IOException e) {
						log.error("CMD_802_11_AD_HOC_STOP", e);
					}
					wlanAdapter = null;
				}

				gameMode = false;
				gameModeEvent_40 = 0L;
				gameModeEventSendMaster = 0L;
				break;
			case CMD_802_11_AD_HOC_JOIN:
				ibss = commandPacketPtr.getArray8(8, 6);
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
					log.debug(String.format("processCommandPacket CMD_802_11_AD_HOC_JOIN bodySize=0x%X, bssid=%s, ssid='%s', bssType=0x%X, beaconPeriod=0x%X, dtimPeriod=0x%X, timestamp=0x%X, startTimestamp=0x%X, capabilities=0x%X, failTimeout=0x%X, probeDelay=0x%X, dataRates=%s", bodySize, convertMacAddressToString(ibss), ssid, bssType, beaconPeriod, dtimPeriod, timestamp, startTimestamp, capabilities, failTimeout, probeDelay, dataRatesString));
				}

				setSsid(ssid);
				adhocJoined = true;
				break;
			case CMD_802_11_AUTHENTICATE:
				peerMacAddress = new pspNetMacAddress();
				peerMacAddress.read(commandPacket, 8);
				int authType = commandPacket.read8(14);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_AUTHENTICATE bodySize=0x%X, peerMacAddress=%s, authType=0x%X", bodySize, peerMacAddress, authType));
				}
				break;
			case CMD_UNKNOWN_0012:
				peerMacAddress = new pspNetMacAddress();
				peerMacAddress.read(commandPacket, 8);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_UNKNOWN_0012 bodySize=0x%X, peerMacAddress=%s, unknown %s", bodySize, peerMacAddress, Utilities.getMemoryDump(commandPacket, 14, bodySize - 14)));
				}
				commandPacket.writeUnsigned16(10, 0); // Unknown value
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
			case CMD_802_11_BEACON_STOP:
				int unknown06 = commandPacket.read16(6);
				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_802_11_BEACON_STOP bodySize=0x%X, unknown=0x%X", bodySize, unknown06));
				}
				break;
			case CMD_UNKNOWN_002A: {
				int unknown8 = commandPacket.read32(8);
				int unknown16 = commandPacket.read8(16);
				int unknown17 = commandPacket.read8(17);
				int unknown18 = commandPacket.read8(18);
				int unknown19 = commandPacket.read8(19);
				int unknown20 = commandPacket.read8(20);
				int unknown21 = commandPacket.read8(21);
				int unknown22 = commandPacket.read8(22);
				int unknown23 = commandPacket.read8(23);
				int unknown24 = commandPacket.read32(24);
				int unknown28 = commandPacket.read32(28);
				int unknown32 = commandPacket.read16(32);
				ssid = commandPacketPtr.getStringNZ(34, 32);
				pspNetMacAddress macAddress66 = new pspNetMacAddress(commandPacketPtr.getArray8(66, MAC_ADDRESS_LENGTH));
				int unknown72 = commandPacket.read16(72);
				byte[] unknown74 = commandPacketPtr.getArray8(72, unknown72);
				int unknown90 = commandPacket.read16(90);
				int unknown92 = commandPacket.read16(92);
				pspNetMacAddress macAddressGameMode = new pspNetMacAddress(commandPacketPtr.getArray8(94, MAC_ADDRESS_LENGTH));

				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_UNKNOWN_002A unknown8=0x%X, unknown16=0x%X, unknown17=0x%X, unknown18=0x%X, unknown19=0x%X, unknown20=0x%X, unknown21=0x%X, unknown22=0x%X, unknown23=0x%X, unknown24=0x%X, unknown28=0x%X, unknown32=0x%X, ssid='%s', macAddress66=%s, unknown72=0x%X, unknown74=%s, unknown90=0x%X, unknown92=0x%X, macAddressGameMode=%s", unknown8, unknown16, unknown17, unknown18, unknown19, unknown20, unknown21, unknown22, unknown23, unknown24, unknown28, unknown32, ssid, macAddress66, unknown72, unknown74, unknown90, unknown92, macAddressGameMode));
				}

				commandPacket.write32(8, unknown8);
				responseSize = 12;

				if (gameMode) {
					if (unknown8 == 1) {
						// Store the GameMode group address
						System.arraycopy(macAddressGameMode.macAddress, 0, gameModeGroupAddress, 0, MAC_ADDRESS_LENGTH);
						// Store the GameMode master address (this is the group address without the multicast bits)
						System.arraycopy(gameModeGroupAddress, 0, gameModeMasterAddress, 0, MAC_ADDRESS_LENGTH);
						gameModeMasterAddress[0] &= 0xFC; // Clear multicast bits

						// Start sending events
//						gameModeEventSendMaster = getNow() + 14000; // 14ms
//						gameModeEvent_40 = 0L;
						gameModeEventSendMaster = 0L;
						gameModeEvent_40 = getNow() + 14000; // 14ms
					} else if (unknown8 == 4 || unknown8 == 9 || unknown8 == 10 || unknown8 == 11) {
						// Ignored
					} else if (unknown8 == 0) {
						// Clear the GameMode group address
						Arrays.fill(gameModeGroupAddress, (byte) 0);
						Arrays.fill(gameModeMasterAddress, (byte) 0);

						clearResultFlag(WLAN_RESULT_DATA_PACKET_RECEIVED);

						// Stop sending events
						gameModeEventSendMaster = 0L;
						gameModeEvent_40 = 0L;
					} else {
						log.error(String.format("processCommandPacket CMD_UNKNOWN_002A unknown unknown8=0x%X, unknown16=0x%X, unknown17=0x%X, unknown18=0x%X, unknown19=0x%X, unknown20=0x%X, unknown21=0x%X, unknown22=0x%X, unknown23=0x%X, unknown24=0x%X, unknown28=0x%X, unknown32=0x%X, ssid='%s', macAddress66=%s, unknown72=0x%X, unknown74=%s, unknown90=0x%X, unknown92=0x%X, macAddressGameMode=%s", unknown8, unknown16, unknown17, unknown18, unknown19, unknown20, unknown21, unknown22, unknown23, unknown24, unknown28, unknown32, ssid, macAddress66, unknown72, unknown74, unknown90, unknown92, macAddressGameMode));
					}
				}
				break;
			}
			case CMD_BBP_REG_ACCESS:
				action = commandPacket.read16(8);
				int registerNumber = commandPacket.read16(10);
				int registerValue = commandPacket.read32(12);

				if (log.isDebugEnabled()) {
					log.debug(String.format("processCommandPacket CMD_BBP_REG_ACCESS action=%d(%s), registerNumber=0x%X, registerValue=0x%X", action, action == 1 ? "SET" : "GET", registerNumber, registerValue));
				}
				if (action == 1 && registerNumber == 0x2C && registerValue == 0x38) {
					// Ignore
				} else if (action == 1 && registerNumber == 0x2D && registerValue == 0x4) {
					// Ignore
				} else {
					log.error(String.format("processCommandPacket CMD_BBP_REG_ACCESS unimplemented action=%d(%s), registerNumber=0x%X, registerValue=0x%X", action, action == 1 ? "SET" : "GET", registerNumber, registerValue));
				}
				gameModeEvent_40 = getNow() + 100000; // 100ms
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
			log.debug(String.format("MMIOHandlerWlan.writeData16 dataAddress=0x%X, dataIndex=0x%X, outputPacketSize=0x%X, chipCodeIndex=0x%X, value=0x%04X, endOfCommand=%b", dataAddress, dataIndex, getWlanOutputPacketSize(), firmwareDataIndex, value, endOfCommand));
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
			log.debug(String.format("MMIOHandlerWlan.writeData32 dataAddress=0x%X, dataIndex=0x%X, outputPacketSize=0x%X, chipCodeIndex=0x%X, value=0x%08X, endOfCommand=%b, booting=%b", dataAddress, dataIndex, getWlanOutputPacketSize(), firmwareDataIndex, value, endOfCommand, booting));
		}

		if (booting) {
			if (getWlanOutputPacketSize() == 0 && dataIndex == 0xC) {
				// Ending the reception of the firmware boot code
				// and continuing with the firmware data
				if (useWlanFirmwareEmulation) {
					// Writing the boot code to the firmware at address 0
					Memory mem = handlerWlanFirmware.getMemory();
					for (int i = 0; i < firmwareBootCodeIndex; i += 4) {
						mem.write32(i, readUnaligned32(firmwareBootCode, i));
					}
					WlanEmulator.getInstance().boot();
				}
				firmwareBootCodeIndex = -1;
				firmwareDataIndex = 0;
				setWlanInputPacketSize(0x600);
			} else if (firmwareBootCodeIndex >= 0) {
				// Receiving the firmware boot code first
				writeUnaligned32(firmwareBootCode, firmwareBootCodeIndex, endianSwap32(value));
				firmwareBootCodeIndex += 4;
			} else if (firmwareDataIndex >= 0) {
				// Receiving the firmware data (after the boot code)
				writeUnaligned32(firmwareData, firmwareDataIndex, endianSwap32(value));
				firmwareDataIndex += 4;
			}
		} else {
			getSendPacket(dataAddress).write32(dataIndex, value);
			if (endOfCommand) {
				writeDataEndOfCommand(dataAddress, dataIndex + 4);
			}
		}
	}

	@Override
	protected void startCmd(int cmd) {
		switch (cmd) {
			case MSPRO_CMD_WAKEUP:
				// Note sure if this is the WAKEUP command which is triggering this event
				setCardEvent(WLAN_EVENT_UNKNOWN_40);
				break;
		}

		super.startCmd(cmd);
	}

	@Override
	protected void startTPC(int tpc) {
		super.startTPC(tpc);

		switch (getTPCCode()) {
			case MS_TPC_WRITE_IO_DATA:
				// Dmac is waiting for a call to sceDdrFlush(WLAN_DDR_FLUSH)
				MMIOHandlerDdr.getInstance().doFlush(WLAN_DDR_FLUSH);
				break;
			case MS_TPC_SET_CMD:
			case MS_TPC_EX_SET_CMD:
				// Clear any previous call to sceDdrFlush(WLAN_DDR_FLUSH) when starting a new command
				MMIOHandlerDdr.getInstance().clearFlushDone(WLAN_DDR_FLUSH);
				break;
		}
	}

	@Override
	public void sendPacketFromAccessPoint(byte[] buffer, int bufferLength, EtherFrame etherFrame) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("sendPacketFromAccessPoint bufferLength=0x%X, etherFrame=%s, buffer: %s", bufferLength, etherFrame, Utilities.getMemoryDump(buffer, 0, bufferLength)));
		}

    	try {
			wlanAdapter.sendAccessPointPacket(buffer, 0, bufferLength, etherFrame);
		} catch (IOException e) {
			log.error("sendPacketFromAccessPoint", e);
		}
	}
}
