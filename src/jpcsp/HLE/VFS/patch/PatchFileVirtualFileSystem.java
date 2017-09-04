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
package jpcsp.HLE.VFS.patch;

import static jpcsp.Allegrex.Common._a0;
import static jpcsp.Allegrex.Common._a1;
import static jpcsp.Allegrex.Common._a2;
import static jpcsp.Allegrex.Common._a3;
import static jpcsp.Allegrex.Common._at;
import static jpcsp.Allegrex.Common._s0;
import static jpcsp.Allegrex.Common._t0;
import static jpcsp.Allegrex.Common._t1;
import static jpcsp.Allegrex.Common._t2;
import static jpcsp.Allegrex.Common._t3;
import static jpcsp.Allegrex.Common._t4;
import static jpcsp.Allegrex.Common._t6;
import static jpcsp.Allegrex.Common._t7;
import static jpcsp.Allegrex.Common._t8;
import static jpcsp.Allegrex.Common._t9;
import static jpcsp.Allegrex.Common._v0;
import static jpcsp.Allegrex.Common._v1;
import static jpcsp.Allegrex.Common._zr;
import static jpcsp.HLE.Modules.ThreadManForKernelModule;
import static jpcsp.HLE.Modules.sceNandModule;
import static jpcsp.HLE.Modules.sceSysregModule;
import static jpcsp.HLE.modules.ThreadManForUser.JR;
import static jpcsp.HLE.modules.ThreadManForUser.MOVE;
import static jpcsp.HLE.modules.ThreadManForUser.NOP;
import static jpcsp.HLE.modules.ThreadManForUser.SYSCALL;
import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.util.LinkedList;
import java.util.List;

import jpcsp.HLE.HLEModule;
import jpcsp.HLE.VFS.AbstractProxyVirtualFileSystem;
import jpcsp.HLE.VFS.ByteArrayVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.format.Elf32Header;
import jpcsp.util.Utilities;

/**
 * Virtual file system patching/modifying files (e.g. PRX's).
 * 
 * @author gid15
 *
 */
