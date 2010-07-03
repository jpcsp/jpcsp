/**
 * 
 */
package jpcsp.graphics;

import javax.media.opengl.GL;

enum Uniforms {
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
	
	int getId() {
		return uniformId;
	}
	
	void allocateId(GL gl, int shaderProgram) {
		uniformId = gl.glGetUniformLocation(shaderProgram, uniformString);
	}
}