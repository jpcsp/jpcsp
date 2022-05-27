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
package jpcsp.HLE.modules;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;

public class sceHibari extends HLEModule {
    public static Logger log = Modules.getLogger("sceHibari");

    @HLEUnimplemented
    @HLEFunction(nid = 0x8CD96FBC, version = 270)
    public int sceHibariGetDisplayStatus() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8FA42787, version = 270)
    public int sceHibariResetEnable() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5F9F4128, version = 270)
    public int sceHibariDisplayOn() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC06EA555, version = 270)
    public int sceHibariDisplayOff() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3EB2A14D, version = 270)
    public int sceHibariSetPollingMode() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x722367A3, version = 270)
    public int sceHibariResetSeq() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB38576DD, version = 270)
    public int sceHibariSeqSync() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3EE467E2, version = 270)
    public int sceHibariSeqIntr() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0C845F0B, version = 270)
    public int sceHibari_driver_0C845F0B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1A5B8690, version = 270)
    public int sceHibari_driver_1A5B8690() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x21281A46, version = 270)
    public int sceHibari_driver_21281A46() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x521DA642, version = 270)
    public int sceHibari_driver_521DA642() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x62A029D3, version = 270)
    public int sceHibari_driver_62A029D3() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6F326EA1, version = 270)
    public int sceHibari_driver_6F326EA1() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6FFE5CA3, version = 270)
    public int sceHibari_driver_6FFE5CA3() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x78B212BE, version = 270)
    public int sceHibari_driver_78B212BE() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9D3779AA, version = 270)
    public int sceHibari_driver_9D3779AA() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB6B5C196, version = 270)
    public int sceHibari_driver_B6B5C196() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCE098CEA, version = 270)
    public int sceHibari_driver_CE098CEA() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEAD2021B, version = 270)
    public int sceHibari_driver_EAD2021B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFA7339D7, version = 270)
    public int sceHibari_driver_FA7339D7() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE0942EC0, version = 660)
    public int sceHibari_driver_E0942EC0() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5708EC83, version = 660)
    public int sceHibari_driver_5708EC83() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9FE05089, version = 660)
    public int sceHibari_driver_9FE05089() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCE6B29C1, version = 660)
    public int sceHibari_driver_CE6B29C1() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1DE6CE92, version = 660)
    public int sceHibari_driver_1DE6CE92() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEA4A819C, version = 660)
    public int sceHibari_driver_EA4A819C(int unknown, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer buffer, int length) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5C05C60A, version = 660)
    public int sceHibari_driver_5C05C60A() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x01E4DE44, version = 660)
    public int sceHibari_driver_01E4DE44() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8C27C42F, version = 660)
    public int sceHibari_driver_8C27C42F() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEA03F94F, version = 660)
    public int sceHibari_driver_EA03F94F() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7C4ED919, version = 660)
    public int sceHibari_driver_7C4ED919() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x01A615B6, version = 660)
    public int sceHibari_driver_01A615B6() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE2979E0C, version = 660)
    public int sceHibari_driver_E2979E0C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1ABA6D77, version = 660)
    public int sceHibari_driver_1ABA6D77() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD5848115, version = 660)
    public int sceHibari_driver_D5848115() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6CFDEB41, version = 660)
    public int sceHibari_driver_6CFDEB41() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB52DE6F7, version = 660)
    public int sceHibari_driver_B52DE6F7() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1A9751BE, version = 660)
    public int sceHibari_driver_1A9751BE() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x46F5BB4E, version = 660)
    public int sceHibari_driver_46F5BB4E() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE768F9C0, version = 660)
    public int sceHibari_driver_E768F9C0() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC475DB89, version = 660)
    public int sceHibari_driver_C475DB89() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x20E82780, version = 660)
    public int sceHibari_driver_20E82780() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x60AB9E40, version = 660)
    public int sceHibari_driver_60AB9E40() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x76326803, version = 660)
    public int sceHibari_driver_76326803() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8A9F73C1, version = 660)
    public int sceHibari_driver_8A9F73C1() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x078A55E9, version = 660)
    public int sceHibari_driver_078A55E9() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD1B2EE85, version = 660)
    public int sceHibari_driver_D1B2EE85() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3353E0CE, version = 660)
    public int sceHibari_driver_3353E0CE() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC5D2D843, version = 660)
    public int sceHibari_driver_C5D2D843() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x700820E2, version = 660)
    public int sceHibari_driver_700820E2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x45D8422C, version = 660)
    public int sceHibari_driver_45D8422C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x10C6C75C, version = 660)
    public int sceHibari_driver_10C6C75C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7FCB5D7B, version = 660)
    public int sceHibari_driver_7FCB5D7B() {
    	return 0;
    }
}
