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

public class MPEG4AudioConfig {
	public int objectType;
	public int samplingIndex;
	public int sampleRate;
	public int chanConfig;
	public int sbr; ///< -1 implicit, 1 presence
	public int extObjectType;
	public int extSamplingIndex;
	public int extSampleRate;
	public int extChanConfig;
	public int ps; ///< -1 implicit, 1 presence

	public void copy(MPEG4AudioConfig that) {
		objectType       = that.objectType;
		samplingIndex    = that.samplingIndex;
		sampleRate       = that.sampleRate;
		chanConfig       = that.chanConfig;
		sbr              = that.sbr;
		extObjectType    = that.extObjectType;
		extSamplingIndex = that.extSamplingIndex;
		extSampleRate    = that.extSampleRate;
		extChanConfig    = that.extChanConfig;
	}
}
