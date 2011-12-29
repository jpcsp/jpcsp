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

import static jpcsp.graphics.RE.software.PixelColor.getColor;
import static jpcsp.util.Utilities.max;
import static jpcsp.util.Utilities.min;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VertexState;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.ImageReader;

/**
 * @author gid15
 *
 */
public abstract class BaseRenderer implements IRenderer {
	protected static final Logger log = VideoEngine.log;
    protected static final int depthBufferPixelFormat = GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED;
	protected final PixelState pixel = new PixelState();
	protected int p1x, p1y;
	protected int p2x, p2y;
	protected int p3x, p3y;
	protected int pxMin, pxMax, pyMin, pyMax;
	protected float p1z, p2z, p3z;
	protected int t1x, t1y;
	protected int t2x, t2y;
	protected int t3x, t3y;
	protected int txMin, txMax, tyMin, tyMax;
	protected int destinationWidth;
	protected int destinationHeight;
	protected ISourceReader sourceReader;
	protected IImageWriter imageWriter;
	protected IImageWriter depthWriter;
	protected final IPixelFilter[] filters = new IPixelFilter[10];
	protected int numberFilters;
	protected boolean needScissoring;

	protected void setPositions(VertexState v1, VertexState v2) {
        p1x = Math.round(v1.p[0]);
        p1y = Math.round(v1.p[1]);
        p1z = v1.p[2];
        p2x = Math.round(v2.p[0]);
        p2y = Math.round(v2.p[1]);
        p2z = v2.p[2];

        pxMax = max(p1x, p2x);
        pxMin = min(p1x, p2x);
        pyMax = max(p1y, p2y);
        pyMin = min(p1y, p2y);
	}

	protected void setPositions(VertexState v1, VertexState v2, VertexState v3) {
		setPositions(v1, v2);

		p3x = Math.round(v3.p[0]);
        p3y = Math.round(v3.p[1]);
        p3z = v3.p[2];

        pxMax = max(pxMax, p3x);
        pxMin = min(pxMin, p3x);
        pyMax = max(pyMax, p3y);
        pyMin = min(pyMin, p3y);
	}

	protected void setTextures(VertexState v1, VertexState v2) {
        t1x = Math.round(v1.t[0]);
        t1y = Math.round(v1.t[1]);
        t2x = Math.round(v2.t[0]);
        t2y = Math.round(v2.t[1]);

        txMax = max(t1x, t2x);
        txMin = min(t1x, t2x);
        tyMax = max(t1y, t2y);
        tyMin = min(t1y, t2y);
	}

	protected void setTextures(VertexState v1, VertexState v2, VertexState v3) {
		setTextures(v1, v2);

		t3x = Math.round(v3.t[0]);
        t3y = Math.round(v3.t[1]);

        txMax = max(txMax, t3x);
        txMin = min(txMin, t3x);
        tyMax = max(tyMax, t3y);
        tyMin = min(tyMin, t3y);
	}

	protected boolean isInsideTriangle() {
		// Based on http://en.wikipedia.org/wiki/Barycentric_coordinates_%28mathematics%29
		int diff03x = pixel.x - p3x;
		int diff03y = pixel.y - p3y;
		int diff13x = p1x - p3x;
		int diff13y = p1y - p3y;
		int diff32x = p3x - p2x;
		int diff23y = p2y - p3y;

		int denom = diff23y * diff13x + diff32x * diff13y;
		float invDenom = 1.0f / denom;
		pixel.triangleWeight1 = (diff23y * diff03x + diff32x * diff03y) * invDenom;
		pixel.triangleWeight2 = (diff13x * diff03y - diff13y * diff03x) * invDenom;
		pixel.triangleWeight3 = 1.f - (pixel.triangleWeight1 + pixel.triangleWeight2);

		return pixel.triangleWeight1 >= 0.f && pixel.triangleWeight2 >= 0.f && pixel.triangleWeight3 >= 0.f;
	}

	protected int getFrameBufferAddress(GeContext context, int x, int y) {
		int baseAddress = context.fbp & Memory.addressMask;
		if (baseAddress < MemoryMap.START_VRAM) {
			baseAddress += MemoryMap.START_VRAM;
		}
		return getTextureAddress(baseAddress, x, y, context.fbw, context.psm);
	}

	protected int getDepthBufferAddress(GeContext context, int x, int y) {
		int baseAddress = context.zbp & Memory.addressMask;
		if (baseAddress < MemoryMap.START_VRAM) {
			baseAddress += MemoryMap.START_VRAM;
		}
		return getTextureAddress(baseAddress, x, y, context.zbw, depthBufferPixelFormat);
	}

