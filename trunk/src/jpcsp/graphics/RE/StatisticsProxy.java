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
import java.util.Arrays;

import jpcsp.graphics.GeContext;
import jpcsp.graphics.VertexInfo;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.buffer.IREBufferManager;
import jpcsp.util.CpuDurationStatistics;
import jpcsp.util.DurationStatistics;

/**
 * @author gid15
 *
 */
public class StatisticsProxy extends BaseRenderingEngineProxy {
	private DurationStatistics[] statistics;

	public StatisticsProxy(IRenderingEngine proxy) {
		super(proxy);

		addStatistic("attachShader", 0);
		addStatistic("beginBoundingBox", 1);
		addStatistic("beginDraw", 2);
		addStatistic("beginQuery", 3);
		addStatistic("bindBuffer", 4);
		addStatistic("bindBufferBase", 5);
		addStatistic("bindFramebuffer", 6);
		addStatistic("bindRenderbuffer", 7);
		addStatistic("bindTexture", 8);
		addStatistic("canAllNativeVertexInfo", 9);
		addStatistic("canNativeSpritesPrimitive", 10);
		addStatistic("clear", 11);
		addStatistic("compilerShader", 12);
		addStatistic("copyTexSubImage", 13);
		addStatistic("createProgram", 14);
		addStatistic("createShader", 15);
		addStatistic("deleteBuffer", 16);
		addStatistic("deleteFramebuffer", 17);
		addStatistic("deleteRenderbuffer", 18);
		addStatistic("deleteTexture", 19);
		addStatistic("disableClientState", 20);
		addStatistic("disableFlag", 21);
		addStatistic("disableVertexAttribArray", 22);
		addStatistic("drawArrays", 23);
		addStatistic("drawBoundingBox", 24);
		addStatistic("drawColor", 25);
		addStatistic("drawColor", 26);
		addStatistic("drawTexCoord", 27);
		addStatistic("drawVertex", 28);
		addStatistic("drawVertex", 29);
		addStatistic("drawVertex3", 30);
		addStatistic("enableClientState", 31);
		addStatistic("enableFlag", 32);
		addStatistic("enableVertexAttribArray", 33);
		addStatistic("endBoundingBox", 34);
		addStatistic("endClearMode", 35);
		addStatistic("endDirectRendering", 36);
		addStatistic("endDisplay", 37);
		addStatistic("endDraw", 38);
		addStatistic("endModelViewMatrixUpdate", 39);
		addStatistic("endQuery", 40);
		addStatistic("genBuffer", 41);
		addStatistic("genFramebuffer", 42);
		addStatistic("genQuery", 43);
		addStatistic("genRenderbuffer", 44);
		addStatistic("genTexture", 45);
		addStatistic("getAttribLocation", 46);
		addStatistic("getBufferManager", 47);
		addStatistic("getProgramInfoLog", 48);
		addStatistic("getQueryResult", 49);
		addStatistic("getQueryResultAvailable", 50);
		addStatistic("getShaderInfoLog", 51);
		addStatistic("getTexImage", 52);
		addStatistic("getUniformBlockIndex", 53);
		addStatistic("getUniformLocation", 54);
		addStatistic("hasBoundingBox", 55);
		addStatistic("isBoundingBoxVisible", 56);
		addStatistic("isExtensionAvailable", 57);
		addStatistic("isFramebufferObjectAvailable", 58);
		addStatistic("isQueryAvailable", 59);
		addStatistic("isShaderAvailable", 60);
		addStatistic("linkProgram", 61);
		addStatistic("readPixels", 62);
		addStatistic("setAlphaFunc", 63);
		addStatistic("setBlendColor", 64);
		addStatistic("setBlendEquation", 65);
		addStatistic("setBlendFunc", 66);
		addStatistic("setBones", 67);
		addStatistic("setBufferData", 68);
		addStatistic("setBufferSubData", 69);
		addStatistic("setColorMask", 70);
		addStatistic("setColorMask", 71);
		addStatistic("setColorMaterial", 72);
		addStatistic("setColorPointer", 73);
		addStatistic("setColorPointer", 74);
		addStatistic("setColorTestFunc", 75);
		addStatistic("setColorTestMask", 76);
		addStatistic("setColorTestReference", 77);
		addStatistic("setCompressedTexImage", 78);
		addStatistic("setDepthFunc", 79);
		addStatistic("setDepthMask", 80);
		addStatistic("setDepthRange", 81);
		addStatistic("setFogColor", 82);
		addStatistic("setFogDist", 83);
		addStatistic("setFogHint", 84);
		addStatistic("setFramebufferRenderbuffer", 85);
		addStatistic("setFramebufferTexture", 86);
		addStatistic("setFrontFace", 87);
		addStatistic("setGeContext", 88);
		addStatistic("setLightAmbientColor", 89);
		addStatistic("setLightColor", 90);
		addStatistic("setLightConstantAttenuation", 91);
		addStatistic("setLightDiffuseColor", 92);
		addStatistic("setLightDirection", 93);
		addStatistic("setLightLinearAttenuation", 94);
		addStatistic("setLightMode", 95);
		addStatistic("setLightModelAmbientColor", 96);
		addStatistic("setLightPosition", 97);
		addStatistic("setLightQuadraticAttenuation", 98);
		addStatistic("setLightSpecularColor", 99);
		addStatistic("setLightSpotCutoff", 100);
		addStatistic("setLightSpotExponent", 101);
		addStatistic("setLightType", 102);
		addStatistic("setLineSmoothHint", 103);
		addStatistic("setLogicOp", 104);
		addStatistic("setMaterialAmbientColor", 105);
		addStatistic("setMaterialColor", 106);
		addStatistic("setMaterialDiffuseColor", 107);
		addStatistic("setMaterialEmissiveColor", 108);
		addStatistic("setMaterialShininess", 109);
		addStatistic("setMaterialSpecularColor", 110);
		addStatistic("setMatrix", 111);
		addStatistic("setModelMatrix", 112);
		addStatistic("setModelViewMatrix", 113);
		addStatistic("setMorphWeight", 114);
		addStatistic("setNormalPointer", 115);
		addStatistic("setNormalPointer", 116);
		addStatistic("setPatchDiv", 117);
		addStatistic("setPatchPrim", 118);
		addStatistic("setPixelStore", 119);
		addStatistic("setProgramParameter", 120);
		addStatistic("setProjectionMatrix", 121);
		addStatistic("setRenderbufferStorage", 122);
		addStatistic("setRenderingEngine", 123);
		addStatistic("setScissor", 124);
		addStatistic("setShadeModel", 125);
		addStatistic("setStencilFunc", 126);
		addStatistic("setStencilOp", 127);
		addStatistic("setTexCoordPointer", 128);
		addStatistic("setTexCoordPointer", 129);
		addStatistic("setTexEnv", 130);
		addStatistic("setTexEnv", 131);
		addStatistic("setTexImage", 132);
		addStatistic("setTexSubImage", 133);
		addStatistic("setTextureEnvColor", 134);
		addStatistic("setTextureEnvironmentMapping", 135);
		addStatistic("setTextureFunc", 136);
		addStatistic("setTextureMapMode", 137);
		addStatistic("setTextureMatrix", 138);
		addStatistic("setTextureMipmapMagFilter", 139);
		addStatistic("setTextureMipmapMaxLevel", 140);
		addStatistic("setTextureMipmapMinFilter", 141);
		addStatistic("setTextureMipmapMinLevel", 142);
		addStatistic("setTextureWrapMode", 143);
		addStatistic("setUniform", 144);
		addStatistic("setUniform", 145);
		addStatistic("setUniform", 146);
		addStatistic("setUniform2", 147);
		addStatistic("setUniform3", 148);
		addStatistic("setUniform4", 149);
		addStatistic("setUniformBlockBinding", 150);
		addStatistic("setUniformMatrix4", 151);
		addStatistic("setVertexAttribPointer", 152);
		addStatistic("setVertexAttribPointer", 153);
		addStatistic("setVertexColor", 154);
		addStatistic("setVertexInfo", 155);
		addStatistic("setVertexPointer", 156);
		addStatistic("setVertexPointer", 157);
		addStatistic("setViewMatrix", 158);
		addStatistic("setViewport", 159);
		addStatistic("setWeightPointer", 160);
		addStatistic("setWeightPointer", 161);
		addStatistic("startClearMode", 162);
		addStatistic("startDirectRendering", 163);
		addStatistic("startDisplay", 164);
		addStatistic("useProgram", 165);
		addStatistic("validateProgram", 166);
		addStatistic("bindVertexArray", 167);
		addStatistic("deleteVertexArray", 168);
		addStatistic("genVertexArray", 169);
		addStatistic("isVertexArrayAvailable", 170);
		addStatistic("multiDrawArrays", 171);
		addStatistic("multMatrix", 172);
		addStatistic("setMatrix", 173);
		addStatistic("setMatrixMode", 174);
		addStatistic("setPixelTransfer", 175);
		addStatistic("setPixelTransfer", 176);
		addStatistic("setPixelTransfer", 177);
		addStatistic("setPixelMap", 178);
		addStatistic("canNativeClut", 179);
		addStatistic("setActiveTexture", 180);
		addStatistic("setTextureFormat", 181);
		addStatistic("getUniformIndex", 182);
		addStatistic("getUniformIndices", 183);
		addStatistic("getActiveUniformOffset", 184);
		addStatistic("bindAttribLocation", 185);
		addStatistic("setUniform4", 186);
		addStatistic("drawArraysBurstMode", 187);
		addStatistic("bindActiveTexture", 188);
	}

