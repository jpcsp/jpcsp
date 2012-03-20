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
package jpcsp.HLE.modules150;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.util.HashMap;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceNetAdhocctl extends HLEModule {

    protected static Logger log = Modules.getLogger("sceNetAdhocctl");

    @Override
    public String getName() {
        return "sceNetAdhocctl";
    }

    public static final int PSP_ADHOCCTL_EVENT_ERROR = 0;
    public static final int PSP_ADHOCCTL_EVENT_CONNECTED = 1;
    public static final int PSP_ADHOCCTL_EVENT_DISCONNECTED = 2;
    public static final int PSP_ADHOCCTL_EVENT_SCAN = 3;
    public static final int PSP_ADHOCCTL_EVENT_GAME = 4;
    public static final int PSP_ADHOCCTL_EVENT_DISCOVER = 5;
    public static final int PSP_ADHOCCTL_EVENT_WOL = 6;
    public static final int PSP_ADHOCCTL_EVENT_WOL_INTERRUPTED = 7;

    public static final int PSP_ADHOCCTL_STATE_DISCONNECTED = 0;
    public static final int PSP_ADHOCCTL_STATE_CONNECTED = 1;
    public static final int PSP_ADHOCCTL_STATE_SCAN = 2;
    public static final int PSP_ADHOCCTL_STATE_GAME = 3;
    public static final int PSP_ADHOCCTL_STATE_DISCOVER = 4;
    public static final int PSP_ADHOCCTL_STATE_WOL = 5;

    protected int adhocctlCurrentState;
    protected String adhocctlCurrentGroup;

    private HashMap<Integer, AdhocctlHandler> adhocctlHandlerMap = new HashMap<Integer, AdhocctlHandler>();
    private int adhocctlHandlerCount = 0;

    protected class AdhocctlHandler {

        private int entryAddr;
        private int currentEvent;
        private int currentError;
        private int currentArg;
        private int handle;

        private AdhocctlHandler(int num, int addr, int arg) {
            entryAddr = addr;
            currentArg = arg;
            handle = makeFakeAdhocctHandle(num);
        }

        protected void triggerAdhocctlHandler() {
            SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
            if (thread != null) {
                Modules.ThreadManForUserModule.executeCallback(thread, entryAddr, null, true, currentEvent, currentError, currentArg);
            }
        }

        protected int makeFakeAdhocctHandle(int num) {
            return 0x0000AD00 | (num & 0xFFFF);
        }

        protected int getHandle() {
            return handle;
        }

        protected void setEvent(int event) {
            currentEvent = event;
        }

        protected void setError(int error) {
            currentError = error;
        }
    }

    protected void notifyAdhocctlHandler(int event, int error) {
        for(AdhocctlHandler handler : adhocctlHandlerMap.values()) {
            handler.setEvent(event);
            handler.setError(error);
            handler.triggerAdhocctlHandler();
        }
    }

    /**
     * Initialise the Adhoc control library
     *
     * @param stacksize - Stack size of the adhocctl thread. Set to 0x2000
     * @param priority - Priority of the adhocctl thread. Set to 0x30
     * @param product - Pass a filled in ::productStruct
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xE26F226E, version = 150)
    public void sceNetAdhocctlInit(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int threadStack = cpu.gpr[4];
        int threadPri = cpu.gpr[5];
        int adhocIDAddr = cpu.gpr[6];

        log.warn("PARTIAL: sceNetAdhocctlInit (threadStack=0x" + Integer.toHexString(threadStack) + ", threadPri=0x" + Integer.toHexString(threadPri) + ", adhocIDAddr=0x" + Integer.toHexString(adhocIDAddr) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(adhocIDAddr)) {
            int adhocType = mem.read32(adhocIDAddr); // 0 - Commercial type / 1 - Debug type.
            String adhocParams = Utilities.readStringNZ(mem, adhocIDAddr + 4, 9);
            log.info("Found Adhoc ID data: type=" + adhocType + ", params=" + adhocParams);
        }
        cpu.gpr[2] = 0;
    }

    /**
     * Terminate the Adhoc control library
     *
     * @return 0 on success, < on error.
     */
    @HLEFunction(nid = 0x9D689E13, version = 150)
    public void sceNetAdhocctlTerm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("PARTIAL: sceNetAdhocctlTerm");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    /**
     * Connect to the Adhoc control
     *
     * @param name - The name of the connection (maximum 8 alphanumeric characters).
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x0AD043ED, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlConnect(TPointer groupNameAddr) {
        Memory mem = Memory.getInstance();

        String groupName = null;
        groupName = Utilities.readStringNZ(mem, groupNameAddr.getAddress(), 8);

        log.warn(String.format("PARTIAL: sceNetAdhocctlConnect groupNameAddr=%s('%s')", groupNameAddr.toString(), groupName));

        adhocctlCurrentGroup = groupName;
        adhocctlCurrentState = PSP_ADHOCCTL_STATE_CONNECTED;
        notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_CONNECTED, 0);

        return 0;
    }

    /**
     * Connect to the Adhoc control (as a host)
     *
     * @param name - The name of the connection (maximum 8 alphanumeric characters).
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xEC0635C1, version = 150)
    public void sceNetAdhocctlCreate(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int groupNameAddr = cpu.gpr[4];

        log.warn("PARTIAL: sceNetAdhocctlCreate (groupNameAddr=0x" + Integer.toHexString(groupNameAddr) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(groupNameAddr)) {
            String groupName = Utilities.readStringNZ(mem, groupNameAddr, 8);
            adhocctlCurrentGroup = groupName;
        }
        adhocctlCurrentState = PSP_ADHOCCTL_STATE_CONNECTED;
        notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_CONNECTED, 0);
        cpu.gpr[2] = 0;
    }

    /**
     * Connect to the Adhoc control (as a client)
     *
     * @param scaninfo - A valid ::SceNetAdhocctlScanInfo struct that has been filled by sceNetAchocctlGetScanInfo
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x5E7F79C9, version = 150)
    public void sceNetAdhocctlJoin(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int scanInfoAddr = cpu.gpr[4];

        log.warn("PARTIAL: sceNetAdhocctlJoin (scanInfoAddr=0x" + Integer.toHexString(scanInfoAddr) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(scanInfoAddr)) {
            // IBSS Data field.
            int nextAddr = mem.read32(scanInfoAddr);  // Next group data.
            int ch = mem.read32(scanInfoAddr + 4);
            String groupName = Utilities.readStringNZ(mem, scanInfoAddr + 8, 8);
            String bssID = Utilities.readStringNZ(mem, scanInfoAddr + 16, 6);
            int mode = mem.read32(scanInfoAddr + 24);

            if (log.isDebugEnabled()) {
            	log.debug(String.format("sceNetAdhocctlJoin nextAddr 0x%08X, ch %d, groupName '%s', bssID '%s', mode %d", nextAddr, ch, groupName, bssID, mode));
            }
            adhocctlCurrentGroup = groupName;
        }
        adhocctlCurrentState = PSP_ADHOCCTL_STATE_CONNECTED;
        notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_CONNECTED, 0);
        cpu.gpr[2] = 0;
    }

    /**
     * Scan the adhoc channels
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x08FFF7A0, version = 150)
    public void sceNetAdhocctlScan(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("PARTIAL: sceNetAdhocctlScan");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        adhocctlCurrentState = PSP_ADHOCCTL_STATE_SCAN;
        notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_SCAN, 0);
        cpu.gpr[2] = 0;
    }

    /**
     * Disconnect from the Adhoc control
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x34401D65, version = 150)
    public void sceNetAdhocctlDisconnect(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("PARTIAL: sceNetAdhocctlDisconnect");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        adhocctlCurrentState = PSP_ADHOCCTL_STATE_DISCONNECTED;
        notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_DISCONNECTED, 0);
        cpu.gpr[2] = 0;
    }

    /**
     * Register an adhoc event handler
     *
     * @param handler - The event handler.
     * @param unknown - Pass NULL.
     *
     * @return Handler id on success, < 0 on error.
     */
    @HLEFunction(nid = 0x20B317A0, version = 150)
    public void sceNetAdhocctlAddHandler(Processor processor) {
        CpuState cpu = processor.cpu;

        int adhocctlHandlerAddr = cpu.gpr[4];
        int adhocctlHandlerArg = cpu.gpr[5];

        log.warn("PARTIAL: sceNetAdhocctlAddHandler (adhocctlHandlerAddr=0x" + Integer.toHexString(adhocctlHandlerAddr) + ", adhocctlHandlerArg=0x" + Integer.toHexString(adhocctlHandlerArg) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        AdhocctlHandler adhocctlHandler = new AdhocctlHandler(adhocctlHandlerCount++, adhocctlHandlerAddr, adhocctlHandlerArg);
        int handle = adhocctlHandler.getHandle();
        adhocctlHandlerMap.put(handle, adhocctlHandler);
        cpu.gpr[2] = handle;
    }

    /**
     * Delete an adhoc event handler
     *
     * @param id - The handler id as returned by sceNetAdhocctlAddHandler.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x6402490B, version = 150)
    public void sceNetAdhocctlDelHandler(Processor processor) {
       CpuState cpu = processor.cpu;

        int adhocctlHandler = cpu.gpr[4];

        log.warn("PARTIAL: sceNetAdhocctlDelHandler (adhocctlHandler=0x" + Integer.toHexString(adhocctlHandler) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        adhocctlHandlerMap.remove(adhocctlHandler);
        cpu.gpr[2] = 0;
    }

    /**
     * Get the state of the Adhoc control
     *
     * @param event - Pointer to an integer to receive the status. Can continue when it becomes 1.
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x75ECD386, version = 150)
    public void sceNetAdhocctlGetState(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int stateAddr = cpu.gpr[4];

        log.warn("PARTIAL: sceNetAdhocctlGetState (stateAddr=0x" + Integer.toHexString(stateAddr) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(stateAddr)) {
            mem.write32(stateAddr, adhocctlCurrentState);
        }
        cpu.gpr[2] = 0;
    }

    /**
     * Get the adhoc ID
     *
     * @param product - A pointer to a  ::productStruct
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x362CBE8F, version = 150)
    public void sceNetAdhocctlGetAdhocId(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetAdhocId");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get a list of peers
     *
     * @param length - The length of the list.
     * @param buf - An allocated area of size length.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xE162CB14, version = 150)
    public int sceNetAdhocctlGetPeerList(TPointer32 lengthAddr, @CanBeNull TPointer buf) {
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocctlGetPeerList length=%s(%d), buf=%s", lengthAddr.toString(), lengthAddr.getValue(), buf.toString()));

        lengthAddr.setValue(0);

        return 0;
    }

    /**
     * Get peer information
     *
     * @param mac - The mac address of the peer.
     * @param size - Size of peerinfo.
     * @param peerinfo - Pointer to store the information.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x8DB83FDC, version = 150)
    public void sceNetAdhocctlGetPeerInfo(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetPeerInfo");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get mac address from nickname
     *
     * @param nickname - The nickname.
     * @param length - The length of the list.
     * @param buf - An allocated area of size length.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x99560ABE, version = 150)
    public void sceNetAdhocctlGetAddrByName(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetAddrByName");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get nickname from a mac address
     *
     * @param mac - The mac address.
     * @param nickname - Pointer to a char buffer where the nickname will be stored.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x8916C003, version = 150)
    public void sceNetAdhocctlGetNameByAddr(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetNameByAddr");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get Adhocctl parameter
     *
     * @param params - Pointer to a ::SceNetAdhocctlParams
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xDED9D28E, version = 150)
    public void sceNetAdhocctlGetParameter(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetParameter");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get the results of a scan
     *
     * @param length - The length of the list.
     * @param buf - An allocated area of size length.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x81AEE1BE, version = 150)
    public void sceNetAdhocctlGetScanInfo(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetScanInfo");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Connect to the Adhoc control game mode (as a host)
     *
     * @param name - The name of the connection (maximum 8 alphanumeric characters).
     * @param unknown - Pass 1.
     * @param num - The total number of players (including the host).
     * @param macs - A pointer to a list of the participating mac addresses, host first, then clients.
     * @param timeout - Timeout in microseconds.
     * @param unknown2 - pass 0.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xA5C055CE, version = 150)
    public void sceNetAdhocctlCreateEnterGameMode(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlCreateEnterGameMode");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xB0B80E80, version = 150)
    public void sceNetAdhocctlCreateEnterGameModeMin(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlCreateEnterGameModeMin");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Connect to the Adhoc control game mode (as a client)
     *
     * @param name - The name of the connection (maximum 8 alphanumeric characters).
     * @param hostmac - The mac address of the host.
     * @param timeout - Timeout in microseconds.
     * @param unknown - pass 0.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x1FF89745, version = 150)
    public void sceNetAdhocctlJoinEnterGameMode(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlJoinEnterGameMode");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Exit game mode.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xCF8E084D, version = 150)
    public void sceNetAdhocctlExitGameMode(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlExitGameMode");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get game mode information
     *
     * @param gamemodeinfo - Pointer to store the info.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x5A014CE0, version = 150)
    public void sceNetAdhocctlGetGameModeInfo(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetGameModeInfo");

        cpu.gpr[2] = 0xDEADC0DE;
    }

}