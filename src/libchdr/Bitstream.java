/* license:BSD-3-Clause
 * copyright-holders:Aaron Giles
***************************************************************************
    bitstream.c
    Helper classes for reading/writing at the bit level.
***************************************************************************/
package libchdr;

import static jpcsp.util.Utilities.read8;

public class Bitstream {
	public int          buffer;       /* current bit accumulator */
	public int          bits;         /* number of bits in the accumulator */
	public byte[]       read;         /* read pointer */
	public int          readOffset;
	public int          doffset;      /* byte offset within the data */
	public int          dlength;      /* length of the data */

	public Bitstream(byte[] src, int srcOffset, int srcLength) {
		read = src;
		readOffset = srcOffset;
		dlength = srcLength;
	}

	public boolean overflow() {
		return ((doffset - bits / 8) > dlength);
	}

	/*-----------------------------------------------------
	 *  bitstream_peek - fetch the requested number of bits
	 *  but don't advance the input pointer
	 *-----------------------------------------------------
	 */
	public int peek(int numBits) {
		if (numBits == 0) {
			return 0;
		}

		/* fetch data if we need more */
		if (numBits > bits) {
			while (bits <= 24) {
				if (doffset < dlength) {
					buffer |= read8(read, readOffset + doffset) << (24 - bits);
				}
				doffset++;
				bits += 8;
			}
		}

		/* return the data */
		return buffer >>> (32 - numBits);
	}

	/*-----------------------------------------------------
	 *  bitstream_remove - advance the input pointer by the
	 *  specified number of bits
	 *-----------------------------------------------------
	 */
	public void remove(int numBits) {
		buffer <<= numBits;
		bits -= numBits;
	}

	/*-----------------------------------------------------
	 *  bitstream_read - fetch the requested number of bits
	 *-----------------------------------------------------
	 */
	public int read(int numBits)
	{
		int result = peek(numBits);
		remove(numBits);
		return result;
	}

	/*-------------------------------------------------
	 *  read_offset - return the current read offset
	 *-------------------------------------------------
	 */
	public int read_offset() {
		int result = doffset;
		int nbits = bits;
		while (nbits >= 8) {
			result--;
			nbits -= 8;
		}
		return result;
	}

	/*-------------------------------------------------
	 *  flush - flush to the nearest byte
	 *-------------------------------------------------
	 */
	public int flush() {
		while (bits >= 8) {
			doffset--;
			bits -= 8;
		}
		bits = buffer = 0;
		return doffset;
	}
}