	private void addStatistic(String name, int index) {
		if (statistics == null || index >= statistics.length) {
			DurationStatistics[] newStatistics = new DurationStatistics[index + 1];
			if (statistics != null) {
				System.arraycopy(statistics, 0, newStatistics, 0, statistics.length);
			}
			statistics = newStatistics;
		}
		statistics[index] = new CpuDurationStatistics(String.format("%-30s", name));
	}

	@Override
	public void exit() {
		Arrays.sort(statistics);
		VideoEngine.log.info("RenderingEngine methods:");

		int lastStatistics = -1;
		for (int i = statistics.length - 1; i >= 0; i--) {
			if (statistics[i].numberCalls > 0) {
				lastStatistics = i;
				break;
			}
		}

		for (int i = 0; i <= lastStatistics; i++) {
			DurationStatistics statistic = statistics[i];
			if (statistic.numberCalls == 0) {
				break;
			}
			VideoEngine.log.info("    " + statistic);
		}

		super.exit();
	}

	@Override
	public void attachShader(int program, int shader) {
		DurationStatistics statistic = statistics[0];
		statistic.start();
		super.attachShader(program, shader);
		statistic.end();
	}

	@Override
	public void beginBoundingBox(int numberOfVertexBoundingBox) {
		DurationStatistics statistic = statistics[1];
		statistic.start();
		super.beginBoundingBox(numberOfVertexBoundingBox);
		statistic.end();
	}

	@Override
	public void beginDraw(int type) {
		DurationStatistics statistic = statistics[2];
		statistic.start();
		super.beginDraw(type);
		statistic.end();
	}

	@Override
	public void beginQuery(int id) {
		DurationStatistics statistic = statistics[3];
		statistic.start();
		super.beginQuery(id);
		statistic.end();
	}

	@Override
	public void bindBuffer(int target, int buffer) {
		DurationStatistics statistic = statistics[4];
		statistic.start();
		super.bindBuffer(target, buffer);
		statistic.end();
	}

	@Override
	public void bindBufferBase(int target, int bindingPoint, int buffer) {
		DurationStatistics statistic = statistics[5];
		statistic.start();
		super.bindBufferBase(target, bindingPoint, buffer);
		statistic.end();
	}

	@Override
	public void bindFramebuffer(int target, int framebuffer) {
		DurationStatistics statistic = statistics[6];
		statistic.start();
		super.bindFramebuffer(target, framebuffer);
		statistic.end();
	}

	@Override
	public void bindRenderbuffer(int renderbuffer) {
		DurationStatistics statistic = statistics[7];
		statistic.start();
		super.bindRenderbuffer(renderbuffer);
		statistic.end();
	}

