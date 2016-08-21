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
    private int parentalControl = PARENTAL_CONTROL_ENABLED;
    private int userAge = 13;

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
    public int sceNpGetNpId(TPointer unknown) {
    	// Unknown structure of 36 bytes
    	unknown.memset((byte) 0, 36);

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
    public int sceNpGetContentRatingFlag(@CanBeNull TPointer32 unknown1, @CanBeNull TPointer32 unknown2) {
    	unknown1.setValue(parentalControl);
    	unknown2.setValue(userAge);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7E0864DF, version = 150)
    public int sceNpGetUserProfile(TPointer unknown) {
    	// Unknown structure of 216 bytes
    	unknown.clear(216);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4B5C71C8, version = 150)
    public int sceNpGetOnlineId(TPointer unknown) {
    	// Unknown structure of 20 bytes
    	unknown.clear(20);

    	return 0;
    }
}