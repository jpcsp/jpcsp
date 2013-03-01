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
package jpcsp.graphics.RE.software;

import java.nio.Buffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;

import jpcsp.Memory;
import jpcsp.graphics.VertexInfo;
import jpcsp.graphics.VertexState;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.BaseRenderingEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.ImageReader;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 * This RenderingEngine class implements a software-based rendering,
 * not using OpenGL or any GPU.
 * This is probably the most accurate implementation but also the slowest one.
 */
public class RESoftware extends BaseRenderingEngine {
	private static final boolean useTextureCache = true;
    protected int genTextureId;
    protected int bindTexture;
    protected VertexState v1 = new VertexState();
    protected VertexState v2 = new VertexState();
    protected VertexState v3 = new VertexState();
    protected VertexState v4 = new VertexState();
    protected VertexState v5 = new VertexState();
    protected VertexState v6 = new VertexState();
    protected RendererExecutor rendererExecutor;
    protected HashMap<Integer, CachedTextureResampled> cachedTextures = new HashMap<Integer, CachedTextureResampled>();
    protected int textureBufferWidth;
    protected static DurationStatistics drawArraysStatistics = new DurationStatistics("RESoftware drawArrays");
    public static DurationStatistics triangleRender3DStatistics = new DurationStatistics("RESoftware TriangleRender3D");
    public static DurationStatistics triangleRender2DStatistics = new DurationStatistics("RESoftware TriangleRender2D");
    public static DurationStatistics spriteRenderStatistics = new DurationStatistics("RESoftware SpriteRender");
    protected static DurationStatistics cachedTextureStatistics = new DurationStatistics("RESoftware CachedTexture");
    public static DurationStatistics textureResamplingStatistics = new DurationStatistics("RESftware Texture resampling");
    protected BoundingBoxRenderer boundingBoxRenderer;
    protected boolean boundingBoxVisible;
    protected BufferVertexReader bufferVertexReader;
    protected boolean useVertexTexture;

    public RESoftware() {
    	log.info("Using SoftwareRenderer");
    }

    @Override
	public void exit() {
		if (DurationStatistics.collectStatistics) {
			log.info(drawArraysStatistics);
			log.info(triangleRender3DStatistics);
			log.info(triangleRender2DStatistics);
			log.info(spriteRenderStatistics);
			log.info(cachedTextureStatistics);
			log.info(textureResamplingStatistics);
		}
	}

