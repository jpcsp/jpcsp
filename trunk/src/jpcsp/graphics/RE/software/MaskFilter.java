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
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class MaskFilter {
	public static IPixelFilter getMaskFilter(GeContext context, boolean clearMode, boolean clearModeColor, boolean clearModeStencil, boolean clearModeDepth) {
		IPixelFilter filter = null;

		if (clearMode) {
			if (!clearModeDepth) {
				// Depth writes disabled
				filter = new DepthColorMask(clearModeColor, clearModeStencil);
			} else if (clearModeColor && clearModeStencil) {
				filter = NopFilter.NOP;
			} else {
				// Depth writes enabled
				filter = new ColorMask(clearModeColor, clearModeStencil);
			}
		} else {
			if (context.colorMask[0] == ZERO && context.colorMask[1] == ZERO && context.colorMask[2] == ZERO && context.colorMask[3] == ZERO) {
				if (context.depthMask) {
					filter = NopFilter.NOP;
				} else {
					// Depth writes disabled
					filter = new DepthMask();
				}
			} else {
				if (context.depthMask) {
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

	private static class ColorMask implements IPixelFilter {
		private int colorMask;
		private int notColorMask;

		public ColorMask(int[] colorMask) {
			// colorMask[i] == ZERO: no masking
			// colorMask[i] == ONE: complete masking
			this.colorMask = PixelColor.getColor(colorMask[3], colorMask[2], colorMask[1], colorMask[0]);
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
	}

	private static final class DepthMask implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.sourceDepth = pixel.destinationDepth;
		}
	}
}
