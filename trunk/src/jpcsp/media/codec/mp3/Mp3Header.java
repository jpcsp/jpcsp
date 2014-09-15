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
package jpcsp.media.codec.mp3;

public class Mp3Header {
	public int frameSize;
	public int errorProtection;
	public int layer;
	public int sampleRate;
	public int sampleRateIndex; // between 0 and 8
	public int bitRate;
	public int nbChannels;
	public int mode;
	public int modeExt;
	public int lsf;
	public int version;
	public int maxSamples;

	@Override
	public String toString() {
		return String.format("Mp3Header[version %d, layer%d, %d Hz, %d kbits/s, %s]", version, layer, sampleRate, bitRate, nbChannels == 2 ? "stereo" : "mono");
	}
}
