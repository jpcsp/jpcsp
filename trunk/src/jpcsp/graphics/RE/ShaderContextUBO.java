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

import static jpcsp.graphics.VideoEngine.SIZEOF_FLOAT;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import jpcsp.graphics.Uniforms;
import jpcsp.settings.Settings;

/**
 * @author gid15
 *
 * Implementation of the ShaderContext using Uniform Buffer Object (UBO)
 * to allow a faster shader program switch.
 */
public class ShaderContextUBO extends ShaderContext {
	private ShaderUniformInfo lightType;
	private ShaderUniformInfo lightKind;
	private ShaderUniformInfo lightEnabled;
	private ShaderUniformInfo vertexColor;
	private ShaderUniformInfo colorMask;
	private ShaderUniformInfo notColorMask;
	private ShaderUniformInfo matFlags;
	private ShaderUniformInfo ctestRef;
	private ShaderUniformInfo ctestMsk;
	private ShaderUniformInfo texShade;
	private ShaderUniformInfo texEnvMode;
	private ShaderUniformInfo ctestFunc;
	private ShaderUniformInfo texMapMode;
	private ShaderUniformInfo texMapProj;
	private ShaderUniformInfo vinfoColor;
	private ShaderUniformInfo vinfoPosition;
	private ShaderUniformInfo vinfoTexture;
	private ShaderUniformInfo vinfoNormal;
	private ShaderUniformInfo positionScale;
	private ShaderUniformInfo normalScale;
	private ShaderUniformInfo textureScale;
	private ShaderUniformInfo weightScale;
	private ShaderUniformInfo colorDoubling;
	private ShaderUniformInfo texEnable;
	private ShaderUniformInfo lightingEnable;
	private ShaderUniformInfo vinfoTransform2D;
	private ShaderUniformInfo ctestEnable;
	private ShaderUniformInfo lightMode;
	private ShaderUniformInfo clutShift;
	private ShaderUniformInfo clutMask;
	private ShaderUniformInfo clutOffset;
	private ShaderUniformInfo mipmapShareClut;
	private ShaderUniformInfo texPixelFormat;
	private ShaderUniformInfo stencilTestEnable;
	private ShaderUniformInfo stencilFunc;
	private ShaderUniformInfo stencilRef;
	private ShaderUniformInfo stencilMask;
	private ShaderUniformInfo stencilOpFail;
	private ShaderUniformInfo stencilOpZFail;
	private ShaderUniformInfo stencilOpZPass;
	private ShaderUniformInfo depthTestEnable;
	private ShaderUniformInfo depthFunc;
	private ShaderUniformInfo depthMask;
	private ShaderUniformInfo alphaTestEnable;
	private ShaderUniformInfo alphaTestFunc;
	private ShaderUniformInfo alphaTestRef;
	private ShaderUniformInfo blendTestEnable;
	private ShaderUniformInfo blendEquation;
	private ShaderUniformInfo blendSrc;
	private ShaderUniformInfo blendDst;
	private ShaderUniformInfo blendSFix;
	private ShaderUniformInfo blendDFix;
	private ShaderUniformInfo colorMaskEnable;
	private ShaderUniformInfo wrapModeS;
	private ShaderUniformInfo wrapModeT;
	private ShaderUniformInfo copyRedToAlpha;
	private ShaderUniformInfo numberBones;
	private ShaderUniformInfo boneMatrix;
	private ShaderUniformInfo endOfUBO;
	private int bufferSize;
	protected static final int bindingPoint = 1;
	protected static final String uniformBlockName = "psp";
	protected static final String uniformMemoryLayout = "std140";
	protected int buffer;
	protected ByteBuffer data;
	private int startUpdate;
	private int endUpdate;
	private String shaderUniformText;
	private ArrayList<ShaderUniformInfo> shaderUniformInfos;

	private static class ShaderUniformInfo {
		private String name;
		private String structureName;
		private String type;
		private int offset;
		private int matrixSize;
		private boolean used;

		public ShaderUniformInfo(Uniforms uniform, String type) {
			name = uniform.getUniformString();
			structureName = this.name;
			this.type = type;
			used = true;
			matrixSize = 0;
		}

