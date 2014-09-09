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

import jpcsp.util.Utilities;

/**
 * Individual Channel Stream
 */
public class IndividualChannelStream {
    public int maxSfb;            ///< number of scalefactor bands per group
    public int windowSequence[] = new int[2];
    public boolean useKbWindow[] = new boolean[2];   ///< If set, use Kaiser-Bessel window, otherwise use a sine window.
    public int numWindowGroups;
    public int groupLen[] = new int[8];
    public LongTermPrediction ltp = new LongTermPrediction();
    public int swbOffset[]; ///< table of offsets to the lowest spectral coefficient of a scalefactor band, sfb, for a particular window
    public int swbSizes[];   ///< table of scalefactor band sizes for a particular window
    public int numSwb;                ///< number of scalefactor window bands
    public int numWindows;
    public int tnsMaxBands;
    public boolean predictorPresent;
    public boolean predictorInitialized;
    public int predictorResetGroup;
    public boolean predictionUsed[] = new boolean[41];

    public void copy(IndividualChannelStream that) {
    	maxSfb = that.maxSfb;
    	Utilities.copy(windowSequence, that.windowSequence);
    	Utilities.copy(useKbWindow, that.useKbWindow);
    	numWindowGroups      = that.numWindowGroups;
    	Utilities.copy(groupLen, that.groupLen);
    	ltp.copy(that.ltp);
    	swbOffset            = that.swbOffset;
    	swbSizes             = that.swbSizes;
    	numSwb               = that.numSwb;
    	numWindows           = that.numWindows;
    	tnsMaxBands          = that.tnsMaxBands;
    	predictorPresent     = that.predictorPresent;
    	predictorInitialized = that.predictorInitialized;
    	predictorResetGroup  = that.predictorResetGroup;
    	Utilities.copy(predictionUsed, that.predictionUsed);
    }
}
