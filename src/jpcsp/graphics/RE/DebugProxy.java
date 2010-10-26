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

import jpcsp.graphics.Uniforms;

/**
 * @author gid15
 *
 */
public class DebugProxy extends BaseRenderingEngineProxy {
	protected int useProgram;

	public DebugProxy(IRenderingEngine proxy) {
		super(proxy);
	}

	protected String getEnabledDisabled(boolean enabled) {
		return enabled ? "enabled" : "disabled";
	}

	@Override
	public void enableFlag(int flag) {
		if (log.isDebugEnabled()) {
			if (flag < context.flags.size()) {
				log.debug(String.format("enableFlag %s", context.flags.get(flag).toString()));
			} else {
				log.debug(String.format("enableFlag %d", flag));
			}
		}
		super.enableFlag(flag);
	}

	@Override
	public void disableFlag(int flag) {
		if (log.isDebugEnabled()) {
			if (flag < context.flags.size()) {
				log.debug(String.format("disableFlag %s", context.flags.get(flag).toString()));
			} else {
				log.debug(String.format("disableFlag %d", flag));
			}
		}
		super.disableFlag(flag);
	}

	@Override
	public void setAlphaFunc(int func, int ref) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setAlphaFunc func=%d, ref=0x%02X", func, ref));
		}
		super.setAlphaFunc(func, ref);
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setTextureFunc func=%d%s%s", func, alphaUsed ? " ALPHA" : "", colorDoubled ? " COLORx2" : ""));
		}
		super.setTextureFunc(func, alphaUsed, colorDoubled);
	}

	@Override
	public void setBlendFunc(int src, int dst) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setBlendFunc src=%d, dst=%d", src, dst));
		}
		super.setBlendFunc(src, dst);
	}

	@Override
	public void setBlendEquation(int mode) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setBlendEquation mode=%d", mode));
		}
		super.setBlendEquation(mode);
	}

	protected void debugMatrix(String name, float[] matrix, int offset) {
		if (matrix == null) {
			matrix = identityMatrix;
		}
        for (int y = 0; y < 4; y++) {
            log.debug(String.format("%s %.3f %.3f %.3f %.3f", name, matrix[offset + 0 + y * 4], matrix[offset + 1 + y * 4], matrix[offset + 2 + y * 4], matrix[offset + 3 + y * 4]));
        }
	}

	protected void debugMatrix(String name, float[] matrix) {
		debugMatrix(name, matrix, 0);
	}

	@Override
	public void setMatrix(int type, float[] values) {
		if (log.isDebugEnabled()) {
			debugMatrix("setMatrix " + type, values);
		}
		super.setMatrix(type, values);
	}

	@Override
	public void setModelMatrix(float[] values) {
		if (log.isDebugEnabled()) {
			debugMatrix("setModelMatrix", values);
		}
		super.setModelMatrix(values);
	}

	@Override
	public void setModelViewMatrix(float[] values) {
		if (log.isDebugEnabled()) {
			debugMatrix("setModelViewMatrix", values);
		}
		super.setModelViewMatrix(values);
	}

	@Override
	public void setProjectionMatrix(float[] values) {
		if (log.isDebugEnabled()) {
			debugMatrix("setProjectionMatrix", values);
		}
		super.setProjectionMatrix(values);
	}

	@Override
	public void setTextureMatrix(float[] values) {
		if (log.isDebugEnabled()) {
			debugMatrix("setTextureMatrix", values);
		}
		super.setTextureMatrix(values);
	}

	@Override
	public void setViewMatrix(float[] values) {
		if (log.isDebugEnabled()) {
			debugMatrix("setViewMatrix", values);
		}
		super.setViewMatrix(values);
	}

	@Override
	public void setTextureMipmapMinLevel(int level) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setTextureMipmapMinLevel %d", level));
		}
		super.setTextureMipmapMinLevel(level);
	}

	@Override
	public void setTextureMipmapMaxLevel(int level) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setTextureMipmapMaxLevel %d", level));
		}
		super.setTextureMipmapMaxLevel(level);
	}

	@Override
	public void setTextureMipmapMinFilter(int filter) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setTextureMipmapMinFilter %d", filter));
		}
		super.setTextureMipmapMinFilter(filter);
	}

	@Override
	public void setTextureMipmapMagFilter(int filter) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setTextureMipmapMagFilter %d", filter));
		}
		super.setTextureMipmapMagFilter(filter);
	}

	@Override
	public void setPixelStore(int rowLength, int alignment) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setPixelStore rowLength=%d, alignment=%d", rowLength, alignment));
		}
		super.setPixelStore(rowLength, alignment);
	}

	@Override
	public void setCompressedTexImage(int level, int internalFormat, int width, int height, int compressedSize, Buffer buffer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setCompressedTexImage level=%d, internalFormat=%d, %dx%d, compressedSize=0x%X", level, internalFormat, width, height, compressedSize));
		}
		super.setCompressedTexImage(level, internalFormat, width, height, compressedSize, buffer);
	}

	@Override
	public void setTexImage(int level, int internalFormat, int width, int height, int format, int type, int textureSize, Buffer buffer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setTexImage level=%d, internalFormat=%d, %dx%d, format=%d, type=%d, textureSize=%d", level, internalFormat, width, height, format, type, textureSize));
		}
		super.setTexImage(level, internalFormat, width, height, format, type, textureSize, buffer);
	}

	@Override
	public void startClearMode(boolean color, boolean stencil, boolean depth) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("startClearMode color=%b, stencil=%b, depth=%b", color, stencil, depth));
		}
		super.startClearMode(color, stencil, depth);
	}

	@Override
	public void endClearMode() {
		if (log.isDebugEnabled()) {
			log.debug("endClearMode");
		}
		super.endClearMode();
	}

	@Override
	public void startDirectRendering(boolean textureEnabled, boolean depthWriteEnabled, boolean colorWriteEnabled, boolean setOrthoMatrix, boolean orthoInverted, int width, int height) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("startDirectRendering texture=%b, depth=%b, color=%b, ortho=%b, inverted=%b, %dx%d", textureEnabled, depthWriteEnabled, colorWriteEnabled, setOrthoMatrix, orthoInverted, width, height));
		}
		super.startDirectRendering(textureEnabled, depthWriteEnabled, colorWriteEnabled, setOrthoMatrix, orthoInverted, width, height);
	}

	@Override
	public void endDirectRendering() {
		if (log.isDebugEnabled()) {
			log.debug("endDirectRendering");
		}
		super.endDirectRendering();
	}

	@Override
	public void bindTexture(int texture) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("bindTexture %d", texture));
		}
		super.bindTexture(texture);
	}

	@Override
	public void drawArrays(int type, int first, int count) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("drawArrays type=%d, first=%d, count=%d", type, first, count));
		}
		super.drawArrays(type, first, count);
	}

	@Override
	public void setDepthFunc(int func) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setDepthFunc %d", func));
		}
		super.setDepthFunc(func);
	}

	@Override
	public void setDepthMask(boolean depthWriteEnabled) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setDepthMask %s", getEnabledDisabled(depthWriteEnabled)));
		}
		super.setDepthMask(depthWriteEnabled);
	}

	@Override
	public void setDepthRange(float zpos, float zscale, float near, float far) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setDepthRange zpos=%f, zscale=%f, near=%f, far=%f", zpos, zscale, near, far));
		}
		super.setDepthRange(zpos, zscale, near, far);
	}

	@Override
	public void setStencilFunc(int func, int ref, int mask) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setStencilFunc func=%d, ref=0x%02X, mask=0x%02X", func, ref, mask));
		}
		super.setStencilFunc(func, ref, mask);
	}

	@Override
	public void setStencilOp(int fail, int zfail, int zpass) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setStencilOp fail=%d, zfail=%d, zpass=%d", fail, zfail, zpass));
		}
		super.setStencilOp(fail, zfail, zpass);
	}

	@Override
	public int setBones(int count, float[] values) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setBones count=%d", count));
			for (int i = 0; i < count; i++) {
				debugMatrix("setBones[" + i + "]", values, i * 16);
			}
		}
		return super.setBones(count, values);
	}

	@Override
	public void setUniformMatrix4(int id, int count, float[] values) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setUniformMatrix4 id=%d, count=%d", id, count));
			for (int i = 0; i < count; i++) {
				debugMatrix("setUniformMatrix4[" + i + "]", values, i * 16);
			}
		}
		super.setUniformMatrix4(id, count, values);
	}

	protected String getUniformName(int id) {
		for (Uniforms uniform : Uniforms.values()) {
			if (uniform.getId(useProgram) == id) {
				return uniform.name();
			}
		}

		return "Uniform " + id;
	}

	@Override
	public void enableVertexAttribArray(int id) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("enableVertexAttribArray %d", id));
		}
		super.enableVertexAttribArray(id);
	}

	@Override
	public void disableVertexAttribArray(int id) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("disableVertexAttribArray %d", id));
		}
		super.disableVertexAttribArray(id);
	}

	@Override
	public void setUniform(int id, float value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setUniform %s=%f", getUniformName(id), value));
		}
		super.setUniform(id, value);
	}

	@Override
	public void setUniform(int id, int value1, int value2) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setUniform %s=%d, %d", getUniformName(id), value1, value2));
		}
		super.setUniform(id, value1, value2);
	}

	@Override
	public void setUniform2(int id, int[] values) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setUniform2 %s=%d, %d", getUniformName(id), values[0], values[1]));
		}
		super.setUniform2(id, values);
	}

	@Override
	public void setUniform3(int id, int[] values) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setUniform3 %s=%d, %d, %d", getUniformName(id), values[0], values[1], values[2]));
		}
		super.setUniform3(id, values);
	}

	@Override
	public void setUniform4(int id, int[] values) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setUniform4 %s=%d, %d, %d, %d", getUniformName(id), values[0], values[1], values[2], values[3]));
		}
		super.setUniform4(id, values);
	}

	@Override
	public void setUniform(int id, int value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setUniform %s=%d", getUniformName(id), value));
		}
		super.setUniform(id, value);
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, int bufferSize, Buffer buffer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setVertexAttribPointer id=%d, size=%d, type=%d, normalized=%b, stride=%d, bufferSize=%d", id, size, type, normalized, stride, bufferSize));
		}
		super.setVertexAttribPointer(id, size, type, normalized, stride, bufferSize, buffer);
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, long offset) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setVertexAttribPointer id=%d, size=%d, type=%d, normalized=%b, stride=%d, offset=%d", id, size, type, normalized, stride, offset));
		}
		super.setVertexAttribPointer(id, size, type, normalized, stride, offset);
	}

	@Override
	public void enableClientState(int type) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("enableClientState %d", type));
		}
		super.enableClientState(type);
	}

	@Override
	public void disableClientState(int type) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("disableClientState %d", type));
		}
		super.disableClientState(type);
	}

	@Override
	public void setColorMask(boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setColorMask red %s, green %s, blue %s, alpha %s", getEnabledDisabled(redWriteEnabled), getEnabledDisabled(greenWriteEnabled), getEnabledDisabled(blueWriteEnabled), getEnabledDisabled(alphaWriteEnabled)));
		}
		super.setColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, alphaWriteEnabled);
	}

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setColorMask red 0x%02X, green 0x%02X, blue 0x%02X, alpha 0x%02X", redMask, greenMask, blueMask, alphaMask));
		}
		super.setColorMask(redMask, greenMask, blueMask, alphaMask);
	}

	@Override
	public void setTextureWrapMode(int s, int t) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setTextureWrapMode %d, %d", s, t));
		}
		super.setTextureWrapMode(s, t);
	}

	@Override
	public void deleteTexture(int texture) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("deleteTexture %d", texture));
		}
		super.deleteTexture(texture);
	}

	@Override
	public void endDisplay() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("endDisplay"));
		}
		super.endDisplay();
	}

	@Override
	public int genTexture() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("genTexture"));
		}
		return super.genTexture();
	}

	@Override
	public void startDisplay() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("startDisplay"));
		}
		super.startDisplay();
	}

	@Override
	public void setTextureMapMode(int mode, int proj) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setTextureMapMode mode=%d, proj=%d", mode, proj));
		}
		super.setTextureMapMode(mode, proj);
	}

	@Override
	public void setFrontFace(boolean cw) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setFrontFace %s", cw ? "clockwise" : "counter-clockwise"));
		}
		super.setFrontFace(cw);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setColorPointer size=%d, type=%d, stride=%d, bufferSize=%d, buffer offset=%d", size, type, stride, bufferSize, buffer.position()));
		}
		super.setColorPointer(size, type, stride, bufferSize, buffer);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, long offset) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setColorPointer size=%d, type=%d, stride=%d, offset=%d", size, type, stride, offset));
		}
		super.setColorPointer(size, type, stride, offset);
	}

	@Override
	public void setNormalPointer(int type, int stride, int bufferSize, Buffer buffer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setNormalPointer type=%d, stride=%d, bufferSize=%d, buffer offset=%d", type, stride, bufferSize, buffer.position()));
		}
		super.setNormalPointer(type, stride, bufferSize, buffer);
	}

	@Override
	public void setNormalPointer(int type, int stride, long offset) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setNormalPointer type=%d, stride=%d, offset=%d", type, stride, offset));
		}
		super.setNormalPointer(type, stride, offset);
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setTexCoordPointer size=%d, type=%d, stride=%d, bufferSize=%d, buffer offset=%d", size, type, stride, bufferSize, buffer.position()));
		}
		super.setTexCoordPointer(size, type, stride, bufferSize, buffer);
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, long offset) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setTexCoordPointer size=%d, type=%d, stride=%d, offset=%d", size, type, stride, offset));
		}
		super.setTexCoordPointer(size, type, stride, offset);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setVertexPointer size=%d, type=%d, stride=%d, bufferSize=%d, buffer offset=%d", size, type, stride, bufferSize, buffer.position()));
		}
		super.setVertexPointer(size, type, stride, bufferSize, buffer);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, long offset) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setVertexPointer size=%d, type=%d, stride=%d, offset=%d", size, type, stride, offset));
		}
		super.setVertexPointer(size, type, stride, offset);
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setWeightPointer size=%d, type=%d, stride=%d, bufferSize=%d, buffer offset=%d", size, type, stride, bufferSize, buffer.position()));
		}
		super.setWeightPointer(size, type, stride, bufferSize, buffer);
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, long offset) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setWeightPointer size=%d, type=%d, stride=%d, offset=%d", size, type, stride, offset));
		}
		super.setWeightPointer(size, type, stride, offset);
	}

	@Override
	public void setBufferData(int size, Buffer buffer, int usage) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setBufferData size=%d, buffer size=%d, usage=%d", size, buffer.capacity(), usage));
		}
		super.setBufferData(size, buffer, usage);
	}

	@Override
	public void bindBuffer(int buffer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("bindBuffer %d", buffer));
		}
		super.bindBuffer(buffer);
	}

	@Override
	public void useProgram(int program) {
		useProgram = program;
		if (log.isDebugEnabled()) {
			log.debug(String.format("useProgram %d", program));
		}
		super.useProgram(program);
	}

	@Override
	public void setViewport(int x, int y, int width, int height) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setViewport x=%d, y=%d, width=%d, height=%d", x, y, width, height));
		}
		super.setViewport(x, y, width, height);
	}

	@Override
	public void setScissor(int x, int y, int width, int height) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setScissor x=%d, y=%d, width=%d, height=%d", x, y, width, height));
		}
		super.setScissor(x, y, width, height);
	}

	@Override
	public void setTexSubImage(int level, int xOffset, int yOffset, int width, int height, int format, int type, int textureSize, Buffer buffer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setTexSubImage level=%d, xOffset=%d, yOffset=%d, width=%d, height=%d, format=%d, type=%d, textureSize=%d", level, xOffset, yOffset, width, height, format, type, textureSize));
		}
		super.setTexSubImage(level, xOffset, yOffset, width, height, format, type, textureSize, buffer);
	}

	@Override
	public void copyTexSubImage(int level, int xOffset, int yOffset, int x, int y, int width, int height) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("copyTexSubImage level=%d, xOffset=%d, yOffset=%d, x=%d, y=%d, width=%d, height=%d", level, xOffset, yOffset, x, y, width, height));
		}
		super.copyTexSubImage(level, xOffset, yOffset, x, y, width, height);
	}

	@Override
	public void getTexImage(int level, int format, int type, Buffer buffer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("getTexImage level=%d, format=%d, type=%d, buffer remaining=%d, buffer class=%s", level, format, type, buffer.remaining(), buffer.getClass().getName()));
		}
		super.getTexImage(level, format, type, buffer);
	}

	@Override
	public void readPixels(int x, int y, int width, int height, int format, int type, Buffer buffer) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("readPixels x=%d, y=%d, width=%d, height=%d, format=%d, type=%d", x, y, width, height, format, type));
		}
		super.readPixels(x, y, width, height, format, type, buffer);
	}
}
