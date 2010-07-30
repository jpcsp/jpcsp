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

import javax.media.opengl.GL;
import javax.media.opengl.GLException;

import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 * An abstract RenderingEngine implementing calls to OpenGL using jogl.
 * The class contains no rendering logic, it just implements the interface to jogl.
 */
public class RenderingEngineJogl extends BaseRenderingEngine {
	protected final static int bufferTarget = GL.GL_ARRAY_BUFFER;
	protected static final int[] flagToGL = {
		GL.GL_ALPHA_TEST,     // GU_ALPHA_TEST
		GL.GL_DEPTH_TEST,     // GU_DEPTH_TEST
		GL.GL_SCISSOR_TEST,   // GU_SCISSOR_TEST
		GL.GL_STENCIL_TEST,   // GU_STENCIL_TEST
		GL.GL_BLEND,          // GU_BLEND
		GL.GL_CULL_FACE,      // GU_CULL_FACE
		GL.GL_DITHER,         // GU_DITHER
		GL.GL_FOG,            // GU_FOG
		0,                    // GU_CLIP_PLANES
		GL.GL_TEXTURE_2D,     // GU_TEXTURE_2D
		GL.GL_LIGHTING,       // GU_LIGHTING
		GL.GL_LIGHT0,         // GU_LIGHT0
		GL.GL_LIGHT1,         // GU_LIGHT1
		GL.GL_LIGHT2,         // GU_LIGHT2
		GL.GL_LIGHT3,         // GU_LIGHT3
		GL.GL_LINE_SMOOTH,    // GU_LINE_SMOOTH
		0,                    // GU_PATCH_CULL_FACE
		0,                    // GU_COLOR_TEST
		GL.GL_COLOR_LOGIC_OP, // GU_COLOR_LOGIC_OP
		0,                    // GU_FACE_NORMAL_REVERSE
		0,                    // GU_PATCH_FACE
		0,                    // GU_FRAGMENT_2X
		GL.GL_COLOR_MATERIAL, // RE_COLOR_MATERIAL
		GL.GL_TEXTURE_GEN_S,  // RE_TEXTURE_GEN_S
		GL.GL_TEXTURE_GEN_T,  // RE_TEXTURE_GEN_T
	};
	protected static final int[] shadeModelToGL = {
		GL.GL_FLAT,           // GU_FLAT
		GL.GL_SMOOTH          // GU_SMOOTH
	};
	protected static final int[] colorTypeToGL = {
		GL.GL_AMBIENT,        // RE_AMBIENT
		GL.GL_EMISSION,       // RE_EMISSIVE
		GL.GL_DIFFUSE,        // RE_DIFFUSE
		GL.GL_SPECULAR        // RE_SPECULAR
	};
	protected static final int[] lightModeToGL = {
		GL.GL_SINGLE_COLOR,   // GU_SINGLE_COLOR
		GL.GL_SEPARATE_SPECULAR_COLOR // GU_SEPARATE_SPECULAR_COLOR
	};
	protected static final int[] blendSrcToGL = {
		GL.GL_DST_COLOR,           // GU_SRC_COLOR
		GL.GL_ONE_MINUS_DST_COLOR, // GU_ONE_MINUS_SRC_COLOR
		GL.GL_SRC_ALPHA,           // GU_SRC_ALPHA
		GL.GL_ONE_MINUS_SRC_ALPHA, // GU_ONE_MINUS_SRC_ALPHA
		GL.GL_DST_ALPHA,           // 4
		GL.GL_ONE_MINUS_DST_ALPHA, // 5
		GL.GL_SRC_ALPHA,           // 6
		GL.GL_ONE_MINUS_SRC_ALPHA, // 7
		GL.GL_DST_ALPHA,           // 8
		GL.GL_ONE_MINUS_DST_ALPHA, // 9
		GL.GL_CONSTANT_COLOR,      // GU_FIX for blend color
		GL.GL_ZERO,                // GU_FIX for 0x000000
		GL.GL_ONE                  // GU_FIX for 0xFFFFFF
	};
	protected static final int[] blendDstToGL = {
		GL.GL_SRC_COLOR,           // GU_SRC_COLOR
		GL.GL_ONE_MINUS_SRC_COLOR, // GU_ONE_MINUS_SRC_COLOR
		GL.GL_SRC_ALPHA,           // GU_SRC_ALPHA
		GL.GL_ONE_MINUS_SRC_ALPHA, // GU_ONE_MINUS_SRC_ALPHA
		GL.GL_DST_ALPHA,           // 4
		GL.GL_ONE_MINUS_DST_ALPHA, // 5
		GL.GL_SRC_ALPHA,           // 6
		GL.GL_ONE_MINUS_SRC_ALPHA, // 7
		GL.GL_DST_ALPHA,           // 8
		GL.GL_ONE_MINUS_DST_ALPHA, // 9
		GL.GL_CONSTANT_COLOR,      // GU_FIX_BLEND_COLOR
		GL.GL_ZERO,                // GU_FIX_BLACK
		GL.GL_ONE                  // GU_FIX_WHITE
	};
	protected static final int[] logicOpToGL = {
        GL.GL_CLEAR,               // LOP_CLEAR
        GL.GL_AND,                 // LOP_AND
        GL.GL_AND_REVERSE,         // LOP_REVERSE_AND
        GL.GL_COPY,                // LOP_COPY
        GL.GL_AND_INVERTED,        // LOP_INVERTED_AND
        GL.GL_NOOP,                // LOP_NO_OPERATION
        GL.GL_XOR,                 // LOP_EXLUSIVE_OR
        GL.GL_OR,                  // LOP_OR
        GL.GL_NOR,                 // LOP_NEGATED_OR
        GL.GL_EQUIV,               // LOP_EQUIVALENCE
        GL.GL_INVERT,              // LOP_INVERTED
        GL.GL_OR_REVERSE,          // LOP_REVERSE_OR
        GL.GL_COPY_INVERTED,       // LOP_INVERTED_COPY
        GL.GL_OR_INVERTED,         // LOP_INVERTED_OR
        GL.GL_NAND,                // LOP_NEGATED_AND
        GL.GL_SET                  // LOP_SET
	};
	protected static final int[] wrapModeToGL = {
		GL.GL_REPEAT,              // TWRAP_WRAP_MODE_REPEAT
		GL.GL_CLAMP_TO_EDGE        // TWRAP_WRAP_MODE_CLAMP
	};
	protected static final int[] colorMaterialToGL = {
		GL.GL_AMBIENT,             // none
		GL.GL_AMBIENT,             // ambient
		GL.GL_DIFFUSE,             // diffuse
		GL.GL_AMBIENT_AND_DIFFUSE, // ambient, diffuse
		GL.GL_SPECULAR,            // specular
		GL.GL_AMBIENT,             // ambient, specular
		GL.GL_DIFFUSE,             // diffuse, specular
		GL.GL_AMBIENT_AND_DIFFUSE  // ambient, diffuse, specular
	};
	protected static final int[] depthFuncToGL = {
		GL.GL_NEVER,               // ZTST_FUNCTION_NEVER_PASS_PIXEL
		GL.GL_ALWAYS,              // ZTST_FUNCTION_ALWAYS_PASS_PIXEL
		GL.GL_EQUAL,               // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_EQUAL
		GL.GL_NOTEQUAL,            // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_ISNOT_EQUAL
		GL.GL_LESS,                // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS
		GL.GL_LEQUAL,              // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS_OR_EQUAL
		GL.GL_GREATER,             // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER
		GL.GL_GEQUAL               // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER_OR_EQUAL
	};
	protected static final int[] texEnvNameToGL = {
		GL.GL_COMBINE_RGB,         // RE_TEXENV_COMBINE_RGB
		GL.GL_COMBINE_ALPHA,       // RE_TEXENV_COMBINE_ALPHA
		GL.GL_RGB_SCALE,           // RE_TEXENV_RGB_SCALE
		GL.GL_ALPHA_SCALE,         // RE_TEXENV_ALPHA_SCALE
		GL.GL_SRC0_RGB,            // RE_TEXENV_SRC0_RGB
		GL.GL_SRC1_RGB,            // RE_TEXENV_SRC1_RGB
		GL.GL_SRC2_RGB,            // RE_TEXENV_SRC2_RGB
		GL.GL_SRC0_ALPHA,          // RE_TEXENV_SRC0_ALPHA
		GL.GL_SRC1_ALPHA,          // RE_TEXENV_SRC1_ALPHA
		GL.GL_SRC2_ALPHA,          // RE_TEXENV_SRC2_ALPHA
		GL.GL_OPERAND0_RGB,        // RE_TEXENV_OPERAND0_RGB
		GL.GL_OPERAND1_RGB,        // RE_TEXENV_OPERAND1_RGB
		GL.GL_OPERAND2_RGB,        // RE_TEXENV_OPERAND2_RGB
		GL.GL_OPERAND0_ALPHA,      // RE_TEXENV_OPERAND0_ALPHA
		GL.GL_OPERAND1_ALPHA,      // RE_TEXENV_OPERAND1_ALPHA
		GL.GL_OPERAND2_ALPHA,      // RE_TEXENV_OPERAND2_ALPHA
		GL.GL_TEXTURE_ENV_MODE     // RE_TEXENV_ENV_MODE
	};
	protected static final int[] texEnvParamToGL = {
		GL.GL_MODULATE,            // RE_TEXENV_MODULATE
		GL.GL_DECAL,               // RE_TEXENV_DECAL
		GL.GL_BLEND,               // RE_TEXENV_BLEND
		GL.GL_REPLACE,             // RE_TEXENV_REPLACE
		GL.GL_ADD,                 // RE_TEXENV_ADD
		GL.GL_INTERPOLATE,         // RE_TEXENV_INTERPOLATE
		GL.GL_SUBTRACT,            // RE_TEXENV_SUBTRACT
		GL.GL_TEXTURE,             // RE_TEXENV_TEXTURE
		GL.GL_CONSTANT,            // RE_TEXENV_CONSTANT
		GL.GL_PREVIOUS,            // RE_TEXENV_PREVIOUS
		GL.GL_SRC_COLOR,           // RE_TEXENV_SRC_COLOR
		GL.GL_SRC_ALPHA,           // RE_TEXENV_SRC_ALPHA
		GL.GL_COMBINE              // RE_TEXENV_COMBINE
	};
	protected static final int[] shaderTypeToGL = {
		GL.GL_VERTEX_SHADER,       // RE_VERTEX_SHADER
		GL.GL_FRAGMENT_SHADER      // RE_FRAGMENT_SHADER
	};
	protected static final int[] primitiveToGL = {
		GL.GL_POINTS,              // PRIM_POINT
		GL.GL_LINES,               // PRIM_LINE
		GL.GL_LINE_STRIP,          // PRIM_LINES_STRIPS
		GL.GL_TRIANGLES,           // PRIM_TRIANGLE
		GL.GL_TRIANGLE_STRIP,      // PRIM_TRIANGLE_STRIPS
		GL.GL_TRIANGLE_FAN,        // PRIM_TRIANGLE_FANS
		GL.GL_QUADS                // PRIM_SPRITES
	};
	protected static final int[] clientStateToGL = {
		GL.GL_TEXTURE_COORD_ARRAY, // RE_TEXTURE
		GL.GL_COLOR_ARRAY,         // RE_COLOR
		GL.GL_NORMAL_ARRAY,        // RE_NORMAL
		GL.GL_VERTEX_ARRAY         // RE_VERTEX
	};
	protected static final int[] pointerTypeToGL = {
		GL.GL_BYTE,                // RE_BYTE
		GL.GL_UNSIGNED_BYTE,       // RE_UNSIGNED_BYTE
		GL.GL_SHORT,               // RE_SHORT
		GL.GL_UNSIGNED_SHORT,      // RE_UNSIGNED_SHORT
		GL.GL_INT,                 // RE_INT
		GL.GL_UNSIGNED_INT,        // RE_UNSIGNED_INT
		GL.GL_FLOAT,               // RE_FLOAT
		GL.GL_DOUBLE               // RE_DOUBLE
	};
	protected static final int[] bufferUsageToGL = {
		GL.GL_STREAM_DRAW,         // RE_STREAM_DRAW
		GL.GL_STREAM_READ,         // RE_STREAM_READ
		GL.GL_STREAM_COPY,         // RE_STREAM_COPY
		GL.GL_STATIC_DRAW,         // RE_STATIC_DRAW
		GL.GL_STATIC_READ,         // RE_STATIC_READ
		GL.GL_STATIC_COPY,         // RE_STATIC_COPY
		GL.GL_DYNAMIC_DRAW,        // RE_DYNAMIC_DRAW
		GL.GL_DYNAMIC_READ,        // RE_DYNAMIC_READ
		GL.GL_DYNAMIC_COPY         // RE_DYNAMIC_COPY
	};
	protected static final int[] mipmapFilterToGL = {
		GL.GL_NEAREST,               // TFLT_NEAREST
		GL.GL_LINEAR,                // TFLT_LINEAR
		GL.GL_NEAREST,               // TFLT_UNKNOW1
		GL.GL_NEAREST,               // TFLT_UNKNOW2
		GL.GL_NEAREST_MIPMAP_NEAREST,// TFLT_NEAREST_MIPMAP_NEAREST
		GL.GL_LINEAR_MIPMAP_NEAREST, // TFLT_LINEAR_MIPMAP_NEAREST
		GL.GL_NEAREST_MIPMAP_LINEAR, // TFLT_NEAREST_MIPMAP_LINEAR
		GL.GL_LINEAR_MIPMAP_LINEAR   // TFLT_LINEAR_MIPMAP_LINEAR
	};
	protected static final int[] textureFormatToGL = {
		GL.GL_RGB,                           // TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
		GL.GL_RGBA,                          // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
		GL.GL_RGBA,                          // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
		GL.GL_RGBA,                          // TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
        0,                                   // TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
        GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT,  // TPSM_PIXEL_STORAGE_MODE_DXT1
        GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, // TPSM_PIXEL_STORAGE_MODE_DXT3
        GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT  // TPSM_PIXEL_STORAGE_MODE_DXT5
	};
	protected static final int[] textureTypeToGL = {
        GL.GL_UNSIGNED_SHORT_5_6_5_REV,      // TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
        GL.GL_UNSIGNED_SHORT_1_5_5_5_REV,    // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
        GL.GL_UNSIGNED_SHORT_4_4_4_4_REV,    // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
        GL.GL_UNSIGNED_BYTE,                 // TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
        0,                                   // TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
        GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT,  // TPSM_PIXEL_STORAGE_MODE_DXT1
        GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, // TPSM_PIXEL_STORAGE_MODE_DXT3
        GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT  // TPSM_PIXEL_STORAGE_MODE_DXT5
	};
	protected static final int[] stencilOpToGL = {
		GL.GL_KEEP,                // SOP_KEEP_STENCIL_VALUE
		GL.GL_ZERO,                // SOP_ZERO_STENCIL_VALUE
		GL.GL_REPLACE,             // SOP_REPLACE_STENCIL_VALUE
		GL.GL_INVERT,              // SOP_INVERT_STENCIL_VALUE
		GL.GL_INCR,                // SOP_INCREMENT_STENCIL_VALUE
		GL.GL_DECR                 // SOP_DECREMENT_STENCIL_VALUE
	};
	protected static final int[] stencilFuncToGL = {
		GL.GL_NEVER,               // STST_FUNCTION_NEVER_PASS_STENCIL_TEST
		GL.GL_ALWAYS,              // STST_FUNCTION_ALWAYS_PASS_STENCIL_TEST
		GL.GL_EQUAL,               // STST_FUNCTION_PASS_TEST_IF_MATCHES
		GL.GL_NOTEQUAL,            // STST_FUNCTION_PASS_TEST_IF_DIFFERS
		GL.GL_LESS,                // STST_FUNCTION_PASS_TEST_IF_LESS
		GL.GL_LEQUAL,              // STST_FUNCTION_PASS_TEST_IF_LESS_OR_EQUAL
		GL.GL_GREATER,             // STST_FUNCTION_PASS_TEST_IF_GREATER
		GL.GL_GEQUAL               // STST_FUNCTION_PASS_TEST_IF_GREATER_OR_EQUAL
	};
	protected static final int[] alphaFuncToGL = {
		GL.GL_NEVER,               // ATST_NEVER_PASS_PIXEL
		GL.GL_ALWAYS,              // ATST_ALWAYS_PASS_PIXEL
		GL.GL_EQUAL,               // ATST_PASS_PIXEL_IF_MATCHES
		GL.GL_NOTEQUAL,            // ATST_PASS_PIXEL_IF_DIFFERS
		GL.GL_LESS,                // ATST_PASS_PIXEL_IF_LESS
		GL.GL_LEQUAL,              // ATST_PASS_PIXEL_IF_LESS_OR_EQUAL
		GL.GL_GREATER,             // ATST_PASS_PIXEL_IF_GREATER
		GL.GL_GEQUAL               // ATST_PASS_PIXEL_IF_GREATER_OR_EQUAL
	};
	protected static final int[] blendModeToGL = {
		GL.GL_FUNC_ADD,            // ALPHA_SOURCE_BLEND_OPERATION_ADD
		GL.GL_FUNC_SUBTRACT,       // ALPHA_SOURCE_BLEND_OPERATION_SUBTRACT
		GL.GL_FUNC_REVERSE_SUBTRACT,// ALPHA_SOURCE_BLEND_OPERATION_REVERSE_SUBTRACT
		GL.GL_MIN,                 // ALPHA_SOURCE_BLEND_OPERATION_MINIMUM_VALUE
		GL.GL_MAX,                 // ALPHA_SOURCE_BLEND_OPERATION_MAXIMUM_VALUE
		GL.GL_FUNC_ADD             // ALPHA_SOURCE_BLEND_OPERATION_ABSOLUTE_VALUE
	};

