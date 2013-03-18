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
package jpcsp.graphics;

import static jpcsp.graphics.GeCommands.TBIAS_MODE_AUTO;
import static jpcsp.graphics.GeCommands.TFLT_NEAREST;
import static jpcsp.graphics.GeCommands.TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV;
import static jpcsp.graphics.GeCommands.TMAP_TEXTURE_PROJECTION_MODE_POSITION;
import static jpcsp.graphics.GeCommands.TWRAP_WRAP_MODE_REPEAT;
import static jpcsp.graphics.VideoEngine.NUM_LIGHTS;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure;
import jpcsp.graphics.RE.IRenderingEngine;

/**
 * @author gid15
 *
 */
public class GeContext extends pspAbstractMemoryMappedStructure {
	private final static Logger log = VideoEngine.log;
	// pspsdk defines the context as an array of 512 unsigned int's
	public static final int SIZE_OF = 512 * 4;

	protected IRenderingEngine re;
	protected boolean dirty;

	public int base;
    // The value of baseOffset has to be added (not ORed) to the base value.
    // baseOffset is updated by the ORIGIN_ADDR and OFFSET_ADDR commands,
    // and both commands share the same value field.
    public int baseOffset;
    public int fbp, fbw; // frame buffer pointer and width
    public int zbp, zbw; // depth buffer pointer and width
    public int psm; // pixel format
    public int region_x1, region_y1, region_x2, region_y2;
    public int region_width, region_height; // derived
    public int scissor_x1, scissor_y1, scissor_x2, scissor_y2;
    public int scissor_width, scissor_height; // derived
    public int offset_x, offset_y;
    public int viewport_width, viewport_height; // derived from xyscale
    public int viewport_cx, viewport_cy;
    public float[] proj_uploaded_matrix = new float[4 * 4];
    public float[] texture_uploaded_matrix = new float[4 * 4];
    public float[] model_uploaded_matrix = new float[4 * 4];
    public float[] view_uploaded_matrix = new float[4 * 4];
    public float[][] bone_uploaded_matrix = new float[8][4 * 3];
    public float[] boneMatrixLinear = new float[8 * 4 * 4]; // Linearized version of bone_uploaded_matrix
	public boolean depthMask;
    public int colorMask[] = new int[] { 0x00, 0x00, 0x00, 0x00 };
    public int alphaFunc;
    public int alphaRef;
	public int stencilFunc;
	public int stencilRef;
	public int stencilMask;
	public int stencilOpFail;
	public int stencilOpZFail;
	public int stencilOpZPass;
    public int textureFunc;
    public boolean textureColorDoubled;
    public boolean textureAlphaUsed;
    public boolean frontFaceCw;
    public int depthFunc;
    public float[] morph_weight = new float[8];
    public float[] tex_envmap_matrix = new float[4 * 4];
    public float[][] light_pos = new float[NUM_LIGHTS][4];
    public float[][] light_dir = new float[NUM_LIGHTS][4];
    public int[] light_type = new int[NUM_LIGHTS];
    public int[] light_kind = new int[NUM_LIGHTS];
    public float[][] lightAmbientColor = new float[NUM_LIGHTS][4];
    public float[][] lightDiffuseColor = new float[NUM_LIGHTS][4];
    public float[][] lightSpecularColor = new float[NUM_LIGHTS][4];
    public float[] spotLightExponent = new float[NUM_LIGHTS];
    public float[] spotLightCutoff = new float[NUM_LIGHTS];
    public float[] spotLightCosCutoff = new float[NUM_LIGHTS];
    public float[] lightConstantAttenuation = new float[NUM_LIGHTS];
    public float[] lightLinearAttenuation = new float[NUM_LIGHTS];
    public float[] lightQuadraticAttenuation = new float[NUM_LIGHTS];
    public int lightMode;
    public float[] fog_color = new float[4];
    public float fog_far = 0.0f, fog_dist = 0.0f;
    public int nearZ, farZ;
    public float zscale, zpos;
    public int mat_flags = 0;
    public float[] mat_ambient = new float[4];
    public float[] mat_diffuse = new float[4];
    public float[] mat_specular = new float[4];
    public float[] mat_emissive = new float[4];
    public float[] ambient_light = new float[4];
    public float materialShininess;
    public int texture_storage, texture_num_mip_maps;
    public boolean texture_swizzle;
    public int[] texture_base_pointer = new int[8];
    public int[] texture_width = new int[8];
    public int[] texture_height = new int[8];
    public int[] texture_buffer_width = new int[8];
    public int tex_min_filter = TFLT_NEAREST;
    public int tex_mag_filter = TFLT_NEAREST;
    public int tex_mipmap_mode;
    public float tex_mipmap_bias;
    public int tex_mipmap_bias_int;
    public boolean mipmapShareClut;
    public float tex_translate_x = 0.f, tex_translate_y = 0.f;
    public float tex_scale_x = 1.f, tex_scale_y = 1.f;
    public float[] tex_env_color = new float[4];
    public int tex_clut_addr;
    public int tex_clut_num_blocks;
    public int tex_clut_mode, tex_clut_shift, tex_clut_mask, tex_clut_start;
    public int tex_wrap_s = TWRAP_WRAP_MODE_REPEAT, tex_wrap_t = TWRAP_WRAP_MODE_REPEAT;
    public int tex_shade_u = 0;
    public int tex_shade_v = 0;
    public int patch_div_s;
    public int patch_div_t;
    public int patch_prim;
    public float tslope_level;
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
    public int sfix;
    public int dfix;
    public int blend_src;
    public int blend_dst;
    public int blendEquation;
    public int[] dither_matrix = new int[16];
    public int tex_map_mode = TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV;
    public int tex_proj_map_mode = TMAP_TEXTURE_PROJECTION_MODE_POSITION;
    public int colorTestFunc;
    public int[] colorTestRef = { 0, 0, 0 };
    public int[] colorTestMsk = { 0, 0, 0 };
    public int shadeModel;
    public int logicOp;
    public final List<EnableDisableFlag> flags = new LinkedList<EnableDisableFlag>();
    public final EnableDisableFlag alphaTestFlag = new EnableDisableFlag("GU_ALPHA_TEST", IRenderingEngine.GU_ALPHA_TEST);
    public final EnableDisableFlag depthTestFlag = new EnableDisableFlag("GU_DEPTH_TEST", IRenderingEngine.GU_DEPTH_TEST);
    public final EnableDisableFlag scissorTestFlag = new EnableDisableFlag("GU_SCISSOR_TEST", IRenderingEngine.GU_SCISSOR_TEST);
    public final EnableDisableFlag stencilTestFlag = new EnableDisableFlag("GU_STENCIL_TEST", IRenderingEngine.GU_STENCIL_TEST);
    public final EnableDisableFlag blendFlag = new EnableDisableFlag("GU_BLEND", IRenderingEngine.GU_BLEND);
    public final EnableDisableFlag cullFaceFlag = new EnableDisableFlag("GU_CULL_FACE", IRenderingEngine.GU_CULL_FACE);
    public final EnableDisableFlag ditherFlag = new EnableDisableFlag("GU_DITHER", IRenderingEngine.GU_DITHER);
    public final EnableDisableFlag fogFlag = new EnableDisableFlag("GU_FOG", IRenderingEngine.GU_FOG);
    public final EnableDisableFlag clipPlanesFlag = new EnableDisableFlag("GU_CLIP_PLANES", IRenderingEngine.GU_CLIP_PLANES);
    public final EnableDisableFlag textureFlag = new EnableDisableFlag("GU_TEXTURE_2D", IRenderingEngine.GU_TEXTURE_2D);
    public final EnableDisableFlag lightingFlag = new EnableDisableFlag("GU_LIGHTING", IRenderingEngine.GU_LIGHTING);
    public final EnableDisableFlag[] lightFlags = new EnableDisableFlag[]{
        new EnableDisableFlag("GU_LIGHT0", IRenderingEngine.GU_LIGHT0),
        new EnableDisableFlag("GU_LIGHT1", IRenderingEngine.GU_LIGHT1),
        new EnableDisableFlag("GU_LIGHT2", IRenderingEngine.GU_LIGHT2),
        new EnableDisableFlag("GU_LIGHT3", IRenderingEngine.GU_LIGHT3)
    };
    public final EnableDisableFlag lineSmoothFlag = new EnableDisableFlag("GU_LINE_SMOOTH", IRenderingEngine.GU_LINE_SMOOTH);
    public final EnableDisableFlag patchCullFaceFlag = new EnableDisableFlag("GU_PATCH_CULL_FACE", IRenderingEngine.GU_PATCH_CULL_FACE);
    public final EnableDisableFlag colorTestFlag = new EnableDisableFlag("GU_COLOR_TEST", IRenderingEngine.GU_COLOR_TEST);
    public final EnableDisableFlag colorLogicOpFlag = new EnableDisableFlag("GU_COLOR_LOGIC_OP", IRenderingEngine.GU_COLOR_LOGIC_OP);
    public final EnableDisableFlag faceNormalReverseFlag = new EnableDisableFlag("GU_FACE_NORMAL_REVERSE", IRenderingEngine.GU_FACE_NORMAL_REVERSE);
    public final EnableDisableFlag patchFaceFlag = new EnableDisableFlag("GU_PATCH_FACE", IRenderingEngine.GU_PATCH_FACE);
    public final EnableDisableFlag fragment2xFlag = new EnableDisableFlag("GU_FRAGMENT_2X", IRenderingEngine.GU_FRAGMENT_2X);
    public final EnableDisableFlag reColorMaterial = new EnableDisableFlag("RE_COLOR_MATERIAL", IRenderingEngine.RE_COLOR_MATERIAL);
    public final EnableDisableFlag reTextureGenS = new EnableDisableFlag("RE_TEXTURE_GEN_S", IRenderingEngine.RE_TEXTURE_GEN_S);
    public final EnableDisableFlag reTextureGenT = new EnableDisableFlag("RE_TEXTURE_GEN_T", IRenderingEngine.RE_TEXTURE_GEN_T);
    public float[] vertexColor = new float[4];
    public boolean useVertexColor;
    public boolean clearMode;
    public boolean clearModeColor;
    public boolean clearModeStencil;
    public boolean clearModeDepth;
    public VertexInfo vinfo = new VertexInfo();
    public int currentTextureId;

