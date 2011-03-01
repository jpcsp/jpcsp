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
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jpcsp.Settings;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.Uniforms;
import jpcsp.graphics.VertexInfo;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.textures.Texture;
import jpcsp.graphics.textures.TextureCache;
import jpcsp.util.CpuDurationStatistics;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 * This RenderingEngine class implements the required logic
 * to use OpenGL vertex and fragment shaders, i.e. without using
 * the OpenGL fixed-function pipeline.
 * If geometry shaders are available, implement the GU_SPRITES primitive by
 * inserting a geometry shader into the shader program.
 * 
 * This class is implemented as a Proxy, forwarding the non-relevant calls
 * to the proxy.
 */
public class REShader extends BaseRenderingEngineFunction {
	protected final static int ACTIVE_TEXTURE_NORMAL = 0;
	protected final static int ACTIVE_TEXTURE_CLUT = 1;
	protected final static float[] positionScale = new float[] { 1, 0x7F, 0x7FFF, 1 };
	protected final static float[] normalScale   = new float[] { 1, 0x7F, 0x7FFF, 1 };
	protected final static float[] textureScale  = new float[] { 1, 0x80, 0x8000, 1 };
	protected final static float[] weightScale   = new float[] { 1, 0x80, 0x8000, 1 };
	protected ShaderContext shaderContext;
	protected int shaderProgram;
	protected int spriteShaderProgram;
	protected int vertexShader;
	protected int geometryShader;
	protected int fragmentShader;
	protected int shaderAttribPosition;
	protected int shaderAttribNormal;
	protected int shaderAttribColor;
	protected int shaderAttribTexture;
	protected int shaderAttribWeights1;
	protected int shaderAttribWeights2;
	protected int numberOfWeightsForShader;
	protected final static int spriteGeometryShaderInputType = GU_LINES;
	protected final static int spriteGeometryShaderOutputType = GU_TRIANGLE_STRIP;
	protected boolean useGeometryShader = true;
	protected boolean useUniformBufferObject = true;
	protected boolean useNativeClut = true;
	protected int clutTextureId = -1;
	protected ByteBuffer clutBuffer;
    protected DurationStatistics textureCacheLookupStatistics = new CpuDurationStatistics("Lookup in TextureCache for CLUTs");

	public REShader(IRenderingEngine proxy) {
		super(proxy);
		initShader();
	}

	protected void initShader() {
		useGeometryShader = Settings.getInstance().readBool("emu.useGeometryShader");

		log.info("Using shaders with Skinning");

		if (!re.isExtensionAvailable("GL_ARB_geometry_shader4")) {
			useGeometryShader = false;
		}
		if (useGeometryShader) {
			log.info("Using Geometry Shader for SPRITES");
		}

		if (!ShaderContextUBO.useUBO(re)) {
			useUniformBufferObject = false;
		}
		if (useUniformBufferObject) {
			log.info("Using Uniform Buffer Object (UBO)");
		}

        useNativeClut = Settings.getInstance().readBool("emu.enablenativeclut");
        if (useNativeClut) {
        	if (!super.canNativeClut()) {
    			log.warn("Disabling Native Color Lookup Tables (CLUT)");
        		useNativeClut = false;
        	} else {
    			log.info("Using Native Color Lookup Tables (CLUT)");
        	}
        }

		loadShaders();

		if (useUniformBufferObject) {
			shaderContext = new ShaderContextUBO(re);
		} else {
			shaderContext = new ShaderContext();
		}

		shaderContext.initShaderProgram(re, shaderProgram);

		for (Uniforms uniform : Uniforms.values()) {
            uniform.allocateId(re, shaderProgram);
            if (useGeometryShader) {
            	uniform.allocateId(re, spriteShaderProgram);
            }
        }

        shaderAttribWeights1 = re.getAttribLocation(shaderProgram, "weights1");
        shaderAttribWeights2 = re.getAttribLocation(shaderProgram, "weights2");
        shaderAttribPosition = re.getAttribLocation(shaderProgram, "position");
        shaderAttribNormal   = re.getAttribLocation(shaderProgram, "normal");
        shaderAttribColor    = re.getAttribLocation(shaderProgram, "color");
        shaderAttribTexture  = re.getAttribLocation(shaderProgram, "texture");

		shaderContext.setColorDoubling(1);

		shaderContext.setTex(ACTIVE_TEXTURE_NORMAL);
		if (useNativeClut) {
			shaderContext.setClut(ACTIVE_TEXTURE_CLUT);
			shaderContext.setUtex(ACTIVE_TEXTURE_NORMAL);
			clutBuffer = ByteBuffer.allocateDirect(4096 * 4).order(ByteOrder.LITTLE_ENDIAN);
		}
	}

