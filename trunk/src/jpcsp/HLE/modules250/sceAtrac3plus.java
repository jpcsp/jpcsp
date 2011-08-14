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
package jpcsp.HLE.modules250;

import jpcsp.HLE.HLEFunction;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

public class sceAtrac3plus extends jpcsp.HLE.modules150.sceAtrac3plus {
    @Override
    public String getName() { return "sceAtrac3plus"; }

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    public void sceAtracGetOutputChannel(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int atID = cpu.gpr[4];
        int outputChannelAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetOutputChannel: atracID = %d, outputChannelAddr = 0x%08X", atID, outputChannelAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetOutputChannel: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            if (Memory.isAddressGood(outputChannelAddr)) {
                mem.write32(outputChannelAddr, 2);
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracIsSecondBufferNeeded(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracIsSecondBufferNeeded atracId=%d", atID));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetOutputChannel: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            // -1 -> Error.
            // 0 -> Second buffer isn't needed.
            // 1 -> Second buffer is needed.
            cpu.gpr[2] = atracIDs.get(atID).isSecondBufferNeeded() ? 1 : 0;
        }
    }

    public void sceAtracReinit(Processor processor) {
        CpuState cpu = processor.cpu;

        int at3IDNum = cpu.gpr[4];
        int at3plusIDNum = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracReinit at3IDNum=%d at3plusIDNum=%d", at3IDNum, at3plusIDNum));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (at3IDNum + at3plusIDNum * 2 > 6) {
            // The total ammount of AT3 IDs and AT3+ IDs (x2) can't be superior to 6.
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_NO_ID;
        } else {
            hleAtracReinit(at3IDNum, at3plusIDNum);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracGetBufferInfoForResetting(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4];
        int sample = cpu.gpr[5];
        int bufferInfoAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracGetBufferInfoForResetting atracID=%d, sample=%d, bufferInfoAddr=0x%08x", atID, sample, bufferInfoAddr));
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracGetOutputChannel: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).getBufferInfoForReseting(sample, bufferInfoAddr);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracSetMOutHalfwayBuffer(Processor processor) {
        CpuState cpu = processor.cpu;

        int atID = cpu.gpr[4] & atracIDMask;
        int MOutHalfBuffer = cpu.gpr[5];
        int readSize = cpu.gpr[6];
        int MOutHalfBufferSize = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetMOutHalfwayBuffer: atID = %d, buffer = 0x%08X, readSize = 0x%08X, bufferSize = 0x%08X", atID, MOutHalfBuffer, readSize, MOutHalfBufferSize));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!atracIDs.containsKey(atID)) {
            log.warn("sceAtracSetMOutHalfwayBuffer: bad atracID= " + atID);
            cpu.gpr[2] = SceKernelErrors.ERROR_ATRAC_BAD_ID;
        } else {
            atracIDs.get(atID).setData(MOutHalfBuffer, readSize, MOutHalfBufferSize, false);
            cpu.gpr[2] = 0;
        }
    }

    public void sceAtracSetMOutData(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented function sceAtracSetMOutData "
    			+ String.format("%08x %08x %08x %08x %08x %08x",
    					cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]));

        cpu.gpr[2] = 0;
    }

    public void sceAtracSetMOutDataAndGetID(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented function sceAtracSetMOutDataAndGetID "
    			+ String.format("%08x %08x %08x %08x %08x %08x",
    					cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]));

        cpu.gpr[2] = 0;
    }

    public void sceAtracSetMOutHalfwayBufferAndGetID(Processor processor) {
        CpuState cpu = processor.cpu;

        int MOutHalfBuffer = cpu.gpr[4];
        int readSize = cpu.gpr[5];
        int MOutHalfBufferSize = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetMOutHalfwayBufferAndGetID buffer = 0x%08X, readSize = 0x%08X, bufferSize = 0x%08X", MOutHalfBuffer, readSize, MOutHalfBufferSize));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int atID = 0;
        if (Memory.isAddressGood(MOutHalfBuffer)) {
        	int codecType = getCodecType(MOutHalfBuffer);
            atID = hleCreateAtracID(codecType);
            if (atracIDs.containsKey(atID)) {
                atracIDs.get(atID).setData(MOutHalfBuffer, readSize, MOutHalfBufferSize, false);
            }
        }
        cpu.gpr[2] = atID;
    }

    public void sceAtracSetAA3DataAndGetID(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int buffer = cpu.gpr[4];
        int bufferSize = cpu.gpr[5];
        int fileSize = cpu.gpr[6];
        int metadataSizeAddr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceAtracSetAA3DataAndGetID buffer = 0x%08X, bufferSize = 0x%08X, fileSize = 0x%08X, metadataSizeAddr = 0x%08X", buffer, bufferSize, fileSize, metadataSizeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        int atID = 0;
        if (Memory.isAddressGood(buffer)) {
        	int codecType = getCodecType(buffer);
            atID = hleCreateAtracID(codecType);
            if (atracIDs.containsKey(atID)) {
                atracIDs.get(atID).setData(buffer, bufferSize, bufferSize, false);
            }
            mem.write32(metadataSizeAddr, 0x400); // Dummy common value found in most .AA3 files.
        }
        cpu.gpr[2] = atID;
    }

    public void sceAtracSetAA3HalfwayBufferAndGetID(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented function sceAtracSetAA3HalfwayBufferAndGetID "
    			+ String.format("%08x %08x %08x %08x %08x %08x",
    					cpu.gpr[4], cpu.gpr[5], cpu.gpr[6], cpu.gpr[7], cpu.gpr[8], cpu.gpr[9]));

        cpu.gpr[2] = 0;
    }
    @HLEFunction(nid = 0xB3B5D042, version = 250) public HLEModuleFunction sceAtracGetOutputChannelFunction;

    @HLEFunction(nid = 0xECA32A99, version = 250) public HLEModuleFunction sceAtracIsSecondBufferNeededFunction;

    @HLEFunction(nid = 0x132F1ECA, version = 250) public HLEModuleFunction sceAtracReinitFunction;

    @HLEFunction(nid = 0x2DD3E298, version = 250) public HLEModuleFunction sceAtracGetBufferInfoForResettingFunction;

    @HLEFunction(nid = 0x5CF9D852, version = 250) public HLEModuleFunction sceAtracSetMOutHalfwayBufferFunction;

    @HLEFunction(nid = 0xF6837A1A, version = 250) public HLEModuleFunction sceAtracSetMOutDataFunction;

    @HLEFunction(nid = 0x472E3825, version = 250) public HLEModuleFunction sceAtracSetMOutDataAndGetIDFunction;

    @HLEFunction(nid = 0x9CD7DE03, version = 250) public HLEModuleFunction sceAtracSetMOutHalfwayBufferAndGetIDFunction;

    @HLEFunction(nid = 0x5622B7C1, version = 250) public HLEModuleFunction sceAtracSetAA3DataAndGetIDFunction;

    @HLEFunction(nid = 0x5DD66588, version = 250) public HLEModuleFunction sceAtracSetAA3HalfwayBufferAndGetIDFunction;

}