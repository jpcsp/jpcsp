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
 *
 * TODO Use a Uniform Buffer Object (UBO) to allow a faster shader program switch.
 */
public class ShaderContext {
	public float zPos;
	public float zScale;
	public int[] matFlags = new int[3];
	public int lightingEnable;
	public int lightMode;
	public int[] lightType = new int[4];
	public int[] lightKind = new int[4];
	public int[] lightEnabled = new int[4];
	public float[] boneMatrix = new float[8 * 16];
	public int numberBones;
	public int texEnable;
	public int texMapMode;
	public int texMapProj;
	public int[] texShade = new int[2];
	public int ctestEnable;
	public int ctestFunc;
	public int[] ctestMsk = new int[3];
	public int[] ctestRef = new int[3];
	public int[] texEnvMode = new int[2];
	public float colorDoubling;
	public int vinfoColor;
	public int vinfoPosition;
	public int vinfoTransform2D;
	public float positionScale;
	public float normalScale;
	public float textureScale;
	public float weightScale;

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
	}
}
