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

import static java.lang.Math.round;
import static java.lang.System.arraycopy;
import static jpcsp.graphics.RE.software.PixelColor.getColor;
import static jpcsp.util.Utilities.matrixMult;
import static jpcsp.util.Utilities.max;
import static jpcsp.util.Utilities.min;
import static jpcsp.util.Utilities.vectorMult;

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
import jpcsp.memory.ImageReader;

/**
 * @author gid15
 *
 */
public abstract class BaseRenderer implements IRenderer {
	protected static final Logger log = VideoEngine.log;
	protected static final boolean captureEachPrimitive = false;
	protected static final boolean captureZbuffer = false;
    public static final int depthBufferPixelFormat = GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED;
    protected static final int MAX_NUMBER_FILTERS = 11;
	protected final PixelState pixel = new PixelState();
	protected int p1x, p1y;
	protected int p2x, p2y;
	protected int p3x, p3y;
	protected int pxMin, pxMax, pyMin, pyMax;
	protected float p1z, p2z, p3z;
	protected float p1w, p2w, p3w;
	protected float p1wInverted, p2wInverted, p3wInverted;
	protected float t1u, t1v;
	protected float t2u, t2v;
	protected float t3u, t3v;
	protected int tuMin, tuMax, tvMin, tvMax;
	protected int destinationWidth;
	protected int destinationHeight;
	protected IImageWriter imageWriter;
	protected IImageWriter depthWriter;
	protected final IPixelFilter[] filters = new IPixelFilter[MAX_NUMBER_FILTERS];
	protected int numberFilters;
	protected boolean needScissoring;
	protected boolean transform2D;
	protected final float modelMatrix[] = new float[16];
	protected final float viewMatrix[] = new float[16];
	protected final float projectionMatrix[] = new float[16];
	protected final float textureMatrix[] = new float[16];
	protected final float modelViewProjectionMatrix[] = new float[16];
	protected final float revertedModelViewProjectionMatrix[] = new float[16];
	protected int viewportWidth;
	protected int viewportHeight;
	protected int viewportX;
	protected int viewportY;
	protected int screenOffsetX;
	protected int screenOffsetY;
	protected float zscale;
	protected float zpos;

	protected void setPositions(VertexState v1, VertexState v2) {
		if (transform2D) {
	        p1x = round(v1.p[0]);
	        p1y = round(v1.p[1]);
	        p1z = v1.p[2];
	        p2x = round(v2.p[0]);
	        p2y = round(v2.p[1]);
	        p2z = v2.p[2];
		} else {
			float[] screenCoordinates = new float[4];
			getScreenCoordinates(screenCoordinates, v1.p);
			p1x = round(screenCoordinates[0]);
			p1y = round(screenCoordinates[1]);
			p1z = round(screenCoordinates[2]);
			p1w = screenCoordinates[3];
			p1wInverted = 1.f / p1w;
			getScreenCoordinates(screenCoordinates, v2.p);
			p2x = round(screenCoordinates[0]);
			p2y = round(screenCoordinates[1]);
			p2z = round(screenCoordinates[2]);
			p2w = screenCoordinates[3];
			p2wInverted = 1.f / p2w;
		}

        pxMax = max(p1x, p2x);
        pxMin = min(p1x, p2x);
        pyMax = max(p1y, p2y);
        pyMin = min(p1y, p2y);
	}

	protected void setPositions(VertexState v1, VertexState v2, VertexState v3) {
		setPositions(v1, v2);

		if (transform2D) {
			p3x = round(v3.p[0]);
	        p3y = round(v3.p[1]);
	        p3z = v3.p[2];
		} else {
			float[] screenCoordinates = new float[4];
			getScreenCoordinates(screenCoordinates, v3.p);
			p3x = round(screenCoordinates[0]);
			p3y = round(screenCoordinates[1]);
			p3z = round(screenCoordinates[2]);
			p3w = screenCoordinates[3];
			p3wInverted = 1.f / p3w;
		}

        pxMax = max(pxMax, p3x);
        pxMin = min(pxMin, p3x);
        pyMax = max(pyMax, p3y);
        pyMin = min(pyMin, p3y);
	}

	protected void setTextures(VertexState v1, VertexState v2) {
        t1u = v1.t[0];
        t1v = v1.t[1];
        t2u = v2.t[0];
        t2v = v2.t[1];

        tuMax = max(round(t1u), round(t2u));
        tuMin = min(round(t1u), round(t2u));
        tvMax = max(round(t1v), round(t2v));
        tvMin = min(round(t1v), round(t2v));
	}

