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
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

public class sceNetAdhoc extends HLEModule {

    protected static Logger log = Modules.getLogger("sceNetAdhoc");

    @Override
    public String getName() {
        return "sceNetAdhoc";
    }

    /**
     * Initialise the adhoc library.
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xE1D621D7, version = 150)
    public void sceNetAdhocInit(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("IGNORING: sceNetAdhocInit");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    /**
     * Terminate the adhoc library
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xA62C6F57, version = 150)
    public void sceNetAdhocTerm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("IGNORING: sceNetAdhocTerm");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x7A662D6B, version = 150)
    public void sceNetAdhocPollSocket(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPollSocket");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x73BFD52D, version = 150)
    public void sceNetAdhocSetSocketAlert(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocSetSocketAlert");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x4D2CE199, version = 150)
    public void sceNetAdhocGetSocketAlert(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGetSocketAlert");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Create a PDP object.
     *
     * @param mac - Your MAC address (from sceWlanGetEtherAddr)
     * @param port - Port to use, lumines uses 0x309
     * @param unk2 - Unknown, lumines sets to 0x400
     * @param unk3 - Unknown, lumines sets to 0
     *
     * @return The ID of the PDP object (< 0 on error)
     */
    @HLEFunction(nid = 0x6F92741B, version = 150)
    public int sceNetAdhocPdpCreate(int macAddr, int port, int unk2, int unk3) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr);
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocPdpCreate macAddr=0x%08X(%s), port=%d, unk2=%d, unk3=%d", macAddr, macAddress.toString(), port, unk2, unk3));

        return 1;
    }

    /**
     * Send a PDP packet to a destination
     *
     * @param id - The ID as returned by ::sceNetAdhocPdpCreate
     * @param destMacAddr - The destination MAC address, can be set to all 0xFF for broadcast
     * @param port - The port to send to
     * @param data - The data to send
     * @param len - The length of the data.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return Bytes sent, < 0 on error
     */
    @HLEFunction(nid = 0xABED3790, version = 150)
    public void sceNetAdhocPdpSend(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPdpSend");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Receive a PDP packet
     *
     * @param id - The ID of the PDP object, as returned by ::sceNetAdhocPdpCreate
     * @param srcMacAddr - Buffer to hold the source mac address of the sender
     * @param port - Buffer to hold the port number of the received data
     * @param data - Data buffer
     * @param dataLength - The length of the data buffer
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return Number of bytes received, < 0 on error.
     */
    @HLEFunction(nid = 0xDFE53E03, version = 150)
    public int sceNetAdhocPdpRecv(int id, int srcMacAddr, int portAddr, int data, int dataLengthAddr, int timeout, int nonblock) {
    	Memory mem = Memory.getInstance();
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocPdpRecv id=%d, srcMacAddr=0x%08X, portAddr=0x%08X, data=0x%08X, dataLengthAddr=0x%08X(%d), timeout=%d, nonblock=%d", id, srcMacAddr, portAddr, data, dataLengthAddr, mem.read32(dataLengthAddr), timeout, nonblock));

        return SceKernelErrors.ERROR_NET_ADHOC_NO_DATA_AVAILABLE;
    }

    /**
     * Delete a PDP object.
     *
     * @param id - The ID returned from ::sceNetAdhocPdpCreate
     * @param unk1 - Unknown, set to 0
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x7F27BB5E, version = 150)
    public void sceNetAdhocPdpDelete(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPdpDelete");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get the status of all PDP objects
     *
     * @param size - Pointer to the size of the stat array (e.g 20 for one structure)
     * @param stat - Pointer to a list of ::pspStatStruct structures.
     *
     * typedef struct pdpStatStruct
     * {
     *    struct pdpStatStruct *next; // Pointer to next PDP structure in list
     *    int pdpId;                  // pdp ID
     *    unsigned char mac[6];       // MAC address
     *    unsigned short port;        // Port
     *    unsigned int rcvdData;      // Bytes received
     * } pdpStatStruct
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xC7C1FC57, version = 150)
    public int sceNetAdhocGetPdpStat(TPointer32 sizeAddr, @CanBeNull TPointer buf) {
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocGetPdpStat sizeAddr=%s(%d), buf=%s", sizeAddr.toString(), sizeAddr.getValue(), buf.toString()));

        sizeAddr.setValue(0);

        return 0;
    }

    /**
     * Open a PTP connection
     *
     * @param srcmac - Local mac address.
     * @param srcport - Local port.
     * @param destmac - Destination mac.
     * @param destport - Destination port
     * @param bufsize - Socket buffer size
     * @param delay - Interval between retrying (microseconds).
     * @param count - Number of retries.
     * @param unk1 - Pass 0.
     *
     * @return A socket ID on success, < 0 on error.
     */
    @HLEFunction(nid = 0x877F6D66, version = 150)
    public void sceNetAdhocPtpOpen(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpOpen");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Wait for connection created by sceNetAdhocPtpOpen()
     *
     * @param id - A socket ID.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xFC6FC07B, version = 150)
    public void sceNetAdhocPtpConnect(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpConnect");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Wait for an incoming PTP connection
     *
     * @param srcmac - Local mac address.
     * @param srcport - Local port.
     * @param bufsize - Socket buffer size
     * @param delay - Interval between retrying (microseconds).
     * @param count - Number of retries.
     * @param queue - Connection queue length.
     * @param unk1 - Pass 0.
     *
     * @return A socket ID on success, < 0 on error.
     */
    @HLEFunction(nid = 0xE08BDAC1, version = 150)
    public void sceNetAdhocPtpListen(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpListen");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Accept an incoming PTP connection
     *
     * @param id - A socket ID.
     * @param mac - Connecting peers mac.
     * @param port - Connecting peers port.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x9DF81198, version = 150)
    public void sceNetAdhocPtpAccept(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpAccept");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Send data
     *
     * @param id - A socket ID.
     * @param data - Data to send.
     * @param datasize - Size of the data.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return 0 success, < 0 on error.
     */
    @HLEFunction(nid = 0x4DA4C788, version = 150)
    public void sceNetAdhocPtpSend(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpSend");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Receive data
     *
     * @param id - A socket ID.
     * @param data - Buffer for the received data.
     * @param datasize - Size of the data received.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x8BEA2B3E, version = 150)
    public void sceNetAdhocPtpRecv(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpRecv");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Wait for data in the buffer to be sent
     *
     * @param id - A socket ID.
     * @param timeout - Timeout in microseconds.
     * @param nonblock - Set to 0 to block, 1 for non-blocking.
     *
     * @return A socket ID on success, < 0 on error.
     */
    @HLEFunction(nid = 0x9AC2EEAC, version = 150)
    public void sceNetAdhocPtpFlush(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpFlush");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Close a socket
     *
     * @param id - A socket ID.
     * @param unk1 - Pass 0.
     *
     * @return A socket ID on success, < 0 on error.
     */
    @HLEFunction(nid = 0x157E6225, version = 150)
    public void sceNetAdhocPtpClose(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpClose");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get the status of all PTP objects
     *
     * @param size - Pointer to the size of the stat array (e.g 20 for one structure)
     * @param stat - Pointer to a list of ::ptpStatStruct structures.
     *
     * typedef struct ptpStatStruct
     * {
     *    struct ptpStatStruct *next; // Pointer to next PTP structure in list
     *    int ptpId;                  // ptp ID
     *    unsigned char mac[6];       // MAC address
     *    unsigned char peermac[6];   // Peer MAC address
     *    unsigned short port;        // Port
     *    unsigned short peerport;    // Peer Port
     *    unsigned int sentData;      // Bytes sent
     *    unsigned int rcvdData;      // Bytes received
     *    int unk1;                   // Unknown
     * } ptpStatStruct;
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xB9685118, version = 150)
    public void sceNetAdhocGetPtpStat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGetPtpStat");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Create own game object type data.
     *
     * @param data - A pointer to the game object data.
     * @param size - Size of the game data.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x7F75C338, version = 150)
    public void sceNetAdhocGameModeCreateMaster(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeCreateMaster");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Create peer game object type data.
     *
     * @param mac - The mac address of the peer.
     * @param data - A pointer to the game object data.
     * @param size - Size of the game data.
     *
     * @return The id of the replica on success, < 0 on error.
     */
    @HLEFunction(nid = 0x3278AB0C, version = 150)
    public void sceNetAdhocGameModeCreateReplica(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeCreateReplica");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Update own game object type data.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x98C204C8, version = 150)
    public void sceNetAdhocGameModeUpdateMaster(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeUpdateMaster");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Update peer game object type data.
     *
     * @param id - The id of the replica returned by sceNetAdhocGameModeCreateReplica.
     * @param unk1 - Pass 0.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xFA324B4E, version = 150)
    public void sceNetAdhocGameModeUpdateReplica(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeUpdateReplica");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Delete own game object type data.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xA0229362, version = 150)
    public void sceNetAdhocGameModeDeleteMaster(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeDeleteMaster");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Delete peer game object type data.
     *
     * @param id - The id of the replica.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x0B2228E9, version = 150)
    public void sceNetAdhocGameModeDeleteReplica(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeDeleteReplica");

        cpu.gpr[2] = 0xDEADC0DE;
    }

}