	protected GL gl;

	public static IRenderingEngine newInstance(GL gl) {
		String openGLVersion = gl.glGetString(GL.GL_VERSION);
		if (openGLVersion.compareTo("1.5") >= 0) {
			return new RenderingEngineJogl15(gl);
		} else if (openGLVersion.compareTo("1.2") >= 0) {
			return new RenderingEngineJogl12(gl);
		}

		return new RenderingEngineJogl(gl);
	}

	public RenderingEngineJogl(GL gl) {
		this.gl = gl;
		init();
	}

	protected void init() {
		String openGLVersion = gl.glGetString(GL.GL_VERSION);
        log.info("OpenGL version: " + openGLVersion);
	}

	@Override
	public void endDirectRendering() {
		// Nothing to do
	}

	@Override
	public void startDirectRendering(boolean textureEnabled, boolean depthWriteEnabled, boolean colorWriteEnabled, boolean setOrthoMatrix, boolean orthoInverted, int width, int height) {
		// Nothing to do
	}

	@Override
	public void startDisplay() {
		// Nothing to do
	}

	@Override
	public void endDisplay() {
		// Nothing to do
	}

	@Override
	public void disableFlag(int flag) {
		int glFlag = flagToGL[flag];
		if (glFlag != 0) {
			gl.glDisable(glFlag);
		}
	}

