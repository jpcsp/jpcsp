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

/**
 * @author gid15
 *
 * The interface for a RenderingEngine pipeline elements.
 */
public interface IRenderingEngine {
	// Flags:
	public static final int GU_ALPHA_TEST          = 0;
	public static final int GU_DEPTH_TEST          = 1;
	public static final int GU_SCISSOR_TEST        = 2;
	public static final int GU_STENCIL_TEST        = 3;
	public static final int GU_BLEND               = 4;
	public static final int GU_CULL_FACE           = 5;
	public static final int GU_DITHER              = 6;
	public static final int GU_FOG                 = 7;
	public static final int GU_CLIP_PLANES         = 8;
	public static final int GU_TEXTURE_2D          = 9;
	public static final int GU_LIGHTING            = 10;
	public static final int GU_LIGHT0              = 11;
	public static final int GU_LIGHT1              = 12;
	public static final int GU_LIGHT2              = 13;
	public static final int GU_LIGHT3              = 14;
	public static final int GU_LINE_SMOOTH         = 15;
	public static final int GU_PATCH_CULL_FACE     = 16;
	public static final int GU_COLOR_TEST          = 17;
	public static final int GU_COLOR_LOGIC_OP      = 18;
	public static final int GU_FACE_NORMAL_REVERSE = 19;
	public static final int GU_PATCH_FACE          = 20;
	public static final int GU_FRAGMENT_2X         = 21;
	public static final int RE_COLOR_MATERIAL      = 22;
	public static final int RE_TEXTURE_GEN_S       = 23;
	public static final int RE_TEXTURE_GEN_T       = 24;
	public static final int RE_NUMBER_FLAGS        = 25; // Always the last one

	// Matrix modes:
	public static final int GU_PROJECTION          = 0;
	public static final int GU_VIEW                = 1;
	public static final int GU_MODEL               = 2;
	public static final int GU_TEXTURE             = 3;

	// Shade models:
	public static final int GU_FLAT                = 0;
	public static final int GU_SMOOTH              = 1;

	// Color types:
	public static final int RE_AMBIENT             = 0;
	public static final int RE_EMISSIVE            = 1;
	public static final int RE_DIFFUSE             = 2;
	public static final int RE_SPECULAR            = 3;

	// Light modes:
	public static final int GU_SINGLE_COLOR        = 0;
	public static final int GU_SEPARATE_SPECULAR_COLOR = 1;

	// Blend functions:
	public static final int GU_FIX_BLEND_COLOR     = 10;
	public static final int GU_FIX_BLACK           = 11;
	public static final int GU_FIX_WHITE           = 12;

	// setTexEnv names:
	public static final int RE_TEXENV_COMBINE_RGB  = 0;
	public static final int RE_TEXENV_COMBINE_ALPHA= 1;
	public static final int RE_TEXENV_RGB_SCALE    = 2;
	public static final int RE_TEXENV_ALPHA_SCALE  = 3;
	public static final int RE_TEXENV_SRC0_RGB     = 4;
	public static final int RE_TEXENV_SRC1_RGB     = 5;
	public static final int RE_TEXENV_SRC2_RGB     = 6;
	public static final int RE_TEXENV_SRC0_ALPHA   = 7;
	public static final int RE_TEXENV_SRC1_ALPHA   = 8;
	public static final int RE_TEXENV_SRC2_ALPHA   = 9;
	public static final int RE_TEXENV_OPERAND0_RGB = 10;
	public static final int RE_TEXENV_OPERAND1_RGB = 11;
	public static final int RE_TEXENV_OPERAND2_RGB = 12;
	public static final int RE_TEXENV_OPERAND0_ALPHA=13;
	public static final int RE_TEXENV_OPERAND1_ALPHA=14;
	public static final int RE_TEXENV_OPERAND2_ALPHA=15;
	public static final int RE_TEXENV_ENV_MODE     = 16;

	// setTexEnv params:
	// values [0..4] are TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_xxx
	public static final int RE_TEXENV_MODULATE     = 0;
	public static final int RE_TEXENV_DECAL        = 1;
	public static final int RE_TEXENV_BLEND        = 2;
	public static final int RE_TEXENV_REPLACE      = 3;
	public static final int RE_TEXENV_ADD          = 4;
	public static final int RE_TEXENV_INTERPOLATE  = 5;
	public static final int RE_TEXENV_SUBTRACT     = 6;
	public static final int RE_TEXENV_TEXTURE      = 7;
	public static final int RE_TEXENV_CONSTANT     = 8;
	public static final int RE_TEXENV_PREVIOUS     = 9;
	public static final int RE_TEXENV_SRC_COLOR    = 10;
	public static final int RE_TEXENV_SRC_ALPHA    = 11;
	public static final int RE_TEXENV_COMBINE      = 12;

