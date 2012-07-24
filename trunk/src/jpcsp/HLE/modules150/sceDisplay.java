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
import static jpcsp.util.Utilities.makePow2;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer32;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import jpcsp.Emulator;
import jpcsp.MainGUI;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
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
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.graphics.GEProfiler;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.RenderingEngineFactory;
import jpcsp.graphics.RE.RenderingEngineLwjgl;
import jpcsp.graphics.RE.buffer.IREBufferManager;
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
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.AWTGLCanvas;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

public class sceDisplay extends HLEModule {
    protected static Logger log = Modules.getLogger("sceDisplay");

    @SuppressWarnings("serial")
	class AWTGLCanvas_sceDisplay extends AWTGLCanvas {

		public AWTGLCanvas_sceDisplay() throws LWJGLException {
			super(null, new PixelFormat().withBitsPerPixel(8).withAlphaBits(8).withStencilBits(8).withSamples(antiAliasSamplesNum), null, new ContextAttribs().withDebug(useDebugGL));
		}

		@Override
		protected void paintGL() {
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
	    		TextureCache.getInstance().reset(re);
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
	    				reDisplay.setGeContext(VideoEngine.getInstance().getContext());
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
	    		VideoEngine.getInstance().start();
	        	drawBuffer = reDisplay.getBufferManager().genBuffer(IRenderingEngine.RE_FLOAT, 16, IRenderingEngine.RE_DYNAMIC_DRAW);
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

	    	// If we are not rendering this frame, skip the next sceDisplaySetFrameBuf call,
	    	// assuming the application is doing double buffering.
			skipNextFrameBufferSwitch = VideoEngine.getInstance().isSkipThisFrame();

	        if (isUsingSoftwareRenderer()) {
	        	// Software rendering: the processing of the GE list is done by the
	        	// SoftwareRenderingDisplayThread.
	        	// We just need to display the frame buffer.
	        	if (softwareRenderingDisplayThread == null) {
	        		re.startDisplay();
	        		VideoEngine.getInstance().update();
	        		re.endDisplay();
	        	}
	        	reDisplay.startDisplay();
	        	drawFrameBufferFromMemory();
	        	reDisplay.endDisplay();
	        } else if (onlyGEGraphics) {
	        	// Hardware rendering where only the currently rendered GE list is displayed,
	        	// not the frame buffer from memory.
	        	re.startDisplay();
	            VideoEngine.getInstance().update();
	            re.endDisplay();
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
	            if (VideoEngine.getInstance().update()) {
	                // Save the GE only if it actually drew something
	            	if (log.isDebugEnabled()) {
	            		log.debug(String.format("sceDisplay.paintGL - saving the GE to memory 0x%08X", topaddrGe));
	            	}

	            	if (saveGEToTexture && !VideoEngine.getInstance().isVideoTexture(topaddrGe)) {
	            		GETexture geTexture = GETextureManager.getInstance().getGETexture(reDisplay, topaddrGe, bufferwidthGe, widthGe, heightGe, pixelformatGe, true);
	            		geTexture.copyScreenToTexture(re);
	            	} else {
		                // Set texFb as the current texture
	            		reDisplay.bindTexture(resizedTexFb);
	            		reDisplay.setTextureFormat(pixelformatGe, false);

		                // Copy screen to the current texture
	            		reDisplay.copyTexSubImage(0, 0, 0, 0, 0, getResizedWidth(widthGe), getResizedHeight(heightGe));

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
	        		GETexture geTexture = GETextureManager.getInstance().getGETexture(reDisplay, topaddrFb, bufferwidthFb, width, height, pixelformatFb, true);
	        		geTexture.copyTextureToScreen(reDisplay);
	        	} else {
	        		drawFrameBufferFromMemory();
	        	}

	            re.endDisplay();

	            if (log.isDebugEnabled()) {
	        		log.debug("sceDisplay.paintGL - end display");
	        	}
	        }

	        // Perform OpenGL double buffering
	        try {
	        	canvas.swapBuffers();
			} catch (LWJGLException e) {
			}

	        // Update the current FPS every second
	        reportFPSStats();

	        if (statistics != null) {
	            statistics.end();
	        }

	        if (getscreen) {
	        	saveScreen();
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
			if (log.isTraceEnabled()) {
				log.trace(String.format("setBounds width=%d, height=%d", width, height));
			}
			canvasWidth = width;
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
    }

    protected AWTGLCanvas_sceDisplay canvas;
    
    public AWTGLCanvas getCanvas() {
    	return canvas;
    }

    private boolean onlyGEGraphics = false;
    private boolean saveGEToTexture = false;
    private boolean useSoftwareRenderer = false;
    private static final boolean useDebugGL = false;
    private static final int internalTextureFormat = GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
    static public boolean ignoreLWJGLError = false;

    // sceDisplayModes enum
    public static final int PSP_DISPLAY_MODE_LCD  = 0;
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
    private boolean resetDisplaySettings;

    // current display mode
    private int mode;
    private int width;
    private int height;
    private int widthGe;
    private int heightGe;

    // Resizing options
    private static float viewportResizeFilterScaleFactor = 1f;
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
    private boolean calledFromCommandLine;

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
    private SoftwareRenderingDisplayThread softwareRenderingDisplayThread;

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
			if (isStarted) {
				resetDisplaySettings = true;
			}
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

    private static class SoftwareRenderingDisplayThread  extends AbstractDisplayThread {
    	@Override
		protected void doDisplay() {
			IRenderingEngine re = Modules.sceDisplayModule.getRenderingEngine();
			if (re != null && VideoEngine.getInstance().hasDrawLists()) {
	        	re.startDisplay();
	        	VideoEngine.getInstance().update();
	        	re.endDisplay();
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
			return sceDisplay.this.vcount < vcount;
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

    	canvas = new AWTGLCanvas_sceDisplay();
        setScreenResolution(Screen.width, Screen.height);

        texFb = -1;
        resizedTexFb = -1;
        startModules = false;
        isStarted = false;
        resizePending = false;
        tempSize = 0;
    }

    public void setDesiredFPS(int desiredFPS) {
    	this.desiredFps = desiredFPS;
    }

    public int getDesiredFPS() {
    	return desiredFps;
    }

    public void setScreenResolution(int width, int height) {
        canvasWidth = width;
        canvasHeight = height;
        canvas.setSize(width, height);
    }

    public float getViewportResizeScaleFactor() {
        return viewportResizeFilterScaleFactor;
    }

    public void setViewportResizeScaleFactor(int width, int height) {
    	// Compute the scale factor in the horizontal and vertical directions
        float scaleWidth = ((float) width) / Screen.width;
        float scaleHeight = ((float) height) / Screen.height;

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

    public void setViewportResizeScaleFactor(float viewportResizeFilterScaleFactor) {
    	if (viewportResizeFilterScaleFactor < 1) {
    		// Invalid value
    		return;
    	}

    	if (viewportResizeFilterScaleFactor != sceDisplay.viewportResizeFilterScaleFactor) {
    		sceDisplay.viewportResizeFilterScaleFactor = viewportResizeFilterScaleFactor;
    		sceDisplay.viewportResizeFilterScaleFactorInt = Math.round((float) Math.ceil(viewportResizeFilterScaleFactor));

    		Dimension size = new Dimension(getResizedWidth(Screen.width), getResizedHeight(Screen.height));

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
     * @param wantedWidth        value on the X-Axis to be resized, must be a power of 2.
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
     * @param wantedWidth        value on the Y-Axis to be resized, must be a power of 2.
     * @return             the resized value, as a power of 2.
     */
    public final static int getResizedHeightPow2(int heightPow2) {
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
        	if (!ignoreLWJGLError)
        	{
        		throw new RuntimeException("Jpcsp cannot display.\nThe cause could be that you are using an old graphic card driver (try to update it)\nor your display format is not compatible with Jpcsp (try to change your display format, Jpcsp requires 32 bit color depth)\nor the anti-aliasing settings is not supported by your display (leave the Jpcsp anti-aliasing to its default setting)");
        	}
        }

        // Reset the FB and GE settings only when not called from a syscall.
        // E.g. sceKernelLoadExec() is not clearing/resetting the display.
        if (!HLEModuleManager.getInstance().isStartFromSyscall()) {
	        mode          = 0;
	        width         = Screen.width;
	        height        = Screen.height;
	        topaddrFb     = MemoryMap.START_VRAM;
	        bufferwidthFb = 512;
	        pixelformatFb = GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
	        sync          = PSP_DISPLAY_SETBUF_IMMEDIATE;

	        bottomaddrFb = topaddrFb + bufferwidthFb * height * getPixelFormatBytes(pixelformatFb);

	        pixelsFb = getPixels(topaddrFb, bottomaddrFb);

	        widthGe       = Screen.width;
	        heightGe      = Screen.height;
	        topaddrGe     = topaddrFb;
	        bufferwidthGe = bufferwidthFb;
	        pixelformatGe = pixelformatFb;
	        bottomaddrGe  = bottomaddrFb;
	        pixelsGe = getPixels(topaddrGe, bottomaddrGe);

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
    	reDisplay = null;
    	resetDisplaySettings = false;

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

    	setSettingsListener("emu.onlyGEGraphics", new OnlyGeSettingsListener());
    	setSettingsListener("emu.useSoftwareRenderer", new SoftwareRendererSettingsListener());

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

        if (dirty && softwareRenderingDisplayThread != null) {
        	// Start immediately the software rendering.
        	// No need to wait for the OpenGL display call.
        	softwareRenderingDisplayThread.display();
        }
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

        // Do not copy the GE to memory or reload it if we are using the software
        // renderer or skipping this frame.
        if (isUsingSoftwareRenderer() || VideoEngine.getInstance().isSkipThisFrame()) {
        	copyGEToMemory = false;
        	forceLoadGEToScreen = false;
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
            (sync != PSP_DISPLAY_SETBUF_IMMEDIATE && sync != PSP_DISPLAY_SETBUF_NEXTFRAME)) {
            String msg = "hleDisplaySetGeBuf bad params ("
                + Integer.toHexString(topaddr)
                + "," + bufferwidth
                + "," + pixelformat + ")";

            // First time is usually initializing GE, so we can ignore it
            if (setGeBufCalledAtLeastOnce) {
                log.warn(msg);
                gotBadGeBufParams = true;
            } else {
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

    	// Always reload the GE memory to the screen,
    	// if not rendering in software and not skipping this frame.
		boolean loadGEToScreen = !isUsingSoftwareRenderer() && !VideoEngine.getInstance().isSkipThisFrame();

		if (copyGEToMemory && (topaddrGe != topaddr || pixelformatGe != pixelformat)) {
			copyGeToMemory(false);
		    loadGEToScreen = true;
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

    private void setOnlyGEGraphics(boolean onlyGEGraphics) {
        this.onlyGEGraphics = onlyGEGraphics;
        VideoEngine.log.info("Only GE Graphics: " + onlyGEGraphics);
    }

    private void setUseSoftwareRenderer(boolean useSoftwareRenderer) {
    	this.useSoftwareRenderer = useSoftwareRenderer;

    	// Start/stop the software rendering display thread
    	if (useSoftwareRenderer) {
    		if (softwareRenderingDisplayThread == null) {
//    			softwareRenderingDisplayThread = new SoftwareRenderingDisplayThread();
//    			softwareRenderingDisplayThread.setDaemon(true);
//    			softwareRenderingDisplayThread.setName("Software Rendering Display Thread");
//    			softwareRenderingDisplayThread.start();
//    			log.debug("Starting Software Rendering Display Thread");
    		}
    	} else {
    		if (softwareRenderingDisplayThread != null) {
    			log.debug("Stopping Software Rendering Display Thread");
    			softwareRenderingDisplayThread.exit();
    			softwareRenderingDisplayThread = null;
    		}
    	}
    }

    public boolean isUsingSoftwareRenderer() {
    	return useSoftwareRenderer;
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
        String fileName = State.discId + "-" + "Shot" + "-" + tag + ".png";
        File screenshot = new File(fileName);
        File directory = new File(System.getProperty("user.dir"));

        for(File file : directory.listFiles()) {
            if (file.getName().contains(State.discId + "-" + "Shot")) {
                fileName = State.discId + "-" + "Shot" + "-" + ++tag + ".png";
                screenshot = new File(fileName);
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
    	if (isUsingSoftwareRenderer()) {
    		Buffer buffer = Memory.getInstance().getBuffer(topaddrGe, bufferwidthGe * heightGe * getPixelFormatBytes(pixelformatGe));
    		CaptureManager.captureImage(topaddrGe, 0, buffer, widthGe, heightGe, bufferwidthGe, pixelformatGe, false, 0, false, false);
    		return;
    	}

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
        long timeNow = System.currentTimeMillis();
        long realElapsedTime = timeNow - prevStatsTime;

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

    public void copyGeToMemory(boolean preserveScreen) {
    	if (isUsingSoftwareRenderer()) {
    		// GE is already in memory when using the software renderer
    		return;
    	}

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

		    if (preserveScreen) {
		    	// Redraw the screen
		    	drawFrameBuffer(true, false, bufferwidthGe, pixelformatGe, widthGe, heightGe);
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
     * @param keepOriginalSize : true  = draw as psp size
     *                           false = draw as window size
     **/
    private void drawFrameBuffer(boolean keepOriginalSize, boolean invert, int bufferwidth, int pixelformat, int width, int height) {
        if (!isrotating) {
            texS1 = texS4 = texS;
            texT1 = texT2 = texT;

            texS2 = texS3 = texT3 = texT4 = 0.0f;
        }

        reDisplay.startDirectRendering(true, false, true, true, !invert, width, height);
        if (keepOriginalSize) {
        	reDisplay.setViewport(0, 0, width, height);
        } else {
        	reDisplay.setViewport(0, 0, getResizedWidth(width), getResizedHeight(height));
        }

        reDisplay.setTextureFormat(pixelformat, false);

        IREBufferManager bufferManager = reDisplay.getBufferManager();
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
        bufferManager.setBufferData(drawBuffer, drawFloatBuffer.position() * SIZEOF_FLOAT, drawByteBuffer.rewind(), IRenderingEngine.RE_DYNAMIC_DRAW);
        reDisplay.drawArrays(IRenderingEngine.RE_QUADS, 0, 4);

        reDisplay.endDirectRendering();

        isrotating = false;
    }

    private void drawFrameBufferFromMemory() {
        pixelsFb.clear();
        reDisplay.bindTexture(texFb);
        reDisplay.setTextureFormat(pixelformatFb, false);
        reDisplay.setPixelStore(bufferwidthFb, pixelformatFb);
        int textureSize = bufferwidthFb * height * getPixelFormatBytes(pixelformatFb);
        reDisplay.setTexSubImage(0,
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
    	reDisplay.setTextureFormat(pixelformatFb, false);

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
        	reDisplay.deleteTexture(textureId);
        }
        textureId = reDisplay.genTexture();

        reDisplay.bindTexture(textureId);
        reDisplay.setTextureFormat(pixelformatFb, false);

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
            isResized ? getResizedWidthPow2(bufferwidthFb) : bufferwidthFb,
            isResized ? getResizedHeightPow2(Utilities.makePow2(height)) : Utilities.makePow2(height),
            pixelformatFb,
            pixelformatFb,
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

	public void setCalledFromCommandLine() {
		calledFromCommandLine = true;
	}

	@HLEFunction(nid = 0x0E20F177, version = 150, checkInsideInterrupt = true)
    public int sceDisplaySetMode(int displayMode, int displayWidth, int displayHeight) {
        if (log.isDebugEnabled()) {
        	log.debug("sceDisplaySetMode(mode=" + displayMode + ",width=" + displayWidth + ",height=" + displayHeight + ")");
        }

        if (displayWidth <= 0 || displayHeight <= 0) {
            return SceKernelErrors.ERROR_INVALID_SIZE;
        }
        
        if (displayMode != PSP_DISPLAY_MODE_LCD) {
            return SceKernelErrors.ERROR_INVALID_MODE;
        }

        this.mode   = displayMode;
        this.width  = displayWidth;
        this.height = displayHeight;

        bottomaddrFb =
            topaddrFb + bufferwidthFb * displayHeight *
            getPixelFormatBytes(pixelformatFb);
        pixelsFb = getPixels(topaddrFb, bottomaddrFb);

        detailsDirty = true;

        return 0;
    }

    @HLEFunction(nid = 0xDEA197D4, version = 150)
    public int sceDisplayGetMode(TPointer32 modeAddr, TPointer32 widthAddr, TPointer32 heightAddr) {
        modeAddr.setValue(mode );
        widthAddr.setValue(width);
        heightAddr.setValue(height);
        
        return 0;
    }
    
    @HLEFunction(nid = 0xDBA6C4C4, version = 150)
    public float sceDisplayGetFramePerSec() {
    	if (log.isDebugEnabled()) {
    		log.debug("sceDisplayGetFramePerSec ret: " + FRAME_PER_SEC);
    	}
    	return FRAME_PER_SEC;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7ED59BC4, version = 150, checkInsideInterrupt = true)
    public int sceDisplaySetHoldMode(int holdMode) {
        log.warn("IGNORING: sceDisplaySetHoldMode holdMode=" + holdMode);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA544C486, version = 150, checkInsideInterrupt = true)
    public int sceDisplaySetResumeMode(int resumeMode) {
        log.warn("IGNORING: sceDisplaySetResumeMode resumeMode=" + resumeMode);

        return 0;
    }

    @HLEFunction(nid = 0x289D82FE, version = 150)
    public int sceDisplaySetFrameBuf(int topaddr, int bufferwidth, int pixelformat, int syncType) {
        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceDisplaySetFrameBuf(topaddr=0x%08X, bufferwidth=%d, pixelformat=%d, syncType=%d)", topaddr, bufferwidth, pixelformat, syncType));
        }

        return hleDisplaySetFrameBuf(topaddr, bufferwidth, pixelformat, syncType);
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
        }
        if (pixelformat < 0 || pixelformat > 3) {
            log.warn(
                "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                ", bufferwidth=" + bufferwidth +
                ", pixelformat=" + pixelformat +
                ", syncType=" + syncType + ") bad pixelformat");
            isFbShowing = false;
            gotBadFbBufParams = true;
            return -1;
        }
        if (syncType != PSP_DISPLAY_SETBUF_IMMEDIATE && syncType != PSP_DISPLAY_SETBUF_NEXTFRAME) {
            log.warn(
                "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                ", bufferwidth=" + bufferwidth +
                ", pixelformat=" + pixelformat +
                ", syncType=" + syncType + ") bad syncType");
            isFbShowing = false;
            gotBadFbBufParams = true;
            return SceKernelErrors.ERROR_INVALID_MODE;
        }
        if (topaddr == 0) {
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
        }
        if (!Memory.isAddressGood(topaddr)) {
            return SceKernelErrors.ERROR_INVALID_POINTER;
        }

        if (topaddr != topaddrFb) {
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
        	if (topaddr != topaddrFb) {
        		Memory.getInstance().memcpy(topaddr, topaddrFb, bottomaddrFb - topaddrFb);
        	}
    		skipNextFrameBufferSwitch = false;
    	}

        if (gotBadFbBufParams) {
            gotBadFbBufParams = false;
            log.info(String.format("sceDisplaySetFrameBuf(topaddr=0x%08X, bufferwidth=%d, pixelformat=%d, syncType=%d ok", topaddr, bufferwidth, pixelformat, syncType));
        }

        if (pixelformat != pixelformatFb || bufferwidth != bufferwidthFb || makePow2(height) != makePow2(height)) {
            createTex = true;
        }

        topaddrFb     = topaddr;
        bufferwidthFb = bufferwidth;
        pixelformatFb = pixelformat;
        sync          = syncType;

        bottomaddrFb = topaddr + bufferwidthFb * height * getPixelFormatBytes(pixelformatFb);
        pixelsFb = getPixels(topaddrFb, bottomaddrFb);

        texS = (float) width / (float) bufferwidth;
        texT = (float) height / (float) makePow2(height);

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

    /**
     * @param topaddrAddr       - 
     * @param bufferwidthAddr   - 
     * @param pixelformatAddr   - 
     * @param syncType          - 0 or 1. All other value than 1 is interpreted as 0.
     * @return
     */
    @HLEFunction(nid = 0xEEDA2E54, version = 150)
    public int sceDisplayGetFrameBuf(TPointer32 topaddrAddr, TPointer32 bufferwidthAddr, TPointer32 pixelformatAddr, int syncType) {
        if (log.isDebugEnabled()) {
    		log.debug(String.format(
    			"sceDisplayGetFrameBuf topaddrAddr=0x%08X, bufferwidthAddr=0x%08X, pixelformatAddr=0x%08X, sync=%d",
    			topaddrAddr.getAddress(), bufferwidthAddr.getAddress(), pixelformatAddr.getAddress(), syncType
    		));
        }

        topaddrAddr.setValue(topaddrFb);
        bufferwidthAddr.setValue(bufferwidthFb);
        pixelformatAddr.setValue(pixelformatFb);
        
        return 0;
    }

    @HLEFunction(nid = 0xB4F378FA, version = 150)
    public boolean sceDisplayIsForeground() {
        if (log.isDebugEnabled()) {
    		log.debug("sceDisplayIsForeground ret: " + isFbShowing);
    	}
        return isFbShowing;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x31C4BAA8, version = 150)
    public int sceDisplayGetBrightness(int leveladdr, int unkaddr) {
        log.warn("IGNORING: sceDisplayGetBrightness leveladdr=0x"
                + Integer.toHexString(leveladdr) + ", unkaddr=0x"
                + Integer.toHexString(unkaddr));
        
        return 0;
    }

    @HLEFunction(nid = 0x9C6EAAD7, version = 150)
    public int sceDisplayGetVcount() {
        // 60 units per second
        return vcount;
    }

    @HLEFunction(nid = 0x4D4E10EC, version = 150)
    public void sceDisplayIsVblank(Processor processor) {
        CpuState cpu = processor.cpu;

        cpu.gpr[2] = isVblank() ? 1 : 0;

        if (log.isDebugEnabled()) {
        	log.debug("sceDisplayIsVblank returns " + cpu.gpr[2]);
        }
    }

    @HLEFunction(nid = 0x36CDFADE, version = 150, checkInsideInterrupt = true)
    public int sceDisplayWaitVblank() {
        if (log.isDebugEnabled()) {
        	log.debug("sceDisplayWaitVblank");
        }

        if (!isVblank()) {
    		sceDisplayWaitVblankStart();
    	}
    	return 0;
    }

    @HLEFunction(nid = 0x8EB9EC49, version = 150, checkInsideInterrupt = true)
    public int sceDisplayWaitVblankCB() {
        if (log.isDebugEnabled()) {
        	log.debug("sceDisplayWaitVblankCB");
        }

        if (!isVblank()) {
        	sceDisplayWaitVblankStartCB();
        }

        return 0;
    }

    @HLEFunction(nid = 0x984C27E7, version = 150, checkInsideInterrupt = true)
    public int sceDisplayWaitVblankStart() {
        if (log.isDebugEnabled()) {
        	log.debug("sceDisplayWaitVblankStart");
        }

        blockCurrentThreadOnVblank(1, false);
        return 0;
    }

    @HLEFunction(nid = 0x46F186C3, version = 150, checkInsideInterrupt = true)
    public int sceDisplayWaitVblankStartCB() {
        if (log.isDebugEnabled()) {
        	log.debug("sceDisplayWaitVblankStartCB");
        }

        blockCurrentThreadOnVblank(1, true);
        return 0;
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

}