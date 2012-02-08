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
	private float t1uw, t1vw;
	private float t2uw, t2vw;
	private float t3uw, t3vw;

	protected void copy(TriangleRenderer from) {
		super.copy(from);
		t1uw = from.t1uw; t1vw = from.t1vw;
		t2uw = from.t2uw; t2vw = from.t2vw;
		t3uw = from.t3uw; t3vw = from.t3vw;
	}

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

	private TriangleRenderer() {
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
	public boolean prepare(GeContext context) {
        if (isLogTraceEnabled) {
        	log.trace(String.format("TriangleRenderer"));
        }

        if (!isVisible()) {
        	return false;
        }

        setVertexTextures(context, v1, v2, v3);

        return true;
	}

	@Override
	public void render() {
		preRender();
		if (transform2D) {
			render2D();
		} else {
			render3D();
		}
        postRender();
	}

	protected void render2D() {
		if (isLogTraceEnabled) {
			log.trace(String.format("Triangle render2D (%d,%d)-(%d,%d) skip=%d", prim.pxMin, prim.pyMin, prim.pxMin + prim.destinationWidth, prim.pyMin + prim.destinationWidth, imageWriterSkipEOL));
		}
		RESoftware.triangleRender2DStatistics.start();

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
        			pixel.newPixel2D();
        			pixel.sourceDepth = round(pixel.getTriangleWeightedValue(prim.p1z, prim.p2z, prim.p3z));
            		pixel.destination = imageWriter.readCurrent();
            		pixel.destinationDepth = depthWriter.readCurrent();
            		if (compiledFilter != null) {
            			compiledFilter.filter(pixel);
            		} else {
	            		for (int i = 0; i < numberFilters; i++) {
	            			filters[i].filter(pixel);
	            			if (!pixel.filterPassed) {
	            				break;
	            			}
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
            			writerSkip(1);
            		}
        		} else {
        			// Do not display, skip the pixel
        			writerSkip(1);
        		}
        		u += prim.uStep;
        		prim.deltaXTriangleWeigths(pixel);
        	}
        	writerSkipEOL();
        	v += prim.vStep;
        }
		RESoftware.triangleRender2DStatistics.end();
	}

	protected void render3DPixel(int x) {
		pixel.newPixel();
		pixel.x = x;

		if (needTextureUV) {
			// Compute the mapped texture u,v coordinates
			// based on the Barycentric coordinates.
			// Apply a perspective correction by weighting the coordinates
			// by their "w" value.
			// See http://en.wikipedia.org/wiki/Texture_mapping#Perspective_correctness
			float u = pixel.getTriangleWeightedValue(t1uw, t2uw, t3uw);
			float v = pixel.getTriangleWeightedValue(t1vw, t2vw, t3vw);
			float weightInverted = 1.f / pixel.getTriangleWeightedValue(prim.p1wInverted, prim.p2wInverted, prim.p3wInverted);
			pixel.u = u * weightInverted;
			pixel.v = v * weightInverted;
		}
		if (needSourceDepth) {
			pixel.sourceDepth = round(pixel.getTriangleWeightedValue(prim.p1z, prim.p2z, prim.p3z));
		}
		pixel.destination = imageWriter.readCurrent();
		pixel.destinationDepth = depthWriter.readCurrent();
		if (compiledFilter != null) {
			compiledFilter.filter(pixel);
		} else {
			for (int i = 0; i < numberFilters; i++) {
				filters[i].filter(pixel);
				if (!pixel.filterPassed) {
					break;
				}
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
			writerSkip(1);
		}
	}

	protected void render3D() {
		RESoftware.triangleRender3DStatistics.start();

		t1uw = prim.t1u * prim.p1wInverted;
		t1vw = prim.t1v * prim.p1wInverted;
		t2uw = prim.t2u * prim.p2wInverted;
		t2vw = prim.t2v * prim.p2wInverted;
		t3uw = prim.t3u * prim.p3wInverted;
		t3vw = prim.t3v * prim.p3wInverted;

		if (isLogTraceEnabled) {
			log.trace(String.format("Triangle render3D (%d,%d)-(%d,%d) skip=%d", prim.pxMin, prim.pyMin, prim.pxMin + prim.destinationWidth, prim.pyMin + prim.destinationHeight, imageWriterSkipEOL));
		}

		Range range = new Range();
		Rasterizer rasterizer = null;
		// No need to use a Rasterizer when rendering very small area.
		// The overhead of the Rasterizer would lead the slower rendering.
		if (prim.destinationWidth >= Rasterizer.MINIMUM_WIDTH && prim.destinationHeight >= Rasterizer.MINIMUM_HEIGHT) {
			rasterizer = new Rasterizer(prim.p1x, prim.p1y, prim.p2x, prim.p2y, prim.p3x, prim.p3y, prim.pyMin, prim.pyMax);
			rasterizer.setY(prim.pyMin);
		}

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
        		writerSkipEOL(prim.destinationWidth);
        	} else {
        		writerSkip(startX - prim.pxMin);
        		pixel.x = startX;
        		prim.computeTriangleWeights(pixel);
	        	for (int x = startX; x <= endX; x++) {
	        		if (pixel.isInsideTriangle()) {
	        			render3DPixel(x);
	        		} else {
	        			// Do not display, skip the pixel
        				writerSkip(1);
	        		}
	        		prim.deltaXTriangleWeigths(pixel);
	        	}
	        	writerSkipEOL(prim.pxMax - endX);
        	}
        }
        RESoftware.triangleRender3DStatistics.end();
	}

	@Override
	public IRenderer duplicate() {
		TriangleRenderer triangleRenderer = new TriangleRenderer();
		triangleRenderer.copy(this);

		return triangleRenderer;
	}
}
