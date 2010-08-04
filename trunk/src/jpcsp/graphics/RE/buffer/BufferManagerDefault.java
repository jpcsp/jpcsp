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
import java.nio.ByteOrder;

import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class BufferManagerDefault extends BaseBufferManager {
	protected int currentBufferId;

	protected void init() {
		super.init();
		currentBufferId = 12345678;
	}

	@Override
	public boolean useVBO() {
		return false;
	}

	@Override
	public int genBuffer(int type, int size, int usage) {
		int totalSize = size * sizeOfType[type];
		ByteBuffer byteBuffer = createByteBuffer(totalSize);

		int buffer = currentBufferId++;

		buffers.put(buffer, new BufferInfo(buffer, byteBuffer, type, size));

		return buffer;
	}

	@Override
	public void bindBuffer(int buffer) {
		// Not supported
	}

	@Override
	public void setColorPointer(int buffer, int size, int type, int stride, int offset) {
		re.setColorPointer(size, type, stride, buffers.get(buffer).getBufferPosition(offset));
	}

	@Override
	public void setNormalPointer(int buffer, int type, int stride, int offset) {
		re.setNormalPointer(type, stride, buffers.get(buffer).getBufferPosition(offset));
	}

	@Override
	public void setTexCoordPointer(int buffer, int size, int type, int stride, int offset) {
		re.setTexCoordPointer(size, type, stride, buffers.get(buffer).getBufferPosition(offset));
	}

	@Override
	public void setVertexAttribPointer(int buffer, int id, int size, int type, boolean normalized, int stride, int offset) {
		re.setVertexAttribPointer(id, size, type, normalized, stride, buffers.get(buffer).getBufferPosition(offset));
	}

	@Override
	public void setVertexPointer(int buffer, int size, int type, int stride, int offset) {
		re.setVertexPointer(size, type, stride, buffers.get(buffer).getBufferPosition(offset));
	}

	@Override
	public void setWeightPointer(int buffer, int size, int type, int stride, int offset) {
		re.setWeightPointer(size, type, stride, buffers.get(buffer).getBufferPosition(offset));
	}

	@Override
	public void setBufferData(int buffer, int size, Buffer data, int usage) {
		BufferInfo bufferInfo = buffers.get(buffer);
		if (bufferInfo.byteBuffer != data) {
			bufferInfo.byteBuffer.clear();
			Utilities.putBuffer(bufferInfo.byteBuffer, data, ByteOrder.nativeOrder());
		} else {
			bufferInfo.byteBuffer.position(0);
		}
	}
}
