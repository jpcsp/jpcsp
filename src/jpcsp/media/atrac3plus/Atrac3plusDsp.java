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
package jpcsp.media.atrac3plus;

import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static jpcsp.media.atrac3plus.Atrac3plusDecoder.ATRAC3P_FRAME_SAMPLES;
import static jpcsp.media.atrac3plus.Atrac3plusDecoder.ATRAC3P_POWER_COMP_OFF;
import static jpcsp.media.atrac3plus.Atrac3plusDecoder.ATRAC3P_SUBBANDS;
import static jpcsp.media.atrac3plus.Atrac3plusDecoder.ATRAC3P_SUBBAND_SAMPLES;
import static jpcsp.media.atrac3plus.Atrac3plusDecoder.CH_UNIT_STEREO;
import static jpcsp.media.atrac3plus.FloatDSP.vectorFmul;
import static jpcsp.media.atrac3plus.FloatDSP.vectorFmulReverse;
import static jpcsp.media.atrac3plus.SineWin.ff_sine_128;
import static jpcsp.media.atrac3plus.SineWin.ff_sine_64;

import java.util.Arrays;

import jpcsp.media.atrac3plus.ChannelUnitContext.IPQFChannelContext;

/*
 * Based on the FFmpeg version from Maxim Poliakovski.
 * All credits go to him.
 */
public class Atrac3plusDsp {
	private static final int ATRAC3P_MDCT_SIZE = ATRAC3P_SUBBAND_SAMPLES * 2;
	private static final float[] sine_table = new float[2048]; ///< wave table
	private static final float[] hann_window = new float[256]; ///< Hann windowing function
	private static final float[] amp_sf_tab = new float[64];   ///< scalefactors for quantized amplitudes
	private static final double TWOPI = 2 * Math.PI;

	/**
	 *  Map quant unit number to its position in the spectrum.
	 *  To get the number of spectral lines in each quant unit do the following:
	 *  num_specs = qu_to_spec_pos[i+1] - qu_to_spec_pos[i]
	 */
	static final public int ff_atrac3p_qu_to_spec_pos[] = new int[] {
	      0,    16,   32,   48,   64,   80,   96,  112,
	    128,   160,  192,  224,  256,  288,  320,  352,
	    384,   448,  512,  576,  640,  704,  768,  896,
	    1024, 1152, 1280, 1408, 1536, 1664, 1792, 1920,
	    2048
	};

	/* Scalefactors table. */
	/* Approx. Equ: pow(2.0, (i - 16.0 + 0.501783948) / 3.0) */
	static final public float ff_atrac3p_sf_tab[] = new float[] {
	    0.027852058f,  0.0350914f, 0.044212341f, 0.055704117f,  0.0701828f,
	    0.088424683f, 0.11140823f,   0.1403656f,  0.17684937f, 0.22281647f, 0.2807312f, 0.35369873f,
	    0.44563293f,   0.5614624f,  0.70739746f,  0.89126587f,  1.1229248f, 1.4147949f,  1.7825317f,
	    2.2458496f,    2.8295898f,   3.5650635f,   4.4916992f,  5.6591797f,  7.130127f,  8.9833984f,
	    11.318359f,    14.260254f,   17.966797f,   22.636719f,  28.520508f, 35.933594f,  45.273438f,
	    57.041016f,    71.867188f,   90.546875f,   114.08203f,  143.73438f, 181.09375f,  228.16406f,
	    287.46875f,     362.1875f,   456.32812f,    574.9375f,    724.375f, 912.65625f,   1149.875f,
	    1448.75f,      1825.3125f,     2299.75f,      2897.5f,   3650.625f,    4599.5f,     5795.0f,
	    7301.25f,         9199.0f,     11590.0f,     14602.5f,    18398.0f,   23180.0f,    29205.0f,
	    36796.0f,        46360.0f,     58410.0f
	};

	/* Mantissa table. */
	/* pow(10, x * log10(2) + 0.05) / 2 / ([1,2,3,5,7,15,31] + 0.5) */
	static final public float ff_atrac3p_mant_tab[] = new float[] {
	    0.0f,
	    0.74801636f,
	    0.44882202f,
	    0.32058716f,
	    0.20400238f,
	    0.1496048f,
	    0.07239151f,
	    0.035619736f
	};

	static final private int subband_to_powgrp[] = new int[] {
	    0, 1, 1, 2, 2, 2, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4
	};

