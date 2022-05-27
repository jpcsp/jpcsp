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
import static jpcsp.graphics.VideoEngine.NUM_LIGHTS;
import static jpcsp.graphics.VideoEngineUtilities.getTexturePixelFormat;
import static jpcsp.util.Utilities.matrixMult;

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
 * If tessellation shaders are available, implement the Spline and Bezier
 * curved surfaces and their patch divisions by inserting a tessellation
 * evaluation shader into the shader program. A tessellation control shader
 * is not required.
 * 
 * This class is implemented as a Proxy, forwarding the non-relevant calls
 * to the proxy.
 */
public class REShader extends BaseRenderingEngineFunction {
	public    final static int ACTIVE_TEXTURE_NORMAL = 0;
	protected final static int ACTIVE_TEXTURE_CLUT = 1;
	protected final static int ACTIVE_TEXTURE_FRAMEBUFFER = 2;
	protected final static int ACTIVE_TEXTURE_INTEGER = 3;
	protected final static int ACTIVE_TEXTURE_DEPTHBUFFER = 4;
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
	protected ShaderProgram defaultCurveShaderProgram;
	protected int numberOfWeightsForShader;
	protected final static int spriteGeometryShaderInputType = GU_LINES;
	protected final static int spriteGeometryShaderOutputType = GU_TRIANGLE_STRIP;
	protected boolean useGeometryShader;
	protected boolean useTessellationShader = true;
	protected boolean useUniformBufferObject = true;
	protected boolean useNativeClut;
	protected boolean useShaderDepthTest = false;
	protected boolean useShaderStencilTest = false;
	protected boolean useShaderColorMask = false;
	// Always use the alpha test implementation in the shader
	// to support the alpha test mask (not being supported by OpenGL)
	protected boolean useShaderAlphaTest = true;
	protected boolean useShaderBlendTest = false;
	protected boolean useRenderToTexture = false;
	protected boolean useTextureBarrier = false;
	protected int clutTextureId = -1;
	protected ByteBuffer clutBuffer;
    protected DurationStatistics textureCacheLookupStatistics = new CpuDurationStatistics("Lookup in TextureCache for CLUTs");
	protected String shaderStaticDefines;
	protected String shaderDummyDynamicDefines;
	protected int shaderVersion = 120;
	protected String shaderProfile = "";
	protected ShaderProgramManager shaderProgramManager;
	protected boolean useDynamicShaders;
	protected ShaderProgram currentShaderProgram;
	protected StringBuilder infoLogs;
	protected GETexture fbTexture;
	protected boolean stencilTestFlag;
	protected int viewportWidth;
	protected int viewportHeight;
	protected FBTexture renderTexture;
	protected FBTexture copyOfRenderTexture;
	protected int pixelFormat;
	private final float[] viewMatrix = new float[16];
	private final float[] modelMatrix = new float[16];
	private final float[] projectionMatrix = new float[16];
	private final float[] modelViewMatrix = new float[16];
	private final float[] modelViewProjectionMatrix = new float[16];
	private boolean modelMatrixChanged;
	private boolean modelViewMatrixChanged;
	private boolean modelViewProjectionMatrixChanged;

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
		if (useGeometryShader && getAvailableShadingLanguageVersion() < 410 && !re.isExtensionAvailable("GL_ARB_separate_shader_objects")) {
			log.warn(String.format("Disabling Geometry Shader as it requires the extension GL_ARB_separate_shader_objects, or at least GLSL version 4.10 (available GLSL version is %d)", getAvailableShadingLanguageVersion()));
			useGeometryShader = false;
		}
		if (useGeometryShader) {
			log.info("Using Geometry Shader for SPRITES");
		}

