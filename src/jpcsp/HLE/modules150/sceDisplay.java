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

import static jpcsp.graphics.GeCommands.TFLT_NEAREST;
import static jpcsp.graphics.GeCommands.TWRAP_WRAP_MODE_CLAMP;
import static jpcsp.graphics.VideoEngine.SIZEOF_FLOAT;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.awt.Robot;
import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Settings;
import jpcsp.State;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.IWaitStateChecker;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.ThreadWaitInfo;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.RenderingEngineFactory;
import jpcsp.graphics.RE.RenderingEngineLwjgl;
import jpcsp.graphics.RE.buffer.IREBufferManager;
import jpcsp.graphics.capture.CaptureManager;
import jpcsp.graphics.textures.GETexture;
import jpcsp.graphics.textures.GETextureManager;
import jpcsp.hardware.Screen;
import jpcsp.scheduler.UnblockThreadAction;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

public class sceDisplay extends AWTGLCanvas implements HLEModule, HLEStartModule {
    protected static Logger log = Modules.getLogger("sceDisplay");

	private static final long serialVersionUID = 2267866365228834812L;

    private boolean onlyGEGraphics = false;
    private boolean saveGEToTexture = false;
    private static final boolean useDebugGL = false;
    private static final int internalTextureFormat = GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;

    // sceDisplayPixelFormats enum
    public static final int PSP_DISPLAY_PIXEL_FORMAT_565  = 0;
    public static final int PSP_DISPLAY_PIXEL_FORMAT_5551 = 1;
    public static final int PSP_DISPLAY_PIXEL_FORMAT_4444 = 2;
    public static final int PSP_DISPLAY_PIXEL_FORMAT_8888 = 3;

    // sceDisplaySetBufSync enum
    public static final int PSP_DISPLAY_SETBUF_IMMEDIATE = 0;
    public static final int PSP_DISPLAY_SETBUF_NEXTFRAME = 1;

    private static final float hCountPerVblank = 285.72f;

    // current Rendering Engine
    private IRenderingEngine re;
    private boolean startModules;
    private boolean isStarted;
    private int drawBuffer;

    // current display mode
    private int mode;
    private int width;
    private int height;
    private int widthGe;
    private int heightGe;

    // Resizing options
    private static float viewportResizeFilterScaleFactor = 1;
    private static int viewportResizeFilterScaleFactorInt = 1;
    private boolean resizePending;

    // current framebuffer settings
    private int topaddrFb;
    private int bufferwidthFb;
    private int pixelformatFb;
    private int sync;
    private boolean setGeBufCalledAtLeastOnce;
    public boolean gotBadGeBufParams;
    public boolean gotBadFbBufParams;
    protected boolean isFbShowing;

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

    private boolean initGLcalled;
    private String openGLversion;

    // Canvas fields
    private Buffer pixelsFb;
    private Buffer pixelsGe;
    private Buffer temp;
    private int tempSize;
    private int canvasWidth;
    private int canvasHeight;
    private boolean createTex;
    private int texFb;
    private int resizedTexFb;
    private float texS;
    private float texT;

    private int captureX;
    private int captureY;
    private int captureWidth;
    private int captureHeight;
    private Robot captureRobot;
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

    public DurationStatistics statistics;
    public DurationStatistics statisticsCopyGeToMemory;
    public DurationStatistics statisticsCopyMemoryToGe;

    // Async Display
    private AsyncDisplayThread asyncDisplayThread;

    // VBLANK Multi.
    private List<WaitVblankInfo> waitingOnVblank;

    // Anti-alias samples.
    private static int antiAliasSamplesNum;

    private static class WaitVblankInfo {
    	public int threadId;
    	public int unblockVcount;

    	public WaitVblankInfo(int threadId, int unblockVcount) {
    		this.threadId = threadId;
    		this.unblockVcount = unblockVcount;
    	}
    }

    private static class AsyncDisplayThread extends Thread {
		private Semaphore displaySemaphore;
		private boolean run;

		public AsyncDisplayThread() {
			displaySemaphore = new Semaphore(1);
			run = true;
		}

		@Override
		public void run() {
			jpcsp.HLE.modules.sceDisplay display = Modules.sceDisplayModule;
			while (run) {
				waitForDisplay();
				if (run) {
		        	if (!display.isOnlyGEGraphics() || VideoEngine.getInstance().hasDrawLists()) {
		        		display.repaint();
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
			return sceDisplay.this.vcount < vcount;
		}
	}

    @Override
    public String getName() {
        return "sceDisplay";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x0E20F177, sceDisplaySetModeFunction);
            mm.addFunction(0xDEA197D4, sceDisplayGetModeFunction);
            mm.addFunction(0xDBA6C4C4, sceDisplayGetFramePerSecFunction);
            mm.addFunction(0x7ED59BC4, sceDisplaySetHoldModeFunction);
            mm.addFunction(0xA544C486, sceDisplaySetResumeModeFunction);
            mm.addFunction(0x289D82FE, sceDisplaySetFrameBufFunction);
            mm.addFunction(0xEEDA2E54, sceDisplayGetFrameBufFunction);
            mm.addFunction(0xB4F378FA, sceDisplayIsForegroundFunction);
            mm.addFunction(0x31C4BAA8, sceDisplayGetBrightnessFunction);
            mm.addFunction(0x9C6EAAD7, sceDisplayGetVcountFunction);
            mm.addFunction(0x4D4E10EC, sceDisplayIsVblankFunction);
            mm.addFunction(0x36CDFADE, sceDisplayWaitVblankFunction);
            mm.addFunction(0x8EB9EC49, sceDisplayWaitVblankCBFunction);
            mm.addFunction(0x984C27E7, sceDisplayWaitVblankStartFunction);
            mm.addFunction(0x46F186C3, sceDisplayWaitVblankStartCBFunction);
            mm.addFunction(0x773DD3A3, sceDisplayGetCurrentHcountFunction);
            mm.addFunction(0x210EAB3A, sceDisplayGetAccumulatedHcountFunction);
            mm.addFunction(0xA83EF139, sceDisplayAdjustAccumulatedHcountFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceDisplaySetModeFunction);
            mm.removeFunction(sceDisplayGetModeFunction);
            mm.removeFunction(sceDisplayGetFramePerSecFunction);
            mm.removeFunction(sceDisplaySetHoldModeFunction);
            mm.removeFunction(sceDisplaySetResumeModeFunction);
            mm.removeFunction(sceDisplaySetFrameBufFunction);
            mm.removeFunction(sceDisplayGetFrameBufFunction);
            mm.removeFunction(sceDisplayIsForegroundFunction);
            mm.removeFunction(sceDisplayGetBrightnessFunction);
            mm.removeFunction(sceDisplayGetVcountFunction);
            mm.removeFunction(sceDisplayIsVblankFunction);
            mm.removeFunction(sceDisplayWaitVblankFunction);
            mm.removeFunction(sceDisplayWaitVblankCBFunction);
            mm.removeFunction(sceDisplayWaitVblankStartFunction);
            mm.removeFunction(sceDisplayWaitVblankStartCBFunction);
            mm.removeFunction(sceDisplayGetCurrentHcountFunction);
            mm.removeFunction(sceDisplayGetAccumulatedHcountFunction);
            mm.removeFunction(sceDisplayAdjustAccumulatedHcountFunction);

        }
    }

