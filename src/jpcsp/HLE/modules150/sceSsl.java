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

import jpcsp.HLE.HLEFunction;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import org.apache.log4j.Logger;

public class sceSsl implements HLEModule {

    protected static Logger log = Modules.getLogger("sceSsl");

    @Override
    public String getName() {
        return "sceSsl";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    private boolean isSslInit;
    private int maxMemSize;
    private int currentMemSize;

    public void sceSslInit(Processor processor) {
        CpuState cpu = processor.cpu;

        int heapSize = cpu.gpr[4];

        log.info("sceSslInit: heapSize=" + Integer.toHexString(heapSize));

        if (isSslInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_SSL_ALREADY_INIT;
        } else if (heapSize <= 0) {
            cpu.gpr[2] = SceKernelErrors.ERROR_SSL_INVALID_PARAMETER;
        } else {
            maxMemSize = heapSize;
            currentMemSize = heapSize / 2; // Dummy value.
            isSslInit = true;
            cpu.gpr[2] = 0;
        }
    }

    public void sceSslEnd(Processor processor) {
        CpuState cpu = processor.cpu;

        log.info("sceSslEnd");

        if (!isSslInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_SSL_NOT_INIT;
        } else {
            isSslInit = false;
            cpu.gpr[2] = 0;
        }
    }

    public void sceSslGetNotAfter(Processor processor) {
        CpuState cpu = processor.cpu;

        int sslCertAddr = cpu.gpr[4];
        int endTimeAddr = cpu.gpr[5];

        log.warn("UNIMPLEMENTED: sceSslGetNotAfter: sslCertAddr=" + Integer.toHexString(sslCertAddr)
                + ", endTimeAddr=" + Integer.toHexString(endTimeAddr));

        cpu.gpr[2] = 0;
    }

    public void sceSslGetNotBefore(Processor processor) {
        CpuState cpu = processor.cpu;

        int sslCertAddr = cpu.gpr[4];
        int startTimeAddr = cpu.gpr[5];

        log.warn("UNIMPLEMENTED: sceSslGetNotAfter: sslCertAddr=" + Integer.toHexString(sslCertAddr)
                + ", startTimeAddr=" + Integer.toHexString(startTimeAddr));

        cpu.gpr[2] = 0;
    }

    public void sceSslGetSubjectName(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceSslGetSubjectName");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceSslGetIssuerName(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceSslGetIssuerName");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceSslGetSerialNumber(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceSslGetSerialNumber");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceSslGetNameEntryCount(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceSslGetNameEntryCount");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceSslGetNameEntryInfo(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceSslGetNameEntryInfo");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceSslGetUsedMemoryMax(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int maxMemAddr = cpu.gpr[4];

        log.info("sceSslGetUsedMemoryMax: maxMemAddr=" + Integer.toHexString(maxMemAddr));

        if (Memory.isAddressGood(maxMemAddr)) {
            mem.write32(maxMemAddr, maxMemSize);
            cpu.gpr[2] = 0;
        } else if (!isSslInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_SSL_NOT_INIT;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SSL_INVALID_PARAMETER;
        }
    }

    public void sceSslGetUsedMemoryCurrent(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int currentMemAddr = cpu.gpr[4];

        log.info("sceSslGetUsedMemoryCurrent: currentMemAddr=" + Integer.toHexString(currentMemAddr));

        if (Memory.isAddressGood(currentMemAddr)) {
            mem.write32(currentMemAddr, currentMemSize);
            cpu.gpr[2] = 0;
        } else if (!isSslInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_SSL_NOT_INIT;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_SSL_INVALID_PARAMETER;
        }
    }

    public void sceSslGetKeyUsage(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceSslGetKeyUsage");

        cpu.gpr[2] = 0xDEADC0DE;
    }
    @HLEFunction(nid = 0x957ECBE2, version = 150) public HLEModuleFunction sceSslInitFunction;

    @HLEFunction(nid = 0x191CDEFF, version = 150) public HLEModuleFunction sceSslEndFunction;

    @HLEFunction(nid = 0x5BFB6B61, version = 150) public HLEModuleFunction sceSslGetNotAfterFunction;

    @HLEFunction(nid = 0x17A10DCC, version = 150) public HLEModuleFunction sceSslGetNotBeforeFunction;

    @HLEFunction(nid = 0x3DD5E023, version = 150) public HLEModuleFunction sceSslGetSubjectNameFunction;

    @HLEFunction(nid = 0x1B7C8191, version = 150) public HLEModuleFunction sceSslGetIssuerNameFunction;

    @HLEFunction(nid = 0xCC0919B0, version = 150) public HLEModuleFunction sceSslGetSerialNumberFunction;

    @HLEFunction(nid = 0x058D21C0, version = 150) public HLEModuleFunction sceSslGetNameEntryCountFunction;

    @HLEFunction(nid = 0xD6D097B4, version = 150) public HLEModuleFunction sceSslGetNameEntryInfoFunction;

    @HLEFunction(nid = 0xB99EDE6A, version = 150) public HLEModuleFunction sceSslGetUsedMemoryMaxFunction;

    @HLEFunction(nid = 0x0EB43B06, version = 150) public HLEModuleFunction sceSslGetUsedMemoryCurrentFunction;

    @HLEFunction(nid = 0xF57765D3, version = 150) public HLEModuleFunction sceSslGetKeyUsageFunction;

}