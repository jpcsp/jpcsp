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

/**
 * @author gid15
 *
 */
public class BoundingBoxRenderer extends BasePrimitiveRenderer {
	protected float[][] boundingBoxPositions;

	public BoundingBoxRenderer(GeContext context) {
		init(context, null, false, false);
	}

	public void drawBoundingBox(float[][] boundingBoxPositions) {
		this.boundingBoxPositions = boundingBoxPositions;
	}

	@Override
	public boolean prepare(GeContext context) {
		for (int i = 0; i < boundingBoxPositions.length; i++) {
			addPosition(boundingBoxPositions[i]);
		}

		if (!insideScissor()) {
			return false;
		}

		return true;
	}

	@Override
	public void render() {
	}

	@Override
	public IRenderer duplicate() {
		return this;
	}
}
