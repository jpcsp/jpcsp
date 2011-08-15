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
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;

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

    @HLEFunction(nid = 0xAB1ABE07, version = 150)
    public void sceHttpInit(Processor processor) {
        CpuState cpu = processor.cpu;

        int heapSize = cpu.gpr[4];

        log.info("sceHttpInit: heapSize=" + Integer.toHexString(heapSize));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isHttpInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_ALREADY_INIT;
        } else {
            maxMemSize = heapSize;
            isHttpInit = true;
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0xD1C8945E, version = 150)
    public void sceHttpEnd(Processor processor) {
        CpuState cpu = processor.cpu;

        log.info("sceHttpEnd");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!isHttpInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_NOT_INIT;
        } else {
            isSystemCookieLoaded = false;
            isHttpInit = false;
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x0282A3BD, version = 150)
    public void sceHttpGetContentLength(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpGetContentLength");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x03D9526F, version = 150)
    public void sceHttpSetResolveRetry(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetResolveRetry");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x06488A1C, version = 150)
    public void sceHttpSetCookieSendCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetCookieSendCallback");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x0809C831, version = 150)
    public void sceHttpEnableRedirect(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpEnableRedirect");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x0B12ABFB, version = 150)
    public void sceHttpDisableCookie(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpDisableCookie");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x0DAFA58F, version = 150)
    public void sceHttpEnableCookie(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpEnableCookie");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x15540184, version = 150)
    public void sceHttpDeleteHeader(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpDeleteHeader");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x1A0EBB69, version = 150)
    public void sceHttpDisableRedirect(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpDisableRedirect");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x1CEDB9D4, version = 150)
    public void sceHttpFlushCache(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpFlushCache");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x1F0FC3E3, version = 150)
    public void sceHttpSetRecvTimeOut(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetRecvTimeOut");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x2255551E, version = 150)
    public void sceHttpGetNetworkPspError(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpGetNetworkPspError");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x267618F4, version = 150)
    public void sceHttpSetAuthInfoCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetAuthInfoCallback");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x2A6C3296, version = 150)
    public void sceHttpSetAuthInfoCB(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetAuthInfoCB");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x2C3C82CF, version = 150)
    public void sceHttpFlushAuthList(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpFlushAuthList");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x3A67F306, version = 150)
    public void sceHttpSetCookieRecvCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetCookieRecvCallback");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x3EABA285, version = 150)
    public void sceHttpAddExtraHeader(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpAddExtraHeader");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x47347B50, version = 150)
    public void sceHttpCreateRequest(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpCreateRequest");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x47940436, version = 150)
    public void sceHttpSetResolveTimeOut(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetResolveTimeOut");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x4CC7D78F, version = 150)
    public void sceHttpGetStatusCode(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpGetStatusCode");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x5152773B, version = 150)
    public void sceHttpDeleteConnection(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpDeleteConnection");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x54E7DF75, version = 150)
    public void sceHttpIsRequestInCache(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpIsRequestInCache");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x59E6D16F, version = 150)
    public void sceHttpEnableCache(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpEnableCache");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x76D1363B, version = 150)
    public void sceHttpSaveSystemCookie(Processor processor) {
        CpuState cpu = processor.cpu;

        log.info("sceHttpSaveSystemCookie");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!isHttpInit) {
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_NOT_INIT;
        } else if (!isSystemCookieLoaded){
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_SYSTEM_COOKIE_NOT_LOADED;
        } else {
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0x7774BF4C, version = 150)
    public void sceHttpAddCookie(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpAddCookie");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x77EE5319, version = 150)
    public void sceHttpLoadAuthList(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpLoadAuthList");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x78A0D3EC, version = 150)
    public void sceHttpEnableKeepAlive(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpEnableKeepAlive");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x78B54C09, version = 150)
    public void sceHttpEndCache(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpEndCache");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x8ACD1F73, version = 150)
    public void sceHttpSetConnectTimeOut(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetConnectTimeOut");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x8EEFD953, version = 150)
    public void sceHttpCreateConnection(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpCreateConnection");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x951D310E, version = 150)
    public void sceHttpDisableProxyAuth(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpDisableProxyAuth");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x9668864C, version = 150)
    public void sceHttpSetRecvBlockSize(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetRecvBlockSize");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x96F16D3E, version = 150)
    public void sceHttpGetCookie(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpGetCookie");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x9988172D, version = 150)
    public void sceHttpSetSendTimeOut(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetSendTimeOut");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x9AFC98B2, version = 150)
    public void sceHttpSendRequestInCacheFirstMode(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSendRequestInCacheFirstMode");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x9B1F1F36, version = 150)
    public void sceHttpCreateTemplate(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpCreateTemplate");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0x9FC5F10D, version = 150)
    public void sceHttpEnableAuth(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpEnableAuth");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xA4496DE5, version = 150)
    public void sceHttpSetRedirectCallback(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetRedirectCallback");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xA5512E01, version = 150)
    public void sceHttpDeleteRequest(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpDeleteRequest");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xA6800C34, version = 150)
    public void sceHttpInitCache(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpInitCache");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xAE948FEE, version = 150)
    public void sceHttpDisableAuth(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpDisableAuth");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xB0C34B1D, version = 150)
    public void sceHttpSetCacheContentLengthMaxSize(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetCacheContentLengthMaxSize");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xB509B09E, version = 150)
    public void sceHttpCreateRequestWithURL(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpCreateRequestWithURL");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xBB70706F, version = 150)
    public void sceHttpSendRequest(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSendRequest");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xC10B6BD9, version = 150)
    public void sceHttpAbortRequest(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpAbortRequest");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xC6330B0D, version = 150)
    public void sceHttpChangeHttpVersion(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpChangeHttpVersion");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xC7EF2559, version = 150)
    public void sceHttpDisableKeepAlive(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpDisableKeepAlive");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xC98CBBA7, version = 150)
    public void sceHttpSetResHeaderMaxSize(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetResHeaderMaxSize");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xCCBD167A, version = 150)
    public void sceHttpDisableCache(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpDisableCache");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xCDB0DC58, version = 150)
    public void sceHttpEnableProxyAuth(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpEnableProxyAuth");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xCDF8ECB9, version = 150)
    public void sceHttpCreateConnectionWithURL(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpCreateConnectionWithURL");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xD081EC8F, version = 150)
    public void sceHttpGetNetworkErrno(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpGetNetworkErrno");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xD70D4847, version = 150)
    public void sceHttpGetProxy(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpGetProxy");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xDB266CCF, version = 150)
    public void sceHttpGetAllHeader(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpGetAllHeader");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xDD6E7857, version = 150)
    public void sceHttpSaveAuthList(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSaveAuthList");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xEDEEB999, version = 150)
    public void sceHttpReadData(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpReadData");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xF0F46C62, version = 150)
    public void sceHttpSetProxy(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetProxy");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xF1657B22, version = 150)
    public void sceHttpLoadSystemCookie(Processor processor) {
        CpuState cpu = processor.cpu;

        log.info("sceHttpLoadSystemCookie");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (isSystemCookieLoaded) { // The system's cookie list can only be loaded once per session.
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_ALREADY_INIT;
        } else if (maxMemSize <  PSP_HTTP_SYSTEM_COOKIE_HEAP_SIZE){
            cpu.gpr[2] = SceKernelErrors.ERROR_HTTP_NO_MEMORY;
        } else {
            isSystemCookieLoaded = true;
            cpu.gpr[2] = 0;
        }
    }

    @HLEFunction(nid = 0xF49934F6, version = 150)
    public void sceHttpSetMallocFunction(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpSetMallocFunction");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xFCF8C055, version = 150)
    public void sceHttpDeleteTemplate(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceHttpDeleteTemplate");

        cpu.gpr[2] = 0xDEADC0DE;
    }

}