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
import java.nio.IntBuffer;

import jpcsp.graphics.GeContext;
import jpcsp.graphics.VertexInfo;
import jpcsp.graphics.RE.buffer.IREBufferManager;

/**
 * This RenderingEngine-Proxy class checks and logs
 * after each RE call any error that has occurred
 * during that particular call.
 * 
 * @author gid15
 *
 */
public class CheckErrorsProxy extends BaseRenderingEngineProxy {
	public CheckErrorsProxy(IRenderingEngine proxy) {
		super(proxy);
	}

	@Override
	public boolean checkAndLogErrors(String logComment) {
		return super.checkAndLogErrors(logComment);
	}

	@Override
	public void exit() {
		super.exit();
		re.checkAndLogErrors("exit");
	}

	@Override
	public void setRenderingEngine(IRenderingEngine re) {
		super.setRenderingEngine(re);
		re.checkAndLogErrors("setRenderingEngine");
	}

	@Override
	public void setGeContext(GeContext context) {
		super.setGeContext(context);
		re.checkAndLogErrors("setGeContext");
	}

	@Override
	public void endDirectRendering() {
		super.endDirectRendering();
		re.checkAndLogErrors("endDirectRendering");
	}

	@Override
	public void startDirectRendering(boolean textureEnabled, boolean depthWriteEnabled, boolean colorWriteEnabled, boolean setOrthoMatrix, boolean orthoInverted, int width, int height) {
		super.startDirectRendering(textureEnabled, depthWriteEnabled, colorWriteEnabled, setOrthoMatrix, orthoInverted, width, height);
		re.checkAndLogErrors("startDirectRendering");
	}

	@Override
	public void startDisplay() {
		super.startDisplay();
		re.checkAndLogErrors("startDisplay");
	}

	@Override
	public void endDisplay() {
		super.endDisplay();
		re.checkAndLogErrors("endDisplay");
	}

	@Override
	public void disableFlag(int flag) {
		super.disableFlag(flag);
		re.checkAndLogErrors("disableFlag");
	}

	@Override
	public void enableFlag(int flag) {
		super.enableFlag(flag);
		re.checkAndLogErrors("enableFlag");
	}

	@Override
	public void setBlendColor(float[] color) {
		super.setBlendColor(color);
		re.checkAndLogErrors("setBlendColor");
	}

