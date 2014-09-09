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
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static jpcsp.media.codec.atrac3.Atrac3Data.clc_length_tab;
import static jpcsp.media.codec.atrac3.Atrac3Data.inv_max_quant;
import static jpcsp.media.codec.atrac3.Atrac3Data.mantissa_clc_tab;
import static jpcsp.media.codec.atrac3.Atrac3Data.mantissa_vlc_tab;
import static jpcsp.media.codec.atrac3.Atrac3Data.matrix_coeffs;
import static jpcsp.media.codec.atrac3.Atrac3Data.subband_tab;
import static jpcsp.media.codec.atrac3plus.Atrac.ff_atrac_sf_table;
import static jpcsp.media.codec.util.CodecUtils.writeOutput;
import static jpcsp.media.codec.util.FloatDSP.vectorFmul;
import static jpcsp.util.Utilities.signExtend;

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
	public int init(int blockAlign, int channels, int outputChannels, int codingMode) {
		int ret;

		initStaticData();

		ctx = new Context();
		ctx.channels = channels;
		ctx.outputChannels = outputChannels;
		ctx.codingMode = codingMode != 0 ? JOINT_STEREO : STEREO;
		ctx.blockAlign = blockAlign;

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

	/**
	 * Decode gain parameters for the coded bands
	 *
	 * @param block     the gainblock for the current band
	 * @param numBands  amount of coded bands
	 */
	private int decodeGainControl(GainBlock block, int numBands) {
		int b;

		for (b = 0; b <= numBands; b++) {
			block.gBlock[b].numPoints = br.read(3);
			int[] level = block.gBlock[b].levCode;
			int[] loc   = block.gBlock[b].locCode;

			for (int j = 0; j < block.gBlock[b].numPoints; j++) {
				level[j] = br.read(4);
				loc[j]   = br.read(5);
				if (j > 0 && loc[j] <= loc[j - 1]) {
					return AT3_ERROR;
				}
			}
		}

		// Clear the unused blocks
		for (; b < 4; b++) {
			block.gBlock[b].numPoints = 0;
		}

		return 0;
	}

	/**
	 * Restore the quantized tonal components
	 *
	 * @param components tonal components
	 * @param numBands   number of coded bands
	 */
	private int decodeTonalComponents(TonalComponent[] components, int numBands) {
		int bandFlags[] = new int[4];
		int mantissa[] = new int[8];
		int componentCount = 0;

		int nbComponents = br.read(5);

		// no tonal components
		if (nbComponents == 0) {
			return 0;
		}

		int codingModeSelector = br.read(2);
		if (codingModeSelector == 2) {
			return AT3_ERROR;
		}

		int codingMode = codingModeSelector & 1;

		for (int i = 0; i < nbComponents; i++) {
			for (int b = 0; b <= numBands; b++) {
				bandFlags[b] = br.read1();
			}

			int codedValuesPerComponent = br.read(3);

			int quantStepIndex = br.read(3);
			if (quantStepIndex <= 1) {
				return AT3_ERROR;
			}

			if (codingModeSelector == 3) {
				codingMode = br.read1();
			}

			for (int b = 0; b < (numBands + 1) * 4; b++) {
				if (bandFlags[b >> 2] == 0) {
					continue;
				}

				int codedComponents = br.read(3);

				for (int c = 0; c < codedComponents; c++) {
					if (componentCount >= 64) {
						return AT3_ERROR;
					}

					TonalComponent cmp = components[componentCount];

					int sfIndex = br.read(6);

					cmp.pos = b * 64 + br.read(6);

					int maxCodedValues = SAMPLES_PER_FRAME - cmp.pos;
					int codedValues = codedValuesPerComponent + 1;
					codedValues = min(maxCodedValues, codedValues);

					float scaleFactor = ff_atrac_sf_table[sfIndex] * inv_max_quant[quantStepIndex];

					readQuantSpectralCoeffs(quantStepIndex, codingMode, mantissa, codedValues);

					cmp.numCoefs = codedValues;

					// inverse quant
					for (int m = 0; m < codedValues; m++) {
						cmp.coef[m] = mantissa[m] * scaleFactor;
					}

					componentCount++;
				}
			}
		}

		return componentCount;
	}

	/**
	 * Mantissa decoding
	 *
	 * @param selector     which table the output values are coded with
	 * @param codingFlag   constant length coding or variable length coding
	 * @param mantissas    mantissa output table
	 * @param numCodes     number of values to get
	 */
	private void readQuantSpectralCoeffs(int selector, int codingFlag, int[] mantissas, int numCodes) {
		if (selector == 1) {
			numCodes /= 2;
		}

		if (codingFlag != 0) {
			// constant length coding (CLC)
			int numBits = clc_length_tab[selector];

			if (selector > 1) {
				for (int i = 0; i < numCodes; i++) {
					int code = (numBits != 0 ? signExtend(br.read(numBits), numBits) : 0);
					mantissas[i] = code;
				}
			} else {
				for (int i = 0; i < numCodes; i++) {
					int code = (numBits != 0 ? br.read(numBits) : 0); // numBits is always 4 in this case
					mantissas[i * 2    ] = mantissa_clc_tab[code >> 2];
					mantissas[i * 2 + 1] = mantissa_clc_tab[code &  3];
				}
			}
		} else {
			// variable length coding (VLC)
			if (selector != 1) {
				for (int i = 0; i < numCodes; i++) {
					int huffSymb = spectral_coeff_tab[selector - 1].getVLC2(br, 3);
					huffSymb += 1;
					int code = huffSymb >> 1;
					if ((huffSymb & 1) != 0) {
						code = -code;
					}
					mantissas[i] = code;
				}
			} else {
				for (int i = 0; i < numCodes; i++) {
					int huffSymb = spectral_coeff_tab[selector - 1].getVLC2(br, 3);
					mantissas[i * 2    ] = mantissa_vlc_tab[huffSymb * 2    ];
					mantissas[i * 2 + 1] = mantissa_vlc_tab[huffSymb * 2 + 1];
				}
			}
		}
	}

	/**
	 * Restore the quantized band spectrum coefficients
	 *
	 * @return subband count, fix for broken specification/files
	 */
	private int decodeSpectrum(float[] output) {
		int subbandVlcIndex[] = new int[32];
		int sfIndex[] = new int[32];
		int mantissas[] = new int[128];

		int numSubbands = br.read(5); // number of coded subbands;
		int codingMode = br.read(1);  // coding Mode: 0 - VLC/ 1-CLC

		// get the VLC selector table for the subbands, 0 means not coded
		for (int i = 0; i <= numSubbands; i++) {
			subbandVlcIndex[i] = br.read(3);
		}

		// read the scale factor indexes from the stream
		for (int i = 0; i <= numSubbands; i++) {
			if (subbandVlcIndex[i] != 0) {
				sfIndex[i] = br.read(6);
			}
		}

		int i;
		for (i = 0; i <= numSubbands; i++) {
			int first = subband_tab[i    ];
			int last  = subband_tab[i + 1];

			int subbandSize = last - first;

			if (subbandVlcIndex[i] != 0) {
	            // decode spectral coefficients for this subband
	            // TODO: This can be done faster is several blocks share the
	            // same VLC selector (subband_vlc_index)
				readQuantSpectralCoeffs(subbandVlcIndex[i], codingMode, mantissas, subbandSize);

				// decode the scale factor for this subband
				float scaleFactor = ff_atrac_sf_table[sfIndex[i]] * inv_max_quant[subbandVlcIndex[i]];

				// inverse quantize the coefficients
				for (int j = 0; first < last; first++, j++) {
					output[first] = mantissas[j] * scaleFactor;
				}
			} else {
				// this subband was not coded, so zero the entire subband
				Arrays.fill(output, first, first + subbandSize, 0f);
			}
		}

		// clear the subbands that were not coded
		Arrays.fill(output, subband_tab[i], SAMPLES_PER_FRAME, 0f);

		return numSubbands;
	}

	/**
	 * Combine the tonal band spectrum and regular band spectrum
	 *
	 * @param spectrum        output spectrum buffer
	 * @param numComponents   number of tonal components
	 * @param components      tonal components for this band
	 * @return                position of the last tonal coefficient
	 */
	private int addTonalComponents(float[] spectrum, int numComponents, TonalComponent[] components) {
		int lastPos = -1;
		for (int i = 0; i < numComponents; i++) {
			lastPos = Math.max(components[i].pos + components[i].numCoefs, lastPos);

			for (int j = 0; j < components[i].numCoefs; j++) {
				spectrum[components[i].pos + j] += components[i].coef[j];
			}
		}

		return lastPos;
	}

	private void reverseMatrixing(float[] su1, float[] su2, int[] prevCode, int[] currCode) {
		for (int i = 0, band = 0; band < 4 * 256; band += 256, i++) {
			int s1 = prevCode[i];
			int s2 = currCode[i];
			int nsample = band;

			if (s1 != s2) {
				// Selector value changed, interpolation needed.
				float mc1l = matrix_coeffs[s1 * 2    ];
				float mc1r = matrix_coeffs[s1 * 2 + 1];
				float mc2l = matrix_coeffs[s2 * 2    ];
				float mc2r = matrix_coeffs[s2 * 2 + 1];

				// Interpolation is done over the first eight samples.
				for (; nsample < band + 8; nsample++) {
					float c1 = su1[nsample];
					float c2 = su2[nsample];
					c2 = c1 * INTERPOLATE(mc1l, mc2l, nsample - band) +
					     c2 * INTERPOLATE(mc1r, mc2r, nsample - band);
					su1[nsample] = c2;
					su2[nsample] = c1 * 2f - c2;
				}
			}

			// Apply the matrix without interpolation.
			switch (s2) {
				case 0: // M/S decoding
					for (; nsample < band + 256; nsample++) {
						float c1 = su1[nsample];
						float c2 = su2[nsample];
						su1[nsample] =  c2       * 2f;
						su2[nsample] = (c1 - c2) * 2f;
					}
					break;
				case 1:
					for (; nsample < band + 256; nsample++) {
						float c1 = su1[nsample];
						float c2 = su2[nsample];
						su1[nsample] = (c1 + c2) *  2f;
						su2[nsample] =  c2       * -2f;
					}
					break;
				case 2:
				case 3:
					for (; nsample < band + 256; nsample++) {
						float c1 = su1[nsample];
						float c2 = su2[nsample];
						su1[nsample] = c1 + c2;
						su2[nsample] = c1 - c2;
					}
					break;
				default:
					log.fatal(String.format("Invalid s2 code %d", s2));
					break;
			}
		}
	}

	private void getChannelWeights(int index, int flag, float ch[]) {
		if (index == 7) {
			ch[0] = 1f;
			ch[1] = 1f;
		} else {
			ch[0] = (index & 7) / 7f;
			ch[1] = (float) sqrt(2f - ch[0] * ch[0]);
			if (flag != 0) {
				float tmp = ch[0];
				ch[0] = ch[1];
				ch[1] = tmp;
			}
		}
	}

	private float INTERPOLATE(float oldValue, float newValue, int nsample) {
		return oldValue + nsample * 0.125f * (newValue - oldValue);
	}

	private void channelWeighting(float[] su1, float[] su2, int[] p3) {
		// w[x][y] y=0 is left y=1 is right
		float w[][] = new float[2][2];

		if (p3[1] != 7 || p3[3] != 7) {
			getChannelWeights(p3[1], p3[0], w[0]);
			getChannelWeights(p3[3], p3[2], w[1]);

			for (int band = 256; band < 4 * 256; band += 256) {
				int nsample;
				for (nsample = band; nsample < band + 8; nsample++) {
					su1[nsample] *= INTERPOLATE(w[0][0], w[0][1], nsample - band);
					su2[nsample] *= INTERPOLATE(w[1][0], w[1][1], nsample - band);
				}
				for (; nsample < band + 256; nsample++) {
					su1[nsample] *= w[1][0];
					su2[nsample] *= w[1][1];
				}
			}
		}
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
			// reverse byte order so we need read in reverse direction
			br.seek(ctx.blockAlign - 1);
			br.setDirection(-1);

			// Skip the sync codes (0xF8).
			while (br.peek(8) == 0xF8) {
				br.read(8);
			}

			// Fill the Weighting coeffs delay buffer
			System.arraycopy(ctx.weightingDelay, 2, ctx.weightingDelay, 0, 4);
			ctx.weightingDelay[4] = br.read1();
			ctx.weightingDelay[5] = br.read(3);

			for (int i = 0; i < 4; i++) {
				ctx.matrixCoeffIndexPrev[i] = ctx.matrixCoeffIndexNow[i];
				ctx.matrixCoeffIndexNow[i] = ctx.matrixCoeffIndexNext[i];
				ctx.matrixCoeffIndexNext[i] = br.read(2);
			}

			// Decode sound Unit 2.
			ret = decodeChannelSoundUnit(ctx.units[1], ctx.samples[1], 1, JOINT_STEREO);
			br.setDirection(1);
			br.seek(ctx.blockAlign);

			if (ret != 0) {
				return ret;
			}

			// Reconstruct the channel coefficients
			reverseMatrixing(ctx.samples[0], ctx.samples[1], ctx.matrixCoeffIndexPrev, ctx.matrixCoeffIndexNow);

			channelWeighting(ctx.samples[0], ctx.samples[1], ctx.weightingDelay);
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

		writeOutput(ctx.samples, outputAddr, SAMPLES_PER_FRAME, ctx.outputChannels);

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
