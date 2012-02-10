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

import static jpcsp.graphics.RE.software.IPixelFilter.DISCARDS_SOURCE_DEPTH;
import static jpcsp.graphics.RE.software.IPixelFilter.REQUIRES_SOURCE_DEPTH;
import static jpcsp.graphics.RE.software.IPixelFilter.REQUIRES_TEXTURE_U_V;
import static jpcsp.graphics.RE.software.PixelColor.getColor;
import static jpcsp.util.Utilities.invertMatrix3x3;
import static jpcsp.util.Utilities.matrixMult;
import static jpcsp.util.Utilities.max;
import static jpcsp.util.Utilities.maxInt;
import static jpcsp.util.Utilities.min;
import static jpcsp.util.Utilities.minInt;
import static jpcsp.util.Utilities.round;
import static jpcsp.util.Utilities.transposeMatrix3x3;
import static jpcsp.util.Utilities.vectorMult44;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VertexState;

/**
 * @author gid15
 *
 * This class extends the BaseRenderer class to include
 * vertex specific information.
 * The methods from this class can be used to set the vertex
 * information specific for the rendering of one primitive (e.g. one triangle)
 */
public abstract class BasePrimitiveRenderer extends BaseRenderer {
	protected final PixelState pixel = new PixelState();
	protected PrimitiveState prim = new PrimitiveState();
	protected boolean needScissoringX;
	protected boolean needScissoringY;
	protected boolean needSourceDepth;
	protected boolean needTextureUV;

	protected void copy(BasePrimitiveRenderer from) {
		super.copy(from);
		pixel.copy(from.pixel);
		prim.copy(from.prim);
		needScissoringX = from.needScissoringX;
		needScissoringY = from.needScissoringY;
		needSourceDepth = from.needSourceDepth;
		needTextureUV = from.needTextureUV;
	}

	@Override
	protected void init(GeContext context, CachedTexture texture, boolean useVertexTexture) {
		super.init(context, texture, useVertexTexture);

		pixel.init();

		prim.pxMax = Integer.MIN_VALUE;
		prim.pxMin = Integer.MAX_VALUE;
		prim.pyMax = Integer.MIN_VALUE;
		prim.pyMin = Integer.MAX_VALUE;
		prim.pzMax = Integer.MIN_VALUE;
		prim.pzMin = Integer.MAX_VALUE;

		if (!transform2D) {
			// Copy the View matrix
			System.arraycopy(context.view_uploaded_matrix, 0, pixel.viewMatrix, 0, pixel.viewMatrix.length);

			// Pre-compute the Model-View matrix
			matrixMult(pixel.modelViewMatrix, context.view_uploaded_matrix, context.model_uploaded_matrix);

			// Pre-compute the Model-View-Projection matrix
			matrixMult(pixel.modelViewProjectionMatrix, context.proj_uploaded_matrix, pixel.modelViewMatrix);
		}
	}

	@Override
	protected void initRendering(GeContext context, boolean isTriangle) {
		if (renderingInitialized) {
			return;
		}

		super.initRendering(context, isTriangle);

		pixel.primaryColor = getColor(context.vertexColor);
		if (context.textureColorDoubled) {
			pixel.primaryColor = ColorDoubling.doubleColor(pixel.primaryColor);
		}
		pixel.materialAmbient = getColor(context.mat_ambient);
		pixel.materialDiffuse = getColor(context.mat_diffuse);
		pixel.materialSpecular = getColor(context.mat_specular);

		if (!transform2D) {
			// Pre-compute the matrix to transform a normal to the eye coordinates
			// See http://www.lighthouse3d.com/tutorials/glsl-tutorial/the-normal-matrix/
			float[] invertedModelViewMatrix = new float[16];
			if (invertMatrix3x3(invertedModelViewMatrix, pixel.modelViewMatrix)) {
				transposeMatrix3x3(pixel.normalMatrix, invertedModelViewMatrix);
			} else {
				// What is using the PSP in this case? Assume it just takes the Model-View matrix
				System.arraycopy(pixel.modelViewMatrix, 0, pixel.normalMatrix, 0, pixel.normalMatrix.length);
				if (isLogDebugEnabled) {
					log.debug(String.format("ModelView matrix cannot be inverted, taking the Model-View matrix itself!"));
				}
			}

			pixel.hasNormal = context.vinfo.normal != 0;
		}
	}

