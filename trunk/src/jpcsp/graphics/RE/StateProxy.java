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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 * 
 * RenderingEngine Proxy class removing redundant calls.
 * E.g. calls setting multiple times the same value,
 * or calls with an invalid parameter (e.g. for unused shader uniforms).
 * This class implements no rendering logic, it just skips unnecessary calls.
 */
public class StateProxy extends BaseRenderingEngineProxy {
	protected boolean[] flags;
	protected float[][] matrix;
	protected static final int RE_BONES_MATRIX = 4;
	protected static final int matrix4Size = 4 * 4;
	public    static final int maxProgramId = 1000;
	public    static final int maxUniformId = 200;
	protected int[][] uniformInt;
	protected int[][][] uniformIntArray;
	protected float[][] uniformFloat;
	protected float[][][] uniformFloatArray;
	protected StateBoolean[] clientState;
	protected StateBoolean[] vertexAttribArray;
	protected boolean colorMaskRed;
	protected boolean colorMaskGreen;
	protected boolean colorMaskBlue;
	protected boolean colorMaskAlpha;
	protected int[] colorMask;
	protected boolean depthMask;
	protected int textureFunc;
	protected boolean textureFuncAlpha;
	protected boolean textureFuncColorDouble;
	protected boolean frontFace;
	protected int stencilFunc;
	protected int stencilFuncRef;
	protected int stencilFuncMask;
	protected int stencilOpFail;
	protected int stencilOpZFail;
	protected int stencilOpZPass;
	protected int depthFunc;
	protected int[] bindTexture;
	protected int[] bindBuffer;
	protected int useProgram;
	protected int textureMapMode;
	protected int textureProjMapMode;
	protected int viewportX;
	protected int viewportY;
	protected int viewportWidth;
	protected int viewportHeight;
	protected HashMap<Integer, int[]> bufferDataInt;
	protected HashMap<Integer, TextureState> textureStates;
	protected TextureState currentTextureState;
	protected int matrixMode;
	protected boolean fogHintSet;
	protected boolean lineSmoothHintSet;
	protected float[] texEnvf;
	protected int[] texEnvi;
	protected int pixelStoreRowLength;
	protected int pixelStoreAlignment;
	protected int scissorX;
	protected int scissorY;
	protected int scissorWidth;
	protected int scissorHeight;
	protected int blendEquation;
	protected int shadeModel;
	protected int alphaFunc;
	protected int alphaFuncRef;
	protected float depthRangeZpos;
	protected float depthRangeZscale;
	protected float depthRangeNear;
	protected float depthRangeFar;
	protected float[] vertexColor;
	protected float[][] lightAmbientColor;
	protected float[][] lightDiffuseColor;
	protected float[][] lightSpecularColor;
	protected float[] lightModelAmbientColor;
	protected int lightMode;
	protected float[] materialAmbientColor;
	protected float[] materialDiffuseColor;
	protected float[] materialSpecularColor;
	protected float[] materialEmissiveColor;
	protected StateBoolean colorMaterialAmbient;
	protected StateBoolean colorMaterialDiffuse;
	protected StateBoolean colorMaterialSpecular;
	protected int bindVertexArray;
	protected int activeTextureUnit;
	protected boolean useTextureAnisotropicFilter;
	protected int dfix;
	protected int sfix;

	protected static class StateBoolean {
		private boolean undefined = true;
		private boolean value;

		public boolean isUndefined() {
			return undefined;
		}

		public void setUndefined() {
			undefined = true;
		}

		public boolean getValue() {
			return value;
		}

		public void setValue(boolean value) {
			this.value = value;
			undefined = false;
		}

		public boolean isTrue() {
			return !undefined && value;
		}

		public boolean isFalse() {
			return !undefined && !value;
		}

		public boolean isValue(boolean value) {
			return !undefined && this.value == value;
		}

		@Override
		public String toString() {
			if (isUndefined()) {
				return "Undefined";
			}
			return Boolean.toString(getValue());
		}
	}

	protected static class TextureState {
		public int textureWrapModeS = GeCommands.TWRAP_WRAP_MODE_REPEAT;
		public int textureWrapModeT = GeCommands.TWRAP_WRAP_MODE_REPEAT;
		public int textureMipmapMinFilter = GeCommands.TFLT_NEAREST_MIPMAP_LINEAR;
		public int textureMipmapMagFilter = GeCommands.TFLT_LINEAR;
		public int textureMipmapMinLevel = 0;
		public int textureMipmapMaxLevel = 1000;
		public float textureAnisotropy = 0;
	}

	public StateProxy(IRenderingEngine proxy) {
		super(proxy);
		init();
	}

