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

import javax.media.opengl.GL;

/**
 * @author gid15
 *
 * A RenderingEngine implementing calls to OpenGL using jogl
 * for OpenGL Version >= 1.2.
 * The class contains no rendering logic, it just implements the interface to jogl.
 */
public class RenderingEngineJogl12 extends RenderingEngineJogl {
	protected static final int[] textureTypeToGL = {
        GL.GL_UNSIGNED_SHORT_5_6_5_REV,      // TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
        GL.GL_UNSIGNED_SHORT_1_5_5_5_REV,    // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
        GL.GL_UNSIGNED_SHORT_4_4_4_4_REV,    // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
        GL.GL_UNSIGNED_INT_8_8_8_8_REV,      // TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
        0,                                   // TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
        GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT,  // TPSM_PIXEL_STORAGE_MODE_DXT1
        GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, // TPSM_PIXEL_STORAGE_MODE_DXT3
        GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT  // TPSM_PIXEL_STORAGE_MODE_DXT5
	};

	public RenderingEngineJogl12(GL gl) {
		super(gl);
	}
}
