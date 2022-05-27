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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_FONT_INVALID_PARAMETER;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_FAMILY_SANS_SERIF;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_FAMILY_SERIF;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_LANGUAGE_CHINESE;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_LANGUAGE_JAPANESE;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_LANGUAGE_KOREAN;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_LANGUAGE_LATIN;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_STYLE_BOLD;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_STYLE_BOLD_ITALIC;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_STYLE_DB;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_STYLE_ITALIC;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_STYLE_REGULAR;
import static jpcsp.graphics.VideoEngineUtilities.getPixelFormatBytes;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUidClass;
import jpcsp.HLE.HLEUidObjectMapping;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TErrorPointer32;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;
import jpcsp.HLE.TPointer32;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceFontInfo;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspCharInfo;
import jpcsp.HLE.kernel.types.pspFontStyle;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.format.BWFont;
import jpcsp.format.PGF;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.capture.CaptureImage;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.Debug;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

//
// The stackUsage values are based on tests performed using JpcspTrace
//
public class sceFont extends HLEModule {
    public static Logger log = Modules.getLogger("sceFont");
    private static final boolean dumpUserFont = false;

	private class UseDebugFontSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setUseDebugFont(value);
		}
	}

	@Override
	public int getMemoryUsage() {
		return 0x7D00;
	}

    @Override
    public void start() {
    	setSettingsListener("emu.useDebugFont", new UseDebugFontSettingsListerner());

        internalFonts = new LinkedList<Font>();
        fontLibsMap = new HashMap<Integer, FontLib>();
        fontsMap = new HashMap<Integer, Font>();
        loadFontRegistry();
        loadDefaultSystemFont();

        super.start();
    }

    @HLEUidClass(errorValueOnNotFound = SceKernelErrors.ERROR_FONT_INVALID_LIBID)
    public class Font {
        public PGF pgf;
        public SceFontInfo fontInfo;
        public FontLib fontLib;
        private final int handle;
        private int fontFileSize;
        private int openAllocMemSizeForMode0;
        public int maxGlyphBaseYI;
        public int maxBitmapWidth;
        public int maxBitmapHeight;

        public Font(PGF pgf, SceFontInfo fontInfo, int fontFileSize) {
            this.pgf = pgf;
            this.fontInfo = fontInfo;
            fontLib = null;
            handle = 0;
            this.fontFileSize = fontFileSize;
            openAllocMemSizeForMode0 = 0;
            if (pgf != null) {
	            maxGlyphBaseYI = pgf.getMaxBaseYAdjust();
	            maxBitmapWidth = pgf.getMaxGlyphWidth();
	            maxBitmapHeight = pgf.getMaxGlyphHeight();
            }
        }

        public Font(Font font, FontLib fontLib, int handle) {
            pgf = font.pgf;
            fontInfo = font.fontInfo;
            this.fontLib = fontLib;
            this.handle = handle;
            fontFileSize = font.fontFileSize;
            openAllocMemSizeForMode0 = font.openAllocMemSizeForMode0;
            maxGlyphBaseYI = font.maxGlyphBaseYI;
            maxBitmapWidth = font.maxBitmapWidth;
            maxBitmapHeight = font.maxBitmapHeight;
        }

        public pspFontStyle getFontStyle() {
            pspFontStyle fontStyle = fontInfo.getFontStyle();
            if (fontStyle == null) {
                fontStyle = new pspFontStyle();
                fontStyle.fontH = pgf.getHSize() / 64.f;
                fontStyle.fontV = pgf.getVSize() / 64.f;
                fontStyle.fontHRes = pgf.getHResolution() / 64.f;
                fontStyle.fontVRes = pgf.getVResolution() / 64.f;
                fontStyle.fontStyle = sceFont.getFontStyle(pgf.getFontType());
                fontStyle.fontName = pgf.getFontName();
                fontStyle.fontFileName = pgf.getFileNamez();
            }

            return fontStyle;
        }
        
        public int getHandle() {
            return handle;
        }

        public boolean isClosed() {
        	return fontLib == null;
        }

        public void close() {
            fontLib = null;
            // Keep PGF and SceFontInfo information.
            // A call to sceFontGetFontInfo is allowed on a closed font.
        }

        @Override
        public String toString() {
        	if (isClosed()) {
        		return String.format("Font[handle=0x%X closed]", getHandle());
        	}
            return String.format("Font[handle=0x%X, '%s' - '%s']", getHandle(), pgf.getFileNamez(), pgf.getFontName());
        }
    }

    public static class FontRegistryEntry {
        public int h_size;
        public int v_size;
        public int h_resolution;
        public int v_resolution;
        public int extra_attributes;
        public int weight;
        public int family_code;
        public int style;
        public int sub_style;
        public int language_code;
        public int region_code;
        public int country_code;
        public String file_name;
        public String font_name;
        public int expire_date;
        public int shadow_option;
        public int fontFileSize;
        public int openAllocMemSizeForMode0;
        public int maxGlyphBaseYI;
        public int maxBitmapWidth;
        public int maxBitmapHeight;

        public FontRegistryEntry(int h_size, int v_size, int h_resolution,
                int v_resolution, int extra_attributes, int weight,
                int family_code, int style, int sub_style, int language_code,
                int region_code, int country_code, String file_name,
                String font_name, int expire_date, int shadow_option,
                int fontFileSize, int openAllocMemSizeForMode0,
                int maxGlyphBaseYI, int maxBitmapWidth, int maxBitmapHeight) {
            this.h_size = h_size;
            this.v_size = v_size;
            this.h_resolution = h_resolution;
            this.v_resolution = v_resolution;
            this.extra_attributes = extra_attributes;
            this.weight = weight;
            this.family_code = family_code;
            this.style = style;
            this.sub_style = sub_style;
            this.language_code = language_code;
            this.region_code = region_code;
            this.country_code = country_code;
            this.file_name = file_name;
            this.font_name = font_name;
            this.expire_date = expire_date;
            this.shadow_option = shadow_option;
            this.fontFileSize = fontFileSize;
            this.openAllocMemSizeForMode0 = openAllocMemSizeForMode0;
            this.maxGlyphBaseYI = maxGlyphBaseYI;
            this.maxBitmapWidth = maxBitmapWidth;
            this.maxBitmapHeight = maxBitmapHeight;
        }

        public FontRegistryEntry() {
        }
    }
    public static final int PGF_MAGIC = 'P' << 24 | 'G' << 16 | 'F' << 8 | '0';
    public static final String customFontFile = "debug.jpft";
    public static final int PSP_FONT_PIXELFORMAT_4 = 0; // 2 pixels packed in 1 byte (natural order)
    public static final int PSP_FONT_PIXELFORMAT_4_REV = 1; // 2 pixels packed in 1 byte (reversed order)
    public static final int PSP_FONT_PIXELFORMAT_8 = 2; // 1 pixel in 1 byte
    public static final int PSP_FONT_PIXELFORMAT_24 = 3; // 1 pixel in 3 bytes (RGB)
    public static final int PSP_FONT_PIXELFORMAT_32 = 4; // 1 pixel in 4 bytes (RGBA)
    public static final int PSP_FONT_MODE_FILE = 0;
    public static final int PSP_FONT_MODE_MEMORY = 1;
    private boolean useDebugFont = false;
    private static final boolean dumpFonts = false;
    private List<Font> internalFonts;
    private HashMap<Integer, FontLib> fontLibsMap;
    private HashMap<Integer, Font> fontsMap;
    protected String uidPurpose = "sceFont";
    private String fontDirPath = "flash0:/font";
    private List<FontRegistryEntry> fontRegistry;
    protected static final float pointDPI = 72.f;

    protected boolean getUseDebugFont() {
        return useDebugFont;
    }

    private void setUseDebugFont(boolean status) {
        useDebugFont = status;
    }

    public List<FontRegistryEntry> getFontRegistry() {
    	return fontRegistry;
    }

    public String getFontDirPath() {
    	return fontDirPath;
    }

    public void setFontDirPath(String fontDirPath) {
    	this.fontDirPath = fontDirPath;
    }

    protected void loadFontRegistry() {
        fontRegistry = new LinkedList<FontRegistryEntry>();
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_DB, 0, FONT_LANGUAGE_JAPANESE, 0, 1, "jpn0.pgf", "FTT-NewRodin Pro DB", 0, 0, 1581700, 145844, 0x4B4, 19, 20));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_REGULAR, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn0.pgf", "FTT-NewRodin Pro Latin", 0, 0, 69108, 16680, 0x4B2, 23, 20));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_REGULAR, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn1.pgf", "FTT-Matisse Pro Latin", 0, 0, 65124, 16920, 0x482, 23, 20));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn2.pgf", "FTT-NewRodin Pro Latin", 0, 0, 72948, 16872, 0x4B2, 25, 20));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn3.pgf", "FTT-Matisse Pro Latin", 0, 0, 67700, 17112, 0x482, 25, 20));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_BOLD, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn4.pgf", "FTT-NewRodin Pro Latin", 0, 0, 72828, 16648, 0x4F7, 24, 21));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_BOLD, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn5.pgf", "FTT-Matisse Pro Latin", 0, 0, 68220, 16928, 0x49C, 24, 20));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_BOLD_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn6.pgf", "FTT-NewRodin Pro Latin", 0, 0, 77032, 16792, 0x4F7, 27, 21));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_BOLD_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn7.pgf", "FTT-Matisse Pro Latin", 0, 0, 71144, 17160, 0x49C, 27, 20));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_REGULAR, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn8.pgf", "FTT-NewRodin Pro Latin", 0, 0, 41000, 16192, 0x321, 16, 14));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_REGULAR, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn9.pgf", "FTT-Matisse Pro Latin", 0, 0, 40164, 16476, 0x302, 16, 14));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn10.pgf", "FTT-NewRodin Pro Latin", 0, 0, 42692, 16300, 0x321, 17, 14));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn11.pgf", "FTT-Matisse Pro Latin", 0, 0, 41488, 16656, 0x302, 17, 14));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_BOLD, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn12.pgf", "FTT-NewRodin Pro Latin", 0, 0, 43136, 16176, 0x34F, 17, 15));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_BOLD, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn13.pgf", "FTT-Matisse Pro Latin", 0, 0, 41772, 16436, 0x312, 17, 14));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_BOLD_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn14.pgf", "FTT-NewRodin Pro Latin", 0, 0, 45184, 16272, 0x34F, 18, 15));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_BOLD_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn15.pgf", "FTT-Matisse Pro Latin", 0, 0, 43044, 16704, 0x312, 18, 14));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_REGULAR, 0, FONT_LANGUAGE_KOREAN, 0, 3, "kr0.pgf", "AsiaNHH(512Johab)", 0, 0, 394192, 51856, 0x3CB, 21, 20));

        // Add the Chinese fixed font file if it is present, i.e. if copied from a real PSP flash0:/font/gb3s1518.bwfon
        if (Modules.IoFileMgrForUserModule.statFile(fontDirPath + "/gb3s1518.bwfon") != null) {
        	fontRegistry.add(new FontRegistryEntry(BWFont.charBitmapWidth << 6, BWFont.charBitmapHeight << 6, 0, 0, 0, 0, 0, FONT_STYLE_REGULAR, 0, FONT_LANGUAGE_CHINESE, 0, 0, "gb3s1518.bwfon", "gb3s1518", 0, 0, 1023372, 0, 0, 0, 0));
        }
    }

    protected void loadDefaultSystemFont() {
        try {
            SeekableDataInput fontFile = Modules.IoFileMgrForUserModule.getFile(fontDirPath + "/" + customFontFile, IoFileMgrForUser.PSP_O_RDONLY);
            if (fontFile != null) {
                fontFile.skipBytes(32);  // Skip custom header.
                byte[] c = new byte[(int) fontFile.length() - 32];
                fontFile.readFully(c);
                fontFile.close();
                Debug.Font.setDebugFont(c); // Set the internal debug font.
                Debug.Font.setDebugCharSize(8);
                Debug.Font.setDebugCharHeight(8);
                Debug.Font.setDebugCharWidth(8);
            }
        } catch (IOException e) {
            // The file was removed from flash0.
            log.error(e);
        }
    }

    /**
     * Dump a font as a .BMP image in the tmp directory for debugging purpose.
     *
     * @param font the font to be dumped
     */
    protected void dumpFont(Font font) {
        int addr = MemoryMap.START_VRAM;
        int fontPixelFormat = PSP_FONT_PIXELFORMAT_32;
        int bufferStorage = GeCommands.TPSM_PIXEL_STORAGE_MODE_32BIT_ABGR8888;
        int bufferWidth = 800;
        int fontBufWidth = bufferWidth;
        int fontBpl = bufferWidth * getPixelFormatBytes(bufferStorage);
        int fontBufHeight = MemoryMap.SIZE_VRAM / fontBpl;
        SceFontInfo fontInfo = font.fontInfo;
        PGF pgf = font.pgf;

        int memoryLength = fontBpl * fontBufHeight * getPixelFormatBytes(bufferStorage);
        Memory mem = Memory.getInstance();
        mem.memset(addr, (byte) 0, memoryLength);

        Buffer memoryBuffer = Memory.getInstance().getBuffer(addr, memoryLength);
        String fileNamePrefix = String.format("Font-%s-", pgf.getFileNamez());

        int maxGlyphWidth = pgf.getMaxSize()[0] >> 6;
        int maxGlyphHeight = pgf.getMaxSize()[1] >> 6;
        int level = 0;
        int x = 0;
        int y = 0;
        int firstCharCode = pgf.getFirstGlyphInCharMap();
        int lastCharCode = pgf.getLastGlyphInCharMap();
        for (int charCode = firstCharCode; charCode <= lastCharCode; charCode++) {
        	if (x == 0) {
        		String linePrefix = String.format("0x%04X: ", charCode);
                Debug.printFramebuffer(addr, fontBufWidth, x, y, 0xFFFFFFFF, 0x00000000, bufferStorage, linePrefix);
                x += linePrefix.length() * jpcsp.util.Debug.Font.charWidth;
        	}

        	fontInfo.printFont(addr, fontBpl, fontBufWidth, fontBufHeight, x, y, 0, 0, 0, 0, fontBufWidth, fontBufHeight, fontPixelFormat, charCode, ' ', SceFontInfo.FONT_PGF_GLYPH_TYPE_CHAR, true);

            x += maxGlyphWidth;
            if (x + maxGlyphWidth > fontBufWidth) {
                x = 0;
                y += maxGlyphHeight;
                if (y + maxGlyphHeight > fontBufHeight) {
                    CaptureImage image = new CaptureImage(addr, level, memoryBuffer, fontBufWidth, fontBufHeight, bufferWidth, bufferStorage, false, 0, false, true, fileNamePrefix);
                	log.info(String.format("Dumping font %s from charCode 0x%04X to file %s", pgf.getFontName(), firstCharCode, image.getFileName()));
                    try {
                        image.write();
                    } catch (IOException e) {
                        log.error(e);
                    }
                    mem.memset(addr, (byte) 0, memoryLength);
                    level++;
                    firstCharCode = charCode + 1;
                    x = 0;
                    y = 0;
                }
            }
        }

        CaptureImage image = new CaptureImage(addr, level, memoryBuffer, fontBufWidth, fontBufHeight, bufferWidth, bufferStorage, false, 0, false, true, fileNamePrefix);
    	log.info(String.format("Dumping font %s from charCode 0x%04X to file %s", pgf.getFontName(), firstCharCode, image.getFileName()));
        try {
            image.write();
        } catch (IOException e) {
            log.error(e);
        }
    }

    protected Font openFontFile(ByteBuffer pgfBuffer, String fileName) {
        Font font = null;

        try {
        	PGF pgfFile;
        	if (fileName != null && fileName.endsWith(".bwfon")) {
        		pgfFile = new BWFont(pgfBuffer, fileName);
        	} else {
        		pgfFile = new PGF(pgfBuffer);
        	}

        	if (fileName != null) {
                pgfFile.setFileNamez(fileName);
            }

            SceFontInfo fontInfo = pgfFile.createFontInfo();

            font = new Font(pgfFile, fontInfo, pgfBuffer.capacity());

            if (dumpFonts) {
                dumpFont(font);
            }
        } catch (Exception e) {
            // Can't parse file.
            log.error("openFontFile", e);
        }

        return font;
    }

    protected Font openFontFile(String fileName) {
        Font font = null;

        try {
            SeekableDataInput fontFile = Modules.IoFileMgrForUserModule.getFile(fileName, IoFileMgrForUser.PSP_O_RDONLY);
            if (fontFile != null) {
	            byte[] pgfBytes = new byte[(int) fontFile.length()];
	            fontFile.readFully(pgfBytes);
	            fontFile.close();
	            ByteBuffer pgfBuffer = ByteBuffer.wrap(pgfBytes);

	            font = openFontFile(pgfBuffer, new File(fileName).getName());
            }
        } catch (IOException e) {
            // Can't open file.
            log.warn(e);
        }

        return font;
    }
    
    protected Font openFontFile(int addr, int length) {
    	if (dumpUserFont) {
			try {
				OutputStream os = new FileOutputStream(String.format("%suserFont-0x%08X.pgf", Settings.getInstance().getTmpDirectory(), addr));
	    		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, length, 1);
	    		for (int i = 0; i < length; i++) {
	    			os.write(memoryReader.readNext());
	    		}
				os.close();
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
    	}
        ByteBuffer pgfBuffer = ByteBuffer.allocate(length);
        Buffer memBuffer = Memory.getInstance().getBuffer(addr, length);
        Utilities.putBuffer(pgfBuffer, memBuffer, ByteOrder.LITTLE_ENDIAN, length);
        pgfBuffer.rewind();

        Font font = openFontFile(pgfBuffer, null);

        return font;
    }

    protected void setFontAttributesFromRegistry(Font font, FontRegistryEntry fontRegistryEntry) {
        pspFontStyle fontStyle = new pspFontStyle();
        fontStyle.fontH = fontRegistryEntry.h_size / 64.f;
        fontStyle.fontV = fontRegistryEntry.v_size / 64.f;
        fontStyle.fontHRes = fontRegistryEntry.h_resolution / 64.f;
        fontStyle.fontVRes = fontRegistryEntry.v_resolution / 64.f;
        fontStyle.fontWeight = fontRegistryEntry.weight;
        fontStyle.fontFamily = (short) fontRegistryEntry.family_code;
        fontStyle.fontStyle = (short) fontRegistryEntry.style;
        fontStyle.fontStyleSub = (short) fontRegistryEntry.sub_style;
        fontStyle.fontLanguage = (short) fontRegistryEntry.language_code;
        fontStyle.fontRegion = (short) fontRegistryEntry.region_code;
        fontStyle.fontCountry = (short) fontRegistryEntry.country_code;
        fontStyle.fontName = fontRegistryEntry.font_name;
        fontStyle.fontFileName = fontRegistryEntry.file_name;
        fontStyle.fontAttributes = fontRegistryEntry.extra_attributes;
        fontStyle.fontExpire = fontRegistryEntry.expire_date;

        font.fontInfo.setFontStyle(fontStyle);

        // The font file size is used during a sceOpenFont() call to allocate memory.
        // The Jpcsp font files differ significantly in size from the real PSP files.
        // So it is important to use the font file sizes from a real PSP, so that
        // the correct memory is being allocated.
        font.fontFileSize = fontRegistryEntry.fontFileSize;
        font.openAllocMemSizeForMode0 = fontRegistryEntry.openAllocMemSizeForMode0;

        // The following values are critical for some applications and need to match
        // the values from a real PSP font file.
        font.maxGlyphBaseYI = fontRegistryEntry.maxGlyphBaseYI;
        font.maxBitmapWidth = fontRegistryEntry.maxBitmapWidth;
        font.maxBitmapHeight = fontRegistryEntry.maxBitmapHeight;
    }

    protected void setFontAttributesFromRegistry(Font font) {
        for (FontRegistryEntry fontRegistryEntry : fontRegistry) {
            if (fontRegistryEntry.file_name.equals(font.pgf.getFileNamez())) {
                if (fontRegistryEntry.font_name.equals(font.pgf.getFontName())) {
                    setFontAttributesFromRegistry(font, fontRegistryEntry);
                    break;
                }
            }
        }
    }

    protected void loadAllFonts() {
        internalFonts.clear();

        // Load the fonts in the same order as on a PSP.
        // Some applications are always using the first font returned by
        // sceFontGetFontList.
        for (FontRegistryEntry fontRegistryEntry : fontRegistry) {
            String fontFileName = fontDirPath + "/" + fontRegistryEntry.file_name;
            SceIoStat stat = Modules.IoFileMgrForUserModule.statFile(fontFileName);
            if (stat != null) {
                Font font = openFontFile(fontFileName);
                if (font != null) {
                    setFontAttributesFromRegistry(font, fontRegistryEntry);
                    internalFonts.add(font);
                    log.info(String.format("Loading font file '%s'. Font='%s' Type='%s'", fontRegistryEntry.file_name, font.pgf.getFontName(), font.pgf.getFontType()));
                }
            }
        }
    }

    protected static short getFontStyle(String styleString) {
        if ("Regular".equals(styleString)) {
            return FONT_STYLE_REGULAR;
        }
        if ("Italic".equals(styleString)) {
            return FONT_STYLE_ITALIC;
        }
        if ("Bold".equals(styleString)) {
            return FONT_STYLE_BOLD;
        }
        if ("Bold Italic".equals(styleString)) {
            return FONT_STYLE_BOLD_ITALIC;
        }

        return 0;
    }

    @HLEUidClass(errorValueOnNotFound = SceKernelErrors.ERROR_FONT_INVALID_LIBID)
    protected class FontLib {
    	private static final int FONT_IS_CLOSED = 0;
    	private static final int FONT_IS_OPEN = 1;
        protected int userDataAddr;
        protected int numFonts;
        protected int cacheDataAddr;
        protected int allocFuncAddr;
        protected int freeFuncAddr;
        protected int openFuncAddr;
        protected int closeFuncAddr;
        protected int readFuncAddr;
        protected int seekFuncAddr;
        protected int errorFuncAddr;
        protected int ioFinishFuncAddr;
        protected int fileFontHandle;
        protected int altCharCode;
        protected float fontHRes = 128.f;
        protected float fontVRes = 128.f;
        protected int handle;
        protected int[] fonts;
        protected int[] allocatedSizes = new int[] { 0x4C, 0x130, 0x8C0, 0xC78 };
        protected int[] allocatedAddresses = new int[allocatedSizes.length];
        protected int allocatedAddressIndex;
        protected int[] openAllocatedAddresses;
        protected int charInfoBitmapAddress;

        public FontLib(TPointer32 params) {
            read(params);

            // On a PSP, FontLib handle and Font handles are addresses pointing to memory
            // allocated by the "Alloc" callback.
            // Here, we just fake theses addresses by allocating an area small enough to
            // provide different addresses for the required FontLib and Font handles.
            // E.g.
            //    addr     = FontLib handle
            //    addr + 4 = Font handle for 1st font
            //    addr + 8 = Font handle for 2nd font
            //    ...
            // Furthermore, the value stored at a Font handle address indicates if the
            // font is closed (e.g. free to be opened) or open.
            //    mem.read32(fontHandle) == FONT_IS_OPEN: font is already open
            //    mem.read32(fontHandle) == FONT_IS_CLOSED: font is not open
            //
            openAllocatedAddresses = new int[numFonts];

            allocateAddresses();
        }

        private void allocateAddresses() {
        	int minimumSize = numFonts * 4 + 4;
        	if (allocatedSizes[0] < minimumSize) {
        		allocatedSizes[0] = minimumSize;
        	}

        	allocatedAddressIndex = 0;
        	triggerAllocCallback(allocatedSizes[allocatedAddressIndex], new AfterCreateAllocCallback());
        }

        public int getNumFonts() {
            return numFonts;
        }

        private Font openFont(Font font, int mode, boolean needAllocForFontFile) {
            if (font == null) {
                throw (new SceKernelErrorException(SceKernelErrors.ERROR_FONT_INVALID_PARAMETER));
            }

            Memory mem = Memory.getInstance();
            int freeFontIndex = -1;
            // Search for a free font slot
            for (int i = 0; i < numFonts; i++) {
            	if (mem.read32(fonts[i]) == FONT_IS_CLOSED) {
            		freeFontIndex = i;
            		break;
            	}
            }
            if (freeFontIndex < 0) {
                throw (new SceKernelErrorException(SceKernelErrors.ERROR_FONT_TOO_MANY_OPEN_FONTS));
            }

            font = new Font(font, this, fonts[freeFontIndex]);
            mem.write32(fonts[freeFontIndex], FONT_IS_OPEN);
            fontsMap.put(font.getHandle(), font);

            int allocSize = 12;
            if (needAllocForFontFile) {
            	if (mode == 0) {
            		// mode == 0: only parts of the font file are read.
            		// For jpn0.pgf, the alloc callback is called multiple times:
            		//     0x7F8, 0x7F8, 0x7F8, 0x350, 0x3FC, 0x0, 0x0, 0x1BFC8, 0x5AB8
            		// in total: 0x239B4
            		// Use the alloc size of the specific font, taken from a real PSP.
            		allocSize = font.openAllocMemSizeForMode0;
            	} else if (mode == 1) {
            		// mode == 1: the whole font file is read in memory
            		allocSize += font.fontFileSize;
            	}
            }

            triggerAllocCallback(allocSize, new AfterOpenAllocCallback(freeFontIndex));

            return font;
        }

        private void closeFont(Font font) {
            HLEUidObjectMapping.removeObject(font);

            for (int i = 0; i < numFonts; i++) {
            	if (fonts[i] == font.getHandle()) {
            		Memory mem = Memory.getInstance();
            		mem.write32(fonts[i], FONT_IS_CLOSED);

            		if (openAllocatedAddresses[i] != 0) {
            			triggerFreeCallback(openAllocatedAddresses[i], null);
            			openAllocatedAddresses[i] = 0;
            		}
            		break;
            	}
            }

            flushFont(font);

            font.close();
        }

        public void flushFont(Font font) {
            if (charInfoBitmapAddress != 0) {
            	triggerFreeCallback(charInfoBitmapAddress, null);
            	charInfoBitmapAddress = 0;
            }
        }

        public void done() {
        	Memory mem = Memory.getInstance();
        	for (int i = 0; i < numFonts; i++) {
        		if (mem.read32(fonts[i]) == FONT_IS_OPEN) {
        			closeFont(fontsMap.get(fonts[i]));
        		}
        		fontsMap.remove(fonts[i]);
        	}
        	triggerFreeCallback(allocatedAddresses[--allocatedAddressIndex], new AfterFreeCallback());
        	fonts = null;
        }

        public int triggetGetCharInfo(pspCharInfo charInfo) {
        	int result = 0;

        	// The callbacks are not triggered by characters not present in the font
        	if (charInfo.sfp26AdvanceH != 0 || charInfo.sfp26AdvanceV != 0) {
        		SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();

	        	if (charInfoBitmapAddress != 0) {
	        		triggerFreeCallback(charInfoBitmapAddress, new AfterCharInfoFreeCallback(thread, charInfo));
	        	} else {
	        		triggerAllocCallback(Math.max(4, charInfo.bitmapWidth * charInfo.bitmapHeight), new AfterCharInfoAllocCallback(thread));
	        	}

	        	if (charInfoBitmapAddress == 0) {
	        		result = SceKernelErrors.ERROR_FONT_OUT_OF_MEMORY;
	        	}
        	}

        	return result;
        }

        public int getHandle() {
            return handle;
        }

        protected void triggerAllocCallback(int size, IAction afterAllocCallback) {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("triggerAllocCallback size=0x%X", size));
        	}
            Modules.ThreadManForUserModule.executeCallback(null, allocFuncAddr, afterAllocCallback, true, userDataAddr, size);
        }

        protected void triggerFreeCallback(int addr, IAction afterFreeCallback) {
            if (Memory.isAddressGood(addr)) {
            	if (log.isDebugEnabled()) {
            		log.debug(String.format("Calling free callback on 0x%08X", addr));
            	}
                Modules.ThreadManForUserModule.executeCallback(null, freeFuncAddr, afterFreeCallback, true, userDataAddr, addr);
            }
        }

        protected void triggerOpenCallback(int fileNameAddr, int errorCodeAddr) {
            Modules.ThreadManForUserModule.executeCallback(null, openFuncAddr, new AfterOpenCallback(), true, userDataAddr, fileNameAddr, errorCodeAddr);
        }

        protected void triggerCloseCallback() {
            if (fileFontHandle != 0) {
                Modules.ThreadManForUserModule.executeCallback(null, closeFuncAddr, null, true, userDataAddr, fileFontHandle);
            }
        }

        private void read(TPointer32 params) {
            userDataAddr = params.getValue(0);
            numFonts = params.getValue(4);
            cacheDataAddr = params.getValue(8);
            allocFuncAddr = params.getValue(12);
            freeFuncAddr = params.getValue(16);
            openFuncAddr = params.getValue(20);
            closeFuncAddr = params.getValue(24);
            readFuncAddr = params.getValue(28);
            seekFuncAddr = params.getValue(32);
            errorFuncAddr = params.getValue(36);
            ioFinishFuncAddr = params.getValue(40);

            if (log.isDebugEnabled()) {
            	log.debug(String.format("userDataAddr 0x%08X, numFonts=%d, cacheDataAddr=0x%08X, allocFuncAddr=0x%08X, freeFuncAddr=0x%08X, openFuncAddr=0x%08X, closeFuncAddr=0x%08X, readFuncAddr=0x%08X, seekFuncAddr=0x%08X, errorFuncAddr=0x%08X, ioFinishFuncAddr=0x%08X", userDataAddr, numFonts, cacheDataAddr, allocFuncAddr, freeFuncAddr, openFuncAddr, closeFuncAddr, readFuncAddr, seekFuncAddr, errorFuncAddr, ioFinishFuncAddr));
            }
        }

        public int getAltCharCode() {
            return altCharCode;
        }

        public void setAltCharCode(int altCharCode) {
            this.altCharCode = altCharCode;
        }

        public int getFontHandle(int index) {
        	return fonts[index];
        }

        private class AfterCreateAllocCallback implements IAction {
			@Override
            public void execute() {
                int allocatedAddr = Emulator.getProcessor().cpu._v0;

                if (log.isDebugEnabled()) {
                	log.debug(String.format("FontLib's allocation callback#%d returned 0x%08X for size 0x%X", allocatedAddressIndex, allocatedAddr, allocatedSizes[allocatedAddressIndex]));
                }

                if (allocatedAddressIndex == 0) {
	                int addr = allocatedAddr;
	                handle = addr;
	                addr += 4;
	                fonts = new int[numFonts];
	                Memory mem = Memory.getInstance();
	                for (int i = 0; i < numFonts; i++) {
	                	mem.write32(addr, FONT_IS_CLOSED);
	                	fonts[i] = addr;
	                	addr += 4;
	                }
                }

                allocatedAddresses[allocatedAddressIndex++] = allocatedAddr;
                if (allocatedAddressIndex < allocatedSizes.length) {
                	triggerAllocCallback(allocatedSizes[allocatedAddressIndex], this);
                }
            }
        }

        private class AfterFreeCallback implements IAction {
			@Override
			public void execute() {
				if (allocatedAddressIndex > 0) {
					triggerFreeCallback(allocatedAddresses[--allocatedAddressIndex], this);
				}
			}
        }

        private class AfterOpenAllocCallback implements IAction {
        	private int fontIndex;

        	public AfterOpenAllocCallback(int fontIndex) {
				this.fontIndex = fontIndex;
			}

			@Override
			public void execute() {
                int allocatedAddr = Emulator.getProcessor().cpu._v0;

                if (log.isDebugEnabled()) {
                	log.debug(String.format("FontLib's allocation callback on open#%d returned 0x%08X", fontIndex, allocatedAddr));
                }

                openAllocatedAddresses[fontIndex] = allocatedAddr;
			}
        }

        private class AfterOpenCallback implements IAction {
            @Override
            public void execute() {
                fileFontHandle = Emulator.getProcessor().cpu._v0;

                if (log.isDebugEnabled()) {
                	log.debug(String.format("FontLib's file open callback returned 0x%X", fileFontHandle));
                }
            }
        }

        private class AfterCharInfoFreeCallback implements IAction {
        	private SceKernelThreadInfo thread;
        	private pspCharInfo charInfo;

            public AfterCharInfoFreeCallback(SceKernelThreadInfo thread, pspCharInfo charInfo) {
            	this.thread = thread;
				this.charInfo = charInfo;
			}

			@Override
            public void execute() {
				charInfoBitmapAddress = 0;
            	triggerAllocCallback(Math.max(4, charInfo.bitmapWidth * charInfo.bitmapHeight), new AfterCharInfoAllocCallback(thread));
            }
        }

        private class AfterCharInfoAllocCallback implements IAction {
        	private SceKernelThreadInfo thread;

        	public AfterCharInfoAllocCallback(SceKernelThreadInfo thread) {
				this.thread = thread;
			}

			@Override
            public void execute() {
                charInfoBitmapAddress = Emulator.getProcessor().cpu._v0;

                if (log.isDebugEnabled()) {
                	log.debug(String.format("FontLib's allocation callback on getCharInfo returned 0x%08X", charInfoBitmapAddress));
                }

                if (charInfoBitmapAddress == 0) {
                	thread.cpuContext._v0 = SceKernelErrors.ERROR_FONT_OUT_OF_MEMORY;
                }
			}
        }

        @Override
        public String toString() {
            return String.format("FontLib - Handle: '0x%08X', Fonts: '%d'", getHandle(), getNumFonts());
        }
    }

    private boolean isFontMatchingStyle(Font font, pspFontStyle fontStyle, boolean optimum) {
    	if (font != null && font.fontInfo != null && font.fontInfo.getFontStyle() != null) {
    		return font.fontInfo.getFontStyle().isMatching(fontStyle, optimum);
    	}
        // Faking: always matching
        return true;
    }

    /**
     * Check if a given font is better matching the fontStyle than the currently best font.
     * The check is based on the fontH and fontV.
     * 
     * @param fontStyle    the criteria for the optimum font
     * @param optimumFont  the currently optimum font
     * @param matchingFont a candidate matching the fontStyle
     * @return             the matchingFont if it is better matching the fontStyle than the optimumFont,
     *                     the optimumFont otherwise
     */
    private Font getOptimiumFont(pspFontStyle fontStyle, Font optimumFont, Font matchingFont) {
    	if (optimumFont == null) {
    		return matchingFont;
    	}
    	pspFontStyle optimiumStyle = optimumFont.fontInfo.getFontStyle();
    	pspFontStyle matchingStyle = matchingFont.fontInfo.getFontStyle();

    	// Check the fontH if it is specified or both fontH and fontV are unspecified
    	boolean testH = fontStyle.fontH != 0f || fontStyle.fontV == 0f;
    	if (testH && Math.abs(fontStyle.fontH - optimiumStyle.fontH) > Math.abs(fontStyle.fontH - matchingStyle.fontH)) {
    		return matchingFont;
    	}

    	// Check the fontV if it is specified or both fontH and fontV are unspecified
    	boolean testV = fontStyle.fontV != 0f || fontStyle.fontH == 0f;
    	if (testV && Math.abs(fontStyle.fontV - optimiumStyle.fontV) > Math.abs(fontStyle.fontV - matchingStyle.fontV)) {
    		return matchingFont;
    	}

    	return optimumFont;
    }

    private Font getOptimumFont(pspFontStyle fontStyle) {
    	Font optimumFont = null;
        for (int i = 0; i < internalFonts.size(); i++) {
        	Font font = internalFonts.get(i);
            if (isFontMatchingStyle(font, fontStyle, true)) {
            	optimumFont = getOptimiumFont(fontStyle, optimumFont, font);
            }
        }

        return optimumFont;
    }

    private int getFontIndex(Font font) {
    	if (font != null) {
	    	for (int i = 0; i < internalFonts.size(); i++) {
	    		if (internalFonts.get(i) == font) {
	    			return i;
	    		}
	    	}
    	}

    	return -1;
    }

    protected FontLib getFontLib(int fontLibHandle) {
    	FontLib fontLib = fontLibsMap.get(fontLibHandle);
    	if (fontLib == null) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_FONT_INVALID_PARAMETER);
    	}

    	return fontLib;
    }

    protected Font getFont(int fontHandle, boolean allowClosedFont) {
    	Font font = fontsMap.get(fontHandle);
    	if (font == null || font.fontInfo == null) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_FONT_INVALID_PARAMETER);
    	}
    	if (!allowClosedFont && font.isClosed()) {
    		throw new SceKernelErrorException(SceKernelErrors.ERROR_FONT_INVALID_PARAMETER);
    	}

    	return font;
    }

    public Font getFont(int index) {
    	if (internalFonts.size() == 0) {
    		loadAllFonts();
    	}
    	return internalFonts.get(index);
    }

    @HLEFunction(nid = 0x67F17ED7, version = 150, checkInsideInterrupt = true, stackUsage = 0x590)
    public int sceFontNewLib(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=44, usage=Usage.in) TPointer32 paramsPtr, @CanBeNull TErrorPointer32 errorCodePtr) {
    	loadAllFonts();
        errorCodePtr.setValue(0);
        FontLib fontLib = new FontLib(paramsPtr);
        fontLibsMap.put(fontLib.getHandle(), fontLib);

        return fontLib.getHandle();
    }

    @HLEFunction(nid = 0x57FCB733, version = 150, checkInsideInterrupt = true)
    public int sceFontOpenUserFile(int fontLibHandle, PspString fileName, int mode, @CanBeNull TErrorPointer32 errorCodePtr) {
        FontLib fontLib = getFontLib(fontLibHandle);
        errorCodePtr.setValue(0);

        // "open" callback is not called in this case. Tested on PSP.

        Font font = openFontFile(fileName.getString());
        if (font == null) {
        	errorCodePtr.setValue(SceKernelErrors.ERROR_FONT_FILE_NOT_FOUND);
        	return 0;
        }

        return fontLib.openFont(font, mode, true).getHandle();
    }

    @HLEFunction(nid = 0xBB8E7FE6, version = 150, checkInsideInterrupt = true, stackUsage = 0x440)
    public int sceFontOpenUserMemory(int fontLibHandle, TPointer memoryFontPtr, int memoryFontLength, @CanBeNull TErrorPointer32 errorCodePtr) {
        FontLib fontLib = getFontLib(fontLibHandle);    
        errorCodePtr.setValue(0);
        return fontLib.openFont(openFontFile(memoryFontPtr.getAddress(), memoryFontLength), 0, false).getHandle();
    }

    @HLEFunction(nid = 0x0DA7535E, version = 150, checkInsideInterrupt = true, stackUsage = 0x0)
    public int sceFontGetFontInfo(int fontHandle, TPointer fontInfoPtr) {
        // A call to sceFontGetFontInfo is allowed on a closed font.
        Font font = getFont(fontHandle, true);

        PGF currentPGF = font.pgf;
        int maxGlyphWidthI = currentPGF.getMaxSize()[0];
        int maxGlyphHeightI = currentPGF.getMaxSize()[1];
        int maxGlyphAscenderI = currentPGF.getMaxAscender();
        int maxGlyphDescenderI = currentPGF.getMaxDescender();
        int maxGlyphLeftXI = currentPGF.getMaxLeftXAdjust();
        int maxGlyphBaseYI = font.maxGlyphBaseYI;
        int minGlyphCenterXI = currentPGF.getMinCenterXAdjust();
        int maxGlyphTopYI = currentPGF.getMaxTopYAdjust();
        int maxGlyphAdvanceXI = currentPGF.getMaxAdvance()[0];
        int maxGlyphAdvanceYI = currentPGF.getMaxAdvance()[1];
        int maxBitmapWidth = font.maxBitmapWidth;
        int maxBitmapHeight = font.maxBitmapHeight;
        pspFontStyle fontStyle = font.getFontStyle();

        // Glyph metrics (in 26.6 signed fixed-point).
        fontInfoPtr.setValue32(0, maxGlyphWidthI);
        fontInfoPtr.setValue32(4, maxGlyphHeightI);
        fontInfoPtr.setValue32(8, maxGlyphAscenderI);
        fontInfoPtr.setValue32(12, maxGlyphDescenderI);
        fontInfoPtr.setValue32(16, maxGlyphLeftXI);
        fontInfoPtr.setValue32(20, maxGlyphBaseYI);
        fontInfoPtr.setValue32(24, minGlyphCenterXI);
        fontInfoPtr.setValue32(28, maxGlyphTopYI);
        fontInfoPtr.setValue32(32, maxGlyphAdvanceXI);
        fontInfoPtr.setValue32(36, maxGlyphAdvanceYI);

        // Glyph metrics (replicated as float).
        for (int i = 0; i < 40; i += 4) {
        	int intValue = fontInfoPtr.getValue32(i);
        	float floatValue = intValue / 64.f;
        	fontInfoPtr.setFloat(i + 40, floatValue);
        }

        // Bitmap dimensions.
        fontInfoPtr.setValue16(80, (short) maxBitmapWidth);
        fontInfoPtr.setValue16(82, (short) maxBitmapHeight);
        fontInfoPtr.setValue32(84, currentPGF.getCharPointerLength()); // Number of elements in the font's charmap.
        fontInfoPtr.setValue32(88, 0); // Number of elements in the font's shadow charmap.

        // Font style (used by font comparison functions).
        fontStyle.write(fontInfoPtr, 92);

        fontInfoPtr.setValue8(260, (byte) currentPGF.getBpp()); // Font's BPP.
        fontInfoPtr.setValue8(261, (byte) 0); // Padding.
        fontInfoPtr.setValue8(262, (byte) 0); // Padding.
        fontInfoPtr.setValue8(263, (byte) 0); // Padding.

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetFontInfo returning maxGlyphWidthI=%d, maxGlyphHeightI=%d, maxGlyphAscenderI=%d, maxGlyphDescenderI=%d, maxGlyphLeftXI=%d, maxGlyphBaseYI=%d, minGlyphCenterXI=%d, maxGlyphTopYI=%d, maxGlyphAdvanceXI=%d, maxGlyphAdvanceYI=%d, maxBitmapWidth=%d, maxBitmapHeight=%d, fontStyle=[%s]%s", maxGlyphWidthI, maxGlyphHeightI, maxGlyphAscenderI, maxGlyphDescenderI, maxGlyphLeftXI, maxGlyphBaseYI, minGlyphCenterXI, maxGlyphTopYI, maxGlyphAdvanceXI, maxGlyphAdvanceYI, maxBitmapWidth, maxBitmapHeight, fontStyle, Utilities.getMemoryDump(fontInfoPtr.getAddress(), 264)));
        }

        return 0;
    }

    @HLEFunction(nid = 0xDCC80C2F, version = 150, checkInsideInterrupt = true, stackUsage = 0x100)
    public int sceFontGetCharInfo(int fontHandle, int charCode, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=60, usage=Usage.out) TPointer charInfoPtr) {
        Font font = getFont(fontHandle, false);       
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetCharInfo charCode=%04X (%c)", charCode, (charCode <= 0xFF ? (char) charCode : '?')));
        }
        charCode &= 0xFFFF;
        pspCharInfo pspCharInfo = null;
        if (!getUseDebugFont()) {
            pspCharInfo = font.fontInfo.getCharInfo(charCode, SceFontInfo.FONT_PGF_GLYPH_TYPE_CHAR);
        }
        if (pspCharInfo == null) {
            pspCharInfo = new pspCharInfo();
            pspCharInfo.bitmapWidth = Debug.Font.charWidth * Debug.fontPixelSize;
            pspCharInfo.bitmapHeight = Debug.Font.charHeight * Debug.fontPixelSize;
            pspCharInfo.sfp26Width = pspCharInfo.bitmapWidth << 6;
            pspCharInfo.sfp26Height = pspCharInfo.bitmapHeight << 6;
            pspCharInfo.sfp26AdvanceH = pspCharInfo.bitmapWidth << 6;
            pspCharInfo.sfp26AdvanceV = pspCharInfo.bitmapHeight << 6;
        }
        int result = font.fontLib.triggetGetCharInfo(pspCharInfo);

        if (result == 0) {
        	pspCharInfo.write(charInfoPtr);
        }

        return result;
    }

    @HLEFunction(nid = 0x980F4895, version = 150, checkInsideInterrupt = true)
    public int sceFontGetCharGlyphImage(int fontHandle, int charCode, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 24, usage = Usage.in) TPointer glyphImagePtr) {
        charCode&= 0xffff;
        Font font = getFont(fontHandle, false);     

        // Read GlyphImage data.
        int pixelFormat = glyphImagePtr.getValue32(0);
        int xPos64 = glyphImagePtr.getValue32(4);
        int yPos64 = glyphImagePtr.getValue32(8);
        int bufWidth = glyphImagePtr.getValue16(12);
        int bufHeight = glyphImagePtr.getValue16(14);
        int bytesPerLine = glyphImagePtr.getValue16(16);
        int buffer = glyphImagePtr.getValue32(20);        
        // 26.6 fixed-point.
        int xPosI = xPos64 >> 6;
        int yPosI = yPos64 >> 6;
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetCharGlyphImage charCode=%04X (%c), xPos=%d, yPos=%d, buffer=0x%08X, bufWidth=%d, bufHeight=%d, bytesPerLine=%d, pixelFormat=%d", charCode, (charCode <= 0xFF ? (char) charCode : '?'), xPosI, yPosI, buffer, bufWidth, bufHeight, bytesPerLine, pixelFormat));
        }

        // If there's an internal font loaded, use it to display the text.
        // Otherwise, switch to our Debug font.
        if (!getUseDebugFont()) {
            font.fontInfo.printFont(
                    buffer, bytesPerLine, bufWidth, bufHeight,
                    xPosI, yPosI,
                    xPos64 % 64, yPos64 % 64,
                    0, 0, bufWidth, bufHeight,
                    pixelFormat, charCode, font.fontLib.getAltCharCode(), SceFontInfo.FONT_PGF_GLYPH_TYPE_CHAR, false);
        } else {
            // Font adjustment.
            // TODO: Instead of using the loaded PGF, figure out
            // the proper values for the Debug font.
            yPosI -= font.pgf.getMaxBaseYAdjust() >> 6;
            yPosI += font.pgf.getMaxTopYAdjust() >> 6;
            
            Debug.printFontbuffer(
                    buffer, bytesPerLine, bufWidth, bufHeight,
                    xPosI, yPosI, pixelFormat, charCode, font.fontLib.getAltCharCode());
        }
        return 0;
    }

    @HLEFunction(nid = 0x099EF33C, version = 150, checkInsideInterrupt = false)
    public int sceFontFindOptimumFont(int fontLibHandle, pspFontStyle fontStyle, @CanBeNull TErrorPointer32 errorCodePtr) {
        if (fontStyle.isEmpty()) {
        	// Always return the first font entry if no criteria is specified for the fontStyle
        	return 0;
        }

        Font optimumFont = getOptimumFont(fontStyle);

        // No font found for the given style, try to find a font without the given font style (bold, italic...)
        if (optimumFont == null && fontStyle.fontStyle != 0) {
	        fontStyle.fontStyle = 0;
	        fontStyle.fontStyleSub = 0;
	        optimumFont = getOptimumFont(fontStyle);
        }

        // No font found for the given style, try to find a font without the given font size.
        if (optimumFont == null && (fontStyle.fontH != 0f || fontStyle.fontV != 0f)) {
	        fontStyle.fontH = 0f;
	        fontStyle.fontV = 0f;
	        optimumFont = getOptimumFont(fontStyle);
        }

        // No font found for the given style, try to find a font without the given country.
        if (optimumFont == null && (fontStyle.fontCountry != 0)) {
	        fontStyle.fontCountry = 0;
	        optimumFont = getOptimumFont(fontStyle);
        }

        int index = getFontIndex(optimumFont);
        if (index < 0) {
        	// optimum font not found, assume font at index 0?
        	index = 0;
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontFindOptimumFont found font at index %d: %s", index, optimumFont));
        }

        return index;
    }

    @HLEFunction(nid = 0x3AEA8CB6, version = 150, checkInsideInterrupt = true)
    public int sceFontClose(int fontHandle) {
        Font font = fontsMap.get(fontHandle);

        if (font != null && font.fontLib != null) {
        	font.fontLib.closeFont(font);
        } else {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceFontClose font already closed font=%s", font));
        	}
        }

        return 0;
    }

    @HLEFunction(nid = 0x574B6FBC, version = 150, checkInsideInterrupt = true)
    public int sceFontDoneLib(int fontLibHandle) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);

        if (fontLib != null) {
	        // Free all reserved font lib space and close all open font files.
	        fontLib.triggerCloseCallback();
	        fontLib.done();
	        fontLibsMap.remove(fontLibHandle);
	        HLEUidObjectMapping.removeObject(fontLib);
        } else {
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceFontDoneLib font lib already done 0x%08X", fontLibHandle));
        	}
        }

        return 0;
    }

    @HLEFunction(nid = 0xA834319D, version = 150, checkInsideInterrupt = false)
    public int sceFontOpen(int fontLibHandle, int index, int mode, @CanBeNull TErrorPointer32 errorCodePtr) {
        FontLib fontLib = getFontLib(fontLibHandle);

        if (index < 0) {
        	errorCodePtr.setValue(SceKernelErrors.ERROR_FONT_INVALID_PARAMETER);
        	return 0;
        }

        Font font = fontLib.openFont(internalFonts.get(index), mode, true);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Opening '%s' - '%s', font=%s", font.pgf.getFontName(), font.pgf.getFontType(), font));
        }
        errorCodePtr.setValue(0);

        return font.getHandle();
    }

    @HLEFunction(nid = 0xCA1E6945, version = 150, checkInsideInterrupt = true, stackUsage = 0x120)
    public int sceFontGetCharGlyphImage_Clip(int fontHandle, int charCode, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 24, usage = Usage.in) TPointer glyphImagePtr, int clipXPos, int clipYPos, int clipWidth, int clipHeight) {
        charCode&= 0xffff;
        Font font = getFont(fontHandle, false);
        // Identical to sceFontGetCharGlyphImage, but uses a clipping
        // rectangle over the char.

        // Read GlyphImage data.
        int pixelFormat = glyphImagePtr.getValue32(0);
        int xPos64 = glyphImagePtr.getValue32(4);
        int yPos64 = glyphImagePtr.getValue32(8);
        int bufWidth = glyphImagePtr.getValue16(12);
        int bufHeight = glyphImagePtr.getValue16(14);
        int bytesPerLine = glyphImagePtr.getValue16(16);
        int buffer = glyphImagePtr.getValue32(20);

        // 26.6 fixed-point.
        int xPosI = xPos64 >> 6;
        int yPosI = yPos64 >> 6;

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetCharGlyphImage_Clip charCode=%04X (%c), xPos=%d(%d), yPos=%d(%d), buffer=0x%08X, bufWidth=%d, bufHeight=%d, bytesPerLine=%d, pixelFormat=%d", charCode, (charCode <= 0xFF ? (char) charCode : '?'), xPosI, xPos64, yPosI, yPos64, buffer, bufWidth, bufHeight, bytesPerLine, pixelFormat));
        }

        // If there's an internal font loaded, use it to display the text.
        // Otherwise, switch to our Debug font.
        if (!getUseDebugFont()) {
            font.fontInfo.printFont(
                    buffer, bytesPerLine, bufWidth, bufHeight,
                    xPosI, yPosI,
                    xPos64 % 64, yPos64 % 64,
                    clipXPos, clipYPos, clipWidth, clipHeight,
                    pixelFormat, charCode, font.fontLib.getAltCharCode(), SceFontInfo.FONT_PGF_GLYPH_TYPE_CHAR, false);
        } else {
            // Font adjustment.
            // TODO: Instead of using the loaded PGF, figure out
            // the proper values for the Debug font.
            yPosI -= font.pgf.getMaxBaseYAdjust() >> 6;
            yPosI += font.pgf.getMaxTopYAdjust() >> 6;
            if (yPosI < 0) {
                yPosI = 0;
            }
            Debug.printFontbuffer(
                    buffer, bytesPerLine, bufWidth, bufHeight,
                    xPosI, yPosI, pixelFormat, charCode, font.fontLib.getAltCharCode());
        }
        return 0;
    }

    @HLEFunction(nid = 0x27F6E642, version = 150, checkInsideInterrupt = true)
    public int sceFontGetNumFontList(int fontLibHandle, @CanBeNull TErrorPointer32 errorCodePtr) {
        // Get all the available fonts
        int numFonts = internalFonts.size();
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetNumFontList returning %d", numFonts));
        }      
        errorCodePtr.setValue(0);

        return numFonts;
    }

    @HLEFunction(nid = 0xBC75D85B, version = 150, checkInsideInterrupt = true)
    public int sceFontGetFontList(int fontLibHandle, TPointer fontStylePtr, int numFonts) {
        int fontsNum = Math.min(internalFonts.size(), numFonts);     
        for (int i = 0; i < fontsNum; i++) {
            Font font = internalFonts.get(i);
            pspFontStyle fontStyle = font.getFontStyle();
            fontStyle.write(fontStylePtr, i * fontStyle.sizeof());
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceFontGetFontList returning font #%d at 0x%08X: %s", i, fontStylePtr.getAddress() + i * fontStyle.sizeof(), fontStyle));
            }
        }

        return 0;
    }

    @HLEFunction(nid = 0xEE232411, version = 150, checkInsideInterrupt = true)
    public int sceFontSetAltCharacterCode(int fontLibHandle, int charCode) {
        FontLib fontLib = getFontLib(fontLibHandle);
        charCode&= 0xffff;
        fontLib.setAltCharCode(charCode);

        return 0;
    }

    @HLEFunction(nid = 0x5C3E4A9E, version = 150, checkInsideInterrupt = true)
    public int sceFontGetCharImageRect(int fontHandle, int charCode, TPointer16 charRectPtr) {
        charCode&= 0xffff;
        Font font = getFont(fontHandle, false);
        pspCharInfo charInfo = font.fontInfo.getCharInfo(charCode, SceFontInfo.FONT_PGF_GLYPH_TYPE_CHAR);

        // This function retrieves the dimensions of a specific char.
        if (charInfo != null) {
        	charRectPtr.setValue(0, charInfo.bitmapWidth);
        	charRectPtr.setValue(2, charInfo.bitmapHeight);
        } else {
        	charRectPtr.setValue(0, 0);
        	charRectPtr.setValue(2, 0);
        }

        return 0;
    }

    @HLEFunction(nid = 0x472694CD, version = 150)
    public float sceFontPointToPixelH(int fontLibHandle, float fontPointsH, @CanBeNull TErrorPointer32 errorCodePtr) {
        FontLib fontLib = getFontLib(fontLibHandle);
        errorCodePtr.setValue(0);

        return fontPointsH * fontLib.fontHRes / pointDPI;
    }

    @HLEFunction(nid = 0x5333322D, version = 150, checkInsideInterrupt = true)
    public int sceFontGetFontInfoByIndexNumber(int fontLibHandle, TPointer fontStylePtr, int fontIndex) {
        // It says FontInfo but it means Style - this is like sceFontGetFontList().
        getFontLib(fontLibHandle);
        if (fontIndex < 0 || fontIndex >= internalFonts.size()) {
        	return ERROR_FONT_INVALID_PARAMETER;
        }
        Font font = internalFonts.get(fontIndex);
        pspFontStyle fontStyle = font.getFontStyle();
        fontStyle.write(fontStylePtr);
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetFontInfoByIndexNumber returning font #%d at %s: %s", fontIndex, fontStylePtr, fontStyle));
        }

        return 0;
    }

    @HLEFunction(nid = 0x48293280, version = 150, checkInsideInterrupt = true)
    public int sceFontSetResolution(int fontLibHandle, float hRes, float vRes) {
        FontLib fontLib = getFontLib(fontLibHandle);
        fontLib.fontHRes = hRes;
        fontLib.fontVRes = vRes;

        return 0;
    }

    @HLEFunction(nid = 0x02D7F94B, version = 150, checkInsideInterrupt = true)
    public int sceFontFlush(int fontHandle) {
        Font font = getFont(fontHandle, false);
    	font.fontLib.flushFont(font);

        return 0;
    }

    @HLEFunction(nid = 0x681E61A7, version = 150, checkInsideInterrupt = true)
    public int sceFontFindFont(int fontLibHandle, pspFontStyle fontStyle, @CanBeNull TErrorPointer32 errorCodePtr) {
        errorCodePtr.setValue(0);
        int fontsNum = internalFonts.size();
        for (int i = 0; i < fontsNum; i++) {
            if (isFontMatchingStyle(internalFonts.get(i), fontStyle, false)) {
                return i;
            }
        }

        return -1;
    }

    @HLEFunction(nid = 0x3C4B7E82, version = 150)
    public float sceFontPointToPixelV(int fontLibHandle, float fontPointsV, @CanBeNull TErrorPointer32 errorCodePtr) {
        FontLib fontLib = getFontLib(fontLibHandle);
        errorCodePtr.setValue(0);

        return fontPointsV * fontLib.fontVRes / pointDPI;
    }

    @HLEFunction(nid = 0x74B21701, version = 150)
    public float sceFontPixelToPointH(int fontLibHandle, float fontPixelsH, @CanBeNull TErrorPointer32 errorCodePtr) {
        FontLib fontLib = getFontLib(fontLibHandle);
        errorCodePtr.setValue(0);

        return fontPixelsH * pointDPI / fontLib.fontHRes;
    }

    @HLEFunction(nid = 0xF8F0752E, version = 150)
    public float sceFontPixelToPointV(int fontLibHandle, float fontPixelsV, @CanBeNull TErrorPointer32 errorCodePtr) {
        FontLib fontLib = getFontLib(fontLibHandle);
        errorCodePtr.setValue(0);

        // Convert vertical pixels to floating points (Pixels Per Inch to Points Per Inch).
        // points = (pixels / dpiX) * 72.
        return fontPixelsV * pointDPI / fontLib.fontVRes;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2F67356A, version = 150)
    public int sceFontCalcMemorySize() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x48B06520, version = 150)
    public int sceFontGetShadowImageRect(int fontHandle, int charCode, TPointer charInfoPtr) {
        charCode&= 0xffff;
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x568BE516, version = 150)
    public int sceFontGetShadowGlyphImage(int fontHandle, int charCode, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 24, usage = Usage.in) TPointer glyphImagePtr) {
        charCode&= 0xffff;
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5DCF6858, version = 150)
    public int sceFontGetShadowGlyphImage_Clip(int fontHandle, int charCode, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 24, usage = Usage.in) TPointer glyphImagePtr, int clipXPos, int clipYPos, int clipWidth, int clipHeight) {
        charCode&= 0xffff;
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAA3DE7B5, version = 150)
    public int sceFontGetShadowInfo(int fontHandle, int charCode, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=60, usage=Usage.out) TPointer charInfoPtr) {
        charCode&= 0xffff;
        return 0;
    }
}