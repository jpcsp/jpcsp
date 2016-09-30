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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.modules.sceFont.FontRegistryEntry;
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
    private int oskVersionId;
    private int oskDispLocale;
    private int oskWritingLocale;
    private int oskInputCharMask;
    private int oskKeytopIndex;
    private String npEnv;
    private String adhocSsidPrefix;
    private int themeCustomThemeCode;
    private int themeColorMode;
    private int themeWallpaperMode;
    private int themeSystemColor;

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

    private int getKey(CategoryHandle categoryHandle, String name, TPointer32 ptype, TPointer32 psize, TPointer buf, int size) {
    	String fullName = categoryHandle.getFullName();
    	fullName = fullName.replace("flash1:/registry/system", "");

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
    				buf.setValue32(2);
    			}
    		} else if ("time_format".equals(name)) {
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
    			if (size >= 16) {
    				buf.clear(16);
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
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/PREMO".equals(fullName)) {
    		if ("ps3_mac".equals(name)) {
    			ptype.setValue(REG_TYPE_BIN);
    			psize.setValue(6);
    			if (size >= 6) {
    				buf.clear(6);
    			}
    		} else if ("ps3_name".equals(name)) {
    			ptype.setValue(REG_TYPE_STR);
    			psize.setValue(1);
    			if (size >= 1) {
        			buf.clear(1);
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
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/XMB".equals(fullName)) {
    		if ("language".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(1);
    			}
    		} else if ("button_assign".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
        			buf.setValue32(1);
    			}
    		} else {
    			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    		}
    	} else if ("/CONFIG/SYSTEM/XMB/THEME".equals(fullName)) {
    		if ("wallpaper_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(themeWallpaperMode);
    			}
    		} else if ("custom_theme_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(themeCustomThemeCode);
    			}
    		} else if ("color_mode".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(themeColorMode);
    			}
    		} else if ("system_color".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(themeSystemColor);
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
    	} else if ("/DATA/COUNT".equals(fullName)) {
    		if ("wifi_connect_count".equals(name)) {
    			ptype.setValue(REG_TYPE_INT);
    			psize.setValue(4);
    			if (size >= 4) {
    				buf.setValue32(wifiConnectCount);
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
		authName = "";
		authKey = "";
		networkLatestId = 0;
		wifiConnectCount = 0;
	    oskVersionId = 0x226;
	    oskDispLocale = 0x1;
	    oskWritingLocale = 0x1;
	    oskInputCharMask = 0xF;
	    oskKeytopIndex = 0x5;
	    npEnv = "np"; // Max length 8
	    adhocSsidPrefix = "XXX"; // Must be of length 3
	    themeWallpaperMode = 0;
	    themeColorMode = 0;
	    themeCustomThemeCode = 0;
	    themeSystemColor = 0;

		super.start();
	}

    @HLEFunction(nid = 0x92E41280, version = 150)
    public int sceRegOpenRegistry(TPointer reg, int mode, TPointer32 h) {
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
    public int sceRegOpenCategory(int h, String name, int mode, TPointer32 hd) {
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
    				return -1;
    			}
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
    		} else {
    			log.warn(String.format("Unknown font registry entry '%s'", name));
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
    			log.warn(String.format("Unknown font registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/NP".equals(fullName)) {
    		if ("env".equals(name)) {
    			npEnv = buf.getStringNZ(size);
    		} else {
    			log.warn(String.format("Unknown font registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/NETWORK/ADHOC".equals(fullName)) {
    		if ("ssid_prefix".equals(name)) {
    			adhocSsidPrefix = buf.getStringNZ(size);
    		} else {
    			log.warn(String.format("Unknown font registry entry '%s'", name));
    		}
    	} else if ("/CONFIG/SYSTEM/XMB/THEME".equals(fullName)) {
    		if ("custom_theme_mode".equals(name)) {
    			themeCustomThemeCode = buf.getValue32();
    		} else if ("color_mode".equals(name)) {
    			themeColorMode = buf.getValue32();
    		} else if ("wallpaper_mode".equals(name)) {
    			themeWallpaperMode = buf.getValue32();
    		} else if ("system_color".equals(name)) {
    			themeSystemColor = buf.getValue32();
    		} else {
    			log.warn(String.format("Unknown font registry entry '%s'", name));
    		}
    	} else {
			log.warn(String.format("Unknown registry entry '%s/%s'", fullName, name));
    	}

    	return 0;
    }

    @HLEFunction(nid = 0xD4475AA8, version = 150)
    public int sceRegGetKeyInfo(int hd, String name, TPointer32 hk, TPointer32 ptype, TPointer32 psize) {
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
    public int sceRegGetKeyValue(int hd, int hk, TPointer buf, int size) {
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
    public int sceRegGetKeyInfoByName(int hd, String name, TPointer32 ptype, TPointer32 psize) {
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
    public int sceRegGetKeyValueByName(int hd, String name, TPointer buf, int size) {
    	CategoryHandle categoryHandle = categoryHandles.get(hd);
    	if (categoryHandle == null) {
    		return -1;
    	}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceRegGetKeyValueByName fullName='%s/%s'", categoryHandle.getFullName(), name));
    	}

    	return getKey(categoryHandle, name, TPointer32.NULL, TPointer32.NULL, buf, size);
    }
}
