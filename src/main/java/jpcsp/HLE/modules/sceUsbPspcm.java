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

import static jpcsp.Allegrex.compiler.RuntimeContext.debugMemory;
import static jpcsp.HLE.Modules.IoFileMgrForKernelModule;
import static jpcsp.HLE.Modules.ThreadManForUserModule;
import static jpcsp.HLE.Modules.sceUsbBusModule;
import static jpcsp.HLE.Modules.sceUsbPspcmModule;
import static jpcsp.HLE.modules.SysMemUserForUser.PSP_SMEM_Low;
import static jpcsp.HLE.modules.SysMemUserForUser.USER_PARTITION_ID;
import static jpcsp.util.Utilities.writeStringZ;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEPointerFunction;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointerFunction;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspIoDrv;
import jpcsp.HLE.kernel.types.pspIoDrvFuncs;
import jpcsp.HLE.kernel.types.pspUsbDriver;
import jpcsp.HLE.kernel.types.pspUsbdDeviceReq;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.memory.ByteArrayMemory;
import jpcsp.scheduler.Scheduler;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

/**
 * Implementation for the PSP flash0:/kd/usbpspcm.prx module.
 * 
 * The following sequence of calls or data exchange is happening:
 *  1. sceKernelLoadModule path=0x09FFEF20('disc0:/PSP_GAME/USRDIR/module/usbpspcm.prx')
 *  1.1. sceUsbbdRegister driver name="USBPSPCommunicationDriver", endpoints=0x4
 *  2. sceUsbStart driverName='USBBusDriver', size=0x0, args=0x00000000
 *  3. sceUsbStart driverName='USBPSPCommunicationDriver', size=0x9, args="UCUS12345"
 *  3.1. sceIoAddDrv driver name="usbpspcm", description="USBPSPCommunicationDriver"
 *  4. sceUsbActivate pid=0x1CB
 *  5. sceIoDevctl devicename="usbpspcm:", cmd=0x3415001, indata=ptr to thread UID, inlen=0x4, outdata=0x00000000, outlen=0x0
 *  6. Receiving USB data 0x41 0x07 0x.... 0x.... 0x0004
 *  6.1. call usbPspcmDriver.recvctl
 *  6.2. sceUsbbdReqRecv size=0x0004, endpointNumber=0x0
 *  6.3. Receiving USB data 0xNNNNNNNN (0xNNNNNNNN seems to be a version number, e.g. 0x02060010 or 0x03000000)
 *  6.4. executing completion function from previous sceUsbbdReqRecv
 *  7. Receiving USB data 0xC1 0x08 0x.... 0x.... 0x.... (request for version?)
 *  7.1. call usbPspcmDriver.recvctl
 *  7.2. sceUsbbdReqSend data=ptr to 0xNNNNNNNN, size=0x4, endpointNumber=0x0 (0xNNNNNNNN seems to be a version number, e.g. 0x02060010 or 0x03000000)
 *  7.3. executing completion function from previous sceUsbbdReqSend
 *  8. Receiving USB data 0x41 0x02 0x.... 0x.... 0x000C
 *  8.1. call usbPspcmDriver.recvctl
 *  8.2. sceUsbbdReqRecv size=0x000C, endpointNumber=0x0
 *  8.3. Receiving USB data 0x00000002 0x00000002 0x00001000
 *  8.4. executing completion function from previous sceUsbbdReqRecv
 *  8.5. sceKernelStartThread UID given in previous sceIoDevctl
 *  9. sceIoDevctl devicename="usbpspcm:", cmd=0x3435005, indata=ptr to 0x0000000C 0x000000FF 0x00000000, inlen=0xC, outdata=buffer, outlen=0x10
 *     BIND request
 *  9.1. sceUsbbdReqSend data=ptr to 0x00000004 0x00 0xFF 0x0000, size=0x0008, endpointNumber=0x3
 *  9.2. executing completion function from previous sceUsbbdReqSend
 *  9.3 Receiving USB data 0xC1 0x04 0x.... 0x.... 0x0000
 *  9.4. call usbPspcmDriver.recvctl
 *  9.5. sceUsbbdReqSend data=ptr to 0x01, size=0x1, endpointNumber=0x0 (0x01 seems to be a success flag)
 *  9.6. executing completion function from previous sceUsbbdReqSend
 *  9.7. sceIoDevctl returning "usbpspcm0:" in outdata
 * 10. sceIoOpen filename="usbpspcm0:", flags=PSP_O_RDWR, permissions=0x0
 * 11. sceIoWrite data=ptr to data, size=0xN
 * 11.1 sceUsbbdReqSend data=ptr to 0x00810000 0x00000008, size=0x8, linked with next request
 *                      data=ptr to data from sceIoWrite, size=size from sceIoWrite, endpointNumber=0x1
 * 11.2 executing completion function #1 from previous sceUsbbdReqSend
 * 11.3 executing completion function #2 from previous sceUsbbdReqSend
 * 11.4 sceIoWrite returning 0xN
 * 12. sceIoRead data=buffer, size=0xN
 * 12.1 sceUsbbdReqRecv size=0x200, endpointNumber=0x2
 * 12.2  Receiving USB data 0x0000, 0x0001, 0x00000010
 * 12.3. executing completion function from previous sceUsbbdReqRecv
 * 12.4. sceUsbbdReqRecv size=0xN, endpointNumber=0x2
 * 12.5  Receiving USB data size=0xN
 * 12.6. executing completion function from previous sceUsbbdReqRecv
 * 12.7. sceIoRead returning 0xN
 * 13. Possibly repeating sceIoRead/sceIoWrite sequences depending on the application protocol
 * 
 * Sequence of calls for ending the connnection:
 * 20. sceIoDevctl devicename="usbpspcm:", cmd=0x3415002, indata=ptr to thread UID, inlen=0x4, outdata=0x00000000, outlen=0x0
 * 21. sceUsbDeactivate
 * 22. sceUsbStop driverName='USBPSPCommunicationDriver', size=0x0, args=0x00000000
 * 23. sceUsbStop driverName='USBBusDriver', size=0x0, args=0x00000000
 * 24. sceUsbbdUnregister driver name="USBPSPCommunicationDriver", endpoints=0x4
 * 25. sceIoClose
 * 
 * The information on the data exchange is partially based on this post:
 *     https://wololo.net/talk/viewtopic.php?p=249571&sid=d27001058e5ea930b293de84a03ab4ec#p249571
 * 
 * @author gid15
 *
 */
