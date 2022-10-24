/* license:BSD-3-Clause
 * copyright-holders:Aaron Giles
 ***************************************************************************
    huffman.h
    Static Huffman compression and decompression helpers.

****************************************************************************

    Maximum codelength is officially (alphabetsize - 1). This would be 255 bits
    (since we use 1 byte values). However, it is also dependent upon the number
    of samples used, as follows:
         2 bits -> 3..4 samples
         3 bits -> 5..7 samples
         4 bits -> 8..12 samples
         5 bits -> 13..20 samples
         6 bits -> 21..33 samples
         7 bits -> 34..54 samples
         8 bits -> 55..88 samples
         9 bits -> 89..143 samples
        10 bits -> 144..232 samples
        11 bits -> 233..376 samples
        12 bits -> 377..609 samples
        13 bits -> 610..986 samples
        14 bits -> 987..1596 samples
        15 bits -> 1597..2583 samples
        16 bits -> 2584..4180 samples   -> note that a 4k data size guarantees codelength <= 16 bits
        17 bits -> 4181..6764 samples
        18 bits -> 6765..10945 samples
        19 bits -> 10946..17710 samples
        20 bits -> 17711..28656 samples
        21 bits -> 28657..46367 samples
        22 bits -> 46368..75024 samples
        23 bits -> 75025..121392 samples
        24 bits -> 121393..196417 samples
        25 bits -> 196418..317810 samples
        26 bits -> 317811..514228 samples
        27 bits -> 514229..832039 samples
        28 bits -> 832040..1346268 samples
        29 bits -> 1346269..2178308 samples
        30 bits -> 2178309..3524577 samples
        31 bits -> 3524578..5702886 samples
        32 bits -> 5702887..9227464 samples
    Looking at it differently, here is where powers of 2 fall into these buckets:
          256 samples -> 11 bits max
          512 samples -> 12 bits max
           1k samples -> 14 bits max
           2k samples -> 15 bits max
           4k samples -> 16 bits max
           8k samples -> 18 bits max
          16k samples -> 19 bits max
          32k samples -> 21 bits max
          64k samples -> 22 bits max
         128k samples -> 24 bits max
         256k samples -> 25 bits max
         512k samples -> 27 bits max
           1M samples -> 28 bits max
           2M samples -> 29 bits max
           4M samples -> 31 bits max
           8M samples -> 32 bits max
****************************************************************************
    Delta-RLE encoding works as follows:
    Starting value is assumed to be 0. All data is encoded as a delta
    from the previous value, such that final[i] = final[i - 1] + delta.
    Long runs of 0s are RLE-encoded as follows:
        0x100 = repeat count of 8
        0x101 = repeat count of 9
        0x102 = repeat count of 10
        0x103 = repeat count of 11
        0x104 = repeat count of 12
        0x105 = repeat count of 13
        0x106 = repeat count of 14
        0x107 = repeat count of 15
        0x108 = repeat count of 16
        0x109 = repeat count of 32
        0x10a = repeat count of 64
        0x10b = repeat count of 128
        0x10c = repeat count of 256
        0x10d = repeat count of 512
        0x10e = repeat count of 1024
        0x10f = repeat count of 2048
    Note that repeat counts are reset at the end of a row, so if a 0 run
    extends to the end of a row, a large repeat count may be used.
    The reason for starting the run counts at 8 is that 0 is expected to
    be the most common symbol, and is typically encoded in 1 or 2 bits.

***************************************************************************/
package libchdr;

import java.util.Arrays;

public class HuffmanDecoder {
	protected int		numcodes;             /* number of total codes being processed */
	protected int		maxbits;              /* maximum bits per code */
	protected int 		prevdata;             /* value of the previous data (for delta-RLE encoding) */
	protected int      	rleremaining;         /* number of RLE bytes remaining (for delta-RLE encoding) */
	protected int[]	 	lookup;               /* pointer to the lookup table */
	protected Node[]	huffnode;             /* array of nodes */
	protected int[]     datahisto;            /* histogram of data values */

