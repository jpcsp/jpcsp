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

import static jpcsp.graphics.GeCommands.CMODE_FORMAT_16BIT_ABGR4444;
import static jpcsp.graphics.GeCommands.CMODE_FORMAT_16BIT_ABGR5551;
import static jpcsp.graphics.GeCommands.CMODE_FORMAT_16BIT_BGR5650;
import static jpcsp.graphics.GeCommands.CMODE_FORMAT_32BIT_ABGR8888;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED;
import static jpcsp.memory.ImageReader.color4444to8888;
import static jpcsp.memory.ImageReader.color5551to8888;
import static jpcsp.memory.ImageReader.color565to8888;
import static jpcsp.util.Utilities.getPower2;
import jpcsp.Memory;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.memory.FastMemory;
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
	protected final int offset;
	protected int[] buffer;
	protected static int[] memAll;
	protected boolean useTextureClut;
	protected int[] clut;

	static {
		if (Memory.getInstance() instanceof FastMemory) {
			memAll = ((FastMemory) Memory.getInstance()).getAll();
		}
	}

	public static CachedTexture getCachedTexture(int width, int height, int pixelFormat, int[] buffer, int bufferOffset, int bufferLength) {
		int offset = 0;
		// When the texture is directly available from the memory,
		// we can reuse the memory array and do not need to copy the whole texture.
		if (pixelFormat == TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888 && buffer == memAll) {
			offset = bufferOffset;
		}

		CachedTexture cachedTexture = getCachedTexture(width, height, pixelFormat, offset);
		cachedTexture.setBuffer(buffer, bufferOffset, bufferLength);

		return cachedTexture;
	}

	public static CachedTexture getCachedTexture(int width, int height, int pixelFormat, short[] buffer, int bufferOffset, int bufferLength) {
		CachedTexture cachedTexture = getCachedTexture(width, height, pixelFormat, 0);
		cachedTexture.setBuffer(buffer, bufferOffset, bufferLength);

		return cachedTexture;
	}

	private static CachedTexture getCachedTexture(int width, int height, int pixelFormat, int offset) {
		CachedTexture cachedTexture;

		if (IRenderingEngine.isTextureTypeIndexed[pixelFormat]) {
			switch (pixelFormat) {
				case TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED:
					cachedTexture = new CachedTextureIndexed8Bit(width, height, pixelFormat, offset);
					break;
				case TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED:
					cachedTexture = new CachedTextureIndexed16Bit(width, height, pixelFormat, offset);
					break;
				case TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED:
					cachedTexture = new CachedTextureIndexed32Bit(width, height, pixelFormat, offset);
					break;
				default:
					VideoEngine.log.error(String.format("CachedTexture: unsupported indexed texture format %d", pixelFormat));
					return null;
			}
			cachedTexture.setClut();
		} else if (width == Utilities.makePow2(width)) {
			if (offset == 0) {
				cachedTexture = new CachedTexturePow2(width, height, pixelFormat);
			} else {
				cachedTexture = new CachedTextureOffsetPow2(width, height, pixelFormat, offset);
			}
		} else {
			if (offset == 0) {
				cachedTexture = new CachedTextureNonPow2(width, height, pixelFormat);
			} else {
				cachedTexture = new CachedTextureOffsetNonPow2(width, height, pixelFormat, offset);
			}
		}

		return cachedTexture;
	}

	protected CachedTexture(int width, int height, int pixelFormat, int offset) {
		this.width = width;
		this.height = height;
		this.pixelFormat = pixelFormat;
		this.offset = offset;
		widthPower2 = getPower2(width);
		heightPower2 = getPower2(height);
	}

	protected void setBuffer(int[] buffer, int bufferOffset, int bufferLength) {
		switch (pixelFormat) {
			case TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
				this.buffer = new int[bufferLength * 2];
				for (int i = 0, j = 0; i < bufferLength; i++) {
					int color = buffer[bufferOffset + i];
					this.buffer[j++] = color565to8888(color & 0xFFFF);
					this.buffer[j++] = color565to8888(color >>> 16);
				}
				break;
			case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
				this.buffer = new int[bufferLength * 2];
				for (int i = 0, j = 0; i < bufferLength; i++) {
					int color = buffer[bufferOffset + i];
					this.buffer[j++] = color5551to8888(color & 0xFFFF);
					this.buffer[j++] = color5551to8888(color >>> 16);
				}
				break;
			case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
				this.buffer = new int[bufferLength * 2];
				for (int i = 0, j = 0; i < bufferLength; i++) {
					int color = buffer[bufferOffset + i];
					this.buffer[j++] = color4444to8888(color & 0xFFFF);
					this.buffer[j++] = color4444to8888(color >>> 16);
				}
				break;
			case TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888:
			case TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED:
			case TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED:
			case TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED:
				// Is the texture directly available from the memory array?
				if (buffer == memAll && offset == bufferOffset) {
					// We do not need to copy the whole texture, we can reuse the memory array
					this.buffer = buffer;
				} else {
					this.buffer = new int[bufferLength];
					System.arraycopy(buffer, bufferOffset, this.buffer, 0, bufferLength);
				}
				break;
			default:
				VideoEngine.log.error(String.format("CachedTexture setBuffer int unsupported pixel format %d", pixelFormat));
				break;
		}
	}

	protected void setClut() {
	}

	protected void setBuffer(short[] buffer, int bufferOffset, int bufferLength) {
		switch (pixelFormat) {
			case TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
				this.buffer = new int[bufferLength];
				for (int i = 0; i < bufferLength; i++) {
					this.buffer[i] = color565to8888(buffer[bufferOffset + i] & 0xFFFF);
				}
				break;
			case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
				this.buffer = new int[bufferLength];
				for (int i = 0; i < bufferLength; i++) {
					this.buffer[i] = color5551to8888(buffer[bufferOffset + i] & 0xFFFF);
				}
				break;
			case TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
				this.buffer = new int[bufferLength];
				for (int i = 0; i < bufferLength; i++) {
					this.buffer[i] = color4444to8888(buffer[bufferOffset + i] & 0xFFFF);
				}
				break;
			case TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888:
				this.buffer = new int[bufferLength / 2];
				for (int i = 0, j = bufferOffset; i < this.buffer.length; i++) {
					this.buffer[i] = (buffer[j++] & 0xFFFF) | (buffer[j++] << 16);
				}
			default:
				VideoEngine.log.error(String.format("CachedTexture setBuffer short unsupported pixel format %d", pixelFormat));
				break;
		}
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
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
			super(width, height, pixelFormat, 0);
		}

		@Override
		public int readPixel(int u, int v) {
			return buffer[(v << widthPower2) + u];
		}
	}

	/**
	 * @author gid15
	 *
	 * A specialized class when the width is a power of 2 (faster)
	 * and using an array offset.
	 */
	private static class CachedTextureOffsetPow2 extends CachedTexture {
		public CachedTextureOffsetPow2(int width, int height, int pixelFormat, int offset) {
			super(width, height, pixelFormat, offset);
		}

		@Override
		public int readPixel(int u, int v) {
			return buffer[(v << widthPower2) + u + offset];
		}
	}

	/**
	 * @author gid15
	 *
	 * A specialized class when the width is not a power of 2.
	 */
	private static class CachedTextureNonPow2 extends CachedTexture {
		public CachedTextureNonPow2(int width, int height, int pixelFormat) {
			super(width, height, pixelFormat, 0);
		}

		@Override
		public int readPixel(int u, int v) {
			return buffer[v * width + u];
		}
	}

	/**
	 * @author gid15
	 *
	 * A specialized class when the width is not a power of 2
	 * and using an array offset.
	 */
	private static class CachedTextureOffsetNonPow2 extends CachedTexture {
		public CachedTextureOffsetNonPow2(int width, int height, int pixelFormat, int offset) {
			super(width, height, pixelFormat, offset);
		}

		@Override
		public int readPixel(int u, int v) {
			return buffer[v * width + u + offset];
		}
	}

	private static abstract class CachedTextureIndexed extends CachedTexture {
		private int[] clut;
		private int shift;
		private int mask;
		private int start;

		protected CachedTextureIndexed(int width, int height, int pixelFormat, int offset) {
			super(width, height, pixelFormat, offset);
		}

		protected int getClut(int index) {
			return clut[((index >> shift) & mask) | (start << 4)];
		}

		public void setClut(int[] clut, int shift, int mask, int start) {
			this.clut = clut;
			this.shift = shift;
			this.mask = mask;
			this.start = start;
		}

		@Override
		public void setClut() {
			VideoEngine videoEngine = VideoEngine.getInstance();
			GeContext context = videoEngine.getContext();
			int clutNumEntries = videoEngine.getClutNumEntries();
			int[] clut = null;
			short[] shortClut;
			switch (context.tex_clut_mode) {
				case CMODE_FORMAT_16BIT_BGR5650:
					shortClut = videoEngine.readClut16(0);
					clut = new int[clutNumEntries];
					for (int i = 0; i < clut.length; i++) {
						clut[i] = color565to8888(shortClut[i]);
					}
					break;
				case CMODE_FORMAT_16BIT_ABGR5551:
					shortClut = videoEngine.readClut16(0);
					clut = new int[clutNumEntries];
					for (int i = 0; i < clut.length; i++) {
						clut[i] = color5551to8888(shortClut[i]);
					}
					break;
				case CMODE_FORMAT_16BIT_ABGR4444:
					shortClut = videoEngine.readClut16(0);
					clut = new int[clutNumEntries];
					for (int i = 0; i < clut.length; i++) {
						clut[i] = color4444to8888(shortClut[i]);
					}
					break;
				case CMODE_FORMAT_32BIT_ABGR8888:
					int[] intClut = videoEngine.readClut32(0);
					clut = new int[clutNumEntries];
					System.arraycopy(intClut, 0, clut, 0, clut.length);
					break;
			}

			if (clut != null) {
				setClut(clut, context.tex_clut_shift, context.tex_clut_mask, context.tex_clut_start);
			}
		}
	}

	private static class CachedTextureIndexed8Bit extends CachedTextureIndexed {
		private static final int[] shift8Bit = new int[] { 0, 8, 16, 24 };

		protected CachedTextureIndexed8Bit(int width, int height, int pixelFormat, int offset) {
			super(width, height, pixelFormat, offset);
		}

		@Override
		public int readPixel(int u, int v) {
			int pixelIndex = v * width + u;
			int index = buffer[(pixelIndex >> 2) + offset] >> shift8Bit[pixelIndex & 3];
			return getClut(index & 0xFF);
		}
	}

	private static class CachedTextureIndexed16Bit extends CachedTextureIndexed {
		private static final int[] shift16Bit = new int[] { 0, 16 };

		protected CachedTextureIndexed16Bit(int width, int height, int pixelFormat, int offset) {
			super(width, height, pixelFormat, offset);
		}

		@Override
		public int readPixel(int u, int v) {
			int pixelIndex = v * width + u;
			int index = buffer[(pixelIndex >> 1) + offset] >> shift16Bit[pixelIndex & 1];
			return getClut(index & 0xFFFF);
		}
	}

	private static class CachedTextureIndexed32Bit extends CachedTextureIndexed {
		protected CachedTextureIndexed32Bit(int width, int height, int pixelFormat, int offset) {
			super(width, height, pixelFormat, offset);
		}

		@Override
		public int readPixel(int u, int v) {
			int index = buffer[v * width + u + offset];
			return getClut(index);
		}
	}
}
