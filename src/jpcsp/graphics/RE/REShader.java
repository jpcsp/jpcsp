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
 * This RenderingEngine class implements the required logic
 * to use OpenGL vertex and fragment shaders, i.e. without using
 * the OpenGL fixed-function pipeline.
 * 
 * This class is implemented as a Proxy, forwarding the non-relevant calls
 * to the proxy.
 */
public class REShader extends BaseRenderingEngineProxy {
	protected final static int NO_UNIFORM_ID = -1;
	protected final int[] flagToUniforms = new int[IRenderingEngine.RE_NUMBER_FLAGS];
	protected final int[] lightEnabled = new int[4];
	protected final int[] lightType = new int[4];
	protected final int[] lightKind = new int[4];
	protected final int[] matFlags = new int[3];

	public REShader(IRenderingEngine proxy) {
		super(proxy);

		init();
	}

	protected void init() {
		for (int i = 0; i < flagToUniforms.length; i++) {
			flagToUniforms[i] = NO_UNIFORM_ID;
		}

		flagToUniforms[IRenderingEngine.GU_COLOR_TEST] = Uniforms.ctestEnable.getId();
		flagToUniforms[IRenderingEngine.GU_LIGHTING] = Uniforms.lightingEnable.getId();
		flagToUniforms[IRenderingEngine.GU_TEXTURE_2D] = Uniforms.texEnable.getId();
	}

	protected void setShaderFlag(int flag, int value) {
		if (flag >= IRenderingEngine.GU_LIGHT0 && flag <= IRenderingEngine.GU_LIGHT3) {
			lightEnabled[flag - IRenderingEngine.GU_LIGHT0] = value;
			re.setUniform4(Uniforms.lightEnabled.getId(), lightEnabled);
		} else if (flagToUniforms[flag] != NO_UNIFORM_ID) {
			re.setUniform(flagToUniforms[flag], value);
		}
	}

	@Override
	public void enableFlag(int flag) {
		setShaderFlag(flag, 1);
		super.enableFlag(flag);
	}

	@Override
	public void disableFlag(int flag) {
		setShaderFlag(flag, 0);
		super.disableFlag(flag);
	}

	@Override
	public void setDepthRange(float zpos, float zscale, float near, float far) {
		re.setUniform(Uniforms.zPos.getId(), zpos);
		re.setUniform(Uniforms.zScale.getId(), zscale);
		super.setDepthRange(zpos, zscale, near, far);
	}

	@Override
	public void setLightMode(int mode) {
		re.setUniform(Uniforms.lightMode.getId(), mode);
		super.setLightMode(mode);
	}

	@Override
	public void setLightType(int light, int type, int kind) {
		lightType[light] = type;
		lightKind[light] = kind;
		re.setUniform4(Uniforms.lightType.getId(), lightType);
		re.setUniform4(Uniforms.lightKind.getId(), lightKind);
		super.setLightType(light, type, kind);
	}

	@Override
	public void setTextureEnvironmentMapping(int u, int v) {
		re.setUniform(Uniforms.texShade.getId(), u, v);
		super.setTextureEnvironmentMapping(u, v);
	}

	@Override
	public void setColorTestFunc(int func) {
		re.setUniform(Uniforms.ctestFunc.getId(), func);
		super.setColorTestFunc(func);
	}

	@Override
	public void setColorTestMask(int[] values) {
		re.setUniform3(Uniforms.ctestMsk.getId(), values);
		super.setColorTestMask(values);
	}

	@Override
	public void setColorTestReference(int[] values) {
		re.setUniform3(Uniforms.ctestRef.getId(), values);
		super.setColorTestReference(values);
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
		re.setUniform(Uniforms.texEnvMode.getId(), func, alphaUsed ? 1 : 0);
		re.setUniform(Uniforms.colorDoubling.getId(), colorDoubled ? 2.0f : 1.0f);
		super.setTextureFunc(func, alphaUsed, colorDoubled);
	}

	@Override
	public void setBones(int count, float[] values) {
		re.setUniform(Uniforms.numberBones.getId(), count);
		if (count > 0) {
			re.setUniformMatrix4(Uniforms.boneMatrix.getId(), count, values);
		}
		super.setBones(count, values);
	}

	@Override
	public void setTextureMapMode(int mode, int proj) {
		re.setUniform(Uniforms.texMapMode.getId(), mode);
		re.setUniform(Uniforms.texMapProj.getId(), proj);
		super.setTextureMapMode(mode, proj);
	}

	@Override
	public void setColorMaterial(boolean ambient, boolean diffuse, boolean specular) {
		matFlags[0] = ambient  ? 1 : 0;
		matFlags[1] = diffuse  ? 1 : 0;
		matFlags[2] = specular ? 1 : 0;
		re.setUniform3(Uniforms.matFlags.getId(), matFlags);
		super.setColorMaterial(ambient, diffuse, specular);
	}
}
