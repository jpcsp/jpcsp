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
	public boolean canNativeClut(int textureAddress) {
		// Shaders are required for native clut
		return false;
	}
}
