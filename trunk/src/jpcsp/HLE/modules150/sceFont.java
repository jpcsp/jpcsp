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
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.format.PGF;
import jpcsp.util.Debug;
import jpcsp.util.Utilities;

public class sceFont implements HLEModule {
	@Override
	public String getName() {
		return "sceFont";
	}

	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {
			mm.addFunction(sceFontFindOptimumFontFunction, 0x99EF33C);
			mm.addFunction(sceFontGetFontInfoFunction, 0xDA7535E);
			mm.addFunction(sceFontCloseFunction, 0x3AEA8CB6);
			mm.addFunction(sceFontDoneLibFunction, 0x574B6FBC);
			mm.addFunction(sceFontNewLibFunction, 0x67F17ED7);
			mm.addFunction(sceFontOpenFunction, 0xA834319D);
			mm.addFunction(sceFontGetCharGlyphImage_ClipFunction, 0xCA1E6945);
			mm.addFunction(sceFontGetCharInfoFunction, 0xDCC80C2F);
			mm.addFunction(sceFontGetCharGlyphImageFunction, 0x980F4895);
			mm.addFunction(sceFontGetNumFontListFunction, 0x27F6E642);
			mm.addFunction(sceFontGetFontListFunction, 0xBC75D85B);
			mm.addFunction(sceFontOpenUserMemoryFunction, 0xBB8E7FE6);
			mm.addFunction(sceFontSetAltCharacterCodeFunction, 0xEE232411);
			mm.addFunction(sceFontGetCharImageRectFunction, 0x5C3E4A9E);
			mm.addFunction(sceFontPointToPixelHFunction, 0x472694CD);
			mm.addFunction(sceFontGetFontInfoByIndexNumberFunction, 0x5333322D);
			mm.addFunction(sceFontSetResolutionFunction, 0x48293280);
			mm.addFunction(sceFontFlushFunction, 0x02D7F94B);
			mm.addFunction(sceFontOpenUserFileFunction, 0x57FCB733);
			mm.addFunction(sceFontFindFontFunction, 0x681E61A7);
            mm.addFunction(sceFontPointToPixelVFunction, 0x3C4B7E82);
            mm.addFunction(sceFontPixelToPointHFunction, 0x74B21701);
            mm.addFunction(sceFontPixelToPointVFunction, 0xF8F0752E);

            fontLibMap = new HashMap<Integer, FontLib>();
            PGFFilesMap = new HashMap<Integer, PGF>();
            fontLibCount = 0;
            currentFontHandle = 0;
            currentExternalFontHandle = 0;
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

	public static final int FONT_PIXELFORMAT_4     = 0; // 2 pixels packed in 1 byte (natural order)
	public static final int FONT_PIXELFORMAT_4_REV = 1; // 2 pixels packed in 1 byte (reversed order)
	public static final int FONT_PIXELFORMAT_8     = 2; // 1 pixel in 1 byte
	public static final int FONT_PIXELFORMAT_24    = 3; // 1 pixel in 3 bytes (RGB)
	public static final int FONT_PIXELFORMAT_32    = 4; // 1 pixel in 4 bytes (RGBA)

	private HashMap<Integer, FontLib> fontLibMap;
    private HashMap<Integer, PGF> PGFFilesMap;
	private int fontLibCount;
	private int currentFontHandle;
	private int currentExternalFontHandle;
    private float globalFontHRes = 0.0f;
    private float globalFontVRes = 0.0f;
	public static final int PGF_MAGIC = 'P' << 24 | 'G' << 16 | 'F' << 8 | '0';
    public static final String fontDirPath = "flash0/font";

    public int makeFakeLibHandle() {
        return 0xF8F80000 | (fontLibCount++ & 0xFFFF);
    }

    protected class FontLib {
        private int unk1;
        private int numFonts;
        private int unk2;
        private int allocFuncAddr;
        private int freeFuncAddr;
        private int openFuncAddr;
        private int closeFuncAddr;
        private int readFuncAddr;
        private int seekFuncAddr;
        private int errorFuncAddr;
        private int ioFinishFuncAddr;

        private int[] fonts;
        private int libBufAddr;

        public FontLib(int params) {
            read(params);
            fonts = new int[numFonts];

            for(int i = 0; i < numFonts; i++) {
                fonts[i] = makeFakeFontHandle(i);
            }

            if (allocFuncAddr != 0) {
                triggerAllocCallback(512);
            }

            loadFontFiles();
        }

        public void loadFontFiles() {
            File f = new File(fontDirPath);
            String[] files = f.list();
            int index = 0;

            for(int i = 0; i < files.length; i++) {
                String currentFile = (fontDirPath + "/" + files[i]);
                if(currentFile.endsWith(".pgf") && index < numFonts) {
                    try {
                        RandomAccessFile fontFile = new RandomAccessFile(currentFile, "r");
                        byte[] pgfBuf = new byte[(int)fontFile.length()];
                        fontFile.read(pgfBuf);
                        ByteBuffer finalBuf = ByteBuffer.wrap(pgfBuf);

                        PGF pgfFile = new PGF(finalBuf);
                        pgfFile.setFileNamez(files[i]);
                        PGFFilesMap.put(getFakeFontHandle(index), pgfFile);

                        Modules.log.info("Found font file '" + files[i] + "'. Font='"
                                + pgfFile.getFontName() + "' Type='" + pgfFile.getFontType() + "'");
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

        public int makeFakeExternalFontHandle(int fontNum) {
            return 0xF7F70000 | (fontNum & 0xFFFF);
        }

        public int getFakeFontHandle(int i) {
            return fonts[i];
        }

        public int getAllocBufferAddr() {
            return libBufAddr;
        }

        public int getNumFonts() {
            return numFonts;
        }

        private void triggerAllocCallback(int size) {
        	Modules.ThreadManForUserModule.executeCallback(null, allocFuncAddr, new AfterAllocCallback(), unk1, size);
        }

        private void triggerFreeCallback() {
        	Modules.ThreadManForUserModule.executeCallback(null, freeFuncAddr, null, unk1, getAllocBufferAddr());
        }

        private void read(int paramsAddr) {
            Memory mem = Memory.getInstance();

            unk1                = mem.read32(paramsAddr);
            numFonts            = mem.read32(paramsAddr + 4);
            unk2                = mem.read32(paramsAddr + 8);
            allocFuncAddr       = mem.read32(paramsAddr + 12);
            freeFuncAddr        = mem.read32(paramsAddr + 16);
            openFuncAddr        = mem.read32(paramsAddr + 20);
            closeFuncAddr       = mem.read32(paramsAddr + 24);
            readFuncAddr        = mem.read32(paramsAddr + 28);
            seekFuncAddr        = mem.read32(paramsAddr + 32);
            errorFuncAddr       = mem.read32(paramsAddr + 36);
            ioFinishFuncAddr    = mem.read32(paramsAddr + 42);
        }

        private class AfterAllocCallback implements IAction {
			@Override
			public void execute() {
				libBufAddr = Emulator.getProcessor().cpu.gpr[2];

				Modules.log.info("FontLib's allocation callback (size=512, numFonts="
                        + numFonts + ") returned 0x"
                        + Integer.toHexString(libBufAddr));
			}
        }
    }

	public void sceFontNewLib(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Memory.getInstance();

		int paramsAddr = cpu.gpr[4];
		int errorCodeAddr = cpu.gpr[5];
		Modules.log.warn(String.format("PARTIAL: sceFontNewLib paramsAddr=0x%08X, errorCodeAddr=0x%08X", paramsAddr, errorCodeAddr));

        FontLib fl = null;
        int handle = 0;

		if (mem.isAddressGood(paramsAddr)) {
            fl = new FontLib(paramsAddr);
            handle = makeFakeLibHandle();
            fontLibMap.put(handle, fl);

			if (Modules.log.isDebugEnabled()) {
				for (int i = 0; i < 44; i += 4) {
					Modules.log.debug(String.format("         paramsAddr+%2d=0x%08X", i, mem.read32(paramsAddr + i)));
				}
			}
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
		String fileName = Utilities.readStringZ(fileNameAddr);
		int mode = cpu.gpr[6];	// Values: 1 - Represents from where the font is loaded. 1 is from file, 0 is from memory.
		int errorCodeAddr = cpu.gpr[7];

		Modules.log.warn(String.format("PARTIAL: sceFontOpenUserFile libHandle=0x%08X, fileName=%s, unknown=0x%08X, errorCodeAddr=0x%08X",
                libHandle, fileName, mode, errorCodeAddr));

        int externalHandle = 0;

		if (!fontLibMap.containsKey(libHandle)) {
			Modules.log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
			cpu.gpr[2] = -1;
		} else {
            FontLib fLib = fontLibMap.get(libHandle);
            externalHandle = fLib.makeFakeExternalFontHandle(currentExternalFontHandle++);
			if (mem.isAddressGood(errorCodeAddr)) {
				mem.write32(errorCodeAddr, 0);
			}

			cpu.gpr[2] = externalHandle;
		}
	}

	public void sceFontOpenUserMemory(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Memory.getInstance();

		int libHandle = cpu.gpr[4];
		int memoryFontAddr = cpu.gpr[5];
		int memoryFontLength = cpu.gpr[6];
		int errorCodeAddr = cpu.gpr[7];

		Modules.log.warn(String.format("PARTIAL: sceFontOpenUserMemory libHandle=0x%08X, memoryFontAddr=0x%08X, memoryFontLength=%d, errorCodeAddr=0x%08X",
                libHandle, memoryFontAddr, memoryFontLength, errorCodeAddr));

        int externalHandle = 0;

		if (!fontLibMap.containsKey(libHandle)) {
			Modules.log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
			cpu.gpr[2] = -1;
		} else {
            FontLib fLib = fontLibMap.get(libHandle);
            externalHandle = fLib.makeFakeExternalFontHandle(currentExternalFontHandle++);
			if (mem.isAddressGood(memoryFontAddr)) {
				int magic = mem.read32(memoryFontAddr + 4);
				if (magic == PGF_MAGIC) {
					Modules.log.info("sceFontOpenUserMemory: PGF format detected");
				}
			}
			if (mem.isAddressGood(errorCodeAddr)) {
				mem.write32(errorCodeAddr, 0);
			}

			cpu.gpr[2] = externalHandle;
		}
	}

	public void sceFontGetFontInfo(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int fontAddr = cpu.gpr[4];
		int fontInfoAddr = cpu.gpr[5];

		Modules.log.warn(String.format("PARTIAL: sceFontGetFontInfo fontAddr=0x%08X, fontInfoAddr=0x%08X"
                , fontAddr, fontInfoAddr));

		if (fontAddr != currentFontHandle) {
			Modules.log.warn("Unknown fontAddr: 0x" + Integer.toHexString(fontAddr));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(fontInfoAddr)) {
                PGF currentPGF = PGFFilesMap.get(fontAddr);

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
                mem.write16(fontInfoAddr + 80, (short)currentPGF.getMaxGlyphWidth());
                mem.write16(fontInfoAddr + 82, (short)currentPGF.getMaxGlyphHeight());

                mem.write32(fontInfoAddr + 84, currentPGF.getCharMapLenght()); // Number of elements in the font's charmap.
                mem.write32(fontInfoAddr + 88, currentPGF.getShadowMapLenght());   // Number of elements in the font's shadow charmap.

                // Font style (used by font comparison functions).
                mem.write32(fontInfoAddr + 92, Float.floatToRawIntBits(0.0f));   // Horizontal size.
                mem.write32(fontInfoAddr + 96, Float.floatToRawIntBits(0.0f));   // Vertical size.
                mem.write32(fontInfoAddr + 100, Float.floatToRawIntBits(globalFontHRes));  // Horizontal resolution.
                mem.write32(fontInfoAddr + 104, Float.floatToRawIntBits(globalFontVRes));  // Vertical resolution.
                mem.write32(fontInfoAddr + 108, Float.floatToRawIntBits(0.0f));  // Font weight.
                mem.write16(fontInfoAddr + 112, (short)0);  // Font family (SYSTEM = 0, probably more).
                mem.write16(fontInfoAddr + 114, (short)0);  // Style (SYSTEM = 0, STANDARD = 1, probably more).
                mem.write16(fontInfoAddr + 116, (short)0);  // Subset of style (only used in Asian fonts, unknown values).
                mem.write16(fontInfoAddr + 118, (short)0);  // Language code (UNK = 0, JAPANESE = 1, ENGLISH = 2, probably more).
                mem.write16(fontInfoAddr + 120, (short)0);  // Region code (UNK = 0, JAPAN = 1, probably more).
                mem.write16(fontInfoAddr + 122, (short)0);  // Country code (UNK = 0, JAPAN = 1, US = 2, probably more).
                Utilities.writeStringNZ(mem, fontInfoAddr + 124, 64, currentPGF.getFontName());  // Font name (maximum size is 64).
                Utilities.writeStringNZ(mem, fontInfoAddr + 188, 64, currentPGF.getFileNamez());   // File name (maximum size is 64).
                mem.write32(fontInfoAddr + 252, 0); // Unknown.
                mem.write32(fontInfoAddr + 256, 0); // Unknown (some sort of timestamp?).

                mem.write8(fontInfoAddr + 260, (byte)4); // Font's BPP.
                mem.write8(fontInfoAddr + 261, (byte)0); // Unknown.
                mem.write8(fontInfoAddr + 262, (byte)0); // Unknown.
                mem.write8(fontInfoAddr + 263, (byte)0); // Unknown.
			}
			cpu.gpr[2] = 0;
		}
	}

	public void sceFontGetCharInfo(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int fontAddr = cpu.gpr[4];
		int charCode = cpu.gpr[5];
		int charInfoAddr = cpu.gpr[6];
		Modules.log.warn(String.format("PARTIAL: sceFontGetCharInfo fontAddr=0x%08X, charCode=%04X (%c), charInfoAddr=%08X"
                , fontAddr, charCode, (charCode <= 0xFF ? (char) charCode : '?'), charInfoAddr));

		if (fontAddr != currentFontHandle && fontAddr != 0) {
			Modules.log.warn("Unknown fontAddr: 0x" + Integer.toHexString(fontAddr));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(charInfoAddr)) {
                int bitmapWidth =  Debug.Font.charWidth * Debug.fontPixelSize;
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
                int sfp26Width =     bitmapWidth << 6;
                int sfp26Height =    bitmapHeight << 6;
                int sfp26Ascender =  0 << 6;
                int sfp26Descender = 0 << 6;
                int sfp26BearingHX = 0 << 6;
                int sfp26BearingHY = 0 << 6;
                int sfp26BearingVX = 0 << 6;
                int sfp26BearingVY = 0 << 6;
                int sfp26AdvanceH =  bitmapWidth << 6;
                int sfp26AdvanceV =  bitmapHeight << 6;

                // Bitmap dimensions.
				mem.write32(charInfoAddr +  0, bitmapWidth);	// bitmapWidth
				mem.write32(charInfoAddr +  4, bitmapHeight);	// bitmapHeight
                mem.write32(charInfoAddr +  8, bitmapLeft);	// bitmapLeft
				mem.write32(charInfoAddr + 12, bitmapTop);	// bitmapTop

                // Glyph metrics (in 26.6 signed fixed-point).
                // These values are used by sceFontGetCharGlyphImage.
                // TODO: Get each char's dimensions from the PGF file.
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

                // Unknown.
                mem.write8(charInfoAddr + 56, (byte)0);
                mem.write8(charInfoAddr + 57, (byte)0);
                mem.write16(charInfoAddr + 58, (short)0);
			}

			cpu.gpr[2] = 0;
		}
	}

	public void sceFontGetCharGlyphImage(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int fontAddr = cpu.gpr[4];
		int charCode = cpu.gpr[5];
		int glyphImageAddr = cpu.gpr[6];
		Modules.log.warn(String.format("PARTIAL: sceFontGetCharGlyphImage fontAddr=0x%08X, charCode=%04X (%c), glyphImageAddr=%08X"
                , fontAddr, charCode, (charCode <= 0xFF ? (char) charCode : '?'), glyphImageAddr));

		if (fontAddr != currentFontHandle && fontAddr != 0) {
			Modules.log.warn("Unknown fontAddr: 0x" + Integer.toHexString(fontAddr));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(glyphImageAddr)) {
                // sceFontGetCharGlyphImage is supposed to read and write the glyph's
                // data. It uses the PGF file for this.

                // Read GlyphImage data
                int pixelFormat  = mem.read32(glyphImageAddr +  0);
                int xPos64       = mem.read32(glyphImageAddr +  4);
                int yPos64       = mem.read32(glyphImageAddr +  8);
                int bufWidth     = mem.read16(glyphImageAddr + 12);
                int bufHeight    = mem.read16(glyphImageAddr + 14);
                int bytesPerLine = mem.read16(glyphImageAddr + 16);
                int buffer       = mem.read32(glyphImageAddr + 20);

                // 26.6 fixed-point.
                int xPosI = xPos64 >> 6;
                int yPosI = yPos64 >> 6;

                Modules.log.info("sceFontGetCharGlyphImage c=" + ((char) charCode)
                        + ", xPos=" + xPosI + ", yPos=" + yPosI
                        + ", buffer=0x" + Integer.toHexString(buffer)
                        + ", bufWidth=" + bufWidth
                        + ", bufHeight=" + bufHeight
                        + ", bytesPerLine=" + bytesPerLine
                        + ", pixelFormat=" + pixelFormat);

                PGF currentPGF = PGFFilesMap.get(fontAddr);
                if(currentPGF != null) {
                    // Font adjustment.
                    // TODO: Instead of using the loaded PGF, figure out
                    // the proper values for the Debug font.
                    yPosI -= (currentPGF.getMaxBaseYAdjust() >> 6);
                    yPosI += (currentPGF.getMaxTopYAdjust() >> 6);
                }

                // Use our Debug font.
                Debug.printFontbuffer(buffer, bytesPerLine, bufWidth, bufHeight,
                        xPosI, yPosI, pixelFormat, (char)charCode);
            }

			cpu.gpr[2] = 0;
		}
	}

	public void sceFontFindOptimumFont(Processor processor) {
		CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int libHandle = cpu.gpr[4];
        int fontStyleAddr = cpu.gpr[5];
        int errorCodeAddr = cpu.gpr[6];

		Modules.log.warn(String.format("PARTIAL: sceFontFindOptimumFont libHandle=0x%08X, fontStyleAddr=0x%08X, errorCodeAddr=0x%08X"
                , libHandle, fontStyleAddr, errorCodeAddr));

        int optimumFontIndex = 0;

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
        int fontUnk1 = mem.read16(fontStyleAddr + 160);
        int fontUnk2 = mem.read16(fontStyleAddr + 164);

        if (!fontLibMap.containsKey(libHandle)) {
			Modules.log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
			cpu.gpr[2] = -1;
		} else {
            // Return the first index.
            // TODO: Use an index based font list class to locate the best match.
            optimumFontIndex = 0;
			if (mem.isAddressGood(errorCodeAddr)) {
				mem.write32(errorCodeAddr, 0);
			}

			cpu.gpr[2] = optimumFontIndex;
		}
	}

	public void sceFontClose(Processor processor) {
		CpuState cpu = processor.cpu;

        int fontAddr = cpu.gpr[4];  // The font handle to close.

		Modules.log.warn(String.format("PARTIAL: sceFontClose fontAddr=0x%08X", fontAddr));

        // Faking.
		cpu.gpr[2] = 0;
	}

	public void sceFontDoneLib(Processor processor) {
		CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];

        Modules.log.warn(String.format("PARTIAL: sceFontDoneLib libHandle=0x%08X", libHandle));

        if (!fontLibMap.containsKey(libHandle)) {
			Modules.log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
			cpu.gpr[2] = -1;
		} else {
            // Free the reserved font lib space.
            fontLibMap.get(libHandle).triggerFreeCallback();

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

        Modules.log.warn(String.format("PARTIAL: sceFontOpen libHandle=0x%08X, index=%d, unk=%d, errorCodeAddr=0x%08X"
                , libHandle, index, mode, errorCodeAddr));

        FontLib fLib = fontLibMap.get(libHandle);
        if(fLib != null) {
            currentFontHandle = fLib.getFakeFontHandle(index);
        }
        PGF currentPGF = PGFFilesMap.get(currentFontHandle);
        if(currentPGF != null) {
            Modules.log.info("Opening '"
                    + currentPGF.getFontName() + "' - '" + currentPGF.getFontType() + "'");
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

		Modules.log.warn(String.format("PARTIAL: sceFontGetCharGlyphImage_Clip fontAddr=0x%08X, charCode=%04X (%c), glyphImageAddr=%08X" +
                ", clipXPos=%d, clipYPos=%d, clipWidth=%d, clipHeight=%d,"
                , fontAddr, charCode, (charCode <= 0xFF ? (char) charCode : '?'), glyphImageAddr
                , clipXPos, clipYPos, clipWidth, clipHeight));

        // Identical to sceFontGetCharGlyphImage, but uses a clipping
        // rectangle over the char.
		if (fontAddr != currentFontHandle && fontAddr != 0) {
			Modules.log.warn("Unknown fontAddr: 0x" + Integer.toHexString(fontAddr));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(glyphImageAddr)) {
                // Read GlyphImage data
                int pixelFormat  = mem.read32(glyphImageAddr +  0);
                int xPos64       = mem.read32(glyphImageAddr +  4);
                int yPos64       = mem.read32(glyphImageAddr +  8);
                int bufWidth     = mem.read16(glyphImageAddr + 12);
                int bufHeight    = mem.read16(glyphImageAddr + 14);
                int bytesPerLine = mem.read16(glyphImageAddr + 16);
                int buffer       = mem.read32(glyphImageAddr + 20);

                // 26.6 fixed-point.
                int xPosI = xPos64 >> 6;
                int yPosI = yPos64 >> 6;

                Modules.log.info("sceFontGetCharGlyphImage_Clip c=" + ((char) charCode)
                        + ", xPos=" + xPosI + ", yPos=" + yPosI
                        + ", buffer=0x" + Integer.toHexString(buffer)
                        + ", bufWidth=" + bufWidth
                        + ", bufHeight=" + bufHeight
                        + ", bytesPerLine=" + bytesPerLine
                        + ", pixelFormat=" + pixelFormat);

                PGF currentPGF = PGFFilesMap.get(fontAddr);               
                if(currentPGF != null) {
                    // Font adjustment.
                    // TODO: Instead of using the loaded PGF, figure out
                    // the proper values for the Debug font.
                    yPosI -= (currentPGF.getMaxBaseYAdjust() >> 6);
                    yPosI += (currentPGF.getMaxTopYAdjust() >> 6);
                }

                // Use our Debug font.
                Debug.printFontbuffer(buffer, bytesPerLine, bufWidth, bufHeight,
                        xPosI, yPosI, pixelFormat, (char)charCode);
            }

			cpu.gpr[2] = 0;
		}
	}

	public void sceFontGetNumFontList(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Memory.getInstance();

		int libHandle = cpu.gpr[4];
		int errorCodeAddr = cpu.gpr[5];

		Modules.log.warn(String.format("PARTIAL: sceFontGetNumFontList libHandle=0x%08X, errorCodeAddr=0x%08X"
                , libHandle, errorCodeAddr));

        int numFonts = 0;

		if (!fontLibMap.containsKey(libHandle)) {
			Modules.log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
			cpu.gpr[2] = -1;
		} else {
            // Get all the available fonts in this font lib.
            numFonts = fontLibMap.get(libHandle).getNumFonts();
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

        Modules.log.warn(String.format("PARTIAL: sceFontGetFontList libHandle=0x%08X, fontListAddr=0x%08X, numFonts=%d"
                , libHandle, fontStyleAddr, numFonts));

        // Font style parameters.
        // TODO: Make a class to store a font list and use numFonts to locate
        // more than the first font.
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
        int fontUnk1 = mem.read16(fontStyleAddr + 160);
        int fontUnk2 = mem.read16(fontStyleAddr + 164);

		if (!fontLibMap.containsKey(libHandle)) {
			Modules.log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
			cpu.gpr[2] = -1;
		} else {
			cpu.gpr[2] = 0;
		}
	}

	public void sceFontSetAltCharacterCode(Processor processor) {
		CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];
        int charCode = cpu.gpr[5];

		Modules.log.warn(String.format("IGNORING: sceFontSetAltCharacterCode libHandle=0x%08X, charCode=%04X"
                , libHandle, charCode));

        cpu.gpr[2] = 0;
	}

	public void sceFontGetCharImageRect(Processor processor) {
		CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int fontAddr = cpu.gpr[4];
		int charCode = cpu.gpr[5];
		int charRectAddr = cpu.gpr[6];

		Modules.log.warn(String.format("IGNORING: sceFontGetCharImageRect fontAddr=0x%08X, charCode=%04X , charRectAddr=0x%08X"
                , fontAddr, charCode, charRectAddr));

        // This function retrieves the dimensions of a specific char.
        // Faking.
        mem.write16(charRectAddr, (short)1);  // Width.
        mem.write16(charRectAddr, (short)1);  // Height.

		cpu.gpr[2] = 0;
	}

	public void sceFontPointToPixelH(Processor processor) {
		CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];
        int fontPointsH = cpu.gpr[5];
        int errorCodeAddr = cpu.gpr[6];

        float fontPointsHF = (float)fontPointsH;
		Modules.log.warn(String.format("PARTIAL: sceFontPointToPixelH libHandle=0x%08X, fontPointsHF=%f , errorCodeAddr=0x%08X"
                , libHandle, fontPointsHF, errorCodeAddr));

        // Convert horizontal floating points to pixels (Points Per Inch to Pixels Per Inch).
        // points = (pixels / dpiX) * 72.

        // Faking.
        // Assume dpiX = 100 and dpiY = 100.
        int pixels = Float.floatToRawIntBits((fontPointsHF / 72) * 100);

		cpu.gpr[2] = pixels;
	}

	public void sceFontGetFontInfoByIndexNumber(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int libHandle = cpu.gpr[4];
		int fontInfoAddr = cpu.gpr[5];
        int fontIndex = cpu.gpr[7];

		Modules.log.warn(String.format("PARTIAL: sceFontGetFontInfoByIndexNumber libHandle=0x%08X, fontInfoAddr=0x%08X, fontIndex=%d"
                , libHandle, fontInfoAddr, fontIndex));

		if (!fontLibMap.containsKey(libHandle)) {
			Modules.log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
			cpu.gpr[2] = -1;
		} else {
            FontLib currentFontLib = fontLibMap.get(libHandle);

			if (mem.isAddressGood(fontInfoAddr) && currentFontLib.getNumFonts() > fontIndex) {
                PGF currentPGF = PGFFilesMap.get(currentFontLib.getFakeFontHandle(fontIndex));

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
                mem.write16(fontInfoAddr + 80, (short)currentPGF.getMaxGlyphWidth());
                mem.write16(fontInfoAddr + 82, (short)currentPGF.getMaxGlyphHeight());

                mem.write32(fontInfoAddr + 84, currentPGF.getCharMapLenght()); // Number of elements in the font's charmap.
                mem.write32(fontInfoAddr + 88, currentPGF.getShadowMapLenght());   // Number of elements in the font's shadow charmap.

                // Font style (used by font comparison functions).
                mem.write32(fontInfoAddr + 92, Float.floatToRawIntBits(0.0f));   // Horizontal size.
                mem.write32(fontInfoAddr + 96, Float.floatToRawIntBits(0.0f));   // Vertical size.
                mem.write32(fontInfoAddr + 100, Float.floatToRawIntBits(globalFontHRes));  // Horizontal resolution.
                mem.write32(fontInfoAddr + 104, Float.floatToRawIntBits(globalFontVRes));  // Vertical resolution.
                mem.write32(fontInfoAddr + 108, Float.floatToRawIntBits(0.0f));  // Font weight.
                mem.write16(fontInfoAddr + 112, (short)0);  // Font family (SYSTEM = 0, probably more).
                mem.write16(fontInfoAddr + 114, (short)0);  // Style (SYSTEM = 0, STANDARD = 1, probably more).
                mem.write16(fontInfoAddr + 116, (short)0);  // Subset of style (only used in Asian fonts, unknown values).
                mem.write16(fontInfoAddr + 118, (short)0);  // Language code (UNK = 0, JAPANESE = 1, ENGLISH = 2, probably more).
                mem.write16(fontInfoAddr + 120, (short)0);  // Region code (UNK = 0, JAPAN = 1, probably more).
                mem.write16(fontInfoAddr + 122, (short)0);  // Country code (UNK = 0, JAPAN = 1, US = 2, probably more).
                Utilities.writeStringNZ(mem, fontInfoAddr + 124, 64, currentPGF.getFontName());  // Font name (maximum size is 64).
                Utilities.writeStringNZ(mem, fontInfoAddr + 188, 64, currentPGF.getFileNamez());   // File name (maximum size is 64).
                mem.write32(fontInfoAddr + 252, 0); // Unknown.
                mem.write32(fontInfoAddr + 256, 0); // Unknown (some sort of timestamp?).

                mem.write8(fontInfoAddr + 260, (byte)4); // Font's BPP.
                mem.write8(fontInfoAddr + 261, (byte)0); // Unknown.
                mem.write8(fontInfoAddr + 262, (byte)0); // Unknown.
                mem.write8(fontInfoAddr + 263, (byte)0); // Unknown.
			}
			cpu.gpr[2] = 0;
		}
	}

	public void sceFontSetResolution(Processor processor) {
		CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];
        int hRes = cpu.gpr[5];
        int vRes = cpu.gpr[6];

        float hResf = (float)hRes;
        float vResf = (float)vRes;

		Modules.log.warn(String.format("PARTIAL: sceFontSetResolution libHandle=0x%08X, hResf=%f, vResf=%f"
                , libHandle, hResf, vResf));

        globalFontHRes = hResf;
        globalFontVRes = vResf;

		cpu.gpr[2] = 0;
	}

	public void sceFontFlush(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontFlush 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontFindFont(Processor processor) {
		CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int libHandle = cpu.gpr[4];
        int fontStyleAddr = cpu.gpr[5];
        int errorCodeAddr = cpu.gpr[6];

		Modules.log.warn(String.format("PARTIAL: sceFontFindFont libHandle=0x%08X, fontStyleAddr=0x%08X, errorCodeAddr=0x%08X"
                , libHandle, fontStyleAddr, errorCodeAddr));

        int fontIndex = 0;

        // Font style parameters (identical to sceFontFindOptimumFont?).
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
        int fontUnk1 = mem.read16(fontStyleAddr + 160);
        int fontUnk2 = mem.read16(fontStyleAddr + 164);

        if (!fontLibMap.containsKey(libHandle)) {
			Modules.log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
			cpu.gpr[2] = -1;
		} else {
            // Return the first index.
            // TODO: Use an index based font list class to locate the best match.
            fontIndex = 0;
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

        float fontPointsVF = (float)fontPointsV;
		Modules.log.warn(String.format("PARTIAL: sceFontPointToPixelV libHandle=0x%08X, fontPointsVF=%f , errorCodeAddr=0x%08X"
                , libHandle, fontPointsVF, errorCodeAddr));

        // Convert vertical floating points to pixels (Points Per Inch to Pixels Per Inch).
        // points = (pixels / dpiX) * 72.

        // Faking.
        // Assume dpiX = 100 and dpiY = 100.
        int pixels = Float.floatToRawIntBits((fontPointsVF / 72) * 100);

		cpu.gpr[2] = pixels;
	}

    public void sceFontPixelToPointH(Processor processor) {
        CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];
        int fontPixelsH = cpu.gpr[5];
        int errorCodeAddr = cpu.gpr[6];

        float fontPixelsHF = (float)fontPixelsH;
		Modules.log.warn(String.format("PARTIAL: sceFontPixelToPointH libHandle=0x%08X, fontPixelsHF=%f , errorCodeAddr=0x%08X"
                , libHandle, fontPixelsHF, errorCodeAddr));

        // Convert horizontal pixels to floating points (Pixels Per Inch to Points Per Inch).
        // points = (pixels / dpiX) * 72.

        // Faking.
        // Assume dpiX = 100 and dpiY = 100.
        int points = Float.floatToRawIntBits((fontPixelsHF / 100) * 72);

		cpu.gpr[2] = points;
	}

    public void sceFontPixelToPointV(Processor processor) {
        CpuState cpu = processor.cpu;

        int libHandle = cpu.gpr[4];
        int fontPixelsV = cpu.gpr[5];
        int errorCodeAddr = cpu.gpr[6];

        float fontPixelsVF = (float)fontPixelsV;
		Modules.log.warn(String.format("PARTIAL: sceFontPixelToPointV libHandle=0x%08X, fontPixelsVF=%f , errorCodeAddr=0x%08X"
                , libHandle, fontPixelsVF, errorCodeAddr));

        // Convert vertical pixels to floating points (Pixels Per Inch to Points Per Inch).
        // points = (pixels / dpiX) * 72.

        // Faking.
        // Assume dpiX = 100 and dpiY = 100.
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