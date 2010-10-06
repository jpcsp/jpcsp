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

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashMap;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.format.PGF;
import jpcsp.util.Debug;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceFont implements HLEModule, HLEStartModule {

    private static Logger log = Modules.getLogger("sceFont");

    @Override
    public String getName() {
        return "sceFont";
    }

    @Override
    public void installModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.addFunction(0x099EF33C, sceFontFindOptimumFontFunction);
            mm.addFunction(0x0DA7535E, sceFontGetFontInfoFunction);
            mm.addFunction(0x3AEA8CB6, sceFontCloseFunction);
            mm.addFunction(0x574B6FBC, sceFontDoneLibFunction);
            mm.addFunction(0x67F17ED7, sceFontNewLibFunction);
            mm.addFunction(0xA834319D, sceFontOpenFunction);
            mm.addFunction(0xCA1E6945, sceFontGetCharGlyphImage_ClipFunction);
            mm.addFunction(0xDCC80C2F, sceFontGetCharInfoFunction);
            mm.addFunction(0x980F4895, sceFontGetCharGlyphImageFunction);
            mm.addFunction(0x27F6E642, sceFontGetNumFontListFunction);
            mm.addFunction(0xBC75D85B, sceFontGetFontListFunction);
            mm.addFunction(0xBB8E7FE6, sceFontOpenUserMemoryFunction);
            mm.addFunction(0xEE232411, sceFontSetAltCharacterCodeFunction);
            mm.addFunction(0x5C3E4A9E, sceFontGetCharImageRectFunction);
            mm.addFunction(0x472694CD, sceFontPointToPixelHFunction);
            mm.addFunction(0x5333322D, sceFontGetFontInfoByIndexNumberFunction);
            mm.addFunction(0x48293280, sceFontSetResolutionFunction);
            mm.addFunction(0x02D7F94B, sceFontFlushFunction);
            mm.addFunction(0x57FCB733, sceFontOpenUserFileFunction);
            mm.addFunction(0x681E61A7, sceFontFindFontFunction);
            mm.addFunction(0x3C4B7E82, sceFontPointToPixelVFunction);
            mm.addFunction(0x74B21701, sceFontPixelToPointHFunction);
            mm.addFunction(0xF8F0752E, sceFontPixelToPointVFunction);

        }
    }

    @Override
    public void uninstallModule(HLEModuleManager mm, int version) {
        if (version >= 150) {

            mm.removeFunction(sceFontFindOptimumFontFunction);
            mm.removeFunction(sceFontGetFontInfoFunction);
            mm.removeFunction(sceFontCloseFunction);
            mm.removeFunction(sceFontDoneLibFunction);
            mm.removeFunction(sceFontNewLibFunction);
            mm.removeFunction(sceFontOpenFunction);
            mm.removeFunction(sceFontGetCharGlyphImage_ClipFunction);
            mm.removeFunction(sceFontGetCharInfoFunction);
            mm.removeFunction(sceFontGetCharGlyphImageFunction);
            mm.removeFunction(sceFontGetNumFontListFunction);
            mm.removeFunction(sceFontGetFontListFunction);
            mm.removeFunction(sceFontOpenUserMemoryFunction);
            mm.removeFunction(sceFontSetAltCharacterCodeFunction);
            mm.removeFunction(sceFontGetCharImageRectFunction);
            mm.removeFunction(sceFontPointToPixelHFunction);
            mm.removeFunction(sceFontGetFontInfoByIndexNumberFunction);
            mm.removeFunction(sceFontSetResolutionFunction);
            mm.removeFunction(sceFontFlushFunction);
            mm.removeFunction(sceFontOpenUserFileFunction);
            mm.removeFunction(sceFontFindFontFunction);
            mm.removeFunction(sceFontPointToPixelVFunction);
            mm.removeFunction(sceFontPixelToPointHFunction);
            mm.removeFunction(sceFontPixelToPointVFunction);

        }
    }

    @Override
    public void start() {
        fontLibMap = new HashMap<Integer, FontLib>();
        PGFFilesMap = new HashMap<Integer, PGF>();
        fontLibCount = 0;
        currentFontHandle = 0;
    }

    @Override
    public void stop() {
    }
    public static final int PGF_MAGIC = 'P' << 24 | 'G' << 16 | 'F' << 8 | '0';
    public static final String fontDirPath = "flash0/font";

    public static final int PSP_FONT_PIXELFORMAT_4 = 0; // 2 pixels packed in 1 byte (natural order)
    public static final int PSP_FONT_PIXELFORMAT_4_REV = 1; // 2 pixels packed in 1 byte (reversed order)
    public static final int PSP_FONT_PIXELFORMAT_8 = 2; // 1 pixel in 1 byte
    public static final int PSP_FONT_PIXELFORMAT_24 = 3; // 1 pixel in 3 bytes (RGB)
    public static final int PSP_FONT_PIXELFORMAT_32 = 4; // 1 pixel in 4 bytes (RGBA)

    public static final int PSP_FONT_MODE_FILE = 0;
    public static final int PSP_FONT_MODE_MEMORY = 1;

    private HashMap<Integer, FontLib> fontLibMap;
    private HashMap<Integer, PGF> PGFFilesMap;

    private int fontLibCount;
    private int currentFontHandle;
    private float globalFontHRes = 0.0f;
    private float globalFontVRes = 0.0f;
    private static char alternateCharacter = '?';

    public int makeFakeLibHandle() {
        return 0xF8F80000 | (fontLibCount++ & 0xFFFF);
    }

    public static char getAlternateChar() {
        return alternateCharacter;
    }

    public static void setAlternateChar(char newChar) {
        alternateCharacter = newChar;
    }

    protected class FontLib {

        private int userDataAddr;
        private int numFonts;
        private int cacheDataAddr;
        private int allocFuncAddr;
        private int freeFuncAddr;
        private int openFuncAddr;
        private int closeFuncAddr;
        private int readFuncAddr;
        private int seekFuncAddr;
        private int errorFuncAddr;
        private int ioFinishFuncAddr;
        private int[] fonts;
        private int fileFontHandleCount;
        private int memFontHandleCount;
        private int memFontAddr;
        private int fileFontHandle;

        public FontLib(int params) {
            read(params);
            fonts = new int[numFonts];
            fileFontHandleCount = 0;
            memFontHandleCount = 0;
            for (int i = 0; i < numFonts; i++) {
                fonts[i] = makeFakeFontHandle(i);
            }
            loadFontFiles();
        }

        public void loadFontFiles() {
            File f = new File(fontDirPath);
            String[] files = f.list();
            int index = 0;
            for (int i = 0; i < files.length; i++) {
                String currentFile = (fontDirPath + "/" + files[i]);
                if (currentFile.endsWith(".pgf") && index < numFonts) {
                    try {
                        RandomAccessFile fontFile = new RandomAccessFile(currentFile, "r");
                        byte[] pgfBuf = new byte[(int) fontFile.length()];
                        fontFile.read(pgfBuf);
                        ByteBuffer finalBuf = ByteBuffer.wrap(pgfBuf);

                        PGF pgfFile = new PGF(finalBuf);
                        pgfFile.setFileNamez(files[i]);
                        PGFFilesMap.put(getFakeFontHandle(index), pgfFile);

                        log.info("Found font file '" + files[i] + "'. Font='" + pgfFile.getFontName() + "' Type='" + pgfFile.getFontType() + "'");
                    } catch (Exception e) {
                        // Can't open file.
                    }
                    index++;
                }
            }
        }

        public int makeFakeFontHandle(int fontNum) {
            return 0xF9F90000 | (fontNum & 0xFFFF);
        }

        public int makeFakeMemFontHandle() {
            return 0xF6F60000 | (memFontHandleCount++ & 0xFFFF);
        }

        public int makeFakeFileFontHandle() {
            return 0xF7F70000 | (fileFontHandleCount++ & 0xFFFF);
        }

        public int getFakeFontHandle(int i) {
            return fonts[i];
        }

        public int getNumFonts() {
            return numFonts;
        }

        private void triggerAllocCallback(int size) {
            Modules.ThreadManForUserModule.executeCallback(null, allocFuncAddr, new AfterAllocCallback(), userDataAddr, size);
        }

        private void triggerFreeCallback() {
            if(Memory.getInstance().isAddressGood(memFontAddr)) {
                Modules.ThreadManForUserModule.executeCallback(null, freeFuncAddr, null, userDataAddr, memFontAddr);
            }
        }

        private void triggerOpenCallback(int fileNameAddr, int errorCodeAddr) {
            Modules.ThreadManForUserModule.executeCallback(null, allocFuncAddr, new AfterOpenCallback(), userDataAddr, fileNameAddr, errorCodeAddr);
        }

        private void triggerCloseCallback() {
            if(fileFontHandle != 0) {
                Modules.ThreadManForUserModule.executeCallback(null, freeFuncAddr, null, userDataAddr, fileFontHandle);
            }
        }

        private void read(int paramsAddr) {
            Memory mem = Memory.getInstance();

            userDataAddr = mem.read32(paramsAddr);
            numFonts = mem.read32(paramsAddr + 4);
            cacheDataAddr = mem.read32(paramsAddr + 8);
            allocFuncAddr = mem.read32(paramsAddr + 12);
            freeFuncAddr = mem.read32(paramsAddr + 16);
            openFuncAddr = mem.read32(paramsAddr + 20);
            closeFuncAddr = mem.read32(paramsAddr + 24);
            readFuncAddr = mem.read32(paramsAddr + 28);
            seekFuncAddr = mem.read32(paramsAddr + 32);
            errorFuncAddr = mem.read32(paramsAddr + 36);
            ioFinishFuncAddr = mem.read32(paramsAddr + 42);
        }

        private class AfterAllocCallback implements IAction {

            @Override
            public void execute() {
                memFontAddr = Emulator.getProcessor().cpu.gpr[2];

                log.info("FontLib's allocation callback returned 0x" + Integer.toHexString(memFontAddr));
            }
        }

        private class AfterOpenCallback implements IAction {

            @Override
            public void execute() {
                fileFontHandle = Emulator.getProcessor().cpu.gpr[2];

                log.info("FontLib's file open callback returned 0x" + Integer.toHexString(fileFontHandle));
            }
        }
    }

    public void sceFontNewLib(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int paramsAddr = cpu.gpr[4];
        int errorCodeAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontNewLib paramsAddr=0x%08X, errorCodeAddr=0x%08X", paramsAddr, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        FontLib fl = null;
        int handle = 0;
        if (mem.isAddressGood(paramsAddr)) {
            fl = new FontLib(paramsAddr);
            handle = makeFakeLibHandle();
            fontLibMap.put(handle, fl);
        }
        if (mem.isAddressGood(errorCodeAddr)) {
            mem.write32(errorCodeAddr, 0);
        }
        cpu.gpr[2] = handle;
    }

    public void sceFontOpenUserFile(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int libHandle = cpu.gpr[4];
        int fileNameAddr = cpu.gpr[5];
        int mode = cpu.gpr[6];
        int errorCodeAddr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontOpenUserFile libHandle=0x%08X, fileNameAddr=0x%08X, mode=0x%08X, errorCodeAddr=0x%08X",
                    libHandle, fileNameAddr, mode, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            FontLib fLib = fontLibMap.get(libHandle);
            if (fLib != null) {
                fLib.triggerOpenCallback(fileNameAddr, errorCodeAddr);
                currentFontHandle = fLib.makeFakeFileFontHandle();
            }
            if (mem.isAddressGood(errorCodeAddr)) {
                mem.write32(errorCodeAddr, 0);
            }
            cpu.gpr[2] = currentFontHandle;
        }
    }

    public void sceFontOpenUserMemory(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int libHandle = cpu.gpr[4];
        int memoryFontAddr = cpu.gpr[5];
        int memoryFontLength = cpu.gpr[6];
        int errorCodeAddr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontOpenUserMemory libHandle=0x%08X, memoryFontAddr=0x%08X, memoryFontLength=%d, errorCodeAddr=0x%08X",
                    libHandle, memoryFontAddr, memoryFontLength, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            FontLib fLib = fontLibMap.get(libHandle);
            if (fLib != null) {
                fLib.triggerAllocCallback(memoryFontLength);
                currentFontHandle = fLib.makeFakeMemFontHandle();
            }
            if (mem.isAddressGood(errorCodeAddr)) {
                mem.write32(errorCodeAddr, 0);
            }
            cpu.gpr[2] = currentFontHandle;
        }
    }

    public void sceFontGetFontInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int fontAddr = cpu.gpr[4];
        int fontInfoAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetFontInfo fontAddr=0x%08X, fontInfoAddr=0x%08X", fontAddr, fontInfoAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mem.isAddressGood(fontInfoAddr)) {
            PGF currentPGF = PGFFilesMap.get(fontAddr);
            if (currentPGF != null) {
                int maxGlyphWidthI = currentPGF.getMaxSize()[0];
                int maxGlyphHeightI = currentPGF.getMaxSize()[1];
                int maxGlyphAscenderI = currentPGF.getMaxBaseYAdjust();
                int maxGlyphDescenderI = (currentPGF.getMaxBaseYAdjust() - currentPGF.getMaxSize()[1]);
                int maxGlyphLeftXI = currentPGF.getMaxLeftXAdjust();
                int maxGlyphBaseYI = currentPGF.getMaxBaseYAdjust();
                int minGlyphCenterXI = currentPGF.getMinCenterXAdjust();
                int maxGlyphTopYI = currentPGF.getMaxTopYAdjust();
                int maxGlyphAdvanceXI = currentPGF.getMaxAdvance()[0];
                int maxGlyphAdvanceYI = currentPGF.getMaxAdvance()[1];
                float maxGlyphWidthF = Float.intBitsToFloat(maxGlyphWidthI);
                float maxGlyphHeightF = Float.intBitsToFloat(maxGlyphHeightI);
                float maxGlyphAscenderF = Float.intBitsToFloat(maxGlyphAscenderI);
                float maxGlyphDescenderF = Float.intBitsToFloat(maxGlyphDescenderI);
                float maxGlyphLeftXF = Float.intBitsToFloat(maxGlyphLeftXI);
                float maxGlyphBaseYF = Float.intBitsToFloat(maxGlyphBaseYI);
                float minGlyphCenterXF = Float.intBitsToFloat(minGlyphCenterXI);
                float maxGlyphTopYF = Float.intBitsToFloat(maxGlyphTopYI);
                float maxGlyphAdvanceXF = Float.intBitsToFloat(maxGlyphAdvanceXI);
                float maxGlyphAdvanceYF = Float.intBitsToFloat(maxGlyphAdvanceYI);

                // Glyph metrics (in 26.6 signed fixed-point).
                mem.write32(fontInfoAddr + 0, maxGlyphWidthI);
                mem.write32(fontInfoAddr + 4, maxGlyphHeightI);
                mem.write32(fontInfoAddr + 8, maxGlyphAscenderI);
                mem.write32(fontInfoAddr + 12, maxGlyphDescenderI);
                mem.write32(fontInfoAddr + 16, maxGlyphLeftXI);
                mem.write32(fontInfoAddr + 20, maxGlyphBaseYI);
                mem.write32(fontInfoAddr + 24, minGlyphCenterXI);
                mem.write32(fontInfoAddr + 28, maxGlyphTopYI);
                mem.write32(fontInfoAddr + 32, maxGlyphAdvanceXI);
                mem.write32(fontInfoAddr + 36, maxGlyphAdvanceYI);
                // Glyph metrics (replicated as float).
                mem.write32(fontInfoAddr + 40, Float.floatToRawIntBits(maxGlyphWidthF));
                mem.write32(fontInfoAddr + 44, Float.floatToRawIntBits(maxGlyphHeightF));
                mem.write32(fontInfoAddr + 48, Float.floatToRawIntBits(maxGlyphAscenderF));
                mem.write32(fontInfoAddr + 52, Float.floatToRawIntBits(maxGlyphDescenderF));
                mem.write32(fontInfoAddr + 56, Float.floatToRawIntBits(maxGlyphLeftXF));
                mem.write32(fontInfoAddr + 60, Float.floatToRawIntBits(maxGlyphBaseYF));
                mem.write32(fontInfoAddr + 64, Float.floatToRawIntBits(minGlyphCenterXF));
                mem.write32(fontInfoAddr + 68, Float.floatToRawIntBits(maxGlyphTopYF));
                mem.write32(fontInfoAddr + 72, Float.floatToRawIntBits(maxGlyphAdvanceXF));
                mem.write32(fontInfoAddr + 76, Float.floatToRawIntBits(maxGlyphAdvanceYF));
                // Bitmap dimensions.
                mem.write16(fontInfoAddr + 80, (short) currentPGF.getMaxGlyphWidth());
                mem.write16(fontInfoAddr + 82, (short) currentPGF.getMaxGlyphHeight());
                mem.write32(fontInfoAddr + 84, currentPGF.getCharMapLenght());     // Number of elements in the font's charmap.
                mem.write32(fontInfoAddr + 88, currentPGF.getShadowMapLenght());   // Number of elements in the font's shadow charmap.
                // Font style (used by font comparison functions).
                mem.write32(fontInfoAddr + 92, Float.floatToRawIntBits(0.0f));            // Horizontal size.
                mem.write32(fontInfoAddr + 96, Float.floatToRawIntBits(0.0f));            // Vertical size.
                mem.write32(fontInfoAddr + 100, Float.floatToRawIntBits(globalFontHRes)); // Horizontal resolution.
                mem.write32(fontInfoAddr + 104, Float.floatToRawIntBits(globalFontVRes)); // Vertical resolution.
                mem.write32(fontInfoAddr + 108, Float.floatToRawIntBits(0.0f));           // Font weight.
                mem.write16(fontInfoAddr + 112, (short) 0);                               // Font family (SYSTEM = 0, probably more).
                mem.write16(fontInfoAddr + 114, (short) 0);                               // Style (SYSTEM = 0, STANDARD = 1, probably more).
                mem.write16(fontInfoAddr + 116, (short) 0);                               // Subset of style (only used in Asian fonts, unknown values).
                mem.write16(fontInfoAddr + 118, (short) 0);                               // Language code (UNK = 0, JAPANESE = 1, ENGLISH = 2, probably more).
                mem.write16(fontInfoAddr + 120, (short) 0);                               // Region code (UNK = 0, JAPAN = 1, probably more).
                mem.write16(fontInfoAddr + 122, (short) 0);                               // Country code (UNK = 0, JAPAN = 1, US = 2, probably more).
                Utilities.writeStringNZ(mem, fontInfoAddr + 124, 64, currentPGF.getFontName());    // Font name (maximum size is 64).
                Utilities.writeStringNZ(mem, fontInfoAddr + 188, 64, currentPGF.getFileNamez());   // File name (maximum size is 64).
                mem.write32(fontInfoAddr + 252, 0); // Additional attributes.
                mem.write32(fontInfoAddr + 256, 0); // Expiration date.
                mem.write8(fontInfoAddr + 260, (byte) 4); // Font's BPP.
                mem.write8(fontInfoAddr + 261, (byte) 0); // Padding.
                mem.write8(fontInfoAddr + 262, (byte) 0); // Padding.
                mem.write8(fontInfoAddr + 263, (byte) 0); // Padding.
            }
        }
        cpu.gpr[2] = 0;
    }

    public void sceFontGetCharInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int fontAddr = cpu.gpr[4];
        int charCode = cpu.gpr[5];
        int charInfoAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetCharInfo fontAddr=0x%08X, charCode=%04X (%c), charInfoAddr=%08X", fontAddr, charCode, (charCode <= 0xFF ? (char) charCode : '?'), charInfoAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mem.isAddressGood(charInfoAddr)) {
            int bitmapWidth = Debug.Font.charWidth * Debug.fontPixelSize;
            int bitmapHeight = Debug.Font.charHeight * Debug.fontPixelSize;
            int bitmapLeft = 0;
            int bitmapTop = 0;

            /*
             * Char's metrics:
             *
             *           Width / Horizontal Advance
             *           <---------->
             *      |           000 |
             *      |           000 |  Ascender
             *      |           000 |
             *      |     000   000 |
             *      | -----000--000-------- Baseline
             *      |        00000  |  Descender
             * Height /
             * Vertical Advance
             *
             * The char's bearings represent the difference between the
             * width and the horizontal advance and/or the difference
             * between the height and the vertical advance.
             * In our debug font, these measures are the same (block pixels),
             * but in real PGF fonts they can vary (italic fonts, for example).
             */
            int sfp26Width = bitmapWidth << 6;
            int sfp26Height = bitmapHeight << 6;
            int sfp26Ascender = 0 << 6;
            int sfp26Descender = 0 << 6;
            int sfp26BearingHX = 0 << 6;
            int sfp26BearingHY = 0 << 6;
            int sfp26BearingVX = 0 << 6;
            int sfp26BearingVY = 0 << 6;
            int sfp26AdvanceH = bitmapWidth << 6;
            int sfp26AdvanceV = bitmapHeight << 6;

            // Bitmap dimensions.
            mem.write32(charInfoAddr + 0, bitmapWidth);	    // bitmapWidth
            mem.write32(charInfoAddr + 4, bitmapHeight);    // bitmapHeight
            mem.write32(charInfoAddr + 8, bitmapLeft);	    // bitmapLeft
            mem.write32(charInfoAddr + 12, bitmapTop);	    // bitmapTop
            // Glyph metrics (in 26.6 signed fixed-point).
            mem.write32(charInfoAddr + 16, sfp26Width);     // Width
            mem.write32(charInfoAddr + 20, sfp26Height);    // Height
            mem.write32(charInfoAddr + 24, sfp26Ascender);  // Ascender
            mem.write32(charInfoAddr + 28, sfp26Descender); // Descender
            mem.write32(charInfoAddr + 32, sfp26BearingHX); // X horizontal bearing
            mem.write32(charInfoAddr + 36, sfp26BearingHY); // Y horizontal bearing
            mem.write32(charInfoAddr + 40, sfp26BearingVX); // X vertical bearing
            mem.write32(charInfoAddr + 44, sfp26BearingVY); // Y vertical bearing
            mem.write32(charInfoAddr + 48, sfp26AdvanceH);  // Horizontal advance
            mem.write32(charInfoAddr + 52, sfp26AdvanceV);  // Vertical advance
            // Padding.
            mem.write8(charInfoAddr + 56, (byte) 0);
            mem.write8(charInfoAddr + 57, (byte) 0);
            mem.write8(charInfoAddr + 58, (byte) 0);
            mem.write8(charInfoAddr + 59, (byte) 0);
        }
        cpu.gpr[2] = 0;
    }

    public void sceFontGetCharGlyphImage(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int fontAddr = cpu.gpr[4];
        int charCode = cpu.gpr[5];
        int glyphImageAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetCharGlyphImage fontAddr=0x%08X, charCode=%04X (%c), glyphImageAddr=%08X", fontAddr, charCode, (charCode <= 0xFF ? (char) charCode : '?'), glyphImageAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (mem.isAddressGood(glyphImageAddr)) {
            // Read GlyphImage data.
            int pixelFormat = mem.read32(glyphImageAddr + 0);
            int xPos64 = mem.read32(glyphImageAddr + 4);
            int yPos64 = mem.read32(glyphImageAddr + 8);
            int bufWidth = mem.read16(glyphImageAddr + 12);
            int bufHeight = mem.read16(glyphImageAddr + 14);
            int bytesPerLine = mem.read16(glyphImageAddr + 16);
            int buffer = mem.read32(glyphImageAddr + 20);
            // 26.6 fixed-point.
            int xPosI = xPos64 >> 6;
            int yPosI = yPos64 >> 6;

            if (log.isDebugEnabled()) {
                log.debug("sceFontGetCharGlyphImage c=" + ((char) charCode) + ", xPos=" + xPosI + ", yPos=" + yPosI + ", buffer=0x" + Integer.toHexString(buffer) + ", bufWidth=" + bufWidth + ", bufHeight=" + bufHeight + ", bytesPerLine=" + bytesPerLine + ", pixelFormat=" + pixelFormat);
            }

            PGF currentPGF = PGFFilesMap.get(fontAddr);
            if (currentPGF != null) {
                // Font adjustment.
                // TODO: Instead of using the loaded PGF, figure out
                // the proper values for the Debug font.
                yPosI -= (currentPGF.getMaxBaseYAdjust() >> 6);
                yPosI += (currentPGF.getMaxTopYAdjust() >> 6);
            }
            // Use our Debug font.
            Debug.printFontbuffer(buffer, bytesPerLine, bufWidth, bufHeight,
                    xPosI, yPosI, pixelFormat, (char) charCode);
        }
        cpu.gpr[2] = 0;
    }

    public void sceFontFindOptimumFont(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int libHandle = cpu.gpr[4];
        int fontStyleAddr = cpu.gpr[5];
        int errorCodeAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontFindOptimumFont libHandle=0x%08X, fontStyleAddr=0x%08X, errorCodeAddr=0x%08X", libHandle, fontStyleAddr, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Font style parameters.
        int fontH = mem.read32(fontStyleAddr);
        int fontV = mem.read32(fontStyleAddr + 4);
        int fontHRes = mem.read32(fontStyleAddr + 8);
        int fontVRes = mem.read32(fontStyleAddr + 12);
        int fontWeight = mem.read32(fontStyleAddr + 16);
        int fontFamily = mem.read16(fontStyleAddr + 20);
        int fontStyle = mem.read16(fontStyleAddr + 22);
        int fontStyleSub = mem.read16(fontStyleAddr + 24);
        int fontLanguage = mem.read16(fontStyleAddr + 26);
        int fontRegion = mem.read16(fontStyleAddr + 28);
        int fontCountry = mem.read16(fontStyleAddr + 30);
        String fontName = Utilities.readStringNZ(fontStyleAddr + 32, 64);
        String fontFileName = Utilities.readStringNZ(fontStyleAddr + 96, 64);
        int fontAttributes = mem.read32(fontStyleAddr + 160);
        int fontExpire = mem.read32(fontStyleAddr + 164);

        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            // Return the first index.
            int optimumFontIndex = 0;
            if (mem.isAddressGood(errorCodeAddr)) {
                mem.write32(errorCodeAddr, 0);
            }
            cpu.gpr[2] = optimumFontIndex;
        }
    }

    public void sceFontClose(Processor processor) {
        CpuState cpu = processor.cpu;

        int fontAddr = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontClose fontAddr=0x%08X", fontAddr));
        }
        cpu.gpr[2] = 0;
    }

    public void sceFontDoneLib(Processor processor) {
        CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontDoneLib libHandle=0x%08X", libHandle));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            // Free all reserved font lib space and close all open font files.
            fontLibMap.get(libHandle).triggerFreeCallback();
            fontLibMap.get(libHandle).triggerCloseCallback();
            cpu.gpr[2] = 0;
        }
    }

    public void sceFontOpen(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int libHandle = cpu.gpr[4];
        int index = cpu.gpr[5];
        int mode = cpu.gpr[6];
        int errorCodeAddr = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontOpen libHandle=0x%08X, index=%d, mode=%d, errorCodeAddr=0x%08X", libHandle, index, mode, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        FontLib fLib = fontLibMap.get(libHandle);
        if (fLib != null) {
            currentFontHandle = fLib.getFakeFontHandle(index);
        }
        PGF currentPGF = PGFFilesMap.get(currentFontHandle);
        if (currentPGF != null) {
            log.info("Opening '" + currentPGF.getFontName() + "' - '" + currentPGF.getFontType() + "'");
        }
        if (mem.isAddressGood(errorCodeAddr)) {
            mem.write32(errorCodeAddr, 0);
        }
        cpu.gpr[2] = currentFontHandle;
    }

    public void sceFontGetCharGlyphImage_Clip(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int fontAddr = cpu.gpr[4];
        int charCode = cpu.gpr[5];
        int glyphImageAddr = cpu.gpr[6];
        int clipXPos = cpu.gpr[7];
        int clipYPos = cpu.gpr[8];
        int clipWidth = cpu.gpr[9];
        int clipHeight = cpu.gpr[10];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetCharGlyphImage_Clip fontAddr=0x%08X, charCode=%04X (%c), glyphImageAddr=%08X" +
                    ", clipXPos=%d, clipYPos=%d, clipWidth=%d, clipHeight=%d,", fontAddr, charCode, (charCode <= 0xFF ? (char) charCode : '?'), glyphImageAddr, clipXPos, clipYPos, clipWidth, clipHeight));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Identical to sceFontGetCharGlyphImage, but uses a clipping
        // rectangle over the char.
        if (mem.isAddressGood(glyphImageAddr)) {
            // Read GlyphImage data
            int pixelFormat = mem.read32(glyphImageAddr + 0);
            int xPos64 = mem.read32(glyphImageAddr + 4);
            int yPos64 = mem.read32(glyphImageAddr + 8);
            int bufWidth = mem.read16(glyphImageAddr + 12);
            int bufHeight = mem.read16(glyphImageAddr + 14);
            int bytesPerLine = mem.read16(glyphImageAddr + 16);
            int buffer = mem.read32(glyphImageAddr + 20);
            // 26.6 fixed-point.
            int xPosI = xPos64 >> 6;
            int yPosI = yPos64 >> 6;

            if (log.isDebugEnabled()) {
                log.debug("sceFontGetCharGlyphImage_Clip c=" + ((char) charCode) + ", xPos=" + xPosI + ", yPos=" + yPosI + ", buffer=0x" + Integer.toHexString(buffer) + ", bufWidth=" + bufWidth + ", bufHeight=" + bufHeight + ", bytesPerLine=" + bytesPerLine + ", pixelFormat=" + pixelFormat);
            }

            PGF currentPGF = PGFFilesMap.get(fontAddr);
            if (currentPGF != null) {
                // Font adjustment.
                // TODO: Instead of using the loaded PGF, figure out
                // the proper values for the Debug font.
                yPosI -= (currentPGF.getMaxBaseYAdjust() >> 6);
                yPosI += (currentPGF.getMaxTopYAdjust() >> 6);
            }
            // Use our Debug font.
            Debug.printFontbuffer(buffer, bytesPerLine, bufWidth, bufHeight,
                    xPosI, yPosI, pixelFormat, (char) charCode);
        }
        cpu.gpr[2] = 0;
    }

    public void sceFontGetNumFontList(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int libHandle = cpu.gpr[4];
        int errorCodeAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetNumFontList libHandle=0x%08X, errorCodeAddr=0x%08X", libHandle, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            // Get all the available fonts in this font lib.
            int numFonts = fontLibMap.get(libHandle).getNumFonts();
            if (mem.isAddressGood(errorCodeAddr)) {
                mem.write32(errorCodeAddr, 0);
            }
            cpu.gpr[2] = numFonts;
        }
    }

    public void sceFontGetFontList(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int libHandle = cpu.gpr[4];
        int fontStyleAddr = cpu.gpr[5];
        int numFonts = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetFontList libHandle=0x%08X, fontListAddr=0x%08X, numFonts=%d", libHandle, fontStyleAddr, numFonts));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            FontLib fLib = fontLibMap.get(libHandle);
            int fonts = (numFonts > fLib.getNumFonts()) ? fLib.getNumFonts() : numFonts;
            for (int i = 0; i < fonts; i++) {
                PGF currentPGF = PGFFilesMap.get(fLib.getFakeFontHandle(i));
                if (currentPGF != null) {
                    mem.write32(fontStyleAddr, Float.floatToRawIntBits(0.0f));                         // Horizontal size.
                    mem.write32(fontStyleAddr + 4, Float.floatToRawIntBits(0.0f));                     // Vertical size.
                    mem.write32(fontStyleAddr + 8, Float.floatToRawIntBits(globalFontHRes));           // Horizontal resolution.
                    mem.write32(fontStyleAddr + 12, Float.floatToRawIntBits(globalFontVRes));          // Vertical resolution.
                    mem.write32(fontStyleAddr + 16, Float.floatToRawIntBits(0.0f));                    // Font weight.
                    mem.write16(fontStyleAddr + 20, (short) 0);                                        // Font family (SYSTEM = 0, probably more).
                    mem.write16(fontStyleAddr + 22, (short) 0);                                        // Style (SYSTEM = 0, STANDARD = 1, probably more).
                    mem.write16(fontStyleAddr + 24, (short) 0);                                        // Subset of style (only used in Asian fonts, unknown values).
                    mem.write16(fontStyleAddr + 26, (short) 0);                                        // Language code (UNK = 0, JAPANESE = 1, ENGLISH = 2, probably more).
                    mem.write16(fontStyleAddr + 28, (short) 0);                                        // Region code (UNK = 0, JAPAN = 1, probably more).
                    mem.write16(fontStyleAddr + 30, (short) 0);                                        // Country code (UNK = 0, JAPAN = 1, US = 2, probably more).
                    Utilities.writeStringNZ(mem, fontStyleAddr + 30, 64, currentPGF.getFontName());    // Font name (maximum size is 64).
                    Utilities.writeStringNZ(mem, fontStyleAddr + 94, 64, currentPGF.getFileNamez());   // File name (maximum size is 64).
                    mem.write32(fontStyleAddr + 158, 0);                                               // Additional attributes.
                    mem.write32(fontStyleAddr + 162, 0);                                               // Expiration date.
                }
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceFontSetAltCharacterCode(Processor processor) {
        CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];
        int charCode = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontSetAltCharacterCode libHandle=0x%08X, charCode=%04X", libHandle, charCode));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        setAlternateChar((char) charCode);
        cpu.gpr[2] = 0;
    }

    public void sceFontGetCharImageRect(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int fontAddr = cpu.gpr[4];
        int charCode = cpu.gpr[5];
        int charRectAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetCharImageRect fontAddr=0x%08X, charCode=%04X , charRectAddr=0x%08X", fontAddr, charCode, charRectAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // This function retrieves the dimensions of a specific char.
        // Faking.
        mem.write16(charRectAddr, (short) 1);  // Width.
        mem.write16(charRectAddr, (short) 1);  // Height.
        cpu.gpr[2] = 0;
    }

    public void sceFontPointToPixelH(Processor processor) {
        CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];
        int fontPointsH = cpu.gpr[5];
        int errorCodeAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontPointToPixelH libHandle=0x%08X, fontPointsH=%d, errorCodeAddr=0x%08X", libHandle, fontPointsH, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Convert horizontal floating points to pixels (Points Per Inch to Pixels Per Inch).
        // points = (pixels / dpiX) * 72.
        // Assume dpiX = 100 and dpiY = 100.
        float fontPointsHF = (float) fontPointsH;
        int pixels = Float.floatToRawIntBits((fontPointsHF / 72) * 100);

        cpu.gpr[2] = pixels;
    }

    public void sceFontGetFontInfoByIndexNumber(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int libHandle = cpu.gpr[4];
        int fontInfoAddr = cpu.gpr[5];
        int fontIndex = cpu.gpr[7];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetFontInfoByIndexNumber libHandle=0x%08X, fontInfoAddr=0x%08X, fontIndex=%d", libHandle, fontInfoAddr, fontIndex));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            FontLib currentFontLib = fontLibMap.get(libHandle);
            if (mem.isAddressGood(fontInfoAddr) && currentFontLib.getNumFonts() > fontIndex) {
                PGF currentPGF = PGFFilesMap.get(currentFontLib.getFakeFontHandle(fontIndex));
                if (currentPGF != null) {
                    int maxGlyphWidthI = currentPGF.getMaxSize()[0];
                    int maxGlyphHeightI = currentPGF.getMaxSize()[1];
                    int maxGlyphAscenderI = currentPGF.getMaxBaseYAdjust();
                    int maxGlyphDescenderI = (currentPGF.getMaxBaseYAdjust() - currentPGF.getMaxSize()[1]);
                    int maxGlyphLeftXI = currentPGF.getMaxLeftXAdjust();
                    int maxGlyphBaseYI = currentPGF.getMaxBaseYAdjust();
                    int minGlyphCenterXI = currentPGF.getMinCenterXAdjust();
                    int maxGlyphTopYI = currentPGF.getMaxTopYAdjust();
                    int maxGlyphAdvanceXI = currentPGF.getMaxAdvance()[0];
                    int maxGlyphAdvanceYI = currentPGF.getMaxAdvance()[1];
                    float maxGlyphWidthF = Float.intBitsToFloat(maxGlyphWidthI);
                    float maxGlyphHeightF = Float.intBitsToFloat(maxGlyphHeightI);
                    float maxGlyphAscenderF = Float.intBitsToFloat(maxGlyphAscenderI);
                    float maxGlyphDescenderF = Float.intBitsToFloat(maxGlyphDescenderI);
                    float maxGlyphLeftXF = Float.intBitsToFloat(maxGlyphLeftXI);
                    float maxGlyphBaseYF = Float.intBitsToFloat(maxGlyphBaseYI);
                    float minGlyphCenterXF = Float.intBitsToFloat(minGlyphCenterXI);
                    float maxGlyphTopYF = Float.intBitsToFloat(maxGlyphTopYI);
                    float maxGlyphAdvanceXF = Float.intBitsToFloat(maxGlyphAdvanceXI);
                    float maxGlyphAdvanceYF = Float.intBitsToFloat(maxGlyphAdvanceYI);

                    // Glyph metrics (in 26.6 signed fixed-point).
                    mem.write32(fontInfoAddr + 0, maxGlyphWidthI);
                    mem.write32(fontInfoAddr + 4, maxGlyphHeightI);
                    mem.write32(fontInfoAddr + 8, maxGlyphAscenderI);
                    mem.write32(fontInfoAddr + 12, maxGlyphDescenderI);
                    mem.write32(fontInfoAddr + 16, maxGlyphLeftXI);
                    mem.write32(fontInfoAddr + 20, maxGlyphBaseYI);
                    mem.write32(fontInfoAddr + 24, minGlyphCenterXI);
                    mem.write32(fontInfoAddr + 28, maxGlyphTopYI);
                    mem.write32(fontInfoAddr + 32, maxGlyphAdvanceXI);
                    mem.write32(fontInfoAddr + 36, maxGlyphAdvanceYI);
                    // Glyph metrics (replicated as float).
                    mem.write32(fontInfoAddr + 40, Float.floatToRawIntBits(maxGlyphWidthF));
                    mem.write32(fontInfoAddr + 44, Float.floatToRawIntBits(maxGlyphHeightF));
                    mem.write32(fontInfoAddr + 48, Float.floatToRawIntBits(maxGlyphAscenderF));
                    mem.write32(fontInfoAddr + 52, Float.floatToRawIntBits(maxGlyphDescenderF));
                    mem.write32(fontInfoAddr + 56, Float.floatToRawIntBits(maxGlyphLeftXF));
                    mem.write32(fontInfoAddr + 60, Float.floatToRawIntBits(maxGlyphBaseYF));
                    mem.write32(fontInfoAddr + 64, Float.floatToRawIntBits(minGlyphCenterXF));
                    mem.write32(fontInfoAddr + 68, Float.floatToRawIntBits(maxGlyphTopYF));
                    mem.write32(fontInfoAddr + 72, Float.floatToRawIntBits(maxGlyphAdvanceXF));
                    mem.write32(fontInfoAddr + 76, Float.floatToRawIntBits(maxGlyphAdvanceYF));
                    // Bitmap dimensions.
                    mem.write16(fontInfoAddr + 80, (short) currentPGF.getMaxGlyphWidth());
                    mem.write16(fontInfoAddr + 82, (short) currentPGF.getMaxGlyphHeight());
                    mem.write32(fontInfoAddr + 84, currentPGF.getCharMapLenght()); // Number of elements in the font's charmap.
                    mem.write32(fontInfoAddr + 88, currentPGF.getShadowMapLenght());   // Number of elements in the font's shadow charmap.
                    // Font style (used by font comparison functions).
                    mem.write32(fontInfoAddr + 92, Float.floatToRawIntBits(0.0f));   // Horizontal size.
                    mem.write32(fontInfoAddr + 96, Float.floatToRawIntBits(0.0f));   // Vertical size.
                    mem.write32(fontInfoAddr + 100, Float.floatToRawIntBits(globalFontHRes));  // Horizontal resolution.
                    mem.write32(fontInfoAddr + 104, Float.floatToRawIntBits(globalFontVRes));  // Vertical resolution.
                    mem.write32(fontInfoAddr + 108, Float.floatToRawIntBits(0.0f));  // Font weight.
                    mem.write16(fontInfoAddr + 112, (short) 0);  // Font family (SYSTEM = 0, probably more).
                    mem.write16(fontInfoAddr + 114, (short) 0);  // Style (SYSTEM = 0, STANDARD = 1, probably more).
                    mem.write16(fontInfoAddr + 116, (short) 0);  // Subset of style (only used in Asian fonts, unknown values).
                    mem.write16(fontInfoAddr + 118, (short) 0);  // Language code (UNK = 0, JAPANESE = 1, ENGLISH = 2, probably more).
                    mem.write16(fontInfoAddr + 120, (short) 0);  // Region code (UNK = 0, JAPAN = 1, probably more).
                    mem.write16(fontInfoAddr + 122, (short) 0);  // Country code (UNK = 0, JAPAN = 1, US = 2, probably more).
                    Utilities.writeStringNZ(mem, fontInfoAddr + 124, 64, currentPGF.getFontName());    // Font name (maximum size is 64).
                    Utilities.writeStringNZ(mem, fontInfoAddr + 188, 64, currentPGF.getFileNamez());   // File name (maximum size is 64).
                    mem.write32(fontInfoAddr + 252, 0); // Additional attributes.
                    mem.write32(fontInfoAddr + 256, 0); // Expiration date.
                    mem.write8(fontInfoAddr + 260, (byte) 4); // Font's BPP.
                    mem.write8(fontInfoAddr + 261, (byte) 0); // Padding.
                    mem.write8(fontInfoAddr + 262, (byte) 0); // Padding.
                    mem.write8(fontInfoAddr + 263, (byte) 0); // Padding.
                }
            }
            cpu.gpr[2] = 0;
        }
    }

    public void sceFontSetResolution(Processor processor) {
        CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];
        int hRes = cpu.gpr[5];
        int vRes = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontSetResolution libHandle=0x%08X, hRes=%d, vRes=%d", libHandle, hRes, vRes));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        globalFontHRes = (float) hRes;
        globalFontVRes = (float) vRes;
        cpu.gpr[2] = 0;
    }

    public void sceFontFlush(Processor processor) {
        CpuState cpu = processor.cpu;

        int fontAddr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontFlush fontAddr=0x%08X", fontAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    public void sceFontFindFont(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int libHandle = cpu.gpr[4];
        int fontStyleAddr = cpu.gpr[5];
        int errorCodeAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontFindFont libHandle=0x%08X, fontStyleAddr=0x%08X, errorCodeAddr=0x%08X", libHandle, fontStyleAddr, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Font style parameters.
        int fontH = mem.read32(fontStyleAddr);
        int fontV = mem.read32(fontStyleAddr + 4);
        int fontHRes = mem.read32(fontStyleAddr + 8);
        int fontVRes = mem.read32(fontStyleAddr + 12);
        int fontWeight = mem.read32(fontStyleAddr + 16);
        int fontFamily = mem.read16(fontStyleAddr + 20);
        int fontStyle = mem.read16(fontStyleAddr + 22);
        int fontStyleSub = mem.read16(fontStyleAddr + 24);
        int fontLanguage = mem.read16(fontStyleAddr + 26);
        int fontRegion = mem.read16(fontStyleAddr + 28);
        int fontCountry = mem.read16(fontStyleAddr + 30);
        String fontName = Utilities.readStringNZ(fontStyleAddr + 32, 64);
        String fontFileName = Utilities.readStringNZ(fontStyleAddr + 96, 64);
        int fontAttributes = mem.read32(fontStyleAddr + 160);
        int fontExpire = mem.read32(fontStyleAddr + 164);

        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            // Faking. If a perfect match is not found, returns -1.
            int fontIndex = 0;
            if (mem.isAddressGood(errorCodeAddr)) {
                mem.write32(errorCodeAddr, 0);
            }
            cpu.gpr[2] = fontIndex;
        }
    }

    public void sceFontPointToPixelV(Processor processor) {
        CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];
        int fontPointsV = cpu.gpr[5];
        int errorCodeAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontPointToPixelV libHandle=0x%08X, fontPointsV=%d, errorCodeAddr=0x%08X", libHandle, fontPointsV, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Convert vertical floating points to pixels (Points Per Inch to Pixels Per Inch).
        // points = (pixels / dpiX) * 72.
        // Assume dpiX = 100 and dpiY = 100.
        float fontPointsVF = (float) fontPointsV;
        int pixels = Float.floatToRawIntBits((fontPointsVF / 72) * 100);

        cpu.gpr[2] = pixels;
    }

    public void sceFontPixelToPointH(Processor processor) {
        CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];
        int fontPixelsH = cpu.gpr[5];
        int errorCodeAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontPixelToPointH libHandle=0x%08X, fontPixelsH=%d, errorCodeAddr=0x%08X", libHandle, fontPixelsH, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Convert horizontal pixels to floating points (Pixels Per Inch to Points Per Inch).
        // points = (pixels / dpiX) * 72.
        // Assume dpiX = 100 and dpiY = 100.
        float fontPixelsHF = (float) fontPixelsH;
        int points = Float.floatToRawIntBits((fontPixelsHF / 100) * 72);

        cpu.gpr[2] = points;
    }

    public void sceFontPixelToPointV(Processor processor) {
        CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];
        int fontPixelsV = cpu.gpr[5];
        int errorCodeAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontPixelToPointV libHandle=0x%08X, fontPixelsV=%d, errorCodeAddr=0x%08X", libHandle, fontPixelsV, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Convert vertical pixels to floating points (Pixels Per Inch to Points Per Inch).
        // points = (pixels / dpiX) * 72.
        // Assume dpiX = 100 and dpiY = 100.
        float fontPixelsVF = (float) fontPixelsV;
        int points = Float.floatToRawIntBits((fontPixelsVF / 100) * 72);

        cpu.gpr[2] = points;
    }

    public final HLEModuleFunction sceFontFindOptimumFontFunction = new HLEModuleFunction("sceFont", "sceFontFindOptimumFont") {

        @Override
        public final void execute(Processor processor) {
            sceFontFindOptimumFont(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontFindOptimumFont(Processor);";
        }
    };
    public final HLEModuleFunction sceFontGetFontInfoFunction = new HLEModuleFunction("sceFont", "sceFontGetFontInfo") {

        @Override
        public final void execute(Processor processor) {
            sceFontGetFontInfo(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontGetFontInfo(Processor);";
        }
    };
    public final HLEModuleFunction sceFontCloseFunction = new HLEModuleFunction("sceFont", "sceFontClose") {

        @Override
        public final void execute(Processor processor) {
            sceFontClose(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontClose(Processor);";
        }
    };
    public final HLEModuleFunction sceFontDoneLibFunction = new HLEModuleFunction("sceFont", "sceFontDoneLib") {

        @Override
        public final void execute(Processor processor) {
            sceFontDoneLib(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontDoneLib(Processor);";
        }
    };
    public final HLEModuleFunction sceFontNewLibFunction = new HLEModuleFunction("sceFont", "sceFontNewLib") {

        @Override
        public final void execute(Processor processor) {
            sceFontNewLib(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontNewLib(Processor);";
        }
    };
    public final HLEModuleFunction sceFontOpenFunction = new HLEModuleFunction("sceFont", "sceFontOpen") {

        @Override
        public final void execute(Processor processor) {
            sceFontOpen(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontOpen(Processor);";
        }
    };
    public final HLEModuleFunction sceFontGetCharGlyphImage_ClipFunction = new HLEModuleFunction("sceFont", "sceFontGetCharGlyphImage_Clip") {

        @Override
        public final void execute(Processor processor) {
            sceFontGetCharGlyphImage_Clip(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontGetCharGlyphImage_Clip(Processor);";
        }
    };
    public final HLEModuleFunction sceFontGetCharInfoFunction = new HLEModuleFunction("sceFont", "sceFontGetCharInfo") {

        @Override
        public final void execute(Processor processor) {
            sceFontGetCharInfo(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontGetCharInfo(Processor);";
        }
    };
    public final HLEModuleFunction sceFontGetCharGlyphImageFunction = new HLEModuleFunction("sceFont", "sceFontGetCharGlyphImage") {

        @Override
        public final void execute(Processor processor) {
            sceFontGetCharGlyphImage(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontGetCharGlyphImage(Processor);";
        }
    };
    public final HLEModuleFunction sceFontGetNumFontListFunction = new HLEModuleFunction("sceFont", "sceFontGetNumFontList") {

        @Override
        public final void execute(Processor processor) {
            sceFontGetNumFontList(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontGetNumFontList(Processor);";
        }
    };
    public final HLEModuleFunction sceFontGetFontListFunction = new HLEModuleFunction("sceFont", "sceFontGetFontList") {

        @Override
        public final void execute(Processor processor) {
            sceFontGetFontList(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontGetFontList(Processor);";
        }
    };
    public final HLEModuleFunction sceFontOpenUserMemoryFunction = new HLEModuleFunction("sceFont", "sceFontOpenUserMemory") {

        @Override
        public final void execute(Processor processor) {
            sceFontOpenUserMemory(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontOpenUserMemory(Processor);";
        }
    };
    public final HLEModuleFunction sceFontSetAltCharacterCodeFunction = new HLEModuleFunction("sceFont", "sceFontSetAltCharacterCode") {

        @Override
        public final void execute(Processor processor) {
            sceFontSetAltCharacterCode(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontSetAltCharacterCode(Processor);";
        }
    };
    public final HLEModuleFunction sceFontGetCharImageRectFunction = new HLEModuleFunction("sceFont", "sceFontGetCharImageRect") {

        @Override
        public final void execute(Processor processor) {
            sceFontGetCharImageRect(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontGetCharImageRect(Processor);";
        }
    };
    public final HLEModuleFunction sceFontPointToPixelHFunction = new HLEModuleFunction("sceFont", "sceFontPointToPixelH") {

        @Override
        public final void execute(Processor processor) {
            sceFontPointToPixelH(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontPointToPixelH(Processor);";
        }
    };
    public final HLEModuleFunction sceFontGetFontInfoByIndexNumberFunction = new HLEModuleFunction("sceFont", "sceFontGetFontInfoByIndexNumber") {

        @Override
        public final void execute(Processor processor) {
            sceFontGetFontInfoByIndexNumber(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontGetFontInfoByIndexNumber(Processor);";
        }
    };
    public final HLEModuleFunction sceFontSetResolutionFunction = new HLEModuleFunction("sceFont", "sceFontSetResolution") {

        @Override
        public final void execute(Processor processor) {
            sceFontSetResolution(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontSetResolution(Processor);";
        }
    };
    public final HLEModuleFunction sceFontFlushFunction = new HLEModuleFunction("sceFont", "sceFontFlush") {

        @Override
        public final void execute(Processor processor) {
            sceFontFlush(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontFlush(Processor);";
        }
    };
    public final HLEModuleFunction sceFontOpenUserFileFunction = new HLEModuleFunction("sceFont", "sceFontOpenUserFile") {

        @Override
        public final void execute(Processor processor) {
            sceFontOpenUserFile(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontOpenUserFile(Processor);";
        }
    };
    public final HLEModuleFunction sceFontFindFontFunction = new HLEModuleFunction("sceFont", "sceFontFindFont") {

        @Override
        public final void execute(Processor processor) {
            sceFontFindFont(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontFindFont(Processor);";
        }
    };
    public final HLEModuleFunction sceFontPointToPixelVFunction = new HLEModuleFunction("sceFont", "sceFontPointToPixelV") {

        @Override
        public final void execute(Processor processor) {
            sceFontPointToPixelV(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontPointToPixelV(Processor);";
        }
    };
    public final HLEModuleFunction sceFontPixelToPointHFunction = new HLEModuleFunction("sceFont", "sceFontPixelToPointH") {

        @Override
        public final void execute(Processor processor) {
            sceFontPixelToPointH(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontPixelToPointH(Processor);";
        }
    };
    public final HLEModuleFunction sceFontPixelToPointVFunction = new HLEModuleFunction("sceFont", "sceFontPixelToPointV") {

        @Override
        public final void execute(Processor processor) {
            sceFontPixelToPointV(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontPixelToPointV(Processor);";
        }
    };
}