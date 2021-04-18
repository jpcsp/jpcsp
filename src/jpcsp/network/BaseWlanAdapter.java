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
package jpcsp.network;

import static jpcsp.network.BaseNetworkAdapter.settingsEnableChat;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.settings.Settings;

/**
 * @author gid15
 *
 */
public abstract class BaseWlanAdapter implements IWlanAdapter {
	public static Logger log = Logger.getLogger("wlan");

	@Override
	public void stop() throws IOException {
	}

	public boolean hasChatEnabled() {
		return Settings.getInstance().readBool(settingsEnableChat);
	}
}
