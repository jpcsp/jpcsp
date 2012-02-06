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

import static jpcsp.graphics.RE.software.ColorDoubling.doubleColor;
import static jpcsp.graphics.RE.software.PixelColor.add;
import static jpcsp.graphics.RE.software.PixelColor.getAlpha;
import static jpcsp.graphics.RE.software.PixelColor.getColor;
import static jpcsp.graphics.RE.software.PixelColor.multiply;
import static jpcsp.graphics.RE.software.TextureReader.pixelToTexel;
import static jpcsp.graphics.RE.software.TextureWrap.wrap;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 * This implementation is not (yet) a proper filter compilation.
 * This is more a hack providing a Java implementation for some filter combinations
 * in order to check if a proper filter compiler would bring a big win.
 */
public class FilterCompiler {
	public static IPixelFilter getCompiledFilter(BaseRenderer baseRenderer, int id, GeContext context) {
		IPixelFilter compiledFilter = null;

		switch (id) {
			case -1441816927:
				compiledFilter = new CompiledFilter1(context);
				break;
			case 1369565309:
				compiledFilter = new CompiledFilter2(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel, baseRenderer.lightingFilter);
				break;
			case -1493749788:
				compiledFilter = new CompiledFilter3(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 385143114:
				compiledFilter = new CompiledFilter4(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 1083476917:
				compiledFilter = new CompiledFilter5(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 536926257:
				compiledFilter = new CompiledFilter6(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 1844988032:
				compiledFilter = new CompiledFilter7(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 1925996299:
				compiledFilter = new CompiledFilter8(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel, baseRenderer.lightingFilter);
				break;
			case 354457015:
			case -53617429:
				compiledFilter = new CompiledFilter9(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 1273726167:
			case 865651723:
				compiledFilter = new CompiledFilter10(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 1653801485: // Scissor XY
			case 1245727041: // Scissor NOP
				compiledFilter = new CompiledFilter11(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 1562615929:
				compiledFilter = new CompiledFilter12(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 1173402078: // ScissorXY
			case 652751344: // ScissorY
				compiledFilter = new CompiledFilter13(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 576423576: // Scissor NOP
				compiledFilter = new CompiledFilter14(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 1322816484:
				compiledFilter = new CompiledFilter15(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel, baseRenderer.lightingFilter);
				break;
			case -1342022891: // Scissor Y
			case -1261688239: // Scissor NOP
			case -821372157: // Scissor XY
			case -878684883: // Scissor X
				compiledFilter = new CompiledFilter16(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 1179545800: // Scissor Y
				compiledFilter = new CompiledFilter17(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 785317071: // Scissor Y
				compiledFilter = new CompiledFilter18(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 1184084933:
				compiledFilter = new CompiledFilter19(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
			case 1335868076:
				compiledFilter = new CompiledFilter20(context, baseRenderer.textureAccess, baseRenderer.mipmapLevel);
				break;
		}

		return compiledFilter;
	}

	private static class CompiledFilter1 implements IPixelFilter {
		private int alphaReferenceValue;

		public CompiledFilter1(GeContext context) {
			alphaReferenceValue = context.alphaRef;
		}

		@Override
		public void filter(PixelState pixel) {
			// SourceColorFilter$SourcePrimary,
			// AlphaTestFilter$AlphaFunctionPassIfGreater,
			// DepthTestFilter$DepthTestPassWhenDepthIsGreaterOrEqual,
			// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
			pixel.filterPassed = pixel.sourceDepth >= pixel.destinationDepth;
			if (pixel.filterPassed) {
				pixel.source = pixel.primaryColor;
				pixel.filterPassed = getAlpha(pixel.source) > alphaReferenceValue;
				if (pixel.filterPassed) {
					// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
					int srcAlpha = PixelColor.getAlpha(pixel.source);
					if (srcAlpha == PixelColor.ZERO) {
						pixel.source = PixelColor.setBGR(pixel.source, pixel.destination);
					} else if (srcAlpha == PixelColor.ONE) {
						// Nothing to change
					} else {
						int oneMinusSrcAlpha = PixelColor.ONE - srcAlpha;
						int filteredSrc = PixelColor.multiplyBGR(pixel.source, srcAlpha, srcAlpha, srcAlpha);
						int filteredDst = PixelColor.multiplyBGR(pixel.destination, oneMinusSrcAlpha, oneMinusSrcAlpha, oneMinusSrcAlpha);
						int source = PixelColor.addBGR(filteredSrc, filteredDst);
						pixel.source = PixelColor.setBGR(pixel.source, source);
					}
				}
			}
		}

		@Override
		public int getCompilationId() {
			return 925556957;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH;
		}
	}

	private static class CompiledFilter2 implements IPixelFilter {
		private int alphaReferenceValue;
		private IPixelFilter lightingFilter;
		private IRandomTextureAccess textureAccess;
		private int width;
		private int height;

		public CompiledFilter2(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel, IPixelFilter lightingFilter) {
			this.lightingFilter = lightingFilter;
			alphaReferenceValue = context.alphaRef;
			this.textureAccess = textureAccess;
			width = context.texture_width[mipmapLevel];
			height = context.texture_height[mipmapLevel];
		}

		@Override
		public void filter(PixelState pixel) {
			// Lighting$LightingFilter,
			// TextureWrap$TextureWrapRepeatST,
			// TextureReader$TextureReader3D,
			// TextureFunction$TextureEffectModulateRGBA,
			// AlphaTestFilter$AlphaFunctionPassIfGreater,
			// DepthTestFilter$DepthTestPassWhenDepthIsGreaterOrEqual,
			// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
			pixel.filterPassed = pixel.sourceDepth >= pixel.destinationDepth;
			if (pixel.filterPassed) {
				lightingFilter.filter(pixel);
				pixel.u = wrap(pixel.u);
				pixel.v = wrap(pixel.v);
				pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u * width), pixelToTexel(pixel.v * height));
				pixel.source = multiply(pixel.source, pixel.primaryColor);
				pixel.filterPassed = getAlpha(pixel.source) > alphaReferenceValue;
				if (pixel.filterPassed) {
					// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
					int srcAlpha = PixelColor.getAlpha(pixel.source);
					if (srcAlpha == PixelColor.ZERO) {
						pixel.source = PixelColor.setBGR(pixel.source, pixel.destination);
					} else if (srcAlpha == PixelColor.ONE) {
						// Nothing to change
					} else {
						int oneMinusSrcAlpha = PixelColor.ONE - srcAlpha;
						int filteredSrc = PixelColor.multiplyBGR(pixel.source, srcAlpha, srcAlpha, srcAlpha);
						int filteredDst = PixelColor.multiplyBGR(pixel.destination, oneMinusSrcAlpha, oneMinusSrcAlpha, oneMinusSrcAlpha);
						int source = PixelColor.addBGR(filteredSrc, filteredDst);
						pixel.source = PixelColor.setBGR(pixel.source, source);
					}
				}
			}
		}

		@Override
		public int getCompilationId() {
			return 665738812;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | REQUIRES_TEXTURE_U_V;
		}
	}

	private static class CompiledFilter3 implements IPixelFilter {
		private int alphaReferenceValue;
		private IRandomTextureAccess textureAccess;
		private int width;
		private int height;
		private float scaleU;
		private float scaleV;
		private float translateU;
		private float translateV;
		private int dstFix;

		public CompiledFilter3(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			alphaReferenceValue = context.alphaRef;
			this.textureAccess = textureAccess;
			width = context.texture_width[mipmapLevel];
			height = context.texture_height[mipmapLevel];
			scaleU = context.tex_scale_x;
			scaleV = context.tex_scale_y;
			translateU = context.tex_translate_x;
			translateV = context.tex_translate_y;
			dstFix = context.dfix;
		}

		@Override
		public void filter(PixelState pixel) {
			// VertexColorFilter$VertexTriangleTextureFilter,
			// TextureMapping$TextureMapUVScaleTranslate,
			// TextureWrap$TextureWrapRepeatST,
			// TextureReader$TextureReader3D,
			// TextureFunction$TextureEffectModulateRGBA,
			// AlphaTestFilter$AlphaFunctionPassIfGreater,
			// DepthTestFilter$DepthTestPassWhenDepthIsGreaterOrEqual,
			// BlendOperationAdd(src=BlendFactorDoubleSrcAlpha, dst=BlendFactorFix),
			// MaskFilter$DepthMask
			pixel.filterPassed = pixel.sourceDepth >= pixel.destinationDepth;
			if (pixel.filterPassed) {
				int a = pixel.getTriangleWeightedValue(pixel.c1a, pixel.c2a, pixel.c3a);
				int b = pixel.getTriangleWeightedValue(pixel.c1b, pixel.c2b, pixel.c3b);
				int g = pixel.getTriangleWeightedValue(pixel.c1g, pixel.c2g, pixel.c3g);
				int r = pixel.getTriangleWeightedValue(pixel.c1r, pixel.c2r, pixel.c3r);
				pixel.primaryColor = getColor(a, b, g, r);
				pixel.u = pixel.u * scaleU + translateU;
				pixel.v = pixel.v * scaleV + translateV;
				pixel.u = wrap(pixel.u);
				pixel.v = wrap(pixel.v);
				pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u * width), pixelToTexel(pixel.v * height));
				pixel.source = multiply(pixel.source, pixel.primaryColor);
				pixel.filterPassed = getAlpha(pixel.source) > alphaReferenceValue;
				if (pixel.filterPassed) {
					// BlendOperationAdd(src=BlendFactorDoubleSrcAlpha, dst=BlendFactorFix)
					int srcAlpha = PixelColor.getAlpha(pixel.source) << 1;
					int filteredSrc = PixelColor.multiplyBGR(pixel.source, srcAlpha, srcAlpha, srcAlpha);
					int filteredDst = PixelColor.multiplyBGR(pixel.destination, dstFix);
					int source = PixelColor.addBGR(filteredSrc, filteredDst);
					pixel.source = PixelColor.setBGR(pixel.source, source);
					pixel.sourceDepth = pixel.destinationDepth;
				}
			}
		}

		@Override
		public int getCompilationId() {
			return 665738812;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | DISCARDS_SOURCE_DEPTH | REQUIRES_TEXTURE_U_V;
		}
	}

	private static class CompiledFilter4 implements IPixelFilter {
		private int alphaReferenceValue;
		private IRandomTextureAccess textureAccess;
		private int width;
		private int height;

		public CompiledFilter4(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			alphaReferenceValue = context.alphaRef;
			this.textureAccess = textureAccess;
			width = context.texture_width[mipmapLevel];
			height = context.texture_height[mipmapLevel];
		}

		@Override
		public void filter(PixelState pixel) {
			// VertexColorFilter$VertexTriangleTextureFilter,
			// TextureWrap$TextureWrapRepeatST,
			// TextureReader$TextureReader3D,
			// TextureFunction$TextureEffectModulateRGBA,
			// AlphaTestFilter$AlphaFunctionPassIfGreater,
			// DepthTestFilter$DepthTestPassWhenDepthIsGreaterOrEqual,
			pixel.filterPassed = pixel.sourceDepth >= pixel.destinationDepth;
			if (pixel.filterPassed) {
				int a = pixel.getTriangleWeightedValue(pixel.c1a, pixel.c2a, pixel.c3a);
				int b = pixel.getTriangleWeightedValue(pixel.c1b, pixel.c2b, pixel.c3b);
				int g = pixel.getTriangleWeightedValue(pixel.c1g, pixel.c2g, pixel.c3g);
				int r = pixel.getTriangleWeightedValue(pixel.c1r, pixel.c2r, pixel.c3r);
				pixel.primaryColor = getColor(a, b, g, r);
				pixel.u = wrap(pixel.u);
				pixel.v = wrap(pixel.v);
				pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u * width), pixelToTexel(pixel.v * height));
				pixel.source = multiply(pixel.source, pixel.primaryColor);
				pixel.filterPassed = getAlpha(pixel.source) > alphaReferenceValue;
			}
		}

		@Override
		public int getCompilationId() {
			return 921415125;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | REQUIRES_TEXTURE_U_V;
		}
	}

	private static class CompiledFilter5 implements IPixelFilter {
		private int alphaReferenceValue;
		private IRandomTextureAccess textureAccess;
		private int width;
		private int height;

		public CompiledFilter5(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			alphaReferenceValue = context.alphaRef;
			this.textureAccess = textureAccess;
			width = context.texture_width[mipmapLevel];
			height = context.texture_height[mipmapLevel];
		}

		@Override
		public void filter(PixelState pixel) {
			// VertexColorFilter$VertexTriangleTextureFilter,
			// TextureWrap$TextureWrapRepeatST,
			// TextureReader$TextureReader3D,
			// TextureFunction$TextureEffectModulateRGBA,
			// AlphaTestFilter$AlphaFunctionPassIfGreater,
			// DepthTestFilter$DepthTestPassWhenDepthIsGreaterOrEqual,
			// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
			pixel.filterPassed = pixel.sourceDepth >= pixel.destinationDepth;
			if (pixel.filterPassed) {
				int a = pixel.getTriangleWeightedValue(pixel.c1a, pixel.c2a, pixel.c3a);
				int b = pixel.getTriangleWeightedValue(pixel.c1b, pixel.c2b, pixel.c3b);
				int g = pixel.getTriangleWeightedValue(pixel.c1g, pixel.c2g, pixel.c3g);
				int r = pixel.getTriangleWeightedValue(pixel.c1r, pixel.c2r, pixel.c3r);
				pixel.primaryColor = getColor(a, b, g, r);
				pixel.u = wrap(pixel.u);
				pixel.v = wrap(pixel.v);
				pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u * width), pixelToTexel(pixel.v * height));
				pixel.source = multiply(pixel.source, pixel.primaryColor);
				pixel.filterPassed = getAlpha(pixel.source) > alphaReferenceValue;
				if (pixel.filterPassed) {
					// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
					int srcAlpha = PixelColor.getAlpha(pixel.source);
					if (srcAlpha == PixelColor.ZERO) {
						pixel.source = PixelColor.setBGR(pixel.source, pixel.destination);
					} else if (srcAlpha == PixelColor.ONE) {
						// Nothing to change
					} else {
						int oneMinusSrcAlpha = PixelColor.ONE - srcAlpha;
						int filteredSrc = PixelColor.multiplyBGR(pixel.source, srcAlpha, srcAlpha, srcAlpha);
						int filteredDst = PixelColor.multiplyBGR(pixel.destination, oneMinusSrcAlpha, oneMinusSrcAlpha, oneMinusSrcAlpha);
						int source = PixelColor.addBGR(filteredSrc, filteredDst);
						pixel.source = PixelColor.setBGR(pixel.source, source);
					}
				}
			}
		}

		@Override
		public int getCompilationId() {
			return 689290254;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | REQUIRES_TEXTURE_U_V;
		}
	}

	private static class CompiledFilter6 implements IPixelFilter {
		private int alphaReferenceValue;
		private IRandomTextureAccess textureAccess;
		private int width;
		private int height;

		public CompiledFilter6(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			alphaReferenceValue = context.alphaRef;
			this.textureAccess = textureAccess;
			width = context.texture_width[mipmapLevel];
			height = context.texture_height[mipmapLevel];
		}

		@Override
		public void filter(PixelState pixel) {
			// VertexColorFilter$VertexTriangleTextureFilter,
			// TextureWrap$TextureWrapRepeatST,
			// TextureReader$TextureReader3D,
			// TextureFunction$TextureEffectModulateRGBA,
			// AlphaTestFilter$AlphaFunctionPassIfGreater,
			// DepthTestFilter$DepthTestPassWhenDepthIsGreaterOrEqual,
			// MaskFilter$DepthMask
			int a = pixel.getTriangleWeightedValue(pixel.c1a, pixel.c2a, pixel.c3a);
			int b = pixel.getTriangleWeightedValue(pixel.c1b, pixel.c2b, pixel.c3b);
			int g = pixel.getTriangleWeightedValue(pixel.c1g, pixel.c2g, pixel.c3g);
			int r = pixel.getTriangleWeightedValue(pixel.c1r, pixel.c2r, pixel.c3r);
			pixel.primaryColor = getColor(a, b, g, r);
			pixel.u = wrap(pixel.u);
			pixel.v = wrap(pixel.v);
			pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u * width), pixelToTexel(pixel.v * height));
			pixel.source = multiply(pixel.source, pixel.primaryColor);
			pixel.filterPassed = getAlpha(pixel.source) > alphaReferenceValue;
			if (pixel.filterPassed) {
				pixel.filterPassed = pixel.sourceDepth >= pixel.destinationDepth;
				if (pixel.filterPassed) {
					pixel.sourceDepth = pixel.destinationDepth;
				}
			}
		}

		@Override
		public int getCompilationId() {
			return 713430044;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | DISCARDS_SOURCE_DEPTH | REQUIRES_TEXTURE_U_V;
		}
	}

	private static class CompiledFilter7 implements IPixelFilter {
		private int alphaReferenceValue;
		private IRandomTextureAccess textureAccess;
		private int width;
		private int height;
		private float translateU;
		private float translateV;

		public CompiledFilter7(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			alphaReferenceValue = context.alphaRef;
			this.textureAccess = textureAccess;
			width = context.texture_width[mipmapLevel];
			height = context.texture_height[mipmapLevel];
			translateU = context.tex_translate_x;
			translateV = context.tex_translate_y;
		}

		@Override
		public void filter(PixelState pixel) {
			// VertexColorFilter$VertexTriangleTextureFilter,
			// TextureMapping$TextureMapUVTranslate,
			// TextureWrap$TextureWrapRepeatST,
			// TextureReader$TextureReader3D,
			// TextureFunction$TextureEffectModulateRGBA,
			// AlphaTestFilter$AlphaFunctionPassIfGreater,
			// DepthTestFilter$DepthTestPassWhenDepthIsGreaterOrEqual,
			// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha),
			// MaskFilter$DepthMask
			pixel.filterPassed = pixel.sourceDepth >= pixel.destinationDepth;
			if (pixel.filterPassed) {
				int a = pixel.getTriangleWeightedValue(pixel.c1a, pixel.c2a, pixel.c3a);
				int b = pixel.getTriangleWeightedValue(pixel.c1b, pixel.c2b, pixel.c3b);
				int g = pixel.getTriangleWeightedValue(pixel.c1g, pixel.c2g, pixel.c3g);
				int r = pixel.getTriangleWeightedValue(pixel.c1r, pixel.c2r, pixel.c3r);
				pixel.primaryColor = getColor(a, b, g, r);
				pixel.u += translateU;
				pixel.v += translateV;
				pixel.u = wrap(pixel.u);
				pixel.v = wrap(pixel.v);
				pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u * width), pixelToTexel(pixel.v * height));
				pixel.source = multiply(pixel.source, pixel.primaryColor);
				pixel.filterPassed = getAlpha(pixel.source) > alphaReferenceValue;
				if (pixel.filterPassed) {
					// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
					int srcAlpha = PixelColor.getAlpha(pixel.source);
					if (srcAlpha == PixelColor.ZERO) {
						pixel.source = PixelColor.setBGR(pixel.source, pixel.destination);
					} else if (srcAlpha == PixelColor.ONE) {
						// Nothing to change
					} else {
						int oneMinusSrcAlpha = PixelColor.ONE - srcAlpha;
						int filteredSrc = PixelColor.multiplyBGR(pixel.source, srcAlpha, srcAlpha, srcAlpha);
						int filteredDst = PixelColor.multiplyBGR(pixel.destination, oneMinusSrcAlpha, oneMinusSrcAlpha, oneMinusSrcAlpha);
						int source = PixelColor.addBGR(filteredSrc, filteredDst);
						pixel.source = PixelColor.setBGR(pixel.source, source);
					}
					pixel.sourceDepth = pixel.destinationDepth;
				}
			}
		}

		@Override
		public int getCompilationId() {
			return 340352085;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | DISCARDS_SOURCE_DEPTH | REQUIRES_TEXTURE_U_V;
		}
	}

	private static class CompiledFilter8 implements IPixelFilter {
		private IPixelFilter lightingFilter;
		private IRandomTextureAccess textureAccess;
		private int width;
		private int height;
		private int dstFix;
		private boolean materialAmbient;
		private boolean materialDiffuse;
		private boolean materialSpecular;

		public CompiledFilter8(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel, IPixelFilter lightingFilter) {
			this.lightingFilter = lightingFilter;
			this.textureAccess = textureAccess;
			width = context.texture_width[mipmapLevel];
			height = context.texture_height[mipmapLevel];
			dstFix = context.dfix;
			materialAmbient = (context.mat_flags & GeCommands.CMAT_FLAG_AMBIENT) != 0;
			materialDiffuse = (context.mat_flags & GeCommands.CMAT_FLAG_DIFFUSE) != 0;
			materialSpecular = (context.mat_flags & GeCommands.CMAT_FLAG_SPECULAR) != 0;
		}

		@Override
		public void filter(PixelState pixel) {
			// VertexColorFilter$ColorTextureFilter,
			// MaterialColorFilter$MaterialColor,
			// Lighting$LightingFilter,
			// TextureReader$TextureReader3D,
			// TextureFunction$TextureEffectModulateRGBA,
			// SourceColorFilter$SourceSecondary,
			// DepthTestFilter$DepthTestPassWhenDepthIsLessOrEqual,
			// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorFix),
			// MaskFilter$DepthMask
			pixel.filterPassed = pixel.sourceDepth <= pixel.destinationDepth;
			if (pixel.filterPassed) {
				pixel.primaryColor = getColor(pixel.c1a, pixel.c1b, pixel.c1g, pixel.c1r);
				if (materialAmbient) {
					pixel.materialAmbient = pixel.primaryColor;
				}
				if (materialDiffuse) {
					pixel.materialDiffuse = pixel.primaryColor;
				}
				if (materialSpecular) {
					pixel.materialSpecular = pixel.primaryColor;
				}
				lightingFilter.filter(pixel);
				pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u * width), pixelToTexel(pixel.v * height));
				pixel.source = multiply(pixel.source, pixel.primaryColor);
				pixel.source = add(pixel.source, pixel.secondaryColor);
				// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorFix)
				int srcAlpha = PixelColor.getAlpha(pixel.source);
				int filteredSrc = PixelColor.multiplyBGR(pixel.source, srcAlpha, srcAlpha, srcAlpha);
				int filteredDst = PixelColor.multiplyBGR(pixel.destination, dstFix);
				int source = PixelColor.addBGR(filteredSrc, filteredDst);
				pixel.source = PixelColor.setBGR(pixel.source, source);
				pixel.sourceDepth = pixel.destinationDepth;
			}
		}

		@Override
		public int getCompilationId() {
			return 228684383;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | DISCARDS_SOURCE_DEPTH | REQUIRES_TEXTURE_U_V;
		}
	}

	private static class CompiledFilter9 implements IPixelFilter {
		private IRandomTextureAccess textureAccess;
		private int dstFix;
		private int srcFix;

		public CompiledFilter9(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			this.textureAccess = textureAccess;
			dstFix = context.dfix;
			srcFix = context.sfix;
		}

		@Override
		public void filter(PixelState pixel) {
			// TextureReader$TextureReader2D,
			// TextureFunction$TextureEffectModulateRGBA,
			// NopFilter,
			// BlendOperationAdd(src=BlendFactorFix, dst=BlendFactorFix),
			// MaskFilter$DepthMask
			pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u), pixelToTexel(pixel.v));
			pixel.source = multiply(pixel.source, pixel.primaryColor);
			// BlendOperationAdd(src=BlendFactorFix, dst=BlendFactorFix)
			int filteredSrc = PixelColor.multiplyBGR(pixel.source, srcFix);
			int filteredDst = PixelColor.multiplyBGR(pixel.destination, dstFix);
			int source = PixelColor.addBGR(filteredSrc, filteredDst);
			pixel.source = PixelColor.setBGR(pixel.source, source);
			pixel.sourceDepth = pixel.destinationDepth;
		}

