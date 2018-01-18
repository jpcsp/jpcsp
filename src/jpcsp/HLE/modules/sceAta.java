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
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer8;

public class sceAta extends HLEModule {
    public static Logger log = Modules.getLogger("sceAta");

    @HLEUnimplemented
    @HLEFunction(nid = 0xBE6261DA, version = 150)
    public void sceAta_driver_BE6261DA(int unknown) {
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4222D6F3, version = 150)
    public int sceAta_driver_4222D6F3() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4D225674, version = 150)
    public int sceAta_driver_4D225674() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAE100256, version = 150)
    public int sceAtaAhbSetAtaBlock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7DE9E14A, version = 150)
    public int sceAtaAhbEnableIntr() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x12805193, version = 150)
    public int sceAtaAhbSetupBus() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF61EAFC0, version = 150)
    public int sceAta_driver_F61EAFC0() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x76C0923D, version = 150)
    public int sceAta_driver_76C0923D() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE3E1EED7, version = 150)
    public int sceAta_driver_E3E1EED7() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0B53CAD8, version = 150)
    public int sceAta_driver_0B53CAD8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC74F04B7, version = 150)
    public int sceAtaExecPacketCmd(TPointer unknown1, int unknown2, int unknown3, int unknown4, int unknown5, int operationCode, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=3, usage=Usage.in) TPointer8 unknown6) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBDD30DEE, version = 150)
    public int sceAtaExecPacketCmdIE() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x010F750B, version = 150)
    public int sceAtaExecSetFeaturesCmd() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1C29566B, version = 150)
    public int sceAtaGetIntrStateFlag() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7F551D66, version = 150)
    public int sceAtaSetIntrStateFlag() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x68DEA2FF, version = 150)
    public int sceAtaClearIntrStateFlag() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD1E6E175, version = 150)
    public int sceAtaEnableClkIo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAC800B1D, version = 150)
    public int sceAtaDisableClkIo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC8BC8B83, version = 150)
    public int sceAtaStart() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCDC50BF0, version = 150)
    public int sceAtaStop() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x52D9A4CA, version = 150)
    public int sceAtaGetAtaDrive() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEBB91566, version = 150)
    public int sceAtaSetupAtaBlock() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6C58F096, version = 150)
    public int sceAtaWaitBusBusy1() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC21318E6, version = 150)
    public int sceAtaWaitBusBusy2() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8DE58134, version = 660)
    public int sceAtaWaitBusBusy2_660() {
    	// Has no parameters
    	return sceAtaWaitBusBusy2();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA77C230B, version = 150)
    public int sceAtaWaitBusBusyIE() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2F7BE141, version = 660)
    public int sceAtaWaitBusBusyIE_660() {
    	// Has no parameters
    	return sceAtaWaitBusBusyIE();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3AA3FA39, version = 150)
    public int sceAtaSelectDevice() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xACCEE63F, version = 150)
    public int sceAta_driver_ACCEE63F() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6E639701, version = 150)
    public int sceAtaScanDevice() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB863FD83, version = 150)
    public int sceAtaCheckDeviceReady() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB6BED47E, version = 150)
    public int sceAtaAccessDataPort() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6DE1C65F, version = 150)
    public int sceAtaAccessDataPortIE() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x84E14690, version = 150)
    public int sceAta_driver_84E14690() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC7B02795, version = 150)
    public int sceAta_driver_C7B02795() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1F05F48C, version = 150)
    public int sceAta_driver_1F05F48C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBA09142A, version = 150)
    public int sceAta_driver_BA09142A() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1C8DA2FD, version = 150)
    public int sceAtaSetDeviceIntr() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2AE26E08, version = 150)
    public int sceAta_driver_2AE26E08() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9D98086E, version = 150)
    public int sceAtaSetBusErrorIntr() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x689FCB7D, version = 150)
    public int sceAta_driver_689FCB7D() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC144826E, version = 150)
    public int sceAta_driver_C144826E() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFCF939D9, version = 150)
    public int sceAtaGetAtaCallBack() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9AE67E14, version = 150)
    public int sceAtaGetDriveStat() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x110D3739, version = 150)
    public void sceAtaSetDriveStat(int driveNumber, int driveStat) {
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7BBA095C, version = 150)
    public void sceAtaClearDriveStat(int driveNumber, int maskDriveStat) {
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3A61BF97, version = 150)
    public int sceAtaIsNormalDriveStat() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xACFF7CB5, version = 150)
    public int sceAta_driver_ACFF7CB5() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD8E525CB, version = 150)
    public int sceAtaGetIntrFlag() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB7F5B2CA, version = 150)
    public int sceAtaSetIntrFlag() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x638EEA14, version = 150)
    public int sceAta_driver_638EEA14() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB5982381, version = 150)
    public int sceAta_driver_B5982381() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7C6B31D8, version = 150)
    public int sceAta_driver_7C6B31D8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6FD8E2AB, version = 150)
    public int sceAta_driver_6FD8E2AB() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBE07B3A7, version = 150)
    public int sceAta_driver_BE07B3A7() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDCC8A89E, version = 150)
    public int sceAtaIsUmdDrive() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6FC42083, version = 150)
    public int sceAtaIsDvdDrive() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD225B43E, version = 660)
    public int sceAta_driver_D225B43E(int driveNumber) {
    	if (driveNumber != 0) {
    		return 0;
    	}
    	return 0; // Return the address of a 8-bytes long structure
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8B32C14C, version = 660)
    public int sceAta_driver_8B32C14C() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC344D497, version = 660)
    public int sceAta_driver_C344D497(int unknown1, int unknown2, int unknown3, int unknown4) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3265D064, version = 660)
    public int sceAta_driver_3265D064(int unknown1, int unknown2) {
    	return 0;
    }
}
