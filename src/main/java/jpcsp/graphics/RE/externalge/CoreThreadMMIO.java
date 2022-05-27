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

import static jpcsp.Allegrex.compiler.RuntimeContext.setLog4jMDC;
import static jpcsp.graphics.RE.externalge.ExternalGE.numberRendererThread;
import static jpcsp.graphics.RE.externalge.NativeUtils.INTR_STAT_END;
import static jpcsp.graphics.RE.externalge.NativeUtils.coreInterpret;
import static jpcsp.graphics.RE.externalge.NativeUtils.getCoreInterrupt;
import static jpcsp.graphics.RE.externalge.NativeUtils.getCoreMadr;
import static jpcsp.graphics.RE.externalge.NativeUtils.getCoreSadr;
import static jpcsp.graphics.RE.externalge.NativeUtils.getRendererIndexCount;
import static jpcsp.graphics.RE.externalge.NativeUtils.isCoreCtrlActive;
import static jpcsp.graphics.RE.externalge.NativeUtils.updateMemoryUnsafeAddr;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.memory.mmio.MMIOHandlerGe;

/**
 * @author gid15
 *
 */
public class CoreThreadMMIO extends Thread {
	protected static Logger log = ExternalGE.log;
	private static boolean enabled = true;
	private static CoreThreadMMIO instance;
	private volatile boolean exit;
	private Semaphore sync;

	public static CoreThreadMMIO getInstance() {
		if (instance == null) {
			instance = new CoreThreadMMIO();
			instance.setDaemon(true);
			instance.setName("ExternalGE - Core Thread for MMIO");
			if (isEnabled()) {
				instance.start();
			}
		}

		return instance;
	}

	public static void exit() {
		if (instance != null) {
			instance.exit = true;
			instance = null;
		}
	}

	public static void setEnabled() {
		boolean wasEnabled = enabled;
		enabled = true;

		if (!wasEnabled && instance != null) {
			instance.start();
		}
	}

	public static void setDisabled() {
		enabled = false;
	}

	public static boolean isEnabled() {
		return enabled;
	}

	private CoreThreadMMIO() {
		sync = new Semaphore(0);
	}

	@Override
	public void run() {
		setLog4jMDC();
		while (!exit) {
			if (!isCoreCtrlActive() || getCoreMadr() == getCoreSadr()) {
				if (!Emulator.pause && log.isTraceEnabled()) {
					log.trace(String.format("CoreThreadMMIO not active... waiting"));
				}

				waitForSync(1000);
			} else {
				updateMemoryUnsafeAddr();

				if (log.isDebugEnabled()) {
					log.debug(String.format("CoreThreadMMIO processing 0x%08X", NativeUtils.getCoreMadr()));
				}

				while (coreInterpret()) {
					updateMemoryUnsafeAddr();

					if (log.isDebugEnabled()) {
						log.debug(String.format("CoreThreadMMIO looping at 0x%08X", NativeUtils.getCoreMadr()));
					}

					if (numberRendererThread > 0 && getRendererIndexCount() > 0) {
						break;
					}
				}

				int interrupt = getCoreInterrupt();
				if ((interrupt & INTR_STAT_END) != 0) {
					MMIOHandlerGe.getInstance().onGeInterrupt();
				}

				if (numberRendererThread > 0 && getRendererIndexCount() > 0) {
					ExternalGE.render();
				}
			}
		}

		log.info(String.format("CoreThreadMMIO exited"));
	}

	public void sync() {
		if (sync != null) {
			sync.release();
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
				log.debug(String.format("CoreThreadMMIO waitForSync %s", e));
			}
		}

		return true;
	}
}
