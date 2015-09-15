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
package jpcsp.sound;

import static jpcsp.HLE.modules.sceSasCore.PSP_SAS_ADSR_CURVE_MODE_DIRECT;
import static jpcsp.HLE.modules.sceSasCore.PSP_SAS_ADSR_CURVE_MODE_EXPONENT_DECREASE;
import static jpcsp.HLE.modules.sceSasCore.PSP_SAS_ADSR_CURVE_MODE_EXPONENT_INCREASE;
import static jpcsp.HLE.modules.sceSasCore.PSP_SAS_ADSR_CURVE_MODE_LINEAR_BENT;
import static jpcsp.HLE.modules.sceSasCore.PSP_SAS_ADSR_CURVE_MODE_LINEAR_DECREASE;
import static jpcsp.HLE.modules.sceSasCore.PSP_SAS_ADSR_CURVE_MODE_LINEAR_INCREASE;
import static jpcsp.HLE.modules.sceSasCore.PSP_SAS_ENVELOPE_FREQ_MAX;
import static jpcsp.HLE.modules.sceSasCore.PSP_SAS_ENVELOPE_HEIGHT_MAX;
import static jpcsp.sound.SoundMixer.getSampleLeft;
import static jpcsp.sound.SoundMixer.getSampleRight;

import org.apache.log4j.Logger;

import jpcsp.HLE.modules.sceSasCore;
import jpcsp.sound.SoundVoice.VoiceADSREnvelope;

/**
 * @author gid15
 *
 */
public class SampleSourceWithADSR implements ISampleSource {
	private static Logger log = SoftwareSynthesizer.log;
	private ISampleSource sampleSource;
	private SoundVoice voice;
	private EnvelopeState envelopeState;
	private final boolean tracing;
	private static final int ATTACK_CURVE_STATE  = 0;
	private static final int DECAY_CURVE_STATE   = 1;
	private static final int SUSTAIN_CURVE_STATE = 2;
	private static final int RELEASE_CURVE_STATE = 3;

	/**
	 * Keep track of an envelope state:
	 * - the 4 defined curve types (attack, decay, sustain, release)
	 * - the current active curve
	 * - the current envelope height
	 */
	private static class EnvelopeState {
		private VoiceADSREnvelope envelope;
		// It really makes life easier to use a "long" value and not have to
		// handle with overflows while doing "int" arithmetic.
		private long envelopeHeight;
		private int curveState;
		private int indexExp;
		private final boolean tracing;

		public EnvelopeState(VoiceADSREnvelope envelope) {
			this.envelope = envelope;
			tracing = sceSasCore.log.isTraceEnabled();
		}

		public void resetToStart() {
			indexExp = 0;
			envelopeHeight = 0;
			curveState = ATTACK_CURVE_STATE;
		}

