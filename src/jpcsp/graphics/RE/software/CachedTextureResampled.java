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

import static jpcsp.graphics.RE.software.PixelColor.add;
import static jpcsp.graphics.RE.software.PixelColor.multiply;
import static jpcsp.graphics.RE.software.TextureReader.pixelToTexel;
import static jpcsp.util.Utilities.makePow2;
import static jpcsp.util.Utilities.round;

import java.util.LinkedList;

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.software.CachedTexture.CachedTexturePow2;

/**
 * @author gid15
 *
 */
public class CachedTextureResampled {
	protected LinkedList<ResampleInfo> resampleInfos = new LinkedList<CachedTextureResampled.ResampleInfo>();
	protected CachedTexture cachedTextureOriginal;

	public CachedTextureResampled(CachedTexture cachedTexture) {
		cachedTextureOriginal = cachedTexture;
		ResampleInfo resampleInfo = new ResampleInfo(cachedTexture.width, cachedTexture.height, cachedTexture);
		resampleInfos.add(resampleInfo);
	}

	public CachedTexture getOriginalTexture() {
		return cachedTextureOriginal;
	}

	public CachedTexture resample(float widthFactor, float heightFactor) {
		if (widthFactor == 1f && heightFactor == 1f) {
			return cachedTextureOriginal;
		}

		int width = round(widthFactor * cachedTextureOriginal.width);
		int height = round(heightFactor * cachedTextureOriginal.height);

		return resample(width, height);
	}

	private CachedTexture resample(int width, int height) {
		// Was the texture already resampled at the given size?
		for (ResampleInfo resampleInfo : resampleInfos) {
			if (resampleInfo.matches(width, height)) {
				return resampleInfo.getCachedTextureResampled();
			}
		}

		// A resampled texture was not yet available, compute one.
		return resampleTexture(width, height);
	}

	private CachedTexture resampleTexture(int width, int height) {
		if (resampleInfos.size() >= 5 && VideoEngine.log.isInfoEnabled()) {
			VideoEngine.log.info(String.format("Resampling texture from (%d,%d) to (%d,%d), pixelFormat=%d, resampled %d times", cachedTextureOriginal.width, cachedTextureOriginal.height, width, height, cachedTextureOriginal.pixelFormat, resampleInfos.size()));
		}
		if (VideoEngine.log.isDebugEnabled()) {
			VideoEngine.log.debug(String.format("Resampling texture from (%d,%d) to (%d,%d), pixelFormat=%d, resampled %d times", cachedTextureOriginal.width, cachedTextureOriginal.height, width, height, cachedTextureOriginal.pixelFormat, resampleInfos.size()));
		}

		int widthPow2 = makePow2(width);
		int heightPow2 = makePow2(height);
		int[] buffer = new int[widthPow2 * heightPow2];
		float widthFactor = cachedTextureOriginal.width / (float) width;
		float heightFactor = cachedTextureOriginal.height / (float) height;
		float v = 0f;
		int index = 0;
		for (int y = 0; y < height; y++) {
			float u = 0f;
			for (int x = 0; x < width; x++) {
				buffer[index + x] = readTexturePixelInterpolated(u, v);
				u += widthFactor;
			}
			index += widthPow2;
			v += heightFactor;
		}

		CachedTexture cachedTextureResampled = new CachedTexturePow2(widthPow2, heightPow2, GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888);
		cachedTextureResampled.setBuffer(buffer, 0, buffer.length);

		ResampleInfo resampleInfo = new ResampleInfo(width, height, cachedTextureResampled);
		resampleInfos.add(resampleInfo);

		return cachedTextureResampled;
	}

	/**
	 * Interpolate a texture value at position (u,v) based on its 4 neighboring texels.
	 *
	 * (u0,v0)-------(u1,v0)
	 *    |   \     /   |
	 *    |    (u,v)    |
	 *    |   /     \   |
	 * (u0,v1)-------(u1,v1)
	 *
	 * Example: for the pixel at (u=1.3, v=1.6), the following texture value is returned:
	 *      texel(1,1) * 0.7 * 0.4 +
	 *      texel(1,2) * 0.7 * 0.6 +
	 *      texel(2,1) * 0.3 * 0.4 +
	 *      texel(2,2) * 0.3 * 0.6
	 *
	 * @param u     pixel position along X-axis
	 * @param v     pixel position along Y-axis
	 * @return      interpolated texture value at (u,v)
	 */
	private int readTexturePixelInterpolated(float u, float v) {
		int texelU0 = pixelToTexel(u);
		int texelV0 = pixelToTexel(v);
		int texelU1 = texelU0 + 1;
		int texelV1 = texelV0 + 1;
		if (texelU1 >= cachedTextureOriginal.width) {
			texelU1 = texelU0;
		}
		if (texelV1 >= cachedTextureOriginal.height) {
			texelV1 = texelV0;
		}

		float factorU1 = u - texelU0;
		float factorV1 = v - texelV0;
		float factorU0 = 1f - factorU1;
		float factorV0 = 1f - factorV1;

		int pixel;
		if (factorU0 > 0f && factorV0 > 0f) {
			pixel = multiply(cachedTextureOriginal.readPixel(texelU0, texelV0), factorU0 * factorV0);
		} else {
			pixel = 0;
		}
		if (factorU1 > 0f && factorV0 > 0f) {
			pixel = add(pixel, multiply(cachedTextureOriginal.readPixel(texelU1, texelV0), factorU1 * factorV0));
		}
		if (factorU0 > 0f && factorV1 > 0f) {
			pixel = add(pixel, multiply(cachedTextureOriginal.readPixel(texelU0, texelV1), factorU0 * factorV1));
		}
		if (factorU1 > 0f && factorV1 > 0f) {
			pixel = add(pixel, multiply(cachedTextureOriginal.readPixel(texelU1, texelV1), factorU1 * factorV1));
		}

		return pixel;
	}

	public void setClut() {
		cachedTextureOriginal.setClut();
	}

	private static class ResampleInfo {
		private int resampleWidth;
		private int resampleHeight;
		private CachedTexture cachedTextureResampled;

		public ResampleInfo(int resampleWidth, int resampleHeight, CachedTexture cachedTextureResampled) {
			this.resampleWidth = resampleWidth;
			this.resampleHeight = resampleHeight;
			this.cachedTextureResampled = cachedTextureResampled;
		}

		public boolean matches(int width, int height) {
			return width == resampleWidth && height == resampleHeight;
		}

		public CachedTexture getCachedTextureResampled() {
			return cachedTextureResampled;
		}
	}
}