	// Shader types:
	public static final int RE_VERTEX_SHADER       = 0;
	public static final int RE_FRAGMENT_SHADER     = 1;

	// Client State types:
	public static final int RE_TEXTURE             = 0;
	public static final int RE_COLOR               = 1;
	public static final int RE_NORMAL              = 2;
	public static final int RE_VERTEX              = 3;

	// Pointer types:
	public static final int RE_BYTE                = 0;
	public static final int RE_UNSIGNED_BYTE       = 1;
	public static final int RE_SHORT               = 2;
	public static final int RE_UNSIGNED_SHORT      = 3;
	public static final int RE_INT                 = 4;
	public static final int RE_UNSIGNED_INT        = 5;
	public static final int RE_FLOAT               = 6;
	public static final int RE_DOUBLE              = 7;

	// Buffer usage:
	public static final int RE_STREAM_DRAW         = 0;
	public static final int RE_STREAM_READ         = 1;
	public static final int RE_STREAM_COPY         = 2;
	public static final int RE_STATIC_DRAW         = 3;
	public static final int RE_STATIC_READ         = 4;
	public static final int RE_STATIC_COPY         = 5;
	public static final int RE_DYNAMIC_DRAW        = 6;
	public static final int RE_DYNAMIC_READ        = 7;
	public static final int RE_DYNAMIC_COPY        = 8;

