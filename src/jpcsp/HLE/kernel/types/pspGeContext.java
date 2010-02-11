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
package jpcsp.HLE.kernel.types;

import javax.media.opengl.GL;

import jpcsp.HLE.Modules;
import static jpcsp.graphics.VideoEngine.NUM_LIGHTS;;

public class pspGeContext extends pspAbstractMemoryMappedStructure {
	//
	// According to the pspsdk, the size of the pspGeContext structure
	// is 512 unsigned int's (2048 bytes).
	//
	// Unfortunately, this leaves us not enough space to store all the parameters
	// we have in the VideoEngine. We would require 2236 bytes, which is 188 bytes too much.
	// I have sacrificed the "bone_uploaded_matrix" as this is the largest
	// structure and I've never seen an application using it...
	// This element is not saved when "fullVersion = false"
	//
	public final static boolean fullVersion = false;

	// Almost all VideoEngine class variables...
	public int base, baseOffset;

	public int fbp, fbw;
    public int zbp, zbw;
    public int psm;

    public int region_x1, region_y1, region_x2, region_y2;
    public int region_width, region_height;
    public int scissor_x1, scissor_y1, scissor_x2, scissor_y2;
    public int scissor_width, scissor_height;
    public int offset_x, offset_y;
    public int viewport_width, viewport_height;
    public int viewport_cx, viewport_cy;

    public float[] proj_uploaded_matrix = new float[4 * 4];
    public float[] texture_uploaded_matrix = new float[4 * 4];
    public float[] model_uploaded_matrix = new float[4 * 4];
    public float[] view_uploaded_matrix = new float[4 * 4];
    public float[][] bone_uploaded_matrix = new float[8][4 * 3];
    public float[] morph_weight = new float[8];
    public float[] tex_envmap_matrix = new float[4*4];
    public float[][] light_pos = new float[NUM_LIGHTS][4];
    public float[][] light_dir = new float[NUM_LIGHTS][3];

    public int[] light_enabled = new int[NUM_LIGHTS];
    public int[] light_type = new int[NUM_LIGHTS];
    public int[] light_kind = new int[NUM_LIGHTS];
    public boolean lighting;
    public float[] spotLightExponent = new float[NUM_LIGHTS];
    public float[] spotLightCutoff = new float[NUM_LIGHTS];

    public float[] fog_color = new float[4];
    public float fog_far, fog_dist;

    public float nearZ, farZ, zscale, zpos;

    public int mat_flags;
    public float[] mat_ambient = new float[4];
    public float[] mat_diffuse = new float[4];
    public float[] mat_specular = new float[4];
    public float[] mat_emissive = new float[4];

    public float[] ambient_light = new float[4];

    public int texture_storage, texture_num_mip_maps;
    public boolean texture_swizzle;

    public int[] texture_base_pointer = new int[8];
    public int[] texture_width = new int[8];
    public int[] texture_height = new int[8];
    public int[] texture_buffer_width = new int[8];
    public int tex_min_filter;
    public int tex_mag_filter;

    public float tex_translate_x, tex_translate_y;
    public float tex_scale_x, tex_scale_y;
    public float[] tex_env_color = new float[4];
    public int tex_enable;

    public int tex_clut_addr;
    public int tex_clut_num_blocks;
    public int tex_clut_mode, tex_clut_shift, tex_clut_mask, tex_clut_start;
    public int tex_wrap_s, tex_wrap_t;
    public int patch_div_s;
    public int patch_div_t;

    public int transform_mode;

    public int textureTx_sourceAddress;
    public int textureTx_sourceLineWidth;
    public int textureTx_destinationAddress;
    public int textureTx_destinationLineWidth;
    public int textureTx_width;
    public int textureTx_height;
    public int textureTx_sx;
    public int textureTx_sy;
    public int textureTx_dx;
    public int textureTx_dy;
    public int textureTx_pixelSize;

