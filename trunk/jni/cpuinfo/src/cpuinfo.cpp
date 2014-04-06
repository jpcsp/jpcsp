// cpuinfo.cpp : Defines the exported functions for the DLL application.
//

#include "stdafx.h"
#include "jpcsp_util_NativeCpuInfo.h"
#include "intrin.h"

bool hasSSE=false;
bool hasSSE2=false;
bool hasSSE3=false;
bool hasSSSE3=false;
bool hasSSE41=false;
bool hasSSE42=false;
bool hasAVX=false;
bool hasAVX2=false;

// Define interface to cpuid instruction.
// input:  eax = functionnumber, ecx = 0
// output: eax = output[0], ebx = output[1], ecx = output[2], edx = output[3]
static inline void cpuid(int output[4], int functionnumber)
{
	__cpuidex(output, functionnumber, 0);
}
JNIEXPORT void JNICALL Java_jpcsp_util_NativeCpuInfo_init(JNIEnv *, jclass)
{
	int abcd[4] = { 0, 0, 0, 0 };                // cpuid results
    cpuid(abcd, 0);                              // call cpuid function 0
	if (abcd[0] == 0)
	{
		//no cpuid :P
	}
	cpuid(abcd, 1);                              // call cpuid function 1 for feature flags
	if ((abcd[3] & (1 << 25)) != 0) hasSSE=true; //  SSE

	if ((abcd[3] & (1 << 26)) != 0) hasSSE2=true; // SSE2

	if ((abcd[2] & (1 <<  0)) != 0) hasSSE3=true; //  SSE3

	if ((abcd[2] & (1 <<  9)) != 0) hasSSSE3=true; //  SSSE3

	if ((abcd[2] & (1 << 19)) != 0) hasSSE41=true; // SSE4.1

	if ((abcd[2] & (1 << 20)) != 0) hasSSE42=true; // SSE4.2

	if ((abcd[2] & (1 << 28)) != 0) hasAVX=true; //  AVX

	cpuid(abcd, 7);                              // call cpuid leaf 7 for feature flags
    if ((abcd[1] & (1 <<  5)) != 0)  hasAVX2=true; // AVX2




}
JNIEXPORT jboolean JNICALL Java_jpcsp_util_NativeCpuInfo_hasSSE(JNIEnv *, jclass)
{
	return hasSSE;
}


JNIEXPORT jboolean JNICALL Java_jpcsp_util_NativeCpuInfo_hasSSE2(JNIEnv *, jclass)
{
	return hasSSE2;
}


JNIEXPORT jboolean JNICALL Java_jpcsp_util_NativeCpuInfo_hasSSE3(JNIEnv *, jclass)
{
	return hasSSE3;
}
JNIEXPORT jboolean JNICALL Java_jpcsp_util_NativeCpuInfo_hasSSSE3(JNIEnv *, jclass)
{
	return hasSSSE3;
}

JNIEXPORT jboolean JNICALL Java_jpcsp_util_NativeCpuInfo_hasSSE41(JNIEnv *, jclass)
{
	return hasSSE41;
}

JNIEXPORT jboolean JNICALL Java_jpcsp_util_NativeCpuInfo_hasSSE42(JNIEnv *, jclass)
{
	return hasSSE42;
}


JNIEXPORT jboolean JNICALL Java_jpcsp_util_NativeCpuInfo_hasAVX(JNIEnv *, jclass)
{
	return hasAVX;
}


JNIEXPORT jboolean JNICALL Java_jpcsp_util_NativeCpuInfo_hasAVX2(JNIEnv *, jclass)
{
	return hasAVX2;
}

