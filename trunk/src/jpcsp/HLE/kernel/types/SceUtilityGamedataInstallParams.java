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

public class SceUtilityGamedataInstallParams extends pspAbstractMemoryMappedStructure {
    public pspUtilityDialogCommon base;
    public int unk1;
    public String gameName;
    public String dataName;
    public String gamedataParamsGameTitle;
    public String gamedataParamsDataTitle;
    public String gamedataParamsData;
    public int unk2;
    public int unkResult1;
    public int unkResult2;

    @Override
    protected void read() {
        base = new pspUtilityDialogCommon();
        read(base);
        setMaxSize(base.totalSizeof());

        unk1 = read32();
        gameName = readStringNZ(13);
        readUnknown(3);
        dataName = readStringNZ(20);
        gamedataParamsGameTitle = readStringNZ(128);
        gamedataParamsDataTitle = readStringNZ(128);
        gamedataParamsData = readStringNZ(1024);
        unk2 = read8();
        readUnknown(7);
        unkResult1 = read32();
        unkResult2 = read32();
        readUnknown(48);
    }

    @Override
    protected void write() {
        write(base);
        setMaxSize(base.totalSizeof());

        write32(unk1);
        writeStringNZ(13, gameName);
        writeUnknown(3);
        writeStringNZ(20, dataName);
        writeStringNZ(128, gamedataParamsGameTitle);
        writeStringNZ(128, gamedataParamsDataTitle);
        writeStringNZ(1024, gamedataParamsData);
        write8((byte) unk2);
        writeUnknown(7);
        write32(unkResult1);
        write32(unkResult2);
        writeUnknown(48);
    }

    @Override
    public int sizeof() {
        return base.totalSizeof();
    }

    @Override
    public String toString() {
    	return String.format("unk1=0x%08X, gameName='%s', dataName='%s', gameTitle='%s', dataTitle='%s', data='%s', unk2=0x%02X", unk1, gameName, dataName, gamedataParamsGameTitle, gamedataParamsDataTitle, gamedataParamsData, unk2);
    }
}