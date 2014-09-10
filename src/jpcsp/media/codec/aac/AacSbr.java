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

import static jpcsp.media.codec.aac.AacDecoder.TYPE_CCE;
import static jpcsp.media.codec.aac.AacDecoder.TYPE_SCE;
import jpcsp.media.codec.util.FFT;
import jpcsp.media.codec.util.IBitReader;

import org.apache.log4j.Logger;

public class AacSbr {
	private static Logger log = AacDecoder.log;
	private static final int EXTENSION_ID_PS = 2;

	private static int readSbrSingleChannelElement(Context ac, SpectralBandReplication sbr) {
		log.warn("Not implemented readSbrSingleChannelElement");
		// TODO
		return 0;
	}

	private static int readSbrExtension(Context ac, SpectralBandReplication sbr, int bsExtensionId, int numBitsLeft) {
		switch (bsExtensionId) {
			case EXTENSION_ID_PS:
				if (ac.oc[1].m4ac.ps == 0) {
					log.error(String.format("Parametric Stereo signaled to be not-present but was found in the bitstream"));
					ac.br.skip(numBitsLeft);
					numBitsLeft = 0;
				} else {
					numBitsLeft -= AacPs.readData(ac, sbr.ps, numBitsLeft);
				}
				break;
			default:
				// some files contain 0-padding
				if (bsExtensionId != 0 || numBitsLeft > 16 || ac.br.peek(numBitsLeft) != 0) {
					log.error(String.format("Reserved SBR extensions"));
				}
				ac.br.skip(numBitsLeft);
				numBitsLeft = 0;
				break;
		}

		return numBitsLeft;
	}

	private static int readSbrData(Context ac, SpectralBandReplication sbr, int idAac) {
		int cnt = ac.br.getBitsRead();

		if (idAac == TYPE_SCE || idAac == TYPE_CCE) {
			if (readSbrSingleChannelElement(ac, sbr) != 0) {
				sbrTurnoff(sbr);
				return ac.br.getBitsRead() - cnt;
			}
		} else {
			log.error(String.format("Invalid bitstream - cannot apply SBR to element type %d", idAac));
			sbrTurnoff(sbr);
			return ac.br.getBitsRead() - cnt;
		}

		if (ac.br.readBool()) { // bs_extended_data
			int numBitsLeft = ac.br.read(4); // bs_extension_size
			if (numBitsLeft == 15) {
				numBitsLeft += ac.br.read(8); // bs_esc_count
			}

			numBitsLeft <<= 3;
			while (numBitsLeft > 7) {
				numBitsLeft -= 2;
				numBitsLeft = readSbrExtension(ac, sbr, ac.br.read(2), numBitsLeft);
			}

			if (numBitsLeft < 0) {
				log.error(String.format("SBD Extension over read"));
			} else if (numBitsLeft > 0) {
				ac.br.skip(numBitsLeft);
			}
		}

		return ac.br.getBitsRead() - cnt;
	}

	/// Master Frequency Band Table (14496-3 sp04 p194)
	private static int sbrMakeFMaster(Context ac, SpectralBandReplication sbr, SpectrumParameters spectrum) {
		log.warn("sbrMakeFMaster not implemented");
		// TODO
		return 0;
	}

	/// Derived Frequency Band Tables (14496-3 sp04 p197)
	private static int sbrMakeFDerived(Context ac, SpectralBandReplication sbr) {
		log.warn("sbrMakeFDerived not implemented");
		// TODO
		return 0;
	}

	private static int readSbrHeader(SpectralBandReplication sbr, IBitReader br) {
		log.warn("readSbrHeader not implemented");
		// TODO
		return 0;
	}

	private static void sbrReset(Context ac, SpectralBandReplication sbr) {
		int err = sbrMakeFMaster(ac, sbr, sbr.spectrumParams);
		if (err >= 0) {
			err = sbrMakeFDerived(ac, sbr);
		}
		if (err < 0) {
			log.error(String.format("SBR reset failed. Switching SBR to pure upsampling mode"));
			sbrTurnoff(sbr);
		}
	}

	/**
	 * Decode Spectral Band Replication extension data; reference: table 4.55.
	 *
	 * @param   crc flag indicating the presence of CRC checksum
	 * @param   cnt length of TYPE_FIL syntactic element in bytes
	 *
	 * @return  Returns number of bytes consumed from the TYPE_FIL element.
	 */
	public static int decodeSbrExtension(Context ac, SpectralBandReplication sbr, boolean crc, int cnt, int idAac) {
		int numSbrBits = 0;

		sbr.reset = false;

		if (sbr.sampleRate == 0) {
			sbr.sampleRate = 2 * ac.oc[1].m4ac.sampleRate;
		}
		if (ac.oc[1].m4ac.extSampleRate == 0) {
			ac.oc[1].m4ac.extSampleRate = 2 * ac.oc[1].m4ac.sampleRate;
		}

		if (crc) {
			ac.br.skip(10); // bs_sbr_crc_bits
			numSbrBits += 10;
		}

		// Save some state from the previous frame
		sbr.kx[0] = sbr.kx[1];
		sbr.m[0] = sbr.m[1];
		sbr.kxAndMPushed = true;

		numSbrBits++;
		if (ac.br.readBool()) { // bs_header_flag
			numSbrBits += readSbrHeader(sbr, ac.br);
		}

		if (sbr.reset) {
			sbrReset(ac, sbr);
		}

		if (sbr.start != 0) {
			numSbrBits += readSbrData(ac, sbr, idAac);
		}

		int numSkipBits = (cnt * 8 - 4 - numSbrBits);
		ac.br.skip(numSkipBits);

		int numAlignBits = numSkipBits & 7;
		int bytesRead = (numSbrBits + numAlignBits + 4) >> 3;

		if (bytesRead > cnt) {
			log.error(String.format("Expected to read %d SBR bytes actually read %d", cnt, bytesRead));
		}

		return cnt;
	}

	// Places SBR in pure upsampling mode
	private static void sbrTurnoff(SpectralBandReplication sbr) {
		sbr.start = 0;
		// Init defaults used in pure upsampling mode
		sbr.kx[1] = 32; // Typo in spec, kx' inits to 32
		sbr.m[1] = 0;
		// Reset values for first SBR header
		sbr.data[0].eA[1] = -1;
		sbr.data[1].eA[1] = -1;
		sbr.spectrumParams.reset();
	}

	public static void ctxInit(SpectralBandReplication sbr) {
		if (sbr.mdct != null) {
			return;
		}

		sbr.kx[0] = sbr.kx[1];
		sbrTurnoff(sbr);
		sbr.data[0].synthesisFilterbankSamplesOffset = SBRData.SBR_SYNTHESIS_BUF_SIZE - (1280 - 128);
		sbr.data[1].synthesisFilterbankSamplesOffset = SBRData.SBR_SYNTHESIS_BUF_SIZE - (1280 - 128);
	    /* SBR requires samples to be scaled to +/-32768.0 to work correctly.
	     * mdct scale factors are adjusted to scale up from +/-1.0 at analysis
	     * and scale back down at synthesis. */
		sbr.mdct = new FFT();
		sbr.mdct.mdctInit(7, true, 1.9 / (64 * 32768.0));
		sbr.mdctAna = new FFT();
		sbr.mdctAna.mdctInit(7, true, -2.0 * 32768.0);
		sbr.ps = new PSContext();
	}

	public static void ctxClose(SpectralBandReplication sbr) {
		sbr.mdct = null;
		sbr.mdctAna = null;
	}
}