		if (useTessellationShader && getAvailableShadingLanguageVersion() < 410 && (!re.isExtensionAvailable("GL_ARB_tessellation_shader") || !re.isExtensionAvailable("GL_ARB_separate_shader_objects"))) {
			log.warn(String.format("Disabling Tessellation Shader as it requires both extensions GL_ARB_tessellation_shader and GL_ARB_separate_shader_objects, or at least GLSL version 4.10 (available GLSL version is %d)", getAvailableShadingLanguageVersion()));
			useTessellationShader = false;
		}
		if (useTessellationShader) {
			log.info("Using Tessellation Shader for Spline and Bezier curved surfaces");
		}

		if (!ShaderContextUBO.useUBO(re)) {
			useUniformBufferObject = false;
		}
		if (useUniformBufferObject) {
			log.info("Using Uniform Buffer Object (UBO)");
		}

        useNativeClut = Settings.getInstance().readBool("emu.enablenativeclut");
        if (useNativeClut) {
        	if (!super.canNativeClut(0, -1, false)) {
    			log.warn("Disabling Native Color Lookup Tables (CLUT)");
        		useNativeClut = false;
        	} else {
    			log.info("Using Native Color Lookup Tables (CLUT)");
        	}
        }

        useShaderStencilTest = Settings.getInstance().readBool("emu.enableshaderstenciltest");
        useShaderColorMask = Settings.getInstance().readBool("emu.enableshadercolormask");

