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
package jpcsp.HLE;

import java.awt.image.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.media.opengl.DebugGL;
import java.nio.IntBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.io.*;
import javax.imageio.*;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Settings;
import jpcsp.State;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.capture.CaptureManager;
import jpcsp.scheduler.UnblockThreadAction;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

import com.sun.opengl.util.Screenshot;

/**
 * @author shadow, aisesal
 */

/*
 * TODO list:
 * 1. Use texture rectangles (NPOT gives problems with ATI drivers) when using
 * "disableGE".
 *
 * 2. Check sceDisplayGetCurrentHcount() on PSP.
 */

public final class pspdisplay extends GLCanvas implements GLEventListener {
	private static final long serialVersionUID = 2267866365228834812L;
	private static pspdisplay instance;
    public static pspdisplay getInstance() {
        if (instance == null) {

            // We need to ask for stencil buffer
            GLCapabilities capabilities = new GLCapabilities();
            capabilities.setStencilBits(8);
            capabilities.setAlphaBits(8);

            instance = new pspdisplay(capabilities);
        }
        return instance;
    }

    private static final boolean useGlReadPixels = true;
    private boolean onlyGEGraphics = false;
    private static final boolean useDebugGL = false;
    private static final int internalTextureFormat = GL.GL_RGBA;

    // PspDisplayPixelFormats enum
    public static final int PSP_DISPLAY_PIXEL_FORMAT_565  = 0;
    public static final int PSP_DISPLAY_PIXEL_FORMAT_5551 = 1;
    public static final int PSP_DISPLAY_PIXEL_FORMAT_4444 = 2;
    public static final int PSP_DISPLAY_PIXEL_FORMAT_8888 = 3;

    // PspDisplaySetBufSync enum
    public static final int PSP_DISPLAY_SETBUF_IMMEDIATE = 0;
    public static final int PSP_DISPLAY_SETBUF_NEXTFRAME = 1;

    public boolean disableGE;

    // current display mode
    private int mode;
    private int width;
    private int height;
    private int widthGe;
    private int heightGe;

    // current framebuffer settings
    private int topaddrFb;
    private int bufferwidthFb;
    private int pixelformatFb;
    private int sync;
    private boolean setGeBufCalledAtLeastOnce;
    public boolean gotBadGeBufParams;
    public boolean gotBadFbBufParams;

    // additional variables
    private int bottomaddrFb;

    private int topaddrGe;
    private int bottomaddrGe;
    private int bufferwidthGe;
    private int pixelformatGe;

    private boolean detailsDirty;
    private boolean displayDirty;
    private boolean geDirty;
    private long lastUpdate;

    // Canvas fields
    private Buffer pixelsFb;
    private Buffer pixelsGe;
    private Buffer temp;
    private int canvasWidth;
    private int canvasHeight;
    private boolean createTex;
    private int texFb;
    private float texS;
    private float texT;

    public boolean getscreen = false;

    //Rotation vars
    private float texS1, texS2, texS3, texS4;
    private float texT1, texT2, texT3, texT4;
    private int ang = 4;
    public boolean isrotating = false;

    // fps counter variables
    private long prevStatsTime;
    private long frameCount;
    private long prevFrameCount;
    private long reportCount;
    private double averageFPS = 0.0;

    private int vcount;
    private long lastVblankMicroTime;
    private DisplayVblankAction displayVblankAction;

    // Texture state
    private float[] texRgbScale = new float[2];
    private int[] texMode = new int[2];
    private int[] texSrc0Alpha = new int[2];
    private int texStackIndex = 0;

    public DurationStatistics statistics;
    public DurationStatistics statisticsCopyGeToMemory;
    public DurationStatistics statisticsCopyMemoryToGe;

    // Async Display
    private AsyncDisplayThread asyncDisplayThread;
    private Semaphore displayLock;
    private boolean tryLockDisplay;
    private long tryLockTimestamp;

    private boolean isFbShowing = false;

    private pspdisplay (GLCapabilities capabilities) {
    	super (capabilities);

        setSize(480, 272);
        addGLEventListener(this);
        texFb = -1;
    }

    public void Initialise() {
        statistics = new DurationStatistics("pspdisplay Statistics");
        statisticsCopyGeToMemory = new DurationStatistics("Copy GE to Memory");
        statisticsCopyMemoryToGe = new DurationStatistics("Copy Memory to GE");

        mode          = 0;
        width         = 480;
        height        = 272;
        topaddrFb     = MemoryMap.START_VRAM;
        bufferwidthFb = 512;
        pixelformatFb = PSP_DISPLAY_PIXEL_FORMAT_8888;
        sync          = PSP_DISPLAY_SETBUF_IMMEDIATE;

        bottomaddrFb = topaddrFb + bufferwidthFb * height * getPixelFormatBytes(pixelformatFb);

        detailsDirty = true;
        displayDirty = true;
        geDirty = false;
        createTex = true;

        disableGE =
            Settings.getInstance().readBool("emu.disablege");

        pixelsFb = getPixels(topaddrFb, bottomaddrFb);

        widthGe       = 480;
        heightGe      = 272;
        topaddrGe     = topaddrFb;
        bufferwidthGe = bufferwidthFb;
        pixelformatGe = pixelformatFb;
        bottomaddrGe  = bottomaddrFb;
        pixelsGe = getPixels(topaddrGe, bottomaddrGe);

        setGeBufCalledAtLeastOnce = false;
        gotBadGeBufParams = false;
        gotBadFbBufParams = false;

        prevStatsTime = 0;
        frameCount = 0;
        prevFrameCount = 0;
        reportCount = 0;
        averageFPS = 0.0;

        vcount = 0;

        displayLock = new Semaphore(1);
    	if (asyncDisplayThread == null) {
    		asyncDisplayThread = new AsyncDisplayThread(this);
    		asyncDisplayThread.setDaemon(true);
    		asyncDisplayThread.setName("Async Display Thread");
    		asyncDisplayThread.start();
    	}

    	if (displayVblankAction == null) {
    		displayVblankAction = new DisplayVblankAction();
    		IntrManager.getInstance().addVBlankAction(displayVblankAction);
    	}
    }

