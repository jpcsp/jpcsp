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
package jpcsp.settings;

/**
 * Simple container for a registered settings listener.
 *
 * See
 *     Settings.registerSettingsListener()
 * for the the registration of settings listeners.
 * 
 * @author gid15
 *
 */
public class SettingsListenerInfo {
	private String name;
	private String key;
	private ISettingsListener listener;

	public SettingsListenerInfo(String name, String key, ISettingsListener listener) {
		this.name = name;
		this.key = key;
		this.listener = listener;
	}

	public String getName() {
		return name;
	}

	public String getKey() {
		return key;
	}

	public ISettingsListener getListener() {
		return listener;
	}

	/**
	 * Test if the current object is matching the given name and key values.
	 * A null value matches any value.
	 *
	 * @param name     name, or null to match any name.
	 * @param key      key, or null to match any key.
	 * @return
	 */
	public boolean equals(String name, String key) {
		if (name != null) {
			if (!this.name.equals(name)) {
				return false;
			}
		}

		if (key != null) {
			if (!this.key.equals(key)) {
				return false;
			}
		}

		return true;
	}
}
