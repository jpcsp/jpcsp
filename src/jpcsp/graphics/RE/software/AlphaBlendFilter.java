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
import static jpcsp.graphics.RE.software.PixelColor.ONE;
import static jpcsp.graphics.RE.software.PixelColor.absBGR;
import static jpcsp.graphics.RE.software.PixelColor.add;
import static jpcsp.graphics.RE.software.PixelColor.addBGR;
import static jpcsp.graphics.RE.software.PixelColor.getAlpha;
import static jpcsp.graphics.RE.software.PixelColor.getColor;
import static jpcsp.graphics.RE.software.PixelColor.getColorBGR;
import static jpcsp.graphics.RE.software.PixelColor.maxBGR;
import static jpcsp.graphics.RE.software.PixelColor.minBGR;
import static jpcsp.graphics.RE.software.PixelColor.multiplyBGR;
import static jpcsp.graphics.RE.software.PixelColor.setBGR;
import static jpcsp.graphics.RE.software.PixelColor.substractBGR;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class AlphaBlendFilter {
	public static IPixelFilter getAlphaBlendFilter(GeContext context) {
		IBlendFactor sourceFactor = getFactorFilter(context, context.blend_src, true);
		IBlendFactor destinationFactor = getFactorFilter(context, context.blend_dst, false);
		IPixelFilter filter = null;

		switch (context.blendEquation) {
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_ADD:
				if (context.blend_src == GeCommands.ALPHA_FIX && context.sfix == 0xFFFFFF &&
				    context.blend_dst == GeCommands.ALPHA_FIX && context.dfix == 0x000000) {
					filter = NopFilter.NOP;
				} else if (context.blend_src == GeCommands.ALPHA_FIX && context.sfix == 0xFFFFFF &&
						   context.blend_dst == GeCommands.ALPHA_FIX && context.dfix == 0xFFFFFF) {
					filter = new BlendOperationAddDst();
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
				// Source and destination factors are not applied
				filter = new BlendOperationMin();
				break;
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_MAXIMUM_VALUE:
				// Source and destination factors are not applied
				filter = new BlendOperationMax();
				break;
			case GeCommands.ALPHA_SOURCE_BLEND_OPERATION_ABSOLUTE_VALUE:
				// Source and destination factors are not applied
				filter = new BlendOperationAbs();
				break;
		}

		return filter;
	}

	private static IBlendFactor getFactorFilter(GeContext context, int factor, boolean isSrc) {
		IBlendFactor filter = null;

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

	private interface IBlendFactor {
		public int getFactor(PixelState pixel);
		public int getCompilationId();
		public int getFlags();
	}

	private static class BlendFactorSrcColor implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			return pixel.source;
		}

		@Override
		public String toString() {
			return "BlendFactorSrcColor";
		}

		@Override
		public int getCompilationId() {
			return 751674897;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class BlendFactorOneMinusSrcColor implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			return 0xFFFFFFFF - pixel.source;
		}

		@Override
		public String toString() {
			return "BlendFactorOneMinusSrcColor";
		}

		@Override
		public int getCompilationId() {
			return 227371043;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class BlendFactorDstColor implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			return pixel.destination;
		}

		@Override
		public String toString() {
			return "BlendFactorDstColor";
		}

		@Override
		public int getCompilationId() {
			return 888983579;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class BlendFactorOneMinusDstColor implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			return 0xFFFFFFFF - pixel.destination;
		}

		@Override
		public String toString() {
			return "BlendFactorOneMinusDstColor";
		}

		@Override
		public int getCompilationId() {
			return 896119972;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class BlendFactorSrcAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = getAlpha(pixel.source);
			return getColorBGR(alpha, alpha, alpha);
		}

		@Override
		public String toString() {
			return "BlendFactorSrcAlpha";
		}

		@Override
		public int getCompilationId() {
			return 303370931;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class BlendFactorOneMinusSrcAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = ONE - getAlpha(pixel.source);
			return getColorBGR(alpha, alpha, alpha);
		}

		@Override
		public String toString() {
			return "BlendFactorOneMinusSrcAlpha";
		}

		@Override
		public int getCompilationId() {
			return 180178452;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class BlendFactorDstAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = getAlpha(pixel.destination);
			return getColorBGR(alpha, alpha, alpha);
		}

		@Override
		public String toString() {
			return "BlendFactorDstAlpha";
		}

		@Override
		public int getCompilationId() {
			return 358356099;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class BlendFactorOneMinusDstAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = ONE - getAlpha(pixel.destination);
			return getColorBGR(alpha, alpha, alpha);
		}

		@Override
		public String toString() {
			return "BlendFactorOneMinusDstAlpha";
		}

		@Override
		public int getCompilationId() {
			return 698613375;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class BlendFactorDoubleSrcAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = getAlpha(pixel.source) << 1;
			return getColorBGR(alpha, alpha, alpha);
		}

		@Override
		public String toString() {
			return "BlendFactorDoubleSrcAlpha";
		}

		@Override
		public int getCompilationId() {
			return 609334188;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class BlendFactorOneMinusDoubleSrcAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = ONE - (getAlpha(pixel.source) << 1);
			return getColorBGR(alpha, alpha, alpha);
		}

		@Override
		public String toString() {
			return "BlendFactorOneMinusDoubleSrcAlpha";
		}

		@Override
		public int getCompilationId() {
			return 208600931;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class BlendFactorDoubleDstAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = getAlpha(pixel.destination) << 1;
			return getColorBGR(alpha, alpha, alpha);
		}

		@Override
		public String toString() {
			return "BlendFactorDoubleDstAlpha";
		}

		@Override
		public int getCompilationId() {
			return 901170246;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class BlendFactorOneMinusDoubleDstAlpha implements IBlendFactor {
		@Override
		public int getFactor(PixelState pixel) {
			int alpha = ONE - (getAlpha(pixel.destination) << 1);
			return getColorBGR(alpha, alpha, alpha);
		}

		@Override
		public String toString() {
			return "BlendFactorOneMinusDoubleDstAlpha";
		}

		@Override
		public int getCompilationId() {
			return 80451709;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class BlendFactorFix implements IBlendFactor {
		private int fixColor;

		public BlendFactorFix(float[] color) {
			fixColor = getColor(color) & 0x00FFFFFF;
		}

		@Override
		public int getFactor(PixelState pixel) {
			return fixColor;
		}

		@Override
		public String toString() {
			return "BlendFactorFix";
		}

		@Override
		public int getCompilationId() {
			return 769918525;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static abstract class BlendOperation implements IPixelFilter {
		protected IBlendFactor sourceFactor;
		protected IBlendFactor destinationFactor;

		public BlendOperation(IBlendFactor sourceFactor, IBlendFactor destinationFactor) {
			this.sourceFactor = sourceFactor;
			this.destinationFactor = destinationFactor;
		}
	}

	private static final class BlendOperationAdd extends BlendOperation {
		public BlendOperationAdd(IBlendFactor sourceFactor, IBlendFactor destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public void filter(PixelState pixel) {
			int filteredSource = multiplyBGR(pixel.source, sourceFactor.getFactor(pixel));
			int filteredDestination = multiplyBGR(pixel.destination, destinationFactor.getFactor(pixel));
			pixel.source = setBGR(pixel.source, addBGR(filteredSource, filteredDestination));
		}

		@Override
		public String toString() {
			return String.format("BlendOperationAdd(src=%s, dst=%s)", sourceFactor.toString(), destinationFactor.toString());
		}

		@Override
		public int getCompilationId() {
			return mixIds(312712738, sourceFactor.getCompilationId(), destinationFactor.getCompilationId());
		}

		@Override
		public int getFlags() {
			return sourceFactor.getFlags() | destinationFactor.getFlags();
		}
	}

	private static final class BlendOperationAddDst implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = add(pixel.source, pixel.destination & 0x00FFFFFF);
		}

		@Override
		public String toString() {
			return String.format("BlendOperationAddDst");
		}

		@Override
		public int getCompilationId() {
			return 546161068;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class BlendOperationSubstract extends BlendOperation {
		public BlendOperationSubstract(IBlendFactor sourceFactor, IBlendFactor destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public void filter(PixelState pixel) {
			int filteredSource = multiplyBGR(pixel.source, sourceFactor.getFactor(pixel));
			int filteredDestination = multiplyBGR(pixel.destination, destinationFactor.getFactor(pixel));
			pixel.source = setBGR(pixel.source, substractBGR(filteredSource, filteredDestination));
		}

		@Override
		public String toString() {
			return String.format("BlendOperationSubstract(src=%s, dst=%s)", sourceFactor.toString(), destinationFactor.toString());
		}

		@Override
		public int getCompilationId() {
			return mixIds(746719570, sourceFactor.getCompilationId(), destinationFactor.getCompilationId());
		}

		@Override
		public int getFlags() {
			return sourceFactor.getFlags() | destinationFactor.getFlags();
		}
	}

	private static final class BlendOperationReverseSubstract extends BlendOperation {
		public BlendOperationReverseSubstract(IBlendFactor sourceFactor, IBlendFactor destinationFactor) {
			super(sourceFactor, destinationFactor);
		}

		@Override
		public void filter(PixelState pixel) {
			int filteredSource = multiplyBGR(pixel.source, sourceFactor.getFactor(pixel));
			int filteredDestination = multiplyBGR(pixel.destination, destinationFactor.getFactor(pixel));
			pixel.source = setBGR(pixel.source, substractBGR(filteredDestination, filteredSource));
		}

		@Override
		public String toString() {
			return String.format("BlendOperationReverseSubstract(src=%s, dst=%s)", sourceFactor.toString(), destinationFactor.toString());
		}

		@Override
		public int getCompilationId() {
			return mixIds(573703791, sourceFactor.getCompilationId(), destinationFactor.getCompilationId());
		}

		@Override
		public int getFlags() {
			return sourceFactor.getFlags() | destinationFactor.getFlags();
		}
	}

	private static final class BlendOperationMin implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = setBGR(pixel.source, minBGR(pixel.source, pixel.destination));
		}

		@Override
		public String toString() {
			return String.format("BlendOperationMin");
		}

		@Override
		public int getCompilationId() {
			return 908977775;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class BlendOperationMax implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = setBGR(pixel.source, maxBGR(pixel.source, pixel.destination));
		}

		@Override
		public String toString() {
			return String.format("BlendOperationMax");
		}

		@Override
		public int getCompilationId() {
			return 173691952;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class BlendOperationAbs implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = setBGR(pixel.source, absBGR(pixel.source, pixel.destination));
		}

		@Override
		public String toString() {
			return String.format("BlendOperationAbs");
		}

		@Override
		public int getCompilationId() {
			return 968835587;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}
}
