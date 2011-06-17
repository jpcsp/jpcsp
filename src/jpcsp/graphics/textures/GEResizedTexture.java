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
package jpcsp.graphics.textures;

import jpcsp.graphics.RE.IRenderingEngine;

/**
 * @author gid15
 *
 */
public class GEResizedTexture extends GEProxyTexture {
	protected int x;
	protected int y;

	public GEResizedTexture(GETexture geTexture, int address, int bufferWidth, int width, int height, int pixelFormat) {
		super(geTexture, address, bufferWidth, width, height, pixelFormat, false);

		x = 0;
		y = height - geTexture.getHeight();
	}

	@Override
	protected void updateTexture(IRenderingEngine re) {
		// Resize the GETexture to the requested texture size.
		// This has to be performed each time the base GETexture has changed.
		geTexture.copyTextureToScreen(re, x, y, getWidth(), getHeight(), false, true, true, true, true);
	}

	@Override
	public String toString() {
		return String.format("GEResizedTexture[%dx%d, base=%s]", getWidth(), getHeight(), geTexture.toString());
	}
}