    public void exit() {
        if (statistics != null) {
            Modules.log.info("----------------------------- pspdisplay exit -----------------------------");
            Modules.log.info(statistics.toString());
            Modules.log.info(statisticsCopyGeToMemory.toString());
            Modules.log.info(statisticsCopyMemoryToGe.toString());
        }
    }

    public void step(boolean immediately) {
        long now = System.currentTimeMillis();
        if (immediately || now - lastUpdate > 1000 / 60 || geDirty) {
        	if (!onlyGEGraphics || VideoEngine.getInstance().hasDrawLists()) {
	            if (geDirty || detailsDirty || displayDirty) {
                    detailsDirty = false;
                    displayDirty = false;
                    geDirty = false;

            		asyncDisplayThread.display();
	            }
        	}
            lastUpdate = now;
        }
    }

    public void step() {
    	step(false);
    }

    public void write8(int address, int data) {
        address &= Memory.addressMask;
        // vram address is lower than main memory so check the end of the buffer first, it's more likely to fail
        if (address < bottomaddrFb && address >= topaddrFb)
            displayDirty = true;
    }

    public void write16(int address, int data) {
        address &= Memory.addressMask;
        if (address < bottomaddrFb && address >= topaddrFb)
            displayDirty = true;
    }

    public void write32(int address, int data) {
        address &= Memory.addressMask;
        if (address < bottomaddrFb && address >= topaddrFb)
            displayDirty = true;
    }

    public void setGeDirty(boolean dirty) {
        geDirty = dirty;
    }

    public void hleDisplaySetGeMode(int width, int height) {
        if (width <= 0 || height <= 0) {
            Modules.log.warn("hleDisplaySetGeMode(" + width + "," + height + ") bad params");
        } else {
            Modules.log.debug("hleDisplaySetGeMode(width=" + width + ",height=" + height + ")");
            widthGe = width;
            heightGe = height;
            bottomaddrGe =
                topaddrGe + bufferwidthGe * heightGe *
                getPixelFormatBytes(pixelformatGe);
            pixelsGe = getPixels(topaddrGe, bottomaddrGe);
        }
    }

    public void hleDisplaySetGeBuf(GL gl, int topaddr, int bufferwidth, int pixelformat, boolean copyGEToMemory) {
    	hleDisplaySetGeBuf(gl, topaddr, bufferwidth, pixelformat, copyGEToMemory, widthGe, heightGe);
    }

    public void hleDisplaySetGeBuf(GL gl,
        int topaddr, int bufferwidth, int pixelformat, boolean copyGEToMemory,
        int width, int height)
    {
        topaddr &= Memory.addressMask;
        // We can get the address relative to 0 or already relative to START_VRAM
        if (topaddr < MemoryMap.START_VRAM) {
        	topaddr += MemoryMap.START_VRAM;
        }

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug(String.format("hleDisplaySetGeBuf topaddr=0x%08X, bufferwidth=%d, pixelformat=%d, copyGE=%b, with=%d, height=%d", topaddr, bufferwidth, pixelformat, copyGEToMemory, width, height));
        }

        if (topaddr == topaddrGe && bufferwidth == bufferwidthGe &&
            pixelformat == pixelformatGe &&
            width == widthGe && height == heightGe) {
        	// Nothing changed
        	return;
        }

        // The lower 2 bits of the bufferwidth are ignored.
        // E.g., the following bufferwidth values are valid: 120, 240, 480, 256, 512...
        bufferwidth = bufferwidth & ~0x3;

        if (topaddr < MemoryMap.START_VRAM || topaddr >= MemoryMap.END_VRAM ||
            bufferwidth <= 0 ||
            pixelformat < 0 || pixelformat > 3 ||
            sync < 0 || sync > 1)
        {
            String msg = "hleDisplaySetGeBuf bad params ("
                + Integer.toHexString(topaddr)
                + "," + bufferwidth
                + "," + pixelformat + ")";

            // First time is usually initializing GE, so we can ignore it
            if (setGeBufCalledAtLeastOnce)
            {
                Modules.log.warn(msg);
                gotBadGeBufParams = true;
            }
            else
            {
                Modules.log.debug(msg);
                setGeBufCalledAtLeastOnce = true;
            }

            return;
        } else {
            if (gotBadGeBufParams) {
                // print when we get good params after bad params
                gotBadGeBufParams = false;
                Modules.log.info("hleDisplaySetGeBuf ok ("
                    + Integer.toHexString(topaddr)
                    + "," + bufferwidth
                    + "," + pixelformat + ")");
            }

            boolean loadGEToScreen = true; // Always reload the GE memory to the screen
            if (copyGEToMemory && this.topaddrGe != topaddr) {
            	if (VideoEngine.log.isDebugEnabled()) {
            		VideoEngine.log.debug(String.format("Copy GE Screen to Memory 0x%08X-0x%08X", topaddrGe, bottomaddrGe));
            	}

            	if (statisticsCopyGeToMemory != null) {
            		statisticsCopyGeToMemory.start();
            	}

            	VideoEngine.getInstance().disableShaders();

            	// Set texFb as the current texture
                gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);

                // Copy screen to the current texture
                gl.glCopyTexSubImage2D(
                    GL.GL_TEXTURE_2D, 0,
                    0, 0, 0, 0, widthGe, heightGe);

                // Re-render GE/current texture upside down
                drawFrameBuffer(gl, true, true, bufferwidthGe, pixelformatGe, widthGe, heightGe);

                copyScreenToPixels(gl, pixelsGe, bufferwidthGe, pixelformatGe, widthGe, heightGe);
                loadGEToScreen = true;

            	VideoEngine.getInstance().enableShaders();

            	if (statisticsCopyGeToMemory != null) {
            		statisticsCopyGeToMemory.end();
            	}
            }

