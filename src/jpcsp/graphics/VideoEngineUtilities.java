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
package jpcsp.graphics;

import static jpcsp.HLE.Modules.sceDisplayModule;
import static jpcsp.HLE.modules.sceDisplay.getResizedHeight;
import static jpcsp.HLE.modules.sceDisplay.getResizedHeightPow2;
import static jpcsp.HLE.modules.sceDisplay.getResizedWidth;
import static jpcsp.graphics.VideoEngine.SIZEOF_FLOAT;
import static jpcsp.util.Utilities.makePow2;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.apache.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import jpcsp.Emulator;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.buffer.IREBufferManager;
import jpcsp.graphics.textures.GETexture;
import jpcsp.graphics.textures.GETextureManager;
import jpcsp.memory.IMemoryReaderWriter;
import jpcsp.memory.MemoryReaderWriter;
import jpcsp.util.DurationStatistics;

/**
 * @author gid15
 *
 */
public class VideoEngineUtilities {
	public static Logger log = VideoEngine.log;
    // For HiDPI monitors
    private static boolean glfwInit;
    private static float monitorContentScaleX = 1f;
    private static float monitorContentScaleY = 1f;
    // Resizing
    private static float viewportResizeScaleFactor = 1f;
    private static int viewportResizeScaleFactorInt = 1;
    //
    private static int drawBuffer;
    private static final float[] drawBufferArray = new float[16];
    // Statistics
    private static DurationStatistics statisticsCopyGeToMemory;
    private static DurationStatistics statisticsCopyMemoryToGe;
    // Stencil copy
    private static final int[] stencilPixelMasks = new int[]{0, 0x7FFF, 0x0FFF, 0x00FFFFFF};
    private static final int[] stencilValueMasks = new int[]{0, 0x80, 0xF0, 0xFF};
    private static final int[] stencilValueShifts = new int[]{0, 8, 8, 24};

    public static void start() {
        statisticsCopyGeToMemory = new DurationStatistics("Copy GE to Memory");
        statisticsCopyMemoryToGe = new DurationStatistics("Copy Memory to GE");
    }

    public static void exit() {
        if (DurationStatistics.collectStatistics) {
            log.info(statisticsCopyGeToMemory.toString());
            log.info(statisticsCopyMemoryToGe.toString());
        }
    }

    public static int getPixelFormatBytes(int pixelformat) {
        return IRenderingEngine.sizeOfTextureType[pixelformat];
    }

    public static void updateMonitorContentScale() {
		String javaVersion = System.getProperty("java.version");
		if (javaVersion.startsWith("1.8.") || javaVersion.startsWith("1.7.")) {
			// Java is HiDPI aware only starting with Java 9
			return;
		}

    	if (!glfwInit) {
    		if (GLFW.glfwInit()) {
    			glfwInit = true;
    		}
    	}

		float[] x = new float[1];
		float[] y = new float[1];
		// Default values in case the call is failing
		x[0] = 1f;
		y[0] = 1f;

		GLFW.glfwGetMonitorContentScale(GLFW.glfwGetPrimaryMonitor(), x, y);

		monitorContentScaleX = x[0];
		monitorContentScaleY = y[0];

		if (log.isTraceEnabled()) {
			log.trace(String.format("monitorContentScaleX=%f, monitorContentScaleY=%f", monitorContentScaleX, monitorContentScaleY));
		}
    }

	public static float getMonitorContentScaleX() {
		return monitorContentScaleX;
	}

	public static float getMonitorContentScaleY() {
		return monitorContentScaleY;
	}

	public static float getViewportResizeScaleFactor() {
		return viewportResizeScaleFactor;
	}

	public static void setViewportResizeScaleFactor(float viewportResizeScaleFactor) {
		VideoEngineUtilities.viewportResizeScaleFactor = viewportResizeScaleFactor;
		VideoEngineUtilities.viewportResizeScaleFactorInt = Math.round((float) Math.ceil(viewportResizeScaleFactor));
	}

	public static int getViewportResizeScaleFactorInt() {
		return viewportResizeScaleFactorInt;
	}

