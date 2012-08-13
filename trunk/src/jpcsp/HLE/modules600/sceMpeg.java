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
package jpcsp.HLE.modules600;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;

public class sceMpeg extends jpcsp.HLE.modules352.sceMpeg {

    @HLEFunction(nid = 0x63B9536A, version = 600)
    public int sceMpegAvcResourceGetAvcDecTopAddr(int unknown) {
        log.warn(String.format("UNIMPLEMENTED: sceMpegAvcResourceGetAvcDecTopAddr unknown=%d", unknown));

        // Unknown value, passed to sceMpegCreate(ddttop)
        return 0x12345678;
    }

    @HLEFunction(nid = 0x8160A2FE, version = 600)
    public void sceMpegAvcResourceFinish(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("IGNORING: sceMpegAvcResourceFinish");

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xAF26BB01, version = 600)
    public int sceMpegAvcResourceGetAvcEsBuf() {
        log.warn("UNIMPLEMENTED: sceMpegAvcResourceGetAvcEsBuf");

        // Unknown value
        return 0;
    }

    @HLEFunction(nid = 0xFCBDB5AD, version = 600)
    public int sceMpegAvcResourceInit(int unknown) {
        log.warn(String.format("IGNORING: sceMpegAvcResourceInit unknown=%d", unknown));

        if (unknown != 1) {
        	return SceKernelErrors.ERROR_MPEG_INVALID_VALUE;
        }

        return 0;
    }
}