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
 * @author gid15
 *
 * The interface for a RenderingEngine pipeline elements.
 */
public interface IRenderingEngine {
	public static final int[] sizeOfTextureType = {
		2, // TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
		2, // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
		2, // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
		4, // TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
		0, // TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
        1, // TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
        2, // TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
        4, // TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
        0, // TPSM_PIXEL_STORAGE_MODE_DXT1
        0, // TPSM_PIXEL_STORAGE_MODE_DXT3
        0, // TPSM_PIXEL_STORAGE_MODE_DXT5
        2, // RE_PIXEL_STORAGE_16BIT_INDEXED_BGR5650
        2, // RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR5651
        2, // RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR4444
        4, // RE_PIXEL_STORAGE_32BIT_INDEXED_ABGR8888
        4, // RE_DEPTH_COMPONENT
        4, // RE_STENCIL_INDEX
        4  // RE_DEPTH_STENCIL
	};
	public static final int[] alignementOfTextureBufferWidth = {
		8, // TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
		8, // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
		8, // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
		4, // TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
		32, // TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
        16, // TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
        8, // TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
        4, // TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
        1, // TPSM_PIXEL_STORAGE_MODE_DXT1
        1, // TPSM_PIXEL_STORAGE_MODE_DXT3
        1, // TPSM_PIXEL_STORAGE_MODE_DXT5
        8, // RE_PIXEL_STORAGE_16BIT_INDEXED_BGR5650
        8, // RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR5651
        8, // RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR4444
        4, // RE_PIXEL_STORAGE_32BIT_INDEXED_ABGR8888
        4, // RE_DEPTH_COMPONENT
        4, // RE_STENCIL_INDEX
        4  // RE_DEPTH_STENCIL
	};
	public static final boolean[] isTextureTypeIndexed = {
		false, // TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
		false, // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
		false, // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
		false, // TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
		true,  // TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
        true,  // TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
        true,  // TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
        true,  // TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
        false, // TPSM_PIXEL_STORAGE_MODE_DXT1
        false, // TPSM_PIXEL_STORAGE_MODE_DXT3
        false, // TPSM_PIXEL_STORAGE_MODE_DXT5
        true,  // RE_PIXEL_STORAGE_16BIT_INDEXED_BGR5650
        true,  // RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR5551
        true,  // RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR4444
        true,  // RE_PIXEL_STORAGE_32BIT_INDEXED_ABGR8888
        false, // RE_DEPTH_COMPONENT
        false, // RE_STENCIL_INDEX
        false  // RE_DEPTH_STENCIL
	};

	// Additional Texture types
	public static final int RE_PIXEL_STORAGE_16BIT_INDEXED_BGR5650  = 11;
	public static final int RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR5551 = 12;
	public static final int RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR4444 = 13;
	public static final int RE_PIXEL_STORAGE_32BIT_INDEXED_ABGR8888 = 14;
	public static final int RE_DEPTH_COMPONENT                      = 15;
	public static final int RE_STENCIL_INDEX                        = 16;
	public static final int RE_DEPTH_STENCIL                        = 17;

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

	// Primitive types:
	public static final int GU_POINTS              = 0;
	public static final int GU_LINES               = 1;
	public static final int GU_LINE_STRIP          = 2;
	public static final int GU_TRIANGLES           = 3;
	public static final int GU_TRIANGLE_STRIP      = 4;
	public static final int GU_TRIANGLE_FAN        = 5;
	public static final int GU_SPRITES             = 6;
	public static final int RE_QUADS               = 7;
	public static final int RE_LINES_ADJACENCY     = 8;
	public static final int RE_TRIANGLES_ADJACENCY = 9;
	public static final int RE_TRIANGLE_STRIP_ADJACENCY = 10;

	// Matrix modes:
	public static final int GU_PROJECTION          = 0;
	public static final int GU_VIEW                = 1;
	public static final int GU_MODEL               = 2;
	public static final int GU_TEXTURE             = 3;
	public static final int RE_MODELVIEW           = 4;

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
	public static final int GU_FIX_BLEND_ONE_MINUS_COLOR = 11;
	public static final int GU_FIX_BLACK           = 12;
	public static final int GU_FIX_WHITE           = 13;

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
	public static final int RE_GEOMETRY_SHADER     = 2;

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
	public static final int[] sizeOfType = {
		1, // RE_BYTE
		1, // RE_UNSIGNED_BYTE
		2, // RE_SHORT
		2, // RE_UNSIGNED_SHORT
		4, // RE_INT
		4, // RE_UNSIGNED_INT
		4, // RE_FLOAT
		8  // RE_DOUBLE
	};

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

	// Program parameters
	public static final int RE_GEOMETRY_INPUT_TYPE   = 0;
	public static final int RE_GEOMETRY_OUTPUT_TYPE  = 1;
	public static final int RE_GEOMETRY_VERTICES_OUT = 2;

