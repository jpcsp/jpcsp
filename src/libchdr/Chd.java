/***************************************************************************
    chd.c
    MAME Compressed Hunks of Data file format
****************************************************************************
    Copyright Aaron Giles
    All rights reserved.
    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are
    met:
        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in
          the documentation and/or other materials provided with the
          distribution.
        * Neither the name 'MAME' nor the names of its contributors may be
          used to endorse or promote products derived from this software
          without specific prior written permission.
    THIS SOFTWARE IS PROVIDED BY AARON GILES ''AS IS'' AND ANY EXPRESS OR
    IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
    WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL AARON GILES BE LIABLE FOR ANY DIRECT,
    INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
    (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
    HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
    STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
    IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
***************************************************************************/
package libchdr;

import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.read8;
import static jpcsp.util.Utilities.write8;
import static libchdr.Cdrom.CD_FRAME_SIZE;
import static libchdr.ChdHeader.CDROM_OLD_METADATA_TAG;
import static libchdr.ChdHeader.CDROM_TRACK_METADATA2_TAG;
import static libchdr.ChdHeader.CDROM_TRACK_METADATA_TAG;
import static libchdr.ChdHeader.CHDFLAGS_HAS_PARENT;
import static libchdr.ChdHeader.CHDFLAGS_IS_WRITEABLE;
import static libchdr.ChdHeader.CHDMETATAG_WILDCARD;
import static libchdr.ChdHeader.CHD_CODEC_CD_FLAC;
import static libchdr.ChdHeader.CHD_CODEC_CD_LZMA;
import static libchdr.ChdHeader.CHD_CODEC_CD_ZLIB;
import static libchdr.ChdHeader.CHD_CODEC_FLAC;
import static libchdr.ChdHeader.CHD_CODEC_LZMA;
import static libchdr.ChdHeader.CHD_CODEC_NONE;
import static libchdr.ChdHeader.CHD_CODEC_ZLIB;
import static libchdr.ChdHeader.CHD_HEADER_VERSION;
import static libchdr.ChdHeader.CHD_MAX_HEADER_SIZE;
import static libchdr.ChdHeader.CHD_MD5_BYTES;
import static libchdr.ChdHeader.CHD_OPEN_READ;
import static libchdr.ChdHeader.CHD_OPEN_READWRITE;
import static libchdr.ChdHeader.CHD_SHA1_BYTES;
import static libchdr.ChdHeader.CHD_V1_HEADER_SIZE;
import static libchdr.ChdHeader.CHD_V2_HEADER_SIZE;
import static libchdr.ChdHeader.CHD_V3_HEADER_SIZE;
import static libchdr.ChdHeader.CHD_V4_HEADER_SIZE;
import static libchdr.ChdHeader.CHD_V5_HEADER_SIZE;
import static libchdr.ChdHeader.GDROM_OLD_METADATA_TAG;
import static libchdr.ChdHeader.GDROM_TRACK_METADATA_TAG;
import static libchdr.ChdHeader.HARD_DISK_METADATA_FORMAT;
import static libchdr.ChdHeader.HARD_DISK_METADATA_TAG;
import static libchdr.ChdHeader.ChdError.CHDERR_CODEC_ERROR;
import static libchdr.ChdHeader.ChdError.CHDERR_DECOMPRESSION_ERROR;
import static libchdr.ChdHeader.ChdError.CHDERR_FILE_NOT_FOUND;
import static libchdr.ChdHeader.ChdError.CHDERR_FILE_NOT_WRITEABLE;
import static libchdr.ChdHeader.ChdError.CHDERR_HUNK_OUT_OF_RANGE;
import static libchdr.ChdHeader.ChdError.CHDERR_INVALID_DATA;
import static libchdr.ChdHeader.ChdError.CHDERR_INVALID_FILE;
import static libchdr.ChdHeader.ChdError.CHDERR_INVALID_PARAMETER;
import static libchdr.ChdHeader.ChdError.CHDERR_INVALID_PARENT;
import static libchdr.ChdHeader.ChdError.CHDERR_METADATA_NOT_FOUND;
import static libchdr.ChdHeader.ChdError.CHDERR_NONE;
import static libchdr.ChdHeader.ChdError.CHDERR_OUT_OF_MEMORY;
import static libchdr.ChdHeader.ChdError.CHDERR_READ_ERROR;
import static libchdr.ChdHeader.ChdError.CHDERR_REQUIRES_PARENT;
import static libchdr.ChdHeader.ChdError.CHDERR_UNSUPPORTED_FORMAT;
import static libchdr.ChdHeader.ChdError.CHDERR_UNSUPPORTED_VERSION;
import static libchdr.HuffmanDecoder.HuffmanError.HUFFERR_NONE;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.local.LocalVirtualFile;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.util.Utilities;
import libchdr.ChdHeader.ChdError;
import libchdr.HuffmanDecoder.HuffmanError;
import libchdr.codec.CdFlac;
import libchdr.codec.CdLzma;
import libchdr.codec.CdZlib;
import libchdr.codec.Flac;
import libchdr.codec.ICodecInterface;
import libchdr.codec.Lzma;
import libchdr.codec.Zlib;

public class Chd {
	public static Logger log = Logger.getLogger("libchdr");

	/***************************************************************************
    	CONSTANTS
	 ***************************************************************************/

	public static final int MAP_STACK_ENTRIES			= 512;			/* max number of entries to use on the stack */
	public static final int MAP_ENTRY_SIZE				= 16;			/* V3 and later */
	public static final int OLD_MAP_ENTRY_SIZE			= 8;			/* V1-V2 */
	public static final int METADATA_HEADER_SIZE		= 16;			/* metadata header size */

	public static final int MAP_ENTRY_FLAG_TYPE_MASK	= 0x0F;			/* what type of hunk */
	public static final int MAP_ENTRY_FLAG_NO_CRC		= 0x10;			/* no CRC is present */

	public static final int CHD_V1_SECTOR_SIZE			= 512;			/* size of a "sector" in the V1 header */

	public static final int COOKIE_VALUE = 0xBAADF00D;

	/* V3-V4 entry types */
	enum V34MapEntryType
	{
		V34_MAP_ENTRY_TYPE_INVALID,             /* invalid type */
		V34_MAP_ENTRY_TYPE_COMPRESSED,          /* standard compression */
		V34_MAP_ENTRY_TYPE_UNCOMPRESSED,        /* uncompressed data */
		V34_MAP_ENTRY_TYPE_MINI,                /* mini: use offset as raw data */
		V34_MAP_ENTRY_TYPE_SELF_HUNK,           /* same as another hunk in this file */
		V34_MAP_ENTRY_TYPE_PARENT_HUNK,         /* same as a hunk in the parent file */
		V34_MAP_ENTRY_TYPE_2ND_COMPRESSED       /* compressed with secondary algorithm (usually FLAC CDDA) */
	};

	/* V5 compression types */
	enum V5CompressionType
	{
		/* codec #0
		 * these types are live when running */
		COMPRESSION_TYPE_0,
		/* codec #1 */
		COMPRESSION_TYPE_1,
		/* codec #2 */
		COMPRESSION_TYPE_2,
		/* codec #3 */
		COMPRESSION_TYPE_3,
		/* no compression; implicit length = hunkbytes */
		COMPRESSION_NONE,
		/* same as another block in this chd */
		COMPRESSION_SELF,
		/* same as a hunk's worth of units in the parent chd */
		COMPRESSION_PARENT,

		/* start of small RLE run (4-bit length)
		 * these additional pseudo-types are used for compressed encodings: */
		COMPRESSION_RLE_SMALL,
		/* start of large RLE run (8-bit length) */
		COMPRESSION_RLE_LARGE,
		/* same as the last COMPRESSION_SELF block */
		COMPRESSION_SELF_0,
		/* same as the last COMPRESSION_SELF block + 1 */
		COMPRESSION_SELF_1,
		/* same block in the parent */
		COMPRESSION_PARENT_SELF,
		/* same as the last COMPRESSION_PARENT block */
		COMPRESSION_PARENT_0,
		/* same as the last COMPRESSION_PARENT block + 1 */
		COMPRESSION_PARENT_1
	};

