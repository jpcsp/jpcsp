/*

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
package jpcsp.graphics.RE.software;

import static jpcsp.util.Utilities.sleep;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 */
public class RendererExecutor {
	private static final int numberThreads = 1;
	private static RendererExecutor instance;
	private final LinkedBlockingQueue<IRenderer> renderersQueue = new LinkedBlockingQueue<IRenderer>();
	private volatile boolean ended;
	private volatile int numberThreadsRendering;
	private final Object numberThreadsRenderingLock = new Object();

	public static RendererExecutor getInstance() {
		if (instance == null) {
			instance = new RendererExecutor();
		}

		return instance;
	}

	private RendererExecutor() {
		for (int i = 0; i < numberThreads; i++) {
			Thread thread = new ThreadRenderer();
			thread.setName(String.format("Thread SoftwareRenderer #%d", i + 1));
			thread.setDaemon(true);
			thread.start();
		}
	}

	public static void exit() {
		if (instance != null) {
			instance.ended = true;
		}
	}

	public void render(IRenderer renderer) {
		if (numberThreads > 0 && !VideoEngine.log.isTraceEnabled()) {
			// Queue for rendering in a ThreadRenderer thread
			renderer = renderer.duplicate();
			renderersQueue.add(renderer);
		} else {
			// Threads are disabled or capture is active, render immediately
			try {
				renderer.render();
			} catch (Exception e) {
				VideoEngine.log.error("Error while rendering", e);
			}
		}
	}

	public void waitForRenderingCompletion() {
		if (numberThreads > 0) {
			while (!renderersQueue.isEmpty() || numberThreadsRendering > 0) {
				sleep(1, 0);
			}
		}
	}

	private class ThreadRenderer extends Thread {
		@Override
		public void run() {
			while (!ended) {
				IRenderer renderer = null;
				try {
					renderer = renderersQueue.poll(100, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					// Ignore Exception
				}

				if (renderer != null) {
					synchronized (numberThreadsRenderingLock) {
						numberThreadsRendering++;
					}

					try {
						renderer.render();
					} catch (Exception e) {
						VideoEngine.log.error("Error while rendering", e);
					}

					synchronized (numberThreadsRenderingLock) {
						numberThreadsRendering--;
					}
				}
			}
		}
	}
}
