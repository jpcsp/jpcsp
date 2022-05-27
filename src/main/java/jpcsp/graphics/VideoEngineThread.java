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

import static jpcsp.MemoryMap.START_VRAM;
import static jpcsp.graphics.GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
import static jpcsp.graphics.VideoEngine.maxDrawingBufferWidth;
import static jpcsp.graphics.VideoEngine.maxDrawingHeight;
import static jpcsp.graphics.VideoEngine.maxDrawingWidth;
import static jpcsp.graphics.VideoEngineUtilities.canShareContext;
import static jpcsp.graphics.VideoEngineUtilities.createContext;
import static jpcsp.graphics.VideoEngineUtilities.setContext;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.lwjgl.opengl.GL;

import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.graphics.textures.FBTexture;

/**
 * Thread rendering the video command lists submitted by the sceGe_user module.
 * 
 * @author gid15
 *
 */
public class VideoEngineThread extends Thread {
	private static Logger log = VideoEngine.log;
    private final Semaphore update = new Semaphore(1);
    private final long context;
    private volatile boolean run = true;
    private volatile boolean doInterpretInstruction;
    private volatile int interpretInstruction;

    public static boolean isActive() {
    	return canShareContext();
    }

    public VideoEngineThread() {
		context = createContext();
	}

	@Override
	public void run() {
		RuntimeContext.setLog4jMDC();
		final VideoEngine videoEngine = VideoEngine.getInstance();
		final IRenderingEngine re = videoEngine.getRenderingEngine();
		final sceDisplay displayModule = Modules.sceDisplayModule;

		if (log.isDebugEnabled()) {
			log.debug(String.format("Start of Video Engine Thread with context=0x%X", context));
		}

		setContext(context);

        GL.createCapabilities();

        re.startDisplay();

        FBTexture renderTexture = new FBTexture(START_VRAM, maxDrawingBufferWidth, maxDrawingWidth, maxDrawingHeight, TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888);

		while (run) {
			waitForUpdate();

			if (doInterpretInstruction) {
				videoEngine.executeCommand(interpretInstruction);
				interpretInstruction = 0;
				doInterpretInstruction = false;
			} else {
				displayModule.lockDisplay();
				renderTexture.bind(re, false);
				re.startDisplay();

				videoEngine.update();

				displayModule.unlockDisplay();
			}
		}
	}

    public void update() {
    	update.release();
    }

    private void waitForUpdate() {
        while (true) {
            try {
                int availablePermits = update.drainPermits();
                if (availablePermits > 0) {
                    break;
                }

                if (update.tryAcquire(100, TimeUnit.MILLISECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
            }
        }
    }

    public void exit() {
        run = false;
        update();
    }

    public void interpretInstruction(int instruction) {
    	interpretInstruction = instruction;
    	doInterpretInstruction = true;

    	update();

    	// Active polling as this should complete very quickly
    	while (doInterpretInstruction) {
    		// Nothing to do
    	}
    }
}