	protected static class MapEntry {
		public long					offset;			/* offset within the file of the data */
		public int					crc;			/* 32-bit CRC of the data */
		public int					length;			/* length of the data */
		public int					flags;			/* misc flags */
	}

	protected static class MetaDataEntry {
		public long					offset;			/* offset within the file of the header */
		public long					next;			/* offset within the file of the next header */
		public long					prev;			/* offset within the file of the previous header */
		public int					length;			/* length of the metadata */
		public int					metatag;		/* metadata tag */
		public int					flags;			/* flag bits */
	}

	protected static class CodecInterface {
		public final int		compression;		/* type of compression */
		public final String	compname;				/* name of the algorithm */
		public final boolean	lossy;				/* is this a lossy algorithm? */
		public final ICodecInterface codec;

		public CodecInterface(int compression, String compname, ICodecInterface codec) {
			this.compression = compression;
			this.compname = compname;
			this.lossy = false;
			this.codec = codec;
		}

		@Override
		public String toString() {
			return compname;
		}
	}

	public static class CodecData {
		public byte[] buffer;
	}

	public static class ZlibCodecData extends CodecData {
	}

	public static class LzmaCodecData extends CodecData {
	}

	public static class FlacCodecData extends CodecData {
		public boolean nativeEndian;
	}

	public static class CdzlCodecData extends CodecData {
	}

	public static class CdlzCodecData extends CodecData {
	}

	public static class CdflCodecData extends CodecData {
		public boolean swapEndian;
	}

	public static class ChdFile {
		public int cookie;                                               /* cookie, should equal COOKIE_VALUE */
		public IVirtualFile file;                                        /* handle to the open core file */
		public ChdHeader header;                                         /* header, extracted from file */
		public ChdFile parent;                                           /* pointer to parent file, or NULL */
		public MapEntry map[];                                           /* array of map entries */
		public byte[] compressed;                                        /* pointer to buffer for compressed data */
		public final CodecInterface codecintf[] = new CodecInterface[4]; /* interface to the codec */
		public ZlibCodecData zlibCodecData = new ZlibCodecData();        /* zlib codec data */
		public LzmaCodecData lzmaCodecData = new LzmaCodecData();        /* lzma codec data */
		public FlacCodecData flacCodecData = new FlacCodecData();        /* flac codec data */
		public CdzlCodecData cdzlCodecData = new CdzlCodecData();        /* cdzl codec data */
		public CdlzCodecData cdlzCodecData = new CdlzCodecData();        /* cdlz codec data */
		public CdflCodecData cdflCodecData = new CdflCodecData();        /* cdfl codec data */

		@Override
		public String toString() {
			return String.format("ChdFile[file=%s, header=%s, parent=%s]", file, header, parent);
		}
	}

	/***************************************************************************
	    GLOBAL VARIABLES
	***************************************************************************/

	public static final byte[] nullmd5 = new byte[CHD_MD5_BYTES];
	public static final byte[] nullsha1 = new byte[CHD_SHA1_BYTES];

	/***************************************************************************
	    CODEC INTERFACES
	***************************************************************************/

	private static final CodecInterface codecInterfaces[] = new CodecInterface[] {
			/* "none" or no compression */
			new CodecInterface(ChdHeader.CHDCOMPRESSION_NONE, "none", null),
			/* standard zlib compression */
			new CodecInterface(ChdHeader.CHDCOMPRESSION_ZLIB, "zlib", new Zlib()),
			/* zlib+ compression */
			new CodecInterface(ChdHeader.CHDCOMPRESSION_ZLIB_PLUS, "zlib+", new Zlib()),
			/* V5 zlib compression */
			new CodecInterface(ChdHeader.CHD_CODEC_ZLIB, "zlib (Deflate)", new Zlib()),
			/* V5 lzma compression */
			new CodecInterface(ChdHeader.CHD_CODEC_LZMA, "lzma (LZMA)", new Lzma()),
			/* V5 flac compression */
			new CodecInterface(ChdHeader.CHD_CODEC_FLAC, "flac (FLAC)", new Flac()),
			/* V5 CD zlib compression */
			new CodecInterface(ChdHeader.CHD_CODEC_CD_ZLIB, "cdzl (CD Deflate)", new CdZlib()),
			/* V5 CD lzma compression */
			new CodecInterface(ChdHeader.CHD_CODEC_CD_LZMA, "cdlz (CD LZMA)", new CdLzma()),
			/* V5 CD flac compression */
			new CodecInterface(ChdHeader.CHD_CODEC_CD_FLAC, "cdfl (CD FLAC)", new CdFlac())
	};

	/***************************************************************************
	    INLINE FUNCTIONS
	***************************************************************************/

	/*-------------------------------------------------
	    get_bigendian_uint64 - fetch a UINT64 from
	    the data stream in bigendian order
	-------------------------------------------------*/

	private static long get_bigendian_uint64(byte[] base, int baseOffset) {
		return (get_bigendian_uint32(base, baseOffset) & 0xFFFFFFFFL) << 32 | (get_bigendian_uint32(base, baseOffset + 4) & 0xFFFFFFFFL);
	}

	/*-------------------------------------------------
	    put_bigendian_uint64 - write a UINT64 to
	    the data stream in bigendian order
	-------------------------------------------------*/

	private static void put_bigendian_uint64(byte[] base, int baseOffset, long value) {
		put_bigendian_uint32(base, baseOffset, (int) (value >> 32));
		put_bigendian_uint32(base, baseOffset + 4, (int) value);
	}

	/*-------------------------------------------------
	    get_bigendian_uint48 - fetch a UINT48 from
	    the data stream in bigendian order
	-------------------------------------------------*/

	private static long get_bigendian_uint48(byte[] base, int baseOffset) {
		return (get_bigendian_uint16(base, baseOffset) & 0xFFFFFFFFL) << 32 | (get_bigendian_uint32(base, baseOffset + 2) & 0xFFFFFFFFL);
	}

	/*-------------------------------------------------
	    put_bigendian_uint48 - write a UINT48 to
	    the data stream in bigendian order
	-------------------------------------------------*/

	private static void put_bigendian_uint48(byte[] base, int baseOffset, long value) {
		put_bigendian_uint16(base, baseOffset, (int) (value >> 32));
		put_bigendian_uint32(base, baseOffset + 2, (int) value);
	}

	/*-------------------------------------------------
	    get_bigendian_uint32 - fetch a UINT32 from
	    the data stream in bigendian order
	-------------------------------------------------*/

	private static int get_bigendian_uint32(byte[] base, int baseOffset) {
		return (read8(base, baseOffset) << 24) | (read8(base, baseOffset + 1) << 16) | (read8(base, baseOffset + 2) << 8) | read8(base, baseOffset + 3);
	}

	/*-------------------------------------------------
	    put_bigendian_uint32 - write a UINT32 to
	    the data stream in bigendian order
	-------------------------------------------------*/

	private static void put_bigendian_uint32(byte[] base, int baseOffset, int value) {
		base[baseOffset + 0] = (byte) (value >> 24);
		base[baseOffset + 1] = (byte) (value >> 16);
		base[baseOffset + 2] = (byte) (value >> 8);
		base[baseOffset + 3] = (byte) (value);
	}

	/*-------------------------------------------------
	    get_bigendian_uint24 - fetch a UINT24 from
	    the data stream in bigendian order
	-------------------------------------------------*/

	private static int get_bigendian_uint24(byte[] base, int baseOffset) {
		return (read8(base, baseOffset) << 16) | (read8(base, baseOffset + 1) << 8) | read8(base, baseOffset + 2);
	}

	/*-------------------------------------------------
	    put_bigendian_uint24 - write a UINT24 to
	    the data stream in bigendian order
	-------------------------------------------------*/

	private static void put_bigendian_uint24(byte[] base, int baseOffset, int value) {
		base[baseOffset + 0] = (byte) (value >> 16);
		base[baseOffset + 1] = (byte) (value >> 8);
		base[baseOffset + 2] = (byte) (value);
	}

	/*-------------------------------------------------
	    get_bigendian_uint16 - fetch a UINT16 from
	    the data stream in bigendian order
	-------------------------------------------------*/

