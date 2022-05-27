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
 * Interface for a settings listener.
 * See Settings.registerSettingsListener for the registration of settings listeners.
 * 
 * @author gid15
 */
public interface ISettingsListener {
	/**
	 * This method is called when the value of the registered settings option
	 * changes.
	 * 
	 * @param option    the option name
	 * @param value     the new option value
	 */
	public void settingsValueChanged(String option, String value);
}
