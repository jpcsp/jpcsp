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

import java.nio.Buffer;

import javax.media.opengl.GL;

/**
 * @author gid15
 *
 * A RenderingEngine implementing calls to OpenGL using jogl
 * for OpenGL Version >= 1.5.
 * The class contains no rendering logic, it just implements the interface to jogl.
 */
public class RenderingEngineJogl15 extends RenderingEngineJogl12 {
	public RenderingEngineJogl15(GL gl) {
		super(gl);
	}

	@Override
	public void deleteBuffer(int buffer) {
		int[] buffers = new int[] { buffer };
		gl.glDeleteBuffers(1, buffers, 0);
	}

	@Override
	public int genBuffer() {
		int[] buffers = new int[1];
		gl.glGenBuffers(1, buffers, 0);
		return buffers[0];
	}

	@Override
	public void setBufferData(int size, Buffer buffer, int usage) {
		gl.glBufferData(bufferTarget, size, buffer, bufferUsageToGL[usage]);
	}

	@Override
	public void bindBuffer(int buffer) {
		gl.glBindBuffer(bufferTarget, buffer);
	}
}
