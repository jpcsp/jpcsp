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
 * Base abstract class for a settings listener with String value.
 * One of the "settingsValueChanged" method has to be overwritten by a concrete class.
 * 
 * @author gid15
 *
 */
public abstract class AbstractStringSettingsListener implements ISettingsListener {
	/* (non-Javadoc)
	 * @see jpcsp.settings.ISettingsListener#settingsValueChanged(java.lang.String, java.lang.String)
	 */
	@Override
	public void settingsValueChanged(String option, String value) {
		settingsValueChanged(value);
	}

	/**
	 * This method is called when the value of the registered settings option
	 * changes.
	 * This method is equivalent to
	 *     settingsValueChanged(String option, String value)
	 * but for simplicity, the option name is omitted.
	 * The option name is only relevant when the same settings listener is registered
	 * for multiple option name which is, for readability reasons, not recommended.
	 * 
	 * @param value     the new option value
	 */
	protected void settingsValueChanged(String value) {
	}
}
