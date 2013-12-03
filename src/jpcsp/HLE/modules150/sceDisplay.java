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
package jpcsp.HLE.modules150;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_FORMAT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_MODE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_POINTER;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_SIZE;
import static jpcsp.MemoryMap.START_VRAM;
import static jpcsp.graphics.GeCommands.TFLT_NEAREST;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.graphics.GeCommands.TWRAP_WRAP_MODE_CLAMP;
import static jpcsp.graphics.VideoEngine.SIZEOF_FLOAT;
import static jpcsp.util.Utilities.makePow2;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.awt.Robot;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jpcsp.Emulator;
import jpcsp.MainGUI;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.graphics.GEProfiler;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VertexCache;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.RenderingEngineFactory;
import jpcsp.graphics.RE.RenderingEngineLwjgl;
import jpcsp.graphics.RE.buffer.IREBufferManager;
import jpcsp.graphics.RE.externalge.ExternalGE;
import jpcsp.graphics.capture.CaptureManager;
import jpcsp.graphics.textures.GETexture;
import jpcsp.graphics.textures.GETextureManager;
import jpcsp.graphics.textures.TextureCache;
import jpcsp.hardware.Screen;
import jpcsp.memory.IMemoryReaderWriter;
import jpcsp.memory.MemoryReaderWriter;
import jpcsp.scheduler.UnblockThreadAction;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.AbstractStringSettingsListener;
import jpcsp.settings.ISettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

@HLELogging
public class sceDisplay extends HLEModule {

    public static Logger log = Modules.getLogger("sceDisplay");

    @SuppressWarnings("serial")
    class AWTGLCanvas_sceDisplay extends AWTGLCanvas {

        public AWTGLCanvas_sceDisplay() throws LWJGLException {
            super(null, new PixelFormat().withBitsPerPixel(8).withAlphaBits(8).withStencilBits(8).withSamples(antiAliasSamplesNum), null, new ContextAttribs().withDebug(useDebugGL));
        }

        @Override
        protected void paintGL() {
            VideoEngine videoEngine = VideoEngine.getInstance();

            if (log.isTraceEnabled()) {
                log.trace(String.format("paintGL resize=%f, size(%dx%d), canvas(%dx%d), location(%d,%d)", viewportResizeFilterScaleFactor, canvas.getSize().width, canvas.getSize().height, canvasWidth, canvasHeight, canvas.getLocation().x, canvas.getLocation().y));
            }

            if (resizePending && Emulator.getMainGUI().isVisible()) {
                // Resize the MainGUI to use the preferred size of this sceDisplay
                Emulator.getMainGUI().pack();
                resizePending = false;
            }

            if (statistics != null) {
                statistics.start();
            }

            if (resetDisplaySettings) {
                // Some display settings have been updated,
                // a new rendering engine has to be created.
                if (isStarted) {
                    videoEngine.stop();
                }
                TextureCache.getInstance().reset(re);
                VertexCache.getInstance().reset(re);
                startModules = true;
                re = null;
                reDisplay = null;
                resetDisplaySettings = false;

                saveGEToTexture = Settings.getInstance().readBool("emu.enablegetexture");
                if (saveGEToTexture) {
                    log.info("Saving GE to Textures");
                }
            }

            if (re == null) {
                if (startModules) {
                    re = RenderingEngineFactory.createRenderingEngine();
                    if (isUsingSoftwareRenderer()) {
                        reDisplay = RenderingEngineFactory.createRenderingEngineForDisplay();
                        reDisplay.setGeContext(videoEngine.getContext());
                    } else {
                        reDisplay = re;
                    }
                } else {
                    re = RenderingEngineFactory.createInitialRenderingEngine();
                    reDisplay = re;
                }
            }

            if (startModules) {
                saveGEToTexture = Settings.getInstance().readBool("emu.enablegetexture");
                if (saveGEToTexture) {
                    GETextureManager.getInstance().reset(reDisplay);
                }
                videoEngine.start();
                drawBuffer = reDisplay.getBufferManager().genBuffer(IRenderingEngine.RE_ARRAY_BUFFER, IRenderingEngine.RE_FLOAT, 16, IRenderingEngine.RE_DYNAMIC_DRAW);
                drawBufferArray = new float[16];
                startModules = false;
                if (saveGEToTexture && !re.isFramebufferObjectAvailable()) {
                    saveGEToTexture = false;
                    log.warn("Saving GE to Textures has been automatically disabled: FBO is not supported by this OpenGL version");
                }
                isStarted = true;
            }

            if (!isStarted) {
                reDisplay.clear(0.0f, 0.0f, 0.0f, 0.0f);
                return;
            }

            if (createTex) {
                // Create two textures: one at original PSP size and
                // one resized to the display size
                texFb = createTexture(texFb, false);
                resizedTexFb = createTexture(resizedTexFb, true);

                checkTemp();
                createTex = false;
            }

	    	if (resetGeTextures) {
	    		if (saveGEToTexture) {
	    			GETextureManager.getInstance().reset(reDisplay);
	    		}
	    		resetGeTextures = false;
	    	}

            // If we are not rendering this frame, skip the next sceDisplaySetFrameBuf call,
            // assuming the application is doing double buffering.
            skipNextFrameBufferSwitch = videoEngine.isSkipThisFrame();

            boolean doSwapBuffers = true;

            // Copy the current frame buffer object, in case it is modified by currentFb while rendering.
            FrameBufferSettings currentFb = fb;

            setInsideRendering(true);

            if (ExternalGE.isActive()) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceDisplay.paintGL - ExternalGE - rendering the FB 0x%08X", currentFb.getTopAddr()));
                }

            	reDisplay.startDisplay();
            	drawFrameBufferFromMemory(currentFb);
            	reDisplay.endDisplay();

                if (log.isDebugEnabled()) {
                    log.debug("sceDisplay.paintGL - ExternalGE - end display");
                }
            } else if (isUsingSoftwareRenderer()) {
                // Software rendering: the processing of the GE list is done by the
                // SoftwareRenderingDisplayThread.
                // We just need to display the frame buffer.
                if (softwareRenderingDisplayThread == null) {
                    re.startDisplay();
                    videoEngine.update();
                    re.endDisplay();
                }

                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceDisplay.paintGL - software - rendering the FB 0x%08X", currentFb.getTopAddr()));
                }

                reDisplay.startDisplay();
                drawFrameBufferFromMemory(currentFb);
                reDisplay.endDisplay();

