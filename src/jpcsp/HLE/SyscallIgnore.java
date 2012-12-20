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
	sceDisplayDisable(0x681EE6A7),
	sceDisplayEnable(0x432D133F),
	sceDisplaySetBrightness(0x9E3C6DC6),
	sceHprmEnd(0x588845DA),
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
	sceKernelSetIntrLevel(0xB5A15B30),
	sceKernelSetPrimarySyscallHandler(0x0FC68A56),
	sceKernelSuspendIntr(0x750E2507),
	sceMpegAvcCopyYCbCr(0x0558B075),
	sceMpegbase_driver_0530BE4E(0x0530BE4E),
	sceMpegBaseCscAvc(0x91929A21),
	sceMpegBaseCscInit(0x492B5E4B),
	sceMpegBasePESpacketCopy(0xBEA18F91),
	sceNetApDialogDummyConnect(0x3811281E),
	sceNetApDialogDummyGetState(0xCA9BE5BF),
	sceNetApDialogDummyInit(0xBB73FF67),
	sceNetApDialogDummyTerm(0xF213BE65),
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
	sceUtilitySetNetParam(0xfc4516f3), // 2.00+
	sceUtilityUnloadUsbModule(0xF64910F0), // 2.71+
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
	unknown_CC9ADCF8(0xCC9ADCF8), // 1.00+
	UnSupportIntr(0x27BC9A45), // 1.00+
	WlanDevAttach(0x482cae9a), // 2.00+
	WlanDevDetach(0xc9a8cab7), // 1.50+
	WlanDevGetStateGPIO(0x05fe320c), // 1.00+
	WlanDevIsGameMode(0x5e7c8d94), // 1.00+
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