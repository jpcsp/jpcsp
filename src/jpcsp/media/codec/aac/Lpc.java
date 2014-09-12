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
package jpcsp.media.codec.aac;

import org.apache.log4j.Logger;

public class Lpc {
	private static Logger log = AacDecoder.log;

	/**
	 * Levinson-Durbin recursion.
	 * Produce LPC coefficients from autocorrelation data.
	 */
	public static int computeLpcCoefs(float autoc[], int maxOrder, float lpc[], int lpcStride, boolean fail, boolean normalize) {
		float err = 0f;
		int autocOffset = 0;
		int lpcOffset = 0;
		int lpcLast = lpcOffset;

		if (!(normalize || !fail)) {
			log.error(String.format("computeLpcCoefs invalid parameters"));
		}

		if (normalize) {
			err = autoc[autocOffset++];
		}

		if (fail && (autoc[autocOffset + maxOrder - 1] == 0 || err <= 0f)) {
			return -1;
		}

		for (int i = 0; i < maxOrder; i++) {
			float r = -autoc[autocOffset + i];

			if (normalize) {
				for (int j = 0; j < i; j++) {
					r -= lpc[lpcLast + j] * autoc[autocOffset + i - j - 1];
				}

				r /= err;
				err *= 1f - (r * r);
			}

			lpc[lpcOffset + i] = r;

			for (int j = 0; j < ((i + 1) >> 1); j++) {
				float f = lpc[lpcLast +         j];
				float b = lpc[lpcLast + i - 1 - j];
				lpc[lpcOffset +         j] = f + r * b;
				lpc[lpcOffset + i - 1 - j] = b + r * f;
			}

			if (fail && err < 0) {
				return -1;
			}

			lpcLast = lpcOffset;
			lpcOffset += lpcStride;
		}

		return 0;
	}
}
