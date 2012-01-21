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

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class StencilTestFilter {
	public static IPixelFilter getStencilTestFilter(GeContext context) {
		IPixelFilter filter = null;

		switch (context.stencilFunc) {
			case GeCommands.STST_FUNCTION_NEVER_PASS_STENCIL_TEST:
				filter = new NeverPassFilter();
				break;
			case GeCommands.STST_FUNCTION_ALWAYS_PASS_STENCIL_TEST:
				filter = NopFilter.NOP;
				break;
			case GeCommands.STST_FUNCTION_PASS_TEST_IF_MATCHES:
				filter = new StencilTestPassIfMatches(context.stencilRef, context.stencilMask);
				break;
			case GeCommands.STST_FUNCTION_PASS_TEST_IF_DIFFERS:
				filter = new StencilTestPassIfDiffers(context.stencilRef, context.stencilMask);
				break;
			case GeCommands.STST_FUNCTION_PASS_TEST_IF_LESS:
				filter = new StencilTestPassIfLess(context.stencilRef, context.stencilMask);
				break;
			case GeCommands.STST_FUNCTION_PASS_TEST_IF_LESS_OR_EQUAL:
				filter = new StencilTestPassIfLessOrEqual(context.stencilRef, context.stencilMask);
				break;
			case GeCommands.STST_FUNCTION_PASS_TEST_IF_GREATER:
				filter = new StencilTestPassIfGreater(context.stencilRef, context.stencilMask);
				break;
			case GeCommands.STST_FUNCTION_PASS_TEST_IF_GREATER_OR_EQUAL:
				filter = new StencilTestPassIfGreaterOrEqual(context.stencilRef, context.stencilMask);
				break;
		}

		return filter;
	}

	private static final class StencilTestPassIfMatches implements IPixelFilter {
		protected int stencilRef;
		protected int stencilMask;

		public StencilTestPassIfMatches(int stencilRef, int stencilMask) {
			this.stencilRef = stencilRef & stencilMask;
			this.stencilMask = stencilMask;
		}

		@Override
		public void filter(PixelState pixel) {
			int stencilValue = PixelColor.getAlpha(pixel.destination);
			pixel.filterPassed = (stencilValue & stencilMask) == stencilRef;
		}
	}

	private static final class StencilTestPassIfDiffers implements IPixelFilter {
		protected int stencilRef;
		protected int stencilMask;

		public StencilTestPassIfDiffers(int stencilRef, int stencilMask) {
			this.stencilRef = stencilRef & stencilMask;
			this.stencilMask = stencilMask;
		}

		@Override
		public void filter(PixelState pixel) {
			int stencilValue = PixelColor.getAlpha(pixel.destination);
			pixel.filterPassed = (stencilValue & stencilMask) != stencilRef;
		}
	}

	private static final class StencilTestPassIfLess implements IPixelFilter {
		protected int stencilRef;
		protected int stencilMask;

		public StencilTestPassIfLess(int stencilRef, int stencilMask) {
			this.stencilRef = stencilRef & stencilMask;
			this.stencilMask = stencilMask;
		}

		@Override
		public void filter(PixelState pixel) {
			int stencilValue = PixelColor.getAlpha(pixel.destination);
			pixel.filterPassed = (stencilValue & stencilMask) < stencilRef;
		}
	}

	private static final class StencilTestPassIfLessOrEqual implements IPixelFilter {
		protected int stencilRef;
		protected int stencilMask;

		public StencilTestPassIfLessOrEqual(int stencilRef, int stencilMask) {
			this.stencilRef = stencilRef & stencilMask;
			this.stencilMask = stencilMask;
		}

		@Override
		public void filter(PixelState pixel) {
			int stencilValue = PixelColor.getAlpha(pixel.destination);
			pixel.filterPassed = (stencilValue & stencilMask) <= stencilRef;
		}
	}

	private static final class StencilTestPassIfGreater implements IPixelFilter {
		protected int stencilRef;
		protected int stencilMask;

		public StencilTestPassIfGreater(int stencilRef, int stencilMask) {
			this.stencilRef = stencilRef & stencilMask;
			this.stencilMask = stencilMask;
		}

		@Override
		public void filter(PixelState pixel) {
			int stencilValue = PixelColor.getAlpha(pixel.destination);
			pixel.filterPassed = (stencilValue & stencilMask) > stencilRef;
		}
	}

	private static final class StencilTestPassIfGreaterOrEqual implements IPixelFilter {
		protected int stencilRef;
		protected int stencilMask;

		public StencilTestPassIfGreaterOrEqual(int stencilRef, int stencilMask) {
			this.stencilRef = stencilRef & stencilMask;
			this.stencilMask = stencilMask;
		}

		@Override
		public void filter(PixelState pixel) {
			int stencilValue = PixelColor.getAlpha(pixel.destination);
			pixel.filterPassed = (stencilValue & stencilMask) >= stencilRef;
		}
	}
}
