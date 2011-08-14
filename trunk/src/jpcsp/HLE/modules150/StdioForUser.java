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
import jpcsp.HLE.modules.IoFileMgrForUser;

import org.apache.log4j.Logger;

public class StdioForUser implements HLEModule {
    private static Logger log = Modules.getLogger("StdioForUser");

    @Override
    public String getName() {
        return "StdioForUser";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) { mm.installModuleWithAnnotations(this, version); }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) { mm.uninstallModuleWithAnnotations(this, version); }

    public void sceKernelStdioRead(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceKernelStdioRead [0x3054D478]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceKernelStdioLseek(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceKernelStdioLseek [0x0CBB0571]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceKernelStdioSendChar(Processor processor) {
        CpuState cpu = processor.cpu; 

        log.warn("Unimplemented NID function sceKernelStdioSendChar [0xA46785C9]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceKernelStdioWrite(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceKernelStdioWrite [0xA3B931DB]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceKernelStdioClose(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceKernelStdioClose [0x9D061C19]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceKernelStdioOpen(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("Unimplemented NID function sceKernelStdioOpen [0x924ABA61]");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    public void sceKernelStdin(Processor processor) {
        CpuState cpu = processor.cpu;

        cpu.gpr[2] = IoFileMgrForUser.STDIN_ID;
    }

    public void sceKernelStdout(Processor processor) {
        CpuState cpu = processor.cpu;

        cpu.gpr[2] = IoFileMgrForUser.STDOUT_ID;
    }

    public void sceKernelStderr(Processor processor) {
        CpuState cpu = processor.cpu;

        cpu.gpr[2] = IoFileMgrForUser.STDERR_ID;
    }
    @HLEFunction(nid = 0x3054D478, version = 150)
    public final HLEModuleFunction sceKernelStdioReadFunction = new HLEModuleFunction("StdioForUser", "sceKernelStdioRead") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStdioRead(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.StdioForUserModule.sceKernelStdioRead(processor);";
        }
    };    @HLEFunction(nid = 0x0CBB0571, version = 150)
    public final HLEModuleFunction sceKernelStdioLseekFunction = new HLEModuleFunction("StdioForUser", "sceKernelStdioLseek") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStdioLseek(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.StdioForUserModule.sceKernelStdioLseek(processor);";
        }
    };    @HLEFunction(nid = 0xA46785C9, version = 150)
    public final HLEModuleFunction sceKernelStdioSendCharFunction = new HLEModuleFunction("StdioForUser", "sceKernelStdioSendChar") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStdioSendChar(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.StdioForUserModule.sceKernelStdioSendChar(processor);";
        }
    };    @HLEFunction(nid = 0xA3B931DB, version = 150)
    public final HLEModuleFunction sceKernelStdioWriteFunction = new HLEModuleFunction("StdioForUser", "sceKernelStdioWrite") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStdioWrite(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.StdioForUserModule.sceKernelStdioWrite(processor);";
        }
    };    @HLEFunction(nid = 0x9D061C19, version = 150)
    public final HLEModuleFunction sceKernelStdioCloseFunction = new HLEModuleFunction("StdioForUser", "sceKernelStdioClose") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStdioClose(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.StdioForUserModule.sceKernelStdioClose(processor);";
        }
    };    @HLEFunction(nid = 0x924ABA61, version = 150)
    public final HLEModuleFunction sceKernelStdioOpenFunction = new HLEModuleFunction("StdioForUser", "sceKernelStdioOpen") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStdioOpen(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.StdioForUserModule.sceKernelStdioOpen(processor);";
        }
    };    @HLEFunction(nid = 0x172D316E, version = 150)
    public final HLEModuleFunction sceKernelStdinFunction = new HLEModuleFunction("StdioForUser", "sceKernelStdin") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStdin(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.StdioForUserModule.sceKernelStdin(processor);";
        }
    };    @HLEFunction(nid = 0xA6BAB2E9, version = 150)
    public final HLEModuleFunction sceKernelStdoutFunction = new HLEModuleFunction("StdioForUser", "sceKernelStdout") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStdout(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.StdioForUserModule.sceKernelStdout(processor);";
        }
    };    @HLEFunction(nid = 0xF78BA90A, version = 150)
    public final HLEModuleFunction sceKernelStderrFunction = new HLEModuleFunction("StdioForUser", "sceKernelStderr") {

        @Override
        public final void execute(Processor processor) {
            sceKernelStderr(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.StdioForUserModule.sceKernelStderr(processor);";
        }
    };
}