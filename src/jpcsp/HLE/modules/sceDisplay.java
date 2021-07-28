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
package jpcsp.HLE.modules;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_FORMAT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_MODE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_POINTER;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_INVALID_SIZE;
import static jpcsp.MemoryMap.START_VRAM;
import static jpcsp.graphics.GeCommands.TFLT_NEAREST;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.graphics.GeCommands.TWRAP_WRAP_MODE_CLAMP;
import static jpcsp.graphics.VideoEngine.maxDrawingBufferWidth;
import static jpcsp.graphics.VideoEngine.maxDrawingHeight;
import static jpcsp.graphics.VideoEngine.maxDrawingWidth;
import static jpcsp.graphics.VideoEngineUtilities.drawFrameBuffer;
import static jpcsp.graphics.VideoEngineUtilities.drawFrameBufferFromMemory;
import static jpcsp.graphics.VideoEngineUtilities.getPixelFormatBytes;
import static jpcsp.graphics.VideoEngineUtilities.getResizedHeight;
import static jpcsp.graphics.VideoEngineUtilities.getResizedHeightPow2;
import static jpcsp.graphics.VideoEngineUtilities.getResizedWidth;
import static jpcsp.graphics.VideoEngineUtilities.getResizedWidthPow2;
import static jpcsp.graphics.VideoEngineUtilities.getTexturePixelFormat;
import static jpcsp.graphics.VideoEngineUtilities.getViewportResizeScaleFactor;
import static jpcsp.graphics.VideoEngineUtilities.getViewportResizeScaleFactorInt;
import static jpcsp.graphics.VideoEngineUtilities.internalTextureFormat;
import static jpcsp.util.Utilities.makePow2;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEModuleManager;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.awt.Dimension;
import java.awt.Graphics;
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
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.graphics.DisplayScreen;
import jpcsp.graphics.FrameBufferSettings;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VertexCache;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.VideoEngineThread;
import jpcsp.graphics.VideoEngineUtilities;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.RenderingEngineFactory;
import jpcsp.graphics.RE.RenderingEngineLwjgl;
import jpcsp.graphics.RE.externalge.ExternalGE;
import jpcsp.graphics.capture.CaptureManager;
import jpcsp.graphics.textures.GETexture;
import jpcsp.graphics.textures.GETextureManager;
import jpcsp.graphics.textures.TextureCache;
import jpcsp.hardware.Screen;
import jpcsp.scheduler.UnblockThreadAction;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.AbstractStringSettingsListener;
import jpcsp.settings.ISettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;
import org.lwjgl.opengl.awt.GLData;
import org.lwjgl.opengl.awt.PlatformLinuxGLCanvas;
import org.lwjgl.opengl.awt.PlatformWin32GLCanvas;
import org.lwjgl.opengl.awt.AWTGLCanvas;

public class sceDisplay extends HLEModule {
    public static Logger log = Modules.getLogger("sceDisplay");
    private static final boolean multiThreadingLockDisplay = false;

    public class AWTGLCanvas_sceDisplay extends AWTGLCanvas {
		private static final long serialVersionUID = -3808789665048696700L;

		public AWTGLCanvas_sceDisplay(GLData glData) {
            super(glData);
        }

		public long getDisplayWindow() {
			if (platformCanvas instanceof PlatformWin32GLCanvas) {
				return ((PlatformWin32GLCanvas) platformCanvas).hwnd;
			}
			if (platformCanvas instanceof PlatformLinuxGLCanvas) {
				return ((PlatformLinuxGLCanvas) platformCanvas).display;
			}
			return 0L;
		}

		public long getDisplayDrawable() {
			if (platformCanvas instanceof PlatformLinuxGLCanvas) {
				return ((PlatformLinuxGLCanvas) platformCanvas).drawable;
			}
			return 0L;
		}

		@Override
		public void paint(Graphics g) {
			render();
		}

		@Override
        public void paintGL() {
            VideoEngine videoEngine = VideoEngine.getInstance();

    		if (log.isTraceEnabled()) {
                log.trace(String.format("paintGL resize=%f, size(%dx%d), canvas(%dx%d), location(%d,%d), createTex=%b", getViewportResizeScaleFactor(), canvas.getSize().width, canvas.getSize().height, canvasWidth, canvasHeight, canvas.getLocation().x, canvas.getLocation().y, createTex));
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
                	ExternalGE.init();
                    re = RenderingEngineFactory.createRenderingEngine();
                    if (isUsingSoftwareRenderer()) {
                        reDisplay = RenderingEngineFactory.createRenderingEngineForDisplay();
                        reDisplay.setGeContext(videoEngine.getContext());
                    } else if (ExternalGE.isActive()) {
                        reDisplay = re;
                    } else {
                        reDisplay = RenderingEngineFactory.createRenderingEngineForDisplay();
                        reDisplay.setGeContext(videoEngine.getContext());
                        // Share the buffer manager between the 2 rendering engines
                        reDisplay.setBufferManager(re.getBufferManager());
                    }
                } else {
                    re = RenderingEngineFactory.createInitialRenderingEngine();
                    reDisplay = re;
                }
            }

            lockDisplay();

