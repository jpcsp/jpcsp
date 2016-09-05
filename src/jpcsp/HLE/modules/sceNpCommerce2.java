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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;

import org.apache.log4j.Logger;

public class sceNpCommerce2 extends HLEModule {
    public static Logger log = Modules.getLogger("sceNpCommerce2");

    @HLEUnimplemented
    @HLEFunction(nid = 0x0E9956E3, version = 150)
    public int sceNpCommerce2Init() {
    	// No parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA5A34EA4, version = 150)
    public int sceNpCommerce2Term() {
    	// No parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x005B5F20, version = 150)
    public int sceNpCommerce2GetProductInfoStart() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x05E7AFBC, version = 150)
    public int sceNpCommerce2GetGameProductInfoFromContentInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1888A9FE, version = 150)
    public int sceNpCommerce2DestroyReq() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1C85ED88, version = 150)
    public int sceNpCommerce2GetPrice() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1C952DCB, version = 150)
    public int sceNpCommerce2GetGameProductInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2B25F6E9, version = 150)
    public int sceNpCommerce2CreateSessionStart() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3371D5F1, version = 150)
    public int sceNpCommerce2GetProductInfoCreateReq() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x490210E0, version = 150)
    public int sceNpCommerce2DestroyGetProductInfoResult() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4ECD4503, version = 150)
    public int sceNpCommerce2CreateSessionCreateReq() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x54FE8871, version = 150)
    public int sceNpCommerce2GetCategoryInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x57C8BD4E, version = 150)
    public int sceNpCommerce2GetCategoryInfoFromContentInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x590A3229, version = 150)
    public int sceNpCommerce2GetSessionInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5D8C2D99, version = 150)
    public int sceNpCommerce2GetCategoryContentsCreateReq() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6592FE83, version = 150)
    public int sceNpCommerce2GetContentRatingDescriptor() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6A2AE572, version = 150)
    public int sceNpCommerce2GetContentRatingInfoFromGameProductInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6F1FE37F, version = 150)
    public int sceNpCommerce2CreateCtx() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x80A7AFDB, version = 150)
    public int sceNpCommerce2GetCategoryContentsGetResult() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAA4A1E3D, version = 150)
    public int sceNpCommerce2GetProductInfoGetResult() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB0D7AA90, version = 150)
    public int sceNpCommerce2GetContentInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBC61FFC8, version = 150)
    public int sceNpCommerce2CreateSessionGetResult() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC5505A19, version = 150)
    public int sceNpCommerce2GetContentRatingInfoFromCategoryInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC7F32242, version = 150)
    public int sceNpCommerce2AbortReq() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCEAB1829, version = 150)
    public int sceNpCommerce2InitGetCategoryContentsResult() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDD6758FA, version = 150)
    public int sceNpCommerce2GetCategoryContentsStart() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xED85ACCE, version = 150)
    public int sceNpCommerce2DestroyGetCategoryContentsResult() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF2278B90, version = 150)
    public int sceNpCommerce2GetGameSkuInfoFromGameProductInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF297AB9C, version = 150)
    public int sceNpCommerce2DestroyCtx() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFC30C19E, version = 150)
    public int sceNpCommerce2InitGetProductInfoResult() {
    	return 0;
    }
}
