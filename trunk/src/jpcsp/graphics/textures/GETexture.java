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
package jpcsp.graphics.textures;

import static jpcsp.graphics.GeCommands.TFLT_NEAREST;
import static jpcsp.graphics.GeCommands.TWRAP_WRAP_MODE_CLAMP;
import static jpcsp.graphics.VideoEngine.SIZEOF_FLOAT;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.buffer.IREBufferManager;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class GETexture {
	protected int address;
	protected int length;
	protected int bufferWidth;
	protected int width;
	protected int height;
	protected int heightPow2;
	protected int pixelFormat;
	protected int bytesPerPixel;
	protected int textureId = -1;
	protected int drawBufferId = -1;
	protected float texS;
	protected float texT;
	private boolean changed;
	protected int bufferLength;
	protected Buffer buffer;
	protected boolean useViewportResize;
	protected float resizeScale;

	public GETexture(int address, int bufferWidth, int width, int height, int pixelFormat, boolean useViewportResize) {
		this.address = address;
		this.bufferWidth = bufferWidth;
		this.width = width;
		this.height = height;
		this.pixelFormat = pixelFormat;
		bytesPerPixel = sceDisplay.getPixelFormatBytes(pixelFormat);
		length = bufferWidth * height * bytesPerPixel;
		heightPow2 = Utilities.makePow2(height);
		this.useViewportResize = useViewportResize;
		changed = true;
		resizeScale = getViewportResizeScaleFactor();
		bufferLength = getTexImageWidth() * getTexImageHeight() * bytesPerPixel;
	}

	private float getViewportResizeScaleFactor() {
		if (!useViewportResize) {
			return 1;
		}

		return Modules.sceDisplayModule.getViewportResizeScaleFactor();
	}

	public void bind(IRenderingEngine re, boolean forDrawing) {
		float viewportResizeScaleFactor = getViewportResizeScaleFactor();
		// Create the texture if not yet created or
		// re-create it if the viewport resize factor has been changed dynamically.
		if (textureId == -1 || viewportResizeScaleFactor != resizeScale) {
			resizeScale = viewportResizeScaleFactor;

			if (useViewportResize) {
				texS = sceDisplay.getResizedWidth(width) / (float) getTexImageWidth();
				texT = sceDisplay.getResizedHeight(height) / (float) getTexImageHeight();
			} else {
				texS = width / (float) bufferWidth;
				texT = height / (float) heightPow2;
			}

			if (textureId != -1) {
				re.deleteTexture(textureId);
			}
			textureId = re.genTexture();
			re.bindTexture(textureId);
    		re.setTexImage(0, pixelFormat, getTexImageWidth(), getTexImageHeight(), pixelFormat, pixelFormat, 0, null);
            re.setTextureMipmapMinFilter(TFLT_NEAREST);
            re.setTextureMipmapMagFilter(TFLT_NEAREST);
            re.setTextureMipmapMinLevel(0);
            re.setTextureMipmapMaxLevel(0);
            re.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);
            if (drawBufferId == -1) {
            	drawBufferId = re.getBufferManager().genBuffer(IRenderingEngine.RE_FLOAT, 16, IRenderingEngine.RE_DYNAMIC_DRAW);
            }
		} else {
			re.bindTexture(textureId);
		}

		if (forDrawing) {
			re.setTextureFormat(pixelFormat, false);
		}
	}

	public int getBufferWidth() {
		return bufferWidth;
	}

	public int getTexImageWidth() {
		return useViewportResize ? sceDisplay.getResizedWidthPow2(bufferWidth) : bufferWidth;
	}

	public int getTexImageHeight() {
		return useViewportResize ? sceDisplay.getResizedHeightPow2(heightPow2) : heightPow2;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getResizedWidth() {
		return useViewportResize ? sceDisplay.getResizedWidth(width) : width;
	}

	public int getResizedHeight() {
		return useViewportResize ? sceDisplay.getResizedHeight(height) : height;
	}

	public int getPixelFormat() {
		return pixelFormat;
	}

	public void copyScreenToTexture(IRenderingEngine re) {
		if (VideoEngine.log.isDebugEnabled()) {
			VideoEngine.log.debug(String.format("GETexture.copyScreenToTexture %s", toString()));
		}

		bind(re, false);

		int texWidth = Math.min(bufferWidth, width);
		int texHeight = height;
		if (useViewportResize) {
			texWidth = sceDisplay.getResizedWidth(texWidth);
			texHeight = sceDisplay.getResizedHeight(texHeight);
		}
		re.copyTexSubImage(0, 0, 0, 0, 0, texWidth, texHeight);

		setChanged(true);
	}

	public void copyTextureToScreen(IRenderingEngine re) {
		copyTextureToScreen(re, 0, 0, width, height, true, true, true, true, true);
	}

	public void copyTextureToScreen(IRenderingEngine re, int x, int y, int projectionWidth, int projectionHeight, boolean scaleToCanvas, boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled) {
		if (VideoEngine.log.isDebugEnabled()) {
			VideoEngine.log.debug(String.format("GETexture.copyTextureToScreen %s at %dx%d", toString(), x, y));
		}

		bind(re, true);

		re.startDirectRendering(true, false, true, true, true, projectionWidth, projectionHeight);
		re.setColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, alphaWriteEnabled);
		if (scaleToCanvas) {
			re.setViewport(0, 0, Modules.sceDisplayModule.getCanvasWidth(), Modules.sceDisplayModule.getCanvasHeight());
		} else {
			re.setViewport(0, 0, projectionWidth, projectionHeight);
		}

        IREBufferManager bufferManager = re.getBufferManager();
        ByteBuffer drawByteBuffer = bufferManager.getBuffer(drawBufferId);
        drawByteBuffer.clear();
        FloatBuffer drawFloatBuffer = drawByteBuffer.asFloatBuffer();
        drawFloatBuffer.clear();
        drawFloatBuffer.put(texS);
        drawFloatBuffer.put(texT);
        drawFloatBuffer.put(x + width);
        drawFloatBuffer.put(y + height);

        drawFloatBuffer.put(0.f);
        drawFloatBuffer.put(texT);
        drawFloatBuffer.put(x);
        drawFloatBuffer.put(y + height);

        drawFloatBuffer.put(0.f);
        drawFloatBuffer.put(0.f);
        drawFloatBuffer.put(x);
        drawFloatBuffer.put(y);

        drawFloatBuffer.put(texS);
        drawFloatBuffer.put(0.f);
        drawFloatBuffer.put(x + width);
        drawFloatBuffer.put(y);

        if (re.isVertexArrayAvailable()) {
        	re.bindVertexArray(0);
        }
        re.setVertexInfo(null, false, false, true, -1);
        re.enableClientState(IRenderingEngine.RE_TEXTURE);
        re.disableClientState(IRenderingEngine.RE_COLOR);
        re.disableClientState(IRenderingEngine.RE_NORMAL);
        re.enableClientState(IRenderingEngine.RE_VERTEX);
        bufferManager.setTexCoordPointer(drawBufferId, 2, IRenderingEngine.RE_FLOAT, 4 * SIZEOF_FLOAT, 0);
        bufferManager.setVertexPointer(drawBufferId, 2, IRenderingEngine.RE_FLOAT, 4 * SIZEOF_FLOAT, 2 * SIZEOF_FLOAT);
        bufferManager.setBufferData(drawBufferId, drawFloatBuffer.position() * SIZEOF_FLOAT, drawByteBuffer.rewind(), IRenderingEngine.RE_DYNAMIC_DRAW);
        re.drawArrays(IRenderingEngine.RE_QUADS, 0, 4);

        re.endDirectRendering();
	}

	protected void setChanged(boolean changed) {
		this.changed = changed;
	}

	protected boolean hasChanged() {
		return changed;
	}

	private void prepareBuffer() {
		if (buffer == null) {
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bufferLength).order(ByteOrder.LITTLE_ENDIAN);
			if (Memory.getInstance().getMainMemoryByteBuffer() instanceof IntBuffer) {
				buffer = byteBuffer.asIntBuffer();
			} else {
				buffer = byteBuffer;
			}
		} else {
			buffer.clear();
		}
	}

	public void copyTextureToMemory(IRenderingEngine re) {
		if (textureId == -1) {
			// Texture not yet created... nothing to copy
			return;
		}

		if (!hasChanged()) {
			// Texture unchanged... don't copy again
			return;
		}

		if (VideoEngine.log.isDebugEnabled()) {
			VideoEngine.log.debug(String.format("GETexture.copyTextureToMemory %s", toString()));
		}

		Buffer memoryBuffer = Memory.getInstance().getBuffer(address, length);
    	prepareBuffer();
        re.bindTexture(textureId);
		re.setTextureFormat(pixelFormat, false);
        re.setPixelStore(bufferWidth, sceDisplay.getPixelFormatBytes(pixelFormat));
        re.getTexImage(0, pixelFormat, pixelFormat, buffer);

    	buffer.clear();
    	if (buffer instanceof IntBuffer) {
    		IntBuffer src = (IntBuffer) buffer;
    		IntBuffer dst = (IntBuffer) memoryBuffer;
    		int pixelsPerElement = 4 / bytesPerPixel;
    		int copyWidth = Math.min(width, bufferWidth);
    		int widthLimit = (copyWidth + pixelsPerElement - 1) / pixelsPerElement;
    		int step = bufferWidth / pixelsPerElement;
    		int srcOffset = 0;
    		int dstOffset = (height - 1) * step;
    		// We have received the texture data upside-down, invert it
    		for (int y = 0; y < height; y++, srcOffset += step, dstOffset -= step) {
    			src.limit(srcOffset + widthLimit);
    			src.position(srcOffset);
    			dst.position(dstOffset);
    			dst.put(src);
    		}
    	} else {
    		ByteBuffer src = (ByteBuffer) buffer;
    		ByteBuffer dst = (ByteBuffer) memoryBuffer;
    		int copyWidth = Math.min(width, bufferWidth);
    		int widthLimit = copyWidth * bytesPerPixel;
    		int step = bufferWidth * bytesPerPixel;
    		int srcOffset = 0;
    		int dstOffset = (height - 1) * step;
    		// We have received the texture data upside-down, invert it
    		for (int y = 0; y < height; y++, srcOffset += step, dstOffset -= step) {
    			src.limit(srcOffset + widthLimit);
    			src.position(srcOffset);
    			dst.position(dstOffset);
    			dst.put(src);
    		}
    	}

    	setChanged(false);
	}

	public void delete(IRenderingEngine re) {
		if (drawBufferId != -1) {
			re.getBufferManager().deleteBuffer(drawBufferId);
			drawBufferId = -1;
		}
		if (textureId != -1) {
			re.deleteTexture(textureId);
			textureId = -1;
		}
	}

	public int getTextureId() {
		return textureId;
	}

	public boolean isCompatible(int width, int height, int bufferWidth, int pixelFormat) {
		if (width != this.width || height != this.height || bufferWidth != this.bufferWidth || pixelFormat != this.pixelFormat) {
			return false;
		}

		if (useViewportResize) {
			if (resizeScale != getViewportResizeScaleFactor()) {
				return false;
			}
		}

		return true;
	}

	@Override
	public String toString() {
		return String.format("GETexture[0x%08X-0x%08X, %dx%d, bufferWidth=%d, pixelFormat=%d(%s)]", address, address + length, width, height, bufferWidth, pixelFormat, VideoEngine.getPsmName(pixelFormat));
	}
}
