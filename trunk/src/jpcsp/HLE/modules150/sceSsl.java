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
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x957ECBE2, sceSslInitFunction);
            mm.addFunction(0x191CDEFF, sceSslEndFunction);
            mm.addFunction(0x5BFB6B61, sceSslGetNotAfterFunction);
            mm.addFunction(0x17A10DCC, sceSslGetNotBeforeFunction);
            mm.addFunction(0x3DD5E023, sceSslGetSubjectNameFunction);
            mm.addFunction(0x1B7C8191, sceSslGetIssuerNameFunction);
            mm.addFunction(0xCC0919B0, sceSslGetSerialNumberFunction);
            mm.addFunction(0x058D21C0, sceSslGetNameEntryCountFunction);
            mm.addFunction(0xD6D097B4, sceSslGetNameEntryInfoFunction);
            mm.addFunction(0xB99EDE6A, sceSslGetUsedMemoryMaxFunction);
            mm.addFunction(0x0EB43B06, sceSslGetUsedMemoryCurrentFunction);
            mm.addFunction(0xF57765D3, sceSslGetKeyUsageFunction);
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceSslInitFunction);
            mm.removeFunction(sceSslEndFunction);
            mm.removeFunction(sceSslGetNotAfterFunction);
            mm.removeFunction(sceSslGetNotBeforeFunction);
            mm.removeFunction(sceSslGetSubjectNameFunction);
            mm.removeFunction(sceSslGetIssuerNameFunction);
            mm.removeFunction(sceSslGetSerialNumberFunction);
            mm.removeFunction(sceSslGetNameEntryCountFunction);
            mm.removeFunction(sceSslGetNameEntryInfoFunction);
            mm.removeFunction(sceSslGetUsedMemoryMaxFunction);
            mm.removeFunction(sceSslGetUsedMemoryCurrentFunction);
            mm.removeFunction(sceSslGetKeyUsageFunction);

        }
    }

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

    public final HLEModuleFunction sceSslInitFunction = new HLEModuleFunction("sceSsl", "sceSslInit") {

        @Override
        public final void execute(Processor processor) {
            sceSslInit(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSslModule.sceSslInit(processor);";
        }
    };

    public final HLEModuleFunction sceSslEndFunction = new HLEModuleFunction("sceSsl", "sceSslEnd") {

        @Override
        public final void execute(Processor processor) {
            sceSslEnd(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSslModule.sceSslEnd(processor);";
        }
    };

    public final HLEModuleFunction sceSslGetNotAfterFunction = new HLEModuleFunction("sceSsl", "sceSslGetNotAfter") {

        @Override
        public final void execute(Processor processor) {
            sceSslGetNotAfter(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSslModule.sceSslGetNotAfter(processor);";
        }
    };

    public final HLEModuleFunction sceSslGetNotBeforeFunction = new HLEModuleFunction("sceSsl", "sceSslGetNotBefore") {

        @Override
        public final void execute(Processor processor) {
            sceSslGetNotBefore(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSslModule.sceSslGetNotBefore(processor);";
        }
    };

    public final HLEModuleFunction sceSslGetSubjectNameFunction = new HLEModuleFunction("sceSsl", "sceSslGetSubjectName") {

        @Override
        public final void execute(Processor processor) {
            sceSslGetSubjectName(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSslModule.sceSslGetSubjectName(processor);";
        }
    };

    public final HLEModuleFunction sceSslGetIssuerNameFunction = new HLEModuleFunction("sceSsl", "sceSslGetIssuerName") {

        @Override
        public final void execute(Processor processor) {
            sceSslGetIssuerName(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSslModule.sceSslGetIssuerName(processor);";
        }
    };

    public final HLEModuleFunction sceSslGetSerialNumberFunction = new HLEModuleFunction("sceSsl", "sceSslGetSerialNumber") {

        @Override
        public final void execute(Processor processor) {
            sceSslGetSerialNumber(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSslModule.sceSslGetSerialNumber(processor);";
        }
    };

    public final HLEModuleFunction sceSslGetNameEntryCountFunction = new HLEModuleFunction("sceSsl", "sceSslGetNameEntryCount") {

        @Override
        public final void execute(Processor processor) {
            sceSslGetNameEntryCount(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSslModule.sceSslGetNameEntryCount(processor);";
        }
    };

    public final HLEModuleFunction sceSslGetNameEntryInfoFunction = new HLEModuleFunction("sceSsl", "sceSslGetNameEntryInfo") {

        @Override
        public final void execute(Processor processor) {
            sceSslGetNameEntryInfo(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSslModule.sceSslGetNameEntryInfo(processor);";
        }
    };

    public final HLEModuleFunction sceSslGetUsedMemoryMaxFunction = new HLEModuleFunction("sceSsl", "sceSslGetUsedMemoryMax") {

        @Override
        public final void execute(Processor processor) {
            sceSslGetUsedMemoryMax(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSslModule.sceSslGetUsedMemoryMax(processor);";
        }
    };

    public final HLEModuleFunction sceSslGetUsedMemoryCurrentFunction = new HLEModuleFunction("sceSsl", "sceSslGetUsedMemoryCurrent") {

        @Override
        public final void execute(Processor processor) {
            sceSslGetUsedMemoryCurrent(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSslModule.sceSslGetUsedMemoryCurrent(processor);";
        }
    };

    public final HLEModuleFunction sceSslGetKeyUsageFunction = new HLEModuleFunction("sceSsl", "sceSslGetKeyUsage") {

        @Override
        public final void execute(Processor processor) {
            sceSslGetKeyUsage(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceSslModule.sceSslGetKeyUsage(processor);";
        }
    };
}