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

import javax.media.opengl.GL;

import jpcsp.Memory;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;

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
	private boolean hashCodeComputed = false;
	private int glId = -1;	// id created by glGenTextures
	private boolean loaded = false;	// is the texture already loaded?

	public Texture(int addr, int lineWidth, int width, int height, int pixelStorage, int clutAddr, int clutMode, int clutStart, int clutShift, int clutMask, int clutNumBlocks, int mipmapLevels) {
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
	}

	private static int hashCode(int addr, int lineWidth, int width, int height, int pixelStorage, int clutAddr, int clutMode, int clutStart, int clutShift, int clutMask, int clutNumBlocks, int mipmapLevels) {
	    //
		// HashCode is computed as follows:
	    // - XOR of pixel buffer
	    // - XOR of clut table
	    //
	    // Rem: to detect simple circular rotation of the values
	    // (e.g. rotating the clut entries like in blend.pbp),
	    // the address index (i) is added to the value itself.
	    //
		int hashCode = mipmapLevels;
		Memory mem = Memory.getInstance();

		if (addr != 0) {
			int bufferLengthInBytes = lineWidth * height;
			switch (pixelStorage) {
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551:
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444:
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED:
					bufferLengthInBytes = bufferLengthInBytes * 2;
					break;
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888:
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED:
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT1:
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT3:
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_DXT5:
					bufferLengthInBytes = bufferLengthInBytes * 4;
					break;
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED:
					bufferLengthInBytes = bufferLengthInBytes / 2;
					break;
				case GeCommands.TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED:
					break;
			}
			VideoEngine.log.debug("Texture.hashCode: " + bufferLengthInBytes + " bytes");

			for (int i = 0; i < bufferLengthInBytes; i += 4) {
				hashCode ^= mem.read32(addr + i) + i;
			}
		}

		if (clutAddr != 0) {
			if (clutMode == GeCommands.CMODE_FORMAT_32BIT_ABGR8888) {
				int clutNumEntries = clutNumBlocks * 8;
				for (int i = clutStart; i < clutNumEntries; i++) {
					hashCode ^= mem.read32(clutAddr + i * 4) + i;
				}
			} else {
				int clutNumEntries = clutNumBlocks * 16;
				for (int i = clutStart; i < clutNumEntries; i++) {
					hashCode ^= mem.read16(clutAddr + i * 2) + i;
				}
			}
		}

		return hashCode;
	}

	@Override
	public int hashCode() {
		if (!hashCodeComputed) {
			hashCode = hashCode(addr, lineWidth, width, height, pixelStorage, clutAddr, clutMode, clutStart, clutShift, clutMask, clutNumBlocks, mipmapLevels);
			hashCodeComputed = true;
		}

		return hashCode;
	}

	public boolean equals(int addr, int lineWidth, int width, int height, int pixelStorage, int clutAddr, int clutMode, int clutStart, int clutShift, int clutMask, int clutNumBlocks, int mipmapLevels) {
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
			this.mipmapLevels != mipmapLevels)
		{
			return false;
		}

		int hashCode = hashCode(addr, lineWidth, width, height, pixelStorage, clutAddr, clutMode, clutStart, clutShift, clutMask, clutNumBlocks, mipmapLevels);
		if (hashCode != hashCode()) {
			return false;
		}

		return true;
	}

	public void bindTexture(GL gl) {
		if (glId == -1) {
			int[] glIds = new int[1];
            gl.glGenTextures(glIds.length, glIds, 0);
            glId = glIds[0];
		}

        gl.glBindTexture(GL.GL_TEXTURE_2D, glId);
	}

	public void deleteTexture(GL gl) {
		if (glId != -1) {
			int[] glIds = new int[1];
			glIds[0] = glId;
            gl.glDeleteTextures(glIds.length, glIds, 0);
            glId = -1;
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

	public int getGlId() {
		return glId;
	}
	
	public int getMipmapLevels() {
		return mipmapLevels;
	}
}
