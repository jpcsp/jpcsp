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

import jpcsp.HLE.modules.sceLcdc;
import jpcsp.hardware.Screen;

/**
 * LCD Sharp LQ043
 *
 * @author gid15
 *
 */
public class MMIOHandlerLcdc extends MMIOHandlerBase {
	public static Logger log = sceLcdc.log;
	private final LcdcController controller1 = new LcdcController();
	private final LcdcController controller2 = new LcdcController();

	private static class LcdcController {
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
			}
		}
	}

	public MMIOHandlerLcdc(int baseAddress) {
		super(baseAddress);

		reset();
	}

	private void reset() {
		controller1.reset();
		controller2.reset();
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
			case 0x04C: controller1.write32(address - baseAddress, value); break;
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
			case 0x14C: controller2.write32(address - baseAddress - 0x100, value); break;
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
