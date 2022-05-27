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
import static jpcsp.graphics.GeCommands.LIGHT_DIRECTIONAL;
import static jpcsp.graphics.GeCommands.LIGHT_POWER_DIFFUSE_SPECULAR;
import static jpcsp.graphics.GeCommands.LIGHT_SPOT;
import static jpcsp.graphics.RE.software.PixelColor.ZERO;
import static jpcsp.graphics.RE.software.PixelColor.addBGR;
import static jpcsp.graphics.RE.software.PixelColor.getAlpha;
import static jpcsp.graphics.RE.software.PixelColor.getColor;
import static jpcsp.graphics.RE.software.PixelColor.multiplyBGR;
import static jpcsp.graphics.RE.software.PixelColor.multiplyComponent;
import static jpcsp.graphics.RE.software.PixelColor.setAlpha;
import static jpcsp.graphics.VideoEngine.NUM_LIGHTS;
import static jpcsp.util.Utilities.clamp;
import static jpcsp.util.Utilities.dot3;
import static jpcsp.util.Utilities.length3;
import static jpcsp.util.Utilities.max;
import static jpcsp.util.Utilities.normalize3;
import static jpcsp.util.Utilities.pow;
import static jpcsp.util.Utilities.vectorMult34;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext.EnableDisableFlag;
import jpcsp.graphics.VideoEngine;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public final class Lighting {
	protected static final Logger log = VideoEngine.log;
	protected static final boolean disableLighting = false;

	private final int materialEmission;
	private final int ambient;
	private final int ambientAlpha;
	private final boolean[] lightEnabled = new boolean[NUM_LIGHTS];
	private final boolean someLightsEnabled;
	private final float[][] ecLightPosition = new float[NUM_LIGHTS][3];
	private final int[] lightKind = new int[NUM_LIGHTS];
	private final int[] lightAmbientColor = new int[NUM_LIGHTS];
	private final int[] lightDiffuseColor = new int[NUM_LIGHTS];
	private final int[] lightSpecularColor = new int[NUM_LIGHTS];
	private final float[] constantAttenuation = new float[NUM_LIGHTS];
	private final float[] linearAttenuation = new float[NUM_LIGHTS];
	private final float[] quadraticAttenuation = new float[NUM_LIGHTS];
	private final float[] spotCutoff = new float[NUM_LIGHTS];
	private final float[] spotCosCutoff = new float[NUM_LIGHTS];
	private final float[][] ecSpotDirection = new float[NUM_LIGHTS][3];
	private final float[] spotExponent = new float[NUM_LIGHTS];
	private float shininess;
	private boolean separateSpecularColor;
	private boolean[] isSpotLight = new boolean[NUM_LIGHTS];
	private boolean[] isDirectionalLight = new boolean[NUM_LIGHTS];
	private boolean hasSomeNonDirectionalLight;
	private final float[] L = new float[3];
	private final float[] H = new float[3];
	private final float[] nL = new float[3];
	private final float[] nH = new float[3];
	private final float[] nSD = new float[3];
	private final float[] Ve = new float[3];
	private final float[] Ne = new float[3];
	private int Al;
	private int Dl;
	private int Sl;
	private boolean hasNormal;

	public Lighting(float[] viewMatrix, float[] materialEmission, float[] ambient, EnableDisableFlag[] lightEnabled, float[][] lightPosition, int[] lightKind, int[] lightType, float[][] lightAmbientColor, float[][] lightDiffuseColor, float[][] lightSpecularColor, float[] constantAttenuation, float[] linearAttenuation, float[] quadraticAttenuation, float[] spotCutoff, float[] spotCosCutoff, float[][] spotDirection, float[] spotExponent, float shininess, int lightMode, boolean hasNormal) {
		this.materialEmission = getColor(materialEmission);
		this.ambient = getColor(ambient);
		this.ambientAlpha = getAlpha(this.ambient);
		this.shininess = shininess;
		this.separateSpecularColor = lightMode == GeCommands.LMODE_SEPARATE_SPECULAR_COLOR;
		this.hasNormal = hasNormal;

		boolean someLightsEnabled = false;
		boolean hasSomeNonDirectionalLight = false;
		for (int l = 0; l < NUM_LIGHTS; l++) {
			boolean isLightEnabled = lightEnabled[l].isEnabled() && !disableLighting;
			this.lightEnabled[l] = isLightEnabled;
			if (isLightEnabled) {
				someLightsEnabled |= isLightEnabled;
				this.lightKind[l] = lightKind[l];
				this.lightAmbientColor[l] = getColor(lightAmbientColor[l]);
				this.lightDiffuseColor[l] = getColor(lightDiffuseColor[l]);
				this.lightSpecularColor[l] = getColor(lightSpecularColor[l]);
				this.constantAttenuation[l] = constantAttenuation[l];
				this.linearAttenuation[l] = linearAttenuation[l];
				this.quadraticAttenuation[l] = quadraticAttenuation[l];
				this.spotCutoff[l] = spotCutoff[l];
				this.spotCosCutoff[l] = spotCosCutoff[l];
				this.spotExponent[l] = spotExponent[l];
				isSpotLight[l] = lightType[l] == LIGHT_SPOT && spotCutoff[l] < 180.f;
				isDirectionalLight[l] = lightType[l] == LIGHT_DIRECTIONAL;
				hasSomeNonDirectionalLight |= !isDirectionalLight[l];
				// The Model transformation does not apply to the light positions and directions.
				// Only apply the View transformation to map to the Eye Coordinate system.
				vectorMult34(ecLightPosition[l], viewMatrix, lightPosition[l]);
				vectorMult34(ecSpotDirection[l], viewMatrix, spotDirection[l]);
			}
		}
		this.someLightsEnabled = someLightsEnabled;
		this.hasSomeNonDirectionalLight = hasSomeNonDirectionalLight;
	}

	/**
	 * This is the equivalent of the vertex shader implementation:
	 *     shader.vert: ComputeLight
	 *
	 * @param pixel
	 * @param l
	 */
	private final void computeLight(int l) {
		boolean isDirectionalLight = this.isDirectionalLight[l];

		if (!hasNormal && isDirectionalLight) {
			// A simple case...
			Al = addBGR(Al, lightAmbientColor[l]);
			return;
		}

		float att = 1.f;
		L[0] = ecLightPosition[l][0];
		L[1] = ecLightPosition[l][1];
		L[2] = ecLightPosition[l][2];
		if (!isDirectionalLight) {
			L[0] -= Ve[0];
			L[1] -= Ve[1];
			L[2] -= Ve[2];

			float d = length3(L);
			att = clamp(1.f / (constantAttenuation[l] + (linearAttenuation[l] + quadraticAttenuation[l] * d) * d), 0.f, 1.f);
			if (isSpotLight[l]) {
				normalize3(nSD, ecSpotDirection[l]);
				float spot = dot3(nSD, -L[0], -L[1], -L[2]);
				att *= spot < spotCosCutoff[l] ? 0.f : pow(spot, spotExponent[l]);
			}
		}

		if (hasNormal) {
			H[0] = L[0];
			H[1] = L[1];
			H[2] = L[2] + 1.f;
			normalize3(nL, L);
			float NdotL = max(dot3(nL, Ne), 0.f);
			normalize3(nH, H);
			float NdotH = max(dot3(nH, Ne), 0.f);
			float k = shininess;
			float Dk = lightKind[l] == LIGHT_POWER_DIFFUSE_SPECULAR ? max(pow(NdotL, k), 0.f) : NdotL;
			float Sk = lightKind[l] != LIGHT_AMBIENT_DIFFUSE ? max(pow(NdotH, k), 0.f) : 0.f;

			Dl = addBGR(Dl, multiplyBGR(lightDiffuseColor[l], att * Dk));
			Sl = addBGR(Sl, multiplyBGR(lightSpecularColor[l], att * Sk));
		}
		Al = addBGR(Al, multiplyBGR(lightAmbientColor[l], att));
	}

	/**
	 * Apply the PSP lighting model.
	 * The implementation is based on the vertex shader implementation:
	 *     shader.vert: ApplyLighting and ComputeLight
	 *
	 * @param colors   the primary and secondary colors will be returned in this object
	 * @param pixel    the current pixel values. The following values are used:
	 *                 - material colors: materialAmbient, materialDiffuse, materialSpecular
	 *                 - normal (in eye coordinates): normalizedNe
	 *                 - vertex (in eye coordinates): Ve
	 */
	public final void applyLighting(PrimarySecondaryColors colors, PixelState pixel) {
		if (ambient == 0xFFFFFFFF && pixel.materialAmbient == 0xFFFFFFFF) {
			if (!someLightsEnabled || !separateSpecularColor || !hasNormal) {
				// A very simple case...
				colors.primaryColor = ambient;
				colors.secondaryColor = ZERO;
				return;
			}
		}

		int primary = materialEmission;
		int secondary = ZERO;

		Al = ambient;

		if (someLightsEnabled) {
			// Get the vector and normal in the eye coordinates
			// (only if they will be used by some light)
			if (hasSomeNonDirectionalLight) {
				pixel.getVe(Ve);
			}
			if (hasNormal) {
				pixel.getNormalizedNe(Ne);
			}

			Dl = ZERO;
			Sl = ZERO;

			for (int l = 0; l < NUM_LIGHTS; l++) {
				if (lightEnabled[l]) {
					computeLight(l);
				}
			}

			if (Dl != ZERO) {
				primary = addBGR(primary, multiplyBGR(Dl, pixel.materialDiffuse));
			}

			if (Sl != ZERO) {
				if (separateSpecularColor) {
					secondary = multiplyBGR(Sl, pixel.materialSpecular);
				} else {
					primary = addBGR(primary, multiplyBGR(Sl, pixel.materialSpecular));
				}
			}
		}

		if (Al != ZERO) {
			primary = addBGR(primary, multiplyBGR(Al, pixel.materialAmbient));
		}

		primary = setAlpha(primary, multiplyComponent(ambientAlpha, getAlpha(pixel.materialAmbient)));

		colors.primaryColor = primary;
		colors.secondaryColor = secondary;
	}
}
