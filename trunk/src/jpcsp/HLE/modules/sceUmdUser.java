/* This autogenerated file is part of jpcsp. */
package jpcsp.HLE.modules;

import jpcsp.HLE.pspSysMem;
import jpcsp.Memory;
import jpcsp.Processor;

public class sceUmdUser implements HLEModule {

    @Override
    public final String getName() {
        return "sceUmdUser";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {

        mm.add(sceUmdCheckMedium, 0x46EBB729);

        mm.add(sceUmdActivate, 0xC6183D47);

        mm.add(sceUmdDeactivate, 0xE83742BA);

        mm.add(sceUmdWaitDriveStat, 0x8EF08FCE);

        mm.add(sceUmdWaitDriveStatWithTimer, 0x56202973);

        mm.add(sceUmdWaitDriveStatCB, 0x4A9E5E29);

        mm.add(sceUmdCancelWaitDriveStat, 0x6AF9B50A);

        mm.add(sceUmdGetDriveStat, 0x6B4A146C);

        mm.add(sceUmdGetErrorStat, 0x20628E6F);

        mm.add(sceUmdGetDiscInfo, 0x340B7686);

        mm.add(sceUmdRegisterUMDCallBack, 0xAEE7404D);

        mm.add(sceUmdUnRegisterUMDCallBack, 0xBD2BDE07);

    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {

        mm.remove(sceUmdCheckMedium);

        mm.remove(sceUmdActivate);

        mm.remove(sceUmdDeactivate);

        mm.remove(sceUmdWaitDriveStat);

        mm.remove(sceUmdWaitDriveStatWithTimer);

        mm.remove(sceUmdWaitDriveStatCB);

        mm.remove(sceUmdCancelWaitDriveStat);

        mm.remove(sceUmdGetDriveStat);

        mm.remove(sceUmdGetErrorStat);

        mm.remove(sceUmdGetDiscInfo);

        mm.remove(sceUmdRegisterUMDCallBack);

        mm.remove(sceUmdUnRegisterUMDCallBack);

    }
    public static final HLEModuleFunction sceUmdCheckMedium = new HLEModuleFunction("sceUmdUser", "sceUmdCheckMedium") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceUmdCheckMedium [0x46EBB729]");
        }
    };
    public static final HLEModuleFunction sceUmdActivate = new HLEModuleFunction("sceUmdUser", "sceUmdActivate") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceUmdActivate [0xC6183D47]");
        }
    };
    public static final HLEModuleFunction sceUmdDeactivate = new HLEModuleFunction("sceUmdUser", "sceUmdDeactivate") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceUmdDeactivate [0xE83742BA]");
        }
    };
    public static final HLEModuleFunction sceUmdWaitDriveStat = new HLEModuleFunction("sceUmdUser", "sceUmdWaitDriveStat") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceUmdWaitDriveStat [0x8EF08FCE]");
        }
    };
    public static final HLEModuleFunction sceUmdWaitDriveStatWithTimer = new HLEModuleFunction("sceUmdUser", "sceUmdWaitDriveStatWithTimer") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceUmdWaitDriveStatWithTimer [0x56202973]");
        }
    };
    public static final HLEModuleFunction sceUmdWaitDriveStatCB = new HLEModuleFunction("sceUmdUser", "sceUmdWaitDriveStatCB") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceUmdWaitDriveStatCB [0x4A9E5E29]");
        }
    };
    public static final HLEModuleFunction sceUmdCancelWaitDriveStat = new HLEModuleFunction("sceUmdUser", "sceUmdCancelWaitDriveStat") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceUmdCancelWaitDriveStat [0x6AF9B50A]");
        }
    };
    public static final HLEModuleFunction sceUmdGetDriveStat = new HLEModuleFunction("sceUmdUser", "sceUmdGetDriveStat") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceUmdGetDriveStat [0x6B4A146C]");
        }
    };
    public static final HLEModuleFunction sceUmdGetErrorStat = new HLEModuleFunction("sceUmdUser", "sceUmdGetErrorStat") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceUmdGetErrorStat [0x20628E6F]");
        }
    };
    public static final HLEModuleFunction sceUmdGetDiscInfo = new HLEModuleFunction("sceUmdUser", "sceUmdGetDiscInfo") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceUmdGetDiscInfo [0x340B7686]");
        }
    };
    public static final HLEModuleFunction sceUmdRegisterUMDCallBack = new HLEModuleFunction("sceUmdUser", "sceUmdRegisterUMDCallBack") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceUmdRegisterUMDCallBack [0xAEE7404D]");
        }
    };
    public static final HLEModuleFunction sceUmdUnRegisterUMDCallBack = new HLEModuleFunction("sceUmdUser", "sceUmdUnRegisterUMDCallBack") {

        @Override
        public void execute(Processor cpu, Memory mem) {
            System.out.println("Unimplement function sceUmdUnRegisterUMDCallBack [0xBD2BDE07]");
        }
    };
};