	@Override
	public void enableFlag(int flag) {
		int glFlag = flagToGL[flag];
		if (glFlag != 0) {
			gl.glEnable(glFlag);
		}
	}

	@Override
	public void setMorphWeight(int index, float value) {
		// Nothing to do
	}

	@Override
	public void setPatchDiv(int s, int t) {
		// Nothing to do
	}

	@Override
	public void setPatchPrim(int prim) {
		// Nothing to do
	}

	@Override
	public void setDepthRange(float zpos, float zscale, float near, float far) {
        gl.glDepthRange(near, far);
	}

	@Override
	public void setDepthFunc(int func) {
        gl.glDepthFunc(depthFuncToGL[func]);
	}

	@Override
	public void setViewport(int x, int y, int width, int height) {
        gl.glViewport(x, y, width, height);
	}

	@Override
	public void setShadeModel(int model) {
		gl.glShadeModel(shadeModelToGL[model]);
	}

	@Override
	public void setMaterialColor(int type, float[] color) {
        gl.glMaterialfv(GL.GL_FRONT, colorTypeToGL[type], color, 0);
	}

	@Override
	public void setProjectionMatrix(float[] values) {
        gl.glMatrixMode(GL.GL_PROJECTION);
        if (values != null) {
        	gl.glLoadMatrixf(values, 0);
        } else {
        	gl.glLoadIdentity();
        }
	}