	protected void addDefine(StringBuilder defines, String name, String value) {
		defines.append(String.format("#define %s %s%s", name, escapeString(value), System.getProperty("line.separator")));
	}

	protected void addDefine(StringBuilder defines, String name, int value) {
		addDefine(defines, name, Integer.toString(value));
	}

	protected void addDefine(StringBuilder defines, String name, boolean value) {
		addDefine(defines, name, value ? 1 : 0);
	}

	protected void replace(StringBuilder s, String oldText, String newText) {
		int offset = s.indexOf(oldText);
		if (offset >= 0) {
			s.replace(offset, offset + oldText.length(), newText);
		}
	}

	protected String escapeString(String s) {
		return s.replace('\n', ' ');
	}

	protected void preprocessShader(StringBuilder src) {
		StringBuilder defines = new StringBuilder();
		int shaderVersion = 120;

		addDefine(defines, "USE_GEOMETRY_SHADER", useGeometryShader);
		addDefine(defines, "USE_UBO", useUniformBufferObject);
		if (useUniformBufferObject) {
			// UBO requires at least shader version 1.40
			shaderVersion = Math.max(140, shaderVersion);
			addDefine(defines, "UBO_STRUCTURE", ShaderContextUBO.getShaderUniformText());
		}
		addDefine(defines, "USE_NATIVE_CLUT", useNativeClut);

		if (useNativeClut) {
			// Native clut requires at least shader version 1.30
			shaderVersion = Math.max(130, shaderVersion);
		}

		if (shaderContext != null) {
			addDefine(defines, "VINFO_TRANSFORM2D", shaderContext.getVinfoTransform2D());
			addDefine(defines, "VINFO_COLOR", shaderContext.getVinfoColor());
			addDefine(defines, "VINFO_POSITION", shaderContext.getVinfoPosition());
			addDefine(defines, "COLOR_ADDITION", shaderContext.getLightMode());
			addDefine(defines, "TEX_MAP_MODE", shaderContext.getTexMapMode());
			addDefine(defines, "TEX_MAP_PROJ", shaderContext.getTexMapProj());
			addDefine(defines, "NUMBER_BONES", shaderContext.getNumberBones());
			addDefine(defines, "LIGHTING_ENABLE", shaderContext.getLightingEnable());
			addDefine(defines, "TEX_ENABLE", shaderContext.getTexEnable());
			addDefine(defines, "TEX_ENABLE", shaderContext.getTexEnable());
			addDefine(defines, "TEX_ENV_MODE_FUNC", shaderContext.getTexEnvMode(0));
			addDefine(defines, "TEX_ENV_MODE_USE_ALPHA", shaderContext.getTexEnvMode(1));
			addDefine(defines, "CTEST_ENABLE", shaderContext.getCtestEnable());
			addDefine(defines, "CTEST_FUNC", shaderContext.getCtestFunc());
		}

		replace(src, "// INSERT VERSION", String.format("#version %d", shaderVersion));
		replace(src, "// INSERT DEFINES", defines.toString());
	}

	protected boolean loadShader(int shader, String resourceName, boolean silentError) {
		StringBuilder src = new StringBuilder();

        try {
        	InputStream resourceStream = getClass().getResourceAsStream(resourceName);
        	if (resourceStream == null) {
        		return false;
        	}
            src.append(Utilities.toString(resourceStream, true));
        } catch (IOException e) {
        	log.error(e);
        	return false;
        }

        preprocessShader(src);

        boolean compiled = re.compilerShader(shader, src.toString());
        if (compiled || !silentError) {
        	printShaderInfoLog(shader);
        }

        return compiled;
	}

