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
package jpcsp.graphics.RE.buffer;

import static jpcsp.graphics.RE.IRenderingEngine.RE_ARRAY_BUFFER;
import static jpcsp.util.Utilities.round4;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.settings.Settings;

/**
 * @author gid15
 *
 */
public class BufferManagerVBO extends BaseBufferManager {
	private int currentBufferDataSize;

	public static boolean useVBO(IRenderingEngine re) {
        return !Settings.getInstance().readBool("emu.disablevbo")
            && re.isExtensionAvailable("GL_ARB_vertex_buffer_object");
	}

	@Override
	protected void init() {
		super.init();
    	log.info("Using VBO");
	}

	@Override
	public boolean useVBO() {
		return true;
	}

	@Override
	public int genBuffer(int target, int type, int size, int usage) {
		int totalSize = size * sizeOfType[type];
		ByteBuffer byteBuffer = createByteBuffer(totalSize);

		int buffer = re.genBuffer();
		setBufferData(target, buffer, totalSize, byteBuffer, usage);

		buffers.put(buffer, new BufferInfo(buffer, byteBuffer, type, size));

		return buffer;
	}

	@Override
	public void bindBuffer(int target, int buffer) {
		re.bindBuffer(target, buffer);
	}

	@Override
	public void deleteBuffer(int buffer) {
		re.deleteBuffer(buffer);
		super.deleteBuffer(buffer);
	}

	@Override
	public void setColorPointer(int buffer, int size, int type, int stride, int offset) {
		bindBuffer(RE_ARRAY_BUFFER, buffer);
		re.setColorPointer(size, type, stride, offset);
	}

	@Override
	public void setNormalPointer(int buffer, int type, int stride, int offset) {
		bindBuffer(RE_ARRAY_BUFFER, buffer);
		re.setNormalPointer(type, stride, offset);
	}

	@Override
	public void setTexCoordPointer(int buffer, int size, int type, int stride, int offset) {
		bindBuffer(RE_ARRAY_BUFFER, buffer);
		re.setTexCoordPointer(size, type, stride, offset);
	}

	@Override
	public void setVertexAttribPointer(int buffer, int id, int size, int type, boolean normalized, int stride, int offset) {
		bindBuffer(RE_ARRAY_BUFFER, buffer);
		re.setVertexAttribPointer(id, size, type, normalized, stride, offset);
	}

	@Override
	public void setVertexPointer(int buffer, int size, int type, int stride, int offset) {
		bindBuffer(RE_ARRAY_BUFFER, buffer);
		re.setVertexPointer(size, type, stride, offset);
	}

	@Override
	public void setWeightPointer(int buffer, int size, int type, int stride, int offset) {
		bindBuffer(RE_ARRAY_BUFFER, buffer);
		re.setWeightPointer(size, type, stride, offset);
	}

	@Override
	public void setBufferData(int target, int buffer, int size, Buffer data, int usage) {
		bindBuffer(target, buffer);
		re.setBufferData(target, size, data, usage);
		currentBufferDataSize = size;
	}

	@Override
	public void setBufferSubData(int target, int buffer, int offset, int size, Buffer data, int usage) {
		bindBuffer(target, buffer);

		// Some drivers seem to require an aligned buffer data size to handle correctly unaligned data.
		int requiredBufferDataSize = round4(offset) + round4(size);
		if (requiredBufferDataSize > currentBufferDataSize) {
			setBufferData(target, buffer, requiredBufferDataSize, null, usage);
		}

		re.setBufferSubData(target, offset, size, data);
	}
}