	@Override
	public void startDirectRendering(boolean textureEnabled,
			boolean depthWriteEnabled, boolean colorWriteEnabled,
			boolean setOrthoMatrix, boolean orthoInverted, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endDirectRendering() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startDisplay() {
		context = VideoEngine.getInstance().getContext();
		rendererExecutor = RendererExecutor.getInstance();
	}

	@Override
	public void endDisplay() {
	}

	@Override
	public void enableFlag(int flag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disableFlag(int flag) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMorphWeight(int index, float value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPatchDiv(int s, int t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPatchPrim(int prim) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMatrixMode(int type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMatrix(float[] values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void multMatrix(float[] values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endModelViewMatrixUpdate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setViewport(int x, int y, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDepthRange(float zpos, float zscale, float near, float far) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDepthFunc(int func) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setShadeModel(int model) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMaterialEmissiveColor(float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMaterialAmbientColor(float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMaterialDiffuseColor(float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMaterialSpecularColor(float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMaterialShininess(float shininess) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightModelAmbientColor(float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightMode(int mode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightPosition(int light, float[] position) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightDirection(int light, float[] direction) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightSpotExponent(int light, float exponent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightSpotCutoff(int light, float cutoff) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightConstantAttenuation(int light, float constant) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightLinearAttenuation(int light, float linear) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightQuadraticAttenuation(int light, float quadratic) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightAmbientColor(int light, float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightDiffuseColor(int light, float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightSpecularColor(int light, float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLightType(int light, int type, int kind) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBlendFunc(int src, int dst) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBlendColor(float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLogicOp(int logicOp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDepthMask(boolean depthWriteEnabled) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColorMask(int redMask, int greenMask, int blueMask,
			int alphaMask) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColorMask(boolean redWriteEnabled,
			boolean greenWriteEnabled, boolean blueWriteEnabled,
			boolean alphaWriteEnabled) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTextureWrapMode(int s, int t) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTextureMipmapMinLevel(int level) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTextureMipmapMaxLevel(int level) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTextureMipmapMinFilter(int filter) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTextureMipmapMagFilter(int filter) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColorMaterial(boolean ambient, boolean diffuse,
			boolean specular) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTextureMapMode(int mode, int proj) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTextureEnvironmentMapping(int u, int v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUniform(int id, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUniform(int id, int value1, int value2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUniform(int id, float value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUniform2(int id, int[] values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUniform3(int id, int[] values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUniform3(int id, float[] values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUniform4(int id, int[] values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUniform4(int id, float[] values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUniformMatrix4(int id, int count, float[] values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColorTestFunc(int func) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColorTestReference(int[] values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColorTestMask(int[] values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTextureFunc(int func, boolean alphaUsed, boolean colorDoubled) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int setBones(int count, float[] values) {
		return count;
	}

	@Override
	public void setTexEnv(int name, int param) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTexEnv(int name, float param) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int createShader(int type) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean compilerShader(int shader, String source) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int createProgram() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void useProgram(int program) {
	}

	@Override
	public void attachShader(int program, int shader) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean linkProgram(int program) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean validateProgram(int program) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getUniformLocation(int program, String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAttribLocation(int program, String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void bindAttribLocation(int program, int index, String name) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getShaderInfoLog(int shader) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProgramInfoLog(int program) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isExtensionAvailable(String name) {
		// TODO Auto-generated method stub
		return false;
	}

	protected void render(IRenderer renderer) {
		if (renderer.prepare(context)) {
			rendererExecutor.render(renderer);
		}
	}

	protected void drawSprite(SpriteRenderer spriteRenderer, VertexState v1, VertexState v2) {
		spriteRenderer.setVertex(v1, v2);
		render(spriteRenderer);
	}

	protected CachedTextureResampled getCachedTexture() {
		CachedTextureResampled cachedTexture = cachedTextures.get(bindTexture);
		if (cachedTexture != null) {
			cachedTexture.setClut();
		}

		return cachedTexture;
	}

	protected void drawArraysSprites(int first, int count) {
		CachedTextureResampled cachedTexture = getCachedTexture();
		SpriteRenderer spriteRenderer = new SpriteRenderer(context, cachedTexture, useVertexTexture);
		boolean readTexture = context.textureFlag.isEnabled() && !context.clearMode;
		Memory mem = Memory.getInstance();
		for (int i = first; i < count - 1; i += 2) {
			int addr1 = context.vinfo.getAddress(mem, i);
			int addr2 = context.vinfo.getAddress(mem, i + 1);
			context.vinfo.readVertex(mem, addr1, v1, readTexture);
			context.vinfo.readVertex(mem, addr2, v2, readTexture);

			drawSprite(spriteRenderer, v1, v2);
		}
	}

	protected void drawTriangle(TriangleRenderer triangleRenderer, VertexState v1, VertexState v2, VertexState v3, boolean invertedFrontFace) {
        triangleRenderer.setVertex(v1, v2, v3);
    	if (!triangleRenderer.isCulled(invertedFrontFace)) {
    		render(triangleRenderer);
    	}
	}

	protected boolean isSprite(VertexInfo vinfo, VertexState tv1, VertexState tv2, VertexState tv3, VertexState tv4) {
		// Sprites are only available in 2D
		if (!vinfo.transform2D) {
			return false;
		}

		// Sprites are not culled. Keep triangles when the back face culling is enabled.
		if (!context.clearMode && context.cullFaceFlag.isEnabled()) {
			return false;
		}

		// Sprites have no normal
		if (vinfo.normal != 0) {
			return false;
		}

		// Color doubling not correctly handled on sprites
		if (context.textureColorDoubled) {
			return false;
		}

		if (vinfo.color != 0) {
			// Color of 4 vertex must be equal
			if (!Utilities.sameColor(tv1.c, tv2.c, tv3.c, tv4.c)) {
				return false;
			}
		}

		// x1 == x2 && y1 == y3 && x4 == x3 && y4 == y2
		if (tv1.p[0] == tv2.p[0] && tv1.p[1] == tv3.p[1] && tv4.p[0] == tv3.p[0] && tv4.p[1] == tv2.p[1]) {
			// z1 == z2 && z1 == z3 && z1 == z4
			if (tv1.p[2] == tv2.p[2] && tv1.p[2] == tv3.p[2] && tv1.p[2] == tv3.p[2]) {
				if (vinfo.texture == 0) {
					return true;
				}
				// u1 == u2 && v1 == v3 && u4 == u3 && v4 == v2
				if (tv1.t[0] == tv2.t[0] && tv1.t[1] == tv3.t[1] && tv4.t[0] == tv3.t[0] && tv4.t[1] == tv2.t[1]) {
					return true;
				}
				// v1 == v2 && u1 == u3 && v4 == v3 && u4 == u2
//				if (tv1.t[1] == tv2.t[1] && tv1.t[0] == tv3.t[0] && tv4.t[1] == tv3.t[1] && tv4.t[0] == tv2.t[0]) {
//					return true;
//				}
			}
		}

		// y1 == y2 && x1 == x3 && y4 == y3 && x4 == x2
		if (tv1.p[1] == tv2.p[1] && tv1.p[0] == tv3.p[0] && tv4.p[1] == tv3.p[1] && tv4.p[0] == tv2.p[0]) {
			// z1 == z2 && z1 == z3 && z1 == z4
			if (tv1.p[2] == tv2.p[2] && tv1.p[2] == tv3.p[2] && tv1.p[2] == tv3.p[2]) {
				if (vinfo.texture == 0) {
					return true;
				}
				// v1 == v2 && u1 == u3 && v4 == v3 && u4 == u2
				if (tv1.t[1] == tv2.t[1] && tv1.t[0] == tv3.t[0] && tv4.t[1] == tv3.t[1] && tv4.t[0] == tv2.t[0]) {
					return true;
				}
				// u1 == u2 && v1 == v3 && u4 == u3 && v4 == v2
//				if (tv1.t[0] == tv2.t[0] && tv1.t[1] == tv3.t[1] && tv4.t[0] == tv3.t[0] && tv4.t[1] == tv2.t[1]) {
//					return true;
//				}
			}
		}

		return false;
	}

	protected void resetBufferVertexReader() {
		bufferVertexReader = null;
	}

	protected void readVertex(Memory mem, int index, VertexState v, boolean readTexture) {
		if (bufferVertexReader == null) {
			int addr = context.vinfo.getAddress(mem, index);
			context.vinfo.readVertex(mem, addr, v, readTexture);
		} else {
			// This is used for spline and bezier curves:
			// the VideoEngine is computing the vertices and is pushing them into a buffer.
			bufferVertexReader.readVertex(index, v);
		}
		if (context.vinfo.weight != 0) {
			VideoEngine.doSkinning(context.bone_uploaded_matrix, context.vinfo, v);
		}
	}

	protected void drawArraysTriangleStrips(int first, int count) {
		Memory mem = Memory.getInstance();
		CachedTextureResampled cachedTexture = getCachedTexture();
		TriangleRenderer triangleRenderer = new TriangleRenderer(context, cachedTexture, useVertexTexture);
		SpriteRenderer spriteRenderer = null;
		VertexState tv1 = null;
		VertexState tv2 = null;
		VertexState tv3 = null;
		VertexState tv4 = v1;
		boolean readTexture = context.textureFlag.isEnabled() && !context.clearMode;
		for (int i = 0; i < count; i++) {
			readVertex(mem, first + i, tv4, readTexture);
			if (tv3 != null) {
				// Displaying a sprite (i.e. rectangular area) is faster.
				// Try to merge adjacent triangles if they form a sprite.
				if (isSprite(context.vinfo, tv1, tv2, tv3, tv4)) {
					if (spriteRenderer == null) {
						spriteRenderer = new SpriteRenderer(context, cachedTexture, useVertexTexture);
					}
					drawSprite(spriteRenderer, tv1, tv4);
					v5.copy(tv3);
					v6.copy(tv4);
					v1.copy(v5);
					v2.copy(v6);
					tv1 = v1;
					tv2 = v2;
					tv3 = null;
					tv4 = v3;
				} else {
					// The Front face direction is inverted every 2 triangles in the strip.
					drawTriangle(triangleRenderer, tv1, tv2, tv3, ((i - 3) & 1) != 0);
					VertexState v = tv1;
					tv1 = tv2;
					tv2 = tv3;
					tv3 = tv4;
					tv4 = v;
				}
			} else if (tv1 == null) {
				tv1 = tv4;
				tv4 = v2;
			} else if (tv2 == null) {
				tv2 = tv4;
				tv4 = v3;
			} else {
				tv3 = tv4;
				tv4 = v4;
			}
		}

		if (tv3 != null) {
			// The Front face direction is inverted every 2 triangles in the strip.
			drawTriangle(triangleRenderer, tv1, tv2, tv3, (count & 1) == 0);
		}
	}

	protected void drawArraysTriangles(int first, int count) {
		Memory mem = Memory.getInstance();
		CachedTextureResampled cachedTexture = getCachedTexture();
		TriangleRenderer triangleRenderer = new TriangleRenderer(context, cachedTexture, useVertexTexture);
		boolean readTexture = context.textureFlag.isEnabled() && !context.clearMode;
		for (int i = 0; i < count; i += 3) {
			readVertex(mem, first + i, v1, readTexture);
			readVertex(mem, first + i + 1, v2, readTexture);
			readVertex(mem, first + i + 2, v3, readTexture);

			drawTriangle(triangleRenderer, v1, v2, v3, false);
		}
	}

	protected void drawArraysTriangleFan(int first, int count) {
		Memory mem = Memory.getInstance();
		CachedTextureResampled cachedTexture = getCachedTexture();
		TriangleRenderer triangleRenderer = new TriangleRenderer(context, cachedTexture, useVertexTexture);
		VertexState tv1 = null;
		VertexState tv2 = null;
		VertexState tv3 = v1;
		boolean readTexture = context.textureFlag.isEnabled() && !context.clearMode;
		for (int i = 0; i < count; i++) {
			readVertex(mem, first + i, tv3, readTexture);
			if (tv2 != null) {
				drawTriangle(triangleRenderer, tv1, tv2, tv3, false);
				VertexState v = tv2;
				tv2 = tv3;
				tv3 = v;
			} else if (tv1 == null) {
				tv1 = tv3;
				tv3 = v2;
			} else {
				tv2 = tv3;
				tv3 = v3;
			}
		}
	}

	@Override
	public void drawArrays(int primitive, int first, int count) {
		drawArraysStatistics.start();
		switch (primitive) {
			case IRenderingEngine.GU_SPRITES:
				drawArraysSprites(first, count);
				break;
			case IRenderingEngine.GU_TRIANGLE_STRIP:
				drawArraysTriangleStrips(first, count);
				break;
			case IRenderingEngine.GU_TRIANGLES:
				drawArraysTriangles(first, count);
				break;
			case IRenderingEngine.GU_TRIANGLE_FAN:
				drawArraysTriangleFan(first, count);
				break;
		}
		drawArraysStatistics.end();
	}

	@Override
	public int genBuffer() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void deleteBuffer(int buffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBufferData(int target, int size, Buffer buffer, int usage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBufferSubData(int target, int offset, int size, Buffer buffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bindBuffer(int target, int buffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enableClientState(int type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disableClientState(int type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enableVertexAttribArray(int id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disableVertexAttribArray(int id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, long offset) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTexCoordPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (bufferVertexReader == null) {
			bufferVertexReader = new BufferVertexReader();
		}
		bufferVertexReader.setTextureComponentInfo(size, type, stride, bufferSize, buffer);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, long offset) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColorPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (bufferVertexReader == null) {
			bufferVertexReader = new BufferVertexReader();
		}
		bufferVertexReader.setColorComponentInfo(size, type, stride, bufferSize, buffer);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, long offset) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (bufferVertexReader == null) {
			bufferVertexReader = new BufferVertexReader();
		}
		bufferVertexReader.setVertexComponentInfo(size, type, stride, bufferSize, buffer);
	}

	@Override
	public void setNormalPointer(int type, int stride, long offset) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setNormalPointer(int type, int stride, int bufferSize, Buffer buffer) {
		if (bufferVertexReader == null) {
			bufferVertexReader = new BufferVertexReader();
		}
		bufferVertexReader.setNormalComponentInfo(type, stride, bufferSize, buffer);
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, long offset) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (bufferVertexReader == null) {
			bufferVertexReader = new BufferVertexReader();
		}
		bufferVertexReader.setWeightComponentInfo(size, type, stride, bufferSize, buffer);
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type,
			boolean normalized, int stride, long offset) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVertexAttribPointer(int id, int size, int type,
			boolean normalized, int stride, int bufferSize, Buffer buffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPixelStore(int rowLength, int alignment) {
		textureBufferWidth = rowLength;
	}

	@Override
	public int genTexture() {
		return genTextureId++;
	}

	@Override
	public void bindTexture(int texture) {
		bindTexture = texture;
	}

	@Override
	public void deleteTexture(int texture) {
		cachedTextures.remove(texture);
	}

	@Override
	public void setCompressedTexImage(int level, int internalFormat, int width, int height, int compressedSize, Buffer buffer) {
		if (useTextureCache) {
			cachedTextureStatistics.start();
			// TODO Cache all the texture levels
			if (level == 0) {
				IMemoryReader imageReader = ImageReader.getImageReader(context.texture_base_pointer[level], width, height, context.texture_buffer_width[level], internalFormat, false, 0, 0, 0, 0, 0, 0, null, null);
				CachedTexture cachedTexture = CachedTexture.getCachedTexture(width, height, internalFormat, imageReader);
				CachedTextureResampled cachedTextureResampled = new CachedTextureResampled(cachedTexture);
				cachedTextures.put(bindTexture, cachedTextureResampled);
			}
			cachedTextureStatistics.end();
		}
	}

	@Override
	public void setTexImage(int level, int internalFormat, int width, int height, int format, int type, int textureSize, Buffer buffer) {
		if (useTextureCache) {
			cachedTextureStatistics.start();
			// TODO Cache all the texture levels
			if (level == 0) {
				CachedTexture cachedTexture = null;
				if (buffer instanceof IntBuffer) {
					cachedTexture = CachedTexture.getCachedTexture(textureBufferWidth, height, format, ((IntBuffer) buffer).array(), buffer.arrayOffset(), textureSize >> 2);
				} else if (buffer instanceof ShortBuffer) {
					cachedTexture = CachedTexture.getCachedTexture(textureBufferWidth, height, format, ((ShortBuffer) buffer).array(), buffer.arrayOffset(), textureSize >> 1);
				}
				CachedTextureResampled cachedTextureResampled = new CachedTextureResampled(cachedTexture);
				cachedTextures.put(bindTexture, cachedTextureResampled);
			}
			cachedTextureStatistics.end();
		}
	}

	@Override
	public void setTexSubImage(int level, int xOffset, int yOffset, int width,
			int height, int format, int type, int textureSize, Buffer buffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void getTexImage(int level, int format, int type, Buffer buffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void copyTexSubImage(int level, int xOffset, int yOffset, int x,
			int y, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStencilOp(int fail, int zfail, int zpass) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setStencilFunc(int func, int ref, int mask) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAlphaFunc(int func, int ref) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFogHint() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFogColor(float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFogDist(float start, float end) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTextureEnvColor(float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFrontFace(boolean cw) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setScissor(int x, int y, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBlendEquation(int mode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLineSmoothHint() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasBoundingBox() {
		return true;
	}

	@Override
	public void beginBoundingBox(int numberOfVertexBoundingBox) {
		boundingBoxRenderer = new BoundingBoxRenderer(context);
		boundingBoxVisible = true;
	}

	@Override
	public void drawBoundingBox(float[][] values) {
		if (boundingBoxVisible) {
			boundingBoxRenderer.drawBoundingBox(values);
			if (!boundingBoxRenderer.prepare(context)) {
				boundingBoxVisible = false;
			}
		}
	}

	@Override
	public void endBoundingBox(VertexInfo vinfo) {
	}

	@Override
	public boolean isBoundingBoxVisible() {
		return boundingBoxVisible;
	}

	@Override
	public int genQuery() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void beginQuery(int id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endQuery() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getQueryResultAvailable(int id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getQueryResult(int id) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void clear(float red, float green, float blue, float alpha) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canAllNativeVertexInfo() {
		return true;
	}

	@Override
	public boolean canNativeSpritesPrimitive() {
		return true;
	}

	@Override
	public void setVertexInfo(VertexInfo vinfo, boolean allNativeVertexInfo, boolean useVertexColor, boolean useTexture, int type) {
		this.useVertexTexture = useTexture;
		resetBufferVertexReader();
	}

	@Override
	public void setProgramParameter(int program, int parameter, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isQueryAvailable() {
		return false;
	}

	@Override
	public boolean isShaderAvailable() {
		return false;
	}

	@Override
	public int getUniformBlockIndex(int program, String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void bindBufferBase(int target, int bindingPoint, int buffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUniformBlockBinding(int program, int blockIndex,
			int bindingPoint) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getUniformIndex(int program, String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int[] getUniformIndices(int program, String[] names) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getActiveUniformOffset(int program, int uniformIndex) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isFramebufferObjectAvailable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int genFramebuffer() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int genRenderbuffer() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void deleteFramebuffer(int framebuffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteRenderbuffer(int renderbuffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bindFramebuffer(int target, int framebuffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void bindRenderbuffer(int renderbuffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRenderbufferStorage(int internalFormat, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFramebufferRenderbuffer(int target, int attachment,
			int renderbuffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFramebufferTexture(int target, int attachment, int texture,
			int level) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int genVertexArray() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void bindVertexArray(int id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteVertexArray(int id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isVertexArrayAvailable() {
		return false;
	}

	@Override
	public void multiDrawArrays(int primitive, IntBuffer first, IntBuffer count) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drawArraysBurstMode(int primitive, int first, int count) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPixelTransfer(int parameter, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPixelTransfer(int parameter, float value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPixelTransfer(int parameter, boolean value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPixelMap(int map, int mapSize, Buffer buffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canNativeClut(int textureAddress) {
		if (Memory.isVRAM(textureAddress)) {
			return true;
		}
		return !useTextureCache;
	}

	@Override
	public void setActiveTexture(int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTextureFormat(int pixelFormat, boolean swizzle) {
	}

	@Override
	public void bindActiveTexture(int index, int texture) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTextureAnisotropy(float value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getMaxTextureAnisotropy() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getShadingLanguageVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setBlendSFix(int sfix, float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBlendDFix(int dfix, float[] color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startClearMode(boolean color, boolean stencil, boolean depth) {
	}

	@Override
	public void endClearMode() {
	}

	@Override
	public void waitForRenderingCompletion() {
		rendererExecutor.waitForRenderingCompletion();
	}

	@Override
	public boolean canReadAllVertexInfo() {
		// drawArrays doesn't need vertex infos in buffers, it can read directly from memory.
		return true;
	}

	@Override
	public void readStencil(int x, int y, int width, int height, int bufferSize, Buffer buffer) {
	}

	@Override
	public void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
	}

	@Override
	public boolean checkAndLogErrors(String logComment) {
		// No error
		return false;
	}

	@Override
	public boolean setCopyRedToAlpha(boolean copyRedToAlpha) {
		return true;
	}

	@Override
	public void drawElements(int primitive, int count, int indexType, Buffer indices, int indicesOffset) {
	}

	@Override
	public void drawElements(int primitive, int count, int indexType, long indicesOffset) {
	}

	@Override
	public void multiDrawElements(int primitive, IntBuffer first, IntBuffer count, int indexType, long indicesOffset) {
	}

	@Override
	public void drawElementsBurstMode(int primitive, int count, int indexType, long indicesOffset) {
	}
}
