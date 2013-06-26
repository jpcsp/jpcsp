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
#ifndef _ENDIAN_H
#define _ENDIAN_H

#ifndef IS_LITTLE_ENDIAN
  #define IS_LITTLE_ENDIAN 0
#endif
#ifndef IS_BIG_ENDIAN
  #define IS_BIG_ENDIAN 0
#endif

/* Test common endianness defines to try to find out the endianness at compile time */
#if defined(__LITTLE_ENDIAN__)
  #define IS_LITTLE_ENDIAN __LITTLE_ENDIAN__
  #define IS_BIG_ENDIAN !__LITTLE_ENDIAN__
#elsif defined(_LITTLE_ENDIAN)
  #define IS_LITTLE_ENDIAN _LITTLE_ENDIAN
  #define IS_BIG_ENDIAN !_LITTLE_ENDIAN
#elsif defined(__BIG_ENDIAN__)
  #define IS_LITTLE_ENDIAN !__BIG_ENDIAN__
  #define IS_BIG_ENDIAN __BIG_ENDIAN__
#elsif defined(_BIG_ENDIAN)
  #define IS_LITTLE_ENDIAN !_BIG_ENDIAN
  #define IS_BIG_ENDIAN _BIG_ENDIAN
#endif

extern int littleEndian;

#if !IS_LITTLE_ENDIAN
static jint reverseBytes32(jint n) {
	return ((n >> 24) & 0xFF) | ((n >> 8) & 0xFF00) | ((n << 8) & 0xFF0000) | (n << 24);
}

static unsigned short reverseBytes16(unsigned short n) {
	return (n >> 8) | (n << 8);
}

static int isLittleEndian() {
	/* gcc -O2 is able to reduce these statements to a simple constant value,
	   eliminating completely the reverseBytesXX functions for little endian.
	*/
	union {
		jint i;
		unsigned char c[4];
	} n;
	n.i = 0x12345678;
	return (n.c[0] == 0x78 && n.c[1] == 0x56 && n.c[2] == 0x34 && n.c[3] == 0x12);
}
#endif

#if IS_BIG_ENDIAN
  #define CONVERT16(value) value = reverseBytes16(value)
  #define CONVERT32(value) value = reverseBytes32(value)
#elsif IS_LITTLE_ENDIAN
  #define CONVERT16(value)
  #define CONVERT32(value)
#else
  #define CONVERT16(value) if (!isLittleEndian()) { value = reverseBytes16(value); }
  #define CONVERT32(value) if (!isLittleEndian()) { value = reverseBytes32(value); }
#endif

#endif