    public GeContext() {
        tex_envmap_matrix[0] = tex_envmap_matrix[5] = tex_envmap_matrix[10] = tex_envmap_matrix[15] = 1.f;
        light_pos[0][3] = light_pos[1][3] = light_pos[2][3] = light_pos[3][3] = 1.f;
        light_dir[0][3] = light_dir[1][3] = light_dir[2][3] = light_dir[3][3] = 1.f;
        morph_weight[0] = 1.f;
        tex_mipmap_mode = TBIAS_MODE_AUTO;
        tex_mipmap_bias = 0.f;
        tex_mipmap_bias_int = 0;
        mipmapShareClut = true;
        base = 0;
        baseOffset = 0;

        light_type[0] = light_type[1] = light_type[2] = light_type[3] = -1;
        light_kind[0] = light_kind[1] = light_kind[2] = light_kind[3] = -1;
        lightSpecularColor[0][0] = lightSpecularColor[1][0] = lightSpecularColor[2][0] = lightSpecularColor[3][0] = -1;

        dirty = false;
    }

    public void setRenderingEngine(IRenderingEngine re) {
    	this.re = re;
    }

    /**
     * Update the RenderingEngine based on the context values.
     * This method can only be called by a thread being allowed to perform
     * RenderingEngine calls (i.e. the GUI thread).
     */
    public void update() {
    	if (!dirty) {
    		return;
    	}

    	for (EnableDisableFlag flag : flags) {
    		flag.update();
    	}
    	if (fogFlag.isEnabled()) {
    		re.setFogHint();
    	}
    	if (lineSmoothFlag.isEnabled()) {
    		re.setLineSmoothHint();
    	}
    	re.setPatchDiv(patch_div_s, patch_div_t);
    	re.setPatchPrim(patch_prim);
    	re.setShadeModel(shadeModel);
    	re.setMaterialEmissiveColor(mat_emissive);
    	re.setMaterialShininess(materialShininess);
    	re.setLightModelAmbientColor(ambient_light);
    	re.setLightMode(lightMode);
    	for (int light = 0; light < NUM_LIGHTS; light++) {
    		re.setLightType(light, light_type[light], light_kind[light]);
    		re.setLightConstantAttenuation(light, lightConstantAttenuation[light]);
    		re.setLightLinearAttenuation(light, lightLinearAttenuation[light]);
    		re.setLightQuadraticAttenuation(light, lightQuadraticAttenuation[light]);
    		re.setLightAmbientColor(light, lightAmbientColor[light]);
    		re.setLightDiffuseColor(light, lightDiffuseColor[light]);
    		re.setLightSpecularColor(light, lightSpecularColor[light]);
    	}
    	re.setFrontFace(frontFaceCw);
    	re.setTextureEnvColor(tex_env_color);
    	re.setFogColor(fog_color);
    	re.setColorTestFunc(colorTestFunc);
    	re.setColorTestReference(colorTestRef);
    	re.setColorTestMask(colorTestMsk);
    	re.setAlphaFunc(alphaFunc, alphaRef);
    	re.setStencilFunc(stencilFunc, stencilRef, stencilMask);
    	re.setStencilOp(stencilOpFail, stencilOpZFail, stencilOpZPass);
    	re.setBlendEquation(blendEquation);
    	re.setLogicOp(logicOp);
    	re.setDepthMask(depthMask);
    	re.setColorMask(colorMask[0], colorMask[1], colorMask[2], colorMask[3]);
    	re.setTextureFunc(textureFunc, textureAlphaUsed, textureColorDoubled);

    	dirty = false;
    }

