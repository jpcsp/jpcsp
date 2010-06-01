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

public class SceUtilityNetconfParams extends pspAbstractMemoryMappedStructure {
    public pspUtilityDialogCommon base;
    public int unknown;
    public int netconfDataAddr;
    public SceUtilityNetconfData netconfData;

    public static class SceUtilityNetconfData extends pspAbstractMemoryMappedStructure {
        public String confTitle;
        public int unk1Addr;
        public int unk2Addr;
        public int unkSize1;
        // Probably has more params.

		@Override
		protected void read() {
            confTitle = readStringNZ(16);  // Seems to represent a net profile name.
            readUnknown(12);
            unk1Addr = read32();           // Points to what seems to be a sceNetAdhoc struct.
            unk2Addr = read32();           // Points to what seems to be a sceNetAdhocctl struct.
            readUnknown(4);
            unkSize1 = read32();           // 256.
		}

		@Override
		protected void write() {
			writeStringNZ(16, confTitle);
            writeUnknown(12);
            write32(unk1Addr);
            write32(unk2Addr);
            writeUnknown(4);
            write32(unkSize1);
		}

		@Override
		public int sizeof() {
			return 7 * 4;
		}
	}

    @Override
    protected void read() {
        base = new pspUtilityDialogCommon();
        read(base);
        setMaxSize(base.size);

        unknown         = read32();
        netconfDataAddr = read32();
        if (netconfDataAddr != 0) {
			netconfData = new SceUtilityNetconfData();
			netconfData.read(mem, netconfDataAddr);
		} else {
			netconfData = null;
		}
        readUnknown(8);
    }

    @Override
    protected void write() {
        setMaxSize(base.size);
        write(base);

        write32(unknown);
        write32(netconfDataAddr);
        if (netconfData != null && netconfDataAddr != 0) {
			netconfData.write(mem, netconfDataAddr);
		}
        writeUnknown(8);
    }

    @Override
    public int sizeof() {
        return base.size;
    }

    @Override
    public String toString() {
        return String.format("title=%s", netconfData.confTitle);
    }
}