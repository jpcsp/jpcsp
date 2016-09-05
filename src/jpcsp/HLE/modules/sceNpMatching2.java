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

import static jpcsp.HLE.kernel.types.SceKernelThreadInfo.THREAD_CALLBACK_USER_DEFINED;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;
import jpcsp.HLE.kernel.types.pspBaseCallback;
import jpcsp.util.Utilities;

// Information based on
// https://github.com/RPCS3/rpcs3/blob/master/rpcs3/Emu/PSP2/Modules/sceNpMatching.cpp
public class sceNpMatching2 extends HLEModule {
    public static Logger log = Modules.getLogger("sceNpMatching2");
    private static final String idContextPurpose = "sceNpMatching2Context";
    private int defaultRequestCallbackFunction;
    private int defaultRequestCallbackArgument;
    private int defaultRequestTimeout;
    private int defaultRequestAppReqId;
    private int defaultRoomEventCallbackFunction;
    private int defaultRoomEventCallbackArgument;
    private int defaultRoomMessageCallbackFunction;
    private int defaultRoomMessageCallbackArgument;
    private Map<Integer, MatchingContext> contextMap = new HashMap<Integer, sceNpMatching2.MatchingContext>();

    private static class MatchingContext {
    	private boolean started;

		public boolean isStarted() {
			return started;
		}

		public void setStarted(boolean started) {
			this.started = started;
		}
    }