	protected void init() {
		flags = new boolean[RE_NUMBER_FLAGS];

		uniformInt = new int[maxProgramId][maxUniformId];
		uniformFloat = new float[maxProgramId][maxUniformId];
		uniformIntArray = new int[maxProgramId][maxUniformId][];
		uniformFloatArray = new float[maxProgramId][maxUniformId][];

		matrix = new float[RE_BONES_MATRIX + 1][];
		matrix[GU_PROJECTION] = new float[matrix4Size];
		matrix[GU_VIEW] = new float[matrix4Size];
		matrix[GU_MODEL] = new float[matrix4Size];
		matrix[GU_TEXTURE] = new float[matrix4Size];
		matrix[RE_BONES_MATRIX] = new float[8 * matrix4Size];

		clientState = new StateBoolean[4];
		for (int i = 0; i < clientState.length; i++) {
			clientState[i] = new StateBoolean();
		}
		vertexAttribArray = new StateBoolean[maxUniformId];
		for (int i = 0; i < vertexAttribArray.length; i++) {
			vertexAttribArray[i] = new StateBoolean();
		}
		colorMask = new int[4];
		bufferDataInt = new HashMap<Integer, int[]>();
		textureStates = new HashMap<Integer, TextureState>();
		currentTextureState = new TextureState();
		textureStates.put(0, currentTextureState);
		texEnvf = new float[17];
		texEnvi = new int[17];
		vertexColor = new float[4];
		bindBuffer = new int[2];
		lightAmbientColor = new float[4][4];
		lightSpecularColor = new float[4][4];
		lightDiffuseColor = new float[4][4];
		lightModelAmbientColor = new float[4];
		materialAmbientColor = new float[4];
		materialSpecularColor = new float[4];
		materialDiffuseColor = new float[4];
		materialEmissiveColor = new float[4];
		bindTexture = new int[3]; // assume max 3 active texture units

		colorMaterialAmbient = new StateBoolean();
		colorMaterialDiffuse = new StateBoolean();
		colorMaterialSpecular = new StateBoolean();
	}

	@Override
	public void startDisplay() {
		// The following properties are lost when starting a new display
		for (int i = 0; i < clientState.length; i++) {
			clientState[i].setUndefined();
		}

		for (int i = 0; i < flags.length; i++) {
			flags[i] = true;
		}
		flags[GU_TEXTURE_2D] = false;

		System.arraycopy(identityMatrix, 0, matrix[GU_PROJECTION], 0, matrix4Size);
		System.arraycopy(identityMatrix, 0, matrix[GU_VIEW], 0, matrix4Size);
		System.arraycopy(identityMatrix, 0, matrix[GU_MODEL], 0, matrix4Size);
		System.arraycopy(identityMatrix, 0, matrix[GU_TEXTURE], 0, matrix4Size);
		colorMaskRed = true;
		colorMaskGreen = true;
		colorMaskBlue = true;
		colorMaskAlpha = true;
		depthMask = true;
		textureFunc = -1;
		textureFuncAlpha = true;
		textureFuncColorDouble = false;
		frontFace = true;
		stencilFunc = -1;
		stencilFuncRef = -1;
		stencilFuncMask = -1;
		stencilOpFail = -1;
		stencilOpZFail = -1;
		stencilOpZPass = -1;
		depthFunc = -1;
		for (int i = 0; i < bindTexture.length; i++) {
			bindTexture[i] = -1;
		}
		currentTextureState = textureStates.get(0);
		for (int i = 0; i < bindBuffer.length; i++) {
			bindBuffer[i] = -1;
		}
		activeTextureUnit = 0;
		frontFace = false;
		useProgram = 0;
		textureMapMode = -1;
		textureProjMapMode = -1;
		viewportX = -1;
		viewportY = -1;
		viewportWidth = -1;
		viewportHeight = -1;
		matrixMode = -1;
		fogHintSet = false;
		lineSmoothHintSet = false;
		for (int i = 0; i < texEnvi.length; i++) {
			texEnvi[i] = -1;
		}
		for (int i = 0; i < texEnvf.length; i++) {
			texEnvf[i] = -1;
		}
		// Default OpenGL texEnv values
		texEnvf[IRenderingEngine.RE_TEXENV_RGB_SCALE] = 1.f;
		texEnvi[IRenderingEngine.RE_TEXENV_ENV_MODE] = IRenderingEngine.RE_TEXENV_MODULATE;
		pixelStoreRowLength = -1;
		pixelStoreAlignment = -1;
		scissorX = -1;
		scissorY = -1;
		scissorWidth = -1;
		scissorHeight = -1;
		blendEquation = -1;
		shadeModel = -1;
		alphaFunc = -1;
		alphaFuncRef = -1;
		depthRangeZpos = 0.f;
		depthRangeZscale = 0.f;
		depthRangeNear = 0.f;
		depthRangeFar = 0.f;
		vertexColor[0] = -1.f;
		for (int i = 0; i < lightAmbientColor.length; i++) {
			lightAmbientColor[i][0] = -1.f;
			lightDiffuseColor[i][0] = -1.f;
			lightSpecularColor[i][0] = -1.f;
		}
		lightModelAmbientColor[0] = -1.f;
		lightMode = -1;
		materialAmbientColor[0] = -10000.f;
		materialDiffuseColor[0] = -1.f;
		materialSpecularColor[0] = -1.f;
		materialEmissiveColor[0] = -1.f;
		colorMaterialAmbient.setUndefined();
		colorMaterialDiffuse.setUndefined();
		colorMaterialSpecular.setUndefined();
		bindVertexArray = 0;

		if (VideoEngine.getInstance().isUseTextureAnisotropicFilter() != useTextureAnisotropicFilter) {
			// The texture anisotropic filter has been changed,
			// invalidate all the texture magnification filters
			for (TextureState textureState : textureStates.values()) {
				textureState.textureMipmapMagFilter = -1;
			}
			useTextureAnisotropicFilter = VideoEngine.getInstance().isUseTextureAnisotropicFilter();
		}

		super.startDisplay();
	}