	/* noise table for power compensation */
	static final private float noise_tab[] = new float[] {
	    -0.01358032f,  -0.05593872f,   0.01696777f,  -0.14871216f,  -0.26412964f,  -0.09893799f,   0.25723267f,
	     0.02008057f,  -0.72235107f,  -0.44351196f,  -0.22985840f,   0.16833496f,   0.46902466f,   0.05917358f,
	    -0.15179443f,   0.41299438f,  -0.01287842f,   0.13360596f,   0.43557739f,  -0.09530640f,  -0.58422852f,
	     0.39266968f,  -0.08343506f,  -0.25604248f,   0.22848511f,   0.26013184f,  -0.65588379f,   0.17288208f,
	    -0.08673096f,  -0.05203247f,   0.07299805f,  -0.28665161f,  -0.35806274f,   0.06552124f,  -0.09387207f,
	     0.21099854f,  -0.28347778f,  -0.72402954f,   0.05050659f,  -0.10635376f,  -0.18853760f,   0.29724121f,
	     0.20703125f,  -0.29791260f,  -0.37634277f,   0.47970581f,  -0.09976196f,   0.32641602f,  -0.29248047f,
	    -0.28237915f,   0.26028442f,  -0.36157227f,   0.22042847f,  -0.03222656f,  -0.37268066f,  -0.03759766f,
	     0.09909058f,   0.23284912f,   0.19320679f,   0.14453125f,  -0.02139282f,  -0.19702148f,   0.31533813f,
	    -0.16741943f,   0.35031128f,  -0.35656738f,  -0.66128540f,  -0.00701904f,   0.20898438f,   0.26837158f,
	    -0.33706665f,  -0.04568481f,   0.12600708f,   0.10284424f,   0.07321167f,  -0.18280029f,   0.38101196f,
	     0.21301270f,   0.04541016f,   0.01156616f,  -0.26391602f,  -0.02346802f,  -0.22125244f,   0.29760742f,
	    -0.36233521f,  -0.31314087f,  -0.13967896f,  -0.11276245f,  -0.19433594f,   0.34490967f,   0.02343750f,
	     0.21963501f,  -0.02777100f,  -0.67678833f,  -0.08999634f,   0.14233398f,  -0.27697754f,   0.51422119f,
	    -0.05047607f,   0.48327637f,   0.37167358f,  -0.60806274f,   0.18728638f,  -0.15191650f,   0.00637817f,
	     0.02832031f,  -0.15618896f,   0.60644531f,   0.21826172f,   0.06384277f,  -0.31863403f,   0.08816528f,
	     0.15447998f,  -0.07015991f,  -0.08154297f,  -0.40966797f,  -0.39785767f,  -0.11709595f,   0.22052002f,
	     0.18466187f,  -0.17257690f,   0.03759766f,  -0.06195068f,   0.00433350f,   0.12176514f,   0.34011841f,
	     0.25610352f,  -0.05294800f,   0.41033936f,   0.16854858f,  -0.76187134f,   0.13845825f,  -0.19418335f,
	    -0.21524048f,  -0.44412231f,  -0.08160400f,  -0.28195190f,  -0.01873779f,   0.15524292f,  -0.37438965f,
	    -0.44860840f,   0.43096924f,  -0.24746704f,   0.49856567f,   0.14859009f,   0.38159180f,   0.20541382f,
	    -0.39175415f,  -0.65850830f,  -0.43716431f,   0.13037109f,  -0.05111694f,   0.39956665f,   0.21447754f,
	    -0.04861450f,   0.33654785f,   0.10589600f,  -0.88085938f,  -0.30822754f,   0.38577271f,   0.30047607f,
	     0.38836670f,   0.09118652f,  -0.36477661f,  -0.01641846f,  -0.23031616f,   0.26058960f,   0.18859863f,
	    -0.21868896f,  -0.17861938f,  -0.29754639f,   0.09777832f,   0.10806274f,  -0.51605225f,   0.00076294f,
	     0.13259888f,   0.11090088f,  -0.24084473f,   0.24957275f,   0.01379395f,  -0.04141235f,  -0.04937744f,
	     0.57394409f,   0.27410889f,   0.27587891f,   0.45013428f,  -0.32592773f,   0.11160278f,  -0.00970459f,
	     0.29092407f,   0.03356934f,  -0.70925903f,   0.04882812f,   0.43499756f,   0.07720947f,  -0.27554321f,
	    -0.01742554f,  -0.08413696f,  -0.04028320f,  -0.52850342f,  -0.07330322f,   0.05181885f,   0.21362305f,
	    -0.18765259f,   0.07058716f,  -0.03009033f,   0.32662964f,   0.27023315f,  -0.28002930f,   0.17568970f,
	     0.03338623f,   0.30242920f,  -0.03921509f,   0.32174683f,  -0.23733521f,   0.08575439f,  -0.38269043f,
	     0.09194946f,  -0.07238770f,   0.17941284f,  -0.51278687f,  -0.25146484f,   0.19790649f,  -0.19195557f,
	     0.16549683f,   0.42456055f,   0.39129639f,  -0.02868652f,   0.17980957f,   0.24902344f,  -0.76583862f,
	    -0.20959473f,   0.61013794f,   0.37011719f,   0.36859131f,  -0.04486084f,   0.10678101f,  -0.15994263f,
	    -0.05328369f,   0.28463745f,  -0.06420898f,  -0.36987305f,  -0.28009033f,  -0.11764526f,   0.04312134f,
	    -0.08038330f,   0.04885864f,  -0.03067017f,  -0.00042725f,   0.34289551f,  -0.00988770f,   0.34838867f,
	     0.32516479f,  -0.16271973f,   0.38269043f,   0.03240967f,   0.12417603f,  -0.14331055f,  -0.34902954f,
	    -0.18325806f,   0.29421997f,   0.44284058f,   0.75170898f,  -0.67245483f,  -0.12176514f,   0.27914429f,
	    -0.29806519f,   0.19863892f,   0.30087280f,   0.22680664f,  -0.36633301f,  -0.32534790f,  -0.57553101f,
	    -0.16641235f,   0.43811035f,   0.08331299f,   0.15942383f,   0.26516724f,  -0.24240112f,  -0.11761475f,
	    -0.16827393f,  -0.14260864f,   0.46343994f,   0.11804199f,  -0.55514526f,  -0.02520752f,  -0.14309692f,
	     0.00448608f,   0.02749634f,  -0.30545044f,   0.70965576f,   0.45108032f,   0.66439819f,  -0.68255615f,
	    -0.12496948f,   0.09146118f,  -0.21109009f,  -0.23791504f,   0.79943848f,  -0.35205078f,  -0.24963379f,
	     0.18719482f,  -0.19079590f,   0.07458496f,   0.07623291f,  -0.28781128f,  -0.37121582f,  -0.19580078f,
	    -0.01773071f,  -0.16717529f,   0.13040161f,   0.14672852f,   0.42379761f,   0.03582764f,   0.11431885f,
	     0.05145264f,   0.44702148f,   0.08963013f,   0.01367188f,  -0.54519653f,  -0.12692261f,   0.21176147f,
	     0.04925537f,   0.30670166f,  -0.11029053f,   0.19555664f,  -0.27740479f,   0.23043823f,   0.15554810f,
	    -0.19299316f,  -0.25729370f,   0.17800903f,  -0.03579712f,  -0.05065918f,  -0.06933594f,  -0.09500122f,
	    -0.07821655f,   0.23889160f,  -0.31900024f,   0.03073120f,  -0.00415039f,   0.61315918f,   0.37176514f,
	    -0.13442993f,  -0.15536499f,  -0.19216919f,  -0.37899780f,   0.19992065f,   0.02630615f,  -0.12573242f,
	     0.25927734f,  -0.02447510f,   0.29629517f,  -0.40731812f,  -0.17333984f,   0.24310303f,  -0.10607910f,
	     0.14828491f,   0.08792114f,  -0.18743896f,  -0.05572510f,  -0.04833984f,   0.10473633f,  -0.29028320f,
	    -0.67687988f,  -0.28170776f,  -0.41687012f,   0.05413818f,  -0.23284912f,   0.09555054f,  -0.08969116f,
	    -0.15112305f,   0.12738037f,   0.35986328f,   0.28948975f,   0.30691528f,   0.23956299f,   0.06973267f,
	    -0.31198120f,  -0.18450928f,   0.22280884f,  -0.21600342f,   0.23522949f,  -0.61840820f,  -0.13012695f,
	     0.26412964f,   0.47320557f,  -0.26440430f,   0.38757324f,   0.17352295f,  -0.26104736f,  -0.25866699f,
	    -0.12274170f,  -0.29733276f,   0.07687378f,   0.18588257f,  -0.08880615f,   0.31185913f,   0.05313110f,
	    -0.10885620f,  -0.14901733f,  -0.22323608f,  -0.08538818f,   0.19812012f,   0.19732666f,  -0.18927002f,
	     0.29058838f,   0.25555420f,  -0.48599243f,   0.18768311f,   0.01345825f,   0.34887695f,   0.21530151f,
	     0.19857788f,   0.18661499f,  -0.01394653f,  -0.09063721f,  -0.38781738f,   0.27160645f,  -0.20379639f,
	    -0.32119751f,  -0.23889160f,   0.27096558f,   0.24951172f,   0.07922363f,   0.07479858f,  -0.50946045f,
	     0.10220337f,   0.58364868f,  -0.19503784f,  -0.18560791f,  -0.01165771f,   0.47195435f,   0.22430420f,
	    -0.38635254f,  -0.03732300f,  -0.09179688f,   0.06991577f,   0.15106201f,   0.20605469f,  -0.05969238f,
	    -0.41821289f,   0.12231445f,  -0.04672241f,  -0.05117798f,  -0.11523438f,  -0.51849365f,  -0.04077148f,
	     0.44284058f,  -0.64086914f,   0.17019653f,   0.02236938f,   0.22848511f,  -0.23214722f,  -0.32354736f,
	    -0.14068604f,  -0.29690552f,  -0.19891357f,   0.02774048f,  -0.20965576f,  -0.52191162f,  -0.19299316f,
	    -0.07290649f,   0.49053955f,  -0.22302246f,   0.05642700f,   0.13122559f,  -0.20819092f,  -0.83590698f,
	    -0.08181763f,   0.26797485f,  -0.00091553f,  -0.09457397f,   0.17089844f,  -0.27020264f,   0.30270386f,
	     0.05496216f,   0.09564209f,  -0.08590698f,   0.02130127f,   0.35931396f,   0.21728516f,  -0.15396118f,
	    -0.05053711f,   0.02719116f,   0.16302490f,   0.43212891f,   0.10229492f,  -0.40820312f,   0.21646118f,
	     0.08435059f,  -0.11145020f,  -0.39962769f,  -0.05618286f,  -0.10223389f,  -0.60839844f,   0.33724976f,
	    -0.06341553f,  -0.47369385f,  -0.32852173f,   0.05242920f,   0.19635010f,  -0.19137573f,  -0.67901611f,
	     0.16180420f,   0.05133057f,  -0.22283936f,   0.09646606f,   0.24288940f,  -0.45007324f,   0.08804321f,
	     0.14053345f,   0.22619629f,  -0.01000977f,   0.36355591f,  -0.19863892f,  -0.30364990f,  -0.24118042f,
	    -0.57461548f,   0.26498413f,   0.04345703f,  -0.09796143f,  -0.47714233f,  -0.23739624f,   0.18737793f,
	     0.08926392f,  -0.02795410f,   0.00305176f,  -0.08700562f,  -0.38711548f,   0.03222656f,   0.10940552f,
	    -0.41906738f,  -0.01620483f,  -0.47061157f,   0.37985229f,  -0.21624756f,   0.47976685f,  -0.20046997f,
	    -0.62533569f,  -0.26907349f,  -0.02877808f,   0.00671387f,  -0.29071045f,  -0.24685669f,  -0.15722656f,
	    -0.26055908f,   0.29968262f,   0.28225708f,  -0.08990479f,  -0.16748047f,  -0.46759033f,  -0.25067139f,
	    -0.25183105f,  -0.45932007f,   0.05828857f,   0.29006958f,   0.23840332f,  -0.17974854f,   0.26931763f,
	     0.10696411f,  -0.06848145f,  -0.17126465f,  -0.10522461f,  -0.55386353f,  -0.42306519f,  -0.07608032f,
	     0.24380493f,   0.38586426f,   0.16882324f,   0.26751709f,   0.17303467f,   0.35809326f,  -0.22094727f,
	    -0.30703735f,  -0.28497314f,  -0.04321289f,   0.15219116f,  -0.17071533f,  -0.39334106f,   0.03439331f,
	    -0.10809326f,  -0.30590820f,   0.26449585f,  -0.07412720f,   0.13638306f,  -0.01062012f,   0.27996826f,
	     0.04397583f,  -0.05557251f,  -0.56933594f,   0.03363037f,  -0.00949097f,   0.52642822f,  -0.44329834f,
	     0.28308105f,  -0.05499268f,  -0.23312378f,  -0.29870605f,  -0.05123901f,   0.26831055f,  -0.35238647f,
	    -0.30993652f,   0.34646606f,  -0.19775391f,   0.44595337f,   0.13769531f,   0.45358276f,   0.19961548f,
	     0.42681885f,   0.15722656f,   0.00128174f,   0.23757935f,   0.40988159f,   0.25164795f,  -0.00732422f,
	    -0.12405396f,  -0.43420410f,  -0.00402832f,   0.34243774f,   0.36264038f,   0.18807983f,  -0.09301758f,
	    -0.10296631f,   0.05532837f,  -0.31652832f,   0.14337158f,   0.35040283f,   0.32540894f,   0.05728149f,
	    -0.12030029f,  -0.25942993f,  -0.20312500f,  -0.16491699f,  -0.46051025f,  -0.08004761f,   0.50772095f,
	     0.16168213f,   0.28439331f,   0.08105469f,  -0.19104004f,   0.38589478f,  -0.16400146f,  -0.25454712f,
	     0.20281982f,  -0.20730591f,  -0.06311035f,   0.32937622f,   0.15032959f,  -0.05340576f,   0.30487061f,
	    -0.11648560f,   0.38009644f,  -0.20062256f,   0.43466187f,   0.01150513f,   0.35754395f,  -0.13146973f,
	     0.67489624f,   0.05212402f,   0.27914429f,  -0.39431763f,   0.75308228f,  -0.13366699f,   0.24453735f,
	     0.42248535f,  -0.65905762f,  -0.00546265f,  -0.03491211f,  -0.13659668f,  -0.08294678f,  -0.45666504f,
	     0.27188110f,   0.12731934f,   0.61148071f,   0.10449219f,  -0.28836060f,   0.00091553f,   0.24618530f,
	     0.13119507f,   0.05685425f,   0.17355347f,   0.42034912f,   0.08514404f,   0.24536133f,   0.18951416f,
	    -0.19107056f,  -0.15036011f,   0.02334595f,   0.54986572f,   0.32321167f,  -0.16104126f,  -0.03054810f,
	     0.43594360f,   0.17309570f,   0.61053467f,   0.24731445f,   0.33334351f,   0.15240479f,   0.15588379f,
	     0.36425781f,  -0.30407715f,  -0.13302612f,   0.00427246f,   0.04171753f,  -0.33178711f,   0.34216309f,
	    -0.12463379f,  -0.02764893f,   0.05905151f,  -0.31436157f,   0.16531372f,   0.34542847f,  -0.03292847f,
	     0.12527466f,  -0.12313843f,  -0.13171387f,   0.04757690f,  -0.45095825f,  -0.19085693f,   0.35342407f,
	    -0.23239136f,  -0.34387207f,   0.11264038f,  -0.15740967f,   0.05273438f,   0.74942017f,   0.21505737f,
	     0.08514404f,  -0.42391968f,  -0.19531250f,   0.35293579f,   0.25305176f,   0.15731812f,  -0.70324707f,
	    -0.21591187f,   0.35604858f,   0.14132690f,   0.11724854f,   0.15853882f,  -0.24597168f,   0.07019043f,
	     0.02127075f,   0.12658691f,   0.06390381f,  -0.12292480f,   0.15441895f,  -0.47640991f,   0.06195068f,
	     0.58981323f,  -0.15151978f,  -0.03604126f,  -0.45059204f,  -0.01672363f,  -0.46997070f,   0.25750732f,
	     0.18084717f,   0.06661987f,   0.13253784f,   0.67828369f,   0.11370850f,   0.11325073f,  -0.04611206f,
	    -0.07791138f,  -0.36544800f,  -0.06747437f,  -0.31594849f,   0.16131592f,   0.41983032f,   0.11071777f,
	    -0.36889648f,   0.30963135f,  -0.37875366f,   0.58508301f,   0.00393677f,   0.12338257f,   0.03424072f,
	    -0.21728516f,  -0.12838745f,  -0.46981812f,   0.05868530f,  -0.25015259f,   0.27407837f,   0.65240479f,
	    -0.34429932f,  -0.15179443f,   0.14056396f,   0.33505249f,   0.28826904f,   0.09921265f,   0.34390259f,
	     0.13656616f,  -0.23608398f,   0.00863647f,   0.02627563f,  -0.19119263f,   0.19775391f,  -0.07214355f,
	     0.07809448f,   0.03454590f,  -0.03417969f,   0.00033569f,  -0.23095703f,   0.18673706f,   0.05798340f,
	     0.03814697f,  -0.04318237f,   0.05487061f,   0.08633423f,   0.55950928f,  -0.06347656f,   0.10333252f,
	     0.25305176f,   0.05853271f,   0.12246704f,  -0.25543213f,  -0.34262085f,  -0.36437988f,  -0.21304321f,
	    -0.05093384f,   0.02777100f,   0.07620239f,  -0.21215820f,  -0.09326172f,   0.19021606f,  -0.40579224f,
	    -0.01193237f,   0.19845581f,  -0.35336304f,  -0.07397461f,   0.20104980f,   0.08615112f,  -0.44375610f,
	     0.11419678f,   0.24453735f,  -0.16555786f,  -0.05081177f,  -0.01406860f,   0.27893066f,  -0.18692017f,
	     0.07473755f,   0.03451538f,  -0.39733887f,   0.21548462f,  -0.22534180f,  -0.39651489f,  -0.04989624f,
	    -0.57662964f,   0.06390381f,   0.62020874f,  -0.13470459f,   0.04345703f,  -0.21862793f,  -0.02789307f,
	     0.51696777f,  -0.27587891f,   0.39004517f,   0.09857178f,  -0.00738525f,   0.31317139f,   0.00048828f,
	    -0.46572876f,   0.29531860f,  -0.10009766f,  -0.27856445f,   0.03594971f,   0.25048828f,  -0.74584961f,
	    -0.25350952f,  -0.03302002f,   0.31188965f,   0.01571655f,   0.46710205f,   0.21591187f,   0.07260132f,
	    -0.42132568f,  -0.53900146f,  -0.13674927f,  -0.16571045f,  -0.34454346f,   0.12359619f,  -0.11184692f,
	     0.00967407f,   0.34576416f,  -0.05761719f,   0.34848022f,   0.17645264f,  -0.39395142f,   0.10339355f,
	     0.18215942f,   0.20697021f,   0.59109497f,  -0.11560059f,  -0.07385254f,   0.10397339f,   0.35437012f,
	    -0.22863770f,   0.01794434f,   0.17559814f,  -0.17495728f,   0.12142944f,   0.10928345f,  -1.00000000f,
	    -0.01379395f,   0.21237183f,  -0.27035522f,   0.27319336f,  -0.37066650f,   0.41354370f,  -0.40054321f,
	     0.00689697f,   0.26321411f,   0.39266968f,   0.65298462f,   0.41625977f,  -0.13909912f,   0.78375244f,
	    -0.30941772f,   0.20169067f,  -0.39367676f,   0.94021606f,  -0.24066162f,   0.05557251f,  -0.24533081f,
	    -0.05444336f,  -0.76754761f,  -0.19375610f,  -0.11041260f,  -0.17532349f,   0.16006470f,   0.02188110f,
	     0.17465210f,  -0.04342651f,  -0.56777954f,  -0.40988159f,   0.26687622f,   0.11700439f,  -0.00344849f,
	    -0.05395508f,   0.37426758f,  -0.40719604f,  -0.15032959f,  -0.01660156f,   0.04196167f,  -0.04559326f,
	    -0.12969971f,   0.12011719f,   0.08419800f,  -0.11199951f,   0.35174561f,   0.10275269f,  -0.25686646f,
	     0.48446655f,   0.03225708f,   0.28408813f,  -0.18701172f,   0.36282349f,  -0.03280640f,   0.32302856f,
	     0.17233276f,   0.48269653f,   0.31112671f,  -0.04946899f,   0.12774658f,   0.52685547f,   0.10211182f,
	     0.05953979f,   0.05999756f,   0.20144653f,   0.00744629f,   0.27316284f,   0.24377441f,   0.39672852f,
	     0.01702881f,  -0.35513306f,   0.11364746f,  -0.13555908f,   0.48880005f,  -0.15417480f,  -0.09149170f,
	    -0.02615356f,   0.46246338f,  -0.72250366f,   0.22332764f,   0.23849487f,  -0.25686646f,  -0.08514404f,
	    -0.02062988f,  -0.34494019f,  -0.02297974f,  -0.80386353f,  -0.08074951f,  -0.12689209f,  -0.06896973f,
	     0.24099731f,  -0.35650635f,  -0.09558105f,   0.29254150f,   0.23132324f,  -0.16726685f,   0.00000000f,
	    -0.24237061f,   0.30899048f,   0.29504395f,  -0.20898438f,   0.17059326f,  -0.07672119f,  -0.14395142f,
	     0.05572510f,   0.20602417f,  -0.51550293f,  -0.03167725f,  -0.48840332f,  -0.20425415f,   0.14144897f,
	     0.07275391f,  -0.76669312f,  -0.22488403f,   0.20651245f,   0.03259277f,   0.00085449f,   0.03039551f,
	     0.47555542f,   0.38351440f
	};

