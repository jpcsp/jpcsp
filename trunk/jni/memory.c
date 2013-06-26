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
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "endian.h"

int littleEndian;

JNIEXPORT void JNICALL Java_jpcsp_memory_NativeMemoryUtils_init(JNIEnv * env, jclass self) {
	/* Test the endianness at runtime */
	littleEndian = isLittleEndian();
}

JNIEXPORT jlong JNICALL Java_jpcsp_memory_NativeMemoryUtils_alloc(JNIEnv * env, jclass self, jint size) {
	return (jlong) malloc(size);
}

JNIEXPORT void JNICALL Java_jpcsp_memory_NativeMemoryUtils_free(JNIEnv * env, jclass self, jlong memory) {
	free((void *) memory);
}

JNIEXPORT jint JNICALL Java_jpcsp_memory_NativeMemoryUtils_read8(JNIEnv * env, jclass self, jlong memory, jint address) {
	unsigned char *p8 = (void *) memory + address;
	return (jint) *p8;
}

JNIEXPORT void JNICALL Java_jpcsp_memory_NativeMemoryUtils_write8(JNIEnv * env, jclass self, jlong memory, jint address, jint value) {
	unsigned char *p8 = (void *) memory + address;
	*p8 = (unsigned char) value;
}

JNIEXPORT jint JNICALL Java_jpcsp_memory_NativeMemoryUtils_read16(JNIEnv * env, jclass self, jlong memory, jint address) {
	unsigned short *p16 = (void *) memory + (address & ~1);
	unsigned short value = *p16;
	CONVERT16(value);
	return value;
}

JNIEXPORT void JNICALL Java_jpcsp_memory_NativeMemoryUtils_write16(JNIEnv * env, jclass self, jlong memory, jint address, jint value) {
	CONVERT16(value);
	unsigned short *p16 = (void *) memory + (address & ~1);
	*p16 = (unsigned short) value;
}

JNIEXPORT jint JNICALL Java_jpcsp_memory_NativeMemoryUtils_read32(JNIEnv * env, jclass self, jlong memory, jint address) {
	jint *p32 = (void *) memory + (address & ~3);
	jint value = *p32;
	CONVERT32(value);
	return value;
}

JNIEXPORT void JNICALL Java_jpcsp_memory_NativeMemoryUtils_write32(JNIEnv * env, jclass self, jlong memory, jint address, jint value) {
	CONVERT32(value);
	jint *p32 = (void *) memory + (address & ~3);
	*p32 = value;
}

JNIEXPORT void JNICALL Java_jpcsp_memory_NativeMemoryUtils_memset(JNIEnv * env, jclass self, jlong memory, jint address, jint value, jint length) {
	memset((void *) memory + address, value, length);
}

JNIEXPORT void JNICALL Java_jpcsp_memory_NativeMemoryUtils_memcpy(JNIEnv * env, jclass self, jlong memoryDestination, jint destination, jlong memorySource, jint source, jint length) {
	memcpy((void *) memoryDestination + destination, (void *) memorySource + source, length);
}

JNIEXPORT jint JNICALL Java_jpcsp_memory_NativeMemoryUtils_strlen(JNIEnv * env, jclass self, jlong memory, jint address) {
	return strlen((void *) memory + address);
}

JNIEXPORT jobject JNICALL Java_jpcsp_memory_NativeMemoryUtils_getBuffer(JNIEnv * env, jclass self, jlong memory, jint address, jint length) {
	return (*env)->NewDirectByteBuffer(env, (void *) memory + address, length);
}

JNIEXPORT jint JNICALL Java_jpcsp_memory_NativeMemoryUtils_isLittleEndian(JNIEnv * env, jclass self) {
	return littleEndian;
}

JNIEXPORT void JNICALL Java_jpcsp_memory_NativeMemoryUtils_copyBufferToMemory(JNIEnv * env, jclass self, jlong memory, jint address, jobject buffer, jint bufferOffset, jint length) {
	void *bufferAddress = (*env)->GetDirectBufferAddress(env, buffer);
	if (bufferAddress != NULL) {
		memcpy((void *) memory + address, bufferAddress + bufferOffset, length);
	}
}

JNIEXPORT void JNICALL Java_jpcsp_memory_NativeMemoryUtils_copyMemoryToBuffer(JNIEnv * env, jclass self, jobject buffer, jint bufferOffset, jlong memory, jint address, jint length) {
	void *bufferAddress = (*env)->GetDirectBufferAddress(env, buffer);
	if (bufferAddress != NULL) {
		memcpy(bufferAddress + bufferOffset, (void *) memory + address, length);
	}
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *pvt) {
	return JNI_VERSION_1_2;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *pvt) {
}
	
void _init() {
}

void _fini() {
}
