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

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.Uniforms;
import jpcsp.settings.Settings;

/**
 * @author gid15
 *
 * This RenderingEngine class implements the required logic
 * to use the OpenGL fixed-function pipeline, i.e. without shaders.
 * 
 * This class is implemented as a Proxy, forwarding the non-relevant calls
 * to the proxy.
 * 
 * When this class splits one call into multiple calls, they are sent to the
 * complete RenderingEngine pipeline, not just the proxy, taking advantage
 * of other RenderingEngines up in the pipeline (e.g. the proxy class removing
 * redundant calls).
 */
public class REFixedFunction extends BaseRenderingEngineFunction {
	private ShaderProgram stencilShaderProgram;

	public REFixedFunction(IRenderingEngine proxy) {
		super(proxy);
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
		if (colorDoubled || !alphaUsed) {
            // GL_RGB_SCALE is only used in OpenGL when GL_TEXTURE_ENV_MODE is GL_COMBINE
            // See http://www.opengl.org/sdk/docs/man/xhtml/glTexEnv.xml
            switch (func) {
            	case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_MODULATE:
                    // Cv = Cp * Cs
                    // Av = Ap * As
            		func = RE_TEXENV_COMBINE;
                    re.setTexEnv(RE_TEXENV_COMBINE_RGB, RE_TEXENV_MODULATE);
                    re.setTexEnv(RE_TEXENV_SRC0_RGB, RE_TEXENV_TEXTURE);
                    re.setTexEnv(RE_TEXENV_OPERAND0_RGB, RE_TEXENV_SRC_COLOR);
                    re.setTexEnv(RE_TEXENV_SRC1_RGB, RE_TEXENV_PREVIOUS);
                    re.setTexEnv(RE_TEXENV_OPERAND1_RGB, RE_TEXENV_SRC_COLOR);

                    if (alphaUsed) {
	                    re.setTexEnv(RE_TEXENV_COMBINE_ALPHA, RE_TEXENV_MODULATE);
	                    re.setTexEnv(RE_TEXENV_SRC0_ALPHA, RE_TEXENV_TEXTURE);
	                    re.setTexEnv(RE_TEXENV_OPERAND0_ALPHA, RE_TEXENV_SRC_ALPHA);
	                    re.setTexEnv(RE_TEXENV_SRC1_ALPHA, RE_TEXENV_PREVIOUS);
	                    re.setTexEnv(RE_TEXENV_OPERAND1_ALPHA, RE_TEXENV_SRC_ALPHA);
                    } else {
                        re.setTexEnv(RE_TEXENV_COMBINE_ALPHA, RE_TEXENV_REPLACE);
                        re.setTexEnv(RE_TEXENV_SRC0_ALPHA, RE_TEXENV_PREVIOUS);
                        re.setTexEnv(RE_TEXENV_OPERAND0_ALPHA, RE_TEXENV_SRC_ALPHA);
                    }
                    break;
                case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_DECAL:
                	func = RE_TEXENV_COMBINE;
                    // Cv = Cs * As + Cp * (1 - As)
                    // Av = Ap
                    if (!alphaUsed) {
                        // DECAL mode with ignored Alpha is always using
                        // the equivalent of Alpha = 1.0 on PSP.
                        // Simplified version when As == 1:
                        // Cv = Cs
                        re.setTexEnv(RE_TEXENV_COMBINE_RGB, RE_TEXENV_REPLACE);
                        re.setTexEnv(RE_TEXENV_SRC0_RGB, RE_TEXENV_TEXTURE);
                        re.setTexEnv(RE_TEXENV_OPERAND0_RGB, RE_TEXENV_SRC_COLOR);
                    } else {
                        re.setTexEnv(RE_TEXENV_COMBINE_RGB, RE_TEXENV_INTERPOLATE);
                        re.setTexEnv(RE_TEXENV_SRC0_RGB, RE_TEXENV_TEXTURE);
                        re.setTexEnv(RE_TEXENV_OPERAND0_RGB, RE_TEXENV_SRC_COLOR);
                        re.setTexEnv(RE_TEXENV_SRC1_RGB, RE_TEXENV_PREVIOUS);
                        re.setTexEnv(RE_TEXENV_OPERAND1_RGB, RE_TEXENV_SRC_COLOR);
                        re.setTexEnv(RE_TEXENV_SRC2_RGB, RE_TEXENV_TEXTURE);
                        re.setTexEnv(RE_TEXENV_OPERAND2_RGB, RE_TEXENV_SRC_ALPHA);
                    }

                    re.setTexEnv(RE_TEXENV_COMBINE_ALPHA, RE_TEXENV_REPLACE);
                    re.setTexEnv(RE_TEXENV_SRC0_ALPHA, RE_TEXENV_PREVIOUS);
                    re.setTexEnv(RE_TEXENV_OPERAND0_ALPHA, RE_TEXENV_SRC_ALPHA);
                    break;
                case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_BLEND:
                    // Cv = Cc * Cs + Cp * (1 - Cs)
                    // Av = As * Ap
                	func = RE_TEXENV_COMBINE;
                    re.setTexEnv(RE_TEXENV_COMBINE_RGB, RE_TEXENV_INTERPOLATE);
                    re.setTexEnv(RE_TEXENV_SRC0_RGB, RE_TEXENV_CONSTANT);
                    re.setTexEnv(RE_TEXENV_OPERAND0_RGB, RE_TEXENV_SRC_COLOR);
                    re.setTexEnv(RE_TEXENV_SRC1_RGB, RE_TEXENV_PREVIOUS);
                    re.setTexEnv(RE_TEXENV_OPERAND1_RGB, RE_TEXENV_SRC_COLOR);
                    re.setTexEnv(RE_TEXENV_SRC2_RGB, RE_TEXENV_TEXTURE);
                    re.setTexEnv(RE_TEXENV_OPERAND2_RGB, RE_TEXENV_SRC_COLOR);

                    if (alphaUsed) {
	                    re.setTexEnv(RE_TEXENV_COMBINE_ALPHA, RE_TEXENV_MODULATE);
	                    re.setTexEnv(RE_TEXENV_SRC0_ALPHA, RE_TEXENV_TEXTURE);
	                    re.setTexEnv(RE_TEXENV_OPERAND0_ALPHA, RE_TEXENV_SRC_ALPHA);
	                    re.setTexEnv(RE_TEXENV_SRC1_ALPHA, RE_TEXENV_PREVIOUS);
	                    re.setTexEnv(RE_TEXENV_OPERAND1_ALPHA, RE_TEXENV_SRC_ALPHA);
                    } else {
                        re.setTexEnv(RE_TEXENV_COMBINE_ALPHA, RE_TEXENV_REPLACE);
                        re.setTexEnv(RE_TEXENV_SRC0_ALPHA, RE_TEXENV_PREVIOUS);
                        re.setTexEnv(RE_TEXENV_OPERAND0_ALPHA, RE_TEXENV_SRC_ALPHA);
                    }
                    break;
                case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_REPLACE:
                    // Cv = Cs
                    // Av = As
                	func = RE_TEXENV_COMBINE;
                    re.setTexEnv(RE_TEXENV_COMBINE_RGB, RE_TEXENV_REPLACE);
                    re.setTexEnv(RE_TEXENV_SRC0_RGB, RE_TEXENV_TEXTURE);
                    re.setTexEnv(RE_TEXENV_OPERAND0_RGB, RE_TEXENV_SRC_COLOR);

                    if (alphaUsed) {
	                    re.setTexEnv(RE_TEXENV_COMBINE_ALPHA, RE_TEXENV_REPLACE);
	                    re.setTexEnv(RE_TEXENV_SRC0_ALPHA, RE_TEXENV_TEXTURE);
	                    re.setTexEnv(RE_TEXENV_OPERAND0_ALPHA, RE_TEXENV_SRC_ALPHA);
                    } else {
                        re.setTexEnv(RE_TEXENV_COMBINE_ALPHA, RE_TEXENV_REPLACE);
                        re.setTexEnv(RE_TEXENV_SRC0_ALPHA, RE_TEXENV_PREVIOUS);
                        re.setTexEnv(RE_TEXENV_OPERAND0_ALPHA, RE_TEXENV_SRC_ALPHA);
                    }
                    break;
                case GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_EFECT_ADD:
                    // Cv = Cp + Cs
                    // Av = Ap * As
                	func = RE_TEXENV_COMBINE;
                    re.setTexEnv(RE_TEXENV_COMBINE_RGB, RE_TEXENV_ADD);
                    re.setTexEnv(RE_TEXENV_SRC0_RGB, RE_TEXENV_TEXTURE);
                    re.setTexEnv(RE_TEXENV_OPERAND0_RGB, RE_TEXENV_SRC_COLOR);
                    re.setTexEnv(RE_TEXENV_SRC1_RGB, RE_TEXENV_PREVIOUS);
                    re.setTexEnv(RE_TEXENV_OPERAND1_RGB, RE_TEXENV_SRC_COLOR);

                    if (alphaUsed) {
	                    re.setTexEnv(RE_TEXENV_COMBINE_ALPHA, RE_TEXENV_MODULATE);
	                    re.setTexEnv(RE_TEXENV_SRC0_ALPHA, RE_TEXENV_TEXTURE);
	                    re.setTexEnv(RE_TEXENV_OPERAND0_ALPHA, RE_TEXENV_SRC_ALPHA);
	                    re.setTexEnv(RE_TEXENV_SRC1_ALPHA, RE_TEXENV_PREVIOUS);
	                    re.setTexEnv(RE_TEXENV_OPERAND1_ALPHA, RE_TEXENV_SRC_ALPHA);
                    } else {
                        re.setTexEnv(RE_TEXENV_COMBINE_ALPHA, RE_TEXENV_REPLACE);
                        re.setTexEnv(RE_TEXENV_SRC0_ALPHA, RE_TEXENV_PREVIOUS);
                        re.setTexEnv(RE_TEXENV_OPERAND0_ALPHA, RE_TEXENV_SRC_ALPHA);
                    }
                    break;
            }
        }

		super.setTextureFunc(func, alphaUsed, colorDoubled);
	}

