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
import javax.media.opengl.GLEventListener;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Settings;
import jpcsp.graphics.VideoEngine;

/**
 * @author shadow, aisesal
 */
public final class pspdisplay extends GLCanvas implements GLEventListener {
    private static pspdisplay instance;
    public static pspdisplay get_instance() {
        if (instance == null)
            instance = new pspdisplay();
        return instance;
    }
    
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
    private int topaddr;
    private int bufferwidth;
    private int pixelformat;
    private int sync;
    
    // additional variables
    private int bottomaddr;
    private boolean refreshRequired;
    private long lastUpdate;
    
    // Canvas fields
    private ByteBuffer pixels;
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
    
    private pspdisplay() {
        setSize(480, 272);
        addGLEventListener(this);
        texFb = -1;
    }
    
    public void Initialise() {
        mode        = 0;
        width       = 480;
        height      = 272;
        topaddr     = MemoryMap.START_VRAM;
        bufferwidth = 512;
        pixelformat = PSP_DISPLAY_PIXEL_FORMAT_8888;
        sync        = PSP_DISPLAY_SETBUF_IMMEDIATE;
        
        bottomaddr =
            topaddr + bufferwidth * height * getPixelFormatBytes(pixelformat);
        
        refreshRequired = true;
        createTex = true;
        
        disableGE =
            Settings.get_instance().readBoolOptions("emuoptions/disablege");
        
        getPixels();
    }
    
