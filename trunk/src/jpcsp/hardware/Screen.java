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
package jpcsp.hardware;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Robot;

import jpcsp.Emulator;

public class Screen {
    private static DisableScreenSaverThread disableScreenSaverThread;
    public static final int width = 480;
    public static final int height = 272;
    private static long lastPowerTick;
    private static boolean hasScreen = true;

    public static void hleKernelPowerTick() {
    	lastPowerTick = getClockMillis();
    }

    public static void start() {
    	lastPowerTick = getClockMillis();

    	if (disableScreenSaverThread == null) {
            disableScreenSaverThread = new DisableScreenSaverThread();
            disableScreenSaverThread.setName("Disable Screen Saver");
            disableScreenSaverThread.setDaemon(true);
            disableScreenSaverThread.start();
		}
	}

    private static long getClockMillis() {
    	return Emulator.getClock().milliTime();
    }

    public static void exit() {
    	if (disableScreenSaverThread != null) {
    		disableScreenSaverThread.exit();
    	}
    }

    private static class DisableScreenSaverThread extends Thread {
    	private volatile boolean exit;
    	private static final int tickMillis = 60 * 1000; // One minute

    	public void exit() {
    		exit = true;
    	}

    	@Override
		public void run() {
			try {
				Robot robot = new Robot();
				while (!exit) {
					try {
						Thread.sleep(tickMillis);
					} catch (InterruptedException e) {
						// Ignore exception
					}

					long now = getClockMillis();
					if (now - lastPowerTick < tickMillis) {
						if (Emulator.log.isTraceEnabled()) {
							Emulator.log.trace(String.format("Moving the mouse to disable the screen saver (PowerTick since %d ms)", now - lastPowerTick));
						}
						robot.waitForIdle();

						// Move the mouse to its current location to disable the screensaver
						PointerInfo mouseInfo = MouseInfo.getPointerInfo();
						robot.mouseMove(mouseInfo.getLocation().x, mouseInfo.getLocation().y);
					} else {
						if (Emulator.log.isTraceEnabled()) {
							Emulator.log.trace(String.format("PowerTick not called since %d ms", now - lastPowerTick));
						}
					}
				}
			} catch (AWTException e) {
				Emulator.log.error(e);
			}
		}
    }

	public static boolean hasScreen() {
		return hasScreen;
	}

	public static void setHasScreen(boolean hasScreen) {
		Screen.hasScreen = hasScreen;
	}
}