	protected int getTextureAddress(int address, int x, int y, int textureWidth, int pixelFormat) {
		int bytesPerPixel = IRenderingEngine.sizeOfTextureType[pixelFormat];
		int numberOfPixels = y * textureWidth + x;
		// bytesPerPixel == 0 means 2 pixels per byte (4bit indexed)
		return address + (bytesPerPixel == 0 ? numberOfPixels >> 1 : numberOfPixels * bytesPerPixel);
	}

	protected boolean insideScissor(GeContext context) {
        needScissoring = false;
        if (!context.clearMode) {
    		if (context.vinfo.texture == 0) {
    			pxMin = Math.max(pxMin, context.scissor_x1);
    			pxMax = Math.min(pxMax, context.scissor_x2);
    			pyMin = Math.max(pyMin, context.scissor_y1);
    			pyMax = Math.min(pyMax, context.scissor_y2);
    		}

    		// Scissoring
        	if (pxMax < context.scissor_x1 || pxMin > context.scissor_x2) {
        		// Completely outside the scissor area, skip
        		return false;
        	}
        	if (pyMax < context.scissor_y1 || pyMin > context.scissor_y2) {
        		// Completely outside the scissor area, skip
        		return false;
        	}

        	if (pxMin < context.scissor_x1 || pxMax > context.scissor_x2) {
        		// partially outside the scissor area, use the scissoring filter
        		needScissoring = true;
        	} else if (pyMin < context.scissor_y1 || pyMax > context.scissor_y2) {
        		// partially outside the scissor area, use the scissoring filter
        		needScissoring = true;
        	}
        }

        return true;
	}

	protected void prepareSourceReader(GeContext context, VertexState v1, VertexState v2) {
		prepareSourceReader(context, v1, v2, null);
	}

	protected void prepareSourceReader(GeContext context, VertexState v1, VertexState v2, VertexState v3) {
		if (context.useVertexColor && context.vinfo.color != 0) {
			pixel.primaryColor = getColor(v1.c);
		} else {
			pixel.primaryColor = getColor(context.vertexColor);
		}
        destinationWidth = pxMax - pxMin;
        destinationHeight = pyMax - pyMin;
    	sourceReader = null;
    	if (context.vinfo.texture != 0 && context.textureFlag.isEnabled()) {
    		final int level = 0;
    		int textureBufferWidth = VideoEngine.alignBufferWidth(context.texture_buffer_width[level], context.texture_storage);
    		int textureHeight = context.texture_height[level];
            int sourceWidth = Math.min(textureBufferWidth, txMax - txMin);
            int sourceHeight = tyMax - tyMin;
    		int cropLeft = txMin;
    		int cropRight = textureBufferWidth - sourceWidth - cropLeft;
    		int cropTop = tyMin;
    		int cropBottom = textureHeight - sourceHeight - cropTop;
            int textureAddress = context.texture_base_pointer[level];
            boolean flipX = false;
            boolean flipY = false;
            boolean rotate = false;
            if (v3 == null) {
            	flipX = (t1x > t2x) ^ (p1x > p2x);
            	flipY = (t1y > t2y) ^ (p1y > p2y);
            }
            if (flipX && flipY) {
            	rotate = true;
            	flipX = false;
            	flipY = false;
            }
        	int[] clut32 = VideoEngine.getInstance().readClut32(level);
        	short[] clut16 = VideoEngine.getInstance().readClut16(level);
        	// Always request the whole buffer width, the texture will be cropped
        	// as required in the next step
            IMemoryReader imageReader = ImageReader.getImageReader(textureAddress, textureBufferWidth, textureHeight, textureBufferWidth, context.texture_storage, context.texture_swizzle, context.tex_clut_addr, context.tex_clut_mode, context.tex_clut_num_blocks, context.tex_clut_start, context.tex_clut_shift, context.tex_clut_mask, clut32, clut16);

            // Flip and rotate the image
            imageReader = ImageFlip.getImageFlip(imageReader, textureBufferWidth, textureHeight, flipX, flipY, rotate);
        	if (rotate) {
            	int swap = cropLeft;
            	cropLeft = cropTop;
            	cropTop = swap;

            	swap = cropRight;
            	cropRight = cropBottom;
            	cropBottom = cropRight;

            	swap = sourceWidth;
        		sourceWidth = sourceHeight;
        		sourceHeight = swap;
        	}
            if (flipX) {
            	int swap = cropLeft;
            	cropLeft = cropRight;
            	cropRight = swap;
            }
            if (flipY) {
            	int swap = cropTop;
            	cropTop = cropBottom;
            	cropBottom = swap;
            }

            imageReader = ImageCrop.getImageCrop(imageReader, sourceWidth, cropLeft, cropRight, cropTop);
        	imageReader = ImageResizer.getImageResizer(imageReader, sourceWidth, sourceHeight, destinationWidth, destinationHeight);
            sourceReader = TextureFunction.getTextureFunction(imageReader, context);
    	} else if (context.useVertexColor) {
    		sourceReader = VertexSourceReader.getVertexSourceReader(v1, v2, v3);
		} else {
			sourceReader = new ColorSourceReader(context.vertexColor);
		}
	}

