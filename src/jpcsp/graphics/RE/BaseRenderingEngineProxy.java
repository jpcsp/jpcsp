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

import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 * Base class for a RenderingEngine implementing a proxy functionality where
 * all the calls are forwarded to a proxy.
 */
public class BaseRenderingEngineProxy implements IRenderingEngine {
	protected final static Logger log = VideoEngine.log;
	protected IRenderingEngine re;
	protected IRenderingEngine proxy;
	protected GeContext context;
	protected static final float[] identityMatrix = {
		1, 0, 0, 0,
		0, 1, 0, 0,
		0, 0, 1, 0,
		0, 0, 0, 1
	};

	public BaseRenderingEngineProxy(IRenderingEngine proxy) {
		this.proxy = proxy;
		this.re = this;
		proxy.setRenderingEngine(this);
	}

	@Override
	public void setRenderingEngine(IRenderingEngine re) {
		this.re = re;
		proxy.setRenderingEngine(re);
	}

	@Override
	public void setGeContext(GeContext context) {
		this.context = context;
		proxy.setGeContext(context);
	}

	@Override
	public void endDirectRendering() {
		proxy.endDirectRendering();
	}

	@Override
	public void startDirectRendering(boolean textureEnabled, boolean depthWriteEnabled, boolean colorWriteEnabled, boolean setOrthoMatrix, boolean orthoInverted, int width, int height) {
		proxy.startDirectRendering(textureEnabled, depthWriteEnabled, colorWriteEnabled, setOrthoMatrix, orthoInverted, width, height);
	}

	@Override
	public void startDisplay() {
		proxy.startDisplay();
	}

	@Override
	public void endDisplay() {
		proxy.endDisplay();
	}

	@Override
	public void disableFlag(int flag) {
		proxy.disableFlag(flag);
	}

	@Override
	public void enableFlag(int flag) {
		proxy.enableFlag(flag);
	}

	@Override
	public void setBlendColor(float[] color) {
		proxy.setBlendColor(color);
	}