	private static int get_bigendian_uint16(byte[] base, int baseOffset) {
		return (read8(base, baseOffset) << 8) | read8(base, baseOffset + 1);
	}

	/*-------------------------------------------------
	    put_bigendian_uint16 - write a UINT16 to
	    the data stream in bigendian order
	-------------------------------------------------*/

	private static void put_bigendian_uint16(byte[] base, int baseOffset, int value) {
		base[baseOffset + 0] = (byte) (value >> 8);
		base[baseOffset + 1] = (byte) (value);
	}

	/*-------------------------------------------------
	    map_extract - extract a single map
	    entry from the datastream
	-------------------------------------------------*/

	private void map_extract(byte[] base, int baseOffset, MapEntry entry) {
		entry.offset = get_bigendian_uint64(base, baseOffset + 0);
		entry.crc = get_bigendian_uint32(base, baseOffset + 8);
		entry.length = get_bigendian_uint16(base, baseOffset + 12) | (read8(base, baseOffset + 14) << 16);
		entry.flags = read8(base, baseOffset + 15);
	}

	/*-------------------------------------------------
	    map_size_v5 - calculate CHDv5 map size
	-------------------------------------------------*/
	private int map_size_v5(ChdHeader header) {
		return header.hunkcount * header.mapentrybytes;
	}
	
	/*-------------------------------------------------
	    crc16 - calculate CRC16 (from hashing.cpp)
	-------------------------------------------------*/
	private int crc16(byte[] data, int dataOffset, int length) {
		int crc = 0xffff;
	
		final int s_table[] = new int[] 
		{
			0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5, 0x60c6, 0x70e7,
			0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c, 0xd1ad, 0xe1ce, 0xf1ef,
			0x1231, 0x0210, 0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7, 0x62d6,
			0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c, 0xf3ff, 0xe3de,
			0x2462, 0x3443, 0x0420, 0x1401, 0x64e6, 0x74c7, 0x44a4, 0x5485,
			0xa56a, 0xb54b, 0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d,
			0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6, 0x5695, 0x46b4,
			0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df, 0xe7fe, 0xd79d, 0xc7bc,
			0x48c4, 0x58e5, 0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823,
			0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948, 0x9969, 0xa90a, 0xb92b,
			0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71, 0x0a50, 0x3a33, 0x2a12,
			0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a,
			0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03, 0x0c60, 0x1c41,
			0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a, 0xbd0b, 0x8d68, 0x9d49,
			0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70,
			0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a, 0x9f59, 0x8f78,
			0x9188, 0x81a9, 0xb1ca, 0xa1eb, 0xd10c, 0xc12d, 0xf14e, 0xe16f,
			0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046, 0x6067,
			0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c, 0xe37f, 0xf35e,
			0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235, 0x5214, 0x6277, 0x7256,
			0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c, 0xc50d,
			0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
			0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f, 0xf77e, 0xc71d, 0xd73c,
			0x26d3, 0x36f2, 0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634,
			0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9, 0xb98a, 0xa9ab,
			0x5844, 0x4865, 0x7806, 0x6827, 0x18c0, 0x08e1, 0x3882, 0x28a3,
			0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a,
			0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1, 0x1ad0, 0x2ab3, 0x3a92,
			0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa, 0xad8b, 0x9de8, 0x8dc9,
			0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1,
			0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba, 0x8fd9, 0x9ff8,
			0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93, 0x3eb2, 0x0ed1, 0x1ef0
		};
	
		/* fetch the current value into a local and rip through the source data */
		while (length-- != 0) {
			crc = ((crc << 8) & 0xFFFF) ^ s_table[(crc >> 8) ^ read8(data, dataOffset++)];
		}
		return crc & 0xFFFF;
	}
	
	/*-------------------------------------------------
		compressed - test if CHD file is compressed
	+-------------------------------------------------*/
	private boolean chd_compressed(ChdHeader header) {
		return header.compression[0] != CHD_CODEC_NONE;
	}
	
	/*-------------------------------------------------
		decompress_v5_map - decompress the v5 map
	-------------------------------------------------*/

	private ChdError decompress_v5_map(ChdFile chd, ChdHeader header) {
		int result = 0;
		int hunknum;
		int repcount = 0;
		int lastcomp = 0;
		int last_self = 0;
		long last_parent = 0;
		Bitstream bitbuf;
		int mapbytes;
		long firstoffs;
		int mapcrc;
		int lengthbits;
		int selfbits;
		int parentbits;
		byte[] compressed_ptr;
		final byte[] rawbuf = new byte[16];
		HuffmanDecoder decoder;
		HuffmanError err;
		long curoffset;	
		int rawmapsize = map_size_v5(header);

		if (!chd_compressed(header)) {
			header.rawmap = new byte[rawmapsize];
			chd.file.ioLseek(header.mapoffset);
			result = chd.file.ioRead(header.rawmap, 0, rawmapsize);
			if (result != rawmapsize) {
				return CHDERR_READ_ERROR;
			}
			return CHDERR_NONE;
		}

		/* read the reader */
		chd.file.ioLseek(header.mapoffset);
		result = chd.file.ioRead(rawbuf, 0, rawbuf.length);
		if (result != rawbuf.length) {
			return CHDERR_READ_ERROR;
		}
		mapbytes = get_bigendian_uint32(rawbuf, 0);
		firstoffs = get_bigendian_uint48(rawbuf, 4);
		mapcrc = get_bigendian_uint16(rawbuf, 10);
		lengthbits = read8(rawbuf, 12);
		selfbits = read8(rawbuf, 13);
		parentbits = read8(rawbuf, 14);

		/* now read the map */
		compressed_ptr = new byte[mapbytes];
		chd.file.ioLseek(header.mapoffset + 16);
		result = chd.file.ioRead(compressed_ptr, 0, mapbytes);
		if (result != mapbytes) {
			return CHDERR_READ_ERROR;
		}
		bitbuf = new Bitstream(compressed_ptr, 0, mapbytes);
		header.rawmap = new byte[rawmapsize];

		/* first decode the compression types */
		decoder = new HuffmanDecoder(16, 8);
		if (decoder == null) {
			return CHDERR_OUT_OF_MEMORY;
		}

		err = decoder.import_tree_rle(bitbuf);
		if (err != HUFFERR_NONE) {
			decoder.delete();
			return CHDERR_DECOMPRESSION_ERROR;
		}

		for (hunknum = 0; hunknum < header.hunkcount; hunknum++) {
			byte[] rawmap = header.rawmap;
			int rawmapOffset = 0 + (hunknum * 12);
			if (repcount > 0) {
				write8(rawmap, rawmapOffset + 0, lastcomp);
				repcount--;
			} else {
				int val = decoder.decode_one(bitbuf);
				if (val == V5CompressionType.COMPRESSION_RLE_SMALL.ordinal()) {
					write8(rawmap, rawmapOffset + 0, lastcomp);
					repcount = 2 + decoder.decode_one(bitbuf);
				} else if (val == V5CompressionType.COMPRESSION_RLE_LARGE.ordinal()) {
					write8(rawmap, rawmapOffset + 0, lastcomp);
					repcount = 2 + 16 + (decoder.decode_one(bitbuf) << 4);
					repcount += decoder.decode_one(bitbuf);
				} else {
					lastcomp = val;
					write8(rawmap, rawmapOffset + 0, lastcomp);
				}
			}
		}

		/* then iterate through the hunks and extract the needed data */
		curoffset = firstoffs;
		for (hunknum = 0; hunknum < header.hunkcount; hunknum++) {
			byte[] rawmap = header.rawmap;
			int rawmapOffset = 0 + (hunknum * 12);
			long offset = curoffset;
			int length = 0;
			int crc = 0;
			switch (V5CompressionType.values()[(read8(rawmap, rawmapOffset + 0))]) {
				/* base types */
				case COMPRESSION_TYPE_0:
				case COMPRESSION_TYPE_1:
				case COMPRESSION_TYPE_2:
				case COMPRESSION_TYPE_3:
					curoffset += length = bitbuf.read(lengthbits);
					crc = bitbuf.read(16);
					break;

				case COMPRESSION_NONE:
					curoffset += length = header.hunkbytes;
					crc = bitbuf.read(16);
					break;

				case COMPRESSION_SELF:
					offset = bitbuf.read(selfbits);
					last_self = (int) offset;
					break;

				case COMPRESSION_PARENT:
					offset = bitbuf.read(parentbits);
					last_parent = offset;
					break;

				/* pseudo-types; convert into base types */
				case COMPRESSION_SELF_1:
					last_self++;
				case COMPRESSION_SELF_0:
					write8(rawmap, rawmapOffset + 0, V5CompressionType.COMPRESSION_SELF.ordinal());
					offset = last_self;
					break;

				case COMPRESSION_PARENT_SELF:
					write8(rawmap, rawmapOffset + 0, V5CompressionType.COMPRESSION_PARENT.ordinal());
					last_parent = offset = ( ((long)hunknum) * ((long)header.hunkbytes) ) / header.unitbytes;
					break;

				case COMPRESSION_PARENT_1:
					last_parent += header.hunkbytes / header.unitbytes;
				case COMPRESSION_PARENT_0:
					write8(rawmap, rawmapOffset + 0, V5CompressionType.COMPRESSION_PARENT.ordinal());
					offset = last_parent;
					break;

				default:
					// Nothing to do
					break;
			}
			/* UINT24 length */
			put_bigendian_uint24(rawmap, rawmapOffset + 1, length);

			/* UINT48 offset */
			put_bigendian_uint48(rawmap, rawmapOffset + 4, offset);

			/* crc16 */
			put_bigendian_uint16(rawmap, rawmapOffset + 10, crc);
		}

		/* free memory */
		decoder.delete();

		/* verify the final CRC */
		if (crc16(header.rawmap, 0, header.hunkcount * 12) != mapcrc) {
			return CHDERR_DECOMPRESSION_ERROR;
		}

		return CHDERR_NONE;
	}

