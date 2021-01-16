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

import java.io.IOException;

import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.protocols.EtherFrame;

/**
 * @author gid15
 *
 */
public interface IWlanAdapter {
	public void start() throws IOException;
	public void stop() throws IOException;
	public void sendWlanPacket(byte[] buffer, int offset, int length) throws IOException;
	public void sendAccessPointPacket(byte[] buffer, int offset, int length, EtherFrame etherFrame) throws IOException;
	public void sendGameModePacket(pspNetMacAddress macAddress, byte[] buffer, int offset, int length) throws IOException;
	public int receiveWlanPacket(byte[] buffer, int offset, int length) throws IOException;
	public int receiveGameModePacket(pspNetMacAddress macAddress, byte[] buffer, int offset, int length) throws IOException;
	public void wlanScan(String ssid, int[] channels) throws IOException;
	public void sendChatMessage(String message);
}