		public ShaderUniformInfo(Uniforms uniform, String type, int matrixSize) {
			name = uniform.getUniformString();
			structureName = String.format("%s[%d]", name, matrixSize);
			this.type = type;
			this.matrixSize = matrixSize;
			used = true;
		}

		public String getName() {
			return name;
		}

		public String getStructureName() {
			return structureName;
		}

		public int getOffset() {
			return offset;
		}

		public void setOffset(int offset) {
			this.offset = offset;
		}

		private String getType() {
			return type;
		}

		public boolean isUsed() {
			return used;
		}

		public void setUnused() {
			used = false;
		}

		public int getMatrixSize() {
			return matrixSize;
		}

		@Override
		public String toString() {
			if (!isUsed()) {
				return String.format("%s(unused)", getName());
			}
			return String.format("%s(offset=%d)", getName(), getOffset());
		}
	}

	public static boolean useUBO(IRenderingEngine re) {
        return !Settings.getInstance().readBool("emu.disableubo")
            && re.isExtensionAvailable("GL_ARB_uniform_buffer_object");
	}

	public ShaderContextUBO(IRenderingEngine re) {
		shaderUniformInfos = new ArrayList<ShaderUniformInfo>();

		// Add all the shader uniform objects
		// in the order they have to be defined in the shader structure
		lightType = addShaderUniform(Uniforms.lightType, "ivec4");
		lightKind = addShaderUniform(Uniforms.lightKind, "ivec4");
		lightEnabled = addShaderUniform(Uniforms.lightEnabled, "ivec4");
		vertexColor = addShaderUniform(Uniforms.vertexColor, "vec4");
		colorMask = addShaderUniform(Uniforms.colorMask, "ivec4");
		notColorMask = addShaderUniform(Uniforms.notColorMask, "ivec4");
		blendSFix = addShaderUniform(Uniforms.blendSFix, "vec3");
		blendDFix = addShaderUniform(Uniforms.blendDFix, "vec3");
		matFlags = addShaderUniform(Uniforms.matFlags, "ivec3");
		ctestRef = addShaderUniform(Uniforms.ctestRef, "ivec3");
		ctestMsk = addShaderUniform(Uniforms.ctestMsk, "ivec3");
		texShade = addShaderUniform(Uniforms.texShade, "ivec2");
		texEnvMode = addShaderUniform(Uniforms.texEnvMode, "ivec2");
		ctestFunc = addShaderUniform(Uniforms.ctestFunc, "int");
		texMapMode = addShaderUniform(Uniforms.texMapMode, "int");
		texMapProj = addShaderUniform(Uniforms.texMapProj, "int");
		vinfoColor = addShaderUniform(Uniforms.vinfoColor, "int");
		vinfoPosition = addShaderUniform(Uniforms.vinfoPosition, "int");
		vinfoTexture = addShaderUniform(Uniforms.vinfoTexture, "int");
		vinfoNormal = addShaderUniform(Uniforms.vinfoNormal, "int");
		positionScale = addShaderUniform(Uniforms.positionScale, "float");
		normalScale = addShaderUniform(Uniforms.normalScale, "float");
		textureScale = addShaderUniform(Uniforms.textureScale, "float");
		weightScale = addShaderUniform(Uniforms.weightScale, "float");
		colorDoubling = addShaderUniform(Uniforms.colorDoubling, "float");
		texEnable = addShaderUniform(Uniforms.texEnable, "bool");
		lightingEnable = addShaderUniform(Uniforms.lightingEnable, "bool");
		vinfoTransform2D = addShaderUniform(Uniforms.vinfoTransform2D, "bool");
		ctestEnable = addShaderUniform(Uniforms.ctestEnable, "bool");
		lightMode = addShaderUniform(Uniforms.lightMode, "bool");
		clutShift = addShaderUniform(Uniforms.clutShift, "int");
		clutMask = addShaderUniform(Uniforms.clutMask, "int");
		clutOffset = addShaderUniform(Uniforms.clutOffset, "int");
		mipmapShareClut = addShaderUniform(Uniforms.mipmapShareClut, "bool");
		texPixelFormat = addShaderUniform(Uniforms.texPixelFormat, "int");
		stencilTestEnable = addShaderUniform(Uniforms.stencilTestEnable, "bool");
		stencilFunc = addShaderUniform(Uniforms.stencilFunc, "int");
		stencilRef = addShaderUniform(Uniforms.stencilRef, "int");
		stencilMask = addShaderUniform(Uniforms.stencilMask, "int");
		stencilOpFail = addShaderUniform(Uniforms.stencilOpFail, "int");
		stencilOpZFail = addShaderUniform(Uniforms.stencilOpZFail, "int");
		stencilOpZPass = addShaderUniform(Uniforms.stencilOpZPass, "int");
		depthTestEnable = addShaderUniform(Uniforms.depthTestEnable, "bool");
		depthFunc = addShaderUniform(Uniforms.depthFunc, "int");
		depthMask = addShaderUniform(Uniforms.depthMask, "int");
		alphaTestEnable = addShaderUniform(Uniforms.alphaTestEnable, "bool");
		alphaTestFunc = addShaderUniform(Uniforms.alphaTestFunc, "int");
		alphaTestRef = addShaderUniform(Uniforms.alphaTestRef, "int");
		blendTestEnable = addShaderUniform(Uniforms.blendTestEnable, "bool");
		blendEquation = addShaderUniform(Uniforms.blendEquation, "int");
		blendSrc = addShaderUniform(Uniforms.blendSrc, "int");
		blendDst = addShaderUniform(Uniforms.blendDst, "int");
		colorMaskEnable = addShaderUniform(Uniforms.colorMaskEnable, "bool");
		wrapModeS = addShaderUniform(Uniforms.wrapModeS, "int");
		wrapModeT = addShaderUniform(Uniforms.wrapModeT, "int");
		copyRedToAlpha = addShaderUniform(Uniforms.copyRedToAlpha, "bool");
		numberBones = addShaderUniform(Uniforms.numberBones, "int");
		boneMatrix = addShaderUniform(Uniforms.boneMatrix, "mat4", 8);
		// The following entry has always to be the last one
		endOfUBO = addShaderUniform(Uniforms.endOfUBO, "int");

		StringBuilder s = new StringBuilder();
		s.append(String.format("layout(%s) uniform %s\n", uniformMemoryLayout, uniformBlockName));
		s.append(String.format("{\n"));
		for (ShaderUniformInfo shaderUniformInfo : shaderUniformInfos) {
			s.append(String.format("   %s %s;\n", shaderUniformInfo.getType(), shaderUniformInfo.getStructureName()));
		}
		s.append(String.format("};\n"));

		shaderUniformText = s.toString();
	}