	protected void addPosition(float[] p) {
		float[] screenCoordinates = new float[4];
		getScreenCoordinates(screenCoordinates, p);
		prim.pxMax = maxInt(prim.pxMax, screenCoordinates[0]);
		prim.pxMin = minInt(prim.pxMin, screenCoordinates[0]);
		prim.pyMax = maxInt(prim.pyMax, screenCoordinates[1]);
		prim.pyMin = minInt(prim.pyMin, screenCoordinates[1]);
		prim.pzMax = maxInt(prim.pzMax, screenCoordinates[2]);
		prim.pzMin = minInt(prim.pzMin, screenCoordinates[2]);
	}

	protected void setVertexPositions(VertexState v1, VertexState v2, VertexState v3) {
		setPositions(v1, v2, v3);
	}

	protected void setVertexPositions(VertexState v1, VertexState v2) {
		setPositions(v1, v2);
	}

	protected void setVertexTextures(GeContext context, VertexState v1, VertexState v2, VertexState v3) {
		setTextures(v1, v2, v3);
		setVertexTextures(context, v1.c, v2.c, v3.c);
	}

	protected void setVertexTextures(GeContext context, VertexState v1, VertexState v2) {
		setTextures(v1, v2);
		setVertexTextures(context, v1.c, v2.c, null);
	}