	protected void linkShaderProgram(int program) {
        re.linkProgram(program);
        printProgramInfoLog(program);
        re.validateProgram(program);
        printProgramInfoLog(program);
	}

	protected void loadShaders() {
		vertexShader = re.createShader(RE_VERTEX_SHADER);
        fragmentShader = re.createShader(RE_FRAGMENT_SHADER);

        loadShader(vertexShader, "/jpcsp/graphics/shader.vert", false);
        loadShader(fragmentShader, "/jpcsp/graphics/shader.frag", false);

        shaderProgram = re.createProgram();
        re.attachShader(shaderProgram, vertexShader);
        re.attachShader(shaderProgram, fragmentShader);
        linkShaderProgram(shaderProgram);

        if (useGeometryShader) {
	        // Create a different shader program where the geometry shader is attached.
	        // This program will be used to process GU_SPRITES primitives.
	        geometryShader = re.createShader(RE_GEOMETRY_SHADER);
	        boolean compiled;
	        compiled = loadShader(geometryShader, "/jpcsp/graphics/shader-150.geom", false);
	        if (compiled) {
	        	log.info("Using Geometry Shader shader-150.geom");
	        } else {
	        	compiled = loadShader(geometryShader, "/jpcsp/graphics/shader-120.geom", false);
	        	if (compiled) {
		        	log.info("Using Geometry Shader shader-120.geom");
	        	}
	        }

	        if (!compiled) {
	        	useGeometryShader = false;
	        } else {
		        spriteShaderProgram = re.createProgram();
		        re.attachShader(spriteShaderProgram, vertexShader);
		        re.attachShader(spriteShaderProgram, geometryShader);
		        re.attachShader(spriteShaderProgram, fragmentShader);
		        re.setProgramParameter(spriteShaderProgram, RE_GEOMETRY_INPUT_TYPE, spriteGeometryShaderInputType);
		        re.setProgramParameter(spriteShaderProgram, RE_GEOMETRY_OUTPUT_TYPE, spriteGeometryShaderOutputType);
		        re.setProgramParameter(spriteShaderProgram, RE_GEOMETRY_VERTICES_OUT, 4);
		        linkShaderProgram(spriteShaderProgram);
	        }
        }

        re.useProgram(shaderProgram);
	}

	public static boolean useShaders(IRenderingEngine re) {
		boolean useShaders = Settings.getInstance().readBool("emu.useshaders");
		boolean availableShaders = re.isShaderAvailable();

		if (useShaders && !availableShaders) {
			log.info("Shaders are not available on your computer. They have been automatically disabled.");
		}

		return useShaders && availableShaders;
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
		switch (flag) {
			case IRenderingEngine.GU_LIGHT0:
			case IRenderingEngine.GU_LIGHT1:
			case IRenderingEngine.GU_LIGHT2:
			case IRenderingEngine.GU_LIGHT3:
				shaderContext.setLightEnabled(flag - IRenderingEngine.GU_LIGHT0, value);
				break;
			case IRenderingEngine.GU_COLOR_TEST:
				shaderContext.setCtestEnable(value);
				break;
			case IRenderingEngine.GU_LIGHTING:
				shaderContext.setLightingEnable(value);
				break;
			case IRenderingEngine.GU_TEXTURE_2D:
				shaderContext.setTexEnable(value);
				break;
		}
	}

	@Override
	public void exit() {
		if (DurationStatistics.collectStatistics) {
			if (useNativeClut) {
				log.info(textureCacheLookupStatistics);
			}
		}
		super.exit();
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
		shaderContext.setZPos(zpos);
		shaderContext.setZScale(zscale);
		super.setDepthRange(zpos, zscale, near, far);
	}

	@Override
	public void setLightMode(int mode) {
		shaderContext.setLightMode(mode);
		super.setLightMode(mode);
	}

