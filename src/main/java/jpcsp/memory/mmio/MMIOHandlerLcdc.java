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

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.HLE.modules.sceLcdc;
import jpcsp.hardware.Screen;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * LCD Sharp LQ043
 *
 * @author gid15
 *
 */
public class MMIOHandlerLcdc extends MMIOHandlerBase {
	public static Logger log = sceLcdc.log;
	private static final int STATE_VERSION = 0;
	private final LcdcController controller1 = new LcdcController();
	private final LcdcController controller2 = new LcdcController();
	// Used only for tachyon version >= 0x00800000
	private int enabled;
	private int scaledXResolution;
	private int yResolution;
	private int unknown18C;
	private int unknown190;
	private int unknown194;
	private int unknown198;
	private int displayFlags;
	private int displayClock;

	private static class LcdcController {
		private static final int STATE_VERSION = 0;
		// The register names are based on https://github.com/uofw/upspd/wiki/Hardware-registers
		public int enable;
		public int synchronizationDifference;
		public int unknown008;
		//
		// The Hsync period is divided in back porch, resolution, front porch:
		//
		// <--------------------Hsync period------------------->
		// <-xBackPorch-><-----xResolution------><-xFrontPorch->
		//
		// The Vsync period is divided in back porch, resolution and front porch:
		//
		//     ^              ^
		//     | yBackPorch   |
		//     v              |
		//     ^              |
		//     |              |
		//     | yResolution  | Vsync period
		//     |              |
		//     v              |
		//     ^              |
		//     | yFrontPorch  |
		//     v              v
		public int xBackPorch;
		public int xPulseWidth;
		public int xFrontPorch;
		public int xResolution;
		public int yBackPorch;
		public int yPulseWidth;
		public int yFrontPorch;
		public int yResolution;
		public int yShift;
		public int xShift;
		public int scaledXResolution;
		public int scaledYResolution;
		public int resume;

		public void reset() {
			enable = 0;
			synchronizationDifference = 0;
			unknown008 = 0;

			xPulseWidth = 41;
			xBackPorch = 2;
			xFrontPorch = 2;
			xResolution = Screen.width;

			yPulseWidth = 10;
			yBackPorch = 2;
			yFrontPorch = 2;
			yResolution = Screen.height;

			yShift = 0x00;
			xShift = 0x00;
			scaledXResolution = Screen.width;
			scaledYResolution = Screen.height;

			resume = 0;
		}

		public int read32(int offset) {
			int value = 0;

			switch (offset) {
				case 0x000: value = enable; break;
				case 0x004: value = synchronizationDifference; break;
				case 0x008: value = unknown008; break;
				case 0x010: value = xPulseWidth; break;
				case 0x014: value = xBackPorch; break;
				case 0x018: value = xFrontPorch; break;
				case 0x01C: value = xResolution; break;
				case 0x020: value = yBackPorch; break;
				case 0x024: value = yFrontPorch; break;
				case 0x028: value = yPulseWidth; break;
				case 0x02C: value = yResolution; break;
				case 0x040: value = yShift; break;
				case 0x044: value = xShift; break;
				case 0x048: value = scaledXResolution; break;
				case 0x04C: value = scaledYResolution; break;
				case 0x050: value = 0x01; break;
			}

			return value;
		}

		public void write32(int offset, int value) {
			switch (offset) {
				case 0x000: enable = value; break;
				case 0x004: synchronizationDifference = value; break;
				case 0x008: unknown008 = value; break;
				case 0x010: xPulseWidth = value; break;
				case 0x014: xBackPorch = value; break;
				case 0x018: xFrontPorch = value; break;
				case 0x01C: xResolution = value; break;
				case 0x020: yBackPorch = value; break;
				case 0x024: yFrontPorch = value; break;
				case 0x028: yPulseWidth = value; break;
				case 0x02C: yResolution = value; break;
				case 0x040: yShift = value; break;
				case 0x044: xShift = value; break;
				case 0x048: scaledXResolution = value; break;
				case 0x04C: scaledYResolution = value; break;
				case 0x070: resume = value; break;
			}
		}

		public void read(StateInputStream stream) throws IOException {
			stream.readVersion(STATE_VERSION);
			enable = stream.readInt();
			synchronizationDifference = stream.readInt();
			unknown008 = stream.readInt();
			xPulseWidth = stream.readInt();
			xBackPorch = stream.readInt();
			xFrontPorch = stream.readInt();
			xResolution = stream.readInt();
			yBackPorch = stream.readInt();
			yFrontPorch = stream.readInt();
			yPulseWidth = stream.readInt();
			yResolution = stream.readInt();
			yShift = stream.readInt();
			xShift = stream.readInt();
			scaledXResolution = stream.readInt();
			scaledYResolution = stream.readInt();
			resume = stream.readInt();
		}