	@Override
	public void bindTexture(int texture) {
		DurationStatistics statistic = statistics[8];
		statistic.start();
		super.bindTexture(texture);
		statistic.end();
	}

	@Override
	public boolean canAllNativeVertexInfo() {
		DurationStatistics statistic = statistics[9];
		statistic.start();
		boolean value = super.canAllNativeVertexInfo();
		statistic.end();
		return value;
	}

	@Override
	public boolean canNativeSpritesPrimitive() {
		DurationStatistics statistic = statistics[10];
		statistic.start();
		boolean value = super.canNativeSpritesPrimitive();
		statistic.end();
		return value;
	}

	@Override
	public void clear(float red, float green, float blue, float alpha) {
		DurationStatistics statistic = statistics[11];
		statistic.start();
		super.clear(red, green, blue, alpha);
		statistic.end();
	}

	@Override
	public boolean compilerShader(int shader, String source) {
		DurationStatistics statistic = statistics[12];
		statistic.start();
		boolean value = super.compilerShader(shader, source);
		statistic.end();
		return value;
	}

	@Override
	public void copyTexSubImage(int level, int offset, int offset2, int x, int y, int width, int height) {
		DurationStatistics statistic = statistics[13];
		statistic.start();
		super.copyTexSubImage(level, offset, offset2, x, y, width, height);
		statistic.end();
	}

	@Override
	public int createProgram() {
		DurationStatistics statistic = statistics[14];
		statistic.start();
		int value = super.createProgram();
		statistic.end();
		return value;
	}

	@Override
	public int createShader(int type) {
		DurationStatistics statistic = statistics[15];
		statistic.start();
		int value = super.createShader(type);
		statistic.end();
		return value;
	}

	@Override
	public void deleteBuffer(int buffer) {
		DurationStatistics statistic = statistics[16];
		statistic.start();
		super.deleteBuffer(buffer);
		statistic.end();
	}

	@Override
	public void deleteFramebuffer(int framebuffer) {
		DurationStatistics statistic = statistics[17];
		statistic.start();
		super.deleteFramebuffer(framebuffer);
		statistic.end();
	}

	@Override
	public void deleteRenderbuffer(int renderbuffer) {
		DurationStatistics statistic = statistics[18];
		statistic.start();
		super.deleteRenderbuffer(renderbuffer);
		statistic.end();
	}

	@Override
	public void deleteTexture(int texture) {
		DurationStatistics statistic = statistics[19];
		statistic.start();
		super.deleteTexture(texture);
		statistic.end();
	}

	@Override
	public void disableClientState(int type) {
		DurationStatistics statistic = statistics[20];
		statistic.start();
		super.disableClientState(type);
		statistic.end();
	}

	@Override
	public void disableFlag(int flag) {
		DurationStatistics statistic = statistics[21];
		statistic.start();
		super.disableFlag(flag);
		statistic.end();
	}

	@Override
	public void disableVertexAttribArray(int id) {
		DurationStatistics statistic = statistics[22];
		statistic.start();
		super.disableVertexAttribArray(id);
		statistic.end();
	}

	@Override
	public void drawArrays(int type, int first, int count) {
		DurationStatistics statistic = statistics[23];
		statistic.start();
		super.drawArrays(type, first, count);
		statistic.end();
	}

	@Override
	public void drawBoundingBox(float[][] values) {
		DurationStatistics statistic = statistics[24];
		statistic.start();
		super.drawBoundingBox(values);
		statistic.end();
	}

	@Override
	public void drawColor(float value1, float value2, float value3, float value4) {
		DurationStatistics statistic = statistics[25];
		statistic.start();
		super.drawColor(value1, value2, value3, value4);
		statistic.end();
	}

	@Override
	public void drawColor(float value1, float value2, float value3) {
		DurationStatistics statistic = statistics[26];
		statistic.start();
		super.drawColor(value1, value2, value3);
		statistic.end();
	}

	@Override
	public void drawTexCoord(float value1, float value2) {
		DurationStatistics statistic = statistics[27];
		statistic.start();
		super.drawTexCoord(value1, value2);
		statistic.end();
	}

	@Override
	public void drawVertex(float value1, float value2) {
		DurationStatistics statistic = statistics[28];
		statistic.start();
		super.drawVertex(value1, value2);
		statistic.end();
	}

	@Override
	public void drawVertex(int value1, int value2) {
		DurationStatistics statistic = statistics[29];
		statistic.start();
		super.drawVertex(value1, value2);
		statistic.end();
	}

	@Override
	public void drawVertex3(float[] values) {
		DurationStatistics statistic = statistics[30];
		statistic.start();
		super.drawVertex3(values);
		statistic.end();
	}

	@Override
	public void enableClientState(int type) {
		DurationStatistics statistic = statistics[31];
		statistic.start();
		super.enableClientState(type);
		statistic.end();
	}

	@Override
	public void enableFlag(int flag) {
		DurationStatistics statistic = statistics[32];
		statistic.start();
		super.enableFlag(flag);
		statistic.end();
	}

	@Override
	public void enableVertexAttribArray(int id) {
		DurationStatistics statistic = statistics[33];
		statistic.start();
		super.enableVertexAttribArray(id);
		statistic.end();
	}

	@Override
	public void endBoundingBox() {
		DurationStatistics statistic = statistics[34];
		statistic.start();
		super.endBoundingBox();
		statistic.end();
	}

	@Override
	public void endClearMode() {
		DurationStatistics statistic = statistics[35];
		statistic.start();
		super.endClearMode();
		statistic.end();
	}

	@Override
	public void endDirectRendering() {
		DurationStatistics statistic = statistics[36];
		statistic.start();
		super.endDirectRendering();
		statistic.end();
	}

	@Override
	public void endDisplay() {
		DurationStatistics statistic = statistics[37];
		statistic.start();
		super.endDisplay();
		statistic.end();
	}

	@Override
	public void endDraw() {
		DurationStatistics statistic = statistics[38];
		statistic.start();
		super.endDraw();
		statistic.end();
	}

