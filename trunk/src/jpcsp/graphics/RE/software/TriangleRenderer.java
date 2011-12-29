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
public class TriangleRenderer extends BaseRenderer {
	protected VertexState v1;
	protected VertexState v2;
	protected VertexState v3;

	public TriangleRenderer(VertexState v1, VertexState v2, VertexState v3) {
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
	}

	@Override
	public boolean prepare(GeContext context) {
		if (context.vinfo.position == 0 || !context.vinfo.transform2D) {
			// TODO Not yet implemented
			return false;
		}

		setPositions(v1, v2, v3);

        if (!insideScissor(context)) {
        	return false;
        }

        setTextures(v1, v2, v3);

        prepareSourceReader(context, v1, v2, v3);
        prepareWriters(context);
        if (sourceReader == null || imageWriter == null || depthWriter == null) {
        	return false;
        }

        if (log.isTraceEnabled()) {
        	log.trace(String.format("TriangleRenderer"));
        }
        prepareFilters(context);

		return true;
	}

	@Override
	public void render() {
    	pixel.y = pyMin;
        for (int y = 0; y < destinationHeight; y++) {
        	pixel.x = pxMin;
        	for (int x = 0; x < destinationWidth; x++) {
        		if (isInsideTriangle()) {
        			pixel.filterPassed = true;
        			pixel.sourceDepth = Math.round(pixel.getTriangleWeightedValue(p1z, p2z, p3z));
            		pixel.destination = imageWriter.readCurrent();
            		pixel.destinationDepth = depthWriter.readCurrent();
            		pixel.source = sourceReader.read(pixel);
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
        		} else {
        			// Do not display, skip the pixel
        			sourceReader.read(pixel);
        			imageWriter.skip(1);
        			depthWriter.skip(1);
        		}
    			pixel.x++;
        	}
        	pixel.y++;
        }
        imageWriter.flush();
        depthWriter.flush();
	}
}