    private void notifyRequestCallback(int ctxId) {
    	MatchingContext context = contextMap.get(ctxId);
    	if (context == null || !context.isStarted() || defaultRequestCallbackFunction == 0) {
    		return;
    	}

		pspBaseCallback requestCallback = Modules.ThreadManForUserModule.hleKernelCreateCallback(defaultRequestCallbackFunction, 6);
		if (Modules.ThreadManForUserModule.hleKernelRegisterCallback(THREAD_CALLBACK_USER_DEFINED, requestCallback)) {
			int reqId = 0x14567; // Dummy value for debugging
			int event = 0x9876; // Dummy value for debugging
			int data = 0xDEADBEEF; // Dummy value for debugging
			int errorCode = 0;
			requestCallback.setArgument(0, ctxId);
			requestCallback.setArgument(1, reqId);
			requestCallback.setArgument(2, event);
			requestCallback.setArgument(3, errorCode);
			requestCallback.setArgument(4, data);
			requestCallback.setArgument(5, defaultRequestCallbackArgument);
			Modules.ThreadManForUserModule.hleKernelNotifyCallback(THREAD_CALLBACK_USER_DEFINED, requestCallback);
		}
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2E61F6E1, version = 150)
    public int sceNpMatching2Init(int poolSize, int threadPriority, int cpuAffinityMask, int threadStackSize) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8BF37D8C, version = 150)
    public int sceNpMatching2Term() {
    	// No parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x12C5A111, version = 150)
    public int sceNpMatching2GetRoomDataExternalList() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1421514B, version = 150)
    public int sceNpMatching2SetDefaultRoomEventOptParam(int ctxId, TPointer optParam) {
    	int callbackFunction = optParam.getValue32(0);
    	int callbackArgument = optParam.getValue32(4);
    	boolean unknownFlag1 = optParam.getValue32(8) != 0;
    	boolean unknownFlag2 = optParam.getValue32(12) != 0;
    	boolean unknownFlag3 = optParam.getValue32(16) != 0;
    	boolean unknownFlag4 = optParam.getValue32(20) != 0;

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNpMatching2SetDefaultRoomEventOptParam callbackFunction=0x%08X, callbackArgument=0x%X, unknownFlag1=%b, unknownFlag2=%b, unknownFlag3=%b, unknownFlag4=%b", callbackFunction, callbackArgument, unknownFlag1, unknownFlag2, unknownFlag3, unknownFlag4));
    	}

    	defaultRoomEventCallbackFunction = callbackFunction;
    	defaultRoomEventCallbackArgument = callbackArgument;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x190FF903, version = 150)
    public int sceNpMatching2ContextStart(int ctxId) {
    	MatchingContext context = contextMap.get(ctxId);
    	if (context == null) {
    		return -1;
    	}

    	context.setStarted(true);

    	notifyRequestCallback(ctxId);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x22F38DAF, version = 150)
    public int sceNpMatching2GetMemoryStat() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2B3892FC, version = 150)
    public int sceNpMatching2ContextStop(int ctxId) {
    	MatchingContext context = contextMap.get(ctxId);
    	if (context == null) {
    		return -1;
    	}

    	context.setStarted(false);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3892E9A6, version = 150)
    public int sceNpMatching2SignalingGetConnectionInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3DE70241, version = 150)
    public int sceNpMatching2DestroyContext(int ctxId) {
    	if (!SceUidManager.releaseId(ctxId, idContextPurpose)) {
    		return -1;
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x495E97BD, version = 150)
    public int sceNpMatching2GrantRoomOwner() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4EE3A8EC, version = 150)
    public int sceNpMatching2GetServerInfo(int ctxId, TPointer16 serverIdAddr, TPointer32 unknown1, TPointer32 unknown2) {
    	int serverId = serverIdAddr.getValue();

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNpMatching2GetServerInfo serverId=0x%X, unknown1: %s, unknown2: %s", serverId, Utilities.getMemoryDump(unknown1.getAddress(), 16), Utilities.getMemoryDump(unknown2.getAddress(), 20)));
    	}

    	unknown2.setValue(0x00010000);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5030CC53, version = 150)
    public int sceNpMatching2CreateContext(TPointer communicationId, TPointer passPhrase, TPointer16 ctxId, int unknown) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNpMatching2CreateContext communicationId=%s, passPhrase=%s", Utilities.getMemoryDump(communicationId.getAddress(), 12), Utilities.getMemoryDump(passPhrase.getAddress(), 128)));
    	}

    	// Returning a ctxId in range [1..7]
    	int uid = SceUidManager.getNewId(idContextPurpose, 1, 7);
    	if (uid == SceUidManager.INVALID_ID) {
    		return -1;
    	}

    	contextMap.put(uid, new MatchingContext());

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNpMatching2CreateContext returning 0x%X", uid));
    	}
    	ctxId.setValue(uid);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x55F7837F, version = 150)
    public int sceNpMatching2SendRoomChatMessage() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5C7DB6A4, version = 150)
    public int sceNpMatching2GetRoomMemberDataInternalList() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x631682CC, version = 150)
    public int sceNpMatching2SetDefaultRequestOptParam(int ctxId, TPointer optParam) {
    	int callbackFunction = optParam.getValue32(0);
    	int callbackArgument = optParam.getValue32(4);
    	int timeout = optParam.getValue32(8);
    	int appReqId = optParam.getValue16(12);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNpMatching2SetDefaultRequestOptParam callbackFunction=0x%08X, callbackArgument=0x%X, timeout=0x%X, appReqId=0x%X", callbackFunction, callbackArgument, timeout, appReqId));
    	}

    	defaultRequestCallbackFunction = callbackFunction;
    	defaultRequestCallbackArgument = callbackArgument;
    	defaultRequestTimeout = timeout;
    	defaultRequestAppReqId = appReqId;

    	notifyRequestCallback(ctxId);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6D6D0C75, version = 150)
    public int sceNpMatching2SignalingGetConnectionStatus() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7BBFC427, version = 150)
    public int sceNpMatching2JoinRoom() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7D1D5F5E, version = 150)
    public int sceNpMatching2SetUserInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7DAA8A90, version = 150)
    public int sceNpMatching2SetRoomMemberDataInternal() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x80F61558, version = 150)
    public int sceNpMatching2GetRoomMemberIdListLocal() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x81C13E6D, version = 150)
    public int sceNpMatching2SearchRoom() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8CD109E7, version = 150)
    public int sceNpMatching2SignalingGetPeerNetInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9462C05A, version = 150)
    public int sceNpMatching2SignalingCancelPeerNetInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x97529ECC, version = 150)
    public int sceNpMatching2KickoutRoomMember() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9A67F5D0, version = 150)
    public int sceNpMatching2SetSignalingOptParam() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA3C298D1, version = 150)
    public int sceNpMatching2RegisterSignalingCallback(int ctxId, TPointer callbackFunction, int callbackArgument) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA53E7C69, version = 150)
    public int sceNpMatching2GetWorldInfoList(int ctxId, TPointer16 serverIdAddr, @CanBeNull TPointer optParam, TPointer32 assignedReqId) {
    	if (optParam.isNotNull()) {
	    	int callbackFunction = optParam.getValue32(0);
	    	int callbackArgument = optParam.getValue32(4);
	    	int timeout = optParam.getValue32(8);
	    	int appReqId = optParam.getValue16(12);

	    	if (log.isDebugEnabled()) {
	    		log.debug(String.format("sceNpMatching2GetWorldInfoList callbackFunction=0x%08X, callbackArgument=0x%X, timeout=0x%X, appReqId=0x%X", callbackFunction, callbackArgument, timeout, appReqId));
	    	}
    	}

    	int serverId = serverIdAddr.getValue();

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNpMatching2GetWorldInfoList serverId=0x%X", serverId));
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA5775DBF, version = 150)
    public int sceNpMatching2GetRoomMemberDataInternal() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAAD0946A, version = 150)
    public int sceNpMatching2CreateJoinRoom() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC7E72EC5, version = 150)
    public int sceNpMatching2GetSignalingOptParamLocal() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC870535A, version = 150)
    public int sceNpMatching2LeaveRoom() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC8FC5D41, version = 150)
    public int sceNpMatching2GetUserInfoList() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD13491AB, version = 150)
    public int sceNpMatching2SetDefaultRoomMessageOptParam(int ctxId, TPointer optParam) {
    	int callbackFunction = optParam.getValue32(0);
    	int callbackArgument = optParam.getValue32(4);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceNpMatching2SetDefaultRoomMessageOptParam callbackFunction=0x%08X, callbackArgument=0x%X", callbackFunction, callbackArgument));
    	}

    	defaultRoomMessageCallbackFunction = callbackFunction;
    	defaultRoomMessageCallbackArgument = callbackArgument;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD7D4AEB2, version = 150)
    public int sceNpMatching2SetRoomDataExternal() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDFEDB642, version = 150)
    public int sceNpMatching2SignalingGetPeerNetInfoResult() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE313E586, version = 150)
    public int sceNpMatching2GetRoomDataInternal() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE6C93DBD, version = 150)
    public int sceNpMatching2SetRoomDataInternal() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEF683F4F, version = 150)
    public int sceNpMatching2GetRoomDataInternalLocal() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF22C7ADC, version = 150)
    public int sceNpMatching2GetRoomMemberDataInternalLocal() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF47342FC, version = 150)
    public int sceNpMatching2GetServerIdListLocal(int ctxId, TPointer16 serverIds, int maxServerIds) {
    	// Return dummy values for debugging
    	for (int i = 0; i < maxServerIds; i++) {
    		serverIds.setValue(i * 2, i + 0x1234);
    	}

    	return maxServerIds;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF739BE92, version = 150)
    public int sceNpMatching2GetRoomPasswordLocal() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF940D9AD, version = 150)
    public int sceNpMatching2SendRoomMessage() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFADBA9DB, version = 150)
    public int sceNpMatching2AbortRequest() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFBF494C0, version = 150)
    public int sceNpMatching2GetRoomMemberDataExternalList() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFF32EA05, version = 150)
    public int sceNpMatching2SignalingGetLocalNetInfo() {
    	return 0;
    }
}