	protected ShaderUniformInfo addShaderUniform(Uniforms uniform, String type) {
		ShaderUniformInfo shaderUniformInfo = new ShaderUniformInfo(uniform, type);
		shaderUniformInfos.add(shaderUniformInfo);

		return shaderUniformInfo;
	}

	protected ShaderUniformInfo addShaderUniform(Uniforms uniform, String type, int matrixSize) {
		ShaderUniformInfo shaderUniformInfo = new ShaderUniformInfo(uniform, type, matrixSize);
		shaderUniformInfos.add(shaderUniformInfo);

		return shaderUniformInfo;
	}

	public String getShaderUniformText() {
		return shaderUniformText;
	}

	@Override
	public void initShaderProgram(IRenderingEngine re, int shaderProgram) {
		int blockIndex = re.getUniformBlockIndex(shaderProgram, uniformBlockName);
		// The uniform block might have been eliminated by the shader compiler
		// if it was not used at all.
		if (blockIndex >= 0) {
			re.setUniformBlockBinding(shaderProgram, blockIndex, bindingPoint);
		}

		if (data == null) {
			int previousOffset = -1;
			for (ShaderUniformInfo shaderUniformInfo : shaderUniformInfos) {
				int index = re.getUniformIndex(shaderProgram, shaderUniformInfo.getName());
				int offset = re.getActiveUniformOffset(shaderProgram, index);

				// Nvidia workaround: the offset of the first uniform is returned as 1 instead of 0.
				if (offset == 1) {
					offset = 0;
				}

				shaderUniformInfo.setOffset(offset);

				// An unused uniform has the same offset as its previous uniform.
				// An unused uniform should not be copied into the UBO buffer,
				// otherwise it would overwrite the previous uniform value.
				if (offset < 0 || offset == previousOffset) {
					shaderUniformInfo.setUnused();
				}

				if (log.isDebugEnabled()) {
					log.debug(String.format("Uniform %s", shaderUniformInfo));
				}

				previousOffset = offset;
			}

			// The size returned by
			//    glGetActiveUniformBlock(program, blockIndex, ARBUniformBufferObject.GL_UNIFORM_BLOCK_DATA_SIZE)
			// is not reliable as the driver is free to reduce array sizes when they
			// are not used in the shader.
			// Use a dummy element of the structure to find the total structure size.
			int lastOffset;
			if (endOfUBO.getOffset() <= 0 || !endOfUBO.isUsed()) {
				// If the endOfUBO uniform has been eliminated by the shader compiler,
				// estimate the end of the buffer by using the offset of the boneMatrix uniform.
				lastOffset = boneMatrix.getOffset() + boneMatrix.getMatrixSize() * 4 * 4 * SIZEOF_FLOAT;
			} else {
				lastOffset = endOfUBO.getOffset();
			}
			bufferSize = lastOffset + 4;

			if (log.isDebugEnabled()) {
				log.debug(String.format("UBO Structure size: %d (including endOfUBO)", bufferSize));
			}

			buffer = re.genBuffer();
			re.bindBuffer(IRenderingEngine.RE_UNIFORM_BUFFER, buffer);

			data = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
			// Initialize the buffer to 0's
			for (int i = 0; i < bufferSize; i++) {
				data.put(i, (byte) 0);
			}
			re.setBufferData(IRenderingEngine.RE_UNIFORM_BUFFER, bufferSize, data, IRenderingEngine.RE_DYNAMIC_DRAW);

			// On AMD hardware, the buffer data has to be set (setBufferData) before calling bindBufferBase
			re.bindBufferBase(IRenderingEngine.RE_UNIFORM_BUFFER, bindingPoint, buffer);

			startUpdate = 0;
			endUpdate = bufferSize;
		}

		super.initShaderProgram(re, shaderProgram);
	}

