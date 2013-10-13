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
package jpcsp.HLE.modules500;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Random;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class sceSfmt19937 extends HLEModule {
    public static Logger log = Modules.getLogger("sceSfmt19937");

    @Override
    public String getName() {
        return "sceSfmt19937";
    }
    
    protected final static int PSP_SFMT19937_LENGTH = 156;
    private HashMap<TPointer, sfmt19937Ctx> ctxMap = new HashMap<TPointer, sfmt19937Ctx>();
    
    private static class sfmt19937Ctx {
        private int index;
        private int indexPos;
        private int[][] sfmt;
        private TPointer addr;
        private int seed;
        private int[] seedArray;
        
        public sfmt19937Ctx(TPointer ctxAddr, int seed) {
            this.index = 0;
            this.indexPos = 0;
            this.sfmt = new int[PSP_SFMT19937_LENGTH][4];
            this.addr = ctxAddr;
            this.seed = seed;
        }
        
        public sfmt19937Ctx(TPointer ctxAddr, int[] seeds) {
            this.index = 0;
            this.indexPos = 0;
            this.sfmt = new int[PSP_SFMT19937_LENGTH][4];
            this.addr = ctxAddr;
            this.seedArray = seeds;
        }
        
        private void generate() {
            // If using a seed array, just assign the first seed.
            if (this.seedArray != null) {
                this.seed = seedArray[0];
            }
            
            // Store the current index.
            this.addr.setValue32(this.index);
            
            // Generate a SFMT19937 context with Random and write the values.
            Random rand = new Random(this.seed);
            for (int i = 0; i < PSP_SFMT19937_LENGTH; i++) {
                for (int j = 0; j < 4; j++) {
                    sfmt[i][j] = rand.nextInt();
                    this.addr.setValue32(sfmt[i][j]);
                }
            }
        } 
        
        private int getNextRand() {
            int r = sfmt[index][indexPos];
            if ((this.indexPos + 1) < 4) {
                this.indexPos++;
            } else {
                this.indexPos = 0;
                this.index++;
            }
            return r;
        }
        
        private long getNextRand64() {
            long r1 = getNextRand();
            long r2 = getNextRand();
            return ((r1 << 32) | r2);
        }
        
    }

    @HLEFunction(nid = 0x161ACEB2, version = 500)
    public int sceSfmt19937InitGenRand(TPointer sfmtctx, int seed) {
        // Assign and store the current context.
        sfmt19937Ctx ctx = new sfmt19937Ctx(sfmtctx, seed);
        ctxMap.put(sfmtctx, ctx);
    	return 0;
    }

    @HLEFunction(nid = 0xDD5A5D6C, version = 500)
    public int sceSfmt19937InitByArray(TPointer sfmtctx, TPointer seeds, int seedsLength) {
        // Read and store the seeds.
        int[] s = new int[seedsLength];
        for (int i = 0; i < seedsLength; i++) {
            s[i] = seeds.getValue32();
        }
        // Assign and store the current context.
        sfmt19937Ctx ctx = new sfmt19937Ctx(sfmtctx, s);
        ctxMap.put(sfmtctx, ctx);
        return 0;
    }
    
    @HLEFunction(nid = 0xB33FE749, version = 500)
    public int sceSfmt19937GenRand32(TPointer sfmtctx) {
        int result = 0;
        // Check if the context has been initialized.
        if (ctxMap.containsKey(sfmtctx)) {
            sfmt19937Ctx ctx = ctxMap.get(sfmtctx);
            ctx.generate();
            result = ctx.getNextRand();
        }
    	return result;
    }
    
    @HLEFunction(nid = 0xD5AC9F99, version = 500)
    public long sceSfmt19937GenRand64(TPointer sfmtctx) {
    	long result = 0;
        // Check if the context has been initialized.
        if (ctxMap.containsKey(sfmtctx)) {
            sfmt19937Ctx ctx = ctxMap.get(sfmtctx);
            ctx.generate();
            result = ctx.getNextRand64();
        }
    	return result;
    }
    
    @HLEFunction(nid = 0xDB025BFA, version = 500)
    public int sceSfmt19937FillArray32(TPointer sfmtctx, TPointer array, int arrayLength) {
        // Check if the context has been initialized.
        if (ctxMap.containsKey(sfmtctx)) {
            sfmt19937Ctx ctx = ctxMap.get(sfmtctx);
            ctx.generate();
            // Fill the array with the random values.
            for (int i = 0; i < arrayLength; i++) {
                array.setValue32(i, ctx.getNextRand());
            }
        }
        return 0;
    }
    
    @HLEFunction(nid = 0xEE2938C4, version = 500)
    public int sceSfmt19937FillArray64(TPointer sfmtctx, TPointer array, int arrayLength) {
        // Check if the context has been initialized.
        if (ctxMap.containsKey(sfmtctx)) {
            sfmt19937Ctx ctx = ctxMap.get(sfmtctx);
            ctx.generate();
            // Fill the array with the random values.
            for (int i = 0; i < arrayLength; i++) {
                array.setValue64(i, ctx.getNextRand64());
            }
        }
        return 0;
    }
}
