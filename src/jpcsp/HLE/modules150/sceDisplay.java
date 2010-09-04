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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.media.opengl.DebugGL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;

import jpcsp.Emulator;
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
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.RE.RenderingEngineFactory;
import jpcsp.graphics.RE.buffer.IREBufferManager;
import jpcsp.graphics.capture.CaptureManager;
import jpcsp.scheduler.UnblockThreadAction;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

import com.sun.opengl.util.Screenshot;

import org.apache.log4j.Logger;

public class sceDisplay extends GLCanvas implements GLEventListener, HLEModule, HLEStartModule {
    protected static Logger log = Modules.getLogger("sceDisplay");

	private static final long serialVersionUID = 2267866365228834812L;

    private static final boolean useReadPixels = false;
    private boolean onlyGEGraphics = false;
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

    // current Rendering Engine
    private IRenderingEngine re;
    private boolean startModules;
    private int drawBuffer;

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

    public DurationStatistics statistics;
    public DurationStatistics statisticsCopyGeToMemory;
    public DurationStatistics statisticsCopyMemoryToGe;

    // Async Display
    private AsyncDisplayThread asyncDisplayThread;
    private Semaphore displayLock;
    private boolean tryLockDisplay;
    private long tryLockTimestamp;

    // VBLANK Multi.
    private int vblankMultiCurrentVcount;
    private int vblankMultiCycleNum;
    private int vblankMultiThreadID = -1;

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
            // Check for previous VBLANK status (multi).
            if(vblankMultiThreadID > 0) {
                checkVblankMulti();
            }
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
			return sceDisplay.this.vcount == vcount;
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

    public static final GLCapabilities capabilities = new GLCapabilities();

    static {
    	capabilities.setStencilBits(8);
    	capabilities.setAlphaBits(8);
    }

    public sceDisplay() {
    	super (capabilities);

        setSize(480, 272);
        addGLEventListener(this);
        texFb = -1;
        startModules = false;
    }

    @Override
    public void start() {
        statistics = new DurationStatistics("sceDisplay Statistics");
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

        pixelsFb = getPixels(topaddrFb, bottomaddrFb);

        widthGe       = 480;
        heightGe      = 272;
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

        displayLock = new Semaphore(1);
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

    	// The VideoEngine needs to be started when a valid GL is available.
    	// Start the VideoEngine at the next display(GLAutoDrawable).
    	startModules = true;
    	re = null;
    }

    @Override
    public void stop() {
    	if (asyncDisplayThread != null) {
    		asyncDisplayThread.exit();
    		asyncDisplayThread = null;
    	}
    	re = null;
    	startModules = false;
    }