	@Override
	public void disableFlag(int flag) {
		if (flags[flag]) {
			super.disableFlag(flag);
			flags[flag] = false;
		}
	}

	@Override
	public void enableFlag(int flag) {
		if (!flags[flag]) {
			super.enableFlag(flag);
			flags[flag] = true;
		}
	}

	@Override
	public void setUniform(int id, int value) {
		// An unused uniform as an id == -1
		if (id >= 0 && id <= maxUniformId) {
			if (uniformInt[useProgram][id] != value) {
				super.setUniform(id, value);
				uniformInt[useProgram][id] = value;
			}
		}
	}

	@Override
	public void setUniform(int id, float value) {
		if (id >= 0 && id <= maxUniformId) {
			if (uniformFloat[useProgram][id] != value) {
				super.setUniform(id, value);
				uniformFloat[useProgram][id] = value;
			}
		}
	}

	@Override
	public void setUniform(int id, int value1, int value2) {
		if (id >= 0 && id <= maxUniformId) {
			int[] oldValues = uniformIntArray[useProgram][id];
			if (oldValues == null || oldValues.length != 2) {
				super.setUniform(id, value1, value2);
				uniformIntArray[useProgram][id] = new int[] { value1, value2 };
			} else {
				if (oldValues[0] != value1 || oldValues[1] != value2) {
					super.setUniform(id, value1, value2);
					oldValues[0] = value1;
					oldValues[1] = value2;
				}
			}
		}
	}

	@Override
	public void setUniform2(int id, int[] values) {
		if (id >= 0 && id <= maxUniformId) {
			int[] oldValues = uniformIntArray[useProgram][id];
			if (oldValues == null || oldValues.length != 2) {
				super.setUniform2(id, values);
				oldValues = new int[2];
				oldValues[0] = values[0];
				oldValues[1] = values[1];
				uniformIntArray[useProgram][id] = oldValues;
			} else if (oldValues[0] != values[0] || oldValues[1] != values[1]) {
				super.setUniform2(id, values);
				oldValues[0] = values[0];
				oldValues[1] = values[1];
			}
		}
	}

	@Override
	public void setUniform3(int id, int[] values) {
		if (id >= 0 && id <= maxUniformId) {
			int[] oldValues = uniformIntArray[useProgram][id];
			if (oldValues == null || oldValues.length != 3) {
				super.setUniform3(id, values);
				oldValues = new int[3];
				oldValues[0] = values[0];
				oldValues[1] = values[1];
				oldValues[2] = values[2];
				uniformIntArray[useProgram][id] = oldValues;
			} else if (oldValues[0] != values[0] || oldValues[1] != values[1] || oldValues[2] != values[2]) {
				super.setUniform3(id, values);
				oldValues[0] = values[0];
				oldValues[1] = values[1];
				oldValues[2] = values[2];
			}
		}
	}