	private void setVertexTextures(GeContext context, float[] c1, float[] c2, float[] c3) {
		// The rendering will be performed into the following ranges:
		// 3D:
		//   - x: [pxMin..pxMax] (min and max included)
		//   - y: [pxMin..pxMax] (min and max included)
		// 2D:
		//   - x: [pxMin..pxMax-1] (min included but max excluded)
		//   - y: [pxMin..pxMax-1] (min included but max excluded)
        if (transform2D) {
        	prim.pxMax--;
        	prim.pyMax--;
        } else {
        	// Restrict the drawn area to the scissor area.
        	// We can just update the min/max values, the TextureMapping filter
        	// will take are of the correct texture mapping.
        	// We do no longer need a scissoring filter.
        	if (needScissoringX) {
        		prim.pxMin = max(prim.pxMin, scissorX1);
        		prim.pxMax = min(prim.pxMax, scissorX2);
        		needScissoringX = false;
        	}
        	if (needScissoringY) {
        		prim.pyMin = max(prim.pyMin, scissorY1);
        		prim.pyMax = min(prim.pyMax, scissorY2);
        		needScissoringY = false;
        	}
        }
        prim.destinationWidth = prim.pxMax - prim.pxMin + 1;
        prim.destinationHeight = prim.pyMax - prim.pyMin + 1;

        if (transform2D) {
	    	boolean flipX = false;
	    	boolean flipY = false;
	    	if (c3 == null) {
	    		// Compute texture flips for a sprite
	    		flipX = (prim.t1u > prim.t2u) ^ (prim.p1x > prim.p2x);
	    		flipY = (prim.t1v > prim.t2v) ^ (prim.p1y > prim.p2y);
	    	} else {
	    		// Compute texture flips for a triangle
	    		flipX = (prim.t1u > prim.t2u) ^ (prim.p1x > prim.p2x);
	    		flipY = (prim.t1v > prim.t2v) ^ (prim.p1y > prim.p2y);
	    		if (!flipX) {
		    		flipX = (prim.t2u > prim.t3u) ^ (prim.p2x > prim.p3x);
	    		}
	    		if (!flipY) {
		    		flipY = (prim.t2v > prim.t3v) ^ (prim.p2y > prim.p3y);
	    		}
	    	}
	    	if (isLogTraceEnabled) {
	    		log.trace(String.format("2D texture flipX=%b, flipY=%b, point (%d,%d)-(%d,%d), texture (%d,%d)-(%d,%d)", flipX, flipY, prim.pxMin, prim.pyMin, prim.pxMax, prim.pyMax, prim.tuMin, prim.tvMin, prim.tuMax, prim.tvMax));
	    	}
	    	prim.uStart = flipX ? prim.tuMax : prim.tuMin;
	    	float uEnd = flipX ? prim.tuMin : prim.tuMax;
	    	prim.vStart = flipY ? prim.tvMax : prim.tvMin;
	    	float vEnd = flipY ? prim.tvMin : prim.tvMax;
	    	prim.uStep = (uEnd - prim.uStart) / prim.destinationWidth;
	    	prim.vStep = (vEnd - prim.vStart) / prim.destinationHeight;
        } else if (c3 == null) {
        	prim.uStart = prim.t1u;
        	float uEnd = prim.t2u;
        	prim.vStart = prim.t1v;
        	float vEnd = prim.t2v;
	    	prim.uStep = (uEnd - prim.uStart) / (prim.destinationWidth - 1);
	    	prim.vStep = (vEnd - prim.vStart) / (prim.destinationHeight - 1);
        }

    	if (setVertexPrimaryColor) {
    		if (c3 != null) {
    			pixel.c1 = getColor(c1);
	    		pixel.c1a = getColor(c1[3]);
	    		pixel.c1b = getColor(c1[2]);
	    		pixel.c1g = getColor(c1[1]);
	    		pixel.c1r = getColor(c1[0]);
	    		pixel.c2a = getColor(c2[3]);
	    		pixel.c2b = getColor(c2[2]);
	    		pixel.c2g = getColor(c2[1]);
	    		pixel.c2r = getColor(c2[0]);
	    		pixel.c3a = getColor(c3[3]);
	    		pixel.c3b = getColor(c3[2]);
	    		pixel.c3g = getColor(c3[1]);
	    		pixel.c3r = getColor(c3[0]);
    		}
        	if (primaryColorFilter >= 0) {
    			// For triangles, take the weighted color from the 3 vertices.
        		filters[primaryColorFilter] = VertexColorFilter.getVertexColorFilter(c1, c2, c3);
        	} else {
    			// For sprites, take only the color from the 2nd vertex
    			pixel.primaryColor = getColor(c2);
    			if (context.textureColorDoubled) {
    				pixel.primaryColor = ColorDoubling.doubleColor(pixel.primaryColor);
    			}
        	}
        }

    	if (scissorFilter >= 0) {
    		filters[scissorFilter] = ScissorFilter.getScissorFilter(scissorX1, scissorY1, scissorX2, scissorY2, needScissoringX, needScissoringY);
    		if (log.isTraceEnabled()) {
    			log.trace(String.format("Using ScissorFilter (%d,%d)-(%d,%d)", scissorX1, scissorY1, scissorX2, scissorY2));
    		}
    	}

    	prepareWriters();

    	int filtersId = getFiltersId();
    	if (filtersId != compiledFilterId || compiledFilter == null) {
			compiledFilterId = filtersId;
    		compiledFilter = FilterCompiler.getCompiledFilter(this, filtersId, context);
    	}

    	if (c3 != null) {
        	prim.preComputeTriangleWeights();
        }
	}

	private void setPositions(VertexState v1, VertexState v2) {
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
        	prim.p1x = pixel.v1x;
        	prim.p1y = pixel.v1y;
        	prim.p1z = pixel.v1z;
        	prim.p2x = pixel.v2x;
        	prim.p2y = pixel.v2y;
        	prim.p2z = pixel.v2z;
		} else {
			float[] screenCoordinates = new float[4];
			getScreenCoordinates(screenCoordinates, pixel.v1x, pixel.v1y, pixel.v1z);
			prim.p1x = screenCoordinates[0];
			prim.p1y = screenCoordinates[1];
			prim.p1z = screenCoordinates[2];
			prim.p1w = screenCoordinates[3];
			prim.p1wInverted = 1.f / prim.p1w;
			getScreenCoordinates(screenCoordinates, pixel.v2x, pixel.v2y, pixel.v2z);
			prim.p2x = screenCoordinates[0];
			prim.p2y = screenCoordinates[1];
			prim.p2z = screenCoordinates[2];
			prim.p2w = screenCoordinates[3];
			prim.p2wInverted = 1.f / prim.p2w;
		}

