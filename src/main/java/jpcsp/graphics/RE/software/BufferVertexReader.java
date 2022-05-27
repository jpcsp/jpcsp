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
package jpcsp.graphics.RE.software;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.apache.log4j.Logger;

import jpcsp.graphics.VertexState;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;

/**
 * @author gid15
 *
 */
public class BufferVertexReader {
	private static Logger log = VideoEngine.log;
	private ComponentInfo textureComponentInfo = new ComponentInfo();
	private ComponentInfo colorComponentInfo = new ComponentInfo();
	private ComponentInfo vertexComponentInfo = new ComponentInfo();
	private ComponentInfo normalComponentInfo = new ComponentInfo();
	private ComponentInfo weightComponentInfo = new ComponentInfo();

	public BufferVertexReader() {
	}

	public void setTextureComponentInfo(int size, int type, int stride, int bufferSize, Buffer buffer) {
		setComponentInfo(textureComponentInfo, size, type, stride, bufferSize, buffer);
	}

	public void setColorComponentInfo(int size, int type, int stride, int bufferSize, Buffer buffer) {
		setComponentInfo(colorComponentInfo, size, type, stride, bufferSize, buffer);
	}

	public void setVertexComponentInfo(int size, int type, int stride, int bufferSize, Buffer buffer) {
		setComponentInfo(vertexComponentInfo, size, type, stride, bufferSize, buffer);
	}

	public void setNormalComponentInfo(int type, int stride, int bufferSize, Buffer buffer) {
		setComponentInfo(normalComponentInfo, 3, type, stride, bufferSize, buffer);
	}

	public void setWeightComponentInfo(int size, int type, int stride, int bufferSize, Buffer buffer) {
		setComponentInfo(weightComponentInfo, size, type, stride, bufferSize, buffer);
	}

	private void setComponentInfo(ComponentInfo componentInfo, int size, int type, int stride, int bufferSize, Buffer buffer) {
		componentInfo.enabled = true;
		componentInfo.size = size;
		componentInfo.type = type;
		componentInfo.stride = stride;
		componentInfo.offset = buffer.position();
		componentInfo.buffer = buffer;
	}

	public void readVertex(int index, VertexState v) {
		readComponent(weightComponentInfo, index, v.boneWeights);
		readComponent(textureComponentInfo, index, v.t);
		readComponent(colorComponentInfo, index, v.c);
		readComponent(normalComponentInfo, index, v.n);
		readComponent(vertexComponentInfo, index, v.p);

		if (log.isTraceEnabled()) {
			log.trace(String.format("Vertex %d:", index));
			if (weightComponentInfo.enabled) {
				log.trace(String.format("  Weights(%d) %f, %f, %f, %f, %f, %f, %f, %f", weightComponentInfo.size, v.boneWeights[0], v.boneWeights[1], v.boneWeights[2], v.boneWeights[3], v.boneWeights[4], v.boneWeights[5], v.boneWeights[6], v.boneWeights[7]));
			}
			if (textureComponentInfo.enabled) {
				log.trace(String.format("  Texture %f, %f", v.t[0], v.t[1]));
			}
			if (colorComponentInfo.enabled) {
				log.trace(String.format("  Color 0x%08X", PixelColor.getColor(v.c)));
			}
			if (normalComponentInfo.enabled) {
				log.trace(String.format("  Normal %f, %f, %f", v.n[0], v.n[1], v.n[2]));
			}
			if (vertexComponentInfo.enabled) {
				log.trace(String.format("  Position %f, %f, %f", v.p[0], v.p[1], v.p[2]));
			}
		}
	}

	private void readComponent(ComponentInfo componentInfo, int index, float[] values) {
		if (!componentInfo.enabled) {
			return;
		}

		switch (componentInfo.type) {
			case IRenderingEngine.RE_FLOAT:
				for (int i = 0; i < componentInfo.size; i++) {
					values[i] = readFloat(componentInfo, index, i);
				}
				break;
		}
	}

	private int getPosition(ComponentInfo componentInfo, int index, int n) {
		return componentInfo.stride * index / IRenderingEngine.sizeOfType[componentInfo.type] + n + componentInfo.offset;
	}

	private float readFloat(ComponentInfo componentInfo, int index, int n) {
		if (componentInfo.buffer instanceof FloatBuffer) {
			FloatBuffer floatBuffer = (FloatBuffer) componentInfo.buffer;
			return floatBuffer.get(getPosition(componentInfo, index, n));
		} else if (componentInfo.buffer instanceof ByteBuffer) {
			ByteBuffer byteBuffer = (ByteBuffer) componentInfo.buffer;
			FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
			return floatBuffer.get(getPosition(componentInfo, index, n));
		}

		return 0.f;
	}

	private static class ComponentInfo {
		public boolean enabled = false;
		public int size;
		public int type;
		public int stride;
		public int offset;
		public Buffer buffer;
	}
}
