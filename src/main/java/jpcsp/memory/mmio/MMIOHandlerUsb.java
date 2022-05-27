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

import static jpcsp.memory.mmio.MMIOHandlerSystemControl.SYSREG_USBMS_USB_INTERRUPT3;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.isFallingBit;
import static jpcsp.util.Utilities.isFallingFlag;
import static jpcsp.util.Utilities.isRaisingFlag;
import static jpcsp.util.Utilities.notHasBit;
import static jpcsp.util.Utilities.notHasFlag;
import static jpcsp.util.Utilities.setBit;
import static jpcsp.util.Utilities.setFlag;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.sceUsb;
import jpcsp.scheduler.Scheduler;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

public class MMIOHandlerUsb extends MMIOHandlerBase {
	public static Logger log = sceUsb.log;
	private static final int STATE_VERSION = 0;
	public static final int BASE_ADDRESS = 0xBD800000;
	private static MMIOHandlerUsb instance;
	private final Endpoint[] sendingEndpoints = new SendingEndpoint[5];
	private final Endpoint[] receivingEndpoints = new ReceivingEndpoint[5];
	private int unknown400;
	private int unknown404;
	private int unknown408;
	public static final int CONNECTION_INTERRUPT_CONNECT = 0;
	public static final int CONNECTION_INTERRUPT_STREAMING = 1;
	public static final int CONNECTION_INTERRUPT_DETACH_1 = 2;
	public static final int CONNECTION_INTERRUPT_DETACH_2 = 4;
	public static final int CONNECTION_INTERRUPT_UNKNOWN_6 = 6;
	private int connectionInterrupt;
	private int connectionInterruptEnabled;
	private int unknown414;
	private int endpointsInterfacesDisabled;
	private int unknown41C;
	// One int value as bit field:
	// bit 19-27: unknown
	private int unknown504;
	// 4 int values as bit field:
	// - bits 0-3: unknown
	// - bits 4: unknown
	// - bits 5-6: unknown
	// - bit 7: unknown
	// - bits 11-14: unknown
	// - bits 15-18: unknown
	// - bits 19-29: unknown
	// - bit 30: unknown
	private final int[] unknown508 = new int[4];
	private int state = 0;

	private class UsbReset implements IAction {
		@Override
		public void execute() {
			MMIOHandlerSystemControl.getInstance().triggerUsbMemoryStickInterrupt(SYSREG_USBMS_USB_INTERRUPT3);
		}
	}

	protected class Endpoint implements IState {
		private static final int STATE_VERSION = 0;
		protected static final int CONTROL_UNKNOWN_002 = 0x002;
		protected static final int CONTROL_UNKNOWN_008 = 0x008;
		protected static final int CONTROL_TRANSFER_TYPE_MASK = 0x030;
		protected static final int TRANSFER_TYPE_CONTROL     = 0;
		protected static final int TRANSFER_TYPE_ISOCHRONOUS = 1;
		protected static final int TRANSFER_TYPE_BULK        = 2;
		protected static final int TRANSFER_TYPE_INTERRUPT   = 3;
		protected static final int CONTROL_UNKNOWN_040 = 0x040;
		protected static final int CONTROL_UNKNOWN_080 = 0x080;
		protected static final int CONTROL_UNKNOWN_100 = 0x100;
		protected static final int STATUS_UNKNOWN_010 = 0x010;
		protected static final int STATUS_UNKNOWN_020 = 0x020;
		protected static final int STATUS_UNKNOWN_040 = 0x040;
		protected static final int STATUS_UNKNOWN_080 = 0x080;
		protected static final int STATUS_UNKNOWN_200 = 0x200;
		protected static final int STATUS_UNKNOWN_400 = 0x400;
		protected static final int STATUS_UNKNOWN_4000 = 0x4000;
		protected final Memory mem;
		protected final int endpointNumber;
		protected int control;
		protected int status;
		protected int maxPacketSizeInWords; // Maximum number of 32-bit values (0x10 means 64 bytes)
		protected int maxPacketSizeInBytes; // Maximum number of bytes
		protected int address10;
		protected int address14;

