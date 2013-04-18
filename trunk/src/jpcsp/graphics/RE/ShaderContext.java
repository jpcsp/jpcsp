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

import org.apache.log4j.Logger;

import jpcsp.graphics.Uniforms;
import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 * The current values for the Shader uniform variables.
 * The Shader uniform values have to be updated when switching the active
 * shader program.
 */
public class ShaderContext {
	protected static Logger log = VideoEngine.log;
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
	private int vinfoTexture;
	private int vinfoNormal;
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
	private int stencilTestEnable;
	private int stencilFunc;
	private int stencilRef;
	private int stencilMask;
	private int stencilOpFail;
	private int stencilOpZFail;
	private int stencilOpZPass;
	private int depthTestEnable;
	private int depthFunc;
	private int depthMask;
	private int fbTex = -1;
	private int colorMaskEnable;
	private int[] colorMask = new int[4];
	private int[] notColorMask = new int[4];
	private int alphaTestEnable;
	private int alphaTestFunc;
	private int alphaTestRef;
	private int blendTestEnable;
	private int blendEquation;
	private int blendSrc;
	private int blendDst;
	private float[] blendSFix = new float[3];
	private float[] blendDFix = new float[3];
	private int copyRedToAlpha;
	private int wrapModeS;
	private int wrapModeT;

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
		re.setUniform(Uniforms.vinfoTexture.getId(shaderProgram), vinfoTexture);
		re.setUniform(Uniforms.vinfoNormal.getId(shaderProgram), vinfoNormal);
		re.setUniform(Uniforms.stencilTestEnable.getId(shaderProgram), stencilTestEnable);
		re.setUniform(Uniforms.stencilFunc.getId(shaderProgram), stencilFunc);
		re.setUniform(Uniforms.stencilRef.getId(shaderProgram), stencilRef);
		re.setUniform(Uniforms.stencilMask.getId(shaderProgram), stencilMask);
		re.setUniform(Uniforms.stencilOpFail.getId(shaderProgram), stencilOpFail);
		re.setUniform(Uniforms.stencilOpZFail.getId(shaderProgram), stencilOpZFail);
		re.setUniform(Uniforms.stencilOpZPass.getId(shaderProgram), stencilOpZPass);
		re.setUniform(Uniforms.depthTestEnable.getId(shaderProgram), depthTestEnable);
		re.setUniform(Uniforms.depthFunc.getId(shaderProgram), depthFunc);
		re.setUniform(Uniforms.depthMask.getId(shaderProgram), depthMask);
		re.setUniform(Uniforms.colorMaskEnable.getId(shaderProgram), colorMaskEnable);
		re.setUniform4(Uniforms.colorMask.getId(shaderProgram), colorMask);
		re.setUniform4(Uniforms.notColorMask.getId(shaderProgram), notColorMask);
		re.setUniform(Uniforms.alphaTestEnable.getId(shaderProgram), alphaTestEnable);
		re.setUniform(Uniforms.alphaTestFunc.getId(shaderProgram), alphaTestFunc);
		re.setUniform(Uniforms.alphaTestRef.getId(shaderProgram), alphaTestRef);
		re.setUniform(Uniforms.blendTestEnable.getId(shaderProgram), blendTestEnable);
		re.setUniform(Uniforms.blendEquation.getId(shaderProgram), blendEquation);
		re.setUniform(Uniforms.blendSrc.getId(shaderProgram), blendSrc);
		re.setUniform(Uniforms.blendDst.getId(shaderProgram), blendDst);
		re.setUniform3(Uniforms.blendSFix.getId(shaderProgram), blendSFix);
		re.setUniform3(Uniforms.blendDFix.getId(shaderProgram), blendDFix);
		re.setUniform(Uniforms.copyRedToAlpha.getId(shaderProgram), copyRedToAlpha);
		re.setUniform(Uniforms.wrapModeS.getId(shaderProgram), wrapModeS);
		re.setUniform(Uniforms.wrapModeT.getId(shaderProgram), wrapModeT);

