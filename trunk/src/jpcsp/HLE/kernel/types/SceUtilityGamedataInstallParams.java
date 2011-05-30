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
    public int unk2;
    public String gamedataParamsGameTitle;
    public String gamedataParamsDataTitle;
    public String gamedataParamsData;

    @Override
    protected void read() {
        base = new pspUtilityDialogCommon();
        read(base);
        setMaxSize(base.size);

        unk1 = read32();
        gameName = readStringNZ(13);
        readUnknown(3);
        dataName = readStringNZ(13);
        readUnknown(3);
        unk2 = read32();
        gamedataParamsGameTitle = readStringNZ(128);
        gamedataParamsDataTitle = readStringNZ(128);
        gamedataParamsData = readStringNZ(1112);
    }

    @Override
    protected void write() {
        setMaxSize(base.size);
        write(base);

        write32(unk1);
        writeStringNZ(13, gameName);
        writeUnknown(3);
        writeStringNZ(13, dataName);
        writeUnknown(3);
        write32(unk2);
        writeStringNZ(128, gamedataParamsGameTitle);
        writeStringNZ(128, gamedataParamsDataTitle);
        writeStringNZ(1112, gamedataParamsData);
    }

    @Override
    public int sizeof() {
        return base.size;
    }

    @Override
    public String toString() {
        // TODO
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }
}