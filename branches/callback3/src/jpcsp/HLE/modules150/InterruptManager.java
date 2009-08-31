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
/*
Attempt to use the callback manager to trigger interrupts.
- Takes care of context switching
- Takes care of queued interrupts (if we ever support more than vblank intr)
- Theoretically takes care of GP register (if any game actually uses it)
- Downside is we can only enter the interrupt under certain limited conditions, such as from a (waitCB) syscall

Currently we only support the vblank interrupt and trigger this at 60Hz from pspdisplay.
- TODO: detect default/no-op vblank interrupt handlers and ignore them for performance.

TODO
- find the difference between enable/disable and suspend/resume intr
*/

package jpcsp.HLE.modules150;

import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.HLE.Modules;
import jpcsp.HLE.ThreadMan;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceKernelCallbackInfo;

import jpcsp.Allegrex.CpuState; // New-Style Processor

public class InterruptManager implements HLEModule {
    @Override
    public String getName() { return "InterruptManager"; }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(sceKernelRegisterSubIntrHandlerFunction, 0xCA04A2B9);
            mm.addFunction(sceKernelReleaseSubIntrHandlerFunction, 0xD61E6961);
            mm.addFunction(sceKernelEnableSubIntrFunction, 0xFB8E22EC);
            mm.addFunction(sceKernelDisableSubIntrFunction, 0x8A389411);
            mm.addFunction(sceKernelSuspendSubIntrFunction, 0x5CB5A78B);
            mm.addFunction(sceKernelResumeSubIntrFunction, 0x7860E0DC);
            mm.addFunction(sceKernelIsSubInterruptOccurredFunction, 0xFC4374B8);
            mm.addFunction(QueryIntrHandlerInfoFunction, 0xD2E8363F); // actually matches the NID, what were they thinking...
            mm.addFunction(sceKernelRegisterUserSpaceIntrStackFunction, 0xEEE43F47);

