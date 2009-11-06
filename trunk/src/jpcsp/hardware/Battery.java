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

public class Battery {
    //battery life time in minutes
    private static int lifeTime = (5 * 60);		// 5 hours
    //some standard battery temperature 28 deg C
    private static int temperature = 28;
    //battery voltage 4,135 in slim
    private static int voltage = 4135;

    private static boolean pluggedIn = true;
    private static boolean present = true;
    private static int currentPowerPercent = 100;
    // led starts flashing at 12%
    private static final int lowPercent = 12;
	// PSP auto suspends at 4%
    private static final int forceSuspendPercent = 4;
	// battery capacity in mAh when it is full
    private static final int fullCapacity = 1800;
    private static boolean charging = false;

    public static void initialize() {
    	BatteryDrainThread.initialize();
    }

    public static int getLifeTime() {
		return lifeTime;
	}

	public static void setLifeTime(int lifeTime) {
		Battery.lifeTime = lifeTime;
	}

	public static int getTemperature() {
		return temperature;
	}

	public static void setTemperature(int temperature) {
		Battery.temperature = temperature;
	}

	public static int getVoltage() {
		return voltage;
	}

	public static void setVoltage(int voltage) {
		Battery.voltage = voltage;
	}

	public static boolean isPluggedIn() {
		return pluggedIn;
	}

	public static void setPluggedIn(boolean pluggedIn) {
		Battery.pluggedIn = pluggedIn;
	}

	public static boolean isPresent() {
		return present;
	}

	public static void setPresent(boolean present) {
		Battery.present = present;
	}

	public static int getCurrentPowerPercent() {
		return currentPowerPercent;
	}

	public static void setCurrentPowerPercent(int currentPowerPercent) {
		Battery.currentPowerPercent = currentPowerPercent;
	}

	public static boolean isCharging() {
		return charging;
	}

	public static void setCharging(boolean charging) {
		Battery.charging = charging;
	}

	public static int getLowPercent() {
		return lowPercent;
	}

	public static int getForceSuspendPercent() {
		return forceSuspendPercent;
	}

	public static int getFullCapacity() {
		return fullCapacity;
	}
}