		private static final short[] expCurve = new short[] {
			0x0000, 0x0380, 0x06E4, 0x0A2D, 0x0D5B, 0x1072, 0x136F, 0x1653,
			0x1921, 0x1BD9, 0x1E7B, 0x2106, 0x237F, 0x25E4, 0x2835, 0x2A73,
			0x2CA0, 0x2EBB, 0x30C6, 0x32C0, 0x34AB, 0x3686, 0x3852, 0x3A10,
			0x3BC0, 0x3D63, 0x3EF7, 0x4081, 0x41FC, 0x436E, 0x44D3, 0x462B,
			0x477B, 0x48BF, 0x49FA, 0x4B2B, 0x4C51, 0x4D70, 0x4E84, 0x4F90,
			0x5095, 0x5191, 0x5284, 0x5370, 0x5455, 0x5534, 0x5609, 0x56D9,
			0x57A3, 0x5867, 0x5924, 0x59DB, 0x5A8C, 0x5B39, 0x5BE0, 0x5C81,
			0x5D1C, 0x5DB5, 0x5E48, 0x5ED5, 0x5F60, 0x5FE5, 0x6066, 0x60E2,
			0x615D, 0x61D2, 0x6244, 0x62B2, 0x631D, 0x6384, 0x63E8, 0x644A,
			0x64A8, 0x6503, 0x655B, 0x65B1, 0x6605, 0x6653, 0x66A2, 0x66ED,
			0x6737, 0x677D, 0x67C1, 0x6804, 0x6844, 0x6882, 0x68BF, 0x68F9,
			0x6932, 0x6969, 0x699D, 0x69D2, 0x6A03, 0x6A34, 0x6A63, 0x6A8F,
			0x6ABC, 0x6AE6, 0x6B0E, 0x6B37, 0x6B5D, 0x6B84, 0x6BA7, 0x6BCB,
			0x6BED, 0x6C0E, 0x6C2D, 0x6C4D, 0x6C6B, 0x6C88, 0x6CA4, 0x6CBF,
			0x6CD9, 0x6CF3, 0x6D0C, 0x6D24, 0x6D3B, 0x6D52, 0x6D68, 0x6D7D,
			0x6D91, 0x6DA6, 0x6DB9, 0x6DCA, 0x6DDE, 0x6DEF, 0x6DFF, 0x6E10,
			0x6E20, 0x6E30, 0x6E3E, 0x6E4C, 0x6E5A, 0x6E68, 0x6E76, 0x6E82,
			0x6E8E, 0x6E9B, 0x6EA5, 0x6EB1, 0x6EBC, 0x6EC6, 0x6ED1, 0x6EDB,
			0x6EE4, 0x6EED, 0x6EF6, 0x6EFE, 0x6F07, 0x6F10, 0x6F17, 0x6F20,
			0x6F27, 0x6F2E, 0x6F35, 0x6F3C, 0x6F43, 0x6F48, 0x6F4F, 0x6F54,
			0x6F5B, 0x6F60, 0x6F66, 0x6F6B, 0x6F70, 0x6F74, 0x6F79, 0x6F7E,
			0x6F82, 0x6F87, 0x6F8A, 0x6F90, 0x6F93, 0x6F97, 0x6F9A, 0x6F9E,
			0x6FA1, 0x6FA5, 0x6FA8, 0x6FAC, 0x6FAD, 0x6FB1, 0x6FB4, 0x6FB6,
			0x6FBA, 0x6FBB, 0x6FBF, 0x6FC1, 0x6FC4, 0x6FC6, 0x6FC8, 0x6FC9,
			0x6FCD, 0x6FCF, 0x6FD0, 0x6FD2, 0x6FD4, 0x6FD6, 0x6FD7, 0x6FD9,
			0x6FDB, 0x6FDD, 0x6FDE, 0x6FDE, 0x6FE0, 0x6FE2, 0x6FE4, 0x6FE5,
			0x6FE5, 0x6FE7, 0x6FE9, 0x6FE9, 0x6FEB, 0x6FEC, 0x6FEC, 0x6FEE,
			0x6FEE, 0x6FF0, 0x6FF0, 0x6FF2, 0x6FF2, 0x6FF3, 0x6FF3, 0x6FF5,
			0x6FF5, 0x6FF7, 0x6FF7, 0x6FF7, 0x6FF9, 0x6FF9, 0x6FF9, 0x6FFA,
			0x6FFA, 0x6FFA, 0x6FFC, 0x6FFC, 0x6FFC, 0x6FFE, 0x6FFE, 0x6FFE,
			0x7000
		};
		private static final short expCurveReference = 0x7000;

		private static short extrapolateSample(short[] curve, int index, int duration) {
			float curveIndex = (index * curve.length) / (float) duration;
			int curveIndex1 = (int) curveIndex;
			int curveIndex2 = curveIndex1 + 1;
			float curveIndexFraction = curveIndex - curveIndex1;

			if (curveIndex1 < 0) {
				return curve[0];
			} else if (curveIndex2 >= curve.length || curveIndex2 < 0) {
				return curve[curve.length - 1];
			}

			float sample = curve[curveIndex1] * (1.f - curveIndexFraction) + curve[curveIndex2] * curveIndexFraction;

			return (short) Math.round(sample);
		}

