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
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public abstract class GEProxyTexture extends GETexture {
	private int fboId = -1;
	protected GETexture geTexture;

	public GEProxyTexture(GETexture geTexture, int address, int bufferWidth, int width, int height, int pixelFormat, boolean useViewportResize) {
		super(address, Utilities.makePow2(width), width, height, pixelFormat, useViewportResize);
		this.geTexture = geTexture;
	}

	@Override
	public void bind(IRenderingEngine re, boolean forDrawing) {
		super.bind(re, forDrawing);

		if (isUpdateRequired(re)) {
			// Update the texture each time the GETexture has changed
			if (fboId == -1) {
				fboId = re.genFramebuffer();
				re.bindFramebuffer(IRenderingEngine.RE_FRAMEBUFFER, fboId);
				re.setFramebufferTexture(IRenderingEngine.RE_FRAMEBUFFER, IRenderingEngine.RE_COLOR_ATTACHMENT0, textureId, 0);
			} else {
				re.bindFramebuffer(IRenderingEngine.RE_FRAMEBUFFER, fboId);
			}

			updateTexture(re);

			re.bindFramebuffer(IRenderingEngine.RE_FRAMEBUFFER, 0);
			re.bindTexture(textureId);
			if (forDrawing) {
				re.setTextureFormat(pixelFormat, false);
			}

			geTexture.setChanged(false);
		}
	}

	protected boolean isUpdateRequired(IRenderingEngine re) {
		return geTexture.hasChanged();
	}

	@Override
	protected boolean hasChanged() {
		return geTexture.hasChanged();
	}

	abstract protected void updateTexture(IRenderingEngine re);
}