		public Endpoint(Memory mem, int endpointNumber) {
			this.mem = mem;
			this.endpointNumber = endpointNumber;
		}

		protected void clearStatus(int mask) {
			status = clearFlag(status, mask);
		}

		protected int getTransferType() {
			return (control & CONTROL_TRANSFER_TYPE_MASK) >> 4;
		}

		protected void setAddress10(int value) {
			address10 = value;
			if (log.isDebugEnabled()) {
				log.debug(String.format("setAddress10: %s", Utilities.getMemoryDump(address10, 16)));
			}
		}

		protected void setAddress14(int value) {
			address14 = value;
			if (log.isDebugEnabled()) {
				log.debug(String.format("setAddress14: %s", Utilities.getMemoryDump(address14, 12)));
			}
		}

		protected int readStatus() {
			return status;
		}

		protected void setControl(int value) {
			control = value;
		}

		public int read32(int offset) {
			int value = 0;
			switch (offset) {
				case 0x00: value = control; break;
				case 0x04: value = readStatus(); break;
				case 0x08: value = maxPacketSizeInWords; break;
				case 0x0C: value = maxPacketSizeInBytes; break;
				case 0x10: value = address10; break;
				case 0x14: value = address14; break;
				default:
					log.error(String.format("Endpoint.read32 invalid offset 0x%X", offset));
					break;
			}

			return value;
		}

		public void write32(int offset, int value) {
			switch (offset) {
				case 0x00: setControl(value); break;
				case 0x04: clearStatus(value); break;
				case 0x08: maxPacketSizeInWords = value; break;
				case 0x0C: maxPacketSizeInBytes = value; break; // Possible values: 0x40
				case 0x10: setAddress10(value); break;
				case 0x14: setAddress14(value); break;
				default:
					log.error(String.format("Endpoint.write32 invalid offset 0x%X", offset));
					break;
			}
		}

		@Override
		public void read(StateInputStream stream) throws IOException {
			stream.readVersion(STATE_VERSION);
			control = stream.readInt();
			status = stream.readInt();
			maxPacketSizeInWords = stream.readInt();
			maxPacketSizeInBytes = stream.readInt();
			address10 = stream.readInt();
			address14 = stream.readInt();
		}

		@Override
		public void write(StateOutputStream stream) throws IOException {
			stream.writeVersion(STATE_VERSION);
			stream.writeInt(control);
			stream.writeInt(status);
			stream.writeInt(maxPacketSizeInWords);
			stream.writeInt(maxPacketSizeInBytes);
			stream.writeInt(address10);
			stream.writeInt(address14);
		}

		public void reset() {
			control = 0;
			status = 0;
			maxPacketSizeInWords = 0;
			maxPacketSizeInBytes = 0;
			address10 = 0;
			address14 = 0;
		}
	}

	protected class ReceivingEndpoint extends Endpoint {
		public ReceivingEndpoint(Memory mem, int endpointNumber) {
			super(mem, endpointNumber);
		}

		private void sendRequest(boolean deviceToHost, int type, int recipient, int bRequest, int wValue, int wIndex, int wLength) {
			unknown414 = setBit(unknown414, 16 + endpointNumber);
			mem.write32(address10, 0x2 << 30);
			status |= STATUS_UNKNOWN_020;
			TPointer deviceRequest = new TPointer(mem, address10 + 8);
			int bmRequestType = 0;
			bmRequestType |= ((deviceToHost ? 1 : 0) << 7); // Device to Host / Host to Device
			bmRequestType |= (type << 5);
			bmRequestType |= (recipient << 0);
			deviceRequest.setUnsignedValue8(0, bmRequestType);
			deviceRequest.setUnsignedValue8(1, bRequest);
			deviceRequest.setUnsignedValue16(2, wValue);
			deviceRequest.setUnsignedValue16(4, wIndex);
			deviceRequest.setUnsignedValue16(6, wLength);
			control = clearFlag(control, CONTROL_UNKNOWN_100);
			control = setFlag(control, CONTROL_UNKNOWN_040);
			RuntimeContextLLE.triggerInterrupt(getProcessor(), IntrManager.PSP_USB_INTERRUPT_CONNECTION);
		}

