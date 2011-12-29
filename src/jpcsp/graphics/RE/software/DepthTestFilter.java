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
public class DepthTestFilter {
	public static IPixelFilter getDepthTestFilter(GeContext context) {
		IPixelFilter filter = null;

		switch (context.depthFunc) {
			case GeCommands.ZTST_FUNCTION_NEVER_PASS_PIXEL:
				filter = new NeverPassFilter();
				break;
			case GeCommands.ZTST_FUNCTION_ALWAYS_PASS_PIXEL:
				filter = NopFilter.NOP;
				break;
			case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_EQUAL:
				filter = new DepthTestPassWhenDepthIsEqual();
				break;
			case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_ISNOT_EQUAL:
				filter = new DepthTestPassWhenDepthIsNotEqual();
				break;
			case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS:
				filter = new DepthTestPassWhenDepthIsLess();
				break;
			case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS_OR_EQUAL:
				filter = new DepthTestPassWhenDepthIsLessOrEqual();
				break;
			case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER:
				filter = new DepthTestPassWhenDepthIsGreater();
				break;
			case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER_OR_EQUAL:
				filter = new DepthTestPassWhenDepthIsGreaterOrEqual();
				break;
		}

		return filter;
	}

	private static abstract class DepthTest implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			if (!pass(pixel)) {
				pixel.filterPassed = false;
			}

			return pixel.source;
		}

		protected abstract boolean pass(PixelState pixel);
	}

	private static final class DepthTestPassWhenDepthIsEqual extends DepthTest {
		@Override
		protected boolean pass(PixelState pixel) {
			return pixel.sourceDepth == pixel.destinationDepth;
		}
	}

	private static final class DepthTestPassWhenDepthIsNotEqual extends DepthTest {
		@Override
		protected boolean pass(PixelState pixel) {
			return pixel.sourceDepth != pixel.destinationDepth;
		}
	}

	private static final class DepthTestPassWhenDepthIsLess extends DepthTest {
		@Override
		protected boolean pass(PixelState pixel) {
			return pixel.sourceDepth < pixel.destinationDepth;
		}
	}

	private static final class DepthTestPassWhenDepthIsLessOrEqual extends DepthTest {
		@Override
		protected boolean pass(PixelState pixel) {
			return pixel.sourceDepth <= pixel.destinationDepth;
		}
	}

	private static final class DepthTestPassWhenDepthIsGreater extends DepthTest {
		@Override
		protected boolean pass(PixelState pixel) {
			return pixel.sourceDepth > pixel.destinationDepth;
		}
	}

	private static final class DepthTestPassWhenDepthIsGreaterOrEqual extends DepthTest {
		@Override
		protected boolean pass(PixelState pixel) {
			return pixel.sourceDepth >= pixel.destinationDepth;
		}
	}
}
