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

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

/**
 * @author gid15
 *
 * A RenderingEngine implementing calls to OpenGL using LWJGL
 * for OpenGL Version >= 3.1.
 * The class contains no rendering logic, it just implements the interface to LWJGL.
 */
public class RenderingEngineLwjgl31 extends RenderingEngineLwjgl15 {
	protected static final int[] bufferTargetToGL = {
		GL15.GL_ARRAY_BUFFER,         // RE_ARRAY_BUFFER
		GL31.GL_UNIFORM_BUFFER        // RE_UNIFORM_BUFFER
	};

	@Override
	public void bindBufferBase(int target, int bindingPoint, int buffer) {
		GL30.glBindBufferBase(bufferTargetToGL[target], bindingPoint, buffer);
	}

	@Override
	public int getUniformBlockIndex(int program, String name) {
		return GL31.glGetUniformBlockIndex(program, name);
	}

	@Override
	public void setUniformBlockBinding(int program, int blockIndex, int bindingPoint) {
		GL31.glUniformBlockBinding(program, blockIndex, bindingPoint);
	}
}
