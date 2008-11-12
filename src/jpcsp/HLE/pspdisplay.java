/*
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/pspdisplay_8h.html


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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Settings;
import jpcsp.graphics.VideoEngine;
import jpcsp.util.Utilities;

/**
 * @author shadow, aisesal
 */
public final class pspdisplay extends GLCanvas implements GLEventListener {
    private static pspdisplay instance;
    public static pspdisplay getInstance() {
        if (instance == null) {

            // We need to ask for stencil buffer
            GLCapabilities capabilities = new GLCapabilities();
            capabilities.setStencilBits(8);

            // Along with swapBuffers() seems to have no effect
            //capabilities.setDoubleBuffered(true);

            instance = new pspdisplay(capabilities);
        }
        return instance;
    }

    private static final boolean useGlReadPixels = true;

    // PspDisplayPixelFormats enum
    public static final int PSP_DISPLAY_PIXEL_FORMAT_565  = 0;
    public static final int PSP_DISPLAY_PIXEL_FORMAT_5551 = 1;
    public static final int PSP_DISPLAY_PIXEL_FORMAT_4444 = 2;
    public static final int PSP_DISPLAY_PIXEL_FORMAT_8888 = 3;

    // PspDisplaySetBufSync enum
    public static final int PSP_DISPLAY_SETBUF_IMMEDIATE = 0;
    public static final int PSP_DISPLAY_SETBUF_NEXTFRAME = 1;

    // PspDisplayErrorCodes enum
    public static final int PSP_DISPLAY_ERROR_OK       = 0;
    public static final int PSP_DISPLAY_ERROR_POINTER  = 0x80000103;
    public static final int PSP_DISPLAY_ERROR_ARGUMENT = 0x80000107;

    public boolean disableGE;

    // current display mode
    private int mode;
    private int width;
    private int height;

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

    private volatile boolean refreshRequired;
    private long lastUpdate;

    // Canvas fields
    private ByteBuffer pixelsFb;
    private ByteBuffer pixelsGe;
    private ByteBuffer temp;
    private int canvasWidth;
    private int canvasHeight;
    private boolean createTex;
    private int texFb;
    private float texS;
    private float texT;

    // fps counter variables
    private long prevStatsTime = 0;
    private long frameCount = 0;
    private long actualframeCount = 0;
    private long reportCount = 0;
    private double averageFPS = 0.0;

    private int vcount;
    private float accumulatedHcount;
    private float currentHcount;

    // Texture state
    private float[] texRgbScale = new float[2];
    private int[] texMode = new int[2];
    private int[] texSrc0Alpha = new int[2];
    private int texStackIndex = 0;

    private pspdisplay (GLCapabilities capabilities) {
    	super (capabilities);

        setSize(480, 272);
        addGLEventListener(this);
        texFb = -1;
    }

    public void Initialise() {
        mode          = 0;
        width         = 480;
        height        = 272;
        topaddrFb     = MemoryMap.START_VRAM;
        bufferwidthFb = 512;
        pixelformatFb = PSP_DISPLAY_PIXEL_FORMAT_8888;
        sync          = PSP_DISPLAY_SETBUF_IMMEDIATE;

        bottomaddrFb =
            topaddrFb + bufferwidthFb * height * getPixelFormatBytes(pixelformatFb);

        refreshRequired = true;
        createTex = true;

        disableGE =
            Settings.getInstance().readBool("emu.disablege");

        pixelsFb = getPixels(topaddrFb, bottomaddrFb);

        topaddrGe     = topaddrFb;
        bufferwidthGe = bufferwidthFb;
        pixelformatGe = pixelformatFb;
        bottomaddrGe  = bottomaddrFb;
        pixelsGe = getPixels(topaddrGe, bottomaddrGe);

        setGeBufCalledAtLeastOnce = false;
        gotBadGeBufParams = false;
        gotBadFbBufParams = false;

        vcount = 0;
        accumulatedHcount = 0.0f;
        currentHcount = 0.0f;
    }

