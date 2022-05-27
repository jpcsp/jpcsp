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
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointer8;

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
    public int sceNpCommerce2DestroyReq(int requestId) {
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

    @HLEUnimplemented
    @HLEFunction(nid = 0x02C307C0, version = 150)
    public int sceNpCommerce2_02C307C0() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x02CADA41, version = 150)
    public int sceNpCommerce2_02CADA41() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x06618C81, version = 150)
    public int sceNpCommerce2_06618C81() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0A6AB496, version = 150)
    public int sceNpCommerce2_0A6AB496() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x157FA8AA, version = 150)
    public int sceNpCommerce2_157FA8AA() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1D9C32C0, version = 150)
    public int sceNpCommerce2_1D9C32C0() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x22CEC5E2, version = 150)
    public int sceNpCommerce2_22CEC5E2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x287B6FB3, version = 150)
    public int sceNpCommerce2_287B6FB3() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2AFD0CC7, version = 150)
    public int sceNpCommerce2_2AFD0CC7() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3546C737, version = 150)
    public int sceNpCommerce2_3546C737(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3755FC84, version = 150)
    public int sceNpCommerce2_3755FC84() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x382803C8, version = 150)
    public int sceNpCommerce2_382803C8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3D88DCCA, version = 150)
    public int sceNpCommerce2_3D88DCCA() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3F6D413C, version = 150)
    public int sceNpCommerce2_3F6D413C(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4484DB5A, version = 150)
    public int sceNpCommerce2_4484DB5A() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x44B8A9A9, version = 150)
    public int sceNpCommerce2_44B8A9A9(int unknown1, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer buffer, int bufferSize) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x460A1D92, version = 150)
    public int sceNpCommerce2_460A1D92() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x48D77A66, version = 150)
    public int sceNpCommerce2_48D77A66() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4D43B84D, version = 150)
    public int sceNpCommerce2_4D43B84D() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4DB0E04F, version = 150)
    public int sceNpCommerce2_4DB0E04F() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x51A88E18, version = 150)
    public int sceNpCommerce2_51A88E18() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5BDD4913, version = 150)
    public int sceNpCommerce2_5BDD4913() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5C691546, version = 150)
    public int sceNpCommerce2_5C691546() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x63B96D73, version = 150)
    public int sceNpCommerce2_63B96D73() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x66EC0829, version = 150)
    public int sceNpCommerce2UrlEncode(@CanBeNull TPointer escapedAddr, int escapedBufferLength, TPointer source, int unused, @CanBeNull TPointer32 escapedLengthAddr) {
    	return Modules.sceParseUriModule.sceUriEscape(escapedAddr, escapedLengthAddr, escapedBufferLength, source);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6795EE19, version = 150)
    public int sceNpCommerce2_6795EE19() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x69FEA1C8, version = 150)
    public int sceNpCommerce2_69FEA1C8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x73F9CD9E, version = 150)
    public int sceNpCommerce2_73F9CD9E() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x75DF99A9, version = 150)
    public int sceNpCommerce2_75DF99A9() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x77BE411B, version = 150)
    public int sceNpCommerce2_77BE411B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7B00DE40, version = 150)
    public int sceNpCommerce2_7B00DE40() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7EB30CAA, version = 150)
    public int sceNpCommerce2_7EB30CAA() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8026FA46, version = 150)
    public int sceNpCommerce2_8026FA46() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x80A1A7A0, version = 150)
    public int sceNpCommerce2_80A1A7A0() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8139E653, version = 150)
    public int sceNpCommerce2_8139E653() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x851E7844, version = 150)
    public int sceNpCommerce2_851E7844() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8642C532, version = 150)
    public int sceNpCommerce2_8642C532() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8E80E661, version = 150)
    public int sceNpCommerce2_8E80E661() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x993C29DB, version = 150)
    public int sceNpCommerce2_993C29DB() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xADAA054C, version = 150)
    public int sceNpCommerce2_ADAA054C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAE614CC6, version = 150)
    public int sceNpCommerce2_AE614CC6(int requestId, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer data, int dataSize) {
    	return Modules.sceHttpModule.sceHttpSendRequest(requestId, data, dataSize);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAED78BDE, version = 150)
    public int sceNpCommerce2_AED78BDE() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB7911D82, version = 150)
    public int sceNpCommerce2_B7911D82() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC27FEAB1, version = 150)
    public int sceNpCommerce2_C27FEAB1(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=1096, usage=Usage.in) TPointer unknown1, int httpRequestId, int unknown3, TPointer8 unknown4, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=60, usage=Usage.out) TPointer unknown5, int unknown6) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC3A3EAED, version = 150)
    public int sceNpCommerce2_C3A3EAED() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCBA487E9, version = 150)
    public int sceNpCommerce2_CBA487E9() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCCEEA788, version = 150)
    public int sceNpCommerce2_CCEEA788() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD4C319C6, version = 150)
    public int sceNpCommerce2_D4C319C6(int httpRequestId, int unknown2, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer unknown3, int unknown4, TPointer32 unknown5) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD5932A6C, version = 150)
    public int sceNpCommerce2_D5932A6C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDA206690, version = 150)
    public int sceNpCommerce2_DA206690() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE3455E27, version = 150)
    public int sceNpCommerce2_E3455E27() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE3EC9E14, version = 150)
    public int sceNpCommerce2_E3EC9E14() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEE7255FE, version = 150)
    public int sceNpCommerce2_EE7255FE() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF0900551, version = 150)
    public int sceNpCommerce2_F0900551() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF8D175FA, version = 150)
    public int sceNpCommerce2_F8D175FA() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFC47B63C, version = 150)
    public int sceNpCommerce2_FC47B63C() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFC687AC7, version = 150)
    public int sceNpCommerce2_FC687AC7(int unknown1, int unknown2, int unknown3, int unknown4, int unknown5, int unknown6) {
    	return 0;
    }
}
