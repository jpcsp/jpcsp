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

import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * @author gid15
 *
 * A RenderingEngine implementing calls to OpenGL using LWJGL
 * for OpenGL Version >= 1.2.
 * The class contains no rendering logic, it just implements the interface to LWJGL.
 */
public class RenderingEngineLwjgl12 extends RenderingEngineLwjgl {
	protected static final int[] textureTypeToGL = {
        GL12.GL_UNSIGNED_SHORT_5_6_5_REV,      // TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
        GL12.GL_UNSIGNED_SHORT_1_5_5_5_REV,    // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
        GL12.GL_UNSIGNED_SHORT_4_4_4_4_REV,    // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
        GL12.GL_UNSIGNED_INT_8_8_8_8_REV,      // TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
        GL11.GL_UNSIGNED_BYTE,                 // TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
        GL11.GL_UNSIGNED_BYTE,                 // TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
        GL11.GL_UNSIGNED_SHORT,                // TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
        GL11.GL_UNSIGNED_INT,                  // TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
        EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT,  // TPSM_PIXEL_STORAGE_MODE_DXT1
        EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, // TPSM_PIXEL_STORAGE_MODE_DXT3
        EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT, // TPSM_PIXEL_STORAGE_MODE_DXT5
        GL12.GL_UNSIGNED_SHORT_5_6_5_REV,      // RE_PIXEL_STORAGE_16BIT_INDEXED_BGR5650
        GL12.GL_UNSIGNED_SHORT_1_5_5_5_REV,    // RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR5551
        GL12.GL_UNSIGNED_SHORT_4_4_4_4_REV,    // RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR4444
        GL12.GL_UNSIGNED_INT_8_8_8_8_REV       // RE_PIXEL_STORAGE_32BIT_INDEXED_ABGR8888
	};

	public RenderingEngineLwjgl12() {
	}
}