	@Override
	public void setLightType(int light, int type, int kind) {
		shaderContext.setLightType(light, type);
		shaderContext.setLightKind(light, kind);
		super.setLightType(light, type, kind);
	}

	@Override
	public void setTextureEnvironmentMapping(int u, int v) {
		shaderContext.setTexShade(0, u);
		shaderContext.setTexShade(1, v);
		super.setTextureEnvironmentMapping(u, v);
	}

	@Override
	public void setColorTestFunc(int func) {
		shaderContext.setCtestFunc(func);
		super.setColorTestFunc(func);
	}

	@Override
	public void setColorTestMask(int[] values) {
		shaderContext.setCtestMsk(0, values[0]);
		shaderContext.setCtestMsk(1, values[1]);
		shaderContext.setCtestMsk(2, values[2]);
		super.setColorTestMask(values);
	}

	@Override
	public void setColorTestReference(int[] values) {
		shaderContext.setCtestRef(0, values[0]);
		shaderContext.setCtestRef(1, values[1]);
		shaderContext.setCtestRef(2, values[2]);
		super.setColorTestReference(values);
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
		shaderContext.setTexEnvMode(0, func);
		shaderContext.setTexEnvMode(1, alphaUsed ? 1 : 0);
		shaderContext.setColorDoubling(colorDoubled ? 2.f : 1.f);
		super.setTextureFunc(func, alphaUsed, colorDoubled);
	}

	@Override
	public int setBones(int count, float[] values) {
		shaderContext.setNumberBones(count);
		shaderContext.setBoneMatrix(count, values);
        numberOfWeightsForShader = count;
		super.setBones(count, values);

		return numberOfWeightsForShader; // Number of weights to be copied into the Buffer
	}

	@Override
	public void setTextureMapMode(int mode, int proj) {
		shaderContext.setTexMapMode(mode);
		shaderContext.setTexMapProj(proj);
		super.setTextureMapMode(mode, proj);
	}

	@Override
	public void setColorMaterial(boolean ambient, boolean diffuse, boolean specular) {
		shaderContext.setMatFlags(0, ambient  ? 1 : 0);
		shaderContext.setMatFlags(1, diffuse  ? 1 : 0);
		shaderContext.setMatFlags(2, specular ? 1 : 0);
		super.setColorMaterial(ambient, diffuse, specular);
	}

	@Override
	public void startDisplay() {
        re.useProgram(shaderProgram);
		super.startDisplay();

		// We don't use Client States
		super.disableClientState(IRenderingEngine.RE_TEXTURE);
		super.disableClientState(IRenderingEngine.RE_COLOR);
		super.disableClientState(IRenderingEngine.RE_NORMAL);
		super.disableClientState(IRenderingEngine.RE_VERTEX);
	}

	@Override
	public void endDisplay() {
        re.useProgram(0);
		super.endDisplay();
	}

	@Override
	public void enableClientState(int type) {
		switch (type) {
			case RE_VERTEX:
				re.enableVertexAttribArray(shaderAttribPosition);

				if (numberOfWeightsForShader > 0) {
		            re.enableVertexAttribArray(shaderAttribWeights1);
		            if (numberOfWeightsForShader > 4) {
		            	re.enableVertexAttribArray(shaderAttribWeights2);
		            } else {
		            	re.disableVertexAttribArray(shaderAttribWeights2);
		            }
				} else {
		        	re.disableVertexAttribArray(shaderAttribWeights1);
		        	re.disableVertexAttribArray(shaderAttribWeights2);
				}
				break;
			case RE_NORMAL:
				re.enableVertexAttribArray(shaderAttribNormal);
				break;
			case RE_COLOR:
				re.enableVertexAttribArray(shaderAttribColor);
				break;
			case RE_TEXTURE:
				re.enableVertexAttribArray(shaderAttribTexture);
				break;
		}
	}

