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

package jpcsp.HLE.modules;

import java.util.HashMap;

import jpcsp.settings.ISettingsListener;
import jpcsp.settings.Settings;

/**
 *
 * @author fiveofhearts
 */
abstract public class HLEModule {
    /** @return Example: StdioForUser */
	abstract public String getName();
	
	public HashMap<String, HLEModuleFunction> installedHLEModuleFunctions = new HashMap<String, HLEModuleFunction>();

	/**
	 * Returns an installed hle function by name.
	 * 
	 * @NOTE: If it is too slow to call on several places, it could be called once. Only when installed.
	 *        And stored in a local field to be used where required.
	 * 
	 * @param functionName
	 * @return
	 * @throws RuntimeException
	 */
	public HLEModuleFunction getHleFunctionByName(String functionName) throws RuntimeException {
		if (!installedHLEModuleFunctions.containsKey(functionName)) {
			for (HLEModuleFunction function : installedHLEModuleFunctions.values()) {
				System.err.println(function);
			}
			throw(new RuntimeException("Can't find hle function '" + functionName + "' on module '" + this.getName() + "'"));
		}
		
		return installedHLEModuleFunctions.get(functionName);
	}

	public void start() {
	}

	public void stop() {
		// Remove all the settings listener defined for this module
		Settings.getInstance().removeSettingsListener(getName());
	}

	protected void setSettingsListener(String option, ISettingsListener settingsListener) {
		Settings.getInstance().registerSettingsListener(getName(), option, settingsListener);
	}

	static protected String getCallingFunctionName(int index) {
    	StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    	return stack[index + 1].getMethodName();
    }
}
