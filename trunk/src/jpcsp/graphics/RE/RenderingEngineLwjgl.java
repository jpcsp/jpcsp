/*

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

import static jpcsp.graphics.RE.DirectBufferUtilities.allocateDirectBuffer;
import static jpcsp.graphics.RE.DirectBufferUtilities.copyBuffer;
import static jpcsp.graphics.RE.DirectBufferUtilities.getDirectBuffer;
import static jpcsp.graphics.RE.DirectBufferUtilities.getDirectByteBuffer;
import static jpcsp.graphics.RE.DirectBufferUtilities.getDirectFloatBuffer;
import static jpcsp.graphics.RE.DirectBufferUtilities.getDirectIntBuffer;
import static jpcsp.graphics.RE.DirectBufferUtilities.getDirectShortBuffer;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.opengl.ARBUniformBufferObject;
import org.lwjgl.opengl.EXTGeometryShader4;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GLContext;

import jpcsp.graphics.VertexInfo;
import jpcsp.graphics.RE.buffer.IREBufferManager;

/**
 * @author gid15
 *
 * An abstract RenderingEngine implementing calls to OpenGL using LWJGL.
 * The class contains no rendering logic, it just implements the interface to LWJGL.
 */
public class RenderingEngineLwjgl extends BaseRenderingEngine {
	protected static final int[] flagToGL = {
		GL11.GL_ALPHA_TEST,     // GU_ALPHA_TEST
		GL11.GL_DEPTH_TEST,     // GU_DEPTH_TEST
		GL11.GL_SCISSOR_TEST,   // GU_SCISSOR_TEST
		GL11.GL_STENCIL_TEST,   // GU_STENCIL_TEST
		GL11.GL_BLEND,          // GU_BLEND
		GL11.GL_CULL_FACE,      // GU_CULL_FACE
		GL11.GL_DITHER,         // GU_DITHER
		GL11.GL_FOG,            // GU_FOG
		0,                    // GU_CLIP_PLANES
		GL11.GL_TEXTURE_2D,     // GU_TEXTURE_2D
		GL11.GL_LIGHTING,       // GU_LIGHTING
		GL11.GL_LIGHT0,         // GU_LIGHT0
		GL11.GL_LIGHT1,         // GU_LIGHT1
		GL11.GL_LIGHT2,         // GU_LIGHT2
		GL11.GL_LIGHT3,         // GU_LIGHT3
		GL11.GL_LINE_SMOOTH,    // GU_LINE_SMOOTH
		0,                    // GU_PATCH_CULL_FACE
		0,                    // GU_COLOR_TEST
		GL11.GL_COLOR_LOGIC_OP, // GU_COLOR_LOGIC_OP
		0,                    // GU_FACE_NORMAL_REVERSE
		0,                    // GU_PATCH_FACE
		0,                    // GU_FRAGMENT_2X
		GL11.GL_COLOR_MATERIAL, // RE_COLOR_MATERIAL
		GL11.GL_TEXTURE_GEN_S,  // RE_TEXTURE_GEN_S
		GL11.GL_TEXTURE_GEN_T,  // RE_TEXTURE_GEN_T
	};
	protected static final int[] shadeModelToGL = {
		GL11.GL_FLAT,           // GU_FLAT
		GL11.GL_SMOOTH          // GU_SMOOTH
	};
	protected static final int[] colorTypeToGL = {
		GL11.GL_AMBIENT,        // RE_AMBIENT
		GL11.GL_EMISSION,       // RE_EMISSIVE
		GL11.GL_DIFFUSE,        // RE_DIFFUSE
		GL11.GL_SPECULAR        // RE_SPECULAR
	};
	protected static final int[] lightModeToGL = {
		GL12.GL_SINGLE_COLOR,   // GU_SINGLE_COLOR
		GL12.GL_SEPARATE_SPECULAR_COLOR // GU_SEPARATE_SPECULAR_COLOR
	};
	protected static final int[] blendSrcToGL = {
		GL11.GL_DST_COLOR,           // GU_SRC_COLOR
		GL11.GL_ONE_MINUS_DST_COLOR, // GU_ONE_MINUS_SRC_COLOR
		GL11.GL_SRC_ALPHA,           // GU_SRC_ALPHA
		GL11.GL_ONE_MINUS_SRC_ALPHA, // GU_ONE_MINUS_SRC_ALPHA
		GL11.GL_DST_ALPHA,           // 4
		GL11.GL_ONE_MINUS_DST_ALPHA, // 5
		GL11.GL_SRC_ALPHA,           // 6
		GL11.GL_ONE_MINUS_SRC_ALPHA, // 7
		GL11.GL_DST_ALPHA,           // 8
		GL11.GL_ONE_MINUS_DST_ALPHA, // 9
		GL11.GL_CONSTANT_COLOR,      // GU_FIX_BLEND_COLOR
		GL11.GL_ONE_MINUS_CONSTANT_COLOR, // GU_FIX_BLEND_ONE_MINUS_COLOR
		GL11.GL_ZERO,                // GU_FIX for 0x000000
		GL11.GL_ONE                  // GU_FIX for 0xFFFFFF
	};
	protected static final int[] blendDstToGL = {
		GL11.GL_SRC_COLOR,           // GU_SRC_COLOR
		GL11.GL_ONE_MINUS_SRC_COLOR, // GU_ONE_MINUS_SRC_COLOR
		GL11.GL_SRC_ALPHA,           // GU_SRC_ALPHA
		GL11.GL_ONE_MINUS_SRC_ALPHA, // GU_ONE_MINUS_SRC_ALPHA
		GL11.GL_DST_ALPHA,           // 4
		GL11.GL_ONE_MINUS_DST_ALPHA, // 5
		GL11.GL_SRC_ALPHA,           // 6
		GL11.GL_ONE_MINUS_SRC_ALPHA, // 7
		GL11.GL_DST_ALPHA,           // 8
		GL11.GL_ONE_MINUS_DST_ALPHA, // 9
		GL11.GL_CONSTANT_COLOR,      // GU_FIX_BLEND_COLOR
		GL11.GL_ONE_MINUS_CONSTANT_COLOR, // GU_FIX_BLEND_ONE_MINUS_COLOR
		GL11.GL_ZERO,                // GU_FIX_BLACK
		GL11.GL_ONE                  // GU_FIX_WHITE
	};
	protected static final int[] logicOpToGL = {
		GL11.GL_CLEAR,               // LOP_CLEAR
		GL11.GL_AND,                 // LOP_AND
		GL11.GL_AND_REVERSE,         // LOP_REVERSE_AND
		GL11.GL_COPY,                // LOP_COPY
		GL11.GL_AND_INVERTED,        // LOP_INVERTED_AND
		GL11.GL_NOOP,                // LOP_NO_OPERATION
		GL11.GL_XOR,                 // LOP_EXLUSIVE_OR
		GL11.GL_OR,                  // LOP_OR
		GL11.GL_NOR,                 // LOP_NEGATED_OR
		GL11.GL_EQUIV,               // LOP_EQUIVALENCE
		GL11.GL_INVERT,              // LOP_INVERTED
		GL11.GL_OR_REVERSE,          // LOP_REVERSE_OR
		GL11.GL_COPY_INVERTED,       // LOP_INVERTED_COPY
		GL11.GL_OR_INVERTED,         // LOP_INVERTED_OR
		GL11.GL_NAND,                // LOP_NEGATED_AND
		GL11.GL_SET                  // LOP_SET
	};
	protected static final int[] wrapModeToGL = {
		GL11.GL_REPEAT,              // TWRAP_WRAP_MODE_REPEAT
		GL12.GL_CLAMP_TO_EDGE        // TWRAP_WRAP_MODE_CLAMP
	};
	protected static final int[] colorMaterialToGL = {
		GL11.GL_AMBIENT,             // none
		GL11.GL_AMBIENT,             // ambient
		GL11.GL_DIFFUSE,             // diffuse
		GL11.GL_AMBIENT_AND_DIFFUSE, // ambient, diffuse
		GL11.GL_SPECULAR,            // specular
		GL11.GL_AMBIENT,             // ambient, specular
		GL11.GL_DIFFUSE,             // diffuse, specular
		GL11.GL_AMBIENT_AND_DIFFUSE  // ambient, diffuse, specular
	};
	protected static final int[] depthFuncToGL = {
		GL11.GL_NEVER,               // ZTST_FUNCTION_NEVER_PASS_PIXEL
		GL11.GL_ALWAYS,              // ZTST_FUNCTION_ALWAYS_PASS_PIXEL
		GL11.GL_EQUAL,               // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_EQUAL
		GL11.GL_NOTEQUAL,            // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_ISNOT_EQUAL
		GL11.GL_LESS,                // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS
		GL11.GL_LEQUAL,              // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_LESS_OR_EQUAL
		GL11.GL_GREATER,             // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER
		GL11.GL_GEQUAL               // ZTST_FUNCTION_PASS_PX_WHEN_DEPTH_IS_GREATER_OR_EQUAL
	};
	protected static final int[] texEnvNameToGL = {
		GL13.GL_COMBINE_RGB,         // RE_TEXENV_COMBINE_RGB
		GL13.GL_COMBINE_ALPHA,       // RE_TEXENV_COMBINE_ALPHA
		GL13.GL_RGB_SCALE,           // RE_TEXENV_RGB_SCALE
		GL11.GL_ALPHA_SCALE,         // RE_TEXENV_ALPHA_SCALE
		GL15.GL_SRC0_RGB,            // RE_TEXENV_SRC0_RGB
		GL15.GL_SRC1_RGB,            // RE_TEXENV_SRC1_RGB
		GL15.GL_SRC2_RGB,            // RE_TEXENV_SRC2_RGB
		GL15.GL_SRC0_ALPHA,          // RE_TEXENV_SRC0_ALPHA
		GL15.GL_SRC1_ALPHA,          // RE_TEXENV_SRC1_ALPHA
		GL15.GL_SRC2_ALPHA,          // RE_TEXENV_SRC2_ALPHA
		GL13.GL_OPERAND0_RGB,        // RE_TEXENV_OPERAND0_RGB
		GL13.GL_OPERAND1_RGB,        // RE_TEXENV_OPERAND1_RGB
		GL13.GL_OPERAND2_RGB,        // RE_TEXENV_OPERAND2_RGB
		GL13.GL_OPERAND0_ALPHA,      // RE_TEXENV_OPERAND0_ALPHA
		GL13.GL_OPERAND1_ALPHA,      // RE_TEXENV_OPERAND1_ALPHA
		GL13.GL_OPERAND2_ALPHA,      // RE_TEXENV_OPERAND2_ALPHA
		GL11.GL_TEXTURE_ENV_MODE     // RE_TEXENV_ENV_MODE
	};
	protected static final int[] texEnvParamToGL = {
		GL11.GL_MODULATE,            // RE_TEXENV_MODULATE
		GL11.GL_DECAL,               // RE_TEXENV_DECAL
		GL11.GL_BLEND,               // RE_TEXENV_BLEND
		GL11.GL_REPLACE,             // RE_TEXENV_REPLACE
		GL11.GL_ADD,                 // RE_TEXENV_ADD
		GL13.GL_INTERPOLATE,         // RE_TEXENV_INTERPOLATE
		GL13.GL_SUBTRACT,            // RE_TEXENV_SUBTRACT
		GL11.GL_TEXTURE,             // RE_TEXENV_TEXTURE
		GL13.GL_CONSTANT,            // RE_TEXENV_CONSTANT
		GL13.GL_PREVIOUS,            // RE_TEXENV_PREVIOUS
		GL11.GL_SRC_COLOR,           // RE_TEXENV_SRC_COLOR
		GL11.GL_SRC_ALPHA,           // RE_TEXENV_SRC_ALPHA
		GL13.GL_COMBINE              // RE_TEXENV_COMBINE
	};
	protected static final int[] shaderTypeToGL = {
		GL20.GL_VERTEX_SHADER,       // RE_VERTEX_SHADER
		GL20.GL_FRAGMENT_SHADER,     // RE_FRAGMENT_SHADER
		GL32.GL_GEOMETRY_SHADER      // RE_GEOMETRY_SHADER
	};
	protected static final int[] primitiveToGL = {
		GL11.GL_POINTS,              // GU_POINTS / PRIM_POINT
		GL11.GL_LINES,               // GU_LINES / PRIM_LINE
		GL11.GL_LINE_STRIP,          // GU_LINE_STRIP / PRIM_LINES_STRIPS
		GL11.GL_TRIANGLES,           // GU_TRIANGLES / PRIM_TRIANGLE
		GL11.GL_TRIANGLE_STRIP,      // GU_TRIANGLE_STRIP / PRIM_TRIANGLE_STRIPS
		GL11.GL_TRIANGLE_FAN,        // GU_TRIANGLE_FAN / PRIM_TRIANGLE_FANS
		GL11.GL_QUADS,               // GU_SPRITES / PRIM_SPRITES
		GL11.GL_QUADS,               // RE_QUADS
		GL32.GL_LINES_ADJACENCY,     // RE_LINES_ADJACENCY
		GL32.GL_TRIANGLES_ADJACENCY, // RE_TRIANGLES_ADJACENCY
		GL32.GL_TRIANGLE_STRIP_ADJACENCY // RE_TRIANGLE_STRIP_ADJACENCY
	};
	protected static final int[] clientStateToGL = {
		GL11.GL_TEXTURE_COORD_ARRAY, // RE_TEXTURE
		GL11.GL_COLOR_ARRAY,         // RE_COLOR
		GL11.GL_NORMAL_ARRAY,        // RE_NORMAL
		GL11.GL_VERTEX_ARRAY         // RE_VERTEX
	};
	protected static final int[] pointerTypeToGL = {
		GL11.GL_BYTE,                // RE_BYTE
		GL11.GL_UNSIGNED_BYTE,       // RE_UNSIGNED_BYTE
		GL11.GL_SHORT,               // RE_SHORT
		GL11.GL_UNSIGNED_SHORT,      // RE_UNSIGNED_SHORT
		GL11.GL_INT,                 // RE_INT
		GL11.GL_UNSIGNED_INT,        // RE_UNSIGNED_INT
		GL11.GL_FLOAT,               // RE_FLOAT
		GL11.GL_DOUBLE               // RE_DOUBLE
	};
	protected static final int[] bufferUsageToGL = {
		GL15.GL_STREAM_DRAW,         // RE_STREAM_DRAW
		GL15.GL_STREAM_READ,         // RE_STREAM_READ
		GL15.GL_STREAM_COPY,         // RE_STREAM_COPY
		GL15.GL_STATIC_DRAW,         // RE_STATIC_DRAW
		GL15.GL_STATIC_READ,         // RE_STATIC_READ
		GL15.GL_STATIC_COPY,         // RE_STATIC_COPY
		GL15.GL_DYNAMIC_DRAW,        // RE_DYNAMIC_DRAW
		GL15.GL_DYNAMIC_READ,        // RE_DYNAMIC_READ
		GL15.GL_DYNAMIC_COPY         // RE_DYNAMIC_COPY
	};
	protected static final int[] mipmapFilterToGL = {
		GL11.GL_NEAREST,               // TFLT_NEAREST
		GL11.GL_LINEAR,                // TFLT_LINEAR
		GL11.GL_NEAREST,               // TFLT_UNKNOW1
		GL11.GL_NEAREST,               // TFLT_UNKNOW2
		GL11.GL_NEAREST_MIPMAP_NEAREST,// TFLT_NEAREST_MIPMAP_NEAREST
		GL11.GL_LINEAR_MIPMAP_NEAREST, // TFLT_LINEAR_MIPMAP_NEAREST
		GL11.GL_NEAREST_MIPMAP_LINEAR, // TFLT_NEAREST_MIPMAP_LINEAR
		GL11.GL_LINEAR_MIPMAP_LINEAR   // TFLT_LINEAR_MIPMAP_LINEAR
	};
	protected static final int[] textureFormatToGL = {
		GL11.GL_RGB,                           // TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
		GL11.GL_RGBA,                          // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
		GL11.GL_RGBA,                          // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
		GL11.GL_RGBA,                          // TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
        0,                                   // TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
        EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT,  // TPSM_PIXEL_STORAGE_MODE_DXT1
        EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, // TPSM_PIXEL_STORAGE_MODE_DXT3
        EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT  // TPSM_PIXEL_STORAGE_MODE_DXT5
	};
	protected static final int[] textureTypeToGL = {
        GL12.GL_UNSIGNED_SHORT_5_6_5_REV,      // TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650
        GL12.GL_UNSIGNED_SHORT_1_5_5_5_REV,    // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551
        GL12.GL_UNSIGNED_SHORT_4_4_4_4_REV,    // TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444
        GL11.GL_UNSIGNED_BYTE,                 // TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888
        0,                                   // TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_8BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_16BIT_INDEXED
        0,                                   // TPSM_PIXEL_STORAGE_MODE_32BIT_INDEXED
        EXTTextureCompressionS3TC.GL_COMPRESSED_RGB_S3TC_DXT1_EXT,  // TPSM_PIXEL_STORAGE_MODE_DXT1
        EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT, // TPSM_PIXEL_STORAGE_MODE_DXT3
        EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT  // TPSM_PIXEL_STORAGE_MODE_DXT5
	};
	protected static final int[] stencilOpToGL = {
		GL11.GL_KEEP,                // SOP_KEEP_STENCIL_VALUE
		GL11.GL_ZERO,                // SOP_ZERO_STENCIL_VALUE
		GL11.GL_REPLACE,             // SOP_REPLACE_STENCIL_VALUE
		GL11.GL_INVERT,              // SOP_INVERT_STENCIL_VALUE
		GL11.GL_INCR,                // SOP_INCREMENT_STENCIL_VALUE
		GL11.GL_DECR                 // SOP_DECREMENT_STENCIL_VALUE
	};
	protected static final int[] stencilFuncToGL = {
		GL11.GL_NEVER,               // STST_FUNCTION_NEVER_PASS_STENCIL_TEST
		GL11.GL_ALWAYS,              // STST_FUNCTION_ALWAYS_PASS_STENCIL_TEST
		GL11.GL_EQUAL,               // STST_FUNCTION_PASS_TEST_IF_MATCHES
		GL11.GL_NOTEQUAL,            // STST_FUNCTION_PASS_TEST_IF_DIFFERS
		GL11.GL_LESS,                // STST_FUNCTION_PASS_TEST_IF_LESS
		GL11.GL_LEQUAL,              // STST_FUNCTION_PASS_TEST_IF_LESS_OR_EQUAL
		GL11.GL_GREATER,             // STST_FUNCTION_PASS_TEST_IF_GREATER
		GL11.GL_GEQUAL               // STST_FUNCTION_PASS_TEST_IF_GREATER_OR_EQUAL
	};
	protected static final int[] alphaFuncToGL = {
		GL11.GL_NEVER,               // ATST_NEVER_PASS_PIXEL
		GL11.GL_ALWAYS,              // ATST_ALWAYS_PASS_PIXEL
		GL11.GL_EQUAL,               // ATST_PASS_PIXEL_IF_MATCHES
		GL11.GL_NOTEQUAL,            // ATST_PASS_PIXEL_IF_DIFFERS
		GL11.GL_LESS,                // ATST_PASS_PIXEL_IF_LESS
		GL11.GL_LEQUAL,              // ATST_PASS_PIXEL_IF_LESS_OR_EQUAL
		GL11.GL_GREATER,             // ATST_PASS_PIXEL_IF_GREATER
		GL11.GL_GEQUAL               // ATST_PASS_PIXEL_IF_GREATER_OR_EQUAL
	};
	protected static final int[] blendModeToGL = {
		GL14.GL_FUNC_ADD,            // ALPHA_SOURCE_BLEND_OPERATION_ADD
		GL14.GL_FUNC_SUBTRACT,       // ALPHA_SOURCE_BLEND_OPERATION_SUBTRACT
		GL14.GL_FUNC_REVERSE_SUBTRACT,// ALPHA_SOURCE_BLEND_OPERATION_REVERSE_SUBTRACT
		GL14.GL_MIN,                 // ALPHA_SOURCE_BLEND_OPERATION_MINIMUM_VALUE
		GL14.GL_MAX,                 // ALPHA_SOURCE_BLEND_OPERATION_MAXIMUM_VALUE
		GL14.GL_FUNC_ADD             // ALPHA_SOURCE_BLEND_OPERATION_ABSOLUTE_VALUE
	};
	protected static final int[] programParameterToGL = {
		GL32.GL_GEOMETRY_INPUT_TYPE,  // RE_GEOMETRY_INPUT_TYPE
		GL32.GL_GEOMETRY_OUTPUT_TYPE, // RE_GEOMETRY_OUTPUT_TYPE
		GL32.GL_GEOMETRY_VERTICES_OUT // RE_GEOMETRY_VERTICES_OUT
	};
	protected static final int[] bufferTargetToGL = {
		GL15.GL_ARRAY_BUFFER,         // RE_ARRAY_BUFFER
		ARBUniformBufferObject.GL_UNIFORM_BUFFER // RE_UNIFORM_BUFFER
	};
	protected static final int[] matrixModeToGL = {
		GL11.GL_PROJECTION,          // GU_PROJECTION
		GL11.GL_MODELVIEW,           // GU_VIEW
		GL11.GL_MODELVIEW,           // GU_MODEL
		GL11.GL_TEXTURE,             // GU_TEXTURE
		GL11.GL_MODELVIEW            // RE_MODELVIEW
	};

