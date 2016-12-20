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
package jpcsp.HLE.modules;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.HLE.Modules;

import org.apache.log4j.Logger;

public class sceSsl extends HLEModule {
    public static Logger log = Modules.getLogger("sceSsl");

    private boolean isSslInit;
    private int maxMemSize;
    private int currentMemSize;
    private SysMemInfo cryptoMalloc;

    @HLELogging(level="info")
    @HLEFunction(nid = 0x957ECBE2, version = 150)
    public int sceSslInit(int heapSize) {
        if (isSslInit) {
            return SceKernelErrors.ERROR_SSL_ALREADY_INIT;
        }
        if (heapSize <= 0) {
            return SceKernelErrors.ERROR_SSL_INVALID_PARAMETER;
        }

        maxMemSize = heapSize;
        currentMemSize = heapSize / 2; // Dummy value.
        isSslInit = true;

        return 0;
    }

    @HLELogging(level="info")
    @HLEFunction(nid = 0x191CDEFF, version = 150)
    public int sceSslEnd() {
        if (!isSslInit) {
            return SceKernelErrors.ERROR_SSL_NOT_INIT;
        }

        isSslInit = false;

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5BFB6B61, version = 150)
    public int sceSslGetNotAfter(@CanBeNull TPointer sslCertAddr, @CanBeNull TPointer endTimeAddr) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x17A10DCC, version = 150)
    public int sceSslGetNotBefore(@CanBeNull TPointer sslCertAddr, @CanBeNull TPointer startTimeAddr) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3DD5E023, version = 150)
    public int sceSslGetSubjectName() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1B7C8191, version = 150)
    public int sceSslGetIssuerName() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCC0919B0, version = 150)
    public int sceSslGetSerialNumber() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x058D21C0, version = 150)
    public int sceSslGetNameEntryCount() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD6D097B4, version = 150)
    public int sceSslGetNameEntryInfo() {
    	return 0;
    }

    @HLEFunction(nid = 0xB99EDE6A, version = 150)
    public int sceSslGetUsedMemoryMax(TPointer32 maxMemAddr) {
    	if (!isSslInit) {
    		return SceKernelErrors.ERROR_SSL_NOT_INIT;
    	}

    	maxMemAddr.setValue(maxMemSize);

        return 0;
    }

    @HLEFunction(nid = 0x0EB43B06, version = 150)
    public int sceSslGetUsedMemoryCurrent(TPointer32 currentMemAddr) {
    	if (!isSslInit) {
    		return SceKernelErrors.ERROR_SSL_NOT_INIT;
    	}

    	currentMemAddr.setValue(currentMemSize);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF57765D3, version = 150)
    public int sceSslGetKeyUsage() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9266C0D5, version = 150)
    public int sceSsl_9266C0D5() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB40D11EA, version = 150)
    public int SSLv3_client_method() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFB8273FE, version = 150)
    public int SSL_CTX_new(int method) {
    	return 0x12345678;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x588F2FE8, version = 150)
    public int SSL_CTX_free(int ctx) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB4D78E98, version = 150)
    public int SSL_CTX_ctrl(int ctx, int cmd, int larg, int parg) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAEBF278B, version = 150)
    public int SSL_CTX_set_verify(int ctx, int mode, @CanBeNull TPointer verify_callback) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x529A9477, version = 150)
    public int sceSsl_lib_529A9477(int ctx, TPointer unknown1, int unknown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBF55C31C, version = 150)
    public int SSL_CTX_set_client_cert_cb(int ctx, @CanBeNull TPointer client_cert_cb) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0861D934, version = 150)
    public int CRYPTO_malloc(int size) {
    	cryptoMalloc = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "CRYPTO_malloc", SysMemUserForUser.PSP_SMEM_Low, size, 0);
    	if (cryptoMalloc == null) {
    		return 0;
    	}
    	return cryptoMalloc.addr;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5E5C873A, version = 150)
    public int CRYPTO_free(int allocatedAddress) {
    	if (cryptoMalloc != null && cryptoMalloc.addr == allocatedAddress) {
    		Modules.SysMemUserForUserModule.free(cryptoMalloc);
    		cryptoMalloc = null;
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEBFB0E3C, version = 150)
    public int SSL_new(int ctx) {
    	return 1;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x84833472, version = 150)
    public int SSL_free(int ssl) {
    	return 1;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x28B4DE33, version = 150)
    public int BIO_new_socket(int socket, int closeFlag) {
    	return 1;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB9C8CCE6, version = 150)
    public void SSL_set_bio(int ssl, int rbio, int wbio) {
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xECE07B61, version = 150)
    public int sceSsl_lib_ECE07B61(int bio, int unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x80608663, version = 150)
    public void SSL_set_connect_state(int ssl) {
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB7CA8717, version = 150)
    public int SSL_write(int ssl, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer buffer, int length) {
    	return length;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB3B04C58, version = 150)
    public int SSL_get_error(int ssl, int returnValue) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE7C29542, version = 150)
    public int SSL_read(int ssl, @BufferInfo(lengthInfo=LengthInfo.returnValue, usage=Usage.out) TPointer buffer, int size) {
    	return 0;
    }
}