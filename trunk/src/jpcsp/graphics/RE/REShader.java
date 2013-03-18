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

import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED;

import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.Uniforms;
import jpcsp.graphics.VertexInfo;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.textures.FBTexture;
import jpcsp.graphics.textures.GETexture;
import jpcsp.graphics.textures.Texture;
import jpcsp.graphics.textures.TextureCache;
import jpcsp.settings.Settings;
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
	public    final static int ACTIVE_TEXTURE_NORMAL = 0;
	protected final static int ACTIVE_TEXTURE_CLUT = 1;
	protected final static int ACTIVE_TEXTURE_FRAMEBUFFER = 2;
	protected final static int ACTIVE_TEXTURE_INTEGER = 3;
	protected final static float[] positionScale = new float[] { 1, 0x7F, 0x7FFF, 1 };
	protected final static float[] normalScale   = new float[] { 1, 0x7F, 0x7FFF, 1 };
	protected final static float[] textureScale  = new float[] { 1, 0x80, 0x8000, 1 };
	protected final static float[] weightScale   = new float[] { 1, 0x80, 0x8000, 1 };
	protected final static String attributeNameTexture = "pspTexture";
	protected final static String attributeNameColor = "pspColor";
	protected final static String attributeNameNormal = "pspNormal";
	protected final static String attributeNamePosition = "pspPosition";
	protected final static String attributeNameWeights1 = "pspWeights1";
	protected final static String attributeNameWeights2 = "pspWeights2";
	protected ShaderContext shaderContext;
	protected ShaderProgram defaultShaderProgram;
	protected ShaderProgram defaultSpriteShaderProgram;
	protected int numberOfWeightsForShader;
	protected final static int spriteGeometryShaderInputType = GU_LINES;
	protected final static int spriteGeometryShaderOutputType = GU_TRIANGLE_STRIP;
	protected boolean useGeometryShader;
	protected boolean useUniformBufferObject = true;
	protected boolean useNativeClut;
	protected boolean useShaderDepthTest = false;
	protected boolean useShaderStencilTest = false;
	protected boolean useShaderColorMask = false;
	protected boolean useShaderAlphaTest = false;
	protected boolean useShaderBlendTest = false;
	protected boolean useRenderToTexture = false;
	protected int clutTextureId = -1;
	protected ByteBuffer clutBuffer;
    protected DurationStatistics textureCacheLookupStatistics = new CpuDurationStatistics("Lookup in TextureCache for CLUTs");
	protected String shaderStaticDefines;
	protected String shaderDummyDynamicDefines;
	protected int shaderVersion = 120;
	protected ShaderProgramManager shaderProgramManager;
	protected boolean useDynamicShaders;
	protected ShaderProgram currentShaderProgram;
	protected StringBuilder infoLogs;
	protected GETexture fbTexture;
	protected boolean stencilTestFlag;
	protected int viewportWidth;
	protected int viewportHeight;
	protected FBTexture renderTexture;
	protected int pixelFormat;

	public REShader(IRenderingEngine proxy) {
		super(proxy);
		initShader();
	}

	protected void initShader() {
		log.info("Using shaders with Skinning");

		useDynamicShaders = Settings.getInstance().readBool("emu.enabledynamicshaders");
		if (useDynamicShaders) {
			log.info("Using dynamic shaders");
			shaderProgramManager = new ShaderProgramManager();
		}

		useGeometryShader = Settings.getInstance().readBool("emu.useGeometryShader");

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
        	if (!super.canNativeClut(0)) {
    			log.warn("Disabling Native Color Lookup Tables (CLUT)");
        		useNativeClut = false;
        	} else {
    			log.info("Using Native Color Lookup Tables (CLUT)");
        	}
        }

        useShaderStencilTest = Settings.getInstance().readBool("emu.enableshaderstenciltest");
        useShaderColorMask = Settings.getInstance().readBool("emu.enableshadercolormask");

        if (useUniformBufferObject) {
			shaderContext = new ShaderContextUBO(re);
		} else {
			shaderContext = new ShaderContext();
		}

		if (useShaderStencilTest) {
			// When implementing the stencil test in the fragment shader,
			// we need to implement the alpha test and blend test
			// in the shader as well.
			// The alpha test has to be performed before the stencil test
			// in order to test the correct alpha values because these are updated
			// by the stencil test.
			// The alpha test of the fixed OpenGL functionality is always
			// executed after the shader execution and would then use
			// incorrect alpha test values.
			// The blend test has also to use the correct alpha value,
			// i.e. the alpha value before the stencil test.
			useShaderAlphaTest = true;
			useShaderBlendTest = true;
			useShaderDepthTest = true;
		}

		if (useShaderStencilTest || useShaderBlendTest || useShaderColorMask) {
			// If we are using shaders requiring the current frame buffer content
			// as a texture, activate the rendering to a texture if available.
			if (re.isFramebufferObjectAvailable()) {
				useRenderToTexture = true;
				log.info("Rendering to a texture");
			} else {
				log.info("Not rendering to a texture, FBO's are not supported by your graphics card. This will have a negative performance impact.");
			}
		}

		initShadersDefines();
		loadShaders();
		if (defaultShaderProgram == null) {
			return;
		}

		shaderContext.setColorDoubling(1);

		shaderContext.setTex(ACTIVE_TEXTURE_NORMAL);
		shaderContext.setFbTex(ACTIVE_TEXTURE_FRAMEBUFFER);
		if (useNativeClut) {
			shaderContext.setClut(ACTIVE_TEXTURE_CLUT);
			shaderContext.setUtex(ACTIVE_TEXTURE_INTEGER);
			clutBuffer = ByteBuffer.allocateDirect(4096 * 4).order(ByteOrder.LITTLE_ENDIAN);
		}
	}

	protected boolean isValidShader() {
		if (defaultShaderProgram == null) {
			return false;
		}

		return true;
	}

	protected static void addDefine(StringBuilder defines, String name, String value) {
		defines.append(String.format("#define %s %s%s", name, escapeString(value), System.getProperty("line.separator")));
	}

	protected static void addDefine(StringBuilder defines, String name, int value) {
		addDefine(defines, name, Integer.toString(value));
	}

	protected static void addDefine(StringBuilder defines, String name, boolean value) {
		addDefine(defines, name, value ? 1 : 0);
	}

	protected void replace(StringBuilder s, String oldText, String newText) {
		int offset = s.indexOf(oldText);
		if (offset >= 0) {
			s.replace(offset, offset + oldText.length(), newText);
		}
	}

	protected static String escapeString(String s) {
		return s.replace('\n', ' ');
	}

	protected int getAvailableShadingLanguageVersion() {
		int availableVersion = 0;

		String shadingLanguageVersion = re.getShadingLanguageVersion();
        if (shadingLanguageVersion == null) {
        	return availableVersion;
        }

        Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+).*").matcher(shadingLanguageVersion);
        if (!matcher.matches()) {
        	log.error(String.format("Cannot parse Shading Language Version '%s'", shadingLanguageVersion));
        	return availableVersion;
        }

        try {
        	int majorNumber = Integer.parseInt(matcher.group(1));
        	int minorNumber = Integer.parseInt(matcher.group(2));

        	availableVersion = majorNumber * 100 + minorNumber;
        } catch (NumberFormatException e) {
        	log.error(String.format("Cannot parse Shading Language Version '%s'", shadingLanguageVersion));
        }

        return availableVersion;
	}

	protected void initShadersDefines() {
        StringBuilder staticDefines = new StringBuilder();

        if (getAvailableShadingLanguageVersion() >= 140) {
        	// Use at least version 1.40 when available.
        	// Version 1.20/1.30 is causing problems with AMD drivers.
        	shaderVersion = Math.max(140, shaderVersion);
        }

        addDefine(staticDefines, "USE_GEOMETRY_SHADER", useGeometryShader);
		addDefine(staticDefines, "USE_UBO", useUniformBufferObject);
		if (useUniformBufferObject) {
			// UBO requires at least shader version 1.40
			shaderVersion = Math.max(140, shaderVersion);
			addDefine(staticDefines, "UBO_STRUCTURE", ((ShaderContextUBO) shaderContext).getShaderUniformText());
		}

		addDefine(staticDefines, "USE_NATIVE_CLUT", useNativeClut);
		if (useNativeClut) {
			// Native clut requires at least shader version 1.30
			shaderVersion = Math.max(130, shaderVersion);
		}

		if (useShaderStencilTest || useShaderBlendTest || useShaderColorMask) {
			// Function texelFetch requires at least shader version 1.30
			shaderVersion = Math.max(130, shaderVersion);
		}

		boolean useBitOperators = re.isExtensionAvailable("GL_EXT_gpu_shader4");
		addDefine(staticDefines, "USE_BIT_OPERATORS", useBitOperators);
		if (!useBitOperators) {
			log.info("Extension GL_EXT_gpu_shader4 not available: not using bit operators in shader");
		}

		shaderStaticDefines = staticDefines.toString();
		shaderDummyDynamicDefines = ShaderProgram.getDummyDynamicDefines();

		if (log.isDebugEnabled()) {
        	log.debug(String.format("Using shader version %d, available shading language version %d", shaderVersion, getAvailableShadingLanguageVersion()));
        }
	}

	protected void preprocessShader(StringBuilder src, ShaderProgram shaderProgram) {
		StringBuilder defines = new StringBuilder(shaderStaticDefines);

		boolean useDynamicDefines;
		if (shaderProgram != null) {
			defines.append(shaderProgram.getDynamicDefines());
			useDynamicDefines = true;
		} else {
			// Set dummy values to the dynamic defines
			// so that the preprocessor doesn't complain about undefined values
			defines.append(shaderDummyDynamicDefines);
			useDynamicDefines = false;
		}
		addDefine(defines, "USE_DYNAMIC_DEFINES", useDynamicDefines);

		replace(src, "// INSERT VERSION", String.format("#version %d", shaderVersion));
		replace(src, "// INSERT DEFINES", defines.toString());
	}

	protected boolean loadShader(int shader, String resourceName, boolean silentError, ShaderProgram shaderProgram) {
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

        preprocessShader(src, shaderProgram);

        if (log.isTraceEnabled()) {
        	log.trace(String.format("Compiling shader %d from %s:\n%s", shader, resourceName, src.toString()));
        }

        boolean compiled = re.compilerShader(shader, src.toString());
        if (compiled || !silentError) {
        	addShaderInfoLog(shader);
        }

        return compiled;
	}

	/**
	 * Link and validate a shader program.
	 * 
	 * @param program       the program to be linked
	 * @return true         if the link was successful
	 *         false        if the link was not successful
	 */
	protected boolean linkShaderProgram(int program) {
        boolean linked = re.linkProgram(program);
        addProgramInfoLog(program);

        // Trying to avoid warning message from AMD drivers:
        // "Validation warning! - Sampler value tex has not been set."
        // and error message:
        // "Validation failed! - Different sampler types for same sample texture unit in fragment shader"
        re.useProgram(program);
        re.setUniform(re.getUniformLocation(program, Uniforms.tex.getUniformString()), ACTIVE_TEXTURE_NORMAL);
        re.setUniform(re.getUniformLocation(program, Uniforms.fbTex.getUniformString()), ACTIVE_TEXTURE_FRAMEBUFFER);
        if (useNativeClut) {
        	re.setUniform(re.getUniformLocation(program, Uniforms.clut.getUniformString()), ACTIVE_TEXTURE_CLUT);
        	re.setUniform(re.getUniformLocation(program, Uniforms.utex.getUniformString()), ACTIVE_TEXTURE_INTEGER);
        }

        boolean validated = re.validateProgram(program);
        addProgramInfoLog(program);

        return linked && validated;
	}

	protected ShaderProgram createShader(boolean hasGeometryShader, ShaderProgram shaderProgram) {
		infoLogs = new StringBuilder();

		int programId = tryCreateShader(hasGeometryShader, shaderProgram);
		if (programId == -1) {
			printInfoLog(true);
			shaderProgram = null;
		} else {
			if (shaderProgram == null) {
				shaderProgram = new ShaderProgram();
			}
			shaderProgram.setProgramId(re, programId);
			printInfoLog(false);
		}

		return shaderProgram;
	}

	private int tryCreateShader(boolean hasGeometryShader, ShaderProgram shaderProgram) {
		int vertexShader = re.createShader(RE_VERTEX_SHADER);
        int fragmentShader = re.createShader(RE_FRAGMENT_SHADER);

        if (!loadShader(vertexShader, "/jpcsp/graphics/shader.vert", false, shaderProgram)) {
        	return -1;
        }
        if (!loadShader(fragmentShader, "/jpcsp/graphics/shader.frag", false, shaderProgram)) {
        	return -1;
        }

        int program = re.createProgram();
        re.attachShader(program, vertexShader);
        re.attachShader(program, fragmentShader);

        if (hasGeometryShader) {
	        int geometryShader = re.createShader(RE_GEOMETRY_SHADER);
	        boolean compiled;
	        compiled = loadShader(geometryShader, "/jpcsp/graphics/shader-150.geom", false, shaderProgram);
	        if (compiled) {
	        	log.info("Using Geometry Shader shader-150.geom");
	        } else {
	        	compiled = loadShader(geometryShader, "/jpcsp/graphics/shader-120.geom", false, shaderProgram);
	        	if (compiled) {
		        	log.info("Using Geometry Shader shader-120.geom");
	        	}
	        }

	        if (!compiled) {
	        	return -1;
	        }
	        re.attachShader(program, geometryShader);
	        re.setProgramParameter(program, RE_GEOMETRY_INPUT_TYPE, spriteGeometryShaderInputType);
	        re.setProgramParameter(program, RE_GEOMETRY_OUTPUT_TYPE, spriteGeometryShaderOutputType);
	        re.setProgramParameter(program, RE_GEOMETRY_VERTICES_OUT, 4);
        }

        // Use the same attribute index values for all shader programs.
        //
        // Issue: AMD driver is incorrectly handling attributes referenced in a shader
        // (even when not really used, e.g. in an "if" statement)
        // but disabled using disableVertexAttribArray. The solution for this
        // issue is to use dynamic shaders.
        //
        // Read on AMD forum: the vertex attribute 0 has to be defined.
        // Using the position here, as it is always defined.
        //
        int index = 0;
        re.bindAttribLocation(program, index++, attributeNamePosition);
        re.bindAttribLocation(program, index++, attributeNameTexture);
        re.bindAttribLocation(program, index++, attributeNameColor);
        re.bindAttribLocation(program, index++, attributeNameNormal);
        re.bindAttribLocation(program, index++, attributeNameWeights1);
        re.bindAttribLocation(program, index++, attributeNameWeights2);

        boolean linked = linkShaderProgram(program);
        if (!linked) {
        	return -1;
        }

        re.useProgram(program);

        if (log.isDebugEnabled()) {
        	int shaderAttribWeights1 = re.getAttribLocation(program, attributeNameWeights1);
        	int shaderAttribWeights2 = re.getAttribLocation(program, attributeNameWeights2);
        	int shaderAttribPosition = re.getAttribLocation(program, attributeNamePosition);
        	int shaderAttribNormal   = re.getAttribLocation(program, attributeNameNormal);
        	int shaderAttribColor    = re.getAttribLocation(program, attributeNameColor);
        	int shaderAttribTexture  = re.getAttribLocation(program, attributeNameTexture);
        	log.debug(String.format("Program %d attribute locations: weights1=%d, weights2=%d, position=%d, normal=%d, color=%d, texture=%d", program, shaderAttribWeights1, shaderAttribWeights2, shaderAttribPosition, shaderAttribNormal, shaderAttribColor, shaderAttribTexture));
        }

		for (Uniforms uniform : Uniforms.values()) {
            uniform.allocateId(re, program);
        }

		shaderContext.initShaderProgram(re, program);

		return program;
	}

	protected void loadShaders() {
		defaultShaderProgram = createShader(false, null);
		if (defaultShaderProgram != null) {
			if (useGeometryShader) {
				defaultSpriteShaderProgram = createShader(true, null);
			}

			defaultShaderProgram.use(re);
		}

		if (defaultSpriteShaderProgram == null) {
			useGeometryShader = false;
		}
	}

	public static boolean useShaders(IRenderingEngine re) {
		if (!Settings.getInstance().readBool("emu.useshaders")) {
			return false;
		}

		if (!re.isShaderAvailable()) {
			log.info("Shaders are not available on your computer. They have been automatically disabled.");
			return false;
		}

		REShader reTestShader = new REShader(re);
		if (!reTestShader.isValidShader()) {
			log.warn("Shaders do not run correctly on your computer. They have been automatically disabled.");
			return false;
		}

		return true;
	}

	protected void printInfoLog(boolean isError) {
		if (infoLogs != null && infoLogs.length() > 0) {
			if (isError) {
				log.error("Shader error log: " + infoLogs);
			} else {
				// Remove all the useless AMD messages
				String infoLog = infoLogs.toString();
				infoLog = infoLog.replace("Vertex shader was successfully compiled to run on hardware.\n", "");
				infoLog = infoLog.replace("Fragment shader was successfully compiled to run on hardware.\n", "");
				infoLog = infoLog.replace("Geometry shader was successfully compiled to run on hardware.\n", "");
				infoLog = infoLog.replace("Fragment shader(s) linked, vertex shader(s) linked. \n", "");
				infoLog = infoLog.replace("Vertex shader(s) linked, fragment shader(s) linked. \n", "");
				infoLog = infoLog.replace("Vertex shader(s) linked, fragment shader(s) linked.\n", "");
				infoLog = infoLog.replace("Validation successful.\n", "");

				if (infoLog.length() > 0) {
					log.warn("Shader log: " + infoLog);
				}
			}
		}
	}

	protected void addInfoLog(String infoLog) {
		if (infoLog != null && infoLog.length() > 0) {
			infoLogs.append(infoLog);
		}
	}

	protected void addShaderInfoLog(int shader) {
		String infoLog = re.getShaderInfoLog(shader);
		addInfoLog(infoLog);
	}

    protected void addProgramInfoLog(int program) {
		String infoLog = re.getProgramInfoLog(program);
		addInfoLog(infoLog);
    }

	/**
	 * Set the given flag in the shader context, if relevant.
	 * @param flag   the flag to be set
	 * @param value  the value of the flag (0 is disabled, 1 is enabled)
	 * @return       true if the flag as to be enabled in OpenGL as well,
	 *               false if the flag has to stay disabled in OpenGL.
	 */
	protected boolean setShaderFlag(int flag, int value) {
		boolean setFlag = true;

		switch (flag) {
			case IRenderingEngine.GU_LIGHT0:
			case IRenderingEngine.GU_LIGHT1:
			case IRenderingEngine.GU_LIGHT2:
			case IRenderingEngine.GU_LIGHT3:
				shaderContext.setLightEnabled(flag - IRenderingEngine.GU_LIGHT0, value);
				setFlag = false;
				break;
			case IRenderingEngine.GU_COLOR_TEST:
				shaderContext.setCtestEnable(value);
				setFlag = false;
				break;
			case IRenderingEngine.GU_LIGHTING:
				shaderContext.setLightingEnable(value);
				setFlag = false;
				break;
			case IRenderingEngine.GU_TEXTURE_2D:
				shaderContext.setTexEnable(value);
				break;
			case IRenderingEngine.GU_DEPTH_TEST:
				if (useShaderDepthTest) {
					shaderContext.setDepthTestEnable(value);
				}
				break;
			case IRenderingEngine.GU_STENCIL_TEST:
				if (useShaderStencilTest) {
					shaderContext.setStencilTestEnable(value);
					stencilTestFlag = (value != 0);
					setAlphaMask(stencilTestFlag);
					setFlag = false;
				}
				break;
			case IRenderingEngine.GU_ALPHA_TEST:
				if (useShaderAlphaTest) {
					shaderContext.setAlphaTestEnable(value);
					setFlag = false;
				}
				break;
			case IRenderingEngine.GU_BLEND:
				if (useShaderBlendTest) {
					shaderContext.setBlendTestEnable(value);
					setFlag = false;
				}
				break;
		}

		return setFlag;
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
			if (setShaderFlag(flag, 1)) {
				super.enableFlag(flag);
			}
		}
	}

	@Override
	public void disableFlag(int flag) {
		if (canUpdateFlag(flag)) {
			if (setShaderFlag(flag, 0)) {
				super.disableFlag(flag);
			}
		}
	}

	@Override
	public void setDepthRange(float zpos, float zscale, int near, int far) {
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
		defaultShaderProgram.use(re);

		if (useRenderToTexture) {
			sceDisplay display = Modules.sceDisplayModule;
			int width = display.getWidthFb();
			int height = display.getHeightFb();
			int bufferWidth = display.getBufferWidthFb();
			int pixelFormat = display.getPixelFormatFb();

			// if the format of the Frame Buffer has changed, re-create a new texture
			if (renderTexture != null && !renderTexture.isCompatible(width, height, bufferWidth, pixelFormat)) {
				renderTexture.delete(re);
				renderTexture = null;
			}

			// Activate the rendering to a texture
			if (renderTexture == null) {
				renderTexture = new FBTexture(display.getTopAddrFb(), bufferWidth, width, height, pixelFormat);
				renderTexture.bind(re, false);
				re.bindActiveTexture(ACTIVE_TEXTURE_FRAMEBUFFER, renderTexture.getTextureId());
			} else {
				renderTexture.bind(re, false);
			}
		}

		super.startDisplay();

		// We don't use Client States
		super.disableClientState(IRenderingEngine.RE_TEXTURE);
		super.disableClientState(IRenderingEngine.RE_COLOR);
		super.disableClientState(IRenderingEngine.RE_NORMAL);
		super.disableClientState(IRenderingEngine.RE_VERTEX);
	}

	@Override
	public void endDisplay() {
		if (useRenderToTexture) {
			// Copy the rendered texture back to the main frame buffer
			renderTexture.copyTextureToScreen(re);
		}

		re.useProgram(0);
		super.endDisplay();
	}

	@Override
	public void enableClientState(int type) {
		switch (type) {
			case RE_VERTEX:
				re.enableVertexAttribArray(currentShaderProgram.getShaderAttribPosition());

				if (numberOfWeightsForShader > 0) {
		            re.enableVertexAttribArray(currentShaderProgram.getShaderAttribWeights1());
		            if (numberOfWeightsForShader > 4) {
		            	re.enableVertexAttribArray(currentShaderProgram.getShaderAttribWeights2());
		            } else {
		            	re.disableVertexAttribArray(currentShaderProgram.getShaderAttribWeights2());
		            }
				} else {
		        	re.disableVertexAttribArray(currentShaderProgram.getShaderAttribWeights1());
		        	re.disableVertexAttribArray(currentShaderProgram.getShaderAttribWeights2());
				}
				break;
			case RE_NORMAL:
				re.enableVertexAttribArray(currentShaderProgram.getShaderAttribNormal());
				break;
			case RE_COLOR:
				re.enableVertexAttribArray(currentShaderProgram.getShaderAttribColor());
				break;
			case RE_TEXTURE:
				re.enableVertexAttribArray(currentShaderProgram.getShaderAttribTexture());
				break;
		}
	}

	@Override
	public void disableClientState(int type) {
		switch (type) {
			case RE_VERTEX:
				re.disableVertexAttribArray(currentShaderProgram.getShaderAttribPosition());
	        	re.disableVertexAttribArray(currentShaderProgram.getShaderAttribWeights1());
	        	re.disableVertexAttribArray(currentShaderProgram.getShaderAttribWeights2());
				break;
			case RE_NORMAL:
				re.disableVertexAttribArray(currentShaderProgram.getShaderAttribNormal());
				break;
			case RE_COLOR:
				re.disableVertexAttribArray(currentShaderProgram.getShaderAttribColor());
				break;
			case RE_TEXTURE:
				re.disableVertexAttribArray(currentShaderProgram.getShaderAttribTexture());
				break;
		}
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (size > 0) {
            re.setVertexAttribPointer(currentShaderProgram.getShaderAttribWeights1(), Math.min(size, 4), type, false, stride, bufferSize, buffer);
            if (size > 4) {
                re.setVertexAttribPointer(currentShaderProgram.getShaderAttribWeights2(), size - 4, type, false, stride, bufferSize, buffer);
            }
		}
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, long offset) {
		if (size > 0) {
            re.setVertexAttribPointer(currentShaderProgram.getShaderAttribWeights1(), Math.min(size, 4), type, false, stride, offset);
            if (size > 4) {
                re.setVertexAttribPointer(currentShaderProgram.getShaderAttribWeights2(), size - 4, type, false, stride, offset + sizeOfType[type] * 4);
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
	public void setVertexInfo(VertexInfo vinfo, boolean allNativeVertexInfo, boolean useVertexColor, boolean useTexture, int type) {
		if (allNativeVertexInfo) {
			// Weight
			shaderContext.setWeightScale(weightScale[vinfo.weight]);

			// Texture
			shaderContext.setVinfoTexture(useTexture ? (vinfo.texture != 0 ? vinfo.texture : 3) : 0);
			shaderContext.setTextureScale(vinfo.transform2D ? 1.f : textureScale[vinfo.texture]);

			// Color
			shaderContext.setVinfoColor(useVertexColor ? vinfo.color : 8);

			// Normal
			shaderContext.setVinfoNormal(vinfo.normal);
			shaderContext.setNormalScale(vinfo.transform2D ? 1.f : normalScale[vinfo.normal]);

			// Position
			shaderContext.setVinfoPosition(vinfo.position);
			shaderContext.setPositionScale(vinfo.transform2D ? 1.f : positionScale[vinfo.position]);
		} else {
			// Weight
			shaderContext.setWeightScale(1);

			// Texture
			shaderContext.setVinfoTexture(useTexture ? 3 : 0);
			shaderContext.setTextureScale(1);

			// Color
			shaderContext.setVinfoColor(useVertexColor ? 0 : 8);

			// Normal
			shaderContext.setVinfoNormal(vinfo == null || vinfo.normal == 0 ? 0 : 3);
			shaderContext.setNormalScale(1);

			// Position
			shaderContext.setVinfoPosition(vinfo != null && vinfo.position == 0 ? 0 : 3);
			shaderContext.setPositionScale(1);
		}
		shaderContext.setVinfoTransform2D(vinfo == null || vinfo.transform2D ? 1 : 0);
		setCurrentShaderProgram(type);

		super.setVertexInfo(vinfo, allNativeVertexInfo, useVertexColor, useTexture, type);
	}

	private void setCurrentShaderProgram(int type) {
		ShaderProgram shaderProgram;
		boolean hasGeometryShader = (type == GU_SPRITES);
		if (useDynamicShaders) {
			shaderProgram = shaderProgramManager.getShaderProgram(shaderContext, hasGeometryShader);
			if (shaderProgram.getProgramId() == -1) {
				shaderProgram = createShader(hasGeometryShader, shaderProgram);
				if (log.isDebugEnabled()) {
					log.debug("Created shader " + shaderProgram);
				}
				if (shaderProgram == null) {
					log.error("Cannot create shader");
					return;
				}
			}
		} else if (hasGeometryShader) {
			shaderProgram = defaultSpriteShaderProgram;
		} else {
			shaderProgram = defaultShaderProgram;
		}
		shaderProgram.use(re);
		if (log.isTraceEnabled()) {
			log.trace("Using shader " + shaderProgram);
		}
		currentShaderProgram = shaderProgram;
	}

	/**
	 * Check if the fragment shader requires the frame buffer texture (fbTex sampler)
	 * to be updated with the current screen content.
	 * 
	 * A copy of the current screen content to the frame buffer texture
	 * can be avoided if this method returns "false". The execution of such a copy
	 * is quite expensive and should be avoided as much as possible.
	 * 
	 * We need to copy the current screen to the FrameBuffer texture only when one
	 * of the following applies:
	 * - the STENCIL_TEST flag is enabled
	 * - the BLEND_TEST flag is enabled
	 * - the Color mask is enabled
	 *
	 * @return  true  if the shader will use the fbTex sampler
	 *          false if the shader will not use the fbTex sampler
	 */
	private boolean isFbTextureNeeded() {
		if (useShaderDepthTest && shaderContext.getDepthTestEnable() != 0) {
			return true;
		}

		if (useShaderStencilTest && shaderContext.getStencilTestEnable() != 0) {
			return true;
		}

		if (useShaderBlendTest && shaderContext.getBlendTestEnable() != 0) {
			return true;
		}

		if (useShaderColorMask && shaderContext.getColorMaskEnable() != 0) {
			return true;
		}

		return false;
	}

	/**
	 * If necessary, load the frame buffer texture with the current screen
	 * content so that it can be used by the fragment shader (fbTex sampler).
	 */
	private void loadFbTexture() {
		if (!isFbTextureNeeded()) {
			return;
		}

		int width = viewportWidth;
		int height = viewportHeight;
		int bufferWidth = context.fbw;
		int pixelFormat = context.psm;

		if (useRenderToTexture) {
			// Use the render texture if it is compatible with the current GE settings.
			if (renderTexture.getResizedWidth() >= width && renderTexture.getResizedHeight() >= height && renderTexture.getBufferWidth() >= bufferWidth && renderTexture.getPixelFormat() == pixelFormat) {
				// Tell the shader which texture has to be used for the fbTex sampler.
				re.bindActiveTexture(ACTIVE_TEXTURE_FRAMEBUFFER, renderTexture.getTextureId());
				return;
			}
			// If the render texture is not compatible with the current GE settings,
			// we are not lucky and have to copy the current screen to a compatible
			// texture.
		}

		// Delete the texture and recreate a new one if its dimension has changed
		if (fbTexture != null && !fbTexture.isCompatible(width, height, bufferWidth, pixelFormat)) {
			fbTexture.delete(re);
			fbTexture = null;
		}

		// Create a new texture
		if (fbTexture == null) {
			fbTexture = new GETexture(Modules.sceDisplayModule.getTopAddrGe(), bufferWidth, width, height, pixelFormat, true);
		}

		// Copy the current screen (FrameBuffer) content to the texture
		re.setActiveTexture(ACTIVE_TEXTURE_FRAMEBUFFER);
		fbTexture.copyScreenToTexture(re);
		re.setActiveTexture(ACTIVE_TEXTURE_NORMAL);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		re.setVertexAttribPointer(currentShaderProgram.getShaderAttribPosition(), size, type, false, stride, bufferSize, buffer);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, long offset) {
		re.setVertexAttribPointer(currentShaderProgram.getShaderAttribPosition(), size, type, false, stride, offset);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		re.setVertexAttribPointer(currentShaderProgram.getShaderAttribColor(), size, type, false, stride, bufferSize, buffer);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, long offset) {
		re.setVertexAttribPointer(currentShaderProgram.getShaderAttribColor(), size, type, false, stride, offset);
	}

	@Override
	public void setNormalPointer(int type, int stride, int bufferSize, Buffer buffer) {
		re.setVertexAttribPointer(currentShaderProgram.getShaderAttribNormal(), 3, type, false, stride, bufferSize, buffer);
	}

	@Override
	public void setNormalPointer(int type, int stride, long offset) {
		re.setVertexAttribPointer(currentShaderProgram.getShaderAttribNormal(), 3, type, false, stride, offset);
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		re.setVertexAttribPointer(currentShaderProgram.getShaderAttribTexture(), size, type, false, stride, bufferSize, buffer);
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, long offset) {
		re.setVertexAttribPointer(currentShaderProgram.getShaderAttribTexture(), size, type, false, stride, offset);
	}

	private int prepareDraw(int primitive, boolean burstMode) {
		if (primitive == GU_SPRITES) {
			primitive = spriteGeometryShaderInputType;
		}
		if (!burstMode) {
			// The uniform values are specific to a shader program:
			// update the uniform values after switching the active shader program.
			shaderContext.setUniforms(re, currentShaderProgram.getProgramId());
		}
		loadFbTexture();
		loadIntegerTexture();

		return primitive;
	}

	@Override
	public void drawArrays(int primitive, int first, int count) {
		primitive = prepareDraw(primitive, false);
		super.drawArrays(primitive, first, count);
	}

	@Override
	public void drawArraysBurstMode(int primitive, int first, int count) {
		// drawArraysBurstMode is equivalent to drawArrays
		// but without the need to set the uniforms (they are unchanged
		// since the last call to drawArrays).
		primitive = prepareDraw(primitive, true);
		super.drawArraysBurstMode(primitive, first, count);
	}

	@Override
	public void drawElements(int primitive, int count, int indexType, Buffer indices, int indicesOffset) {
		primitive = prepareDraw(primitive, false);
		super.drawElements(primitive, count, indexType, indices, indicesOffset);
	}

	@Override
	public void drawElements(int primitive, int count, int indexType, long indicesOffset) {
		primitive = prepareDraw(primitive, false);
		super.drawElements(primitive, count, indexType, indicesOffset);
	}

	@Override
	public void drawElementsBurstMode(int primitive, int count, int indexType, long indicesOffset) {
		// drawElementsBurstMode is equivalent to drawElements
		// but without the need to set the uniforms (they are unchanged
		// since the last call to drawElements).
		primitive = prepareDraw(primitive, true);
		super.drawElementsBurstMode(primitive, count, indexType, indicesOffset);
	}

	@Override
	public boolean canNativeClut(int textureAddress) {
		// The clut processing is implemented into the fragment shader
		// and the clut values are passed as a sampler2D
		return useNativeClut;
	}

	private int getClutIndexHint(int pixelFormat, int numEntries) {
		if (context.tex_clut_start == 0 && context.tex_clut_mask == numEntries - 1) {
			int currentBit = 0;
			if (context.tex_clut_shift == currentBit) {
				return IRenderingEngine.RE_CLUT_INDEX_RED_ONLY;
			}

			switch (pixelFormat) {
				case IRenderingEngine.RE_PIXEL_STORAGE_16BIT_INDEXED_BGR5650:  currentBit += 5; break;
				case IRenderingEngine.RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR5551: currentBit += 5; break;
				case IRenderingEngine.RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR4444: currentBit += 4; break;
				case IRenderingEngine.RE_PIXEL_STORAGE_32BIT_INDEXED_ABGR8888: currentBit += 8; break;
				default:
					switch (context.tex_clut_mode) {
						case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:  currentBit += 5; break;
						case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551: currentBit += 5; break;
						case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444: currentBit += 4; break;
						case GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888: currentBit += 8; break;
					}
			}
			if (context.tex_clut_shift == currentBit) {
				return IRenderingEngine.RE_CLUT_INDEX_GREEN_ONLY;
			}

			switch (pixelFormat) {
				case IRenderingEngine.RE_PIXEL_STORAGE_16BIT_INDEXED_BGR5650:  currentBit += 6; break;
				case IRenderingEngine.RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR5551: currentBit += 5; break;
				case IRenderingEngine.RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR4444: currentBit += 4; break;
				case IRenderingEngine.RE_PIXEL_STORAGE_32BIT_INDEXED_ABGR8888: currentBit += 8; break;
				default:
					switch (context.tex_clut_mode) {
						case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:  currentBit += 6; break;
						case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551: currentBit += 5; break;
						case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444: currentBit += 4; break;
						case GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888: currentBit += 8; break;
					}
			}
			if (context.tex_clut_shift == currentBit) {
				return IRenderingEngine.RE_CLUT_INDEX_BLUE_ONLY;
			}

			switch (pixelFormat) {
				case IRenderingEngine.RE_PIXEL_STORAGE_16BIT_INDEXED_BGR5650:  currentBit += 5; break;
				case IRenderingEngine.RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR5551: currentBit += 5; break;
				case IRenderingEngine.RE_PIXEL_STORAGE_16BIT_INDEXED_ABGR4444: currentBit += 4; break;
				case IRenderingEngine.RE_PIXEL_STORAGE_32BIT_INDEXED_ABGR8888: currentBit += 8; break;
				default:
					switch (context.tex_clut_mode) {
						case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_BGR5650:  currentBit += 5; break;
						case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR5551: currentBit += 5; break;
						case GeCommands.TPSM_PIXEL_STORAGE_MODE_16BIT_ABGR4444: currentBit += 4; break;
						case GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888: currentBit += 8; break;
					}
			}
			if (context.tex_clut_shift == currentBit) {
				return IRenderingEngine.RE_CLUT_INDEX_ALPHA_ONLY;
			}
		}

		return IRenderingEngine.RE_CLUT_INDEX_NO_HINT;
	}

	private void loadIntegerTexture() {
		if (!context.textureFlag.isEnabled() || context.clearMode) {
			// Not using a texture
			return;
		}
		if (!useNativeClut || !isTextureTypeIndexed[pixelFormat]) {
			// Not using a native clut
			return;
		}
		if (pixelFormat == TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED) {
			// 4bit index has been decoded in the VideoEngine
			return;
		}

		// Associate the current texture to the integer texture sampler.
		// AMD/ATI driver requires different texture units for samplers of different types.
		// Otherwise, the shader compilation fails with the following error:
		// "Different sampler types for same sample texture unit in fragment shader"
		re.bindActiveTexture(ACTIVE_TEXTURE_INTEGER, context.currentTextureId);
	}

	private void loadClut(int pixelFormat) {
		int numEntries = VideoEngine.getInstance().getClutNumEntries();
		int clutPixelFormat = context.tex_clut_mode;
		int bytesPerEntry = sizeOfTextureType[clutPixelFormat];

		shaderContext.setClutShift(context.tex_clut_shift);
		shaderContext.setClutMask(context.tex_clut_mask);
		shaderContext.setClutOffset(context.tex_clut_start << 4);
		shaderContext.setMipmapShareClut(context.mipmapShareClut);
		shaderContext.setClutIndexHint(getClutIndexHint(pixelFormat, numEntries));

		int[]   clut32 = (bytesPerEntry == 4 ? VideoEngine.getInstance().readClut32(0) : null);
		short[] clut16 = (bytesPerEntry == 2 ? VideoEngine.getInstance().readClut16(0) : null);

		Texture texture;
		int textureId;
		if (VideoEngine.useTextureCache) {
			TextureCache textureCache = TextureCache.getInstance();

			textureCacheLookupStatistics.start();
			texture = textureCache.getTexture(context.tex_clut_addr, numEntries, numEntries, 1, clutPixelFormat, 0, 0, 0, 0, 0, 0, 0, false, clut16, clut32);
			textureCacheLookupStatistics.end();

			if (texture == null) {
				texture = new Texture(textureCache, context.tex_clut_addr, numEntries, numEntries, 1, clutPixelFormat, 0, 0, 0, 0, 0, 0, 0, false, clut16, clut32);
				textureCache.addTexture(re, texture);
			}

			textureId = texture.getTextureId(re);
		} else {
			texture = null;
			if (clutTextureId == -1) {
				clutTextureId = re.genTexture();
			}

			textureId = clutTextureId;
		}

		if (texture == null || !texture.isLoaded()) {
			re.setActiveTexture(ACTIVE_TEXTURE_CLUT);
			re.bindTexture(textureId);

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
			re.setTexImage(0, clutPixelFormat, numEntries, 1, clutPixelFormat, clutPixelFormat, clutSize, clutBuffer);

			if (texture != null) {
				texture.setIsLoaded();
			}

			re.setActiveTexture(ACTIVE_TEXTURE_NORMAL);
		} else {
			// The call
			//     bindActiveTexture(ACTIVE_TEXTURE_CLUT, textureId)
			// is equivalent to
			//     setActiveTexture(ACTIVE_TEXTURE_CLUT)
			//     bindTexture(textureId)
			//     setActiveTexture(ACTIVE_TEXTURE_NORMAL)
			// but executes faster: StateProxy can eliminate the 3 OpenGL calls
			// if they are redundant.
			re.bindActiveTexture(ACTIVE_TEXTURE_CLUT, textureId);
		}
	}

	@Override
	public void setTextureFormat(int pixelFormat, boolean swizzle) {
		this.pixelFormat = pixelFormat;
		if (isTextureTypeIndexed[pixelFormat]) {
			if (pixelFormat == GeCommands.TPSM_PIXEL_STORAGE_MODE_4BIT_INDEXED) {
				// 4bit index has been decoded in the VideoEngine
				pixelFormat = context.tex_clut_mode;
			} else if (!useNativeClut) {
				// Textures are decoded in the VideoEngine when not using native CLUTs
				pixelFormat = context.tex_clut_mode;
			} else {
				loadClut(pixelFormat);
			}
		}
		shaderContext.setTexPixelFormat(pixelFormat);
		super.setTextureFormat(pixelFormat, swizzle);
	}

	@Override
	public void setVertexColor(float[] color) {
		shaderContext.setVertexColor(color);
		super.setVertexColor(color);
	}

	@Override
	public void setDepthFunc(int func) {
		if (useShaderDepthTest) {
			shaderContext.setDepthFunc(func);
		}
		super.setDepthFunc(func);
	}

	@Override
	public void setDepthMask(boolean depthWriteEnabled) {
		if (useShaderDepthTest) {
			shaderContext.setDepthMask(depthWriteEnabled ? 0xFF : 0x00);
		}
		super.setDepthMask(depthWriteEnabled);
	}

	@Override
	public void setStencilFunc(int func, int ref, int mask) {
		if (useShaderStencilTest) {
			shaderContext.setStencilFunc(func);
			// Pre-mask the reference value with the mask value
			shaderContext.setStencilRef(ref & mask);
			shaderContext.setStencilMask(mask);
		}
		super.setStencilFunc(func, ref, mask);
	}

	@Override
	public void setStencilOp(int fail, int zfail, int zpass) {
		if (useShaderStencilTest) {
			shaderContext.setStencilOpFail(fail);
			shaderContext.setStencilOpZFail(zfail);
			shaderContext.setStencilOpZPass(zpass);
		}
		super.setStencilOp(fail, zfail, zpass);
	}

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask, int alphaMask) {
		if (useShaderColorMask) {
			shaderContext.setColorMask(redMask, greenMask, blueMask, alphaMask);
			// The pre-computed not-values for the color masks
			shaderContext.setNotColorMask((~redMask) & 0xFF, (~greenMask) & 0xFF, (~blueMask) & 0xFF, (~alphaMask) & 0xFF);
			// The color mask is enabled when at least one color mask is non-zero
			shaderContext.setColorMaskEnable(redMask != 0x00 || greenMask != 0x00 || blueMask != 0x00 || alphaMask != 0x00 ? 1 : 0);

			// Do not call the "super" method in BaseRenderingEngineFunction
			proxy.setColorMask(redMask, greenMask, blueMask, alphaMask);
			// Set the on/off color masks
			re.setColorMask(redMask != 0xFF, greenMask != 0xFF, blueMask != 0xFF, stencilTestFlag);
		} else {
			super.setColorMask(redMask, greenMask, blueMask, alphaMask);
		}
	}

	@Override
	public void setAlphaFunc(int func, int ref) {
		if (useShaderAlphaTest) {
			shaderContext.setAlphaTestFunc(func);
			shaderContext.setAlphaTestRef(ref);
		}
		super.setAlphaFunc(func, ref);
	}

	@Override
	public void setBlendEquation(int mode) {
		if (useShaderBlendTest) {
			shaderContext.setBlendEquation(mode);
		}
		super.setBlendEquation(mode);
	}

	@Override
	public void setBlendFunc(int src, int dst) {
		if (useShaderBlendTest) {
			shaderContext.setBlendSrc(src);
			shaderContext.setBlendDst(dst);
			// Do not call the "super" method in BaseRenderingEngineFunction
			proxy.setBlendFunc(src, dst);
		} else {
			super.setBlendFunc(src, dst);
		}
	}

	@Override
	public void setBlendSFix(int sfix, float[] color) {
		if (useShaderBlendTest) {
			shaderContext.setBlendSFix(color);
			// Do not call the "super" method in BaseRenderingEngineFunction
			proxy.setBlendSFix(sfix, color);
		} else {
			super.setBlendSFix(sfix, color);
		}
	}

	@Override
	public void setBlendDFix(int dfix, float[] color) {
		if (useShaderBlendTest) {
			shaderContext.setBlendDFix(color);
			// Do not call the "super" method in BaseRenderingEngineFunction
			proxy.setBlendDFix(dfix, color);
		} else {
			super.setBlendDFix(dfix, color);
		}
	}

	@Override
	public void setViewport(int x, int y, int width, int height) {
		// Remember the viewport size for later use in loadFbTexture().
		viewportWidth = width;
		viewportHeight = height;
		super.setViewport(x, y, width, height);
	}

	@Override
	public boolean setCopyRedToAlpha(boolean copyRedToAlpha) {
		shaderContext.setCopyRedToAlpha(copyRedToAlpha ? 1 : 0);
		return super.setCopyRedToAlpha(copyRedToAlpha);
	}
}