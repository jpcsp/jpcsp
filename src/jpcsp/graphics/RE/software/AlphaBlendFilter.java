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

import static jpcsp.graphics.RE.software.PixelColor.ONE;
import static jpcsp.graphics.RE.software.PixelColor.abs;
import static jpcsp.graphics.RE.software.PixelColor.add;
import static jpcsp.graphics.RE.software.PixelColor.getAlpha;
import static jpcsp.graphics.RE.software.PixelColor.getColor;
import static jpcsp.graphics.RE.software.PixelColor.min;
import static jpcsp.graphics.RE.software.PixelColor.multiply;
import static jpcsp.graphics.RE.software.PixelColor.substract;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class AlphaBlendFilter {
	public static IPixelFilter getAlphaBlendFilter(GeContext context) {
		IPixelFilter sourceFactor = getFactorFilter(context, context.blend_src, true);
		IPixelFilter destinationFactor = getFactorFilter(context, context.blend_dst, false);
		IPixelFilter filter = null;

		switch (context.blendEquation) {
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_ADD:
				if (context.blend_src == GeCommands.ALPHA_FIX && context.sfix == 0xFFFFFF &&
				    context.blend_dst == GeCommands.ALPHA_FIX && context.dfix == 0x000000) {
					filter = NopFilter.NOP;
				} else {
					filter = new BlendOperationAdd(sourceFactor, destinationFactor);
				}
				break;
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_SUBTRACT:
				filter = new BlendOperationSubstract(sourceFactor, destinationFactor);
				break;
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_REVERSE_SUBTRACT:
				filter = new BlendOperationReverseSubstract(sourceFactor, destinationFactor);
				break;
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_MINIMUM_VALUE:
				filter = new BlendOperationMin(sourceFactor, destinationFactor);
				break;
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_MAXIMUM_VALUE:
				filter = new BlendOperationMax(sourceFactor, destinationFactor);
				break;
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_ABSOLUTE_VALUE:
				filter = new BlendOperationAbs(sourceFactor, destinationFactor);
				break;
		}

		return filter;
	}

	private static IPixelFilter getFactorFilter(GeContext context, int factor, boolean isSrc) {
		IPixelFilter filter = NopFilter.NOP;

		switch (factor) {
			case GeCommands.ALPHA_SOURCE_COLOR:
				filter = isSrc ? new BlendFactorSrcColor() : new BlendFactorDstColor();
				break;
			case GeCommands.ALPHA_ONE_MINUS_SOURCE_COLOR:
				filter = isSrc ? new BlendFactorOneMinusSrcColor() : new BlendFactorOneMinusDstColor();
				break;
			case GeCommands.ALPHA_SOURCE_ALPHA:
				filter = new BlendFactorSrcAlpha();
				break;
			case GeCommands.ALPHA_ONE_MINUS_SOURCE_ALPHA:
				filter = new BlendFactorOneMinusSrcAlpha();
				break;
			case GeCommands.ALPHA_DESTINATION_ALPHA:
				filter = new BlendFactorDstAlpha();
				break;
			case GeCommands.ALPHA_ONE_MINUS_DESTINATION_ALPHA:
				filter = new BlendFactorOneMinusDstAlpha();
				break;
			case GeCommands.ALPHA_DOUBLE_SOURCE_ALPHA:
				filter = new BlendFactorDoubleSrcAlpha();
				break;
			case GeCommands.ALPHA_ONE_MINUS_DOUBLE_SOURCE_ALPHA:
				filter = new BlendFactorOneMinusDoubleSrcAlpha();
				break;
			case GeCommands.ALPHA_DOUBLE_DESTINATION_ALPHA:
				filter = new BlendFactorDoubleDstAlpha();
				break;
			case GeCommands.ALPHA_ONE_MINUS_DOUBLE_DESTINATION_ALPHA:
				filter = new BlendFactorOneMinusDoubleDstAlpha();
				break;
			case GeCommands.ALPHA_FIX:
				filter = isSrc ? new BlendFactorFix(context.sfix_color) : new BlendFactorFix(context.dfix_color);
				break;
		}

		return filter;
	}

	private static class BlendFactorSrcColor implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			return pixel.source;
		}
	}

	private static class BlendFactorOneMinusSrcColor implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			return 0xFFFFFFFF - pixel.source;
		}
	}

	private static class BlendFactorDstColor implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			return pixel.destination;
		}
	}

	private static class BlendFactorOneMinusDstColor implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			return 0xFFFFFFFF - pixel.destination;
		}
	}

	private static class BlendFactorSrcAlpha implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			int alpha = getAlpha(pixel.source);
			return getColor(alpha, alpha, alpha, alpha);
		}
	}

	private static class BlendFactorOneMinusSrcAlpha implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			int alpha = ONE - getAlpha(pixel.source);
			return getColor(alpha, alpha, alpha, alpha);
		}
	}

	private static class BlendFactorDstAlpha implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			int alpha = getAlpha(pixel.destination);
			return getColor(alpha, alpha, alpha, alpha);
		}
	}

	private static class BlendFactorOneMinusDstAlpha implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			int alpha = ONE - getAlpha(pixel.destination);
			return getColor(alpha, alpha, alpha, alpha);
		}
	}

	private static class BlendFactorDoubleSrcAlpha implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			int alpha = getAlpha(pixel.source) << 1;
			return getColor(alpha, alpha, alpha, alpha);
		}
	}

	private static class BlendFactorOneMinusDoubleSrcAlpha implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			int alpha = ONE - (getAlpha(pixel.source) << 1);
			return getColor(alpha, alpha, alpha, alpha);
		}
	}

	private static class BlendFactorDoubleDstAlpha implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			int alpha = getAlpha(pixel.destination) << 1;
			return getColor(alpha, alpha, alpha, alpha);
		}
	}

	private static class BlendFactorOneMinusDoubleDstAlpha implements IPixelFilter {
		@Override
		public int filter(PixelState pixel) {
			int alpha = ONE - (getAlpha(pixel.destination) << 1);
			return getColor(alpha, alpha, alpha, alpha);
		}
	}

	private static class BlendFactorFix implements IPixelFilter {
		private int fixColor;

		public BlendFactorFix(float[] color) {
			fixColor = getColor(color);
		}

		@Override
		public int filter(PixelState pixel) {
			return fixColor;
		}
	}

	private static abstract class BlendOperation implements IPixelFilter {
		protected IPixelFilter sourceFactor;
		protected IPixelFilter destinationFactor;

		public BlendOperation(IPixelFilter sourceFactor, IPixelFilter destinationFactor) {
			this.sourceFactor = sourceFactor;
			this.destinationFactor = destinationFactor;
		}
	}

	private static final class BlendOperationAdd extends BlendOperation {
		public BlendOperationAdd(IPixelFilter sourceFactor, IPixelFilter destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public int filter(PixelState pixel) {
			int filteredSource = multiply(pixel.source, sourceFactor.filter(pixel));
			int filteredDestination = multiply(pixel.destination, destinationFactor.filter(pixel));
			return add(filteredSource, filteredDestination);
		}
	}

	private static final class BlendOperationSubstract extends BlendOperation {
		public BlendOperationSubstract(IPixelFilter sourceFactor, IPixelFilter destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public int filter(PixelState pixel) {
			int filteredSource = multiply(pixel.source, sourceFactor.filter(pixel));
			int filteredDestination = multiply(pixel.destination, destinationFactor.filter(pixel));
			return substract(filteredSource, filteredDestination);
		}
	}

	private static final class BlendOperationReverseSubstract extends BlendOperation {
		public BlendOperationReverseSubstract(IPixelFilter sourceFactor, IPixelFilter destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public int filter(PixelState pixel) {
			int filteredSource = multiply(pixel.source, sourceFactor.filter(pixel));
			int filteredDestination = multiply(pixel.destination, destinationFactor.filter(pixel));
			return substract(filteredDestination, filteredSource);
		}
	}

	private static final class BlendOperationMin extends BlendOperation {
		public BlendOperationMin(IPixelFilter sourceFactor, IPixelFilter destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public int filter(PixelState pixel) {
			// Source and destination factors are not applied
			return min(pixel.source, pixel.destination);
		}
	}

	private static final class BlendOperationMax extends BlendOperation {
		public BlendOperationMax(IPixelFilter sourceFactor, IPixelFilter destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public int filter(PixelState pixel) {
			// Source and destination factors are not applied
			return min(pixel.source, pixel.destination);
		}
	}

	private static final class BlendOperationAbs extends BlendOperation {
		public BlendOperationAbs(IPixelFilter sourceFactor, IPixelFilter destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public int filter(PixelState pixel) {
			// Source and destination factors are not applied
			return abs(pixel.source, pixel.destination);
		}
	}
}
