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
package jpcsp.HLE.kernel.types;

public class SceNetWlanScanInfo extends pspAbstractMemoryMappedStructure {
	public String bssid;
	public int channel;
	public String ssid;
	public int mode;
	public int beaconInterval;

	@Override
	protected void read() {
		bssid = readStringNZ(6); // Offset 0
		channel = read8(); // Offset 6
		int ssidLength = read8(); // Offset 7
		ssid = readStringNZ(ssidLength); // Offset 8
		readUnknown(32 - ssidLength);
		mode = read32(); // Offset 40
		beaconInterval = read32(); // Offset 44
	}

	@Override
	protected void write() {
		writeStringN(6, bssid); // Offset 0
		write8((byte) channel); // Offset 6
		if (ssid == null) {
			write8((byte) 0); // Offset 7
		} else {
			write8((byte) ssid.length()); // Offset 7
		}
		writeStringN(32, ssid); // Offset 8
		write32(mode); // Offset 40
		write32(beaconInterval); // Offset 44
		writeUnknown(44); // Offset 48
	}

	@Override
	public int sizeof() {
		return 92;
	}

	@Override
	public String toString() {
		return String.format("bssid='%s', channel=%d, ssid='%s', mode=0x%X, beaconInterval=0x%X", bssid, channel, ssid, mode, beaconInterval);
	}
}
