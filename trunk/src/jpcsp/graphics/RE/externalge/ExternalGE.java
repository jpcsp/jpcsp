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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.HLE.modules.sceGe_user;
import jpcsp.util.DurationStatistics;

/**
 * @author gid15
 *
 */
public class ExternalGE {
	public static final boolean enableAsyncRendering = false;
	public static final boolean useUnsafe = false;
	public static Logger log = Logger.getLogger("externalge");
	private static ConcurrentLinkedQueue<PspGeList> drawListQueue;
	private static PspGeList currentList;
	private static RendererThread[] rendererThreads;
	private static Semaphore rendererThreadsDone;

	public static void init() {
		NativeUtils.init();
		if (isActive()) {
			drawListQueue = new ConcurrentLinkedQueue<PspGeList>();
		}

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
	}

	public static void exit() {
		if (isActive()) {
			NativeUtils.exit();
			NativeCallbacks.exit();
			CoreThread.exit();
			if (enableAsyncRendering) {
				for (int i = 0; i < rendererThreads.length; i++) {
					rendererThreads[i].exit();
				}
			}
		}
	}

	public static boolean isActive() {
		return NativeUtils.isActive();
	}

	public static void startList(PspGeList list) {
		if (list == null) {
			return;
		}

		if (currentList == null) {
			if (DurationStatistics.collectStatistics) {
				NativeUtils.notifyEvent(NativeUtils.EVENT_GE_START_LIST);
			}
			list.status = sceGe_user.PSP_GE_LIST_DRAWING;
			NativeUtils.setLogLevel();
			NativeUtils.setCoreSadr(list.getStallAddr());
			NativeUtils.setCoreCtrlActive();
			currentList = list;
			CoreThread.getInstance().sync();
		} else {
			drawListQueue.add(list);
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
		if (list == null) {
			return;
		}

		if (list == currentList) {
			if (DurationStatistics.collectStatistics) {
				NativeUtils.notifyEvent(NativeUtils.EVENT_GE_UPDATE_STALL_ADDR);
			}
			NativeUtils.setCoreSadr(list.getStallAddr());
			NativeUtils.setCoreCtrlActive();
			CoreThread.getInstance().sync();
		}
	}

	public static void onRestartList(PspGeList list) {
		if (list == null) {
			return;
		}

		if (list == currentList) {
			list.status = sceGe_user.PSP_GE_LIST_DRAWING;
			NativeUtils.setCoreCtrlActive();
			CoreThread.getInstance().sync();
			list.sync();
		}
	}

	public static void finishList(PspGeList list) {
		Modules.sceGe_userModule.hleGeListSyncDone(list);

		if (list == currentList) {
			if (DurationStatistics.collectStatistics) {
				NativeUtils.notifyEvent(NativeUtils.EVENT_GE_FINISH_LIST);
			}
			currentList = null;
		} else {
			drawListQueue.remove(list);
		}

		if (currentList == null) {
			startList(drawListQueue.poll());
		}
	}

	public static PspGeList getLastDrawList() {
        PspGeList lastList = null;

        for (PspGeList list : drawListQueue) {
            if (list != null) {
                lastList = list;
            }
        }

        if (lastList == null) {
            lastList = currentList;
        }

        return lastList;
	}

	public static PspGeList getCurrentList() {
		return currentList;
	}

	public static void onGeStartWaitList() {
		if (DurationStatistics.collectStatistics) {
			NativeUtils.startEvent(NativeUtils.EVENT_GE_WAIT_FOR_LIST);
		}
	}

	public static void onGeStopWaitList() {
		if (DurationStatistics.collectStatistics) {
			NativeUtils.stopEvent(NativeUtils.EVENT_GE_WAIT_FOR_LIST);
		}
	}

	public static void onDisplayStartWaitVblank() {
		if (DurationStatistics.collectStatistics) {
			NativeUtils.startEvent(NativeUtils.EVENT_DISPLAY_WAIT_VBLANK);
		}
	}

	public static void onDisplayStopWaitVblank() {
		if (DurationStatistics.collectStatistics) {
			NativeUtils.stopEvent(NativeUtils.EVENT_DISPLAY_WAIT_VBLANK);
		}
	}

	public static void onDisplayVblank() {
		if (DurationStatistics.collectStatistics) {
			NativeUtils.notifyEvent(NativeUtils.EVENT_DISPLAY_VBLANK);
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
}
