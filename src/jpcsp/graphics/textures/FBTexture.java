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

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;

/**
 * A texture being used as a render target, using OpenGL FrameBuffer Object (FBO).
 * 
 * @author gid15
 *
 */
public class FBTexture extends GETexture {
	private int fboId = -1;
	private int depthRenderBufferId = -1;

	public FBTexture(int address, int bufferWidth, int width, int height, int pixelFormat) {
		super(address, bufferWidth, width, height, pixelFormat, true);
	}

	public FBTexture(FBTexture copy) {
		super(copy.address, copy.bufferWidth, copy.width, copy.height, copy.pixelFormat, copy.useViewportResize);
	}

	@Override
	public void bind(IRenderingEngine re, boolean forDrawing) {
		if (forDrawing) {
			// We are copying the texture back to the main frame buffer,
			// bind the texture, not the FBO.
			re.bindFramebuffer(IRenderingEngine.RE_FRAMEBUFFER, 0);
			super.bind(re, forDrawing);
		} else {
			if (fboId == -1) {
				createFBO(re, forDrawing);
			} else {
				// Bind the FBO
				re.bindFramebuffer(IRenderingEngine.RE_FRAMEBUFFER, fboId);
			}
		}
	}

	protected void createFBO(IRenderingEngine re, boolean forDrawing) {
		// Create the FBO and associate it to the texture
		fboId = re.genFramebuffer();
		re.bindFramebuffer(IRenderingEngine.RE_FRAMEBUFFER, fboId);

		// Create a render buffer for the depth buffer
		depthRenderBufferId = re.genRenderbuffer();
		re.bindRenderbuffer(depthRenderBufferId);
		re.setRenderbufferStorage(IRenderingEngine.RE_DEPTH_COMPONENT, getTexImageWidth(), getTexImageHeight());

		// Create the texture
		super.bind(re, forDrawing);

		// Attach the texture to the FBO
		re.setFramebufferTexture(IRenderingEngine.RE_FRAMEBUFFER, IRenderingEngine.RE_COLOR_ATTACHMENT0, textureId, 0);
		// Attach the depth buffer to the FBO
		re.setFramebufferRenderbuffer(IRenderingEngine.RE_FRAMEBUFFER, IRenderingEngine.RE_DEPTH_ATTACHMENT, depthRenderBufferId);
	}

	@Override
	public void delete(IRenderingEngine re) {
		if (fboId != -1) {
			re.deleteFramebuffer(fboId);
			fboId = -1;
		}
		if (depthRenderBufferId != -1) {
			re.deleteRenderbuffer(depthRenderBufferId);
			depthRenderBufferId = -1;
		}
		super.delete(re);
	}

	public void blitFrom(IRenderingEngine re, FBTexture src) {
		if (fboId == -1) {
			createFBO(re, false);
		}

		// Bind the source and destination FBOs
		re.bindFramebuffer(IRenderingEngine.RE_READ_FRAMEBUFFER, src.fboId);
		re.bindFramebuffer(IRenderingEngine.RE_DRAW_FRAMEBUFFER, fboId);

		// Copy the source FBO to the destination FBO
		re.blitFramebuffer(0, 0, src.getResizedWidth(), src.getResizedHeight(), 0, 0, getResizedWidth(), getResizedHeight(), IRenderingEngine.RE_COLOR_BUFFER_BIT, GeCommands.TFLT_NEAREST);

		// Re-bind the source FBO
		re.bindFramebuffer(IRenderingEngine.RE_FRAMEBUFFER, src.fboId);
	}

	@Override
	public String toString() {
		return String.format("FBTexture[0x%08X-0x%08X, %dx%d, bufferWidth=%d, pixelFormat=%d(%s)]", address, address + length, width, height, bufferWidth, pixelFormat, VideoEngine.getPsmName(pixelFormat));
	}
}