    public void step() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 1000 / 60) {
            if (refreshRequired) {
                display();
                refreshRequired = false;
            }
            lastUpdate = now;
        }
    }
    
    public void write8(int address, int data) {
        address &= 0x3FFFFFFF;
        if (address >= topaddr && address < bottomaddr)
            setDirty(true);
    }
    
    public void write16(int address, int data) {
        address &= 0x3FFFFFFF;
        if (address >= topaddr && address < bottomaddr)
            setDirty(true);
    }
    
    public void write32(int address, int data) {
        address &= 0x3FFFFFFF;
        if (address >= topaddr && address < bottomaddr)
            setDirty(true);
    }
    
    public void setDirty(boolean dirty) {
        refreshRequired = dirty;
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
    
    private void getPixels() {
        Memory memory = Emulator.getMemory();
        byte[] all = memory.videoram.array();
        pixels = ByteBuffer.wrap(
            all,
            topaddr - MemoryMap.START_VRAM + memory.videoram.arrayOffset(),
            bottomaddr - topaddr).slice();
        pixels.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    private int makePow2(int n) {
        --n;
        n = (n >>  1) | n;
        n = (n >>  2) | n;
        n = (n >>  4) | n;
        n = (n >>  8) | n;
        n = (n >> 16) | n;
        return ++n;
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
    
    private void drawFrameBuffer(final GL gl, boolean first, boolean invert) {
        gl.glPushAttrib(GL.GL_ALL_ATTRIB_BITS);
        
        if (first)
            gl.glViewport(0, 0, width, height);
        else
            gl.glViewport(0, 0, canvasWidth, canvasHeight);
        
        gl.glDisable(GL.GL_DEPTH_TEST);
        
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
        
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex2f(0.0f, 0.0f);
        
        gl.glTexCoord2f(0.0f, texT);
        gl.glVertex2f(0.0f, height);
        
        gl.glTexCoord2f(texS, texT);
        gl.glVertex2f(width, height);
        
        gl.glTexCoord2f(texS, 0.0f);
        gl.glVertex2f(width, 0.0f);
        
        gl.glEnd();
        
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopAttrib();
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
            
            gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);
            gl.glTexImage2D(
                GL.GL_TEXTURE_2D, 0,
                GL.GL_RGBA,
                bufferwidth, makePow2(height), 0,
                GL.GL_RGBA,
                getPixelFormatGL(pixelformat), null);
            gl.glTexParameteri(
                GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
            gl.glTexParameteri(
                GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
            gl.glTexParameteri(
                GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
            gl.glTexParameteri(
                GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
            
            temp = ByteBuffer.allocate(
                bufferwidth * makePow2(height) *
                getPixelFormatBytes(pixelformat));
            temp.order(ByteOrder.LITTLE_ENDIAN);
            
            createTex = false;
        }
        
        if (texFb == -1) {
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            return;
        }
        
        pixels.clear();
        gl.glTexSubImage2D(
            GL.GL_TEXTURE_2D, 0,
            0, 0, bufferwidth, height,
            GL.GL_RGBA, getPixelFormatGL(pixelformat), pixels);
        
        if (disableGE) {
            drawFrameBuffer(gl, false, true);
        } else {
            drawFrameBuffer(gl, true, true);
            
            gl.glViewport(0, 0, width, height);
            VideoEngine.getEngine(gl, true, true).update();
        
            gl.glBindTexture(GL.GL_TEXTURE_2D, texFb);
            gl.glCopyTexSubImage2D(
                GL.GL_TEXTURE_2D, 0,
                0, 0, 0, 0, width, height);
        
            temp.clear();
            pixels.clear();
            gl.glGetTexImage(
                GL.GL_TEXTURE_2D, 0, GL.GL_RGBA,
                getPixelFormatGL(pixelformat), temp);
            temp.put(pixels);
            drawFrameBuffer(gl, false, false);
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
        System.out.println(
            "sceDisplaySetMode(mode=" + mode +
            ",width=" + width +
            ",height=" + height + ")");
        
        if (width <= 0 || height <= 0) {
            Emulator.getProcessor().gpr[2] = -1;
        } else {
            this.mode   = mode;
            this.width  = width;
            this.height = height;
            
            bottomaddr =
                topaddr + bufferwidth * height *
                getPixelFormatBytes(pixelformat);
            
            refreshRequired = true;
            
            Emulator.getProcessor().gpr[2] = 0;
        }
    }
    
    public void sceDisplayGetMode(int pmode, int pwidth, int pheight) {
        Memory memory = Emulator.getMemory();
        if (!memory.isAddressGood(pmode  ) ||
            !memory.isAddressGood(pwidth ) ||
            !memory.isAddressGood(pheight))
        {
            Emulator.getProcessor().gpr[2] = -1;
        } else {
            memory.write32(pmode  , mode  );
            memory.write32(pwidth , width );
            memory.write32(pheight, height);
            Emulator.getProcessor().gpr[2] = 0;
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
            Emulator.getProcessor().gpr[2] = -1;
        } else {
            if (pixelformat != this.pixelformat ||
                bufferwidth != this.bufferwidth ||
                makePow2(height) != makePow2(this.height))
            {
                createTex = true;
            }
            
            this.topaddr     = topaddr;
            this.bufferwidth = bufferwidth;
            this.pixelformat = pixelformat;
            this.sync        = sync;
            
            bottomaddr =
                topaddr + bufferwidth * height *
                getPixelFormatBytes(pixelformat);
            getPixels();
            
            texS = (float)width / (float)bufferwidth;
            texT = (float)height / (float)makePow2(height);
            
            refreshRequired = true;
            display();
            
            Emulator.getProcessor().gpr[2] = 0;
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
            Emulator.getProcessor().gpr[2] = -1;
        } else {
            memory.write32(topaddr    , this.topaddr    );
            memory.write32(bufferwidth, this.bufferwidth);
            memory.write32(pixelformat, this.pixelformat);
            memory.write32(sync       , this.sync       );
            Emulator.getProcessor().gpr[2] = 0;
        }
    }
    
    public void sceDisplayGetVcount() {
        // TODO: implement sceDisplayGetVcount
        Emulator.getProcessor().gpr[2] = 0;
    }
    
    public void sceDisplayWaitVblankStart() {
        // TODO: implement sceDisplayWaitVblankStart
        ThreadMan.get_instance().ThreadMan_sceKernelDelayThread(0);
        Emulator.getProcessor().gpr[2] = 0;
    }
    
    public void sceDisplayWaitVblankStartCB() {
        // TODO: implement sceDisplayWaitVblankStartCB
        Emulator.getProcessor().gpr[2] = 0;
    }
    
    public void sceDisplayWaitVblank() {
        // TODO: implement sceDisplayWaitVblank
        ThreadMan.get_instance().ThreadMan_sceKernelDelayThread(0);
        Emulator.getProcessor().gpr[2] = 0;
    }
    
    public void sceDisplayWaitVblankCB() {
        // TODO: implement sceDisplayWaitVblankCB
        Emulator.getProcessor().gpr[2] = 0;
    }
}
