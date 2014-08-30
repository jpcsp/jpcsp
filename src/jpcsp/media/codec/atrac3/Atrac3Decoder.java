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
package jpcsp.media.codec.atrac3;

import static java.lang.Math.max;
import static java.lang.Math.sin;
import static jpcsp.media.codec.atrac3.Atrac3Data.subband_tab;
import static jpcsp.media.codec.util.CodecUtils.writeOutput;
import static jpcsp.media.codec.util.FloatDSP.vectorFmul;

import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.atrac3plus.Atrac;
import jpcsp.media.codec.util.BitReader;
import jpcsp.media.codec.util.FFT;
import jpcsp.media.codec.util.VLC;

/*
 * Based on the FFmpeg version from Maxim Poliakovski and Benjamin Larsson.
 * All credits go to them.
 */
public class Atrac3Decoder implements ICodec {
	public static Logger log = Logger.getLogger("atrac3");
	public static final int AT3_ERROR = -2;
	public static final int JOINT_STEREO = 0x12;
	public static final int STEREO       = 0x2;
	public static final int SAMPLES_PER_FRAME = 1024;
	private static final int MDCT_SIZE = 512;
	private static final float[] mdct_window = new float[MDCT_SIZE];
	private static final VLC[] spectral_coeff_tab = new VLC[7];
	private Context ctx;
	private BitReader br;
	private static boolean staticInitDone = false;

	private static void initStaticData() {
		if (staticInitDone) {
			return;
		}

		initImdctWindow();
		Atrac.generateTables();

		// Initialize the VLC tables
		for (int i = 0; i < 7; i++) {
			spectral_coeff_tab[i] = new VLC();
			spectral_coeff_tab[i].initVLCSparse(9, Atrac3Data.huff_tab_sizes[i], Atrac3Data.huff_bits[i], Atrac3Data.huff_codes[i], null);
		}

		staticInitDone = true;
	}

	@Override
	public int init() {
		int ret;

		initStaticData();

		ctx = new Context();

		// initialize th MDCT transform
		ctx.mdctCtx = new FFT();
		ret = ctx.mdctCtx.mdctInit(9, true, 1.0 / 32768.0);
		if (ret < 0) {
			return ret;
		}

		// init the joint-stereo decoding data
		ctx.weightingDelay[0] = 0;
		ctx.weightingDelay[1] = 7;
		ctx.weightingDelay[2] = 0;
		ctx.weightingDelay[3] = 7;
		ctx.weightingDelay[4] = 0;
		ctx.weightingDelay[5] = 7;

		for (int i = 0; i < 4; i++) {
			ctx.matrixCoeffIndexPrev[i] = 3;
			ctx.matrixCoeffIndexNow[i]  = 3;
			ctx.matrixCoeffIndexNext[i] = 3;
		}

		ctx.gaincCtx = new Atrac();
		ctx.gaincCtx.initGainCompensation(4, 3);

		for (int i = 0; i < ctx.units.length; i++) {
			ctx.units[i] = new ChannelUnit();
		}

		return 0;
	}

	private void imlt(float[] input, int inputOffset, float[] output, int outputOffset, boolean oddBand) {
		if (oddBand) {
	        /**
	         * Reverse the odd bands before IMDCT, this is an effect of the QMF
	         * transform or it gives better compression to do it this way.
	         * FIXME: It should be possible to handle this in imdct_calc
	         * for that to happen a modification of the prerotation step of
	         * all SIMD code and C code is needed.
	         * Or fix the functions before so they generate a pre reversed spectrum.
	         */
			for (int i = 0; i < 128; i++) {
				float tmp = input[inputOffset + i];
				input[inputOffset + i] = input[inputOffset + 255 - i];
				input[inputOffset + 255 - i] = tmp;
			}
		}

		ctx.mdctCtx.imdctCalc(output, outputOffset, input, inputOffset);

		// Perform windowing on the output
		vectorFmul(output, outputOffset, output, outputOffset, mdct_window, 0, MDCT_SIZE);
	}

	private static void initImdctWindow() {
		// generate the mdct window, for details see
		// http://wiki.multimedia.cx/index.php?title=RealAudio_atrc#Windows
		for (int i = 0, j = 255; i < 128; i++, j--) {
			float wi = (float) sin(((i + 0.5) / 256.0 - 0.5) * Math.PI) + 1.0f;
			float wj = (float) sin(((j + 0.5) / 256.0 - 0.5) * Math.PI) + 1.0f;
			float w  = 0.5f * (wi * wi + wj * wj);
			mdct_window[i] = wi / w;
			mdct_window[j] = wj / w;
			mdct_window[511 - i] = wi / w;
			mdct_window[511 - j] = wj / w;
		}
	}

	private int decodeGainControl(GainBlock gain, int bandsCoded) {
		// TODO
		return 0;
	}

	private int decodeTonalComponents(TonalComponent[] components, int bandsCoded) {
		// TODO
		return 0;
	}