        if (useUniformBufferObject) {
			shaderContext = new ShaderContextUBO();
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
				useTextureBarrier = re.isTextureBarrierAvailable();
				log.info(String.format("Rendering to a texture with %s", useTextureBarrier ? "texture barrier" : "texture blit (slow)"));
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
		shaderContext.setDepthTex(ACTIVE_TEXTURE_DEPTHBUFFER);
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

        if (useGeometryShader) {
        	// Geometry shader requires at least GLSL version 1.50 (for GL_ARB_separate_shader_objects implementing layout location)
        	shaderVersion = Math.max(150, shaderVersion);
        }

        if (useTessellationShader) {
        	// Tessellation shader requires at least GLSL version 1.50 (for GL_ARB_tessellation_shader and for GL_ARB_separate_shader_objects implementing layout location)
        	shaderVersion = Math.max(150, shaderVersion);
        }

        addDefine(staticDefines, "USE_GEOMETRY_SHADER", useGeometryShader);
        addDefine(staticDefines, "USE_TESSELLATION_SHADER", useTessellationShader);
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

		if (shaderVersion >= 150) {
			shaderProfile = " compatibility";
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

	protected void preprocessShader(StringBuilder src, boolean hasGeometryShader, boolean hasTessellationShader, ShaderProgram shaderProgram) {
		StringBuilder defines = new StringBuilder(shaderStaticDefines);

		boolean useDynamicDefines;
		if (shaderProgram != null) {
			defines.append(shaderProgram.getDynamicDefines());
			useDynamicDefines = true;
		} else {
			// Set dummy values to the dynamic defines
			// so that the preprocessor doesn't complain about undefined values
			defines.append(shaderDummyDynamicDefines);
			addDefine(defines, "HAS_GEOMETRY_SHADER", hasGeometryShader);
			addDefine(defines, "HAS_TESSELLATION_SHADER", hasTessellationShader);
			useDynamicDefines = false;
		}
		addDefine(defines, "USE_DYNAMIC_DEFINES", useDynamicDefines);
		addDefine(defines, "USE_SHADER_DEPTH_TEST", useShaderDepthTest);
		addDefine(defines, "USE_SHADER_STENCIL_TEST", useShaderStencilTest);
		addDefine(defines, "USE_SHADER_COLOR_MASK", useShaderColorMask);
		addDefine(defines, "USE_SHADER_ALPHA_TEST", useShaderAlphaTest);
		addDefine(defines, "USE_SHADER_BLEND_TEST", useShaderBlendTest);
		addDefine(defines, "NUM_LIGHTS", NUM_LIGHTS);

		// Process #include "xxx" directives
		while (true) {
			int startInclude = src.indexOf("#include \"");
			if (startInclude < 0) {
				break;
			}
			int endInclude = src.indexOf("\"", startInclude + 10);
			String resourceName = src.substring(startInclude + 10, endInclude);

	        try {
	        	InputStream resourceStream = getClass().getResourceAsStream(resourceName);
	        	if (resourceStream == null) {
	        		break;
	        	}
	            src.replace(startInclude, endInclude + 1, Utilities.toString(resourceStream, true));
	        } catch (IOException e) {
	        	log.error(e);
	        	break;
	        }
		}

		replace(src, "// INSERT VERSION", String.format("#version %d%s", shaderVersion, shaderProfile));
		replace(src, "// INSERT DEFINES", defines.toString());
	}

	protected boolean loadShader(int shader, String resourceName, boolean silentError, boolean hasGeometryShader, boolean hasTessellationShader, ShaderProgram shaderProgram) {
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

        preprocessShader(src, hasGeometryShader, hasTessellationShader, shaderProgram);

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
        if (!linked) {
        	return false;
        }

        // Trying to avoid warning message from AMD drivers:
        // "Validation warning! - Sampler value tex has not been set."
        // and error message:
        // "Validation failed! - Different sampler types for same sample texture unit in fragment shader"
        re.useProgram(program);
        re.setUniform(re.getUniformLocation(program, Uniforms.tex.getUniformString()), ACTIVE_TEXTURE_NORMAL);
        re.setUniform(re.getUniformLocation(program, Uniforms.fbTex.getUniformString()), ACTIVE_TEXTURE_FRAMEBUFFER);
        re.setUniform(re.getUniformLocation(program, Uniforms.depthTex.getUniformString()), ACTIVE_TEXTURE_DEPTHBUFFER);
        if (useNativeClut) {
        	re.setUniform(re.getUniformLocation(program, Uniforms.clut.getUniformString()), ACTIVE_TEXTURE_CLUT);
        	re.setUniform(re.getUniformLocation(program, Uniforms.utex.getUniformString()), ACTIVE_TEXTURE_INTEGER);
        }

        boolean validated = re.validateProgram(program);
        addProgramInfoLog(program);

        return linked && validated;
	}

	protected ShaderProgram createShader(boolean hasGeometryShader, boolean hasTessellationShader, ShaderProgram shaderProgram) {
		infoLogs = new StringBuilder();

		int programId = tryCreateShader(hasGeometryShader, hasTessellationShader, shaderProgram);
		if (programId < 0) {
			switch (programId) {
				case -1: log.error("Error while compiling the vertex shader:"); break;
				case -2: log.error("Error while compiling the fragment shader:"); break;
				case -3: log.error("Error while compiling the geometry shader:"); break;
				case -4: log.error("Error while compiling the tessellation control shader:"); break;
				case -5: log.error("Error while compiling the tessellation evalution shader:"); break;
				case -6: log.error("Error while linking the shader program:"); break;
			}
			printInfoLog(true);
			shaderProgram = null;
		} else {
			if (shaderProgram == null) {
				shaderProgram = new ShaderProgram(hasGeometryShader, hasTessellationShader);
			}
			shaderProgram.setProgramId(re, programId);
			printInfoLog(false);
		}

		return shaderProgram;
	}

	private int tryCreateShader(boolean hasGeometryShader, boolean hasTessellationShader, ShaderProgram shaderProgram) {
        int program = re.createProgram();

        int vertexShader = re.createShader(RE_VERTEX_SHADER);
        if (!loadShader(vertexShader, "/jpcsp/graphics/shader.vert", false, hasGeometryShader, hasTessellationShader, shaderProgram)) {
        	return -1;
        }
        re.attachShader(program, vertexShader);

        int fragmentShader = re.createShader(RE_FRAGMENT_SHADER);
        if (!loadShader(fragmentShader, "/jpcsp/graphics/shader.frag", false, hasGeometryShader, hasTessellationShader, shaderProgram)) {
        	return -2;
        }
        re.attachShader(program, fragmentShader);

        if (hasGeometryShader) {
	        int geometryShader = re.createShader(RE_GEOMETRY_SHADER);
	        if (!loadShader(geometryShader, "/jpcsp/graphics/shader.geom", false, hasGeometryShader, hasTessellationShader, shaderProgram)) {
	        	return -3;
	        }
	        re.attachShader(program, geometryShader);
	        re.setProgramParameter(program, RE_GEOMETRY_INPUT_TYPE, spriteGeometryShaderInputType);
	        re.setProgramParameter(program, RE_GEOMETRY_OUTPUT_TYPE, spriteGeometryShaderOutputType);
	        re.setProgramParameter(program, RE_GEOMETRY_VERTICES_OUT, 4);
        }

        if (hasTessellationShader) {
        	// Load the Tessellation Evaluation shader, a Tessellation Control shader is not required
        	int tessellationEvaluationShader = re.createShader(RE_TESS_EVALUATION_SHADER);
        	if (!loadShader(tessellationEvaluationShader, "/jpcsp/graphics/shader.tese", false, hasGeometryShader, hasTessellationShader, shaderProgram)) {
        		return -5;
        	}
        	re.attachShader(program, tessellationEvaluationShader);
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
        	return -6;
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
		defaultShaderProgram = createShader(false, false, null);
		if (defaultShaderProgram != null) {
			if (useGeometryShader) {
				defaultSpriteShaderProgram = createShader(true, false, null);
			}
			if (useTessellationShader) {
				defaultCurveShaderProgram = createShader(false, true, null);
			}

			defaultShaderProgram.use(re);
		}

		if (defaultSpriteShaderProgram == null) {
			useGeometryShader = false;
		}
		if (defaultCurveShaderProgram == null) {
			useTessellationShader = false;
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
					setFlag = false;
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
			case IRenderingEngine.GU_FOG:
				shaderContext.setFogEnable(value);
				setFlag = false;
				break;
			case IRenderingEngine.GU_CLIP_PLANES:
				shaderContext.setClipPlaneEnable(value);
				setFlag = false;
				break;
			case IRenderingEngine.GU_PATCH_FACE:
				if (useTessellationShader) {
					shaderContext.setPatchFace(value);
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
		shaderContext.startDisplay(re);
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
				if (copyOfRenderTexture != null) {
					copyOfRenderTexture.delete(re);
					copyOfRenderTexture = null;
				}
			}

			// Activate the rendering to a texture
			if (renderTexture == null) {
				renderTexture = new FBTexture(display.getTopAddrFb(), bufferWidth, width, height, getTexturePixelFormat(pixelFormat));
				renderTexture.bind(re, false);
				bindActiveTexture(renderTexture);
			} else {
				renderTexture.bind(re, false);
			}
		}

		super.startDisplay();

		modelViewMatrixChanged = true;
		modelViewProjectionMatrixChanged = true;

		// We don't use Client States
		super.disableClientState(IRenderingEngine.RE_TEXTURE);
		super.disableClientState(IRenderingEngine.RE_COLOR);
		super.disableClientState(IRenderingEngine.RE_NORMAL);
		super.disableClientState(IRenderingEngine.RE_VERTEX);

		// The value of the flags are lost when starting a new display
		setShaderFlag(IRenderingEngine.GU_CLIP_PLANES, 1);
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
	public boolean canNativeCurvePrimitive() {
		// Tessellation shader supports native Spline and Bezier curves
		return useTessellationShader;
	}

	@Override
	public void setVertexInfo(VertexInfo vinfo, boolean allNativeVertexInfo, boolean useVertexColor, boolean useTexture, boolean useNormal, int type) {
		if (allNativeVertexInfo) {
			// Weight
			shaderContext.setWeightScale(weightScale[vinfo.weight]);

			// Texture
			shaderContext.setVinfoTexture(useTexture ? (vinfo.texture != 0 ? vinfo.texture : 3) : 0);
			shaderContext.setTextureScale(vinfo.transform2D ? 1.f : textureScale[vinfo.texture]);

			// Color
			shaderContext.setVinfoColor(useVertexColor ? vinfo.color : 8);

			// Normal
			shaderContext.setVinfoNormal(useNormal ? (vinfo.normal != 0 ? vinfo.normal : 3) : 0);
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
			shaderContext.setVinfoNormal(useNormal ? 3 : 0);
			shaderContext.setNormalScale(1);

			// Position
			shaderContext.setVinfoPosition(vinfo != null && vinfo.position == 0 ? 0 : 3);
			shaderContext.setPositionScale(1);
		}
		shaderContext.setVinfoTransform2D(vinfo == null || vinfo.transform2D ? 1 : 0);
		setCurrentShaderProgram(type);

		super.setVertexInfo(vinfo, allNativeVertexInfo, useVertexColor, useTexture, useNormal, type);
	}

	private static boolean isSpline(int type) {
		return type == RE_SPLINE_TRIANGLES || type == RE_SPLINE_LINES || type == RE_SPLINE_POINTS;
	}

	private static boolean isBezier(int type) {
		return type == RE_BEZIER_TRIANGLES || type == RE_BEZIER_LINES || type == RE_BEZIER_POINTS;
	}

	private static boolean isCurve(int type) {
		return isSpline(type) || isBezier(type);
	}

	private void setCurrentShaderProgram(int type) {
		int curvedSurfaceType = RE_NOT_CURVED_SURFACE;
		if (isSpline(type)) {
			curvedSurfaceType = RE_SPLINE;
		} else if (isBezier(type)) {
			curvedSurfaceType = RE_BEZIER;
		}
		shaderContext.setCurvedSurfaceType(curvedSurfaceType);

		ShaderProgram shaderProgram;
		boolean hasGeometryShader = (type == GU_SPRITES);
		boolean hasTessellationShader = isCurve(type);
		if (useDynamicShaders) {
			shaderProgram = shaderProgramManager.getShaderProgram(shaderContext, hasGeometryShader, hasTessellationShader);
			if (shaderProgram.getProgramId() == -1) {
				shaderProgram = createShader(hasGeometryShader, hasTessellationShader, shaderProgram);
				if (log.isDebugEnabled()) {
					log.debug("Created shader " + shaderProgram);
				}
				if (shaderProgram == null) {
					log.error("Cannot create shader");
					return;
				}
			}
		} else if (hasTessellationShader) {
			shaderProgram = defaultCurveShaderProgram;
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
		// No need for the frame buffer texture in direct or clear mode
		if (directMode || isClearMode()) {
			return false;
		}

		if (useShaderDepthTest && (shaderContext.getDepthTestEnable() != 0 || !shaderContext.isDepthWriteEnabled())) {
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

	private void bindActiveTexture(FBTexture renderTexture) {
		// Tell the shader which texture has to be used for the fbTex sampler.
		re.bindActiveTexture(ACTIVE_TEXTURE_FRAMEBUFFER, renderTexture.getTextureId());
		// Tell the shader which texture has to be used for the depthTex sampler.
		re.bindActiveTexture(ACTIVE_TEXTURE_DEPTHBUFFER, renderTexture.getDepthTextureId());
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
			if (renderTexture.getResizedWidth() >= width && renderTexture.getResizedHeight() >= height && renderTexture.getBufferWidth() >= bufferWidth) {
				if (useTextureBarrier) {
					// The shader can use as input the texture used as output for the frame buffer.
					// For this feature, the texture barrier extension is required.
					renderTexture.bind(re, false);

					bindActiveTexture(renderTexture);

					re.textureBarrier();
				} else {
					// The shader cannot use as input the texture used as output for the frame buffer,
					// we need to copy the output texture to another texture and use the copy
					// as input for the shader.
					if (copyOfRenderTexture == null) {
						copyOfRenderTexture = new FBTexture(renderTexture);
					}
					copyOfRenderTexture.blitFrom(re, renderTexture);

					bindActiveTexture(copyOfRenderTexture);
				}
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
		int polygonMode = RE_POLYGON_MODE_FILL;

		if (primitive == GU_SPRITES) {
			primitive = spriteGeometryShaderInputType;
		} else if (isCurve(primitive)) {
			if (primitive == RE_BEZIER_LINES || primitive == RE_SPLINE_LINES) {
				// The OpenGL polygon mode LINE is not exactly matching the PSP
				// behaviour, i.e. it is rendering more lines than the PSP.
				// This is just an approximation used for debugging.
				polygonMode = RE_POLYGON_MODE_LINE;
			} else if (primitive == RE_BEZIER_POINTS || primitive == RE_SPLINE_POINTS) {
				polygonMode = RE_POLYGON_MODE_POINT;
			}

			primitive = RE_PATCHES;

			// 16 control points for one Spline or Bezier patch
			re.setPatchParameter(RE_PATCH_VERTICES, 16);

			float patchDivS = (float) context.patch_div_s;
			float patchDivT = (float) context.patch_div_t;
			re.setPatchParameter(RE_PATCH_DEFAULT_INNER_LEVEL, new float[] { patchDivT, patchDivS });
			re.setPatchParameter(RE_PATCH_DEFAULT_OUTER_LEVEL, new float[] { patchDivS, patchDivT, patchDivS, patchDivT });
		}

		re.setPolygonMode(polygonMode);

		if (modelMatrixChanged) {
			shaderContext.setModelMatrix(modelMatrix);
			modelMatrixChanged = false;
			// We also need to update the MV matrix
			modelViewMatrixChanged = true;
		}
		if (modelViewMatrixChanged) {
			matrixMult(modelViewMatrix, viewMatrix, modelMatrix);
			shaderContext.setModelViewMatrix(modelViewMatrix);
			modelViewMatrixChanged = false;
			// We also need to update the MVP matrix
			modelViewProjectionMatrixChanged = true;
		}
		if (modelViewProjectionMatrixChanged) {
			matrixMult(modelViewProjectionMatrix, projectionMatrix, modelViewMatrix);
			shaderContext.setModelViewProjectionMatrix(modelViewProjectionMatrix);
			modelViewProjectionMatrixChanged = false;
		}

		if (!burstMode) {
			// The uniform values are specific to a shader program:
			// update the uniform values after switching the active shader program.
			shaderContext.setUniforms(re, currentShaderProgram.getProgramId());
		}

		loadFbTexture();
		loadIntegerTexture();

		// Exclude the direct and clear modes as they do not need the frame buffer texture
		if (useShaderStencilTest && useShaderDepthTest && !directMode && !isClearMode()) {
			// When the stencil and depth tests are implemented
			// in the shader, we need to disable the depth test
			// performed by OpenGL. I.e. we have to make it always pass.
			super.enableFlag(IRenderingEngine.GU_DEPTH_TEST);
			super.setDepthFunc(GeCommands.ZTST_FUNCTION_ALWAYS_PASS_PIXEL);

			// As we have now enabled the RE depth test, we have to make sure
			// that the depth values are not written
			// when the PSP depth test was actually disabled.
			// Depth values are only written when both the depth write
			// and the depth test are enabled.
			super.setDepthMask(shaderContext.isDepthWriteEnabled() && shaderContext.getDepthTestEnable() != 0);
		}

		return primitive;
	}

	private boolean requiresTextureBarrierPerPrimitive(int primitive, int count) {
		if (!useTextureBarrier) {
			return false;
		}

		int numberVertexPerPrimitive = IRenderingEngine.numberVertexPerPrimitive[primitive];
		if (numberVertexPerPrimitive == 0 || count <= numberVertexPerPrimitive) {
			return false;
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

	@Override
	public void drawArrays(int primitive, int first, int count) {
		primitive = prepareDraw(primitive, false);

		//
		// The PSP has no issue rendering overlapping primitives in a single PRIM command
		// At least this is confirmed for PRIM_SPRITES, but not verified for triangles.
		// The result looks like each primitive is rendered one after the other.
		// This is specially visible when alpha blending is enabled: the destination
		// colour behind a primitive is already including the results of the rendering
		// from the previous primitive(s), even when rendered in a single PRIM command.
		//
		// To mimic this behaviour on OpenGL, we have to make sure that when our shader
		// is using fbTex to evaluate the destination colour, we do insert a texture
		// barrier when a primitive is overlapping with previous ones.
		// As we do not really know here if the rendered primitives are overlapping or not,
		// we simply do insert a texture barrier between each primitive, which is not
		// the most efficient approach.
		// This is only done for PRIM_SPRITES.
		//
		if (requiresTextureBarrierPerPrimitive(primitive, count)) {
			int numberVertexPerPrimitive = IRenderingEngine.numberVertexPerPrimitive[primitive];
			for (int i = 0; i < count; i += numberVertexPerPrimitive) {
				if (i > 0) {
					// Insert a textureBarrier between each PRIM_SPRITES rectangle
					re.textureBarrier();
				}
				super.drawArrays(primitive, first + i, Math.min(numberVertexPerPrimitive, count - i));
			}
		} else {
			super.drawArrays(primitive, first, count);
		}
	}

	@Override
	public void drawArraysBurstMode(int primitive, int first, int count) {
		// drawArraysBurstMode is equivalent to drawArrays
		// but without the need to set the uniforms (they are unchanged
		// since the last call to drawArrays).
		primitive = prepareDraw(primitive, true);

		if (requiresTextureBarrierPerPrimitive(primitive, count)) {
			int numberVertexPerPrimitive = IRenderingEngine.numberVertexPerPrimitive[primitive];
			for (int i = 0; i < count; i += numberVertexPerPrimitive) {
				if (i > 0) {
					// Insert a textureBarrier between each PRIM_SPRITES rectangle
					re.textureBarrier();
				}
				super.drawArraysBurstMode(primitive, first + i, Math.min(numberVertexPerPrimitive, count - i));
			}
		} else {
			super.drawArraysBurstMode(primitive, first, count);
		}
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
	public boolean canNativeClut(int textureAddress, int pixelFormat, boolean textureSwizzle) {
		// The clut processing is implemented into the fragment shader
		// and the clut values are passed as a sampler2D.
		// Do not process clut's for swizzled texture, there is no performance gain.
		return useNativeClut && !textureSwizzle && super.canNativeClut(textureAddress, pixelFormat, textureSwizzle);
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
			} else if (!canNativeClut(context.texture_base_pointer[0], pixelFormat, swizzle)) {
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
			shaderContext.setDepthWriteEnabled(depthWriteEnabled);
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
	public void setAlphaFunc(int func, int ref, int mask) {
		if (useShaderAlphaTest) {
			shaderContext.setAlphaTestFunc(func);
			shaderContext.setAlphaTestRef(ref & mask);
			shaderContext.setAlphaTestMask(mask);
		}
		super.setAlphaFunc(func, ref, mask);
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

	@Override
	public void setTextureWrapMode(int s, int t) {
		shaderContext.setWrapModeS(s);
		shaderContext.setWrapModeT(t);
		super.setTextureWrapMode(s, t);
	}

	@Override
	public void setFogColor(float[] color) {
		shaderContext.setFogColor(color);
		super.setFogColor(color);
	}

	@Override
	public void setFogDist(float end, float scale) {
		shaderContext.setFogEnd(end);
		shaderContext.setFogScale(scale);
		super.setFogDist(end, scale);
	}

	@Override
	public boolean canDiscardVertices() {
		// Functionality to discard vertices has been implemented in the shaders
		return true;
	}

	@Override
	public void setViewportPos(float x, float y, float z) {
		shaderContext.setViewportPos(x, y, z);
		super.setViewportPos(x, y, z);
	}

	@Override
	public void setViewportScale(float sx, float sy, float sz) {
		shaderContext.setViewportScale(sx, sy, sz);
		super.setViewportScale(sx, sy, sz);
	}

	@Override
	public void setShadeModel(int model) {
		shaderContext.setShadeModel(model);
		super.setShadeModel(model);
	}

	@Override
	public void setLightAmbientColor(int light, float[] color) {
		shaderContext.setLightAmbientColor(light, color);
	}

	@Override
	public void setLightConstantAttenuation(int light, float constant) {
		shaderContext.setLightConstantAttenuation(light, constant);
	}

	@Override
	public void setLightDiffuseColor(int light, float[] color) {
		shaderContext.setLightDiffuseColor(light, color);
	}

	@Override
	public void setLightDirection(int light, float[] direction) {
		shaderContext.setLightDirection(light, direction);
	}

	@Override
	public void setLightLinearAttenuation(int light, float linear) {
		shaderContext.setLightLinearAttenuation(light, linear);
	}

	@Override
	public void setLightPosition(int light, float[] position) {
		shaderContext.setLightPosition(light, position);
	}

	@Override
	public void setLightQuadraticAttenuation(int light, float quadratic) {
		shaderContext.setLightQuadraticAttenuation(light, quadratic);
	}

	@Override
	public void setLightSpecularColor(int light, float[] color) {
		shaderContext.setLightSpecularColor(light, color);
	}

	@Override
	public void setLightSpotCutoff(int light, float cutoff, float pspCutoff) {
		shaderContext.setLightSpotLightCutoff(light, pspCutoff);
	}

	@Override
	public void setLightSpotExponent(int light, float exponent) {
		shaderContext.setLightSpotLightExponent(light, exponent);
	}

	@Override
	public void setLightModelAmbientColor(float[] color) {
		shaderContext.setAmbientLightColor(color);
	}

	@Override
	public void setMaterialAmbientColor(float[] color) {
		shaderContext.setMaterialAmbientColor(color);
	}

	@Override
	public void setMaterialDiffuseColor(float[] color) {
		shaderContext.setMaterialDiffuseColor(color);
	}

	@Override
	public void setMaterialEmissionColor(float[] color) {
		shaderContext.setMaterialEmissionColor(color);
	}

	@Override
	public void setMaterialSpecularColor(float[] color) {
		shaderContext.setMaterialSpecularColor(color);
	}

	@Override
	public void setMaterialShininess(float shininess) {
		shaderContext.setMaterialShininess(shininess);
	}

	@Override
	public void setTextureMatrix(float[] values) {
		if (values == null) {
			values = identityMatrix;
		}
		shaderContext.setTextureMatrix(values);
	}

	@Override
	public void setViewMatrix(float[] values) {
		if (values == null) {
			values = identityMatrix;
		}
		System.arraycopy(values, 0, viewMatrix, 0, viewMatrix.length);
		modelViewMatrixChanged = true;
		super.setViewMatrix(values);
	}

	@Override
	public void setModelMatrix(float[] values) {
		if (values == null) {
			values = identityMatrix;
		}
		System.arraycopy(values, 0, modelMatrix, 0, modelMatrix.length);
		modelMatrixChanged = true;
		super.setModelMatrix(values);
	}

	@Override
	public void setProjectionMatrix(float[] values) {
		if (values == null) {
			values = identityMatrix;
		}
		System.arraycopy(values, 0, projectionMatrix, 0, projectionMatrix.length);
		modelViewProjectionMatrixChanged = true;
		super.setProjectionMatrix(values);
	}

	@Override
	public void setModelViewMatrix(float[] values) {
		if (values == null) {
			values = identityMatrix;
		}
		System.arraycopy(values, 0, modelMatrix, 0, modelMatrix.length);
		System.arraycopy(identityMatrix, 0, viewMatrix, 0, viewMatrix.length);
		modelMatrixChanged = true;
		super.setModelViewMatrix(values);
	}

	@Override
	public void setTextureEnvColor(float[] color) {
		shaderContext.setTexEnvColor(color);
	}

	@Override
	public void setSplineInfo(int ucount, int vcount, int utype, int vtype) {
		if (useTessellationShader) {
			shaderContext.setSplineInfo(ucount, vcount, utype, vtype);
		}
		super.setSplineInfo(ucount, vcount, utype, vtype);
	}
}