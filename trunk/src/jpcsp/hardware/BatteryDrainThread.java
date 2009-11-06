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

/**
 * @author gid15
 *
 */
public class BatteryDrainThread extends Thread {
	private static BatteryDrainThread instance = null;
	private long sleepMillis;

	public static void initialize() {
		if (instance == null) {
			long secondsForOnePercentDrain = Battery.getLifeTime() * 60 / 100;
			instance = new BatteryDrainThread(secondsForOnePercentDrain * 1000);
			instance.setDaemon(true);
			instance.setName("Battery Drain");
			instance.start();
		}
	}

	public BatteryDrainThread(long sleepMillis) {
		this.sleepMillis = sleepMillis;
	}

	@Override
	public void run() {
		while (true) {
			try {
				sleep(sleepMillis);
			} catch (InterruptedException e) {
				// Ignore exception
			}

			int powerPercent = Battery.getCurrentPowerPercent();

			// Increase/decrease power by 1%
			if (Battery.isCharging()) {
				if (powerPercent < 100) {
					powerPercent++;
				}
			} else {
				if (powerPercent > 0) {
					powerPercent--;
				}
			}

			Battery.setCurrentPowerPercent(powerPercent);
		}
	}
}
