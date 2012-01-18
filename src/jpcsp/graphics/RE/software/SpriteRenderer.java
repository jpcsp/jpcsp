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
public class SpriteRenderer extends BaseRenderer {
	protected VertexState v1;
	protected VertexState v2;
	int sourceDepth;

	public SpriteRenderer(VertexState v1, VertexState v2) {
		this.v1 = v1;
		this.v2 = v2;
	}

	@Override
	public boolean prepare(GeContext context, CachedTexture texture) {
		init(context);
		setPositions(v1, v2);

        if (!isVisible(context)) {
        	return false;
        }

        if (log.isTraceEnabled()) {
        	log.trace(String.format("SpriteRenderer"));
        }

        setTextures(v1, v2);

        prepareTextureReader(context, texture, v1, v2);
    	prepareWriters(context);
        if (imageWriter == null || depthWriter == null) {
        	return false;
        }

        prepareFilters(context);

        sourceDepth = (int) v2.p[2];

        return true;
	}

	@Override
	public void render() {
    	pixel.sourceDepth = sourceDepth;
    	float v = vStart;
        for (int y = 0; y < destinationHeight; y++) {
        	pixel.y = pyMin + y;
    		pixel.v = v;
    		float u = uStart;
        	for (int x = 0; x < destinationWidth; x++) {
            	pixel.x = pxMin + x;
        		pixel.u = u;
        		pixel.filterPassed = true;
        		pixel.destination = imageWriter.readCurrent();
        		pixel.destinationDepth = depthWriter.readCurrent();
        		for (int i = 0; i < numberFilters; i++) {
        			pixel.source = filters[i].filter(pixel);
        			if (!pixel.filterPassed) {
        				break;
        			}
        		}
        		if (pixel.filterPassed) {
        			imageWriter.writeNext(pixel.source);
        			depthWriter.writeNext(pixel.sourceDepth);
        		} else {
        			// Filter did not pass, do not update the pixel
        			imageWriter.skip(1);
        			depthWriter.skip(1);
        		}
        		u += uStep;
        	}
        	v += vStep;
        }
        super.render();
	}
}
