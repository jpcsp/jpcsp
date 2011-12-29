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
package jpcsp.graphics.RE.software;

import static jpcsp.graphics.RE.software.PixelColor.getBlue;
import static jpcsp.graphics.RE.software.PixelColor.getGreen;
import static jpcsp.graphics.RE.software.PixelColor.getRed;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class ColorTestFilter {
	public static IPixelFilter getColorTestFilter(GeContext context) {
		IPixelFilter filter = null;
		switch (context.colorTestFunc) {
			case GeCommands.CTST_COLOR_FUNCTION_ALWAYS_PASS_PIXEL:
				filter = NopFilter.NOP;
				break;
			case GeCommands.CTST_COLOR_FUNCTION_NEVER_PASS_PIXEL:
				filter = new NeverPassFilter();
				break;
			case GeCommands.CTST_COLOR_FUNCTION_PASS_PIXEL_IF_COLOR_MATCHES:
				filter = new ColorTestPassIfColorMatches(context.colorTestRef, context.colorTestMsk);
				break;
			case GeCommands.CTST_COLOR_FUNCTION_PASS_PIXEL_IF_COLOR_DIFFERS:
				filter = new ColorTestPassIfColorDiffers(context.colorTestRef, context.colorTestMsk);
				break;
		}

		return filter;
	}

	private static abstract class ColorTest implements IPixelFilter {
		protected int colorTestRefBlue;
		protected int colorTestRefGreen;
		protected int colorTestRefRed;
		protected int colorTestMskBlue;
		protected int colorTestMskGreen;
		protected int colorTestMskRed;

		public ColorTest(int[] colorTestRef, int[] colorTestMsk) {
			colorTestRefBlue = colorTestRef[2] & colorTestMsk[2];
			colorTestRefGreen = colorTestRef[1] & colorTestMsk[1];
			colorTestRefRed = colorTestRef[0] & colorTestMsk[0];
			colorTestMskBlue = colorTestMsk[2];
			colorTestMskGreen = colorTestMsk[1];
			colorTestMskRed = colorTestMsk[0];
		}

		protected boolean colorMatches(int color) {
			return (getBlue(color) & colorTestMskBlue) == colorTestRefBlue &&
			       (getGreen(color) & colorTestMskGreen) == colorTestRefGreen &&
			       (getRed(color) & colorTestMskRed) == colorTestRefRed;
		}
	}

	private static final class ColorTestPassIfColorMatches extends ColorTest {
		public ColorTestPassIfColorMatches(int[] colorTestRef, int[] colorTestMsk) {
			super(colorTestRef, colorTestMsk);
		}

		@Override
		public int filter(PixelState pixel) {
			if (!colorMatches(pixel.source)) {
				pixel.filterPassed = false;
		    }
			return pixel.source;
		}
	}

	private static final class ColorTestPassIfColorDiffers extends ColorTest {
		public ColorTestPassIfColorDiffers(int[] colorTestRef, int[] colorTestMsk) {
			super(colorTestRef, colorTestMsk);
		}

		@Override
		public int filter(PixelState pixel) {
			if (colorMatches(pixel.source)) {
				pixel.filterPassed = false;
		    }
			return pixel.source;
		}
	}
}
