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
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.NullRenderingEngine;
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
public class RESoftware extends NullRenderingEngine {
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
	public void startDisplay() {
		context = VideoEngine.getInstance().getContext();
		rendererExecutor = RendererExecutor.getInstance();
	}

	@Override
	public int setBones(int count, float[] values) {
		return count;
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
			context.vinfo.readVertex(mem, addr1, v1, readTexture, VideoEngine.getInstance().isDoubleTexture2DCoords());
			context.vinfo.readVertex(mem, addr2, v2, readTexture, VideoEngine.getInstance().isDoubleTexture2DCoords());

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
			context.vinfo.readVertex(mem, addr, v, readTexture, VideoEngine.getInstance().isDoubleTexture2DCoords());
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
	public void setTexCoordPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (bufferVertexReader == null) {
			bufferVertexReader = new BufferVertexReader();
		}
		bufferVertexReader.setTextureComponentInfo(size, type, stride, bufferSize, buffer);
	}

	@Override
	public void setColorPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (bufferVertexReader == null) {
			bufferVertexReader = new BufferVertexReader();
		}
		bufferVertexReader.setColorComponentInfo(size, type, stride, bufferSize, buffer);
	}

	@Override
	public void setVertexPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (bufferVertexReader == null) {
			bufferVertexReader = new BufferVertexReader();
		}
		bufferVertexReader.setVertexComponentInfo(size, type, stride, bufferSize, buffer);
	}

	@Override
	public void setNormalPointer(int type, int stride, int bufferSize, Buffer buffer) {
		if (bufferVertexReader == null) {
			bufferVertexReader = new BufferVertexReader();
		}
		bufferVertexReader.setNormalComponentInfo(type, stride, bufferSize, buffer);
	}

	@Override
	public void setWeightPointer(int size, int type, int stride, int bufferSize, Buffer buffer) {
		if (bufferVertexReader == null) {
			bufferVertexReader = new BufferVertexReader();
		}
		bufferVertexReader.setWeightComponentInfo(size, type, stride, bufferSize, buffer);
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
				int bufferWidth = context.texture_buffer_width[level];
				IMemoryReader imageReader = ImageReader.getImageReader(context.texture_base_pointer[level], width, height, bufferWidth, internalFormat, false, 0, 0, 0, 0, 0, 0, null, null);
				CachedTexture cachedTexture = CachedTexture.getCachedTexture(Math.min(width, bufferWidth), height, internalFormat, imageReader);
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
	public boolean canNativeClut(int textureAddress, boolean textureSwizzle) {
		if (Memory.isVRAM(textureAddress) && !textureSwizzle) {
			return true;
		}
		return !useTextureCache;
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
	public boolean setCopyRedToAlpha(boolean copyRedToAlpha) {
		return true;
	}

	@Override
	public void setVertexColor(float[] color) {
		for (int i = 0; i < context.vertexColor.length; i++) {
			context.vertexColor[i] = color[i];
		}
	}
}