	@Override
	public void disableClientState(int type) {
		switch (type) {
			case RE_VERTEX:
				re.disableVertexAttribArray(shaderAttribPosition);
	        	re.disableVertexAttribArray(shaderAttribWeights1);
	        	re.disableVertexAttribArray(shaderAttribWeights2);
				break;
			case RE_NORMAL:
				re.disableVertexAttribArray(shaderAttribNormal);
				break;
			case RE_COLOR:
				re.disableVertexAttribArray(shaderAttribColor);
				break;
			case RE_TEXTURE:
				re.disableVertexAttribArray(shaderAttribTexture);
				break;
		}
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (size > 0) {
            re.setVertexAttribPointer(shaderAttribWeights1, Math.min(size, 4), type, false, stride, bufferSize, buffer);
            if (size > 4) {
                re.setVertexAttribPointer(shaderAttribWeights2, size - 4, type, false, stride, bufferSize, buffer);
            }
		}
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, long offset) {
		if (size > 0) {
            re.setVertexAttribPointer(shaderAttribWeights1, Math.min(size, 4), type, false, stride, offset);
            if (size > 4) {
                re.setVertexAttribPointer(shaderAttribWeights2, size - 4, type, false, stride, offset + sizeOfType[type] * 4);
            }
		}
	}

	@Override
	public boolean canAllNativeVertexInfo() {
		// Shader supports all PSP native vertex formats
		return true;
	}

	@Override
	public boolean canNativeSpritesPrimitive() {
		// Geometry shader supports native GU_SPRITES primitive
		return useGeometryShader;
	}

	@Override
	public void setVertexInfo(VertexInfo vinfo, boolean allNativeVertexInfo, boolean useVertexColor) {
		if (allNativeVertexInfo) {
			shaderContext.setWeightScale(weightScale[vinfo.weight]);
			shaderContext.setTextureScale(vinfo.transform2D ? 1.f : textureScale[vinfo.texture]);
			shaderContext.setVinfoColor(useVertexColor ? vinfo.color : -1);
			shaderContext.setNormalScale(vinfo.transform2D ? 1.f : normalScale[vinfo.normal]);
			shaderContext.setVinfoPosition(vinfo.position);
			shaderContext.setPositionScale(vinfo.transform2D ? 1.f : positionScale[vinfo.position]);
		} else {
			shaderContext.setWeightScale(1);
			shaderContext.setTextureScale(1);
			shaderContext.setVinfoColor(useVertexColor ? 0 : -1);
			shaderContext.setNormalScale(1);
			shaderContext.setVinfoPosition(0);
			shaderContext.setPositionScale(1);
		}
		shaderContext.setVinfoTransform2D(vinfo == null || vinfo.transform2D ? 1 : 0);
		super.setVertexInfo(vinfo, allNativeVertexInfo, useVertexColor);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		re.setVertexAttribPointer(shaderAttribPosition, size, type, false, stride, bufferSize, buffer);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, long offset) {
		re.setVertexAttribPointer(shaderAttribPosition, size, type, false, stride, offset);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		re.setVertexAttribPointer(shaderAttribColor, size, type, false, stride, bufferSize, buffer);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, long offset) {
		re.setVertexAttribPointer(shaderAttribColor, size, type, false, stride, offset);
	}

	@Override
	public void setNormalPointer(int type, int stride, int bufferSize, Buffer buffer) {
		re.setVertexAttribPointer(shaderAttribNormal, 3, type, false, stride, bufferSize, buffer);
	}

	@Override
	public void setNormalPointer(int type, int stride, long offset) {
		re.setVertexAttribPointer(shaderAttribNormal, 3, type, false, stride, offset);
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		re.setVertexAttribPointer(shaderAttribTexture, size, type, false, stride, bufferSize, buffer);
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, long offset) {
		re.setVertexAttribPointer(shaderAttribTexture, size, type, false, stride, offset);
	}

	@Override
	public void drawArrays(int type, int first, int count) {
		int program;
		if (type == GU_SPRITES) {
			type = spriteGeometryShaderInputType;
			program = spriteShaderProgram;
		} else {
			program = shaderProgram;
		}
		re.useProgram(program);
		// The uniform values are specific to a shader program:
		// update the uniform values after switching the active shader program.
		shaderContext.setUniforms(re, program);
		super.drawArrays(type, first, count);
	}

