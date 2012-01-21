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
import static jpcsp.util.Utilities.matrixMult;
import static jpcsp.util.Utilities.max;
import static jpcsp.util.Utilities.maxInt;
import static jpcsp.util.Utilities.min;
import static jpcsp.util.Utilities.minInt;
import static jpcsp.util.Utilities.round;
import static jpcsp.util.Utilities.vectorMult44;

import java.nio.Buffer;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VertexState;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.capture.CaptureManager;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryReaderWriter;
import jpcsp.memory.ImageReader;

/**
 * @author gid15
 *
 */
public abstract class BaseRenderer implements IRenderer {
	protected static final Logger log = VideoEngine.log;
    protected final boolean isLogTraceEnabled;
    protected final boolean isLogDebugEnabled;

    protected static final boolean captureEachPrimitive = false;
	protected static final boolean captureZbuffer = false;
    public static final int depthBufferPixelFormat = GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED;
    protected static final int MAX_NUMBER_FILTERS = 11;
	protected final PixelState pixel = new PixelState();
	protected float p1x, p1y, p1z, p1w, p1wInverted;
	protected float p2x, p2y, p2z, p2w, p2wInverted;
	protected float p3x, p3y, p3z, p3w, p3wInverted;
	protected int pxMin, pxMax, pyMin, pyMax, pzMin, pzMax;
	protected float t1u, t1v;
	protected float t2u, t2v;
	protected float t3u, t3v;
	protected int tuMin, tuMax, tvMin, tvMax;
	protected float uStart, uStep;
	protected float vStart, vStep;
	protected int destinationWidth;
	protected int destinationHeight;
	protected IMemoryReaderWriter imageWriter;
	protected IMemoryReaderWriter depthWriter;
	protected int imageWriterSkipEOL;
	protected int depthWriterSkipEOL;
	protected final IPixelFilter[] filters = new IPixelFilter[MAX_NUMBER_FILTERS];
	protected int numberFilters;
	protected boolean needScissoringX;
	protected boolean needScissoringY;
	protected boolean transform2D;
	protected int viewportWidth;
	protected int viewportHeight;
	protected int viewportX;
	protected int viewportY;
	protected int screenOffsetX;
	protected int screenOffsetY;
	protected float zscale;
	protected float zpos;
	protected int fbw;
	protected int nearZ;
	protected int farZ;
	// Pre-computed values for triangle weights
	private float diff13x;
	private float diff13y;
	private float diff32x;
	private float diff23y;
	private float denomInverted;

	protected BaseRenderer() {
		isLogTraceEnabled = log.isTraceEnabled();
		isLogDebugEnabled = log.isDebugEnabled();
	}

	protected void addPosition(float[] p) {
		float[] screenCoordinates = new float[4];
		getScreenCoordinates(screenCoordinates, p);
		pxMax = maxInt(pxMax, screenCoordinates[0]);
		pxMin = minInt(pxMin, screenCoordinates[0]);
		pyMax = maxInt(pyMax, screenCoordinates[1]);
		pyMin = minInt(pyMin, screenCoordinates[1]);
		pzMax = maxInt(pzMax, screenCoordinates[2]);
		pzMin = minInt(pzMin, screenCoordinates[2]);
	}

	protected void setPositions(VertexState v1, VertexState v2) {
        pixel.v1x = v1.p[0];
        pixel.v1y = v1.p[1];
        pixel.v1z = v1.p[2];
        pixel.n1x = v1.n[0];
        pixel.n1y = v1.n[1];
        pixel.n1z = v1.n[2];

        pixel.v2x = v2.p[0];
        pixel.v2y = v2.p[1];
        pixel.v2z = v2.p[2];
        pixel.n2x = v2.n[0];
        pixel.n2y = v2.n[1];
        pixel.n2z = v2.n[2];

        if (transform2D) {
	        p1x = pixel.v1x;
	        p1y = pixel.v1y;
	        p1z = pixel.v1z;
	        p2x = pixel.v2x;
	        p2y = pixel.v2y;
	        p2z = pixel.v2z;
		} else {
			float[] screenCoordinates = new float[4];
			getScreenCoordinates(screenCoordinates, pixel.v1x, pixel.v1y, pixel.v1z);
			p1x = screenCoordinates[0];
			p1y = screenCoordinates[1];
			p1z = screenCoordinates[2];
			p1w = screenCoordinates[3];
			p1wInverted = 1.f / p1w;
			getScreenCoordinates(screenCoordinates, pixel.v2x, pixel.v2y, pixel.v2z);
			p2x = screenCoordinates[0];
			p2y = screenCoordinates[1];
			p2z = screenCoordinates[2];
			p2w = screenCoordinates[3];
			p2wInverted = 1.f / p2w;
		}

        pxMax = maxInt(p1x, p2x);
        pxMin = minInt(p1x, p2x);
        pyMax = maxInt(p1y, p2y);
        pyMin = minInt(p1y, p2y);
        pzMax = maxInt(p1z, p2z);
        pzMin = minInt(p1z, p2z);
	}