	/** Noise level table for power compensation.
	 *  Equ: pow(2.0f, (double)(6 - i) / 3.0f) where i = 0...15 */
	static final private float pwc_levs[] = new float[] {
	    3.96875f, 3.15625f,     2.5f,    2.0f, 1.59375f,   1.25f,     1.0f, 0.78125f,
	    0.625f,       0.5f, 0.40625f, 0.3125f,    0.25f, 0.1875f, 0.15625f, 0.0f
	};

	/** Map subband number to quant unit number. */
	static final private int subband_to_qu[] = new int[] {
	    0, 8, 12, 16, 18, 20, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
	};

	/* lookup table for fast modulo 23 op required for cyclic buffers of the IPQF */
	static final int mod23_lut[] = new int[] {
	    23,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11,
	    12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 0
	};

	// First half of the 384-tap IPQF filtering coefficients.
	static final float ipqf_coeffs1[][] = new float[][] {
	    { -5.8336207e-7f,    -8.0604229e-7f,    -4.2005411e-7f,    -4.4400572e-8f,
	       3.226247e-8f,      3.530856e-8f,      1.2660377e-8f,     0.000010516783f,
	      -0.000011838618f,   6.005389e-7f,      0.0000014333754f,  0.0000023108685f,
	       0.0000032569742f,  0.0000046192422f,  0.0000063894258f,  0.0000070302972f },
	    { -0.0000091622824f, -0.000010502935f,  -0.0000079212787f, -0.0000041712024f,
	      -0.0000026336629f, -0.0000015432918f, -5.7168614e-7f,     0.0000018111954f,
	       0.000023530851f,   0.00002780562f,    0.000032302323f,   0.000036968919f,
	       0.000041575615f,   0.000045337845f,   0.000046043948f,   0.000048585582f },
	    { -0.000064464548f,  -0.000068306952f,  -0.000073081472f,  -0.00007612785f,
	      -0.000074850752f,  -0.000070208509f,  -0.000062285151f,  -0.000058270442f,
	      -0.000056296329f,  -0.000049888811f,  -0.000035615325f,  -0.000018532943f,
	       0.0000016657353f,  0.00002610587f,    0.000053397067f,   0.00008079566f },
	    { -0.00054488552f,   -0.00052537228f,   -0.00049731287f,   -0.00045778f,
	      -0.00040612387f,   -0.00034301577f,   -0.00026866337f,   -0.00018248901f,
	      -0.000084307925f,   0.000025081157f,   0.00014135583f,    0.00026649953f,
	       0.00039945057f,    0.00053928449f,    0.00068422867f,    0.00083093712f },
	    { -0.0014771431f,    -0.001283227f,     -0.0010566821f,    -0.00079780724f,
	      -0.00050782406f,   -0.00018855913f,    0.00015771533f,    0.00052769453f,
	       0.00091862219f,    0.001326357f,      0.0017469483f,     0.0021754825f,
	       0.0026067684f,     0.0030352892f,     0.0034549395f,     0.0038591374f },
	    { -0.0022995141f,    -0.001443546f,     -0.00049266568f,    0.00055068987f,
	       0.001682895f,      0.0028992873f,     0.0041943151f,     0.0055614738f,
	       0.0069935122f,     0.0084823566f,     0.010018963f,      0.011593862f,
	       0.013196872f,      0.014817309f,      0.016444042f,      0.018065533f },
	    { -0.034426283f,     -0.034281436f,     -0.033992987f,     -0.033563249f,
	      -0.032995768f,     -0.032295227f,     -0.031467363f,     -0.030518902f,
	      -0.02945766f,      -0.028291954f,     -0.027031265f,     -0.025685543f,
	      -0.024265358f,     -0.022781773f,     -0.021246184f,     -0.019670162f },
	    { -0.0030586775f,    -0.0037203205f,    -0.0042847847f,    -0.0047529764f,
	      -0.0051268316f,    -0.0054091476f,    -0.0056034233f,    -0.005714261f,
	      -0.0057445862f,    -0.0057025906f,    -0.0055920109f,    -0.0054194843f,
	      -0.0051914565f,    -0.0049146507f,    -0.0045959447f,    -0.0042418269f },
	    { -0.0016376863f,    -0.0017651899f,    -0.0018608454f,    -0.0019252141f,
	      -0.0019593791f,    -0.0019653172f,    -0.0019450618f,    -0.0018990048f,
	      -0.00183808f,      -0.0017501717f,    -0.0016481078f,    -0.0015320742f,
	      -0.0014046903f,    -0.0012685474f,    -0.001125814f,     -0.00097943726f },
	    { -0.00055432378f,   -0.00055472925f,   -0.00054783461f,   -0.00053276919f,
	      -0.00051135791f,   -0.00048466062f,   -0.00045358928f,   -0.00042499689f,
	      -0.00036942671f,   -0.0003392619f,    -0.00030001783f,   -0.00025986304f,
	      -0.0002197204f,    -0.00018116167f,   -0.00014691355f,   -0.00011279432f },
	    { -0.000064147389f,  -0.00006174868f,   -0.000054267788f,  -0.000047133824f,
	      -0.000042927582f,  -0.000039477309f,  -0.000036340745f,  -0.000029687517f,
	      -0.000049787737f,  -0.000041577889f,  -0.000033864744f,  -0.000026534748f,
	      -0.000019841305f,  -0.000014789486f,  -0.000013131184f,  -0.0000099198869f },
	    { -0.0000062990207f, -0.0000072701259f, -0.000011984052f,  -0.000017348082f,
	      -0.000019907106f,  -0.000021348773f,  -0.000021961965f,  -0.000012203576f,
	      -0.000010840992f,   4.6299544e-7f,     5.2588763e-7f,     2.7792686e-7f,
	      -2.3649704e-7f,    -0.0000010897784f, -9.171448e-7f,     -5.22682e-7f }
	};

