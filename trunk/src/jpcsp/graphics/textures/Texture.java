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
import jpcsp.util.Hash;

public class Texture {
	private int addr;
	private int lineWidth;
	private int width;
	private int height;
	private int pixelStorage;
	private int clutAddr;
	private int clutMode;
	private int clutStart;
	private int clutShift;
	private int clutMask;
	private int clutNumBlocks;
	private int hashCode;
	private int mipmapLevels;
	private boolean mipmapShareClut;
	private int textureId = -1;	// id created by genTexture
	private boolean loaded = false;	// is the texture already loaded?
	private TextureCache textureCache;
	private final static int hashStride = 64 + 8;
	private short[] cachedValues16;
	private int[] cachedValues32;

	public Texture(TextureCache textureCache, int addr, int lineWidth, int width, int height, int pixelStorage, int clutAddr, int clutMode, int clutStart, int clutShift, int clutMask, int clutNumBlocks, int mipmapLevels, boolean mipmapShareClut, short[] values16, int[] values32) {
		this.textureCache = textureCache;
		this.addr = addr;
		this.lineWidth = lineWidth;
		this.width = width;
		this.height = height;
		this.pixelStorage = pixelStorage;
		this.clutAddr = clutAddr;
		this.clutMode = clutMode;
		this.clutStart = clutStart;
		this.clutShift = clutShift;
		this.clutMask = clutMask;
		this.clutNumBlocks = clutNumBlocks;
		this.mipmapLevels = mipmapLevels;
		this.mipmapShareClut = mipmapShareClut;

		if (values16 != null) {
			cachedValues16 = new short[lineWidth];
			System.arraycopy(values16, 0, cachedValues16, 0, lineWidth);
		} else if (values32 != null) {
			cachedValues32 = new int[lineWidth];
			System.arraycopy(values32, 0, cachedValues32, 0, lineWidth);
		} else {
			hashCode = hashCode(addr, lineWidth, width, height, pixelStorage, clutAddr, clutMode, clutStart, clutShift, clutMask, clutNumBlocks, mipmapLevels);
		}
	}

	/**
	 * Compute the Texture hashCode value,
	 * based on the pixel buffer and the clut table.
	 *
	 * @param addr          pixel buffer
	 * @param lineWidth     texture buffer width
	 * @param width         texture width
	 * @param height        texture height
	 * @param pixelStorage  texture storage
	 * @param clutAddr      clut table address
	 * @param clutMode      clut mode
	 * @param clutStart     clut start
	 * @param clutShift     clut shift
	 * @param clutMask      clut mask
	 * @param clutNumBlocks clut number of blocks
	 * @param mipmapLevels  number of mipmaps
	 * @return              hashcode value
	 */
	private static int hashCode(int addr, int lineWidth, int width, int height, int pixelStorage, int clutAddr, int clutMode, int clutStart, int clutShift, int clutMask, int clutNumBlocks, int mipmapLevels) {
		int hashCode = mipmapLevels;

		if (addr != 0) {
			int bufferLengthInBytes = lineWidth * height;
			int lineWidthInBytes = lineWidth;
			switch (pixelStorage) {
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED:
					bufferLengthInBytes *= 2;
					lineWidthInBytes *= 2;
					break;
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888:
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED:
					bufferLengthInBytes *= 4;
					lineWidthInBytes *= 4;
					break;
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT1:
					bufferLengthInBytes = VideoEngine.getCompressedTextureSize(lineWidth, height, 8);
					break;
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT3:
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT5:
					bufferLengthInBytes = VideoEngine.getCompressedTextureSize(lineWidth, height, 4);
					break;
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED:
					bufferLengthInBytes /= 2;
					lineWidthInBytes /= 2;
					break;
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED:
					break;
			}
			if (VideoEngine.log.isDebugEnabled()) {
				VideoEngine.log.debug("Texture.hashCode: " + bufferLengthInBytes + " bytes");
			}

			int strideInBytes = hashStride;
			if (lineWidthInBytes < hashStride) {
				if (lineWidthInBytes <= 32) {
					// No stride at all for narrow textures
					strideInBytes = 0;
				} else {
					strideInBytes = lineWidthInBytes - 4;
				}
			}
			hashCode = Hash.getHashCode(hashCode, addr, bufferLengthInBytes, strideInBytes);
		}

		if (clutAddr != 0) {
			hashCode = Hash.getHashCode(hashCode, clutAddr, clutNumBlocks * 32);
		}

		return hashCode;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	public boolean equals(int addr, int lineWidth, int width, int height, int pixelStorage, int clutAddr, int clutMode, int clutStart, int clutShift, int clutMask, int clutNumBlocks, int mipmapLevels, boolean mipmapShareClut, short[] values16, int[] values32) {
		if (this.addr != addr ||
			this.lineWidth != lineWidth ||
			this.width != width ||
			this.height != height ||
			this.pixelStorage != pixelStorage ||
			this.clutAddr != clutAddr ||
			this.clutMode != clutMode ||
			this.clutStart != clutStart ||
			this.clutShift != clutShift ||
			this.clutMask != clutMask ||
			this.clutNumBlocks != clutNumBlocks ||
			this.mipmapLevels != mipmapLevels ||
			this.mipmapShareClut != mipmapShareClut)
		{
			return false;
		}

		// Do not compute the hashCode of the new texture if it has already
		// been checked during this display cycle
		if (!textureCache.textureAlreadyHashed(addr, clutAddr)) {
			if (values16 != null) {
				return equals(values16);
			}
			if (values32 != null) {
				return equals(values32);
			}
			int hashCode = hashCode(addr, lineWidth, width, height, pixelStorage, clutAddr, clutMode, clutStart, clutShift, clutMask, clutNumBlocks, mipmapLevels);
			if (hashCode != hashCode()) {
				return false;
			}
			textureCache.setTextureAlreadyHashed(addr, clutAddr);
		}

		return true;
	}

	private boolean equals(short[] values16) {
		if (cachedValues16 == null) {
			return false;
		}

		for (int i = 0; i < lineWidth; i++) {
			if (values16[i] != cachedValues16[i]) {
				return false;
			}
		}

		return true;
	}

	private boolean equals(int[] values32) {
		if (cachedValues32 == null) {
			return false;
		}

		for (int i = 0; i < lineWidth; i++) {
			if (values32[i] != cachedValues32[i]) {
				return false;
			}
		}

		return true;
	}

	public void bindTexture(IRenderingEngine re) {
		if (textureId == -1) {
			textureId = re.genTexture();
		}

		re.bindTexture(textureId);
	}

	public void deleteTexture(IRenderingEngine re) {
		if (textureId != -1) {
			re.deleteTexture(textureId);
            textureId = -1;
		}

		setLoaded(false);
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setIsLoaded() {
		setLoaded(true);
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public int getAddr() {
		return addr;
	}

	public int getClutAddr() {
		return clutAddr;
	}

	public int getGlId() {
		return textureId;
	}
	
	public int getMipmapLevels() {
		return mipmapLevels;
	}
}