	protected void setPositions(VertexState v1, VertexState v2, VertexState v3) {
		setPositions(v1, v2);

		pixel.v3x = v3.p[0];
		pixel.v3y = v3.p[1];
		pixel.v3z = v3.p[2];
		pixel.n3x = v3.n[0];
		pixel.n3y = v3.n[1];
		pixel.n3z = v3.n[2];

        if (transform2D) {
			p3x = pixel.v3x;
	        p3y = pixel.v3y;
	        p3z = pixel.v3z;
		} else {
			float[] screenCoordinates = new float[4];
			getScreenCoordinates(screenCoordinates, pixel.v3x, pixel.v3y, pixel.v3z);
			p3x = screenCoordinates[0];
			p3y = screenCoordinates[1];
			p3z = screenCoordinates[2];
			p3w = screenCoordinates[3];
			p3wInverted = 1.f / p3w;
		}

        pxMax = maxInt(pxMax, p3x);
        pxMin = minInt(pxMin, p3x);
        pyMax = maxInt(pyMax, p3y);
        pyMin = minInt(pyMin, p3y);
        pzMax = maxInt(pzMax, p3z);
        pzMin = minInt(pzMin, p3z);
	}

	protected void setTextures(VertexState v1, VertexState v2) {
        t1u = v1.t[0];
        t1v = v1.t[1];
        t2u = v2.t[0];
        t2v = v2.t[1];

        if (transform2D) {
	        tuMax = max(round(t1u), round(t2u));
	        tuMin = min(round(t1u), round(t2u));
	        tvMax = max(round(t1v), round(t2v));
	        tvMin = min(round(t1v), round(t2v));
        }
	}

	protected void setTextures(VertexState v1, VertexState v2, VertexState v3) {
		setTextures(v1, v2);

		t3u = v3.t[0];
        t3v = v3.t[1];

        if (transform2D) {
	        tuMax = max(tuMax, round(t3u));
	        tuMin = min(tuMin, round(t3u));
	        tvMax = max(tvMax, round(t3v));
	        tvMin = min(tvMin, round(t3v));
        }
	}

	protected void preComputeTriangleWeights() {
		diff13x = p1x - p3x;
		diff13y = p1y - p3y;
		diff32x = p3x - p2x;
		diff23y = p2y - p3y;

		float denom = diff23y * diff13x + diff32x * diff13y;
		denomInverted = 1.0f / denom;
	}

	protected void computeTriangleWeights() {
		// Based on http://en.wikipedia.org/wiki/Barycentric_coordinates_%28mathematics%29
		//
		// All the values independent of the current pixel haven been pre-computed
		// in preComputeTriangleWeights().
		//
		float diff03x = pixel.x - p3x;
		float diff03y = pixel.y - p3y;
		pixel.triangleWeight1 = (diff23y * diff03x + diff32x * diff03y) * denomInverted;
		pixel.triangleWeight2 = (diff13x * diff03y - diff13y * diff03x) * denomInverted;
		pixel.triangleWeight3 = 1.f - (pixel.triangleWeight1 + pixel.triangleWeight2);
	}

