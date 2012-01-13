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

import static jpcsp.memory.ImageReader.color4444to8888;
import static jpcsp.memory.ImageReader.color5551to8888;
import static jpcsp.memory.ImageReader.color565to8888;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 * A cached texture for the software Rendering Engine (RESoftware).
 * The whole texture is read at initialization.
 */
public abstract class CachedTexture implements IRandomTextureAccess {
	protected final int width;
	protected final int height;
	protected final int pixelFormat;
	protected final int widthPower2;
	protected final int heightPower2;
	protected int[] buffer;

	public static CachedTexture getCachedTexture(int width, int height, int pixelFormat, int[] buffer, int bufferOffset, int bufferLength) {
		CachedTexture cachedTexture = getCachedTexture(width, height, pixelFormat);
		cachedTexture.setBuffer(buffer, bufferOffset, bufferLength);

		return cachedTexture;
	}

	public static CachedTexture getCachedTexture(int width, int height, int pixelFormat, short[] buffer, int bufferOffset, int bufferLength) {
		CachedTexture cachedTexture = getCachedTexture(width, height, pixelFormat);
		cachedTexture.setBuffer(buffer, bufferOffset, bufferLength);

		return cachedTexture;
	}

	private static CachedTexture getCachedTexture(int width, int height, int pixelFormat) {
		CachedTexture cachedTexture;

		if (width == Utilities.makePow2(width)) {
			cachedTexture = new CachedTexturePow2(width, height, pixelFormat);
		} else {
			cachedTexture = new CachedTextureNonPow2(width, height, pixelFormat);
		}

		return cachedTexture;
	}

	protected CachedTexture(int width, int height, int pixelFormat) {
		this.width = width;
		this.height = height;
		this.pixelFormat = pixelFormat;
		widthPower2 = getPower2(width);
		heightPower2 = getPower2(height);
	}

	protected static int getPower2(int n) {
		return Integer.numberOfTrailingZeros(Utilities.makePow2(n));
	}

	protected void setBuffer(int[] buffer, int bufferOffset, int bufferLength) {
		switch (pixelFormat) {
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
				this.buffer = new int[bufferLength * 2];
				for (int i = 0, j = 0; i < bufferLength; i++) {
					int color = buffer[bufferOffset + i];
					this.buffer[j++] = color565to8888(color & 0xFFFF);
					this.buffer[j++] = color565to8888(color >>> 16);
				}
				break;
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
				this.buffer = new int[bufferLength * 2];
				for (int i = 0, j = 0; i < bufferLength; i++) {
					int color = buffer[bufferOffset + i];
					this.buffer[j++] = color5551to8888(color & 0xFFFF);
					this.buffer[j++] = color5551to8888(color >>> 16);
				}
				break;
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
				this.buffer = new int[bufferLength * 2];
				for (int i = 0, j = 0; i < bufferLength; i++) {
					int color = buffer[bufferOffset + i];
					this.buffer[j++] = color4444to8888(color & 0xFFFF);
					this.buffer[j++] = color4444to8888(color >>> 16);
				}
				break;
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888:
				this.buffer = new int[bufferLength];
				System.arraycopy(buffer, bufferOffset, this.buffer, 0, bufferLength);
				break;
			default:
				VideoEngine.log.error(String.format("CachedTexture setBuffer int unsupported pixel format %d", pixelFormat));
				break;
		}
	}

	protected void setBuffer(short[] buffer, int bufferOffset, int bufferLength) {
		switch (pixelFormat) {
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
				this.buffer = new int[bufferLength];
				for (int i = 0; i < bufferLength; i++) {
					this.buffer[i] = color565to8888(buffer[bufferOffset + i] & 0xFFFF);
				}
				break;
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
				this.buffer = new int[bufferLength];
				for (int i = 0; i < bufferLength; i++) {
					this.buffer[i] = color5551to8888(buffer[bufferOffset + i] & 0xFFFF);
				}
				break;
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
				this.buffer = new int[bufferLength];
				for (int i = 0; i < bufferLength; i++) {
					this.buffer[i] = color4444to8888(buffer[bufferOffset + i] & 0xFFFF);
				}
				break;
			case GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888:
				this.buffer = new int[bufferLength / 2];
				for (int i = 0, j = bufferOffset; i < this.buffer.length; i++) {
					this.buffer[i] = (buffer[j++] & 0xFFFF) | (buffer[j++] << 16);
				}
			default:
				VideoEngine.log.error(String.format("CachedTexture setBuffer short unsupported pixel format %d", pixelFormat));
				break;
		}
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getPixelFormat() {
		return pixelFormat;
	}

	/**
	 * @author gid15
	 *
	 * A specialized class when the width is a power of 2 (faster).
	 */
	private static class CachedTexturePow2 extends CachedTexture {
		public CachedTexturePow2(int width, int height, int pixelFormat) {
			super(width, height, pixelFormat);
		}

		@Override
		public int readPixel(int u, int v) {
			return buffer[(v << widthPower2) + u];
		}
	}

	/**
	 * @author gid15
	 *
	 * A specialized class when the width is not a power of 2.
	 */
	private static class CachedTextureNonPow2 extends CachedTexture {
		public CachedTextureNonPow2(int width, int height, int pixelFormat) {
			super(width, height, pixelFormat);
		}

		@Override
		public int readPixel(int u, int v) {
			return buffer[v * width + u];
		}
	}
}
