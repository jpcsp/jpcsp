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

import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.StateProxy;

public enum Uniforms {
	zPos("psp_zPos"),
	zScale("psp_zScale"),
	lightingEnable("lightingEnable"),
	lightEnabled("psp_lightEnabled"),
	lightType("psp_lightType"),
	lightKind("psp_lightKind"),
	lightMode("colorAddition"),
	matFlags("psp_matFlags"),
	tex("tex"),
	texEnable("texEnable"),
	texEnvMode("texEnvMode"),
	texMapMode("texMapMode"),
	texMapProj("texMapProj"),
	texShade("texShade"),
	colorDoubling("colorDoubling"),
	ctestEnable("ctestEnable"),
	ctestFunc("ctestFunc"),
	ctestRef("ctestRef"),
	ctestMsk("ctestMsk"),
	boneMatrix("psp_boneMatrix"),
	weights("psp_weights"),
	numberBones("psp_numberBones"),
	vinfoColor("vinfoColor"),
	vinfoPosition("vinfoPosition"),
	vinfoTransform2D("vinfoTransform2D"),
	positionScale("positionScale"),
	normalScale("normalScale"),
	textureScale("textureScale"),
	weightScale("weightScale");

	String uniformString;
	int[] uniformId = new int[StateProxy.maxProgramId];

	Uniforms(String uniformString) {
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