	/*-------------------------------------------------
	    map_extract_old - extract a single map
	    entry in old format from the datastream
	-------------------------------------------------*/
	
	private void map_extract_old(byte[] base, int baseOffset, MapEntry entry, int hunkbytes) {
		entry.offset = get_bigendian_uint64(base, baseOffset + 0);
		entry.crc = 0;
		entry.length = (int) (entry.offset >> 44);
		entry.flags = MAP_ENTRY_FLAG_NO_CRC | ((entry.length == hunkbytes) ? V34MapEntryType.V34_MAP_ENTRY_TYPE_UNCOMPRESSED.ordinal() : V34MapEntryType.V34_MAP_ENTRY_TYPE_COMPRESSED.ordinal());
		entry.offset = entry.offset & 0x00000FFFFFFFFFFFL;
	}

	public ChdError chd_open_file(String fileName, int mode, ChdFile parent, ChdFile chd[]) {
		if (fileName == null) {
			return CHDERR_INVALID_PARAMETER;
		}

		switch (mode) {
			case CHD_OPEN_READ:
				break;
			default:
				return CHDERR_INVALID_PARAMETER;
		}

		/* open the file */
		IVirtualFile vFile;
		try {
			vFile = new LocalVirtualFile(new SeekableRandomFile(fileName, "r"));
		} catch (FileNotFoundException e) {
			return CHDERR_FILE_NOT_FOUND;
		}

		ChdError err = chd_open_core_file(vFile, mode, parent, chd);
		if (err != CHDERR_NONE && vFile != null) {
			vFile.ioClose();
		}

		return err;
	}

	public void chd_close(ChdFile chd) {
		/* punt if NULL or invalid */
		if (chd == null || chd.cookie != COOKIE_VALUE) {
			return;
		}

		/* deinit the codec */
		if (chd.header.version < 5) {
			if (chd.codecintf[0] != null && chd.codecintf[0].codec != null) {
				chd.codecintf[0].codec.free(chd.zlibCodecData);
			}
		} else {
			int i;
			/* Free the codecs */
			for (i = 0 ; i < chd.codecintf.length; i++) {
				CodecData codec = null;

				if (chd.codecintf[i] == null) {
					continue;
				}

				int compression = chd.codecintf[i].compression;
				if (compression == CHD_CODEC_ZLIB) {
					codec = chd.zlibCodecData;
				} else if (compression == CHD_CODEC_LZMA) {
					codec = chd.lzmaCodecData;
				} else if (compression == CHD_CODEC_FLAC) {
					codec = chd.flacCodecData;
				} else if (compression == CHD_CODEC_CD_ZLIB) {
					codec = chd.cdzlCodecData;
				} else if (compression == CHD_CODEC_CD_LZMA) {
					codec = chd.cdlzCodecData;
				} else if (compression == CHD_CODEC_CD_FLAC) {
					codec = chd.cdflCodecData;
				}

				if (codec != null && chd.codecintf[i].codec != null) {
					chd.codecintf[i].codec.free(codec);
				}
			}

			/* Free the raw map */
			if (chd.header.rawmap != null) {
				chd.header.rawmap = null;
			}
		}

		/* free the compressed data buffer */
		if (chd.compressed != null) {
			chd.compressed = null;
		}

		/* free the hunk map */
		if (chd.map != null) {
			chd.map = null;
		}

		/* close the file */
		if (chd.file != null) {
			chd.file.ioClose();
		}

		if (chd.parent != null) {
			chd_close(chd.parent);
		}
	}

	/***************************************************************************
	    CHD HEADER MANAGEMENT
	***************************************************************************/
	
	/*-------------------------------------------------
	    chd_get_header - return a pointer to the
	    extracted header data
	-------------------------------------------------*/
	
	public ChdHeader chd_get_header(ChdFile chd) {
		/* punt if NULL or invalid */
		if (chd == null || chd.cookie != COOKIE_VALUE) {
			return null;
		}

		return chd.header;
	}

	/***************************************************************************
	    CORE DATA READ/WRITE
	***************************************************************************/
	
	/*-------------------------------------------------
	    chd_read - read a single hunk from the CHD
	    file
	-------------------------------------------------*/
	
	public ChdError chd_read(ChdFile chd, int hunknum, byte[] buffer, int bufferOffset) {
		/* punt if NULL or invalid */
		if (chd == null || chd.cookie != COOKIE_VALUE) {
			return CHDERR_INVALID_PARAMETER;
		}
	
		/* if we're past the end, fail */
		if (hunknum >= chd.header.totalhunks) {
			return CHDERR_HUNK_OUT_OF_RANGE;
		}
	
		/* perform the read */
		return hunk_read_into_memory(chd, hunknum, buffer, bufferOffset);
	}

