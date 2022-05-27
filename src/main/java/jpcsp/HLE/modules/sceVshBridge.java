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
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.util.Utilities;

public class sceVshBridge extends HLEModule {
    public static Logger log = Modules.getLogger("sceVshBridge");

    @HLEUnimplemented
    @HLEFunction(nid = 0xA5628F0D, version = 150)
    public int vshKernelLoadModuleVSH(PspString fileName, int flags, @CanBeNull @BufferInfo(lengthInfo = LengthInfo.variableLength, usage = Usage.in) TPointer optionAddr) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x41C54ADF, version = 150)
    public int vshKernelLoadModuleVSHByID() {
    	return 0;
    }

    @HLEFunction(nid = 0xC9626587, version = 150)
    public int vshKernelLoadModuleBufferVSH(int bufferSize, @BufferInfo(lengthInfo=LengthInfo.previousParameter, usage=Usage.in) TPointer buffer, int flags, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.variableLength, usage=Usage.in) TPointer optionAddr) {
    	return Modules.ModuleMgrForKernelModule.sceKernelLoadModuleBufferVSH(bufferSize, buffer, flags, optionAddr);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5C2983C2, version = 150)
    public int vshChkregCheckRegion() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x61001D64, version = 150)
    public int vshChkregGetPsCode() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC949966C, version = 150)
    public int sceVshBridge_C949966C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5D4213EA, version = 150)
    public int vshMePowerControlAvcPower() {
    	return 0;
    }

    @HLEFunction(nid = 0xC6395C03, version = 150)
    public int vshCtrlReadBufferPositive(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.out) TPointer dataAddr, int numBuf) {
    	return Modules.sceCtrlModule.sceCtrlReadBufferPositive(dataAddr, numBuf);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0163A8E7, version = 150)
    public int vshUmdManTerm() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7C1658F2, version = 150)
    public int sceVshBridge_7C1658F2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0E10922A, version = 150)
    public int vshDisplaySetHoldMode() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCA719C34, version = 150)
    public int vshImposeGetStatus() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4E4E4DA3, version = 150)
    public int vshImposeSetStatus() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x639C3CB3, version = 150)
    public int vshImposeGetParam() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4A596D2D, version = 150)
    public int vshImposeSetParam() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5894C339, version = 150)
    public int vshImposeChanges() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0FA48729, version = 150)
    public int vshRtcSetConf() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x16415246, version = 150)
    public int vshRtcSetCurrentTick() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5350C073, version = 150)
    public int vshMSAudioFormatICV() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2380DC08, version = 150)
    public int vshIoDevctl() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4DB43867, version = 150)
    public int vshIdStorageLookup() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7B67394E, version = 150)
    public int vshAudioSetFrequency() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCE32CBEF, version = 150)
    public int vshMSAudioInit() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE5DA5E95, version = 150)
    public int vshMSAudioEnd() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6CAEB765, version = 150)
    public int vshMSAudioAuth() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x53BFD101, version = 150)
    public int vshMSAudioCheckICV() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE174218C, version = 150)
    public int vshMSAudioCheckICVn() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7EA32357, version = 150)
    public int vshMSAudioDeauth() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x14877197, version = 150)
    public int sceVshBridge_14877197() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5BBB35E4, version = 150)
    public int sceVshBridge_5BBB35E4() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB27C593F, version = 150)
    public int sceVshBridge_B27C593F() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0D2CEAD2, version = 150)
    public int sceVshBridge_0D2CEAD2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD120667D, version = 150)
    public int sceVshBridge_D120667D() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD907B6AA, version = 150)
    public int sceVshBridge_D907B6AA() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD46D4528, version = 150)
    public int vshMSAudioInvalidateICV() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7A63BE73, version = 150)
    public int sceVshBridge_7A63BE73() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x222A18C4, version = 150)
    public int sceVshBridge_222A18C4() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x04310D7C, version = 150)
    public int sceVshBridge_04310D7C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4E61C72E, version = 150)
    public int vshMSAudioReadMACList() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE2DD0A81, version = 150)
    public int vshMSAudioGetInitialEKB() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6396ACBD, version = 150)
    public int vshMSAudioGetICVInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x274BB6AE, version = 150)
    public int vshVaudioOutputBlocking() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8C440581, version = 150)
    public int vshVaudioChReserve() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x07EC5661, version = 150)
    public int vshVaudioChRelease() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3B3D9F5D, version = 150)
    public int vshAudioSRCOutputBlocking() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB7F233A2, version = 150)
    public int vshAudioSRCChReserve() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC58D0939, version = 150)
    public int vshAudioSRCChRelease() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCBDA2613, version = 150)
    public int vshMeRpcLock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA7F0E8E0, version = 150)
    public int vshMeRpcUnlock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x98B4117E, version = 150)
    public int vshKernelLoadExecBufferPlain0() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8399A8AA, version = 150)
    public int vshKernelLoadExecBufferPlain() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE614F45F, version = 150)
    public int vshKernelLoadExecFromHost() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEEFB02BB, version = 150)
    public int vshKernelLoadExec() {
    	return 0;
    }

    @HLEFunction(nid = 0x9929DDA5, version = 150)
    public int vshKernelExitVSH() {
    	return Modules.LoadExecForUserModule.sceKernelExitGame();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB7C46DCA, version = 150)
    public int vshKernelLoadExecVSHDiscUpdater() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF4873F4D, version = 150)
    public int vshKernelLoadExecVSHDisk() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x83528906, version = 150)
    public int vshKernelLoadExecBufferVSHUsbWlan() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF35BFB7D, version = 150)
    public int vshKernelLoadExecVSHMs1() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x97FB006F, version = 150)
    public int vshKernelLoadExecVSHMs2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x029EF6C9, version = 150)
    public int vshKernelLoadExecVSHMs3() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x40716012, version = 150)
    public int vshKernelExitVSHVSH() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x88DA81A5, version = 150)
    public int vshKernelLoadExecBufferVSHPlain() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC1D3AE95, version = 150)
    public int vshKernelDipswSet() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD08C1FBE, version = 150)
    public int vshKernelDipswClear() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x04AEC74C, version = 150)
    public int vshKernelLoadExecVSHDiscDebug() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x68BE3316, version = 150)
    public int vshKernelLoadExecBufferVSHUsbWlanDebug() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x88BD8364, version = 150)
    public int vshKernelLoadExecBufferVSHFromHost() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x74DA9D25, version = 150)
    public int vshLflashFatfmtStartFatfmt(int numberParameters, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.in) TPointer32 parameters) {
    	if (log.isDebugEnabled()) {
    		for (int i = 0; i < numberParameters; i++) {
    			log.debug(String.format("vshLflashFatfmtStartFatfmt parameter#%d=%s: '%s'", i, parameters.getPointer(i * 4), Utilities.readStringZ(parameters.getValue(i * 4))));
    		}
    	}

    	return 0;
    }
}
