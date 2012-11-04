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
package jpcsp.HLE.modules150;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceMT19937;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class sceMt19937 extends HLEModule {
    public static Logger log = Modules.getLogger("sceMt19937");

    @Override
	public String getName() {
		return "sceMt19937";
	}

    // Based on http://beam.acclab.helsinki.fi/~knordlun/mc/mt19937.c
    /* A C-program for MT19937: Real number version                */
    /*   genrand() generates one pseudorandom real number (double) */
    /* which is uniformly distributed on [0,1]-interval, for each  */
    /* call. sgenrand(seed) set initial values to the working area */
    /* of 624 words. Before genrand(), sgenrand(seed) must be      */
    /* called once. (seed is any 32-bit integer except for 0).     */
    /* Integer generator is obtained by modifying two lines.       */
    /*   Coded by Takuji Nishimura, considering the suggestions by */
    /* Topher Cooper and Marc Rieffel in July-Aug. 1997.           */

    /* This library is free software; you can redistribute it and/or   */
    /* modify it under the terms of the GNU Library General Public     */
    /* License as published by the Free Software Foundation; either    */
    /* version 2 of the License, or (at your option) any later         */
    /* version.                                                        */
    /* This library is distributed in the hope that it will be useful, */
    /* but WITHOUT ANY WARRANTY; without even the implied warranty of  */
    /* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.            */
    /* See the GNU Library General Public License for more details.    */
    /* You should have received a copy of the GNU Library General      */
    /* Public License along with this library; if not, write to the    */
    /* Free Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA   */ 
    /* 02111-1307  USA                                                 */

    /* Copyright (C) 1997 Makoto Matsumoto and Takuji Nishimura.       */
    /* Any feedback is very welcome. For any question, comments,       */
    /* see http://www.math.keio.ac.jp/matumoto/emt.html or email       */
    /* matumoto@math.keio.ac.jp                                        */
    public static class MT19937 {
    	public static final int N = 624;
    	private static final int M = 397;
    	private static final int MATRIX_A = 0x9908b0df; /* constant vector a */
    	private static final int UPPER_MASK = 0x80000000;
    	private static final int LOWER_MASK = 0x7FFFFFFF;
    	private static final int[] mag01 = new int[] { 0, MATRIX_A };

    	/* Tempering parameters */   
    	public static final int TEMPERING_MASK_B = 0x9d2c5680;
    	public static final int TEMPERING_MASK_C = 0xefc60000;
    	public static final int TEMPERING_SHIFT_U = 11;
    	public static final int TEMPERING_SHIFT_S = 7;
    	public static final int TEMPERING_SHIFT_T = 15;
    	public static final int TEMPERING_SHIFT_L = 18;

    	protected static void init(SceMT19937 mt19937, int seed) {
    		mt19937.mt[0] = seed;
    		for (int mti = 1; mti < N; mti++) {
    			mt19937.mt[mti] = 69069 * mt19937.mt[mti - 1];
    		}
    		mt19937.mti = N;
    	}

    	protected static int getInt(SceMT19937 mt19937) {
    		if (mt19937.mti >= N) {
    			// Generate N words at one time

    			if (mt19937.mti == N + 1) {
    				// init has not been called
    				init(mt19937, 4357);
    			}

    			int kk;
    			for (kk = 0; kk < N-M; kk++) {
    				int y = (mt19937.mt[kk] & UPPER_MASK) | (mt19937.mt[kk + 1] & LOWER_MASK);
    				mt19937.mt[kk] = mt19937.mt[kk + M] ^ (y >>> 1) ^ mag01[y & 0x1];
    			}
    			for (; kk < N - 1; kk++) {
    				int y = (mt19937.mt[kk] & UPPER_MASK) | (mt19937.mt[kk + 1] & LOWER_MASK);
    				mt19937.mt[kk] = mt19937.mt[kk + (M - N)] ^ (y >>> 1) ^ mag01[y & 0x1];
    			}
				int y = (mt19937.mt[N - 1] & UPPER_MASK) | (mt19937.mt[0] & LOWER_MASK);
				mt19937.mt[N - 1] = mt19937.mt[M - 1] ^ (y >>> 1) ^ mag01[y & 0x1];

				mt19937.mti = 0;
    		}

    		long y = mt19937.mt[mt19937.mti++];
    		y ^= y >> TEMPERING_SHIFT_U;
    		y ^= (y << TEMPERING_SHIFT_S) & TEMPERING_MASK_B;
    		y ^= (y << TEMPERING_SHIFT_T) & TEMPERING_MASK_C;
    		y ^= (y >> TEMPERING_SHIFT_L);

    		return (int) y;
    	}
    }

    @HLEFunction(nid = 0xECF5D379, version = 150)
    public int sceMt19937Init(TPointer mt19937Addr, int seed) {
    	SceMT19937 mt19937 = new SceMT19937();
    	MT19937.init(mt19937, seed);
    	mt19937.write(mt19937Addr);

    	return 0;
    }

    @HLEFunction(nid = 0xF40C98E6, version = 150)
    public int sceMt19937UInt(SceMT19937 mt19937) {
    	int random = MT19937.getInt(mt19937);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceMt19937UInt returning 0x%08X", random));
    	}
    	mt19937.write(Memory.getInstance());

    	return random;
    }
}
