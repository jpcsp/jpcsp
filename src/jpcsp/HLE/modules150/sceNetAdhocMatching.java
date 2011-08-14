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
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import org.apache.log4j.Logger;

public class sceNetAdhocMatching implements HLEModule {

    protected static Logger log = Modules.getLogger("sceNetAdhocMatching");

    @Override
    public String getName() {
        return "sceNetAdhocMatching";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    @HLEFunction(nid = 0x2A2A1E07, version = 150)
    public void sceNetAdhocMatchingInit(Processor processor) {
        CpuState cpu = processor.cpu;

        int poolSize = cpu.gpr[4];

        log.warn("IGNORING: sceNetAdhocMatchingInit: poolSize=0x" + Integer.toHexString(poolSize));

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x7945ECDA, version = 150)
    public void sceNetAdhocMatchingTerm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("IGNORING: sceNetAdhocMatchingTerm");

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xCA5EDA6F, version = 150)
    public void sceNetAdhocMatchingCreate(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingCreate");

        cpu.gpr[2] = 1;
    }

    @HLEFunction(nid = 0x93EF3843, version = 150)
    public void sceNetAdhocMatchingStart(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingStart");

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x32B156B3, version = 150)
    public void sceNetAdhocMatchingStop(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingStop");

        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xF16EAF4F, version = 150)
    public void sceNetAdhocMatchingDelete(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingDelete");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xF79472D7, version = 150)
    public void sceNetAdhocMatchingSendData(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingSendData");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xEC19337D, version = 150)
    public void sceNetAdhocMatchingAbortSendData(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingAbortSendData");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x5E3D4B79, version = 150)
    public void sceNetAdhocMatchingSelectTarget(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingSelectTarget");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xEA3C6108, version = 150)
    public void sceNetAdhocMatchingCancelTarget(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingCancelTarget");

        cpu.gpr[2] = 0xDEADC0DE;
    }


    @HLEFunction(nid = 0x8F58BEDF, version = 150)
    public void sceNetAdhocMatchingCancelTargetWithOpt(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingCancelTargetWithOpt");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xB5D96C2A, version = 150)
    public void sceNetAdhocMatchingGetHelloOpt(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingGetHelloOpt");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xB58E61B7, version = 150)
    public void sceNetAdhocMatchingSetHelloOpt(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingSetHelloOpt");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xC58BCD9E, version = 150)
    public void sceNetAdhocMatchingGetMembers(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingGetMembers");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x9C5CFB7D, version = 150)
    public void sceNetAdhocMatchingGetPoolStat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingGetPoolStat");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x40F8F435, version = 150)
    public void sceNetAdhocMatchingGetPoolMaxAlloc(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingGetPoolMaxAlloc");

        cpu.gpr[2] = 0xDEADC0DE;
    }

}