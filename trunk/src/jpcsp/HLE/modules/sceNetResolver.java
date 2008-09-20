/* This autogenerated file is part of jpcsp. */
package jpcsp.HLE.modules;

import jpcsp.HLE.pspSysMem;
import jpcsp.Memory;
import jpcsp.Processor;

public class sceNetResolver implements HLEModule {

    @Override
    public final String getName() {
        return "sceNetResolver";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {

        mm.add(sceNetResolverInit, 0xF3370E61);

        mm.add(sceNetResolverTerm, 0x6138194A);

        mm.add(sceNetResolverCreate, 0x244172AF);

        mm.add(sceNetResolverDelete, 0x94523E09);

        mm.add(sceNetResolverStartNtoA, 0x224C5F44);

        mm.add(sceNetResolverStartAtoN, 0x629E2FB7);

        mm.add(sceNetResolverStop, 0x808F6063);

    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {

        mm.remove(sceNetResolverInit);

        mm.remove(sceNetResolverTerm);

        mm.remove(sceNetResolverCreate);

        mm.remove(sceNetResolverDelete);

        mm.remove(sceNetResolverStartNtoA);

        mm.remove(sceNetResolverStartAtoN);

        mm.remove(sceNetResolverStop);

    }
    public static final HLEModuleFunction sceNetResolverInit = new HLEModuleFunction("sceNetResolver", "sceNetResolverInit") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceNetResolverInit [0xF3370E61]");
        }
    };
    public static final HLEModuleFunction sceNetResolverTerm = new HLEModuleFunction("sceNetResolver", "sceNetResolverTerm") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceNetResolverTerm [0x6138194A]");
        }
    };
    public static final HLEModuleFunction sceNetResolverCreate = new HLEModuleFunction("sceNetResolver", "sceNetResolverCreate") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceNetResolverCreate [0x244172AF]");
        }
    };
    public static final HLEModuleFunction sceNetResolverDelete = new HLEModuleFunction("sceNetResolver", "sceNetResolverDelete") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceNetResolverDelete [0x94523E09]");
        }
    };
    public static final HLEModuleFunction sceNetResolverStartNtoA = new HLEModuleFunction("sceNetResolver", "sceNetResolverStartNtoA") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceNetResolverStartNtoA [0x224C5F44]");
        }
    };
    public static final HLEModuleFunction sceNetResolverStartAtoN = new HLEModuleFunction("sceNetResolver", "sceNetResolverStartAtoN") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceNetResolverStartAtoN [0x629E2FB7]");
        }
    };
    public static final HLEModuleFunction sceNetResolverStop = new HLEModuleFunction("sceNetResolver", "sceNetResolverStop") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceNetResolverStop [0x808F6063]");
        }
    };
};
