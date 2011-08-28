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
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

public class sceHttp extends HLEModule {

    protected static Logger log = Modules.getLogger("sceHttp");

    @Override
    public String getName() {
        return "sceHttp";
    }
    public static final int PSP_HTTP_SYSTEM_COOKIE_HEAP_SIZE = 130 * 1024;
    private boolean isHttpInit;
    private boolean isSystemCookieLoaded;
    private int maxMemSize;
    
    public void checkHttpInit() {
    	if (!isHttpInit) {
    		throw(new SceKernelErrorException(SceKernelErrors.ERROR_HTTP_NOT_INIT));
    	}
    }

    @HLEFunction(nid = 0xAB1ABE07, version = 150, checkInsideInterrupt = true)
    public int sceHttpInit(int heapSize) {
        log.info("sceHttpInit: heapSize=" + Integer.toHexString(heapSize));

        if (isHttpInit) {
            return SceKernelErrors.ERROR_HTTP_ALREADY_INIT;
        }
        
        maxMemSize = heapSize;
        isHttpInit = true;
        return 0;
    }

    @HLEFunction(nid = 0xD1C8945E, version = 150, checkInsideInterrupt = true)
    public int sceHttpEnd() {
        log.info("sceHttpEnd");
        
        checkHttpInit();

        isSystemCookieLoaded = false;
        isHttpInit = false;
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0282A3BD, version = 150)
    public int sceHttpGetContentLength(){
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x03D9526F, version = 150)
    public int sceHttpSetResolveRetry() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x06488A1C, version = 150)
    public int sceHttpSetCookieSendCallback() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0809C831, version = 150)
    public int sceHttpEnableRedirect() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0B12ABFB, version = 150)
    public int sceHttpDisableCookie() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0DAFA58F, version = 150)
    public int sceHttpEnableCookie() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x15540184, version = 150)
    public int sceHttpDeleteHeader() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1A0EBB69, version = 150)
    public int sceHttpDisableRedirect() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1CEDB9D4, version = 150)
    public int sceHttpFlushCache() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1F0FC3E3, version = 150)
    public int sceHttpSetRecvTimeOut() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2255551E, version = 150)
    public int sceHttpGetNetworkPspError() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x267618F4, version = 150)
    public int sceHttpSetAuthInfoCallback() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2A6C3296, version = 150)
    public int sceHttpSetAuthInfoCB() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2C3C82CF, version = 150)
    public int sceHttpFlushAuthList() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3A67F306, version = 150)
    public int sceHttpSetCookieRecvCallback() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3EABA285, version = 150)
    public int sceHttpAddExtraHeader() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x47347B50, version = 150)
    public int sceHttpCreateRequest() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x47940436, version = 150)
    public int sceHttpSetResolveTimeOut() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4CC7D78F, version = 150)
    public int sceHttpGetStatusCode() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5152773B, version = 150)
    public int sceHttpDeleteConnection() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x54E7DF75, version = 150)
    public int sceHttpIsRequestInCache() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x59E6D16F, version = 150)
    public int sceHttpEnableCache() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x76D1363B, version = 150, checkInsideInterrupt = true)
    public int sceHttpSaveSystemCookie() {
        log.info("sceHttpSaveSystemCookie");
        
        checkHttpInit();

        if (!isSystemCookieLoaded){
            return SceKernelErrors.ERROR_HTTP_SYSTEM_COOKIE_NOT_LOADED;
        }
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7774BF4C, version = 150)
    public int sceHttpAddCookie() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x77EE5319, version = 150)
    public int sceHttpLoadAuthList() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x78A0D3EC, version = 150)
    public int sceHttpEnableKeepAlive() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x78B54C09, version = 150)
    public int sceHttpEndCache() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8ACD1F73, version = 150)
    public int sceHttpSetConnectTimeOut() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8EEFD953, version = 150)
    public int sceHttpCreateConnection() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x951D310E, version = 150)
    public int sceHttpDisableProxyAuth() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9668864C, version = 150)
    public int sceHttpSetRecvBlockSize() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x96F16D3E, version = 150)
    public int sceHttpGetCookie() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9988172D, version = 150)
    public int sceHttpSetSendTimeOut() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9AFC98B2, version = 150)
    public int sceHttpSendRequestInCacheFirstMode() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9B1F1F36, version = 150)
    public int sceHttpCreateTemplate() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9FC5F10D, version = 150)
    public int sceHttpEnableAuth() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA4496DE5, version = 150)
    public int sceHttpSetRedirectCallback() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA5512E01, version = 150)
    public int sceHttpDeleteRequest() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA6800C34, version = 150)
    public int sceHttpInitCache() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAE948FEE, version = 150)
    public int sceHttpDisableAuth() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB0C34B1D, version = 150)
    public int sceHttpSetCacheContentLengthMaxSize() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB509B09E, version = 150)
    public int sceHttpCreateRequestWithURL() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBB70706F, version = 150)
    public int sceHttpSendRequest() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC10B6BD9, version = 150)
    public int sceHttpAbortRequest() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC6330B0D, version = 150)
    public int sceHttpChangeHttpVersion() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC7EF2559, version = 150)
    public int sceHttpDisableKeepAlive() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC98CBBA7, version = 150)
    public int sceHttpSetResHeaderMaxSize() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCCBD167A, version = 150)
    public int sceHttpDisableCache() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCDB0DC58, version = 150)
    public int sceHttpEnableProxyAuth() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCDF8ECB9, version = 150)
    public int sceHttpCreateConnectionWithURL() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD081EC8F, version = 150)
    public int sceHttpGetNetworkErrno() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD70D4847, version = 150)
    public int sceHttpGetProxy() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDB266CCF, version = 150)
    public int sceHttpGetAllHeader() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDD6E7857, version = 150)
    public int sceHttpSaveAuthList() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEDEEB999, version = 150)
    public int sceHttpReadData() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF0F46C62, version = 150)
    public int sceHttpSetProxy() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF1657B22, version = 150, checkInsideInterrupt = true)
    public int sceHttpLoadSystemCookie() {
        log.info("sceHttpLoadSystemCookie");
        
        checkHttpInit();

        if (isSystemCookieLoaded) { // The system's cookie list can only be loaded once per session.
            return SceKernelErrors.ERROR_HTTP_ALREADY_INIT;
        } else if (maxMemSize <  PSP_HTTP_SYSTEM_COOKIE_HEAP_SIZE){
            return SceKernelErrors.ERROR_HTTP_NO_MEMORY;
        } else {
            isSystemCookieLoaded = true;
            return 0;
        }
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF49934F6, version = 150)
    public int sceHttpSetMallocFunction() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFCF8C055, version = 150)
    public int sceHttpDeleteTemplate() {
        return 0xDEADC0DE;
    }
}