		protected void setControl(int value) {
			int oldControl = control;
			super.setControl(value);

			if (isRaisingFlag(oldControl, control, CONTROL_UNKNOWN_100) && notHasFlag(control, CONTROL_UNKNOWN_040) && endpointNumber == 0) {
				if (state == 0) {
					log.error(String.format("receivingEndpoints[0].control CONTROL_UNKNOWN_100 state=%d", state));
					sendRequest(true, 2, 1, 1, 3, 0, 1);
					state++;
				} else if (state == 3) {
					log.error(String.format("receivingEndpoints[0].control CONTROL_UNKNOWN_100 state=%d", state));
					sendRequest(false, 2, 1, 1, 0, 0, 8);
					state++;
				} else if (state == 5) {
					log.error(String.format("receivingEndpoints[0].control CONTROL_UNKNOWN_100 state=%d", state));
					// GET_DESCRIPTOR for the device descriptor:
					// the PSP will send an 18 bytes long device descriptor
					sendRequest(true, 0, 0, 6, 0x100, 0, 18);
					state++;
				} else if (state == 8) {
					log.error(String.format("receivingEndpoints[0].control CONTROL_UNKNOWN_100 state=%d", state));
					// GET_DESCRIPTOR for the configuration descriptor
					sendRequest(true, 0, 0, 6, 0x200, 0, 0x94);
					state++;
				} else if (state == 11) {
					log.error(String.format("receivingEndpoints[0].control CONTROL_UNKNOWN_100 state=%d", state));
					sendingEndpoints[endpointNumber].control = clearFlag(sendingEndpoints[endpointNumber].control, CONTROL_UNKNOWN_008 | CONTROL_UNKNOWN_002);
					state++;
				} else if (state == 15) {
					log.error(String.format("receivingEndpoints[0].control CONTROL_UNKNOWN_100 state=%d", state));
					sendRequest(true, 2, 1, 3, 0, 3, 5);
					state++;
				} else if (state == 18) {
					log.error(String.format("receivingEndpoints[0].control CONTROL_UNKNOWN_100 state=%d", state));
					sendingEndpoints[endpointNumber].control = clearFlag(sendingEndpoints[endpointNumber].control, CONTROL_UNKNOWN_008 | CONTROL_UNKNOWN_002);
					state++;
				} else if (state == 20) {
					log.error(String.format("receivingEndpoints[0].control CONTROL_UNKNOWN_100 state=%d", state));
					sendRequest(true, 2, 1, 8, 0, 3, 8);
					state++;
				} else if (state == 23) {
					log.error(String.format("receivingEndpoints[0].control CONTROL_UNKNOWN_100 state=%d", state));
					sendingEndpoints[endpointNumber].control = clearFlag(sendingEndpoints[endpointNumber].control, CONTROL_UNKNOWN_008 | CONTROL_UNKNOWN_002);
					state++;
				} else if (state == 25) {
					log.error(String.format("receivingEndpoints[0].control CONTROL_UNKNOWN_100 state=%d", state));
					sendingEndpoints[endpointNumber].control = clearFlag(sendingEndpoints[endpointNumber].control, CONTROL_UNKNOWN_008 | CONTROL_UNKNOWN_002);
					unknown414 = setBit(unknown414, 16 + endpointNumber);
					mem.write32(address14, 0x2 << 30);
					state++;
				} else {
					log.error(String.format("receivingEndpoints[0].control CONTROL_UNKNOWN_100 unimplemented state=%d", state));
				}
			}
		}
	}

	protected class SendingEndpoint extends Endpoint {
		public SendingEndpoint(Memory mem, int endpointNumber) {
			super(mem, endpointNumber);
		}