    public void step() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 1000 / 60) {
            if (refreshRequired) {
                display();
                refreshRequired = false;
            }
            vcount++;
            accumulatedHcount += 286.15f;
            currentHcount += 0.15f;
            lastUpdate = now;
        }
    }

    public void write8(int address, int data) {
        address &= 0x3FFFFFFF;
        if (address >= topaddrFb && address < bottomaddrFb)
            setDirty(true);
    }

    public void write16(int address, int data) {
        address &= 0x3FFFFFFF;
        if (address >= topaddrFb && address < bottomaddrFb)
            setDirty(true);
    }

    public void write32(int address, int data) {
        address &= 0x3FFFFFFF;
        if (address >= topaddrFb && address < bottomaddrFb)
            setDirty(true);
    }

    public void setDirty(boolean dirty) {
        refreshRequired = dirty;
    }

    public void hleDisplaySetGeBuf(
        int topaddr, int bufferwidth, int pixelformat)
    {
        topaddr &= 0x3fffffff;
        topaddr += MemoryMap.START_VRAM;
        if (topaddr < MemoryMap.START_VRAM || topaddr >= MemoryMap.END_VRAM ||
            bufferwidth <= 0 || (bufferwidth & (bufferwidth - 1)) != 0 ||
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

            this.topaddrGe     = topaddr;
            this.bufferwidthGe = bufferwidth;
            this.pixelformatGe = pixelformat;

            bottomaddrGe =
                topaddr + bufferwidthGe * height *
                getPixelFormatBytes(pixelformatGe);
            pixelsGe = getPixels(topaddrGe, bottomaddrGe);
        }

        setGeBufCalledAtLeastOnce = true;
    }

    private static int getPixelFormatBytes(int pixelformat) {
        return pixelformat == PSP_DISPLAY_PIXEL_FORMAT_8888 ? 4 : 2;
    }

    private static int getPixelFormatGL(int pixelformat) {
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

    private ByteBuffer getPixels(int topaddr, int bottomaddr) {
        Memory memory = Emulator.getMemory();
        byte[] all = memory.videoram.array();
        ByteBuffer pixels = ByteBuffer.wrap(
            all,
            topaddr - MemoryMap.START_VRAM + memory.videoram.arrayOffset(),
            bottomaddr - topaddr).slice();
        pixels.order(ByteOrder.LITTLE_ENDIAN);
        return pixels;
    }

    private void reportFPSStats() {
        frameCount++;
        long timeNow = System.nanoTime();
        long realElapsedTime = (timeNow - prevStatsTime) / 1000000L;

        if (realElapsedTime > 1000L) {
            reportCount++;
            int lastFPS = (int)(frameCount - actualframeCount);
            averageFPS = (double)frameCount / reportCount;
            actualframeCount = frameCount;
            prevStatsTime = timeNow;

            Emulator.setFpsTitle("averageFPS: " + String.format("%.1f", averageFPS) + " lastFPS: " + lastFPS);
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

    private void drawFrameBuffer(final GL gl, boolean first, boolean invert) {
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

        pushTexEnv(gl);
        gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_RGB_SCALE, 1.0f);
        gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
        gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_SRC0_ALPHA, GL.GL_TEXTURE);

        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, getPixelFormatBytes(pixelformatFb));
        gl.glPixelStorei(GL.GL_UNPACK_ROW_LENGTH, bufferwidthFb);

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

        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);
        gl.glBegin(GL.GL_QUADS);

        gl.glColor3f(1.0f, 1.0f, 1.0f);

        gl.glTexCoord2f(texS, texT);
        gl.glVertex2f(width, height);

        gl.glTexCoord2f(0.0f, texT);
        gl.glVertex2f(0.0f, height);

        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex2f(0.0f, 0.0f);

        gl.glTexCoord2f(texS, 0.0f);
        gl.glVertex2f(width, 0.0f);

        gl.glEnd();

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopAttrib();

        popTexEnv(gl);
    }

    private void copyScreenToPixels(GL gl, ByteBuffer pixels, int bufferwidth, int pixelformat) {
        // Using glReadPixels instead of glGetTexImage is showing
        // between 7 and 13% performance increase.
        if (useGlReadPixels) {
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glOrtho(0.0, width, height, 0.0, -1.0, 1.0);
            int bufferStep = bufferwidth * getPixelFormatBytes(pixelformat);
            int pixelFormatGL = getPixelFormatGL(pixelformat);
            // Y-Axis on PSP is flipped against OpenGL, so we have to copy row by row
            for (int y = 0, bufferPos = 0; y < height; y++, bufferPos += bufferStep) {
                pixels.position(bufferPos);
                gl.glReadPixels(0, y, width, 1, GL.GL_RGBA, pixelFormatGL, pixels);
            }
            gl.glPopMatrix();
        } else {
            // Set texFb as the current texture
            gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);

            // Copy screen to the current texture
            gl.glCopyTexSubImage2D(
                GL.GL_TEXTURE_2D, 0,
                0, 0, 0, 0, width, height);

            // Copy the current texture into memory
            temp.clear();
            gl.glGetTexImage(
                GL.GL_TEXTURE_2D, 0, GL.GL_RGBA,
                getPixelFormatGL(pixelformat), temp);

            // Copy temp into pixels, temp is probably square and pixels is less,
            // a smaller rectangle, otherwise we could copy straight into pixels.
            temp.clear();
            int limit = temp.limit();
            temp.limit(pixels.limit());
            pixels.clear();
            pixels.put(temp);
            temp.limit(limit);
        }
    }

    // GLEventListener methods

    @Override
    public void init(GLAutoDrawable drawable) {
        final GL gl = drawable.getGL();
        gl.setSwapInterval(1);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
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
            gl.glTexImage2D(
                GL.GL_TEXTURE_2D, 0,
                GL.GL_RGB, // GL.GL_RGBA
                bufferwidthFb, Utilities.makePow2(height), 0,
                GL.GL_RGBA,
                getPixelFormatGL(pixelformatFb), null);
            gl.glTexParameteri(
                GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
            gl.glTexParameteri(
                GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
            gl.glTexParameteri(
                GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
            gl.glTexParameteri(
                GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);

            if (!useGlReadPixels) {
                temp = ByteBuffer.allocate(
                    bufferwidthFb * Utilities.makePow2(height) *
                    getPixelFormatBytes(pixelformatFb));
                temp.order(ByteOrder.LITTLE_ENDIAN);
            }

            createTex = false;
        }

        if (texFb == -1) {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            return;
        }

        if (disableGE) {
            // TODO: Use texture rectangles, as NPOT give problems with ATI drivers
            pixelsFb.clear();
            gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);
            gl.glTexSubImage2D(
                GL.GL_TEXTURE_2D, 0,
                0, 0, bufferwidthFb, height,
                GL.GL_RGBA, getPixelFormatGL(pixelformatFb), pixelsFb);

            // Debug step, copy screen back into pixelsFb
            drawFrameBuffer(gl, false, false);
            copyScreenToPixels(gl, pixelsFb, bufferwidthFb, pixelformatFb);

            drawFrameBuffer(gl, false, true);
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
	            gl.glTexSubImage2D(
	                GL.GL_TEXTURE_2D, 0,
	                0, 0, width, height,
	                GL.GL_RGBA, getPixelFormatGL(pixelformatGe), pixelsGe);
	            drawFrameBuffer(gl, false, true);
            }

            if (VideoEngine.getEngine(gl, true, true).update()) {
                // Update VRAM only if GE actually drew something
                // Set texFb as the current texture
                gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);

                // Copy screen to the current texture
                gl.glCopyTexSubImage2D(
                    GL.GL_TEXTURE_2D, 0,
                    0, 0, 0, 0, width, height);

                // Re-render GE/current texture upside down
                drawFrameBuffer(gl, true, true);

                // Save GE/current texture to vram
                copyScreenToPixels(gl, pixelsGe, bufferwidthGe, pixelformatGe);
            }

            // Render FB
            pixelsFb.clear();
            gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);
            gl.glTexSubImage2D(
                GL.GL_TEXTURE_2D, 0,
                0, 0, bufferwidthFb, height,
                GL.GL_RGBA, getPixelFormatGL(pixelformatFb), pixelsFb);
            drawFrameBuffer(gl, false, true);

            //swapBuffers();
        }

        reportFPSStats();
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

            refreshRequired = true;

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceDisplayGetMode(int pmode, int pwidth, int pheight) {
        Memory memory = Emulator.getMemory();
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

    public void sceDisplaySetFrameBuf(
        int topaddr, int bufferwidth, int pixelformat, int sync)
    {
        topaddr &= 0x3FFFFFFF;
        if (topaddr < MemoryMap.START_VRAM || topaddr >= MemoryMap.END_VRAM ||
            bufferwidth <= 0 || (bufferwidth & (bufferwidth - 1)) != 0 ||
            pixelformat < 0 || pixelformat > 3 ||
            sync < 0 || sync > 1)
        {
            // TODO allow main memory as well as vram when we find an app that does that
            Modules.log.warn(
                "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                ",bufferwidth=" + bufferwidth +
                ",pixelformat=" + pixelformat +
                ",sync=" + sync + ") bad params");
            gotBadFbBufParams = true;
            Emulator.getProcessor().cpu.gpr[2] = -1;
        } else {
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

            refreshRequired = true;
            //display();

            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceDisplayGetFrameBuf(
        int topaddr, int bufferwidth, int pixelformat, int sync)
    {
        Memory memory = Emulator.getMemory();
        if (!memory.isAddressGood(topaddr    ) ||
            !memory.isAddressGood(bufferwidth) ||
            !memory.isAddressGood(pixelformat) ||
            !memory.isAddressGood(sync))
        {
            Emulator.getProcessor().cpu.gpr[2] = -1;
        } else {
            memory.write32(topaddr    , this.topaddrFb    );
            memory.write32(bufferwidth, this.bufferwidthFb);
            memory.write32(pixelformat, this.pixelformatFb);
            memory.write32(sync       , this.sync         );
            Emulator.getProcessor().cpu.gpr[2] = 0;
        }
    }

    public void sceDisplayGetVcount() {
        Emulator.getProcessor().cpu.gpr[2] = vcount;
    }

    public void sceDisplayWaitVblankStart() {
        // TODO: implement sceDisplayWaitVblankStart
        Emulator.getProcessor().cpu.gpr[2] = 0;
        ThreadMan.getInstance().yieldCurrentThread();
    }

    public void sceDisplayWaitVblankStartCB() {
        // TODO: implement sceDisplayWaitVblankStartCB
        Emulator.getProcessor().cpu.gpr[2] = 0;
        ThreadMan.getInstance().yieldCurrentThread();
    }

    public void sceDisplayWaitVblank() {
        // TODO: implement sceDisplayWaitVblank
        Emulator.getProcessor().cpu.gpr[2] = 0;
        ThreadMan.getInstance().yieldCurrentThread();
    }

    public void sceDisplayWaitVblankCB() {
        // TODO: implement sceDisplayWaitVblankCB
        Emulator.getProcessor().cpu.gpr[2] = 0;
        ThreadMan.getInstance().yieldCurrentThread();
    }

    public void sceDisplayGetCurrentHcount() {
        Emulator.getProcessor().cpu.gpr[2] = (int)currentHcount;
    }

    public void sceDisplayGetAccumulatedHcount() {
        Emulator.getProcessor().cpu.gpr[2] = (int)accumulatedHcount;
    }

    public boolean isGeAddress(int address) {
        address &= 0x3FFFFFFF;
        if (address >= topaddrGe && address < bottomaddrGe) {
        	return true;
        }

        return false;
    }

    public boolean isFbAddress(int address) {
        address &= 0x3FFFFFFF;
        if (address >= topaddrFb && address < bottomaddrFb) {
        	return true;
        }

        return false;
    }
}