                if (log.isDebugEnabled()) {
                    log.debug("sceDisplay.paintGL - software - end display");
                }
            } else if (isOnlyGEGraphics()) {
                // Hardware rendering where only the currently rendered GE list is displayed,
                // not the frame buffer from memory.
                if (log.isDebugEnabled()) {
                    log.debug("sceDisplay.paintGL - start display - only GE");
                }
                re.startDisplay();

                // Display this screen (i.e. swap buffers) only if something has been rendered
                doSwapBuffers = videoEngine.update();

                re.endDisplay();
                if (log.isDebugEnabled()) {
                    log.debug("sceDisplay.paintGL - end display - only GE");
                }
            } else {
                // Hardware rendering:
                // 1) GE list is rendered to the screen
                // 2) the result of the rendering is stored into the GE frame buffer
                // 3) the active FB frame buffer is reloaded from memory to the screen for final display
                if (log.isDebugEnabled()) {
                    log.debug("sceDisplay.paintGL - start display");
                }
                re.startDisplay();

                // The GE will be reloaded to the screen by the VideoEngine
                if (videoEngine.update()) {
                    // Save the GE only if it actually drew something
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("sceDisplay.paintGL - saving the GE to memory 0x%08X", ge.getTopAddr()));
                    }

                    if (saveGEToTexture && !videoEngine.isVideoTexture(ge.getTopAddr())) {
                        GETexture geTexture = GETextureManager.getInstance().getGETexture(reDisplay, ge.getTopAddr(), ge.getBufferWidth(), ge.getWidth(), ge.getHeight(), ge.getPixelFormat(), true);
                        geTexture.copyScreenToTexture(re);
                    } else {
                        // Set texFb as the current texture
                        reDisplay.bindTexture(resizedTexFb);
                        reDisplay.setTextureFormat(ge.getPixelFormat(), false);

                        // Copy screen to the current texture
                        reDisplay.copyTexSubImage(0, 0, 0, 0, 0, getResizedWidth(ge.getWidth()), getResizedHeight(ge.getHeight()));

                        // Re-render GE/current texture upside down
                        drawFrameBuffer(currentFb, true, true, currentFb.getBufferWidth(), currentFb.getPixelFormat(), currentFb.getWidth(), currentFb.getHeight());

                        // Save GE/current texture to vram
                        copyScreenToPixels(ge.getPixels(), ge.getBufferWidth(), ge.getPixelFormat(), ge.getWidth(), ge.getHeight());
                    }
                }

                // Render the FB
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceDisplay.paintGL - rendering the FB 0x%08X", currentFb.getTopAddr()));
                }
                if (saveGEToTexture && !videoEngine.isVideoTexture(fb.getTopAddr())) {
                    GETexture geTexture = GETextureManager.getInstance().getGETexture(reDisplay, currentFb.getTopAddr(), currentFb.getBufferWidth(), currentFb.getWidth(), currentFb.getHeight(), currentFb.getPixelFormat(), true);
                    geTexture.copyTextureToScreen(reDisplay);
                } else {
                    drawFrameBufferFromMemory(currentFb);
                }

                re.endDisplay();

                if (log.isDebugEnabled()) {
                    log.debug("sceDisplay.paintGL - end display");
                }
            }

            setInsideRendering(false);

            // Perform OpenGL double buffering
            if (doSwapBuffers) {
                paintFrameCount++;
                try {
                    canvas.swapBuffers();
                } catch (LWJGLException e) {
                }
            }

            // Update the current FPS every second
            reportFPSStats();

            if (statistics != null) {
                statistics.end();
            }

            if (wantScreenshot) {
                saveScreen();
            }
        }

        @Override
        protected void initGL() {
            setSwapInterval(0);
            super.initGL();

            // Collect debugging information...
            initGLcalled = true;
            openGLversion = RenderingEngineLwjgl.getVersion();
        }

        @Override
        public void setBounds(int x, int y, int width, int height) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("setBounds width=%d, height=%d", width, height));
            }
            canvasWidth = width;
            canvasHeight = height;
            super.setBounds(x, y, width, height);
        }

        @Override
        public void componentResized(ComponentEvent e) {
            setViewportResizeScaleFactor(getWidth(), getHeight());
        }
    }

    private static class FrameBufferSettings {

        private int topAddr;
        private int bottomAddr;
        private int bufferWidth;
        private int width;
        private int height;
        private int pixelFormat;
        private Buffer pixels;

        public FrameBufferSettings(int topAddr, int bufferWidth, int width, int height, int pixelFormat) {
            this.topAddr = topAddr & Memory.addressMask;
            this.bufferWidth = bufferWidth;
            this.width = width;
            this.height = height;
            this.pixelFormat = pixelFormat;
            update();
        }

        public FrameBufferSettings(FrameBufferSettings copy) {
            topAddr = copy.topAddr;
            bottomAddr = copy.bottomAddr;
            bufferWidth = copy.bufferWidth;
            width = copy.width;
            height = copy.height;
            pixelFormat = copy.pixelFormat;
            pixels = copy.pixels;
        }

        private void update() {
            int size = bufferWidth * height * getPixelFormatBytes(pixelFormat);
            bottomAddr = topAddr + size;
            pixels = Memory.getInstance().getBuffer(topAddr, size);
        }

        public int getTopAddr() {
            return topAddr;
        }

        public int getBottomAddr() {
            return bottomAddr;
        }

        public int getBufferWidth() {
            return bufferWidth;
        }

        public int getPixelFormat() {
            return pixelFormat;
        }

        public Buffer getPixels() {
            return pixels;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public boolean isRawAddressInside(int address) {
            // vram address is lower than main memory so check the end of the buffer first, it's more likely to fail
            return address >= topAddr && address < bottomAddr;
        }

        public boolean isAddressInside(int address) {
            return isRawAddressInside(address & Memory.addressMask);
        }

        public void setDimension(int width, int height) {
            this.width = width;
            this.height = height;
            update();
        }

        @Override
        public String toString() {
            return String.format("0x%08X-0x%08X, %dx%d, bufferWidth=%d, pixelFormat=%d", topAddr, bottomAddr, width, height, bufferWidth, pixelFormat);
        }
    }
    protected AWTGLCanvas_sceDisplay canvas;

    public AWTGLCanvas getCanvas() {
        return canvas;
    }
    private boolean onlyGEGraphics = false;
    private boolean saveGEToTexture = false;
    private boolean useSoftwareRenderer = false;
    private boolean saveStencilToMemory = false;
    private static final boolean useDebugGL = false;
    private static final int internalTextureFormat = GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
    private static final String resizeScaleFactorSettings = "emu.graphics.resizeScaleFactor";
    // sceDisplayModes enum
    public static final int PSP_DISPLAY_MODE_LCD = 0;
    public static final int PSP_DISPLAY_MODE_VESA1A = 0x1A;
    public static final int PSP_DISPLAY_MODE_PSEUDO_VGA = 0x60;
    // sceDisplaySetBufSync enum
    public static final int PSP_DISPLAY_SETBUF_IMMEDIATE = 0;
    public static final int PSP_DISPLAY_SETBUF_NEXTFRAME = 1;
    private static final float hCountPerVblank = 285.72f;
    private static final float FRAME_PER_SEC = 59.940060f;
    // current Rendering Engine
    private IRenderingEngine re;
    private IRenderingEngine reDisplay;
    private boolean startModules;
    private boolean isStarted;
    private int drawBuffer;
    private float[] drawBufferArray;
    private boolean resetDisplaySettings;
    private boolean resetGeTextures;

    // current display mode
    private int mode;
    // Resizing options
    private static float viewportResizeFilterScaleFactor = 1f;
    private static int viewportResizeFilterScaleFactorInt = 1;
    private boolean resizePending;
    // current framebuffer and GE settings
    private FrameBufferSettings fb;
    private FrameBufferSettings ge;
    private int sync;
    private boolean setGeBufCalledAtLeastOnce;
    public boolean gotBadGeBufParams;
    public boolean gotBadFbBufParams;
    protected boolean isFbShowing;
    private DisplayScreen displayScreen;

    // additional variables
    private boolean detailsDirty;
    private boolean displayDirty;
    private boolean geDirty;
    private long lastUpdate;
    private boolean initGLcalled;
    private String openGLversion;
    private boolean calledFromCommandLine;
    // Canvas fields
    private Buffer temp;
    private ByteBuffer tempByteBuffer;
    private int tempSize;
    private int canvasWidth;
    private int canvasHeight;
    private boolean createTex;
    private int texFb;
    private int resizedTexFb;
    private float texS;
    private float texT;
    private Robot captureRobot;
    private boolean wantScreenshot;
    // fps counter variables
    private long prevStatsTime;
    private long frameCount;
    private long paintFrameCount;
    private long prevFrameCount;
    private long prevPaintFrameCount;
    private long reportCount;
    private int vcount;
    private long lastVblankMicroTime;
    private DisplayVblankAction displayVblankAction;
    public DurationStatistics statistics;
    public DurationStatistics statisticsCopyGeToMemory;
    public DurationStatistics statisticsCopyMemoryToGe;
    // Async Display
    private AsyncDisplayThread asyncDisplayThread;
    private SoftwareRenderingDisplayThread softwareRenderingDisplayThread;
    private volatile boolean insideRendering;
    // VBLANK Multi.
    private List<WaitVblankInfo> waitingOnVblank;
    // Anti-alias samples.
    private int antiAliasSamplesNum;
    // Frame skipping
    private int desiredFps = 0;
    private int maxFramesSkippedInSequence = 3;
    private int framesSkippedInSequence;
    private LinkedList<Long> frameTimestamps = new LinkedList<Long>();
    private boolean skipNextFrameBufferSwitch;
    // Stencil copy
    private static final int[] stencilPixelMasks = new int[]{0, 0x7FFF, 0x0FFF, 0x00FFFFFF};
    private static final int[] stencilValueMasks = new int[]{0, 0x80, 0xF0, 0xFF};
    private static final int[] stencilValueShifts = new int[]{0, 8, 8, 24};

    private class OnlyGeSettingsListener extends AbstractBoolSettingsListener {

        @Override
        protected void settingsValueChanged(boolean value) {
            setOnlyGEGraphics(value);
        }
    }

    private class SoftwareRendererSettingsListener extends AbstractBoolSettingsListener {

        @Override
        protected void settingsValueChanged(boolean value) {
            setUseSoftwareRenderer(value);
        }
    }
    //do the same as above but for external render only

   private class ExternalSoftwareRendererSettingsListener extends AbstractBoolSettingsListener {

       @Override
        protected void settingsValueChanged(boolean value) {
           setUseSoftwareRenderer(value);
       }
    }


    private class SaveStencilToMemorySettingsListener extends AbstractBoolSettingsListener {

        @Override
        protected void settingsValueChanged(boolean value) {
            setSaveStencilToMemory(value);
        }
    }

    private static class WaitVblankInfo {

        public int threadId;
        public int unblockVcount;

        public WaitVblankInfo(int threadId, int unblockVcount) {
            this.threadId = threadId;
            this.unblockVcount = unblockVcount;
        }
    }

    private static abstract class AbstractDisplayThread extends Thread {

        private Semaphore displaySemaphore = new Semaphore(1);
        protected boolean run = true;

        public AbstractDisplayThread() {
            // Force the creation of a VideoEngine instance in this thread
            VideoEngine.getInstance();
        }

        @Override
        public void run() {
            while (run) {
                waitForDisplay();
                if (run) {
                    doDisplay();
                }
            }
        }

        protected abstract void doDisplay();

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

    private static class AsyncDisplayThread extends AbstractDisplayThread {

        @Override
        protected void doDisplay() {
            if (!Modules.sceDisplayModule.isOnlyGEGraphics() || VideoEngine.getInstance().hasDrawLists()) {
                Modules.sceDisplayModule.canvas.repaint();
            }
        }
    }

    private static class SoftwareRenderingDisplayThread extends AbstractDisplayThread {

        @Override
        protected void doDisplay() {
            if (VideoEngine.getInstance().hasDrawLists()) {
                IRenderingEngine re = Modules.sceDisplayModule.getRenderingEngine();

                if (re == null && !Screen.hasScreen()) {
            		re = RenderingEngineFactory.createRenderingEngine();
                    Modules.sceDisplayModule.setRenderingEngine(re);
                    VideoEngine.getInstance().start();
            	}

            	if (re != null) {
                    re.startDisplay();
                    VideoEngine.getInstance().update();
                    re.endDisplay();
                }
            }
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
        	boolean continueWait = sceDisplay.this.vcount < vcount;

        	if (!continueWait) {
        		ExternalGE.onDisplayStopWaitVblank();
        	}

        	return continueWait;
        }
    }

    private class VblankUnblockThreadAction extends UnblockThreadAction {
		public VblankUnblockThreadAction(int threadId) {
			super(threadId);
		}

		@Override
		public void execute() {
			ExternalGE.onDisplayStopWaitVblank();
			super.execute();
		}
    }

    private class AntiAliasSettingsListerner extends AbstractStringSettingsListener {

        private Pattern pattern = Pattern.compile("x(\\d+)", Pattern.CASE_INSENSITIVE);

        @Override
        protected void settingsValueChanged(String value) {
            int samples = 0;
            if (value != null) {
                Matcher matcher = pattern.matcher(value);
                if (matcher.matches()) {
                    samples = Integer.parseInt(matcher.group(1));
                }
            }
            setAntiAliasSamplesNum(samples);
        }
    }

    private class DisplaySettingsListener implements ISettingsListener {

        @Override
        public void settingsValueChanged(String option, String value) {
            if (isStarted) {
                resetDisplaySettings = true;
            }
        }
    }

    @Override
    public String getName() {
        return "sceDisplay";
    }

    public sceDisplay() throws LWJGLException {
        setSettingsListener("emu.graphics.antialias", new AntiAliasSettingsListerner());

        DisplaySettingsListener displaySettingsListener = new DisplaySettingsListener();
        setSettingsListener("emu.useVertexCache", displaySettingsListener);
        setSettingsListener("emu.useshaders", displaySettingsListener);
        setSettingsListener("emu.useGeometryShader", displaySettingsListener);
        setSettingsListener("emu.disableubo", displaySettingsListener);
        setSettingsListener("emu.enablevao", displaySettingsListener);
        setSettingsListener("emu.enablegetexture", displaySettingsListener);
        setSettingsListener("emu.enablenativeclut", displaySettingsListener);
        setSettingsListener("emu.enabledynamicshaders", displaySettingsListener);
        setSettingsListener("emu.enableshaderstenciltest", displaySettingsListener);
        setSettingsListener("emu.enableshadercolormask", displaySettingsListener);

    	displayScreen = new DisplayScreen();

        canvas = new AWTGLCanvas_sceDisplay();
        setScreenResolution(displayScreen.getWidth(), displayScreen.getHeight());

        // Remember the last window size only if not running in full screen
        if (!Emulator.getMainGUI().isFullScreen()) {
            setViewportResizeScaleFactor(Settings.getInstance().readFloat(resizeScaleFactorSettings, 1f));
        }

        texFb = -1;
        resizedTexFb = -1;
        startModules = false;
        isStarted = false;
        resizePending = false;
        tempSize = 0;

        fb = new FrameBufferSettings(START_VRAM, 512, Screen.width, Screen.height, TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888);
        ge = new FrameBufferSettings(fb);
    }

    public void setDesiredFPS(int desiredFPS) {
        this.desiredFps = desiredFPS;
    }

    public int getDesiredFPS() {
        return desiredFps;
    }

    public final void setScreenResolution(int width, int height) {
        canvasWidth = width;
        canvasHeight = height;
        canvas.setSize(width, height);
    }

    public float getViewportResizeScaleFactor() {
        return viewportResizeFilterScaleFactor;
    }

    public void setViewportResizeScaleFactor(int width, int height) {
        // Compute the scale factor in the horizontal and vertical directions
        float scaleWidth = ((float) width) / displayScreen.getWidth();
        float scaleHeight = ((float) height) / displayScreen.getHeight();

        // We are currently using only one scale factor to keep the PSP aspect ratio
        float scaleAspectRatio;
        if (Emulator.getMainGUI().isFullScreen()) {
            // In full screen mode, also keep the aspect ratio.
            // The best aspect ratio is when the horizontal or vertical dimension
            // is matching the screen size and the other dimension is less or equal
            // to the screen size.
            Dimension fullScreenDimension = MainGUI.getFullScreenDimension();
            if (fullScreenDimension.width == width && fullScreenDimension.height > height) {
                // Screen stretched to the full width
                scaleAspectRatio = scaleWidth;
            } else if (fullScreenDimension.height == height && fullScreenDimension.width > width) {
                // Screen stretched to the full height
                scaleAspectRatio = scaleHeight;
            } else {
                scaleAspectRatio = Math.min(scaleWidth, scaleHeight);
            }
        } else {
            scaleAspectRatio = (scaleWidth + scaleHeight) / 2;
        }
        setViewportResizeScaleFactor(scaleAspectRatio);

        resizePending = true;
    }

    private void forceSetViewportResizeScaleFactor(float viewportResizeFilterScaleFactor) {
		// Save the current window size only if not in full screen
		if (!Emulator.getMainGUI().isFullScreen()) {
			Settings.getInstance().writeFloat(resizeScaleFactorSettings, viewportResizeFilterScaleFactor);
		}

		// The GE has been resized, reset the GETextureManager at next paintGL
		resetGeTextures = true;

		sceDisplay.viewportResizeFilterScaleFactor = viewportResizeFilterScaleFactor;
		sceDisplay.viewportResizeFilterScaleFactorInt = Math.round((float) Math.ceil(viewportResizeFilterScaleFactor));

		Dimension size = new Dimension(getResizedWidth(displayScreen.getWidth()), getResizedHeight(displayScreen.getHeight()));

		// Resize the component while keeping the PSP aspect ratio
		canvas.setSize(size);

		// The preferred size is used when resizing the MainGUI
		canvas.setPreferredSize(size);

		if (Emulator.getMainGUI().isFullScreen()) {
			Emulator.getMainGUI().setFullScreenDisplaySize();
		}

		// Recreate the texture if the scaling factor has changed
		createTex = true;

		if (log.isDebugEnabled()) {
			log.debug(String.format("setViewportResizeScaleFactor resize=%f, size(%dx%d), canvas(%dx%d), location(%d,%d)", viewportResizeFilterScaleFactor, size.width, size.height, canvasWidth, canvasHeight, canvas.getLocation().x, canvas.getLocation().y));
		}
    }

    public void setViewportResizeScaleFactor(float viewportResizeFilterScaleFactor) {
    	if (viewportResizeFilterScaleFactor < 1) {
    		// Invalid value
    		return;
    	}

    	if (viewportResizeFilterScaleFactor != sceDisplay.viewportResizeFilterScaleFactor) {
    		forceSetViewportResizeScaleFactor(viewportResizeFilterScaleFactor);
    	}
    }

    public void updateDisplaySize() {
    	float scaleFactor = viewportResizeFilterScaleFactor;
    	setDisplayMinimumSize();
    	Emulator.getMainGUI().setDisplaySize(getResizedWidth(displayScreen.getWidth()), getResizedHeight(displayScreen.getHeight()));
		forceSetViewportResizeScaleFactor(scaleFactor);
    }

    public void setDisplayMinimumSize() {
		Emulator.getMainGUI().setDisplayMinimumSize(displayScreen.getWidth(), displayScreen.getHeight());
    }

    /**
     * Resize the given value according to the viewport resizing factor,
     * assuming it is a value along the X-Axis (e.g. "x" or "width" value).
     *
     * @param width value on the X-Axis to be resized
     * @return the resized value
     */
    public static int getResizedWidth(int width) {
        return Math.round(width * viewportResizeFilterScaleFactor);
    }

    public boolean isDisplaySwappedXY() {
    	return displayScreen.isSwappedXY();
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
        return widthPow2 * viewportResizeFilterScaleFactorInt;
    }

    /**
     * Resize the given value according to the viewport resizing factor,
     * assuming it is a value along the Y-Axis (e.g. "y" or "height" value).
     *
     * @param height value on the Y-Axis to be resized
     * @return the resized value
     */
    public static int getResizedHeight(int height) {
        return Math.round(height * viewportResizeFilterScaleFactor);
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
        return heightPow2 * viewportResizeFilterScaleFactorInt;
    }

    private void setAntiAliasSamplesNum(int samples) {
        antiAliasSamplesNum = samples;
    }

    @Override
    public void start() {
        statistics = new DurationStatistics("sceDisplay Statistics");
        statisticsCopyGeToMemory = new DurationStatistics("Copy GE to Memory");
        statisticsCopyMemoryToGe = new DurationStatistics("Copy Memory to GE");

        // Log debug information...
        if (log.isDebugEnabled()) {
            try {
                DisplayMode[] availableDisplayModes = Display.getAvailableDisplayModes();
                for (int i = 0; availableDisplayModes != null && i < availableDisplayModes.length; i++) {
                    log.debug(String.format("Available Display Mode #%d = %s", i, availableDisplayModes[i]));
                }
                log.debug(String.format("Desktop Display Mode = %s", Display.getDesktopDisplayMode()));
                log.debug(String.format("Current Display Mode = %s", Display.getDisplayMode()));
                log.debug(String.format("initGL called = %b, OpenGL Version = %s", initGLcalled, openGLversion));
            } catch (LWJGLException e) {
                log.error(e);
            }
        }

        if (!initGLcalled && !calledFromCommandLine) {
            // Some problem occurred during the OpenGL/LWJGL initialization...
            throw new RuntimeException("Jpcsp cannot display.\nThe cause could be that you are using an old graphic card driver (try to update it)\nor your display format is not compatible with Jpcsp (try to change your display format, Jpcsp requires 32 bit color depth)\nor the anti-aliasing settings is not supported by your display (leave the Jpcsp anti-aliasing to its default setting)");
        }

        // Reset the FB and GE settings only when not called from a syscall.
        // E.g. sceKernelLoadExec() is not clearing/resetting the display.
        if (!HLEModuleManager.getInstance().isStartFromSyscall()) {
            mode = 0;
            fb = new FrameBufferSettings(START_VRAM, 512, Screen.width, Screen.height, TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888);
            ge = new FrameBufferSettings(fb);
            sync = PSP_DISPLAY_SETBUF_IMMEDIATE;

            createTex = true;
        }

        detailsDirty = true;
        displayDirty = true;
        geDirty = false;

        isFbShowing = false;
        setGeBufCalledAtLeastOnce = false;
        gotBadGeBufParams = false;
        gotBadFbBufParams = false;

        prevStatsTime = 0;
        frameCount = 0;
        paintFrameCount = 0;
        prevFrameCount = 0;
        prevFrameCount = 0;
        reportCount = 0;
        insideRendering = false;

        vcount = 0;

        if (asyncDisplayThread == null) {
            asyncDisplayThread = new AsyncDisplayThread();
            asyncDisplayThread.setDaemon(true);
            asyncDisplayThread.setName("Async Display Thread");
            asyncDisplayThread.start();
        }

        if (displayVblankAction == null) {
            displayVblankAction = new DisplayVblankAction();
            IntrManager.getInstance().addVBlankAction(displayVblankAction);
        }

        waitingOnVblank = new LinkedList<WaitVblankInfo>();

        // The VideoEngine needs to be started when a valid GL is available.
        // Start the VideoEngine at the next display(GLAutoDrawable).
        startModules = true;
        re = null;
        reDisplay = null;
        resetDisplaySettings = false;

        saveGEToTexture = Settings.getInstance().readBool("emu.enablegetexture");
        if (saveGEToTexture) {
            log.info("Saving GE to Textures");
        }

        try {
            captureRobot = new Robot();
            captureRobot.setAutoDelay(0);
        } catch (Exception e) {
            // Ignore.
        }

        setSettingsListener("emu.onlyGEGraphics", new OnlyGeSettingsListener());
        if(Settings.getInstance().readBool("emu.useSoftwareRenderer"))
          setSettingsListener("emu.useSoftwareRenderer", new ExternalSoftwareRendererSettingsListener());
        if(Settings.getInstance().readBool("emu.useExternalSoftwareRenderer"))
            setSettingsListener("emu.useExternalSoftwareRenderer", new SoftwareRendererSettingsListener());

        setSettingsListener("emu.saveStencilToMemory", new SaveStencilToMemorySettingsListener());

        super.start();
    }

    @Override
    public void stop() {
        VideoEngine.getInstance().stop();
        if (asyncDisplayThread != null) {
            asyncDisplayThread.exit();
            asyncDisplayThread = null;
        }
        re = null;
        reDisplay = null;
        startModules = false;
        isStarted = false;

        super.stop();
    }

    public void exit() {
        if (statistics != null && DurationStatistics.collectStatistics) {
            log.info("----------------------------- sceDisplay exit -----------------------------");
            log.info(statistics.toString());
            log.info(statisticsCopyGeToMemory.toString());
            log.info(statisticsCopyMemoryToGe.toString());
        }
    }

    public void step(boolean immediately) {
        long now = System.currentTimeMillis();
        if (immediately || now - lastUpdate > 1000 / 60 || geDirty) {
            if (!isOnlyGEGraphics() || VideoEngine.getInstance().hasDrawLists()) {
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

    public final void write8(int rawAddress) {
        if (fb.isRawAddressInside(rawAddress)) {
            displayDirty = true;
        }
    }

    public final void write16(int rawAddress) {
        if (fb.isRawAddressInside(rawAddress)) {
            displayDirty = true;
        }
    }

    public final void write32(int rawAddress) {
        if (fb.isRawAddressInside(rawAddress)) {
            displayDirty = true;
        }
    }

    public final void write(int address) {
        if (fb.isAddressInside(address)) {
            displayDirty = true;
        }
    }

    public IRenderingEngine getRenderingEngine() {
        return re;
    }

    public void setRenderingEngine(IRenderingEngine re) {
    	this.re = re;
    }

    public void setGeDirty(boolean dirty) {
        geDirty = dirty;

        if (dirty && softwareRenderingDisplayThread != null) {
            // Start immediately the software rendering.
            // No need to wait for the OpenGL display call.
            softwareRenderingDisplayThread.display();
        }
    }

    public void hleDisplaySetGeMode(int width, int height) {
        if (width <= 0 || height <= 0) {
            log.warn(String.format("hleDisplaySetGeMode width=%d, height=%d bad params", width, height));
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleDisplaySetGeMode width=%d, height=%d", width, height));
            }
            ge.setDimension(width, height);
        }
    }

    public void hleDisplaySetGeBuf(int topaddr, int bufferwidth, int pixelformat, boolean copyGEToMemory, boolean forceLoadGEToScreen) {
        hleDisplaySetGeBuf(topaddr, bufferwidth, pixelformat, copyGEToMemory, forceLoadGEToScreen, ge.getWidth(), ge.getHeight());
    }

    public void hleDisplaySetGeBuf(int topaddr, int bufferwidth, int pixelformat, boolean copyGEToMemory, boolean forceLoadGEToScreen, int width, int height) {
        topaddr &= Memory.addressMask;
        // We can get the address relative to 0 or already relative to START_VRAM
        if (topaddr < MemoryMap.START_VRAM) {
            topaddr += MemoryMap.START_VRAM;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("hleDisplaySetGeBuf topaddr=0x%08X, bufferwidth=%d, pixelformat=%d, copyGE=%b, with=%d, height=%d", topaddr, bufferwidth, pixelformat, copyGEToMemory, width, height));
        }

        // Do not copy the GE to memory or reload it if we are using the software
        // renderer or skipping this frame.
        if (isUsingSoftwareRenderer() || VideoEngine.getInstance().isSkipThisFrame()) {
            copyGEToMemory = false;
            forceLoadGEToScreen = false;
        }

        if (topaddr == ge.getTopAddr() && bufferwidth == ge.getBufferWidth()
                && pixelformat == ge.getPixelFormat()
                && width == ge.getWidth() && height == ge.getHeight()) {

            // Nothing changed
            if (forceLoadGEToScreen) {
                loadGEToScreen();
            }

            return;
        }

        // The lower 2 bits of the bufferwidth are ignored.
        // E.g., the following bufferwidth values are valid: 120, 240, 480, 256, 512...
        bufferwidth = bufferwidth & ~0x3;

        if (topaddr < MemoryMap.START_VRAM || topaddr >= MemoryMap.END_VRAM
                || bufferwidth <= 0
                || pixelformat < 0 || pixelformat > 3
                || (sync != PSP_DISPLAY_SETBUF_IMMEDIATE && sync != PSP_DISPLAY_SETBUF_NEXTFRAME)) {
            // First time is usually initializing GE, so we can ignore it
            if (setGeBufCalledAtLeastOnce) {
                log.warn(String.format("hleDisplaySetGeBuf topaddr=0x%08X, bufferwidth=%d, pixelformat=%d bad params", topaddr, bufferwidth, pixelformat));
                gotBadGeBufParams = true;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("hleDisplaySetGeBuf topaddr=0x%08X, bufferwidth=%d, pixelformat=%d bad params", topaddr, bufferwidth, pixelformat));
                }
                setGeBufCalledAtLeastOnce = true;
            }

            return;
        }
        if (gotBadGeBufParams) {
            // print when we get good params after bad params
            gotBadGeBufParams = false;
            if (log.isInfoEnabled()) {
                log.info(String.format("hleDisplaySetGeBuf topaddr=0x%08X, bufferwidth=%d, pixelformat=%d OK", topaddr, bufferwidth, pixelformat));
            }
        }

        if (re.isVertexArrayAvailable()) {
            re.bindVertexArray(0);
        }

        // Always reload the GE memory to the screen,
        // if not rendering in software and not skipping this frame.
        boolean loadGEToScreen = !isUsingSoftwareRenderer() && !VideoEngine.getInstance().isSkipThisFrame();

        if (copyGEToMemory && (ge.getTopAddr() != topaddr || ge.getPixelFormat() != pixelformat)) {
            copyGeToMemory(false, false);
            loadGEToScreen = true;
        }

        ge = new FrameBufferSettings(topaddr, bufferwidth, width, height, pixelformat);

        // Tested on PSP:
        // The height of the buffer always matches the display height.
        // This data can be obtained from hleDisplaySetGeMode, since the width
        // represents the display width in pixels, and the height represents
        // the display height in lines.

        checkTemp();

        if (loadGEToScreen) {
            loadGEToScreen();

            if (State.captureGeNextFrame) {
                captureGeImage();
            }
        }

        setGeBufCalledAtLeastOnce = true;
    }

    public static int getPixelFormatBytes(int pixelformat) {
        return IRenderingEngine.sizeOfTextureType[pixelformat];
    }

    public boolean isGeAddress(int address) {
        return ge.isAddressInside(address);
    }

    public boolean isFbAddress(int address) {
        return fb.isAddressInside(address);
    }

    public boolean isOnlyGEGraphics() {
        return onlyGEGraphics;
    }

    public void setOnlyGEGraphics(boolean onlyGEGraphics) {
        this.onlyGEGraphics = onlyGEGraphics;
        log.info(String.format("Only GE Graphics: %b", onlyGEGraphics));
    }

    public boolean isSaveStencilToMemory() {
        return saveStencilToMemory;
    }

    public void setSaveStencilToMemory(boolean saveStencilToMemory) {
        this.saveStencilToMemory = saveStencilToMemory;
        log.info(String.format("Save Stencil To Memory: %b", saveStencilToMemory));
    }

    public void setUseSoftwareRenderer(boolean useSoftwareRenderer) {
        this.useSoftwareRenderer = useSoftwareRenderer;

        // Start/stop the software rendering display thread
        if (useSoftwareRenderer) {
            if (!Screen.hasScreen() && softwareRenderingDisplayThread == null) {
    			softwareRenderingDisplayThread = new SoftwareRenderingDisplayThread();
    			softwareRenderingDisplayThread.setDaemon(true);
    			softwareRenderingDisplayThread.setName("GUI");
    			softwareRenderingDisplayThread.start();
    			log.debug("Starting Software Rendering Display Thread");
            }
        } else {
            if (softwareRenderingDisplayThread != null) {
                log.debug("Stopping Software Rendering Display Thread");
                softwareRenderingDisplayThread.exit();
                softwareRenderingDisplayThread = null;
            }
        }

        if (isStarted) {
            resetDisplaySettings = true;
        }
    }

    public boolean isUsingSoftwareRenderer() {
        return useSoftwareRenderer;
    }

    public void rotate(int angleId) {
    	switch (angleId) {
    		case 0:
    			displayScreen = new DisplayScreenRotation90();
    			break;
    		case 1:
    			displayScreen = new DisplayScreenRotation270();
    			break;
    		case 2:
    			displayScreen = new DisplayScreenRotation180();
    			break;
    		case 3:
    			displayScreen = new DisplayScreenMirrorX(new DisplayScreen());
    			break;
    		case 4:
    			displayScreen = new DisplayScreen();
    			break;
    	}
    	updateDisplaySize();
    }

    public void saveScreen() {
        String fileFormat = "png";
        String fileName;
        for (int id = 1; true; id++) {
            fileName = String.format("%s-Shot-%d.%s", State.discId, id, fileFormat);
            if (!new File(fileName).exists()) {
                break;
            }
        }

        Rectangle rect = Emulator.getMainGUI().getCaptureRectangle();
        try {
            BufferedImage img = captureRobot.createScreenCapture(rect);
            ImageIO.write(img, fileFormat, new File(fileName));
            img.flush();
        } catch (IOException e) {
            log.error("Error saving screenshot", e);
        }

        wantScreenshot = false;
    }

    // For capture/replay
    public int getTopAddrFb() {
        return fb.getTopAddr();
    }

    public int getBufferWidthFb() {
        return fb.getBufferWidth();
    }

    public int getPixelFormatFb() {
        return fb.getPixelFormat();
    }

    public int getSync() {
        return sync;
    }

    public int getWidthFb() {
        return fb.getWidth();
    }

    public int getHeightFb() {
        return fb.getHeight();
    }

    public int getTopAddrGe() {
        return ge.getTopAddr();
    }

    public int getBufferWidthGe() {
        return ge.getBufferWidth();
    }

    public int getWidthGe() {
        return ge.getWidth();
    }

    public int getHeightGe() {
        return ge.getHeight();
    }

    public BufferInfo getBufferInfoGe() {
        return new BufferInfo(ge.getTopAddr(), ge.getBottomAddr(), ge.getWidth(), ge.getHeight(), ge.getBufferWidth(), ge.getPixelFormat());
    }

    public BufferInfo getBufferInfoFb() {
        return new BufferInfo(fb.getTopAddr(), fb.getBottomAddr(), fb.getWidth(), fb.getHeight(), fb.getBufferWidth(), fb.getPixelFormat());
    }

    public boolean getSaveGEToTexture() {
        return saveGEToTexture;
    }

    public int getCanvasWidth() {
        return canvasWidth;
    }

    public int getCanvasHeight() {
        return canvasHeight;
    }

    public void captureGeImage() {
        if (isUsingSoftwareRenderer()) {
            Buffer buffer = Memory.getInstance().getBuffer(ge.getTopAddr(), ge.getBufferWidth() * ge.getHeight() * getPixelFormatBytes(ge.getPixelFormat()));
            CaptureManager.captureImage(ge.getTopAddr(), 0, buffer, ge.getWidth(), ge.getHeight(), ge.getBufferWidth(), ge.getPixelFormat(), false, 0, false, false);
            return;
        }

        // Create a GE texture (the texture texFb might not have the right size)
        int texGe = re.genTexture();

        re.bindTexture(texGe);
        re.setTextureFormat(ge.getPixelFormat(), false);
        re.setTexImage(0,
                internalTextureFormat,
                getResizedWidthPow2(ge.getBufferWidth()),
                getResizedHeightPow2(Utilities.makePow2(ge.getHeight())),
                ge.getPixelFormat(),
                ge.getPixelFormat(),
                0, null);

        re.setTextureMipmapMinFilter(TFLT_NEAREST);
        re.setTextureMipmapMagFilter(TFLT_NEAREST);
        re.setTextureMipmapMinLevel(0);
        re.setTextureMipmapMaxLevel(0);
        re.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);
        re.setPixelStore(getResizedWidthPow2(ge.getBufferWidth()), getPixelFormatBytes(ge.getPixelFormat()));

        // Copy screen to the GE texture
        re.copyTexSubImage(0, 0, 0, 0, 0, getResizedWidth(Math.min(ge.getWidth(), ge.getBufferWidth())), getResizedHeight(ge.getHeight()));

        // Copy the GE texture into temp buffer
        temp.clear();
        re.getTexImage(0, ge.getPixelFormat(), ge.getPixelFormat(), temp);

        // Capture the GE image
        CaptureManager.captureImage(ge.getTopAddr(), 0, temp, getResizedWidth(ge.getWidth()), getResizedHeight(ge.getHeight()), getResizedWidthPow2(ge.getBufferWidth()), ge.getPixelFormat(), false, 0, true, false);

        // Delete the GE texture
        re.deleteTexture(texGe);
    }

    public void captureCurrentTexture(int address, int width, int height, int bufferWidth, int pixelFormat) {
        // Copy the texture into temp buffer
        re.setPixelStore(bufferWidth, getPixelFormatBytes(pixelFormat));
        temp.clear();
        re.getTexImage(0, pixelFormat, pixelFormat, temp);

        // Capture the image
        CaptureManager.captureImage(address, 0, temp, width, height, bufferWidth, pixelFormat, false, 0, true, false);
    }

    private void reportFPSStats() {
        long timeNow = System.currentTimeMillis();
        long realElapsedTime = timeNow - prevStatsTime;

        if (realElapsedTime > 1000L) {
            reportCount++;

            if (frameCount == prevFrameCount) {
                // If the application is not using a double-buffering technique
                // for the framebuffer display (i.e. if the application is not changing
                // the value of the framebuffer address), then use the number
                // of GE list executed to compute the FPS value.
                frameCount = paintFrameCount;
                prevFrameCount = prevPaintFrameCount;
            }

            int lastFPS = (int) (frameCount - prevFrameCount);
            double averageFPS = frameCount / (double) reportCount;
            prevFrameCount = frameCount;
            prevPaintFrameCount = paintFrameCount;
            prevStatsTime = timeNow;

            Emulator.setFpsTitle(String.format("FPS: %d, averageFPS: %.1f", lastFPS, averageFPS));
        }
    }

    private void loadGEToScreen() {
        if (VideoEngine.log.isDebugEnabled()) {
            VideoEngine.log.debug(String.format("Reloading GE Memory (0x%08X-0x%08X) to screen (%dx%d)", ge.getTopAddr(), ge.getBottomAddr(), ge.getWidth(), ge.getHeight()));
        }

        if (statisticsCopyMemoryToGe != null) {
            statisticsCopyMemoryToGe.start();
        }

        if (saveGEToTexture && !VideoEngine.getInstance().isVideoTexture(ge.getTopAddr())) {
            GETexture geTexture = GETextureManager.getInstance().getGETexture(re, ge.getTopAddr(), ge.getBufferWidth(), ge.getWidth(), ge.getHeight(), ge.getPixelFormat(), true);
            geTexture.copyTextureToScreen(re);
        } else {
            if (re.isVertexArrayAvailable()) {
                re.bindVertexArray(0);
            }

            // Set texFb as the current texture
            re.bindTexture(texFb);

            // Define the texture from the GE Memory
            re.setPixelStore(ge.getBufferWidth(), getPixelFormatBytes(ge.getPixelFormat()));
            int textureSize = ge.getBufferWidth() * ge.getHeight() * getPixelFormatBytes(ge.getPixelFormat());
            ge.getPixels().clear();
            re.setTexSubImage(0, 0, 0, ge.getBufferWidth(), ge.getHeight(), ge.getPixelFormat(), ge.getPixelFormat(), textureSize, ge.getPixels());

            // Draw the GE
            drawFrameBuffer(fb, false, true, ge.getBufferWidth(), ge.getPixelFormat(), ge.getWidth(), ge.getHeight());
        }

        if (statisticsCopyMemoryToGe != null) {
            statisticsCopyMemoryToGe.end();
        }
    }

    private void copyStencilToMemory() {
        if (ge.getPixelFormat() >= stencilPixelMasks.length) {
            log.warn(String.format("copyGeToMemory: unimplemented pixelformat %d for Stencil buffer copy", ge.getPixelFormat()));
            return;
        }
        if (stencilValueMasks[ge.getPixelFormat()] == 0) {
            // No stencil value for BGR5650, nothing to copy for the stencil
            return;
        }

        // Be careful to not overwrite parts of the GE memory used by the application for another purpose.
        VideoEngine videoEngine = VideoEngine.getInstance();
        int stencilWidth = Math.min(ge.getWidth(), ge.getBufferWidth());
        int stencilHeight = Math.min(ge.getHeight(), videoEngine.getMaxSpriteHeight());
        if (log.isDebugEnabled()) {
            log.debug(String.format("Copy stencil to GE: pixelFormat=%d, %dx%d, maxSprite=%dx%d", ge.getPixelFormat(), stencilWidth, stencilHeight, videoEngine.getMaxSpriteWidth(), videoEngine.getMaxSpriteHeight()));
        }

        int stencilBufferSize = stencilWidth * stencilHeight;
        tempByteBuffer.clear();
        re.setPixelStore(stencilWidth, 1);
        re.readStencil(0, 0, stencilWidth, stencilHeight, stencilBufferSize, tempByteBuffer);

        int bytesPerPixel = IRenderingEngine.sizeOfTextureType[ge.getPixelFormat()];
        IMemoryReaderWriter memoryReaderWriter = MemoryReaderWriter.getMemoryReaderWriter(ge.getTopAddr(), stencilHeight * ge.getBufferWidth() * bytesPerPixel, bytesPerPixel);
        tempByteBuffer.rewind();
        final int stencilPixelMask = stencilPixelMasks[ge.getPixelFormat()];
        final int stencilValueMask = stencilValueMasks[ge.getPixelFormat()];
        final int stencilValueShift = stencilValueShifts[ge.getPixelFormat()];
        for (int y = 0; y < stencilHeight; y++) {
            // The stencil buffer is stored upside-down by OpenGL
            tempByteBuffer.position((stencilHeight - y - 1) * stencilWidth);

            for (int x = 0; x < stencilWidth; x++) {
                int pixel = memoryReaderWriter.readCurrent();
                int stencilValue = tempByteBuffer.get() & stencilValueMask;
                pixel = (pixel & stencilPixelMask) | (stencilValue << stencilValueShift);
                memoryReaderWriter.writeNext(pixel);
            }

            if (stencilWidth < ge.getBufferWidth()) {
                memoryReaderWriter.skip(ge.getBufferWidth() - stencilWidth);
            }
        }
        memoryReaderWriter.flush();

        if (GEProfiler.isProfilerEnabled()) {
            GEProfiler.copyStencilToMemory();
        }
    }

    public void copyGeToMemory(boolean preserveScreen, boolean forceCopyToMemory) {
        if (isUsingSoftwareRenderer()) {
            // GE is already in memory when using the software renderer
            return;
        }

        if (VideoEngine.log.isDebugEnabled()) {
            VideoEngine.log.debug(String.format("Copy GE Screen to Memory 0x%08X-0x%08X", ge.getTopAddr(), ge.getBottomAddr()));
        }

        if (statisticsCopyGeToMemory != null) {
            statisticsCopyGeToMemory.start();
        }

        if (saveGEToTexture && !VideoEngine.getInstance().isVideoTexture(ge.getTopAddr())) {
            GETexture geTexture = GETextureManager.getInstance().getGETexture(re, ge.getTopAddr(), ge.getBufferWidth(), ge.getWidth(), ge.getHeight(), ge.getPixelFormat(), true);
            geTexture.copyScreenToTexture(re);
        } else {
        	forceCopyToMemory = true;
        }

        if (forceCopyToMemory) {
            // Set texFb as the current texture
            re.bindTexture(resizedTexFb);
            re.setTextureFormat(ge.getPixelFormat(), false);

            // Copy screen to the current texture
            re.copyTexSubImage(0, 0, 0, 0, 0, getResizedWidth(Math.min(ge.getBufferWidth(), ge.getWidth())), getResizedHeight(ge.getHeight()));

            // Re-render GE/current texture upside down
            drawFrameBuffer(fb, true, true, ge.getBufferWidth(), ge.getPixelFormat(), ge.getWidth(), ge.getHeight());

            copyScreenToPixels(ge.getPixels(), ge.getBufferWidth(), ge.getPixelFormat(), ge.getWidth(), ge.getHeight());

            if (saveStencilToMemory) {
                copyStencilToMemory();
            }

            if (preserveScreen) {
                // Redraw the screen
                re.bindTexture(resizedTexFb);
                drawFrameBuffer(fb, true, false, ge.getBufferWidth(), ge.getPixelFormat(), ge.getWidth(), ge.getHeight());
            }
        }

        if (statisticsCopyGeToMemory != null) {
            statisticsCopyGeToMemory.end();
        }

        if (GEProfiler.isProfilerEnabled()) {
            GEProfiler.copyGeToMemory();
        }
    }

    /**
     * @param keepOriginalSize : true = draw as psp size false = draw as window
     * size
     *
     */
    private void drawFrameBuffer(FrameBufferSettings fb, boolean keepOriginalSize, boolean invert, int bufferwidth, int pixelformat, int width, int height) {
        if (log.isDebugEnabled()) {
        	log.debug(String.format("drawFrameBuffer fb=%s, keepOriginalSize=%b, invert=%b, bufferWidth=%d, pixelFormat=%d, width=%d, height=%d, %s", fb, keepOriginalSize, invert, bufferwidth, pixelformat, width, height, displayScreen));
        }

        reDisplay.startDirectRendering(true, false, true, true, !invert, width, height);
        if (keepOriginalSize) {
            reDisplay.setViewport(0, 0, width, height);
        } else {
            reDisplay.setViewport(0, 0, getResizedWidth(width), getResizedHeight(height));
        }

        reDisplay.setTextureFormat(pixelformat, false);

        float scale = 1f;
        if (keepOriginalSize) {
        	// When keeping the original size, we still have to adjust the size of the texture mapping.
        	// E.g. when the screen has been resized to 576x326 (resizeScaleFactor=1.2),
        	// the texture has been created with a size 1024x1024 and the following texture
        	// coordinates have to used:
        	//     (576/1024, 326/1024),
        	// while texS==480/512 and texT==272/512
        	scale = (float) getResizedHeight(height) / (float) getResizedHeightPow2(makePow2(height));
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

        int bufferSizeInFloats = i;
        IREBufferManager bufferManager = reDisplay.getBufferManager();
        ByteBuffer byteBuffer = bufferManager.getBuffer(drawBuffer);
        byteBuffer.clear();
        byteBuffer.asFloatBuffer().put(drawBufferArray, 0, bufferSizeInFloats);

        if (reDisplay.isVertexArrayAvailable()) {
            reDisplay.bindVertexArray(0);
        }
        reDisplay.setVertexInfo(null, false, false, true, IRenderingEngine.RE_QUADS);
        reDisplay.enableClientState(IRenderingEngine.RE_TEXTURE);
        reDisplay.disableClientState(IRenderingEngine.RE_COLOR);
        reDisplay.disableClientState(IRenderingEngine.RE_NORMAL);
        reDisplay.enableClientState(IRenderingEngine.RE_VERTEX);
        bufferManager.setTexCoordPointer(drawBuffer, 2, IRenderingEngine.RE_FLOAT, 4 * SIZEOF_FLOAT, 0);
        bufferManager.setVertexPointer(drawBuffer, 2, IRenderingEngine.RE_FLOAT, 4 * SIZEOF_FLOAT, 2 * SIZEOF_FLOAT);
        bufferManager.setBufferData(IRenderingEngine.RE_ARRAY_BUFFER, drawBuffer, bufferSizeInFloats * SIZEOF_FLOAT, byteBuffer, IRenderingEngine.RE_DYNAMIC_DRAW);
        reDisplay.drawArrays(IRenderingEngine.RE_QUADS, 0, 4);

        reDisplay.endDirectRendering();
    }

    private void drawFrameBufferFromMemory(FrameBufferSettings fb) {
        fb.getPixels().clear();
        reDisplay.bindTexture(texFb);
        reDisplay.setTextureFormat(fb.getPixelFormat(), false);
        reDisplay.setPixelStore(fb.getBufferWidth(), getPixelFormatBytes(fb.getPixelFormat()));
        int textureSize = fb.getBufferWidth() * fb.getHeight() * getPixelFormatBytes(fb.getPixelFormat());
        reDisplay.setTexSubImage(0,
                0, 0, fb.getBufferWidth(), fb.getHeight(),
                fb.getPixelFormat(),
                fb.getPixelFormat(),
                textureSize, fb.getPixels());

        drawFrameBuffer(fb, false, true, fb.getBufferWidth(), fb.getPixelFormat(), displayScreen.getWidth(fb), displayScreen.getHeight(fb));
    }

    private void copyBufferByLines(IntBuffer dstBuffer, IntBuffer srcBuffer, int dstBufferWidth, int srcBufferWidth, int pixelFormat, int width, int height) {
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

    private void copyScreenToPixels(Buffer pixels, int bufferWidth, int pixelFormat, int width, int height) {
        // Set texFb as the current texture
        reDisplay.bindTexture(texFb);
        reDisplay.setTextureFormat(fb.getPixelFormat(), false);

        reDisplay.setPixelStore(bufferWidth, getPixelFormatBytes(pixelFormat));

        // Copy screen to the current texture
        reDisplay.copyTexSubImage(0, 0, 0, 0, 0, Math.min(bufferWidth, width), height);

        // Copy the current texture into memory
        Buffer buffer = (pixels.capacity() >= temp.capacity() ? pixels : temp);
        buffer.clear();
        reDisplay.getTexImage(0, pixelFormat, pixelFormat, buffer);

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

    public int hleDisplayWaitVblankStart(int cycles, boolean doCallbacks) {
        if (cycles <= 0) {
            return SceKernelErrors.ERROR_INVALID_VALUE;
        }

        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        SceKernelThreadInfo thread = threadMan.getCurrentThread();
        int threadId = threadMan.getCurrentThreadID();

        int lastWaitVblank = thread.displayLastWaitVcount;
        int unblockVcount = lastWaitVblank + cycles;
        if (unblockVcount <= vcount) {
            // This thread has just to wait for the next VBLANK.
            // Add a Vblank action to unblock the thread
            UnblockThreadAction vblankAction = new VblankUnblockThreadAction(threadId);
            IntrManager.getInstance().addVBlankActionOnce(vblankAction);
            thread.displayLastWaitVcount = vcount + 1;
        } else {
            // This thread has to wait for multiple VBLANK's
            WaitVblankInfo waitVblankInfo = new WaitVblankInfo(threadId, unblockVcount);
            waitingOnVblank.add(waitVblankInfo);
        }

        // Block the current thread.
        threadMan.hleBlockCurrentThread(SceKernelThreadInfo.JPCSP_WAIT_DISPLAY_VBLANK, unblockVcount, doCallbacks, null, new VblankWaitStateChecker(unblockVcount));

    	ExternalGE.onDisplayStartWaitVblank();

        return 0;
    }

    private void hleVblankStart() {
        lastVblankMicroTime = Emulator.getClock().microTime();
        // Vcount increases at each VBLANK.
        vcount++;

    	ExternalGE.onDisplayVblank();

        // Check the threads waiting for VBLANK (multi).
        if (!waitingOnVblank.isEmpty()) {
            for (ListIterator<WaitVblankInfo> lit = waitingOnVblank.listIterator(); lit.hasNext();) {
                WaitVblankInfo waitVblankInfo = lit.next();
                if (waitVblankInfo.unblockVcount <= vcount) {
                    ThreadManForUser threadMan = Modules.ThreadManForUserModule;
                    SceKernelThreadInfo thread = threadMan.getThreadById(waitVblankInfo.threadId);
                    if (thread != null) {
                		ExternalGE.onDisplayStopWaitVblank();
                        thread.displayLastWaitVcount = vcount;
                        threadMan.hleUnblockThread(waitVblankInfo.threadId);
                    }
                    lit.remove();
                }
            }
        }
    }

    private boolean isVblank() {
        // Test result: isVblank == true during 4.39% of the time
        // -> Vblank takes 731.5 micros at each vblank interrupt
        long nowMicroTime = Emulator.getClock().microTime();
        long microTimeSinceLastVblank = nowMicroTime - lastVblankMicroTime;

        return (microTimeSinceLastVblank <= 731);
    }

    private int getCurrentHcount() {
        // Test result: currentHcount is 0 at the start of a Vblank and increases
        // up to 285 just before the next Vblank.
        long nowMicroTime = Emulator.getClock().microTime();
        long microTimeSinceLastVblank = nowMicroTime - lastVblankMicroTime;

        float vblankStep = microTimeSinceLastVblank / 16666.6666f;
        if (vblankStep > 1) {
            vblankStep = 1;
        }

        return (int) (vblankStep * hCountPerVblank);
    }

    public int getVcount() {
        return vcount;
    }

    private int createTexture(int textureId, boolean isResized) {
        if (textureId != -1) {
            reDisplay.deleteTexture(textureId);
        }
        textureId = reDisplay.genTexture();

        reDisplay.bindTexture(textureId);
        reDisplay.setTextureFormat(fb.getPixelFormat(), false);

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
        reDisplay.setTexImage(0,
                internalTextureFormat,
                isResized ? getResizedWidthPow2(fb.getBufferWidth()) : fb.getBufferWidth(),
                isResized ? getResizedHeightPow2(Utilities.makePow2(fb.getHeight())) : Utilities.makePow2(fb.getHeight()),
                fb.getPixelFormat(),
                fb.getPixelFormat(),
                0, null);
        reDisplay.setTextureMipmapMinFilter(GeCommands.TFLT_NEAREST);
        reDisplay.setTextureMipmapMagFilter(GeCommands.TFLT_NEAREST);
        reDisplay.setTextureMipmapMinLevel(0);
        reDisplay.setTextureMipmapMaxLevel(0);
        reDisplay.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);

        return textureId;
    }

    private void checkTemp() {
        // Buffer large enough to store the complete FB or GE texture
        int sizeInBytes = getResizedWidthPow2(Math.max(fb.getBufferWidth(), ge.getBufferWidth()))
                * getResizedHeightPow2(Utilities.makePow2(Math.max(fb.getHeight(), ge.getHeight())))
                * getPixelFormatBytes(Math.max(fb.getPixelFormat(), ge.getPixelFormat()));

        if (sizeInBytes > tempSize) {
            tempByteBuffer = ByteBuffer.allocateDirect(sizeInBytes).order(ByteOrder.LITTLE_ENDIAN);

            if (Memory.getInstance().getMainMemoryByteBuffer() instanceof IntBuffer) {
                temp = tempByteBuffer.asIntBuffer();
            } else {
                temp = tempByteBuffer;
            }
            tempSize = sizeInBytes;
        }
    }

    public void setCalledFromCommandLine() {
        calledFromCommandLine = true;
    }

    public void takeScreenshot() {
        wantScreenshot = true;
    }

    public boolean isInsideRendering() {
        return insideRendering;
    }

    public void setInsideRendering(boolean insideRendering) {
        this.insideRendering = insideRendering;
    }

    /**
     * If the display is currently rendering to the given address, wait for the
     * rendering completion. Otherwise, return immediately.
     *
     * @param address the address to be checked.
     */
    public void waitForRenderingCompletion(int address) {
        while (isInsideRendering() && isGeAddress(address)) {
            // Sleep 10 microseconds for polling...
            Utilities.sleep(10);
        }
    }

    @HLEFunction(nid = 0x0E20F177, version = 150, checkInsideInterrupt = true)
    public int sceDisplaySetMode(int displayMode, int displayWidth, int displayHeight) {
        if (displayWidth <= 0 || displayHeight <= 0) {
            return SceKernelErrors.ERROR_INVALID_SIZE;
        }

        if (displayMode != PSP_DISPLAY_MODE_LCD) {
            return SceKernelErrors.ERROR_INVALID_MODE;
        }

        mode = displayMode;
        fb.setDimension(displayWidth, displayHeight);

        detailsDirty = true;

        return 0;
    }

    @HLEFunction(nid = 0xDEA197D4, version = 150)
    public int sceDisplayGetMode(TPointer32 modeAddr, TPointer32 widthAddr, TPointer32 heightAddr) {
        modeAddr.setValue(mode);
        widthAddr.setValue(fb.getWidth());
        heightAddr.setValue(fb.getHeight());

        return 0;
    }

    @HLEFunction(nid = 0xDBA6C4C4, version = 150)
    public float sceDisplayGetFramePerSec() {
        return FRAME_PER_SEC;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7ED59BC4, version = 150, checkInsideInterrupt = true)
    public int sceDisplaySetHoldMode(int holdMode) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA544C486, version = 150, checkInsideInterrupt = true)
    public int sceDisplaySetResumeMode(int resumeMode) {
        return 0;
    }

    @HLEFunction(nid = 0x289D82FE, version = 150)
    public int sceDisplaySetFrameBuf(@CanBeNull TPointer topaddr, int bufferwidth, int pixelformat, int syncType) {
        return hleDisplaySetFrameBuf(topaddr.getAddress(), bufferwidth, pixelformat, syncType);
    }

    private int hleDisplaySetFrameBufError(int topaddr, int bufferwidth, int pixelformat, int syncType, int error, String errorString) {
        log.warn(String.format("sceDisplaySetFrameBuf topaddr=0x%08X, bufferwidth=%d, pixelformat=%d, syncType=%d %s: returning 0x%08X", topaddr, bufferwidth, pixelformat, syncType, errorString, error));
        gotBadFbBufParams = true;
        return error;
    }

    public int hleDisplaySetFrameBuf(int topaddr, int bufferwidth, int pixelformat, int syncType) {
        // The PSP is performing the following parameter checks in this sequence

        if (syncType != PSP_DISPLAY_SETBUF_IMMEDIATE && syncType != PSP_DISPLAY_SETBUF_NEXTFRAME) {
            return hleDisplaySetFrameBufError(topaddr, bufferwidth, pixelformat, syncType, ERROR_INVALID_MODE, "bad syncType");
        }

        if ((topaddr & 0xF) != 0) {
            return hleDisplaySetFrameBufError(topaddr, bufferwidth, pixelformat, syncType, ERROR_INVALID_POINTER, "bad topaddr");
        }

        if (topaddr != 0 && !Memory.isRAM(topaddr) && !Memory.isVRAM(topaddr)) {
            return hleDisplaySetFrameBufError(topaddr, bufferwidth, pixelformat, syncType, ERROR_INVALID_POINTER, "bad topaddr");
        }

        if ((bufferwidth & 0x3F) != 0) {
            return hleDisplaySetFrameBufError(topaddr, bufferwidth, pixelformat, syncType, ERROR_INVALID_SIZE, "bad bufferwidth");
        }

        // bufferwidth can only be 0 when topaddr is NULL
        if (bufferwidth == 0 && topaddr != 0) {
            return hleDisplaySetFrameBufError(topaddr, bufferwidth, pixelformat, syncType, ERROR_INVALID_SIZE, "bad bufferwidth");
        }

        if (pixelformat < 0 || pixelformat > TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888) {
            return hleDisplaySetFrameBufError(topaddr, bufferwidth, pixelformat, syncType, ERROR_INVALID_FORMAT, "bad pixelformat");
        }

        if (topaddr == 0) {
            // If topaddr is NULL, the PSP's screen will be displayed as fully black
            // as the output is blocked. Under these circumstances, bufferwidth can be 0.
            log.info(String.format("sceDisplaySetFrameBuf topaddr=0x%08X, bufferwidth=%d, pixelformat=%d, syncType=%d (blocking display output)", topaddr, bufferwidth, pixelformat, syncType));
            isFbShowing = false;
            gotBadFbBufParams = true;
            return 0;
        }

        if (gotBadFbBufParams) {
            gotBadFbBufParams = false;
            log.info(String.format("sceDisplaySetFrameBuf topaddr=0x%08X, bufferwidth=%d, pixelformat=%d, syncType=%d ok", topaddr, bufferwidth, pixelformat, syncType));
        }

        if (topaddr == fb.getTopAddr() && bufferwidth == fb.getBufferWidth() && pixelformat == fb.getPixelFormat() && syncType == sync) {
            // No FB parameter changed, nothing to do...
            return 0;
        }

        if (topaddr != fb.getTopAddr()) {
            // New frame counting for FPS
            frameCount++;
        }

        // Keep track of how many frames have been skipped in sequence
        // (i.e. since the last rendering).
        if (skipNextFrameBufferSwitch) {
            framesSkippedInSequence++;
        } else {
            framesSkippedInSequence = 0;
        }

        boolean skipThisFrame = false;
        if (desiredFps > 0) {
            // Remember the time stamps of the frames displayed during the last second.
            long currentFrameTimestamp = Emulator.getClock().currentTimeMillis();
            frameTimestamps.addLast(currentFrameTimestamp);
            // Remove the time stamps older than 1 second
            while (currentFrameTimestamp - frameTimestamps.getFirst().longValue() > 1000L) {
                frameTimestamps.removeFirst();
            }

            // The current FPS is the number of frames displayed during the last second.
            int currentFps = frameTimestamps.size();

            // Skip the rendering of the next frame if we are below the desired FPS
            // and if we have not already skipped too many frames since the last rendering.
            if (currentFps < desiredFps && framesSkippedInSequence < maxFramesSkippedInSequence) {
                skipThisFrame = true;
            }
        }
        VideoEngine.getInstance().setSkipThisFrame(skipThisFrame);

        if (skipNextFrameBufferSwitch) {
            // The rendering of the previous frame has been skipped.
            // Reuse the frame buffer of the current frame to avoid flickering.
            if (topaddr != fb.getTopAddr()) {
                Memory.getInstance().memcpy(topaddr, fb.getTopAddr(), fb.getBottomAddr() - fb.getTopAddr());
            }
            skipNextFrameBufferSwitch = false;
        }

        if (pixelformat != fb.getPixelFormat() || bufferwidth != fb.getBufferWidth() || makePow2(fb.getHeight()) != makePow2(fb.getHeight())) {
            createTex = true;
        }

        fb = new FrameBufferSettings(topaddr, bufferwidth, fb.getWidth(), fb.getHeight(), pixelformat);
        sync = syncType;

        texS = (float) fb.getWidth() / (float) bufferwidth;
        texT = (float) fb.getHeight() / (float) makePow2(fb.getHeight());
        displayScreen.update();

        detailsDirty = true;
        isFbShowing = true;

        if (State.captureGeNextFrame && CaptureManager.hasListExecuted()) {
            State.captureGeNextFrame = false;
            CaptureManager.captureFrameBufDetails();
            CaptureManager.endCapture();
        }

        VideoEngine.getInstance().hleSetFrameBuf(fb.getTopAddr(), fb.getBufferWidth(), fb.getPixelFormat());

        return 0;
    }

    /**
     * @param topaddrAddr -
     * @param bufferwidthAddr -
     * @param pixelformatAddr -
     * @param syncType - 0 or 1. All other value than 1 is interpreted as 0.
     * @return
     */
    @HLEFunction(nid = 0xEEDA2E54, version = 150)
    public int sceDisplayGetFrameBuf(TPointer32 topaddrAddr, TPointer32 bufferwidthAddr, TPointer32 pixelformatAddr, int syncType) {
        topaddrAddr.setValue(fb.getTopAddr());
        bufferwidthAddr.setValue(fb.getBufferWidth());
        pixelformatAddr.setValue(fb.getPixelFormat());

        return 0;
    }

    @HLEFunction(nid = 0xB4F378FA, version = 150)
    public boolean sceDisplayIsForeground() {
        return isFbShowing;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x31C4BAA8, version = 150)
    public int sceDisplayGetBrightness(int leveladdr, int unkaddr) {
        return 0;
    }

    @HLEFunction(nid = 0x9C6EAAD7, version = 150)
    public int sceDisplayGetVcount() {
        // 60 units per second
        return vcount;
    }

    @HLEFunction(nid = 0x4D4E10EC, version = 150)
    public boolean sceDisplayIsVblank() {
        boolean isVblank = isVblank();

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceDisplayIsVblank returns %b", isVblank));
        }

        return isVblank;
    }

    @HLEFunction(nid = 0x36CDFADE, version = 150, checkInsideInterrupt = true)
    public int sceDisplayWaitVblank() {
        if (!isVblank()) {
            sceDisplayWaitVblankStart();
        }
        return 0;
    }

    @HLEFunction(nid = 0x8EB9EC49, version = 150, checkInsideInterrupt = true)
    public int sceDisplayWaitVblankCB() {
        if (!isVblank()) {
            sceDisplayWaitVblankStartCB();
        }
        return 0;
    }

    @HLEFunction(nid = 0x984C27E7, version = 150, checkInsideInterrupt = true)
    public int sceDisplayWaitVblankStart() {
        return hleDisplayWaitVblankStart(1, false);
    }

    @HLEFunction(nid = 0x46F186C3, version = 150, checkInsideInterrupt = true)
    public int sceDisplayWaitVblankStartCB() {
        return hleDisplayWaitVblankStart(1, true);
    }

    @HLEFunction(nid = 0x773DD3A3, version = 150)
    public int sceDisplayGetCurrentHcount() {
        int currentHcount = getCurrentHcount();

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceDisplayGetCurrentHcount returning %d", currentHcount));
        }

        return currentHcount;
    }

    @HLEFunction(nid = 0x210EAB3A, version = 150)
    public int sceDisplayGetAccumulatedHcount() {
        // The accumulatedHcount is the currentHcount plus the sum of the Hcounts
        // from all the previous vblanks (vcount * number of Hcounts per Vblank).
        int currentHcount = getCurrentHcount();
        int accumulatedHcount = currentHcount + (int) (vcount * hCountPerVblank);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceDisplayGetAccumulatedHcount returning %d (currentHcount=%d)", accumulatedHcount, currentHcount));
        }

        return accumulatedHcount;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA83EF139, version = 150)
    public int sceDisplayAdjustAccumulatedHcount() {
        return 0;
    }

    public static class BufferInfo {

        public int topAddr;
        public int bottomAddr;
        public int width;
        public int height;
        public int bufferWidth;
        public int pixelFormat;

        public BufferInfo(int topAddr, int bottomAddr, int width, int height, int bufferWidth, int pixelFormat) {
            this.topAddr = topAddr;
            this.bottomAddr = bottomAddr;
            this.width = width;
            this.height = height;
            this.bufferWidth = bufferWidth;
            this.pixelFormat = pixelFormat;
        }
    }

    protected class DisplayScreen {
    	private float[] values;

    	public DisplayScreen() {
    		update();
    	}

    	public void update() {
    		int[] indices = getIndices();
    		if (indices == null) {
    			return;
    		}
    		float[] baseValues = new float[] { 0f, 0f, texS, 0f, 0f, texT, texS, texT };
    		values = new float[baseValues.length];
    		for (int i = 0; i < values.length; i++) {
    			values[i] = baseValues[indices[i]];
    		}
    	}

    	protected int[] getIndices() {
    		return new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };
    	}

    	protected boolean isSwappedXY() {
    		return false;
    	}

    	protected int getWidth(int width, int height) {
    		return isSwappedXY() ? height : width;
    	}

    	protected int getHeight(int width, int height) {
    		return isSwappedXY() ? width : height;
    	}

    	public int getWidth() {
    		return getWidth(Screen.width, Screen.height);
    	}

    	public int getHeight() {
    		return getHeight(Screen.width, Screen.height);
    	}

    	public int getWidth(FrameBufferSettings fb) {
    		return getWidth(fb.getWidth(), fb.getHeight());
    	}

    	public int getHeight(FrameBufferSettings fb) {
    		return getHeight(fb.getWidth(), fb.getHeight());
    	}

    	public float getTextureUpperLeftS() {
    		return values[0];
    	}

    	public float getTextureUpperLeftT() {
    		return values[1];
    	}

    	public float getTextureUpperRightS() {
    		return values[2];
    	}

    	public float getTextureUpperRightT() {
    		return values[3];
    	}

    	public float getTextureLowerLeftS() {
    		return values[4];
    	}

    	public float getTextureLowerLeftT() {
    		return values[5];
    	}

    	public float getTextureLowerRightS() {
    		return values[6];
    	}

    	public float getTextureLowerRightT() {
    		return values[7];
    	}

		@Override
		public String toString() {
			return String.format("DisplayScreen [%f, %f, %f, %f, %f, %f, %f, %f, %b]", values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], isSwappedXY());
		}
    }

    protected class DisplayScreenRotation90 extends DisplayScreen {
		@Override
		protected int[] getIndices() {
			return new int[] { 4, 5, 0, 1, 6, 7, 2, 3 };
		}

		@Override
		protected boolean isSwappedXY() {
			return true;
		}
    }

    protected class DisplayScreenRotation180 extends DisplayScreen {
		@Override
		protected int[] getIndices() {
			return new int[] { 6, 7, 4, 5, 2, 3, 0, 1 };
		}
    }

    protected class DisplayScreenRotation270 extends DisplayScreen {
		@Override
		protected int[] getIndices() {
			return new int[] { 2, 3, 6, 7, 0, 1, 4, 5 };
		}

		@Override
		protected boolean isSwappedXY() {
			return true;
		}
    }

    protected class DisplayScreenMirrorX extends DisplayScreen {
    	private DisplayScreen displayScreen;

    	public DisplayScreenMirrorX(DisplayScreen displayScreen) {
    		this.displayScreen = displayScreen;
    		update();
    	}

    	@Override
		protected int[] getIndices() {
    		if (displayScreen == null) {
    			return null;
    		}
    		int[] i = displayScreen.getIndices();
			return new int[] { i[2], i[3], i[0], i[1], i[6], i[7], i[4], i[5] };
		}

		@Override
		protected boolean isSwappedXY() {
			return displayScreen.isSwappedXY();
		}
    }

    protected class DisplayScreenMirrorY extends DisplayScreen {
    	private DisplayScreen displayScreen;

    	public DisplayScreenMirrorY(DisplayScreen displayScreen) {
    		this.displayScreen = displayScreen;
    	}

		@Override
		protected int[] getIndices() {
    		int[] i = displayScreen.getIndices();
			return new int[] { i[4], i[5], i[6], i[7], i[0], i[1], i[2], i[3] };
		}

		@Override
		protected boolean isSwappedXY() {
			return displayScreen.isSwappedXY();
		}
    }
}
