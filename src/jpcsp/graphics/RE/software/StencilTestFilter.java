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
public class StencilTestFilter {
	public static IPixelFilter getStencilTestFilter(GeContext context) {
		IPixelFilter filter = null;

		IPixelFilter opFail = getStencilOp(context.stencilOpFail, context.stencilRef, true);

		if (opFail == NopFilter.NOP) {
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
		} else {
			switch (context.stencilFunc) {
				case GeCommands.STST_FUNCTION_NEVER_PASS_STENCIL_TEST:
					filter = new NeverPassFilterWithOpFail(opFail);
					break;
				case GeCommands.STST_FUNCTION_ALWAYS_PASS_STENCIL_TEST:
					filter = NopFilter.NOP;
					break;
				case GeCommands.STST_FUNCTION_PASS_TEST_IF_MATCHES:
					filter = new StencilTestPassIfMatchesWithOpFail(context.stencilRef, context.stencilMask, opFail);
					break;
				case GeCommands.STST_FUNCTION_PASS_TEST_IF_DIFFERS:
					filter = new StencilTestPassIfDiffersWithOpFail(context.stencilRef, context.stencilMask, opFail);
					break;
				case GeCommands.STST_FUNCTION_PASS_TEST_IF_LESS:
					filter = new StencilTestPassIfLessWithOpFail(context.stencilRef, context.stencilMask, opFail);
					break;
				case GeCommands.STST_FUNCTION_PASS_TEST_IF_LESS_OR_EQUAL:
					filter = new StencilTestPassIfLessOrEqualWithOpFail(context.stencilRef, context.stencilMask, opFail);
					break;
				case GeCommands.STST_FUNCTION_PASS_TEST_IF_GREATER:
					filter = new StencilTestPassIfGreaterWithOpFail(context.stencilRef, context.stencilMask, opFail);
					break;
				case GeCommands.STST_FUNCTION_PASS_TEST_IF_GREATER_OR_EQUAL:
					filter = new StencilTestPassIfGreaterOrEqualWithOpFail(context.stencilRef, context.stencilMask, opFail);
					break;
			}
		}

		return filter;
	}

	public static IPixelFilter getStencilOp(int stencilOp, int stencilRef, boolean isFailOp) {
		IPixelFilter filter = NopFilter.NOP;

		switch (stencilOp) {
			case GeCommands.SOP_KEEP_STENCIL_VALUE:
				// Nothing to do in case of a failed test
				if (!isFailOp) {
					filter = new StencilOpKeep();
				}
				break;
			case GeCommands.SOP_ZERO_STENCIL_VALUE:
				filter = new StencilOpZero();
				break;
			case GeCommands.SOP_REPLACE_STENCIL_VALUE:
				if (stencilRef == 0) {
					// SOP_REPLACE_STENCIL_VALUE with a 0 value is equivalent
					// to SOP_ZERO_STENCIL_VALUE
					filter = new StencilOpZero();
				} else {
					filter = new StencilOpReplace(stencilRef);
				}
				break;
			case GeCommands.SOP_INVERT_STENCIL_VALUE:
				filter = new StencilOpInvert();
				break;
			case GeCommands.SOP_INCREMENT_STENCIL_VALUE:
				filter = new StencilOpIncrement();
				break;
			case GeCommands.SOP_DECREMENT_STENCIL_VALUE:
				filter = new StencilOpDecrement();
				break;
		}

		return filter;
	}