	private ChdError chd_open_core_file(IVirtualFile file, int mode, ChdFile parent, ChdFile chd[]) {
		int intfnum;

		/* verify parameters */
		if (file == null) {
			return CHDERR_INVALID_PARAMETER;
		}

		/* punt if invalid parent */
		if (parent != null && parent.cookie != COOKIE_VALUE) {
			return CHDERR_INVALID_PARAMETER;
		}

		/* allocate memory for the final result */
		ChdFile newChd = new ChdFile();
		newChd.cookie = COOKIE_VALUE;
		newChd.parent = parent;
		newChd.file = file;

		/* now attempt to read the header */
		newChd.header = new ChdHeader();
		ChdError err = header_read(newChd, newChd.header);
		if (err != CHDERR_NONE) {
			return err;
		}

		/* validate the header */
		err = header_validate(newChd.header);
		if (err != CHDERR_NONE) {
			return err;
		}

		/* make sure we don't open a read-only file writeable */
		if (mode == CHD_OPEN_READWRITE && !(hasFlag(newChd.header.flags, CHDFLAGS_IS_WRITEABLE))) {
			return CHDERR_FILE_NOT_WRITEABLE;
		}

		/* also, never open an older version writeable */
		if (mode == CHD_OPEN_READWRITE && newChd.header.version < CHD_HEADER_VERSION) {
			return CHDERR_UNSUPPORTED_VERSION;
		}

		/* if we need a parent, make sure we have one */
		if (parent == null) {
			/* Detect parent requirement for versions below 5 */
			if (newChd.header.version < 5 && hasFlag(newChd.header.flags, CHDFLAGS_HAS_PARENT)) {
				return CHDERR_REQUIRES_PARENT;
			/* Detection for version 5 and above - if parentsha1 != 0, we have a parent */
			} else if (newChd.header.version >= 5 && !Arrays.equals(nullsha1, newChd.header.parentsha1)) {
				return CHDERR_REQUIRES_PARENT;
			}
		}

		/* make sure we have a valid parent */
		if (parent != null) {
			/* check MD5 if it isn't empty */
			if (!Arrays.equals(nullmd5, newChd.header.parentmd5) &&
				!Arrays.equals(nullmd5, newChd.parent.header.md5) &&
				!Arrays.equals(newChd.parent.header.md5, newChd.header.parentmd5)) {
				return CHDERR_INVALID_PARENT;
			}

			/* check SHA1 if it isn't empty */
			if (!Arrays.equals(nullsha1, newChd.header.parentsha1) &&
				!Arrays.equals(nullsha1, newChd.parent.header.sha1) &&
				!Arrays.equals(newChd.parent.header.sha1, newChd.header.parentsha1)) {
				return CHDERR_INVALID_PARENT;
			}
		}

		/* now read the hunk map */
		if (newChd.header.version < 5) {
			err = map_read(newChd);
			if (err != CHDERR_NONE) {
				return err;
			}
		} else {
			err = decompress_v5_map(newChd, newChd.header);
		}
		if (err != CHDERR_NONE) {
			return err;
		}


		/* allocate the temporary compressed buffer */
		newChd.compressed = new byte[newChd.header.hunkbytes];
		if (newChd.compressed == null) {
			return CHDERR_OUT_OF_MEMORY;
		}

		/* find the codec interface */
		if (newChd.header.version < 5) {
			for (intfnum = 0; intfnum < codecInterfaces.length; intfnum++) {
				if (codecInterfaces[intfnum].compression == newChd.header.compression[0]) {
					newChd.codecintf[0] = codecInterfaces[intfnum];
					break;
				}
			}

			if (intfnum == codecInterfaces.length) {
				return CHDERR_UNSUPPORTED_FORMAT;
			}

			/* initialize the codec */
			if (newChd.codecintf[0].codec != null) {
				err = newChd.codecintf[0].codec.init(newChd.zlibCodecData, newChd.header.hunkbytes);
				if (err != CHDERR_NONE) {
					return err;
				}
			}
		} else {
			int decompnum;
			/* verify the compression types and initialize the codecs */
			for (decompnum = 0; decompnum < newChd.header.compression.length; decompnum++) {
				int i;
				for (i = 0 ; i < codecInterfaces.length; i++) {
					if (codecInterfaces[i].compression == newChd.header.compression[decompnum]) {
						newChd.codecintf[decompnum] = codecInterfaces[i];
						break;
					}
				}

				if (newChd.codecintf[decompnum] == null && newChd.header.compression[decompnum] != 0) {
					return CHDERR_UNSUPPORTED_FORMAT;
				}

				/* initialize the codec */
				if (newChd.codecintf[decompnum].codec != null) {
					CodecData codec = null;
					int compression = newChd.header.compression[decompnum];
					if (compression == CHD_CODEC_ZLIB) {
						codec = newChd.zlibCodecData;
					} else if (compression == CHD_CODEC_LZMA) {
						codec = newChd.lzmaCodecData;
					} else if (compression == CHD_CODEC_FLAC) {
						codec = newChd.flacCodecData;
					} else if (compression == CHD_CODEC_CD_ZLIB) {
						codec = newChd.cdzlCodecData;
					} else if (compression == CHD_CODEC_CD_LZMA) {
						codec = newChd.cdlzCodecData;
					} else if (compression == CHD_CODEC_CD_FLAC) {
						codec = newChd.cdflCodecData;
					}

					if (codec == null) {
						return CHDERR_UNSUPPORTED_FORMAT;
					}

					err = newChd.codecintf[decompnum].codec.init(codec, newChd.header.hunkbytes);
					if (err != CHDERR_NONE) {
						return err;
					}
				}
			}
		}

		/* all done */
		chd[0] = newChd;

		return CHDERR_NONE;
	}

	/*-------------------------------------------------
    	header_guess_unitbytes - for older CHD formats,
    	guess at the bytes/unit based on metadata
	-------------------------------------------------*/

	private int header_guess_unitbytes(ChdFile chd) {
		/* look for hard disk metadata; if found, then the unit size == sector size */
		final byte metadata[] = new byte[512];
		int i0, i1, i2, i3;
		if (chd_get_metadata(chd, HARD_DISK_METADATA_TAG, 0, metadata, metadata.length, null, null, null) == CHDERR_NONE) {
			Pattern p = Pattern.compile("CYLS:(\\d+),HEADS:(\\\\d+),SECS:(\\\\d+),BPS:(\\\\d+)");
			Matcher m = p.matcher(new String(metadata));
			if (m.find()) {
				i0 = Integer.parseInt(m.group(1));
				i1 = Integer.parseInt(m.group(2));
				i2 = Integer.parseInt(m.group(3));
				i3 = Integer.parseInt(m.group(4));
				if (log.isDebugEnabled()) {
					log.debug(String.format("CD Header i0=%d, i1=%d, i2=%d, i3=%d", i0, i1, i2, i3));
				}
				return i3;
			}
		}

		/* look for CD-ROM metadata; if found, then the unit size == CD frame size */
		if (chd_get_metadata(chd, CDROM_OLD_METADATA_TAG, 0, metadata, metadata.length, null, null, null) == CHDERR_NONE ||
			chd_get_metadata(chd, CDROM_TRACK_METADATA_TAG, 0, metadata, metadata.length, null, null, null) == CHDERR_NONE ||
			chd_get_metadata(chd, CDROM_TRACK_METADATA2_TAG, 0, metadata, metadata.length, null, null, null) == CHDERR_NONE ||
			chd_get_metadata(chd, GDROM_OLD_METADATA_TAG, 0, metadata, metadata.length, null, null, null) == CHDERR_NONE ||
			chd_get_metadata(chd, GDROM_TRACK_METADATA_TAG, 0, metadata, metadata.length, null, null, null) == CHDERR_NONE)
			return CD_FRAME_SIZE;
	
		/* otherwise, just map 1:1 with the hunk size */
		return chd.header.hunkbytes;
	}


	/***************************************************************************
	    INTERNAL HEADER OPERATIONS
	***************************************************************************/
	
	/*-------------------------------------------------
	    header_validate - check the validity of a
	    CHD header
	-------------------------------------------------*/
	
	private ChdError header_validate(ChdHeader header) {
		int intfnum;
	
		/* require a valid version */
		if (header.version == 0 || header.version > CHD_HEADER_VERSION)
			return CHDERR_UNSUPPORTED_VERSION;
	
		/* require a valid length */
		if ((header.version == 1 && header.length != CHD_V1_HEADER_SIZE) ||
			(header.version == 2 && header.length != CHD_V2_HEADER_SIZE) ||
			(header.version == 3 && header.length != CHD_V3_HEADER_SIZE) ||
			(header.version == 4 && header.length != CHD_V4_HEADER_SIZE) ||
			(header.version == 5 && header.length != CHD_V5_HEADER_SIZE)) {
			return CHDERR_INVALID_PARAMETER;
		}
	
		/* Do not validate v5 header */
		if (header.version <= 4) {
			/* require valid flags */
			if (hasFlag(header.flags, ChdHeader.CHDFLAGS_UNDEFINED)) {
				return CHDERR_INVALID_PARAMETER;
			}
	
			/* require a supported compression mechanism */
			for (intfnum = 0; intfnum < codecInterfaces.length; intfnum++) {
				if (codecInterfaces[intfnum].compression == header.compression[0]) {
					break;
				}
			}
	
			if (intfnum == codecInterfaces.length) {
				return CHDERR_INVALID_PARAMETER;
			}
	
			/* require a valid hunksize */
			if (header.hunkbytes == 0 || header.hunkbytes >= 65536 * 256) {
				return CHDERR_INVALID_PARAMETER;
			}
	
			/* require a valid hunk count */
			if (header.totalhunks == 0) {
				return CHDERR_INVALID_PARAMETER;
			}
	
			/* require a valid MD5 and/or SHA1 if we're using a parent */
			if (hasFlag(header.flags, CHDFLAGS_HAS_PARENT) && Arrays.equals(header.parentmd5, nullmd5) && Arrays.equals(header.parentsha1, nullsha1)) {
				return CHDERR_INVALID_PARAMETER;
			}
	
			/* if we're V3 or later, the obsolete fields must be 0 */
			if (header.version >= 3 &&
				(header.obsolete_cylinders != 0 || header.obsolete_sectors != 0 ||
				 header.obsolete_heads != 0 || header.obsolete_hunksize != 0)) {
				return CHDERR_INVALID_PARAMETER;
			}
	
			/* if we're pre-V3, the obsolete fields must NOT be 0 */
			if (header.version < 3 &&
				(header.obsolete_cylinders == 0 || header.obsolete_sectors == 0 ||
				 header.obsolete_heads == 0 || header.obsolete_hunksize == 0)) {
				return CHDERR_INVALID_PARAMETER;
			}
		}
	
		return CHDERR_NONE;
	}