	public static DisplayScreen getDisplayScreen() {
		return sceDisplayModule.getDisplayScreen();
	}

	public static void updateDisplaySize() {
    	float scaleFactor = viewportResizeScaleFactor;
    	setDisplayMinimumSize();
    	Emulator.getMainGUI().setDisplaySize(getResizedWidth(getDisplayScreen().getWidth()), getResizedHeight(getDisplayScreen().getHeight()));
		sceDisplayModule.forceSetViewportResizeScaleFactor(scaleFactor);
    }

    public static void setDisplayMinimumSize() {
		Emulator.getMainGUI().setDisplayMinimumSize(getDisplayScreen().getWidth(), getDisplayScreen().getHeight());
    }

    public static void drawFrameBuffer(IRenderingEngine re) {
        drawFrameBuffer(re, true, true, sceDisplayModule.getBufferWidthFb(), sceDisplayModule.getPixelFormatFb(), sceDisplayModule.getWidthFb(), sceDisplayModule.getHeightFb());
    }

    public static void drawFrameBuffer(IRenderingEngine re, boolean keepOriginalSize, boolean invert, int bufferwidth, int pixelformat, int width, int height) {
    	DisplayScreen displayScreen = getDisplayScreen();

    	if (log.isDebugEnabled()) {
        	log.debug(String.format("drawFrameBuffer keepOriginalSize=%b, invert=%b, bufferWidth=%d, pixelFormat=%d, width=%d, height=%d, %s", keepOriginalSize, invert, bufferwidth, pixelformat, width, height, displayScreen));
        }

    	re.startDirectRendering(true, false, true, true, !invert, width, height);
        if (keepOriginalSize) {
            re.setViewport(0, 0, width, height);
        } else {
    		updateMonitorContentScale();
        	updateDisplaySize();
            re.setViewport(0, 0, Math.round(getResizedWidth(width) * monitorContentScaleX), Math.round(getResizedHeight(height) * monitorContentScaleY));
        }

        re.setTextureFormat(pixelformat, false);

        float scale = 1f;
        if (keepOriginalSize) {
        	// When keeping the original size, we still have to adjust the size of the texture mapping.
        	// E.g. when the screen has been resized to 576x326 (resizeScaleFactor=1.2),
        	// the texture has been created with a size 1024x1024 and the following texture
        	// coordinates have to used:
        	//     (576/1024, 326/1024),
        	// while texS==480/512 and texT==272/512
        	scale = (float) getResizedHeight(height) / (float) getResizedHeightPow2(makePow2(height));
        	float texT = sceDisplayModule.getTexT();
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("drawFrameBuffer scale = %f / %f = %f", scale, texT, scale / texT));
        	}
        	scale /= texT;
        }

        int i = 0;
        drawBufferArray[i++] = displayScreen.getTextureLowerRightS() * scale;
        drawBufferArray[i++] = displayScreen.getTextureLowerRightT() * scale;
        drawBufferArray[i++] = (float) width;
        drawBufferArray[i++] = (float) height;

        drawBufferArray[i++] = displayScreen.getTextureLowerLeftS() * scale;
        drawBufferArray[i++] = displayScreen.getTextureLowerLeftT() * scale;
        drawBufferArray[i++] = 0f;
        drawBufferArray[i++] = (float) height;

        drawBufferArray[i++] = displayScreen.getTextureUpperLeftS() * scale;
        drawBufferArray[i++] = displayScreen.getTextureUpperLeftT() * scale;
        drawBufferArray[i++] = 0f;
        drawBufferArray[i++] = 0f;

        drawBufferArray[i++] = displayScreen.getTextureUpperRightS() * scale;
        drawBufferArray[i++] = displayScreen.getTextureUpperRightT() * scale;
        drawBufferArray[i++] = (float) width;
        drawBufferArray[i++] = 0f;

        if (drawBuffer == 0) {
            drawBuffer = re.getBufferManager().genBuffer(re, IRenderingEngine.RE_ARRAY_BUFFER, IRenderingEngine.RE_FLOAT, 16, IRenderingEngine.RE_DYNAMIC_DRAW);
        }

        int bufferSizeInFloats = i;
        IREBufferManager bufferManager = re.getBufferManager();
        ByteBuffer byteBuffer = bufferManager.getBuffer(drawBuffer);
        byteBuffer.clear();
        byteBuffer.asFloatBuffer().put(drawBufferArray, 0, bufferSizeInFloats);

        if (re.isVertexArrayAvailable()) {
            re.bindVertexArray(0);
        }
        re.setVertexInfo(null, false, false, true, IRenderingEngine.RE_QUADS);
        re.enableClientState(IRenderingEngine.RE_TEXTURE);
        re.disableClientState(IRenderingEngine.RE_COLOR);
        re.disableClientState(IRenderingEngine.RE_NORMAL);
        re.enableClientState(IRenderingEngine.RE_VERTEX);
        bufferManager.setTexCoordPointer(re, drawBuffer, 2, IRenderingEngine.RE_FLOAT, 4 * SIZEOF_FLOAT, 0);
        bufferManager.setVertexPointer(re, drawBuffer, 2, IRenderingEngine.RE_FLOAT, 4 * SIZEOF_FLOAT, 2 * SIZEOF_FLOAT);
        bufferManager.setBufferData(re, IRenderingEngine.RE_ARRAY_BUFFER, drawBuffer, bufferSizeInFloats * SIZEOF_FLOAT, byteBuffer, IRenderingEngine.RE_DYNAMIC_DRAW);
        re.drawArrays(IRenderingEngine.RE_QUADS, 0, 4);

        re.endDirectRendering();
    }

    public static void drawFrameBufferFromMemory(IRenderingEngine re, FrameBufferSettings fb, int texFb) {
        fb.getPixels().clear();
        re.bindTexture(texFb);
        re.setTextureFormat(fb.getPixelFormat(), false);
        re.setPixelStore(fb.getBufferWidth(), getPixelFormatBytes(fb.getPixelFormat()));
        int textureSize = fb.getBufferWidth() * fb.getHeight() * getPixelFormatBytes(fb.getPixelFormat());
        re.setTexSubImage(0,
                0, 0, fb.getBufferWidth(), fb.getHeight(),
                fb.getPixelFormat(),
                fb.getPixelFormat(),
                textureSize, fb.getPixels());

        drawFrameBuffer(re, false, true, fb.getBufferWidth(), fb.getPixelFormat(), getDisplayScreen().getWidth(fb), getDisplayScreen().getHeight(fb));
    }

    public static void copyGeToMemory(IRenderingEngine re, boolean preserveScreen, boolean forceCopyToMemory) {
    	copyGeToMemory(re, sceDisplayModule.getTopAddrGe(), preserveScreen, forceCopyToMemory);
    }

    public static void copyGeToMemory(IRenderingEngine re, int geTopAddress, boolean preserveScreen, boolean forceCopyToMemory) {
        if (sceDisplayModule.isUsingSoftwareRenderer()) {
            // GE is already in memory when using the software renderer
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Copy GE Screen to Memory 0x%08X-0x%08X", geTopAddress, geTopAddress + sceDisplayModule.getSizeGe()));
        }

        if (statisticsCopyGeToMemory != null) {
            statisticsCopyGeToMemory.start();
        }

        if (sceDisplayModule.getSaveGEToTexture() && !VideoEngine.getInstance().isVideoTexture(geTopAddress)) {
            GETexture geTexture = GETextureManager.getInstance().getGETexture(re, geTopAddress, sceDisplayModule.getBufferWidthGe(), sceDisplayModule.getWidthGe(), sceDisplayModule.getHeightGe(), sceDisplayModule.getPixelFormatGe(), true);
            geTexture.copyScreenToTexture(re);
        } else {
        	forceCopyToMemory = true;
        }

        if (forceCopyToMemory) {
            // Lock the resizedTexFb to avoid that it is recreated at the same time by the GUI thread
        	synchronized (sceDisplayModule.resizedTexFbLock) {
	            // Set resizedTexFb as the current texture
	            re.bindTexture(sceDisplayModule.getResizedTexFb());
	            re.setTextureFormat(sceDisplayModule.getPixelFormatGe(), false);

	            // Copy screen to the current texture
	            re.copyTexSubImage(0, 0, 0, 0, 0, getResizedWidth(Math.min(sceDisplayModule.getBufferWidthGe(), sceDisplayModule.getWidthGe())), getResizedHeight(sceDisplayModule.getHeightGe()));

	            // Re-render GE/current texture upside down
	            drawFrameBuffer(re, true, true, sceDisplayModule.getBufferWidthGe(), sceDisplayModule.getPixelFormatGe(), sceDisplayModule.getWidthGe(), sceDisplayModule.getHeightGe());

	            copyScreenToPixels(re, sceDisplayModule.getPixelsGe(geTopAddress), sceDisplayModule.getBufferWidthGe(), sceDisplayModule.getPixelFormatGe(), sceDisplayModule.getWidthGe(), sceDisplayModule.getHeightGe());

	            if (sceDisplayModule.isSaveStencilToMemory()) {
	                copyStencilToMemory(re);
	            }
        	}

            if (preserveScreen) {
                // Lock the resizedTexFb to avoid that it is recreated at the same time by the GUI thread
            	synchronized (sceDisplayModule.resizedTexFbLock) {
            		// Redraw the screen
            		re.bindTexture(sceDisplayModule.getResizedTexFb());
            		drawFrameBuffer(re, false, false, sceDisplayModule.getBufferWidthGe(), sceDisplayModule.getPixelFormatGe(), sceDisplayModule.getWidthGe(), sceDisplayModule.getHeightGe());
            	}
            }
        }

        if (statisticsCopyGeToMemory != null) {
            statisticsCopyGeToMemory.end();
        }

        if (GEProfiler.isProfilerEnabled()) {
            GEProfiler.copyGeToMemory();
        }
    }

    private static void copyStencilToMemory(IRenderingEngine re) {
        if (sceDisplayModule.getPixelFormatGe() >= stencilPixelMasks.length) {
            log.warn(String.format("copyGeToMemory: unimplemented pixelformat %d for Stencil buffer copy", sceDisplayModule.getPixelFormatGe()));
            return;
        }
        if (stencilValueMasks[sceDisplayModule.getPixelFormatGe()] == 0) {
            // No stencil value for BGR5650, nothing to copy for the stencil
            return;
        }

        // Be careful to not overwrite parts of the GE memory used by the application for another purpose.
        VideoEngine videoEngine = VideoEngine.getInstance();
        int stencilWidth = Math.min(sceDisplayModule.getWidthGe(), sceDisplayModule.getBufferWidthGe());
        int stencilHeight = Math.min(sceDisplayModule.getHeightGe(), videoEngine.getMaxSpriteHeight());
        if (log.isDebugEnabled()) {
            log.debug(String.format("Copy stencil to GE: pixelFormat=%d, %dx%d, maxSprite=%dx%d", sceDisplayModule.getPixelFormatGe(), stencilWidth, stencilHeight, videoEngine.getMaxSpriteWidth(), videoEngine.getMaxSpriteHeight()));
        }

        int stencilBufferSize = stencilWidth * stencilHeight;
        ByteBuffer tempByteBuffer = sceDisplayModule.getTempByteBuffer();
        tempByteBuffer.clear();
        re.setPixelStore(stencilWidth, 1);
        re.readStencil(0, 0, stencilWidth, stencilHeight, stencilBufferSize, tempByteBuffer);

        int bytesPerPixel = IRenderingEngine.sizeOfTextureType[sceDisplayModule.getPixelFormatGe()];
        IMemoryReaderWriter memoryReaderWriter = MemoryReaderWriter.getMemoryReaderWriter(sceDisplayModule.getTopAddrGe(), stencilHeight * sceDisplayModule.getBufferWidthGe() * bytesPerPixel, bytesPerPixel);
        tempByteBuffer.rewind();
        final int stencilPixelMask = stencilPixelMasks[sceDisplayModule.getPixelFormatGe()];
        final int stencilValueMask = stencilValueMasks[sceDisplayModule.getPixelFormatGe()];
        final int stencilValueShift = stencilValueShifts[sceDisplayModule.getPixelFormatGe()];
        for (int y = 0; y < stencilHeight; y++) {
            // The stencil buffer is stored upside-down by OpenGL
            tempByteBuffer.position((stencilHeight - y - 1) * stencilWidth);

            for (int x = 0; x < stencilWidth; x++) {
                int pixel = memoryReaderWriter.readCurrent();
                int stencilValue = tempByteBuffer.get() & stencilValueMask;
                pixel = (pixel & stencilPixelMask) | (stencilValue << stencilValueShift);
                memoryReaderWriter.writeNext(pixel);
            }

            if (stencilWidth < sceDisplayModule.getBufferWidthGe()) {
                memoryReaderWriter.skip(sceDisplayModule.getBufferWidthGe() - stencilWidth);
            }
        }
        memoryReaderWriter.flush();

        if (GEProfiler.isProfilerEnabled()) {
            GEProfiler.copyStencilToMemory();
        }
    }

    public static void copyScreenToPixels(IRenderingEngine re, Buffer pixels, int bufferWidth, int pixelFormat, int width, int height) {
        Buffer temp = sceDisplayModule.getTempBuffer();
        Buffer buffer = (pixels.capacity() >= temp.capacity() ? pixels : temp);
        buffer.clear();

        // Lock the texFb to avoid that it is recreated at the same time by the GUI thread
        synchronized (sceDisplayModule.texFbLock) {
	        // Set texFb as the current texture
	        re.bindTexture(sceDisplayModule.getTexFb());
	        re.setTextureFormat(sceDisplayModule.getPixelFormatFb(), false);

	        re.setPixelStore(bufferWidth, getPixelFormatBytes(pixelFormat));

	        // Copy screen to the current texture
	        re.copyTexSubImage(0, 0, 0, 0, 0, Math.min(bufferWidth, width), height);

	        // Copy the current texture into memory
	        re.getTexImage(0, pixelFormat, pixelFormat, buffer);
    	}

        // Copy temp into pixels, temp is probably square and pixels is less,
        // a smaller rectangle, otherwise we could copy straight into pixels.
        if (buffer == temp) {
            temp.clear();
            pixels.clear();
            temp.limit(pixels.limit());

            if (temp instanceof ByteBuffer) {
                ByteBuffer srcBuffer = (ByteBuffer) temp;
                ByteBuffer dstBuffer = (ByteBuffer) pixels;
                dstBuffer.put(srcBuffer);
            } else if (temp instanceof IntBuffer) {
                IntBuffer srcBuffer = (IntBuffer) temp;
                IntBuffer dstBuffer = (IntBuffer) pixels;

                VideoEngine videoEngine = VideoEngine.getInstance();
                if (videoEngine.isUsingTRXKICK() && videoEngine.getMaxSpriteHeight() < Integer.MAX_VALUE) {
                    // Hack: God of War is using GE command lists stored into the non-visible
                    // part of the GE buffer. The lists are copied from the main memory into
                    // the VRAM using TRXKICK. Be careful to not overwrite these non-visible
                    // parts.
                    //
                    // Copy only the visible part of the GE to the memory, e.g.
                    // when width==480 and bufferwidth==1024, copy only 480 pixels
                    // per line and skip 1024-480 pixels.
                    int srcBufferWidth = bufferWidth;
                    int dstBufferWidth = bufferWidth;
                    int pixelsPerElement = 4 / getPixelFormatBytes(pixelFormat);
                    int maxHeight = videoEngine.getMaxSpriteHeight();
                    int maxWidth = videoEngine.getMaxSpriteWidth();
                    int textureAlignment = (pixelsPerElement == 1 ? 3 : 7);
                    maxHeight = (maxHeight + textureAlignment) & ~textureAlignment;
                    maxWidth = (maxWidth + textureAlignment) & ~textureAlignment;
                    if (VideoEngine.log.isDebugEnabled()) {
                        VideoEngine.log.debug("maxSpriteHeight=" + maxHeight + ", maxSpriteWidth=" + maxWidth);
                    }
                    if (maxHeight > height) {
                        maxHeight = height;
                    }
                    if (maxWidth > width) {
                        maxWidth = width;
                    }
                    copyBufferByLines(dstBuffer, srcBuffer, dstBufferWidth, srcBufferWidth, pixelFormat, maxWidth, maxHeight);
                } else {
                    dstBuffer.put(srcBuffer);
                }
            } else {
                throw new RuntimeException("unhandled buffer type");
            }
        }
        // We only use "temp" buffer in this function, its limit() will get restored on the next call to clear()
    }

    private static void copyBufferByLines(IntBuffer dstBuffer, IntBuffer srcBuffer, int dstBufferWidth, int srcBufferWidth, int pixelFormat, int width, int height) {
        int pixelsPerElement = 4 / getPixelFormatBytes(pixelFormat);
        for (int y = 0; y < height; y++) {
            int srcStartOffset = y * srcBufferWidth / pixelsPerElement;
            int dstStartOffset = y * dstBufferWidth / pixelsPerElement;
            srcBuffer.limit(srcStartOffset + (width + 1) / pixelsPerElement);
            srcBuffer.position(srcStartOffset);
            dstBuffer.position(dstStartOffset);
            if (srcBuffer.remaining() < dstBuffer.remaining()) {
                dstBuffer.put(srcBuffer);
            }
        }
    }

    public static void loadGEToScreen(IRenderingEngine re) {
        if (VideoEngine.log.isDebugEnabled()) {
            VideoEngine.log.debug(String.format("Reloading GE Memory (0x%08X-0x%08X) to screen (%dx%d)", sceDisplayModule.getTopAddrGe(), sceDisplayModule.getBottomAddrGe(), sceDisplayModule.getWidthGe(), sceDisplayModule.getHeightGe()));
        }

        if (statisticsCopyMemoryToGe != null) {
            statisticsCopyMemoryToGe.start();
        }

        if (sceDisplayModule.getSaveGEToTexture() && !VideoEngine.getInstance().isVideoTexture(sceDisplayModule.getTopAddrGe())) {
            GETexture geTexture = GETextureManager.getInstance().getGETexture(re, sceDisplayModule.getTopAddrGe(), sceDisplayModule.getBufferWidthGe(), sceDisplayModule.getWidthGe(), sceDisplayModule.getHeightGe(), sceDisplayModule.getPixelFormatGe(), true);
            geTexture.copyTextureToScreen(re);
        } else {
            if (re.isVertexArrayAvailable()) {
                re.bindVertexArray(0);
            }

            // Lock the texFb to avoid that it is recreated at the same time by the GUI thread
            synchronized (sceDisplayModule.texFbLock) {
	            // Set texFb as the current texture
	            re.bindTexture(sceDisplayModule.getTexFb());

	            // Define the texture from the GE Memory
	            re.setPixelStore(sceDisplayModule.getBufferWidthGe(), getPixelFormatBytes(sceDisplayModule.getPixelFormatGe()));
	            int textureSize = sceDisplayModule.getBufferWidthGe() * sceDisplayModule.getHeightGe() * getPixelFormatBytes(sceDisplayModule.getPixelFormatGe());
	            sceDisplayModule.getPixelsGe().clear();
	            re.setTexSubImage(0, 0, 0, sceDisplayModule.getBufferWidthGe(), sceDisplayModule.getHeightGe(), sceDisplayModule.getPixelFormatGe(), sceDisplayModule.getPixelFormatGe(), textureSize, sceDisplayModule.getPixelsGe());

	            // Draw the GE
	            drawFrameBuffer(re, false, true, sceDisplayModule.getBufferWidthGe(), sceDisplayModule.getPixelFormatGe(), sceDisplayModule.getWidthGe(), sceDisplayModule.getHeightGe());
            }
        }

        if (statisticsCopyMemoryToGe != null) {
            statisticsCopyMemoryToGe.end();
        }
    }
}