		private long stepCurveExp(int rate) {
			int duration;
			if (rate == 0) {
				duration = PSP_SAS_ENVELOPE_FREQ_MAX;
			} else {
				// From experimental tests on a PSP:
				//   rate=0x7FFFFFFF => duration=0x10
				//   rate=0x3FFFFFFF => duration=0x22
				//   rate=0x1FFFFFFF => duration=0x44
				//   rate=0x0FFFFFFF => duration=0x81
				//   rate=0x07FFFFFF => duration=0xF1
				//   rate=0x03FFFFFF => duration=0x1B9
				//
				// The correct curve model is still unknown.
				// We use the following approximation:
				//   duration = 0x7FFFFFFF / rate * 0x10
				duration = PSP_SAS_ENVELOPE_FREQ_MAX / rate * 0x10;
			}

			short expFactor = extrapolateSample(expCurve, indexExp, duration);

			indexExp++;

			return ((long) expFactor) * PSP_SAS_ENVELOPE_HEIGHT_MAX / expCurveReference;
		}

		private void setCurve(int curve) {
			if (this.curveState != curve) {
				this.curveState = curve;
				indexExp = 0;
			}
		}

		/**
		 * Return the given envelope height as a 32-bit integer value by
		 * cutting the value to the allowed range [0..0x40000000].
		 *
		 * @param envelopeHeight    33-bit integer value
		 * @return                  32-bit integer value [0..0x40000000]
		 */
		private int getIntEnvelopeHeight(long envelopeHeight) {
			if (envelopeHeight <= 0) {
				return 0;
			}
			if (envelopeHeight >= PSP_SAS_ENVELOPE_HEIGHT_MAX) {
				return PSP_SAS_ENVELOPE_HEIGHT_MAX;
			}
			return (int) envelopeHeight;
		}

		private void stepCurve(int type, int rate) {
			switch (type) {
				case PSP_SAS_ADSR_CURVE_MODE_LINEAR_INCREASE:
					// The curve value will increase linearly according to its rate.
					envelopeHeight += rate;
					break;
				case PSP_SAS_ADSR_CURVE_MODE_LINEAR_DECREASE:
					// The curve value will decrease linearly according to its rate.
					envelopeHeight -= rate;
					break;
				case PSP_SAS_ADSR_CURVE_MODE_LINEAR_BENT:
					// The curve value will increase linearly according to its rate up to 75%
					// of the maximum envelope value. Over 75%, the curve value will
					// increase linearly according to 1/4th of its rate.
					// Between 0% and 75%: linear increase with given rate.
					// Between 75% and 100%: linear increase with 1/4th of the rate.
					if (envelopeHeight <= (PSP_SAS_ENVELOPE_HEIGHT_MAX / 4 * 3)) {
						envelopeHeight += rate;
					} else {
						envelopeHeight += rate >> 2;
					}
					break;
				case PSP_SAS_ADSR_CURVE_MODE_EXPONENT_DECREASE:
					// The curve value will decrease exponentially according to its rate.
					// The exact curve algorithm is still unknown. The same empirical approximation
					// as for the curve type PSP_SAS_ADSR_CURVE_MODE_EXPONENT_INCREASE is used.
					envelopeHeight = PSP_SAS_ENVELOPE_HEIGHT_MAX - stepCurveExp(rate);
					break;
				case PSP_SAS_ADSR_CURVE_MODE_EXPONENT_INCREASE:
					// The curve value will increase exponentially according to its rate.
					// The exact curve algorithm is still unknown. An empirical approximation
					// is used.
					envelopeHeight = stepCurveExp(rate);
					break;
				case PSP_SAS_ADSR_CURVE_MODE_DIRECT:
					// The curve value is directly set to its rate.
					envelopeHeight = rate;
					break;
			}
		}

		/**
		 * Return the next envelope height. Switch to the next curve if required.
		 * 
		 * @return     the next envelope height in the range [0..0x40000000]
		 */
		public int getNextEnvelopeHeight() {
			long currentEnvelopeHeight = envelopeHeight;

			switch (curveState) {
				case ATTACK_CURVE_STATE:
					stepCurve(envelope.AttackCurveType, envelope.AttackRate);
					// Switch Attack to Decay: when the envelope height gets over the upper limit
					//                         or under the lower limit
					if (envelopeHeight >= PSP_SAS_ENVELOPE_HEIGHT_MAX || envelopeHeight < 0) {
						setCurve(DECAY_CURVE_STATE);
					}
					break;
				case DECAY_CURVE_STATE:
					stepCurve(envelope.DecayCurveType, envelope.DecayRate);
					// Switch Decay to Sustain: when the envelope height gets over the upper limit
					//                          or under the sustain level
					if (envelopeHeight >= PSP_SAS_ENVELOPE_HEIGHT_MAX || envelopeHeight < envelope.SustainLevel) {
						setCurve(SUSTAIN_CURVE_STATE);
					}
					break;
				case SUSTAIN_CURVE_STATE:
					stepCurve(envelope.SustainCurveType, envelope.SustainRate);
					// Switch Sustain to Release: this switch only happens setting the key off.
					break;
				case RELEASE_CURVE_STATE:
					stepCurve(envelope.ReleaseCurveType, envelope.ReleaseRate);
					break;
			}

			if (tracing) {
				sceSasCore.log.trace(String.format("getNextEnvelopeHeight curve=%d, current=0x%08X, next=0x%08X", curveState, currentEnvelopeHeight, envelopeHeight));
			}

			return getIntEnvelopeHeight(currentEnvelopeHeight);
		}

