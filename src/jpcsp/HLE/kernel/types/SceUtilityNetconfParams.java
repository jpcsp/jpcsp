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
package jpcsp.HLE.kernel.types;

import static jpcsp.HLE.modules150.sceNetAdhocctl.GROUP_NAME_LENGTH;

public class SceUtilityNetconfParams extends pspAbstractMemoryMappedStructure {
    public pspUtilityDialogCommon base;
    public int netAction;           // The netconf action (PSPSDK): sets how to connect.
        public final static int PSP_UTILITY_NETCONF_CONNECT_APNET = 0;
        public final static int PSP_UTILITY_NETCONF_GET_STATUS_APNET = 1;
        public final static int PSP_UTILITY_NETCONF_CONNECT_ADHOC = 2;
        public final static int PSP_UTILITY_NETCONF_CONNECT_APNET_LASTUSED = 3;
        public final static int PSP_UTILITY_NETCONF_CREATE_ADHOC = 4;
        public final static int PSP_UTILITY_NETCONF_JOIN_ADHOC = 5;
    public int netconfDataAddr;
    public SceUtilityNetconfData netconfData;
    public int netHotspot;          // Flag to allow hotspot connections (PSPSDK).
    public int netHotspotConnected; // Flag to check if a hotspot connection is active (PSPSDK).
    public int netWifiSp;           // Flag to allow WIFI connections (PSPSDK).

    public static class SceUtilityNetconfData extends pspAbstractMemoryMappedStructure {
        public String groupName;
        public int timeout;

		@Override
		protected void read() {
            groupName = readStringNZ(GROUP_NAME_LENGTH);
            timeout = read32();
		}

		@Override
		protected void write() {
			writeStringNZ(GROUP_NAME_LENGTH, groupName);
            write32(timeout);
		}

		@Override
		public int sizeof() {
			return GROUP_NAME_LENGTH + 4;
		}

		@Override
		public String toString() {
			return String.format("groupName=%s, timeout=%d", groupName, timeout);
		}
	}

    @Override
    protected void read() {
        base = new pspUtilityDialogCommon();
        read(base);
        setMaxSize(base.totalSizeof());

        netAction = read32();
        netconfDataAddr = read32();
        if (netconfDataAddr != 0) {
			netconfData = new SceUtilityNetconfData();
			netconfData.read(mem, netconfDataAddr);
		} else {
			netconfData = null;
		}
        netHotspot = read32();
        netHotspotConnected = read32();
        netWifiSp = read32();
    }

    @Override
    protected void write() {
        write(base);
        setMaxSize(base.totalSizeof());

        write32(netAction);
        write32(netconfDataAddr);
        if (netconfData != null && netconfDataAddr != 0) {
			netconfData.write(mem, netconfDataAddr);
		}
        write32(netHotspot);
        write32(netHotspotConnected);
        write32(netWifiSp);
    }

    @Override
    public int sizeof() {
        return base.totalSizeof();
    }

    @Override
    public String toString() {
        return String.format("SceUtilityNetconf[address=0x%08X, netAction=%d, %s]", getBaseAddress(), netAction, netconfData);
    }
}