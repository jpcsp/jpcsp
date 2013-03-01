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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_HTTP_ALREADY_INIT;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_HTTP_NOT_INIT;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.util.HashMap;

import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

@HLELogging
public class sceHttps extends HLEModule {
    public static Logger log = Modules.getLogger("sceHttps");

    @Override
    public String getName() {
        return "sceHttps";
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

    /**
     * Init the https library.
     *
     * @param rootCertNum - Pass 0
     * @param rootCertListAddr - Pass 0
     * @param clientCertAddr - Pass 0
     * @param keyAddr - Pass 0
     *
     * @return 0 on success, < 0 on error.
    */
    @HLEUnimplemented
    @HLEFunction(nid = 0xE4D21302, version = 150)
    public int sceHttpsInit(int rootCertNum, @CanBeNull TPointer rootCertListAddr, @CanBeNull TPointer clientCertAddr, @CanBeNull TPointer keyAddr) {
        if (isHttpsInit) {
            return ERROR_HTTP_ALREADY_INIT;
        }

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x68AB0F86, version = 150)
    public int sceHttpsInitWithPath(@CanBeNull PspString rootCertFileList, @CanBeNull PspString clientCertFile, @CanBeNull PspString keyFile) {
        if (isHttpsInit) {
        	return ERROR_HTTP_ALREADY_INIT;
        }

        return 0;
    }

    /**
     * Terminate the https library
     *
     * @return 0 on success, < 0 on error.
    */
    @HLEUnimplemented
    @HLEFunction(nid = 0xF9D8EB63, version = 150)
    public int sceHttpsEnd() {
        if (!isHttpsInit) {
        	return ERROR_HTTP_NOT_INIT;
        }

        isHttpsInit = false;

        return 0;
    }

    /**
     * Load default certificate
     *
     * @param certIssuer - Pass 0
     * @param certType - Pass 0
     * @return 0 on success, < 0 on error.
    */
    @HLEUnimplemented
    @HLEFunction(nid = 0x87797BDD, version = 150)
    public int sceHttpsLoadDefaultCert(int certIssuer, int certType) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAB1540D5, version = 150)
    public int sceHttpsGetSslError(TPointer sslIdAddr, TPointer32 errorAddr, TPointer32 errorDetailAddr) {
        if (!isHttpsInit) {
        	return ERROR_HTTP_NOT_INIT;
        }

        errorAddr.setValue(0);
        errorDetailAddr.setValue(0);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBAC31BF1, version = 150)
    public int sceHttpsEnableOption(int flag) {
        if (!isHttpsInit) {
        	return ERROR_HTTP_NOT_INIT;
        }

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB3FAF831, version = 150)
    public int sceHttpsDisableOption(int flag) {
        if (!isHttpsInit) {
        	return ERROR_HTTP_NOT_INIT;
        }

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD11DAB01, version = 150)
    public int sceHttpsGetCaList(TPointer rootCAAddr, TPointer32 rootCANumAddr) {
        if (!isHttpsInit) {
            return ERROR_HTTP_NOT_INIT;
        }

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x569A1481, version = 150)
    public int sceHttpsSetSslCallback(int sslID, TPointer sslCallback, int sslArg) {
        if (!isHttpsInit) {
            return ERROR_HTTP_NOT_INIT;
        }

        SslHandler sslHandler = new SslHandler(sslID, sslCallback.getAddress(), sslArg);
        sslHandlers.put(sslID, sslHandler);

        return 0;
    }
}