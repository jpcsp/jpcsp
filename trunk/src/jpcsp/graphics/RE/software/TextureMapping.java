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
package jpcsp.graphics.RE.software;

import static jpcsp.graphics.GeCommands.LIGHT_AMBIENT_DIFFUSE;
import static jpcsp.util.Utilities.copy;
import static jpcsp.util.Utilities.dot3;
import static jpcsp.util.Utilities.normalize3;

import org.apache.log4j.Logger;

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 */
public class TextureMapping {
	protected static final Logger log = VideoEngine.log;

	public static IPixelFilter getTextureMapping(GeContext context) {
		IPixelFilter textureMapping = null;

		switch (context.tex_map_mode) {
			case GeCommands.TMAP_TEXTURE_MAP_MODE_TEXTURE_COORDIATES_UV:
				if (context.tex_scale_x != 1.f || context.tex_scale_y != 1.f) {
					if (context.tex_translate_x != 0.f || context.tex_translate_y != 0.f) {
						// Scale!=1.0 and translate!=0.0, both scale and translation mappings necessary
						textureMapping = new TextureMapUVScaleTranslate(context.tex_scale_x, context.tex_scale_y, context.tex_translate_x, context.tex_translate_y);
					} else {
						// Scale!=1.0 and translate==0.0, only scale mapping necessary
						textureMapping = new TextureMapUVScale(context.tex_scale_x, context.tex_scale_y);
					}
				} else {
					if (context.tex_translate_x != 0.f || context.tex_translate_y != 0.f) {
						// Scale==1.0 and translate!=0.0, only translation mapping necessary
						textureMapping = new TextureMapUVTranslate(context.tex_translate_x, context.tex_translate_y);
					} else {
						// Scale==1.0 and translate==0.0, no mapping necessary
					}
				}
				break;
			case GeCommands.TMAP_TEXTURE_MAP_MODE_TEXTURE_MATRIX:
				switch (context.tex_proj_map_mode) {
					case GeCommands.TMAP_TEXTURE_PROJECTION_MODE_POSITION:
						if (context.vinfo.position != 0) {
							textureMapping = new TextureMapTextureMatrixPosition(context.texture_uploaded_matrix);
						}
						break;
					case GeCommands.TMAP_TEXTURE_PROJECTION_MODE_TEXTURE_COORDINATES:
						if (context.vinfo.texture != 0) {
							textureMapping = new TextureMapTextureMatrixUV(context.texture_uploaded_matrix);
						}
						break;
					case GeCommands.TMAP_TEXTURE_PROJECTION_MODE_NORMALIZED_NORMAL:
						if (context.vinfo.normal != 0) {
							textureMapping = new TextureMapTextureMatrixNormalizedNormal(context.texture_uploaded_matrix);
						}
						break;
					case GeCommands.TMAP_TEXTURE_PROJECTION_MODE_NORMAL:
						if (context.vinfo.normal != 0) {
							textureMapping = new TextureMapTextureMatrixNormal(context.texture_uploaded_matrix);
						}
						break;
				}
				break;
			case GeCommands.TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP:
				int u = context.tex_shade_u;
				int v = context.tex_shade_v;
				textureMapping = new TextureMapEnvironment(context.materialShininess, context.light_pos[u], context.light_pos[v], context.light_type[u], context.light_type[v]);
				break;
		}

		return textureMapping;
	}

	private static class TextureMapUVScaleTranslate implements IPixelFilter {
		private float scaleU;
		private float scaleV;
		private float translateU;
		private float translateV;

		public TextureMapUVScaleTranslate(float scaleU, float scaleV, float translateU, float translateV) {
			this.scaleU = scaleU;
			this.scaleV = scaleV;
			this.translateU = translateU;
			this.translateV = translateV;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.u = pixel.u * scaleU + translateU;
			pixel.v = pixel.v * scaleV + translateV;
		}

		@Override
		public int getCompilationId() {
			return 670254118;
		}

