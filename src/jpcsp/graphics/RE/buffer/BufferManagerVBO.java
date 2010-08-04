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

import java.nio.Buffer;
import java.nio.ByteBuffer;

import jpcsp.Settings;
import jpcsp.graphics.RE.IRenderingEngine;

/**
 * @author gid15
 *
 */
public class BufferManagerVBO extends BaseBufferManager {
	public static boolean useVBO(IRenderingEngine re) {
        return !Settings.getInstance().readBool("emu.disablevbo")
             && re.isFunctionAvailable("glGenBuffersARB")
             && re.isFunctionAvailable("glBindBufferARB")
             && re.isFunctionAvailable("glBufferDataARB")
             && re.isFunctionAvailable("glDeleteBuffersARB")
             && re.isFunctionAvailable("glGenBuffers");
	}

	protected void init() {
		super.init();
    	log.info("Using VBO");
	}

	@Override
	public boolean useVBO() {
		return true;
	}

	@Override
	public int genBuffer(int type, int size, int usage) {
		int totalSize = size * sizeOfType[type];
		ByteBuffer byteBuffer = createByteBuffer(totalSize);

		int buffer = re.genBuffer();
		re.bindBuffer(buffer);
		re.setBufferData(totalSize, byteBuffer, usage);

		buffers.put(buffer, new BufferInfo(buffer, byteBuffer, type, size));

		return buffer;
	}

	@Override
	public void bindBuffer(int buffer) {
		re.bindBuffer(buffer);
	}

	@Override
	public void deleteBuffer(int buffer) {
		re.deleteBuffer(buffer);
		super.deleteBuffer(buffer);
	}

	@Override
	public void setColorPointer(int buffer, int size, int type, int stride, int offset) {
		bindBuffer(buffer);
		re.setColorPointer(size, type, stride, offset);
	}

	@Override
	public void setNormalPointer(int buffer, int type, int stride, int offset) {
		bindBuffer(buffer);
		re.setNormalPointer(type, stride, offset);
	}

	@Override
	public void setTexCoordPointer(int buffer, int size, int type, int stride, int offset) {
		bindBuffer(buffer);
		re.setTexCoordPointer(size, type, stride, offset);
	}

	@Override
	public void setVertexAttribPointer(int buffer, int id, int size, int type, boolean normalized, int stride, int offset) {
		bindBuffer(buffer);
		re.setVertexAttribPointer(id, size, type, normalized, stride, offset);
	}

	@Override
	public void setVertexPointer(int buffer, int size, int type, int stride, int offset) {
		bindBuffer(buffer);
		re.setVertexPointer(size, type, stride, offset);
	}

	@Override
	public void setWeightPointer(int buffer, int size, int type, int stride, int offset) {
		bindBuffer(buffer);
		re.setWeightPointer(size, type, stride, offset);
	}

	@Override
	public void setBufferData(int buffer, int size, Buffer data, int usage) {
		re.bindBuffer(buffer);
		re.setBufferData(size, data, usage);
	}
}