	/**
	 * Update the triangle weights (Barycentric coordinates) by knowing that
	 *    pixel.x
	 * has been incremented by 1.
	 */
	protected void deltaXTriangleWeigths() {
		// When pixel.x is incremented by 1, diff03x is also incremented by 1.
		// Which leads to simple increments of the triangle weights.
		pixel.triangleWeight1 += diff23y * denomInverted;
		pixel.triangleWeight2 -= diff13y * denomInverted;
		pixel.triangleWeight3 = 1.f - (pixel.triangleWeight1 + pixel.triangleWeight2);
	}

	protected boolean isClockwise() {
		float crossProduct = (p2x - p1x) * (p3y - p1y) - (p2y - p1y) * (p3x - p1x);
		return crossProduct >= 0.f;
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

	protected boolean isVisible(GeContext context) {
        if (!context.clearMode) {
        	if (!transform2D) {
        		// Each vertex screen coordinates (without offset) has to be in the range:
        		// - x: [0..4095]
        		// - y: [0..4095]
        		// - z: [..65535]
        		// If one of the vertex coordinate is not in the valid range, the whole
        		// primitive is discarded.
	        	if ((pxMin + screenOffsetX) < 0 ||
	        	    (pxMax + screenOffsetX) >= 4096 ||
	        	    (pyMin + screenOffsetY) < 0 ||
	        	    (pyMax + screenOffsetY) >= 4096 ||
	        	    pzMax >= 65536) {
	        		return false;
	        	}

	        	// This is probably a rounding error when one triangle
	        	// extends from back to front over a very large distance
	        	// (more than the allowed range for Z values).
	        	if (pzMin < 0 && pzMax > 0 && pzMax - pzMin > 65536) {
	        		return false;
	        	}

	        	if (!context.clipPlanesFlag.isEnabled()) {
	        		// The primitive is discarded when one of the vertex is behind the viewpoint
	        		// (only the the ClipPlanes flag is not enabled).
	        		if (pzMin < 0) {
	        			return false;
	        		}
	        	}
        	}

    		if (context.vinfo.texture == 0) {
    			pxMin = Math.max(pxMin, context.scissor_x1);
    			pxMax = Math.min(pxMax, context.scissor_x2);
    			pyMin = Math.max(pyMin, context.scissor_y1);
    			pyMax = Math.min(pyMax, context.scissor_y2);
    		}

    		pxMin = Math.max(0, pxMin);
    		pxMax = Math.min(pxMax, context.fbw);
    		pyMin = Math.max(0, pyMin);
    		pyMax = Math.min(pyMax, 1024);

    		if (pxMin == pxMax || pyMin == pyMax) {
    			// Empty area to be displayed
    			return false;
    		}

    		if (!insideScissor(context)) {
    			return false;
    		}
        }

        return true;
	}

	protected boolean insideScissor(GeContext context) {
        needScissoringX = false;
        needScissoringY = false;
        if (!context.clearMode) {
    		// Scissoring
        	if (pxMax < context.scissor_x1 || pxMin > context.scissor_x2) {
        		// Completely outside the scissor area, skip
        		return false;
        	}
        	if (pyMax < context.scissor_y1 || pyMin > context.scissor_y2) {
        		// Completely outside the scissor area, skip
        		return false;
        	}
        	if (!transform2D) {
	        	if (pzMax < nearZ || pzMin > farZ) {
	        		// Completely outside the view area, skip
	        		return false;
	        	}
        	}

        	if (pxMin < context.scissor_x1 || pxMax > context.scissor_x2) {
        		// partially outside the scissor area, use the scissoring filter
        		needScissoringX = true;
        	}
        	if (pyMin < context.scissor_y1 || pyMax > context.scissor_y2) {
        		// partially outside the scissor area, use the scissoring filter
        		needScissoringY = true;
        	}
        }

        return true;
	}

	protected void init(GeContext context) {
		pxMax = Integer.MIN_VALUE;
		pxMin = Integer.MAX_VALUE;
		pyMax = Integer.MIN_VALUE;
		pyMin = Integer.MAX_VALUE;
		pzMax = Integer.MIN_VALUE;
		pzMin = Integer.MAX_VALUE;
		fbw = context.fbw;
		nearZ = round(context.nearZ * 0xFFFF);
		farZ = round(context.farZ * 0xFFFF);
		numberFilters = 0;

		transform2D = context.vinfo.transform2D;
		if (!transform2D) {
			// Copy the View matrix
			System.arraycopy(context.view_uploaded_matrix, 0, pixel.viewMatrix, 0, pixel.viewMatrix.length);

			// Pre-compute the Model-View matrix
			matrixMult(pixel.modelViewMatrix, context.view_uploaded_matrix, context.model_uploaded_matrix);

			// Pre-compute the Model-View-Projection matrix
			matrixMult(pixel.modelViewProjectionMatrix, context.proj_uploaded_matrix, pixel.modelViewMatrix);

			viewportWidth = context.viewport_width;
			viewportHeight = context.viewport_height;
			viewportX = context.viewport_cx;
			viewportY = context.viewport_cy;
			screenOffsetX = context.offset_x;
			screenOffsetY = context.offset_y;
			zscale = context.zscale * 65535.f;
			zpos = context.zpos * 65535.f;
			pixel.hasNormal = context.vinfo.normal != 0;
		}
	}

	protected void prepareTextureReader(GeContext context, CachedTexture texture, VertexState v1, VertexState v2) {
		prepareTextureReader(context, texture, v1, v2, null);
	}

	private static final String getLightState(GeContext context, int l) {
		if (context.light_enabled[l] == 0) {
			return "OFF";
		}
		return "ON";
	}

	protected void prepareTextureReader(GeContext context, CachedTexture texture, VertexState v1, VertexState v2, VertexState v3) {
		if (context.useVertexColor && context.vinfo.color != 0) {
			pixel.primaryColor = getColor(v2.c);
		} else {
			pixel.primaryColor = getColor(context.vertexColor);
		}

		// The rendering will be performed into the following ranges:
		// 3D:
		//   - x: [pxMin..pxMax] (min and max included)
		//   - y: [pxMin..pxMax] (min and max included)
		// 2D:
		//   - x: [pxMin..pxMax-1] (min included but max excluded)
		//   - y: [pxMin..pxMax-1] (min included but max excluded)
        if (transform2D) {
        	pxMax--;
        	pyMax--;
        } else {
        	// Restrict the drawn area to the scissor area.
        	// We can just update the min/max values, the TextureMapping filter
        	// will take are of the correct texture mapping.
        	// We do no longer need a scissoring filter.
        	if (needScissoringX) {
        		pxMin = max(pxMin, context.scissor_x1);
        		pxMax = min(pxMax, context.scissor_x2);
        		needScissoringX = false;
        	}
        	if (needScissoringY) {
        		pyMin = max(pyMin, context.scissor_y1);
        		pyMax = min(pyMax, context.scissor_y2);
        		needScissoringY = false;
        	}
        }
		destinationWidth = pxMax - pxMin + 1;
        destinationHeight = pyMax - pyMin + 1;

        if (v3 != null) {
        	preComputeTriangleWeights();
        }

        IPixelFilter filter = Lighting.getLighting(context, pixel);
        boolean added = addFilter(filter);
        if (added && isLogTraceEnabled) {
        	log.trace(String.format("Using Lighting Filter %s, %s, %s, %s", getLightState(context, 0), getLightState(context, 1), getLightState(context, 2), getLightState(context, 3)));
        }

        IPixelFilter textureFilter;
    	if (context.textureFlag.isEnabled() && (!transform2D || context.vinfo.texture != 0)) {
    		final int level = 0;
    		int textureBufferWidth = VideoEngine.alignBufferWidth(context.texture_buffer_width[level], context.texture_storage);
    		int textureHeight = context.texture_height[level];
            int textureAddress = context.texture_base_pointer[level];
        	IRandomTextureAccess textureAccess = texture;
        	if (textureAccess == null) {
            	int[] clut32 = VideoEngine.getInstance().readClut32(level);
            	short[] clut16 = VideoEngine.getInstance().readClut16(level);
	        	// Always request the whole buffer width, the texture will be cropped
	        	// as required in the next step
	            IMemoryReader imageReader = ImageReader.getImageReader(textureAddress, textureBufferWidth, textureHeight, textureBufferWidth, context.texture_storage, context.texture_swizzle, context.tex_clut_addr, context.tex_clut_mode, context.tex_clut_num_blocks, context.tex_clut_start, context.tex_clut_shift, context.tex_clut_mask, clut32, clut16);
	            textureAccess = new RandomTextureAccessReader(imageReader, textureBufferWidth, textureHeight);
        	}
        	// Avoid an access outside the texture area
        	textureAccess = new TextureClip(textureAccess, textureBufferWidth, textureHeight);

            if (!transform2D) {
            	// Perform the texture mapping (UV / texture matrix / environment map)
            	textureFilter = TextureMapping.getTextureMapping(context, v1, v2, v3);
            	addFilter(textureFilter);
            } else {
            	boolean flipX = false;
            	boolean flipY = false;
            	if (v3 == null) {
            		flipX = (t1u > t2u) ^ (p1x > p2x);
            		flipY = (t1v > t2v) ^ (p1y > p2y);
            	} else {
            		// TODO compute texture flips for a triangle
            		// In 2D, the texture is already read from top left to bottom right.
            		// This has to be transformed to the desired triangle texture mapping.
            	}
            	if (isLogTraceEnabled) {
            		log.trace(String.format("2D texture flipX=%b, flipY=%b", flipX, flipY));
            	}
            	uStart = flipX ? tuMax : tuMin;
            	float uEnd = flipX ? tuMin : tuMax;
            	vStart = flipY ? tvMax : tvMin;
            	float vEnd = flipY ? tvMin : tvMax;
            	uStep = (uEnd - uStart) / destinationWidth;
            	vStep = (vEnd - vStart) / destinationHeight;
            }

            // Apply the texture wrap mode (clamp or repeat)
            textureFilter = TextureWrap.getTextureWrap(context, level);
            addFilter(textureFilter);

            // Read the corresponding texture texel
            textureFilter = TextureReader.getTextureReader(textureAccess, context, level);
            addFilter(textureFilter);

            // Apply the texture function (modulate, decal, blend, replace, add)
            textureFilter = TextureFunction.getTextureFunction(context);
            addFilter(textureFilter);

            addFilter(SourceColorFilter.getSourceColorFilter(context, false));
    	} else if (context.useVertexColor) {
    		// Read the color from the vertex
    		textureFilter = VertexColorFilter.getVertexColorFilter(v1, v2, v3);
        	addFilter(textureFilter);

        	addFilter(SourceColorFilter.getSourceColorFilter(context, false));
		} else {
			if (isLogTraceEnabled) {
				log.trace(String.format("Using ColorTextureFilter vertexColor=0x%08X", getColor(context.vertexColor)));
			}
            addFilter(SourceColorFilter.getSourceColorFilter(context, true));
		}
	}

	protected void prepareWriters(GeContext context) {
        int fbAddress = getFrameBufferAddress(context, pxMin, pyMin);
    	int depthAddress = getDepthBufferAddress(context, pxMin, pyMin);
    	// Request image writers with a width covering the complete buffer width,
    	// we will skip unnecessary pixels at the end of each line manually (more efficient).
        imageWriter = ImageWriter.getImageWriter(fbAddress, context.fbw, context.fbw, context.psm);
        depthWriter = ImageWriter.getImageWriter(depthAddress, context.zbw, context.zbw, depthBufferPixelFormat);
        imageWriterSkipEOL = context.fbw - destinationWidth;
        depthWriterSkipEOL = context.zbw - destinationWidth;
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

        //
        // Add all the enabled tests and filters, in the correct processing order
        //
        if (needScissoringX || needScissoringY) {
        	added = addFilter(ScissorFilter.getScissorFilter(context, needScissoringX, needScissoringY));
        	if (added && log.isTraceEnabled()) {
        		log.trace(String.format("Using ScissorFilter (%d,%d)-(%d,%d)", context.scissor_x1, context.scissor_y1, context.scissor_x2, context.scissor_y2));
        	}
        }
        if (!transform2D && !context.clearMode) {
        	added = addFilter(ScissorDepthFilter.getScissorDepthFilter(context, nearZ, farZ));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using ScissorDepthFilter near=%f, far=%f", context.nearZ, context.farZ));
        	}
        }
        if (context.colorTestFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(ColorTestFilter.getColorTestFilter(context));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using ColorTestFilter func=%d, ref=0x%02X, mask=0x%02X", context.colorTestFunc, context.colorTestRef, context.colorTestMsk));
        	}
        }
        if (context.alphaTestFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(AlphaTestFilter.getAlphaTestFilter(context));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using AlphaTestFilter func=%d, ref=0x%02X", context.alphaFunc, context.alphaRef));
        	}
        }
        if (context.stencilTestFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(StencilTestFilter.getStencilTestFilter(context));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using StencilTestFilter func=%d, ref=0x%02X, mask=0x%02X", context.stencilFunc, context.stencilRef, context.stencilMask));
        	}
        }
        if (context.depthTestFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(DepthTestFilter.getDepthTestFilter(context));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using DepthTestFilter func=%d", context.depthFunc));
        	}
        }
        if (context.blendFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(AlphaBlendFilter.getAlphaBlendFilter(context));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using AlphaBlendFilter func=%d, src=%d, dst=%d, sfix=0x%06X, dfix=0x%06X", context.blendEquation, context.blend_src, context.blend_dst, context.sfix, context.dfix));
        	}
        }
        if (context.colorLogicOpFlag.isEnabled() && !context.clearMode) {
        	added = addFilter(LogicalOperationFilter.getLogicalOperationFilter(context));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using LogicalOperationFilter func=%d", context.logicOp));
        	}
        }
        {
        	added = addFilter(MaskFilter.getMaskFilter(context, context.clearMode, context.clearModeColor, context.clearModeStencil, context.clearModeDepth));
        	if (added && isLogTraceEnabled) {
        		log.trace(String.format("Using MaskFilter colorMask=0x%02X%02X%02X, depthMask=%b, clearMode=%b, clearModeColor=%b, clearModeStencil=%b, clearModeDepth=%b", context.colorMask[0], context.colorMask[1], context.colorMask[2], context.depthMask, context.clearMode, context.clearModeColor, context.clearModeStencil, context.clearModeDepth));
        	}
        }
	}

	protected void getScreenCoordinates(float[] screenCoordinates, float[] position) {
		getScreenCoordinates(screenCoordinates, position[0], position[1], position[2]);
	}

	protected void getScreenCoordinates(float[] screenCoordinates, float x, float y, float z) {
		float[] position4 = new float[4];
		position4[0] = x;
		position4[1] = y;
		position4[2] = z;
		position4[3] = 1.f;
		float[] projectedCoordinates = new float[4];
		vectorMult44(projectedCoordinates, pixel.modelViewProjectionMatrix, position4);
		float w = projectedCoordinates[3];
		float wInverted = 1.f / w;
		screenCoordinates[0] = projectedCoordinates[0] * wInverted * viewportWidth + viewportX - screenOffsetX;
		screenCoordinates[1] = projectedCoordinates[1] * wInverted * viewportHeight + viewportY - screenOffsetY;
		screenCoordinates[2] = projectedCoordinates[2] * wInverted * zscale + zpos;
		screenCoordinates[3] = w;

		if (isLogTraceEnabled) {
			log.trace(String.format("X,Y,Z = %f, %f, %f, projected X,Y,Z,W = %f, %f, %f, %f -> Screen %d, %d, %d", x, y, z, projectedCoordinates[0] / w, projectedCoordinates[1] / w, projectedCoordinates[2] / w, w, round(screenCoordinates[0]), round(screenCoordinates[1]), round(screenCoordinates[2])));
		}
	}

	@Override
	public void render() {
        imageWriter.flush();
        depthWriter.flush();

        if (captureEachPrimitive && State.captureGeNextFrame) {
        	// Capture the GE screen after each primitive
        	Modules.sceDisplayModule.captureGeImage();
        }
        if (captureZbuffer && State.captureGeNextFrame) {
        	captureZbufferImage();
        }
	}

	protected void captureZbufferImage() {
		GeContext context = VideoEngine.getInstance().getContext();
		int width = context.zbw;
		int height = Modules.sceDisplayModule.getHeightFb();
		int address = getDepthBufferAddress(context, 0, 0);
		Buffer buffer = Memory.getInstance().getBuffer(address, width * height * IRenderingEngine.sizeOfTextureType[depthBufferPixelFormat]);
		CaptureManager.captureImage(address, 0, buffer, width, height, width, depthBufferPixelFormat, false, 0, false, false);
	}
}
