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

import static jpcsp.media.codec.mp3.Mp3Decoder.LAST_BUF_SIZE;
import static jpcsp.media.codec.mp3.Mp3Decoder.MP3_MAX_CHANNELS;
import static jpcsp.media.codec.mp3.Mp3Decoder.SBLIMIT;
import jpcsp.media.codec.util.BitReader;

public class Context {
	public BitReader br;
	public Mp3Header header = new Mp3Header();
	public int frameSize;
	public float samples[][];
	public int lastBuf[] = new int[LAST_BUF_SIZE];
	public int lastBufSize;
	public float synthBuf[][] = new float[MP3_MAX_CHANNELS][512 * 2];
	public int synthBufOffset[] = new int[MP3_MAX_CHANNELS];
	public float sbSamples[][] = new float[MP3_MAX_CHANNELS][36 * SBLIMIT];
	public float mdctBuf[][] = new float[MP3_MAX_CHANNELS][SBLIMIT * 18]; // previous samples, for layer 3 MDCT
	public Granule granules[][] = new Granule[2][2]; // Used in Layer 3
	public int aduMode; ///<0 for standard mp3, 1 for adu formatted mp3
	public int ditherState[] = new int[1];
	public int errRecognition;

	public Context() {
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				granules[i][j] = new Granule();
			}
		}
	}
}