    public void exit() {
        if (statistics != null) {
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

    public void hleDisplaySetGeBuf(int topaddr, int bufferwidth, int pixelformat, boolean copyGEToMemory) {
    	hleDisplaySetGeBuf(topaddr, bufferwidth, pixelformat, copyGEToMemory, widthGe, heightGe);
    }

    public void hleDisplaySetGeBuf(int topaddr, int bufferwidth, int pixelformat, boolean copyGEToMemory, int width, int height) {
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

		boolean loadGEToScreen = true; // Always reload the GE memory to the screen
		if (copyGEToMemory && topaddrGe != topaddr) {
			if (VideoEngine.log.isDebugEnabled()) {
				VideoEngine.log.debug(String.format("Copy GE Screen to Memory 0x%08X-0x%08X", topaddrGe, bottomaddrGe));
			}

			if (statisticsCopyGeToMemory != null) {
				statisticsCopyGeToMemory.start();
			}

			// Set texFb as the current texture
		    re.bindTexture(texFb);

		    // Copy screen to the current texture
		    re.copyTexSubImage(0, 0, 0, 0, 0, widthGe, heightGe);

		    // Re-render GE/current texture upside down
		    drawFrameBuffer(true, true, bufferwidthGe, pixelformatGe, widthGe, heightGe);

		    copyScreenToPixels(pixelsGe, bufferwidthGe, pixelformatGe, widthGe, heightGe);
		    loadGEToScreen = true;

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

		if (loadGEToScreen) {
			if (VideoEngine.log.isDebugEnabled()) {
				VideoEngine.log.debug(String.format("Reloading GE Memory (0x%08X-0x%08X) to screen (%dx%d)", topaddrGe, bottomaddrGe, widthGe, heightGe));
			}

			if (statisticsCopyMemoryToGe != null) {
				statisticsCopyMemoryToGe.start();
			}

			// Set texFb as the current texture
			re.bindTexture(texFb);

			// Define the texture from the GE Memory
		    re.setPixelStore(bufferwidthGe, getPixelFormatBytes(pixelformatGe));
			re.setTexSubImage(0, 0, 0, bufferwidthGe, heightGe, pixelformatGe, pixelformatGe, pixelsGe);

			// Draw the GE
		    drawFrameBuffer(false, true, bufferwidthGe, pixelformatGe, widthGe, heightGe);

			if (statisticsCopyMemoryToGe != null) {
				statisticsCopyMemoryToGe.end();
			}

			if (State.captureGeNextFrame) {
		    	captureGeImage();
		    }
		}

        setGeBufCalledAtLeastOnce = true;
    }

    public static int getPixelFormatBytes(int pixelformat) {
        return pixelformat == PSP_DISPLAY_PIXEL_FORMAT_8888 ? 4 : 2;
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

    public BufferInfo getBufferInfoGe() {
    	return new BufferInfo(topaddrGe, bottomaddrGe, widthGe, heightGe, bufferwidthGe, pixelformatGe);
    }

    public BufferInfo getBufferInfoFb() {
    	return new BufferInfo(topaddrFb, bottomaddrFb, width, height, bufferwidthFb, pixelformatFb);
    }

    public void captureGeImage() {
    	// Create a GE texture (the texture texFb might not have the right size)
        int texGe = re.genTexture();

        re.bindTexture(texGe);
        re.setTexImage(0,
            internalTextureFormat,
            bufferwidthGe, Utilities.makePow2(heightGe),
            pixelformatGe,
            pixelformatGe, null);

        re.setTextureMipmapMinFilter(TFLT_NEAREST);
        re.setTextureMipmapMagFilter(TFLT_NEAREST);
        re.setTextureMipmapMinLevel(0);
        re.setTextureMipmapMaxLevel(0);
        re.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);
        re.setPixelStore(bufferwidthGe, getPixelFormatBytes(pixelformatGe));

        // Copy screen to the GE texture
        re.copyTexSubImage(0, 0, 0, 0, 0, widthGe, heightGe);

        // Copy the GE texture into temp buffer
        temp.clear();
        re.getTexImage(0, pixelformatGe, pixelformatGe, temp);

        // Capture the GE image
        CaptureManager.captureImage(topaddrGe, 0, temp, widthGe, heightGe, bufferwidthGe, pixelformatGe, false, 0, false);

    	// Delete the GE texture
        re.deleteTexture(texGe);
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

    /** @param first : true  = draw as psp size
     *                 false = draw as window size */
    private void drawFrameBuffer(boolean first, boolean invert, int bufferwidth, int pixelformat, int width, int height) {
        if(!isrotating){

            texS1 = texS4 = texS;
            texT1 = texT2 = texT;

            texS2 = texS3 = texT3 = texT4 = 0.0f;
        }

    	re.startDirectRendering(true, false, true, true, !invert, width, height);
        if (first) {
            re.setViewport(0, 0, width, height);
        } else {
            re.setViewport(0, 0, canvasWidth, canvasHeight);
        }

        re.setPixelStore(bufferwidth, getPixelFormatBytes(pixelformat));
        re.bindTexture(texFb);

        IREBufferManager bufferManager = re.getBufferManager();
        ByteBuffer drawByteBuffer = bufferManager.getBuffer(drawBuffer);
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

        re.enableClientState(IRenderingEngine.RE_TEXTURE);
        re.disableClientState(IRenderingEngine.RE_COLOR);
        re.disableClientState(IRenderingEngine.RE_NORMAL);
        re.enableClientState(IRenderingEngine.RE_VERTEX);
        bufferManager.setTexCoordPointer(drawBuffer, 2, IRenderingEngine.RE_FLOAT, 4 * SIZEOF_FLOAT, 0);
        bufferManager.setVertexPointer(drawBuffer, 2, IRenderingEngine.RE_FLOAT, 4 * SIZEOF_FLOAT, 2 * SIZEOF_FLOAT);
        bufferManager.setBufferData(drawBuffer, drawFloatBuffer.position() * SIZEOF_FLOAT, drawByteBuffer.rewind(), IRenderingEngine.RE_DYNAMIC_DRAW);
        re.setVertexInfo(null, false, false);
        re.drawArrays(IRenderingEngine.RE_QUADS, 0, 4);

        re.endDirectRendering();

        isrotating = false;

    }

    private void copyScreenToPixels(Buffer pixels, int bufferwidth, int pixelformat, int width, int height) {
    	re.setModelViewMatrix(null);
    	re.setTextureMatrix(null);

        // Using glReadPixels instead of glGetTexImage is showing
        // between 7 and 13% performance increase.
        // But glReadPixels seems only to work correctly with 32bit pixels...
    	// Update: glReadPixels has a poorer performance than glGetTextImage
    	// on modern graphic cards.
        if (useReadPixels && pixelformat == PSP_DISPLAY_PIXEL_FORMAT_8888) {
            re.setProjectionMatrix(VideoEngine.getOrthoMatrix(0, width, height, 0, -1, 1));
            int bufferStep = bufferwidth * getPixelFormatBytes(pixelformat);
            int widthToRead = Math.min(width, bufferwidth);
            // Y-Axis on PSP is flipped against OpenGL, so we have to copy row by row
            for (int y = 0, bufferPos = 0; y < height; y++, bufferPos += bufferStep) {
            	Utilities.bytePositionBuffer(pixels, bufferPos); // this uses reflection -> slow(?)
                re.readPixels(0, y, widthToRead, 1, pixelformat, pixelformat, pixels);
            }
        } else {
        	re.setProjectionMatrix(null);

        	// Set texFb as the current texture
            re.bindTexture(texFb);

            re.setPixelStore(bufferwidth, getPixelFormatBytes(pixelformat));

            // Copy screen to the current texture
            re.copyTexSubImage(0, 0, 0, 0, 0, width, height);

            // Copy the current texture into memory
            temp.clear();
            re.getTexImage(0, pixelformat, pixelformat, temp);

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
        // re.setProjectionMatrix(context.proj_uploaded_matrix);
        // re.setTextureMatrix(context.texture_uploaded_matrix);
        // re.setViewMatrix(context.view_uploaded_matrix);
        // re.setModelMatrix(context.model_uploaded_matrix);
    }

    protected void blockCurrentThreadOnVblank(boolean doCallbacks) {
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        int threadId = threadMan.getCurrentThreadID();

        // Add a Vblank action to unblock the thread
        UnblockThreadAction vblankAction = new UnblockThreadAction(threadId);
        IntrManager.getInstance().addVBlankActionOnce(vblankAction);

        // Block the current thread
        if (doCallbacks) {
        	threadMan.hleBlockCurrentThreadCB(null, new VblankWaitStateChecker(vcount));
        } else {
        	threadMan.hleBlockCurrentThread();
        }
    }

    protected void blockCurrentThreadOnVblankMulti(int cycles, boolean doCallbacks) {
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        // Save the current status.
        vblankMultiCurrentVcount = vcount;
        vblankMultiCycleNum = cycles;
        vblankMultiThreadID = threadMan.getCurrentThreadID();
        // Block the current thread.
        if (doCallbacks) {
        	threadMan.hleBlockCurrentThreadCB(null, new VblankWaitStateChecker(vcount));
        } else {
        	threadMan.hleBlockCurrentThread();
        }
    }

    protected void checkVblankMulti() {
        // If the current vcount matches the desired sum of cycles, add a new action.
        if(vcount == (vblankMultiCurrentVcount + vblankMultiCycleNum)) {
            // Add a Vblank action to unblock the thread.
            UnblockThreadAction vblankAction = new UnblockThreadAction(vblankMultiThreadID);
            IntrManager.getInstance().addVBlankActionOnce(vblankAction);
            // Reset the status.
            vblankMultiCurrentVcount = 0;
            vblankMultiCycleNum = 0;
            vblankMultiThreadID = -1;
        }
    }

    private void hleVblankStart() {
    	lastVblankMicroTime = Emulator.getClock().microTime();
    	// Vcount increases at each VBLANK.
    	vcount++;
    }

    private boolean isVblank() {
    	// Test result: isVblank == true during 4.39% of the time
    	// -> Vblank takes 731.5 micros at each vblank interrupt
    	long nowMicroTime = Emulator.getClock().microTime();
    	long microTimeSinceLastVblank = nowMicroTime - lastVblankMicroTime;

    	return (microTimeSinceLastVblank <= 731);
    }

	@Override
	public void display(GLAutoDrawable drawable) {
    	if (statistics != null) {
            statistics.start();
        }

    	if (re == null) {
    		if (startModules) {
    			re = RenderingEngineFactory.createRenderingEngine(drawable.getGL());
    		} else {
    			re = RenderingEngineFactory.createInitialRenderingEngine(drawable.getGL());
    		}
    	}

    	if (startModules) {
    		VideoEngine.getInstance().start();
        	drawBuffer = re.getBufferManager().genBuffer(IRenderingEngine.RE_FLOAT, 16, IRenderingEngine.RE_DYNAMIC_DRAW);
	    	startModules = false;
    	}

        if (createTex) {
            if (texFb != -1) {
            	re.deleteTexture(texFb);
            }
            texFb = re.genTexture();

            re.bindTexture(texFb);

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
                bufferwidthFb, Utilities.makePow2(height),
                pixelformatFb,
                pixelformatFb, null);
            re.setTextureMipmapMinFilter(GeCommands.TFLT_NEAREST);
            re.setTextureMipmapMagFilter(GeCommands.TFLT_NEAREST);
            re.setTextureMipmapMinLevel(0);
            re.setTextureMipmapMaxLevel(0);
            re.setTextureWrapMode(TWRAP_WRAP_MODE_CLAMP, TWRAP_WRAP_MODE_CLAMP);

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
        	re.clear(0.0f, 0.0f, 0.0f, 0.0f);
            return;
        }

        if (onlyGEGraphics) {
        	re.startDisplay();
            VideoEngine.getInstance().update();
            re.endDisplay();
        } else {
        	re.startDisplay();

        	// Render GE
            re.setViewport(0, 0, width, height);

            // If the GE is not at the same address as the FrameBuffer,
            // redisplay the GE so that the VideoEngine can update it
            if (bottomaddrGe != bottomaddrFb) {
	            pixelsGe.clear();
	            re.bindTexture(texFb);

	            // An alternative to glTexSubImage2D would be to use glDrawPixels to
	            // render the frame buffer.
	            // But glDrawPixels is showing around 10% performance decrease
	            // against glTexSubImage2D.

	            // Use format without alpha channel if the source buffer doesn't have one
				re.setTexSubImage(0,
	                0, 0, bufferwidthGe, heightGe,
	                pixelformatGe,
	                pixelformatGe, pixelsGe);

	            drawFrameBuffer(false, true, bufferwidthFb, pixelformatFb, width, height);
            }

            if (VideoEngine.getInstance().update()) {
                // Update VRAM only if GE actually drew something
                // Set texFb as the current texture
                re.bindTexture(texFb);

                // Copy screen to the current texture
                re.copyTexSubImage(0, 0, 0, 0, 0, widthGe, heightGe);

                // Re-render GE/current texture upside down
                drawFrameBuffer(true, true, bufferwidthFb, pixelformatFb, width, height);

                // Save GE/current texture to vram
                copyScreenToPixels(pixelsGe, bufferwidthGe, pixelformatGe, widthGe, heightGe);
            }

            // Render FB
            pixelsFb.clear();
            re.bindTexture(texFb);
            re.setTexSubImage(0,
                0, 0, bufferwidthFb, height,
                pixelformatFb,
                pixelformatFb, pixelsFb);

            //Call the rotating function (if needed)
            if(ang != 4)
                rotate(ang);

            drawFrameBuffer(false, true, bufferwidthFb, pixelformatFb, width, height);

            re.endDisplay();
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
	public void displayChanged(GLAutoDrawable drawable, boolean arg1, boolean arg2) {
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		// Use DebugGL to get exceptions when some OpenGL operation fails
    	// Not safe to use in release mode since people could have a few exceptions
    	// that remained silent when some operations fails but still output the
    	// intended result
    	if (useDebugGL) {
    		drawable.setGL(new DebugGL(drawable.getGL()));
    	}
        drawable.getGL().setSwapInterval(1);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		canvasWidth  = width;
        canvasHeight = height;
	}

    public void sceDisplaySetMode(Processor processor) {
        CpuState cpu = processor.cpu;

        int displayMode = cpu.gpr[4];
        int displayWidth = cpu.gpr[5];
        int displayHeight = cpu.gpr[6];

        log.debug("sceDisplaySetMode(mode=" + displayMode + ",width=" + displayWidth + ",height=" + displayHeight + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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

        if (!memory.isAddressGood(modeAddr  ) ||
            !memory.isAddressGood(widthAddr ) ||
            !memory.isAddressGood(heightAddr))
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceDisplaySetResumeMode(Processor processor) {
        CpuState cpu = processor.cpu;

        int resumeMode = cpu.gpr[4];

        log.warn("IGNORING: sceDisplaySetResumeMode resumeMode=" + resumeMode);

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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

        cpu.gpr[2] = hleDisplaySetFrameBuf(topaddr, bufferwidth, pixelformat, syncType);
    }

    public int hleDisplaySetFrameBuf(int topaddr, int bufferwidth, int pixelformat, int syncType) {
		topaddr &= Memory.addressMask;

        if (bufferwidth <= 0 || (bufferwidth & (bufferwidth - 1)) != 0 ||
            pixelformat < 0 || pixelformat > 3 ||
            syncType < 0 || syncType > 1) {
            log.warn(
                "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                ",bufferwidth=" + bufferwidth +
                ",pixelformat=" + pixelformat +
                ",syncType=" + syncType + ") bad params");
            isFbShowing = false;
            gotBadFbBufParams = true;
            return -1;
        } else if (topaddr == 0) {
            // Got 0 as topaddr, but it's ok, it will be correctly set on the
            // next call (tested and checked).
            log.warn(
                "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                ",bufferwidth=" + bufferwidth +
                ",pixelformat=" + pixelformat +
                ",syncType=" + syncType + ") bad params (topaddr==0)");
            isFbShowing = false;
            gotBadFbBufParams = true;
            return 0;
        } else if (Memory.getInstance().isAddressGood(topaddr)){
            if(topaddr < MemoryMap.START_VRAM || topaddr >= MemoryMap.END_VRAM) {
                log.warn("sceDisplaySetFrameBuf (topaddr=0x" + Integer.toHexString(topaddr) + ")"
                        + " is using main memory.");
            }

            if (gotBadFbBufParams) {
                gotBadFbBufParams = false;
                log.info(
                    "sceDisplaySetFrameBuf(topaddr=0x" + Integer.toHexString(topaddr) +
                    ",bufferwidth=" + bufferwidth +
                    ",pixelformat=" + pixelformat +
                    ",syncType=" + syncType + ") ok");
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
            return 0;
        }
        return -1;
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

        if (!mem.isAddressGood(topaddrAddr    ) ||
            !mem.isAddressGood(bufferwidthAddr) ||
            !mem.isAddressGood(pixelformatAddr)) {
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
        blockCurrentThreadOnVblank(false);
    }

    public void sceDisplayWaitVblankStartCB(Processor processor) {
        CpuState cpu = processor.cpu;

        if (log.isDebugEnabled()) {
        	log.debug("sceDisplayWaitVblankStartCB");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
        blockCurrentThreadOnVblank(true);
    }

    public void sceDisplayGetCurrentHcount(Processor processor) {
        CpuState cpu = processor.cpu;
        cpu.gpr[2] = (int) (vcount * 0.15f);
    }

    public void sceDisplayGetAccumulatedHcount(Processor processor) {
        CpuState cpu = processor.cpu;
        // 17143 units per second
        cpu.gpr[2] = (int) (vcount * 285.72f);
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