    public float[] dfix_color = new float[4];
    public float[] sfix_color = new float[4];
    public int blend_src;
    public int blend_dst;

    public boolean clearMode;
    public int depthFuncClearMode;

    public int depthFunc2D;
    public int depthFunc3D;

    public int tex_map_mode;
    public int tex_proj_map_mode;

	public boolean[] glColorMask = new boolean[4];

    // OpenGL flags (enable/disable)
	private boolean glTexture2D;
	private boolean glLight0;
	private boolean glLight1;
	private boolean glLight2;
	private boolean glLight3;
	private boolean glLighting;
	private boolean glDither;
	private boolean glCullFace;
	private boolean glFog;
	private boolean glBlend;
	private boolean glAlphaTest;
	private boolean glDepthTest;
	private boolean glStencilTest;
	private boolean glLineSmooth;
	private boolean glColorLogicOp;
	private boolean glScissorTest;
	private boolean glTextureGenS;
	private boolean glTextureGenT;
	private boolean glColorMaterial;

	// OpenGL Light parameters
	private float[][] glLightAmbient = new float[4][4];
	private float[][] glLightDiffuse = new float[4][4];
	private float[][] glLightSpecular = new float[4][4];
	private float[][] glLightConstantAttenuation = new float[4][1];
	private float[][] glLightLinearAttenuation = new float[4][1];
	private float[][] glLightQuadraticAttenuation = new float[4][1];

	// OpenGL Light Model parameters
	private float[] glLightModelAmbient = new float[4];

	// OpenGL Material parameters
	// First dimension is for GL_FRONT and GL_BACK
	private float[][] glMaterialAmbient = new float[2][4];
	private float[][] glMaterialDiffuse = new float[2][4];
	private float[][] glMaterialSpecular = new float[2][4];
	private float[][] glMaterialEmission = new float[2][4];
	private float[][] glMaterialShininess = new float[2][1];
	private float[][] glMaterialColorIndexes = new float[2][3];

	// OpenGL TexEnv parameters
	private float[] glTexEnvRGBScale = new float[1];
	private int[] glTexEnvMode = new int[1];
	private int[] glTexEnvSrc0Alpha = new int[1];
	private float[] glTexEnvColor = new float[4];

	// OpenGL Fog parameters
	private int[] glFogMode = new int[1];
	private float[] glFogDensity = new float[1];
	private int[] glFogHint = new int[1];
	private float[] glFogColor = new float[4];
	private float[] glFogStart = new float[1];
	private float[] glFogEnd = new float[1];

	// Other OpenGL parameters
	private int[] glBlendEquation = new int[1];
	private int[] glShadeModel = new int[1];
	private int[] glFrontFace = new int[1];
	private int[] glLineSmoothHint = new int[1];
	private byte[] glDepthWriteMask = new byte[1];
	private int[] glAlphaTestFunc = new int[1];
	private float[] glAlphaTestRef = new float[1];
	private int[] glStencilFunc = new int[1];
	private int[] glStencilRef = new int[1];
	private int[] glStencilValueMask = new int[1];
	private int[] glScissorBox = new int[4];
	private float[] glDepthRange = new float[2];
	private int[] glStencilFail = new int[1];
	private int[] glStencilPassDepthPass = new int[1];
	private int[] glStencilPassDepthFail = new int[1];

