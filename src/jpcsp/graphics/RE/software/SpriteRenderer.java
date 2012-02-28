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

import jpcsp.graphics.GeContext;
import jpcsp.graphics.VertexState;

/**
 * @author gid15
 *
 */
public class SpriteRenderer extends BasePrimitiveRenderer {
	protected VertexState v1;
	protected VertexState v2;
	protected int sourceDepth;

	protected void copy(SpriteRenderer from) {
		super.copy(from);
		sourceDepth = from.sourceDepth;
	}

	private SpriteRenderer() {
	}

	public SpriteRenderer(GeContext context, CachedTexture texture, boolean useVertexTexture) {
		init(context, texture, useVertexTexture, false);
	}

	public void setVertex(VertexState v1, VertexState v2) {
		this.v1 = v1;
		this.v2 = v2;
		setVertexPositions(v1, v2);
	}

	@Override
	public boolean prepare(GeContext context) {
        if (log.isTraceEnabled()) {
        	log.trace(String.format("SpriteRenderer"));
        }

        if (!isVisible()) {
        	return false;
        }

        initRendering(context);

        setVertexTextures(context, v1, v2);

        if (transform2D) {
        	sourceDepth = (int) v2.p[2];
        } else {
        	sourceDepth = (int) prim.p2z;
        }

        return true;
	}

	@Override
	public void render() {
		if (compiledRenderer != null) {
			compiledRenderer.render(this);
			return;
		}

		log.info("Non-compiled rendering!");

		preRender();

		RESoftware.spriteRenderStatistics.start();

		float v = prim.vStart;
        for (int y = 0; y < prim.destinationHeight; y++) {
        	pixel.y = prim.pyMin + y;
    		float u = prim.uStart;
        	for (int x = 0; x < prim.destinationWidth; x++) {
        		pixel.newPixel2D();
            	pixel.x = prim.pxMin + x;
        		pixel.u = u;
        		pixel.v = v;
            	pixel.sourceDepth = sourceDepth;
            	rendererWriter.readCurrent(pixel);
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
        			rendererWriter.writeNext(pixel);
    			} else if (pixel.filterOnFailed != null) {
    				// Filter did not pass, but we have a filter to be executed in that case
    				pixel.source = pixel.destination;
    				pixel.filterOnFailed.filter(pixel);
    				rendererWriter.writeNextColor(pixel);
        		} else {
        			// Filter did not pass, do not update the pixel
        			writerSkip(1);
        		}
        		u += prim.uStep;
        	}
        	writerSkipEOL();
        	v += prim.vStep;
        }

		RESoftware.spriteRenderStatistics.end();

		postRender();
	}

	@Override
	public IRenderer duplicate() {
		SpriteRenderer spriteRenderer = new SpriteRenderer();
		spriteRenderer.copy(this);

		return spriteRenderer;
	}
}