		@Override
		public int getCompilationId() {
			return 589920535;
		}

		@Override
		public int getFlags() {
			return DISCARDS_SOURCE_DEPTH | REQUIRES_TEXTURE_U_V;
		}
	}

	private static class CompiledFilter10 implements IPixelFilter {
		public CompiledFilter10(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
		}

		@Override
		public void filter(PixelState pixel) {
			// SourceColorFilter$SourcePrimary,
			// NopFilter,
			// MaskFilter$NoColorMask
			pixel.source = pixel.primaryColor;
			pixel.source = pixel.destination;
		}

		@Override
		public int getCompilationId() {
			return 810484951;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class CompiledFilter11 implements IPixelFilter {
		private int scissorX1;
		private int scissorY1;
		private int scissorX2;
		private int scissorY2;
		private int colorMask;
		private int notColorMask;

		public CompiledFilter11(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			if (context.clearMode) {
				colorMask = context.clearModeColor ? 0x000000 : 0xFFFFFF;
				colorMask |= context.clearModeStencil ? 0x00000000 : 0xFF000000;
			} else {
				colorMask = getColor(context.colorMask);
			}
			notColorMask = ~colorMask;
			scissorX1 = context.scissor_x1;
			scissorY1 = context.scissor_y1;
			scissorX2 = context.scissor_x2;
			scissorY2 = context.scissor_y2;
		}

		@Override
		public void filter(PixelState pixel) {
			// SourceColorFilter$SourcePrimary,
			// ScissorFilter$ScissorXY
			// MaskFilter$ColorMask
			pixel.source = pixel.primaryColor;
			pixel.filterPassed = (pixel.x >= scissorX1 && pixel.x <= scissorX2 && pixel.y >= scissorY1 && pixel.y <= scissorY2);
			if (pixel.filterPassed) {
				pixel.source = (pixel.source & notColorMask) | (pixel.destination & colorMask);
			}
		}

		@Override
		public int getCompilationId() {
			return 934128801;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class CompiledFilter12 implements IPixelFilter {
		public CompiledFilter12(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
		}

		@Override
		public void filter(PixelState pixel) {
			// VertexColorFilter$ColorTextureFilter,
			// SourceColorFilter$SourcePrimary,
			// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha),
			// MaskFilter$DepthMask
			pixel.primaryColor = getColor(pixel.c1a, pixel.c1b, pixel.c1g, pixel.c1r);
			pixel.source = pixel.primaryColor;
			// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
			int srcAlpha = PixelColor.getAlpha(pixel.source);
			if (srcAlpha == PixelColor.ZERO) {
				pixel.source = PixelColor.setBGR(pixel.source, pixel.destination);
			} else if (srcAlpha == PixelColor.ONE) {
				// Nothing to change
			} else {
				int oneMinusSrcAlpha = PixelColor.ONE - srcAlpha;
				int filteredSrc = PixelColor.multiplyBGR(pixel.source, srcAlpha, srcAlpha, srcAlpha);
				int filteredDst = PixelColor.multiplyBGR(pixel.destination, oneMinusSrcAlpha, oneMinusSrcAlpha, oneMinusSrcAlpha);
				int source = PixelColor.addBGR(filteredSrc, filteredDst);
				pixel.source = PixelColor.setBGR(pixel.source, source);
			}
			pixel.sourceDepth = pixel.destinationDepth;
		}

		@Override
		public int getCompilationId() {
			return 283832457;
		}

		@Override
		public int getFlags() {
			return DISCARDS_SOURCE_DEPTH;
		}
	}

	private static class CompiledFilter13 implements IPixelFilter {
		private int scissorX1;
		private int scissorY1;
		private int scissorX2;
		private int scissorY2;

		public CompiledFilter13(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			scissorX1 = context.scissor_x1;
			scissorY1 = context.scissor_y1;
			scissorX2 = context.scissor_x2;
			scissorY2 = context.scissor_y2;
		}

		@Override
		public void filter(PixelState pixel) {
			// SourceColorFilter$SourcePrimary,
			// ScissorFilter$ScissorXY
			pixel.source = pixel.primaryColor;
			pixel.filterPassed = (pixel.x >= scissorX1 && pixel.x <= scissorX2 && pixel.y >= scissorY1 && pixel.y <= scissorY2);
		}

		@Override
		public int getCompilationId() {
			return 506062748;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class CompiledFilter14 implements IPixelFilter {
		private IRandomTextureAccess textureAccess;
		private int alphaReferenceValue;

		public CompiledFilter14(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			this.textureAccess = textureAccess;
			alphaReferenceValue = context.alphaRef;
		}

		@Override
		public void filter(PixelState pixel) {
			// TextureReader$TextureReader2D,
			// TextureFunction$TextureEffectModulateRGBA,
			// NopFilter,
			// AlphaTestFilter$AlphaFunctionPassIfGreater,
			// DepthTestFilter$DepthTestPassWhenDepthIsGreaterOrEqual,
			// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
			pixel.filterPassed = pixel.sourceDepth >= pixel.destinationDepth;
			if (pixel.filterPassed) {
				pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u), pixelToTexel(pixel.v));
				pixel.source = multiply(pixel.source, pixel.primaryColor);
				pixel.filterPassed = getAlpha(pixel.source) > alphaReferenceValue;
				if (pixel.filterPassed) {
					// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
					int srcAlpha = PixelColor.getAlpha(pixel.source);
					if (srcAlpha == PixelColor.ZERO) {
						pixel.source = PixelColor.setBGR(pixel.source, pixel.destination);
					} else if (srcAlpha == PixelColor.ONE) {
						// Nothing to change
					} else {
						int oneMinusSrcAlpha = PixelColor.ONE - srcAlpha;
						int filteredSrc = PixelColor.multiplyBGR(pixel.source, srcAlpha, srcAlpha, srcAlpha);
						int filteredDst = PixelColor.multiplyBGR(pixel.destination, oneMinusSrcAlpha, oneMinusSrcAlpha, oneMinusSrcAlpha);
						int source = PixelColor.addBGR(filteredSrc, filteredDst);
						pixel.source = PixelColor.setBGR(pixel.source, source);
					}
				}
			}
		}

		@Override
		public int getCompilationId() {
			return 407288136;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH;
		}
	}

	private static class CompiledFilter15 implements IPixelFilter {
		private IPixelFilter lightingFilter;
		private IRandomTextureAccess textureAccess;
		private int alphaReferenceValue;
		private int width;
		private int height;

		public CompiledFilter15(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel, IPixelFilter lightingFilter) {
			this.lightingFilter = lightingFilter;
			this.textureAccess = textureAccess;
			alphaReferenceValue = context.alphaRef;
			width = context.texture_width[mipmapLevel];
			height = context.texture_height[mipmapLevel];
		}

		@Override
		public void filter(PixelState pixel) {
			// Lighting$LightingFilter,
			// TextureWrap$TextureWrapRepeatST,
			// TextureReader$TextureReader3D,
			// TextureFunction$TextureEffectModulateRGBA,
			// SourceColorFilter$SourceSecondary,
			// AlphaTestFilter$AlphaFunctionPassIfGreater,
			// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha),
			// StencilTestFilter$StencilOpKeep,
			// MaskFilter$DepthMask
			lightingFilter.filter(pixel);
			pixel.u = wrap(pixel.u);
			pixel.v = wrap(pixel.v);
			pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u * width), pixelToTexel(pixel.v * height));
			pixel.source = multiply(pixel.source, pixel.primaryColor);
			pixel.source = add(pixel.source, pixel.secondaryColor);
			pixel.filterPassed = getAlpha(pixel.source) > alphaReferenceValue;
			if (pixel.filterPassed) {
				// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
				int srcAlpha = PixelColor.getAlpha(pixel.source);
				if (srcAlpha == PixelColor.ZERO) {
					pixel.source = PixelColor.setBGR(pixel.source, pixel.destination);
				} else if (srcAlpha == PixelColor.ONE) {
					// Nothing to change
				} else {
					int oneMinusSrcAlpha = PixelColor.ONE - srcAlpha;
					int filteredSrc = PixelColor.multiplyBGR(pixel.source, srcAlpha, srcAlpha, srcAlpha);
					int filteredDst = PixelColor.multiplyBGR(pixel.destination, oneMinusSrcAlpha, oneMinusSrcAlpha, oneMinusSrcAlpha);
					int source = PixelColor.addBGR(filteredSrc, filteredDst);
					pixel.source = PixelColor.setBGR(pixel.source, source);
				}
				pixel.source = (pixel.source & 0x00FFFFFF) | (pixel.destination & 0xFF000000);
				pixel.sourceDepth = pixel.destinationDepth;
			}
		}

		@Override
		public int getCompilationId() {
			return 367346109;
		}

		@Override
		public int getFlags() {
			return DISCARDS_SOURCE_DEPTH;
		}
	}

	private static class CompiledFilter16 implements IPixelFilter {
		private IRandomTextureAccess textureAccess;
		private int scissorX1;
		private int scissorY1;
		private int scissorX2;
		private int scissorY2;

		public CompiledFilter16(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			this.textureAccess = textureAccess;
			scissorX1 = context.scissor_x1;
			scissorY1 = context.scissor_y1;
			scissorX2 = context.scissor_x2;
			scissorY2 = context.scissor_y2;
		}

		@Override
		public void filter(PixelState pixel) {
			// TextureReader$TextureReader2D,
			// TextureFunction$TextureEffectModulateRGBA,
			// ScissorFilter$ScissorXY,
			// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
			pixel.filterPassed = (pixel.x >= scissorX1 && pixel.x <= scissorX2 && pixel.y >= scissorY1 && pixel.y <= scissorY2);
			if (pixel.filterPassed) {
				pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u), pixelToTexel(pixel.v));
				pixel.source = multiply(pixel.source, pixel.primaryColor);
				// BlendOperationAdd(src=BlendFactorSrcAlpha, dst=BlendFactorOneMinusSrcAlpha)
				int srcAlpha = PixelColor.getAlpha(pixel.source);
				if (srcAlpha == PixelColor.ZERO) {
					pixel.source = PixelColor.setBGR(pixel.source, pixel.destination);
				} else if (srcAlpha == PixelColor.ONE) {
					// Nothing to change
				} else {
					int oneMinusSrcAlpha = PixelColor.ONE - srcAlpha;
					int filteredSrc = PixelColor.multiplyBGR(pixel.source, srcAlpha, srcAlpha, srcAlpha);
					int filteredDst = PixelColor.multiplyBGR(pixel.destination, oneMinusSrcAlpha, oneMinusSrcAlpha, oneMinusSrcAlpha);
					int source = PixelColor.addBGR(filteredSrc, filteredDst);
					pixel.source = PixelColor.setBGR(pixel.source, source);
				}
			}
		}

		@Override
		public int getCompilationId() {
			return 705446947;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class CompiledFilter17 implements IPixelFilter {
		private int scissorX1;
		private int scissorY1;
		private int scissorX2;
		private int scissorY2;

		public CompiledFilter17(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			scissorX1 = context.scissor_x1;
			scissorY1 = context.scissor_y1;
			scissorX2 = context.scissor_x2;
			scissorY2 = context.scissor_y2;
		}

		@Override
		public void filter(PixelState pixel) {
			// SourceColorFilter$SourcePrimary,
			// ScissorFilter$ScissorXY,
			// StencilTestFilter$StencilOpZero
			pixel.filterPassed = (pixel.x >= scissorX1 && pixel.x <= scissorX2 && pixel.y >= scissorY1 && pixel.y <= scissorY2);
			if (pixel.filterPassed) {
				pixel.source = pixel.primaryColor;
				pixel.source &= 0x00FFFFFF;
			}
		}

		@Override
		public int getCompilationId() {
			return 642621606;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class CompiledFilter18 implements IPixelFilter {
		private int scissorX1;
		private int scissorY1;
		private int scissorX2;
		private int scissorY2;

		public CompiledFilter18(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			scissorX1 = context.scissor_x1;
			scissorY1 = context.scissor_y1;
			scissorX2 = context.scissor_x2;
			scissorY2 = context.scissor_y2;
		}

		@Override
		public void filter(PixelState pixel) {
			// SourceColorFilter$SourcePrimary,
			// ScissorFilter$ScissorXY,
			// MaskFilter$NoColorMask
			pixel.filterPassed = (pixel.x >= scissorX1 && pixel.x <= scissorX2 && pixel.y >= scissorY1 && pixel.y <= scissorY2);
			if (pixel.filterPassed) {
				pixel.source = pixel.primaryColor;
				pixel.source = pixel.destination;
			}
		}

		@Override
		public int getCompilationId() {
			return 108803444;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static class CompiledFilter19 implements IPixelFilter {
		private int alphaReferenceValue;
		private IRandomTextureAccess textureAccess;
		private int width;
		private int height;

		public CompiledFilter19(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			alphaReferenceValue = context.alphaRef;
			this.textureAccess = textureAccess;
			width = context.texture_width[mipmapLevel];
			height = context.texture_height[mipmapLevel];
		}

		@Override
		public void filter(PixelState pixel) {
			// VertexColorFilter$VertexTriangleTextureFilter,
			// TextureWrap$TextureWrapRepeatST,
			// TextureReader$TextureReader3D,
			// TextureFunction$TextureEffectModulateRGBA,
			// ColorDoubling$SourceColorDoubling,
			// AlphaTestFilter$AlphaFunctionPassIfGreater,
			// DepthTestFilter$DepthTestPassWhenDepthIsGreaterOrEqual,
			pixel.filterPassed = pixel.sourceDepth >= pixel.destinationDepth;
			if (pixel.filterPassed) {
				int a = pixel.getTriangleWeightedValue(pixel.c1a, pixel.c2a, pixel.c3a);
				int b = pixel.getTriangleWeightedValue(pixel.c1b, pixel.c2b, pixel.c3b);
				int g = pixel.getTriangleWeightedValue(pixel.c1g, pixel.c2g, pixel.c3g);
				int r = pixel.getTriangleWeightedValue(pixel.c1r, pixel.c2r, pixel.c3r);
				pixel.primaryColor = getColor(a, b, g, r);
				pixel.u = wrap(pixel.u);
				pixel.v = wrap(pixel.v);
				pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u * width), pixelToTexel(pixel.v * height));
				pixel.source = multiply(pixel.source, pixel.primaryColor);
				pixel.source = doubleColor(pixel.source);
				pixel.filterPassed = getAlpha(pixel.source) > alphaReferenceValue;
			}
		}

		@Override
		public int getCompilationId() {
			return 918242030;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | REQUIRES_TEXTURE_U_V;
		}
	}

	private static class CompiledFilter20 implements IPixelFilter {
		private int alphaReferenceValue;
		private IRandomTextureAccess textureAccess;
		private int width;
		private int height;

		public CompiledFilter20(GeContext context, IRandomTextureAccess textureAccess, int mipmapLevel) {
			alphaReferenceValue = context.alphaRef;
			this.textureAccess = textureAccess;
			width = context.texture_width[mipmapLevel];
			height = context.texture_height[mipmapLevel];
		}

		@Override
		public void filter(PixelState pixel) {
			// VertexColorFilter$VertexTriangleTextureFilter,
			// TextureWrap$TextureWrapRepeatST,
			// TextureReader$TextureReader3D,
			// TextureFunction$TextureEffectModulateRGBA,
			// ColorDoubling$SourceColorDoubling,
			// AlphaTestFilter$AlphaFunctionPassIfGreater,
			// DepthTestFilter$DepthTestPassWhenDepthIsGreaterOrEqual,
			// MaskFilter$DepthMask
			int a = pixel.getTriangleWeightedValue(pixel.c1a, pixel.c2a, pixel.c3a);
			int b = pixel.getTriangleWeightedValue(pixel.c1b, pixel.c2b, pixel.c3b);
			int g = pixel.getTriangleWeightedValue(pixel.c1g, pixel.c2g, pixel.c3g);
			int r = pixel.getTriangleWeightedValue(pixel.c1r, pixel.c2r, pixel.c3r);
			pixel.primaryColor = getColor(a, b, g, r);
			pixel.u = wrap(pixel.u);
			pixel.v = wrap(pixel.v);
			pixel.source = textureAccess.readPixel(pixelToTexel(pixel.u * width), pixelToTexel(pixel.v * height));
			pixel.source = multiply(pixel.source, pixel.primaryColor);
			pixel.source = doubleColor(pixel.source);
			pixel.filterPassed = getAlpha(pixel.source) > alphaReferenceValue;
			if (pixel.filterPassed) {
				pixel.filterPassed = pixel.sourceDepth >= pixel.destinationDepth;
				if (pixel.filterPassed) {
					pixel.sourceDepth = pixel.destinationDepth;
				}
			}
		}

		@Override
		public int getCompilationId() {
			return 759297165;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH | DISCARDS_SOURCE_DEPTH | REQUIRES_TEXTURE_U_V;
		}
	}
}
