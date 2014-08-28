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
package jpcsp.media.atrac3plus;

public class SineWin {
	public static final float[] ff_sine_64  = new float[64];
	public static final float[] ff_sine_128 = new float[128];

	private static void sineWindowInit(float[] window) {
		int n = window.length;
		for (int i = 0; i < n; i++) {
			window[i] = (float) Math.sin((i + 0.5) * (Math.PI / (2.0 * n)));
		}
	}

	public static void initFfSineWindows() {
		sineWindowInit(ff_sine_64 );
		sineWindowInit(ff_sine_128);
	}
}
