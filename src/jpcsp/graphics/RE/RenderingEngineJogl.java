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

import javax.media.opengl.GL;
import javax.media.opengl.GLException;

import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 * A RenderingEngine implementing calls to OpenGL using jogl.
 * The class contains no rendering logic, it just implements the interface to jogl.
 */
public class RenderingEngineJogl extends BaseRenderingEngine {
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

	protected GL gl;

	public RenderingEngineJogl(GL gl) {
		this.gl = gl;
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
	public void setProjectionMatrixElements(float[] values) {
        gl.glMatrixMode(GL.GL_PROJECTION);
        if (values != null) {
        	gl.glLoadMatrixf(values, 0);
        } else {
        	gl.glLoadIdentity();
        }
	}

	@Override
	public void setViewMatrixElements(float[] values) {
        gl.glMatrixMode(GL.GL_MODELVIEW);
        if (values != null) {
        	gl.glLoadMatrixf(values, 0);
        } else {
        	gl.glLoadIdentity();
        }
	}

	@Override
	public void setModelMatrixElements(float[] values) {
        gl.glMatrixMode(GL.GL_MODELVIEW);
        if (values != null) {
        	gl.glMultMatrixf(values, 0);
        }
	}

	@Override
	public void setTextureMatrixElements(float[] values) {
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

    private static boolean getBooleanColorMask(String name, int bitMask) {
        if (bitMask == 0x00) {
            return true;
        } else if (bitMask == 0xFF) {
            return false;
        } else {
            VideoEngine.log.warn(String.format("Unimplemented %s 0x%02X", name, bitMask));
            return true;
        }
    }

    @Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
        boolean redWriteEnabled   = getBooleanColorMask("Red color mask", redMask);
        boolean greenWriteEnabled = getBooleanColorMask("Green color mask", greenMask);
        boolean blueWriteEnabled  = getBooleanColorMask("Blue color mask", blueMask);
        boolean alphaWriteEnabled = getBooleanColorMask("Alpha mask", alphaMask);

        gl.glColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, alphaWriteEnabled);
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
	public void setBones(int count, float[] values) {
		// Nothing to do
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
}
