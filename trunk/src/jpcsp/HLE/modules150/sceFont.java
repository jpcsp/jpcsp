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

import java.util.HashMap;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import jpcsp.Emulator;
import jpcsp.format.PGF;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.ThreadMan;
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
		}
	}

    private HashMap<Integer, FontLib> fontLibMap;
    private HashMap<Integer, PGF> PGFFilesMap;
	private int fontLibCount;
	private int currentFontHandle;
	private int currentExternalFontHandle;
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

            if(allocFuncAddr != 0) {
                libBufAddr = triggerAllocCallback(unk1, 512);
                Modules.log.info("FontLib's allocation callback (size=512, numFonts="
                        + numFonts + ") returned 0x"
                        + Integer.toHexString(libBufAddr));
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

        private int triggerAllocCallback(int a0, int a1) {
            CpuState cpu = Emulator.getProcessor().cpu;
            ThreadMan threadMan = ThreadMan.getInstance();

            cpu.gpr[4] = a0;
            cpu.gpr[5] = a1;

            threadMan.executeCallback(allocFuncAddr, null);

            return (cpu.gpr[2]);
        }

        private void triggerFreeCallback(int a0, int a1) {
            CpuState cpu = Emulator.getProcessor().cpu;
            ThreadMan threadMan = ThreadMan.getInstance();

            cpu.gpr[4] = a0;
            cpu.gpr[5] = a1;

            threadMan.executeCallback(freeFuncAddr, null);
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

		Modules.log.warn(String.format("Unimplemented sceFontOpenUserFile libHandle=0x%08X, fileName=%s, unknown=0x%08X, errorCodeAddr=0x%08X",
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

		Modules.log.warn(String.format("Unimplemented sceFontOpenUserMemory libHandle=0x%08X, memoryFontAddr=0x%08X, memoryFontLength=%d, errorCodeAddr=0x%08X",
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
		Modules.log.warn(String.format("Unimplemented sceFontGetFontInfo fontAddr=0x%08X, fontInfoAddr=0x%08X"
                , fontAddr, fontInfoAddr));

		if (fontAddr != currentFontHandle) {
			Modules.log.warn("Unknown fontAddr: 0x" + Integer.toHexString(fontAddr));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(fontInfoAddr)) {
                PGF currentPGF = PGFFilesMap.get(fontAddr);
				// Maximal structure length is 264, but might be shorter.
				float unknownFloatValue = 100.f;
				short unknownShortValue = 100;
                float unknownSffValue = 100 / 64.0f;

                // Glyph metrics (in 26.6 signed fixed float).
                mem.write32(fontInfoAddr + 0, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(fontInfoAddr + 4, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(fontInfoAddr + 8, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(fontInfoAddr + 12, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(fontInfoAddr + 16, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(fontInfoAddr + 20, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(fontInfoAddr + 24, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(fontInfoAddr + 28, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(fontInfoAddr + 32, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(fontInfoAddr + 36, Float.floatToRawIntBits(unknownSffValue));

                // Glyph metrics (replicated as float?).
                mem.write32(fontInfoAddr + 40, Float.floatToRawIntBits(unknownFloatValue));
                mem.write32(fontInfoAddr + 44, Float.floatToRawIntBits(unknownFloatValue));
                mem.write32(fontInfoAddr + 48, Float.floatToRawIntBits(unknownFloatValue));
                mem.write32(fontInfoAddr + 52, Float.floatToRawIntBits(unknownFloatValue));
                mem.write32(fontInfoAddr + 56, Float.floatToRawIntBits(unknownFloatValue));
                mem.write32(fontInfoAddr + 60, Float.floatToRawIntBits(unknownFloatValue));
                mem.write32(fontInfoAddr + 64, Float.floatToRawIntBits(unknownFloatValue));
                mem.write32(fontInfoAddr + 68, Float.floatToRawIntBits(unknownFloatValue));
                mem.write32(fontInfoAddr + 72, Float.floatToRawIntBits(unknownFloatValue));
                mem.write32(fontInfoAddr + 76, Float.floatToRawIntBits(unknownFloatValue));

                // Bitmap dimensions.
                mem.write16(fontInfoAddr + 80, unknownShortValue);
                mem.write16(fontInfoAddr + 82, unknownShortValue);

                mem.write32(fontInfoAddr + 84, currentPGF.getCharMapLenght()); // Number of elements in the font's charmap.
                mem.write32(fontInfoAddr + 88, currentPGF.getShadowMapLenght());   // Number of elements in the font's shadow charmap.

                // Unknown. Seem to be font size related (float).
                mem.write32(fontInfoAddr + 92, Float.floatToRawIntBits(0.0f));
                mem.write32(fontInfoAddr + 96, Float.floatToRawIntBits(0.0f));
                mem.write32(fontInfoAddr + 100, Float.floatToRawIntBits(0.0f));
                mem.write32(fontInfoAddr + 104, Float.floatToRawIntBits(0.0f));
                mem.write32(fontInfoAddr + 108, Float.floatToRawIntBits(0.0f));

                mem.write16(fontInfoAddr + 112, (short)0); // Unknown.
                mem.write16(fontInfoAddr + 114, (short)0);  // Font style as in the font file (0 = default/system).
                mem.write16(fontInfoAddr + 116, (short)0);  // Unknown.
                mem.write16(fontInfoAddr + 118, (short)0);  // Unknown.
                mem.write16(fontInfoAddr + 120, (short)0);  // Unknown.
                mem.write16(fontInfoAddr + 122, (short)0);  // Unknown.

                Utilities.writeStringNZ(mem, fontInfoAddr + 124, 64, currentPGF.getFontName());  // Font name (maximum size is 64).
                Utilities.writeStringNZ(mem, fontInfoAddr + 188, 64, currentPGF.getFileNamez());   // File name (maximum size is 64).

                mem.write32(fontInfoAddr + 252, 0); // Unknown.
                mem.write32(fontInfoAddr + 256, 0); // Unknown.
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

		// Accept fontAddr == NULL as long as we have no real font list
		if (fontAddr != currentFontHandle && fontAddr != 0) {
			Modules.log.warn("Unknown fontAddr: 0x" + Integer.toHexString(fontAddr));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(charInfoAddr)) {
                float unknownSffValue = 100 / 64.0f;
				// Write dummy charInfo data
				mem.write32(charInfoAddr +  0, Debug.Font.charWidth);	// bitmapWidth
				mem.write32(charInfoAddr +  4, Debug.Font.charHeight);	// bitmapHeight
				mem.write32(charInfoAddr +  8, 0);	// bitmapLeft
				mem.write32(charInfoAddr + 12, 0);	// bitmapRight

                // Glyph metrics (in 26.6 signed fixed float).
                mem.write32(charInfoAddr + 0, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(charInfoAddr + 4, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(charInfoAddr + 8, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(charInfoAddr + 12, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(charInfoAddr + 16, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(charInfoAddr + 20, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(charInfoAddr + 24, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(charInfoAddr + 28, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(charInfoAddr + 32, Float.floatToRawIntBits(unknownSffValue));
                mem.write32(charInfoAddr + 36, Float.floatToRawIntBits(unknownSffValue));

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

		// Accept fontAddr == NULL as long as we have no real font list
		if (fontAddr != currentFontHandle && fontAddr != 0) {
			Modules.log.warn("Unknown fontAddr: 0x" + Integer.toHexString(fontAddr));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(glyphImageAddr)) {
                // sceFontGetCharGlyphImage is supposed to write the glyph's
                // data. It uses the PGF file for this.

				// Write GlyphImage data
                mem.write32(glyphImageAddr, 0);      // Pixel format
                mem.write32(glyphImageAddr + 4, 0);  // xPos64
                mem.write32(glyphImageAddr + 8, 0);  // yPos64
                mem.write16(glyphImageAddr + 12, (short)0); // Buffer's width
                mem.write16(glyphImageAddr + 14, (short)0); // Buffer's height
                mem.write16(glyphImageAddr + 16, (short)0); // Bytes per line
                mem.write16(glyphImageAddr + 18, (short)0); // Padding?
                mem.write32(glyphImageAddr + 20, 0); // Buffer's address
            }

            // Faking.
            // TODO: PGF file parsing.

			cpu.gpr[2] = 0;
		}
	}

	public void sceFontFindOptimumFont(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontFindOptimumFont 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontClose(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontClose 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontDoneLib(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontDoneLib 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontOpen(Processor processor) {
		CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int libHandle = cpu.gpr[4];
        int index = cpu.gpr[5];
        int unk = cpu.gpr[6];  // Mode (file/memory)?
        int errorCodeAddr = cpu.gpr[7];

        Modules.log.warn(String.format("PARTIAL: sceFontOpen libHandle=0x%08X, index=%d, unk=%d, errorCodeAddr=0x%08X"
                , libHandle, index, unk, errorCodeAddr));

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

		Modules.log.warn(String.format("Unimplemented sceFontGetCharGlyphImage_Clip 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontGetNumFontList(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Memory.getInstance();

		int libHandle = cpu.gpr[4];
		int errorCodeAddr = cpu.gpr[5];

		Modules.log.warn(String.format("Unimplemented sceFontGetNumFontList libHandle=0x%08X, errorCodeAddr=0x%08X", libHandle, errorCodeAddr));

		if (!fontLibMap.containsKey(libHandle)) {
			Modules.log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(errorCodeAddr)) {
				mem.write32(errorCodeAddr, 0);
			}

			cpu.gpr[2] = 0;
		}
	}

	public void sceFontGetFontList(Processor processor) {
		CpuState cpu = processor.cpu;

		int libHandle = cpu.gpr[4];
		int fontListAddr = cpu.gpr[5];	// points to 168 bytes per font entry (i.e. numFont * 168 bytes)
		int numFonts = cpu.gpr[6];	// Value returned by sceFontGetNumFontList

		/*
		 * FontList entry: 168 bytes per entry
		 *	offset+4: float value
		 *	offset+22: u16 value
		 *	offset+32..168?: stringZ (font name?)
		 */

		Modules.log.warn(String.format("Unimplemented sceFontGetFontList libHandle=0x%08X, fontListAddr=0x%08X, numFonts=%d", libHandle, fontListAddr, numFonts));

		if (!fontLibMap.containsKey(libHandle)) {
			Modules.log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
			cpu.gpr[2] = -1;
		} else {
			cpu.gpr[2] = 0;
		}
	}

	public void sceFontSetAltCharacterCode(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontSetAltCharacterCode 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontGetCharImageRect(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontGetCharImageRect 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontPointToPixelH(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontPointToPixelH 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontGetFontInfoByIndexNumber(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontGetFontInfoByIndexNumber 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontSetResolution(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontSetResolution 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontFlush(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontFlush 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontFindFont(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontFindFont 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
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
}