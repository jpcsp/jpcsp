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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

public class sceMp4 extends HLEModule {
    protected static Logger log = Modules.getLogger("sceMp4");

    @Override
    public String getName() {
        return "sceMp4";
    }

	@HLEUnimplemented
    @HLEFunction(nid = 0x68651CBC, version = 150, checkInsideInterrupt = true)
    public int sceMp4Init(boolean unk1, boolean unk2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9042B257, version = 150, checkInsideInterrupt = true)
    public int sceMp4Finish() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB1221EE7, version = 150, checkInsideInterrupt = true)
    public int sceMp4Create(int unknown1, int unknown2, int unknown3, int unknown4) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x538C2057, version = 150)
    public int sceMp4Delete() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x113E9E7B, version = 150)
    public int sceMp4_113E9E7B(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7443AF1D, version = 150)
    public int sceMp4_7443AF1D(int unknown1, TPointer unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5EB65F26, version = 150)
    public int sceMp4_5EB65F26(int unknown1, int unknown2) {
        // Application expects return value > 0
        return 1;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7ADFD01C, version = 150)
    public int sceMp4_7ADFD01C(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5) {
        return 0;
    }

    @HLEFunction(nid = 0xBCA9389C, version = 150)
    public int sceMp4_BCA9389C(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5) {
        log.warn(String.format("PARTIAL: sceMp4_BCA9389C unknown1=%d, unknown2=%d, unknown3=%d, unknown4=%d, unknown5=%d", unknown1, unknown2, unknown3, unknown4, unknown5));

        int value = Math.max(unknown2 * unknown3, unknown4 << 1) + (unknown2 << 6) + unknown5 + 256;
        log.warn(String.format("sceMp4_BCA9389C returning %d", value));

        return value;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9C8F4FC1, version = 150)
    public int sceMp4_9C8F4FC1(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6, int unknown7, int unknown8) {
    	// unknown4 == value returned by sceMp4_BCA9389C
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0F0187D2, version = 150)
    public int sceMp4_0F0187D2(int unknown1, int unknown2, int unknown3) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9CE6F5CF, version = 150)
    public int sceMp4_9CE6F5CF(int unknown1, int unknown2, int unknown3) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4ED4AB1E, version = 150)
    public int sceMp4_4ED4AB1E(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x10EE0D2C, version = 150)
    public int sceMp4_10EE0D2C(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x496E8A65, version = 150)
    public int sceMp4_496E8A65(int unknown1, int unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB4B400D1, version = 150)
    public int sceMp4_B4B400D1(int unknown1, int unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF7C51EC1, version = 150)
    public int sceMp4_F7C51EC1(int unknown1, int unknown2, int unknown3, TPointer unknown4) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x74A1CA3E, version = 150)
    public int sceMp4_74A1CA3E(int unknown1, int unknown2, int unknown3, int unknown4) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD8250B75, version = 150)
    public int sceMp4_D8250B75(int unknown1, int unknown2, int unknown3) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8754ECB8, version = 150)
    public int sceMp4_8754ECB8(int unknown1, int unknown2, int unknown3, int unknown4) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5601A6F0, version = 150)
    public int sceMp4_5601A6F0(int unknown1, int unknown2, int unknown3, int unknown4) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7663CB5C, version = 150)
    public int sceMp4_7663CB5C(int unknown1, int unknown2, int unknown3, int unknown4) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x503A3CBA, version = 150)
    public int sceMp4_503A3CBA(int unknown1, int unknown2, int unknown3, int unknown4) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x01C76489, version = 150)
    public int sceMp4_01C76489(int unknown1, int unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6710FE77, version = 150)
    public int sceMp4_6710FE77(int unknown1, int unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5D72B333, version = 150)
    public int sceMp4_5D72B333(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7D332394, version = 150)
    public int sceMp4_7D332394() {
        return 0;
    }
}