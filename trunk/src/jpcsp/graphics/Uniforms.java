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
	matFlags("psp_matFlags"),
	tex("tex"),
	texEnable("texEnable"),
	texEnvMode("texEnvMode");
	
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