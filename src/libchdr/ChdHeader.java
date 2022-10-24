/***************************************************************************
    chd.h
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


/***************************************************************************
Compressed Hunks of Data header format. All numbers are stored in
Motorola (big-endian) byte ordering. The header is 76 (V1) or 80 (V2)
bytes long.
V1 header:
[  0] char   tag[8];        // 'MComprHD'
[  8] UINT32 length;        // length of header (including tag and length fields)
[ 12] UINT32 version;       // drive format version
[ 16] UINT32 flags;         // flags (see below)
[ 20] UINT32 compression;   // compression type
[ 24] UINT32 hunksize;      // 512-byte sectors per hunk
[ 28] UINT32 totalhunks;    // total # of hunks represented
[ 32] UINT32 cylinders;     // number of cylinders on hard disk
[ 36] UINT32 heads;         // number of heads on hard disk
[ 40] UINT32 sectors;       // number of sectors on hard disk
[ 44] UINT8  md5[16];       // MD5 checksum of raw data
[ 60] UINT8  parentmd5[16]; // MD5 checksum of parent file
[ 76] (V1 header length)
V2 header:
[  0] char   tag[8];        // 'MComprHD'
[  8] UINT32 length;        // length of header (including tag and length fields)
[ 12] UINT32 version;       // drive format version
[ 16] UINT32 flags;         // flags (see below)
[ 20] UINT32 compression;   // compression type
[ 24] UINT32 hunksize;      // seclen-byte sectors per hunk
[ 28] UINT32 totalhunks;    // total # of hunks represented
[ 32] UINT32 cylinders;     // number of cylinders on hard disk
[ 36] UINT32 heads;         // number of heads on hard disk
[ 40] UINT32 sectors;       // number of sectors on hard disk
[ 44] UINT8  md5[16];       // MD5 checksum of raw data
[ 60] UINT8  parentmd5[16]; // MD5 checksum of parent file
[ 76] UINT32 seclen;        // number of bytes per sector
[ 80] (V2 header length)
V3 header:
[  0] char   tag[8];        // 'MComprHD'
[  8] UINT32 length;        // length of header (including tag and length fields)
[ 12] UINT32 version;       // drive format version
[ 16] UINT32 flags;         // flags (see below)
[ 20] UINT32 compression;   // compression type
[ 24] UINT32 totalhunks;    // total # of hunks represented
[ 28] UINT64 logicalbytes;  // logical size of the data (in bytes)
[ 36] UINT64 metaoffset;    // offset to the first blob of metadata
[ 44] UINT8  md5[16];       // MD5 checksum of raw data
[ 60] UINT8  parentmd5[16]; // MD5 checksum of parent file
[ 76] UINT32 hunkbytes;     // number of bytes per hunk
[ 80] UINT8  sha1[20];      // SHA1 checksum of raw data
[100] UINT8  parentsha1[20];// SHA1 checksum of parent file
[120] (V3 header length)
V4 header:
[  0] char   tag[8];        // 'MComprHD'
[  8] UINT32 length;        // length of header (including tag and length fields)
[ 12] UINT32 version;       // drive format version
[ 16] UINT32 flags;         // flags (see below)
[ 20] UINT32 compression;   // compression type
[ 24] UINT32 totalhunks;    // total # of hunks represented
[ 28] UINT64 logicalbytes;  // logical size of the data (in bytes)
[ 36] UINT64 metaoffset;    // offset to the first blob of metadata
[ 44] UINT32 hunkbytes;     // number of bytes per hunk
[ 48] UINT8  sha1[20];      // combined raw+meta SHA1
[ 68] UINT8  parentsha1[20];// combined raw+meta SHA1 of parent
[ 88] UINT8  rawsha1[20];   // raw data SHA1
[108] (V4 header length)
Flags:
    0x00000001 - set if this drive has a parent
    0x00000002 - set if this drive allows writes
=========================================================================
V5 header:
[  0] char   tag[8];        // 'MComprHD'
[  8] uint32_t length;        // length of header (including tag and length fields)
[ 12] uint32_t version;       // drive format version
[ 16] uint32_t compressors[4];// which custom compressors are used?
[ 32] uint64_t logicalbytes;  // logical size of the data (in bytes)
[ 40] uint64_t mapoffset;     // offset to the map
[ 48] uint64_t metaoffset;    // offset to the first blob of metadata
[ 56] uint32_t hunkbytes;     // number of bytes per hunk (512k maximum)
[ 60] uint32_t unitbytes;     // number of bytes per unit within each hunk
[ 64] uint8_t  rawsha1[20];   // raw data SHA1
[ 84] uint8_t  sha1[20];      // combined raw+meta SHA1
[104] uint8_t  parentsha1[20];// combined raw+meta SHA1 of parent
[124] (V5 header length)
If parentsha1 != 0, we have a parent (no need for flags)
If compressors[0] == 0, we are uncompressed (including maps)
V5 uncompressed map format:
[  0] uint32_t offset;        // starting offset / hunk size
V5 compressed map format header:
[  0] uint32_t length;        // length of compressed map
[  4] UINT48 datastart;     // offset of first block
[ 10] uint16_t crc;           // crc-16 of the map
[ 12] uint8_t lengthbits;     // bits used to encode complength
[ 13] uint8_t hunkbits;       // bits used to encode self-refs
[ 14] uint8_t parentunitbits; // bits used to encode parent unit refs
[ 15] uint8_t reserved;       // future use
[ 16] (compressed header length)
Each compressed map entry, once expanded, looks like:
[  0] uint8_t compression;    // compression type
[  1] UINT24 complength;    // compressed length
[  4] UINT48 offset;        // offset
[ 10] uint16_t crc;           // crc-16 of the data
***************************************************************************/
public class ChdHeader {
	/***************************************************************************
    	CONSTANTS
	 ***************************************************************************/

