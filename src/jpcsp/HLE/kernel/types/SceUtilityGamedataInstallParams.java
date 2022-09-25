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

import jpcsp.HLE.TPointer;

public class SceUtilityGamedataInstallParams extends pspUtilityBaseDialog {
	public static final int PSP_UTILITY_GAMEDATA_MODE_SHOW_PROGRESS = 1;
    public int mode;                               // 0 for silent mode, 1 to show progress UI
    public String gameName;
    public String dataName;
    public String gamedataParamsGameTitle;
    public String gamedataParamsDataTitle;
    public String gamedataParamsData;
    public int parentalLevel;
    public int progress;                           // Progress in percent: [0..100]
    public long remainingSize;                     // When in progress, remaining size (in bytes) still to be copied
    public long memoryStickMissingFreeSpace;       // Additional memory stick space (in bytes) that would be required to be able to install the gamedata
    public String memoryStickMissingFreeSpaceText; // Additional memory stick space (in text form) that would be required to be able to install the gamedata
    public long memoryStickFreeSpace;              // Memory stick free space (in bytes)
    public String memoryStickFreeSpaceText;        // Memory stick free space (in text form)

    @Override
    protected void read() {
        base = new pspUtilityDialogCommon();
        read(base);
        setMaxSize(base.totalSizeof());

        mode = read32();
        gameName = readStringNZ(13);
        readUnknown(3);
        dataName = readStringNZ(20);
        gamedataParamsGameTitle = readStringNZ(128);
        gamedataParamsDataTitle = readStringNZ(128);
        gamedataParamsData = readStringNZ(1024);
        parentalLevel = read32();
        progress = read32();
        remainingSize = read64();
        memoryStickMissingFreeSpace = read64();
        memoryStickMissingFreeSpaceText = readStringNZ(8);
        memoryStickFreeSpace = read64();
        memoryStickFreeSpaceText = readStringNZ(8);
        readUnknown(16);
    }

    @Override
    protected void write() {
        write(base);
        setMaxSize(base.totalSizeof());

        write32(mode);
        writeStringNZ(13, gameName);
        writeUnknown(3);
        writeStringNZ(20, dataName);
        writeStringNZ(128, gamedataParamsGameTitle);
        writeStringNZ(128, gamedataParamsDataTitle);
        writeStringNZ(1024, gamedataParamsData);
        write32(parentalLevel);
        write32(progress);
        write64(remainingSize);
        write64(memoryStickMissingFreeSpace);
        writeStringNZ(8, memoryStickMissingFreeSpaceText);
        write64(memoryStickFreeSpace);
        writeStringNZ(8, memoryStickFreeSpaceText);
        writeUnknown(16);
    }

    public void writeProgress(TPointer baseAddress) {
    	baseAddress.setValue32(1372, progress);
    	baseAddress.setValue64(1376, remainingSize);
    }

    @Override
    public int sizeof() {
        return base.totalSizeof();
    }

    @Override
    public String toString() {
    	return String.format("mode=0x%08X, progress=%d, gameName='%s', dataName='%s', gameTitle='%s', dataTitle='%s', data='%s', parentalLevel=%d", mode, progress, gameName, dataName, gamedataParamsGameTitle, gamedataParamsDataTitle, gamedataParamsData, parentalLevel);
    }
}