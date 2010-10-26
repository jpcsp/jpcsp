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
import java.util.Hashtable;

import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public abstract class BaseBufferManager implements IREBufferManager {
	protected final static Logger log = VideoEngine.log;
	protected IRenderingEngine re;
	protected Hashtable<Integer, BufferInfo> buffers;
	protected static final int[] sizeOfType = new int[] {
		1, // RE_BYTE
		1, // RE_UNSIGNED_BYTE
		2, // RE_SHORT
		2, // RE_UNSIGNED_SHORT
		4, // RE_INT
		4, // RE_UNSIGNED_INT
		4, // RE_FLOAT
		8  // RE_DOUBLE
	};

	protected static class BufferInfo {
		public int buffer;
		public ByteBuffer byteBuffer;
		public Buffer typedBuffer;
		public int type;
		public int size;
		public int usage;
		public int elementSize;
		public int totalSize;

		public BufferInfo(int buffer, ByteBuffer byteBuffer, int type, int size) {
			this.buffer = buffer;
			this.byteBuffer = byteBuffer;
			this.type = type;
			this.size = size;
			elementSize = sizeOfType[type];
			totalSize = size * elementSize;

			typedBuffer = byteBuffer;
			switch (type) {
				case IRenderingEngine.RE_BYTE:
				case IRenderingEngine.RE_UNSIGNED_BYTE:
					break;
				case IRenderingEngine.RE_SHORT:
				case IRenderingEngine.RE_UNSIGNED_SHORT:
					typedBuffer = byteBuffer.asShortBuffer();
					break;
				case IRenderingEngine.RE_INT:
				case IRenderingEngine.RE_UNSIGNED_INT:
					typedBuffer = byteBuffer.asIntBuffer();
					break;
				case IRenderingEngine.RE_FLOAT:
					typedBuffer = byteBuffer.asFloatBuffer();
					break;
				case IRenderingEngine.RE_DOUBLE:
					typedBuffer = byteBuffer.asDoubleBuffer();
					break;
			}
		}

		public Buffer getBufferPosition(int offset) {
			return typedBuffer.position(offset / elementSize);
		}

		public int getBufferSize() {
			return totalSize;
		}
	}

	public BaseBufferManager() {
		init();
	}

	protected void init() {
		buffers = new Hashtable<Integer, BufferInfo>();
	}

	@Override
	public void setRenderingEngine(IRenderingEngine re) {
		this.re = re;
	}

	protected ByteBuffer createByteBuffer(int size) {
		return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
	}

	@Override
	public void deleteBuffer(int buffer) {
		buffers.remove(buffer);
	}

	@Override
	public ByteBuffer getBuffer(int buffer) {
		return buffers.get(buffer).byteBuffer;
	}
}
