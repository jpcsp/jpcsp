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
package jpcsp.memory;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Collection of native functions to handle memory allocated natively.
 * This is similar to direct buffers (NIO), but more PSP-like.
 * 
 * @author gid15
 *
 */
public class NativeMemoryUtils {
	/**
	 * Initialization method.
	 * Has to be called at least once before any other native method can be used.
	 */
	public static native void init();

	/**
	 * Allocate native memory.
	 * 
	 * @param size size in bytes of the memory to be allocated.
	 * @return     the base address of the allocated native memory.
	 */
	public static native long alloc(int size);

	/**
	 * Free native memory.
	 * 
	 * @param memory the base address of the native memory.
	 */
	public static native void free(long memory);

	/**
	 * Read one byte (8 bits) from a native memory.
	 * 
	 * @param memory   the base address of the native memory (as returned by alloc).
	 * @param address  the offset inside the native memory.
	 * @return         the unsigned 8-bits value at the given address.
	 */
	public static native int read8(long memory, int address);

	/**
	 * Write one byte (8 bits) into a native memory.
	 * 
	 * @param memory   the base address of the native memory (as returned by alloc).
	 * @param address  the offset inside the native memory.
	 * @param value    the unsigned 8-bits value to be written.
	 */
	public static native void write8(long memory, int address, int value);

	/**
	 * Read one short (16 bits) from a native memory.
	 * 
	 * @param memory   the base address of the native memory (as returned by alloc).
	 * @param address  the offset inside the native memory.
	 * @return         the unsigned 16-bits value at the given address.
	 */
	public static native int read16(long memory, int address);

	/**
	 * Write one short (16 bits) into a native memory.
	 * 
	 * @param memory   the base address of the native memory (as returned by alloc).
	 * @param address  the offset inside the native memory.
	 * @param value    the unsigned 16-bits value to be written.
	 */
	public static native void write16(long memory, int address, int value);

	/**
	 * Read one int (32 bits) from a native memory.
	 * 
	 * @param memory   the base address of the native memory (as returned by alloc).
	 * @param address  the offset inside the native memory.
	 * @return         the signed 32-bits value at the given address.
	 */
	public static native int read32(long memory, int address);

	/**
	 * Write one int (32 bits) into a native memory.
	 * 
	 * @param memory   the base address of the native memory (as returned by alloc).
	 * @param address  the offset inside the native memory.
	 * @param value    the signed 32-bits value to be written.
	 */
	public static native void write32(long memory, int address, int value);

	/**
	 * Fill an area into a native memory with one byte (8 bits) value.
	 * 
	 * @param memory   the base address of the native memory (as returned by alloc).
	 * @param address  the offset inside the native memory.
	 * @param value    the unsigned 8-bits value to be written.
	 * @param length   the number of bytes to be written.
	 */
	public static native void memset(long memory, int address, int value, int length);

	/**
	 * Copy an area from a native memory to another area of the native memory.
	 * 
	 * @param memoryDestination the destination base address of the native memory (as returned by alloc).
	 * @param destination       the offset inside the native memory for the destination area.
	 * @param memorySource      the source base address of the native memory (as returned by alloc).
	 * @param source            the offset inside the native memory for the source area.
	 * @param length            the number of bytes to be copied.
	 */
	public static native void memcpy(long memoryDestination, int destination, long memorySource, int source, int length);

	/**
	 * Return the length of a null-terminated string stored in a native memory
	 * (using the standard "strlen" function).
	 * The string has to be terminated by a byte having the value 0.
	 * 
	 * @param memory   the base address of the native memory (as returned by alloc).
	 * @param address  the offset inside the native memory.
	 * @return         the length of the string.
	 */
	public static native int strlen(long memory, int address);

	/**
	 * Create a Direct Buffer representing an area into a native memory.
	 * 
	 * @param memory   the base address of the native memory (as returned by alloc).
	 * @param address  the offset inside the native memory.
	 * @param length   the number of bytes of the area.
	 * @return         a new Direct Buffer representing the area of the native memory.
	 *                 The Direct Buffer has always the default ByteOrder BIG_ENDIAN.
	 */
	public static native ByteBuffer getBuffer(long memory, int address, int length);

	/**
	 * Check if the current emulator host is little or big endian.
	 * Remark: the PSP is little endian.
	 * 
	 * @return  true if the current host is little endian.
	 *          false if the current host is big endian.
	 */
	public static native boolean isLittleEndian();

	/**
	 * Copy bytes from a Direct Buffer to a native memory.
	 * 
	 * @param memory   the base address of the native memory (as returned by alloc).
	 * @param address  the offset inside the native memory.
	 * @param buffer   the Direct Buffer to be used as a source.
	 *                 The buffer position, capacity and limit are ignored.
	 * @param bufferOffset the offset in bytes from the Direct Buffer.
	 * @param length   the number of bytes to be copied.
	 */
	public static native void copyBufferToMemory(long memory, int address, Buffer buffer, int bufferOffset, int length);

	/**
	 * Copy bytes from a native memory to a Direct Buffer.
	 * 
	 * @param buffer   the Direct Buffer to be used as a destination.
	 *                 The buffer position, capacity and limit are ignored.
	 * @param bufferOffset the offset in bytes from the Direct Buffer.
	 * @param memory   the base address of the native memory (as returned by alloc).
	 * @param address  the offset inside the native memory.
	 * @param length   the number of bytes to be copied.
	 */
	public static native void copyMemoryToBuffer(Buffer buffer, int bufferOffset, long memory, int address, int length);
}
