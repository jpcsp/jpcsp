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

import java.util.HashMap;

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.RE.IRenderingEngine;

/**
 * @author gid15
 *
 */
public class GETextureManager {
	private static GETextureManager instance;
	private HashMap<Long, GETexture> geTextures = new HashMap<Long, GETexture>();

	public static GETextureManager getInstance() {
		if (instance == null) {
			instance = new GETextureManager();
		}
		return instance;
	}

	private Long getKey(int address, int bufferWidth, int width, int height, int pixelFormat) {
		return address +
		       (((long) bufferWidth) << 30) +
		       (((long) width) << 40) +
		       (((long) height) << 50) +
		       (((long) pixelFormat) << 60);
	}

	public GETexture checkGETexture(int address, int bufferWidth, int width, int height, int pixelFormat) {
		Long key = getKey(address, bufferWidth, width, height, pixelFormat);
		return geTextures.get(key);
	}

	private GETexture checkGETexturePSM8888(int address, int bufferWidth, int width, int height, int pixelFormat) {
		GETexture geTexture = null;

		if (pixelFormat == GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
			geTexture = checkGETexture(address, bufferWidth << 1, width, height, GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650);
			if (geTexture != null) {
				Long key = getKey(address, bufferWidth, width, height, pixelFormat);
				geTextures.remove(key);
			}
		}

		return geTexture;
	}

	public GETexture getGETexture(IRenderingEngine re, int address, int bufferWidth, int width, int height, int pixelFormat, boolean useViewportResize) {
		GETexture geTexture = checkGETexturePSM8888(address, bufferWidth, width, height, pixelFormat);
		if (geTexture == null) {
			geTexture = checkGETexture(address, bufferWidth, width, height, pixelFormat);
		}

		if (geTexture == null) {
			Long key = getKey(address, bufferWidth, width, height, pixelFormat);
			geTexture = new GETexture(address, bufferWidth, width, height, pixelFormat, useViewportResize);
			geTextures.put(key, geTexture);
		}

		return geTexture;
	}

	public GETexture getGEResizedTexture(IRenderingEngine re, GETexture baseGETexture, int address, int bufferWidth, int width, int height, int pixelFormat) {
		GETexture geTexture = checkGETexturePSM8888(address, bufferWidth, width, height, pixelFormat);
		if (geTexture == null) {
			geTexture = checkGETexture(address, bufferWidth, width, height, pixelFormat);
		}

		if (geTexture == null) {
			Long key = getKey(address, bufferWidth, width, height, pixelFormat);
			geTexture = new GEResizedTexture(baseGETexture, address, bufferWidth, width, height, pixelFormat);
			geTextures.put(key, geTexture);
		}

		return geTexture;
	}

	public GETexture getGEIndexedTexture(IRenderingEngine re, GETexture baseGETexture, int address, int bufferWidth, int width, int height, int pixelFormat) {
		GETexture geTexture = checkGETexture(address, bufferWidth, width, height, pixelFormat);

		if (geTexture == null) {
			Long key = getKey(address, bufferWidth, width, height, pixelFormat);
			geTexture = new GEIndexedTexture(baseGETexture, address, bufferWidth, width, height, pixelFormat);
			geTextures.put(key, geTexture);
		}

		return geTexture;
	}

	public void reset(IRenderingEngine re) {
		for (GETexture geTexture : geTextures.values()) {
			geTexture.delete(re);
		}

		geTextures.clear();
	}
}