            vblankInterrupts = new PspSubIntrInfo[32];
        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceKernelRegisterSubIntrHandlerFunction);
            mm.removeFunction(sceKernelReleaseSubIntrHandlerFunction);
            mm.removeFunction(sceKernelEnableSubIntrFunction);
            mm.removeFunction(sceKernelDisableSubIntrFunction);
            mm.removeFunction(sceKernelSuspendSubIntrFunction);
            mm.removeFunction(sceKernelResumeSubIntrFunction);
            mm.removeFunction(sceKernelIsSubInterruptOccurredFunction);
            mm.removeFunction(QueryIntrHandlerInfoFunction);
            mm.removeFunction(sceKernelRegisterUserSpaceIntrStackFunction);

        }
    }

    // http://psp.jim.sh/pspsdk-doc/structtag__IntrHandlerOptionParam.html
    // may actually be called IntrHandlerOptionParam but it looks ugly
    class PspSubIntrInfo
    {
        public final int size = 56;
        public final int entry; // handler_addr(?)
        public final int common; // arg(?)
        public final int gp;
        public final int intr_code; // intno(?)
        public final int sub_count; // no(?)
        public final int intr_level = 0; // ?
        public int enabled;
        public int calls;
        public int field_1C;
        public int total_clock_lo;
        public int total_clock_hi;
        public int min_clock_lo;
        public int min_clock_hi;
        public int max_clock_lo;
        public int max_clock_hi;

        // Use callback manager to trigger interrupts
        public int cbid;

        public PspSubIntrInfo(int intno, int no, int handler_addr, int arg) {
            this.intr_code = intno;
            this.sub_count = no;
            this.entry = handler_addr;
            this.common = arg;

            // We're going to use the callback manager,
            // so the interrupt will execute on the same thread it was registered on making this useless for now,
            // plus the chances of a game using GP and sub intr have got to be low :)
            gp = ThreadMan.getInstance().getCurrentThread().gpReg_addr;

            // Apps always seem to call sceKernelEnableSubIntr so assume they start off disabled (check)
            enabled = 0;
        }
    }

    // http://psp.jim.sh/pspsdk-doc/pspintrman_8h-source.html
    // missing 0 - 3
    public static final int PSP_GPIO_INT        = 4;
    public static final int PSP_ATA_INT         = 5;
    public static final int PSP_UMD_INT         = 6;
    public static final int PSP_MSCM0_INT       = 7;
    public static final int PSP_WLAN_INT        = 8;
    // missing 9
    public static final int PSP_AUDIO_INT       = 10;
    // missing 11
    public static final int PSP_I2C_INT         = 12;
    // missing 13
    public static final int PSP_SIRCS_INT       = 14;
    public static final int PSP_SYSTIMER0_INT   = 15;
    public static final int PSP_SYSTIMER1_INT   = 16;
    public static final int PSP_SYSTIMER2_INT   = 17;
    public static final int PSP_SYSTIMER3_INT   = 18;
    public static final int PSP_THREAD0_INT     = 19;
    public static final int PSP_NAND_INT        = 20;
    public static final int PSP_DMACPLUS_INT    = 21;
    public static final int PSP_DMA0_INT        = 22;
    public static final int PSP_DMA1_INT        = 23;
    public static final int PSP_MEMLMD_INT      = 24;
    public static final int PSP_GE_INT          = 25;
    // missing 26 - 29
    public static final int PSP_VBLANK_INT      = 30;
    public static final int PSP_MECODEC_INT     = 31;
    // missing 32 - 35
    public static final int PSP_HPREMOTE_INT    = 36;
    // missing 37 - 59
    public static final int PSP_MSCM1_INT       = 60;
    public static final int PSP_MSCM2_INT       = 61;
    // missing 62 - 64
    public static final int PSP_THREAD1_INT     = 65;
    public static final int PSP_INTERRUPT_INT   = 66;

    protected PspSubIntrInfo[] vblankInterrupts;

    public void hleKernelNotifySubIntr(int intno) {
        if (intno == PSP_VBLANK_INT) {
            for (int i = 0; i < vblankInterrupts.length; i++) {
                PspSubIntrInfo info = vblankInterrupts[i];
                if (info != null && info.enabled != 0) {
                    Managers.callbacks.hleKernelNotifyCallbackCommon(info.cbid, info.common, 0, true);
                }
            }
        }
    }

    public void sceKernelRegisterSubIntrHandler(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor

        int intno = cpu.gpr[4];
        int no = cpu.gpr[5];
        int handler_addr = cpu.gpr[6];
        int arg = cpu.gpr[7];

        String msg = "sceKernelRegisterSubIntrHandler(intno=" + intno
            + ",no=" + no
            + ",handler_addr=0x" + Integer.toHexString(handler_addr)
            + ",arg=0x" + Integer.toHexString(arg) + ")";

        if (intno == PSP_VBLANK_INT) {
            Modules.log.info(msg + " PSP_VBLANK_INT");
            int slot = no % vblankInterrupts.length;

            PspSubIntrInfo info = new PspSubIntrInfo(intno, no, handler_addr, arg);
            SceKernelCallbackInfo cbInfo = Managers.callbacks.hleKernelCreateCallback(
                "subIntr_0x" + Integer.toHexString(intno) + "_0x" + Integer.toHexString(handler_addr),
                handler_addr, arg);
            info.cbid = cbInfo.uid;

            if (vblankInterrupts[slot] != null) {
                Modules.log.warn("clobbering previous sub intr handler in slot " + slot);
            }
            vblankInterrupts[slot] = info;

            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("UNIMPLEMENTED:" + msg + " unimplemented sub intr type");
            cpu.gpr[2] = -1;
        }
    }

    public void sceKernelReleaseSubIntrHandler(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor

        int intno = cpu.gpr[4];
        int no = cpu.gpr[5];

        boolean handled = false;
        if (intno == PSP_VBLANK_INT) {
            int slot = no % vblankInterrupts.length;
            if (vblankInterrupts[slot] != null) {
                Managers.callbacks.hleKernelDeleteCallback(vblankInterrupts[slot].cbid);
                vblankInterrupts[slot] = null;
                handled = true;
            }
        }

        if (handled) {
            Modules.log.info("sceKernelReleaseSubIntrHandler(intno=" + intno + ",no=" + no + ")");
            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceKernelReleaseSubIntrHandler(intno=" + intno + ",no=" + no + ") sub intr not found");
            cpu.gpr[2] = -1;
        }
    }

    public void sceKernelEnableSubIntr(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor

        int intno = cpu.gpr[4];
        int no = cpu.gpr[5];

        boolean handled = false;
        if (intno == PSP_VBLANK_INT) {
            int slot = no % vblankInterrupts.length;
            if (vblankInterrupts[slot] != null) {
                Modules.log.info("sceKernelEnableSubIntr(intno=" + intno + ",no=" + no + ") PSP_VBLANK_INT");
                vblankInterrupts[slot].enabled = 1;
                handled = true;
            }
        }

        if (handled) {
            //Modules.log.info("sceKernelEnableSubIntr(intno=" + intno + ",no=" + no + ")");
            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceKernelEnableSubIntr(intno=" + intno + ",no=" + no + ") sub intr not found");
            cpu.gpr[2] = -1;
        }
    }

    public void sceKernelDisableSubIntr(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor

        int intno = cpu.gpr[4];
        int no = cpu.gpr[5];

        boolean handled = false;
        if (intno == PSP_VBLANK_INT) {
            int slot = no % vblankInterrupts.length;
            if (vblankInterrupts[slot] != null) {
                vblankInterrupts[slot].enabled = 0;
                handled = true;
            }
        }

        if (handled) {
            Modules.log.info("sceKernelDisableSubIntr(intno=" + intno + ",no=" + no + ")");
            cpu.gpr[2] = 0;
        } else {
            Modules.log.warn("sceKernelDisableSubIntr(intno=" + intno + ",no=" + no + ") sub intr not found");
            cpu.gpr[2] = -1;
        }
    }

    public void sceKernelSuspendSubIntr(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        Modules.log.warn("Unimplemented NID function sceKernelSuspendSubIntr [0x5CB5A78B]");

        cpu.gpr[2] = 0xDEADC0DE;

        // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
    }

    public void sceKernelResumeSubIntr(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        Modules.log.warn("Unimplemented NID function sceKernelResumeSubIntr [0x7860E0DC]");

        cpu.gpr[2] = 0xDEADC0DE;

        // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
    }

    public void sceKernelIsSubInterruptOccurred(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        Modules.log.warn("Unimplemented NID function sceKernelIsSubInterruptOccurred [0xFC4374B8]");

        cpu.gpr[2] = 0xDEADC0DE;

        // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
    }

    public void QueryIntrHandlerInfo(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        Modules.log.warn("Unimplemented NID function QueryIntrHandlerInfo [0xD2E8363F]");

        cpu.gpr[2] = 0xDEADC0DE;

        // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
    }

    public void sceKernelRegisterUserSpaceIntrStack(Processor processor) {
        CpuState cpu = processor.cpu; // New-Style Processor
        // Processor cpu = processor; // Old-Style Processor
        Memory mem = Processor.memory;

        /* put your own code here instead */

        // int a0 = cpu.gpr[4];  int a1 = cpu.gpr[5];  ...  int t3 = cpu.gpr[11];
        // float f12 = cpu.fpr[12];  float f13 = cpu.fpr[13];  ... float f19 = cpu.fpr[19];

        Modules.log.warn("Unimplemented NID function sceKernelRegisterUserSpaceIntrStack [0xEEE43F47]");

        cpu.gpr[2] = 0xDEADC0DE;

        // cpu.gpr[2] = (int)(result & 0xffffffff);  cpu.gpr[3] = (int)(result  32); cpu.fpr[0] = result;
    }

    public final HLEModuleFunction sceKernelRegisterSubIntrHandlerFunction = new HLEModuleFunction("InterruptManager", "sceKernelRegisterSubIntrHandler") {
        @Override
        public final void execute(Processor processor) {
            sceKernelRegisterSubIntrHandler(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.InterruptManagerModule.sceKernelRegisterSubIntrHandler(processor);";
        }
    };

    public final HLEModuleFunction sceKernelReleaseSubIntrHandlerFunction = new HLEModuleFunction("InterruptManager", "sceKernelReleaseSubIntrHandler") {
        @Override
        public final void execute(Processor processor) {
            sceKernelReleaseSubIntrHandler(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.InterruptManagerModule.sceKernelReleaseSubIntrHandler(processor);";
        }
    };

    public final HLEModuleFunction sceKernelEnableSubIntrFunction = new HLEModuleFunction("InterruptManager", "sceKernelEnableSubIntr") {
        @Override
        public final void execute(Processor processor) {
            sceKernelEnableSubIntr(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.InterruptManagerModule.sceKernelEnableSubIntr(processor);";
        }
    };

    public final HLEModuleFunction sceKernelDisableSubIntrFunction = new HLEModuleFunction("InterruptManager", "sceKernelDisableSubIntr") {
        @Override
        public final void execute(Processor processor) {
            sceKernelDisableSubIntr(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.InterruptManagerModule.sceKernelDisableSubIntr(processor);";
        }
    };

    public final HLEModuleFunction sceKernelSuspendSubIntrFunction = new HLEModuleFunction("InterruptManager", "sceKernelSuspendSubIntr") {
        @Override
        public final void execute(Processor processor) {
            sceKernelSuspendSubIntr(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.InterruptManagerModule.sceKernelSuspendSubIntr(processor);";
        }
    };

    public final HLEModuleFunction sceKernelResumeSubIntrFunction = new HLEModuleFunction("InterruptManager", "sceKernelResumeSubIntr") {
        @Override
        public final void execute(Processor processor) {
            sceKernelResumeSubIntr(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.InterruptManagerModule.sceKernelResumeSubIntr(processor);";
        }
    };

    public final HLEModuleFunction sceKernelIsSubInterruptOccurredFunction = new HLEModuleFunction("InterruptManager", "sceKernelIsSubInterruptOccurred") {
        @Override
        public final void execute(Processor processor) {
            sceKernelIsSubInterruptOccurred(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.InterruptManagerModule.sceKernelIsSubInterruptOccurred(processor);";
        }
    };

    public final HLEModuleFunction QueryIntrHandlerInfoFunction = new HLEModuleFunction("InterruptManager", "QueryIntrHandlerInfo") {
        @Override
        public final void execute(Processor processor) {
            QueryIntrHandlerInfo(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.InterruptManagerModule.QueryIntrHandlerInfo(processor);";
        }
    };

    public final HLEModuleFunction sceKernelRegisterUserSpaceIntrStackFunction = new HLEModuleFunction("InterruptManager", "sceKernelRegisterUserSpaceIntrStack") {
        @Override
        public final void execute(Processor processor) {
            sceKernelRegisterUserSpaceIntrStack(processor);
        }
        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.InterruptManagerModule.sceKernelRegisterUserSpaceIntrStack(processor);";
        }
    };

}