		public void write(StateOutputStream stream) throws IOException {
			stream.writeVersion(STATE_VERSION);
			stream.writeInt(enable);
			stream.writeInt(synchronizationDifference);
			stream.writeInt(unknown008);
			stream.writeInt(xPulseWidth);
			stream.writeInt(xBackPorch);
			stream.writeInt(xFrontPorch);
			stream.writeInt(xResolution);
			stream.writeInt(yBackPorch);
			stream.writeInt(yFrontPorch);
			stream.writeInt(yPulseWidth);
			stream.writeInt(yResolution);
			stream.writeInt(yShift);
			stream.writeInt(xShift);
			stream.writeInt(scaledXResolution);
			stream.writeInt(scaledYResolution);
			stream.writeInt(resume);
		}
	}

	public MMIOHandlerLcdc(int baseAddress) {
		super(baseAddress);

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		controller1.read(stream);
		controller2.read(stream);
		enabled = stream.readInt();
		scaledXResolution = stream.readInt();
		yResolution = stream.readInt();
		unknown18C = stream.readInt();
		unknown190 = stream.readInt();
		unknown194 = stream.readInt();
		unknown198 = stream.readInt();
		displayFlags = stream.readInt();
		displayClock = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		controller1.write(stream);
		controller2.write(stream);
		stream.writeInt(enabled);
		stream.writeInt(scaledXResolution);
		stream.writeInt(yResolution);
		stream.writeInt(unknown18C);
		stream.writeInt(unknown190);
		stream.writeInt(unknown194);
		stream.writeInt(unknown198);
		stream.writeInt(displayFlags);
		stream.writeInt(displayClock);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		controller1.reset();
		controller2.reset();
		enabled = 0;
		scaledXResolution = 0;
		yResolution = 0;
		unknown18C = 0;
		unknown190 = 0;
		unknown194 = 0;
		unknown198 = 0;
		displayFlags = 0;
		displayClock = 0;
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x000:
			case 0x004:
			case 0x008:
			case 0x010:
			case 0x014:
			case 0x018:
			case 0x01C:
			case 0x020:
			case 0x024:
			case 0x028:
			case 0x02C:
			case 0x040:
			case 0x044:
			case 0x048:
			case 0x04C:
			case 0x050: value = controller1.read32(address - baseAddress); break;
			case 0x100:
			case 0x104:
			case 0x108:
			case 0x110:
			case 0x114:
			case 0x118:
			case 0x11C:
			case 0x120:
			case 0x124:
			case 0x128:
			case 0x12C:
			case 0x140:
			case 0x144:
			case 0x148:
			case 0x14C:
			case 0x150: value = controller2.read32(address - baseAddress - 0x100); break;
			case 0x180: value = enabled; break;
			case 0x184: value = scaledXResolution; break;
			case 0x188: value = yResolution; break;
			case 0x18C: value = unknown18C; break;
			case 0x190: value = unknown190; break;
			case 0x194: value = unknown194; break;
			case 0x198: value = unknown198; break;
			case 0x1A0: value = displayFlags; break;
			case 0x1B0: value = displayClock; break;
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
			case 0x010:
			case 0x014:
			case 0x018:
			case 0x01C:
			case 0x020:
			case 0x024:
			case 0x028:
			case 0x02C:
			case 0x040:
			case 0x044:
			case 0x048:
			case 0x04C:
			case 0x070: controller1.write32(address - baseAddress, value); break;
			case 0x100:
			case 0x104:
			case 0x108:
			case 0x110:
			case 0x114:
			case 0x118:
			case 0x11C:
			case 0x120:
			case 0x124:
			case 0x128:
			case 0x12C:
			case 0x140:
			case 0x144:
			case 0x148:
			case 0x14C:
			case 0x170: controller2.write32(address - baseAddress - 0x100, value); break;
			case 0x180: enabled = value; break;
			case 0x184: scaledXResolution = value; break;
			case 0x188: yResolution = value; break;
			case 0x18C: unknown18C = value; break;
			case 0x190: unknown190 = value; break;
			case 0x194: unknown194 = value; break;
			case 0x198: unknown198 = value; break;
			case 0x1A0: displayFlags = value; break;
			case 0x1B0: displayClock = value; break;
			case 0x200: if (value != 1) { super.write32(address, value); } break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public String toString() {
		return String.format("MMIOHandlerLcdc");
	}
}