	@Override
	public void endModelViewMatrixUpdate() {
		DurationStatistics statistic = statistics[39];
		statistic.start();
		super.endModelViewMatrixUpdate();
		statistic.end();
	}

	@Override
	public void endQuery() {
		DurationStatistics statistic = statistics[40];
		statistic.start();
		super.endQuery();
		statistic.end();
	}

	@Override
	public int genBuffer() {
		DurationStatistics statistic = statistics[41];
		statistic.start();
		int value = super.genBuffer();
		statistic.end();
		return value;
	}

	@Override
	public int genFramebuffer() {
		DurationStatistics statistic = statistics[42];
		statistic.start();
		int value = super.genFramebuffer();
		statistic.end();
		return value;
	}

	@Override
	public int genQuery() {
		DurationStatistics statistic = statistics[43];
		statistic.start();
		int value = super.genQuery();
		statistic.end();
		return value;
	}

	@Override
	public int genRenderbuffer() {
		DurationStatistics statistic = statistics[44];
		statistic.start();
		int value = super.genRenderbuffer();
		statistic.end();
		return value;
	}

	@Override
	public int genTexture() {
		DurationStatistics statistic = statistics[45];
		statistic.start();
		int value = super.genTexture();
		statistic.end();
		return value;
	}

	@Override
	public int getAttribLocation(int program, String name) {
		DurationStatistics statistic = statistics[46];
		statistic.start();
		int value = super.getAttribLocation(program, name);
		statistic.end();
		return value;
	}

	@Override
	public IREBufferManager getBufferManager() {
		DurationStatistics statistic = statistics[47];
		statistic.start();
		IREBufferManager value = super.getBufferManager();
		statistic.end();
		return value;
	}

	@Override
	public String getProgramInfoLog(int program) {
		DurationStatistics statistic = statistics[48];
		statistic.start();
		String value = super.getProgramInfoLog(program);
		statistic.end();
		return value;
	}

	@Override
	public int getQueryResult(int id) {
		DurationStatistics statistic = statistics[49];
		statistic.start();
		int value = super.getQueryResult(id);
		statistic.end();
		return value;
	}

	@Override
	public boolean getQueryResultAvailable(int id) {
		DurationStatistics statistic = statistics[50];
		statistic.start();
		boolean value = super.getQueryResultAvailable(id);
		statistic.end();
		return value;
	}

	@Override
	public String getShaderInfoLog(int shader) {
		DurationStatistics statistic = statistics[51];
		statistic.start();
		String value = super.getShaderInfoLog(shader);
		statistic.end();
		return value;
	}

	@Override
	public void getTexImage(int level, int format, int type, Buffer buffer) {
		DurationStatistics statistic = statistics[52];
		statistic.start();
		super.getTexImage(level, format, type, buffer);
		statistic.end();
	}

	@Override
	public int getUniformBlockIndex(int program, String name) {
		DurationStatistics statistic = statistics[53];
		statistic.start();
		int value = super.getUniformBlockIndex(program, name);
		statistic.end();
		return value;
	}

	@Override
	public int getUniformLocation(int program, String name) {
		DurationStatistics statistic = statistics[54];
		statistic.start();
		int value = super.getUniformLocation(program, name);
		statistic.end();
		return value;
	}

	@Override
	public boolean hasBoundingBox() {
		DurationStatistics statistic = statistics[55];
		statistic.start();
		boolean value = super.hasBoundingBox();
		statistic.end();
		return value;
	}

	@Override
	public boolean isBoundingBoxVisible() {
		DurationStatistics statistic = statistics[56];
		statistic.start();
		boolean value = super.isBoundingBoxVisible();
		statistic.end();
		return value;
	}

	@Override
	public boolean isExtensionAvailable(String name) {
		DurationStatistics statistic = statistics[57];
		statistic.start();
		boolean value = super.isExtensionAvailable(name);
		statistic.end();
		return value;
	}

	@Override
	public boolean isFramebufferObjectAvailable() {
		DurationStatistics statistic = statistics[58];
		statistic.start();
		boolean value = super.isFramebufferObjectAvailable();
		statistic.end();
		return value;
	}

	@Override
	public boolean isQueryAvailable() {
		DurationStatistics statistic = statistics[59];
		statistic.start();
		boolean value = super.isQueryAvailable();
		statistic.end();
		return value;
	}

	@Override
	public boolean isShaderAvailable() {
		DurationStatistics statistic = statistics[60];
		statistic.start();
		boolean value = super.isShaderAvailable();
		statistic.end();
		return value;
	}

	@Override
	public void linkProgram(int program) {
		DurationStatistics statistic = statistics[61];
		statistic.start();
		super.linkProgram(program);
		statistic.end();
	}

	@Override
	public void readPixels(int x, int y, int width, int height, int format, int type, Buffer buffer) {
		DurationStatistics statistic = statistics[62];
		statistic.start();
		super.readPixels(x, y, width, height, format, type, buffer);
		statistic.end();
	}

	@Override
	public void setAlphaFunc(int func, int ref) {
		DurationStatistics statistic = statistics[63];
		statistic.start();
		super.setAlphaFunc(func, ref);
		statistic.end();
	}

	@Override
	public void setBlendColor(float[] color) {
		DurationStatistics statistic = statistics[64];
		statistic.start();
		super.setBlendColor(color);
		statistic.end();
	}

	@Override
	public void setBlendEquation(int mode) {
		DurationStatistics statistic = statistics[65];
		statistic.start();
		super.setBlendEquation(mode);
		statistic.end();
	}

	@Override
	public void setBlendFunc(int src, int dst) {
		DurationStatistics statistic = statistics[66];
		statistic.start();
		super.setBlendFunc(src, dst);
		statistic.end();
	}

	@Override
	public int setBones(int count, float[] values) {
		DurationStatistics statistic = statistics[67];
		statistic.start();
		int value = super.setBones(count, values);
		statistic.end();
		return value;
	}

	@Override
	public void setBufferData(int target, int size, Buffer buffer, int usage) {
		DurationStatistics statistic = statistics[68];
		statistic.start();
		super.setBufferData(target, size, buffer, usage);
		statistic.end();
	}