	// Buffer Target
	public static final int RE_ARRAY_BUFFER        = 0;
	public static final int RE_UNIFORM_BUFFER      = 1;

	// Framebuffer Target
	public static final int RE_FRAMEBUFFER         = 0;
	public static final int RE_READ_FRAMEBUFFER    = 1;
	public static final int RE_DRAW_FRAMEBUFFER    = 2;

	// Framebuffer Attachment
	public static final int RE_DEPTH_ATTACHMENT         = 0;
	public static final int RE_STENCIL_ATTACHMENT       = 1;
	public static final int RE_DEPTH_STENCIL_ATTACHMENT = 2;
	public static final int RE_COLOR_ATTACHMENT0        = 3;
	public static final int RE_COLOR_ATTACHMENT1        = 4;
	public static final int RE_COLOR_ATTACHMENT2        = 5;
	public static final int RE_COLOR_ATTACHMENT3        = 6;
	public static final int RE_COLOR_ATTACHMENT4        = 7;
	public static final int RE_COLOR_ATTACHMENT5        = 8;
	public static final int RE_COLOR_ATTACHMENT6        = 9;
	public static final int RE_COLOR_ATTACHMENT7        = 10;

	// Pixel Transfer parameter
	public static final int RE_MAP_COLOR           = 0;
	public static final int RE_MAP_STENCIL         = 1;
	public static final int RE_INDEX_SHIFT         = 2;
	public static final int RE_INDEX_OFFSET        = 3;
	public static final int RE_RED_SCALE           = 4;
	public static final int RE_GREEN_SCALE         = 5;
	public static final int RE_BLUE_SCALE          = 6;
	public static final int RE_ALPHA_SCALE         = 7;
	public static final int RE_DEPTH_SCALE         = 8;
	public static final int RE_RED_BIAS            = 9;
	public static final int RE_GREEN_BIAS          = 10;
	public static final int RE_BLUE_BIAS           = 11;
	public static final int RE_ALPHA_BIAS          = 12;
	public static final int RE_DEPTH_BIAS          = 13;

	// Pixel map
	public static final int RE_PIXEL_MAP_I_TO_I    = 0;
	public static final int RE_PIXEL_MAP_S_TO_S    = 1;
	public static final int RE_PIXEL_MAP_I_TO_R    = 2;
	public static final int RE_PIXEL_MAP_I_TO_G    = 3;
	public static final int RE_PIXEL_MAP_I_TO_B    = 4;
	public static final int RE_PIXEL_MAP_I_TO_A    = 5;
	public static final int RE_PIXEL_MAP_R_TO_R    = 6;
	public static final int RE_PIXEL_MAP_G_TO_G    = 7;
	public static final int RE_PIXEL_MAP_B_TO_B    = 8;
	public static final int RE_PIXEL_MAP_A_TO_A    = 9;

	// Clut Index Hint
	public static final int RE_CLUT_INDEX_NO_HINT    = 0;
	public static final int RE_CLUT_INDEX_RED_ONLY   = 1;
	public static final int RE_CLUT_INDEX_GREEN_ONLY = 2;
	public static final int RE_CLUT_INDEX_BLUE_ONLY  = 3;
	public static final int RE_CLUT_INDEX_ALPHA_ONLY = 4;

	// Buffers flag
	public static final int RE_COLOR_BUFFER_BIT      = (1 << 0);
	public static final int RE_DEPTH_BUFFER_BIT      = (1 << 1);
	public static final int RE_STENCIL_BUFFER_BIT    = (1 << 2);