public class PatchFileVirtualFileSystem extends AbstractProxyVirtualFileSystem {
	private static final PatchInfo[] allPatches = new PatchInfo[] {
			// sysmem.prx: disable the function MemoryProtectInit(sub_0000A2C4)
			new PrxPatchInfo("kd/sysmem.prx", 0x0000A2C4, 0x27BDFFF0, JR()),
			new PrxPatchInfo("kd/sysmem.prx", 0x0000A2C8, 0xAFB10004, MOVE(_v0, _zr)),
			// loadcore.prx used by loadCoreInit()
			new PrxPatchInfo("kd/loadcore.prx", 0x0000469C, 0x15C0FFA0, NOP()),      // Allow loading of privileged modules being not encrypted (https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L339)
			new PrxPatchInfo("kd/loadcore.prx", 0x00004548, 0x7C0F6244, NOP()),      // Allow loading of privileged modules being not encrypted (take SceLoadCoreExecFileInfo.modInfoAttribute from the ELF module info, https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L351)
			new PrxPatchInfo("kd/loadcore.prx", 0x00004550, 0x14E0002C, 0x1000002C), // Allow loading of privileged modules being not encrypted (https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L352)
			new PrxPatchInfo("kd/loadcore.prx", 0x00003D58, 0x10C0FFBE, NOP()),      // Allow linking user stub to kernel lib
			new PrxPatchInfo("kd/loadcore.prx", 0x00005D1C, 0x5040FE91, NOP()),      // Allow loading of non-encrypted modules for apiType==80 (Disable test at https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L1059)
			new PrxPatchInfo("kd/loadcore.prx", 0x00005D20, 0x3C118002, NOP()),      // Allow loading of non-encrypted modules for apiType==80 (Disable test at https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L1059)
			new PrxPatchInfo("kd/loadcore.prx", 0x00004378, 0x5120FFDB, NOP()),      // Allow loading of kernel modules being not encrypted (set "execInfo->isKernelMod = SCE_TRUE" even when "decryptMode == 0": https://github.com/uofw/uofw/blob/master/src/loadcore/loadelf.c#L118)
			// exceptionman.prx used by ExcepManInit()
			new PrxPatchInfo("kd/exceptionman.prx", 0x00000568, 0xACAF0000, NOP()), // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/exceptionman/exceptions.c#L71)
			new PrxPatchInfo("kd/exceptionman.prx", 0x0000018C, 0xAC430004, NOP()), // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/exceptionman/excep.S#L148)
			// interruptman.prx used by IntrManInit()
			new PrxPatchInfo("kd/interruptman.prx", 0x0000103C, 0xAC220008, NOP()), // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1273)
			new PrxPatchInfo("kd/interruptman.prx", 0x00001040, 0xAC200018, NOP()), // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1274)
			new PrxPatchInfo("kd/interruptman.prx", 0x00001044, 0xAC200028, NOP()), // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1275)
			// interruptman.prx used by sceKernelSuspendIntr()/sceKernelResumeIntr()
			new PrxPatchInfo("kd/interruptman.prx", 0x00001084, 0x8C220008, MOVE(_v0, _zr)), // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1297)
			new PrxPatchInfo("kd/interruptman.prx", 0x00001088, 0x8C230018, MOVE(_v1, _zr)), // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1298)
			new PrxPatchInfo("kd/interruptman.prx", 0x0000108C, 0x8C210028, MOVE(_at, _zr)), // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1299)
			new PrxPatchInfo("kd/interruptman.prx", 0x000010C0, 0xAC220008, NOP()),          // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1315)
			new PrxPatchInfo("kd/interruptman.prx", 0x000010D8, 0xAC230018, NOP()),          // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1321)
			new PrxPatchInfo("kd/interruptman.prx", 0x000010DC, 0xAC220028, NOP()),          // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/interruptman/start.S#L1322)
			// threadman.prx
			new PrxPatchInfo("kd/threadman.prx", 0x0000E674, 0xAC33000C, NOP()), // Avoid access to hardware register (*(int*)0xBC60000C = 1)
			new PrxPatchInfo("kd/threadman.prx", 0x0000E67C, 0xAC320008, NOP()), // Avoid access to hardware register (*(int*)0xBC600008 = 48)
			new PrxPatchInfo("kd/threadman.prx", 0x0000E684, 0xAC200010, NOP()), // Avoid access to hardware register (*(int*)0xBC600010 = 0)
			new PrxPatchInfo("kd/threadman.prx", 0x0000E78C, 0xAC280004, NOP()), // Avoid access to hardware register (*(int*)0xBC600004 = 0xF0000000)
			new PrxPatchInfo("kd/threadman.prx", 0x0000E798, 0xAC200000, NOP()), // Avoid access to hardware register (*(int*)0xBC600000 = 0)
			// threadman.prx reading from 0xBC600000 is equivalent to a call of sceKernelGetSystemTimeLow()
			new PrxPatchInfo("kd/threadman.prx", 0x0000ECF0, 0x3C0ABC60, SYSCALL(ThreadManForKernelModule, "hleKernelGetSystemTimeLow")), // Replace reading of hardware register *(int*)0xBC600000 with a call to ThreadManForKernel.hleKernelGetSystemTimeLow()
			new PrxPatchInfo("kd/threadman.prx", 0x0000ECF4, 0x8D4A0000, MOVE(_t2, _v0)),                                                 // Replace reading of hardware register *(int*)0xBC600000 with a call to ThreadManForKernel.hleKernelGetSystemTimeLow()
			// threadman.prx replace the implementation sceKernelGetSystemTimeLow() with a call to ThreadManForKernel.hleKernelGetSystemTimeLow()
			new PrxPatchInfo("kd/threadman.prx", 0x00010D28, 0x3C02BC60, JR()),                                                           // Replace reading of hardware register *(int*)0xBC600000 with a call to ThreadManForKernel.hleKernelGetSystemTimeLow()
			new PrxPatchInfo("kd/threadman.prx", 0x00010D2C, 0x03E00008, SYSCALL(ThreadManForKernelModule, "hleKernelGetSystemTimeLow")), // Replace reading of hardware register *(int*)0xBC600000 with a call to ThreadManForKernel.hleKernelGetSystemTimeLow()
			// threadman.prx reading from 0xBC600000 is equivalent to a call of sceKernelGetSystemTimeLow()
			new PrxPatchInfo("kd/threadman.prx", 0x000112E4, 0x8C420000, SYSCALL(ThreadManForKernelModule, "hleKernelGetSystemTimeLow")), // Replace reading of hardware register *(int*)0xBC600000 with a call to ThreadManForKernel.hleKernelGetSystemTimeLow()
			// threadman.prx reading from 0xBC600000 is equivalent to a call of sceKernelGetSystemTimeLow()
			new PrxPatchInfo("kd/threadman.prx", 0x0001131C, 0x8C420000, SYSCALL(ThreadManForKernelModule, "hleKernelGetSystemTimeLow")), // Replace reading of hardware register *(int*)0xBC600000 with a call to ThreadManForKernel.hleKernelGetSystemTimeLow()
			// systimer.prx used by SysTimerInit
			new PrxPatchInfo("kd/systimer.prx", 0x00000334, 0xAD330000, NOP()), // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/systimer/systimer.c#L203)
			new PrxPatchInfo("kd/systimer.prx", 0x00000340, 0xAD320008, NOP()), // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/systimer/systimer.c#L204)
			new PrxPatchInfo("kd/systimer.prx", 0x00000348, 0xAD32000C, NOP()), // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/systimer/systimer.c#L205)
			new PrxPatchInfo("kd/systimer.prx", 0x00000350, 0x8D230000, NOP()), // Avoid access to hardware register (https://github.com/uofw/uofw/blob/master/src/systimer/systimer.c#L206)
			// memlmd_01g.prx used by module_start
			new PrxPatchInfo("kd/memlmd_01g.prx", 0x00001C44, 0x27BDFFE0, JR()),           // NOP the function sceUtilsBufferCopyByPolling()
			new PrxPatchInfo("kd/memlmd_01g.prx", 0x00001C48, 0x3C020000, MOVE(_v0, _zr)), // NOP the function sceUtilsBufferCopyByPolling()
			// lowio.prx used by module_start
			new PrxPatchInfo("kd/lowio.prx", 0x00001CCC, 0x8C490000, MOVE(_t1, _zr)), // Avoid access to hardware register in sceSysregPllUpdateFrequency()
			new PrxPatchInfo("kd/lowio.prx", 0x000008A0, 0x8D090058, MOVE(_t1, _zr)), // Avoid access to hardware register in sceSysregGpioClkEnable()
			new PrxPatchInfo("kd/lowio.prx", 0x000008C4, 0xAD090058, NOP()),          // Avoid access to hardware register in sceSysregGpioClkEnable()
			new PrxPatchInfo("kd/lowio.prx", 0x00002D48, 0x8D8C0000, MOVE(_t4, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00002D50, 0x8F390040, MOVE(_t9, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00002D58, 0x8C630010, MOVE(_v1, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00002D60, 0x8CE70014, MOVE(_a3, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00002D68, 0x8CC60018, MOVE(_a2, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00002D70, 0x8D08001C, MOVE(_t0, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00002DA0, 0xAC380010, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00002DB0, 0xAC2F0014, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00002DBC, 0xAC2E0018, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00002DC4, 0xAC2D001C, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00002DD0, 0xAC200030, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00002DF0, 0xAC200034, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00000CF8, 0x8D090078, MOVE(_t1, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00000D1C, 0xAD090078, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00003898, 0x8CA80004, MOVE(_t0, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x000004D8, 0x8D090050, MOVE(_t1, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x000004FC, 0xAD090050, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x000053E8, 0x8C840110, MOVE(_a0, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00005770, 0x8CA50110, MOVE(_a1, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00005790, 0xAC2A0110, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x0000579C, 0xAC290100, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00005740, 0xAC20010C, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00005748, 0xAC250110, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00005484, 0x8D8C0110, MOVE(_t4, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00005498, 0xAC270110, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00005954, 0xAC220100, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00005960, 0xAC20010C, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x0000591C, 0x8F390110, MOVE(_t9, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00005928, 0xAC270110, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00005FF4, 0x8F0E0000, MOVE(_t6, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x0000600C, 0x8D0B0000, MOVE(_t3, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00006024, 0x8CE60000, MOVE(_a2, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00006038, 0x8D490000, MOVE(_t1, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00006098, 0xAC400000, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x0000199C, 0x8CC60040, MOVE(_a2, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x000079E8, 0x8DA60004, MOVE(_a2, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007A5C, 0x8DA60008, MOVE(_a2, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007A68, 0x8DB80010, MOVE(_t8, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007A6C, 0x8DA20014, MOVE(_v0, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007A70, 0x8DA80018, MOVE(_t0, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007A74, 0x8DA5001C, MOVE(_a1, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007A78, 0x8DA90020, MOVE(_t1, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007A7C, 0x8DAA0024, MOVE(_t2, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007A80, 0x8DAB0028, MOVE(_t3, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007A84, 0x8DA6002C, MOVE(_a2, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007A88, 0x8DA70040, MOVE(_a3, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007A94, 0x8DA40044, MOVE(_a0, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007AA0, 0x8DA70048, MOVE(_a3, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007AAC, 0x8DA4004C, MOVE(_a0, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007AEC, 0x8D8C0060, MOVE(_t4, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007678, 0x8E8B0000, MOVE(_t3, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00007688, 0xAE820000, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00008BBC, 0xAC231038, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00008C0C, 0xAC291200, NOP()),          // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x00008C8C, 0x8C631004, MOVE(_v1, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x0000B038, 0x00653023, MOVE(_a2, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x000001D8, 0x8D09004C, MOVE(_t1, _zr)), // Avoid access to hardware register
			new PrxPatchInfo("kd/lowio.prx", 0x000001FC, 0xAD09004C, NOP()),          // Avoid access to hardware register
			// lowio.prx used by sceSysregSetMasterPriv()
			new PrxPatchInfo("kd/lowio.prx", 0x00001AD0, 0x8C420044, MOVE(_v0, _zr)), // Avoid access to hardware register in sceSysregSetMasterPriv()
			new PrxPatchInfo("kd/lowio.prx", 0x00001AE4, 0xAC220044, NOP()),          // Avoid access to hardware register in sceSysregSetMasterPriv()
			// lowio.prx: syscalls from sceSysreg module
			new PrxSyscallPatchInfo("kd/lowio.prx", sceSysregModule, "sceSysregGetFuseId", 0x00001AF4, 0x3C03BC10, 0x3C07BC10),
			// lowio.prx: syscalls from sceNand module
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandSetWriteProtect"     , 0x00009014, 0x27BDFFF0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandLock"                , 0x00009094, 0x27BDFFF0, 0x3C030000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandUnlock"              , 0x00009114, 0x27BDFFF0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReset"               , 0x00009180, 0x27BDFFF0, 0xAFB00000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadId"              , 0x00009208, 0x24030090, 0x3C01BD10),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadAccess"          , 0x00009260, 0x27BDFFD0, 0x3C028000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandWriteAccess"         , 0x00009444, 0x27BDFFD0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandEraseBlock"          , 0x00009608, 0x27BDFFF0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadExtraOnly"       , 0x0000970C, 0x27BDFFE0, 0xAFB3000C),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadStatus"          , 0x00009888, 0x3C09BD10, 0x35231008),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandSetScramble"         , 0x000098BC, 0x3C030000, 0x00001021),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadPages"           , 0x000098CC, 0x27BDFFF0, 0x00004021),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandWritePages"          , 0x00009910, 0x24080010, 0x0005400B),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadPagesRawExtra"   , 0x00009938, 0x27BDFFF0, 0xAFBF0000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandWritePagesRawExtra"  , 0x00009954, 0x24090030, 0x24080020),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadPagesRawAll"     , 0x00009978, 0x27BDFFF0, 0xAFBF0000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandWritePagesRawAll"    , 0x00009994, 0x27BDFFF0, 0xAFBF0000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandDetectChip"          , 0x0000A010, 0x27BDFFE0, 0x03A02021),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandWriteBlockWithVerify", 0x0000A134, 0x27BDFFE0, 0xAFBF0014),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandReadBlockWithRetry"  , 0x0000A1E8, 0x27BDFFE0, 0xAFBF0014),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandVerifyBlockWithRetry", 0x0000A26C, 0x27BDFFC0, 0xAFB50024),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandEraseBlockWithRetry" , 0x0000A3BC, 0x3C030000, 0x8C661A30),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandIsBadBlock"          , 0x0000A430, 0x3C030000, 0x8C661A30),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandDoMarkAsBadBlock"    , 0x0000A4BC, 0x27BDFFE0, 0xAFB50014),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandDumpWearBBMSize"     , 0x0000A5C8, 0x27BDFFE0, 0xAFB60018),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandCountChipMakersBBM"  , 0x0000A6B4, 0x27BDFFE0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandDetectChipMakersBBM" , 0x0000A748, 0x27BDFFD0, 0xAFB60018),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandEraseAllBlock"       , 0x0000A840, 0x27BDFFE0, 0xAFB10004),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandTestBlock"           , 0x0000A8D8, 0x27BDBDE0, 0xAFB24208),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandGetPageSize"         , 0x0000AA88, 0x3C040000, 0x03E00008),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandGetPagesPerBlock"    , 0x0000AA94, 0x3C040000, 0x03E00008),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandGetTotalBlocks"      , 0x0000AAA0, 0x3C040000, 0x03E00008),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandWriteBlock"          , 0x0000AAAC, 0x27BDFFF0, 0xAFB20008),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandCalcEcc"             , 0x0000AB0C, 0x27BDFFF0, 0xAFB3000C),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandVerifyEcc"           , 0x0000AD38, 0x27BDFFF0, 0xAFBF0000),
			new PrxSyscallPatchInfo("kd/lowio.prx", sceNandModule, "sceNandCorrectEcc"          , 0x0000AD54, 0x27BDFFF0, 0xAFBF0000),
			// ge.prx used by module_start / sceGeInit
			new PrxPatchInfo("kd/ge.prx", 0x00000284, 0x8F180804, MOVE(_t8, _zr)), // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x0000028C, 0x8F390810, MOVE(_t9, _zr)), // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000294, 0x8E100814, MOVE(_s0, _zr)), // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x0000029C, 0x8DCE0818, MOVE(_t6, _zr)), // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x000002A8, 0x8D8C08EC, MOVE(_t4, _zr)), // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x000002D4, 0xAC200100, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x000002E4, 0xAC200108, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x000002F4, 0xAC20010C, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000304, 0xAC200110, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x0000030C, 0xAC200114, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000314, 0xAC200118, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x0000031C, 0xAC20011C, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000324, 0xAC200120, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x0000032C, 0xAC200124, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000334, 0xAC200128, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x0000033C, 0x8C630304, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000344, 0xAC230310, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000350, 0x8C420308, MOVE(_v0, _zr)), // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000358, 0xAC22030C, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000360, 0xAC230308, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000480, 0x8E100308, MOVE(_s0, _zr)), // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000360, 0xAC230308, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000494, 0xAC30030C, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x0000049C, 0xAC390108, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x000004A8, 0xAC20010C, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x000004B4, 0xAC270100, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x000004BC, 0x8C630100, MOVE(_v1, _zr)), // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x000004D8, 0xAC200100, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x000004E8, 0xAC200108, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x000004F8, 0xAC20010C, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000508, 0x8D8C0304, MOVE(_t4, _zr)), // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000518, 0xAC2C0310, NOP()),          // Avoid access to hardware register in sceGeInit()
			new PrxPatchInfo("kd/ge.prx", 0x00000524, 0xAC2B0308, NOP()),          // Avoid access to hardware register in sceGeInit()
			// ge.prx used by module_start / sceGeEdramInit()
			new PrxPatchInfo("kd/ge.prx", 0x0000265C, 0xAC220010, NOP()),          // Avoid access to hardware register in sceGeEdramInit()
			new PrxPatchInfo("kd/ge.prx", 0x00002664, 0x8CA50010, MOVE(_a1, _zr)), // Avoid access to hardware register in sceGeEdramInit()
			new PrxPatchInfo("kd/ge.prx", 0x0000268C, 0xAC290020, NOP()),          // Avoid access to hardware register in sceGeEdramInit()
			new PrxPatchInfo("kd/ge.prx", 0x00002694, 0xAC280040, NOP()),          // Avoid access to hardware register in sceGeEdramInit()
			new PrxPatchInfo("kd/ge.prx", 0x0000269C, 0xAC270090, NOP()),          // Avoid access to hardware register in sceGeEdramInit()
			new PrxPatchInfo("kd/ge.prx", 0x000026A4, 0xAC220400, NOP()),          // Avoid access to hardware register in sceGeEdramInit()
			new PrxPatchInfo("kd/ge.prx", 0x000026B4, 0x8D6B0070, MOVE(_t3, _zr)), // Avoid access to hardware register in sceGeEdramInit()
			new PrxPatchInfo("kd/ge.prx", 0x0000275C, 0x8DEF0080, MOVE(_t7, _zr)), // Avoid access to hardware register in sceGeEdramInit()
			new PrxPatchInfo("kd/ge.prx", 0x000026E8, 0x8CA50008, MOVE(_a1, _zr)), // Avoid access to hardware register in sceGeEdramInit()
			// Last entry is a dummy one
			new PatchInfo("XXX dummy XXX", 0, 0, 0) // Dummy entry for easier formatting of the above entries
	};

	private static class PatchInfo {
		protected String fileName;
		protected int offset;
		protected int oldValue;
		protected int newValue;

		public PatchInfo(String fileName, int offset, int oldValue, int newValue) {
			this.fileName = fileName;
			this.offset = offset;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		public boolean matches(String fileName) {
			return this.fileName.equalsIgnoreCase(fileName);
		}

		protected void apply(byte[] buffer, int offset) {
			if (offset >= 0 && offset < buffer.length) {
				int checkValue = readUnaligned32(buffer, offset);
				if (checkValue != oldValue) {
		    		log.error(String.format("Patching of file '%s' failed at offset 0x%08X, 0x%08X found instead of 0x%08X", fileName, offset, checkValue, oldValue));
				} else {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Patching file '%s' at offset 0x%08X: 0x%08X -> 0x%08X", fileName, offset, oldValue, newValue));
					}
					writeUnaligned32(buffer, offset, newValue);
				}
			}
		}

		public void apply(byte[] buffer) {
			apply(buffer, offset);
		}

		@Override
		public String toString() {
			return String.format("Patch '%s' at offset 0x%08X: 0x%08X -> 0x%08X", fileName, offset, oldValue, newValue);
		}
	}

	private static class PrxPatchInfo extends PatchInfo {
		public PrxPatchInfo(String fileName, int offset, int oldValue, int newValue) {
			super(fileName, offset, oldValue, newValue);
		}

		protected int getFileOffset(byte[] buffer) {
			int elfMagic = readUnaligned32(buffer, 0);
			if (elfMagic != Elf32Header.ELF_MAGIC) {
				return offset;
			}

			int phOffset = readUnaligned32(buffer, 28);
			int phEntSize = readUnaligned16(buffer, 42);
			int phNum = readUnaligned16(buffer, 44);

			int segmentOffset = offset;
			// Scan all the ELF program headers
			for (int i = 0; i < phNum; i++) {
				int offset = phOffset + i * phEntSize;
				int phEntFileSize = readUnaligned32(buffer, offset + 16);
				if (segmentOffset < phEntFileSize) {
					int phFileOffset = readUnaligned32(buffer, offset + 4);
					return phFileOffset + segmentOffset;
				}

				int phEntMemSize = readUnaligned32(buffer, offset + 20);
				segmentOffset -= phEntMemSize;
				if (segmentOffset < 0) {
		    		log.error(String.format("Patching of file '%s' failed: incorrect offset 0x%08X outside of program header segment #%d", fileName, offset, i));
		    		return -1;
				}
			}

    		log.error(String.format("Patching of file '%s' failed: incorrect offset 0x%08X outside of all program header segments", fileName, offset));
			return -1;
		}

		@Override
		public void apply(byte[] buffer) {
			int fileOffset = getFileOffset(buffer);

			if (fileOffset >= 0) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Patching file '%s' at PRX offset 0x%08X: 0x%08X -> 0x%08X", fileName, offset, oldValue, newValue));
				}
				super.apply(buffer, fileOffset);
			}
		}
	}

	private static class PrxSyscallPatchInfo extends PrxPatchInfo {
		private PrxPatchInfo patchInfo2;
		private String functionName;

		public PrxSyscallPatchInfo(String fileName, HLEModule hleModule, String functionName, int offset, int oldValue1, int oldValue2) {
			super(fileName, offset, oldValue1, JR());
			this.functionName = functionName;
			patchInfo2 = new PrxPatchInfo(fileName, offset + 4, oldValue2, SYSCALL(hleModule, functionName));
		}

		@Override
		public void apply(byte[] buffer) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Patching file '%s' at PRX offset 0x%08X: %s", fileName, offset, functionName));
			}
			super.apply(buffer);
			patchInfo2.apply(buffer);
		}
	}

	public PatchFileVirtualFileSystem(IVirtualFileSystem vfs) {
		super(vfs);
	}

	private List<PatchInfo> getPatches(String fileName) {
		List<PatchInfo> filePatches = new LinkedList<PatchInfo>();
		for (PatchInfo patch : allPatches) {
			if (patch.matches(fileName)) {
				filePatches.add(patch);
			}
		}

		if (filePatches.isEmpty()) {
			return null;
		}
		return filePatches;
	}

	private IVirtualFile ioOpenPatchedFile(String fileName, int flags, int mode, List<PatchInfo> patches) {
		IVirtualFile vFile = super.ioOpen(fileName, flags, mode);
		if (vFile == null) {
			return null;
		}

		byte[] buffer = Utilities.readCompleteFile(vFile);
		vFile.ioClose();
		if (buffer == null) {
			return null;
		}

		for (PatchInfo patch : patches) {
			patch.apply(buffer);
		}

		return new ByteArrayVirtualFile(buffer);
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		List<PatchInfo> patches = getPatches(fileName);
		if (patches != null) {
			return ioOpenPatchedFile(fileName, flags, mode, patches);
		}

		return super.ioOpen(fileName, flags, mode);
	}
}
