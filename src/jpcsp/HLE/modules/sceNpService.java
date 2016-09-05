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
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelErrors;

import org.apache.log4j.Logger;

public class sceNpService extends HLEModule {
    public static Logger log = Modules.getLogger("sceNpService");

    private boolean initialized;
    private int npManagerMemSize;     // Memory allocated by the NP Manager utility.
    private int npManagerMaxMemSize;  // Maximum memory used by the NP Manager utility.
    private int npManagerFreeMemSize; // Free memory available to use by the NP Manager utility.
    private int dummyNumberFriends = 5;
    private int dummyNumberBlockList = 2;

	@Override
	public void start() {
		initialized = false;
		super.start();
	}

    protected void checkInitialized() {
    	if (!initialized) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_NPSERVICE_NOT_INIT);
    	}
    }

	/**
     * Initialization.
     * 
     * @param poolSize
     * @param stackSize
     * @param threadPriority
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x0F8F5821, version = 150, checkInsideInterrupt = true)
    public int sceNpServiceInit(int poolSize, int stackSize, int threadPriority) {
        npManagerMemSize = poolSize;
        npManagerMaxMemSize = poolSize / 2;    // Dummy
        npManagerFreeMemSize = poolSize - 16;  // Dummy.

        initialized = true;

        return 0;
    }

    /**
     * Termination.
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x00ACFAC3, version = 150, checkInsideInterrupt = true)
    public int sceNpServiceTerm() {
    	initialized = false;
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x250488F9, version = 150, checkInsideInterrupt = true)
    public int sceNpServiceGetMemoryStat(TPointer32 memStatAddr) {
    	checkInitialized();

    	memStatAddr.setValue(0, npManagerMemSize);
        memStatAddr.setValue(4, npManagerMaxMemSize);
        memStatAddr.setValue(8, npManagerFreeMemSize);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBE22EEA3, version = 150)
    public int sceNpRosterCreateRequest() {
    	int requestId = 0x1234; // Dummy value for testing purpose

    	return requestId;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x66C64821, version = 150)
    public int sceNpRosterDeleteRequest(int requestId) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x58251346, version = 150)
    public int sceNpRosterGetFriendListEntryCount(int requestId, @CanBeNull TPointer32 options) {
    	if (options.isNotNull() && options.getValue() != 4) {
    		return -1;
    	}

    	return dummyNumberFriends;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4E851B10, version = 150)
    public int sceNpRosterGetFriendListEntry(int requestId, int numEntries, int startIndex, TPointer32 countAddr, TPointer32 numberRetrieved, TPointer buffer, @CanBeNull TPointer32 options) {
    	if (options.isNotNull() && options.getValue() != 4) {
    		return -1;
    	}

    	countAddr.setValue(dummyNumberFriends);
    	numberRetrieved.setValue(dummyNumberFriends);

    	for (int i = 0; i < dummyNumberFriends; i++) {
    		buffer.setStringNZ(i * 36, 16, String.format("DummyFriend#%d", i));
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x72A1CE0D, version = 150)
    public int sceNpRosterDeleteFriendListEntry() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x75DACB57, version = 150)
    public int sceNpRosterAcceptFriendListEntry() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x788F2B5E, version = 150)
    public int sceNpRosterAddFriendListEntry() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA01443AA, version = 150)
    public int sceNpRosterGetBlockListEntryCount(int requestId, TPointer32 options) {
    	if (options.isNotNull() && options.getValue() != 4) {
    		return -1;
    	}

    	return dummyNumberBlockList;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x506C318D, version = 150)
    public int sceNpRosterGetBlockListEntry(int requestId, int numEntries, int startIndex, TPointer32 countAddr, TPointer32 numberRetrieved, TPointer buffer, @CanBeNull TPointer32 options) {
    	if (options.isNotNull() && options.getValue() != 4) {
    		return -1;
    	}

    	countAddr.setValue(dummyNumberBlockList);
    	numberRetrieved.setValue(dummyNumberBlockList);

    	for (int i = 0; i < dummyNumberBlockList; i++) {
    		buffer.setStringNZ(i * 36, 16, String.format("DummyBlocked#%d", i));
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x174D0D24, version = 150)
    public int sceNpRosterDeleteBlockListEntry() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFC0BC8DB, version = 150)
    public int sceNpRosterAddBlockListEntry() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1DA3E950, version = 150)
    public int sceNpManagerSigninUpdateInitStart() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x168B8DE5, version = 150)
    public int sceNpManagerSigninUpdateGetStatus() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x78802D5F, version = 150)
    public int sceNpManagerSigninUpdateShutdownStart() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x268C009D, version = 150)
    public int sceNpManagerSigninUpdateAbort() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5494274B, version = 150)
    public int sceNpLookupCreateTransactionCtx() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA670D3A3, version = 150)
    public int sceNpLookupDestroyTransactionCtx() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x90E4DB6A, version = 150)
    public int sceNpLookupUserProfile() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x389A0D44, version = 150)
    public int sceNpLookupNpId() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4B4E4E71, version = 150)
    public int sceNpLookupAbortTransaction() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5F5E32AF, version = 150)
    public int sceNpRosterAbort() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA164CACC, version = 150)
    public int sceNpRosterGetFriendListMessage() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC76F55ED, version = 150)
    public int sceNpLookupTitleSmallStorage() {
    	return 0;
    }
}