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
package jpcsp.graphics;

import java.util.HashMap;

import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;

import org.apache.log4j.Logger;

/**
 * Profiler for the Graphics Engine
 *
 * @author gid15
 *
 */
public class GEProfiler {
    public static Logger log = Logger.getLogger("profiler");
    private static boolean profilerEnabled = true;
    private static ProfilerEnabledSettingsListerner profilerEnabledSettingsListerner;
    private static final Long zero = new Long(0);
    private static HashMap<Integer, Long> cmdCounts = new HashMap<Integer, Long>();
    private static long geListCount;

	private static class ProfilerEnabledSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setProfilerEnabled(value);
		}
	}

    public static void initialise() {
    	if (profilerEnabledSettingsListerner == null) {
    		profilerEnabledSettingsListerner = new ProfilerEnabledSettingsListerner();
    		Settings.getInstance().registerSettingsListener("Profiler", "emu.profiler", profilerEnabledSettingsListerner);
    	}

    	reset();
    }

	private static void setProfilerEnabled(boolean enabled) {
    	profilerEnabled = enabled;
    }

	public static boolean isProfilerEnabled() {
    	return profilerEnabled;
    }

    public static void reset() {
        if (!profilerEnabled) {
            return;
        }

        cmdCounts.clear();
        geListCount = 0;
    }

    public static void exit() {
        if (!profilerEnabled) {
            return;
        }

        log.info("------------------ GEProfiler ----------------------");
        log.info(String.format("GE list count: %d", geListCount));
        GeCommands geCommands = GeCommands.getInstance();
        for (Integer cmd : cmdCounts.keySet()) {
        	Long cmdCount = cmdCounts.get(cmd);
        	log.info(String.format("%s: called %d times, average %.1f per GE list", geCommands.getCommandString(cmd.intValue()), cmdCount.longValue(), cmdCount.longValue() / (double) geListCount));
        }
    }

    public static void startGeList() {
    	geListCount++;
    }

    public static void startGeCmd(int cmd) {
    	Long cmdCount = cmdCounts.get(cmd);
    	if (cmdCount == null) {
    		cmdCount = zero;
    	}

    	cmdCounts.put(cmd, cmdCount + 1);
    }
}