	@Override
	public void setBufferSubData(int target, int offset, int size, Buffer buffer) {
		DurationStatistics statistic = statistics[69];
		statistic.start();
		super.setBufferSubData(target, offset, size, buffer);
		statistic.end();
	}

	@Override
	public void setColorMask(boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled) {
		DurationStatistics statistic = statistics[70];
		statistic.start();
		super.setColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, alphaWriteEnabled);
		statistic.end();
	}

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
		DurationStatistics statistic = statistics[71];
		statistic.start();
		super.setColorMask(redMask, greenMask, blueMask, alphaMask);
		statistic.end();
	}

	@Override
	public void setColorMaterial(boolean ambient, boolean diffuse, boolean specular) {
		DurationStatistics statistic = statistics[72];
		statistic.start();
		super.setColorMaterial(ambient, diffuse, specular);
		statistic.end();
	}

	@Override
	public void setColorPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		DurationStatistics statistic = statistics[73];
		statistic.start();
		super.setColorPointer(size, type, stride, bufferSize, buffer);
		statistic.end();
	}

	@Override
	public void setColorPointer(int size, int type, int stride, long offset) {
		DurationStatistics statistic = statistics[74];
		statistic.start();
		super.setColorPointer(size, type, stride, offset);
		statistic.end();
	}

	@Override
	public void setColorTestFunc(int func) {
		DurationStatistics statistic = statistics[75];
		statistic.start();
		super.setColorTestFunc(func);
		statistic.end();
	}

	@Override
	public void setColorTestMask(int[] values) {
		DurationStatistics statistic = statistics[76];
		statistic.start();
		super.setColorTestMask(values);
		statistic.end();
	}

	@Override
	public void setColorTestReference(int[] values) {
		DurationStatistics statistic = statistics[77];
		statistic.start();
		super.setColorTestReference(values);
		statistic.end();
	}

	@Override
	public void setCompressedTexImage(int level, int internalFormat, int width, int height, int compressedSize, Buffer buffer) {
		DurationStatistics statistic = statistics[78];
		statistic.start();
		super.setCompressedTexImage(level, internalFormat, width, height, compressedSize, buffer);
		statistic.end();
	}

	@Override
	public void setDepthFunc(int func) {
		DurationStatistics statistic = statistics[79];
		statistic.start();
		super.setDepthFunc(func);
		statistic.end();
	}

	@Override
	public void setDepthMask(boolean depthWriteEnabled) {
		DurationStatistics statistic = statistics[80];
		statistic.start();
		super.setDepthMask(depthWriteEnabled);
		statistic.end();
	}

	@Override
	public void setDepthRange(float zpos, float zscale, float near, float far) {
		DurationStatistics statistic = statistics[81];
		statistic.start();
		super.setDepthRange(zpos, zscale, near, far);
		statistic.end();
	}

	@Override
	public void setFogColor(float[] color) {
		DurationStatistics statistic = statistics[82];
		statistic.start();
		super.setFogColor(color);
		statistic.end();
	}

	@Override
	public void setFogDist(float start, float end) {
		DurationStatistics statistic = statistics[83];
		statistic.start();
		super.setFogDist(start, end);
		statistic.end();
	}

	@Override
	public void setFogHint() {
		DurationStatistics statistic = statistics[84];
		statistic.start();
		super.setFogHint();
		statistic.end();
	}

	@Override
	public void setFramebufferRenderbuffer(int target, int attachment, int renderbuffer) {
		DurationStatistics statistic = statistics[85];
		statistic.start();
		super.setFramebufferRenderbuffer(target, attachment, renderbuffer);
		statistic.end();
	}

	@Override
	public void setFramebufferTexture(int target, int attachment, int texture, int level) {
		DurationStatistics statistic = statistics[86];
		statistic.start();
		super.setFramebufferTexture(target, attachment, texture, level);
		statistic.end();
	}

	@Override
	public void setFrontFace(boolean cw) {
		DurationStatistics statistic = statistics[87];
		statistic.start();
		super.setFrontFace(cw);
		statistic.end();
	}

	@Override
	public void setGeContext(GeContext context) {
		DurationStatistics statistic = statistics[88];
		statistic.start();
		super.setGeContext(context);
		statistic.end();
	}

	@Override
	public void setLightAmbientColor(int light, float[] color) {
		DurationStatistics statistic = statistics[89];
		statistic.start();
		super.setLightAmbientColor(light, color);
		statistic.end();
	}

	@Override
	public void setLightConstantAttenuation(int light, float constant) {
		DurationStatistics statistic = statistics[91];
		statistic.start();
		super.setLightConstantAttenuation(light, constant);
		statistic.end();
	}

	@Override
	public void setLightDiffuseColor(int light, float[] color) {
		DurationStatistics statistic = statistics[92];
		statistic.start();
		super.setLightDiffuseColor(light, color);
		statistic.end();
	}

	@Override
	public void setLightDirection(int light, float[] direction) {
		DurationStatistics statistic = statistics[93];
		statistic.start();
		super.setLightDirection(light, direction);
		statistic.end();
	}

	@Override
	public void setLightLinearAttenuation(int light, float linear) {
		DurationStatistics statistic = statistics[94];
		statistic.start();
		super.setLightLinearAttenuation(light, linear);
		statistic.end();
	}

	@Override
	public void setLightMode(int mode) {
		DurationStatistics statistic = statistics[95];
		statistic.start();
		super.setLightMode(mode);
		statistic.end();
	}

	@Override
	public void setLightModelAmbientColor(float[] color) {
		DurationStatistics statistic = statistics[96];
		statistic.start();
		super.setLightModelAmbientColor(color);
		statistic.end();
	}

	@Override
	public void setLightPosition(int light, float[] position) {
		DurationStatistics statistic = statistics[97];
		statistic.start();
		super.setLightPosition(light, position);
		statistic.end();
	}

	@Override
	public void setLightQuadraticAttenuation(int light, float quadratic) {
		DurationStatistics statistic = statistics[98];
		statistic.start();
		super.setLightQuadraticAttenuation(light, quadratic);
		statistic.end();
	}

	@Override
	public void setLightSpecularColor(int light, float[] color) {
		DurationStatistics statistic = statistics[99];
		statistic.start();
		super.setLightSpecularColor(light, color);
		statistic.end();
	}

	@Override
	public void setLightSpotCutoff(int light, float cutoff) {
		DurationStatistics statistic = statistics[100];
		statistic.start();
		super.setLightSpotCutoff(light, cutoff);
		statistic.end();
	}

	@Override
	public void setLightSpotExponent(int light, float exponent) {
		DurationStatistics statistic = statistics[101];
		statistic.start();
		super.setLightSpotExponent(light, exponent);
		statistic.end();
	}

	@Override
	public void setLightType(int light, int type, int kind) {
		DurationStatistics statistic = statistics[102];
		statistic.start();
		super.setLightType(light, type, kind);
		statistic.end();
	}

	@Override
	public void setLineSmoothHint() {
		DurationStatistics statistic = statistics[103];
		statistic.start();
		super.setLineSmoothHint();
		statistic.end();
	}

	@Override
	public void setLogicOp(int logicOp) {
		DurationStatistics statistic = statistics[104];
		statistic.start();
		super.setLogicOp(logicOp);
		statistic.end();
	}

	@Override
	public void setMaterialAmbientColor(float[] color) {
		DurationStatistics statistic = statistics[105];
		statistic.start();
		super.setMaterialAmbientColor(color);
		statistic.end();
	}

	@Override
	public void setMaterialDiffuseColor(float[] color) {
		DurationStatistics statistic = statistics[107];
		statistic.start();
		super.setMaterialDiffuseColor(color);
		statistic.end();
	}

	@Override
	public void setMaterialEmissiveColor(float[] color) {
		DurationStatistics statistic = statistics[108];
		statistic.start();
		super.setMaterialEmissiveColor(color);
		statistic.end();
	}

	@Override
	public void setMaterialShininess(float shininess) {
		DurationStatistics statistic = statistics[109];
		statistic.start();
		super.setMaterialShininess(shininess);
		statistic.end();
	}

	@Override
	public void setMaterialSpecularColor(float[] color) {
		DurationStatistics statistic = statistics[110];
		statistic.start();
		super.setMaterialSpecularColor(color);
		statistic.end();
	}

	@Override
	public void setModelMatrix(float[] values) {
		DurationStatistics statistic = statistics[112];
		statistic.start();
		super.setModelMatrix(values);
		statistic.end();
	}

	@Override
	public void setModelViewMatrix(float[] values) {
		DurationStatistics statistic = statistics[113];
		statistic.start();
		super.setModelViewMatrix(values);
		statistic.end();
	}

	@Override
	public void setMorphWeight(int index, float value) {
		DurationStatistics statistic = statistics[114];
		statistic.start();
		super.setMorphWeight(index, value);
		statistic.end();
	}

	@Override
	public void setNormalPointer(int type, int stride, int bufferSize, Buffer buffer) {
		DurationStatistics statistic = statistics[115];
		statistic.start();
		super.setNormalPointer(type, stride, bufferSize, buffer);
		statistic.end();
	}

	@Override
	public void setNormalPointer(int type, int stride, long offset) {
		DurationStatistics statistic = statistics[116];
		statistic.start();
		super.setNormalPointer(type, stride, offset);
		statistic.end();
	}

	@Override
	public void setPatchDiv(int s, int t) {
		DurationStatistics statistic = statistics[117];
		statistic.start();
		super.setPatchDiv(s, t);
		statistic.end();
	}

	@Override
	public void setPatchPrim(int prim) {
		DurationStatistics statistic = statistics[118];
		statistic.start();
		super.setPatchPrim(prim);
		statistic.end();
	}

	@Override
	public void setPixelStore(int rowLength, int alignment) {
		DurationStatistics statistic = statistics[119];
		statistic.start();
		super.setPixelStore(rowLength, alignment);
		statistic.end();
	}

	@Override
	public void setProgramParameter(int program, int parameter, int value) {
		DurationStatistics statistic = statistics[120];
		statistic.start();
		super.setProgramParameter(program, parameter, value);
		statistic.end();
	}

	@Override
	public void setProjectionMatrix(float[] values) {
		DurationStatistics statistic = statistics[121];
		statistic.start();
		super.setProjectionMatrix(values);
		statistic.end();
	}

	@Override
	public void setRenderbufferStorage(int renderbuffer, int internalFormat, int width, int height) {
		DurationStatistics statistic = statistics[122];
		statistic.start();
		super.setRenderbufferStorage(renderbuffer, internalFormat, width, height);
		statistic.end();
	}

	@Override
	public void setRenderingEngine(IRenderingEngine re) {
		DurationStatistics statistic = statistics[123];
		statistic.start();
		super.setRenderingEngine(re);
		statistic.end();
	}

	@Override
	public void setScissor(int x, int y, int width, int height) {
		DurationStatistics statistic = statistics[124];
		statistic.start();
		super.setScissor(x, y, width, height);
		statistic.end();
	}

	@Override
	public void setShadeModel(int model) {
		DurationStatistics statistic = statistics[125];
		statistic.start();
		super.setShadeModel(model);
		statistic.end();
	}

	@Override
	public void setStencilFunc(int func, int ref, int mask) {
		DurationStatistics statistic = statistics[126];
		statistic.start();
		super.setStencilFunc(func, ref, mask);
		statistic.end();
	}

	@Override
	public void setStencilOp(int fail, int zfail, int zpass) {
		DurationStatistics statistic = statistics[127];
		statistic.start();
		super.setStencilOp(fail, zfail, zpass);
		statistic.end();
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		DurationStatistics statistic = statistics[128];
		statistic.start();
		super.setTexCoordPointer(size, type, stride, bufferSize, buffer);
		statistic.end();
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, long offset) {
		DurationStatistics statistic = statistics[129];
		statistic.start();
		super.setTexCoordPointer(size, type, stride, offset);
		statistic.end();
	}

	@Override
	public void setTexEnv(int name, float param) {
		DurationStatistics statistic = statistics[130];
		statistic.start();
		super.setTexEnv(name, param);
		statistic.end();
	}

	@Override
	public void setTexEnv(int name, int param) {
		DurationStatistics statistic = statistics[131];
		statistic.start();
		super.setTexEnv(name, param);
		statistic.end();
	}

	@Override
	public void setTexImage(int level, int internalFormat, int width, int height, int format, int type, int textureSize, Buffer buffer) {
		DurationStatistics statistic = statistics[132];
		statistic.start();
		super.setTexImage(level, internalFormat, width, height, format, type, textureSize, buffer);
		statistic.end();
	}

	@Override
	public void setTexSubImage(int level, int offset, int offset2, int width, int height, int format, int type, int textureSize, Buffer buffer) {
		DurationStatistics statistic = statistics[133];
		statistic.start();
		super.setTexSubImage(level, offset, offset2, width, height, format, type, textureSize, buffer);
		statistic.end();
	}

	@Override
	public void setTextureEnvColor(float[] color) {
		DurationStatistics statistic = statistics[134];
		statistic.start();
		super.setTextureEnvColor(color);
		statistic.end();
	}

	@Override
	public void setTextureEnvironmentMapping(int u, int v) {
		DurationStatistics statistic = statistics[135];
		statistic.start();
		super.setTextureEnvironmentMapping(u, v);
		statistic.end();
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
		DurationStatistics statistic = statistics[136];
		statistic.start();
		super.setTextureFunc(func, alphaUsed, colorDoubled);
		statistic.end();
	}

	@Override
	public void setTextureMapMode(int mode, int proj) {
		DurationStatistics statistic = statistics[137];
		statistic.start();
		super.setTextureMapMode(mode, proj);
		statistic.end();
	}

	@Override
	public void setTextureMatrix(float[] values) {
		DurationStatistics statistic = statistics[138];
		statistic.start();
		super.setTextureMatrix(values);
		statistic.end();
	}

	@Override
	public void setTextureMipmapMagFilter(int filter) {
		DurationStatistics statistic = statistics[139];
		statistic.start();
		super.setTextureMipmapMagFilter(filter);
		statistic.end();
	}

	@Override
	public void setTextureMipmapMaxLevel(int level) {
		DurationStatistics statistic = statistics[140];
		statistic.start();
		super.setTextureMipmapMaxLevel(level);
		statistic.end();
	}

	@Override
	public void setTextureMipmapMinFilter(int filter) {
		DurationStatistics statistic = statistics[141];
		statistic.start();
		super.setTextureMipmapMinFilter(filter);
		statistic.end();
	}

	@Override
	public void setTextureMipmapMinLevel(int level) {
		DurationStatistics statistic = statistics[142];
		statistic.start();
		super.setTextureMipmapMinLevel(level);
		statistic.end();
	}

	@Override
	public void setTextureWrapMode(int s, int t) {
		DurationStatistics statistic = statistics[143];
		statistic.start();
		super.setTextureWrapMode(s, t);
		statistic.end();
	}

	@Override
	public void setUniform(int id, float value) {
		DurationStatistics statistic = statistics[144];
		statistic.start();
		super.setUniform(id, value);
		statistic.end();
	}

	@Override
	public void setUniform(int id, int value1, int value2) {
		DurationStatistics statistic = statistics[145];
		statistic.start();
		super.setUniform(id, value1, value2);
		statistic.end();
	}

	@Override
	public void setUniform(int id, int value) {
		DurationStatistics statistic = statistics[146];
		statistic.start();
		super.setUniform(id, value);
		statistic.end();
	}

	@Override
	public void setUniform2(int id, int[] values) {
		DurationStatistics statistic = statistics[147];
		statistic.start();
		super.setUniform2(id, values);
		statistic.end();
	}

	@Override
	public void setUniform3(int id, int[] values) {
		DurationStatistics statistic = statistics[148];
		statistic.start();
		super.setUniform3(id, values);
		statistic.end();
	}

	@Override
	public void setUniform4(int id, int[] values) {
		DurationStatistics statistic = statistics[149];
		statistic.start();
		super.setUniform4(id, values);
		statistic.end();
	}

	@Override
	public void setUniformBlockBinding(int program, int blockIndex, int bindingPoint) {
		DurationStatistics statistic = statistics[150];
		statistic.start();
		super.setUniformBlockBinding(program, blockIndex, bindingPoint);
		statistic.end();
	}

	@Override
	public void setUniformMatrix4(int id, int count, float[] values) {
		DurationStatistics statistic = statistics[151];
		statistic.start();
		super.setUniformMatrix4(id, count, values);
		statistic.end();
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, int bufferSize, Buffer buffer) {
		DurationStatistics statistic = statistics[152];
		statistic.start();
		super.setVertexAttribPointer(id, size, type, normalized, stride, bufferSize, buffer);
		statistic.end();
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, long offset) {
		DurationStatistics statistic = statistics[153];
		statistic.start();
		super.setVertexAttribPointer(id, size, type, normalized, stride, offset);
		statistic.end();
	}

	@Override
	public void setVertexColor(float[] color) {
		DurationStatistics statistic = statistics[154];
		statistic.start();
		super.setVertexColor(color);
		statistic.end();
	}

	@Override
	public void setVertexInfo(VertexInfo vinfo, boolean allNativeVertexInfo, boolean useVertexColor, int type) {
		DurationStatistics statistic = statistics[155];
		statistic.start();
		super.setVertexInfo(vinfo, allNativeVertexInfo, useVertexColor, type);
		statistic.end();
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		DurationStatistics statistic = statistics[156];
		statistic.start();
		super.setVertexPointer(size, type, stride, bufferSize, buffer);
		statistic.end();
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, long offset) {
		DurationStatistics statistic = statistics[157];
		statistic.start();
		super.setVertexPointer(size, type, stride, offset);
		statistic.end();
	}

	@Override
	public void setViewMatrix(float[] values) {
		DurationStatistics statistic = statistics[158];
		statistic.start();
		super.setViewMatrix(values);
		statistic.end();
	}

	@Override
	public void setViewport(int x, int y, int width, int height) {
		DurationStatistics statistic = statistics[159];
		statistic.start();
		super.setViewport(x, y, width, height);
		statistic.end();
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		DurationStatistics statistic = statistics[160];
		statistic.start();
		super.setWeightPointer(size, type, stride, bufferSize, buffer);
		statistic.end();
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, long offset) {
		DurationStatistics statistic = statistics[161];
		statistic.start();
		super.setWeightPointer(size, type, stride, offset);
		statistic.end();
	}

	@Override
	public void startClearMode(boolean color, boolean stencil, boolean depth) {
		DurationStatistics statistic = statistics[162];
		statistic.start();
		super.startClearMode(color, stencil, depth);
		statistic.end();
	}

	@Override
	public void startDirectRendering(boolean textureEnabled, boolean depthWriteEnabled, boolean colorWriteEnabled, boolean setOrthoMatrix, boolean orthoInverted, int width, int height) {
		DurationStatistics statistic = statistics[163];
		statistic.start();
		super.startDirectRendering(textureEnabled, depthWriteEnabled, colorWriteEnabled, setOrthoMatrix, orthoInverted, width, height);
		statistic.end();
	}

	@Override
	public void startDisplay() {
		DurationStatistics statistic = statistics[164];
		statistic.start();
		super.startDisplay();
		statistic.end();
	}

	@Override
	public void useProgram(int program) {
		DurationStatistics statistic = statistics[165];
		statistic.start();
		super.useProgram(program);
		statistic.end();
	}

	@Override
	public void validateProgram(int program) {
		DurationStatistics statistic = statistics[166];
		statistic.start();
		super.validateProgram(program);
		statistic.end();
	}

	@Override
	public void bindVertexArray(int id) {
		DurationStatistics statistic = statistics[167];
		statistic.start();
		super.bindVertexArray(id);
		statistic.end();
	}

	@Override
	public void deleteVertexArray(int id) {
		DurationStatistics statistic = statistics[168];
		statistic.start();
		super.deleteVertexArray(id);
		statistic.end();
	}

	@Override
	public int genVertexArray() {
		DurationStatistics statistic = statistics[169];
		statistic.start();
		int value = super.genVertexArray();
		statistic.end();
		return value;
	}

	@Override
	public boolean isVertexArrayAvailable() {
		DurationStatistics statistic = statistics[170];
		statistic.start();
		boolean value = super.isVertexArrayAvailable();
		statistic.end();
		return value;
	}

	@Override
	public void multiDrawArrays(int primitive, IntBuffer first, IntBuffer count) {
		DurationStatistics statistic = statistics[171];
		statistic.start();
		super.multiDrawArrays(primitive, first, count);
		statistic.end();
	}

	@Override
	public void multMatrix(float[] values) {
		DurationStatistics statistic = statistics[172];
		statistic.start();
		super.multMatrix(values);
		statistic.end();
	}

	@Override
	public void setMatrix(float[] values) {
		DurationStatistics statistic = statistics[173];
		statistic.start();
		super.setMatrix(values);
		statistic.end();
	}

	@Override
	public void setMatrixMode(int type) {
		DurationStatistics statistic = statistics[174];
		statistic.start();
		super.setMatrixMode(type);
		statistic.end();
	}

	@Override
	public void setPixelTransfer(int parameter, float value) {
		DurationStatistics statistic = statistics[175];
		statistic.start();
		super.setPixelTransfer(parameter, value);
		statistic.end();
	}

	@Override
	public void setPixelTransfer(int parameter, int value) {
		DurationStatistics statistic = statistics[176];
		statistic.start();
		super.setPixelTransfer(parameter, value);
		statistic.end();
	}

	@Override
	public void setPixelTransfer(int parameter, boolean value) {
		DurationStatistics statistic = statistics[177];
		statistic.start();
		super.setPixelTransfer(parameter, value);
		statistic.end();
	}

	@Override
	public void setPixelMap(int map, int mapSize, Buffer buffer) {
		DurationStatistics statistic = statistics[178];
		statistic.start();
		super.setPixelMap(map, mapSize, buffer);
		statistic.end();
	}

	@Override
	public boolean canNativeClut() {
		DurationStatistics statistic = statistics[179];
		statistic.start();
		boolean value = super.canNativeClut();
		statistic.end();
		return value;
	}

	@Override
	public void setActiveTexture(int index) {
		DurationStatistics statistic = statistics[180];
		statistic.start();
		super.setActiveTexture(index);
		statistic.end();
	}

	@Override
	public void setTextureFormat(int pixelFormat, boolean swizzle) {
		DurationStatistics statistic = statistics[181];
		statistic.start();
		super.setTextureFormat(pixelFormat, swizzle);
		statistic.end();
	}

	@Override
	public int getUniformIndex(int program, String name) {
		DurationStatistics statistic = statistics[182];
		statistic.start();
		int value = super.getUniformIndex(program, name);
		statistic.end();
		return value;
	}

	@Override
	public int[] getUniformIndices(int program, String[] names) {
		DurationStatistics statistic = statistics[183];
		statistic.start();
		int[] value = super.getUniformIndices(program, names);
		statistic.end();
		return value;
	}

	@Override
	public int getActiveUniformOffset(int program, int uniformIndex) {
		DurationStatistics statistic = statistics[184];
		statistic.start();
		int value = super.getActiveUniformOffset(program, uniformIndex);
		statistic.end();
		return value;
	}

	@Override
	public void bindAttribLocation(int program, int index, String name) {
		DurationStatistics statistic = statistics[185];
		statistic.start();
		super.bindAttribLocation(program, index, name);
		statistic.end();
	}

	@Override
	public void setUniform4(int id, float[] values) {
		DurationStatistics statistic = statistics[186];
		statistic.start();
		super.setUniform4(id, values);
		statistic.end();
	}

	@Override
	public void drawArraysBurstMode(int primitive, int first, int count) {
		DurationStatistics statistic = statistics[187];
		statistic.start();
		super.drawArraysBurstMode(primitive, first, count);
		statistic.end();
	}

	@Override
	public void bindActiveTexture(int index, int texture) {
		DurationStatistics statistic = statistics[188];
		statistic.start();
		super.bindActiveTexture(index, texture);
		statistic.end();
	}
}