	@Override
	public void setUniforms(IRenderingEngine re, int shaderProgram) {
		if (startUpdate < endUpdate) {
			re.bindBuffer(IRenderingEngine.RE_UNIFORM_BUFFER, buffer);

			data.position(startUpdate);
			re.setBufferSubData(IRenderingEngine.RE_UNIFORM_BUFFER, startUpdate, endUpdate - startUpdate, data);
			data.limit(data.capacity());
			startUpdate = bufferSize;
			endUpdate = 0;
		}

		// Samplers can only be passed as uniforms
		setUniformsSamplers(re, shaderProgram);
	}

	protected void prepareCopy(int offset, int length) {
		data.position(offset);

		if (offset < startUpdate) {
			startUpdate = offset;
		}
		if (offset + length > endUpdate) {
			endUpdate = offset + length;
		}
	}

	protected void copy(int value, ShaderUniformInfo shaderUniformInfo) {
		// Do not copy unused uniform, to avoid overwriting other used uniforms
		if (shaderUniformInfo.isUsed()) {
			prepareCopy(shaderUniformInfo.getOffset(), 4);
			data.putInt(value);
		}
	}

	protected void copy(int value, ShaderUniformInfo shaderUniformInfo, int index) {
		if (shaderUniformInfo.isUsed()) {
			prepareCopy(shaderUniformInfo.getOffset() + index * 4, 4);
			data.putInt(value);
		}
	}

	protected void copy(float value, ShaderUniformInfo shaderUniformInfo) {
		if (shaderUniformInfo.isUsed()) {
			prepareCopy(shaderUniformInfo.getOffset(), 4);
			data.putFloat(value);
		}
	}

