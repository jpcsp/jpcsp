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
import java.util.Arrays;

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
	private static final int OFFSET_START             = 0;
	private static final int OFFSET_LIGHT_TYPE        = OFFSET_START;
	private static final int OFFSET_LIGHT_KIND        = OFFSET_LIGHT_TYPE + 4 * 4;
	private static final int OFFSET_LIGHT_ENABLED     = OFFSET_LIGHT_KIND + 4 * 4;
	private static final int OFFSET_MAT_FLAGS         = OFFSET_LIGHT_ENABLED + 4 * 4;
	private static final int OFFSET_CTEST_REF         = OFFSET_MAT_FLAGS + 4 * 4;
	private static final int OFFSET_CTEST_MSK         = OFFSET_CTEST_REF + 4 * 4;
	private static final int OFFSET_TEX_SHADE         = OFFSET_CTEST_MSK + 4 * 4;
	private static final int OFFSET_TEX_ENV_MODE      = OFFSET_TEX_SHADE + 2 * 4;
	private static final int OFFSET_CTEST_FUNC        = OFFSET_TEX_ENV_MODE + 2 * 4;
	private static final int OFFSET_TEX_MAP_MODE      = OFFSET_CTEST_FUNC + 4;
	private static final int OFFSET_TEX_MAP_PROJ      = OFFSET_TEX_MAP_MODE + 4;
	private static final int OFFSET_VINFO_COLOR       = OFFSET_TEX_MAP_PROJ + 4;
	private static final int OFFSET_VINFO_POSITION    = OFFSET_VINFO_COLOR + 4;
	private static final int OFFSET_POSITION_SCALE    = OFFSET_VINFO_POSITION + 4;
	private static final int OFFSET_NORMAL_SCALE      = OFFSET_POSITION_SCALE + 4;
	private static final int OFFSET_TEXTURE_SCALE     = OFFSET_NORMAL_SCALE + 4;
	private static final int OFFSET_WEIGHT_SCALE      = OFFSET_TEXTURE_SCALE + 4;
	private static final int OFFSET_COLOR_DOUBLING    = OFFSET_WEIGHT_SCALE + 4;
	private static final int OFFSET_TEX_ENABLE        = OFFSET_COLOR_DOUBLING + 4;
	private static final int OFFSET_LIGHTING_ENABLE   = OFFSET_TEX_ENABLE + 4;
	private static final int OFFSET_VINFO_TRANSFORM2D = OFFSET_LIGHTING_ENABLE + 4;
	private static final int OFFSET_CTEST_ENABLE      = OFFSET_VINFO_TRANSFORM2D + 4;
	private static final int OFFSET_COLOR_ADDITION    = OFFSET_CTEST_ENABLE + 4;
	private static final int OFFSET_CLUT_SHIFT        = OFFSET_COLOR_ADDITION + 4;
	private static final int OFFSET_CLUT_MASK         = OFFSET_CLUT_SHIFT + 4;
	private static final int OFFSET_CLUT_OFFSET       = OFFSET_CLUT_MASK + 4;
	private static final int OFFSET_MIPMAP_SHARE_CLUT = OFFSET_CLUT_OFFSET + 4;
	private static final int OFFSET_NUMBER_BONES      = OFFSET_MIPMAP_SHARE_CLUT + 4;
	private static final int OFFSET_BONE_MATRIX       = OFFSET_NUMBER_BONES + 4;
	private static final int OFFSET_END               = OFFSET_BONE_MATRIX + 8 * 4 * 4 * 4;
	private static final int bufferSize = OFFSET_END - OFFSET_START;
	protected static final int bindingPoint = 0;
	protected static final String uniformBlockName = "psp";
	protected int buffer;
	protected ByteBuffer data;
	private int startUpdate;
	private int endUpdate;
	private static String shaderUniformText;

	private static class ShaderUniformInfo implements Comparable<ShaderUniformInfo> {
		public String name;
		public String type;
		private int offset;

		public ShaderUniformInfo(Uniforms uniform, String type, int offset) {
			this.name = uniform.getUniformString();
			this.type = type;
			this.offset = offset;
		}

		public ShaderUniformInfo(Uniforms uniform, String type, int offset, int matrixSize) {
			this.name = String.format("%s[%d]", uniform.getUniformString(), matrixSize);
			this.type = type;
			this.offset = offset;
		}

		@Override
		public int compareTo(ShaderUniformInfo o) {
			return offset - o.offset;
		}
	}

	public static boolean useUBO(IRenderingEngine re) {
        return !Settings.getInstance().readBool("emu.disableubo")
            && re.isExtensionAvailable("GL_ARB_uniform_buffer_object");
	}

	public static String getShaderUniformText() {
		if (shaderUniformText == null) {
			if ((OFFSET_BONE_MATRIX % 16) != 0) {
				VideoEngine.log.error(String.format("ShaderContextUBO: bone matrix has to be 16bytes-aligned (offset=%d)", OFFSET_BONE_MATRIX));
			}

			StringBuilder s = new StringBuilder();
			ArrayList<ShaderUniformInfo> shaderUniformInfos = new ArrayList<ShaderUniformInfo>();

			// Add all the shader uniform objects in any order,
			// they will be sorted by offset afterwards
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.lightType,        "ivec4", OFFSET_LIGHT_TYPE));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.lightKind,        "ivec4", OFFSET_LIGHT_KIND));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.lightEnabled,     "ivec4", OFFSET_LIGHT_ENABLED));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.matFlags,         "ivec3", OFFSET_MAT_FLAGS));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.ctestRef,         "ivec3", OFFSET_CTEST_REF));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.ctestMsk,         "ivec3", OFFSET_CTEST_MSK));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.texShade,         "ivec2", OFFSET_TEX_SHADE));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.texEnvMode,       "ivec2", OFFSET_TEX_ENV_MODE));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.ctestFunc,        "int",   OFFSET_CTEST_FUNC));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.texMapMode,       "int",   OFFSET_TEX_MAP_MODE));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.texMapProj,       "int",   OFFSET_TEX_MAP_PROJ));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.vinfoColor,       "int",   OFFSET_VINFO_COLOR));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.vinfoPosition,    "int",   OFFSET_VINFO_POSITION));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.positionScale,    "float", OFFSET_POSITION_SCALE));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.normalScale,      "float", OFFSET_NORMAL_SCALE));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.textureScale,     "float", OFFSET_TEXTURE_SCALE));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.weightScale,      "float", OFFSET_WEIGHT_SCALE));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.colorDoubling,    "float", OFFSET_COLOR_DOUBLING));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.texEnable,        "bool",  OFFSET_TEX_ENABLE));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.lightingEnable,   "bool",  OFFSET_LIGHTING_ENABLE));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.vinfoTransform2D, "bool",  OFFSET_VINFO_TRANSFORM2D));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.ctestEnable,      "bool",  OFFSET_CTEST_ENABLE));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.lightMode,        "bool",  OFFSET_COLOR_ADDITION));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.clutShift,        "int",   OFFSET_CLUT_SHIFT));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.clutMask,         "int",   OFFSET_CLUT_MASK));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.clutOffset,       "int",   OFFSET_CLUT_OFFSET));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.mipmapShareClut,  "bool",  OFFSET_MIPMAP_SHARE_CLUT));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.numberBones,      "int",   OFFSET_NUMBER_BONES));
			shaderUniformInfos.add(new ShaderUniformInfo(Uniforms.boneMatrix,       "mat4",  OFFSET_BONE_MATRIX, 8));

			// Sort the shader uniform objects by offset
			ShaderUniformInfo[] sortedShaderUniformInfos = new ShaderUniformInfo[shaderUniformInfos.size()];
			shaderUniformInfos.toArray(sortedShaderUniformInfos);
			Arrays.sort(sortedShaderUniformInfos);

			s.append(String.format("layout(std140) uniform %s\n", uniformBlockName));
			s.append(String.format("{\n"));
			for (ShaderUniformInfo shaderUniformInfo : sortedShaderUniformInfos) {
				s.append(String.format("   %s %s;\n", shaderUniformInfo.type, shaderUniformInfo.name));
			}
			s.append(String.format("};\n"));

			shaderUniformText = s.toString();
		}

		return shaderUniformText;
	}

	public ShaderContextUBO(IRenderingEngine re) {
		data = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder());
		buffer = re.genBuffer();
		re.bindBuffer(IRenderingEngine.RE_UNIFORM_BUFFER, buffer);
		re.setBufferData(IRenderingEngine.RE_UNIFORM_BUFFER, bufferSize, null, IRenderingEngine.RE_DYNAMIC_DRAW);
		re.bindBufferBase(IRenderingEngine.RE_UNIFORM_BUFFER, bindingPoint, buffer);
		startUpdate = OFFSET_START;
		endUpdate = OFFSET_END;
	}

	@Override
	public void initShaderProgram(IRenderingEngine re, int shaderProgram) {
		int blockIndex = re.getUniformBlockIndex(shaderProgram, uniformBlockName);
		re.setUniformBlockBinding(shaderProgram, blockIndex, bindingPoint);

		super.initShaderProgram(re, shaderProgram);
	}

	@Override
	public void setUniforms(IRenderingEngine re, int shaderProgram) {
		if (startUpdate < endUpdate) {
			re.bindBuffer(IRenderingEngine.RE_UNIFORM_BUFFER, buffer);

			data.position(startUpdate);
			re.setBufferSubData(IRenderingEngine.RE_UNIFORM_BUFFER, startUpdate, endUpdate - startUpdate, data);
			data.limit(data.capacity());
			startUpdate = OFFSET_END;
			endUpdate = OFFSET_START;
		}
		re.setUniform(Uniforms.clut.getId(shaderProgram), getClut());
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

	protected void copy(int value, int offset) {
		prepareCopy(offset, 4);
		data.putInt(value);
	}

	protected void copy(int value, int offset, int index) {
		prepareCopy(offset + index * 4, 4);
		data.putInt(value);
	}

	protected void copy(float value, int offset) {
		prepareCopy(offset, 4);
		data.putFloat(value);
	}

	protected void copy(float value, int offset, int index) {
		prepareCopy(offset + index * 4, 4);
		data.putFloat(value);
	}

	protected void copy(float[] values, int offset, int start, int end) {
		prepareCopy(offset + start * 4, (end - start) * 4);
		for (int i = start; i < end; i++) {
			data.putFloat(values[i]);
		}
	}

	protected void copy(boolean value, int offset) {
		copy(value ? 1 : 0, offset);
	}

	@Override
	public void setTexEnable(int texEnable) {
		if (texEnable != getTexEnable()) {
			copy(texEnable, OFFSET_TEX_ENABLE);
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
				copy(boneMatrix, OFFSET_BONE_MATRIX, start, end);

				super.setBoneMatrix(count, boneMatrix);
			}
		}
	}

	@Override
	public void setColorDoubling(float colorDoubling) {
		if (colorDoubling != getColorDoubling()) {
			copy(colorDoubling, OFFSET_COLOR_DOUBLING);
			super.setColorDoubling(colorDoubling);
		}
	}

	@Override
	public void setCtestEnable(int ctestEnable) {
		if (ctestEnable != getCtestEnable()) {
			copy(ctestEnable, OFFSET_CTEST_ENABLE);
			super.setCtestEnable(ctestEnable);
		}
	}

	@Override
	public void setCtestFunc(int ctestFunc) {
		if (ctestFunc != getCtestFunc()) {
			copy(ctestFunc, OFFSET_CTEST_FUNC);
			super.setCtestFunc(ctestFunc);
		}
	}

	@Override
	public void setCtestMsk(int index, int ctestMsk) {
		if (ctestMsk != getCtestMsk(index)) {
			copy(ctestMsk, OFFSET_CTEST_MSK, index);
			super.setCtestMsk(index, ctestMsk);
		}
	}

	@Override
	public void setCtestRef(int index, int ctestRef) {
		if (ctestRef != getCtestRef(index)) {
			copy(ctestRef, OFFSET_CTEST_REF, index);
			super.setCtestRef(index, ctestRef);
		}
	}

	@Override
	public void setLightEnabled(int light, int lightEnabled) {
		if (lightEnabled != getLightEnabled(light)) {
			copy(lightEnabled, OFFSET_LIGHT_ENABLED, light);
			super.setLightEnabled(light, lightEnabled);
		}
	}

	@Override
	public void setLightingEnable(int lightingEnable) {
		if (lightingEnable != getLightingEnable()) {
			copy(lightingEnable, OFFSET_LIGHTING_ENABLE);
			super.setLightingEnable(lightingEnable);
		}
	}

	@Override
	public void setLightKind(int light, int lightKind) {
		if (lightKind != getLightKind(light)) {
			copy(lightKind, OFFSET_LIGHT_KIND, light);
			super.setLightKind(light, lightKind);
		}
	}

	@Override
	public void setLightMode(int lightMode) {
		if (lightMode != getLightMode()) {
			copy(lightMode, OFFSET_COLOR_ADDITION);
			super.setLightMode(lightMode);
		}
	}

	@Override
	public void setLightType(int light, int lightType) {
		if (lightType != getLightType(light)) {
			copy(lightType, OFFSET_LIGHT_TYPE, light);
			super.setLightType(light, lightType);
		}
	}

	@Override
	public void setMatFlags(int index, int matFlags) {
		if (matFlags != getMatFlags(index)) {
			copy(matFlags, OFFSET_MAT_FLAGS, index);
			super.setMatFlags(index, matFlags);
		}
	}

	@Override
	public void setNormalScale(float normalScale) {
		if (normalScale != getNormalScale()) {
			copy(normalScale, OFFSET_NORMAL_SCALE);
			super.setNormalScale(normalScale);
		}
	}

	@Override
	public void setNumberBones(int numberBones) {
		if (numberBones != getNumberBones()) {
			copy(numberBones, OFFSET_NUMBER_BONES);
			super.setNumberBones(numberBones);
		}
	}

	@Override
	public void setPositionScale(float positionScale) {
		if (positionScale != getPositionScale()) {
			copy(positionScale, OFFSET_POSITION_SCALE);
			super.setPositionScale(positionScale);
		}
	}

	@Override
	public void setTexEnvMode(int index, int texEnvMode) {
		if (texEnvMode != getTexEnvMode(index)) {
			copy(texEnvMode, OFFSET_TEX_ENV_MODE, index);
			super.setTexEnvMode(index, texEnvMode);
		}
	}

	@Override
	public void setTexMapMode(int texMapMode) {
		if (texMapMode != getTexMapMode()) {
			copy(texMapMode, OFFSET_TEX_MAP_MODE);
			super.setTexMapMode(texMapMode);
		}
	}

	@Override
	public void setTexMapProj(int texMapProj) {
		if (texMapProj != getTexMapProj()) {
			copy(texMapProj, OFFSET_TEX_MAP_PROJ);
			super.setTexMapProj(texMapProj);
		}
	}

	@Override
	public void setTexShade(int index, int texShade) {
		if (texShade != getTexShade(index)) {
			copy(texShade, OFFSET_TEX_SHADE, index);
			super.setTexShade(index, texShade);
		}
	}

	@Override
	public void setTextureScale(float textureScale) {
		if (textureScale != getTextureScale()) {
			copy(textureScale, OFFSET_TEXTURE_SCALE);
			super.setTextureScale(textureScale);
		}
	}

	@Override
	public void setVinfoColor(int vinfoColor) {
		if (vinfoColor != getVinfoColor()) {
			copy(vinfoColor, OFFSET_VINFO_COLOR);
			super.setVinfoColor(vinfoColor);
		}
	}

	@Override
	public void setVinfoPosition(int vinfoPosition) {
		if (vinfoPosition != getVinfoPosition()) {
			copy(vinfoPosition, OFFSET_VINFO_POSITION);
			super.setVinfoPosition(vinfoPosition);
		}
	}

	@Override
	public void setVinfoTransform2D(int vinfoTransform2D) {
		if (vinfoTransform2D != getVinfoTransform2D()) {
			copy(vinfoTransform2D, OFFSET_VINFO_TRANSFORM2D);
			super.setVinfoTransform2D(vinfoTransform2D);
		}
	}

	@Override
	public void setWeightScale(float weightScale) {
		if (weightScale != getWeightScale()) {
			copy(weightScale, OFFSET_WEIGHT_SCALE);
			super.setWeightScale(weightScale);
		}
	}

	@Override
	public void setClutShift(int clutShift) {
		if (clutShift != getClutShift()) {
			copy(clutShift, OFFSET_CLUT_SHIFT);
			super.setClutShift(clutShift);
		}
	}

	@Override
	public void setClutMask(int clutMask) {
		if (clutMask != getClutMask()) {
			copy(clutMask, OFFSET_CLUT_MASK);
			super.setClutMask(clutMask);
		}
	}

	@Override
	public void setClutOffset(int clutOffset) {
		if (clutOffset != getClutOffset()) {
			copy(clutOffset, OFFSET_CLUT_OFFSET);
			super.setClutOffset(clutOffset);
		}
	}

	@Override
	public void setMipmapShareClut(boolean mipmapShareClut) {
		if (mipmapShareClut != isMipmapShareClut()) {
			copy(mipmapShareClut, OFFSET_MIPMAP_SHARE_CLUT);
			super.setMipmapShareClut(mipmapShareClut);
		}
	}
}