	@Override
	public void setUniform4(int id, int[] values) {
		if (id >= 0 && id <= maxUniformId) {
			int[] oldValues = uniformIntArray[useProgram][id];
			if (oldValues == null || oldValues.length != 4) {
				super.setUniform4(id, values);
				oldValues = new int[4];
				oldValues[0] = values[0];
				oldValues[1] = values[1];
				oldValues[2] = values[2];
				oldValues[3] = values[3];
				uniformIntArray[useProgram][id] = oldValues;
			} else if (oldValues[0] != values[0] || oldValues[1] != values[1] || oldValues[2] != values[2] || oldValues[3] != values[3]) {
				super.setUniform4(id, values);
				oldValues[0] = values[0];
				oldValues[1] = values[1];
				oldValues[2] = values[2];
				oldValues[3] = values[3];
			}
		}
	}

	@Override
	public void setUniformMatrix4(int id, int count, float[] values) {
		if (id >= 0 && id <= maxUniformId && count > 0) {
			float[] oldValues = uniformFloatArray[useProgram][id];
			int length = count * matrix4Size;
			if (oldValues == null || oldValues.length < length) {
				super.setUniformMatrix4(id, count, values);
				oldValues = new float[length];
				System.arraycopy(values, 0, oldValues, 0, length);
				uniformFloatArray[useProgram][id] = oldValues;
			} else {
				boolean differ = false;
				for (int i = 0; i < length; i++) {
					if (oldValues[i] != values[i]) {
						differ = true;
						break;
					}
				}

				if (differ) {
					super.setUniformMatrix4(id, count, values);
					System.arraycopy(values, 0, oldValues, 0, length);
				}
			}
		}
	}

	protected int matrixFirstUpdated(int id, float[] values) {
		if (values == null) {
			values = identityMatrix;
		}

		float[] oldValues = matrix[id];
		for (int i = 0; i < values.length; i++) {
			if (values[i] != oldValues[i]) {
				// Update the remaining values
				System.arraycopy(values, i, oldValues, i, values.length - i);
				return i;
			}
		}

		return values.length;
	}

	protected int matrixLastUpdated(int id, float[] values, int length) {
		float[] oldValues = matrix[id];

		if (values == null) {
			values = identityMatrix;
		}

		for (int i = length - 1; i >= 0; i--) {
			if (oldValues[i] != values[i]) {
				// Update the remaining values
				System.arraycopy(values, 0, oldValues, 0, i + 1);
				return i;
			}
		}

		return 0;
	}

	protected boolean isIdentityMatrix(float[] values) {
		if (values == null) {
			return true;
		}

		if (values.length != identityMatrix.length) {
			return false;
		}

		for (int i = 0; i < identityMatrix.length; i++) {
			if (values[i] != identityMatrix[i]) {
				return false;
			}
		}

		return true;
	}

	@Override
	public void disableClientState(int type) {
		StateBoolean state = clientState[type];
		if (!state.isFalse()) {
			super.disableClientState(type);
			state.setValue(false);
		}
	}

	@Override
	public void enableClientState(int type) {
		// enableClientState(RE_VERTEX) cannot be cached: it is required each time
		// by OpenGL and seems to trigger the correct Vertex generation.
		StateBoolean state = clientState[type];
		if (type == RE_VERTEX || !state.isTrue()) {
			super.enableClientState(type);
			state.setValue(true);
		}
	}

	@Override
	public void disableVertexAttribArray(int id) {
		if (id >= 0 && id <= maxUniformId) {
			StateBoolean state = vertexAttribArray[id];
			if (!state.isFalse()) {
				super.disableVertexAttribArray(id);
				state.setValue(false);
			}
		}
	}

	@Override
	public void enableVertexAttribArray(int id) {
		if (id >= 0 && id <= maxUniformId) {
			StateBoolean state = vertexAttribArray[id];
			if (!state.isTrue()) {
				super.enableVertexAttribArray(id);
				state.setValue(true);
			}
		}
	}

