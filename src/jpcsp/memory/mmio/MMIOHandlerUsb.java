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

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.sceUsb;
import jpcsp.scheduler.Scheduler;

public class MMIOHandlerUsb extends MMIOHandlerBase {
	public static Logger log = sceUsb.log;
	public static final int BASE_ADDRESS = 0xBD800000;
	private static MMIOHandlerUsb instance;
	private int unknown400;
	private int unknown404;
	private int unknown40C;
	private int unknown410;
	private int unknown414;
	private int unknown418;
	private int unknown41C;

	private class UsbReset implements IAction {
		@Override
		public void execute() {
			MMIOHandlerSystemControl.getInstance().triggerUsbMemoryStickInterrupt(MMIOHandlerSystemControl.SYSREG_USBMS_USB_INTERRUPT3);
			RuntimeContextLLE.triggerInterrupt(getProcessor(), IntrManager.PSP_USB_57);
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
	}

	public void triggerReset() {
		Emulator.getScheduler().addAction(Scheduler.getNow() + 1000, new UsbReset());
	}

	private void clearUnknown40C(int mask) {
		unknown40C &= ~mask;
	}

	private void clearUnknown414(int mask) {
		unknown414 &= ~mask;
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x400: value = unknown400; break;
			case 0x404: value = unknown404; break;
			case 0x40C: value = unknown40C; break;
			case 0x410: value = unknown410; break;
			case 0x414: value = unknown414; break;
			case 0x418: value = unknown418; break;
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
			case 0x400: unknown400 = value; break;
			case 0x404: unknown404 = value; break;
			case 0x40C: clearUnknown40C(value); break;
			case 0x410: unknown410 = value; break;
			case 0x414: clearUnknown414(value); break;
			case 0x418: unknown418 = value; break;
			case 0x41C: unknown41C = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
