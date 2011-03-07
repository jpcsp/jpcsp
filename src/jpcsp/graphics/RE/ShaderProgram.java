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

import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 */
public class ShaderProgram {
	private int programId = -1;
	private long key;
	private int shaderAttribPosition;
	private int shaderAttribNormal;
	private int shaderAttribColor;
	private int shaderAttribTexture;
	private int shaderAttribWeights1;
	private int shaderAttribWeights2;
	private boolean hasGeometryShader;
	private int[] lightType = new int[VideoEngine.NUM_LIGHTS]; // values: [0..2]
	private int[] lightKind = new int[VideoEngine.NUM_LIGHTS]; // values: [0..2]
	private int[] lightEnabled = new int[VideoEngine.NUM_LIGHTS]; // values: [0..1]
	private int[] matFlags = new int[3]; // values: [0..1]
	private int[] texShade = new int[2]; // values: [0..3]
	private int[] texEnvMode = new int[2]; // values: #0:[0..4], #1:[0..1]
	private int ctestFunc; // values: [0..3]
	private int texMapMode; // values: [0..2]
	private int texMapProj; // values: [0..3]
	private int vinfoColor; // values: [0..8]
	private int vinfoPosition; // values: [0..3]
	private float colorDoubling; // values: [1..2]
	private int texEnable; // values: [0..1]
	private int lightingEnable; // values: [0..1]
	private int vinfoTransform2D; // values: [0..1]
	private int ctestEnable; // values: [0..1]
	private int lightMode; // values: [0..1]
	private int texPixelFormat; // values: [0..14]
	private int numberBones; // values: [0..8]
	private int clutIndexHint; // values: [0..4]

	public ShaderProgram(IRenderingEngine re, int programId) {
		setProgramId(re, programId);
	}

	public ShaderProgram(ShaderContext shaderContext, boolean hasGeometryShader) {
		this.hasGeometryShader = hasGeometryShader;
		for (int i = 0; i < lightType.length; i++) {
			lightType[i] = shaderContext.getLightType(i);
			lightKind[i] = shaderContext.getLightKind(i);
			lightEnabled[i] = shaderContext.getLightEnabled(i);
		}
		matFlags[0] = shaderContext.getMatFlags(0);
		matFlags[1] = shaderContext.getMatFlags(1);
		matFlags[2] = shaderContext.getMatFlags(2);
		texShade[0] = shaderContext.getTexShade(0);
		texShade[1] = shaderContext.getTexShade(1);
		texEnvMode[0] = shaderContext.getTexEnvMode(0);
		texEnvMode[1] = shaderContext.getTexEnvMode(1);
		ctestFunc = shaderContext.getCtestFunc();
		texMapMode = shaderContext.getTexMapMode();
		texMapProj = shaderContext.getTexMapProj();
		vinfoColor = shaderContext.getVinfoColor();
		vinfoPosition = shaderContext.getVinfoPosition();
		colorDoubling = shaderContext.getColorDoubling();
		texEnable = shaderContext.getTexEnable();
		lightingEnable = shaderContext.getLightingEnable();
		vinfoTransform2D = shaderContext.getVinfoTransform2D();
		ctestEnable = shaderContext.getCtestEnable();
		lightMode = shaderContext.getLightMode();
		texPixelFormat = shaderContext.getTexPixelFormat();
		numberBones = shaderContext.getNumberBones();

		key = getKey(shaderContext, hasGeometryShader);
	}

