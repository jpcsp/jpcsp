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
	public TriangleRenderer(GeContext context, CachedTextureResampled texture, boolean useVertexTexture) {
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
        	if (isLogTraceEnabled) {
        		log.trace(String.format("Triangle not visible"));
        	}
        	return false;
        }

        initRendering(context);

        setVertexTextures(context, v1, v2, v3);

        return true;
	}

	@Override
	public IRenderer duplicate() {
		TriangleRenderer triangleRenderer = new TriangleRenderer();
		triangleRenderer.copy(this);

		return triangleRenderer;
	}
}
