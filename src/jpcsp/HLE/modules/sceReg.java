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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.sceFont.FontRegistryEntry;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

public class sceReg extends HLEModule {
    public static Logger log = Modules.getLogger("sceReg");
    protected static final int REG_TYPE_DIR = 1;
    protected static final int REG_TYPE_INT = 2;
    protected static final int REG_TYPE_STR = 3;
    protected static final int REG_TYPE_BIN = 4;
    protected static final int REG_MODE_READ_WRITE = 1;
    protected static final int REG_MODE_READ_ONLY = 2;
    private Map<Integer, RegistryHandle> registryHandles;
    private Map<Integer, CategoryHandle> categoryHandles;
    private Map<Integer, KeyHandle> keyHandles;
    private String authName;
    private String authKey;
    private int networkLatestId;
    private int wifiConnectCount;
    private int usbConnectCount;
    private int psnAccountCount;
    private int slideCount;
    private int bootCount;
    private int gameExecCount;
    private int oskVersionId;
    private int oskDispLocale;
    private int oskWritingLocale;
    private int oskInputCharMask;
    private int oskKeytopIndex;
    private String npEnv;
    private String adhocSsidPrefix;
    private int musicVisualizerMode;
    private int musicTrackInfoMode;
    private String lockPassword;
    private String browserHomeUri;
    private String npAccountId;
    private String npLoginId;
    private String npPassword;
    private int npAutoSignInEnable;
    private String ownerName;

    protected static class RegistryHandle {
    	private static final String registryHandlePurpose = "sceReg.RegistryHandle";
    	public int uid;
    	public int type;
    	public String name;
    	public int unknown1;
    	public int unknown2;

    	public RegistryHandle(int type, String name, int unknown1, int unknown2) {
			this.type = type;
			this.name = name;
			this.unknown1 = unknown1;
			this.unknown2 = unknown2;
			uid = SceUidManager.getNewUid(registryHandlePurpose);
		}

    	public void release() {
    		SceUidManager.releaseUid(uid, registryHandlePurpose);
    		uid = -1;
    	}
    }

    protected static class CategoryHandle {
    	private static final String categoryHandlePurpose = "sceReg.CategoryHandle";
    	public int uid;
    	public RegistryHandle registryHandle;
		public String name;
    	public int mode;

    	public CategoryHandle(RegistryHandle registryHandle, String name, int mode) {
			this.registryHandle = registryHandle;
			this.name = name;
			this.mode = mode;
			uid = SceUidManager.getNewUid(categoryHandlePurpose);
		}

    	public String getFullName() {
    		return registryHandle.name + name;
    	}

    	public void release() {
    		SceUidManager.releaseUid(uid, categoryHandlePurpose);
    		uid = -1;
    	}
    }

    protected static class KeyHandle {
    	private static int index = 0;
    	public int uid;
    	public String name;

    	public KeyHandle(String name) {
    		this.name = name;
			uid = index++;
    	}
    }

	public String getAuthName() {
		return authName;
	}

	public void setAuthName(String authName) {
		this.authName = authName;
	}

	public String getAuthKey() {
		return authKey;
	}

	public void setAuthKey(String authKey) {
		this.authKey = authKey;
	}

	public int getNetworkLatestId() {
		return networkLatestId;
	}

	public void setNetworkLatestId(int networkLatestId) {
		this.networkLatestId = networkLatestId;
	}

	public String getNpLoginId() {
		return npLoginId;
	}

	public String getNpPassword() {
		return npPassword;
	}

