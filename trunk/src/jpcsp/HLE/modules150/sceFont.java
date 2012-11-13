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

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUidClass;
import jpcsp.HLE.HLEUidObjectMapping;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TErrorPointer32;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer16;

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

@HLELogging
public class sceFont extends HLEModule {
    public static Logger log = Modules.getLogger("sceFont");

	private class UseDebugFontSettingsListerner extends AbstractBoolSettingsListener {
		@Override
		protected void settingsValueChanged(boolean value) {
			setUseDebugFont(value);
		}
	}

    @Override
    public String getName() {
        return "sceFont";
    }

    @Override
    public void start() {
    	setSettingsListener("emu.useDebugFont", new UseDebugFontSettingsListerner());

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
        private final int handle;

        public Font(PGF pgf, SceFontInfo fontInfo) {
            this.pgf = pgf;
            this.fontInfo = fontInfo;
            fontLib = null;
            this.handle = 0;
        }

        public Font(Font font, FontLib fontLib, int handle) {
            this.pgf = font.pgf;
            this.fontInfo = font.fontInfo;
            this.fontLib = fontLib;
            this.handle = handle;
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
    private boolean useDebugFont = false;
    private static final boolean dumpFonts = false;
    private List<Font> internalFonts;
    private HashMap<Integer, FontLib> fontLibsMap;
    private HashMap<Integer, Font> fontsMap;
    protected String uidPurpose = "sceFont";
    private List<FontRegistryEntry> fontRegistry;
    protected static final float pointDPI = 72.f;

    protected boolean getUseDebugFont() {
        return useDebugFont;
    }

    private void setUseDebugFont(boolean status) {
        useDebugFont = status;
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
            fontInfo.printFont(addr, fontBpl, fontBufWidth, fontBufHeight, x, y, 0, 0, fontBufWidth, fontBufHeight, fontPixelFormat, charCode, ' ');

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
            log.error("openFontFile", e);
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
        protected int allocatedAddr;

        public FontLib(TPointer params) {
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
            triggerAllocCallback(numFonts * 4 + 4, new AfterCreateAllocCallback());
        }

        public int getNumFonts() {
            return numFonts;
        }

        private Font openFont(Font font) {
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

            return font;
        }

        private void closeFont(Font font) {
            HLEUidObjectMapping.removeObject(font);

            for (int i = 0; i < numFonts; i++) {
            	if (fonts[i] == font.getHandle()) {
            		Memory mem = Memory.getInstance();
            		mem.write32(fonts[i], FONT_IS_CLOSED);
            		break;
            	}
            }

            font.close();
        }

        public void done() {
        	Memory mem = Memory.getInstance();
        	for (int i = 0; i < numFonts; i++) {
        		if (mem.read32(fonts[i]) == FONT_IS_OPEN) {
        			closeFont(fontsMap.get(fonts[i]));
        		}
        		fontsMap.remove(fonts[i]);
        	}
        	triggerFreeCallback(allocatedAddr);
        	allocatedAddr = 0;
        	fonts = null;
        }
        
        public int getHandle() {
            return handle;
        }

        protected void triggerAllocCallback(int size, IAction afterAllocCallback) {
            Modules.ThreadManForUserModule.executeCallback(null, allocFuncAddr, afterAllocCallback, true, userDataAddr, size);
        }

        protected void triggerFreeCallback(int addr) {
            if (Memory.isAddressGood(addr)) {
                Modules.ThreadManForUserModule.executeCallback(null, freeFuncAddr, null, true, userDataAddr, addr);
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

        private void read(TPointer params) {
            userDataAddr = params.getValue32(0);
            numFonts = params.getValue32(4);
            cacheDataAddr = params.getValue32(8);
            allocFuncAddr = params.getValue32(12);
            freeFuncAddr = params.getValue32(16);
            openFuncAddr = params.getValue32(20);
            closeFuncAddr = params.getValue32(24);
            readFuncAddr = params.getValue32(28);
            seekFuncAddr = params.getValue32(32);
            errorFuncAddr = params.getValue32(36);
            ioFinishFuncAddr = params.getValue32(40);

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
                allocatedAddr = Emulator.getProcessor().cpu._v0;

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

                if (log.isDebugEnabled()) {
                	log.debug(String.format("FontLib's allocation callback returned 0x%08X", allocatedAddr));
                }
            }
        }

        private class AfterOpenCallback implements IAction {
            @Override
            public void execute() {
                fileFontHandle = Emulator.getProcessor().cpu._v0;

                log.info("FontLib's file open callback returned 0x" + Integer.toHexString(fileFontHandle));
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

    @HLEFunction(nid = 0x67F17ED7, version = 150, checkInsideInterrupt = true)
    public int sceFontNewLib(TPointer paramsPtr, @CanBeNull TErrorPointer32 errorCodePtr) {
        errorCodePtr.setValue(0);
        FontLib fontLib = new FontLib(paramsPtr);
        fontLibsMap.put(fontLib.getHandle(), fontLib);

        return fontLib.getHandle();
    }

    @HLEFunction(nid = 0x57FCB733, version = 150, checkInsideInterrupt = true)
    public int sceFontOpenUserFile(int fontLibHandle, PspString fileName, int mode, @CanBeNull TErrorPointer32 errorCodePtr) {
        FontLib fontLib = getFontLib(fontLibHandle);
        errorCodePtr.setValue(0);
        if (fontLib.openFuncAddr != 0) {
        	fontLib.triggerOpenCallback(fileName.getAddress(), errorCodePtr.getAddress());
        }

        return fontLib.openFont(openFontFile(fileName.getString())).getHandle();
    }

    @HLEFunction(nid = 0xBB8E7FE6, version = 150, checkInsideInterrupt = true)
    public int sceFontOpenUserMemory(int fontLibHandle, TPointer memoryFontPtr, int memoryFontLength, @CanBeNull TErrorPointer32 errorCodePtr) {
        FontLib fontLib = getFontLib(fontLibHandle);    
        errorCodePtr.setValue(0);
        return fontLib.openFont(openFontFile(memoryFontPtr.getAddress(), memoryFontLength)).getHandle();
    }

    @HLEFunction(nid = 0x0DA7535E, version = 150, checkInsideInterrupt = true)
    public int sceFontGetFontInfo(int fontHandle, TPointer fontInfoPtr) {
        // A call to sceFontGetFontInfo is allowed on a closed font.
        Font font = getFont(fontHandle, true);

        PGF currentPGF = font.pgf;
        int maxGlyphWidthI = currentPGF.getMaxSize()[0];
        int maxGlyphHeightI = currentPGF.getMaxSize()[1];
        int maxGlyphAscenderI = currentPGF.getMaxAscender();
        int maxGlyphDescenderI = currentPGF.getMaxDescender();
        int maxGlyphLeftXI = currentPGF.getMaxLeftXAdjust();
        int maxGlyphBaseYI = currentPGF.getMaxBaseYAdjust();
        int minGlyphCenterXI = currentPGF.getMinCenterXAdjust();
        int maxGlyphTopYI = currentPGF.getMaxTopYAdjust();
        int maxGlyphAdvanceXI = currentPGF.getMaxAdvance()[0];
        int maxGlyphAdvanceYI = currentPGF.getMaxAdvance()[1];
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
        fontInfoPtr.setValue16(80, (short) currentPGF.getMaxGlyphWidth());
        fontInfoPtr.setValue16(82, (short) currentPGF.getMaxGlyphHeight());
        fontInfoPtr.setValue32(84, currentPGF.getCharPointerLength()); // Number of elements in the font's charmap.
        fontInfoPtr.setValue32(88, 0); // Number of elements in the font's shadow charmap.

        // Font style (used by font comparison functions).
        fontStyle.write(fontInfoPtr, 92);

        fontInfoPtr.setValue8(260, (byte) currentPGF.getBpp()); // Font's BPP.
        fontInfoPtr.setValue8(261, (byte) 0); // Padding.
        fontInfoPtr.setValue8(262, (byte) 0); // Padding.
        fontInfoPtr.setValue8(263, (byte) 0); // Padding.

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetFontInfo returning maxGlyphWidthI=%d, maxGlyphHeightI=%d, maxGlyphAscenderI=%d, maxGlyphDescenderI=%d, maxGlyphLeftXI=%d, maxGlyphBaseYI=%d, minGlyphCenterXI=%d, maxGlyphTopYI=%d, maxGlyphAdvanceXI=%d, maxGlyphAdvanceYI=%d, fontStyle=[%s]", maxGlyphWidthI, maxGlyphHeightI, maxGlyphAscenderI, maxGlyphDescenderI, maxGlyphLeftXI, maxGlyphBaseYI, minGlyphCenterXI, maxGlyphTopYI, maxGlyphAdvanceXI, maxGlyphAdvanceYI, fontStyle));
        }

        return 0;
    }

    @HLEFunction(nid = 0xDCC80C2F, version = 150, checkInsideInterrupt = true)
    public int sceFontGetCharInfo(int fontHandle, int charCode, TPointer charInfoPtr) {
        Font font = getFont(fontHandle, false);       
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetCharInfo charCode=%04X (%c)", charCode, (charCode <= 0xFF ? (char) charCode : '?')));
        }

        pspCharInfo pspCharInfo = null;
        if (!getUseDebugFont()) {
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
        pspCharInfo.write(charInfoPtr);    

        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceFontGetCharInfo returning %s", pspCharInfo));
        }

        return 0;
    }

    @HLEFunction(nid = 0x980F4895, version = 150, checkInsideInterrupt = true)
    public int sceFontGetCharGlyphImage(int fontHandle, int charCode, TPointer glyphImagePtr) {
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
                    0, 0, bufWidth, bufHeight,
                    pixelFormat, charCode, font.fontLib.getAltCharCode());
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

        Font font = fontLib.openFont(internalFonts.get(index));
        if (log.isDebugEnabled()) {
            log.debug(String.format("Opening '%s' - '%s', font=%s", font.pgf.getFontName(), font.pgf.getFontType(), font));
        }
        errorCodePtr.setValue(0);

        return font.getHandle();
    }

    @HLEFunction(nid = 0xCA1E6945, version = 150, checkInsideInterrupt = true)
    public int sceFontGetCharGlyphImage_Clip(int fontHandle, int charCode, TPointer glyphImagePtr, int clipXPos, int clipYPos, int clipWidth, int clipHeight) {
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
            log.debug(String.format("sceFontGetCharGlyphImage_Clip charCode=%04X (%c), xPos=%d, yPos=%d, buffer=0x%08X, bufWidth=%d, bufHeight=%d, bytesPerLine=%d, pixelFormat=%d", charCode, (charCode <= 0xFF ? (char) charCode : '?'), xPosI, yPosI, buffer, bufWidth, bufHeight, bytesPerLine, pixelFormat));
        }

        // If there's an internal font loaded, use it to display the text.
        // Otherwise, switch to our Debug font.
        if (!getUseDebugFont()) {
            font.fontInfo.printFont(
                    buffer, bytesPerLine, bufWidth, bufHeight,
                    xPosI, yPosI,
                    clipXPos, clipYPos, clipWidth, clipHeight,
                    pixelFormat, charCode, font.fontLib.getAltCharCode());
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
        fontLib.setAltCharCode(charCode);

        return 0;
    }

    @HLEFunction(nid = 0x5C3E4A9E, version = 150, checkInsideInterrupt = true)
    public int sceFontGetCharImageRect(int fontHandle, int charCode, TPointer16 charRectPtr) {
        Font font = getFont(fontHandle, false);
        pspCharInfo charInfo = font.fontInfo.getCharInfo(charCode);

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
    public int sceFontGetFontInfoByIndexNumber(int fontLibHandle, TPointer fontInfoPtr, int unknown, int fontIndex) {
        FontLib fontLib = getFontLib(fontLibHandle);
        return sceFontGetFontInfo(fontLib.getFontHandle(fontIndex), fontInfoPtr);
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
    public int sceFontGetShadowImageRect() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x568BE516, version = 150)
    public int sceFontGetShadowGlyphImage() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5DCF6858, version = 150)
    public int sceFontGetShadowGlyphImage_Clip() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAA3DE7B5, version = 150)
    public int sceFontGetShadowInfo() {
        return 0;
    }
}