	/*-------------------------------------------------
    	header_read - read a CHD header into the
    	internal data structure
	-------------------------------------------------*/
	private ChdError header_read(ChdFile chd, ChdHeader header) {
		byte[] rawHeader = new byte[CHD_MAX_HEADER_SIZE];
		int count;

		/* punt if NULL */
		if (header == null) {
			return CHDERR_INVALID_PARAMETER;
		}

		/* punt if invalid file */
		if (chd.file == null) {
			return CHDERR_INVALID_FILE;
		}

		/* seek and read */
		chd.file.ioLseek(0L);
		count = chd.file.ioRead(rawHeader, 0, rawHeader.length);
		if (count != rawHeader.length) {
			return CHDERR_READ_ERROR;
		}

		/* verify the tag */
		if (!"MComprHD".equals(new String(rawHeader, 0, 8))) {
			return CHDERR_INVALID_DATA;
		}

		/* extract the direct data */
		header.length = get_bigendian_uint32(rawHeader, 8);
		header.version = get_bigendian_uint32(rawHeader, 12);

		/* make sure it's a version we understand */
		if (header.version == 0 || header.version > CHD_HEADER_VERSION) {
			return CHDERR_UNSUPPORTED_VERSION;
		}

		/* make sure the length is expected */
		if ((header.version == 1 && header.length != CHD_V1_HEADER_SIZE) ||
			(header.version == 2 && header.length != CHD_V2_HEADER_SIZE) ||
			(header.version == 3 && header.length != CHD_V3_HEADER_SIZE) ||
			(header.version == 4 && header.length != CHD_V4_HEADER_SIZE) ||
			(header.version == 5 && header.length != CHD_V5_HEADER_SIZE)) {
			return CHDERR_INVALID_DATA;
		}

		/* extract the common data */
		header.flags         	= get_bigendian_uint32(rawHeader, 16);
		header.compression[0]	= get_bigendian_uint32(rawHeader, 20);
		header.compression[1]	= CHD_CODEC_NONE;
		header.compression[2]	= CHD_CODEC_NONE;
		header.compression[3]	= CHD_CODEC_NONE;

		/* extract the V1/V2-specific data */
		if (header.version < 3) {
			int seclen = (header.version == 1) ? CHD_V1_SECTOR_SIZE : get_bigendian_uint32(rawHeader, 76);
			header.obsolete_hunksize  = get_bigendian_uint32(rawHeader, 24);
			header.totalhunks         = get_bigendian_uint32(rawHeader, 28);
			header.obsolete_cylinders = get_bigendian_uint32(rawHeader, 32);
			header.obsolete_heads     = get_bigendian_uint32(rawHeader, 36);
			header.obsolete_sectors   = get_bigendian_uint32(rawHeader, 40);
			System.arraycopy(rawHeader, 44, header.md5, 0, CHD_MD5_BYTES);
			System.arraycopy(rawHeader, 60, header.parentmd5, 0, CHD_MD5_BYTES);
			header.logicalbytes = (long) header.obsolete_cylinders * (long) header.obsolete_heads * (long) header.obsolete_sectors * (long) seclen;
			header.hunkbytes = seclen * header.obsolete_hunksize;
			header.unitbytes          = header_guess_unitbytes(chd);
			header.unitcount          = (header.logicalbytes + header.unitbytes - 1) / header.unitbytes;
			header.metaoffset = 0;
		/* extract the V3-specific data */
		} else if (header.version == 3) {
			header.totalhunks   = get_bigendian_uint32(rawHeader, 24);
			header.logicalbytes = get_bigendian_uint64(rawHeader, 28);
			header.metaoffset   = get_bigendian_uint64(rawHeader, 36);
			System.arraycopy(rawHeader, 44, header.md5, 0, CHD_MD5_BYTES);
			System.arraycopy(rawHeader, 60, header.parentmd5, 0, CHD_MD5_BYTES);
			header.hunkbytes    = get_bigendian_uint32(rawHeader, 76);
			header.unitbytes    = header_guess_unitbytes(chd);
			header.unitcount    = (header.logicalbytes + header.unitbytes - 1) / header.unitbytes;
			System.arraycopy(rawHeader, 80, header.sha1, 0, CHD_SHA1_BYTES);
			System.arraycopy(rawHeader, 100, header.parentsha1, 0, CHD_SHA1_BYTES);
		/* extract the V4-specific data */
		} else if (header.version == 4) {
			header.totalhunks   = get_bigendian_uint32(rawHeader, 24);
			header.logicalbytes = get_bigendian_uint64(rawHeader, 28);
			header.metaoffset   = get_bigendian_uint64(rawHeader, 36);
			header.hunkbytes    = get_bigendian_uint32(rawHeader, 44);
			header.unitbytes    = header_guess_unitbytes(chd);
			header.unitcount    = (header.logicalbytes + header.unitbytes - 1) / header.unitbytes;
			System.arraycopy(rawHeader, 48, header.sha1, 0, CHD_SHA1_BYTES);
			System.arraycopy(rawHeader, 68, header.parentsha1, 0, CHD_SHA1_BYTES);
			System.arraycopy(rawHeader, 88, header.rawsha1, 0, CHD_SHA1_BYTES);
		/* extract the V5-specific data */
		} else if (header.version == 5) {
			header.compression[0]  = get_bigendian_uint32(rawHeader, 16);
			header.compression[1]  = get_bigendian_uint32(rawHeader, 20);
			header.compression[2]  = get_bigendian_uint32(rawHeader, 24);
			header.compression[3]  = get_bigendian_uint32(rawHeader, 28);
			header.logicalbytes    = get_bigendian_uint64(rawHeader, 32);
			header.mapoffset       = get_bigendian_uint64(rawHeader, 40);
			header.metaoffset      = get_bigendian_uint64(rawHeader, 48);
			header.hunkbytes       = get_bigendian_uint32(rawHeader, 56);
			header.hunkcount       = (int) ((header.logicalbytes + header.hunkbytes - 1) / header.hunkbytes);
			header.unitbytes       = get_bigendian_uint32(rawHeader, 60);
			header.unitcount       = (header.logicalbytes + header.unitbytes - 1) / header.unitbytes;
			System.arraycopy(rawHeader, 84, header.sha1, 0, CHD_SHA1_BYTES);
			System.arraycopy(rawHeader, 104, header.parentsha1, 0, CHD_SHA1_BYTES);
			System.arraycopy(rawHeader, 64, header.rawsha1, 0, CHD_SHA1_BYTES);

			/* determine properties of map entries */
			header.mapentrybytes = chd_compressed(header) ? 12 : 4;

			/* hack */
			header.totalhunks 		= header.hunkcount;
		/* Unknown version */
		} else {
			log.error(String.format("Unknown version %d", header.version));
		}

		return ChdError.CHDERR_NONE;
	}

	/***************************************************************************
	    INTERNAL HUNK READ/WRITE
	***************************************************************************/

	/*-------------------------------------------------
	    hunk_read_compressed - read a compressed
	    hunk
	-------------------------------------------------*/

	private byte[] hunk_read_compressed(ChdFile chd, long offset, int size) {
		int bytes;
		chd.file.ioLseek(offset);
		bytes = chd.file.ioRead(chd.compressed, 0, size);
		if (bytes != size) {
			return null;
		}
		return chd.compressed;
	}
	