    public void setDirty() {
    	dirty = true;
    }

    @Override
	protected void read() {
		base = read32();
		baseOffset = read32();

		fbp = read32();
		fbw = read32();
		zbp = read32();
		zbw = read32();
		psm = read32();

		int flagBits = read32();
		for (EnableDisableFlag flag : flags) {
			flag.restore(flagBits);
		}

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
		readFloatArray(bone_uploaded_matrix);

		// Rebuild boneMatrixLinear from bone_uploaded_matrix
		for (int matrix = 0, j = 0; matrix < bone_uploaded_matrix.length; matrix++) {
			for (int i = 0; i < 12; i++, j++) {
				boneMatrixLinear[(j / 3) * 4 + (j % 3)] = bone_uploaded_matrix[matrix][i];
			}
		}

		depthMask = readBoolean();
		read32Array(colorMask);
		alphaFunc = read32();
		alphaRef = read32();
		stencilFunc = read32();
		stencilRef = read32();
		stencilMask = read32();
		stencilOpFail = read32();
		stencilOpZFail = read32();
		stencilOpZPass = read32();
		textureFunc = read32();
		textureColorDoubled = readBoolean();
		textureAlphaUsed = readBoolean();
		frontFaceCw = readBoolean();
		depthFunc = read32();

		readFloatArray(morph_weight);
		readFloatArray(tex_envmap_matrix);
		readFloatArray(light_pos);
		readFloatArray(light_dir);

		read32Array(light_type);
		read32Array(light_kind);
		readFloatArray(lightAmbientColor);
		readFloatArray(lightDiffuseColor);
		readFloatArray(lightSpecularColor);
		readFloatArray(spotLightExponent);
		readFloatArray(spotLightCutoff);
		readFloatArray(lightConstantAttenuation);
		readFloatArray(lightLinearAttenuation);
		readFloatArray(lightQuadraticAttenuation);
		lightMode = read32();

		readFloatArray(fog_color);
		fog_far = readFloat();
		fog_dist = readFloat();

		nearZ = read16();
		farZ = read16();
		zscale = readFloat();
		zpos = readFloat();

		mat_flags = read32();
		readFloatArray(mat_ambient);
		readFloatArray(mat_diffuse);
		readFloatArray(mat_specular);
		readFloatArray(mat_emissive);

		readFloatArray(ambient_light);
		materialShininess = readFloat();

		texture_storage = read32();
		texture_num_mip_maps = read32();
		texture_swizzle = readBoolean();

		read32Array(texture_base_pointer);
		read32Array(texture_width);
		read32Array(texture_height);
		read32Array(texture_buffer_width);
		tex_min_filter = read32();
		tex_mag_filter = read32();
		tex_mipmap_mode = read32();
		tex_mipmap_bias = readFloat();
		tex_mipmap_bias_int = read32();
		mipmapShareClut = readBoolean();

		tex_translate_x = readFloat();
		tex_translate_y = readFloat();
		tex_scale_x = readFloat();
		tex_scale_y = readFloat();
		readFloatArray(tex_env_color);

		tex_clut_addr = read32();
		tex_clut_num_blocks = read32();
		tex_clut_mode = read32();
		tex_clut_shift = read32();
		tex_clut_mask = read32();
		tex_clut_start = read32();
		tex_wrap_s = read32();
		tex_wrap_t = read32();
		tex_shade_u = read32();
		tex_shade_v = read32();
		patch_div_s = read32();
		patch_div_t = read32();
		patch_prim = read32();
		tslope_level = readFloat();

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
		blendEquation = read32();

		read32Array(dither_matrix);

		tex_map_mode = read32();
		tex_proj_map_mode = read32();

		colorTestFunc = read32();
		read32Array(colorTestRef);
		read32Array(colorTestMsk);

		shadeModel = read32();
		logicOp = read32();

		if (getOffset() > sizeof()) {
			log.error(String.format("GE context overflow: %d (max allowed=%d)", getOffset(), sizeof()));
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("GE context read size: %d (max allowed=%d)", getOffset(), sizeof()));
		}
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