	protected void setTextures(VertexState v1, VertexState v2, VertexState v3) {
		setTextures(v1, v2);

		t3u = v3.t[0];
        t3v = v3.t[1];

        tuMax = max(tuMax, round(t3u));
        tuMin = min(tuMin, round(t3u));
        tvMax = max(tvMax, round(t3v));
        tvMin = min(tvMin, round(t3v));
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

	protected void init(GeContext context) {
		numberFilters = 0;
		transform2D = context.vinfo.transform2D;
		if (!transform2D) {
			// Initialization for 3D
			arraycopy(context.model_uploaded_matrix, 0, modelMatrix, 0, modelMatrix.length);
			arraycopy(context.view_uploaded_matrix, 0, viewMatrix, 0, viewMatrix.length);
			arraycopy(context.proj_uploaded_matrix, 0, projectionMatrix, 0, projectionMatrix.length);
			arraycopy(context.texture_uploaded_matrix, 0, textureMatrix, 0, textureMatrix.length);

			// Pre-compute the Model-View-Projection matrix
			matrixMult(modelViewProjectionMatrix, projectionMatrix, viewMatrix);
			matrixMult(modelViewProjectionMatrix, modelViewProjectionMatrix, modelMatrix);

			// Compute the reverted Model-View-Projection matrix: this is just a transposition
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < 4; j++) {
					revertedModelViewProjectionMatrix[i * 4 + j] = modelViewProjectionMatrix[j * 4 + i];
				}
			}

			viewportWidth = context.viewport_width;
			viewportHeight = context.viewport_height;
			viewportX = context.viewport_cx;
			viewportY = context.viewport_cy;
			screenOffsetX = context.offset_x;
			screenOffsetY = context.offset_y;
			zscale = context.zscale * 65535.f;
			zpos = context.zpos * 65535.f;
		}
	}

	protected void prepareTextureReader(GeContext context, CachedTexture texture, VertexState v1, VertexState v2) {
		prepareTextureReader(context, texture, v1, v2, null);
	}

	protected void prepareTextureReader(GeContext context, CachedTexture texture, VertexState v1, VertexState v2, VertexState v3) {
		if (context.useVertexColor && context.vinfo.color != 0) {
			pixel.primaryColor = getColor(v1.c);
		} else {
			pixel.primaryColor = getColor(context.vertexColor);
		}

		pxMin = Math.max(0, pxMin);
		pxMax = Math.min(pxMax, context.fbw);
		pyMin = Math.max(0, pyMin);
		pyMax = Math.min(pyMax, 1023);

		destinationWidth = pxMax - pxMin;
        destinationHeight = pyMax - pyMin;

        IPixelFilter textureFilter;
    	if (context.vinfo.texture != 0 && context.textureFlag.isEnabled()) {
    		final int level = 0;
    		int textureBufferWidth = VideoEngine.alignBufferWidth(context.texture_buffer_width[level], context.texture_storage);
    		int textureHeight = context.texture_height[level];
            int sourceWidth = Math.min(textureBufferWidth, tuMax - tuMin);
            int sourceHeight = tvMax - tvMin;
    		int cropLeft = tuMin;
    		int cropRight = textureBufferWidth - sourceWidth - cropLeft;
    		int cropTop = tvMin;
    		int cropBottom = textureHeight - sourceHeight - cropTop;
            int textureAddress = context.texture_base_pointer[level];
            boolean flipX = false;
            boolean flipY = false;
            boolean rotate = false;
            if (v3 == null) {
            	flipX = (t1u > t2u) ^ (p1x > p2x);
            	flipY = (t1v > t2v) ^ (p1y > p2y);
            }
            if (flipX && flipY) {
            	rotate = true;
            	flipX = false;
            	flipY = false;
            }
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

            // Flip and rotate the image
        	textureAccess = TextureFlip.getImageFlip(textureAccess, textureBufferWidth, textureHeight, flipX, flipY, rotate);
            if (transform2D) {
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

            	textureAccess = TextureCrop.getImageCrop(textureAccess, sourceWidth, cropLeft, cropRight, cropTop);
            	textureAccess = TextureResizer.getImageResizer(textureAccess, sourceWidth, sourceHeight, destinationWidth, destinationHeight);
            }

            if (!transform2D) {
            	// Perform the texture mapping (UV / texture matrix / environment map)
            	textureFilter = TextureMapping.getTextureMapping(context);
            	addFilter(textureFilter);
            }

            // Read the corresponding texture texel
            textureFilter = TextureReader.getTextureReader(textureAccess, context);
            addFilter(textureFilter);

            // Apply the texture function (modulate, decal, blend, replace, add)
            textureFilter = TextureFunction.getTextureFunction(context);
            addFilter(textureFilter);
    	} else if (context.useVertexColor) {
    		// Read the color from the vertex
    		textureFilter = VertexColorFilter.getVertexColorFilter(v1, v2, v3);
        	addFilter(textureFilter);
		} else {
			if (log.isTraceEnabled()) {
				log.trace(String.format("Using ColorTextureFilter vertexColor=0x%08X", getColor(context.vertexColor)));
			}
			// Read the color from the context vertexColor
			textureFilter = new ColorTextureFilter(context.vertexColor);
	    	addFilter(textureFilter);
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

	protected void getScreenCoordinates(float[] screenCoordinates, float[] position) {
		float[] position4 = new float[4];
		position4[0] = position[0];
		position4[1] = position[1];
		position4[2] = position[2];
		position4[3] = 1.f;
		float[] projectedCoordinates = new float[4];
		vectorMult(projectedCoordinates, modelViewProjectionMatrix, position4);
		float w = projectedCoordinates[3];
		screenCoordinates[0] = projectedCoordinates[0] * viewportWidth / w + viewportX - screenOffsetX;
		screenCoordinates[1] = projectedCoordinates[1] * viewportHeight / w + viewportY - screenOffsetY;
		screenCoordinates[2] = projectedCoordinates[2] * zscale / w + zpos;
		screenCoordinates[3] = w;

		if (log.isTraceEnabled()) {
			log.trace(String.format("X,Y,Z = %f, %f, %f -> Screen %d, %d, %d", projectedCoordinates[0] / w, projectedCoordinates[1] / w, projectedCoordinates[2] / w, screenCoordinates[0], screenCoordinates[1], screenCoordinates[2]));
		}
	}

	protected void getPosition(float[] modelCoordinates, int screenX, int screenY, int screenZ) {
		float[] temp = new float[4];
		temp[0] = screenX;
		temp[1] = screenY;
		temp[2] = screenZ;
		temp[3] = 1.f;
		vectorMult(modelCoordinates, revertedModelViewProjectionMatrix, temp);
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
