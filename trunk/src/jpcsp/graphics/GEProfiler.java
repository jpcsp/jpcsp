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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

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
    private static HashMap<Integer, Long> primVtypeCounts = new HashMap<Integer, Long>();
    private static HashMap<Integer, String> vtypeNames = new HashMap<Integer, String>();
    private static long geListCount;
    private static long textureLoadCount;
    private static long copyGeToMemoryCount;
    private static long copyStencilToMemoryCount;

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
        primVtypeCounts.clear();
        vtypeNames.clear();
        geListCount = 0;
        textureLoadCount = 0;
        copyGeToMemoryCount = 0;
    }

    public static void exit() {
        if (!profilerEnabled) {
            return;
        }

        log.info("------------------ GEProfiler ----------------------");
        log.info(String.format("GE list count: %d", geListCount));
        log.info(String.format("Texture load count: %d, average %.1f per GE list", textureLoadCount, textureLoadCount / (double) geListCount));
        log.info(String.format("Copy GE to memory: %d, average %.1f per GE list", copyGeToMemoryCount, copyGeToMemoryCount / (double) geListCount));
        log.info(String.format("Copy Stencil to memory: %d, average %.1f per GE list", copyStencilToMemoryCount, copyStencilToMemoryCount / (double) geListCount));
        GeCommands geCommands = GeCommands.getInstance();
        for (Integer cmd : cmdCounts.keySet()) {
        	Long cmdCount = cmdCounts.get(cmd);
        	log.info(String.format("%s: called %d times, average %.1f per GE list", geCommands.getCommandString(cmd.intValue()), cmdCount.longValue(), cmdCount.longValue() / (double) geListCount));
        }

        // Sort the primVtypeCounts based on their counts (highest count first).
        List<Integer> primVtypeSorted = new ArrayList<Integer>(primVtypeCounts.keySet());
        Collections.sort(primVtypeSorted, new Comparator<Integer>() {
			@Override
			public int compare(Integer vtype1, Integer vtype2) {
				return -primVtypeCounts.get(vtype1).compareTo(primVtypeCounts.get(vtype2));
			}
		});

        for (Integer vtype : primVtypeSorted) {
        	Long vtypeCount = primVtypeCounts.get(vtype);
        	log.info(String.format("%s: used %d times in PRIM, average %.1f per GE list", vtypeNames.get(vtype), vtypeCount.longValue(), vtypeCount.longValue() / (double) geListCount));
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

    	if (cmd == GeCommands.PRIM) {
    		VertexInfo vinfo = VideoEngine.getInstance().getContext().vinfo;
    		int vtype = vinfo.vtype;
    		Long vtypeCount = primVtypeCounts.get(vtype);
    		if (vtypeCount == null) {
    			vtypeCount = zero;
    			vtypeNames.put(vtype, vinfo.toString());
    		}
    		primVtypeCounts.put(vtype, vtypeCount + 1);
    	}
    }

    public static void loadTexture() {
    	textureLoadCount++;
    }

    public static void copyGeToMemory() {
    	copyGeToMemoryCount++;
    }

    public static void copyStencilToMemory() {
    	copyStencilToMemoryCount++;
    }
}
