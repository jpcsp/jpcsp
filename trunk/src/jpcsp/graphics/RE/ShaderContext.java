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

import jpcsp.graphics.Uniforms;

/**
 * @author gid15
 *
 * The current values for the Shader uniform variables.
 * The Shader uniform values have to be updated when switching the active
 * shader program.
 */
public class ShaderContext {
	private float zPos;
	private float zScale;
	private int[] matFlags = new int[3];
	private int lightingEnable;
	private int lightMode;
	private int[] lightType = new int[4];
	private int[] lightKind = new int[4];
	private int[] lightEnabled = new int[4];
	private float[] boneMatrix = new float[8 * 16];
	private int numberBones;
	private int texEnable;
	private int texMapMode;
	private int texMapProj;
	private int[] texShade = new int[2];
	private int ctestEnable;
	private int ctestFunc;
	private int[] ctestMsk = new int[3];
	private int[] ctestRef = new int[3];
	private int[] texEnvMode = new int[2];
	private float colorDoubling;
	private int vinfoColor;
	private int vinfoPosition;
	private int vinfoTransform2D;
	private float positionScale;
	private float normalScale;
	private float textureScale;
	private float weightScale;
	private int clutShift;
	private int clutMask;
	private int clutOffset;
	private boolean mipmapShareClut;
	private int clut = -1;
	private int texPixelFormat;
	private int tex = 0;
	private int utex = -1;
	private float[] vertexColor = new float[4];
	private int clutIndexHint;

	public void setUniforms(IRenderingEngine re, int shaderProgram) {
		re.setUniform(Uniforms.zPos.getId(shaderProgram), zPos);
		re.setUniform(Uniforms.zScale.getId(shaderProgram), zScale);
		re.setUniform3(Uniforms.matFlags.getId(shaderProgram), matFlags);
		re.setUniform4(Uniforms.lightEnabled.getId(shaderProgram), lightEnabled);
		re.setUniform(Uniforms.lightMode.getId(shaderProgram), lightMode);
		re.setUniform4(Uniforms.lightType.getId(shaderProgram), lightType);
		re.setUniform4(Uniforms.lightKind.getId(shaderProgram), lightKind);
		re.setUniform(Uniforms.lightingEnable.getId(shaderProgram), lightingEnable);
		re.setUniformMatrix4(Uniforms.boneMatrix.getId(shaderProgram), numberBones, boneMatrix);
		re.setUniform(Uniforms.numberBones.getId(shaderProgram), numberBones);
		re.setUniform(Uniforms.texEnable.getId(shaderProgram), texEnable);
		re.setUniform(Uniforms.texMapMode.getId(shaderProgram), texMapMode);
		re.setUniform(Uniforms.texMapProj.getId(shaderProgram), texMapProj);
		re.setUniform2(Uniforms.texShade.getId(shaderProgram), texShade);
		re.setUniform(Uniforms.ctestEnable.getId(shaderProgram), ctestEnable);
		re.setUniform(Uniforms.ctestFunc.getId(shaderProgram), ctestFunc);
		re.setUniform3(Uniforms.ctestMsk.getId(shaderProgram), ctestMsk);
		re.setUniform3(Uniforms.ctestRef.getId(shaderProgram), ctestRef);
		re.setUniform2(Uniforms.texEnvMode.getId(shaderProgram), texEnvMode);
		re.setUniform(Uniforms.colorDoubling.getId(shaderProgram), colorDoubling);
		re.setUniform(Uniforms.vinfoColor.getId(shaderProgram), vinfoColor);
		re.setUniform(Uniforms.vinfoPosition.getId(shaderProgram), vinfoPosition);
		re.setUniform(Uniforms.vinfoTransform2D.getId(shaderProgram), vinfoTransform2D);
		re.setUniform(Uniforms.positionScale.getId(shaderProgram), positionScale);
		re.setUniform(Uniforms.normalScale.getId(shaderProgram), normalScale);
		re.setUniform(Uniforms.textureScale.getId(shaderProgram), textureScale);
		re.setUniform(Uniforms.weightScale.getId(shaderProgram), weightScale);
		re.setUniform(Uniforms.clutShift.getId(shaderProgram), clutShift);
		re.setUniform(Uniforms.clutMask.getId(shaderProgram), clutMask);
		re.setUniform(Uniforms.clutOffset.getId(shaderProgram), clutOffset);
		re.setUniform(Uniforms.mipmapShareClut.getId(shaderProgram), mipmapShareClut ? 1 : 0);
		re.setUniform(Uniforms.texPixelFormat.getId(shaderProgram), texPixelFormat);
		re.setUniform4(Uniforms.vertexColor.getId(shaderProgram), vertexColor);
	}

	protected void setUniformsSamplers(IRenderingEngine re, int shaderProgram) {
		re.setUniform(Uniforms.clut.getId(shaderProgram), clut);
		re.setUniform(Uniforms.tex.getId(shaderProgram), tex);
		re.setUniform(Uniforms.utex.getId(shaderProgram), utex);
	}

	public void initShaderProgram(IRenderingEngine re, int shaderProgram) {
		// Nothing to do here
	}

	public float getZPos() {
		return zPos;
	}

	public void setZPos(float pos) {
		zPos = pos;
	}

	public float getZScale() {
		return zScale;
	}

	public void setZScale(float scale) {
		zScale = scale;
	}

	public int getMatFlags(int index) {
		return matFlags[index];
	}

	public void setMatFlags(int index, int matFlags) {
		this.matFlags[index] = matFlags;
	}

	public int getLightingEnable() {
		return lightingEnable;
	}

	public void setLightingEnable(int lightingEnable) {
		this.lightingEnable = lightingEnable;
	}

