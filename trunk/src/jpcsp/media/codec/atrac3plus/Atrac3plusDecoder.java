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
package jpcsp.media.codec.atrac3plus;

import static jpcsp.media.codec.util.CodecUtils.writeOutput;
import jpcsp.media.codec.ICodec;
import jpcsp.media.codec.util.BitReader;
import jpcsp.media.codec.util.FFT;

import org.apache.log4j.Logger;

/*
 * Based on the FFmpeg version from Maxim Poliakovski.
 * All credits go to him.
 */
public class Atrac3plusDecoder implements ICodec {
	public static Logger log = Logger.getLogger("atrac3plus");
	public static final int AT3P_ERROR = -1;
	public static final int CH_UNIT_MONO       = 0;        ///< unit containing one coded channel
	public static final int CH_UNIT_STEREO     = 1;        ///< unit containing two jointly-coded channels
	public static final int CH_UNIT_EXTENSION  = 2;        ///< unit containing extension information
	public static final int CH_UNIT_TERMINATOR = 3;        ///< unit sequence terminator
	public static final int ATRAC3P_POWER_COMP_OFF = 15;   ///< disable power compensation
	public static final int ATRAC3P_SUBBANDS = 16;         ///< number of PQF subbands
	public static final int ATRAC3P_SUBBAND_SAMPLES = 128; ///< number of samples per subband
	public static final int ATRAC3P_FRAME_SAMPLES = ATRAC3P_SUBBANDS * ATRAC3P_SUBBAND_SAMPLES;
	public static final int ATRAC3P_PQF_FIR_LEN = 12;      ///< length of the prototype FIR of the PQF
	private Context ctx;

	public static int avLog2(int n) {
		if (n == 0) {
			return 0;
		}
		return 31 - Integer.numberOfLeadingZeros(n);
	}

	@Override
	public int init(int bytesPerFrame, int channels, int outputChannels, int codingMode) {
		ChannelUnit.init();

		ctx = new Context();
		ctx.outputChannels = outputChannels;
		ctx.dsp = new Atrac3plusDsp();
		for (int i = 0; i < ctx.numChannelBlocks; i++) {
			ctx.channelUnits[i] = new ChannelUnit();
			ctx.channelUnits[i].setDsp(ctx.dsp);
		}

		// initialize IPQF
		ctx.ipqfDctCtx = new FFT();
		ctx.ipqfDctCtx.mdctInit(5, true, 31.0 / 32768.9);

		ctx.mdctCtx = new FFT();
		ctx.dsp.initImdct(ctx.mdctCtx);

		Atrac3plusDsp.initWaveSynth();

		ctx.gaincCtx = new Atrac();
		ctx.gaincCtx.initGainCompensation(6, 2);

		return 0;
	}

	@Override
	public int decode(int inputAddr, int inputLength, int outputAddr) {
		int ret;

		if (inputLength < 0) {
			return AT3P_ERROR;
		}
		if (inputLength == 0) {
			return 0;
		}

		ctx.br = new BitReader(inputAddr, inputLength);
		if (ctx.br.readBool()) {
			log.error(String.format("Invalid start bit"));
			return AT3P_ERROR;
		}

		int chBlock = 0;
		int channelsToProcess = 0;
		while (ctx.br.getBitsLeft() >= 2) {
			int chUnitId = ctx.br.read(2);
			if (chUnitId == CH_UNIT_TERMINATOR) {
				break;
			}
			if (chUnitId == CH_UNIT_EXTENSION) {
				log.warn(String.format("Non implemented channel unit extension"));
				return AT3P_ERROR;
			}

			if (chBlock >= ctx.channelUnits.length) {
				log.error(String.format("Too many channel blocks"));
				return AT3P_ERROR;
			}

			ctx.channelUnits[chBlock].setBitReader(ctx.br);

			ctx.channelUnits[chBlock].ctx.unitType = chUnitId;
			channelsToProcess = chUnitId + 1;
			ctx.channelUnits[chBlock].setNumChannels(channelsToProcess);

			ret = ctx.channelUnits[chBlock].decode();
			if (ret < 0) {
				return ret;
			}

			ctx.channelUnits[chBlock].decodeResidualSpectrum(ctx.samples);
			ctx.channelUnits[chBlock].reconstructFrame(ctx);

			writeOutput(ctx.outpBuf, outputAddr, ATRAC3P_FRAME_SAMPLES, ctx.outputChannels);

			chBlock++;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Bytes read 0x%X", ctx.br.getBytesRead()));
		}

		return ctx.br.getBytesRead();
	}

	@Override
	public int getNumberOfSamples() {
		return ATRAC3P_FRAME_SAMPLES;
	}
}
