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
import static jpcsp.graphics.GeCommands.TFLT_NEAREST;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.graphics.GeCommands.TWRAP_WRAP_MODE_CLAMP;
import static jpcsp.graphics.RE.IRenderingEngine.RE_DEPTH_COMPONENT;
import static jpcsp.graphics.RE.IRenderingEngine.sizeOfTextureType;
import static jpcsp.graphics.VideoEngine.SIZEOF_FLOAT;
import static jpcsp.util.Utilities.makePow2;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.apache.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GLX;
import org.lwjgl.opengl.WGL;
import org.lwjgl.system.Platform;
import org.lwjgl.system.linux.X11;
import org.lwjgl.system.linux.XVisualInfo;
import org.lwjgl.system.windows.User32;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.buffer.IREBufferManager;
import jpcsp.graphics.capture.CaptureManager;
import jpcsp.graphics.textures.GETexture;
import jpcsp.graphics.textures.GETextureManager;
import jpcsp.memory.IMemoryReaderWriter;
import jpcsp.memory.MemoryReaderWriter;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class VideoEngineUtilities {
	public static Logger log = VideoEngine.log;
    public static final int internalTextureFormat = TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
    // For HiDPI monitors
    private static boolean glfwInit;
    private static float monitorContentScaleX = 1f;
    private static float monitorContentScaleY = 1f;
    // Resizing
    private static float viewportResizeScaleFactor = 1f;
    private static int viewportResizeScaleFactorInt = 1;
    private static boolean viewportResizeScaleFactorChanged;
    //
    private static int drawBuffer;
    // Statistics
    private static DurationStatistics statisticsCopyGeToMemory;
    private static DurationStatistics statisticsCopyMemoryToGe;
    // Stencil copy
    private static final int[] stencilPixelMasks = new int[]{0, 0x7FFF, 0x0FFF, 0x00FFFFFF};
    private static final int[] stencilValueMasks = new int[]{0, 0x80, 0xF0, 0xFF};
    private static final int[] stencilValueShifts = new int[]{0, 8, 8, 24};
    // GE texture
    private static final Object geTextureLock = new Object();
    private static int geTextureId;
    private static int geTextureBufferWidth;
    private static int geTextureWidth;
    private static int geTextureHeight;
    private static int geTexturePixelFormat;

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

    public static void stop() {
    	drawBuffer = 0;
    	geTextureId = 0;
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

	public static void setViewportResizeScaleFactor(float value) {
		if (value != viewportResizeScaleFactor) {
			viewportResizeScaleFactor = value;
			viewportResizeScaleFactorInt = Math.round((float) Math.ceil(value));
			viewportResizeScaleFactorChanged = true;
		}
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

    	if (viewportResizeScaleFactorChanged) {
    		viewportResizeScaleFactorChanged = false;
    		sceDisplayModule.forceSetViewportResizeScaleFactor(scaleFactor);
    	}
    }

    public static void setDisplayMinimumSize() {
		Emulator.getMainGUI().setDisplayMinimumSize(getDisplayScreen().getWidth(), getDisplayScreen().getHeight());
    }

    public static void drawTexture(IRenderingEngine re, int drawBufferId, int pixelFormat, int x, int y, int width, int height, int viewportWidth, int viewportHeight, float textureLowerRightS, float textureLowerRightT, float textureLowerLeftS, float textureLowerLeftT, float textureUpperLeftS, float textureUpperLeftT, float textureUpperRightS, float textureUpperRightT, boolean invert, boolean redWriteEnabled, boolean greenWriteEnabled, boolean blueWriteEnabled, boolean alphaWriteEnabled) {
		re.startDirectRendering(true, false, true, true, !invert, width, height);
		re.setColorMask(redWriteEnabled, greenWriteEnabled, blueWriteEnabled, alphaWriteEnabled);
		re.setViewport(0, 0, viewportWidth, viewportHeight);
        re.setTextureFormat(pixelFormat, false);

        IREBufferManager bufferManager = re.getBufferManager();
        ByteBuffer drawByteBuffer = bufferManager.getBuffer(drawBufferId);
        if (drawByteBuffer == null) {
        	return;
        }
        drawByteBuffer.clear();
        FloatBuffer drawFloatBuffer = drawByteBuffer.asFloatBuffer();
        drawFloatBuffer.clear();
        drawFloatBuffer.put(textureLowerRightS);
        drawFloatBuffer.put(textureLowerRightT);
        drawFloatBuffer.put(x + width);
        drawFloatBuffer.put(y + height);

        drawFloatBuffer.put(textureLowerLeftS);
        drawFloatBuffer.put(textureLowerLeftT);
        drawFloatBuffer.put(x);
        drawFloatBuffer.put(y + height);

        drawFloatBuffer.put(textureUpperLeftS);
        drawFloatBuffer.put(textureUpperLeftT);
        drawFloatBuffer.put(x);
        drawFloatBuffer.put(y);

        drawFloatBuffer.put(textureUpperRightS);
        drawFloatBuffer.put(textureUpperRightT);
        drawFloatBuffer.put(x + width);
        drawFloatBuffer.put(y);

        if (re.isVertexArrayAvailable()) {
        	re.bindVertexArray(0);
        }
        re.setVertexInfo(null, false, false, true, false, IRenderingEngine.RE_QUADS);
        re.enableClientState(IRenderingEngine.RE_TEXTURE);
        re.disableClientState(IRenderingEngine.RE_COLOR);
        re.disableClientState(IRenderingEngine.RE_NORMAL);
        re.enableClientState(IRenderingEngine.RE_VERTEX);
        bufferManager.setTexCoordPointer(re, drawBufferId, 2, IRenderingEngine.RE_FLOAT, 4 * SIZEOF_FLOAT, 0);
        bufferManager.setVertexPointer(re, drawBufferId, 2, IRenderingEngine.RE_FLOAT, 4 * SIZEOF_FLOAT, 2 * SIZEOF_FLOAT);
        bufferManager.setBufferData(re, IRenderingEngine.RE_ARRAY_BUFFER, drawBufferId, drawFloatBuffer.position() * SIZEOF_FLOAT, drawByteBuffer.rewind(), IRenderingEngine.RE_DYNAMIC_DRAW);
        re.drawArrays(IRenderingEngine.RE_QUADS, 0, 4);

        re.endDirectRendering();
    }

    public static void drawFrameBuffer(IRenderingEngine re) {
        drawFrameBuffer(re, true, true, sceDisplayModule.getBufferWidthFb(), sceDisplayModule.getPixelFormatFb(), sceDisplayModule.getWidthFb(), sceDisplayModule.getHeightFb());
    }

    public static void drawFrameBuffer(IRenderingEngine re, boolean keepOriginalSize, boolean invert, int bufferWidth, int pixelFormat, int width, int height) {
    	DisplayScreen displayScreen = getDisplayScreen();

    	if (log.isDebugEnabled()) {
        	log.debug(String.format("drawFrameBuffer keepOriginalSize=%b, invert=%b, bufferWidth=%d, pixelFormat=%d, width=%d, height=%d, %s", keepOriginalSize, invert, bufferWidth, pixelFormat, width, height, displayScreen));
        }

        float scale = 1f;
        int viewportWidth = width;
        int viewportHeight = height;
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
        } else {
    		updateMonitorContentScale();
        	updateDisplaySize();
            viewportWidth = Math.round(getResizedWidth(width) * monitorContentScaleX);
            viewportHeight = Math.round(getResizedHeight(height) * monitorContentScaleY);
        }

        if (drawBuffer == 0) {
            drawBuffer = re.getBufferManager().genBuffer(re, IRenderingEngine.RE_ARRAY_BUFFER, IRenderingEngine.RE_FLOAT, 16, IRenderingEngine.RE_DYNAMIC_DRAW);
        }

        drawTexture(re,
        		drawBuffer,
        		pixelFormat,
        		0, 0,
        		width, height,
        		viewportWidth, viewportHeight,
        		displayScreen.getTextureLowerRightS() * scale, displayScreen.getTextureLowerRightT() * scale,
        		displayScreen.getTextureLowerLeftS() * scale, displayScreen.getTextureLowerLeftT() * scale,
        		displayScreen.getTextureUpperLeftS() * scale, displayScreen.getTextureUpperLeftT() * scale,
        		displayScreen.getTextureUpperRightS() * scale, displayScreen.getTextureUpperRightT() * scale,
        		invert, true, true, true, true
        		);
    }

    public static void drawFrameBufferFromMemory(IRenderingEngine re, FrameBufferSettings fb, int texFb) {
    	drawFromMemory(re, texFb, fb.getBufferWidth(), fb.getPixelFormat(), fb.getHeight(), getDisplayScreen().getWidth(fb), getDisplayScreen().getHeight(fb), fb.getPixels());
    }

    public static void drawFromMemory(IRenderingEngine re, int textureId, int bufferWidth, int pixelFormat, int height, int renderWidth, int renderHeight, Buffer buffer) {
        buffer.clear();
        re.bindTexture(textureId);
        re.setTextureFormat(pixelFormat, false);
        re.setPixelStore(bufferWidth, getPixelFormatBytes(pixelFormat));
        int textureSize = bufferWidth * height * getPixelFormatBytes(pixelFormat);
        re.setTexSubImage(0, 0, 0, bufferWidth, height, pixelFormat, pixelFormat, textureSize, buffer);

        drawFrameBuffer(re, false, true, bufferWidth, pixelFormat, renderWidth, renderHeight);
    }

    public static void copyGeToMemory(IRenderingEngine re, boolean preserveScreen, boolean forceCopyToMemory) {
    	copyGeToMemory(re, sceDisplayModule.getTopAddrGe(), preserveScreen, forceCopyToMemory);
    }

    public static void copyGeToMemory(IRenderingEngine re, int geTopAddress, boolean preserveScreen, boolean forceCopyToMemory) {
        if (sceDisplayModule.isUsingSoftwareRenderer()) {
            // GE is already in memory when using the software renderer
            return;
        }

    	final int bufferWidth = sceDisplayModule.getBufferWidthGe();
    	final int width = sceDisplayModule.getWidthGe();
    	final int height = sceDisplayModule.getHeightGe();
    	final int pixelFormat = sceDisplayModule.getPixelFormatGe();

    	if (log.isDebugEnabled()) {
            log.debug(String.format("Copy GE Screen to Memory 0x%08X-0x%08X", geTopAddress, geTopAddress + sceDisplayModule.getSizeGe()));
        }

        if (statisticsCopyGeToMemory != null) {
            statisticsCopyGeToMemory.start();
        }

        if (sceDisplayModule.getSaveGEToTexture() && !VideoEngine.getInstance().isVideoTexture(geTopAddress)) {
            GETexture geTexture = GETextureManager.getInstance().getGETexture(re, geTopAddress, bufferWidth, width, height, pixelFormat, true);
            geTexture.copyScreenToTexture(re);
        } else {
        	forceCopyToMemory = true;
        }

        if (forceCopyToMemory) {
        	int resizedBufferWidth = getResizedWidthPow2(bufferWidth);
        	int resizedWidth = getResizedWidth(width);
        	int resizedHeight = getResizedHeight(height);
            if (resizedBufferWidth != geTextureBufferWidth || resizedWidth != geTextureWidth || resizedHeight != geTextureHeight || pixelFormat != geTexturePixelFormat) {
                // Lock the geTexture to avoid that it is being used at the same time by another thread
            	synchronized (geTextureLock) {
	            	if (geTextureId != 0) {
	            		re.deleteTexture(geTextureId);
	            	}
	            	geTextureId = re.genTexture();
	            	geTexturePixelFormat = pixelFormat;
	            	geTextureBufferWidth = resizedBufferWidth;
	            	geTextureWidth = resizedWidth;
	            	geTextureHeight = resizedHeight;

	            	re.bindTexture(geTextureId);
	            	re.setTextureFormat(geTexturePixelFormat, false);

	    	        //
	    	        // The format of the frame (or GE) buffer is
	    	        //   A the alpha & stencil value
	    	        //   R the Red color component
	    	        //   G the Green color component
	    	        //   B the Blue color component
	    	        //
	    	        // GU_PSM_8888 : 0xAABBGGRR
	    	        // GU_PSM_4444 : 0xABGR
	    	        // GU_PSM_5551 : ABBBBBGGGGGRRRRR
	    	        // GU_PSM_5650 : BBBBBGGGGGGRRRRR
	    	        //
	            	re.setTexImage(0,
	    	                internalTextureFormat,
	    	                resizedBufferWidth,
	    	                getResizedHeightPow2(makePow2(height)),
	    	                getTexturePixelFormat(geTexturePixelFormat),
	    	                getTexturePixelFormat(geTexturePixelFormat),
	    	                0, null);
	            	re.setTextureMipmapMinFilter(GeCommands.TFLT_NEAREST);
	            	re.setTextureMipmapMagFilter(GeCommands.TFLT_NEAREST);
	            	re.setTextureMipmapMinLevel(0);
	            	re.setTextureMipmapMaxLevel(0);
	            	re.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);
            	}
            }

            // Lock the geTexture to avoid that it is recreated at the same time by another thread
        	synchronized (geTextureLock) {
	            // Set resizedTexFb as the current texture
	            re.bindTexture(geTextureId);
	            re.setTextureFormat(pixelFormat, false);

	            // Copy screen to the current texture
	            re.copyTexSubImage(0, 0, 0, 0, 0, Math.min(resizedBufferWidth, resizedWidth), resizedHeight);

	            // Re-render GE/current texture upside down
	            drawFrameBuffer(re);

	            copyGeScreenToPixels(re, sceDisplayModule.getPixelsGe(geTopAddress), bufferWidth, pixelFormat, width, height);

	            if (sceDisplayModule.isSaveStencilToMemory()) {
	                copyStencilToMemory(re);
	            }
        	}

            if (preserveScreen) {
                // Lock the geTexture to avoid that it is recreated at the same time by another thread
            	synchronized (geTextureLock) {
            		// Redraw the screen
            		re.bindTexture(geTextureId);
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

    public static void copyGeScreenToPixels(IRenderingEngine re, Buffer pixels, int bufferWidth, int pixelFormat, int width, int height) {
        Buffer temp = sceDisplayModule.getTempBuffer();
        Buffer buffer = (pixels.capacity() >= temp.capacity() ? pixels : temp);
        buffer.clear();

        // Lock the geTexture to avoid that it is recreated at the same time by another thread
        synchronized (geTextureLock) {
	        // Set texFb as the current texture
	        re.bindTexture(geTextureId);
	        re.setTextureFormat(pixelFormat, false);

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
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("maxSpriteHeight=%d, maxSpriteWidth=%d", maxHeight, maxWidth));
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
        if (log.isDebugEnabled()) {
            log.debug(String.format("Reloading GE Memory (0x%08X-0x%08X) to screen (%dx%d)", sceDisplayModule.getTopAddrGe(), sceDisplayModule.getBottomAddrGe(), sceDisplayModule.getWidthGe(), sceDisplayModule.getHeightGe()));
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

            // Lock the geTexture to avoid that it is recreated at the same time by another thread
            synchronized (geTextureLock) {
            	drawFromMemory(re, geTextureId, Math.min(sceDisplayModule.getBufferWidthGe(), geTextureBufferWidth), sceDisplayModule.getPixelFormatGe(), Math.min(sceDisplayModule.getHeightGe(), geTextureHeight), Math.min(sceDisplayModule.getWidthGe(), geTextureWidth), sceDisplayModule.getHeightGe(), sceDisplayModule.getPixelsGe());
            }
        }

        if (statisticsCopyMemoryToGe != null) {
            statisticsCopyMemoryToGe.end();
        }
    }

    public static boolean canShareContext() {
    	switch (Platform.get()) {
	    	case WINDOWS:
	    	case LINUX:
	    		return true;
			default:
		    	return false;
		}
    }

    public static long createContext() {
    	long newContext;
		long currentContext;
		long displayWindow = sceDisplayModule.getCanvas().getDisplayWindow();
		switch (Platform.get()) {
			case WINDOWS:
				long dc = User32.GetDC(displayWindow);
				newContext = WGL.wglCreateContext(dc);
				currentContext = WGL.wglGetCurrentContext();
				if (!WGL.wglShareLists(currentContext, newContext)) {
					log.error(String.format("VideoEngineUtilities.createContext: Cannot share context 0x%X with 0x%X", currentContext, newContext));
				}
				User32.ReleaseDC(displayWindow, dc);
				break;
			case LINUX:
				int screen = X11.XDefaultScreen(displayWindow);
				if (log.isDebugEnabled()) {
					log.debug(String.format("XDefaultScreen displayWindow=0x%X, screen=%d", displayWindow, screen));
				}
				XVisualInfo visualInfo = GLX.glXChooseVisual(displayWindow, screen, new int[] { GLX.GLX_RGBA, GLX.GLX_DOUBLEBUFFER, 0 });
				currentContext = GLX.glXGetCurrentContext();
				if (log.isDebugEnabled()) {
					log.debug(String.format("glxCurrentContext=0x%X, XVisualInfo=%s", currentContext, visualInfo));
				}
				newContext = GLX.glXCreateContext(displayWindow, visualInfo, currentContext, true);
				if (log.isDebugEnabled()) {
					log.debug(String.format("videoEngineContext=0x%X", newContext));
				}
				break;
			default:
				log.error(String.format("VideoEngineUtilities.createContext: Unsupported platform %s", Platform.get().getName()));
				newContext = 0L;
				break;
		}

		return newContext;
    }

    public static void setContext(long context) {
		long displayWindow = sceDisplayModule.getCanvas().getDisplayWindow();
		switch (Platform.get()) {
			case WINDOWS:
				long dc = User32.GetDC(displayWindow);
				if (!WGL.wglMakeCurrent(dc, context)) {
					log.error(String.format("VideoEngineUtilities.setContext: Cannot make context 0x%X current", context));
				}
				User32.ReleaseDC(displayWindow, dc);
				break;
			case LINUX:
				long drawable = sceDisplayModule.getCanvas().getDisplayDrawable();
				if (!GLX.glXMakeCurrent(displayWindow, drawable, context)) {
					log.error(String.format("VideoEngineUtilities.setContext: Cannot make context 0x%X current", context));
				}
				break;
			default:
				log.error(String.format("VideoEngineUtilities.setContext: Unsupported platform %s", Platform.get().getName()));
				break;
		}
    }

    public static int createTexture(IRenderingEngine re, FrameBufferSettings fb, int textureId, boolean isResized) {
        if (textureId != -1) {
        	re.deleteTexture(textureId);
        }
        textureId = re.genTexture();

        re.bindTexture(textureId);
        re.setTextureFormat(fb.getPixelFormat(), false);

        //
        // The format of the frame (or GE) buffer is
        //   A the alpha & stencil value
        //   R the Red color component
        //   G the Green color component
        //   B the Blue color component
        //
        // GU_PSM_8888 : 0xAABBGGRR
        // GU_PSM_4444 : 0xABGR
        // GU_PSM_5551 : ABBBBBGGGGGRRRRR
        // GU_PSM_5650 : BBBBBGGGGGGRRRRR
        //
        re.setTexImage(0,
                internalTextureFormat,
                isResized ? getResizedWidthPow2(fb.getBufferWidth()) : fb.getBufferWidth(),
                isResized ? getResizedHeightPow2(Utilities.makePow2(fb.getHeight())) : Utilities.makePow2(fb.getHeight()),
                getTexturePixelFormat(fb.getPixelFormat()),
                getTexturePixelFormat(fb.getPixelFormat()),
                0, null);
        re.setTextureMipmapMinFilter(GeCommands.TFLT_NEAREST);
        re.setTextureMipmapMagFilter(GeCommands.TFLT_NEAREST);
        re.setTextureMipmapMinLevel(0);
        re.setTextureMipmapMaxLevel(0);
        re.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);

        return textureId;
    }

    /**
     * Resize the given value according to the viewport resizing factor,
     * assuming it is a value along the X-Axis (e.g. "x" or "width" value).
     *
     * @param width value on the X-Axis to be resized
     * @return the resized value
     */
    public static int getResizedWidth(int width) {
        return Math.round(width * getViewportResizeScaleFactor());
    }

    /**
     * Resize the given value according to the viewport resizing factor,
     * assuming it is a value along the X-Axis being a power of 2 (i.e. 2^n).
     *
     * @param wantedWidth value on the X-Axis to be resized, must be a power of
     * 2.
     * @return the resized value, as a power of 2.
     */
    public static int getResizedWidthPow2(int widthPow2) {
        return widthPow2 * getViewportResizeScaleFactorInt();
    }

    /**
     * Resize the given value according to the viewport resizing factor,
     * assuming it is a value along the Y-Axis (e.g. "y" or "height" value).
     *
     * @param height value on the Y-Axis to be resized
     * @return the resized value
     */
    public static int getResizedHeight(int height) {
        return Math.round(height * getViewportResizeScaleFactor());
    }

    /**
     * Resize the given value according to the viewport resizing factor,
     * assuming it is a value along the Y-Axis being a power of 2 (i.e. 2^n).
     *
     * @param wantedWidth value on the Y-Axis to be resized, must be a power of
     * 2.
     * @return the resized value, as a power of 2.
     */
    public static int getResizedHeightPow2(int heightPow2) {
        return heightPow2 * getViewportResizeScaleFactorInt();
    }

    public static int getTexturePixelFormat(int pixelFormat) {
		// Always use a 32-bit texture to store the GE.
		// 16-bit textures are causing color artifacts.
    	return GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
    }

    public static void dumpDepthBufferImage(IRenderingEngine re) {
    	GeContext context = VideoEngine.getInstance().getContext();
    	int zbp = context.zbp;
    	int zbw = context.zbw;
    	int height = sceDisplayModule.getHeightGe();
    	int pixelFormat = RE_DEPTH_COMPONENT;
        int depthBufferSize = zbw * height * sizeOfTextureType[pixelFormat];

        Buffer buffer;
        if (sceDisplayModule.isUsingSoftwareRenderer()) {
    		buffer = Memory.getInstance().getBuffer(zbp, depthBufferSize);
    	} else {
    		buffer = sceDisplayModule.getTempByteBuffer().clear();
            re.setTextureMipmapMinFilter(TFLT_NEAREST);
            re.setTextureMipmapMagFilter(TFLT_NEAREST);
            re.setTextureMipmapMinLevel(0);
            re.setTextureMipmapMaxLevel(0);
            re.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);
    		re.setPixelStore(zbw, getPixelFormatBytes(pixelFormat));
    		re.readDepth(0, 0, zbw, height, depthBufferSize, buffer);
    		buffer.rewind();
    	}

        CaptureManager.dumpImage(zbp, 0, buffer, zbw, height, zbw, pixelFormat, false, 0, true, false);
    }
}
