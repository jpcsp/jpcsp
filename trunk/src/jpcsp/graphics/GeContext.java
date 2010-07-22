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

import jpcsp.graphics.RE.IRenderingEngine;

/**
 * @author gid15
 *
 */
public class GeContext {
	private final static Logger log = VideoEngine.log;
	private IRenderingEngine re;

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
    public float[] boneMatrixForShader = new float[8 * 4 * 4]; // Linearized version of bone_uploaded_matrix
	public boolean depthMask;
    public int colorMask[] = new int[] { 0x00, 0x00, 0x00, 0x00 };
	public int stencilFunc;
	public int stencilRef;
	public int stencilMask;
	public int stencilOpFail;
	public int stencilOpZFail;
	public int stencilOpZPass;
    public int textureFunc;
    public boolean textureColorDoubled;
    public boolean textureAlphaUsed;
    public int depthFunc;
    public float[] morph_weight = new float[8];
    public float[] tex_envmap_matrix = new float[4 * 4];
    public float[][] light_pos = new float[NUM_LIGHTS][4];
    public float[][] light_dir = new float[NUM_LIGHTS][3];
    public int[] light_enabled = new int[NUM_LIGHTS];
    public int[] light_type = new int[NUM_LIGHTS];
    public int[] light_kind = new int[NUM_LIGHTS];
    public float[][] lightAmbientColor = new float[NUM_LIGHTS][4];
    public float[][] lightDiffuseColor = new float[NUM_LIGHTS][4];
    public float[][] lightSpecularColor = new float[NUM_LIGHTS][4];
    public float[] spotLightExponent = new float[NUM_LIGHTS];
    public float[] spotLightCutoff = new float[NUM_LIGHTS];
    public float[] fog_color = new float[4];
    public float fog_far = 0.0f, fog_dist = 0.0f;
    public float nearZ = 0.0f, farZ = 0.0f, zscale, zpos;
    public int mat_flags = 0;
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
    public int blend_src;
    public int blend_dst;
    public int[] dither_matrix = new int[16];
    public int tex_map_mode = TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV;
    public int tex_proj_map_mode = TMAP_TEXTURE_PROJECTION_MODE_POSITION;
    public int shaderCtestFunc;
    public int[] shaderCtestRef = { 0, 0, 0 };
    public int[] shaderCtestMsk = { 0, 0, 0 };
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

    public GeContext() {
        tex_envmap_matrix[0] = tex_envmap_matrix[5] = tex_envmap_matrix[10] = tex_envmap_matrix[15] = 1.f;
        light_pos[0][3] = light_pos[1][3] = light_pos[2][3] = light_pos[3][3] = 1.f;
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
        light_enabled[0] = light_enabled[1] = light_enabled[2] = light_enabled[3] = -1;
    }

    public void setRenderingEngine(IRenderingEngine re) {
    	this.re = re;
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

            if (log.isDebugEnabled()) {
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

        public int save(int bits) {
            return bits | (1 << contextBit);
        }

        public void restore(int bits) {
            setEnabled((bits & (1 << contextBit)) != 0);
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