	protected void copy(float value, ShaderUniformInfo shaderUniformInfo, int index) {
		if (shaderUniformInfo.isUsed()) {
			prepareCopy(shaderUniformInfo.getOffset() + index * 4, 4);
			data.putFloat(value);
		}
	}

	protected void copy(float[] values, ShaderUniformInfo shaderUniformInfo, int start, int end) {
		if (shaderUniformInfo.isUsed()) {
			prepareCopy(shaderUniformInfo.getOffset() + start * 4, (end - start) * 4);
			for (int i = start; i < end; i++) {
				data.putFloat(values[i]);
			}
		}
	}

	protected void copy(int[] values, ShaderUniformInfo shaderUniformInfo, int start, int end) {
		if (shaderUniformInfo.isUsed()) {
			prepareCopy(shaderUniformInfo.getOffset() + start * 4, (end - start) * 4);
			for (int i = start; i < end; i++) {
				data.putInt(values[i]);
			}
		}
	}

	protected void copy(boolean value, ShaderUniformInfo shaderUniformInfo) {
		copy(value ? 1 : 0, shaderUniformInfo);
	}

	@Override
	public void setTexEnable(int texEnable) {
		if (texEnable != getTexEnable()) {
			copy(texEnable, this.texEnable);
			super.setTexEnable(texEnable);
		}
	}

	@Override
	public void setBoneMatrix(final int count, final float[] boneMatrix) {
		if (count > 0) {
			final float[] previousBoneMatrix = getBoneMatrix();
			final int length = 16 * count;
			int start = -1;
			for (int i = 0; i < length; i++) {
				if (previousBoneMatrix[i] != boneMatrix[i]) {
					start = i;
					break;
				}
			}

			if (start >= 0) {
				int end = start + 1;
				for (int i = length - 1; i > start; i--) {
					if (previousBoneMatrix[i] != boneMatrix[i]) {
						end = i + 1;
						break;
					}
				}
				copy(boneMatrix, this.boneMatrix, start, end);

				super.setBoneMatrix(count, boneMatrix);
			}
		}
	}

	@Override
	public void setColorDoubling(float colorDoubling) {
		if (colorDoubling != getColorDoubling()) {
			copy(colorDoubling, this.colorDoubling);
			super.setColorDoubling(colorDoubling);
		}
	}

	@Override
	public void setCtestEnable(int ctestEnable) {
		if (ctestEnable != getCtestEnable()) {
			copy(ctestEnable, this.ctestEnable);
			super.setCtestEnable(ctestEnable);
		}
	}

	@Override
	public void setCtestFunc(int ctestFunc) {
		if (ctestFunc != getCtestFunc()) {
			copy(ctestFunc, this.ctestFunc);
			super.setCtestFunc(ctestFunc);
		}
	}

	@Override
	public void setCtestMsk(int index, int ctestMsk) {
		if (ctestMsk != getCtestMsk(index)) {
			copy(ctestMsk, this.ctestMsk, index);
			super.setCtestMsk(index, ctestMsk);
		}
	}

	@Override
	public void setCtestRef(int index, int ctestRef) {
		if (ctestRef != getCtestRef(index)) {
			copy(ctestRef, this.ctestRef, index);
			super.setCtestRef(index, ctestRef);
		}
	}

	@Override
	public void setLightEnabled(int light, int lightEnabled) {
		if (lightEnabled != getLightEnabled(light)) {
			copy(lightEnabled, this.lightEnabled, light);
			super.setLightEnabled(light, lightEnabled);
		}
	}

	@Override
	public void setLightingEnable(int lightingEnable) {
		if (lightingEnable != getLightingEnable()) {
			copy(lightingEnable, this.lightingEnable);
			super.setLightingEnable(lightingEnable);
		}
	}

	@Override
	public void setLightKind(int light, int lightKind) {
		if (lightKind != getLightKind(light)) {
			copy(lightKind, this.lightKind, light);
			super.setLightKind(light, lightKind);
		}
	}

	@Override
	public void setLightMode(int lightMode) {
		if (lightMode != getLightMode()) {
			copy(lightMode, this.lightMode);
			super.setLightMode(lightMode);
		}
	}

