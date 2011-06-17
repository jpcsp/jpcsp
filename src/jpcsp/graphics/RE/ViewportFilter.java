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
package jpcsp.graphics.RE;

import jpcsp.HLE.modules.sceDisplay;

public class ViewportFilter extends BaseRenderingEngineProxy {
    private boolean isDirectRendering;

	public ViewportFilter(IRenderingEngine proxy) {
		super(proxy);
	}

	@Override
	public void startDisplay() {
		isDirectRendering = false;
		super.startDisplay();
	}

	@Override
	public void setRenderingEngine(IRenderingEngine re) {
		super.setRenderingEngine(re);
	}

	@Override
	public void setViewport(int x, int y, int width, int height) {
		// No viewport resizing when rendering in direct mode
		if (!isDirectRendering) {
			x = sceDisplay.getResizedWidth(x);
			y = sceDisplay.getResizedHeight(y);
			width = sceDisplay.getResizedWidth(width);
			height = sceDisplay.getResizedHeight(height);
		}
		super.setViewport(x, y, width, height);
	}

    @Override
	public void setScissor(int x, int y, int width, int height) {
		// No viewport resizing when rendering in direct mode
		if (!isDirectRendering) {
			x = sceDisplay.getResizedWidth(x);
			y = sceDisplay.getResizedHeight(y);
			width = sceDisplay.getResizedWidth(width);
			height = sceDisplay.getResizedHeight(height);
		}
		super.setScissor(x, y, width, height);
	}

	@Override
	public void endDirectRendering() {
		isDirectRendering = false;
		super.endDirectRendering();
	}

	@Override
	public void startDirectRendering(boolean textureEnabled, boolean depthWriteEnabled, boolean colorWriteEnabled, boolean setOrthoMatrix, boolean orthoInverted, int width, int height) {
		isDirectRendering = true;
		super.startDirectRendering(textureEnabled, depthWriteEnabled, colorWriteEnabled, setOrthoMatrix, orthoInverted, width, height);
	}
}