		@Override
		public int getFlags() {
			return REQUIRES_TEXTURE_U_V;
		}
	}

	private static class TextureMapUVScale implements IPixelFilter {
		private float scaleU;
		private float scaleV;

		public TextureMapUVScale(float scaleU, float scaleV) {
			this.scaleU = scaleU;
			this.scaleV = scaleV;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.u = pixel.u * scaleU;
			pixel.v = pixel.v * scaleV;
		}

		@Override
		public int getCompilationId() {
			return 66102184;
		}

		@Override
		public int getFlags() {
			return REQUIRES_TEXTURE_U_V;
		}
	}

	private static class TextureMapUVTranslate implements IPixelFilter {
		private float translateU;
		private float translateV;

		public TextureMapUVTranslate(float translateU, float translateV) {
			this.translateU = translateU;
			this.translateV = translateV;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.u += translateU;
			pixel.v += translateV;
		}

		@Override
		public int getCompilationId() {
			return 609727972;
		}

		@Override
		public int getFlags() {
			return REQUIRES_TEXTURE_U_V;
		}
	}

	private static abstract class TextureMapTextureMatrix implements IPixelFilter {
		private float matrix00;
		private float matrix01;
		private float matrix02;
		private float matrix10;
		private float matrix11;
		private float matrix12;
		private float matrix20;
		private float matrix21;
		private float matrix22;
		private float matrix30;
		private float matrix31;
		private float matrix32;

		protected TextureMapTextureMatrix(float[] matrix) {
			matrix00 = matrix[0];
			matrix01 = matrix[1];
			matrix02 = matrix[2];
			matrix10 = matrix[3];
			matrix11 = matrix[4];
			matrix12 = matrix[5];
			matrix20 = matrix[6];
			matrix21 = matrix[7];
			matrix22 = matrix[8];
			matrix30 = matrix[9];
			matrix31 = matrix[10];
			matrix32 = matrix[11];
		}

		protected void filter(PixelState pixel, float x, float y, float z) {
			pixel.u = x * matrix00 + y * matrix10 + z * matrix20 + matrix30;
			pixel.v = x * matrix01 + y * matrix11 + z * matrix21 + matrix31;
			pixel.q = x * matrix02 + y * matrix12 + z * matrix22 + matrix32;
		}

		protected void filter(PixelState pixel, float x, float y) {
			// We know that "z" is 0
			pixel.u = x * matrix00 + y * matrix10 + matrix30;
			pixel.v = x * matrix01 + y * matrix11 + matrix31;
			pixel.q = x * matrix02 + y * matrix12 + matrix32;
		}
	}

	private static class TextureMapTextureMatrixPosition extends TextureMapTextureMatrix {
		protected TextureMapTextureMatrixPosition(float[] matrix) {
			super(matrix);
		}

		@Override
		public void filter(PixelState pixel) {
			float[] V = pixel.getV();
			filter(pixel, V[0], V[1], V[2]);
		}

		@Override
		public int getCompilationId() {
			return 559249837;
		}

		@Override
		public int getFlags() {
			return DISCARDS_TEXTURE_U_V;
		}
	}

	private static class TextureMapTextureMatrixUV extends TextureMapTextureMatrix {
		protected TextureMapTextureMatrixUV(float[] matrix) {
			super(matrix);
		}

		@Override
		public void filter(PixelState pixel) {
			filter(pixel, pixel.u, pixel.v);
		}

		@Override
		public int getCompilationId() {
			return 873419340;
		}

		@Override
		public int getFlags() {
			return REQUIRES_TEXTURE_U_V;
		}
	}

	private static class TextureMapTextureMatrixNormal extends TextureMapTextureMatrix {
		protected TextureMapTextureMatrixNormal(float[] matrix) {
			super(matrix);
		}

		@Override
		public void filter(PixelState pixel) {
			float[] N = pixel.getN();
			filter(pixel, N[0], N[1], N[2]);
		}