	// Second half of the 384-tap IPQF filtering coefficients.
	static final float ipqf_coeffs2[][] = new float[][] {
	    {  5.22682e-7f,       9.171448e-7f,      0.0000010897784f,  2.3649704e-7f,
	      -2.7792686e-7f,    -5.2588763e-7f,    -4.6299544e-7f,     0.000010840992f,
	      -0.000012203576f,  -0.000021961965f,  -0.000021348773f,  -0.000019907106f,
	      -0.000017348082f,  -0.000011984052f,  -0.0000072701259f, -0.0000062990207f },
	    {  0.0000099198869f,  0.000013131184f,   0.000014789486f,   0.000019841305f,
	       0.000026534748f,   0.000033864744f,   0.000041577889f,   0.000049787737f,
	      -0.000029687517f,  -0.000036340745f,  -0.000039477309f,  -0.000042927582f,
	      -0.000047133824f,  -0.000054267788f,  -0.00006174868f,   -0.000064147389f },
	    {  0.00011279432f,    0.00014691355f,    0.00018116167f,    0.0002197204f,
	       0.00025986304f,    0.00030001783f,    0.0003392619f,     0.00036942671f,
	      -0.00042499689f,   -0.00045358928f,   -0.00048466062f,   -0.00051135791f,
	      -0.00053276919f,   -0.00054783461f,   -0.00055472925f,   -0.00055432378f },
	    {  0.00097943726f,    0.001125814f,      0.0012685474f,     0.0014046903f,
	       0.0015320742f,     0.0016481078f,     0.0017501717f,     0.00183808f,
	      -0.0018990048f,    -0.0019450618f,    -0.0019653172f,    -0.0019593791f,
	      -0.0019252141f,    -0.0018608454f,    -0.0017651899f,    -0.0016376863f },
	    {  0.0042418269f,     0.0045959447f,     0.0049146507f,     0.0051914565f,
	       0.0054194843f,     0.0055920109f,     0.0057025906f,     0.0057445862f,
	      -0.005714261f,     -0.0056034233f,    -0.0054091476f,    -0.0051268316f,
	      -0.0047529764f,    -0.0042847847f,    -0.0037203205f,    -0.0030586775f },
	    {  0.019670162f,      0.021246184f,      0.022781773f,      0.024265358f,
	       0.025685543f,      0.027031265f,      0.028291954f,      0.02945766f,
	      -0.030518902f,     -0.031467363f,     -0.032295227f,     -0.032995768f,
	      -0.033563249f,     -0.033992987f,     -0.034281436f,     -0.034426283f },
	    { -0.018065533f,     -0.016444042f,     -0.014817309f,     -0.013196872f,
	      -0.011593862f,     -0.010018963f,     -0.0084823566f,    -0.0069935122f,
	       0.0055614738f,     0.0041943151f,     0.0028992873f,     0.001682895f,
	       0.00055068987f,   -0.00049266568f,   -0.001443546f,     -0.0022995141f },
	    { -0.0038591374f,    -0.0034549395f,    -0.0030352892f,    -0.0026067684f,
	      -0.0021754825f,    -0.0017469483f,    -0.001326357f,     -0.00091862219f,
	       0.00052769453f,    0.00015771533f,   -0.00018855913f,   -0.00050782406f,
	      -0.00079780724f,   -0.0010566821f,    -0.001283227f,     -0.0014771431f },
	    { -0.00083093712f,   -0.00068422867f,   -0.00053928449f,   -0.00039945057f,
	      -0.00026649953f,   -0.00014135583f,   -0.000025081157f,   0.000084307925f,
	      -0.00018248901f,   -0.00026866337f,   -0.00034301577f,   -0.00040612387f,
	      -0.00045778f,      -0.00049731287f,   -0.00052537228f,   -0.00054488552f },
	    { -0.00008079566f,   -0.000053397067f,  -0.00002610587f,   -0.0000016657353f,
	       0.000018532943f,   0.000035615325f,   0.000049888811f,   0.000056296329f,
	      -0.000058270442f,  -0.000062285151f,  -0.000070208509f,  -0.000074850752f,
	      -0.00007612785f,   -0.000073081472f,  -0.000068306952f,  -0.000064464548f },
	    { -0.000048585582f,  -0.000046043948f,  -0.000045337845f,  -0.000041575615f,
	      -0.000036968919f,  -0.000032302323f,  -0.00002780562f,   -0.000023530851f,
	       0.0000018111954f, -5.7168614e-7f,    -0.0000015432918f, -0.0000026336629f,
	      -0.0000041712024f, -0.0000079212787f, -0.000010502935f,  -0.0000091622824f },
	    { -0.0000070302972f, -0.0000063894258f, -0.0000046192422f, -0.0000032569742f,
	      -0.0000023108685f, -0.0000014333754f, -6.005389e-7f,      0.000011838618f,
	       0.000010516783f,   1.2660377e-8f,     3.530856e-8f,      3.226247e-8f,
	      -4.4400572e-8f,    -4.2005411e-7f,    -8.0604229e-7f,    -5.8336207e-7f }
	};

