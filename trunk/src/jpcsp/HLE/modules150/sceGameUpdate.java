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

public class sceGameUpdate implements HLEModule {

    protected static Logger log = Modules.getLogger("sceGameUpdate");

    @Override
    public String getName() {
        return "sceGameUpdate";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    public void sceGameUpdateInit(Processor processor) {
        CpuState cpu = processor.cpu;
        
        log.warn("UNIMPLEMENTED: sceGameUpdateInit");

        cpu.gpr[2] = 0;
    }

    public void sceGameUpdateTerm(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceGameUpdateTerm");

        cpu.gpr[2] = 0;
    }

    public void sceGameUpdateRun(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceGameUpdateRun");

        cpu.gpr[2] = 0;
    }

    public void sceGameUpdateAbort(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceGameUpdateAbort");

        cpu.gpr[2] = 0;
    }
    @HLEFunction(nid = 0xCBE69FB3, version = 150)
    public final HLEModuleFunction sceGameUpdateInitFunction = new HLEModuleFunction("sceGameUpdate", "sceGameUpdateInit") {

        @Override
        public final void execute(Processor processor) {
            sceGameUpdateInit(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGameUpdateModule.sceGameUpdateInit(processor);";
        }
    };
    @HLEFunction(nid = 0xBB4B68DE, version = 150)
    public final HLEModuleFunction sceGameUpdateTermFunction = new HLEModuleFunction("sceGameUpdate", "sceGameUpdateTerm") {

        @Override
        public final void execute(Processor processor) {
            sceGameUpdateTerm(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGameUpdateModule.sceGameUpdateTerm(processor);";
        }
    };
    @HLEFunction(nid = 0x596AD78C, version = 150)
    public final HLEModuleFunction sceGameUpdateRunFunction = new HLEModuleFunction("sceGameUpdate", "sceGameUpdateRun") {

        @Override
        public final void execute(Processor processor) {
            sceGameUpdateRun(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGameUpdateModule.sceGameUpdateRun(processor);";
        }
    };
    @HLEFunction(nid = 0x5F5D98A6, version = 150)
    public final HLEModuleFunction sceGameUpdateAbortFunction = new HLEModuleFunction("sceGameUpdate", "sceGameUpdateAbort") {

        @Override
        public final void execute(Processor processor) {
            sceGameUpdateAbort(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceGameUpdateModule.sceGameUpdateAbort(processor);";
        }
    };
}