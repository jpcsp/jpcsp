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

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import org.apache.log4j.Logger;

public class sceNp extends HLEModule {
    public static Logger log = Modules.getLogger("sceNp");
    public static final int PARENTAL_CONTROL_DISABLED = 0;
    public static final int PARENTAL_CONTROL_ENABLED = 1;
    public int parentalControl = PARENTAL_CONTROL_ENABLED;
    public int userAge = 13;
    public String onlineId = "DummyOnlineId";
    public String avatarUrl = "http://DummyAvatarUrl";
    protected boolean initialized;

	@Override
	public void start() {
		initialized = false;
		super.start();
	}

    /**
     * Initialization.
     * 
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x857B47D3, version = 150, checkInsideInterrupt = true)
    public int sceNpInit() {
    	// No parameters
    	initialized = true;

    	return 0;
    }

    /**
     * Termination.
     * 
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x37E1E274, version = 150, checkInsideInterrupt = true)
    public int sceNpTerm() {
    	// No parameters
    	initialized = false;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x633B5F71, version = 150)
    public int sceNpGetNpId(TPointer buffer) {
    	// The first 20 bytes are the onlineId
    	buffer.setStringNZ(0, 20, onlineId);
    	// The next 16 bytes are unknown
    	buffer.clear(20, 16);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA0BE3C4B, version = 150)
    public int sceNpGetAccountRegion(TPointer unknown1, TPointer32 unknown2) {
    	// Unknown structure of 3 bytes
    	unknown1.setValue8(0, (byte) 0);
    	unknown1.setValue8(1, (byte) 0);
    	unknown1.setValue8(2, (byte) 0);

    	unknown2.setValue(0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBB069A87, version = 150)
    public int sceNpGetContentRatingFlag(@CanBeNull TPointer32 parentalControlAddr, @CanBeNull TPointer32 userAgeAddr) {
    	parentalControlAddr.setValue(parentalControl);
    	userAgeAddr.setValue(userAge);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7E0864DF, version = 150)
    public int sceNpGetUserProfile(TPointer buffer) {
    	// The first 20 bytes are the onlineId
    	buffer.setStringNZ(0, 20, onlineId);
    	// The next 16 bytes are unknown
    	buffer.clear(20, 16);
    	// The next 127 bytes are the avatar URL
    	buffer.setStringNZ(36, 128, avatarUrl);
    	// The next 52 bytes are unknown
    	buffer.clear(164, 52);
    	// Total size 216 bytes

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4B5C71C8, version = 150)
    public int sceNpGetOnlineId(TPointer buffer) {
    	buffer.setStringNZ(0, 20, onlineId);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1D60AE4B, version = 150)
    public int sceNpGetChatRestrictionFlag() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCDCC21D3, version = 150)
    public int sceNpGetMyLanguages() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x02CA8CAA, version = 150)
    public int sceNp_02CA8CAA() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0B8BEE48, version = 150)
    public int sceNp_0B8BEE48() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1916432C, version = 150)
    public int sceNp_1916432C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2D8E93D2, version = 150)
    public int sceNp_2D8E93D2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2E4769C3, version = 150)
    public int sceNp_2E4769C3() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4556A257, version = 150)
    public int sceNp_4556A257() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4B09907A, version = 150)
    public int sceNp_4B09907A() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5FA879D8, version = 150)
    public int sceNp_5FA879D8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x60A4791A, version = 150)
    public int sceNp_60A4791A() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x64F72B22, version = 150)
    public int sceNp_64F72B22() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x66B86876, version = 150)
    public int sceNp_66B86876() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x67BCF9E3, version = 150)
    public int sceNp_67BCF9E3() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6999EDC4, version = 150)
    public int sceNp_6999EDC4() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6CBB0614, version = 150)
    public int sceNp_6CBB0614() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9A264EF2, version = 150)
    public int sceNp_9A264EF2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9B87B19B, version = 150)
    public int sceNp_9B87B19B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB2BFADB2, version = 150)
    public int sceNp_B2BFADB2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB4063F7A, version = 150)
    public int sceNp_B4063F7A() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB819A0C8, version = 150)
    public int sceNp_B819A0C8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC0B3616C, version = 150)
    public int sceNp_C0B3616C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC250A650, version = 150)
    public int sceNp_C250A650() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC48F2847, version = 150)
    public int sceNp_C48F2847() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCF83CC3B, version = 150)
    public int sceNp_CF83CC3B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE24DA399, version = 150)
    public int sceNp_E24DA399() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE41288E7, version = 150)
    public int sceNp_E41288E7() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE90DFBC4, version = 150)
    public int sceNp_E90DFBC4() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF2B95034, version = 150)
    public int sceNp_F2B95034() {
    	return 0;
    }
}