#ifndef __LIBFONT_H
	#define __LIBFONT_H

	typedef u32 FontLibraryHandle;
	typedef u32 FontHandle;

	typedef struct {
		u32* userDataAddr;
		u32  numFonts;
		u32* cacheDataAddr;

		// Driver callbacks.
		void *(*allocFuncAddr)(void *, u32);
		void  (*freeFuncAddr )(void *, void *);
		u32* openFuncAddr;
		u32* closeFuncAddr;
		u32* readFuncAddr;
		u32* seekFuncAddr;
		u32* errorFuncAddr;
		u32* ioFinishFuncAddr;
	} FontNewLibParams;

	typedef enum {
		FONT_FAMILY_SANS_SERIF = 1,
		FONT_FAMILY_SERIF      = 2,
	} Family;
	
	typedef enum {
		FONT_STYLE_REGULAR     = 1,
		FONT_STYLE_ITALIC      = 2,
		FONT_STYLE_BOLD        = 5,
		FONT_STYLE_BOLD_ITALIC = 6,
		FONT_STYLE_DB          = 103, // Demi-Bold / semi-bold
	} Style;
	
	typedef enum {
		FONT_LANGUAGE_JAPANESE = 1,
		FONT_LANGUAGE_LATIN    = 2,
		FONT_LANGUAGE_KOREAN   = 3,
	} Language;

	typedef struct {
		float  fontH;
		float  fontV;
		float  fontHRes;
		float  fontVRes;
		float  fontWeight;
		u16    fontFamily;
		u16    fontStyle;
		// Check.
		u16    fontStyleSub;
		u16    fontLanguage;
		u16    fontRegion;
		u16    fontCountry;
		char   fontName[64];
		char   fontFileName[64];
		u32    fontAttributes;
		u32    fontExpire;
	} FontStyle;

	typedef struct {
		// Glyph metrics (in 26.6 signed fixed-point).
		u32 maxGlyphWidthI;
		u32 maxGlyphHeightI;
		u32 maxGlyphAscenderI;
		u32 maxGlyphDescenderI;
		u32 maxGlyphLeftXI;
		u32 maxGlyphBaseYI;
		u32 minGlyphCenterXI;
		u32 maxGlyphTopYI;
		u32 maxGlyphAdvanceXI;
		u32 maxGlyphAdvanceYI;

		// Glyph metrics (replicated as float).
		float maxGlyphWidthF;
		float maxGlyphHeightF;
		float maxGlyphAscenderF;
		float maxGlyphDescenderF;
		float maxGlyphLeftXF;
		float maxGlyphBaseYF;
		float minGlyphCenterXF;
		float maxGlyphTopYF;
		float maxGlyphAdvanceXF;
		float maxGlyphAdvanceYF;
		
		// Bitmap dimensions.
		short maxGlyphWidth;
		short maxGlyphHeight;
		u32  charMapLength;   // Number of elements in the font's charmap.
		u32  shadowMapLength; // Number of elements in the font's shadow charmap.
		
		// Font style (used by font comparison functions).
		FontStyle fontStyle;
		
		u8 BPP; // Font's BPP.
		u8 pad[3];
	} FontInfo;
	
	/**
	 * Creates a new font library.
	 *
	 * @param  params     Parameters of the new library.
	 * @param  errorCode  Pointer to store any error code.
	 *
	 * @return FontLibraryHandle
	 */
	FontLibraryHandle sceFontNewLib(FontNewLibParams *params, uint *errorCode);

	/**
	 * Releases the font library.
	 *
	 * @param  libHandle  Handle of the library.
	 *
	 * @return 0 on success
	 */
	int sceFontDoneLib(FontLibraryHandle libHandle);
	
	/**
	 * Opens a new font.
	 *
	 * @param  libHandle  Handle of the library.
	 * @param  index      Index of the font.
	 * @param  mode       Mode for opening the font.
	 * @param  errorCode  Pointer to store any error code.
	 *
	 * @return FontHandle
	 */
	FontHandle sceFontOpen(FontLibraryHandle libHandle, int index, int mode, uint *errorCode);

	/**
	 * Opens a new font from memory.
	 *
	 * @param  libHandle         Handle of the library.
	 * @param  memoryFontAddr    Index of the font.
	 * @param  memoryFontLength  Mode for opening the font.
	 * @param  errorCode         Pointer to store any error code.
	 *
	 * @return FontHandle
	 */
	FontHandle sceFontOpenUserMemory(FontLibraryHandle libHandle, void *memoryFontAddr, int memoryFontLength, uint *errorCode);
	
	/**
	 * Opens a new font from a file.
	 *
	 * @param  libHandle  Handle of the library.
	 * @param  fileName   Path to the font file to open.
	 * @param  mode       Mode for opening the font.
	 * @param  errorCode  Pointer to store any error code.
	 *
	 * @return FontHandle
	 */
	FontHandle sceFontOpenUserFile(FontLibraryHandle libHandle, char *fileName, int mode, uint *errorCode);

	/**
	 * Closes the specified font file.
	 *
	 * @param  fontHandle  Handle of the font.
	 *
	 * @return 0 on success.
	 */
	int sceFontClose(FontHandle fontHandle);

	/**
	 * Returns the number of available fonts.
	 *
	 * @param  libHandle  Handle of the library.
	 * @param  errorCode  Pointer to store any error code.
	 *
	 * @return Number of fonts
	 */
	int sceFontGetNumFontList(FontLibraryHandle libHandle, uint *errorCode);

	/**
	 * Returns a font index that best matches the specified FontStyle.
	 *
	 * @param  libHandle  Handle of the library.
	 * @param  fontStyle  Family, style and 
	 * @param  errorCode  Pointer to store any error code.
	 *
	 * @return Font index
	 */
	int sceFontFindOptimumFont(FontLibraryHandle libHandle, FontStyle *fontStyle, uint *errorCode);

	/**
	 * Returns a font index that best matches the specified FontStyle.
	 *
	 * @param  libHandle  Handle of the library.
	 * @param  fontStyle  Family, style and language.
	 * @param  errorCode  Pointer to store any error code.
	 *
	 * @return Font index
	 */
	int sceFontFindFont(FontLibraryHandle libHandle, FontStyle *fontStyle, uint *errorCode);

	/**
	 * Obtains the FontInfo of a FontHandle.
	 *
	 * @param  fontHandle  Font Handle to get the information from.
	 * @param  fontInfo    Pointer to a FontInfo structure that will hold the information.
	 *
	 * @return 0 on success
	 */
	int sceFontGetFontInfo(FontHandle fontHandle, FontInfo *fontInfo);
	
	/**
	 * Obtains the FontInfo of a Font with its index.
	 *
	 * @param  libHandle  Handle of the library.
	 * @param  fontInfo   Pointer to a FontInfo structure that will hold the information.
	 * @param  unknown    ???
	 * @param  fontIndex  Index of the font to get the information from.
	 *
	 * @return 0 on success
	 */
	int sceFontGetFontInfoByIndexNumber(FontLibraryHandle libHandle, FontInfo *fontInfo, int unknown, int fontIndex);

#endif