	@Override
	protected void read() {
		base = read32();
		baseOffset = read32();

		fbp = read32();
		fbw = read32();
		zbp = read32();
		zbw = read32();
		psm = read32();

		region_x1 = read32();
		region_y1 = read32();
		region_x2 = read32();
		region_y2 = read32();

		region_width = read32();
		region_height = read32();
		scissor_x1 = read32();
		scissor_y1 = read32();
		scissor_x2 = read32();
		scissor_y2 = read32();
		scissor_width = read32();
		scissor_height = read32();
		offset_x = read32();
		offset_y = read32();
		viewport_width = read32();
		viewport_height = read32();
		viewport_cx = read32();
		viewport_cy = read32();

		readFloatArray(proj_uploaded_matrix);
		readFloatArray(texture_uploaded_matrix);
		readFloatArray(model_uploaded_matrix);
		readFloatArray(view_uploaded_matrix);
		if (fullVersion) {
			readFloatArray(bone_uploaded_matrix);
		}
		readFloatArray(morph_weight);
		readFloatArray(tex_envmap_matrix);
		readFloatArray(light_pos);
		readFloatArray(light_dir);

		read32Array(light_enabled);
		read32Array(light_type);
		read32Array(light_kind);
		lighting = readBoolean();
		readFloatArray(spotLightExponent);
		readFloatArray(spotLightCutoff);

		readFloatArray(fog_color);
		fog_far = readFloat();
		fog_dist = readFloat();

		nearZ = readFloat();
		farZ = readFloat();
		zscale = readFloat();
		zpos = readFloat();

		mat_flags = read32();
		readFloatArray(mat_ambient);
		readFloatArray(mat_diffuse);
		readFloatArray(mat_specular);
		readFloatArray(mat_emissive);

		readFloatArray(ambient_light);

		texture_storage = read32();
		texture_num_mip_maps = read32();
		texture_swizzle = readBoolean();

		read32Array(texture_base_pointer);
		read32Array(texture_width);
		read32Array(texture_height);
		read32Array(texture_buffer_width);
		tex_min_filter = read32();
		tex_mag_filter = read32();

		tex_translate_x = readFloat();
		tex_translate_y = readFloat();
		tex_scale_x = readFloat();
		tex_scale_y = readFloat();
		readFloatArray(tex_env_color);
		tex_enable = read32();

		tex_clut_addr = read32();
		tex_clut_num_blocks = read32();
		tex_clut_mode = read32();
		tex_clut_shift = read32();
		tex_clut_mask = read32();
		tex_clut_start = read32();
		tex_wrap_s = read32();
		tex_wrap_t = read32();
		patch_div_s = read32();
		patch_div_t = read32();

		transform_mode = read32();

		textureTx_sourceAddress = read32();
		textureTx_sourceLineWidth = read32();
		textureTx_destinationAddress = read32();
		textureTx_destinationLineWidth = read32();
		textureTx_width = read32();
		textureTx_height = read32();
		textureTx_sx = read32();
		textureTx_sy = read32();
		textureTx_dx = read32();
		textureTx_dy = read32();
		textureTx_pixelSize = read32();

		readFloatArray(dfix_color);
		readFloatArray(sfix_color);
		blend_src = read32();
		blend_dst = read32();

		clearMode = readBoolean();
		depthFuncClearMode = read32();

		depthFunc2D = read32();
		depthFunc3D = read32();

		tex_map_mode = read32();
		tex_proj_map_mode = read32();

		glTexture2D = readBoolean();
		glLight0 = readBoolean();
		glLight1 = readBoolean();
		glLight2 = readBoolean();
		glLight3 = readBoolean();
		glLighting = readBoolean();
		glDither = readBoolean();
		glCullFace = readBoolean();
		glFog = readBoolean();
		glBlend = readBoolean();
		glAlphaTest = readBoolean();
		glDepthTest = readBoolean();
		glStencilTest = readBoolean();
		glLineSmooth = readBoolean();
		glColorLogicOp = readBoolean();
		glScissorTest = readBoolean();
		glTextureGenS = readBoolean();
		glTextureGenT = readBoolean();
		glColorMaterial = readBoolean();

		readFloatArray(glLightAmbient);
		readFloatArray(glLightDiffuse);
		readFloatArray(glLightSpecular);
		readFloatArray(glLightConstantAttenuation);
		readFloatArray(glLightLinearAttenuation);
		readFloatArray(glLightQuadraticAttenuation);

		readFloatArray(glLightModelAmbient);

		readFloatArray(glMaterialAmbient);
		readFloatArray(glMaterialDiffuse);
		readFloatArray(glMaterialSpecular);
		readFloatArray(glMaterialEmission);
		readFloatArray(glMaterialShininess);
		readFloatArray(glMaterialColorIndexes);

		readFloatArray(glTexEnvRGBScale);
		read32Array(glTexEnvMode);
		read32Array(glTexEnvSrc0Alpha);
		readFloatArray(glTexEnvColor);

		read32Array(glFogMode);
		readFloatArray(glFogDensity);
		read32Array(glFogHint);
		readFloatArray(glFogColor);
		readFloatArray(glFogStart);
		readFloatArray(glFogEnd);

		read32Array(glBlendEquation);
		read32Array(glShadeModel);
		read32Array(glFrontFace);
		read32Array(glLineSmoothHint);
		read8Array(glDepthWriteMask);
		read32Array(glAlphaTestFunc);
		readFloatArray(glAlphaTestRef);
		read32Array(glStencilFunc);
		read32Array(glStencilRef);
		read32Array(glStencilValueMask);
		read32Array(glScissorBox);
		readFloatArray(glDepthRange);
		read32Array(glStencilFail);
		read32Array(glStencilPassDepthPass);
		read32Array(glStencilPassDepthFail);
		readBooleanArray(glColorMask);
	}

