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

	public SpriteRenderer(GeContext context, CachedTextureResampled texture, boolean useVertexTexture) {
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
	public IRenderer duplicate() {
		SpriteRenderer spriteRenderer = new SpriteRenderer();
		spriteRenderer.copy(this);

		return spriteRenderer;
	}
}