	@Override
	public boolean canNativeClut() {
		// The clut processing is implemented into the fragment shader
		// and the clut values are passed as a sampler2D
		return useNativeClut;
	}

	private void loadClut() {
		// E.g. mask==0xFF requires 256 entries
		// also mask==0xF0 requires 256 entries
		int numEntries = Integer.highestOneBit(context.tex_clut_mask) << 1;
		int pixelFormat = context.tex_clut_mode;
		int bytesPerEntry = sizeOfTextureType[pixelFormat];

		shaderContext.setClutShift(context.tex_clut_shift);
		shaderContext.setClutMask(context.tex_clut_mask);
		shaderContext.setClutOffset(context.tex_clut_start);
		shaderContext.setMipmapShareClut(context.mipmapShareClut);

		int[]   clut32 = (bytesPerEntry == 4 ? VideoEngine.getInstance().readClut32(0) : null);
		short[] clut16 = (bytesPerEntry == 2 ? VideoEngine.getInstance().readClut16(0) : null);

		Texture texture;
		re.setActiveTexture(ACTIVE_TEXTURE_CLUT);
		if (VideoEngine.useTextureCache) {
			TextureCache textureCache = TextureCache.getInstance();

			textureCacheLookupStatistics.start();
			texture = textureCache.getTexture(context.tex_clut_addr, numEntries, numEntries, 1, pixelFormat, 0, 0, 0, 0, 0, 0, 0, false, clut16, clut32);
			textureCacheLookupStatistics.end();

			if (texture == null) {
				texture = new Texture(textureCache, context.tex_clut_addr, numEntries, numEntries, 1, pixelFormat, 0, 0, 0, 0, 0, 0, 0, false, clut16, clut32);
				textureCache.addTexture(re, texture);
			}

			texture.bindTexture(re);
		} else {
			texture = null;
			if (clutTextureId == -1) {
				clutTextureId = re.genTexture();
			}

			re.bindTexture(clutTextureId);
		}

		if (texture == null || !texture.isLoaded()) {
			clutBuffer.clear();
			if (clut32 != null) {
				clutBuffer.asIntBuffer().put(clut32, 0, numEntries);
			} else {
				clutBuffer.asShortBuffer().put(clut16, 0, numEntries);
			}

	        re.setPixelStore(numEntries, bytesPerEntry);
			re.setTextureMipmapMagFilter(GeCommands.TFLT_NEAREST);
			re.setTextureMipmapMinFilter(GeCommands.TFLT_NEAREST);
			re.setTextureMipmapMinLevel(0);
			re.setTextureMipmapMaxLevel(0);
			re.setTextureWrapMode(GeCommands.TWRAP_WRAP_MODE_CLAMP, GeCommands.TWRAP_WRAP_MODE_CLAMP);

			// Load the CLUT as a Nx1 texture.
			// (gid15) I did not manage to make this code work with 1D textures,
			// probably because they are very seldom used and buggy.
			// To use a 2D texture Nx1 is the safest way...
			int clutSize = bytesPerEntry * numEntries;
			re.setTexImage(0, pixelFormat, numEntries, 1, pixelFormat, pixelFormat, clutSize, clutBuffer);

			if (texture != null) {
				texture.setIsLoaded();
			}
		}
		re.setActiveTexture(ACTIVE_TEXTURE_NORMAL);
	}

	@Override
	public void setTextureFormat(int pixelFormat, boolean swizzle) {
		if (isTextureTypeIndexed[pixelFormat]) {
			if (pixelFormat == GeCommands.TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED) {
				// 4bit index has been decoded in the VideoEngine
				pixelFormat = context.tex_clut_mode;
			} else if (!useNativeClut) {
				// Textures are decoded in the VideoEngine when not using native CLUTs
				pixelFormat = context.tex_clut_mode;
			} else {
				loadClut();
			}
		}
		shaderContext.setTexPixelFormat(pixelFormat);
		super.setTextureFormat(pixelFormat, swizzle);
	}
}
