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

import static jpcsp.HLE.modules.sceRtc.hleGetCurrentMicros;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
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
		@Override
		public void run() {
			RuntimeContext.setLog4jMDC();

			while (true) {
				checkDeltaSynchronize(deltaSyncDelayMillis);;
				Utilities.sleep(deltaSyncIntervalMillis, 0);
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

	protected ScePspDateTime now() {
		return ScePspDateTime.fromMicros(hleGetCurrentMicros());
	}

	private int checkDeltaSynchronize(int syncDelayMillis) {
		int result = 0;

		synchronized (lock) {
	    	long now = Emulator.getClock().currentTimeMillis();
	    	long millisSinceLastWrite = now - lastWrite;
	    	if (lastSync < lastWrite && millisSinceLastWrite >= syncDelayMillis) {
	    		result = deltaSynchronize();
	    		lastSync = now;
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

    @Override
	public void notifyWrite() {
    	long now = Emulator.getClock().currentTimeMillis();
		synchronized (lock) {
			lastWrite = now;
		}
	}
}
