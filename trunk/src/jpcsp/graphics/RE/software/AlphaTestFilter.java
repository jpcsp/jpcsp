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

import static jpcsp.graphics.RE.software.PixelColor.getAlpha;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class AlphaTestFilter {
	public static IPixelFilter getAlphaTestFilter(GeContext context) {
		IPixelFilter filter = null;

		switch (context.alphaFunc) {
			case GeCommands.ATST_ALWAYS_PASS_PIXEL:
				filter = NopFilter.NOP;
				break;
			case GeCommands.ATST_NEVER_PASS_PIXEL:
				filter = new NeverPassFilter();
				break;
			case GeCommands.ATST_PASS_PIXEL_IF_MATCHES:
				filter = new AlphaFunctionPassIfMatches(context.alphaRef);
				break;
			case GeCommands.ATST_PASS_PIXEL_IF_DIFFERS:
				filter = new AlphaFunctionPassIfDiffers(context.alphaRef);
				break;
			case GeCommands.ATST_PASS_PIXEL_IF_LESS:
				filter = new AlphaFunctionPassIfLess(context.alphaRef);
				break;
			case GeCommands.ATST_PASS_PIXEL_IF_LESS_OR_EQUAL:
				// No test if alphaRef==0xFF
				if (context.alphaRef < 0xFF) {
					filter = new AlphaFunctionPassIfLessOrEqual(context.alphaRef);
				}
				break;
			case GeCommands.ATST_PASS_PIXEL_IF_GREATER:
				filter = new AlphaFunctionPassIfGreater(context.alphaRef);
				break;
			case GeCommands.ATST_PASS_PIXEL_IF_GREATER_OR_EQUAL:
				// No test if alphaRef==0x00
				if (context.alphaRef > 0x00) {
					filter = new AlphaFunctionPassIfGreaterOrEqual(context.alphaRef);
				}
				break;
		}

		return filter;
	}

	private static final class AlphaFunctionPassIfMatches implements IPixelFilter {
		private int alphaReferenceValue;

		public AlphaFunctionPassIfMatches(int alphaReferenceValue) {
			this.alphaReferenceValue = alphaReferenceValue;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = getAlpha(pixel.source) == alphaReferenceValue;
		}

		@Override
		public int getCompilationId() {
			return 960150625;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class AlphaFunctionPassIfDiffers implements IPixelFilter {
		private int alphaReferenceValue;

		public AlphaFunctionPassIfDiffers(int alphaReferenceValue) {
			this.alphaReferenceValue = alphaReferenceValue;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = getAlpha(pixel.source) != alphaReferenceValue;
		}

		@Override
		public int getCompilationId() {
			return 205574712;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class AlphaFunctionPassIfLess implements IPixelFilter {
		private int alphaReferenceValue;

		public AlphaFunctionPassIfLess(int alphaReferenceValue) {
			this.alphaReferenceValue = alphaReferenceValue;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = getAlpha(pixel.source) < alphaReferenceValue;
		}

		@Override
		public int getCompilationId() {
			return 254016514;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class AlphaFunctionPassIfLessOrEqual implements IPixelFilter {
		private int alphaReferenceValue;

		public AlphaFunctionPassIfLessOrEqual(int alphaReferenceValue) {
			this.alphaReferenceValue = alphaReferenceValue;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = getAlpha(pixel.source) <= alphaReferenceValue;
		}

		@Override
		public int getCompilationId() {
			return 314931959;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class AlphaFunctionPassIfGreater implements IPixelFilter {
		private int alphaReferenceValue;

		public AlphaFunctionPassIfGreater(int alphaReferenceValue) {
			this.alphaReferenceValue = alphaReferenceValue;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = getAlpha(pixel.source) > alphaReferenceValue;
		}

		@Override
		public int getCompilationId() {
			return 850794903;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class AlphaFunctionPassIfGreaterOrEqual implements IPixelFilter {
		private int alphaReferenceValue;

		public AlphaFunctionPassIfGreaterOrEqual(int alphaReferenceValue) {
			this.alphaReferenceValue = alphaReferenceValue;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = getAlpha(pixel.source) >= alphaReferenceValue;
		}

		@Override
		public int getCompilationId() {
			return 161777271;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}
}
