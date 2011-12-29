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
				filter = new AlphaFunctionPassIfLessOrEqual(context.alphaRef);
				break;
			case GeCommands.ATST_PASS_PIXEL_IF_GREATER:
				filter = new AlphaFunctionPassIfGreater(context.alphaRef);
				break;
			case GeCommands.ATST_PASS_PIXEL_IF_GREATER_OR_EQUAL:
				filter = new AlphaFunctionPassIfGreaterOrEqual(context.alphaRef);
				break;
		}

		return filter;
	}

	private static abstract class AlphaFunction implements IPixelFilter {
		protected int alphaReferenceValue;

		public AlphaFunction(int alphaReferenceValue) {
			this.alphaReferenceValue = alphaReferenceValue;
		}

		@Override
		public int filter(PixelState pixel) {
			if (!pass(getAlpha(pixel.source))) {
				pixel.filterPassed = false;
			}
			return pixel.source;
		}

		protected abstract boolean pass(int alpha);
	}

	private static final class AlphaFunctionPassIfMatches extends AlphaFunction {
		public AlphaFunctionPassIfMatches(int alphaReferenceValue) {
			super(alphaReferenceValue);
		}

		@Override
		protected boolean pass(int alpha) {
			return alpha == alphaReferenceValue;
		}
	}

	private static final class AlphaFunctionPassIfDiffers extends AlphaFunction {
		public AlphaFunctionPassIfDiffers(int alphaReferenceValue) {
			super(alphaReferenceValue);
		}

		@Override
		protected boolean pass(int alpha) {
			return alpha != alphaReferenceValue;
		}
	}

	private static final class AlphaFunctionPassIfLess extends AlphaFunction {
		public AlphaFunctionPassIfLess(int alphaReferenceValue) {
			super(alphaReferenceValue);
		}

		@Override
		protected boolean pass(int alpha) {
			return alpha < alphaReferenceValue;
		}
	}

	private static final class AlphaFunctionPassIfLessOrEqual extends AlphaFunction {
		public AlphaFunctionPassIfLessOrEqual(int alphaReferenceValue) {
			super(alphaReferenceValue);
		}

		@Override
		protected boolean pass(int alpha) {
			return alpha <= alphaReferenceValue;
		}
	}

	private static final class AlphaFunctionPassIfGreater extends AlphaFunction {
		public AlphaFunctionPassIfGreater(int alphaReferenceValue) {
			super(alphaReferenceValue);
		}

		@Override
		protected boolean pass(int alpha) {
			return alpha > alphaReferenceValue;
		}
	}

	private static final class AlphaFunctionPassIfGreaterOrEqual extends AlphaFunction {
		public AlphaFunctionPassIfGreaterOrEqual(int alphaReferenceValue) {
			super(alphaReferenceValue);
		}

		@Override
		protected boolean pass(int alpha) {
			return alpha >= alphaReferenceValue;
		}
	}
}