	public enum HuffmanError {
		HUFFERR_NONE,
		HUFFERR_TOO_MANY_BITS,
		HUFFERR_INVALID_DATA,
		HUFFERR_INPUT_BUFFER_TOO_SMALL,
		HUFFERR_OUTPUT_BUFFER_TOO_SMALL,
		HUFFERR_INTERNAL_INCONSISTENCY,
		HUFFERR_TOO_MANY_CONTEXTS
	}

	protected static class Node implements Comparable<Node> {
		Node		parent;		/* pointer to parent node */
		int			count;		/* number of hits on this node */
		int			weight;		/* assigned weight of this node */
		int			bits;		/* bits used to encode the node */
		int			numbits;	/* number of bits needed for this node */

		@Override
		public int compareTo(Node node) {
			if (node.weight != weight) {
				return node.weight - weight;
			}
			return bits - node.bits;
		}
	}

	private static int MAKE_LOOKUP(int code, int bits) {
		return (code << 5) | (bits & 0x1F);
	}

	public HuffmanDecoder(int numcodes, int maxbits) {
		this.numcodes = numcodes;
		this.maxbits = maxbits;
		lookup = new int[1 << maxbits];
		huffnode = new Node[numcodes];
		for (int i = 0; i < numcodes; i++) {
			huffnode[i] = new Node();
		}
	}

	public void delete() {
		lookup = null;
		huffnode = null;
	}

	public int decode_one(Bitstream bitbuf) {
		/* peek ahead to get maxbits worth of data */
		int bits = bitbuf.peek(maxbits);

		/* look it up, then remove the actual number of bits for this code */
		int lookupValue = lookup[bits];
		bitbuf.remove(lookupValue & 0x1F);

		/* return the value */
		return lookupValue >> 5;
	}

	/*-------------------------------------------------
	 *  import_tree_rle - import an RLE-encoded
	 *  huffman tree from a source data stream
	 *-------------------------------------------------
	 */

	public HuffmanError import_tree_rle(Bitstream bitbuf) {
		int numbits;
		int curnode;
		HuffmanError error;

		/* bits per entry depends on the maxbits */
		if (maxbits >= 16) {
			numbits = 5;
		} else if (maxbits >= 8) {
			numbits = 4;
		} else {
			numbits = 3;
		}

		/* loop until we read all the nodes */
		for (curnode = 0; curnode < numcodes; ) {
			/* a non-one value is just raw */
			int nodebits = bitbuf.read(numbits);
			if (nodebits != 1) {
				huffnode[curnode++].numbits = nodebits;
			/* a one value is an escape code */
			} else {
				/* a double 1 is just a single 1 */
				nodebits = bitbuf.read(numbits);
				if (nodebits == 1) {
					huffnode[curnode++].numbits = nodebits;
				/* otherwise, we need one for value for the repeat count */
				} else {
					int repcount = bitbuf.read(numbits) + 3;
					while (repcount-- != 0) {
						huffnode[curnode++].numbits = nodebits;
					}
				}
			}
		}

		/* make sure we ended up with the right number */
		if (curnode != numcodes) {
			return HuffmanError.HUFFERR_INVALID_DATA;
		}

		/* assign canonical codes for all nodes based on their code lengths */
		error = assign_canonical_codes();
		if (error != HuffmanError.HUFFERR_NONE) {
			return error;
		}

		/* build the lookup table */
		build_lookup_table();

		/* determine final input length and report errors */
		return bitbuf.overflow() ? HuffmanError.HUFFERR_INPUT_BUFFER_TOO_SMALL : HuffmanError.HUFFERR_NONE;
	}


	/*-------------------------------------------------
	 *  import_tree_huffman - import a huffman-encoded
	 *  huffman tree from a source data stream
	 *-------------------------------------------------
	 */

