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
import jpcsp.HLE.TPointer32;

public class sceMgr_driver extends HLEModule {
    public static Logger log = Modules.getLogger("sceMgr_driver");

    @HLEUnimplemented
    @HLEFunction(nid = 0x2B68BFD5, version = 150)
    public int sceMgr_driver_2B68BFD5() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x31C1B9CE, version = 150)
    public int sceMgr_driver_31C1B9CE() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x40976398, version = 150)
    public int sceMgr_driver_40976398() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7505C8FB, version = 150)
    public int sceMgr_driver_7505C8FB() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x831F92C7, version = 150)
    public int sceMgr_driver_831F92C7() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD9059D0E, version = 150)
    public int sceMgr_driver_D9059D0E() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7B11D7AD, version = 150)
    public int sceMgr_driver_7B11D7AD() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x02117F1E, version = 150)
    public int sceMgr_driver_02117F1E() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x09C11491, version = 150)
    public int sceMgr_driver_09C11491() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0C813CF1, version = 150)
    public int sceMgr_driver_0C813CF1() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0EBA1587, version = 150)
    public int sceMgr_driver_0EBA1587() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x156A83C3, version = 150)
    public int sceMgr_driver_156A83C3() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1B4DB19C, version = 150)
    public int sceMgr_driver_1B4DB19C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1B63AFF9, version = 150)
    public int sceMgr_driver_1B63AFF9() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x20610CDF, version = 150)
    public int sceMgr_driver_20610CDF() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x275C6686, version = 150)
    public int sceMgr_driver_275C6686() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2D1CB2B8, version = 150)
    public int sceMgr_driver_2D1CB2B8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x39E49050, version = 150)
    public int sceMgr_driver_39E49050() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3B548024, version = 150)
    public int sceMgr_driver_3B548024() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x401F71DC, version = 150)
    public int sceMgr_driver_401F71DC() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x45EA1DB5, version = 150)
    public int sceMgr_driver_45EA1DB5() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4FD151B7, version = 150)
    public int sceMgr_driver_4FD151B7() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5A63B6A4, version = 150)
    public int sceMgr_driver_5A63B6A4() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5BABFFAE, version = 150)
    public int sceMgr_driver_5BABFFAE(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.inout) TPointer32 unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5C5AB33B, version = 150)
    public int sceMgr_driver_5C5AB33B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6B4C5BC5, version = 150)
    public int sceMgr_driver_6B4C5BC5() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x735972DB, version = 150)
    public int sceMgr_driver_735972DB() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7DB1B0E5, version = 150)
    public int sceMgr_driver_7DB1B0E5() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7EC9DF8B, version = 150)
    public int sceMgr_driver_7EC9DF8B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7F37BECF, version = 150)
    public int sceMgr_driver_7F37BECF() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x84B044C7, version = 150)
    public int sceMgr_driver_84B044C7() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x85F518AB, version = 150)
    public int sceMgr_driver_85F518AB() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x864EA078, version = 150)
    public int sceMgr_driver_864EA078() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9315D849, version = 150)
    public int sceMgr_driver_9315D849() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x97B245D5, version = 150)
    public int sceMgr_driver_97B245D5() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9973CD5B, version = 150)
    public int sceMgr_driver_9973CD5B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9AE6ED58, version = 150)
    public int sceMgr_driver_9AE6ED58() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA329A73B, version = 150)
    public int sceMgr_driver_A329A73B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA3A302CC, version = 150)
    public int sceMgr_driver_A3A302CC() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB40FEAD1, version = 150)
    public int sceMgr_driver_B40FEAD1() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB53B2B5B, version = 150)
    public int sceMgr_driver_B53B2B5B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB8626BA2, version = 150)
    public int sceMgr_driver_B8626BA2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBBF6C182, version = 150)
    public int sceMgr_driver_BBF6C182() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC0B28CED, version = 150)
    public int sceMgr_driver_C0B28CED() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC3C8C1FC, version = 150)
    public int sceMgr_driver_C3C8C1FC() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCEE87932, version = 150)
    public int sceMgr_driver_CEE87932() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCF1EC2ED, version = 150)
    public int sceMgr_driver_CF1EC2ED() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDA7D8C82, version = 150)
    public int sceMgr_driver_DA7D8C82() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE6A203F8, version = 150)
    public int sceMgr_driver_E6A203F8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEA241D32, version = 150)
    public int sceMgr_driver_EA241D32() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEBED61A7, version = 150)
    public int sceMgr_driver_EBED61A7() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEDEAAA00, version = 150)
    public int sceMgr_driver_EDEAAA00() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFDF97B95, version = 150)
    public int sceMgr_driver_FDF97B95() {
    	return 0;
    }
}