            this.topaddrGe     = topaddr;
            this.bufferwidthGe = bufferwidth;
            this.pixelformatGe = pixelformat;
            this.widthGe       = width;
            this.heightGe      = height;

            bottomaddrGe =
                topaddr + bufferwidthGe * heightGe *
                getPixelFormatBytes(pixelformatGe);

            // This is kind of interesting, we don't actually know the height
            // of the buffer, we're only guessing it matches the display height,
            // even then developers might be sneaky.
            // The safe option is to set it to go from topaddrGe to MemoryMap.END_VRAM
            // everytime, but we could be wasting time copying unnecessary pixels around.
            // For now use height but clamp it to the valid area (fiveofhearts)
            // update: I think we can get it from XSCALE/YSCALE GE command/sceGuViewport/hleDisplaySetGeMode
            if (bottomaddrGe > MemoryMap.END_VRAM + 1 && !Memory.getInstance().isIgnoreInvalidMemoryAccess() && !VideoEngine.getInstance().isUseViewport()) {
                // We can probably remove this message since it's allowed on
                // real PSP but it's interesting to see what games do it.
                Modules.log.warn("clamping ge buf top=" + Integer.toHexString(topaddrGe)
                    + " bottom=" + Integer.toHexString(bottomaddrGe)
                    + " w=" + bufferwidthGe
                    + " bpp=" + (getPixelFormatBytes(pixelformatGe) * 8));
            	heightGe = (MemoryMap.END_VRAM + 1 - topaddrGe) / (bufferwidthGe * getPixelFormatBytes(pixelformatGe));
                bottomaddrGe =
                	topaddrGe + bufferwidthGe * heightGe *
                    getPixelFormatBytes(pixelformatGe);
            }

            pixelsGe = getPixels(topaddrGe, bottomaddrGe);

            if (loadGEToScreen) {
            	if (VideoEngine.log.isDebugEnabled()) {
            		VideoEngine.log.debug(String.format("Reloading GE Memory (0x%08X-0x%08X) to screen (%dx%d)", topaddrGe, bottomaddrGe, widthGe, heightGe));
            	}

            	if (statisticsCopyMemoryToGe != null) {
            		statisticsCopyMemoryToGe.start();
            	}

            	VideoEngine.getInstance().disableShaders();

            	// Set texFb as the current texture
            	gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);

