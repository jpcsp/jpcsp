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

import static jpcsp.graphics.RE.software.PixelColor.ZERO;
import static jpcsp.graphics.RE.software.PixelColor.getColor;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class MaskFilter {
	public static IPixelFilter getMaskFilter(GeContext context, boolean clearMode, boolean clearModeColor, boolean clearModeStencil, boolean clearModeDepth) {
		IPixelFilter filter = null;
		boolean depthMask = clearMode ? clearModeDepth : context.depthMask;

		if (clearMode) {
			filter = getMaskFilter(depthMask, clearModeColor, clearModeStencil);
		} else {
			if (context.colorMask[0] == ZERO && context.colorMask[1] == ZERO && context.colorMask[2] == ZERO && context.colorMask[3] == ZERO) {
				if (depthMask) {
					filter = NopFilter.NOP;
				} else {
					// Depth writes disabled
					filter = new DepthMask();
				}
			} else {
				if (depthMask) {
					// Depth writes enabled
					filter = new ColorMask(context.colorMask);
				} else {
					// Depth writes disabled
					filter = new DepthColorMask(context.colorMask);
				}
			}
		}

		return filter;
	}

	private static IPixelFilter getMaskFilter(boolean depthMask, boolean colorMask, boolean stencilMask) {
		IPixelFilter filter = null;

		if (!colorMask && !stencilMask) {
			if (depthMask) {
				filter = new NoColorMask();
			} else {
				filter = new DepthNoColorMask();
			}
		} else if (depthMask && colorMask && stencilMask) {
			return NopFilter.NOP;
		} else {
			if (depthMask) {
				filter = new ColorMask(colorMask, stencilMask);
			} else {
				filter = new DepthColorMask(colorMask, stencilMask);
			}
		}

		return filter;
	}

	private static class ColorMask implements IPixelFilter {
		private int colorMask;
		private int notColorMask;

		public ColorMask(int[] colorMask) {
			// colorMask[i] == ZERO: no masking
			// colorMask[i] == ONE: complete masking
			this.colorMask = getColor(colorMask);
			notColorMask = ~this.colorMask;
		}

		public ColorMask(boolean colorMask, boolean stencilMask) {
			this.colorMask = (colorMask ? 0x000000 : 0xFFFFFF);
			this.colorMask |= (stencilMask ? 0x00000000 : 0xFF000000);
			notColorMask = ~this.colorMask;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.source = (pixel.source & notColorMask) | (pixel.destination & colorMask);
		}

		@Override
		public int getCompilationId() {
			return 512641045;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class DepthColorMask extends ColorMask {
		public DepthColorMask(int[] colorMask) {
			super(colorMask);
		}

		public DepthColorMask(boolean colorMask, boolean stencilMask) {
			super(colorMask, stencilMask);
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.sourceDepth = pixel.destinationDepth;
			super.filter(pixel);
		}

		@Override
		public int getCompilationId() {
			return 853655278;
		}

		@Override
		public int getFlags() {
			return DISCARDS_SOURCE_DEPTH;
		}
	}

	private static final class DepthMask implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.sourceDepth = pixel.destinationDepth;
		}

		@Override
		public int getCompilationId() {
			return 151783143;
		}

		@Override
		public int getFlags() {
			return DISCARDS_SOURCE_DEPTH;
		}
	}

	private static final class NoColorMask implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = pixel.destination;
		}

		@Override
		public int getCompilationId() {
			return 132565727;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class DepthNoColorMask implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = pixel.destination;
			pixel.sourceDepth = pixel.destinationDepth;
		}

		@Override
		public int getCompilationId() {
			return 473223297;
		}

		@Override
		public int getFlags() {
			return DISCARDS_SOURCE_DEPTH;
		}
	}
}
