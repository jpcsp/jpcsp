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

import static jpcsp.graphics.RE.software.PixelColor.divideBy2;
import static jpcsp.graphics.RE.software.PixelColor.divideBy4;
import static jpcsp.graphics.RE.software.PixelColor.multiply;
import static jpcsp.util.Utilities.pixelToTexel;
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
	private static final boolean disableResampleAllTextures = false;
	private static final boolean disableResampleVRAMTexture = true;
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

	public boolean canResample(float widthFactor, float heightFactor) {
		if (disableResampleAllTextures) {
			return false;
		}

		if (disableResampleVRAMTexture && cachedTextureOriginal.isVRAMTexture()) {
			// VRAM textures are often minimized or magnified by a factor of 2.
			// Allow these resamplings.
			if ((widthFactor == .5f && heightFactor == .5f) || (widthFactor == 2f && heightFactor == 2f)) {
				return true;
			}
			return false;
		}

		return widthFactor >= .5f && heightFactor >= .5f && widthFactor <= 2f && heightFactor <= 2f;
	}

	public CachedTexture resample(float widthFactor, float heightFactor) {
		if (widthFactor == 1f && heightFactor == 1f) {
			return cachedTextureOriginal;
		}

		int width = round(widthFactor * cachedTextureOriginal.width);
		int height = round(heightFactor * cachedTextureOriginal.height);

		return resample(width, height);
	}

	/**
	 * This method has to be synchronized because it can be used but multiple
	 * renderer threads in parallel (see RendererExecutor).
	 */
	private synchronized CachedTexture resample(int width, int height) {
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
		} else if (VideoEngine.log.isDebugEnabled()) {
			VideoEngine.log.debug(String.format("Resampling texture from (%d,%d) to (%d,%d), pixelFormat=%d, resampled %d times", cachedTextureOriginal.width, cachedTextureOriginal.height, width, height, cachedTextureOriginal.pixelFormat, resampleInfos.size()));
		}

		RESoftware.textureResamplingStatistics.start();

		int widthPow2 = makePow2(width);
		int heightPow2 = makePow2(height);
		int[] buffer = new int[widthPow2 * heightPow2];
		int widthSkipEOL = widthPow2 - width;
		if (cachedTextureOriginal.width == (width << 1) &&
		    cachedTextureOriginal.height == (height << 1)) {
			// Optimized common case: minimize texture by a factor of 2
			resampleTextureMinimize2(buffer, width, height, widthSkipEOL);
		} else if ((cachedTextureOriginal.width << 1) == width &&
		           (cachedTextureOriginal.height << 1) == height) {
			// Optimized common case: magnify texture by a factor of 2
			resampleTextureMagnify2(buffer, width, height, widthSkipEOL);
		} else {
			// Generic case: magnify/minimize by arbitrary factors
			float widthFactor = cachedTextureOriginal.width / (float) width;
			float heightFactor = cachedTextureOriginal.height / (float) height;
			resampleTexture(buffer, width, height, widthSkipEOL, widthFactor, heightFactor);
		}

		CachedTexture cachedTextureResampled = new CachedTexturePow2(widthPow2, heightPow2, width, height, GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888);
		cachedTextureResampled.setBuffer(buffer, 0, buffer.length);

		ResampleInfo resampleInfo = new ResampleInfo(width, height, cachedTextureResampled);
		resampleInfos.add(resampleInfo);

		RESoftware.textureResamplingStatistics.end();

		return cachedTextureResampled;
	}

	private void resampleTexture(int[] buffer, int width, int height, int widthSkipEOL, float widthFactor, float heightFactor) {
		float v = 0f;
		int index = 0;
		for (int y = 0; y < height; y++) {
			float u = 0f;
			for (int x = 0; x < width; x++) {
				buffer[index++] = readTexturePixelInterpolated(u, v, widthFactor, heightFactor);
				u += widthFactor;
			}
			index += widthSkipEOL;
			v += heightFactor;
		}
	}

	private void resampleTextureMinimize2(int[] buffer, int width, int height, int widthSkipEOL) {
		int index = 0;
		int pixel;
		for (int y = 0, v = 0; y < height; y++, v += 2) {
			for (int x = 0, u = 0; x < width; x++, u += 2) {
				pixel = divideBy4(cachedTextureOriginal.readPixel(u, v));
				pixel += divideBy4(cachedTextureOriginal.readPixel(u + 1, v));
				pixel += divideBy4(cachedTextureOriginal.readPixel(u, v + 1));
				pixel += divideBy4(cachedTextureOriginal.readPixel(u + 1, v + 1));
				buffer[index++] = pixel;
			}
			index += widthSkipEOL;
		}
	}

	private void resampleTextureMagnify2(int[] buffer, int width, int height, int widthSkipEOL) {
		int index = 0;
		int pixel;
		height -= 2;
		width -= 2;
		int lastU = width / 2;
		int lastV = height / 2;
		for (int y = 0, v = 0; y < height; y += 2, v++) {
			int currentPixel = cachedTextureOriginal.readPixel(0, v);
			for (int x = 0, u = 1; x < width; x += 2, u++) {
				buffer[index++] = currentPixel;
				pixel = divideBy2(currentPixel);
				currentPixel = cachedTextureOriginal.readPixel(u, v);
				pixel += divideBy2(currentPixel);
				buffer[index++] = pixel;
			}

			int pixelLastU = cachedTextureOriginal.readPixel(lastU, v);
			buffer[index++] = pixelLastU;
			buffer[index++] = pixelLastU;

			index += widthSkipEOL;

			for (int x = 0, u = 0; x < width; x += 2, u++) {
				pixel = divideBy2(cachedTextureOriginal.readPixel(u, v));
				pixel += divideBy2(cachedTextureOriginal.readPixel(u, v + 1));
				buffer[index++] = pixel;
				pixel = divideBy2(pixel);
				pixel += divideBy4(cachedTextureOriginal.readPixel(u + 1, v));
				pixel += divideBy4(cachedTextureOriginal.readPixel(u + 1, v + 1));
				buffer[index++] = pixel;
			}

			pixel = divideBy2(pixelLastU);
			pixel += divideBy2(cachedTextureOriginal.readPixel(lastU, v + 1));
			buffer[index++] = pixel;
			buffer[index++] = pixel;

			index += widthSkipEOL;
		}

		int currentPixel = cachedTextureOriginal.readPixel(0, lastV);
		int index2 = index + width + widthSkipEOL + 2;
		for (int x = 0, u = 0; x < width; x += 2, u++) {
			buffer[index++] = currentPixel;
			buffer[index2++] = currentPixel;
			pixel = divideBy2(currentPixel);
			currentPixel = cachedTextureOriginal.readPixel(u, lastV);
			pixel += divideBy2(currentPixel);
			buffer[index++] = pixel;
			buffer[index2++] = pixel;
		}

		int pixelLastU = cachedTextureOriginal.readPixel(lastU, lastV);
		buffer[index++] = pixelLastU;
		buffer[index] = pixelLastU;
		buffer[index2++] = pixelLastU;
		buffer[index2] = pixelLastU;
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
	 * @param u            pixel position along X-axis
	 * @param v            pixel position along Y-axis
	 * @param widthFactor  factor between original and resampled width
	 * @param heightFactor factor between original and resampled height
	 * @return             interpolated texture value at (u,v)
	 */
	private int readTexturePixelInterpolated(float u, float v, float widthFactor, float heightFactor) {
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

		// If we fall exactly on one texel, take also the next texel into account
		// if we are minimizing the texture
		// (i.e. if the resampling factor is larger than 1)
		if (factorU1 == 0f && widthFactor > 1f) {
			factorU1 = (widthFactor - 1f) / widthFactor;
			factorU0 = 1f / widthFactor;
		}
		if (factorV1 == 0f && heightFactor > 1f) {
			factorV1 = (heightFactor - 1f) / heightFactor;
			factorV0 = 1f / heightFactor;
		}

		int pixel;
		if (factorU0 > 0f && factorV0 > 0f) {
			pixel = multiply(cachedTextureOriginal.readPixel(texelU0, texelV0), factorU0 * factorV0);
		} else {
			pixel = 0;
		}
		if (factorU1 > 0f && factorV0 > 0f) {
			pixel += multiply(cachedTextureOriginal.readPixel(texelU1, texelV0), factorU1 * factorV0);
		}
		if (factorU0 > 0f && factorV1 > 0f) {
			pixel += multiply(cachedTextureOriginal.readPixel(texelU0, texelV1), factorU0 * factorV1);
		}
		if (factorU1 > 0f && factorV1 > 0f) {
			pixel += multiply(cachedTextureOriginal.readPixel(texelU1, texelV1), factorU1 * factorV1);
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
