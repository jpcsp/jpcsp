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
import jpcsp.graphics.GeContext.EnableDisableFlag;

/**
 * @author gid15
 *
 * Base class the RenderingEngine providing basic functionalities:
 * - generic clear mode handling: implementation matching the PSP behavior,
 *           but probably not the most efficient implementation.
 */
public class BaseRenderingEngineFunction extends BaseRenderingEngineProxy {
	protected boolean clearMode;
	ClearModeContext clearModeContext = new ClearModeContext();
	protected static final boolean[] flagsValidInClearMode = new boolean[] {
		false, // GU_ALPHA_TEST
		false, // GU_DEPTH_TEST
		true,  // GU_SCISSOR_TEST
		false, // GU_STENCIL_TEST
		false, // GU_BLEND
		false, // GU_CULL_FACE
		true,  // GU_DITHER
		false, // GU_FOG
		true,  // GU_CLIP_PLANES
		false, // GU_TEXTURE_2D
		true,  // GU_LIGHTING
		true,  // GU_LIGHT0
		true,  // GU_LIGHT1
		true,  // GU_LIGHT2
		true,  // GU_LIGHT3
		true,  // GU_LINE_SMOOTH
		true,  // GU_PATCH_CULL_FACE
		false, // GU_COLOR_TEST
		false, // GU_COLOR_LOGIC_OP
		true,  // GU_FACE_NORMAL_REVERSE
		true,  // GU_PATCH_FACE
		true,  // GU_FRAGMENT_2X
		true,  // RE_COLOR_MATERIAL
		true,  // RE_TEXTURE_GEN_S
		true,  // RE_TEXTURE_GEN_T
	};
	protected static class ClearModeContext {
		public int depthFunc;
		public int textureFunc;
		public boolean textureAlphaUsed;
		public boolean textureColorDoubled;
		public int stencilFunc;
		public int stencilRef;
		public int stencilMask;
		public int stencilOpFail;
		public int stencilOpZFail;
		public int stencilOpZPass;
	};

	public BaseRenderingEngineFunction(IRenderingEngine proxy) {
		super(proxy);
	}

	protected void setFlag(EnableDisableFlag flag) {
		if (flag.isEnabled()) {
			re.enableFlag(flag.getReFlag());
		} else {
			re.disableFlag(flag.getReFlag());
		}
	}

	@Override
	public void startClearMode(boolean color, boolean stencil, boolean depth) {
		// Disable all the flags invalid in clear mode
		for (int i = 0; i < flagsValidInClearMode.length; i++) {
			if (!flagsValidInClearMode[i]) {
				re.disableFlag(i);
			}
		}

		clearModeContext.depthFunc = context.depthFunc;
		clearModeContext.textureFunc = context.textureFunc;
		clearModeContext.textureAlphaUsed = context.textureAlphaUsed;
		clearModeContext.textureColorDoubled = context.textureColorDoubled;
		clearModeContext.stencilFunc = context.stencilFunc;
		clearModeContext.stencilRef = context.stencilRef;
		clearModeContext.stencilMask = context.stencilMask;
		clearModeContext.stencilOpFail = context.stencilOpFail;
		clearModeContext.stencilOpZFail = context.stencilOpZFail;
		clearModeContext.stencilOpZPass = context.stencilOpZPass;

		if (stencil) {
            // TODO Stencil not perfect, pspsdk clear code is doing more things
            re.enableFlag(GU_STENCIL_TEST);
            re.setStencilFunc(GeCommands.STST_FUNCTION_ALWAYS_PASS_STENCIL_TEST, 0, 0);
            re.setStencilOp(GeCommands.SOP_KEEP_STENCIL_VALUE, GeCommands.SOP_KEEP_STENCIL_VALUE, GeCommands.SOP_ZERO_STENCIL_VALUE);
		}
		if (depth) {
            re.enableFlag(GU_DEPTH_TEST);
            context.depthFunc = GeCommands.ZTST_FUNCTION_ALWAYS_PASS_PIXEL;
    		re.setDepthFunc(context.depthFunc);
		}

		re.setDepthMask(depth);
		re.setColorMask(color, color, color, stencil);
		re.setTexEnv(RE_TEXENV_RGB_SCALE, 1.0f);
		re.setTexEnv(RE_TEXENV_ENV_MODE, RE_TEXENV_REPLACE);

		super.startClearMode(color, stencil, depth);

		clearMode = true;
	}

	@Override
	public void endClearMode() {
		clearMode = false;

		// Reset all the flags disabled in CLEAR mode
		for (EnableDisableFlag flag : context.flags) {
			if (!flagsValidInClearMode[flag.getReFlag()]) {
				setFlag(flag);
			}
		}

		context.depthFunc = clearModeContext.depthFunc;
		re.setDepthFunc(clearModeContext.depthFunc);

		context.textureFunc = clearModeContext.textureFunc;
		context.textureAlphaUsed = clearModeContext.textureAlphaUsed;
		context.textureColorDoubled = clearModeContext.textureColorDoubled;
		re.setTextureFunc(context.textureFunc, context.textureAlphaUsed, context.textureColorDoubled);

		context.stencilFunc = clearModeContext.stencilFunc;
		context.stencilRef = clearModeContext.stencilRef;
		context.stencilMask = clearModeContext.stencilMask;
		re.setStencilFunc(context.stencilFunc, context.stencilRef, context.stencilRef);

		context.stencilOpFail = clearModeContext.stencilOpFail;
		context.stencilOpZFail = clearModeContext.stencilOpZFail;
		context.stencilOpZPass = clearModeContext.stencilOpZPass;
		re.setStencilOp(context.stencilOpFail, context.stencilOpZFail, context.stencilOpZPass);

		re.setDepthMask(context.depthMask);
    	re.setColorMask(context.colorMask[0], context.colorMask[1], context.colorMask[2], context.colorMask[3]);

		super.endClearMode();
	}

	protected boolean isClearMode() {
		return clearMode;
	}

	protected boolean canUpdateFlag(int flag) {
		return !clearMode || flagsValidInClearMode[flag];
	}

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
		if (!clearMode) {
			super.setColorMask(redMask, greenMask, blueMask, alphaMask);
		}
	}

	@Override
	public void setColorMask(boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled) {
		if (!clearMode) {
			super.setColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, alphaWriteEnabled);
		}
	}

	@Override
	public void setDepthMask(boolean depthWriteEnabled) {
		if (!clearMode) {
			super.setDepthMask(depthWriteEnabled);
		}
	}
}