		private void completeRequest(boolean pendingData) {
			log.error(String.format("setControl CONTROL_UNKNOWN_008 state=%d", state));
			if (pendingData) {
				control = clearFlag(control, CONTROL_UNKNOWN_008);
			}
			unknown414 = setBit(unknown414, endpointNumber);
			status |= Endpoint.STATUS_UNKNOWN_400;
			receivingEndpoints[endpointNumber].control = clearFlag(receivingEndpoints[endpointNumber].control, CONTROL_UNKNOWN_040 | CONTROL_UNKNOWN_100);
			int flags = mem.read16(address14 + 2);
			flags = (flags & 0x3FFF) | (2 << 14);
			mem.write16(address14 + 2, (short) flags);
			RuntimeContextLLE.triggerInterrupt(getProcessor(), IntrManager.PSP_USB_INTERRUPT_CONNECTION);
			state++;
		}

		protected void setControl(int value) {
			int oldControl = control;

			super.setControl(value);

			if (isRaisingFlag(oldControl, control, CONTROL_UNKNOWN_008)) {
				int length = mem.read16(address14 + 0);
				int flags = mem.read16(address14 + 2);
				int address = mem.read32(address14 + 8);
				if (log.isTraceEnabled()) {
					log.trace(String.format("setControl CONTROL_UNKNOWN_008 length=0x%X, flags=0x%X, address=0x%08X: %s", length, flags, address, Utilities.getMemoryDump(mem, address, length)));
				}

				if (length == 4) {
					if (log.isDebugEnabled()) {
						int parameter0 = mem.read16(address + 0);
						int parameter2 = mem.read8(address + 2);
						int length3 = mem.read8(address + 3);
						log.debug(String.format("setControl sending length=0x%X, flags=0x%04X, parameter0=0x%04X, parameter2=0x%02X, length3=0x%02X", length, flags, parameter0, parameter2, length3, Utilities.getMemoryDump(address + 4, length3)));
					}
					switch (mem.read8(address + 2)) {
						case 0xA4: // set saturation
							int saturationValue = mem.read8(address + 4); // [0..6]
							if (log.isDebugEnabled()) {
								log.debug(String.format("setSaturation %d", saturationValue));
							}
							break;
						case 0xA5: // set brightness
							int brightnessValue = mem.read8(address + 4); // [128..255]
							if (log.isDebugEnabled()) {
								log.debug(String.format("setBrightness %d", brightnessValue));
							}
							break;
						case 0xA6: // set contrast
							int contrastValue = mem.read8(address + 4);
							if (log.isDebugEnabled()) {
								log.debug(String.format("setContrast %d", contrastValue));
							}
							break;
						case 0x2B:
							log.error("setControl 0x2B triggering interrupt");
							mem.writeUnsigned16(address14 + 2, (flags & 0x3FFF) | (0x2 << 14));
							triggerConnectionInterrupt(5);
							break;
						default:
							log.error(String.format("setControl length=%d, unknown parameter2 0x%02X", length, mem.read8(address + 2)));
							break;
					}
				} else if (length == 1) {
					int parameter0 = mem.read8(address + 0);
					switch (parameter0) {
						case 0x00:
							log.error("setControl length=1, parameter0=0x00");
							break;
						case 0x01:
							log.error("setControl length=1, parameter0=0x01");
							break;
						default:
							log.error(String.format("setControl length=%d, unknown parameter0 0x%02X", length, mem.read8(address + 2)));
							break;
					}
				} else if (length == 18) {
					log.error("setControl length=18");
					if (log.isDebugEnabled()) {
						log.debug(String.format("GET_DESCRIPTOR for device: %s", Utilities.getMemoryDump(mem, address, length)));
					}
				} else if (length == 0x40) {
//					mem.memset(address, (byte) 0, length);
				} else if (length == 6) {
//					mem.memset(address, (byte) 0, length);
				}

				if (state == 2 || state == 7 || state == 14 || state == 19 || state == 24) {
					completeRequest(false);
				} else if (state == 12 || state == 13) {
					completeRequest(true);
				} else {
					log.error(String.format("setControl CONTROL_UNKNOWN_008 unimplemented state=%d", state));
				}
			} else if (isFallingFlag(oldControl, control, CONTROL_UNKNOWN_008)) {
				if (state == 4 || state == 9 || state == 16 || state == 21) {
					log.error(String.format("setControl clear CONTROL_UNKNOWN_008 state=%d", state));
					receivingEndpoints[endpointNumber].control = clearFlag(receivingEndpoints[endpointNumber].control, CONTROL_UNKNOWN_040);
					state++;
				} else {
					log.error(String.format("setControl clear CONTROL_UNKNOWN_008 unimplemented state=%d", state));
				}
			}
		}
	}