	/* header information */
	public static final int CHD_HEADER_VERSION			= 5;
	public static final int CHD_V1_HEADER_SIZE			= 76;
	public static final int CHD_V2_HEADER_SIZE			= 80;
	public static final int CHD_V3_HEADER_SIZE			= 120;
	public static final int CHD_V4_HEADER_SIZE			= 108;
	public static final int CHD_V5_HEADER_SIZE			= 124;
	public static final int CHD_MAX_HEADER_SIZE			= CHD_V5_HEADER_SIZE;

	/* checksumming information */
	public static final int CHD_MD5_BYTES				= 16;
	public static final int CHD_SHA1_BYTES				= 20;

	/* CHD global flags */
	public static final int CHDFLAGS_HAS_PARENT			= 0x00000001;
	public static final int CHDFLAGS_IS_WRITEABLE		= 0x00000002;
	public static final int CHDFLAGS_UNDEFINED			= 0xfffffffc;

	public static int CHD_MAKE_TAG(char a, char b, char c, char d) {
		return (((a) << 24) | ((b) << 16) | ((c) << 8) | (d));
	}

	/* compression types */
	public static final int CHDCOMPRESSION_NONE			= 0;
	public static final int CHDCOMPRESSION_ZLIB			= 1;
	public static final int CHDCOMPRESSION_ZLIB_PLUS	= 2;
	public static final int CHDCOMPRESSION_AV			= 3;

	public static final int CHD_CODEC_NONE              = 0;
	public static final int CHD_CODEC_ZLIB				= CHD_MAKE_TAG('z','l','i','b');
	public static final int CHD_CODEC_LZMA				= CHD_MAKE_TAG('l','z','m','a');
	public static final int CHD_CODEC_FLAC				= CHD_MAKE_TAG('f','l','a','c');
	/* general codecs with CD frontend */
	public static final int CHD_CODEC_CD_ZLIB			= CHD_MAKE_TAG('c','d','z','l');
	public static final int CHD_CODEC_CD_LZMA			= CHD_MAKE_TAG('c','d','l','z');
	public static final int CHD_CODEC_CD_FLAC			= CHD_MAKE_TAG('c','d','f','l');

	/* A/V codec configuration parameters */
	public static final int AV_CODEC_COMPRESS_CONFIG	= 1;
	public static final int AV_CODEC_DECOMPRESS_CONFIG	= 2;

	/* metadata parameters */
	public static final int CHDMETATAG_WILDCARD			= 0;
	public static final int CHD_METAINDEX_APPEND		= -1;

	/* metadata flags */
	public static final int CHD_MDFLAGS_CHECKSUM		= 0x01;		/* indicates data is checksummed */

	/* standard hard disk metadata */
	public static final int    HARD_DISK_METADATA_TAG		= CHD_MAKE_TAG('G','D','D','D');
	public static final String HARD_DISK_METADATA_FORMAT	= "CYLS:%d,HEADS:%d,SECS:%d,BPS:%d";

	/* hard disk identify information */
	public static final int    HARD_DISK_IDENT_METADATA_TAG = CHD_MAKE_TAG('I','D','N','T');

	/* hard disk key information */
	public static final int    HARD_DISK_KEY_METADATA_TAG	= CHD_MAKE_TAG('K','E','Y',' ');

	/* pcmcia CIS information */
	public static final int    PCMCIA_CIS_METADATA_TAG		= CHD_MAKE_TAG('C','I','S',' ');