	@Override
	public void setLightType(int light, int lightType) {
		if (lightType != getLightType(light)) {
			copy(lightType, this.lightType, light);
			super.setLightType(light, lightType);
		}
	}

	@Override
	public void setMatFlags(int index, int matFlags) {
		if (matFlags != getMatFlags(index)) {
			copy(matFlags, this.matFlags, index);
			super.setMatFlags(index, matFlags);
		}
	}

	@Override
	public void setNormalScale(float normalScale) {
		if (normalScale != getNormalScale()) {
			copy(normalScale, this.normalScale);
			super.setNormalScale(normalScale);
		}
	}

	@Override
	public void setNumberBones(int numberBones) {
		if (numberBones != getNumberBones()) {
			copy(numberBones, this.numberBones);
			super.setNumberBones(numberBones);
		}
	}

	@Override
	public void setPositionScale(float positionScale) {
		if (positionScale != getPositionScale()) {
			copy(positionScale, this.positionScale);
			super.setPositionScale(positionScale);
		}
	}

	@Override
	public void setTexEnvMode(int index, int texEnvMode) {
		if (texEnvMode != getTexEnvMode(index)) {
			copy(texEnvMode, this.texEnvMode, index);
			super.setTexEnvMode(index, texEnvMode);
		}
	}

	@Override
	public void setTexMapMode(int texMapMode) {
		if (texMapMode != getTexMapMode()) {
			copy(texMapMode, this.texMapMode);
			super.setTexMapMode(texMapMode);
		}
	}

	@Override
	public void setTexMapProj(int texMapProj) {
		if (texMapProj != getTexMapProj()) {
			copy(texMapProj, this.texMapProj);
			super.setTexMapProj(texMapProj);
		}
	}

	@Override
	public void setTexShade(int index, int texShade) {
		if (texShade != getTexShade(index)) {
			copy(texShade, this.texShade, index);
			super.setTexShade(index, texShade);
		}
	}

	@Override
	public void setTextureScale(float textureScale) {
		if (textureScale != getTextureScale()) {
			copy(textureScale, this.textureScale);
			super.setTextureScale(textureScale);
		}
	}

	@Override
	public void setVinfoColor(int vinfoColor) {
		if (vinfoColor != getVinfoColor()) {
			copy(vinfoColor, this.vinfoColor);
			super.setVinfoColor(vinfoColor);
		}
	}

	@Override
	public void setVinfoPosition(int vinfoPosition) {
		if (vinfoPosition != getVinfoPosition()) {
			copy(vinfoPosition, this.vinfoPosition);
			super.setVinfoPosition(vinfoPosition);
		}
	}

	@Override
	public void setVinfoTransform2D(int vinfoTransform2D) {
		if (vinfoTransform2D != getVinfoTransform2D()) {
			copy(vinfoTransform2D, this.vinfoTransform2D);
			super.setVinfoTransform2D(vinfoTransform2D);
		}
	}

	@Override
	public void setWeightScale(float weightScale) {
		if (weightScale != getWeightScale()) {
			copy(weightScale, this.weightScale);
			super.setWeightScale(weightScale);
		}
	}

	@Override
	public void setClutShift(int clutShift) {
		if (clutShift != getClutShift()) {
			copy(clutShift, this.clutShift);
			super.setClutShift(clutShift);
		}
	}

	@Override
	public void setClutMask(int clutMask) {
		if (clutMask != getClutMask()) {
			copy(clutMask, this.clutMask);
			super.setClutMask(clutMask);
		}
	}

	@Override
	public void setClutOffset(int clutOffset) {
		if (clutOffset != getClutOffset()) {
			copy(clutOffset, this.clutOffset);
			super.setClutOffset(clutOffset);
		}
	}

	@Override
	public void setMipmapShareClut(boolean mipmapShareClut) {
		if (mipmapShareClut != isMipmapShareClut()) {
			copy(mipmapShareClut, this.mipmapShareClut);
			super.setMipmapShareClut(mipmapShareClut);
		}
	}