	/*-------------------------------------------------
	    hunk_read_uncompressed - read an uncompressed
	    hunk
	-------------------------------------------------*/
	
	private ChdError hunk_read_uncompressed(ChdFile chd, long offset, int size, byte[] dest, int destOffset) {
		int bytes;
		chd.file.ioLseek(offset);
		bytes = chd.file.ioRead(dest, destOffset, size);
		if (bytes != size) {
			return CHDERR_READ_ERROR;
		}
		return CHDERR_NONE;
	}
	
	/*-------------------------------------------------
	    hunk_read_into_memory - read a hunk into
	    memory at the given location
	-------------------------------------------------*/
	
	private ChdError hunk_read_into_memory(ChdFile chd, int hunknum, byte[] dest, int destOffset) {
		ChdError err;

		/* punt if no file */
		if (chd.file == null) {
			return CHDERR_INVALID_FILE;
		}

		/* return an error if out of range */
		if (hunknum >= chd.header.totalhunks) {
			return CHDERR_HUNK_OUT_OF_RANGE;
		}

		if (dest == null) {
			return CHDERR_INVALID_PARAMETER;
		}

		if (chd.header.version < 5) {
			MapEntry entry = chd.map[hunknum];
			int bytes;
			byte[] compressed_bytes;

			/* switch off the entry type */
			switch (V34MapEntryType.values()[entry.flags & MAP_ENTRY_FLAG_TYPE_MASK]) {
				/* compressed data */
				case V34_MAP_ENTRY_TYPE_COMPRESSED: {
					CodecData codec = null;

					/* read it into the decompression buffer */
					compressed_bytes = hunk_read_compressed(chd, entry.offset, entry.length);
					if (compressed_bytes == null) {
						return CHDERR_READ_ERROR;
					}

					/* now decompress using the codec */
					err = CHDERR_NONE;
					codec = chd.zlibCodecData;
					if (chd.codecintf[0].codec != null) {
						err = chd.codecintf[0].codec.decompress(codec, compressed_bytes, 0, entry.length, dest, destOffset, chd.header.hunkbytes);
					}
					if (err != CHDERR_NONE) {
						return err;
					}
					break;
				}

				/* uncompressed data */
				case V34_MAP_ENTRY_TYPE_UNCOMPRESSED:
					err = hunk_read_uncompressed(chd, entry.offset, chd.header.hunkbytes, dest, destOffset);
					if (err != CHDERR_NONE) {
						return err;
					}
					break;

				/* mini-compressed data */
				case V34_MAP_ENTRY_TYPE_MINI:
					put_bigendian_uint64(dest, destOffset + 0, entry.offset);
					for (bytes = 8; bytes < chd.header.hunkbytes; bytes++) {
						dest[destOffset + bytes] = dest[destOffset + bytes - 8];
					}
					break;

				/* self-referenced data */
				case V34_MAP_ENTRY_TYPE_SELF_HUNK:
					return hunk_read_into_memory(chd, (int) entry.offset, dest, destOffset);

				/* parent-referenced data */
				case V34_MAP_ENTRY_TYPE_PARENT_HUNK:
					err = hunk_read_into_memory(chd.parent, (int) entry.offset, dest, destOffset);
					if (err != CHDERR_NONE) {
						return err;
					}
					break;

				default:
					break;
			}
			return CHDERR_NONE;
		} else {
			CodecData codec = null;
			/* get a pointer to the map entry */
			long blockoffs;
			int blocklen;
			int blockcrc;
			byte[] rawmap = chd.header.rawmap;
			int rawmapOffset = chd.header.mapentrybytes * hunknum;
			byte[] compressed_bytes;

			/* uncompressed case */
			if (!chd_compressed(chd.header)) {
				blockoffs = (get_bigendian_uint32(rawmap, rawmapOffset) & 0xFFFFFFFFL) * (long)chd.header.hunkbytes;
				if (blockoffs != 0) {
					chd.file.ioLseek(blockoffs);
					int result = chd.file.ioRead(dest, destOffset, chd.header.hunkbytes);
					if (result != chd.header.hunkbytes) {
						return CHDERR_READ_ERROR;
					}
				} else if (chd.parent != null) {
					err = hunk_read_into_memory(chd.parent, hunknum, dest, destOffset);
					if (err != CHDERR_NONE) {
						return err;
					}
				} else {
					Arrays.fill(dest, destOffset, destOffset + chd.header.hunkbytes, (byte) 0);
				}
	
				return CHDERR_NONE;
			}

			/* compressed case */
			blocklen = get_bigendian_uint24(rawmap, rawmapOffset + 1);
			blockoffs = get_bigendian_uint48(rawmap, rawmapOffset + 4);
			blockcrc = get_bigendian_uint16(rawmap, rawmapOffset + 10);
			int compressionType = read8(rawmap, rawmapOffset + 0);
			switch (V5CompressionType.values()[compressionType]) {
				case COMPRESSION_TYPE_0:
				case COMPRESSION_TYPE_1:
				case COMPRESSION_TYPE_2:
				case COMPRESSION_TYPE_3:
					compressed_bytes = hunk_read_compressed(chd, blockoffs, blocklen);
					if (compressed_bytes == null) {
						return CHDERR_READ_ERROR;
					}
					int compression = chd.codecintf[compressionType].compression;
					if (compression == CHD_CODEC_ZLIB) {
						codec = chd.zlibCodecData;
					} else if (compression == CHD_CODEC_LZMA) {
						codec = chd.lzmaCodecData;
					} else if (compression == CHD_CODEC_FLAC) {
						codec = chd.flacCodecData;
					} else if (compression == CHD_CODEC_CD_ZLIB) {
						codec = chd.cdzlCodecData;
					} else if (compression == CHD_CODEC_CD_LZMA) {
						codec = chd.cdlzCodecData;
					} else if (compression == CHD_CODEC_CD_FLAC) {
						codec = chd.cdflCodecData;
					}
					if (codec == null) {
						return CHDERR_CODEC_ERROR;
					}
					if (log.isDebugEnabled()) {
						log.debug(String.format("V5 hunk#%d compressed with %s at 0x%X, size=0x%X, decompressed size=0x%X%s", hunknum, chd.codecintf[compressionType], blockoffs, blocklen, chd.header.hunkbytes, chd.parent != null ? ", from child": ""));
					}
					err = chd.codecintf[compressionType].codec.decompress(codec, compressed_bytes, 0, blocklen, dest, destOffset, chd.header.hunkbytes);
					if (err != CHDERR_NONE) {
						return err;
					}
					if (crc16(dest, destOffset, chd.header.hunkbytes) != blockcrc) {
						return CHDERR_DECOMPRESSION_ERROR;
					}
					return CHDERR_NONE;

				case COMPRESSION_NONE:
					if (log.isDebugEnabled()) {
						log.debug(String.format("V5 hunk#%d uncompressed 0x%X, size=0x%X", hunknum, blockoffs, blocklen));
					}
					err = hunk_read_uncompressed(chd, blockoffs, blocklen, dest, destOffset);
					if (err != CHDERR_NONE) {
						return err;
					}
					if (crc16(dest, destOffset, chd.header.hunkbytes) != blockcrc) {
						return CHDERR_DECOMPRESSION_ERROR;
					}
					return CHDERR_NONE;

				case COMPRESSION_SELF:
					if (log.isDebugEnabled()) {
						log.debug(String.format("V5 hunk#%d compression self #%d", hunknum, blockoffs));
					}
					return hunk_read_into_memory(chd, (int) blockoffs, dest, destOffset);

				case COMPRESSION_PARENT:
					if (chd.parent == null) {
						return CHDERR_REQUIRES_PARENT;
					}
					int units_in_hunk = chd.header.hunkbytes / chd.header.unitbytes;

					if (log.isDebugEnabled()) {
						log.debug(String.format("V5 hunk#%d compression parent #%d", hunknum, blockoffs / units_in_hunk));
					}

					/* blockoffs is aligned to units_in_hunk */
					if (blockoffs % units_in_hunk == 0) {
						return hunk_read_into_memory(chd.parent, (int) (blockoffs / units_in_hunk), dest, destOffset);
					/* blockoffs is not aligned to units_in_hunk */
					} else {
						int unit_in_hunk = (int) (blockoffs % units_in_hunk);
						final byte[] buf = new byte[chd.header.hunkbytes];
						/* Read first half of hunk which contains blockoffs */
						err = hunk_read_into_memory(chd.parent, (int) (blockoffs / units_in_hunk), buf, 0);
						if (err != CHDERR_NONE) {
							return err;
						}
						System.arraycopy(buf, unit_in_hunk * chd.header.unitbytes, dest, destOffset, (units_in_hunk - unit_in_hunk) * chd.header.unitbytes);
						/* Read second half of hunk which contains blockoffs */
						err = hunk_read_into_memory(chd.parent, (int) ((blockoffs / units_in_hunk) + 1), buf, 0);
						if (err != CHDERR_NONE) {
							return err;
						}
						System.arraycopy(buf, 0, dest, destOffset + (units_in_hunk - unit_in_hunk) * chd.header.unitbytes, unit_in_hunk * chd.header.unitbytes);
						break;
					}
				default:
					break;
			}
			return CHDERR_NONE;
		}

		/* We should not reach this code */
	}