public class sceUsbPspcm extends HLEModule {
    public static Logger log = Modules.getLogger("sceUsbPspcm");
    private final UsbCommunicationStub usbCommunicationStub = new UsbCommunicationStub();
    private pspUsbDriver usbDriver;
    private pspIoDrv ioDriver;
    private int registeredThreadUid = -1;
    private boolean registeredThreadStarted;
	private SysMemInfo startThreadParametersSysMemInfo;
	private TPointer startThreadParameters;
	private boolean usbActivated;
	private int simulateIoReadIndex;

    public UsbCommunicationStub getUsbCommunicationStub() {
    	return usbCommunicationStub;
    }

    /**
     * This class is used when flash0:/kd/usbpspcm.prx is loaded.
     * It emulates the required functions for this module to run.
     * 
     * @author gid15
     *
     */
    public static class UsbCommunicationStub implements IAction {
    	private static enum Action {
    		ACTIVATE,
    		REQUEST_VERSION,
    		INITIALIZE_CONNECTION,
    		BIND,
    		TRANSFER,
    		TRANSFER_DATA,
    		RECEIVE_DATA,
    		SEND_DATA_HEADER
    	}

    	private static class UsbCommunicationEndpoint {
    		public final TPointer receiveBuffer = new ByteArrayMemory(new byte[512]).getPointer();
    		public int receiveBufferSize;
    		public int receiveBufferSize2;
    		public pspUsbdDeviceReq pendingReadRequest;
    		public TPointer pendingReadRequestPtr;
    	}

