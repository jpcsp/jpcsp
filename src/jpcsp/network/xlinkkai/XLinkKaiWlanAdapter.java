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
package jpcsp.network.xlinkkai;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.BaseWlanAdapter;
import jpcsp.network.protocols.EtherFrame;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;

public class XLinkKaiWlanAdapter extends BaseWlanAdapter {
	public static Logger log = Logger.getLogger("XlinkKai");
	private static boolean enabled = false;

	private static class EnabledSettingsListener extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setEnabled(value);
		}
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean enabled) {
		XLinkKaiWlanAdapter.enabled = enabled;
		if (enabled) {
			log.info("Enabling XLink Kai network");
		}
	}

	public static void init() {
		Settings.getInstance().registerSettingsListener("XLinkKai", "emu.enableXLinkKai", new EnabledSettingsListener());
	}

	public static void exit() {
		Settings.getInstance().removeSettingsListener("XLinkKai");

		if (!isEnabled()) {
			return;
		}
	}

	@Override
	public void start() throws IOException {
		log.error("Unimplemented");
	}

	@Override
	public void stop() throws IOException {
		log.error("Unimplemented");
	}

	@Override
	public void sendWlanPacket(byte[] buffer, int offset, int length) {
		log.error("Unimplemented");
	}

	@Override
	public void sendAccessPointPacket(byte[] buffer, int offset, int length, EtherFrame etherFrame) {
		log.error("Unimplemented");
	}

	@Override
	public void sendGameModePacket(pspNetMacAddress macAddress, byte[] buffer, int offset, int length) {
		log.error("Unimplemented");
	}

	@Override
	public int receiveWlanPacket(byte[] buffer, int offset, int length) throws IOException {
		log.error("Unimplemented");
		return -1;
	}

	@Override
	public int receiveGameModePacket(pspNetMacAddress macAddress, byte[] buffer, int offset, int length) throws IOException {
		log.error("Unimplemented");
		return -1;
	}

	@Override
	public void wlanScan() {
		log.error("Unimplemented");
	}
}