	public static final class StencilOpKeep implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = (pixel.source & 0x00FFFFFF) | (pixel.destination & 0xFF000000);
		}

		@Override
		public int getCompilationId() {
			return 22562764;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	public static final class StencilOpZero implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source &= 0x00FFFFFF;
		}

		@Override
		public int getCompilationId() {
			return 526794456;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	public static final class StencilOpReplace implements IPixelFilter {
		protected int alpha;

		public StencilOpReplace(int stencilRef) {
			alpha = stencilRef << 24;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.source = (pixel.source & 0x00FFFFFF) | alpha;
		}

		@Override
		public int getCompilationId() {
			return 278836038;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	public static final class StencilOpInvert implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = (pixel.source & 0x00FFFFFF) | ((~pixel.destination) & 0xFF000000);
		}

		@Override
		public int getCompilationId() {
			return 430888637;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	public static final class StencilOpIncrement implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			int alpha = pixel.destination & 0xFF000000;
			if (alpha != 0xFF000000) {
				alpha += 0x01000000;
			}
			pixel.source = (pixel.source & 0x00FFFFFF) | alpha;
		}

		@Override
		public int getCompilationId() {
			return 349450655;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	public static final class StencilOpDecrement implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			int alpha = pixel.destination & 0xFF000000;
			if (alpha != 0x00000000) {
				alpha -= 0x01000000;
			}
			pixel.source = (pixel.source & 0x00FFFFFF) | alpha;
		}

		@Override
		public int getCompilationId() {
			return 994050166;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class NeverPassFilterWithOpFail implements IPixelFilter {
		protected IPixelFilter opFail;

		public NeverPassFilterWithOpFail(IPixelFilter opFail) {
			this.opFail = opFail;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = false;
			pixel.filterOnFailed = opFail;
		}

		@Override
		public int getCompilationId() {
			return mixIds(991989036, opFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return opFail.getFlags();
		}
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

		@Override
		public int getCompilationId() {
			return 824428805;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class StencilTestPassIfMatchesWithOpFail implements IPixelFilter {
		protected int stencilRef;
		protected int stencilMask;
		protected IPixelFilter opFail;

		public StencilTestPassIfMatchesWithOpFail(int stencilRef, int stencilMask, IPixelFilter opFail) {
			this.stencilRef = stencilRef & stencilMask;
			this.stencilMask = stencilMask;
			this.opFail = opFail;
		}

		@Override
		public void filter(PixelState pixel) {
			int stencilValue = PixelColor.getAlpha(pixel.destination);
			pixel.filterPassed = (stencilValue & stencilMask) == stencilRef;
			if (!pixel.filterPassed) {
				pixel.filterOnFailed = opFail;
			}
		}

		@Override
		public int getCompilationId() {
			return mixIds(745113575, opFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return opFail.getFlags();
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

		@Override
		public int getCompilationId() {
			return 772236055;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class StencilTestPassIfDiffersWithOpFail implements IPixelFilter {
		protected int stencilRef;
		protected int stencilMask;
		protected IPixelFilter opFail;

		public StencilTestPassIfDiffersWithOpFail(int stencilRef, int stencilMask, IPixelFilter opFail) {
			this.stencilRef = stencilRef & stencilMask;
			this.stencilMask = stencilMask;
			this.opFail = opFail;
		}

		@Override
		public void filter(PixelState pixel) {
			int stencilValue = PixelColor.getAlpha(pixel.destination);
			pixel.filterPassed = (stencilValue & stencilMask) != stencilRef;
			if (!pixel.filterPassed) {
				pixel.filterOnFailed = opFail;
			}
		}

		@Override
		public int getCompilationId() {
			return mixIds(828935920, opFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return opFail.getFlags();
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

		@Override
		public int getCompilationId() {
			return 585428831;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class StencilTestPassIfLessWithOpFail implements IPixelFilter {
		protected int stencilRef;
		protected int stencilMask;
		protected IPixelFilter opFail;

		public StencilTestPassIfLessWithOpFail(int stencilRef, int stencilMask, IPixelFilter opFail) {
			this.stencilRef = stencilRef & stencilMask;
			this.stencilMask = stencilMask;
			this.opFail = opFail;
		}

		@Override
		public void filter(PixelState pixel) {
			int stencilValue = PixelColor.getAlpha(pixel.destination);
			pixel.filterPassed = (stencilValue & stencilMask) < stencilRef;
			if (!pixel.filterPassed) {
				pixel.filterOnFailed = opFail;
			}
		}

		@Override
		public int getCompilationId() {
			return mixIds(506586036, opFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return opFail.getFlags();
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

		@Override
		public int getCompilationId() {
			return 112785132;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class StencilTestPassIfLessOrEqualWithOpFail implements IPixelFilter {
		protected int stencilRef;
		protected int stencilMask;
		protected IPixelFilter opFail;

		public StencilTestPassIfLessOrEqualWithOpFail(int stencilRef, int stencilMask, IPixelFilter opFail) {
			this.stencilRef = stencilRef & stencilMask;
			this.stencilMask = stencilMask;
			this.opFail = opFail;
		}

		@Override
		public void filter(PixelState pixel) {
			int stencilValue = PixelColor.getAlpha(pixel.destination);
			pixel.filterPassed = (stencilValue & stencilMask) <= stencilRef;
			if (!pixel.filterPassed) {
				pixel.filterOnFailed = opFail;
			}
		}

		@Override
		public int getCompilationId() {
			return mixIds(809704087, opFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return opFail.getFlags();
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

		@Override
		public int getCompilationId() {
			return 785243230;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class StencilTestPassIfGreaterWithOpFail implements IPixelFilter {
		protected int stencilRef;
		protected int stencilMask;
		protected IPixelFilter opFail;

		public StencilTestPassIfGreaterWithOpFail(int stencilRef, int stencilMask, IPixelFilter opFail) {
			this.stencilRef = stencilRef & stencilMask;
			this.stencilMask = stencilMask;
			this.opFail = opFail;
		}

		@Override
		public void filter(PixelState pixel) {
			int stencilValue = PixelColor.getAlpha(pixel.destination);
			pixel.filterPassed = (stencilValue & stencilMask) > stencilRef;
			if (!pixel.filterPassed) {
				pixel.filterOnFailed = opFail;
			}
		}

		@Override
		public int getCompilationId() {
			return mixIds(867126294, opFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return opFail.getFlags();
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

		@Override
		public int getCompilationId() {
			return 578049064;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class StencilTestPassIfGreaterOrEqualWithOpFail implements IPixelFilter {
		protected int stencilRef;
		protected int stencilMask;
		protected IPixelFilter opFail;

		public StencilTestPassIfGreaterOrEqualWithOpFail(int stencilRef, int stencilMask, IPixelFilter opFail) {
			this.stencilRef = stencilRef & stencilMask;
			this.stencilMask = stencilMask;
			this.opFail = opFail;
		}

		@Override
		public void filter(PixelState pixel) {
			int stencilValue = PixelColor.getAlpha(pixel.destination);
			pixel.filterPassed = (stencilValue & stencilMask) >= stencilRef;
			if (!pixel.filterPassed) {
				pixel.filterOnFailed = opFail;
			}
		}

		@Override
		public int getCompilationId() {
			return mixIds(440201346, opFail.getCompilationId());
		}

		@Override
		public int getFlags() {
			return opFail.getFlags();
		}
	}
}
