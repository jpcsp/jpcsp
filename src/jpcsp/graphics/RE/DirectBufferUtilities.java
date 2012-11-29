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

import static jpcsp.util.Utilities.round2;
import static jpcsp.util.Utilities.round4;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import jpcsp.graphics.VideoEngine;

public class DirectBufferUtilities {
	protected static int directBufferSize = 100;
	protected static ByteBuffer directBuffer = ByteBuffer.allocateDirect(directBufferSize).order(ByteOrder.nativeOrder());
	protected static FloatBuffer directFloatBuffer = ByteBuffer.allocateDirect(128 * VideoEngine.SIZEOF_FLOAT).order(ByteOrder.nativeOrder()).asFloatBuffer();

	public static FloatBuffer getDirectBuffer(float[] values) {
		return getDirectBuffer(values, values.length);
	}

	public static FloatBuffer getDirectBuffer(float[] values, int length) {
		directFloatBuffer.clear();
		directFloatBuffer.limit(length);
		directFloatBuffer.put(values, 0, length);
		directFloatBuffer.rewind();

		return directFloatBuffer;
	}

	public static IntBuffer getDirectBuffer(int size, IntBuffer buffer) {
		if (buffer == null) {
			return buffer;
		}

		size = round4(size);

		if (buffer.isDirect()) {
			buffer.limit((size >> 2) + buffer.position());
			return buffer;
		}

		IntBuffer directBuffer = allocateDirectBuffer(size).asIntBuffer();
		directBuffer.put((IntBuffer) ((IntBuffer) buffer).slice().limit(size >> 2));
		directBuffer.rewind();

		return directBuffer;
	}

	public static FloatBuffer getDirectBuffer(int size, FloatBuffer buffer) {
		if (buffer == null) {
			return buffer;
		}

		size = round4(size);

		if (buffer.isDirect()) {
			buffer.limit((size >> 2) + buffer.position());
			return buffer;
		}

		FloatBuffer directBuffer = allocateDirectBuffer(size).asFloatBuffer();
		directBuffer.put((FloatBuffer) ((FloatBuffer) buffer).slice().limit(size >> 2));
		directBuffer.rewind();

		return directBuffer;
	}

	public static ShortBuffer getDirectBuffer(int size, ShortBuffer buffer) {
		if (buffer == null) {
			return buffer;
		}

		size = round2(size);

		if (buffer.isDirect()) {
			buffer.limit((size >> 1) + buffer.position());
			return buffer;
		}

		ShortBuffer directBuffer = allocateDirectBuffer(size).asShortBuffer();
		directBuffer.put((ShortBuffer) ((ShortBuffer) buffer).slice().limit(size >> 1));
		directBuffer.rewind();

		return directBuffer;
	}

	public static ByteBuffer getDirectBuffer(int size, ByteBuffer buffer) {
		if (buffer == null) {
			return buffer;
		}

		if (buffer.isDirect()) {
			buffer.limit(size + buffer.position());
			return buffer;
		}

		ByteBuffer directBuffer = allocateDirectBuffer(size);
		directBuffer.put((ByteBuffer) ((ByteBuffer) buffer).slice().limit(size));
		directBuffer.rewind();

		return directBuffer;
	}

	private static ByteBuffer getByteBuffer(int size, Buffer buffer, int bufferOffset) {
		if (buffer instanceof ByteBuffer) {
			buffer.limit(size);
			buffer.position(bufferOffset);
			return (ByteBuffer) buffer;
		} else if (buffer instanceof IntBuffer) {
			size = round4(size);
			ByteBuffer directBuffer = allocateDirectBuffer(size);
			directBuffer.asIntBuffer().put((IntBuffer) ((IntBuffer) buffer).slice().limit(size >> 2));
			directBuffer.position(bufferOffset);
			return directBuffer;
		} else if (buffer instanceof ShortBuffer) {
			size = round2(size);
			ByteBuffer directBuffer = allocateDirectBuffer(size);
			directBuffer.asShortBuffer().put((ShortBuffer) ((ShortBuffer) buffer).slice().limit(size >> 1));
			directBuffer.position(bufferOffset);
			return directBuffer;
		} else if (buffer instanceof FloatBuffer) {
			size = round4(size);
			ByteBuffer directBuffer = allocateDirectBuffer(size);
			directBuffer.asFloatBuffer().put((FloatBuffer) ((FloatBuffer) buffer).slice().limit(size >> 2));
			directBuffer.position(bufferOffset);
			return directBuffer;
		}

		throw new IllegalArgumentException();
	}

	public static ByteBuffer getDirectByteBuffer(int size, Buffer buffer, int bufferOffset) {
		return getDirectBuffer(size, getByteBuffer(size, buffer, bufferOffset));
	}

	public static FloatBuffer getDirectFloatBuffer(int size, Buffer buffer, int bufferOffset) {
		return getDirectBuffer(size, getByteBuffer(size, buffer, bufferOffset).asFloatBuffer());
	}

	public static IntBuffer getDirectIntBuffer(int size, Buffer buffer, int bufferOffset) {
		return getDirectBuffer(size, getByteBuffer(size, buffer, bufferOffset).asIntBuffer());
	}

	public static ShortBuffer getDirectShortBuffer(int size, Buffer buffer, int bufferOffset) {
		return getDirectBuffer(size, getByteBuffer(size, buffer, bufferOffset).asShortBuffer());
	}

	public static ByteBuffer allocateDirectBuffer(int size) {
		if (size > directBufferSize) {
			directBufferSize = size;
			directBuffer = ByteBuffer.allocateDirect(directBufferSize).order(ByteOrder.nativeOrder());
		}

		directBuffer.clear();
		directBuffer.limit(size);

		return directBuffer;
	}

	public static ByteBuffer allocateDirectBuffer(ByteBuffer buffer) {
		if (buffer.isDirect()) {
			return buffer;
		}

		return allocateDirectBuffer(buffer.remaining());
	}

	public static IntBuffer allocateDirectBuffer(IntBuffer buffer) {
		if (buffer.isDirect()) {
			return buffer;
		}

		return allocateDirectBuffer(buffer.remaining() << 2).asIntBuffer();
	}

	public static ShortBuffer allocateDirectBuffer(ShortBuffer buffer) {
		if (buffer.isDirect()) {
			return buffer;
		}

		return allocateDirectBuffer(buffer.remaining() << 1).asShortBuffer();
	}

	public static FloatBuffer allocateDirectBuffer(FloatBuffer buffer) {
		if (buffer.isDirect()) {
			return buffer;
		}

		return allocateDirectBuffer(buffer.remaining() << 2).asFloatBuffer();
	}

	public static void copyBuffer(ByteBuffer dstBuffer, ByteBuffer srcBuffer) {
		if (dstBuffer != srcBuffer) {
			srcBuffer.rewind();
			dstBuffer.put(srcBuffer);
		}
	}

	public static void copyBuffer(IntBuffer dstBuffer, IntBuffer srcBuffer) {
		if (dstBuffer != srcBuffer) {
			srcBuffer.rewind();
			dstBuffer.put(srcBuffer);
		}
	}

	public static void copyBuffer(ShortBuffer dstBuffer, ShortBuffer srcBuffer) {
		if (dstBuffer != srcBuffer) {
			srcBuffer.rewind();
			dstBuffer.put(srcBuffer);
		}
	}

	public static void copyBuffer(FloatBuffer dstBuffer, FloatBuffer srcBuffer) {
		if (dstBuffer != srcBuffer) {
			srcBuffer.rewind();
			dstBuffer.put(srcBuffer);
		}
	}
}
