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

import static jpcsp.Emulator.getProcessor;
import static jpcsp.HLE.Modules.sceDisplayModule;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_DMACPLUS_INTR;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.modules.sceDmacplus;

public class MMIOHandlerDmacplus extends MMIOHandlerBase {
	public static final int FLAG_UNKNOWN      = 0x01;
	public static final int FLAG_AVC          = 0x02;
	public static final int FLAG_SC2ME        = 0x04;
	public static final int FLAG_ME2SC        = 0x08;
	public static final int FLAG_SC128_MEMCPY = 0x10;
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
	private int memcpySource;
	private int memcpyDestination;
	private int memcpyAttributes;
	private int displayFrameBufferAddr;
	private int displayWidth; // E.g. 480, must be a multiple of 8
	private int displayFrameBufferWidth; // E.g. 512, must be a multiple of 64
	private int displayPixelFormatCoded; // Values: [0..3]
	public static final int DISPLAY_FLAG_ENABLED = 0x1;
	public static final int DISPLAY_FLAG_UNKNOWN = 0x2;
	private int displayFlags;

	public MMIOHandlerDmacplus(int baseAddress) {
		super(baseAddress);
	}

	private void startMemcpy(int value) {
		int memcpyLengthShift = (memcpyAttributes >> 18) & 0x7;
		int memcpyLength = (memcpyAttributes & 0xFFF) << memcpyLengthShift;

		if (log.isDebugEnabled()) {
			log.debug(String.format("startMemcpy dst=0x%08X, src=0x%08X, length=0x%X", memcpyDestination, memcpySource, memcpyLength));
		}

		RuntimeContextLLE.getMMIO().memcpy(memcpyDestination, memcpySource, memcpyAttributes);
		flagsCompleted |= FLAG_SC128_MEMCPY;

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
		sceDisplayModule.hleDisplaySetFrameBuf(displayFrameBufferAddr, displayFrameBufferWidth, displayPixelFormat, 0);
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
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", Emulator.getProcessor().cpu.pc, address, value));
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
			case 0x1C0: memcpySource = value; break;
			case 0x1C4: memcpyDestination = value; break;
			case 0x1CC: memcpyAttributes = value; break;
			case 0x1D0: startMemcpy(value); break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", Emulator.getProcessor().cpu.pc, address, value, this));
		}
	}
}
