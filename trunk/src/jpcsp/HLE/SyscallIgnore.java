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
package jpcsp.HLE;

/**
 *
 * @author George
 */
public enum SyscallIgnore {
	_sceKernelGetCpuClockCounterHigh(0xE9E652A9),
	_sceKernelGetCpuClockCounterLow(0x2DC9709B),
	InterruptManagerForKernel_15894D0B(0x15894D0B),
	InterruptManagerForKernel_43A7BBDC(0x43A7BBDC),
	InterruptManagerForKernel_53991063(0x53991063),
	Kprintf(0x84F370BC),
	LoadExecForUser_362A956B(0x362A956B),
	QueryInterruptManCB(0x00B6B0F3),
	RegCloseRegistry(0xfa8a5739),
	RegExit(0x9b25edf1),
	RegFlushRegistry(0x39461b4d),
	RegisterContextHooks(0x2CD783A1),
	RegOpenRegistry(0x92e41280),
	RegRemoveRegistry(0xdeda92bf),
	ReleaseContextHooks(0x55242A8B),
	ReturnToThread(0x43CD40EF),
	SaveThreadContext(0x85F7766D),
	sceAtracSetAA3DataAndGetID(0x5622B7C1),
	sceAudiocodec_59176A0F(0x59176A0F),
	sceAudiocodec_6CD2A861(0x6CD2A861),
	sceAudiocodecDecode(0x70A703F8),
	sceAudiocodecGetEDRAM(0x3A20A200),
	sceAudiocodecGetInfo(0x8ACA11D5),
	sceAudiocodeCheckNeedMem(0x9D3F790C),
	sceAudiocodecInit(0x5B37EB1D),
	sceAudiocodecReleaseEDRAM(0x29681260),
	sceCccSetTable(0xB4D1CBBF),
	sceDisplayDisable(0x681EE6A7),
	sceDisplayEnable(0x432D133F),
	sceDisplaySetBrightness(0x9E3C6DC6),
	sceHprmEnd(0x588845DA),
	sceHttpAbortRequest(0xc10b6bd9),
	sceHttpAddExtraHeader(0x3eaba285),
	sceHttpCreateConnectionWithURL(0xcdf8ecb9),
	sceHttpCreateRequestWithURL(0xb509b09e),
	sceHttpCreateTemplate(0x9b1f1f36),
	sceHttpDeleteConnection(0x5152773b),
	sceHttpDeleteHeader(0x15540184),
	sceHttpDeleteRequest(0xa5512e01),
	sceHttpDeleteTemplate(0xfcf8c055),
	sceHttpDisableAuth(0xae948fee),
	sceHttpDisableCache(0xccbd167a),
	sceHttpDisableCookie(0x0b12abfb),
	sceHttpDisableRedirect(0x1a0ebb69),
	sceHttpEnableKeepAlive(0x78a0d3ec),
	sceHttpEnd(0xd1c8945e),
	sceHttpEndCache(0x78b54c09),
	sceHttpGetAllHeader(0xdb266ccf),
	sceHttpGetContentLength(0x0282A3BD),
	sceHttpGetNetworkErrno(0xd081ec8f),
	sceHttpGetNetworkPspError(0x2255551E),
	sceHttpGetStatusCode(0x4cc7d78f),
	sceHttpInit(0xab1abe07),
	sceHttpInitCache(0xa6800c34),
	sceHttpLoadSystemCookie(0xF1657B22),
	sceHttpReadData(0xedeeb999),
	sceHttpSaveSystemCookie(0x76D1363B),
	sceHttpsDisableOption(0xB3FAF831),
	sceHttpsEnd(0xf9d8eb63),
	sceHttpSendRequest(0xbb70706f),
	sceHttpSetAuthInfoCB(0x2a6c3296),
	sceHttpSetConnectTimeOut(0x8ACD1F73),
	sceHttpSetMallocFunction(0xF49934F6),
	sceHttpSetRecvTimeOut(0x1F0FC3E3),
	sceHttpSetRedirectCallback(0xA4496DE5),
	sceHttpSetResolveRetry(0x03D9526F),
	sceHttpSetResolveTimeOut(0x47940436),
	sceHttpSetSendTimeOut(0x9988172D),
    sceHttpEnableRedirect(0x0809c831),
	sceHttpsGetSslError(0xAB1540D5),
	sceHttpsInit(0xE4D21302),
	sceHttpsInitWithPath(0x68ab0f86),
	sceHttpsLoadDefaultCert(0x87797bdd),
    sceHttpsEnableOption(0xbac31bf1),
	sceImposeGetParam(0x531C9778),
	sceImposeSetParam(0x810FB7FB),
	sceKernelCallSubIntrHandler(0xCDC86B64),
	sceKernelCallUserIntrHandler(0xF4454E44),
	sceKernelCpuEnableIntr(0x02314986),
	sceKernelDipswAll(0xD636B827),
	sceKernelDisableIntr(0xD774BA45),
	sceKernelEnableIntr(0x4D6E7305),
	sceKernelGetCpuClockCounter(0x30C08374),
	sceKernelGetCpuClockCounterWide(0x35634A64),
	sceKernelGetInterruptExitCount(0x468BC716),
	sceKernelGetUserIntrStack(0xD6878EB6),
	sceKernelIsInterruptOccurred(0x02475AAF),
	sceKernelIsIntrContext(0xFE28C6D9),
	sceKernelQuerySystemCall(0x8B61808B),
	sceKernelRegisterDebuggerIntrHandler(0x272766F8),
	sceKernelRegisterDebugPutchar(0xE146606D),
	sceKernelRegisterDefaultExceptionHandler(0x565C0B0E),
	sceKernelRegisterIntrHandler(0x58DD8978),
	sceKernelRegisterKprintfHandler(0x7CEB2C09),
	sceKernelRegisterSystemCallTable(0xF4D443F3),
	sceKernelReleaseDebuggerIntrHandler(0xB386A459),
	sceKernelReleaseIntrHandler(0xF987B1F0),
	sceKernelResumeIntr(0x494D6D2B),
	sceKernelSetCompiledSdkVersion370(0x342061E5),
	sceKernelSetCompiledSdkVersion380_390(0x315AD3A0),
	sceKernelSetCompiledSdkVersion395(0xEBD5C3E6),
	sceKernelSetIntrLevel(0xB5A15B30),
	sceKernelSetPrimarySyscallHandler(0x0FC68A56),
	sceKernelSuspendIntr(0x750E2507),
	sceMpegAvcCopyYCbCr(0x0558B075),
	sceMpegbase_driver_0530BE4E(0x0530BE4E),
	sceMpegBaseCscAvc(0x91929A21),
	sceMpegBaseCscInit(0x492B5E4B),
	sceMpegBasePESpacketCopy(0xBEA18F91),
	sceMpegGetUserdataAu(0x01977054),
	sceMpegQueryUserdataEsSize(0xC45C99CC),
	sceMpegRingbufferQueryPackNum(0x769BEBB6),
	sceNetAdhocMatchingAbortSendData(0xEC19337D),
	sceNetAdhocMatchingCancelTarget(0xEA3C6108),
	sceNetAdhocMatchingCancelTargetWithOpt(0x8f58bedf),
	sceNetAdhocMatchingCreate(0xCA5EDA6F),
	sceNetAdhocMatchingDelete(0xF16EAF4F),
	sceNetAdhocMatchingGetHelloOpt(0xB5D96C2A),
	sceNetAdhocMatchingGetMembers(0xC58BCD9E),
	sceNetAdhocMatchingGetPoolMaxAlloc(0x40F8F435),
	sceNetAdhocMatchingInit(0x2A2A1E07),
	sceNetAdhocMatchingSelectTarget(0x5E3D4B79),
	sceNetAdhocMatchingSendData(0xF79472D7),
	sceNetAdhocMatchingSetHelloOpt(0xB58E61B7),
	sceNetAdhocMatchingStart(0x93EF3843),
	sceNetAdhocMatchingStop(0x32B156B3),
	sceNetAdhocMatchingTerm(0x7945ECDA),
	sceNetApctlAddHandler(0x8abadd51),
	sceNetApctlConnect(0xCFB957C6),
	sceNetApctlDelHandler(0x5963991b),
	sceNetApctlDisconnect(0x24fe91a1),
	sceNetApctlGetInfo(0x2befdf23),
	sceNetApctlGetState(0x5deac81b),
	sceNetApctlInit(0xe2f91f9b),
	sceNetApctlTerm(0xb3edd0ec),
	sceNetApDialogDummyConnect(0x3811281E),
	sceNetApDialogDummyGetState(0xCA9BE5BF),
	sceNetApDialogDummyInit(0xBB73FF67),
	sceNetApDialogDummyTerm(0xF213BE65),
	sceNetInetAccept(0xDB094E1B),
	sceNetInetBind(0x1A33F9AE),
	sceNetInetClose(0x8D7284EA),
	sceNetInetCloseWithRST(0x805502DD),
	sceNetInetConnect(0x410B34AA),
	sceNetInetGetErrno(0xFBABE411),
	sceNetInetGetpeername(0xE247B6D6), // 1.00+
	sceNetInetGetPspError(0x8CA3A97E), // 1.00+
	sceNetInetGetsockname(0x162E6FD5), // 1.00+
	sceNetInetGetsockopt(0x4A114C7C), // 1.00+
	sceNetInetGetTcpcbstat(0xB3888AD4), // 1.00+
	sceNetInetGetUdpcbstat(0x39B0C7D3), // 1.00+
	sceNetInetInetAddr(0xB75D5B0A), // 1.00+
	sceNetInetInetAton(0x1BDF5D13), // 1.00+
	sceNetInetInetNtop(0xD0792666), // 1.00+
	sceNetInetInetPton(0xE30B8C19), // 1.00+
	sceNetInetInit(0x17943399), // 1.00+
	sceNetInetListen(0xD10A1A7A), // 1.00+
	sceNetInetPoll(0xFAABB1DD), // 1.00+
	sceNetInetRecv(0xCDA85C99), // 1.00+
	sceNetInetRecvfrom(0xC91142E4), // 1.00+
	sceNetInetRecvmsg(0xEECE61D2), // fw 2.71 or less?
	sceNetInetSelect(0x5BE8D595), //2.50+
	sceNetInetSend(0x7AA671BC),//2.00 +
	sceNetInetSendmsg(0x774E36F4),//2.00+
	sceNetInetSendto(0x05038FC7),//1.00+
	sceNetInetSetsockopt(0x2FE71FE7),//1.00+
	sceNetInetShutdown(0x4CFE4E56),//1.00+
	sceNetInetSocket(0x8B7B220F),//1.00+
	sceNetInetSocketAbort(0x80A21ABD),//1.00+
	sceNetInetTerm(0xA9ED66B9),//1.00+
	sceNetResolverCreate(0x244172af),//1.00+
	sceNetResolverDelete(0x94523e09),//1.00+
	sceNetResolverInit(0xf3370e61),//1.00+
	sceNetResolverStartAtoN(0x629E2FB7),//1.00+
	sceNetResolverStartNtoA(0x224c5f44),//1.00+
	sceNetResolverStop(0x808F6063),//1.00+
	sceNetResolverTerm(0x6138194a),//1.00+
	sceParseHttpResponseHeader(0xAD7BFDEF),//1.00+
	sceParseHttpStatusLine(0x8077a433),//1.00+
	sceRegCloseCategory(0x0cae832b),//1.00+
	sceRegCreateKey(0x57641a81),//1.00+
	sceRegFlushCategory(0x0d69bf40),//1.00+
	sceRegGetKeyInfo(0xd4475aa8),//1.00+
	sceRegGetKeys(0x2d211135),//1.00+
	sceRegGetKeysNum(0x2c0db9dd),//1.00+
	sceRegGetKeyValue(0x28a8e98a),//1.00+
	sceRegGetKeyValueByName(0x30be0259),//1.00+
	sceRegKickBackDiscover(0xc5768d02),//1.00+
	sceRegOpenCategory(0x1d8a762e),//1.00+
	sceRegRemoveCategory(0x4ca16893),//1.00+
	sceRegRemoveKey(0x3615bc87),//1.00+
	sceRegSetKeyValue(0x17768e14),//1.00+
	sceSslEnd(0x191cdeff),//1.00+
	sceSslInit(0x957ecbe2),//1.00+
	sceSysconCtrlHRPower(0x44439604),//1.00+
	sceSysregMeBusClockDisable(0x158AD4FC), //1.00+
	sceSysregMeBusClockEnable(0x44F6CDA7),//1.00+
	sceSysregUartIoEnable(0x7FD7A631),//1.00+
	sceUriParse(0x568518c9), //3.90+ or lower
	sceUsbstorBootSetCapacity(0xE58818A8), //1.50+
	sceUtility_private_17CB4D96(0x17cb4d96), //1.50+
	sceUtility_private_19461966(0x19461966), //1.50+
	sceUtility_private_1DFA62EF(0x1dfa62ef), //1.50+
	sceUtility_private_4405BA38(0x4405ba38), //1.50+
	sceUtility_private_5FF96ED3(0x5ff96ed3), //1.50+
	sceUtility_private_9C9DD5BC(0x9c9dd5bc), //1.50+
	sceUtility_private_EE7AC503(0xee7ac503), //1.50+
	sceUtility_private_EF5BC2D1(0xef5bc2d1), //1.50+
	sceUtilityCopyNetParam(0xfb0c4840), //1.50+
	sceUtilityCreateNetParam(0x072debf2), //1.50+
	sceUtilityDeleteNetParam(0x9ce50172), //1.50+
	sceUtilityDialogGetParam(0x4f2206bc), //1.50+
	sceUtilityDialogGetSpeed(0xe01fe32a), //1.50+
	sceUtilityDialogGetType(0xb222e887), //1.00 - 2.50
	sceUtilityDialogLoadModule(0xa5168a5d), //1.50+
	sceUtilityDialogPowerLock(0x3ceae1df), //1.50+
	sceUtilityDialogPowerUnlock(0x56bedca4), //1.50+
	sceUtilityDialogSetStatus(0x680c0ea8), //1.50+
	sceUtilityDialogSetThreadId(0x6f923bd3), //1.50+
	sceUtilityGetNetParamLatestID(0x4FED24D8), //1.50+
	sceUtilityInstallGetStatus(0xC4700FA3), // 2.00+
	sceUtilityInstallInitStart(0x1281DA8E), // 2.00+
	sceUtilityInstallShutdownStart(0x5EF1C24A), // 2.00+
	sceUtilityInstallUpdate(0xA03D29BA), // 2.00+
	sceUtilityLoadUsbModule(0x0D5BC6D2), // 2.00+
	sceUtilitySetNetParam(0xfc4516f3), // 2.00+
	sceUtilityUnloadUsbModule(0xF64910F0), // 2.71+
	sceVaudioChRelease(0x67585dfd), // 3.95+
	sceVaudioChReserve(0x03b6807d), // 3.95+
	sceVaudioOutputBlocking(0x8986295e), // 3.95+
	sceVaudioSetEffectType(0x346fbe94), // 3.95+
	sceVideocodecDecode(0xDBA273FA), // 2.71+
	sceVideocodecDelete(0x307E6E1C), // 2.71+
	sceVideocodecGetEDRAM(0x2D31F5B1), // 2.71+
	sceVideocodecGetVersion(0x26927D19), // 5.00+
	sceVideocodecInit(0x17099F0A), // 5.00+ sceKernelSetCompiledSdkVersionXXX
	sceVideocodecOpen(0xC01EC829), // 5.00+
	sceVideocodecReleaseEDRAM(0x4F160BF4), // 1.00+
	sceVideocodecScanHeader(0x2F385E7F), // 2.00+
	sceVideocodecSetMemory(0x745A7B7A), // 1.00+
	sceVideocodecStop(0xA2F0564E), // 1.00+
	SircsSend(0x71eef62d), // 1.00+
	SupportIntr(0x0E224D66), // 1.00+
	SysMemUserForUser_35669D4C(0x35669D4C), // 1.00 to 3.52, gone in 3.95+
	SysMemUserForUser_91DE343C(0x91DE343C), // 1.00+
	SysMemUserForUser_D8DE5C1E(0xD8DE5C1E), // 1.00+
	unknown_CC9ADCF8(0xCC9ADCF8), // 1.00+
	UnSupportIntr(0x27BC9A45), // 1.00+
	UsbActivate(0x586db82c), // 1.00+
	UsbDeactivate(0xc572a9c8), // 1.00+
	UsbGetDrvList(0x4e537366), // 1.00+
	UsbGetDrvState(0x112cc951), // 3.71+? homebrew psplayer 2.0 (mp3 player)
	UsbGetState(0xc21645a4), // 2.71+
	UsbStart(0xae5de6af), // 2.71+
	UsbStop(0xc2464fa0), // 2.71+
	UsbWaitCancel(0x1c360735), // 2.71+
	UsbWaitState(0x5be0e002), // 2.00+
	WlanDevAttach(0x482cae9a), // 2.00+
	WlanDevDetach(0xc9a8cab7), // 1.50+
	WlanDevGetStateGPIO(0x05fe320c), // 1.00+
	WlanDevIsGameMode(0x5e7c8d94), // 1.00+
	WlanDevIsPowerOn(0x93440b11), // 1.00+
	WlanDevSetGPIO(0x7ff54bd2), // 1.00+
	WlanDrv_lib_19E51F54(0x19e51f54), // 1.00+
	WlanDrv_lib_2D0FAE4E(0x2d0fae4e), // 2.71+
	WlanDrv_lib_40B0AA4A(0x40b0aa4a), // 1.50+
	WlanDrv_lib_4C14BACA(0x4c14baca), // 2.71+
	WlanDrv_lib_56F467CA(0x56f467ca), // 2.50+
	WlanDrv_lib_5BAA1FE5(0x5baa1fe5), // 2.00+
	WlanDrv_lib_81579D36(0x81579d36), // 2.00+
	WlanDrv_lib_8D5F551B(0x8d5f551b), // 2.00+
	WlanDrv_lib_FE8A0B46(0xfe8a0b46), // 6.00+ sceKernelSetCompiledSdkVersionXXX
	WlanGPPrevEstablishActive(0x5ed4049a),
	WlanGPRecv(0xa447103a),
	WlanGPRegisterCallback(0x9658c9f7),
	WlanGPSend(0xb4d7cb74),
	WlanGPUnRegisterCallback(0x4c7f62e0),

	// CFW
	kuKernelLoadModule(0x4C25EA72),
	systemctrl_user_577AF198(0x577AF198),
	systemctrl_user_75643FCA(0x75643FCA),
	systemctrl_user_ABA7F1B0(0xABA7F1B0),
	sctrlSEGetConfig(0x16C3B7EE),
	systemctrl_user_85B520C6(0x85B520C6),

	// Module magic
	module_bootstart(0xD3744BE0),
	module_reboot_before(0x2F064FA6),
	module_reboot_phase(0xADF12745),
	module_start(0xD632ACDB),
	module_stop_1(0xCEE8593C),
	module_stop_2(0xF01D73A7),
	module_0F7C276C(0x0F7C276C),

	hleDummy(0xcdcdcdcd);

	private int nid;

	SyscallIgnore(int nid) {
		this.nid = nid;
	}

	public int getNID() {
		return nid;
	}

	public int getSyscall() {
		return ordinal() + 0x2000;
	}
}