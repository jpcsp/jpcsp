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

import static jpcsp.media.codec.aac.AacDecoder.MAX_CHANNELS;

/**
 * Dynamic Range Control - decoded from the bitstream but not processed further.
 */
public class DynamicRangeControl {
    int pceInstanceTag;                           ///< Indicates with which program the DRC info is associated.
    int dynRngSgn[] = new int[17];                ///< DRC sign information; 0 - positive, 1 - negative
    int dynRngCtl[] = new int[17];                ///< DRC magnitude information
    int excludeMask[] = new int[MAX_CHANNELS];    ///< Channels to be excluded from DRC processing.
    int bandIncr;                                 ///< Number of DRC bands greater than 1 having DRC info.
    int interpolationScheme;                      ///< Indicates the interpolation scheme used in the SBR QMF domain.
    int bandTop[] = new int[17];                  ///< Indicates the top of the i-th DRC band in units of 4 spectral lines.
    int progRefLevel;                             /**< A reference level for the long-term program audio level for all
                                                   *   channels combined.
                                                   */
}
