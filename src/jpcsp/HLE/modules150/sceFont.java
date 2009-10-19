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

import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
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

	private static final int fontLibHandle = 0x12345678;	// Dummy value
	private static final int fontHandle    = 0x11223344;	// Dummy value
	public static final int PGF_MAGIC = 'P' << 24 | 'G' << 16 | 'F' << 8 | '0';

	public void sceFontNewLib(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Memory.getInstance();

		int paramsAddr = cpu.gpr[4];
		/* paramsAddr points to a block of 44 bytes:
			+0: 0
			+4: Values: 1, 4
			+8: 0
			+12: function allocating memory (e.g. calling sceKernelTryAllocateVpl)
			+16: function freeing memory (e.g. calling sceKernelFreeVpl)
			+20: function calling sceIoOpen (might be 0)
			+24: function calling sceIoClose (might be 0)
			+28: function calling sceIoRead (might be 0)
			+32: function calling sceIoLseek (might be 0)
			+36: 0
			+40: 0
		*/
		int errorCodeAddr = cpu.gpr[5];
		Modules.log.warn(String.format("PARTIAL: sceFontNewLib paramsAddr=0x%08X, errorCodeAddr=0x%08X", paramsAddr, errorCodeAddr));

		if (mem.isAddressGood(paramsAddr)) {
			if (Modules.log.isInfoEnabled()) {
				for (int i = 0; i < 44; i += 4) {
					Modules.log.info(String.format("         paramsAddr+%2d=0x%08X", i, mem.read32(paramsAddr + i)));
				}
			}
		}

		if (mem.isAddressGood(errorCodeAddr)) {
			mem.write32(errorCodeAddr, 0);
		}

		cpu.gpr[2] = fontLibHandle;
	}

	public void sceFontOpenUserFile(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Memory.getInstance();

		int libHandle = cpu.gpr[4];
		int fileNameAddr = cpu.gpr[5];
		String fileName = Utilities.readStringZ(fileNameAddr);
		int unknown = cpu.gpr[6];	// Values: 1
		int errorCodeAddr = cpu.gpr[7];

		Modules.log.warn(String.format("Unimplemented sceFontOpenUserFile libHandle=0x%08X, fileName=%s, unknown=0x%08X, errorCodeAddr=0x%08X", libHandle, fileName, unknown, errorCodeAddr));

		if (libHandle != fontLibHandle) {
			Modules.log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(errorCodeAddr)) {
				mem.write32(errorCodeAddr, 0);
			}

			cpu.gpr[2] = fontHandle;
		}
	}

	public void sceFontOpenUserMemory(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Memory.getInstance();

		int libHandle = cpu.gpr[4];
		int memoryFontAddr = cpu.gpr[5];
		int memoryFontLength = cpu.gpr[6];
		int errorCodeAddr = cpu.gpr[7];

		Modules.log.warn(String.format("Unimplemented sceFontOpenUserMemory libHandle=0x%08X, memoryFontAddr=0x%08X, memoryFontLength=%d, errorCodeAddr=0x%08X", libHandle, memoryFontAddr, memoryFontLength, errorCodeAddr));

		if (libHandle != fontLibHandle) {
			Modules.log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(memoryFontAddr)) {
				int magic = mem.read32(memoryFontAddr + 4);
				if (magic == PGF_MAGIC) {
					Modules.log.info("sceFontOpenUserMemory: PGF format detected");
				}
			}
			if (mem.isAddressGood(errorCodeAddr)) {
				mem.write32(errorCodeAddr, 0);
			}

			cpu.gpr[2] = fontHandle;
		}
	}

	public void sceFontGetFontInfo(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int fontAddr = cpu.gpr[4];
		int fontInfoAddr = cpu.gpr[6];
		Modules.log.warn(String.format("Unimplemented sceFontGetFontInfo fontAddr=0x%08X, fontInfoAddr=0x%08X", fontAddr, fontInfoAddr));

		if (fontAddr != fontHandle) {
			Modules.log.warn("Unknown fontAddr: 0x" + Integer.toHexString(fontAddr));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(fontInfoAddr)) {
				// Maximal structure length is 264, but might be shorter.
				float unknownFloatValue = 1234.f;
				short unknownShortValue = 1234;
				mem.write32(fontInfoAddr + 48, Float.floatToRawIntBits(unknownFloatValue));
				mem.write32(fontInfoAddr + 56, Float.floatToRawIntBits(unknownFloatValue));
				mem.write32(fontInfoAddr + 60, Float.floatToRawIntBits(unknownFloatValue));
				mem.write32(fontInfoAddr + 64, Float.floatToRawIntBits(unknownFloatValue));
				mem.write32(fontInfoAddr + 68, Float.floatToRawIntBits(unknownFloatValue));
				mem.write16(fontInfoAddr + 80, unknownShortValue);
				mem.write16(fontInfoAddr + 82, unknownShortValue);
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
		Modules.log.warn(String.format("PARTIAL: sceFontGetCharInfo fontAddr=0x%08X, charCode=%04X (%c), charInfoAddr=%08X", fontAddr, charCode, (charCode <= 0xFF ? (char) charCode : '?'), charInfoAddr));

		if (fontAddr != fontHandle) {
			Modules.log.warn("Unknown fontAddr: 0x" + Integer.toHexString(fontAddr));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(charInfoAddr)) {
				// Write dummy charInfo data
				mem.write32(charInfoAddr +  0, Debug.Font.charWidth);	// bitmapWidth
				mem.write32(charInfoAddr +  4, Debug.Font.charHeight);	// bitmapHeight
				mem.write32(charInfoAddr +  8, 0);	// bitmapLeft
				mem.write32(charInfoAddr + 12, 0);	// bitmapRight
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
		Modules.log.warn(String.format("PARTIAL: sceFontGetCharGlyphImage fontAddr=0x%08X, charCode=%04X (%c), charInfoAddr=%08X", fontAddr, charCode, (charCode <= 0xFF ? (char) charCode : '?'), glyphImageAddr));

		if (fontAddr != fontHandle) {
			Modules.log.warn("Unknown fontAddr: 0x" + Integer.toHexString(fontAddr));
			cpu.gpr[2] = -1;
		} else {
			if (mem.isAddressGood(glyphImageAddr)) {
				// Read GlyphImage data
				int pixelFormat  = mem.read32(glyphImageAddr +  0);
				int xPos64       = mem.read32(glyphImageAddr +  4);
				int yPos64       = mem.read32(glyphImageAddr +  8);
				int bytesPerLine = mem.read16(glyphImageAddr + 16);
				int buffer       = mem.read32(glyphImageAddr + 20);
				int xPos = (xPos64 >> 6);
				int yPos = (yPos64 >> 6);
				Modules.log.info("sceFontGetCharGlyphImage c=" + ((char) charCode) + ", xPos=" + xPos + ", yPos=" + yPos + ", buffer=0x" + Integer.toHexString(buffer) + ", pixelFormat=" + pixelFormat);

				int targetAddr = buffer + xPos / 2 + yPos * bytesPerLine;

				//
				// The font bitmap has to be stored in a pixel buffer
				// using the following format:
				//  - upper left pixel starting at targetAddr
				//  - one line is using "bytesPerLine" bytes in the buffer
				//	- one pixel is coded in 4 bits:
				//		0x0 = white (i.e. no dot on a white paper)
				//		0xF = black (i.e. a full dot)
				//		0x1-0xE = intermediate values are grey
				//
				// Use the debug font, just to "see" something,
				// as long as we have no PGF file reading.
				//
				char[] font = Debug.Font.font;
				int fontBaseIndex = (charCode & 0xFF) * 8;
				for (int y = 0; y < Debug.Font.charHeight; y++, targetAddr += bytesPerLine) {
					int x = (xPos64 >> 5) & 1;	// might start at a half-byte pixel
					for (int w = 0; w < Debug.Font.charWidth; w++, x++) {
						int pixel = font[fontBaseIndex + y] & (128 >> w);
						int pixelValue;
						if (pixel != 0) {
							pixelValue = 0xF;	// black
						} else {
							pixelValue = 0x0;	// white
						}

						int pixelAddr = targetAddr + x / 2;
						int previousValue = mem.read8(pixelAddr);
						int newValue;
						if ((x & 1) == 0) {
							newValue = (previousValue & 0xF0) | (pixelValue     );
						} else {
							newValue = (previousValue & 0x0F) | (pixelValue << 4);
						}

						if (previousValue != newValue) {
							mem.write8(pixelAddr, (byte) newValue);
						}
					}
				}
			}

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

		Modules.log.warn(String.format("Unimplemented sceFontOpen 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontGetCharGlyphImage_Clip(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontGetCharGlyphImage_Clip 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontGetNumFontList(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontGetNumFontList 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
	}

	public void sceFontGetFontList(Processor processor) {
		CpuState cpu = processor.cpu;

		Modules.log.warn(String.format("Unimplemented sceFontGetFontList 0x%08X, 0x%08X, 0x%08X", cpu.gpr[4], cpu.gpr[5], cpu.gpr[6]));
		cpu.gpr[2] = 0;
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
