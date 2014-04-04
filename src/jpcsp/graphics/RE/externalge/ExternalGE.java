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
package jpcsp.graphics.RE.externalge;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.HLE.modules.sceGe_user;
import jpcsp.graphics.capture.CaptureManager;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.DurationStatistics;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class ExternalGE {
	public static final boolean enableAsyncRendering = false;
	public static       boolean activateWhenAvailable = true;
	public static final boolean useUnsafe = false;
	public static Logger log = Logger.getLogger("externalge");
	private static ConcurrentLinkedQueue<PspGeList> drawListQueue;
	private static volatile PspGeList currentList;
	private static RendererThread[] rendererThreads;
	private static Semaphore rendererThreadsDone;
	private static Level logLevel;
	private static SetLogLevelThread setLogLevelThread;
	private static int screenScale = 1;
	private static Object screenScaleLock = new Object();
	private static ExternalGESettingsListerner externalGESettingsListerner;

	private static class SetLogLevelThread extends Thread {
		private volatile boolean exit;

		public void exit() {
			exit = true;
		}

		@Override
		public void run() {
			while (!exit) {
				NativeUtils.setLogLevel();
				Utilities.sleep(100);
			}
		}
	}

    private static class ExternalGESettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			activateWhenAvailable = value;
			init();
		}
    }

    private static void activate() {
		drawListQueue = new ConcurrentLinkedQueue<PspGeList>();

		setLogLevelThread = new SetLogLevelThread();
		setLogLevelThread.setName("ExternelGE Set Log Level Thread");
		setLogLevelThread.setDaemon(true);
		setLogLevelThread.start();

		if (enableAsyncRendering) {
			rendererThreads = new RendererThread[2];
			rendererThreads[0] = new RendererThread(0xFF00FF00);
			rendererThreads[1] = new RendererThread(0x00FF00FF);
			for (int i = 0; i < rendererThreads.length; i++) {
				rendererThreads[i].setName(String.format("Renderer Thread #%d", i));
				rendererThreads[i].start();
			}
			rendererThreadsDone = new Semaphore(0);
		}
		NativeUtils.setRendererAsyncRendering(enableAsyncRendering);
		setScreenScale(sceDisplay.getResizedWidthPow2(1));
		synchronized (screenScaleLock) {
			NativeUtils.setScreenScale(getScreenScale());
		}
    }

    private static void deactivate() {
    	drawListQueue = null;

    	if (setLogLevelThread != null) {
    		setLogLevelThread.exit();
    		setLogLevelThread = null;
    	}

    	CoreThread.exit();

    	if (rendererThreads != null) {
			for (int i = 0; i < rendererThreads.length; i++) {
				rendererThreads[i].exit();
			}
			rendererThreads = null;
		}
    }

    public static void init() {
    	if (externalGESettingsListerner == null) {
    		externalGESettingsListerner = new ExternalGESettingsListerner();
    		Settings.getInstance().registerSettingsListener("ExternalGE", "emu.useExternalSoftwareRenderer", externalGESettingsListerner);
    	}

    	if (activateWhenAvailable) {
        	NativeUtils.init();
			activate();
		} else {
			deactivate();
		}
	}

	public static void exit() {
		if (externalGESettingsListerner != null) {
			Settings.getInstance().removeSettingsListener("ExternalGE");
			externalGESettingsListerner = null;
		}

		if (isActive()) {
			NativeUtils.exit();
			NativeCallbacks.exit();
			CoreThread.exit();
			setLogLevelThread.exit();
			if (enableAsyncRendering) {
				for (int i = 0; i < rendererThreads.length; i++) {
					rendererThreads[i].exit();
				}
			}
		}
	}

	public static boolean isActive() {
		return activateWhenAvailable && isAvailable();
	}

	public static boolean isAvailable() {
		return NativeUtils.isAvailable();
	}

	public static void startList(PspGeList list) {
		if (list == null) {
			return;
		}

		synchronized (drawListQueue) {
			if (currentList == null) {
				if (State.captureGeNextFrame) {
					State.captureGeNextFrame = false;
					CaptureManager.captureInProgress = true;
					NativeUtils.setDumpFrames(true);
					NativeUtils.setDumpTextures(true);
					logLevel = log.getLevel();
					log.setLevel(Level.TRACE);
				}
	
		        // Save the context at the beginning of the list processing to the given address (used by sceGu).
				if (list.hasSaveContextAddr()) {
		            saveContext(list.getSaveContextAddr());
				}
	
				list.status = sceGe_user.PSP_GE_LIST_DRAWING;
				NativeUtils.setLogLevel();
				NativeUtils.setCoreSadr(list.getStallAddr());
				NativeUtils.setCoreCtrlActive();
				synchronized (screenScaleLock) {
					// Update the screen scale only at the start of a new list
					NativeUtils.setScreenScale(getScreenScale());
				}
				currentList = list;
				CoreThread.getInstance().sync();
			} else {
				drawListQueue.add(list);
			}
		}
	}

	public static void startListHead(PspGeList list) {
		if (list == null) {
			return;
		}

		if (currentList == null) {
			startList(list);
		} else {
	        // The ConcurrentLinkedQueue type doesn't allow adding
	        // objects directly at the head of the queue.

	        // This function creates a new array using the given list as it's head
	        // and constructs a new ConcurrentLinkedQueue based on it.
	        // The actual drawListQueue is then replaced by this new one.
            int arraySize = drawListQueue.size();

            if (arraySize > 0) {
                PspGeList[] array = drawListQueue.toArray(new PspGeList[arraySize]);

                ConcurrentLinkedQueue<PspGeList> newQueue = new ConcurrentLinkedQueue<PspGeList>();
                PspGeList[] newArray = new PspGeList[arraySize + 1];

                newArray[0] = list;
                for (int i = 0; i < arraySize; i++) {
                    newArray[i + 1] = array[i];
                    newQueue.add(newArray[i]);
                }

                drawListQueue = newQueue;
            } else {    // If the queue is empty.
                drawListQueue.add(list);
            }
		}
	}

	public static void onStallAddrUpdated(PspGeList list) {
		if (isAvailable() && DurationStatistics.collectStatistics) {
			NativeUtils.stopEvent(NativeUtils.EVENT_GE_UPDATE_STALL_ADDR);
		}

		if (isActive()) {
			if (list == null) {
				return;
			}

			if (list == currentList) {
				NativeUtils.setCoreSadr(list.getStallAddr());
				CoreThread.getInstance().sync();
			}
		}
	}

	public static void onRestartList(PspGeList list) {
		if (isActive()) {
			if (list == null || list.isFinished()) {
				return;
			}

			synchronized (drawListQueue) {
				if (list == currentList) {
					list.status = sceGe_user.PSP_GE_LIST_DRAWING;
					NativeUtils.setCoreCtrlActive();
					CoreThread.getInstance().sync();
					list.sync();
				}
			}
		}
	}

	public static void finishList(PspGeList list) {
		Modules.sceGe_userModule.hleGeListSyncDone(list);

		synchronized (drawListQueue) {
			if (list == currentList) {
				if (CaptureManager.captureInProgress) {
					log.setLevel(logLevel);
					NativeUtils.setDumpFrames(false);
					NativeUtils.setDumpTextures(false);
					NativeUtils.setLogLevel();
					CaptureManager.captureInProgress = false;
					Emulator.PauseEmu();
				}

		        // Restore the context to the state at the beginning of the list processing (used by sceGu).
		        if (list.hasSaveContextAddr()) {
		            restoreContext(list.getSaveContextAddr());
		        }

		        currentList = null;
			} else {
				drawListQueue.remove(list);
			}
		}

		if (currentList == null) {
			startList(drawListQueue.poll());
		}
	}

	public static PspGeList getLastDrawList() {
        PspGeList lastList = null;

        synchronized (drawListQueue) {
	        for (PspGeList list : drawListQueue) {
	            if (list != null) {
	                lastList = list;
	            }
	        }

	        if (lastList == null) {
	            lastList = currentList;
	        }
        }

        return lastList;
	}

    public static PspGeList getFirstDrawList() {
        PspGeList firstList;

        synchronized (drawListQueue) {
            firstList = currentList;
            if (firstList == null) {
                firstList = drawListQueue.peek();
            }
        }

        return firstList;
    }

    public static PspGeList getCurrentList() {
		return currentList;
	}

	public static void onGeStartWaitList() {
		if (isAvailable() && DurationStatistics.collectStatistics) {
			NativeUtils.startEvent(NativeUtils.EVENT_GE_WAIT_FOR_LIST);
		}
	}

	public static void onGeStopWaitList() {
		if (isAvailable() && DurationStatistics.collectStatistics) {
			NativeUtils.stopEvent(NativeUtils.EVENT_GE_WAIT_FOR_LIST);
		}
	}

	public static void onDisplayStartWaitVblank() {
		if (isAvailable() && DurationStatistics.collectStatistics) {
			NativeUtils.startEvent(NativeUtils.EVENT_DISPLAY_WAIT_VBLANK);
		}
	}

	public static void onDisplayStopWaitVblank() {
		if (isAvailable() && DurationStatistics.collectStatistics) {
			NativeUtils.stopEvent(NativeUtils.EVENT_DISPLAY_WAIT_VBLANK);
		}
	}

	public static void onDisplayVblank() {
		if (isAvailable() && DurationStatistics.collectStatistics) {
			NativeUtils.notifyEvent(NativeUtils.EVENT_DISPLAY_VBLANK);
		}
	}

	public static void onGeStartList(PspGeList list) {
		if (isAvailable() && DurationStatistics.collectStatistics) {
			NativeUtils.notifyEvent(NativeUtils.EVENT_GE_START_LIST);
		}
	}

	public static void onGeFinishList(PspGeList list) {
		if (isAvailable() && DurationStatistics.collectStatistics) {
			NativeUtils.notifyEvent(NativeUtils.EVENT_GE_FINISH_LIST);
		}
	}

	public static void render() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("ExternalGE starting rendering"));
		}

		for (int i = 0; i < rendererThreads.length; i++) {
			rendererThreads[i].sync(rendererThreadsDone);
		}

		try {
			rendererThreadsDone.acquire(rendererThreads.length);
		} catch (InterruptedException e) {
			log.error("render", e);
		}

		NativeUtils.rendererTerminate();

		if (log.isDebugEnabled()) {
			log.debug(String.format("ExternalGE terminating rendering"));
		}
	}

	public static int saveContext(int addr) {
		if (NativeUtils.isCoreCtrlActive()) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Saving Core context to 0x%08X - Core busy", addr));
			}
			return -1;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Saving Core context to 0x%08X", addr));
		}

		NativeUtils.saveCoreContext(addr);

		return 0;
	}

	public static int restoreContext(int addr) {
		if (NativeUtils.isCoreCtrlActive()) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Restoring Core context from 0x%08X - Core busy", addr));
			}
			return SceKernelErrors.ERROR_BUSY;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Restoring Core context from 0x%08X", addr));
		}

		NativeUtils.restoreCoreContext(addr);

		return 0;
	}

	public static int getCmd(int cmd) {
		return NativeUtils.getCoreCmdArray(cmd);
	}

	public static float[] getMatrix(int mtxType) {
		// TODO Not implemented
		return null;
	}

	public static int getScreenScale() {
		return screenScale;
	}

	public static void setScreenScale(int screenScale) {
		log.info(String.format("Setting screen scale to factor %d", screenScale));
		ExternalGE.screenScale = screenScale;
	}

	public static ByteBuffer getScaledScreen(int address, int bufferWidth, int height, int pixelFormat) {
		synchronized (screenScaleLock) {
			return NativeUtils.getScaledScreen(address, bufferWidth, height, pixelFormat);
		}
	}

    public static void addVideoTexture(int startAddress, int endAddress) {
    	NativeUtils.addVideoTexture(startAddress, endAddress);
    }

    public static void onGeUserStop() {
		synchronized (drawListQueue) {
			drawListQueue.clear();
			if (currentList != null) {
				currentList.sync();
			}
			currentList = null;
			CoreThread.getInstance().sync();
		}
    }

    public static boolean hasDrawList(int listAddr) {
        boolean result = false;
        boolean waitAndRetry = false;

        synchronized (drawListQueue) {
            if (currentList != null && currentList.list_addr == listAddr) {
                result = true;
                // The current list has already reached the FINISH command,
                // but the list processing is not yet completed.
                // Wait a little for the list to complete.
                if (currentList.isFinished()) {
                    waitAndRetry = true;
                }
            } else {
                for (PspGeList list : drawListQueue) {
                    if (list != null && list.list_addr == listAddr) {
                        result = true;
                        break;
                    }
                }
            }
        }

        if (waitAndRetry) {
            // The current list is already finished but its processing is not yet
            // completed. Wait a little (100ms) and check again to avoid
            // the "can't enqueue duplicate list address" error.
            for (int i = 0; i < 100; i++) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("hasDrawList(0x%08X) waiting on finished list %s", listAddr, currentList));
                }
                Utilities.sleep(1, 0);
                synchronized (drawListQueue) {
                    if (currentList == null || currentList.list_addr != listAddr) {
                        result = false;
                        break;
                    }
                }
            }
        }

        return result;
    }
}
