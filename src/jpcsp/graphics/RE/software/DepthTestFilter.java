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

import static jpcsp.graphics.RE.software.BaseRenderer.mixIds;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class DepthTestFilter {
	public static IPixelFilter getDepthTestFilter(GeContext context) {
		IPixelFilter filter = null;

		IPixelFilter stencilOpZFail = NopFilter.NOP;
		if (context.stencilTestFlag.isEnabled()) {
			stencilOpZFail = StencilTestFilter.getStencilOp(context.stencilOpZFail, context.stencilRef, true);
		}

		if (stencilOpZFail == NopFilter.NOP) {
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
		} else {
			switch (context.depthFunc) {
				case GeCommands.ZTST_FUNCTION_NEVER_PASS_PIXEL:
					filter = new NeverPassFilterWithStencilOpFail(stencilOpZFail);
					break;
				case GeCommands.ZTST_FUNCTION_ALWAYS_PASS_PIXEL:
					// No filter required
					break;
				case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_EQUAL:
					filter = new DepthTestPassWhenDepthIsEqualWithStencilOpFail(stencilOpZFail);
					break;
				case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_ISNOT_EQUAL:
					filter = new DepthTestPassWhenDepthIsNotEqualWithStencilOpFail(stencilOpZFail);
					break;
				case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS:
					filter = new DepthTestPassWhenDepthIsLessWithStencilOpFail(stencilOpZFail);
					break;
				case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS_OR_EQUAL:
					filter = new DepthTestPassWhenDepthIsLessOrEqualWithStencilOpFail(stencilOpZFail);
					break;
				case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER:
					filter = new DepthTestPassWhenDepthIsGreaterWithStencilOpFail(stencilOpZFail);
					break;
				case GeCommands.ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER_OR_EQUAL:
					filter = new DepthTestPassWhenDepthIsGreaterOrEqualWithStencilOpFail(stencilOpZFail);
					break;
			}
		}

		return filter;
	}

	private static final class NeverPassFilterWithStencilOpFail implements IPixelFilter {
		IPixelFilter stencilOpZFail;

		public NeverPassFilterWithStencilOpFail(IPixelFilter stencilOpZFail) {
			this.stencilOpZFail = stencilOpZFail;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = false;
			pixel.filterOnFailed = stencilOpZFail;
		}

		@Override
		public int getCompilationId() {
			return mixIds(525741144, stencilOpZFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return stencilOpZFail.getFlags();
		}
	}

	private static final class DepthTestPassWhenDepthIsEqual implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth == pixel.destinationDepth;
		}

		@Override
		public int getCompilationId() {
			return 442547772;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH;
		}
	}

	private static final class DepthTestPassWhenDepthIsEqualWithStencilOpFail implements IPixelFilter {
		IPixelFilter stencilOpZFail;

		public DepthTestPassWhenDepthIsEqualWithStencilOpFail(IPixelFilter stencilOpZFail) {
			this.stencilOpZFail = stencilOpZFail;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth == pixel.destinationDepth;
			if (!pixel.filterPassed) {
				pixel.filterOnFailed = stencilOpZFail;
			}
		}

		@Override
		public int getCompilationId() {
			return mixIds(920481612, stencilOpZFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | stencilOpZFail.getFlags();
		}
	}

	private static final class DepthTestPassWhenDepthIsNotEqual implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth != pixel.destinationDepth;
		}

		@Override
		public int getCompilationId() {
			return 700601021;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH;
		}
	}

	private static final class DepthTestPassWhenDepthIsNotEqualWithStencilOpFail implements IPixelFilter {
		IPixelFilter stencilOpZFail;

		public DepthTestPassWhenDepthIsNotEqualWithStencilOpFail(IPixelFilter stencilOpZFail) {
			this.stencilOpZFail = stencilOpZFail;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth != pixel.destinationDepth;
			if (!pixel.filterPassed) {
				pixel.filterOnFailed = stencilOpZFail;
			}
		}

		@Override
		public int getCompilationId() {
			return mixIds(404958745, stencilOpZFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | stencilOpZFail.getFlags();
		}
	}

	private static final class DepthTestPassWhenDepthIsLess implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth < pixel.destinationDepth;
		}

		@Override
		public int getCompilationId() {
			return 633626864;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH;
		}
	}

	private static final class DepthTestPassWhenDepthIsLessWithStencilOpFail implements IPixelFilter {
		IPixelFilter stencilOpZFail;

		public DepthTestPassWhenDepthIsLessWithStencilOpFail(IPixelFilter stencilOpZFail) {
			this.stencilOpZFail = stencilOpZFail;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth < pixel.destinationDepth;
			if (!pixel.filterPassed) {
				pixel.filterOnFailed = stencilOpZFail;
			}
		}

		@Override
		public int getCompilationId() {
			return mixIds(337882176, stencilOpZFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | stencilOpZFail.getFlags();
		}
	}

	private static final class DepthTestPassWhenDepthIsLessOrEqual implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth <= pixel.destinationDepth;
		}

		@Override
		public int getCompilationId() {
			return 846794366;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH;
		}
	}

	private static final class DepthTestPassWhenDepthIsLessOrEqualWithStencilOpFail implements IPixelFilter {
		IPixelFilter stencilOpZFail;

		public DepthTestPassWhenDepthIsLessOrEqualWithStencilOpFail(IPixelFilter stencilOpZFail) {
			this.stencilOpZFail = stencilOpZFail;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth <= pixel.destinationDepth;
			if (!pixel.filterPassed) {
				pixel.filterOnFailed = stencilOpZFail;
			}
		}

		@Override
		public int getCompilationId() {
			return mixIds(243486869, stencilOpZFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | stencilOpZFail.getFlags();
		}
	}

	private static final class DepthTestPassWhenDepthIsGreater implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth > pixel.destinationDepth;
		}

		@Override
		public int getCompilationId() {
			return 228625488;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH;
		}
	}

	private static final class DepthTestPassWhenDepthIsGreaterWithStencilOpFail implements IPixelFilter {
		IPixelFilter stencilOpZFail;

		public DepthTestPassWhenDepthIsGreaterWithStencilOpFail(IPixelFilter stencilOpZFail) {
			this.stencilOpZFail = stencilOpZFail;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth > pixel.destinationDepth;
			if (!pixel.filterPassed) {
				pixel.filterOnFailed = stencilOpZFail;
			}
		}

		@Override
		public int getCompilationId() {
			return mixIds(716664343, stencilOpZFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | stencilOpZFail.getFlags();
		}
	}

	private static final class DepthTestPassWhenDepthIsGreaterOrEqual implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth >= pixel.destinationDepth;
		}

		@Override
		public int getCompilationId() {
			return 987316912;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH;
		}
	}

	private static final class DepthTestPassWhenDepthIsGreaterOrEqualWithStencilOpFail implements IPixelFilter {
		IPixelFilter stencilOpZFail;

		public DepthTestPassWhenDepthIsGreaterOrEqualWithStencilOpFail(IPixelFilter stencilOpZFail) {
			this.stencilOpZFail = stencilOpZFail;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth >= pixel.destinationDepth;
			if (!pixel.filterPassed) {
				pixel.filterOnFailed = stencilOpZFail;
			}
		}

		@Override
		public int getCompilationId() {
			return mixIds(572912981, stencilOpZFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | stencilOpZFail.getFlags();
		}
	}
}