	private static int DEQUANT_PHASE(int ph) {
		return (ph & 0x1F) << 6;
	}

	public void initImdct(FFT mdctCtx) {
		SineWin.initFfSineWindows();

		// Initialize the MDCT transform
		mdctCtx.mdctInit(8, true, -1.0);
	}

	public static void initWaveSynth() {
		// generate sine wave table
		for (int i = 0; i < 2048; i++) {
			sine_table[i] = (float) sin(TWOPI * i / 2048);
		}

		// generate Hann window
		for (int i = 0; i < 256; i++) {
			hann_window[i] = (float) ((1.0 - cos(TWOPI * i / 256)) * 0.5);
		}

		// generate amplitude scalefactors table
		for (int i = 0; i < 64; i++) {
			amp_sf_tab[i] = (float) pow(2.0, ((double) (i - 3)) / 4.0);
		}
	}

	public void powerCompensation(ChannelUnitContext ctx, int chIndex, float[] sp, int rngIndex, int sb) {
		float pwcsp[] = new float[ATRAC3P_SUBBAND_SAMPLES];
		int gcv = 0;
		int swapCh = (ctx.unitType == CH_UNIT_STEREO && ctx.swapChannels[sb] ? 1 : 0);

		if (ctx.channels[chIndex ^ swapCh].powerLevs[subband_to_powgrp[sb]] == ATRAC3P_POWER_COMP_OFF) {
			return;
		}

		// generate initial noise spectrum
		for (int i = 0; i < ATRAC3P_SUBBAND_SAMPLES; i++, rngIndex++) {
			pwcsp[i] = noise_tab[rngIndex & 0x3FF];
		}

		// check gain control information
		AtracGainInfo g1 = ctx.channels[chIndex ^ swapCh].gainData[sb];
		AtracGainInfo g2 = ctx.channels[chIndex ^ swapCh].gainDataPrev[sb];

		int gainLev = (g1.numPoints > 0 ? (6 - g1.levCode[0]) : 0);

		for (int i = 0; i < g2.numPoints; i++) {
			gcv = max(gcv, gainLev - (g2.levCode[i] - 6));
		}

		for (int i = 0; i < g1.numPoints; i++) {
			gcv = max(gcv, 6 - g1.levCode[i]);
		}

		float grpLev = pwc_levs[ctx.channels[chIndex ^ swapCh].powerLevs[subband_to_powgrp[sb]]] / (1 << gcv);

		// skip the lowest two quant units (frequencies 0...351 Hz) for subband 0
		for (int qu = subband_to_qu[sb] + (sb == 0 ? 2 : 0); qu < subband_to_qu[sb + 1]; qu++) {
			if (ctx.channels[chIndex].quWordlen[qu] <= 0) {
				continue;
			}

			float quLev = ff_atrac3p_sf_tab[ctx.channels[chIndex].quSfIdx[qu]] * ff_atrac3p_mant_tab[ctx.channels[chIndex].quWordlen[qu]] / (1 << ctx.channels[chIndex].quWordlen[qu]) * grpLev;

			int dst = ff_atrac3p_qu_to_spec_pos[qu];
			int nsp = ff_atrac3p_qu_to_spec_pos[qu + 1] - ff_atrac3p_qu_to_spec_pos[qu];

			for (int i = 0; i < nsp; i++) {
				sp[dst + i] += pwcsp[i] * quLev;
			}
		}
	}