	@Override
	public void setTexPixelFormat(int texPixelFormat) {
		if (texPixelFormat != getTexPixelFormat()) {
			copy(texPixelFormat, this.texPixelFormat);
			super.setTexPixelFormat(texPixelFormat);
		}
	}

	@Override
	public void setVertexColor(float[] vertexColor) {
		float[] currentVertexColor = getVertexColor();
		if (vertexColor[0] != currentVertexColor[0] || vertexColor[1] != currentVertexColor[1] || vertexColor[2] != currentVertexColor[2] || vertexColor[3] != currentVertexColor[3]) {
			copy(vertexColor, this.vertexColor, 0, 4);
			super.setVertexColor(vertexColor);
		}
	}

	@Override
	public void setVinfoTexture(int vinfoTexture) {
		if (vinfoTexture != getVinfoTexture()) {
			copy(vinfoTexture, this.vinfoTexture);
			super.setVinfoTexture(vinfoTexture);
		}
	}

	@Override
	public void setVinfoNormal(int vinfoNormal) {
		if (vinfoNormal != getVinfoNormal()) {
			copy(vinfoNormal, this.vinfoNormal);
			super.setVinfoNormal(vinfoNormal);
		}
	}

	@Override
	public void setStencilTestEnable(int stencilTestEnable) {
		if (stencilTestEnable != getStencilTestEnable()) {
			copy(stencilTestEnable, this.stencilTestEnable);
			super.setStencilTestEnable(stencilTestEnable);
		}
	}

	@Override
	public void setStencilFunc(int stencilFunc) {
		if (stencilFunc != getStencilFunc()) {
			copy(stencilFunc, this.stencilFunc);
			super.setStencilFunc(stencilFunc);
		}
	}

	@Override
	public void setStencilMask(int stencilMask) {
		if (stencilMask != getStencilMask()) {
			copy(stencilMask, this.stencilMask);
			super.setStencilMask(stencilMask);
		}
	}

	@Override
	public void setStencilOpFail(int stencilOpFail) {
		if (stencilOpFail != getStencilOpFail()) {
			copy(stencilOpFail, this.stencilOpFail);
			super.setStencilOpFail(stencilOpFail);
		}
	}

	@Override
	public void setStencilOpZFail(int stencilOpZFail) {
		if (stencilOpZFail != getStencilOpZFail()) {
			copy(stencilOpZFail, this.stencilOpZFail);
			super.setStencilOpZFail(stencilOpZFail);
		}
	}

	@Override
	public void setStencilOpZPass(int stencilOpZPass) {
		if (stencilOpZPass != getStencilOpZPass()) {
			copy(stencilOpZPass, this.stencilOpZPass);
			super.setStencilOpZPass(stencilOpZPass);
		}
	}
        
	@Override
	public void setDepthTestEnable(int depthTestEnable) {
		if (depthTestEnable != getDepthTestEnable()) {
			copy(depthTestEnable, this.depthTestEnable);
			super.setDepthTestEnable(depthTestEnable);
		}
	}

	@Override
	public void setDepthFunc(int depthFunc) {
		if (depthFunc != getDepthFunc()) {
			copy(depthFunc, this.depthFunc);
			super.setDepthFunc(depthFunc);
		}
	}

	@Override
	public void setDepthMask(int depthMask) {
		if (depthMask != getDepthMask()) {
			copy(depthMask, this.depthMask);
			super.setDepthMask(depthMask);
		}
	}

	@Override
	public void setStencilRef(int stencilRef) {
		if (stencilRef != getStencilRef()) {
			copy(stencilRef, this.stencilRef);
			super.setStencilRef(stencilRef);
		}
	}