	private int decodeSpectrum(float[] spectrum) {
		// TODO
		return 0;
	}

	private int addTonalComponents(float[] spectrum, int numComponents, TonalComponent[] components) {
		// TODO
		return 0;
	}

	/**
	 * Decode a Sound Unit
	 *
	 * @param snd           the channel unit to be used
	 * @param output        the decoded samples before IQMF in float representation
	 * @param channelNum    channel number
	 * @param codingMode    the coding mode (JOINT_STEREO or regular stereo/mono)
	 */
	private int decodeChannelSoundUnit(ChannelUnit snd, float[] output, int channelNum, int codingMode) {
		int ret;
		GainBlock gain1 = snd.gainBlock[    snd.gcBlkSwitch];
		GainBlock gain2 = snd.gainBlock[1 - snd.gcBlkSwitch];

		if (codingMode == JOINT_STEREO && channelNum == 1) {
			if (br.read(2) != 3) {
				log.error(String.format("JS mono Sound Unit id != 3"));
				return AT3_ERROR;
			}
		} else {
			if (br.read(6) != 0x28) {
				log.error(String.format("Sound Unit id != 0x28"));
				return AT3_ERROR;
			}
		}

		// number of coded QMF bands
		snd.bandsCoded = br.read(2);

		ret = decodeGainControl(gain2, snd.bandsCoded);
		if (ret != 0) {
			return ret;
		}

		snd.numComponents = decodeTonalComponents(snd.components, snd.bandsCoded);

		if (snd.numComponents < 0) {
			return snd.numComponents;
		}

		int numSubbands = decodeSpectrum(snd.spectrum);

		// Merge the decoded spectrum and tonal components
		int lastTonal = addTonalComponents(snd.spectrum, snd.numComponents, snd.components);

		// calculate number of used MLT/QMF bands according to the amount of coded
		// spectral lines
		int numBands = (subband_tab[numSubbands] - 1) >> 8;
		if (lastTonal >= 0) {
			numBands = max((lastTonal + 256) >> 8, numBands);
		}

		// Reconstruct time domain samples
		for (int band = 0; band < 4; band++) {
			// Perform the IMDCT step without overlapping
			if (band <= numBands) {
				imlt(snd.spectrum, band * 256, snd.imdctBuf, 0, (band & 1) != 0);
			} else {
				Arrays.fill(snd.imdctBuf, 0, 512, 0f);
			}

			// gain compensation and overlapping
			ctx.gaincCtx.gainCompensation(snd.imdctBuf, 0, snd.prevFrame, band * 256, gain1.gBlock[band], gain2.gBlock[band], 256, output, band * 256);
		}

		// Swap the gain control buffers for the next frame
		snd.gcBlkSwitch ^= 1;

		return 0;
	}

	private int decodeFrame() {
		int ret;

		if (ctx.codingMode == JOINT_STEREO) {
			// channel coupling mode
			// decode Sound Unit 1
			ret = decodeChannelSoundUnit(ctx.units[0], ctx.samples[0], 0, JOINT_STEREO);
			if (ret != 0) {
				return ret;
			}

			// Framedata of the su2 in the joint-stereo mode is encoded in
			// reverse byte order so we need to swap it first

			// TODO
			log.error("JOINT_STEREO not implemented yet");
		} else {
			// normal stereo mode or mono
			// Decode the channel sound units
			for (int i = 0; i < ctx.channels; i++) {
				// Set the bitstream reader at the start of a channel sound unit
				br.seek(i * ctx.blockAlign / ctx.channels);

				ret = decodeChannelSoundUnit(ctx.units[i], ctx.samples[i], i, ctx.codingMode);
				if (ret != 0) {
					return ret;
				}
			}
		}

		// Apply the iQMF synthesis filter
		for (int i = 0; i < ctx.channels; i++) {
			Atrac.iqmf(ctx.samples[i],   0, ctx.samples[i], 256, 256, ctx.samples[i],   0, ctx.units[i].delayBuf1, ctx.tempBuf);
			Atrac.iqmf(ctx.samples[i], 768, ctx.samples[i], 512, 256, ctx.samples[i], 512, ctx.units[i].delayBuf2, ctx.tempBuf);
			Atrac.iqmf(ctx.samples[i],   0, ctx.samples[i], 512, 512, ctx.samples[i],   0, ctx.units[i].delayBuf3, ctx.tempBuf);
		}

		return 0;
	}

	@Override
	public int decode(int inputAddr, int inputLength, int outputAddr) {
		br = new BitReader(inputAddr, inputLength);
		ctx.br = br;

		int ret = decodeFrame();
		if (ret < 0) {
			return ret;
		}

		writeOutput(ctx.samples, outputAddr, SAMPLES_PER_FRAME);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Bytes read 0x%X", ctx.br.getBytesRead()));
		}

		return ctx.br.getBytesRead();
	}

	@Override
	public int getNumberOfSamples() {
		return SAMPLES_PER_FRAME;
	}
}