		/**
		 * This method has to be called when setting a key off.
		 * It switches to the Release curve.
		 */
		public void setKeyOff() {
			// Switch to the release curve
			setCurve(RELEASE_CURVE_STATE);
		}

		/**
		 * A voice is ended when the envelope reaches 0 in the Sustain
		 * or Release curves.
		 * 
		 * @return    true    if the voice is ended (envelope reached 0
		 *                    in the Sustain or Release curves)
		 *            false   if the voice is not ended
		 */
		public boolean isEnded() {
			return curveState >= SUSTAIN_CURVE_STATE && envelopeHeight <= 0;
		}
	}

	public SampleSourceWithADSR(ISampleSource sampleSource, SoundVoice voice, VoiceADSREnvelope envelope) {
		this.sampleSource = sampleSource;
		this.voice = voice;
		envelopeState = new EnvelopeState(envelope);
		tracing = sceSasCore.log.isTraceEnabled();
	}

	/**
	 * Return the next sample value.
	 * The next sample of the sampleSource is modulated by the next
	 * ADSR envelope value.
	 * The voice is ended if the envelope reaches 0 in the SR curves. 
	 * The current ADSR envelope height is stored in the voice envelope structure.
	 */
	@Override
	public int getNextSample() {
		if (log.isTraceEnabled()) {
			log.trace(String.format("SampleSourceWithADSR.getNextSample height=0x%X, state=%d", envelopeState.envelopeHeight, envelopeState.curveState));
		}

		if (!voice.isOn()) {
			// The voice has been keyed Off, process the Release part of the wave
			envelopeState.setKeyOff();
		}

		if (envelopeState.isEnded()) {
			// The Release/Sustain has ended, stop playing the voice
			voice.setPlaying(false);
			return 0;
		}

		int envelopeHeight = envelopeState.getNextEnvelopeHeight();
		int sample = sampleSource.getNextSample();
		// envelopeHeight: [0..0x40000000]
		// sample: [-0x8000..0x7FFF]
		// Modulate the sample by the envelope value, assuming the envelope value
		// is ranging from 0.0f to 1.0f (0x40000000).
		//
		// First reduce the envelope value to a 16 bit value,
		// with rounding: [0..0x8000].
		int envelopeHeight16 = ((envelopeHeight >> 14) + 1) >> 1;
		// Multiply the sample by the envelope value with rounding
		short modulatedSampleLeft = modulate(getSampleLeft(sample), envelopeHeight16);
		short modulatedSampleRight = modulate(getSampleRight(sample), envelopeHeight16);
		int modulatedSample = SoundMixer.getSampleStereo(modulatedSampleLeft, modulatedSampleRight);

		if (tracing) {
			sceSasCore.log.trace(String.format("getNextSample voice=%d, sample=0x%08X, envelopeHeight=0x%08X, modulatedSample=0x%08X", voice.getIndex(), sample, envelopeHeight, modulatedSample));
		}

		// Store the current envelope height
		// (can be retrieved by the application using __sceSasGetEnvelopeHeight)
		voice.getEnvelope().height = envelopeHeight;

		return modulatedSample;
	}

	private short modulate(short sample, int envelopeHeight16) {
		return (short) ((sample * envelopeHeight16 + 0x4000) >> 15);
	}

	@Override
	public void resetToStart() {
		sampleSource.resetToStart();
		envelopeState.resetToStart();
	}

	@Override
	public boolean isEnded() {
		return sampleSource.isEnded();
	}
}