	public int getLightMode() {
		return lightMode;
	}

	public void setLightMode(int lightMode) {
		this.lightMode = lightMode;
	}

	public int getLightType(int light) {
		return lightType[light];
	}

	public void setLightType(int light, int lightType) {
		this.lightType[light] = lightType;
	}

	public int getLightKind(int light) {
		return lightKind[light];
	}

	public void setLightKind(int light, int lightKind) {
		this.lightKind[light] = lightKind;
	}

	public int getLightEnabled(int light) {
		return lightEnabled[light];
	}

	public void setLightEnabled(int light, int lightEnabled) {
		this.lightEnabled[light] = lightEnabled;
	}

	public int getBoneMatrixLength() {
		return boneMatrix.length;
	}

	public float[] getBoneMatrix() {
		return boneMatrix;
	}

	public void setBoneMatrix(int count, float[] boneMatrix) {
		if (count > 0) {
			System.arraycopy(boneMatrix, 0, this.boneMatrix, 0, 16 * count);
		}
	}

	public int getNumberBones() {
		return numberBones;
	}

	public void setNumberBones(int numberBones) {
		this.numberBones = numberBones;
	}

	public int getTexEnable() {
		return texEnable;
	}

	public void setTexEnable(int texEnable) {
		this.texEnable = texEnable;
	}

	public int getTexMapMode() {
		return texMapMode;
	}

	public void setTexMapMode(int texMapMode) {
		this.texMapMode = texMapMode;
	}

	public int getTexMapProj() {
		return texMapProj;
	}

	public void setTexMapProj(int texMapProj) {
		this.texMapProj = texMapProj;
	}

	public int getTexShade(int index) {
		return texShade[index];
	}

	public void setTexShade(int index, int texShade) {
		this.texShade[index] = texShade;
	}

	public int getCtestEnable() {
		return ctestEnable;
	}

	public void setCtestEnable(int ctestEnable) {
		this.ctestEnable = ctestEnable;
	}

	public int getCtestFunc() {
		return ctestFunc;
	}

	public void setCtestFunc(int ctestFunc) {
		this.ctestFunc = ctestFunc;
	}

	public int getCtestMsk(int index) {
		return ctestMsk[index];
	}

	public void setCtestMsk(int index, int ctestMsk) {
		this.ctestMsk[index] = ctestMsk;
	}

	public int getCtestRef(int index) {
		return ctestRef[index];
	}

	public void setCtestRef(int index, int ctestRef) {
		this.ctestRef[index] = ctestRef;
	}

	public int getTexEnvMode(int index) {
		return texEnvMode[index];
	}

	public void setTexEnvMode(int index, int texEnvMode) {
		this.texEnvMode[index] = texEnvMode;
	}

	public float getColorDoubling() {
		return colorDoubling;
	}

	public void setColorDoubling(float colorDoubling) {
		this.colorDoubling = colorDoubling;
	}

	public int getVinfoColor() {
		return vinfoColor;
	}

	public void setVinfoColor(int vinfoColor) {
		this.vinfoColor = vinfoColor;
	}

	public int getVinfoPosition() {
		return vinfoPosition;
	}

	public void setVinfoPosition(int vinfoPosition) {
		this.vinfoPosition = vinfoPosition;
	}

	public int getVinfoTransform2D() {
		return vinfoTransform2D;
	}

	public void setVinfoTransform2D(int vinfoTransform2D) {
		this.vinfoTransform2D = vinfoTransform2D;
	}

	public float getPositionScale() {
		return positionScale;
	}

	public void setPositionScale(float positionScale) {
		this.positionScale = positionScale;
	}

	public float getNormalScale() {
		return normalScale;
	}

	public void setNormalScale(float normalScale) {
		this.normalScale = normalScale;
	}

	public float getTextureScale() {
		return textureScale;
	}

	public void setTextureScale(float textureScale) {
		this.textureScale = textureScale;
	}

	public float getWeightScale() {
		return weightScale;
	}

	public void setWeightScale(float weightScale) {
		this.weightScale = weightScale;
	}

	public int getClutShift() {
		return clutShift;
	}

	public void setClutShift(int clutShift) {
		this.clutShift = clutShift;
	}

	public int getClutMask() {
		return clutMask;
	}

	public void setClutMask(int clutMask) {
		this.clutMask = clutMask;
	}

	public int getClutOffset() {
		return clutOffset;
	}

	public void setClutOffset(int clutOffset) {
		this.clutOffset = clutOffset;
	}

	public boolean isMipmapShareClut() {
		return mipmapShareClut;
	}

	public void setMipmapShareClut(boolean mipmapShareClut) {
		this.mipmapShareClut = mipmapShareClut;
	}

	public int getClut() {
		return clut;
	}

	public void setClut(int clut) {
		this.clut = clut;
	}

	public int getTexPixelFormat() {
		return texPixelFormat;
	}

	public void setTexPixelFormat(int texPixelFormat) {
		this.texPixelFormat = texPixelFormat;
	}

	public int getTex() {
		return tex;
	}

	public void setTex(int tex) {
		this.tex = tex;
	}

	public int getUtex() {
		return utex;
	}

	public void setUtex(int utex) {
		this.utex = utex;
	}

	public float[] getVertexColor() {
		return vertexColor;
	}

	public void setVertexColor(float[] vertexColor) {
		this.vertexColor[0] = vertexColor[0];
		this.vertexColor[1] = vertexColor[1];
		this.vertexColor[2] = vertexColor[2];
		this.vertexColor[3] = vertexColor[3];
	}

	public int getClutIndexHint() {
		return clutIndexHint;
	}

	public void setClutIndexHint(int clutIndexHint) {
		this.clutIndexHint = clutIndexHint;
	}
}
