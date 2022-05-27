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

public class GPS {
	// Simulate position of New-York City
	private static float positionLatitude = 40.713387f;
	private static float positionLongitude = -74.005516f;
	private static float positionAltitude = 39f;

	public static float getPositionLatitude() {
		return positionLatitude;
	}

	public static void setPositionLatitude(float positionLatitude) {
		GPS.positionLatitude = positionLatitude;
	}

	public static float getPositionLongitude() {
		return positionLongitude;
	}

	public static void setPositionLongitude(float positionLongitude) {
		GPS.positionLongitude = positionLongitude;
	}

	public static float getPositionAltitude() {
		return positionAltitude;
	}

	public static void setPositionAltitude(float positionAltitude) {
		GPS.positionAltitude = positionAltitude;
	}

	public static void initialize() {
		FakeGPSMove.initialize();
    }

	private static class FakeGPSMove extends Thread {
		private static FakeGPSMove instance;
		private long sleepMillis;
		private float latitudeDelta;
		private float longitudeDelta;
		private float altitudeDelta;

		private static void initialize() {
			if (instance == null) {
				// Fake a slight position move every 2 seconds
				instance = new FakeGPSMove(2000, 0.00001f, 0.00001f, 0f);
				instance.setDaemon(true);
				instance.setName("Fake GPS Move");
				instance.start();
			}
		}

		public FakeGPSMove(long sleepMillis, float latitudeDelta, float longitudeDelta, float altitudeDelta) {
			this.sleepMillis = sleepMillis;
			this.latitudeDelta = latitudeDelta;
			this.longitudeDelta = longitudeDelta;
			this.altitudeDelta = altitudeDelta;
		}

		@Override
		public void run() {
			while (true) {
				try {
					sleep(sleepMillis);
				} catch (InterruptedException e) {
					// Ignore exception
				}

				if (latitudeDelta != 0f) {
					GPS.setPositionLatitude(GPS.getPositionLatitude() + latitudeDelta);
				}
				if (longitudeDelta != 0f) {
					GPS.setPositionLongitude(GPS.getPositionLongitude() + longitudeDelta);
				}
				if (altitudeDelta != 0f) {
					GPS.setPositionAltitude(GPS.getPositionAltitude() + altitudeDelta);
				}
			}
		}
	}
}