	private int getKey(CategoryHandle categoryHandle, String name, TPointer32 ptype, TPointer32 psize, TPointer buf, int size) {
    	String fullName = categoryHandle.getFullName();
    	fullName = fullName.replace("flash1:/registry/system", "");
    	fullName = fullName.replace("flash1/registry/system", "");
    	fullName = fullName.replace("flash2/registry/system", "");

    	Settings settings = Settings.getInstance();
    	if ("/system/DATA/FONT".equals(fullName) || "/DATA/FONT".equals(fullName)) {
    		List<sceFont.FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
    		if ("path_name".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(Modules.sceFontModule.getFontDirPath().length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, Modules.sceFontModule.getFontDirPath());
    			}
    		} else if ("num_fonts".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(fontRegistry.size());
    			}
    		} else {
    			log.warn(String.format("Unknown font registry entry '%s'", name));
    		}
    	} else if (fullName.startsWith("/system/DATA/FONT/PROPERTY/INFO") || fullName.startsWith("/DATA/FONT/PROPERTY/INFO")) {
    		List<sceFont.FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
    		int index = Integer.parseInt(fullName.substring(fullName.indexOf("INFO") + 4));
    		if (index < 0 || index >= fontRegistry.size()) {
    			return -1;
    		}
    		FontRegistryEntry entry = fontRegistry.get(index);
    		if ("h_size".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(entry.h_size);
    			}
    		} else if ("v_size".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(entry.v_size);
    			}
    		} else if ("h_resolution".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.h_resolution);
    			}
    		} else if ("v_resolution".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.v_resolution);
    			}
    		} else if ("extra_attributes".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.extra_attributes);
    			}
    		} else if ("weight".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.weight);
    			}
    		} else if ("family_code".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.family_code);
    			}
    		} else if ("style".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.style);
    			}
    		} else if ("sub_style".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.sub_style);
    			}
    		} else if ("language_code".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.language_code);
    			}
    		} else if ("region_code".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.region_code);
    			}
    		} else if ("country_code".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.country_code);
    			}
    		} else if ("file_name".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(entry.file_name == null ? 0 : entry.file_name.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, entry.file_name);
    			}
    		} else if ("font_name".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(entry.font_name == null ? 0 : entry.font_name.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, entry.font_name);
    			}
    		} else if ("expire_date".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.expire_date);
    			}
    		} else if ("shadow_option".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(entry.shadow_option);
    			}
    		} else {
    			log.warn(String.format("Unknown font registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/DATE".equals(fullName)) {
    		if ("date_format".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.date_format", 2));
    			}
    		} else if ("time_format".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.time_format", 0));
    			}
    		} else if ("time_zone_offset".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.time_zone_offset", 0));
    			}
    		} else if ("time_zone_area".equals(name)) {
				String timeZoneArea = settings.readString("registry.time_zone_area", "united_kingdom");
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(timeZoneArea.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, timeZoneArea);
    			}
    		} else if ("summer_time".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.summer_time", 0));
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/BROWSER".equals(fullName)) {
    		if ("flash_activated".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("home_uri".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(0x200);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, browserHomeUri);
    			}
    		} else if ("cookie_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("proxy_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(2);
    			}
    		} else if ("proxy_address".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(0x80);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, "");
    			}
    		} else if ("proxy_port".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("picture".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("animation".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("javascript".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("cache_size".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0x200); // Cache Size in KB
    			}
    		} else if ("char_size".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("disp_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("flash_play".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("connect_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("proxy_protect".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("proxy_autoauth".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("proxy_user".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(0x80);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, "");
    			}
    		} else if ("proxy_password".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(0x80);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, "");
    			}
    		} else if ("webpage_quality".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/BROWSER2".equals(fullName)) {
    		if ("tm_service".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("tm_ec_ttl".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("tm_ec_ttl_update_time".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/NP".equals(fullName)) {
    		if ("account_id".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(16);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npAccountId);
    			}
    		} else if ("login_id".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(npLoginId.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npLoginId);
    			}
    		} else if ("password".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(npPassword.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npPassword);
    			}
    		} else if ("auto_sign_in_enable".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(npAutoSignInEnable);
    			}
    		} else if ("nav_only".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("env".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(npEnv.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npEnv);
    			}
    		} else if ("guest_country".equals(name)) {
    			String guestCount = "";
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(guestCount.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, guestCount);
    			}
    		} else if ("view_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("check_drm".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/PREMO".equals(fullName)) {
    		if ("ps3_mac".equals(name)) {
    			byte ps3Mac[] = new byte[6];
    			ps3Mac[0] = 0x11;
    			ps3Mac[1] = 0x22;
    			ps3Mac[2] = 0x33;
    			ps3Mac[3] = 0x44;
    			ps3Mac[4] = 0x55;
    			ps3Mac[5] = 0x66;
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(ps3Mac.length);
    			if (size > 0) {
    				buf.setArray(ps3Mac, ps3Mac.length);
    			}
    		} else if ("ps3_name".equals(name)) {
    			String ps3Name = "My PS3";
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(ps3Name.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, ps3Name);
    			}
    		} else if ("guide_page".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("response".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("custom_video_buffer1".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("custom_video_bitrate1".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("setting_internet".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("custom_video_buffer2".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("custom_video_bitrate2".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("button_assign".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("ps3_keytype".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("ps3_key".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(16);
    			if (size >= 16) {
    				buf.clear(16);
    			}
    		} else if ("flags".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("account_id".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(16);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npAccountId);
    			}
    		} else if ("login_id".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(npLoginId.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npLoginId);
    			}
    		} else if ("password".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(npPassword.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, npPassword);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM".equals(fullName)) {
    		if ("exh_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("umd_autoboot".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("usb_auto_connect".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(1);
    			}
    		} else if ("owner_mob".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("owner_dob".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("umd_cache".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else if ("owner_name".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(ownerName.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, ownerName);
    			}
    		} else if ("slide_welcome".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(1);
    			}
    		} else if ("first_boot_tick".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			String firstBootTick = "";
    			psize.setValue(firstBootTick.length());
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, firstBootTick);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/SOUND".equals(fullName)) {
    		if ("dynamic_normalizer".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("operation_sound_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(1);
    			}
    		} else if ("avls".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/CHARACTER_SET".equals(fullName)) {
    		if ("oem".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(5);
    			}
    		} else if ("ansi".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(0x13);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/XMB".equals(fullName)) {
    		if ("language".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(sceUtility.getSystemParamLanguage());
    			}
    		} else if ("button_assign".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(sceUtility.getSystemParamButtonPreference());
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/XMB/THEME".equals(fullName)) {
    		if ("wallpaper_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.theme.wallpaper_mode", 0));
    			}
    		} else if ("custom_theme_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.theme.custom_theme_mode", 0));
    			}
    		} else if ("color_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.theme.color_mode", 0));
    			}
    		} else if ("system_color".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(settings.readInt("registry.theme.system_color", 0));
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/SYSPROFILE/RESOLUTION".equals(fullName)) {
    		if ("horizontal".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(8210);
    			}
    		} else if ("vertical".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(8210);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/ALARM".equals(fullName)) {
    		if (name.matches("alarm_\\d+_time")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(-1);
    			}
    		} else if (name.matches("alarm_\\d+_property")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/NETWORK/GO_MESSENGER".equals(fullName)) {
    		if (name.equals("auth_name")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authName.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authName);
    			}
    		} else if (name.equals("auth_key")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authKey.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authKey);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/NETWORK/ADHOC".equals(fullName)) {
    		if (name.equals("ssid_prefix")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(adhocSsidPrefix.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, adhocSsidPrefix);
    			}
    		} else if (name.equals("channel")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(sceUtility.getSystemParamAdhocChannel());
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/NETWORK/INFRASTRUCTURE".equals(fullName)) {
    		if ("latest_id".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(networkLatestId);
    			}
    		} else if (name.equals("eap_md5")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if (name.equals("auto_setting")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if (name.equals("wifisvc_setting")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if (fullName.matches("/CONFIG/NETWORK/INFRASTRUCTURE/\\d+")) {
    		String indexName = fullName.replace("/CONFIG/NETWORK/INFRASTRUCTURE/", "");
    		int index = Integer.parseInt(indexName);
            if ("cnf_name".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String cnfName = sceUtility.getNetParamName(index);
    			psize.setValue(cnfName.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, cnfName);
    			}
    		} else if ("ssid".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String ssid = sceNetApctl.getSSID();
    			psize.setValue(ssid.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, ssid);
    			}
    		} else if ("auth_proto".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is no security.
                    // 1 is WEP (64bit).
                    // 2 is WEP (128bit).
                    // 3 is WPA.
    				buf.setValue32(1);
    			}
    		} else if ("wep_key".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			String wepKey = "XXXXXXXXXXXXX"; // Max length is 13
    			psize.setValue(wepKey.length());
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, wepKey);
    			}
    		} else if ("how_to_set_ip".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is DHCP.
                    // 1 is static.
                    // 2 is PPPOE.
    				buf.setValue32(0);
    			}
    		} else if ("dns_flag".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is auto.
                    // 1 is manual.
    				buf.setValue32(0);
    			}
    		} else if ("primary_dns".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String dns = sceNetApctl.getPrimaryDNS();
    			psize.setValue(dns.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, dns);
    			}
    		} else if ("secondary_dns".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String dns = sceNetApctl.getSecondaryDNS();
    			psize.setValue(dns.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, dns);
    			}
    		} else if ("http_proxy_flag".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is to not use proxy.
                    // 1 is to use proxy.
    				buf.setValue32(0);
    			}
    		} else if ("http_proxy_server".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String httpProxyServer = "";
    			psize.setValue(httpProxyServer.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, httpProxyServer);
    			}
    		} else if ("http_proxy_port".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(80);
    			}
    		} else if ("version".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is not used.
                    // 1 is old version.
                    // 2 is new version.
    				buf.setValue32(2);
    			}
    		} else if ("auth_8021x_type".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is none.
                    // 1 is EAP (MD5).
    				buf.setValue32(0);
    			}
    		} else if ("browser_flag".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
                    // 0 is to not start the native browser.
                    // 1 is to start the native browser.
    				buf.setValue32(0);
    			}
    		} else if ("ip_address".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String ip = sceNetApctl.getLocalHostIP();
    			psize.setValue(ip.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, ip);
    			}
    		} else if ("netmask".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String netmask = sceNetApctl.getSubnetMask();
    			psize.setValue(netmask.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, netmask);
    			}
    		} else if ("default_route".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String gateway = sceNetApctl.getGateway();
    			psize.setValue(gateway.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, gateway);
    			}
    		} else if (name.equals("device")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if (name.equals("auth_name")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authName.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authName);
    			}
    		} else if (name.equals("auth_key")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authKey.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authKey);
    			}
    		} else if (name.equals("auth_8021x_auth_name")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authName.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authName);
    			}
    		} else if (name.equals("auth_8021x_auth_key")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authKey.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authKey);
    			}
    		} else if ("wpa_key_type".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if (name.equals("wpa_key")) {
    			ptype.setValue(REG_TYPE_BIN);
    			String wpaKey = "";
    			psize.setValue(wpaKey.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, wpaKey);
    			}
    		} else if ("wifisvc_config".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if (fullName.matches("/CONFIG/NETWORK/INFRASTRUCTURE/\\d+/SUB1")) {
    		String indexName = fullName.replace("/CONFIG/NETWORK/INFRASTRUCTURE/", "");
    		int index = Integer.parseInt(indexName.substring(0, indexName.indexOf("/")));
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("/CONFIG/NETWORK/INFRASTRUCTURE, index=%d, SUB1", index));
    		}
    		if ("last_leased_dhcp_addr".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			String lastLeasedDhcpAddr = "";
    			psize.setValue(lastLeasedDhcpAddr.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, lastLeasedDhcpAddr);
    			}
    		} else if (name.equals("wifisvc_auth_name")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authName.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authName);
    			}
    		} else if (name.equals("wifisvc_auth_key")) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(authKey.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, authKey);
    			}
    		} else if (name.equals("wifisvc_option")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if (name.equals("bt_id")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if (name.equals("at_command")) {
    			ptype.setValue(REG_TYPE_STR);
    			String atCommand = "";
    			psize.setValue(atCommand.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, atCommand);
    			}
    		} else if (name.equals("phone_number")) {
    			ptype.setValue(REG_TYPE_STR);
    			String phoneNumber = "";
    			psize.setValue(phoneNumber.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, phoneNumber);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/DATA/COUNT".equals(fullName)) {
    		if ("wifi_connect_count".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(wifiConnectCount);
    			}
    		} else if (name.equals("usb_connect_count")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(usbConnectCount);
    			}
    		} else if (name.equals("psn_access_count")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(psnAccountCount);
    			}
    		} else if (name.equals("slide_count")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(slideCount);
    			}
    		} else if (name.equals("boot_count")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(bootCount);
    			}
    		} else if (name.equals("game_exec_count")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(gameExecCount);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/LOCK".equals(fullName)) {
    		if ("parental_level".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("browser_start".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("password".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(lockPassword.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, lockPassword);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/POWER_SAVING".equals(fullName)) {
    		if ("backlight_off_interval".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("suspend_interval".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("wlan_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("active_backlight_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/TOOL/CONFIG".equals(fullName)) {
    		if ("np_debug".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/REGISTRY".equals(fullName)) {
    		if ("category_version".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0x66);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/OSK".equals(fullName)) {
    		if ("version_id".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(oskVersionId);
    			}
    		} else if (name.equals("disp_locale")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(oskDispLocale);
    			}
    		} else if (name.equals("writing_locale")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(oskWritingLocale);
    			}
    		} else if (name.equals("input_char_mask")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(oskInputCharMask);
    			}
    		} else if (name.equals("keytop_index")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(oskKeytopIndex);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/MUSIC".equals(fullName)) {
    		if ("visualizer_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(musicVisualizerMode);
    			}
    		} else if (name.equals("track_info_mode")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(musicTrackInfoMode);
    			}
    		} else if (name.equals("wma_play")) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/PHOTO".equals(fullName)) {
    		if ("slideshow_speed".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/VIDEO".equals(fullName)) {
    		if ("lr_button_enable".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("list_play_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("title_display_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("sound_language".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(2);
    			if (size >= 2) {
    				buf.setValue8(0, (byte) '0');
    				buf.setValue8(1, (byte) '0');
    			}
    		} else if ("subtitle_language".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(2);
    			if (size >= 2) {
    				buf.setValue8(0, (byte) 'e');
    				buf.setValue8(1, (byte) 'n');
    			}
    		} else if ("menu_language".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(2);
    			if (size >= 2) {
    				buf.setValue8(0, (byte) 'e');
    				buf.setValue8(1, (byte) 'n');
    			}
    		} else if ("appended_volume".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/INFOBOARD".equals(fullName)) {
    		if ("locale_lang".equals(name)) {
    			String localeLang = "en/en/rss.xml";
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(localeLang.length() + 1);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, localeLang);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/CAMERA".equals(fullName)) {
    		if ("still_size".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("still_quality".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("movie_size".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("movie_quality".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("white_balance".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("exposure_bias".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("still_effect".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("file_folder".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0x65);
    			}
    		} else if ("file_number".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("movie_fps".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("shutter_sound_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("file_number_eflash".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("folder_number_eflash".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0x65);
    			}
    		} else if ("msid".equals(name)) {
    			String msid = "";
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(16);
    			if (size > 0) {
    				Utilities.writeStringNZ(buf.getMemory(), buf.getAddress(), size, msid);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/RSS".equals(fullName)) {
    		if ("download_items".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(5);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/DISPLAY".equals(fullName)) {
    		if ("color_space_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else if ("screensaver_start_time".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(0);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else {
			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    	}

    	return 0;
    }

    @Override
	public void start() {
		registryHandles = new HashMap<Integer, sceReg.RegistryHandle>();
		categoryHandles = new HashMap<Integer, sceReg.CategoryHandle>();
		keyHandles = new HashMap<Integer, sceReg.KeyHandle>();

		// TODO Read these values from the configuration file
		Settings settings = Settings.getInstance();
		authName = "";
		authKey = "";
		networkLatestId = 0;
		wifiConnectCount = 0;
		usbConnectCount = 0;
		gameExecCount = 0;
	    oskVersionId = 0x226;
	    oskDispLocale = 0x1;
	    oskWritingLocale = 0x1;
	    oskInputCharMask = 0xF;
	    oskKeytopIndex = 0x5;
	    npEnv = "np"; // Max length 8
	    adhocSsidPrefix = "PSP"; // Must be of length 3
	    musicVisualizerMode = 0;
	    musicTrackInfoMode = 1;
	    lockPassword = "0000"; // 4-digit password
	    browserHomeUri = "";
	    npAccountId = settings.readString("registry.npAccountId");
	    npLoginId = settings.readString("registry.npLoginId");
	    npPassword = settings.readString("registry.npPassword");
	    npAutoSignInEnable = settings.readInt("registry.npAutoSignInEnable");
		ownerName = sceUtility.getSystemParamNickname();

		super.start();
	}

    @HLEFunction(nid = 0x92E41280, version = 150)
    public int sceRegOpenRegistry(TPointer reg, int mode, @BufferInfo(usage=Usage.out) TPointer32 h) {
    	int regType = reg.getValue32(0);
    	int nameLen = reg.getValue32(260);
    	int unknown1 = reg.getValue32(264);
    	int unknown2 = reg.getValue32(268);
    	String name = Utilities.readStringNZ(reg.getAddress() + 4, nameLen);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("RegParam: regType=%d, name='%s'(len=%d), unknown1=%d, unknown2=%d", regType, name, nameLen, unknown1, unknown2));
    	}

    	RegistryHandle registryHandle = new RegistryHandle(regType, name, unknown1, unknown2);
    	registryHandles.put(registryHandle.uid, registryHandle);

    	h.setValue(registryHandle.uid);

    	return 0;
    }

    @HLEFunction(nid = 0xFA8A5739, version = 150)
    public int sceRegCloseRegistry(int h) {
    	RegistryHandle registryHandle = registryHandles.get(h);
    	if (registryHandle == null) {
    		return -1;
    	}

    	registryHandle.release();
    	registryHandles.remove(h);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDEDA92BF, version = 150)
    public int sceRegRemoveRegistry(TPointer reg) {
    	return 0;
    }

    @HLEFunction(nid = 0x1D8A762E, version = 150)
    public int sceRegOpenCategory(int h, String name, int mode, @BufferInfo(usage=Usage.out) TPointer32 hd) {
    	RegistryHandle registryHandle = registryHandles.get(h);
    	if (registryHandle == null) {
    		return -1;
    	}
    	CategoryHandle categoryHandle = new CategoryHandle(registryHandle, name, mode);
    	categoryHandles.put(categoryHandle.uid, categoryHandle);
    	hd.setValue(categoryHandle.uid);

    	if (categoryHandle.getFullName().startsWith("/system/DATA/FONT/PROPERTY/INFO")) {
    		List<sceFont.FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
    		int index = Integer.parseInt(categoryHandle.getFullName().substring(31));
    		if (index < 0 || index >= fontRegistry.size()) {
    			if (mode != REG_MODE_READ_WRITE) {
    				return SceKernelErrors.ERROR_REGISTRY_NOT_FOUND;
    			}
    		}
    	} else if (categoryHandle.getFullName().startsWith("flash2/registry/system/CONFIG/NETWORK/INFRASTRUCTURE/")) {
    		String indexString = categoryHandle.getFullName().substring(53);
    		int sep = indexString.indexOf('/');
    		if (sep >= 0) {
    			indexString = indexString.substring(0, sep);
    		}
    		int index = Integer.parseInt(indexString);
    		// We do not return too many entries as some homebrew only support a limited number of entries.
    		if (index > sceUtility.PSP_NETPARAM_MAX_NUMBER_DUMMY_ENTRIES) {
    			return SceKernelErrors.ERROR_REGISTRY_NOT_FOUND;
    		}
    	}

    	return 0;
    }

    @HLEFunction(nid = 0x0CAE832B, version = 150)
    public int sceRegCloseCategory(int hd) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	categoryHandle.release();
    	categoryHandles.remove(hd);

    	return 0;
    }

    @HLEFunction(nid = 0x39461B4D, version = 150)
    public int sceRegFlushRegistry(int h) {
    	RegistryHandle registryHandle = registryHandles.get(h);
    	if (registryHandle == null) {
    		return -1;
    	}
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0D69BF40, version = 150)
    public int sceRegFlushCategory(int hd) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}
    	return 0;
    }

    @HLEFunction(nid = 0x57641A81, version = 150)
    public int sceRegCreateKey(int hd, String name, int type, int size) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	if (categoryHandle.getFullName().startsWith("/system/DATA/FONT/PROPERTY/INFO")) {
			List<sceFont.FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
			int index = Integer.parseInt(categoryHandle.getFullName().substring(31));
			if (index < 0 || index > fontRegistry.size()) {
				return -1;
			} else if (index == fontRegistry.size()) {
				log.info(String.format("sceRegCreateKey creating a new font entry '%s'", categoryHandle.getFullName()));
				FontRegistryEntry entry = new FontRegistryEntry();
				fontRegistry.add(entry);
	    		if ("h_size".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("v_size".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("h_resolution".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("v_resolution".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("extra_attributes".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("weight".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("family_code".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("style".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("sub_style".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("language_code".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("region_code".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("country_code".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("file_name".equals(name) && size >= 0 && type == REG_TYPE_STR) {
	    			// OK
	    		} else if ("font_name".equals(name) && size >= 0 && type == REG_TYPE_STR) {
	    			// OK
	    		} else if ("expire_date".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else if ("shadow_option".equals(name) && size >= 4 && type == REG_TYPE_INT) {
	    			// OK
	    		} else {
	    			log.warn(String.format("Unknown font registry entry '%s' size=0x%X, type=%d", name, size, type));
	    		}
			}
    	} else {
			log.warn(String.format("Unknown registry entry '%s/%s'", categoryHandle.getFullName(), name));
    	}

    	return 0;
    }

    @HLEFunction(nid = 0x17768E14, version = 150)
    public int sceRegSetKeyValue(int hd, String name, TPointer buf, int size) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("buf: %s", Utilities.getMemoryDump(buf.getAddress(), size)));
    	}

    	String fullName = categoryHandle.getFullName();
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceRegSetKeyValue fullName='%s/%s'", fullName, name));
    	}
    	fullName = fullName.replace("flash1:/registry/system", "");
    	fullName = fullName.replace("flash1/registry/system", "");
    	fullName = fullName.replace("flash2/registry/system", "");

    	Settings settings = Settings.getInstance();
    	if ("/system/DATA/FONT".equals(fullName)) {
    		if ("path_name".equals(name)) {
    			String fontDirPath = buf.getStringNZ(size);
    			if (log.isInfoEnabled()) {
    				log.info(String.format("Setting font dir path to '%s'", fontDirPath));
    			}
    			Modules.sceFontModule.setFontDirPath(fontDirPath);
    		} else if ("num_fonts".equals(name) && size >= 4) {
        		List<sceFont.FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
    			int numFonts = buf.getValue32();
    			if (numFonts != fontRegistry.size()) {
	    			if (log.isInfoEnabled()) {
	    				log.info(String.format("Changing the number of fonts from %d to %d", fontRegistry.size(), numFonts));
	    			}
    			}
    		} else {
    			log.warn(String.format("Unknown font registry entry '%s'", name));
    		}
    	} else if (fullName.startsWith("/system/DATA/FONT/PROPERTY/INFO")) {
    		List<sceFont.FontRegistryEntry> fontRegistry = Modules.sceFontModule.getFontRegistry();
    		int index = Integer.parseInt(fullName.substring(31));
    		if (index < 0 || index >= fontRegistry.size()) {
    			return -1;
    		}
    		FontRegistryEntry entry = fontRegistry.get(index);
    		if ("h_size".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("v_size".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("h_resolution".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("v_resolution".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("extra_attributes".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("weight".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("family_code".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("style".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("sub_style".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("language_code".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("region_code".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("country_code".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("file_name".equals(name)) {
    			entry.file_name = buf.getStringNZ(size);
    		} else if ("font_name".equals(name)) {
    			entry.font_name = buf.getStringNZ(size);
    		} else if ("expire_date".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else if ("shadow_option".equals(name) && size >= 4) {
    			entry.h_size = buf.getValue32();
    		} else {
    			log.warn(String.format("Unknown font registry entry '%s'", name));
    		}
    	} else if ("/DATA/COUNT".equals(fullName)) {
    		if ("wifi_connect_count".equals(name) && size >= 4) {
    			wifiConnectCount = buf.getValue32();
    		} else if ("usb_connect_count".equals(name) && size >= 4) {
    			usbConnectCount = buf.getValue32();
    		} else if ("psn_access_count".equals(name) && size >= 4) {
    			psnAccountCount = buf.getValue32();
    		} else if ("slide_count".equals(name) && size >= 4) {
    			slideCount = buf.getValue32();
    		} else if ("boot_count".equals(name) && size >= 4) {
    			bootCount = buf.getValue32();
    		} else if ("game_exec_count".equals(name) && size >= 4) {
    			gameExecCount = buf.getValue32();
    		} else {
    			log.warn(String.format("Unknown registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/OSK".equals(fullName)) {
    		if ("version_id".equals(name) && size >= 4) {
    			oskVersionId = buf.getValue32();
    		} else if (name.equals("disp_locale") && size >= 4) {
    			oskDispLocale = buf.getValue32();
    		} else if (name.equals("writing_locale") && size >= 4) {
    			oskWritingLocale = buf.getValue32();
    		} else if (name.equals("input_char_mask") && size >= 4) {
    			oskInputCharMask = buf.getValue32();
    		} else if (name.equals("keytop_index") && size >= 4) {
    			oskKeytopIndex = buf.getValue32();
    		} else {
    			log.warn(String.format("Unknown registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/NP".equals(fullName)) {
    		if ("env".equals(name)) {
    			npEnv = buf.getStringNZ(size);
    		} else if ("account_id".equals(name)) {
    			npAccountId = buf.getStringNZ(size);
    			settings.writeString("registry.npAccountId", npAccountId);
    		} else if ("login_id".equals(name)) {
    			npLoginId = buf.getStringNZ(size);
    			settings.writeString("registry.npLoginId", npLoginId);
    		} else if ("password".equals(name)) {
    			npPassword = buf.getStringNZ(size);
    			settings.writeString("registry.npPassword", npPassword);
    		} else if ("auto_sign_in_enable".equals(name) && size >= 4) {
    			npAutoSignInEnable = buf.getValue32();
    			settings.writeInt("registry.npAutoSignInEnable", npAutoSignInEnable);
    		} else {
    			log.warn(String.format("Unknown registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/NETWORK/INFRASTRUCTURE".equals(fullName)) {
    		if ("latest_id".equals(name) && size >= 4) {
    			networkLatestId = buf.getValue32();
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if (fullName.matches("/CONFIG/NETWORK/INFRASTRUCTURE/\\d+")) {
    		String indexName = fullName.replace("/CONFIG/NETWORK/INFRASTRUCTURE/", "");
    		int index = Integer.parseInt(indexName);
            if ("cnf_name".equals(name)) {
            	String cnfName = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set cnf_name#%d='%s'", index, cnfName));
            	}
    		} else if ("ssid".equals(name)) {
            	String ssid = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set ssid#%d='%s'", index, ssid));
            	}
    		} else if ("auth_proto".equals(name) && size >= 4) {
            	int authProto = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set auth_proto#%d='%s'", index, authProto));
            	}
    		} else if ("wep_key".equals(name)) {
            	String wepKey = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wep_key#%d='%s'", index, wepKey));
            	}
    		} else if ("how_to_set_ip".equals(name) && size >= 4) {
            	int howToSetIp = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set how_to_set_ip#%d=%d", index, howToSetIp));
            	}
    		} else if ("ip_address".equals(name)) {
            	String ipAddress = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set ip_address#%d='%s'", index, ipAddress));
            	}
    		} else if ("netmask".equals(name)) {
            	String netmask = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set netmask#%d='%s'", index, netmask));
            	}
    		} else if ("default_route".equals(name)) {
            	String defaultRoute = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set default_route#%d='%s'", index, defaultRoute));
            	}
    		} else if ("dns_flag".equals(name) && size >= 4) {
            	int dnsFlag = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set dns_flag#%d=%d", index, dnsFlag));
            	}
    		} else if ("primary_dns".equals(name)) {
            	String primaryDns = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set primary_dns#%d='%s'", index, primaryDns));
            	}
    		} else if ("secondary_dns".equals(name)) {
            	String secondaryDns = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set secondary_dns#%d='%s'", index, secondaryDns));
            	}
    		} else if ("auth_name".equals(name)) {
            	String authName = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set auth_name#%d='%s'", index, authName));
            	}
    		} else if ("auth_key".equals(name)) {
            	String authKey = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set auth_key#%d='%s'", index, authKey));
            	}
    		} else if ("http_proxy_flag".equals(name) && size >= 4) {
            	int httpProxyFlag = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set http_proxy_flag#%d=%d", index, httpProxyFlag));
            	}
    		} else if ("http_proxy_server".equals(name)) {
            	String httpProxyServer = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set http_proxy_server#%d='%s'", index, httpProxyServer));
            	}
    		} else if ("http_proxy_port".equals(name) && size >= 4) {
            	int httpProxyPort = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set http_proxy_port#%d=%d", index, httpProxyPort));
            	}
    		} else if ("version".equals(name) && size >= 4) {
            	int version = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set version#%d=%d", index, version));
            	}
    		} else if ("device".equals(name) && size >= 4) {
            	int device = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set device#%d=%d", index, device));
            	}
    		} else if ("auth_8021x_type".equals(name) && size >= 4) {
            	int authType = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set auth_8021x_type#%d=%d", index, authType));
            	}
    		} else if ("auth_8021x_auth_name".equals(name)) {
            	String authName = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set auth_8021x_auth_name#%d='%s'", index, authName));
            	}
    		} else if ("auth_8021x_auth_key".equals(name)) {
            	String authKey = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set auth_8021x_auth_key#%d='%s'", index, authKey));
            	}
    		} else if ("wpa_key_type".equals(name) && size >= 4) {
            	int wpaKeyType = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wpa_key_type#%d=%d", index, wpaKeyType));
            	}
    		} else if ("wpa_key".equals(name)) {
            	String wpaKey = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wpa_key#%d='%s'", index, wpaKey));
            	}
    		} else if ("browser_flag".equals(name) && size >= 4) {
            	int browserFlag = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set browser_flag#%d=%d", index, browserFlag));
            	}
    		} else if ("wifisvc_config".equals(name) && size >= 4) {
            	int wifisvcConfig = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wifisvc_config#%d=%d", index, wifisvcConfig));
            	}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if (fullName.matches("/CONFIG/NETWORK/INFRASTRUCTURE/\\d+/SUB1")) {
    		String indexName = fullName.replace("/CONFIG/NETWORK/INFRASTRUCTURE/", "");
    		int index = Integer.parseInt(indexName.substring(0, indexName.indexOf("/")));
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("/CONFIG/NETWORK/INFRASTRUCTURE, index=%d, SUB1", index));
    		}
    		if ("wifisvc_auth_name".equals(name)) {
            	String authName = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wifisvc_auth_name#%d='%s'", index, authName));
            	}
    		} else if ("wifisvc_auth_key".equals(name)) {
            	String authKey = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wifisvc_auth_key#%d='%s'", index, authKey));
            	}
    		} else if ("wifisvc_option".equals(name)) {
            	int wifisvcOption = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set wifisvc_option#%d=%d", index, wifisvcOption));
            	}
    		} else if ("last_leased_dhcp_addr".equals(name)) {
            	String lastLeasedDhcpAddr = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set last_leased_dhcp_addr#%d='%s'", index, lastLeasedDhcpAddr));
            	}
    		} else if ("bt_id".equals(name)) {
            	int btId = buf.getValue32();
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set bt_id#%d=%d", index, btId));
            	}
    		} else if ("at_command".equals(name)) {
            	String atCommand = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set at_command#%d='%s'", index, atCommand));
            	}
    		} else if ("phone_number".equals(name)) {
            	String phoneNumber = buf.getStringNZ(size);
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("set phone_number#%d='%s'", index, phoneNumber));
            	}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/NETWORK/ADHOC".equals(fullName)) {
    		if ("ssid_prefix".equals(name)) {
    			adhocSsidPrefix = buf.getStringNZ(size);
    		} else {
    			log.warn(String.format("Unknown registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/SYSTEM".equals(fullName)) {
    		if ("owner_name".equals(name)) {
    			ownerName = buf.getStringNZ(size);
    		}
    	} else if ("/CONFIG/SYSTEM/XMB/THEME".equals(fullName)) {
    		if ("custom_theme_mode".equals(name) && size >= 4) {
    			settings.writeInt("registry.theme.custom_theme_mode", buf.getValue32());
    		} else if ("color_mode".equals(name) && size >= 4) {
    			settings.writeInt("registry.theme.color_mode", buf.getValue32());
    		} else if ("wallpaper_mode".equals(name) && size >= 4) {
    			settings.writeInt("registry.theme.wallpaper_mode", buf.getValue32());
    		} else if ("system_color".equals(name) && size >= 4) {
    			settings.writeInt("registry.theme.system_color", buf.getValue32());
    		} else {
    			log.warn(String.format("Unknown registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/MUSIC".equals(fullName)) {
    		if ("visualizer_mode".equals(name) && size >= 4) {
    			musicVisualizerMode = buf.getValue32();
    		} else if (name.equals("track_info_mode") && size >= 4) {
    			musicTrackInfoMode = buf.getValue32();
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/CAMERA".equals(fullName)) {
    		if ("msid".equals(name) && size >= 0) {
    			String msid = buf.getStringNZ(16);
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceRegSetKeyValue msid='%s'", msid));
    			}
    		} else if (name.equals("file_folder") && size >= 4) {
    			int fileFolder = buf.getValue32();
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceRegSetKeyValue fileFolder=0x%X", fileFolder));
    			}
    		} else if (name.equals("file_number") && size >= 4) {
    			int fileNumber = buf.getValue32();
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceRegSetKeyValue fileNumber=0x%X", fileNumber));
    			}
    		} else if (name.equals("movie_quality") && size >= 4) {
    			int movieQuality = buf.getValue32();
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceRegSetKeyValue movieQuality=0x%X", movieQuality));
    			}
    		} else if (name.equals("movie_size") && size >= 4) {
    			int movieSize = buf.getValue32();
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceRegSetKeyValue movieSize=0x%X", movieSize));
    			}
    		} else if (name.equals("movie_fps") && size >= 4) {
    			int movieFps = buf.getValue32();
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("sceRegSetKeyValue movieFps=0x%X", movieFps));
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/DATE".equals(fullName)) {
    		if ("date_format".equals(name)) {
    			settings.writeInt("registry.date_format", buf.getValue32());
    		} else if ("time_format".equals(name)) {
    			settings.writeInt("registry.time_format", buf.getValue32());
    		} else if ("time_zone_offset".equals(name)) {
    			settings.writeInt("registry.time_zone_offset", buf.getValue32());
    		} else if ("time_zone_area".equals(name)) {
    			settings.writeString("registry.time_zone_area", buf.getStringZ());
    		} else if ("summer_time".equals(name)) {
    			settings.writeInt("registry.summer_time", buf.getValue32());
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/XMB".equals(fullName)) {
    		if ("language".equals(name)) {
    			settings.writeInt(sceUtility.SYSTEMPARAM_SETTINGS_OPTION_LANGUAGE, buf.getValue32());
    		} else if ("button_assign".equals(name)) {
    			settings.writeInt(sceUtility.SYSTEMPARAM_SETTINGS_OPTION_BUTTON_PREFERENCE, buf.getValue32());
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else {
			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    	}

    	return 0;
    }

    @HLEFunction(nid = 0xD4475AA8, version = 150)
    public int sceRegGetKeyInfo(int hd, String name, TPointer32 hk, @BufferInfo(usage=Usage.out) TPointer32 ptype, @BufferInfo(usage=Usage.out) TPointer32 psize) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	KeyHandle keyHandle = new KeyHandle(name);
    	keyHandles.put(keyHandle.uid, keyHandle);

    	hk.setValue(keyHandle.uid);

    	return getKey(categoryHandle, name, ptype, psize, TPointer.NULL, 0);
    }

    @HLEFunction(nid = 0x28A8E98A, version = 150)
    public int sceRegGetKeyValue(int hd, int hk, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer buf, int size) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	KeyHandle keyHandle = keyHandles.get(hk);
    	if (keyHandle == null) {
    		return -1;
    	}

    	return getKey(categoryHandle, keyHandle.name, TPointer32.NULL, TPointer32.NULL, buf, size);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2C0DB9DD, version = 150)
    public int sceRegGetKeysNum(int hd, int num) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2D211135, version = 150)
    public int sceRegGetKeys(int hd, TPointer buf, int num) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4CA16893, version = 150)
    public int sceRegRemoveCategory(int h, String name) {
    	return 0;
    }

    @HLEFunction(nid = 0xC5768D02, version = 150)
    public int sceRegGetKeyInfoByName(int hd, String name, @BufferInfo(usage=Usage.out) TPointer32 ptype, @BufferInfo(usage=Usage.out) TPointer32 psize) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceRegGetKeyInfoByName fullName='%s/%s'", categoryHandle.getFullName(), name));
    	}

    	return getKey(categoryHandle, name, ptype, psize, TPointer.NULL, 0);
    }

    @HLEFunction(nid = 0x30BE0259, version = 150)
    public int sceRegGetKeyValueByName(int hd, String name, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer buf, int size) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceRegGetKeyValueByName fullName='%s/%s'", categoryHandle.getFullName(), name));
    	}

    	return getKey(categoryHandle, name, TPointer32.NULL, TPointer32.NULL, buf, size);
    }

    @HLEFunction(nid = 0xDBA46704, version = 150)
    public int sceRegOpenRegistry_660(TPointer reg, int mode, @BufferInfo(usage=Usage.out) TPointer32 h) {
    	return sceRegOpenRegistry(reg, mode, h);
    }

    @HLEFunction(nid = 0x4F471457, version = 150)
    public int sceRegOpenCategory_660(int h, String name, int mode, @BufferInfo(usage=Usage.out) TPointer32 hd) {
    	return sceRegOpenCategory(h, name, mode, hd);
    }

    @HLEFunction(nid = 0x9980519F, version = 150)
    public int sceRegGetKeyInfo_660(int hd, String name, TPointer32 hk, @BufferInfo(usage=Usage.out) TPointer32 ptype, @BufferInfo(usage=Usage.out) TPointer32 psize) {
    	return sceRegGetKeyInfo(hd, name, hk, ptype, psize);
    }

    @HLEFunction(nid = 0xF2619407, version = 150)
    public int sceRegGetKeyInfoByName_660(int hd, String name, @BufferInfo(usage=Usage.out) TPointer32 ptype, @BufferInfo(usage=Usage.out) TPointer32 psize) {
    	return sceRegGetKeyInfoByName(hd, name, ptype, psize);
    }

    @HLEFunction(nid = 0xF4A3E396, version = 150)
    public int sceRegGetKeyValue_660(int hd, int hk, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer buf, int size) {
    	return sceRegGetKeyValue(hd, hk, buf, size);
    }

    @HLEFunction(nid = 0x38415B9F, version = 150)
    public int sceRegGetKeyValueByName_660(int hd, String name, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer buf, int size) {
    	return sceRegGetKeyValueByName(hd, name, buf, size);
    }

    @HLEFunction(nid = 0x3B6CA1E6, version = 150)
    public int sceRegCreateKey_660(int hd, String name, int type, int size) {
    	return sceRegCreateKey(hd, name, type, size);
    }

    @HLEFunction(nid = 0x49C70163, version = 150)
    public int sceRegSetKeyValue_660(int hd, String name, TPointer buf, int size) {
    	return sceRegSetKeyValue(hd, name, buf, size);
    }

    @HLEFunction(nid = 0x5FD4764A, version = 150)
    public int sceRegFlushRegistry_660(int h) {
    	return sceRegFlushRegistry(h);
    }

    @HLEFunction(nid = 0xFC742751, version = 150)
    public int sceRegCloseCategory_660(int hd) {
    	return sceRegCloseCategory(hd);
    }

    @HLEFunction(nid = 0x49D77D65, version = 150)
    public int sceRegCloseRegistry_660(int h) {
    	return sceRegCloseRegistry(h);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x61DB9D06, version = 150)
    public int sceRegRemoveCategory_660(int h, String name) {
    	return sceRegRemoveCategory(h, name);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD743A608, version = 150)
    public int sceRegFlushCategory_660(int hd) {
    	return sceRegFlushCategory(hd);
    }
}