	public static IRenderingEngine newInstance() {
		if (GLContext.getCapabilities().OpenGL31) {
			log.info("Using RenderingEngineLwjgl31");
			return new RenderingEngineLwjgl31();
		} else if (GLContext.getCapabilities().OpenGL15) {
			log.info("Using RenderingEngineLwjgl15");
			return new RenderingEngineLwjgl15();
		} else if (GLContext.getCapabilities().OpenGL12) {
			log.info("Using RenderingEngineLwjgl12");
			return new RenderingEngineLwjgl12();
		}

		log.info("Using RenderingEngineLwjgl");
		return new RenderingEngineLwjgl();
	}

	public RenderingEngineLwjgl() {
		init();
	}

	protected void init() {
		String openGLVersion = GL11.glGetString(GL11.GL_VERSION);
        log.info("OpenGL version: " + openGLVersion);

        if (GLContext.getCapabilities().OpenGL20) {
        	String shadingLanguageVersion = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
        	log.info("Shading Language version: " + shadingLanguageVersion);
        }

        if (GLContext.getCapabilities().OpenGL30) {
        	int contextFlags = GL11.glGetInteger(GL30.GL_CONTEXT_FLAGS);
        	String s = String.format("GL_CONTEXT_FLAGS; 0x%X", contextFlags);
        	if ((contextFlags & GL30.GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT) != 0) {
        		s += " (GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT)";
        	}
        	log.info(s);
        }

        if (GLContext.getCapabilities().OpenGL32) {
        	int contextProfileMask = GL11.glGetInteger(GL32.GL_CONTEXT_PROFILE_MASK);
        	String s = String.format("GL_CONTEXT_PROFILE_MASK: 0x%X", contextProfileMask);
        	if ((contextProfileMask & GL32.GL_CONTEXT_CORE_PROFILE_BIT) != 0) {
        		s += " (GL_CONTEXT_CORE_PROFILE_BIT)";
        	}
        	if ((contextProfileMask & GL32.GL_CONTEXT_COMPATIBILITY_PROFILE_BIT) != 0) {
        		s += " (GL_CONTEXT_COMPATIBILITY_PROFILE_BIT)";
        	}
        	log.info(s);
        }
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
			GL11.glDisable(glFlag);
		}
	}

	@Override
	public void enableFlag(int flag) {
		int glFlag = flagToGL[flag];
		if (glFlag != 0) {
			GL11.glEnable(glFlag);
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
        GL11.glDepthRange(near, far);
	}

	@Override
	public void setDepthFunc(int func) {
        GL11.glDepthFunc(depthFuncToGL[func]);
	}

	@Override
	public void setViewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
	}

	@Override
	public void setShadeModel(int model) {
		GL11.glShadeModel(shadeModelToGL[model]);
	}

	@Override
	public void setMaterialEmissiveColor(float[] color) {
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_EMISSION, getDirectBuffer(color));
	}

	@Override
	public void setMaterialAmbientColor(float[] color) {
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_AMBIENT, getDirectBuffer(color));
	}

	@Override
	public void setMaterialDiffuseColor(float[] color) {
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_DIFFUSE, getDirectBuffer(color));
	}

	@Override
	public void setMaterialSpecularColor(float[] color) {
        GL11.glMaterial(GL11.GL_FRONT, GL11.GL_SPECULAR, getDirectBuffer(color));
	}

	@Override
	public void endModelViewMatrixUpdate() {
		// Nothing to do
	}

	@Override
	public void setLightModelAmbientColor(float[] color) {
        GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, getDirectBuffer(color));
	}

	@Override
	public void setLightMode(int mode) {
        GL11.glLightModeli(GL12.GL_LIGHT_MODEL_COLOR_CONTROL, lightModeToGL[mode]);
	}

	@Override
	public void setLightAmbientColor(int light, float[] color) {
		GL11.glLight(GL11.GL_LIGHT0 + light, GL11.GL_AMBIENT, getDirectBuffer(color));
	}

	@Override
	public void setLightDiffuseColor(int light, float[] color) {
		GL11.glLight(GL11.GL_LIGHT0 + light, GL11.GL_DIFFUSE, getDirectBuffer(color));
	}

	@Override
	public void setLightSpecularColor(int light, float[] color) {
		GL11.glLight(GL11.GL_LIGHT0 + light, GL11.GL_SPECULAR, getDirectBuffer(color));
	}

	@Override
	public void setLightConstantAttenuation(int light, float constant) {
        GL11.glLightf(GL11.GL_LIGHT0 + light, GL11.GL_CONSTANT_ATTENUATION, constant);
	}

	@Override
	public void setLightLinearAttenuation(int light, float linear) {
        GL11.glLightf(GL11.GL_LIGHT0 + light, GL11.GL_LINEAR_ATTENUATION, linear);
	}

	@Override
	public void setLightQuadraticAttenuation(int light, float quadratic) {
        GL11.glLightf(GL11.GL_LIGHT0 + light, GL11.GL_QUADRATIC_ATTENUATION, quadratic);
	}

	@Override
	public void setLightDirection(int light, float[] direction) {
        GL11.glLight(GL11.GL_LIGHT0 + light, GL11.GL_SPOT_DIRECTION, getDirectBuffer(direction));
	}

	@Override
	public void setLightPosition(int light, float[] position) {
        GL11.glLight(GL11.GL_LIGHT0 + light, GL11.GL_POSITION, getDirectBuffer(position));
	}

	@Override
	public void setLightSpotCutoff(int light, float cutoff) {
        GL11.glLightf(GL11.GL_LIGHT0 + light, GL11.GL_SPOT_CUTOFF, cutoff);
	}

	@Override
	public void setLightSpotExponent(int light, float exponent) {
        GL11.glLightf(GL11.GL_LIGHT0 + light, GL11.GL_SPOT_EXPONENT, exponent);
	}

	@Override
	public void setLightType(int light, int type, int kind) {
		// Nothing to do
	}

	@Override
	public void setBlendColor(float[] color) {
		try {
			GL14.glBlendColor(color[0], color[1], color[2], color[3]);
		} catch (IllegalStateException e) {
			log.warn("VideoEngine: " + e.getMessage());
		}
	}

	@Override
	public void setBlendFunc(int src, int dst) {
		try {
			GL11.glBlendFunc(blendSrcToGL[src], blendDstToGL[dst]);
		} catch (IllegalStateException e) {
			log.warn("VideoEngine: " + e.getMessage());
		}
	}

	@Override
	public void setLogicOp(int logicOp) {
        GL11.glLogicOp(logicOpToGL[logicOp]);
	}

	@Override
	public void setColorMask(boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled) {
        GL11.glColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, alphaWriteEnabled);
	}

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
    	// Not supported, nothing to do
    }

	@Override
	public void setDepthMask(boolean depthWriteEnabled) {
		GL11.glDepthMask(depthWriteEnabled);
	}

	@Override
	public void setTextureWrapMode(int s, int t) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapModeToGL[s]);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrapModeToGL[t]);
	}

	@Override
	public void setTextureMipmapMinLevel(int level) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, level);
	}

	@Override
	public void setTextureMipmapMaxLevel(int level) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, level);
	}

	@Override
	public void setTextureMipmapMinFilter(int filter) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mipmapFilterToGL[filter]);
	}

	@Override
	public void setTextureMipmapMagFilter(int filter) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mipmapFilterToGL[filter]);
	}

	@Override
	public void setColorMaterial(boolean ambient, boolean diffuse, boolean specular) {
		int index = (ambient ? 1 : 0) | (diffuse ? 2 : 0) | (specular ? 4 : 0);
        GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, colorMaterialToGL[index]);
	}

	@Override
	public void setTextureEnvironmentMapping(int u, int v) {
        GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_SPHERE_MAP);
        GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_SPHERE_MAP);
	}

	@Override
	public void setVertexColor(float[] color) {
        GL11.glColor4f(color[0], color[1], color[2], color[3]);
	}

	@Override
	public void setUniform(int id, int value) {
        GL20.glUniform1i(id, value);
	}

	@Override
	public void setUniform(int id, int value1, int value2) {
        GL20.glUniform2i(id, value1, value2);
	}

	@Override
	public void setUniform(int id, float value) {
		GL20.glUniform1f(id, value);
	}

	@Override
	public void setUniform2(int id, int[] values) {
		GL20.glUniform2i(id, values[0], values[1]);
	}

	@Override
	public void setUniform3(int id, int[] values) {
        GL20.glUniform3i(id, values[0], values[1], values[2]);
	}

	@Override
	public void setUniform4(int id, int[] values) {
        GL20.glUniform4i(id, values[0], values[1], values[2], values[3]);
	}

	@Override
	public void setUniformMatrix4(int id, int count, float[] values) {
		GL20.glUniformMatrix4(id, false, getDirectBuffer(values, count * 16));
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
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, texEnvNameToGL[name], texEnvParamToGL[param]);
	}

	@Override
	public void setTexEnv(int name, float param) {
		GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, texEnvNameToGL[name], param);
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
		GL20.glAttachShader(program, shader);
	}

	@Override
	public boolean compilerShader(int shader, String source) {
		GL20.glShaderSource(shader, source);
		GL20.glCompileShader(shader);
		return GL20.glGetShader(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_TRUE;
	}

	@Override
	public int createProgram() {
		return GL20.glCreateProgram();
	}

	@Override
	public void useProgram(int program) {
		GL20.glUseProgram(program);
	}

	@Override
	public int createShader(int type) {
		return GL20.glCreateShader(shaderTypeToGL[type]);
	}

	@Override
	public int getAttribLocation(int program, String name) {
		return GL20.glGetAttribLocation(program, name);
	}

	@Override
	public int getUniformLocation(int program, String name) {
		return GL20.glGetUniformLocation(program, name);
	}

	@Override
	public void linkProgram(int program) {
		GL20.glLinkProgram(program);
	}

	@Override
	public void validateProgram(int program) {
		GL20.glValidateProgram(program);
	}

	@Override
	public String getProgramInfoLog(int program) {
		int infoLogLength = GL20.glGetProgram(program, GL20.GL_INFO_LOG_LENGTH);

		if (infoLogLength <= 1) {
			return null;
		}

        String infoLog = GL20.glGetProgramInfoLog(program, infoLogLength);

        // Remove ending '\0' byte(s)
        while (infoLog.length() > 0 && infoLog.charAt(infoLog.length() - 1) == '\0') {
        	infoLog = infoLog.substring(0, infoLog.length() - 1);
        }

        return infoLog;
	}

	@Override
	public String getShaderInfoLog(int shader) {
		int infoLogLength = GL20.glGetShader(shader, GL20.GL_INFO_LOG_LENGTH);
		if (infoLogLength <= 1) {
			return null;
		}

        String infoLog = GL20.glGetShaderInfoLog(shader, infoLogLength);

        // Remove ending '\0' byte(s)
        while (infoLog.length() > 0 && infoLog.charAt(infoLog.length() - 1) == '\0') {
        	infoLog = infoLog.substring(0, infoLog.length() - 1);
        }

        return infoLog;
	}

	@Override
	public boolean isExtensionAvailable(String name) {
		String extensions = GL11.glGetString(GL11.GL_EXTENSIONS);
		if (extensions == null) {
			return false;
		}

		// Extensions are space separated
		return (" " + extensions + " ").indexOf(" " + name + " ") >= 0;
	}

	@Override
	public void drawArrays(int primitive, int first, int count) {
		GL11.glDrawArrays(primitiveToGL[primitive], first, count);
	}

	@Override
	public void deleteBuffer(int buffer) {
		ARBBufferObject.glDeleteBuffersARB(buffer);
	}

	@Override
	public int genBuffer() {
		return ARBBufferObject.glGenBuffersARB();
	}

	@Override
	public void setBufferData(int target, int size, Buffer buffer, int usage) {
		if (buffer instanceof ByteBuffer) {
			ARBBufferObject.glBufferDataARB(bufferTargetToGL[target], getDirectBuffer(size, (ByteBuffer) buffer), bufferUsageToGL[usage]);
		} else if (buffer instanceof IntBuffer) {
			ARBBufferObject.glBufferDataARB(bufferTargetToGL[target], getDirectBuffer(size, (IntBuffer) buffer), bufferUsageToGL[usage]);
		} else if (buffer instanceof ShortBuffer) {
			ARBBufferObject.glBufferDataARB(bufferTargetToGL[target], getDirectBuffer(size, (ShortBuffer) buffer), bufferUsageToGL[usage]);
		} else if (buffer instanceof FloatBuffer) {
			ARBBufferObject.glBufferDataARB(bufferTargetToGL[target], getDirectBuffer(size, (FloatBuffer) buffer), bufferUsageToGL[usage]);
		} else if (buffer == null) {
			ARBBufferObject.glBufferDataARB(bufferTargetToGL[target], size, bufferUsageToGL[usage]);
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void setBufferSubData(int target, int offset, int size, Buffer buffer) {
		if (buffer instanceof ByteBuffer) {
			ARBBufferObject.glBufferSubDataARB(bufferTargetToGL[target], offset, getDirectBuffer(size, (ByteBuffer) buffer));
		} else if (buffer instanceof IntBuffer) {
			ARBBufferObject.glBufferSubDataARB(bufferTargetToGL[target], offset, getDirectBuffer(size, (IntBuffer) buffer));
		} else if (buffer instanceof ShortBuffer) {
			ARBBufferObject.glBufferSubDataARB(bufferTargetToGL[target], offset, getDirectBuffer(size, (ShortBuffer) buffer));
		} else if (buffer instanceof FloatBuffer) {
			ARBBufferObject.glBufferSubDataARB(bufferTargetToGL[target], offset, getDirectBuffer(size, (FloatBuffer) buffer));
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void enableClientState(int type) {
		GL11.glEnableClientState(clientStateToGL[type]);
	}

	@Override
	public void enableVertexAttribArray(int id) {
		GL20.glEnableVertexAttribArray(id);
	}

	@Override
	public void disableClientState(int type) {
		GL11.glDisableClientState(clientStateToGL[type]);
	}

	@Override
	public void disableVertexAttribArray(int id) {
		GL20.glDisableVertexAttribArray(id);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, long offset) {
		GL11.glColorPointer(size, pointerTypeToGL[type], stride, offset);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		switch (type) {
			case RE_FLOAT:
				GL11.glColorPointer(size, stride, getDirectFloatBuffer(bufferSize, buffer));
				break;
			case RE_BYTE:
				GL11.glColorPointer(size, false, stride, getDirectByteBuffer(bufferSize, buffer));
				break;
			case RE_UNSIGNED_BYTE:
				GL11.glColorPointer(size, true, stride, getDirectByteBuffer(bufferSize, buffer));
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void setNormalPointer(int type, int stride, long offset) {
		GL11.glNormalPointer(pointerTypeToGL[type], stride, offset);
	}

	@Override
	public void setNormalPointer(int type, int stride, int bufferSize, Buffer buffer) {
		switch (type) {
			case RE_FLOAT:
				GL11.glNormalPointer(stride, getDirectFloatBuffer(bufferSize, buffer));
				break;
			case RE_BYTE:
				GL11.glNormalPointer(stride, getDirectByteBuffer(bufferSize, buffer));
				break;
			case RE_INT:
				GL11.glNormalPointer(stride, getDirectIntBuffer(bufferSize, buffer));
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, long offset) {
		GL11.glTexCoordPointer(size, pointerTypeToGL[type], stride, offset);
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		switch (type) {
			case RE_FLOAT:
				GL11.glTexCoordPointer(size, stride, getDirectFloatBuffer(bufferSize, buffer));
				break;
			case RE_SHORT:
				GL11.glTexCoordPointer(size, stride, getDirectShortBuffer(bufferSize, buffer));
				break;
			case RE_INT:
				GL11.glTexCoordPointer(size, stride, getDirectIntBuffer(bufferSize, buffer));
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, long offset) {
		GL11.glVertexPointer(size, pointerTypeToGL[type], stride, offset);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		switch (type) {
			case RE_FLOAT:
				GL11.glVertexPointer(size, stride, getDirectFloatBuffer(bufferSize, buffer));
				break;
			case RE_SHORT:
				GL11.glVertexPointer(size, stride, getDirectShortBuffer(bufferSize, buffer));
				break;
			case RE_INT:
				GL11.glVertexPointer(size, stride, getDirectIntBuffer(bufferSize, buffer));
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, long offset) {
		GL20.glVertexAttribPointer(id, size, pointerTypeToGL[type], normalized, stride, offset);
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type, boolean normalized, int stride, int bufferSize, Buffer buffer) {
		switch (type) {
			case RE_FLOAT:
				GL20.glVertexAttribPointer(id, size, false, stride, getDirectFloatBuffer(bufferSize, buffer));
				break;
			case RE_SHORT:
				GL20.glVertexAttribPointer(id, size, false, false, stride, getDirectShortBuffer(bufferSize, buffer));
				break;
			case RE_UNSIGNED_SHORT:
				GL20.glVertexAttribPointer(id, size, true, false, stride, getDirectShortBuffer(bufferSize, buffer));
				break;
			case RE_INT:
				GL20.glVertexAttribPointer(id, size, false, false, stride, getDirectIntBuffer(bufferSize, buffer));
				break;
			case RE_UNSIGNED_INT:
				GL20.glVertexAttribPointer(id, size, true, false, stride, getDirectIntBuffer(bufferSize, buffer));
				break;
			case RE_BYTE:
				GL20.glVertexAttribPointer(id, size, false, false, stride, getDirectByteBuffer(bufferSize, buffer));
				break;
			case RE_UNSIGNED_BYTE:
				GL20.glVertexAttribPointer(id, size, true, false, stride, getDirectByteBuffer(bufferSize, buffer));
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void setPixelStore(int rowLength, int alignment) {
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
        GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, rowLength);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, alignment);
	}

	@Override
	public int genTexture() {
		return GL11.glGenTextures();
	}

	@Override
	public void bindTexture(int texture) {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
	}

	@Override
	public void deleteTexture(int texture) {
		GL11.glDeleteTextures(texture);
	}

	@Override
	public void setCompressedTexImage(int level, int internalFormat, int width, int height, int compressedSize, Buffer buffer) {
		GL13.glCompressedTexImage2D(GL11.GL_TEXTURE_2D, level, textureFormatToGL[internalFormat], width, height, 0, getDirectByteBuffer(compressedSize, buffer));
	}

	@Override
	public void setTexImage(int level, int internalFormat, int width, int height, int format, int type, int textureSize, Buffer buffer) {
		if (buffer instanceof ByteBuffer || buffer == null) {
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, textureFormatToGL[internalFormat], width, height, 0, textureFormatToGL[format], textureTypeToGL[type], getDirectBuffer(textureSize, (ByteBuffer) buffer));
		} else if (buffer instanceof IntBuffer) {
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, textureFormatToGL[internalFormat], width, height, 0, textureFormatToGL[format], textureTypeToGL[type], getDirectBuffer(textureSize, (IntBuffer) buffer));
		} else if (buffer instanceof ShortBuffer) {
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, textureFormatToGL[internalFormat], width, height, 0, textureFormatToGL[format], textureTypeToGL[type], getDirectBuffer(textureSize, (ShortBuffer) buffer));
		} else if (buffer instanceof FloatBuffer) {
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, level, textureFormatToGL[internalFormat], width, height, 0, textureFormatToGL[format], textureTypeToGL[type], getDirectBuffer(textureSize, (FloatBuffer) buffer));
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void setTexSubImage(int level, int xOffset, int yOoffset, int width, int height, int format, int type, int textureSize, Buffer buffer) {
		if (buffer instanceof ByteBuffer || buffer == null) {
			GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, xOffset, yOoffset, width, height, textureFormatToGL[format], textureTypeToGL[type], getDirectBuffer(textureSize, (ByteBuffer) buffer));
		} else if (buffer instanceof IntBuffer) {
			GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, xOffset, yOoffset, width, height, textureFormatToGL[format], textureTypeToGL[type], getDirectBuffer(textureSize, (IntBuffer) buffer));
		} else if (buffer instanceof ShortBuffer) {
			GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, xOffset, yOoffset, width, height, textureFormatToGL[format], textureTypeToGL[type], getDirectBuffer(textureSize, (ShortBuffer) buffer));
		} else if (buffer instanceof FloatBuffer) {
			GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, level, xOffset, yOoffset, width, height, textureFormatToGL[format], textureTypeToGL[type], getDirectBuffer(textureSize, (FloatBuffer) buffer));
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void setStencilOp(int fail, int zfail, int zpass) {
		GL11.glStencilOp(stencilOpToGL[fail], stencilOpToGL[zfail], stencilOpToGL[zpass]);
	}

	@Override
	public void setStencilFunc(int func, int ref, int mask) {
		GL11.glStencilFunc(stencilFuncToGL[func], ref, mask);
	}

	@Override
	public void setAlphaFunc(int func, int ref) {
		GL11.glAlphaFunc(alphaFuncToGL[func], ref / 255.0f);
	}

	@Override
	public void setFogColor(float[] color) {
		GL11.glFog(GL11.GL_FOG_COLOR, getDirectBuffer(color));
	}

	@Override
	public void setFogDist(float start, float end) {
        GL11.glFogf(GL11.GL_FOG_START, start);
        GL11.glFogf(GL11.GL_FOG_END, end);
	}

	@Override
	public void setTextureEnvColor(float[] color) {
        GL11.glTexEnv(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_COLOR, getDirectBuffer(color));
	}

	@Override
	public void setFrontFace(boolean cw) {
		GL11.glFrontFace(cw ? GL11.GL_CW : GL11.GL_CCW);
	}

	@Override
	public void setScissor(int x, int y, int width, int height) {
		GL11.glScissor(x, y, width, height);
	}

	@Override
	public void setBlendEquation(int mode) {
		try {
			GL14.glBlendEquation(blendModeToGL[mode]);
		} catch (IllegalStateException e) {
			log.warn("VideoEngine: " + e.getMessage());
		}
	}

	@Override
	public void setFogHint() {
        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
        GL11.glHint(GL11.GL_FOG_HINT, GL11.GL_DONT_CARE);
	}

	@Override
	public void setLineSmoothHint() {
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
	}

	@Override
	public void setMaterialShininess(float shininess) {
        GL11.glMaterialf(GL11.GL_FRONT, GL11.GL_SHININESS, shininess);
	}

	@Override
	public void beginDraw(int primitive) {
		GL11.glBegin(primitiveToGL[primitive]);
	}

	@Override
	public void beginQuery(int id) {
		GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, id);
	}

	@Override
	public void drawVertex3(float[] values) {
		GL11.glVertex3f(values[0], values[1], values[2]);
	}

	@Override
	public void endDraw() {
		GL11.glEnd();
	}

	@Override
	public void endQuery() {
		GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);
	}

	@Override
	public void drawColor(float value1, float value2, float value3) {
		GL11.glColor3f(value1, value2, value3);
	}

	@Override
	public void drawColor(float value1, float value2, float value3, float value4) {
		GL11.glColor4f(value1, value2, value3, value4);
	}

	@Override
	public void drawTexCoord(float value1, float value2) {
		GL11.glTexCoord2f(value1, value2);
	}

	@Override
	public void drawVertex(int value1, int value2) {
		GL11.glVertex2i(value1, value2);
	}

	@Override
	public void drawVertex(float value1, float value2) {
		GL11.glVertex2f(value1, value2);
	}

	@Override
	public int genQuery() {
		return GL15.glGenQueries();
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
	public void beginBoundingBox(int numberOfVertexBoundingBox) {
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
        // 0 means result not yet available, 1 means result available
        return GL15.glGetQueryObjecti(id, GL15.GL_QUERY_RESULT_AVAILABLE) != 0;
	}

	@Override
	public int getQueryResult(int id) {
        return GL15.glGetQueryObjecti(id, GL15.GL_QUERY_RESULT);
	}

	@Override
	public void copyTexSubImage(int level, int xOffset, int yOffset, int x, int y, int width, int height) {
		GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, level, xOffset, yOffset, x, y, width, height);
	}

	@Override
	public void getTexImage(int level, int format, int type, Buffer buffer) {
		if (buffer instanceof ByteBuffer) {
			ByteBuffer directBuffer = allocateDirectBuffer((ByteBuffer) buffer);
			GL11.glGetTexImage(GL11.GL_TEXTURE_2D, level, textureFormatToGL[format], textureTypeToGL[type], (ByteBuffer) buffer);
			copyBuffer((ByteBuffer) buffer, directBuffer);
		} else if (buffer instanceof IntBuffer) {
			IntBuffer directBuffer = allocateDirectBuffer((IntBuffer) buffer);
			GL11.glGetTexImage(GL11.GL_TEXTURE_2D, level, textureFormatToGL[format], textureTypeToGL[type], directBuffer);
			copyBuffer((IntBuffer) buffer, directBuffer);
		} else if (buffer instanceof ShortBuffer) {
			ShortBuffer directBuffer = allocateDirectBuffer((ShortBuffer) buffer);
			GL11.glGetTexImage(GL11.GL_TEXTURE_2D, level, textureFormatToGL[format], textureTypeToGL[type], (ShortBuffer) buffer);
			copyBuffer((ShortBuffer) buffer, directBuffer);
		} else if (buffer instanceof FloatBuffer) {
			FloatBuffer directBuffer = allocateDirectBuffer((FloatBuffer) buffer);
			GL11.glGetTexImage(GL11.GL_TEXTURE_2D, level, textureFormatToGL[format], textureTypeToGL[type], (FloatBuffer) buffer);
			copyBuffer((FloatBuffer) buffer, directBuffer);
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void readPixels(int x, int y, int width, int height, int format,	int type, Buffer buffer) {
		if (buffer instanceof ByteBuffer) {
			ByteBuffer directBuffer = allocateDirectBuffer((ByteBuffer) buffer);
			GL11.glReadPixels(x, y, width, height, textureFormatToGL[format], textureTypeToGL[type], (ByteBuffer) buffer);
			copyBuffer((ByteBuffer) buffer, directBuffer);
		} else if (buffer instanceof IntBuffer) {
			IntBuffer directBuffer = allocateDirectBuffer((IntBuffer) buffer);
			GL11.glReadPixels(x, y, width, height, textureFormatToGL[format], textureTypeToGL[type], (IntBuffer) buffer);
			copyBuffer((IntBuffer) buffer, directBuffer);
		} else if (buffer instanceof ShortBuffer) {
			ShortBuffer directBuffer = allocateDirectBuffer((ShortBuffer) buffer);
			GL11.glReadPixels(x, y, width, height, textureFormatToGL[format], textureTypeToGL[type], (ShortBuffer) buffer);
			copyBuffer((ShortBuffer) buffer, directBuffer);
		} else if (buffer instanceof FloatBuffer) {
			FloatBuffer directBuffer = allocateDirectBuffer((FloatBuffer) buffer);
			GL11.glReadPixels(x, y, width, height, textureFormatToGL[format], textureTypeToGL[type], (FloatBuffer) buffer);
			copyBuffer((FloatBuffer) buffer, directBuffer);
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void clear(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, long offset) {
		// Nothing to do
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		// Nothing to do
	}

	@Override
	public IREBufferManager getBufferManager() {
		// Nothing to do
		return null;
	}

	@Override
	public boolean canAllNativeVertexInfo() {
		return false;
	}

	@Override
	public boolean canNativeSpritesPrimitive() {
		return false;
	}

	@Override
	public void setVertexInfo(VertexInfo vinfo, boolean allNativeVertexInfo, boolean useVertexColor) {
		// Nothing to do
	}

	@Override
	public void setProgramParameter(int program, int parameter, int value) {
		if (parameter == RE_GEOMETRY_INPUT_TYPE || parameter == RE_GEOMETRY_OUTPUT_TYPE) {
			value = primitiveToGL[value];
		}
		EXTGeometryShader4.glProgramParameteriEXT(program, programParameterToGL[parameter], value);
	}

	@Override
	public boolean isQueryAvailable() {
		// glGenQueries is available only if the GL version is 1.5 or greater
		return GLContext.getCapabilities().OpenGL15;
	}

	@Override
	public boolean isShaderAvailable() {
		// glCreateShader is available only if the GL version is 2.0 or greater
		return GLContext.getCapabilities().OpenGL20;
	}

	@Override
	public void bindBuffer(int target, int buffer) {
		ARBBufferObject.glBindBufferARB(bufferTargetToGL[target], buffer);
	}

	@Override
	public void bindBufferBase(int target, int bindingPoint, int buffer) {
		ARBUniformBufferObject.glBindBufferBase(bufferTargetToGL[target], bindingPoint, buffer);
	}

	@Override
	public int getUniformBlockIndex(int program, String name) {
		return ARBUniformBufferObject.glGetUniformBlockIndex(program, name);
	}

	@Override
	public void setUniformBlockBinding(int program, int blockIndex, int bindingPoint) {
		ARBUniformBufferObject.glUniformBlockBinding(program, blockIndex, bindingPoint);
	}

	@Override
	public void setMatrix(float[] values) {
        if (values != null) {
        	GL11.glLoadMatrix(getDirectBuffer(values));
        } else {
        	GL11.glLoadIdentity();
        }
	}

	@Override
	public void setMatrixMode(int type) {
        GL11.glMatrixMode(matrixModeToGL[type]);
	}

	@Override
	public void multMatrix(float[] values) {
		if (values != null) {
			GL11.glMultMatrix(getDirectBuffer(values));
		}
	}
}