	public static MMIOHandlerUsb getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerUsb(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerUsb(int baseAddress) {
		super(baseAddress);

		for (int i = 0; i < sendingEndpoints.length; i++) {
			sendingEndpoints[i] = new SendingEndpoint(getMemory(), i);
		}
		for (int i = 0; i < receivingEndpoints.length; i++) {
			receivingEndpoints[i] = new ReceivingEndpoint(getMemory(), i);
		}
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		for (int i = 0; i < sendingEndpoints.length; i++) {
			sendingEndpoints[i].read(stream);
		}
		for (int i = 0; i < receivingEndpoints.length; i++) {
			receivingEndpoints[i].read(stream);
		}
		unknown400 = stream.readInt();
		unknown404 = stream.readInt();
		unknown408 = stream.readInt();
		connectionInterrupt = stream.readInt();
		connectionInterruptEnabled = stream.readInt();
		unknown414 = stream.readInt();
		endpointsInterfacesDisabled = stream.readInt();
		unknown41C = stream.readInt();
		unknown504 = stream.readInt();
		stream.readInts(unknown508);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		for (int i = 0; i < sendingEndpoints.length; i++) {
			sendingEndpoints[i].write(stream);
		}
		for (int i = 0; i < receivingEndpoints.length; i++) {
			receivingEndpoints[i].write(stream);
		}
		stream.writeInt(unknown400);
		stream.writeInt(unknown404);
		stream.writeInt(unknown408);
		stream.writeInt(connectionInterrupt);
		stream.writeInt(connectionInterruptEnabled);
		stream.writeInt(unknown414);
		stream.writeInt(endpointsInterfacesDisabled);
		stream.writeInt(unknown41C);
		stream.writeInt(unknown504);
		stream.writeInts(unknown508);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		for (int i = 0; i < sendingEndpoints.length; i++) {
			sendingEndpoints[i].reset();
		}
		for (int i = 0; i < receivingEndpoints.length; i++) {
			receivingEndpoints[i].reset();
		}
		unknown400 = 0;
		unknown404 = 0;
		unknown408 = 0;
		connectionInterrupt = 0;
		connectionInterruptEnabled = 0;
		unknown414 = 0;
		endpointsInterfacesDisabled = 0;
		unknown41C = 0;
		unknown504 = 0;
		for (int i = 0; i < unknown508.length; i++) {
			unknown508[i] = 0;
		}
	}

	public void triggerReset() {
		Emulator.getScheduler().addAction(Scheduler.getNow() + 1000, new UsbReset());
	}

	private void checkConnectionInterrupt() {
		if ((connectionInterrupt & connectionInterruptEnabled) != 0) {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), IntrManager.PSP_USB_INTERRUPT_CONNECTION);
		} else {
			RuntimeContextLLE.clearInterrupt(getProcessor(), IntrManager.PSP_USB_INTERRUPT_CONNECTION);
		}
	}

	private void triggerConnectionInterrupt(int bit) {
		connectionInterrupt = setBit(connectionInterrupt, bit);
		checkConnectionInterrupt();
	}

	private void clearConnectionInterrupt(int mask) {
		connectionInterrupt &= ~mask;
		checkConnectionInterrupt();
	}

	private void clearUnknown414(int mask) {
		unknown414 &= ~mask;
	}

	private void setConnectionInterruptEnabled(int value) {
		connectionInterruptEnabled = value;
		checkConnectionInterrupt();
	}

	private void moveToConnectionEstablished() {
		// Normal USB: this is required to move the connection to state PSP_USB_CONNECTION_ESTABLISHED
		unknown408 = 0xF;
		triggerConnectionInterrupt(CONNECTION_INTERRUPT_CONNECT);
	}

	private void setUnknown404(int value) {
		int oldValue = unknown404;
		unknown404 = value;

		if (value == 0x210) {
			if (false) {
				moveToConnectionEstablished();
			}
		} else if (value == 0x21C && oldValue == 0x210) {
			if (false) {
				// Camera USB: this is required to move the connection to state PSP_USB_CONNECTION_ESTABLISHED
				triggerConnectionInterrupt(5);
			}
		} else if (value == 0x61C) {
			// Set during sceUsbDeactivate()
		} else if (value == 0x610) {
			// Set at initialization
		}
	}

	protected boolean isEndpointEnabled(int endpointNumber) {
		return notHasBit(endpointsInterfacesDisabled, endpointNumber);
	}

	protected boolean isInterfaceEnabled(int interfaceNumber) {
		return notHasBit(endpointsInterfacesDisabled, interfaceNumber + 16);
	}

	private void setEndpointsInterfacesDisabled(int value) {
		int oldEndpointsInterfacesDisabled = endpointsInterfacesDisabled;
		value &= 0x01FF01FF; // Real PSP supports only those values
		endpointsInterfacesDisabled = value;

		for (int i = 0; i < sendingEndpoints.length; i++) {
			if (isFallingBit(oldEndpointsInterfacesDisabled, endpointsInterfacesDisabled, i)) {
				if (state == 1 || state == 6 || state == 10 || state == 17 || state == 22) {
					log.error(String.format("setEndpointsInterfacesDisabled enabling interface %d, state=%d", i, state));
					unknown414 = setBit(unknown414, i);
					sendingEndpoints[i].status |= Endpoint.STATUS_UNKNOWN_040;
					RuntimeContextLLE.triggerInterrupt(getProcessor(), IntrManager.PSP_USB_INTERRUPT_CONNECTION);
					state++;
				} else {
					log.error(String.format("setEndpointsInterfacesDisabled unimplemented enabling interface %d, state=%d", i, state));
				}
			}
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x000:
			case 0x004:
			case 0x008:
			case 0x00C:
			case 0x010:
			case 0x014:
			case 0x018:
			case 0x01C: value = sendingEndpoints[0].read32(address - baseAddress - 0x000); break;
			case 0x020:
			case 0x024:
			case 0x028:
			case 0x02C:
			case 0x030:
			case 0x034:
			case 0x038:
			case 0x03C: value = sendingEndpoints[1].read32(address - baseAddress - 0x020); break;
			case 0x040:
			case 0x044:
			case 0x048:
			case 0x04C:
			case 0x050:
			case 0x054:
			case 0x058:
			case 0x05C: value = sendingEndpoints[2].read32(address - baseAddress - 0x040); break;
			case 0x060:
			case 0x064:
			case 0x068:
			case 0x06C:
			case 0x070:
			case 0x074:
			case 0x078:
			case 0x07C: value = sendingEndpoints[3].read32(address - baseAddress - 0x060); break;
			case 0x080:
			case 0x084:
			case 0x088:
			case 0x08C:
			case 0x090:
			case 0x094:
			case 0x098:
			case 0x09C: value = sendingEndpoints[4].read32(address - baseAddress - 0x080); break;
			case 0x200:
			case 0x204:
			case 0x208:
			case 0x20C:
			case 0x210:
			case 0x214:
			case 0x218:
			case 0x21C: value = receivingEndpoints[0].read32(address - baseAddress - 0x200); break;
			case 0x220:
			case 0x224:
			case 0x228:
			case 0x22C:
			case 0x230:
			case 0x234:
			case 0x238:
			case 0x23C: value = receivingEndpoints[1].read32(address - baseAddress - 0x220); break;
			case 0x240:
			case 0x244:
			case 0x248:
			case 0x24C:
			case 0x250:
			case 0x254:
			case 0x258:
			case 0x25C: value = receivingEndpoints[2].read32(address - baseAddress - 0x240); break;
			case 0x260:
			case 0x264:
			case 0x268:
			case 0x26C:
			case 0x270:
			case 0x274:
			case 0x278:
			case 0x27C: value = receivingEndpoints[3].read32(address - baseAddress - 0x260); break;
			case 0x280:
			case 0x284:
			case 0x288:
			case 0x28C:
			case 0x290:
			case 0x294:
			case 0x298:
			case 0x29C: value = receivingEndpoints[4].read32(address - baseAddress - 0x280); break;
			case 0x400: value = unknown400; break;
			case 0x404: value = unknown404; break;
			case 0x408: value = unknown408; break;
			case 0x40C: value = connectionInterrupt; break;
			case 0x410: value = connectionInterruptEnabled; break;
			case 0x414: value = unknown414; break;
			case 0x418: value = endpointsInterfacesDisabled; break;
			case 0x41C: value = unknown41C; break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x000:
			case 0x004:
			case 0x008:
			case 0x00C:
			case 0x010:
			case 0x014:
			case 0x018:
			case 0x01C: sendingEndpoints[0].write32(address - baseAddress - 0x000, value); break;
			case 0x020:
			case 0x024:
			case 0x028:
			case 0x02C:
			case 0x030:
			case 0x034:
			case 0x038:
			case 0x03C: sendingEndpoints[1].write32(address - baseAddress - 0x020, value); break;
			case 0x040:
			case 0x044:
			case 0x048:
			case 0x04C:
			case 0x050:
			case 0x054:
			case 0x058:
			case 0x05C: sendingEndpoints[2].write32(address - baseAddress - 0x040, value); break;
			case 0x060:
			case 0x064:
			case 0x068:
			case 0x06C:
			case 0x070:
			case 0x074:
			case 0x078:
			case 0x07C: sendingEndpoints[3].write32(address - baseAddress - 0x060, value); break;
			case 0x080:
			case 0x084:
			case 0x088:
			case 0x08C:
			case 0x090:
			case 0x094:
			case 0x098:
			case 0x09C: sendingEndpoints[4].write32(address - baseAddress - 0x080, value); break;
			case 0x200:
			case 0x204:
			case 0x208:
			case 0x20C:
			case 0x210:
			case 0x214:
			case 0x218:
			case 0x21C: receivingEndpoints[0].write32(address - baseAddress - 0x200, value); break;
			case 0x220:
			case 0x224:
			case 0x228:
			case 0x22C:
			case 0x230:
			case 0x234:
			case 0x238:
			case 0x23C: receivingEndpoints[1].write32(address - baseAddress - 0x220, value); break;
			case 0x240:
			case 0x244:
			case 0x248:
			case 0x24C:
			case 0x250:
			case 0x254:
			case 0x258:
			case 0x25C: receivingEndpoints[2].write32(address - baseAddress - 0x240, value); break;
			case 0x260:
			case 0x264:
			case 0x268:
			case 0x26C:
			case 0x270:
			case 0x274:
			case 0x278:
			case 0x27C: receivingEndpoints[3].write32(address - baseAddress - 0x260, value); break;
			case 0x280:
			case 0x284:
			case 0x288:
			case 0x28C:
			case 0x290:
			case 0x294:
			case 0x298:
			case 0x29C: receivingEndpoints[4].write32(address - baseAddress - 0x280, value); break;
			case 0x400: unknown400 = value; break; // Possible values: 0xA0 (unknown meaning) | 0x8 (activated without charging) | 0x1 (unknown meaning)
			case 0x404: setUnknown404(value); break;
			case 0x40C: clearConnectionInterrupt(value); break;
			case 0x410: setConnectionInterruptEnabled(value); break;
			case 0x414: clearUnknown414(value); break;
			case 0x418: setEndpointsInterfacesDisabled(value); break;
			case 0x41C: unknown41C = value; break; // Possible values: 0
			case 0x504: unknown504 = value; break;
			case 0x508: unknown508[0] = value; break;
			case 0x50C: unknown508[1] = value; break;
			case 0x510: unknown508[2] = value; break;
			case 0x514: unknown508[3] = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
