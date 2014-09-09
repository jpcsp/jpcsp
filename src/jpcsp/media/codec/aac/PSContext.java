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

public class PSContext {
	public static final int PS_MAX_NUM_ENV = 5;
	public static final int PS_MAX_NR_IIDICC = 34;
	public static final int PS_MAX_NR_IPDOPD = 17;
	public static final int PS_MAX_SSB = 91;
	public static final int PS_MAX_AP_BANDS = 50;
	public static final int PS_QMF_TIME_SLOTS = 32;
	public static final int PS_MAX_DELAY = 14;
	public static final int PS_AP_LINKS = 3;
	public static final int PS_MAX_AP_DELAY = 5;

	public int start;
	int enableIid;
	int iidQuant;
	int nrIidPar;
	int nrIpdopdPar;
	int enableIcc;
	int iccMode;
	int nrIccPar;
	int enableExt;
	int frameClass;
	int numEnvOld;
	int numEnv;
	int enableIpdopd;
	int borderPosition[] = new int[PS_MAX_NUM_ENV + 1];
	int iidPar[][] = new int[PS_MAX_NUM_ENV][PS_MAX_NR_IIDICC]; ///< Inter-channel Intensity Difference Parameters
	int iccPar[][] = new int[PS_MAX_NUM_ENV][PS_MAX_NR_IIDICC]; ///< Inter-Channel Coherence Parameters
	// ipd/opd is iid/icc sized so that the same functions can handle both
	int ipdPar[][] = new int[PS_MAX_NUM_ENV][PS_MAX_NR_IIDICC]; ///< Inter-channel Phase Difference Parameters
	int opdPar[][] = new int[PS_MAX_NUM_ENV][PS_MAX_NR_IIDICC]; ///< Overall Phase Difference Parameters
	int is34bands;
	int is34bandsOld;

	float inBuf[][][] = new float[5][44][2];
	float delay[][][] = new float[PS_MAX_SSB][PS_QMF_TIME_SLOTS + PS_MAX_DELAY][2];
	float apDelay[][][][] = new float[PS_MAX_AP_BANDS][PS_AP_LINKS][PS_QMF_TIME_SLOTS + PS_MAX_AP_DELAY][2];
	float peakDecayNrg[] = new float[34];
	float powerSmooth[] = new float[34];
	float peakDecayDiffSmooth[] = new float[34];
	float H11[][][] = new float [2][PS_MAX_NUM_ENV+1][PS_MAX_NR_IIDICC];
	float H12[][][] = new float [2][PS_MAX_NUM_ENV+1][PS_MAX_NR_IIDICC];
	float H21[][][] = new float [2][PS_MAX_NUM_ENV+1][PS_MAX_NR_IIDICC];
	float H22[][][] = new float [2][PS_MAX_NUM_ENV+1][PS_MAX_NR_IIDICC];
	int opdHist[] = new int[PS_MAX_NR_IIDICC];
	int ipdHist[] = new int[PS_MAX_NR_IIDICC];

	public void copy(PSContext that) {
		start = that.start;
		enableIid = that.enableIid;
		iidQuant = that.iidQuant;
		nrIidPar = that.nrIidPar;
		nrIpdopdPar = that.nrIpdopdPar;
		enableIcc = that.enableIcc;
		iccMode = that.iccMode;
		nrIccPar = that.nrIccPar;
		enableExt = that.enableExt;
		frameClass = that.frameClass;
		numEnvOld = that.numEnvOld;
		numEnv = that.numEnv;
		enableIpdopd = that.enableIpdopd;
		Utilities.copy(borderPosition, that.borderPosition);
		Utilities.copy(iidPar, that.iidPar);
		Utilities.copy(iccPar, that.iccPar);
		Utilities.copy(ipdPar, that.ipdPar);
		Utilities.copy(opdPar, that.opdPar);
		is34bands = that.is34bands;
		is34bandsOld = that.is34bandsOld;

		Utilities.copy(inBuf, that.inBuf);
		Utilities.copy(delay, that.delay);
		Utilities.copy(apDelay, that.apDelay);
		Utilities.copy(peakDecayNrg, that.peakDecayNrg);
		Utilities.copy(powerSmooth, that.powerSmooth);
		Utilities.copy(peakDecayDiffSmooth, that.peakDecayDiffSmooth);
		Utilities.copy(H11, that.H11);
		Utilities.copy(H12, that.H12);
		Utilities.copy(H21, that.H21);
		Utilities.copy(H22, that.H22);
		Utilities.copy(opdHist, that.opdHist);
		Utilities.copy(ipdHist, that.ipdHist);
	}
}
