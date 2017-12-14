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

import static jpcsp.HLE.Modules.sceDisplayModule;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_DMACPLUS_INTR;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.sceDmacplus;
import jpcsp.mediaengine.MEProcessor;
import jpcsp.memory.mmio.dmac.DmacProcessor;

public class MMIOHandlerDmacplus extends MMIOHandlerBase {
	public static Logger log = sceDmacplus.log;
	public static final int COMPLETED_FLAG_UNKNOWN      = 0x01;
	public static final int COMPLETED_FLAG_AVC          = 0x02;
	public static final int COMPLETED_FLAG_SC2ME        = 0x04;
	public static final int COMPLETED_FLAG_ME2SC        = 0x08;
	public static final int COMPLETED_FLAG_SC128_MEMCPY = 0x10;
	private final DmacProcessor dmacProcessors[] = new DmacProcessor[3];
	// flagsCompleted:
	// - 0x01: not used
	// - 0x02: triggers call to sceLowIO_Driver.sub_000063DC (sceKernelSetEventFlag name=SceDmacplusAvc, bits=1)
	// - 0x04: triggers call to sceLowIO_Driver.sub_00006898 (sceKernelSetEventFlag name=SceDmacplusSc2Me, bits=1)
	// - 0x08: triggers call to sceLowIO_Driver.sub_00006D20 (sceKernelSetEventFlag name=SceDmacplusMe2Sc, bits=1)
	// - 0x10: triggers call to sceLowIO_Driver.sub_00006DDC (sceKernelSetEventFlag name=SceDmacplusSc128, bits=1, sceKernelSignalSema name=SceDmacplusSc128, signal=1)
	private int flagsCompleted;
	// flagsError:
	// - 0x01: triggers call to sceLowIO_Driver.sub_00005A40 (accessing 0xBC800110)
	// - 0x02: triggers call to sceLowIO_Driver.sub_000062DC (accessing 0xBC800160 and 0xBC800120-0xBC80014C, sceKernelSetEventFlag name=SceDmacplusAvc, bits=2)
	// - 0x04: triggers call to sceLowIO_Driver.sub_00006818 (accessing 0xBC800190 and 0xBC800180-0xBC80018C, sceKernelSetEventFlag name=SceDmacplusSc2Me, bits=2)
	// - 0x08: triggers call to sceLowIO_Driver.sub_00006CA0 (accessing 0xBC8001B0 and 0xBC8001A0-0xBC8001AC, sceKernelSetEventFlag name=SceDmacplusMe2Sc, bits=2)
	// - 0x10: triggers call to sceLowIO_Driver.sub_00006E30 (accessing 0xBC8001C0-0xBC8001CC, sceKernelSetEventFlag name=SceDmacplusSc128, bits=2)
	private int flagsError;
	private int displayFrameBufferAddr;
	private int displayWidth; // E.g. 480, must be a multiple of 8
	private int displayFrameBufferWidth; // E.g. 512, must be a multiple of 64
	private int displayPixelFormatCoded; // Values: [0..3]
	public static final int DISPLAY_FLAG_ENABLED = 0x1;
	public static final int DISPLAY_FLAG_UNKNOWN = 0x2;
	private int displayFlags;

	private class DmacCompletedAction implements IAction {
		private int flagCompleted;

		public DmacCompletedAction(int flagCompleted) {
			this.flagCompleted = flagCompleted;
		}

		@Override
		public void execute() {
			memcpyCompleted(flagCompleted);
		}
	}

	public MMIOHandlerDmacplus(int baseAddress) {
		super(baseAddress);

		Memory meMemory = MEProcessor.getInstance().getMEMemory();
		Memory scMemory = getMemory();
		dmacProcessors[0] = new DmacProcessor(scMemory, meMemory, baseAddress + 0x180, new DmacCompletedAction(COMPLETED_FLAG_SC2ME));
		dmacProcessors[1] = new DmacProcessor(meMemory, scMemory, baseAddress + 0x1A0, new DmacCompletedAction(COMPLETED_FLAG_ME2SC));
		dmacProcessors[2] = new DmacProcessor(scMemory, scMemory, baseAddress + 0x1C0, new DmacCompletedAction(COMPLETED_FLAG_SC128_MEMCPY));
	}