    	private int version;
    	private Action action;
    	private SysMemInfo bufferSysMemInfo;
    	private TPointer buffer;
    	private pspUsbDriver usbPspcmDriver;
    	private SceKernelThreadInfo thread;
    	private UsbCommunicationEndpoint endpoints[];

    	private TPointer getBuffer() {
    		if (bufferSysMemInfo == null) {
    			bufferSysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "sceUsb Buffer", SysMemUserForUser.PSP_SMEM_Low, 8, 0);
    			buffer = new TPointer(Memory.getInstance(), bufferSysMemInfo.addr);
    		}

    		return buffer;
    	}

    	public int hleUsbbdReqRecv(pspUsbDriver usbDriver, pspUsbdDeviceReq deviceReq, TPointer req) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("UsbCommunicationStub.sceUsbbdReqRecv endpnum=0x%X", deviceReq.endp.getValue32(0)));
    		}

    		UsbCommunicationEndpoint endpoint = endpoints[deviceReq.endp.getValue32(0)];

    		if (version == 0) {
    			scheduleAction(usbDriver, Action.REQUEST_VERSION);
    		}

    		// Any data available in the receiveBuffer?
    		receiveData(endpoint, deviceReq, req);

    		return 0;
    	}

    	public int hleUsbbdReqSend(pspUsbDriver usbDriver, pspUsbdDeviceReq deviceReq, TPointer req) {
    		TPointer data = deviceReq.data;

    		if (log.isDebugEnabled()) {
    			log.debug(String.format("UsbCommunicationStub.sceUsbbdReqSend endpnum=0x%X, action=%s", deviceReq.endp.getValue32(0), action));
    		}

    		switch (action) {
    			case REQUEST_VERSION:
    				if (deviceReq.size == 4) {
    					version = data.getValue32(0);
    					scheduleAction(usbDriver, Action.INITIALIZE_CONNECTION);
    				}
    				break;

    			case INITIALIZE_CONNECTION:
    				// Bind request?
    				if (deviceReq.size == 8 && data.getValue32(0) == 0x4) {
    					int unknown0 = data.getUnsignedValue8(4);
    					int unknown1 = data.getUnsignedValue8(5);
    					if (log.isDebugEnabled()) {
    						log.debug(String.format("Bind request unknown0=0x%02X, unknown1=0x%02X", unknown0, unknown1));
    					}
    					scheduleAction(usbDriver, Action.BIND);
    				}
    				break;

    			case BIND:
    				if (deviceReq.size == 1) {
    					int success = data.getUnsignedValue8(0);
    					if (log.isDebugEnabled()) {
    						log.debug(String.format("Bind response success=%d", success));
    					}
    					if (success != 0) {
    						action = Action.TRANSFER;
    					}
    				}
    				break;

    			case TRANSFER:
    				if (deviceReq.size == 8 && data.getValue32(0) == 0x00810000) {
    					action = Action.RECEIVE_DATA;
    				} else if (deviceReq.size == 8 && data.getValue32(0) == 0x00010000) {
    					action = Action.TRANSFER_DATA;
    				}
    				break;

    			case TRANSFER_DATA:
    				sceUsbPspcmModule.simulateIoWrite(data, deviceReq.size);
    				action = Action.TRANSFER;
    				break;

    			case RECEIVE_DATA:
    				sceUsbPspcmModule.simulateIoWrite(data, deviceReq.size);
    				scheduleAction(usbDriver, Action.SEND_DATA_HEADER);
    				break;

    			default:
					break;
    		}

    		return 0;
    	}

    	public int hleUsbActivate(pspUsbDriver usbDriver, int pid) {
    		endpoints = new UsbCommunicationEndpoint[usbDriver.endpoints];
    		for (int i = 0; i < endpoints.length; i++) {
    			endpoints[i] = new UsbCommunicationEndpoint();
    		}

    		scheduleAction(usbDriver, Action.ACTIVATE);

    		return 0;
    	}

    	private void scheduleAction(pspUsbDriver usbDriver, Action action) {
    		this.usbPspcmDriver = usbDriver;
    		this.action = action;
    		this.thread = ThreadManForUserModule.getCurrentThread();
			Emulator.getScheduler().addAction(Scheduler.getNow() + 100000, this);
    	}

    	private void storeResponse(pspUsbdDeviceReq deviceReq, TPointer req, int size) {
			deviceReq.recvsize = size;
			deviceReq.retcode = 0;
			deviceReq.write(req);
    	}

    	@Override
		public void execute() {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("UsbCommunicationStub action=%s", action));
    		}

    		UsbCommunicationEndpoint ctrlEndpoint = endpoints[0];

			TPointer buffer = getBuffer();
    		switch (action) {
    			case ACTIVATE:
    				ctrlEndpoint.receiveBuffer.setValue32(0, 0x03000000); // Version number
    				ctrlEndpoint.receiveBuffer.setValue32(0, 0x02060010); // Version number
    				ctrlEndpoint.receiveBufferSize = 4;
    				buffer.setUnsignedValue8(0, 0x41);
    				buffer.setUnsignedValue8(1, 0x07);
    				buffer.setUnsignedValue16(6, ctrlEndpoint.receiveBufferSize);

    				callReceivedCallback(buffer);
    				break;

    			case REQUEST_VERSION:
            		buffer.setUnsignedValue8(0, 0xC1);
            		buffer.setUnsignedValue8(1, 0x08);

            		callReceivedCallback(buffer);
    				break;

    			case INITIALIZE_CONNECTION:
    				ctrlEndpoint.receiveBuffer.setValue32(0, 0x2);
    				ctrlEndpoint.receiveBuffer.setValue32(4, 0x2);
    				ctrlEndpoint.receiveBuffer.setValue32(8, 0x1000);
    				ctrlEndpoint.receiveBufferSize = 12;
    				buffer.setUnsignedValue8(0, 0x41);
    				buffer.setUnsignedValue8(1, 0x02);
    				buffer.setUnsignedValue16(2, 0x0000); // USB address
    				buffer.setUnsignedValue16(6, ctrlEndpoint.receiveBufferSize);
    				callReceivedCallback(buffer);
    				break;

    			case BIND:
    				ctrlEndpoint.receiveBufferSize = 0;
    				buffer.setUnsignedValue8(0, 0xC1);
    				buffer.setUnsignedValue8(1, 0x04);
    				buffer.setUnsignedValue16(6, ctrlEndpoint.receiveBufferSize);
    				callReceivedCallback(buffer);
    				break;

    			case SEND_DATA_HEADER:
    				TPointer data = new TPointer(endpoints[2].receiveBuffer, 8);
    				int readSize = sceUsbPspcmModule.simulateIoRead(data, 512 - 8);

    				endpoints[2].receiveBuffer.setUnalignedValue16(0, 0);
    				endpoints[2].receiveBuffer.setUnalignedValue16(2, 0x1); // containing 3 flags: 0x1, 0x2, 0x80
    				endpoints[2].receiveBuffer.setValue32(4, readSize); // size of following data
    				endpoints[2].receiveBufferSize = 8;
    				endpoints[2].receiveBufferSize2 = readSize;
    				onDataReceived(endpoints[2]);

    				action = Action.TRANSFER;
    				break;

    			default:
					break;
    		}
		}

    	private void receiveData(UsbCommunicationEndpoint endpoint, pspUsbdDeviceReq deviceReq, TPointer req) {
    		if (endpoint.receiveBufferSize <= 0) {
    			endpoint.pendingReadRequest = deviceReq;
    			endpoint.pendingReadRequestPtr = req;
    		} else {
        		TPointer data = deviceReq.data;
	    		// Copy any data pending in the receiveBuffer
	    		int size = Math.min(endpoint.receiveBufferSize, deviceReq.size);
	    		data.memcpy(endpoint.receiveBuffer, size);
				endpoint.receiveBufferSize -= size;
				storeResponse(deviceReq, req, size);

				if (endpoint.receiveBufferSize == 0 && endpoint.receiveBufferSize2 > 0) {
					endpoint.receiveBuffer.memcpy(endpoint.receiveBufferSize, new TPointer(endpoint.receiveBuffer, size), endpoint.receiveBufferSize2);
					endpoint.receiveBufferSize += endpoint.receiveBufferSize2;
					endpoint.receiveBufferSize2 = 0;
				}

	    		sceUsbBusModule.triggerCompletionFunction(deviceReq);
    		}
    	}

    	private void onDataReceived(UsbCommunicationEndpoint endpoint) {
    		if (endpoint.pendingReadRequest != null) {
    			receiveData(endpoint, endpoint.pendingReadRequest, endpoint.pendingReadRequestPtr);
    		}
    	}

    	private void callReceivedCallback(TPointer buffer) {
			TPointerFunction recvctl = usbPspcmDriver.recvctl;
			int recipient = buffer.getUnsignedValue8(0) & 0x1F;
			if (recvctl != null) {
				recvctl.executeCallback(thread, recipient, 0, buffer.getAddress());
			}
    	}
    }

    private class StartRegisteredThreadAction implements IAction {
		@Override
		public void execute() {
			startRegisteredThread();
		}
    }

    private class IoDevctlCallback extends HLEPointerFunction {
		@Override
		public int executeCallback(int unknownArg0, int deviceName, int cmd, int indata, int inlen, int outdata, int outlen) {
			switch (cmd) {
				case 0x3415001: // Register thread
					registeredThreadStarted = false;
					if (inlen >= 4) {
						registeredThreadUid = getMemory().read32(indata);
						Scheduler.getInstance().addAction(Scheduler.getNow() + 100000, new StartRegisteredThreadAction());
					} else {
						registeredThreadUid = -1;
					}
					break;
				case 0x3415002: // Unregister thread
					registeredThreadStarted = false;
					registeredThreadUid = -1;
					break;
				case 0x3435005: // Bind
					if (outlen >= 11) {
						writeStringZ(getMemory(), outdata, "usbpspcm0:");
					}
					break;
			}

			return 0;
		}
    }

    private class IoOpenCallback extends HLEPointerFunction {
		@Override
		public int executeCallback(int iob) {
			TPointer iobBuffer = new TPointer(getMemory(), iob);
			int fsNum = iobBuffer.getValue32(4);
			if (log.isDebugEnabled()) {
				log.debug(String.format("IoOpenCallback on usbpspcm%d:", fsNum));
			}

			return 0;
		}
    }

    private class IoIoctlCallback extends HLEPointerFunction {
		@Override
		public int executeCallback(int iob, int cmd, int indata, int inlen, int outdata, int outlen) {
			switch (cmd) {
				case 0x3416102: // Set read/write mode?
					if (inlen >= 4) {
						int mode = getMemory().read32(indata);
						switch (mode) {
							case 0: log.debug("IoIoctlCallback set unknown mode"); break;
							case 1: log.debug("IoIoctlCallback set read mode"); break;
							case 2: log.debug("IoIoctlCallback set write mode"); break;
							default: log.error(String.format("IoIoctlCallback unknown mode 0x%X", mode)); break;
						}
					}
					break;
			}

			return 0;
		}
    }

    private class IoWriteCallback extends HLEPointerFunction {
		@Override
		public int executeCallback(int iob, int dataAddr, int size) {
			return simulateIoWrite(new TPointer(getMemory(), dataAddr), size);
		}
    }

    private class IoReadCallback extends HLEPointerFunction {
		@Override
		public int executeCallback(int iob, int dataAddr, int size) {
			return simulateIoRead(new TPointer(getMemory(), dataAddr), size);
		}
    }

    private int simulateIoRead(TPointer data, int size) {
    	debugMemory(data.getAddress(), size);

    	simulateIoReadIndex++;
    	String simulateIoReadProperty = Settings.getInstance().readString(String.format("sceUsbPspcm.simulateIoRead.%d", simulateIoReadIndex));
    	simulateIoReadProperty = simulateIoReadProperty.replace(" ", "");
    	int result = 0;
    	for (int i = 0; i < simulateIoReadProperty.length() - 1; i += 2, result++) {
    		String byteString = simulateIoReadProperty.substring(i, i + 2);
    		data.setUnsignedValue8(result, Integer.parseInt(byteString, 16));
    	}

		if (log.isInfoEnabled()) {
			log.info(String.format("simulateIoRead returning 0x%X: %s", result, Utilities.getMemoryDump(data, result)));
		}

		return result;
    }

    private int simulateIoWrite(TPointer data, int size) {
		if (log.isInfoEnabled()) {
			log.info(String.format("simulateIoWrite size=0x%X, %s", size, Utilities.getMemoryDump(data, size)));
		}

		return size;
    }

    @Override
	public void load() {
		if (usbDriver == null) {
			usbDriver = new pspUsbDriver();
			usbDriver.name = "USBPSPCommunicationDriver";
			usbDriver.endpoints = 4;

			sceUsbBusModule.hleUsbbdRegister(usbDriver);
		}

		if (ioDriver == null) {
			ioDriver = new pspIoDrv();
			ioDriver.name = "usbpspcm";
			ioDriver.ioDrvFuncs = new pspIoDrvFuncs();
			ioDriver.ioDrvFuncs.ioDevctl = new IoDevctlCallback();
			ioDriver.ioDrvFuncs.ioOpen = new IoOpenCallback();
			ioDriver.ioDrvFuncs.ioIoctl = new IoIoctlCallback();
			ioDriver.ioDrvFuncs.ioWrite = new IoWriteCallback();
			ioDriver.ioDrvFuncs.ioRead = new IoReadCallback();

			IoFileMgrForKernelModule.hleIoAddDrv(ioDriver);
		}

		super.load();
	}

	@Override
	public void unload() {
		if (usbDriver != null) {
			sceUsbBusModule.hleUsbbdUnregister(usbDriver);
			usbDriver = null;
		}

		if (ioDriver != null) {
			IoFileMgrForKernelModule.hleIoDelDrv(ioDriver.name);
			ioDriver = null;
		}

		super.unload();
	}

	private void startRegisteredThread() {
		if (usbActivated && !registeredThreadStarted && registeredThreadUid >= 0) {
			if (startThreadParameters == null) {
				startThreadParametersSysMemInfo = Modules.SysMemUserForUserModule.malloc(USER_PARTITION_ID, "sceUsbPspcm Registered Thread Parameters", PSP_SMEM_Low, 8, 0);
				startThreadParameters = new TPointer(getMemory(), startThreadParametersSysMemInfo.addr);
			}

			SceKernelThreadInfo registeredThread = ThreadManForUserModule.getThreadById(registeredThreadUid);
			if (registeredThread != null) {
				startThreadParameters.setValue32(0, 0);
				startThreadParameters.setValue32(4, 0x80 | 0x01); // Flags meaning that the USB is in connected state
				ThreadManForUserModule.hleKernelStartThread(registeredThread, 8, startThreadParameters, 0);

				registeredThreadStarted = true;
			}
		}
	}

	public void onUsbActivate(int pid) {
		if (usbDriver != null) {
			usbActivated = true;
			startRegisteredThread();
		} else {
			pspUsbDriver usbDriver = Modules.sceUsbBusModule.getRegisteredUsbDriver("USBPSPCommunicationDriver");
			if (usbDriver != null) {
				sceUsbPspcmModule.getUsbCommunicationStub().hleUsbActivate(usbDriver, pid);
			}
		}
	}

	public void onUsbDeactivate() {
		usbActivated = false;
		registeredThreadStarted = false;
	}
}
