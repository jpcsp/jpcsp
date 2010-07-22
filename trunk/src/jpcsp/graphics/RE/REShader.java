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

import java.io.IOException;

import jpcsp.Settings;
import jpcsp.graphics.Uniforms;
import jpcsp.graphics.VideoEngine;
import jpcsp.util.Utilities;

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
public class REShader extends BaseRenderingEngineFunction {
	protected final static int NO_UNIFORM_ID = -1;
	protected final int[] flagToUniforms = new int[IRenderingEngine.RE_NUMBER_FLAGS];
	protected final int[] lightEnabled = new int[4];
	protected final int[] lightType = new int[4];
	protected final int[] lightKind = new int[4];
	protected final int[] matFlags = new int[3];
	protected final int[] invertedColorMask = new int[4];
	protected int shaderProgram;
	protected int vertexShader;
	protected int fragmentShader;
	protected int shaderAttribWeights1;
	protected int shaderAttribWeights2;

	public REShader(IRenderingEngine proxy) {
		super(proxy);

		init();
	}

	protected void init() {
        log.info("Using shaders with Skinning");

		loadShaders();

		for (Uniforms uniform : Uniforms.values()) {
            uniform.allocateId(re, shaderProgram);
        }

        shaderAttribWeights1 = re.getAttribLocation(shaderProgram, "psp_weights1");
        shaderAttribWeights2 = re.getAttribLocation(shaderProgram, "psp_weights2");

		for (int i = 0; i < flagToUniforms.length; i++) {
			flagToUniforms[i] = NO_UNIFORM_ID;
		}
        flagToUniforms[IRenderingEngine.GU_COLOR_TEST] = Uniforms.ctestEnable.getId();
		flagToUniforms[IRenderingEngine.GU_LIGHTING] = Uniforms.lightingEnable.getId();
		flagToUniforms[IRenderingEngine.GU_TEXTURE_2D] = Uniforms.texEnable.getId();
	}

	protected void loadShaders() {
        vertexShader = re.createShader(RE_VERTEX_SHADER);
        fragmentShader = re.createShader(RE_FRAGMENT_SHADER);

        String[] srcArray = new String[1];
        final String shaderVert = "/jpcsp/graphics/shader.vert";
        try {
            srcArray[0] = Utilities.toString(getClass().getResourceAsStream(shaderVert), true);
        } catch (IOException e) {
        	log.error(e);
        }
        re.compilerShader(vertexShader, srcArray);
        printShaderInfoLog(vertexShader);
        final String shaderFrag = "/jpcsp/graphics/shader.frag";
        try {
            srcArray[0] = Utilities.toString(getClass().getResourceAsStream(shaderFrag), true);
        } catch (IOException e) {
        	log.error(e);
        }
        re.compilerShader(fragmentShader, srcArray);
        printShaderInfoLog(fragmentShader);

        shaderProgram = re.createProgram();
        re.attachShader(shaderProgram, vertexShader);
        re.attachShader(shaderProgram, fragmentShader);
        re.linkProgram(shaderProgram);
        printProgramInfoLog(shaderProgram);
        re.validateProgram(shaderProgram);
        printProgramInfoLog(shaderProgram);

        re.useProgram(shaderProgram);

        // TODO Remove this call
        VideoEngine.getInstance().setShaderProgram(shaderProgram);
	}

	public static boolean useShaders(IRenderingEngine re) {
		boolean useShaders = Settings.getInstance().readBool("emu.useshaders")
		                     && re.isFunctionAvailable("glCreateShader")
		                     && re.isFunctionAvailable("glShaderSource")
		                     && re.isFunctionAvailable("glCompileShader")
		                     && re.isFunctionAvailable("glCreateProgram")
		                     && re.isFunctionAvailable("glAttachShader")
		                     && re.isFunctionAvailable("glLinkProgram")
		                     && re.isFunctionAvailable("glValidateProgram")
		                     && re.isFunctionAvailable("glUseProgram");

        return useShaders;
	}

	protected void printShaderInfoLog(int shader) {
		String infoLog = re.getShaderInfoLog(shader);
		if (infoLog != null) {
			log.error("Shader info log: " + infoLog);
		}
	}

    protected void printProgramInfoLog(int program) {
		String infoLog = re.getProgramInfoLog(program);
		if (infoLog != null) {
			log.error("Program info log: " + infoLog);
		}
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
		if (canUpdateFlag(flag)) {
			setShaderFlag(flag, 1);
			super.enableFlag(flag);
		}
	}

	@Override
	public void disableFlag(int flag) {
		if (canUpdateFlag(flag)) {
			setShaderFlag(flag, 0);
			super.disableFlag(flag);
		}
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

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
		invertedColorMask[0] = (~redMask  ) & 0xFF;
		invertedColorMask[1] = (~greenMask) & 0xFF;
		invertedColorMask[2] = (~blueMask ) & 0xFF;
		invertedColorMask[3] = (~alphaMask) & 0xFF;
		re.setUniform4(Uniforms.invertedColorMask.getId(), invertedColorMask);
		super.setColorMask(redMask, greenMask, blueMask, alphaMask);
	}
}