	public static String getDummyDynamicDefines() {
		StringBuilder defines = new StringBuilder();
		int dummyValue = -1;

		for (int i = 0; i < VideoEngine.NUM_LIGHTS; i++) {
			REShader.addDefine(defines, "LIGHT_TYPE" + i, dummyValue);
			REShader.addDefine(defines, "LIGHT_KIND" + i, dummyValue);
			REShader.addDefine(defines, "LIGHT_ENABLED" + i, dummyValue);
		}
		REShader.addDefine(defines, "MAT_FLAGS0", dummyValue);
		REShader.addDefine(defines, "MAT_FLAGS1", dummyValue);
		REShader.addDefine(defines, "MAT_FLAGS2", dummyValue);
		REShader.addDefine(defines, "TEX_SHADE0", dummyValue);
		REShader.addDefine(defines, "TEX_SHADE1", dummyValue);
		REShader.addDefine(defines, "TEX_ENV_MODE0", dummyValue);
		REShader.addDefine(defines, "TEX_ENV_MODE1", dummyValue);
		REShader.addDefine(defines, "CTEST_FUNC", dummyValue);
		REShader.addDefine(defines, "TEX_MAP_MODE", dummyValue);
		REShader.addDefine(defines, "TEX_MAP_PROJ", dummyValue);
		REShader.addDefine(defines, "VINFO_COLOR", dummyValue);
		REShader.addDefine(defines, "VINFO_POSITION", dummyValue);
		REShader.addDefine(defines, "COLOR_DOUBLING", dummyValue);
		REShader.addDefine(defines, "TEX_ENABLE", dummyValue);
		REShader.addDefine(defines, "LIGHTING_ENABLE", dummyValue);
		REShader.addDefine(defines, "VINFO_TRANSFORM_2D", dummyValue);
		REShader.addDefine(defines, "CTEST_ENABLE", dummyValue);
		REShader.addDefine(defines, "LIGHT_MODE", dummyValue);
		REShader.addDefine(defines, "TEX_PIXEL_FORMAT", dummyValue);
		REShader.addDefine(defines, "NUMBER_BONES", dummyValue);
		REShader.addDefine(defines, "CLUT_INDEX_HINT", dummyValue);

		return defines.toString();
	}

	public String getDynamicDefines() {
		StringBuilder defines = new StringBuilder();

		for (int i = 0; i < lightType.length; i++) {
			REShader.addDefine(defines, "LIGHT_TYPE" + i, lightType[i]);
			REShader.addDefine(defines, "LIGHT_KIND" + i, lightKind[i]);
			REShader.addDefine(defines, "LIGHT_ENABLED" + i, lightEnabled[i]);
		}
		REShader.addDefine(defines, "MAT_FLAGS0", matFlags[0]);
		REShader.addDefine(defines, "MAT_FLAGS1", matFlags[1]);
		REShader.addDefine(defines, "MAT_FLAGS2", matFlags[2]);
		REShader.addDefine(defines, "TEX_SHADE0", texShade[0]);
		REShader.addDefine(defines, "TEX_SHADE1", texShade[1]);
		REShader.addDefine(defines, "TEX_ENV_MODE0", texEnvMode[0]);
		REShader.addDefine(defines, "TEX_ENV_MODE1", texEnvMode[1]);
		REShader.addDefine(defines, "CTEST_FUNC", ctestFunc);
		REShader.addDefine(defines, "TEX_MAP_MODE", texMapMode);
		REShader.addDefine(defines, "TEX_MAP_PROJ", texMapProj);
		REShader.addDefine(defines, "VINFO_COLOR", vinfoColor);
		REShader.addDefine(defines, "VINFO_POSITION", vinfoPosition);
		REShader.addDefine(defines, "COLOR_DOUBLING", (int) colorDoubling);
		REShader.addDefine(defines, "TEX_ENABLE", texEnable);
		REShader.addDefine(defines, "LIGHTING_ENABLE", lightingEnable);
		REShader.addDefine(defines, "VINFO_TRANSFORM_2D", vinfoTransform2D);
		REShader.addDefine(defines, "CTEST_ENABLE", ctestEnable);
		REShader.addDefine(defines, "LIGHT_MODE", lightMode);
		REShader.addDefine(defines, "TEX_PIXEL_FORMAT", texPixelFormat);
		REShader.addDefine(defines, "NUMBER_BONES", numberBones);
		REShader.addDefine(defines, "CLUT_INDEX_HINT", clutIndexHint);

		return defines.toString();
	}