	public void imdct(FFT mdctCtx, float in[], int inOffset, float out[], int outOffset, int windId, int sb) {
		if ((sb & 1) != 0) {
			for (int i = 0; i < ATRAC3P_SUBBAND_SAMPLES / 2; i++) {
				float tmp = in[inOffset + i];
				in[inOffset + i] = in[inOffset + ATRAC3P_SUBBAND_SAMPLES - 1 - i];
				in[inOffset + ATRAC3P_SUBBAND_SAMPLES - 1 - i] = tmp;
			}
		}

		mdctCtx.imdctCalc(out, outOffset, in, inOffset);

	    /* Perform windowing on the output.
	     * ATRAC3+ uses two different MDCT windows:
	     * - The first one is just the plain sine window of size 256
	     * - The 2nd one is the plain sine window of size 128
	     *   wrapped into zero (at the start) and one (at the end) regions.
	     *   Both regions are 32 samples long. */
		if ((windId & 2) != 0) { // 1st half: steep window
			Arrays.fill(out, outOffset, outOffset + 32, 0f);
			vectorFmul(out, outOffset + 32, out, outOffset + 32, ff_sine_64, 0, 64);
		} else { // 1st halt: simple sine window
			vectorFmul(out, outOffset, out, outOffset, ff_sine_128, 0, ATRAC3P_MDCT_SIZE / 2);
		}

		if ((windId & 1) != 0) { // 2nd half: steep window
			vectorFmulReverse(out, outOffset + 160, out, outOffset + 160, ff_sine_64, 0, 64);
			Arrays.fill(out, outOffset + 224, outOffset + 224 + 32, 0f);
		} else { // 2nd half: simple sine window
			vectorFmulReverse(out, outOffset + 128, out, outOffset + 128, ff_sine_128, 0, ATRAC3P_MDCT_SIZE / 2);
		}
	}