	@Override
	public void setBlendFunc(int src, int dst) {
		super.setBlendFunc(src, dst);
		re.checkAndLogErrors("setBlendFunc");
	}

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
		super.setColorMask(redMask, greenMask, blueMask, alphaMask);
		re.checkAndLogErrors("setColorMask");
	}

	@Override
	public void setColorMask(boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled) {
		super.setColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, alphaWriteEnabled);
		re.checkAndLogErrors("setColorMask");
	}

	@Override
	public void setColorMaterial(boolean ambient, boolean diffuse, boolean specular) {
		super.setColorMaterial(ambient, diffuse, specular);
		re.checkAndLogErrors("setColorMaterial");
	}

	@Override
	public void setDepthFunc(int func) {
		super.setDepthFunc(func);
		re.checkAndLogErrors("setDepthFunc");
	}

	@Override
	public void setDepthMask(boolean depthWriteEnabled) {
		super.setDepthMask(depthWriteEnabled);
		re.checkAndLogErrors("setDepthMask");
	}

	@Override
	public void setDepthRange(float zpos, float zscale, int near, int far) {
		super.setDepthRange(zpos, zscale, near, far);
		re.checkAndLogErrors("setDepthRange");
	}

	@Override
	public void setLightAmbientColor(int light, float[] color) {
		super.setLightAmbientColor(light, color);
		re.checkAndLogErrors("setLightAmbientColor");
	}

	@Override
	public void setLightConstantAttenuation(int light, float constant) {
		super.setLightConstantAttenuation(light, constant);
		re.checkAndLogErrors("setLightConstantAttenuation");
	}

	@Override
	public void setLightDiffuseColor(int light, float[] color) {
		super.setLightDiffuseColor(light, color);
		re.checkAndLogErrors("setLightDiffuseColor");
	}

	@Override
	public void setLightDirection(int light, float[] direction) {
		super.setLightDirection(light, direction);
		re.checkAndLogErrors("setLightDirection");
	}

	@Override
	public void setLightLinearAttenuation(int light, float linear) {
		super.setLightLinearAttenuation(light, linear);
		re.checkAndLogErrors("setLightLinearAttenuation");
	}

	@Override
	public void setLightMode(int mode) {
		super.setLightMode(mode);
		re.checkAndLogErrors("setLightMode");
	}

	@Override
	public void setLightModelAmbientColor(float[] color) {
		super.setLightModelAmbientColor(color);
		re.checkAndLogErrors("setLightModelAmbientColor");
	}

	@Override
	public void setLightPosition(int light, float[] position) {
		super.setLightPosition(light, position);
		re.checkAndLogErrors("setLightPosition");
	}

	@Override
	public void setLightQuadraticAttenuation(int light, float quadratic) {
		super.setLightQuadraticAttenuation(light, quadratic);
		re.checkAndLogErrors("setLightQuadraticAttenuation");
	}

	@Override
	public void setLightSpecularColor(int light, float[] color) {
		super.setLightSpecularColor(light, color);
		re.checkAndLogErrors("setLightSpecularColor");
	}

	@Override
	public void setLightSpotCutoff(int light, float cutoff) {
		super.setLightSpotCutoff(light, cutoff);
		re.checkAndLogErrors("setLightSpotCutoff");
	}

	@Override
	public void setLightSpotExponent(int light, float exponent) {
		super.setLightSpotExponent(light, exponent);
		re.checkAndLogErrors("setLightSpotExponent");
	}

	@Override
	public void setLightType(int light, int type, int kind) {
		super.setLightType(light, type, kind);
		re.checkAndLogErrors("setLightType");
	}

	@Override
	public void setLogicOp(int logicOp) {
		super.setLogicOp(logicOp);
		re.checkAndLogErrors("setLogicOp");
	}

	@Override
	public void setMaterialAmbientColor(float[] color) {
		super.setMaterialAmbientColor(color);
		re.checkAndLogErrors("setMaterialAmbientColor");
	}

	@Override
	public void setMaterialDiffuseColor(float[] color) {
		super.setMaterialDiffuseColor(color);
		re.checkAndLogErrors("setMaterialDiffuseColor");
	}

	@Override
	public void setMaterialEmissiveColor(float[] color) {
		super.setMaterialEmissiveColor(color);
		re.checkAndLogErrors("setMaterialEmissiveColor");
	}

	@Override
	public void setMaterialSpecularColor(float[] color) {
		super.setMaterialSpecularColor(color);
		re.checkAndLogErrors("setMaterialSpecularColor");
	}

	@Override
	public void setMatrix(float[] values) {
		super.setMatrix(values);
		re.checkAndLogErrors("setMatrix");
	}

	@Override
	public void setMatrixMode(int type) {
		super.setMatrixMode(type);
		re.checkAndLogErrors("setMatrixMode");
	}

	@Override
	public void multMatrix(float[] values) {
		super.multMatrix(values);
		re.checkAndLogErrors("multMatrix");
	}

	@Override
	public void setModelMatrix(float[] values) {
		super.setModelMatrix(values);
		re.checkAndLogErrors("setModelMatrix");
	}

	@Override
	public void endModelViewMatrixUpdate() {
		super.endModelViewMatrixUpdate();
		re.checkAndLogErrors("endModelViewMatrixUpdate");
	}

	@Override
	public void setModelViewMatrix(float[] values) {
		super.setModelViewMatrix(values);
		re.checkAndLogErrors("setModelViewMatrix");
	}

	@Override
	public void setMorphWeight(int index, float value) {
		super.setMorphWeight(index, value);
		re.checkAndLogErrors("setMorphWeight");
	}

	@Override
	public void setPatchDiv(int s, int t) {
		super.setPatchDiv(s, t);
		re.checkAndLogErrors("setPatchDiv");
	}

	@Override
	public void setPatchPrim(int prim) {
		super.setPatchPrim(prim);
		re.checkAndLogErrors("setPatchPrim");
	}

	@Override
	public void setProjectionMatrix(float[] values) {
		super.setProjectionMatrix(values);
		re.checkAndLogErrors("setProjectionMatrix");
	}

	@Override
	public void setShadeModel(int model) {
		super.setShadeModel(model);
		re.checkAndLogErrors("setShadeModel");
	}

	@Override
	public void setTextureEnvironmentMapping(int u, int v) {
		super.setTextureEnvironmentMapping(u, v);
		re.checkAndLogErrors("setTextureEnvironmentMapping");
	}

	@Override
	public void setTextureMatrix(float[] values) {
		super.setTextureMatrix(values);
		re.checkAndLogErrors("setTextureMatrix");
	}

	@Override
	public void setTextureMipmapMaxLevel(int level) {
		super.setTextureMipmapMaxLevel(level);
		re.checkAndLogErrors("setTextureMipmapMaxLevel");
	}

	@Override
	public void setTextureMipmapMinLevel(int level) {
		super.setTextureMipmapMinLevel(level);
		re.checkAndLogErrors("setTextureMipmapMinLevel");
	}

	@Override
	public void setTextureMipmapMinFilter(int filter) {
		super.setTextureMipmapMinFilter(filter);
		re.checkAndLogErrors("setTextureMipmapMinFilter");
	}

	@Override
	public void setTextureMipmapMagFilter(int filter) {
		super.setTextureMipmapMagFilter(filter);
		re.checkAndLogErrors("setTextureMipmapMagFilter");
	}

	@Override
	public void setTextureWrapMode(int s, int t) {
		super.setTextureWrapMode(s, t);
		re.checkAndLogErrors("setTextureWrapMode");
	}

	@Override
	public void setVertexColor(float[] color) {
		super.setVertexColor(color);
		re.checkAndLogErrors("setVertexColor");
	}

	@Override
	public void setViewMatrix(float[] values) {
		super.setViewMatrix(values);
		re.checkAndLogErrors("setViewMatrix");
	}

	@Override
	public void setViewport(int x, int y, int width, int height) {
		super.setViewport(x, y, width, height);
		re.checkAndLogErrors("setViewport");
	}

	@Override
	public void setUniform(int id, int value) {
		super.setUniform(id, value);
		re.checkAndLogErrors("setUniform");
	}

	@Override
	public void setUniform(int id, int value1, int value2) {
		super.setUniform(id, value1, value2);
		re.checkAndLogErrors("setUniform");
	}

	@Override
	public void setUniform(int id, float value) {
		super.setUniform(id, value);
		re.checkAndLogErrors("setUniform");
	}

	@Override
	public void setUniform2(int id, int[] values) {
		super.setUniform2(id, values);
		re.checkAndLogErrors("setUniform2");
	}

	@Override
	public void setUniform3(int id, int[] values) {
		super.setUniform3(id, values);
		re.checkAndLogErrors("setUniform3");
	}

	@Override
	public void setUniform3(int id, float[] values) {
		super.setUniform3(id, values);
		re.checkAndLogErrors("setUniform3");
	}

	@Override
	public void setUniform4(int id, int[] values) {
		super.setUniform4(id, values);
		re.checkAndLogErrors("setUniform4");
	}

	@Override
	public void setUniform4(int id, float[] values) {
		super.setUniform4(id, values);
		re.checkAndLogErrors("setUniform4");
	}

	@Override
	public void setUniformMatrix4(int id, int count, float[] values) {
		super.setUniformMatrix4(id, count, values);
		re.checkAndLogErrors("setUniformMatrix4");
	}

	@Override
	public void setColorTestFunc(int func) {
		super.setColorTestFunc(func);
		re.checkAndLogErrors("setColorTestFunc");
	}

	@Override
	public void setColorTestMask(int[] values) {
		super.setColorTestMask(values);
		re.checkAndLogErrors("setColorTestMask");
	}

	@Override
	public void setColorTestReference(int[] values) {
		super.setColorTestReference(values);
		re.checkAndLogErrors("setColorTestReference");
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
		super.setTextureFunc(func, alphaUsed, colorDoubled);
		re.checkAndLogErrors("setTextureFunc");
	}

	@Override
	public int setBones(int count, float[] values) {
		int value = super.setBones(count, values);
		re.checkAndLogErrors("setBones");
		return value;
	}

	@Override
	public void setTextureMapMode(int mode, int proj) {
		super.setTextureMapMode(mode, proj);
		re.checkAndLogErrors("setTextureMapMode");
	}

	@Override
	public void setTexEnv(int name, int param) {
		super.setTexEnv(name, param);
		re.checkAndLogErrors("setTexEnv");
	}

	@Override
	public void setTexEnv(int name, float param) {
		super.setTexEnv(name, param);
		re.checkAndLogErrors("setTexEnv");
	}

	@Override
	public void endClearMode() {
		super.endClearMode();
		re.checkAndLogErrors("endClearMode");
	}

	@Override
	public void startClearMode(boolean color, boolean stencil, boolean depth) {
		super.startClearMode(color, stencil, depth);
		re.checkAndLogErrors("startClearMode");
	}

	@Override
	public void attachShader(int program, int shader) {
		super.attachShader(program, shader);
		re.checkAndLogErrors("attachShader");
	}

	@Override
	public boolean compilerShader(int shader, String source) {
		boolean value = super.compilerShader(shader, source);
		re.checkAndLogErrors("compilerShader");
		return value;
	}

	@Override
	public int createProgram() {
		int value = super.createProgram();
		re.checkAndLogErrors("createProgram");
		return value;
	}

	@Override
	public void useProgram(int program) {
		super.useProgram(program);
		re.checkAndLogErrors("useProgram");
	}

	@Override
	public int createShader(int type) {
		int value = super.createShader(type);
		re.checkAndLogErrors("createShader");
		return value;
	}

	@Override
	public int getAttribLocation(int program, String name) {
		int value = super.getAttribLocation(program, name);
		re.checkAndLogErrors("getAttribLocation");
		return value;
	}

	@Override
	public void bindAttribLocation(int program, int index, String name) {
		super.bindAttribLocation(program, index, name);
		re.checkAndLogErrors("bindAttribLocation");
	}

	@Override
	public String getProgramInfoLog(int program) {
		String value = super.getProgramInfoLog(program);
		re.checkAndLogErrors("getProgramInfoLog");
		return value;
	}

	@Override
	public String getShaderInfoLog(int shader) {
		String value = super.getShaderInfoLog(shader);
		re.checkAndLogErrors("getShaderInfoLog");
		return value;
	}

	@Override
	public int getUniformLocation(int program, String name) {
		int value = super.getUniformLocation(program, name);
		re.checkAndLogErrors("getUniformLocation");
		return value;
	}

	@Override
	public boolean linkProgram(int program) {
		boolean value = super.linkProgram(program);
		re.checkAndLogErrors("linkProgram");
		return value;
	}

	@Override
	public boolean validateProgram(int program) {
		boolean value = super.validateProgram(program);
		re.checkAndLogErrors("validateProgram");
		return value;
	}

	@Override
	public boolean isExtensionAvailable(String name) {
		boolean value = super.isExtensionAvailable(name);
		re.checkAndLogErrors("isExtensionAvailable");
		return value;
	}

	@Override
	public void drawArrays(int type, int first, int count) {
		super.drawArrays(type, first, count);
		re.checkAndLogErrors("drawArrays");
	}

	@Override
	public void deleteBuffer(int buffer) {
		super.deleteBuffer(buffer);
		re.checkAndLogErrors("deleteBuffer");
	}

	@Override
	public int genBuffer() {
		int value = super.genBuffer();
		re.checkAndLogErrors("genBuffer");
		return value;
	}

	@Override
	public void setBufferData(int target, int size, Buffer buffer, int usage) {
		super.setBufferData(target, size, buffer, usage);
		re.checkAndLogErrors("setBufferData");
	}

	@Override
	public void setBufferSubData(int target, int offset, int size, Buffer buffer) {
		super.setBufferSubData(target, offset, size, buffer);
		re.checkAndLogErrors("setBufferSubData");
	}

	@Override
	public void bindBuffer(int target, int buffer) {
		super.bindBuffer(target, buffer);
		re.checkAndLogErrors("bindBuffer");
	}

	@Override
	public void enableClientState(int type) {
		super.enableClientState(type);
		re.checkAndLogErrors("enableClientState");
	}

	@Override
	public void enableVertexAttribArray(int id) {
		super.enableVertexAttribArray(id);
		re.checkAndLogErrors("enableVertexAttribArray");
	}

	@Override
	public void disableClientState(int type) {
		super.disableClientState(type);
		re.checkAndLogErrors("disableClientState");
	}

	@Override
	public void disableVertexAttribArray(int id) {
		super.disableVertexAttribArray(id);
		re.checkAndLogErrors("disableVertexAttribArray");
	}

	@Override
	public void setColorPointer(int size, int type, int stride, long offset) {
		super.setColorPointer(size, type, stride, offset);
		re.checkAndLogErrors("setColorPointer");
	}

	@Override
	public void setColorPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		super.setColorPointer(size, type, stride, bufferSize, buffer);
		re.checkAndLogErrors("setColorPointer");
	}

	@Override
	public void setNormalPointer(int type, int stride, long offset) {
		super.setNormalPointer(type, stride, offset);
		re.checkAndLogErrors("setNormalPointer");
	}

	@Override
	public void setNormalPointer(int type, int stride, int bufferSize, Buffer buffer) {
		super.setNormalPointer(type, stride, bufferSize, buffer);
		re.checkAndLogErrors("setNormalPointer");
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, long offset) {
		super.setTexCoordPointer(size, type, stride, offset);
		re.checkAndLogErrors("setTexCoordPointer");
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		super.setTexCoordPointer(size, type, stride, bufferSize, buffer);
		re.checkAndLogErrors("setTexCoordPointer");
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, long offset) {
		super.setVertexPointer(size, type, stride, offset);
		re.checkAndLogErrors("setVertexPointer");
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		super.setVertexPointer(size, type, stride, bufferSize, buffer);
		re.checkAndLogErrors("setVertexPointer");
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, long offset) {
		super.setVertexAttribPointer(id, size, type, normalized, stride, offset);
		re.checkAndLogErrors("setVertexAttribPointer");
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, int bufferSize, Buffer buffer) {
		super.setVertexAttribPointer(id, size, type, normalized, stride, bufferSize, buffer);
		re.checkAndLogErrors("setVertexAttribPointer");
	}

	@Override
	public void setPixelStore(int rowLength, int alignment) {
		super.setPixelStore(rowLength, alignment);
		re.checkAndLogErrors("setPixelStore");
	}

	@Override
	public int genTexture() {
		int value = super.genTexture();
		re.checkAndLogErrors("genTexture");
		return value;
	}

	@Override
	public void bindTexture(int texture) {
		super.bindTexture(texture);
		re.checkAndLogErrors("bindTexture");
	}

	@Override
	public void deleteTexture(int texture) {
		super.deleteTexture(texture);
		re.checkAndLogErrors("deleteTexture");
	}

	@Override
	public void setCompressedTexImage(int level, int internalFormat, int width, int height, int compressedSize, Buffer buffer) {
		super.setCompressedTexImage(level, internalFormat, width, height, compressedSize, buffer);
		re.checkAndLogErrors("setCompressedTexImage");
	}

	@Override
	public void setTexImage(int level, int internalFormat, int width, int height, int format, int type, int textureSize, Buffer buffer) {
		super.setTexImage(level, internalFormat, width, height, format, type, textureSize, buffer);
		re.checkAndLogErrors("setTexImage");
	}

	@Override
	public void setStencilOp(int fail, int zfail, int zpass) {
		super.setStencilOp(fail, zfail, zpass);
		re.checkAndLogErrors("setStencilOp");
	}

	@Override
	public void setStencilFunc(int func, int ref, int mask) {
		super.setStencilFunc(func, ref, mask);
		re.checkAndLogErrors("setStencilFunc");
	}

	@Override
	public void setAlphaFunc(int func, int ref) {
		super.setAlphaFunc(func, ref);
		re.checkAndLogErrors("setAlphaFunc");
	}

	@Override
	public void setBlendEquation(int mode) {
		super.setBlendEquation(mode);
		re.checkAndLogErrors("setBlendEquation");
	}

	@Override
	public void setFogColor(float[] color) {
		super.setFogColor(color);
		re.checkAndLogErrors("setFogColor");
	}

	@Override
	public void setFogDist(float start, float end) {
		super.setFogDist(start, end);
		re.checkAndLogErrors("setFogDist");
	}

	@Override
	public void setFrontFace(boolean cw) {
		super.setFrontFace(cw);
		re.checkAndLogErrors("setFrontFace");
	}

	@Override
	public void setScissor(int x, int y, int width, int height) {
		super.setScissor(x, y, width, height);
		re.checkAndLogErrors("setScissor");
	}

	@Override
	public void setTextureEnvColor(float[] color) {
		super.setTextureEnvColor(color);
		re.checkAndLogErrors("setTextureEnvColor");
	}

	@Override
	public void setFogHint() {
		super.setFogHint();
		re.checkAndLogErrors("setFogHint");
	}

	@Override
	public void setLineSmoothHint() {
		super.setLineSmoothHint();
		re.checkAndLogErrors("setLineSmoothHint");
	}

	@Override
	public void setMaterialShininess(float shininess) {
		super.setMaterialShininess(shininess);
		re.checkAndLogErrors("setMaterialShininess");
	}

	@Override
	public void setTexSubImage(int level, int xOffset, int yOffset, int width, int height, int format, int type, int textureSize, Buffer buffer) {
		super.setTexSubImage(level, xOffset, yOffset, width, height, format, type, textureSize, buffer);
		re.checkAndLogErrors("setTexSubImage");
	}

	@Override
	public void beginQuery(int id) {
		super.beginQuery(id);
		re.checkAndLogErrors("beginQuery");
	}

	@Override
	public void endQuery() {
		super.endQuery();
		re.checkAndLogErrors("endQuery");
	}

	@Override
	public int genQuery() {
		int value = super.genQuery();
		re.checkAndLogErrors("genQuery");
		return value;
	}

	@Override
	public void drawBoundingBox(float[][] values) {
		super.drawBoundingBox(values);
		re.checkAndLogErrors("drawBoundingBox");
	}

	@Override
	public void endBoundingBox(VertexInfo vinfo) {
		super.endBoundingBox(vinfo);
		re.checkAndLogErrors("endBoundingBox");
	}

	@Override
	public void beginBoundingBox(int numberOfVertexBoundingBox) {
		super.beginBoundingBox(numberOfVertexBoundingBox);
		re.checkAndLogErrors("beginBoundingBox");
	}

	@Override
	public boolean hasBoundingBox() {
		boolean value = super.hasBoundingBox();
		re.checkAndLogErrors("hasBoundingBox");
		return value;
	}

	@Override
	public boolean isBoundingBoxVisible() {
		boolean value = super.isBoundingBoxVisible();
		re.checkAndLogErrors("isBoundingBoxVisible");
		return value;
	}

	@Override
	public int getQueryResult(int id) {
		int value = super.getQueryResult(id);
		re.checkAndLogErrors("getQueryResult");
		return value;
	}

	@Override
	public boolean getQueryResultAvailable(int id) {
		boolean value = super.getQueryResultAvailable(id);
		re.checkAndLogErrors("getQueryResultAvailable");
		return value;
	}

	@Override
	public void clear(float red, float green, float blue, float alpha) {
		super.clear(red, green, blue, alpha);
		re.checkAndLogErrors("clear");
	}

	@Override
	public void copyTexSubImage(int level, int xOffset, int yOffset, int x, int y, int width, int height) {
		super.copyTexSubImage(level, xOffset, yOffset, x, y, width, height);
		re.checkAndLogErrors("copyTexSubImage");
	}

	@Override
	public void getTexImage(int level, int format, int type, Buffer buffer) {
		super.getTexImage(level, format, type, buffer);
		re.checkAndLogErrors("getTexImage");
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, long offset) {
		super.setWeightPointer(size, type, stride, offset);
		re.checkAndLogErrors("setWeightPointer");
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		super.setWeightPointer(size, type, stride, bufferSize, buffer);
		re.checkAndLogErrors("setWeightPointer");
	}

	@Override
	public IREBufferManager getBufferManager() {
		IREBufferManager value = super.getBufferManager();
		re.checkAndLogErrors("getBufferManager");
		return value;
	}

	@Override
	public boolean canAllNativeVertexInfo() {
		boolean value = super.canAllNativeVertexInfo();
		re.checkAndLogErrors("canAllNativeVertexInfo");
		return value;
	}

	@Override
	public boolean canNativeSpritesPrimitive() {
		boolean value = super.canNativeSpritesPrimitive();
		re.checkAndLogErrors("canNativeSpritesPrimitive");
		return value;
	}

	@Override
	public void setVertexInfo(VertexInfo vinfo, boolean allNativeVertexInfo, boolean useVertexColor, boolean useTexture, int type) {
		super.setVertexInfo(vinfo, allNativeVertexInfo, useVertexColor, useTexture, type);
		re.checkAndLogErrors("setVertexInfo");
	}

	@Override
	public void setProgramParameter(int program, int parameter, int value) {
		super.setProgramParameter(program, parameter, value);
		re.checkAndLogErrors("setProgramParameter");
	}

	@Override
	public boolean isQueryAvailable() {
		boolean value = super.isQueryAvailable();
		re.checkAndLogErrors("isQueryAvailable");
		return value;
	}

	@Override
	public boolean isShaderAvailable() {
		boolean value = super.isShaderAvailable();
		re.checkAndLogErrors("isShaderAvailable");
		return value;
	}

	@Override
	public void bindBufferBase(int target, int bindingPoint, int buffer) {
		super.bindBufferBase(target, bindingPoint, buffer);
		re.checkAndLogErrors("bindBufferBase");
	}

	@Override
	public int getUniformBlockIndex(int program, String name) {
		int value = super.getUniformBlockIndex(program, name);
		re.checkAndLogErrors("getUniformBlockIndex");
		return value;
	}

	@Override
	public void setUniformBlockBinding(int program, int blockIndex, int bindingPoint) {
		super.setUniformBlockBinding(program, blockIndex, bindingPoint);
		re.checkAndLogErrors("setUniformBlockBinding");
	}

	@Override
	public int getUniformIndex(int program, String name) {
		int value = super.getUniformIndex(program, name);
		re.checkAndLogErrors("getUniformIndex");
		return value;
	}

	@Override
	public int[] getUniformIndices(int program, String[] names) {
		int[] value = super.getUniformIndices(program, names);
		re.checkAndLogErrors("getUniformIndices");
		return value;
	}

	@Override
	public int getActiveUniformOffset(int program, int uniformIndex) {
		int value = super.getActiveUniformOffset(program, uniformIndex);
		re.checkAndLogErrors("getActiveUniformOffset");
		return value;
	}

	@Override
	public void bindFramebuffer(int target, int framebuffer) {
		super.bindFramebuffer(target, framebuffer);
		re.checkAndLogErrors("bindFramebuffer");
	}

	@Override
	public void bindRenderbuffer(int renderbuffer) {
		super.bindRenderbuffer(renderbuffer);
		re.checkAndLogErrors("bindRenderbuffer");
	}

	@Override
	public void deleteFramebuffer(int framebuffer) {
		super.deleteFramebuffer(framebuffer);
		re.checkAndLogErrors("deleteFramebuffer");
	}

	@Override
	public void deleteRenderbuffer(int renderbuffer) {
		super.deleteRenderbuffer(renderbuffer);
		re.checkAndLogErrors("deleteRenderbuffer");
	}

	@Override
	public int genFramebuffer() {
		int value = super.genFramebuffer();
		re.checkAndLogErrors("genFramebuffer");
		return value;
	}

	@Override
	public int genRenderbuffer() {
		int value = super.genRenderbuffer();
		re.checkAndLogErrors("genRenderbuffer");
		return value;
	}

	@Override
	public boolean isFramebufferObjectAvailable() {
		boolean value = super.isFramebufferObjectAvailable();
		re.checkAndLogErrors("isFramebufferObjectAvailable");
		return value;
	}

	@Override
	public void setFramebufferRenderbuffer(int target, int attachment, int renderbuffer) {
		super.setFramebufferRenderbuffer(target, attachment, renderbuffer);
		re.checkAndLogErrors("setFramebufferRenderbuffer");
	}

	@Override
	public void setFramebufferTexture(int target, int attachment, int texture, int level) {
		super.setFramebufferTexture(target, attachment, texture, level);
		re.checkAndLogErrors("setFramebufferTexture");
	}

	@Override
	public void setRenderbufferStorage(int internalFormat, int width, int height) {
		super.setRenderbufferStorage(internalFormat, width, height);
		re.checkAndLogErrors("setRenderbufferStorage");
	}

	@Override
	public void bindVertexArray(int id) {
		super.bindVertexArray(id);
		re.checkAndLogErrors("bindVertexArray");
	}

	@Override
	public void deleteVertexArray(int id) {
		super.deleteVertexArray(id);
		re.checkAndLogErrors("deleteVertexArray");
	}

	@Override
	public int genVertexArray() {
		int value = super.genVertexArray();
		re.checkAndLogErrors("genVertexArray");
		return value;
	}

	@Override
	public boolean isVertexArrayAvailable() {
		boolean value = super.isVertexArrayAvailable();
		re.checkAndLogErrors("isVertexArrayAvailable");
		return value;
	}

	@Override
	public void multiDrawArrays(int primitive, IntBuffer first, IntBuffer count) {
		super.multiDrawArrays(primitive, first, count);
		re.checkAndLogErrors("multiDrawArrays");
	}

	@Override
	public void drawArraysBurstMode(int primitive, int first, int count) {
		super.drawArraysBurstMode(primitive, first, count);
		re.checkAndLogErrors("drawArraysBurstMode");
	}

	@Override
	public void setPixelTransfer(int parameter, int value) {
		super.setPixelTransfer(parameter, value);
		re.checkAndLogErrors("setPixelTransfer");
	}

	@Override
	public void setPixelTransfer(int parameter, float value) {
		super.setPixelTransfer(parameter, value);
		re.checkAndLogErrors("setPixelTransfer");
	}

	@Override
	public void setPixelTransfer(int parameter, boolean value) {
		super.setPixelTransfer(parameter, value);
		re.checkAndLogErrors("setPixelTransfer");
	}

	@Override
	public void setPixelMap(int map, int mapSize, Buffer buffer) {
		super.setPixelMap(map, mapSize, buffer);
		re.checkAndLogErrors("setPixelMap");
	}

	@Override
	public boolean canNativeClut(int textureAddress, boolean textureSwizzle) {
		boolean value = super.canNativeClut(textureAddress, textureSwizzle);
		re.checkAndLogErrors("canNativeClut");
		return value;
	}

	@Override
	public void setActiveTexture(int index) {
		super.setActiveTexture(index);
		re.checkAndLogErrors("setActiveTexture");
	}

	@Override
	public void setTextureFormat(int pixelFormat, boolean swizzle) {
		super.setTextureFormat(pixelFormat, swizzle);
		re.checkAndLogErrors("setTextureFormat");
	}

	@Override
	public void bindActiveTexture(int index, int texture) {
		super.bindActiveTexture(index, texture);
		re.checkAndLogErrors("bindActiveTexture");
	}

	@Override
	public float getMaxTextureAnisotropy() {
		float value = super.getMaxTextureAnisotropy();
		re.checkAndLogErrors("getMaxTextureAnisotropy");
		return value;
	}

	@Override
	public void setTextureAnisotropy(float value) {
		super.setTextureAnisotropy(value);
		re.checkAndLogErrors("setTextureAnisotropy");
	}

	@Override
	public String getShadingLanguageVersion() {
		String value = super.getShadingLanguageVersion();
		re.checkAndLogErrors("getShadingLanguageVersion");
		return value;
	}

	@Override
	public void setBlendDFix(int sfix, float[] color) {
		super.setBlendDFix(sfix, color);
		re.checkAndLogErrors("setBlendDFix");
	}

	@Override
	public void setBlendSFix(int dfix, float[] color) {
		super.setBlendSFix(dfix, color);
		re.checkAndLogErrors("setBlendSFix");
	}

	@Override
	public void waitForRenderingCompletion() {
		super.waitForRenderingCompletion();
		re.checkAndLogErrors("waitForRenderingCompletion");
	}

	@Override
	public boolean canReadAllVertexInfo() {
		boolean value = super.canReadAllVertexInfo();
		re.checkAndLogErrors("canReadAllVertexInfo");
		return value;
	}

	@Override
	public void readStencil(int x, int y, int width, int height, int bufferSize, Buffer buffer) {
		super.readStencil(x, y, width, height, bufferSize, buffer);
		re.checkAndLogErrors("readStencil");
	}

	@Override
	public void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
		super.blitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
		re.checkAndLogErrors("blitFramebuffer");
	}

	@Override
	public boolean setCopyRedToAlpha(boolean copyRedToAlpha) {
		boolean value = super.setCopyRedToAlpha(copyRedToAlpha);
		re.checkAndLogErrors("setCopyRedToAlpha");
		return value;
	}

	@Override
	public void drawElements(int primitive, int count, int indexType, Buffer indices, int indicesOffset) {
		super.drawElements(primitive, count, indexType, indices, indicesOffset);
		re.checkAndLogErrors("drawElements");
	}

	@Override
	public void drawElements(int primitive, int count, int indexType, long indicesOffset) {
		super.drawElements(primitive, count, indexType, indicesOffset);
		re.checkAndLogErrors("drawElements");
	}

	@Override
	public void multiDrawElements(int primitive, IntBuffer first, IntBuffer count, int indexType, long indicesOffset) {
		super.multiDrawElements(primitive, first, count, indexType, indicesOffset);
		re.checkAndLogErrors("multiDrawElements");
	}

	@Override
	public void drawElementsBurstMode(int primitive, int count, int indexType, long indicesOffset) {
		super.drawElementsBurstMode(primitive, count, indexType, indicesOffset);
		re.checkAndLogErrors("drawElementsBurstMode");
	}
}