	@Override
	protected void write() {
		write32(base);
		write32(baseOffset);

		write32(fbp);
		write32(fbw);
		write32(zbp);
		write32(zbw);
		write32(psm);

		write32(region_x1);
		write32(region_y1);
		write32(region_x2);
		write32(region_y2);

		write32(region_width);
		write32(region_height);
		write32(scissor_x1);
		write32(scissor_y1);
		write32(scissor_x2);
		write32(scissor_y2);
		write32(scissor_width);
		write32(scissor_height);
		write32(offset_x);
		write32(offset_y);
		write32(viewport_width);
		write32(viewport_height);
		write32(viewport_cx);
		write32(viewport_cy);

		writeFloatArray(proj_uploaded_matrix);
		writeFloatArray(texture_uploaded_matrix);
		writeFloatArray(model_uploaded_matrix);
		writeFloatArray(view_uploaded_matrix);
		if (fullVersion) {
			writeFloatArray(bone_uploaded_matrix);
		}
		writeFloatArray(morph_weight);
		writeFloatArray(tex_envmap_matrix);
		writeFloatArray(light_pos);
		writeFloatArray(light_dir);

		write32Array(light_enabled);
		write32Array(light_type);
		write32Array(light_kind);
		writeBoolean(lighting);
		writeFloatArray(spotLightExponent);
		writeFloatArray(spotLightCutoff);

		writeFloatArray(fog_color);
		writeFloat(fog_far);
		writeFloat(fog_dist);

		writeFloat(nearZ);
		writeFloat(farZ);
		writeFloat(zscale);
		writeFloat(zpos);

		write32(mat_flags);
		writeFloatArray(mat_ambient);
		writeFloatArray(mat_diffuse);
		writeFloatArray(mat_specular);
		writeFloatArray(mat_emissive);

		writeFloatArray(ambient_light);

		write32(texture_storage);
		write32(texture_num_mip_maps);
		writeBoolean(texture_swizzle);

		write32Array(texture_base_pointer);
		write32Array(texture_width);
		write32Array(texture_height);
		write32Array(texture_buffer_width);
		write32(tex_min_filter);
		write32(tex_mag_filter);

		writeFloat(tex_translate_x);
		writeFloat(tex_translate_y);
		writeFloat(tex_scale_x);
		writeFloat(tex_scale_y);
		writeFloatArray(tex_env_color);
		write32(tex_enable);

		write32(tex_clut_addr);
		write32(tex_clut_num_blocks);
		write32(tex_clut_mode);
		write32(tex_clut_shift);
		write32(tex_clut_mask);
		write32(tex_clut_start);
		write32(tex_wrap_s);
		write32(tex_wrap_t);
		write32(patch_div_s);
		write32(patch_div_t);

		write32(transform_mode);

		write32(textureTx_sourceAddress);
		write32(textureTx_sourceLineWidth);
		write32(textureTx_destinationAddress);
		write32(textureTx_destinationLineWidth);
		write32(textureTx_width);
		write32(textureTx_height);
		write32(textureTx_sx);
		write32(textureTx_sy);
		write32(textureTx_dx);
		write32(textureTx_dy);
		write32(textureTx_pixelSize);

		writeFloatArray(dfix_color);
		writeFloatArray(sfix_color);
		write32(blend_src);
		write32(blend_dst);

		writeBoolean(clearMode);
		write32(depthFuncClearMode);

		write32(depthFunc2D);
		write32(depthFunc3D);

		write32(tex_map_mode);
		write32(tex_proj_map_mode);

		writeBoolean(glTexture2D);
		writeBoolean(glLight0);
		writeBoolean(glLight1);
		writeBoolean(glLight2);
		writeBoolean(glLight3);
		writeBoolean(glLighting);
		writeBoolean(glDither);
		writeBoolean(glCullFace);
		writeBoolean(glFog);
		writeBoolean(glBlend);
		writeBoolean(glAlphaTest);
		writeBoolean(glDepthTest);
		writeBoolean(glStencilTest);
		writeBoolean(glLineSmooth);
		writeBoolean(glColorLogicOp);
		writeBoolean(glScissorTest);
		writeBoolean(glTextureGenS);
		writeBoolean(glTextureGenT);
		writeBoolean(glColorMaterial);

		writeFloatArray(glLightAmbient);
		writeFloatArray(glLightDiffuse);
		writeFloatArray(glLightSpecular);
		writeFloatArray(glLightConstantAttenuation);
		writeFloatArray(glLightLinearAttenuation);
		writeFloatArray(glLightQuadraticAttenuation);

		writeFloatArray(glLightModelAmbient);

		writeFloatArray(glMaterialAmbient);
		writeFloatArray(glMaterialDiffuse);
		writeFloatArray(glMaterialSpecular);
		writeFloatArray(glMaterialEmission);
		writeFloatArray(glMaterialShininess);
		writeFloatArray(glMaterialColorIndexes);

		writeFloatArray(glTexEnvRGBScale);
		write32Array(glTexEnvMode);
		write32Array(glTexEnvSrc0Alpha);
		writeFloatArray(glTexEnvColor);

		write32Array(glFogMode);
		writeFloatArray(glFogDensity);
		write32Array(glFogHint);
		writeFloatArray(glFogColor);
		writeFloatArray(glFogStart);
		writeFloatArray(glFogEnd);

		write32Array(glBlendEquation);
		write32Array(glShadeModel);
		write32Array(glFrontFace);
		write32Array(glLineSmoothHint);
		write8Array(glDepthWriteMask);
		write32Array(glAlphaTestFunc);
		writeFloatArray(glAlphaTestRef);
		write32Array(glStencilFunc);
		write32Array(glStencilRef);
		write32Array(glStencilValueMask);
		write32Array(glScissorBox);
		writeFloatArray(glDepthRange);
		write32Array(glStencilFail);
		write32Array(glStencilPassDepthPass);
		write32Array(glStencilPassDepthFail);
		writeBooleanArray(glColorMask);

		if (getOffset() > sizeof()) {
			Modules.log.error("pspGeContext buffer overflow (" + getOffset() + " bytes)!");
		} else {
			if (Modules.log.isDebugEnabled()) {
				Modules.log.debug("pspGeContext currently using " + getOffset() + " bytes out of " + sizeof());
			}

			// Fill rest with "Unknown" bytes
			writeUnknown(sizeof() - getOffset());
		}
	}