	public static long getKey(ShaderContext shaderContext, boolean hasGeometryShader) {
		long key = 0;
		int shift = 0;

		key += hasGeometryShader ? 1 : 0;
		shift++;
		for (int i = 0; i < VideoEngine.NUM_LIGHTS; i++) {
			key += shaderContext.getLightType(i) << shift;
			shift += 2;
			key += shaderContext.getLightKind(i) << shift;
			shift += 2;
			key += shaderContext.getLightEnabled(i) << shift;
			shift++;
		}
		key += ((long) shaderContext.getMatFlags(0)) << shift;
		shift++;
		key += ((long) shaderContext.getMatFlags(1)) << shift;
		shift++;
		key += ((long) shaderContext.getMatFlags(2)) << shift;
		shift++;
		key += ((long) shaderContext.getTexShade(0)) << shift;
		shift += 2;
		key += ((long) shaderContext.getTexShade(1)) << shift;
		shift += 2;
		key += ((long) shaderContext.getTexEnvMode(0)) << shift;
		shift += 3;
		key += ((long) shaderContext.getTexEnvMode(1)) << shift;
		shift++;
		key += ((long) shaderContext.getCtestFunc()) << shift;
		shift += 2;
		key += ((long) shaderContext.getTexMapMode()) << shift;
		shift += 2;
		key += ((long) shaderContext.getTexMapProj()) << shift;
		shift += 2;
		key += ((long) shaderContext.getVinfoColor()) << shift;
		shift += 4;
		key += ((long) shaderContext.getVinfoPosition()) << shift;
		shift += 2;
		key += (shaderContext.getColorDoubling() == 2.f ? 1L : 0L) << shift;
		shift++;
		key += ((long) shaderContext.getTexEnable()) << shift;
		shift++;
		key += ((long) shaderContext.getLightingEnable()) << shift;
		shift++;
		key += ((long) shaderContext.getVinfoTransform2D()) << shift;
		shift++;
		key += ((long) shaderContext.getCtestEnable()) << shift;
		shift++;
		key += ((long) shaderContext.getLightMode()) << shift;
		shift++;
		key += ((long) shaderContext.getTexPixelFormat()) << shift;
		shift += 4;
		key += ((long) shaderContext.getNumberBones()) << shift;
		shift += 4;
		key += ((long) shaderContext.getClutIndexHint()) << shift;
		shift += 3;

		if (shift > Long.SIZE) {
			VideoEngine.log.error(String.format("ShaderProgram: too long key: %d bits", shift));
		}

		return key;
	}

	public boolean matches(ShaderContext shaderContext, boolean hasGeometryShader) {
		long key = getKey(shaderContext, hasGeometryShader);

		return key == this.key;
	}

	public void use(IRenderingEngine re) {
		re.useProgram(programId);
	}

	public int getProgramId() {
		return programId;
	}

	public void setProgramId(IRenderingEngine re, int programId) {
		this.programId = programId;

		shaderAttribWeights1 = re.getAttribLocation(programId, REShader.attributeNameWeights1);
        shaderAttribWeights2 = re.getAttribLocation(programId, REShader.attributeNameWeights2);
        shaderAttribPosition = re.getAttribLocation(programId, REShader.attributeNamePosition);
        shaderAttribNormal   = re.getAttribLocation(programId, REShader.attributeNameNormal);
        shaderAttribColor    = re.getAttribLocation(programId, REShader.attributeNameColor);
        shaderAttribTexture  = re.getAttribLocation(programId, REShader.attributeNameTexture);
	}

	public int getShaderAttribPosition() {
		return shaderAttribPosition;
	}

	public int getShaderAttribNormal() {
		return shaderAttribNormal;
	}

	public int getShaderAttribColor() {
		return shaderAttribColor;
	}

	public int getShaderAttribTexture() {
		return shaderAttribTexture;
	}

	public int getShaderAttribWeights1() {
		return shaderAttribWeights1;
	}

	public int getShaderAttribWeights2() {
		return shaderAttribWeights2;
	}

	public long getKey() {
		return key;
	}

	@Override
	public String toString() {
		return String.format("ShaderProgram[%d, geometryShader=%b, %s]", programId, hasGeometryShader, getDynamicDefines().replace(System.getProperty("line.separator"), ", "));
	}
}
