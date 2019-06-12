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

public class sceUsbBus extends HLEModule {
    public static Logger log = Modules.getLogger("sceUsbBus");

    /**
     * Queue a send request (IN from host pov)
     * 
     * @param req Pointer to a filled out UsbdDeviceReq structure.
     * @return    0 on success, < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x23E51D8F, version = 150)
    public int sceUsbbdReqSend(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer req) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x48CCE3C1, version = 150)
    public int sceUsbBus_driver_48CCE3C1() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7B87815D, version = 150)
    public int sceUsbBus_driver_7B87815D() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8A3EB5D2, version = 150)
    public int sceUsbBus_driver_8A3EB5D2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x90B82F55, version = 150)
    public int sceUsbBus_driver_90B82F55() {
    	return 0;
    }

    /**
     * Queue a receive request (OUT from host pov)
     * 
     * @param req Pointer to a filled out UsbdDeviceReq structure
     * @return    0 on success, < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x913EC15D, version = 150)
    public int sceUsbbdReqRecv(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer req) {
    	return 0;
    }

    /**
     * Clear the FIFO on an endpoint.
     * 
     * @param endp The endpoint to clear
     * @return     0 on success, < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x951A24CC, version = 150)
    public int sceUsbbdClearFIFO(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.inout) TPointer endp) {
    	return 0;
    }

    /**
     * Register a USB driver.
     * 
     * @param drv Pointer to a filled out USB driver
     * @return    0 on success, < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xB1644BE7, version = 150)
    public int sceUsbbdRegister(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.in) TPointer drv) {
    	return 0;
    }

    /**
     * Unregister a USB driver.
     * 
     * @param drv Pointer to a filled out USB driver
     * @return    0 on success, < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xC1E2A540, version = 150)
    public int sceUsbbdUnregister(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.in) TPointer drv) {
    	return 0;
    }

    /**
     * Cancel any pending requests on an endpoint.
     * 
     * @param endp The endpoint to cancel
     * @return     0 on success, < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xC5E53685, version = 150)
    public int sceUsbbdReqCancelAll(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.inout) TPointer endp) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCC57EC9D, version = 150)
    public int sceUsbbdReqCancel() {
    	return 0;
    }

    /**
     * Stall an endpoint.
     * 
     * @param endp The endpoint to stall
     * @return     0 on success, < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xE65441C1, version = 150)
    public int sceUsbbdStall(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.inout) TPointer endp) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFBA2072B, version = 150)
    public int sceUsbBus_driver_FBA2072B() {
    	return 0;
    }
}