	public HuffmanError import_tree_huffman(Bitstream bitbuf) {
		int start;
		int last = 0;
		int count = 0;
		int index;
		int curcode;
		int rlefullbits = 0;
		int temp;
		HuffmanError error;

		/* start by parsing the lengths for the small tree */
		HuffmanDecoder smallhuff = new HuffmanDecoder(24, 6);
		smallhuff.huffnode[0].numbits = bitbuf.read(3);
		start = bitbuf.read(3) + 1;
		for (index = 1; index < 24; index++)
		{
			if (index < start || count == 7)
				smallhuff.huffnode[index].numbits = 0;
			else
			{
				count = bitbuf.read(3);
				smallhuff.huffnode[index].numbits = (count == 7) ? 0 : count;
			}
		}

		/* then regenerate the tree */
		error = smallhuff.assign_canonical_codes();
		if (error != HuffmanError.HUFFERR_NONE)
			return error;
		smallhuff.build_lookup_table();

		/* determine the maximum length of an RLE count */
		temp = numcodes - 9;
		while (temp != 0) {
			temp >>= 1;
			rlefullbits++;
		}

		/* now process the rest of the data */
		for (curcode = 0; curcode < numcodes; ) {
			int value = smallhuff.decode_one(bitbuf);
			if (value != 0) {
				huffnode[curcode++].numbits = last = value - 1;
			} else {
				count = bitbuf.read(3) + 2;
				if (count == 7+2) {
					count += bitbuf.read(rlefullbits);
				}
				for ( ; count != 0 && curcode < numcodes; count--) {
					huffnode[curcode++].numbits = last;
				}
			}
		}

		/* make sure we ended up with the right number */
		if (curcode != numcodes) {
			return HuffmanError.HUFFERR_INVALID_DATA;
		}

		/* assign canonical codes for all nodes based on their code lengths */
		error = assign_canonical_codes();
		if (error != HuffmanError.HUFFERR_NONE) {
			return error;
		}

		/* build the lookup table */
		build_lookup_table();

		/* determine final input length and report errors */
		return bitbuf.overflow() ? HuffmanError.HUFFERR_INPUT_BUFFER_TOO_SMALL : HuffmanError.HUFFERR_NONE;
	}

	/*-------------------------------------------------
	 *  compute_tree_from_histo - common backend for
	 *  computing a tree based on the data histogram
	 *-------------------------------------------------
	 */

	public HuffmanError compute_tree_from_histo() {
		int i;
		int lowerweight;
		int upperweight;
		/* compute the number of data items in the histogram */
		int sdatacount = 0;
		for (i = 0; i < numcodes; i++) {
			sdatacount += datahisto[i];
		}

		/* binary search to achieve the optimum encoding */
		lowerweight = 0;
		upperweight = sdatacount * 2;
		while (true) {
			/* build a tree using the current weight */
			int curweight = (upperweight + lowerweight) / 2;
			int curmaxbits = build_tree(sdatacount, curweight);

			/* apply binary search here */
			if (curmaxbits <= maxbits) {
				lowerweight = curweight;

				/* early out if it worked with the raw weights, or if we're done searching */
				if (curweight == sdatacount || (upperweight - lowerweight) <= 1) {
					break;
				}
			} else {
				upperweight = curweight;
			}
		}

		/* assign canonical codes for all nodes based on their code lengths */
		return assign_canonical_codes();
	}

	/***************************************************************************
	 *  INTERNAL FUNCTIONS
	 ***************************************************************************
	 */

	/*-------------------------------------------------
	 *  build_tree - build a huffman tree based on the
	 *  data distribution
	 *-------------------------------------------------
	 */