	/***************************************************************************
    	METADATA MANAGEMENT
	 ***************************************************************************/

	/*-------------------------------------------------
	    chd_get_metadata - get the indexed metadata
	    of the given type
	-------------------------------------------------*/

	public ChdError chd_get_metadata(ChdFile chd, int searchtag, int searchindex, byte[] output, int outputlen, int[] resultlen, int[] resulttag, int[] resultflags) {
		MetaDataEntry metaentry = new MetaDataEntry();
		ChdError err;
		int count;

		/* if we didn't find it, just return */
		err = metadata_find_entry(chd, searchtag, searchindex, metaentry);
		if (err != CHDERR_NONE) {
			/* unless we're an old version and they are requesting hard disk metadata */
			if (chd.header.version < 3 && (searchtag == HARD_DISK_METADATA_TAG || searchtag == CHDMETATAG_WILDCARD) && searchindex == 0) {
				/* fill in the faux metadata */
				String s = String.format(HARD_DISK_METADATA_FORMAT, chd.header.obsolete_cylinders, chd.header.obsolete_heads, chd.header.obsolete_sectors, chd.header.hunkbytes / chd.header.obsolete_hunksize);
				byte[] faux_metadata = s.getBytes();
				faux_metadata = Utilities.add(faux_metadata, (byte) 0);
				int faux_length = faux_metadata.length;

				/* copy the metadata itself */
				System.arraycopy(faux_metadata, 0, output, 0, Math.min(outputlen, faux_length));

				/* return the length of the data and the tag */
				if (resultlen != null) {
					resultlen[0] = faux_length;
				}
				if (resulttag != null) {
					resulttag[0] = HARD_DISK_METADATA_TAG;
				}
				return CHDERR_NONE;
			}
			return err;
		}

		/* read the metadata */
		outputlen = Math.min(outputlen, metaentry.length);
		chd.file.ioLseek(metaentry.offset + METADATA_HEADER_SIZE);
		count = chd.file.ioRead(output, 0, outputlen);
		if (count != outputlen) {
			return CHDERR_READ_ERROR;
		}

		/* return the length of the data and the tag */
		if (resultlen != null) {
			resultlen[0] = metaentry.length;
		}
		if (resulttag != null) {
			resulttag[0] = metaentry.metatag;
		}
		if (resultflags != null) {
			resultflags[0] = metaentry.flags;
		}
		return CHDERR_NONE;
	}

	/*-------------------------------------------------
	    metadata_find_entry - find a metadata entry
	-------------------------------------------------*/
	
	private ChdError metadata_find_entry(ChdFile chd, int metatag, int metaindex, MetaDataEntry metaentry) {
		/* start at the beginning */
		metaentry.offset = chd.header.metaoffset;
		metaentry.prev = 0;

		/* loop until we run out of options */
		while (metaentry.offset != 0) {
			final byte raw_meta_header[] = new byte[METADATA_HEADER_SIZE];
			int	count;

			/* read the raw header */
			chd.file.ioLseek(metaentry.offset);
			count = chd.file.ioRead(raw_meta_header, 0, raw_meta_header.length);
			if (count != raw_meta_header.length) {
				break;
			}

			/* extract the data */
			metaentry.metatag = get_bigendian_uint32(raw_meta_header, 0);
			metaentry.length = get_bigendian_uint32(raw_meta_header, 4);
			metaentry.next = get_bigendian_uint64(raw_meta_header, 8);

			/* flags are encoded in the high byte of length */
			metaentry.flags = (metaentry.length >> 24) & 0xFF;
			metaentry.length &= 0x00ffffff;

			/* if we got a match, proceed */
			if (metatag == CHDMETATAG_WILDCARD || metaentry.metatag == metatag) {
				if (metaindex-- == 0) {
					return CHDERR_NONE;
				}
			}

			/* no match, fetch the next link */
			metaentry.prev = metaentry.offset;
			metaentry.offset = metaentry.next;
		}

		/* if we get here, we didn't find it */
		return CHDERR_METADATA_NOT_FOUND;
	}

	/***************************************************************************
	    INTERNAL MAP ACCESS
	***************************************************************************/
	
	/*-------------------------------------------------
	    map_read - read the initial sector map
	-------------------------------------------------*/
	
	private ChdError map_read(ChdFile chd) {
		int entrysize = (chd.header.version < 3) ? OLD_MAP_ENTRY_SIZE : MAP_ENTRY_SIZE;
		final byte[] raw_map_entries = new byte[MAP_STACK_ENTRIES * MAP_ENTRY_SIZE];
		long fileoffset, maxoffset = 0;
		final byte[] cookie = new byte[MAP_ENTRY_SIZE];
		int count;

		/* first allocate memory */
		chd.map = new MapEntry[chd.header.totalhunks];
		if (chd.map == null) {
			return CHDERR_OUT_OF_MEMORY;
		}
	
		/* read the map entries in in chunks and extract to the map list */
		fileoffset = chd.header.length;
		for (int i = 0; i < chd.header.totalhunks; i += MAP_STACK_ENTRIES) {
			/* compute how many entries this time */
			int entries = chd.header.totalhunks - i, j;
			if (entries > MAP_STACK_ENTRIES) {
				entries = MAP_STACK_ENTRIES;
			}
	
			/* read that many */
			chd.file.ioLseek(fileoffset);
			count = chd.file.ioRead(raw_map_entries, 0, entries * entrysize);
			if (count != entries * entrysize) {
				return CHDERR_READ_ERROR;
			}
			fileoffset += entries * entrysize;
	
			/* process that many */
			if (entrysize == MAP_ENTRY_SIZE) {
				for (j = 0; j < entries; j++) {
					map_extract(raw_map_entries, j * MAP_ENTRY_SIZE, chd.map[i + j]);
				}
			} else {
				for (j = 0; j < entries; j++) {
					map_extract_old(raw_map_entries, j * OLD_MAP_ENTRY_SIZE, chd.map[i + j], chd.header.hunkbytes);
				}
			}
	
			/* track the maximum offset */
			for (j = 0; j < entries; j++) {
				if ((chd.map[i + j].flags & MAP_ENTRY_FLAG_TYPE_MASK) == V34MapEntryType.V34_MAP_ENTRY_TYPE_COMPRESSED.ordinal() ||
					(chd.map[i + j].flags & MAP_ENTRY_FLAG_TYPE_MASK) == V34MapEntryType.V34_MAP_ENTRY_TYPE_UNCOMPRESSED.ordinal()) {
					maxoffset = Math.max(maxoffset, chd.map[i + j].offset + chd.map[i + j].length);
				}
			}
		}
	
		/* verify the cookie */
		chd.file.ioLseek(fileoffset);
		count = chd.file.ioRead(cookie, 0, entrysize);
		if (count != entrysize || !"EndOfListCookie".equals(new String(cookie, 0, entrysize))) {
			return CHDERR_INVALID_FILE;
		}
	
		/* verify the length */
		if (maxoffset > chd.file.length()) {
			return CHDERR_INVALID_FILE;
		}

		return CHDERR_NONE;
	}
}
