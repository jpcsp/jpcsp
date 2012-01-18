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
import static jpcsp.util.Utilities.length3;
import static jpcsp.util.Utilities.matrixMult;
import static jpcsp.util.Utilities.vectorMult;

import org.apache.log4j.Logger;

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.VertexState;
import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 */
public class TextureMapping {
	protected static final Logger log = VideoEngine.log;

	public static IPixelFilter getTextureMapping(GeContext context, VertexState v1, VertexState v2, VertexState v3) {
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
							textureMapping = new TextureMapTextureMatrixPosition(context.texture_uploaded_matrix, v1.p, v2.p, v3.p);
						}
						break;
					case GeCommands.TMAP_TEXTURE_PROJECTION_MODE_TEXTURE_COORDINATES:
						if (context.vinfo.texture != 0) {
							textureMapping = new TextureMapTextureMatrixUV(context.texture_uploaded_matrix);
						}
						break;
					case GeCommands.TMAP_TEXTURE_PROJECTION_MODE_NORMALIZED_NORMAL:
						if (context.vinfo.normal != 0) {
							textureMapping = new TextureMapTextureMatrixNormalizedNormal(context.texture_uploaded_matrix, v1.n, v2.n, v3.n);
						}
						break;
					case GeCommands.TMAP_TEXTURE_PROJECTION_MODE_NORMAL:
						if (context.vinfo.normal != 0) {
							textureMapping = new TextureMapTextureMatrixNormal(context.texture_uploaded_matrix, v1.n, v2.n, v3.n);
						}
						break;
				}
				break;
			case GeCommands.TMAP_TEXTURE_MAP_MODE_ENVIRONMENT_MAP:
				int u = context.tex_shade_u;
				int v = context.tex_shade_v;
				// Compute the Model-View matrix
				float[] modelViewMatrix = new float[16];
				matrixMult(modelViewMatrix, context.view_uploaded_matrix, context.model_uploaded_matrix);
				textureMapping = new TextureMapEnvironment(modelViewMatrix, context.materialShininess, context.light_pos[u], context.light_pos[v], context.light_type[u], context.light_type[v], v1, v2, v3);
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
		public int filter(PixelState pixel) {
			pixel.u = pixel.u * scaleU + translateU;
			pixel.v = pixel.v * scaleV + translateV;
			return pixel.source;
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
		public int filter(PixelState pixel) {
			pixel.u = pixel.u * scaleU;
			pixel.v = pixel.v * scaleV;
			return pixel.source;
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
		public int filter(PixelState pixel) {
			pixel.u += translateU;
			pixel.v += translateV;
			return pixel.source;
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

		protected int filter(PixelState pixel, float x, float y, float z) {
			pixel.u = x * matrix00 + y * matrix10 + z * matrix20 + matrix30;
			pixel.v = x * matrix01 + y * matrix11 + z * matrix21 + matrix31;
			pixel.q = x * matrix02 + y * matrix12 + z * matrix22 + matrix32;
			return pixel.source;
		}

		protected int filter(PixelState pixel, float x, float y) {
			// We know that "z" is 0
			pixel.u = x * matrix00 + y * matrix10 + matrix30;
			pixel.v = x * matrix01 + y * matrix11 + matrix31;
			pixel.q = x * matrix02 + y * matrix12 + matrix32;
			return pixel.source;
		}
	}

	private static class TextureMapTextureMatrixPosition extends TextureMapTextureMatrix {
		protected float p1x, p1y, p1z;
		protected float p2x, p2y, p2z;
		protected float p3x, p3y, p3z;

		protected TextureMapTextureMatrixPosition(float[] matrix, float[] p1, float[] p2, float[] p3) {
			super(matrix);
			p1x = p1[0];
			p1y = p1[1];
			p1z = p1[2];
			p2x = p2[0];
			p2y = p2[1];
			p2z = p2[2];
			p3x = p3[0];
			p3y = p3[1];
			p3z = p3[2];
		}

		@Override
		public int filter(PixelState pixel) {
			float x = pixel.getTriangleWeightedValue(p1x, p2x, p3x);
			float y = pixel.getTriangleWeightedValue(p1y, p2y, p3y);
			float z = pixel.getTriangleWeightedValue(p1z, p2z, p3z);
			return filter(pixel, x, y, z);
		}
	}

	private static class TextureMapTextureMatrixUV extends TextureMapTextureMatrix {
		protected TextureMapTextureMatrixUV(float[] matrix) {
			super(matrix);
		}

		@Override
		public int filter(PixelState pixel) {
			return filter(pixel, pixel.u, pixel.v);
		}
	}

	private static class TextureMapTextureMatrixNormal extends TextureMapTextureMatrix {
		protected float n1x, n1y, n1z;
		protected float n2x, n2y, n2z;
		protected float n3x, n3y, n3z;

		protected TextureMapTextureMatrixNormal(float[] matrix, float[] n1, float[] n2, float[] n3) {
			super(matrix);
			n1x = n1[0];
			n1y = n1[1];
			n1z = n1[2];
			n2x = n2[0];
			n2y = n2[1];
			n2z = n2[2];
			n3x = n3[0];
			n3y = n3[1];
			n3z = n3[2];
		}

		@Override
		public int filter(PixelState pixel) {
			float x = pixel.getTriangleWeightedValue(n1x, n2x, n3x);
			float y = pixel.getTriangleWeightedValue(n1y, n2y, n3y);
			float z = pixel.getTriangleWeightedValue(n1z, n2z, n3z);
			return filter(pixel, x, y, z);
		}
	}

	private static class TextureMapTextureMatrixNormalizedNormal extends TextureMapTextureMatrix {
		protected float n1x, n1y, n1z;
		protected float n2x, n2y, n2z;
		protected float n3x, n3y, n3z;

		protected TextureMapTextureMatrixNormalizedNormal(float[] matrix, float[] n1, float[] n2, float[] n3) {
			super(matrix);
			float invertedLength = getInvertedLength(n1);
			n1x = n1[0] * invertedLength;
			n1y = n1[1] * invertedLength;
			n1z = n1[2] * invertedLength;
			invertedLength = getInvertedLength(n2);
			n2x = n2[0] * invertedLength;
			n2y = n2[1] * invertedLength;
			n2z = n2[2] * invertedLength;
			invertedLength = getInvertedLength(n3);
			n3x = n3[0] * invertedLength;
			n3y = n3[1] * invertedLength;
			n3z = n3[2] * invertedLength;
		}

		protected static float getInvertedLength(float[] n) {
			float length = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
			return 1.f / length;
		}

		@Override
		public int filter(PixelState pixel) {
			float x = pixel.getTriangleWeightedValue(n1x, n2x, n3x);
			float y = pixel.getTriangleWeightedValue(n1y, n2y, n3y);
			float z = pixel.getTriangleWeightedValue(n1z, n2z, n3z);
			return filter(pixel, x, y, z);
		}
	}

	private static class TextureMapEnvironment implements IPixelFilter {
		protected final float[] modelViewMatrix = new float[16];
		protected float shininess;
		protected float[] lightPositionU = new float[4];
		protected float[] lightPositionV = new float[4];
		protected boolean diffuseLightU;
		protected boolean diffuseLightV;
		protected final float[] Ve = new float[3];
		protected final float[] V = new float[3];
		protected final float[] N = new float[3];
		protected final float[] Lu = new float[3];
		protected final float[] Lv = new float[3];
		protected final float[] p1 = new float[3];
		protected final float[] p2 = new float[3];
		protected final float[] p3 = new float[3];
		protected final float[] n1 = new float[3];
		protected final float[] n2 = new float[3];
		protected final float[] n3 = new float[3];

		public TextureMapEnvironment(float[] modelViewMatrix, float shininess, float[] lightPositionU, float[] lightPositionV, int lightTypeU, int lightTypeV, VertexState v1, VertexState v2, VertexState v3) {
			System.arraycopy(modelViewMatrix, 0, this.modelViewMatrix, 0, this.modelViewMatrix.length);
			this.shininess = shininess;
			diffuseLightU = (lightTypeU == LIGHT_AMBIENT_DIFFUSE);
			diffuseLightV = (lightTypeV == LIGHT_AMBIENT_DIFFUSE);
			copy(this.lightPositionU, lightPositionU);
			copy(this.lightPositionV, lightPositionV);
			copy(p1, v1.p);
			copy(p2, v2.p);
			copy(p3, v3.p);
			copy(n1, v1.n);
			copy(n2, v2.n);
			copy(n3, v3.n);
		}

		protected void normalize3(float[] a) {
			float lengthInvertex = 1.f / length3(a);
			a[0] *= lengthInvertex;
			a[1] *= lengthInvertex;
			a[2] *= lengthInvertex;
		}

		protected float getP(boolean diffuseLight, float[] L) {
			float P;
			if (diffuseLight) {
				normalize3(L);
				P = dot3(N, L);
			} else {
				L[2] += 1.f;
				normalize3(L);
				P = (float) Math.pow(dot3(N, L), shininess);
			}

			return P;
		}

		@Override
		public int filter(PixelState pixel) {
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

			pixel.getTriangleWeightedValue(V, p1, p2, p3);
			vectorMult(Ve, modelViewMatrix, V);

			pixel.getTriangleWeightedValue(N, n1, n2, n3);
			normalize3(N);

			for (int i = 0; i < 3; i++) {
				Lu[i] = lightPositionU[i] - Ve[i] * lightPositionU[3];
				Lv[i] = lightPositionV[i] - Ve[i] * lightPositionV[3];
			}

			float Pu = getP(diffuseLightU, Lu);
			float Pv = getP(diffuseLightV, Lv);

			pixel.u = (Pu + 1.f) * 0.5f;
			pixel.v = (Pv + 1.f) * 0.5f;
			pixel.q = 1.f;

			return pixel.source;
		}
	}
}