		int flagBits = 0;
		for (EnableDisableFlag flag : flags) {
			flagBits = flag.save(flagBits);
		}
		write32(flagBits);

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
		writeFloatArray(bone_uploaded_matrix);

		writeBoolean(depthMask);
		write32Array(colorMask);
		write32(alphaFunc);
		write32(alphaRef);
		write32(stencilFunc);
		write32(stencilRef);
		write32(stencilMask);
		write32(stencilOpFail);
		write32(stencilOpZFail);
		write32(stencilOpZPass);
		write32(textureFunc);
		writeBoolean(textureColorDoubled);
		writeBoolean(textureAlphaUsed);
		writeBoolean(frontFaceCw);
		write32(depthFunc);

		writeFloatArray(morph_weight);
		writeFloatArray(tex_envmap_matrix);
		writeFloatArray(light_pos);
		writeFloatArray(light_dir);

		write32Array(light_type);
		write32Array(light_kind);
		writeFloatArray(lightAmbientColor);
		writeFloatArray(lightDiffuseColor);
		writeFloatArray(lightSpecularColor);
		writeFloatArray(spotLightExponent);
		writeFloatArray(spotLightCutoff);
		writeFloatArray(lightConstantAttenuation);
		writeFloatArray(lightLinearAttenuation);
		writeFloatArray(lightQuadraticAttenuation);
		write32(lightMode);

