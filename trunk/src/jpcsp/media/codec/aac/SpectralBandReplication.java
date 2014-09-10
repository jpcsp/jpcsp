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

import jpcsp.media.codec.util.FFT;
import jpcsp.util.Utilities;

/**
 * Spectral Band Replication
 */
public class SpectralBandReplication {
	int sampleRate;
	int start;
	boolean reset;
	SpectrumParameters spectrumParams = new SpectrumParameters();
	int bsAmpResHeader;
	int bsLimiterBands;
	int bsLimiterGains;
	int bsInterpolFreq;
	int bsSmoothingMode;
	int bsCoupling;
	int k[] = new int[5]; ///< k0, k1, k2
	///kx', and kx respectively, kx is the first QMF subband where SBR is used.
	///kx' is its value from the previous frame
	int kx[] = new int[2];
	///M' and M respectively, M is the number of QMF subbands that use SBR.
	int m[] = new int[2];
	boolean kxAndMPushed;
	///The number of frequency bands in f_master
	int nMaster;
	SBRData data[] = new SBRData[2];
	PSContext ps;
	///N_Low and N_High respectively, the number of frequency bands for low and high resolution
	int n[] = new int[2];
	///Number of noise floor bands
	int nQ;
	///Number of limiter bands
	int nLim;
    ///The master QMF frequency grouping
    int fMaster[] = new int[49];
    ///Frequency borders for low resolution SBR
    int fTablelow[] = new int[25];
    ///Frequency borders for high resolution SBR
    int fTablehigh[] = new int[49];
    ///Frequency borders for noise floors
    int fTablenoise[] = new int[6];
    ///Frequency borders for the limiter
    int fTablelim[] = new int[30];
    int numPatches;
    int patchNumSubbands[] = new int[6];
    int patchStartSubband[] = new int[6];
    ///QMF low frequency input to the HF generator
    float Xlow[][][] = new float[32][40][2];
    ///QMF output of the HF generator
    float Xhigh[][][] = new float[64][40][2];
    ///QMF values of the reconstructed signal
    float X[][][][] = new float[2][2][38][64];
    ///Zeroth coefficient used to filter the subband signals
    float alpha0[][] = new float[64][2];
    ///First coefficient used to filter the subband signals
    float alpha1[][] = new float[64][2];
    ///Dequantized envelope scalefactors, remapped
    float eOrigmapped[][] = new float[7][48];
    ///Dequantized noise scalefactors, remapped
    float qMapped[][] = new float[7][48];
    ///Sinusoidal presence, remapped
    int sMapped[][] = new int[7][48];
    ///Estimated envelope
    float eCurr[][] = new float[7][48];
    ///Amplitude adjusted noise scalefactors
    float qM[][] = new float[7][48];
    ///Sinusoidal levels
    float sM[][] = new float[7][48];
    float gain[][] = new float [7][48];
    float qmfFilterScratch[][] = new float[5][64];
    FFT mdctAna;
    FFT mdct;

    public void copy(SpectralBandReplication that) {
    	sampleRate = that.sampleRate;
    	start = that.start;
    	reset = that.reset;
    	spectrumParams.copy(that.spectrumParams);
    	bsAmpResHeader = that.bsAmpResHeader;
    	bsLimiterBands = that.bsLimiterBands;
    	bsLimiterGains = that.bsLimiterGains;
    	bsInterpolFreq = that.bsInterpolFreq;
    	bsSmoothingMode = that.bsSmoothingMode;
    	bsCoupling = that.bsCoupling;
    	Utilities.copy(k, that.k);
    	Utilities.copy(kx, that.kx);
    	Utilities.copy(m, that.m);
    	kxAndMPushed = that.kxAndMPushed;
    	nMaster = that.nMaster;
    	for (int i = 0; i < data.length; i++) {
    		data[i].copy(that.data[i]);
    	}
    	ps.copy(that.ps);
    	Utilities.copy(n, that.n);
    	nQ = that.nQ;
    	nLim = that.nLim;
    	Utilities.copy(fMaster, that.fMaster);
    	Utilities.copy(fTablelow, that.fTablelow);
    	Utilities.copy(fTablehigh, that.fTablehigh);
    	Utilities.copy(fTablenoise, that.fTablenoise);
    	Utilities.copy(fTablelim, that.fTablelim);
        numPatches = that.numPatches;
    	Utilities.copy(patchNumSubbands, that.patchNumSubbands);
    	Utilities.copy(patchStartSubband, that.patchStartSubband);
    	Utilities.copy(Xlow, that.Xlow);
    	Utilities.copy(Xhigh, that.Xhigh);
    	Utilities.copy(X, that.X);
    	Utilities.copy(alpha0, that.alpha0);
    	Utilities.copy(alpha1, that.alpha1);
    	Utilities.copy(eOrigmapped, that.eOrigmapped);
    	Utilities.copy(qMapped, that.qMapped);
    	Utilities.copy(sMapped, that.sMapped);
    	Utilities.copy(eCurr, that.eCurr);
    	Utilities.copy(qM, that.qM);
    	Utilities.copy(sM, that.sM);
    	Utilities.copy(gain, that.gain);
    	Utilities.copy(qmfFilterScratch, that.qmfFilterScratch);
        mdctAna.copy(that.mdctAna);
        mdct.copy(that.mdct);
    }
}
