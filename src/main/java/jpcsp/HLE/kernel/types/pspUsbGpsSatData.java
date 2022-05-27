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

/*
 * GPS Satellite Data Structure for sceUsbGpsGetData().
 * Based on MapThis! homebrew v5.2
 */
public class pspUsbGpsSatData extends pspAbstractMemoryMappedStructure {
	public static final int MAX_SATELLITES = 24;
	public short satellitesInView;
	public short garbage;
	public pspUsbGpsSatInfo[] satInfo;

	@Override
	protected void read() {
		satellitesInView = (short) read16();
		garbage = (short) read16();
		satInfo = new pspUsbGpsSatInfo[Math.min(satellitesInView, MAX_SATELLITES)];
		for (int i = 0; i < satInfo.length; i++) {
			satInfo[i] = new pspUsbGpsSatInfo();
			read(satInfo[i]);
		}
	}

	@Override
	protected void write() {
		write16(satellitesInView);
		write16(garbage);
		for (int i = 0; satInfo != null && i < satInfo.length; i++) {
			write(satInfo[i]);
		}
	}

	public void setSatellitesInView(int satellitesInView) {
		this.satellitesInView = (short) satellitesInView;
		satInfo = new pspUsbGpsSatInfo[satellitesInView];
		for (int i = 0; i < satellitesInView; i++) {
			satInfo[i] = new pspUsbGpsSatInfo();
		}
	}

	@Override
	public int sizeof() {
		return 4 + MAX_SATELLITES * pspUsbGpsSatInfo.SIZEOF;
	}
}