	@Override
	public void setBlendFunc(int src, int dst) {
		proxy.setBlendFunc(src, dst);
	}

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
		proxy.setColorMask(redMask, greenMask, blueMask, alphaMask);
	}

	@Override
	public void setColorMask(boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled) {
		proxy.setColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, alphaWriteEnabled);
	}

	@Override
	public void setColorMaterial(boolean ambient, boolean diffuse, boolean specular) {
		proxy.setColorMaterial(ambient, diffuse, specular);
	}

	@Override
	public void setDepthFunc(int func) {
		proxy.setDepthFunc(func);
	}

	@Override
	public void setDepthMask(boolean depthWriteEnabled) {
		proxy.setDepthMask(depthWriteEnabled);
	}

	@Override
	public void setDepthRange(float zpos, float zscale, float near, float far) {
		proxy.setDepthRange(zpos, zscale, near, far);
	}

	@Override
	public void setLightAmbientColor(int light, float[] color) {
		proxy.setLightAmbientColor(light, color);
	}

	@Override
	public void setLightColor(int type, int light, float[] color) {
		proxy.setLightColor(type, light, color);
	}

	@Override
	public void setLightConstantAttenuation(int light, float constant) {
		proxy.setLightConstantAttenuation(light, constant);
	}

	@Override
	public void setLightDiffuseColor(int light, float[] color) {
		proxy.setLightDiffuseColor(light, color);
	}

	@Override
	public void setLightDirection(int light, float[] direction) {
		proxy.setLightDirection(light, direction);
	}

	@Override
	public void setLightLinearAttenuation(int light, float linear) {
		proxy.setLightLinearAttenuation(light, linear);
	}

	@Override
	public void setLightMode(int mode) {
		proxy.setLightMode(mode);
	}

	@Override
	public void setLightModelAmbientColor(float[] color) {
		proxy.setLightModelAmbientColor(color);
	}

	@Override
	public void setLightPosition(int light, float[] position) {
		proxy.setLightPosition(light, position);
	}

	@Override
	public void setLightQuadraticAttenuation(int light, float quadratic) {
		proxy.setLightQuadraticAttenuation(light, quadratic);
	}

	@Override
	public void setLightSpecularColor(int light, float[] color) {
		proxy.setLightSpecularColor(light, color);
	}

	@Override
	public void setLightSpotCutoff(int light, float cutoff) {
		proxy.setLightSpotCutoff(light, cutoff);
	}

	@Override
	public void setLightSpotExponent(int light, float exponent) {
		proxy.setLightSpotExponent(light, exponent);
	}

	@Override
	public void setLightType(int light, int type, int kind) {
		proxy.setLightType(light, type, kind);
	}

	@Override
	public void setLogicOp(int logicOp) {
		proxy.setLogicOp(logicOp);
	}

	@Override
	public void setMaterialAmbientColor(float[] color) {
		proxy.setMaterialAmbientColor(color);
	}

	@Override
	public void setMaterialColor(int type, float[] color) {
		proxy.setMaterialColor(type, color);
	}

	@Override
	public void setMaterialDiffuseColor(float[] color) {
		proxy.setMaterialDiffuseColor(color);
	}

	@Override
	public void setMaterialEmissiveColor(float[] color) {
		proxy.setMaterialEmissiveColor(color);
	}

	@Override
	public void setMaterialSpecularColor(float[] color) {
		proxy.setMaterialSpecularColor(color);
	}

	@Override
	public void setMatrix(int type, float[] values) {
		proxy.setMatrix(type, values);
	}

	@Override
	public void setModelMatrix(float[] values) {
		proxy.setModelMatrix(values);
	}

	@Override
	public void endModelViewMatrixUpdate() {
		proxy.endModelViewMatrixUpdate();
	}

	@Override
	public void setModelViewMatrix(float[] values) {
		proxy.setModelViewMatrix(values);
	}

	@Override
	public void setMorphWeight(int index, float value) {
		proxy.setMorphWeight(index, value);
	}

	@Override
	public void setPatchDiv(int s, int t) {
		proxy.setPatchDiv(s, t);
	}

	@Override
	public void setPatchPrim(int prim) {
		proxy.setPatchPrim(prim);
	}

	@Override
	public void setProjectionMatrix(float[] values) {
		proxy.setProjectionMatrix(values);
	}

	@Override
	public void setShadeModel(int model) {
		proxy.setShadeModel(model);
	}

	@Override
	public void setTextureEnvironmentMapping(int u, int v) {
		proxy.setTextureEnvironmentMapping(u, v);
	}

	@Override
	public void setTextureMatrix(float[] values) {
		proxy.setTextureMatrix(values);
	}

	@Override
	public void setTextureMipmapMaxLevel(int level) {
		proxy.setTextureMipmapMaxLevel(level);
	}

	@Override
	public void setTextureMipmapMinLevel(int level) {
		proxy.setTextureMipmapMinLevel(level);
	}

	@Override
	public void setTextureMipmapMinFilter(int filter) {
		proxy.setTextureMipmapMinFilter(filter);
	}

	@Override
	public void setTextureMipmapMagFilter(int filter) {
		proxy.setTextureMipmapMagFilter(filter);
	}

	@Override
	public void setTextureWrapMode(int s, int t) {
		proxy.setTextureWrapMode(s, t);
	}

	@Override
	public void setVertexColor(float[] color) {
		proxy.setVertexColor(color);
	}

	@Override
	public void setViewMatrix(float[] values) {
		proxy.setViewMatrix(values);
	}

	@Override
	public void setViewport(int x, int y, int width, int height) {
		proxy.setViewport(x, y, width, height);
	}

	@Override
	public void setUniform(int id, int value) {
		proxy.setUniform(id, value);
	}

	@Override
	public void setUniform(int id, int value1, int value2) {
		proxy.setUniform(id, value1, value2);
	}

	@Override
	public void setUniform(int id, float value) {
		proxy.setUniform(id, value);
	}

	@Override
	public void setUniform3(int id, int[] values) {
		proxy.setUniform3(id, values);
	}

	@Override
	public void setUniform4(int id, int[] values) {
		proxy.setUniform4(id, values);
	}

	@Override
	public void setUniformMatrix4(int id, int count, float[] values) {
		proxy.setUniformMatrix4(id, count, values);
	}

	@Override
	public void setColorTestFunc(int func) {
		proxy.setColorTestFunc(func);
	}

	@Override
	public void setColorTestMask(int[] values) {
		proxy.setColorTestMask(values);
	}

	@Override
	public void setColorTestReference(int[] values) {
		proxy.setColorTestReference(values);
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
		proxy.setTextureFunc(func, alphaUsed, colorDoubled);
	}

	@Override
	public int setBones(int count, float[] values) {
		return proxy.setBones(count, values);
	}

	@Override
	public void setTextureMapMode(int mode, int proj) {
		proxy.setTextureMapMode(mode, proj);
	}

	@Override
	public void setTexEnv(int name, int param) {
		proxy.setTexEnv(name, param);
	}

	@Override
	public void setTexEnv(int name, float param) {
		proxy.setTexEnv(name, param);
	}

	@Override
	public void endClearMode() {
		proxy.endClearMode();
	}

	@Override
	public void startClearMode(boolean color, boolean stencil, boolean depth) {
		proxy.startClearMode(color, stencil, depth);
	}

	@Override
	public void attachShader(int program, int shader) {
		proxy.attachShader(program, shader);
	}

	@Override
	public void compilerShader(int shader, String[] source) {
		proxy.compilerShader(shader, source);
	}

	@Override
	public int createProgram() {
		return proxy.createProgram();
	}

	@Override
	public void useProgram(int program) {
		proxy.useProgram(program);
	}

	@Override
	public int createShader(int type) {
		return proxy.createShader(type);
	}

	@Override
	public int getAttribLocation(int program, String name) {
		return proxy.getAttribLocation(program, name);
	}

	@Override
	public String getProgramInfoLog(int program) {
		return proxy.getProgramInfoLog(program);
	}

	@Override
	public String getShaderInfoLog(int shader) {
		return proxy.getShaderInfoLog(shader);
	}

	@Override
	public int getUniformLocation(int program, String name) {
		return proxy.getUniformLocation(program, name);
	}

	@Override
	public void linkProgram(int program) {
		proxy.linkProgram(program);
	}

	@Override
	public void validateProgram(int program) {
		proxy.validateProgram(program);
	}

	@Override
	public boolean isFunctionAvailable(String name) {
		return proxy.isFunctionAvailable(name);
	}

	@Override
	public void drawArrays(int type, int first, int count) {
		proxy.drawArrays(type, first, count);
	}

	@Override
	public void deleteBuffer(int buffer) {
		proxy.deleteBuffer(buffer);
	}

	@Override
	public int genBuffer() {
		return proxy.genBuffer();
	}

	@Override
	public void setBufferData(int size, Buffer buffer, int usage) {
		proxy.setBufferData(size, buffer, usage);
	}

	@Override
	public void bindBuffer(int buffer) {
		proxy.bindBuffer(buffer);
	}

	@Override
	public void enableClientState(int type) {
		proxy.enableClientState(type);
	}

	@Override
	public void enableVertexAttribArray(int id) {
		proxy.enableVertexAttribArray(id);
	}

	@Override
	public void disableClientState(int type) {
		proxy.disableClientState(type);
	}

	@Override
	public void disableVertexAttribArray(int id) {
		proxy.disableVertexAttribArray(id);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, long offset) {
		proxy.setColorPointer(size, type, stride, offset);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, Buffer buffer) {
		proxy.setColorPointer(size, type, stride, buffer);
	}

	@Override
	public void setNormalPointer(int type, int stride, long offset) {
		proxy.setNormalPointer(type, stride, offset);
	}

	@Override
	public void setNormalPointer(int type, int stride, Buffer buffer) {
		proxy.setNormalPointer(type, stride, buffer);
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, long offset) {
		proxy.setTexCoordPointer(size, type, stride, offset);
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, Buffer buffer) {
		proxy.setTexCoordPointer(size, type, stride, buffer);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, long offset) {
		proxy.setVertexPointer(size, type, stride, offset);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, Buffer buffer) {
		proxy.setVertexPointer(size, type, stride, buffer);
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, long offset) {
		proxy.setVertexAttribPointer(id, size, type, normalized, stride, offset);
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, Buffer buffer) {
		proxy.setVertexAttribPointer(id, size, type, normalized, stride, buffer);
	}

	@Override
	public void setPixelStore(int rowLength, int alignment) {
		proxy.setPixelStore(rowLength, alignment);
	}

	@Override
	public int genTexture() {
		return proxy.genTexture();
	}

	@Override
	public void bindTexture(int texture) {
		proxy.bindTexture(texture);
	}

	@Override
	public void deleteTexture(int texture) {
		proxy.deleteTexture(texture);
	}

	@Override
	public void setCompressedTexImage(int level, int internalFormat, int width, int height, int compressedSize, Buffer buffer) {
		proxy.setCompressedTexImage(level, internalFormat, width, height, compressedSize, buffer);
	}

	@Override
	public void setTexImage(int level, int internalFormat, int width, int height, int format, int type, Buffer buffer) {
		proxy.setTexImage(level, internalFormat, width, height, format, type, buffer);
	}

	@Override
	public void setStencilOp(int fail, int zfail, int zpass) {
		proxy.setStencilOp(fail, zfail, zpass);
	}

	@Override
	public void setStencilFunc(int func, int ref, int mask) {
		proxy.setStencilFunc(func, ref, mask);
	}

	@Override
	public void setAlphaFunc(int func, int ref) {
		proxy.setAlphaFunc(func, ref);
	}

	@Override
	public void setBlendEquation(int mode) {
		proxy.setBlendEquation(mode);
	}

	@Override
	public void setFogColor(float[] color) {
		proxy.setFogColor(color);
	}

	@Override
	public void setFogDist(float start, float end) {
		proxy.setFogDist(start, end);
	}

	@Override
	public void setFrontFace(boolean cw) {
		proxy.setFrontFace(cw);
	}

	@Override
	public void setScissor(int x, int y, int width, int height) {
		proxy.setScissor(x, y, width, height);
	}

	@Override
	public void setTextureEnvColor(float[] color) {
		proxy.setTextureEnvColor(color);
	}

	@Override
	public void setFogHint() {
		proxy.setFogHint();
	}

	@Override
	public void setLineSmoothHint() {
		proxy.setLineSmoothHint();
	}

	@Override
	public void setMaterialShininess(float shininess) {
		proxy.setMaterialShininess(shininess);
	}

	@Override
	public void setTexSubImage(int level, int xOffset, int yOffset, int width, int height, int format, int type, Buffer buffer) {
		proxy.setTexSubImage(level, xOffset, yOffset, width, height, format, type, buffer);
	}

	@Override
	public void beginDraw(int type) {
		proxy.beginDraw(type);
	}

	@Override
	public void beginQuery(int id) {
		proxy.beginQuery(id);
	}

	@Override
	public void drawColor(float value1, float value2, float value3) {
		proxy.drawColor(value1, value2, value3);
	}

	@Override
	public void drawColor(float value1, float value2, float value3, float value4) {
		proxy.drawColor(value1, value2, value3, value4);
	}

	@Override
	public void drawTexCoord(float value1, float value2) {
		proxy.drawTexCoord(value1, value2);
	}

	@Override
	public void drawVertex(int value1, int value2) {
		proxy.drawVertex(value1, value2);
	}

	@Override
	public void drawVertex(float value1, float value2) {
		proxy.drawVertex(value1, value2);
	}

	@Override
	public void drawVertex3(float[] values) {
		proxy.drawVertex3(values);
	}

	@Override
	public void endDraw() {
		proxy.endDraw();
	}

	@Override
	public void endQuery() {
		proxy.endQuery();
	}

	@Override
	public int genQuery() {
		return proxy.genQuery();
	}

	@Override
	public void drawBoundingBox(float[][] values) {
		proxy.drawBoundingBox(values);
	}

	@Override
	public void endBoundingBox() {
		proxy.endBoundingBox();
	}

	@Override
	public void beginBoundingBox() {
		proxy.beginBoundingBox();
	}

	@Override
	public boolean hasBoundingBox() {
		return proxy.hasBoundingBox();
	}

	@Override
	public boolean isBoundingBoxVisible() {
		return proxy.isBoundingBoxVisible();
	}

	@Override
	public int getQueryResult(int id) {
		return proxy.getQueryResult(id);
	}

	@Override
	public boolean getQueryResultAvailable(int id) {
		return proxy.getQueryResultAvailable(id);
	}

	@Override
	public void clear(float red, float green, float blue, float alpha) {
		proxy.clear(red, green, blue, alpha);
	}

	@Override
	public void copyTexSubImage(int level, int xOffset, int yOffset, int x, int y, int width, int height) {
		proxy.copyTexSubImage(level, xOffset, yOffset, x, y, width, height);
	}

	@Override
	public void getTexImage(int level, int format, int type, Buffer buffer) {
		proxy.getTexImage(level, format, type, buffer);
	}

	@Override
	public void readPixels(int x, int y, int width, int height, int format, int type, Buffer buffer) {
		proxy.readPixels(x, y, width, height, format, type, buffer);
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, long offset) {
		proxy.setWeightPointer(size, type, stride, offset);
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, Buffer buffer) {
		proxy.setWeightPointer(size, type, stride, buffer);
	}
}