	protected void prepareWriters(GeContext context) {
        int fbAddress = getFrameBufferAddress(context, pxMin, pyMin);
    	int depthAddress = getDepthBufferAddress(context, pxMin, pyMin);
        imageWriter = ImageWriter.getImageWriter(fbAddress, destinationWidth, context.fbw, context.psm);
        depthWriter = ImageWriter.getImageWriter(depthAddress, destinationWidth, context.zbw, depthBufferPixelFormat);
	}

	private boolean addFilter(IPixelFilter filter) {
		if (filter == null || filter == NopFilter.NOP) {
			return false;
		}

		filters[numberFilters++] = filter;

		return true;
	}

	protected void prepareFilters(GeContext context) {
		boolean added;

		numberFilters = 0;
        //
        // Add all the enabled tests and filters, in the correct processing order
        //
        if (needScissoring) {
        	added = addFilter(ScissorFilter.getScissorFilter(context));
        	if (added && log.isTraceEnabled()) {
        		log.trace(String.format("Using ScissorFilter (%d,%d)-(%d,%d)", context.scissor_x1, context.scissor_y1, context.scissor_x2, context.scissor_y2));
        	}
        }
        if (context.colorTestFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(ColorTestFilter.getColorTestFilter(context));
        	if (added && log.isTraceEnabled()) {
        		log.trace(String.format("Using ColorTestFilter func=%d, ref=0x%02X, mask=0x%02X", context.colorTestFunc, context.colorTestRef, context.colorTestMsk));
        	}
        }
        if (context.alphaTestFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(AlphaTestFilter.getAlphaTestFilter(context));
        	if (added && log.isTraceEnabled()) {
        		log.trace(String.format("Using AlphaTestFilter func=%d, ref=0x%02X", context.alphaFunc, context.alphaRef));
        	}
        }
        if (context.stencilTestFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(StencilTestFilter.getStencilTestFilter(context));
        	if (added && log.isTraceEnabled()) {
        		log.trace(String.format("Using StencilTestFilter func=%d, ref=0x%02X, mask=0x%02X", context.stencilFunc, context.stencilRef, context.stencilMask));
        	}
        }
        if (context.depthTestFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(DepthTestFilter.getDepthTestFilter(context));
        	if (added && log.isTraceEnabled()) {
        		log.trace(String.format("Using DepthTestFilter func=%d", context.depthFunc));
        	}
        }
        if (context.blendFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(AlphaBlendFilter.getAlphaBlendFilter(context));
        	if (added && log.isTraceEnabled()) {
        		log.trace(String.format("Using AlphaBlendFilter func=%d, src=%d, dst=%d, sfix=0x%06X, dfix=0x%06X", context.blendEquation, context.blend_src, context.blend_dst, context.sfix, context.dfix));
        	}
        }
        if (context.colorLogicOpFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(LogicalOperationFilter.getLogicalOperationFilter(context));
        	if (added && log.isTraceEnabled()) {
        		log.trace(String.format("Using LogicalOperationFilter func=%d", context.logicOp));
        	}
        }
        {
        	added = addFilter(MaskFilter.getMaskFilter(context, context.clearMode, context.clearModeColor, context.clearModeStencil, context.clearModeDepth));
        	if (added && log.isTraceEnabled()) {
        		log.trace(String.format("Using MaskFilter colorMask=0x%02X%02X%02X, depthMask=%b, clearMode=%b, clearModeColor=%b, clearModeStencil=%b, clearModeDepth=%b", context.colorMask[0], context.colorMask[1], context.colorMask[2], context.depthMask, context.clearMode, context.clearModeColor, context.clearModeStencil, context.clearModeDepth));
        	}
        }
	}
}
