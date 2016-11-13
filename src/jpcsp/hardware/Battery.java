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

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;
import jpcsp.util.OS;

import java.util.Arrays;
import java.util.List;

public class Battery {
    //battery life time in minutes
    private static int lifeTime = (5 * 60);        // 5 hours
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
        if (OS.isWindows) {
            int batteryLifeTimeInSeconds = BatteryWindows.status().BatteryLifeTime;
            if (batteryLifeTimeInSeconds < 0) batteryLifeTimeInSeconds = 5 * 3600; // Unknown lifetime
            return batteryLifeTimeInSeconds / 60;
        }
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
        if (OS.isWindows) {
            return BatteryWindows.status().ACLineStatus == 1;
        }
        return pluggedIn;
    }

    public static void setPluggedIn(boolean pluggedIn) {
        Battery.pluggedIn = pluggedIn;
    }

    public static boolean isPresent() {
        if (OS.isWindows) {
            return (BatteryWindows.status().BatteryFlag & 128) != 0;
        }
        return present;
    }

    public static void setPresent(boolean present) {
        Battery.present = present;
    }

    public static int getCurrentPowerPercent() {
        if (OS.isWindows) {
            int percent = BatteryWindows.status().BatteryLifePercent;
            if (percent >= 0 && percent <= 100) {
                return percent;
            } else {
                return currentPowerPercent; // Invalid value, provide fallback!
            }
        }
        return currentPowerPercent;
    }

    public static void setCurrentPowerPercent(int currentPowerPercent) {
        Battery.currentPowerPercent = currentPowerPercent;
    }

    public static boolean isCharging() {
        if (OS.isWindows) {
            return (BatteryWindows.status().BatteryFlag & 8) != 0;
        }
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

    static class BatteryWindows {
        static public Kernel32.SYSTEM_POWER_STATUS status() {
            Kernel32.SYSTEM_POWER_STATUS out = new Kernel32.SYSTEM_POWER_STATUS();
            Kernel32.INSTANCE.GetSystemPowerStatus(out);
            return out;
        }

        // http://stackoverflow.com/questions/3434719/how-to-get-the-remaining-battery-life-in-a-windows-system
        // http://msdn2.microsoft.com/en-us/library/aa373232.aspx
        public interface Kernel32 extends StdCallLibrary {
            Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("Kernel32", Kernel32.class);

            class SYSTEM_POWER_STATUS extends Structure {
                public byte ACLineStatus; // 0 = Offline, 1 = Online, other = Unknown
                public byte BatteryFlag; // 1 = High (more than 66%), 2 = Low (less than 33%), 4 = Critical (less than 5%), 8 = Charging, 128 = No system battery
                public byte BatteryLifePercent; // 0-100 (-1 on desktop)
                public byte Reserved1;
                public int BatteryLifeTime; // Estimated Lifetime in seconds
                public int BatteryFullLifeTime; // Estimated Lifetime in seconds on full charge

                @Override
                protected List<String> getFieldOrder() {
                    return Arrays.asList("ACLineStatus", "BatteryFlag", "BatteryLifePercent", "Reserved1", "BatteryLifeTime", "BatteryFullLifeTime");
                }
            }

            int GetSystemPowerStatus(SYSTEM_POWER_STATUS result);
        }
    }
}