            if (startModules) {
                saveGEToTexture = Settings.getInstance().readBool("emu.enablegetexture");
                if (saveGEToTexture) {
                    GETextureManager.getInstance().reset(reDisplay);
                }
                videoEngine.start();
                startModules = false;
                if (saveGEToTexture && !re.isFramebufferObjectAvailable()) {
                    saveGEToTexture = false;
                    log.warn("Saving GE to Textures has been automatically disabled: FBO is not supported by this OpenGL version");
                }
                isStarted = true;
            }

            if (!isStarted) {
                reDisplay.clear(0.0f, 0.0f, 0.0f, 0.0f);
                unlockDisplay();
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
            	if (ExternalGE.getScreenScale() <= 1) {
            		drawFrameBufferFromMemory(reDisplay, currentFb, texFb);
            	} else {
				    ByteBuffer scaledScreen = ExternalGE.getScaledScreen(fb.getTopAddr(), fb.getBufferWidth(), fb.getHeight(), fb.getPixelFormat());
				    if (scaledScreen == null) {
	            		drawFrameBufferFromMemory(reDisplay, currentFb, texFb);
				    } else {
	            		int screenScale = ExternalGE.getScreenScale();
					    fb.getPixels().clear();
					    reDisplay.bindTexture(resizedTexFb);
					    reDisplay.setTextureFormat(fb.getPixelFormat(), false);
					    reDisplay.setPixelStore(fb.getBufferWidth() * screenScale, getPixelFormatBytes(fb.getPixelFormat()));
					    reDisplay.setTexSubImage(0,
					            0, 0, fb.getBufferWidth() * screenScale, fb.getHeight() * screenScale,
					            fb.getPixelFormat(),
					            fb.getPixelFormat(),
					            scaledScreen.remaining(), scaledScreen);

					    drawFrameBuffer(reDisplay, false, true, fb.getBufferWidth(), fb.getPixelFormat(), displayScreen.getWidth(fb), displayScreen.getHeight(fb));
				    }
            	}
            	reDisplay.endDisplay();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceDisplay.paintGL - ExternalGE - end display - isFbShowing=%b", isFbShowing));
                }
            } else if (isUsingSoftwareRenderer()) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceDisplay.paintGL - software - rendering the FB 0x%08X", currentFb.getTopAddr()));
                }

                reDisplay.startDisplay();
        		drawFrameBufferFromMemory(reDisplay, currentFb, texFb);
                reDisplay.endDisplay();

                if (log.isDebugEnabled()) {
                    log.debug("sceDisplay.paintGL - software - end display");
                }
            } else if (isOnlyGEGraphics()) {
            	// Nothing to do
            } else if (VideoEngineThread.isActive()) {
                if (log.isDebugEnabled()) {
                    log.debug("sceDisplay.paintGL - start display with Video Engine Thread");
                }
                reDisplay.startDisplay();

                // Render the FB
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceDisplay.paintGL - rendering the FB 0x%08X", currentFb.getTopAddr()));
                }
                if (saveGEToTexture && !videoEngine.isVideoTexture(fb.getTopAddr())) {
                    GETexture geTexture = GETextureManager.getInstance().getGETexture(reDisplay, currentFb.getTopAddr(), currentFb.getBufferWidth(), currentFb.getWidth(), currentFb.getHeight(), currentFb.getPixelFormat(), true);
                    geTexture.copyTextureToScreen(reDisplay);
                } else {
            		drawFrameBufferFromMemory(reDisplay, currentFb, texFb);
                }

                reDisplay.endDisplay();

                if (log.isDebugEnabled()) {
                    log.debug("sceDisplay.paintGL - end display");
                }
            } else {
                // Hardware rendering:
                // 1) GE list is rendered to the screen
                // 2) the result of the rendering is stored into the GE frame buffer
                // 3) the active FB frame buffer is reloaded from memory to the screen for final display
                if (log.isDebugEnabled()) {
                    log.debug("sceDisplay.paintGL - start display without Video Engine Thread");
                }

                re.startDisplay();
                videoEngine.update();

                // Render the FB
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceDisplay.paintGL - rendering the FB 0x%08X", currentFb.getTopAddr()));
                }
                if (saveGEToTexture && !videoEngine.isVideoTexture(fb.getTopAddr())) {
                    GETexture geTexture = GETextureManager.getInstance().getGETexture(re, currentFb.getTopAddr(), currentFb.getBufferWidth(), currentFb.getWidth(), currentFb.getHeight(), currentFb.getPixelFormat(), true);
                    geTexture.copyTextureToScreen(re);
                } else {
            		drawFrameBufferFromMemory(re, currentFb, texFb);
                }

                re.endDisplay();

                if (log.isDebugEnabled()) {
                    log.debug("sceDisplay.paintGL - end display");
                }
            }

            if (!isFbShowing) {
                reDisplay.clear(0.0f, 0.0f, 0.0f, 0.0f);
            }

            setInsideRendering(false);

            // Perform OpenGL double buffering
            if (doSwapBuffers) {
                paintFrameCount++;
                canvas.swapBuffers();
            }

            // Update the current FPS every second
            reportFPSStats();

            if (statistics != null) {
                statistics.end();
            }

            for (IAction action: displayActions) {
            	action.execute();
            }

            for (IAction action: displayActionsOnce) {
            	action.execute();
            }
            displayActionsOnce.clear();

            unlockDisplay();
        }

        @Override
        public void initGL() {
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
            setViewportResizeScaleFactor(width, height);
            super.setBounds(x, y, width, height);
        }
    }

    protected AWTGLCanvas_sceDisplay canvas;

    public AWTGLCanvas_sceDisplay getCanvas() {
        return canvas;
    }

    private boolean onlyGEGraphics = false;
    private boolean saveGEToTexture = false;
    private boolean useSoftwareRenderer = false;
    private boolean saveStencilToMemory = false;
    private static final boolean useDebugGL = false;
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
    private boolean resetDisplaySettings;
    private boolean resetGeTextures;

    // current display mode
    private int mode;
    // Resizing options
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
    private volatile boolean doneCopyGeToMemory;
    // Canvas fields
    private Buffer temp;
    private ByteBuffer tempByteBuffer;
    private int[] tempIntArray;
    private int tempSize;
    private int canvasWidth;
    private int canvasHeight;
    private boolean createTex;
    private int texFb;
    private int resizedTexFb;
    private float texS;
    private float texT;
    private Robot captureRobot;
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
    // Mpeg audio hack
    private int framePerSecFactor;
    // Display actions
    private List<IAction> displayActions = new LinkedList<IAction>();
    private List<IAction> displayActionsOnce = new LinkedList<IAction>();
    // For multi-threading rendering
    private Semaphore lockDisplay = new Semaphore(1);

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
			// Check that the thread is still waiting on the VBLANK.
			// The thread could have been resumed earlier through the VblankWaitStateChecker.
			boolean unblock = true;
			SceKernelThreadInfo threadInfo = getThreadInfo();
			if (threadInfo != null) {
				if (threadInfo.waitType != SceKernelThreadInfo.JPCSP_WAIT_DISPLAY_VBLANK) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("VblankUnblockThreadAction not unblocking %s", threadInfo));
					}
					unblock = false;
				}
			}

			if (unblock) {
				ExternalGE.onDisplayStopWaitVblank();
				super.execute();
			}
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

    public sceDisplay() {
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

        canvas = new AWTGLCanvas_sceDisplay(createGLData());
        setScreenResolution(displayScreen.getWidth(), displayScreen.getHeight());

        // Remember the last window size only if not running in full screen
        if (Emulator.getMainGUI() != null && !Emulator.getMainGUI().isFullScreen()) {
            setViewportResizeScaleFactor(Settings.getInstance().readFloat(resizeScaleFactorSettings, 1f));
        }

        texFb = -1;
        resizedTexFb = -1;
        startModules = false;
        isStarted = false;
        resizePending = false;
        tempSize = 0;

        fb = new FrameBufferSettings(START_VRAM, maxDrawingBufferWidth, maxDrawingWidth, maxDrawingHeight, TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888);
        ge = new FrameBufferSettings(fb);
    }

    public void lockDisplay() {
    	if (multiThreadingLockDisplay) {
	    	try {
	    		log.debug("lockDisplay");
	    		lockDisplay.acquire();
	    		log.debug("lockDisplay acquired");
	    	} catch (InterruptedException e) {
	    		log.error("lockDisplay", e);
	    	}
    	}
    }

    public void unlockDisplay() {
    	if (multiThreadingLockDisplay) {
    		log.debug("unlockDisplay");
    		lockDisplay.release();
    	}
    }

    private GLData createGLData() {
		GLData glData = new GLData();
		glData.redSize = 8;
		glData.blueSize = 8;
		glData.greenSize = 8;
		glData.alphaSize = 8;
		glData.stencilSize = 8;
		glData.samples = antiAliasSamplesNum;
		glData.debug = useDebugGL;
		glData.swapInterval = 0;
		return glData;
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

    public void setViewportResizeScaleFactor(int width, int height) {
        // Compute the scale factor in the horizontal and vertical directions
        float scaleWidth = ((float) width) / displayScreen.getWidth();
        float scaleHeight = ((float) height) / displayScreen.getHeight();

        // We are currently using only one scale factor to keep the PSP aspect ratio
        float scaleAspectRatio;
        if (Emulator.getMainGUI() != null && Emulator.getMainGUI().isFullScreen()) {
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

    public void forceSetViewportResizeScaleFactor(float viewportResizeFilterScaleFactor) {
		// Save the current window size only if not in full screen
		if (!Emulator.getMainGUI().isFullScreen()) {
			Settings.getInstance().writeFloat(resizeScaleFactorSettings, viewportResizeFilterScaleFactor);
		}

		// The GE has been resized, reset the GETextureManager at next paintGL
		resetGeTextures = true;

		VideoEngineUtilities.setViewportResizeScaleFactor(viewportResizeFilterScaleFactor);

		Dimension size = new Dimension(getResizedWidth(displayScreen.getWidth()), getResizedHeight(displayScreen.getHeight()));

		// Resize the component while keeping the PSP aspect ratio
		canvas.setSize(size);

		// The preferred size is used when resizing the MainGUI
		canvas.setPreferredSize(size);

		if (Emulator.getMainGUI().isFullScreen()) {
			Emulator.getMainGUI().setFullScreenDisplaySize();
		}

		// Recreate the texture if the scale factor has changed
		createTex = true;

		if (ExternalGE.isActive()) {
			ExternalGE.setScreenScale(getViewportResizeScaleFactorInt());
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("setViewportResizeScaleFactor resize=%f, size(%dx%d), canvas(%dx%d), location(%d,%d)", viewportResizeFilterScaleFactor, size.width, size.height, canvasWidth, canvasHeight, canvas.getLocation().x, canvas.getLocation().y));
		}
    }

    public void setViewportResizeScaleFactor(float viewportResizeFilterScaleFactor) {
    	if (viewportResizeFilterScaleFactor < 1) {
    		// Invalid value
    		return;
    	}

    	if (viewportResizeFilterScaleFactor != getViewportResizeScaleFactor()) {
    		forceSetViewportResizeScaleFactor(viewportResizeFilterScaleFactor);
    	}
    }

    public boolean isDisplaySwappedXY() {
    	return displayScreen.isSwappedXY();
    }

    private void setAntiAliasSamplesNum(int samples) {
        antiAliasSamplesNum = samples;
    }

    public void setFramePerSecFactor(int framePerSecFactor) {
    	if (log.isInfoEnabled()) {
    		log.info(String.format("setFramePerSecFactor %d", framePerSecFactor));
    	}
    	this.framePerSecFactor = framePerSecFactor;
    }

    @Override
    public void start() {
        statistics = new DurationStatistics("sceDisplay Statistics");
        VideoEngineUtilities.start();

        // Log debug information...
        if (log.isDebugEnabled()) {
//            DisplayMode[] availableDisplayModes = Display.getAvailableDisplayModes();
//            for (int i = 0; availableDisplayModes != null && i < availableDisplayModes.length; i++) {
//                log.debug(String.format("Available Display Mode #%d = %s", i, availableDisplayModes[i]));
//            }
//            log.debug(String.format("Desktop Display Mode = %s", Display.getDesktopDisplayMode()));
//            log.debug(String.format("Current Display Mode = %s", Display.getDisplayMode()));
            log.debug(String.format("initGL called = %b, OpenGL Version = %s", initGLcalled, openGLversion));
        }

        if (!initGLcalled && !calledFromCommandLine) {
            // Some problem occurred during the OpenGL/LWJGL initialization...
            throw new RuntimeException("Jpcsp cannot display.\nThe cause could be that you are using an old graphic card driver (try to update it)\nor your display format is not compatible with Jpcsp (try to change your display format, Jpcsp requires 32 bit color depth)\nor the anti-aliasing settings is not supported by your display (leave the Jpcsp anti-aliasing to its default setting)");
        }

        // Reset the FB and GE settings only when not called from a syscall.
        // E.g. sceKernelLoadExec() is not clearing/resetting the display.
        if (!HLEModuleManager.getInstance().isStartFromSyscall()) {
            mode = 0;
            fb = new FrameBufferSettings(START_VRAM, maxDrawingBufferWidth, maxDrawingWidth, maxDrawingHeight, TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888);
            ge = new FrameBufferSettings(fb);
            sync = PSP_DISPLAY_SETBUF_IMMEDIATE;

            texS = (float) fb.getWidth() / (float) maxDrawingBufferWidth;
            texT = (float) fb.getHeight() / (float) makePow2(fb.getHeight());
            displayScreen.update(texS, texT);

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
        prevPaintFrameCount = 0;
        reportCount = 0;
        insideRendering = false;
        framePerSecFactor = 1;

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
        setSettingsListener("emu.useSoftwareRenderer", new SoftwareRendererSettingsListener());
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
            VideoEngineUtilities.exit();
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

    public void clear() {
    	// Clear the whole VRAM
    	getMemory().memset(MemoryMap.START_VRAM, (byte) 0, MemoryMap.SIZE_VRAM);
        setGeDirty(true);
        step(true);
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

    public final void write(int rawAddress) {
        if (fb.isAddressInside(rawAddress)) {
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

        if (dirty) {
	        if (softwareRenderingDisplayThread != null) {
	            // Start immediately the software rendering.
	            // No need to wait for the OpenGL display call.
	            softwareRenderingDisplayThread.display();
	        } else {
	        	VideoEngine.getInstance().onUpdateDrawList();
	        }
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
        if (log.isDebugEnabled()) {
            log.debug(String.format("hleDisplaySetGeBuf topaddr=0x%08X, bufferwidth=%d, pixelformat=%d, copyGE=%b, with=%d, height=%d", topaddr, bufferwidth, pixelformat, copyGEToMemory, width, height));
        }

        // Do not copy the GE to memory or reload it if we are using the software
        // renderer or skipping this frame.
        if (isUsingSoftwareRenderer() || VideoEngine.getInstance().isSkipThisFrame()) {
            copyGEToMemory = false;
            forceLoadGEToScreen = false;
        }

        // The lower 2 bits of the bufferwidth are ignored.
        // E.g., the following bufferwidth values are valid: 120, 240, 480, 256, 512...
        bufferwidth = bufferwidth & ~0x3;

        // The lower 3 bits of FBP are ignored and the upper 8 bits are forced to VRAM.
        topaddr = (topaddr & 0x00FFFFF0) | MemoryMap.START_VRAM;

        if (topaddr == ge.getTopAddr() && bufferwidth == ge.getBufferWidth()
                && pixelformat == ge.getPixelFormat()
                && width == ge.getWidth() && height == ge.getHeight()) {

            // Nothing changed
            if (forceLoadGEToScreen) {
                VideoEngineUtilities.loadGEToScreen(re);
            }

            return;
        }

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
            if (log.isDebugEnabled()) {
                log.debug(String.format("hleDisplaySetGeBuf topaddr=0x%08X, bufferwidth=%d, pixelformat=%d OK", topaddr, bufferwidth, pixelformat));
            }
        }

        if (re.isVertexArrayAvailable()) {
            re.bindVertexArray(0);
        }

        // Always reload the GE memory to the screen,
        // if not rendering in software and not skipping this frame.
        boolean loadGEToScreen = !isUsingSoftwareRenderer() && !VideoEngine.getInstance().isSkipThisFrame();

        if (copyGEToMemory && (ge.getTopAddr() != topaddr || ge.getPixelFormat() != pixelformat)) {
            VideoEngineUtilities.copyGeToMemory(re, false, false);
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
            VideoEngineUtilities.loadGEToScreen(re);

            if (State.captureGeNextFrame) {
                captureGeImage();
            }
        }

        setGeBufCalledAtLeastOnce = true;
    }

    public boolean isGeAddress(int address) {
    	if (ExternalGE.isActive()) {
    		return ExternalGE.isGeAddress(address);
    	}
        return ge.isAddressInside(address);
    }

    public boolean isFbAddress(int address) {
        return fb.isAddressInside(address);
    }

    public boolean isOnlyGEGraphics() {
    	// "Only GE Graphics" makes only sense when the ExternalGE is not active
        return onlyGEGraphics && !ExternalGE.isActive();
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
    			displayScreen = new DisplayScreen.DisplayScreenRotation90();
    			break;
    		case 1:
    			displayScreen = new DisplayScreen.DisplayScreenRotation270();
    			break;
    		case 2:
    			displayScreen = new DisplayScreen.DisplayScreenRotation180();
    			break;
    		case 3:
    			displayScreen = new DisplayScreen.DisplayScreenMirrorX(new DisplayScreen());
    			break;
    		case 4:
    			displayScreen = new DisplayScreen();
    			break;
    	}
    	VideoEngineUtilities.updateDisplaySize();
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

    public int getBottomAddrGe() {
        return ge.getBottomAddr();
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

    public int getPixelFormatGe() {
        return ge.getPixelFormat();
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

    public Buffer getPixelsGe() {
    	return ge.getPixels();
    }

    public int getSizeGe() {
    	return ge.getSize();
    }

    public Buffer getPixelsGe(int topAddr) {
    	return ge.getPixels(topAddr);
    }

    public DisplayScreen getDisplayScreen() {
    	return displayScreen;
    }

    public float getTexS() {
    	return texS;
    }

    public float getTexT() {
    	return texT;
    }

    public ByteBuffer getTempByteBuffer() {
    	return tempByteBuffer;
    }

    public Buffer getTempBuffer() {
    	return temp;
    }

    public void captureGeImage() {
        if (isUsingSoftwareRenderer()) {
            Buffer buffer = Memory.getInstance().getBuffer(ge.getTopAddr(), ge.getBufferWidth() * ge.getHeight() * getPixelFormatBytes(ge.getPixelFormat()));
            CaptureManager.captureImage(ge.getTopAddr(), 0, buffer, ge.getWidth(), ge.getHeight(), ge.getBufferWidth(), ge.getPixelFormat(), false, 0, false, false);
            return;
        }

        // Create a GE texture (the texture texFb might not have the right size)
        int texGe = re.genTexture();

        int texturePixelFormat = getTexturePixelFormat(ge.getPixelFormat());
        re.bindTexture(texGe);
        re.setTextureFormat(ge.getPixelFormat(), false);
        re.setTexImage(0,
                internalTextureFormat,
                getResizedWidthPow2(ge.getBufferWidth()),
                getResizedHeightPow2(Utilities.makePow2(ge.getHeight())),
                texturePixelFormat,
                texturePixelFormat,
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
        re.getTexImage(0, texturePixelFormat, texturePixelFormat, temp);

        // Capture the GE image
        CaptureManager.captureImage(ge.getTopAddr(), 0, temp, getResizedWidth(ge.getWidth()), getResizedHeight(ge.getHeight()), getResizedWidthPow2(ge.getBufferWidth()), texturePixelFormat, false, 0, true, false);

        // Delete the GE texture
        re.deleteTexture(texGe);
    }

    private void convertABGRtoARGB(int[] abgr, int imageSize, boolean needAlpha) {
    	if (needAlpha) {
	    	for (int i = 0; i < imageSize; i++) {
	    		abgr[i] = Utilities.convertABGRtoARGB(abgr[i]);
	    	}
    	} else {
	    	for (int i = 0; i < imageSize; i++) {
	    		abgr[i] = Utilities.convertABGRtoARGB(abgr[i]) & 0x00FFFFFF;
	    	}
    	}
    }

    public BufferedImage getCurrentDisplayAsBufferedImage(boolean needAlpha) {
    	BufferedImage image = null;
    	int[] abgr = tempIntArray;
    	int width = fb.getWidth();
    	int height = fb.getHeight();
    	int bufferWidth = fb.getBufferWidth();
    	int pixelFormat = fb.getPixelFormat();
    	if (isUsingSoftwareRenderer()) {
	    	int imageSize = bufferWidth * height;

	    	Buffer buffer = Memory.getInstance().getBuffer(fb.getTopAddr(), imageSize * getPixelFormatBytes(pixelFormat));
            if (buffer instanceof IntBuffer) {
    	    	image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            	((IntBuffer) buffer).get(abgr, 0, imageSize);
		    	convertABGRtoARGB(abgr, imageSize, needAlpha);
            } else {
            	// TODO Implement getCurrentDisplayAsBufferedImage for the software renderer
            	log.warn("sceDisplay.getCurrentDisplayAsBufferedImage not yet implemented for the software renderer");
            }
    	} else {
	    	int lineWidth = getResizedWidth(Math.min(width, bufferWidth));
	    	width = getResizedWidth(width);
	    	height = getResizedHeight(height);
	    	bufferWidth = getResizedWidthPow2(bufferWidth);
	    	int imageSize = bufferWidth * height;
	    	image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

	        if (ExternalGE.isActive()) {
			    ByteBuffer scaledScreen = ExternalGE.getScaledScreen(fb.getTopAddr(), fb.getBufferWidth(), fb.getHeight(), pixelFormat);
			    if (scaledScreen == null) {
			    	Buffer buffer = Memory.getInstance().getBuffer(fb.getTopAddr(), imageSize * getPixelFormatBytes(pixelFormat));
		            if (buffer instanceof IntBuffer) {
		            	((IntBuffer) buffer).get(abgr, 0, imageSize);
				    	convertABGRtoARGB(abgr, imageSize, needAlpha);
		            }
			    } else {
			    	scaledScreen.asIntBuffer().get(abgr, 0, imageSize);
			    	for (int i = 0; i < imageSize; i++) {
			    		abgr[i] = abgr[i] >>> 8;
			    	}
			    }
	        } else {
		        // Create a GE texture (the texture texFb might not have the right size)
		        int texGe = re.genTexture();

		        re.bindTexture(texGe);
		        re.setTextureFormat(fb.getPixelFormat(), false);
		        re.setTexImage(0,
		                internalTextureFormat,
		                bufferWidth,
		                getResizedHeightPow2(Utilities.makePow2(fb.getHeight())),
		                pixelFormat,
		                pixelFormat,
		                0, null);

		        re.setTextureMipmapMinFilter(TFLT_NEAREST);
		        re.setTextureMipmapMagFilter(TFLT_NEAREST);
		        re.setTextureMipmapMinLevel(0);
		        re.setTextureMipmapMaxLevel(0);
		        re.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);
		        re.setPixelStore(bufferWidth, getPixelFormatBytes(pixelFormat));

		        // Copy screen to the GE texture
		        re.copyTexSubImage(0, 0, 0, 0, 0, lineWidth, height);

		        // Copy the GE texture into temp buffer
		        temp.clear();
		        re.getTexImage(0, pixelFormat, pixelFormat, temp);

		        // Delete the GE texture
		        re.deleteTexture(texGe);

		    	IntBuffer intBuffer = tempByteBuffer.asIntBuffer();
		    	// The image is flipped vertically
		    	for (int y = 0; y < height; y++) {
		    		intBuffer.get(abgr, (height - 1 - y) * bufferWidth, bufferWidth);
		    	}

		    	convertABGRtoARGB(abgr, imageSize, needAlpha);
	        }
    	}

    	image.setRGB(0, 0, Math.min(bufferWidth, width), height, abgr, 0, bufferWidth);

    	return image;
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

    /**
     * Copy the GE at from given address to memory.
     * This is only required when saving the GE to textures.
     * 
     * @param geTopAddress the GE address that need to be saved to memory
     */
    public void copyGeToMemory(int geTopAddress) {
    	if (isUsingSoftwareRenderer() || ExternalGE.isActive()) {
            // GE is already in memory when using the internal/external software renderer
    		return;
    	}
    	if (!saveGEToTexture) {
    		// Copying the GE to memory is only necessary when saving the GE to textures
    		return;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("copyGeToMemory starting with geTopAddress=0x%08X", geTopAddress));
    	}

    	doneCopyGeToMemory = false;
    	addDisplayActionOce(new CopyGeToMemoryAction(geTopAddress));

    	geDirty = true;
    	step(true);

    	// Poll completion of copyGeToMemory action
    	while (!doneCopyGeToMemory) {
    		Utilities.sleep(1, 0);
    	}
    	doneCopyGeToMemory = false;

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("copyGeToMemory done with geTopAddress=0x%08X", geTopAddress));
    	}
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
                getTexturePixelFormat(getPixelFormatFb()),
                getTexturePixelFormat(getPixelFormatFb()),
                0, null);
        reDisplay.setTextureMipmapMinFilter(GeCommands.TFLT_NEAREST);
        reDisplay.setTextureMipmapMagFilter(GeCommands.TFLT_NEAREST);
        reDisplay.setTextureMipmapMinLevel(0);
        reDisplay.setTextureMipmapMaxLevel(0);
        reDisplay.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);

        return textureId;
    }

    private void checkTemp() {
    	int bytesPerPixel = getPixelFormatBytes(Math.max(fb.getPixelFormat(), ge.getPixelFormat()));
        // Buffer large enough to store the complete FB or GE texture
        int sizeInBytes = getResizedWidthPow2(Math.max(fb.getBufferWidth(), ge.getBufferWidth()))
                * getResizedHeightPow2(Utilities.makePow2(Math.max(fb.getHeight(), ge.getHeight())))
                * bytesPerPixel;

        if (sizeInBytes > tempSize) {
            tempByteBuffer = ByteBuffer.allocateDirect(sizeInBytes).order(ByteOrder.LITTLE_ENDIAN);

            if (Memory.getInstance().getMainMemoryByteBuffer() instanceof IntBuffer) {
                temp = tempByteBuffer.asIntBuffer();
            } else {
                temp = tempByteBuffer;
            }

            tempIntArray = new int[sizeInBytes / bytesPerPixel];

            tempSize = sizeInBytes;
        }
    }

    public void setCalledFromCommandLine() {
        calledFromCommandLine = true;
    }

    public void takeScreenshot() {
    	addDisplayActionOce(new ScreenshotAction());
    }

    public boolean isInsideRendering() {
    	if (ExternalGE.isActive()) {
    		if (ExternalGE.isInsideRendering()) {
    			return true;
    		}
    	} else {
    		if (insideRendering) {
    			PspGeList currentList = VideoEngine.getInstance().getCurrentList();
    			if (currentList != null && currentList.isStalledAtStart()) {
    				// We are not really rendering when stalling at the start of the list
					return false;
    			}
    		}
    	}

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
    	int countWaitingOnStall = 0;
        while (isInsideRendering() && isGeAddress(address)) {
        	// Do not wait too long when the VideoEngine is also waiting on a stalled list...
        	if (VideoEngine.getInstance().isWaitingOnStall()) {
        		countWaitingOnStall++;
        		if (countWaitingOnStall > 10) {
        			break;
        		}
        	} else {
        		countWaitingOnStall = 0;
        	}

        	// Sleep 10 microseconds for polling...
            Utilities.sleep(10);
        }
    }

    public void addDisplayAction(IAction action) {
    	displayActions.add(action);
    }

    public boolean removeDisplayAction(IAction action) {
    	return displayActions.remove(action);
    }

    public void addDisplayActionOce(IAction action) {
    	displayActionsOnce.add(action);
    }

    public boolean removeDisplayActionOce(IAction action) {
    	return displayActionsOnce.remove(action);
    }

    @HLEFunction(nid = 0x0E20F177, version = 150, checkInsideInterrupt = true)
    public int sceDisplaySetMode(int displayMode, int displayWidth, int displayHeight) {
        if (displayWidth <= 0 || displayHeight <= 0 || (displayWidth & 0x7) != 0 || displayHeight > Screen.height) {
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
    public int sceDisplayGetMode(@CanBeNull TPointer32 modeAddr, @CanBeNull TPointer32 widthAddr, @CanBeNull TPointer32 heightAddr) {
        modeAddr.setValue(mode);
        widthAddr.setValue(fb.getWidth());
        heightAddr.setValue(fb.getHeight());

        return 0;
    }

    @HLEFunction(nid = 0xDBA6C4C4, version = 150)
    public float sceDisplayGetFramePerSec() {
    	// Some applications are using a video playback loop requiring a very exact
    	// audio synchronization: the video playback keeps 4 buffers each for video
    	// images and for decoded audio buffers. When the buffer timestamps between
    	// audio and video differ by a too long delay value, the video playback breaks.
    	// The application actually tries to skip video frames to sync up but usually
    	// fails to it.
    	//
    	// The synchronization problem in Jpcsp is caused by the audio queues in sceAudio:
    	// the blocking methods (sceAudioOutputXXXBlocking) are not blocking as much as the
    	// PSP methods are doing. Especially, the first few calls (until the Jpcsp queue is
    	// filled up) are not blocking at all. This is causing the audio to play faster than
    	// the video at the beginning.
    	//
    	// The allowed delay is computed as follows:
    	//    2 * int(sceMpeg.mpegTimestampPerSecond / sceDisplayGetFramePerSec() * 2) = 6006
    	//
    	// Allow artificially a longer delay by returning here a lower value:
        float framePerSec = FRAME_PER_SEC / framePerSecFactor;

        // The hack is only used once
        framePerSecFactor = 1;

        return framePerSec;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7ED59BC4, version = 150, checkInsideInterrupt = true)
    @HLEFunction(nid = 0x3552AB11, version = 660, checkInsideInterrupt = true)
    public int sceDisplaySetHoldMode(int holdMode) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA544C486, version = 150, checkInsideInterrupt = true)
    @HLEFunction(nid = 0x03F16FD4, version = 660, checkInsideInterrupt = true)
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
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceDisplaySetFrameBuf topaddr=0x%08X, bufferwidth=%d, pixelformat=%d, syncType=%d (blocking display output)", topaddr, bufferwidth, pixelformat, syncType));
        	}
            isFbShowing = false;
            gotBadFbBufParams = true;
            return 0;
        }

        if (gotBadFbBufParams) {
            gotBadFbBufParams = false;
            if (log.isDebugEnabled()) {
            	log.debug(String.format("sceDisplaySetFrameBuf topaddr=0x%08X, bufferwidth=%d, pixelformat=%d, syncType=%d ok", topaddr, bufferwidth, pixelformat, syncType));
            }
        }

        if (topaddr == fb.getTopAddr() && bufferwidth == fb.getBufferWidth() && pixelformat == fb.getPixelFormat() && syncType == sync && isFbShowing) {
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
        displayScreen.update(texS, texT);

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
    public int sceDisplayGetFrameBuf(TPointer32 topaddrAddr, @CanBeNull TPointer32 bufferwidthAddr, @CanBeNull TPointer32 pixelformatAddr, int syncType) {
        topaddrAddr.setValue(fb.getTopAddr());
        bufferwidthAddr.setValue(fb.getBufferWidth());
        pixelformatAddr.setValue(fb.getPixelFormat());

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceDisplayGetFrameBuf returning topaddr=0x%08X, bufferwidth=0x%X, pixelformat=0x%X", fb.getTopAddr(), fb.getBufferWidth(), fb.getPixelFormat()));
        }
        return 0;
    }

    @HLEFunction(nid = 0xB4F378FA, version = 150)
    public boolean sceDisplayIsForeground() {
        return isFbShowing;
    }

    @HLEFunction(nid = 0x31C4BAA8, version = 150)
    public int sceDisplayGetBrightness(@CanBeNull TPointer32 levelAddr, @CanBeNull TPointer32 unknownAddr) {
    	levelAddr.setValue(Screen.getBrightnessLevel());
    	unknownAddr.setValue(0); // Always 0

    	return 0;
    }

    @HLEFunction(nid = 0x9E3C6DC6, version = 150)
    public int sceDisplaySetBrightness(int level, int syncType) {
    	if (level < 0 || level > 100) {
    		return SceKernelErrors.ERROR_INVALID_ARGUMENT;
    	}
    	if (syncType != 0 && syncType != 1) {
    		return SceKernelErrors.ERROR_INVALID_MODE;
    	}

    	Screen.setBrightnessLevel(level);

    	return 0;
    }

    @HLEFunction(nid = 0x9C6EAAD7, version = 150)
    public int sceDisplayGetVcount() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceDisplayGetVcount returning %d", vcount));
    	}
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
    @HLEFunction(nid = 0xE38CA615, version = 660, checkInsideInterrupt = true)
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

	@HLEUnimplemented
	@HLEFunction(nid = 0xBF79F646, version = 200)
	public int sceDisplayGetResumeMode() {
		return 0;
	}
    
	@HLEUnimplemented
	@HLEFunction(nid = 0x69B53541, version = 200)
	public int sceDisplayGetVblankRest() {
		return 0;
	}
    
	@HLEUnimplemented
	@HLEFunction(nid = 0x21038913, version = 200)
	public int sceDisplayIsVsync() {
		return 0;
	}

	/**
	 * Wait for Vblank start after multiple VSYNCs.
	 *
	 * @param cycleNum  Number of VSYNCs to wait before blocking the thread on VBLANK.
	 * @return 0
	 */
	@HLEFunction(nid = 0x40F1469C, version = 500, checkInsideInterrupt = true)
	public int sceDisplayWaitVblankStartMulti(int cycleNum) {
		return hleDisplayWaitVblankStart(cycleNum, false);
	}

	/**
	 * Wait for Vblank start after multiple VSYNCs, with Callback execution.
	 *
	 * @param cycleNum  Number of VSYNCs to wait before blocking the thread on VBLANK.
	 * @return 0
	 */
	@HLEFunction(nid = 0x77ED8B3A, version = 500, checkInsideInterrupt = true)
	public int sceDisplayWaitVblankStartMultiCB(int cycleNum) {
		return hleDisplayWaitVblankStart(cycleNum, true);
	}

	@HLEFunction(nid = 0x996881D2, version = 660)
	public int sceDisplay_driver_996881D2() {
		// Has no parameters
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x9B18DDDD, version = 660)
	public int sceDisplay_driver_9B18DDDD(int unknown) {
		// This seems to be related to the registry entry "/CONFIG/DISPLAY/color_space_mode"
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xF455917F, version = 660)
	public int sceDisplay_driver_F455917F(int unknown) {
		// This seems to be related to the registry entry "/CONFIG/SYSTEM/POWER_SAVING/active_backlight_mode"
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xE55F0D50, version = 150)
	public int sceDisplaySetBacklightSel(int unknown0, int unknown1) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xE97E9718, version = 660)
	public int sceDisplaySetPseudoVsync(int unknown) {
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

    private class ScreenshotAction implements IAction {
		@Override
		public void execute() {
            saveScreen();
		}
    }

    private class CopyGeToMemoryAction implements IAction {
    	private int geTopAddress;

		public CopyGeToMemoryAction(int geTopAddress) {
			this.geTopAddress = geTopAddress;
		}

		@Override
		public void execute() {
			VideoEngineUtilities.copyGeToMemory(re, geTopAddress, true, true);
			doneCopyGeToMemory = true;
		}
    }
}
