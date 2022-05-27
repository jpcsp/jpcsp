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
import jpcsp.graphics.RE.IRenderingEngine;

/**
 * @author gid15
 *
 */
public class GEIndexedTexture extends GEResizedTexture {
	public GEIndexedTexture(GETexture geTexture, int address, int bufferWidth, int width, int height, int pixelFormat) {
		super(geTexture, address, bufferWidth, width, height, pixelFormat);

		// Map the pixel format:
		// TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED -> RE_PIXEL_STORAGE_16BIT_INDEXED_BGR5650
		//                                          RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR5651
		//                                          RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR4444
		// TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED -> RE_PIXEL_STORAGE_32BIT_INDEXED_ABGR8888
		switch (pixelFormat) {
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED:
				this.pixelFormat = geTexture.pixelFormat - GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650 + IRenderingEngine.RE_PIXEL_STORAGE_16BIT_INDEXED_BGR5650;
				break;
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED:
				this.pixelFormat = IRenderingEngine.RE_PIXEL_STORAGE_32BIT_INDEXED_ABGR8888;
				break;
		}
	}

	@Override
	protected void updateTexture(IRenderingEngine re) {
		re.setTextureFormat(pixelFormat, false);
		super.updateTexture(re);
	}

	@Override
	public String toString() {
		return String.format("GEIndexedTexture[%dx%d, base=%s]", getWidth(), getHeight(), geTexture.toString());
	}
}
