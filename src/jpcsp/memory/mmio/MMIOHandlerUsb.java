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

import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.notHasBit;
import static jpcsp.util.Utilities.setBit;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
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
	private final Endpoint[] sendingEndpoints;
	private final Endpoint[] receivingEndpoints;
	private int unknown400;
	private int unknown404;
	private int unknown408;
	private int connectionInterrupt;
	private int connectionInterruptEnabled;
	private int unknown414;
	private int endpointsInterfacesDisabled;
	private int unknown41C;
	private int unknown504;
	private int unknown508;
	private int unknown50C;
	private int unknown510;
	private int unknown514;

	private class UsbReset implements IAction {
		@Override
		public void execute() {
			MMIOHandlerSystemControl.getInstance().triggerUsbMemoryStickInterrupt(MMIOHandlerSystemControl.SYSREG_USBMS_USB_INTERRUPT3);
		}
	}

	protected static class Endpoint implements IState {
		private static final int STATE_VERSION = 0;
		protected static final int CONTROL_UNKNOWN_008 = 0x008;
		protected static final int CONTROL_UNKNOWN_040 = 0x040;
		protected static final int CONTROL_UNKNOWN_100 = 0x100;
		protected static final int CONTROL_UNKNOWN04_UNKNOWN_040 = 0x040;
		protected static final int CONTROL_UNKNOWN04_UNKNOWN_080 = 0x080;
		protected static final int CONTROL_UNKNOWN04_UNKNOWN_200 = 0x200;
		protected static final int CONTROL_UNKNOWN04_UNKNOWN_400 = 0x400;
		private final Memory mem;
		private int control;
		private int unknown04;
		private int unknown08;
		private int unknown0C;
		private int address10;
		private int address14;
		private int unknown18;
		private int unknown1C;

		public Endpoint(Memory mem) {
			this.mem = mem;
		}

		private void clearUnknown04(int mask) {
			unknown04 = clearFlag(unknown04, mask);
		}

		public int read32(int offset) {
			int value = 0;
			switch (offset) {
				case 0x00: value = control; break;
				case 0x04: value = unknown04; break;
				case 0x08: value = unknown08; break;
				case 0x0C: value = unknown0C; break;
				case 0x10: value = address10; break;
				case 0x14: value = address14; break;
				case 0x18: value = unknown18; break;
				case 0x1C: value = unknown1C; break;
				default:
					log.error(String.format("Endpoint.read32 invalid offset 0x%X", offset));
					break;
			}

			return value;
		}

		private void setAddress14(int value) {
			address14 = value;
			if (log.isDebugEnabled()) {
				log.debug(String.format("setAddress14: %s", Utilities.getMemoryDump(address14, 32)));
			}
		}

		private void setControl(int value) {
			control = value;

			if (Utilities.hasFlag(control, CONTROL_UNKNOWN_008)) {
				int length = mem.read16(address14 + 0);
				int flags = mem.read16(address14 + 2);
				int address = mem.read32(address14 + 8);
				if (log.isDebugEnabled()) {
					log.debug(String.format("setControl sending length=0x%X, flags=0x%X, %s", length, flags, Utilities.getMemoryDump(address, length)));
				}
			}
		}

		public void write32(int offset, int value) {
			switch (offset) {
				case 0x00: setControl(value); break;
				case 0x04: clearUnknown04(value); break;
				case 0x08: unknown08 = value; break;
				case 0x0C: unknown0C = value; break;
				case 0x10: address10 = value; break;
				case 0x14: setAddress14(value); break;
				case 0x18: unknown18 = value; break;
				case 0x1C: unknown1C = value; break;
				default:
					log.error(String.format("Endpoint.write32 invalid offset 0x%X", offset));
					break;
			}
		}

		@Override
		public void read(StateInputStream stream) throws IOException {
			stream.readVersion(STATE_VERSION);
			control = stream.readInt();
			unknown04 = stream.readInt();
			unknown08 = stream.readInt();
			unknown0C = stream.readInt();
			address10 = stream.readInt();
			address14 = stream.readInt();
			unknown18 = stream.readInt();
			unknown1C = stream.readInt();
		}

		@Override
		public void write(StateOutputStream stream) throws IOException {
			stream.writeVersion(STATE_VERSION);
			stream.writeInt(control);
			stream.writeInt(unknown04);
			stream.writeInt(unknown08);
			stream.writeInt(unknown0C);
			stream.writeInt(address10);
			stream.writeInt(address14);
			stream.writeInt(unknown18);
			stream.writeInt(unknown1C);
		}

		public void reset() {
			control = 0;
			unknown04 = 0;
			unknown08 = 0;
			unknown0C = 0;
			address10 = 0;
			address14 = 0;
			unknown18 = 0;
			unknown1C = 0;
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

		sendingEndpoints = new Endpoint[5];
		for (int i = 0; i < sendingEndpoints.length; i++) {
			sendingEndpoints[i] = new Endpoint(getMemory());
		}

		receivingEndpoints = new Endpoint[4];
		for (int i = 0; i < receivingEndpoints.length; i++) {
			receivingEndpoints[i] = new Endpoint(getMemory());
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
		unknown508 = stream.readInt();
		unknown50C = stream.readInt();
		unknown510 = stream.readInt();
		unknown514 = stream.readInt();
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
		stream.writeInt(unknown508);
		stream.writeInt(unknown50C);
		stream.writeInt(unknown510);
		stream.writeInt(unknown514);
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
		unknown508 = 0;
		unknown50C = 0;
		unknown510 = 0;
		unknown514 = 0;
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

	private void setUnknown404(int value) {
		unknown404 = value;

		if (value == 0x210) {
			// Normal USB: this is required to move the connection to state PSP_USB_CONNECTION_ESTABLISHED
			unknown408 = 0xF;
			triggerConnectionInterrupt(0);
//			triggerConnectionInterrupt(1);
//			triggerConnectionInterrupt(2);
//			triggerConnectionInterrupt(3);
//			triggerConnectionInterrupt(4);
//			triggerConnectionInterrupt(5);
//			triggerConnectionInterrupt(6);
		} else if (value == 0x21C) {
			// Camera USB: this is required to move the connection to state PSP_USB_CONNECTION_ESTABLISHED
			unknown408 = 0xF;
			triggerConnectionInterrupt(2);
//			triggerConnectionInterrupt(5);
		}
	}

	protected boolean isEndpointEnabled(int endpointNumber) {
		return notHasBit(endpointsInterfacesDisabled, endpointNumber);
	}

	protected boolean isInterfaceEnabled(int interfaceNumber) {
		return notHasBit(endpointsInterfacesDisabled, interfaceNumber + 16);
	}

	private void setEndpointsInterfacesDisabled(int value) {
		endpointsInterfacesDisabled = value;

		if (isEndpointEnabled(1)) {
			unknown414 = setBit(unknown414, 1);
			sendingEndpoints[1].unknown04 |= Endpoint.CONTROL_UNKNOWN04_UNKNOWN_040;
			triggerConnectionInterrupt(5);
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
			case 0x400: unknown400 = value; break; // Possible values: 0xA0 (unknown meaning) | 0x8 (activated without charging) | 0x1 (unknown meaning)
			case 0x404: setUnknown404(value); break;
			case 0x40C: clearConnectionInterrupt(value); break;
			case 0x410: setConnectionInterruptEnabled(value); break;
			case 0x414: clearUnknown414(value); break;
			case 0x418: setEndpointsInterfacesDisabled(value); break;
			case 0x41C: unknown41C = value; break; // Possible values: 0
			case 0x504: unknown504 = value; break;
			case 0x508: unknown508 = value; break;
			case 0x50C: unknown50C = value; break;
			case 0x510: unknown510 = value; break;
			case 0x514: unknown514 = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
