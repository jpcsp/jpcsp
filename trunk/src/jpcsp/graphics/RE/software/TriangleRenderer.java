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

import static jpcsp.util.Utilities.max;
import static jpcsp.util.Utilities.min;
import static jpcsp.util.Utilities.round;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VertexState;
import jpcsp.graphics.RE.software.Rasterizer.Range;

/**
 * @author gid15
 *
 */
public class TriangleRenderer extends BasePrimitiveRenderer {
	protected boolean initialized;
	private VertexState v1;
	private VertexState v2;
	private VertexState v3;

	/**
	 * Create a triangle renderer using the current settings from the
	 * GE context and a cached texture.
	 * 
	 * The GE context values used by the rendering will be copied
	 * from the GE context during this call. Later updates of the
	 * GE context values will not be considered.
	 *
	 * This triangle renderer can be re-used for rendering multiple
	 * triangles (i.e. multiple vertex-triples) sharing all the same
	 * settings from the GE context.
	 *
	 * @param context    the current GE context
	 * @param texture    the texture to be used (or null if no texture used)
	 */
	public TriangleRenderer(GeContext context, CachedTexture texture, boolean useVertexTexture) {
		init(context, texture, useVertexTexture, true);
	}

	/**
	 * This method has to be called when using this triangle renderer
	 * for a new set of vertices.
	 * The vertices will be rendered using the GE context values defined
	 * when creating the triangle renderer.
	 *
	 * @param v1	first vertex of the triangle
	 * @param v2	second vertex of the triangle
	 * @param v3	third vertex of the triangle
	 */
	public void setVertex(VertexState v1, VertexState v2, VertexState v3) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
		setVertexPositions(v1, v2, v3);
	}

	public boolean isCulled(boolean invertedFrontFace) {
		// Back face culling enabled?
		// It is disabled in clear mode and 2D
        if (!clearMode && !transform2D && cullFaceEnabled) {
        	if (frontFaceCw) {
        		// The visible face is clockwise
        		if (!prim.isClockwise() ^ invertedFrontFace) {
        			if (log.isTraceEnabled()) {
        				log.trace("Counterclockwise triangle not displayed");
        			}
        			return true;
        		}
        	} else {
        		// The visible face is counterclockwise
        		if (prim.isClockwise() ^ invertedFrontFace) {
        			if (log.isTraceEnabled()) {
        				log.trace("Clockwise triangle not displayed");
        			}
        			return true;
        		}
        	}
        }

        return false;
	}

	@Override
	public boolean prepare() {
        if (log.isTraceEnabled()) {
        	log.trace(String.format("TriangleRenderer"));
        }

        if (!isVisible()) {
        	return false;
        }

        setVertexTextures(v1, v2, v3);

		return true;
	}

	@Override
	public void render() {
		if (transform2D) {
			render2D();
		} else {
			render3D();
		}
        super.render();
	}

	protected void render2D() {
		if (isLogTraceEnabled) {
			log.trace(String.format("Triangle render2D (%d,%d)-(%d,%d) skip=%d", prim.pxMin, prim.pyMin, prim.pxMin + prim.destinationWidth, prim.pyMin + prim.destinationWidth, imageWriterSkipEOL));
		}
		float v = prim.vStart;
        for (int y = 0; y < prim.destinationHeight; y++) {
        	pixel.y = prim.pyMin + y;
    		pixel.v = v;
    		float u = prim.uStart;
    		pixel.x = prim.pxMin;
    		prim.computeTriangleWeights(pixel);
        	for (int x = 0; x < prim.destinationWidth; x++) {
        		pixel.x = prim.pxMin + x;
        		pixel.u = u;
        		if (pixel.isInsideTriangle()) {
        			pixel.newPixel();
        			pixel.sourceDepth = round(pixel.getTriangleWeightedValue(prim.p1z, prim.p2z, prim.p3z));
            		pixel.destination = imageWriter.readCurrent();
            		pixel.destinationDepth = depthWriter.readCurrent();
            		for (int i = 0; i < numberFilters; i++) {
            			filters[i].filter(pixel);
            			if (!pixel.filterPassed) {
            				break;
            			}
            		}
if (isLogTraceEnabled) {
	log.trace(String.format("Pixel (%d,%d), passed=%b, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d, filterOnFailed=%s", pixel.x, pixel.y, pixel.filterPassed, pixel.u, pixel.v, pixel.source, pixel.destination, pixel.primaryColor, pixel.secondaryColor, pixel.sourceDepth, pixel.destinationDepth, pixel.filterOnFailed));
}
            		if (pixel.filterPassed) {
	            		imageWriter.writeNext(pixel.source);
	            		depthWriter.writeNext(pixel.sourceDepth);
        			} else if (pixel.filterOnFailed != null) {
        				// Filter did not pass, but we have a filter to be executed in that case
        				pixel.source = pixel.destination;
        				pixel.filterOnFailed.filter(pixel);
        				imageWriter.writeNext(pixel.source);
        				depthWriter.skip(1);
            		} else {
            			// Filter did not pass, do not update the pixel
            			imageWriter.skip(1);
            			depthWriter.skip(1);
            		}
        		} else {
        			// Do not display, skip the pixel
        			imageWriter.skip(1);
        			depthWriter.skip(1);
        		}
        		u += prim.uStep;
        		prim.deltaXTriangleWeigths(pixel);
        	}
        	imageWriter.skip(imageWriterSkipEOL);
        	depthWriter.skip(depthWriterSkipEOL);
        	v += prim.vStep;
        }
	}

	protected void render3D() {
		RESoftware.triangleRender3DStatistics.start();

		Range range = new Range();
		Rasterizer rasterizer = null;
		// No need to use a Rasterizer when rendering very small area.
		// The overhead of the Rasterizer would lead the slower rendering.
		if (prim.destinationWidth >= Rasterizer.MINIMUM_WIDTH && prim.destinationHeight >= Rasterizer.MINIMUM_HEIGHT) {
			rasterizer = new Rasterizer(prim.p1x, prim.p1y, prim.p2x, prim.p2y, prim.p3x, prim.p3y, prim.pyMin, prim.pyMax);
			rasterizer.setY(prim.pyMin);
		}

		int numberPixels = 0;
        for (int y = prim.pyMin; y <= prim.pyMax; y++) {
        	pixel.y = y;
        	int startX = prim.pxMin;
        	int endX = prim.pxMax;
        	if (rasterizer != null) {
            	rasterizer.getNextRange(range);
            	startX = max(range.xMin, startX);
            	endX = min(range.xMax, endX);
        	}

        	if (startX >= endX) {
        		imageWriter.skip(prim.destinationWidth + imageWriterSkipEOL);
        		depthWriter.skip(prim.destinationWidth + depthWriterSkipEOL);
        	} else {
        		numberPixels += endX - startX + 1;
        		imageWriter.skip(startX - prim.pxMin);
        		depthWriter.skip(startX - prim.pxMin);
        		pixel.x = startX;
        		prim.computeTriangleWeights(pixel);
	        	for (int x = startX; x <= endX; x++) {
	        		if (pixel.isInsideTriangle()) {
	        			pixel.newPixel();
		        		pixel.x = x;
	        			// Compute the mapped texture u,v coordinates
	        			// based on the Barycentric coordinates.
	        			// Apply a perspective correction by weighting the coordinates
	        			// by their "w" value.
	        			// See http://en.wikipedia.org/wiki/Texture_mapping#Perspective_correctness
	        			float u = pixel.getTriangleWeightedValue(prim.t1u * prim.p1wInverted, prim.t2u * prim.p2wInverted, prim.t3u * prim.p3wInverted);
	        			float v = pixel.getTriangleWeightedValue(prim.t1v * prim.p1wInverted, prim.t2v * prim.p2wInverted, prim.t3v * prim.p3wInverted);
	        			float weightInverted = 1.f / pixel.getTriangleWeightedValue(prim.p1wInverted, prim.p2wInverted, prim.p3wInverted);
	        			pixel.u = u * weightInverted;
	        			pixel.v = v * weightInverted;
	        			pixel.sourceDepth = round(pixel.getTriangleWeightedValue(prim.p1z, prim.p2z, prim.p3z));
	        			pixel.destination = imageWriter.readCurrent();
	        			pixel.destinationDepth = depthWriter.readCurrent();
	        			for (int i = 0; i < numberFilters; i++) {
	        				filters[i].filter(pixel);
	        				if (!pixel.filterPassed) {
	        					break;
	        				}
	        			}
if (isLogTraceEnabled) {
	log.trace(String.format("Pixel (%d,%d), passed=%b, tex (%f, %f), source=0x%08X, dest=0x%08X, prim=0x%08X, sec=0x%08X, sourceDepth=%d, destDepth=%d, filterOnFailed=%s", pixel.x, pixel.y, pixel.filterPassed, pixel.u, pixel.v, pixel.source, pixel.destination, pixel.primaryColor, pixel.secondaryColor, pixel.sourceDepth, pixel.destinationDepth, pixel.filterOnFailed));
}
	        			if (pixel.filterPassed) {
	        	    		imageWriter.writeNext(pixel.source);
	        	    		depthWriter.writeNext(pixel.sourceDepth);
	        			} else if (pixel.filterOnFailed != null) {
	        				// Filter did not pass, but we have a filter to be executed in that case
	        				pixel.source = pixel.destination;
	        				pixel.filterOnFailed.filter(pixel);
	        				imageWriter.writeNext(pixel.source);
	        				depthWriter.skip(1);
	        			} else {
	        				// Filter did not pass, do not update the pixel
	        				imageWriter.skip(1);
	        				depthWriter.skip(1);
	        			}
	        		} else {
	        			// Do not display, skip the pixel
	        			imageWriter.skip(1);
	        			depthWriter.skip(1);
	        		}
	        		prim.deltaXTriangleWeigths(pixel);
	        	}
	    		imageWriter.skip((prim.pxMax - endX) + imageWriterSkipEOL);
	    		depthWriter.skip((prim.pxMax - endX) + depthWriterSkipEOL);
        	}
        }
        if (numberPixels > 10000 && isLogDebugEnabled) {
        	log.debug(String.format("render3D: %d pixels, (%d,%d)-(%d,%d), duration %dms", numberPixels, prim.pxMin, prim.pyMin, prim.pxMax, prim.pyMax, RESoftware.triangleRender3DStatistics.getDurationMillis()));
        }
        RESoftware.triangleRender3DStatistics.end();
	}
}
