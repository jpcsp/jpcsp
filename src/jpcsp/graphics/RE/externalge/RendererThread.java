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

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class RendererThread extends Thread {
	private static final Logger log = ExternalGE.log;
	private int lineMask;
	private Semaphore sync;
	private volatile boolean exit;
	private Semaphore response;

	public RendererThread(int lineMask) {
		this.lineMask = lineMask;
		sync = new Semaphore(0);
	}

	@Override
	public void run() {
		while (!exit) {
			if (waitForSync(100)) {
				if (lineMask != 0) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Starting async rendering lineMask=0x%08X", lineMask));
					}
					NativeUtils.rendererRender(lineMask);
				}

				if (response != null) {
					// Be careful to clear the response before releasing it!
					Semaphore responseToBeReleased = response;
					response = null;
					responseToBeReleased.release();
				}
			}
		}
	}

	public void exit() {
		exit = true;
		sync(null);
	}

	public void sync(Semaphore response) {
		this.response = response;
		if (sync != null) {
			sync.release();
		}
	}

	private boolean waitForSync(int millis) {
		while (true) {
	    	try {
    			if (sync.tryAcquire(millis, TimeUnit.MILLISECONDS)) {
    				break;
    			}
				return false;
			} catch (InterruptedException e) {
				// Ignore exception and retry again
			}
		}

		return true;
	}
}
