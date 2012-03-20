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
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

public class sceNetAdhocMatching extends HLEModule {
    protected static Logger log = Modules.getLogger("sceNetAdhocMatching");
    protected int currentMatchingId = 1;

    @Override
    public String getName() {
        return "sceNetAdhocMatching";
    }

    /**
     * Initialise the Adhoc matching library
     *
     * @param memsize - Internal memory pool size. Lumines uses 0x20000
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x2A2A1E07, version = 150)
    public void sceNetAdhocMatchingInit(Processor processor) {
        CpuState cpu = processor.cpu;

        int poolSize = cpu.gpr[4];

        log.warn("IGNORING: sceNetAdhocMatchingInit: poolSize=0x" + Integer.toHexString(poolSize));

        cpu.gpr[2] = 0;
    }

    /**
     * Terminate the Adhoc matching library
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x7945ECDA, version = 150)
    public void sceNetAdhocMatchingTerm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("IGNORING: sceNetAdhocMatchingTerm");

        cpu.gpr[2] = 0;
    }

    /**
     * Create an Adhoc matching object
     *
     * @param mode - One of ::pspAdhocMatchingModes
     * @param maxpeers - Maximum number of peers to match (only used when mode is PSP_ADHOC_MATCHING_MODE_HOST)
     * @param port - Port. Lumines uses 0x22B
     * @param bufsize - Receiving buffer size
     * @param hellodelay - Hello message send delay in microseconds (only used when mode is PSP_ADHOC_MATCHING_MODE_HOST or PSP_ADHOC_MATCHING_MODE_PTP)
     * @param pingdelay - Ping send delay in microseconds. Lumines uses 0x5B8D80 (only used when mode is PSP_ADHOC_MATCHING_MODE_HOST or PSP_ADHOC_MATCHING_MODE_PTP)
     * @param initcount - Initial count of the of the resend counter. Lumines uses 3
     * @param msgdelay - Message send delay in microseconds
     * @param callback - Callback to be called for matching
     *
     * @return ID of object on success, < 0 on error.
     */
    @HLEFunction(nid = 0xCA5EDA6F, version = 150)
    public int sceNetAdhocMatchingCreate(int mode, int maxPeers, int port, int bufSize, int helloDelay, int pingDelay, int initCount, int msgDelay, @CanBeNull TPointer callback) {
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingCreate mode=%d, maxPeers=%d, port=%d, bufSize=%d, helloDelay=%d, pingDelay=%d, initCount=%d, msgDelay=%d, callback=%s", mode, maxPeers, port, bufSize, helloDelay, pingDelay, initCount, msgDelay, callback));

        return currentMatchingId++;
    }

    /**
     * Start a matching object
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param evthpri - Priority of the event handler thread. Lumines uses 0x10
     * @param evthstack - Stack size of the event handler thread. Lumines uses 0x2000
     * @param inthpri - Priority of the input handler thread. Lumines uses 0x10
     * @param inthstack - Stack size of the input handler thread. Lumines uses 0x2000
     * @param optlen - Size of hellodata
     * @param optdata - Pointer to block of data passed to callback
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x93EF3843, version = 150)
    public void sceNetAdhocMatchingStart(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingStart");

        cpu.gpr[2] = 0;
    }

    /**
     * Stop a matching object
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x32B156B3, version = 150)
    public int sceNetAdhocMatchingStop(int matchingId) {
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingStop matchingId=%d", matchingId));

        return 0;
    }

    /**
     * Delete an Adhoc matching object
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xF16EAF4F, version = 150)
    public int sceNetAdhocMatchingDelete(int matchingId) {
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingDelete matchingId=%d", matchingId));

        return 0;
    }

    /**
     * Send data to a matching target
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - The MAC address to send the data to
     * @param datalen - Length of the data
     * @param data - Pointer to the data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xF79472D7, version = 150)
    public void sceNetAdhocMatchingSendData(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingSendData");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Abort a data send to a matching target
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - The MAC address to send the data to
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xEC19337D, version = 150)
    public void sceNetAdhocMatchingAbortSendData(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingAbortSendData");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Select a matching target
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - MAC address to select
     * @param optlen - Optional data length
     * @param optdata - Pointer to the optional data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x5E3D4B79, version = 150)
    public void sceNetAdhocMatchingSelectTarget(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingSelectTarget");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Cancel a matching target
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - The MAC address to cancel
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xEA3C6108, version = 150)
    public void sceNetAdhocMatchingCancelTarget(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingCancelTarget");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Cancel a matching target (with optional data)
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - The MAC address to cancel
     * @param optlen - Optional data length
     * @param optdata - Pointer to the optional data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x8F58BEDF, version = 150)
    public void sceNetAdhocMatchingCancelTargetWithOpt(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingCancelTargetWithOpt");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get the optional hello message
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param optlen - Length of the hello data
     * @param optdata - Pointer to the hello data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xB5D96C2A, version = 150)
    public void sceNetAdhocMatchingGetHelloOpt(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingGetHelloOpt");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Set the optional hello message
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param optlen - Length of the hello data
     * @param optdata - Pointer to the hello data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xB58E61B7, version = 150)
    public void sceNetAdhocMatchingSetHelloOpt(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingSetHelloOpt");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get a list of matching members
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param length - The length of the list.
     * @param buf - An allocated area of size length.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xC58BCD9E, version = 150)
    public int sceNetAdhocMatchingGetMembers(int matchingId, TPointer32 lengthAddr, @CanBeNull TPointer buf) {
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingGetMembers matchingId=%d, lengthAddr=%s(%d), buf=%s", matchingId, lengthAddr.toString(), lengthAddr.getValue(), buf.toString()));

        lengthAddr.setValue(0);

        return 0;
    }

    /**
     * Get the status of the memory pool used by the matching library
     *
     * @param poolstat - A ::pspAdhocPoolStat.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x9C5CFB7D, version = 150)
    public void sceNetAdhocMatchingGetPoolStat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingGetPoolStat");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get the maximum memory usage by the matching library
     *
     * @return The memory usage on success, < 0 on error.
     */
    @HLEFunction(nid = 0x40F8F435, version = 150)
    public void sceNetAdhocMatchingGetPoolMaxAlloc(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingGetPoolMaxAlloc");

        cpu.gpr[2] = 0xDEADC0DE;
    }

}