	private void memcpyCompleted(int flagCompleted) {
		flagsCompleted |= flagCompleted;

		checkInterrupt();
	}

	private void checkInterrupt() {
		if (flagsCompleted != 0) {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_DMACPLUS_INTR);
		} else {
			RuntimeContextLLE.clearInterrupt(getProcessor(), PSP_DMACPLUS_INTR);
		}
	}

	public void setDisplayFrameBufferAddr(int displayFrameBufferAddr) {
		if (this.displayFrameBufferAddr != displayFrameBufferAddr) {
			this.displayFrameBufferAddr = displayFrameBufferAddr;
			updateDisplay();
		}
	}

	public void setDisplayWidth(int displayWidth) {
		this.displayWidth = displayWidth;
	}

	public void setDisplayFrameBufferWidth(int displayFrameBufferWidth) {
		if (this.displayFrameBufferWidth != displayFrameBufferWidth) {
			this.displayFrameBufferWidth = displayFrameBufferWidth;
			updateDisplay();
		}
	}

	public void setDisplayPixelFormat(int displayPixelFormat) {
		if (this.displayPixelFormatCoded != displayPixelFormat) {
			this.displayPixelFormatCoded = displayPixelFormat;
			updateDisplay();
		}
	}

	public void setDisplayFlags(int displayFlags) {
		this.displayFlags = displayFlags;
	}

	private void updateDisplay() {
		int displayPixelFormat = sceDmacplus.pixelFormatFromCode[displayPixelFormatCoded & 0x3];
		int frameBufferAddr = displayFrameBufferAddr;
		if ((displayFlags & DISPLAY_FLAG_ENABLED) == 0) {
			frameBufferAddr = 0;
		}
		sceDisplayModule.hleDisplaySetFrameBuf(frameBufferAddr, displayFrameBufferWidth, displayPixelFormat, 0);
		sceDisplayModule.step();
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x004: value = flagsCompleted; break;
			case 0x00C: value = flagsError; break;
			case 0x100: value = displayFrameBufferAddr; break;
			case 0x104: value = displayPixelFormatCoded; break;
			case 0x108: value = displayWidth; break;
			case 0x10C: value = displayFrameBufferWidth; break;
			case 0x110: value = displayFlags; break;
			case 0x150: value = 0; break; // TODO Unknown
			case 0x154: value = 0; break; // TODO Unknown
			case 0x158: value = 0; break; // TODO Unknown
			case 0x15C: value = 0; break; // TODO Unknown
			case 0x180:
			case 0x184:
			case 0x188:
			case 0x18C:
			case 0x190: value = dmacProcessors[0].read32(address - baseAddress - 0x180); break;
			case 0x1A0:
			case 0x1A4:
			case 0x1A8:
			case 0x1AC:
			case 0x1B0: value = dmacProcessors[1].read32(address - baseAddress - 0x1A0); break;
			case 0x1C0:
			case 0x1C4:
			case 0x1C8:
			case 0x1CC:
			case 0x1D0: value = dmacProcessors[2].read32(address - baseAddress - 0x1C0); break;
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
			case 0x008: flagsCompleted &= ~value; checkInterrupt(); break;
			case 0x010: flagsError &= ~value; break;
			case 0x100: setDisplayFrameBufferAddr(value); break;
			case 0x104: setDisplayPixelFormat(value); break;
			case 0x108: setDisplayWidth(value); break;
			case 0x110: setDisplayFlags(value); break;
			case 0x10C: setDisplayFrameBufferWidth(value); break;
			case 0x160: break; // TODO reset?
			case 0x180:
			case 0x184:
			case 0x188:
			case 0x18C:
			case 0x190: dmacProcessors[0].write32(address - baseAddress - 0x180, value); break;
			case 0x1A0:
			case 0x1A4:
			case 0x1A8:
			case 0x1AC:
			case 0x1B0: dmacProcessors[1].write32(address - baseAddress - 0x1A0, value); break;
			case 0x1C0:
			case 0x1C4:
			case 0x1C8:
			case 0x1CC:
			case 0x1D0: dmacProcessors[2].write32(address - baseAddress - 0x1C0, value); break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