	private int build_tree(int totaldata, int totalweight) {
		int curcode;
		int nextalloc;
		int listitems = 0;
		int maxbits = 0;
		/* make a list of all non-zero nodes */
		Node[] list = new Node[numcodes * 2];
		for (int i = 0; i < numcodes; i++) {
			huffnode[i] = new Node();
		}
		for (curcode = 0; curcode < numcodes; curcode++) {
			if (datahisto[curcode] != 0) {
				list[listitems++] = huffnode[curcode];
				huffnode[curcode].count = datahisto[curcode];
				huffnode[curcode].bits = curcode;

				/* scale the weight by the current effective length, ensuring we don't go to 0 */
				huffnode[curcode].weight = (int) (((long)datahisto[curcode]) * ((long)totalweight) / ((long)totaldata));
				if (huffnode[curcode].weight == 0) {
					huffnode[curcode].weight = 1;
				}
			}
		}

		/* sort the list by weight, largest weight first */
		Arrays.sort(list, 0, listitems);

		/* now build the tree */
		nextalloc = numcodes;
		while (listitems > 1) {
			int curitem;
			/* remove lowest two items */
			Node node1 = list[--listitems];
			Node node0 = list[--listitems];

			/* create new node */
			Node newnode = huffnode[nextalloc++];
			newnode.parent = null;
			node0.parent = node1.parent = newnode;
			newnode.weight = node0.weight + node1.weight;

			/* insert into list at appropriate location */
			for (curitem = 0; curitem < listitems; curitem++) {
				if (newnode.weight > list[curitem].weight) {
					System.arraycopy(list, curitem, list, curitem + 1, listitems - curitem);
					break;
				}
			}
			list[curitem] = newnode;
			listitems++;
		}

		/* compute the number of bits in each code, and fill in another histogram */
		for (curcode = 0; curcode < numcodes; curcode++) {
			Node curnode;
			Node node = huffnode[curcode];
			node.numbits = 0;
			node.bits = 0;

			/* if we have a non-zero weight, compute the number of bits */
			if (node.weight > 0) {
				/* determine the number of bits for this node */
				for (curnode = node; curnode.parent != null; curnode = curnode.parent) {
					node.numbits++;
				}
				if (node.numbits == 0) {
					node.numbits = 1;
				}

				/* keep track of the max */
				maxbits = Math.max(maxbits, node.numbits);
			}
		}

		return maxbits;
	}

	/*-------------------------------------------------
	 *  assign_canonical_codes - assign canonical codes
	 *  to all the nodes based on the number of bits
	 *  in each
	 *-------------------------------------------------
	 */

	private HuffmanError assign_canonical_codes() {
		int curcode;
		int codelen;
		int curstart = 0;
		/* build up a histogram of bit lengths */
		final int bithisto[] = new int[33];
		for (curcode = 0; curcode < numcodes; curcode++)
		{
			Node node = huffnode[curcode];
			if (node.numbits > maxbits) {
				return HuffmanError.HUFFERR_INTERNAL_INCONSISTENCY;
			}
			if (node.numbits <= 32) {
				bithisto[node.numbits]++;
			}
		}

		/* for each code length, determine the starting code number */
		for (codelen = 32; codelen > 0; codelen--) {
			int nextstart = (curstart + bithisto[codelen]) >> 1;
			if (codelen != 1 && nextstart * 2 != (curstart + bithisto[codelen])) {
				return HuffmanError.HUFFERR_INTERNAL_INCONSISTENCY;
			}
			bithisto[codelen] = curstart;
			curstart = nextstart;
		}

		/* now assign canonical codes */
		for (curcode = 0; curcode < numcodes; curcode++) {
			Node node = huffnode[curcode];
			if (node.numbits > 0) {
				node.bits = bithisto[node.numbits]++;
			}
		}

		return HuffmanError.HUFFERR_NONE;
	}

	/*-------------------------------------------------
	 *  build_lookup_table - build a lookup table for
	 *  fast decoding
	 *-------------------------------------------------
	 */

	private void build_lookup_table() {
		int curcode;
		/* iterate over all codes */
		for (curcode = 0; curcode < numcodes; curcode++) {
			/* process all nodes which have non-zero bits */
			Node node = huffnode[curcode];
			if (node.numbits > 0) {
				/* set up the entry */
				int value = MAKE_LOOKUP(curcode, node.numbits);

				/* fill all matching entries */
				int shift = maxbits - node.numbits;
				Arrays.fill(lookup, node.bits << shift, (node.bits + 1) << shift, value);
			}
		}
	}
}