		setUniformsSamplers(re, shaderProgram);
	}

	protected void setUniformsSamplers(IRenderingEngine re, int shaderProgram) {
		re.setUniform(Uniforms.clut.getId(shaderProgram), clut);
		re.setUniform(Uniforms.tex.getId(shaderProgram), tex);
		re.setUniform(Uniforms.utex.getId(shaderProgram), utex);
		re.setUniform(Uniforms.fbTex.getId(shaderProgram), fbTex);
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

	public int getVinfoTexture() {
		return vinfoTexture;
	}

	public void setVinfoTexture(int vinfoTexture) {
		this.vinfoTexture = vinfoTexture;
	}

	public int getVinfoNormal() {
		return vinfoNormal;
	}

	public void setVinfoNormal(int vinfoNormal) {
		this.vinfoNormal = vinfoNormal;
	}

	public int getStencilTestEnable() {
		return stencilTestEnable;
	}

	public void setStencilTestEnable(int stencilTestEnable) {
		this.stencilTestEnable = stencilTestEnable;
	}

	public int getStencilFunc() {
		return stencilFunc;
	}

	public void setStencilFunc(int stencilFunc) {
		this.stencilFunc = stencilFunc;
	}

	public int getStencilRef() {
		return stencilRef;
	}

	public void setStencilRef(int stencilRef) {
		this.stencilRef = stencilRef;
	}

	public int getStencilMask() {
		return stencilMask;
	}

	public void setStencilMask(int stencilMask) {
		this.stencilMask = stencilMask;
	}

	public int getStencilOpFail() {
		return stencilOpFail;
	}

	public void setStencilOpFail(int stencilOpFail) {
		this.stencilOpFail = stencilOpFail;
	}

	public int getStencilOpZFail() {
		return stencilOpZFail;
	}

	public void setStencilOpZFail(int stencilOpZFail) {
		this.stencilOpZFail = stencilOpZFail;
	}

	public int getStencilOpZPass() {
		return stencilOpZPass;
	}

	public void setStencilOpZPass(int stencilOpZPass) {
		this.stencilOpZPass = stencilOpZPass;
	}
        
	public int getDepthTestEnable() {
		return depthTestEnable;
	}

	public void setDepthTestEnable(int depthTestEnable) {
		this.depthTestEnable = depthTestEnable;
	}

	public int getDepthFunc() {
		return depthFunc;
	}

	public void setDepthFunc(int depthFunc) {
		this.depthFunc = depthFunc;
	}

	public int getDepthMask() {
		return depthMask;
	}

	public void setDepthMask(int depthMask) {
		this.depthMask = depthMask;
	}

	public int getFbTex() {
		return fbTex;
	}

	public void setFbTex(int fbTex) {
		this.fbTex = fbTex;
	}

	public int getColorMaskEnable() {
		return colorMaskEnable;
	}

	public void setColorMaskEnable(int colorMaskEnable) {
		this.colorMaskEnable = colorMaskEnable;
	}

	public int[] getColorMask() {
		return colorMask;
	}

	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
		this.colorMask[0] = redMask;
		this.colorMask[1] = greenMask;
		this.colorMask[2] = blueMask;
		this.colorMask[3] = alphaMask;
	}

	public int[] getNotColorMask() {
		return notColorMask;
	}

	public void setNotColorMask(int notRedMask, int notGreenMask, int notBlueMask, int notAlphaMask) {
		this.notColorMask[0] = notRedMask;
		this.notColorMask[1] = notGreenMask;
		this.notColorMask[2] = notBlueMask;
		this.notColorMask[3] = notAlphaMask;
	}

	public int getAlphaTestEnable() {
		return alphaTestEnable;
	}

	public void setAlphaTestEnable(int alphaTestEnable) {
		this.alphaTestEnable = alphaTestEnable;
	}

	public int getAlphaTestFunc() {
		return alphaTestFunc;
	}

	public void setAlphaTestFunc(int alphaTestFunc) {
		this.alphaTestFunc = alphaTestFunc;
	}

	public int getAlphaTestRef() {
		return alphaTestRef;
	}

	public void setAlphaTestRef(int alphaTestRef) {
		this.alphaTestRef = alphaTestRef;
	}

	public int getBlendTestEnable() {
		return blendTestEnable;
	}

	public void setBlendTestEnable(int blendTestEnable) {
		this.blendTestEnable = blendTestEnable;
	}

	public int getBlendEquation() {
		return blendEquation;
	}

	public void setBlendEquation(int blendEquation) {
		this.blendEquation = blendEquation;
	}

	public int getBlendSrc() {
		return blendSrc;
	}

	public void setBlendSrc(int blendSrc) {
		this.blendSrc = blendSrc;
	}

	public int getBlendDst() {
		return blendDst;
	}

	public void setBlendDst(int blendDst) {
		this.blendDst = blendDst;
	}

	public float[] getBlendSFix() {
		return blendSFix;
	}

	public void setBlendSFix(float[] blendSFix) {
		this.blendSFix[0] = blendSFix[0];
		this.blendSFix[1] = blendSFix[1];
		this.blendSFix[2] = blendSFix[2];
	}

	public float[] getBlendDFix() {
		return blendDFix;
	}

	public void setBlendDFix(float[] blendDFix) {
		this.blendDFix[0] = blendDFix[0];
		this.blendDFix[1] = blendDFix[1];
		this.blendDFix[2] = blendDFix[2];
	}

	public int getCopyRedToAlpha() {
		return copyRedToAlpha;
	}

	public void setCopyRedToAlpha(int copyRedToAlpha) {
		this.copyRedToAlpha = copyRedToAlpha;
	}

	public int getWrapModeS() {
		return wrapModeS;
	}

	public void setWrapModeS(int wrapModeS) {
		this.wrapModeS = wrapModeS;
	}

	public int getWrapModeT() {
		return wrapModeT;
	}

	public void setWrapModeT(int wrapModeT) {
		this.wrapModeT = wrapModeT;
	}
}