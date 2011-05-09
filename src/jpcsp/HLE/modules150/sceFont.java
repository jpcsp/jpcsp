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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceFontInfo;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.pspCharInfo;
import jpcsp.HLE.kernel.types.pspFontStyle;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.HLEStartModule;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.format.PGF;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.capture.CaptureImage;
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
            mm.addFunction(0x2F67356A, sceFontCalcMemorySizeFunction);
            mm.addFunction(0x48B06520, sceFontGetShadowImageRectFunction);
            mm.addFunction(0x568BE516, sceFontGetShadowGlyphImageFunction);
            mm.addFunction(0x5DCF6858, sceFontGetShadowGlyphImage_ClipFunction);
            mm.addFunction(0xAA3DE7B5, sceFontGetShadowInfoFunction);

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
            mm.removeFunction(sceFontCalcMemorySizeFunction);
            mm.removeFunction(sceFontGetShadowImageRectFunction);
            mm.removeFunction(sceFontGetShadowGlyphImageFunction);
            mm.removeFunction(sceFontGetShadowGlyphImage_ClipFunction);
            mm.removeFunction(sceFontGetShadowInfoFunction);

        }
    }

    @Override
    public void start() {
    	loadFontRegistry();
        loadDefaultSystemFont();
        fontLibMap = new HashMap<Integer, FontLib>();
        fontMap = new HashMap<Integer, Font>();
        loadAllFonts();
    }

    @Override
    public void stop() {
    }

    private static class Font {
    	public PGF pgf;
    	public SceFontInfo fontInfo;
    	public FontLib fontLib;
    	public int fontHandle;

    	public Font(PGF pgf, SceFontInfo fontInfo) {
    		this.pgf = pgf;
    		this.fontInfo = fontInfo;
    		fontLib = null;
    		fontHandle = -1;
    	}

    	public Font(Font font, FontLib fontLib, int fontHandle) {
    		this.pgf = font.pgf;
    		this.fontInfo = font.fontInfo;
    		this.fontLib = fontLib;
    		this.fontHandle = fontHandle;
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

    	@Override
		public String toString() {
			return String.format("Font '%s' - '%s'", pgf.getFileNamez(), pgf.getFontName());
		}
    }

    private static class FontRegistryEntry {
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
    private HashMap<Integer, FontLib> fontLibMap;
    private HashMap<Integer, Font> fontMap;
    private static boolean allowInternalFonts = false;
    private static final boolean dumpFonts = false;
    private List<Font> allFonts;
    protected String uidPurpose = "sceFont";
    private List<FontRegistryEntry> fontRegistry;
    protected static final float pointDPI = 72.f;

    public static boolean getAllowInternalFonts() {
        return allowInternalFonts;
    }

    public static void setAllowInternalFonts(boolean status) {
        allowInternalFonts = status;
    }

    protected void loadFontRegistry() {
    	fontRegistry = new LinkedList<FontRegistryEntry>();
    	fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_DB         , 0, FONT_LANGUAGE_JAPANESE, 0, 1, "jpn0.pgf" , "FTT-NewRodin Pro DB"   , 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_REGULAR    , 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn0.pgf" , "FTT-NewRodin Pro Latin", 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF     , FONT_STYLE_REGULAR    , 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn1.pgf" , "FTT-Matisse Pro Latin" , 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_ITALIC     , 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn2.pgf" , "FTT-NewRodin Pro Latin", 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF     , FONT_STYLE_ITALIC     , 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn3.pgf" , "FTT-Matisse Pro Latin" , 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_BOLD       , 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn4.pgf" , "FTT-NewRodin Pro Latin", 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF     , FONT_STYLE_BOLD       , 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn5.pgf" , "FTT-Matisse Pro Latin" , 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_BOLD_ITALIC, 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn6.pgf" , "FTT-NewRodin Pro Latin", 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF     , FONT_STYLE_BOLD_ITALIC, 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn7.pgf" , "FTT-Matisse Pro Latin" , 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_REGULAR    , 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn8.pgf" , "FTT-NewRodin Pro Latin", 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF     , FONT_STYLE_REGULAR    , 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn9.pgf" , "FTT-Matisse Pro Latin" , 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_ITALIC     , 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn10.pgf", "FTT-NewRodin Pro Latin", 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF     , FONT_STYLE_ITALIC     , 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn11.pgf", "FTT-Matisse Pro Latin" , 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_BOLD       , 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn12.pgf", "FTT-NewRodin Pro Latin", 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF     , FONT_STYLE_BOLD       , 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn13.pgf", "FTT-Matisse Pro Latin" , 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_BOLD_ITALIC, 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn14.pgf", "FTT-NewRodin Pro Latin", 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x1c0, 0x1c0, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SERIF     , FONT_STYLE_BOLD_ITALIC, 0, FONT_LANGUAGE_LATIN   , 0, 1, "ltn15.pgf", "FTT-Matisse Pro Latin" , 0, 0));
    	fontRegistry.add(new FontRegistryEntry(0x288, 0x288, 0x2000, 0x2000, 0, 0, FONT_FAMILY_SANS_SERIF, FONT_STYLE_REGULAR    , 0, FONT_LANGUAGE_KOREAN  , 0, 3, "kr0.pgf"  , "AsiaNHH(512Johab)"     , 0, 0));
    }

    protected void loadDefaultSystemFont() {
        try {
            RandomAccessFile raf = new RandomAccessFile(fontDirPath + "/" + customFontFile, "r");
            raf.skipBytes(32);  // Skip custom header.
            char[] c = new char[(int) raf.length() - 32];
            for (int i = 0; i < c.length; i++) {
                c[i] = (char) (raf.readByte() & 0xFF);
            }
            Debug.Font.setDebugFont(c); // Set the internal debug font.
            Debug.Font.setDebugCharSize(8);
            Debug.Font.setDebugCharHeight(8);
            Debug.Font.setDebugCharWidth(8);
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
    	allFonts = new LinkedList<sceFont.Font>();

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
            		allFonts.add(font);
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
        protected HashMap<Integer, Font> fonts;
        protected int memFontAddr;
        protected int fileFontHandle;
        protected int altCharCode;
        protected float fontHRes = 128.f;
        protected float fontVRes = 128.f;

        public FontLib(int params) {
            read(params);
            fonts = new HashMap<Integer, sceFont.Font>();
        }

        public int getNumFonts() {
            return numFonts;
        }

        public int openFont(Font font) {
        	if (font == null) {
        		return SceKernelErrors.ERROR_FONT_INVALID_PARAMETER;
        	}
        	if (fonts.size() >= numFonts) {
        		return SceKernelErrors.ERROR_FONT_TOO_MANY_OPEN_FONTS;
        	}

        	int uid = SceUidManager.getNewUid(uidPurpose);
        	font = new Font(font, this, uid);
        	fonts.put(uid, font);
        	fontMap.put(uid, font);

        	return uid;
        }

        public void closeFont(Font font) {
        	int uid = font.fontHandle;
            SceUidManager.releaseUid(uid, uidPurpose);
            fonts.remove(uid);
            fontMap.remove(uid);

            font.fontHandle = -1;
            font.fontLib = null;
            font.pgf = null;
            font.fontInfo = null;
        }

        public void closeAllFonts() {
        	while (fonts.size() > 0) {
        		Font font = fonts.get(0);
        		closeFont(font);
        	}
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
    }

    protected boolean isFontMatchingStyle(Font font, pspFontStyle fontStyle) {
    	// Faking: always matching
    	return true;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        FontLib fl = null;
        int libHandle = 0;
        if (Memory.isAddressGood(paramsAddr)) {
            fl = new FontLib(paramsAddr);
        	libHandle = SceUidManager.getNewUid(uidPurpose);
            fontLibMap.put(libHandle, fl);
        }
        if (Memory.isAddressGood(errorCodeAddr)) {
            mem.write32(errorCodeAddr, 0);
        }
        cpu.gpr[2] = libHandle;
    }

    public void sceFontOpenUserFile(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int libHandle = cpu.gpr[4];
        int fileNameAddr = cpu.gpr[5];
        int mode = cpu.gpr[6];
        int errorCodeAddr = cpu.gpr[7];
        String fileName = Utilities.readStringZ(fileNameAddr);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontOpenUserFile libHandle=0x%08X, fileNameAddr=0x%08X ('%s'), mode=0x%08X, errorCodeAddr=0x%08X",
                    libHandle, fileNameAddr, fileName, mode, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            FontLib fLib = fontLibMap.get(libHandle);
            int fontHandle = 0;
            if (fLib != null) {
                fLib.triggerOpenCallback(fileNameAddr, errorCodeAddr);
                Font font = openFontFile(fileName);
                if (font != null) {
                	fontHandle = fLib.openFont(font);
                }
            }
            if (Memory.isAddressGood(errorCodeAddr)) {
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

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontOpenUserMemory libHandle=0x%08X, memoryFontAddr=0x%08X, memoryFontLength=%d, errorCodeAddr=0x%08X",
                    libHandle, memoryFontAddr, memoryFontLength, errorCodeAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            FontLib fLib = fontLibMap.get(libHandle);
            int fontHandle = 0;
            if (fLib != null) {
                fLib.triggerAllocCallback(memoryFontLength);
                Font font = openFontFile(memoryFontAddr, memoryFontLength);
                if (font != null) {
                    fontHandle = fLib.openFont(font);
                }
            }
            if (Memory.isAddressGood(errorCodeAddr)) {
                mem.write32(errorCodeAddr, 0);
            }
            cpu.gpr[2] = fontHandle;
        }
    }

    public void sceFontGetFontInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int fontHandle = cpu.gpr[4];
        int fontInfoAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetFontInfo fontHandle=0x%08X, fontInfoAddr=0x%08X", fontHandle, fontInfoAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(fontInfoAddr)) {
    		Font font = fontMap.get(fontHandle);
        	if (font != null) {
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
            }
        }
        cpu.gpr[2] = 0;
    }

    public void sceFontGetCharInfo(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int fontHandle = cpu.gpr[4];
        int charCode = cpu.gpr[5];
        int charInfoAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetCharInfo fontHandle=0x%08X, charCode=%04X (%c), charInfoAddr=%08X", fontHandle, charCode, (charCode <= 0xFF ? (char) charCode : '?'), charInfoAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(charInfoAddr)) {
        	pspCharInfo pspCharInfo = null;
        	if (getAllowInternalFonts() && fontMap.containsKey(fontHandle)) {
        		pspCharInfo = fontMap.get(fontHandle).fontInfo.getCharInfo(charCode);
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
        	pspCharInfo.write(mem, charInfoAddr);
            cpu.gpr[2] = 0;
        } else {
            cpu.gpr[2] = -1;
        }
    }

    public void sceFontGetCharGlyphImage(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int fontHandle = cpu.gpr[4];
        int charCode = cpu.gpr[5];
        int glyphImageAddr = cpu.gpr[6];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetCharGlyphImage fontHandle=0x%08X, charCode=%04X (%c), glyphImageAddr=%08X", fontHandle, charCode, (charCode <= 0xFF ? (char) charCode : '?'), glyphImageAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(glyphImageAddr)) {
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

            // If there's an internal font loaded, use it to display the text.
            // Otherwise, switch to our Debug font.
        	Font font = fontMap.get(fontHandle);
            if (getAllowInternalFonts() && font != null) {
                font.fontInfo.printFont(buffer, bytesPerLine, bufWidth, bufHeight,
                        xPosI, yPosI, pixelFormat, charCode, font.fontLib.getAltCharCode());
            } else {
                if (font != null) {
                    // Font adjustment.
                    // TODO: Instead of using the loaded PGF, figure out
                    // the proper values for the Debug font.
                    yPosI -= font.pgf.getMaxBaseYAdjust() >> 6;
                    yPosI += font.pgf.getMaxTopYAdjust() >> 6;
                    Debug.printFontbuffer(buffer, bytesPerLine, bufWidth, bufHeight,
                        xPosI, yPosI, pixelFormat, charCode, font.fontLib.getAltCharCode());
                }
            }
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
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Font style parameters.
        pspFontStyle fontStyle = new pspFontStyle();
        fontStyle.read(mem, fontStyleAddr);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontFindOptimumFont: %s", fontStyle.toString()));
        }

        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
        	int fontIndex = -1;
        	for (int i = 0; i < allFonts.size(); i++) {
        		if (isFontMatchingStyle(allFonts.get(i), fontStyle)) {
        			fontIndex = i;
        			break;
        		}
        	}
            if (Memory.isAddressGood(errorCodeAddr)) {
                mem.write32(errorCodeAddr, 0);
            }
            cpu.gpr[2] = fontIndex;
        }
    }

    public void sceFontClose(Processor processor) {
        CpuState cpu = processor.cpu;

        int fontHandle = cpu.gpr[4];

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontClose fontHandle=0x%08X", fontHandle));
        }

        Font font = fontMap.get(fontHandle);
        if (font != null && font.fontLib != null) {
        	font.fontLib.closeFont(font);
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
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            // Free all reserved font lib space and close all open font files.
        	FontLib fontLib = fontLibMap.get(libHandle);
            fontLib.triggerFreeCallback();
            fontLib.triggerCloseCallback();
            fontLib.closeAllFonts();
            SceUidManager.releaseUid(libHandle, uidPurpose);

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
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        FontLib fLib = fontLibMap.get(libHandle);
        int fontHandle = 0;
        if (fLib != null && index < allFonts.size()) {
        	Font font = allFonts.get(index);
        	fontHandle = fLib.openFont(font);
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("Opening '%s' - '%s', fontHandle=0x%08X", font.pgf.getFontName(), font.pgf.getFontType(), fontHandle));
        	}
        }
        if (Memory.isAddressGood(errorCodeAddr)) {
            mem.write32(errorCodeAddr, 0);
        }
        cpu.gpr[2] = fontHandle;
    }

    public void sceFontGetCharGlyphImage_Clip(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int fontHandle = cpu.gpr[4];
        int charCode = cpu.gpr[5];
        int glyphImageAddr = cpu.gpr[6];
        int clipXPos = cpu.gpr[7];
        int clipYPos = cpu.gpr[8];
        int clipWidth = cpu.gpr[9];
        int clipHeight = cpu.gpr[10];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontGetCharGlyphImage_Clip fontHandle=0x%08X, charCode=%04X (%c), glyphImageAddr=%08X" +
                    ", clipXPos=%d, clipYPos=%d, clipWidth=%d, clipHeight=%d,", fontHandle, charCode, (charCode <= 0xFF ? (char) charCode : '?'), glyphImageAddr, clipXPos, clipYPos, clipWidth, clipHeight));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Identical to sceFontGetCharGlyphImage, but uses a clipping
        // rectangle over the char.
        if (Memory.isAddressGood(glyphImageAddr)) {
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

            // If there's an internal font loaded, use it to display the text.
            // Otherwise, switch to our Debug font.
            Font font = fontMap.get(fontHandle);
            if (getAllowInternalFonts() && font != null) {
                font.fontInfo.printFont(buffer, bytesPerLine, bufWidth, bufHeight,
                        xPosI, yPosI, pixelFormat, charCode, font.fontLib.getAltCharCode());
            } else {
                if (font != null) {
                    // Font adjustment.
                    // TODO: Instead of using the loaded PGF, figure out
                    // the proper values for the Debug font.
                    yPosI -= font.pgf.getMaxBaseYAdjust() >> 6;
                    yPosI += font.pgf.getMaxTopYAdjust() >> 6;
                    if (yPosI < 0) {
                        yPosI = 0;
                    }
                    Debug.printFontbuffer(buffer, bytesPerLine, bufWidth, bufHeight,
                        xPosI, yPosI, pixelFormat, charCode, font.fontLib.getAltCharCode());
                }
            }
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
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            // Get all the available fonts
            int numFonts = allFonts.size();
            if (Memory.isAddressGood(errorCodeAddr)) {
                mem.write32(errorCodeAddr, 0);
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("sceFontGetNumFontList returning %d", numFonts));
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
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            int fonts = Math.min(allFonts.size(), numFonts);
            for (int i = 0; i < fonts; i++) {
                Font font = allFonts.get(i);
                pspFontStyle fontStyle = font.getFontStyle();
            	fontStyle.write(mem, fontStyleAddr);

                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceFontGetFontList returning font #%d at 0x%08X: %s", i, fontStyleAddr, fontStyle.toString()));
                }

                fontStyleAddr += fontStyle.sizeof();
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
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            FontLib fLib = fontLibMap.get(libHandle);
            fLib.setAltCharCode(charCode);
        }
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
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
        Memory mem = Processor.memory;

        int libHandle = cpu.gpr[4];
        float fontPointsH = cpu.fpr[12];
        int errorCodeAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontPointToPixelH libHandle=0x%08X, fontPointsH=%f, errorCodeAddr=0x%08X", libHandle, fontPointsH, errorCodeAddr));
        }

        int errorCode = 0;
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            errorCode = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
            cpu.fpr[0] = 0;
        } else {
        	FontLib fontLib = fontLibMap.get(libHandle);
	        // Convert horizontal floating points to pixels (Points Per Inch to Pixels Per Inch).
	        // points = (pixels * dpiX) / 72.
	        cpu.fpr[0] = fontPointsH * fontLib.fontHRes / pointDPI;
        }
        mem.write32(errorCodeAddr, errorCode);
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
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            if (Memory.isAddressGood(fontInfoAddr) && fontIndex < allFonts.size()) {
            	Font font = allFonts.get(fontIndex);
                if (font != null) {
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
                    mem.write32(fontInfoAddr + 84, currentPGF.getCharMapLength()); // Number of elements in the font's charmap.
                    mem.write32(fontInfoAddr + 88, currentPGF.getShadowMapLength());   // Number of elements in the font's shadow charmap.
                    // Font style (used by font comparison functions).
                    fontStyle.write(mem, fontInfoAddr + 92);
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
        float hRes = cpu.fpr[12];
        float vRes = cpu.fpr[13];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontSetResolution libHandle=0x%08X, hRes=%f, vRes=%f", libHandle, hRes, vRes));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
            FontLib fLib = fontLibMap.get(libHandle);
            fLib.fontHRes = hRes;
            fLib.fontVRes = vRes;
            cpu.gpr[2] = 0;
        }
    }

    public void sceFontFlush(Processor processor) {
        CpuState cpu = processor.cpu;

        int fontAddr = cpu.gpr[4];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontFlush fontAddr=0x%08X", fontAddr));
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
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
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        // Font style parameters.
        pspFontStyle fontStyle = new pspFontStyle();
        fontStyle.read(mem, fontStyleAddr);

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontFindFont: %s", fontStyle.toString()));
        }

        FontLib fLib = fontLibMap.get(libHandle);
        if (fLib == null) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            cpu.gpr[2] = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
        } else {
        	int fontIndex = -1;
        	for (int i = 0; i < allFonts.size(); i++) {
        		if (isFontMatchingStyle(allFonts.get(i), fontStyle)) {
        			fontIndex = i;
        			break;
        		}
        	}
            if (Memory.isAddressGood(errorCodeAddr)) {
                mem.write32(errorCodeAddr, 0);
            }
            cpu.gpr[2] = fontIndex;
        }
    }

    public void sceFontPointToPixelV(Processor processor) {
        CpuState cpu = processor.cpu;
    	Memory mem = Processor.memory;

        int libHandle = cpu.gpr[4];
        float fontPointsV = cpu.fpr[12];
        int errorCodeAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontPointToPixelV libHandle=0x%08X, fontPointsV=%f, errorCodeAddr=0x%08X", libHandle, fontPointsV, errorCodeAddr));
        }

        int errorCode = 0;
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            errorCode = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
            cpu.fpr[0] = 0;
        } else {
        	FontLib fontLib = fontLibMap.get(libHandle);
	        // Convert vertical floating points to pixels (Points Per Inch to Pixels Per Inch).
	        // points = (pixels * dpiX) / 72.
	        cpu.fpr[0] = fontPointsV * fontLib.fontVRes / pointDPI;
        }
        mem.write32(errorCodeAddr, errorCode);
    }

    public void sceFontPixelToPointH(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int libHandle = cpu.gpr[4];
        float fontPixelsH = cpu.fpr[12];
        int errorCodeAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontPixelToPointH libHandle=0x%08X, fontPixelsH=%f, errorCodeAddr=0x%08X", libHandle, fontPixelsH, errorCodeAddr));
        }

        int errorCode = 0;
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            errorCode = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
            cpu.fpr[0] = 0;
        } else {
        	FontLib fontLib = fontLibMap.get(libHandle);
            // Convert horizontal pixels to floating points (Pixels Per Inch to Points Per Inch).
            // points = (pixels / dpiX) * 72.
	        cpu.fpr[0] = fontPixelsH * pointDPI / fontLib.fontHRes;
        }
        mem.write32(errorCodeAddr, errorCode);
    }

    public void sceFontPixelToPointV(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Processor.memory;

        int libHandle = cpu.gpr[4];
        float fontPixelsV = cpu.fpr[12];
        int errorCodeAddr = cpu.gpr[5];

        if (log.isDebugEnabled()) {
            log.debug(String.format("sceFontPixelToPointV libHandle=0x%08X, fontPixelsV=%f, errorCodeAddr=0x%08X", libHandle, fontPixelsV, errorCodeAddr));
        }

        int errorCode = 0;
        if (!fontLibMap.containsKey(libHandle)) {
            log.warn("Unknown libHandle: 0x" + Integer.toHexString(libHandle));
            errorCode = SceKernelErrors.ERROR_FONT_INVALID_LIBID;
            cpu.fpr[0] = 0;
        } else {
        	FontLib fontLib = fontLibMap.get(libHandle);
            // Convert vertical pixels to floating points (Pixels Per Inch to Points Per Inch).
            // points = (pixels / dpiX) * 72.
	        cpu.fpr[0] = fontPixelsV * pointDPI / fontLib.fontVRes;
        }
        mem.write32(errorCodeAddr, errorCode);
    }

    public void sceFontCalcMemorySize(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("UNIMPLEMENTED: sceFontCalcMemorySize");

		cpu.gpr[2] = 0xDEADC0DE;
	}

    public void sceFontGetShadowImageRect(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("UNIMPLEMENTED: sceFontGetShadowImageRect");

		cpu.gpr[2] = 0xDEADC0DE;
	}

    public void sceFontGetShadowGlyphImage(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("UNIMPLEMENTED: sceFontGetShadowGlyphImage");

		cpu.gpr[2] = 0xDEADC0DE;
	}

    public void sceFontGetShadowGlyphImage_Clip(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("UNIMPLEMENTED: sceFontGetShadowGlyphImage_Clip");

		cpu.gpr[2] = 0xDEADC0DE;
	}

    public void sceFontGetShadowInfo(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("UNIMPLEMENTED: sceFontGetShadowInfo");

		cpu.gpr[2] = 0xDEADC0DE;
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
    public final HLEModuleFunction sceFontCalcMemorySizeFunction = new HLEModuleFunction("sceFont", "sceFontCalcMemorySize") {

        @Override
        public final void execute(Processor processor) {
            sceFontCalcMemorySize(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontCalcMemorySize(Processor);";
        }
    };
    public final HLEModuleFunction sceFontGetShadowImageRectFunction = new HLEModuleFunction("sceFont", "sceFontGetShadowImageRect") {

        @Override
        public final void execute(Processor processor) {
            sceFontGetShadowImageRect(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontGetShadowImageRect(Processor);";
        }
    };
    public final HLEModuleFunction sceFontGetShadowGlyphImageFunction = new HLEModuleFunction("sceFont", "sceFontGetShadowGlyphImage") {

        @Override
        public final void execute(Processor processor) {
            sceFontGetShadowGlyphImage(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontGetShadowGlyphImage(Processor);";
        }
    };
    public final HLEModuleFunction sceFontGetShadowGlyphImage_ClipFunction = new HLEModuleFunction("sceFont", "sceFontGetShadowGlyphImage_Clip") {

        @Override
        public final void execute(Processor processor) {
            sceFontGetShadowGlyphImage_Clip(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontGetShadowGlyphImage_Clip(Processor);";
        }
    };
    public final HLEModuleFunction sceFontGetShadowInfoFunction = new HLEModuleFunction("sceFont", "sceFontGetShadowInfo") {

        @Override
        public final void execute(Processor processor) {
            sceFontGetShadowInfo(processor);
        }

        @Override
        public final String compiledString() {
            return "jpcsp.HLE.Modules.sceFontModule.sceFontGetShadowInfo(Processor);";
        }
    };
}