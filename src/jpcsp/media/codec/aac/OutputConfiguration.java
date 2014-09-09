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

import static jpcsp.media.codec.aac.AacDecoder.MAX_ELEM_ID;
import jpcsp.util.Utilities;

public class OutputConfiguration {
	public static final int OC_NONE        = 0; ///< Output unconfigured
	public static final int OC_TRIAL_PCE   = 1; ///< Output configuration under trial specified by an inband PCE
	public static final int OC_TRIAL_FRAME = 2; ///< Output configuration under trial specified by a frame header
	public static final int OC_GLOBAL_HDR  = 3; ///< Output configuration set in a global header but not yet locked
	public static final int OC_LOCKED      = 4; ///< Output configuration locked in place

	public MPEG4AudioConfig m4ac = new MPEG4AudioConfig();
	public int layoutMap[][] = new int[MAX_ELEM_ID*4][3];
	public int layoutMapTags;
	public int channels;
	public int channelLayout;
	public int status;

	public void copy(OutputConfiguration that) {
		m4ac.copy(that.m4ac);
		Utilities.copy(layoutMap, that.layoutMap);
		layoutMapTags = that.layoutMapTags;
		channels      = that.channels;
		channelLayout = that.channelLayout;
		status        = that.status;
	}
}