	/* standard CD-ROM metadata */
	public static final int    CDROM_OLD_METADATA_TAG		= CHD_MAKE_TAG('C','H','C','D');
	public static final int    CDROM_TRACK_METADATA_TAG     = CHD_MAKE_TAG('C','H','T','R');
	public static final String CDROM_TRACK_METADATA_FORMAT	= "TRACK:%d TYPE:%s SUBTYPE:%s FRAMES:%d";
	public static final int    CDROM_TRACK_METADATA2_TAG	= CHD_MAKE_TAG('C','H','T','2');
	public static final String CDROM_TRACK_METADATA2_FORMAT	= "TRACK:%d TYPE:%s SUBTYPE:%s FRAMES:%d PREGAP:%d PGTYPE:%s PGSUB:%s POSTGAP:%d";
	public static final int    GDROM_OLD_METADATA_TAG		= CHD_MAKE_TAG('C','H','G','T');
	public static final int    GDROM_TRACK_METADATA_TAG     = CHD_MAKE_TAG('C', 'H', 'G', 'D');
	public static final String GDROM_TRACK_METADATA_FORMAT	= "TRACK:%d TYPE:%s SUBTYPE:%s FRAMES:%d PAD:%d PREGAP:%d PGTYPE:%s PGSUB:%s POSTGAP:%d";

	/* standard A/V metadata */
	public static final int AV_METADATA_TAG				= CHD_MAKE_TAG('A','V','A','V');
	public static final String AV_METADATA_FORMAT		= "FPS:%d.%06d WIDTH:%d HEIGHT:%d INTERLACED:%d CHANNELS:%d SAMPLERATE:%d";

	/* A/V laserdisc frame metadata */
	public static final int AV_LD_METADATA_TAG			= CHD_MAKE_TAG('A','V','L','D');

	/* CHD open values */
	public static final int CHD_OPEN_READ				= 1;
	public static final int CHD_OPEN_READWRITE			= 2;

	public enum ChdError {
		CHDERR_NONE,
		CHDERR_NO_INTERFACE,
		CHDERR_OUT_OF_MEMORY,
		CHDERR_INVALID_FILE,
		CHDERR_INVALID_PARAMETER,
		CHDERR_INVALID_DATA,
		CHDERR_FILE_NOT_FOUND,
		CHDERR_REQUIRES_PARENT,
		CHDERR_FILE_NOT_WRITEABLE,
		CHDERR_READ_ERROR,
		CHDERR_WRITE_ERROR,
		CHDERR_CODEC_ERROR,
		CHDERR_INVALID_PARENT,
		CHDERR_HUNK_OUT_OF_RANGE,
		CHDERR_DECOMPRESSION_ERROR,
		CHDERR_COMPRESSION_ERROR,
		CHDERR_CANT_CREATE_FILE,
		CHDERR_CANT_VERIFY,
		CHDERR_NOT_SUPPORTED,
		CHDERR_METADATA_NOT_FOUND,
		CHDERR_INVALID_METADATA_SIZE,
		CHDERR_UNSUPPORTED_VERSION,
		CHDERR_VERIFY_INCOMPLETE,
		CHDERR_INVALID_METADATA,
		CHDERR_INVALID_STATE,
		CHDERR_OPERATION_PENDING,
		CHDERR_NO_ASYNC_OPERATION,
		CHDERR_UNSUPPORTED_FORMAT
	};

	public int		  length;									/* length of header data */
	public int		  version;									/* drive format version */
	public int		  flags;									/* flags field */
	public final int  compression[] = new int[4];				/* compression type */
	public int		  hunkbytes;								/* number of bytes per hunk */
	public int		  totalhunks;								/* total # of hunks represented */
	public long		  logicalbytes;								/* logical size of the data */
	public long		  metaoffset;								/* offset in file of first metadata */
	public long		  mapoffset;								/* TOOD V5 */
	public final byte md5[] = new byte[CHD_MD5_BYTES];			/* overall MD5 checksum */
	public final byte parentmd5[] = new byte[CHD_MD5_BYTES];	/* overall MD5 checksum of parent */
	public final byte sha1[] = new byte[CHD_SHA1_BYTES];		/* overall SHA1 checksum */
	public final byte rawsha1[] = new byte[CHD_SHA1_BYTES];		/* SHA1 checksum of raw data */
	public final byte parentsha1[] = new byte[CHD_SHA1_BYTES];	/* overall SHA1 checksum of parent */
	public int		  unitbytes;								/* TODO V5 */
	public long		  unitcount;								/* TODO V5 */
    public int        hunkcount;                  				/* TODO V5 */

    /* map information */
    public int      mapentrybytes;              				/* length of each entry in a map (V5) */
    public byte     rawmap[];                   				/* raw map data */

	public int		obsolete_cylinders;							/* obsolete field -- do not use! */
	public int		obsolete_sectors;							/* obsolete field -- do not use! */
	public int		obsolete_heads;								/* obsolete field -- do not use! */
	public int		obsolete_hunksize;							/* obsolete field -- do not use! */

	@Override
	public String toString() {
		return String.format("ChdHeader[version=%d, hunkbytes=%d, totalhunks=%d]", version, hunkbytes, totalhunks);
	}
}
