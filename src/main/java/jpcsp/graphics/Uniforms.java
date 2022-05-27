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

import java.util.Arrays;

import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.StateProxy;

public enum Uniforms {
	lightingEnable("lightingEnable"),
	lightEnabled("pspLightEnabled"),
	lightType("pspLightType"),
	lightKind("pspLightKind"),
	lightPosition("pspLightPosition"),
	lightDirection("pspLightDirection"),
	lightAmbientColor("pspLightAmbientColor"),
	lightDiffuseColor("pspLightDiffuseColor"),
	lightSpecularColor("pspLightSpecularColor"),
	lightSpotLightExponent("pspLightSpotLightExponent"),
	lightSpotLightCutoff("pspLightSpotLightCutoff"),
	lightAttenuation("pspLightAttenuation"),
	lightMode("colorAddition"),
	matFlags("pspMatFlags"),
	tex("tex"),
	texEnable("texEnable"),
	texEnvMode("texEnvMode"),
	texEnvColor("texEnvColor"),
	texMapMode("texMapMode"),
	texMapProj("texMapProj"),
	texShade("texShade"),
	colorDoubling("colorDoubling"),
	ctestEnable("ctestEnable"),
	ctestFunc("ctestFunc"),
	ctestRef("ctestRef"),
	ctestMsk("ctestMsk"),
	boneMatrix("pspBoneMatrix"),
	weights("pspWeights"),
	numberBones("pspNumberBones"),
	vinfoColor("vinfoColor"),
	vinfoPosition("vinfoPosition"),
	vinfoTransform2D("vinfoTransform2D"),
	positionScale("positionScale"),
	normalScale("normalScale"),
	textureScale("textureScale"),
	weightScale("weightScale"),
	clutShift("clutShift"),
	clutMask("clutMask"),
	clutOffset("clutOffset"),
	clut("clut"),
	mipmapShareClut("mipmapShareClut"),
	texPixelFormat("texPixelFormat"),
	utex("utex"),
	endOfUBO("endOfUBO"),
	vertexColor("vertexColor"),
	vinfoTexture("vinfoTexture"),
	vinfoNormal("vinfoNormal"),
	stencilTestEnable("stencilTestEnable"),
	stencilFunc("stencilFunc"),
	stencilRef("stencilRef"),
	stencilMask("stencilMask"),
	stencilOpFail("stencilOpFail"),
	stencilOpZFail("stencilOpZFail"),
	stencilOpZPass("stencilOpZPass"),
	depthTestEnable("depthTestEnable"),
	depthFunc("depthFunc"),
	depthWriteEnabled("depthWriteEnabled"),
	fbTex("fbTex"),
	depthTex("depthTex"),
	colorMaskEnable("colorMaskEnable"),
	colorMask("colorMask"),
	notColorMask("notColorMask"),
	alphaTestEnable("alphaTestEnable"),
	alphaTestFunc("alphaTestFunc"),
	alphaTestRef("alphaTestRef"),
	alphaTestMask("alphaTestMask"),
	blendTestEnable("blendTestEnable"),
	blendEquation("blendEquation"),
	blendSrc("blendSrc"),
	blendDst("blendDst"),
	blendSFix("blendSFix"),
	blendDFix("blendDFix"),
	copyRedToAlpha("copyRedToAlpha"),
	wrapModeS("wrapModeS"),
	wrapModeT("wrapModeT"),
	fogEnable("fogEnable"),
	fogColor("fogColor"),
	fogEnd("fogEnd"),
	fogScale("fogScale"),
	fogDepth("fogDepth"),
	clipPlaneEnable("clipPlaneEnable"),
	viewportPos("viewportPos"),
	viewportScale("viewportScale"),
	shadeModel("shadeModel"),
	ambientLightColor("ambientLightColor"),
	materialShininess("materialShininess"),
	materialAmbientColor("materialAmbientColor"),
	materialDiffuseColor("materialDiffuseColor"),
	materialSpecularColor("materialSpecularColor"),
	materialEmissionColor("materialEmissionColor"),
	pspTextureMatrix("pspTextureMatrix"),
	modelMatrix("modelMatrix"),
	modelViewMatrix("modelViewMatrix"),
	modelViewProjectionMatrix("modelViewProjectionMatrix"),
	curvedSurfaceType("curvedSurfaceType"),
	splineInfo("splineInfo"),
	patchFace("patchFace");

	String uniformString;
	int[] uniformId = new int[StateProxy.maxProgramId];

	private Uniforms(String uniformString) {
		Arrays.fill(uniformId, -1);
		this.uniformString = uniformString;
	}

	public int getId(int shaderProgram) {
		return uniformId[shaderProgram];
	}

	public String getUniformString() {
		return uniformString;
	}

	public void allocateId(IRenderingEngine re, int shaderProgram) {
		uniformId[shaderProgram] = re.getUniformLocation(shaderProgram, uniformString);
	}
}