	@Override
	public void setColorMask(boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled) {
		if (redWriteEnabled != colorMaskRed || greenWriteEnabled != colorMaskGreen || blueWriteEnabled != colorMaskBlue || alphaWriteEnabled != colorMaskAlpha) {
			super.setColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, alphaWriteEnabled);
			colorMaskRed = redWriteEnabled;
			colorMaskGreen = greenWriteEnabled;
			colorMaskBlue = blueWriteEnabled;
			colorMaskAlpha = alphaWriteEnabled;
//			colorMask[0] = redWriteEnabled ? 0x00 : 0xFF;
//			colorMask[1] = greenWriteEnabled ? 0x00 : 0xFF;
//			colorMask[2] = blueWriteEnabled ? 0x00 : 0xFF;
//			colorMask[3] = alphaWriteEnabled ? 0x00 : 0xFF;
		}
	}

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
		if (redMask != colorMask[0] || greenMask != colorMask[1] || blueMask != colorMask[2] || alphaMask != colorMask[3]) {
			super.setColorMask(redMask, greenMask, blueMask, alphaMask);
//			colorMaskRed = redMask != 0xFF;
//			colorMaskGreen = greenMask != 0xFF;
//			colorMaskBlue = blueMask != 0xFF;
//			colorMaskAlpha = alphaMask != 0xFF;
			colorMask[0] = redMask;
			colorMask[1] = greenMask;
			colorMask[2] = blueMask;
			colorMask[3] = alphaMask;
		}
	}

	@Override
	public void setDepthMask(boolean depthWriteEnabled) {
		if (depthWriteEnabled != depthMask) {
			super.setDepthMask(depthWriteEnabled);
			depthMask = depthWriteEnabled;
		}
	}

	@Override
	public void setFrontFace(boolean cw) {
		if (cw != frontFace) {
			super.setFrontFace(cw);
			frontFace = cw;
		}
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
		if (func != textureFunc || alphaUsed != textureFuncAlpha || colorDoubled != textureFuncColorDouble) {
			super.setTextureFunc(func, alphaUsed, colorDoubled);
			textureFunc = func;
			textureFuncAlpha = alphaUsed;
			textureFuncColorDouble = colorDoubled;
		}
	}

	@Override
	public void setTextureMipmapMinFilter(int filter) {
		if (filter != currentTextureState.textureMipmapMinFilter) {
			super.setTextureMipmapMinFilter(filter);
			currentTextureState.textureMipmapMinFilter = filter;
		}
	}

	@Override
	public void setTextureMipmapMagFilter(int filter) {
		if (filter != currentTextureState.textureMipmapMagFilter) {
			super.setTextureMipmapMagFilter(filter);
			currentTextureState.textureMipmapMagFilter = filter;
		}
	}

	@Override
	public void setTextureMipmapMinLevel(int level) {
		if (level != currentTextureState.textureMipmapMinLevel) {
			super.setTextureMipmapMinLevel(level);
			currentTextureState.textureMipmapMinLevel = level;
		}
	}

	@Override
	public void setTextureMipmapMaxLevel(int level) {
		if (level != currentTextureState.textureMipmapMaxLevel) {
			super.setTextureMipmapMaxLevel(level);
			currentTextureState.textureMipmapMaxLevel = level;
		}
	}

	@Override
	public void setTextureWrapMode(int s, int t) {
		if (s != currentTextureState.textureWrapModeS || t != currentTextureState.textureWrapModeT) {
			super.setTextureWrapMode(s, t);
			currentTextureState.textureWrapModeS = s;
			currentTextureState.textureWrapModeT = t;
		}
	}

	@Override
	public void bindTexture(int texture) {
		if (texture != bindTexture[activeTextureUnit]) {
			super.bindTexture(texture);
			bindTexture[activeTextureUnit] = texture;
			// Binding a new texture change the OpenGL texture wrap mode and min/mag filters
			currentTextureState = textureStates.get(texture);
			if (currentTextureState == null) {
				currentTextureState = new TextureState();
				textureStates.put(texture, currentTextureState);
			}
		}
	}

	@Override
	public void setDepthFunc(int func) {
		if (func != depthFunc) {
			super.setDepthFunc(func);
			depthFunc = func;
		}
	}

	@Override
	public void setStencilFunc(int func, int ref, int mask) {
		if (func != stencilFunc || ref != stencilFuncRef || mask != stencilFuncMask) {
			super.setStencilFunc(func, ref, mask);
			stencilFunc = func;
			stencilFuncRef = ref;
			stencilFuncMask = mask;
		}
	}

	@Override
	public void setStencilOp(int fail, int zfail, int zpass) {
		if (fail != stencilOpFail || zfail != stencilOpZFail || zpass != stencilOpZPass) {
			super.setStencilOp(fail, zfail, zpass);
			stencilOpFail = fail;
			stencilOpZFail = zfail;
			stencilOpZPass = zpass;
		}
	}

	@Override
	public void deleteTexture(int texture) {
		textureStates.remove(texture);
		// When deleting the current texture, the current binding is reset to 0
		for (int i = 0; i < bindTexture.length; i++) {
			if (texture == bindTexture[i]) {
				bindTexture[i] = 0;
				if (i == activeTextureUnit) {
					currentTextureState = textureStates.get(bindTexture[activeTextureUnit]);
				}
			}
		}
		super.deleteTexture(texture);
	}

	@Override
	public void bindBuffer(int target, int buffer) {
		if (bindBuffer[target] != buffer) {
			super.bindBuffer(target, buffer);
			bindBuffer[target] = buffer;
		}
	}

	@Override
	public void deleteBuffer(int buffer) {
		// When deleting the current buffer, the current binding is reset to 0
		for (int target = 0; target < bindBuffer.length; target++) {
			if (buffer == bindBuffer[target]) {
				bindBuffer[target] = 0;
			}
		}
		super.deleteBuffer(buffer);
	}

	@Override
	public void setViewport(int x, int y, int width, int height) {
		// Negative x and y values are valid values
		if (width >= 0 && height >= 0) {
			if (x != viewportX || y != viewportY || width != viewportWidth || height != viewportHeight) {
				super.setViewport(x, y, width, height);
				viewportX = x;
				viewportY = y;
				viewportWidth = width;
				viewportHeight = height;
			}
		}
	}

	@Override
	public void useProgram(int program) {
		if (useProgram != program) {
			super.useProgram(program);
			useProgram = program;
		}
	}

	@Override
	public void setTextureMapMode(int mode, int proj) {
		if (mode != textureMapMode || proj != textureProjMapMode) {
			super.setTextureMapMode(mode, proj);
			textureMapMode = mode;
			textureProjMapMode = proj;
		}
	}

	private void setBufferData(int target, int size, IntBuffer buffer, int usage) {
		int[] oldData = bufferDataInt.get(bindBuffer[target]);
		int[] newData = new int[size / 4];
		int position = buffer.position();
		buffer.get(newData);
		buffer.position(position);

		boolean differ = true;
		boolean setBufferData = true;
		if (oldData != null && newData.length == oldData.length) {
			differ = false;
			setBufferData = false;
			int limit = buffer.limit();
			for (int i = 0; i < newData.length; i++) {
				if (newData[i] != oldData[i]) {
					differ = true;
					int end = i + 1;
					for (int j = i + 1; j < newData.length; j++) {
						if (newData[j] == oldData[j]) {
							end = j;
							break;
						}
					}
					buffer.position(i);
					super.setBufferSubData(target, i * 4, (end - i) * 4, buffer);
					buffer.limit(limit);
					i = end;
				}
			}
		}

		if (setBufferData) {
			super.setBufferData(target, size, buffer, usage);
		}
		if (differ) {
			bufferDataInt.put(bindBuffer[target], newData);
		}
	}

	@Override
	public void setBufferData(int target, int size, Buffer buffer, int usage) {
		if (target == RE_UNIFORM_BUFFER && size <= 1024 && (size & 3) == 0 && buffer instanceof IntBuffer) {
			setBufferData(target, size, (IntBuffer) buffer, usage);
		} else if (target == RE_UNIFORM_BUFFER && size <= 1024 && (size & 3) == 0 && buffer instanceof ByteBuffer) {
			setBufferData(target, size, ((ByteBuffer) buffer).asIntBuffer(), usage);
		} else {
			super.setBufferData(target, size, buffer, usage);
		}
	}

	@Override
	public void setMatrixMode(int type) {
		if (type != matrixMode) {
			super.setMatrixMode(type);
			matrixMode = type;
		}
	}

	@Override
	public void setMatrix(float[] values) {
		if (matrixFirstUpdated(matrixMode, values) < matrix4Size) {
			if (isIdentityMatrix(values)) {
				// Identity Matrix is identified by the special value "null"
				super.setMatrix(null);
			} else {
				super.setMatrix(values);
			}
		}
	}

	@Override
	public void multMatrix(float[] values) {
		if (!isIdentityMatrix(values)) {
			super.multMatrix(values);
		}
	}

	@Override
	public void setFogHint() {
		if (!fogHintSet) {
			super.setFogHint();
			fogHintSet = true;
		}
	}

	@Override
	public void setLineSmoothHint() {
		if (!lineSmoothHintSet) {
			super.setLineSmoothHint();
			lineSmoothHintSet = true;
		}
	}

	@Override
	public void setTexEnv(int name, float param) {
		if (texEnvf[name] != param) {
			super.setTexEnv(name, param);
			texEnvf[name] = param;
		}
	}

	@Override
	public void setTexEnv(int name, int param) {
		if (texEnvi[name] != param) {
			super.setTexEnv(name, param);
			texEnvi[name] = param;
		}
	}

	@Override
	public void setPixelStore(int rowLength, int alignment) {
		if (pixelStoreRowLength != rowLength || pixelStoreAlignment != alignment) {
			super.setPixelStore(rowLength, alignment);
			pixelStoreRowLength = rowLength;
			pixelStoreAlignment = alignment;
		}
	}

	@Override
	public void setScissor(int x, int y, int width, int height) {
		if (x >= 0 && y >= 0 && width >= 0 && height >= 0) {
			if (x != scissorX || y != scissorY || width != scissorWidth || height != scissorHeight) {
				super.setScissor(x, y, width, height);
				scissorX = x;
				scissorY = y;
				scissorWidth = width;
				scissorHeight = height;
			}
		}
	}

	@Override
	public void setBlendEquation(int mode) {
		if (blendEquation != mode) {
			super.setBlendEquation(mode);
			blendEquation = mode;
		}
	}

	@Override
	public void setShadeModel(int model) {
		if (shadeModel != model) {
			super.setShadeModel(model);
			shadeModel = model;
		}
	}

	@Override
	public void setAlphaFunc(int func, int ref) {
		if (alphaFunc != func || alphaFuncRef != ref) {
			super.setAlphaFunc(func, ref);
			alphaFunc = func;
			alphaFuncRef = ref;
		}
	}

	@Override
	public void setDepthRange(float zpos, float zscale, float near, float far) {
		if (depthRangeZpos != zpos || depthRangeZscale != zscale || depthRangeNear != near || depthRangeFar != far) {
			super.setDepthRange(zpos, zscale, near, far);
			depthRangeZpos = zpos;
			depthRangeZscale = zscale;
			depthRangeNear = near;
			depthRangeFar = far;
		}
	}

	@Override
	public void setVertexColor(float[] color) {
		if (vertexColor[0] != color[0] || vertexColor[1] != color[1] || vertexColor[2] != color[2] || vertexColor[3] != color[3]) {
			super.setVertexColor(color);
			vertexColor[0] = color[0];
			vertexColor[1] = color[1];
			vertexColor[2] = color[2];
			vertexColor[3] = color[3];
		}
	}

	@Override
	public void setLightAmbientColor(int light, float[] color) {
		float[] stateColor = lightAmbientColor[light];
		if (stateColor[0] != color[0] || stateColor[1] != color[1] || stateColor[2] != color[2] || stateColor[3] != color[3]) {
			super.setLightAmbientColor(light, color);
			stateColor[0] = color[0];
			stateColor[1] = color[1];
			stateColor[2] = color[2];
			stateColor[3] = color[3];
		}
	}

	@Override
	public void setLightDiffuseColor(int light, float[] color) {
		float[] stateColor = lightDiffuseColor[light];
		if (stateColor[0] != color[0] || stateColor[1] != color[1] || stateColor[2] != color[2] || stateColor[3] != color[3]) {
			super.setLightDiffuseColor(light, color);
			stateColor[0] = color[0];
			stateColor[1] = color[1];
			stateColor[2] = color[2];
			stateColor[3] = color[3];
		}
	}

	@Override
	public void setLightSpecularColor(int light, float[] color) {
		float[] stateColor = lightSpecularColor[light];
		if (stateColor[0] != color[0] || stateColor[1] != color[1] || stateColor[2] != color[2] || stateColor[3] != color[3]) {
			super.setLightSpecularColor(light, color);
			stateColor[0] = color[0];
			stateColor[1] = color[1];
			stateColor[2] = color[2];
			stateColor[3] = color[3];
		}
	}

	@Override
	public void setMaterialAmbientColor(float[] color) {
		if (materialAmbientColor[0] != color[0] || materialAmbientColor[1] != color[1] || materialAmbientColor[2] != color[2] || materialAmbientColor[3] != color[3]) {
			super.setMaterialAmbientColor(color);
			materialAmbientColor[0] = color[0];
			materialAmbientColor[1] = color[1];
			materialAmbientColor[2] = color[2];
			materialAmbientColor[3] = color[3];
		}
	}

	@Override
	public void setMaterialDiffuseColor(float[] color) {
		if (materialDiffuseColor[0] != color[0] || materialDiffuseColor[1] != color[1] || materialDiffuseColor[2] != color[2] || materialDiffuseColor[3] != color[3]) {
			super.setMaterialDiffuseColor(color);
			materialDiffuseColor[0] = color[0];
			materialDiffuseColor[1] = color[1];
			materialDiffuseColor[2] = color[2];
			materialDiffuseColor[3] = color[3];
		}
	}

	@Override
	public void setMaterialEmissiveColor(float[] color) {
		if (materialEmissiveColor[0] != color[0] || materialEmissiveColor[1] != color[1] || materialEmissiveColor[2] != color[2] || materialEmissiveColor[3] != color[3]) {
			super.setMaterialEmissiveColor(color);
			materialEmissiveColor[0] = color[0];
			materialEmissiveColor[1] = color[1];
			materialEmissiveColor[2] = color[2];
			materialEmissiveColor[3] = color[3];
		}
	}

	@Override
	public void setMaterialSpecularColor(float[] color) {
		if (materialSpecularColor[0] != color[0] || materialSpecularColor[1] != color[1] || materialSpecularColor[2] != color[2] || materialSpecularColor[3] != color[3]) {
			super.setMaterialSpecularColor(color);
			materialSpecularColor[0] = color[0];
			materialSpecularColor[1] = color[1];
			materialSpecularColor[2] = color[2];
			materialSpecularColor[3] = color[3];
		}
	}

	@Override
	public void setColorMaterial(boolean ambient, boolean diffuse, boolean specular) {
		if (!colorMaterialAmbient.isValue(ambient) || !colorMaterialDiffuse.isValue(diffuse) || !colorMaterialSpecular.isValue(specular)) {
			super.setColorMaterial(ambient, diffuse, specular);
			colorMaterialAmbient.setValue(ambient);
			colorMaterialDiffuse.setValue(diffuse);
			colorMaterialSpecular.setValue(specular);
		}
	}

	private void invalidateMaterialColors() {
		// Drawing with "color material" enabled overwrites the material colors
		if (flags[IRenderingEngine.RE_COLOR_MATERIAL]) {
			if (colorMaterialAmbient.isTrue()) {
				materialAmbientColor[0] = -1.f;
			}
			if (colorMaterialDiffuse.isTrue()) {
				materialDiffuseColor[0] = -1.f;
			}
			if (colorMaterialSpecular.isTrue()) {
				materialSpecularColor[0] = -1.f;
			}
		}
	}

	@Override
	public void drawArrays(int type, int first, int count) {
		invalidateMaterialColors();
		super.drawArrays(type, first, count);
	}

	@Override
	public void setLightMode(int mode) {
		if (lightMode != mode) {
			super.setLightMode(mode);
			lightMode = mode;
		}
	}

	@Override
	public void setLightModelAmbientColor(float[] color) {
		if (lightModelAmbientColor[0] != color[0] || lightModelAmbientColor[1] != color[1] || lightModelAmbientColor[2] != color[2] || lightModelAmbientColor[3] != color[3]) {
			super.setLightModelAmbientColor(color);
			lightModelAmbientColor[0] = color[0];
			lightModelAmbientColor[1] = color[1];
			lightModelAmbientColor[2] = color[2];
			lightModelAmbientColor[3] = color[3];
		}
	}

	private void onVertexArrayChanged() {
		for (int i = 0; i < clientState.length; i++) {
			clientState[i].setUndefined();
		}
		for (int i = 0; i < vertexAttribArray.length; i++) {
			vertexAttribArray[i].setUndefined();
		}
	}

	@Override
	public void bindVertexArray(int id) {
		if (id != bindVertexArray) {
			onVertexArrayChanged();
			super.bindVertexArray(id);
			bindVertexArray = id;
		}
	}

	@Override
	public void deleteVertexArray(int id) {
		// When deleting the current vertex array, the current binding is reset to 0
		if (id == bindVertexArray) {
			onVertexArrayChanged();
			bindVertexArray = 0;
		}
		super.deleteVertexArray(id);
	}

	@Override
	public void setActiveTexture(int index) {
		if (index != activeTextureUnit) {
			super.setActiveTexture(index);
			activeTextureUnit = index;
			currentTextureState = textureStates.get(bindTexture[activeTextureUnit]);
		}
	}

	@Override
	public void bindActiveTexture(int index, int texture) {
		if (texture != bindTexture[index]) {
			super.bindActiveTexture(index, texture);
			bindTexture[index] = texture;
			if (index == activeTextureUnit) {
				// Binding a new texture change the OpenGL texture wrap mode and min/mag filters
				currentTextureState = textureStates.get(texture);
				if (currentTextureState == null) {
					currentTextureState = new TextureState();
					textureStates.put(texture, currentTextureState);
				}
			}
		}
	}

	@Override
	public void setTextureAnisotropy(float value) {
		if (value != currentTextureState.textureAnisotropy) {
			super.setTextureAnisotropy(value);
			currentTextureState.textureAnisotropy = value;
		}
	}

	@Override
	public void setBlendSFix(int sfix, float[] color) {
		if (this.sfix != sfix) {
			super.setBlendSFix(sfix, color);
			this.sfix = sfix;
		}
	}

	@Override
	public void setBlendDFix(int dfix, float[] color) {
		if (this.dfix != dfix) {
			super.setBlendDFix(dfix, color);
			this.dfix = dfix;
		}
	}
}
