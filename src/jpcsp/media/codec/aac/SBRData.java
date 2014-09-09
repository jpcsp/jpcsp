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

public class SBRData {
	public static final int SBR_SYNTHESIS_BUF_SIZE = (1280 - 128) * 2;

	// Main bitstream data variables
	int bsFrameClass;
	int bsAddHarmonicFlag;
	int bsNumEnv;
	int bsFreqRes[] = new int[7];
	int bsNumNoise;
	int bsDfEnv[] = new int[5];
	int bsDfNoise[] = new int[2];
	int bsInvfMode[][] = new int[2][5];
	int bsAddHarmonic[] = new int[48];
	int bsAmpRes;

	// State variables
	float synthesiesFilterbankSamples[] = new float[SBR_SYNTHESIS_BUF_SIZE];
	float analysisFilterbankSamples[] = new float[1312];
	int synthesisFilterbankSamplesOffset;
    ///l_APrev and l_A
    int eA[] = new int[2];
    ///Chirp factors
    float bwArray[] = new float[5];
    ///QMF values of the original signal
    float W[][][][] = new float[2][32][32][2];
    ///QMF output of the HF adjustor
    int Ypos;
    float Y[][][][] = new float[2][38][64][2];
    float gTemp[][] = new float[42][48];
    float qTemp[][] = new float[42][48];
    int sIndexmapped[][] = new int[8][48];
    ///Envelope scalefactors
    float envFacs[][] = new float[6][48];
    ///Noise scalefactors
    float noiseFacs[][] = new float[3][5];
    ///Envelope time borders
    int tEnv[] = new int[8];
    ///Envelope time border of the last envelope of the previous frame
    int tEnvNumEnvOld;
    ///Noise time borders
    int tQ[] = new int[3];
    int fIndexnoise;
    int fIndexsine;

    public void copy(SBRData that) {
    	bsFrameClass = that.bsFrameClass;
    	bsAddHarmonicFlag = that.bsAddHarmonicFlag;
    	bsNumEnv = that.bsNumEnv;
    	Utilities.copy(bsFreqRes, that.bsFreqRes);
    	bsNumNoise = that.bsNumNoise;
    	Utilities.copy(bsDfEnv, that.bsDfEnv);
    	Utilities.copy(bsDfNoise, that.bsDfNoise);
    	Utilities.copy(bsInvfMode, that.bsInvfMode);
    	Utilities.copy(bsAddHarmonic, that.bsAddHarmonic);
    	bsAmpRes = that.bsAmpRes;

    	// State variables
    	Utilities.copy(synthesiesFilterbankSamples, that.synthesiesFilterbankSamples);
    	Utilities.copy(analysisFilterbankSamples, that.analysisFilterbankSamples);
    	synthesisFilterbankSamplesOffset = that.synthesisFilterbankSamplesOffset;
    	Utilities.copy(eA, that.eA);
    	Utilities.copy(bwArray, that.bwArray);
    	Utilities.copy(W, that.W);
    	Utilities.copy(Y, that.Y);
    	Utilities.copy(gTemp, that.gTemp);
    	Utilities.copy(qTemp, that.qTemp);
    	Utilities.copy(sIndexmapped, that.sIndexmapped);
    	Utilities.copy(envFacs, that.envFacs);
    	Utilities.copy(noiseFacs, that.noiseFacs);
    	Utilities.copy(tEnv, that.tEnv);
        tEnvNumEnvOld = that.tEnvNumEnvOld;
    	Utilities.copy(tQ, that.tQ);
        fIndexnoise = that.fIndexnoise;
        fIndexsine = that.fIndexsine;
    }
}