	@Override
	public void setViewMatrix(float[] values) {
		// The View matrix has always to be set BEFORE the Model matrix
        gl.glMatrixMode(GL.GL_MODELVIEW);
        if (values != null) {
        	gl.glLoadMatrixf(values, 0);
        } else {
        	gl.glLoadIdentity();
        }
	}

	@Override
	public void setModelMatrix(float[] values) {
		// The Model matrix has always to be set AFTER the View matrix
        gl.glMatrixMode(GL.GL_MODELVIEW);
        if (values != null) {
        	gl.glMultMatrixf(values, 0);
        }
	}

	@Override
	public void endModelViewMatrixUpdate() {
		// Nothing to do
	}

	@Override
	public void setModelViewMatrix(float[] values) {
        gl.glMatrixMode(GL.GL_MODELVIEW);
        if (values != null) {
        	gl.glLoadMatrixf(values, 0);
        } else {
        	gl.glLoadIdentity();
        }
	}

	@Override
	public void setTextureMatrix(float[] values) {
        gl.glMatrixMode(GL.GL_TEXTURE);
        if (values != null) {
        	gl.glLoadMatrixf(values, 0);
        } else {
        	gl.glLoadIdentity();
        }
	}

	@Override
	public void setLightModelAmbientColor(float[] color) {
        gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, color, 0);
	}

	@Override
	public void setLightMode(int mode) {
        gl.glLightModeli(GL.GL_LIGHT_MODEL_COLOR_CONTROL, lightModeToGL[mode]);
	}

	@Override
	public void setLightColor(int type, int light, float[] color) {
		gl.glLightfv(GL.GL_LIGHT0 + light, colorTypeToGL[type], color, 0);
	}

	@Override
	public void setLightConstantAttenuation(int light, float constant) {
        gl.glLightf(GL.GL_LIGHT0 + light, GL.GL_CONSTANT_ATTENUATION, constant);
	}

	@Override
	public void setLightLinearAttenuation(int light, float linear) {
        gl.glLightf(GL.GL_LIGHT0 + light, GL.GL_LINEAR_ATTENUATION, linear);
	}

	@Override
	public void setLightQuadraticAttenuation(int light, float quadratic) {
        gl.glLightf(GL.GL_LIGHT0 + light, GL.GL_QUADRATIC_ATTENUATION, quadratic);
	}

	@Override
	public void setLightDirection(int light, float[] direction) {
        gl.glLightfv(GL.GL_LIGHT0 + light, GL.GL_SPOT_DIRECTION, direction, 0);
	}

	@Override
	public void setLightPosition(int light, float[] position) {
        gl.glLightfv(GL.GL_LIGHT0 + light, GL.GL_POSITION, position, 0);
	}

	@Override
	public void setLightSpotCutoff(int light, float cutoff) {
        gl.glLightf(GL.GL_LIGHT0 + light, GL.GL_SPOT_CUTOFF, cutoff);
	}

	@Override
	public void setLightSpotExponent(int light, float exponent) {
        gl.glLightf(GL.GL_LIGHT0 + light, GL.GL_SPOT_EXPONENT, exponent);
	}

	@Override
	public void setLightType(int light, int type, int kind) {
		// Nothing to do
	}

	@Override
	public void setBlendColor(float[] color) {
		gl.glBlendColor(color[0], color[1], color[2], color[3]);
	}

	@Override
	public void setBlendFunc(int src, int dst) {
		try {
			gl.glBlendFunc(blendSrcToGL[src], blendDstToGL[dst]);
		} catch (GLException e) {
			VideoEngine.log.warn(e.toString());
		}
	}

	@Override
	public void setLogicOp(int logicOp) {
        gl.glLogicOp(logicOpToGL[logicOp]);
	}

	@Override
	public void setColorMask(boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled) {
        gl.glColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, alphaWriteEnabled);
	}

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
    	// Not supported, nothing to do
    }

	@Override
	public void setDepthMask(boolean depthWriteEnabled) {
		gl.glDepthMask(depthWriteEnabled);
	}

	@Override
	public void setTextureWrapMode(int s, int t) {
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, wrapModeToGL[s]);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, wrapModeToGL[t]);
	}

	@Override
	public void setTextureMipmapMinLevel(int level) {
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_BASE_LEVEL, level);
	}

	@Override
	public void setTextureMipmapMaxLevel(int level) {
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_LEVEL, level);
	}

	@Override
	public void setTextureMipmapMinFilter(int filter) {
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, mipmapFilterToGL[filter]);
	}

	@Override
	public void setTextureMipmapMagFilter(int filter) {
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, mipmapFilterToGL[filter]);
	}

	@Override
	public void setColorMaterial(boolean ambient, boolean diffuse, boolean specular) {
		int index = (ambient ? 1 : 0) | (diffuse ? 2 : 0) | (specular ? 4 : 0);
        gl.glColorMaterial(GL.GL_FRONT_AND_BACK, colorMaterialToGL[index]);
	}

	@Override
	public void setTextureEnvironmentMapping(int u, int v) {
        gl.glTexGeni(GL.GL_S, GL.GL_TEXTURE_GEN_MODE, GL.GL_SPHERE_MAP);
        gl.glTexGeni(GL.GL_T, GL.GL_TEXTURE_GEN_MODE, GL.GL_SPHERE_MAP);
	}

	@Override
	public void setVertexColor(float[] color) {
        gl.glColor4fv(color, 0);
	}

	@Override
	public void setUniform(int id, int value) {
        gl.glUniform1i(id, value);
	}

	@Override
	public void setUniform(int id, int value1, int value2) {
        gl.glUniform2i(id, value1, value2);
	}

	@Override
	public void setUniform(int id, float value) {
        gl.glUniform1f(id, value);
	}
	@Override
	public void setUniform3(int id, int[] values) {
        gl.glUniform3iv(id, 1, values, 0);
	}

	@Override
	public void setUniform4(int id, int[] values) {
        gl.glUniform4iv(id, 1, values, 0);
	}

	@Override
	public void setUniformMatrix4(int id, int count, float[] values) {
        gl.glUniformMatrix4fv(id, count, false, values, 0);
	}

	@Override
	public void setColorTestFunc(int func) {
		// Not supported
	}

	@Override
	public void setColorTestMask(int[] values) {
		// Not supported
	}

	@Override
	public void setColorTestReference(int[] values) {
		// Not supported
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
		// Nothing to do
	}

	@Override
	public int setBones(int count, float[] values) {
		return 0; // Bones are not supported
	}

	@Override
	public void setTextureMapMode(int mode, int proj) {
		// Nothing to do
	}

	@Override
	public void setTexEnv(int name, int param) {
		gl.glTexEnvi(GL.GL_TEXTURE_ENV, texEnvNameToGL[name], texEnvParamToGL[param]);
	}

	@Override
	public void setTexEnv(int name, float param) {
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, texEnvNameToGL[name], param);
	}

	@Override
	public void endClearMode() {
		// Nothing to do
	}

	@Override
	public void startClearMode(boolean color, boolean stencil, boolean depth) {
		// Nothing to do
	}

	@Override
	public void attachShader(int program, int shader) {
		gl.glAttachShader(program, shader);
	}

	@Override
	public void compilerShader(int shader, String[] source) {
		gl.glShaderSource(shader, 1, source, null, 0);
		gl.glCompileShader(shader);
	}

	@Override
	public int createProgram() {
		return gl.glCreateProgram();
	}

	@Override
	public void useProgram(int program) {
		gl.glUseProgram(program);
	}

	@Override
	public int createShader(int type) {
		return gl.glCreateShader(shaderTypeToGL[type]);
	}

	@Override
	public int getAttribLocation(int program, String name) {
		return gl.glGetAttribLocation(program, name);
	}

	@Override
	public int getUniformLocation(int program, String name) {
		return gl.glGetUniformLocation(program, name);
	}

	@Override
	public void linkProgram(int program) {
		gl.glLinkProgram(program);
	}

	@Override
	public void validateProgram(int program) {
		gl.glValidateProgram(program);
	}

	@Override
	public String getProgramInfoLog(int program) {
		int[] infoLogLength = new int[1];
		int[] charsWritten = new int[1];

		gl.glGetProgramiv(program, GL.GL_INFO_LOG_LENGTH, infoLogLength, 0);

		int length = infoLogLength[0];
		if (length <= 1) {
			return null;
		}

		byte[] infoLog = new byte[length];
        gl.glGetProgramInfoLog(program, length, charsWritten, 0, infoLog, 0);

        return new String(infoLog, 0, length - 1);
	}

	@Override
	public String getShaderInfoLog(int shader) {
		int[] infoLogLength = new int[1];
		int[] charsWritten = new int[1];

		gl.glGetShaderiv(shader, GL.GL_INFO_LOG_LENGTH, infoLogLength, 0);

		int length = infoLogLength[0];
		if (length <= 1) {
			return null;
		}

		byte[] infoLog = new byte[length];
        gl.glGetShaderInfoLog(shader, length, charsWritten, 0, infoLog, 0);

        return new String(infoLog, 0, length - 1);
	}

	@Override
	public boolean isFunctionAvailable(String name) {
		return gl.isFunctionAvailable(name);
	}

	@Override
	public void drawArrays(int primitive, int first, int count) {
		gl.glDrawArrays(primitiveToGL[primitive], first, count);
	}

	@Override
	public void deleteBuffer(int buffer) {
		int[] buffers = new int[] { buffer };
		gl.glDeleteBuffersARB(1, buffers, 0);
	}

	@Override
	public int genBuffer() {
		int[] buffers = new int[1];
		gl.glGenBuffersARB(1, buffers, 0);
		return buffers[0];
	}

	@Override
	public void setBufferData(int size, Buffer buffer, int usage) {
		gl.glBufferDataARB(bufferTarget, size, buffer, bufferUsageToGL[usage]);
	}

	@Override
	public void bindBuffer(int buffer) {
		gl.glBindBufferARB(bufferTarget, buffer);
	}

	@Override
	public void enableClientState(int type) {
		gl.glEnableClientState(clientStateToGL[type]);
	}

	@Override
	public void enableVertexAttribArray(int id) {
		gl.glEnableVertexAttribArray(id);
	}

	@Override
	public void disableClientState(int type) {
		gl.glDisableClientState(clientStateToGL[type]);
	}

	@Override
	public void disableVertexAttribArray(int id) {
		gl.glDisableVertexAttribArray(id);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, long offset) {
		gl.glColorPointer(size, pointerTypeToGL[type], stride, offset);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, Buffer buffer) {
		gl.glColorPointer(size, pointerTypeToGL[type], stride, buffer);
	}

	@Override
	public void setNormalPointer(int type, int stride, long offset) {
		gl.glNormalPointer(pointerTypeToGL[type], stride, offset);
	}

	@Override
	public void setNormalPointer(int type, int stride, Buffer buffer) {
		gl.glNormalPointer(pointerTypeToGL[type], stride, buffer);
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, long offset) {
		gl.glTexCoordPointer(size, pointerTypeToGL[type], stride, offset);
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, Buffer buffer) {
		gl.glTexCoordPointer(size, pointerTypeToGL[type], stride, buffer);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, long offset) {
		gl.glVertexPointer(size, pointerTypeToGL[type], stride, offset);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, Buffer buffer) {
		gl.glVertexPointer(size, pointerTypeToGL[type], stride, buffer);
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, long offset) {
		gl.glVertexAttribPointer(id, size, pointerTypeToGL[type], normalized, stride, offset);
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, Buffer buffer) {
		gl.glVertexAttribPointer(id, size, pointerTypeToGL[type], normalized, stride, buffer);
	}

	@Override
	public void setPixelStore(int rowLength, int alignment) {
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, alignment);
        gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, rowLength);
	}

	@Override
	public int genTexture() {
		int[] textures = new int[1];
		gl.glGenTextures(1, textures, 0);
		return textures[0];
	}

	@Override
	public void bindTexture(int texture) {
		gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
	}

	@Override
	public void deleteTexture(int texture) {
		int[] textures = new int[] { texture };
		gl.glDeleteTextures(1, textures, 0);
	}

	@Override
	public void setCompressedTexImage(int level, int internalFormat, int width, int height, int compressedSize, Buffer buffer) {
		gl.glCompressedTexImage2D(GL.GL_TEXTURE_2D, level, textureFormatToGL[internalFormat], width, height, 0, compressedSize, buffer);
	}

	@Override
	public void setTexImage(int level, int internalFormat, int width, int height, int format, int type, Buffer buffer) {
		gl.glTexImage2D(GL.GL_TEXTURE_2D, level, textureFormatToGL[internalFormat], width, height, 0, textureFormatToGL[format], textureTypeToGL[type], buffer);
	}

	@Override
	public void setTexSubImage(int level, int xOffset, int yOoffset, int width, int height, int format, int type, Buffer buffer) {
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, level, xOffset, yOoffset, width, height, textureFormatToGL[format], textureTypeToGL[type], buffer);
	}

	@Override
	public void setStencilOp(int fail, int zfail, int zpass) {
		gl.glStencilOp(stencilOpToGL[fail], stencilOpToGL[zfail], stencilOpToGL[zpass]);
	}

	@Override
	public void setStencilFunc(int func, int ref, int mask) {
		gl.glStencilFunc(stencilFuncToGL[func], ref, mask);
	}

	@Override
	public void setAlphaFunc(int func, int ref) {
		gl.glAlphaFunc(alphaFuncToGL[func], ref / 255.0f);
	}

	@Override
	public void setFogColor(float[] color) {
		gl.glFogfv(GL.GL_FOG_COLOR, color, 0);
	}

	@Override
	public void setFogDist(float start, float end) {
        gl.glFogf(GL.GL_FOG_START, start);
        gl.glFogf(GL.GL_FOG_END, end);
	}

	@Override
	public void setTextureEnvColor(float[] color) {
        gl.glTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_COLOR, color, 0);
	}

	@Override
	public void setFrontFace(boolean cw) {
		gl.glFrontFace(cw ? GL.GL_CW : GL.GL_CCW);
	}

	@Override
	public void setScissor(int x, int y, int width, int height) {
		gl.glScissor(x, y, width, height);
	}

	@Override
	public void setBlendEquation(int mode) {
        try {
            gl.glBlendEquation(blendModeToGL[mode]);
        } catch (GLException e) {
            log.warn("VideoEngine: " + e.getMessage());
        }
	}

	@Override
	public void setFogHint() {
        gl.glFogi(GL.GL_FOG_MODE, GL.GL_LINEAR);
        gl.glHint(GL.GL_FOG_HINT, GL.GL_DONT_CARE);
	}

	@Override
	public void setLineSmoothHint() {
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
	}

	@Override
	public void setMaterialShininess(float shininess) {
        gl.glMaterialf(GL.GL_FRONT, GL.GL_SHININESS, shininess);
	}

	@Override
	public void beginDraw(int primitive) {
		gl.glBegin(primitiveToGL[primitive]);
	}

	@Override
	public void beginQuery(int id) {
		gl.glBeginQuery(GL.GL_SAMPLES_PASSED, id);
	}

	@Override
	public void drawVertex3(float[] values) {
		gl.glVertex3fv(values, 0);
	}

	@Override
	public void endDraw() {
		gl.glEnd();
	}

	@Override
	public void endQuery() {
		gl.glEndQuery(GL.GL_SAMPLES_PASSED);
	}

	@Override
	public void drawColor(float value1, float value2, float value3) {
		gl.glColor3f(value1, value2, value3);
	}

	@Override
	public void drawColor(float value1, float value2, float value3, float value4) {
		gl.glColor4f(value1, value2, value3, value4);
	}

	@Override
	public void drawTexCoord(float value1, float value2) {
		gl.glTexCoord2f(value1, value2);
	}

	@Override
	public void drawVertex(int value1, int value2) {
		gl.glVertex2i(value1, value2);
	}

	@Override
	public void drawVertex(float value1, float value2) {
		gl.glVertex2f(value1, value2);
	}

	@Override
	public int genQuery() {
		int[] queries = new int[1];
		gl.glGenQueries(1, queries, 0);
		return queries[0];
	}

	@Override
	public void drawBoundingBox(float[][] values) {
		// Nothing to do
	}

	@Override
	public void endBoundingBox() {
		// Nothing to do
	}

	@Override
	public void beginBoundingBox() {
		// Nothing to do
	}

	@Override
	public boolean hasBoundingBox() {
		return true;
	}

	@Override
	public boolean isBoundingBoxVisible() {
		return true;
	}

	@Override
	public boolean getQueryResultAvailable(int id) {
        int[] result = new int[1];
        gl.glGetQueryObjectiv(id, GL.GL_QUERY_RESULT_AVAILABLE, result, 0);
        // 0 means result not yet available, 1 means result available
		return result[0] != 0;
	}

	@Override
	public int getQueryResult(int id) {
        int[] result = new int[1];
        gl.glGetQueryObjectiv(id, GL.GL_QUERY_RESULT, result, 0);
		return result[0];
	}

	@Override
	public void copyTexSubImage(int level, int xOffset, int yOffset, int x, int y, int width, int height) {
		gl.glCopyTexSubImage2D(GL.GL_TEXTURE_2D, level, xOffset, yOffset, x, y, width, height);
	}

	@Override
	public void getTexImage(int level, int format, int type, Buffer buffer) {
		gl.glGetTexImage(GL.GL_TEXTURE_2D, level, textureFormatToGL[format], textureTypeToGL[type], buffer);
	}

	@Override
	public void readPixels(int x, int y, int width, int height, int format,	int type, Buffer buffer) {
		gl.glReadPixels(x, y, width, height, textureFormatToGL[format], textureTypeToGL[type], buffer);
	}

	@Override
	public void clear(float red, float green, float blue, float alpha) {
        gl.glClearColor(red, green, blue, alpha);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, long offset) {
		// Nothing to do
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, Buffer buffer) {
		// Nothing to do
	}
}
