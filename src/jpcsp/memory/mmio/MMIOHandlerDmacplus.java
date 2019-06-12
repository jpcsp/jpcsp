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
import static jpcsp.HLE.modules.sceDisplay.PSP_DISPLAY_SETBUF_IMMEDIATE;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceMp4AvcCscStruct;
import jpcsp.HLE.modules.sceDmacplus;
import jpcsp.HLE.modules.sceMpegbase;
import jpcsp.mediaengine.MEProcessor;
import jpcsp.memory.mmio.dmac.DmacProcessor;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerDmacplus extends MMIOHandlerBase {
	public static Logger log = sceDmacplus.log;
	private static final int STATE_VERSION = 0;
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
	private final int[] mpegAvcYuvBuffers = new int[8];
	private int mpegAvcWidth;
	private int mpegAvcHeight;
	private int mpegAvcMode0;
	private int mpegAvcMode1;
	private int mpegAvcBufferWidth;
	private int mpegAvcInternalPixelMode;
	private int mpegAvcBufferUnknownPresent;
	private int mpegAvcBufferRGB;
	private int mpegAvcBufferUnknown;
	private final int[] mpegAvcCodes = new int[4];

	private class DmacCompletedAction implements IAction {
		private int flagCompleted;

		public DmacCompletedAction(int flagCompleted) {
			this.flagCompleted = flagCompleted;
		}

		@Override
		public void execute() {
			setCompleted(flagCompleted);
		}
	}

	public MMIOHandlerDmacplus(int baseAddress) {
		super(baseAddress);

		Memory meMemory = MEProcessor.getInstance().getMEMemory();
		Memory scMemory = getMemory();
		dmacProcessors[0] = new DmacProcessor(scMemory, meMemory, baseAddress + 0x180, new DmacCompletedAction(COMPLETED_FLAG_SC2ME));
		dmacProcessors[1] = new DmacProcessor(meMemory, scMemory, baseAddress + 0x1A0, new DmacCompletedAction(COMPLETED_FLAG_ME2SC));
		dmacProcessors[2] = new DmacProcessor(scMemory, scMemory, baseAddress + 0x1C0, new DmacCompletedAction(COMPLETED_FLAG_SC128_MEMCPY));

		updateDisplay();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		flagsCompleted = stream.readInt();
		flagsError = stream.readInt();
		for (int i = 0; i < dmacProcessors.length; i++) {
			dmacProcessors[i].read(stream);
		}
		displayFrameBufferAddr = stream.readInt();
		displayWidth = stream.readInt();
		displayFrameBufferWidth = stream.readInt();
		displayPixelFormatCoded = stream.readInt();
		displayFlags = stream.readInt();
		super.read(stream);

		updateDisplay();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(flagsCompleted);
		stream.writeInt(flagsError);
		for (int i = 0; i < dmacProcessors.length; i++) {
			dmacProcessors[i].write(stream);
		}
		stream.writeInt(displayFrameBufferAddr);
		stream.writeInt(displayWidth);
		stream.writeInt(displayFrameBufferWidth);
		stream.writeInt(displayPixelFormatCoded);
		stream.writeInt(displayFlags);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		flagsCompleted = 0;
		flagsError = 0;
		for (int i = 0; i < dmacProcessors.length; i++) {
			dmacProcessors[i].reset();
		}
		displayFrameBufferAddr = 0;
		displayWidth = 0;
		displayFrameBufferWidth = 0;
		displayPixelFormatCoded = 0;
		displayFlags = 0;
		updateDisplay();
	}

	private void setCompleted(int flagCompleted) {
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

	public void setDisplayPixelFormat(int displayPixelFormatCoded) {
		if (this.displayPixelFormatCoded != displayPixelFormatCoded) {
			this.displayPixelFormatCoded = displayPixelFormatCoded;
			updateDisplay();
		}
	}

	public void setDisplayFlags(int displayFlags) {
		if (this.displayFlags != displayFlags) {
			this.displayFlags = displayFlags;
			updateDisplay();
		}
	}

	private void updateDisplay() {
		int displayPixelFormat = sceDmacplus.pixelFormatFromCode[displayPixelFormatCoded & 0x3];
		int frameBufferAddr = displayFrameBufferAddr;
		if ((displayFlags & DISPLAY_FLAG_ENABLED) == 0) {
			frameBufferAddr = 0;
		}

		// hleDisplaySetFrameBuf is returning an error when bufferWidth == 0 and frameBufferAddr != 0
		if (displayFrameBufferWidth == 0) {
			frameBufferAddr = 0;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("updateDisplay.hleDisplaySetFrameBuf frameBufferAddr=0x%08X, displayFrameBufferWidth=0x%X, displayPixelFormat=0x%X, displayFlags=0x%X", frameBufferAddr, displayFrameBufferWidth, displayPixelFormat, displayFlags));
		}

		sceDisplayModule.hleDisplaySetFrameBuf(frameBufferAddr, displayFrameBufferWidth, displayPixelFormat, PSP_DISPLAY_SETBUF_IMMEDIATE);
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
			case 0x150: value = mpegAvcCodes[0]; break;
			case 0x154: value = mpegAvcCodes[1]; break;
			case 0x158: value = mpegAvcCodes[2]; break;
			case 0x15C: value = mpegAvcCodes[3]; break;
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

	private void setMpegAvc140(int value) {
		mpegAvcHeight = (value >> 16) & 0x3F;
		mpegAvcWidth = (value >> 8) & 0x3F;
		mpegAvcMode0 = (value >> 2) & 0x1;
		mpegAvcMode1 = (value >> 1) & 0x1;
	}

	private void setMpegAvc14C(int value) {
		mpegAvcBufferWidth = value >>> 8;
		mpegAvcInternalPixelMode = (value >> 1) & 0x3;
		mpegAvcBufferUnknownPresent = (value >> 0) & 0x1;
	}

	private void setMpegAvcCmd(int cmd) {
		switch (cmd) {
			case 0x0:
				break;
			case 0xD:
				// Start sceMpegBaseCscAvc
				if (mpegAvcCodes[0] != 0x0CC00095 || mpegAvcCodes[1] != 0x398F3895 || mpegAvcCodes[2] != 0x00040895 || mpegAvcCodes[3] != 0x00000110) {
					log.error(String.format("setMpegAvcCmd sceMpegBaseCscAvc unknown mpegAvcCodes 0x%08X, 0x%08X, 0x%08X, 0x%08X", mpegAvcCodes[0], mpegAvcCodes[1], mpegAvcCodes[2], mpegAvcCodes[3]));
				}

				if (log.isDebugEnabled()) {
					log.debug(String.format("setMpegAvcCmd sceMpegBaseCscAvc bufferRGB=0x%08X, bufferUnknown=0x%08X, width=0x%X, height=0x%X, bufferWidth=0x%X, mode0=0x%X, mode1=0x%X, internalPixelMode=0x%X, bufferUnknownPresent=0x%X", mpegAvcBufferRGB, mpegAvcBufferUnknown, mpegAvcWidth, mpegAvcHeight, mpegAvcBufferWidth, mpegAvcMode0, mpegAvcMode1, mpegAvcInternalPixelMode, mpegAvcBufferUnknownPresent));
				}

				int pixelMode = sceMpegbase.getPixelMode(mpegAvcInternalPixelMode);
				TPointer bufferRGB = new TPointer(Emulator.getMemory(mpegAvcBufferRGB), mpegAvcBufferRGB);
				TPointer bufferUnknown = mpegAvcBufferUnknownPresent != 0 ? new TPointer(Emulator.getMemory(mpegAvcBufferUnknown), mpegAvcBufferUnknown) : TPointer.NULL;
				SceMp4AvcCscStruct mp4AvcCscStruct = new SceMp4AvcCscStruct();
				mp4AvcCscStruct.height = mpegAvcHeight;
				mp4AvcCscStruct.width = mpegAvcWidth;
				mp4AvcCscStruct.mode0 = mpegAvcMode0;
				mp4AvcCscStruct.mode1 = mpegAvcMode1;
				mp4AvcCscStruct.buffer0 = mpegAvcYuvBuffers[0];
				mp4AvcCscStruct.buffer1 = mpegAvcYuvBuffers[1];
				mp4AvcCscStruct.buffer2 = mpegAvcYuvBuffers[2];
				mp4AvcCscStruct.buffer3 = mpegAvcYuvBuffers[3];
				mp4AvcCscStruct.buffer4 = mpegAvcYuvBuffers[4];
				mp4AvcCscStruct.buffer5 = mpegAvcYuvBuffers[5];
				mp4AvcCscStruct.buffer6 = mpegAvcYuvBuffers[6];
				mp4AvcCscStruct.buffer7 = mpegAvcYuvBuffers[7];
				mp4AvcCscStruct.bufferMemory = MEProcessor.getInstance().getMEMemory();
				Modules.sceMpegbaseModule.hleMpegBaseCscAvc(bufferRGB, bufferUnknown, mpegAvcBufferWidth, pixelMode, mp4AvcCscStruct);

				setCompleted(COMPLETED_FLAG_AVC);
				break;
			default:
				log.error(String.format("setMpegAvcCmd unknown cmd=0x%X", cmd));
				break;
		}
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x008: flagsCompleted &= ~value; checkInterrupt(); break;
			case 0x010: flagsError &= ~value; break;
			case 0x100: setDisplayFrameBufferAddr(value); break;
			case 0x104: setDisplayPixelFormat(value); break;
			case 0x108: setDisplayWidth(value); break;
			case 0x10C: setDisplayFrameBufferWidth(value); break;
			case 0x110: setDisplayFlags(value); break;
			case 0x120: mpegAvcYuvBuffers[0] = value; break;
			case 0x124: mpegAvcYuvBuffers[1] = value; break;
			case 0x128: mpegAvcYuvBuffers[2] = value; break;
			case 0x12C: mpegAvcYuvBuffers[3] = value; break;
			case 0x130: mpegAvcYuvBuffers[4] = value; break;
			case 0x134: mpegAvcYuvBuffers[5] = value; break;
			case 0x138: mpegAvcYuvBuffers[6] = value; break;
			case 0x13C: mpegAvcYuvBuffers[7] = value; break;
			case 0x140: setMpegAvc140(value); break;
			case 0x144: mpegAvcBufferRGB = value; break;
			case 0x148: mpegAvcBufferUnknown = value; break;
			case 0x14C: setMpegAvc14C(value); break;
			case 0x150: mpegAvcCodes[0] = value; break;
			case 0x154: mpegAvcCodes[1] = value; break;
			case 0x158: mpegAvcCodes[2] = value; break;
			case 0x15C: mpegAvcCodes[3] = value; break;
			case 0x160: setMpegAvcCmd(value); break;
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
