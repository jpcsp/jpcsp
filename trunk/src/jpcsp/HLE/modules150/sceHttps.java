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

import java.util.HashMap;

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;

import org.apache.log4j.Logger;

public class sceHttps implements HLEModule {

    protected static Logger log = Modules.getLogger("sceHttps");

    @Override
    public String getName() {
        return "sceHttps";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0xE4D21302, sceHttpsInitFunction);
            mm.addFunction(0x68AB0F86, sceHttpsInitWithPathFunction);
            mm.addFunction(0xF9D8EB63, sceHttpsEndFunction);
            mm.addFunction(0x87797BDD, sceHttpsLoadDefaultCertFunction);
            mm.addFunction(0xAB1540D5, sceHttpsGetSslErrorFunction);
            mm.addFunction(0xBAC31BF1, sceHttpsEnableOptionFunction);
            mm.addFunction(0xB3FAF831, sceHttpsDisableOptionFunction);
            mm.addFunction(0xD11DAB01, sceHttpsGetCaListFunction);
            mm.addFunction(0x569A1481, sceHttpsSetSslCallbackFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceHttpsInitFunction);
            mm.removeFunction(sceHttpsInitWithPathFunction);
            mm.removeFunction(sceHttpsEndFunction);
            mm.removeFunction(sceHttpsLoadDefaultCertFunction);
            mm.removeFunction(sceHttpsGetSslErrorFunction);
            mm.removeFunction(sceHttpsEnableOptionFunction);
            mm.removeFunction(sceHttpsDisableOptionFunction);
            mm.removeFunction(sceHttpsGetCaListFunction);
            mm.removeFunction(sceHttpsSetSslCallbackFunction);

        }
    }
    // Certificate related statics (guessed from a PSP's certificates' list).
    // The PSP currently handles certificates for the following issuers:
    //   - RSA: 2 certificates (resolved);
    //   - VERISIGN: 14 certificates (resolved);
    //   - SCE: 5 (resolved);
    //   - GEOTRUST: 4 (resolved);
    //   - ENTRUST: 1 (resolved);
    //   - VALICERT: 1 (resolved);
    //   - CYBERTRUST: 4 (resolved);
    //   - THAWTE: 2 (resolved);
    //   - COMODO: 3 (resolved).
    public static final int PSP_HTTPS_ISSUER_ALL = 0x00000000; // Loads all certificates from flash.
    public static final int PSP_HTTPS_ISSUER_RSA = 0x00000001;
    public static final int PSP_HTTPS_ISSUER_VERISIGN = 0x00000002;
    public static final int PSP_HTTPS_ISSUER_SCE = 0x00000003;
    public static final int PSP_HTTPS_ISSUER_GEOTRUST = 0x00000004;
    public static final int PSP_HTTPS_ISSUER_ENTRUST = 0x00000005;
    public static final int PSP_HTTPS_ISSUER_VALICERT = 0x00000006;
    public static final int PSP_HTTPS_ISSUER_CYBERTRUST = 0x00000007;
    public static final int PSP_HTTPS_ISSUER_THAWTE = 0x00000008;
    public static final int PSP_HTTPS_ISSUER_COMODO = 0x00000009;

    public static final int PSP_HTTPS_CERT_ALL = 0xFFFFFFFF; // Loads all certificates for a particular issuer.
    public static final int PSP_HTTPS_CERT_RSA_1024_V1_C3 = 0x00000001;
    public static final int PSP_HTTPS_CERT_RSA_2048_V3 = 0x00000002;
    public static final int PSP_HTTPS_CERT_RSA_ALL = (PSP_HTTPS_CERT_RSA_1024_V1_C3 | PSP_HTTPS_CERT_RSA_2048_V3);

    public static final int PSP_HTTPS_CERT_VERISIGN_PCA_C1 = 0x00000001;
    public static final int PSP_HTTPS_CERT_VERISIGN_PCA_C2 = 0x00000002;
    public static final int PSP_HTTPS_CERT_VERISIGN_PCA_C3 = 0x00000004;
    public static final int PSP_HTTPS_CERT_VERISIGN_PCA_C1_G2 = 0x00000008;
    public static final int PSP_HTTPS_CERT_VERISIGN_PCA_C2_G2 = 0x00000010;
    public static final int PSP_HTTPS_CERT_VERISIGN_PCA_C3_G2 = 0x00000020;
    public static final int PSP_HTTPS_CERT_VERISIGN_PCA_C4_G2 = 0x00000040;
    public static final int PSP_HTTPS_CERT_VERISIGN_PCA_C1_G3 = 0x00000080;
    public static final int PSP_HTTPS_CERT_VERISIGN_PCA_C2_G3 = 0x00000100;
    public static final int PSP_HTTPS_CERT_VERISIGN_PCA_C3_G3 = 0x00000200;
    public static final int PSP_HTTPS_CERT_VERISIGN_PCA_C4_G3 = 0x00000400;
    public static final int PSP_HTTPS_CERT_VERISIGN_TSA = 0x00000800;
    public static final int PSP_HTTPS_CERT_VERISIGN_RSA_SS = 0x00001000;
    public static final int PSP_HTTPS_CERT_VERISIGN_PCA_C3_G5 = 0x00002000;
    public static final int PSP_HTTPS_CERT_VERISIGN_ALL = (PSP_HTTPS_CERT_VERISIGN_PCA_C1 | PSP_HTTPS_CERT_VERISIGN_PCA_C2
            | PSP_HTTPS_CERT_VERISIGN_PCA_C3 | PSP_HTTPS_CERT_VERISIGN_PCA_C1_G2 | PSP_HTTPS_CERT_VERISIGN_PCA_C2_G2 | PSP_HTTPS_CERT_VERISIGN_PCA_C3_G2
            | PSP_HTTPS_CERT_VERISIGN_PCA_C4_G2 | PSP_HTTPS_CERT_VERISIGN_PCA_C1_G3 | PSP_HTTPS_CERT_VERISIGN_PCA_C2_G3 | PSP_HTTPS_CERT_VERISIGN_PCA_C2_G3
            | PSP_HTTPS_CERT_VERISIGN_PCA_C3_G3 | PSP_HTTPS_CERT_VERISIGN_PCA_C4_G3 | PSP_HTTPS_CERT_VERISIGN_TSA | PSP_HTTPS_CERT_VERISIGN_RSA_SS | PSP_HTTPS_CERT_VERISIGN_PCA_C3_G5);

    public static final int PSP_HTTPS_CERT_SCEI_ROOT_CA_01 = 0x00000001;
    public static final int PSP_HTTPS_CERT_SCEI_ROOT_CA_02 = 0x00000002;
    public static final int PSP_HTTPS_CERT_SCEI_ROOT_CA_03 = 0x00000004;
    public static final int PSP_HTTPS_CERT_SCEI_ROOT_CA_04 = 0x00000008;
    public static final int PSP_HTTPS_CERT_SCEI_ROOT_CA_05 = 0x00000010;
    public static final int PSP_HTTPS_CERT_SCEI_ALL = (PSP_HTTPS_CERT_SCEI_ROOT_CA_01 | PSP_HTTPS_CERT_SCEI_ROOT_CA_02
            | PSP_HTTPS_CERT_SCEI_ROOT_CA_03 | PSP_HTTPS_CERT_SCEI_ROOT_CA_04 | PSP_HTTPS_CERT_SCEI_ROOT_CA_05);

    public static final int PSP_HTTPS_CERT_GEOTRUST_GLOBAL_CA = 0x00000001;
    public static final int PSP_HTTPS_CERT_GEOTRUST_EQUIFAX_SECURE_CA = 0x00000002;
    public static final int PSP_HTTPS_CERT_GEOTRUST_EQUIFAX_SECURE_EBUSINESS_CA1 = 0x00000004;
    public static final int PSP_HTTPS_CERT_GEOTRUST_EQUIFAX_SECURE_GLOBAL_EBUSINESS_CA1 = 0x00000008;
    public static final int PSP_HTTPS_CERT_GEOTRUST_ALL = (PSP_HTTPS_CERT_GEOTRUST_GLOBAL_CA | PSP_HTTPS_CERT_GEOTRUST_EQUIFAX_SECURE_CA
            | PSP_HTTPS_CERT_GEOTRUST_EQUIFAX_SECURE_EBUSINESS_CA1 | PSP_HTTPS_CERT_GEOTRUST_EQUIFAX_SECURE_GLOBAL_EBUSINESS_CA1);

    public static final int PSP_HTTPS_CERT_ENTRUST_SECURE_SERVER_CA = 0x00000001;
    public static final int PSP_HTTPS_CERT_ENTRUST_ALL = PSP_HTTPS_CERT_ENTRUST_SECURE_SERVER_CA;

    public static final int PSP_HTTPS_CERT_VALICERT_C2_CA = 0x00000001;
    public static final int PSP_HTTPS_CERT_VALICERT_ALL = PSP_HTTPS_CERT_VALICERT_C2_CA;

    public static final int PSP_HTTPS_CERT_CYBERTRUST_BALTIMORE_ROOT_CA = 0x00000001;
    public static final int PSP_HTTPS_CERT_CYBERTRUST_GTE_GLOBAL_ROOT_CA = 0x00000002;
    public static final int PSP_HTTPS_CERT_CYBERTRUST_GTE_ROOT_CA = 0x00000004;
    public static final int PSP_HTTPS_CERT_CYBERTRUST_GLOBALSIGN_ROOT_CA_R1 = 0x00000008;
    public static final int PSP_HTTPS_CERT_CYBERTRUST_ALL = (PSP_HTTPS_CERT_CYBERTRUST_BALTIMORE_ROOT_CA | PSP_HTTPS_CERT_CYBERTRUST_GTE_GLOBAL_ROOT_CA
            | PSP_HTTPS_CERT_CYBERTRUST_GTE_ROOT_CA | PSP_HTTPS_CERT_CYBERTRUST_GLOBALSIGN_ROOT_CA_R1);

    public static final int PSP_HTTPS_CERT_THAWTE_PREMIUMSERVER_CA = 0x00000001;
    public static final int PSP_HTTPS_CERT_THAWTE_SERVER_CA = 0x00000002;
    public static final int PSP_HTTPS_CERT_THAWTE_ALL = (PSP_HTTPS_CERT_THAWTE_PREMIUMSERVER_CA | PSP_HTTPS_CERT_THAWTE_SERVER_CA);

    public static final int PSP_HTTPS_CERT_COMODO_ATE_CA_ROOT = 0x00000001;
    public static final int PSP_HTTPS_CERT_COMODO_AAA_CS = 0x00000002;
    public static final int PSP_HTTPS_CERT_COMODO_UTN_UFH = 0x00000004;
    public static final int PSP_HTTPS_CERT_COMODO_ALL = (PSP_HTTPS_CERT_COMODO_ATE_CA_ROOT | PSP_HTTPS_CERT_COMODO_AAA_CS | PSP_HTTPS_CERT_COMODO_UTN_UFH);

    // Error detail statics.
    public static final int PSP_HTTPS_ERROR_DETAIL_INTERNAL = 0x1;
    public static final int PSP_HTTPS_ERROR_DETAIL_INVALID_CERT = 0x2;
    public static final int PSP_HTTPS_ERROR_DETAIL_COMMON_NAME_CHECK = 0x4;
    public static final int PSP_HTTPS_ERROR_DETAIL_NOT_AFTER_CHECK = 0x8;
    public static final int PSP_HTTPS_ERROR_DETAIL_NOT_BEFORE_CHECK= 0x10;
    public static final int PSP_HTTPS_ERROR_DETAIL_INVALID_ROOT_CA = 0x20;

    // SSL flag statics (same values as error detail).
    public static final int PSP_HTTPS_SSL_FLAG_CHECK_SERVER = 0x1;
    public static final int PSP_HTTPS_SSL_FLAG_CHECK_CLIENT = 0x2;
    public static final int PSP_HTTPS_SSL_FLAG_CHECK_COMMON_NAME = 0x4;
    public static final int PSP_HTTPS_SSL_FLAG_CHECK_NOT_AFTER = 0x8;
    public static final int PSP_HTTPS_SSL_FLAG_CHECK_NOT_BEFORE = 0x10;
    public static final int PSP_HTTPS_SSL_FLAG_CHECK_VALID_ROOT_CA = 0x20;

    private boolean isHttpsInit;
    private HashMap<Integer, SslHandler> sslHandlers = new HashMap<Integer, SslHandler>();

    protected class SslHandler {
    	private int id;
        private int addr;
        private int pArg;

        private SslHandler(int id, int addr, int pArg) {
        	this.id = id;
        	this.addr = addr;
        	this.pArg = pArg;
        }

        protected void triggerHandler(int oldState, int newState, int event, int error) {
            SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
            if (thread != null) {
                Modules.ThreadManForUserModule.executeCallback(thread, addr, null, true, oldState, newState, event, error, pArg);
            }
        }

		@Override
		public String toString() {
			return String.format("SslHandler[id=%d, addr=0x%08X, pArg=0x%08X]", id, addr, pArg);
		}
    }

    protected void notifyHandler(int oldState, int newState, int event, int error) {
        for (SslHandler handler : sslHandlers.values()) {
            handler.triggerHandler(oldState, newState, event, error);
        }
    }

    public void sceHttpsInit(Processor processor) {
        CpuState cpu = processor.cpu;

        int rootCertNum = cpu.gpr[4];
        int rootCertListAddr = cpu.gpr[5];
        int clientCertAddr = cpu.gpr[6];
        int keyAddr = cpu.gpr[7];

        log.info("sceHttpsInit: rootCertNum=" + rootCertNum
                + ", rootCertListAddr=" + Integer.toHexString(rootCertListAddr)
                + ", clientCertAddr=" + Integer.toHexString(clientCertAddr)
                + ", keyAddr=" + Integer.toHexString(keyAddr));

        if (isHttpsInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_ALREADY_INIT;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void sceHttpsInitWithPath(Processor processor) {
        CpuState cpu = processor.cpu;

        int rootCertFileListAddr = cpu.gpr[4];
        int clientCertFileAddr = cpu.gpr[5];
        int keyFileAddr = cpu.gpr[6];

        log.info("sceHttpsInitWithPath: rootCertFileListAddr=" + Integer.toHexString(rootCertFileListAddr)
                + ", clientCertFileAddr=" + Integer.toHexString(clientCertFileAddr)
                + ", keyFileAddr=" + Integer.toHexString(keyFileAddr));

        if (isHttpsInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_ALREADY_INIT;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void sceHttpsEnd(Processor processor) {
        CpuState cpu = processor.cpu;

        log.info("sceHttpsEnd");

        if (!isHttpsInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_NOT_INIT;
        } else {
            isHttpsInit = false;
            cpu.gpr[2] = 0;
        }
    }

    public void sceHttpsLoadDefaultCert(Processor processor) {
        CpuState cpu = processor.cpu;

        int certIssuer = cpu.gpr[4];
        int certType = cpu.gpr[5];

        log.info("IGNORING: sceHttpsLoadDefaultCert: certIssuer=" + certIssuer
                + ", certType=" + certType);

        cpu.gpr[2] = 0;
    }

    public void sceHttpsGetSslError(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int sslIdAddr = cpu.gpr[4];
        int errorAddr = cpu.gpr[5];
        int errorDetailAddr = cpu.gpr[6];

        log.warn("PARTIAL: sceHttpsLoadDefaultCert: sslIdAddr=" + Integer.toHexString(sslIdAddr)
                + ", errorAddr=" + Integer.toHexString(errorAddr)
                + ", errorDetailAddr=" + Integer.toHexString(errorDetailAddr));

        if (Memory.isAddressGood(errorAddr) && Memory.isAddressGood(errorDetailAddr)) {
            mem.write32(errorAddr, 0);
            mem.write32(errorDetailAddr, 0);
            cpu.gpr[2] = 0;
        } else if (!isHttpsInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_NOT_INIT;
        } else {
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_INVALID_PARAMETER;
        }

        cpu.gpr[2] = 0;
    }

    public void sceHttpsEnableOption(Processor processor) {
        CpuState cpu = processor.cpu;

        int flag = cpu.gpr[4];

        log.warn("PARTIAL: sceHttpsEnableOption: flag=" + flag);

        if (!isHttpsInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_NOT_INIT;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void sceHttpsDisableOption(Processor processor) {
        CpuState cpu = processor.cpu;

        int flag = cpu.gpr[4];

        log.warn("PARTIAL: sceHttpsDisableOption: flag=" + flag);

        if (!isHttpsInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_NOT_INIT;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void sceHttpsGetCaList(Processor processor) {
        CpuState cpu = processor.cpu;

        int rootCAAddr = cpu.gpr[4];
        int rootCANumAddr = cpu.gpr[4];

        log.warn("IGNORING: sceHttpsGetCaList: rootCAAddr=" + Integer.toHexString(rootCAAddr)
                + ", rootCANumAddr=" + Integer.toHexString(rootCANumAddr));

        if (!isHttpsInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_NOT_INIT;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    public void sceHttpsSetSslCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        int sslID = cpu.gpr[4];
        int sslCallback = cpu.gpr[4];
		int sslArg = cpu.gpr[5];

        log.warn("PARTIAL: sceHttpsSetSslCallback: sslID=" + sslID
                + ", sslCallback=" + Integer.toHexString(sslCallback)
                + ", sslArg=" + Integer.toHexString(sslArg));

        if (!isHttpsInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_NOT_INIT;
        } else {
            SslHandler sslHandler = new SslHandler(sslID, sslCallback, sslArg);
            sslHandlers.put(sslID, sslHandler);
            cpu.gpr[2] = 0;
        }
    }

    public final HLEModuleFunction sceHttpsInitFunction = new HLEModuleFunction("sceHttps", "sceHttpsInit") {

        @Override
        public final void execute(Processor processor) {
            sceHttpsInit(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceHttpsModule.sceHttpsInit(processor);";
        }
    };

    public final HLEModuleFunction sceHttpsInitWithPathFunction = new HLEModuleFunction("sceHttps", "sceHttpsInitWithPath") {

        @Override
        public final void execute(Processor processor) {
            sceHttpsInitWithPath(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceHttpsModule.sceHttpsInitWithPath(processor);";
        }
    };

    public final HLEModuleFunction sceHttpsEndFunction = new HLEModuleFunction("sceHttps", "sceHttpsEnd") {

        @Override
        public final void execute(Processor processor) {
            sceHttpsEnd(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceHttpsModule.sceHttpsEnd(processor);";
        }
    };

    public final HLEModuleFunction sceHttpsLoadDefaultCertFunction = new HLEModuleFunction("sceHttps", "sceHttpsLoadDefaultCert") {

        @Override
        public final void execute(Processor processor) {
            sceHttpsLoadDefaultCert(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceHttpsModule.sceHttpsLoadDefaultCert(processor);";
        }
    };

    public final HLEModuleFunction sceHttpsGetSslErrorFunction = new HLEModuleFunction("sceHttps", "sceHttpsGetSslError") {

        @Override
        public final void execute(Processor processor) {
            sceHttpsGetSslError(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceHttpsModule.sceHttpsGetSslError(processor);";
        }
    };

    public final HLEModuleFunction sceHttpsEnableOptionFunction = new HLEModuleFunction("sceHttps", "sceHttpsEnableOption") {

        @Override
        public final void execute(Processor processor) {
            sceHttpsEnableOption(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceHttpsModule.sceHttpsEnableOption(processor);";
        }
    };

    public final HLEModuleFunction sceHttpsDisableOptionFunction = new HLEModuleFunction("sceHttps", "sceHttpsDisableOption") {

        @Override
        public final void execute(Processor processor) {
            sceHttpsDisableOption(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceHttpsModule.sceHttpsDisableOption(processor);";
        }
    };

    public final HLEModuleFunction sceHttpsGetCaListFunction = new HLEModuleFunction("sceHttps", "sceHttpsGetCaList") {

        @Override
        public final void execute(Processor processor) {
            sceHttpsGetCaList(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceHttpsModule.sceHttpsGetCaList(processor);";
        }
    };

    public final HLEModuleFunction sceHttpsSetSslCallbackFunction = new HLEModuleFunction("sceHttps", "sceHttpsSetSslCallback") {

        @Override
        public final void execute(Processor processor) {
            sceHttpsSetSslCallback(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceHttpsModule.sceHttpsSetSslCallback(processor);";
        }
    };
}