	@Override
	public void setColorMaskEnable(int colorMaskEnable) {
		if (colorMaskEnable != getColorMaskEnable()) {
			copy(colorMaskEnable, this.colorMaskEnable);
			super.setColorMaskEnable(colorMaskEnable);
		}
	}

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
		int[] currentColorMask = getColorMask();
		if (redMask != currentColorMask[0] || greenMask != currentColorMask[1] || blueMask != currentColorMask[2] || alphaMask != currentColorMask[3]) {
			copy(new int[] { redMask, greenMask, blueMask, alphaMask }, this.colorMask, 0, 4);
			super.setColorMask(redMask, greenMask, blueMask, alphaMask);
		}
	}

	@Override
	public void setNotColorMask(int notRedMask, int notGreenMask, int notBlueMask, int notAlphaMask) {
		int[] currentNotColorMask = getNotColorMask();
		if (notRedMask != currentNotColorMask[0] || notGreenMask != currentNotColorMask[1] || notBlueMask != currentNotColorMask[2] || notAlphaMask != currentNotColorMask[3]) {
			copy(new int[] { notRedMask, notGreenMask, notBlueMask, notAlphaMask }, this.notColorMask, 0, 4);
			super.setNotColorMask(notRedMask, notGreenMask, notBlueMask, notAlphaMask);
		}
	}

	@Override
	public void setAlphaTestEnable(int alphaTestEnable) {
		if (alphaTestEnable != getAlphaTestEnable()) {
			copy(alphaTestEnable, this.alphaTestEnable);
			super.setAlphaTestEnable(alphaTestEnable);
		}
	}

	@Override
	public void setAlphaTestFunc(int alphaTestFunc) {
		if (alphaTestFunc != getAlphaTestFunc()) {
			copy(alphaTestFunc, this.alphaTestFunc);
			super.setAlphaTestFunc(alphaTestFunc);
		}
	}

	@Override
	public void setAlphaTestRef(int alphaTestRef) {
		if (alphaTestRef != getAlphaTestRef()) {
			copy(alphaTestRef, this.alphaTestRef);
			super.setAlphaTestRef(alphaTestRef);
		}
	}

	@Override
	public void setBlendTestEnable(int blendTestEnable) {
		if (blendTestEnable != getBlendTestEnable()) {
			copy(blendTestEnable, this.blendTestEnable);
			super.setBlendTestEnable(blendTestEnable);
		}
	}

	@Override
	public void setBlendEquation(int blendEquation) {
		if (blendEquation != getBlendEquation()) {
			copy(blendEquation, this.blendEquation);
			super.setBlendEquation(blendEquation);
		}
	}

	@Override
	public void setBlendSrc(int blendSrc) {
		if (blendSrc != getBlendSrc()) {
			copy(blendSrc, this.blendSrc);
			super.setBlendSrc(blendSrc);
		}
	}

	@Override
	public void setBlendDst(int blendDst) {
		if (blendDst != getBlendDst()) {
			copy(blendDst, this.blendDst);
			super.setBlendDst(blendDst);
		}
	}

	@Override
	public void setBlendSFix(float[] blendSFix) {
		float[] sfix = getBlendSFix();
		if (blendSFix[0] != sfix[0] || blendSFix[1] != sfix[1] || blendSFix[2] != sfix[2]) {
			copy(blendSFix, this.blendSFix, 0, 3);
			super.setBlendSFix(blendSFix);
		}
	}

	@Override
	public void setBlendDFix(float[] blendDFix) {
		float[] dfix = getBlendDFix();
		if (blendDFix[0] != dfix[0] || blendDFix[1] != dfix[1] || blendDFix[2] != dfix[2]) {
			copy(blendDFix, this.blendDFix, 0, 3);
			super.setBlendDFix(blendDFix);
		}
	}

	@Override
	public void setCopyRedToAlpha(int copyRedToAlpha) {
		if (copyRedToAlpha != getCopyRedToAlpha()) {
			copy(copyRedToAlpha, this.copyRedToAlpha);
			super.setCopyRedToAlpha(copyRedToAlpha);
		}
	}

	@Override
	public void setWrapModeS(int wrapModeS) {
		if (wrapModeS != getWrapModeS()) {
			copy(wrapModeS, this.wrapModeS);
			super.setWrapModeS(wrapModeS);
		}
	}

	@Override
	public void setWrapModeT(int wrapModeT) {
		if (wrapModeT != getWrapModeT()) {
			copy(wrapModeT, this.wrapModeT);
			super.setWrapModeT(wrapModeT);
		}
	}
}