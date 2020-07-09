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

import java.util.HashMap;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;

public class scePaf extends HLEModule {
    public static Logger log = Modules.getLogger("scePaf");
    private HashMap<Integer, SysMemInfo> allocated = new HashMap<Integer, SysMemInfo>();

    @HLEFunction(nid = 0xA138A376, version = 660)
    public int scePaf_A138A376_sprintf(CpuState cpu, TPointer buffer, String format) {
    	return Modules.SysclibForKernelModule.sprintf(cpu, buffer, format);
    }

    @HLEFunction(nid = 0x0FCDFA1E, version = 150)
    public int scePaf_0FCDFA1E_malloc(int size) {
    	SysMemInfo sysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "scePaf_0FCDFA1E", SysMemUserForUser.PSP_SMEM_Low, size, 0);
    	if (sysMemInfo == null) {
    		return 0;
    	}

    	int addr = sysMemInfo.addr;
    	allocated.put(addr, sysMemInfo);

    	return addr;
    }

    @HLEFunction(nid = 0xB4652CFE, version = 150)
    public int scePaf_B4652CFE_memcpy(@CanBeNull TPointer destAddr, TPointer srcAddr, int size) {
    	return Modules.SysclibForKernelModule.memcpy(destAddr, srcAddr, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x003B3F87, version = 150)
    public int scePaf_003B3F87() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0C2CD696, version = 150)
    public int scePaf_0C2CD696() {
    	return 0;
    }

    @HLEFunction(nid = 0x1F02DD65, version = 150)
    public int scePaf_1F02DD65_strncpy(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextNextParameter, usage=Usage.out) TPointer destAddr, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer srcAddr, int size) {
    	return Modules.SysclibForKernelModule.strncpy(destAddr, srcAddr, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x20BEF384, version = 150)
    public int scePaf_20BEF384() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x22420CC7, version = 150)
    public int scePaf_22420CC7() {
    	return 0;
    }

    @HLEFunction(nid = 0x3C4BC2CD, version = 150)
    public int scePaf_3C4BC2CD_strtol(@CanBeNull PspString string, @CanBeNull TPointer32 endString, int base) {
    	return Modules.SysclibForKernelModule.strtol(string, endString, base);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3E564415, version = 150)
    public int scePaf_3E564415(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 0x620, usage = Usage.inout) TPointer unknownBuffer, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 29, usage = Usage.in) TPointer unknownBuffer3, @BufferInfo(usage = Usage.out) TPointer32 unknownBuffer4, @BufferInfo(usage = Usage.out) TPointer32 unknownBuffer5, @BufferInfo(usage = Usage.out) TPointer32 unknownBuffer6, @BufferInfo(usage = Usage.out) TPointer32 unknownBuffer7, @BufferInfo(usage = Usage.out) TPointer32 unknownBuffer8, @BufferInfo(usage = Usage.out) TPointer32 unknownBuffer9) {
    	return 1;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x43759F51, version = 150)
    public int scePaf_43759F51(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 0x620, usage = Usage.inout) TPointer unknownBuffer) {
    	int unknownBuffer2Size = 0xFC;
    	SysMemInfo sysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "scePaf_8C5CC663", SysMemUserForUser.PSP_SMEM_Low, unknownBuffer2Size, 0);
    	TPointer unknownBuffer2 = new TPointer(getMemory(), sysMemInfo.addr);
    	unknownBuffer2.clear(unknownBuffer2Size);
    	return unknownBuffer2.getAddress();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4E43A742, version = 150)
    public void scePaf_4E43A742(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 0x620, usage = Usage.inout) TPointer unknownBuffer, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 0xFC, usage = Usage.inout) TPointer unknownBuffer2) {
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5A12583F, version = 150)
    public void scePaf_5A12583F(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 0x620, usage = Usage.inout) TPointer unknownBuffer, int unknown, TPointer unknownCallback) {
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x65169E51, version = 150)
    public int scePaf_65169E51() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x66CCA794, version = 150)
    public int scePaf_66CCA794() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6D55F3F5, version = 150)
    public int scePaf_6D55F3F5() {
    	return 0;
    }

    @HLEFunction(nid = 0x706ABBFF, version = 150)
    public int scePaf_706ABBFF_strncpy(@CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextNextParameter, usage=Usage.out) TPointer destAddr, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer srcAddr, int size) {
    	return Modules.SysclibForKernelModule.strncpy(destAddr, srcAddr, size);
    }

    @HLEFunction(nid = 0x71B92320, version = 150)
    public void scePaf_71B92320_free(TPointer address) {
    	SysMemInfo sysMemInfo = allocated.remove(address.getAddress());
    	if (sysMemInfo == null) {
    		return;
    	}
    	Modules.SysMemUserForUserModule.free(sysMemInfo);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7B7133D5, version = 150)
    public int scePaf_7B7133D5() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8166CA82, version = 150)
    public int scePaf_8166CA82() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8C5CC663, version = 150)
    public int scePaf_8C5CC663(PspString unknownVersion, int unknown1, int unknown2, int unknown3) {
    	int unknownBufferSize = 0x620;
    	SysMemInfo sysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "scePaf_8C5CC663", SysMemUserForUser.PSP_SMEM_Low, unknownBufferSize, 0);
    	TPointer unknownBuffer = new TPointer(getMemory(), sysMemInfo.addr);
    	unknownBuffer.clear(unknownBufferSize);
    	return unknownBuffer.getAddress();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8E805192, version = 150)
    public int scePaf_8E805192() {
    	return 0;
    }

    @HLEFunction(nid = 0x9A418CCC, version = 150)
    public int scePaf_9A418CCC_memcpy(@CanBeNull TPointer destAddr, TPointer srcAddr, int size) {
    	return Modules.SysclibForKernelModule.memcpy(destAddr, srcAddr, size);
    }

    @HLEFunction(nid = 0x9CD6C5F4, version = 150)
    public int scePaf_9CD6C5F4_memcmp(@BufferInfo(lengthInfo = LengthInfo.nextNextParameter, usage = Usage.in) TPointer src1Addr, @BufferInfo(lengthInfo = LengthInfo.nextParameter, usage = Usage.in) TPointer src2Addr, int size) {
    	return Modules.SysclibForKernelModule.memcmp(src1Addr, src2Addr, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9D854500, version = 150)
    public int scePaf_9D854500() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA631AC8B, version = 150)
    public int scePaf_A631AC8B() {
    	return 0;
    }

    @HLEFunction(nid = 0xB05D9677, version = 150)
    public int scePaf_B05D9677_memcmp(@BufferInfo(lengthInfo = LengthInfo.nextNextParameter, usage = Usage.in) TPointer src1Addr, @BufferInfo(lengthInfo = LengthInfo.nextParameter, usage = Usage.in) TPointer src2Addr, int size) {
    	return Modules.SysclibForKernelModule.memcmp(src1Addr, src2Addr, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB110AF46, version = 150)
    public int scePaf_B110AF46() {
    	return 0;
    }

    @HLEFunction(nid = 0xBB89C9EA, version = 150)
    public int scePaf_BB89C9EA_memset(@CanBeNull TPointer destAddr, int data, int size) {
    	return Modules.SysclibForKernelModule.memset(destAddr, data, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD229572C, version = 150)
    public int scePaf_D229572C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD7DCB972, version = 150)
    public int scePaf_D7DCB972() {
    	return 0;
    }

    @HLEFunction(nid = 0xD9E2D6E1, version = 150)
    public int scePaf_D9E2D6E1_memset(@CanBeNull TPointer destAddr, int data, int size) {
    	return Modules.SysclibForKernelModule.memset(destAddr, data, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE0B32AE8, version = 150)
    public int scePaf_E0B32AE8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE49835DC, version = 150)
    public int scePaf_E49835DC() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFF9C876B, version = 150)
    public int scePaf_FF9C876B() {
    	return 0;
    }
}