	public void setRenderingEngine(IRenderingEngine re);
	public void setGeContext(GeContext context);
	public void exit();
	public void startDirectRendering(boolean textureEnabled, boolean depthWriteEnabled, boolean colorWriteEnabled, boolean setOrthoMatrix, boolean orthoInverted, int width, int height);
	public void endDirectRendering();
	public void startDisplay();
	public void endDisplay();
	public void enableFlag(int flag);
	public void disableFlag(int flag);
	public void setMorphWeight(int index, float value);
	public void setPatchDiv(int s, int t);
	public void setPatchPrim(int prim);
	public void setMatrixMode(int type);
	public void setMatrix(float[] values);
	public void multMatrix(float[] values);
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
	public void setUniform2(int id, int[] values);
	public void setUniform3(int id, int[] values);
	public void setUniform3(int id, float[] values);
	public void setUniform4(int id, int[] values);
	public void setUniform4(int id, float[] values);
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
	public boolean compilerShader(int shader, String source);
	public int createProgram();
	public void useProgram(int program);
	public void attachShader(int program, int shader);
	public boolean linkProgram(int program);
	public boolean validateProgram(int program);
	public int getUniformLocation(int program, String name);
	public int getAttribLocation(int program, String name);
	public void bindAttribLocation(int program, int index, String name);
	public String getShaderInfoLog(int shader);
	public String getProgramInfoLog(int program);
	public boolean isExtensionAvailable(String name);
	public void drawArrays(int primitive, int first, int count);
	public int genBuffer();
	public void deleteBuffer(int buffer);
	public void setBufferData(int target, int size, Buffer buffer, int usage);
	public void setBufferSubData(int target, int offset, int size, Buffer buffer);
	public void bindBuffer(int target, int buffer);
	public void enableClientState(int type);
	public void disableClientState(int type);
	public void enableVertexAttribArray(int id);
	public void disableVertexAttribArray(int id);
	public void setTexCoordPointer(int size, int type, int stride, long offset);
	public void setTexCoordPointer(int size, int type, int stride, int bufferSize, Buffer buffer);
	public void setColorPointer(int size, int type, int stride, long offset);
	public void setColorPointer(int size, int type, int stride, int bufferSize, Buffer buffer);
	public void setVertexPointer(int size, int type, int stride, long offset);
	public void setVertexPointer(int size, int type, int stride, int bufferSize, Buffer buffer);
	public void setNormalPointer(int type, int stride, long offset);
	public void setNormalPointer(int type, int stride, int bufferSize, Buffer buffer);
	public void setWeightPointer(int size, int type, int stride, long offset);
	public void setWeightPointer(int size, int type, int stride, int bufferSize, Buffer buffer);
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, long offset);
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, int bufferSize, Buffer buffer);
	public void setPixelStore(int rowLength, int alignment);
	public int genTexture();
	public void bindTexture(int texture);
	public void deleteTexture(int texture);
	public void setCompressedTexImage(int level, int internalFormat, int width, int height, int compressedSize, Buffer buffer);
	public void setTexImage(int level, int internalFormat, int width, int height, int format, int type, int textureSize, Buffer buffer);
	public void setTexSubImage(int level, int xOffset, int yOffset, int width, int height, int format, int type, int textureSize, Buffer buffer);
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
	public void beginBoundingBox(int numberOfVertexBoundingBox);
	public void drawBoundingBox(float[][] values);
	public void endBoundingBox(VertexInfo vinfo);
	public boolean isBoundingBoxVisible();
	public int genQuery();
	public void beginQuery(int id);
	public void endQuery();
	public boolean getQueryResultAvailable(int id);
	public int getQueryResult(int id);
	public void clear(float red, float green, float blue, float alpha);
	public IREBufferManager getBufferManager();
	public boolean canAllNativeVertexInfo();
	public boolean canNativeSpritesPrimitive();
	public void setVertexInfo(VertexInfo vinfo, boolean allNativeVertexInfo, boolean useVertexColor, boolean useTexture, int type);
	public void setProgramParameter(int program, int parameter, int value);
	public boolean isQueryAvailable();
	public boolean isShaderAvailable();
	public int getUniformBlockIndex(int program, String name);
	public void bindBufferBase(int target, int bindingPoint, int buffer);
	public void setUniformBlockBinding(int program, int blockIndex, int bindingPoint);
	public int getUniformIndex(int program, String name);
	public int[] getUniformIndices(int program, String[] names);
	public int getActiveUniformOffset(int program, int uniformIndex);
	public boolean isFramebufferObjectAvailable();
	public int genFramebuffer();
	public int genRenderbuffer();
	public void deleteFramebuffer(int framebuffer);
	public void deleteRenderbuffer(int renderbuffer);
	public void bindFramebuffer(int target, int framebuffer);
	public void bindRenderbuffer(int renderbuffer);
	public void setRenderbufferStorage(int internalFormat, int width, int height);
	public void setFramebufferRenderbuffer(int target, int attachment, int renderbuffer);
	public void setFramebufferTexture(int target, int attachment, int texture, int level);
	public int genVertexArray();
	public void bindVertexArray(int id);
	public void deleteVertexArray(int id);
	public boolean isVertexArrayAvailable();
	public void multiDrawArrays(int primitive, IntBuffer first, IntBuffer count);
	public void drawArraysBurstMode(int primitive, int first, int count);
	public void setPixelTransfer(int parameter, int value);
	public void setPixelTransfer(int parameter, float value);
	public void setPixelTransfer(int parameter, boolean value);
	public void setPixelMap(int map, int mapSize, Buffer buffer);
	public boolean canNativeClut(int textureAddress);
	public void setActiveTexture(int index);
	public void setTextureFormat(int pixelFormat, boolean swizzle);
	public void bindActiveTexture(int index, int texture);
	public void setTextureAnisotropy(float value);
	public float getMaxTextureAnisotropy();
	public String getShadingLanguageVersion();
	public void setBlendSFix(int sfix, float[] color);
	public void setBlendDFix(int dfix, float[] color);
	public void waitForRenderingCompletion();
	public boolean canReadAllVertexInfo();
	public void readStencil(int x, int y, int width, int height, int bufferSize, Buffer buffer);
	public void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter);
	public boolean checkAndLogErrors(String logComment);
	public boolean setCopyRedToAlpha(boolean copyRedToAlpha);
}
