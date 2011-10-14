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

import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_FAMILY_SANS_SERIF;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_FAMILY_SERIF;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_LANGUAGE_JAPANESE;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_LANGUAGE_KOREAN;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_LANGUAGE_LATIN;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_STYLE_BOLD;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_STYLE_BOLD_ITALIC;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_STYLE_DB;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_STYLE_ITALIC;
import static jpcsp.HLE.kernel.types.pspFontStyle.FONT_STYLE_REGULAR;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEUidClass;
import jpcsp.HLE.HLEUidObjectMapping;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TErrorPointer32;
import jpcsp.HLE.TPointer;

import java.io.File;
import java.io.IOException;
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
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceFontInfo;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.pspCharInfo;
import jpcsp.HLE.kernel.types.pspFontStyle;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.format.PGF;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.capture.CaptureImage;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.util.Debug;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceFont extends HLEModule {
    private static Logger log = Modules.getLogger("sceFont");

	private class AllowInternalFontsSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setAllowInternalFonts(value);
		}
	}

    @Override
    public String getName() {
        return "sceFont";
    }

    @Override
    public void start() {
    	setSettingsListener("emu.useFlashFonts", new AllowInternalFontsSettingsListerner());

    	fontIndex = -1;
        fontLibIndex = -1;
        internalFonts = new LinkedList<Font>();
        fontLibsMap = new HashMap<Integer, FontLib>();
        fontsMap = new HashMap<Integer, Font>();
        loadFontRegistry();
        loadDefaultSystemFont();
        loadAllFonts();

        super.start();
    }

    public int fontIndex;
    public int fontLibIndex;

    @HLEUidClass(errorValueOnNotFound = SceKernelErrors.ERROR_FONT_INVALID_LIBID)
    public class Font {

        public PGF pgf;
        public SceFontInfo fontInfo;
        public FontLib fontLib;
        private int handle;

        public Font(PGF pgf, SceFontInfo fontInfo) {
            this.pgf = pgf;
            this.fontInfo = fontInfo;
            fontLib = null;
            makeHandle();
        }

        public Font(Font font, FontLib fontLib) {
            this.pgf = font.pgf;
            this.fontInfo = font.fontInfo;
            this.fontLib = fontLib;
            makeHandle();
        }
        
        private void makeHandle() {
            handle = (((++fontIndex) << 8) | 0x0000001C);
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

        @Override
        public String toString() {
            return String.format("Font '%s' - '%s'", pgf.getFileNamez(), pgf.getFontName());
        }
    }

    public class FontRegistryEntry {

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

        public FontRegistryEntry(int h_size, int v_size, int h_resolution,
                int v_resolution, int extra_attributes, int weight,
                int family_code, int style, int sub_style, int language_code,
                int region_code, int country_code, String file_name,
                String font_name, int expire_date, int shadow_option) {
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
        }
    }
    public static final int PGF_MAGIC = 'P' << 24 | 'G' << 16 | 'F' << 8 | '0';
    public static final String fontDirPath = "flash0:/font";
    public static final String customFontFile = "debug.jpft";
    public static final int PSP_FONT_PIXELFORMAT_4 = 0; // 2 pixels packed in 1 byte (natural order)
    public static final int PSP_FONT_PIXELFORMAT_4_REV = 1; // 2 pixels packed in 1 byte (reversed order)
    public static final int PSP_FONT_PIXELFORMAT_8 = 2; // 1 pixel in 1 byte
    public static final int PSP_FONT_PIXELFORMAT_24 = 3; // 1 pixel in 3 bytes (RGB)
    public static final int PSP_FONT_PIXELFORMAT_32 = 4; // 1 pixel in 4 bytes (RGBA)
    public static final int PSP_FONT_MODE_FILE = 0;
    public static final int PSP_FONT_MODE_MEMORY = 1;
    private boolean allowInternalFonts = false;
    private static final boolean dumpFonts = false;
    private List<Font> internalFonts;
    private HashMap<Integer, FontLib> fontLibsMap;
    private HashMap<Integer, Font> fontsMap;
    protected String uidPurpose = "sceFont";
    private List<FontRegistryEntry> fontRegistry;
    protected static final float pointDPI = 72.f;

    protected boolean getAllowInternalFonts() {
        return allowInternalFonts;
    }

    private void setAllowInternalFonts(boolean status) {
        allowInternalFonts = status;
    }

    protected void loadFontRegistry() {
        fontRegistry = new LinkedList<FontRegistryEntry>();
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_DB, 0, FONT_LANGUAGE_JAPANESE, 0, 1, "jpn0.pgf", "FTT-NewRodin Pro DB", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_REGULAR, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn0.pgf", "FTT-NewRodin Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_REGULAR, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn1.pgf", "FTT-Matisse Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn2.pgf", "FTT-NewRodin Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn3.pgf", "FTT-Matisse Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_BOLD, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn4.pgf", "FTT-NewRodin Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_BOLD, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn5.pgf", "FTT-Matisse Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_BOLD_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn6.pgf", "FTT-NewRodin Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_BOLD_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn7.pgf", "FTT-Matisse Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_REGULAR, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn8.pgf", "FTT-NewRodin Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_REGULAR, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn9.pgf", "FTT-Matisse Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn10.pgf", "FTT-NewRodin Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn11.pgf", "FTT-Matisse Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_BOLD, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn12.pgf", "FTT-NewRodin Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_BOLD, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn13.pgf", "FTT-Matisse Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_BOLD_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn14.pgf", "FTT-NewRodin Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF, FONT_STYLE_BOLD_ITALIC, 0, FONT_LANGUAGE_LATIN, 0, 1, "ltn15.pgf", "FTT-Matisse Pro Latin", 0, 0));
        fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_REGULAR, 0, FONT_LANGUAGE_KOREAN, 0, 3, "kr0.pgf", "AsiaNHH(512Johab)", 0, 0));
    }

    protected void loadDefaultSystemFont() {
        try {
            SeekableDataInput fontFile = Modules.IoFileMgrForUserModule.getFile(fontDirPath + "/" + customFontFile, IoFileMgrForUser.PSP_O_RDONLY);
            if (fontFile != null) {
                fontFile.skipBytes(32);  // Skip custom header.
                char[] c = new char[(int) fontFile.length() - 32];
                for (int i = 0; i < c.length; i++) {
                    c[i] = (char) (fontFile.readByte() & 0xFF);
                }
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
        int fontBpl = bufferWidth * sceDisplay.getPixelFormatBytes(bufferStorage);
        int fontBufHeight = MemoryMap.SIZE_VRAM / fontBpl;
        int x = 0;
        int y = 0;
        SceFontInfo fontInfo = font.fontInfo;
        PGF pgf = font.pgf;

        int memoryLength = fontBpl * fontBufHeight * sceDisplay.getPixelFormatBytes(bufferStorage);
        Memory.getInstance().memset(addr, (byte) 0, memoryLength);

        int maxGlyphWidth = pgf.getMaxSize()[0] >> 6;
        int maxGlyphHeight = pgf.getMaxSize()[1] >> 6;
        for (int charCode = pgf.getFirstGlyphInCharMap(); charCode <= pgf.getLastGlyphInCharMap(); charCode++) {
            fontInfo.printFont(addr, fontBpl, fontBufWidth, fontBufHeight, x, y, fontPixelFormat, charCode, ' ');

            x += maxGlyphWidth;
            if (x + maxGlyphWidth >= fontBufWidth) {
                x = 0;
                y += maxGlyphHeight;
                if (y >= fontBufHeight) {
                    break;
                }
            }
        }

        Buffer memoryBuffer = Memory.getInstance().getBuffer(addr, memoryLength);
        String fileNamePrefix = String.format("Font-%s-", pgf.getFileNamez());
        CaptureImage image = new CaptureImage(addr, 0, memoryBuffer, fontBufWidth, fontBufHeight, bufferWidth, bufferStorage, false, 0, false, true, fileNamePrefix);
        try {
            image.write();
        } catch (IOException e) {
            log.error(e);
        }
    }

    protected Font openFontFile(ByteBuffer pgfBuffer, String fileName) {
        Font font = null;

        try {
            PGF pgfFile = new PGF(pgfBuffer);
            if (fileName != null) {
                pgfFile.setFileNamez(fileName);
            }

            SceFontInfo fontInfo = new SceFontInfo(pgfFile);

            font = new Font(pgfFile, fontInfo);

            if (dumpFonts) {
                dumpFont(font);
            }
        } catch (Exception e) {
            // Can't parse file.
            log.error(e);
        }

        return font;
    }

    protected Font openFontFile(String fileName) {
        Font font = null;

        try {
            SeekableDataInput fontFile = Modules.IoFileMgrForUserModule.getFile(fileName, IoFileMgrForUser.PSP_O_RDONLY);
            byte[] pgfBytes = new byte[(int) fontFile.length()];
            fontFile.readFully(pgfBytes);
            fontFile.close();
            ByteBuffer pgfBuffer = ByteBuffer.wrap(pgfBytes);

            font = openFontFile(pgfBuffer, new File(fileName).getName());
        } catch (IOException e) {
            // Can't open file.
            log.warn(e);
        }

        return font;
    }
    
    protected Font openFontFile(int addr, int length) {
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
        protected int memFontAddr;
        protected int fileFontHandle;
        protected int altCharCode;
        protected float fontHRes = 128.f;
        protected float fontVRes = 128.f;
        protected int handle;
        protected List<Font> fonts;

        public FontLib(int params) {
            read(params);
            fonts = new LinkedList<Font>();
            makeHandle();
        }
        
        private void makeHandle() {
            handle = (((++fontLibIndex) << 8) | 0x0000001B);
        }

        public int getNumFonts() {
            return numFonts;
        }

        private Font openFont(Font font) {
            if (font == null) {
                throw (new SceKernelErrorException(SceKernelErrors.ERROR_FONT_INVALID_PARAMETER));
            }
            if (fonts.size() >= numFonts) {
                throw (new SceKernelErrorException(SceKernelErrors.ERROR_FONT_TOO_MANY_OPEN_FONTS));
            }

            font = new Font(font, this);
            fonts.add(font);
            fontsMap.put(font.getHandle(), font);

            return font;
        }

        private void closeFont(Font font) {
            HLEUidObjectMapping.removeObject(font);

            fonts.remove(font);

            font.fontLib = null;
            font.pgf = null;
            font.fontInfo = null;
        }

        public void closeAllFonts() {
            for (Font font : fonts) {
                closeFont(font);
            }
        }
        
        public int getHandle() {
            return handle;
        }

        private void triggerAllocCallback(int size) {
            Modules.ThreadManForUserModule.executeCallback(null, allocFuncAddr, new AfterAllocCallback(), true, userDataAddr, size);
        }

        private void triggerFreeCallback() {
            if (Memory.isAddressGood(memFontAddr)) {
                Modules.ThreadManForUserModule.executeCallback(null, freeFuncAddr, null, true, userDataAddr, memFontAddr);
            }
        }

        private void triggerOpenCallback(int fileNameAddr, int errorCodeAddr) {
            Modules.ThreadManForUserModule.executeCallback(null, allocFuncAddr, new AfterOpenCallback(), true, userDataAddr, fileNameAddr, errorCodeAddr);
        }

        private void triggerCloseCallback() {
            if (fileFontHandle != 0) {
                Modules.ThreadManForUserModule.executeCallback(null, freeFuncAddr, null, true, userDataAddr, fileFontHandle);
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

        public int getAltCharCode() {
            return altCharCode;
        }

        public void setAltCharCode(int altCharCode) {
            this.altCharCode = altCharCode;
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
        
        @Override
        public String toString() {
            return String.format("FontLib - Handle: '0x%08X', Fonts: '%d'", getHandle(), getNumFonts());
        }
    }

    private boolean isFontMatchingStyle(Font font, pspFontStyle fontStyle) {
        // Faking: always matching
        return true;
    }

    @HLEFunction(nid = 0x67F17ED7, version = 150, checkInsideInterrupt = true)
    public int sceFontNewLib(TPointer paramsPtr, TErrorPointer32 errorCodePtr) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontNewLib paramsAddr=%s, errorCodeAddr=%s", paramsPtr, errorCodePtr));
        }
        if (errorCodePtr.isAddressGood()) {
            errorCodePtr.setValue(0);
        }
        FontLib fontLib = new FontLib(paramsPtr.getAddress());
        fontLibsMap.put(fontLib.getHandle(), fontLib);
        return fontLib.getHandle();
    }

    @HLEFunction(nid = 0x57FCB733, version = 150, checkInsideInterrupt = true)
    public int sceFontOpenUserFile(int fontLibHandle, int fileNameAddr, int mode, TErrorPointer32 errorCodePtr) {
        String fileName = Utilities.readStringZ(fileNameAddr);
        FontLib fontLib = fontLibsMap.get(fontLibHandle);
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "sceFontOpenUserFile fontLib=%s, fileName=0x%08X ('%s'), mode=0x%08X, errorCodeAddr=0x%08X",
                    fontLib, fileNameAddr, fileName, mode, errorCodePtr.getAddress()));
        }
        if (errorCodePtr.isAddressGood()) {
            errorCodePtr.setValue(0);
        } 
        fontLib.triggerOpenCallback(fileNameAddr, errorCodePtr.getAddress());
        return fontLib.openFont(openFontFile(fileName)).getHandle();
    }

    @HLEFunction(nid = 0xBB8E7FE6, version = 150, checkInsideInterrupt = true)
    public int sceFontOpenUserMemory(int fontLibHandle, TPointer memoryFontPtr, int memoryFontLength, TErrorPointer32 errorCodePtr) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);    
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "sceFontOpenUserMemory fontLib=%s, memoryFontAddr=0x%08X, memoryFontLength=%d, errorCodeAddr=0x%08X",
                    fontLib, memoryFontPtr.getAddress(), memoryFontLength, errorCodePtr.getAddress()));
        }
        if (errorCodePtr.isAddressGood()) {
            errorCodePtr.setValue(0);
        } 
        fontLib.triggerAllocCallback(memoryFontLength);
        return fontLib.openFont(openFontFile(memoryFontPtr.getAddress(), memoryFontLength)).getHandle();
    }

    @HLEFunction(nid = 0x0DA7535E, version = 150, checkInsideInterrupt = true)
    public int sceFontGetFontInfo(int fontHandle, TPointer fontInfoPtr) {
        Memory mem = fontInfoPtr.getMemory();
        int fontInfoAddr = fontInfoPtr.getAddress();
        Font font = fontsMap.get(fontHandle);     
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetFontInfo font=%s, fontInfoAddr=0x%08X", font, fontInfoPtr.getAddress()));
        }
        PGF currentPGF = font.pgf;
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
        pspFontStyle fontStyle = font.getFontStyle();

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
        mem.write32(fontInfoAddr + 84, currentPGF.getCharMapLength());     // Number of elements in the font's charmap.
        mem.write32(fontInfoAddr + 88, currentPGF.getShadowMapLength());   // Number of elements in the font's shadow charmap.
        
        // Font style (used by font comparison functions).
        fontStyle.write(mem, fontInfoAddr + 92);
        mem.write8(fontInfoAddr + 260, (byte) 4); // Font's BPP.
        mem.write8(fontInfoAddr + 261, (byte) 0); // Padding.
        mem.write8(fontInfoAddr + 262, (byte) 0); // Padding.
        mem.write8(fontInfoAddr + 263, (byte) 0); // Padding.

        return 0;
    }

    @HLEFunction(nid = 0xDCC80C2F, version = 150, checkInsideInterrupt = true)
    public int sceFontGetCharInfo(int fontHandle, int charCode, TPointer charInfoPtr) {
        Font font = fontsMap.get(fontHandle);       
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "sceFontGetCharInfo font=%s, charCode=%04X (%c), charInfoAddr=%08X",
                    font, charCode, (charCode <= 0xFF ? (char) charCode : '?'), charInfoPtr.getAddress()));
        }
        pspCharInfo pspCharInfo = null;
        if (getAllowInternalFonts()) {
            pspCharInfo = font.fontInfo.getCharInfo(charCode);
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
        pspCharInfo.write(charInfoPtr.getMemory(), charInfoPtr.getAddress());    
        return 0;
    }

    @HLEFunction(nid = 0x980F4895, version = 150, checkInsideInterrupt = true)
    public int sceFontGetCharGlyphImage(int fontHandle, int charCode, TPointer glyphImagePtr) {
        Font font = fontsMap.get(fontHandle);     
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "sceFontGetCharGlyphImage font=%s, charCode=%04X (%c), glyphImageAddr=%08X",
                    font, charCode, (charCode <= 0xFF ? (char) charCode : '?'), glyphImagePtr.getAddress()));
        }
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
            log.debug("sceFontGetCharGlyphImage c=" + ((char) charCode) + ", xPos=" + xPosI + ", yPos=" + yPosI + ", buffer=0x" + Integer.toHexString(buffer) + ", bufWidth=" + bufWidth + ", bufHeight=" + bufHeight + ", bytesPerLine=" + bytesPerLine + ", pixelFormat=" + pixelFormat);
        }
        // If there's an internal font loaded, use it to display the text.
        // Otherwise, switch to our Debug font.
        if (getAllowInternalFonts()) {
            font.fontInfo.printFont(
                    buffer, bytesPerLine, bufWidth, bufHeight,
                    xPosI, yPosI, pixelFormat, charCode, font.fontLib.getAltCharCode());
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
    public int sceFontFindOptimumFont(int fontLibHandle, TPointer fontStylePtr, TErrorPointer32 errorCodePtr) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);    
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "sceFontFindOptimumFont fontLib=%s, fontStyleAddr=0x%08X, errorCodeAddr=0x%08X",
                    fontLib, fontStylePtr.getAddress(), errorCodePtr.getAddress()));
        }
        // Font style parameters.
        pspFontStyle fontStyle = new pspFontStyle();
        fontStyle.read(fontStylePtr.getMemory(), fontStylePtr.getAddress());
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontFindOptimumFont: %s", fontStyle.toString()));
        }
        if (errorCodePtr.isAddressGood()) {
            errorCodePtr.setValue(0);
        }     
        for (int i = 0; i < internalFonts.size(); i++) {
            if (isFontMatchingStyle(internalFonts.get(i), fontStyle)) {
                return i;
            }
        }
        return -1;
    }

    @HLEFunction(nid = 0x3AEA8CB6, version = 150, checkInsideInterrupt = true)
    public int sceFontClose(int fontHandle) {
        Font font = fontsMap.get(fontHandle);    
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontClose font=%s", font));
        }
        font.fontLib.closeFont(font);
        return 0;
    }

    @HLEFunction(nid = 0x574B6FBC, version = 150, checkInsideInterrupt = true)
    public int sceFontDoneLib(int fontLibHandle) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);        
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontDoneLib fontLib=%s", fontLib));
        }
        // Free all reserved font lib space and close all open font files.
        fontLib.triggerFreeCallback();
        fontLib.triggerCloseCallback();
        fontLib.closeAllFonts();
        HLEUidObjectMapping.removeObject(fontLib);
        return 0;
    }

    @HLEFunction(nid = 0xA834319D, version = 150, checkInsideInterrupt = false)
    public int sceFontOpen(int fontLibHandle, int index, int mode, TErrorPointer32 errorCodePtr) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);      
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "sceFontOpen fontLib=%s, index=%d, mode=%d, errorCodeAddr=0x%08X",
                    fontLib, index, mode, errorCodePtr.getAddress()));
        }
        Font font = fontLib.openFont(internalFonts.get(index));
        if (log.isDebugEnabled()) {
            log.debug(String.format("Opening '%s' - '%s', font=%s", font.pgf.getFontName(), font.pgf.getFontType(), font));
        }     
        if (errorCodePtr.isAddressGood()) {
            errorCodePtr.setValue(0);
        }
        return font.getHandle();
    }

    @HLEFunction(nid = 0xCA1E6945, version = 150, checkInsideInterrupt = true)
    public int sceFontGetCharGlyphImage_Clip(int fontHandle, int charCode, TPointer glyphImagePtr, int clipXPos, int clipYPos, int clipWidth, int clipHeight) {
        Font font = fontsMap.get(fontHandle);     
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "sceFontGetCharGlyphImage_Clip fontHandle=0x%08X, charCode=%04X (%c), glyphImageAddr=%08X"
                    + ", clipXPos=%d, clipYPos=%d, clipWidth=%d, clipHeight=%d,",
                    fontHandle, charCode, (charCode <= 0xFF ? (char) charCode : '?'), glyphImagePtr.getAddress(), clipXPos, clipYPos, clipWidth, clipHeight));
        }
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
            log.debug("sceFontGetCharGlyphImage_Clip c=" + ((char) charCode) + ", xPos=" + xPosI + ", yPos=" + yPosI + ", buffer=0x" + Integer.toHexString(buffer) + ", bufWidth=" + bufWidth + ", bufHeight=" + bufHeight + ", bytesPerLine=" + bytesPerLine + ", pixelFormat=" + pixelFormat);
        }

        // If there's an internal font loaded, use it to display the text.
        // Otherwise, switch to our Debug font.
        if (getAllowInternalFonts()) {
            font.fontInfo.printFont(
                    buffer, bytesPerLine, bufWidth, bufHeight,
                    xPosI, yPosI, pixelFormat, charCode, font.fontLib.getAltCharCode());
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
    public int sceFontGetNumFontList(int fontLibHandle, TErrorPointer32 errorCodePtr) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);       
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetNumFontList libHandle=%s, errorCodeAddr=0x%08X", fontLib, errorCodePtr.getAddress()));
        }
        // Get all the available fonts
        int numFonts = internalFonts.size();
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetNumFontList returning %d", numFonts));
        }      
        if (errorCodePtr.isAddressGood()) {
            errorCodePtr.setValue(0);
        }
        return numFonts;
    }

    @HLEFunction(nid = 0xBC75D85B, version = 150, checkInsideInterrupt = true)
    public int sceFontGetFontList(int fontLibHandle, TPointer fontStylePtr, int numFonts) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);       
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "sceFontGetFontList fontLib=%s, fontListAddr=0x%08X, numFonts=%d",
                    fontLib, fontStylePtr.getAddress(), numFonts));
        }
        int fontsNum = Math.min(internalFonts.size(), numFonts);     
        Memory mem = fontStylePtr.getMemory();
        int fontStyleAddr = fontStylePtr.getAddress();
        for (int i = 0; i < fontsNum; i++) {
            Font font = internalFonts.get(i);
            pspFontStyle fontStyle = font.getFontStyle();
            fontStyle.write(mem, fontStyleAddr);
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceFontGetFontList returning font #%d at 0x%08X: %s", i, fontStyleAddr, fontStyle.toString()));
            }
            fontStyleAddr += fontStyle.sizeof();
        }
        return 0;
    }

    @HLEFunction(nid = 0xEE232411, version = 150, checkInsideInterrupt = true)
    public int sceFontSetAltCharacterCode(int fontLibHandle, int charCode) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);       
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontSetAltCharacterCode fontLib=%s, charCode=%04X", fontLib, charCode));
        }
        fontLib.setAltCharCode(charCode);
        return 0;
    }

    @HLEFunction(nid = 0x5C3E4A9E, version = 150, checkInsideInterrupt = true)
    public int sceFontGetCharImageRect(int fontHandle, int charCode, TPointer charRectPtr) {
        Font font = fontsMap.get(fontHandle);      
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetCharImageRect font=%s, charCode=%04X , charRectAddr=0x%08X", font, charCode, charRectPtr.getAddress()));
        }
        // This function retrieves the dimensions of a specific char.
        // Faking.
        charRectPtr.setValue16(0, (short) 1); // Width.
        charRectPtr.setValue16(2, (short) 1); // Height.
        return 0;
    }

    @HLEFunction(nid = 0x472694CD, version = 150)
    public float sceFontPointToPixelH(int fontLibHandle, float fontPointsH, TErrorPointer32 errorCodePtr) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);      
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontPointToPixelH fontLib=%s, fontPointsH=%f, errorCodeAddr=0x%08X", fontLib, fontPointsH, errorCodePtr.getAddress()));
        }    
        if (errorCodePtr.isAddressGood()) {
            errorCodePtr.setValue(0);
        }
        return fontPointsH * fontLib.fontHRes / pointDPI;
    }

    @HLEFunction(nid = 0x5333322D, version = 150, checkInsideInterrupt = true)
    public int sceFontGetFontInfoByIndexNumber(int fontLibHandle, TPointer fontInfoPtr, int __unk, int fontIndex) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);       
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetFontInfoByIndexNumber fontLib=%s, fontInfoAddr=0x%08X, fontIndex=%d", fontLib, fontInfoPtr.getAddress(), fontIndex));
        }
        return sceFontGetFontInfo(fontLibsMap.get(fontLibHandle).fonts.get(fontIndex).getHandle(), fontInfoPtr);
    }

    @HLEFunction(nid = 0x48293280, version = 150, checkInsideInterrupt = true)
    public int sceFontSetResolution(int fontLibHandle, float hRes, float vRes) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);     
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontSetResolution fontLib=%s, hRes=%f, vRes=%f", fontLib, hRes, vRes));
        }
        fontLib.fontHRes = hRes;
        fontLib.fontVRes = vRes;
        return 0;
    }

    @HLEFunction(nid = 0x02D7F94B, version = 150, checkInsideInterrupt = true)
    public int sceFontFlush(int fontHandle) {
        return 0;
    }

    @HLEFunction(nid = 0x681E61A7, version = 150, checkInsideInterrupt = true)
    public int sceFontFindFont(int fontLibHandle, TPointer fontStylePtr, TErrorPointer32 errorCodePtr) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);       
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontFindFont fontLib=%s, fontStyleAddr=0x%08X, errorCodeAddr=0x%08X", fontLib, fontStylePtr.getAddress(), errorCodePtr.getAddress()));
        }
        // Font style parameters.
        pspFontStyle fontStyle = new pspFontStyle();
        fontStyle.read(fontStylePtr.getMemory(), fontStylePtr.getAddress());
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontFindFont: %s", fontStyle.toString()));
        }     
        if (errorCodePtr.isAddressGood()) {
            errorCodePtr.setValue(0);
        }        
        int fontsNum = internalFonts.size();
        for (int i = 0; i < fontsNum; i++) {
            if (isFontMatchingStyle(internalFonts.get(i), fontStyle)) {
                return i;
            }
        }
        return -1;
    }

    @HLEFunction(nid = 0x3C4B7E82, version = 150)
    public float sceFontPointToPixelV(int fontLibHandle, float fontPointsV, TErrorPointer32 errorCodePtr) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);        
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontPointToPixelV fontLib=%s, fontPointsV=%f, errorCodeAddr=0x%08X", fontLib, fontPointsV, errorCodePtr.getAddress()));
        }      
        if (errorCodePtr.isAddressGood()) {
            errorCodePtr.setValue(0);
        }
        return fontPointsV * fontLib.fontVRes / pointDPI;
    }

    @HLEFunction(nid = 0x74B21701, version = 150)
    public float sceFontPixelToPointH(int fontLibHandle, float fontPixelsH, TErrorPointer32 errorCodePtr) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);       
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontPixelToPointH fontLib=%s, fontPixelsH=%f, errorCodeAddr=0x%08X", fontLib, fontPixelsH, errorCodePtr.getAddress()));
        }       
        if (errorCodePtr.isAddressGood()) {
            errorCodePtr.setValue(0);
        }
        return fontPixelsH * pointDPI / fontLib.fontHRes;
    }

    @HLEFunction(nid = 0xF8F0752E, version = 150)
    public float sceFontPixelToPointV(int fontLibHandle, float fontPixelsV, TErrorPointer32 errorCodePtr) {
        FontLib fontLib = fontLibsMap.get(fontLibHandle);     
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontPixelToPointV fontLib=%s, fontPixelsV=%f, errorCodeAddr=0x%08X", fontLib, fontPixelsV, errorCodePtr.getAddress()));
        }     
        if (errorCodePtr.isAddressGood()) {
            errorCodePtr.setValue(0);
        }
        // Convert vertical pixels to floating points (Pixels Per Inch to Points Per Inch).
        // points = (pixels / dpiX) * 72.
        return fontPixelsV * pointDPI / fontLib.fontVRes;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2F67356A, version = 150)
    public int sceFontCalcMemorySize() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x48B06520, version = 150)
    public int sceFontGetShadowImageRect() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x568BE516, version = 150)
    public int sceFontGetShadowGlyphImage() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5DCF6858, version = 150)
    public int sceFontGetShadowGlyphImage_Clip() {
        return 0xDEADC0DE;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAA3DE7B5, version = 150)
    public int sceFontGetShadowInfo() {
        return 0xDEADC0DE;
    }
}