		writeFloatArray(fog_color);
		writeFloat(fog_far);
		writeFloat(fog_dist);

		write16((short) nearZ);
		write16((short) farZ);
		writeFloat(zscale);
		writeFloat(zpos);

		write32(mat_flags);
		writeFloatArray(mat_ambient);
		writeFloatArray(mat_diffuse);
		writeFloatArray(mat_specular);
		writeFloatArray(mat_emissive);

		writeFloatArray(ambient_light);
		writeFloat(materialShininess);

		write32(texture_storage);
		write32(texture_num_mip_maps);
		writeBoolean(texture_swizzle);

		write32Array(texture_base_pointer);
		write32Array(texture_width);
		write32Array(texture_height);
		write32Array(texture_buffer_width);
		write32(tex_min_filter);
		write32(tex_mag_filter);
		write32(tex_mipmap_mode);
		writeFloat(tex_mipmap_bias);
		write32(tex_mipmap_bias_int);
		writeBoolean(mipmapShareClut);

		writeFloat(tex_translate_x);
		writeFloat(tex_translate_y);
		writeFloat(tex_scale_x);
		writeFloat(tex_scale_y);
		writeFloatArray(tex_env_color);

		write32(tex_clut_addr);
		write32(tex_clut_num_blocks);
		write32(tex_clut_mode);
		write32(tex_clut_shift);
		write32(tex_clut_mask);
		write32(tex_clut_start);
		write32(tex_wrap_s);
		write32(tex_wrap_t);
		write32(tex_shade_u);
		write32(tex_shade_v);
		write32(patch_div_s);
		write32(patch_div_t);
		write32(patch_prim);
		writeFloat(tslope_level);

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
		write32(blendEquation);