	/**
	 *  Synthesize sine waves according to given parameters.
	 *
	 *  @param[in]    synth_param   ptr to common synthesis parameters
	 *  @param[in]    waves_info    parameters for each sine wave
	 *  @param[in]    envelope      envelope data for all waves in a group
	 *  @param[in]    phase_shift   flag indicates 180 degrees phase shift
	 *  @param[in]    reg_offset    region offset for trimming envelope data
	 *  @param[out]   out           receives sythesized data
	 */
	private void wavesSynth(WaveSynthParams synthParams, WavesData wavesInfo, WaveEnvelope envelope, int phaseShift, int regOffset, float[] out) {
		int waveParam = wavesInfo.startIndex;

		for (int wn = 0; wn < wavesInfo.numWavs; wn++, waveParam++) {
			// amplitude dequantization
			double amp = amp_sf_tab[synthParams.waves[waveParam].ampSf] * (synthParams.amplitudeMode == 0 ? (synthParams.waves[waveParam].ampIndex + 1) / 15.13f : 1.0f);

			int inc = synthParams.waves[waveParam].freqIndex;
			int pos = DEQUANT_PHASE(synthParams.waves[waveParam].phaseIndex) - (regOffset ^ 128) * inc & 2047;

			// waveform generation
			for (int i = 0; i < 128; i++) {
				out[i] += sine_table[pos] * amp;
				pos     = (pos + inc) & 2047;
			}
		}

		// fade in with steep Hann window if requested
		if (envelope.hasStartPoint) {
			int pos = (envelope.startPos << 2) - regOffset;
			if (pos > 0 && pos <= 128) {
				Arrays.fill(out, 0, pos, 0f);
				if (!envelope.hasStopPoint || envelope.startPos != envelope.stopPos) {
					out[pos + 0] *= hann_window[ 0];
					out[pos + 1] *= hann_window[32];
					out[pos + 2] *= hann_window[64];
					out[pos + 3] *= hann_window[96];
				}
			}
		}

		// fade out with steep Hann window if requested
		if (envelope.hasStopPoint) {
			int pos = (envelope.stopPos + 1 << 2) - regOffset;
			if (pos > 0 && pos <= 128) {
				out[pos - 4] *= hann_window[96];
				out[pos - 3] *= hann_window[64];
				out[pos - 2] *= hann_window[32];
				out[pos - 1] *= hann_window[ 0];
				Arrays.fill(out, pos, 128, 0f);
			}
		}
	}

