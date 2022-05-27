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
package jpcsp.mediaengine;

import static jpcsp.Allegrex.compiler.RuntimeContext.setLog4jMDC;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Thread running the Media Engine processor.
 *
 * @author gid15
 *
 */
public class METhread extends Thread {
	private static METhread instance;
	private volatile boolean exit;
	private MEProcessor processor;
	private final Semaphore sync;

	public static METhread getInstance() {
		if (instance == null) {
			instance = new METhread();
		}
		return instance;
	}

	public static boolean isMediaEngine(Thread thread) {
		return thread == instance;
	}

	public static void exit() {
		if (instance != null) {
			instance.exit = true;
			instance.sync();
		}
	}

	private METhread() {
		sync = new Semaphore(0);

		setName("Media Engine Thread");
		setDaemon(true);
		start();
	}

	public void setProcessor(MEProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void run() {
		setLog4jMDC();
		while (!exit) {
			if (waitForSync(100)) {
				processor.run();
			}
		}
	}

	private boolean waitForSync(int millis) {
    	while (true) {
	    	try {
	    		int availablePermits = sync.drainPermits();
	    		if (availablePermits > 0) {
	    			break;
	    		}

    			if (sync.tryAcquire(millis, TimeUnit.MILLISECONDS)) {
    				break;
    			}
				return false;
			} catch (InterruptedException e) {
				// Ignore exception and retry again
				processor.getLogger().debug(String.format("METhread.waitForSync %s", e));
			}
    	}

    	return true;
    }

	public void sync() {
		if (sync != null) {
			sync.release();
		}
	}
}
