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

import static jpcsp.Emulator.getScheduler;
import static jpcsp.HLE.Modules.ThreadManForUserModule;
import static jpcsp.HLE.Modules.sceUsbPspcmModule;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspUsbDriver;
import jpcsp.HLE.kernel.types.pspUsbdDeviceReq;
import jpcsp.scheduler.Scheduler;
import jpcsp.util.Utilities;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;

public class sceUsbBus extends HLEModule {
    public static Logger log = Modules.getLogger("sceUsbBus");
    private boolean unknownFlag1;
    private boolean unknownFlag2;
    private boolean unknownFlag3;
    private final List<pspUsbDriver> registeredDrivers = new LinkedList<pspUsbDriver>();

    public pspUsbDriver getRegisteredUsbDriver(String name) {
    	for (pspUsbDriver usbDriver : registeredDrivers) {
    		if (name.equals(usbDriver.name)) {
    			return usbDriver;
    		}
    	}

    	return null;
    }

    private class sceUsbbdReqComplete implements IAction {
    	private final pspUsbdDeviceReq deviceReq;
    	private final SceKernelThreadInfo threadInfo;

		public sceUsbbdReqComplete(pspUsbdDeviceReq deviceReq, SceKernelThreadInfo threadInfo) {
			this.deviceReq = deviceReq;
			this.threadInfo = threadInfo;
		}

		@Override
		public void execute() {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Executing completion function for %s, endpnum=0x%X, isRegistered=%b: %s", deviceReq, deviceReq.endp.getValue32(0), isRegistered(deviceReq), Utilities.getMemoryDump(deviceReq.data, deviceReq.recvsize)));
			}

			if (isRegistered(deviceReq)) {
				IAction nextAction = null;
				if (deviceReq.nextRequest != 0) {
					pspUsbdDeviceReq nextDeviceReq = new pspUsbdDeviceReq();
					nextDeviceReq.read(getMemory(), deviceReq.nextRequest);
					nextAction = new sceUsbbdReqComplete(nextDeviceReq, threadInfo);

					if (log.isDebugEnabled()) {
						log.debug(String.format("next deviceReq=%s, endpnum=0x%X", nextDeviceReq, nextDeviceReq.endp.getValue32(0)));
					}
				}
				// Function to be called on completion
				ThreadManForUserModule.executeCallback(threadInfo, deviceReq.func.getAddress(), nextAction, false, true, deviceReq.getBaseAddress());
			}
		}
    	
    }

    public int hleUsbbdRegister(pspUsbDriver usbDriver) {
    	registeredDrivers.add(usbDriver);

    	return 0;
    }

    public int hleUsbbdUnregister(pspUsbDriver usbDriver) {
    	for (pspUsbDriver registeredDriver : registeredDrivers) {
    		if (registeredDriver == usbDriver || registeredDriver.getBaseAddress() == usbDriver.getBaseAddress()) {
    			registeredDrivers.remove(registeredDriver);
    			break;
    		}
    	}

    	return 0;
    }

    private boolean isRegistered(pspUsbdDeviceReq deviceReq) {
    	for (pspUsbDriver driver : registeredDrivers) {
    		TPointer endpoint = new TPointer(driver.endp);
    		for (int i = 0; i < driver.endpoints; i++) {
    			if (deviceReq.endp.equals(endpoint)) {
    				return true;
    			}
    			endpoint.add(12);
    		}
    	}

    	return false;
    }

    public void triggerCompletionFunction(pspUsbdDeviceReq deviceReq) {
    	if (deviceReq.func != null && deviceReq.func.isNotNull()) {
    		getScheduler().addAction(new sceUsbbdReqComplete(deviceReq, ThreadManForUserModule.getCurrentThread()));
    	}
    }

    /**
     * Queue a send request (IN from host pov)
     * 
     * @param req Pointer to a filled out UsbdDeviceReq structure.
     * @return    0 on success, < 0 on error
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x23E51D8F, version = 150)
    public int sceUsbbdReqSend(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer req) {
    	int result = 0;

		pspUsbDriver usbDriver = getRegisteredUsbDriver("USBPSPCommunicationDriver");
		TPointer deviceReqAddress = new TPointer(req);
		boolean first = true;
		while (true) {
	    	pspUsbdDeviceReq deviceReq = new pspUsbdDeviceReq();
	    	deviceReq.read(deviceReqAddress);

	    	if (log.isDebugEnabled()) {
	    		log.debug(String.format("sceUsbbdReqSend size=0x%X, data=%s", deviceReq.size, Utilities.getMemoryDump(deviceReq.data, deviceReq.size)));
	    	}

	    	deviceReq.retcode = 0;
	    	deviceReq.recvsize = deviceReq.size;

	    	if (usbDriver != null) {
	    		result = sceUsbPspcmModule.getUsbCommunicationStub().hleUsbbdReqSend(usbDriver, deviceReq, deviceReqAddress);
	    	}

	    	deviceReq.write(deviceReqAddress);

	    	if (deviceReq.func.isNotNull() && first) {
	    		Emulator.getScheduler().addAction(Scheduler.getNow() + 5000, new sceUsbbdReqComplete(deviceReq, ThreadManForUserModule.getCurrentThread()));
	    		first = false;
	    	}

	    	if (deviceReq.nextRequest == 0) {
	    		break;
	    	}
	    	deviceReqAddress = new TPointer(req.getMemory(), deviceReq.nextRequest);
		}

    	return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x90B82F55, version = 150)
    public void sceUsbBus_driver_90B82F55() {
    	unknownFlag1 = true;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7B87815D, version = 150)
    public void sceUsbBus_driver_7B87815D() {
    	unknownFlag1 = false;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8A3EB5D2, version = 150)
    public boolean sceUsbBus_driver_8A3EB5D2() {
    	return unknownFlag2;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFBA2072B, version = 150)
    public void sceUsbBus_driver_FBA2072B() {
    	if (unknownFlag1) {
    		unknownFlag2 = true;
    	}
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x48CCE3C1, version = 150)
    public boolean sceUsbBus_driver_48CCE3C1() {
    	return unknownFlag3;
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
    	pspUsbdDeviceReq deviceReq = new pspUsbdDeviceReq();
    	deviceReq.read(req);

    	int result = 0;

		pspUsbDriver usbDriver = getRegisteredUsbDriver("USBPSPCommunicationDriver");
    	if (usbDriver != null) {
    		result = sceUsbPspcmModule.getUsbCommunicationStub().hleUsbbdReqRecv(usbDriver, deviceReq, req);
    	} else {
        	if (deviceReq.func.isNotNull()) {
        		Emulator.getScheduler().addAction(Scheduler.getNow() + 5000, new sceUsbbdReqComplete(deviceReq, ThreadManForUserModule.getCurrentThread()));
        	}
    	}

    	return result;
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
    	pspUsbDriver usbDriver = new pspUsbDriver();
    	usbDriver.read(drv);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceUsbbdRegister %s", usbDriver));
    	}

    	return hleUsbbdRegister(usbDriver);
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
    	pspUsbDriver usbDriver = new pspUsbDriver();
    	usbDriver.read(drv);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceUsbbdUnregister %s", usbDriver));
    	}

    	return hleUsbbdUnregister(usbDriver);
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
    public int sceUsbbdReqCancel(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer req) {
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
}