	public void setRenderingEngine(IRenderingEngine re);
	public void setGeContext(GeContext context);
	public void startDirectRendering(boolean textureEnabled, boolean depthWriteEnabled, boolean colorWriteEnabled, boolean setOrthoMatrix, boolean orthoInverted, int width, int height);
	public void endDirectRendering();
	public void startDisplay();
	public void endDisplay();
	public void enableFlag(int flag);
	public void disableFlag(int flag);
	public void setMorphWeight(int index, float value);
	public void setPatchDiv(int s, int t);
	public void setPatchPrim(int prim);
	public void setMatrix(int type, float[] values);
	public void setProjectionMatrix(float[] values);
	public void setViewMatrix(float[] values);
	public void setModelMatrix(float[] values);
	public void setModelViewMatrix(float[] values);
	public void setTextureMatrix(float[] values);
	public void endModelViewMatrixUpdate();
	public void setViewport(int x, int y, int width, int height);
	public void setDepthRange(float zpos, float zscale, float near, float far);
	public void setDepthFunc(int func);
	public void setShadeModel(int model);
	public void setMaterialColor(int type, float[] color);
	public void setMaterialEmissiveColor(float[] color);
	public void setMaterialAmbientColor(float[] color);
	public void setMaterialDiffuseColor(float[] color);
	public void setMaterialSpecularColor(float[] color);
	public void setMaterialShininess(float shininess);
	public void setLightModelAmbientColor(float[] color);
	public void setLightMode(int mode);
	public void setLightPosition(int light, float[] position);
	public void setLightDirection(int light, float[] direction);
	public void setLightSpotExponent(int light, float exponent);
	public void setLightSpotCutoff(int light, float cutoff);
	public void setLightConstantAttenuation(int light, float constant);
	public void setLightLinearAttenuation(int light, float linear);
	public void setLightQuadraticAttenuation(int light, float quadratic);
	public void setLightAmbientColor(int light, float[] color);
	public void setLightDiffuseColor(int light, float[] color);
	public void setLightSpecularColor(int light, float[] color);
	public void setLightColor(int type, int light, float[] color);
	public void setLightType(int light, int type, int kind);
	public void setBlendFunc(int src, int dst);
	public void setBlendColor(float[] color);
	public void setLogicOp(int logicOp);
	public void setDepthMask(boolean depthWriteEnabled);
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask);
	public void setColorMask(boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled);
	public void setTextureWrapMode(int s, int t);
	public void setTextureMipmapMinLevel(int level);
	public void setTextureMipmapMaxLevel(int level);
	public void setTextureMipmapMinFilter(int filter);
	public void setTextureMipmapMagFilter(int filter);
	public void setColorMaterial(boolean ambient, boolean diffuse, boolean specular);
	public void setTextureMapMode(int mode, int proj);
	public void setTextureEnvironmentMapping(int u, int v);
	public void setVertexColor(float[] color);
	public void setUniform(int id, int value);
	public void setUniform(int id, int value1, int value2);
	public void setUniform(int id, float value);
	public void setUniform3(int id, int[] values);
	public void setUniform4(int id, int[] values);
	public void setUniformMatrix4(int id, int count, float[] values);
	public void setColorTestFunc(int func);
	public void setColorTestReference(int[] values);
	public void setColorTestMask(int[] values);
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled);
	public int setBones(int count, float[] values);
	public void setTexEnv(int name, int param);
	public void setTexEnv(int name, float param);
	public void startClearMode(boolean color, boolean stencil, boolean depth);
	public void endClearMode();
	public int createShader(int type);
	public void compilerShader(int shader, String[] source);
	public int createProgram();
	public void useProgram(int program);
	public void attachShader(int program, int shader);
	public void linkProgram(int program);
	public void validateProgram(int program);
	public int getUniformLocation(int program, String name);
	public int getAttribLocation(int program, String name);
	public String getShaderInfoLog(int shader);
	public String getProgramInfoLog(int program);
	public boolean isFunctionAvailable(String name);
	public void drawArrays(int primitive, int first, int count);
	public int genBuffer();
	public void deleteBuffer(int buffer);
	public void setBufferData(int size, Buffer buffer, int usage);
	public void bindBuffer(int buffer);
	public void enableClientState(int type);
	public void disableClientState(int type);
	public void enableVertexAttribArray(int id);
	public void disableVertexAttribArray(int id);
	public void setTexCoordPointer(int size, int type, int stride, long offset);
	public void setTexCoordPointer(int size, int type, int stride, Buffer buffer);
	public void setColorPointer(int size, int type, int stride, long offset);
	public void setColorPointer(int size, int type, int stride, Buffer buffer);
	public void setVertexPointer(int size, int type, int stride, long offset);
	public void setVertexPointer(int size, int type, int stride, Buffer buffer);
	public void setNormalPointer(int type, int stride, long offset);
	public void setNormalPointer(int type, int stride, Buffer buffer);
	public void setWeightPointer(int size, int type, int stride, long offset);
	public void setWeightPointer(int size, int type, int stride, Buffer buffer);
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, long offset);
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, Buffer buffer);
	public void setPixelStore(int rowLength, int alignment);
	public int genTexture();
	public void bindTexture(int texture);
	public void deleteTexture(int texture);
	public void setCompressedTexImage(int level, int internalFormat, int width, int height, int compressedSize, Buffer buffer);
	public void setTexImage(int level, int internalFormat, int width, int height, int format, int type, Buffer buffer);
	public void setTexSubImage(int level, int xOffset, int yOffset, int width, int height, int format, int type, Buffer buffer);
	public void getTexImage(int level, int format, int type, Buffer buffer);
	public void copyTexSubImage(int level, int xOffset, int yOffset, int x, int y, int width, int height);
	public void setStencilOp(int fail, int zfail, int zpass);
	public void setStencilFunc(int func, int ref, int mask);
	public void setAlphaFunc(int func, int ref);
	public void setFogHint();
	public void setFogColor(float[] color);
	public void setFogDist(float start, float end);
	public void setTextureEnvColor(float[] color);
	public void setFrontFace(boolean cw);
	public void setScissor(int x, int y, int width, int height);
	public void setBlendEquation(int mode);
	public void setLineSmoothHint();
	public boolean hasBoundingBox();
	public void beginBoundingBox();
	public void drawBoundingBox(float[][] values);
	public void endBoundingBox();
	public boolean isBoundingBoxVisible();
	public int genQuery();
	public void beginQuery(int id);
	public void endQuery();
	public boolean getQueryResultAvailable(int id);
	public int getQueryResult(int id);
	public void beginDraw(int type);
	public void endDraw();
	public void drawVertex3(float[] values);
	public void drawVertex(int value1, int value2);
	public void drawVertex(float value1, float value2);
	public void drawTexCoord(float value1, float value2);
	public void drawColor(float value1, float value2, float value3);
	public void drawColor(float value1, float value2, float value3, float value4);
	public void readPixels(int x, int y, int width, int height, int format, int type, Buffer buffer);
	public void clear(float red, float green, float blue, float alpha);
}