		write32Array(dither_matrix);

		write32(tex_map_mode);
		write32(tex_proj_map_mode);

		write32(colorTestFunc);
		write32Array(colorTestRef);
		write32Array(colorTestMsk);

		write32(shadeModel);
		write32(logicOp);

		if (getOffset() > sizeof()) {
			log.error(String.format("GE context overflow: %d (max allowed=%d)", getOffset(), sizeof()));
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("GE context write size: %d (max allowed=%d)", getOffset(), sizeof()));
		}
	}

	@Override
	public int sizeof() {
		return SIZE_OF;
	}

    @Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		for (EnableDisableFlag flag : flags) {
			if (flag.isEnabled()) {
				if (result.length() > 0) {
					result.append(", ");
				}
				result.append(flag.toString());
			}
		}

		return result.toString();
	}

    private static int contextBitCount = 0;
    public class EnableDisableFlag {
        private boolean enabled;
        private final int reFlag;
        private final String name;
        private int contextBit;

        public EnableDisableFlag(String name, int reFlag) {
            this.name = name;
            this.reFlag = reFlag;
            init();
        }

        private void init() {
            enabled = false;
            contextBit = contextBitCount++;
            flags.add(this);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int isEnabledInt() {
            return enabled ? 1 : 0;
        }

        public void setEnabled(int enabledInt) {
            setEnabled(enabledInt != 0);
        }

        /**
         * Enable/Disable the flag. Update the flag in RenderingEngine.
         *
         * @param enabled        new flag value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            update();

            if (log.isDebugEnabled() && name != null) {
                log.debug(String.format("sceGu%s(%s)", enabled ? "Enable" : "Disable", name));
            }
        }

        public void update() {
            // Update the flag in RenderingEngine
            if (enabled) {
            	re.enableFlag(reFlag);
            } else {
            	re.disableFlag(reFlag);
            }
        }

        public void updateEnabled() {
        	if (enabled) {
        		re.enableFlag(reFlag);
        	}
        }

        public int save(int bits) {
        	if (enabled) {
        		bits |= (1 << contextBit);
        	}
        	return bits;
        }

        public void restore(int bits) {
        	enabled = (bits & (1 << contextBit)) != 0;
        }

		public int getReFlag() {
			return reFlag;
		}

		@Override
		public String toString() {
			return name;
		}
    }
}