		@Override
		public int getCompilationId() {
			return 854835260;
		}

		@Override
		public int getFlags() {
			return DISCARDS_TEXTURE_U_V;
		}
	}

	private static class TextureMapTextureMatrixNormalizedNormal extends TextureMapTextureMatrix {
		protected TextureMapTextureMatrixNormalizedNormal(float[] matrix) {
			super(matrix);
		}

		@Override
		public void filter(PixelState pixel) {
			float[] normalizedN = pixel.getNormalizedN();
			filter(pixel, normalizedN[0], normalizedN[1], normalizedN[2]);
		}

		@Override
		public int getCompilationId() {
			return 971192707;
		}

		@Override
		public int getFlags() {
			return DISCARDS_TEXTURE_U_V;
		}
	}

	private static class TextureMapEnvironment implements IPixelFilter {
		protected float shininess;
		protected float[] lightPositionU = new float[4];
		protected float[] lightPositionV = new float[4];
		protected boolean diffuseLightU;
		protected boolean diffuseLightV;
		protected final float[] Ve = new float[3];
		protected final float[] Ne = new float[3];
		protected final float[] Lu = new float[3];
		protected final float[] Lv = new float[3];

		public TextureMapEnvironment(float shininess, float[] lightPositionU, float[] lightPositionV, int lightTypeU, int lightTypeV) {
			this.shininess = shininess;
			diffuseLightU = (lightTypeU == LIGHT_AMBIENT_DIFFUSE);
			diffuseLightV = (lightTypeV == LIGHT_AMBIENT_DIFFUSE);
			copy(this.lightPositionU, lightPositionU);
			copy(this.lightPositionV, lightPositionV);
		}

		protected float getP(boolean diffuseLight, float[] L) {
			float P;
			if (diffuseLight) {
				normalize3(L, L);
				P = dot3(Ne, L);
			} else {
				L[2] += 1.f;
				normalize3(L, L);
				P = (float) Math.pow(dot3(Ne, L), shininess);
			}

			return P;
		}

		@Override
		public void filter(PixelState pixel) {
			// Implementation based on shader.vert/ApplyTexture:
			//
			//   vec3  Nn = normalize(N);
            //   vec3  Ve = vec3(gl_ModelViewMatrix * V);
            //   float k  = gl_FrontMaterial.shininess;
            //   vec3  Lu = gl_LightSource[texShade.x].position.xyz - Ve.xyz * gl_LightSource[texShade.x].position.w;
            //   vec3  Lv = gl_LightSource[texShade.y].position.xyz - Ve.xyz * gl_LightSource[texShade.y].position.w;
            //   float Pu = psp_lightKind[texShade.x] == 0 ? dot(Nn, normalize(Lu)) : pow(dot(Nn, normalize(Lu + vec3(0.0, 0.0, 1.0))), k);
            //   float Pv = psp_lightKind[texShade.y] == 0 ? dot(Nn, normalize(Lv)) : pow(dot(Nn, normalize(Lv + vec3(0.0, 0.0, 1.0))), k);
            //   T.xyz = vec3(0.5*vec2(1.0 + Pu, 1.0 + Pv), 1.0);
			//

			pixel.getVe(Ve);
			pixel.getNormalizedNe(Ne);

			for (int i = 0; i < 3; i++) {
				Lu[i] = lightPositionU[i] - Ve[i] * lightPositionU[3];
				Lv[i] = lightPositionV[i] - Ve[i] * lightPositionV[3];
			}

			float Pu = getP(diffuseLightU, Lu);
			float Pv = getP(diffuseLightV, Lv);

			pixel.u = (Pu + 1.f) * 0.5f;
			pixel.v = (Pv + 1.f) * 0.5f;
			pixel.q = 1.f;
		}

		@Override
		public int getCompilationId() {
			return 490704930;
		}

		@Override
		public int getFlags() {
			return DISCARDS_TEXTURE_U_V;
		}
	}
}