    public sceDisplay() throws LWJGLException {
    	super(null, new PixelFormat().withBitsPerPixel(8).withAlphaBits(8).withStencilBits(8).withSamples(antiAliasSamplesNum), null, new ContextAttribs().withDebug(useDebugGL));
        setScreenResolution(Screen.width, Screen.height);

        texFb = -1;
        resizedTexFb = -1;
        startModules = false;
        isStarted = false;
        resizePending = false;
        tempSize = 0;
    }

    public void setScreenResolution(int width, int height) {
        canvasWidth = width;
        canvasHeight = height;
        setSize(width, height);
    }

    public float getViewportResizeScaleFactor() {
        return viewportResizeFilterScaleFactor;
    }

    public void setViewportResizeScaleFactor(int width, int height) {
    	// Compute the scale factor in the horizontal and vertical directions
        float scaleWidth = ((float) width) / Screen.width;
        float scaleHeight = ((float) height) / Screen.height;

        // We are currently using only one scale factor to keep the PSP aspect ratio
        float scaleAspectRatio = (scaleWidth + scaleHeight) / 2;
        setViewportResizeScaleFactor(scaleAspectRatio);

        resizePending = true;
    }

    public void setViewportResizeScaleFactor(float viewportResizeFilterScaleFactor) {
    	if (viewportResizeFilterScaleFactor < 1) {
    		// Invalid value
    		return;
    	}

    	if (viewportResizeFilterScaleFactor != sceDisplay.viewportResizeFilterScaleFactor) {
    		sceDisplay.viewportResizeFilterScaleFactor = viewportResizeFilterScaleFactor;
    		sceDisplay.viewportResizeFilterScaleFactorInt = Math.round((float) Math.ceil(viewportResizeFilterScaleFactor));

    		// Resize the component while keeping the PSP aspect ratio
    		setMinimumSize(new Dimension(getResizedWidth(Screen.width), getResizedHeight(Screen.height)));

    		// The preferred size is used when resizing the MainGUI
    		setPreferredSize(getSize());

    		// Recreate the texture if the scaling factor has changed
			createTex = true;
    	}
    }

    /**
     * Resize the given value according to the viewport resizing factor, assuming
     * it is a value along the X-Axis (e.g. "x" or "width" value).
     *
     * @param width        value on the X-Axis to be resized
     * @return             the resized value
     */
    public final static int getResizedWidth(int width) {
    	return Math.round(width * viewportResizeFilterScaleFactor);
    }

    /**
     * Resize the given value according to the viewport resizing factor, assuming
     * it is a value along the X-Axis being a power of 2 (i.e. 2^n).
     *
     * @param width        value on the X-Axis to be resized, must be a power of 2.
     * @return             the resized value, as a power of 2.
     */
    public final static int getResizedWidthPow2(int widthPow2) {
    	return widthPow2 * viewportResizeFilterScaleFactorInt;
    }

    /**
     * Resize the given value according to the viewport resizing factor, assuming
     * it is a value along the Y-Axis (e.g. "y" or "height" value).
     *
     * @param height       value on the Y-Axis to be resized
     * @return             the resized value
     */
    public final static int getResizedHeight(int height) {
    	return Math.round(height * viewportResizeFilterScaleFactor);
    }

    /**
     * Resize the given value according to the viewport resizing factor, assuming
     * it is a value along the Y-Axis being a power of 2 (i.e. 2^n).
     *
     * @param width        value on the Y-Axis to be resized, must be a power of 2.
     * @return             the resized value, as a power of 2.
     */
    public final static int getResizedHeightPow2(int heightPow2) {
    	return heightPow2 * viewportResizeFilterScaleFactorInt;
    }

    public static void setAntiAliasSamplesNum(int samples) {
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

        mode          = 0;
        width         = Screen.width;
        height        = Screen.height;
        topaddrFb     = MemoryMap.START_VRAM;
        bufferwidthFb = 512;
        pixelformatFb = PSP_DISPLAY_PIXEL_FORMAT_8888;
        sync          = PSP_DISPLAY_SETBUF_IMMEDIATE;

        bottomaddrFb = topaddrFb + bufferwidthFb * height * getPixelFormatBytes(pixelformatFb);

        detailsDirty = true;
        displayDirty = true;
        geDirty = false;
        createTex = true;

        pixelsFb = getPixels(topaddrFb, bottomaddrFb);

        widthGe       = Screen.width;
        heightGe      = Screen.height;
        topaddrGe     = topaddrFb;
        bufferwidthGe = bufferwidthFb;
        pixelformatGe = pixelformatFb;
        bottomaddrGe  = bottomaddrFb;
        pixelsGe = getPixels(topaddrGe, bottomaddrGe);

        isFbShowing = false;
        setGeBufCalledAtLeastOnce = false;
        gotBadGeBufParams = false;
        gotBadFbBufParams = false;

        prevStatsTime = 0;
        frameCount = 0;
        prevFrameCount = 0;
        reportCount = 0;
        averageFPS = 0.0;

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

    	saveGEToTexture = Settings.getInstance().readBool("emu.enablegetexture");
    	if (saveGEToTexture) {
    		log.info("Saving GE to Textures");
    	}

        captureX = Settings.getInstance().readInt("gui.windows.mainwindow.x") + 4;
        captureY = Settings.getInstance().readInt("gui.windows.mainwindow.y") + 76;
        captureWidth = getCanvasWidth();
        captureHeight = getCanvasHeight();
        try {
            captureRobot = new Robot();
            captureRobot.setAutoDelay(0);
        } catch (Exception e) {
            // Ignore.
        }
    }