        prim.pxMax = maxInt(prim.p1x, prim.p2x);
        prim.pxMin = minInt(prim.p1x, prim.p2x);
        prim.pyMax = maxInt(prim.p1y, prim.p2y);
        prim.pyMin = minInt(prim.p1y, prim.p2y);
        prim.pzMax = maxInt(prim.p1z, prim.p2z);
        prim.pzMin = minInt(prim.p1z, prim.p2z);
	}

	private void setPositions(VertexState v1, VertexState v2, VertexState v3) {
		setPositions(v1, v2);

		pixel.v3x = v3.p[0];
		pixel.v3y = v3.p[1];
		pixel.v3z = v3.p[2];
		pixel.n3x = v3.n[0];
		pixel.n3y = v3.n[1];
		pixel.n3z = v3.n[2];

        if (transform2D) {
        	prim.p3x = pixel.v3x;
        	prim.p3y = pixel.v3y;
        	prim.p3z = pixel.v3z;
		} else {
			float[] screenCoordinates = new float[4];
			getScreenCoordinates(screenCoordinates, pixel.v3x, pixel.v3y, pixel.v3z);
			prim.p3x = screenCoordinates[0];
			prim.p3y = screenCoordinates[1];
			prim.p3z = screenCoordinates[2];
			prim.p3w = screenCoordinates[3];
			prim.p3wInverted = 1.f / prim.p3w;
		}

        prim.pxMax = maxInt(prim.pxMax, prim.p3x);
        prim.pxMin = minInt(prim.pxMin, prim.p3x);
        prim.pyMax = maxInt(prim.pyMax, prim.p3y);
        prim.pyMin = minInt(prim.pyMin, prim.p3y);
        prim.pzMax = maxInt(prim.pzMax, prim.p3z);
        prim.pzMin = minInt(prim.pzMin, prim.p3z);
	}

	private void setTextures(VertexState v1, VertexState v2) {
		prim.t1u = v1.t[0];
		prim.t1v = v1.t[1];
		prim.t2u = v2.t[0];
		prim.t2v = v2.t[1];

        if (transform2D) {
        	prim.tuMax = max(round(prim.t1u), round(prim.t2u));
        	prim.tuMin = min(round(prim.t1u), round(prim.t2u));
        	prim.tvMax = max(round(prim.t1v), round(prim.t2v));
        	prim.tvMin = min(round(prim.t1v), round(prim.t2v));
        }
	}

	private void setTextures(VertexState v1, VertexState v2, VertexState v3) {
		setTextures(v1, v2);

		prim.t3u = v3.t[0];
		prim.t3v = v3.t[1];

        if (transform2D) {
        	prim.tuMax = max(prim.tuMax, round(prim.t3u));
        	prim.tuMin = min(prim.tuMin, round(prim.t3u));
        	prim.tvMax = max(prim.tvMax, round(prim.t3v));
        	prim.tvMin = min(prim.tvMin, round(prim.t3v));
        }
	}

	private void prepareWriters() {
        int fbAddress = getTextureAddress(fbp, prim.pxMin, prim.pyMin, fbw, psm);
    	int depthAddress = getTextureAddress(zbp, prim.pxMin, prim.pyMin, zbw, depthBufferPixelFormat);
    	// Request image writers with a width covering the complete buffer width,
    	// we will skip unnecessary pixels at the end of each line manually (more efficient).
        imageWriter = ImageWriter.getImageWriter(fbAddress, fbw, fbw, psm);
        depthWriter = ImageWriter.getImageWriter(depthAddress, zbw, zbw, depthBufferPixelFormat);
        imageWriterSkipEOL = fbw - prim.destinationWidth;
        depthWriterSkipEOL = zbw - prim.destinationWidth;
	}

	protected boolean isVisible() {
    	if (!transform2D) {
    		// Each vertex screen coordinates (without offset) has to be in the range:
    		// - x: [0..4095]
    		// - y: [0..4095]
    		// - z: [..65535]
    		// If one of the vertex coordinate is not in the valid range, the whole
    		// primitive is discarded.
        	if ((prim.pxMin + screenOffsetX) < 0 ||
        	    (prim.pxMax + screenOffsetX) >= 4096 ||
        	    (prim.pyMin + screenOffsetY) < 0 ||
        	    (prim.pyMax + screenOffsetY) >= 4096 ||
        	    prim.pzMax >= 65536) {
        		return false;
        	}

        	// This is probably a rounding error when one triangle
        	// extends from back to front over a very large distance
        	// (more than the allowed range for Z values).
        	if (prim.pzMin < 0 && prim.pzMax > 0 && prim.pzMax - prim.pzMin > 65536) {
        		return false;
        	}

        	if (!clipPlanesEnabled) {
        		// The primitive is discarded when one of the vertex is behind the viewpoint
        		// (only the the ClipPlanes flag is not enabled).
        		if (prim.pzMin < 0) {
        			return false;
        		}
        	} else {
            	// TODO Implement proper triangle clipping against the near plane
            	if (prim.p1w < 0f || prim.p2w < 0f || prim.p3w < 0f) {
            		return false;
            	}
        	}
    	}

		if (!useVertexTexture) {
			prim.pxMin = Math.max(prim.pxMin, scissorX1);
			prim.pxMax = Math.min(prim.pxMax, scissorX2 + 1);
			prim.pyMin = Math.max(prim.pyMin, scissorY1);
			prim.pyMax = Math.min(prim.pyMax, scissorY2 + 1);
		}

		prim.pxMin = Math.max(0, prim.pxMin);
		prim.pxMax = Math.min(prim.pxMax, fbw);
		prim.pyMin = Math.max(0, prim.pyMin);
		prim.pyMax = Math.min(prim.pyMax, 1024);

		if (prim.pxMin == prim.pxMax || prim.pyMin == prim.pyMax) {
			// Empty area to be displayed
			return false;
		}

		if (!insideScissor()) {
			return false;
		}

        return true;
	}

	protected boolean insideScissor() {
        needScissoringX = false;
        needScissoringY = false;

        // Scissoring (also applied in clear mode)
    	if (prim.pxMax < scissorX1 || prim.pxMin > scissorX2) {
    		// Completely outside the scissor area, skip
    		return false;
    	}
    	if (prim.pyMax < scissorY1 || prim.pyMin > scissorY2) {
    		// Completely outside the scissor area, skip
    		return false;
    	}
    	if (!transform2D) {
        	if (prim.pzMax < nearZ || prim.pzMin > farZ) {
        		// Completely outside the view area, skip
        		return false;
        	}
    	}

    	if (prim.pxMin < scissorX1 || prim.pxMax > scissorX2) {
    		// partially outside the scissor area, use the scissoring filter
    		needScissoringX = true;
    	}
    	if (prim.pyMin < scissorY1 || prim.pyMax > scissorY2) {
    		// partially outside the scissor area, use the scissoring filter
    		needScissoringY = true;
    	}

        return true;
	}

	private void getScreenCoordinates(float[] screenCoordinates, float[] position) {
		getScreenCoordinates(screenCoordinates, position[0], position[1], position[2]);
	}

	private void getScreenCoordinates(float[] screenCoordinates, float x, float y, float z) {
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
	protected void postRender() {
		super.postRender();

		statisticsFilters(pixel.getNumberPixels());
	}

	@Override
	protected void preRender() {
		super.preRender();

		int flags = getFiltersFlags();

		// Try to avoid to compute expensive values
		needSourceDepth = hasFlag(flags, REQUIRES_SOURCE_DEPTH) || !hasFlag(flags, DISCARDS_SOURCE_DEPTH);
		needTextureUV = hasFlag(flags, REQUIRES_TEXTURE_U_V);
	}
}