	public void generateTones(ChannelUnitContext ctx, int chNum, int sb, float out[], int outOffset) {
		float[] wavreg1 = new float[128];
		float[] wavreg2 = new float[128];
		WavesData tonesNow = ctx.channels[chNum].tonesInfoPrev[sb];
		WavesData tonesNext = ctx.channels[chNum].tonesInfo[sb];

		// reconstruct full envelopes for both overlapping regions
		// from truncated bitstream data
		if (tonesNext.pendEnv.hasStartPoint && tonesNext.pendEnv.startPos < tonesNext.pendEnv.stopPos) {
			tonesNext.currEnv.hasStartPoint = true;
			tonesNext.currEnv.startPos = tonesNext.pendEnv.startPos + 32;
		} else if (tonesNow.pendEnv.hasStartPoint) {
			tonesNext.currEnv.hasStartPoint = true;
			tonesNext.currEnv.startPos = tonesNow.pendEnv.startPos;
		} else {
			tonesNext.currEnv.hasStartPoint = false;
			tonesNext.currEnv.startPos = 0;
		}

		if (tonesNow.pendEnv.hasStopPoint && tonesNow.pendEnv.stopPos >= tonesNext.currEnv.startPos) {
			tonesNext.currEnv.hasStopPoint = true;
			tonesNext.currEnv.stopPos = tonesNow.pendEnv.stopPos;
		} else if (tonesNext.pendEnv.hasStopPoint) {
			tonesNext.currEnv.hasStopPoint = true;
			tonesNext.currEnv.stopPos = tonesNext.pendEnv.stopPos + 32;
		} else {
			tonesNext.currEnv.hasStopPoint = false;
			tonesNext.currEnv.stopPos = 64;
		}

		// is the visible part of the envelope non-zero?
		boolean reg1EnvNonzero = (tonesNow.currEnv.stopPos    < 32 ? false : true);
		boolean reg2EnvNonzero = (tonesNext.currEnv.startPos >= 32 ? false : true);

		// synthesize waves for both overlapping regions
		if (tonesNow.numWavs > 0 && reg1EnvNonzero) {
			wavesSynth(ctx.wavesInfoPrev, tonesNow, tonesNow.currEnv, ctx.wavesInfoPrev.phaseShift[sb] ? chNum & 1 : 0, 128, wavreg1);
		}

		if (tonesNext.numWavs > 0 && reg2EnvNonzero) {
			wavesSynth(ctx.wavesInfoPrev, tonesNext, tonesNext.currEnv, ctx.wavesInfo.phaseShift[sb] ? chNum & 1 : 0, 0, wavreg2);
		}

		// Hann windowing for non-faded wave signals
		if (tonesNow.numWavs > 0 && tonesNext.numWavs > 0 && reg1EnvNonzero && reg2EnvNonzero) {
			vectorFmul(wavreg1, 0, wavreg1, 0, hann_window, 128, 128);
			vectorFmul(wavreg2, 0, wavreg2, 0, hann_window,   0, 128);
		} else {
			if (tonesNow.numWavs > 0 && !tonesNow.currEnv.hasStopPoint) {
				vectorFmul(wavreg1, 0, wavreg1, 0, hann_window, 128, 128);
			}
			if (tonesNext.numWavs > 0 && !tonesNext.currEnv.hasStartPoint) {
				vectorFmul(wavreg2, 0, wavreg2, 0, hann_window, 0, 128);
			}
		}

		// Overlap and add to residual
		for (int i = 0; i < 128; i++) {
			out[outOffset + i] += wavreg1[i] + wavreg2[i];
		}
	}

	public void ipqf(FFT dctCtx, IPQFChannelContext hist, float[] in, float[] out) {
		float[] idctIn  = new float[ATRAC3P_SUBBANDS];
		float[] idctOut = new float[ATRAC3P_SUBBANDS];

		Arrays.fill(out, 0, ATRAC3P_FRAME_SAMPLES, 0f);

		for (int s = 0; s < ATRAC3P_SUBBAND_SAMPLES; s++) {
			// pack up one sample from each subband
			for (int sb = 0; sb < ATRAC3P_SUBBANDS; sb++) {
				idctIn[sb] = in[sb * ATRAC3P_SUBBAND_SAMPLES + s];
			}

			// Calculate the sine and cosine part of the PQF using IDCT-IV
			dctCtx.imdctHalf(idctOut, 0, idctIn, 0);

			// append the result to the history
			for (int i = 0; i < 8; i++) {
				hist.buf1[hist.pos][i] = idctOut[i + 8];
				hist.buf2[hist.pos][i] = idctOut[7 - i];
			}

			int posNow = hist.pos;
			int posNext = mod23_lut[posNow + 2]; // posNext = (posNow + 1) % 23

			for (int t = 0; t < Atrac3plusDecoder.ATRAC3P_PQF_FIR_LEN; t++) {
				for (int i = 0; i < 8; i++) {
					out[s * 16 + i + 0] += hist.buf1[posNow][i] * ipqf_coeffs1[t][i] + hist.buf2[posNext][i] * ipqf_coeffs2[t][i];
					out[s * 16 + i + 8] += hist.buf1[posNow][7 - i] * ipqf_coeffs1[t][i + 8] + hist.buf2[posNext][7 - i] * ipqf_coeffs2[t][i + 8];
				}

				posNow = mod23_lut[posNext + 2]; // posNow = (posNow + 2) % 23;
				posNext = mod23_lut[posNow + 2]; // posNext = (posNext + 2) % 23;
			}

			hist.pos = mod23_lut[hist.pos]; // hist.pos = (hist.pos - 1) % 23;
		}
	}
}