            	// Define the texture from the GE Memory
                gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, getPixelFormatBytes(pixelformatGe));
                gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, bufferwidthGe);
	            int pixelFormatGL = getPixelFormatGL(pixelformatGe);
				gl.glTexSubImage2D(
	                GL.GL_TEXTURE_2D, 0,
	                0, 0, bufferwidthGe, heightGe,
	                getFormatGL(pixelformatGe),
	                pixelFormatGL, pixelsGe);

				// Draw the GE
	            drawFrameBuffer(gl, false, true, bufferwidthGe, pixelformatGe, widthGe, heightGe);

            	VideoEngine.getInstance().enableShaders();

            	if (statisticsCopyMemoryToGe != null) {
            		statisticsCopyMemoryToGe.end();
            	}

            	if (State.captureGeNextFrame) {
	            	captureGeImage(gl);
	            }
            }
        }

        setGeBufCalledAtLeastOnce = true;
    }

    public static int getFormatGL(int pixelformat) {
    	return pixelformat == PSP_DISPLAY_PIXEL_FORMAT_565 ? GL.GL_RGB : GL.GL_RGBA;
    }

    public static int getPixelFormatBytes(int pixelformat) {
        return pixelformat == PSP_DISPLAY_PIXEL_FORMAT_8888 ? 4 : 2;
    }

    public static int getPixelFormatGL(int pixelformat) {
        switch (pixelformat) {
        case PSP_DISPLAY_PIXEL_FORMAT_565:
            return GL.GL_UNSIGNED_SHORT_5_6_5_REV;
        case PSP_DISPLAY_PIXEL_FORMAT_5551:
            return GL.GL_UNSIGNED_SHORT_1_5_5_5_REV;
        case PSP_DISPLAY_PIXEL_FORMAT_4444:
            return GL.GL_UNSIGNED_SHORT_4_4_4_4_REV;
        default:
            return GL.GL_UNSIGNED_BYTE;
        }
    }

    private Buffer getPixels(int topaddr, int bottomaddr) {
        Buffer pixels = Memory.getInstance().getBuffer(topaddr, bottomaddr - topaddr);
        return pixels;
    }

    private void reportFPSStats() {
        frameCount++;
        long timeNow = System.nanoTime();
        long realElapsedTime = (timeNow - prevStatsTime) / 1000000L;

        if (realElapsedTime > 1000L) {
            reportCount++;
            int lastFPS = (int)(frameCount - prevFrameCount);
            averageFPS = (double)frameCount / reportCount;
            prevFrameCount = frameCount;
            prevStatsTime = timeNow;

            Emulator.setFpsTitle(String.format("FPS: %d, averageFPS: %.1f", lastFPS, averageFPS));
        }
    }

    public void pushTexEnv(final GL gl) {
        gl.glGetTexEnvfv(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, texRgbScale, texStackIndex);
        gl.glGetTexEnviv(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, texMode, texStackIndex);
        gl.glGetTexEnviv(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, texSrc0Alpha, texStackIndex);
        texStackIndex++;
    }

    public void popTexEnv(final GL gl) {
        texStackIndex--;
        gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, texRgbScale[texStackIndex]);
        gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, texMode[texStackIndex]);
        gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, texSrc0Alpha[texStackIndex]);
    }

    /** @param first : true  = draw as psp size
     *                 false = draw as window size */
    private void drawFrameBuffer(final GL gl, boolean first, boolean invert, int bufferwidth, int pixelformat, int width, int height) {
        if(!isrotating){

            texS1 = texS4 = texS;
            texT1 = texT2 = texT;

            texS2 = texS3 = texT3 = texT4 = 0.0f;
        }

        gl.glPushAttrib(GL.GL_ALL_ATTRIB_BITS);

        if (first)
            gl.glViewport(0, 0, width, height);
        else
            gl.glViewport(0, 0, canvasWidth, canvasHeight);

        gl.glDisable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_BLEND);
        gl.glDisable(GL.GL_ALPHA_TEST);
        gl.glDisable(GL.GL_FOG);
        gl.glDisable(GL.GL_LIGHTING);
        gl.glDisable(GL.GL_LOGIC_OP);
        gl.glDisable(GL.GL_STENCIL_TEST);
        gl.glDisable(GL.GL_SCISSOR_TEST);
        gl.glColorMask(true, true, true, false);

        pushTexEnv(gl);
        gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, 1.0f);
        gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_REPLACE);

        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, getPixelFormatBytes(pixelformat));
        gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, bufferwidth);

    	gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        if (invert)
            gl.glOrtho(0.0, width, height, 0.0, -1.0, 1.0);
        else
            gl.glOrtho(0.0, width, 0.0, height, -1.0, 1.0);

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glFrontFace(GL.GL_CW);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);
        gl.glBegin(GL.GL_QUADS);

        gl.glColor3f(1.0f, 1.0f, 1.0f);

        gl.glTexCoord2f(texS1, texT1);
        gl.glVertex2f(width, height);

        gl.glTexCoord2f(texS2, texT2);
        gl.glVertex2f(0.0f, height);

        gl.glTexCoord2f(texS3, texT3);
        gl.glVertex2f(0.0f, 0.0f);

        gl.glTexCoord2f(texS4, texT4);
        gl.glVertex2f(width, 0.0f);

        gl.glEnd();

        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glPopAttrib();

        popTexEnv(gl);

        isrotating = false;

    }

    private void copyScreenToPixels(GL gl, Buffer pixels, int bufferwidth, int pixelformat, int width, int height) {
    	gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        // Using glReadPixels instead of glGetTexImage is showing
        // between 7 and 13% performance increase.
        // But glReadPixels seems only to work correctly with 32bit pixels...
        if (useGlReadPixels && pixelformat == PSP_DISPLAY_PIXEL_FORMAT_8888) {
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glOrtho(0.0, width, height, 0.0, -1.0, 1.0);
            int bufferStep = bufferwidth * getPixelFormatBytes(pixelformat);
            int pixelFormatGL = getPixelFormatGL(pixelformat);
            int widthToRead = Math.min(width, bufferwidth);
            int formatGL = getFormatGL(pixelformat);
            // Y-Axis on PSP is flipped against OpenGL, so we have to copy row by row
            for (int y = 0, bufferPos = 0; y < height; y++, bufferPos += bufferStep) {
            	Utilities.bytePositionBuffer(pixels, bufferPos); // this uses reflection -> slow(?)
                gl.glReadPixels(0, y, widthToRead, 1, formatGL, pixelFormatGL, pixels);
            }
        } else {
            // Set texFb as the current texture
            gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);

            gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, getPixelFormatBytes(pixelformat));
            gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH, bufferwidth);

            // Copy screen to the current texture
            gl.glCopyTexSubImage2D(
                GL.GL_TEXTURE_2D, 0,
                0, 0, 0, 0, width, height);

            // Copy the current texture into memory
            temp.clear();
            int pixelFormatGL = getPixelFormatGL(pixelformat);
            gl.glGetTexImage(
                GL.GL_TEXTURE_2D, 0, getFormatGL(pixelformat),
                pixelFormatGL, temp);

            // Copy temp into pixels, temp is probably square and pixels is less,
            // a smaller rectangle, otherwise we could copy straight into pixels.
            temp.clear();
            pixels.clear();
            temp.limit(pixels.limit());
            if (temp instanceof ByteBuffer) {
                ((ByteBuffer) pixels).put((ByteBuffer) temp);
            } else if (temp instanceof IntBuffer) {
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
            		IntBuffer srcBuffer = (IntBuffer) temp;
            		IntBuffer dstBuffer = (IntBuffer) pixels;
            		int pixelsPerElement = 4 / getPixelFormatBytes(pixelformat);
            		int maxHeight = VideoEngine.getInstance().getMaxSpriteHeight();
            		if (VideoEngine.log.isDebugEnabled()) {
            			VideoEngine.log.debug("maxSpriteHeight=" + maxHeight);
            		}
            		if (maxHeight > height) {
            			maxHeight = height;
            		}
            		for (int y = 0; y < maxHeight; y++) {
            			int startOffset = y * bufferwidth / pixelsPerElement;
            			srcBuffer.limit(startOffset + (width + 1) / pixelsPerElement);
            			srcBuffer.position(startOffset);
            			dstBuffer.position(startOffset);
            			dstBuffer.put(srcBuffer);
            		}
            	} else {
            		((IntBuffer) pixels).put((IntBuffer) temp);
            	}
            } else {
                throw new RuntimeException("unhandled buffer type");
            }
            // We only use "temp" buffer in this function, its limit() will get restored on the next call to clear()
        }

        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();
    }

    // GLEventListener methods

    @Override
    public void init(GLAutoDrawable drawable) {
    	// Use DebugGL to get exceptions when some OpenGL operation fails
    	// Not safe to use in release mode since people could have a few exceptions
    	// that remained silent when some operations fails but still output the
    	// intended result
    	if (useDebugGL) {
    		drawable.setGL(new DebugGL(drawable.getGL()));
    	}
        final GL gl = drawable.getGL();
        VideoEngine.getInstance().setGL(gl); // Initialize shaders on startup
        gl.setSwapInterval(1);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        if (statistics != null) {
            statistics.start();
        }

        final GL gl = drawable.getGL();

        if (createTex) {
            int[] textures = new int[1];
            if (texFb != -1) {
                textures[0] = texFb;
                gl.glDeleteTextures(1, textures, 0);
            }
            gl.glGenTextures(1, textures, 0);
            texFb = textures[0];
            //Modules.log.debug("texFb = " + texFb);

            gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);

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
            gl.glTexImage2D(
                GL.GL_TEXTURE_2D, 0,
                internalTextureFormat,
                bufferwidthFb, Utilities.makePow2(height), 0,
                getFormatGL(pixelformatFb),
                getPixelFormatGL(pixelformatFb), null);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
            gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);

            if (Memory.getInstance().getMainMemoryByteBuffer() instanceof IntBuffer) {
            	temp = IntBuffer.allocate(
            			bufferwidthFb * Utilities.makePow2(height) *
            			getPixelFormatBytes(pixelformatFb) / 4);
            } else {
            	temp = ByteBuffer.allocate(
            			bufferwidthFb * Utilities.makePow2(height) *
            			getPixelFormatBytes(pixelformatFb)).order(ByteOrder.LITTLE_ENDIAN);
            }

            createTex = false;
        }

        if (texFb == -1) {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            return;
        }

        if (disableGE) {
            pixelsFb.clear();
            gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);
            gl.glTexSubImage2D(
                GL.GL_TEXTURE_2D, 0,
                0, 0, bufferwidthFb, height,
                GL.GL_RGBA, getPixelFormatGL(pixelformatFb), pixelsFb);

            drawFrameBuffer(gl, false, true, bufferwidthFb, pixelformatFb, width, height);
        } else if (onlyGEGraphics) {
            VideoEngine.getInstance().update();
        } else {
            // Render GE
            gl.glViewport(0, 0, width, height);

            // If the GE is not at the same address as the FrameBuffer,
            // redisplay the GE so that the VideoEngine can update it
            if (bottomaddrGe != bottomaddrFb) {
	            pixelsGe.clear();
	            gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);

	            // An alternative to glTexSubImage2D would be to use glDrawPixels to
	            // render the frame buffer.
	            // But glDrawPixels is showing around 10% performance decrease
	            // against glTexSubImage2D.

	            // Use format without alpha channel if the source buffer doesn't have one
	            int pixelFormatGL = getPixelFormatGL(pixelformatGe);
				gl.glTexSubImage2D(
	                GL.GL_TEXTURE_2D, 0,
	                0, 0, bufferwidthGe, heightGe,
	                getFormatGL(pixelformatGe),
	                pixelFormatGL, pixelsGe);

	            drawFrameBuffer(gl, false, true, bufferwidthFb, pixelformatFb, width, height);
            }

            if (VideoEngine.getInstance().update()) {
                // Update VRAM only if GE actually drew something
                // Set texFb as the current texture
                gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);

                // Copy screen to the current texture
                gl.glCopyTexSubImage2D(
                    GL.GL_TEXTURE_2D, 0,
                    0, 0, 0, 0, widthGe, heightGe);

                // Re-render GE/current texture upside down
                drawFrameBuffer(gl, true, true, bufferwidthFb, pixelformatFb, width, height);

                // Save GE/current texture to vram
                copyScreenToPixels(gl, pixelsGe, bufferwidthGe, pixelformatGe, widthGe, heightGe);
            }

            // Render FB
            pixelsFb.clear();
            gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);
            int pixelFormatGL = getPixelFormatGL(pixelformatFb);
            gl.glTexSubImage2D(
                GL.GL_TEXTURE_2D, 0,
                0, 0, bufferwidthFb, height,
                getFormatGL(pixelformatFb),
                pixelFormatGL, pixelsFb);

            //Call the rotating function (if needed)
            if(ang != 4)
                rotate(ang);

            drawFrameBuffer(gl, false, true, bufferwidthFb, pixelformatFb, width, height);
        }

        reportFPSStats();

        if (statistics != null) {
            statistics.end();
        }

        if (getscreen) {
        	savescreen(this);
        }
    }

    @Override
    public void reshape(
        GLAutoDrawable drawable,
        int x, int y,
        int width, int height)
    {
        canvasWidth  = width;
        canvasHeight = height;
    }

    @Override
    public void displayChanged(
        GLAutoDrawable drawable,
        boolean modeChanged,
        boolean displayChanged)
    {
    }

    // HLE functions

    public void sceDisplaySetMode(int mode, int width, int height) {
        Modules.log.debug(
            "sceDisplaySetMode(mode=" + mode +
            ",width=" + width +
            ",height=" + height + ")");

        if (width <= 0 || height <= 0) {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        } else {
            this.mode   = mode;
            this.width  = width;
            this.height = height;

            bottomaddrFb =
                topaddrFb + bufferwidthFb * height *
                getPixelFormatBytes(pixelformatFb);
            pixelsFb = getPixels(topaddrFb, bottomaddrFb);

            detailsDirty = true;

            if (mode != 0)
                Modules.log.warn("UNIMPLEMENTED:sceDisplaySetMode mode=" + mode);

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceDisplayGetMode(int pmode, int pwidth, int pheight) {
        Memory memory = Memory.getInstance();
        if (!memory.isAddressGood(pmode  ) ||
            !memory.isAddressGood(pwidth ) ||
            !memory.isAddressGood(pheight))
        {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        } else {
            memory.write32(pmode  , mode  );
            memory.write32(pwidth , width );
            memory.write32(pheight, height);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceDisplaySetFrameBuf(int topaddr, int bufferwidth, int pixelformat, int sync) {
        topaddr &= Memory.addressMask;

        if (bufferwidth <= 0 || (bufferwidth & (bufferwidth - 1)) != 0 ||
            pixelformat < 0 || pixelformat > 3 ||
            sync < 0 || sync > 1) {
            Modules.log.warn(
                "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                ",bufferwidth=" + bufferwidth +
                ",pixelformat=" + pixelformat +
                ",sync=" + sync + ") bad params");
            isFbShowing = false;
            gotBadFbBufParams = true;
            Emulator.getProcessor().cpu.gpr[2] = -1;
        } else if (topaddr == 0) {
            // Got 0 as topaddr, but it's ok, it will be correctly set on the
            // next call (tested and checked).
            Modules.log.warn(
                "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                ",bufferwidth=" + bufferwidth +
                ",pixelformat=" + pixelformat +
                ",sync=" + sync + ") bad params (topaddr==0)");
            isFbShowing = false;  // This is also set to false if topaddr == 0.
            gotBadFbBufParams = true;
            Emulator.getProcessor().cpu.gpr[2] = 0;
        } else if (Memory.getInstance().isAddressGood(topaddr)){
            if(topaddr < MemoryMap.START_VRAM || topaddr >= MemoryMap.END_VRAM) {
                Modules.log.warn("sceDisplaySetFrameBuf (topaddr=0x" + Integer.toHexString(topaddr) + ")"
                        + " is using main memory.");
            }

            if (gotBadFbBufParams) {
                gotBadFbBufParams = false;
                Modules.log.info(
                    "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                    ",bufferwidth=" + bufferwidth +
                    ",pixelformat=" + pixelformat +
                    ",sync=" + sync + ") ok");
            }

            if (pixelformatFb != this.pixelformatFb ||
                bufferwidthFb != this.bufferwidthFb ||
                Utilities.makePow2(height) != Utilities.makePow2(this.height))
            {
                createTex = true;
            }

            this.topaddrFb     = topaddr;
            this.bufferwidthFb = bufferwidth;
            this.pixelformatFb = pixelformat;
            this.sync          = sync;

            bottomaddrFb =
                topaddr + bufferwidthFb * height *
                getPixelFormatBytes(pixelformatFb);
            pixelsFb = getPixels(topaddrFb, bottomaddrFb);

            texS = (float)width / (float)bufferwidth;
            texT = (float)height / (float)Utilities.makePow2(height);

            detailsDirty = true;

            if (State.captureGeNextFrame && CaptureManager.hasListExecuted()) {
                CaptureManager.captureFrameBufDetails();
                CaptureManager.endCapture();
                State.captureGeNextFrame = false;
            }

            isFbShowing = true;
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceDisplayGetFrameBuf(
        int topaddr, int bufferwidth, int pixelformat, int sync)
    {
    	// Parameter sync can have the values 0 or 1. Unknown meaning.

    	Memory memory = Memory.getInstance();
        if (!memory.isAddressGood(topaddr    ) ||
            !memory.isAddressGood(bufferwidth) ||
            !memory.isAddressGood(pixelformat))
        {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        } else {
            memory.write32(topaddr    , this.topaddrFb    );
            memory.write32(bufferwidth, this.bufferwidthFb);
            memory.write32(pixelformat, this.pixelformatFb);
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    private int getVcount() {
        return vcount;
    }

    public void sceDisplayGetVcount() {
    	// 60 units per second
        Emulator.getProcessor().cpu.gpr[2] = getVcount();
    }

    private void blockCurrentThreadOnVblank(boolean doCallbacks) {
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        int threadId = threadMan.getCurrentThreadID();

        // Add a Vblank action to unblock the thread
        UnblockThreadAction vblankAction = new UnblockThreadAction(threadId);
        IntrManager.getInstance().addVBlankActionOnce(vblankAction);

        // Block the current thread
        if (doCallbacks) {
        	threadMan.hleBlockCurrentThreadCB(null, new VblankWaitStateChecker(getVcount()));
        } else {
        	threadMan.hleBlockCurrentThread();
        }
    }

    public void sceDisplayWaitVblankStart() {
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceDisplayWaitVblankStart");
        }

        Emulator.getProcessor().cpu.gpr[2] = 0;

        blockCurrentThreadOnVblank(false);
    }

    public void sceDisplayWaitVblankStartCB() {
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceDisplayWaitVblankStartCB");
        }

        Emulator.getProcessor().cpu.gpr[2] = 0;

        blockCurrentThreadOnVblank(true);
    }

    private void hleVblankStart() {
    	lastVblankMicroTime = Emulator.getClock().microTime();

    	// Vcount increases at each VBLANK
    	vcount++;
    }

    private boolean isVblank() {
    	// Test result: isVblank == true during 4.39% of the time
    	// -> Vblank takes 731.5 micros at each vblank interrupt
    	long nowMicroTime = Emulator.getClock().microTime();
    	long microTimeSinceLastVblank = nowMicroTime - lastVblankMicroTime;

    	return (microTimeSinceLastVblank <= 731);
    }

    public void sceDisplayIsVblank() {
        Emulator.getProcessor().cpu.gpr[2] = isVblank() ? 1 : 0;

        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceDisplayIsVblank returns " + Emulator.getProcessor().cpu.gpr[2]);
        }
    }

    public void sceDisplayWaitVblank() {
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceDisplayWaitVblank");
        }

        Emulator.getProcessor().cpu.gpr[2] = 0;
    	if (!isVblank()) {
    		sceDisplayWaitVblankStart();
    	}
    }

    public void sceDisplayWaitVblankStartMulti() {
        // Same as sceDisplayWaitVblankStart().
        // May be different on the PSP, because it suggests multiple graphics'
        // chips interaction (GE and ME, probably).
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceDisplayWaitVblankStartMulti");
        }

        Emulator.getProcessor().cpu.gpr[2] = 0;

        blockCurrentThreadOnVblank(false);
    }

    public void sceDisplayWaitVblankCB() {
        if (Modules.log.isDebugEnabled()) {
        	Modules.log.debug("sceDisplayWaitVblankCB");
        }

        Emulator.getProcessor().cpu.gpr[2] = 0;
        if (!isVblank()) {
        	sceDisplayWaitVblankStartCB();
        }
    }

    public void sceDisplayGetCurrentHcount() {
        Emulator.getProcessor().cpu.gpr[2] = (int) (getVcount() * 0.15f);
    }

    public void sceDisplayGetAccumulatedHcount() {
    	// 17143 units per second
        Emulator.getProcessor().cpu.gpr[2] = (int) (getVcount() * 285.72f);
    }

    public void sceDisplayGetFramePerSec() {
    	// Return float value in $f0
    	Emulator.getProcessor().cpu.fpr[0] = 59.940060f;
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceDisplayGetFramePerSec ret: " + Emulator.getProcessor().cpu.fpr[0]);
    	}
    }

    public void sceDisplayIsForeground() {
        if(isFbShowing) {
            Emulator.getProcessor().cpu.gpr[2] = 1;
        } else {
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    	if (Modules.log.isDebugEnabled()) {
    		Modules.log.debug("sceDisplayIsForeground ret: " + Emulator.getProcessor().cpu.gpr[2]);
    	}
    }

    public void sceDisplaySetHoldMode(int unk) {
        Modules.log.warn("IGNORING: sceDisplaySetHoldMode unk=" + unk);
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceDisplaySetResumeMode(int unk) {
        Modules.log.warn("IGNORING: sceDisplaySetResumeMode unk=" + unk);
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceDisplayGetBrightness(int leveladdr, int unkaddr) {
        Modules.log.warn("IGNORING: sceDisplayGetBrightness leveladdr=0x"
                + Integer.toHexString(leveladdr) + ", unkaddr=0x"
                + Integer.toHexString(unkaddr));
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public void sceDisplay_A83EF139() {
        Modules.log.warn("UNIMPLEMENTED: sceDisplay_A83EF139");
        Emulator.getProcessor().cpu.gpr[2] = 0;
    }

    public boolean isGeAddress(int address) {
        address &= Memory.addressMask;
        if (address >= topaddrGe && address < bottomaddrGe) {
            return true;
        }

        return false;
    }

    public boolean isFbAddress(int address) {
        address &= Memory.addressMask;
        if (address >= topaddrFb && address < bottomaddrFb) {
            return true;
        }

        return false;
    }

    public boolean isOnlyGEGraphics() {
        return onlyGEGraphics;
    }

    public void setOnlyGEGraphics(boolean onlyGEGraphics) {
        this.onlyGEGraphics = onlyGEGraphics;
        VideoEngine.log.info("Only GE Graphics: " + onlyGEGraphics);
    }

    //Screen rotation function
    public void rotate(int angleid)
    {
        ang = angleid;

        switch(angleid){
            case 0: //Rotate screen - 90º CW
                texS1 = texS2 = texS;
                texT2 = texT3 = texT;

                texS3 = texS4 = texT1 = texT4 = 0.0f;

                isrotating = true;

                break;

            case 1: //Rotate screen - 90º CCW
                texS3 = texS4 = texS;
                texT1 = texT4 = texT;

                texS1 = texS2 = texT2 = texT3 = 0.0f;

                isrotating = true;

                break;

            case 2: //Rotate screen - 180º (inverted, y axis)
                texS1 = texS4 = texS;
                texT3 = texT4 = texT;

                texS2 = texS3 = texT1 = texT2 = 0.0f;

                isrotating = true;

                break;

            case 3: //Rotate screen - Mirror (inverted, x axis)
                texS2 = texS3 = texS;
                texT1 = texT2 = texT;

                texS1 = texS4 = texT3 = texT4 = 0.0f;

                isrotating = true;

                break;

            case 4: //Normal display (reset)
            default:
                isrotating = false;

                break;
        }
    }

    //Testing screenshot taking function (using disc id)
    public void savescreen(GLAutoDrawable drawable)
    {
        final GL gl = drawable.getGL();

        int tag = 0;

        File screenshot = new File(State.discId + "-" + "Shot" + "-" + tag + ".png");
        File directory = new File(System.getProperty("user.dir"));

        for(File file : directory.listFiles())
        {
            if(file.getName().equals(screenshot.getName()))
            {
               tag++;
               screenshot = new File(State.discId + "-" + "Shot" + "-" + tag + ".png");
            }
        }

        gl.glReadBuffer(mode);
        BufferedImage img = Screenshot.readToBufferedImage(width, height);
        try{
            ImageIO.write(img, "png", screenshot);
        }catch(IOException e){
            return;
        }finally{
            img.flush();
        }

        getscreen = false;
    }


    // For capture/replay
    public int getTopAddrFb() { return topaddrFb; }
    public int getBufferWidthFb() { return bufferwidthFb; }
    public int getPixelFormatFb() { return pixelformatFb; }
    public int getSync() { return sync; }

    public int getTopAddrGe() {
    	return topaddrGe;
    }

    public int getBufferWidthGe() {
    	return bufferwidthGe;
    }

    public void captureGeImage(GL gl) {
    	// Create a GE texture (the texture texFb might not have the right size)
        int[] textures = new int[1];
        gl.glGenTextures(1, textures, 0);
        int texGe = textures[0];

        int pixelFormatGL = getPixelFormatGL(pixelformatGe);
        int formatGL = getFormatGL(pixelformatGe);

        gl.glBindTexture(GL.GL_TEXTURE_2D, texGe);
        gl.glTexImage2D(
            GL.GL_TEXTURE_2D, 0,
            internalTextureFormat,
            bufferwidthGe, Utilities.makePow2(heightGe), 0,
            formatGL,
            pixelFormatGL, null);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, getPixelFormatBytes(pixelformatGe));
        gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH, bufferwidthGe);

        // Copy screen to the GE texture
        gl.glCopyTexSubImage2D(
            GL.GL_TEXTURE_2D, 0,
            0, 0, 0, 0, widthGe, heightGe);

        // Copy the GE texture into temp buffer
        temp.clear();
        gl.glGetTexImage(
            GL.GL_TEXTURE_2D, 0, formatGL,
            pixelFormatGL, temp);

        // Capture the GE image
        CaptureManager.captureImage(topaddrGe, 0, temp, widthGe, heightGe, bufferwidthGe, pixelFormatGL, false, 0, false);

    	// Delete the GE texture
        gl.glDeleteTextures(1, textures, 0);
    }

    public boolean tryLockDisplay() {
    	boolean locked = displayLock.tryAcquire();
    	if (!locked) {
    		// Could not lock the display...
    		// Remember for 1 second that someone tried to lock the display
    		tryLockDisplay = true;
    		tryLockTimestamp = Emulator.getClock().milliTime();
    	} else {
    		tryLockDisplay = false;
    	}

    	return locked;
    }

    public void lockDisplay() {
    	while (true) {
	    	try {
				displayLock.acquire();
				break;
			} catch (InterruptedException e) {
				// Try again
			}
    	}
    }

    public void unlockDisplay() {
    	displayLock.release();
    }

    public boolean isTryLockActive() {
    	if (tryLockDisplay) {
    		long now = Emulator.getClock().milliTime();
    		// tryLockDisplay is only active for 1 second, then release it
    		if (now - tryLockTimestamp > 1000) {
    			tryLockDisplay = false;
    		}
    	}

    	return tryLockDisplay;
    }

    private static class AsyncDisplayThread extends Thread {
		private Semaphore displaySemaphore;
		private pspdisplay display;
		private boolean run;

		public AsyncDisplayThread(pspdisplay display) {
			this.display = display;
			displaySemaphore = new Semaphore(1);
			run = true;
		}

		@Override
		public void run() {
			while (run) {
				// If someone is trying to lock the display, leave it to him.
				if (display.isTryLockActive()) {
					try {
						// Sleep for 10 ms
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// Ignore Interrupt
					}
				} else {
					waitForDisplay();
					if (run) {
			        	if (!display.isOnlyGEGraphics() || VideoEngine.getInstance().hasDrawLists()) {
			        		display.lockDisplay();
			        		display.display();
			        		display.unlockDisplay();
			        	}
					}
				}
			}
		}

		public void display() {
			displaySemaphore.release();
		}

		private void waitForDisplay() {
			while (true) {
				try {
					int availablePermits = displaySemaphore.drainPermits();
					if (availablePermits > 0) {
						break;
					}

					if (displaySemaphore.tryAcquire(100, TimeUnit.MILLISECONDS)) {
						break;
					} else {
						//VideoEngine.log.info("Waiting for Display...");
					}
				} catch (InterruptedException e) {
				}
			}
		}

		public void exit() {
			run = false;
			display();
		}
	}

	private class DisplayVblankAction implements IAction {
		@Override
		public void execute() {
			hleVblankStart();
		}
	}

	private class VblankWaitStateChecker implements IWaitStateChecker {
		private int vcount;

		public VblankWaitStateChecker(int vcount) {
			this.vcount = vcount;
		}

		@Override
		public boolean continueWaitState(SceKernelThreadInfo thread, ThreadWaitInfo wait) {
			// Continue the wait state until the vcount changes
			return getVcount() == vcount;
		}
	}
}