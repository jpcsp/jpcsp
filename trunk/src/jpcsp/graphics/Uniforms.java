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

import javax.media.opengl.GL;

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
	numberBones("psp_numberBones");

	String uniformString;
	int uniformId;
	
	Uniforms(String uniformString) {
		this.uniformString = uniformString;
	}
	
	public int getId() {
		return uniformId;
	}
	
	void allocateId(GL gl, int shaderProgram) {
		uniformId = gl.glGetUniformLocation(shaderProgram, uniformString);
	}
}