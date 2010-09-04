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
import java.nio.Buffer;

import jpcsp.Settings;
import jpcsp.graphics.Uniforms;
import jpcsp.graphics.VertexInfo;
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

		loadShaders();

		shaderContext = new ShaderContext();

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

		shaderContext.colorDoubling = 1;
	}

	protected boolean loadShader(int shader, String resourceName, boolean silentError) {
        String[] srcArray = new String[1];

        try {
            srcArray[0] = Utilities.toString(getClass().getResourceAsStream(resourceName), true);
        } catch (IOException e) {
        	log.error(e);
        }

        boolean compiled = re.compilerShader(shader, srcArray);
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
		boolean availableShaders = re.isFunctionAvailable("glCreateShader")
		                        && re.isFunctionAvailable("glShaderSource")
		                        && re.isFunctionAvailable("glCompileShader")
		                        && re.isFunctionAvailable("glCreateProgram")
		                        && re.isFunctionAvailable("glAttachShader")
		                        && re.isFunctionAvailable("glLinkProgram")
		                        && re.isFunctionAvailable("glValidateProgram")
		                        && re.isFunctionAvailable("glUseProgram");

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
				shaderContext.lightEnabled[flag - IRenderingEngine.GU_LIGHT0] = value;
				break;
			case IRenderingEngine.GU_COLOR_TEST:
				shaderContext.ctestEnable = value;
				break;
			case IRenderingEngine.GU_LIGHTING:
				shaderContext.lightingEnable = value;
				break;
			case IRenderingEngine.GU_TEXTURE_2D:
				shaderContext.texEnable = value;
				break;
		}
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
		shaderContext.zPos = zpos;
		shaderContext.zScale = zscale;
		super.setDepthRange(zpos, zscale, near, far);
	}

	@Override
	public void setLightMode(int mode) {
		shaderContext.lightMode = mode;
		super.setLightMode(mode);
	}

	@Override
	public void setLightType(int light, int type, int kind) {
		shaderContext.lightType[light] = type;
		shaderContext.lightKind[light] = kind;
		super.setLightType(light, type, kind);
	}

	@Override
	public void setTextureEnvironmentMapping(int u, int v) {
		shaderContext.texShade[0] = u;
		shaderContext.texShade[1] = v;
		super.setTextureEnvironmentMapping(u, v);
	}

	@Override
	public void setColorTestFunc(int func) {
		shaderContext.ctestFunc = func;
		super.setColorTestFunc(func);
	}

	@Override
	public void setColorTestMask(int[] values) {
		shaderContext.ctestMsk[0] = values[0];
		shaderContext.ctestMsk[1] = values[1];
		shaderContext.ctestMsk[2] = values[2];
		super.setColorTestMask(values);
	}

	@Override
	public void setColorTestReference(int[] values) {
		shaderContext.ctestRef[0] = values[0];
		shaderContext.ctestRef[1] = values[1];
		shaderContext.ctestRef[2] = values[2];
		super.setColorTestReference(values);
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
		shaderContext.texEnvMode[0] = func;
		shaderContext.texEnvMode[1] = alphaUsed ? 1 : 0;
		shaderContext.colorDoubling = colorDoubled ? 2.0f : 1.0f;
		super.setTextureFunc(func, alphaUsed, colorDoubled);
	}

	@Override
	public int setBones(int count, float[] values) {
		shaderContext.numberBones = count;
		if (count > 0) {
			System.arraycopy(values, 0, shaderContext.boneMatrix, 0, 16 * count);
		}
        numberOfWeightsForShader = count;
		super.setBones(count, values);

		return numberOfWeightsForShader; // Number of weights to be copied into the Buffer
	}

	@Override
	public void setTextureMapMode(int mode, int proj) {
		shaderContext.texMapMode = mode;
		shaderContext.texMapProj = proj;
		super.setTextureMapMode(mode, proj);
	}

	@Override
	public void setColorMaterial(boolean ambient, boolean diffuse, boolean specular) {
		shaderContext.matFlags[0] = ambient  ? 1 : 0;
		shaderContext.matFlags[1] = diffuse  ? 1 : 0;
		shaderContext.matFlags[2] = specular ? 1 : 0;
		super.setColorMaterial(ambient, diffuse, specular);
	}

	@Override
	public void startDisplay() {
        re.useProgram(shaderProgram);
		super.startDisplay();
	}

	@Override
	public void endDisplay() {
        re.useProgram(0);
		super.endDisplay();
	}

	@Override
	public void enableClientState(int type) {
		super.disableClientState(type);
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
		super.disableClientState(type);
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, Buffer buffer) {
		if (size > 0) {
            re.setVertexAttribPointer(shaderAttribWeights1, Math.min(size, 4), type, false, stride, buffer);
            if (size > 4) {
                re.setVertexAttribPointer(shaderAttribWeights2, size - 4, type, false, stride, buffer);
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
			shaderContext.weightScale = weightScale[vinfo.weight];
			shaderContext.textureScale = vinfo.transform2D ? 1.f : textureScale[vinfo.texture];
			shaderContext.vinfoColor = useVertexColor ? vinfo.color : -1;
			shaderContext.normalScale = vinfo.transform2D ? 1.f : normalScale[vinfo.normal];
			shaderContext.vinfoPosition = vinfo.position;
			shaderContext.positionScale = vinfo.transform2D ? 1.f : positionScale[vinfo.position];
		} else {
			shaderContext.weightScale = 1;
			shaderContext.textureScale = 1;
			shaderContext.vinfoColor = useVertexColor ? 0 : -1;
			shaderContext.normalScale = 1;
			shaderContext.vinfoPosition = 0;
			shaderContext.positionScale = 1;
		}
		shaderContext.vinfoTransform2D = vinfo == null || vinfo.transform2D ? 1 : 0;
		super.setVertexInfo(vinfo, allNativeVertexInfo, useVertexColor);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, Buffer buffer) {
		re.setVertexAttribPointer(shaderAttribPosition, size, type, false, stride, buffer);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, long offset) {
		re.setVertexAttribPointer(shaderAttribPosition, size, type, false, stride, offset);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, Buffer buffer) {
		re.setVertexAttribPointer(shaderAttribColor, size, type, false, stride, buffer);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, long offset) {
		re.setVertexAttribPointer(shaderAttribColor, size, type, false, stride, offset);
	}

	@Override
	public void setNormalPointer(int type, int stride, Buffer buffer) {
		re.setVertexAttribPointer(shaderAttribNormal, 3, type, false, stride, buffer);
	}

	@Override
	public void setNormalPointer(int type, int stride, long offset) {
		re.setVertexAttribPointer(shaderAttribNormal, 3, type, false, stride, offset);
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, Buffer buffer) {
		re.setVertexAttribPointer(shaderAttribTexture, size, type, false, stride, buffer);
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
}
