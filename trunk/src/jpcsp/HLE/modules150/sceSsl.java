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

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

@HLELogging
public class sceSsl extends HLEModule {
    public static Logger log = Modules.getLogger("sceSsl");

    @Override
    public String getName() {
        return "sceSsl";
    }

    private boolean isSslInit;
    private int maxMemSize;
    private int currentMemSize;

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
}