    @Override
    public void stop() {
    	if (asyncDisplayThread != null) {
    		asyncDisplayThread.exit();
    		asyncDisplayThread = null;
    	}
    	re = null;
    	startModules = false;
    	isStarted = false;
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

    public final void write8(int rawAddress) {
        // vram address is lower than main memory so check the end of the buffer first, it's more likely to fail
        if (rawAddress < bottomaddrFb && rawAddress >= topaddrFb) {
            displayDirty = true;
        }
    }

    public final void write16(int rawAddress) {
        if (rawAddress < bottomaddrFb && rawAddress >= topaddrFb) {
            displayDirty = true;
        }
    }

    public final void write32(int rawAddress) {
        if (rawAddress < bottomaddrFb && rawAddress >= topaddrFb) {
            displayDirty = true;
        }
    }

    public IRenderingEngine getRenderingEngine() {
    	return re;
    }

    public void setGeDirty(boolean dirty) {
        geDirty = dirty;
    }

    public void hleDisplaySetGeMode(int width, int height) {
        if (width <= 0 || height <= 0) {
            log.warn("hleDisplaySetGeMode(" + width + "," + height + ") bad params");
        } else {
            log.debug("hleDisplaySetGeMode(width=" + width + ",height=" + height + ")");
            widthGe = width;
            heightGe = height;
            bottomaddrGe =
                topaddrGe + bufferwidthGe * heightGe *
                getPixelFormatBytes(pixelformatGe);
            pixelsGe = getPixels(topaddrGe, bottomaddrGe);
        }
    }

    public void hleDisplaySetGeBuf(int topaddr, int bufferwidth, int pixelformat, boolean copyGEToMemory, boolean forceLoadGEToScreen) {
    	hleDisplaySetGeBuf(topaddr, bufferwidth, pixelformat, copyGEToMemory, forceLoadGEToScreen, widthGe, heightGe);
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

        if (topaddr == topaddrGe && bufferwidth == bufferwidthGe &&
            pixelformat == pixelformatGe &&
            width == widthGe && height == heightGe) {

        	// Nothing changed
        	if (forceLoadGEToScreen) {
        		loadGEToScreen();
        	}

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
                log.warn(msg);
                gotBadGeBufParams = true;
            }
            else
            {
                log.debug(msg);
                setGeBufCalledAtLeastOnce = true;
            }

            return;
        }
		if (gotBadGeBufParams) {
		    // print when we get good params after bad params
		    gotBadGeBufParams = false;
		    log.info("hleDisplaySetGeBuf ok ("
		        + Integer.toHexString(topaddr)
		        + "," + bufferwidth
		        + "," + pixelformat + ")");
		}

    	if (re.isVertexArrayAvailable()) {
    		re.bindVertexArray(0);
    	}

		boolean loadGEToScreen = true; // Always reload the GE memory to the screen
		if (copyGEToMemory && (topaddrGe != topaddr || pixelformatGe != pixelformat)) {
			if (VideoEngine.log.isDebugEnabled()) {
				VideoEngine.log.debug(String.format("Copy GE Screen to Memory 0x%08X-0x%08X", topaddrGe, bottomaddrGe));
			}

			if (statisticsCopyGeToMemory != null) {
				statisticsCopyGeToMemory.start();
			}

			if (saveGEToTexture && !VideoEngine.getInstance().isVideoTexture(topaddrGe)) {
				GETexture geTexture = GETextureManager.getInstance().getGETexture(re, topaddrGe, bufferwidthGe, widthGe, heightGe, pixelformatGe, true);
				geTexture.copyScreenToTexture(re);
			} else {
	        	// Set texFb as the current texture
			    re.bindTexture(resizedTexFb);
				re.setTextureFormat(pixelformatGe, false);

			    // Copy screen to the current texture
			    re.copyTexSubImage(0, 0, 0, 0, 0, getResizedWidth(Math.min(bufferwidthGe, widthGe)), getResizedHeight(heightGe));

			    // Re-render GE/current texture upside down
			    drawFrameBuffer(true, true, bufferwidthGe, pixelformatGe, widthGe, heightGe);

			    copyScreenToPixels(pixelsGe, bufferwidthGe, pixelformatGe, widthGe, heightGe);
			    loadGEToScreen = true;
			}

			if (statisticsCopyGeToMemory != null) {
				statisticsCopyGeToMemory.end();
			}
		}

		topaddrGe     = topaddr;
		bufferwidthGe = bufferwidth;
		pixelformatGe = pixelformat;
		widthGe       = width;
		heightGe      = height;

		bottomaddrGe =
		    topaddrGe + bufferwidthGe * heightGe *
		    getPixelFormatBytes(pixelformatGe);

        // Tested on PSP:
        // The height of the buffer always matches the display height.
        // This data can be obtained from hleDisplaySetGeMode, since the width
        // represents the display width in pixels, and the height represents
        // the display height in lines.

		pixelsGe = getPixels(topaddrGe, bottomaddrGe);
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

    private Buffer getPixels(int topaddr, int bottomaddr) {
        Buffer pixels = Memory.getInstance().getBuffer(topaddr, bottomaddr - topaddr);
        return pixels;
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

    public void rotate(int angleId) {
        ang = angleId;

        switch(angleId){
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

    public void saveScreen() {
        int tag = 0;
        File screenshot = new File(State.discId + "-" + "Shot" + "-" + tag + ".png");
        File directory = new File(System.getProperty("user.dir"));

        for(File file : directory.listFiles()) {
            if(file.getName().equals(screenshot.getName())) {
               tag++;
               screenshot = new File(State.discId + "-" + "Shot" + "-" + tag + ".png");
            }
        }

        Rectangle rect = new Rectangle(captureX, captureY, captureWidth, captureHeight);
        try {
            BufferedImage img = captureRobot.createScreenCapture(rect);
            ImageIO.write(img, "png", screenshot);
            img.flush();
        } catch (Exception e) {
            return;
        }

        getscreen = false;
    }


    // For capture/replay
    public int getTopAddrFb() { return topaddrFb; }
    public int getBufferWidthFb() { return bufferwidthFb; }
    public int getPixelFormatFb() { return pixelformatFb; }
    public int getSync() { return sync; }

    public int getWidthFb() {
    	return width;
    }

    public int getHeightFb() {
    	return height;
    }

    public int getTopAddrGe() {
    	return topaddrGe;
    }

    public int getBufferWidthGe() {
    	return bufferwidthGe;
    }

    public int getWidthGe() {
    	return widthGe;
    }

    public int getHeightGe() {
    	return heightGe;
    }

    public BufferInfo getBufferInfoGe() {
    	return new BufferInfo(topaddrGe, bottomaddrGe, widthGe, heightGe, bufferwidthGe, pixelformatGe);
    }

    public BufferInfo getBufferInfoFb() {
    	return new BufferInfo(topaddrFb, bottomaddrFb, width, height, bufferwidthFb, pixelformatFb);
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
    	// Create a GE texture (the texture texFb might not have the right size)
        int texGe = re.genTexture();

        re.bindTexture(texGe);
		re.setTextureFormat(pixelformatGe, false);
        re.setTexImage(0,
            internalTextureFormat,
            getResizedWidthPow2(bufferwidthGe),
            getResizedHeightPow2(Utilities.makePow2(heightGe)),
            pixelformatGe,
            pixelformatGe,
            0, null);

        re.setTextureMipmapMinFilter(TFLT_NEAREST);
        re.setTextureMipmapMagFilter(TFLT_NEAREST);
        re.setTextureMipmapMinLevel(0);
        re.setTextureMipmapMaxLevel(0);
        re.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);
        re.setPixelStore(getResizedWidthPow2(bufferwidthGe), getPixelFormatBytes(pixelformatGe));

        // Copy screen to the GE texture
        re.copyTexSubImage(0, 0, 0, 0, 0, getResizedWidth(Math.min(widthGe, bufferwidthGe)), getResizedHeight(heightGe));

        // Copy the GE texture into temp buffer
        temp.clear();
        re.getTexImage(0, pixelformatGe, pixelformatGe, temp);

        // Capture the GE image
        CaptureManager.captureImage(topaddrGe, 0, temp, getResizedWidth(widthGe), getResizedHeight(heightGe), getResizedWidthPow2(bufferwidthGe), pixelformatGe, false, 0, true, false);

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

    private void loadGEToScreen() {
		if (VideoEngine.log.isDebugEnabled()) {
			VideoEngine.log.debug(String.format("Reloading GE Memory (0x%08X-0x%08X) to screen (%dx%d)", topaddrGe, bottomaddrGe, widthGe, heightGe));
		}

		if (statisticsCopyMemoryToGe != null) {
			statisticsCopyMemoryToGe.start();
		}

    	if (saveGEToTexture && !VideoEngine.getInstance().isVideoTexture(topaddrGe)) {
			GETexture geTexture = GETextureManager.getInstance().getGETexture(re, topaddrGe, bufferwidthGe, widthGe, heightGe, pixelformatGe, true);
			geTexture.copyTextureToScreen(re);
    	} else {
        	if (re.isVertexArrayAvailable()) {
        		re.bindVertexArray(0);
        	}

			// Set texFb as the current texture
			re.bindTexture(texFb);

			// Define the texture from the GE Memory
		    re.setPixelStore(bufferwidthGe, getPixelFormatBytes(pixelformatGe));
		    int textureSize = bufferwidthGe * heightGe * getPixelFormatBytes(pixelformatGe);
		    pixelsGe.clear();
			re.setTexSubImage(0, 0, 0, bufferwidthGe, heightGe, pixelformatGe, pixelformatGe, textureSize, pixelsGe);

			// Draw the GE
		    drawFrameBuffer(false, true, bufferwidthGe, pixelformatGe, widthGe, heightGe);
    	}

		if (statisticsCopyMemoryToGe != null) {
			statisticsCopyMemoryToGe.end();
		}
    }

    /**
     * @param keepOriginalSize : true  = draw as psp size
     *                           false = draw as window size
     **/
    private void drawFrameBuffer(boolean keepOriginalSize, boolean invert, int bufferwidth, int pixelformat, int width, int height) {
        if (!isrotating) {
            texS1 = texS4 = texS;
            texT1 = texT2 = texT;

            texS2 = texS3 = texT3 = texT4 = 0.0f;
        }

    	re.startDirectRendering(true, false, true, true, !invert, width, height);
        if (keepOriginalSize) {
            re.setViewport(0, 0, width, height);
        } else {
            re.setViewport(0, 0, getResizedWidth(width), getResizedHeight(height));
        }

		re.setTextureFormat(pixelformat, false);

        IREBufferManager bufferManager = re.getBufferManager();
        ByteBuffer drawByteBuffer = bufferManager.getBuffer(drawBuffer);
        drawByteBuffer.clear();
        FloatBuffer drawFloatBuffer = drawByteBuffer.asFloatBuffer();
        drawFloatBuffer.clear();
        drawFloatBuffer.put(texS1);
        drawFloatBuffer.put(texT1);
        drawFloatBuffer.put(width);
        drawFloatBuffer.put(height);

        drawFloatBuffer.put(texS2);
        drawFloatBuffer.put(texT2);
        drawFloatBuffer.put(0);
        drawFloatBuffer.put(height);

        drawFloatBuffer.put(texS3);
        drawFloatBuffer.put(texT3);
        drawFloatBuffer.put(0);
        drawFloatBuffer.put(0);

        drawFloatBuffer.put(texS4);
        drawFloatBuffer.put(texT4);
        drawFloatBuffer.put(width);
        drawFloatBuffer.put(0);

        if (re.isVertexArrayAvailable()) {
        	re.bindVertexArray(0);
        }
        re.setVertexInfo(null, false, false, true, IRenderingEngine.RE_QUADS);
        re.enableClientState(IRenderingEngine.RE_TEXTURE);
        re.disableClientState(IRenderingEngine.RE_COLOR);
        re.disableClientState(IRenderingEngine.RE_NORMAL);
        re.enableClientState(IRenderingEngine.RE_VERTEX);
        bufferManager.setTexCoordPointer(drawBuffer, 2, IRenderingEngine.RE_FLOAT, 4 * SIZEOF_FLOAT, 0);
        bufferManager.setVertexPointer(drawBuffer, 2, IRenderingEngine.RE_FLOAT, 4 * SIZEOF_FLOAT, 2 * SIZEOF_FLOAT);
        bufferManager.setBufferData(drawBuffer, drawFloatBuffer.position() * SIZEOF_FLOAT, drawByteBuffer.rewind(), IRenderingEngine.RE_DYNAMIC_DRAW);
        re.drawArrays(IRenderingEngine.RE_QUADS, 0, 4);

        re.endDirectRendering();

        isrotating = false;
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
        re.bindTexture(texFb);
		re.setTextureFormat(pixelformatFb, false);

        re.setPixelStore(bufferWidth, getPixelFormatBytes(pixelFormat));

        // Copy screen to the current texture
        re.copyTexSubImage(0, 0, 0, 0, 0, Math.min(bufferWidth, width), height);

        // Copy the current texture into memory
        Buffer buffer = (pixels.capacity() >= temp.capacity() ? pixels : temp);
        buffer.clear();
        re.getTexImage(0, pixelFormat, pixelFormat, buffer);

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

    protected void blockCurrentThreadOnVblank(int cycles, boolean doCallbacks) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        SceKernelThreadInfo thread = threadMan.getCurrentThread();
        int threadId = threadMan.getCurrentThreadID();

        int lastWaitVblank = thread.displayLastWaitVcount;
        int unblockVcount = lastWaitVblank + cycles;
        if (unblockVcount <= vcount) {
        	// This thread has just to wait for the next VBLANK.
            // Add a Vblank action to unblock the thread
            UnblockThreadAction vblankAction = new UnblockThreadAction(threadId);
            IntrManager.getInstance().addVBlankActionOnce(vblankAction);
            thread.displayLastWaitVcount = vcount + 1;
        } else {
        	// This thread has to wait for multiple VBLANK's
        	WaitVblankInfo waitVblankInfo = new WaitVblankInfo(threadId, unblockVcount);
        	waitingOnVblank.add(waitVblankInfo);
        }

        // Block the current thread.
        if (doCallbacks) {
        	threadMan.hleBlockCurrentThreadCB(null, new VblankWaitStateChecker(vcount + cycles));
        } else {
        	threadMan.hleBlockCurrentThread();
        }
    }

    private void hleVblankStart() {
    	lastVblankMicroTime = Emulator.getClock().microTime();
    	// Vcount increases at each VBLANK.
    	vcount++;

    	// Check the threads waiting for VBLANK (multi).
    	if (!waitingOnVblank.isEmpty()) {
    		for (ListIterator<WaitVblankInfo> lit = waitingOnVblank.listIterator(); lit.hasNext();) {
    			WaitVblankInfo waitVblankInfo = lit.next();
    			if (waitVblankInfo.unblockVcount <= vcount) {
    				ThreadManForUser threadMan = Modules.ThreadManForUserModule;
    				SceKernelThreadInfo thread = threadMan.getThreadById(waitVblankInfo.threadId);
    				if (thread != null) {
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
        	re.deleteTexture(textureId);
        }
        textureId = re.genTexture();

        re.bindTexture(textureId);
		re.setTextureFormat(pixelformatFb, false);

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
            isResized ? getResizedWidthPow2(bufferwidthFb) : bufferwidthFb,
            isResized ? getResizedHeightPow2(Utilities.makePow2(height)) : Utilities.makePow2(height),
            pixelformatFb,
            pixelformatFb,
            0, null);
        re.setTextureMipmapMinFilter(GeCommands.TFLT_NEAREST);
        re.setTextureMipmapMagFilter(GeCommands.TFLT_NEAREST);
        re.setTextureMipmapMinLevel(0);
        re.setTextureMipmapMaxLevel(0);
        re.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);

        return textureId;
    }

    @Override
	protected void paintGL() {
    	if (resizePending) {
    		// Resize the MainGUI to use the preferred size of this sceDisplay
    		Emulator.getMainGUI().pack();
    		resizePending = false;
    	}

    	if (statistics != null) {
            statistics.start();
        }

    	if (re == null) {
    		if (startModules) {
    			re = RenderingEngineFactory.createRenderingEngine();
    		} else {
    			re = RenderingEngineFactory.createInitialRenderingEngine();
    		}
    	}

    	if (startModules) {
    		if (saveGEToTexture) {
    			GETextureManager.getInstance().reset(re);
    		}
    		VideoEngine.getInstance().start();
        	drawBuffer = re.getBufferManager().genBuffer(IRenderingEngine.RE_FLOAT, 16, IRenderingEngine.RE_DYNAMIC_DRAW);
	    	startModules = false;
	    	if (saveGEToTexture && !re.isFramebufferObjectAvailable()) {
	    		saveGEToTexture = false;
	    		log.warn("Saving GE to Textures has been automatically disabled: FBO is not supported by this OpenGL version");
	    	}
	    	isStarted = true;
    	}

    	if (!isStarted) {
        	re.clear(0.0f, 0.0f, 0.0f, 0.0f);
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

        if (onlyGEGraphics) {
        	re.startDisplay();
            VideoEngine.getInstance().update();
            re.endDisplay();
        } else {
        	if (log.isDebugEnabled()) {
        		log.debug("sceDisplay.paintGL - start display");
        	}
        	re.startDisplay();

        	// The GE will be reloaded to the screen by the VideoEngine
            if (VideoEngine.getInstance().update()) {
                // Save the GE only if it actually drew something
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("sceDisplay.paintGL - saving the GE to memory 0x%08X", topaddrGe));
            	}

            	if (saveGEToTexture && !VideoEngine.getInstance().isVideoTexture(topaddrGe)) {
            		GETexture geTexture = GETextureManager.getInstance().getGETexture(re, topaddrGe, bufferwidthGe, widthGe, heightGe, pixelformatGe, true);
            		geTexture.copyScreenToTexture(re);
            	} else {
	                // Set texFb as the current texture
	                re.bindTexture(resizedTexFb);
	    			re.setTextureFormat(pixelformatGe, false);

	                // Copy screen to the current texture
	                re.copyTexSubImage(0, 0, 0, 0, 0, getResizedWidth(widthGe), getResizedHeight(heightGe));

	                // Re-render GE/current texture upside down
	                drawFrameBuffer(true, true, bufferwidthFb, pixelformatFb, width, height);

	                // Save GE/current texture to vram
	                copyScreenToPixels(pixelsGe, bufferwidthGe, pixelformatGe, widthGe, heightGe);
            	}
            }

            // Render the FB
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceDisplay.paintGL - rendering the FB 0x%08X", topaddrFb));
        	}
        	if (saveGEToTexture && !VideoEngine.getInstance().isVideoTexture(topaddrFb)) {
        		GETexture geTexture = GETextureManager.getInstance().getGETexture(re, topaddrFb, bufferwidthFb, width, height, pixelformatFb, true);
        		geTexture.copyTextureToScreen(re);
        	} else {
	            pixelsFb.clear();
	            re.bindTexture(texFb);
				re.setTextureFormat(pixelformatFb, false);
	            re.setPixelStore(bufferwidthFb, pixelformatFb);
	            int textureSize = bufferwidthFb * height * getPixelFormatBytes(pixelformatFb);
	            re.setTexSubImage(0,
	                0, 0, bufferwidthFb, height,
	                pixelformatFb,
	                pixelformatFb,
	                textureSize, pixelsFb);

	            // Call the rotating function (if needed)
	            if (ang != 4) {
	                rotate(ang);
	            }

	            drawFrameBuffer(false, true, bufferwidthFb, pixelformatFb, width, height);
        	}

            re.endDisplay();

            if (log.isDebugEnabled()) {
        		log.debug("sceDisplay.paintGL - end display");
        	}
        }

        try {
			swapBuffers();
		} catch (LWJGLException e) {
		}

        reportFPSStats();

        if (statistics != null) {
            statistics.end();
        }

        if (getscreen) {
        	saveScreen();
        }
	}

	private void checkTemp() {
        // Buffer large enough to store the complete FB or GE texture
        int sizeInBytes = getResizedWidthPow2(Math.max(bufferwidthFb, bufferwidthGe))
                        * getResizedHeightPow2(Utilities.makePow2(Math.max(height, heightGe)))
                        * getPixelFormatBytes(Math.max(pixelformatFb, pixelformatGe));

        if (sizeInBytes > tempSize) {
        	ByteBuffer byteBuffer = ByteBuffer.allocateDirect(sizeInBytes).order(ByteOrder.LITTLE_ENDIAN);

        	if (Memory.getInstance().getMainMemoryByteBuffer() instanceof IntBuffer) {
        		temp = byteBuffer.asIntBuffer();
        	} else {
        		temp = byteBuffer;
        	}
        	tempSize = sizeInBytes;
        }
	}

	@Override
	protected void initGL() {
		setSwapInterval(1);
		super.initGL();

		// Collect debugging information...
		initGLcalled = true;
		openGLversion = RenderingEngineLwjgl.getVersion();
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		canvasWidth  = width;
        canvasHeight = height;
        super.setBounds(x, y, width, height);
	}

    @Override
	public void componentMoved(ComponentEvent e) {
        captureX = getX();
        captureY = getY();
        captureWidth = getWidth();
        captureHeight = getHeight();
	}

    @Override
	public void componentResized(ComponentEvent e) {
        captureX = getX();
        captureY = getY();
        captureWidth = getWidth();
        captureHeight = getHeight();

        setViewportResizeScaleFactor(getWidth(), getHeight());
    }

    public void sceDisplaySetMode(Processor processor) {
        CpuState cpu = processor.cpu;

        int displayMode = cpu.gpr[4];
        int displayWidth = cpu.gpr[5];
        int displayHeight = cpu.gpr[6];

        if (log.isDebugEnabled()) {
        	log.debug("sceDisplaySetMode(mode=" + displayMode + ",width=" + displayWidth + ",height=" + displayHeight + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (displayWidth <= 0 || displayHeight <= 0) {
            cpu.gpr[2] = -1;
        } else {
            this.mode   = displayMode;
            this.width  = displayWidth;
            this.height = displayHeight;

            bottomaddrFb =
                topaddrFb + bufferwidthFb * displayHeight *
                getPixelFormatBytes(pixelformatFb);
            pixelsFb = getPixels(topaddrFb, bottomaddrFb);

            detailsDirty = true;

            if (displayMode != 0) {
                log.warn("UNIMPLEMENTED:sceDisplaySetMode mode=" + displayMode);
            }

            cpu.gpr[2] = 0;
        }
    }

    public void sceDisplayGetMode(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory memory = Processor.memory;

        int modeAddr = cpu.gpr[4];
        int widthAddr = cpu.gpr[5];
        int heightAddr = cpu.gpr[6];

        if (!Memory.isAddressGood(modeAddr  ) ||
            !Memory.isAddressGood(widthAddr ) ||
            !Memory.isAddressGood(heightAddr))
        {
            cpu.gpr[2] = -1;
        } else {
            memory.write32(modeAddr  , mode  );
            memory.write32(widthAddr , width );
            memory.write32(heightAddr, height);
            cpu.gpr[2] = 0;
        }
    }

    public void sceDisplayGetFramePerSec(Processor processor) {
        CpuState cpu = processor.cpu;
        // Return float value in $f0
    	cpu.fpr[0] = 59.940060f;
    	if (log.isDebugEnabled()) {
    		log.debug("sceDisplayGetFramePerSec ret: " + cpu.fpr[0]);
    	}
    }

    public void sceDisplaySetHoldMode(Processor processor) {
        CpuState cpu = processor.cpu;

        int holdMode = cpu.gpr[4];

        log.warn("IGNORING: sceDisplaySetHoldMode holdMode=" + holdMode);

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceDisplaySetResumeMode(Processor processor) {
        CpuState cpu = processor.cpu;

        int resumeMode = cpu.gpr[4];

        log.warn("IGNORING: sceDisplaySetResumeMode resumeMode=" + resumeMode);

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceDisplaySetFrameBuf(Processor processor) {
        CpuState cpu = processor.cpu;

        int topaddr = cpu.gpr[4];
        int bufferwidth = cpu.gpr[5];
        int pixelformat = cpu.gpr[6];
        int syncType = cpu.gpr[7];

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceDisplaySetFrameBuf(topaddr=0x%08X, bufferwidth=%d, pixelformat=%d, syncType=%d)", topaddr, bufferwidth, pixelformat, syncType));
        }

        cpu.gpr[2] = hleDisplaySetFrameBuf(topaddr, bufferwidth, pixelformat, syncType);
    }

    public int hleDisplaySetFrameBuf(int topaddr, int bufferwidth, int pixelformat, int syncType) {
		topaddr &= Memory.addressMask;

        if ((bufferwidth < 0 || (bufferwidth & (bufferwidth - 1)) != 0) ||
                ((bufferwidth == 0) && (topaddr != 0))) {
            log.warn(
                "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                ", bufferwidth=" + bufferwidth +
                ", pixelformat=" + pixelformat +
                ", syncType=" + syncType + ") bad bufferwidth");
            isFbShowing = false;
            gotBadFbBufParams = true;
            return SceKernelErrors.ERROR_INVALID_SIZE;
        }else if (pixelformat < 0 || pixelformat > 3) {
            log.warn(
                "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                ", bufferwidth=" + bufferwidth +
                ", pixelformat=" + pixelformat +
                ", syncType=" + syncType + ") bad pixelformat");
            isFbShowing = false;
            gotBadFbBufParams = true;
            return -1;
        } else if (syncType < 0 || syncType > 1) {
            log.warn(
                "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                ", bufferwidth=" + bufferwidth +
                ", pixelformat=" + pixelformat +
                ", syncType=" + syncType + ") bad syncType");
            isFbShowing = false;
            gotBadFbBufParams = true;
            return SceKernelErrors.ERROR_INVALID_MODE;
        } else if ((topaddr == 0)) {
            // If topaddr is NULL, the PSP's screen will be displayed as fully black
            // as the output is blocked. Under these circumstances, bufferwidth can be 0.
            log.warn(
                "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                ", bufferwidth=" + bufferwidth +
                ", pixelformat=" + pixelformat +
                ", syncType=" + syncType + ") (blocking display output)");
            isFbShowing = false;
            gotBadFbBufParams = true;
            return 0;
        } else if (Memory.isAddressGood(topaddr)){
            if(topaddr < MemoryMap.START_VRAM || topaddr >= MemoryMap.END_VRAM) {
                log.warn("sceDisplaySetFrameBuf (topaddr=0x" + Integer.toHexString(topaddr) + ")"
                        + " is using main memory.");
            }

            if (gotBadFbBufParams) {
                gotBadFbBufParams = false;
                log.info(
                    "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                    ", bufferwidth=" + bufferwidth +
                    ", pixelformat=" + pixelformat +
                    ", syncType=" + syncType + ") ok");
            }

            if (pixelformat != pixelformatFb ||
                bufferwidth != bufferwidthFb ||
                Utilities.makePow2(height) != Utilities.makePow2(height)) {
                createTex = true;
            }

            topaddrFb     = topaddr;
            bufferwidthFb = bufferwidth;
            pixelformatFb = pixelformat;
            sync          = syncType;

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

            VideoEngine.getInstance().hleSetFrameBuf(topaddrFb, bufferwidthFb, pixelformatFb);

            return 0;
        }
        return SceKernelErrors.ERROR_INVALID_POINTER;
	}

    public void sceDisplayGetFrameBuf(Processor processor) {
        CpuState cpu = processor.cpu;
    	Memory mem = Memory.getInstance();

        int topaddrAddr = cpu.gpr[4];
        int bufferwidthAddr = cpu.gpr[5];
        int pixelformatAddr = cpu.gpr[6];
        int syncType = cpu.gpr[7];

        if (log.isDebugEnabled()) {
    		log.debug(String.format("sceDisplayGetFrameBuf topaddrAddr=0x%08X, bufferwidthAddr=0x%08X, pixelformatAddr=0x%08X, sync=%d",
                    topaddrAddr, bufferwidthAddr, pixelformatAddr, syncType));
        }

        // The PSP checks only if syncType == 1.
        // Any other syncType value is interpreted as 0.
        if (!Memory.isAddressGood(topaddrAddr    ) ||
            !Memory.isAddressGood(bufferwidthAddr) ||
            !Memory.isAddressGood(pixelformatAddr)) {
            cpu.gpr[2] = -1;
        } else {
            mem.write32(topaddrAddr    , topaddrFb    );
            mem.write32(bufferwidthAddr, bufferwidthFb);
            mem.write32(pixelformatAddr, pixelformatFb);
            cpu.gpr[2] = 0;
        }
    }

    public void sceDisplayIsForeground(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
    		log.debug("sceDisplayIsForeground ret: " + isFbShowing);
    	}
        cpu.gpr[2] = isFbShowing ? 1 : 0;
    }

    public void sceDisplayGetBrightness(Processor processor) {
        CpuState cpu = processor.cpu;

        int leveladdr = cpu.gpr[4];
        int unkaddr = cpu.gpr[5];

        log.warn("IGNORING: sceDisplayGetBrightness leveladdr=0x"
                + Integer.toHexString(leveladdr) + ", unkaddr=0x"
                + Integer.toHexString(unkaddr));
        cpu.gpr[2] = 0;
    }

    public void sceDisplayGetVcount(Processor processor) {
        CpuState cpu = processor.cpu;
        // 60 units per second
        cpu.gpr[2] = vcount;
    }

    public void sceDisplayIsVblank(Processor processor) {
        CpuState cpu = processor.cpu;

        cpu.gpr[2] = isVblank() ? 1 : 0;

        if (log.isDebugEnabled()) {
        	log.debug("sceDisplayIsVblank returns " + cpu.gpr[2]);
        }
    }

    public void sceDisplayWaitVblank(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
        	log.debug("sceDisplayWaitVblank");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    	if (!isVblank()) {
    		sceDisplayWaitVblankStart(processor);
    	}
    }

    public void sceDisplayWaitVblankCB(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
        	log.debug("sceDisplayWaitVblankCB");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
        if (!isVblank()) {
        	sceDisplayWaitVblankStartCB(processor);
        }
    }

    public void sceDisplayWaitVblankStart(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
        	log.debug("sceDisplayWaitVblankStart");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
        blockCurrentThreadOnVblank(1, false);
    }

    public void sceDisplayWaitVblankStartCB(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
        	log.debug("sceDisplayWaitVblankStartCB");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
        blockCurrentThreadOnVblank(1, true);
    }

    public void sceDisplayGetCurrentHcount(Processor processor) {
        CpuState cpu = processor.cpu;
        int currentHcount = getCurrentHcount();

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceDisplayGetCurrentHcount returning %d", currentHcount));
        }

        cpu.gpr[2] = currentHcount;
    }

    public void sceDisplayGetAccumulatedHcount(Processor processor) {
        CpuState cpu = processor.cpu;

        // The accumulatedHcount is the currentHcount plus the sum of the Hcounts
        // from all the previous vblanks (vcount * number of Hcounts per Vblank).
        int currentHcount = getCurrentHcount();
        int accumulatedHcount = currentHcount + (int) (vcount * hCountPerVblank);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceDisplayGetAccumulatedHcount returning %d (currentHcount=%d)", accumulatedHcount, currentHcount));
        }

        cpu.gpr[2] = accumulatedHcount;
    }

    public void sceDisplayAdjustAccumulatedHcount(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceDisplayAdjustAccumulatedHcount");
        cpu.gpr[2] = 0;
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

    public final HLEModuleFunction sceDisplaySetModeFunction = new HLEModuleFunction("sceDisplay", "sceDisplaySetMode") {

        @Override
        public final void execute(Processor processor) {
            sceDisplaySetMode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplaySetMode(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayGetModeFunction = new HLEModuleFunction("sceDisplay", "sceDisplayGetMode") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayGetMode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayGetMode(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayGetFramePerSecFunction = new HLEModuleFunction("sceDisplay", "sceDisplayGetFramePerSec") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayGetFramePerSec(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayGetFramePerSec(processor);";
        }
    };
    public final HLEModuleFunction sceDisplaySetHoldModeFunction = new HLEModuleFunction("sceDisplay", "sceDisplaySetHoldMode") {

        @Override
        public final void execute(Processor processor) {
            sceDisplaySetHoldMode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplaySetHoldMode(processor);";
        }
    };
    public final HLEModuleFunction sceDisplaySetResumeModeFunction = new HLEModuleFunction("sceDisplay", "sceDisplaySetResumeMode") {

        @Override
        public final void execute(Processor processor) {
            sceDisplaySetResumeMode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplaySetResumeMode(processor);";
        }
    };
    public final HLEModuleFunction sceDisplaySetFrameBufFunction = new HLEModuleFunction("sceDisplay", "sceDisplaySetFrameBuf") {

        @Override
        public final void execute(Processor processor) {
            sceDisplaySetFrameBuf(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplaySetFrameBuf(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayGetFrameBufFunction = new HLEModuleFunction("sceDisplay", "sceDisplayGetFrameBuf") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayGetFrameBuf(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayGetFrameBuf(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayIsForegroundFunction = new HLEModuleFunction("sceDisplay", "sceDisplayIsForeground") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayIsForeground(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayIsForeground(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayGetBrightnessFunction = new HLEModuleFunction("sceDisplay", "sceDisplayGetBrightness") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayGetBrightness(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayGetBrightness(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayGetVcountFunction = new HLEModuleFunction("sceDisplay", "sceDisplayGetVcount") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayGetVcount(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayGetVcount(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayIsVblankFunction = new HLEModuleFunction("sceDisplay", "sceDisplayIsVblank") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayIsVblank(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayIsVblank(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayWaitVblankFunction = new HLEModuleFunction("sceDisplay", "sceDisplayWaitVblank") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayWaitVblank(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayWaitVblank(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayWaitVblankCBFunction = new HLEModuleFunction("sceDisplay", "sceDisplayWaitVblankCB") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayWaitVblankCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayWaitVblankCB(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayWaitVblankStartFunction = new HLEModuleFunction("sceDisplay", "sceDisplayWaitVblankStart") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayWaitVblankStart(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayWaitVblankStart(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayWaitVblankStartCBFunction = new HLEModuleFunction("sceDisplay", "sceDisplayWaitVblankStartCB") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayWaitVblankStartCB(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayWaitVblankStartCB(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayGetCurrentHcountFunction = new HLEModuleFunction("sceDisplay", "sceDisplayGetCurrentHcount") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayGetCurrentHcount(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayGetCurrentHcount(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayGetAccumulatedHcountFunction = new HLEModuleFunction("sceDisplay", "sceDisplayGetAccumulatedHcount") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayGetAccumulatedHcount(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayGetAccumulatedHcount(processor);";
        }
    };
    public final HLEModuleFunction sceDisplayAdjustAccumulatedHcountFunction = new HLEModuleFunction("sceDisplay", "sceDisplayAdjustAccumulatedHcount") {

        @Override
        public final void execute(Processor processor) {
            sceDisplayAdjustAccumulatedHcount(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceDisplayModule.sceDisplayAdjustAccumulatedHcount(processor);";
        }
    };
}