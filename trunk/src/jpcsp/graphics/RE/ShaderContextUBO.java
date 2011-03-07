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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import jpcsp.Settings;
import jpcsp.graphics.Uniforms;
import jpcsp.graphics.VideoEngine;

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

		public ShaderUniformInfo(Uniforms uniform, String type) {
			name = uniform.getUniformString();
			structureName = this.name;
			this.type = type;
		}

		public ShaderUniformInfo(Uniforms uniform, String type, int matrixSize) {
			name = uniform.getUniformString();
			structureName = String.format("%s[%d]", name, matrixSize);
			this.type = type;
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

		@Override
		public String toString() {
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
		re.setUniformBlockBinding(shaderProgram, blockIndex, bindingPoint);

		if (data == null) {
			buffer = re.genBuffer();
			re.bindBuffer(IRenderingEngine.RE_UNIFORM_BUFFER, buffer);
			re.bindBufferBase(IRenderingEngine.RE_UNIFORM_BUFFER, bindingPoint, buffer);
		}

		if (data == null) {
			for (ShaderUniformInfo shaderUniformInfo : shaderUniformInfos) {
				int index = re.getUniformIndex(shaderProgram, shaderUniformInfo.getName());
				int offset = re.getActiveUniformOffset(shaderProgram, index);
				shaderUniformInfo.setOffset(offset);
			}

			// The size returned by
			//    glGetActiveUniformBlock(program, blockIndex, ARBUniformBufferObject.GL_UNIFORM_BLOCK_DATA_SIZE)
			// is not reliable as the driver is free to reduce array sizes when they
			// are not used in the shader.
			// Use a dummy element of the structure to find the total structure size.
			bufferSize = endOfUBO.getOffset() + 4;
			if (VideoEngine.log.isDebugEnabled()) {
				VideoEngine.log.debug(String.format("UBO Structure size: %d (including endOfUBO)", bufferSize));
			}

			data = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
			re.setBufferData(IRenderingEngine.RE_UNIFORM_BUFFER, bufferSize, data, IRenderingEngine.RE_DYNAMIC_DRAW);
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
		prepareCopy(shaderUniformInfo.getOffset(), 4);
		data.putInt(value);
	}

	protected void copy(int value, ShaderUniformInfo shaderUniformInfo, int index) {
		prepareCopy(shaderUniformInfo.getOffset() + index * 4, 4);
		data.putInt(value);
	}

	protected void copy(float value, ShaderUniformInfo shaderUniformInfo) {
		prepareCopy(shaderUniformInfo.getOffset(), 4);
		data.putFloat(value);
	}

	protected void copy(float value, ShaderUniformInfo shaderUniformInfo, int index) {
		prepareCopy(shaderUniformInfo.getOffset() + index * 4, 4);
		data.putFloat(value);
	}

	protected void copy(float[] values, ShaderUniformInfo shaderUniformInfo, int start, int end) {
		prepareCopy(shaderUniformInfo.getOffset() + start * 4, (end - start) * 4);
		for (int i = start; i < end; i++) {
			data.putFloat(values[i]);
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
}
