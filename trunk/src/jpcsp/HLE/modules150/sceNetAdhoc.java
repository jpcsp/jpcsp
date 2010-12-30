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

import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import org.apache.log4j.Logger;

public class sceNetAdhoc implements HLEModule {

    protected static Logger log = Modules.getLogger("sceNetAdhoc");

    @Override
    public String getName() {
        return "sceNetAdhoc";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0xE1D621D7, sceNetAdhocInitFunction);
            mm.addFunction(0xA62C6F57, sceNetAdhocTermFunction);
            mm.addFunction(0x7A662D6B, sceNetAdhocPollSocketFunction);
            mm.addFunction(0x73BFD52D, sceNetAdhocSetSocketAlertFunction);
            mm.addFunction(0x4D2CE199, sceNetAdhocGetSocketAlertFunction);
            mm.addFunction(0x6F92741B, sceNetAdhocPdpCreateFunction);
            mm.addFunction(0xABED3790, sceNetAdhocPdpSendFunction);
            mm.addFunction(0xDFE53E03, sceNetAdhocPdpRecvFunction);
            mm.addFunction(0x7F27BB5E, sceNetAdhocPdpDeleteFunction);
            mm.addFunction(0xC7C1FC57, sceNetAdhocGetPdpStatFunction);
            mm.addFunction(0x877F6D66, sceNetAdhocPtpOpenFunction);
            mm.addFunction(0xFC6FC07B, sceNetAdhocPtpConnectFunction);
            mm.addFunction(0xE08BDAC1, sceNetAdhocPtpListenFunction);
            mm.addFunction(0x9DF81198, sceNetAdhocPtpAcceptFunction);
            mm.addFunction(0x4DA4C788, sceNetAdhocPtpSendFunction);
            mm.addFunction(0x8BEA2B3E, sceNetAdhocPtpRecvFunction);
            mm.addFunction(0x9AC2EEAC, sceNetAdhocPtpFlushFunction);
            mm.addFunction(0x157E6225, sceNetAdhocPtpCloseFunction);
            mm.addFunction(0xB9685118, sceNetAdhocGetPtpStatFunction);
            mm.addFunction(0x7F75C338, sceNetAdhocGameModeCreateMasterFunction);
            mm.addFunction(0x3278AB0C, sceNetAdhocGameModeCreateReplicaFunction);
            mm.addFunction(0x98C204C8, sceNetAdhocGameModeUpdateMasterFunction);
            mm.addFunction(0xFA324B4E, sceNetAdhocGameModeUpdateReplicaFunction);
            mm.addFunction(0xA0229362, sceNetAdhocGameModeDeleteMasterFunction);
            mm.addFunction(0x0B2228E9, sceNetAdhocGameModeDeleteReplicaFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceNetAdhocInitFunction);
            mm.removeFunction(sceNetAdhocTermFunction);
            mm.removeFunction(sceNetAdhocPollSocketFunction);
            mm.removeFunction(sceNetAdhocSetSocketAlertFunction);
            mm.removeFunction(sceNetAdhocGetSocketAlertFunction);
            mm.removeFunction(sceNetAdhocPdpCreateFunction);
            mm.removeFunction(sceNetAdhocPdpSendFunction);
            mm.removeFunction(sceNetAdhocPdpRecvFunction);
            mm.removeFunction(sceNetAdhocPdpDeleteFunction);
            mm.removeFunction(sceNetAdhocGetPdpStatFunction);
            mm.removeFunction(sceNetAdhocPtpOpenFunction);
            mm.removeFunction(sceNetAdhocPtpConnectFunction);
            mm.removeFunction(sceNetAdhocPtpListenFunction);
            mm.removeFunction(sceNetAdhocPtpAcceptFunction);
            mm.removeFunction(sceNetAdhocPtpSendFunction);
            mm.removeFunction(sceNetAdhocPtpRecvFunction);
            mm.removeFunction(sceNetAdhocPtpFlushFunction);
            mm.removeFunction(sceNetAdhocPtpCloseFunction);
            mm.removeFunction(sceNetAdhocGetPtpStatFunction);
            mm.removeFunction(sceNetAdhocGameModeCreateMasterFunction);
            mm.removeFunction(sceNetAdhocGameModeCreateReplicaFunction);
            mm.removeFunction(sceNetAdhocGameModeUpdateMasterFunction);
            mm.removeFunction(sceNetAdhocGameModeUpdateReplicaFunction);
            mm.removeFunction(sceNetAdhocGameModeDeleteMasterFunction);
            mm.removeFunction(sceNetAdhocGameModeDeleteReplicaFunction);

        }
    }

    public void sceNetAdhocInit(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("IGNORING: sceNetAdhocInit");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceNetAdhocTerm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("IGNORING: sceNetAdhocTerm");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceNetAdhocPollSocket(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPollSocket");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocSetSocketAlert(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocSetSocketAlert");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocGetSocketAlert(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGetSocketAlert");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocPdpCreate(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPdpCreate");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocPdpSend(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPdpSend");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocPdpRecv(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPdpRecv");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocPdpDelete(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPdpDelete");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocGetPdpStat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGetPdpStat");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocPtpOpen(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpOpen");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocPtpConnect(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpConnect");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocPtpListen(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpListen");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocPtpAccept(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpAccept");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocPtpSend(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpSend");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocPtpRecv(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpRecv");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocPtpFlush(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpFlush");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocPtpClose(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocPtpClose");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocGetPtpStat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGetPtpStat");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocGameModeCreateMaster(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeCreateMaster");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocGameModeCreateReplica(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeCreateReplica");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocGameModeUpdateMaster(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeUpdateMaster");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocGameModeUpdateReplica(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeUpdateReplica");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocGameModeDeleteMaster(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeDeleteMaster");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceNetAdhocGameModeDeleteReplica(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocGameModeDeleteReplica");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public final HLEModuleFunction sceNetAdhocInitFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocInit") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocInit(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocInit(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocTermFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocTerm") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocTerm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocTerm(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPollSocketFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPollSocket") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPollSocket(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPollSocket(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocSetSocketAlertFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocSetSocketAlert") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocSetSocketAlert(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocSetSocketAlert(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocGetSocketAlertFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocGetSocketAlert") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocGetSocketAlert(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocGetSocketAlert(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPdpCreateFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPdpCreate") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPdpCreate(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPdpCreate(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPdpSendFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPdpSend") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPdpSend(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPdpSend(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPdpRecvFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPdpRecv") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPdpRecv(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPdpRecv(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPdpDeleteFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPdpDelete") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPdpDelete(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPdpDelete(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocGetPdpStatFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocGetPdpStat") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocGetPdpStat(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocGetPdpStat(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPtpOpenFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPtpOpen") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPtpOpen(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPtpOpen(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPtpConnectFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPtpConnect") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPtpConnect(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPtpConnect(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPtpListenFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPtpListen") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPtpListen(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPtpListen(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPtpAcceptFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPtpAccept") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPtpAccept(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPtpAccept(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPtpSendFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPtpSend") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPtpSend(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPtpSend(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPtpRecvFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPtpRecv") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPtpRecv(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPtpRecv(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPtpFlushFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPtpFlush") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPtpFlush(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPtpFlush(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocPtpCloseFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocPtpClose") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocPtpClose(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocPtpClose(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocGetPtpStatFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocGetPtpStat") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocGetPtpStat(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocGetPtpStat(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocGameModeCreateMasterFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocGameModeCreateMaster") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocGameModeCreateMaster(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocGameModeCreateMaster(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocGameModeCreateReplicaFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocGameModeCreateReplica") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocGameModeCreateReplica(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocGameModeCreateReplica(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocGameModeUpdateMasterFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocGameModeUpdateMaster") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocGameModeUpdateMaster(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocGameModeUpdateMaster(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocGameModeUpdateReplicaFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocGameModeUpdateReplica") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocGameModeUpdateReplica(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocGameModeUpdateReplica(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocGameModeDeleteMasterFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocGameModeDeleteMaster") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocGameModeDeleteMaster(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocGameModeDeleteMaster(processor);";
        }
    };

    public final HLEModuleFunction sceNetAdhocGameModeDeleteReplicaFunction = new HLEModuleFunction("sceNetAdhoc", "sceNetAdhocGameModeDeleteReplica") {

        @Override
        public final void execute(Processor processor) {
            sceNetAdhocGameModeDeleteReplica(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceNetAdhocModule.sceNetAdhocGameModeDeleteReplica(processor);";
        }
    };
}