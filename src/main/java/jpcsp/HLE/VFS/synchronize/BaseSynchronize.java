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
package jpcsp.HLE.VFS.synchronize;

import static jpcsp.HLE.VFS.AbstractVirtualFileSystem.IO_ERROR;
import static jpcsp.HLE.modules.sceRtc.hleGetCurrentMicros;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

public abstract class BaseSynchronize implements ISynchronize {
	public static Logger log = Logger.getLogger("synchronize");
	private static final int STATE_VERSION = 0;
    protected static final int deltaSyncDelayMillis = 1000;
    protected static final int deltaSyncIntervalMillis = 100;
    protected String name;
    protected final Object lock;
	private long lastWrite;
	private long lastSync;
    private SynchronizeThread synchronizeThread;

    private class SynchronizeThread extends Thread {
    	private volatile boolean exit;
    	private volatile boolean done;

    	@Override
		public void run() {
    		done = false;

    		RuntimeContext.setLog4jMDC();

			while (!exit) {
				Utilities.sleep(deltaSyncIntervalMillis, 0);
				checkDeltaSynchronize(deltaSyncDelayMillis);
			}

			// Perform an immediate check before exiting
			checkDeltaSynchronize(0);

			done = true;
		}

    	public void exit() {
    		exit = true;

    		// Wait for completion
    		while (!done) {
    			Utilities.sleep(100);
    		}
    	}
    }

	protected BaseSynchronize(String name, Object lock) {
		this.name = name;
		this.lock = lock;

		synchronizeThread = new SynchronizeThread();
		synchronizeThread.setName(String.format("Synchronize Thread - %s", name));
		synchronizeThread.setDaemon(true);
		synchronizeThread.start();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	lastWrite = stream.readLong();
    	lastSync = stream.readLong();
		invalidateCachedData();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	stream.writeLong(lastWrite);
    	stream.writeLong(lastSync);
	}

	protected ScePspDateTime nowDate() {
		// Time is stored as local time in FAT directory entries
		return ScePspDateTime.fromMicrosLocal(hleGetCurrentMicros());
	}

	protected long now() {
		// Use the real time, not the PSP clock so that the
		// synch can be done while the PSP is paused.
		return System.currentTimeMillis();
	}

	private int checkDeltaSynchronize(int syncDelayMillis) {
		int result = 0;

		synchronized (lock) {
	    	long now = now();
	    	long millisSinceLastWrite = now - lastWrite;

//	    	if (log.isTraceEnabled()) {
//	    		log.trace(String.format("checkDeltaSynchronize syncDelayMillis=0x%X, millisSinceLastWrite=0x%X, lastSync=0x%X, lastWrite=0x%X, now=0x%X", syncDelayMillis, millisSinceLastWrite, lastSync, lastWrite, now));
//	    	}

	    	if (lastSync < lastWrite && millisSinceLastWrite >= syncDelayMillis) {
		    	if (log.isTraceEnabled()) {
		    		log.trace(String.format("checkDeltaSynchronize deltaSynchronize() now"));
		    	}

		    	try {
		    		result = deltaSynchronize();
		    	} catch (Exception e) {
		    		log.error("checkDeltaSynchronize", e);
		    		result = IO_ERROR;
		    	}

		    	if (result >= 0) {
	    			lastSync = now;
	    		}
	    	}
		}

		return result;
	}

	@Override
	public int synchronize() {
		return checkDeltaSynchronize(0);
	}

	protected abstract int deltaSynchronize();

    protected abstract void invalidateCachedData();
    protected abstract void flushCachedData();

    @Override
	public void notifyWrite() {
    	long now = now();
		synchronized (lock) {
			lastWrite = now;
		}
	}

    public void exit() {
    	synchronizeThread.exit();
    }
}