	@Override
	public void disableFlag(int flag) {
		if (canUpdateFlag(flag)) {
			super.disableFlag(flag);
		}
	}

	@Override
	public void enableFlag(int flag) {
		if (canUpdateFlag(flag)) {
			super.enableFlag(flag);
		}
	}

	@Override
	public void enableVertexAttribArray(int id) {
		// This call is used only by Shader
	}

	@Override
	public void disableVertexAttribArray(int id) {
		// This call is used only by Shader
	}

	@Override
	public boolean canNativeClut(int textureAddress, boolean textureSwizzle) {
		// Shaders are required for native clut
		return false;
	}

	@Override
	public boolean setCopyRedToAlpha(boolean copyRedToAlpha) {
		if (copyRedToAlpha) {
			// The stencil index is now available in the red channel of the stencil texture.
			// We need to copy it to the alpha channel of the GE texture.
			// As I've not found how to perform this copy from one channel into another channel
			// using the OpenGL fixed pipeline functionality,
			// we use a small fragment shader program.
			if (stencilShaderProgram == null) {
				if (!re.isShaderAvailable()) {
					log.info("Shaders are not available on your computer. They are required to save stencil information into the GE texture. Saving of the stencil information has been disabled.");
					return false;
				}
	
				// The fragment shader is just copying the stencil texture red channel to the GE texture alpha channel
				String fragmentShaderSource =
						"uniform sampler2D tex;" +
						"void main() {" +
						"    gl_FragColor.a = texture2DProj(tex, gl_TexCoord[0].xyz).r;" +
						"}";
				int shaderId = re.createShader(IRenderingEngine.RE_FRAGMENT_SHADER);
				boolean compiled = re.compilerShader(shaderId, fragmentShaderSource);
				if (!compiled) {
					log.error(String.format("Cannot compile shader required for storing stencil information into the GE texture: %s", re.getShaderInfoLog(shaderId)));
					return false;
				}
	
				int stencilShaderProgramId = re.createProgram();
				re.attachShader(stencilShaderProgramId, shaderId);
				boolean linked = re.linkProgram(stencilShaderProgramId);
				if (!linked) {
					log.error(String.format("Cannot link shader required for storing stencil information into the GE texture: %s", re.getProgramInfoLog(stencilShaderProgramId)));
					return false;
				}
	
				Uniforms.tex.allocateId(re, stencilShaderProgramId);
	
				stencilShaderProgram = new ShaderProgram();
				stencilShaderProgram.setProgramId(re, stencilShaderProgramId);
	
				if (!Settings.getInstance().readBool("emu.useshaders")) {
					log.info("Shaders are disabled in the Jpcsp video settings. However a small shader program is required to implement the saving of the Stencil information into the GE texture. This small shader program will still be used even though the shaders are disabled in the settings. This was just for your information, you do not need to take special actions.");
				}
			}
	
			stencilShaderProgram.use(re);
			re.setUniform(Uniforms.tex.getId(stencilShaderProgram.getProgramId()), REShader.ACTIVE_TEXTURE_NORMAL);
			re.checkAndLogErrors("setUniform");
		} else {
			// Disable the shader program
			re.useProgram(0);
		}

		return super.setCopyRedToAlpha(copyRedToAlpha);
	}
}
