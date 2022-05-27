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

/**
 * @author gid15
 */
public class BatteryUpdateThread extends Thread {
    private static BatteryUpdateThread instance = null;
    private long sleepMillis;

    public static void initialize() {
        if (instance == null) {
            long secondsForOnePercentDrain = Battery.getLifeTime() * 60 / 100;
            instance = new BatteryUpdateThread(secondsForOnePercentDrain * 1000);
            instance.setDaemon(true);
            instance.setName("Battery Drain");
            instance.start();
        }
    }

    public BatteryUpdateThread(long sleepMillis) {
        this.sleepMillis = sleepMillis;
    }

    @Override
    public void run() {
        if (OS.isWindows) updateWindows();
        else if (OS.isLinux) updateLinux();
        else if (OS.isMac) updateMac();
        else updateGeneric();
    }

    private void updateWindows() {
        while (true) {
            BatteryWindows.Kernel32.SYSTEM_POWER_STATUS status = BatteryWindows.status();

            // getLifeTime
            int batteryLifeTimeInSeconds = status.BatteryLifeTime;
            if (batteryLifeTimeInSeconds < 0) batteryLifeTimeInSeconds = 5 * 3600; // Unknown lifetime
            Battery.setLifeTime(batteryLifeTimeInSeconds / 60);

            // isPluggedIn
            Battery.setPluggedIn(status.ACLineStatus == 1);

            // isPResent
            Battery.setPresent((status.BatteryFlag & 128) != 0);

            // currentPowerPercent
            int percent = status.BatteryLifePercent;
            if (percent >= 0 && percent <= 100) {
                Battery.setCurrentPowerPercent(percent);
            } else {
                // Invalid value, not update it!
            }

            // isCharging
            Battery.setCharging((status.BatteryFlag & 8) != 0);

            sleepMillis(5 * 1000); // Wait five second between updates
        }
    }

    static private void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void updateLinux() {
        updateGeneric();
    }

    private void updateMac() {
        updateGeneric();
    }

    private void updateGeneric() {
        while (true) {
            sleepMillis(sleepMillis);

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

    static class BatteryWindows {
        static private Kernel32.SYSTEM_POWER_STATUS result = new Kernel32.SYSTEM_POWER_STATUS();

        static public Kernel32.SYSTEM_POWER_STATUS status() {
            Kernel32.INSTANCE.GetSystemPowerStatus(result);
            return result;
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

    // http://stackoverflow.com/questions/22128336/how-to-add-a-command-to-check-battery-level-in-linux-shell
    static class BatteryLinux {
        static private BatteryLinux INSTANCE = new BatteryLinux();
        static private String FOLDER = "/sys/class/power_supply/BAT0";

        static public BatteryLinux status() {
            return INSTANCE;
        }

        public static void refresh() {
        }
    }
}