	/*
	 * Some VideoEngine context information are not stored into VideoEngine local
	 * variables but stored directly into OpenGL.
	 * We have to retrieve these information into the pspGeContext as well.
	 */
	public void copyGLToContext(GL gl) {
		glTexture2D = gl.glIsEnabled(GL.GL_TEXTURE_2D);
		glLight0 = gl.glIsEnabled(GL.GL_LIGHT0);
		glLight1 = gl.glIsEnabled(GL.GL_LIGHT1);
		glLight2 = gl.glIsEnabled(GL.GL_LIGHT2);
		glLight3 = gl.glIsEnabled(GL.GL_LIGHT3);
		glLighting = gl.glIsEnabled(GL.GL_LIGHTING);
		glDither = gl.glIsEnabled(GL.GL_DITHER);
		glCullFace = gl.glIsEnabled(GL.GL_CULL_FACE);
		glFog = gl.glIsEnabled(GL.GL_FOG);
		glBlend = gl.glIsEnabled(GL.GL_BLEND);
		glAlphaTest = gl.glIsEnabled(GL.GL_ALPHA_TEST);
		glDepthTest = gl.glIsEnabled(GL.GL_DEPTH_TEST);
		glStencilTest = gl.glIsEnabled(GL.GL_STENCIL_TEST);
		glLineSmooth = gl.glIsEnabled(GL.GL_LINE_SMOOTH);
		glColorLogicOp = gl.glIsEnabled(GL.GL_COLOR_LOGIC_OP);
		glScissorTest = gl.glIsEnabled(GL.GL_SCISSOR_TEST);
		glTextureGenS = gl.glIsEnabled(GL.GL_TEXTURE_GEN_S);
		glTextureGenT = gl.glIsEnabled(GL.GL_TEXTURE_GEN_T);
		glColorMaterial = gl.glIsEnabled(GL.GL_COLOR_MATERIAL);

		for (int light = 0; light < glLightAmbient.length; light++) {
			gl.glGetLightfv(GL.GL_LIGHT0 + light, GL.GL_AMBIENT, glLightAmbient[light], 0);
			gl.glGetLightfv(GL.GL_LIGHT0 + light, GL.GL_DIFFUSE, glLightDiffuse[light], 0);
			gl.glGetLightfv(GL.GL_LIGHT0 + light, GL.GL_SPECULAR, glLightSpecular[light], 0);
			gl.glGetLightfv(GL.GL_LIGHT0 + light, GL.GL_CONSTANT_ATTENUATION, glLightConstantAttenuation[light], 0);
			gl.glGetLightfv(GL.GL_LIGHT0 + light, GL.GL_LINEAR_ATTENUATION, glLightLinearAttenuation[light], 0);
			gl.glGetLightfv(GL.GL_LIGHT0 + light, GL.GL_QUADRATIC_ATTENUATION, glLightQuadraticAttenuation[light], 0);
		}

		gl.glGetFloatv(GL.GL_LIGHT_MODEL_AMBIENT, glLightModelAmbient, 0);

		for (int side = 0; side < glMaterialAmbient.length; side++) {
			int glSide = (side == 0 ? GL.GL_FRONT : GL.GL_BACK);
			gl.glGetMaterialfv(glSide, GL.GL_AMBIENT, glMaterialAmbient[side], 0);
			gl.glGetMaterialfv(glSide, GL.GL_DIFFUSE, glMaterialDiffuse[side], 0);
			gl.glGetMaterialfv(glSide, GL.GL_SPECULAR, glMaterialSpecular[side], 0);
			gl.glGetMaterialfv(glSide, GL.GL_EMISSION, glMaterialEmission[side], 0);
			gl.glGetMaterialfv(glSide, GL.GL_SHININESS, glMaterialShininess[side], 0);
			gl.glGetMaterialfv(glSide, GL.GL_COLOR_INDEXES, glMaterialColorIndexes[side], 0);
		}

		gl.glGetTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, glTexEnvRGBScale, 0);
		gl.glGetTexEnviv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, glTexEnvMode, 0);
		gl.glGetTexEnviv(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, glTexEnvSrc0Alpha, 0);
		gl.glGetTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_COLOR, glTexEnvColor, 0);

		gl.glGetIntegerv(GL.GL_FOG_MODE, glFogMode, 0);
		gl.glGetFloatv(GL.GL_FOG_DENSITY, glFogDensity, 0);
		gl.glGetIntegerv(GL.GL_FOG_HINT, glFogHint, 0);
		gl.glGetFloatv(GL.GL_FOG_COLOR, glFogColor, 0);
		gl.glGetFloatv(GL.GL_FOG_START, glFogStart, 0);
		gl.glGetFloatv(GL.GL_FOG_END, glFogEnd, 0);

		gl.glGetIntegerv(GL.GL_BLEND_EQUATION, glBlendEquation, 0);
		gl.glGetIntegerv(GL.GL_SHADE_MODEL, glShadeModel, 0);
		gl.glGetIntegerv(GL.GL_FRONT_FACE, glFrontFace, 0);
		gl.glGetIntegerv(GL.GL_LINE_SMOOTH_HINT, glLineSmoothHint, 0);
		gl.glGetBooleanv(GL.GL_DEPTH_WRITEMASK, glDepthWriteMask, 0);
		gl.glGetIntegerv(GL.GL_ALPHA_TEST_FUNC, glAlphaTestFunc, 0);
		gl.glGetFloatv(GL.GL_ALPHA_TEST_REF, glAlphaTestRef, 0);
		gl.glGetIntegerv(GL.GL_STENCIL_FUNC, glStencilFunc, 0);
		gl.glGetIntegerv(GL.GL_STENCIL_REF, glStencilRef, 0);
		gl.glGetIntegerv(GL.GL_STENCIL_VALUE_MASK, glStencilValueMask, 0);
		gl.glGetIntegerv(GL.GL_SCISSOR_BOX, glScissorBox, 0);
		gl.glGetFloatv(GL.GL_DEPTH_RANGE, glDepthRange, 0);
		gl.glGetIntegerv(GL.GL_STENCIL_FAIL, glStencilFail, 0);
		gl.glGetIntegerv(GL.GL_STENCIL_PASS_DEPTH_PASS, glStencilPassDepthPass, 0);
		gl.glGetIntegerv(GL.GL_STENCIL_PASS_DEPTH_FAIL, glStencilPassDepthFail, 0);
	}

	private void setGlFlag(GL gl, int parameter, boolean value) {
		if (value) {
			gl.glEnable(parameter);
		} else {
			gl.glDisable(parameter);
		}
	}

	public void copyContextToGL(GL gl) {
		setGlFlag(gl, GL.GL_TEXTURE_2D, glTexture2D);
		setGlFlag(gl, GL.GL_LIGHT0, glLight0);
		setGlFlag(gl, GL.GL_LIGHT1, glLight1);
		setGlFlag(gl, GL.GL_LIGHT2, glLight2);
		setGlFlag(gl, GL.GL_LIGHT3, glLight3);
		setGlFlag(gl, GL.GL_LIGHTING, glLighting);
		setGlFlag(gl, GL.GL_DITHER, glDither);
		setGlFlag(gl, GL.GL_CULL_FACE, glCullFace);
		setGlFlag(gl, GL.GL_FOG, glFog);
		setGlFlag(gl, GL.GL_BLEND, glBlend);
		setGlFlag(gl, GL.GL_ALPHA_TEST, glAlphaTest);
		setGlFlag(gl, GL.GL_DEPTH_TEST, glDepthTest);
		setGlFlag(gl, GL.GL_STENCIL_TEST, glStencilTest);
		setGlFlag(gl, GL.GL_LINE_SMOOTH, glLineSmooth);
		setGlFlag(gl, GL.GL_COLOR_LOGIC_OP, glColorLogicOp);
		setGlFlag(gl, GL.GL_SCISSOR_TEST, glScissorTest);
		setGlFlag(gl, GL.GL_TEXTURE_GEN_S, glTextureGenS);
		setGlFlag(gl, GL.GL_TEXTURE_GEN_T, glTextureGenT);
		setGlFlag(gl, GL.GL_COLOR_MATERIAL, glColorMaterial);

		for (int light = 0; light < glLightAmbient.length; light++) {
			gl.glLightfv(GL.GL_LIGHT0 + light, GL.GL_AMBIENT, glLightAmbient[light], 0);
			gl.glLightfv(GL.GL_LIGHT0 + light, GL.GL_DIFFUSE, glLightDiffuse[light], 0);
			gl.glLightfv(GL.GL_LIGHT0 + light, GL.GL_SPECULAR, glLightSpecular[light], 0);
			gl.glLightfv(GL.GL_LIGHT0 + light, GL.GL_CONSTANT_ATTENUATION, glLightConstantAttenuation[light], 0);
			gl.glLightfv(GL.GL_LIGHT0 + light, GL.GL_LINEAR_ATTENUATION, glLightLinearAttenuation[light], 0);
			gl.glLightfv(GL.GL_LIGHT0 + light, GL.GL_QUADRATIC_ATTENUATION, glLightQuadraticAttenuation[light], 0);
		}

		gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, glLightModelAmbient, 0);

		for (int side = 0; side < glMaterialAmbient.length; side++) {
			int glSide = (side == 0 ? GL.GL_FRONT : GL.GL_BACK);
			gl.glMaterialfv(glSide, GL.GL_AMBIENT, glMaterialAmbient[side], 0);
			gl.glMaterialfv(glSide, GL.GL_DIFFUSE, glMaterialDiffuse[side], 0);
			gl.glMaterialfv(glSide, GL.GL_SPECULAR, glMaterialSpecular[side], 0);
			gl.glMaterialfv(glSide, GL.GL_EMISSION, glMaterialEmission[side], 0);
			gl.glMaterialfv(glSide, GL.GL_SHININESS, glMaterialShininess[side], 0);
			gl.glMaterialfv(glSide, GL.GL_COLOR_INDEXES, glMaterialColorIndexes[side], 0);
		}

		gl.glTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, glTexEnvRGBScale, 0);
		gl.glTexEnviv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, glTexEnvMode, 0);
		gl.glTexEnviv(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, glTexEnvSrc0Alpha, 0);
		gl.glTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_COLOR, glTexEnvColor, 0);

		gl.glFogiv(GL.GL_FOG_MODE, glFogMode, 0);
		gl.glFogfv(GL.GL_FOG_DENSITY, glFogDensity, 0);
		gl.glHint(GL.GL_FOG_HINT, glFogHint[0]);
		gl.glFogfv(GL.GL_FOG_COLOR, glFogColor, 0);
		gl.glFogfv(GL.GL_FOG_START, glFogStart, 0);
		gl.glFogfv(GL.GL_FOG_END, glFogEnd, 0);

		gl.glBlendEquation(glBlendEquation[0]);
		gl.glShadeModel(glShadeModel[0]);
		gl.glFrontFace(glFrontFace[0]);
		gl.glHint(GL.GL_LINE_SMOOTH_HINT, glLineSmoothHint[0]);
		gl.glDepthMask(glDepthWriteMask[0] == 0 ? false : true);
		gl.glAlphaFunc(glAlphaTestFunc[0], glAlphaTestRef[0]);
		gl.glStencilFunc(glStencilFunc[0], glStencilRef[0], glStencilValueMask[0]);
		gl.glScissor(glScissorBox[0], glScissorBox[1], glScissorBox[2], glScissorBox[3]);
		gl.glDepthRange(glDepthRange[0], glDepthRange[1]);
		gl.glStencilOp(glStencilFail[0], glStencilPassDepthFail[0], glStencilPassDepthPass[0]);
	}

	@Override
	public int sizeof() {
		// pspsdk defines the context as an array of 512 unsigned int's
		return 512 * 4;
	}
}
